package com.kosh.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kosh.app.data.local.KoshDatabase
import com.kosh.app.data.model.Currency
import com.kosh.app.data.model.DeletedTransactionHashEntity
import com.kosh.app.data.model.HouseholdObligationEntity
import com.kosh.app.data.model.PaymentInstrumentEntity
import com.kosh.app.data.model.PersonLedgerEntity
import com.kosh.app.data.model.RelationshipType
import com.kosh.app.data.model.SplitDebtEntity
import com.kosh.app.data.model.TransactionCategory
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.data.scanner.SmsAutoScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.NumberFormat
import java.util.Locale

class KoshRepository(
    val db: KoshDatabase,
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kosh_preferences", Context.MODE_PRIVATE)

    private val _currentCurrency = MutableStateFlow(loadSavedCurrency())
    val currentCurrency: StateFlow<Currency> = _currentCurrency.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, true))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    // Month time-range queries
    fun getTransactionsForMonth(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
        db.transactionDao().getTransactionsForMonthFlow(startTime, endTime)

    fun getTrueNetExpenseForMonth(startTime: Long, endTime: Long): Flow<Double?> =
        db.transactionDao().getTrueNetExpenseForMonthFlow(startTime, endTime)

    fun getGrossOutflowForMonth(startTime: Long, endTime: Long): Flow<Double?> =
        db.transactionDao().getGrossOutflowForMonthFlow(startTime, endTime)

    fun getWealthInvestmentForMonth(startTime: Long, endTime: Long): Flow<Double?> =
        db.transactionDao().getWealthInvestmentForMonthFlow(startTime, endTime)

    // Database flows
    val allTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactionsFlow()
    val recentTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getRecentTransactionsFlow(10)
    val openSplitDebts: Flow<List<SplitDebtEntity>> = db.splitDebtDao().getOpenSplitDebtsFlow()
    val openPersonDebts: Flow<List<SplitDebtEntity>> = db.splitDebtDao().getOpenPersonDebtsFlow()
    val openMerchantRefunds: Flow<List<SplitDebtEntity>> = db.splitDebtDao().getOpenMerchantRefundsFlow()
    val totalOpenReceivables: Flow<Double?> = db.splitDebtDao().getTotalOpenReceivablesFlow()
    val totalGrossOutflow: Flow<Double?> = db.transactionDao().getTotalGrossOutflowFlow()
    val totalTrueNetExpense: Flow<Double?> = db.transactionDao().getTotalTrueNetExpenseFlow()
    val totalWealthInvestments: Flow<Double?> = db.transactionDao().getTotalWealthInvestmentFlow()
    val householdObligations: Flow<List<HouseholdObligationEntity>> = db.householdObligationDao().getAllObligationsFlow()
    val paymentInstruments: Flow<List<PaymentInstrumentEntity>> = db.paymentInstrumentDao().getAllInstrumentsFlow()
    val personLedgers: Flow<List<PersonLedgerEntity>> = db.personLedgerDao().getAllPersonLedgersFlow()
    val partnerLedger: Flow<PersonLedgerEntity?> = db.personLedgerDao().getPartnerLedgerFlow()

    // Person Directory flows
    val allPersons: Flow<List<com.kosh.app.data.model.PersonEntity>> = db.personDao().getAllPersonsFlow()
    val pinnedPersons: Flow<List<com.kosh.app.data.model.PersonEntity>> = db.personDao().getPinnedPersonsFlow()
    val circleMember: Flow<com.kosh.app.data.model.PersonEntity?> = db.personDao().getCircleMemberFlow()
    val customCategories: Flow<List<com.kosh.app.data.model.CustomCategoryEntity>> = db.categoryDao().getAllCustomCategoriesFlow()

    // Currency management
    private fun loadSavedCurrency(): Currency {
        val code = prefs.getString(KEY_CURRENCY_CODE, Currency.INR.name) ?: Currency.INR.name
        return try {
            Currency.valueOf(code)
        } catch (e: Exception) {
            Currency.INR
        }
    }

    fun setCurrency(currency: Currency) {
        prefs.edit().putString(KEY_CURRENCY_CODE, currency.name).apply()
        _currentCurrency.value = currency
    }

    fun completeOnboarding(currency: Currency) {
        setCurrency(currency)
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        _onboardingCompleted.value = true
    }

    fun formatMoney(amount: Double): String {
        val curr = _currentCurrency.value
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 0
        val formattedNumber = formatter.format(amount)
        return "${curr.symbol}$formattedNumber"
    }

    // Add expense with optional split
    suspend fun addExpense(
        title: String,
        amount: Double,
        category: TransactionCategory,
        instrumentName: String,
        isSplit: Boolean = false,
        splitPersonName: String = "",
        splitAmountExpected: Double = 0.0,
        isAssetWealth: Boolean = false,
        note: String = ""
    ): Long = addExpense(
        title = title,
        amount = amount,
        categoryName = category.name,
        instrumentName = instrumentName,
        isSplit = isSplit,
        splitPersonName = splitPersonName,
        splitAmountExpected = splitAmountExpected,
        isAssetWealth = isAssetWealth,
        note = note
    )

    suspend fun addExpense(
        title: String,
        amount: Double,
        categoryName: String,
        instrumentName: String,
        isSplit: Boolean = false,
        splitPersonName: String = "",
        splitAmountExpected: Double = 0.0,
        isAssetWealth: Boolean = false,
        note: String = ""
    ): Long {
        val trueNetAmount = if (isAssetWealth) {
            0.0
        } else if (isSplit) {
            (amount - splitAmountExpected).coerceAtLeast(0.0)
        } else {
            amount
        }

        val reimbursable = if (isSplit) splitAmountExpected else 0.0

        val tx = TransactionEntity(
            title = title,
            grossAmount = amount,
            trueNetAmount = trueNetAmount,
            reimbursableAmount = reimbursable,
            category = categoryName,
            instrumentName = instrumentName,
            isAssetWealth = isAssetWealth,
            note = note,
            hasSplit = isSplit
        )
        val txId = db.transactionDao().insertTransaction(tx)

        if (isSplit && splitAmountExpected > 0) {
            val assignedName = if (splitPersonName.isNotBlank()) splitPersonName.trim() else "General Split"
            // Insert Split Debt
            db.splitDebtDao().insertSplitDebt(
                SplitDebtEntity(
                    parentTransactionId = txId,
                    personName = assignedName,
                    amount = splitAmountExpected,
                    itemDescription = title,
                    isSettled = false
                )
            )

            // Update Person Ledger
            val existingPerson = db.personLedgerDao().getPersonByName(assignedName)
            if (existingPerson != null) {
                db.personLedgerDao().updateBalance(
                    existingPerson.name,
                    existingPerson.runningBalance + splitAmountExpected
                )
            } else {
                db.personLedgerDao().insertPerson(
                    PersonLedgerEntity(
                        name = assignedName,
                        relationshipType = if (assignedName == "General Split") RelationshipType.OTHER.name else RelationshipType.FLATMATE.name,
                        avatarTag = if (assignedName == "General Split") "👥" else assignedName.take(2).uppercase(),
                        runningBalance = splitAmountExpected,
                        notes = "Added via expense split"
                    )
                )
            }
        }
        return txId
    }

    suspend fun addExpenseWithSplits(
        title: String,
        amount: Double,
        categoryName: String,
        instrumentName: String,
        isSplit: Boolean = false,
        participantSplits: List<ParticipantSplitShare> = emptyList(),
        unassignedSplitAmount: Double = 0.0,
        isAssetWealth: Boolean = false,
        note: String = ""
    ): Long {
        val totalSplit = if (isSplit) participantSplits.sumOf { it.shareAmount } + unassignedSplitAmount else 0.0
        val trueNetAmount = if (isAssetWealth) {
            0.0
        } else if (isSplit) {
            (amount - totalSplit).coerceAtLeast(0.0)
        } else {
            amount
        }

        val reimbursable = if (isSplit) totalSplit else 0.0

        val tx = TransactionEntity(
            title = title,
            grossAmount = amount,
            trueNetAmount = trueNetAmount,
            reimbursableAmount = reimbursable,
            category = categoryName,
            instrumentName = instrumentName,
            isAssetWealth = isAssetWealth,
            note = note,
            hasSplit = isSplit && totalSplit > 0
        )
        val txId = db.transactionDao().insertTransaction(tx)

        if (isSplit && totalSplit > 0) {
            for (p in participantSplits) {
                if (p.shareAmount <= 0.0) continue
                db.splitDebtDao().insertSplitDebt(
                    SplitDebtEntity(
                        parentTransactionId = txId,
                        personName = p.name.trim(),
                        amount = p.shareAmount,
                        itemDescription = title,
                        isSettled = false
                    )
                )

                val existingPerson = db.personLedgerDao().getPersonByName(p.name.trim())
                if (existingPerson != null) {
                    db.personLedgerDao().updateBalance(
                        existingPerson.name,
                        existingPerson.runningBalance + p.shareAmount
                    )
                } else {
                    db.personLedgerDao().insertPerson(
                        PersonLedgerEntity(
                            name = p.name.trim(),
                            relationshipType = RelationshipType.FLATMATE.name,
                            avatarTag = p.name.take(2).uppercase(),
                            runningBalance = p.shareAmount,
                            notes = "Added via expense split"
                        )
                    )
                }
            }

            if (unassignedSplitAmount > 0.0) {
                val generalName = "General Split"
                db.splitDebtDao().insertSplitDebt(
                    SplitDebtEntity(
                        parentTransactionId = txId,
                        personName = generalName,
                        amount = unassignedSplitAmount,
                        itemDescription = title,
                        isSettled = false
                    )
                )
                val existingGeneral = db.personLedgerDao().getPersonByName(generalName)
                if (existingGeneral != null) {
                    db.personLedgerDao().updateBalance(
                        generalName,
                        existingGeneral.runningBalance + unassignedSplitAmount
                    )
                } else {
                    db.personLedgerDao().insertPerson(
                        PersonLedgerEntity(
                            name = generalName,
                            relationshipType = RelationshipType.OTHER.name,
                            avatarTag = "👥",
                            runningBalance = unassignedSplitAmount,
                            notes = "Open split receivable to be settled later"
                        )
                    )
                }
            }
        }
        return txId
    }

    // Add Explicit IOU
    suspend fun addExplicitIou(
        personName: String,
        amount: Double,
        description: String,
        isMerchantRefund: Boolean = false
    ) {
        val trimmedName = personName.trim()
        db.splitDebtDao().insertSplitDebt(
            SplitDebtEntity(
                personName = trimmedName,
                amount = amount,
                itemDescription = description,
                isSettled = false,
                isMerchantRefund = isMerchantRefund,
                merchantName = if (isMerchantRefund) trimmedName else null
            )
        )

        val existingPerson = db.personLedgerDao().getPersonByName(trimmedName)
        if (existingPerson != null) {
            db.personLedgerDao().updateBalance(
                trimmedName,
                existingPerson.runningBalance + amount
            )
        } else {
            val relType = if (isMerchantRefund) RelationshipType.MERCHANT.name else RelationshipType.FRIEND.name
            val avatar = if (isMerchantRefund) "📦" else trimmedName.take(2).uppercase()
            db.personLedgerDao().insertPerson(
                PersonLedgerEntity(
                    name = trimmedName,
                    relationshipType = relType,
                    avatarTag = avatar,
                    runningBalance = amount,
                    notes = description
                )
            )
        }
    }

    // Smart Credit-to-Debit Reconciliation with Excess/Deficit Handling
    suspend fun reconcileCreditFromPerson(
        personName: String,
        amountReceived: Double
    ): ReconciliationResult {
        val trimmedName = personName.trim()
        val openDebts = db.splitDebtDao().getOpenDebtsForPerson(trimmedName)
        val totalOwed = openDebts.sumOf { it.amount }

        // Settle debts
        for (debt in openDebts) {
            db.splitDebtDao().settleDebt(debt.id)
        }

        // Update Person Ledger
        val person = db.personLedgerDao().getPersonByName(trimmedName)
        val excessAmount = amountReceived - totalOwed
        val newBalance = if (person != null) {
            person.runningBalance - amountReceived
        } else {
            -excessAmount
        }

        if (person != null) {
            db.personLedgerDao().updateBalance(trimmedName, newBalance)
        }

        // Also record the incoming credit transaction
        db.transactionDao().insertTransaction(
            TransactionEntity(
                title = "Credit from $trimmedName",
                grossAmount = amountReceived,
                trueNetAmount = 0.0,
                reimbursableAmount = 0.0,
                category = "INFLOW",
                instrumentName = "Bank Credit",
                note = "Matched & Reconciled split (₹${amountReceived.toInt()} received)"
            )
        )

        return ReconciliationResult(
            personName = trimmedName,
            settledAmount = totalOwed,
            excessOrDeficit = excessAmount,
            debtsSettledCount = openDebts.size
        )
    }

    // Person Directory Operations
    suspend fun insertPerson(
        name: String,
        tag: String = "Friend",
        phoneNumber: String? = null,
        isPinned: Boolean = false,
        avatarColor: Long = 0xFF10B981
    ): Long {
        val trimmedName = name.trim()
        val person = com.kosh.app.data.model.PersonEntity(
            name = trimmedName,
            tag = tag.trim(),
            phoneNumber = phoneNumber?.trim()?.ifBlank { null },
            isPinned = isPinned,
            avatarColor = avatarColor
        )
        val id = db.personDao().insertPerson(person)

        // Ensure also present in personLedgers
        val existingLedger = db.personLedgerDao().getPersonByName(trimmedName)
        if (existingLedger == null) {
            db.personLedgerDao().insertPerson(
                PersonLedgerEntity(
                    name = trimmedName,
                    relationshipType = tag.trim().uppercase().ifBlank { RelationshipType.FRIEND.name },
                    avatarTag = trimmedName.take(2).uppercase(),
                    runningBalance = 0.0,
                    notes = "$tag contact",
                    phoneNumber = phoneNumber?.trim()?.ifBlank { null }
                )
            )
        }
        return id
    }

    suspend fun togglePinPerson(personId: Long, isPinned: Boolean) {
        db.personDao().togglePin(personId, isPinned)
    }

    suspend fun setCircleMember(personId: Long) {
        db.personDao().setCircleMember(personId)
        val person = db.personDao().getPersonById(personId)
        if (person != null) {
            val existing = db.personLedgerDao().getPersonByName(person.name)
            if (existing != null) {
                db.personLedgerDao().updatePerson(
                    existing.copy(relationshipType = RelationshipType.PARTNER.name)
                )
            } else {
                db.personLedgerDao().insertPerson(
                    PersonLedgerEntity(
                        name = person.name,
                        relationshipType = RelationshipType.PARTNER.name,
                        avatarTag = "❤️",
                        runningBalance = 0.0,
                        notes = "Close Circle member"
                    )
                )
            }
        }
    }

    suspend fun clearCircleMember() {
        db.personDao().clearCircleMembers()
    }

    suspend fun updatePerson(person: com.kosh.app.data.model.PersonEntity) {
        db.personDao().updatePerson(person)
        val existingLedger = db.personLedgerDao().getPersonByName(person.name)
        if (existingLedger != null) {
            db.personLedgerDao().updatePerson(
                existingLedger.copy(
                    relationshipType = person.tag.trim().uppercase().ifBlank { existingLedger.relationshipType },
                    phoneNumber = person.phoneNumber
                )
            )
        }
    }

    suspend fun deletePerson(person: com.kosh.app.data.model.PersonEntity) {
        db.personDao().deletePerson(person)
        val existingLedger = db.personLedgerDao().getPersonByName(person.name)
        if (existingLedger != null) {
            db.personLedgerDao().deletePerson(existingLedger)
        }
    }

    suspend fun settleSpecificDebt(debtId: Long) {
        val debt = db.splitDebtDao().getDebtById(debtId) ?: return
        db.splitDebtDao().settleDebt(debtId)
        val person = db.personLedgerDao().getPersonByName(debt.personName)
        if (person != null) {
            val newBalance = (person.runningBalance - debt.amount).coerceAtLeast(0.0)
            db.personLedgerDao().updateBalance(debt.personName, newBalance)
        }
        db.transactionDao().insertTransaction(
            TransactionEntity(
                title = "Credit from ${debt.personName}",
                grossAmount = debt.amount,
                trueNetAmount = 0.0,
                reimbursableAmount = 0.0,
                category = "INFLOW",
                instrumentName = "Bank Credit",
                note = "Settled split: ${debt.itemDescription} (₹${debt.amount.toInt()})"
            )
        )
    }

    suspend fun recordPartialSettlement(
        personName: String,
        amountSettled: Double,
        linkedTransactionId: Long? = null,
        note: String = "Manual settlement"
    ) {
        val trimmed = personName.trim()
        val person = db.personLedgerDao().getPersonByName(trimmed) ?: return
        val newBalance = (person.runningBalance - amountSettled).coerceAtLeast(0.0)
        db.personLedgerDao().updateBalance(trimmed, newBalance)

        var remaining = amountSettled
        val openDebts = db.splitDebtDao().getOpenDebtsForPerson(trimmed).sortedBy { it.dateCreated }
        for (debt in openDebts) {
            if (remaining <= 0.0) break
            if (remaining >= debt.amount) {
                db.splitDebtDao().settleDebt(debt.id)
                remaining -= debt.amount
            } else {
                db.splitDebtDao().updateSplitDebt(debt.copy(amount = debt.amount - remaining))
                remaining = 0.0
            }
        }

        if (linkedTransactionId == null) {
            db.transactionDao().insertTransaction(
                TransactionEntity(
                    title = "Settlement from $trimmed",
                    grossAmount = amountSettled,
                    trueNetAmount = 0.0,
                    reimbursableAmount = 0.0,
                    category = "INFLOW",
                    instrumentName = "Cash / Direct",
                    note = "Received ${formatMoney(amountSettled)} ($note)"
                )
            )
        } else {
            val existingTx = db.transactionDao().getTransactionById(linkedTransactionId)
            if (existingTx != null) {
                val updatedNote = if (existingTx.note.isBlank()) {
                    "Settled ${formatMoney(amountSettled)} from $trimmed ($note)"
                } else {
                    "${existingTx.note} • Settled ${formatMoney(amountSettled)} from $trimmed"
                }
                db.transactionDao().updateTransaction(existingTx.copy(note = updatedNote))
            }
        }
    }

    // Custom Category Management
    suspend fun addCustomCategory(
        name: String,
        iconEmoji: String = "📁",
        colorHex: Long = 0xFF10B981,
        isWealthAsset: Boolean = false
    ): Long {
        return db.categoryDao().insertCategory(
            com.kosh.app.data.model.CustomCategoryEntity(
                name = name.trim(),
                iconEmoji = iconEmoji.ifBlank { "📁" },
                colorHex = colorHex,
                isWealthAsset = isWealthAsset
            )
        )
    }

    suspend fun deleteCustomCategory(category: com.kosh.app.data.model.CustomCategoryEntity) {
        db.categoryDao().deleteCategory(category)
    }

    // Flexible Household Pay & Split Flow (Solo, Equal, Custom)
    suspend fun payAndSplitHouseholdObligation(
        obligationId: Long,
        splitMode: String, // SOLO, EQUAL, CUSTOM
        participantSplits: List<ParticipantSplitShare> = emptyList(),
        instrumentName: String = "Bank Account"
    ) {
        val ob = db.householdObligationDao().getObligationById(obligationId) ?: return

        val totalAmount = ob.totalAmount
        val reimbursable = participantSplits.sumOf { it.shareAmount }
        val netPersonalBurn = (totalAmount - reimbursable).coerceAtLeast(0.0)

        // Insert Parent Transaction
        val txId = db.transactionDao().insertTransaction(
            TransactionEntity(
                title = ob.title,
                grossAmount = totalAmount,
                trueNetAmount = netPersonalBurn,
                reimbursableAmount = reimbursable,
                category = TransactionCategory.RENT.name,
                instrumentName = instrumentName,
                note = "Household: ${ob.title} ($splitMode split)",
                hasSplit = participantSplits.isNotEmpty()
            )
        )

        // Mark obligation as paid with the link to txId
        db.householdObligationDao().updatePaidDetails(
            id = obligationId,
            isPaid = true,
            paidDate = System.currentTimeMillis(),
            paidTxId = txId
        )

        // Split among participants
        for (p in participantSplits) {
            if (p.shareAmount > 0) {
                db.splitDebtDao().insertSplitDebt(
                    SplitDebtEntity(
                        parentTransactionId = txId,
                        personId = p.personId,
                        personName = p.name,
                        amount = p.shareAmount,
                        itemDescription = "${ob.title} Share",
                        isSettled = false
                    )
                )

                val person = db.personLedgerDao().getPersonByName(p.name)
                if (person != null) {
                    db.personLedgerDao().updateBalance(p.name, person.runningBalance + p.shareAmount)
                } else {
                    db.personLedgerDao().insertPerson(
                        PersonLedgerEntity(
                            name = p.name,
                            relationshipType = RelationshipType.FLATMATE.name,
                            avatarTag = p.name.take(2).uppercase(),
                            runningBalance = p.shareAmount,
                            notes = "Flatmate"
                        )
                    )
                }
            }
        }
    }

    // Bulletproof Household Undo Payment Flow
    suspend fun undoHouseholdPayment(obligationId: Long) {
        val ob = db.householdObligationDao().getObligationById(obligationId) ?: return
        val txId = ob.paidTransactionId

        if (txId != null) {
            // Find child split debts
            val debts = db.splitDebtDao().getDebtsByParentTransactionId(txId)
            for (debt in debts) {
                val ledger = db.personLedgerDao().getPersonByName(debt.personName)
                if (ledger != null) {
                    db.personLedgerDao().updateBalance(
                        debt.personName,
                        (ledger.runningBalance - debt.amount).coerceAtLeast(0.0)
                    )
                }
            }
            // Delete split debts & transaction
            db.splitDebtDao().deleteDebtsByParentTransactionId(txId)
            db.transactionDao().deleteById(txId)
        }

        // Reset obligation payment state
        db.householdObligationDao().resetPaymentStatus(obligationId)
    }

    // Household & Instrument Add / Delete Helpers
    suspend fun addHouseholdObligation(obligation: HouseholdObligationEntity): Long {
        return db.householdObligationDao().insertObligation(obligation)
    }

    suspend fun updateHouseholdObligation(obligation: HouseholdObligationEntity) {
        db.householdObligationDao().updateObligation(obligation)
    }

    suspend fun linkTransactionToHouseholdObligation(transactionId: Long, obligationId: Long) {
        db.householdObligationDao().updatePaidDetails(
            id = obligationId,
            isPaid = true,
            paidDate = System.currentTimeMillis(),
            paidTxId = transactionId
        )
    }

    suspend fun deleteHouseholdObligation(obligation: HouseholdObligationEntity) {
        db.householdObligationDao().deleteObligation(obligation)
    }

    suspend fun addPaymentInstrument(instrument: PaymentInstrumentEntity): Long {
        return db.paymentInstrumentDao().insertInstrument(instrument)
    }

    suspend fun deletePaymentInstrument(instrument: PaymentInstrumentEntity) {
        db.paymentInstrumentDao().deleteInstrument(instrument)
    }

    suspend fun updatePaymentInstrument(instrument: PaymentInstrumentEntity) {
        db.paymentInstrumentDao().updateInstrument(instrument)
    }

    // Transaction Deletion (with permanent tombstone hash to prevent re-import on SMS scan)
    suspend fun deleteTransaction(transaction: TransactionEntity) {
        val tombstones = mutableListOf<String>()

        transaction.smsRefHash?.let { if (it.isNotBlank()) tombstones.add(it) }

        val exactHash = SmsAutoScanner.generateHash(
            transaction.timestamp,
            transaction.grossAmount,
            transaction.title,
            transaction.instrumentName
        )
        if (exactHash.isNotBlank()) tombstones.add(exactHash)

        tombstones.add(
            SmsAutoScanner.generateSignature(
                transaction.timestamp,
                transaction.grossAmount,
                transaction.title
            )
        )
        tombstones.add(
            SmsAutoScanner.generateMinuteSignature(
                transaction.timestamp,
                transaction.grossAmount,
                transaction.title
            )
        )
        tombstones.add(
            SmsAutoScanner.generateDaySignature(
                transaction.timestamp,
                transaction.grossAmount,
                transaction.title
            )
        )

        for (tombstone in tombstones) {
            if (tombstone.isNotBlank()) {
                db.deletedTransactionHashDao().insertDeletedHash(DeletedTransactionHashEntity(tombstone))
            }
        }

        // Revert any split debts attached to this transaction
        val debts = db.splitDebtDao().getDebtsByParentTransactionId(transaction.id)
        for (debt in debts) {
            val ledger = db.personLedgerDao().getPersonByName(debt.personName)
            if (ledger != null) {
                db.personLedgerDao().updateBalance(
                    debt.personName,
                    (ledger.runningBalance - debt.amount).coerceAtLeast(0.0)
                )
            }
        }
        db.splitDebtDao().deleteDebtsByParentTransactionId(transaction.id)
        db.transactionDao().deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        val tx = db.transactionDao().getTransactionById(id) ?: return
        deleteTransaction(tx)
    }

    suspend fun updateTransaction(
        id: Long,
        newTitle: String,
        newAmount: Double,
        newCategory: String,
        isAssetWealth: Boolean,
        newNote: String,
        splitWithPersonName: String? = null,
        splitAmount: Double = 0.0
    ) {
        val existing = db.transactionDao().getTransactionById(id) ?: return

        val hasSplit = splitAmount > 0
        val targetPerson = if (splitWithPersonName.isNullOrBlank()) "General Split" else splitWithPersonName.trim()
        val reimbursable = if (hasSplit) splitAmount else 0.0
        val netAmount = if (isAssetWealth) 0.0 else (newAmount - reimbursable).coerceAtLeast(0.0)

        val updated = existing.copy(
            title = newTitle,
            grossAmount = newAmount,
            trueNetAmount = netAmount,
            reimbursableAmount = reimbursable,
            category = newCategory,
            isAssetWealth = isAssetWealth,
            note = newNote,
            hasSplit = hasSplit
        )
        db.transactionDao().updateTransaction(updated)

        // Clear previous debts for this parent transaction
        val oldDebts = db.splitDebtDao().getDebtsByParentTransactionId(id)
        for (d in oldDebts) {
            val ledger = db.personLedgerDao().getPersonByName(d.personName)
            if (ledger != null) {
                db.personLedgerDao().updateBalance(
                    d.personName,
                    (ledger.runningBalance - d.amount).coerceAtLeast(0.0)
                )
            }
        }
        db.splitDebtDao().deleteDebtsByParentTransactionId(id)

        // If split is configured, insert new debt
        if (hasSplit) {
            db.splitDebtDao().insertSplitDebt(
                SplitDebtEntity(
                    parentTransactionId = id,
                    personName = targetPerson,
                    amount = splitAmount,
                    itemDescription = "$newTitle (Split)"
                )
            )

            val ledger = db.personLedgerDao().getPersonByName(targetPerson)
            if (ledger != null) {
                db.personLedgerDao().updateBalance(
                    targetPerson,
                    ledger.runningBalance + splitAmount
                )
            } else {
                db.personLedgerDao().insertPerson(
                    PersonLedgerEntity(
                        name = targetPerson,
                        relationshipType = if (targetPerson == "General Split") "OTHER" else "FRIEND",
                        avatarTag = if (targetPerson == "General Split") "👥" else targetPerson.take(2).uppercase(),
                        runningBalance = splitAmount
                    )
                )
            }
        }
    }

    suspend fun updateTransactionWithSplits(
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
        val existing = db.transactionDao().getTransactionById(id) ?: return

        val totalSplit = if (isSplit) participantSplits.sumOf { it.shareAmount } + unassignedSplitAmount else 0.0
        val reimbursable = if (isSplit) totalSplit else 0.0
        val netAmount = if (isAssetWealth) 0.0 else (newAmount - reimbursable).coerceAtLeast(0.0)

        val updated = existing.copy(
            title = newTitle,
            grossAmount = newAmount,
            trueNetAmount = netAmount,
            reimbursableAmount = reimbursable,
            category = newCategory,
            isAssetWealth = isAssetWealth,
            note = newNote,
            hasSplit = isSplit && totalSplit > 0
        )
        db.transactionDao().updateTransaction(updated)

        // Clear existing split debts for this transaction
        val oldDebts = db.splitDebtDao().getDebtsByParentTransactionId(id)
        for (d in oldDebts) {
            val ledger = db.personLedgerDao().getPersonByName(d.personName)
            if (ledger != null) {
                db.personLedgerDao().updateBalance(
                    d.personName,
                    (ledger.runningBalance - d.amount).coerceAtLeast(0.0)
                )
            }
        }
        db.splitDebtDao().deleteDebtsByParentTransactionId(id)

        if (isSplit && totalSplit > 0) {
            for (p in participantSplits) {
                if (p.shareAmount <= 0.0) continue
                db.splitDebtDao().insertSplitDebt(
                    SplitDebtEntity(
                        parentTransactionId = id,
                        personName = p.name.trim(),
                        amount = p.shareAmount,
                        itemDescription = "$newTitle (Split)"
                    )
                )
                val ledger = db.personLedgerDao().getPersonByName(p.name.trim())
                if (ledger != null) {
                    db.personLedgerDao().updateBalance(p.name.trim(), ledger.runningBalance + p.shareAmount)
                } else {
                    db.personLedgerDao().insertPerson(
                        PersonLedgerEntity(
                            name = p.name.trim(),
                            relationshipType = "FRIEND",
                            avatarTag = p.name.take(2).uppercase(),
                            runningBalance = p.shareAmount
                        )
                    )
                }
            }

            if (unassignedSplitAmount > 0.0) {
                val generalName = "General Split"
                db.splitDebtDao().insertSplitDebt(
                    SplitDebtEntity(
                        parentTransactionId = id,
                        personName = generalName,
                        amount = unassignedSplitAmount,
                        itemDescription = "$newTitle (Split)"
                    )
                )
                val ledger = db.personLedgerDao().getPersonByName(generalName)
                if (ledger != null) {
                    db.personLedgerDao().updateBalance(generalName, ledger.runningBalance + unassignedSplitAmount)
                } else {
                    db.personLedgerDao().insertPerson(
                        PersonLedgerEntity(
                            name = generalName,
                            relationshipType = "OTHER",
                            avatarTag = "👥",
                            runningBalance = unassignedSplitAmount
                        )
                    )
                }
            }
        }
    }

    // One-tap cleanup routine to eliminate duplicate bloated transactions from past repeated scans
    suspend fun cleanupDuplicateTransactions(): Int {
        val allTransactions = db.transactionDao().getAllTransactionsList()
        var deletedCount = 0

        val grouped = allTransactions.groupBy { tx ->
            SmsAutoScanner.generateSignature(tx.timestamp, tx.grossAmount, tx.title)
        }

        for ((_, list) in grouped) {
            if (list.size > 1) {
                val sorted = list.sortedBy { it.id }
                val toKeep = sorted.first()
                val duplicates = sorted.drop(1)

                val keptHash = toKeep.smsRefHash ?: SmsAutoScanner.generateHash(
                    toKeep.timestamp,
                    toKeep.grossAmount,
                    toKeep.title,
                    toKeep.instrumentName
                )
                if (toKeep.smsRefHash.isNullOrBlank()) {
                    db.transactionDao().updateTransaction(toKeep.copy(smsRefHash = keptHash))
                }

                for (dup in duplicates) {
                    db.splitDebtDao().deleteDebtsByParentTransactionId(dup.id)
                    db.transactionDao().deleteTransaction(dup)
                    deletedCount++
                }
            } else {
                val single = list.first()
                if (single.smsRefHash.isNullOrBlank()) {
                    val hash = SmsAutoScanner.generateHash(
                        single.timestamp,
                        single.grossAmount,
                        single.title,
                        single.instrumentName
                    )
                    db.transactionDao().updateTransaction(single.copy(smsRefHash = hash))
                }
            }
        }
        return deletedCount
    }

    suspend fun insertTransaction(tx: TransactionEntity): Long {
        return db.transactionDao().insertTransaction(tx)
    }

    suspend fun cleanupCorruptedInstruments(): Int {
        val instruments = db.paymentInstrumentDao().getAllInstrumentsList()
        var count = 0
        for (inst in instruments) {
            if (inst.bankName.equals("Bank Credit", ignoreCase = true)) {
                db.paymentInstrumentDao().deleteInstrument(inst)
                count++
            } else if (inst.instrumentType == "CREDIT_CARD" && inst.cardOrAccountName.contains("Account", ignoreCase = true)) {
                db.paymentInstrumentDao().updateInstrument(
                    inst.copy(
                        instrumentType = "BANK_ACCOUNT",
                        creditLimit = 0.0
                    )
                )
                count++
            }
        }
        return count
    }

    companion object {
        private const val KEY_CURRENCY_CODE = "key_currency_code"
        private const val KEY_ONBOARDING_DONE = "key_onboarding_done"
    }
}

data class ParticipantSplitShare(
    val personId: Long? = null,
    val name: String,
    val shareAmount: Double
)

data class ReconciliationResult(
    val personName: String,
    val settledAmount: Double,
    val excessOrDeficit: Double,
    val debtsSettledCount: Int
)
