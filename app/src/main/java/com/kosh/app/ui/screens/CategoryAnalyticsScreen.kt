package com.kosh.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.data.model.Currency
import com.kosh.app.data.model.TransactionCategory
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBackground
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ObsidianBg
import com.kosh.app.ui.theme.ObsidianSurface
import com.kosh.app.ui.theme.PartnerPink
import com.kosh.app.ui.theme.PurpleAccent
import com.kosh.app.ui.theme.RoyalBlue
import com.kosh.app.ui.theme.SubSurface
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class CategorySpendData(
    val categoryKey: String,
    val category: TransactionCategory? = null,
    val displayName: String,
    val icon: String,
    val currentSpend: Double,
    val baselineSpend: Double,
    val percentageOfTotal: Float,
    val deltaPercent: Double?,
    val accentColor: Color,
    val transactions: List<TransactionEntity>,
    val isWealthAsset: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryAnalyticsScreen(
    currency: Currency,
    selectedMonthTitle: String,
    currentTransactions: List<TransactionEntity>,
    comparisonMonthTitle: String,
    comparisonTransactions: List<TransactionEntity>,
    selectedYearMonth: YearMonth,
    comparisonYearMonth: YearMonth,
    customCategories: List<com.kosh.app.data.model.CustomCategoryEntity> = emptyList(),
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSetComparisonToPreviousMonth: () -> Unit,
    onSetComparisonToTwoMonthsAgo: () -> Unit,
    onSelectComparisonMonth: (YearMonth) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedCategoryId by remember { mutableStateOf<String?>(null) }
    var showCustomMonthPicker by remember { mutableStateOf(false) }

    // 1. Calculate Monthly Outflows
    val currentNetSpend = remember(currentTransactions) {
        currentTransactions.filter { !it.isAssetWealth }.sumOf { it.trueNetAmount }
    }
    val baselineNetSpend = remember(comparisonTransactions) {
        comparisonTransactions.filter { !it.isAssetWealth }.sumOf { it.trueNetAmount }
    }

    val overallSpendDelta = currentNetSpend - baselineNetSpend
    val overallPercentDelta: Double? = if (baselineNetSpend > 0) {
        ((currentNetSpend - baselineNetSpend) / baselineNetSpend) * 100
    } else null

    // 2. Aggregate Category Breakdown
    val categoryBreakdowns = remember(currentTransactions, comparisonTransactions, currentNetSpend, customCategories) {
        val categories = TransactionCategory.values()
        val standardItems = categories.map { cat ->
            val curTxList = currentTransactions.filter { tx ->
                when (cat) {
                    TransactionCategory.OTHER -> {
                        tx.category.equals("OTHER", ignoreCase = true) ||
                        tx.category.isBlank() ||
                        (categories.none { it != TransactionCategory.OTHER && tx.category.equals(it.name, ignoreCase = true) } &&
                         customCategories.none { tx.category.equals(it.name, ignoreCase = true) })
                    }
                    else -> tx.category.equals(cat.name, ignoreCase = true)
                }
            }

            val baseTxList = comparisonTransactions.filter { tx ->
                when (cat) {
                    TransactionCategory.OTHER -> {
                        tx.category.equals("OTHER", ignoreCase = true) ||
                        tx.category.isBlank() ||
                        (categories.none { it != TransactionCategory.OTHER && tx.category.equals(it.name, ignoreCase = true) } &&
                         customCategories.none { tx.category.equals(it.name, ignoreCase = true) })
                    }
                    else -> tx.category.equals(cat.name, ignoreCase = true)
                }
            }

            val curAmount = if (cat == TransactionCategory.SIP_WEALTH) {
                curTxList.sumOf { it.grossAmount }
            } else {
                curTxList.sumOf { it.trueNetAmount }
            }

            val baseAmount = if (cat == TransactionCategory.SIP_WEALTH) {
                baseTxList.sumOf { it.grossAmount }
            } else {
                baseTxList.sumOf { it.trueNetAmount }
            }

            val percentage = if (currentNetSpend > 0 && cat != TransactionCategory.SIP_WEALTH) {
                (curAmount / currentNetSpend).toFloat().coerceIn(0f, 1f)
            } else 0f

            val delta: Double? = if (baseAmount > 0) {
                ((curAmount - baseAmount) / baseAmount) * 100
            } else if (curAmount > 0) 100.0 else null

            val accent = when (cat) {
                TransactionCategory.RENT -> Color(0xFF6366F1)
                TransactionCategory.CAB_COMMUTE -> AmberGold
                TransactionCategory.COOK_MAID -> Color(0xFFFB923C)
                TransactionCategory.GIFTS -> Color(0xFFF472B6)
                TransactionCategory.PARTNER_FAMILY -> PartnerPink
                TransactionCategory.SIP_WEALTH -> RoyalBlue
                TransactionCategory.GROCERIES -> ElectricMint
                TransactionCategory.UTILITIES -> Color(0xFFFACC15)
                TransactionCategory.SHOPPING -> PurpleAccent
                TransactionCategory.OTHER -> Color(0xFF94A3B8)
            }

            CategorySpendData(
                categoryKey = cat.name,
                category = cat,
                displayName = cat.displayName,
                icon = cat.icon,
                currentSpend = curAmount,
                baselineSpend = baseAmount,
                percentageOfTotal = percentage,
                deltaPercent = delta,
                accentColor = accent,
                transactions = curTxList.sortedByDescending { it.timestamp },
                isWealthAsset = cat == TransactionCategory.SIP_WEALTH
            )
        }

        val customItems = customCategories.map { customCat ->
            val curTxList = currentTransactions.filter { tx ->
                tx.category.equals(customCat.name, ignoreCase = true)
            }
            val baseTxList = comparisonTransactions.filter { tx ->
                tx.category.equals(customCat.name, ignoreCase = true)
            }

            val curAmount = if (customCat.isWealthAsset) {
                curTxList.sumOf { it.grossAmount }
            } else {
                curTxList.sumOf { it.trueNetAmount }
            }

            val baseAmount = if (customCat.isWealthAsset) {
                baseTxList.sumOf { it.grossAmount }
            } else {
                baseTxList.sumOf { it.trueNetAmount }
            }

            val percentage = if (currentNetSpend > 0 && !customCat.isWealthAsset) {
                (curAmount / currentNetSpend).toFloat().coerceIn(0f, 1f)
            } else 0f

            val delta: Double? = if (baseAmount > 0) {
                ((curAmount - baseAmount) / baseAmount) * 100
            } else if (curAmount > 0) 100.0 else null

            CategorySpendData(
                categoryKey = "CUSTOM_${customCat.id}",
                category = null,
                displayName = customCat.name,
                icon = customCat.iconEmoji,
                currentSpend = curAmount,
                baselineSpend = baseAmount,
                percentageOfTotal = percentage,
                deltaPercent = delta,
                accentColor = Color(customCat.colorHex),
                transactions = curTxList.sortedByDescending { it.timestamp },
                isWealthAsset = customCat.isWealthAsset
            )
        }

        (standardItems + customItems).sortedWith(compareByDescending<CategorySpendData> { it.currentSpend > 0 }.thenByDescending { it.currentSpend })
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Header: Category Breakdown + Month Navigator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MONTHLY SPEND BREAKDOWN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricMint,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Category Analytics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Month Selector Carousel
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onPreviousMonth, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = selectedMonthTitle.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(onClick = onNextMonth, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Comparison Control Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "COMPARE AGAINST",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )

                val isPrevSelected = comparisonYearMonth == selectedYearMonth.minusMonths(1)
                val isTwoMonthsSelected = comparisonYearMonth == selectedYearMonth.minusMonths(2)
                val isCustomSelected = !isPrevSelected && !isTwoMonthsSelected

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: Previous Month
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isPrevSelected) ElectricMint else SubSurface)
                            .border(1.dp, if (isPrevSelected) ElectricMint else CardBorder, RoundedCornerShape(10.dp))
                            .clickable { onSetComparisonToPreviousMonth() }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "vs Last Month (${selectedYearMonth.minusMonths(1).month.name.take(3)})",
                            fontSize = 11.sp,
                            fontWeight = if (isPrevSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPrevSelected) Color(0xFF0F141C) else TextSecondary
                        )
                    }

                    // Option 2: 2 Months Ago
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isTwoMonthsSelected) ElectricMint else SubSurface)
                            .border(1.dp, if (isTwoMonthsSelected) ElectricMint else CardBorder, RoundedCornerShape(10.dp))
                            .clickable { onSetComparisonToTwoMonthsAgo() }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "vs 2 Months Ago (${selectedYearMonth.minusMonths(2).month.name.take(3)})",
                            fontSize = 11.sp,
                            fontWeight = if (isTwoMonthsSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTwoMonthsSelected) Color(0xFF0F141C) else TextSecondary
                        )
                    }

                    // Option 3: Custom Month Picker
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCustomSelected) ElectricMint else SubSurface)
                            .border(1.dp, if (isCustomSelected) ElectricMint else CardBorder, RoundedCornerShape(10.dp))
                            .clickable { showCustomMonthPicker = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Custom Month",
                                modifier = Modifier.size(13.dp),
                                tint = if (isCustomSelected) Color(0xFF0F141C) else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCustomSelected) "vs ${comparisonMonthTitle.take(8)}" else "Custom Month...",
                                fontSize = 11.sp,
                                fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCustomSelected) Color(0xFF0F141C) else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Hero Spend Delta Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF131B2A), Color(0xFF0B101A))
                        )
                    )
                    .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "${selectedMonthTitle.uppercase()} TOTAL SPEND",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currency.symbol}${currentNetSpend.toInt()}",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Baseline (${comparisonMonthTitle}): ${currency.symbol}${baselineNetSpend.toInt()}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        // Delta Pill
                        if (overallPercentDelta != null) {
                            val isHigher = overallPercentDelta > 0
                            val deltaColor = if (isHigher) Color(0xFFEF4444) else ElectricMint
                            val deltaBg = if (isHigher) Color(0x33EF4444) else Color(0x3310B981)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(deltaBg)
                                    .border(1.dp, deltaColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isHigher) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = deltaColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${if (isHigher) "+" else ""}${String.format(Locale.US, "%.1f", overallPercentDelta)}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = deltaColor
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SubSurface)
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "No Baseline",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // Explanatory Subtitle
                    val diffAmt = abs(overallSpendDelta).toInt()
                    val summaryText = when {
                        overallPercentDelta == null -> "No recorded spend in baseline month to compare against."
                        overallSpendDelta > 0 -> "You have spent ${currency.symbol}$diffAmt more than in $comparisonMonthTitle."
                        overallSpendDelta < 0 -> "Fantastic pacing! You saved ${currency.symbol}$diffAmt compared to $comparisonMonthTitle."
                        else -> "Spend is exactly identical to $comparisonMonthTitle."
                    }

                    Text(
                        text = summaryText,
                        fontSize = 12.sp,
                        color = if (overallSpendDelta > 0) Color(0xFFFCA5A5) else ElectricMint,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Section Title: Category Breakdown List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CATEGORIZED SPEND (${categoryBreakdowns.count { it.currentSpend > 0 }} ACTIVE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Tap to Inspect",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Category Cards
        items(categoryBreakdowns, key = { it.categoryKey }) { itemData ->
            val isExpanded = expandedCategoryId == itemData.categoryKey

            CategoryCardItem(
                data = itemData,
                currency = currency,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedCategoryId = if (isExpanded) null else itemData.categoryKey
                },
                onTransactionClick = onTransactionClick
            )
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    // Custom Month Picker Modal Dialog
    if (showCustomMonthPicker) {
        val currentYm = YearMonth.now()
        val availableMonths = remember(currentYm) {
            (0..11).map { currentYm.minusMonths(it.toLong()) }
        }

        AlertDialog(
            onDismissRequest = { showCustomMonthPicker = false },
            title = {
                Text(
                    text = "Select Comparison Month",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableMonths.forEach { ym ->
                        val isCurrent = ym == comparisonYearMonth
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) ElectricMint.copy(alpha = 0.2f) else ObsidianSurface)
                                .border(1.dp, if (isCurrent) ElectricMint else CardBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectComparisonMonth(ym)
                                    showCustomMonthPicker = false
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${ym.month.name} ${ym.year}",
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) ElectricMint else TextPrimary
                                )
                                if (isCurrent) {
                                    Text(
                                        text = "Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricMint
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCustomMonthPicker = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = SubSurface
        )
    }
}

@Composable
fun CategoryCardItem(
    data: CategorySpendData,
    currency: Currency,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    val progressAnim by animateFloatAsState(
        targetValue = data.percentageOfTotal,
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, if (isExpanded) data.accentColor.copy(alpha = 0.5f) else CardBorder.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .clickable { onToggleExpand() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top Row: Icon + Name + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(data.accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = data.icon, fontSize = 18.sp)
                    }

                    Column {
                        Text(
                            text = data.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${(data.percentageOfTotal * 100).toInt()}% of monthly burn • ${data.transactions.size} txns",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${currency.symbol}${data.currentSpend.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    // Month-over-Month Delta badge
                    if (data.deltaPercent != null) {
                        val isUp = data.deltaPercent > 0
                        val badgeColor = if (data.isWealthAsset || data.category == TransactionCategory.SIP_WEALTH) {
                            if (isUp) ElectricMint else Color(0xFFEF4444)
                        } else {
                            if (isUp) Color(0xFFEF4444) else ElectricMint
                        }

                        Text(
                            text = "${if (isUp) "+" else ""}${data.deltaPercent.toInt()}% vs base",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeColor
                        )
                    } else if (data.baselineSpend > 0) {
                        Text(
                            text = "0% vs base",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    } else {
                        Text(
                            text = "New spend",
                            fontSize = 10.sp,
                            color = AmberGold
                        )
                    }
                }
            }

            // Visual Progress Meter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(data.accentColor)
                )
            }

            // Baseline Comparison Detail Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Baseline: ${currency.symbol}${data.baselineSpend.toInt()}",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Hide details" else "View details",
                        fontSize = 11.sp,
                        color = data.accentColor,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = data.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Drill-down Accordion of filtered transactions
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (data.transactions.isEmpty()) {
                        Text(
                            text = "No transactions logged in this category for this month.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        data.transactions.forEach { tx ->
                            val dateStr = remember(tx.timestamp) {
                                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                sdf.format(Date(tx.timestamp))
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ObsidianSurface)
                                    .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { onTransactionClick(tx) }
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${tx.instrumentName} • $dateStr",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                        if (tx.category.contains("PARTNER", ignoreCase = true) || tx.note.contains("Partner") || tx.note.contains("Family") || tx.note.contains("Girlfriend")) {
                                            val isFamily = tx.note.contains("Family", ignoreCase = true)
                                            val badgeColor = if (isFamily) ElectricMint else PartnerPink
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(badgeColor.copy(alpha = 0.18f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                val tagText = when {
                                                    tx.note.isNotBlank() && !tx.note.startsWith("Auto-parsed") -> {
                                                        when {
                                                            tx.note.startsWith("Partner: ") -> "❤️ " + tx.note
                                                            tx.note.startsWith("Family: ") -> "👨‍👩‍👧 " + tx.note
                                                            else -> tx.note
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
                                        } else if (tx.category.contains("GIFT", ignoreCase = true) || tx.note.startsWith("Gift")) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFF472B6).copy(alpha = 0.18f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                val tagText = when {
                                                    tx.note.isNotBlank() && !tx.note.startsWith("Auto-parsed") -> {
                                                        if (tx.note.startsWith("🎁")) tx.note else "🎁 ${tx.note}"
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
                                        } else if (tx.note.isNotBlank() && !tx.note.startsWith("Auto-parsed")) {
                                            Text(
                                                text = tx.note,
                                                fontSize = 9.sp,
                                                color = AmberGold
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${currency.symbol}${tx.trueNetAmount.toInt()}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.isAssetWealth) RoyalBlue else TextPrimary
                                        )
                                        if (tx.hasSplit) {
                                            Text(
                                                text = "Split w/ Flatmate",
                                                fontSize = 9.sp,
                                                color = AmberGold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
