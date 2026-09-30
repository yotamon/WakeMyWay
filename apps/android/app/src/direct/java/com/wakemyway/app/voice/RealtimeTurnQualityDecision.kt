package com.wakemyway.app.voice

/**
 * Converts the hidden Realtime turn classifier output into the smallest possible product signal.
 *
 * This parser is intentionally strict: anything except the exact allowlisted decision is treated
 * as unusable spoken engagement. Provider prose, malformed output, or future schema drift must
 * never manufacture Wake Runtime activation evidence.
 */
internal object RealtimeTurnQualityDecision {
    fun fromModelOutput(output: String): Boolean = output.trim().uppercase() == "USABLE"
}
