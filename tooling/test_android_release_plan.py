import json
import tempfile
import unittest
from pathlib import Path

from android_release_plan import bump_version, extract_release_notes, write_manifest


class AndroidReleasePlanTest(unittest.TestCase):
    def test_patch_minor_major_bumps(self):
        self.assertEqual("0.2.5", bump_version("0.2.4", "patch"))
        self.assertEqual("0.3.0", bump_version("0.2.4", "minor"))
        self.assertEqual("1.0.0", bump_version("0.2.4", "major"))

    def test_invalid_semver_is_rejected(self):
        with self.assertRaises(ValueError):
            bump_version("v0.2.4", "patch")

    def test_release_notes_are_plain_and_bounded(self):
        markdown = """
## What's Changed

* [Widget polish](https://example.test/pr/1) by @yotam in https://example.test/pr/1
- Reliability hardening
"""
        self.assertEqual(
            ["Widget polish", "Reliability hardening"],
            extract_release_notes(markdown),
        )

    def test_plain_custom_notes_are_preserved(self):
        self.assertEqual(
            ["Fixes widget sizing"],
            extract_release_notes("## What's new\n\nFixes widget sizing\n"),
        )

    def test_manifest_contains_public_latest_url_and_sha(self):
        with tempfile.TemporaryDirectory() as tmp:
            output = Path(tmp) / "update.json"
            write_manifest(
                version_code=7,
                version_name="0.2.5",
                priority=1,
                repository="yotamon/WakeMyWay",
                apk_sha256="ABC123",
                published_at="2026-09-25T14:00:00Z",
                notes_markdown="- Better widget\n",
                output=output,
            )
            payload = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual(7, payload["versionCode"])
            self.assertEqual("0.2.5", payload["versionName"])
            self.assertEqual(["Better widget"], payload["releaseNotes"])
            self.assertEqual("abc123", payload["apk"]["sha256"])
            self.assertEqual(
                "https://github.com/yotamon/WakeMyWay/releases/latest/download/WakeMyWay-direct.apk",
                payload["apk"]["url"],
            )


if __name__ == "__main__":
    unittest.main()
