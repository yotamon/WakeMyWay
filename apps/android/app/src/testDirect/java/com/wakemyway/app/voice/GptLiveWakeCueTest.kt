package com.wakemyway.app.voice

import com.wakemyway.core.runtime.SpeechIntent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GptLiveWakeCueTest {
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
}
