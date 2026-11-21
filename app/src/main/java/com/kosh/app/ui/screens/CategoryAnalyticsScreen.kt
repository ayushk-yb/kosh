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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.data.model.Currency
import com.kosh.app.data.model.TransactionCategory
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.data.model.isCreditCardBillPayment
import com.kosh.app.data.model.isCreditOrInflow
import com.kosh.app.ui.components.CategoryIconTile
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBackground
import com.kosh.app.ui.theme.CardBackgroundElevated
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.CardBorderSubtle
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.KoshShapes
import com.kosh.app.ui.theme.ObsidianBg
import com.kosh.app.ui.theme.ObsidianSurface
import com.kosh.app.ui.theme.PartnerPink
import com.kosh.app.ui.theme.PurpleAccent
import com.kosh.app.ui.theme.RoyalBlue
import com.kosh.app.ui.theme.SubSurface
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary
import com.kosh.app.ui.theme.Typography
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.util.Calendar
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
    allTransactions: List<TransactionEntity> = emptyList(),
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
    val context = LocalContext.current

    // Mode: Month View vs Custom Date Range
    var isCustomRangeMode by remember { mutableStateOf(false) }
    var enableComparison by remember { mutableStateOf(false) }

    // Primary Date Range State
    val nowMillis = remember { System.currentTimeMillis() }
    var primaryPreset by remember { mutableStateOf("Last 30 Days") }
    var primaryStartMillis by remember { mutableStateOf(nowMillis - 30L * 86400000L) }
    var primaryEndMillis by remember { mutableStateOf(nowMillis) }

    // Comparison Date Range State (for custom range mode)
    var compPreset by remember { mutableStateOf("Previous Period") }
    var compStartMillis by remember { mutableStateOf(nowMillis - 60L * 86400000L) }
    var compEndMillis by remember { mutableStateOf(nowMillis - 30L * 86400000L) }

    // Active transactions according to mode
    val activeCurrentTransactions = remember(
        isCustomRangeMode, currentTransactions, allTransactions, primaryStartMillis, primaryEndMillis
    ) {
        if (isCustomRangeMode) {
            allTransactions.filter { it.timestamp in primaryStartMillis..primaryEndMillis }
        } else {
            currentTransactions
        }
    }

    val activeComparisonTransactions = remember(
        enableComparison, isCustomRangeMode, comparisonTransactions, allTransactions, compStartMillis, compEndMillis
    ) {
        if (!enableComparison) {
            emptyList()
        } else if (isCustomRangeMode) {
            allTransactions.filter { it.timestamp in compStartMillis..compEndMillis }
        } else {
            comparisonTransactions
        }
    }

    val rangeDateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val shortDateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    val activeRangeTitle = if (isCustomRangeMode) {
        "${shortDateFormatter.format(Date(primaryStartMillis))} - ${shortDateFormatter.format(Date(primaryEndMillis))}"
    } else {
        selectedMonthTitle
    }

    val activeCompRangeTitle = if (isCustomRangeMode) {
        "${shortDateFormatter.format(Date(compStartMillis))} - ${shortDateFormatter.format(Date(compEndMillis))}"
    } else {
        comparisonMonthTitle
    }

    // 1. Calculate Outflows
    val currentNetSpend = remember(activeCurrentTransactions) {
        activeCurrentTransactions.filter { !it.isAssetWealth && !it.isCreditOrInflow() && !it.isCreditCardBillPayment() }
            .sumOf { it.trueNetAmount }
    }
    val baselineNetSpend = remember(activeComparisonTransactions, enableComparison) {
        if (enableComparison) {
            activeComparisonTransactions.filter { !it.isAssetWealth && !it.isCreditOrInflow() && !it.isCreditCardBillPayment() }
                .sumOf { it.trueNetAmount }
        } else 0.0
    }

    val overallSpendDelta = currentNetSpend - baselineNetSpend
    val overallPercentDelta: Double? = if (enableComparison && baselineNetSpend > 0) {
        ((currentNetSpend - baselineNetSpend) / baselineNetSpend) * 100
    } else null

    // 2. Aggregate Category Breakdown (Splitting Partner & Family into distinct items)
    val categoryBreakdowns = remember(activeCurrentTransactions, activeComparisonTransactions, currentNetSpend, customCategories, enableComparison) {
        val standardCats = TransactionCategory.values().filter {
            it != TransactionCategory.PARTNER_FAMILY && it != TransactionCategory.OTHER
        }

        val isFamilyTx: (TransactionEntity) -> Boolean = { tx ->
            val cat = tx.category.trim()
            val note = tx.note
            val title = tx.title
            cat.equals("FAMILY", ignoreCase = true) ||
            note.contains("Family:", ignoreCase = true) ||
            note.contains("Family Expense", ignoreCase = true) ||
            note.contains("Family (", ignoreCase = true) ||
            note.contains("Mom", ignoreCase = true) ||
            note.contains("Dad", ignoreCase = true) ||
            note.contains("Papa", ignoreCase = true) ||
            note.contains("Maa", ignoreCase = true) ||
            note.contains("Mother", ignoreCase = true) ||
            note.contains("Father", ignoreCase = true) ||
            note.contains("Brother", ignoreCase = true) ||
            note.contains("Sister", ignoreCase = true) ||
            note.contains("Parents", ignoreCase = true) ||
            title.contains("Family", ignoreCase = true)
        }

        val isPartnerTx: (TransactionEntity) -> Boolean = { tx ->
            val cat = tx.category.trim()
            val note = tx.note
            val title = tx.title
            !isFamilyTx(tx) && (
                cat.equals("PARTNER", ignoreCase = true) ||
                cat.equals("PARTNER_FAMILY", ignoreCase = true) ||
                cat.contains("Partner", ignoreCase = true) ||
                note.contains("Partner", ignoreCase = true) ||
                note.contains("Girlfriend", ignoreCase = true) ||
                note.contains("Wife", ignoreCase = true) ||
                note.contains("Husband", ignoreCase = true) ||
                note.contains("Boyfriend", ignoreCase = true) ||
                note.contains("Ananya", ignoreCase = true) ||
                title.contains("Partner", ignoreCase = true)
            )
        }

        // Standard non-partner categories
        val standardItems = standardCats.map { cat ->
            val curTxList = activeCurrentTransactions.filter { tx ->
                tx.category.equals(cat.name, ignoreCase = true)
            }
            val baseTxList = if (enableComparison) {
                activeComparisonTransactions.filter { tx ->
                    tx.category.equals(cat.name, ignoreCase = true)
                }
            } else emptyList()

            val curAmount = if (cat == TransactionCategory.SIP_WEALTH) {
                curTxList.sumOf { it.grossAmount }
            } else {
                curTxList.filter { !it.isCreditOrInflow() && !it.isCreditCardBillPayment() }.sumOf { it.trueNetAmount }
            }

            val baseAmount = if (enableComparison) {
                if (cat == TransactionCategory.SIP_WEALTH) {
                    baseTxList.sumOf { it.grossAmount }
                } else {
                    baseTxList.filter { !it.isCreditOrInflow() && !it.isCreditCardBillPayment() }.sumOf { it.trueNetAmount }
                }
            } else 0.0

            val percentage = if (currentNetSpend > 0 && cat != TransactionCategory.SIP_WEALTH) {
                (curAmount / currentNetSpend).toFloat().coerceIn(0f, 1f)
            } else 0f

            val delta: Double? = if (enableComparison && baseAmount > 0) {
                ((curAmount - baseAmount) / baseAmount) * 100
            } else if (enableComparison && curAmount > 0) 100.0 else null

            val accent = when (cat) {
                TransactionCategory.RENT -> Color(0xFF6366F1)
                TransactionCategory.CAB_COMMUTE -> AmberGold
                TransactionCategory.COOK_MAID -> Color(0xFFFB923C)
                TransactionCategory.GIFTS -> Color(0xFFF472B6)
                TransactionCategory.SIP_WEALTH -> RoyalBlue
                TransactionCategory.GROCERIES -> ElectricMint
                TransactionCategory.UTILITIES -> Color(0xFFFACC15)
                TransactionCategory.SHOPPING -> PurpleAccent
                else -> Color(0xFF94A3B8)
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

        // Distinct Partner Category Item
        val partnerCurList = activeCurrentTransactions.filter { isPartnerTx(it) }
        val partnerBaseList = if (enableComparison) activeComparisonTransactions.filter { isPartnerTx(it) } else emptyList()
        val partnerCurAmount = partnerCurList.filter { !it.isCreditOrInflow() && !it.isCreditCardBillPayment() }.sumOf { it.trueNetAmount }
        val partnerBaseAmount = if (enableComparison) partnerBaseList.filter { !it.isCreditOrInflow() && !it.isCreditCardBillPayment() }.sumOf { it.trueNetAmount } else 0.0
        val partnerPercentage = if (currentNetSpend > 0) (partnerCurAmount / currentNetSpend).toFloat().coerceIn(0f, 1f) else 0f
        val partnerDelta: Double? = if (enableComparison && partnerBaseAmount > 0) {
            ((partnerCurAmount - partnerBaseAmount) / partnerBaseAmount) * 100
        } else if (enableComparison && partnerCurAmount > 0) 100.0 else null
        val partnerItem = CategorySpendData(
            categoryKey = "PARTNER",
            category = TransactionCategory.PARTNER_FAMILY,
            displayName = "Partner",
            icon = "❤️",
            currentSpend = partnerCurAmount,
            baselineSpend = partnerBaseAmount,
            percentageOfTotal = partnerPercentage,
            deltaPercent = partnerDelta,
            accentColor = PartnerPink,
            transactions = partnerCurList.sortedByDescending { it.timestamp },
            isWealthAsset = false
        )

        // Distinct Family Category Item
        val familyCurList = activeCurrentTransactions.filter { isFamilyTx(it) }
        val familyBaseList = if (enableComparison) activeComparisonTransactions.filter { isFamilyTx(it) } else emptyList()
        val familyCurAmount = familyCurList.filter { !it.isCreditOrInflow() && !it.isCreditCardBillPayment() }.sumOf { it.trueNetAmount }
        val familyBaseAmount = if (enableComparison) familyBaseList.filter { !it.isCreditOrInflow() && !it.isCreditCardBillPayment() }.sumOf { it.trueNetAmount } else 0.0
        val familyPercentage = if (currentNetSpend > 0) (familyCurAmount / currentNetSpend).toFloat().coerceIn(0f, 1f) else 0f
        val familyDelta: Double? = if (enableComparison && familyBaseAmount > 0) {
            ((familyCurAmount - familyBaseAmount) / familyBaseAmount) * 100
        } else if (enableComparison && familyCurAmount > 0) 100.0 else null
        val familyItem = CategorySpendData(
            categoryKey = "FAMILY",
            category = null,
            displayName = "Family",
            icon = "👨‍👩‍👧",
            currentSpend = familyCurAmount,
            baselineSpend = familyBaseAmount,
            percentageOfTotal = familyPercentage,
            deltaPercent = familyDelta,
            accentColor = Color(0xFFF59E0B),
            transactions = familyCurList.sortedByDescending { it.timestamp },
            isWealthAsset = false
        )

        // Other item
        val otherCurList = activeCurrentTransactions.filter { tx ->
            !isPartnerTx(tx) && !isFamilyTx(tx) &&
            (tx.category.equals("OTHER", ignoreCase = true) || tx.category.isBlank() ||
             (standardCats.none { tx.category.equals(it.name, ignoreCase = true) } &&
              customCategories.none { tx.category.equals(it.name, ignoreCase = true) }))
        }
        val otherBaseList = if (enableComparison) {
            activeComparisonTransactions.filter { tx ->
                !isPartnerTx(tx) && !isFamilyTx(tx) &&
                (tx.category.equals("OTHER", ignoreCase = true) || tx.category.isBlank() ||
                 (standardCats.none { tx.category.equals(it.name, ignoreCase = true) } &&
                  customCategories.none { tx.category.equals(it.name, ignoreCase = true) }))
            }
        } else emptyList()
        val otherCurAmount = otherCurList.filter { !it.isCreditOrInflow() && !it.isCreditCardBillPayment() }.sumOf { it.trueNetAmount }
        val otherBaseAmount = if (enableComparison) otherBaseList.filter { !it.isCreditOrInflow() && !it.isCreditCardBillPayment() }.sumOf { it.trueNetAmount } else 0.0
        val otherPercentage = if (currentNetSpend > 0) (otherCurAmount / currentNetSpend).toFloat().coerceIn(0f, 1f) else 0f
        val otherDelta: Double? = if (enableComparison && otherBaseAmount > 0) {
            ((otherCurAmount - otherBaseAmount) / otherBaseAmount) * 100
        } else if (enableComparison && otherCurAmount > 0) 100.0 else null
        val otherItem = CategorySpendData(
            categoryKey = "OTHER",
            category = TransactionCategory.OTHER,
            displayName = "Other",
            icon = "🏷️",
            currentSpend = otherCurAmount,
            baselineSpend = otherBaseAmount,
            percentageOfTotal = otherPercentage,
            deltaPercent = otherDelta,
            accentColor = Color(0xFF94A3B8),
            transactions = otherCurList.sortedByDescending { it.timestamp },
            isWealthAsset = false
        )

        val customItems = customCategories.map { customCat ->
            val curTxList = activeCurrentTransactions.filter { tx ->
                tx.category.equals(customCat.name, ignoreCase = true)
            }
            val baseTxList = if (enableComparison) {
                activeComparisonTransactions.filter { tx ->
                    tx.category.equals(customCat.name, ignoreCase = true)
                }
            } else emptyList()

            val curAmount = if (customCat.isWealthAsset) {
                curTxList.sumOf { it.grossAmount }
            } else {
                curTxList.filter { !it.isCreditOrInflow() && !it.isCreditCardBillPayment() }.sumOf { it.trueNetAmount }
            }

            val baseAmount = if (enableComparison) {
                if (customCat.isWealthAsset) {
                    baseTxList.sumOf { it.grossAmount }
                } else {
                    baseTxList.filter { !it.isCreditOrInflow() && !it.isCreditCardBillPayment() }.sumOf { it.trueNetAmount }
                }
            } else 0.0

            val percentage = if (currentNetSpend > 0 && !customCat.isWealthAsset) {
                (curAmount / currentNetSpend).toFloat().coerceIn(0f, 1f)
            } else 0f

            val delta: Double? = if (enableComparison && baseAmount > 0) {
                ((curAmount - baseAmount) / baseAmount) * 100
            } else if (enableComparison && curAmount > 0) 100.0 else null

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

        val allItems = standardItems + listOf(partnerItem, familyItem, otherItem) + customItems
        allItems.sortedWith(compareByDescending<CategorySpendData> { it.currentSpend > 0 }.thenByDescending { it.currentSpend })
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

            // Header: Category Breakdown + Navigator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isCustomRangeMode) "CUSTOM RANGE BREAKDOWN" else "MONTHLY SPEND BREAKDOWN",
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

                if (!isCustomRangeMode) {
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

            Spacer(modifier = Modifier.height(10.dp))

            // View Mode Selector: Month View vs Date Range
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KoshShapes.SubCard)
                    .background(CardBackgroundElevated)
                    .border(0.5.dp, CardBorderSubtle, KoshShapes.SubCard)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(KoshShapes.Chip)
                        .background(if (!isCustomRangeMode) CardBackground else Color.Transparent)
                        .clickable { isCustomRangeMode = false }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (!isCustomRangeMode) ElectricMint else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Month View",
                            fontSize = 12.sp,
                            fontWeight = if (!isCustomRangeMode) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (!isCustomRangeMode) TextPrimary else TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(KoshShapes.Chip)
                        .background(if (isCustomRangeMode) CardBackground else Color.Transparent)
                        .clickable { isCustomRangeMode = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (isCustomRangeMode) ElectricMint else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Custom Range",
                            fontSize = 12.sp,
                            fontWeight = if (isCustomRangeMode) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isCustomRangeMode) TextPrimary else TextSecondary
                        )
                    }
                }
            }
        }

        // Custom Range Presets Bar (when in custom range mode)
        if (isCustomRangeMode) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SELECT DATE RANGE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    // Preset chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Last 7 Days" to 7L, "Last 14 Days" to 14L, "Last 30 Days" to 30L, "Last 90 Days" to 90L).forEach { (label, days) ->
                            val isSelected = primaryPreset == label
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ElectricMint else SubSurface)
                                    .border(1.dp, if (isSelected) ElectricMint else CardBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        primaryPreset = label
                                        val now = System.currentTimeMillis()
                                        primaryStartMillis = now - days * 86400000L
                                        primaryEndMillis = now
                                        // Auto-adjust comparison period to previous matching duration
                                        compStartMillis = primaryStartMillis - days * 86400000L
                                        compEndMillis = primaryStartMillis
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF0F141C) else TextSecondary
                                )
                            }
                        }

                        // Custom Range Chip
                        val isCustomSelected = primaryPreset == "Custom Range"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCustomSelected) ElectricMint else SubSurface)
                                .border(1.dp, if (isCustomSelected) ElectricMint else CardBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    primaryPreset = "Custom Range"
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Custom Range 📅",
                                fontSize = 11.sp,
                                fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCustomSelected) Color(0xFF0F141C) else TextSecondary
                            )
                        }
                    }

                    // Interactive Custom Date Pickers (From Date & To Date)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Start Date Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardBackground)
                                .border(
                                    1.dp,
                                    if (primaryPreset == "Custom Range") ElectricMint.copy(alpha = 0.8f) else CardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = primaryStartMillis }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val picked = Calendar.getInstance().apply {
                                                set(year, month, day, 0, 0, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }.timeInMillis
                                            primaryStartMillis = picked
                                            if (primaryEndMillis < primaryStartMillis) {
                                                primaryEndMillis = picked + 86400000L - 1
                                            }
                                            val duration = (primaryEndMillis - primaryStartMillis).coerceAtLeast(86400000L)
                                            compStartMillis = primaryStartMillis - duration
                                            compEndMillis = primaryStartMillis - 1
                                            primaryPreset = "Custom Range"
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "FROM DATE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Pick Start Date",
                                        tint = ElectricMint,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = rangeDateFormatter.format(Date(primaryStartMillis)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        // End Date Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardBackground)
                                .border(
                                    1.dp,
                                    if (primaryPreset == "Custom Range") ElectricMint.copy(alpha = 0.8f) else CardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = primaryEndMillis }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val picked = Calendar.getInstance().apply {
                                                set(year, month, day, 23, 59, 59)
                                                set(Calendar.MILLISECOND, 999)
                                            }.timeInMillis
                                            primaryEndMillis = picked
                                            if (primaryStartMillis > primaryEndMillis) {
                                                primaryStartMillis = picked - 86400000L + 1
                                            }
                                            val duration = (primaryEndMillis - primaryStartMillis).coerceAtLeast(86400000L)
                                            compStartMillis = primaryStartMillis - duration
                                            compEndMillis = primaryStartMillis - 1
                                            primaryPreset = "Custom Range"
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "TO DATE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Pick End Date",
                                        tint = ElectricMint,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = rangeDateFormatter.format(Date(primaryEndMillis)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Display active dates
                    Text(
                        text = "Active Range: ${rangeDateFormatter.format(Date(primaryStartMillis))} to ${rangeDateFormatter.format(Date(primaryEndMillis))}",
                        fontSize = 11.sp,
                        color = ElectricMint,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Comparison Toggle Switch Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F1521))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Compare With Another Range",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (enableComparison) "Comparing against $activeCompRangeTitle" else "Disabled: Showing pure expenses in this range",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    androidx.compose.material3.Switch(
                        checked = enableComparison,
                        onCheckedChange = { enableComparison = it },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = ElectricMint,
                            checkedTrackColor = ElectricMint.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }
        }

        // Comparison Options (only shown if enableComparison is ON)
        if (enableComparison) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "COMPARE AGAINST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    if (!isCustomRangeMode) {
                        val isPrevSelected = comparisonYearMonth == selectedYearMonth.minusMonths(1)
                        val isTwoMonthsSelected = comparisonYearMonth == selectedYearMonth.minusMonths(2)
                        val isCustomSelected = !isPrevSelected && !isTwoMonthsSelected

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                    } else {
                        // Custom range comparison options
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val duration = primaryEndMillis - primaryStartMillis
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (compPreset == "Previous Period") ElectricMint else SubSurface)
                                    .border(1.dp, if (compPreset == "Previous Period") ElectricMint else CardBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        compPreset = "Previous Period"
                                        compEndMillis = primaryStartMillis
                                        compStartMillis = primaryStartMillis - duration
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "vs Previous Period",
                                    fontSize = 11.sp,
                                    fontWeight = if (compPreset == "Previous Period") FontWeight.Bold else FontWeight.Medium,
                                    color = if (compPreset == "Previous Period") Color(0xFF0F141C) else TextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (compPreset == "Last Year") ElectricMint else SubSurface)
                                    .border(1.dp, if (compPreset == "Last Year") ElectricMint else CardBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        compPreset = "Last Year"
                                        val cal = Calendar.getInstance()
                                        cal.timeInMillis = primaryStartMillis
                                        cal.add(Calendar.YEAR, -1)
                                        compStartMillis = cal.timeInMillis
                                        cal.timeInMillis = primaryEndMillis
                                        cal.add(Calendar.YEAR, -1)
                                        compEndMillis = cal.timeInMillis
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "vs Same Period Last Year",
                                    fontSize = 11.sp,
                                    fontWeight = if (compPreset == "Last Year") FontWeight.Bold else FontWeight.Medium,
                                    color = if (compPreset == "Last Year") Color(0xFF0F141C) else TextSecondary
                                )
                            }
                        }

                        Text(
                            text = "Comparison Range: ${rangeDateFormatter.format(Date(compStartMillis))} to ${rangeDateFormatter.format(Date(compEndMillis))}",
                            fontSize = 11.sp,
                            color = AmberGold,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Hero Spend Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KoshShapes.CardHero)
                    .background(CardBackground)
                    .border(0.5.dp, CardBorder, KoshShapes.CardHero)
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "${activeRangeTitle.uppercase()} TOTAL SPEND",
                                style = Typography.labelSmall,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currency.symbol}${currentNetSpend.toInt()}",
                                style = Typography.displayMedium,
                                color = TextPrimary
                            )
                            if (enableComparison) {
                                Text(
                                    text = "Baseline ($activeCompRangeTitle): ${currency.symbol}${baselineNetSpend.toInt()}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Delta Pill (only when comparison is active)
                        if (enableComparison) {
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
                    }

                    // Explanatory Subtitle
                    if (enableComparison) {
                        val diffAmt = abs(overallSpendDelta).toInt()
                        val summaryText = when {
                            overallPercentDelta == null -> "No recorded spend in baseline period to compare against."
                            overallSpendDelta > 0 -> "You have spent ${currency.symbol}$diffAmt more than in $activeCompRangeTitle."
                            overallSpendDelta < 0 -> "Fantastic pacing! You saved ${currency.symbol}$diffAmt compared to $activeCompRangeTitle."
                            else -> "Spend is exactly identical to $activeCompRangeTitle."
                        }

                        Text(
                            text = summaryText,
                            fontSize = 12.sp,
                            color = if (overallSpendDelta > 0) Color(0xFFFCA5A5) else ElectricMint,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Showing pure category burn for this range. Toggle comparison above if you wish to benchmark.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
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
                showComparison = enableComparison,
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
    showComparison: Boolean = true,
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
            .clip(KoshShapes.Card)
            .background(CardBackground)
            .border(0.5.dp, if (isExpanded) data.accentColor.copy(alpha = 0.4f) else CardBorderSubtle, KoshShapes.Card)
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryIconTile(
                        categoryKeyOrName = data.categoryKey,
                        fallbackEmoji = data.icon,
                        size = 40.dp,
                        iconSize = 20.dp
                    )

                    Column {
                        Text(
                            text = data.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${(data.percentageOfTotal * 100).toInt()}% of burn • ${data.transactions.size} txns",
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

                    // Month-over-Month Delta badge (only shown if comparison enabled)
                    if (showComparison) {
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
                if (showComparison) {
                    Text(
                        text = "Baseline: ${currency.symbol}${data.baselineSpend.toInt()}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                } else {
                    Text(
                        text = "Pure Spend View",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

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
