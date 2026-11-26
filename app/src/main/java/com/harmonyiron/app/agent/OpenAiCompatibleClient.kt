package com.harmonyiron.app.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 轻量级 OpenAI 兼容接口客户端（支持 DeepSeek / 通义千问 / OpenAI 等兼容 /chat/completions 服务）。
 */
class OpenAiCompatibleClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun chatCompletion(
        baseUrl: String,
        apiKey: String,
        requestPayload: ChatCompletionRequest
    ): Result<ChatCompletionResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "${baseUrl.trimEnd('/')}/chat/completions"
            val bodyString = json.encodeToString(requestPayload)
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(bodyString.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("模型请求失败 (${response.code}): ${rawBody.take(240)}")
                }
                json.decodeFromString<ChatCompletionResponse>(rawBody)
            }
        }
    }
}
