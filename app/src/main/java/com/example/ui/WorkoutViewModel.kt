package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.WorkoutRepository
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
import com.example.notifications.SoundAndVibrationHelper
import com.example.notifications.WorkoutNotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RestTimerState(
    val totalSeconds: Int = 45,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isCompletedAlertVisible: Boolean = false
) {
    val isActiveOrPaused: Boolean
        get() = isRunning || (isPaused && remainingSeconds > 0)

    val progressFraction: Float
        get() = if (totalSeconds > 0) {
            (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

data class TempoGuideState(
    val isActive: Boolean = false,
    val phaseLabelSr: String = "Spreman za ritam",
    val phaseSecondsRemaining: Int = 0,
    val guidedReps: Int = 0
)

data class WorkoutUiState(
    val exercises: List<ExerciseItem> = emptyList(),
    val logs: List<WorkoutLogItem> = emptyList(),
    val queue: List<QueuedTaskItem> = emptyList(),
    val nextTask: NextTask = NextTask(
        exerciseId = 1L,
        exerciseName = "Sklekovi",
        exerciseEmoji = "💪",
        targetAmount = 12,
        unit = ExerciseUnit.REPS
    ),
    val settings: ReminderSettings = ReminderSettings(),
    val todayReps: Int = 0,
    val todaySeconds: Int = 0,
    val todaySets: Int = 0,
    val allTimeReps: Int = 0,
    val allTimeSeconds: Int = 0,
    val streakDays: Int = 0,
    val exerciseStats: List<ExerciseProgressStats> = emptyList(),
    val historyGroups: List<DayHistoryGroup> = emptyList(),
    val restTimer: RestTimerState = RestTimerState(),
    val tempoGuide: TempoGuideState = TempoGuideState(),
    val hasNotificationPermission: Boolean = false,
    val personalProfile: PersonalProfile = PersonalProfile(),
    val healthChecklist: DailyHealthChecklist = DailyHealthChecklist(),
    val secondsUntilNextReminder: Long = 0L,
    val steadyReminderAlertVisible: Boolean = false
)

class WorkoutViewModel(
    private val repository: WorkoutRepository,
    private val notificationHelper: WorkoutNotificationHelper,
    private val soundHelper: SoundAndVibrationHelper
) : ViewModel() {

    private val restTimerFlow = MutableStateFlow(RestTimerState())
    private val tempoGuideFlow = MutableStateFlow(TempoGuideState())
    private val permissionFlow = MutableStateFlow(notificationHelper.hasNotificationPermission())
    private val secondsUntilReminderFlow = MutableStateFlow(0L)
    private val steadyAlertVisibleFlow = MutableStateFlow(false)
    private val latestSettingsFlow = MutableStateFlow(ReminderSettings())

    private var restTimerJob: Job? = null
    private var tempoJob: Job? = null
    private var reminderTickerJob: Job? = null

    private data class CoreDataBundle(
        val exercises: List<ExerciseItem>,
        val logs: List<WorkoutLogItem>,
        val queue: List<QueuedTaskItem>,
        val nextTask: NextTask,
        val settings: ReminderSettings
    )

    private data class AuxiliaryStateBundle(
        val restTimer: RestTimerState,
        val tempoGuide: TempoGuideState,
        val hasPermission: Boolean,
        val profile: PersonalProfile,
        val checklist: DailyHealthChecklist,
        val secondsUntilReminder: Long,
        val steadyAlertVisible: Boolean
    )

    private val coreBundleFlow = combine(
        repository.exercisesFlow,
        repository.logsFlow,
        repository.queueFlow,
        repository.nextTaskFlow,
        repository.settingsFlow
    ) { exercises, logs, queue, nextTask, settings ->
        CoreDataBundle(exercises, logs, queue, nextTask, settings)
    }

    private val healthBundleFlow = combine(
        repository.personalProfileFlow,
        repository.dailyHealthChecklistFlow
    ) { profile, checklist ->
        profile to checklist
    }

    private val reminderUiFlow = combine(
        secondsUntilReminderFlow,
        steadyAlertVisibleFlow
    ) { secondsLeft, alertVisible ->
        secondsLeft to alertVisible
    }

    private val auxBundleFlow = combine(
        restTimerFlow,
        tempoGuideFlow,
        permissionFlow,
        healthBundleFlow,
        reminderUiFlow
    ) { restTimer, tempoGuide, hasPermission, (profile, checklist), (secondsLeft, alertVisible) ->
        AuxiliaryStateBundle(
            restTimer = restTimer,
            tempoGuide = tempoGuide,
            hasPermission = hasPermission,
            profile = profile,
            checklist = checklist,
            secondsUntilReminder = secondsLeft,
            steadyAlertVisible = alertVisible
        )
    }

    val uiState: StateFlow<WorkoutUiState> = combine(
        coreBundleFlow,
        auxBundleFlow
    ) { core, aux ->
        val todayKey = WorkoutRepository.todayDateKey()
        val todayLogs = core.logs.filter { it.dateKey == todayKey }

        WorkoutUiState(
            exercises = core.exercises,
            logs = core.logs,
            queue = core.queue,
            nextTask = core.nextTask,
            settings = core.settings,
            todayReps = todayLogs.filter { it.unit == ExerciseUnit.REPS }.sumOf { it.amount },
            todaySeconds = todayLogs.filter { it.unit == ExerciseUnit.SECONDS }.sumOf { it.amount },
            todaySets = todayLogs.size,
            allTimeReps = core.logs.filter { it.unit == ExerciseUnit.REPS }.sumOf { it.amount },
            allTimeSeconds = core.logs.filter { it.unit == ExerciseUnit.SECONDS }.sumOf { it.amount },
            streakDays = WorkoutRepository.calculateStreak(core.logs),
            exerciseStats = WorkoutRepository.buildExerciseStats(core.exercises, core.logs, todayKey),
            historyGroups = WorkoutRepository.buildHistoryGroups(core.logs),
            restTimer = aux.restTimer,
            tempoGuide = aux.tempoGuide,
            hasNotificationPermission = aux.hasPermission,
            personalProfile = aux.profile,
            healthChecklist = aux.checklist,
            secondsUntilNextReminder = aux.secondsUntilReminder,
            steadyReminderAlertVisible = aux.steadyAlertVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutUiState(
            hasNotificationPermission = notificationHelper.hasNotificationPermission()
        )
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            startReminderCountdownTicker()
            repository.settingsFlow.collect { settings ->
                latestSettingsFlow.value = settings
                updateReminderCountdownSnapshot(settings)
            }
        }
    }

    private fun startReminderCountdownTicker() {
        reminderTickerJob?.cancel()
        reminderTickerJob = viewModelScope.launch {
            while (true) {
                val settings = latestSettingsFlow.value
                if (!settings.steadyReminderActive || settings.reminderIntervalMinutes <= 0) {
                    secondsUntilReminderFlow.value = 0L
                } else {
                    val now = System.currentTimeMillis()
                    val targetAt = settings.nextReminderAtMillis
                    if (targetAt <= 0L) {
                        val nextAt = now + settings.reminderIntervalMinutes * 60_000L
                        repository.setSteadyReminderState(
                            active = true,
                            intervalMinutes = settings.reminderIntervalMinutes,
                            nextAtMillis = nextAt
                        )
                        secondsUntilReminderFlow.value = settings.reminderIntervalMinutes * 60L
                    } else if (now >= targetAt) {
                        onSteadyReminderFired(settings)
                    } else {
                        secondsUntilReminderFlow.value = ((targetAt - now) + 999L) / 1000L
                    }
                }
                delay(1_000L)
            }
        }
    }

    private fun updateReminderCountdownSnapshot(settings: ReminderSettings) {
        if (!settings.steadyReminderActive || settings.reminderIntervalMinutes <= 0 || settings.nextReminderAtMillis <= 0L) {
            secondsUntilReminderFlow.value = 0L
        } else {
            val remainingMs = (settings.nextReminderAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            secondsUntilReminderFlow.value = (remainingMs + 999L) / 1000L
        }
    }

    private suspend fun onSteadyReminderFired(settings: ReminderSettings) {
        steadyAlertVisibleFlow.value = true
        soundHelper.playRestCompletedCue(
            soundEnabled = settings.soundEnabled,
            vibrationEnabled = settings.vibrationEnabled
        )
        if (settings.notificationsEnabled && notificationHelper.hasNotificationPermission()) {
            notificationHelper.showNextTaskReminderNotification(
                nextTask = uiState.value.nextTask,
                speedLevel = settings.speedLevel
            )
        }
        val nextAt = System.currentTimeMillis() + settings.reminderIntervalMinutes * 60_000L
        repository.setSteadyReminderState(
            active = true,
            intervalMinutes = settings.reminderIntervalMinutes,
            nextAtMillis = nextAt
        )
        secondsUntilReminderFlow.value = settings.reminderIntervalMinutes * 60L
        if (settings.notificationsEnabled && notificationHelper.hasNotificationPermission()) {
            notificationHelper.scheduleReminderAlarm(settings.reminderIntervalMinutes)
        }
    }

    fun refreshNotificationPermission() {
        permissionFlow.value = notificationHelper.hasNotificationPermission()
    }

    fun selectSpeedLevel(level: SpeedLevel) {
        viewModelScope.launch {
            repository.setSpeedLevel(level)
            if (tempoGuideFlow.value.isActive) {
                restartTempoGuide(level)
            }
        }
    }

    fun completeNextStep() {
        val currentState = uiState.value
        val currentTask = currentState.nextTask
        val speed = currentState.settings.speedLevel

        stopTempoGuide()
        steadyAlertVisibleFlow.value = false
        soundHelper.playSetLoggedCue(
            soundEnabled = currentState.settings.soundEnabled,
            vibrationEnabled = currentState.settings.vibrationEnabled
        )

        viewModelScope.launch {
            repository.completeCurrentTaskAndAdvance(
                currentTask = currentTask,
                speedLevel = speed,
                planWeek = currentState.settings.planWeek
            )
            startRestTimer(speed.defaultRestSeconds)
        }
    }

    fun setPlanWeek(week: Int) {
        val safeWeek = InputSanitizer.sanitizePlanWeek(week)
        viewModelScope.launch {
            repository.setPlanWeekAndSyncActive(safeWeek)
        }
    }

    fun quickLogExercise(
        exercise: ExerciseItem,
        amount: Int,
        startRestAfter: Boolean = false
    ) {
        val safeAmount = InputSanitizer.sanitizeAmount(amount, exercise.unit)
        val settings = uiState.value.settings
        soundHelper.playSetLoggedCue(
            soundEnabled = settings.soundEnabled,
            vibrationEnabled = settings.vibrationEnabled
        )
        viewModelScope.launch {
            repository.logSet(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                exerciseEmoji = exercise.emoji,
                amount = safeAmount,
                unit = exercise.unit,
                speedLevel = settings.speedLevel
            )
            if (startRestAfter) {
                startRestTimer(settings.speedLevel.defaultRestSeconds)
            }
        }
    }

    fun startRestTimer(seconds: Int = uiState.value.settings.speedLevel.defaultRestSeconds) {
        val target = seconds.coerceIn(5, 600)
        restTimerJob?.cancel()
        restTimerFlow.value = RestTimerState(
            totalSeconds = target,
            remainingSeconds = target,
            isRunning = true,
            isPaused = false,
            isCompletedAlertVisible = false
        )
        launchCountdownLoop()
    }

    fun togglePauseRestTimer() {
        val current = restTimerFlow.value
        if (current.isRunning) {
            restTimerJob?.cancel()
            restTimerFlow.value = current.copy(isRunning = false, isPaused = true)
        } else if (current.isPaused && current.remainingSeconds > 0) {
            restTimerFlow.value = current.copy(isRunning = true, isPaused = false)
            launchCountdownLoop()
        } else {
            startRestTimer(uiState.value.settings.speedLevel.defaultRestSeconds)
        }
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val current = restTimerFlow.value
        val baseRemaining = if (current.isActiveOrPaused) {
            current.remainingSeconds
        } else {
            uiState.value.settings.speedLevel.defaultRestSeconds
        }
        val updatedRemaining = (baseRemaining + deltaSeconds).coerceIn(5, 600)
        val updatedTotal = maxOf(current.totalSeconds, updatedRemaining)
        restTimerFlow.value = current.copy(
            totalSeconds = updatedTotal,
            remainingSeconds = updatedRemaining,
            isCompletedAlertVisible = false
        )
    }

    fun resetRestTimer() {
        val defaultSeconds = uiState.value.settings.speedLevel.defaultRestSeconds
        restTimerJob?.cancel()
        restTimerFlow.value = RestTimerState(
            totalSeconds = defaultSeconds,
            remainingSeconds = defaultSeconds,
            isRunning = false,
            isPaused = true,
            isCompletedAlertVisible = false
        )
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        restTimerFlow.value = RestTimerState(
            totalSeconds = uiState.value.settings.speedLevel.defaultRestSeconds,
            remainingSeconds = 0,
            isRunning = false,
            isPaused = false,
            isCompletedAlertVisible = false
        )
    }

    fun dismissRestCompletedAlert() {
        restTimerFlow.value = restTimerFlow.value.copy(isCompletedAlertVisible = false)
    }

    fun dismissSteadyReminderAlert() {
        steadyAlertVisibleFlow.value = false
    }

    private fun launchCountdownLoop() {
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (restTimerFlow.value.isRunning && restTimerFlow.value.remainingSeconds > 0) {
                delay(1_000L)
                val current = restTimerFlow.value
                if (!current.isRunning) break
                val nextRemaining = (current.remainingSeconds - 1).coerceAtLeast(0)
                if (nextRemaining == 0) {
                    onRestTimerFinished(current.totalSeconds)
                    break
                } else {
                    restTimerFlow.value = current.copy(remainingSeconds = nextRemaining)
                }
            }
        }
    }

    private fun onRestTimerFinished(totalSeconds: Int) {
        val state = uiState.value
        restTimerFlow.value = RestTimerState(
            totalSeconds = totalSeconds,
            remainingSeconds = 0,
            isRunning = false,
            isPaused = false,
            isCompletedAlertVisible = true
        )
        soundHelper.playRestCompletedCue(
            soundEnabled = state.settings.soundEnabled,
            vibrationEnabled = state.settings.vibrationEnabled
        )
        if (state.settings.notificationsEnabled) {
            notificationHelper.showRestCompletedNotification(
                nextTask = state.nextTask,
                speedLevel = state.settings.speedLevel
            )
        }
    }

    fun toggleTempoGuide() {
        if (tempoGuideFlow.value.isActive) {
            stopTempoGuide()
        } else {
            restartTempoGuide(uiState.value.settings.speedLevel)
        }
    }

    private fun stopTempoGuide() {
        tempoJob?.cancel()
        tempoGuideFlow.value = TempoGuideState(isActive = false)
    }

    private fun restartTempoGuide(level: SpeedLevel) {
        tempoJob?.cancel()
        tempoJob = viewModelScope.launch {
            var reps = tempoGuideFlow.value.guidedReps
            while (true) {
                for (sec in level.downSeconds downTo 1) {
                    tempoGuideFlow.value = TempoGuideState(
                        isActive = true,
                        phaseLabelSr = "Spuštanje (kontrolisano)",
                        phaseSecondsRemaining = sec,
                        guidedReps = reps
                    )
                    soundHelper.playTempoTick(uiState.value.settings.soundEnabled)
                    delay(1_000L)
                }
                if (level.holdSeconds > 0) {
                    for (sec in level.holdSeconds downTo 1) {
                        tempoGuideFlow.value = TempoGuideState(
                            isActive = true,
                            phaseLabelSr = "Zadržavanje u donjoj tački",
                            phaseSecondsRemaining = sec,
                            guidedReps = reps
                        )
                        delay(1_000L)
                    }
                }
                for (sec in level.upSeconds downTo 1) {
                    tempoGuideFlow.value = TempoGuideState(
                        isActive = true,
                        phaseLabelSr = "Podizanje (snaga)",
                        phaseSecondsRemaining = sec,
                        guidedReps = reps
                    )
                    soundHelper.playTempoTick(uiState.value.settings.soundEnabled)
                    delay(1_000L)
                }
                reps++
            }
        }
    }

    fun setCustomNextTask(
        exercise: ExerciseItem?,
        customText: String,
        targetAmount: Int
    ) {
        val cleanText = InputSanitizer.sanitizeText(customText, 80)
        viewModelScope.launch {
            val selected = exercise ?: uiState.value.exercises.firstOrNull()
            val task = if (selected != null) {
                NextTask(
                    exerciseId = selected.id,
                    exerciseName = selected.name,
                    exerciseEmoji = selected.emoji,
                    targetAmount = InputSanitizer.sanitizeAmount(targetAmount, selected.unit),
                    unit = selected.unit,
                    unitNoteSr = selected.unitNoteSr,
                    customText = cleanText
                )
            } else {
                NextTask(
                    exerciseId = 1L,
                    exerciseName = "Trening",
                    exerciseEmoji = "💪",
                    targetAmount = InputSanitizer.sanitizeAmount(targetAmount, ExerciseUnit.REPS),
                    unit = ExerciseUnit.REPS,
                    customText = cleanText
                )
            }
            repository.setNextTask(task)
        }
    }

    fun setExerciseAsNext(exercise: ExerciseItem, amount: Int = exercise.defaultAmount) {
        val safeAmount = InputSanitizer.sanitizeAmount(amount, exercise.unit)
        viewModelScope.launch {
            repository.setNextTask(
                NextTask(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    exerciseEmoji = exercise.emoji,
                    targetAmount = safeAmount,
                    unit = exercise.unit,
                    unitNoteSr = exercise.unitNoteSr,
                    customText = ""
                )
            )
        }
    }

    fun setExerciseAsNextForWeek(exercise: ExerciseItem) {
        val week = uiState.value.settings.planWeek
        setExerciseAsNext(exercise, exercise.targetForWeek(week))
    }

    fun addToQueue(
        exercise: ExerciseItem?,
        amount: Int,
        customNote: String
    ) {
        val cleanNote = InputSanitizer.sanitizeText(customNote, 80)
        viewModelScope.launch {
            val selected = exercise ?: uiState.value.exercises.firstOrNull()
            val unit = selected?.unit ?: ExerciseUnit.REPS
            repository.addTaskToQueue(
                exerciseId = selected?.id ?: 1L,
                exerciseName = selected?.name ?: "Vežba",
                exerciseEmoji = selected?.emoji ?: "💪",
                amount = InputSanitizer.sanitizeAmount(amount, unit),
                unit = unit,
                customNote = cleanNote
            )
        }
    }

    fun removeFromQueue(id: Long) {
        viewModelScope.launch {
            repository.removeQueuedTask(id)
        }
    }

    fun promoteFromQueue(item: QueuedTaskItem) {
        viewModelScope.launch {
            repository.promoteQueuedTaskToActive(item)
        }
    }

    fun addCustomExercise(
        name: String,
        emoji: String,
        unit: ExerciseUnit,
        defaultAmount: Int
    ) {
        val cleanName = InputSanitizer.sanitizeText(name, 40)
        if (cleanName.isBlank()) return
        val safeAmount = InputSanitizer.sanitizeAmount(defaultAmount, unit)
        viewModelScope.launch {
            repository.addCustomExercise(
                name = cleanName,
                emoji = emoji.ifBlank { "💪" },
                unit = unit,
                defaultAmount = safeAmount
            )
        }
    }

    fun deleteCustomExercise(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomExercise(id)
        }
    }

    fun deleteLog(log: WorkoutLogItem) {
        viewModelScope.launch {
            repository.deleteLog(log.id)
        }
    }

    fun restoreLog(log: WorkoutLogItem) {
        viewModelScope.launch {
            repository.restoreLog(log)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }

    fun updateDailyGoal(goal: Int) {
        val safeGoal = InputSanitizer.sanitizeDailyGoal(goal)
        viewModelScope.launch {
            repository.setDailyGoalReps(safeGoal)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setVibrationEnabled(enabled) }
    }

    fun setPersistentBannerEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setPersistentBannerEnabled(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationsEnabled(enabled)
            val current = latestSettingsFlow.value
            if (enabled &&
                current.steadyReminderActive &&
                current.reminderIntervalMinutes > 0 &&
                notificationHelper.hasNotificationPermission()
            ) {
                notificationHelper.scheduleReminderAlarm(current.reminderIntervalMinutes)
            } else {
                notificationHelper.cancelReminderAlarm()
            }
        }
    }

    fun toggleSteadyReminder(active: Boolean) {
        viewModelScope.launch {
            val current = latestSettingsFlow.value
            val fallbackInterval = if (current.reminderIntervalMinutes > 0) {
                current.reminderIntervalMinutes
            } else {
                180
            }
            if (active) {
                val safeInterval = InputSanitizer.sanitizeReminderIntervalMinutes(fallbackInterval)
                val nextAt = System.currentTimeMillis() + safeInterval * 60_000L
                repository.setSteadyReminderState(
                    active = true,
                    intervalMinutes = safeInterval,
                    nextAtMillis = nextAt
                )
                secondsUntilReminderFlow.value = safeInterval * 60L
                if (current.notificationsEnabled && notificationHelper.hasNotificationPermission()) {
                    notificationHelper.scheduleReminderAlarm(safeInterval)
                }
            } else {
                steadyAlertVisibleFlow.value = false
                secondsUntilReminderFlow.value = 0L
                notificationHelper.cancelReminderAlarm()
                repository.setSteadyReminderState(
                    active = false,
                    intervalMinutes = fallbackInterval,
                    nextAtMillis = 0L
                )
            }
        }
    }

    fun setSteadyReminderInterval(minutes: Int) {
        val safeMinutes = InputSanitizer.sanitizeReminderIntervalMinutes(minutes)
        viewModelScope.launch {
            if (safeMinutes <= 0) {
                steadyAlertVisibleFlow.value = false
                secondsUntilReminderFlow.value = 0L
                notificationHelper.cancelReminderAlarm()
                repository.setSteadyReminderState(
                    active = false,
                    intervalMinutes = 0,
                    nextAtMillis = 0L
                )
            } else {
                val nextAt = System.currentTimeMillis() + safeMinutes * 60_000L
                steadyAlertVisibleFlow.value = false
                secondsUntilReminderFlow.value = safeMinutes * 60L
                repository.setSteadyReminderState(
                    active = true,
                    intervalMinutes = safeMinutes,
                    nextAtMillis = nextAt
                )
                if (latestSettingsFlow.value.notificationsEnabled &&
                    notificationHelper.hasNotificationPermission()
                ) {
                    notificationHelper.scheduleReminderAlarm(safeMinutes)
                } else {
                    notificationHelper.cancelReminderAlarm()
                }
            }
        }
    }

    fun updatePersonalProfile(ageYears: Int, heightCm: Int, weightKg: Int) {
        viewModelScope.launch {
            repository.updatePersonalProfile(
                ageYears = ageYears.coerceIn(10, 99),
                heightCm = heightCm.coerceIn(110, 230),
                weightKg = weightKg.coerceIn(25, 220)
            )
        }
    }

    fun toggleHealthHabit(habitId: String, done: Boolean) {
        viewModelScope.launch {
            repository.toggleHealthHabit(habitId, done)
        }
    }

    fun sendTestNotification(): Boolean {
        refreshNotificationPermission()
        if (!notificationHelper.hasNotificationPermission()) return false
        val state = uiState.value
        notificationHelper.showNextTaskReminderNotification(
            nextTask = state.nextTask,
            speedLevel = state.settings.speedLevel
        )
        return true
    }

    override fun onCleared() {
        super.onCleared()
        restTimerJob?.cancel()
        tempoJob?.cancel()
        reminderTickerJob?.cancel()
    }

    class Factory(
        private val repository: WorkoutRepository,
        private val notificationHelper: WorkoutNotificationHelper,
        private val soundHelper: SoundAndVibrationHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutViewModel(
                repository = repository,
                notificationHelper = notificationHelper,
                soundHelper = soundHelper
            ) as T
        }
    }
}
