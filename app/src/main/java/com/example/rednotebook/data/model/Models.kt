package com.example.rednotebook.data.model

data class Entry(
    val date: String,
    val day: Int,
    val month: Int,
    val year: Int,
    val text: String,
    val has_content: Boolean
)

data class EntryBody(val text: String)

data class MonthRef(val year: Int, val month: Int)
