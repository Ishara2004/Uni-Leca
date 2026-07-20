package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupRestoreHelper
import com.example.data.model.*
import com.example.data.repository.AttendanceRepository
import com.example.notification.AlarmScheduler
import com.example.ui.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AttendanceRepository(application)
    private val alarmScheduler = AlarmScheduler(application)
    val settingsManager = SettingsManager(application)

    // Current screen navigation state
    private val _currentScreen = MutableStateFlow(Screen.Today)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Date navigation for today's screen
    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Active semester (if any)
    val activeSemester: StateFlow<Semester?> = repository.activeSemester
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All semesters list
    val allSemesters: StateFlow<List<Semester>> = repository.allSemesters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected semester (for history/current viewing)
    private val _selectedSemesterId = MutableStateFlow<Int?>(null)
    val selectedSemesterId: StateFlow<Int?> = _selectedSemesterId.asStateFlow()

    // Modules for selected semester
    val modules: StateFlow<List<Module>> = _selectedSemesterId
        .flatMapLatest { id ->
            if (id != null) repository.getModulesForSemester(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Slots with module details for selected semester
    val slots: StateFlow<List<TimetableSlotWithModule>> = _selectedSemesterId
        .flatMapLatest { id ->
            if (id != null) repository.getSlotsWithModuleForSemester(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stats for modules of selected semester
    val moduleStats: StateFlow<List<ModuleAttendanceStats>> = _selectedSemesterId
        .flatMapLatest { id ->
            if (id != null) repository.getModuleAttendanceStats(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All attendance details of selected semester (Logs)
    val attendanceDetails: StateFlow<List<AttendanceDetail>> = _selectedSemesterId
        .flatMapLatest { id ->
            if (id != null) repository.getAttendanceDetailsForSemester(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered attendance details for selected date on selected semester
    val todayAttendanceDetails: StateFlow<List<AttendanceDetail>> = combine(
        _selectedSemesterId,
        _selectedDate
    ) { semId, date ->
        Pair(semId, date)
    }.flatMapLatest { (semId, date) ->
        if (semId != null) repository.getAttendanceDetailsForDate(semId, date) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically sync selectedSemesterId with the active semester
        viewModelScope.launch {
            activeSemester.collect { active ->
                if (active != null && _selectedSemesterId.value == null) {
                    _selectedSemesterId.value = active.id
                } else if (active == null && _selectedSemesterId.value == null) {
                    allSemesters.firstOrNull()?.firstOrNull()?.let {
                        _selectedSemesterId.value = it.id
                    }
                }
            }
        }

        // Reschedule alarms whenever slots change
        viewModelScope.launch {
            slots.collect { currentSlots ->
                val active = activeSemester.value
                val selected = _selectedSemesterId.value
                if (active != null && selected == active.id) {
                    alarmScheduler.scheduleAlarmsForSlots(currentSlots)
                }
            }
        }
    }

    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectSemester(id: Int) {
        _selectedSemesterId.value = id
    }

    fun changeSelectedDate(date: String) {
        _selectedDate.value = date
    }

    // Operations
    fun createSemester(name: String, duplicateFromId: Int? = null) {
        viewModelScope.launch {
            val newId = repository.insertSemester(name).toInt()
            if (duplicateFromId != null) {
                repository.duplicateSemester(duplicateFromId, newId)
            }
            _selectedSemesterId.value = newId
            _currentScreen.value = Screen.Today
        }
    }

    fun endCurrentSemester() {
        val active = activeSemester.value ?: return
        viewModelScope.launch {
            repository.endSemester(active.id)
            _currentScreen.value = Screen.SemesterHistory
        }
    }

    fun addModule(name: String, threshold: Float) {
        val semId = _selectedSemesterId.value ?: return
        viewModelScope.launch {
            repository.insertModule(semId, name, threshold)
        }
    }

    fun deleteModule(module: Module) {
        viewModelScope.launch {
            repository.deleteModule(module)
        }
    }

    fun addTimetableSlot(moduleId: Int, dayOfWeek: Int, startTime: String, endTime: String, sessionType: String) {
        viewModelScope.launch {
            repository.insertSlot(moduleId, dayOfWeek, startTime, endTime, sessionType)
        }
    }

    fun deleteTimetableSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            repository.deleteSlot(slot)
        }
    }

    fun markAttendance(slotId: Int, date: String, heldStatus: String, attendanceStatus: String) {
        viewModelScope.launch {
            repository.markAttendance(slotId, date, heldStatus, attendanceStatus)
        }
    }

    // Import/Export
    suspend fun exportCSV(outputStream: OutputStream) {
        val details = attendanceDetails.value
        val stats = moduleStats.value
        withContext(Dispatchers.IO) {
            com.example.ui.reports.ExportHelper.exportToCSV(details, stats, outputStream)
        }
    }

    suspend fun exportPDF(outputStream: OutputStream) {
        val semName = allSemesters.value.find { it.id == _selectedSemesterId.value }?.name ?: "Semester"
        val details = attendanceDetails.value
        val stats = moduleStats.value
        withContext(Dispatchers.IO) {
            com.example.ui.reports.ExportHelper.exportToPDF(semName, details, stats, outputStream)
        }
    }

    suspend fun exportBackup(outputStream: OutputStream): Boolean {
        val helper = BackupRestoreHelper(getApplication())
        return helper.exportData(outputStream)
    }

    suspend fun importBackup(inputStream: InputStream): Boolean {
        val helper = BackupRestoreHelper(getApplication())
        val success = helper.importData(inputStream)
        if (success) {
            // Reset selected semester after successful restore
            val firstSem = repository.semesterDao.getAllSemestersDirect().firstOrNull()
            _selectedSemesterId.value = firstSem?.id
        }
        return success
    }

    // Helpers
    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    enum class Screen {
        Today,
        Timetable,
        Analytics,
        Reports,
        Settings,
        SemesterSetup,
        SemesterHistory
    }
}
