package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val createdBy: String,
    val isGhost: Boolean,
    val linkedUserId: String?
)
