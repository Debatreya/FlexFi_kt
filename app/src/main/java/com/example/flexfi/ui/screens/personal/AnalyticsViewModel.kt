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
import java.util.Calendar

data class CategorySum(
    val category: String,
    val totalAmount: Double,
    val percentage: Float
)

data class DailySum(
    val dayOfMonth: Int,
    val totalAmount: Double
)

class AnalyticsViewModel(
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val authService: FirebaseAuthService
) : ViewModel() {

    private val currentUserPhone = authService.getCurrentUser()?.phoneNumber ?: ""

    private val _expenses = MutableStateFlow<List<PersonalExpenseEntity>>(emptyList())
    
    // Derived state for the current month
    private val _monthlyTotal = MutableStateFlow(0.0)
    val monthlyTotal: StateFlow<Double> = _monthlyTotal.asStateFlow()

    private val _categoryBreakdown = MutableStateFlow<List<CategorySum>>(emptyList())
    val categoryBreakdown: StateFlow<List<CategorySum>> = _categoryBreakdown.asStateFlow()

    private val _dailyTrend = MutableStateFlow<List<DailySum>>(emptyList())
    val dailyTrend: StateFlow<List<DailySum>> = _dailyTrend.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        if (currentUserPhone.isBlank()) return

        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)

            personalExpenseRepository.getExpenses(currentUserPhone).collect { list ->
                // Filter to only expenses for the current month
                val monthExpenses = list.filter { expense ->
                    val expenseCal = Calendar.getInstance().apply { timeInMillis = expense.createdAt }
                    expenseCal.get(Calendar.MONTH) == currentMonth && 
                    expenseCal.get(Calendar.YEAR) == currentYear &&
                    expense.type == "EXPENSE"
                }

                val total = monthExpenses.sumOf { it.baseAmount }
                _monthlyTotal.value = total

                // Calculate Category Breakdown
                val categoryMap = monthExpenses.groupBy { it.category }
                val breakdown = categoryMap.map { (category, expensesList) ->
                    val sum = expensesList.sumOf { it.baseAmount }
                    val percentage = if (total > 0) (sum / total).toFloat() else 0f
                    CategorySum(category, sum, percentage)
                }.sortedByDescending { it.totalAmount }
                _categoryBreakdown.value = breakdown

                // Calculate Daily Trend
                val dailyMap = monthExpenses.groupBy { expense ->
                    val expenseCal = Calendar.getInstance().apply { timeInMillis = expense.createdAt }
                    expenseCal.get(Calendar.DAY_OF_MONTH)
                }
                
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val trend = (1..daysInMonth).map { day ->
                    val sum = dailyMap[day]?.sumOf { it.baseAmount } ?: 0.0
                    DailySum(day, sum)
                }
                _dailyTrend.value = trend
            }
        }
    }
}
