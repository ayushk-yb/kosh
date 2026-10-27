package com.kosh.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kosh.app.data.model.SplitDebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDebtDao {
    @Query("SELECT * FROM split_debts ORDER BY isSettled ASC, dateCreated DESC")
    fun getAllSplitDebtsFlow(): Flow<List<SplitDebtEntity>>

    @Query("SELECT * FROM split_debts WHERE isSettled = 0 ORDER BY dateCreated DESC")
    fun getOpenSplitDebtsFlow(): Flow<List<SplitDebtEntity>>

    @Query("SELECT * FROM split_debts WHERE isSettled = 0 AND isMerchantRefund = 0")
    fun getOpenPersonDebtsFlow(): Flow<List<SplitDebtEntity>>

    @Query("SELECT * FROM split_debts WHERE isSettled = 0 AND isMerchantRefund = 1")
    fun getOpenMerchantRefundsFlow(): Flow<List<SplitDebtEntity>>

    @Query("SELECT * FROM split_debts WHERE personName = :personName AND isSettled = 0")
    suspend fun getOpenDebtsForPerson(personName: String): List<SplitDebtEntity>

    @Query("SELECT * FROM split_debts WHERE id = :id LIMIT 1")
    suspend fun getDebtById(id: Long): SplitDebtEntity?

    @Query("SELECT SUM(amount) FROM split_debts WHERE isSettled = 0")
    fun getTotalOpenReceivablesFlow(): Flow<Double?>

    @Query("UPDATE split_debts SET isSettled = 1, dateSettled = :settledDate WHERE id = :id")
    suspend fun settleDebt(id: Long, settledDate: Long = System.currentTimeMillis())

    @Query("UPDATE split_debts SET isSettled = 1, dateSettled = :settledDate WHERE personName = :personName AND isSettled = 0")
    suspend fun settleAllDebtsForPerson(personName: String, settledDate: Long = System.currentTimeMillis())

    @Query("SELECT * FROM split_debts WHERE parentTransactionId = :txId")
    suspend fun getDebtsByParentTransactionId(txId: Long): List<SplitDebtEntity>

    @Query("DELETE FROM split_debts WHERE parentTransactionId = :txId")
    suspend fun deleteDebtsByParentTransactionId(txId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplitDebt(splitDebt: SplitDebtEntity): Long

    @Update
    suspend fun updateSplitDebt(splitDebt: SplitDebtEntity)

    @Delete
    suspend fun deleteSplitDebt(splitDebt: SplitDebtEntity)
}
