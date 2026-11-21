package com.kosh.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Kosh Strict Shape Lock System (Appllama Anti-Slop Law 4).
 * Enforces a disciplined 3-tier corner radius scale across the app:
 * - Small/Inputs/Chips: 10dp - 12dp
 * - Cards & Surfaces: 20dp (or 24dp for hero)
 * - Sheets & Modals: 28dp
 * - Action Pills: 999dp (Circle)
 */
object KoshShapes {
    val Pill = CircleShape
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val CardHero = RoundedCornerShape(24.dp)
    val Card = RoundedCornerShape(20.dp)
    val SubCard = RoundedCornerShape(12.dp)
    val Input = RoundedCornerShape(12.dp)
    val Chip = RoundedCornerShape(10.dp)
    val IconTile = RoundedCornerShape(12.dp)
}

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
