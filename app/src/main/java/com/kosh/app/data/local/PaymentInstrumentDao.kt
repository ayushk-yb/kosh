package com.kosh.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kosh.app.data.model.PaymentInstrumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentInstrumentDao {
    @Query("SELECT * FROM credit_card_accounts ORDER BY id ASC")
    fun getAllInstrumentsFlow(): Flow<List<PaymentInstrumentEntity>>

    @Query("SELECT * FROM credit_card_accounts WHERE instrumentType = 'CREDIT_CARD'")
    fun getCreditCardsFlow(): Flow<List<PaymentInstrumentEntity>>

    @Query("SELECT * FROM credit_card_accounts WHERE instrumentType != 'CREDIT_CARD'")
    fun getBankAccountsFlow(): Flow<List<PaymentInstrumentEntity>>

    @Query("SELECT * FROM credit_card_accounts WHERE id = :id")
    suspend fun getInstrumentById(id: Long): PaymentInstrumentEntity?

    @Query("SELECT * FROM credit_card_accounts WHERE last4Digits = :last4 LIMIT 1")
    suspend fun getByLast4(last4: String): PaymentInstrumentEntity?

    @Query("UPDATE credit_card_accounts SET unbilledSpend = unbilledSpend + :amount WHERE id = :id")
    suspend fun addSpend(id: Long, amount: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstrument(instrument: PaymentInstrumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(instruments: List<PaymentInstrumentEntity>)

    @Update
    suspend fun updateInstrument(instrument: PaymentInstrumentEntity)

    @Delete
    suspend fun deleteInstrument(instrument: PaymentInstrumentEntity)
}
