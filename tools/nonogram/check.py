"""Checks the pictures of Mini Nonogram: pictures5.txt (easy, 5x5), pictures.txt (medium, 8x8), pictures10.txt (hard, 10x10).

A picture is accepted if the rows and columns alone solve it (line logic, no guessing), it is 35-65% filled, it has at most one
empty row or column, and no other accepted picture of its size has the same shape (also not mirrored) or the same English name.
Run: python tools/nonogram/check.py [5|8|10]   (no argument checks all three)
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
FILES = {5: 'pictures5.txt', 8: 'pictures.txt', 10: 'pictures10.txt'}


def load(size=8):
    pictures, current = [], None
    for raw in open(os.path.join(HERE, FILES[size]), encoding='utf-8'):
        line = raw.rstrip('\n')
        if line.startswith('# '):
            continue
        if not line.strip():
            current = None
            continue
        if '|' in line:
            pid, tr, en = line.split('|')
            current = {'id': pid, 'tr': tr, 'en': en, 'rows': [], 'size': size}
            pictures.append(current)
        elif current is not None:
            current['rows'].append(line)
    return pictures


def clues(line):
    out, run = [], 0
    for c in line:
        if c:
            run += 1
        elif run:
            out.append(run)
            run = 0
    if run:
        out.append(run)
    return out


def placements(known, clue):
    """All fillings of a line that follow the clue and agree with the known cells (None unknown)."""
    n = len(known)
    result = []

    def go(pos, k, cur):
        if k == len(clue):
            rest = cur + [False] * (n - len(cur))
            if all(known[i] is None or known[i] == rest[i] for i in range(n)):
                result.append(rest)
            return
        for start in range(pos, n - clue[k] + 1):
            cells = cur + [False] * (start - len(cur)) + [True] * clue[k]
            if k + 1 < len(clue):
                cells = cells + [False]
            if len(cells) <= n and all(known[i] is None or known[i] == cells[i] for i in range(len(cells))):
                go(len(cells), k + 1, cells)

    go(0, 0, [])
    return result


def solve_line(known, clue):
    options = placements(known, clue)
    if not options:
        return None
    return [options[0][i] if all(o[i] == options[0][i] for o in options) else None for i in range(len(known))]


def solvable(grid):
    n = len(grid)
    rows = [clues(r) for r in grid]
    cols = [clues([grid[r][c] for r in range(n)]) for c in range(n)]
    board = [[None] * n for _ in range(n)]
    changed = True
    while changed:
        changed = False
        for r in range(n):
            new = solve_line(board[r], rows[r])
            if new is None:
                return False
            for c in range(n):
                if board[r][c] is None and new[c] is not None:
                    board[r][c] = new[c]
                    changed = True
        for c in range(n):
            new = solve_line([board[r][c] for r in range(n)], cols[c])
            if new is None:
                return False
            for r in range(n):
                if board[r][c] is None and new[r] is not None:
                    board[r][c] = new[r]
                    changed = True
    return all(board[r][c] is not None for r in range(n) for c in range(n))


def verdict(p):
    n = p.get('size', 8)
    if len(p['rows']) != n or any(len(r) != n or set(r) - set('#.') for r in p['rows']):
        return 'bad shape'
    grid = [[ch == '#' for ch in r] for r in p['rows']]
    fill = sum(map(sum, grid)) / (n * n)
    if not 0.35 <= fill <= 0.65:
        return 'fill %.0f%%' % (fill * 100)
    empty = sum(1 for r in grid if not any(r)) + sum(1 for c in range(n) if not any(grid[r][c] for r in range(n)))
    if empty > 1:
        return '%d empty lines' % empty
    if not solvable(grid):
        return 'needs guessing'
    return None


def accepted(size=8):
    seen, names, out = set(), set(), []
    for p in load(size):
        if verdict(p) is not None:
            continue
        key = tuple(p['rows'])
        mirror = tuple(r[::-1] for r in p['rows'])
        if key in seen or mirror in seen or p['en'] in names:
            continue
        seen.add(key)
        names.add(p['en'])
        out.append(p)
    return out


if __name__ == '__main__':
    sizes = [int(a) for a in sys.argv[1:]] or [5, 8, 10]
    for size in sizes:
        if not os.path.exists(os.path.join(HERE, FILES[size])):
            continue
        pictures = load(size)
        for p in pictures:
            print('%2d %-12s %s' % (size, p['id'], verdict(p) or 'ok'))
        print('%dx%d: %d of %d accepted' % (size, size, len(accepted(size)), len(pictures)))
