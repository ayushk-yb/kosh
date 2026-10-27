package com.kosh.app.data.scanner

import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import com.kosh.app.data.local.KoshDatabase
import com.kosh.app.data.model.TransactionCategory
import com.kosh.app.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Locale
import java.util.regex.Pattern

data class SmsScanResult(
    val scannedCount: Int,
    val importedTransactions: Int,
    val detectedCredits: Int
)

object SmsAutoScanner {

    val amountPatterns = listOf(
        // "Amt Deducted! Rs.65000", "deducted Rs. 500", "debited by Rs. 500", "spent Rs. 500", "transferred Rs. 500"
        Pattern.compile("(?:amt\\s+deducted!?|deducted\\s*(?:by)?|debited\\s*(?:by)?|spent|sent|transferred|withdrawn|paid)\\s*(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        // "Amt Credited! Rs.65000", "credited by Rs. 500", "received Rs. 500", "refund of Rs. 500"
        Pattern.compile("(?:amt\\s+credited!?|credited\\s*(?:by|with)?|received|refund\\s+of)\\s*(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        // "Rs. 500 debited/spent/deducted/transferred/withdrawn/paid"
        Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:debited|spent|withdrawn|transferred|deducted|paid)", Pattern.CASE_INSENSITIVE),
        // "Rs. 500 credited/received/refunded"
        Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:credited|received|refunded)", Pattern.CASE_INSENSITIVE),
        // "NEFT txn via ... Rs. 65000" or "NEFT txn of Rs. 65000" or "for NEFT txn"
        Pattern.compile("(?:neft|imps|rtgs|upi)\\s*(?:txn|transfer)?\\s*(?:of|for)?\\s*(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        // Fallback: Rs.65000 anywhere in transaction message
        Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
    )

    private val merchantToPattern = Pattern.compile("(?:at|to|info:|via|towards)\\s+([A-Za-z0-9&\\.\\-_\\s]{3,24})(?:\\son|\\.|\\sthrough|\\susing|\\sref|$)", Pattern.CASE_INSENSITIVE)

    fun isIgnoredMessage(body: String): Boolean {
        val lower = body.lowercase()

        // 1. Reject OTP & Security Codes
        val otpKeywords = listOf(
            "otp",
            "one time password",
            "one-time password",
            "verification code",
            "verification pin",
            "security code",
            "auth code",
            "authorization code",
            "secret code",
            "valid for",
            "valid till",
            "valid up to",
            "do not share",
            "never share",
            "don't share",
            "cvv"
        )
        if (otpKeywords.any { lower.contains(it) }) return true

        // 2. Reject Promotional & Marketing Offers
        val promoKeywords = listOf(
            "pre-approved",
            "pre approved",
            "apply now",
            "instant loan",
            "personal loan",
            "congratulations",
            "cashback points",
            "reward points",
            "discount voucher",
            "special offer",
            "exclusive offer",
            "flat discount",
            "upgrade your card",
            "claim your",
            "win up to",
            "free voucher",
            "zero interest"
        )
        if (promoKeywords.any { lower.contains(it) }) return true

        // 3. Reject Statement / Due Reminders (prior to actual debits)
        val billNoticeKeywords = listOf(
            "bill generated",
            "statement generated",
            "minimum amount due",
            "total amount due",
            "due date is",
            "is due on",
            "payment is due"
        )
        if (billNoticeKeywords.any { lower.contains(it) }) return true

        // 4. Reject Mandate Requests, UPI Collect Requests & Failed Transactions
        val nonTxKeywords = listOf(
            "requested money",
            "request to pay",
            "has requested",
            "request for money",
            "collect request",
            "mandate request",
            "mandate created",
            "mandate",
            "autopay scheduled",
            "autopay request",
            "pre-auth",
            "preauth",
            "transaction failed",
            "txn failed",
            "payment failed",
            "request failed",
            "failed",
            "declined",
            "unsuccessful",
            "insufficient funds",
            "could not be processed",
            "reversed"
        )
        if (nonTxKeywords.any { lower.contains(it) }) return true

        return false
    }

    fun generateHash(timestamp: Long, amount: Double, title: String, instrument: String): String {
        // Round timestamp to seconds (since SMS provider timestamp is in ms)
        val sec = timestamp / 1000
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val normalizedTitle = title.trim().lowercase()
        val normalizedInstrument = instrument.trim().lowercase()
        val raw = "$sec|$formattedAmount|$normalizedTitle|$normalizedInstrument"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "${sec}_${formattedAmount}_${normalizedTitle.take(10)}"
        }
    }

    fun generateSignature(timestamp: Long, amount: Double, title: String): String {
        val sec = timestamp / 1000
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val normalizedTitle = title.trim().lowercase()
        return "${sec}_${formattedAmount}_$normalizedTitle"
    }

    fun generateMinuteSignature(timestamp: Long, amount: Double, title: String): String {
        val min = timestamp / 60000
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val normalizedTitle = title.trim().lowercase()
        return "min_${min}_${formattedAmount}_$normalizedTitle"
    }

    fun generateDaySignature(timestamp: Long, amount: Double, title: String): String {
        val day = timestamp / 86400000
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val normalizedTitle = title.trim().lowercase()
        return "day_${day}_${formattedAmount}_$normalizedTitle"
    }

    suspend fun scanInbox(
        context: Context,
        database: KoshDatabase,
        forceFullScan: Boolean = false
    ): SmsScanResult = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("kosh_scanner_prefs", Context.MODE_PRIVATE)
        val lastScannedTimestamp = if (forceFullScan) 0L else prefs.getLong("last_scanned_sms_timestamp", 0L)
        var maxTimestampFound = lastScannedTimestamp

        val selection = if (lastScannedTimestamp > 0L) "${Telephony.Sms.DATE} > ?" else null
        val selectionArgs = if (lastScannedTimestamp > 0L) arrayOf(lastScannedTimestamp.toString()) else null

        val resolver = context.contentResolver
        val cursor: Cursor? = resolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            ),
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} DESC"
        )

        var scannedCount = 0
        var importedCount = 0
        var detectedCredits = 0

        // Fetch existing and deleted hashes for O(1) deduplication check
        val allExistingTransactions = database.transactionDao().getAllTransactionsList()
        val existingHashes = database.transactionDao().getAllSmsRefHashes().toHashSet()
        val existingSignatures = HashSet<String>()
        for (it in allExistingTransactions) {
            existingSignatures.add(generateSignature(it.timestamp, it.grossAmount, it.title))
            existingSignatures.add(generateMinuteSignature(it.timestamp, it.grossAmount, it.title))
            existingSignatures.add(generateDaySignature(it.timestamp, it.grossAmount, it.title))
        }
        val deletedHashes = database.deletedTransactionHashDao().getAllDeletedHashes().toHashSet()

        cursor?.use { c ->
            val addressCol = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyCol = c.getColumnIndex(Telephony.Sms.BODY)
            val dateCol = c.getColumnIndex(Telephony.Sms.DATE)

            while (c.moveToNext()) {
                scannedCount++
                val address = if (addressCol >= 0) c.getString(addressCol) ?: "" else ""
                val body = if (bodyCol >= 0) c.getString(bodyCol) ?: "" else ""
                val date = if (dateCol >= 0) c.getLong(dateCol) else System.currentTimeMillis()
                if (date > maxTimestampFound) {
                    maxTimestampFound = date
                }

                // 1. Strict OTP & Promo Rejection Filter
                if (isIgnoredMessage(body)) continue

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
                for (p in amountPatterns) {
                    val m = p.matcher(body)
                    if (m.find()) {
                        val raw = m.group(1)?.replace(",", "")
                        amount = raw?.toDoubleOrNull()
                        if (amount != null && amount > 0) break
                    }
                }

                if (amount == null || amount <= 0) continue

                // 3. Identify Category, Title, & Instrument
                val (category, isWealth) = categorize(lower)
                val title = extractTitle(body, lower, address, category)
                val instrument = identifyInstrument(address, lower)

                // 4. Deterministic Deduplication & Deleted Transaction Check
                val hash = generateHash(date, amount, title, instrument)
                val signature = generateSignature(date, amount, title)
                val minuteSig = generateMinuteSignature(date, amount, title)
                val daySig = generateDaySignature(date, amount, title)

                // If user deleted this transaction previously, NEVER import again
                if (deletedHashes.contains(hash) ||
                    deletedHashes.contains(signature) ||
                    deletedHashes.contains(minuteSig) ||
                    deletedHashes.contains(daySig)
                ) {
                    continue
                }

                // If already existing in database, skip duplicate
                if (existingHashes.contains(hash) ||
                    existingSignatures.contains(signature) ||
                    existingSignatures.contains(minuteSig) ||
                    existingSignatures.contains(daySig)
                ) {
                    continue
                }

                if (isCredit) {
                    detectedCredits++
                }

                // Auto-discover Payment Instruments from SMS
                try {
                    val cardPattern = Pattern.compile("(?:card|a/c|acct|account)\\s*(?:ending\\s*(?:with|in)?\\s*)?(?:[x\\*]{2,}|no\\.?\\s*)?(\\d{4})", Pattern.CASE_INSENSITIVE)
                    val cm = cardPattern.matcher(body)
                    if (cm.find()) {
                        val last4 = cm.group(1)
                        if (!last4.isNullOrBlank()) {
                            val existing = database.paymentInstrumentDao().getByLast4(last4)
                            if (existing == null) {
                                val isCard = lower.contains("card") || lower.contains("credit")
                                database.paymentInstrumentDao().insertInstrument(
                                    com.kosh.app.data.model.PaymentInstrumentEntity(
                                        bankName = instrument,
                                        cardOrAccountName = if (isCard) "$instrument Card" else "$instrument Account",
                                        last4Digits = last4,
                                        instrumentType = if (isCard) "CREDIT_CARD" else "BANK_ACCOUNT",
                                        creditLimit = if (isCard) 100000.0 else 0.0,
                                        unbilledSpend = 0.0,
                                        badgeText = "SMS Detected"
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore card parsing exception
                }

                // Insert into Database with Hash
                val tx = TransactionEntity(
                    title = title,
                    grossAmount = amount,
                    trueNetAmount = if (isWealth || isCredit) 0.0 else amount,
                    reimbursableAmount = 0.0,
                    category = if (isCredit) "INFLOW" else category.name,
                    instrumentName = instrument,
                    timestamp = date,
                    isAssetWealth = isWealth,
                    note = if (isCredit) "Credit / Inflow parsed from SMS" else "Auto-parsed from SMS",
                    hasSplit = false,
                    smsRefHash = hash
                )

                database.transactionDao().insertTransaction(tx)
                existingHashes.add(hash)
                existingSignatures.add(signature)
                existingSignatures.add(minuteSig)
                existingSignatures.add(daySig)
                importedCount++
            }
        }

        if (maxTimestampFound > lastScannedTimestamp) {
            prefs.edit().putLong("last_scanned_sms_timestamp", maxTimestampFound).apply()
        }

        SmsScanResult(scannedCount, importedCount, detectedCredits)
    }

    private fun categorize(lower: String): Pair<TransactionCategory, Boolean> {
        return when {
            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("metro") || lower.contains("chalo") ->
                Pair(TransactionCategory.CAB_COMMUTE, false)

            lower.contains("sip") || lower.contains("mutual fund") || lower.contains("zerodha") || lower.contains("groww") || lower.contains("amfi") || lower.contains("parag parikh") || lower.contains("uti") ->
                Pair(TransactionCategory.SIP_WEALTH, true)

            lower.contains("rent") || lower.contains("society") || lower.contains("maintenance") || lower.contains("nobroker") || lower.contains("mygate") ->
                Pair(TransactionCategory.RENT, false)

            lower.contains("cook") || lower.contains("maid") || lower.contains("salary") ->
                Pair(TransactionCategory.COOK_MAID, false)

            lower.contains("gift") || lower.contains("flower") || lower.contains("fnp") || lower.contains("igp") || lower.contains("archies") || lower.contains("cake") || lower.contains("baker") ->
                Pair(TransactionCategory.GIFTS, false)

            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") || lower.contains("bigbasket") || lower.contains("starbucks") ->
                Pair(TransactionCategory.GROCERIES, false)

            lower.contains("airtel") || lower.contains("jio") || lower.contains("vi") || lower.contains("electricity") || lower.contains("bescom") || lower.contains("tatapower") || lower.contains("billdesk") ->
                Pair(TransactionCategory.UTILITIES, false)

            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("zara") || lower.contains("ajio") ->
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
            lower.contains("flipkart") -> return "Flipkart Online"
            lower.contains("zara") -> return "Zara Apparel"
            lower.contains("starbucks") -> return "Starbucks Coffee"
            lower.contains("fnp") || lower.contains("ferns") -> return "Ferns N Petals Gift"
            lower.contains("igp") -> return "IGP Gift Delivery"
            lower.contains("archies") -> return "Archies Gifts"
            lower.contains("cake") || lower.contains("baker") -> return "Cake & Bakery Treat"
            lower.contains("gift") -> return "Gift Purchase"
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
