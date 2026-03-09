package com.example.flexfi.data.remote.firestoreModels

data class SplitDoc(
    val phone: String = "",
    val share: Double = 0.0
)

data class ExpenseDoc(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val paidByPhone: String = "",
    val category: String = "",
    val createdAt: Long = 0,
    val splits: List<SplitDoc> = emptyList()
)
