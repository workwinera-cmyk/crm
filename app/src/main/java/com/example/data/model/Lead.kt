package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val company: String,
    val email: String,
    val phone: String,
    val status: String, // NEW, CONTACTED, QUALIFIED, PROPOSAL, WON, LOST
    val value: Double,
    val assigneeEmail: String, // Assigned sales rep/user
    val tagsAsString: String = "", // Comma-separated tags
    val notes: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val dateUpdated: Long = System.currentTimeMillis(),
    val nextFollowUp: Long? = null
) {
    val tags: List<String>
        get() = if (tagsAsString.isBlank()) emptyList() else tagsAsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
