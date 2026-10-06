"""Live smoke test of the leaderboards and friends against the REAL Supabase project (not part of `npm test`).

    PYTHONUTF8=1 python supabase/tests/live_social_check.py

Uses only the public publishable key, exactly like the app. It can only sign in as NEW anonymous users (a Google or
Apple login cannot be scripted), so it checks what an anonymous player must and must not be able to do: look at the
tables, but not take a username, add friends or see the friends tables; that nothing can be read or written
around the functions; that anonymous counts and crash reports can be added but not read back; and that an account can delete itself. The full rules (usernames, ranking, friends) are checked offline by `npm test`
(verify_leaderboards.mjs). Run it after `npx supabase db push` of the leaderboards migration.
"""
import json, urllib.request, urllib.error

URL = "https://lxrfnaxecvofmgkdtvrn.supabase.co"
KEY = "sb_publishable_K6wdedPxnM_zY9NIuV0SJQ__Cwf2eIP"


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


def rpc(fn, token, **params):
    return call(f"/rest/v1/rpc/{fn}", params, token)


failures = 0


def check(name, ok, extra=""):
    global failures
    print(("PASS " if ok else "FAIL ") + name + (f"  {extra}" if extra and not ok else ""))
    if not ok:
        failures += 1


status, body = call("/auth/v1/signup", {})
assert status == 200 and body["user"]["is_anonymous"], (status, body)
token = body["access_token"]

# ---- an anonymous player can look at the tables ----
for lang in ("EN", "TR"):
    for scope in ("TODAY", "ALL_TIME"):
        s, b = rpc("get_daily_leaderboard", token, p_language=lang, p_scope=scope, p_friends=False)
        check(f"speed table {lang} {scope} is readable", s == 200 and isinstance(b.get("rows"), list) and b.get("me") is None, (s, b))
for kind in ("CURRENT", "LONGEST"):
    s, b = rpc("get_streak_leaderboard", token, p_kind=kind, p_friends=False)
    check(f"streak table {kind} is readable", s == 200 and isinstance(b.get("rows"), list) and b.get("me") is None, (s, b))
check("a table never has more than 50 rows", len(rpc("get_streak_leaderboard", token, p_kind="LONGEST", p_friends=False)[1]["rows"]) <= 50)

# ---- ... but is not a member ----
s, b = rpc("get_my_profile", token)
check("profile says anonymous, no username, no invite code", s == 200 and b == {"is_member": False, "username": None, "invite_code": None}, (s, b))
s, b = rpc("set_username", token, p_name="livecheck_anon")
check("an anonymous player cannot take a username (403)", s == 403 and b.get("code") == "PT403", (s, b))
s, b = rpc("add_friend_by_code", token, p_code="ABCD2345")
check("... cannot add a friend by code (403)", s == 403, (s, b))
s, b = rpc("send_friend_request", token, p_username="someone")
check("... cannot send a friend request (403)", s == 403, (s, b))
s, b = rpc("get_friends", token)
check("... has no friends list (403)", s == 403, (s, b))
s, b = rpc("get_daily_leaderboard", token, p_language="EN", p_scope="TODAY", p_friends=True)
check("... has no friends table (403)", s == 403, (s, b))
s, b = rpc("get_streak_leaderboard", token, p_kind="CURRENT", p_friends=True)
check("... has no friends streak table (403)", s == 403, (s, b))

# ---- bad input and visitors ----
s, b = rpc("get_daily_leaderboard", token, p_language="FR", p_scope="TODAY", p_friends=False)
check("an unknown language is refused", s >= 400, (s, b))
s, b = rpc("get_streak_leaderboard", token, p_kind="WEEKLY", p_friends=False)
check("an unknown streak kind is refused", s >= 400, (s, b))
s, b = rpc("get_daily_leaderboard", None, p_language="EN", p_scope="TODAY", p_friends=False)
check("a visitor without a session is refused", s >= 400, (s, b))

# ---- nothing around the functions ----
for table in ("profile", "friendship", "friend_request", "streak_stat", "blocked_word"):
    s, b = call(f"/rest/v1/{table}?select=*", token=token, method="GET")
    check(f"table {table} cannot be read", s >= 400 or b == [], (s, b))
    s, b = call(f"/rest/v1/{table}", {}, token)
    check(f"table {table} cannot be written", s >= 400, (s, b))
for fn, params in (("streak_apply", {"p_user": "00000000-0000-0000-0000-000000000000", "p_day": 1}),
                   ("current_member", {}), ("username_blocked", {"p_name": "x"}),
                   ("make_friends", {"p_a": "00000000-0000-0000-0000-000000000000", "p_b": "00000000-0000-0000-0000-000000000001"}),
                   ("friend_scope", {"p_user": "00000000-0000-0000-0000-000000000000"})):
    s, b = rpc(fn, token, **params)
    check(f"helper {fn} is not callable", s >= 400, (s, b))

# ---- anonymous counts and crash reports (needs the telemetry migration) ----
s, b = rpc("log_events", token, p_platform="desktop", p_version="livecheck", p_events=[{"name": "app_open"}, {"name": "tab_viewed", "props": {"tab": "ranks"}}])
check("a batch of known events is accepted", s in (200, 204), (s, b))
s, b = rpc("log_events", token, p_platform="desktop", p_version="livecheck", p_events=[{"name": "not_on_the_list"}])
check("an event that is not on the list is refused", s >= 400, (s, b))
s, b = rpc("log_events", token, p_platform="toaster", p_version="livecheck", p_events=[{"name": "app_open"}])
check("an unknown platform is refused", s >= 400, (s, b))
s, b = rpc("log_crash", token, p_platform="desktop", p_version="livecheck", p_kind="LiveCheck", p_message="not a real crash", p_stack="at live_social_check")
check("a crash report is accepted", s in (200, 204), (s, b))
s, b = rpc("log_events", None, p_platform="desktop", p_version="livecheck", p_events=[{"name": "app_open"}])
check("a visitor without a session cannot log", s >= 400, (s, b))
for table in ("app_event", "crash_report", "event_name"):
    s, b = call(f"/rest/v1/{table}?select=*", token=token, method="GET")
    check(f"table {table} cannot be read back", s >= 400 or b == [], (s, b))
s, b = rpc("purge_telemetry", token)
check("purge_telemetry is not callable by app users", s >= 400, (s, b))

# ---- account merge (needs the account_merge migration): only the anonymous side can be tested without a Google login ----
s, b = rpc("start_account_merge", token)
check("an anonymous account gets a merge ticket", s == 200 and isinstance(b, str) and len(b) >= 60, (s, b))
s2, b2 = rpc("complete_account_merge", token, p_token=b if isinstance(b, str) else "x")
check("an anonymous account cannot complete a merge (403)", s2 == 403, (s2, b2))
s, b = rpc("complete_account_merge", token, p_token="not-a-ticket")
check("a made-up ticket is refused", s >= 400, (s, b))
s, b = rpc("start_account_merge", None)
check("a visitor without a session cannot ask for a ticket", s >= 400, (s, b))
s, b = call("/rest/v1/merge_ticket?select=*", token=token, method="GET")
check("table merge_ticket cannot be read", s >= 400 or b == [], (s, b))

# ---- account deletion (needs the delete_account migration): the throwaway user deletes itself ----
s, b = call("/auth/v1/user", token=token, method="GET")
check("before deleting, the session belongs to a user", s == 200 and b.get("id"), (s, b))
s, b = rpc("delete_my_account", token)
check("an account can delete itself", s in (200, 204), (s, b))
s, b = call("/auth/v1/user", token=token, method="GET")
check("afterwards the user no longer exists", s in (401, 403, 404), (s, b))
s, b = rpc("delete_my_account", None)
check("a visitor without a session cannot delete anything", s >= 400, (s, b))

print("\nALL CHECKS PASSED" if failures == 0 else f"\n{failures} CHECK(S) FAILED")
raise SystemExit(0 if failures == 0 else 1)
