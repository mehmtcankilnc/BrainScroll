// Applies the real migrations to an in-memory Postgres that imitates Supabase's auth, then checks account deletion:
// everything about the player is gone (every table), nobody else is touched, and only a signed-in player can call it.
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
const alice = '11111111-1111-1111-1111-111111111111'
const bob = '22222222-2222-2222-2222-222222222222'
const dave = '44444444-4444-4444-4444-444444444444' // anonymous
await db.exec(`insert into auth.users (id, is_anonymous) values ('${alice}', false), ('${bob}', false), ('${dave}', true)`)
for (const f of readdirSync(dir).filter(f => f.endsWith('.sql')).sort()) await db.exec(readFileSync(`${dir}/${f}`, 'utf8'))

let failures = 0
const check = (name, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')); if (!ok) failures++ }
const as = async (user, sql) => {
  await db.exec(`set role authenticated; select set_config('request.jwt.claim.sub', '${user ?? ''}', false)`)
  try { return await db.query(sql) } finally { await db.exec('reset role') }
}
const codeOf = async (user, sql) => { try { await as(user, sql); return null } catch (e) { return e.code ?? e.message } }
const owner = async (sql) => (await db.query(sql)).rows

// Give alice and bob a full set of data in every table, and make them friends.
const today = (await owner('select public.istanbul_day(now()) as d'))[0].d
for (const [u, n] of [[alice, 'a'], [bob, 'b']]) {
  await owner(`insert into public.game_result (user_id, id, game, mode, language, answer, guesses, outcome, was_skipped, finished_at, day_index, duration_ms)
    values ('${u}', 'e-${n}', 'word', 'ENDLESS', 'EN', 'CRANE', 'SLATE,CRANE', 'WON', false, 1, 5, null),
           ('${u}', 'd-${n}', 'word', 'DAILY', 'EN', 'CRANE', 'CRANE', 'WON', false, 1, ${today}, 9000)`)
  await owner(`insert into public.favorite (user_id, result_id, is_favorite, updated_at) values ('${u}', 'e-${n}', true, 1)`)
  await owner(`insert into public.daily_session (user_id, day_index, language) values ('${u}', ${today}, 'EN')`)
}
await as(alice, `select public.set_username('alice')`)
await as(bob, `select public.set_username('bob')`)
const code = (await as(alice, `select public.get_my_profile() as r`)).rows[0].r.invite_code
await as(bob, `select public.add_friend_by_code('${code}')`)
await as(dave, `select public.start_daily('EN')`)
const tables = ['game_result', 'favorite', 'daily_session', 'profile', 'streak_stat']
const countFor = async (u) => {
  let n = 0
  for (const t of tables) n += Number((await owner(`select count(*) c from public.${t} where user_id = '${u}'`))[0].c)
  n += Number((await owner(`select count(*) c from public.friendship where user_a = '${u}' or user_b = '${u}'`))[0].c)
  n += Number((await owner(`select count(*) c from public.friend_request where from_user = '${u}' or to_user = '${u}'`))[0].c)
  return n
}
check('alice has data in every table before', (await countFor(alice)) === 7, String(await countFor(alice)))

// Only a signed-in player can call it, and only on their own account.
check('a visitor without a session cannot delete (28000)', (await codeOf(null, `select public.delete_my_account()`)) === '28000')
await db.exec('set role anon')
let anonBlocked = false
try { await db.query(`select public.delete_my_account()`) } catch { anonBlocked = true }
await db.exec('reset role')
check('the anon role cannot call it at all', anonBlocked)
check('nothing was deleted by the refused calls', (await countFor(alice)) === 7)

// Alice deletes her account.
await as(alice, `select public.delete_my_account()`)
check('the auth user is gone', (await owner(`select count(*) c from auth.users where id = '${alice}'`))[0].c === 0)
check('every trace of alice is gone: results, favorites, sessions, username, streak, friendships', (await countFor(alice)) === 0, String(await countFor(alice)))
check('bob keeps everything of his own', Number((await owner(`select count(*) c from public.game_result where user_id = '${bob}'`))[0].c) === 2
  && Number((await owner(`select count(*) c from public.favorite where user_id = '${bob}'`))[0].c) === 1)
check('bob has no friends left, and nothing else of his is broken', (await as(bob, `select public.get_friends() as r`)).rows[0].r.friends.length === 0)
check('the tables do not list alice any more', !(JSON.stringify((await as(bob, `select public.get_daily_leaderboard('EN','TODAY',false) as r`)).rows[0].r)).includes('alice'))
check('her username is free again', (await codeOf(bob, `select public.set_username('alice')`)) === null)

// Anonymous players can delete theirs too.
await as(dave, `select public.delete_my_account()`)
check('an anonymous account can be deleted as well', (await owner(`select count(*) c from auth.users where id = '${dave}'`))[0].c === 0
  && Number((await owner(`select count(*) c from public.daily_session where user_id = '${dave}'`))[0].c) === 0)

console.log(failures === 0 ? '\nALL CHECKS PASSED' : `\n${failures} CHECK(S) FAILED`)
process.exit(failures === 0 ? 0 : 1)
