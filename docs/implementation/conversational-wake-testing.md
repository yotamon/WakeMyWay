# Conversational Wake founder dogfood checklist

Use only a debug founder build with explicit cloud-audio consent.

1. Confirm normal Wake preflight is Ready.
2. Configure the founder Realtime broker once in the debug lab.
3. Schedule the production-path T+2m wake and lock the phone.
4. Verify critical alarm audio starts independently of Realtime connection.
5. Verify WakeActivity appears with local Stop/Snooze.
6. Verify Alfred can complete at least two natural conversational turns with the user.
7. Interrupt Alfred mid-response and verify barge-in does not deadlock the session.
8. Disable connectivity during a later turn and verify the same WakeRuntime intent falls back to local Alfred.
9. Restore connectivity only after the wake; no active session should depend on reconnect for completion.
10. Verify the wake completes only after WakeRuntime activation evidence reaches policy requirements.
11. Verify Stop and Snooze terminate immediately regardless of Realtime state.
12. Verify audio routing/mode returns to normal after terminal action.

Do not treat emulator success as proof of physical audio-route, lock-screen, Bluetooth, VAD, or OEM behavior.