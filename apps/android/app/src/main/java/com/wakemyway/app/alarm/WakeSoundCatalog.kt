package com.wakemyway.app.alarm

import android.content.Context
import androidx.annotation.RawRes
import com.wakemyway.app.R
import com.wakemyway.core.alarm.WakeSoundId

/**
 * Critical-path playback tuning for one bundled WakeMyWay wake sound.
 *
 * Ramp metadata is intentionally explicit even while the approved profiles use IMMEDIATE today.
 * A gentler ramp must only be enabled after physical-device validation proves the beginning remains
 * reliably audible. Voice ducking is a playback concern and never changes alarm authority.
 */
data class WakeSoundPlaybackSpec(
    val criticalGain: Float,
    val voiceWindowGain: Float,
    val rampStartGain: Float,
    val rampDurationMillis: Long,
) {
    init {
        require(criticalGain in 0f..1f)
        require(voiceWindowGain in 0f..criticalGain)
        require(rampStartGain in 0f..criticalGain)
        require(rampDurationMillis >= 0)
    }
}

data class WakeSoundProfile(
    val id: WakeSoundId,
    /** Android raw-resource basename, without extension. */
    val rawResourceName: String,
    val playback: WakeSoundPlaybackSpec,
)

data class ResolvedWakeSound(
    val requestedId: WakeSoundId,
    val profile: WakeSoundProfile?,
    @RawRes val rawResourceId: Int,
    val playback: WakeSoundPlaybackSpec,
    val usedEmergencyFallback: Boolean,
)

/**
 * Registry for consumer-selectable bundled wake sounds.
 *
 * The three approved ids already belong to the product model. A profile is considered available
 * only when its exact raw resource exists in the APK. Until the approved WAV bytes are committed,
 * resolution fails safe to the private emergency fallback instead of pretending another sound is
 * the selected profile.
 */
object WakeSoundCatalog {
    private val safeImmediatePlayback = WakeSoundPlaybackSpec(
        criticalGain = 1f,
        voiceWindowGain = 0.12f,
        rampStartGain = 1f,
        rampDurationMillis = 0L,
    )

    val emergencyPlaybackSpec = WakeSoundPlaybackSpec(
        criticalGain = 1f,
        voiceWindowGain = 0.12f,
        rampStartGain = 1f,
        rampDurationMillis = 0L,
    )

    val consumerProfiles: List<WakeSoundProfile> = listOf(
        WakeSoundProfile(
            id = WakeSoundId.MORNING_LIGHT,
            rawResourceName = "morning_light",
            playback = safeImmediatePlayback,
        ),
        WakeSoundProfile(
            id = WakeSoundId.SOFT_START,
            rawResourceName = "soft_start",
            playback = safeImmediatePlayback,
        ),
        WakeSoundProfile(
            id = WakeSoundId.MORNING_PULSE,
            rawResourceName = "morning_pulse",
            playback = safeImmediatePlayback,
        ),
    )

    val defaultId: WakeSoundId = WakeSoundId.MORNING_LIGHT

    fun profile(id: WakeSoundId): WakeSoundProfile? =
        consumerProfiles.firstOrNull { it.id == id }

    fun availableProfiles(context: Context): List<WakeSoundProfile> =
        consumerProfiles.filter { rawResourceId(context, it.rawResourceName) != 0 }

    fun isBundled(context: Context, id: WakeSoundId): Boolean =
        profile(id)?.let { rawResourceId(context, it.rawResourceName) != 0 } == true

    fun resolve(context: Context, id: WakeSoundId): ResolvedWakeSound {
        val profile = profile(id)
        val bundledId = profile?.let { rawResourceId(context, it.rawResourceName) } ?: 0
        if (profile != null && bundledId != 0) {
            return ResolvedWakeSound(
                requestedId = id,
                profile = profile,
                rawResourceId = bundledId,
                playback = profile.playback,
                usedEmergencyFallback = false,
            )
        }

        return ResolvedWakeSound(
            requestedId = id,
            profile = profile,
            rawResourceId = R.raw.emergency_alarm,
            playback = emergencyPlaybackSpec,
            usedEmergencyFallback = true,
        )
    }

    @Suppress("DiscouragedApi")
    private fun rawResourceId(context: Context, resourceName: String): Int =
        context.resources.getIdentifier(resourceName, "raw", context.packageName)
}
