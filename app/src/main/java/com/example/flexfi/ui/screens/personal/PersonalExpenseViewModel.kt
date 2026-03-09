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

    // ──────────────────────────────────────────────
    //  MANUAL EXPENSE CRUD
    // ──────────────────────────────────────────────

    fun addManualExpense(
        title: String,
        amount: Double,
        category: String,
        dateMillis: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val phone = currentUserPhone
        if (phone.isBlank()) { onError("User not signed in"); return }

        viewModelScope.launch {
            try {
                personalExpenseRepository.insertManual(
                    userPhone = phone,
                    title = title,
                    amount = amount,
                    category = category,
                    dateMillis = dateMillis
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to add expense")
            }
        }
    }

    fun updateExpense(
        expense: PersonalExpenseEntity,
        title: String,
        amount: Double,
        category: String,
        dateMillis: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                personalExpenseRepository.update(
                    expense.copy(
                        description = title,
                        amount = amount,
                        category = category.ifBlank { "Other" },
                        createdAt = dateMillis
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update expense")
            }
        }
    }

    fun deleteExpense(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                personalExpenseRepository.deleteById(id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete expense")
            }
        }
    }

    /** Returns the expense entity matching the given id from the current list, or null */
    fun findExpenseById(id: String): PersonalExpenseEntity? =
        _expenses.value.find { it.id == id }
}
