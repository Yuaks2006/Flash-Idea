package com.flashidea.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flashidea.app.BuildConfig
import com.flashidea.app.ai.VivoRequestInterceptor
import com.flashidea.app.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "flash_idea_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides fun provideIdeaDao(db: AppDatabase): IdeaDao = db.ideaDao()
    @Provides fun provideLinkDao(db: AppDatabase): LinkDao = db.linkDao()
    @Provides fun provideInsightDao(db: AppDatabase): InsightDao = db.insightDao()
    @Provides fun provideAgentRunDao(db: AppDatabase): AgentRunDao = db.agentRunDao()
    @Provides fun provideMemoryGraphDao(db: AppDatabase): MemoryGraphDao = db.memoryGraphDao()

    @Provides @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.AI_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    VivoRequestInterceptor(
                        appKey = BuildConfig.AI_API_KEY,
                        appId = BuildConfig.VIVO_APP_ID
                    )
                )
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BASIC
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
                    }
                )
                .build()
        )
        .build()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `agent_runs` (
                    `id` TEXT NOT NULL,
                    `goal` TEXT NOT NULL,
                    `taskType` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `providerId` TEXT NOT NULL,
                    `providerName` TEXT NOT NULL,
                    `privacyMode` TEXT NOT NULL,
                    `planJson` TEXT NOT NULL,
                    `toolTraceJson` TEXT NOT NULL,
                    `relatedIdeaIds` TEXT NOT NULL,
                    `finalAnswer` TEXT NOT NULL,
                    `reflection` TEXT NOT NULL,
                    `errorMessage` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `completedAt` INTEGER NOT NULL,
                    `latencyMs` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `memory_atoms` (
                    `id` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `label` TEXT NOT NULL,
                    `sourceIdeaId` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `memory_relations` (
                    `id` TEXT NOT NULL,
                    `sourceId` TEXT NOT NULL,
                    `targetId` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `weight` REAL NOT NULL,
                    `evidence` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `memory_communities` (
                    `id` TEXT NOT NULL,
                    `label` TEXT NOT NULL,
                    `ideaIds` TEXT NOT NULL,
                    `summary` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }
}
