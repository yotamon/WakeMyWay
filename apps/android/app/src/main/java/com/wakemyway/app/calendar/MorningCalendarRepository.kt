package com.wakemyway.app.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.UserManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Read-only, local Calendar Provider boundary. It uses Google calendars already synchronized with
 * Android; the Neon/Better Auth Google sign-in is identity-only and must NOT be treated as a
 * Google Calendar OAuth credential.
 *
 * No event names, meeting times, model prompts or transcripts are written to disk. Settings live
 * in credential-protected preferences, never the Alarm Kernel's Direct Boot state.
 */
internal class MorningCalendarRepository(context: Context) {
    private val app = context.applicationContext
    private val preferences = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class CalendarChoice(val id: Long, val name: String, val accountLabel: String)

    data class Settings(
        val selectedCalendarId: Long? = null,
        val includeEventTitlesInVoice: Boolean = false,
        val consentRevision: Long = 0L,
    ) {
        val enabled: Boolean get() = selectedCalendarId != null
    }

    data class MorningFact(
        val description: String,
        val observedAtMillis: Long,
        val startsAtMillis: Long,
        val consentRevision: Long,
    )

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun settings(): Settings = Settings(
        selectedCalendarId = preferences.getLong(CALENDAR_ID, -1L).takeIf { it > 0L },
        includeEventTitlesInVoice = preferences.getBoolean(SHARE_TITLES, false),
        consentRevision = preferences.getLong(CONSENT_REVISION, 0L),
    )

    fun selectCalendar(id: Long) {
        require(id > 0L)
        // Revocation or changing calendars clears the sensitive-title consent.
        preferences.edit().putLong(CALENDAR_ID, id).putBoolean(SHARE_TITLES, false)
            .putLong(CONSENT_REVISION, settings().consentRevision + 1L).apply()
    }

    fun setShareTitles(enabled: Boolean) {
        if (!settings().enabled) return
        preferences.edit().putBoolean(SHARE_TITLES, enabled)
            .putLong(CONSENT_REVISION, settings().consentRevision + 1L).apply()
    }

    fun disconnect() {
        // Keep a monotonic non-sensitive consent generation to invalidate any in-flight result.
        // Clearing it back to zero could accidentally re-match a prior revision after reconnect.
        preferences.edit()
            .remove(CALENDAR_ID)
            .remove(SHARE_TITLES)
            .putLong(CONSENT_REVISION, settings().consentRevision + 1L)
            .commit()
    }

    /**
     * Queries only calendars with Google's Android sync adapter. Failure or missing permission is
     * not an empty calendar and must not become "you have no plans" speech.
     */
    fun availableGoogleCalendars(): List<CalendarChoice> {
        if (!hasPermission() || !isUserUnlocked()) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.SYNC_EVENTS,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val choices = mutableListOf<CalendarChoice>()
        app.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)
            ?.use { cursor ->
                while (cursor.moveToNext() && choices.size < MAX_CALENDARS) {
                    val id = cursor.getLong(0)
                    val name = cursor.getString(1).orEmpty().trim()
                    val account = cursor.getString(2).orEmpty()
                    val type = cursor.getString(3).orEmpty()
                    val visible = cursor.getInt(4) != 0
                    val synced = cursor.getInt(5) != 0
                    val readAccess = cursor.getInt(6) >= CalendarContract.Calendars.CAL_ACCESS_READ
                    if (id > 0L && name.isNotEmpty() && type == "com.google" &&
                        visible && synced && readAccess
                    ) choices += CalendarChoice(id, name.take(120), account.take(120))
                }
            }
        return choices.sortedWith(compareBy({ it.accountLabel }, { it.name }, { it.id }))
    }

    /**
     * Called only on a background dispatcher by the optional voice controller. The provider may
     * block; this operation may never run on an alarm delivery or Stop/Snooze thread.
     * No hidden network requests, event descriptions, attendees or locations are requested.
     */
    fun readNextFact(nowMillis: Long = System.currentTimeMillis()): MorningFact? {
        if (!hasPermission() || !isUserUnlocked()) return null
        val selected = settings().takeIf { it.enabled } ?: return null
        val calendarId = selected.selectedCalendarId ?: return null

        // Protect against a stale/recycled calendar ID or an account that was removed.
        if (availableGoogleCalendars().none { it.id == calendarId }) return null

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.ACCESS_LEVEL,
            CalendarContract.Events.AVAILABILITY,
            CalendarContract.Events.STATUS,
            CalendarContract.Events.SELF_ATTENDEE_STATUS,
        )
        val until = nowMillis + 12L * 60L * 60L * 1_000L
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
            ContentUris.appendId(this, nowMillis)
            ContentUris.appendId(this, until)
        }.build()

        var first: Candidate? = null
        app.contentResolver.query(
            uri, projection, "${CalendarContract.Events.CALENDAR_ID} = ?",
            arrayOf(calendarId.toString()), CalendarContract.Instances.BEGIN + " ASC",
        )
            ?.use { cursor ->
                var seen = 0
                while (cursor.moveToNext() && seen++ < MAX_INSTANCES) {
                    val start = cursor.getLong(0)
                    if (start < nowMillis || start >= until || cursor.getLong(2) != calendarId) continue
                    if (cursor.getInt(4) != 0) continue // all-day is not time pressure
                    if (cursor.isNull(5) || cursor.getInt(5) == CalendarContract.Events.ACCESS_PRIVATE ||
                        cursor.getInt(5) == CalendarContract.Events.ACCESS_CONFIDENTIAL
                    ) continue
                    if (cursor.isNull(6) ||
                        cursor.getInt(6) != CalendarContract.Events.AVAILABILITY_BUSY
                    ) continue
                    if (!cursor.isNull(7) &&
                        cursor.getInt(7) == CalendarContract.Events.STATUS_CANCELED
                    ) continue
                    if (!cursor.isNull(8) &&
                        cursor.getInt(8) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
                    ) continue
                    val title = cursor.getString(3)
                    val candidate = Candidate(start, title)
                    if (first?.let { candidate.startMillis < it.startMillis } != false) first = candidate
                }
            } ?: return null

        val event = first ?: return null
        val localTime = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(event.startMillis))
        val title = if (selected.includeEventTitlesInVoice) safeTitle(event.title) else null
        val description = if (title == null) {
            "A scheduled event on the selected Google calendar starts at $localTime."
        } else {
            "The selected Google calendar shows an event titled \"$title\" at $localTime."
        }
        // The provider may be out of sync with Google. This is a locally observed fact, not proof
        // of a currently confirmed appointment, user's intention, commute or preparation time.
        return MorningFact(description = description, observedAtMillis = nowMillis, startsAtMillis = event.startMillis, consentRevision = selected.consentRevision)
    }

    private data class Candidate(val startMillis: Long, val title: String?)

    private fun isUserUnlocked(): Boolean =
        app.getSystemService(UserManager::class.java)?.isUserUnlocked == true

    companion object {
        private const val PREFS = "morning-calendar-consent-v1"
        private const val CALENDAR_ID = "selected-calendar"
        private const val SHARE_TITLES = "remote-voice-event-titles"
        private const val CONSENT_REVISION = "consent-revision"
        private const val MAX_CALENDARS = 100
        private const val MAX_INSTANCES = 250

        /**
         * Titles are untrusted provider data, never agent instructions. Drop suspicious, multiline
         * or control-bearing content; conservative omission is safer than a compromised prompt.
         */
        internal fun safeTitle(title: String?): String? {
            val input = title?.trim() ?: return null
            if (input.length !in 1..80 || input.any { it.isISOControl() }) return null
            val normalized = input.replace(Regex("\\s+"), " ")
            val hostile = Regex(
                "(ignore (all |previous |prior )?instructions|system prompt|developer message|" +
                    "assistant:|role:|tool call|\\bexecute\\b|\\bprompt\\b)",
                RegexOption.IGNORE_CASE,
            )
            return normalized.takeIf { !hostile.containsMatchIn(it) }
        }
    }
}
