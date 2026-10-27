package com.kosh.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.data.model.Currency
import com.kosh.app.data.model.HouseholdObligationEntity
import com.kosh.app.data.model.PaymentInstrumentEntity
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.data.model.isCreditOrInflow
import com.kosh.app.data.model.displayAmount
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBackground
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ElectricMintContainer
import com.kosh.app.ui.theme.ObsidianBg
import com.kosh.app.ui.theme.ObsidianSurface
import com.kosh.app.ui.theme.PartnerPink
import com.kosh.app.ui.theme.RoyalBlue
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    currency: Currency,
    monthFormattedTitle: String,
    netSpend: Double,
    grossSpend: Double,
    totalReceivables: Double,
    householdObligations: List<HouseholdObligationEntity>,
    paymentInstruments: List<PaymentInstrumentEntity>,
    transactions: List<TransactionEntity>,
    isScanningSms: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onScanSms: () -> Unit,
    onSimulateInflow: () -> Unit,
    onOpenCurrencyPicker: () -> Unit,
    onPayHousehold: (HouseholdObligationEntity) -> Unit,
    onQuickPay: (HouseholdObligationEntity) -> Unit = {},
    onUndoHousehold: (HouseholdObligationEntity) -> Unit = {},
    onEditHousehold: (HouseholdObligationEntity) -> Unit = {},
    onAddHousehold: () -> Unit = {},
    onViewAllTransactions: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalCashOutflow = netSpend + totalReceivables
    val netPercent = if (totalCashOutflow > 0) ((netSpend / totalCashOutflow) * 100).toInt().coerceIn(10, 90) else 50
    val splitPercent = (100 - netPercent).coerceAtLeast(0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Interactive Month Selector Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Month Selector Carousel
                        IconButton(
                            onClick = onPreviousMonth,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = monthFormattedTitle.uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )

                        IconButton(
                            onClick = onNextMonth,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Month",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Currency Switcher Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, AmberGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { onOpenCurrencyPicker() }
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${currency.symbol} ${currency.code} ▾",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                        }
                    }
                }

                // Action Button: Scan SMS
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF162032))
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isScanningSms) { onScanSms() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isScanningSms) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                color = ElectricMint,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan SMS",
                                tint = ElectricMint,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Scan SMS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // The True-Cost Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF182132), Color(0xFF111723))
                        )
                    )
                    .border(1.dp, CardBorder, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "My True Expense",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "What actually came out of your pocket",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currency.symbol}${netSpend.toInt()}",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Gross Outflow",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted
                            )
                            Text(
                                text = "${currency.symbol}${grossSpend.toInt()}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }

                    // Split Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(AmberGold, shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pending from Friends",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmberGold
                                )
                            }
                            Text(
                                text = "${currency.symbol}${totalReceivables.toInt()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                        }

                        // Dual Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(netPercent.toFloat().coerceAtLeast(1f))
                                    .fillMaxSize()
                                    .background(TextSecondary.copy(alpha = 0.8f))
                            )
                            Box(
                                modifier = Modifier
                                    .weight(splitPercent.toFloat().coerceAtLeast(1f))
                                    .fillMaxSize()
                                    .background(AmberGold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$netPercent% Personal",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "$splitPercent% Group Splits",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // Monthly Home Bills Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FIXED MONTHLY EXPENSES & BILLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(RoyalBlue.copy(alpha = 0.15f))
                                .clickable { onAddHousehold() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "+ Add Expense",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue
                            )
                        }
                        Text(
                            text = "${householdObligations.size} bills",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                if (householdObligations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF131A26))
                            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { onAddHousehold() }
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No fixed expenses set up yet. Tap + to add Rent, SIP, Cook, Wifi.",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "+ Add Rent, SIP, or Utilities",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricMint
                            )
                        }
                    }
                } else {
                    householdObligations.forEach { ob ->
                        HouseholdCard(
                            currency = currency,
                            obligation = ob,
                            onClick = { onEditHousehold(ob) },
                            onPay = { onPayHousehold(ob) },
                            onQuickPay = { onQuickPay(ob) },
                            onUndo = { onUndoHousehold(ob) }
                        )
                    }
                }
            }
        }

        // Transactions Feed for the Selected Month
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT TRANSACTIONS (${monthFormattedTitle.uppercase()})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "View All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricMint,
                        modifier = Modifier.clickable { onViewAllTransactions() }
                    )
                }

                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF111723))
                            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions recorded for $monthFormattedTitle.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    transactions.take(6).forEach { tx ->
                        TransactionRow(
                            currency = currency,
                            transaction = tx,
                            onClick = { onTransactionClick(tx) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}


@Composable
fun HouseholdCard(
    currency: Currency,
    obligation: HouseholdObligationEntity,
    onClick: () -> Unit = {},
    onPay: () -> Unit,
    onQuickPay: () -> Unit = {},
    onUndo: () -> Unit = {}
) {
    val isNonSplittable = obligation.category.contains("SIP", ignoreCase = true) ||
            obligation.category.contains("WEALTH", ignoreCase = true) ||
            obligation.category.contains("INSURANCE", ignoreCase = true) ||
            obligation.category.contains("EMI", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF131A26))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .background(Color(0xFF1C2536), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = obligation.iconEmoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = obligation.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    val dueText = if (obligation.isPaidThisMonth) "Paid this month" else "Due on ${obligation.dueDayOfMonth}th"
                    Text(
                        text = dueText,
                        fontSize = 11.sp,
                        color = if (obligation.isPaidThisMonth) ElectricMint else AmberGold,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${currency.symbol}${obligation.totalAmount.toInt()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (obligation.isPaidThisMonth) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricMint.copy(alpha = 0.15f))
                            .clickable { onUndo() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "✓ Paid · Undo",
                            fontSize = 11.sp,
                            color = ElectricMint,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isNonSplittable) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricMint)
                            .clickable { onQuickPay() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Mark Paid ✓",
                            fontSize = 11.sp,
                            color = Color(0xFF0F141C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricMint.copy(alpha = 0.2f))
                            .clickable { onPay() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Pay & Split →",
                            fontSize = 11.sp,
                            color = ElectricMint,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    currency: Currency,
    transaction: TransactionEntity,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x99111724))
            .border(1.dp, CardBorder.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val isCredit = transaction.isCreditOrInflow()
                val icon = when {
                    isCredit -> "💰"
                    transaction.isAssetWealth -> "📈"
                    transaction.category.contains("CAB") -> "🚕"
                    transaction.category.contains("GIFT") -> "🎁"
                    transaction.category.contains("PARTNER") -> "👨‍👩‍👧"
                    transaction.category.contains("RENT") -> "🏠"
                    transaction.category.contains("COOK") -> "🍳"
                    transaction.category.contains("UTILITIES") -> "⚡"
                    transaction.category.contains("GROCERIES") -> "🛒"
                    transaction.category.contains("SHOPPING") -> "🛍️"
                    else -> "💳"
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (isCredit) ElectricMint.copy(alpha = 0.2f) else Color(0xFF1A2232),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = transaction.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = transaction.instrumentName,
                            fontSize = 10.sp,
                            color = RoyalBlue
                        )
                        if (transaction.hasSplit) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AmberGold.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Split w/ Flatmate",
                                    fontSize = 9.sp,
                                    color = AmberGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else if (transaction.isAssetWealth) {
                            Text(
                                text = "• Automated SIP • Wealth",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        } else if (transaction.category.contains("PARTNER") || transaction.note.contains("Partner") || transaction.note.contains("Family") || transaction.note.contains("Girlfriend")) {
                            val isFamily = transaction.note.contains("Family", ignoreCase = true)
                            val badgeColor = if (isFamily) ElectricMint else PartnerPink
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                val tagText = when {
                                    transaction.note.isNotBlank() && !transaction.note.startsWith("Auto-parsed") -> {
                                        when {
                                            transaction.note.startsWith("Partner: ") -> "❤️ " + transaction.note
                                            transaction.note.startsWith("Family: ") -> "👨‍👩‍👧 " + transaction.note
                                            else -> transaction.note
                                        }
                                    }
                                    isFamily -> "👨‍👩‍👧 Family Expense"
                                    else -> "❤️ Partner & Family"
                                }
                                Text(
                                    text = tagText,
                                    fontSize = 9.sp,
                                    color = badgeColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else if (transaction.category.contains("GIFT") || transaction.note.startsWith("Gift")) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFF472B6).copy(alpha = 0.18f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                val tagText = when {
                                    transaction.note.isNotBlank() && !transaction.note.startsWith("Auto-parsed") -> {
                                        if (transaction.note.startsWith("🎁")) transaction.note else "🎁 ${transaction.note}"
                                    }
                                    else -> "🎁 Gift"
                                }
                                Text(
                                    text = tagText,
                                    fontSize = 9.sp,
                                    color = Color(0xFFF472B6),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else if (transaction.note.isNotBlank() && !transaction.note.startsWith("Auto-parsed")) {
                            Text(
                                text = "• ${transaction.note}",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (transaction.isCreditOrInflow()) {
                    val amount = transaction.displayAmount()
                    Text(
                        text = "+${currency.symbol}${amount.toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricMint
                    )
                    Text(
                        text = "Credit / Inflow",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricMint.copy(alpha = 0.85f)
                    )
                } else if (transaction.isAssetWealth) {
                    Text(
                        text = "${currency.symbol}${transaction.grossAmount.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricMint
                    )
                    Text(
                        text = "Auto-debited",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                } else {
                    Text(
                        text = "-${currency.symbol}${transaction.grossAmount.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (transaction.hasSplit) {
                        Text(
                            text = "Net: -${currency.symbol}${transaction.trueNetAmount.toInt()}",
                            fontSize = 10.sp,
                            color = ElectricMint,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
