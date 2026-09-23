#!/usr/bin/env python3
"""Report compressed and logical composition of Android release artifacts."""

from __future__ import annotations

import argparse
import json
import zipfile
from collections import defaultdict
from pathlib import Path


def human_bytes(value: int) -> str:
    units = ["B", "KiB", "MiB", "GiB"]
    size = float(value)
    for unit in units:
        if size < 1024 or unit == units[-1]:
            return f"{size:.2f} {unit}"
        size /= 1024
    raise AssertionError("unreachable")


def group_name(name: str) -> str:
    if name.startswith("res/raw/"):
        return "res/raw"
    if name.startswith("lib/"):
        parts = name.split("/")
        return "/".join(parts[:2]) if len(parts) > 1 else "lib"
    if name.startswith("classes") and name.endswith(".dex"):
        return "dex"
    if name.startswith("res/"):
        return "res/other"
    if name.startswith("assets/"):
        return "assets"
    if name.startswith("META-INF/"):
        return "META-INF"
    if name == "resources.arsc":
        return "resources.arsc"
    if name.startswith("base/"):
        return "aab/base"
    return name.split("/", 1)[0]


def inspect(path: Path) -> dict:
    groups: dict[str, dict[str, int]] = defaultdict(
        lambda: {"compressed": 0, "uncompressed": 0, "entries": 0}
    )
    with zipfile.ZipFile(path) as archive:
        entries = [entry for entry in archive.infolist() if not entry.is_dir()]
        for entry in entries:
            group = groups[group_name(entry.filename)]
            group["compressed"] += entry.compress_size
            group["uncompressed"] += entry.file_size
            group["entries"] += 1
        largest = sorted(entries, key=lambda entry: entry.compress_size, reverse=True)[:25]

    return {
        "path": str(path),
        "artifact_bytes": path.stat().st_size,
        "artifact_human": human_bytes(path.stat().st_size),
        "groups": [
            {
                "name": name,
                **values,
                "compressed_human": human_bytes(values["compressed"]),
                "uncompressed_human": human_bytes(values["uncompressed"]),
            }
            for name, values in sorted(
                groups.items(),
                key=lambda item: item[1]["compressed"],
                reverse=True,
            )
        ],
        "largest_entries": [
            {
                "name": entry.filename,
                "compressed": entry.compress_size,
                "compressed_human": human_bytes(entry.compress_size),
                "uncompressed": entry.file_size,
                "uncompressed_human": human_bytes(entry.file_size),
            }
            for entry in largest
        ],
    }


def markdown(reports: list[dict]) -> str:
    lines = ["# WakeMyWay Android release-size report", ""]
    for report in reports:
        lines += [
            f"## {Path(report['path']).name}",
            "",
            f"Artifact size: **{report['artifact_human']}**",
            "",
            "| Group | Compressed | Uncompressed | Entries |",
            "|---|---:|---:|---:|",
        ]
        for group in report["groups"][:20]:
            lines.append(
                f"| {group['name']} | {group['compressed_human']} | "
                f"{group['uncompressed_human']} | {group['entries']} |"
            )
        lines += [
            "",
            "### Largest entries",
            "",
            "| Entry | Compressed | Uncompressed |",
            "|---|---:|---:|",
        ]
        for entry in report["largest_entries"]:
            lines.append(
                f"| `{entry['name']}` | {entry['compressed_human']} | "
                f"{entry['uncompressed_human']} |"
            )
        lines.append("")
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("artifacts", nargs="+", type=Path)
    parser.add_argument("--json", type=Path)
    parser.add_argument("--markdown", type=Path)
    args = parser.parse_args()

    missing = [path for path in args.artifacts if not path.is_file()]
    if missing:
        raise SystemExit(f"Missing artifact(s): {', '.join(map(str, missing))}")

    reports = [inspect(path) for path in args.artifacts]
    rendered = markdown(reports)

    if args.json:
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(json.dumps(reports, indent=2) + "\n", encoding="utf-8")
    if args.markdown:
        args.markdown.parent.mkdir(parents=True, exist_ok=True)
        args.markdown.write_text(rendered + "\n", encoding="utf-8")

    print(rendered)


if __name__ == "__main__":
    main()
