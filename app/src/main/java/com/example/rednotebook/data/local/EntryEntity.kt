package com.example.rednotebook.data.local

import androidx.room.Entity

@Entity(tableName = "entries", primaryKeys = ["year", "month", "day"])
data class EntryEntity(
    val year: Int,
    val month: Int,
    val day: Int,
    val text: String,
    val date: String,          // e.g. "2025-04-08"
    val isSynced: Boolean = true
)
