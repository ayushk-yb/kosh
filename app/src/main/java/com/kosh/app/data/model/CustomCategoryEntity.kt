package com.kosh.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconEmoji: String = "📁",
    val colorHex: Long = 0xFF10B981,
    val isWealthAsset: Boolean = false
)
