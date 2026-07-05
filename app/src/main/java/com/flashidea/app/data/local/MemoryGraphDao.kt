package com.flashidea.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryGraphDao {
    @Query("SELECT * FROM memory_atoms ORDER BY updatedAt DESC")
    fun getAtoms(): Flow<List<MemoryAtomEntity>>

    @Query("SELECT * FROM memory_relations ORDER BY createdAt DESC")
    fun getRelations(): Flow<List<MemoryRelationEntity>>

    @Query("SELECT * FROM memory_communities ORDER BY updatedAt DESC")
    fun getCommunities(): Flow<List<MemoryCommunityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAtoms(atoms: List<MemoryAtomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRelations(relations: List<MemoryRelationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCommunities(communities: List<MemoryCommunityEntity>)
}
