package com.kosh.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val tag: String = "",                    // e.g. "Flatmate", "Office", "Family", "Landlord"
    val phoneNumber: String? = null,         // For WhatsApp nudge & UPI auto-reconciliation
    val isPinned: Boolean = false,           // Quick chips at the top of split pickers
    val avatarColor: Long = 0xFF10B981,      // Visual color tag
    val isCircleMember: Boolean = false      // Linked member in Circle tab
)
