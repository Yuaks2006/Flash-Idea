package com.flashidea.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "memory_relations")
data class MemoryRelationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val targetId: String,
    val type: String,
    val weight: Double,
    val evidence: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
