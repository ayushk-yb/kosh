package com.kosh.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kosh.app.data.model.HouseholdObligationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdObligationDao {
    @Query("SELECT * FROM household_obligations ORDER BY dueDayOfMonth ASC")
    fun getAllObligationsFlow(): Flow<List<HouseholdObligationEntity>>

    @Query("SELECT * FROM household_obligations")
    suspend fun getAllObligationsList(): List<HouseholdObligationEntity>

    @Query("SELECT * FROM household_obligations WHERE id = :id")
    suspend fun getObligationById(id: Long): HouseholdObligationEntity?

    @Query("UPDATE household_obligations SET isPaidThisMonth = :isPaid, lastPaidDate = :paidDate, paidTransactionId = :paidTxId WHERE id = :id")
    suspend fun updatePaidDetails(id: Long, isPaid: Boolean, paidDate: Long?, paidTxId: Long?)

    @Query("UPDATE household_obligations SET isPaidThisMonth = 0, lastPaidDate = NULL, paidTransactionId = NULL WHERE id = :id")
    suspend fun resetPaymentStatus(id: Long)

    @Query("UPDATE household_obligations SET isPaidThisMonth = :isPaid, lastPaidDate = :paidDate WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, isPaid: Boolean, paidDate: Long? = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObligation(obligation: HouseholdObligationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(obligations: List<HouseholdObligationEntity>)

    @Update
    suspend fun updateObligation(obligation: HouseholdObligationEntity)

    @Delete
    suspend fun deleteObligation(obligation: HouseholdObligationEntity)
}
