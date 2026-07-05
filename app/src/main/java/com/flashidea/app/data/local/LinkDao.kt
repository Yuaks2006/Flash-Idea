package com.flashidea.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    @Query("SELECT * FROM links")
    fun getAllLinks(): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE sourceId = :ideaId OR targetId = :ideaId")
    fun getLinksForIdea(ideaId: String): Flow<List<LinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: LinkEntity)

    @Query("DELETE FROM links WHERE sourceId = :ideaId OR targetId = :ideaId")
    suspend fun deleteLinksForIdea(ideaId: String)
}
