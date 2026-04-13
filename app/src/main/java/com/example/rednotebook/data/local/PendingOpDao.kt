package com.example.rednotebook.data.local

import androidx.room.*

@Dao
interface PendingOpDao {

    @Query("SELECT * FROM pending_ops ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingOpEntity>

    @Insert
    suspend fun insert(op: PendingOpEntity)

    @Delete
    suspend fun delete(op: PendingOpEntity)

    @Query("DELETE FROM pending_ops WHERE year = :year AND month = :month AND day = :day")
    suspend fun deleteForDay(year: Int, month: Int, day: Int)

    @Query("SELECT COUNT(*) FROM pending_ops")
    suspend fun count(): Int
}
