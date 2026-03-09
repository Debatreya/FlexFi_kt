package com.example.flexfi.ui.screens.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.repository.PersonalExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PersonalExpenseViewModel(
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val authService: FirebaseAuthService
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<PersonalExpenseEntity>>(emptyList())
    val expenses: StateFlow<List<PersonalExpenseEntity>> = _expenses.asStateFlow()

    private val _totalSpent = MutableStateFlow(0.0)
    val totalSpent: StateFlow<Double> = _totalSpent.asStateFlow()

    /** Phone of the currently signed-in user; empty string if not signed in. */
    val currentUserPhone: String
        get() = authService.getCurrentUser()?.phoneNumber ?: ""

    init {
        loadPersonalData()
    }

    private fun loadPersonalData() {
        val phone = currentUserPhone
        if (phone.isBlank()) return

        viewModelScope.launch {
            personalExpenseRepository.getExpenses(phone).collect { list ->
                _expenses.value = list
            }
        }

        viewModelScope.launch {
            personalExpenseRepository.getTotalSpent(phone).collect { total ->
                _totalSpent.value = total ?: 0.0
            }
        }
    }
}
