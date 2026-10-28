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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.kosh.app.data.model.CustomCategoryEntity
import com.kosh.app.data.model.HouseholdObligationEntity
import com.kosh.app.data.model.PersonEntity
import com.kosh.app.data.model.TransactionCategory
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.data.repository.ParticipantSplitShare
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBackground
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ObsidianSurface
import com.kosh.app.ui.theme.PartnerPink
import com.kosh.app.ui.theme.RoyalBlue
import com.kosh.app.ui.theme.SubSurface
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionBottomSheet(
    transaction: TransactionEntity,
    currency: Currency,
    allPersons: List<PersonEntity>,
    customCategories: List<CustomCategoryEntity> = emptyList(),
    householdObligations: List<HouseholdObligationEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        category: String,
        isAssetWealth: Boolean,
        note: String,
        splitPersonName: String?,
        splitAmount: Double
    ) -> Unit,
    onSaveWithSplits: ((
        title: String,
        amount: Double,
        category: String,
        isAssetWealth: Boolean,
        note: String,
        isSplit: Boolean,
        participantSplits: List<ParticipantSplitShare>,
        unassignedSplitAmount: Double
    ) -> Unit)? = null,
    onLinkToBill: (Long) -> Unit = {},
    onDelete: () -> Unit,
    onAddNewPerson: (name: String, tag: String, phone: String?, isPinned: Boolean) -> Unit = { _, _, _, _ -> },
    onTogglePin: (personId: Long, isPinned: Boolean) -> Unit = { _, _ -> },
    onPickContact: () -> Unit = {},
    onCreateCustomCategory: (name: String, icon: String, colorHex: Long, isWealthAsset: Boolean) -> Unit = { _, _, _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(transaction.title) }
    var amountText by remember { mutableStateOf(transaction.grossAmount.toString()) }
    var linkedObligationId by remember {
        mutableStateOf(
            householdObligations.firstOrNull {
                transaction.note.contains("[Bill: ${it.title}]") ||
                it.paidTransactionId == transaction.id
            }?.id
        )
    }
    var selectedCategory by remember {
        mutableStateOf(
            TransactionCategory.values().firstOrNull {
                it.name.equals(transaction.category, ignoreCase = true) ||
                (it == TransactionCategory.PARTNER_FAMILY && (transaction.category.contains("PARTNER", ignoreCase = true) || transaction.category.contains("FAMILY", ignoreCase = true))) ||
                (it == TransactionCategory.GIFTS && transaction.category.contains("GIFT", ignoreCase = true)) ||
                (it == TransactionCategory.CAB_COMMUTE && transaction.category.contains("CAB", ignoreCase = true)) ||
                (it == TransactionCategory.SIP_WEALTH && transaction.category.contains("SIP", ignoreCase = true))
            } ?: if (customCategories.any { it.name.equals(transaction.category, ignoreCase = true) }) null else TransactionCategory.OTHER
        )
    }
    var selectedCustomCategory by remember {
        mutableStateOf(
            customCategories.firstOrNull { it.name.equals(transaction.category, ignoreCase = true) }
        )
    }
    var showPersonPicker by remember { mutableStateOf(false) }
    var showAddCategorySheet by remember { mutableStateOf(false) }
    var isAssetWealth by remember { mutableStateOf(transaction.isAssetWealth) }
    var note by remember {
        mutableStateOf(
            when {
                transaction.note.startsWith("Auto-parsed") -> ""
                transaction.note.startsWith("Partner: ") -> {
                    val after = transaction.note.removePrefix("Partner: ")
                    if (after.contains("•")) after.substringAfter("•").trim() else ""
                }
                transaction.note.startsWith("Family: ") -> {
                    val after = transaction.note.removePrefix("Family: ")
                    if (after.contains("•")) after.substringAfter("•").trim() else ""
                }
                transaction.note.startsWith("Gift for ") -> {
                    val after = transaction.note.removePrefix("Gift for ")
                    if (after.contains("•")) after.substringAfter("•").trim() else ""
                }
                else -> transaction.note
            }
        )
    }

    // Partner / Family Tagging state
    var partnerOrFamilyType by remember {
        mutableStateOf(
            if (transaction.note.contains("Family", ignoreCase = true)) "Family" else "Partner"
        )
    }
    var taggedPersonName by remember {
        mutableStateOf(
            when {
                transaction.note.startsWith("Partner: ") -> transaction.note.removePrefix("Partner: ").substringBefore("•").trim()
                transaction.note.startsWith("Family: ") -> transaction.note.removePrefix("Family: ").substringBefore("•").trim()
                transaction.note.startsWith("Gift for ") -> transaction.note.removePrefix("Gift for ").substringBefore("•").trim()
                transaction.note.contains("Partner (") -> transaction.note.substringAfter("Partner (").substringBefore(")")
                transaction.note.contains("Family (") -> transaction.note.substringAfter("Family (").substringBefore(")")
                transaction.note.contains("Partner:") -> transaction.note.substringAfter("Partner:").substringBefore("•").trim()
                transaction.note.contains("Family:") -> transaction.note.substringAfter("Family:").substringBefore("•").trim()
                else -> ""
            }
        )
    }

    // Split state
    var isSplitEnabled by remember { mutableStateOf(transaction.hasSplit) }
    var splitType by remember { mutableStateOf("GENERAL") }
    var selectedPersons by remember { mutableStateOf(emptyList<PersonEntity>()) }
    var splitMode by remember { mutableStateOf(SplitMode.EQUAL) }
    val customShares = remember { mutableStateMapOf<Long, String>() }
    var myShareText by remember {
        mutableStateOf(
            if (transaction.hasSplit && transaction.trueNetAmount > 0) transaction.trueNetAmount.toInt().toString() else ""
        )
    }
    var unassignedSplitText by remember {
        mutableStateOf(
            if (transaction.hasSplit && transaction.reimbursableAmount > 0) transaction.reimbursableAmount.toInt().toString() else ""
        )
    }

    val totalAmount = amountText.toDoubleOrNull() ?: 0.0

    // Participant splits calculation
    val participantSplits: List<ParticipantSplitShare> = remember(isSplitEnabled, splitType, selectedPersons, splitMode, customShares.toMap(), totalAmount) {
        if (!isSplitEnabled || splitType != "PEOPLE" || selectedPersons.isEmpty()) {
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
    val unassignedSplitAmount: Double = remember(isSplitEnabled, splitType, totalAmount, myShareText, unassignedSplitText) {
        if (!isSplitEnabled || splitType != "GENERAL") 0.0
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

    val totalReimbursable = remember(isSplitEnabled, splitType, participantSplits, unassignedSplitAmount) {
        if (!isSplitEnabled) 0.0
        else if (splitType == "PEOPLE") participantSplits.sumOf { it.shareAmount }
        else unassignedSplitAmount
    }

    val myTrueCost = remember(totalAmount, totalReimbursable) {
        (totalAmount - totalReimbursable).coerceAtLeast(0.0)
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                Column {
                    Text(
                        text = "Edit Transaction",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Paid via ${transaction.instrumentName}",
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

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Merchant / Title", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                )
            )

            // Gross Amount Field
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        amountText = input
                    }
                },
                label = { Text("Amount (${currency.symbol})", color = TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                )
            )

            // Category Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionCategory.values().forEach { cat ->
                        val isSelected = selectedCategory == cat && selectedCustomCategory == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ElectricMint else ObsidianSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricMint else CardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedCategory = cat
                                    selectedCustomCategory = null
                                    if (cat == TransactionCategory.SIP_WEALTH) {
                                        isAssetWealth = true
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${cat.icon} ${cat.displayName}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF0F141C) else TextSecondary
                            )
                        }
                    }

                    // Custom categories
                    customCategories.forEach { customCat ->
                        val isSelected = selectedCustomCategory?.id == customCat.id
                        val accentColor = Color(customCat.colorHex)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) accentColor else ObsidianSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) accentColor else CardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedCustomCategory = customCat
                                    selectedCategory = null
                                    if (customCat.isWealthAsset) {
                                        isAssetWealth = true
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${customCat.iconEmoji} ${customCat.name}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF0F141C) else TextSecondary
                            )
                        }
                    }

                    // + New Category Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2838))
                            .border(1.dp, RoyalBlue.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { showAddCategorySheet = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Category",
                                tint = RoyalBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "New Category",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue
                            )
                        }
                    }
                }
            }

            // Partner & Family Member Tagging Section
            if (selectedCategory == TransactionCategory.PARTNER_FAMILY) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, PartnerPink.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tag Partner / Family Expense",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PartnerPink
                            )
                            // Toggle Pill: [ ❤️ Partner ] [ 👨‍👩‍👧 Family ]
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (partnerOrFamilyType == "Partner") PartnerPink else Color.Transparent)
                                        .clickable { partnerOrFamilyType = "Partner" }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "❤️ Partner",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (partnerOrFamilyType == "Partner") Color(0xFF0F141C) else TextSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (partnerOrFamilyType == "Family") ElectricMint else Color.Transparent)
                                        .clickable { partnerOrFamilyType = "Family" }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "👨‍👩‍👧 Family",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (partnerOrFamilyType == "Family") Color(0xFF0F141C) else TextSecondary
                                    )
                                }
                            }
                        }

                        // Companion / Contact Quick Chips
                        val memberChips = remember(allPersons, partnerOrFamilyType) {
                            val fromPersons = allPersons.map { it.name }
                            val presets = if (partnerOrFamilyType == "Partner") {
                                listOf("Ananya", "Partner", "Girlfriend", "Wife")
                            } else {
                                listOf("Mom", "Dad", "Papa", "Maa", "Brother", "Sister")
                            }
                            (fromPersons + presets).distinct()
                        }

                        Text(
                            text = "Quick Tag Member:",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            memberChips.forEach { name ->
                                val isSelected = taggedPersonName.equals(name, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PartnerPink else Color(0xFF1E293B))
                                        .border(1.dp, if (isSelected) PartnerPink else CardBorder, RoundedCornerShape(8.dp))
                                        .clickable { taggedPersonName = name }
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF0F141C) else TextPrimary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = taggedPersonName,
                            onValueChange = { taggedPersonName = it },
                            label = { Text(if (partnerOrFamilyType == "Partner") "Partner's Name (e.g. Ananya)" else "Family Member (e.g. Mom, Dad)", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PartnerPink,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SubSurface,
                                unfocusedContainerColor = SubSurface
                            )
                        )
                    }
                }
            } else if (selectedCategory == TransactionCategory.GIFTS) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, Color(0xFFF472B6).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "🎁 Tag Gift Recipient / Occasion",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF472B6)
                        )

                        val giftPresets = listOf("Ananya", "Birthday", "Anniversary", "Mom", "Dad", "Diwali")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            giftPresets.forEach { preset ->
                                val isSelected = taggedPersonName.equals(preset, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFF472B6) else Color(0xFF1E293B))
                                        .border(1.dp, if (isSelected) Color(0xFFF472B6) else CardBorder, RoundedCornerShape(8.dp))
                                        .clickable { taggedPersonName = preset }
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = preset,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF0F141C) else TextPrimary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = taggedPersonName,
                            onValueChange = { taggedPersonName = it },
                            label = { Text("Gift for whom or occasion (e.g. Ananya, Birthday)", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF472B6),
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

            // Wealth / SIP Asset Toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mark as Investment / SIP",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Asset wealth — does not count towards living expense burn",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = isAssetWealth,
                        onCheckedChange = { isAssetWealth = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RoyalBlue,
                            checkedTrackColor = RoyalBlue.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }

            // Split Outlay Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Split with Friend / Group",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberGold
                            )
                            Text(
                                text = "Track money owed to you and subtract it from your net spend",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = isSplitEnabled,
                            onCheckedChange = { isSplitEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmberGold,
                                checkedTrackColor = AmberGold.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }

                    if (isSplitEnabled) {
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

            // Link to Fixed Monthly Expense Section
            if (householdObligations.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Link to Fixed Monthly Expense",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            if (linkedObligationId != null) {
                                Text(
                                    text = "Clear Link",
                                    fontSize = 11.sp,
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { linkedObligationId = null }
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            householdObligations.forEach { ob ->
                                val isSelected = linkedObligationId == ob.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ElectricMint.copy(alpha = 0.2f) else SubSurface)
                                        .border(1.dp, if (isSelected) ElectricMint else CardBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            linkedObligationId = if (isSelected) null else ob.id
                                        }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = ob.iconEmoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = ob.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ElectricMint else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notes Field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Personal Notes (Optional)", color = TextSecondary) },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                )
            )

            // Original Bank SMS Preview
            if (!transaction.originalSms.isNullOrBlank()) {
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                var copied by remember { mutableStateOf(false) }

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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📩 ORIGINAL BANK SMS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (copied) "Copied!" else "Copy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricMint,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(transaction.originalSms))
                                    copied = true
                                }
                            )
                        }
                        Text(
                            text = transaction.originalSms,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

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
                        val parsedAmount = amountText.toDoubleOrNull() ?: transaction.grossAmount

                        var finalNote = when {
                            selectedCategory == TransactionCategory.PARTNER_FAMILY -> {
                                val tagPrefix = if (taggedPersonName.isNotBlank()) {
                                    "$partnerOrFamilyType: ${taggedPersonName.trim()}"
                                } else {
                                    "$partnerOrFamilyType Expense"
                                }
                                val userNoteClean = note.trim()
                                if (userNoteClean.isBlank()) {
                                    tagPrefix
                                } else {
                                    "$tagPrefix • $userNoteClean"
                                }
                            }
                            selectedCategory == TransactionCategory.GIFTS -> {
                                val tagPrefix = if (taggedPersonName.isNotBlank()) {
                                    "Gift for ${taggedPersonName.trim()}"
                                } else {
                                    "Gift"
                                }
                                val userNoteClean = note.trim()
                                if (userNoteClean.isBlank()) {
                                    tagPrefix
                                } else {
                                    "$tagPrefix • $userNoteClean"
                                }
                            }
                            else -> note.trim()
                        }

                        val linkedOb = householdObligations.firstOrNull { it.id == linkedObligationId }
                        if (linkedOb != null) {
                            if (!finalNote.contains("[Bill: ${linkedOb.title}]")) {
                                finalNote = if (finalNote.isBlank()) "[Bill: ${linkedOb.title}]" else "[Bill: ${linkedOb.title}] $finalNote"
                            }
                            onLinkToBill(linkedOb.id)
                        }

                        val finalCategory = selectedCustomCategory?.name ?: selectedCategory?.name ?: transaction.category
                        val finalWealth = selectedCustomCategory?.isWealthAsset ?: isAssetWealth

                        if (onSaveWithSplits != null) {
                            onSaveWithSplits(
                                title.trim().ifEmpty { transaction.title },
                                parsedAmount,
                                finalCategory,
                                finalWealth,
                                finalNote,
                                isSplitEnabled,
                                participantSplits,
                                unassignedSplitAmount
                            )
                        } else {
                            val splitPerson = if (isSplitEnabled) {
                                if (splitType == "PEOPLE" && selectedPersons.isNotEmpty()) {
                                    selectedPersons.joinToString(", ") { it.name }
                                } else {
                                    "General Split"
                                }
                            } else null
                            val splitAmt = if (isSplitEnabled) totalReimbursable else 0.0

                            onSave(
                                title.trim().ifEmpty { transaction.title },
                                parsedAmount,
                                finalCategory,
                                finalWealth,
                                finalNote,
                                splitPerson,
                                splitAmt
                            )
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricMint,
                        contentColor = Color(0xFF0F141C)
                    ),
                    shape = RoundedCornerShape(14.dp)
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

    // Modal Person Picker
    if (showPersonPicker) {
        val selectedIds = selectedPersons.map { it.id }.toSet()
        PersonPickerModal(
            title = "Select People to Split With",
            persons = allPersons,
            selectedPersonIds = selectedIds,
            isMultiSelect = true,
            onSelectionConfirmed = { selected ->
                selectedPersons = selected
                showPersonPicker = false
            },
            onPersonSelected = { p ->
                if (!selectedPersons.any { it.id == p.id }) {
                    selectedPersons = selectedPersons + p
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

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = "Delete Transaction?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${transaction.title}'? It will be removed permanently and will NEVER come back even after scanning SMS again.",
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
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
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
