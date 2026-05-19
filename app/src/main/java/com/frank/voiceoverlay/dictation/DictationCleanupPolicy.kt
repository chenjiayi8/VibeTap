package com.frank.voiceoverlay.dictation

internal object DictationCleanupPolicy {
    fun finalize(rawTranscript: String, cleanedCandidate: String): String {
        val normalizedRaw = rawTranscript.normalizeWhitespace()
        val trimmedCandidate = cleanedCandidate.trim()
        val normalizedCandidate = cleanedCandidate.normalizeWhitespace()

        if (normalizedCandidate.isBlank()) {
            return rawTranscript
        }
        if (looksAssistantLike(trimmedCandidate)) {
            return rawTranscript
        }
        if (wordCount(normalizedCandidate) > wordCount(normalizedRaw) + 4) {
            return rawTranscript
        }
        if (wordOverlap(normalizedRaw, normalizedCandidate) < 0.6) {
            return rawTranscript
        }
        return normalizedCandidate
    }

    private fun looksAssistantLike(text: String): Boolean {
        val lowercase = text.lowercase()
        return lowercase.contains('\n') ||
            lowercase.contains("q:") ||
            lowercase.contains("a:") ||
            lowercase.startsWith("here are") ||
            lowercase.startsWith("here's") ||
            lowercase.startsWith("i can") ||
            lowercase.startsWith("if you ")
    }

    private fun wordCount(text: String): Int =
        comparisonTokens(text).size

    private fun wordOverlap(raw: String, candidate: String): Double {
        val candidateTokens = comparisonTokens(candidate)
        if (candidateTokens.isEmpty()) {
            return 0.0
        }

        val rawTokens = comparisonTokens(raw).toSet()
        if (rawTokens.isEmpty()) {
            return 0.0
        }

        val overlapCount = candidateTokens.count { it in rawTokens }
        return overlapCount.toDouble() / candidateTokens.size.toDouble()
    }

    private fun comparisonTokens(text: String): List<String> =
        WORD_TOKEN_REGEX.findAll(text.lowercase())
            .map { it.value }
            .toList()

    private fun String.normalizeWhitespace(): String =
        trim().replace(Regex("\\s+"), " ")

    private val WORD_TOKEN_REGEX = Regex("[\\p{L}\\p{N}]+")
}
