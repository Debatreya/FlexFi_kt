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
import com.example.flexfi.data.remote.ExchangeRateApi
import com.example.flexfi.data.repository.ExpenseRepository

class ContactViewModel(
    private val contactRepository: ContactRepository,
    private val expenseRepository: ExpenseRepository,
    private val exchangeRateApi: ExchangeRateApi,
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

    fun addContact(name: String, phone: String, onComplete: () -> Unit = {}) {
        val userPhone = authService.getCurrentUser()?.phoneNumber ?: return
        viewModelScope.launch {
            contactRepository.addContact(name, phone, userPhone)
            onComplete()
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            contactRepository.deleteContact(contactId)
        }
    }

    fun recordDirectPayment(
        toPhone: String,
        amount: Double,
        currency: String,
        note: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserPhone = authService.getCurrentUser()?.phoneNumber ?: return
        viewModelScope.launch {
            try {
                val rateToBase = exchangeRateApi.getRate(currency, "USD", System.currentTimeMillis())
                val baseAmount = amount * rateToBase
                expenseRepository.recordSettlement(
                    groupId = null,
                    fromPhone = currentUserPhone,
                    toPhone = toPhone,
                    amount = baseAmount,
                    note = note
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to record payment")
            }
        }
    }
}
