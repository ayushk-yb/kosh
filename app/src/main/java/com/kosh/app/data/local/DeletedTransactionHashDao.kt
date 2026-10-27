package com.kosh.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kosh.app.data.model.DeletedTransactionHashEntity

@Dao
interface DeletedTransactionHashDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedHash(entity: DeletedTransactionHashEntity)

    @Query("SELECT hash FROM deleted_transaction_hashes")
    suspend fun getAllDeletedHashes(): List<String>

    @Query("SELECT COUNT(*) FROM deleted_transaction_hashes WHERE hash = :hash")
    suspend fun isHashDeleted(hash: String): Boolean
}
