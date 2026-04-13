package com.example.rednotebook.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OpType { UPSERT, DELETE }

@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val opType: OpType,
    val year: Int,
    val month: Int,
    val day: Int,
    val text: String = "",         // empty for DELETE ops
    val createdAt: Long = System.currentTimeMillis()
)
