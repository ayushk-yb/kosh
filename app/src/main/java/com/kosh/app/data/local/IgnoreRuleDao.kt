package com.kosh.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kosh.app.data.model.IgnoreRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoreRuleDao {
    @Query("SELECT * FROM ignore_rules ORDER BY createdAt DESC")
    fun getAllRulesFlow(): Flow<List<IgnoreRuleEntity>>

    @Query("SELECT * FROM ignore_rules ORDER BY createdAt DESC")
    suspend fun getAllRulesList(): List<IgnoreRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: IgnoreRuleEntity): Long

    @Delete
    suspend fun deleteRule(rule: IgnoreRuleEntity)

    @Query("DELETE FROM ignore_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
