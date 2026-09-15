package com.wakemyway.app.alarm

import android.content.Context
import androidx.annotation.RawRes
import com.wakemyway.app.R
import com.wakemyway.core.alarm.WakeSoundId

/**
 * Critical playback tuning for one bundled WakeMyWay sound.
 *
 * These gains affect only audio rendering. Alarm authority, Stop/Snooze durability and wake
 * lifecycle remain owned by AlarmKernel.
 */
data class WakeSoundPlaybackSpec(
    val criticalGain: Float,
    val voiceWindowGain: Float,
) {
    init {
        require(criticalGain in 0f..1f) { "Critical wake gain must be between 0 and 1" }
        require(voiceWindowGain in 0f..criticalGain) {
            "Voice-window gain must be between 0 and the critical wake gain"
        }
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
 * Direct-Boot-safe registry for the approved WakeMyWay sonic identities.
 *
 * The product model can persist these ids before the actual WAVs are bundled. A profile becomes
 * executable only when its exact `res/raw` resource exists. Missing or unknown sounds fail safe to
 * the private emergency WAV, never to silence or to a different branded sound.
 */
object WakeSoundCatalog {
    private val safeImmediatePlayback = WakeSoundPlaybackSpec(
        criticalGain = 1f,
        voiceWindowGain = 0.12f,
    )

    val emergencyPlaybackSpec = WakeSoundPlaybackSpec(
        criticalGain = 1f,
        voiceWindowGain = 0.12f,
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
        consumerProfiles.filter { profile ->
            rawResourceId(context, profile.rawResourceName) != 0
        }

    fun isBundled(context: Context, id: WakeSoundId): Boolean =
        profile(id)?.let { profile -> rawResourceId(context, profile.rawResourceName) != 0 } == true

    fun resolve(context: Context, id: WakeSoundId): ResolvedWakeSound = resolve(
        id = id,
        emergencyResourceId = R.raw.emergency_alarm,
        resourceLookup = { rawName -> rawResourceId(context, rawName) },
    )

    internal fun resolve(
        id: WakeSoundId,
        @RawRes emergencyResourceId: Int,
        resourceLookup: (String) -> Int,
    ): ResolvedWakeSound {
        val profile = profile(id)
        val bundledResourceId = profile?.let { resourceLookup(it.rawResourceName) } ?: 0
        if (profile != null && bundledResourceId != 0) {
            return ResolvedWakeSound(
                requestedId = id,
                profile = profile,
                rawResourceId = bundledResourceId,
                playback = profile.playback,
                usedEmergencyFallback = false,
            )
        }

        return ResolvedWakeSound(
            requestedId = id,
            profile = profile,
            rawResourceId = emergencyResourceId,
            playback = emergencyPlaybackSpec,
            usedEmergencyFallback = true,
        )
    }

    @Suppress("DiscouragedApi")
    private fun rawResourceId(context: Context, resourceName: String): Int =
        context.resources.getIdentifier(resourceName, "raw", context.packageName)
}
