package com.flashidea.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {
    @Query("SELECT * FROM insights ORDER BY createdAt DESC")
    fun getAllInsights(): Flow<List<InsightEntity>>

    @Query("SELECT COUNT(*) > 0 FROM insights WHERE isRead = 0")
    fun hasUnreadInsights(): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: InsightEntity)

    @Query("UPDATE insights SET isRead = 1")
    suspend fun markAllRead()
}
