package com.flashidea.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 灵感孵化调度入口。
 *
 * - [enqueueIncremental]：保存灵感后触发，单条增量孵化（与最近 30 条做关联）。
 * - [enqueuePeriodic]：每日全量关联分析，复活休眠灵感。由 [com.flashidea.app.FlashIdeaApp.onCreate] 启动一次。
 *
 * 约束：
 * - 增量孵化要求网络可用（云端模型）；本地规则 fallback 也会因 retry 而最终完成。
 * - 全量孵化要求网络可用 + 电量不低，避免 O(n²) 计算拖垮电量。
 *
 * 用 uniqueWork 避免重复：同 ideaId 的增量不会并发；全量周期任务全局唯一。
 */
@Singleton
class IncubationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    /** 保存灵感后调用：触发该 idea 的增量孵化。 */
    fun enqueueIncremental(ideaId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<IncubationWorker>()
            .setInputData(
                workDataOf(
                    IncubationWorker.KEY_MODE to IncubationWorker.MODE_INCREMENTAL,
                    IncubationWorker.KEY_IDEA_ID to ideaId
                )
            )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "${IncubationWorker.UNIQUE_INCREMENTAL_PREFIX}$ideaId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /** 应用启动时调用：注册每日全量孵化周期任务（幂等，重复调用安全）。 */
    fun enqueuePeriodic() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<IncubationWorker>(
            REPEAT_INTERVAL_HOURS, TimeUnit.HOURS,
            FLEX_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setInputData(workDataOf(IncubationWorker.KEY_MODE to IncubationWorker.MODE_FULL))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            IncubationWorker.UNIQUE_PERIODIC_FULL,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        // 周期：24 小时；flex 窗口 1 小时，让系统在用户闲时合并执行
        private const val REPEAT_INTERVAL_HOURS = 24L
        private const val FLEX_INTERVAL_HOURS = 1L
    }
}
