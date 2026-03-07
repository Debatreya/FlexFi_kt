package com.example.flexfi.domain.models

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val date: Long = 0L
)
