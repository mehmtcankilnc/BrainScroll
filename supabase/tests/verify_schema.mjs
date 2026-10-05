// Applies the first migration to an in-memory Postgres that imitates Supabase's auth schema, then checks the rules
// (row level security, append-only results, last-writer-wins favorites).
// Run: npm install && npm test   (from this folder)
import { PGlite } from '@electric-sql/pglite'
import { readFileSync } from 'node:fs'

const dir = process.argv[2] ?? new URL('../migrations', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')
const migration = readFileSync(`${dir}/20261005120000_initial_schema.sql`, 'utf8')
const db = new PGlite()

// What Supabase provides before our migration runs.
await db.exec(`
  create role authenticated nologin;
  create role anon nologin;
  create schema auth;
  create table auth.users (id uuid primary key);
  create function auth.uid() returns uuid language sql stable
    as $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
  grant usage on schema auth to authenticated, anon;
  grant execute on function auth.uid() to authenticated, anon;
`)
const alice = '11111111-1111-1111-1111-111111111111'
const bob = '22222222-2222-2222-2222-222222222222'
await db.exec(`insert into auth.users values ('${alice}'), ('${bob}')`)

await db.exec(migration)

let failures = 0
const check = (name, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')); if (!ok) failures++ }
const as = async (user, sql, params) => {
  await db.exec(`set role authenticated; select set_config('request.jwt.claim.sub', '${user}', false)`)
  try { return await db.query(sql, params) } finally { await db.exec('reset role') }
}
const fails = async (user, sql, params) => { try { await as(user, sql, params); return false } catch (e) { return e.message } }

const result = (id, mode = 'ENDLESS') =>
  `insert into public.game_result(id, game, mode, language, answer, guesses, outcome, was_skipped, finished_at, day_index)
   values ('${id}', 'word', '${mode}', 'EN', 'CRANE', 'SLATE,CRANE', 'WON', false, 1000, 5)`

// game_result
await as(alice, result('r1'))
check('user_id is filled from the session', (await db.query(`select user_id from public.game_result where id='r1'`)).rows[0].user_id === alice)
check('alice reads her result', (await as(alice, `select * from public.game_result`)).rows.length === 1)
check('bob cannot see alice\'s result', (await as(bob, `select * from public.game_result`)).rows.length === 0)
check('daily result from a client is rejected', (await fails(alice, result('d1', 'DAILY'))) !== false)
check('writing a row for another user is rejected',
  (await fails(alice, `insert into public.game_result(user_id,id,game,mode,language,answer,guesses,outcome,was_skipped,finished_at,day_index)
                       values ('${bob}','x','word','ENDLESS','EN','A','A','WON',false,1,1)`)) !== false)
check('invalid outcome is rejected', (await fails(alice, `insert into public.game_result(id,game,mode,language,answer,guesses,outcome,was_skipped,finished_at,day_index)
                       values ('o','word','ENDLESS','EN','A','A','DRAW',false,1,1)`)) !== false)
await as(alice, result('r1') + ' on conflict (user_id, id) do nothing')
check('uploading the same result twice keeps one row', (await as(alice, `select * from public.game_result`)).rows.length === 1)
// Blocked means: the database raised an error (no privilege) or touched no row (no policy). Either is fine.
const blocked = async (user, sql) => { try { return (await as(user, sql)).rows.length === 0 } catch { return true } }
check('results cannot be updated (append-only)', await blocked(alice, `update public.game_result set answer='HACKS' where id='r1' returning id`))
check('results cannot be deleted by the client', await blocked(alice, `delete from public.game_result where id='r1' returning id`))
check('favorites cannot be deleted by the client', await blocked(alice, `delete from public.favorite returning result_id`))

// favorite
const fav = (user, id, flag, at) => as(user, `insert into public.favorite(result_id,is_favorite,updated_at) values ('${id}', ${flag}, ${at})
   on conflict (user_id, result_id) do update set is_favorite = excluded.is_favorite, updated_at = excluded.updated_at`)
await fav(alice, 'r1', true, 100)
await fav(alice, 'r1', false, 200)
check('newer favorite write wins', (await as(alice, `select is_favorite from public.favorite`)).rows[0].is_favorite === false)
await fav(alice, 'r1', true, 150) // arrives late from an offline device
let row = (await as(alice, `select is_favorite, updated_at from public.favorite`)).rows[0]
check('older favorite write is ignored', row.is_favorite === false && Number(row.updated_at) === 200, JSON.stringify(row))
await fav(alice, 'r1', true, 300)
check('even newer write wins again', (await as(alice, `select is_favorite from public.favorite`)).rows[0].is_favorite === true)
check('bob cannot see alice\'s favorites', (await as(bob, `select * from public.favorite`)).rows.length === 0)
await fav(bob, 'r1', true, 1) // same result id, different user: a separate row
check('same result id for another user is a separate row', (await db.query(`select * from public.favorite`)).rows.length === 2)

// the anon (not logged in) role has no policies
await db.exec(`set role anon`)
let anonBlocked = false
try { anonBlocked = (await db.query(`select * from public.game_result`)).rows.length === 0 } catch { anonBlocked = true }
await db.exec('reset role')
check('a visitor without a session sees nothing', anonBlocked)

// deleting the user removes their data
await db.exec(`delete from auth.users where id='${alice}'`)
check('deleting a user cascades to their rows',
  (await db.query(`select count(*)::int as n from public.game_result where user_id='${alice}'`)).rows[0].n === 0 &&
  (await db.query(`select count(*)::int as n from public.favorite where user_id='${alice}'`)).rows[0].n === 0)

console.log(failures === 0 ? '\nALL CHECKS PASSED' : `\n${failures} CHECK(S) FAILED`)
process.exit(failures === 0 ? 0 : 1)
