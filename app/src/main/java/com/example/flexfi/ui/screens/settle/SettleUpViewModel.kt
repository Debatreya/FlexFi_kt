package com.example.flexfi.ui.screens.settle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.data.local.entities.GroupEntity
import com.example.flexfi.data.remote.ExchangeRateApi
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.repository.AppSettingsRepository
import com.example.flexfi.data.repository.ContactRepository
import com.example.flexfi.data.repository.ExpenseRepository
import com.example.flexfi.data.repository.GroupRepository
import com.example.flexfi.data.repository.Settlement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SettleUpDebt(
    val canonicalPhone: String,
    val personPhone: String,
    val personName: String,
    val amountBase: Double,
    val youOwe: Boolean, // true = you owe them, false = they owe you
    val groupId: String?
)

data class PendingPaymentUI(
    val id: String,
    val otherPhone: String,
    val otherName: String,
    val amount: Double,
    val isIncoming: Boolean // true if I am receiving, false if I am sending
)

class SettleUpViewModel(
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository,
    private val authService: FirebaseAuthService,
    private val exchangeRateApi: ExchangeRateApi,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val currentUserPhone = authService.getCurrentUser()?.phoneNumber ?: ""

    private val _debts = MutableStateFlow<List<SettleUpDebt>>(emptyList())
    val debts: StateFlow<List<SettleUpDebt>> = _debts.asStateFlow()

    private val _totalYouOwe = MutableStateFlow(0.0)
    val totalYouOwe: StateFlow<Double> = _totalYouOwe.asStateFlow()

    private val _totalOwedToYou = MutableStateFlow(0.0)
    val totalOwedToYou: StateFlow<Double> = _totalOwedToYou.asStateFlow()

    private val _pendingPayments = MutableStateFlow<List<PendingPaymentUI>>(emptyList())
    val pendingPayments: StateFlow<List<PendingPaymentUI>> = _pendingPayments.asStateFlow()

    private var currentGroupIdTarget: String? = null

    init {
        loadAllDebts()
        observeSettlements()
    }

    private fun canonicalPhone(phone: String): String {
        val trimmed = phone.trim()
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    private fun pickPreferredPhone(current: String?, candidate: String): String {
        if (current.isNullOrBlank()) return candidate
        if (current.startsWith("+") && !candidate.startsWith("+")) return current
        if (!current.startsWith("+") && candidate.startsWith("+")) return candidate
        return if (candidate.length > current.length) candidate else current
    }

    fun loadAllDebts(targetGroupId: String? = currentGroupIdTarget) {
        currentGroupIdTarget = targetGroupId
        viewModelScope.launch {
            expenseRepository.syncSettlementsForUser(currentUserPhone)

            val netBalancesByPhone = mutableMapOf<String, Double>()
            val canonicalToOriginalPhone = mutableMapOf<String, String>()
            
            if (targetGroupId == null) {
                // Global view: aggregate all groups
                val groups = groupRepository.getGroupsForUserOnce(currentUserPhone)
                for (group in groups) {
                    expenseRepository.syncExpensesForGroup(group.id)
                    val balances = expenseRepository.calculateGroupBalances(group.id)
                    val settlements = expenseRepository.calculateSettlements(balances)
                    for (s in settlements) {
                        if (s.fromPhone == currentUserPhone) {
                            val key = canonicalPhone(s.toPhone)
                            canonicalToOriginalPhone[key] = pickPreferredPhone(canonicalToOriginalPhone[key], s.toPhone)
                            netBalancesByPhone[key] = (netBalancesByPhone[key] ?: 0.0) - s.amount
                        } else if (s.toPhone == currentUserPhone) {
                            val key = canonicalPhone(s.fromPhone)
                            canonicalToOriginalPhone[key] = pickPreferredPhone(canonicalToOriginalPhone[key], s.fromPhone)
                            netBalancesByPhone[key] = (netBalancesByPhone[key] ?: 0.0) + s.amount
                        }
                    }
                }
                
                // Add direct settlements (groupId == null && status == COMPLETED)
                val allSettlements = expenseRepository.getSettlementsForUserOnce(currentUserPhone)
                val globalSettlements = allSettlements.filter { it.groupId == null && it.status == "COMPLETED" }
                for (s in globalSettlements) {
                    if (s.fromPhone == currentUserPhone) {
                        val key = canonicalPhone(s.toPhone)
                        canonicalToOriginalPhone[key] = pickPreferredPhone(canonicalToOriginalPhone[key], s.toPhone)
                        netBalancesByPhone[key] = (netBalancesByPhone[key] ?: 0.0) + s.amount
                    } else if (s.toPhone == currentUserPhone) {
                        val key = canonicalPhone(s.fromPhone)
                        canonicalToOriginalPhone[key] = pickPreferredPhone(canonicalToOriginalPhone[key], s.fromPhone)
                        netBalancesByPhone[key] = (netBalancesByPhone[key] ?: 0.0) - s.amount
                    }
                }
            } else {
                // Group-specific view
                expenseRepository.syncExpensesForGroup(targetGroupId)
                val balances = expenseRepository.calculateGroupBalances(targetGroupId)
                val settlements = expenseRepository.calculateSettlements(balances)
                for (s in settlements) {
                    if (s.fromPhone == currentUserPhone) {
                        val key = canonicalPhone(s.toPhone)
                        canonicalToOriginalPhone[key] = pickPreferredPhone(canonicalToOriginalPhone[key], s.toPhone)
                        netBalancesByPhone[key] = (netBalancesByPhone[key] ?: 0.0) - s.amount
                    } else if (s.toPhone == currentUserPhone) {
                        val key = canonicalPhone(s.fromPhone)
                        canonicalToOriginalPhone[key] = pickPreferredPhone(canonicalToOriginalPhone[key], s.fromPhone)
                        netBalancesByPhone[key] = (netBalancesByPhone[key] ?: 0.0) + s.amount
                    }
                }
            }

            // Convert to SettleUpDebt
            val contacts = contactRepository.getAllContactsOnce()
            val contactsByCanonical = contacts.associateBy { canonicalPhone(it.phone) }
            val allDebts = mutableListOf<SettleUpDebt>()
            for ((canonical, net) in netBalancesByPhone) {
                if (kotlin.math.abs(net) < 0.01) continue
                val originalPhone = canonicalToOriginalPhone[canonical] ?: canonical
                val name = contactsByCanonical[canonical]?.name ?: originalPhone
                allDebts.add(
                    SettleUpDebt(
                        canonicalPhone = canonical,
                        personPhone = originalPhone,
                        personName = name,
                        amountBase = kotlin.math.abs(net),
                        youOwe = net < 0,
                        groupId = targetGroupId
                    )
                )
            }

            _debts.value = allDebts
            _totalYouOwe.value = allDebts.filter { it.youOwe }.sumOf { it.amountBase }
            _totalOwedToYou.value = allDebts.filter { !it.youOwe }.sumOf { it.amountBase }
        }
    }

    fun recordPayment(
        toPhone: String,
        amount: Double,
        currency: String,
        groupId: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val rateToBase = exchangeRateApi.getRate(currency, "USD", System.currentTimeMillis())
                val baseAmount = amount * rateToBase
                expenseRepository.recordSettlement(
                    groupId = groupId,
                    fromPhone = currentUserPhone,
                    toPhone = toPhone,
                    amount = baseAmount,
                    note = "Pending approval"
                )
                loadAllDebts() // Refresh
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to record payment")
            }
        }
    }

    fun recordPaymentInBase(
        toPhone: String,
        amountBase: Double,
        groupId: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                expenseRepository.recordSettlement(
                    groupId = groupId,
                    fromPhone = currentUserPhone,
                    toPhone = toPhone,
                    amount = amountBase,
                    note = "Pending approval"
                )
                loadAllDebts()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to record payment")
            }
        }
    }

    private fun observeSettlements() {
        viewModelScope.launch {
            expenseRepository.getSettlementsForUser(currentUserPhone)?.collectLatest { records ->
                val contacts = contactRepository.getAllContactsOnce()
                val pendingList = records.filter { it.status == "PENDING" }.map { record ->
                    val isIncoming = record.toPhone == currentUserPhone
                    val otherPhone = if (isIncoming) record.fromPhone else record.toPhone
                    val otherName = contacts.find { it.phone == otherPhone }?.name ?: otherPhone
                    PendingPaymentUI(
                        id = record.id,
                        otherPhone = otherPhone,
                        otherName = otherName,
                        amount = record.amount,
                        isIncoming = isIncoming
                    )
                }
                _pendingPayments.value = pendingList
            }
        }
    }

    fun acceptPayment(settlementId: String) {
        viewModelScope.launch {
            val settlement = expenseRepository.getSettlementById(settlementId)
            if (settlement != null) {
                appSettingsRepository.adjustCurrentBankBalance(settlement.amount) // receiver gets money
            }
            expenseRepository.updateSettlementStatus(settlementId, "COMPLETED")
            loadAllDebts()
        }
    }

    fun rejectPayment(settlementId: String) {
        viewModelScope.launch {
            expenseRepository.updateSettlementStatus(settlementId, "REJECTED")
            loadAllDebts()
        }
    }
}
