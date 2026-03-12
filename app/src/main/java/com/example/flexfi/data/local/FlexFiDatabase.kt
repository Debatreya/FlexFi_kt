package com.example.flexfi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.flexfi.data.local.dao.BudgetGoalDao
import com.example.flexfi.data.local.dao.ContactDao
import com.example.flexfi.data.local.dao.ExpenseDao
import com.example.flexfi.data.local.dao.GroupDao
import com.example.flexfi.data.local.dao.PersonalExpenseDao
import com.example.flexfi.data.local.dao.SettlementDao
import com.example.flexfi.data.local.dao.StreakDao
import com.example.flexfi.data.local.dao.UserDao
import com.example.flexfi.data.local.entities.BudgetGoalEntity
import com.example.flexfi.data.local.entities.CategoryEntity
import com.example.flexfi.data.local.entities.ContactEntity
import com.example.flexfi.data.local.entities.ExpenseEntity
import com.example.flexfi.data.local.entities.ExpenseSplitEntity
import com.example.flexfi.data.local.entities.GroupEntity
import com.example.flexfi.data.local.entities.GroupMemberEntity
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import com.example.flexfi.data.local.entities.SettlementRecordEntity
import com.example.flexfi.data.local.entities.StreakEntity
import com.example.flexfi.data.local.entities.UserEntity

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
        SettlementRecordEntity::class
    ],
    version = 8, // Bumped to 8 for BudgetGoals + Settlements (Phase 8)
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
}
