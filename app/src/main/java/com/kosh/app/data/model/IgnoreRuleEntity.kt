package com.kosh.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ignore_rules")
data class IgnoreRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pattern: String,
    val isWholeWord: Boolean = false,
    val isCaseSensitive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
