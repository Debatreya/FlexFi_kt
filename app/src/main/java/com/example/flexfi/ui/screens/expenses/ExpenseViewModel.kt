package com.example.flexfi.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.data.local.entities.ExpenseEntity
import com.example.flexfi.data.repository.ExpenseRepository
import com.example.flexfi.data.repository.GroupRepository
import com.example.flexfi.data.repository.Settlement
import com.example.flexfi.data.repository.StreakRepository
import com.example.flexfi.data.remote.FirebaseAuthService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExpenseViewModel(
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val streakRepository: StreakRepository,
    private val authService: FirebaseAuthService
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    val expenses: StateFlow<List<ExpenseEntity>> = _expenses.asStateFlow()

    private val _balances = MutableStateFlow<Map<String, Double>>(emptyMap())
    val balances: StateFlow<Map<String, Double>> = _balances.asStateFlow()

    private val _settlements = MutableStateFlow<List<Settlement>>(emptyList())
    val settlements: StateFlow<List<Settlement>> = _settlements.asStateFlow()

    private var expenseCollectionJob: Job? = null

    fun loadExpensesForGroup(groupId: String) {
        expenseCollectionJob?.cancel()
        expenseCollectionJob = viewModelScope.launch {
            // Sync from remote first
            expenseRepository.syncExpensesForGroup(groupId)
            
            // Then listen to local DB
            expenseRepository.getExpensesForGroup(groupId).collect { list ->
                _expenses.value = list
                // Recalculate balances whenever expenses change
                loadBalances(groupId)
            }
        }
    }

    private fun loadBalances(groupId: String) {
        viewModelScope.launch {
            val groupBalances = expenseRepository.calculateGroupBalances(groupId)
            _balances.value = groupBalances
            _settlements.value = expenseRepository.calculateSettlements(groupBalances)
        }
    }

    fun addExpense(
        title: String,
        amount: Double,
        groupId: String,
        paidByPhone: String,
        category: String,
        splitType: String,
        selectedMemberPhones: List<String>,
        exactAmounts: Map<String, Double>? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                expenseRepository.addExpense(
                    title = title,
                    amount = amount,
                    groupId = groupId,
                    paidByPhone = paidByPhone,
                    category = category,
                    splitType = splitType,
                    selectedMemberPhones = selectedMemberPhones,
                    exactAmounts = exactAmounts
                )
                val userPhone = authService.getCurrentUser()?.phoneNumber
                if (!userPhone.isNullOrBlank()) {
                    streakRepository.updateStreak(userPhone)
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "An error occurred")
            }
        }
    }
}
