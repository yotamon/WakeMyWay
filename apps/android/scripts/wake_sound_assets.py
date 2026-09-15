#!/usr/bin/env python3
"""Import and verify the approved WakeMyWay branded wake-sound assets.

The product intentionally keeps the sound picker hidden until all three approved WAV files are
bundled. This tool makes that boundary explicit: zero branded assets is a valid development state,
but any partial, malformed, or checksum-drifted bundled state fails verification.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from dataclasses import asdict, dataclass
from pathlib import Path
import shutil
import struct
import sys
from typing import Iterable


@dataclass(frozen=True)
class AssetSpec:
    sound_id: str
    source_filename: str
    resource_filename: str


@dataclass(frozen=True)
class WavInfo:
    audio_format: int
    channels: int
    sample_rate_hz: int
    bits_per_sample: int
    data_bytes: int
    duration_ms: int


ASSETS: tuple[AssetSpec, ...] = (
    AssetSpec("morning-light", "Morning Light.wav", "morning_light.wav"),
    AssetSpec("soft-start", "Soft Start.wav", "soft_start.wav"),
    AssetSpec("morning-pulse", "Morning Pulse.wav", "morning_pulse.wav"),
)

MANIFEST_SCHEMA = 1
DEFAULT_ANDROID_ROOT = Path(__file__).resolve().parents[1]
ALLOWED_WAV_FORMATS = {1, 3, 0xFFFE}  # PCM, IEEE float, WAVE_FORMAT_EXTENSIBLE.


class AssetError(RuntimeError):
    pass


def raw_dir(android_root: Path) -> Path:
    return android_root / "app" / "src" / "main" / "res" / "raw"


def manifest_path(android_root: Path) -> Path:
    return android_root / "app" / "src" / "main" / "wake-sound-assets.json"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def inspect_wav(path: Path) -> WavInfo:
    file_size = path.stat().st_size
    if file_size < 44:
        raise AssetError(f"{path.name}: WAV is too small ({file_size} bytes)")

    with path.open("rb") as handle:
        header = handle.read(12)
        if len(header) != 12 or header[:4] != b"RIFF" or header[8:12] != b"WAVE":
            raise AssetError(f"{path.name}: expected a RIFF/WAVE file")

        fmt: tuple[int, int, int, int, int, int] | None = None
        data_bytes = 0

        while handle.tell() + 8 <= file_size:
            chunk_header = handle.read(8)
            if len(chunk_header) != 8:
                break
            chunk_id = chunk_header[:4]
            chunk_size = struct.unpack("<I", chunk_header[4:])[0]
            payload_start = handle.tell()
            next_chunk = payload_start + chunk_size + (chunk_size & 1)
            if next_chunk > file_size:
                raise AssetError(f"{path.name}: truncated {chunk_id!r} WAV chunk")

            if chunk_id == b"fmt ":
                if chunk_size < 16:
                    raise AssetError(f"{path.name}: invalid fmt WAV chunk")
                payload = handle.read(16)
                fmt = struct.unpack("<HHIIHH", payload)
            elif chunk_id == b"data":
                data_bytes += chunk_size

            handle.seek(next_chunk)

    if fmt is None:
        raise AssetError(f"{path.name}: missing fmt WAV chunk")
    if data_bytes <= 0:
        raise AssetError(f"{path.name}: missing or empty data WAV chunk")

    audio_format, channels, sample_rate, byte_rate, block_align, bits_per_sample = fmt
    if audio_format not in ALLOWED_WAV_FORMATS:
        raise AssetError(
            f"{path.name}: unsupported WAV encoding {audio_format}; expected PCM, IEEE float, or extensible"
        )
    if not 1 <= channels <= 8:
        raise AssetError(f"{path.name}: invalid channel count {channels}")
    if not 8_000 <= sample_rate <= 384_000:
        raise AssetError(f"{path.name}: invalid sample rate {sample_rate} Hz")
    if byte_rate <= 0 or block_align <= 0:
        raise AssetError(f"{path.name}: invalid WAV byte rate/block alignment")
    if not 8 <= bits_per_sample <= 64:
        raise AssetError(f"{path.name}: invalid sample depth {bits_per_sample} bits")

    duration_ms = round(data_bytes * 1000 / byte_rate)
    if duration_ms <= 0:
        raise AssetError(f"{path.name}: computed duration is zero")

    return WavInfo(
        audio_format=audio_format,
        channels=channels,
        sample_rate_hz=sample_rate,
        bits_per_sample=bits_per_sample,
        data_bytes=data_bytes,
        duration_ms=duration_ms,
    )


def asset_record(spec: AssetSpec, path: Path) -> dict[str, object]:
    wav = inspect_wav(path)
    return {
        "sound_id": spec.sound_id,
        "source_filename": spec.source_filename,
        "resource_filename": spec.resource_filename,
        "sha256": sha256(path),
        "bytes": path.stat().st_size,
        "wav": asdict(wav),
    }


def expected_resource_paths(android_root: Path) -> dict[AssetSpec, Path]:
    directory = raw_dir(android_root)
    return {spec: directory / spec.resource_filename for spec in ASSETS}


def import_assets(source_dir: Path, android_root: Path = DEFAULT_ANDROID_ROOT) -> list[dict[str, object]]:
    source_dir = source_dir.expanduser().resolve()
    if not source_dir.is_dir():
        raise AssetError(f"Source directory does not exist: {source_dir}")

    sources = {spec: source_dir / spec.source_filename for spec in ASSETS}
    missing = [spec.source_filename for spec, path in sources.items() if not path.is_file()]
    if missing:
        raise AssetError("Missing approved source file(s): " + ", ".join(missing))

    # Validate and hash every source before mutating the Android resources directory.
    records = [asset_record(spec, sources[spec]) for spec in ASSETS]

    destination_dir = raw_dir(android_root)
    destination_dir.mkdir(parents=True, exist_ok=True)
    temp_paths: list[Path] = []
    try:
        for spec in ASSETS:
            destination = destination_dir / spec.resource_filename
            temporary = destination_dir / f".{spec.resource_filename}.importing"
            temp_paths.append(temporary)
            shutil.copyfile(sources[spec], temporary)
            os.replace(temporary, destination)

        manifest = {
            "schema": MANIFEST_SCHEMA,
            "purpose": "Approved WakeMyWay branded wake-sound assets",
            "assets": records,
        }
        output_manifest = manifest_path(android_root)
        output_manifest.parent.mkdir(parents=True, exist_ok=True)
        temporary_manifest = output_manifest.with_suffix(".json.importing")
        temporary_manifest.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        os.replace(temporary_manifest, output_manifest)
    finally:
        for temporary in temp_paths:
            temporary.unlink(missing_ok=True)

    return records


def load_manifest(path: Path) -> dict[str, object]:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise AssetError(f"Cannot read wake-sound manifest {path}: {exc}") from exc
    if not isinstance(data, dict) or data.get("schema") != MANIFEST_SCHEMA:
        raise AssetError(f"Unsupported or malformed wake-sound manifest: {path}")
    return data


def check_assets(android_root: Path = DEFAULT_ANDROID_ROOT) -> str:
    paths = expected_resource_paths(android_root)
    present = {spec: path for spec, path in paths.items() if path.is_file()}
    manifest = manifest_path(android_root)

    if not present:
        if manifest.exists():
            raise AssetError("Wake-sound checksum manifest exists but no branded WAV resources are bundled")
        return "Branded wake sounds are not bundled; emergency fallback remains the expected runtime path."

    if len(present) != len(ASSETS):
        present_names = ", ".join(spec.resource_filename for spec in present)
        missing_names = ", ".join(spec.resource_filename for spec in ASSETS if spec not in present)
        raise AssetError(
            "Partial branded wake-sound bundle is forbidden. "
            f"Present: {present_names or 'none'}. Missing: {missing_names or 'none'}."
        )

    if not manifest.is_file():
        raise AssetError(
            "All branded WAV resources are present but wake-sound-assets.json is missing; "
            "import them through scripts/wake_sound_assets.py"
        )

    manifest_data = load_manifest(manifest)
    manifest_assets = manifest_data.get("assets")
    if not isinstance(manifest_assets, list):
        raise AssetError("wake-sound-assets.json must contain an assets array")

    indexed: dict[str, dict[str, object]] = {}
    for entry in manifest_assets:
        if not isinstance(entry, dict) or not isinstance(entry.get("sound_id"), str):
            raise AssetError("wake-sound-assets.json contains an invalid asset entry")
        sound_id = entry["sound_id"]
        if sound_id in indexed:
            raise AssetError(f"wake-sound-assets.json contains duplicate sound id {sound_id}")
        indexed[sound_id] = entry

    expected_ids = {spec.sound_id for spec in ASSETS}
    if set(indexed) != expected_ids:
        raise AssetError(
            "wake-sound-assets.json sound ids do not match the approved catalog: "
            + ", ".join(sorted(indexed))
        )

    for spec in ASSETS:
        path = paths[spec]
        current = asset_record(spec, path)
        recorded = indexed[spec.sound_id]
        for field in ("source_filename", "resource_filename", "sha256", "bytes", "wav"):
            if recorded.get(field) != current[field]:
                raise AssetError(
                    f"{spec.resource_filename}: manifest {field} does not match the bundled file; "
                    "re-import the exact approved source WAVs"
                )

    return "Verified all three approved branded wake sounds and their checksum manifest."


def describe(records: Iterable[dict[str, object]]) -> str:
    lines = []
    for record in records:
        wav = record["wav"]
        assert isinstance(wav, dict)
        lines.append(
            f"{record['source_filename']} -> {record['resource_filename']} | "
            f"{wav['duration_ms']} ms | {wav['sample_rate_hz']} Hz | "
            f"{wav['channels']} ch | sha256 {record['sha256']}"
        )
    return "\n".join(lines)


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument(
        "--import",
        dest="source_dir",
        metavar="SOURCE_DIR",
        type=Path,
        help="directory containing the three approved source WAV filenames",
    )
    mode.add_argument(
        "--check",
        action="store_true",
        help="verify that the repository has either zero branded assets or one complete approved bundle",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    try:
        if args.source_dir is not None:
            records = import_assets(args.source_dir)
            print(describe(records))
            print(check_assets())
        else:
            print(check_assets())
    except AssetError as exc:
        print(f"wake-sound asset check failed: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
