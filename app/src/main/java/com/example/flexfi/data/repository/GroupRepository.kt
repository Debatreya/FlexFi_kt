package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.ContactDao
import com.example.flexfi.data.local.dao.GroupDao
import com.example.flexfi.data.local.dao.GroupMemberInfo
import com.example.flexfi.data.local.entities.GroupEntity
import com.example.flexfi.data.local.entities.GroupMemberEntity
import com.example.flexfi.data.remote.FirestoreGroupService
import com.example.flexfi.data.remote.firestoreModels.GroupDoc
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class GroupRepository(
    private val groupDao: GroupDao,
    private val contactDao: ContactDao,
    private val firestoreGroupService: FirestoreGroupService
) {
    suspend fun createGroup(name: String, creatorPhone: String, memberPhones: List<String>) {
        val groupId = UUID.randomUUID().toString()
        val group = GroupEntity(
            id = groupId,
            name = name,
            createdByPhone = creatorPhone,
            adminPhone = creatorPhone,
            createdAt = System.currentTimeMillis(),
            totalExpense = 0.0
        )
        groupDao.insertGroup(group)

        val allPhones = (memberPhones + creatorPhone).distinct()
        
        allPhones.forEach { phone ->
            groupDao.insertMember(
                GroupMemberEntity(
                    groupId = groupId,
                    phone = phone,
                    joinedAt = System.currentTimeMillis()
                )
            )
        }

        // Push to Firestore
        val groupDoc = GroupDoc(
            id = groupId,
            name = name,
            createdByPhone = creatorPhone,
            adminPhone = creatorPhone,
            createdAt = group.createdAt,
            memberPhones = allPhones
        )
        firestoreGroupService.createGroup(groupDoc)
    }

    suspend fun syncGroupsForUser(userPhone: String) {
        val remoteGroups = firestoreGroupService.getGroupsForPhone(userPhone)

        remoteGroups.forEach { doc ->
            val groupEntity = GroupEntity(
                id = doc.id,
                name = doc.name,
                createdByPhone = doc.createdByPhone,
                adminPhone = doc.adminPhone,
                createdAt = doc.createdAt,
                totalExpense = 0.0
            )
            groupDao.insertGroup(groupEntity)

            // Refresh membership for this specific group only.
            groupDao.removeAllMembers(doc.id)

            for (phone in doc.memberPhones) {
                groupDao.insertMember(
                    GroupMemberEntity(
                        groupId = doc.id,
                        phone = phone,
                        joinedAt = doc.createdAt
                    )
                )
            }
        }
    }

    fun getGroupsForUser(userPhone: String): Flow<List<GroupEntity>> =
        groupDao.getGroupsForUserPhone(userPhone)

    suspend fun getGroupsForUserOnce(userPhone: String): List<GroupEntity> =
        groupDao.getGroupsForUserPhoneOnce(userPhone)

    fun getGroupMembers(groupId: String): Flow<List<GroupMemberInfo>> =
        groupDao.getGroupMembersInfo(groupId)

    suspend fun getMemberCount(groupId: String): Int =
        groupDao.getMemberCount(groupId)

    suspend fun getGroupMembersOnce(groupId: String): List<GroupMemberInfo> =
        groupDao.getGroupMembersOnce(groupId)

    suspend fun getGroupById(groupId: String): GroupEntity? =
        groupDao.getGroupById(groupId)

    suspend fun updateGroup(groupId: String, newName: String, adminPhone: String, memberPhones: List<String>) {
        val existingGroup = groupDao.getGroupById(groupId) ?: return
        val updatedGroup = existingGroup.copy(name = newName)
        groupDao.updateGroup(updatedGroup)

        groupDao.removeAllMembers(groupId)

        val allPhones = (memberPhones + adminPhone).distinct()
        allPhones.forEach { phone ->
            groupDao.insertMember(
                GroupMemberEntity(
                    groupId = groupId,
                    phone = phone,
                    joinedAt = System.currentTimeMillis()
                )
            )
        }

        val groupDoc = GroupDoc(
            id = groupId,
            name = newName,
            createdByPhone = existingGroup.createdByPhone,
            adminPhone = adminPhone,
            createdAt = existingGroup.createdAt,
            memberPhones = allPhones
        )
        firestoreGroupService.createGroup(groupDoc)
    }

    suspend fun deleteGroup(groupId: String) {
        val group = groupDao.getGroupById(groupId) ?: return
        groupDao.deleteGroup(group)
        groupDao.removeAllMembers(groupId)
        // Also delete from Firestore (source of truth)
        firestoreGroupService.deleteGroup(groupId)
    }
}