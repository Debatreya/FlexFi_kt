package com.example.flexfi.domain.models

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val paidByPhone: String = "",
    val category: String = "",
    val createdAt: Long = 0L
)
