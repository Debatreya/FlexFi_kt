# FlexFi Development Progress

## Snapshot

- Project: FlexFi (Android)
- Current milestone: Phase 10 Flex Card rollout
- Last updated: March 2026

FlexFi is now in a product-hardening stage focused on trust in balances, settlements, and sync behavior.

## Completed Foundations (Phases 0-7)

1. Architecture and navigation baseline with Kotlin + Compose + Room + Firebase
2. Offline-first local data model with DAO/repository layers
3. OTP auth and cloud/local user sync
4. Contacts with ghost-user model
5. Group creation, member sync, and group detail flows
6. Group split and settlement suggestion engine
7. Personal finance flows, streaks, categorization, and dashboard analytics

## Phase 8 Stabilization (March 2026)

### F. On-device AI insights integration (MediaPipe LLM)

- Added `AIManager` layer between `HomeViewModel` and on-device model runtime.
- Implemented monthly summary prompt pipeline with deterministic 3-insight parsing.
- Added "Explain My Spending" flow with fallback explanation path.
- Added local cache for generated insights keyed by summary hash.
- Added resilient model download pipeline:
- Runtime download from configured model URL
- Progress state surfaced in Home UI
- Retry with exponential backoff on failures
- 401 unauthorized handling with clear log guidance

### G. Runtime validation on physical device

- Model download now starts and progresses on real device (no longer stuck at unauthorized URL state).
- Dashboard shows fallback insights while model is downloading, then switches to model-backed outputs once ready.
- AI inference works end-to-end, including explanation generation.
- On older 4G Android hardware, first inference is slow (tens of seconds), which is expected for 1B on-device models.
- Current behavior is functionally correct; performance optimization is an open tuning task.

### A. Settlement integrity and currency correctness

- Fixed Settle All path to avoid re-converting already base-currency debt values.
- Introduced a base-amount settlement recording path for bulk settlement actions.
- Ensured settlement totals and debt rows render from base storage through display conversion only at UI level.

### B. Duplicate-person settlement rows removed

- Added canonical phone normalization in settle aggregation.
- Different phone formatting variants now merge into one summed row per person.

### C. Group settle view sync reliability

- Added pre-calculation sync for group expenses and user settlements.
- Group settle page and global settle page now use aligned source data and recalc flow.

### D. Pending approval lifecycle improved

- Settlement records now follow explicit status lifecycle: PENDING, COMPLETED, REJECTED.
- Pending approvals are visible and actionable, with acceptance/rejection updating balances accordingly.

### E. Profile identity completeness

- Signup name now appears on profile page.
- Added editable profile identity fields (name/email) with local + Firestore update path.

## Validation

- File-level static error checks on edited Kotlin files: clean.
- Build command intentionally not run in terminal (project workflow: validate in Android Studio).
- Android Studio device run: AI download + inference path verified through logs and UI state transitions.

## Open Follow-ups

1. Add historical reconciliation utility for legacy settlement/phone-format data.
2. Add instrumentation tests for settle aggregation and cross-screen consistency.
3. Add migration-safe normalization for old contact/phone entries.
4. Add release-safe model distribution path that does not require embedding any Hugging Face token.
5. Add optional warmup and prompt-size trimming for faster first-token latency on low-end devices.

## Phase 10 Flex Card (March 2026)

### H. Flex score engine

- Implemented weighted score model (0-100):
- Budget adherence (30%)
- Savings rate (25%)
- Spending growth (15%)
- Category balance (10%)
- Streak (10%)
- Impulse behavior (10%)
- Added grade mapping: ELITE, GOLD, SILVER, BRONZE, BEGINNER.
- Added trend mapping vs previous month score: Improving, Stable, Declining.
- Added edge handling for no income, no previous month data, low transaction volume, and zero-spend cap.

### I. AI JSON card content

- Added strict JSON prompt for card content generation.
- Added parser + validator requiring exactly:
- 3 highlights
- 1 improvement
- 1 tagline
- Added fallback card content on malformed/incomplete LLM output.
- Added monthly cache for Flex Card AI responses.

### J. Template rendering and sharing

- Added template system: Dark, Gradient, Minimal.
- Added high-resolution card renderer for 1080x1920 output.
- Added profile photo support with initials avatar fallback.
- Added PNG export to cache and social sharing via ACTION_SEND.
- Added FileProvider + XML paths for secure sharing.

### K. UI flow

- Added Generate Flex Card action near profile photo.
- Added preview screen with template chips, regenerate, and share actions.
- Added persistent template preference in settings.
- Updated card typography/layout hierarchy:
- Numeric score emphasized over label
- Grade/trend displayed with symbols and semantic color
- Improvement panel moved below highlights
- Tagline moved outside inner rounded card with spacing

### L. Identity sync reliability

- Added profile-time user sync from Firestore into local user table.
- Sync path now attempts UID and falls back to phone lookup.
- Profile and card name fallback now prioritizes name, then email prefix, then phone-derived label.
