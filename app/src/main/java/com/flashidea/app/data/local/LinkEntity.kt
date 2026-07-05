package com.flashidea.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "links")
data class LinkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val targetId: String,
    val strength: Float = 0.5f,
    val createdBy: String = "local_ai",
    val createdAt: Long = System.currentTimeMillis()
)
