package com.example.flexfi.data.remote.firestoreModels

data class ExpenseDoc(
    val id: String = "",
    val groupId: String? = null,
    val title: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val category: String = "",
    val createdAt: Long = 0
)
