package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 0, // Only one row for settings
    val isDarkMode: Boolean = false,
    val displayCurrency: String = "INR",
    val monthlyIncome: Double = 0.0,
    val currentBankBalance: Double = 0.0,
    val profilePhotoUri: String? = null
)
