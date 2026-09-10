package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.model.*
import com.example.ui.reports.ExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val repository = container.repository
    val settingsManager = container.settings

    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val activeSemester: StateFlow<Semester?> = repository.activeSemester
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allSemesters: StateFlow<List<Semester>> = repository.allSemesters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedSemesterId = MutableStateFlow<Int?>(null)
    val selectedSemesterId: StateFlow<Int?> = _selectedSemesterId.asStateFlow()

    val selectedSemester: StateFlow<Semester?> = combine(allSemesters, selectedSemesterId) { semesters, id ->
        semesters.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val modules: StateFlow<List<Module>> = _selectedSemesterId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.getModulesForSemester(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val slots: StateFlow<List<TimetableSlotWithModule>> = _selectedSemesterId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.getSlotsWithModuleForSemester(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sessionsForSelectedDate: StateFlow<List<TimetableSlotWithModule>> = combine(
        _selectedSemesterId,
        _selectedDate
    ) { semesterId, date -> semesterId to date }
        .flatMapLatest { (semesterId, date) ->
            if (semesterId == null) flowOf(emptyList()) else repository.getSessionsForDate(semesterId, date)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val moduleStats: StateFlow<List<ModuleAttendanceStats>> = _selectedSemesterId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.getModuleAttendanceStats(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val attendanceDetails: StateFlow<List<AttendanceDetail>> = _selectedSemesterId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.getAttendanceDetailsForSemester(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val attendanceForSelectedDate: StateFlow<List<AttendanceDetail>> = combine(
        _selectedSemesterId,
        _selectedDate
    ) { semesterId, date -> semesterId to date }
        .flatMapLatest { (semesterId, date) ->
            if (semesterId == null) flowOf(emptyList()) else repository.getAttendanceDetailsForDate(semesterId, date)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _navigation = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val navigation: SharedFlow<String> = _navigation.asSharedFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(activeSemester, allSemesters, selectedSemesterId) { active, semesters, selected ->
                Triple(active, semesters, selected)
            }.collect { (active, semesters, selected) ->
                if (selected == null || semesters.none { it.id == selected }) {
                    _selectedSemesterId.value = active?.id ?: semesters.firstOrNull()?.id
                }
            }
        }

        // Alarm scheduling follows the active semester, not whichever historical semester the user is viewing.
        viewModelScope.launch {
            activeSemester.flatMapLatest { active ->
                if (active == null) flowOf(emptyList()) else repository.getSlotsWithModuleForSemester(active.id)
            }.collect { activeSlots ->
                container.alarmScheduler.scheduleAlarmsForSlots(activeSlots)
            }
        }
    }

    fun selectSemester(id: Int) {
        _selectedSemesterId.value = id
    }

    fun changeSelectedDate(date: String) {
        if (runCatching { LocalDate.parse(date) }.isSuccess) _selectedDate.value = date
    }

    fun jumpToToday() {
        _selectedDate.value = LocalDate.now().toString()
    }

    fun openDateFromNotification(date: String) {
        changeSelectedDate(date)
        _navigation.tryEmit("today")
    }

    fun createSemester(name: String, duplicateFromId: Int? = null) = launchOperation("Semester created") {
        require(name.isNotBlank()) { "Semester name is required" }
        val newId = repository.createSemester(name, duplicateFromId).toInt()
        _selectedSemesterId.value = newId
        _selectedDate.value = LocalDate.now().toString()
        _navigation.emit("today")
    }

    fun endCurrentSemester() {
        val active = activeSemester.value ?: return
        viewModelScope.launch {
            runCatching {
                container.alarmScheduler.cancelAllKnownAlarms()
                repository.endSemester(active.id)
                _selectedSemesterId.value = active.id
                _navigation.emit("semester-history")
            }.onFailure { _messages.emit(it.message ?: "Could not end semester") }
        }
    }

    fun deleteSemester(semesterId: Int) {
        viewModelScope.launch {
            runCatching {
                if (activeSemester.value?.id == semesterId) container.alarmScheduler.cancelAllKnownAlarms()
                repository.deleteSemester(semesterId)
                val remaining = allSemesters.value.filterNot { it.id == semesterId }
                _selectedSemesterId.value = remaining.firstOrNull()?.id
                _messages.emit("Semester deleted")
            }.onFailure { _messages.emit(it.message ?: "Could not delete semester") }
        }
    }

    fun addModule(name: String, threshold: Float) = launchOperation("Module added") {
        val semId = requireSelectedEditableSemester()
        require(name.isNotBlank()) { "Module name is required" }
        repository.insertModule(semId, name, threshold)
    }

    fun updateModule(module: Module) = launchOperation("Module updated") {
        requireSelectedEditableSemester()
        repository.updateModule(module)
    }

    fun deleteModule(module: Module) = launchOperation("Module removed; existing history was preserved when needed") {
        requireSelectedEditableSemester()
        container.alarmScheduler.scheduleAlarmsForSlots(slots.value.filterNot { it.slot.moduleId == module.id })
        repository.deleteModule(module)
    }

    fun addTimetableSlot(moduleId: Int, dayOfWeek: Int, start: String, end: String, type: String) =
        launchOperation("Timetable slot added") {
            requireSelectedEditableSemester()
            validateNoOverlap(moduleId, dayOfWeek, null, start, end, recurring = true)
            repository.insertRecurringSlot(moduleId, dayOfWeek, start, end, type)
        }

    fun addExtraSession(moduleId: Int, date: String, start: String, end: String, type: String) =
        launchOperation("Extra session added") {
            requireSelectedEditableSemester()
            val localDate = LocalDate.parse(date)
            validateNoOverlap(moduleId, localDate.dayOfWeek.value, date, start, end, recurring = false)
            repository.insertExtraSession(moduleId, date, start, end, type)
        }

    fun updateTimetableSlot(slot: TimetableSlot) = launchOperation("Session updated") {
        requireSelectedEditableSemester()
        repository.updateSlot(slot)
    }

    fun deleteTimetableSlot(slot: TimetableSlot) = launchOperation("Session removed; attendance history preserved") {
        requireSelectedEditableSemester()
        container.alarmScheduler.cancelAlarm(slot.id)
        repository.deleteSlot(slot)
    }

    fun markAttendance(slotId: Int, date: String, heldStatus: String, attendanceStatus: String) {
        viewModelScope.launch {
            runCatching {
                val selected = selectedSemester.value ?: error("No semester selected")
                require(selected.status == SemesterStatus.ACTIVE.dbValue || LocalDate.parse(date) <= endDate(selected)) {
                    "This date is outside the semester"
                }
                require(LocalDate.parse(date) <= LocalDate.now()) { "Future attendance cannot be recorded" }
                repository.markAttendance(slotId, date, heldStatus, attendanceStatus)
            }.onFailure { _messages.emit(it.message ?: "Could not save attendance") }
        }
    }

    fun clearAttendance(slotId: Int, date: String) = launchOperation("Attendance mark cleared") {
        repository.clearAttendance(slotId, date)
    }

    fun riskFor(stat: ModuleAttendanceStats): AttendanceRisk = repository.calculateRisk(stat)

    suspend fun exportCSV(outputStream: OutputStream) {
        val semName = selectedSemester.value?.name ?: "Semester"
        withContext(Dispatchers.IO) {
            ExportHelper.exportToCSV(semName, attendanceDetails.value, moduleStats.value, outputStream)
        }
    }

    suspend fun exportPDF(outputStream: OutputStream) {
        val semName = selectedSemester.value?.name ?: "Semester"
        withContext(Dispatchers.IO) {
            ExportHelper.exportToPDF(semName, attendanceDetails.value, moduleStats.value, outputStream)
        }
    }

    suspend fun exportBackup(outputStream: OutputStream): Boolean =
        container.backupRestoreHelper.exportData(outputStream, appVersion = "2.0")

    suspend fun importBackup(inputStream: InputStream): Boolean {
        val success = container.backupRestoreHelper.importData(inputStream)
        if (success) {
            _selectedSemesterId.value = repository.getActiveSemesterSync()?.id
            container.alarmScheduler.cancelAllKnownAlarms()
        }
        return success
    }

    private fun launchOperation(successMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { _messages.emit(successMessage) }
                .onFailure { _messages.emit(it.message ?: "Operation failed") }
        }
    }

    private fun requireSelectedEditableSemester(): Int {
        val sem = selectedSemester.value ?: error("No semester selected")
        require(sem.status == SemesterStatus.ACTIVE.dbValue) { "Ended semesters are read-only" }
        return sem.id
    }

    private fun endDate(semester: Semester): LocalDate =
        semester.endedDate?.let {
            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        } ?: LocalDate.now()

    private fun validateNoOverlap(
        moduleId: Int,
        dayOfWeek: Int,
        specificDate: String?,
        start: String,
        end: String,
        recurring: Boolean
    ) {
        val startTime = java.time.LocalTime.parse(normalizeTime(start))
        val endTime = java.time.LocalTime.parse(normalizeTime(end))
        require(endTime.isAfter(startTime)) { "End time must be after start time" }

        val conflict = slots.value.any { item ->
            val s = item.slot
            if (s.moduleId == moduleId && s.startTime == normalizeTime(start) && s.endTime == normalizeTime(end) &&
                s.isRecurring == recurring && (recurring && s.dayOfWeek == dayOfWeek || !recurring && s.specificDate == specificDate)) {
                true
            } else {
                val sameDay = if (recurring) s.isRecurring && s.dayOfWeek == dayOfWeek
                else !s.isRecurring && s.specificDate == specificDate
                if (!sameDay) false
                else {
                    val existingStart = java.time.LocalTime.parse(s.startTime)
                    val existingEnd = java.time.LocalTime.parse(s.endTime)
                    startTime < existingEnd && endTime > existingStart
                }
            }
        }
        require(!conflict) { "This session overlaps an existing slot" }
    }

    private fun normalizeTime(value: String): String {
        val parts = value.trim().split(":")
        require(parts.size == 2) { "Use HH:mm format" }
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        require(h in 0..23 && m in 0..59) { "Invalid time" }
        return "%02d:%02d".format(h, m)
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) return MainViewModel(container) as T
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
