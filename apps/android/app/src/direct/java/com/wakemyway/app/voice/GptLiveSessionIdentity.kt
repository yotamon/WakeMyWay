package com.wakemyway.app.voice

/** Live session IDs are opaque provider-owned identifiers, not app-defined prefixes. */
internal object GptLiveSessionIdentity {
    fun isValid(id: String): Boolean =
        id.length in 4..256 && id.none { it.isWhitespace() || it.isISOControl() }
}
