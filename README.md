# FlexFi

FlexFi is an Android app for personal finance plus social expense splitting.
It combines:
- Personal income/expense tracking
- Group expense splitting and settlement flows
- Contacts including ghost users (phone-first)
- Budgets, recurring transactions, and goals
- Multi-currency display with base-currency storage

## Current Status (March 2026)

Core workflows are implemented and connected to Room + Firebase sync.

Recent fixes include:
- Settlement amount integrity for Settle All (prevents double conversion)
- Settlement aggregation by canonical phone to avoid duplicate person rows
- Group settle page sync improvements (expenses + settlements refreshed before calculation)
- Profile identity support (signup name visible and editable)

## Tech Stack

- Kotlin
- Jetpack Compose
- Room (offline-first local source)
- Firebase Auth (OTP)
- Firestore (cloud sync)

## Core Flows

1. Authentication
2. Profile setup
3. Personal and group expense tracking
4. Settlement recording and approval
5. Budget, goals, and recurring transactions

## Currency Model

- Financial values are persisted in USD (base currency)
- UI renders values in selected display currency
- Input/output boundaries apply conversion

## Documentation Map

- Full product progress: Dev_Progress.md
- User operations: USER_MANUAL.md
- In-app quick help: IN_APP_HELP.md
- QA/support checks: SUPPORT_GUIDE.md

## Changelog

### 2026-03-17 - Settlement and Profile Reliability Update

- Fixed Settle All conversion path to prevent incorrect tiny/large settlement values.
- Merged duplicate settle rows for the same person via canonical phone aggregation.
- Improved group and global settle consistency by syncing expenses and settlements before debt recalculation.
- Added pending approval lifecycle clarity (PENDING, COMPLETED, REJECTED) in docs and flows.
- Added profile identity completion support: signup name now visible and editable from Profile.

### 2026-03 - Balance and Currency Integrity Patch

- Fixed monthly spending recomputation in personal dashboards.
- Improved derived home balance behavior based on transaction and settlement cashflow.
- Reduced display drift by using transaction-aware amount rendering when display currency matches source currency.