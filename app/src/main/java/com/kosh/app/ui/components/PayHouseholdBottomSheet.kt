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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.runtime.mutableStateMapOf
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
import com.kosh.app.data.model.PersonEntity
import com.kosh.app.data.repository.ParticipantSplitShare
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.CoralRed
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ObsidianSurface
import com.kosh.app.ui.theme.RoyalBlue
import com.kosh.app.ui.theme.SubSurface
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary

enum class SplitMode(val displayName: String, val subtitle: String) {
    SOLO("Solo", "100% Personal Burn (0% split)"),
    EQUAL("Equal Split", "Divide equally among selected people"),
    CUSTOM("Custom", "Set exact amounts per person")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PayHouseholdBottomSheet(
    currency: Currency,
    obligation: HouseholdObligationEntity,
    allPersons: List<PersonEntity>,
    onDismiss: () -> Unit,
    onConfirmPay: (
        splitMode: String,
        participants: List<ParticipantSplitShare>
    ) -> Unit,
    onAddNewPerson: (name: String, tag: String, phone: String?, isPinned: Boolean) -> Unit = { _, _, _, _ -> },
    onTogglePin: (personId: Long, isPinned: Boolean) -> Unit = { _, _ -> },
    onPickContact: () -> Unit = {},
    onOpenPersonPicker: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isNonSplittable = obligation.category.contains("SIP", ignoreCase = true) ||
            obligation.category.contains("WEALTH", ignoreCase = true) ||
            obligation.category.contains("INSURANCE", ignoreCase = true) ||
            obligation.category.contains("EMI", ignoreCase = true)

    var currentMode by remember {
        mutableStateOf(if (isNonSplittable) SplitMode.SOLO else SplitMode.EQUAL)
    }
    var selectedPersons by remember {
        mutableStateOf(allPersons.filter { it.isPinned }.take(2))
    }
    var showPersonPicker by remember { mutableStateOf(false) }
    val customShares = remember { mutableStateMapOf<Long, String>() }

    val totalAmount = obligation.totalAmount

    // Calculations based on mode
    val participantSplits: List<ParticipantSplitShare> = when (currentMode) {
        SplitMode.SOLO -> emptyList()
        SplitMode.EQUAL -> {
            if (selectedPersons.isEmpty()) emptyList()
            else {
                val totalParticipants = selectedPersons.size + 1 // +1 for user
                val perPersonShare = (totalAmount / totalParticipants)
                selectedPersons.map { p ->
                    ParticipantSplitShare(
                        personId = p.id,
                        name = p.name,
                        shareAmount = perPersonShare
                    )
                }
            }
        }
        SplitMode.CUSTOM -> {
            selectedPersons.map { p ->
                val entered = customShares[p.id]?.toDoubleOrNull() ?: 0.0
                ParticipantSplitShare(
                    personId = p.id,
                    name = p.name,
                    shareAmount = entered
                )
            }
        }
    }

    val totalReimbursable = participantSplits.sumOf { it.shareAmount }
    val myPersonalNet = (totalAmount - totalReimbursable).coerceAtLeast(0.0)

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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF1E2838), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = obligation.iconEmoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = obligation.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Due on ${obligation.dueDayOfMonth}th • Total: ${currency.symbol}${totalAmount.toInt()}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            // Split Mode Selector Tabs (Hidden for Non-Splittable investments)
            if (isNonSplittable) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131A26))
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "🔒 Personal Outflow: SIP, Wealth & Insurance investments are personal portfolio additions and cannot be split with flatmates.",
                        fontSize = 12.sp,
                        color = ElectricMint,
                        lineHeight = 16.sp
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F1521))
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SplitMode.values().forEach { mode ->
                        val isSelected = currentMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ElectricMint else Color.Transparent)
                                .clickable { currentMode = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF0F141C) else TextSecondary
                            )
                        }
                    }
                }
            }

            // Mode Details & Participants Picker
            if (currentMode != SplitMode.SOLO) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SPLITTING WITH (${selectedPersons.size} Flatmates)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(RoyalBlue.copy(alpha = 0.15f))
                                .clickable { showPersonPicker = true }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "+ Select Flatmates",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue
                            )
                        }
                    }

                    if (selectedPersons.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131A26))
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                .clickable { showPersonPicker = true }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tap here to select flatmates splitting this bill.",
                                fontSize = 12.sp,
                                color = ElectricMint,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Participant list
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // "You" share card for Equal Mode
                            if (currentMode == SplitMode.EQUAL) {
                                val totalPeople = selectedPersons.size + 1
                                val equalShare = totalAmount / totalPeople
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricMint.copy(alpha = 0.12f))
                                        .border(1.dp, ElectricMint.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(ElectricMint.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "👤", fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "You (1 share included)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ElectricMint
                                            )
                                            Text(
                                                text = "Your personal net burn",
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${currency.symbol}${equalShare.toInt()}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricMint
                                    )
                                }
                            }

                            selectedPersons.forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF131A26))
                                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color(p.avatarColor).copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = p.name.take(1).uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(p.avatarColor)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = p.name + if (p.tag.isNotBlank()) " (${p.tag})" else "",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (currentMode == SplitMode.EQUAL) {
                                            val share = totalAmount / (selectedPersons.size + 1)
                                            Text(
                                                text = "${currency.symbol}${share.toInt()}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AmberGold
                                            )
                                        } else {
                                            // Custom input
                                            OutlinedTextField(
                                                value = customShares[p.id] ?: "",
                                                onValueChange = { customShares[p.id] = it },
                                                placeholder = { Text("0", fontSize = 12.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.width(90.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = ElectricMint,
                                                    unfocusedBorderColor = CardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary,
                                                    focusedContainerColor = SubSurface,
                                                    unfocusedContainerColor = SubSurface
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { selectedPersons = selectedPersons.filter { it.id != p.id } },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = TextMuted,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (currentMode == SplitMode.EQUAL) {
                                val totalPeople = selectedPersons.size + 1
                                val equalShare = totalAmount / totalPeople
                                Text(
                                    text = "💡 ${currency.symbol}${totalAmount.toInt()} split equally among $totalPeople people (${currency.symbol}${equalShare.toInt()} each).",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131A26))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Solo Burn: You are paying 100% of this bill alone (${currency.symbol}${totalAmount.toInt()}). No split debt requests will be created.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Summary Breakdown Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F1521))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total Paid Outflow", fontSize = 12.sp, color = TextMuted)
                        Text(text = "${currency.symbol}${totalAmount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Your Net Personal Burn", fontSize = 12.sp, color = ElectricMint, fontWeight = FontWeight.SemiBold)
                        Text(text = "${currency.symbol}${myPersonalNet.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElectricMint)
                    }
                    if (totalReimbursable > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Pending Receivables (Splits)", fontSize = 12.sp, color = AmberGold)
                            Text(text = "${currency.symbol}${totalReimbursable.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                        }
                    }
                }
            }

            // Confirm Button
            Button(
                onClick = {
                    onConfirmPay(currentMode.name, participantSplits)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricMint),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Confirm & Mark as Paid (${currency.symbol}${totalAmount.toInt()})",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F141C),
                    fontSize = 14.sp
                )
            }
        }
    }

    if (showPersonPicker) {
        PersonPickerModal(
            title = "Select Flatmates",
            persons = allPersons,
            selectedPersonIds = selectedPersons.map { it.id }.toSet(),
            isMultiSelect = true,
            onPersonSelected = { person ->
                if (selectedPersons.any { it.id == person.id }) {
                    selectedPersons = selectedPersons.filter { it.id != person.id }
                } else {
                    selectedPersons = selectedPersons + person
                }
            },
            onSelectionConfirmed = { selectedList ->
                selectedPersons = selectedList
                showPersonPicker = false
            },
            onAddNewPerson = onAddNewPerson,
            onTogglePin = onTogglePin,
            onPickContact = onPickContact,
            onDismiss = { showPersonPicker = false }
        )
    }
}

@Composable
fun UndoHouseholdPaymentDialog(
    currency: Currency,
    obligation: HouseholdObligationEntity,
    onDismiss: () -> Unit,
    onConfirmUndo: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ObsidianSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CoralRed.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = null,
                            tint = CoralRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Mark as Unpaid (Undo)?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "This will unmark '${obligation.title}' as paid, remove the ${currency.symbol}${obligation.totalAmount.toInt()} expense entry, and cancel any open split debts created for your flatmates.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Keep Paid", color = TextSecondary, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onConfirmUndo()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Undo Payment",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
