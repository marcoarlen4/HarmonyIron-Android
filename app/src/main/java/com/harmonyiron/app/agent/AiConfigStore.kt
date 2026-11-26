package com.harmonyiron.app.agent

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class AiConfig(
    val useByok: Boolean = true,
    val baseUrl: String = DEFAULT_BASE_URL,
    val apiKey: String = "",
    val modelName: String = DEFAULT_MODEL
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com/v1"
        const val DEFAULT_MODEL = "deepseek-chat"
        // V1.1 轻量体验网关占位（后续可替换为线上边缘函数地址）
        const val BUILTIN_GATEWAY_URL = "https://gateway.harmonyiron.app/v1"
    }
}

/**
 * 基于 Android Keystore + EncryptedSharedPreferences 安全存储 BYOK 模型配置与 API Key。
 */
class AiConfigStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "harmony_iron_ai_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun load(): AiConfig {
        return AiConfig(
            useByok = prefs.getBoolean(KEY_USE_BYOK, true),
            baseUrl = prefs.getString(KEY_BASE_URL, AiConfig.DEFAULT_BASE_URL) ?: AiConfig.DEFAULT_BASE_URL,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            modelName = prefs.getString(KEY_MODEL_NAME, AiConfig.DEFAULT_MODEL) ?: AiConfig.DEFAULT_MODEL
        )
    }

    fun save(config: AiConfig) {
        prefs.edit()
            .putBoolean(KEY_USE_BYOK, config.useByok)
            .putString(KEY_BASE_URL, config.baseUrl.trim().trimEnd('/'))
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putString(KEY_MODEL_NAME, config.modelName.trim())
            .apply()
    }

    companion object {
        private const val KEY_USE_BYOK = "use_byok"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL_NAME = "model_name"
    }
}
