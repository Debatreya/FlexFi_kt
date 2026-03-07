package com.example.flexfi

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.room.Room
import com.example.flexfi.data.local.FlexFiDatabase
import com.example.flexfi.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Database
        val db = Room.databaseBuilder(
            applicationContext,
            FlexFiDatabase::class.java,
            "flexfi_db"
        ).fallbackToDestructiveMigration().build()

        // Run Tests
        CoroutineScope(Dispatchers.IO).launch {
            try {
                testDatabase(db)
            } catch (e: Exception) {
                Log.e("PHASE1_TEST", "Test failed", e)
            }
        }

        setContent {
            FlexFiApp()
        }
    }

    private suspend fun testDatabase(db: FlexFiDatabase) {
        val userDao = db.userDao()
        val contactDao = db.contactDao()
        val groupDao = db.groupDao()
        val expenseDao = db.expenseDao()

        // 1. Test Users
        val user = UserEntity(
            id = "user_001",
            name = "Deb",
            phone = "9999999999",
            email = "deb@flexfi.com",
            joinedAt = System.currentTimeMillis(),
            streakCount = 5,
            totalExpense = 1500.0
        )
        userDao.insertUser(user)
        val fetchedUser = userDao.getUser("user_001")
        Log.d("PHASE1_TEST", "USER TEST: $fetchedUser")

        // 2. Test Contacts
        val contact = ContactEntity(
            id = "contact_001",
            name = "Rahul",
            phone = "8888888888",
            createdBy = "user_001",
            isGhost = true,
            linkedUserId = null
        )
        contactDao.insertContact(contact)
        val contacts = contactDao.getAllContacts().first()
        Log.d("PHASE1_TEST", "CONTACT TEST: Found ${contacts.size} contacts. First: ${contacts.firstOrNull()?.name}")

        // 3. Test Groups
        val group = GroupEntity(
            id = "group_001",
            name = "Trip to Goa",
            createdBy = "user_001",
            createdAt = System.currentTimeMillis(),
            totalExpense = 0.0
        )
        groupDao.insertGroup(group)
        
        val member = GroupMemberEntity(
            groupId = "group_001",
            memberId = "contact_001",
            memberType = "CONTACT",
            joinedAt = System.currentTimeMillis()
        )
        groupDao.insertMember(member)
        val groups = groupDao.getAllGroups().first()
        Log.d("PHASE1_TEST", "GROUP TEST: Found ${groups.size} groups. First: ${groups.firstOrNull()?.name}")

        // 4. Test Expenses
        val expense = ExpenseEntity(
            id = "exp_001",
            title = "Dinner at Britto's",
            groupId = "group_001",
            amount = 1200.0,
            paidBy = "user_001",
            category = "Food",
            timestamp = System.currentTimeMillis(),
            createdBy = "user_001",
            isSynced = false
        )
        expenseDao.insertExpense(expense)

        val split = ExpenseSplitEntity(
            id = "split_001",
            expenseId = "exp_001",
            memberId = "contact_001",
            owedAmount = 600.0,
            status = "PENDING"
        )
        expenseDao.insertSplit(split)

        val expenses = expenseDao.getAllExpenses().first()
        Log.d("PHASE1_TEST", "EXPENSE TEST: Found ${expenses.size} expenses. Last Title: ${expenses.firstOrNull()?.title}")

        Log.d("PHASE1_TEST", "✅ ALL PHASE 1 DATABASE TESTS PASSED")
    }
}

@Composable
fun FlexFiApp() {
    MaterialTheme {
        Surface {
            Text("FlexFi 🚀 Phase 1 Testing...")
        }
    }
}
