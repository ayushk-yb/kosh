# KOSH Architecture & Implementation Handoff (V4)
**Date**: October 27, 2026  
**Status**: Ready for Implementation in Next Session  
**Package**: `com.kosh.app` (`D:\Android Apps\Kosh`)  
**Target Device**: Physical Vivo V2514 (`10BFCT1GEK002PH`)  

---

## 1. Executive Summary of Requirements & Bug Fixes

| # | Feature / Bug | Root Cause & Current State | Target Files |
|---|---------------|----------------------------|--------------|
| **1** | **Direct & Perfectly Aligned "+ Add Person" Button in Splits** | Users currently have to record an IOU or tap Circle Companion to add a person. Top header has only "Match Split" and "Record Split". | [`PendingInScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/PendingInScreen.kt), [`MainActivity.kt`](app/src/main/java/com/kosh/app/MainActivity.kt) |
| **2** | **Partial Settlement & Optional Transaction Linking (Cash / UPI / Bank)** | Clicking "Settle" immediately wipes the entire debt balance. There is no partial amount input, nor an option to specify whether it was a Cash deposit or linked to an incoming bank transaction. | [`PendingInScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/PendingInScreen.kt), [`SettlePersonBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/SettlePersonBottomSheet.kt) *(New)*, [`KoshRepository.kt`](app/src/main/java/com/kosh/app/data/repository/KoshRepository.kt), [`KoshViewModel.kt`](app/src/main/java/com/kosh/app/ui/viewmodel/KoshViewModel.kt) |
| **3** | **CRITICAL BUG: Flat Rent & Maintenance Multi-Select Flatmates Broken** | In `MainActivity.kt` line 404, `onSelectionConfirmed` in `PersonPickerModal` has `// Handled in dialog` and **does nothing**! `PayHouseholdBottomSheet` maintains an isolated local state (`take(2)`) disconnected from the picker. Selecting flatmates never updates the split share. | [`PayHouseholdBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/PayHouseholdBottomSheet.kt), [`MainActivity.kt`](app/src/main/java/com/kosh/app/MainActivity.kt) |
| **4** | **Disable Split for SIP / Wealth & Insurance / EMI** | SIPs and Insurance payments are personal investments; they should never show "Pay & Split". | [`DashboardScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/DashboardScreen.kt), [`PayHouseholdBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/PayHouseholdBottomSheet.kt) |
| **5** | **Custom Category Creation** | Categories are hardcoded enums. Users cannot add custom expense or investment categories. | [`CategoryEntity.kt`](app/src/main/java/com/kosh/app/data/model/CategoryEntity.kt) *(New)*, [`CategoryDao.kt`](app/src/main/java/com/kosh/app/data/local/CategoryDao.kt) *(New)*, [`AddCategoryBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/AddCategoryBottomSheet.kt) *(New)* |
| **6** | **Clean Split Selection Modal ("Split this Expense?" flow)** | Enabling "Split this Expense" dumps raw chips on screen. It should show a clean **"Select Person to Split With"** button opening an in-app person picker with contacts linking. | [`AddExpenseBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/AddExpenseBottomSheet.kt), [`EditTransactionBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/EditTransactionBottomSheet.kt) |

---

## 2. Deep-Dive Root Cause & Architectural Blueprint

### Task 1: Direct & Perfectly Aligned "+ Add Person" Button in Splits
1. **Layout Alignment**:
   - In `PendingInScreen.kt`, redesign the header actions into a balanced, symmetrical horizontal row:
     - **`+ Person`** (Outlined with ElectricMint/SubSurface)
     - **`Match Split`** (SubSurface obsidian border)
     - **`Record Split`** (AmberGold primary pill)
   - Ensure uniform height (`36.dp`), vertical alignment, and spacing (`8.dp`) so that nothing overflows or wraps awkwardly on smaller screens.
2. **Behavior**:
   - Clicking **`+ Person`** directly opens the `AddPersonDialog` / `PersonPickerModal` without having to initiate an IOU or expense.

---

### Task 2: Flexible Settlement Bottom Sheet (`SettlePersonBottomSheet.kt`)
1. **Current Flaw**:
   - Tapping "Settle" immediately calls `manualSettlePerson(personName)`, which zeroes out the person's entire balance and marks all open debts as settled.
   - If a friend returns ₹1,000 out of ₹3,500, or pays via cash, there is no way to record it.
2. **Architecture for `SettlePersonBottomSheet.kt`**:
   - **Header**: Shows person's name, avatar, and total outstanding debt (e.g. `₹4,500 owed`).
   - **Settlement Amount Field**:
     - Pre-filled with full balance, but editable for partial settlement (e.g. ₹1,500).
     - Quick chips: `[₹500]` `[₹1,000]` `[₹2,000]` `[Full Amount]`.
   - **Payment Mode / Transaction Link (Optional)**:
     - **Option A: Cash / Direct Settlement** (Default, no bank transaction required).
     - **Option B: Link to Inflow Transaction** (Dropdown/list showing recent unlinked credits, e.g. "UPI Credit from PhonePe - ₹1,500").
   - **Notes Field**: Optional note (e.g., "Cash given at office").
   - **Action Button**: `Confirm Settlement (₹1,500)`.
3. **Repository Logic (`KoshRepository.kt`)**:
   ```kotlin
   suspend fun recordPartialSettlement(
       personName: String,
       amountSettled: Double,
       linkedTransactionId: Long? = null,
       note: String = "Manual settlement"
   ) {
       // 1. Reduce person's running balance
       val person = db.personLedgerDao().getPersonByName(personName) ?: return
       val newBalance = (person.runningBalance - amountSettled).coerceAtLeast(0.0)
       db.personLedgerDao().updateBalance(personName, newBalance)

       // 2. Proportionally or sequentially settle open debts
       var remaining = amountSettled
       val openDebts = db.splitDebtDao().getOpenDebtsForPerson(personName).sortedBy { it.dateCreated }
       for (debt in openDebts) {
           if (remaining <= 0) break
           if (remaining >= debt.amount) {
               db.splitDebtDao().settleDebt(debt.id)
               remaining -= debt.amount
           } else {
               // Partially settle this debt item by reducing its open amount
               db.splitDebtDao().updateSplitDebt(debt.copy(amount = debt.amount - remaining))
               remaining = 0.0
           }
       }

       // 3. Record transaction entry if cash / not linked
       if (linkedTransactionId == null) {
           db.transactionDao().insertTransaction(
               TransactionEntity(
                   title = "Settlement from $personName",
                   grossAmount = 0.0,
                   trueNetAmount = 0.0,
                   reimbursableAmount = 0.0,
                   category = TransactionCategory.OTHER.name,
                   instrumentName = "Cash / Direct",
                   note = "Received ₹${amountSettled.toInt()} ($note)"
               )
           )
       }
   }
   ```

---

### Task 3: Root Cause & Fix for Flat Rent Multi-Select Flatmates
1. **The Bug Located**:
   - In `MainActivity.kt` line 397:
     ```kotlin
     if (showPersonPickerForHousehold) {
         PersonPickerModal(
             title = "Select Flatmates",
             persons = allPersons,
             selectedPersonIds = emptySet(), // BUG 1: Never passes currently selected flatmates!
             isMultiSelect = true,
             onPersonSelected = {},
             onSelectionConfirmed = { selectedList ->
                 // Handled in dialog // BUG 2: DOES ABSOLUTELY NOTHING!
             },
             ...
         )
     }
     ```
   - In `PayHouseholdBottomSheet.kt` line 86:
     ```kotlin
     var selectedPersons by remember {
         mutableStateOf(allPersons.filter { it.isPinned }.take(2)) // BUG 3: Hardcodes top 2 pinned!
     }
     ```
   - When the user taps "+ Select Flatmates", `PayHouseholdBottomSheet` calls `onOpenPersonPicker()`. But `PayHouseholdBottomSheet` has no way to receive the selected flatmates back from `MainActivity`!
2. **The Clean Solution**:
   - Embed the multi-select selection directly inside `PayHouseholdBottomSheet` or pass a state holder:
     - In `PayHouseholdBottomSheet.kt`, render an inline flatmate selection sheet / dialog that directly mutates `selectedPersons`.
     - Or pass `initialSelectedPersonIds: Set<Long>` and `onSelectedFlatmatesChanged: (List<PersonEntity>) -> Unit` through `MainActivity`.
   - **User Inclusion**: Clearly state the math:
     - Total = `₹30,000`
     - If 2 flatmates selected -> Total sharing = 3 people (You + 2 Flatmates).
     - Each share = `₹10,000`.
     - Your personal net burn = `₹10,000`.
     - Reimbursable owed back = `₹20,000`.
   - Add a visual chip for **"👤 You (1 share)"** so the user clearly sees they are included in the equal split calculation!

---

### Task 4: Disable Split Option for SIP / Wealth & Insurance / EMI
1. **Dashboard Card Action**:
   - In `DashboardScreen.kt`, check `obligation.category`:
     ```kotlin
     val isNonSplittable = obligation.category.contains("SIP", ignoreCase = true) ||
                           obligation.category.contains("WEALTH", ignoreCase = true) ||
                           obligation.category.contains("INSURANCE", ignoreCase = true) ||
                           obligation.category.contains("EMI", ignoreCase = true)
     ```
   - If `isNonSplittable`:
     - Render button as: **`Mark Paid ✓`** (ElectricMint container).
     - Tapping it directly invokes `onQuickPay(obligation)` which records it as paid without opening the Pay & Split sheet.
   - If splittable (Rent, Cook, Maid, Wifi, Utilities):
     - Keep **`Pay & Split →`**.
2. **In `PayHouseholdBottomSheet.kt`**:
   - If opened for SIP or Insurance, hide the `EQUAL` and `CUSTOM` tabs completely, locking it to `SOLO` ("100% Personal Burn / Investment").

---

### Task 5: Custom Category Creation
1. **Room Database Model**:
   ```kotlin
   @Entity(tableName = "custom_categories")
   data class CustomCategoryEntity(
       @PrimaryKey(autoGenerate = true) val id: Long = 0,
       val name: String,
       val iconEmoji: String = "📁",
       val colorHex: Long = 0xFF10B981,
       val isWealthAsset: Boolean = false
   )
   ```
2. **UI Integration**:
   - In `AddExpenseBottomSheet.kt`, `EditTransactionBottomSheet.kt`, and `CategoryAnalyticsScreen.kt`:
     - Combine built-in `TransactionCategory` with `customCategories`.
     - Append a **`+ New Category`** chip at the end of the category chips row.
     - Tapping it opens `AddCategoryBottomSheet.kt` where the user types the category name and selects an emoji and color tag.

---

### Task 6: Refactored "Split this Expense?" Flow
1. **The Problem**:
   - Currently, toggling "Split with Friend / Group" in `AddExpenseBottomSheet` or `EditTransactionBottomSheet` dumps all contacts into chips across the screen.
2. **The Redesign**:
   - When "Split with Friend / Group" switch is enabled:
     - If no person selected yet:
       - Show a clean button: **`👤 Select Person to Split With`**.
     - Tapping this button opens `PersonPickerModal` displaying **only the saved people in Kosh**.
     - Inside `PersonPickerModal`, the user can also tap **`📱 Contacts`** to import someone from their phone, or **`+ New`** to add a new person.
     - Once selected, show a compact selected person card:
       - Avatar, Name, Relationship Tag, and an "Edit / Change" button.
       - Amount field: "Their Share to Pay Back".
       - Live calculation: "💡 Your true net cost: ₹X • Expected return: ₹Y".

---

## 3. Checklist for Next Session

- [ ] **Step 1**: Add perfectly aligned **`+ Person`** button in the header of `PendingInScreen.kt` and wire it to open the add person modal.
- [ ] **Step 2**: Create `SettlePersonBottomSheet.kt` with partial amount entry, quick percentage chips, and optional transaction / cash toggle.
- [ ] **Step 3**: Fix the broken `onSelectionConfirmed` in `MainActivity.kt` and embed flatmate selection cleanly in `PayHouseholdBottomSheet.kt` with "You + Flatmates" equal math.
- [ ] **Step 4**: Change action button to **`Mark Paid ✓`** for SIP / Wealth and Insurance in `DashboardScreen.kt`, bypassing the split flow.
- [ ] **Step 5**: Create `CustomCategoryEntity` in Room and add **`+ New Category`** chip in transaction sheets.
- [ ] **Step 6**: Refactor "Split this Expense?" in `AddExpenseBottomSheet.kt` & `EditTransactionBottomSheet.kt` to use a clean **`Select Person`** modal instead of dumping chips.
- [ ] **Step 7**: Compile with `./gradlew assembleDebug` and install to Vivo V2514 (`10BFCT1GEK002PH`).
