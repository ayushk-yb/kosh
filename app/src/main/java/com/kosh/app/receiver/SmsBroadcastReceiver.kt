package com.kosh.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.kosh.app.data.local.KoshDatabase
import com.kosh.app.data.model.TransactionCategory
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.data.scanner.SmsAutoScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class SmsBroadcastReceiver : BroadcastReceiver() {

    private val amountPatterns = SmsAutoScanner.amountPatterns
    private val merchantToPattern = Pattern.compile("(?:at|to|info:|via|towards)\\s+([A-Za-z0-9&\\.\\-_\\s]{3,24})(?:\\son|\\.|\\sthrough|\\susing|\\sref|$)", Pattern.CASE_INSENSITIVE)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = KoshDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))

                for (sms in messages) {
                    val body = sms.messageBody ?: continue
                    val date = sms.timestampMillis
                    val address = sms.originatingAddress ?: ""

                    // 1. Strict OTP & Promo Rejection Filter
                    if (SmsAutoScanner.isIgnoredMessage(body)) continue

                    val lower = body.lowercase()
                    val isDebit = lower.contains("debited") ||
                            lower.contains("spent") ||
                            lower.contains("paid") ||
                            lower.contains("sent rs") ||
                            lower.contains("deducted") ||
                            lower.contains("amt deducted") ||
                            lower.contains("withdrawn") ||
                            lower.contains("transferred") ||
                            (lower.contains("neft") && (lower.contains("from") || lower.contains("deducted") || lower.contains("debit") || lower.contains("dr"))) ||
                            (lower.contains("imps") && (lower.contains("from") || lower.contains("deducted") || lower.contains("debit") || lower.contains("dr"))) ||
                            (lower.contains("rtgs") && (lower.contains("from") || lower.contains("deducted") || lower.contains("debit") || lower.contains("dr")))

                    val isCredit = lower.contains("credited") ||
                            lower.contains("amt credited") ||
                            lower.contains("received") ||
                            lower.contains("refund") ||
                            lower.contains("cr to") ||
                            (lower.contains("neft") && (lower.contains("to your") || lower.contains("credit") || lower.contains("cr"))) ||
                            (lower.contains("imps") && (lower.contains("to your") || lower.contains("credit") || lower.contains("cr")))

                    if (!isDebit && !isCredit) continue

                    // 2. Extract Amount
                    var amount: Double? = null
                    for (p in SmsAutoScanner.amountPatterns) {
                        val m = p.matcher(body)
                        if (m.find()) {
                            val raw = m.group(1)?.replace(",", "")
                            amount = raw?.toDoubleOrNull()
                            if (amount != null && amount > 0) break
                        }
                    }

                    if (amount == null || amount <= 0) continue

                    // 3. Categorize & Title
                    val (category, isWealth) = categorize(lower)
                    val title = extractTitle(body, lower, address, category)
                    val instrument = identifyInstrument(address, lower)

                    // 4. Deterministic Hash & Deduplication check
                    val hash = SmsAutoScanner.generateHash(date, amount, title, instrument)
                    val minuteSig = SmsAutoScanner.generateMinuteSignature(date, amount, title)
                    val daySig = SmsAutoScanner.generateDaySignature(date, amount, title)

                    // Check if already deleted by user or already inserted
                    if (db.deletedTransactionHashDao().isHashDeleted(hash) ||
                        db.deletedTransactionHashDao().isHashDeleted(minuteSig) ||
                        db.deletedTransactionHashDao().isHashDeleted(daySig)
                    ) continue
                    if (db.transactionDao().getAllSmsRefHashes().contains(hash)) continue

                    // 5. Insert Transaction
                    val tx = TransactionEntity(
                        title = title,
                        grossAmount = amount,
                        trueNetAmount = if (isWealth || isCredit) 0.0 else amount,
                        reimbursableAmount = 0.0,
                        category = if (isCredit) "INFLOW" else category.name,
                        instrumentName = instrument,
                        timestamp = date,
                        isAssetWealth = isWealth,
                        note = if (isCredit) "Credit received live from SMS" else "Live spend parsed from SMS",
                        hasSplit = false,
                        smsRefHash = hash
                    )
                    val txId = db.transactionDao().insertTransaction(tx)

                    // 6. Trigger Heads-Up Notification
                    if (isDebit) {
                        SmsCreditNotificationManager.showDebitDetectedNotification(
                            context = context,
                            transactionId = txId,
                            merchantName = title,
                            amount = amount,
                            instrument = instrument
                        )
                    } else {
                        // Credit
                        var sender = "Contact"
                        val fromPattern = Pattern.compile("(?:from|by)\\s+([A-Za-z]+)", Pattern.CASE_INSENSITIVE)
                        val fromMatcher = fromPattern.matcher(body)
                        if (fromMatcher.find()) {
                            sender = fromMatcher.group(1)?.replaceFirstChar { it.uppercase() } ?: "Contact"
                        }
                        SmsCreditNotificationManager.showCreditDetectedNotification(context, sender, amount)
                    }
                }
            } catch (e: Exception) {
                // Ignore background broadcast error
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun categorize(lower: String): Pair<TransactionCategory, Boolean> {
        return when {
            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("metro") || lower.contains("chalo") ->
                Pair(TransactionCategory.CAB_COMMUTE, false)

            lower.contains("sip") || lower.contains("mutual fund") || lower.contains("zerodha") || lower.contains("groww") || lower.contains("amfi") ->
                Pair(TransactionCategory.SIP_WEALTH, true)

            lower.contains("rent") || lower.contains("society") || lower.contains("maintenance") ->
                Pair(TransactionCategory.RENT, false)

            lower.contains("cook") || lower.contains("maid") || lower.contains("salary") ->
                Pair(TransactionCategory.COOK_MAID, false)

            lower.contains("gift") || lower.contains("flower") || lower.contains("fnp") || lower.contains("igp") || lower.contains("archies") || lower.contains("cake") || lower.contains("baker") ->
                Pair(TransactionCategory.GIFTS, false)

            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") ->
                Pair(TransactionCategory.GROCERIES, false)

            lower.contains("airtel") || lower.contains("jio") || lower.contains("electricity") || lower.contains("bescom") ->
                Pair(TransactionCategory.UTILITIES, false)

            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("zara") ->
                Pair(TransactionCategory.SHOPPING, false)

            else -> Pair(TransactionCategory.OTHER, false)
        }
    }

    private fun extractTitle(body: String, lower: String, address: String, category: TransactionCategory): String {
        if (lower.contains("neft")) {
            val inst = identifyInstrument(address, lower)
            return if (lower.contains("credited") || lower.contains("received") || lower.contains("credit") || lower.contains("cr")) {
                "$inst NEFT Inflow"
            } else {
                "$inst NEFT Transfer"
            }
        }
        if (lower.contains("imps")) {
            val inst = identifyInstrument(address, lower)
            return if (lower.contains("credited") || lower.contains("received") || lower.contains("credit") || lower.contains("cr")) {
                "$inst IMPS Inflow"
            } else {
                "$inst IMPS Transfer"
            }
        }
        if (lower.contains("rtgs")) {
            val inst = identifyInstrument(address, lower)
            return if (lower.contains("credited") || lower.contains("received") || lower.contains("credit") || lower.contains("cr")) {
                "$inst RTGS Inflow"
            } else {
                "$inst RTGS Transfer"
            }
        }

        when {
            lower.contains("uber") -> return "Uber Premier Cab"
            lower.contains("ola") -> return "Ola Ride"
            lower.contains("rapido") -> return "Rapido Bike / Auto"
            lower.contains("swiggy") -> return "Swiggy Food / Instamart"
            lower.contains("zomato") -> return "Zomato Dining"
            lower.contains("blinkit") -> return "Blinkit Quick Commerce"
            lower.contains("zepto") -> return "Zepto Groceries"
            lower.contains("amazon") -> return "Amazon India Purchase"
            lower.contains("fnp") || lower.contains("ferns") -> return "Ferns N Petals Gift"
            lower.contains("igp") -> return "IGP Gift Delivery"
            lower.contains("archies") -> return "Archies Gifts"
            lower.contains("cake") || lower.contains("baker") -> return "Cake & Bakery Treat"
            lower.contains("gift") -> return "Gift Purchase"
            lower.contains("flipkart") -> return "Flipkart Online"
            lower.contains("zara") -> return "Zara Apparel"
            lower.contains("airtel") -> return "Airtel Broadband / Xstream"
            lower.contains("jio") -> return "Jio Fiber / Mobile"
            lower.contains("groww") -> return "Groww Wealth SIP"
            lower.contains("zerodha") -> return "Zerodha Coin Investment"
        }

        val m = merchantToPattern.matcher(body)
        if (m.find()) {
            val merchant = m.group(1)?.trim()?.replace("\n", " ")
            if (!merchant.isNullOrBlank() && merchant.length in 3..25) {
                return merchant.split(" ").take(3).joinToString(" ")
            }
        }

        return "${category.displayName} Spend"
    }

    private fun identifyInstrument(address: String, lower: String): String {
        val upperAddr = address.uppercase()
        return when {
            upperAddr.contains("HDFC") || lower.contains("hdfc") -> "HDFC Bank"
            upperAddr.contains("SBI") || lower.contains("sbi") -> "SBI Card"
            upperAddr.contains("ICICI") || lower.contains("icici") -> "ICICI Bank"
            upperAddr.contains("AXIS") || lower.contains("axis") -> "Axis Bank"
            upperAddr.contains("KOTAK") || lower.contains("kotak") -> "Kotak Bank"
            upperAddr.contains("PNB") || lower.contains("pnb") -> "PNB Bank"
            upperAddr.contains("CANARA") || lower.contains("canara") -> "Canara Bank"
            upperAddr.contains("BOB") || lower.contains("baroda") -> "Bank of Baroda"
            upperAddr.contains("IDFC") || lower.contains("idfc") -> "IDFC FIRST"
            upperAddr.contains("INDUS") || lower.contains("indusind") -> "IndusInd Bank"
            upperAddr.contains("YES") || lower.contains("yes bank") -> "Yes Bank"
            upperAddr.contains("FED") || lower.contains("federal") -> "Federal Bank"
            upperAddr.contains("RBL") || lower.contains("rbl") -> "RBL Bank"
            lower.contains("upi") -> "UPI Account"
            else -> "Bank Account"
        }
    }
}
