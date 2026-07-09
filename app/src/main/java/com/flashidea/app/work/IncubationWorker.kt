package com.flashidea.app.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.flashidea.app.R
import com.flashidea.app.ai.IdeaProcessor
import com.flashidea.app.data.local.InsightEntity
import com.flashidea.app.data.local.LinkEntity
import com.flashidea.app.data.repository.IdeaRepository
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * 后台灵感孵化 Worker。
 *
 * 两种模式（由 inputData[KEY_MODE] 决定）：
 * - MODE_INCREMENTAL：单条增量孵化。读取 [KEY_IDEA_ID] 对应灵感，调
 *   [IdeaProcessor.processWithLinks] 与最近 30 条灵感做关联分析，结果写入 Link/Insight。
 * - MODE_FULL：全量关联分析。调 [IdeaProcessor.analyzeAllLinks] 对全库做 O(n²) 配对，
 *   生成新 Link/Insight。建议仅由 [IncubationScheduler.enqueuePeriodic] 每日触发。
 *
 * 失败策略：网络/解析类异常 → Result.retry()（受 backoff 约束）；致命异常 → Result.failure()。
 */
@HiltWorker
class IncubationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val ideaProcessor: IdeaProcessor,
    private val repository: IdeaRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val mode = inputData.getString(KEY_MODE) ?: MODE_INCREMENTAL
            when (mode) {
                MODE_INCREMENTAL -> doIncremental()
                MODE_FULL -> doFull()
                else -> doIncremental()
            }
        }.getOrElse { e ->
            // 网络或解析异常 → 重试；重试次数达上限后由 WorkManager 自动放弃
            if (runAttemptCount < MAX_RETRY) Result.retry()
            else Result.failure(workDataOf(KEY_ERROR to (e.message ?: "unknown")))
        }
    }

    private suspend fun doIncremental(): Result {
        val ideaId = inputData.getString(KEY_IDEA_ID)
            ?: return Result.failure(workDataOf(KEY_ERROR to "missing ideaId"))

        val idea = repository.getIdeaById(ideaId)
            ?: return Result.failure(workDataOf(KEY_ERROR to "idea not found: $ideaId"))

        val allIdeas = repository.getAllIdeas().first()
        val candidates = allIdeas
            .filter { it.id != idea.id }
            .sortedByDescending { it.updatedAt }
            .take(30)

        val result = ideaProcessor.processWithLinks(idea, candidates)

        // 写 LinkEntity（去重）
        val validIds = allIdeas.map { it.id }.toSet()
        val seen = mutableSetOf<String>()
        result.links
            .filter { it.targetId in validIds }
            .forEach { suggestion ->
                val pairKey = "${minOf(idea.id, suggestion.targetId)}_${maxOf(idea.id, suggestion.targetId)}"
                if (seen.add(pairKey)) {
                    repository.saveLink(
                        LinkEntity(
                            sourceId = idea.id,
                            targetId = suggestion.targetId,
                            strength = suggestion.strength,
                            createdBy = "background_incubation"
                        )
                    )
                }
            }

        // 写 InsightEntity（仅当存在强关联时）
        val hasStrongLink = result.links.any { it.strength >= 0.7f }
        if (hasStrongLink && result.insight != null) {
            repository.saveInsight(
                InsightEntity(
                    type = "hidden_link",
                    content = result.insight.content,
                    relatedIdeaIds = Gson().toJson(result.insight.relatedIds),
                    generatedBy = "background_incubation",
                    isRead = false
                )
            )
        }

        return Result.success()
    }

    private suspend fun doFull(): Result {
        val allIdeas = repository.getAllIdeas().first()
        if (allIdeas.size < 2) return Result.success()

        val (pairs, insights) = ideaProcessor.analyzeAllLinks(allIdeas)

        val validIds = allIdeas.map { it.id }.toSet()
        val seen = mutableSetOf<String>()
        pairs
            .filter { it.a in validIds && it.b in validIds }
            .forEach { p ->
                val pairKey = "${minOf(p.a, p.b)}_${maxOf(p.a, p.b)}"
                if (seen.add(pairKey)) {
                    repository.saveLink(
                        LinkEntity(
                            sourceId = p.a,
                            targetId = p.b,
                            strength = p.strength,
                            createdBy = "background_incubation_full"
                        )
                    )
                }
            }

        insights.forEach { ins ->
            repository.saveInsight(
                InsightEntity(
                    type = "hidden_link",
                    content = ins.content,
                    relatedIdeaIds = Gson().toJson(ins.relatedIds),
                    generatedBy = "background_incubation_full",
                    isRead = false
                )
            )
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            buildNotification("Flash Idea 正在孵化灵感…")
        )
    }

    private fun buildNotification(text: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "灵感孵化",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "后台 AI 孵化进度通知" }
            manager?.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Flash Idea")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val KEY_MODE = "mode"
        const val KEY_IDEA_ID = "idea_id"
        const val KEY_ERROR = "error"
        const val MODE_INCREMENTAL = "incremental"
        const val MODE_FULL = "full"

        const val UNIQUE_INCREMENTAL_PREFIX = "incubation_incremental_"
        const val UNIQUE_PERIODIC_FULL = "incubation_periodic_full"

        private const val NOTIFICATION_ID = 4242
        private const val NOTIFICATION_CHANNEL_ID = "flash_idea_incubation"
        private const val MAX_RETRY = 3
    }
}
