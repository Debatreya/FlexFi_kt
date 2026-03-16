package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single personal expense record.
 * Generated automatically from group expense splits — never synced to Firestore.
 */
@Entity(tableName = "personal_expenses")
data class PersonalExpenseEntity(
    @PrimaryKey
    val id: String,
    val userPhone: String,
    val amount: Double, // Original amount in original currency
    val currency: String = "INR",
    val exchangeRateToBase: Double = 1.0,
    val baseAmount: Double = 0.0, // Amount in USD
    val category: String,
    val subCategory: String? = null,
    val type: String = "EXPENSE", // "INCOME", "EXPENSE", "TRANSFER"
    val paymentMode: String = "Cash", // "Cash", "Credit Card", "Debit Card", "UPI"
    val merchant: String? = null,
    val tags: String? = null, // Comma-separated tags e.g. "#Vacation2024,#WorkRelated"
    val attachmentUri: String? = null, // Local URI to receipt photo
    val source: String, // "GROUP" or "PERSONAL"
    val sourceExpenseId: String?,
    val sourceGroupId: String?,
    val description: String?,
    val createdAt: Long
)
