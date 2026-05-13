package com.frank.voiceoverlay.dictation

sealed class OpenAiClientException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class HttpError(
        val operation: String,
        val statusCode: Int,
        val responseBody: String,
    ) : OpenAiClientException("OpenAI $operation failed with HTTP $statusCode")

    class EmptyBody(
        val operation: String,
    ) : OpenAiClientException("OpenAI $operation returned an empty body")

    class ParseFailure(
        val operation: String,
        val responseBody: String,
        cause: Throwable,
    ) : OpenAiClientException("OpenAI $operation returned an invalid body", cause)

    class NetworkFailure(
        val operation: String,
        cause: Throwable,
    ) : OpenAiClientException("OpenAI $operation request failed", cause)
}
