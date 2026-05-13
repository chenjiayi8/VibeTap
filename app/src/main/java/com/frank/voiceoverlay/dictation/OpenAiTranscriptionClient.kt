package com.frank.voiceoverlay.dictation

import java.io.File
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

class OpenAiTranscriptionClient(
    private val baseUrl: String,
    private val apiKeyProvider: () -> String,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun transcribe(audioFile: File): String {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", "whisper-1")
            .addFormDataPart(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/m4a".toMediaType()),
            )
            .build()

        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/v1/audio/transcriptions")
            .header("Authorization", "Bearer ${apiKeyProvider()}")
            .post(requestBody)
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val body = requireNotNull(response.body) { "Missing response body" }.string()
            check(response.isSuccessful) { "OpenAI transcription failed: ${response.code} $body" }
            json.decodeFromString<TranscriptionResponse>(body).text
        }
    }

    @Serializable
    private data class TranscriptionResponse(
        @SerialName("text") val text: String,
    )
}
