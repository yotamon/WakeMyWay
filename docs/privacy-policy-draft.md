# Wake My Way Privacy Policy (draft)

**Status:** Draft for the first local-first Android release. Publish at a stable public URL and perform a final legal/product review before using it as the Google Play privacy-policy URL.

**Last updated:** September 14, 2026

Wake My Way is designed to help you wake up using alarms, voice interaction, movement, and personalized wake routines. We design the app to keep private morning information on your device whenever possible.

## Information stored on your device

Wake My Way may store information locally on your device such as:

- your wake schedule and alarm settings
- your Tomorrow Contract or other morning intentions you enter
- prepared wake-plan content
- local wake history and derived wake-strategy information
- application preferences and reliability state needed to restore scheduled alarms

The first local-first Android release does not require an account for base use.

## Microphone and voice interaction

Wake My Way requests microphone access only for features that listen for your spoken responses during an interactive wake session or other voice-input flow.

Wake My Way does not intentionally create or retain an archive of raw microphone recordings.

Speech recognition is provided through the speech-recognition service installed on your Android device. Depending on your device, recognition provider, and settings, speech may be processed on the device or by that provider. Processing performed by your device's recognition provider is governed by that provider's terms and privacy practices.

## Text-to-Speech

Wake My Way may use the Text-to-Speech service installed on your Android device to speak wake prompts. The available voices and their processing behavior depend on the Text-to-Speech provider configured on your device.

## Network and cloud services

The first local-first production Android release is designed to operate without an Internet permission and does not directly send Wake My Way application data to Wake My Way servers.

Experimental developer-only realtime/cloud features are excluded from the production release build.

If a future public version introduces optional cloud, synchronization, AI, analytics, crash reporting, or account features, this policy and the Google Play Data Safety disclosure must be updated before that version is released.

## Alarms and notifications

Wake My Way uses Android alarm, notification, foreground-service, reboot, and full-screen presentation capabilities to deliver alarms and maintain active wake sessions. These capabilities are used for the core alarm experience, not for advertising or unrelated background activity.

## Backups

The production Android application disables Android application backup for Wake My Way private local application state.

## Advertising and tracking

The first local-first release does not include advertising SDKs or third-party behavioral advertising trackers.

## Data sharing

For the current local-first release, Wake My Way does not intentionally share locally stored Wake My Way application data with Wake My Way servers or advertising partners.

Device-provided services such as speech recognition and Text-to-Speech may process information according to the provider selected on your device, as described above.

## Data retention and deletion

Wake My Way retains local information for as long as needed to provide the relevant app feature or until it is removed through app controls, application-data clearing, or uninstalling the app, subject to Android platform behavior.

Before public release, Wake My Way will provide an appropriate in-app reset path for private local wake data. Future account/cloud features, if introduced, will define separate export, retention, and deletion controls.

## Security

Wake My Way is designed so that critical alarm state is kept separate from more private wake content. Private user content should not be placed in Android device-protected storage used for pre-unlock alarm recovery.

The production app disallows cleartext network traffic by default and does not embed third-party provider secret keys in the APK.

## Children

Wake My Way is not currently designed or marketed specifically for children. The final Google Play target-audience declaration must remain consistent with the released product and store listing.

## Changes to this policy

If Wake My Way changes how public releases collect, process, store, or share information, this policy will be updated before or alongside the relevant release.

## Contact

A public privacy/support contact must be added here before this draft is published or used in Google Play.
