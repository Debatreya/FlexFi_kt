# 🚀 FlexFi Development Progress (Phase 7 Complete)

**Project:** FlexFi – Social Expense Tracking Platform
**Vision:** *The “Strava for Personal Finance”*

FlexFi combines **expense tracking, group splitting, and social financial insights** into a single mobile application. Built with an **offline-first philosophy**, the app remains fully functional without internet while syncing with Firestore.

---

# 📑 Phase Summaries (0 — 6) Brief

### Phase 0 — Architecture Setup
Established the foundation with **Kotlin, Jetpack Compose, Room, and Firebase**. Implemented the Repository pattern and base navigation.

### Phase 1 — Local Data Engine
Defined the **offline-first schema**. Created Room entities and DAOs for Users, Contacts, Groups, and Expenses, enabling reactive data flows using Kotlin Flow.

### Phase 2 — Authentication & Cloud Sync
Integrated **Firebase Phone Auth (OTP)**. Implemented user profile creation and the sync strategy to mirror Firestore data into the local Room cache upon login.

### Phase 3 — Ghost Contacts System
Solved friction by allowing users to split expenses with **"Ghost Contacts"** (non-app users). Implemented ghost detection, registered user auto-linking, and contact management.

### Phase 4 — Group System
Built **collaborative group management**. Implemented Firestore-backed groups, admin controls (edit/delete), member name resolution via contacts, and multi-device sync.

### Phase 5 — Expense Splitting Engine (CORE)
Implemented the math and UI for adding expenses and calculating actionable debts. Refactored the core data layers, established rounding precisions, and a greedy net-settlement matching algorithm ("who owes whom"). Built the full Group Balance Dashboard UI.

### Phase 6 — Personal Expense Tracking
Added **Manual Private Expenses** ensuring offline-first CRUD. Users can now categorize personal items, view isolated charts/stats, and mirror their split group-shares to provide a unified `Total Spent` value reflecting true aggregate outflow.

---

# 🌟 Phase 7 — Smart Categorization + Gamification (Current)

**Goal:** Transform the raw tool into a frictionless and engaging "smart habit" app.

### 1. Smart Expense Categorization
- Built `ExpenseCategorizer.kt`, a responsive utility capable of sniffing expense keywords in real-time.
- Whenever a user enters known terms (e.g., "Uber", "Zomato", "Flight", "Netflix"), the system immediately auto-matches it to categories like `Transport`, `Food`, `Travel`, or `Subscriptions`.
- Integrated seamlessly into both `AddExpenseScreen` and `AddPersonalExpenseScreen`'s title fields.

### 2. Strava-style Streak Tracking
- Implemented `StreakEntity`, `StreakDao`, and `StreakRepository` to persist user-logging velocity locally.
- Designed dynamic time-bounds: logging consecutively daily loops a streak, missing a day rests it, logging multiple times daily holds ground.
- Intercepted expense saves in `PersonalExpenseViewModel` and `ExpenseViewModel` to update the Streak metric efficiently on completion.

### 3. Dynamic Gamified Dashboard UI
- Pulled the `HomeScreen` into its own designated package with an exclusive `HomeViewModel` aggregator.
- Orchestrated the grand dashboard elements:
  - **Main Balance Gradient Card**: Displays large sweeping figures evaluating the aggregate network values (Net Balance, You Owe, Owed to You).
  - **Habit Banner**: Displays context-aware 🔥 gamified milestones (e.g., Bronze Tracker, Finance Athlete) depending on current streak velocity.
  - **Active Groups Horizontal Carousel**: Direct launchpads summarizing current active projects.
  - **Dual-Sourced Recent Feed**: Mixed pipeline timeline uniting personal entries alongside group shares cleanly on a unified vertical board.

### Outcome
FlexFi isn’t just tracking balances—it helps shape habit cycles via immediate dopamine cues and minimal-friction categorized inputs.

---

### Folder Structure (after Phase 7)

```
C:\USERS\DEBAT\ONEDRIVE\DESKTOP\DEVS\FLEXFI\APP\SRC\MAIN\JAVA\COM\EXAMPLE\FLEXFI
|   MainActivity.kt
|   
+---data
|   +---local
|   |   |   FlexFiDatabase.kt
|   |   |   
|   |   +---dao
|   |   |       ContactDao.kt
|   |   |       ExpenseDao.kt
|   |   |       GroupDao.kt
|   |   |       PersonalExpenseDao.kt
|   |   |       StreakDao.kt
|   |   |       UserDao.kt
|   |   |       
|   |   \---entities
|   |           .gitkeep
|   |           CategoryEntity.kt
|   |           ContactEntity.kt
|   |           ExpenseEntity.kt
|   |           ExpenseSplitEntity.kt
|   |           GroupEntity.kt
|   |           GroupMemberEntity.kt
|   |           PersonalExpenseEntity.kt
|   |           StreakEntity.kt
|   |           UserEntity.kt
|   |           
|   +---remote
|   |   |   .gitkeep
|   |   |   FirebaseAuthService.kt
|   |   |   FirestoreExpenseService.kt
|   |   |   FirestoreGroupService.kt
|   |   |   FirestoreUserService.kt
|   |   |   
|   |   \---firestoreModels
|   |           .gitkeep
|   |           ExpenseDoc.kt
|   |           GroupDoc.kt
|   |           UserDoc.kt
|   |           
|   \---repository
|           .gitkeep
|           ContactRepository.kt
|           ExpenseRepository.kt
|           GroupRepository.kt
|           PersonalExpenseRepository.kt
|           StreakRepository.kt
|           UserRepository.kt
|           
+---domain
|   +---models
|   |       .gitkeep
|   |       Contact.kt
|   |       Expense.kt
|   |       ExpenseSplit.kt
|   |       Group.kt
|   |       PersonalExpense.kt
|   |       User.kt
|   |       
|   \---usecases
|           .gitkeep
|           
+---ui
|   +---components
|   |       .gitkeep
|   |       
|   +---screens
|   |   |   .gitkeep
|   |   |   
|   |   +---auth
|   |   |       AuthViewModel.kt
|   |   |       LoginScreen.kt
|   |   |       OtpScreen.kt
|   |   |       ProfileSetupScreen.kt
|   |   |       
|   |   +---contacts
|   |   |       AddContactScreen.kt
|   |   |       ContactsScreen.kt
|   |   |       ContactViewModel.kt
|   |   |       
|   |   +---expenses
|   |   |       AddExpenseScreen.kt
|   |   |       ExpenseListItem.kt
|   |   |       ExpenseViewModel.kt
|   |   |       
|   |   +---groups
|   |   |       CreateGroupScreen.kt
|   |   |       EditGroupScreen.kt
|   |   |       GroupDetailScreen.kt
|   |   |       GroupsScreen.kt
|   |   |       GroupViewModel.kt
|   |   |       
|   |   +---home
|   |   |       HomeScreen.kt
|   |   |       HomeViewModel.kt
|   |   |       
|   |   \---personal
|   |           AddPersonalExpenseScreen.kt
|   |           EditPersonalExpenseScreen.kt
|   |           PersonalDashboardScreen.kt
|   |           PersonalExpenseViewModel.kt
|   |           
|   \---theme
|           Color.kt
|           Theme.kt
|           Type.kt
|           
\---utils
        .gitkeep
        ExpenseCategorizer.kt
```
