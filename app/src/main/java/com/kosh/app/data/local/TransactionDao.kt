package com.kosh.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kosh.app.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    fun getTransactionsForMonthFlow(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactionsFlow(limit: Int = 10): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT SUM(grossAmount) FROM transactions WHERE isAssetWealth = 0 AND category != 'INFLOW' AND category != 'CREDIT'")
    fun getTotalGrossOutflowFlow(): Flow<Double?>

    @Query("SELECT SUM(grossAmount) FROM transactions WHERE isAssetWealth = 0 AND category != 'INFLOW' AND category != 'CREDIT' AND timestamp >= :startTime AND timestamp < :endTime")
    fun getGrossOutflowForMonthFlow(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(trueNetAmount) FROM transactions WHERE isAssetWealth = 0 AND category != 'INFLOW' AND category != 'CREDIT'")
    fun getTotalTrueNetExpenseFlow(): Flow<Double?>

    @Query("SELECT SUM(trueNetAmount) FROM transactions WHERE isAssetWealth = 0 AND category != 'INFLOW' AND category != 'CREDIT' AND timestamp >= :startTime AND timestamp < :endTime")
    fun getTrueNetExpenseForMonthFlow(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(grossAmount) FROM transactions WHERE isAssetWealth = 1")
    fun getTotalWealthInvestmentFlow(): Flow<Double?>

    @Query("SELECT SUM(grossAmount) FROM transactions WHERE isAssetWealth = 1 AND timestamp >= :startTime AND timestamp < :endTime")
    fun getWealthInvestmentForMonthFlow(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT smsRefHash FROM transactions WHERE smsRefHash IS NOT NULL AND smsRefHash != ''")
    suspend fun getAllSmsRefHashes(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
