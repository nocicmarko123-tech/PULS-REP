package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ExerciseItem
import com.example.domain.ExerciseUnit
import com.example.domain.InputSanitizer
import com.example.domain.QueuedTaskItem
import com.example.ui.WorkoutUiState
import com.example.ui.components.AthleticCard
import com.example.ui.components.formatHmsCountdown
import com.example.ui.components.repPulsTextFieldColors
import com.example.ui.theme.BgPureWhite
import com.example.ui.theme.BorderIceBlue
import com.example.ui.theme.CardIceBlue
import com.example.ui.theme.MonoCounterMedium
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueTint
import com.example.ui.theme.SubtleDeleteRed
import com.example.ui.theme.TealBorder
import com.example.ui.theme.TealGreen
import com.example.ui.theme.TealSoftTint
import com.example.ui.theme.TextOnSolidWhite
import com.example.ui.theme.TextPrimaryNavy
import com.example.ui.theme.TextSecondarySlate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindersScreen(
    uiState: WorkoutUiState,
    onSetNextTask: (ExerciseItem?, String, Int) -> Unit,
    onAddToQueue: (ExerciseItem?, Int, String) -> Unit,
    onRemoveFromQueue: (Long) -> Unit,
    onPromoteFromQueue: (QueuedTaskItem) -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onSetVibrationEnabled: (Boolean) -> Unit,
    onSetPersistentBannerEnabled: (Boolean) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onToggleSteadyReminder: (Boolean) -> Unit,
    onSetSteadyReminderInterval: (Int) -> Unit,
    onRefreshPermission: () -> Unit,
    onSendTestNotification: () -> Boolean,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedExercise by remember(uiState.exercises) {
        mutableStateOf(uiState.exercises.firstOrNull())
    }
    var amountText by remember(selectedExercise, uiState.settings.planWeek) {
        mutableStateOf(
            (selectedExercise?.targetForWeek(uiState.settings.planWeek) ?: 12).toString()
        )
    }
    var customNote by remember { mutableStateOf("") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRefreshPermission()
        if (granted) {
            onSetNotificationsEnabled(true)
            onShowMessage("🔔 Obaveštenja su uključena")
        } else {
            onSetNotificationsEnabled(false)
            onShowMessage("Dozvola nije odobrena — podsetnici rade unutar aplikacije.")
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reminders_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "reminders_header") {
            Column {
                Text(
                    text = "🔔 Podsetnici",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimaryNavy
                )
                Text(
                    text = "Planiraj sledeći korak i ritam obaveštenja",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySlate
                )
            }
        }

        // Current Active Next Task Banner
        item(key = "active_reminder_preview") {
            AthleticCard(
                borderColor = RoyalBlue.copy(alpha = 0.45f),
                containerColor = CardIceBlue
            ) {
                Text(
                    text = "SLEDEĆE NA REDU",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoyalBlue
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = uiState.nextTask.formatHeadline(uiState.settings.speedLevel),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryNavy
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uiState.settings.speedLevel.tempoDescriptionSr,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySlate
                )
            }
        }

        // Steady Reminder Card (3h / 4h / test 1m + live countdown + On/Off toggle)
        item(key = "steady_reminder_card") {
            val settings = uiState.settings
            val isSteadyOn = settings.steadyReminderActive && settings.reminderIntervalMinutes > 0
            AthleticCard(
                borderColor = if (isSteadyOn) TealBorder else BorderIceBlue,
                containerColor = BgPureWhite
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "🔔 Stalni podsetnik (3h / 4h)",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimaryNavy
                        )
                        Text(
                            text = if (isSteadyOn) "Aktivan • zvuk i poruka na svakom ciklusu" else "Isključen",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSteadyOn) TealGreen else TextSecondarySlate
                        )
                    }
                    Switch(
                        checked = isSteadyOn,
                        onCheckedChange = { wantOn ->
                            onToggleSteadyReminder(wantOn)
                            onShowMessage(
                                if (wantOn) "🔔 Stalni podsetnik uključen"
                                else "Stalni podsetnik isključen"
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextOnSolidWhite,
                            checkedTrackColor = TealGreen,
                            uncheckedThumbColor = TextSecondarySlate,
                            uncheckedTrackColor = SoftBlueTint,
                            uncheckedBorderColor = BorderIceBlue
                        ),
                        modifier = Modifier.testTag("toggle_steady_reminder")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CardIceBlue,
                    border = BorderStroke(1.dp, BorderIceBlue)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isSteadyOn && uiState.secondsUntilNextReminder > 0L) {
                                "⏱️ Sledeći podsetnik za: ${formatHmsCountdown(uiState.secondsUntilNextReminder)}"
                            } else {
                                "⏱️ Sledeći podsetnik: pauziran"
                            },
                            style = MonoCounterMedium,
                            color = if (isSteadyOn) RoyalBlue else TextSecondarySlate,
                            modifier = Modifier.testTag("steady_reminder_countdown")
                        )
                        Text(
                            text = "Sledeće: ${uiState.nextTask.formatShortTask()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryNavy,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Brzi izbor intervala:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondarySlate
                )
                Spacer(modifier = Modifier.height(8.dp))

                val intervals = listOf(
                    1 to "1m (test)",
                    15 to "15m",
                    60 to "1h",
                    180 to "3h",
                    240 to "4h"
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    intervals.forEach { (mins, label) ->
                        val selected = isSteadyOn && settings.reminderIntervalMinutes == mins
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSetSteadyReminderInterval(mins)
                                    onShowMessage("🔔 Stalni podsetnik podešen na $label")
                                }
                                .minimumInteractiveComponentSize()
                                .testTag("interval_$mins"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) RoyalBlue else CardIceBlue,
                            border = BorderStroke(
                                1.dp,
                                if (selected) RoyalBlue else BorderIceBlue
                            )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) TextOnSolidWhite else TextPrimaryNavy,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Composer for Next Task or Queue Item
        item(key = "task_composer") {
            AthleticCard {
                Text(
                    text = "Dodaj ili promeni zadatak",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimaryNavy
                )
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.exercises.forEach { ex ->
                        val active = selectedExercise?.id == ex.id
                        FilterChip(
                            selected = active,
                            onClick = {
                                selectedExercise = ex
                                amountText = ex.targetForWeek(uiState.settings.planWeek).toString()
                            },
                            label = { Text("${ex.emoji} ${ex.name}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalBlue,
                                selectedLabelColor = TextOnSolidWhite,
                                containerColor = BgPureWhite,
                                labelColor = TextPrimaryNavy
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = active,
                                borderColor = BorderIceBlue,
                                selectedBorderColor = RoyalBlue
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = {
                            Text(
                                if (selectedExercise?.unit == ExerciseUnit.SECONDS) "Sek" else "Pon"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = repPulsTextFieldColors(),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("queue_amount_input")
                    )

                    OutlinedTextField(
                        value = customNote,
                        onValueChange = { customNote = it },
                        label = { Text("Podsetnik (opciono)") },
                        placeholder = { Text("npr. 15 sklekova") },
                        singleLine = true,
                        colors = repPulsTextFieldColors(),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("queue_custom_note_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val unit = selectedExercise?.unit ?: ExerciseUnit.REPS
                            val fallback = selectedExercise?.targetForWeek(uiState.settings.planWeek) ?: 12
                            val parsed = InputSanitizer.parsePositiveInt(
                                text = amountText,
                                fallback = fallback,
                                min = 1,
                                max = if (unit == ExerciseUnit.SECONDS) 600 else 500
                            )
                            val cleanNote = InputSanitizer.sanitizeText(customNote, 80)
                            onSetNextTask(selectedExercise, cleanNote, parsed)
                            customNote = ""
                            onShowMessage("🔔 Postavljeno kao sledeći korak")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealGreen,
                            contentColor = TextOnSolidWhite
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("set_immediate_next_button")
                    ) {
                        Text("Postavi sad", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = {
                            val unit = selectedExercise?.unit ?: ExerciseUnit.REPS
                            val fallback = selectedExercise?.targetForWeek(uiState.settings.planWeek) ?: 12
                            val parsed = InputSanitizer.parsePositiveInt(
                                text = amountText,
                                fallback = fallback,
                                min = 1,
                                max = if (unit == ExerciseUnit.SECONDS) 600 else 500
                            )
                            val cleanNote = InputSanitizer.sanitizeText(customNote, 80)
                            onAddToQueue(selectedExercise, parsed, cleanNote)
                            customNote = ""
                            onShowMessage("Dodato u redosled")
                        },
                        border = BorderStroke(1.dp, RoyalBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_to_queue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("U redosled", color = RoyalBlue, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Upcoming Queue List
        item(key = "queue_header") {
            Text(
                text = "Redosled koraka (${uiState.queue.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryNavy
            )
        }

        if (uiState.queue.isEmpty()) {
            item(key = "empty_queue") {
                AthleticCard {
                    Text(
                        text = "Kada je redosled prazan, posle svake serije automatski ide sledeći blok iz plana.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySlate
                    )
                }
            }
        } else {
            items(
                items = uiState.queue,
                key = { "q_${it.id}" }
            ) { item ->
                val title = if (item.customNote.isNotBlank()) {
                    item.customNote
                } else if (item.unit == ExerciseUnit.SECONDS) {
                    "${item.exerciseEmoji} ${item.exerciseName} — ${item.amount}s"
                } else {
                    "${item.exerciseEmoji} ${item.exerciseName} — ${item.amount} pon."
                }

                AthleticCard(
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimaryNavy,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    onPromoteFromQueue(item)
                                    onShowMessage("🔔 Postavljeno kao sledeće")
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("promote_queue_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Postavi kao sledeće",
                                    tint = TealGreen
                                )
                            }
                            IconButton(
                                onClick = { onRemoveFromQueue(item.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("remove_queue_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Ukloni iz reda",
                                    tint = SubtleDeleteRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // Notification & Alert Preferences
        item(key = "alerts_settings_card") {
            val settings = uiState.settings
            AthleticCard {
                Text(
                    text = "⏱️ Zvuk i obaveštenja",
                    style = MaterialTheme.typography.titleSmall,
                    color = RoyalBlue
                )
                Spacer(modifier = Modifier.height(12.dp))

                SettingToggleRow(
                    title = "Stalna traka podsetnika",
                    subtitle = "Prikazuj sledeći korak na vrhu ekrana",
                    checked = settings.persistentBannerEnabled,
                    onCheckedChange = onSetPersistentBannerEnabled,
                    testTag = "toggle_persistent_banner"
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Zvučni signal tajmera",
                    subtitle = "Signal kada istekne pauza",
                    checked = settings.soundEnabled,
                    onCheckedChange = onSetSoundEnabled,
                    testTag = "toggle_sound"
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Vibracija",
                    subtitle = "Kratak znak na kraju pauze",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = onSetVibrationEnabled,
                    testTag = "toggle_vibration"
                )
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (uiState.hasNotificationPermission) BgPureWhite else TealSoftTint,
                    border = BorderStroke(
                        1.dp,
                        if (uiState.hasNotificationPermission) BorderIceBlue else TealBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingToggleRow(
                            title = "Sistemska obaveštenja",
                            subtitle = if (uiState.hasNotificationPermission) {
                                "Aktivno za kraj pauze i podsetnike"
                            } else {
                                "Dozvoli obaveštenja kada pauza istekne"
                            },
                            checked = settings.notificationsEnabled && uiState.hasNotificationPermission,
                            onCheckedChange = { wantEnable ->
                                if (!wantEnable) {
                                    onSetNotificationsEnabled(false)
                                } else if (uiState.hasNotificationPermission) {
                                    onSetNotificationsEnabled(true)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSetNotificationsEnabled(true)
                                }
                            },
                            testTag = "toggle_system_notifications"
                        )

                        if (!uiState.hasNotificationPermission &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ) {
                            Button(
                                onClick = {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TealGreen,
                                    contentColor = TextOnSolidWhite
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("grant_notification_permission_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Dozvoli obaveštenja")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val sent = onSendTestNotification()
                                if (sent) {
                                    onShowMessage("🔔 Test obaveštenje poslato")
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }
                            },
                            border = BorderStroke(1.dp, RoyalBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("send_test_notification_button")
                        ) {
                            Text(
                                text = "Pošalji test podsetnik",
                                color = RoyalBlue,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        item(key = "reminders_bottom_spacer") {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimaryNavy
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondarySlate
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextOnSolidWhite,
                checkedTrackColor = RoyalBlue,
                uncheckedThumbColor = TextSecondarySlate,
                uncheckedTrackColor = SoftBlueTint,
                uncheckedBorderColor = BorderIceBlue
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
