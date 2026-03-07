package com.example.flexfi.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "memberId"]
)
data class GroupMemberEntity(
    val groupId: String,
    val memberId: String,
    val memberType: String, // USER or CONTACT
    val joinedAt: Long
)
