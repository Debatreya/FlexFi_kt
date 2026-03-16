package com.example.flexfi.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.data.local.entities.BudgetGoalEntity
import com.example.flexfi.data.local.entities.GoalContributionEntity
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.repository.BudgetGoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Achievement(
    val title: String,
    val icon: String,
    val earned: Boolean
)

class BudgetGoalViewModel(
    private val budgetGoalRepository: BudgetGoalRepository,
    private val authService: FirebaseAuthService
) : ViewModel() {

    private val currentUserPhone = authService.getCurrentUser()?.phoneNumber ?: ""

    private val _goals = MutableStateFlow<List<BudgetGoalEntity>>(emptyList())
    val goals: StateFlow<List<BudgetGoalEntity>> = _goals.asStateFlow()

    private val _totalSaved = MutableStateFlow(0.0)
    val totalSaved: StateFlow<Double> = _totalSaved.asStateFlow()

    private val _totalTarget = MutableStateFlow(0.0)
    val totalTarget: StateFlow<Double> = _totalTarget.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _selectedGoalContributions = MutableStateFlow<List<GoalContributionEntity>>(emptyList())
    val selectedGoalContributions: StateFlow<List<GoalContributionEntity>> = _selectedGoalContributions.asStateFlow()

    init {
        loadGoals()
    }

    private fun loadGoals() {
        if (currentUserPhone.isBlank()) return

        viewModelScope.launch {
            budgetGoalRepository.getGoalsForUser(currentUserPhone).collect { goalList ->
                _goals.value = goalList
                computeAchievements(goalList)
            }
        }
        viewModelScope.launch {
            budgetGoalRepository.getTotalSaved(currentUserPhone).collect { _totalSaved.value = it }
        }
        viewModelScope.launch {
            budgetGoalRepository.getTotalTarget(currentUserPhone).collect { _totalTarget.value = it }
        }
    }

    private fun computeAchievements(goals: List<BudgetGoalEntity>) {
        val list = mutableListOf<Achievement>()
        val hasGoals = goals.isNotEmpty()
        val anyHalfway = goals.any { it.targetAmount > 0 && it.savedAmount / it.targetAmount >= 0.5 }
        val anyComplete = goals.any { it.targetAmount > 0 && it.savedAmount >= it.targetAmount }

        list.add(Achievement("First Goal", "🎯", hasGoals))
        list.add(Achievement("50% Halfway", "🏆", anyHalfway))
        list.add(Achievement("Goal Complete", "⚡", anyComplete))
        list.add(Achievement("3 Goals", "🥇", goals.size >= 3))

        _achievements.value = list
    }

    fun createGoal(
        title: String,
        description: String,
        targetAmount: Double,
        targetDate: Long?,
        autoSaveAmount: Double,
        autoSaveFrequency: String,
        isSinkingFund: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                budgetGoalRepository.createGoal(
                    userPhone = currentUserPhone,
                    title = title,
                    description = description,
                    targetAmount = targetAmount,
                    targetDate = targetDate,
                    autoSaveAmount = autoSaveAmount,
                    autoSaveFrequency = autoSaveFrequency,
                    isSinkingFund = isSinkingFund
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to create goal")
            }
        }
    }

    fun addSavings(goalId: String, amount: Double, note: String = "", onMilestoneReached: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val goal = budgetGoalRepository.getGoalById(goalId) ?: return@launch
            val newSaved = goal.savedAmount + amount
            val percentage = if (goal.targetAmount > 0) (newSaved / goal.targetAmount) * 100 else 0.0

            val currentMilestone = when {
                percentage >= 100 -> 100
                percentage >= 75 -> 75
                percentage >= 50 -> 50
                percentage >= 25 -> 25
                else -> 0
            }

            if (currentMilestone > goal.lastNotifiedMilestone) {
                onMilestoneReached?.invoke("🎉 Congratulations! You have reached $currentMilestone% of your goal: ${goal.title}")
                budgetGoalRepository.addSavings(goalId, amount, note)
                budgetGoalRepository.updateGoal(
                    // Fetch directly from DB after addSavings since addSavings saves the history
                    budgetGoalRepository.getGoalById(goalId)?.copy(lastNotifiedMilestone = currentMilestone) ?: return@launch
                )
            } else {
                budgetGoalRepository.addSavings(goalId, amount, note)
            }
        }
    }

    fun loadContributionsForGoal(goalId: String) {
        viewModelScope.launch {
            budgetGoalRepository.getContributionsForGoal(goalId).collect {
                _selectedGoalContributions.value = it
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            budgetGoalRepository.deleteGoal(goalId)
        }
    }

    suspend fun getGoalById(goalId: String): BudgetGoalEntity? =
        budgetGoalRepository.getGoalById(goalId)

    fun updateGoal(goal: BudgetGoalEntity) {
        viewModelScope.launch {
            budgetGoalRepository.updateGoal(goal)
        }
    }
}
