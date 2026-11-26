package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DayHistoryGroup
import com.example.domain.ExerciseProgressStats
import com.example.domain.ExerciseUnit
import com.example.domain.PersonalHealthAdvisor
import com.example.domain.PersonalProfile
import com.example.domain.WorkoutLogItem
import com.example.ui.WorkoutUiState
import com.example.ui.components.AthleticCard
import com.example.ui.components.repPulsTextFieldColors
import com.example.ui.theme.BgPureWhite
import com.example.ui.theme.BorderIceBlue
import com.example.ui.theme.BrightBlue
import com.example.ui.theme.CardIceBlue
import com.example.ui.theme.MonoCounterMedium
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressScreen(
    uiState: WorkoutUiState,
    onUpdateDailyGoal: (Int) -> Unit,
    onUpdateProfile: (ageYears: Int, heightCm: Int, weightKg: Int) -> Unit,
    onDeleteLog: (WorkoutLogItem) -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showHealthTips by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val profile = uiState.personalProfile
    val principles = remember(profile) { PersonalHealthAdvisor.buildPrinciples(profile) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("progress_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "progress_header") {
            Column {
                Text(
                    text = "📈 Progres",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimaryNavy
                )
                Text(
                    text = "Dnevni učinak, niz dana i istorija",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySlate
                )
            }
        }

        // Streak & Today vs All-Time Summary
        item(key = "summary_metrics") {
            SummaryMetricsCard(
                uiState = uiState,
                onEditDailyGoal = { showGoalDialog = true }
            )
        }

        // Concise Personal Profile & Health Guidance Card
        item(key = "personal_health_card") {
            AthleticCard(
                borderColor = BorderIceBlue,
                containerColor = CardIceBlue
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lični profil i zdrav razvoj",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimaryNavy
                        )
                        Text(
                            text = "${profile.ageYears} god • ${profile.heightCm} cm • ${profile.weightKg} kg",
                            style = MonoCounterSmall,
                            color = RoyalBlue
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BgPureWhite,
                        border = BorderStroke(1.dp, BorderIceBlue),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showProfileDialog = true }
                            .minimumInteractiveComponentSize()
                            .testTag("edit_profile_button")
                    ) {
                        Text(
                            text = "Izmeni",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = profile.growthSummarySr,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySlate
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BgPureWhite,
                    border = BorderStroke(1.dp, BorderIceBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showHealthTips = !showHealthTips }
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_health_tips_button")
                ) {
                    Text(
                        text = if (showHealthTips) "Sakrij zdravstvene smernice" else "Prikaži zdravstvene smernice (${principles.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = TealGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                if (showHealthTips) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = profile.bmiNoteSr,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryNavy
                        )
                        principles.forEach { p ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BgPureWhite,
                                border = BorderStroke(1.dp, BorderIceBlue)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "${p.title} (${p.subtitle})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = RoyalBlue
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = p.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondarySlate
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Per-Exercise Records & Weekly Plan Progress
        item(key = "personal_records_header") {
            Text(
                text = "💪 Pregled po vežbama (Nedelja ${uiState.settings.planWeek})",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryNavy
            )
        }

        items(
            items = uiState.exerciseStats,
            key = { "pr_${it.exercise.id}" }
        ) { stats ->
            ExercisePersonalRecordRow(
                stats = stats,
                planWeek = uiState.settings.planWeek
            )
        }

        // Day-by-Day History
        item(key = "history_header") {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 Kratka istorija",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryNavy
                )
                if (uiState.historyGroups.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearHistoryDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_all_history_button")
                    ) {
                        Text(
                            text = "Obriši sve",
                            style = MaterialTheme.typography.labelMedium,
                            color = SubtleDeleteRed
                        )
                    }
                }
            }
        }

        if (uiState.historyGroups.isEmpty()) {
            item(key = "empty_history") {
                AthleticCard(
                    modifier = Modifier.testTag("empty_history_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Još nema unosa",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimaryNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Potvrdi prvu seriju na ekranu Trening da vidiš napredak i niz dana.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySlate
                        )
                    }
                }
            }
        } else {
            items(
                items = uiState.historyGroups,
                key = { "day_${it.dateKey}" }
            ) { dayGroup ->
                DayHistoryCard(
                    group = dayGroup,
                    onDeleteEntry = onDeleteLog
                )
            }
        }

        item(key = "progress_bottom_spacer") {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showGoalDialog) {
        EditDailyGoalDialog(
            currentGoal = uiState.settings.dailyGoalReps,
            onDismiss = { showGoalDialog = false },
            onSave = { newGoal ->
                onUpdateDailyGoal(newGoal)
                showGoalDialog = false
            }
        )
    }

    if (showProfileDialog) {
        EditProfileMinimalDialog(
            profile = profile,
            onDismiss = { showProfileDialog = false },
            onSave = { age, height, weight ->
                onUpdateProfile(age, height, weight)
                showProfileDialog = false
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = BgPureWhite,
            titleContentColor = TextPrimaryNavy,
            textContentColor = TextPrimaryNavy,
            title = { Text("Obriši celu istoriju?") },
            text = {
                Text(
                    text = "Svi zabeleženi unosi i dnevni zbirovi biće trajno uklonjeni sa ovog uređaja.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondarySlate
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryDialog = false
                        onClearAllHistory()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SubtleDeleteRed,
                        contentColor = TextOnSolidWhite
                    ),
                    modifier = Modifier.testTag("confirm_clear_all_history_button")
                ) {
                    Text("Obriši sve")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Otkaži", color = TextSecondarySlate)
                }
            }
        )
    }
}

@Composable
private fun SummaryMetricsCard(
    uiState: WorkoutUiState,
    onEditDailyGoal: () -> Unit
) {
    val goal = uiState.settings.dailyGoalReps.coerceAtLeast(1)
    val goalProgress = (uiState.todayReps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val goalReached = uiState.todayReps >= goal

    AthleticCard(
        borderColor = if (goalReached) TealBorder else BorderIceBlue
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricBox(
                label = "🔥 Niz dana",
                value = "${uiState.streakDays}",
                accentColor = RoyalBlue,
                containerColor = BgPureWhite,
                borderColor = BorderIceBlue,
                modifier = Modifier.weight(1f)
            )
            MetricBox(
                label = "💪 Danas pon.",
                value = "${uiState.todayReps}",
                accentColor = TealGreen,
                containerColor = TealSoftTint,
                borderColor = TealBorder,
                modifier = Modifier.weight(1f)
            )
            MetricBox(
                label = "⏱️ Izdržaj",
                value = "${uiState.todaySeconds}s",
                accentColor = BrightBlue,
                containerColor = BgPureWhite,
                borderColor = BorderIceBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (goalReached) {
                    "Dnevni cilj ispunjen (${uiState.todayReps}/$goal)"
                } else {
                    "Dnevni cilj: ${uiState.todayReps} / $goal pon."
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (goalReached) TealGreen else TextPrimaryNavy
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BgPureWhite,
                border = BorderStroke(1.dp, BorderIceBlue),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onEditDailyGoal)
                    .minimumInteractiveComponentSize()
                    .testTag("edit_daily_goal_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Izmeni dnevni cilj",
                        tint = RoyalBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cilj",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoyalBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { goalProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (goalReached) TealGreen else RoyalBlue,
            trackColor = SoftBlueTint
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Ukupno zabeleženo:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondarySlate
            )
            Text(
                text = "${uiState.allTimeReps} pon. • ${uiState.allTimeSeconds}s",
                style = MonoCounterSmall,
                color = TextPrimaryNavy
            )
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondarySlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MonoCounterMedium,
                color = accentColor
            )
        }
    }
}

@Composable
private fun ExercisePersonalRecordRow(
    stats: ExerciseProgressStats,
    planWeek: Int
) {
    val unitSuffix = if (stats.exercise.unit == ExerciseUnit.SECONDS) "s" else ""
    val weekTarget = stats.exercise.targetForWeek(planWeek)

    AthleticCard(
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${stats.exercise.emoji} ${stats.exercise.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimaryNavy
                )
                Text(
                    text = "Cilj nedelje: ${stats.exercise.formatAmountLabel(weekTarget)} • Ukupno: ${stats.allTimeTotal}$unitSuffix",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySlate
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TealSoftTint,
                border = BorderStroke(1.dp, TealBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rekord",
                        style = MaterialTheme.typography.labelSmall,
                        color = TealGreen
                    )
                    Text(
                        text = "${stats.personalRecord}$unitSuffix",
                        style = MonoCounterSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryNavy
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHistoryCard(
    group: DayHistoryGroup,
    onDeleteEntry: (WorkoutLogItem) -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    AthleticCard(
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.displayTitle,
                style = MaterialTheme.typography.titleSmall,
                color = RoyalBlue
            )
            Text(
                text = buildString {
                    if (group.totalReps > 0) append("${group.totalReps} pon.")
                    if (group.totalReps > 0 && group.totalSeconds > 0) append(" • ")
                    if (group.totalSeconds > 0) append("${group.totalSeconds}s")
                },
                style = MonoCounterSmall,
                color = TealGreen
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.entries.forEach { entry ->
                val unitLabel = if (entry.unit == ExerciseUnit.SECONDS) "s" else " pon."
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BgPureWhite,
                    border = BorderStroke(1.dp, BorderIceBlue),
                    modifier = Modifier.testTag("history_entry_${entry.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${entry.exerciseEmoji} ${entry.exerciseName}: ${entry.amount}$unitLabel",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimaryNavy
                            )
                            Text(
                                text = "${entry.dateKey} • ${timeFormatter.format(Date(entry.timestampMillis))} • ${entry.speedLevel.bolts} ${entry.speedLevel.shortTitleSr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondarySlate
                            )
                            if (entry.note.isNotBlank()) {
                                Text(
                                    text = entry.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RoyalBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteEntry(entry) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("delete_log_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Obriši unos",
                                tint = SubtleDeleteRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditDailyGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by remember(currentGoal) { mutableStateOf(currentGoal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgPureWhite,
        titleContentColor = TextPrimaryNavy,
        textContentColor = TextPrimaryNavy,
        title = { Text("Dnevni cilj ponavljanja") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Cilj ponavljanja za dan") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = repPulsTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_goal_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = text.toIntOrNull()?.coerceIn(10, 2000) ?: currentGoal
                    onSave(parsed)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = TextOnSolidWhite
                ),
                modifier = Modifier.testTag("save_daily_goal_button")
            ) {
                Text("Sačuvaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Otkaži", color = TextSecondarySlate)
            }
        }
    )
}

@Composable
private fun EditProfileMinimalDialog(
    profile: PersonalProfile,
    onDismiss: () -> Unit,
    onSave: (ageYears: Int, heightCm: Int, weightKg: Int) -> Unit
) {
    var ageText by remember(profile) { mutableStateOf(profile.ageYears.toString()) }
    var heightText by remember(profile) { mutableStateOf(profile.heightCm.toString()) }
    var weightText by remember(profile) { mutableStateOf(profile.weightKg.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgPureWhite,
        titleContentColor = TextPrimaryNavy,
        textContentColor = TextPrimaryNavy,
        title = { Text("Izmeni lični profil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Godine") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = repPulsTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Visina (cm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = repPulsTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Težina (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = repPulsTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val age = ageText.toIntOrNull()?.coerceIn(10, 99) ?: profile.ageYears
                    val height = heightText.toIntOrNull()?.coerceIn(110, 230) ?: profile.heightCm
                    val weight = weightText.toIntOrNull()?.coerceIn(25, 220) ?: profile.weightKg
                    onSave(age, height, weight)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = TextOnSolidWhite
                )
            ) {
                Text("Sačuvaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Otkaži", color = TextSecondarySlate)
            }
        }
    )
}
