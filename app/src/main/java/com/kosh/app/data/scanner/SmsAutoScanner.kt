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

data class ParsedSmsResult(
    val rawBody: String,
    val address: String,
    val isValid: Boolean,
    val isIgnored: Boolean,
    val ignoreReason: String?,
    val isDebit: Boolean,
    val isCredit: Boolean,
    val isMandate: Boolean,
    val amount: Double?,
    val matchedAmountPattern: String?,
    val category: TransactionCategory,
    val isWealth: Boolean,
    val title: String,
    val instrument: String,
    val last4: String?,
    val matchedKeywords: List<String>
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
    private val mandateMerchantPattern1 = Pattern.compile("towards\\s+(?:upi\\s+)?mandate\\s+(?:to|for)\\s+([A-Za-z0-9&\\.\\-_\\s]{3,25})", Pattern.CASE_INSENSITIVE)
    private val mandateMerchantPattern2 = Pattern.compile("mandate\\s+execution\\s+(?:for|to)\\s+([A-Za-z0-9&\\.\\-_\\s]{3,25})", Pattern.CASE_INSENSITIVE)
    private val mandateMerchantPattern3 = Pattern.compile("for\\s+mandate\\s+towards\\s+([A-Za-z0-9&\\.\\-_\\s]{3,25})", Pattern.CASE_INSENSITIVE)

    fun isDebit(body: String): Boolean {
        val lower = body.lowercase()
        return lower.contains("debited") ||
                lower.contains("spent") ||
                lower.contains("paid") ||
                lower.contains("sent rs") ||
                lower.contains("deducted") ||
                lower.contains("amt deducted") ||
                lower.contains("withdrawn") ||
                lower.contains("transferred") ||
                lower.contains("mandate execution") ||
                lower.contains("towards mandate") ||
                lower.contains("for mandate") ||
                lower.contains("towards upi mandate") ||
                lower.contains("mandate debited") ||
                lower.contains("autopay debited") ||
                lower.contains("si debited") ||
                (lower.contains("neft") && (lower.contains("from") || lower.contains("deducted") || lower.contains("debit") || lower.contains("dr"))) ||
                (lower.contains("imps") && (lower.contains("from") || lower.contains("deducted") || lower.contains("debit") || lower.contains("dr"))) ||
                (lower.contains("rtgs") && (lower.contains("from") || lower.contains("deducted") || lower.contains("debit") || lower.contains("dr")))
    }

    fun isCredit(body: String): Boolean {
        val lower = body.lowercase()
        return lower.contains("credited") ||
                lower.contains("amt credited") ||
                lower.contains("received") ||
                lower.contains("refund") ||
                lower.contains("cr to") ||
                (lower.contains("neft") && (lower.contains("to your") || lower.contains("credit") || lower.contains("cr"))) ||
                (lower.contains("imps") && (lower.contains("to your") || lower.contains("credit") || lower.contains("cr")))
    }

    fun extractAmount(body: String): Pair<Double?, String?> {
        for ((idx, p) in amountPatterns.withIndex()) {
            val m = p.matcher(body)
            if (m.find()) {
                val raw = m.group(1)?.replace(",", "")
                val amt = raw?.toDoubleOrNull()
                if (amt != null && amt > 0) {
                    val desc = when (idx) {
                        0 -> "Debit prefix (e.g. 'debited by Rs')"
                        1 -> "Credit prefix (e.g. 'credited by Rs')"
                        2 -> "Debit suffix (e.g. 'Rs. ... debited')"
                        3 -> "Credit suffix (e.g. 'Rs. ... credited')"
                        4 -> "NEFT/IMPS/UPI prefix"
                        else -> "Fallback currency pattern"
                    }
                    return Pair(amt, desc)
                }
            }
        }
        return Pair(null, null)
    }

    fun getIgnoreReason(body: String): String? {
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
        for (kw in otpKeywords) {
            if (lower.contains(kw)) return "OTP / Security code ('$kw')"
        }

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
        for (kw in promoKeywords) {
            if (lower.contains(kw)) return "Promotional offer ('$kw')"
        }

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
        for (kw in billNoticeKeywords) {
            if (lower.contains(kw)) return "Bill notice / Statement generated ('$kw')"
        }

        // 4. Reject Mandate Requests, Pre-auth & Failed Transactions
        // If this message is an actual executed debit, do NOT reject for mandate/autopay keywords!
        val actualDebit = isDebit(body)

        val nonTxKeywords = listOf(
            "requested money",
            "request to pay",
            "has requested",
            "request for money",
            "collect request",
            "mandate request",
            "mandate registered",
            "mandate scheduled",
            "mandate created",
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

        for (kw in nonTxKeywords) {
            if (lower.contains(kw)) {
                if (actualDebit && (kw.contains("mandate") || kw.contains("autopay"))) {
                    // Executed debit with mandate execution info, do not reject!
                    continue
                }
                return "Failed / Request / Non-transaction ('$kw')"
            }
        }

        return null
    }

    fun isIgnoredMessage(body: String): Boolean {
        return getIgnoreReason(body) != null
    }

    fun extractLast4(body: String): String? {
        val cardPattern = Pattern.compile("(?:card|a/c|acct|account)\\s*(?:ending\\s*(?:with|in)?\\s*)?(?:[x\\*]{2,}|no\\.?\\s*)?(\\d{4})", Pattern.CASE_INSENSITIVE)
        val cm = cardPattern.matcher(body)
        if (cm.find()) {
            val last4 = cm.group(1)
            if (!last4.isNullOrBlank()) return last4
        }
        return null
    }

    fun identifyInstrument(address: String, lower: String, last4: String? = null): String {
        val upperAddr = address.uppercase()
        val base = when {
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
        return if (!last4.isNullOrBlank()) "$base ($last4)" else base
    }

    fun categorize(lower: String): Pair<TransactionCategory, Boolean> {
        return when {
            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("metro") || lower.contains("chalo") ->
                Pair(TransactionCategory.CAB_COMMUTE, false)

            lower.contains("sip") || lower.contains("mutual fund") || lower.contains("zerodha") || lower.contains("groww") ||
            lower.contains("amfi") || lower.contains("parag parikh") || lower.contains("uti") || lower.contains("nippon") ->
                Pair(TransactionCategory.SIP_WEALTH, true)

            lower.contains("rent") || lower.contains("society") || lower.contains("maintenance") || lower.contains("nobroker") || lower.contains("mygate") ->
                Pair(TransactionCategory.RENT, false)

            lower.contains("cook") || lower.contains("maid") || lower.contains("salary") ->
                Pair(TransactionCategory.COOK_MAID, false)

            lower.contains("gift") || lower.contains("flower") || lower.contains("fnp") || lower.contains("igp") || lower.contains("archies") || lower.contains("cake") || lower.contains("baker") ->
                Pair(TransactionCategory.GIFTS, false)

            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") || lower.contains("bigbasket") || lower.contains("starbucks") ->
                Pair(TransactionCategory.GROCERIES, false)

            lower.contains("airtel") || lower.contains("jio") || lower.contains("vi") || lower.contains("electricity") || lower.contains("bescom") || lower.contains("tatapower") || lower.contains("billdesk") ||
            lower.contains("spotify") || lower.contains("netflix") || lower.contains("hotstar") || lower.contains("prime") || lower.contains("youtube") ->
                Pair(TransactionCategory.UTILITIES, false)

            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("zara") || lower.contains("ajio") ->
                Pair(TransactionCategory.SHOPPING, false)

            else -> Pair(TransactionCategory.OTHER, false)
        }
    }

    fun extractTitle(body: String, lower: String, address: String, category: TransactionCategory, instrument: String = ""): String {
        // 1. Subscription & Mandate Specifics
        if (lower.contains("spotify")) return "Spotify Subscription"
        if (lower.contains("netflix")) return "Netflix Subscription"
        if (lower.contains("hotstar")) return "Disney+ Hotstar"
        if (lower.contains("prime")) return "Amazon Prime"
        if (lower.contains("youtube")) return "YouTube Premium"

        val mm1 = mandateMerchantPattern1.matcher(body)
        if (mm1.find()) {
            val merchant = mm1.group(1)?.trim()?.replace("\n", " ")
            if (!merchant.isNullOrBlank() && merchant.length in 3..25) {
                val clean = merchant.split(" ").take(3).joinToString(" ").replace(Regex("(?i)\\s+(via|using|on|ref).*"), "")
                if (clean.isNotBlank()) return clean
            }
        }

        val mm2 = mandateMerchantPattern2.matcher(body)
        if (mm2.find()) {
            val merchant = mm2.group(1)?.trim()?.replace("\n", " ")
            if (!merchant.isNullOrBlank() && merchant.length in 3..25) {
                val clean = merchant.split(" ").take(3).joinToString(" ").replace(Regex("(?i)\\s+(via|using|on|ref).*"), "")
                if (clean.isNotBlank()) return clean
            }
        }

        val mm3 = mandateMerchantPattern3.matcher(body)
        if (mm3.find()) {
            val merchant = mm3.group(1)?.trim()?.replace("\n", " ")
            if (!merchant.isNullOrBlank() && merchant.length in 3..25) {
                val clean = merchant.split(" ").take(3).joinToString(" ").replace(Regex("(?i)\\s+(via|using|on|ref).*"), "")
                if (clean.isNotBlank()) return clean
            }
        }

        if (lower.contains("mandate") || lower.contains("autopay")) {
            val inst = if (instrument.isNotBlank()) instrument else identifyInstrument(address, lower)
            return "$inst UPI Mandate"
        }

        // 2. NEFT / IMPS / RTGS
        if (lower.contains("neft")) {
            val inst = if (instrument.isNotBlank()) instrument else identifyInstrument(address, lower)
            return if (lower.contains("credited") || lower.contains("received") || lower.contains("credit") || lower.contains("cr")) {
                "$inst NEFT Inflow"
            } else {
                "$inst NEFT Transfer"
            }
        }
        if (lower.contains("imps")) {
            val inst = if (instrument.isNotBlank()) instrument else identifyInstrument(address, lower)
            return if (lower.contains("credited") || lower.contains("received") || lower.contains("credit") || lower.contains("cr")) {
                "$inst IMPS Inflow"
            } else {
                "$inst IMPS Transfer"
            }
        }
        if (lower.contains("rtgs")) {
            val inst = if (instrument.isNotBlank()) instrument else identifyInstrument(address, lower)
            return if (lower.contains("credited") || lower.contains("received") || lower.contains("credit") || lower.contains("cr")) {
                "$inst RTGS Inflow"
            } else {
                "$inst RTGS Transfer"
            }
        }

        // 3. Known Merchants
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

    fun parseSms(body: String, address: String = ""): ParsedSmsResult {
        val lower = body.lowercase()
        val ignoreReason = getIgnoreReason(body)
        val isIgnored = ignoreReason != null
        val debit = isDebit(body)
        val credit = isCredit(body)
        val mandate = lower.contains("mandate") || lower.contains("autopay")
        val (amt, amtDesc) = extractAmount(body)
        val last4 = extractLast4(body)
        val instrument = identifyInstrument(address, lower, last4)
        val (category, isWealth) = categorize(lower)
        val title = extractTitle(body, lower, address, category, instrument)

        // Find candidate trigger keywords in the text
        val candidateList = listOf(
            "debited", "credited", "deducted", "paid", "spent", "received",
            "refund", "transferred", "withdrawn", "mandate", "autopay",
            "neft", "imps", "rtgs", "upi", "card", "a/c", "account",
            "hdfc", "sbi", "icici", "axis", "kotak", "idfc"
        )
        val matchedKws = candidateList.filter { lower.contains(it) }.toMutableList()
        if (last4 != null) matchedKws.add("Ending $last4")

        val isValid = !isIgnored && (amt != null && amt > 0) && (debit || credit)

        return ParsedSmsResult(
            rawBody = body,
            address = address,
            isValid = isValid,
            isIgnored = isIgnored,
            ignoreReason = ignoreReason,
            isDebit = debit,
            isCredit = credit,
            isMandate = mandate,
            amount = amt,
            matchedAmountPattern = amtDesc,
            category = category,
            isWealth = isWealth,
            title = title,
            instrument = instrument,
            last4 = last4,
            matchedKeywords = matchedKws
        )
    }

    fun generateHash(timestamp: Long, amount: Double, title: String, instrument: String): String {
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

                val parsed = parseSms(body, address)
                if (!parsed.isValid || parsed.amount == null || parsed.amount <= 0) continue

                val hash = generateHash(date, parsed.amount, parsed.title, parsed.instrument)
                val signature = generateSignature(date, parsed.amount, parsed.title)
                val minuteSig = generateMinuteSignature(date, parsed.amount, parsed.title)
                val daySig = generateDaySignature(date, parsed.amount, parsed.title)

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

                if (parsed.isCredit) {
                    detectedCredits++
                }

                // Auto-discover Payment Instruments from SMS
                try {
                    val last4 = parsed.last4
                    if (!last4.isNullOrBlank()) {
                        val existing = database.paymentInstrumentDao().getByLast4(last4)
                        if (existing == null) {
                            val lower = body.lowercase()
                            val isCreditCard = (lower.contains("credit card") || lower.contains("card ending") || lower.contains("card xx") || lower.contains("card no")) &&
                                               !lower.contains("credited") && !lower.contains("a/c") && !lower.contains("account")

                            database.paymentInstrumentDao().insertInstrument(
                                com.kosh.app.data.model.PaymentInstrumentEntity(
                                    bankName = parsed.instrument.substringBefore(" ("),
                                    cardOrAccountName = if (isCreditCard) "${parsed.instrument.substringBefore(" (")} Card" else "${parsed.instrument.substringBefore(" (")} Account",
                                    last4Digits = last4,
                                    instrumentType = if (isCreditCard) "CREDIT_CARD" else "BANK_ACCOUNT",
                                    creditLimit = if (isCreditCard) 100000.0 else 0.0,
                                    unbilledSpend = 0.0,
                                    badgeText = "SMS Detected"
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignore instrument parsing exception
                }

                // Insert into Database with Hash and original SMS
                val tx = TransactionEntity(
                    title = parsed.title,
                    grossAmount = parsed.amount,
                    trueNetAmount = if (parsed.isWealth || parsed.isCredit) 0.0 else parsed.amount,
                    reimbursableAmount = 0.0,
                    category = if (parsed.isCredit) "INFLOW" else parsed.category.name,
                    instrumentName = parsed.instrument,
                    timestamp = date,
                    isAssetWealth = parsed.isWealth,
                    note = if (parsed.isCredit) "Credit / Inflow parsed from SMS" else "Auto-parsed from SMS",
                    hasSplit = false,
                    smsRefHash = hash,
                    originalSms = body
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
}
