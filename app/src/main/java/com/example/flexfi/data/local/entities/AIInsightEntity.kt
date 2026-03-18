package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_insights")
data class AIInsightEntity(
    @PrimaryKey
    val id: String,
    val insights: String,                  // JSON serialized List<String>
    val dataHash: Long,                    // Hash of input data (for cache matching)
    val generatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L               // currentTimeMillis() + 24h
)
