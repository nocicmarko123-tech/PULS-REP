package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ExerciseItem
import com.example.domain.ExerciseProgressStats
import com.example.domain.ExerciseUnit
import com.example.domain.SpeedLevel
import com.example.ui.WorkoutUiState
import com.example.ui.components.AddCustomExerciseDialog
import com.example.ui.components.AthleticCard
import com.example.ui.components.EditNextTaskDialog
import com.example.ui.components.PersistentReminderBanner
import com.example.ui.components.QuickCustomAmountDialog
import com.example.ui.components.RestTimerCard
import com.example.ui.components.SpeedLevelSelector
import com.example.ui.components.formatHmsCountdown
import com.example.ui.theme.BgPureWhite
import com.example.ui.theme.BorderIceBlue
import com.example.ui.theme.BrightBlue
import com.example.ui.theme.CardIceBlue
import com.example.ui.theme.MonoCounterSmall
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueTint
import com.example.ui.theme.SubtleDeleteRed
import com.example.ui.theme.TealBorder
import com.example.ui.theme.TealGreen
import com.example.ui.theme.TealSoftTint
import com.example.ui.theme.TextOnSolidWhite
import com.example.ui.theme.TextPrimaryNavy
import com.example.ui.theme.TextSecondarySlate

@Composable
fun TrainingScreen(
    uiState: WorkoutUiState,
    onSelectSpeedLevel: (SpeedLevel) -> Unit,
    onSetPlanWeek: (Int) -> Unit,
    onCompleteNextStep: () -> Unit,
    onEditNextTask: (ExerciseItem?, String, Int) -> Unit,
    onStartRestDefault: () -> Unit,
    onTogglePauseRest: () -> Unit,
    onAdjustRestSeconds: (Int) -> Unit,
    onResetRest: () -> Unit,
    onSkipRest: () -> Unit,
    onDismissRestAlert: () -> Unit,
    onDismissSteadyAlert: () -> Unit,
    onQuickLog: (ExerciseItem, Int, Boolean) -> Unit,
    onSetExerciseAsNext: (ExerciseItem) -> Unit,
    onAddCustomExercise: (String, String, ExerciseUnit, Int) -> Unit,
    onDeleteCustomExercise: (Long) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditNextDialog by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var customAmountExercise by remember { mutableStateOf<ExerciseItem?>(null) }

    val statsByExerciseId = remember(uiState.exerciseStats) {
        uiState.exerciseStats.associateBy { it.exercise.id }
    }
    val planWeek = uiState.settings.planWeek

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("training_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Minimalist Top Header
        item(key = "minimal_header") {
            MinimalTrainingHeader(
                streakDays = uiState.streakDays,
                todayReps = uiState.todayReps,
                todaySeconds = uiState.todaySeconds
            )
        }

        // 2. Persistent In-App Reminder / Rest Alert
        item(key = "persistent_banner") {
            PersistentReminderBanner(
                nextTask = uiState.nextTask,
                speedLevel = uiState.settings.speedLevel,
                restTimer = uiState.restTimer,
                showSteadyBanner = uiState.settings.persistentBannerEnabled,
                steadyReminderActive = uiState.settings.steadyReminderActive,
                secondsUntilNextReminder = uiState.secondsUntilNextReminder,
                steadyReminderAlertVisible = uiState.steadyReminderAlertVisible,
                onDismissCompletedAlert = onDismissRestAlert,
                onDismissSteadyAlert = onDismissSteadyAlert,
                onQuickComplete = {
                    onCompleteNextStep()
                    onShowMessage("Serija zabeležena • pauza pokrenuta")
                }
            )
        }

        // 3. Main Reminder Card ("Sledeće")
        item(key = "next_step_hero_card") {
            MinimalNextStepCard(
                uiState = uiState,
                onCompleteNextStep = {
                    onCompleteNextStep()
                    onShowMessage(
                        "Urađeno • pauza ${uiState.settings.speedLevel.defaultRestSeconds}s"
                    )
                },
                onOpenEditNext = { showEditNextDialog = true }
            )
        }

        // 4. Rest Timer
        item(key = "rest_timer_card") {
            RestTimerCard(
                restTimer = uiState.restTimer,
                defaultRestSeconds = uiState.settings.speedLevel.defaultRestSeconds,
                nextTask = uiState.nextTask,
                onStartDefault = onStartRestDefault,
                onTogglePause = onTogglePauseRest,
                onAdjustSeconds = onAdjustRestSeconds,
                onReset = onResetRest,
                onSkip = onSkipRest
            )
        }

        // 5. Speed Level Selector (3 levels: Sporo 75s / Srednje 45s / Brzo 25s)
        item(key = "speed_selector") {
            SpeedLevelSelector(
                selectedLevel = uiState.settings.speedLevel,
                onSelectLevel = { level ->
                    onSelectSpeedLevel(level)
                    onShowMessage("${level.bolts} ${level.titleSr} (${level.defaultRestSeconds}s pauza)")
                }
            )
        }

        // 6. Custom Progressive Plan Header + Week Stepper
        item(key = "plan_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "💪 Prilagođen plan",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryNavy
                        )
                        Text(
                            text = "Kraće serije i nedeljni napredak",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySlate
                        )
                    }

                    OutlinedButton(
                        onClick = { showAddExerciseDialog = true },
                        border = BorderStroke(1.dp, BorderIceBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_custom_exercise_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Dodaj vežbu",
                            tint = RoyalBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dodaj",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoyalBlue
                        )
                    }
                }

                // Week Progression Stepper
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CardIceBlue,
                    border = BorderStroke(1.dp, BorderIceBlue)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (planWeek > 1) onSetPlanWeek(planWeek - 1) },
                            enabled = planWeek > 1,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("plan_week_prev")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Prethodna nedelja",
                                tint = if (planWeek > 1) RoyalBlue else TextSecondarySlate.copy(alpha = 0.4f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Nedelja $planWeek",
                                style = MaterialTheme.typography.titleSmall,
                                color = RoyalBlue,
                                modifier = Modifier.testTag("plan_week_label")
                            )
                            Text(
                                text = "Ciljevi po seriji za ovu nedelju",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondarySlate
                            )
                        }

                        IconButton(
                            onClick = { onSetPlanWeek(planWeek + 1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("plan_week_next")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Sledeća nedelja",
                                tint = RoyalBlue
                            )
                        }
                    }
                }
            }
        }

        // 7. Plan Exercise Blocks (Blok 1..6 + custom)
        itemsIndexed(
            items = uiState.exercises,
            key = { _, ex -> ex.id }
        ) { index, exercise ->
            val stats = statsByExerciseId[exercise.id]
            PlanExerciseBlockCard(
                blockNumber = index + 1,
                exercise = exercise,
                planWeek = planWeek,
                stats = stats,
                isCurrentlyNext = uiState.nextTask.exerciseId == exercise.id &&
                    uiState.nextTask.customText.isBlank(),
                onQuickAdd = { amount ->
                    onQuickLog(exercise, amount, false)
                    val unitLabel = if (exercise.unit == ExerciseUnit.SECONDS) "s" else ""
                    onShowMessage("+${amount}$unitLabel ${exercise.name}")
                },
                onOpenCustomAmount = { customAmountExercise = exercise },
                onSetAsNext = {
                    onSetExerciseAsNext(exercise)
                    onShowMessage("🔔 Sledeće: ${exercise.name}")
                },
                onDeleteCustom = if (exercise.isCustom) {
                    { onDeleteCustomExercise(exercise.id) }
                } else null
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showEditNextDialog) {
        EditNextTaskDialog(
            exercises = uiState.exercises,
            currentNextTask = uiState.nextTask,
            onDismiss = { showEditNextDialog = false },
            onConfirm = { exercise, customText, amount ->
                onEditNextTask(exercise, customText, amount)
                showEditNextDialog = false
                onShowMessage("🔔 Podsetnik ažuriran")
            }
        )
    }

    if (showAddExerciseDialog) {
        AddCustomExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onConfirm = { name, emoji, unit, defaultAmount ->
                onAddCustomExercise(name, emoji, unit, defaultAmount)
                showAddExerciseDialog = false
                onShowMessage("Dodata vežba: $name")
            }
        )
    }

    customAmountExercise?.let { exercise ->
        QuickCustomAmountDialog(
            exercise = exercise,
            onDismiss = { customAmountExercise = null },
            onLog = { amount, startRest ->
                onQuickLog(exercise, amount, startRest)
                customAmountExercise = null
                onShowMessage("${exercise.name}: $amount upisano")
            }
        )
    }
}

@Composable
private fun MinimalTrainingHeader(
    streakDays: Int,
    todayReps: Int,
    todaySeconds: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RepPuls",
                    style = MaterialTheme.typography.headlineLarge,
                    color = RoyalBlue
                )
                Text(
                    text = "Trening sopstvenom težinom • 100% besplatno",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySlate
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CardIceBlue,
                border = BorderStroke(1.dp, BorderIceBlue)
            ) {
                Text(
                    text = "🔥 $streakDays ${if (streakDays == 1) "dan" else "dana"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimaryNavy,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = CardIceBlue,
                border = BorderStroke(1.dp, BorderIceBlue)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💪 Danas",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySlate
                    )
                    Text(
                        text = "$todayReps pon.",
                        style = MonoCounterSmall,
                        color = RoyalBlue
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = CardIceBlue,
                border = BorderStroke(1.dp, BorderIceBlue)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ Izdržaj",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySlate
                    )
                    Text(
                        text = "${todaySeconds}s",
                        style = MonoCounterSmall,
                        color = TealGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalNextStepCard(
    uiState: WorkoutUiState,
    onCompleteNextStep: () -> Unit,
    onOpenEditNext: () -> Unit
) {
    val speed = uiState.settings.speedLevel
    val nextTask = uiState.nextTask

    AthleticCard(
        modifier = Modifier.testTag("next_step_hero_card"),
        borderColor = BrightBlue.copy(alpha = 0.65f),
        containerColor = BgPureWhite
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔔 SLEDEĆE",
                style = MaterialTheme.typography.labelMedium,
                color = RoyalBlue
            )

            IconButton(
                onClick = onOpenEditNext,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("edit_next_task_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Izmeni sledeći zadatak",
                    tint = RoyalBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = nextTask.formatHeadline(speed),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimaryNavy,
            modifier = Modifier.testTag("hero_next_task_title")
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${speed.tempoDescriptionSr} • pauza ${speed.defaultRestSeconds}s",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondarySlate
        )

        if (uiState.settings.steadyReminderActive && uiState.secondsUntilNextReminder > 0L) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "⏱️ Sledeći stalni podsetnik za: ${formatHmsCountdown(uiState.secondsUntilNextReminder)}",
                style = MaterialTheme.typography.labelSmall,
                color = RoyalBlue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCompleteNextStep,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("done_next_step_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = TealGreen,
                contentColor = TextOnSolidWhite
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Urađeno",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PlanExerciseBlockCard(
    blockNumber: Int,
    exercise: ExerciseItem,
    planWeek: Int,
    stats: ExerciseProgressStats?,
    isCurrentlyNext: Boolean,
    onQuickAdd: (Int) -> Unit,
    onOpenCustomAmount: () -> Unit,
    onSetAsNext: () -> Unit,
    onDeleteCustom: (() -> Unit)?
) {
    val weekTarget = exercise.targetForWeek(planWeek)
    val unitSuffix = if (exercise.unit == ExerciseUnit.SECONDS) "s" else ""
    val quickOptions = if (exercise.unit == ExerciseUnit.SECONDS) {
        listOf(15, weekTarget, 45).distinct().sorted()
    } else {
        listOf(5, 10, weekTarget).distinct().sorted()
    }

    AthleticCard(
        modifier = Modifier.testTag("exercise_card_${exercise.id}"),
        borderColor = if (isCurrentlyNext) RoyalBlue else BorderIceBlue,
        containerColor = CardIceBlue,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Blok $blockNumber • ${exercise.emoji} ${exercise.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimaryNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Cilj: ${exercise.formatAmountLabel(weekTarget)} • ${exercise.formatWeeklyProgression()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySlate
                )
                Text(
                    text = "Danas: ${stats?.todayTotal ?: 0}$unitSuffix",
                    style = MaterialTheme.typography.labelSmall,
                    color = TealGreen
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrentlyNext) RoyalBlue else BgPureWhite,
                    border = BorderStroke(1.dp, if (isCurrentlyNext) RoyalBlue else BorderIceBlue),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSetAsNext)
                        .minimumInteractiveComponentSize()
                        .testTag("pin_next_${exercise.id}")
                ) {
                    Text(
                        text = if (isCurrentlyNext) "Aktivno" else "Sledeće",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentlyNext) TextOnSolidWhite else RoyalBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                if (onDeleteCustom != null) {
                    IconButton(
                        onClick = onDeleteCustom,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Obriši vežbu",
                            tint = SubtleDeleteRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickOptions.forEach { amount ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onQuickAdd(amount) }
                        .minimumInteractiveComponentSize()
                        .testTag("quick_add_${exercise.id}_$amount"),
                    shape = RoundedCornerShape(8.dp),
                    color = TealSoftTint,
                    border = BorderStroke(1.dp, TealBorder)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$amount$unitSuffix",
                            style = MonoCounterSmall,
                            color = TealGreen
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenCustomAmount)
                    .minimumInteractiveComponentSize()
                    .testTag("quick_custom_${exercise.id}"),
                shape = RoundedCornerShape(8.dp),
                color = BgPureWhite,
                border = BorderStroke(1.dp, BorderIceBlue)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unesi",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoyalBlue
                    )
                }
            }
        }
    }
}
