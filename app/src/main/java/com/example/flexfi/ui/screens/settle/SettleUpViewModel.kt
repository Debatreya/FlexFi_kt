package com.example.flexfi.ui.screens.settle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.data.local.entities.GroupEntity
import com.example.flexfi.data.remote.ExchangeRateApi
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.repository.ContactRepository
import com.example.flexfi.data.repository.ExpenseRepository
import com.example.flexfi.data.repository.GroupRepository
import com.example.flexfi.data.repository.Settlement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettleUpDebt(
    val personPhone: String,
    val personName: String,
    val amount: Double,
    val youOwe: Boolean, // true = you owe them, false = they owe you
    val groupId: String?
)

class SettleUpViewModel(
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository,
    private val authService: FirebaseAuthService,
    private val exchangeRateApi: ExchangeRateApi
) : ViewModel() {

    private val currentUserPhone = authService.getCurrentUser()?.phoneNumber ?: ""

    private val _debts = MutableStateFlow<List<SettleUpDebt>>(emptyList())
    val debts: StateFlow<List<SettleUpDebt>> = _debts.asStateFlow()

    private val _totalYouOwe = MutableStateFlow(0.0)
    val totalYouOwe: StateFlow<Double> = _totalYouOwe.asStateFlow()

    private val _totalOwedToYou = MutableStateFlow(0.0)
    val totalOwedToYou: StateFlow<Double> = _totalOwedToYou.asStateFlow()

    init {
        loadAllDebts()
    }

    fun loadAllDebts() {
        viewModelScope.launch {
            val groups = groupRepository.getGroupsForUserOnce(currentUserPhone)
            val allDebts = mutableListOf<SettleUpDebt>()
            val contacts = contactRepository.getAllContactsOnce()

            for (group in groups) {
                val balances = expenseRepository.calculateGroupBalances(group.id)
                val settlements = expenseRepository.calculateSettlements(balances)

                for (settlement in settlements) {
                    if (settlement.fromPhone == currentUserPhone) {
                        val name = contacts.find { it.phone == settlement.toPhone }?.name
                            ?: settlement.toPhone
                        allDebts.add(
                            SettleUpDebt(
                                personPhone = settlement.toPhone,
                                personName = name,
                                amount = settlement.amount,
                                youOwe = true,
                                groupId = group.id
                            )
                        )
                    } else if (settlement.toPhone == currentUserPhone) {
                        val name = contacts.find { it.phone == settlement.fromPhone }?.name
                            ?: settlement.fromPhone
                        allDebts.add(
                            SettleUpDebt(
                                personPhone = settlement.fromPhone,
                                personName = name,
                                amount = settlement.amount,
                                youOwe = false,
                                groupId = group.id
                            )
                        )
                    }
                }
            }

            _debts.value = allDebts
            _totalYouOwe.value = allDebts.filter { it.youOwe }.sumOf { it.amount }
            _totalOwedToYou.value = allDebts.filter { !it.youOwe }.sumOf { it.amount }
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
                expenseRepository.recordSettlement(
                    groupId = groupId,
                    fromPhone = currentUserPhone,
                    toPhone = toPhone,
                    amount = amount * rateToBase,
                    note = "Settled externally"
                )
                loadAllDebts() // Refresh
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to record payment")
            }
        }
    }
}
