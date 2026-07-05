package com.flashidea.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        IdeaEntity::class,
        LinkEntity::class,
        InsightEntity::class,
        AgentRunEntity::class,
        MemoryAtomEntity::class,
        MemoryRelationEntity::class,
        MemoryCommunityEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ideaDao(): IdeaDao
    abstract fun linkDao(): LinkDao
    abstract fun insightDao(): InsightDao
    abstract fun agentRunDao(): AgentRunDao
    abstract fun memoryGraphDao(): MemoryGraphDao
}
