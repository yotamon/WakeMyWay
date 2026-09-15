#!/usr/bin/env python3

from __future__ import annotations

import json
from pathlib import Path
import sys
import tempfile
import unittest
import wave

sys.path.insert(0, str(Path(__file__).resolve().parent))

from wake_sound_assets import ASSETS, AssetError, check_assets, import_assets, manifest_path, raw_dir


def write_test_wav(path: Path, frames: int = 4_410) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as handle:
        handle.setnchannels(2)
        handle.setsampwidth(2)
        handle.setframerate(44_100)
        handle.writeframes(b"\x00\x00\x00\x00" * frames)


class WakeSoundAssetGuardTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name) / "android"
        self.source = Path(self.temp.name) / "approved"
        self.source.mkdir(parents=True)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def populate_approved_sources(self) -> None:
        for spec in ASSETS:
            write_test_wav(self.source / spec.source_filename)

    def test_empty_bundle_is_valid_development_state(self) -> None:
        message = check_assets(self.root)
        self.assertIn("not bundled", message)

    def test_partial_bundle_is_rejected(self) -> None:
        write_test_wav(raw_dir(self.root) / ASSETS[0].resource_filename)

        with self.assertRaisesRegex(AssetError, "Partial branded wake-sound bundle"):
            check_assets(self.root)

    def test_import_creates_complete_verified_bundle_and_manifest(self) -> None:
        self.populate_approved_sources()

        import_assets(self.source, self.root)

        self.assertIn("Verified all three", check_assets(self.root))
        manifest = json.loads(manifest_path(self.root).read_text(encoding="utf-8"))
        self.assertEqual(1, manifest["schema"])
        self.assertEqual({spec.sound_id for spec in ASSETS}, {entry["sound_id"] for entry in manifest["assets"]})
        for spec in ASSETS:
            self.assertTrue((raw_dir(self.root) / spec.resource_filename).is_file())

    def test_tampered_bundled_wav_is_rejected(self) -> None:
        self.populate_approved_sources()
        import_assets(self.source, self.root)
        target = raw_dir(self.root) / ASSETS[1].resource_filename
        target.write_bytes(target.read_bytes() + b"tamper")

        with self.assertRaisesRegex(AssetError, "manifest sha256 does not match"):
            check_assets(self.root)

    def test_import_rejects_missing_source_file_before_writing_resources(self) -> None:
        for spec in ASSETS[:2]:
            write_test_wav(self.source / spec.source_filename)

        with self.assertRaisesRegex(AssetError, "Missing approved source file"):
            import_assets(self.source, self.root)

        self.assertFalse(raw_dir(self.root).exists())
        self.assertFalse(manifest_path(self.root).exists())

    def test_import_rejects_non_wav_source_before_writing_resources(self) -> None:
        self.populate_approved_sources()
        (self.source / ASSETS[2].source_filename).write_bytes(b"not a wav")

        with self.assertRaisesRegex(AssetError, "WAV is too small|expected a RIFF/WAVE file"):
            import_assets(self.source, self.root)

        self.assertFalse(raw_dir(self.root).exists())
        self.assertFalse(manifest_path(self.root).exists())


if __name__ == "__main__":
    unittest.main()
