package com.kosh.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.data.model.Currency
import com.kosh.app.data.model.PaymentInstrumentEntity
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.data.model.displayAmount
import com.kosh.app.data.model.isCreditCardBillPayment
import com.kosh.app.data.model.isCreditOrInflow
import com.kosh.app.data.model.isUpiTransaction
import com.kosh.app.ui.components.CategoryIconTile
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBackground
import com.kosh.app.ui.theme.CardBackgroundElevated
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.CardBorderSubtle
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ElectricMintContainer
import com.kosh.app.ui.theme.KoshShapes
import com.kosh.app.ui.theme.ObsidianBg
import com.kosh.app.ui.theme.PartnerPink
import com.kosh.app.ui.theme.PurpleAccent
import com.kosh.app.ui.theme.RoyalBlue
import com.kosh.app.ui.theme.SubSurface
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary
import com.kosh.app.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CardPeriodFilter(val label: String) {
    CURRENT_CYCLE("Billing Cycle"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

fun getCardCycleDateRange(
    cycleStartDay: Int,
    cycleEndDay: Int,
    currentEpochMillis: Long = System.currentTimeMillis()
): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    cal.timeInMillis = currentEpochMillis
    val currentDay = cal.get(Calendar.DAY_OF_MONTH)

    val startCal = Calendar.getInstance()
    startCal.timeInMillis = currentEpochMillis
    startCal.set(Calendar.HOUR_OF_DAY, 0)
    startCal.set(Calendar.MINUTE, 0)
    startCal.set(Calendar.SECOND, 0)
    startCal.set(Calendar.MILLISECOND, 0)

    if (currentDay < cycleStartDay) {
        startCal.add(Calendar.MONTH, -1)
    }
    val maxDayInStartMonth = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    startCal.set(Calendar.DAY_OF_MONTH, cycleStartDay.coerceIn(1, maxDayInStartMonth))

    val endCal = Calendar.getInstance()
    endCal.timeInMillis = startCal.timeInMillis
    if (cycleEndDay < cycleStartDay) {
        endCal.add(Calendar.MONTH, 1)
    }
    val maxDayInEndMonth = endCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    endCal.set(Calendar.DAY_OF_MONTH, cycleEndDay.coerceIn(1, maxDayInEndMonth))
    endCal.set(Calendar.HOUR_OF_DAY, 23)
    endCal.set(Calendar.MINUTE, 59)
    endCal.set(Calendar.SECOND, 59)
    endCal.set(Calendar.MILLISECOND, 999)

    return Pair(startCal.timeInMillis, endCal.timeInMillis)
}

fun getBankBrandColors(bankName: String): Pair<Color, Color> {
    val upper = bankName.uppercase()
    return when {
        upper.contains("HDFC") -> Pair(Color(0xFF0D3366), Color(0xFF071933))
        upper.contains("SBI") -> Pair(Color(0xFF0A4F80), Color(0xFF04243B))
        upper.contains("ICICI") -> Pair(Color(0xFF8A1E24), Color(0xFF450D10))
        upper.contains("AXIS") -> Pair(Color(0xFF75103A), Color(0xFF38081C))
        upper.contains("KOTAK") -> Pair(Color(0xFF9E1318), Color(0xFF470709))
        upper.contains("ONECARD") -> Pair(Color(0xFF232A36), Color(0xFF101318))
        upper.contains("AMEX") || upper.contains("AMERICAN EXPRESS") -> Pair(Color(0xFF0058A3), Color(0xFF00294D))
        upper.contains("IDFC") -> Pair(Color(0xFF7A1417), Color(0xFF3D090B))
        upper.contains("RBL") -> Pair(Color(0xFF0D2E5C), Color(0xFF06152B))
        upper.contains("INDUS") -> Pair(Color(0xFF6B151A), Color(0xFF33090C))
        upper.contains("AU") -> Pair(Color(0xFF5E2777), Color(0xFF2C1038))
        else -> Pair(Color(0xFF192233), Color(0xFF0E131C))
    }
}

fun matchesCard(tx: TransactionEntity, card: PaymentInstrumentEntity): Boolean {
    if (card.last4Digits.isNotBlank()) {
        val last4 = card.last4Digits.trim()
        if (tx.instrumentName.contains(last4)) return true
        if (tx.originalSms != null && tx.originalSms.contains(last4)) return true
        if (tx.note.contains(last4)) return true
    }
    val cardName = card.cardOrAccountName.trim()
    val bank = card.bankName.trim()
    if (cardName.isNotBlank() && tx.instrumentName.contains(cardName, ignoreCase = true)) return true
    if (bank.isNotBlank() && tx.instrumentName.contains(bank, ignoreCase = true)) return true
    if (tx.originalSms != null && bank.isNotBlank() && tx.originalSms.contains(bank, ignoreCase = true) &&
        (tx.originalSms.contains("card", ignoreCase = true) || tx.instrumentName.contains("card", ignoreCase = true))
    ) return true
    return false
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardsScreen(
    currency: Currency,
    instruments: List<PaymentInstrumentEntity>,
    transactions: List<TransactionEntity> = emptyList(),
    onAddCard: () -> Unit = {},
    onEditCard: (PaymentInstrumentEntity) -> Unit = {},
    onTransactionClick: (TransactionEntity) -> Unit = {},
    onSyncCardsFromMessages: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val creditCards = remember(instruments) { instruments.filter { it.instrumentType == "CREDIT_CARD" } }
    val bankAccounts = remember(instruments) { instruments.filter { it.instrumentType != "CREDIT_CARD" } }

    var selectedSection by remember(creditCards, bankAccounts) {
        mutableStateOf(if (creditCards.isNotEmpty() || bankAccounts.isEmpty()) "CARDS" else "BANK")
    }

    var selectedCardId by remember(creditCards) {
        mutableStateOf(creditCards.firstOrNull()?.id ?: -1L)
    }

    var periodFilter by remember { mutableStateOf(CardPeriodFilter.CURRENT_CYCLE) }
    var searchQuery by remember { mutableStateOf("") }

    val selectedCard = creditCards.firstOrNull { it.id == selectedCardId } ?: creditCards.firstOrNull()

    val (cycleStart, cycleEnd) = remember(selectedCard?.cycleStartDay, selectedCard?.cycleEndDay) {
        if (selectedCard != null) {
            getCardCycleDateRange(selectedCard.cycleStartDay, selectedCard.cycleEndDay)
        } else Pair(0L, 0L)
    }

    val calMonth = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val monthStart = calMonth.timeInMillis
    val monthEnd = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    // All transactions belonging to this card
    val cardTransactionsAll = remember(selectedCard, transactions) {
        if (selectedCard == null) emptyList()
        else transactions.filter { tx ->
            !tx.isAssetWealth &&
            !tx.isCreditCardBillPayment() &&
            matchesCard(tx, selectedCard)
        }.sortedByDescending { it.timestamp }
    }

    // Transactions filtered by period
    val cardTransactions = remember(cardTransactionsAll, periodFilter, cycleStart, cycleEnd) {
        when (periodFilter) {
            CardPeriodFilter.CURRENT_CYCLE -> cardTransactionsAll.filter { it.timestamp in cycleStart..cycleEnd }
            CardPeriodFilter.THIS_MONTH -> cardTransactionsAll.filter { it.timestamp in monthStart..monthEnd }
            CardPeriodFilter.ALL_TIME -> cardTransactionsAll
        }
    }

    // Filtered by search query if any
    val displayedTransactions = remember(cardTransactions, searchQuery) {
        if (searchQuery.isBlank()) cardTransactions
        else cardTransactions.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.note.contains(searchQuery, ignoreCase = true)
        }
    }

    // Dynamic spend calculation
    val cycleSpend = remember(cardTransactionsAll, cycleStart, cycleEnd, selectedCard) {
        if (selectedCard == null) 0.0
        else {
            val sum = cardTransactionsAll
                .filter { it.timestamp in cycleStart..cycleEnd && !it.isCreditOrInflow() }
                .sumOf { it.trueNetAmount }
            if (sum > 0) sum else selectedCard.unbilledSpend
        }
    }

    val activePeriodSpend = remember(cardTransactions) {
        cardTransactions.filter { !it.isCreditOrInflow() }.sumOf { it.trueNetAmount }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PAYMENT INSTRUMENTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricMint,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Cards & Accounts",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Sync Messages Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF131D2D))
                            .border(1.dp, RoyalBlue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable { onSyncCardsFromMessages() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = RoyalBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sync",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RoyalBlue
                            )
                        }
                    }

                    // Add Card Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricMintContainer)
                            .border(1.dp, ElectricMint.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { onAddCard() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ Add",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricMint
                        )
                    }
                }
            }
        }

        // Section Tabs: [ 💳 Credit Cards ] [ 🏦 Bank & UPI ]
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackgroundElevated)
                    .border(0.5.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selectedSection == "CARDS") CardBackground else Color.Transparent)
                        .clickable { selectedSection = "CARDS" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💳 Credit Cards (${creditCards.size})",
                        fontSize = 12.sp,
                        fontWeight = if (selectedSection == "CARDS") FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedSection == "CARDS") ElectricMint else TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selectedSection == "BANK") CardBackground else Color.Transparent)
                        .clickable { selectedSection = "BANK" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏦 Bank & UPI (${bankAccounts.size})",
                        fontSize = 12.sp,
                        fontWeight = if (selectedSection == "BANK") FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedSection == "BANK") ElectricMint else TextSecondary
                    )
                }
            }
        }

        // CONTENT SECTION: CREDIT CARDS
        if (selectedSection == "CARDS") {
            if (creditCards.isEmpty()) {
                // Empty state for Credit Cards
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(CardBackground)
                            .border(1.dp, CardBorderSubtle, RoundedCornerShape(18.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(ElectricMintContainer, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = ElectricMint,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "No Credit Cards Detected Yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Kosh can automatically detect cards from your bank SMS messages and group all your transactions under their respective cards.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onSyncCardsFromMessages,
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricMint, contentColor = Color(0xFF042013)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("🔄 Detect from SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onAddCard,
                                    colors = ButtonDefaults.buttonColors(containerColor = CardBackgroundElevated, contentColor = TextPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+ Add Manually", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            } else {
                // Horizontal Card Selector Chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(creditCards) { card ->
                            val isSelected = selectedCard?.id == card.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) ElectricMintContainer else SubSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) ElectricMint else CardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCardId = card.id }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = card.bankName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) ElectricMint else TextSecondary
                                    )
                                    if (card.last4Digits.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "••${card.last4Digits}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) TextPrimary else TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        // "All Cards" option chip
                        item {
                            val isAllSelected = selectedCardId == -999L
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isAllSelected) ElectricMintContainer else SubSurface)
                                    .border(1.dp, if (isAllSelected) ElectricMint else CardBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedCardId = -999L }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "🗂️ All Cards",
                                    fontSize = 11.sp,
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAllSelected) ElectricMint else TextSecondary
                                )
                            }
                        }
                    }
                }

                // If specific card is selected
                if (selectedCard != null && selectedCardId != -999L) {
                    // Render Selected Hero Card
                    item {
                        CreditCardVisual(
                            currency = currency,
                            card = selectedCard,
                            spend = cycleSpend,
                            cycleStartEpoch = cycleStart,
                            cycleEndEpoch = cycleEnd,
                            onEdit = { onEditCard(selectedCard) }
                        )
                    }

                    // Period Filter Selector: [ Current Cycle ] [ This Month ] [ All Time ]
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardBackgroundElevated)
                                .border(0.5.dp, CardBorderSubtle, RoundedCornerShape(10.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CardPeriodFilter.values().forEach { filter ->
                                val isSelected = periodFilter == filter
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CardBackground else Color.Transparent)
                                        .clickable { periodFilter = filter }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = filter.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) ElectricMint else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Summary Row & Search
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TRANSACTIONS ON THIS CARD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${cardTransactions.size} transactions (${currency.symbol}${activePeriodSpend.toInt()} spend)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }

                            if (cardTransactionsAll.size > cardTransactions.size && periodFilter == CardPeriodFilter.CURRENT_CYCLE) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF131D2D))
                                        .clickable { periodFilter = CardPeriodFilter.ALL_TIME }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "View All ${cardTransactionsAll.size} Txns",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue
                                    )
                                }
                            }
                        }

                        if (cardTransactions.size > 4) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Filter transactions on this card...", fontSize = 12.sp, color = TextMuted) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricMint,
                                    unfocusedBorderColor = CardBorderSubtle,
                                    focusedContainerColor = CardBackgroundElevated,
                                    unfocusedContainerColor = CardBackgroundElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }

                    // Transactions List for this Card
                    if (displayedTransactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardBackground)
                                    .border(0.5.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (cardTransactionsAll.isEmpty())
                                            "No Transactions Linked to This Card"
                                        else
                                            "No Transactions in ${periodFilter.label}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (cardTransactionsAll.isNotEmpty() && periodFilter != CardPeriodFilter.ALL_TIME)
                                            "This card has ${cardTransactionsAll.size} transactions in total. Tap below to see all time transactions."
                                        else
                                            "Tap 'Sync' to match transactions from your messages, or log an expense on this card.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    if (cardTransactionsAll.isNotEmpty() && periodFilter != CardPeriodFilter.ALL_TIME) {
                                        Button(
                                            onClick = { periodFilter = CardPeriodFilter.ALL_TIME },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricMintContainer, contentColor = ElectricMint),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Show All Time (${cardTransactionsAll.size} Txns)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(displayedTransactions) { tx ->
                            CardTransactionItem(
                                currency = currency,
                                transaction = tx,
                                onClick = { onTransactionClick(tx) }
                            )
                        }
                    }
                } else if (selectedCardId == -999L) {
                    // ALL CARDS VIEW
                    item {
                        Text(
                            text = "ALL CREDIT CARDS SUMMARY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }

                    items(creditCards) { card ->
                        val (cycleStart, cycleEnd) = remember(card.cycleStartDay, card.cycleEndDay) {
                            getCardCycleDateRange(card.cycleStartDay, card.cycleEndDay)
                        }
                        val matched = remember(card, transactions) {
                            transactions.filter { matchesCard(it, card) && !it.isAssetWealth && !it.isCreditCardBillPayment() }
                        }
                        val spend = remember(matched) {
                            val sum = matched.filter { !it.isCreditOrInflow() }.sumOf { it.trueNetAmount }
                            if (sum > 0) sum else card.unbilledSpend
                        }

                        CreditCardVisual(
                            currency = currency,
                            card = card,
                            spend = spend,
                            cycleStartEpoch = cycleStart,
                            cycleEndEpoch = cycleEnd,
                            onEdit = { onEditCard(card) },
                            onSelect = { selectedCardId = card.id }
                        )
                    }
                }
            }
        }

        // CONTENT SECTION: BANK & UPI ACCOUNTS
        if (selectedSection == "BANK") {
            if (bankAccounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(CardBackground)
                            .border(1.dp, CardBorderSubtle, RoundedCornerShape(18.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No Bank or UPI Accounts Added",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Add your savings bank account or UPI handles to monitor account transfers and track inflows & outflows.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = onAddCard,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricMint, contentColor = Color(0xFF042013)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("+ Add Bank / UPI Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "BANK & UPI ACCOUNTS (${bankAccounts.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }

                items(bankAccounts) { bank ->
                    BankAccountItem(
                        currency = currency,
                        bank = bank,
                        transactions = transactions,
                        onClick = { onEditCard(bank) },
                        onTransactionClick = onTransactionClick
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
fun CreditCardVisual(
    currency: Currency,
    card: PaymentInstrumentEntity,
    spend: Double = card.unbilledSpend,
    cycleStartEpoch: Long = 0L,
    cycleEndEpoch: Long = 0L,
    onEdit: () -> Unit = {},
    onSelect: (() -> Unit)? = null
) {
    val (brandColor1, brandColor2) = remember(card.bankName) { getBankBrandColors(card.bankName) }
    val available = (card.creditLimit - spend).coerceAtLeast(0.0)
    val utilRatio = if (card.creditLimit > 0) (spend / card.creditLimit).toFloat() else 0.15f

    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val cycleRangeText = if (cycleStartEpoch > 0 && cycleEndEpoch > 0) {
        "${dateFormat.format(Date(cycleStartEpoch))} - ${dateFormat.format(Date(cycleEndEpoch))}"
    } else {
        "${card.cycleStartDay}th to ${card.cycleEndDay}th"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(brandColor1, brandColor2, Color(0xFF0D121B))
                )
            )
            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
            .clickable { onSelect?.invoke() ?: onEdit() }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Card Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = card.bankName.take(3).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = card.bankName.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricMint,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = card.cardOrAccountName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (card.badgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .border(0.5.dp, CardBorderSubtle, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = card.badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricMint
                            )
                        }
                    }

                    // Edit Pencil Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { onEdit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Card",
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Masked Card Number with Chip simulation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gold EMV Chip visual
                Box(
                    modifier = Modifier
                        .size(32.dp, 24.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFE5C07B), Color(0xFF9E782F))
                            )
                        )
                        .border(0.5.dp, Color(0xFFD4AF37), RoundedCornerShape(5.dp))
                )

                Text(
                    text = if (card.last4Digits.isNotBlank()) "••••  ••••  ••••  ${card.last4Digits}" else "••••  ••••  ••••  ••••",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 2.sp
                )
            }

            // Spend vs Limit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Current Billing Spend",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${currency.symbol}${spend.toInt()}",
                        style = Typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Limit: ${currency.symbol}${card.creditLimit.toInt()}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${currency.symbol}${available.toInt()} Available",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (available > 0) ElectricMint else AmberGold
                    )
                }
            }

            // Progress Bar & Billing Cycle Information
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cycle: $cycleRangeText",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Due: ${card.paymentDueDay}th",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    val barColor = when {
                        utilRatio > 0.7f -> Color(0xFFEF4444)
                        utilRatio > 0.3f -> AmberGold
                        else -> ElectricMint
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(utilRatio.coerceIn(0.04f, 1f))
                            .fillMaxSize()
                            .background(barColor)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val utilPercent = (utilRatio * 100).toInt()
                    val statusText = if (utilPercent <= 30) "Healthy Utilization (< 30%)" else "High Utilization (> 30%)"
                    Text(
                        text = "$utilPercent% limit used • $statusText",
                        fontSize = 10.sp,
                        color = if (utilPercent <= 30) ElectricMint else AmberGold
                    )
                }
            }
        }
    }
}

@Composable
fun CardTransactionItem(
    currency: Currency,
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    val isCredit = transaction.isCreditOrInflow()
    val amt = if (isCredit) transaction.displayAmount() else transaction.trueNetAmount
    val dateStr = remember(transaction.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(0.5.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconTile(
                    categoryKeyOrName = transaction.category,
                    size = 38.dp,
                    iconSize = 18.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    if (transaction.hasSplit || transaction.reimbursableAmount > 0) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AmberGold.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Split: ${currency.symbol}${transaction.reimbursableAmount.toInt()} receivable",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isCredit) "+" else "-"}${currency.symbol}${amt.toInt()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) ElectricMint else TextPrimary
                )
                Text(
                    text = transaction.category,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun BankAccountItem(
    currency: Currency,
    bank: PaymentInstrumentEntity,
    transactions: List<TransactionEntity> = emptyList(),
    onClick: () -> Unit = {},
    onTransactionClick: (TransactionEntity) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    val matchedTransactions = remember(bank, transactions) {
        transactions.filter { tx ->
            matchesCard(tx, bank)
        }.sortedByDescending { it.timestamp }
    }

    val totalOutflow = matchedTransactions
        .filter { !it.isAssetWealth && !it.isCreditOrInflow() }
        .sumOf { it.trueNetAmount }

    val totalInflow = matchedTransactions
        .filter { it.isCreditOrInflow() }
        .sumOf { it.displayAmount() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF111722))
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(RoyalBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bank.bankName.take(4).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = RoyalBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${bank.cardOrAccountName} •••• ${bank.last4Digits}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = bank.upiHandle ?: "Linked Account (${matchedTransactions.size} transactions)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "-${currency.symbol}${totalOutflow.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (totalInflow > 0) {
                        Text(
                            text = "+${currency.symbol}${totalInflow.toInt()} in",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricMint
                        )
                    } else {
                        Text(
                            text = "Total Outflow",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            if (matchedTransactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "Hide transactions" else "View ${matchedTransactions.size} transactions on this account",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RoyalBlue
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle",
                        tint = RoyalBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        matchedTransactions.take(15).forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0C111A))
                                    .clickable { onTransactionClick(tx) }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp)),
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                }
                                val isCredit = tx.isCreditOrInflow()
                                val amt = if (isCredit) tx.displayAmount() else tx.trueNetAmount
                                Text(
                                    text = "${if (isCredit) "+" else "-"}${currency.symbol}${amt.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCredit) ElectricMint else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
