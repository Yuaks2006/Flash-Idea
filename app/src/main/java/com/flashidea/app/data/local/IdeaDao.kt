package com.flashidea.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas ORDER BY createdAt DESC")
    fun getAllIdeas(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas WHERE id = :id")
    suspend fun getIdeaById(id: String): IdeaEntity?

    @Query("SELECT * FROM ideas WHERE content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchIdeas(query: String): Flow<List<IdeaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(idea: IdeaEntity)

    @Update
    suspend fun update(idea: IdeaEntity)

    @Delete
    suspend fun delete(idea: IdeaEntity)

    @Query("DELETE FROM ideas WHERE id = :id")
    suspend fun deleteById(id: String)
}
