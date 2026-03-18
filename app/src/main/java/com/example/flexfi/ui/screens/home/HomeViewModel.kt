package com.example.flexfi.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.ai.AIManager
import com.example.flexfi.ai.ModelDownloadState
import com.example.flexfi.ai.ModelDownloadStatus
import com.example.flexfi.data.local.entities.ExpenseEntity
import com.example.flexfi.data.local.entities.GroupEntity
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import com.example.flexfi.data.local.entities.SettlementRecordEntity
import com.example.flexfi.data.local.entities.StreakEntity
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.repository.AppSettingsRepository
import com.example.flexfi.data.repository.ExpenseRepository
import com.example.flexfi.data.repository.GroupRepository
import com.example.flexfi.data.repository.PersonalExpenseRepository
import com.example.flexfi.data.repository.StreakRepository
import com.example.flexfi.data.repository.UserRepository
import com.example.flexfi.data.repository.ContactRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class DashboardBalances(
    val totalBalance: Double = 0.0,
    val youOwe: Double = 0.0,
    val owedToYou: Double = 0.0,
    val net: Double = 0.0
)

data class ActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val baseAmount: Double,
    val currency: String,
    val category: String,
    val createdAt: Long
)

class HomeViewModel(
    private val authService: FirebaseAuthService,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val streakRepository: StreakRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val contactRepository: ContactRepository,
    private val aiManager: AIManager
) : ViewModel() {

    private val currentUserPhone = authService.getCurrentUser()?.phoneNumber ?: ""

    // ─────── EXISTING STATE ───────
    private val _streak = MutableStateFlow<StreakEntity?>(null)
    val streak: StateFlow<StreakEntity?> = _streak.asStateFlow()

    private val _recentActivity = MutableStateFlow<List<ActivityItem>>(emptyList())
    val recentActivity: StateFlow<List<ActivityItem>> = _recentActivity.asStateFlow()

    private val _activeGroups = MutableStateFlow<List<GroupEntity>>(emptyList())
    val activeGroups: StateFlow<List<GroupEntity>> = _activeGroups.asStateFlow()

    private val _balances = MutableStateFlow(DashboardBalances())
    val balances: StateFlow<DashboardBalances> = _balances.asStateFlow()

    // ─────── NEW: AI INSIGHTS STATE ───────
    private val _aiInsights = MutableStateFlow<List<String>>(emptyList())
    val aiInsights: StateFlow<List<String>> = _aiInsights.asStateFlow()

    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation: StateFlow<String?> = _aiExplanation.asStateFlow()

    private val _aiLoading = MutableStateFlow(true)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiLoadingLabel = MutableStateFlow("Analyzing...")
    val aiLoadingLabel: StateFlow<String> = _aiLoadingLabel.asStateFlow()

    private val _modelDownloadState = MutableStateFlow(ModelDownloadState())
    val modelDownloadState: StateFlow<ModelDownloadState> = _modelDownloadState.asStateFlow()

    private val _aiExplainLoading = MutableStateFlow(false)
    val aiExplainLoading: StateFlow<Boolean> = _aiExplainLoading.asStateFlow()

    private val _aiExplainError = MutableStateFlow<String?>(null)
    val aiExplainError: StateFlow<String?> = _aiExplainError.asStateFlow()

    private val _assistantQuestion = MutableStateFlow("")
    val assistantQuestion: StateFlow<String> = _assistantQuestion.asStateFlow()

    private val _assistantResponse = MutableStateFlow<String?>(null)
    val assistantResponse: StateFlow<String?> = _assistantResponse.asStateFlow()

    private val _assistantLoading = MutableStateFlow(false)
    val assistantLoading: StateFlow<Boolean> = _assistantLoading.asStateFlow()

    private val _assistantError = MutableStateFlow<String?>(null)
    val assistantError: StateFlow<String?> = _assistantError.asStateFlow()

    private var settingsBankBalanceBase: Double = 0.0
    private var openingBankBalanceBase: Double = 0.0
    private var balanceAnchorMillis: Long = 0L
    private var cachedPersonalExpenses: List<PersonalExpenseEntity> = emptyList()
    private var cachedGroupExpenses: List<ExpenseEntity> = emptyList()
    private var cachedSettlements: List<SettlementRecordEntity> = emptyList()
    private var cachedGroups: List<GroupEntity> = emptyList()
    private var cachedStreakDays: Int = 0

    private var aiDataSignature: Long? = null
    private var aiFirstLoadTriggered = false
    private var aiInsightsJob: Job? = null
    private var aiDebounceJob: Job? = null
    private var aiExplainJob: Job? = null
    private var assistantJob: Job? = null

    val currentUserFlow = userRepository.getCurrentUserFlow()

    init {
        loadDashboardData()
        syncAllData()
        observeModelState()
        triggerInsightsIfNeeded(force = true)
    }

    private fun observeModelState() {
        viewModelScope.launch {
            aiManager.modelDownloadState.collectLatest { state ->
                _modelDownloadState.value = state
                if (_aiLoading.value && !aiFirstLoadTriggered && state.status == ModelDownloadStatus.READY) {
                    triggerInsightsIfNeeded(force = true)
                }
            }
        }
    }

    private fun syncAllData() {
        if (currentUserPhone.isBlank()) return
        viewModelScope.launch {
            try {
                userRepository.syncUser(currentUserPhone)
                contactRepository.syncAllContacts()
                groupRepository.syncGroupsForUser(currentUserPhone)
                expenseRepository.syncSettlementsForUser(currentUserPhone)
                // After syncing groups, get the latest to sync expenses
                val groups = groupRepository.getGroupsForUserOnce(currentUserPhone)
                for (group in groups) {
                    expenseRepository.syncExpensesForGroup(group.id)
                }
            } catch (e: Exception) {
                // Ignore sync errors silently for now
            }
        }
    }

    private fun loadDashboardData() {
        if (currentUserPhone.isBlank()) return

        // Load Streak
        viewModelScope.launch {
            streakRepository.getStreak(currentUserPhone).collect { streakEntity ->
                _streak.value = streakEntity
                cachedStreakDays = streakEntity?.currentStreak ?: 0
                triggerInsightsIfNeeded()
            }
        }

        // Load Recent Activity (personal & group combined via the mirror)
        viewModelScope.launch {
            personalExpenseRepository.getExpenses(currentUserPhone).collect { expenses ->
                cachedPersonalExpenses = expenses
                recalculateAndPersistTotalBalance()
                triggerInsightsIfNeeded()
            }
        }

        // Load Active Groups and Calculate Global Balances
        viewModelScope.launch {
            groupRepository.getGroupsForUser(currentUserPhone).collect { groups ->
                cachedGroups = groups
                _activeGroups.value = groups
                calculateGlobalBalances(groups)
                recalculateAndPersistTotalBalance()
            }
        }

        viewModelScope.launch {
            expenseRepository.getAllExpenses().collect { expenses ->
                cachedGroupExpenses = expenses
                recalculateAndPersistTotalBalance()
            }
        }

        viewModelScope.launch {
            expenseRepository.getSettlementsForUser(currentUserPhone)?.collect { records ->
                cachedSettlements = records
                recalculateAndPersistTotalBalance()
                triggerInsightsIfNeeded()
            }
        }

        // Keep dashboard total in sync with locally stored bank value.
        viewModelScope.launch {
            appSettingsRepository.getSettings().collect { settings ->
                settingsBankBalanceBase = settings.currentBankBalance
                appSettingsRepository.ensureBalanceAnchor(settings.currentBankBalance)
                openingBankBalanceBase = appSettingsRepository.getOpeningBalanceBase(settings.currentBankBalance)
                balanceAnchorMillis = appSettingsRepository.getBalanceAnchorMillis()
                recalculateAndPersistTotalBalance()
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
            totalBalance = _balances.value.totalBalance,
            youOwe = totalYouOwe,
            owedToYou = totalOwedToYou,
            net = net
        )
    }

    private fun updateRecentActivity() {
        val items = mutableListOf<ActivityItem>()
        cachedPersonalExpenses.forEach { p ->
            items.add(ActivityItem(
                id = p.id,
                title = p.description ?: "Expense",
                subtitle = "${p.category} • ${if (p.source == "GROUP") "Group" else "Personal"}",
                amount = p.amount,
                baseAmount = p.baseAmount,
                currency = p.currency,
                category = p.category,
                createdAt = p.createdAt
            ))
        }
        cachedSettlements.forEach { s ->
            val isIncoming = s.toPhone == currentUserPhone
            val title = if (isIncoming) "Money Received" else "Money Sent"
            val typeStr = if (isIncoming) "Incoming" else "Outgoing"
            items.add(ActivityItem(
                id = s.id,
                title = title,
                subtitle = "Transfer • $typeStr",
                amount = s.amount,
                baseAmount = s.amount,
                currency = "USD",
                category = "Transfer",
                createdAt = s.createdAt
            ))
        }
        _recentActivity.value = items.sortedByDescending { it.createdAt }.take(10)
    }

    private fun recalculateAndPersistTotalBalance() {
        if (currentUserPhone.isBlank()) return
        
        updateRecentActivity()

        val activeGroupIds = cachedGroups.map { it.id }.toHashSet()
        val anchor = balanceAnchorMillis

        val personalCashDelta = cachedPersonalExpenses
            .asSequence()
            .filter { it.source == "PERSONAL" }
            .filter { it.createdAt >= anchor }
            .sumOf { personal ->
                when (personal.type.uppercase()) {
                    "INCOME" -> personal.baseAmount
                    "EXPENSE" -> -personal.baseAmount
                    else -> 0.0
                }
            }

        val groupCashOut = cachedGroupExpenses
            .asSequence()
            .filter { it.groupId in activeGroupIds }
            .filter { it.paidByPhone == currentUserPhone }
            .filter { it.createdAt >= anchor }
            .sumOf { it.baseAmount }

        val settlementNet = cachedSettlements
            .asSequence()
            .filter { it.createdAt >= anchor }
            .sumOf { record ->
                when {
                    record.toPhone == currentUserPhone -> record.amount
                    record.fromPhone == currentUserPhone -> -record.amount
                    else -> 0.0
                }
            }

        val derivedBalance = openingBankBalanceBase + personalCashDelta - groupCashOut + settlementNet
        _balances.value = _balances.value.copy(totalBalance = derivedBalance)

        if (kotlin.math.abs(derivedBalance - settingsBankBalanceBase) > 0.01) {
            viewModelScope.launch {
                val latest = appSettingsRepository.getSettingsOnce()
                appSettingsRepository.saveSettings(latest.copy(currentBankBalance = derivedBalance))
            }
        }
    }

    // ─────── AI INSIGHTS METHODS ───────

    /**
     * Generates AI insights for the current month.
     * Runs asynchronously; results emitted to aiInsights StateFlow.
     */
    private fun generateAIInsights() {
        if (currentUserPhone.isBlank()) return

        aiInsightsJob?.cancel()
        _aiLoading.value = true
        _aiLoadingLabel.value = "Analyzing..."
        aiInsightsJob = viewModelScope.launch {
            try {
                aiManager.generateInsights(currentUserPhone).collect { insights ->
                    _aiInsights.value = insights
                    _aiLoading.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "AI insights error", e)
                _aiLoading.value = false
                _aiInsights.value = emptyList()
            }
        }
    }

    /**
     * Generates an explanation of the user's spending.
     * Call this when user taps "Explain My Spending" button.
     */
    fun explainSpending() {
        if (currentUserPhone.isBlank()) return

        if (!aiManager.isReady()) {
            _aiExplainError.value = "Model is not ready yet. Please retry in a moment."
            return
        }

        _aiExplainLoading.value = true
        _aiLoadingLabel.value = "Analyzing..."
        _aiExplainError.value = null
        _aiExplanation.value = null
        aiExplainJob?.cancel()
        aiExplainJob = viewModelScope.launch {
            try {
                aiManager.explainSpending(currentUserPhone).collect { explanation ->
                    _aiExplanation.value = explanation
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "AI explanation error", e)
                _aiExplanation.value = null
                _aiExplainError.value = "Inference failed. Please retry."
            } finally {
                _aiExplainLoading.value = false
            }
        }
    }

    fun retryExplain() {
        explainSpending()
    }

    fun submitAssistantQuestion(question: String) {
        val trimmed = question.trim()
        _assistantQuestion.value = trimmed

        if (trimmed.isBlank()) {
            _assistantError.value = "Enter a question to continue."
            return
        }

        if (!aiManager.isReady()) {
            _assistantError.value = "Model is not ready yet. Please retry in a moment."
            return
        }

        _assistantLoading.value = true
        _aiLoadingLabel.value = "Thinking..."
        _assistantError.value = null
        _assistantResponse.value = null

        assistantJob?.cancel()
        assistantJob = viewModelScope.launch {
            try {
                val response = aiManager.askAssistant(trimmed, currentUserPhone)
                _assistantResponse.value = response
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Assistant generation error", e)
                _assistantError.value = "Inference failed. Please retry."
            } finally {
                _assistantLoading.value = false
            }
        }
    }

    fun retryAssistant() {
        submitAssistantQuestion(_assistantQuestion.value)
    }

    fun clearAssistantState() {
        _assistantQuestion.value = ""
        _assistantResponse.value = null
        _assistantError.value = null
        _assistantLoading.value = false
    }

    private fun triggerInsightsIfNeeded(force: Boolean = false) {
        if (currentUserPhone.isBlank()) return

        val newSignature = calculateAiDataSignature()
        val shouldGenerate = force || !aiFirstLoadTriggered || aiDataSignature != newSignature
        if (!shouldGenerate) return

        aiDebounceJob?.cancel()
        aiDebounceJob = viewModelScope.launch {
            delay(400)
            aiDataSignature = newSignature
            aiFirstLoadTriggered = true
            generateAIInsights()
        }
    }

    private fun calculateAiDataSignature(): Long {
        val personalSum = cachedPersonalExpenses.sumOf { it.baseAmount }
        val settlementSum = cachedSettlements.sumOf { it.amount }
        val latestPersonal = cachedPersonalExpenses.maxOfOrNull { it.createdAt } ?: 0L
        val latestSettlement = cachedSettlements.maxOfOrNull { it.createdAt } ?: 0L

        return listOf(
            cachedPersonalExpenses.size,
            cachedSettlements.size,
            cachedStreakDays,
            personalSum.toInt(),
            settlementSum.toInt(),
            latestPersonal,
            latestSettlement
        ).joinToString("|").hashCode().toLong()
    }
}
