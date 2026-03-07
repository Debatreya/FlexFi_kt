package com.example.flexfi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.flexfi.data.local.dao.ContactDao
import com.example.flexfi.data.local.dao.ExpenseDao
import com.example.flexfi.data.local.dao.GroupDao
import com.example.flexfi.data.local.dao.UserDao
import com.example.flexfi.data.local.entities.CategoryEntity
import com.example.flexfi.data.local.entities.ContactEntity
import com.example.flexfi.data.local.entities.ExpenseEntity
import com.example.flexfi.data.local.entities.ExpenseSplitEntity
import com.example.flexfi.data.local.entities.GroupEntity
import com.example.flexfi.data.local.entities.GroupMemberEntity
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
        StreakEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FlexFiDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
}
