package com.flashidea.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.flashidea.app.work.IncubationScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application 入口。
 *
 * 职责：
 * 1. @HiltAndroidApp 触发 Hilt 依赖图生成。
 * 2. 实现 [Configuration.Provider]，把 [HiltWorkerFactory] 注入 WorkManager，
 *    使 [com.flashidea.app.work.IncubationWorker] 等 @HiltWorker 能正常注入依赖。
 * 3. onCreate 中注册每日全量孵化周期任务（幂等）。
 */
@HiltAndroidApp
class FlashIdeaApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var incubationScheduler: IncubationScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // 启动每日全量孵化周期任务（KEEP 策略，重复启动安全）
        runCatching { incubationScheduler.enqueuePeriodic() }
    }
}
