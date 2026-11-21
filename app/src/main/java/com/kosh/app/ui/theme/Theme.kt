package com.kosh.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricMint,
    onPrimary = Color(0xFF042013),
    primaryContainer = ElectricMintContainer,
    onPrimaryContainer = ElectricMint,
    secondary = AmberGold,
    onSecondary = Color(0xFF241400),
    secondaryContainer = AmberGoldContainer,
    onSecondaryContainer = AmberGold,
    tertiary = RoyalBlue,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = CardBorderSubtle
)

@Composable
fun KoshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
