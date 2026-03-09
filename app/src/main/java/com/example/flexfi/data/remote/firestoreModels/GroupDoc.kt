package com.example.flexfi.data.remote.firestoreModels

data class GroupDoc(
    val id: String = "",
    val name: String = "",
    val createdByPhone: String = "",
    val adminPhone: String = "",
    val createdAt: Long = 0,
    val memberPhones: List<String> = emptyList()
)
