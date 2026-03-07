package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastUpdated: Long
)
