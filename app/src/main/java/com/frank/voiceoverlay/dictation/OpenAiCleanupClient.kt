package com.frank.voiceoverlay.dictation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAiCleanupClient(
    private val baseUrl: String,
    private val apiKeyProvider: () -> String,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun clean(rawText: String): String {
        val requestBody = json.encodeToString(
            CleanupRequest(
                model = "gpt-5-nano",
                instructions = CLEANUP_INSTRUCTIONS,
                input = rawText,
            ),
        ).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/v1/responses")
            .header("Authorization", "Bearer ${apiKeyProvider()}")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val body = requireNotNull(response.body) { "Missing response body" }.string()
            check(response.isSuccessful) { "OpenAI cleanup failed: ${response.code} $body" }
            json.decodeFromString<CleanupResponse>(body).outputText.trim()
        }
    }

    @Serializable
    private data class CleanupRequest(
        @SerialName("model") val model: String,
        @SerialName("instructions") val instructions: String,
        @SerialName("input") val input: String,
    )

    @Serializable
    private data class CleanupResponse(
        @SerialName("output_text") val outputText: String,
    )

    private companion object {
        const val CLEANUP_INSTRUCTIONS =
            "Clean up dictated text by removing filler words and obvious duplicate stutters while preserving the user's meaning."
    }
}
