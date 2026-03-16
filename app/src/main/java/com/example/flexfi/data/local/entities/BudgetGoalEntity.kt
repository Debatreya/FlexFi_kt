package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_goals")
data class BudgetGoalEntity(
    @PrimaryKey
    val id: String,
    val userPhone: String,
    val title: String,
    val description: String = "",
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val targetDate: Long? = null,
    val autoSaveAmount: Double = 0.0,
    val autoSaveFrequency: String = "monthly", // "weekly", "monthly"
    val isSinkingFund: Boolean = false, // True if this goal is a sinking fund (saving for a specific date/expense)
    val lastNotifiedMilestone: Int = 0, // Records highest milestone notified (e.g., 25, 50, 75, 100)
    val createdAt: Long = System.currentTimeMillis()
)
