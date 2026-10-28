package com.kosh.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val grossAmount: Double,
    val trueNetAmount: Double,
    val reimbursableAmount: Double = 0.0,
    val category: String, // from TransactionCategory
    val instrumentName: String, // e.g. "HDFC CC", "Salary Account"
    val timestamp: Long = System.currentTimeMillis(),
    val isAssetWealth: Boolean = false, // SIP / Mutual funds
    val note: String = "",
    val hasSplit: Boolean = false,
    val smsRefHash: String? = null,
    val originalSms: String? = null
)

fun TransactionEntity.isCreditOrInflow(): Boolean {
    return category.equals("INFLOW", ignoreCase = true) ||
           category.equals("CREDIT", ignoreCase = true) ||
           title.contains("Credit", ignoreCase = true) ||
           title.contains("Settlement from", ignoreCase = true) ||
           title.contains("Refund", ignoreCase = true) ||
           instrumentName.equals("Bank Credit", ignoreCase = true) ||
           note.contains("Credit", ignoreCase = true) ||
           note.contains("received", ignoreCase = true) ||
           note.contains("settled", ignoreCase = true)
}

fun TransactionEntity.displayAmount(): Double {
    if (grossAmount > 0.0) return grossAmount
    val pattern = java.util.regex.Pattern.compile("(?:[₹]|rs\\.?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", java.util.regex.Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(note)
    if (matcher.find()) {
        val parsed = matcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        if (parsed != null && parsed > 0.0) return parsed
    }
    return 0.0
}
