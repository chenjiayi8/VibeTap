package com.frank.voiceoverlay.dictation

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
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

        val audioFile = kotlin.io.path.createTempFile(suffix = ".m4a").toFile().apply { writeText("fake") }
        assertEquals("hello world", client.transcribe(audioFile))

        val request = server.takeRequest()
        assertEquals("/v1/audio/transcriptions", request.path)
        assertTrue(request.getHeader("Authorization")!!.startsWith("Bearer "))
        assertTrue(request.body.readUtf8().contains("whisper-1"))
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
}
