package com.kosh.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kosh.app.data.model.Currency
import com.kosh.app.data.model.HouseholdObligationEntity
import com.kosh.app.data.model.PaymentInstrumentEntity
import com.kosh.app.data.model.PersonLedgerEntity
import com.kosh.app.data.model.SplitDebtEntity
import com.kosh.app.data.model.TransactionCategory
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.data.repository.KoshRepository
import com.kosh.app.data.repository.ParticipantSplitShare
import com.kosh.app.data.scanner.SmsAutoScanner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

enum class AppTab {
    DASHBOARD,
    SPLITS,
    CARDS,
    ANALYTICS
}

data class CreditInflowAlert(
    val senderName: String,
    val amount: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class KoshViewModel(
    private val repository: KoshRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            val cleaned = repository.cleanupDuplicateTransactions()
            if (cleaned > 0) {
                emitMessage("Cleaned up $cleaned duplicate transactions!")
            }
        }
    }

    val currentCurrency: StateFlow<Currency> = repository.currentCurrency
    val onboardingCompleted: StateFlow<Boolean> = repository.onboardingCompleted

    val currentTab = MutableStateFlow(AppTab.DASHBOARD)

    // Monthwise data selection
    val selectedYearMonth = MutableStateFlow(YearMonth.now())

    val monthFormattedTitle: StateFlow<String> = selectedYearMonth.map { ym ->
        "${ym.month.name} ${ym.year}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "OCTOBER 2026")

    // Monthwise streams
    val monthTransactions: StateFlow<List<TransactionEntity>> = selectedYearMonth.flatMapLatest { ym ->
        val start = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repository.getTransactionsForMonth(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthNetSpend: StateFlow<Double> = selectedYearMonth.flatMapLatest { ym ->
        val start = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repository.getTrueNetExpenseForMonth(start, end).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthGrossSpend: StateFlow<Double> = selectedYearMonth.flatMapLatest { ym ->
        val start = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repository.getGrossOutflowForMonth(start, end).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Month comparison selection for Category Analytics
    val comparisonYearMonth = MutableStateFlow(YearMonth.now().minusMonths(1))

    val comparisonMonthFormattedTitle: StateFlow<String> = comparisonYearMonth.map { ym ->
        "${ym.month.name} ${ym.year}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SEPTEMBER 2026")

    val comparisonTransactions: StateFlow<List<TransactionEntity>> = comparisonYearMonth.flatMapLatest { ym ->
        val start = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repository.getTransactionsForMonth(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val comparisonNetSpend: StateFlow<Double> = selectedYearMonth.flatMapLatest { _ ->
        comparisonYearMonth.flatMapLatest { cym ->
            val start = cym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val end = cym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            repository.getTrueNetExpenseForMonth(start, end).map { it ?: 0.0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Data streams
    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openSplitDebts: StateFlow<List<SplitDebtEntity>> = repository.openSplitDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openPersonDebts: StateFlow<List<SplitDebtEntity>> = repository.openPersonDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openMerchantRefunds: StateFlow<List<SplitDebtEntity>> = repository.openMerchantRefunds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val householdObligations: StateFlow<List<HouseholdObligationEntity>> = repository.householdObligations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentInstruments: StateFlow<List<PaymentInstrumentEntity>> = repository.paymentInstruments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personLedgers: StateFlow<List<PersonLedgerEntity>> = repository.personLedgers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partnerLedger: StateFlow<PersonLedgerEntity?> = repository.partnerLedger
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Person Directory streams
    val allPersons: StateFlow<List<com.kosh.app.data.model.PersonEntity>> = repository.allPersons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedPersons: StateFlow<List<com.kosh.app.data.model.PersonEntity>> = repository.pinnedPersons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val circleMember: StateFlow<com.kosh.app.data.model.PersonEntity?> = repository.circleMember
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val customCategories: StateFlow<List<com.kosh.app.data.model.CustomCategoryEntity>> = repository.customCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated metrics (Lifetime / Overall)
    val totalOpenReceivables: StateFlow<Double> = repository.totalOpenReceivables
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // UI Interactive States
    val showAddExpenseSheet = MutableStateFlow(false)
    val showAddIouSheet = MutableStateFlow(false)
    val showCurrencySelector = MutableStateFlow(false)
    val isScanningSms = MutableStateFlow(false)

    private val _inflowAlert = MutableStateFlow<CreditInflowAlert?>(null)
    val inflowAlert: StateFlow<CreditInflowAlert?> = _inflowAlert.asStateFlow()

    private val _intentTransactionToEdit = MutableStateFlow<TransactionEntity?>(null)
    val intentTransactionToEdit: StateFlow<TransactionEntity?> = _intentTransactionToEdit.asStateFlow()

    fun selectTransactionForEdit(id: Long) {
        viewModelScope.launch {
            val tx = repository.db.transactionDao().getTransactionById(id)
            _intentTransactionToEdit.value = tx
        }
    }

    fun clearIntentTransactionToEdit() {
        _intentTransactionToEdit.value = null
    }

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun switchTab(tab: AppTab) {
        currentTab.value = tab
    }

    fun goToPreviousMonth() {
        val newYm = selectedYearMonth.value.minusMonths(1)
        selectedYearMonth.value = newYm
        comparisonYearMonth.value = newYm.minusMonths(1)
    }

    fun goToNextMonth() {
        val newYm = selectedYearMonth.value.plusMonths(1)
        selectedYearMonth.value = newYm
        comparisonYearMonth.value = newYm.minusMonths(1)
    }

    fun setSelectedYearMonth(ym: YearMonth) {
        selectedYearMonth.value = ym
        comparisonYearMonth.value = ym.minusMonths(1)
    }

    fun setComparisonYearMonth(ym: YearMonth) {
        comparisonYearMonth.value = ym
    }

    fun setComparisonToPreviousMonth() {
        comparisonYearMonth.value = selectedYearMonth.value.minusMonths(1)
    }

    fun setComparisonToTwoMonthsAgo() {
        comparisonYearMonth.value = selectedYearMonth.value.minusMonths(2)
    }

    fun setCurrency(currency: Currency) {
        repository.setCurrency(currency)
        emitMessage("Base currency switched to ${currency.symbol} (${currency.code})")
    }

    fun completeOnboarding(currency: Currency) {
        repository.completeOnboarding(currency)
    }

    fun formatMoney(amount: Double): String {
        return repository.formatMoney(amount)
    }

    // Inflow Simulation & Reconciliation
    fun triggerCreditSimulation(sender: String = "Rahul", amount: Double = 12000.0) {
        _inflowAlert.value = CreditInflowAlert(sender, amount)
    }

    fun dismissInflowAlert() {
        _inflowAlert.value = null
    }

    fun matchAndReconcileInflow() {
        val alert = _inflowAlert.value ?: return
        dismissInflowAlert()
        viewModelScope.launch {
            val result = repository.reconcileCreditFromPerson(alert.senderName, alert.amount)
            emitMessage("🎉 Matched! ₹${result.settledAmount.toInt()} received from ${result.personName} settled.")
        }
    }

    fun scanSmsInbox(context: Context, forceFullScan: Boolean = true) {
        viewModelScope.launch {
            isScanningSms.value = true
            val result = SmsAutoScanner.scanInbox(context, repository.db, forceFullScan)
            isScanningSms.value = false
            if (result.importedTransactions > 0 || result.detectedCredits > 0) {
                emitMessage("📥 Imported ${result.importedTransactions} new transactions (${result.detectedCredits} credits detected).")
            } else if (forceFullScan) {
                emitMessage("Inbox up to date. No new transactions found.")
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            emitMessage("🗑️ Transaction '${transaction.title}' deleted permanently.")
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
            emitMessage("🗑️ Transaction deleted permanently.")
        }
    }

    fun updateTransaction(
        id: Long,
        newTitle: String,
        newAmount: Double,
        newCategory: String,
        isAssetWealth: Boolean,
        newNote: String,
        splitWithPersonName: String? = null,
        splitAmount: Double = 0.0
    ) {
        viewModelScope.launch {
            repository.updateTransaction(
                id,
                newTitle,
                newAmount,
                newCategory,
                isAssetWealth,
                newNote,
                splitWithPersonName,
                splitAmount
            )
            emitMessage("Transaction updated successfully.")
        }
    }

    fun updateTransactionWithSplits(
        id: Long,
        newTitle: String,
        newAmount: Double,
        newCategory: String,
        isAssetWealth: Boolean,
        newNote: String,
        isSplit: Boolean = false,
        participantSplits: List<ParticipantSplitShare> = emptyList(),
        unassignedSplitAmount: Double = 0.0
    ) {
        viewModelScope.launch {
            repository.updateTransactionWithSplits(
                id = id,
                newTitle = newTitle,
                newAmount = newAmount,
                newCategory = newCategory,
                isAssetWealth = isAssetWealth,
                newNote = newNote,
                isSplit = isSplit,
                participantSplits = participantSplits,
                unassignedSplitAmount = unassignedSplitAmount
            )
            emitMessage("Transaction updated successfully.")
        }
    }

    fun cleanupDuplicateTransactions() {
        viewModelScope.launch {
            val count = repository.cleanupDuplicateTransactions()
            if (count > 0) {
                emitMessage("✨ Cleaned up $count duplicate transactions! Totals updated.")
            } else {
                emitMessage("✅ No duplicate transactions found. All clean!")
            }
        }
    }

    fun manualSettlePerson(personName: String) {
        viewModelScope.launch {
            val person = personLedgers.value.find { it.name == personName }
            val amount = person?.runningBalance ?: 0.0
            val result = repository.reconcileCreditFromPerson(personName, amount)
            emitMessage("${result.personName} marked as settled manually.")
        }
    }

    fun recordPartialSettlement(
        personName: String,
        amount: Double,
        linkedTxId: Long? = null,
        note: String = "Manual settlement"
    ) {
        viewModelScope.launch {
            repository.recordPartialSettlement(personName, amount, linkedTxId, note)
            emitMessage("Settlement of ${formatMoney(amount)} for $personName recorded!")
        }
    }

    // Custom Category Management
    fun addCustomCategory(
        name: String,
        iconEmoji: String = "📁",
        colorHex: Long = 0xFF10B981,
        isWealthAsset: Boolean = false
    ) {
        viewModelScope.launch {
            repository.addCustomCategory(name, iconEmoji, colorHex, isWealthAsset)
            emitMessage("Category '$name' created successfully!")
        }
    }

    fun deleteCustomCategory(category: com.kosh.app.data.model.CustomCategoryEntity) {
        viewModelScope.launch {
            repository.deleteCustomCategory(category)
            emitMessage("Category '${category.name}' removed.")
        }
    }

    // Person Directory Methods
    fun addPerson(
        name: String,
        tag: String = "Friend",
        phoneNumber: String? = null,
        isPinned: Boolean = false
    ) {
        viewModelScope.launch {
            repository.insertPerson(name, tag, phoneNumber, isPinned)
            emitMessage("Added $name ($tag) to contacts.")
        }
    }

    fun togglePinPerson(personId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePinPerson(personId, isPinned)
        }
    }

    fun setCircleMember(personId: Long) {
        viewModelScope.launch {
            repository.setCircleMember(personId)
            emitMessage("Close Circle member linked!")
        }
    }

    fun clearCircleMember() {
        viewModelScope.launch {
            repository.clearCircleMember()
            emitMessage("Circle member unlinked.")
        }
    }

    fun updatePerson(person: com.kosh.app.data.model.PersonEntity) {
        viewModelScope.launch {
            repository.updatePerson(person)
            emitMessage("Updated ${person.name}.")
        }
    }

    fun deletePerson(person: com.kosh.app.data.model.PersonEntity) {
        viewModelScope.launch {
            repository.deletePerson(person)
            emitMessage("Removed ${person.name}.")
        }
    }

    fun importPersonFromContactUri(context: Context, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var name: String? = null
            var phone: String? = null
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        android.provider.ContactsContract.Contacts._ID,
                        android.provider.ContactsContract.Contacts.DISPLAY_NAME,
                        android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                        if (nameIdx >= 0) name = cursor.getString(nameIdx)
                        val idIdx = cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                        val contactId = if (idIdx >= 0) cursor.getString(idIdx) else null
                        val hasPhoneIdx = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        val hasPhone = if (hasPhoneIdx >= 0) cursor.getInt(hasPhoneIdx) > 0 else false

                        if (hasPhone && contactId != null) {
                            context.contentResolver.query(
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                                "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )?.use { phoneCursor ->
                                if (phoneCursor.moveToFirst()) {
                                    val pIdx = phoneCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (pIdx >= 0) phone = phoneCursor.getString(pIdx)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore query error
            }

            if (!name.isNullOrBlank()) {
                repository.insertPerson(
                    name = name!!,
                    tag = "Friend",
                    phoneNumber = phone,
                    isPinned = true
                )
                emitMessage("📱 Linked $name from Contacts")
            }
        }
    }

    fun settleSpecificDebt(debtId: Long) {
        viewModelScope.launch {
            repository.settleSpecificDebt(debtId)
            emitMessage("Split settled and balance updated.")
        }
    }

    // Household Management
    fun payAndSplitHousehold(
        obligationId: Long,
        splitMode: String = "SOLO",
        participantSplits: List<com.kosh.app.data.repository.ParticipantSplitShare> = emptyList(),
        instrumentName: String = "Bank Account"
    ) {
        viewModelScope.launch {
            repository.payAndSplitHouseholdObligation(obligationId, splitMode, participantSplits, instrumentName)
            emitMessage("Marked obligation as paid! Splits recorded cleanly.")
        }
    }

    fun undoHouseholdPayment(obligationId: Long) {
        viewModelScope.launch {
            repository.undoHouseholdPayment(obligationId)
            emitMessage("Payment undone. Obligation marked unpaid and split debts reverted.")
        }
    }

    fun addHouseholdObligation(
        title: String,
        category: String,
        recipient: String,
        totalAmount: Double,
        dueDay: Int,
        emoji: String = "🏠"
    ) {
        viewModelScope.launch {
            val ob = HouseholdObligationEntity(
                title = title,
                category = category,
                recipientName = recipient,
                totalAmount = totalAmount,
                dueDayOfMonth = dueDay,
                myShareAmount = totalAmount,
                iconEmoji = emoji
            )
            repository.addHouseholdObligation(ob)
            emitMessage("Household obligation '$title' added.")
        }
    }

    fun updateHouseholdObligation(obligation: HouseholdObligationEntity) {
        viewModelScope.launch {
            repository.updateHouseholdObligation(obligation)
            emitMessage("Fixed expense '${obligation.title}' updated.")
        }
    }

    fun linkTransactionToBill(transactionId: Long, obligationId: Long) {
        viewModelScope.launch {
            repository.linkTransactionToHouseholdObligation(transactionId, obligationId)
            emitMessage("Linked transaction to monthly expense.")
        }
    }

    fun deleteHouseholdObligation(obligation: HouseholdObligationEntity) {
        viewModelScope.launch {
            repository.deleteHouseholdObligation(obligation)
            emitMessage("Obligation '${obligation.title}' deleted.")
        }
    }

    // Payment Instrument Management
    fun addPaymentInstrument(
        bankName: String,
        cardName: String,
        last4: String,
        isCreditCard: Boolean,
        creditLimit: Double = 0.0,
        cycleStartDay: Int = 1,
        dueDay: Int = 20
    ) {
        viewModelScope.launch {
            val instrument = PaymentInstrumentEntity(
                bankName = bankName.trim(),
                cardOrAccountName = cardName.trim(),
                last4Digits = last4.trim(),
                instrumentType = if (isCreditCard) "CREDIT_CARD" else "BANK_ACCOUNT",
                creditLimit = if (isCreditCard) creditLimit else 0.0,
                cycleStartDay = cycleStartDay,
                paymentDueDay = dueDay,
                isPrimary = false,
                badgeText = if (isCreditCard) "Card" else "Bank"
            )
            repository.addPaymentInstrument(instrument)
            emitMessage("Added ${bankName.trim()} ${cardName.trim()} to instruments.")
        }
    }

    fun deletePaymentInstrument(instrument: PaymentInstrumentEntity) {
        viewModelScope.launch {
            repository.deletePaymentInstrument(instrument)
            emitMessage("Removed instrument.")
        }
    }

    fun updatePaymentInstrument(
        id: Long,
        bankName: String,
        cardName: String,
        last4: String,
        isCreditCard: Boolean,
        creditLimit: Double,
        cycleStartDay: Int,
        dueDay: Int,
        upiHandle: String?
    ) {
        viewModelScope.launch {
            val existing = repository.db.paymentInstrumentDao().getInstrumentById(id) ?: return@launch
            val updated = existing.copy(
                bankName = bankName.trim(),
                cardOrAccountName = cardName.trim(),
                last4Digits = last4.trim(),
                instrumentType = if (isCreditCard) "CREDIT_CARD" else "BANK_ACCOUNT",
                creditLimit = if (isCreditCard) creditLimit else 0.0,
                cycleStartDay = cycleStartDay,
                paymentDueDay = dueDay,
                isPrimary = false,
                upiHandle = upiHandle?.trim()?.ifEmpty { null }
            )
            repository.updatePaymentInstrument(updated)
            emitMessage("Updated ${bankName.trim()} ${cardName.trim()}.")
        }
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: TransactionCategory,
        instrumentName: String,
        isSplit: Boolean,
        splitPersonName: String,
        splitAmount: Double
    ) {
        addExpense(
            title = title,
            amount = amount,
            categoryName = category.name,
            instrumentName = instrumentName,
            isSplit = isSplit,
            splitPersonName = splitPersonName,
            splitAmount = splitAmount
        )
    }

    fun addExpense(
        title: String,
        amount: Double,
        categoryName: String,
        instrumentName: String,
        isSplit: Boolean,
        splitPersonName: String,
        splitAmount: Double,
        isAssetWealth: Boolean = false
    ) {
        viewModelScope.launch {
            repository.addExpense(
                title = title,
                amount = amount,
                categoryName = categoryName,
                instrumentName = instrumentName,
                isSplit = isSplit,
                splitPersonName = splitPersonName,
                splitAmountExpected = splitAmount,
                isAssetWealth = isAssetWealth
            )
            showAddExpenseSheet.value = false
            val target = if (splitPersonName.isBlank()) "General Split" else splitPersonName
            emitMessage("Expense logged! Net spend updated, reimbursable added to $target.")
        }
    }

    fun addExpenseWithSplits(
        title: String,
        amount: Double,
        categoryName: String,
        instrumentName: String,
        isSplit: Boolean,
        participantSplits: List<ParticipantSplitShare> = emptyList(),
        unassignedSplitAmount: Double = 0.0,
        isAssetWealth: Boolean = false,
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.addExpenseWithSplits(
                title = title,
                amount = amount,
                categoryName = categoryName,
                instrumentName = instrumentName,
                isSplit = isSplit,
                participantSplits = participantSplits,
                unassignedSplitAmount = unassignedSplitAmount,
                isAssetWealth = isAssetWealth,
                note = note
            )
            showAddExpenseSheet.value = false
            emitMessage("Expense logged! Net spend and split balances updated.")
        }
    }

    fun addExplicitIou(personName: String, amount: Double, note: String, isRefund: Boolean) {
        viewModelScope.launch {
            repository.addExplicitIou(personName, amount, note, isRefund)
            showAddIouSheet.value = false
            emitMessage("IOU added: ${formatMoney(amount)} expected from $personName.")
        }
    }

    private fun emitMessage(msg: String) {
        viewModelScope.launch {
            _userMessage.emit(msg)
        }
    }

    class Factory(private val repository: KoshRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return KoshViewModel(repository) as T
        }
    }
}
