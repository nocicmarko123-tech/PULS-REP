package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutLogEntity::class,
        QueuedTaskEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RepPulsDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun queuedTaskDao(): QueuedTaskDao

    companion object {
        @Volatile
        private var INSTANCE: RepPulsDatabase? = null

        fun getInstance(context: Context): RepPulsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RepPulsDatabase::class.java,
                    "reppuls_workout_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
