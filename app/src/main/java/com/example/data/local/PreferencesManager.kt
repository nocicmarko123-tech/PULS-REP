package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.DailyHealthChecklist
import com.example.domain.ExerciseUnit
import com.example.domain.InputSanitizer
import com.example.domain.NextTask
import com.example.domain.PersonalProfile
import com.example.domain.ReminderSettings
import com.example.domain.SpeedLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reppuls_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val SPEED_LEVEL = stringPreferencesKey("speed_level")
        val PLAN_WEEK = intPreferencesKey("plan_week")
        val DAILY_GOAL_REPS = intPreferencesKey("daily_goal_reps")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val PERSISTENT_BANNER_ENABLED = booleanPreferencesKey("persistent_banner_enabled")
        val STEADY_REMINDER_ACTIVE = booleanPreferencesKey("steady_reminder_active")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_INTERVAL_MINUTES = intPreferencesKey("reminder_interval_minutes")
        val NEXT_REMINDER_AT_MILLIS = longPreferencesKey("next_reminder_at_millis")

        val NEXT_EXERCISE_ID = longPreferencesKey("next_exercise_id")
        val NEXT_EXERCISE_NAME = stringPreferencesKey("next_exercise_name")
        val NEXT_EXERCISE_EMOJI = stringPreferencesKey("next_exercise_emoji")
        val NEXT_AMOUNT = intPreferencesKey("next_amount")
        val NEXT_UNIT = stringPreferencesKey("next_unit")
        val NEXT_UNIT_NOTE = stringPreferencesKey("next_unit_note")
        val NEXT_CUSTOM_TEXT = stringPreferencesKey("next_custom_text")

        val PROFILE_AGE = intPreferencesKey("profile_age_years")
        val PROFILE_HEIGHT_CM = intPreferencesKey("profile_height_cm")
        val PROFILE_WEIGHT_KG = intPreferencesKey("profile_weight_kg")

        val CHECKLIST_DATE_KEY = stringPreferencesKey("checklist_date_key")
        val CHECKLIST_WATER = booleanPreferencesKey("checklist_water")
        val CHECKLIST_MEALS = booleanPreferencesKey("checklist_meals")
        val CHECKLIST_SLEEP = booleanPreferencesKey("checklist_sleep")
        val CHECKLIST_WARMUP = booleanPreferencesKey("checklist_warmup")
    }

    private val safeDataFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException || exception is Exception) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val reminderSettingsFlow: Flow<ReminderSettings> = safeDataFlow.map { prefs ->
        val speed = SpeedLevel.fromName(prefs[Keys.SPEED_LEVEL])
        val rawInterval = prefs[Keys.REMINDER_INTERVAL_MINUTES] ?: 180
        ReminderSettings(
            speedLevel = speed,
            planWeek = InputSanitizer.sanitizePlanWeek(prefs[Keys.PLAN_WEEK] ?: 1),
            dailyGoalReps = InputSanitizer.sanitizeDailyGoal(prefs[Keys.DAILY_GOAL_REPS] ?: 62),
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            persistentBannerEnabled = prefs[Keys.PERSISTENT_BANNER_ENABLED] ?: true,
            steadyReminderActive = prefs[Keys.STEADY_REMINDER_ACTIVE] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            reminderIntervalMinutes = InputSanitizer.sanitizeReminderIntervalMinutes(rawInterval),
            nextReminderAtMillis = (prefs[Keys.NEXT_REMINDER_AT_MILLIS] ?: 0L).coerceAtLeast(0L)
        )
    }

    val nextTaskFlow: Flow<NextTask> = safeDataFlow.map { prefs ->
        val unit = ExerciseUnit.fromName(prefs[Keys.NEXT_UNIT] ?: ExerciseUnit.REPS.name)
        NextTask(
            exerciseId = (prefs[Keys.NEXT_EXERCISE_ID] ?: 1L).coerceAtLeast(1L),
            exerciseName = InputSanitizer.sanitizeText(
                prefs[Keys.NEXT_EXERCISE_NAME] ?: "Sklekovi",
                40
            ).ifBlank { "Sklekovi" },
            exerciseEmoji = prefs[Keys.NEXT_EXERCISE_EMOJI] ?: "💪",
            targetAmount = InputSanitizer.sanitizeAmount(prefs[Keys.NEXT_AMOUNT] ?: 12, unit),
            unit = unit,
            unitNoteSr = InputSanitizer.sanitizeText(prefs[Keys.NEXT_UNIT_NOTE] ?: "", 20),
            customText = InputSanitizer.sanitizeText(prefs[Keys.NEXT_CUSTOM_TEXT] ?: "", 80)
        )
    }

    val personalProfileFlow: Flow<PersonalProfile> = safeDataFlow.map { prefs ->
        PersonalProfile(
            ageYears = (prefs[Keys.PROFILE_AGE] ?: 14).coerceIn(10, 99),
            heightCm = (prefs[Keys.PROFILE_HEIGHT_CM] ?: 172).coerceIn(110, 230),
            weightKg = (prefs[Keys.PROFILE_WEIGHT_KG] ?: 48).coerceIn(25, 220)
        )
    }

    val dailyHealthChecklistFlow: Flow<DailyHealthChecklist> = safeDataFlow.map { prefs ->
        val today = WorkoutRepository.todayDateKey()
        val storedDate = prefs[Keys.CHECKLIST_DATE_KEY] ?: ""
        if (storedDate != today) {
            DailyHealthChecklist(dateKey = today)
        } else {
            DailyHealthChecklist(
                dateKey = today,
                waterDone = prefs[Keys.CHECKLIST_WATER] ?: false,
                mealsDone = prefs[Keys.CHECKLIST_MEALS] ?: false,
                sleepDone = prefs[Keys.CHECKLIST_SLEEP] ?: false,
                warmupDone = prefs[Keys.CHECKLIST_WARMUP] ?: false
            )
        }
    }

    suspend fun getNextTaskOnce(): NextTask =
        runCatching { nextTaskFlow.first() }.getOrElse {
            NextTask(1L, "Sklekovi", "💪", 12, ExerciseUnit.REPS)
        }

    suspend fun getReminderSettingsOnce(): ReminderSettings =
        runCatching { reminderSettingsFlow.first() }.getOrElse { ReminderSettings() }

    suspend fun setSpeedLevel(level: SpeedLevel) {
        safeEdit { prefs ->
            prefs[Keys.SPEED_LEVEL] = level.name
        }
    }

    suspend fun setPlanWeek(week: Int) {
        safeEdit { prefs ->
            prefs[Keys.PLAN_WEEK] = InputSanitizer.sanitizePlanWeek(week)
        }
    }

    suspend fun setDailyGoalReps(goal: Int) {
        safeEdit { prefs ->
            prefs[Keys.DAILY_GOAL_REPS] = InputSanitizer.sanitizeDailyGoal(goal)
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        safeEdit { prefs -> prefs[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        safeEdit { prefs -> prefs[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setPersistentBannerEnabled(enabled: Boolean) {
        safeEdit { prefs -> prefs[Keys.PERSISTENT_BANNER_ENABLED] = enabled }
    }

    suspend fun setSteadyReminderState(active: Boolean, intervalMinutes: Int, nextAtMillis: Long) {
        val safeInterval = InputSanitizer.sanitizeReminderIntervalMinutes(intervalMinutes)
        safeEdit { prefs ->
            prefs[Keys.STEADY_REMINDER_ACTIVE] = active
            prefs[Keys.REMINDER_INTERVAL_MINUTES] = safeInterval
            prefs[Keys.NEXT_REMINDER_AT_MILLIS] = nextAtMillis.coerceAtLeast(0L)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        safeEdit { prefs -> prefs[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setNextTask(task: NextTask) {
        safeEdit { prefs ->
            prefs[Keys.NEXT_EXERCISE_ID] = task.exerciseId.coerceAtLeast(1L)
            prefs[Keys.NEXT_EXERCISE_NAME] =
                InputSanitizer.sanitizeText(task.exerciseName, 40).ifBlank { "Vežba" }
            prefs[Keys.NEXT_EXERCISE_EMOJI] = task.exerciseEmoji.ifBlank { "💪" }
            prefs[Keys.NEXT_AMOUNT] = InputSanitizer.sanitizeAmount(task.targetAmount, task.unit)
            prefs[Keys.NEXT_UNIT] = task.unit.name
            prefs[Keys.NEXT_UNIT_NOTE] = InputSanitizer.sanitizeText(task.unitNoteSr, 20)
            prefs[Keys.NEXT_CUSTOM_TEXT] = InputSanitizer.sanitizeText(task.customText, 80)
        }
    }

    suspend fun updatePersonalProfile(ageYears: Int, heightCm: Int, weightKg: Int) {
        safeEdit { prefs ->
            prefs[Keys.PROFILE_AGE] = ageYears.coerceIn(10, 99)
            prefs[Keys.PROFILE_HEIGHT_CM] = heightCm.coerceIn(110, 230)
            prefs[Keys.PROFILE_WEIGHT_KG] = weightKg.coerceIn(25, 220)
        }
    }

    suspend fun toggleHealthHabit(habitId: String, done: Boolean) {
        val today = WorkoutRepository.todayDateKey()
        safeEdit { prefs ->
            if (prefs[Keys.CHECKLIST_DATE_KEY] != today) {
                prefs[Keys.CHECKLIST_DATE_KEY] = today
                prefs[Keys.CHECKLIST_WATER] = false
                prefs[Keys.CHECKLIST_MEALS] = false
                prefs[Keys.CHECKLIST_SLEEP] = false
                prefs[Keys.CHECKLIST_WARMUP] = false
            }
            when (habitId) {
                "water" -> prefs[Keys.CHECKLIST_WATER] = done
                "meals" -> prefs[Keys.CHECKLIST_MEALS] = done
                "sleep" -> prefs[Keys.CHECKLIST_SLEEP] = done
                "warmup" -> prefs[Keys.CHECKLIST_WARMUP] = done
            }
        }
    }

    private suspend fun safeEdit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        runCatching {
            context.dataStore.edit(block)
        }
    }
}
