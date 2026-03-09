package com.example.flexfi.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "phone"]
)
data class GroupMemberEntity(
    val groupId: String,
    val phone: String, // Global Identity
    val joinedAt: Long
)
