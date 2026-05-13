package com.frank.voiceoverlay.dictation

import kotlin.io.path.createTempFile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiClientsTest {
    @Test
    fun transcribe_postsMultipartAudioToAudioTranscriptions() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"text":"hello world"}"""))
        server.start()

        val client = OpenAiTranscriptionClient(
            baseUrl = server.url("/").toString(),
            apiKeyProvider = { "sk-test" },
        )

        val audioFile = createTempFile(suffix = ".m4a").toFile().apply { writeText("fake") }
        assertEquals("hello world", client.transcribe(audioFile))

        val request = server.takeRequest()
        assertEquals("/v1/audio/transcriptions", request.path)
        assertTrue(request.getHeader("Authorization")!!.startsWith("Bearer "))
        assertTrue(request.body.readUtf8().contains("whisper-1"))
        server.shutdown()
    }

    @Test
    fun transcribe_throwsHttpErrorForNon2xxResponse() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"nope"}"""))
        server.start()

        val client = OpenAiTranscriptionClient(
            baseUrl = server.url("/").toString(),
            apiKeyProvider = { "sk-test" },
        )

        val audioFile = createTempFile(suffix = ".m4a").toFile().apply { writeText("fake") }
        val error = assertThrows(OpenAiClientException.HttpError::class.java) {
            runBlocking { client.transcribe(audioFile) }
        }

        assertEquals(401, error.statusCode)
        assertTrue(error.responseBody.contains("nope"))
        server.shutdown()
    }

    @Test
    fun transcribe_throwsEmptyBodyForBlankSuccessResponse() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(""))
        server.start()

        val client = OpenAiTranscriptionClient(
            baseUrl = server.url("/").toString(),
            apiKeyProvider = { "sk-test" },
        )

        val audioFile = createTempFile(suffix = ".m4a").toFile().apply { writeText("fake") }
        assertThrows(OpenAiClientException.EmptyBody::class.java) {
            runBlocking { client.transcribe(audioFile) }
        }
        server.shutdown()
    }

    @Test
    fun cleanup_postsInstructionsAndInputToResponsesApi() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"output_text":"hello"}"""))
        server.start()

        val client = OpenAiCleanupClient(
            baseUrl = server.url("/").toString(),
            apiKeyProvider = { "sk-test" },
        )

        assertEquals("hello", client.clean("uh hello hello"))

        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertEquals("/v1/responses", request.path)
        assertTrue(body.contains("\"model\":\"gpt-5-nano\""))
        assertTrue(body.contains("\"instructions\""))
        assertTrue(body.contains("\"input\":\"uh hello hello\""))
        server.shutdown()
    }

    @Test
    fun cleanup_throwsParseFailureForMalformedJson() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("not-json"))
        server.start()

        val client = OpenAiCleanupClient(
            baseUrl = server.url("/").toString(),
            apiKeyProvider = { "sk-test" },
        )

        val error = assertThrows(OpenAiClientException.ParseFailure::class.java) {
            runBlocking { client.clean("uh hello hello") }
        }

        assertTrue(error.responseBody.contains("not-json"))
        server.shutdown()
    }

    @Test
    fun cleanup_throwsParseFailureWhenOutputTextIsMissing() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"id":"resp_123"}"""))
        server.start()

        val client = OpenAiCleanupClient(
            baseUrl = server.url("/").toString(),
            apiKeyProvider = { "sk-test" },
        )

        val error = assertThrows(OpenAiClientException.ParseFailure::class.java) {
            runBlocking { client.clean("uh hello hello") }
        }

        assertTrue(error.responseBody.contains("resp_123"))
        server.shutdown()
    }
}
