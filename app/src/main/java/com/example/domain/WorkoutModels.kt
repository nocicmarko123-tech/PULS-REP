package com.example.domain

enum class ExerciseUnit(
    val shortLabelSr: String,
    val genitivePluralSr: String
) {
    REPS(
        shortLabelSr = "pon.",
        genitivePluralSr = "ponavljanja"
    ),
    SECONDS(
        shortLabelSr = "s",
        genitivePluralSr = "sekundi"
    );

    companion object {
        fun fromName(value: String): ExerciseUnit {
            return entries.firstOrNull { it.name == value } ?: REPS
        }
    }
}

data class ExerciseItem(
    val id: Long,
    val name: String,
    val emoji: String,
    val unit: ExerciseUnit,
    val defaultAmount: Int,
    val weeklyIncrement: Int = 0,
    val unitNoteSr: String = "",
    val isCustom: Boolean,
    val sortOrder: Int
) {
    fun targetForWeek(weekNumber: Int): Int {
        val safeWeek = weekNumber.coerceAtLeast(1)
        return defaultAmount + (safeWeek - 1) * weeklyIncrement
    }

    fun formatAmountLabel(amount: Int): String {
        return when {
            unit == ExerciseUnit.SECONDS -> "${amount}s"
            unitNoteSr.isNotBlank() -> "$amount $unitNoteSr"
            else -> "$amount pon."
        }
    }

    fun formatWeeklyProgression(): String {
        if (weeklyIncrement <= 0) return "Sopstvena vežba"
        val suffix = if (unit == ExerciseUnit.SECONDS) "s" else ""
        return "+$weeklyIncrement$suffix svake nedelje"
    }
}

data class WorkoutLogItem(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val exerciseEmoji: String,
    val amount: Int,
    val unit: ExerciseUnit,
    val speedLevel: SpeedLevel,
    val note: String,
    val timestampMillis: Long,
    val dateKey: String
)

data class QueuedTaskItem(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val exerciseEmoji: String,
    val amount: Int,
    val unit: ExerciseUnit,
    val customNote: String,
    val sortOrder: Int
)

data class NextTask(
    val exerciseId: Long,
    val exerciseName: String,
    val exerciseEmoji: String,
    val targetAmount: Int,
    val unit: ExerciseUnit,
    val unitNoteSr: String = "",
    val customText: String = ""
) {
    fun formatHeadline(speedLevel: SpeedLevel): String {
        return "${formatShortTask()} • ${speedLevel.bolts} ${speedLevel.titleSr}"
    }

    fun formatShortTask(): String {
        return if (customText.isNotBlank()) {
            customText.trim()
        } else if (unit == ExerciseUnit.SECONDS) {
            "$exerciseEmoji $exerciseName — ${targetAmount}s"
        } else if (unitNoteSr.isNotBlank()) {
            "$exerciseEmoji $exerciseName — $targetAmount $unitNoteSr"
        } else {
            "$exerciseEmoji $exerciseName — $targetAmount pon."
        }
    }
}

data class ExerciseProgressStats(
    val exercise: ExerciseItem,
    val todayTotal: Int,
    val allTimeTotal: Int,
    val personalRecord: Int,
    val totalSets: Int
)

data class DayHistoryGroup(
    val dateKey: String,
    val displayTitle: String,
    val totalReps: Int,
    val totalSeconds: Int,
    val totalSets: Int,
    val entries: List<WorkoutLogItem>
)

data class ReminderSettings(
    val speedLevel: SpeedLevel = SpeedLevel.STANDARD,
    val planWeek: Int = 1,
    val dailyGoalReps: Int = 62,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val persistentBannerEnabled: Boolean = true,
    val steadyReminderActive: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val reminderIntervalMinutes: Int = 180,
    val nextReminderAtMillis: Long = 0L
)
