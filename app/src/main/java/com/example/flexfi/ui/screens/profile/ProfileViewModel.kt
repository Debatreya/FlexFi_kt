package com.example.flexfi.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.ai.AIManager
import com.example.flexfi.data.local.entities.AppSettingsEntity
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import com.example.flexfi.data.local.entities.RecurringTransactionEntity
import com.example.flexfi.data.local.entities.UserEntity
import com.example.flexfi.data.remote.ExchangeRateApi
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.repository.AppSettingsRepository
import com.example.flexfi.data.repository.BudgetRepository
import com.example.flexfi.data.repository.PersonalExpenseRepository
import com.example.flexfi.data.repository.RecurringTransactionRepository
import com.example.flexfi.data.repository.StreakRepository
import com.example.flexfi.data.repository.UserRepository
import com.example.flexfi.flexcard.CardTemplate
import com.example.flexfi.flexcard.FlexCardData
import com.example.flexfi.flexcard.FlexCardSummaryBuilder
import com.example.flexfi.flexcard.FlexScoreCalculator
import com.example.flexfi.flexcard.FlexScoreInput
import com.example.flexfi.utils.CurrencyProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private data class ProfileState(
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val recurring: List<RecurringTransactionEntity> = emptyList()
)

class ProfileViewModel(
    private val authService: FirebaseAuthService,
    private val settingsRepository: AppSettingsRepository,
    private val recurringRepository: RecurringTransactionRepository,
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val streakRepository: StreakRepository,
    private val aiManager: AIManager,
    private val exchangeRateApi: ExchangeRateApi,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val SMALL_TX_BASE_THRESHOLD_USD = 2.5
    }

    private val _state = MutableStateFlow(ProfileState())

    private val _settingsFlow = MutableStateFlow(AppSettingsEntity())
    val settings: StateFlow<AppSettingsEntity> = _settingsFlow.asStateFlow()

    private val _recurringFlow = MutableStateFlow<List<RecurringTransactionEntity>>(emptyList())
    val recurring: StateFlow<List<RecurringTransactionEntity>> = _recurringFlow.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _selectedTemplate = MutableStateFlow(CardTemplate.DARK)
    val selectedTemplate: StateFlow<CardTemplate> = _selectedTemplate.asStateFlow()

    private val _flexCardData = MutableStateFlow<FlexCardData?>(null)
    val flexCardData: StateFlow<FlexCardData?> = _flexCardData.asStateFlow()

    private val _flexCardLoading = MutableStateFlow(false)
    val flexCardLoading: StateFlow<Boolean> = _flexCardLoading.asStateFlow()

    private val _flexCardError = MutableStateFlow<String?>(null)
    val flexCardError: StateFlow<String?> = _flexCardError.asStateFlow()

    private var lastGeneratedMonthKey: String? = null

    init {
        viewModelScope.launch {
            settingsRepository.ensureDefaults()
            syncCurrentUserFromRemote()
            observeSettings()
            observeRecurring()
            observeCurrentUser()
            processDueRecurringTransactions()
            refreshDisplayRate()
        }
    }

    private suspend fun syncCurrentUserFromRemote() {
        val current = authService.getCurrentUser() ?: return
        runCatching {
            userRepository.syncUser(current.uid)
        }.onFailure {
            val phone = current.phoneNumber
            if (!phone.isNullOrBlank()) {
                runCatching { userRepository.syncUser(phone) }
            }
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUserFlow().collect {
                _currentUser.value = it
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect {
                _state.value = _state.value.copy(settings = it)
                _settingsFlow.value = it
                CurrencyProvider.setDisplayCurrency(it.displayCurrency)
                _selectedTemplate.value = runCatching { CardTemplate.valueOf(it.flexCardTemplate) }
                    .getOrDefault(CardTemplate.DARK)
            }
        }
    }

    fun selectTemplate(template: CardTemplate) {
        _selectedTemplate.value = template
        _flexCardData.value = _flexCardData.value?.copy(template = template)
        saveSettings(_state.value.settings.copy(flexCardTemplate = template.name))
    }

    fun clearFlexCardError() {
        _flexCardError.value = null
    }

    fun generateFlexCard(force: Boolean = false) {
        val userPhone = authService.getCurrentUser()?.phoneNumber.orEmpty()
        if (userPhone.isBlank()) {
            _flexCardError.value = "Unable to load profile session."
            return
        }

        val monthKey = currentMonthKey(Calendar.getInstance())
        if (!force && _flexCardData.value != null && lastGeneratedMonthKey == monthKey) {
            return
        }

        _flexCardLoading.value = true
        _flexCardError.value = null

        viewModelScope.launch {
            try {
                val cardData = withContext(Dispatchers.Default) {
                    buildFlexCardData(userPhone)
                }
                _flexCardData.value = cardData
                lastGeneratedMonthKey = monthKey
            } catch (e: Exception) {
                _flexCardError.value = e.message ?: "Unable to generate Flex Card"
            } finally {
                _flexCardLoading.value = false
            }
        }
    }

    private suspend fun buildFlexCardData(userPhone: String): FlexCardData {
        val now = Calendar.getInstance()
        val monthStart = now.monthRange(0)
        val previousMonth = now.monthRange(-1)
        val beforePreviousMonth = now.monthRange(-2)

        val currentExpenses = queryExpenses(userPhone, monthStart.first, monthStart.second)
        val previousExpenses = queryExpenses(userPhone, previousMonth.first, previousMonth.second)
        val twoMonthAgoExpenses = queryExpenses(userPhone, beforePreviousMonth.first, beforePreviousMonth.second)

        val expenseOnly = currentExpenses.filter { it.type.uppercase() != "INCOME" }
        val previousExpenseOnly = previousExpenses.filter { it.type.uppercase() != "INCOME" }
        val twoMonthAgoExpenseOnly = twoMonthAgoExpenses.filter { it.type.uppercase() != "INCOME" }

        val totalSpend = expenseOnly.sumOf { abs(it.baseAmount) }
        val previousSpend = previousExpenseOnly.sumOf { abs(it.baseAmount) }
        val twoMonthAgoSpend = twoMonthAgoExpenseOnly.sumOf { abs(it.baseAmount) }

        val settings = settingsRepository.getSettingsOnce()
        val income = settings.monthlyIncome

        val displayMonth = now.get(Calendar.MONTH) + 1
        val displayYear = now.get(Calendar.YEAR)
        val previousDisplayMonth = if (displayMonth == 1) 12 else displayMonth - 1
        val previousDisplayYear = if (displayMonth == 1) displayYear - 1 else displayYear

        val budget = budgetRepository
            .getOverallBudget(userPhone, displayMonth, displayYear)
            ?.limitAmount
            ?: 0.0

        val previousBudget = budgetRepository
            .getOverallBudget(userPhone, previousDisplayMonth, previousDisplayYear)
            ?.limitAmount
            ?: budget

        val categoryTotals = expenseOnly.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { abs(it.baseAmount) } }
        val maxCategoryShare = if (totalSpend <= 0.0) {
            0.0
        } else {
            (categoryTotals.maxOfOrNull { it.value } ?: 0.0) / totalSpend
        }

        val streakDays = streakRepository.getStreakSync(userPhone)?.currentStreak ?: 0
        val totalTransactions = expenseOnly.size
        val smallTransactions = expenseOnly.count { abs(it.baseAmount) <= SMALL_TX_BASE_THRESHOLD_USD }
        val smallTxRatio = if (totalTransactions == 0) 0.0 else smallTransactions.toDouble() / totalTransactions

        val lastMonthScore = FlexScoreCalculator.calculate(
            FlexScoreInput(
                totalSpend = previousSpend,
                totalIncome = income,
                budget = previousBudget,
                previousMonthSpend = twoMonthAgoSpend,
                maxCategoryShare = computeMaxCategoryShare(previousExpenseOnly),
                streakDays = streakDays,
                smallTransactionRatio = computeSmallTxRatio(previousExpenseOnly),
                totalTransactions = previousExpenseOnly.size,
                lastMonthScore = null
            )
        ).score

        val score = FlexScoreCalculator.calculate(
            FlexScoreInput(
                totalSpend = totalSpend,
                totalIncome = income,
                budget = budget,
                previousMonthSpend = previousSpend,
                maxCategoryShare = maxCategoryShare,
                streakDays = streakDays,
                smallTransactionRatio = smallTxRatio,
                totalTransactions = totalTransactions,
                lastMonthScore = lastMonthScore
            )
        )

        val topCategory = categoryTotals.maxByOrNull { it.value }?.key ?: "Other"
        val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.US).format(now.time)
        val summary = FlexCardSummaryBuilder.build(
            FlexCardSummaryBuilder.Input(
                totalSpend = totalSpend,
                totalIncome = income,
                budget = budget,
                previousSpend = previousSpend,
                topCategory = topCategory,
                maxCategoryShare = maxCategoryShare,
                streakDays = streakDays,
                smallTransactionRatio = smallTxRatio,
                totalTransactions = totalTransactions,
                monthLabel = monthLabel
            )
        )

        val llm = aiManager.generateFlexCardContent(summary, currentMonthKey(now))

        val profile = currentUser.value
        val name = profile?.name?.takeIf { it.isNotBlank() }
            ?: profile?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: profile?.phone?.takeLast(6)?.let { "User $it" }
            ?: "User"
        val initials = buildInitials(name)

        return FlexCardData(
            userName = name,
            initials = initials,
            profilePhotoUri = settings.profilePhotoUri?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() },
            score = score,
            highlights = llm.highlights,
            improvement = llm.improvement,
            tagline = llm.tagline,
            monthlySpendText = CurrencyProvider.formatAmount(totalSpend),
            streakText = "${streakDays}-Day",
            template = selectedTemplate.value,
            monthLabel = monthLabel
        )
    }

    private suspend fun queryExpenses(userPhone: String, start: Long, end: Long): List<PersonalExpenseEntity> {
        return personalExpenseRepository
            .getExpensesInRange(userPhone, start, end)
            .firstOrNull()
            .orEmpty()
    }

    private fun computeMaxCategoryShare(expenses: List<PersonalExpenseEntity>): Double {
        val total = expenses.sumOf { abs(it.baseAmount) }
        if (total <= 0.0) return 0.0

        val max = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { abs(it.baseAmount) } }
            .maxOfOrNull { it.value }
            ?: 0.0

        return max / total
    }

    private fun computeSmallTxRatio(expenses: List<PersonalExpenseEntity>): Double {
        if (expenses.isEmpty()) return 0.0
        val small = expenses.count { abs(it.baseAmount) <= SMALL_TX_BASE_THRESHOLD_USD }
        return small.toDouble() / expenses.size
    }

    private fun buildInitials(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        if (parts.isEmpty()) return "FF"
        if (parts.size == 1) return parts.first().take(2).uppercase(Locale.US)
        return "${parts[0].first()}${parts[1].first()}".uppercase(Locale.US)
    }

    private fun currentMonthKey(calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(year, month)
    }

    private fun Calendar.monthRange(monthOffset: Int): Pair<Long, Long> {
        val local = (this.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, monthOffset)
        }
        val start = local.timeInMillis
        local.add(Calendar.MONTH, 1)
        val end = local.timeInMillis - 1L
        return start to end
    }

    private fun observeRecurring() {
        viewModelScope.launch {
            recurringRepository.getAllRecurring().collect {
                _state.value = _state.value.copy(recurring = it)
                _recurringFlow.value = it.sortedBy { tx -> tx.nextOccurrence }
            }
        }
    }

    fun refreshDisplayRate() {
        viewModelScope.launch {
            val target = _state.value.settings.displayCurrency
            val rate = exchangeRateApi.getRate("USD", target, System.currentTimeMillis())
            CurrencyProvider.setDisplayCurrency(target)
            CurrencyProvider.setDisplayRateFromBase(rate)
        }
    }

    fun updateTheme(isDarkMode: Boolean) {
        saveSettings(_state.value.settings.copy(isDarkMode = isDarkMode))
    }

    fun updateDisplayCurrency(code: String) {
        saveSettings(_state.value.settings.copy(displayCurrency = code))
        viewModelScope.launch {
            val rate = exchangeRateApi.getRate("USD", code, System.currentTimeMillis())
            CurrencyProvider.setDisplayCurrency(code)
            CurrencyProvider.setDisplayRateFromBase(rate)
        }
    }

    fun updateMonthlyIncomeInDisplayCurrency(amountInDisplay: Double) {
        viewModelScope.launch {
            syncDisplayRateForSettings()
            val baseAmount = CurrencyProvider.convertToBase(amountInDisplay)
            settingsRepository.saveSettings(_state.value.settings.copy(monthlyIncome = baseAmount))
        }
    }

    fun updateBankBalanceInDisplayCurrency(amountInDisplay: Double) {
        viewModelScope.launch {
            syncDisplayRateForSettings()
            val baseAmount = CurrencyProvider.convertToBase(amountInDisplay)
            settingsRepository.setBalanceAnchor(baseAmount)
            settingsRepository.saveSettings(_state.value.settings.copy(currentBankBalance = baseAmount))
        }
    }

    /** Saves both income and balance in a single write to avoid race conditions. */
    fun saveFinancialSettings(incomeInDisplay: Double, balanceInDisplay: Double) {
        viewModelScope.launch {
            syncDisplayRateForSettings()
            val baseIncome = CurrencyProvider.convertToBase(incomeInDisplay)
            val baseBalance = CurrencyProvider.convertToBase(balanceInDisplay)
            settingsRepository.setBalanceAnchor(baseBalance)
            settingsRepository.saveSettings(
                _state.value.settings.copy(
                    monthlyIncome = baseIncome,
                    currentBankBalance = baseBalance
                )
            )
        }
    }

    private suspend fun syncDisplayRateForSettings() {
        val currencyCode = _state.value.settings.displayCurrency
        val rate = exchangeRateApi.getRate("USD", currencyCode, System.currentTimeMillis())
        CurrencyProvider.setDisplayCurrency(currencyCode)
        CurrencyProvider.setDisplayRateFromBase(rate)
    }

    fun updateProfilePhoto(uri: String?) {
        saveSettings(_state.value.settings.copy(profilePhotoUri = uri))
    }

    fun updateProfileIdentity(
        name: String,
        email: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val trimmedName = name.trim()
                if (trimmedName.isBlank()) {
                    onError("Name cannot be empty")
                    return@launch
                }
                userRepository.updateCurrentUserProfile(trimmedName, email)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update profile")
            }
        }
    }

    private fun saveSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            settingsRepository.saveSettings(settings)
        }
    }

    fun addRecurring(
        title: String,
        amount: Double,
        currency: String,
        category: String,
        type: String,
        interval: String
    ) {
        viewModelScope.launch {
            recurringRepository.createRecurring(
                title = title,
                amount = amount,
                currency = currency,
                category = category,
                type = type,
                interval = interval,
                firstOccurrence = System.currentTimeMillis()
            )
        }
    }

    fun toggleRecurring(id: String, active: Boolean) {
        viewModelScope.launch {
            recurringRepository.setStatus(id, active)
        }
    }

    fun deleteRecurring(transaction: RecurringTransactionEntity) {
        viewModelScope.launch {
            recurringRepository.deleteRecurring(transaction)
        }
    }

    fun runRecurringNow() {
        viewModelScope.launch {
            processDueRecurringTransactions()
        }
    }

    private suspend fun processDueRecurringTransactions() {
        val userPhone = authService.getCurrentUser()?.phoneNumber ?: return
        val now = System.currentTimeMillis()
        val dueTransactions = recurringRepository.getDueTransactions(now)
        if (dueTransactions.isEmpty()) return

        var currentSettings = settingsRepository.getSettingsOnce()

        dueTransactions.forEach { tx ->
            val rateToBase = exchangeRateApi.getRate(tx.currency, "USD", tx.nextOccurrence)
            val baseAmount = tx.amount * rateToBase

            if (tx.type.uppercase() == "EXPENSE") {
                personalExpenseRepository.insertFromRecurring(
                    userPhone = userPhone,
                    title = tx.title,
                    amount = tx.amount,
                    currency = tx.currency,
                    category = tx.category,
                    dateMillis = tx.nextOccurrence
                )
                currentSettings = currentSettings.copy(
                    currentBankBalance = currentSettings.currentBankBalance - baseAmount
                )
            } else {
                currentSettings = currentSettings.copy(
                    currentBankBalance = currentSettings.currentBankBalance + baseAmount
                )
            }

            recurringRepository.updateRecurring(
                tx.copy(
                    lastProcessed = now,
                    nextOccurrence = nextOccurrence(tx.interval, tx.nextOccurrence)
                )
            )
        }

        settingsRepository.saveSettings(currentSettings)
    }

    private fun nextOccurrence(interval: String, from: Long): Long {
        val dayMillis = 24L * 60L * 60L * 1000L
        return when (interval.uppercase()) {
            "DAILY" -> from + dayMillis
            "WEEKLY" -> from + (7L * dayMillis)
            "MONTHLY" -> from + (30L * dayMillis)
            "YEARLY" -> from + (365L * dayMillis)
            else -> from + (30L * dayMillis)
        }
    }
}
