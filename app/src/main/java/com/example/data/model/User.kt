package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    MANAGER,
    SALES_REP
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val name: String,
    val role: UserRole,
    val avatarColorHex: String = "#FF1E88E5" // Hex representation
)
