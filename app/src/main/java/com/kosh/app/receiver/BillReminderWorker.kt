package com.kosh.app.receiver

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kosh.app.data.local.KoshDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class BillReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = KoshDatabase.getDatabase(applicationContext, CoroutineScope(Dispatchers.IO))
            val obligations = db.householdObligationDao().getAllObligationsList()
            val today = LocalDate.now().dayOfMonth

            for (ob in obligations) {
                if (ob.isPaidThisMonth) continue

                val dueDay = ob.dueDayOfMonth
                val daysDiff = dueDay - today

                // Remind if due today or in the next 2 days
                if (daysDiff in 0..2) {
                    SmsCreditNotificationManager.showBillDueNotification(
                        context = applicationContext,
                        title = ob.title,
                        amount = ob.totalAmount,
                        daysLeft = daysDiff
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "kosh_bill_reminder_periodic_work"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
