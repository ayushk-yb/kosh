package com.kosh.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kosh.app.data.model.PersonLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonLedgerDao {
    @Query("SELECT * FROM person_ledgers ORDER BY id ASC")
    fun getAllPersonLedgersFlow(): Flow<List<PersonLedgerEntity>>

    @Query("SELECT * FROM person_ledgers WHERE relationshipType = 'PARTNER' LIMIT 1")
    fun getPartnerLedgerFlow(): Flow<PersonLedgerEntity?>

    @Query("SELECT * FROM person_ledgers WHERE name = :name LIMIT 1")
    suspend fun getPersonByName(name: String): PersonLedgerEntity?

    @Query("UPDATE person_ledgers SET runningBalance = :newBalance WHERE name = :name")
    suspend fun updateBalance(name: String, newBalance: Double)

    @Query("UPDATE person_ledgers SET notes = :notes WHERE name = :name")
    suspend fun updateNotes(name: String, notes: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonLedgerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(persons: List<PersonLedgerEntity>)

    @Update
    suspend fun updatePerson(person: PersonLedgerEntity)

    @Delete
    suspend fun deletePerson(person: PersonLedgerEntity)
}
