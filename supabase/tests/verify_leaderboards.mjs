// Applies the real migrations to an in-memory Postgres that imitates Supabase's auth, then checks phase 7:
// usernames (rules, filter, anonymous users), the streak table, the daily and streak leaderboards, friends.
// Run: npm install && npm test   (from this folder)
import { PGlite } from '@electric-sql/pglite'
import { readdirSync, readFileSync } from 'node:fs'

const dir = process.argv[2] ?? new URL('../migrations', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')
const db = new PGlite()

await db.exec(`
  create role authenticated nologin;
  create role anon nologin;
  create schema auth;
  create table auth.users (id uuid primary key, is_anonymous boolean not null default false);
  create function auth.uid() returns uuid language sql stable
    as $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
  grant usage on schema auth to authenticated, anon;
  grant execute on function auth.uid() to authenticated, anon;
`)
const id = (n) => `${String(n).repeat(8)}-${String(n).repeat(4)}-${String(n).repeat(4)}-${String(n).repeat(4)}-${String(n).repeat(12)}`
const [alice, bob, carol, dave, erin] = [1, 2, 3, 4, 5].map(id) // dave is anonymous
await db.exec(`insert into auth.users (id, is_anonymous) values
  ('${alice}', false), ('${bob}', false), ('${carol}', false), ('${dave}', true), ('${erin}', false)`)

// A daily result written before phase 7, to see the migration build the streak from existing results.
const files = readdirSync(dir).filter(f => f.endsWith('.sql')).sort()
for (const f of files) {
  if (f.startsWith('20261007')) {
    await db.exec(`insert into public.game_result (user_id, id, game, mode, language, answer, guesses, outcome, was_skipped, finished_at, day_index, duration_ms)
      values ('${erin}', 'old1', 'word', 'DAILY', 'EN', 'CRANE', 'SLATE,CRANE', 'WON', false, 1, 100, 5000),
             ('${erin}', 'old2', 'word', 'DAILY', 'EN', 'CRANE', 'SLATE,CRANE', 'WON', false, 1, 101, 5000)`)
  }
  await db.exec(readFileSync(`${dir}/${f}`, 'utf8'))
}

let failures = 0
const check = (name, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')); if (!ok) failures++ }
const as = async (user, sql) => {
  await db.exec(`set role authenticated; select set_config('request.jwt.claim.sub', '${user ?? ''}', false)`)
  try { return await db.query(sql) } finally { await db.exec('reset role') }
}
const codeOf = async (user, sql) => { try { await as(user, sql); return null } catch (e) { return e.code ?? e.message } }
const call = async (user, fn, ...args) =>
  (await as(user, `select public.${fn}(${args.map(a => typeof a === 'boolean' ? a : `'${a}'`).join(', ')}) as r`)).rows[0].r
const owner = async (sql) => (await db.query(sql)).rows
const today = (await owner('select public.istanbul_day(now()) as d'))[0].d

// ---- locked down ----
for (const t of ['profile', 'friendship', 'friend_request', 'streak_stat', 'blocked_word'])
  check(`app user cannot read ${t}`, (await codeOf(alice, `select * from public.${t}`)) !== null)
check('app user cannot write a profile', (await codeOf(alice, `insert into public.profile values ('${alice}', 'hacker', 'X', now())`)) !== null)
for (const fn of ['streak_apply', 'current_member', 'username_blocked', 'make_friends'])
  check(`helper ${fn} is not callable`, (await codeOf(alice, `select public.${fn}()`)) !== null)
check('visitors without a session cannot ask for a table', (await codeOf(null, `select public.get_daily_leaderboard('EN','TODAY',false)`)) !== null)

// ---- usernames ----
const profile0 = await call(alice, 'get_my_profile')
check('a new member has no username yet', profile0.is_member === true && profile0.username === null && profile0.invite_code === null)
check('an anonymous player is not a member', (await call(dave, 'get_my_profile')).is_member === false)
check('anonymous player cannot take a username (PT403)', (await codeOf(dave, `select public.set_username('daveanon')`)) === 'PT403')
for (const bad of ['ab', 'abcdefghijklmnopq', 'has space', 'tür_kçe', 'a-b-c', ''])
  check(`username "${bad}" is invalid (22023)`, (await codeOf(alice, `select public.set_username('${bad}')`)) === '22023')
for (const bad of ['Fuck_You', 'xxSHITxx', 'amk123', 'f_u_c_k', 'sh1t', 'BrainScroll', 'admin_1'])
  check(`username "${bad}" is not allowed (PT422)`, (await codeOf(alice, `select public.set_username('${bad}')`)) === 'PT422')
const a1 = await call(alice, 'set_username', 'Alice_99')
check('a good username works and the invite code is created', a1.username === 'Alice_99' && /^[A-HJ-NP-Z2-9]{8}$/.test(a1.invite_code), JSON.stringify(a1))
check('the same name in another case is taken (PT409)', (await codeOf(bob, `select public.set_username('alice_99')`)) === 'PT409')
const a2 = await call(alice, 'set_username', 'alice_100')
check('renaming keeps the invite code', a2.username === 'alice_100' && a2.invite_code === a1.invite_code)
const b1 = await call(bob, 'set_username', 'bob')
const c1 = await call(carol, 'set_username', 'carol')
check('your own name is not "taken" when you set it again', (await call(bob, 'set_username', 'bob')).username === 'bob')

// ---- streaks (same cases as the app's DayStreaksTest rules) ----
const streakOf = async (u) => (await owner(`select * from public.streak_stat where user_id = '${u}'`))[0]
const erinStreak = await streakOf(erin)
check('the migration built the streak from results that already existed', erinStreak && erinStreak.streak === 2 && erinStreak.best === 2 && erinStreak.last_day === 101, JSON.stringify(erinStreak))
let n = 0
const daily = async (user, day, lang, guesses, outcome, ms) => {
  await owner(`insert into public.game_result (user_id, id, game, mode, language, answer, guesses, outcome, was_skipped, finished_at, day_index, duration_ms)
    values ('${user}', 'r${++n}', 'word', 'DAILY', '${lang}', 'CRANE', '${guesses}', '${outcome}', false, 1, ${day}, ${ms})`)
}
// bob: 7 days in a row ends on day 6 -> streak 7 and a freeze; then day 8 skips day 7 -> the freeze bridges it.
for (let d = 0; d < 7; d++) await daily(bob, 1000 + d, 'EN', 'SLATE,CRANE', 'WON', 9000)
let s = await streakOf(bob)
check('7 days in a row: streak 7 and one freeze earned', s.streak === 7 && s.best === 7 && s.freezes === 1, JSON.stringify(s))
await daily(bob, 1008, 'EN', 'CRANE', 'WON', 9000)
s = await streakOf(bob)
check('a missed day is bridged by the freeze', s.streak === 8 && s.freezes === 0, JSON.stringify(s))
await daily(bob, 1008, 'TR', 'KİTAP', 'LOST', 9000)
check('a second language on the same day changes nothing', (await streakOf(bob)).streak === 8)
await daily(bob, 1011, 'EN', 'CRANE', 'WON', 9000)
s = await streakOf(bob)
check('two missed days with no freeze start a new streak but keep the best', s.streak === 1 && s.best === 8, JSON.stringify(s))
// carol: a streak that ends today. alice: one that ended 3 days ago (broken now).
await daily(carol, today - 1, 'EN', 'CRANE', 'WON', 9000)
await daily(carol, today, 'EN', 'CRANE', 'WON', 9000)
await daily(alice, today - 4, 'EN', 'CRANE', 'WON', 9000)
await daily(alice, today - 3, 'EN', 'CRANE', 'WON', 9000)

// ---- streak leaderboards ----
const cur = await call(dave, 'get_streak_leaderboard', 'CURRENT', false)
check('current streaks: only players with a streak that is still alive', cur.rows.map(r => r.username).join() === 'carol' && cur.rows[0].value === 2, JSON.stringify(cur.rows))
const longest = await call(alice, 'get_streak_leaderboard', 'LONGEST', false)
check('longest streaks ranked, ties share a rank', longest.rows.map(r => `${r.rank}:${r.username}:${r.value}`).join() === '1:bob:8,2:alice_100:2,2:carol:2', longest.rows.map(r => `${r.rank}:${r.username}:${r.value}`).join())
check('"me" is my row', longest.me.username === 'alice_100' && longest.me.rank === 2)
check('anonymous players are never in a table', !longest.rows.some(r => r.username === 'daveanon') && (await call(dave, 'get_streak_leaderboard', 'LONGEST', false)).me === null)
check('players without a username are not in a table (erin)', !longest.rows.some(r => r.value === 2 && r.username === null))
check('a friends table needs a member (PT403)', (await codeOf(dave, `select public.get_streak_leaderboard('LONGEST', true)`)) === 'PT403')
check('unknown kind is refused', (await codeOf(alice, `select public.get_streak_leaderboard('WEEKLY', false)`)) === '22023')

// ---- daily speed leaderboard ----
// today: carol wins in 3 guesses/20s, bob in 3 guesses/12s, alice in 2 guesses/30s, in EN; carol lost in TR.
await daily(bob, today, 'EN', 'SLATE,TRAIN,CRANE', 'WON', 12000)
await daily(alice, today, 'EN', 'SLATE,CRANE', 'WON', 30000)
await daily(carol, today, 'TR', 'KİTAP,KİTAP,KİTAP,KİTAP,KİTAP,KİTAP', 'LOST', 5000)
await owner(`update public.game_result set guesses = 'SLATE,TRAIN,CRANE', duration_ms = 20000 where user_id = '${carol}' and day_index = ${today} and language = 'EN'`)
const t = await call(dave, 'get_daily_leaderboard', 'EN', 'TODAY', false)
check('today: fewest guesses first, then the shortest time', t.rows.map(r => `${r.rank}:${r.username}`).join() === '1:alice_100,2:bob,3:carol', t.rows.map(r => `${r.rank}:${r.username}`).join())
check('rows carry guesses and time, anonymous viewer has no "me"', t.rows[0].guesses === 2 && t.rows[0].duration_ms === 30000 && t.me === null)
check('Turkish table is separate and lost puzzles are not ranked', (await call(alice, 'get_daily_leaderboard', 'TR', 'TODAY', false)).rows.length === 0)
check('yesterday\'s results are not in "today"', !t.rows.some(r => r.username === 'carol' && r.guesses === 1))
const all = await call(carol, 'get_daily_leaderboard', 'EN', 'ALL_TIME', false)
check('all time: one best result per player', all.rows.length === 3 && all.rows.every((r, i, a) => a.findIndex(x => x.username === r.username) === i))
check('all time: bob\'s best is 2 guesses (day 1008 CRANE is 1 guess)', all.rows[0].guesses === 1 && all.rows.find(r => r.username === 'bob').guesses === 1)
check('"me" shows my own row with my rank', all.me.username === 'carol' && all.me.is_me === true && all.me.rank === all.rows.find(r => r.username === 'carol').rank)
check('unknown scope is refused', (await codeOf(alice, `select public.get_daily_leaderboard('EN','WEEK',false)`)) === '22023')
// top 50 only, "me" still shown below it
for (let i = 0; i < 55; i++) {
  const u = `aaaaaaaa-0000-0000-0000-${String(i).padStart(12, '0')}`
  await db.exec(`insert into auth.users (id) values ('${u}'); insert into public.profile (user_id, username, invite_code) values ('${u}', 'fast${i}', 'FASTCODE${i}')`)
  await daily(u, today, 'EN', 'CRANE', 'WON', 1000 + i)
}
const crowded = await call(carol, 'get_daily_leaderboard', 'EN', 'TODAY', false)
check('at most 50 rows, and "me" is still returned when outside them', crowded.rows.length === 50 && crowded.me.rank > 50 && !crowded.rows.some(r => r.is_me), `${crowded.rows.length} rows, me rank ${crowded.me?.rank}`)

// ---- friends ----
const codeAlice = (await call(alice, 'get_my_profile')).invite_code
check('no friends yet', (await call(alice, 'get_friends')).friends.length === 0)
check('anonymous player cannot add a friend (PT403)', (await codeOf(dave, `select public.add_friend_by_code('${codeAlice}')`)) === 'PT403')
check('a member without a username cannot add a friend (PT403)', (await owner(`select 1`)) && (await codeOf(erin, `select public.add_friend_by_code('${codeAlice}')`)) === 'PT403')
check('unknown code (PT404)', (await codeOf(bob, `select public.add_friend_by_code('ZZZZZZZZ')`)) === 'PT404')
check('own code is refused', (await codeOf(alice, `select public.add_friend_by_code('${codeAlice}')`)) === '22023')
const added = await call(bob, 'add_friend_by_code', codeAlice.toLowerCase())
check('a code makes friends at once, ignoring case', added.username === 'alice_100')
check('both sides see each other', (await call(alice, 'get_friends')).friends.join() === 'bob' && (await call(bob, 'get_friends')).friends.join() === 'alice_100')
await call(bob, 'add_friend_by_code', codeAlice)
check('using the code twice is harmless', (await owner(`select count(*) n from public.friendship`))[0].n === 1)

// request flow: carol asks alice by username
check('no such player (PT404)', (await codeOf(carol, `select public.send_friend_request('nobody_here')`)) === 'PT404')
const rq = await call(carol, 'send_friend_request', 'ALICE_100')
check('a request by username waits for the other side', rq.status === 'REQUESTED' && (await call(alice, 'get_friends')).incoming.join() === 'carol' && (await call(carol, 'get_friends')).outgoing.join() === 'alice_100')
check('the request does not make a friendship', !(await call(carol, 'get_friends')).friends.includes('alice_100'))
await call(alice, 'respond_friend_request', 'carol', true)
check('accepting makes friends and clears the request', (await call(carol, 'get_friends')).friends.join() === 'alice_100' && (await call(alice, 'get_friends')).incoming.length === 0)
check('answering a request that does not exist (PT404)', (await codeOf(alice, `select public.respond_friend_request('carol', true)`)) === 'PT404')
// crossing requests become friends at once; decline and cancel
await call(alice, 'remove_friend', 'carol')
check('removing a friend works one-sidedly for both', (await call(carol, 'get_friends')).friends.join() === '')
await call(alice, 'send_friend_request', 'carol')
check('a request to someone who asked you becomes friendship', (await call(carol, 'send_friend_request', 'alice_100')).status === 'FRIENDS')
await call(alice, 'remove_friend', 'carol')
await call(alice, 'send_friend_request', 'carol')
await call(carol, 'respond_friend_request', 'alice_100', false)
check('declining removes the request', (await call(alice, 'get_friends')).outgoing.length === 0 && (await call(carol, 'get_friends')).friends.length === 0)
await call(alice, 'send_friend_request', 'carol')
await call(alice, 'cancel_friend_request', 'carol')
check('cancelling removes the request', (await call(carol, 'get_friends')).incoming.length === 0)
check('you cannot ask yourself', (await codeOf(alice, `select public.send_friend_request('alice_100')`)) === '22023')

// friends tables: alice and bob are friends, carol is not
const fl = await call(alice, 'get_daily_leaderboard', 'EN', 'TODAY', true)
check('friends table: me and my friends only', fl.rows.map(r => r.username).join() === 'alice_100,bob' && fl.rows.map(r => r.rank).join() === '1,2', fl.rows.map(r => r.username).join())
const fs = await call(bob, 'get_streak_leaderboard', 'LONGEST', true)
check('friends streak table: me and my friends only', fs.rows.map(r => r.username).join() === 'bob,alice_100', fs.rows.map(r => r.username).join())

// deleting a user removes them from everything
await owner(`delete from auth.users where id = '${bob}'`)
check('deleting an account removes its profile, friendships and streak', (await owner(`select (select count(*) from public.profile where user_id='${bob}') + (select count(*) from public.friendship) + (select count(*) from public.streak_stat where user_id='${bob}') as n`))[0].n === 0)

console.log(failures === 0 ? '\nALL CHECKS PASSED' : `\n${failures} CHECK(S) FAILED`)
process.exit(failures === 0 ? 0 : 1)
