package com.kosh.app.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.data.model.Currency
import com.kosh.app.data.model.PersonEntity
import com.kosh.app.data.model.PersonLedgerEntity
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ObsidianSurface
import com.kosh.app.ui.theme.SubSurface
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary

enum class SettlementMode {
    CASH_DIRECT,
    LINK_TRANSACTION
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettlePersonBottomSheet(
    currency: Currency,
    personLedger: PersonLedgerEntity,
    personEntity: PersonEntity? = null,
    recentTransactions: List<TransactionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmSettlement: (
        personName: String,
        amount: Double,
        linkedTransactionId: Long?,
        note: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val totalBalance = personLedger.runningBalance
    var amountText by remember { mutableStateOf(totalBalance.toInt().toString()) }
    var selectedMode by remember { mutableStateOf(SettlementMode.CASH_DIRECT) }
    var selectedTransactionId by remember { mutableStateOf<Long?>(null) }
    var noteText by remember { mutableStateOf("") }

    val currentAmount = amountText.toDoubleOrNull() ?: 0.0

    // Quick amount chip options
    val quickAmounts = remember(totalBalance) {
        val list = mutableListOf<Double>()
        if (totalBalance >= 1000) list.add(500.0)
        if (totalBalance >= 2000) list.add(1000.0)
        if (totalBalance >= 3000) list.add(2000.0)
        val half = (totalBalance / 2).toInt().toDouble()
        if (half > 0 && !list.contains(half) && half != totalBalance) {
            list.add(half)
        }
        list.distinct().sorted()
    }

    val potentialCreditTxs = remember(recentTransactions) {
        recentTransactions.filter {
            it.grossAmount == 0.0 || it.title.contains("Credit", ignoreCase = true) || it.instrumentName.contains("Credit", ignoreCase = true)
        }.take(5)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val avatarColor = personEntity?.avatarColor?.let { Color(it) } ?: AmberGold
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(avatarColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = personLedger.avatarTag.ifBlank { personLedger.name.take(2).uppercase() },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = avatarColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Settle with ${personLedger.name}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Currently owes ${currency.symbol}${totalBalance.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AmberGold
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            // Settlement Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        amountText = input
                    }
                },
                label = { Text("Settlement Amount", color = TextSecondary) },
                leadingIcon = {
                    Text(
                        text = currency.symbol,
                        color = ElectricMint,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SubSurface,
                    unfocusedContainerColor = SubSurface
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // Quick Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickAmounts.forEach { amt ->
                    val isSel = currentAmount == amt
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) ElectricMint.copy(alpha = 0.2f) else SubSurface)
                            .border(1.dp, if (isSel) ElectricMint else CardBorder, RoundedCornerShape(8.dp))
                            .clickable { amountText = amt.toInt().toString() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${currency.symbol}${amt.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) ElectricMint else TextSecondary
                        )
                    }
                }

                // Full Amount Chip
                val isFull = currentAmount == totalBalance
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFull) AmberGold.copy(alpha = 0.25f) else SubSurface)
                        .border(1.dp, if (isFull) AmberGold else CardBorder, RoundedCornerShape(8.dp))
                        .clickable { amountText = totalBalance.toInt().toString() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Full (${currency.symbol}${totalBalance.toInt()})",
                        fontSize = 11.sp,
                        fontWeight = if (isFull) FontWeight.Bold else FontWeight.Medium,
                        color = if (isFull) AmberGold else TextSecondary
                    )
                }
            }

            // Remaining Balance Forecast
            val remainingBal = (totalBalance - currentAmount).coerceAtLeast(0.0)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF131A26))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (remainingBal <= 0) "🎉 Full settlement — zero remaining balance." else "Remaining owed after this: ${currency.symbol}${remainingBal.toInt()}",
                        fontSize = 11.sp,
                        color = if (remainingBal <= 0) ElectricMint else AmberGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Payment Mode Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SubSurface)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedMode == SettlementMode.CASH_DIRECT) ElectricMint else Color.Transparent)
                        .clickable { selectedMode = SettlementMode.CASH_DIRECT; selectedTransactionId = null }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💵 Cash / Direct",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMode == SettlementMode.CASH_DIRECT) Color(0xFF0F141C) else TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedMode == SettlementMode.LINK_TRANSACTION) ElectricMint else Color.Transparent)
                        .clickable { selectedMode = SettlementMode.LINK_TRANSACTION }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏦 Link Bank / UPI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMode == SettlementMode.LINK_TRANSACTION) Color(0xFF0F141C) else TextSecondary
                    )
                }
            }

            // Link Transaction Options if selected
            if (selectedMode == SettlementMode.LINK_TRANSACTION) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LINK TO RECENT INFLOW TRANSACTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )

                    if (potentialCreditTxs.isEmpty()) {
                        Text(
                            text = "No recent bank credit transactions found. Direct settlement will be recorded.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    } else {
                        potentialCreditTxs.forEach { tx ->
                            val isSel = selectedTransactionId == tx.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) ElectricMint.copy(alpha = 0.15f) else SubSurface)
                                    .border(1.dp, if (isSel) ElectricMint else CardBorder, RoundedCornerShape(10.dp))
                                    .clickable { selectedTransactionId = if (isSel) null else tx.id }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = tx.title, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(text = "${tx.instrumentName} • ${tx.note.ifBlank { "Incoming credit" }}", fontSize = 10.sp, color = TextMuted)
                                }
                                if (isSel) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = ElectricMint, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Optional Note
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note (Optional, e.g. Cash at office)", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SubSurface,
                    unfocusedContainerColor = SubSurface
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // CTA Button
            Button(
                onClick = {
                    if (currentAmount > 0) {
                        onConfirmSettlement(
                            personLedger.name,
                            currentAmount,
                            selectedTransactionId,
                            noteText.ifBlank { if (selectedMode == SettlementMode.CASH_DIRECT) "Cash settlement" else "Bank / UPI settlement" }
                        )
                        onDismiss()
                    }
                },
                enabled = currentAmount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricMint,
                    disabledContainerColor = CardBorder
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Confirm Settlement (${currency.symbol}${currentAmount.toInt()})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F141C)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
