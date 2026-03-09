Below is a **clean development log** you can keep in your repo (README / progress doc).
Earlier phases summarized, **Phase 4 explained in more detail** as the latest completed phase.

---

# 🚀 FlexFi Development Progress (till Phase 4)

**Project:** FlexFi – Social Expense Tracking Platform
**Vision:** *The “Strava for Personal Finance”*

FlexFi aims to combine **expense tracking, group splitting, and social financial insights** into a single mobile application. The architecture is built with an **offline-first philosophy**, meaning the app remains fully functional even without internet connectivity while syncing with the cloud when available.

---

# Phase 0 — Architecture Setup

Goal: Establish a **scalable Android architecture** before implementing features.

### Technologies Integrated

* **Kotlin**
* **Jetpack Compose** (UI)
* **Room Database** (local persistence)
* **Firebase Authentication**
* **Firebase Firestore**
* **Repository Pattern**

### Architecture Layers

```
UI (Compose Screens)
↓
ViewModel
↓
Repository
↓
Local DB (Room)
↓
Remote DB (Firestore)
```

### Outcome

* Modular project structure created
* Dependency setup completed
* Base navigation implemented
* Project successfully builds and runs

FlexFi now had a **stable development foundation**.

---

# Phase 1 — Local Data Engine

Goal: Implement the **offline-first database layer**.

### Room Entities Created

```
UserEntity
ContactEntity
GroupEntity
GroupMemberEntity
ExpenseEntity
ExpenseSplitEntity
CategoryEntity
StreakEntity
```

### DAO Layer

DAO interfaces implemented for:

```
UserDao
ContactDao
GroupDao
ExpenseDao
```

These provide:

* CRUD operations
* reactive queries using **Flow**

### Repository Layer

Repositories abstracted database logic from UI.

Example:

```
UserRepository
GroupRepository
ExpenseRepository
```

### Outcome

FlexFi gained:

* Persistent local storage
* reactive data flow
* clean separation between UI and data

The app now supports **offline-first data handling**, which is essential for mobile expense tracking.

---

# Phase 2 — Authentication & Cloud Sync

Goal: Introduce **user identity and cloud storage**.

### Firebase Phone Authentication

Users authenticate using:

```
Phone Number
↓
OTP Verification
```

### Profile Setup

After verification, users create their profile:

```
Name
Optional Email
```

### Firestore User Schema

```
users
   {firebase_uid}
       id
       name
       phone
       email
       joinedAt
       streakCount
       totalExpense
```

### Sync Strategy

After login:

```
Firebase Auth
↓
Fetch/Create Firestore user
↓
Sync user to Room DB
```

This provides:

* **offline access**
* **fast UI performance**
* **cloud backup**

### Navigation Flow

```
LoginScreen
↓
OtpScreen
↓
ProfileSetupScreen
↓
HomeScreen
```

### Outcome

FlexFi became a **cloud-backed mobile app** with secure authentication.

Capabilities achieved:

* phone-based identity
* cloud user storage
* local caching of user data
* authenticated navigation flow

---

# Phase 3 — Ghost Contacts System

Goal: Allow users to track expenses with people **who have not installed FlexFi**.

This solves the **biggest friction in traditional splitting apps** where all members must install the app.

### What Was Built

* **Add Contact Screen** — users provide a name and phone number
* **Ghost Detection** — Firestore is queried to determine if the phone belongs to a registered user
* **Contact List Screen** — displays contacts with 👻 (ghost) or ✔ (registered) indicators
* **Ghost → Real Activation** — when a ghost contact later installs FlexFi, their status auto-updates
* **Phone Validation** — phone numbers must start with country code (`+`), duplicates update the existing name
* **Delete Contact** — contacts can be removed with a confirmation dialog
* **Offline-First** — all contacts are stored in Room DB; cloud lookup only during creation

### Contact Data Model

```
ContactEntity
- id, name, phone, createdBy, isGhost, linkedUserId, createdAt
```

### Outcome

FlexFi gained a **personal contact management system** with ghost profile support, forming the social graph layer needed for group expense splitting.

---

# Phase 4 — Group System

Goal: Implement **group management** with Firestore sync, admin controls, and integration with the contacts system.

This phase transforms FlexFi from a contact tracker into a **collaborative expense platform** where users organize shared financial activity.

---

## Group Creation

Users can create groups by providing:

```
Group Name
Selected Members (from local contacts)
```

Key behaviors:

* The **creator is automatically added** as a member
* The creator receives the **Admin role**
* Groups can be created with **only the creator** (no members required)
* Members are stored as **phone numbers** (not names), ensuring consistency across devices

---

## Group Data Model

### Local (Room DB)

```
GroupEntity
- id, name, createdByPhone, adminPhone, createdAt, totalExpense
```

```
GroupMemberEntity
- groupId, phone, joinedAt
```

### Remote (Firestore)

```
GroupDoc
- id, name, createdByPhone, adminPhone, createdAt, memberPhones[]
```

Firestore is the **source of truth**. Room DB is the **local cache** for offline use and fast UI rendering.

---

## Contact System Integration

Groups store only **phone numbers**. Member names are resolved using a SQL JOIN with the local contacts table:

```
if phone exists in contacts → show contact name
else → show phone number
```

Example group display:

```
Trip to Goa

Rahul
+918889991122
Aman 👻
```

Ghost members are displayed with a **ghost badge** based on the contact's `isGhost` flag.

---

## Member Display Logic

When a group is opened, the following tags are applied:

| Condition              | Display         |
| ---------------------- | --------------- |
| Member is current user | **Me**          |
| Member is admin        | **Admin** badge |
| Member is ghost        | **👻 Ghost**   |
| Member is registered   | **✔** icon      |
| Member not in contacts | Phone number    |

Example:

```
Rahul (Admin)
Me
Aman 👻
+918889991122
```

---

## Admin Controls

Only the **Admin (creator)** can:

* Edit the group name
* Add or remove members
* Delete the group

Non-admin members have **read-only access**. Admin is identified by comparing:

```
adminPhone == loggedInUserPhone
```

Edit and delete buttons are only visible to the admin.

---

## Group Editing

The Edit Group screen:

* Pre-fills the current group name
* Pre-selects current members from the contact list
* Allows adding/removing members
* Updates both **Room DB and Firestore** on save

---

## Group Deletion

When an admin deletes a group:

* The group is removed from **Room DB**
* All group members are cleared locally
* The group document is deleted from **Firestore**
* A confirmation dialog prevents accidental deletions

---

## Group Sync on Login

When a user logs in:

```
Firebase Auth
↓
Fetch user profile from Firestore
↓
Sync groups where memberPhones contains userPhone
↓
Cache groups locally in Room DB
```

This ensures:

* **Multi-device sync** — groups appear on any device the user logs into
* **Stale data cleanup** — old local groups are cleared and refreshed from Firestore
* **Immediate UI update** — groups appear right after login without app restart

---

## Reactive UI Updates

ViewModels use **dynamic phone resolution** to ensure the UI always reflects the currently logged-in user:

```
val currentUserPhone: String
    get() = authService.getCurrentUser()?.phoneNumber ?: ""
```

Screens use `LaunchedEffect` to trigger data refresh every time they are opened:

```
LaunchedEffect(Unit) {
    viewModel.refreshGroups()
}
```

This prevents stale data when navigating between screens or after login/logout.

---

## Logout Behavior

When a user logs out:

```
User table → cleared
Group cache → cleared
Group members cache → cleared
Contacts → preserved (single device, single user assumption)
```

Firebase Auth is signed out. The user is navigated back to the login screen.

---

## Offline Behavior

Because FlexFi is **offline-first**:

* Groups are cached locally in Room DB
* Users can open and view groups offline
* New groups are pushed to Firestore when connectivity is available
* Group sync occurs on login when internet is available

---

## Screens Implemented

| Screen             | Purpose                                |
| ------------------ | -------------------------------------- |
| GroupsScreen        | Lists all groups the user belongs to   |
| CreateGroupScreen   | Create a new group with contact picker |
| GroupDetailScreen   | View group members with tags           |
| EditGroupScreen     | Edit group name and members (admin)    |

---

## Firestore Services Added

```
FirestoreGroupService
- createGroup(group)
- getGroupsForPhone(phone)
- deleteGroup(groupId)
```

---

# Current Application Capabilities (after Phase 4)

FlexFi now supports:

```
User authentication (phone + OTP)
User profiles (Firestore + Room)
Local database persistence (Room, offline-first)
Cloud user storage (Firestore)
Ghost contact creation and detection
Existing user detection via Firestore
Contact list management (add, delete, duplicate handling)
Phone validation on contact creation
Ghost → registered auto-linking
Group creation with contact picker
Group Firestore sync (source of truth)
Group member name resolution via contacts
Admin-only group editing and deletion
"Me" tag for logged-in user
Ghost and Admin badges in group view
Multi-device group sync on login
Reactive UI with immediate data refresh
Selective logout (contacts preserved)
```

At this stage, FlexFi has a **complete social infrastructure** — contacts, groups, identity management, and cloud sync — ready for the upcoming **expense splitting engine**.

---

# Next Phase

## Phase 5 — Expense Splitting Engine

Users will be able to:

```
Add expenses within groups
Split expenses among members
Track who owes whom
View settlement dashboard
```

This will leverage the group and contact systems to enable **collaborative financial tracking**.

---

### Folder Structure (after Phase 4)

```
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
│   │   │   FirebaseAuthService.kt
│   │   │   FirestoreGroupService.kt
│   │   │   FirestoreUserService.kt
│   │   │
│   │   └───firestoreModels
│   │           ExpenseDoc.kt
│   │           GroupDoc.kt
│   │           UserDoc.kt
│   │
│   └───repository
│           ContactRepository.kt
│           ExpenseRepository.kt
│           GroupRepository.kt
│           UserRepository.kt
│
├───domain
│   ├───models
│   │       Contact.kt
│   │       Expense.kt
│   │       ExpenseSplit.kt
│   │       Group.kt
│   │       User.kt
│   │
│   └───usecases
│
├───ui
│   ├───components
│   │
│   ├───screens
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
```
