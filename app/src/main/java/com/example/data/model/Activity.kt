package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leadId: Int,
    val title: String,
    val description: String,
    val type: String, // CREATION, STATUS, CALL, WHATSAPP, EMAIL, NOTE
    val timestamp: Long = System.currentTimeMillis(),
    val actorName: String = "Staff Member"
)
