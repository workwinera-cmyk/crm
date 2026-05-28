package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leadId: Int,
    val leadName: String,
    val title: String,
    val note: String = "",
    val triggerTime: Long,
    val isCompleted: Boolean = false,
    val isNotified: Boolean = false
)
