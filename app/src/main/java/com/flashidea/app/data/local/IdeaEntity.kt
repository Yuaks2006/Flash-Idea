package com.flashidea.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "ideas")
data class IdeaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val content: String,
    val category: String = "",
    val summary: String = "",
    val tags: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isCloudAuthorized: Boolean = false
)
