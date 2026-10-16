"""
Applies the changes copied from tools/crossword/review.html: lines `TR|WORD|new clue` change a clue, `TR|WORD|-` removes the word.
The changes are read from the file given (or from the standard input) and written into the bank files.

A word that is on a ready board must not be removed (the board could not be opened any more); the script tells which words that
are, and leaves them. Change the boards first (Gen.java make/write) or keep the word.

Usage: python tools/crossword/bank_apply.py changes.txt [--force]   (--force removes words that are on boards too; make the boards again after)
"""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
BOARDS = HERE.parent.parent / "shared/src/commonMain/kotlin/com/mehmtcan/brainscroll/game/crossword/CrosswordBoards.kt"


def used_words(lang):
    """The words on the boards written in CrosswordBoards.kt."""
    text = BOARDS.read_text(encoding="utf-8")
    block = text.split("val " + lang + ":")[1].split(")\n")[0]
    boards = [line.strip().strip('",') for line in block.splitlines() if line.strip().startswith('"')]
    words = set()
    for b in boards:
        rows = [b[i * 16:(i + 1) * 16] for i in range(16)]
        lines = rows + ["".join(rows[r][c] for r in range(16)) for c in range(16)]
        for line in lines:
            for run in line.split("#"):
                if len(run) >= 2:
                    words.add(run)
    return words


def main():
    force = "--force" in sys.argv
    files = [a for a in sys.argv[1:] if not a.startswith("--")]
    source = Path(files[0]).read_text(encoding="utf-8") if files else sys.stdin.read()
    changes = {}
    for line in source.splitlines():
        if line.strip():
            lang, word, clue = line.split("|", 2)
            changes[(lang.strip().upper(), word.strip())] = clue.strip()
    in_boards = {lang: used_words(lang) for lang in ("TR", "EN")}
    changed = removed = 0
    for lang in ("TR", "EN"):
        for path in sorted((HERE / "bank").glob(lang.lower() + "_*.txt")):
            out = []
            for line in path.read_text(encoding="utf-8").splitlines():
                if "|" not in line:
                    continue
                word, clue = (p.strip() for p in line.split("|", 1))
                new = changes.pop((lang, word), None)
                if new == "-":
                    if word in in_boards[lang] and not force:
                        print("kept (on a board):", lang, word)
                        out.append(line)
                    else:
                        removed += 1
                    continue
                if new:
                    line = word + "|" + new
                    changed += 1
                out.append(line)
            path.write_text("\n".join(out) + "\n", encoding="utf-8", newline="\n")
    # What is left is new words (a renamed answer or a word that was added): they go to a file of their own.
    added = {"TR": [], "EN": []}
    for (lang, word), clue in changes.items():
        if clue == "-":
            print("not found:", lang, word)
        elif 2 <= len(word) <= 8 and clue:
            added[lang].append(word + "|" + clue)
    for lang, lines in added.items():
        if lines:
            path = HERE / "bank" / (lang.lower() + "_added.txt")
            old = path.read_text(encoding="utf-8").splitlines() if path.exists() else []
            path.write_text(chr(10).join(old + lines) + chr(10), encoding="utf-8", newline=chr(10))
    print("changed", changed, "removed", removed, "added", sum(len(v) for v in added.values()))
    print("next: python tools/crossword/bank_dedupe.py; python tools/crossword/bank_check.py; java tools/crossword/Gen.java bank")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
