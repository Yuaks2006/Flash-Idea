package com.flashidea.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_atoms")
data class MemoryAtomEntity(
    @PrimaryKey val id: String,
    val type: String,
    val label: String,
    val sourceIdeaId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
