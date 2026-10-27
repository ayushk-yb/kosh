# KOSH New Tasks & Feature Architecture Handoff
**Date**: October 27, 2026  
**Status**: Ready for Implementation in New Session  
**Target Device**: Physical Vivo V2514 (`10BFCT1GEK002PH`) & Pixel 4 Emulator (`emulator-5554`)  
**Package**: `com.kosh.app` (`D:\Android Apps\Kosh`)

---

## 1. Executive Summary of User Feedback & New Tasks

The user verified the app on their device and identified 4 specific issues and feature requirements:

| # | User Feedback / Requirement | Current Root Cause / Status | Target Files |
|---|-----------------------------|----------------------------|--------------|
| **1** | **Remove "Primary Instrument" completely**: User never selected a primary instrument; wants this concept eliminated entirely. | `DashboardScreen.kt` lines 82 & 403–485 displays a "CARDS & ACCOUNTS / Primary Instrument" hero box by taking `paymentInstruments.firstOrNull()`. `PaymentInstrumentEntity` also has `isPrimary`. | [`DashboardScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/DashboardScreen.kt), [`PaymentInstrumentEntity.kt`](app/src/main/java/com/kosh/app/data/model/PaymentInstrumentEntity.kt), [`CardsScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/CardsScreen.kt), [`EditPaymentInstrumentBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/EditPaymentInstrumentBottomSheet.kt) |
| **2** | **Replace Partner/Family tab with Monthly Category Analytics ("Breakdown")**: User needs to see monthly spend by category clearly, including "Uncategorized", and compare against any chosen month showing `+X%` or `-X%` deltas. | Tab 4 is currently `FamilyLedgerScreen.kt` ("Circle"), focused on a single companion. Need a dedicated, beautiful `CategoryAnalyticsScreen.kt` showing categorized spends, category progress bars, and month-over-month comparison engine. | [`BottomNavBar.kt`](app/src/main/java/com/kosh/app/ui/components/BottomNavBar.kt), [`MainActivity.kt`](app/src/main/java/com/kosh/app/MainActivity.kt), [`CategoryAnalyticsScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/CategoryAnalyticsScreen.kt) (New), [`KoshViewModel.kt`](app/src/main/java/com/kosh/app/ui/viewmodel/KoshViewModel.kt) |
| **3** | **Partner/Family transaction tagging not reflecting**: Clicking a transaction and choosing partner/family should let the user tag the specific partner/family name, and it should clearly show up on the transaction row as that partner/family expense. | In `EditTransactionBottomSheet.kt`, selecting `PARTNER_FAMILY` category does not open a dedicated person/partner picker or update the note/title with the person's name (e.g. "Gift for Ananya" or "Family Expense"). | [`EditTransactionBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/EditTransactionBottomSheet.kt), [`DashboardScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/DashboardScreen.kt), [`AllTransactionsScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/AllTransactionsScreen.kt) |
| **4** | **Add "Gift" Category**: There is currently no gift category. | `TransactionCategory` enum in `Enums.kt` lacks a standalone `GIFT` category. `PARTNER_FAMILY` is overloaded with gift emoji. | [`Enums.kt`](app/src/main/java/com/kosh/app/data/model/Enums.kt), [`EditTransactionBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/EditTransactionBottomSheet.kt), [`AddExpenseBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/AddExpenseBottomSheet.kt), [`SmsAutoScanner.kt`](app/src/main/java/com/kosh/app/data/scanner/SmsAutoScanner.kt) |

---

## 2. Detailed Technical Blueprint

### Task 1: Complete Removal of "Primary Instrument"
1. **Dashboard Hero Card Removal**:
   - In [`DashboardScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/DashboardScreen.kt), remove `val primaryCard = ...` and delete the entire `CARDS & ACCOUNTS` section (lines 402–485) that renders "Primary Instrument".
   - The user manages cards and accounts exclusively inside the dedicated **Cards** tab (`CardsScreen.kt`).
2. **Sheet Cleanups**:
   - In [`EditPaymentInstrumentBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/EditPaymentInstrumentBottomSheet.kt) and [`AddPaymentInstrumentBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/AddPaymentInstrumentBottomSheet.kt), remove the "Set as Primary Instrument" switch and all references to `isPrimary`.
   - Update `PaymentInstrumentDao.kt` query: `SELECT * FROM credit_card_accounts ORDER BY id ASC`.

---

### Task 2: Replace Circle Tab with Beautiful Monthly Category Breakdown Screen
1. **Bottom Navigation Update**:
   - In [`BottomNavBar.kt`](app/src/main/java/com/kosh/app/ui/components/BottomNavBar.kt):
     * Rename Tab 4 from `"Circle"` to **`"Categories"`** (or **`"Analytics"`**).
     * Replace `Icons.Default.Favorite` with `Icons.Default.PieChart` or `Icons.Default.BarChart`.
   - In [`MainActivity.kt`](app/src/main/java/com/kosh/app/MainActivity.kt):
     * Replace `AppTab.FAMILY -> FamilyLedgerScreen(...)` with `AppTab.FAMILY -> CategoryAnalyticsScreen(...)`.
2. **Category Analytics Architecture (`CategoryAnalyticsScreen.kt`)**:
   - **Header & Month Comparison Controls**:
     * Current Selected Month (e.g., `OCTOBER 2026`) with total outflow.
     * Comparison Selector: Pill toggle / dropdown to compare against:
       - `vs Previous Month (September 2026)` (default)
       - `vs Any Custom Month` (interactive month picker modal)
     * Hero Spend Delta Card: Shows overall spend delta:
       - e.g. `₹32,450 vs ₹28,100` (`+15.4% higher ↗` in Coral Red or `-8.2% lower ↘` in Electric Mint).
   - **Category Breakdown Cards**:
     * Calculates total spent and % of monthly budget for every category:
       - 🏠 Rent & Maintenance
       - 🍳 Cook / Maid
       - 🚕 Cab / Auto
       - 🎁 Gifts & Celebrations
       - 👨‍👩‍👧 Family & Partner
       - 🛒 Groceries & Food
       - ⚡ Utilities & Bills
       - 🛍️ Shopping & Lifestyle
       - 📈 SIP / Wealth
       - 💳 Uncategorized (transactions with `OTHER` or blank category)
     * Visual Progress Bar filling according to proportion of monthly spend.
     * **Comparison Badge per Category**:
       - e.g. `Cab: ₹4,200 vs ₹3,100 (+35% ↗)`
       - e.g. `Groceries: ₹8,100 vs ₹9,500 (-14% ↘)`
     * Tapping any category card expands/opens transactions filtered to that category.
3. **ViewModel Additions in [`KoshViewModel.kt`](app/src/main/java/com/kosh/app/ui/viewmodel/KoshViewModel.kt)**:
   - Add `comparisonMonthOffset: MutableState<Int>` (e.g. -1 for previous month, or arbitrary offset).
   - Add state flows or helper functions:
     * `getTransactionsForMonthOffset(offset: Int)`
     * `getCategoryBreakdown(transactions: List<TransactionEntity>)`
     * `getCategoryComparison(current: List<TransactionEntity>, baseline: List<TransactionEntity>)`

---

### Task 3: Partner & Family Tagging & Reflection
1. **The Issue**:
   - Currently, selecting "Partner / Family" in `EditTransactionBottomSheet.kt` only sets the category string, but doesn't let the user specify *which* partner or family member it belongs to, nor does it tag the transaction title/note.
2. **The Solution**:
   - In [`EditTransactionBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/EditTransactionBottomSheet.kt):
     * When `TransactionCategory.PARTNER_FAMILY` is selected:
       - Render a dedicated "Tag Person / Family Member" selector (chips showing companions from `allPersons`, e.g. "Ananya", "Mom", "Dad", "Brother", "+ Add New").
       - Toggle option: `[ Partner ]` vs `[ Family ]`.
       - When saved, store the person's name in `note` or dedicated metadata, and show a distinct badge on the transaction row:
         * e.g. `🎁 Gift for Ananya` or `👨‍👩‍👧 Family Expense (Mom)`.
   - In [`DashboardScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/DashboardScreen.kt) & [`AllTransactionsScreen.kt`](app/src/main/java/com/kosh/app/ui/screens/AllTransactionsScreen.kt):
     * Update `TransactionRow`: when category is `PARTNER_FAMILY`, display the tag badge with `PartnerPink` / `ElectricMint`.

---

### Task 4: Add Standalone "Gift" Category
1. **Update [`Enums.kt`](app/src/main/java/com/kosh/app/data/model/Enums.kt)**:
   ```kotlin
   enum class TransactionCategory(val displayName: String, val icon: String) {
       RENT("Rent & Maintenance", "🏠"),
       COOK_MAID("Cook / Maid", "🍳"),
       CAB_COMMUTE("Cab / Auto", "🚕"),
       GIFTS("Gifts & Celebrations", "🎁"),
       PARTNER_FAMILY("Partner & Family", "👨‍👩‍👧"),
       SIP_WEALTH("SIP / Wealth", "📈"),
       GROCERIES("Groceries & Food", "🛒"),
       UTILITIES("Utilities & Bills", "⚡"),
       SHOPPING("Shopping & Lifestyle", "🛍️"),
       OTHER("Other / Uncategorized", "💳")
   }
   ```
2. **Update Receptors & Pickers**:
   - Add the `GIFTS` chip to [`EditTransactionBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/EditTransactionBottomSheet.kt) and [`AddExpenseBottomSheet.kt`](app/src/main/java/com/kosh/app/ui/components/AddExpenseBottomSheet.kt).
   - Update `SmsAutoScanner.kt` and `SmsBroadcastReceiver.kt`:
     * Keywords `gift`, `flower`, `fnp`, `igp`, `archies`, `cake`, `baker` categorize into `GIFTS`.

---

## 3. Step-by-Step Implementation Checklist for Next Session

- [ ] **Step 1: Remove Primary Instrument**
  - Delete `CARDS & ACCOUNTS` / `Primary Instrument` section from `DashboardScreen.kt`.
  - Remove `isPrimary` switches from `EditPaymentInstrumentBottomSheet.kt` and `AddPaymentInstrumentBottomSheet.kt`.
- [ ] **Step 2: Add Gift Category & Separate Partner/Family**
  - Update `TransactionCategory` enum in `Enums.kt` with `GIFTS("Gifts & Celebrations", "🎁")` and `PARTNER_FAMILY("Partner & Family", "👨‍👩‍👧")`.
  - Update `EditTransactionBottomSheet.kt` with person tagging chips for Partner/Family expenses.
- [ ] **Step 3: Build `CategoryAnalyticsScreen.kt`**
  - Create full-page analytics UI showing:
    * Total Monthly Outflow Hero
    * Month-over-month comparison selector (`vs Last Month` or custom picker)
    * Spend delta indicator (`+X.X% higher ↗` or `-X.X% lower ↘`)
    * Categorized breakdown cards with progress bars and amounts
    * Explicit "Uncategorized" section
    * Drill-down: Tapping a category displays its filtered transactions.
- [ ] **Step 4: Wire New Tab 4 in `BottomNavBar.kt` and `MainActivity.kt`**
  - Switch Tab 4 from Circle to Categories/Analytics.
- [ ] **Step 5: Compile, Test & Deploy to Vivo V2514**
  - Run `./gradlew assembleDebug`
  - Deploy to `10BFCT1GEK002PH` and verify live.
