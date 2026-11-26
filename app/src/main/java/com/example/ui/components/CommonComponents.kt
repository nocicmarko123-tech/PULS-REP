package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.NextTask
import com.example.domain.SpeedLevel
import com.example.ui.RestTimerState
import com.example.ui.theme.BgPureWhite
import com.example.ui.theme.BorderIceBlue
import com.example.ui.theme.BrightBlue
import com.example.ui.theme.CardIceBlue
import com.example.ui.theme.MonoCounterLarge
import com.example.ui.theme.MonoCounterSmall
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TealBorder
import com.example.ui.theme.TealGreen
import com.example.ui.theme.TealSoftTint
import com.example.ui.theme.TextOnSolidWhite
import com.example.ui.theme.TextPrimaryNavy
import com.example.ui.theme.TextSecondarySlate

@Composable
fun AthleticCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderIceBlue,
    containerColor: Color = CardIceBlue,
    cornerRadius: Dp = 14.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun PersistentReminderBanner(
    nextTask: NextTask,
    speedLevel: SpeedLevel,
    restTimer: RestTimerState,
    showSteadyBanner: Boolean,
    steadyReminderActive: Boolean,
    secondsUntilNextReminder: Long,
    steadyReminderAlertVisible: Boolean,
    onDismissCompletedAlert: () -> Unit,
    onDismissSteadyAlert: () -> Unit,
    onQuickComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showRestAlert = restTimer.isCompletedAlertVisible
    val showPeriodicAlert = !showRestAlert && steadyReminderAlertVisible
    val showUrgent = showRestAlert || showPeriodicAlert
    val isVisible = showUrgent || showSteadyBanner

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        val bgColor = if (showUrgent) TealSoftTint else CardIceBlue
        val border = if (showUrgent) TealGreen else BorderIceBlue
        val accent = if (showUrgent) TealGreen else RoyalBlue

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("persistent_reminder_banner"),
            shape = RoundedCornerShape(12.dp),
            color = bgColor,
            border = BorderStroke(1.dp, border),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            showRestAlert -> "⏱️ Pauza je istekla — kreni na sledeću seriju"
                            showPeriodicAlert -> "🔔 Stalni podsetnik — vreme je za trening!"
                            else -> "🔔 Sledeće na redu • ${speedLevel.bolts} ${speedLevel.shortTitleSr}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = nextTask.formatShortTask(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryNavy,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!showUrgent && steadyReminderActive && secondsUntilNextReminder > 0L) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "⏱️ Sledeći podsetnik za: ${formatHmsCountdown(secondsUntilNextReminder)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondarySlate
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (showUrgent) {
                    IconButton(
                        onClick = if (showRestAlert) onDismissCompletedAlert else onDismissSteadyAlert,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dismiss_rest_alert_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Zatvori obaveštenje",
                            tint = TextPrimaryNavy
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TealGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onQuickComplete)
                            .minimumInteractiveComponentSize()
                            .testTag("banner_quick_done_button")
                    ) {
                        Text(
                            text = "Urađeno",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextOnSolidWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedLevelSelector(
    selectedLevel: SpeedLevel,
    onSelectLevel: (SpeedLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    AthleticCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nivo brzine",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimaryNavy
            )
            Text(
                text = "Pauza ${selectedLevel.defaultRestSeconds}s",
                style = MonoCounterSmall,
                color = RoyalBlue
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedLevel.entries.forEach { level ->
                val isSelected = level == selectedLevel
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) RoyalBlue else BgPureWhite,
                    label = "speedBg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) RoyalBlue else BorderIceBlue,
                    label = "speedBorder"
                )
                val titleColor by animateColorAsState(
                    targetValue = if (isSelected) TextOnSolidWhite else TextPrimaryNavy,
                    label = "speedText"
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectLevel(level) }
                        .minimumInteractiveComponentSize()
                        .testTag("speed_level_${level.name.lowercase()}"),
                    shape = RoundedCornerShape(10.dp),
                    color = containerColor,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = level.bolts,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = level.shortTitleSr,
                            style = MaterialTheme.typography.labelMedium,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${level.defaultRestSeconds}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) TextOnSolidWhite.copy(alpha = 0.85f) else TextSecondarySlate
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${selectedLevel.bolts} ${selectedLevel.titleSr} — ${selectedLevel.tempoDescriptionSr}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondarySlate
        )
    }
}

@Composable
fun RestTimerCard(
    restTimer: RestTimerState,
    defaultRestSeconds: Int,
    nextTask: NextTask,
    onStartDefault: () -> Unit,
    onTogglePause: () -> Unit,
    onAdjustSeconds: (Int) -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = when {
        restTimer.isCompletedAlertVisible -> TealGreen
        restTimer.isRunning -> BrightBlue
        restTimer.isPaused -> RoyalBlue
        else -> RoyalBlue
    }
    val displaySeconds = if (restTimer.isActiveOrPaused) {
        restTimer.remainingSeconds
    } else {
        defaultRestSeconds
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (restTimer.isActiveOrPaused) restTimer.progressFraction else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "restArc"
    )

    AthleticCard(
        modifier = modifier.testTag("rest_timer_card"),
        borderColor = when {
            restTimer.isCompletedAlertVisible -> TealBorder
            restTimer.isRunning -> RoyalBlue.copy(alpha = 0.5f)
            else -> BorderIceBlue
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⏱️ Tajmer pauze",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimaryNavy
            )
            Text(
                text = when {
                    restTimer.isCompletedAlertVisible -> "Pauza gotova"
                    restTimer.isRunning -> "Odmor u toku"
                    restTimer.isPaused -> "Pauzirano"
                    else -> "Spreman (${defaultRestSeconds}s)"
                },
                style = MaterialTheme.typography.labelMedium,
                color = activeColor
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(104.dp)) {
                    val stroke = 7.dp.toPx()
                    drawArc(
                        color = BorderIceBlue,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = activeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatCountdown(displaySeconds),
                        style = MonoCounterLarge.copy(
                            fontSize = 24.sp,
                            lineHeight = 28.sp
                        ),
                        color = TextPrimaryNavy,
                        modifier = Modifier.testTag("rest_timer_countdown")
                    )
                    Text(
                        text = "sek",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondarySlate
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Posle pauze:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondarySlate
                )
                Text(
                    text = nextTask.formatShortTask(),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimaryNavy,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAdjustSeconds(-15) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("rest_minus_15_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, BorderIceBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Umanji za 15 sekundi",
                            modifier = Modifier.size(16.dp),
                            tint = RoyalBlue
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("15s", color = RoyalBlue, style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { onAdjustSeconds(15) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("rest_plus_15_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, BorderIceBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Dodaj 15 sekundi",
                            modifier = Modifier.size(16.dp),
                            tint = RoyalBlue
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("15s", color = RoyalBlue, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (restTimer.isActiveOrPaused) onTogglePause() else onStartDefault()
                },
                modifier = Modifier
                    .weight(1.4f)
                    .height(48.dp)
                    .testTag("rest_play_pause_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = TextOnSolidWhite
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (restTimer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (restTimer.isRunning) "Pauziraj tajmer" else "Pokreni tajmer",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        restTimer.isRunning -> "Pauza"
                        restTimer.isPaused -> "Nastavi"
                        else -> "Pokreni"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .weight(0.7f)
                    .height(48.dp)
                    .testTag("rest_reset_button"),
                border = BorderStroke(1.dp, BorderIceBlue),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Resetuj tajmer",
                    tint = TextSecondarySlate,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (restTimer.isActiveOrPaused) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("rest_skip_button"),
                    border = BorderStroke(1.dp, TealBorder),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Preskoči pauzu",
                        tint = TealGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Preskoči",
                        color = TealGreen,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

fun formatCountdown(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%02d:%02d".format(mins, secs)
}

fun formatHmsCountdown(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val mins = (safe % 3600L) / 60L
    val secs = safe % 60L
    return "%02d:%02d:%02d".format(hours, mins, secs)
}
