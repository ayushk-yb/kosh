package com.kosh.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kosh.app.data.model.PaymentInstrumentEntity
import com.kosh.app.ui.theme.CardBackgroundElevated
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.CardBorderSubtle
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.KoshShapes
import com.kosh.app.ui.theme.ObsidianSurface
import com.kosh.app.ui.theme.SubSurface
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPaymentInstrumentBottomSheet(
    instrument: PaymentInstrumentEntity,
    currency: Currency,
    onDismiss: () -> Unit,
    onSave: (
        bankName: String,
        cardName: String,
        last4: String,
        isCreditCard: Boolean,
        limit: Double,
        cycleStartDay: Int,
        dueDay: Int,
        upiHandle: String?,
        cycleEndDay: Int
    ) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var bankName by remember { mutableStateOf(instrument.bankName) }
    var cardName by remember { mutableStateOf(instrument.cardOrAccountName) }
    var last4 by remember { mutableStateOf(instrument.last4Digits) }
    var isCreditCard by remember { mutableStateOf(instrument.instrumentType == "CREDIT_CARD") }
    var creditLimitText by remember { mutableStateOf(instrument.creditLimit.toInt().toString()) }
    var cycleStartDayText by remember { mutableStateOf(instrument.cycleStartDay.toString()) }
    var cycleEndDayText by remember { mutableStateOf(instrument.cycleEndDay.toString()) }
    var paymentDueDayText by remember { mutableStateOf(instrument.paymentDueDay.toString()) }
    var upiHandle by remember { mutableStateOf(instrument.upiHandle ?: "") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isCreditCard) "Edit Credit Card" else "Edit Bank Account",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${instrument.bankName} •••• ${instrument.last4Digits}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Bank Name Field
            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank / Issuer (e.g. HDFC, ICICI, SBI)", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = KoshShapes.Input,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CardBackgroundElevated,
                    unfocusedContainerColor = CardBackgroundElevated
                )
            )

            // Card / Account Nickname
            OutlinedTextField(
                value = cardName,
                onValueChange = { cardName = it },
                label = { Text("Card / Account Nickname (e.g. Millennia, Salary A/C)", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = KoshShapes.Input,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CardBackgroundElevated,
                    unfocusedContainerColor = CardBackgroundElevated
                )
            )

            // Last 4 Digits
            OutlinedTextField(
                value = last4,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) last4 = it },
                label = { Text("Last 4 Digits", color = TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = KoshShapes.Input,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CardBackgroundElevated,
                    unfocusedContainerColor = CardBackgroundElevated
                )
            )

            // Credit Limit (if Credit Card)
            if (isCreditCard) {
                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) creditLimitText = it },
                    label = { Text("Credit Limit (${currency.symbol})", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = KoshShapes.Input,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricMint,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CardBackgroundElevated,
                        unfocusedContainerColor = CardBackgroundElevated
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = cycleStartDayText,
                        onValueChange = { input ->
                            if (input.isEmpty() || (input.toIntOrNull() in 1..31)) {
                                cycleStartDayText = input
                                val s = input.toIntOrNull()
                                if (s != null) {
                                    cycleEndDayText = if (s == 1) "31" else (s - 1).toString()
                                }
                            }
                        },
                        label = { Text("Cycle Start", color = TextSecondary, fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = KoshShapes.Input,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricMint,
                            unfocusedBorderColor = CardBorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CardBackgroundElevated,
                            unfocusedContainerColor = CardBackgroundElevated
                        )
                    )

                    OutlinedTextField(
                        value = cycleEndDayText,
                        onValueChange = { if (it.isEmpty() || (it.toIntOrNull() in 1..31)) cycleEndDayText = it },
                        label = { Text("Cycle End", color = TextSecondary, fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = KoshShapes.Input,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricMint,
                            unfocusedBorderColor = CardBorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CardBackgroundElevated,
                            unfocusedContainerColor = CardBackgroundElevated
                        )
                    )

                    OutlinedTextField(
                        value = paymentDueDayText,
                        onValueChange = { if (it.isEmpty() || (it.toIntOrNull() in 1..31)) paymentDueDayText = it },
                        label = { Text("Due Day", color = TextSecondary, fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = KoshShapes.Input,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricMint,
                            unfocusedBorderColor = CardBorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CardBackgroundElevated,
                            unfocusedContainerColor = CardBackgroundElevated
                        )
                    )
                }

                // Cycle preview chip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(KoshShapes.Chip)
                        .background(CardBackgroundElevated)
                        .border(0.5.dp, CardBorderSubtle, KoshShapes.Chip)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = ElectricMint,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Billing Cycle: ${cycleStartDayText.ifEmpty { "1" }}th to ${cycleEndDayText.ifEmpty { "31" }}th of every month",
                            fontSize = 11.sp,
                            color = ElectricMint,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // UPI Handle Field
            OutlinedTextField(
                value = upiHandle,
                onValueChange = { upiHandle = it },
                label = { Text("Linked UPI Handle (e.g. name@okhdfcbank)", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = KoshShapes.Input,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CardBackgroundElevated,
                    unfocusedContainerColor = CardBackgroundElevated
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons: Delete & Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                    shape = KoshShapes.Pill
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }

                Button(
                    onClick = {
                        val limit = creditLimitText.toDoubleOrNull() ?: instrument.creditLimit
                        val cycleStartDay = cycleStartDayText.toIntOrNull() ?: instrument.cycleStartDay
                        val cycleEndDay = cycleEndDayText.toIntOrNull() ?: (if (cycleStartDay == 1) 31 else cycleStartDay - 1)
                        val dueDay = paymentDueDayText.toIntOrNull() ?: instrument.paymentDueDay

                        onSave(
                            bankName.trim().ifEmpty { instrument.bankName },
                            cardName.trim().ifEmpty { instrument.cardOrAccountName },
                            last4.trim().ifEmpty { instrument.last4Digits },
                            isCreditCard,
                            limit,
                            cycleStartDay,
                            dueDay,
                            upiHandle.trim().ifEmpty { null },
                            cycleEndDay
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricMint,
                        contentColor = Color(0xFF090D14)
                    ),
                    shape = KoshShapes.Pill
                ) {
                    Text(
                        text = "Save Changes",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = ObsidianSurface,
            shape = KoshShapes.CardHero,
            title = {
                Text(
                    text = "Remove Instrument?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${instrument.bankName} ${instrument.cardOrAccountName}'? Existing logged transactions will not be deleted.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = KoshShapes.Pill
                ) {
                    Text("Remove", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
