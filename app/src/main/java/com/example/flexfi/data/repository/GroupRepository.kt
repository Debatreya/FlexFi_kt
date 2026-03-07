package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.GroupDao
import com.example.flexfi.data.local.entities.GroupEntity
import com.example.flexfi.data.local.entities.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

class GroupRepository(
    private val groupDao: GroupDao
) {
    suspend fun addGroup(group: GroupEntity) {
        groupDao.insertGroup(group)
    }

    suspend fun addMember(member: GroupMemberEntity) {
        groupDao.insertMember(member)
    }

    fun getAllGroups(): Flow<List<GroupEntity>> = groupDao.getAllGroups()
}
