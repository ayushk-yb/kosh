package com.kosh.app.ui.theme

import androidx.compose.ui.graphics.Color

// Obsidian Dark Palette (Deep, high-contrast OLED black & calm slate surfaces)
val ObsidianBg = Color(0xFF090D14)
val ObsidianSurface = Color(0xFF101622)
val CardBackground = Color(0xFF131B28)
val CardBackgroundVariant = Color(0xFF182232)
val CardBackgroundElevated = Color(0xFF1C273A)

// Translucent hairline borders for native depth (replaces harsh opaque wireframes)
val CardBorder = Color(0x1AFFFFFF) // 10% white subtle boundary
val CardBorderSubtle = Color(0x0FFFFFFF) // 6% white hairline
val CardBorderFocused = Color(0x3310B981) // 20% accent focus

val SubSurface = Color(0xFF070A0F)

// Primary Brand & Inflow Accent (Locked single brand accent: Mint Emerald)
val ElectricMint = Color(0xFF10B981)
val ElectricMintDark = Color(0xFF059669)
val ElectricMintContainer = Color(0x1810B981)

// Semantic Financial Indicators
val AmberGold = Color(0xFFF59E0B) // Warnings, Due Dates, Pending Receivables
val AmberGoldContainer = Color(0x1DF59E0B)

val SoftCrimson = Color(0xFFF43F5E) // Expenses, Outflow, Destructive actions
val SoftCrimsonContainer = Color(0x1DF43F5E)
val CoralRed = SoftCrimson

val RoyalBlue = Color(0xFF38BDF8) // Informational, Bank/UPI sync
val RoyalBlueContainer = Color(0x1D38BDF8)

// Category Identity Accents (Used strictly for category avatars/tags, never chrome borders)
val PartnerPink = Color(0xFFEC4899)
val PartnerPinkContainer = Color(0x1DEC4899)

val PurpleAccent = Color(0xFF8B5CF6)
val PurpleAccentContainer = Color(0x1D8B5CF6)

// Text Colors (Refined hierarchy with optical contrast)
val TextPrimary = Color(0xFFF8FAFC)     // 100% white-slate for headlines & primary values
val TextSecondary = Color(0xFF94A3B8)   // 60% slate for descriptions & labels
val TextMuted = Color(0xFF64748B)       // 40% slate for secondary metadata & captions
