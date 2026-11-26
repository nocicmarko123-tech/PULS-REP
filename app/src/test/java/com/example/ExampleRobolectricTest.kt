package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WorkoutRepository
import com.example.domain.ExerciseItem
import com.example.domain.ExerciseUnit
import com.example.domain.InputSanitizer
import com.example.domain.PersonalHealthAdvisor
import com.example.domain.PersonalProfile
import com.example.domain.ReminderSettings
import com.example.domain.SpeedLevel
import com.example.domain.WorkoutLogItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RepPuls", appName)
    }

    @Test
    fun `speed levels have minimalist rest durations`() {
        assertEquals(75, SpeedLevel.SLOW.defaultRestSeconds)
        assertEquals(45, SpeedLevel.STANDARD.defaultRestSeconds)
        assertEquals(25, SpeedLevel.FAST.defaultRestSeconds)
    }

    @Test
    fun `weekly progression calculates exact block targets including wall sit`() {
        val pushups = ExerciseItem(
            id = 1L,
            name = "Sklekovi",
            emoji = "💪",
            unit = ExerciseUnit.REPS,
            defaultAmount = 12,
            weeklyIncrement = 2,
            isCustom = false,
            sortOrder = 1
        )
        val wallSit = ExerciseItem(
            id = 6L,
            name = "Zidni čučanj / Nevidljiva stolica",
            emoji = "💪",
            unit = ExerciseUnit.SECONDS,
            defaultAmount = 30,
            weeklyIncrement = 10,
            isCustom = false,
            sortOrder = 6
        )
        assertEquals(12, pushups.targetForWeek(1))
        assertEquals(14, pushups.targetForWeek(2))
        assertEquals(16, pushups.targetForWeek(3))
        assertEquals(30, wallSit.targetForWeek(1))
        assertEquals(40, wallSit.targetForWeek(2))
        assertEquals(50, wallSit.targetForWeek(3))
    }

    @Test
    fun `input sanitizer strips html script tags and clamps bounds`() {
        val dirty = "<script>alert('xss')</script>Sklekovi javascript:bad onclick=evil"
        val clean = InputSanitizer.sanitizeText(dirty, 80)
        assertFalse(clean.contains("<"))
        assertFalse(clean.contains(">"))
        assertFalse(clean.lowercase().contains("javascript:"))
        assertFalse(clean.lowercase().contains("onclick="))
        assertTrue(clean.contains("Sklekovi"))

        assertEquals(1, InputSanitizer.sanitizeAmount(-50, ExerciseUnit.REPS))
        assertEquals(500, InputSanitizer.sanitizeAmount(99999, ExerciseUnit.REPS))
        assertEquals(600, InputSanitizer.sanitizeAmount(99999, ExerciseUnit.SECONDS))
        assertEquals(1, InputSanitizer.parsePositiveInt("abc", fallback = 1, min = 1, max = 500))
        assertEquals(180, InputSanitizer.sanitizeReminderIntervalMinutes(999))
        assertEquals(240, InputSanitizer.sanitizeReminderIntervalMinutes(240))
        assertEquals(1, InputSanitizer.sanitizeReminderIntervalMinutes(1))
    }

    @Test
    fun `default steady reminder interval is 3 hours`() {
        val defaults = ReminderSettings()
        assertTrue(defaults.steadyReminderActive)
        assertEquals(180, defaults.reminderIntervalMinutes)
    }

    @Test
    fun `personal profile defaults match user stats and teen health guidance`() {
        val profile = PersonalProfile()
        assertEquals(14, profile.ageYears)
        assertEquals(172, profile.heightCm)
        assertEquals(48, profile.weightKg)
        assertTrue(profile.isTeen)
        assertTrue(profile.growthSummarySr.contains("14"))
        assertTrue(profile.growthSummarySr.contains("172 cm"))
        assertTrue(profile.growthSummarySr.contains("48 kg"))
        val principles = PersonalHealthAdvisor.buildPrinciples(profile)
        assertEquals(5, principles.size)
    }

    @Test
    fun `streak counts consecutive logged days ending today`() {
        val cal = Calendar.getInstance()
        val todayMs = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayMs = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val twoDaysAgoMs = cal.timeInMillis

        val logs = listOf(
            WorkoutLogItem(
                id = 1L,
                exerciseId = 1L,
                exerciseName = "Sklekovi",
                exerciseEmoji = "💪",
                amount = 12,
                unit = ExerciseUnit.REPS,
                speedLevel = SpeedLevel.STANDARD,
                note = "",
                timestampMillis = todayMs,
                dateKey = WorkoutRepository.formatDateKey(todayMs)
            ),
            WorkoutLogItem(
                id = 2L,
                exerciseId = 2L,
                exerciseName = "Čučnjevi",
                exerciseEmoji = "💪",
                amount = 15,
                unit = ExerciseUnit.REPS,
                speedLevel = SpeedLevel.SLOW,
                note = "",
                timestampMillis = yesterdayMs,
                dateKey = WorkoutRepository.formatDateKey(yesterdayMs)
            ),
            WorkoutLogItem(
                id = 3L,
                exerciseId = 1L,
                exerciseName = "Sklekovi",
                exerciseEmoji = "💪",
                amount = 12,
                unit = ExerciseUnit.REPS,
                speedLevel = SpeedLevel.FAST,
                note = "",
                timestampMillis = twoDaysAgoMs,
                dateKey = WorkoutRepository.formatDateKey(twoDaysAgoMs)
            )
        )

        assertEquals(3, WorkoutRepository.calculateStreak(logs, todayMs))
    }
}
