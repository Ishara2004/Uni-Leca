package com.example.ui

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val allSemesters by viewModel.allSemesters.collectAsStateWithLifecycle()
    val selectedSemesterId by viewModel.selectedSemesterId.collectAsStateWithLifecycle()

    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val moduleStats by viewModel.moduleStats.collectAsStateWithLifecycle()
    val attendanceDetails by viewModel.attendanceDetails.collectAsStateWithLifecycle()
    val todayAttendanceDetails by viewModel.todayAttendanceDetails.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var showAddSemesterDialog by remember { mutableStateOf(false) }

    // Request permissions for Android 13+ notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification reminders disabled.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Uni Leca",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val activeSemName = allSemesters.find { it.id == selectedSemesterId }?.name ?: "No Semester"
                        val endedLabel = if (allSemesters.find { it.id == selectedSemesterId }?.status == "Ended") " (Ended)" else ""
                        Text(
                            text = "$activeSemName$endedLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddSemesterDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Semester")
                    }
                    if (activeSemester != null && selectedSemesterId == activeSemester?.id) {
                        Button(
                            onClick = { viewModel.endCurrentSemester() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("End Sem")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentScreen == MainViewModel.Screen.Today,
                    onClick = { viewModel.setScreen(MainViewModel.Screen.Today) },
                    icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                    label = { Text("Today") }
                )
                NavigationBarItem(
                    selected = currentScreen == MainViewModel.Screen.Timetable,
                    onClick = { viewModel.setScreen(MainViewModel.Screen.Timetable) },
                    icon = { Icon(Icons.Default.CalendarViewWeek, contentDescription = "Timetable") },
                    label = { Text("Timetable") }
                )
                NavigationBarItem(
                    selected = currentScreen == MainViewModel.Screen.Analytics,
                    onClick = { viewModel.setScreen(MainViewModel.Screen.Analytics) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Analytics") },
                    label = { Text("Analytics") }
                )
                NavigationBarItem(
                    selected = currentScreen == MainViewModel.Screen.Reports,
                    onClick = { viewModel.setScreen(MainViewModel.Screen.Reports) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text("Reports") }
                )
                NavigationBarItem(
                    selected = currentScreen == MainViewModel.Screen.Settings,
                    onClick = { viewModel.setScreen(MainViewModel.Screen.Settings) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (allSemesters.isEmpty()) {
                EmptySemesterSetupView { showAddSemesterDialog = true }
            } else {
                when (currentScreen) {
                    MainViewModel.Screen.Today -> TodayScreen(
                        viewModel = viewModel,
                        slots = slots,
                        todayEntries = todayAttendanceDetails,
                        selectedDate = selectedDate
                    )
                    MainViewModel.Screen.Timetable -> TimetableScreen(
                        viewModel = viewModel,
                        modules = modules,
                        slots = slots,
                        isReadOnly = allSemesters.find { it.id == selectedSemesterId }?.status == "Ended"
                    )
                    MainViewModel.Screen.Analytics -> AnalyticsScreen(
                        moduleStats = moduleStats
                    )
                    MainViewModel.Screen.Reports -> ReportsScreen(
                        viewModel = viewModel,
                        semesters = allSemesters,
                        selectedSemesterId = selectedSemesterId
                    )
                    MainViewModel.Screen.Settings -> SettingsScreen(
                        viewModel = viewModel
                    )
                    else -> Unit
                }
            }
        }
    }

    if (showAddSemesterDialog) {
        AddSemesterDialog(
            semesters = allSemesters,
            onDismiss = { showAddSemesterDialog = false },
            onConfirm = { name, duplicateFromId ->
                viewModel.createSemester(name, duplicateFromId)
                showAddSemesterDialog = false
            }
        )
    }
}

@Composable
fun EmptySemesterSetupView(onSetupClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = "School",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to Uni Leca",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Track your lecture, tutorial, and LAB attendance. Instantly verify if you are meeting threshold criteria.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onSetupClick,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("setup_first_semester_button")
        ) {
            Text("Create Your First Semester")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSemesterDialog(
    semesters: List<Semester>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, duplicateFromId: Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var duplicateOption by remember { mutableStateOf(false) }
    var selectedDuplicateId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(semesters) {
        if (semesters.isNotEmpty()) {
            selectedDuplicateId = semesters.first().id
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Semester") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Semester Name") },
                    placeholder = { Text("e.g. Semester 1 2026") },
                    modifier = Modifier.fillMaxWidth().testTag("semester_name_input")
                )
                if (semesters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { duplicateOption = !duplicateOption }
                    ) {
                        Checkbox(
                            checked = duplicateOption,
                            onCheckedChange = { duplicateOption = it }
                        )
                        Text("Duplicate previous modules & timetable")
                    }
                    if (duplicateOption) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Select source semester:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        semesters.forEach { sem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDuplicateId = sem.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedDuplicateId == sem.id,
                                    onClick = { selectedDuplicateId = sem.id }
                                )
                                Text(sem.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, if (duplicateOption) selectedDuplicateId else null)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TodayScreen(
    viewModel: MainViewModel,
    slots: List<TimetableSlotWithModule>,
    todayEntries: List<AttendanceDetail>,
    selectedDate: String
) {
    val context = LocalContext.current
    val dayOfWeek = getDayOfWeekForDate(selectedDate)

    // Filter slots for the day of the week
    val todaySlots = slots.filter { it.slot.dayOfWeek == dayOfWeek }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Date Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.changeSelectedDate(adjustDateString(selectedDate, -1))
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = getFormattedSelectedDate(selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = getDayOfWeekLabel(dayOfWeek),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = {
                viewModel.changeSelectedDate(adjustDateString(selectedDate, 1))
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (todaySlots.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Weekend,
                        contentDescription = "No classes",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No classes scheduled for today!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Enjoy your day off!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(todaySlots) { slotWithModule ->
                    val matchingEntry = todayEntries.find { it.slot.id == slotWithModule.slot.id }
                    TodaySessionCard(
                        slotWithModule = slotWithModule,
                        entry = matchingEntry?.entry,
                        onMark = { held, status ->
                            viewModel.markAttendance(
                                slotId = slotWithModule.slot.id,
                                date = selectedDate,
                                heldStatus = held,
                                attendanceStatus = status
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TodaySessionCard(
    slotWithModule: TimetableSlotWithModule,
    entry: AttendanceEntry?,
    onMark: (held: String, status: String) -> Unit
) {
    val isHeld = entry?.heldStatus ?: "Unmarked"
    val status = entry?.attendanceStatus ?: "Present"

    // Adapt background and border colors according to marking state to match Next/Active Session banner vs. normal list cards
    val cardColor = if (isHeld == "Unmarked") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val onCardColor = if (isHeld == "Unmarked") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val onCardVariant = if (isHeld == "Unmarked") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    val cardBorder = if (isHeld != "Unmarked") BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant) else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Main Slot Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slotWithModule.moduleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onCardColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = if (isHeld == "Unmarked") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = slotWithModule.slot.sessionType.uppercase(Locale.US),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isHeld == "Unmarked") Color.White else onCardVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = "${slotWithModule.slot.startTime} - ${slotWithModule.slot.endTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onCardVariant
                        )
                    }
                }

                // Header status badge
                val badgeText = when (isHeld) {
                    "Held" -> status
                    "Not Held" -> "No Class"
                    else -> "Upcoming"
                }
                val badgeColor = when (badgeText) {
                    "Present" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                    "Medical" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
                    "Absent" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                    "No Class" -> Color(0xFFF1F5F9) to Color(0xFF64748B)
                    else -> MaterialTheme.colorScheme.primary to Color.White
                }
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = badgeColor.first
                ) {
                    Text(
                        text = badgeText.uppercase(Locale.US),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor.second,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = if (isHeld == "Unmarked") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Attendance marking actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val buttonShape = RoundedCornerShape(12.dp)
                Button(
                    onClick = { onMark("Held", "Present") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHeld == "Held" && status == "Present") Color(0xFF4CAF50)
                                         else if (isHeld == "Unmarked") Color.White
                                         else Color(0xFFF1F5F9),
                        contentColor = if (isHeld == "Held" && status == "Present") Color.White
                                       else if (isHeld == "Unmarked") MaterialTheme.colorScheme.primary
                                       else Color(0xFF475569)
                    ),
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = buttonShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Present", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = { onMark("Held", "Absent") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHeld == "Held" && status == "Absent") Color(0xFFFF5252)
                                         else if (isHeld == "Unmarked") Color(0xFFF8FAFF).copy(alpha = 0.6f)
                                         else Color(0xFFF1F5F9),
                        contentColor = if (isHeld == "Held" && status == "Absent") Color.White
                                       else Color(0xFF475569)
                    ),
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = buttonShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Absent", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = { onMark("Held", "Medical") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHeld == "Held" && status == "Medical") Color(0xFF0061A4)
                                         else if (isHeld == "Unmarked") Color(0xFFF8FAFF).copy(alpha = 0.6f)
                                         else Color(0xFFF1F5F9),
                        contentColor = if (isHeld == "Held" && status == "Medical") Color.White
                                       else Color(0xFF475569)
                    ),
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = buttonShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Medical", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = { onMark("Not Held", "Present") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHeld == "Not Held") Color(0xFF64748B)
                                         else if (isHeld == "Unmarked") Color(0xFFF8FAFF).copy(alpha = 0.6f)
                                         else Color(0xFFF1F5F9),
                        contentColor = if (isHeld == "Not Held") Color.White
                                       else Color(0xFF64748B)
                    ),
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = buttonShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("No Class", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TimetableScreen(
    viewModel: MainViewModel,
    modules: List<Module>,
    slots: List<TimetableSlotWithModule>,
    isReadOnly: Boolean
) {
    var showAddModuleDialog by remember { mutableStateOf(false) }
    var showAddSlotDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (!isReadOnly) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showAddModuleDialog = true },
                    modifier = Modifier.weight(1f).testTag("add_module_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Module")
                }
                Button(
                    onClick = { showAddSlotDialog = true },
                    modifier = Modifier.weight(1f).testTag("add_slot_button"),
                    enabled = modules.isNotEmpty()
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Slot")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // List weekly scheduled timetable slots
        if (slots.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No timetable slots added yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                "Weekly Timetable",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val groupedByDay = slots.groupBy { it.slot.dayOfWeek }
                for (day in 1..7) {
                    val daySlots = groupedByDay[day] ?: emptyList()
                    if (daySlots.isNotEmpty()) {
                        item {
                            Text(
                                text = getDayOfWeekLabel(day),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(daySlots) { slotWithModule ->
                            TimetableSlotItem(
                                slotWithModule = slotWithModule,
                                isReadOnly = isReadOnly,
                                onDelete = { viewModel.deleteTimetableSlot(slotWithModule.slot) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddModuleDialog) {
        AddModuleDialog(
            defaultThreshold = viewModel.settingsManager.defaultThreshold,
            onDismiss = { showAddModuleDialog = false },
            onConfirm = { name, threshold ->
                viewModel.addModule(name, threshold)
                showAddModuleDialog = false
            }
        )
    }

    if (showAddSlotDialog) {
        AddSlotDialog(
            modules = modules,
            onDismiss = { showAddSlotDialog = false },
            onConfirm = { moduleId, day, start, end, type ->
                viewModel.addTimetableSlot(moduleId, day, start, end, type)
                showAddSlotDialog = false
            }
        )
    }
}

@Composable
fun TimetableSlotItem(
    slotWithModule: TimetableSlotWithModule,
    isReadOnly: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Vertical Slate color bar to match Clean Minimalism visual identity
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), shape = RoundedCornerShape(100.dp))
                )
                Column {
                    Text(
                        text = slotWithModule.moduleName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = slotWithModule.slot.sessionType.uppercase(Locale.US),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${slotWithModule.slot.startTime} - ${slotWithModule.slot.endTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (!isReadOnly) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun AddModuleDialog(
    defaultThreshold: Float,
    onDismiss: () -> Unit,
    onConfirm: (name: String, threshold: Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var threshold by remember { mutableStateOf(defaultThreshold) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Module") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Module Name") },
                    placeholder = { Text("e.g. COMP101") },
                    modifier = Modifier.fillMaxWidth().testTag("module_name_input")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Attendance Threshold: ${threshold.toInt()}%")
                Slider(
                    value = threshold,
                    onValueChange = { threshold = it },
                    valueRange = 50f..100f,
                    steps = 49
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, threshold)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddSlotDialog(
    modules: List<Module>,
    onDismiss: () -> Unit,
    onConfirm: (moduleId: Int, dayOfWeek: Int, start: String, end: String, type: String) -> Unit
) {
    var selectedModuleId by remember { mutableStateOf(modules.firstOrNull()?.id ?: 0) }
    var selectedDay by remember { mutableStateOf(1) }
    var startHour by remember { mutableStateOf("09") }
    var startMin by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("10") }
    var endMin by remember { mutableStateOf("30") }
    var selectedType by remember { mutableStateOf("Lecture") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Timetable Slot") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Module:")
                modules.forEach { mod ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedModuleId = mod.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedModuleId == mod.id,
                            onClick = { selectedModuleId = mod.id }
                        )
                        Text(mod.name)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Day of the week:")
                val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                var expandedDayDropdown by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { expandedDayDropdown = true }) {
                        Text(days[selectedDay - 1])
                    }
                    DropdownMenu(
                        expanded = expandedDayDropdown,
                        onDismissRequest = { expandedDayDropdown = false }
                    ) {
                        days.forEachIndexed { idx, dayName ->
                            DropdownMenuItem(
                                text = { Text(dayName) },
                                onClick = {
                                    selectedDay = idx + 1
                                    expandedDayDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Session Type:")
                val types = listOf("Lecture", "Tutorial", "LAB")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Start Time (HH:MM):")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startHour,
                        onValueChange = { if (it.length <= 2) startHour = it },
                        modifier = Modifier.width(60.dp),
                        maxLines = 1
                    )
                    Text(":", modifier = Modifier.align(Alignment.CenterVertically))
                    OutlinedTextField(
                        value = startMin,
                        onValueChange = { if (it.length <= 2) startMin = it },
                        modifier = Modifier.width(60.dp),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("End Time (HH:MM):")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = endHour,
                        onValueChange = { if (it.length <= 2) endHour = it },
                        modifier = Modifier.width(60.dp),
                        maxLines = 1
                    )
                    Text(":", modifier = Modifier.align(Alignment.CenterVertically))
                    OutlinedTextField(
                        value = endMin,
                        onValueChange = { if (it.length <= 2) endMin = it },
                        modifier = Modifier.width(60.dp),
                        maxLines = 1
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val start = "${startHour.padStart(2, '0')}:${startMin.padStart(2, '0')}"
                    val end = "${endHour.padStart(2, '0')}:${endMin.padStart(2, '0')}"
                    onConfirm(selectedModuleId, selectedDay, start, end, selectedType)
                }
            ) {
                Text("Add Slot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AnalyticsScreen(moduleStats: List<ModuleAttendanceStats>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Attendance Analytics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (moduleStats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No data available for analytics yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            // Aggregate totals for the Pie Chart
            val totalPresent = moduleStats.sumOf { it.presentCount }
            val totalMedical = moduleStats.sumOf { it.medicalCount }
            val totalAbsent = moduleStats.sumOf { it.absentCount }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Overall Attendance Ratio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AttendancePieChart(
                            present = totalPresent,
                            medical = totalMedical,
                            absent = totalAbsent
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LegendItem(color = MaterialTheme.colorScheme.primary, text = "Present (${totalPresent})")
                            LegendItem(color = Color(0xFF1565C0), text = "Medical (${totalMedical})")
                            LegendItem(color = Color(0xFFE57373), text = "Absent (${totalAbsent})")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Module Dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            moduleStats.forEach { stat ->
                ModuleStatCard(stat = stat)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun AttendancePieChart(present: Int, medical: Int, absent: Int) {
    val total = present + medical + absent
    if (total == 0) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("No Data", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val presentPct = present.toFloat() / total
    val medicalPct = medical.toFloat() / total
    val absentPct = absent.toFloat() / total

    val presentAngle = presentPct * 360f
    val medicalAngle = medicalPct * 360f
    val absentAngle = absentPct * 360f

    val primaryColor = MaterialTheme.colorScheme.primary
    val blueColor = Color(0xFF1565C0)
    val redColor = Color(0xFFE57373)

    Canvas(modifier = Modifier.size(140.dp)) {
        var start = -90f
        drawArc(primaryColor, start, presentAngle, true)
        start += presentAngle
        drawArc(blueColor, start, medicalAngle, true)
        start += medicalAngle
        drawArc(redColor, start, absentAngle, true)
    }
}

@Composable
fun ModuleStatCard(stat: ModuleAttendanceStats) {
    val meets = stat.meetsThreshold
    val diff = stat.percentage - stat.module.attendanceThreshold
    
    // Color coding thresholds matching Clean Minimalism exactly
    val colorCode = when {
        meets && diff >= 5f -> Color(0xFF4CAF50) // Healthy Green (Safe)
        meets -> Color(0xFFFFC107) // Yellow / Orange (At Risk)
        else -> Color(0xFFFF5252) // Red (Warning)
    }

    val statusText = when {
        meets && diff >= 5f -> "Safe"
        meets -> "At Risk"
        else -> "Warning"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Vertical Status Indicator Bar matching the HTML's minimalist visual accent
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(40.dp)
                            .background(color = colorCode, shape = RoundedCornerShape(100.dp))
                    )
                    Column {
                        Text(
                            text = stat.module.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${stat.presentCount}/${stat.heldCount} sessions held",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", stat.percentage)}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorCode
                    )
                    Text(
                        text = statusText.uppercase(Locale.US),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attendance ratio progress indicator
            LinearProgressIndicator(
                progress = { (stat.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = colorCode,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Present: ${stat.presentCount} | Medical: ${stat.medicalCount} | Absent: ${stat.absentCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!meets) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "BELOW THRESHOLD",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    semesters: List<Semester>,
    selectedSemesterId: Int?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // SAF File save triggers
    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        viewModel.exportPDF(os)
                    }
                    Toast.makeText(context, "PDF Report exported successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "PDF export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        viewModel.exportCSV(os)
                    }
                    Toast.makeText(context, "CSV Report exported successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "CSV export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Export Reports",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Semester for Report:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                var dropdownExpanded by remember { mutableStateOf(false) }
                val currentSem = semesters.find { it.id == selectedSemesterId }
                Box {
                    OutlinedButton(
                        onClick = { dropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(currentSem?.name ?: "Select Semester")
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        semesters.forEach { sem ->
                            DropdownMenuItem(
                                text = { Text(sem.name) },
                                onClick = {
                                    viewModel.selectSemester(sem.id)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { pdfExportLauncher.launch("Attendance_Report_${currentSem?.name ?: "Report"}.pdf") },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_pdf_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export PDF Report")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { csvExportLauncher.launch("Attendance_Report_${currentSem?.name ?: "Report"}.csv") },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_csv_button")
                ) {
                    Icon(Icons.Default.TableView, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export CSV / Excel Report")
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var notificationsEnabled by remember { mutableStateOf(viewModel.settingsManager.notificationsEnabled) }
    var defaultThreshold by remember { mutableStateOf(viewModel.settingsManager.defaultThreshold) }

    // SAF Import/Export triggers
    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        val success = viewModel.exportBackup(os)
                        if (success) {
                            Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Backup failed.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { isStream ->
                        val success = viewModel.importBackup(isStream)
                        if (success) {
                            Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Data restore failed.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Data restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings & Management",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Notification Trigger Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Class Reminders", fontWeight = FontWeight.Medium)
                        Text(
                            "Notify 30 mins after session ends to mark attendance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = {
                            notificationsEnabled = it
                            viewModel.settingsManager.notificationsEnabled = it
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Default Threshold Selector
                Text(
                    text = "Default Attendance Threshold: ${defaultThreshold.toInt()}%",
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = defaultThreshold,
                    onValueChange = {
                        defaultThreshold = it
                        viewModel.settingsManager.defaultThreshold = it
                    },
                    valueRange = 50f..100f,
                    steps = 49
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Backup and Restore Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Local Storage Backup & Restore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "All data is offline on this device. Export JSON files as local backups to keep data safe or restore after resets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = { backupExportLauncher.launch("UniLeca_Backup.json") },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("backup_export_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup Data to File")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { backupImportLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("backup_import_button")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Data from Backup")
                }
            }
        }
    }
}

// Global Calculation & Date Helpers
fun getDayOfWeekForDate(dateString: String): Int {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateString) ?: return 1
        val cal = Calendar.getInstance()
        cal.time = date
        // Map Calendar DAY_OF_WEEK (Sun=1, Mon=2, Sat=7) to our DayOfWeek (Mon=1, Sun=7)
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    } catch (e: Exception) {
        1
    }
}

fun getDayOfWeekLabel(day: Int): String {
    return when (day) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> "Monday"
    }
}

fun adjustDateString(dateString: String, daysToAdjust: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateString) ?: return dateString
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DAY_OF_MONTH, daysToAdjust)
        sdf.format(cal.time)
    } catch (e: Exception) {
        dateString
    }
}

fun getFormattedSelectedDate(dateString: String): String {
    return try {
        val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputSdf = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        val date = inputSdf.parse(dateString) ?: return dateString
        outputSdf.format(date)
    } catch (e: Exception) {
        dateString
    }
}
