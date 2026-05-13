package com.frank.voiceoverlay.dictation

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun clean(rawText: String): String = withContext(ioDispatcher) {
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

        executeRequest(request).trim()
    }

    private fun executeRequest(request: Request): String {
        val responseBody = try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw OpenAiClientException.HttpError(
                        operation = "cleanup",
                        statusCode = response.code,
                        responseBody = body,
                    )
                }
                if (body.isBlank()) {
                    throw OpenAiClientException.EmptyBody("cleanup")
                }
                body
            }
        } catch (exception: OpenAiClientException) {
            throw exception
        } catch (exception: IOException) {
            throw OpenAiClientException.NetworkFailure("cleanup", exception)
        }

        try {
            return json.decodeFromString<CleanupResponse>(responseBody).requireOutputText()
        } catch (exception: SerializationException) {
            throw OpenAiClientException.ParseFailure("cleanup", responseBody, exception)
        } catch (exception: IllegalArgumentException) {
            throw OpenAiClientException.ParseFailure("cleanup", responseBody, exception)
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
        @SerialName("output") val output: List<ResponseOutputItem> = emptyList(),
    ) {
        fun requireOutputText(): String {
            val text = output.asSequence()
                .flatMap { it.content.asSequence() }
                .mapNotNull { item ->
                    item.text?.takeIf { item.type == "output_text" && it.isNotBlank() }
                }
                .joinToString(separator = "")

            require(text.isNotBlank()) { "Response did not include output_text content" }
            return text
        }
    }

    @Serializable
    private data class ResponseOutputItem(
        @SerialName("content") val content: List<ResponseContentItem> = emptyList(),
    )

    @Serializable
    private data class ResponseContentItem(
        @SerialName("type") val type: String,
        @SerialName("text") val text: String? = null,
    )

    private companion object {
        const val CLEANUP_INSTRUCTIONS =
            "Clean up dictated text by removing filler words and obvious duplicate stutters while preserving the user's meaning."
    }
}
