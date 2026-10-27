package com.kosh.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.kosh.app.KoshApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReconciliationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SmsCreditNotificationManager.ACTION_MATCH_SPLIT) {
            val sender = intent.getStringExtra(SmsCreditNotificationManager.EXTRA_SENDER_NAME) ?: "Rahul"
            val amount = intent.getDoubleExtra(SmsCreditNotificationManager.EXTRA_AMOUNT, 12000.0)

            NotificationManagerCompat.from(context).cancel(SmsCreditNotificationManager.NOTIFICATION_ID)

            val app = context.applicationContext as KoshApplication
            CoroutineScope(Dispatchers.IO).launch {
                val result = app.repository.reconcileCreditFromPerson(sender, amount)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Matched! Settled ₹${result.settledAmount.toInt()} for ${result.personName}.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
