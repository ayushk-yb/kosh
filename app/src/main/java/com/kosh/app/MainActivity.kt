package com.kosh.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.kosh.app.data.model.HouseholdObligationEntity
import com.kosh.app.data.model.PersonEntity
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.receiver.SmsCreditNotificationManager
import com.kosh.app.ui.components.AddExpenseBottomSheet
import com.kosh.app.ui.components.AddHouseholdObligationBottomSheet
import com.kosh.app.ui.components.AddIouBottomSheet
import com.kosh.app.ui.components.AddPaymentInstrumentBottomSheet
import com.kosh.app.ui.components.BottomNavBar
import com.kosh.app.ui.components.CurrencySelectorModal
import com.kosh.app.ui.components.EditHouseholdObligationBottomSheet
import com.kosh.app.ui.components.EditPaymentInstrumentBottomSheet
import com.kosh.app.ui.components.EditPersonBottomSheet
import com.kosh.app.ui.components.EditTransactionBottomSheet
import com.kosh.app.ui.components.MatchSplitBottomSheet
import com.kosh.app.ui.components.PayHouseholdBottomSheet
import com.kosh.app.ui.components.PersonPickerModal
import com.kosh.app.ui.components.SimulateInflowToast
import com.kosh.app.ui.components.UndoHouseholdPaymentDialog
import com.kosh.app.ui.screens.AllTransactionsScreen
import com.kosh.app.ui.screens.CardsScreen
import com.kosh.app.ui.screens.CategoryAnalyticsScreen
import com.kosh.app.ui.screens.DashboardScreen
import com.kosh.app.ui.screens.OnboardingScreen
import com.kosh.app.ui.screens.PendingInScreen
import com.kosh.app.ui.theme.KoshTheme
import com.kosh.app.ui.theme.ObsidianBg
import com.kosh.app.ui.viewmodel.AppTab
import com.kosh.app.ui.viewmodel.KoshViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private val viewModel: KoshViewModel by viewModels {
        KoshViewModel.Factory((application as KoshApplication).repository)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_SMS] == true) {
            viewModel.scanSmsInbox(this)
        }
    }

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { viewModel.importPersonFromContactUri(this, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()
        handleIntent(intent)

        setContent {
            KoshTheme {
                KoshApp(
                    viewModel = viewModel,
                    onLaunchContactPicker = { contactPickerLauncher.launch(null) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val sender = intent.getStringExtra(SmsCreditNotificationManager.EXTRA_SENDER_NAME)
        val amount = intent.getDoubleExtra(SmsCreditNotificationManager.EXTRA_AMOUNT, 0.0)
        if (!sender.isNullOrBlank() && amount > 0.0) {
            viewModel.triggerCreditSimulation(sender, amount)
        }

        val txId = intent.getLongExtra(SmsCreditNotificationManager.EXTRA_TRANSACTION_ID, -1L)
        if (txId > 0) {
            viewModel.selectTransactionForEdit(txId)
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_SMS)
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            // Already granted, auto-scan SMS inbox
            viewModel.scanSmsInbox(this)
        }
    }
}

@Composable
fun KoshApp(
    viewModel: KoshViewModel,
    onLaunchContactPicker: () -> Unit = {}
) {
    val context = LocalContext.current
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    val currency by viewModel.currentCurrency.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    // Monthwise data
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsState()
    val monthTitle by viewModel.monthFormattedTitle.collectAsState()
    val monthNetSpend by viewModel.monthNetSpend.collectAsState()
    val monthGrossSpend by viewModel.monthGrossSpend.collectAsState()
    val monthTransactions by viewModel.monthTransactions.collectAsState()
    val isScanningSms by viewModel.isScanningSms.collectAsState()

    // Comparison data for Category Analytics
    val comparisonYearMonth by viewModel.comparisonYearMonth.collectAsState()
    val comparisonMonthTitle by viewModel.comparisonMonthFormattedTitle.collectAsState()
    val comparisonTransactions by viewModel.comparisonTransactions.collectAsState()

    val totalReceivables by viewModel.totalOpenReceivables.collectAsState()

    val openDebts by viewModel.openSplitDebts.collectAsState()
    val householdObligations by viewModel.householdObligations.collectAsState()
    val paymentInstruments by viewModel.paymentInstruments.collectAsState()
    val personLedgers by viewModel.personLedgers.collectAsState()
    val partnerLedger by viewModel.partnerLedger.collectAsState()

    val allPersons by viewModel.allPersons.collectAsState()
    val pinnedPersons by viewModel.pinnedPersons.collectAsState()
    val circleMember by viewModel.circleMember.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()

    val showAddExpenseSheet by viewModel.showAddExpenseSheet.collectAsState()
    val showAddIouSheet by viewModel.showAddIouSheet.collectAsState()
    val showCurrencySelector by viewModel.showCurrencySelector.collectAsState()
    val inflowAlert by viewModel.inflowAlert.collectAsState()

    var payingObligation by remember { mutableStateOf<HouseholdObligationEntity?>(null) }
    var undoingObligation by remember { mutableStateOf<HouseholdObligationEntity?>(null) }
    var editingObligation by remember { mutableStateOf<HouseholdObligationEntity?>(null) }
    var editingPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var showMatchSplitSheet by remember { mutableStateOf(false) }
    var showAddHouseholdSheet by remember { mutableStateOf(false) }
    var showAddCardSheet by remember { mutableStateOf(false) }
    var showPersonPickerForCircle by remember { mutableStateOf(false) }
    var showPersonPickerForSplits by remember { mutableStateOf(false) }
    var showAllTransactions by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var editingInstrument by remember { mutableStateOf<com.kosh.app.data.model.PaymentInstrumentEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val intentTx by viewModel.intentTransactionToEdit.collectAsState()
    LaunchedEffect(intentTx) {
        val tx = intentTx
        if (tx != null) {
            editingTransaction = tx
            viewModel.clearIntentTransactionToEdit()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(currentTab) {
        showAllTransactions = false
    }

    if (!onboardingCompleted) {
        OnboardingScreen(
            initialCurrency = currency,
            onComplete = { selectedCurr ->
                viewModel.completeOnboarding(selectedCurr)
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                BottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = {
                        showAllTransactions = false
                        viewModel.switchTab(it)
                    },
                    onFabClick = { viewModel.showAddExpenseSheet.value = true }
                )
            },
            containerColor = ObsidianBg
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(ObsidianBg)
            ) {
                // Active Screen Content
                when (currentTab) {
                    AppTab.DASHBOARD -> DashboardScreen(
                        currency = currency,
                        monthFormattedTitle = monthTitle,
                        netSpend = monthNetSpend,
                        grossSpend = monthGrossSpend,
                        totalReceivables = totalReceivables,
                        householdObligations = householdObligations,
                        paymentInstruments = paymentInstruments,
                        transactions = monthTransactions,
                        isScanningSms = isScanningSms,
                        onPreviousMonth = { viewModel.goToPreviousMonth() },
                        onNextMonth = { viewModel.goToNextMonth() },
                        onScanSms = { viewModel.scanSmsInbox(context) },
                        onSimulateInflow = { viewModel.triggerCreditSimulation("Rahul", 12000.0) },
                        onOpenCurrencyPicker = { viewModel.showCurrencySelector.value = true },
                        onPayHousehold = { ob -> payingObligation = ob },
                        onQuickPay = { ob -> viewModel.payAndSplitHousehold(obligationId = ob.id, splitMode = "SOLO") },
                        onUndoHousehold = { ob -> undoingObligation = ob },
                        onEditHousehold = { ob -> editingObligation = ob },
                        onAddHousehold = { showAddHouseholdSheet = true },
                        onViewAllTransactions = { showAllTransactions = true },
                        onTransactionClick = { tx -> editingTransaction = tx }
                    )
                    AppTab.SPLITS -> PendingInScreen(
                        currency = currency,
                        totalReceivables = totalReceivables,
                        personLedgers = personLedgers,
                        openDebts = openDebts,
                        allPersons = allPersons,
                        recentTransactions = monthTransactions,
                        onOpenAddIou = { viewModel.showAddIouSheet.value = true },
                        onOpenAddPerson = { showPersonPickerForSplits = true },
                        onMatchSplit = { showMatchSplitSheet = true },
                        onSettlePerson = { personName -> viewModel.manualSettlePerson(personName) },
                        onRecordPartialSettlement = { personName, amount, linkedTxId, note ->
                            viewModel.recordPartialSettlement(personName, amount, linkedTxId, note)
                        },
                        onEditPerson = { person -> editingPerson = person },
                        onWhatsAppNudge = { _, _ -> }
                    )
                    AppTab.CARDS -> CardsScreen(
                        currency = currency,
                        instruments = paymentInstruments,
                        transactions = monthTransactions,
                        onAddCard = { showAddCardSheet = true },
                        onEditCard = { inst -> editingInstrument = inst }
                    )
                    AppTab.ANALYTICS -> CategoryAnalyticsScreen(
                        currency = currency,
                        selectedMonthTitle = monthTitle,
                        currentTransactions = monthTransactions,
                        comparisonMonthTitle = comparisonMonthTitle,
                        comparisonTransactions = comparisonTransactions,
                        selectedYearMonth = selectedYearMonth,
                        comparisonYearMonth = comparisonYearMonth,
                        customCategories = customCategories,
                        onPreviousMonth = { viewModel.goToPreviousMonth() },
                        onNextMonth = { viewModel.goToNextMonth() },
                        onSetComparisonToPreviousMonth = { viewModel.setComparisonToPreviousMonth() },
                        onSetComparisonToTwoMonthsAgo = { viewModel.setComparisonToTwoMonthsAgo() },
                        onSelectComparisonMonth = { ym -> viewModel.setComparisonYearMonth(ym) },
                        onTransactionClick = { tx -> editingTransaction = tx }
                    )
                }

                // Inflow Alert Notification Toast (Floating Banner)
                SimulateInflowToast(
                    alert = inflowAlert,
                    onMatchSplit = { viewModel.matchAndReconcileInflow() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Add Expense Bottom Sheet
                if (showAddExpenseSheet) {
                    AddExpenseBottomSheet(
                        currency = currency,
                        allPersons = allPersons,
                        customCategories = customCategories,
                        onDismiss = { viewModel.showAddExpenseSheet.value = false },
                        onSaveExpense = { title, amount, cat, inst, isSplit, person, splitAmt ->
                            viewModel.addExpense(title, amount, cat, inst, isSplit, person, splitAmt)
                        },
                        onSaveExpenseCustomCategory = { title, amount, catName, inst, isSplit, person, splitAmt, isWealth ->
                            viewModel.addExpense(title, amount, catName, inst, isSplit, person, splitAmt, isWealth)
                        },
                        onSaveExpenseWithSplits = { title, amount, catName, inst, isSplit, participants, unassignedSplit, isWealth ->
                            viewModel.addExpenseWithSplits(title, amount, catName, inst, isSplit, participants, unassignedSplit, isWealth)
                        },
                        onAddNewPerson = { name, tag, phone, isPinned ->
                            viewModel.addPerson(name, tag, phone, isPinned)
                        },
                        onTogglePin = { personId, isPinned ->
                            viewModel.togglePinPerson(personId, isPinned)
                        },
                        onPickContact = { onLaunchContactPicker() },
                        onCreateCustomCategory = { name, icon, colorHex, isWealth ->
                            viewModel.addCustomCategory(name, icon, colorHex, isWealth)
                        }
                    )
                }

                // Add Explicit IOU Bottom Sheet
                if (showAddIouSheet) {
                    AddIouBottomSheet(
                        currency = currency,
                        allPersons = allPersons,
                        onDismiss = { viewModel.showAddIouSheet.value = false },
                        onSaveIou = { person, amount, note, isRefund ->
                            viewModel.addExplicitIou(person, amount, note, isRefund)
                        }
                    )
                }

                // Pay Household Bottom Sheet
                payingObligation?.let { ob ->
                    PayHouseholdBottomSheet(
                        currency = currency,
                        obligation = ob,
                        allPersons = allPersons,
                        onDismiss = { payingObligation = null },
                        onConfirmPay = { splitMode, participants ->
                            viewModel.payAndSplitHousehold(
                                obligationId = ob.id,
                                splitMode = splitMode,
                                participantSplits = participants
                            )
                        },
                        onAddNewPerson = { name, tag, phone, isPinned ->
                            viewModel.addPerson(name, tag, phone, isPinned)
                        },
                        onTogglePin = { personId, isPinned ->
                            viewModel.togglePinPerson(personId, isPinned)
                        },
                        onPickContact = { onLaunchContactPicker() }
                    )
                }

                // Undo Household Dialog
                undoingObligation?.let { ob ->
                    UndoHouseholdPaymentDialog(
                        currency = currency,
                        obligation = ob,
                        onDismiss = { undoingObligation = null },
                        onConfirmUndo = {
                            viewModel.undoHouseholdPayment(ob.id)
                        }
                    )
                }

                // Add Household Bill Bottom Sheet
                if (showAddHouseholdSheet) {
                    AddHouseholdObligationBottomSheet(
                        currency = currency,
                        onDismiss = { showAddHouseholdSheet = false },
                        onSave = { title, cat, recip, amt, day, emoji ->
                            viewModel.addHouseholdObligation(title, cat, recip, amt, day, emoji)
                        }
                    )
                }

                // Add Card Bottom Sheet
                if (showAddCardSheet) {
                    AddPaymentInstrumentBottomSheet(
                        currency = currency,
                        onDismiss = { showAddCardSheet = false },
                        onSave = { bank, card, last4, isCredit, limit, cycle, due ->
                            viewModel.addPaymentInstrument(bank, card, last4, isCredit, limit, cycle, due)
                        }
                    )
                }

                // Person Picker for Circle
                if (showPersonPickerForCircle) {
                    PersonPickerModal(
                        title = "Select Circle Companion",
                        persons = allPersons,
                        selectedPersonIds = circleMember?.let { setOf(it.id) } ?: emptySet(),
                        isMultiSelect = false,
                        onPersonSelected = { person ->
                            viewModel.setCircleMember(person.id)
                        },
                        onAddNewPerson = { name, tag, phone, isPinned ->
                            viewModel.addPerson(name, tag, phone, isPinned)
                        },
                        onTogglePin = { personId, isPinned ->
                            viewModel.togglePinPerson(personId, isPinned)
                        },
                        onPickContact = { onLaunchContactPicker() },
                        onDismiss = { showPersonPickerForCircle = false }
                    )
                }

                // Person Picker for Splits (+ Person from header)
                if (showPersonPickerForSplits) {
                    PersonPickerModal(
                        title = "People & Contacts",
                        persons = allPersons,
                        selectedPersonIds = emptySet(),
                        isMultiSelect = false,
                        onPersonSelected = {
                            showPersonPickerForSplits = false
                        },
                        onAddNewPerson = { name, tag, phone, isPinned ->
                            viewModel.addPerson(name, tag, phone, isPinned)
                        },
                        onTogglePin = { personId, isPinned ->
                            viewModel.togglePinPerson(personId, isPinned)
                        },
                        onPickContact = { onLaunchContactPicker() },
                        onDismiss = { showPersonPickerForSplits = false }
                    )
                }

                // Currency Selector Dialog
                if (showCurrencySelector) {
                    CurrencySelectorModal(
                        currentCurrency = currency,
                        onSelectCurrency = { newCurr ->
                            viewModel.setCurrency(newCurr)
                        },
                        onDismiss = { viewModel.showCurrencySelector.value = false }
                    )
                }

                // Edit Household Obligation Bottom Sheet
                editingObligation?.let { ob ->
                    EditHouseholdObligationBottomSheet(
                        obligation = ob,
                        currency = currency,
                        onDismiss = { editingObligation = null },
                        onSave = { updated ->
                            viewModel.updateHouseholdObligation(updated)
                        },
                        onDelete = {
                            viewModel.deleteHouseholdObligation(ob)
                        }
                    )
                }

                // Edit Person Bottom Sheet
                editingPerson?.let { person ->
                    EditPersonBottomSheet(
                        person = person,
                        onDismiss = { editingPerson = null },
                        onSave = { updated ->
                            viewModel.updatePerson(updated)
                        },
                        onDelete = {
                            viewModel.deletePerson(person)
                        },
                        onPickContact = {
                            onLaunchContactPicker()
                        }
                    )
                }

                // Match Inflow to Open Split Bottom Sheet
                if (showMatchSplitSheet) {
                    MatchSplitBottomSheet(
                        currency = currency,
                        openDebts = openDebts,
                        onDismiss = { showMatchSplitSheet = false },
                        onSettleDebt = { debt ->
                            viewModel.settleSpecificDebt(debt.id)
                        }
                    )
                }

                // Edit Transaction Bottom Sheet
                editingTransaction?.let { tx ->
                    EditTransactionBottomSheet(
                        transaction = tx,
                        currency = currency,
                        allPersons = allPersons,
                        customCategories = customCategories,
                        householdObligations = householdObligations,
                        onDismiss = { editingTransaction = null },
                        onSave = { title, amt, cat, isAsset, note, splitPerson, splitAmt ->
                            viewModel.updateTransaction(
                                id = tx.id,
                                newTitle = title,
                                newAmount = amt,
                                newCategory = cat,
                                isAssetWealth = isAsset,
                                newNote = note,
                                splitWithPersonName = splitPerson,
                                splitAmount = splitAmt
                            )
                        },
                        onSaveWithSplits = { title, amt, cat, isAsset, note, isSplit, participants, unassignedSplit ->
                            viewModel.updateTransactionWithSplits(
                                id = tx.id,
                                newTitle = title,
                                newAmount = amt,
                                newCategory = cat,
                                isAssetWealth = isAsset,
                                newNote = note,
                                isSplit = isSplit,
                                participantSplits = participants,
                                unassignedSplitAmount = unassignedSplit
                            )
                        },
                        onLinkToBill = { obId ->
                            viewModel.linkTransactionToBill(tx.id, obId)
                        },
                        onDelete = {
                            viewModel.deleteTransaction(tx)
                        },
                        onAddNewPerson = { name, tag, phone, isPinned ->
                            viewModel.addPerson(name, tag, phone, isPinned)
                        },
                        onTogglePin = { personId, isPinned ->
                            viewModel.togglePinPerson(personId, isPinned)
                        },
                        onPickContact = { onLaunchContactPicker() },
                        onCreateCustomCategory = { name, icon, colorHex, isWealth ->
                            viewModel.addCustomCategory(name, icon, colorHex, isWealth)
                        }
                    )
                }

                // Edit Payment Instrument Bottom Sheet
                editingInstrument?.let { inst ->
                    EditPaymentInstrumentBottomSheet(
                        instrument = inst,
                        currency = currency,
                        onDismiss = { editingInstrument = null },
                        onSave = { bank, card, last4, isCredit, limit, cycle, due, upi ->
                            viewModel.updatePaymentInstrument(
                                id = inst.id,
                                bankName = bank,
                                cardName = card,
                                last4 = last4,
                                isCreditCard = isCredit,
                                creditLimit = limit,
                                cycleStartDay = cycle,
                                dueDay = due,
                                upiHandle = upi
                            )
                        },
                        onDelete = {
                            viewModel.deletePaymentInstrument(inst)
                        }
                    )
                }

                // All Transactions Full View
                if (showAllTransactions) {
                    AllTransactionsScreen(
                        currency = currency,
                        monthFormattedTitle = monthTitle,
                        transactions = monthTransactions,
                        onBack = { showAllTransactions = false },
                        onTransactionClick = { tx -> editingTransaction = tx }
                    )
                }
            }
        }
    }
}
