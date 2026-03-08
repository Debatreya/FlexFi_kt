package com.example.flexfi.data.remote.firestoreModels

data class UserDoc(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String? = null,
    val joinedAt: Long = 0,
    val streakCount: Int = 0,
    val totalExpense: Double = 0.0
)
