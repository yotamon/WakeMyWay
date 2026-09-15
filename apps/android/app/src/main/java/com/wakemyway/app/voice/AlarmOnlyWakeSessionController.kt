package com.wakemyway.app.voice

/**
 * Wake-session lifecycle for alarms that explicitly disable Voice Check-In.
 *
 * This controller is intentionally inert. It does not create speech recognition, TTS, Realtime,
 * microphone, or motion resources. AlarmPlaybackService remains the execution authority and the
 * user ends or snoozes the wake from the Wake Surface controls.
 */
class AlarmOnlyWakeSessionController : WakeSessionController {
    override fun onSurfaceVisible() = Unit

    override fun onSurfaceHidden() = Unit

    override fun closeForTerminalAction() = Unit

    override fun close() = Unit
}