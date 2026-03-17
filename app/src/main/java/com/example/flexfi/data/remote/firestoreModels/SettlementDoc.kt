package com.example.flexfi.data.remote.firestoreModels

data class SettlementDoc(
    val id: String = "",
    val groupId: String? = null,
    val fromPhone: String = "",
    val toPhone: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)
