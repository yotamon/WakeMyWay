#!/usr/bin/env python3
"""Static privacy contracts for the Windows-only physical acceptance helper.

These checks intentionally do not execute PowerShell or use an Android device.
They prevent accidental expansion of the default evidence footprint; device
behavior and PowerShell syntax still require validation on a Windows host.
"""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "tooling" / "physical-reliability.ps1"
IGNORE = ROOT / ".gitignore"


class PhysicalAcceptancePrivacyContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.script = SCRIPT.read_text(encoding="utf-8")
        cls.collect = cls.script.split('    "collect" {', 1)[1]
        cls.wake_check = cls.script.split('    "wake-check" {', 1)[1].split(
            '    "collect" {', 1
        )[0]
        cls.safe_collect, cls.private_collect = cls.collect.split(
            "        if ($IncludePrivateDiagnostics) {", 1
        )
        cls.safe_wake, cls.private_wake = cls.wake_check.split(
            "        if ($IncludePrivateDiagnostics) {", 1
        )

    def test_real_evidence_folder_is_ignored(self) -> None:
        lines = IGNORE.read_text(encoding="utf-8").splitlines()
        self.assertIn("artifacts/physical-acceptance/", lines)
        self.assertIn("artifacts/physical-reliability/", lines)

    def test_private_diagnostics_are_explicit_opt_in(self) -> None:
        self.assertIn("[switch]$IncludePrivateDiagnostics", self.script)
        self.assertIn('Join-Path $root "private-local"', self.private_collect)
        self.assertIn('Join-Path $root "private-local"', self.private_wake)
        self.assertIn("Write-Warning", self.private_collect)
        self.assertIn("Write-Warning", self.private_wake)

    def test_default_collector_omits_cross_app_private_dumps(self) -> None:
        for forbidden in (
            "Invoke-Adb devices -l",
            "Invoke-Adb shell getprop",
            "Invoke-Adb logcat",
            "Invoke-Adb shell dumpsys notification",
            "Invoke-Adb shell dumpsys audio",
            "enabled_accessibility_services",
            "Copy-BenchmarkEvidence",
        ):
            with self.subTest(forbidden=forbidden):
                self.assertNotIn(forbidden, self.safe_collect)
        self.assertIn('Join-Path $root "package-version.txt"', self.safe_collect)
        self.assertIn('Join-Path $root "alarm-count.txt"', self.safe_collect)
        self.assertIn('Join-Path $root "animation-scales.json"', self.safe_collect)

    def test_default_active_wake_capture_filters_shared_dumps(self) -> None:
        for command in (
            "Invoke-Adb shell dumpsys power",
            "Invoke-Adb shell dumpsys audio",
            "Invoke-Adb shell dumpsys activity services",
        ):
            self.assertIn(command, self.safe_wake)
        self.assertGreaterEqual(self.safe_wake.count("Select-String -Pattern"), 3)
        self.assertNotIn("vibrator_manager", self.safe_wake)
        self.assertNotIn("Invoke-Adb logcat", self.safe_wake)

    def test_no_unredacted_notification_dump_even_opted_in(self) -> None:
        self.assertNotIn("Invoke-Adb shell dumpsys notification --noredact", self.script)
        self.assertIn("Invoke-Adb shell dumpsys notification", self.private_collect)

    def test_evidence_is_marked_local_and_not_auto_uploaded(self) -> None:
        self.assertIn('Join-Path $root "README-LOCAL.txt"', self.script)
        self.assertNotIn("Invoke-RestMethod", self.collect)
        self.assertNotIn("Invoke-WebRequest", self.collect)


if __name__ == "__main__":
    unittest.main()
