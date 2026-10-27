# KOSH Improvements & Feature Architecture Handoff (V3)
**Date**: October 27, 2026  
**Status**: Ready for Implementation in Next Session  
**Package**: `com.kosh.app` (`D:\Android Apps\Kosh`)  
**Target Device**: Physical Vivo V2514 (`10BFCT1GEK002PH`) & Pixel Emulator  

---

## 1. Executive Summary of New Requirements

| # | User Requirement | Root Cause & Current State | Target Files |
|---|------------------|---------------------------|--------------|
| **1** | **Click & Edit Monthly Bills**: Bill item cannot be clicked to edit. | `DashboardScreen.kt` lines 388–398 renders `HouseholdItem` with only `onPay` and `onUndo`. No edit sheet exists. | [`DashboardScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/DashboardScreen.kt), [`EditHouseholdObligationBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/EditHouseholdObligationBottomSheet.kt) *(New)*, [`MainActivity.kt`](app/src/main/java/com/kosh/app/MainActivity.kt) |
| **2** | **Link Transaction to Bill & Link Incoming Credit to Split**: Ability to link a debit transaction to a bill, and link an incoming credit to a specific split debt. | Currently transactions and household obligations are loosely paired. Incoming credits only auto-reconcile by exact name without manual picker. | [`EditTransactionBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/EditTransactionBottomSheet.kt), [`PendingInScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/PendingInScreen.kt), [`KoshRepository.kt`](app/src/main/java/com/kosh/app/data/repository/KoshRepository.kt) |
| **3** | **Fixed Monthly Expenses & SIP Category in Bills**: Rebrand home bills as "Fixed Monthly Expenses" and add SIP/Wealth category. | `AddHouseholdObligationBottomSheet.kt` only has Rent, Cook, Maid, Wifi, etc. Lacks `SIP_WEALTH` option. Title is "Home Bills". | [`AddHouseholdObligationBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/AddHouseholdObligationBottomSheet.kt), [`DashboardScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/DashboardScreen.kt), [`HouseholdObligationEntity.kt`](app/src/main/java/com/kosh/app/data/model/HouseholdObligationEntity.kt) |
| **4** | **Silent SMS Scanning (No 5000+ message toast on open)**: Stop showing toast on every launch when no new messages are imported. | `KoshViewModel.kt` `scanSmsInbox()` always emits message on launch regardless of count. Also scans entire inbox every launch. | [`KoshViewModel.kt`](app/src/main/java/com/kosh/app/ui/viewmodel/KoshViewModel.kt), [`SmsAutoScanner.kt`](app/src/main/java/com/kosh/app/data/scanner/SmsAutoScanner.kt) |
| **5** | **Uniform Credit Card Colors**: First card (ICICI) shows blue while others are green; all should have uniform styling. | `CardsScreen.kt` lines 207–237 applies `RoyalBlue` gradient only if `isFirst` is true. | [`CardsScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/CardsScreen.kt) |
| **6** | **Bank & UPI Outflow Spend Amounts & Noise SMS Filtering**: Cards section doesn't show money spent on Bank/UPI. Filter out requested money/mandate messages. | `BankAccountItem` only displays "Active Listener" badge with no calculated spend. `SmsAutoScanner` needs stricter filter for "requested", "mandate", "autopay scheduled". | [`CardsScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/CardsScreen.kt), [`SmsAutoScanner.kt`](app/src/main/java/com/kosh/app/data/scanner/SmsAutoScanner.kt) |
| **7** | **Edit Person Profile**: Clicking a person in Shared Splits should open person editor. | `PersonLedgerCard` in `PendingInScreen.kt` has no click listener or edit modal. | [`PendingInScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/PendingInScreen.kt), [`EditPersonBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/EditPersonBottomSheet.kt) *(New)*, [`KoshViewModel.kt`](app/src/main/java/com/kosh/app/ui/viewmodel/KoshViewModel.kt) |
| **8** | **Contact Linking & Person Categories**: Add person with phone contact integration (system contact picker on-demand) or custom categories (Friend, Flatmate, Colleague, Family, Partner). | `PersonPickerModal.kt` only has name and hardcoded "Friend". Need system contact picker intent (`ActivityResultContracts.PickContact`) and category selector. | [`PersonPickerModal.kt`](app/src/main/java/com/kosh/app/ui/components/PersonPickerModal.kt), [`MainActivity.kt`](app/src/main/java/com/kosh/app/MainActivity.kt), [`PersonEntity.kt`](app/src/main/java/com/kosh/app/data/model/PersonEntity.kt) |
| **9** | **Dual Ledger Persona Support (Circle + Owed)**: A person can be a circle/companion and also owe money in splits. | System must ensure `circleMember` and `splitDebts` coexist cleanly without mutual exclusion. | [`KoshRepository.kt`](app/src/main/java/com/kosh/app/data/repository/KoshRepository.kt), [`PendingInScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/PendingInScreen.kt) |
| **10** | **Itemized WhatsApp Nudge**: Nudge message hardcodes "Flat expenses"; should list each shared expense item and total. | `launchWhatsApp()` in `PendingInScreen.kt` line 489 hardcodes `"split for flat expenses ($amount)"` instead of iterating `debts: List<SplitDebtEntity>`. | [`PendingInScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/PendingInScreen.kt) |

---

## 2. Technical Blueprint & Step-by-Step Architecture

### Task 1 & 3: Fixed Monthly Expenses, Bill Editing & SIP Category
1. **Rebrand & Redesign Section**:
   - In `DashboardScreen.kt`, rename header from `HOUSEHOLD OBLIGATIONS` to **`FIXED MONTHLY EXPENSES & BILLS`**.
   - Make each `HouseholdItem` row clickable:
     - Tapping the row opens `EditHouseholdObligationBottomSheet`.
     - "Pay & Split" / "Mark Paid" button remains accessible as a direct action.
2. **`EditHouseholdObligationBottomSheet.kt` (New)**:
   - Fields: Title, Category (with icons), Expected Amount, Due Day of month, Recipient / Notes, Split toggle.
   - Action buttons: "Delete Expense" (red outline) and "Save Changes" (ElectricMint).
3. **Add SIP / Wealth Category to Fixed Obligations**:
   - In `AddHouseholdObligationBottomSheet.kt` & `EditHouseholdObligationBottomSheet.kt`, include:
     - 📈 **SIP / Mutual Funds**
     - 🏠 **Rent & Maintenance**
     - 🍳 **Cook / Chef**
     - 🧹 **Maid / Cleaning**
     - ⚡ **Electricity & Utilities**
     - 📶 **Wi-Fi / Broadband**
     - 📱 **Mobile / DTH**
     - 🛡️ **Insurance / EMI**

---

### Task 2: Linking Transactions to Bills & Linking Inflow Credits to Splits
1. **Link Transaction to Fixed Obligation**:
   - In `EditTransactionBottomSheet.kt`:
     - Add section: **"Link to Fixed Monthly Expense"**.
     - Dropdown / chip picker showing active obligations (`householdObligations`).
     - Selecting one links `transaction.note = "[Bill: ${ob.title}] $note"` and updates obligation status in Room as paid for the month.
2. **Manual Inflow Reconcile to Specific Split**:
   - In `PendingInScreen.kt`, add a **"Match Payment to Split"** button:
     - Opens a bottom sheet showing all open `SplitDebtEntity` items.
     - User picks which specific split was paid (e.g. "Dinner at Toit - ₹850").
     - Automatically settles that debt and adjusts the person's running balance.

---

### Task 4: Silent Incremental SMS Scanning
1. **Incremental Timestamp Tracker**:
   - In `SmsAutoScanner.kt`, store `last_scanned_sms_timestamp` in `SharedPreferences`.
   - When scanning, only query SMS where `date > lastScannedTimestamp`.
   - This reduces scan time from thousands of messages to just 0–5 new messages!
2. **Quiet Notification**:
   - In `KoshViewModel.kt`:
     ```kotlin
     if (result.importedTransactions > 0 || result.detectedCredits > 0) {
         emitMessage("📥 Imported ${result.importedTransactions} new transactions (${result.detectedCredits} credits detected).")
     }
     // If 0 new messages, remain silent!
     ```

---

### Task 5: Uniform Credit Card Colors in Cards Section
1. In `CardsScreen.kt`:
   - Remove `isFirst` color branching.
   - Apply a premium, uniform obsidian dark glass gradient to all credit cards:
     ```kotlin
     val cardGradient = listOf(Color(0xFF0F1A26), Color(0xFF131E2E), Color(0xFF101622))
     ```
   - Accent color on all cards should be uniform `ElectricMint` or bank-specific subtle accent.

---

### Task 6: Bank & UPI Dynamic Outflow & Stricter SMS Noise Filter
1. **Bank & UPI Outflow Spend Amount**:
   - In `CardsScreen.kt`, pass `transactions` into `BankAccountItem`.
   - Compute month spend for the account:
     ```kotlin
     val upiSpend = transactions.filter { tx ->
         !tx.isAssetWealth && (
             tx.instrumentName.contains("UPI", ignoreCase = true) ||
             tx.instrumentName.contains(bank.bankName, ignoreCase = true) ||
             (bank.upiHandle?.isNotBlank() == true && tx.instrumentName.contains(bank.upiHandle))
         )
     }.sumOf { it.trueNetAmount }
     ```
   - Display: `${currency.symbol}${upiSpend.toInt()} outflow this month`.
2. **SMS Noise Filtering**:
   - In `SmsAutoScanner.kt` and `SmsBroadcastReceiver.kt`:
   - Reject messages containing:
     * `requested money`
     * `request to pay`
     * `has requested`
     * `mandate created`
     * `mandate request`
     * `autopay scheduled`
     * `pre-auth`
     * `otp`
     * `verification code`

---

### Task 7 & 8: Edit Person Profile, Contact Linking & Person Categories
1. **Edit Person Sheet (`EditPersonBottomSheet.kt`)**:
   - Ability to edit Name, Phone Number, Relationship/Category (`Friend`, `Flatmate`, `Colleague`, `Partner`, `Family`, `Custom`), Pin/Unpin, and Delete person.
2. **System Contact Picker**:
   - In `MainActivity.kt`, register `ActivityResultContracts.PickContact()`:
     ```kotlin
     val contactPickerLauncher = registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
         uri?.let { viewModel.importPersonFromContactUri(context, it) }
     }
     ```
   - In `PersonPickerModal.kt`: Add button **"📱 Link from Contacts"** which triggers `contactPickerLauncher.launch(null)`.
   - *Note: `PickContact` uses Android's native contact picker and does NOT require upfront `READ_CONTACTS` permission!*
3. **Unique Mapping**:
   - Fallback if no contact permission: mapped uniquely by Room auto-generated `id` + `name`.

---

### Task 9: Dual Persona Coexistence (Circle + Debt Split)
- Ensure a person who is marked as `isCircleMember = true` can also have split records in `split_debts`.
- In `PendingInScreen.kt`, display a subtle `❤️ Circle Companion` tag on their card, while maintaining their exact split debts and running balance.

---

### Task 10: Itemized WhatsApp Nudge
1. In `PendingInScreen.kt`, update `launchWhatsApp()`:
   ```kotlin
   fun buildWhatsAppMessage(personName: String, debts: List<SplitDebtEntity>, totalAmount: String, currencySymbol: String): String {
       val itemsList = debts.map { "• ${it.itemDescription}: $currencySymbol${it.amount.toInt()}" }.joinToString("\n")
       return """
       Hey $personName! 👋
       Here is the breakdown of our shared expenses:
       
       $itemsList
       ━━━━━━━━━━━━━━━
       Total pending: $totalAmount
       
       Whenever you get a chance to settle, thanks!
       """.trimIndent()
   }
   ```
2. Pass `specificDebts` directly into `onNudge` so the generated message is fully itemized!

---

## 3. Checklist for Next Session

- [ ] **Step 1**: Create `EditHouseholdObligationBottomSheet.kt` and wire tap-to-edit in `DashboardScreen.kt`.
- [ ] **Step 2**: Add SIP/Wealth category to `AddHouseholdObligationBottomSheet.kt` & rebrand header to "Fixed Monthly Expenses & Bills".
- [ ] **Step 3**: Update `SmsAutoScanner.kt` with timestamp-based incremental scanning & silence 0-message toasts.
- [ ] **Step 4**: Filter out UPI payment requests and mandates in `SmsAutoScanner.kt`.
- [ ] **Step 5**: Standardize card colors in `CardsScreen.kt` & calculate monthly outflow for Bank/UPI accounts.
- [ ] **Step 6**: Add itemized breakdown to WhatsApp nudge in `PendingInScreen.kt`.
- [ ] **Step 7**: Create `EditPersonBottomSheet.kt` and integrate Android Native Contact Picker (`PickContact`).
- [ ] **Step 8**: Add "Link to Bill" dropdown in `EditTransactionBottomSheet.kt` and "Match Payment to Split" in `PendingInScreen.kt`.
- [ ] **Step 9**: Compile with `./gradlew assembleDebug` and install to Vivo V2514 (`10BFCT1GEK002PH`).
