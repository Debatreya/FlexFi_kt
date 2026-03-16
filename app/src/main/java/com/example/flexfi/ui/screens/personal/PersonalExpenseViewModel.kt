package com.example.flexfi.ui.screens.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import com.example.flexfi.data.remote.ExchangeRateApi
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.repository.BudgetRepository
import com.example.flexfi.data.repository.PersonalExpenseRepository
import com.example.flexfi.data.repository.StreakRepository
import com.example.flexfi.data.local.entities.BudgetEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategoryBudgetProgress(
    val category: String,
    val spentBase: Double,
    val limitBase: Double,
    val isRollover: Boolean
) {
    val percentage: Float get() = if (limitBase > 0) (spentBase / limitBase).toFloat().coerceIn(0f, 1f) else 0f
    val isOverBudget: Boolean get() = spentBase > limitBase
    val isWarning: Boolean get() = percentage >= 0.8f && !isOverBudget
}

class PersonalExpenseViewModel(
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val streakRepository: StreakRepository,
    private val authService: FirebaseAuthService,
    private val exchangeRateApi: ExchangeRateApi
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<PersonalExpenseEntity>>(emptyList())
    val expenses: StateFlow<List<PersonalExpenseEntity>> = _expenses.asStateFlow()

    private val _totalSpent = MutableStateFlow(0.0)
    val totalSpent: StateFlow<Double> = _totalSpent.asStateFlow()

    private val _budgetProgress = MutableStateFlow<List<CategoryBudgetProgress>>(emptyList())
    val budgetProgress: StateFlow<List<CategoryBudgetProgress>> = _budgetProgress.asStateFlow()

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

                val cal = Calendar.getInstance()
                val currentMonth = cal.get(Calendar.MONTH)
                val currentYear = cal.get(Calendar.YEAR)
                val monthExpenseTotal = list
                    .asSequence()
                    .filter { it.type == "EXPENSE" }
                    .filter {
                        val expenseCal = Calendar.getInstance().apply { timeInMillis = it.createdAt }
                        expenseCal.get(Calendar.MONTH) == currentMonth &&
                            expenseCal.get(Calendar.YEAR) == currentYear
                    }
                    .sumOf { it.baseAmount }
                _totalSpent.value = monthExpenseTotal
            }
        }

        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val month = cal.get(Calendar.MONTH) + 1
            val year = cal.get(Calendar.YEAR)

            budgetRepository.getBudgets(phone, month, year).combine(
                personalExpenseRepository.getExpenses(phone)
            ) { budgets, allExpenses ->
                // Filter expenses to current month for budget calculation
                val currentMonthExpenses = allExpenses.filter { expense ->
                    val expenseCal = Calendar.getInstance().apply { timeInMillis = expense.createdAt }
                    expenseCal.get(Calendar.MONTH) + 1 == month && expenseCal.get(Calendar.YEAR) == year && expense.type == "EXPENSE"
                }

                val spentByCategory = currentMonthExpenses
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.baseAmount } }

                budgets.filter { it.category != "OVERALL" }.map { budget ->
                    val spent = spentByCategory[budget.category] ?: 0.0
                    CategoryBudgetProgress(
                        category = budget.category,
                        spentBase = spent,
                        limitBase = budget.limitAmount + budget.rolledOverAmount,
                        isRollover = budget.rolledOverAmount > 0
                    )
                }.sortedByDescending { it.percentage }
            }.collect { progressList ->
                _budgetProgress.value = progressList
            }
        }
    }

    // ──────────────────────────────────────────────
    //  MANUAL EXPENSE CRUD
    // ──────────────────────────────────────────────

    fun addManualExpense(
        title: String,
        amount: Double,
        currency: String,
        category: String,
        dateMillis: Long,
        subCategory: String? = null,
        type: String = "EXPENSE",
        paymentMode: String = "Cash",
        merchant: String? = null,
        tags: String? = null,
        attachmentUri: String? = null,
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
                    currency = currency,
                    category = category,
                    dateMillis = dateMillis,
                    subCategory = subCategory,
                    type = type,
                    paymentMode = paymentMode,
                    merchant = merchant,
                    tags = tags,
                    attachmentUri = attachmentUri
                )
                streakRepository.updateStreak(phone)
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
        currency: String,
        category: String,
        dateMillis: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val rateToBase = exchangeRateApi.getRate(
                    fromCurrency = currency,
                    toCurrency = "USD",
                    dateMillis = dateMillis
                )
                personalExpenseRepository.update(
                    expense.copy(
                        description = title,
                        amount = amount,
                        currency = currency,
                        exchangeRateToBase = rateToBase,
                        baseAmount = amount * rateToBase,
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
