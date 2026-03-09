# 🚀 FlexFi Development Progress (Phase 5 Complete)

**Project:** FlexFi – Social Expense Tracking Platform
**Vision:** *The “Strava for Personal Finance”*

FlexFi combines **expense tracking, group splitting, and social financial insights** into a single mobile application. Built with an **offline-first philosophy**, the app remains fully functional without internet while syncing with Firestore.

---

# 📑 Phase Summaries (0 — 4)

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

---

# Phase 5 — Expense Splitting Engine (CORE) ⚡

Goal: Implement the math and UI for adding expenses and calculating actionable debts.

### 1. Data Layer Refactoring
Refactored the schema to align with MVP requirements:
- **`ExpenseEntity`**: Now tracks `paidByPhone`, `groupId`, and `createdAt` with precision.
- **`ExpenseSplitEntity`**: Tracks individual member shares (`shareAmount`) linked by phone.
- **Firestore `ExpenseDoc`**: Uses a unified document structure with embedded splits for optimized cloud performance.

### 2. The Debt Engine (Business Logic)
Implemented the core financial algorithms in `ExpenseRepository`:
- **Split Logic**:
    - **Equal Split**: Automatically divides the total among participants.
    - **Rounding Precision**: Implemented a "remainder to last member" logic (e.g., ₹100 / 3 = 33.33, 33.33, 33.34) to prevent balance drift.
    - **Exact split**: Allows manual entry with mathematical validation (sum of splits must equal total).
- **Settlement Algorithm**: A greedy matching engine that converts net balances into minimal transactions (e.g., "Rahul owes You ₹300").

### 3. Expense Management UI
- **`AddExpenseScreen`**: A full-featured form with a payer dropdown, split-type toggle, and multi-member selector.
- **`ExpenseListItem`**: Visual timeline of group costs showing title, date, payer, and amount.
- **Real-time Sync**: Expenses added while offline are queued and synced to Firestore; remote expenses are pulled and cached automatically.

### 4. Group Balance Dashboard
Modified `GroupDetailScreen` to serve as a financial command center:
- **Balance Summary**: Real-time view of who "gets back" vs "owes" money, color-coded for clarity.
- **Actionable Settlements**: Lists exactly who needs to pay whom to clear all debts.
- **Expense History**: A searchable list of all transactional activity within the group.

### Outcome
FlexFi is now a **fully functional Splitwise competitor**. It handles the complexity of shared expenses, mathematical precision, and offline reliability.

---

### Folder Structure (after Phase 5)

```
PS C:\Users\debat\OneDrive\Desktop\Devs\FlexFi\app\src\main\java\com\example\flexfi> tree /F
Folder PATH listing for volume OS
Volume serial number is 8C4D-41E5
C:.
│   MainActivity.kt
│   
├───data
│   ├───local
│   │   │   FlexFiDatabase.kt
│   │   │
│   │   ├───dao
│   │   │       ContactDao.kt
│   │   │       ExpenseDao.kt
│   │   │       GroupDao.kt
│   │   │       UserDao.kt
│   │   │
│   │   └───entities
│   │           .gitkeep
│   │           CategoryEntity.kt
│   │           ContactEntity.kt
│   │           ExpenseEntity.kt
│   │           ExpenseSplitEntity.kt
│   │           GroupEntity.kt
│   │           GroupMemberEntity.kt
│   │           StreakEntity.kt
│   │           UserEntity.kt
│   │
│   ├───remote
│   │   │   .gitkeep
│   │   │   FirebaseAuthService.kt
│   │   │   FirestoreExpenseService.kt
│   │   │   FirestoreGroupService.kt
│   │   │   FirestoreUserService.kt
│   │   │
│   │   └───firestoreModels
│   │           .gitkeep
│   │           ExpenseDoc.kt
│   │           GroupDoc.kt
│   │           UserDoc.kt
│   │
│   └───repository
│           .gitkeep
│           ContactRepository.kt
│           ExpenseRepository.kt
│           GroupRepository.kt
│           UserRepository.kt
│
├───domain
│   ├───models
│   │       .gitkeep
│   │       Contact.kt
│   │       Expense.kt
│   │       ExpenseSplit.kt
│   │       Group.kt
│   │       User.kt
│   │
│   └───usecases
│           .gitkeep
│
├───ui
│   ├───components
│   │       .gitkeep
│   │
│   ├───screens
│   │   │   .gitkeep
│   │   │
│   │   ├───auth
│   │   │       AuthViewModel.kt
│   │   │       LoginScreen.kt
│   │   │       OtpScreen.kt
│   │   │       ProfileSetupScreen.kt
│   │   │
│   │   ├───contacts
│   │   │       AddContactScreen.kt
│   │   │       ContactsScreen.kt
│   │   │       ContactViewModel.kt
│   │   │
│   │   ├───expenses
│   │   │       AddExpenseScreen.kt
│   │   │       ExpenseListItem.kt
│   │   │       ExpenseViewModel.kt
│   │   │
│   │   └───groups
│   │           CreateGroupScreen.kt
│   │           EditGroupScreen.kt
│   │           GroupDetailScreen.kt
│   │           GroupsScreen.kt
│   │           GroupViewModel.kt
│   │
│   └───theme
│           Color.kt
│           Theme.kt
│           Type.kt
│
└───utils
        .gitkeep
```
