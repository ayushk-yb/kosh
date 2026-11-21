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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBackgroundElevated
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.CardBorderSubtle
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ElectricMintContainer
import com.kosh.app.ui.theme.KoshShapes
import com.kosh.app.ui.theme.ObsidianSurface
import com.kosh.app.ui.theme.RoyalBlue
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPaymentInstrumentBottomSheet(
    currency: Currency,
    onDismiss: () -> Unit,
    onSave: (
        bankName: String,
        cardName: String,
        last4: String,
        isCreditCard: Boolean,
        creditLimit: Double,
        cycleStartDay: Int,
        dueDay: Int
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var bankName by remember { mutableStateOf("HDFC Bank") }
    var cardName by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var isCreditCard by remember { mutableStateOf(true) }
    var creditLimitText by remember { mutableStateOf("200000") }
    var cycleStartDayText by remember { mutableStateOf("1") }
    var dueDayText by remember { mutableStateOf("20") }

    val popularBanks = listOf("HDFC Bank", "SBI Card", "ICICI Bank", "Axis Bank", "Kotak Bank")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        shape = KoshShapes.Sheet,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(TextMuted.copy(alpha = 0.4f), shape = KoshShapes.Pill)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCreditCard) "Add Credit Card" else "Add Bank Account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            // Type Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KoshShapes.Pill)
                    .background(CardBackgroundElevated)
                    .border(0.5.dp, CardBorderSubtle, KoshShapes.Pill)
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(KoshShapes.Pill)
                        .background(if (isCreditCard) ElectricMintContainer else Color.Transparent)
                        .clickable { isCreditCard = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Credit Card",
                        fontSize = 12.sp,
                        fontWeight = if (isCreditCard) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isCreditCard) TextPrimary else TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(KoshShapes.Pill)
                        .background(if (!isCreditCard) ElectricMintContainer else Color.Transparent)
                        .clickable { isCreditCard = false }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bank Account / UPI",
                        fontSize = 12.sp,
                        fontWeight = if (!isCreditCard) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (!isCreditCard) TextPrimary else TextSecondary
                    )
                }
            }

            // Quick Bank Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "BANK NAME",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    popularBanks.forEach { b ->
                        val isSel = bankName.equals(b, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(KoshShapes.Chip)
                                .background(if (isSel) RoyalBlue.copy(alpha = 0.25f) else CardBackgroundElevated)
                                .border(0.5.dp, if (isSel) RoyalBlue else CardBorderSubtle, KoshShapes.Chip)
                                .clickable { bankName = b }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = b,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color(0xFF93C5FD) else TextSecondary
                            )
                        }
                    }
                }
            }

            // Card / Account Name
            OutlinedTextField(
                value = cardName,
                onValueChange = { cardName = it },
                label = { Text(if (isCreditCard) "Card Variant (e.g. Millennia, Regalia Gold)" else "Account Nickname (e.g. Salary, Savings)", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CardBackgroundElevated,
                    unfocusedContainerColor = CardBackgroundElevated
                ),
                shape = KoshShapes.Input
            )

            // Last 4 Digits
            OutlinedTextField(
                value = last4,
                onValueChange = { if (it.length <= 4) last4 = it },
                label = { Text("Last 4 Digits *", fontSize = 12.sp) },
                placeholder = { Text("e.g. 4092") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CardBackgroundElevated,
                    unfocusedContainerColor = CardBackgroundElevated
                ),
                shape = KoshShapes.Input
            )

            // Credit Card Specifics: Limit, Statement Date, Due Date
            if (isCreditCard) {
                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { creditLimitText = it },
                    label = { Text("Credit Limit (${currency.symbol})", fontSize = 12.sp) },
                    placeholder = { Text("200000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricMint,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CardBackgroundElevated,
                        unfocusedContainerColor = CardBackgroundElevated
                    ),
                    shape = KoshShapes.Input
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = cycleStartDayText,
                        onValueChange = { cycleStartDayText = it },
                        label = { Text("Billing Day (1-31)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricMint,
                            unfocusedBorderColor = CardBorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CardBackgroundElevated,
                            unfocusedContainerColor = CardBackgroundElevated
                        ),
                        shape = KoshShapes.Input
                    )

                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it },
                        label = { Text("Due Day (1-31)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricMint,
                            unfocusedBorderColor = CardBorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CardBackgroundElevated,
                            unfocusedContainerColor = CardBackgroundElevated
                        ),
                        shape = KoshShapes.Input
                    )
                }
            }

            Button(
                onClick = {
                    if (cardName.isNotBlank() && last4.isNotBlank()) {
                        val limit = creditLimitText.toDoubleOrNull() ?: 0.0
                        val cycleStart = cycleStartDayText.toIntOrNull() ?: 1
                        val dueDay = dueDayText.toIntOrNull() ?: 20
                        onSave(bankName, cardName, last4, isCreditCard, limit, cycleStart, dueDay)
                        onDismiss()
                    }
                },
                enabled = cardName.isNotBlank() && last4.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricMint,
                    contentColor = Color(0xFF090D14)
                ),
                shape = KoshShapes.Pill
            ) {
                Text(
                    text = "Save Instrument",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
