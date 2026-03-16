package com.example.flexfi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.flexfi.data.local.FlexFiDatabase
import com.example.flexfi.data.local.entities.AppSettingsEntity
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.remote.ExchangeRateApi
import com.example.flexfi.data.remote.FirestoreGroupService
import com.example.flexfi.data.remote.FirestoreUserService
import com.example.flexfi.data.remote.FirestoreExpenseService
import com.example.flexfi.data.repository.*
import com.example.flexfi.ui.screens.auth.*
import com.example.flexfi.ui.screens.contacts.*
import com.example.flexfi.ui.screens.expenses.*
import com.example.flexfi.ui.screens.goals.*
import com.example.flexfi.ui.screens.groups.*
import com.example.flexfi.ui.screens.home.*
import com.example.flexfi.ui.screens.personal.*
import com.example.flexfi.ui.screens.profile.*
import com.example.flexfi.ui.screens.settle.*
import com.example.flexfi.ui.screens.budget.*
import com.example.flexfi.ui.screens.splash.SplashScreen
import com.example.flexfi.ui.theme.FlexFiTheme
import com.example.flexfi.utils.CurrencyProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize CurrencyProvider
        CurrencyProvider.init(applicationContext)

        // Manual dependency injection
        val db = Room.databaseBuilder(
            applicationContext,
            FlexFiDatabase::class.java,
            "flexfi_db"
        ).fallbackToDestructiveMigration().build()

        val firestoreService = FirestoreUserService()
        val firestoreGroupService = FirestoreGroupService()
        val firestoreExpenseService = FirestoreExpenseService()
        val exchangeRateApi = ExchangeRateApi()
        val authService = FirebaseAuthService()
        val userRepository = UserRepository(db.userDao(), firestoreService)
        val contactRepository = ContactRepository(db.contactDao(), firestoreService)
        val groupRepository = GroupRepository(db.groupDao(), db.contactDao(), firestoreGroupService)
        val personalExpenseRepository = PersonalExpenseRepository(db.personalExpenseDao(), exchangeRateApi)
        val streakRepository = StreakRepository(db.streakDao())
        val appSettingsRepository = AppSettingsRepository(db.appSettingsDao())
        val recurringTransactionRepository = RecurringTransactionRepository(db.recurringTransactionDao())
        val expenseRepository = ExpenseRepository(
            db.expenseDao(),
            firestoreExpenseService,
            db.personalExpenseDao(),
            exchangeRateApi,
            db.settlementDao()
        )
        val budgetGoalRepository = BudgetGoalRepository(db.budgetGoalDao())
        val budgetRepository = BudgetRepository(db.budgetDao())

        setContent {
            val settings by appSettingsRepository.getSettings().collectAsState(initial = AppSettingsEntity())

            LaunchedEffect(settings.displayCurrency) {
                CurrencyProvider.setDisplayCurrency(settings.displayCurrency)
                val rate = exchangeRateApi.getRate("USD", settings.displayCurrency, System.currentTimeMillis())
                CurrencyProvider.setDisplayRateFromBase(rate)
            }

            FlexFiTheme(darkTheme = settings.isDarkMode) {
                FlexFiApp(
                    authService = authService,
                    exchangeRateApi = exchangeRateApi,
                    firestoreService = firestoreService,
                    userRepository = userRepository,
                    contactRepository = contactRepository,
                    groupRepository = groupRepository,
                    expenseRepository = expenseRepository,
                    personalExpenseRepository = personalExpenseRepository,
                    streakRepository = streakRepository,
                    budgetGoalRepository = budgetGoalRepository,
                    appSettingsRepository = appSettingsRepository,
                    recurringTransactionRepository = recurringTransactionRepository,
                    budgetRepository = budgetRepository,
                    onLogoutRequested = {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                db.userDao().deleteAllUsers()
                                db.groupDao().deleteAllGroups()
                                db.groupDao().deleteAllGroupMembers()
                                db.expenseDao().deleteAllExpenses()
                                db.expenseDao().deleteAllSplits()
                                db.personalExpenseDao().deleteAll()
                                db.appSettingsDao().deleteAll()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FlexFiApp(
    authService: FirebaseAuthService,
    exchangeRateApi: ExchangeRateApi,
    firestoreService: FirestoreUserService,
    userRepository: UserRepository,
    contactRepository: ContactRepository,
    groupRepository: GroupRepository,
    expenseRepository: ExpenseRepository,
    personalExpenseRepository: PersonalExpenseRepository,
    streakRepository: StreakRepository,
    budgetGoalRepository: BudgetGoalRepository,
    appSettingsRepository: AppSettingsRepository,
    recurringTransactionRepository: RecurringTransactionRepository,
    budgetRepository: BudgetRepository,
    onLogoutRequested: () -> Unit
) {
    val navController = rememberNavController()

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(authService, userRepository, groupRepository, expenseRepository, personalExpenseRepository, streakRepository, appSettingsRepository)
    )

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authService, firestoreService, userRepository, groupRepository)
    )

    val analyticsViewModel: AnalyticsViewModel = viewModel(
        factory = AnalyticsViewModelFactory(personalExpenseRepository, authService)
    )

    val contactViewModel: ContactViewModel = viewModel(
        factory = ContactViewModelFactory(contactRepository, authService)
    )

    val groupViewModel: GroupViewModel = viewModel(
        factory = GroupViewModelFactory(groupRepository, contactRepository, expenseRepository, authService)
    )

    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModelFactory(
            expenseRepository,
            groupRepository,
            streakRepository,
            authService,
            appSettingsRepository
        )
    )

    val personalExpenseViewModel: PersonalExpenseViewModel = viewModel(
        factory = PersonalExpenseViewModelFactory(personalExpenseRepository, budgetRepository, streakRepository, authService, exchangeRateApi)
    )

    val settleUpViewModel: SettleUpViewModel = viewModel(
        factory = SettleUpViewModelFactory(
            expenseRepository,
            groupRepository,
            contactRepository,
            authService,
            exchangeRateApi,
            appSettingsRepository
        )
    )

    val budgetGoalViewModel: BudgetGoalViewModel = viewModel(
        factory = BudgetGoalViewModelFactory(budgetGoalRepository, authService)
    )

    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(
            authService,
            appSettingsRepository,
            recurringTransactionRepository,
            personalExpenseRepository,
            exchangeRateApi
        )
    )

    val budgetViewModel: BudgetViewModel = viewModel(
        factory = BudgetViewModelFactory(
            budgetRepository,
            personalExpenseRepository,
            authService
        )
    )

    val startDestination = "splash"

    Surface {
        NavHost(navController = navController, startDestination = startDestination) {
            composable("splash") {
                SplashScreen(
                    onNavigate = {
                        val dest = if (authService.getCurrentUser() != null) "home" else "login"
                        navController.navigate(dest) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }
            composable("login") {
                LoginScreen(
                    viewModel = authViewModel,
                    onOtpSent = { navController.navigate("otp") }
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
                    },
                    onBack = { navController.popBackStack() }
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
                    onGroupClick = { groupId -> navController.navigate("group_detail/$groupId") },
                    onContactsClick = { navController.navigate("contacts") },
                    onGroupsClick = { navController.navigate("groups") },
                    onProfileClick = { navController.navigate("profile") },
                    onPersonalExpensesClick = { navController.navigate("personal_dashboard") },
                    onSettleUpClick = { navController.navigate("settle_up") },
                    onBudgetGoalsClick = { navController.navigate("budget_goals") },
                    onAddExpenseClick = { navController.navigate("groups") }
                )
            }
            composable("contacts") {
                val contacts by contactViewModel.contacts.collectAsState()
                ContactsScreen(
                    contacts = contacts,
                    onDeleteContact = { contactViewModel.deleteContact(it) },
                    onAddContactClick = { navController.navigate("add_contact") },
                    onBackClick = { navController.popBackStack() },
                    onContactsTab = {},
                    onGroupsTab = { navController.navigate("groups") },
                    onHomeTab = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                    onExpensesTab = { navController.navigate("personal_dashboard") },
                    onProfileTab = { navController.navigate("profile") }
                )
            }
            composable("add_contact") {
                AddContactScreen(
                    viewModel = contactViewModel,
                    onContactAdded = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("groups") {
                GroupsScreen(
                    viewModel = groupViewModel,
                    onCreateGroupClick = { navController.navigate("create_group") },
                    onGroupClick = { groupId -> navController.navigate("group_detail/$groupId") },
                    onHomeTab = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                    onContactsTab = { navController.navigate("contacts") },
                    onExpensesTab = { navController.navigate("personal_dashboard") },
                    onProfileTab = { navController.navigate("profile") }
                )
            }
            composable("create_group") {
                CreateGroupScreen(
                    groupViewModel = groupViewModel,
                    contactViewModel = contactViewModel,
                    onGroupCreated = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
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
                    onAddExpenseClick = { id -> navController.navigate("add_expense/$id") },
                    onExpenseClick = { id -> navController.navigate("expense_detail/$id") }
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
                    onGroupUpdated = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
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
            composable(
                route = "expense_detail/{expenseId}",
                arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable
                ExpenseDetailScreen(
                    expenseId = expenseId,
                    viewModel = expenseViewModel,
                    groupViewModel = groupViewModel,
                    onBack = { navController.popBackStack() },
                    onDeleteSuccess = { navController.popBackStack() }
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
            composable("profile") {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenExpenses = { navController.navigate("personal_dashboard") },
                    onOpenAnalytics = { navController.navigate("analytics") }
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
            composable("analytics") {
                AnalyticsScreen(
                    viewModel = analyticsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            // ── New Routes: Settle Up & Budget Goals ──
            composable("settle_up") {
                SettleUpScreen(
                    viewModel = settleUpViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("budget_goals") {
                BudgetGoalsScreen(
                    viewModel = budgetGoalViewModel,
                    onAddGoalClick = { navController.navigate("add_goal") },
                    onEditGoalClick = { goalId -> navController.navigate("edit_goal/$goalId") },
                    onGoalClick = { goalId -> navController.navigate("goal_detail/$goalId") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("add_goal") {
                AddEditGoalScreen(
                    viewModel = budgetGoalViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_goal/{goalId}",
                arguments = listOf(navArgument("goalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getString("goalId") ?: return@composable
                AddEditGoalScreen(
                    viewModel = budgetGoalViewModel,
                    goalId = goalId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "goal_detail/{goalId}",
                arguments = listOf(navArgument("goalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getString("goalId") ?: return@composable
                GoalDetailScreen(
                    goalId = goalId,
                    viewModel = budgetGoalViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ── ViewModelFactories ──

class HomeViewModelFactory(
    private val authService: FirebaseAuthService,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val streakRepository: StreakRepository,
    private val appSettingsRepository: AppSettingsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(
            authService,
            userRepository,
            groupRepository,
            expenseRepository,
            personalExpenseRepository,
            streakRepository,
            appSettingsRepository
        ) as T
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

class AnalyticsViewModelFactory(
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val authService: FirebaseAuthService
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return AnalyticsViewModel(personalExpenseRepository, authService) as T
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
    private val expenseRepository: ExpenseRepository,
    private val authService: FirebaseAuthService
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return GroupViewModel(groupRepository, contactRepository, expenseRepository, authService) as T
    }
}

class ExpenseViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val streakRepository: StreakRepository,
    private val authService: FirebaseAuthService,
    private val appSettingsRepository: AppSettingsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ExpenseViewModel(
            expenseRepository,
            groupRepository,
            streakRepository,
            authService,
            appSettingsRepository
        ) as T
    }
}

class PersonalExpenseViewModelFactory(
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val streakRepository: StreakRepository,
    private val authService: FirebaseAuthService,
    private val exchangeRateApi: ExchangeRateApi
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return PersonalExpenseViewModel(personalExpenseRepository, budgetRepository, streakRepository, authService, exchangeRateApi) as T
    }
}

class SettleUpViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository,
    private val authService: FirebaseAuthService,
    private val exchangeRateApi: ExchangeRateApi,
    private val appSettingsRepository: AppSettingsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return SettleUpViewModel(
            expenseRepository,
            groupRepository,
            contactRepository,
            authService,
            exchangeRateApi,
            appSettingsRepository
        ) as T
    }
}

class BudgetGoalViewModelFactory(
    private val budgetGoalRepository: BudgetGoalRepository,
    private val authService: FirebaseAuthService
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return BudgetGoalViewModel(budgetGoalRepository, authService) as T
    }
}

class ProfileViewModelFactory(
    private val authService: FirebaseAuthService,
    private val appSettingsRepository: AppSettingsRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val exchangeRateApi: ExchangeRateApi
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(
            authService = authService,
            settingsRepository = appSettingsRepository,
            recurringRepository = recurringTransactionRepository,
            personalExpenseRepository = personalExpenseRepository,
            exchangeRateApi = exchangeRateApi
        ) as T
    }
}

class BudgetViewModelFactory(
    private val budgetRepository: BudgetRepository,
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val authService: FirebaseAuthService
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return BudgetViewModel(
            budgetRepository = budgetRepository,
            personalExpenseRepository = personalExpenseRepository,
            authService = authService
        ) as T
    }
}
