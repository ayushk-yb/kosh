package com.kosh.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "split_debts")
data class SplitDebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentTransactionId: Long? = null,
    val personId: Long? = null,
    val personName: String,
    val amount: Double,
    val itemDescription: String,
    val isSettled: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateSettled: Long? = null,
    val isMerchantRefund: Boolean = false,
    val merchantName: String? = null
)
