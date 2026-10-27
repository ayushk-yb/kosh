package com.kosh.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.data.model.Currency
import com.kosh.app.data.model.PersonEntity
import com.kosh.app.data.model.PersonLedgerEntity
import com.kosh.app.data.model.SplitDebtEntity
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.ui.components.SettlePersonBottomSheet
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBackground
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ObsidianBg
import com.kosh.app.ui.theme.PartnerPink
import com.kosh.app.ui.theme.RoyalBlue
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary

@Composable
fun PendingInScreen(
    currency: Currency,
    totalReceivables: Double,
    personLedgers: List<PersonLedgerEntity>,
    openDebts: List<SplitDebtEntity>,
    allPersons: List<PersonEntity> = emptyList(),
    recentTransactions: List<TransactionEntity> = emptyList(),
    onOpenAddIou: () -> Unit,
    onOpenAddPerson: () -> Unit = {},
    onMatchSplit: () -> Unit = {},
    onSettlePerson: (String) -> Unit = {},
    onRecordPartialSettlement: (personName: String, amount: Double, linkedTxId: Long?, note: String) -> Unit = { _, _, _, _ -> },
    onEditPerson: (PersonEntity) -> Unit = {},
    onWhatsAppNudge: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val personDebts = openDebts.filter { !it.isMerchantRefund }
    val merchantRefunds = openDebts.filter { it.isMerchantRefund }

    var settlingPerson by remember { mutableStateOf<PersonLedgerEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SHARED SPLITS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Owed to You",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Symmetrical, perfectly aligned 3-action row with uniform 36.dp height
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. + Person
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, ElectricMint.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { onOpenAddPerson() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add Person",
                                tint = ElectricMint,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Person",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricMint
                            )
                        }
                    }

                    // 2. Match Split
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .clickable { onMatchSplit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Match",
                                tint = ElectricMint,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Match Split",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricMint
                            )
                        }
                    }

                    // 3. Record Split
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberGold)
                            .clickable { onOpenAddIou() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Record Split",
                                tint = Color(0xFF0F141C),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Record Split",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F141C)
                            )
                        }
                    }
                }
            }
        }

        // Total Receivables Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF261908), Color(0xFF131924))
                        )
                    )
                    .border(1.dp, AmberGold.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Total Owed Back to You",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AmberGold
                    )
                    Text(
                        text = "${currency.symbol}${totalReceivables.toInt()}",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Across ${personLedgers.filter { it.relationshipType != "MERCHANT" }.size} people & ${merchantRefunds.size} pending merchant return. Independent of calendar months.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // People Ledgers
        val activePeople = personLedgers.filter { it.relationshipType != "MERCHANT" }
        if (activePeople.isEmpty() && merchantRefunds.isEmpty()) {
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
                            text = "No Pending Receivables",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "All settled! When you split an expense, pay flat rent, or lend money to someone, open receivables will appear here.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onOpenAddPerson,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricMint.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = ElectricMint,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Person",
                                    color = ElectricMint,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = onOpenAddIou,
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "+ Record Split",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F141C),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "PEOPLE LEDGERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            }

            items(activePeople.size) { index ->
                val person = activePeople[index]
                val specificDebts = personDebts.filter { it.personName.equals(person.name, ignoreCase = true) }
                val personEntity = allPersons.firstOrNull { it.name.equals(person.name, ignoreCase = true) }
                val isCircle = personEntity?.isCircleMember == true || person.relationshipType == "PARTNER"

                PersonLedgerCard(
                    currency = currency,
                    person = person,
                    isCircleMember = isCircle,
                    debts = specificDebts,
                    onClick = {
                        onEditPerson(personEntity ?: PersonEntity(name = person.name, tag = person.relationshipType))
                    },
                    onSettle = { settlingPerson = person },
                    onNudge = {
                        val amountStr = "${currency.symbol}${person.runningBalance.toInt()}"
                        onWhatsAppNudge(person.name, amountStr)
                        launchWhatsApp(context, person.name, specificDebts, amountStr, currency.symbol)
                    }
                )
            }
        }

        // Pending Merchant Refunds
        if (merchantRefunds.isNotEmpty()) {
            item {
                Text(
                    text = "PENDING MERCHANT REFUNDS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            }

            items(merchantRefunds.size) { idx ->
                val refund = merchantRefunds[idx]
                MerchantRefundCard(currency = currency, refund = refund)
            }
        }

        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }

    // Settle Person Bottom Sheet
    settlingPerson?.let { pLedger ->
        val pEntity = allPersons.firstOrNull { it.name.equals(pLedger.name, ignoreCase = true) }
        SettlePersonBottomSheet(
            currency = currency,
            personLedger = pLedger,
            personEntity = pEntity,
            recentTransactions = recentTransactions,
            onDismiss = { settlingPerson = null },
            onConfirmSettlement = { name, amt, linkedTxId, note ->
                onRecordPartialSettlement(name, amt, linkedTxId, note)
                onSettlePerson(name)
                settlingPerson = null
            }
        )
    }
}

@Composable
fun PersonLedgerCard(
    currency: Currency,
    person: PersonLedgerEntity,
    isCircleMember: Boolean = false,
    debts: List<SplitDebtEntity>,
    onClick: () -> Unit = {},
    onSettle: () -> Unit,
    onNudge: () -> Unit
) {
    val isPartner = isCircleMember || person.relationshipType == "PARTNER"
    val avatarBg = if (isPartner) PartnerPink.copy(alpha = 0.2f) else RoyalBlue.copy(alpha = 0.2f)
    val avatarBorder = if (isPartner) PartnerPink.copy(alpha = 0.4f) else RoyalBlue.copy(alpha = 0.4f)
    val avatarColor = if (isPartner) PartnerPink else RoyalBlue

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF131A26))
            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(avatarBg)
                            .border(1.dp, avatarBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPartner && person.avatarTag.isBlank()) "❤️" else person.avatarTag,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = avatarColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = person.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (isPartner) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PartnerPink.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "❤️ Circle",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PartnerPink
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (debts.isNotEmpty()) "${debts.size} open split${if (debts.size > 1) "s" else ""}" else if (isPartner) "Shared Outlays & Circle" else "Direct Split",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val isSettled = person.runningBalance <= 0.0
                    Text(
                        text = if (isSettled) "${currency.symbol}0 (Settled)" else "${currency.symbol}${person.runningBalance.toInt()}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSettled) ElectricMint else AmberGold
                    )
                    Text(
                        text = if (isSettled) "All clear" else "Owes you",
                        fontSize = 10.sp,
                        color = if (isSettled) ElectricMint else AmberGold,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Nested Split Breakdown if available
            if (debts.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0C111A))
                        .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        debts.forEach { debt ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = debt.itemDescription,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${currency.symbol}${debt.amount.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            } else if (person.notes.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0C111A))
                        .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = person.notes,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            // Action Buttons
            if (person.runningBalance > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNudge,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardBackground,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "💬 WhatsApp Nudge",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = onSettle,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricMint.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ElectricMint
                        )
                    ) {
                        Text(
                            text = "Settle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MerchantRefundCard(
    currency: Currency,
    refund: SplitDebtEntity
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF131A26))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF2A1C0E), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📦", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = refund.personName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = refund.itemDescription,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${currency.symbol}${refund.amount.toInt()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricMint
                )
                Text(
                    text = "Awaiting credit SMS",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}

fun buildWhatsAppMessage(
    personName: String,
    debts: List<SplitDebtEntity>,
    totalAmount: String,
    currencySymbol: String
): String {
    val itemsList = if (debts.isNotEmpty()) {
        debts.joinToString("\n") { "• ${it.itemDescription}: $currencySymbol${it.amount.toInt()}" }
    } else {
        "• Shared expenses balance: $totalAmount"
    }
    return """
    Hey $personName! 👋
    Here is the breakdown of our shared expenses:

    $itemsList
    ━━━━━━━━━━━━━━━
    Total pending: $totalAmount

    Whenever you get a chance to settle, thanks!
    """.trimIndent()
}

private fun launchWhatsApp(
    context: Context,
    person: String,
    debts: List<SplitDebtEntity>,
    amount: String,
    currencySymbol: String
) {
    val message = buildWhatsAppMessage(person, debts, amount, currencySymbol)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // WhatsApp not installed, intent falls back safely
    }
}
