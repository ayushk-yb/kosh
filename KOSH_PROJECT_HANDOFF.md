# KOSH (कोश) — Master Project Handoff & Architecture Plan

> **Project Name:** Kosh (कोश — *Treasury / Personal Reserves*)  
> **Workspace Directory:** `D:\Android Apps\Kosh`  
> **Target Platform:** Android (Native Jetpack Compose, Kotlin 2.1, AGP 8.7.3, compileSdk 35, minSdk 26)  
> **Tested Physical Device:** Vivo V2514 (`10BFCT1GEK002PH`), resolution 1260x2800  
> **Handoff Date:** October 26, 2026  

---

## 1. Executive Summary & Core Pivot

In the initial bootstrap phase, placeholder data inspired by the UI prototype (`app_mockup.html`) was pre-populated into the database (e.g., hardcoded names like *"Ananya"*, *"Rahul"*, and hardcoded instruments like *"SBI Cashback"*).

**User Directive:** The app must eliminate all hardcoded fictional defaults. Every person, card, split ratio, and household obligation must be real, user-configured, or dynamically discovered from actual SMS. The app must transition from a static mockup replica into a **fully configurable, personal financial system**.

---

## 2. Critical Flaws Identified & Required Solutions

### Flaw 1: Household Obligation "Paid" State Cannot Be Undone
* **Current Behavior:** Clicking "Pay & Split" permanently sets `isPaid = true` and creates split records; there is no way to undo an accidental click.
* **Required Solution:**
  1. Tapping an already-paid obligation should prompt: **"Mark as Unpaid (Undo)?"**
  2. Undoing must revert `isPaid` back to `false` and either safely delete or prompt to delete the associated split debt receivables created during the payment.
  3. Visual badge: Clear toggleable state `[✓ Paid on 24 Oct · Tap to Undo]` vs `[Pay & Split]`.

---

### Flaw 2: Rent / Household Split Hardcoded to 2-Way (50/50)
* **Current Behavior:** Paying rent automatically divides the amount by 2 (`amount / 2`), presuming a 50/50 split with a single roommate.
* **Required Solution:**
  1. **User-Defined Split Configuration per Obligation:**
     - **Mode 1: Solo (0% Split):** 100% personal burn (e.g., user pays entire rent/wifi alone, no reimbursement).
     - **Mode 2: Equal N-Way Split:** Divide equally among user + $N$ selected persons (e.g., 3 flatmates = 33.3% each).
     - **Mode 3: Custom Unequal Split:** Specify exact amounts or percentages for each flatmate.
  2. **Pay & Split Confirmation Sheet:** When clicking "Pay", a compact confirmation sheet pops up showing:
     - Total Amount
     - "Who is splitting this with you?" (Multi-select person picker with assigned shares)
     - Calculated Net Burn vs Total Receivables.
     - 1-tap "Confirm & Pay".
  3. Store split preference on the obligation entity so recurring monthly bills remember the user's split settings.

---

### Flaw 3: Ambiguous Persons & Contact Management ("Two Rahuls" Problem)
* **Current Behavior:** People are stored as raw strings (`personName = "Rahul"`). Two people named Rahul (e.g., flatmate vs coworker) collide and combine into a single ledger.
* **Required Solution:**
  1. **New `PersonEntity` Architecture:**
     ```kotlin
     @Entity(tableName = "persons")
     data class PersonEntity(
         @PrimaryKey(autoGenerate = true) val id: Long = 0,
         val name: String,                    // e.g. "Rahul"
         val tag: String = "",                // e.g. "Flatmate", "Office", "Landlord"
         val phoneNumber: String? = null,     // For WhatsApp nudge & UPI auto-reconciliation
         val isPinned: Boolean = false,       // Pinned chips at top of split dialogs
         val avatarColor: Long = 0xFF10B981   // Accent color for avatar icon
     )
     ```
  2. **Universal Person Picker Component:**
     - Displays **Pinned / Recent contacts** as quick-tap chips.
     - Searchable list showing `Name (Tag)` (e.g., *"Rahul (Flatmate)"* vs *"Rahul (Office)"*).
     - Inline `+ Add New Person` button (Name + optional Tag + optional Phone).
  3. All `split_debts`, `person_ledgers`, and transactions reference `personId: Long` rather than raw strings.

---

### Flaw 4: Hardcoded Close Circle ("Ananya" Hardcoding & Optional Circle)
* **Current Behavior:** The Circle tab assumes a partner named "Ananya" with pre-set transactions and running balances.
* **Required Solution:**
  1. **Zero Hardcoded Names:** Remove all fictitious names and seed data from the database.
  2. **Configurable / Optional Circle:**
     - If no one is assigned: Show a clean, aesthetic empty state:
       > *"Track shared outlays or discretionary gifts with your partner, spouse, or close companion."*  
       > `[ + Link or Add Circle Member ]`
     - User chooses any person from their `Person` list or creates a new one.
     - The user can rename, change, or remove the circle member at any time from settings/top bar.
  3. **Discretionary / Gift Support:** Retain the distinction that outlays can be pure gifts (100% personal burn) or running adjustments.

---

### Flaw 5: Pre-Populated Fake Cards & Receivables
* **Current Behavior:** The Room DB pre-populates dummy cards (*"HDFC Regalia Gold"*, *"SBI Cashback"*) and dummy IOUs (*"Aman"*, *"Rahul"*).
* **Required Solution:**
  1. **Clean Initial State:** Remove `prepopulateDatabase` hardcoded entities from `KoshDatabase.kt`.
  2. **Empty States with Clear Calls-to-Action:**
     - **Cards Tab:** *"No payment cards added. Add your credit or debit cards to monitor billing cycles and limit utilization."* `[ + Add Card ]`
     - **Pending In Tab:** *"No pending receivables. All settled!"* `[ + Add IOU / Split ]`
  3. **Add Card Dialog:**
     - Card Name / Nickname (e.g., "Sapphiro", "Millennia")
     - Bank Name
     - Last 4 digits (e.g., "4012")
     - Credit Limit (e.g., ₹2,50,000)
     - Billing Cycle Statement Date (Day of Month, 1–31)
     - Payment Due Date (Day of Month, 1–31)
  4. **Smart SMS Card Discovery:**
     - When scanning bank SMS, if an unrecognized card is found (e.g., *"HDFC Card ending 1234"*), show a suggestion pill: *"Detected HDFC Card ending 1234. Save to Cards?"*

---

### Flaw 6: Floating Action Button (FAB) Visual Clipping
* **Current Behavior:** The green Plus (+) button in the bottom navigation bar is clipped at the top (top curve sliced off flat).
* **Root Cause:**
  - In `BottomNavBar.kt`, the root container has:
    ```kotlin
    .height(84.dp)
    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ```
  - The FAB is placed inside that same Box with `offset(y = (-14).dp)`. Because the parent clips its content to its 84.dp bounds, any pixels above `y = 0` are clipped.
* **Required Solution:**
  - Restructure `BottomNavBar`:
    ```kotlin
    Box(
        modifier = modifier.fillMaxWidth() // No clip on outer container!
    ) {
        // 1. Clipped background bar container (height = 80.dp, aligned to BottomCenter)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(SubSurface.copy(alpha = 0.95f), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(...)
        ) { ... nav items ... }

        // 2. Unclipped FAB floating above the bar (aligned to TopCenter, outside clipped region)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(56.dp)
                .shadow(...)
                .background(ElectricMint, CircleShape)
                ...
        ) { ... }
    }
    ```

---

## 3. Database Schema Migration Plan

### New Entities to Add:
1. **`PersonEntity`** (`persons` table)
   - `id: Long` (PK)
   - `name: String`
   - `tag: String`
   - `phoneNumber: String?`
   - `isPinned: Boolean`
   - `avatarColor: Long`

### Modified Entities:
1. **`SplitDebtEntity`**:
   - Add `personId: Long` (Foreign Key to `persons.id`).
   - Retain `personName` as display cache, but query by `personId`.
2. **`HouseholdObligationEntity`**:
   - Add `splitMode: String` (`"SOLO"`, `"EQUAL"`, `"CUSTOM"`).
   - Add `participantIdsJson: String` (comma-separated or JSON list of person IDs).
   - Add `paidTransactionId: Long?` (links to the transaction generated when marked paid, allowing clean undo).
3. **`PersonLedgerEntity`**:
   - Add `personId: Long` (FK).
   - Remove hardcoded "Ananya" singleton assumption.
4. **`KoshDatabase`**:
   - Remove all mock pre-population in `KoshDatabaseCallback`.
   - On first install, database starts completely clean (0 dummy cards, 0 dummy debts, 0 dummy household bills).

---

## 4. Step-by-Step Execution Plan for Next Session

| Step | Component | Description |
|---|---|---|
| **1** | `BottomNavBar.kt` | Fix FAB clipping by separating the clipped background bar from the unclipped floating FAB container. Verify on device. |
| **2** | `PersonEntity.kt` & `PersonDao.kt` | Create `persons` Room table with CRUD, pinning, and tagging support. |
| **3** | `PersonPicker.kt` | Build reusable UI component with Pinned Chips, Searchable List, and "+ Add Person" dialog. |
| **4** | `KoshDatabase.kt` | Purge all hardcoded dummy data (`prepopulateDatabase`). Implement clean zero-state initialization. |
| **5** | `HouseholdObligations` | Add Undo functionality for paid bills. Add "Configure Split" sheet (Solo, Equal, Custom) when marking paid. |
| **6** | `CircleScreen.kt` | Implement clean empty state ("No Circle member linked"). Add Person selector to link/swap/unlink Circle member. |
| **7** | `CardsScreen.kt` | Implement "Add Card" bottom sheet and empty state. Add SMS card discovery prompt. |
| **8** | Device Build & Test | Deploy to Vivo V2514 (`10BFCT1GEK002PH`), verify UI on screen, test SMS import into clean database. |

---

## 5. Working Environment Reference
* **SDK Path:** `C:\Users\Ayush Kushwaha\AppData\Local\Android\Sdk`
* **ADB Command:** `& "C:\Users\Ayush Kushwaha\AppData\Local\Android\Sdk\platform-tools\adb.exe"`
* **Gradle Wrapper:** `./gradlew.bat`
* **Package Name:** `com.kosh.app` (`com.kosh.app.MainActivity`)
