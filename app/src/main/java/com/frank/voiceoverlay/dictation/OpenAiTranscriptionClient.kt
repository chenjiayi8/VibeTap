package com.frank.voiceoverlay.dictation

import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun transcribe(audioFile: File): String = withContext(ioDispatcher) {
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

        executeRequest(request)
    }

    private fun executeRequest(request: Request): String {
        val responseBody = try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw OpenAiClientException.HttpError(
                        operation = "transcription",
                        statusCode = response.code,
                        responseBody = body,
                    )
                }
                if (body.isBlank()) {
                    throw OpenAiClientException.EmptyBody("transcription")
                }
                body
            }
        } catch (exception: OpenAiClientException) {
            throw exception
        } catch (exception: IOException) {
            throw OpenAiClientException.NetworkFailure("transcription", exception)
        }

        try {
            return json.decodeFromString<TranscriptionResponse>(responseBody).text
        } catch (exception: SerializationException) {
            throw OpenAiClientException.ParseFailure("transcription", responseBody, exception)
        } catch (exception: IllegalArgumentException) {
            throw OpenAiClientException.ParseFailure("transcription", responseBody, exception)
        }
    }

    @Serializable
    private data class TranscriptionResponse(
        @SerialName("text") val text: String,
    )
}
