package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val email: String?,
    val joinedAt: Long,
    val streakCount: Int,
    val totalExpense: Double
)
