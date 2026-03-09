package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.ExpenseDao
import com.example.flexfi.data.local.dao.PersonalExpenseDao
import com.example.flexfi.data.local.entities.ExpenseEntity
import com.example.flexfi.data.local.entities.ExpenseSplitEntity
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import com.example.flexfi.data.remote.FirestoreExpenseService
import com.example.flexfi.data.remote.firestoreModels.ExpenseDoc
import com.example.flexfi.data.remote.firestoreModels.SplitDoc
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Represents a simplified settlement: fromPhone owes toPhone the given amount.
 */
data class Settlement(
    val fromPhone: String,
    val toPhone: String,
    val amount: Double
)

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val firestoreExpenseService: FirestoreExpenseService,
    private val personalExpenseDao: PersonalExpenseDao
) {

    // ──────────────────────────────────────────────
    //  CREATE
    // ──────────────────────────────────────────────

    /**
     * Creates an expense with splits and pushes to Firestore.
     *
     * @param splitType "equal" or "exact"
     * @param selectedMemberPhones phones of members participating in this expense
     * @param exactAmounts required when splitType == "exact"; map of phone→amount
     */
    suspend fun addExpense(
        title: String,
        amount: Double,
        groupId: String,
        paidByPhone: String,
        category: String,
        splitType: String,
        selectedMemberPhones: List<String>,
        exactAmounts: Map<String, Double>? = null
    ) {
        val expenseId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val expense = ExpenseEntity(
            id = expenseId,
            groupId = groupId,
            title = title,
            amount = amount,
            paidByPhone = paidByPhone,
            category = category.ifBlank { "Other" },
            createdAt = now
        )

        val splits = when (splitType) {
            "exact" -> {
                val amounts = exactAmounts
                    ?: throw IllegalArgumentException("exactAmounts required for exact split")
                val sum = amounts.values.sum()
                if (kotlin.math.abs(sum - amount) > 0.01) {
                    throw IllegalArgumentException(
                        "Exact split amounts (₹${"%.2f".format(sum)}) don't match total (₹${"%.2f".format(amount)})"
                    )
                }
                selectedMemberPhones.map { phone ->
                    ExpenseSplitEntity(
                        id = UUID.randomUUID().toString(),
                        expenseId = expenseId,
                        memberPhone = phone,
                        shareAmount = amounts[phone] ?: 0.0
                    )
                }
            }
            else -> { // "equal"
                val share = kotlin.math.round((amount / selectedMemberPhones.size) * 100) / 100.0
                val mutableSplits = mutableListOf<ExpenseSplitEntity>()
                var totalSplitSoFar = 0.0

                for (i in 0 until selectedMemberPhones.size - 1) {
                    mutableSplits.add(
                        ExpenseSplitEntity(
                            id = UUID.randomUUID().toString(),
                            expenseId = expenseId,
                            memberPhone = selectedMemberPhones[i],
                            shareAmount = share
                        )
                    )
                    totalSplitSoFar += share
                }

                // Last member gets the remainder to ensure sum equals exactly the total amount
                val lastMemberPhone = selectedMemberPhones.last()
                val lastShare = kotlin.math.round((amount - totalSplitSoFar) * 100) / 100.0
                mutableSplits.add(
                    ExpenseSplitEntity(
                        id = UUID.randomUUID().toString(),
                        expenseId = expenseId,
                        memberPhone = lastMemberPhone,
                        shareAmount = lastShare
                    )
                )
                mutableSplits
            }
        }

        // Save to Room
        expenseDao.insertExpense(expense)
        expenseDao.insertSplits(splits)

        // Generate personal expense records for every participant (offline-first, Room-only)
        createPersonalExpenses(expense, splits)

        // Push to Firestore
        val expenseDoc = ExpenseDoc(
            id = expenseId,
            groupId = groupId,
            title = title,
            amount = amount,
            paidByPhone = paidByPhone,
            category = expense.category,
            createdAt = now,
            splits = splits.map { SplitDoc(phone = it.memberPhone, share = it.shareAmount) }
        )
        firestoreExpenseService.createExpense(expenseDoc)
    }

    // ──────────────────────────────────────────────
    //  READ
    // ──────────────────────────────────────────────

    fun getExpensesForGroup(groupId: String): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesForGroup(groupId)

    fun getSplitsForExpense(expenseId: String): Flow<List<ExpenseSplitEntity>> =
        expenseDao.getSplitsForExpense(expenseId)

    // ──────────────────────────────────────────────
    //  SYNC
    // ──────────────────────────────────────────────

    suspend fun syncExpensesForGroup(groupId: String) {
        val remoteDocs = firestoreExpenseService.getExpensesForGroup(groupId)

        // Clear local cache for this group (both group expenses and their personal mirrors)
        expenseDao.deleteSplitsForGroup(groupId)
        expenseDao.deleteExpensesForGroup(groupId)
        personalExpenseDao.deleteBySourceGroupId(groupId)

        // Re-populate from Firestore
        remoteDocs.forEach { doc ->
            val entity = ExpenseEntity(
                id = doc.id,
                groupId = doc.groupId,
                title = doc.title,
                amount = doc.amount,
                paidByPhone = doc.paidByPhone,
                category = doc.category.takeIf { it.isNotBlank() } ?: "Other",
                createdAt = doc.createdAt
            )
            expenseDao.insertExpense(entity)

            val splits = doc.splits.map { splitDoc ->
                ExpenseSplitEntity(
                    id = UUID.randomUUID().toString(),
                    expenseId = doc.id,
                    memberPhone = splitDoc.phone,
                    shareAmount = splitDoc.share
                )
            }
            expenseDao.insertSplits(splits)

            // Rebuild personal expense mirrors from synced data
            createPersonalExpenses(entity, splits)
        }
    }

    // ──────────────────────────────────────────────
    //  DEBT CALCULATION ENGINE
    // ──────────────────────────────────────────────

    /**
     * Calculates net balance for every member in the group.
     * Positive = is owed money (creditor), Negative = owes money (debtor).
     *
     * For each expense:
     *   balances[paidBy] += amount
     *   for each split: balances[member] -= share
     */
    suspend fun calculateGroupBalances(groupId: String): Map<String, Double> {
        val expenses = expenseDao.getExpensesForGroupOnce(groupId)
        val splits = expenseDao.getSplitsForGroup(groupId)

        val balances = mutableMapOf<String, Double>()

        // Credit the payer with the full amount
        for (expense in expenses) {
            balances[expense.paidByPhone] =
                (balances[expense.paidByPhone] ?: 0.0) + expense.amount
        }

        // Debit each participant with their share
        for (split in splits) {
            balances[split.memberPhone] =
                (balances[split.memberPhone] ?: 0.0) - split.shareAmount
        }

        return balances
    }

    /**
     * Converts a balance map into a minimal list of settlements.
     * Uses a greedy approach: match the largest debtor with the largest creditor.
     */
    fun calculateSettlements(balances: Map<String, Double>): List<Settlement> {
        val creditors = mutableListOf<Pair<String, Double>>() // phone, amount owed to them
        val debtors = mutableListOf<Pair<String, Double>>()   // phone, amount they owe

        for ((phone, balance) in balances) {
            when {
                balance > 0.01 -> creditors.add(phone to balance)
                balance < -0.01 -> debtors.add(phone to -balance) // store as positive
            }
        }

        // Sort descending by amount for greedy matching
        creditors.sortByDescending { it.second }
        debtors.sortByDescending { it.second }

        val settlements = mutableListOf<Settlement>()
        var ci = 0
        var di = 0

        val credAmounts = creditors.map { it.second }.toMutableList()
        val debtAmounts = debtors.map { it.second }.toMutableList()

        while (ci < creditors.size && di < debtors.size) {
            val settle = minOf(credAmounts[ci], debtAmounts[di])
            settlements.add(
                Settlement(
                    fromPhone = debtors[di].first,
                    toPhone = creditors[ci].first,
                    amount = kotlin.math.round(settle * 100) / 100.0
                )
            )
            credAmounts[ci] -= settle
            debtAmounts[di] -= settle

            if (credAmounts[ci] < 0.01) ci++
            if (debtAmounts[di] < 0.01) di++
        }

        return settlements
    }

    // ──────────────────────────────────────────────
    //  PERSONAL EXPENSE MIRROR
    // ──────────────────────────────────────────────

    /**
     * Creates one [PersonalExpenseEntity] per split, recording each member's
     * individual share as a personal expense record in the local DB.
     *
     * Called after splits are written in [addExpense] and [syncExpensesForGroup].
     */
    private suspend fun createPersonalExpenses(
        expense: ExpenseEntity,
        splits: List<ExpenseSplitEntity>
    ) {
        splits.forEach { split ->
            val personal = PersonalExpenseEntity(
                id = UUID.randomUUID().toString(),
                userPhone = split.memberPhone,
                amount = split.shareAmount,
                category = expense.category,
                source = "GROUP",
                sourceExpenseId = expense.id,
                sourceGroupId = expense.groupId,
                description = expense.title,
                createdAt = expense.createdAt
            )
            personalExpenseDao.insert(personal)
        }
    }

    // ──────────────────────────────────────────────
    //  DELETE
    // ──────────────────────────────────────────────

    /**
     * Deletes a single expense and all its associated data:
     * - expense_splits rows for this expense
     * - the expense row itself
     * - personal_expenses mirrors created from this expense
     */
    suspend fun deleteExpense(expenseId: String) {
        // Remove splits first (FK-safe ordering)
        expenseDao.deleteSplitsByExpenseId(expenseId)
        expenseDao.deleteExpenseById(expenseId)
        // Remove the personal mirrors that were created from this expense
        personalExpenseDao.deleteBySourceExpenseId(expenseId)
    }
}
