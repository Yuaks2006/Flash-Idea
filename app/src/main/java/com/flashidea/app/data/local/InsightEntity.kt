package com.flashidea.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "insights")
data class InsightEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String,
    val content: String,
    val relatedIdeaIds: String = "[]",
    val generatedBy: String = "local_ai",
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
