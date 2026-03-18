# FlexFi User Manual

## Version

Current workspace build, March 2026 (Phase 10)

## 1. Overview

FlexFi helps you manage:
- Personal expenses, income, and transfers
- Group expense splitting
- Settlements and pending approvals
- Budgets and goals
- Recurring transactions
- Analytics and trends
- On-device AI insights
- Flex Card social sharing

## 2. Sign In and Profile Setup

1. Sign in with phone and OTP.
2. If new user, complete profile setup (name and optional email).
3. Open Profile and set:
   - Display currency
   - Monthly income
   - Current bank balance

You can edit your name/email later from Profile.
Profile now syncs your stored identity from Firestore into local app data when available.

## 3. Navigation

- Home: balances, activity, quick actions
- Groups: shared expense groups
- Contacts: contact list and direct transfers
- Expenses: personal transaction dashboard
- Profile: identity, settings, recurring, analytics, budgets

## 4. Personal Transactions

Path: Home -> Expenses -> Add Expense

Supported transaction types:
- Expense
- Income
- Transfer

Each entry can include amount, currency, category, date, notes, and more metadata.

## 5. Group Expenses

Path: Groups -> Select Group -> Add Expense

Split modes:
- Equal
- Exact

Group Detail shows balances and suggested settlements.

## 6. Settlements

### 6.1 Global Settle Up

Path: Home -> Settle Up

You can:
- View total you owe and owed to you
- Record payment for one person
- Use Settle All Debts
- Approve/reject incoming pending payments

### 6.2 Group Settle Up

Path: Groups -> Group Detail -> Settle Up

Shows settle data for that specific group.

### 6.3 How settlement amounts are handled

- Data is stored in base currency internally.
- UI displays converted values using current display currency.
- Settle All uses base-safe recording to keep amounts accurate.

## 7. Contacts and Direct Transfers

Path: Contacts

You can record a direct payment to a contact. This contributes to settlement history and debt context.

## 8. Budgets

Path: Profile -> Open Budgets

Capabilities:
- Category budgets
- Overall budget
- Rollover support
- Warning and over-budget indicators

## 9. Goals

Path: Home -> Goals

Track target amount/date and contributions.

## 10. Recurring Transactions

Path: Profile -> Recurring Transactions

Supported intervals: Daily, Weekly, Monthly, Yearly

Actions: add, activate/deactivate, delete, Run Auto-Pay Now

## 11. Analytics

Path: Profile -> Open Analytics and Trends

Includes monthly totals, category patterns, and trend views.

## 12. AI Insights

Path: Home

Capabilities:
- Monthly AI highlights generated from local financial summary
- Explain My Spending response
- Fallback insights while model is unavailable/downloading

## 13. Flex Card (Shareable Financial Identity)

Path: Profile -> Generate Flex Card

What it includes:
- FlexFi Score (0-100)
- Grade + trend
- Highlights
- Areas for improvement
- Monthly spend and streak
- Motivational tagline

Template options:
- Dark
- Gradient
- Minimal

Actions:
- Regenerate card content
- Share card as PNG via system share sheet

## 14. Troubleshooting

### Settlement amount is incorrect

1. Verify payment currency used while recording.
2. Verify display currency in Profile.
3. Reopen Settle Up after sync.

### Same person appears twice in Settle Up

1. Ensure contact phone variants represent same person.
2. Reopen Settle Up after sync refresh.

### Group settle page is empty

1. Confirm group has expenses.
2. Open group detail and then Settle Up again.

### Profile name not visible

1. Ensure profile setup completed.
2. Open Profile and save name once.

### Flex Card shows generic user label

1. Reopen Profile with internet once to sync remote identity.
2. Confirm name/email exists in profile fields.
3. Regenerate card.

### Flex Card layout looks crowded

1. Switch template and regenerate.
2. Ensure latest app build is installed.
3. Verify preview uses updated renderer layout.

## 15. Best Practices

1. Set bank balance baseline early.
2. Use consistent contact phone formatting.
3. Review Settle Up pending approvals regularly.
4. Keep display currency stable while auditing numbers.
5. Generate Flex Card after monthly data has stabilized for best insight quality.
