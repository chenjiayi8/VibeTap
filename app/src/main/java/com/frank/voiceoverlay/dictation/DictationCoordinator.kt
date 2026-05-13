package com.frank.voiceoverlay.dictation

data class DictationDraft(
    val transcript: String = "",
    val cleanedText: String = "",
)

class DictationCoordinator {
    fun nextStateAfterStop(hasEditableTarget: Boolean): RecordingState {
        return if (hasEditableTarget) {
            RecordingState.PROCESSING
        } else {
            RecordingState.ERROR
        }
    }
}
