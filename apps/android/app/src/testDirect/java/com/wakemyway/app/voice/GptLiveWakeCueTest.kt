package com.wakemyway.app.voice

import com.wakemyway.core.runtime.SpeechIntent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GptLiveWakeCueTest {
    @Test fun calendarFactIsOnlyPresentInOrientationCue() {
        val plan = com.wakemyway.core.personalization.WakeSessionStrategyResolver.resolve(
            preferences = com.wakemyway.core.personalization.WakePreferences(),
            characterId = com.wakemyway.core.alarm.CharacterId.ALFRED,
            voiceStyle = com.wakemyway.core.alarm.VoiceStyle.DEFAULT,
            wakePolicy = com.wakemyway.core.runtime.WakePolicy(),
        )
        val info = "A meeting at ten."
        val early = GptLiveWakeCue.forRequest(WakeSpeechRequest(SpeechIntent.InitialWake, plan, info))
        val late = GptLiveWakeCue.forRequest(WakeSpeechRequest(SpeechIntent.Orientation, plan, info))
        assertFalse(early.contains(info))
        assertTrue(late.contains(info))
        assertTrue(late.length <= 500)
    }
    @Test fun onlyAuthorizedAppIntentsCreateSpeechCues() {
        val initial = GptLiveWakeCue.forIntent(SpeechIntent.InitialWake)
        assertTrue(initial.contains("invite them to sit up"))
        assertFalse(initial.contains("you are awake"))
    }

    @Test fun spokenSnoozeCannotPretendToHaveScheduledAnything() {
        val cue = GptLiveWakeCue.forIntent(SpeechIntent.SnoozeConfirmation)
        assertTrue(cue.contains("tapping"))
        assertFalse(cue.contains("Snooze set"))
    }

    @Test fun reengagementRemainsNonDemanding() {
        val cue = GptLiveWakeCue.forIntent(SpeechIntent.ReEngage(1))
        assertTrue(cue.contains("no demand"))
    }

    @Test fun heldEngagementDoesNotIssueAnotherPhysicalTask() {
        assertTrue(GptLiveWakeCue.forIntent(SpeechIntent.HoldEngagement).contains("Do not give another task"))
    }

    @Test fun reluctantFirstReplyIsAcknowledgedRatherThanAutomaticallyCommanded() {
        val move = GptLiveWakeCue.forIntent(SpeechIntent.AskToMove)
        assertTrue(move.contains("reluctant or exhausted"))
        assertTrue(move.contains("without another instruction"))
        assertTrue(move.contains("Respond to what they just said first"))
    }

    @Test fun intentOverridesActionStyleForSocialReplies() {
        val plan = com.wakemyway.core.personalization.WakeSessionStrategyResolver.resolve(
            preferences = com.wakemyway.core.personalization.WakePreferences(),
            characterId = com.wakemyway.core.alarm.CharacterId.ALFRED,
            voiceStyle = com.wakemyway.core.alarm.VoiceStyle.DEFAULT,
            wakePolicy = com.wakemyway.core.runtime.WakePolicy(),
        )
        val cue = GptLiveWakeCue.forRequest(WakeSpeechRequest(SpeechIntent.HoldEngagement, plan))
        assertTrue(cue.contains("No new action"))
        assertFalse(cue.contains("Focus on one tiny practical action"))
        val silence = GptLiveWakeCue.forRequest(WakeSpeechRequest(SpeechIntent.ReEngage(1), plan))
        assertFalse(silence.contains("Focus on one tiny practical action"))
        assertTrue(silence.contains("no repeated movement cue"))
    }
}
