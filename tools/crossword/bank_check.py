"""
Checks the word bank files of the crossword (tools/crossword/bank/tr_*.txt and en_*.txt): one `WORD|clue` per line, the word in capitals
of its language, two to eight letters, a short clue, no word twice, and no clue that gives the answer away.

Usage: python tools/crossword/bank_check.py
"""
import sys
from pathlib import Path

BANK = Path(__file__).resolve().parent / "bank"
ALPHABET = {"tr": "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ", "en": "ABCDEFGHIJKLMNOPQRSTUVWXYZ"}
MAX_CLUE = 30

# Words whose clue shares a root with the answer on purpose (decided by hand).
ALLOWED_LEAKS = {("tr", "KLİMA"), ("tr", "KEMANCI"), ("tr", "UYKUSUZ")}


def lower(text, lang):
    if lang == "tr":
        return text.replace("İ", "i").replace("I", "ı").lower()
    return text.lower()


def check(lang):
    seen = {}
    problems = 0
    counts = {}
    for path in sorted(BANK.glob(lang + "_*.txt")):
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            line = line.strip()
            if not line:
                continue
            where = f"{path.name}:{number}"
            if "|" not in line:
                print(where, "no bar:", line)
                problems += 1
                continue
            word, clue = (part.strip() for part in line.split("|", 1))
            if not 2 <= len(word) <= 8:
                print(where, "length", word)
                problems += 1
            if any(ch not in ALPHABET[lang] for ch in word):
                print(where, "letters", word)
                problems += 1
            if len(clue) > MAX_CLUE or not clue:
                print(where, "clue length", len(clue), word, clue)
                problems += 1
            if word in seen:
                print(where, "twice", word, "also in", seen[word])
                problems += 1
            seen[word] = where
            stem = lower(word, lang)
            if (lang, word) not in ALLOWED_LEAKS and len(stem) >= 4 and stem[: max(4, len(stem) - 2)] in lower(clue, lang):
                print(where, "gives the answer away?", word, "|", clue)
                problems += 1
            counts[len(word)] = counts.get(len(word), 0) + 1
    total = sum(counts.values())
    print(lang.upper(), total, "words", dict(sorted(counts.items())), problems, "problems")
    return problems


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    bad = sum(check(lang) for lang in ("tr", "en"))
    sys.exit(1 if bad else 0)
