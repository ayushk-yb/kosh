package com.kosh.app.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.data.model.Currency
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.data.model.isCreditOrInflow
import com.kosh.app.data.model.displayAmount
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBackground
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ObsidianBg
import com.kosh.app.ui.theme.PartnerPink
import com.kosh.app.ui.theme.RoyalBlue
import com.kosh.app.ui.theme.SubSurface
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TransactionFilter {
    ALL,
    SPENDS,
    INVESTMENTS,
    SPLITS
}

@Composable
fun AllTransactionsScreen(
    currency: Currency,
    monthFormattedTitle: String,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }

    // Filtered transaction list
    val filteredTransactions = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter { tx ->
            val matchesFilter = when (selectedFilter) {
                TransactionFilter.ALL -> true
                TransactionFilter.SPENDS -> !tx.isAssetWealth && !tx.hasSplit && !tx.isCreditOrInflow()
                TransactionFilter.INVESTMENTS -> tx.isAssetWealth
                TransactionFilter.SPLITS -> tx.hasSplit || tx.reimbursableAmount > 0
            }

            val query = searchQuery.trim().lowercase()
            val matchesQuery = query.isEmpty() ||
                    tx.title.lowercase().contains(query) ||
                    tx.category.lowercase().contains(query) ||
                    tx.instrumentName.lowercase().contains(query) ||
                    tx.note.lowercase().contains(query) ||
                    tx.grossAmount.toString().contains(query)

            matchesFilter && matchesQuery
        }
    }

    val totalOutflow = remember(filteredTransactions) {
        filteredTransactions.filter { !it.isAssetWealth && !it.isCreditOrInflow() }.sumOf { it.trueNetAmount }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF162032))
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "All Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = monthFormattedTitle,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SubSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search by merchant, card, category...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(ElectricMint),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { searchQuery = "" }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val chips = listOf(
                TransactionFilter.ALL to "All (${transactions.size})",
                TransactionFilter.SPENDS to "Spends",
                TransactionFilter.INVESTMENTS to "Investments / SIP",
                TransactionFilter.SPLITS to "Group Splits"
            )

            items(chips) { (filter, label) ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) ElectricMint else Color(0xFF162032))
                        .border(
                            1.dp,
                            if (isSelected) ElectricMint else CardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF0F141C) else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Summary Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF131A26))
                .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredTransactions.size} transactions found",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Text(
                text = "Total Outflow: ${currency.symbol}${totalOutflow.toInt()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction List
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No matching transactions found",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your search or filters",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    AllTransactionRowItem(
                        currency = currency,
                        transaction = tx,
                        onClick = { onTransactionClick(tx) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun AllTransactionRowItem(
    currency: Currency,
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    val dateStr = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(transaction.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val isCredit = transaction.isCreditOrInflow()
                // Category Icon Indicator
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCredit -> ElectricMint.copy(alpha = 0.2f)
                                transaction.isAssetWealth -> RoyalBlue.copy(alpha = 0.2f)
                                transaction.hasSplit -> AmberGold.copy(alpha = 0.2f)
                                transaction.category.contains("PARTNER", ignoreCase = true) -> PartnerPink.copy(alpha = 0.2f)
                                transaction.category.contains("GIFT", ignoreCase = true) -> Color(0xFFF472B6).copy(alpha = 0.2f)
                                else -> ElectricMint.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            isCredit -> "💰"
                            transaction.isAssetWealth -> "📈"
                            transaction.hasSplit -> "👥"
                            transaction.category.contains("CAB", ignoreCase = true) -> "🚕"
                            transaction.category.contains("GIFT", ignoreCase = true) -> "🎁"
                            transaction.category.contains("PARTNER", ignoreCase = true) -> "👨‍👩‍👧"
                            transaction.category.contains("COOK", ignoreCase = true) -> "🍳"
                            transaction.category.contains("FOOD", ignoreCase = true) || transaction.category.contains("GROCERIES", ignoreCase = true) -> "🛒"
                            transaction.category.contains("RENT", ignoreCase = true) -> "🏠"
                            transaction.category.contains("UTILITIES", ignoreCase = true) -> "⚡"
                            transaction.category.contains("SHOPPING", ignoreCase = true) -> "🛍️"
                            else -> "💳"
                        },
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transaction.instrumentName} • $dateStr",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    if (transaction.category.contains("PARTNER", ignoreCase = true) || transaction.note.contains("Partner") || transaction.note.contains("Family") || transaction.note.contains("Girlfriend")) {
                        val isFamily = transaction.note.contains("Family", ignoreCase = true)
                        val badgeColor = if (isFamily) ElectricMint else PartnerPink
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.18f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            val tagText = when {
                                transaction.note.isNotBlank() && !transaction.note.startsWith("Auto-parsed") -> {
                                    when {
                                        transaction.note.startsWith("Partner: ") -> "❤️ " + transaction.note
                                        transaction.note.startsWith("Family: ") -> "👨‍👩‍👧 " + transaction.note
                                        else -> transaction.note
                                    }
                                }
                                isFamily -> "👨‍👩‍👧 Family Expense"
                                else -> "❤️ Partner & Family"
                            }
                            Text(
                                text = tagText,
                                fontSize = 9.sp,
                                color = badgeColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (transaction.category.contains("GIFT", ignoreCase = true) || transaction.note.startsWith("Gift")) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF472B6).copy(alpha = 0.18f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            val tagText = when {
                                transaction.note.isNotBlank() && !transaction.note.startsWith("Auto-parsed") -> {
                                    if (transaction.note.startsWith("🎁")) transaction.note else "🎁 ${transaction.note}"
                                }
                                else -> "🎁 Gift"
                            }
                            Text(
                                text = tagText,
                                fontSize = 9.sp,
                                color = Color(0xFFF472B6),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (transaction.note.isNotBlank() && !transaction.note.startsWith("Auto-parsed")) {
                        Text(
                            text = transaction.note,
                            fontSize = 10.sp,
                            color = AmberGold
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (transaction.isCreditOrInflow()) {
                    val amount = transaction.displayAmount()
                    Text(
                        text = "+${currency.symbol}${amount.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricMint
                    )
                    Text(
                        text = "Credit / Inflow",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricMint.copy(alpha = 0.85f)
                    )
                } else {
                    Text(
                        text = "${currency.symbol}${transaction.trueNetAmount.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.isAssetWealth) RoyalBlue else TextPrimary
                    )
                    if (transaction.hasSplit && transaction.reimbursableAmount > 0) {
                        Text(
                            text = "${currency.symbol}${transaction.reimbursableAmount.toInt()} back",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberGold
                        )
                    } else if (transaction.isAssetWealth) {
                        Text(
                            text = "Asset / SIP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = RoyalBlue
                        )
                    }
                }
            }
        }
    }
}
