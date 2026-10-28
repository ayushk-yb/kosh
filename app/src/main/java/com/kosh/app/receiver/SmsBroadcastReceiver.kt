package com.kosh.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.kosh.app.data.local.KoshDatabase
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.data.scanner.SmsAutoScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class SmsBroadcastReceiver : BroadcastReceiver() {

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

                    // Centralized parse using SmsAutoScanner
                    val parsed = SmsAutoScanner.parseSms(body, address)
                    if (!parsed.isValid || parsed.amount == null || parsed.amount <= 0) continue

                    // Deduplication check
                    val hash = SmsAutoScanner.generateHash(date, parsed.amount, parsed.title, parsed.instrument)
                    val minuteSig = SmsAutoScanner.generateMinuteSignature(date, parsed.amount, parsed.title)
                    val daySig = SmsAutoScanner.generateDaySignature(date, parsed.amount, parsed.title)

                    if (db.deletedTransactionHashDao().isHashDeleted(hash) ||
                        db.deletedTransactionHashDao().isHashDeleted(minuteSig) ||
                        db.deletedTransactionHashDao().isHashDeleted(daySig)
                    ) continue
                    if (db.transactionDao().getAllSmsRefHashes().contains(hash)) continue

                    // Auto-discover Payment Instruments
                    try {
                        val last4 = parsed.last4
                        if (!last4.isNullOrBlank()) {
                            val existing = db.paymentInstrumentDao().getByLast4(last4)
                            if (existing == null) {
                                val lower = body.lowercase()
                                val isCreditCard = (lower.contains("credit card") || lower.contains("card ending") || lower.contains("card xx") || lower.contains("card no")) &&
                                                   !lower.contains("credited") && !lower.contains("a/c") && !lower.contains("account")

                                db.paymentInstrumentDao().insertInstrument(
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

                    // Insert Transaction with original SMS body
                    val tx = TransactionEntity(
                        title = parsed.title,
                        grossAmount = parsed.amount,
                        trueNetAmount = if (parsed.isWealth || parsed.isCredit) 0.0 else parsed.amount,
                        reimbursableAmount = 0.0,
                        category = if (parsed.isCredit) "INFLOW" else parsed.category.name,
                        instrumentName = parsed.instrument,
                        timestamp = date,
                        isAssetWealth = parsed.isWealth,
                        note = if (parsed.isCredit) "Credit received live from SMS" else "Live spend parsed from SMS",
                        hasSplit = false,
                        smsRefHash = hash,
                        originalSms = body
                    )
                    val txId = db.transactionDao().insertTransaction(tx)

                    // Trigger Heads-Up Notification
                    if (parsed.isDebit) {
                        SmsCreditNotificationManager.showDebitDetectedNotification(
                            context = context,
                            transactionId = txId,
                            merchantName = parsed.title,
                            amount = parsed.amount,
                            instrument = parsed.instrument
                        )
                    } else {
                        // Credit
                        var sender = "Contact"
                        val fromPattern = Pattern.compile("(?:from|by)\\s+([A-Za-z]+)", Pattern.CASE_INSENSITIVE)
                        val fromMatcher = fromPattern.matcher(body)
                        if (fromMatcher.find()) {
                            sender = fromMatcher.group(1)?.replaceFirstChar { it.uppercase() } ?: "Contact"
                        }
                        SmsCreditNotificationManager.showCreditDetectedNotification(context, sender, parsed.amount)
                    }
                }
            } catch (e: Exception) {
                // Ignore background broadcast error
            } finally {
                pendingResult.finish()
            }
        }
    }
}
