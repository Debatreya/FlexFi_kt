package com.example.flexfi.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.data.local.entities.GroupEntity
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import com.example.flexfi.data.local.entities.StreakEntity
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.repository.AppSettingsRepository
import com.example.flexfi.data.repository.ExpenseRepository
import com.example.flexfi.data.repository.GroupRepository
import com.example.flexfi.data.repository.PersonalExpenseRepository
import com.example.flexfi.data.repository.StreakRepository
import com.example.flexfi.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardBalances(
    val totalBalance: Double = 0.0,
    val youOwe: Double = 0.0,
    val owedToYou: Double = 0.0,
    val net: Double = 0.0
)

class HomeViewModel(
    private val authService: FirebaseAuthService,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val streakRepository: StreakRepository,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val currentUserPhone = authService.getCurrentUser()?.phoneNumber ?: ""

    private val _streak = MutableStateFlow<StreakEntity?>(null)
    val streak: StateFlow<StreakEntity?> = _streak.asStateFlow()

    private val _recentActivity = MutableStateFlow<List<PersonalExpenseEntity>>(emptyList())
    val recentActivity: StateFlow<List<PersonalExpenseEntity>> = _recentActivity.asStateFlow()

    private val _activeGroups = MutableStateFlow<List<GroupEntity>>(emptyList())
    val activeGroups: StateFlow<List<GroupEntity>> = _activeGroups.asStateFlow()

    private val _balances = MutableStateFlow(DashboardBalances())
    val balances: StateFlow<DashboardBalances> = _balances.asStateFlow()

    private var settingsBankBalanceBase: Double = 0.0

    val currentUserFlow = userRepository.getCurrentUserFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        if (currentUserPhone.isBlank()) return

        // Load Streak
        viewModelScope.launch {
            streakRepository.getStreak(currentUserPhone).collect { streakEntity ->
                _streak.value = streakEntity
            }
        }

        // Load Recent Activity (personal & group combined via the mirror)
        viewModelScope.launch {
            personalExpenseRepository.getExpenses(currentUserPhone).collect { expenses ->
                _recentActivity.value = expenses.take(10) // Only top 10 for dashboard
            }
        }

        // Load Active Groups and Calculate Global Balances
        viewModelScope.launch {
            groupRepository.getGroupsForUser(currentUserPhone).collect { groups ->
                _activeGroups.value = groups
                calculateGlobalBalances(groups)
            }
        }

        // Keep dashboard total in sync with locally stored bank value.
        viewModelScope.launch {
            appSettingsRepository.getSettings().collect {
                settingsBankBalanceBase = it.currentBankBalance
                recalculateTotalBalance()
            }
        }
    }

    private suspend fun calculateGlobalBalances(groups: List<GroupEntity>) {
        var totalYouOwe = 0.0
        var totalOwedToYou = 0.0
        
        // Sum up from groups
        for (group in groups) {
            val groupBalances = expenseRepository.calculateGroupBalances(group.id)
            val myBalance = groupBalances[currentUserPhone] ?: 0.0
            
            if (myBalance < 0) {
                totalYouOwe += kotlin.math.abs(myBalance)
            } else if (myBalance > 0) {
                totalOwedToYou += myBalance
            }
        }

        val net = totalOwedToYou - totalYouOwe
        
        _balances.value = DashboardBalances(
            totalBalance = settingsBankBalanceBase,
            youOwe = totalYouOwe,
            owedToYou = totalOwedToYou,
            net = net
        )
    }

    private fun recalculateTotalBalance() {
        if (currentUserPhone.isBlank()) return
        _balances.value = _balances.value.copy(totalBalance = settingsBankBalanceBase)
    }
}
