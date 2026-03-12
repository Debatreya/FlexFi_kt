package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records an external payment between two users.
 * This adjusts group balances by crediting fromPhone and debiting toPhone.
 */
@Entity(tableName = "settlement_records")
data class SettlementRecordEntity(
    @PrimaryKey
    val id: String,
    val groupId: String?,       // null if cross-group / global settlement
    val fromPhone: String,      // who paid
    val toPhone: String,        // who received
    val amount: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
