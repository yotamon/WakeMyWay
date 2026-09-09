#!/usr/bin/env python3
"""Validate local Markdown links and canonical documentation structure."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MARKDOWN = [ROOT / "README.md", ROOT / "AGENTS.md", ROOT / "CONTEXT.md", *sorted((ROOT / "docs").rglob("*.md"))]
LINK_RE = re.compile(r"\[[^\]]*\]\(([^)]+)\)")

errors: list[str] = []

required = [ROOT / "CONTEXT.md", ROOT / "docs/00-project-status.md", ROOT / "docs/adr/README.md"]
for path in required:
    if not path.exists():
        errors.append(f"missing required source-of-truth file: {path.relative_to(ROOT)}")

for md in MARKDOWN:
    text = md.read_text(encoding="utf-8")
    for raw in LINK_RE.findall(text):
        target = raw.strip().split("#", 1)[0]
        if not target or target.startswith(("http://", "https://", "mailto:", "tel:")):
            continue
        target = target.replace("%20", " ")
        resolved = (md.parent / target).resolve()
        try:
            resolved.relative_to(ROOT.resolve())
        except ValueError:
            errors.append(f"{md.relative_to(ROOT)}: link escapes repo: {raw}")
            continue
        if not resolved.exists():
            errors.append(f"{md.relative_to(ROOT)}: broken local link: {raw}")

if errors:
    print("Documentation validation failed:\n")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print(f"Documentation validation passed ({len(MARKDOWN)} Markdown files checked).")
