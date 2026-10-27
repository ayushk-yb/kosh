package com.kosh.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "person_ledgers")
data class PersonLedgerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relationshipType: String, // FLATMATE, PARTNER, FAMILY, FRIEND, MERCHANT
    val avatarTag: String, // "RK", "❤️", "VK", "📦"
    val runningBalance: Double = 0.0, // positive: owes user; negative: user owes them / surplus
    val notes: String = "",
    val phoneNumber: String? = null
)
