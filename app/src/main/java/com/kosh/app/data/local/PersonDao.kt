package com.kosh.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kosh.app.data.model.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query("SELECT * FROM persons ORDER BY isPinned DESC, name ASC")
    fun getAllPersonsFlow(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE isPinned = 1 ORDER BY name ASC")
    fun getPinnedPersonsFlow(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE isCircleMember = 1 LIMIT 1")
    fun getCircleMemberFlow(): Flow<PersonEntity?>

    @Query("SELECT * FROM persons WHERE isCircleMember = 1 LIMIT 1")
    suspend fun getCircleMember(): PersonEntity?

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getPersonById(id: Long): PersonEntity?

    @Query("SELECT * FROM persons WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getPersonByName(name: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(persons: List<PersonEntity>)

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Query("UPDATE persons SET isPinned = :isPinned WHERE id = :personId")
    suspend fun togglePin(personId: Long, isPinned: Boolean)

    @Query("UPDATE persons SET isCircleMember = 0")
    suspend fun clearCircleMembers()

    @Query("UPDATE persons SET isCircleMember = 1 WHERE id = :personId")
    suspend fun markCircleMember(personId: Long)

    @Transaction
    suspend fun setCircleMember(personId: Long) {
        clearCircleMembers()
        markCircleMember(personId)
    }

    @Query("DELETE FROM persons")
    suspend fun deleteAll()
}
