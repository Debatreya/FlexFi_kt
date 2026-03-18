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
- On-device AI Insights engine (MediaPipe LLM) integrated on Home
- Runtime model download with progress + retry/fallback handling
- Flex Card social share pipeline (score + AI content + template rendering + PNG share)
- Profile sync hardening: user name/email fetched from Firestore into local profile on load

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
6. AI insights and spending explanation
7. Generate Flex Card and share to social apps

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

### 2026-03-19 - Phase 10 Flex Card System

- Added FlexFi Score engine (0-100) with weighted behavioral model and grade/trend output.
- Added on-device LLM JSON generation for card content (3 highlights, 1 improvement, 1 tagline).
- Added strict parser/validator with deterministic fallback content.
- Added month-aware local cache for Flex Card AI payloads.
- Added template-driven renderer (Dark, Gradient, Minimal) with profile photo or initials avatar.
- Added share pipeline: PNG export to cache + ACTION_SEND intent via FileProvider.
- Added Profile entry action and preview screen with template selection, regenerate, and share.
- Updated card visual hierarchy and layout (score emphasis, grade/trend symbols/colors, improvement below highlights, tagline outside inner card).
- Improved profile identity sync path to fetch Firestore user into local profile reliably.

### 2026-03-19 - Phase 8 AI Runtime Activation

- Enabled on-device LLM insights and explanation flow from Home screen.
- Added AIManager orchestration, prompt pipeline, and deterministic parsing.
- Added local insight caching and rule-based fallback system.
- Added model download progress UI and retry/backoff behavior.
- Confirmed physical-device runtime behavior: download and inference now functional.
- Noted expected latency on older devices during first inference.

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