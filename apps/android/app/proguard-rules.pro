# Wake My Way release shrinker rules.
#
# Keep this file intentionally small. AndroidX/Compose/Kotlin dependencies ship their own
# consumer rules; broad -keep rules would hide release-only problems and reduce R8 value.

# WorkManager persists worker class names and may recreate workers reflectively after an app
# restart/update. Keep worker class names and their standard constructor stable.
-keepnames class * extends androidx.work.ListenableWorker
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
