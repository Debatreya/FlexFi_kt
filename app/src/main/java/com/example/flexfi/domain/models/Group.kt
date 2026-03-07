package com.example.flexfi.domain.models

data class Group(
    val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList()
)
