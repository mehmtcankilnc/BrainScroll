"""Removes from the bank files every line whose word is already in an earlier file (the *_base.txt files come first)."""
import sys
from pathlib import Path

bank = Path(__file__).resolve().parent / "bank"
for lang in ("tr", "en"):
    files = sorted(bank.glob(lang + "_*.txt"), key=lambda p: (0 if "base" in p.name else 1, p.name))
    seen = set()
    for path in files:
        keep = []
        removed = 0
        for line in path.read_text(encoding="utf-8").splitlines():
            if not line.strip() or "|" not in line or "?" in line:
                if line.strip():
                    removed += 1
                continue
            word = line.split("|", 1)[0].strip()
            if word in seen:
                removed += 1
                continue
            seen.add(word)
            keep.append(line)
        path.write_text("\n".join(keep) + "\n", encoding="utf-8", newline="\n")
        print(path.name, "kept", len(keep), "removed", removed)
