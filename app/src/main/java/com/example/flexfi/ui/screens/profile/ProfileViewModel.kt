package com.example.flexfi.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.data.local.entities.AppSettingsEntity
import com.example.flexfi.data.local.entities.RecurringTransactionEntity
import com.example.flexfi.data.remote.ExchangeRateApi
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.repository.AppSettingsRepository
import com.example.flexfi.data.repository.PersonalExpenseRepository
import com.example.flexfi.data.repository.RecurringTransactionRepository
import com.example.flexfi.utils.CurrencyProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private data class ProfileState(
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val recurring: List<RecurringTransactionEntity> = emptyList()
)

class ProfileViewModel(
    private val authService: FirebaseAuthService,
    private val settingsRepository: AppSettingsRepository,
    private val recurringRepository: RecurringTransactionRepository,
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val exchangeRateApi: ExchangeRateApi
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())

    private val _settingsFlow = MutableStateFlow(AppSettingsEntity())
    val settings: StateFlow<AppSettingsEntity> = _settingsFlow.asStateFlow()

    private val _recurringFlow = MutableStateFlow<List<RecurringTransactionEntity>>(emptyList())
    val recurring: StateFlow<List<RecurringTransactionEntity>> = _recurringFlow.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.ensureDefaults()
            observeSettings()
            observeRecurring()
            processDueRecurringTransactions()
            refreshDisplayRate()
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect {
                _state.value = _state.value.copy(settings = it)
                _settingsFlow.value = it
                CurrencyProvider.setDisplayCurrency(it.displayCurrency)
            }
        }
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
            settingsRepository.saveSettings(_state.value.settings.copy(currentBankBalance = baseAmount))
        }
    }

    /** Saves both income and balance in a single write to avoid race conditions. */
    fun saveFinancialSettings(incomeInDisplay: Double, balanceInDisplay: Double) {
        viewModelScope.launch {
            syncDisplayRateForSettings()
            val baseIncome = CurrencyProvider.convertToBase(incomeInDisplay)
            val baseBalance = CurrencyProvider.convertToBase(balanceInDisplay)
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
