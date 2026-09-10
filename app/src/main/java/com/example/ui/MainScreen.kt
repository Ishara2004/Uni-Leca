package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AddSemesterDialog
import com.example.ui.components.ConfirmDialog
import com.example.ui.screens.*
import com.example.ui.viewmodel.MainViewModel
import com.ishara.unileca.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val semesters by viewModel.allSemesters.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val selectedSemester by viewModel.selectedSemester.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAddSemester by remember { mutableStateOf(false) }
    var showEndSemester by remember { mutableStateOf(false) }
    var showNotificationRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.settingsManager.notificationsEnabled = granted
        viewModel.settingsManager.notificationRationaleSeen = true
    }

    LaunchedEffect(activeSemester?.id) {
        if (activeSemester != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !viewModel.settingsManager.notificationRationaleSeen &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            showNotificationRationale = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(navController) {
        viewModel.navigation.collect { route ->
            navController.navigate(route) { launchSingleTop = true }
        }
    }

    if (semesters.isEmpty()) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            WelcomeScreen(Modifier.padding(padding)) { showAddSemester = true }
        }
    } else {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: Routes.TODAY
        val bottomRoutes = listOf(
            NavItem(Routes.TODAY, R.string.today),
            NavItem(Routes.TIMETABLE, R.string.timetable),
            NavItem(Routes.ANALYTICS, R.string.analytics),
            NavItem(Routes.REPORTS, R.string.reports),
            NavItem(Routes.SETTINGS, R.string.settings)
        )

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                            Text(selectedSemester?.name ?: stringResource(R.string.no_semester), style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddSemester = true }) {
                            Icon(Icons.Default.Add, stringResource(R.string.new_semester))
                        }
                        if (selectedSemester?.id == activeSemester?.id && activeSemester != null) {
                            TextButton(onClick = { showEndSemester = true }) {
                                Text(stringResource(R.string.end_semester))
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    bottomRoutes.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.TODAY) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(item.labelSymbol()) },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.TODAY,
                modifier = Modifier.padding(padding)
            ) {
                composable(Routes.TODAY) { TodayScreen(viewModel) }
                composable(Routes.TIMETABLE) { TimetableScreen(viewModel) }
                composable(Routes.ANALYTICS) { AnalyticsScreen(viewModel) }
                composable(Routes.REPORTS) { ReportsScreen(viewModel) }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onOpenSemesterHistory = { navController.navigate(Routes.SEMESTER_HISTORY) },
                        onOpenAttendanceHistory = { navController.navigate(Routes.ATTENDANCE_HISTORY) }
                    )
                }
                composable(Routes.SEMESTER_HISTORY) {
                    SemesterHistoryScreen(viewModel) { navController.navigate(Routes.ANALYTICS) }
                }
                composable(Routes.ATTENDANCE_HISTORY) {
                    AttendanceHistoryScreen(viewModel) { date ->
                        viewModel.changeSelectedDate(date)
                        navController.navigate(Routes.TODAY) { launchSingleTop = true }
                    }
                }
            }
        }
    }

    if (showAddSemester) {
        AddSemesterDialog(
            semesters = semesters,
            onDismiss = { showAddSemester = false },
            onConfirm = { name, source ->
                viewModel.createSemester(name, source)
                showAddSemester = false
            }
        )
    }

    if (showEndSemester) {
        ConfirmDialog(
            title = stringResource(R.string.end_semester),
            message = stringResource(R.string.end_semester_confirm),
            confirmLabel = stringResource(R.string.end_semester),
            destructive = true,
            onDismiss = { showEndSemester = false },
            onConfirm = { viewModel.endCurrentSemester(); showEndSemester = false }
        )
    }

    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.enable_notifications_title)) },
            text = { Text(stringResource(R.string.enable_notifications_body)) },
            confirmButton = {
                Button(onClick = {
                    showNotificationRationale = false
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.enable)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotificationRationale = false
                    viewModel.settingsManager.notificationsEnabled = false
                    viewModel.settingsManager.notificationRationaleSeen = true
                }) { Text(stringResource(R.string.not_now)) }
            }
        )
    }
}

@Composable
private fun WelcomeScreen(modifier: Modifier, onCreate: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.welcome_body), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_first_semester)) }
    }
}

private data class NavItem(val route: String, val labelRes: Int) {
    fun labelSymbol(): String = when (route) {
        Routes.TODAY -> "●"
        Routes.TIMETABLE -> "▦"
        Routes.ANALYTICS -> "◔"
        Routes.REPORTS -> "▤"
        else -> "⚙"
    }
}

private object Routes {
    const val TODAY = "today"
    const val TIMETABLE = "timetable"
    const val ANALYTICS = "analytics"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val SEMESTER_HISTORY = "semester-history"
    const val ATTENDANCE_HISTORY = "attendance-history"
}
