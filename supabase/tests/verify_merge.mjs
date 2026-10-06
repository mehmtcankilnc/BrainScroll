// Applies the real migrations to an in-memory Postgres that imitates Supabase's auth, then checks the account merge:
// the hand-over moves everything of the anonymous account, rebuilds the streak, works once, and cannot be abused.
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
const anonA = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' // plays anonymously
const googleB = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb' // the existing account the player signs in to
const anonC = 'cccccccc-cccc-cccc-cccc-cccccccccccc' // another anonymous account
const googleD = 'dddddddd-dddd-dddd-dddd-dddddddddddd' // somebody else
await db.exec(`insert into auth.users (id, is_anonymous) values ('${anonA}', true), ('${googleB}', false), ('${anonC}', true), ('${googleD}', false)`)
for (const f of readdirSync(dir).filter(f => f.endsWith('.sql')).sort()) await db.exec(readFileSync(`${dir}/${f}`, 'utf8'))

let failures = 0
const check = (name, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')); if (!ok) failures++ }
const as = async (user, sql) => {
  await db.exec(`set role authenticated; select set_config('request.jwt.claim.sub', '${user ?? ''}', false)`)
  try { return await db.query(sql) } finally { await db.exec('reset role') }
}
const codeOf = async (user, sql) => { try { await as(user, sql); return null } catch (e) { return e.code ?? e.message } }
const owner = async (sql) => (await db.query(sql)).rows
const count = async (sql) => Number((await owner(sql))[0].c)

let n = 0
const daily = (u, day, lang, ms = 9000, guesses = 'CRANE') => owner(`insert into public.game_result (user_id, id, game, mode, language, answer, guesses, outcome, was_skipped, finished_at, day_index, duration_ms)
  values ('${u}', 'r${++n}', 'word', 'DAILY', '${lang}', 'CRANE', '${guesses}', 'WON', false, 1, ${day}, ${ms})`)
const session = (u, day, lang) => owner(`insert into public.daily_session (user_id, day_index, language, finished_at, outcome, result_id) values ('${u}', ${day}, '${lang}', now(), 'WON', 'r${n}')`)

// Anonymous A played days 100, 101, 102 (EN) and one endless puzzle, with a favorite.
for (const d of [100, 101, 102]) { await daily(anonA, d, 'EN'); await session(anonA, d, 'EN') }
await owner(`insert into public.game_result (user_id, id, game, mode, language, answer, guesses, outcome, was_skipped, finished_at, day_index)
  values ('${anonA}', 'endless-a', 'word', 'ENDLESS', 'EN', 'SLATE', 'SLATE', 'WON', false, 1, 100)`)
await owner(`insert into public.favorite (user_id, result_id, is_favorite, updated_at) values ('${anonA}', 'endless-a', true, 50)`)
// The existing Google account B has day 103 (EN) and 101 (EN: the same day and language as A's), plus its own favorite.
await daily(googleB, 103, 'EN', 5000)
await daily(googleB, 101, 'EN', 7000)
await owner(`insert into public.favorite (user_id, result_id, is_favorite, updated_at) values ('${googleB}', 'endless-a', false, 10)`)
await as(googleB, `select public.set_username('bee')`)

// ---- step 1: the ticket ----
const secret = (await as(anonA, `select public.start_account_merge() as r`)).rows[0].r
check('an anonymous account gets a long random secret', typeof secret === 'string' && secret.length >= 60, secret.length)
check('only a hash of it is stored', (await count(`select count(*) c from public.merge_ticket where token_hash = '${secret}'`)) === 0
  && (await count(`select count(*) c from public.merge_ticket where from_user = '${anonA}'`)) === 1)
check('a signed-in (non-anonymous) account cannot start a merge (PT403)', (await codeOf(googleB, `select public.start_account_merge()`)) === 'PT403')
check('a visitor without a session cannot (28000)', (await codeOf(null, `select public.start_account_merge()`)) === '28000')
const second = (await as(anonA, `select public.start_account_merge() as r`)).rows[0].r
check('asking again replaces the ticket: still one, and the old secret is dead', second !== secret
  && (await count(`select count(*) c from public.merge_ticket where from_user = '${anonA}'`)) === 1
  && (await codeOf(googleB, `select public.complete_account_merge('${secret}')`)) === 'PT404')

// ---- abuse ----
check('an anonymous account cannot complete a merge (PT403)', (await codeOf(anonC, `select public.complete_account_merge('${second}')`)) === 'PT403')
check('a wrong secret finds nothing (PT404)', (await codeOf(googleB, `select public.complete_account_merge('nope')`)) === 'PT404')
check('the table cannot be read or written by app users', (await codeOf(anonA, `select * from public.merge_ticket`)) !== null
  && (await codeOf(anonA, `insert into public.merge_ticket values ('x', '${anonA}', now())`)) !== null)
await owner(`update public.merge_ticket set expires_at = now() - interval '1 minute'`)
check('an expired ticket is refused (PT404)', (await codeOf(googleB, `select public.complete_account_merge('${second}')`)) === 'PT404')
check('the failed attempts moved nothing', (await count(`select count(*) c from public.game_result where user_id = '${googleB}'`)) === 2)

// ---- step 2: the hand-over ----
const secret2 = (await as(anonA, `select public.start_account_merge() as r`)).rows[0].r
const summary = (await as(googleB, `select public.complete_account_merge('${secret2}') as r`)).rows[0].r
check('B now holds A\'s daily results of days 100 and 102, and keeps its own 101 and 103', (await count(`select count(*) c from public.game_result where user_id = '${googleB}' and mode = 'DAILY'`)) === 4
  && (await count(`select count(*) c from public.game_result where user_id = '${googleB}' and mode = 'DAILY' and day_index = 101 and duration_ms = 7000`)) === 1, JSON.stringify(summary))
check('... and the endless result', (await count(`select count(*) c from public.game_result where user_id = '${googleB}' and id = 'endless-a'`)) === 1)
check('the sessions of the days that did not clash came along (100 and 102, not 101)', (await count(`select count(*) c from public.daily_session where user_id = '${googleB}'`)) === 2
  && (await count(`select count(*) c from public.daily_session where user_id = '${googleB}' and day_index = 101`)) === 0)
check('the newer favorite wins: A\'s (updated 50) replaced B\'s (updated 10)', (await count(`select count(*) c from public.favorite where user_id = '${googleB}' and result_id = 'endless-a' and is_favorite and updated_at = 50`)) === 1)
const streak = (await owner(`select streak, best, last_day from public.streak_stat where user_id = '${googleB}'`))[0]
check('the streak is rebuilt from all days: 100 to 103 in a row', streak.streak === 4 && streak.best === 4 && streak.last_day === 103, JSON.stringify(streak))
check('the anonymous account is gone, with its ticket', (await count(`select count(*) c from auth.users where id = '${anonA}'`)) === 0
  && (await count(`select count(*) c from public.merge_ticket where from_user = '${anonA}'`)) === 0
  && (await count(`select count(*) c from public.game_result where user_id = '${anonA}'`)) === 0)
check('the summary says what moved', summary.sessions === 2 && summary.results >= 3, JSON.stringify(summary))
check('a ticket works once (PT404)', (await codeOf(googleB, `select public.complete_account_merge('${secret2}')`)) === 'PT404')
check('B appears in the leaderboard with its merged results and username', JSON.stringify((await as(googleD, `select public.get_streak_leaderboard('LONGEST', false) as r`)).rows[0].r).includes('bee'))

// ---- nobody else is touched, and one cannot take over somebody else's anonymous account ----
await daily(anonC, 200, 'EN')
check('D (someone else) is untouched', (await count(`select count(*) c from public.game_result where user_id = '${googleD}'`)) === 0)
const cSecret = (await as(anonC, `select public.start_account_merge() as r`)).rows[0].r
check('without C\'s secret nobody can take C\'s data', (await codeOf(googleD, `select public.complete_account_merge('${secret2}')`)) === 'PT404'
  && (await count(`select count(*) c from public.game_result where user_id = '${anonC}'`)) === 1)
// A ticket whose anonymous account was linked to Google in the meantime (no longer anonymous) is refused.
await owner(`update auth.users set is_anonymous = false where id = '${anonC}'`)
check('a ticket of an account that is not anonymous any more is refused (PT404)', (await codeOf(googleD, `select public.complete_account_merge('${cSecret}')`)) === 'PT404')
check('and nothing of C moved', (await count(`select count(*) c from public.game_result where user_id = '${googleD}'`)) === 0)

console.log(failures === 0 ? '\nALL CHECKS PASSED' : `\n${failures} CHECK(S) FAILED`)
process.exit(failures === 0 ? 0 : 1)
