# VibeTap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build VibeTap, an Android overlay app that records speech from a floating bubble, transcribes it with OpenAI audio transcription, cleans the transcript with `gpt-5-nano`, and inserts the result into the focused field through Accessibility, with double-tap radial shortcuts for preset phrases.

**Architecture:** Treat this as a greenfield Android app rooted at `VibeTap/`. Use a single settings activity plus feature-focused packages for `settings`, `dictation`, `overlay`, `insertion`, and `shortcuts`. Call OpenAI directly from the device with OkHttp: `/v1/audio/transcriptions` for speech-to-text and `/v1/responses` for cleanup. Keep the overlay service, accessibility service, and AI transport separated behind a `DictationCoordinator` so Android-specific concerns do not leak into UI state.

**Tech Stack:** Kotlin, Android SDK, Jetpack Compose, Coroutines/StateFlow, Android DataStore, OkHttp, kotlinx.serialization, JUnit4, MockWebServer, Robolectric, AndroidX instrumentation tests

---

## Repository Assumption

This app does not exist yet. Execute this plan in a new repository rooted at:

- `VibeTap/`

Save docs in:

- `.codex/docs/plans/`

The file paths below are relative to `VibeTap/`.

## File Structure

### Create

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/frank/voiceoverlay/MainActivity.kt`
- `app/src/main/java/com/frank/voiceoverlay/settings/AppSettings.kt`
- `app/src/main/java/com/frank/voiceoverlay/settings/SettingsStore.kt`
- `app/src/main/java/com/frank/voiceoverlay/settings/SettingsViewModel.kt`
- `app/src/main/java/com/frank/voiceoverlay/settings/SettingsScreen.kt`
- `app/src/main/java/com/frank/voiceoverlay/shortcuts/ShortcutPreset.kt`
- `app/src/main/java/com/frank/voiceoverlay/shortcuts/ShortcutPresetRepository.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/RecordingState.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/AudioRecorder.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/AndroidAudioRecorder.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/OpenAiTranscriptionClient.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/OpenAiCleanupClient.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/DictationCoordinator.kt`
- `app/src/main/java/com/frank/voiceoverlay/insertion/FocusedFieldInserter.kt`
- `app/src/main/java/com/frank/voiceoverlay/insertion/OverlayAccessibilityService.kt`
- `app/src/main/java/com/frank/voiceoverlay/overlay/BubbleGestureInterpreter.kt`
- `app/src/main/java/com/frank/voiceoverlay/overlay/BubbleUiState.kt`
- `app/src/main/java/com/frank/voiceoverlay/overlay/OverlayBubbleService.kt`
- `app/src/main/java/com/frank/voiceoverlay/overlay/OverlayBubbleView.kt`
- `app/src/main/java/com/frank/voiceoverlay/overlay/RadialShortcutMenu.kt`
- `app/src/main/java/com/frank/voiceoverlay/permissions/PermissionGate.kt`
- `app/src/main/res/xml/accessibility_service_config.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/drawable/ic_bubble.xml`
- `app/src/test/java/com/frank/voiceoverlay/shortcuts/ShortcutPresetRepositoryTest.kt`
- `app/src/test/java/com/frank/voiceoverlay/settings/SettingsStoreTest.kt`
- `app/src/test/java/com/frank/voiceoverlay/dictation/OpenAiClientsTest.kt`
- `app/src/test/java/com/frank/voiceoverlay/dictation/DictationCoordinatorTest.kt`
- `app/src/test/java/com/frank/voiceoverlay/overlay/BubbleGestureInterpreterTest.kt`
- `app/src/test/java/com/frank/voiceoverlay/insertion/FocusedFieldInserterTest.kt`
- `app/src/androidTest/java/com/frank/voiceoverlay/settings/SettingsScreenTest.kt`
- `app/src/androidTest/java/com/frank/voiceoverlay/overlay/OverlayBubbleSmokeTest.kt`
- `README.md`

### Modify

- `.codex/docs/plans/2026-05-13-vibetap-design.md`
  Add a short “Implemented by plan” backlink after plan approval if desired during execution, not during planning.

## Task 1: Bootstrap the Android project and lock the domain vocabulary

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/frank/voiceoverlay/MainActivity.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/shortcuts/ShortcutPreset.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/dictation/RecordingState.kt`
- Test: `app/src/test/java/com/frank/voiceoverlay/shortcuts/ShortcutPresetRepositoryTest.kt`

- [ ] **Step 1: Write the failing shortcut-domain test**

```kotlin
package com.frank.voiceoverlay.shortcuts

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutPresetRepositoryTest {
    @Test
    fun defaultPresets_includeShipPrPhrase() {
        val presets = ShortcutPreset.defaultPresets()
        assertEquals("Good, please proceed to use \$ship-pr", presets.first { it.id == "ship-pr" }.text)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.shortcuts.ShortcutPresetRepositoryTest"`

Expected: FAIL because the Android app module and `ShortcutPreset` model do not exist yet.

- [ ] **Step 3: Create the baseline project files and minimal domain types**

```kotlin
// app/src/main/java/com/frank/voiceoverlay/shortcuts/ShortcutPreset.kt
package com.frank.voiceoverlay.shortcuts

import kotlinx.serialization.Serializable

@Serializable
data class ShortcutPreset(
    val id: String,
    val label: String,
    val text: String,
    val order: Int,
) {
    companion object {
        fun defaultPresets(): List<ShortcutPreset> = listOf(
            ShortcutPreset("ship-pr", "Ship-PR", "Good, please proceed to use \$ship-pr", 0),
            ShortcutPreset("review", "Review", "Please review the latest changes carefully.", 1),
            ShortcutPreset("proceed", "Proceed", "Good, please proceed.", 2),
        )
    }
}

// app/src/main/java/com/frank/voiceoverlay/dictation/RecordingState.kt
package com.frank.voiceoverlay.dictation

enum class RecordingState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR,
}
```

```kotlin
// app/src/main/java/com/frank/voiceoverlay/MainActivity.kt
package com.frank.voiceoverlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("Voice Overlay Settings") }
    }
}
```

- [ ] **Step 4: Run the unit test and a baseline assemble**

Run: `./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.shortcuts.ShortcutPresetRepositoryTest" :app:assembleDebug`

Expected: PASS for the unit test and `BUILD SUCCESSFUL` for the debug assemble.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/com/frank/voiceoverlay/MainActivity.kt app/src/main/java/com/frank/voiceoverlay/shortcuts/ShortcutPreset.kt app/src/main/java/com/frank/voiceoverlay/dictation/RecordingState.kt app/src/test/java/com/frank/voiceoverlay/shortcuts/ShortcutPresetRepositoryTest.kt
git commit -m $'Create the baseline Android shell for overlay dictation

Constraint: Greenfield Android app with overlay-only MVP scope
Rejected: Full IME-first architecture | too broad for the first release
Confidence: high
Scope-risk: narrow
Directive: Keep package boundaries feature-first and avoid unnecessary frameworks
Tested: ./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.shortcuts.ShortcutPresetRepositoryTest" :app:assembleDebug
Not-tested: device-only overlay and accessibility behavior'
```

## Task 2: Persist API key, overlay setting, and shortcut presets

**Files:**
- Create: `app/src/main/java/com/frank/voiceoverlay/settings/AppSettings.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/settings/SettingsStore.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/shortcuts/ShortcutPresetRepository.kt`
- Test: `app/src/test/java/com/frank/voiceoverlay/settings/SettingsStoreTest.kt`

- [ ] **Step 1: Write the failing settings persistence test**

```kotlin
package com.frank.voiceoverlay.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun saveAndRead_roundTripsApiKeyAndPresets() = runTest {
        val store: DataStore<androidx.datastore.preferences.core.Preferences> =
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { temporaryFolder.newFile("settings.preferences_pb") },
            )

        val settingsStore = SettingsStore(store)
        val input = AppSettings(
            openAiApiKey = "sk-test",
            overlayEnabled = true,
            presets = listOf(ShortcutPreset("ship-pr", "Ship-PR", "Good, please proceed to use \$ship-pr", 0)),
        )

        settingsStore.save(input)

        assertEquals(input, settingsStore.readOnce())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.settings.SettingsStoreTest"`

Expected: FAIL because `AppSettings` and `SettingsStore` do not exist.

- [ ] **Step 3: Implement the settings data model and repository**

```kotlin
// app/src/main/java/com/frank/voiceoverlay/settings/AppSettings.kt
package com.frank.voiceoverlay.settings

import com.frank.voiceoverlay.shortcuts.ShortcutPreset

data class AppSettings(
    val openAiApiKey: String = "",
    val overlayEnabled: Boolean = false,
    val presets: List<ShortcutPreset> = ShortcutPreset.defaultPresets(),
)
```

```kotlin
// app/src/main/java/com/frank/voiceoverlay/settings/SettingsStore.kt
package com.frank.voiceoverlay.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json,
) {
    private val apiKeyKey = stringPreferencesKey("openai_api_key")
    private val overlayEnabledKey = booleanPreferencesKey("overlay_enabled")
    private val presetsKey = stringPreferencesKey("shortcut_presets")

    suspend fun save(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[apiKeyKey] = settings.openAiApiKey
            prefs[overlayEnabledKey] = settings.overlayEnabled
            prefs[presetsKey] = json.encodeToString(settings.presets)
        }
    }

    suspend fun readOnce(): AppSettings {
        val prefs = dataStore.data.first()
        return AppSettings(
            openAiApiKey = prefs[apiKeyKey].orEmpty(),
            overlayEnabled = prefs[overlayEnabledKey] ?: false,
            presets = prefs[presetsKey]
                ?.let { json.decodeFromString<List<ShortcutPreset>>(it) }
                ?: ShortcutPreset.defaultPresets(),
        )
    }
}
```

```kotlin
// app/src/main/java/com/frank/voiceoverlay/shortcuts/ShortcutPresetRepository.kt
package com.frank.voiceoverlay.shortcuts

import com.frank.voiceoverlay.settings.SettingsStore

class ShortcutPresetRepository(
    private val settingsStore: SettingsStore,
) {
    suspend fun list(): List<ShortcutPreset> = settingsStore.readOnce().presets.sortedBy { it.order }

    suspend fun save(presets: List<ShortcutPreset>) {
        val current = settingsStore.readOnce()
        settingsStore.save(current.copy(presets = presets.sortedBy { it.order }))
    }
}
```

- [ ] **Step 4: Run the settings test**

Run: `./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.settings.SettingsStoreTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/frank/voiceoverlay/settings/AppSettings.kt app/src/main/java/com/frank/voiceoverlay/settings/SettingsStore.kt app/src/main/java/com/frank/voiceoverlay/shortcuts/ShortcutPresetRepository.kt app/src/test/java/com/frank/voiceoverlay/settings/SettingsStoreTest.kt
git commit -m $'Persist local settings required for voice overlay operation

Constraint: Personal-use MVP stores API key and presets only on device
Rejected: Backend-backed settings sync | out of scope for MVP
Confidence: high
Scope-risk: narrow
Directive: Keep persisted schema minimal and backward-compatible
Tested: ./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.settings.SettingsStoreTest"
Not-tested: migration behavior across future schema versions'
```

## Task 3: Add OpenAI transcription and cleanup clients

**Files:**
- Create: `app/src/main/java/com/frank/voiceoverlay/dictation/OpenAiTranscriptionClient.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/dictation/OpenAiCleanupClient.kt`
- Test: `app/src/test/java/com/frank/voiceoverlay/dictation/OpenAiClientsTest.kt`

- [ ] **Step 1: Write the failing OpenAI client tests**

```kotlin
package com.frank.voiceoverlay.dictation

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.dictation.OpenAiClientsTest"`

Expected: FAIL because the clients do not exist.

- [ ] **Step 3: Implement the minimal HTTP clients**

```kotlin
// app/src/main/java/com/frank/voiceoverlay/dictation/OpenAiTranscriptionClient.kt
package com.frank.voiceoverlay.dictation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class OpenAiTranscriptionClient(
    private val baseUrl: String = "https://api.openai.com/",
    private val apiKeyProvider: suspend () -> String,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun transcribe(audioFile: File): String {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/v1/audio/transcriptions")
            .header("Authorization", "Bearer ${apiKeyProvider()}")
            .post(
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-1")
                    .addFormDataPart("file", audioFile.name, audioFile.asRequestBody("audio/m4a".toMediaType()))
                    .build()
            )
            .build()

        httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Transcription failed: ${response.code}" }
            return json.decodeFromString<TranscriptionResponse>(response.body!!.string()).text
        }
    }
}

@Serializable
private data class TranscriptionResponse(val text: String)
```

```kotlin
// app/src/main/java/com/frank/voiceoverlay/dictation/OpenAiCleanupClient.kt
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
    private val baseUrl: String = "https://api.openai.com/",
    private val apiKeyProvider: suspend () -> String,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun clean(rawText: String): String {
        val body = CleanupRequest(
            model = "gpt-5-nano",
            instructions = "Remove duplicate fragments, breath/noise artifacts, and obvious speech disfluencies. Preserve meaning, tone, and wording whenever possible. Do not summarize or paraphrase.",
            input = rawText,
        )

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/v1/responses")
            .header("Authorization", "Bearer ${apiKeyProvider()}")
            .header("Content-Type", "application/json")
            .post(json.encodeToString(body).toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Cleanup failed: ${response.code}" }
            return json.decodeFromString<CleanupResponse>(response.body!!.string()).outputText.trim()
        }
    }
}

@Serializable
private data class CleanupRequest(
    val model: String,
    val instructions: String,
    val input: String,
)

@Serializable
private data class CleanupResponse(
    @SerialName("output_text") val outputText: String,
)
```

- [ ] **Step 4: Run the OpenAI client tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.dictation.OpenAiClientsTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/frank/voiceoverlay/dictation/OpenAiTranscriptionClient.kt app/src/main/java/com/frank/voiceoverlay/dictation/OpenAiCleanupClient.kt app/src/test/java/com/frank/voiceoverlay/dictation/OpenAiClientsTest.kt
git commit -m $'Add direct OpenAI transport for transcription and cleanup

Constraint: Current API plan uses /v1/audio/transcriptions and /v1/responses
Rejected: Server proxy in MVP | personal-use device-only release
Confidence: medium
Scope-risk: moderate
Directive: Keep prompts narrow and avoid hidden content rewriting
Tested: ./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.dictation.OpenAiClientsTest"
Not-tested: real network calls against production API'
```

## Task 4: Build the recording and processing state machine

**Files:**
- Create: `app/src/main/java/com/frank/voiceoverlay/dictation/AudioRecorder.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/dictation/AndroidAudioRecorder.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/dictation/DictationCoordinator.kt`
- Test: `app/src/test/java/com/frank/voiceoverlay/dictation/DictationCoordinatorTest.kt`

- [ ] **Step 1: Write the failing coordinator tests**

```kotlin
package com.frank.voiceoverlay.dictation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DictationCoordinatorTest {
    @Test
    fun stopRecording_runsTranscriptionCleanupAndInsertion() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val inserted = mutableListOf<String>()
        val coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribe = { "um hello hello" },
            clean = { "hello" },
            insertText = { inserted += it },
            deleteFile = { true },
        )

        coordinator.startRecording()
        coordinator.stopRecording()

        assertEquals(RecordingState.IDLE, coordinator.state.value)
        assertEquals(listOf("hello"), inserted)
    }
}

private class FakeAudioRecorder : AudioRecorder {
    override suspend fun start(): Unit = Unit
    override suspend fun stop(): File = kotlin.io.path.createTempFile(suffix = ".m4a").toFile()
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.dictation.DictationCoordinatorTest"`

Expected: FAIL because `AudioRecorder` and `DictationCoordinator` do not exist.

- [ ] **Step 3: Implement the recording abstraction and coordinator**

```kotlin
// app/src/main/java/com/frank/voiceoverlay/dictation/AudioRecorder.kt
package com.frank.voiceoverlay.dictation

import java.io.File

interface AudioRecorder {
    suspend fun start()
    suspend fun stop(): File
}
```

```kotlin
// app/src/main/java/com/frank/voiceoverlay/dictation/DictationCoordinator.kt
package com.frank.voiceoverlay.dictation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class DictationCoordinator(
    private val recorder: AudioRecorder,
    private val transcribe: suspend (File) -> String,
    private val clean: suspend (String) -> String,
    private val insertText: suspend (String) -> Unit,
    private val deleteFile: (File) -> Boolean = File::delete,
) {
    private val mutableState = MutableStateFlow(RecordingState.IDLE)
    val state: StateFlow<RecordingState> = mutableState

    suspend fun startRecording() {
        check(mutableState.value == RecordingState.IDLE) { "Recorder is busy" }
        recorder.start()
        mutableState.value = RecordingState.LISTENING
    }

    suspend fun stopRecording() {
        check(mutableState.value == RecordingState.LISTENING) { "Recorder is not listening" }
        mutableState.value = RecordingState.PROCESSING
        val audioFile = recorder.stop()
        try {
            val transcript = transcribe(audioFile)
            val cleaned = clean(transcript)
            insertText(cleaned)
            mutableState.value = RecordingState.IDLE
        } catch (t: Throwable) {
            mutableState.value = RecordingState.ERROR
            throw t
        } finally {
            deleteFile(audioFile)
        }
    }
}
```

```kotlin
// app/src/main/java/com/frank/voiceoverlay/dictation/AndroidAudioRecorder.kt
package com.frank.voiceoverlay.dictation

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class AndroidAudioRecorder(
    private val context: Context,
) : AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    override suspend fun start() {
        val file = File.createTempFile("voice-overlay-", ".m4a", context.cacheDir)
        outputFile = file
        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
    }

    override suspend fun stop(): File {
        val finishedFile = checkNotNull(outputFile)
        recorder?.stop()
        recorder?.release()
        recorder = null
        outputFile = null
        return finishedFile
    }
}
```

- [ ] **Step 4: Run the coordinator tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.dictation.DictationCoordinatorTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/frank/voiceoverlay/dictation/AudioRecorder.kt app/src/main/java/com/frank/voiceoverlay/dictation/AndroidAudioRecorder.kt app/src/main/java/com/frank/voiceoverlay/dictation/DictationCoordinator.kt app/src/test/java/com/frank/voiceoverlay/dictation/DictationCoordinatorTest.kt
git commit -m $'Create the dictation coordinator that owns record-process-insert flow

Constraint: Only one recording session may exist at a time
Rejected: UI-owned recording logic | too coupled to overlay rendering
Confidence: high
Scope-risk: moderate
Directive: Keep cleanup and insertion as injected boundaries for testability
Tested: ./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.dictation.DictationCoordinatorTest"
Not-tested: MediaRecorder behavior on physical devices'
```

## Task 5: Implement focused-field insertion and accessibility service wiring

**Files:**
- Create: `app/src/main/java/com/frank/voiceoverlay/insertion/FocusedFieldInserter.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/insertion/OverlayAccessibilityService.kt`
- Create: `app/src/main/res/xml/accessibility_service_config.xml`
- Test: `app/src/test/java/com/frank/voiceoverlay/insertion/FocusedFieldInserterTest.kt`

- [ ] **Step 1: Write the failing insertion-policy tests**

```kotlin
package com.frank.voiceoverlay.insertion

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusedFieldInserterTest {
    @Test
    fun mergeText_appendsAtCursorWhenNoSelection() {
        val result = FocusedFieldInserter.mergeText(
            existing = "Hello",
            insertion = " world",
            selectionStart = 5,
            selectionEnd = 5,
        )
        assertEquals("Hello world", result)
    }

    @Test
    fun mergeText_replacesSelectedRangeWhenSelectionExists() {
        val result = FocusedFieldInserter.mergeText(
            existing = "Hello there",
            insertion = " world",
            selectionStart = 5,
            selectionEnd = 11,
        )
        assertEquals("Hello world", result)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.insertion.FocusedFieldInserterTest"`

Expected: FAIL because `FocusedFieldInserter` does not exist.

- [ ] **Step 3: Implement the insertion helper and service shell**

```kotlin
// app/src/main/java/com/frank/voiceoverlay/insertion/FocusedFieldInserter.kt
package com.frank.voiceoverlay.insertion

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

object FocusedFieldInserter {
    fun mergeText(existing: String, insertion: String, selectionStart: Int, selectionEnd: Int): String {
        val safeStart = selectionStart.coerceAtLeast(0)
        val safeEnd = selectionEnd.coerceAtLeast(safeStart)
        return if (safeStart == safeEnd) {
            existing.substring(0, safeStart) + insertion + existing.substring(safeStart)
        } else {
            existing.substring(0, safeStart) + insertion + existing.substring(safeEnd)
        }
    }

    fun replaceText(node: AccessibilityNodeInfo, mergedText: String): Boolean {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, mergedText)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }
}
```

```kotlin
// app/src/main/java/com/frank/voiceoverlay/insertion/OverlayAccessibilityService.kt
package com.frank.voiceoverlay.insertion

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class OverlayAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    fun insert(text: String): Boolean {
        val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val existing = node.text?.toString().orEmpty()
        val start = node.textSelectionStart.takeIf { it >= 0 } ?: existing.length
        val end = node.textSelectionEnd.takeIf { it >= 0 } ?: start
        val merged = FocusedFieldInserter.mergeText(existing, text, start, end)
        return FocusedFieldInserter.replaceText(node, merged)
    }
}
```

- [ ] **Step 4: Run the insertion test and a local assemble**

Run: `./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.insertion.FocusedFieldInserterTest" :app:assembleDebug`

Expected: PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/frank/voiceoverlay/insertion/FocusedFieldInserter.kt app/src/main/java/com/frank/voiceoverlay/insertion/OverlayAccessibilityService.kt app/src/main/res/xml/accessibility_service_config.xml app/src/test/java/com/frank/voiceoverlay/insertion/FocusedFieldInserterTest.kt app/src/main/AndroidManifest.xml
git commit -m $'Wire Accessibility-based text insertion for focused fields

Constraint: MVP inserts into the focused editable node or fails fast
Rejected: Clipboard fallback path | not selected for first release
Confidence: medium
Scope-risk: broad
Directive: Do not silently guess targets when no focused editable field exists
Tested: ./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.insertion.FocusedFieldInserterTest" :app:assembleDebug
Not-tested: cross-app insertion behavior on a connected device'
```

## Task 6: Implement bubble gesture parsing and radial shortcuts

**Files:**
- Create: `app/src/main/java/com/frank/voiceoverlay/overlay/BubbleGestureInterpreter.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/overlay/BubbleUiState.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/overlay/RadialShortcutMenu.kt`
- Test: `app/src/test/java/com/frank/voiceoverlay/overlay/BubbleGestureInterpreterTest.kt`

- [ ] **Step 1: Write the failing gesture interpreter tests**

```kotlin
package com.frank.voiceoverlay.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleGestureInterpreterTest {
    @Test
    fun secondTapWithinThreshold_emitsDoubleTap() {
        val interpreter = BubbleGestureInterpreter(doubleTapWindowMillis = 250)
        assertEquals(BubbleGesture.SingleTapPending, interpreter.onTap(1000))
        assertEquals(BubbleGesture.DoubleTap, interpreter.onTap(1180))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.overlay.BubbleGestureInterpreterTest"`

Expected: FAIL because the interpreter does not exist.

- [ ] **Step 3: Implement the gesture parser and radial menu model**

```kotlin
// app/src/main/java/com/frank/voiceoverlay/overlay/BubbleGestureInterpreter.kt
package com.frank.voiceoverlay.overlay

sealed interface BubbleGesture {
    data object SingleTapPending : BubbleGesture
    data object DoubleTap : BubbleGesture
}

class BubbleGestureInterpreter(
    private val doubleTapWindowMillis: Long = 250,
) {
    private var lastTapAt: Long? = null

    fun onTap(timestampMillis: Long): BubbleGesture {
        val previous = lastTapAt
        return if (previous != null && timestampMillis - previous <= doubleTapWindowMillis) {
            lastTapAt = null
            BubbleGesture.DoubleTap
        } else {
            lastTapAt = timestampMillis
            BubbleGesture.SingleTapPending
        }
    }
}
```

```kotlin
// app/src/main/java/com/frank/voiceoverlay/overlay/BubbleUiState.kt
package com.frank.voiceoverlay.overlay

import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.shortcuts.ShortcutPreset

data class BubbleUiState(
    val recordingState: RecordingState = RecordingState.IDLE,
    val shortcutsVisible: Boolean = false,
    val shortcuts: List<ShortcutPreset> = emptyList(),
)
```

```kotlin
// app/src/main/java/com/frank/voiceoverlay/overlay/RadialShortcutMenu.kt
package com.frank.voiceoverlay.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadialShortcutMenu(
    presets: List<ShortcutPreset>,
    onPresetClick: (ShortcutPreset) -> Unit,
) {
    Box {
        presets.forEachIndexed { index, preset ->
            val angle = Math.toRadians((360.0 / presets.size) * index)
            Button(
                modifier = Modifier.offset(
                    x = (cos(angle) * 96).dp,
                    y = (sin(angle) * 96).dp,
                ),
                onClick = { onPresetClick(preset) },
            ) {
                Text(preset.label)
            }
        }
    }
}
```

- [ ] **Step 4: Run the gesture tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.overlay.BubbleGestureInterpreterTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/frank/voiceoverlay/overlay/BubbleGestureInterpreter.kt app/src/main/java/com/frank/voiceoverlay/overlay/BubbleUiState.kt app/src/main/java/com/frank/voiceoverlay/overlay/RadialShortcutMenu.kt app/src/test/java/com/frank/voiceoverlay/overlay/BubbleGestureInterpreterTest.kt
git commit -m $'Define bubble tap semantics and shortcut menu state

Constraint: Single tap records, double tap expands shortcuts
Rejected: Mode-heavy gesture system | unnecessary for MVP
Confidence: high
Scope-risk: narrow
Directive: Keep gesture timing isolated from rendering code
Tested: ./gradlew :app:testDebugUnitTest --tests "com.frank.voiceoverlay.overlay.BubbleGestureInterpreterTest"
Not-tested: device-specific gesture feel tuning'
```

## Task 7: Build the overlay service and connect it to the coordinator

**Files:**
- Create: `app/src/main/java/com/frank/voiceoverlay/overlay/OverlayBubbleService.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/overlay/OverlayBubbleView.kt`
- Test: `app/src/androidTest/java/com/frank/voiceoverlay/overlay/OverlayBubbleSmokeTest.kt`

- [ ] **Step 1: Write the failing overlay smoke test**

```kotlin
package com.frank.voiceoverlay.overlay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayBubbleSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun overlayViewShowsBubbleAndShortcutLabels() {
        composeRule.setContent {
            OverlayBubbleView(
                uiState = BubbleUiState(
                    shortcutsVisible = true,
                    shortcuts = listOf(
                        com.frank.voiceoverlay.shortcuts.ShortcutPreset("ship-pr", "Ship-PR", "Good, please proceed to use \$ship-pr", 0),
                    ),
                ),
                onBubbleTap = {},
                onShortcutTap = {},
            )
        }

        composeRule.onNodeWithContentDescription("Voice input bubble").assertIsDisplayed()
        composeRule.onNodeWithText("Ship-PR").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the Android test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.frank.voiceoverlay.overlay.OverlayBubbleSmokeTest`

Expected: FAIL because the service and test wiring do not exist yet, or because the service is not implemented.

- [ ] **Step 3: Implement the overlay service with WindowManager-hosted Compose content**

```kotlin
// app/src/main/java/com/frank/voiceoverlay/overlay/OverlayBubbleService.kt
package com.frank.voiceoverlay.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView

class OverlayBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        composeView = ComposeView(this).apply {
            setContent {
                OverlayBubbleView()
            }
        }
        windowManager.addView(
            composeView,
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 32
                y = 240
            }
        )
    }

    override fun onDestroy() {
        if (::composeView.isInitialized) windowManager.removeView(composeView)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

```kotlin
// app/src/main/java/com/frank/voiceoverlay/overlay/OverlayBubbleView.kt
package com.frank.voiceoverlay.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun OverlayBubbleView(
    uiState: BubbleUiState = BubbleUiState(),
    onBubbleTap: () -> Unit = {},
    onShortcutTap: (com.frank.voiceoverlay.shortcuts.ShortcutPreset) -> Unit = {},
) {
    Box {
        Surface(
            modifier = Modifier
                .size(64.dp)
                .semantics { contentDescription = "Voice input bubble" },
            shape = CircleShape,
            onClick = onBubbleTap,
        ) {}

        if (uiState.shortcutsVisible) {
            RadialShortcutMenu(
                presets = uiState.shortcuts,
                onPresetClick = onShortcutTap,
            )
        }
    }
}
```

- [ ] **Step 4: Run the Android smoke test**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.frank.voiceoverlay.overlay.OverlayBubbleSmokeTest`

Expected: PASS on an emulator or device with overlay permission granted.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/frank/voiceoverlay/overlay/OverlayBubbleService.kt app/src/main/java/com/frank/voiceoverlay/overlay/OverlayBubbleView.kt app/src/androidTest/java/com/frank/voiceoverlay/overlay/OverlayBubbleSmokeTest.kt app/src/main/AndroidManifest.xml
git commit -m $'Render the always-on bubble overlay through a dedicated service

Constraint: Bubble must stay system-wide while enabled
Rejected: Activity-only floating UI | not viable across other apps
Confidence: medium
Scope-risk: broad
Directive: Keep service state thin and delegate business flow to coordinator
Tested: ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.frank.voiceoverlay.overlay.OverlayBubbleSmokeTest
Not-tested: OEM-specific overlay restrictions'
```

## Task 8: Add settings UI, permission gates, and preset management

**Files:**
- Create: `app/src/main/java/com/frank/voiceoverlay/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/frank/voiceoverlay/permissions/PermissionGate.kt`
- Test: `app/src/androidTest/java/com/frank/voiceoverlay/settings/SettingsScreenTest.kt`
- Modify: `app/src/main/java/com/frank/voiceoverlay/MainActivity.kt`

- [ ] **Step 1: Write the failing settings screen test**

```kotlin
package com.frank.voiceoverlay.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun screenShowsApiKeyAndShortcutSections() {
        composeRule.onNodeWithText("OpenAI API Key").assertIsDisplayed()
        composeRule.onNodeWithText("Shortcut Presets").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.frank.voiceoverlay.settings.SettingsScreenTest`

Expected: FAIL because the screen is not implemented.

- [ ] **Step 3: Implement the settings surface and permission checks**

```kotlin
// app/src/main/java/com/frank/voiceoverlay/settings/SettingsScreen.kt
package com.frank.voiceoverlay.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onApiKeyChanged: (String) -> Unit,
    onOverlayEnabledChanged: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
) {
    Column {
        Text("OpenAI API Key")
        BasicTextField(
            value = settings.openAiApiKey,
            onValueChange = onApiKeyChanged,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Enable Overlay")
        Switch(
            checked = settings.overlayEnabled,
            onCheckedChange = onOverlayEnabledChanged,
        )
        Text("Shortcut Presets")
        settings.presets.forEach { Text(it.label) }
        Button(onClick = onOpenAccessibilitySettings) { Text("Open Accessibility Settings") }
        Button(onClick = onOpenOverlaySettings) { Text("Open Overlay Settings") }
    }
}
```

```kotlin
// app/src/main/java/com/frank/voiceoverlay/settings/SettingsViewModel.kt
package com.frank.voiceoverlay.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AppSettings())
    val state: StateFlow<AppSettings> = mutableState

    fun load() {
        viewModelScope.launch {
            mutableState.value = settingsStore.readOnce()
        }
    }

    fun updateApiKey(value: String) {
        mutableState.value = mutableState.value.copy(openAiApiKey = value)
    }

    fun setOverlayEnabled(enabled: Boolean) {
        mutableState.value = mutableState.value.copy(overlayEnabled = enabled)
    }

    fun persist() {
        viewModelScope.launch {
            settingsStore.save(mutableState.value)
        }
    }
}
```

```kotlin
// app/src/main/java/com/frank/voiceoverlay/permissions/PermissionGate.kt
package com.frank.voiceoverlay.permissions

import android.content.Context
import android.provider.Settings

class PermissionGate(
    private val context: Context,
) {
    fun canDrawOverApps(): Boolean = Settings.canDrawOverlays(context)
}
```

- [ ] **Step 4: Run the settings screen test**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.frank.voiceoverlay.settings.SettingsScreenTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/frank/voiceoverlay/MainActivity.kt app/src/main/java/com/frank/voiceoverlay/settings/SettingsViewModel.kt app/src/main/java/com/frank/voiceoverlay/settings/SettingsScreen.kt app/src/main/java/com/frank/voiceoverlay/permissions/PermissionGate.kt app/src/androidTest/java/com/frank/voiceoverlay/settings/SettingsScreenTest.kt
git commit -m $'Expose the local settings needed to operate the overlay app

Constraint: MVP needs API key entry, overlay toggle, and preset visibility only
Rejected: Full admin-style settings console | excessive for personal use
Confidence: high
Scope-risk: moderate
Directive: Keep permission prompts explicit and user-driven
Tested: ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.frank.voiceoverlay.settings.SettingsScreenTest
Not-tested: manual UX polish on small-screen devices'
```

## Task 9: Final verification and operator documentation

**Files:**
- Create: `README.md`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write the failing documentation check**

```text
Expected README sections:
- What this app does
- Required Android permissions
- How to add the OpenAI API key
- How recording and shortcuts behave
- Known limitations
```

- [ ] **Step 2: Run the verification suite before writing docs**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: PASS for unit tests and debug assemble. If anything fails, fix code before documenting.

- [ ] **Step 3: Write the operator-facing README**

```markdown
# VibeTap

VibeTap is a personal-use Android overlay app for dictation without a keyboard.

## Permissions

- Draw over other apps
- Accessibility service
- Microphone

## Setup

1. Install the debug build.
2. Open settings.
3. Paste your OpenAI API key.
4. Enable overlay and accessibility.

## Behavior

- Single tap starts and stops recording.
- Double tap opens radial shortcut presets.
- Dictation inserts directly into the focused text field.

## Known limitations

- Accessibility insertion depends on the target app.
- The app requires network access for transcription and cleanup.
- API keys are stored locally and should be treated as personal-use only.
```

- [ ] **Step 4: Run the full verification pass**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest`

Expected: `BUILD SUCCESSFUL` for unit, assemble, and connected tests on a configured emulator/device.

- [ ] **Step 5: Commit**

```bash
git add README.md app/src/main/AndroidManifest.xml
git commit -m $'Document how to operate and verify the overlay dictation app

Constraint: Personal-use release must be self-service to set up
Rejected: Undocumented prototype handoff | too brittle for future iteration
Confidence: high
Scope-risk: narrow
Directive: Keep README aligned with actual runtime permissions and behaviors
Tested: ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest
Not-tested: release-signing and store-distribution flows'
```

## Spec Coverage Check

- Floating bubble overlay: Tasks 6 and 7
- Tap-to-start / tap-to-stop recording: Tasks 4 and 7
- Whisper transcription: Task 3
- `gpt-5-nano` cleanup: Task 3
- Immediate Accessibility insertion: Task 5
- Double-tap radial shortcuts: Tasks 6 and 7
- User-editable presets: Tasks 2 and 8
- Local API key settings: Tasks 2 and 8
- Error and permission handling: Tasks 5, 7, and 8
- README/operator guidance: Task 9

## Placeholder Scan

No `TODO`, `TBD`, or “implement later” markers are permitted during execution. If a step requires new names or paths, update this plan first rather than improvising mid-task.

## Type Consistency Notes

- Use `RecordingState` exactly as `IDLE`, `LISTENING`, `PROCESSING`, `ERROR`.
- Keep the primary preset model name as `ShortcutPreset`.
- Keep the main orchestration class name as `DictationCoordinator`.
- Keep the accessibility service name as `OverlayAccessibilityService`.
