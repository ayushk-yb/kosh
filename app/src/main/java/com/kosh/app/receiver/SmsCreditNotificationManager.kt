package com.kosh.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kosh.app.MainActivity
import com.kosh.app.R

object SmsCreditNotificationManager {
    const val CHANNEL_CREDITS_ID = "kosh_credit_inflows"
    const val CHANNEL_SPENDS_ID = "kosh_spends"
    const val CHANNEL_BILLS_ID = "kosh_bills"

    const val ACTION_MATCH_SPLIT = "com.kosh.app.ACTION_MATCH_SPLIT"
    const val ACTION_SPLIT_TRANSACTION = "com.kosh.app.ACTION_SPLIT"
    const val ACTION_CATEGORIZE_TRANSACTION = "com.kosh.app.ACTION_CATEGORIZE"

    const val EXTRA_SENDER_NAME = "extra_sender_name"
    const val EXTRA_AMOUNT = "extra_amount"
    const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    const val EXTRA_ACTION_TYPE = "extra_action_type"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val creditChannel = NotificationChannel(
                CHANNEL_CREDITS_ID,
                "Credit Inflows & Refunds",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for incoming money and split reconciliation"
                enableVibration(true)
                setShowBadge(true)
            }

            val spendChannel = NotificationChannel(
                CHANNEL_SPENDS_ID,
                "Transaction Spends & UPI",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up notifications on spends with quick split & categorize actions"
                enableVibration(true)
                setShowBadge(true)
            }

            val billChannel = NotificationChannel(
                CHANNEL_BILLS_ID,
                "Upcoming Bill Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for household rent, cook, maid, and recurring bills"
                enableVibration(true)
                setShowBadge(true)
            }

            manager.createNotificationChannel(creditChannel)
            manager.createNotificationChannel(spendChannel)
            manager.createNotificationChannel(billChannel)
        }
    }

    fun showDebitDetectedNotification(
        context: Context,
        transactionId: Long,
        merchantName: String,
        amount: Double,
        instrument: String
    ) {
        createNotificationChannels(context)

        // Tapping body opens MainActivity with transaction edit sheet
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            transactionId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Split Bill
        val splitIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            putExtra(EXTRA_ACTION_TYPE, ACTION_SPLIT_TRANSACTION)
        }
        val splitPendingIntent = PendingIntent.getActivity(
            context,
            (transactionId * 10 + 1).toInt(),
            splitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Categorize
        val catIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            putExtra(EXTRA_ACTION_TYPE, ACTION_CATEGORIZE_TRANSACTION)
        }
        val catPendingIntent = PendingIntent.getActivity(
            context,
            (transactionId * 10 + 2).toInt(),
            catIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SPENDS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("₹${amount.toInt()} spent at $merchantName")
            .setContentText("Paid via $instrument • Tap to split or categorize")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Paid ₹${amount.toInt()} via $instrument. Tap Split Bill to record flatmate shares, or Categorize to assign to living burn or wealth."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(0, "Split Bill", splitPendingIntent)
            .addAction(0, "Categorize", catPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(transactionId.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission not yet granted
        }
    }

    fun showCreditDetectedNotification(context: Context, senderName: String, amount: Double) {
        createNotificationChannels(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SENDER_NAME, senderName)
            putExtra(EXTRA_AMOUNT, amount)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val matchIntent = Intent(context, ReconciliationActionReceiver::class.java).apply {
            action = ACTION_MATCH_SPLIT
            putExtra(EXTRA_SENDER_NAME, senderName)
            putExtra(EXTRA_AMOUNT, amount)
        }
        val matchPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            matchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CREDITS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Credit Detected: ₹${amount.toInt()} from $senderName")
            .setContentText("Match this credit to open receivables or merchant refunds?")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "₹${amount.toInt()} received from $senderName. Tap Match Split to auto-reconcile open debts and settle ledger."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .addAction(0, "Match Split", matchPendingIntent)
            .addAction(0, "Dismiss", null)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (e: SecurityException) {
            // Permission not yet granted
        }
    }

    fun showBillDueNotification(context: Context, title: String, amount: Double, daysLeft: Int) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = when (daysLeft) {
            0 -> "🚨 $title is due today (₹${amount.toInt()})"
            1 -> "⏰ $title is due tomorrow (₹${amount.toInt()})"
            else -> "📅 $title is due in $daysLeft days (₹${amount.toInt()})"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_BILLS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titleText)
            .setContentText("Tap to mark as paid or split with flatmates")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Pay & Split", pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(title.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permission not yet granted
        }
    }
}
