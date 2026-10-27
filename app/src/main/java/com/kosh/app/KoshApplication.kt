package com.kosh.app

import android.app.Application
import com.kosh.app.data.local.KoshDatabase
import com.kosh.app.data.repository.KoshRepository
import com.kosh.app.receiver.BillReminderWorker
import com.kosh.app.receiver.SmsCreditNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KoshApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy { KoshDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { KoshRepository(database, this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        SmsCreditNotificationManager.createNotificationChannels(this)
        BillReminderWorker.schedule(this)

        applicationScope.launch(Dispatchers.IO) {
            val partner = database.personLedgerDao().getPersonByName("Ananya")
            if (partner != null && partner.notes.contains("she", ignoreCase = true)) {
                database.personLedgerDao().updateNotes(
                    "Ananya",
                    "Purchased Zara jacket on HDFC CC. Can be treated as a pure gift (₹0 expected back) or adjusted against future mutual expenses."
                )
            }
        }
    }

    companion object {
        lateinit var instance: KoshApplication
            private set
    }
}
