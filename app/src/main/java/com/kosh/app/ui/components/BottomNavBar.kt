package com.kosh.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBorderSubtle
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ElectricMintContainer
import com.kosh.app.ui.theme.KoshShapes
import com.kosh.app.ui.theme.SubSurface
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.viewmodel.AppTab

@Composable
fun BottomNavBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        // Nav bar container anchored to bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter)
                .background(SubSurface.copy(alpha = 0.97f), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(
                    width = 0.5.dp,
                    color = CardBorderSubtle,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Wing: Home & Shared Splits
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(
                        icon = Icons.Default.Home,
                        label = "Home",
                        isSelected = currentTab == AppTab.DASHBOARD,
                        onClick = { onTabSelected(AppTab.DASHBOARD) }
                    )

                    NavItem(
                        icon = Icons.Default.People,
                        label = "Splits",
                        isSelected = currentTab == AppTab.SPLITS,
                        hasBadge = true,
                        badgeColor = AmberGold,
                        onClick = { onTabSelected(AppTab.SPLITS) }
                    )
                }

                // Dedicated center gap for floating FAB
                Spacer(modifier = Modifier.width(64.dp))

                // Right Wing: Cards & Breakdown
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(
                        icon = Icons.Default.CreditCard,
                        label = "Cards",
                        isSelected = currentTab == AppTab.CARDS,
                        onClick = { onTabSelected(AppTab.CARDS) }
                    )

                    NavItem(
                        icon = Icons.Default.PieChart,
                        label = "Insights",
                        isSelected = currentTab == AppTab.ANALYTICS,
                        onClick = { onTabSelected(AppTab.ANALYTICS) }
                    )
                }
            }
        }

        // Floating Action Button (FAB)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 10.dp)
                .size(52.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = KoshShapes.Pill,
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = ElectricMint.copy(alpha = 0.25f)
                )
                .background(ElectricMint, shape = KoshShapes.Pill)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onFabClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Expense",
                tint = Color(0xFF061A10),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    hasBadge: Boolean = false,
    badgeColor: Color = AmberGold,
    onClick: () -> Unit
) {
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) ElectricMint else TextMuted,
        label = "nav_item_color"
    )

    Column(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(KoshShapes.Pill)
                .background(if (isSelected) ElectricMintContainer else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(20.dp)
            )
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(badgeColor, shape = CircleShape)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) TextPrimary else TextMuted,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
