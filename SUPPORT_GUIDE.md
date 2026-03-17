# FlexFi Support Guide

Use this guide for QA, debugging, and user support.

## Scope
Covers current app version behavior for:
- Transactions
- Balance updates
- Budgets
- Goals
- Recurring
- Analytics
- Group splits and settlements

## Core Verification Checklist

### 1. Financial Baseline
1. Open Profile.
2. Save Monthly Income and Current Bank Balance.
3. Confirm values are shown on Home/Profile consistently.

Expected:
- Home total balance equals Profile current bank balance (display-converted).

### 2. Personal Expense Flow
1. Add personal EXPENSE.
2. Add personal INCOME.
3. Edit one item.
4. Delete one item.

Expected:
- Items appear in Personal Dashboard.
- Analytics and category totals update.

### 3. Group Expense Flow
1. Create/add group members.
2. Add group expense (paid by current user).
3. Test both Equal and Exact splits.
4. Delete one group expense.

Expected:
- Group balances update.
- Settlement suggestions update.
- Home/Profile balance updates for payer-related cash movement.

### 4. Settle Up Flow
1. Open Settle Up.
2. Record a payment.

Expected:
- Debt list refreshes.
- Balance adjusts accordingly.

### 5. Budget Flow
1. Open Profile -> Open Budgets.
2. Add category budget.
3. Add/update OVERALL budget.
4. Enable rollover on one category.
5. Add expenses to exceed thresholds.

Expected:
- Progress bars move.
- Warning near ~80% and over-budget indicators appear.

### 6. Goal Flow
1. Add goal with target amount/date.
2. Add contribution.
3. Edit goal.

Expected:
- Progress and milestone state update.

### 7. Recurring Flow
1. Add recurring transaction.
2. Toggle active.
3. Run Auto-Pay Now.

Expected:
- New generated transaction where applicable.
- Balance updates where applicable.

### 8. Analytics Flow
1. Open analytics from Profile.
2. Confirm monthly total, category breakdown, and trend chart.

## Common Issues and Fix Checks

### Issue: Balance reverts after relaunch/rebuild
Check:
1. Opening balance anchor logic runs.
2. Home reconciliation derives and syncs total from transaction ledger.
3. Profile and Home display same base value.

### Issue: Group expense added but balance unchanged
Check:
1. Current user is actual payer.
2. Expense saved successfully.
3. Balance delta write path executed.

### Issue: Budget screen missing
Check:
1. Route exists in NavHost.
2. Profile has Open Budgets entry.

## Support Response Template
1. Ask for exact path user followed.
2. Ask whether it was personal expense, group expense, or settlement.
3. Ask payer identity and currency selected.
4. Ask for before/after balance screenshots (Home + Profile).
5. Reproduce with same flow and date.

## Suggested Regression Suite
- Currency switch with existing transactions
- Save financial settings then add group expense
- Delete group expense and verify reversal
- Settlement record and balance update
- Rebuild/relaunch and verify reconciliation
