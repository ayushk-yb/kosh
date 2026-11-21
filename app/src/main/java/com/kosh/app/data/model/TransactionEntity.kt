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
    if (isCreditCardBillPayment()) return false
    val cleanTitle = title.replace("Credit Card", "", ignoreCase = true)
    val cleanNote = note.replace("Credit Card", "", ignoreCase = true)
    return category.equals("INFLOW", ignoreCase = true) ||
           category.equals("CREDIT", ignoreCase = true) ||
           category.equals("REFUND", ignoreCase = true) ||
           cleanTitle.contains("Credit", ignoreCase = true) ||
           cleanTitle.contains("Settlement from", ignoreCase = true) ||
           cleanTitle.contains("Refund", ignoreCase = true) ||
           cleanTitle.contains("Reversal", ignoreCase = true) ||
           cleanTitle.contains("Cashback", ignoreCase = true) ||
           instrumentName.equals("Bank Credit", ignoreCase = true) ||
           cleanNote.contains("Credit", ignoreCase = true) ||
           cleanNote.contains("received", ignoreCase = true) ||
           cleanNote.contains("settled", ignoreCase = true) ||
           cleanNote.contains("refund", ignoreCase = true)
}

fun TransactionEntity.isCreditCardBillPayment(): Boolean {
    return category.equals("BILL_PAYMENT", ignoreCase = true) ||
           category.equals("CC_PAYMENT", ignoreCase = true) ||
           title.contains("Credit Card Bill", ignoreCase = true) ||
           title.contains("CC Bill Payment", ignoreCase = true) ||
           note.contains("Credit Card Bill Payment", ignoreCase = true) ||
           note.contains("[CC_PAYMENT]", ignoreCase = true)
}

fun TransactionEntity.isUpiTransaction(): Boolean {
    return (originalSms?.contains("upi", ignoreCase = true) == true) ||
           note.contains("upi", ignoreCase = true) ||
           title.contains("upi", ignoreCase = true) ||
           instrumentName.contains("upi", ignoreCase = true)
}

fun TransactionEntity.isRefundOrCreditOffset(): Boolean {
    return isCreditOrInflow() && (
        category.equals("REFUND", ignoreCase = true) ||
        title.contains("Refund", ignoreCase = true) ||
        title.contains("Reversal", ignoreCase = true) ||
        title.contains("Cashback", ignoreCase = true) ||
        title.contains("Return", ignoreCase = true) ||
        note.contains("Refund", ignoreCase = true) ||
        note.contains("Reversal", ignoreCase = true) ||
        note.contains("Cashback", ignoreCase = true) ||
        note.contains("[OFFSET_EXPENSE]", ignoreCase = true)
    )
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
