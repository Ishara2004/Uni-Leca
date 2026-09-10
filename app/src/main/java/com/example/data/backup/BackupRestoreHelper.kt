package com.example.data.backup

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.SemesterStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

class BackupRestoreHelper(private val db: AppDatabase) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val payloadAdapter = moshi.adapter(BackupPayload::class.java)
    private val envelopeAdapter = moshi.adapter(BackupEnvelope::class.java)

    suspend fun exportData(outputStream: OutputStream, appVersion: String = "2.0"): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = BackupPayload(
                    semesters = db.semesterDao().getAllSemestersDirect(),
                    modules = db.moduleDao().getAllModulesDirect(),
                    slots = db.timetableSlotDao().getAllSlotsDirect(),
                    entries = db.attendanceEntryDao().getAllEntriesDirect()
                )
                val payloadJson = payloadAdapter.toJson(payload)
                val envelope = BackupEnvelope(
                    formatVersion = CURRENT_FORMAT_VERSION,
                    appVersion = appVersion,
                    createdAt = System.currentTimeMillis(),
                    payload = payload,
                    checksumSha256 = sha256(payloadJson)
                )
                outputStream.use { it.write(envelopeAdapter.toJson(envelope).toByteArray(Charsets.UTF_8)) }
            }.isSuccess
        }

    suspend fun inspectBackup(inputStream: InputStream): BackupPreview? = withContext(Dispatchers.IO) {
        runCatching {
            val json = readLimited(inputStream)
            val decoded = decode(json) ?: return@runCatching null
            BackupPreview(
                formatVersion = decoded.formatVersion,
                createdAt = decoded.createdAt,
                semesterCount = decoded.payload.semesters.size,
                moduleCount = decoded.payload.modules.size,
                attendanceCount = decoded.payload.entries.size,
                isLegacy = decoded.isLegacy
            )
        }.getOrNull()
    }

    suspend fun importData(inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val json = readLimited(inputStream)
            val decoded = decode(json) ?: error("Unsupported backup format")
            validatePayload(decoded.payload)
            val payload = normalizePayload(decoded.payload)

            db.withTransaction {
                db.clearAllTables()
                if (payload.semesters.isNotEmpty()) db.semesterDao().insertSemestersList(payload.semesters)
                if (payload.modules.isNotEmpty()) db.moduleDao().insertModulesList(payload.modules)
                if (payload.slots.isNotEmpty()) db.timetableSlotDao().insertSlotsList(payload.slots)
                if (payload.entries.isNotEmpty()) db.attendanceEntryDao().insertEntriesList(payload.entries)
            }
        }.isSuccess
    }

    private data class DecodedBackup(
        val formatVersion: Int,
        val createdAt: Long?,
        val payload: BackupPayload,
        val isLegacy: Boolean
    )

    private fun decode(json: String): DecodedBackup? {
        val envelope = runCatching { envelopeAdapter.fromJson(json) }.getOrNull()
        if (envelope != null) {
            require(envelope.formatVersion in 1..CURRENT_FORMAT_VERSION) { "Backup version is too new" }
            val payloadJson = payloadAdapter.toJson(envelope.payload)
            require(sha256(payloadJson).equals(envelope.checksumSha256, ignoreCase = true)) {
                "Backup checksum mismatch"
            }
            return DecodedBackup(envelope.formatVersion, envelope.createdAt, envelope.payload, false)
        }

        // v1 Uni Leca backups were a plain object with semesters/modules/slots/entries.
        val legacyPayload = runCatching { payloadAdapter.fromJson(json) }.getOrNull() ?: return null
        return DecodedBackup(1, null, legacyPayload, true)
    }

    private fun validatePayload(payload: BackupPayload) {
        require(payload.semesters.size <= MAX_SEMESTERS)
        require(payload.modules.size <= MAX_MODULES)
        require(payload.slots.size <= MAX_SLOTS)
        require(payload.entries.size <= MAX_ATTENDANCE_ENTRIES)

        val semesterIds = payload.semesters.map { it.id }.toSet()
        require(payload.modules.all { it.semesterId in semesterIds }) { "Module references missing semester" }
        val moduleIds = payload.modules.map { it.id }.toSet()
        require(payload.slots.all { it.moduleId in moduleIds }) { "Slot references missing module" }
        val slotIds = payload.slots.map { it.id }.toSet()
        require(payload.entries.all { it.timetableSlotId in slotIds }) { "Attendance references missing slot" }
    }

    private fun normalizePayload(payload: BackupPayload): BackupPayload {
        val newestActiveId = payload.semesters
            .filter { it.status == SemesterStatus.ACTIVE.dbValue }
            .maxByOrNull { it.createdDate }?.id

        val semesters = payload.semesters.map { sem ->
            if (sem.status == SemesterStatus.ACTIVE.dbValue && sem.id != newestActiveId) {
                sem.copy(status = SemesterStatus.ENDED.dbValue, endedDate = sem.endedDate ?: System.currentTimeMillis())
            } else sem
        }

        // Preserve the last row when a legacy backup contains duplicate slot/date entries.
        val entries = payload.entries
            .groupBy { it.timetableSlotId to it.date }
            .values
            .mapNotNull { group -> group.maxByOrNull { it.id } }

        return payload.copy(semesters = semesters, entries = entries)
    }

    private fun readLimited(inputStream: InputStream): String {
        inputStream.use { input ->
            val bytes = input.readBytes(MAX_BACKUP_BYTES + 1)
            require(bytes.size <= MAX_BACKUP_BYTES) { "Backup file is too large" }
            return bytes.toString(Charsets.UTF_8)
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    companion object {
        const val CURRENT_FORMAT_VERSION = 2
        private const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
        private const val MAX_SEMESTERS = 200
        private const val MAX_MODULES = 5_000
        private const val MAX_SLOTS = 50_000
        private const val MAX_ATTENDANCE_ENTRIES = 500_000
    }
}
