package com.example.flexfi.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.data.local.entities.BudgetEntity
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.repository.BudgetRepository
import com.example.flexfi.data.repository.PersonalExpenseRepository
import com.example.flexfi.utils.CurrencyProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class BudgetWithSpent(
    val budget: BudgetEntity,
    val spentBase: Double,
    val effectiveLimit: Double // limitAmount + rolledOverAmount
) {
    val percentage: Float get() = if (effectiveLimit > 0) (spentBase / effectiveLimit).toFloat().coerceIn(0f, 1.5f) else 0f
    val isOverBudget: Boolean get() = spentBase > effectiveLimit
    val isWarning: Boolean get() = percentage >= 0.8f && !isOverBudget
}

data class BudgetScreenState(
    val budgets: List<BudgetWithSpent> = emptyList(),
    val overallBudget: BudgetWithSpent? = null,
    val totalSpent: Double = 0.0,
    val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val isLoading: Boolean = true
)

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val authService: FirebaseAuthService
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetScreenState())
    val state: StateFlow<BudgetScreenState> = _state.asStateFlow()

    private val phone: String
        get() = authService.getCurrentUser()?.phoneNumber ?: ""

    init {
        loadBudgets()
    }

    private fun loadBudgets() {
        val p = phone
        if (p.isBlank()) return
        val month = _state.value.month
        val year = _state.value.year

        // Get start and end millis of the month
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val endMillis = cal.timeInMillis - 1

        viewModelScope.launch {
            budgetRepository.getBudgets(p, month, year).combine(
                personalExpenseRepository.getExpensesInRange(p, startMillis, endMillis)
            ) { budgets, expenses ->
                // Calculate spent per category
                val spentByCategory = expenses
                    .filter { it.type == "EXPENSE" }
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.baseAmount } }
                val totalSpent = spentByCategory.values.sum()

                val budgetItems = budgets
                    .filter { it.category != "OVERALL" }
                    .map { budget ->
                        val spent = spentByCategory[budget.category] ?: 0.0
                        BudgetWithSpent(
                            budget = budget,
                            spentBase = spent,
                            effectiveLimit = budget.limitAmount + budget.rolledOverAmount
                        )
                    }

                val overall = budgets.find { it.category == "OVERALL" }?.let { budget ->
                    BudgetWithSpent(
                        budget = budget,
                        spentBase = totalSpent,
                        effectiveLimit = budget.limitAmount + budget.rolledOverAmount
                    )
                }

                _state.value = _state.value.copy(
                    budgets = budgetItems,
                    overallBudget = overall,
                    totalSpent = totalSpent,
                    isLoading = false
                )
            }.collect()
        }
    }

    fun addOrUpdateBudget(category: String, limitInDisplay: Double, rollover: Boolean) {
        val p = phone
        if (p.isBlank()) return
        val baseLimit = CurrencyProvider.convertToBase(limitInDisplay)
        viewModelScope.launch {
            budgetRepository.upsertBudget(
                userPhone = p,
                category = category,
                limitAmount = baseLimit,
                month = _state.value.month,
                year = _state.value.year,
                rollover = rollover
            )
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(id)
        }
    }

    fun navigateMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _state.value.year)
            set(Calendar.MONTH, _state.value.month - 1)
            add(Calendar.MONTH, delta)
        }
        _state.value = _state.value.copy(
            month = cal.get(Calendar.MONTH) + 1,
            year = cal.get(Calendar.YEAR),
            isLoading = true
        )
        loadBudgets()
    }
}
