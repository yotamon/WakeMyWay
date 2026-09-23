param(
    [Parameter(Mandatory = $false)]
    [ValidateSet("status", "force-idle", "unidle", "kill-process", "reboot", "collect")]
    [string]$Action = "status",

    [Parameter(Mandatory = $false)]
    [string]$Serial
)

$ErrorActionPreference = "Stop"
$PackageName = "com.wakemyway.app"

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

function Assert-Device {
    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        throw "adb is not available on PATH. Install Android platform-tools first."
    }

    $state = (Invoke-Adb get-state 2>$null | Select-Object -Last 1).Trim()
    if ($state -ne "device") {
        throw "No authorized Android device is ready. Current adb state: $state"
    }
}

function Get-DeviceSummary {
    $manufacturer = (Invoke-Adb shell getprop ro.product.manufacturer | Select-Object -Last 1).Trim()
    $model = (Invoke-Adb shell getprop ro.product.model | Select-Object -Last 1).Trim()
    $sdk = (Invoke-Adb shell getprop ro.build.version.sdk | Select-Object -Last 1).Trim()
    $release = (Invoke-Adb shell getprop ro.build.version.release | Select-Object -Last 1).Trim()
    "$manufacturer $model · Android $release · API $sdk"
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
        Invoke-Adb shell am kill $PackageName
    }

    "reboot" {
        Write-Host "Rebooting the device. For DIRECT_BOOT, do not unlock before the scheduled wake."
        Invoke-Adb reboot
    }

    "collect" {
        $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
        $root = Join-Path (Get-Location) "artifacts/physical-reliability/$stamp"
        New-Item -ItemType Directory -Force -Path $root | Out-Null

        "device=$(Get-DeviceSummary)" | Set-Content -Encoding UTF8 (Join-Path $root "device.txt")
        Invoke-Adb devices -l | Out-File -Encoding UTF8 (Join-Path $root "adb-devices.txt")
        Invoke-Adb shell dumpsys deviceidle | Out-File -Encoding UTF8 (Join-Path $root "deviceidle.txt")
        Invoke-Adb shell dumpsys alarm | Out-File -Encoding UTF8 (Join-Path $root "alarm.txt")
        Invoke-Adb shell dumpsys package $PackageName | Out-File -Encoding UTF8 (Join-Path $root "package.txt")
        Invoke-Adb shell dumpsys notification --noredact | Out-File -Encoding UTF8 (Join-Path $root "notification.txt")
        Invoke-Adb logcat -d -t 4000 | Out-File -Encoding UTF8 (Join-Path $root "logcat.txt")

        Write-Host "Evidence collected to $root"
        Write-Host "Also export the in-app Wake Alarm Lab reliability report for the same scenario."
    }
}
