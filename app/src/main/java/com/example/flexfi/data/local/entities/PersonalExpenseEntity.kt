package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single personal expense record.
 * Generated automatically from group expense splits — never synced to Firestore.
 *
 * [source] is "GROUP" when generated from a split, "PERSONAL" for future direct entries.
 * [sourceExpenseId] links back to the exact [ExpenseEntity] that produced this record,
 * enabling clean deletion when a single expense is removed.
 */
@Entity(tableName = "personal_expenses")
data class PersonalExpenseEntity(

    @PrimaryKey
    val id: String,

    /** Phone number of the user this expense belongs to */
    val userPhone: String,

    /** This user's share of the expense */
    val amount: Double,

    /** e.g. "Food", "Transport", "Shopping", "Other" */
    val category: String,

    /** "GROUP" or "PERSONAL" */
    val source: String,

    /** Links back to the exact ExpenseEntity that generated this record */
    val sourceExpenseId: String?,

    /** Null when source == "PERSONAL" */
    val sourceGroupId: String?,

    /** Expense title / description */
    val description: String?,

    val createdAt: Long
)
