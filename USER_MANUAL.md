# FlexFi User Manual

## Version
Current workspace build (March 2026)

## 1. What FlexFi Does
FlexFi helps you manage:
- Daily personal expenses and income
- Group expense splitting and settlements
- Category and overall budgets
- Savings goals and milestones
- Recurring transactions
- Spending analytics

It supports multiple display currencies and dark/light theme.

## 2. Getting Started

### 2.1 Sign In
1. Open the app.
2. Enter your phone number on the login screen.
3. Verify using OTP.
4. If you are a new user, complete profile setup.

### 2.2 First-Time Setup (Recommended)
1. Open Profile.
2. Set:
   - Display Currency
   - Monthly Income
   - Current Bank Balance
3. Tap Save Financial Settings.

Tip: The bank balance you set acts as your baseline for tracking cash movement.

## 3. Main Navigation

### Home
- Total Balance card
- You Owe / Owed To You / Net summary
- Active Groups
- Recent Activity
- Quick actions: Settle Up, Goals

### Bottom Tabs
- Home
- Groups
- Contacts
- Expenses
- Profile

## 4. Personal Expense Tracking

### 4.1 Add Personal Expense/Income/Transfer
Path:
- Home -> Expenses (Personal Dashboard) -> Add Expense

You can log:
- Amount
- Type (Expense, Income, Transfer)
- Category and Sub-category
- Date
- Payment Mode
- Merchant
- Tags
- Attachment (receipt image)
- Notes/description

### 4.2 Edit/Delete Personal Transactions
Path:
- Home -> Expenses -> tap an item

## 5. Group Expense Splitting

### 5.1 Create/Manage Groups
Path:
- Home -> Groups

### 5.2 Add Group Expense
Path:
- Groups -> Select Group -> Add Expense

Fields include:
- Title
- Amount
- Currency
- Category
- Paid by (member)
- Split mode:
  - Equal split
  - Exact split

### 5.3 View Split Details
Path:
- Group Detail -> Expense Detail

## 6. Settlements (Settle Up)
Path:
- Home -> Settle Up

Use this screen to:
- See who you owe / who owes you
- Record external payments

Recording a settlement updates the balance and debt tracking.

## 7. Budgets
Path:
- Profile -> Open Budgets

### 7.1 What You Can Do
- Create category budgets (Food, Transport, etc.)
- Create/update an OVERALL budget
- Enable rollover for unused budget amounts
- Navigate month-to-month
- See budget vs actual progress bars
- See warning and over-budget indicators

### 7.2 Understanding Alerts
- Warning: near limit (around 80% usage)
- Over budget: spending exceeded the limit

## 8. Goals
Path:
- Home -> Goals

### 8.1 Goal Features
- Set target amount
- Set target date
- Track saved amount
- Mark as sinking fund
- Add auto-save settings

### 8.2 Milestones
Progress milestones are tracked as you contribute toward goals.

## 9. Recurring Transactions
Path:
- Profile -> Recurring Transactions

### 9.1 Supported Intervals
- Daily
- Weekly
- Monthly
- Yearly

### 9.2 Actions
- Add recurring item
- Toggle active/inactive
- Delete recurring item
- Run Auto-Pay Now (manual trigger)

## 10. Analytics
Path:
- Profile -> Open Analytics & Trends

You can view:
- Monthly spending total
- Category breakdown
- Recent activity insights
- Time-based trend visuals

## 11. Currency and Balance Behavior

### 11.1 Currency
- You can choose display currency in Profile.
- Values are internally normalized for consistency and shown in selected display currency.

### 11.2 Balance
- Home and Profile balance values are synchronized.
- Balance updates from:
  - Personal transactions
  - Group expenses paid by you
  - Settlements
  - Recurring processing

If you manually save a new Current Bank Balance in Profile, that value becomes the new baseline from that point onward.

## 12. Troubleshooting

### Issue: Balance looks unchanged after actions
Try:
1. Return to Home and wait for sync.
2. Reopen the app once.
3. Check transaction date and payer details.
4. Verify whether action was personal, group, or settlement.

### Issue: Wrong currency symbol
1. Open Profile.
2. Confirm Display Currency.
3. Save settings and return to Home.

### Issue: Budget screen not found
Use:
- Profile -> Open Budgets

## 13. Best Practices
- Set your opening bank balance before heavy usage.
- Use categories and sub-categories consistently.
- Add merchant and tags for cleaner analytics.
- Review budgets weekly.
- Use recurring transactions for subscriptions/rent.
- Set goals with realistic target dates.

## 14. Data Notes
- Local data and sync behavior depend on current app architecture.
- Reinstalling/clearing app data may reset local state.
- Keep backups/exports if you rely on historical records.

## 15. Quick Paths Cheat Sheet
- Add personal expense: Home -> Expenses -> Add Expense
- Add group expense: Home -> Groups -> Group -> Add Expense
- Budgets: Profile -> Open Budgets
- Goals: Home -> Goals
- Recurring: Profile -> Recurring Transactions
- Analytics: Profile -> Open Analytics & Trends
- Settle Up: Home -> Settle Up

---
If you want, this manual can be split into:
- End-user guide (short)
- Admin/support guide (diagnostics and edge cases)
for cleaner in-app Help integration.
