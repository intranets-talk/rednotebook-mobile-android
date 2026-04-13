package com.example.rednotebook.data.local

import androidx.room.*

@Dao
interface EntryDao {

    @Query("SELECT * FROM entries WHERE year = :year AND month = :month ORDER BY day ASC")
    suspend fun getMonthEntries(year: Int, month: Int): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE year = :year AND month = :month AND day = :day")
    suspend fun getEntry(year: Int, month: Int, day: Int): EntryEntity?

    @Query("SELECT DISTINCT year, month FROM entries ORDER BY year ASC, month ASC")
    suspend fun getAvailableMonths(): List<MonthRef>

    @Query("SELECT * FROM entries WHERE text LIKE '%' || :query || '%' ORDER BY year DESC, month DESC, day DESC")
    suspend fun search(query: String): List<EntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: EntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<EntryEntity>)

    @Query("DELETE FROM entries WHERE year = :year AND month = :month AND day = :day")
    suspend fun delete(year: Int, month: Int, day: Int)

    @Query("DELETE FROM entries WHERE year = :year AND month = :month")
    suspend fun deleteMonth(year: Int, month: Int)
}

// Lightweight projection for getAvailableMonths()
data class MonthRef(val year: Int, val month: Int)
