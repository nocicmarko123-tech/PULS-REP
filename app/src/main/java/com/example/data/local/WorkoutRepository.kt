package com.example.data.local

import com.example.domain.DailyHealthChecklist
import com.example.domain.DayHistoryGroup
import com.example.domain.ExerciseItem
import com.example.domain.ExerciseProgressStats
import com.example.domain.ExerciseUnit
import com.example.domain.InputSanitizer
import com.example.domain.NextTask
import com.example.domain.PersonalProfile
import com.example.domain.QueuedTaskItem
import com.example.domain.ReminderSettings
import com.example.domain.SpeedLevel
import com.example.domain.WorkoutLogItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WorkoutRepository(
    private val exerciseDao: ExerciseDao,
    private val workoutLogDao: WorkoutLogDao,
    private val queuedTaskDao: QueuedTaskDao,
    private val preferencesManager: PreferencesManager
) {
    val exercisesFlow: Flow<List<ExerciseItem>> = exerciseDao.observeAll()
        .map { list -> list.map { it.toDomain() } }
        .catch { emit(emptyList()) }

    val logsFlow: Flow<List<WorkoutLogItem>> = workoutLogDao.observeAllLogs()
        .map { list -> list.map { it.toDomain() } }
        .catch { emit(emptyList()) }

    val queueFlow: Flow<List<QueuedTaskItem>> = queuedTaskDao.observeQueue()
        .map { list -> list.map { it.toDomain() } }
        .catch { emit(emptyList()) }

    val settingsFlow: Flow<ReminderSettings> = preferencesManager.reminderSettingsFlow
    val nextTaskFlow: Flow<NextTask> = preferencesManager.nextTaskFlow
    val personalProfileFlow: Flow<PersonalProfile> = preferencesManager.personalProfileFlow
    val dailyHealthChecklistFlow: Flow<DailyHealthChecklist> = preferencesManager.dailyHealthChecklistFlow

    suspend fun ensureSeeded() {
        runCatching {
            if (exerciseDao.count() == 0) {
                val defaults = listOf(
                    ExerciseEntity(
                        name = "Sklekovi",
                        emoji = "💪",
                        unit = ExerciseUnit.REPS.name,
                        defaultAmount = 12,
                        weeklyIncrement = 2,
                        unitNoteSr = "",
                        isCustom = false,
                        sortOrder = 1
                    ),
                    ExerciseEntity(
                        name = "Čučnjevi",
                        emoji = "💪",
                        unit = ExerciseUnit.REPS.name,
                        defaultAmount = 15,
                        weeklyIncrement = 3,
                        unitNoteSr = "",
                        isCustom = false,
                        sortOrder = 2
                    ),
                    ExerciseEntity(
                        name = "Trbušnjaci",
                        emoji = "💪",
                        unit = ExerciseUnit.REPS.name,
                        defaultAmount = 15,
                        weeklyIncrement = 2,
                        unitNoteSr = "",
                        isCustom = false,
                        sortOrder = 3
                    ),
                    ExerciseEntity(
                        name = "Izdržaj / Plank",
                        emoji = "💪",
                        unit = ExerciseUnit.SECONDS.name,
                        defaultAmount = 30,
                        weeklyIncrement = 10,
                        unitNoteSr = "",
                        isCustom = false,
                        sortOrder = 4
                    ),
                    ExerciseEntity(
                        name = "Iskoraci",
                        emoji = "💪",
                        unit = ExerciseUnit.REPS.name,
                        defaultAmount = 10,
                        weeklyIncrement = 2,
                        unitNoteSr = "po nozi",
                        isCustom = false,
                        sortOrder = 5
                    ),
                    ExerciseEntity(
                        name = "Zidni čučanj / Nevidljiva stolica",
                        emoji = "💪",
                        unit = ExerciseUnit.SECONDS.name,
                        defaultAmount = 30,
                        weeklyIncrement = 10,
                        unitNoteSr = "",
                        isCustom = false,
                        sortOrder = 6
                    )
                )
                val ids = exerciseDao.insertAll(defaults)
                val firstId = ids.firstOrNull() ?: 1L
                preferencesManager.setNextTask(
                    NextTask(
                        exerciseId = firstId,
                        exerciseName = "Sklekovi",
                        exerciseEmoji = "💪",
                        targetAmount = 12,
                        unit = ExerciseUnit.REPS
                    )
                )
            } else {
                // Ensure Blok 6 (Wall Sit / Invisible Chair) exists on upgrades without wiping user logs
                val wallSitName = "Zidni čučanj / Nevidljiva stolica"
                if (exerciseDao.findByName(wallSitName) == null) {
                    exerciseDao.insert(
                        ExerciseEntity(
                            name = wallSitName,
                            emoji = "💪",
                            unit = ExerciseUnit.SECONDS.name,
                            defaultAmount = 30,
                            weeklyIncrement = 10,
                            unitNoteSr = "",
                            isCustom = false,
                            sortOrder = 6
                        )
                    )
                }
            }

            // Initialize first 3-hour reminder timestamp if active and unset
            val settings = preferencesManager.getReminderSettingsOnce()
            if (settings.steadyReminderActive &&
                settings.reminderIntervalMinutes > 0 &&
                settings.nextReminderAtMillis <= 0L
            ) {
                val nextAt = System.currentTimeMillis() + settings.reminderIntervalMinutes * 60_000L
                preferencesManager.setSteadyReminderState(
                    active = true,
                    intervalMinutes = settings.reminderIntervalMinutes,
                    nextAtMillis = nextAt
                )
            }
        }
    }

    suspend fun logSet(
        exerciseId: Long,
        exerciseName: String,
        exerciseEmoji: String,
        amount: Int,
        unit: ExerciseUnit,
        speedLevel: SpeedLevel,
        note: String = "",
        timestampMillis: Long = System.currentTimeMillis()
    ): Long {
        val safeName = InputSanitizer.sanitizeText(exerciseName, 50).ifBlank { "Vežba" }
        val safeNote = InputSanitizer.sanitizeText(note, 80)
        val safeAmount = InputSanitizer.sanitizeAmount(amount, unit)
        val dateKey = formatDateKey(timestampMillis)
        return runCatching {
            workoutLogDao.insert(
                WorkoutLogEntity(
                    exerciseId = exerciseId.coerceAtLeast(1L),
                    exerciseName = safeName,
                    exerciseEmoji = exerciseEmoji.ifBlank { "💪" },
                    amount = safeAmount,
                    unit = unit.name,
                    speedLevelName = speedLevel.name,
                    note = safeNote,
                    timestampMillis = timestampMillis,
                    dateKey = dateKey
                )
            )
        }.getOrDefault(0L)
    }

    suspend fun restoreLog(log: WorkoutLogItem) {
        runCatching {
            workoutLogDao.insert(
                WorkoutLogEntity(
                    id = log.id,
                    exerciseId = log.exerciseId,
                    exerciseName = InputSanitizer.sanitizeText(log.exerciseName, 50),
                    exerciseEmoji = log.exerciseEmoji,
                    amount = InputSanitizer.sanitizeAmount(log.amount, log.unit),
                    unit = log.unit.name,
                    speedLevelName = log.speedLevel.name,
                    note = InputSanitizer.sanitizeText(log.note, 80),
                    timestampMillis = log.timestampMillis,
                    dateKey = log.dateKey
                )
            )
        }
    }

    suspend fun deleteLog(id: Long) {
        runCatching { workoutLogDao.deleteById(id) }
    }

    suspend fun clearAllLogs() {
        runCatching { workoutLogDao.deleteAll() }
    }

    suspend fun addCustomExercise(
        name: String,
        emoji: String,
        unit: ExerciseUnit,
        defaultAmount: Int
    ): ExerciseItem? {
        val cleanName = InputSanitizer.sanitizeText(name, 40)
        if (cleanName.isBlank()) return null
        val safeAmount = InputSanitizer.sanitizeAmount(defaultAmount, unit)
        return runCatching {
            val count = exerciseDao.count()
            val entity = ExerciseEntity(
                name = cleanName,
                emoji = emoji.ifBlank { "💪" },
                unit = unit.name,
                defaultAmount = safeAmount,
                weeklyIncrement = 0,
                unitNoteSr = "",
                isCustom = true,
                sortOrder = count + 1
            )
            val id = exerciseDao.insert(entity)
            entity.copy(id = id).toDomain()
        }.getOrNull()
    }

    suspend fun deleteCustomExercise(id: Long) {
        runCatching { exerciseDao.deleteCustomById(id) }
    }

    suspend fun addTaskToQueue(
        exerciseId: Long,
        exerciseName: String,
        exerciseEmoji: String,
        amount: Int,
        unit: ExerciseUnit,
        customNote: String = ""
    ) {
        val safeName = InputSanitizer.sanitizeText(exerciseName, 40).ifBlank { "Vežba" }
        val safeNote = InputSanitizer.sanitizeText(customNote, 80)
        val safeAmount = InputSanitizer.sanitizeAmount(amount, unit)
        runCatching {
            val nextSort = queuedTaskDao.getMaxSortOrder() + 1
            queuedTaskDao.insert(
                QueuedTaskEntity(
                    exerciseId = exerciseId.coerceAtLeast(1L),
                    exerciseName = safeName,
                    exerciseEmoji = exerciseEmoji.ifBlank { "💪" },
                    amount = safeAmount,
                    unit = unit.name,
                    customNote = safeNote,
                    sortOrder = nextSort
                )
            )
        }
    }

    suspend fun removeQueuedTask(id: Long) {
        runCatching { queuedTaskDao.deleteById(id) }
    }

    suspend fun promoteQueuedTaskToActive(task: QueuedTaskItem) {
        preferencesManager.setNextTask(
            NextTask(
                exerciseId = task.exerciseId,
                exerciseName = InputSanitizer.sanitizeText(task.exerciseName, 40),
                exerciseEmoji = task.exerciseEmoji,
                targetAmount = InputSanitizer.sanitizeAmount(task.amount, task.unit),
                unit = task.unit,
                customText = InputSanitizer.sanitizeText(task.customNote, 80)
            )
        )
        runCatching { queuedTaskDao.deleteById(task.id) }
    }

    suspend fun completeCurrentTaskAndAdvance(
        currentTask: NextTask,
        speedLevel: SpeedLevel,
        planWeek: Int
    ): NextTask {
        logSet(
            exerciseId = currentTask.exerciseId,
            exerciseName = currentTask.exerciseName,
            exerciseEmoji = currentTask.exerciseEmoji,
            amount = currentTask.targetAmount,
            unit = currentTask.unit,
            speedLevel = speedLevel,
            note = currentTask.customText
        )

        val queued = runCatching { queuedTaskDao.getFirstOnce() }.getOrNull()
        val nextTask = if (queued != null) {
            runCatching { queuedTaskDao.deleteById(queued.id) }
            NextTask(
                exerciseId = queued.exerciseId,
                exerciseName = InputSanitizer.sanitizeText(queued.exerciseName, 40),
                exerciseEmoji = queued.exerciseEmoji,
                targetAmount = InputSanitizer.sanitizeAmount(
                    queued.amount,
                    ExerciseUnit.fromName(queued.unit)
                ),
                unit = ExerciseUnit.fromName(queued.unit),
                customText = InputSanitizer.sanitizeText(queued.customNote, 80)
            )
        } else {
            val allExercises = runCatching {
                exerciseDao.getAllOnce().map { it.toDomain() }
            }.getOrDefault(emptyList())
            if (allExercises.isEmpty()) {
                currentTask
            } else {
                val currentIdx = allExercises.indexOfFirst { it.id == currentTask.exerciseId }
                val nextExercise = allExercises[(currentIdx + 1) % allExercises.size]
                NextTask(
                    exerciseId = nextExercise.id,
                    exerciseName = nextExercise.name,
                    exerciseEmoji = nextExercise.emoji,
                    targetAmount = nextExercise.targetForWeek(planWeek),
                    unit = nextExercise.unit,
                    unitNoteSr = nextExercise.unitNoteSr,
                    customText = ""
                )
            }
        }
        preferencesManager.setNextTask(nextTask)
        return nextTask
    }

    suspend fun setPlanWeekAndSyncActive(week: Int) {
        val safeWeek = InputSanitizer.sanitizePlanWeek(week)
        preferencesManager.setPlanWeek(safeWeek)
        val current = preferencesManager.getNextTaskOnce()
        if (current.customText.isBlank()) {
            val match = runCatching {
                exerciseDao.getAllOnce().map { it.toDomain() }
            }.getOrDefault(emptyList()).firstOrNull { it.id == current.exerciseId }
            if (match != null) {
                preferencesManager.setNextTask(
                    current.copy(
                        targetAmount = match.targetForWeek(safeWeek),
                        unitNoteSr = match.unitNoteSr
                    )
                )
            }
        }
    }

    suspend fun setNextTask(task: NextTask) = preferencesManager.setNextTask(task)
    suspend fun setSpeedLevel(level: SpeedLevel) = preferencesManager.setSpeedLevel(level)
    suspend fun setDailyGoalReps(goal: Int) = preferencesManager.setDailyGoalReps(goal)
    suspend fun setSoundEnabled(enabled: Boolean) = preferencesManager.setSoundEnabled(enabled)
    suspend fun setVibrationEnabled(enabled: Boolean) = preferencesManager.setVibrationEnabled(enabled)
    suspend fun setPersistentBannerEnabled(enabled: Boolean) =
        preferencesManager.setPersistentBannerEnabled(enabled)

    suspend fun setSteadyReminderState(active: Boolean, intervalMinutes: Int, nextAtMillis: Long) =
        preferencesManager.setSteadyReminderState(active, intervalMinutes, nextAtMillis)

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        preferencesManager.setNotificationsEnabled(enabled)

    suspend fun updatePersonalProfile(ageYears: Int, heightCm: Int, weightKg: Int) =
        preferencesManager.updatePersonalProfile(ageYears, heightCm, weightKg)

    suspend fun toggleHealthHabit(habitId: String, done: Boolean) =
        preferencesManager.toggleHealthHabit(habitId, done)

    suspend fun getNextTaskOnce(): NextTask = preferencesManager.getNextTaskOnce()
    suspend fun getSettingsOnce(): ReminderSettings = preferencesManager.getReminderSettingsOnce()

    companion object {
        private val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun formatDateKey(timestampMillis: Long): String {
            return synchronized(keyFormat) {
                keyFormat.format(Date(timestampMillis))
            }
        }

        fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
            return formatDateKey(nowMillis)
        }

        fun calculateStreak(logs: List<WorkoutLogItem>, nowMillis: Long = System.currentTimeMillis()): Int {
            if (logs.isEmpty()) return 0
            val loggedDates = logs.map { it.dateKey }.toHashSet()

            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val todayKey = formatDateKey(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayKey = formatDateKey(cal.timeInMillis)

            val startOffset = when {
                loggedDates.contains(todayKey) -> 0
                loggedDates.contains(yesterdayKey) -> -1
                else -> return 0
            }

            var streak = 0
            val checkCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, startOffset)
            }
            while (loggedDates.contains(formatDateKey(checkCal.timeInMillis))) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            return streak
        }

        fun buildExerciseStats(
            exercises: List<ExerciseItem>,
            logs: List<WorkoutLogItem>,
            todayKey: String = todayDateKey()
        ): List<ExerciseProgressStats> {
            val logsByExercise = logs.groupBy { it.exerciseId }
            return exercises.map { ex ->
                val exLogs = logsByExercise[ex.id].orEmpty()
                ExerciseProgressStats(
                    exercise = ex,
                    todayTotal = exLogs.filter { it.dateKey == todayKey }.sumOf { it.amount },
                    allTimeTotal = exLogs.sumOf { it.amount },
                    personalRecord = exLogs.maxOfOrNull { it.amount } ?: 0,
                    totalSets = exLogs.size
                )
            }
        }

        fun buildHistoryGroups(
            logs: List<WorkoutLogItem>,
            nowMillis: Long = System.currentTimeMillis()
        ): List<DayHistoryGroup> {
            val calToday = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val todayKey = formatDateKey(calToday.timeInMillis)
            calToday.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayKey = formatDateKey(calToday.timeInMillis)

            val displayFormat = SimpleDateFormat("dd. MMMM yyyy.", Locale("sr", "RS"))

            return logs.groupBy { it.dateKey }
                .entries
                .sortedByDescending { it.key }
                .map { (dateKey, dayLogs) ->
                    val title = when (dateKey) {
                        todayKey -> "Danas ($dateKey)"
                        yesterdayKey -> "Juče ($dateKey)"
                        else -> {
                            val parsed = synchronized(keyFormat) { keyFormat.parse(dateKey) }
                            if (parsed != null) "${displayFormat.format(parsed)} ($dateKey)" else dateKey
                        }
                    }
                    DayHistoryGroup(
                        dateKey = dateKey,
                        displayTitle = title,
                        totalReps = dayLogs.filter { it.unit == ExerciseUnit.REPS }.sumOf { it.amount },
                        totalSeconds = dayLogs.filter { it.unit == ExerciseUnit.SECONDS }.sumOf { it.amount },
                        totalSets = dayLogs.size,
                        entries = dayLogs.sortedByDescending { it.timestampMillis }
                    )
                }
        }
    }
}
