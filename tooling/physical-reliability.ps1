param(
    [Parameter(Mandatory = $false)]
    [ValidateSet(
        "status",
        "preflight",
        "semantics",
        "benchmark",
        "animations-off",
        "animations-restore",
        "talkback-status",
        "force-idle",
        "unidle",
        "kill-process",
        "reboot",
        "wake-check",
        "collect"
    )]
    [string]$Action = "status",

    [Parameter(Mandatory = $false)]
    [string]$Serial,

    # Explicit, LOCAL-ONLY diagnostic expansion. Never share without reviewing content.
    [Parameter(Mandatory = $false)]
    [switch]$IncludePrivateDiagnostics
)

$ErrorActionPreference = "Stop"
$PackageName = "com.wakemyway.app"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$AndroidDir = Join-Path $RepoRoot "apps/android"
$AcceptanceRoot = Join-Path $RepoRoot "artifacts/physical-acceptance"
$AnimationStateFile = Join-Path $AcceptanceRoot ".animation-scales.json"

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    $prefix = @()
    if ($Serial) {
        $prefix += @("-s", $Serial)
    }

    & adb @prefix @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: adb $($prefix -join ' ') $($Arguments -join ' ')"
    }
}

function Invoke-Gradle {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    $gradle = Join-Path $AndroidDir "gradlew.bat"
    if (-not (Test-Path $gradle)) {
        $gradle = Join-Path $AndroidDir "gradlew"
    }
    if (-not (Test-Path $gradle)) {
        throw "Gradle wrapper not found under $AndroidDir"
    }

    Push-Location $AndroidDir
    try {
        & $gradle @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle failed: $($Arguments -join ' ')"
        }
    } finally {
        Pop-Location
    }
}

function Assert-Device {
    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        throw "adb is not available on PATH. Install Android platform-tools first."
    }

    $state = (Invoke-Adb get-state 2>$null | Select-Object -Last 1).Trim()
    if ($state -ne "device") {
        throw "No authorized Android device is ready. Current adb state: $state"
    }
}

function Get-PackagePids {
    $prefix = @()
    if ($Serial) {
        $prefix += @("-s", $Serial)
    }

    $output = & adb @prefix shell pidof $PackageName 2>$null
    if ($LASTEXITCODE -notin @(0, 1)) {
        throw "adb pidof failed with exit code $LASTEXITCODE"
    }

    (($output -join " ").Trim())
}

function Get-DeviceSummary {
    $manufacturer = (Invoke-Adb shell getprop ro.product.manufacturer | Select-Object -Last 1).Trim()
    $model = (Invoke-Adb shell getprop ro.product.model | Select-Object -Last 1).Trim()
    $sdk = (Invoke-Adb shell getprop ro.build.version.sdk | Select-Object -Last 1).Trim()
    $release = (Invoke-Adb shell getprop ro.build.version.release | Select-Object -Last 1).Trim()
    "$manufacturer $model · Android $release · API $sdk"
}

function Get-AnimationScales {
    [ordered]@{
        window_animation_scale = (Invoke-Adb shell settings get global window_animation_scale | Select-Object -Last 1).Trim()
        transition_animation_scale = (Invoke-Adb shell settings get global transition_animation_scale | Select-Object -Last 1).Trim()
        animator_duration_scale = (Invoke-Adb shell settings get global animator_duration_scale | Select-Object -Last 1).Trim()
    }
}

function Write-TalkBackStatus {
    Write-Host "Accessibility enabled:"
    Invoke-Adb shell settings get secure accessibility_enabled
    Write-Host "Enabled accessibility services:"
    Invoke-Adb shell settings get secure enabled_accessibility_services
}

function New-EvidenceDirectory {
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $root = Join-Path $AcceptanceRoot $stamp
    New-Item -ItemType Directory -Force -Path $root | Out-Null
    @(
        "LOCAL WakeMyWay physical acceptance evidence. Nothing is uploaded by this script.",
        "Inspect and redact any file before sharing; never attach private-local files to a public issue.",
        "The in-app Wake Lab semantic report is separate and must be exported deliberately."
    ) | Set-Content -Encoding UTF8 (Join-Path $root "README-LOCAL.txt")
    $root
}

function Copy-BenchmarkEvidence {
    param([Parameter(Mandatory = $true)][string]$Destination)

    $buildRoot = Join-Path $AndroidDir "benchmark/build"
    if (-not (Test-Path $buildRoot)) {
        return
    }

    $candidates = Get-ChildItem -Path $buildRoot -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Extension -in @(".json", ".trace", ".perfetto-trace") -or
            $_.Name -match "benchmark|macrobenchmark"
        }

    if ($candidates) {
        $target = Join-Path $Destination "benchmark"
        New-Item -ItemType Directory -Force -Path $target | Out-Null
        foreach ($candidate in $candidates) {
            Copy-Item $candidate.FullName -Destination $target -Force
        }
    }
}

Assert-Device

switch ($Action) {
    "status" {
        Write-Host "WakeMyWay physical reliability helper"
        Write-Host "Device: $(Get-DeviceSummary)"
        Write-Host ""
        Invoke-Adb devices -l
        Write-Host ""
        Invoke-Adb shell dumpsys deviceidle
        Write-Host ""
        Invoke-Adb shell dumpsys package $PackageName | Select-String -Pattern "versionName|versionCode|firstInstallTime|lastUpdateTime"
    }

    "preflight" {
        Write-Host "WakeMyWay 1.0 physical acceptance preflight"
        Write-Host "Device: $(Get-DeviceSummary)"
        Write-Host "Repository: $RepoRoot"
        $commit = (& git -C $RepoRoot rev-parse HEAD).Trim()
        Write-Host "Commit: $commit"
        Write-Host ""
        Write-Host "Animation scales:"
        Get-AnimationScales | Format-List
        Write-Host ""
        Write-TalkBackStatus
        Write-Host ""
        Write-Host "ADB device:"
        Invoke-Adb devices -l
    }

    "semantics" {
        Write-Host "Running TalkBack-critical Compose semantics contract on the physical device."
        $gradleArgs = @(
            ":app:connectedDirectDebugAndroidTest",
            "-Pandroid.testInstrumentationRunnerArguments.class=com.wakemyway.app.AccessibilitySemanticsInstrumentedTest",
            "--stacktrace"
        )
        Invoke-Gradle @gradleArgs
    }

    "benchmark" {
        Write-Host "Running WakeMyWay cold-start Macrobenchmark on the physical device."
        Write-Host "Keep the device thermally stable and do not interact with it until the run completes."
        $gradleArgs = @(
            ":benchmark:connectedPlayBenchmarkAndroidTest",
            "-Pandroid.testInstrumentationRunnerArguments.class=com.wakemyway.benchmark.StartupBenchmark",
            "--stacktrace"
        )
        Invoke-Gradle @gradleArgs

        $root = New-EvidenceDirectory
        Copy-BenchmarkEvidence -Destination $root
        "device=$(Get-DeviceSummary)" | Set-Content -Encoding UTF8 (Join-Path $root "device.txt")
        (& git -C $RepoRoot rev-parse HEAD).Trim() | Set-Content -Encoding UTF8 (Join-Path $root "commit.txt")
        Write-Host "Benchmark evidence copied to $root"
    }

    "animations-off" {
        New-Item -ItemType Directory -Force -Path $AcceptanceRoot | Out-Null
        $current = Get-AnimationScales
        $current | ConvertTo-Json | Set-Content -Encoding UTF8 $AnimationStateFile

        Invoke-Adb shell settings put global window_animation_scale 0
        Invoke-Adb shell settings put global transition_animation_scale 0
        Invoke-Adb shell settings put global animator_duration_scale 0

        Write-Host "System animation scales are now 0 for the reduced-motion acceptance pass."
        Write-Host "Original values were saved to $AnimationStateFile"
        Get-AnimationScales | Format-List
    }

    "animations-restore" {
        if (-not (Test-Path $AnimationStateFile)) {
            throw "No saved animation-scale state exists at $AnimationStateFile. Refusing to guess previous values."
        }

        $saved = Get-Content $AnimationStateFile -Raw | ConvertFrom-Json
        Invoke-Adb shell settings put global window_animation_scale $saved.window_animation_scale
        Invoke-Adb shell settings put global transition_animation_scale $saved.transition_animation_scale
        Invoke-Adb shell settings put global animator_duration_scale $saved.animator_duration_scale
        Remove-Item $AnimationStateFile -Force

        Write-Host "Original system animation scales restored."
        Get-AnimationScales | Format-List
    }

    "talkback-status" {
        Write-Host "Device: $(Get-DeviceSummary)"
        Write-TalkBackStatus
    }

    "force-idle" {
        Write-Host "Forcing device idle/Doze. Do this only after the Wake Lab scenario is scheduled."
        Invoke-Adb shell dumpsys battery unplug
        Invoke-Adb shell dumpsys deviceidle force-idle
        Write-Host "Device is now forced idle. Keep it locked until the wake target."
    }

    "unidle" {
        Write-Host "Restoring normal idle/battery simulation state."
        Invoke-Adb shell dumpsys deviceidle unforce
        Invoke-Adb shell dumpsys battery reset
    }

    "kill-process" {
        Write-Host "Killing the WakeMyWay process without Force Stop."
        Write-Host "Use this only for SERVICE_RECREATION or post-STOP resurrection evidence."
        $before = Get-PackagePids
        if (-not $before) {
            Write-Host "WakeMyWay has no running process."
            break
        }

        Invoke-Adb shell am kill $PackageName
        Start-Sleep -Milliseconds 750
        $afterAmKill = Get-PackagePids
        $originalPids = $before -split "\s+" | Where-Object { $_ }

        if ($originalPids | Where-Object { $afterAmKill -split "\s+" -contains $_ }) {
            Write-Host "Android kept the protected process alive; sending SIGKILL via run-as."
            foreach ($pidValue in $originalPids) {
                Invoke-Adb shell run-as $PackageName kill -9 $pidValue
            }
            Start-Sleep -Milliseconds 750
        }

        $after = Get-PackagePids
        $survivors = $originalPids | Where-Object { $after -split "\s+" -contains $_ }
        if ($survivors) {
            throw "Original WakeMyWay process survived: $($survivors -join ', ')"
        }
        Write-Host "Original PID(s) $before terminated without Force Stop. Current PID(s): $after"
        Write-Host "If RECOVERY_GUARD evidence is intended, the OS recovery alarm must now re-kick playback."
    }

    "reboot" {
        Write-Host "Rebooting the device. For DIRECT_BOOT, do not unlock before the scheduled wake."
        Invoke-Adb reboot
    }

    "wake-check" {
        Write-Host "Capturing Active Wake execution evidence. Run this WHILE a wake is ringing."
        Write-Host "Device: $(Get-DeviceSummary)"

        $pids = Get-PackagePids
        if (-not $pids) {
            throw "WakeMyWay has no running process. Start an active wake first (Wake Lab scenario or a real alarm)."
        }
        Write-Host "WakeMyWay process alive: PID(s) $pids"

        $root = New-EvidenceDirectory
        "device=$(Get-DeviceSummary)" | Set-Content -Encoding UTF8 (Join-Path $root "device.txt")
        (& git -C $RepoRoot rev-parse HEAD).Trim() | Set-Content -Encoding UTF8 (Join-Path $root "commit.txt")

        # Default exports include only app-scoped diagnostics. Full Android
        # service dumps can contain unrelated app/user data, even when WakeMyWay
        # is the only app being investigated.
        Invoke-Adb shell dumpsys activity services $PackageName |
            Select-String -Pattern "com\\.wakemyway|activeWake" |
            ForEach-Object { $_.Line } |
            Out-File -Encoding UTF8 (Join-Path $root "wake-check-services.txt")
        Invoke-Adb shell dumpsys power |
            Select-String -Pattern "activeWakeStartup|activeWakeTone|wakemyway" |
            ForEach-Object { $_.Line } |
            Out-File -Encoding UTF8 (Join-Path $root "wake-check-power.txt")
        Invoke-Adb shell dumpsys audio |
            Select-String -Pattern "wakemyway" |
            ForEach-Object { $_.Line } |
            Out-File -Encoding UTF8 (Join-Path $root "wake-check-audio.txt")

        if ($IncludePrivateDiagnostics) {
            Write-Warning "Private local diagnostics may include other apps, user data and identifiers. Review before sharing."
            $privateRoot = Join-Path $root "private-local"
            New-Item -ItemType Directory -Force -Path $privateRoot | Out-Null
            Invoke-Adb shell dumpsys activity services $PackageName |
                Out-File -Encoding UTF8 (Join-Path $privateRoot "services.txt")
            Invoke-Adb shell dumpsys power |
                Out-File -Encoding UTF8 (Join-Path $privateRoot "power.txt")
            Invoke-Adb shell dumpsys audio |
                Out-File -Encoding UTF8 (Join-Path $privateRoot "audio.txt")
            try {
                Invoke-Adb shell dumpsys vibrator_manager |
                    Out-File -Encoding UTF8 (Join-Path $privateRoot "vibrator.txt")
            } catch {
                Invoke-Adb shell dumpsys vibrator |
                    Out-File -Encoding UTF8 (Join-Path $privateRoot "vibrator.txt")
            }
        }

        Write-Host ""
        Write-Host "Wake locks mentioning wakemyway (expect activeWakeStartup in the fire-to-audio window, activeWakeTone while the emergency tone plays):"
        (Invoke-Adb shell dumpsys power) |
            Select-String -Pattern "wakemyway" |
            ForEach-Object { "  $($_.Line.Trim())" }
        Write-Host ""
        Write-Host "Audio focus / stream entries mentioning wakemyway (expect USAGE_ALARM ownership):"
        (Invoke-Adb shell dumpsys audio) |
            Select-String -Pattern "wakemyway" |
            ForEach-Object { "  $($_.Line.Trim())" }
        Write-Host ""
        Write-Host "Human checks for the hardened fallback layers:"
        Write-Host "  1. The repeating haptic pattern is felt, including with the alarm stream muted."
        Write-Host "  2. Notification Stop ends the wake and the vibration stops with it."
        Write-Host "  3. Snooze schedules the replacement before the current wake ends (see the Lab report)."
        Write-Host ""
        Write-Host "Wake-check evidence collected to $root"
        Write-Host "Also export the in-app Wake Alarm Lab reliability report for the same scenario."
    }

    "collect" {
        $root = New-EvidenceDirectory

        "device=$(Get-DeviceSummary)" | Set-Content -Encoding UTF8 (Join-Path $root "device.txt")
        (& git -C $RepoRoot rev-parse HEAD).Trim() | Set-Content -Encoding UTF8 (Join-Path $root "commit.txt")
        # Safe-by-default support evidence. No device serial, whole-device
        # properties, raw notification content, cross-app audio dumps or logcat.
        Invoke-Adb shell wm size | Out-File -Encoding UTF8 (Join-Path $root "display-size.txt")
        Invoke-Adb shell wm density | Out-File -Encoding UTF8 (Join-Path $root "display-density.txt")
        Invoke-Adb shell dumpsys package $PackageName |
            Select-String -Pattern "^\\s*(versionName|versionCode|firstInstallTime|lastUpdateTime)=" |
            ForEach-Object { $_.Line.Trim() } |
            Out-File -Encoding UTF8 (Join-Path $root "package-version.txt")
        Invoke-Adb shell dumpsys deviceidle |
            Select-String -Pattern "^\\s*m(State|LightState|ForceIdle|ScreenOn|Charging)=" |
            ForEach-Object { $_.Line.Trim() } |
            Out-File -Encoding UTF8 (Join-Path $root "deviceidle-summary.txt")
        $wakeAlarmLineCount = @(Invoke-Adb shell dumpsys alarm |
            Select-String -SimpleMatch $PackageName).Count
        "wakeMyWayAlarmLines=$wakeAlarmLineCount" |
            Set-Content -Encoding UTF8 (Join-Path $root "alarm-count.txt")
        Invoke-Adb shell settings get secure accessibility_enabled |
            Out-File -Encoding UTF8 (Join-Path $root "accessibility-enabled.txt")
        Get-AnimationScales | ConvertTo-Json |
            Set-Content -Encoding UTF8 (Join-Path $root "animation-scales.json")

        if ($IncludePrivateDiagnostics) {
            Write-Warning "Private local diagnostics may include logs, other apps, notification details and identifiers. Never share unreviewed."
            $privateRoot = Join-Path $root "private-local"
            New-Item -ItemType Directory -Force -Path $privateRoot | Out-Null
            Invoke-Adb devices -l | Out-File -Encoding UTF8 (Join-Path $privateRoot "adb-devices.txt")
            Invoke-Adb shell getprop | Out-File -Encoding UTF8 (Join-Path $privateRoot "getprop.txt")
            Invoke-Adb shell dumpsys deviceidle | Out-File -Encoding UTF8 (Join-Path $privateRoot "deviceidle.txt")
            Invoke-Adb shell dumpsys alarm | Out-File -Encoding UTF8 (Join-Path $privateRoot "alarm.txt")
            Invoke-Adb shell dumpsys package $PackageName | Out-File -Encoding UTF8 (Join-Path $privateRoot "package.txt")
            # Even normal notification dumps can include private text; do not use --noredact.
            Invoke-Adb shell dumpsys notification | Out-File -Encoding UTF8 (Join-Path $privateRoot "notification.txt")
            Invoke-Adb shell dumpsys audio | Out-File -Encoding UTF8 (Join-Path $privateRoot "audio.txt")
            Invoke-Adb shell settings get secure enabled_accessibility_services |
                Out-File -Encoding UTF8 (Join-Path $privateRoot "accessibility-services.txt")
            Invoke-Adb logcat -d -t 4000 | Out-File -Encoding UTF8 (Join-Path $privateRoot "logcat.txt")
            Copy-BenchmarkEvidence -Destination $privateRoot
        }

        Write-Host "Evidence collected to $root"
        Write-Host "Also export the in-app Wake Alarm Lab reliability report for the same scenario."
    }
}
