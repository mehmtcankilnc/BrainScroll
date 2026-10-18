#!/usr/bin/env python3
"""Independent check of a reel puzzle (does not share code with puzzle.mjs).
1. the numbers/target follow the app's rules
2. the shown solution is a legal sequence of moves and ends on the target
3. the target cannot be made with fewer operations than the solution uses (brute force)
usage: verify.py out/A/puzzle.json
"""
import json
import sys


def op(name, a, b):
    if name == "+":
        return a + b
    if name == "-":
        return a - b if a > b else None
    if name == "*":
        return a * b
    if name == "/":
        return a // b if b != 0 and a % b == 0 else None


p = json.load(open(sys.argv[1]))
nums, target, steps = p["numbers"], p["target"], p["steps"]
assert 10 <= target <= 99, "target must be two digits"
assert target not in nums, "target must not be on the table at the start"
assert all(1 <= n <= 9 for n in nums), "numbers are 1..9"

table = list(nums)
for i, s in enumerate(steps):
    assert s["a"] in table, f"step {i}: {s['a']} is not on the table {table}"
    table.remove(s["a"])
    assert s["b"] in table, f"step {i}: {s['b']} is not on the table {table}"
    table.remove(s["b"])
    r = op(s["op"], s["a"], s["b"])
    assert r is not None and r > 0, f"step {i}: illegal move {s}"
    assert r == s["result"], f"step {i}: {s['a']} {s['op']} {s['b']} is {r}, not {s['result']}"
    table.append(r)
assert steps[-1]["result"] == target, "the last step must make the target"
print(f"solution ok: {len(steps)} legal steps, ends on {target}")

# minimality: nothing equal to the target can be made in fewer than len(steps) operations
par = len(steps)
frontier = {tuple(sorted(nums))}
for depth in range(1, par):
    nxt = set()
    for state in frontier:
        n = len(state)
        for i in range(n):
            for j in range(n):
                if i == j:
                    continue
                for name in "+-*/":
                    r = op(name, state[i], state[j])
                    if r is None:
                        continue
                    assert r != target, f"target reachable in {depth} operations (< {par})"
                    if depth < par - 1:
                        rest = [state[k] for k in range(n) if k not in (i, j)] + [r]
                        nxt.add(tuple(sorted(rest)))
    frontier = nxt
    print(f"depth {depth}: {len(nxt) if depth < par - 1 else '-'} states, target not made")
print(f"minimal: the target needs exactly {par} operations")
