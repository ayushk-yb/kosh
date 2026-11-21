package com.kosh.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.data.model.TransactionCategory
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.AmberGoldContainer
import com.kosh.app.ui.theme.CardBackgroundElevated
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ElectricMintContainer
import com.kosh.app.ui.theme.KoshShapes
import com.kosh.app.ui.theme.PartnerPink
import com.kosh.app.ui.theme.PartnerPinkContainer
import com.kosh.app.ui.theme.PurpleAccent
import com.kosh.app.ui.theme.PurpleAccentContainer
import com.kosh.app.ui.theme.RoyalBlue
import com.kosh.app.ui.theme.RoyalBlueContainer
import com.kosh.app.ui.theme.TextMuted

data class CategoryVisual(
    val icon: ImageVector,
    val tint: Color,
    val container: Color
)

fun getCategoryVisual(categoryKeyOrName: String): CategoryVisual {
    val upper = categoryKeyOrName.uppercase()
    return when {
        upper.contains("RENT") || upper.contains("HOUSE") || upper.contains("HOME") ->
            CategoryVisual(Icons.Default.Home, RoyalBlue, RoyalBlueContainer)
        upper.contains("COOK") || upper.contains("MAID") || upper.contains("FOOD") || upper.contains("DINING") ->
            CategoryVisual(Icons.Default.Restaurant, AmberGold, AmberGoldContainer)
        upper.contains("CAB") || upper.contains("AUTO") || upper.contains("COMMUTE") || upper.contains("TRAVEL") ->
            CategoryVisual(Icons.Default.DirectionsCar, AmberGold, AmberGoldContainer)
        upper.contains("GIFT") || upper.contains("CELEBRATION") ->
            CategoryVisual(Icons.Default.CardGiftcard, PartnerPink, PartnerPinkContainer)
        upper.contains("FAMILY") ->
            CategoryVisual(Icons.Default.Group, AmberGold, AmberGoldContainer)
        upper.contains("PARTNER") || upper.contains("RELATIONSHIP") ->
            CategoryVisual(Icons.Default.Favorite, PartnerPink, PartnerPinkContainer)
        upper.contains("SIP") || upper.contains("WEALTH") || upper.contains("INVEST") || upper.contains("MUTUAL") ->
            CategoryVisual(Icons.Default.TrendingUp, ElectricMint, ElectricMintContainer)
        upper.contains("SPLIT") || upper.contains("PEOPLE") || upper.contains("GROUP") ->
            CategoryVisual(Icons.Default.Group, AmberGold, AmberGoldContainer)
        upper.contains("GROCERY") || upper.contains("GROCERIES") || upper.contains("SUPERMARKET") ->
            CategoryVisual(Icons.Default.ShoppingCart, ElectricMint, ElectricMintContainer)
        upper.contains("UTILITY") || upper.contains("UTILITIES") || upper.contains("ELECTRIC") || upper.contains("WIFI") || upper.contains("BILL") ->
            CategoryVisual(Icons.Default.Bolt, AmberGold, AmberGoldContainer)
        upper.contains("SHOP") || upper.contains("LIFESTYLE") || upper.contains("CLOTHES") ->
            CategoryVisual(Icons.Default.ShoppingBag, PurpleAccent, PurpleAccentContainer)
        upper.contains("SALARY") || upper.contains("INCOME") || upper.contains("CREDIT") ->
            CategoryVisual(Icons.Default.AccountBalance, ElectricMint, ElectricMintContainer)
        else ->
            CategoryVisual(Icons.Default.ReceiptLong, TextMuted, CardBackgroundElevated)
    }
}

@Composable
fun CategoryIconTile(
    categoryKeyOrName: String,
    fallbackEmoji: String = "",
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val visual = getCategoryVisual(categoryKeyOrName)

    Box(
        modifier = modifier
            .size(size)
            .background(visual.container, shape = KoshShapes.IconTile),
        contentAlignment = Alignment.Center
    ) {
        // If fallbackEmoji is passed and doesn't match any keyword and isn't empty, check if we should show vector or emoji
        Icon(
            imageVector = visual.icon,
            contentDescription = categoryKeyOrName,
            tint = visual.tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
