package com.kosh.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kosh.app.data.model.CustomCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM custom_categories ORDER BY id ASC")
    fun getAllCustomCategoriesFlow(): Flow<List<CustomCategoryEntity>>

    @Query("SELECT * FROM custom_categories ORDER BY id ASC")
    suspend fun getAllCustomCategoriesList(): List<CustomCategoryEntity>

    @Query("SELECT * FROM custom_categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CustomCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CustomCategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CustomCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CustomCategoryEntity)
}
