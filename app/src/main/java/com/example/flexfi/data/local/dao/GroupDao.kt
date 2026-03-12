package com.example.flexfi.data.local.dao

import androidx.room.*
import com.example.flexfi.data.local.entities.ContactEntity
import com.example.flexfi.data.local.entities.GroupEntity
import com.example.flexfi.data.local.entities.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

data class GroupMemberInfo(
    val phone: String,
    val joinedAt: Long,
    val contactName: String?,
    val isGhost: Boolean?
)

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMemberEntity)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun removeAllMembers(groupId: String)

    @Query("""
        SELECT groups.* FROM groups 
        INNER JOIN group_members ON groups.id = group_members.groupId 
        WHERE group_members.phone = :userPhone
    """)
    fun getGroupsForUserPhone(userPhone: String): Flow<List<GroupEntity>>

    @Query("""
        SELECT groups.* FROM groups 
        INNER JOIN group_members ON groups.id = group_members.groupId 
        WHERE group_members.phone = :userPhone
    """)
    suspend fun getGroupsForUserPhoneOnce(userPhone: String): List<GroupEntity>

    @Query("""
        SELECT 
            gm.phone as phone,
            gm.joinedAt as joinedAt,
            c.name as contactName,
            c.isGhost as isGhost
        FROM group_members gm
        LEFT JOIN contacts c ON gm.phone = c.phone
        WHERE gm.groupId = :groupId
    """)
    fun getGroupMembersInfo(groupId: String): Flow<List<GroupMemberInfo>>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): GroupEntity?
    
    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("DELETE FROM groups")
    suspend fun deleteAllGroups()

    @Query("DELETE FROM group_members")
    suspend fun deleteAllGroupMembers()
}
