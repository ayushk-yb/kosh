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
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.SubSurface
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextSecondary
import com.kosh.app.ui.viewmodel.AppTab

@Composable
fun BottomNavBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Outer unclipped container providing headroom for the floating FAB
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
    ) {
        // 1. Clipped background bar container (anchored to bottom)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .align(Alignment.BottomCenter)
                .background(SubSurface.copy(alpha = 0.95f), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(
                    width = 1.dp,
                    color = CardBorder.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            // Two equal-width wings (Left 50% & Right 50%) separated by a dedicated center gap
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Half: Home & Pending In
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
                        label = "Shared Splits",
                        isSelected = currentTab == AppTab.SPLITS,
                        hasBadge = true,
                        badgeColor = AmberGold,
                        onClick = { onTabSelected(AppTab.SPLITS) }
                    )
                }

                // Fixed Center Gap specifically for the FAB
                Spacer(modifier = Modifier.width(68.dp))

                // Right Half: Cards & Circle
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
                        label = "Breakdown",
                        isSelected = currentTab == AppTab.ANALYTICS,
                        onClick = { onTabSelected(AppTab.ANALYTICS) }
                    )
                }
            }
        }

        // 2. Floating Action Button (FAB): Unclipped, positioned at TopCenter of the outer container
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(54.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = ElectricMint,
                    spotColor = ElectricMint
                )
                .background(ElectricMint, shape = RoundedCornerShape(18.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onFabClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Expense",
                tint = Color(0xFF0F141C),
                modifier = Modifier.size(28.dp)
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(22.dp)
            )
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(badgeColor, shape = CircleShape)
                        .offset(x = 2.dp, y = (-2).dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) ElectricMint else TextSecondary,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}
