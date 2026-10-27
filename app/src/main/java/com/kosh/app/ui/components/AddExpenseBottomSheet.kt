package com.kosh.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.mutableStateMapOf
import com.kosh.app.data.repository.ParticipantSplitShare
import com.kosh.app.data.model.Currency
import com.kosh.app.data.model.CustomCategoryEntity
import com.kosh.app.data.model.PersonEntity
import com.kosh.app.data.model.TransactionCategory
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBackground
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
fun AddExpenseBottomSheet(
    currency: Currency,
    allPersons: List<PersonEntity> = emptyList(),
    customCategories: List<CustomCategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSaveExpense: (
        title: String,
        amount: Double,
        category: TransactionCategory,
        instrument: String,
        isSplit: Boolean,
        personName: String,
        splitAmount: Double
    ) -> Unit,
    onSaveExpenseCustomCategory: (
        title: String,
        amount: Double,
        categoryName: String,
        instrument: String,
        isSplit: Boolean,
        personName: String,
        splitAmount: Double,
        isAssetWealth: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSaveExpenseWithSplits: (
        title: String,
        amount: Double,
        categoryName: String,
        instrument: String,
        isSplit: Boolean,
        participantSplits: List<ParticipantSplitShare>,
        unassignedSplitAmount: Double,
        isWealthAsset: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onAddNewPerson: (name: String, tag: String, phone: String?, isPinned: Boolean) -> Unit = { _, _, _, _ -> },
    onTogglePin: (personId: Long, isPinned: Boolean) -> Unit = { _, _ -> },
    onPickContact: () -> Unit = {},
    onCreateCustomCategory: (name: String, icon: String, colorHex: Long, isWealthAsset: Boolean) -> Unit = { _, _, _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var amountText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TransactionCategory?>(TransactionCategory.SHOPPING) }
    var selectedCustomCategory by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    var isSplit by remember { mutableStateOf(false) }
    var splitType by remember { mutableStateOf("PEOPLE") } // "PEOPLE" or "GENERAL"
    var selectedPersons by remember { mutableStateOf<List<PersonEntity>>(emptyList()) }
    var splitMode by remember { mutableStateOf(SplitMode.EQUAL) }
    val customShares = remember { mutableStateMapOf<Long, String>() }
    var myShareText by remember { mutableStateOf("") }
    var unassignedSplitText by remember { mutableStateOf("") }
    var showAddCategorySheet by remember { mutableStateOf(false) }
    var showPersonPicker by remember { mutableStateOf(false) }
    val instrumentName = "Manual Entry"

    val totalAmount = amountText.toDoubleOrNull() ?: 0.0

    // Participant splits calculation
    val participantSplits: List<ParticipantSplitShare> = remember(isSplit, splitType, selectedPersons, splitMode, customShares.toMap(), totalAmount) {
        if (!isSplit || splitType != "PEOPLE" || selectedPersons.isEmpty()) {
            emptyList()
        } else when (splitMode) {
            SplitMode.SOLO -> emptyList()
            SplitMode.EQUAL -> {
                val count = selectedPersons.size + 1
                val perPerson = if (count > 0 && totalAmount > 0) totalAmount / count else 0.0
                selectedPersons.map { p ->
                    ParticipantSplitShare(p.id, p.name, perPerson)
                }
            }
            SplitMode.CUSTOM -> {
                selectedPersons.map { p ->
                    val entered = customShares[p.id]?.toDoubleOrNull() ?: 0.0
                    ParticipantSplitShare(p.id, p.name, entered)
                }
            }
        }
    }

    // Unassigned split amount calculation
    val unassignedSplitAmount: Double = remember(isSplit, splitType, totalAmount, myShareText, unassignedSplitText) {
        if (!isSplit || splitType != "GENERAL") 0.0
        else {
            val enteredSplit = unassignedSplitText.toDoubleOrNull()
            if (enteredSplit != null) enteredSplit
            else {
                val enteredMyShare = myShareText.toDoubleOrNull()
                if (enteredMyShare != null) (totalAmount - enteredMyShare).coerceAtLeast(0.0)
                else (totalAmount / 2.0)
            }
        }
    }

    val totalReimbursable = remember(isSplit, splitType, participantSplits, unassignedSplitAmount) {
        if (!isSplit) 0.0
        else if (splitType == "PEOPLE") participantSplits.sumOf { it.shareAmount }
        else unassignedSplitAmount
    }

    val myTrueCost = remember(totalAmount, totalReimbursable) {
        (totalAmount - totalReimbursable).coerceAtLeast(0.0)
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Log Expense with Split",
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

            // Title input
            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text("Title / Description", color = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            // Amount input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount Spent", color = TextSecondary) },
                leadingIcon = {
                    Text(
                        currency.symbol,
                        color = ElectricMint,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            // Category Chips (Built-in + Custom + Add New)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CATEGORY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Standard categories
                    items(TransactionCategory.values()) { cat ->
                        val isSelected = selectedCategory == cat && selectedCustomCategory == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ElectricMint.copy(alpha = 0.2f) else CardBackground)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ElectricMint else CardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedCategory = cat
                                    selectedCustomCategory = null
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${cat.icon} ${cat.displayName}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ElectricMint else TextSecondary
                            )
                        }
                    }

                    // Custom categories
                    items(customCategories) { customCat ->
                        val isSelected = selectedCustomCategory?.id == customCat.id
                        val accentColor = Color(customCat.colorHex)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) accentColor.copy(alpha = 0.2f) else CardBackground)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) accentColor else CardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedCustomCategory = customCat
                                    selectedCategory = null
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${customCat.iconEmoji} ${customCat.name}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) accentColor else TextSecondary
                            )
                        }
                    }

                    // + New Category Chip
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E2838))
                                .border(1.dp, RoyalBlue.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .clickable { showAddCategorySheet = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Category",
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "New Category",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue
                                )
                            }
                        }
                    }
                }
            }

            // Split Toggle Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF101724))
                    .border(width = 1.dp, color = AmberGold.copy(alpha = 0.35f), shape = RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Split This Expense?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                            Text(
                                text = "Track receivables & settled balances",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = isSplit,
                            onCheckedChange = { isSplit = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0F141C),
                                checkedTrackColor = AmberGold
                            )
                        )
                    }

                    if (isSplit) {
                        // Split Target Selector: With People vs General Split (My Share only)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F1521))
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (splitType == "PEOPLE") AmberGold else Color.Transparent)
                                    .clickable { splitType = "PEOPLE" }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedPersons.isEmpty()) "👥 Select People" else "👥 People (${selectedPersons.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (splitType == "PEOPLE") Color(0xFF0F141C) else TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (splitType == "GENERAL") AmberGold else Color.Transparent)
                                    .clickable { splitType = "GENERAL" }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚡ My Share Only",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (splitType == "GENERAL") Color(0xFF0F141C) else TextSecondary
                                )
                            }
                        }

                        if (splitType == "PEOPLE") {
                            // People Splitting Mode
                            if (selectedPersons.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E2838))
                                        .border(1.dp, AmberGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clickable { showPersonPicker = true }
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = AmberGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Select People to Split With (Multi-select)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberGold
                                        )
                                    }
                                }
                            } else {
                                // Selected People Header & Change Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedPersons.size} People Selected",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF162032))
                                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                            .clickable { showPersonPicker = true }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "+ Add / Edit",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberGold
                                        )
                                    }
                                }

                                // Equal vs Custom mode tabs
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SubSurface)
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (splitMode == SplitMode.EQUAL) ElectricMint else Color.Transparent)
                                            .clickable { splitMode = SplitMode.EQUAL }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Equal (${selectedPersons.size + 1} shares)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (splitMode == SplitMode.EQUAL) Color(0xFF0F141C) else TextSecondary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (splitMode == SplitMode.CUSTOM) ElectricMint else Color.Transparent)
                                            .clickable {
                                                splitMode = SplitMode.CUSTOM
                                                for (p in selectedPersons) {
                                                    if (customShares[p.id].isNullOrBlank() && totalAmount > 0) {
                                                        customShares[p.id] = (totalAmount / (selectedPersons.size + 1)).toInt().toString()
                                                    }
                                                }
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Custom Shares",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (splitMode == SplitMode.CUSTOM) Color(0xFF0F141C) else TextSecondary
                                        )
                                    }
                                }

                                if (splitMode == SplitMode.EQUAL) {
                                    val count = selectedPersons.size + 1
                                    val perShare = if (count > 0 && totalAmount > 0) totalAmount / count else 0.0
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(ElectricMint.copy(alpha = 0.15f))
                                                        .border(1.dp, ElectricMint.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("👤 You (${currency.symbol}${perShare.toInt()})", fontSize = 11.sp, color = ElectricMint, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                            items(selectedPersons) { p ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF1E2838))
                                                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("${p.name} (${currency.symbol}${perShare.toInt()})", fontSize = 11.sp, color = TextPrimary)
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Each person pays ${currency.symbol}${perShare.toInt()} (split across $count shares)",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                } else {
                                    // Custom Shares input for each selected person
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        selectedPersons.forEach { p ->
                                            val currentVal = customShares[p.id] ?: if (totalAmount > 0) (totalAmount / (selectedPersons.size + 1)).toInt().toString() else ""
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF131A26))
                                                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(AmberGold.copy(alpha = 0.2f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(p.name.take(1).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                                }

                                                OutlinedTextField(
                                                    value = currentVal,
                                                    onValueChange = { customShares[p.id] = it },
                                                    leadingIcon = {
                                                        Text(currency.symbol, color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.width(110.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = AmberGold,
                                                        unfocusedBorderColor = CardBorder,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary,
                                                        focusedContainerColor = SubSurface,
                                                        unfocusedContainerColor = SubSurface
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "💡 Your net cost: ${currency.symbol}${myTrueCost.toInt()} • Expected from others: ${currency.symbol}${totalReimbursable.toInt()}",
                                    fontSize = 11.sp,
                                    color = ElectricMint,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            // General Split (Unassigned / My Share only)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF141F30))
                                        .border(1.dp, RoyalBlue.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "ℹ️ Open Split: No specific person required. Enter your personal share below. The remainder will be saved under 'General Split' to be settled partially or completely through one or multiple payments.",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = myShareText,
                                        onValueChange = { input ->
                                            myShareText = input
                                            val entered = input.toDoubleOrNull()
                                            if (entered != null && totalAmount > 0) {
                                                unassignedSplitText = (totalAmount - entered).coerceAtLeast(0.0).toInt().toString()
                                            }
                                        },
                                        label = { Text("My Share", color = TextSecondary, fontSize = 11.sp) },
                                        leadingIcon = {
                                            Text(currency.symbol, color = ElectricMint, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                                        },
                                        placeholder = {
                                            if (totalAmount > 0) Text("${(totalAmount / 2).toInt()}", color = TextMuted, fontSize = 12.sp)
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ElectricMint,
                                            unfocusedBorderColor = CardBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedContainerColor = SubSurface,
                                            unfocusedContainerColor = SubSurface
                                        )
                                    )

                                    OutlinedTextField(
                                        value = unassignedSplitText,
                                        onValueChange = { input ->
                                            unassignedSplitText = input
                                            val entered = input.toDoubleOrNull()
                                            if (entered != null && totalAmount > 0) {
                                                myShareText = (totalAmount - entered).coerceAtLeast(0.0).toInt().toString()
                                            }
                                        },
                                        label = { Text("Split / Return", color = TextSecondary, fontSize = 11.sp) },
                                        leadingIcon = {
                                            Text(currency.symbol, color = AmberGold, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                                        },
                                        placeholder = {
                                            if (totalAmount > 0) Text("${(totalAmount / 2).toInt()}", color = TextMuted, fontSize = 12.sp)
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AmberGold,
                                            unfocusedBorderColor = CardBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedContainerColor = SubSurface,
                                            unfocusedContainerColor = SubSurface
                                        )
                                    )
                                }

                                Text(
                                    text = "💡 Your net cost: ${currency.symbol}${myTrueCost.toInt()} • Open split: ${currency.symbol}${unassignedSplitAmount.toInt()}",
                                    fontSize = 11.sp,
                                    color = ElectricMint,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Save CTA Button
            Button(
                onClick = {
                    if (totalAmount > 0) {
                        val chosenCustom = selectedCustomCategory
                        val catName = chosenCustom?.name ?: selectedCategory?.name ?: "OTHER"
                        val isWealth = chosenCustom?.isWealthAsset ?: (selectedCategory == TransactionCategory.SIP_WEALTH)
                        val title = titleText.ifBlank { chosenCustom?.name ?: selectedCategory?.displayName ?: "Expense" }

                        onSaveExpenseWithSplits(
                            title,
                            totalAmount,
                            catName,
                            instrumentName,
                            isSplit,
                            participantSplits,
                            unassignedSplitAmount,
                            isWealth
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricMint,
                    contentColor = Color(0xFF0F141C)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isSplit) "Save & Track Split" else "Save Expense",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Modal Person Picker (Multi-select)
    if (showPersonPicker) {
        PersonPickerModal(
            title = "Select People to Split With",
            persons = allPersons,
            selectedPersonIds = selectedPersons.map { it.id }.toSet(),
            isMultiSelect = true,
            onPersonSelected = { p ->
                selectedPersons = listOf(p)
                if (totalAmount > 0) {
                    customShares[p.id] = (totalAmount / 2).toInt().toString()
                }
                showPersonPicker = false
            },
            onSelectionConfirmed = { list ->
                selectedPersons = list
                for (p in list) {
                    if (customShares[p.id].isNullOrBlank() && totalAmount > 0) {
                        customShares[p.id] = (totalAmount / (list.size + 1)).toInt().toString()
                    }
                }
                showPersonPicker = false
            },
            onAddNewPerson = onAddNewPerson,
            onTogglePin = onTogglePin,
            onPickContact = onPickContact,
            onDismiss = { showPersonPicker = false }
        )
    }

    // Add Custom Category Bottom Sheet
    if (showAddCategorySheet) {
        AddCategoryBottomSheet(
            onDismiss = { showAddCategorySheet = false },
            onSaveCategory = { name, icon, colorHex, isWealth ->
                onCreateCustomCategory(name, icon, colorHex, isWealth)
                showAddCategorySheet = false
            }
        )
    }
}
