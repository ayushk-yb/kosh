package com.kosh.app.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.data.model.Currency
import com.kosh.app.data.model.PersonEntity
import com.kosh.app.data.model.PersonLedgerEntity
import com.kosh.app.ui.theme.AmberGold
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ObsidianBg
import com.kosh.app.ui.theme.PartnerPink
import com.kosh.app.ui.theme.PurpleAccent
import com.kosh.app.ui.theme.TextMuted
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary

@Composable
fun FamilyLedgerScreen(
    currency: Currency,
    circleMember: PersonEntity?,
    partnerLedger: PersonLedgerEntity?,
    onOpenLinkMember: () -> Unit,
    onUnlinkMember: () -> Unit,
    onRecordDeficit: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PARTNER & COMPANION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PartnerPink,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Shared Ledger with Companion",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (circleMember != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF22121E))
                            .border(1.dp, PartnerPink.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { onOpenLinkMember() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Swap Member",
                                tint = PartnerPink,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Switch / Unlink",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PartnerPink
                            )
                        }
                    }
                }
            }
        }

        if (circleMember == null) {
            // High-fidelity Empty State (No hardcoded Ananya)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF201322), Color(0xFF161522), Color(0xFF0F141E))
                            )
                        )
                        .border(1.dp, CardBorder, RoundedCornerShape(26.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PartnerPink.copy(alpha = 0.15f))
                                .border(1.dp, PartnerPink.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = PartnerPink,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "No Circle Member Linked",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "Track shared outlays, trips, dinners, or discretionary gifts with your partner, spouse, sibling, or close companion — completely decoupled from rigid split debts.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Button(
                            onClick = onOpenLinkMember,
                            colors = ButtonDefaults.buttonColors(containerColor = PartnerPink),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Link or Add Companion",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            // Linked Member Running Balance Card
            val runningBalance = partnerLedger?.runningBalance ?: 0.0
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF22121E), Color(0xFF161522), Color(0xFF0F141E))
                            )
                        )
                        .border(1.dp, CardBorder, RoundedCornerShape(26.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Profile Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PartnerPink.copy(alpha = 0.15f))
                                        .border(1.dp, PartnerPink.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = circleMember.name.take(2).uppercase(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PartnerPink
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = circleMember.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (circleMember.tag.isNotBlank()) "${circleMember.tag} • Close Circle" else "Close Circle • Decoupled from Splits",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Status Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1724))
                                    .border(1.dp, PartnerPink.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Shared Outlays",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PartnerPink
                                )
                            }
                        }

                        // Running Balance Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0D121B))
                                .border(1.dp, CardBorder.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CURRENT RUNNING BALANCE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted,
                                        letterSpacing = 0.8.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AmberGold.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Discretionary / Gift",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AmberGold
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = "${currency.symbol}${kotlin.math.abs(runningBalance).toInt()}",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary,
                                        letterSpacing = (-0.5).sp
                                    )

                                    Text(
                                        text = "Not a hard debt",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Log Outlay Action
                        Button(
                            onClick = onRecordDeficit,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22121E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "+ Log Outlay / Gift for ${circleMember.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PartnerPink
                            )
                        }
                    }
                }
            }
        }

        // Philosophy distinction cards
        item {
            Text(
                text = "HOW KOSH TREATS CLOSE CIRCLE EXPENSES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        }

        item {
            DistinctionCard(
                icon = "🎁",
                title = "1. Pure Gift / Support",
                tagColor = PartnerPink,
                description = "100% personal outlay, 0 expected back. Tagged for personal category analytics, never treated as a split debt."
            )
        }

        item {
            DistinctionCard(
                icon = "🍷",
                title = "2. Partial Gift / Partial Split",
                tagColor = PurpleAccent,
                description = "e.g. A shared hotel or dinner where your treat/gift is absorbed into your personal burn, and the rest is tracked without pressure."
            )
        }

        item {
            DistinctionCard(
                icon = "⚖️",
                title = "3. Shared Purchases & Discretionary Outlays",
                tagColor = ElectricMint,
                description = "Joint purchases and advance payments flow into a flexible running balance that absorbs surpluses and gifts without calendar month boundaries."
            )
        }

        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
fun DistinctionCard(
    icon: String,
    title: String,
    tagColor: Color,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF131A26))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(tagColor.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
