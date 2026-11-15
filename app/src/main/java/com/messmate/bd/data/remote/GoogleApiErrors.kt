package com.messmate.bd.data.remote

/**
 * The Google APIs answer with a JSON body that is far too long for a snackbar.
 *
 * This pulls the human readable part out of it:
 * `{"error":{"code":400,"message":"Invalid requests[0]...","status":"INVALID_ARGUMENT"}}`
 * becomes `Invalid requests[0]...`.
 */
object GoogleApiErrors {

    private val MESSAGE = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")

    /** The short, human readable part of an API error. */
    fun shorten(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return "Unknown error"

        return MESSAGE.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
            ?: text.take(200)
    }

    /** [shorten] applied to the message of this throwable. */
    fun Throwable.shortMessage(): String = shorten(message)
}
