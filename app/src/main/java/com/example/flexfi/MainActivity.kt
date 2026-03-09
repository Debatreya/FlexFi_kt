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
import com.example.flexfi.data.remote.FirestoreExpenseService
import com.example.flexfi.ui.screens.auth.*
import com.example.flexfi.ui.screens.contacts.*
import com.example.flexfi.ui.screens.groups.*
import com.example.flexfi.ui.screens.expenses.*
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
    onLogoutRequested: () -> Unit
) {
    val navController = rememberNavController()
    
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
        factory = ExpenseViewModelFactory(expenseRepository, groupRepository)
    )

    val personalExpenseViewModel: PersonalExpenseViewModel = viewModel(
        factory = PersonalExpenseViewModelFactory(personalExpenseRepository, authService)
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
                        authService = authService,
                        onLogout = {
                            onLogoutRequested()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onNavigateToContacts = { navController.navigate("contacts") },
                        onNavigateToGroups = { navController.navigate("groups") },
                        onNavigateToPersonal = { navController.navigate("personal_dashboard") },
                        userRepository = userRepository
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
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authService: FirebaseAuthService, 
    onLogout: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToPersonal: () -> Unit,
    userRepository: UserRepository
) {
    val currentUser by userRepository.getCurrentUserFlow().collectAsState(initial = null)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Welcome, ${currentUser?.name ?: "FlexFi User"} 🚀") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onNavigateToGroups,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Groups, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("My Groups")
            }

            Button(
                onClick = onNavigateToContacts,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Manage Contacts")
            }

            Button(
                onClick = onNavigateToPersonal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("My Spending")
            }

            Button(
                onClick = {
                    authService.signOut()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }
        }
    }
}

// Factories for ViewModels
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
    private val groupRepository: GroupRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ExpenseViewModel(expenseRepository, groupRepository) as T
    }
}

class PersonalExpenseViewModelFactory(
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val authService: FirebaseAuthService
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return PersonalExpenseViewModel(personalExpenseRepository, authService) as T
    }
}
