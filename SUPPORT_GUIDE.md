# FlexFi Support Guide

Use this for QA, bug triage, and support reproduction.

## Scope

- Auth and profile identity
- Personal transactions
- Group splits and settlements
- Settle All and pending approvals
- Budgets, goals, recurring, analytics
- Currency and balance integrity
- AI insights and on-device model behavior
- Flex Card generation, rendering, and sharing

## Critical Regression Checks

### 1. Profile identity

1. Sign up as a new user.
2. Enter name in profile setup.
3. Open Profile screen.
4. Edit and save name.

Expected:
- Name appears after signup.
- Name remains after relaunch/login.

### 2. Settlement amount integrity

1. Create debt to a contact (example: 400.89 in display currency).
2. Tap Settle All.
3. Inspect pending settlement amount and post-approval behavior.

Expected:
- Amount remains correct in display terms.
- No tiny/incorrect values caused by conversion mismatch.

### 3. Duplicate settle rows

1. Create contact variants for same person phone formatting.
2. Open global Settle Up.

Expected:
- A single summed row per logical person.

### 4. Group settle page consistency

1. Add group expense.
2. Open Group Detail -> Settle Up.
3. Open Home -> Settle Up.

Expected:
- Group settle page is not empty when balances exist.
- Group and global views are consistent for that group data.

### 5. Pending approval lifecycle

1. Record payment.
2. Verify receiver sees PENDING approval item.
3. Accept and verify effect; repeat with reject.

Expected:
- Status transitions: PENDING -> COMPLETED or REJECTED.
- Debt refreshes after action.

### 6. Flex Card identity and content

1. Login with existing phone account.
2. Open Profile and confirm name/email are visible.
3. Tap Generate Flex Card.

Expected:
- Card shows synced user identity, not generic placeholder text.
- Card contains 3 highlights, 1 improvement, 1 tagline.

### 7. Flex Card layout and visual checks

1. Open Flex Card preview.
2. Verify sections and spacing.
3. Switch templates and regenerate.

Expected:
- Improvement section appears below highlights.
- Score number has strongest visual emphasis.
- Grade/trend show semantic color and symbol.
- Tagline is outside inner rounded card with visible spacing.

### 8. Flex Card share flow

1. From preview, tap Share.
2. Select target app from chooser.

Expected:
- Share sheet opens with image/png payload.
- Receiving app can open attached PNG.

## Standard Functional Checklist

1. Personal add/edit/delete transaction
2. Group expense with equal and exact split
3. Budget creation + threshold behavior
4. Goal creation + contribution
5. Recurring transaction execution
6. Analytics view integrity
7. Home AI insights/explain flow integrity
8. Flex Card preview/template/share integrity

## Triage Questions Template

1. Exact screen path used
2. Group settle or global settle
3. Currency selected at action time
4. Before/after screenshots from Home, Settle Up, Profile
5. Whether issue reproduces after app reopen

## Fast Root-Cause Hints

- Wrong settle amount: check conversion boundary at recording path.
- Duplicate person row: check phone normalization/canonicalization.
- Empty group settle page: check pre-calculation sync and groupId filtering.
- Missing profile name: check local user sync and profile identity write path.
- Flex Card generic name: check Firestore user lookup by uid and phone fallback.
- Flex Card malformed text blocks: check strict JSON parser fallback path.
- Flex Card share failure: check FileProvider authority and xml file paths configuration.
