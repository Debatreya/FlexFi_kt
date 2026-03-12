package com.example.flexfi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.flexfi.data.local.FlexFiDatabase
import com.example.flexfi.data.local.entities.UserEntity
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.remote.FirestoreGroupService
import com.example.flexfi.data.remote.FirestoreUserService
import com.example.flexfi.data.repository.ContactRepository
import com.example.flexfi.data.repository.GroupRepository
import com.example.flexfi.data.repository.UserRepository
import com.example.flexfi.data.repository.ExpenseRepository
import com.example.flexfi.data.repository.PersonalExpenseRepository
import com.example.flexfi.data.repository.StreakRepository
import com.example.flexfi.data.remote.FirestoreExpenseService
import com.example.flexfi.ui.screens.home.HomeScreen
import com.example.flexfi.ui.screens.home.HomeViewModel
import com.example.flexfi.ui.screens.auth.*
import com.example.flexfi.ui.screens.contacts.*
import com.example.flexfi.ui.screens.groups.*
import com.example.flexfi.ui.screens.expenses.*
import com.example.flexfi.ui.screens.personal.AddPersonalExpenseScreen
import com.example.flexfi.ui.screens.personal.EditPersonalExpenseScreen
import com.example.flexfi.ui.screens.personal.PersonalDashboardScreen
import com.example.flexfi.ui.screens.personal.PersonalExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Manual dependency injection
        val db = Room.databaseBuilder(
            applicationContext,
            FlexFiDatabase::class.java,
            "flexfi_db"
        ).fallbackToDestructiveMigration().build()

        val firestoreService = FirestoreUserService()
        val firestoreGroupService = FirestoreGroupService()
        val firestoreExpenseService = FirestoreExpenseService()
        val authService = FirebaseAuthService()
        val userRepository = UserRepository(db.userDao(), firestoreService)
        val contactRepository = ContactRepository(db.contactDao(), firestoreService)
        val groupRepository = GroupRepository(db.groupDao(), db.contactDao(), firestoreGroupService)
        val personalExpenseRepository = PersonalExpenseRepository(db.personalExpenseDao())
        val streakRepository = StreakRepository(db.streakDao())
        val expenseRepository = ExpenseRepository(
            db.expenseDao(),
            firestoreExpenseService,
            db.personalExpenseDao()
        )

        setContent {
            FlexFiApp(
                authService, 
                firestoreService, 
                userRepository, 
                contactRepository, 
                groupRepository,
                expenseRepository,
                personalExpenseRepository,
                streakRepository,
                onLogoutRequested = {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            // Only clear user and group data on logout
                            // Contacts are kept since only one person uses a device
                            db.userDao().deleteAllUsers()
                            db.groupDao().deleteAllGroups()
                            db.groupDao().deleteAllGroupMembers()
                            db.expenseDao().deleteAllExpenses()
                            db.expenseDao().deleteAllSplits()
                            db.personalExpenseDao().deleteAll()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun FlexFiApp(
    authService: FirebaseAuthService,
    firestoreService: FirestoreUserService,
    userRepository: UserRepository,
    contactRepository: ContactRepository,
    groupRepository: GroupRepository,
    expenseRepository: ExpenseRepository,
    personalExpenseRepository: PersonalExpenseRepository,
    streakRepository: StreakRepository,
    onLogoutRequested: () -> Unit
) {
    val navController = rememberNavController()
    
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(authService, userRepository, groupRepository, expenseRepository, personalExpenseRepository, streakRepository)
    )

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authService, firestoreService, userRepository, groupRepository)
    )
    
    val contactViewModel: ContactViewModel = viewModel(
        factory = ContactViewModelFactory(contactRepository, authService)
    )

    val groupViewModel: GroupViewModel = viewModel(
        factory = GroupViewModelFactory(groupRepository, contactRepository, authService)
    )

    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModelFactory(expenseRepository, groupRepository, streakRepository, authService)
    )

    val personalExpenseViewModel: PersonalExpenseViewModel = viewModel(
        factory = PersonalExpenseViewModelFactory(personalExpenseRepository, streakRepository, authService)
    )

    val startDestination = if (authService.getCurrentUser() != null) "home" else "login"

    MaterialTheme {
        Surface {
            NavHost(navController = navController, startDestination = startDestination) {
                composable("login") {
                    LoginScreen(
                        viewModel = authViewModel,
                        onCodeSent = { navController.navigate("otp") }
                    )
                }
                composable("otp") {
                    OtpScreen(
                        viewModel = authViewModel,
                        onVerified = { state ->
                            if (state is AuthState.NewUser) {
                                navController.navigate("profile_setup")
                            } else {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable("profile_setup") {
                    ProfileSetupScreen(
                        viewModel = authViewModel,
                        onComplete = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }
                composable("home") {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToGroups = { navController.navigate("groups") },
                        onNavigateToContacts = { navController.navigate("contacts") },
                        onNavigateToPersonal = { navController.navigate("personal_dashboard") },
                        onCreateGroupClick = { navController.navigate("create_group") }
                    )
                }
                composable("contacts") {
                    ContactsScreen(
                        viewModel = contactViewModel,
                        onAddContactClick = { navController.navigate("add_contact") }
                    )
                }
                composable("add_contact") {
                    AddContactScreen(
                        viewModel = contactViewModel,
                        onContactAdded = { navController.popBackStack() }
                    )
                }
                composable("groups") {
                    GroupsScreen(
                        viewModel = groupViewModel,
                        onCreateGroupClick = { navController.navigate("create_group") },
                        onGroupClick = { groupId -> navController.navigate("group_detail/$groupId") }
                    )
                }
                composable("create_group") {
                    CreateGroupScreen(
                        groupViewModel = groupViewModel,
                        contactViewModel = contactViewModel,
                        onGroupCreated = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "group_detail/{groupId}",
                    arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                    GroupDetailScreen(
                        groupId = groupId,
                        viewModel = groupViewModel,
                        expenseViewModel = expenseViewModel,
                        onEditClick = { id -> navController.navigate("edit_group/$id") },
                        onDeleteSuccess = { navController.popBackStack() },
                        onAddExpenseClick = { id -> navController.navigate("add_expense/$id") }
                    )
                }
                composable(
                    route = "edit_group/{groupId}",
                    arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                    EditGroupScreen(
                        groupId = groupId,
                        groupViewModel = groupViewModel,
                        contactViewModel = contactViewModel,
                        onGroupUpdated = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "add_expense/{groupId}",
                    arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                    AddExpenseScreen(
                        groupId = groupId,
                        expenseViewModel = expenseViewModel,
                        groupViewModel = groupViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("personal_dashboard") {
                    PersonalDashboardScreen(
                        viewModel = personalExpenseViewModel,
                        onBack = { navController.popBackStack() },
                        onAddExpenseClick = { navController.navigate("add_personal_expense") },
                        onExpenseClick = { expense ->
                            navController.navigate("edit_personal_expense/${expense.id}")
                        }
                    )
                }
                composable("add_personal_expense") {
                    AddPersonalExpenseScreen(
                        viewModel = personalExpenseViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "edit_personal_expense/{expenseId}",
                    arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable
                    val expense = personalExpenseViewModel.findExpenseById(expenseId) ?: return@composable
                    EditPersonalExpenseScreen(
                        expense = expense,
                        viewModel = personalExpenseViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}



// Factories for ViewModels
class HomeViewModelFactory(
    private val authService: FirebaseAuthService,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val streakRepository: StreakRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(authService, userRepository, groupRepository, expenseRepository, personalExpenseRepository, streakRepository) as T
    }
}

class AuthViewModelFactory(
    private val authService: FirebaseAuthService,
    private val firestoreService: FirestoreUserService,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(authService, firestoreService, userRepository, groupRepository) as T
    }
}

class ContactViewModelFactory(
    private val contactRepository: ContactRepository,
    private val authService: FirebaseAuthService
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ContactViewModel(contactRepository, authService) as T
    }
}

class GroupViewModelFactory(
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository,
    private val authService: FirebaseAuthService
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return GroupViewModel(groupRepository, contactRepository, authService) as T
    }
}

class ExpenseViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val streakRepository: StreakRepository,
    private val authService: FirebaseAuthService
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ExpenseViewModel(expenseRepository, groupRepository, streakRepository, authService) as T
    }
}

class PersonalExpenseViewModelFactory(
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val streakRepository: StreakRepository,
    private val authService: FirebaseAuthService
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return PersonalExpenseViewModel(personalExpenseRepository, streakRepository, authService) as T
    }
}
