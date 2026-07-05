package com.flashidea.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_communities")
data class MemoryCommunityEntity(
    @PrimaryKey val id: String,
    val label: String,
    val ideaIds: String,
    val summary: String,
    val updatedAt: Long = System.currentTimeMillis()
)
