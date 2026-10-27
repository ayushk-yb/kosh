package com.kosh.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kosh.app.data.model.CustomCategoryEntity
import com.kosh.app.data.model.DeletedTransactionHashEntity
import com.kosh.app.data.model.HouseholdObligationEntity
import com.kosh.app.data.model.PaymentInstrumentEntity
import com.kosh.app.data.model.PersonEntity
import com.kosh.app.data.model.PersonLedgerEntity
import com.kosh.app.data.model.SplitDebtEntity
import com.kosh.app.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [
        TransactionEntity::class,
        SplitDebtEntity::class,
        HouseholdObligationEntity::class,
        PaymentInstrumentEntity::class,
        PersonLedgerEntity::class,
        PersonEntity::class,
        DeletedTransactionHashEntity::class,
        CustomCategoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class KoshDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun splitDebtDao(): SplitDebtDao
    abstract fun householdObligationDao(): HouseholdObligationDao
    abstract fun paymentInstrumentDao(): PaymentInstrumentDao
    abstract fun personLedgerDao(): PersonLedgerDao
    abstract fun personDao(): PersonDao
    abstract fun deletedTransactionHashDao(): DeletedTransactionHashDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: KoshDatabase? = null

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN smsRefHash TEXT DEFAULT NULL")
                db.execSQL("CREATE TABLE IF NOT EXISTS deleted_transaction_hashes (hash TEXT PRIMARY KEY NOT NULL, deletedAt INTEGER NOT NULL)")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        iconEmoji TEXT NOT NULL,
                        colorHex INTEGER NOT NULL,
                        isWealthAsset INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): KoshDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KoshDatabase::class.java,
                    "kosh_treasury_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
