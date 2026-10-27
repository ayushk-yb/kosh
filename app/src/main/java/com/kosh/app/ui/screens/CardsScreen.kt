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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.kosh.app.data.model.PaymentInstrumentEntity
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ObsidianBg
import com.kosh.app.ui.theme.RoyalBlue
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary

@Composable
fun CardsScreen(
    currency: Currency,
    instruments: List<PaymentInstrumentEntity>,
    transactions: List<TransactionEntity> = emptyList(),
    onAddCard: () -> Unit = {},
    onEditCard: (PaymentInstrumentEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val creditCards = instruments.filter { it.instrumentType == "CREDIT_CARD" }
    val bankAccounts = instruments.filter { it.instrumentType != "CREDIT_CARD" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
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
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Cards & Cycles",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElectricMint.copy(alpha = 0.15f))
                        .clickable { onAddCard() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+ Add Instrument",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricMint
                    )
                }
            }
        }

        if (instruments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF131A26))
                        .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "No Payment Instruments Added",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Add your credit cards to monitor billing cycles and safe 30% utilization limits, or scan your bank SMS to auto-detect them.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Button(
                            onClick = onAddCard,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ElectricMint),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "+ Add Card or Bank Account",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F141C),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Credit Cards
            if (creditCards.isNotEmpty()) {
                items(creditCards.size) { idx ->
                    val card = creditCards[idx]
                    val dynamicSpend = androidx.compose.runtime.remember(card, transactions) {
                        val sum = transactions.filter { tx ->
                            !tx.isAssetWealth && (
                                tx.instrumentName.equals(card.bankName, ignoreCase = true) ||
                                tx.instrumentName.contains(card.last4Digits)
                            )
                        }.sumOf { it.trueNetAmount }
                        if (sum > 0) sum else card.unbilledSpend
                    }
                    CreditCardItem(
                        currency = currency,
                        card = card,
                        spend = dynamicSpend,
                        onClick = { onEditCard(card) }
                    )
                }
            }

            // Bank Accounts & UPI
            if (bankAccounts.isNotEmpty()) {
                item {
                    Text(
                        text = "BANK & UPI HANDLES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(bankAccounts.size) { idx ->
                    val bank = bankAccounts[idx]
                    BankAccountItem(
                        currency = currency,
                        bank = bank,
                        transactions = transactions,
                        onClick = { onEditCard(bank) }
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
fun CreditCardItem(
    currency: Currency,
    card: PaymentInstrumentEntity,
    spend: Double = card.unbilledSpend,
    onClick: () -> Unit = {}
) {
    val gradientColors = listOf(Color(0xFF0F1A26), Color(0xFF131E2E), Color(0xFF101622))

    val available = (card.creditLimit - spend).coerceAtLeast(0.0)
    val utilRatio = if (card.creditLimit > 0) (spend / card.creditLimit).toFloat() else 0.15f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(1.dp, ElectricMint.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Card Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = card.bankName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricMint,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${card.cardOrAccountName} •••• ${card.last4Digits}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (card.badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricMint.copy(alpha = 0.15f))
                            .border(1.dp, ElectricMint.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = card.badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricMint
                        )
                    }
                }
            }

            // Spend vs Limit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Current Unbilled Spend",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${currency.symbol}${spend.toInt()}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Limit: ${currency.symbol}${card.creditLimit.toInt()}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${currency.symbol}${available.toInt()} Available",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricMint
                    )
                }
            }

            // Cycle & Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cycle: ${card.cycleStartDay}th to ${card.cycleEndDay}th",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Due: ${card.paymentDueDay}th",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(utilRatio.coerceIn(0.05f, 1f))
                            .fillMaxSize()
                            .background(ElectricMint)
                    )
                }
            }
        }
    }
}

@Composable
fun BankAccountItem(
    currency: Currency,
    bank: PaymentInstrumentEntity,
    transactions: List<TransactionEntity> = emptyList(),
    onClick: () -> Unit = {}
) {
    val upiSpend = transactions.filter { tx ->
        !tx.isAssetWealth && (
            tx.instrumentName.contains("UPI", ignoreCase = true) ||
            tx.instrumentName.contains(bank.bankName, ignoreCase = true) ||
            (bank.upiHandle?.isNotBlank() == true && tx.instrumentName.contains(bank.upiHandle))
        )
    }.sumOf { it.trueNetAmount }

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
                        .background(RoyalBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bank.bankName.take(4),
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
                        text = bank.upiHandle ?: "UPI Linked",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${currency.symbol}${upiSpend.toInt()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "outflow this month",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}
