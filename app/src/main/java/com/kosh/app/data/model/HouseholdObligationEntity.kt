package com.kosh.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "household_obligations")
data class HouseholdObligationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String, // Rent, Cook, Maid, Wifi, etc.
    val recipientName: String, // e.g. "Landlord", "Ramesh", "Sunita"
    val totalAmount: Double,
    val splitWays: Int = 1, // e.g. 3 ways for flat rent, 2 ways for maid
    val myShareAmount: Double,
    val dueDayOfMonth: Int = 1, // 1st of month
    val isAutoPayEnabled: Boolean = false,
    val isPaidThisMonth: Boolean = false,
    val lastPaidDate: Long? = null,
    val iconEmoji: String = "🏠",
    val splitMode: String = "SOLO", // SOLO, EQUAL, CUSTOM
    val participantIdsJson: String = "",
    val paidTransactionId: Long? = null
)
