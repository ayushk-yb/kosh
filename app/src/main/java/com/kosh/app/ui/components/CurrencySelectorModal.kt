package com.kosh.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosh.app.data.model.Currency
import com.kosh.app.ui.theme.CardBackground
import com.kosh.app.ui.theme.CardBackgroundElevated
import com.kosh.app.ui.theme.CardBorder
import com.kosh.app.ui.theme.CardBorderSubtle
import com.kosh.app.ui.theme.ElectricMint
import com.kosh.app.ui.theme.ElectricMintContainer
import com.kosh.app.ui.theme.KoshShapes
import com.kosh.app.ui.theme.ObsidianSurface
import com.kosh.app.ui.theme.TextPrimary
import com.kosh.app.ui.theme.TextSecondary

@Composable
fun CurrencySelectorModal(
    currentCurrency: Currency,
    onSelectCurrency: (Currency) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        shape = KoshShapes.CardHero,
        title = {
            Text(
                text = "Select Base Currency",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "All dashboard metrics, split ledgers, and credit limits will format with this currency.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                ) {
                    items(Currency.values()) { curr ->
                        val isSelected = curr == currentCurrency
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(KoshShapes.SubCard)
                                .background(if (isSelected) ElectricMintContainer else CardBackground)
                                .border(
                                    width = 0.5.dp,
                                    color = if (isSelected) ElectricMint.copy(alpha = 0.5f) else CardBorderSubtle,
                                    shape = KoshShapes.SubCard
                                )
                                .clickable {
                                    onSelectCurrency(curr)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(if (isSelected) ElectricMintContainer else CardBackgroundElevated, shape = KoshShapes.Pill),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = curr.symbol.trim(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) ElectricMint else TextPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = curr.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = curr.code,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (isSelected) {
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = ElectricMint, fontWeight = FontWeight.Bold)
            }
        }
    )
}
