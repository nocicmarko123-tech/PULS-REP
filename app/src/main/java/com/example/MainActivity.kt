package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.WorkoutViewModel
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.TrainingScreen
import com.example.ui.theme.BgPureWhite
import com.example.ui.theme.BorderIceBlue
import com.example.ui.theme.RepPulsTheme
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueTint
import com.example.ui.theme.TealGreen
import com.example.ui.theme.TextOnSolidWhite
import com.example.ui.theme.TextPrimaryNavy
import com.example.ui.theme.TextSecondarySlate
import kotlinx.coroutines.launch

object RepPulsRoutes {
    const val TRAINING = "trening"
    const val PROGRESS = "progres"
    const val REMINDERS = "podsetnici"
}

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navDestinations = listOf(
    NavDestinationItem(
        route = RepPulsRoutes.TRAINING,
        label = "Trening",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter,
        testTag = "nav_tab_training"
    ),
    NavDestinationItem(
        route = RepPulsRoutes.PROGRESS,
        label = "Progres",
        selectedIcon = Icons.Filled.TrendingUp,
        unselectedIcon = Icons.Outlined.TrendingUp,
        testTag = "nav_tab_progress"
    ),
    NavDestinationItem(
        route = RepPulsRoutes.REMINDERS,
        label = "Podsetnici",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        testTag = "nav_tab_reminders"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as RepPulsApp
        setContent {
            RepPulsTheme {
                val workoutViewModel: WorkoutViewModel = viewModel(
                    factory = WorkoutViewModel.Factory(
                        repository = app.repository,
                        notificationHelper = app.notificationHelper,
                        soundHelper = app.soundHelper
                    )
                )
                RepPulsRootScreen(viewModel = workoutViewModel)
            }
        }
    }
}

@Composable
fun RepPulsRootScreen(
    viewModel: WorkoutViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val showMessage: (String) -> Unit = remember(snackbarHostState, coroutineScope) {
        { message ->
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val navigateToTab: (String) -> Unit = remember(navController) {
        { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPureWhite)
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = BgPureWhite,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = TextPrimaryNavy,
                        contentColor = TextOnSolidWhite,
                        actionColor = TealGreen
                    )
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    Box {
                        HorizontalDivider(
                            color = BorderIceBlue,
                            thickness = 1.dp,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                        NavigationBar(
                            containerColor = BgPureWhite,
                            tonalElevation = 0.dp
                        ) {
                            navDestinations.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == item.route
                                } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = RoyalBlue,
                                        selectedTextColor = RoyalBlue,
                                        indicatorColor = SoftBlueTint,
                                        unselectedIconColor = TextSecondarySlate,
                                        unselectedTextColor = TextSecondarySlate
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = BgPureWhite,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navDestinations.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = RoyalBlue,
                                    selectedTextColor = RoyalBlue,
                                    indicatorColor = SoftBlueTint,
                                    unselectedIconColor = TextSecondarySlate,
                                    unselectedTextColor = TextSecondarySlate
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                    VerticalDivider(color = BorderIceBlue, thickness = 1.dp)
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = RepPulsRoutes.TRAINING,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                    ) {
                        composable(RepPulsRoutes.TRAINING) {
                            TrainingScreen(
                                uiState = uiState,
                                onSelectSpeedLevel = viewModel::selectSpeedLevel,
                                onSetPlanWeek = viewModel::setPlanWeek,
                                onCompleteNextStep = viewModel::completeNextStep,
                                onEditNextTask = viewModel::setCustomNextTask,
                                onStartRestDefault = {
                                    viewModel.startRestTimer(uiState.settings.speedLevel.defaultRestSeconds)
                                },
                                onTogglePauseRest = viewModel::togglePauseRestTimer,
                                onAdjustRestSeconds = viewModel::adjustRestTimer,
                                onResetRest = viewModel::resetRestTimer,
                                onSkipRest = viewModel::skipRestTimer,
                                onDismissRestAlert = viewModel::dismissRestCompletedAlert,
                                onDismissSteadyAlert = viewModel::dismissSteadyReminderAlert,
                                onQuickLog = viewModel::quickLogExercise,
                                onSetExerciseAsNext = { exercise ->
                                    viewModel.setExerciseAsNextForWeek(exercise)
                                },
                                onAddCustomExercise = viewModel::addCustomExercise,
                                onDeleteCustomExercise = viewModel::deleteCustomExercise,
                                onShowMessage = showMessage
                            )
                        }

                        composable(RepPulsRoutes.PROGRESS) {
                            ProgressScreen(
                                uiState = uiState,
                                onUpdateDailyGoal = { newGoal ->
                                    viewModel.updateDailyGoal(newGoal)
                                    showMessage("Dnevni cilj: $newGoal ponavljanja")
                                },
                                onUpdateProfile = { age, height, weight ->
                                    viewModel.updatePersonalProfile(age, height, weight)
                                    showMessage("Profil sačuvan ($age god, $height cm, $weight kg)")
                                },
                                onDeleteLog = { entry ->
                                    viewModel.deleteLog(entry)
                                    coroutineScope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val res = snackbarHostState.showSnackbar(
                                            message = "Unos obrisan (${entry.exerciseName})",
                                            actionLabel = "Vrati",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreLog(entry)
                                        }
                                    }
                                },
                                onClearAllHistory = {
                                    viewModel.clearAllHistory()
                                    showMessage("Istorija je obrisana")
                                }
                            )
                        }

                        composable(RepPulsRoutes.REMINDERS) {
                            RemindersScreen(
                                uiState = uiState,
                                onSetNextTask = viewModel::setCustomNextTask,
                                onAddToQueue = viewModel::addToQueue,
                                onRemoveFromQueue = viewModel::removeFromQueue,
                                onPromoteFromQueue = viewModel::promoteFromQueue,
                                onSetSoundEnabled = viewModel::setSoundEnabled,
                                onSetVibrationEnabled = viewModel::setVibrationEnabled,
                                onSetPersistentBannerEnabled = viewModel::setPersistentBannerEnabled,
                                onSetNotificationsEnabled = viewModel::setNotificationsEnabled,
                                onToggleSteadyReminder = viewModel::toggleSteadyReminder,
                                onSetSteadyReminderInterval = viewModel::setSteadyReminderInterval,
                                onRefreshPermission = viewModel::refreshNotificationPermission,
                                onSendTestNotification = viewModel::sendTestNotification,
                                onShowMessage = showMessage
                            )
                        }
                    }
                }
            }
        }
    }
}
