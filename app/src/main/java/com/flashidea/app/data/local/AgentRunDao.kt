package com.flashidea.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentRunDao {
    @Query("SELECT * FROM agent_runs ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentRuns(limit: Int = 30): Flow<List<AgentRunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(run: AgentRunEntity)
}
