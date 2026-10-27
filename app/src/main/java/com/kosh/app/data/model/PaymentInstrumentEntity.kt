package com.kosh.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_card_accounts")
data class PaymentInstrumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val cardOrAccountName: String,
    val last4Digits: String,
    val instrumentType: String = InstrumentType.CREDIT_CARD.name,
    val creditLimit: Double = 0.0,
    val unbilledSpend: Double = 0.0,
    val cycleStartDay: Int = 15,
    val cycleEndDay: Int = 14,
    val paymentDueDay: Int = 5,
    val isPrimary: Boolean = false,
    val upiHandle: String? = null,
    val badgeText: String? = null
)
