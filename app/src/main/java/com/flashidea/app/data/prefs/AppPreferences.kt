package com.flashidea.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用级偏好设置（基于 DataStore）。
 *
 * 当前持有：
 * - [autoIncubate]：灵感保存后是否自动触发后台 AI 孵化（默认 true）。
 *
 * 后续可扩展：每日全量孵化开关、孵化网络约束（Wi-Fi only）等。
 */
private val Context.appPrefsDataStore by preferencesDataStore(name = "flash_idea_app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyAutoIncubate = booleanPreferencesKey("auto_incubate")

    /** 自动孵化开关流，默认开启。 */
    val autoIncubate: Flow<Boolean> = context.appPrefsDataStore.data
        .map { it[keyAutoIncubate] ?: DEFAULT_AUTO_INCUBATE }

    /** 一次性读取当前开关值（suspend）。 */
    suspend fun isAutoIncubateEnabled(): Boolean =
        autoIncubate.first()

    suspend fun setAutoIncubate(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[keyAutoIncubate] = enabled }
    }

    companion object {
        const val DEFAULT_AUTO_INCUBATE = true
    }
}
