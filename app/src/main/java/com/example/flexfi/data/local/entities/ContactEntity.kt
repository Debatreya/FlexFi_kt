package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val createdBy: String, // phone number of logged-in user (NOT firebase UID)
    val isGhost: Boolean,
    val linkedUserId: String?,
    val createdAt: Long
)
