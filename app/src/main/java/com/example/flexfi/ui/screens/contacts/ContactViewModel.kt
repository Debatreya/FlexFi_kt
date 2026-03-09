package com.example.flexfi.ui.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.data.local.entities.ContactEntity
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.repository.ContactRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactViewModel(
    private val contactRepository: ContactRepository,
    private val authService: FirebaseAuthService
) : ViewModel() {

    init {
        refreshContacts()
    }

    val contacts: StateFlow<List<ContactEntity>> = contactRepository.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshContacts() {
        viewModelScope.launch {
            contactRepository.syncAllContacts()
        }
    }

    fun addContact(name: String, phone: String) {
        val userPhone = authService.getCurrentUser()?.phoneNumber ?: return
        viewModelScope.launch {
            contactRepository.addContact(name, phone, userPhone)
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            contactRepository.deleteContact(contactId)
        }
    }
}
