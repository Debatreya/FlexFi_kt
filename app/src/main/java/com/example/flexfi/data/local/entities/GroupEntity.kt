package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdBy: String,
    val createdAt: Long,
    val totalExpense: Double
)
