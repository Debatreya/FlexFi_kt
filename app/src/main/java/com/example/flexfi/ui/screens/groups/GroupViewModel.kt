package com.example.flexfi.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.data.local.dao.GroupMemberInfo
import com.example.flexfi.data.local.entities.ContactEntity
import com.example.flexfi.data.local.entities.GroupEntity
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.repository.ContactRepository
import com.example.flexfi.data.repository.GroupRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupViewModel(
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository,
    private val authService: FirebaseAuthService
) : ViewModel() {

    private val _groups = MutableStateFlow<List<GroupEntity>>(emptyList())
    val groups: StateFlow<List<GroupEntity>> = _groups.asStateFlow()

    // Dynamic getter — always reads the CURRENT logged-in user's phone
    val currentUserPhone: String
        get() = authService.getCurrentUser()?.phoneNumber ?: ""

    // Track the collection job so we can cancel and restart it
    private var groupCollectionJob: Job? = null

    init {
        refreshGroups()
    }

    fun refreshGroups() {
        val phone = authService.getCurrentUser()?.phoneNumber
        if (phone != null) {
            // Cancel any previous collection coroutine to avoid duplicates
            groupCollectionJob?.cancel()
            groupCollectionJob = viewModelScope.launch {
                groupRepository.syncGroupsForUser(phone)
                groupRepository.getGroupsForUser(phone).collect {
                    _groups.value = it
                }
            }
        }
    }

    fun createGroup(name: String, selectedContacts: List<ContactEntity>) {
        val phone = authService.getCurrentUser()?.phoneNumber ?: return
        viewModelScope.launch {
            val memberPhones = selectedContacts.map { it.phone }
            groupRepository.createGroup(name, phone, memberPhones)
        }
    }

    fun updateGroup(groupId: String, newName: String, selectedContacts: List<ContactEntity>) {
        val phone = authService.getCurrentUser()?.phoneNumber ?: return
        viewModelScope.launch {
            val memberPhones = selectedContacts.map { it.phone }
            groupRepository.updateGroup(groupId, newName, phone, memberPhones)
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId)
        }
    }

    fun getGroupMembers(groupId: String): Flow<List<GroupMemberInfo>> = 
        groupRepository.getGroupMembers(groupId)

    suspend fun getGroupById(groupId: String): GroupEntity? = 
        groupRepository.getGroupById(groupId)
}
