package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.ExerciseItem
import com.example.domain.ExerciseUnit
import com.example.domain.InputSanitizer
import com.example.domain.NextTask
import com.example.ui.theme.BgPureWhite
import com.example.ui.theme.BorderIceBlue
import com.example.ui.theme.CardIceBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueTint
import com.example.ui.theme.TealGreen
import com.example.ui.theme.TextOnSolidWhite
import com.example.ui.theme.TextPrimaryNavy
import com.example.ui.theme.TextSecondarySlate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditNextTaskDialog(
    exercises: List<ExerciseItem>,
    currentNextTask: NextTask,
    onDismiss: () -> Unit,
    onConfirm: (exercise: ExerciseItem?, customText: String, targetAmount: Int) -> Unit
) {
    var selectedExercise by remember(exercises, currentNextTask) {
        mutableStateOf(
            exercises.firstOrNull { it.id == currentNextTask.exerciseId } ?: exercises.firstOrNull()
        )
    }
    var amountText by remember(currentNextTask) {
        mutableStateOf(currentNextTask.targetAmount.toString())
    }
    var customText by remember(currentNextTask) {
        mutableStateOf(currentNextTask.customText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgPureWhite,
        titleContentColor = TextPrimaryNavy,
        textContentColor = TextPrimaryNavy,
        title = {
            Text(
                text = "🔔 Postavi sledeći zadatak",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Izaberi vežbu iz plana ili upiši sopstveni podsetnik:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondarySlate
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exercises.forEach { exercise ->
                        val selected = selectedExercise?.id == exercise.id
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedExercise = exercise
                                amountText = exercise.defaultAmount.toString()
                            },
                            label = {
                                Text("${exercise.emoji} ${exercise.name}")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalBlue,
                                selectedLabelColor = TextOnSolidWhite,
                                containerColor = CardIceBlue,
                                labelColor = TextPrimaryNavy
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = BorderIceBlue,
                                selectedBorderColor = RoyalBlue
                            ),
                            modifier = Modifier.testTag("dialog_pick_exercise_${exercise.id}")
                        )
                    }
                }

                val unitLabel = selectedExercise?.unit?.genitivePluralSr ?: "ponavljanja"
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input.filter { it.isDigit() }.take(4)
                    },
                    label = { Text("Broj ($unitLabel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = repPulsTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_next_amount_input")
                )

                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = { Text("Ručni tekst podsetnika (opciono)") },
                    placeholder = { Text("npr. 15 sklekova + 30s izdržaj") },
                    maxLines = 2,
                    colors = repPulsTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_next_custom_text_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unit = selectedExercise?.unit ?: ExerciseUnit.REPS
                    val fallback = selectedExercise?.defaultAmount ?: 10
                    val parsedAmount = InputSanitizer.parsePositiveInt(
                        text = amountText,
                        fallback = fallback,
                        min = 1,
                        max = if (unit == ExerciseUnit.SECONDS) 600 else 500
                    )
                    val cleanCustom = InputSanitizer.sanitizeText(customText, 80)
                    onConfirm(selectedExercise, cleanCustom, parsedAmount)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = TextOnSolidWhite
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("dialog_save_next_button")
            ) {
                Text("Sačuvaj")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("Otkaži", color = TextSecondarySlate)
            }
        }
    )
}

@Composable
fun AddCustomExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, unit: ExerciseUnit, defaultAmount: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(ExerciseUnit.REPS) }
    var defaultAmountText by remember { mutableStateOf("12") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgPureWhite,
        titleContentColor = TextPrimaryNavy,
        textContentColor = TextPrimaryNavy,
        title = {
            Text(
                text = "💪 Nova vežba",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Naziv vežbe") },
                    placeholder = { Text("npr. Propadanja ili Zgibovi") },
                    singleLine = true,
                    colors = repPulsTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_exercise_name_input")
                )

                Text(
                    text = "Jedinica:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondarySlate
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExerciseUnit.entries.forEach { unit ->
                        val active = unit == selectedUnit
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedUnit = unit
                                    defaultAmountText = if (unit == ExerciseUnit.SECONDS) "30" else "12"
                                }
                                .minimumInteractiveComponentSize()
                                .testTag("custom_unit_${unit.name.lowercase()}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) RoyalBlue else CardIceBlue,
                            border = BorderStroke(1.dp, if (active) RoyalBlue else BorderIceBlue)
                        ) {
                            Text(
                                text = if (unit == ExerciseUnit.REPS) "Ponavljanja" else "Sekunde",
                                color = if (active) TextOnSolidWhite else TextPrimaryNavy,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = defaultAmountText,
                    onValueChange = { defaultAmountText = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = {
                        Text(
                            if (selectedUnit == ExerciseUnit.REPS) "Cilj ponavljanja"
                            else "Cilj sekundi"
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = repPulsTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_exercise_amount_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = InputSanitizer.sanitizeText(name, 40)
                    val parsed = InputSanitizer.parsePositiveInt(
                        text = defaultAmountText,
                        fallback = 10,
                        min = 1,
                        max = if (selectedUnit == ExerciseUnit.SECONDS) 600 else 500
                    )
                    if (cleanName.isNotBlank()) {
                        onConfirm(cleanName, "💪", selectedUnit, parsed)
                    }
                },
                enabled = InputSanitizer.sanitizeText(name, 40).isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = TextOnSolidWhite
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("save_custom_exercise_button")
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("Otkaži", color = TextSecondarySlate)
            }
        }
    )
}

@Composable
fun QuickCustomAmountDialog(
    exercise: ExerciseItem,
    onDismiss: () -> Unit,
    onLog: (amount: Int, startRest: Boolean) -> Unit
) {
    var amount by remember(exercise) { mutableIntStateOf(exercise.defaultAmount) }
    var textValue by remember(exercise) { mutableStateOf(exercise.defaultAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgPureWhite,
        titleContentColor = TextPrimaryNavy,
        textContentColor = TextPrimaryNavy,
        title = {
            Text("${exercise.emoji} ${exercise.name}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Unesi broj (${exercise.unit.genitivePluralSr}):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondarySlate
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val next = (amount - 5).coerceAtLeast(1)
                                amount = next
                                textValue = next.toString()
                            }
                            .minimumInteractiveComponentSize(),
                        color = CardIceBlue,
                        border = BorderStroke(1.dp, BorderIceBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "-5",
                            style = MaterialTheme.typography.titleMedium,
                            color = RoyalBlue,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }.take(4)
                            textValue = clean
                            clean.toIntOrNull()?.let { if (it > 0) amount = it }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = repPulsTextFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_custom_amount_input")
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val next = (amount + 5).coerceAtMost(9999)
                                amount = next
                                textValue = next.toString()
                            }
                            .minimumInteractiveComponentSize(),
                        color = CardIceBlue,
                        border = BorderStroke(1.dp, BorderIceBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "+5",
                            style = MaterialTheme.typography.titleMedium,
                            color = RoyalBlue,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        onLog(InputSanitizer.sanitizeAmount(amount, exercise.unit), false)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Samo upiši", color = RoyalBlue)
                }
                Button(
                    onClick = {
                        onLog(InputSanitizer.sanitizeAmount(amount, exercise.unit), true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealGreen,
                        contentColor = TextOnSolidWhite
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("confirm_quick_log_button")
                ) {
                    Text("Upiši + Pauza")
                }
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
fun repPulsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RoyalBlue,
    unfocusedBorderColor = BorderIceBlue,
    focusedLabelColor = RoyalBlue,
    unfocusedLabelColor = TextSecondarySlate,
    focusedTextColor = TextPrimaryNavy,
    unfocusedTextColor = TextPrimaryNavy,
    cursorColor = RoyalBlue,
    focusedContainerColor = CardIceBlue,
    unfocusedContainerColor = CardIceBlue
)
