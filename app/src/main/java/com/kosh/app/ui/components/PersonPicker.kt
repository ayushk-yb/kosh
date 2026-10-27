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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.data.model.PersonEntity
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
fun PersonPickerModal(
    title: String = "Select Person",
    persons: List<PersonEntity>,
    selectedPersonIds: Set<Long>,
    isMultiSelect: Boolean = false,
    onPersonSelected: (PersonEntity) -> Unit,
    onSelectionConfirmed: (List<PersonEntity>) -> Unit = {},
    onAddNewPerson: (name: String, tag: String, phone: String?, isPinned: Boolean) -> Unit,
    onTogglePin: (personId: Long, isPinned: Boolean) -> Unit,
    onPickContact: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var currentSelectedIds by remember { mutableStateOf(selectedPersonIds) }

    val filteredPersons = remember(searchQuery, persons) {
        if (searchQuery.isBlank()) persons
        else persons.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.tag.contains(searchQuery, ignoreCase = true)
        }
    }

    val pinnedPersons = remember(persons) {
        persons.filter { it.isPinned }
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Contact Picker shortcut
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            .clickable { onPickContact() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "📱 Contacts",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    // + Add Person shortcut
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricMint.copy(alpha = 0.15f))
                            .clickable { showAddPersonDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = ElectricMint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricMint
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Quick Pinned Chips
            if (pinnedPersons.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PINNED CONTACTS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        pinnedPersons.forEach { p ->
                            val isSelected = currentSelectedIds.contains(p.id)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) ElectricMint.copy(alpha = 0.2f) else SubSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) ElectricMint else CardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isMultiSelect) {
                                            currentSelectedIds = if (isSelected) {
                                                currentSelectedIds - p.id
                                            } else {
                                                currentSelectedIds + p.id
                                            }
                                        } else {
                                            onPersonSelected(p)
                                            onDismiss()
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
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
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = p.name + if (p.tag.isNotBlank()) " (${p.tag})" else "",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) ElectricMint else TextPrimary
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = ElectricMint,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name or tag (e.g. Flatmate)...", fontSize = 13.sp, color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricMint,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Color(0xFF131A26),
                    unfocusedContainerColor = Color(0xFF131A26)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Person List
            if (filteredPersons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isBlank()) "No contacts added yet." else "No one found matching '$searchQuery'.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricMint.copy(alpha = 0.15f))
                                .clickable { showAddPersonDialog = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+ Add ${if (searchQuery.isNotBlank()) "\"$searchQuery\"" else "Person"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricMint
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPersons, key = { it.id }) { person ->
                        val isSelected = currentSelectedIds.contains(person.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) ElectricMint.copy(alpha = 0.12f) else Color(0xFF131A26))
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricMint else CardBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    if (isMultiSelect) {
                                        currentSelectedIds = if (isSelected) {
                                            currentSelectedIds - person.id
                                        } else {
                                            currentSelectedIds + person.id
                                        }
                                    } else {
                                        onPersonSelected(person)
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(person.avatarColor).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = person.name.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(person.avatarColor)
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
                                        if (person.tag.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF1C2738))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = person.tag,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = RoyalBlue
                                                )
                                            }
                                        }
                                    }
                                    if (!person.phoneNumber.isNullOrBlank()) {
                                        Text(
                                            text = person.phoneNumber,
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onTogglePin(person.id, !person.isPinned) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (person.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = "Pin Contact",
                                        tint = if (person.isPinned) AmberGold else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = ElectricMint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Multi-Select Confirmation Button
            if (isMultiSelect) {
                Button(
                    onClick = {
                        val selectedList = persons.filter { currentSelectedIds.contains(it.id) }
                        onSelectionConfirmed(selectedList)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricMint),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Confirm Selection (${currentSelectedIds.size})",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F141C),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    if (showAddPersonDialog) {
        AddPersonDialog(
            initialName = searchQuery.trim(),
            onDismiss = { showAddPersonDialog = false },
            onSave = { name, tag, phone, isPinned ->
                onAddNewPerson(name, tag, phone, isPinned)
                showAddPersonDialog = false
            },
            onPickContact = onPickContact
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPersonDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, tag: String, phone: String?, isPinned: Boolean) -> Unit,
    onPickContact: () -> Unit = {}
) {
    var name by remember { mutableStateOf(initialName) }
    var tag by remember { mutableStateOf("Friend") }
    var phone by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(true) }

    val tagOptions = listOf("Friend", "Flatmate", "Colleague", "Family", "Partner", "Landlord")

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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add New Person",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Rahul Sharma") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricMint,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Tag Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "TAG / RELATIONSHIP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tagOptions.forEach { t ->
                            val isSel = tag.equals(t, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) RoyalBlue.copy(alpha = 0.25f) else Color(0xFF131A26))
                                    .border(
                                        1.dp,
                                        if (isSel) RoyalBlue else CardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { tag = t }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = t,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color(0xFF93C5FD) else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Phone field with Contact Picker button
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Phone Number (Optional)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "📱 Link from Contacts",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricMint,
                            modifier = Modifier.clickable {
                                onPickContact()
                                onDismiss()
                            }
                        )
                    }
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = { Text("e.g. +91 98765 43210") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricMint,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Pin toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pin to Quick Access",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Show as quick chip in split dialogs",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0F141C),
                            checkedTrackColor = ElectricMint,
                            uncheckedTrackColor = Color(0xFF1E293B)
                        )
                    )
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name.trim(), tag.trim(), phone.trim().ifBlank { null }, isPinned)
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricMint),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Save Contact",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F141C),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
