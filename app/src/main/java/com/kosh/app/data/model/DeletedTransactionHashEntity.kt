package com.kosh.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_transaction_hashes")
data class DeletedTransactionHashEntity(
    @PrimaryKey
    val hash: String,
    val deletedAt: Long = System.currentTimeMillis()
)
