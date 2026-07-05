package com.flashidea.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "agent_runs")
data class AgentRunEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goal: String,
    val taskType: String,
    val status: String,
    val providerId: String = "",
    val providerName: String = "",
    val privacyMode: String = "",
    val planJson: String = "[]",
    val toolTraceJson: String = "[]",
    val relatedIdeaIds: String = "[]",
    val finalAnswer: String = "",
    val reflection: String = "",
    val errorMessage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0L,
    val latencyMs: Long = 0L
)
