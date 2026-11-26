package com.example

import android.app.Application
import com.example.data.local.PreferencesManager
import com.example.data.local.RepPulsDatabase
import com.example.data.local.WorkoutRepository
import com.example.notifications.SoundAndVibrationHelper
import com.example.notifications.WorkoutNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RepPulsApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: RepPulsDatabase
        private set
    lateinit var preferencesManager: PreferencesManager
        private set
    lateinit var repository: WorkoutRepository
        private set
    lateinit var notificationHelper: WorkoutNotificationHelper
        private set
    lateinit var soundHelper: SoundAndVibrationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        database = RepPulsDatabase.getInstance(this)
        preferencesManager = PreferencesManager(this)
        repository = WorkoutRepository(
            exerciseDao = database.exerciseDao(),
            workoutLogDao = database.workoutLogDao(),
            queuedTaskDao = database.queuedTaskDao(),
            preferencesManager = preferencesManager
        )
        notificationHelper = WorkoutNotificationHelper(this)
        soundHelper = SoundAndVibrationHelper(this)

        notificationHelper.createNotificationChannels()

        appScope.launch {
            repository.ensureSeeded()
        }
    }
}
