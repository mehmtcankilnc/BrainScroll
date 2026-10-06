"""Live smoke test of the daily puzzle against the REAL Supabase project (not part of `npm test`).

    python supabase/tests/live_daily_check.py      (PYTHONUTF8=1 on Windows)

Uses only the public publishable key, exactly like the app. Every scenario signs in as a NEW anonymous user, so each run
leaves a few anonymous users and daily result rows in the project (harmless; delete them in Authentication > Users).
Run it after `npx supabase db push` of a daily-puzzle migration. 33 checks: locked tables, hidden answer, colors
recomputed independently, server clock, i/I rules, 10 parallel guesses, 8 parallel starts.
"""
import json, sys, threading, urllib.request, urllib.error, os

URL = "https://lxrfnaxecvofmgkdtvrn.supabase.co"
KEY = "sb_publishable_K6wdedPxnM_zY9NIuV0SJQ__Cwf2eIP"
WORDS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "shared", "src", "commonMain", "composeResources", "files", "words")
UPPER_TR = str.maketrans("abcçdefgğhıijklmnoöprsştuüvyz", "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ")


def words(name, tr=False):
    out = []
    for line in open(os.path.join(WORDS, name), encoding="utf-8"):
        w = line.strip()
        if w and not w.startswith("#"):
            out.append(w.translate(UPPER_TR) if tr else w.upper())
    return out

EN, TR = words("en_answers.txt"), words("tr_answers.txt", tr=True)


def call(path, body=None, token=None, method="POST"):
    req = urllib.request.Request(URL + path, method=method, data=None if body is None else json.dumps(body).encode(),
                                 headers={"apikey": KEY, "Content-Type": "application/json",
                                          **({"Authorization": f"Bearer {token}"} if token else {})})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read().decode()
            return r.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, raw


def new_user():
    status, body = call("/auth/v1/signup", {})
    assert status == 200 and body["user"]["is_anonymous"], (status, body)
    return body["access_token"], body["user"]["id"]


def rpc(fn, token, **params):
    return call(f"/rest/v1/rpc/{fn}", params, token)


def evaluate(guess, answer):
    res = ["A"] * len(guess)
    pool = {}
    for i, (g, a) in enumerate(zip(guess, answer)):
        if g == a:
            res[i] = "C"
        else:
            pool[a] = pool.get(a, 0) + 1
    for i, g in enumerate(guess):
        if res[i] != "C" and pool.get(g, 0) > 0:
            res[i] = "P"
            pool[g] -= 1
    return "".join(res)


failures = 0
def check(name, ok, extra=""):
    global failures
    print(("PASS " if ok else "FAIL ") + name + (("  " + str(extra)) if extra else ""))
    failures += 0 if ok else 1


def play_until_over(token, language, pool, skip=()):
    """Sends guesses (words from the pool) until the attempt ends. Returns the last state."""
    state = None
    for w in [x for x in pool if x not in skip][:20]:
        status, state = rpc("submit_daily_guess", token, p_language=language, p_guess=w)
        assert status == 200, (status, state)
        if state["status"] != "PLAYING":
            return state
    raise AssertionError("never finished")


# ---------------------------------------------------------------- 1. start, state, idempotence
tokenA, userA = new_user()
check("anonymous sign-in works", bool(tokenA))
status, st = rpc("get_daily_state", tokenA, p_language="EN")
check("before starting: NOT_STARTED, no answer", status == 200 and st["status"] == "NOT_STARTED" and st["answer"] is None, st.get("status") if isinstance(st, dict) else st)
status, st1 = rpc("start_daily", tokenA, p_language="EN")
check("start_daily: PLAYING, 6 attempts, answer hidden", status == 200 and st1["status"] == "PLAYING" and st1["max_attempts"] == 6 and st1["answer"] is None)
status, st2 = rpc("start_daily", tokenA, p_language="EN")
check("starting again keeps the same clock", st2["started_at_ms"] == st1["started_at_ms"])
check("server clock is sane (within 5 min of this PC)", abs(st1["server_now_ms"] - __import__("time").time() * 1000) < 300_000, st1["server_now_ms"])

# ---------------------------------------------------------------- 2. locked down
for t in ("daily_puzzle", "answer_pool", "daily_session"):
    status, body = call(f"/rest/v1/{t}?select=*", token=tokenA, method="GET")
    check(f"cannot read table {t}", status in (401, 403, 404) or (status == 200 and body == []), f"HTTP {status}")
for fn, params in (("daily_feedback", {"p_guess": "CRANE", "p_answer": "CRANE"}), ("ensure_daily_puzzle", {"p_day": 1, "p_language": "EN"}), ("istanbul_day", {"p_at": "2026-01-01T00:00:00Z"})):
    status, body = rpc(fn, tokenA, **params)
    check(f"helper {fn} is not callable", status in (401, 403, 404), f"HTTP {status}")
status, body = call("/rest/v1/rpc/start_daily", {"p_language": "EN"})
check("no login: start_daily refused", status in (401, 403), f"HTTP {status}")
status, body = call("/rest/v1/daily_puzzle", [{"day_index": 1, "language": "EN", "answer": "CRANE"}], token=tokenA)
check("cannot write the answer table", status in (401, 403, 404), f"HTTP {status}")

# ---------------------------------------------------------------- 3. invalid guesses cost nothing
for bad in ("CRAN", "CRANES", "CR4NE", "ÇAĞRI"):
    status, body = rpc("submit_daily_guess", tokenA, p_language="EN", p_guess=bad)
    check(f"invalid guess {bad!r} refused", status == 400 and "invalid guess" in json.dumps(body), f"HTTP {status}")
status, st = rpc("get_daily_state", tokenA, p_language="EN")
check("nothing counted for them", len(st["guesses"]) == 0)

# ---------------------------------------------------------------- 4. play to the end, cross-check the colors
final = play_until_over(tokenA, "EN", EN)
answer = final["answer"]
check("attempt ended with the answer revealed", final["status"] in ("WON", "LOST") and bool(answer), f"{final['status']} {answer}")
mismatch = [(g["word"], g["feedback"], evaluate(g["word"], answer)) for g in final["guesses"] if g["feedback"] != evaluate(g["word"], answer)]
check("server colors equal an independent recomputation (all guesses)", not mismatch, mismatch[:2])
check("duration measured by the server", isinstance(final["duration_ms"], int) and final["duration_ms"] >= 0, final["duration_ms"])
status, rows = call("/rest/v1/game_result?select=id,mode,outcome,language,duration_ms,day_index,answer&mode=eq.DAILY", token=tokenA, method="GET")
check("result stored as DAILY with the same id, time and day", status == 200 and len(rows) == 1 and rows[0]["id"] == final["result_id"] and rows[0]["duration_ms"] == final["duration_ms"] and rows[0]["day_index"] == final["day_index"], rows)
status, body = rpc("submit_daily_guess", tokenA, p_language="EN", p_guess=EN[0])
check("guessing after the end is refused with 409 (not 500)", status == 409 and "already finished" in json.dumps(body), f"HTTP {status}")
status, st = rpc("start_daily", tokenA, p_language="EN")
check("start after the end returns the result, not a new attempt", st["status"] == final["status"] and st["duration_ms"] == final["duration_ms"])
status, body = call("/rest/v1/game_result", [{"id": "cheat", "game": "word", "mode": "DAILY", "language": "EN", "answer": "CRANE", "guesses": "CRANE", "outcome": "WON", "was_skipped": False, "finished_at": 1, "day_index": 1, "duration_ms": 1}], token=tokenA)
check("a client cannot write a daily result itself", status in (401, 403), f"HTTP {status}")

# ---------------------------------------------------------------- 5. everyone gets the same puzzle
tokenB, userB = new_user()
rpc("start_daily", tokenB, p_language="EN")
finalB = play_until_over(tokenB, "EN", EN)
check("another player has the same puzzle of the day (same answer)", finalB["answer"] == answer, f"{finalB['answer']} vs {answer}")
status, rowsB = call("/rest/v1/game_result?select=id&mode=eq.DAILY", token=tokenB, method="GET")
check("each player only sees their own result", len(rowsB) == 1 and rowsB[0]["id"] == finalB["result_id"])

# ---------------------------------------------------------------- 6. Turkish: its own puzzle, i / ı rules
tokenC, _ = new_user()
rpc("start_daily", tokenC, p_language="TR")
finalC = play_until_over(tokenC, "TR", TR)
check("Turkish puzzle finishes and reveals a Turkish answer", finalC["language"] == "TR" and finalC["answer"] != answer, finalC["answer"])
badC = [(g["word"], g["feedback"]) for g in finalC["guesses"] if g["feedback"] != evaluate(g["word"], finalC["answer"])]
check("Turkish colors equal the independent recomputation", not badC, badC[:2])
tokenD, _ = new_user()
rpc("start_daily", tokenD, p_language="TR")
lower = finalC["answer"].replace("İ", "i").replace("I", "ı").lower()
status, st = rpc("submit_daily_guess", tokenD, p_language="TR", p_guess=lower)
check(f"lowercase Turkish '{lower}' is read with the i/ı rules (WON)", status == 200 and st["status"] == "WON", st.get("status") if isinstance(st, dict) else st)

# ---------------------------------------------------------------- 7. truly parallel guesses on one attempt
tokenE, _ = new_user()
rpc("start_daily", tokenE, p_language="EN")
results = []
barrier = threading.Barrier(10)
def shoot(word):
    barrier.wait()
    results.append(rpc("submit_daily_guess", tokenE, p_language="EN", p_guess=word))
pool10 = [w for w in EN if w != answer][:10]
threads = [threading.Thread(target=shoot, args=(w,)) for w in pool10]
[t.start() for t in threads]; [t.join() for t in threads]
accepted = [r for r in results if r[0] == 200]
refused = [r for r in results if r[0] != 200]
status, st = rpc("get_daily_state", tokenE, p_language="EN")
check("10 simultaneous guesses: exactly 6 accepted, the rest refused", len(accepted) == 6 and len(refused) == 4, f"{len(accepted)} accepted / {len(refused)} refused")
check("the attempt holds exactly 6 distinct guesses and is LOST", len(st["guesses"]) == 6 and len({g['word'] for g in st['guesses']}) == 6 and st["status"] == "LOST", st["status"])
status, rowsE = call("/rest/v1/game_result?select=id&mode=eq.DAILY", token=tokenE, method="GET")
check("exactly one result row was written", len(rowsE) == 1, len(rowsE))

# ---------------------------------------------------------------- 8. parallel Start calls
tokenF, _ = new_user()
res = []
barrier2 = threading.Barrier(8)
def go():
    barrier2.wait(); res.append(rpc("start_daily", tokenF, p_language="EN"))
ts = [threading.Thread(target=go) for _ in range(8)]
[t.start() for t in ts]; [t.join() for t in ts]
check("8 simultaneous starts all succeed with ONE clock", all(r[0] == 200 for r in res) and len({r[1]["started_at_ms"] for r in res}) == 1, len({r[1]['started_at_ms'] for r in res if r[0]==200}))

print("\nALL LIVE CHECKS PASSED" if failures == 0 else f"\n{failures} LIVE CHECK(S) FAILED")
sys.exit(1 if failures else 0)
