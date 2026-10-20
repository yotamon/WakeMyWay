package com.wakemyway.app.voice

/**
 * Conservative admission of tentative spoken engagement into WakeRuntime.
 * Transcript fragments have no done event or source-verification guarantees.
 * Overlap could be speaker echo, so never credit it as Activation Evidence.
 * Caller still reports non-coherent turns to release the listening state.
 */
internal object GptLiveUserTurnGate {
    private const val CREDIT_COOLDOWN_MS = 4_000L
    private val word = Regex("[\\p{L}\\p{N}]+")
    private val fillers = setOf("uh", "um", "hmm", "hm", "eh", "ah", "mm", "mmm")

    fun isPlausible(
        content: String,
        overlappedOutput: Boolean,
        msSinceLastCredit: Long,
    ): Boolean {
        if (overlappedOutput || msSinceLastCredit < CREDIT_COOLDOWN_MS) return false
        val words = word.findAll(content.trim()).map { it.value }.take(30).toList()
        if (words.isEmpty() || words.size >= 30) return false
        val letters = words.sumOf { it.count(Char::isLetter) }
        if (letters < 2) return false
        return !(words.size == 1 && words[0].lowercase() in fillers)
    }
}
