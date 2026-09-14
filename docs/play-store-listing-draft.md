# Google Play Store listing draft

**Status:** Draft for the first Android public listing. Final copy must match the exact features enabled in the production build submitted to Play.

## App name

**Wake My Way**

## Short description

**An alarm that learns what helps you actually get up.**

## Full description

Most alarms are good at one thing: making noise at a specific time.

Wake My Way is built for the harder part: helping you move from half-awake to actually starting your morning.

Set the time you want to wake up, tell Wake My Way what matters tomorrow, and let your wake routine guide you through the first minutes of the day. Instead of treating every morning the same, Wake My Way adapts its wake strategy around your responses, movement, snoozing, and what has helped you get going before.

### More than a ringtone

Wake My Way combines a reliable native Android alarm with spoken guidance and simple actions designed to help you transition out of sleep inertia.

Depending on what you need, a wake session can use conversation, movement prompts, reminders of what matters that morning, and controlled snoozing rather than simply repeating the same alarm sound.

### Built around your morning

Before bed, you can prepare your next morning and capture the thing you want to remember when the alarm goes off. Wake My Way keeps the critical alarm path local so the alarm itself does not depend on an internet connection.

### Learns with you

Wake My Way uses your previous wake outcomes to adjust the strategy it chooses over time. The goal is not to make waking up harder. It is to use the minimum amount of friction that actually helps you get moving.

### Designed for reliability

Wake My Way uses Android's native alarm capabilities, foreground alarm playback, lock-screen presentation, reboot recovery, and local fallback behavior. If optional voice capabilities are unavailable, the alarm remains the authority.

### Privacy-first by default

The first local-first release stores wake settings, morning intentions, and learning state on your device. Wake My Way does not keep an archive of raw microphone recordings. Base use does not require an account.

### Made for people who mean it when they set an alarm

Wake My Way is especially useful if you:

- snooze repeatedly even when you genuinely want to get up
- dismiss alarms while still half asleep
- respond better when someone actually talks to you
- want your alarm to help you start the morning, not just make a sound

**Wake up your way.**

## Category recommendation

Primary candidate: **Lifestyle**

Re-evaluate the current Play category set at submission time. Do not use a medical/health category or medical claims unless the product scope and compliance posture intentionally change.

## Screenshot story

The first screenshot set should tell one coherent product story rather than showing unrelated screens.

| Order | Screen / state | Suggested headline | What it proves |
|---|---|---|---|
| 1 | Tonight / next wake overview | **Wake up your way.** | Immediate category + brand promise |
| 2 | Wake setup | **Set the time. Shape the morning.** | It is a real configurable alarm, not a demo |
| 3 | Tomorrow Contract / preparation | **Give tomorrow morning a reason.** | Differentiated night-before preparation |
| 4 | Active wake conversation | **An alarm that talks you into the day.** | Conversational wake experience |
| 5 | Movement / activation state | **Less snoozing. More getting up.** | Behavior-oriented wake strategy |
| 6 | Completion / morning orientation | **Start with one clear next move.** | The product bridges wake → morning action |

Only include a state if it exists and is stable in the exact production build being listed.

## Store asset checklist

- 512 × 512 high-resolution Play icon
- 1024 × 500 feature graphic
- phone screenshots captured from the release/Play-installed build
- final in-app launcher/adaptive icon matching the brand system
- no debug labels, founder controls, infrastructure wording, placeholder art, or development-only realtime UI in store screenshots

## Copy guardrails

Do not claim:

- "never miss an alarm"
- "guaranteed to wake you up"
- delivery while the device is powered off
- delivery after Android Force Stop
- delivery after permissions/capabilities required by Android are revoked
- diagnosis or treatment of ADHD, depression, sleep disorders, or other medical conditions
- cloud/AI capabilities that are not enabled in the submitted production build

Prefer:

- "designed for reliable alarms"
- "helps you move from waking up to starting your morning"
- "learns what wake strategies work for you"
- "critical alarm behavior stays local"

## First-release notes draft

**Wake My Way 0.1.0**

The first Wake My Way release introduces the local-first wake system: reliable Android alarms, wake preparation, spoken wake guidance, movement-aware activation, snooze/stop controls, and local wake-strategy learning.

This is the beginning of Wake My Way's public testing phase. Reliability comes first, and the alarm remains designed to work without depending on cloud services.
