package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.ExerciseItem
import com.example.domain.ExerciseUnit
import com.example.domain.QueuedTaskItem
import com.example.domain.SpeedLevel
import com.example.domain.WorkoutLogItem

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val unit: String,
    val defaultAmount: Int,
    val weeklyIncrement: Int = 0,
    val unitNoteSr: String = "",
    val isCustom: Boolean = false,
    val sortOrder: Int = 0
) {
    fun toDomain() = ExerciseItem(
        id = id,
        name = name,
        emoji = emoji,
        unit = ExerciseUnit.fromName(unit),
        defaultAmount = defaultAmount,
        weeklyIncrement = weeklyIncrement,
        unitNoteSr = unitNoteSr,
        isCustom = isCustom,
        sortOrder = sortOrder
    )
}

@Entity(tableName = "workout_logs")
data class WorkoutLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val exerciseName: String,
    val exerciseEmoji: String,
    val amount: Int,
    val unit: String,
    val speedLevelName: String,
    val note: String = "",
    val timestampMillis: Long,
    val dateKey: String
) {
    fun toDomain() = WorkoutLogItem(
        id = id,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        exerciseEmoji = exerciseEmoji,
        amount = amount,
        unit = ExerciseUnit.fromName(unit),
        speedLevel = SpeedLevel.fromName(speedLevelName),
        note = note,
        timestampMillis = timestampMillis,
        dateKey = dateKey
    )
}

@Entity(tableName = "queued_tasks")
data class QueuedTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val exerciseName: String,
    val exerciseEmoji: String,
    val amount: Int,
    val unit: String,
    val customNote: String = "",
    val sortOrder: Int = 0
) {
    fun toDomain() = QueuedTaskItem(
        id = id,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        exerciseEmoji = exerciseEmoji,
        amount = amount,
        unit = ExerciseUnit.fromName(unit),
        customNote = customNote,
        sortOrder = sortOrder
    )
}
