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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.data.model.Currency
import com.kosh.app.data.scanner.ParsedSmsResult
import com.kosh.app.data.scanner.SmsAutoScanner
import com.kosh.app.ui.theme.AmberGold
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
fun SmsParserLabBottomSheet(
    currency: Currency,
    onDismiss: () -> Unit,
    onImportTransaction: (ParsedSmsResult) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current

    var smsText by remember { mutableStateOf("") }
    var senderAddress by remember { mutableStateOf("VM-HDFCBK") }

    val parsedResult: ParsedSmsResult? = remember(smsText, senderAddress) {
        if (smsText.isNotBlank()) {
            SmsAutoScanner.parseSms(smsText, senderAddress)
        } else {
            null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SubSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricMint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧪", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SMS Parser Lab",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Test & diagnose bank SMS regex parsing",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
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

            // Quick Samples Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "QUICK TEST PRESETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SampleChip(
                        label = "Mandate Debit",
                        onClick = {
                            senderAddress = "VM-HDFCBK"
                            smsText = "Dear Customer, your A/c XX4450 has been debited by Rs.499.00 on 24-Oct-26 towards UPI Mandate to NETFLIX."
                        }
                    )
                    SampleChip(
                        label = "Spotify Mandate",
                        onClick = {
                            senderAddress = "AD-HDFCBK"
                            smsText = "Your A/c ending 1234 debited for Rs 199.00 towards Mandate execution for SPOTIFY via UPI. Avail Bal: Rs 45,210."
                        }
                    )
                    SampleChip(
                        label = "Credit Card Spend",
                        onClick = {
                            senderAddress = "BZ-SBICRD"
                            smsText = "Rs. 2,450.00 spent on your SBI Credit Card ending 7891 at STARBUCKS COFFEE on 28-OCT-26. Avl lmt: Rs 1,45,000."
                        }
                    )
                    SampleChip(
                        label = "Account Credit (Inflow)",
                        onClick = {
                            senderAddress = "VM-ICICIB"
                            smsText = "Dear Customer, your A/c XX8890 is credited with Rs 3,500.00 on 28-Oct-26 by UPI/P2A/Ref 6291823901/Settlement from Rahul."
                        }
                    )
                    SampleChip(
                        label = "OTP Message",
                        onClick = {
                            senderAddress = "VK-AXISBK"
                            smsText = "392811 is your OTP for purchase of Rs. 1,299 at Amazon. Valid for 10 mins. Do NOT share OTP or CVV with anyone."
                        }
                    )
                }
            }

            // Sender Address
            OutlinedTextField(
                value = senderAddress,
                onValueChange = { senderAddress = it },
                label = { Text("SMS Sender ID / Header", color = TextSecondary, fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                )
            )

            // SMS Body Input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMS MESSAGE BODY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricMint.copy(alpha = 0.12f))
                            .clickable {
                                clipboardManager.getText()?.text?.let {
                                    smsText = it
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = ElectricMint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Paste from Clipboard",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricMint
                        )
                    }
                }

                OutlinedTextField(
                    value = smsText,
                    onValueChange = { smsText = it },
                    placeholder = { Text("Paste any bank SMS here to test parser and verify detection rules...", color = TextMuted, fontSize = 12.sp) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricMint,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface
                    )
                )
            }

            // Diagnostic Report Section
            if (parsedResult != null) {
                DiagnosticCard(
                    currency = currency,
                    result = parsedResult
                )

                // Import Button
                if (parsedResult.isValid && parsedResult.amount != null) {
                    Button(
                        onClick = {
                            onImportTransaction(parsedResult)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricMint,
                            contentColor = Color(0xFF0F141C)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Import",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Import as ${if (parsedResult.isCredit) "Inflow (+${currency.symbol}${parsedResult.amount.toInt()})" else "Expense (-${currency.symbol}${parsedResult.amount.toInt()})"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SampleChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF162032))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiagnosticCard(
    currency: Currency,
    result: ParsedSmsResult
) {
    val (statusColor, statusBg, statusIcon, statusTitle) = when {
        result.isValid && result.isCredit -> {
            listOf(Color(0xFF10B981), Color(0xFF064E3B).copy(alpha = 0.4f), Icons.Default.CheckCircle, "💰 Valid Credit / Inflow Detected")
        }
        result.isValid && result.isDebit -> {
            listOf(ElectricMint, ElectricMint.copy(alpha = 0.12f), Icons.Default.CheckCircle, if (result.isMandate) "⚡ Valid UPI Mandate Debit Detected" else "✅ Valid Debit Transaction Detected")
        }
        result.isIgnored -> {
            listOf(Color(0xFFEF4444), Color(0xFF7F1D1D).copy(alpha = 0.35f), Icons.Default.Close, "🚫 Message Ignored / Filtered Out")
        }
        else -> {
            listOf(AmberGold, AmberGold.copy(alpha = 0.15f), Icons.Default.Warning, "⚠️ Unrecognized Message (No Valid Debit/Credit Match)")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E1522))
            .border(1.dp, (statusColor as Color).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Status Header Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusBg as Color)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon as androidx.compose.ui.graphics.vector.ImageVector,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusTitle as String,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            // Reason if ignored
            if (result.isIgnored && result.ignoreReason != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1A24))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Filter reason: ${result.ignoreReason}",
                        fontSize = 11.sp,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }

            // Extracted Fields Breakdown
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF131B2A))
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagnosticRow(
                    label = "Parsed Amount",
                    value = if (result.amount != null) "${currency.symbol}${result.amount}" else "None detected",
                    highlight = result.amount != null
                )
                if (result.matchedAmountPattern != null) {
                    DiagnosticRow(
                        label = "Amount Match Rule",
                        value = result.matchedAmountPattern,
                        highlight = false
                    )
                }
                DiagnosticRow(
                    label = "Transaction Type",
                    value = when {
                        result.isCredit -> "CREDIT / INFLOW"
                        result.isMandate -> "UPI MANDATE DEBIT"
                        result.isDebit -> "DEBIT"
                        else -> "UNKNOWN"
                    },
                    highlight = result.isDebit || result.isCredit
                )
                DiagnosticRow(
                    label = "Merchant / Title",
                    value = result.title,
                    highlight = true
                )
                DiagnosticRow(
                    label = "Bank / Instrument",
                    value = result.instrument,
                    highlight = false
                )
                DiagnosticRow(
                    label = "Category",
                    value = "${result.category.icon} ${result.category.displayName}${if (result.isWealth) " (Wealth Asset)" else ""}",
                    highlight = false
                )
                if (result.last4 != null) {
                    DiagnosticRow(
                        label = "Account / Card Last 4",
                        value = "•••• ${result.last4}",
                        highlight = false
                    )
                }
            }

            // Matched Trigger Keywords
            if (result.matchedKeywords.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "MATCHED TRIGGER KEYWORDS IN SMS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        result.matchedKeywords.forEach { kw ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E2838))
                                    .border(1.dp, ElectricMint.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = kw,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElectricMint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    highlight: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) ElectricMint else TextPrimary
        )
    }
}
