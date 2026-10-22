# Premium voice alarm: real-device acceptance & human quality

**Status: NOT RUN.** This is a reproducible acceptance contract, not a claim that field evidence exists.
**Target:** same production-signed Direct APK identity as the currently installed WakeMyWay build. Never uninstall the existing app to get around a signing mismatch, because it can erase alarms.

## Record before each session

Timestamp and timezone; build/version/APK SHA; device model and Android patch; wake occurrence type; selected character, voice language and pacing; network (Wi-Fi/mobile/offline); screen state; output route (speaker/Bluetooth/call); permission state; local wake audible (yes/no); first **human-heard** voice time; Stop/Snooze behavior; audio overlap (yes/no); privacy-safe diagnostic stage and final provider usage (if present). Do not export speech, transcripts, calendar titles, private morning reasons, credentials or personal recordings.

## Alarm and OS release gates

Each row needs **PASS / FAIL / NOT RUN**, timestamp and evidence reference; a failure blocks wider release:

| ID | Exact scenario | Pass condition |
|---|---|---|
| A01 | Normal scheduled wake, screen locked | Local music and actionable Stop/Snooze, on time |
| A02 | Airplane mode | Local alarm works, no false voice-ready |
| A03 | Microphone denied or revoked | Local alarm works; no hidden capture |
| A04 | Doze/idle during scheduled wake | Physical OS fires the alarm and sound |
| A05 | Reboot and first wake before unlock | Direct Boot-safe alarm, no credential data |
| A06 | Stop while Live is speaking | Authoritative Stop; mic and route released |
| A07 | Snooze while Live is speaking | Replacement scheduled before current wake terminates |
| A08 | Failed Snooze replacement | Current alarm remains audible and actionable |
| A09 | Wake UI hidden or service recreated | Critical alarm independent and restored |
| A10 | Signed APK upgrade with 2 configured alarms | Both retained with correct next occurrences |
| A11 | Call or VoIP session present | Voice does not hijack call route |
| A12 | Bluetooth attach/remove mid-conversation | Lost voice route closes safely, local alarm continues |
| A13 | Wi-Fi to mobile handoff / total connection loss | Bounded failure, no second potentially paid Live |
| A14 | Full-screen/notification permissions withdrawn | Honest repair state and accessible controls |
| A15 | Actual overnight use across 7-14 days | No missed or falsely successful critical alarms |

Capture the in-app Wake Lab reliability report. *Decoded remote PCM is not proof of speaker audibility.*

## Voice scenario pack

Run all of the following separately in **English, German and Hebrew**, for at least two characters selected for the pilot. Rate naturalness (1–5), irritation (1–5), character continuity (yes/no), duplicated instructions (count), unexpected claims about app actions (count), and whether the real person heard the entire utterance.

| ID | Human behavior | Target |
|---|---|---|
| V01 | Silent morning; drowsy | One gentle invitation, no forced verbal proof |
| V02 | “Five more minutes” | No false Snooze confirmation |
| V03 | “Already up” | Acknowledge without inventing motion |
| V04 | Swearing or irritated complaint | Natural response, never shame or command-loop |
| V05 | Barge in twice mid-sentence | One character, no overlapping assistant voices |
| V06 | Pause/hesitation while assistant speaks | No premature completed-turn transition |
| V07 | Mumble then self-correct | Conservative engagement evidence |
| V08 | Dog, TV or another speaker | No invented human compliance or native action |
| V09 | Joke, unrelated chat or “please be quiet” | Natural acknowledgment without repetitive tasks |
| V10 | Ask it to change persona mid-wake | Selected identity remains stable |
| V11 | No calendar consent or revoked access | No invented appointment or lockscreen disclosure |
| V12 | Authorized calendar fact | Only verified, minimal orientation context |
| V13 | Demand “stop my alarm” by voice | Model never claims an authoritative action |
| V14 | Session expiry | Live disconnects, local alarm continues |

Targets for a sufficiently sized review sample: mean naturalness >=4/5, stable character in >=95% of judged sessions, zero model-authorized Stop/Snooze, zero critical-alarm regressions. **No scores have been measured yet.**

## Repeated-morning user outcome trial

Over 7–14 waking mornings, alternate traditional local-alarm-only and Live-enriched alarms when safe. Record genuinely out-of-bed within ten minutes (self-report), number of snoozes, sleep inertia, annoyance and helpfulness. Phone pickup and plausible speech are engagement signals, *not* biological wake verification. Do not optimize toward eliciting microphone responses if that raises annoyance.

## Provider usage and cost

As of the 2026-10-22 pricing check, GPT-Live 1 is USD $0.05 per active session minute, billed by the second; backend delegation is charged separately. See [OpenAI pricing](https://developers.openai.com/api/docs/pricing) and [Live cost optimization](https://developers.openai.com/api/docs/guides/voice-latency-cost). Verify the latest price again before rollout. Read final usage from `session.closed` and compare the OpenAI billing/usage dashboard. The local Wake Lab duration is an observational proxy, **not a bill or proof of audible speech**. Neon admission rows count reservations (including some failed provider attempts), not completed billable sessions.

Read-only aggregate admission query for an authorized operator (do not export account identifiers):

```sql
SELECT utc_day,
       COUNT(*) AS accounts_with_reservations,
       SUM(sessions_started) AS reserved_session_starts
FROM wmw_private.live_session_admissions
WHERE utc_day >= (NOW() AT TIME ZONE 'utc')::date - 14
GROUP BY utc_day ORDER BY utc_day DESC;
```

Before general enrollment, verify project spend alerts, per-account admission limits, actual provider usage and an explicit staged allowlist.

## Go / no-go

Require **all**: six automated workflows green at exact HEAD; signed in-place upgrade; completed physical cases without critical failures; human voice ratings/continuity recorded; adequate repeat-morning outcomes; backend, cost and privacy gates confirmed; a rollback plan. The release remains draft if any condition is unverified. The documented `Actions → Release WakeMyWay` workflow on current `main` is a separate manual publication step.
