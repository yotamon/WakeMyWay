#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


SEMVER_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")
MARKDOWN_LINK_RE = re.compile(r"\[([^\]]+)\]\([^\)]+\)")
PR_SUFFIX_RE = re.compile(r"\s+by\s+@[^\s]+\s+in\s+https?://\S+\s*$", re.IGNORECASE)


def bump_version(current: str, release_type: str) -> str:
    match = SEMVER_RE.fullmatch(current.strip())
    if not match:
        raise ValueError(f"Expected stable semantic version X.Y.Z, got: {current!r}")

    major, minor, patch = map(int, match.groups())
    if release_type == "patch":
        patch += 1
    elif release_type == "minor":
        minor += 1
        patch = 0
    elif release_type == "major":
        major += 1
        minor = 0
        patch = 0
    else:
        raise ValueError(f"Unsupported release type: {release_type!r}")

    return f"{major}.{minor}.{patch}"


def extract_release_notes(markdown: str, limit: int = 12) -> list[str]:
    notes: list[str] = []
    for raw_line in markdown.splitlines():
        line = raw_line.strip()
        if not (line.startswith("- ") or line.startswith("* ")):
            continue

        text = line[2:].strip()
        text = MARKDOWN_LINK_RE.sub(r"\1", text)
        text = PR_SUFFIX_RE.sub("", text)
        text = re.sub(r"\s+", " ", text).strip()
        if text and text not in notes:
            notes.append(text)
        if len(notes) >= limit:
            break

    return notes


def write_manifest(
    *,
    version_code: int,
    version_name: str,
    priority: int,
    repository: str,
    apk_sha256: str,
    published_at: str,
    notes_markdown: str,
    output: Path,
) -> None:
    payload = {
        "schemaVersion": 1,
        "channel": "stable",
        "versionCode": version_code,
        "versionName": version_name,
        "priority": priority,
        "publishedAt": published_at,
        "releaseNotes": extract_release_notes(notes_markdown),
        "apk": {
            "url": f"https://github.com/{repository}/releases/latest/download/WakeMyWay-direct.apk",
            "sha256": apk_sha256.lower(),
        },
    }
    output.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="WakeMyWay Android release utility")
    subparsers = parser.add_subparsers(dest="command", required=True)

    bump = subparsers.add_parser("bump")
    bump.add_argument("--current", required=True)
    bump.add_argument("--release-type", choices=("patch", "minor", "major"), required=True)

    manifest = subparsers.add_parser("manifest")
    manifest.add_argument("--version-code", type=int, required=True)
    manifest.add_argument("--version-name", required=True)
    manifest.add_argument("--priority", type=int, default=1)
    manifest.add_argument("--repository", required=True)
    manifest.add_argument("--apk-sha256", required=True)
    manifest.add_argument("--published-at", required=True)
    manifest.add_argument("--notes-file", type=Path, required=True)
    manifest.add_argument("--output", type=Path, required=True)

    args = parser.parse_args()
    if args.command == "bump":
        print(bump_version(args.current, args.release_type))
        return

    write_manifest(
        version_code=args.version_code,
        version_name=args.version_name,
        priority=args.priority,
        repository=args.repository,
        apk_sha256=args.apk_sha256,
        published_at=args.published_at,
        notes_markdown=args.notes_file.read_text(encoding="utf-8"),
        output=args.output,
    )


if __name__ == "__main__":
    main()
