package com.example.domain

object InputSanitizer {

    private val htmlTagRegex = Regex("<[^>]*>")
    private val dangerousProtocolRegex = Regex("(?i)(javascript:|data:|vbscript:|on\\w+\\s*=)")
    private val controlCharsRegex = Regex("[\\p{Cntrl}&&[^\n\t]]")

    fun sanitizeText(raw: String, maxLength: Int = 80): String {
        val noTags = htmlTagRegex.replace(raw, "")
        val noScripts = dangerousProtocolRegex.replace(noTags, "")
        val noControls = controlCharsRegex.replace(noScripts, "")
        return noControls
            .replace("<", "")
            .replace(">", "")
            .trim()
            .take(maxLength)
    }

    fun sanitizeAmount(raw: Int, unit: ExerciseUnit): Int {
        val max = if (unit == ExerciseUnit.SECONDS) 600 else 500
        return raw.coerceIn(1, max)
    }

    fun parsePositiveInt(text: String, fallback: Int, min: Int = 1, max: Int = 600): Int {
        val digitsOnly = text.filter { it.isDigit() }.take(4)
        val parsed = digitsOnly.toIntOrNull() ?: return fallback.coerceIn(min, max)
        return parsed.coerceIn(min, max)
    }

    fun sanitizeReminderIntervalMinutes(minutes: Int): Int {
        val allowed = setOf(0, 1, 15, 60, 180, 240)
        return if (minutes in allowed) minutes else 180
    }

    fun sanitizeDailyGoal(goal: Int): Int = goal.coerceIn(10, 1000)

    fun sanitizePlanWeek(week: Int): Int = week.coerceIn(1, 52)
}
