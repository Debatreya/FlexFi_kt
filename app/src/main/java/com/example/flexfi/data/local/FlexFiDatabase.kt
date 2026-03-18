package com.example.flexfi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.flexfi.data.local.dao.*
import com.example.flexfi.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        ContactEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        CategoryEntity::class,
        StreakEntity::class,
        PersonalExpenseEntity::class,
        BudgetGoalEntity::class,
        SettlementRecordEntity::class,
        AppSettingsEntity::class,
        BudgetEntity::class,
        GoalContributionEntity::class,
        RecurringTransactionEntity::class,
        AIInsightEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class FlexFiDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun personalExpenseDao(): PersonalExpenseDao
    abstract fun streakDao(): StreakDao
    abstract fun budgetGoalDao(): BudgetGoalDao
    abstract fun settlementDao(): SettlementDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun aiInsightDao(): AIInsightDao
}
