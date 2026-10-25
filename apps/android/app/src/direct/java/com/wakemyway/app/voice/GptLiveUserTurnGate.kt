package com.wakemyway.app.voice

import java.util.Locale

/**
 * Conservative admission of tentative spoken engagement into WakeRuntime.
 * Transcript fragments have no done event or source-verification guarantees.
 * Overlap could be speaker echo, so never credit it as Activation Evidence.
 * Caller still reports non-coherent turns to release the listening state.
 *
 * A blanket 4s cooldown made genuinely different short replies appear ignored.
 * Keep a short anti-fragment interval and apply the longer cooldown only to
 * identical recent replies. This is still not proof of wakefulness.
 */
internal object GptLiveUserTurnGate {
    private const val MIN_SEPARATE_TURN_MS = 1_500L
    private const val REPEATED_TURN_COOLDOWN_MS = 4_000L
    private val word = Regex("[\\p{L}\\p{N}]+")
    private val whitespace = Regex("\\s+")
    private val fillers = setOf("uh", "um", "hmm", "hm", "eh", "ah", "mm", "mmm")

    /**
     * Ephemeral hash for anti-duplicate comparison. The caller does not retain
     * microphone text or write the fingerprint to logs/history.
     */
    fun fingerprint(content: String): Int =
        content.trim().lowercase(Locale.ROOT).replace(whitespace, " ").hashCode()

    fun isPlausible(
        content: String,
        overlappedOutput: Boolean,
        msSinceLastCredit: Long,
        sameAsRecentCreditedTurn: Boolean = false,
    ): Boolean {
        if (overlappedOutput || msSinceLastCredit < MIN_SEPARATE_TURN_MS) return false
        if (sameAsRecentCreditedTurn && msSinceLastCredit < REPEATED_TURN_COOLDOWN_MS) return false
        val words = word.findAll(content.trim()).map { it.value }.take(30).toList()
        if (words.isEmpty() || words.size >= 30) return false
        val letters = words.sumOf { it.count(Char::isLetter) }
        if (letters < 2) return false
        return !(words.size == 1 && words[0].lowercase(Locale.ROOT) in fillers)
    }
}
