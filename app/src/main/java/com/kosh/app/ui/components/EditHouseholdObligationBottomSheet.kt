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
import com.kosh.app.data.model.HouseholdObligationEntity
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ObsidianSurface
import com.kosh.app.ui.theme.RoyalBlue
import com.kosh.app.ui.theme.SubSurface
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditHouseholdObligationBottomSheet(
    obligation: HouseholdObligationEntity,
    currency: Currency,
    onDismiss: () -> Unit,
    onSave: (HouseholdObligationEntity) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(obligation.title) }
    var category by remember { mutableStateOf(obligation.category) }
    var recipient by remember { mutableStateOf(obligation.recipientName) }
    var amountText by remember { mutableStateOf(obligation.totalAmount.toString()) }
    var dueDayText by remember { mutableStateOf(obligation.dueDayOfMonth.toString()) }
    var selectedEmoji by remember { mutableStateOf(obligation.iconEmoji) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val templates = listOf(
        Triple("SIP / Wealth", "Automated SIP", "📈"),
        Triple("Rent", "Flat Rent & Maintenance", "🏠"),
        Triple("Cook", "Cook Salary", "🍳"),
        Triple("Maid", "Maid Salary", "🧹"),
        Triple("Electricity", "Electricity & Utilities", "⚡"),
        Triple("Wifi", "Wi-Fi Broadband", "📶"),
        Triple("Mobile", "Mobile / DTH", "📱"),
        Triple("Insurance", "Insurance / EMI", "🛡️")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SubSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit Monthly Expense",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Fixed recurring bill or wealth SIP",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            // Quick Category Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CATEGORY / PRESET",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    templates.forEach { (cat, name, emoji) ->
                        val isSel = category.equals(cat, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) RoyalBlue.copy(alpha = 0.25f) else Color(0xFF131A26))
                                .border(1.dp, if (isSel) RoyalBlue else CardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    category = cat
                                    title = name
                                    selectedEmoji = emoji
                                    when (cat) {
                                        "Cook" -> recipient = "Cook"
                                        "Maid" -> recipient = "Maid"
                                        "Rent" -> recipient = "Landlord"
                                        "SIP / Wealth" -> recipient = "AMC / Groww / Zerodha"
                                        "Electricity" -> recipient = "Electricity Board"
                                        "Wifi" -> recipient = "Broadband Provider"
                                        "Mobile" -> recipient = "Telecom Operator"
                                        "Insurance" -> recipient = "Bank / Insurer"
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = emoji, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color(0xFF93C5FD) else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Expense Title *", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = input
                        }
                    },
                    label = { Text("Total Monthly Amount * (${currency.symbol})", fontSize = 11.sp) },
                    placeholder = { Text("e.g. 30000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricMint,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d{1,2}$"))) {
                            val v = input.toIntOrNull()
                            if (v == null || v in 1..31) {
                                dueDayText = input
                            }
                        }
                    },
                    label = { Text("Due Day (1-31)", fontSize = 11.sp) },
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricMint,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text("Recipient / Landlord / Vendor / AMC", fontSize = 12.sp) },
                placeholder = { Text("e.g. Landlord / Groww / Bescom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

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
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp)
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
                        val amt = amountText.toDoubleOrNull() ?: obligation.totalAmount
                        val day = dueDayText.toIntOrNull() ?: obligation.dueDayOfMonth
                        if (title.isNotBlank() && amt > 0) {
                            val updated = obligation.copy(
                                title = title.trim(),
                                category = category.trim(),
                                recipientName = recipient.trim(),
                                totalAmount = amt,
                                myShareAmount = if (obligation.splitWays > 1) amt / obligation.splitWays else amt,
                                dueDayOfMonth = day,
                                iconEmoji = selectedEmoji
                            )
                            onSave(updated)
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricMint,
                        contentColor = Color(0xFF0F141C)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Save Changes",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F141C),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = "Delete Fixed Expense?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${obligation.title}'? This will remove this recurring bill from your fixed monthly expenses.",
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
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
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
