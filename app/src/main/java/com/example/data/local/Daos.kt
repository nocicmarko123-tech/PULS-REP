package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllOnce(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExerciseEntity>): List<Long>

    @Query("DELETE FROM exercises WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomById(id: Long)
}

@Dao
interface WorkoutLogDao {
    @Query("SELECT * FROM workout_logs ORDER BY timestampMillis DESC")
    fun observeAllLogs(): Flow<List<WorkoutLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkoutLogEntity): Long

    @Query("DELETE FROM workout_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workout_logs")
    suspend fun deleteAll()
}

@Dao
interface QueuedTaskDao {
    @Query("SELECT * FROM queued_tasks ORDER BY sortOrder ASC, id ASC")
    fun observeQueue(): Flow<List<QueuedTaskEntity>>

    @Query("SELECT * FROM queued_tasks ORDER BY sortOrder ASC, id ASC LIMIT 1")
    suspend fun getFirstOnce(): QueuedTaskEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM queued_tasks")
    suspend fun getMaxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QueuedTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<QueuedTaskEntity>)

    @Query("DELETE FROM queued_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
