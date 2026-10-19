// Applies every migration to an in-memory Postgres that imitates Supabase's auth, then checks the difficulty column of the results:
// an upload without it (an older app) is medium, 0 to 2 are accepted, anything else is refused.
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
await db.exec(`insert into auth.users (id, is_anonymous) values ('${alice}', true)`)
for (const f of readdirSync(dir).filter(f => f.endsWith('.sql')).sort()) await db.exec(readFileSync(`${dir}/${f}`, 'utf8'))

let failures = 0
const check = (name, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')); if (!ok) failures++ }
const as = async (user, sql) => {
  await db.exec(`set role authenticated; select set_config('request.jwt.claim.sub', '${user}', false)`)
  try { return await db.query(sql) } finally { await db.exec('reset role') }
}
const fails = async (user, sql) => { try { await as(user, sql); return false } catch (e) { return e.message } }

const insert = (id, difficulty) =>
  `insert into public.game_result(id, game, mode, language, answer, guesses, outcome, was_skipped, finished_at, day_index${difficulty === undefined ? '' : ', difficulty'})
   values ('${id}', 'sudoku', 'ENDLESS', 'EN', 'CRANE', '3', 'WON', false, 1000, 5${difficulty === undefined ? '' : ', ' + difficulty})`

await as(alice, insert('a'))
await as(alice, insert('b', 0))
await as(alice, insert('c', 2))
const level = async id => (await db.query(`select difficulty from public.game_result where id='${id}'`)).rows[0].difficulty
check('an upload without the column is medium', (await level('a')) === 1)
check('easy is kept', (await level('b')) === 0)
check('hard is kept', (await level('c')) === 2)
check('level 3 is refused', (await fails(alice, insert('d', 3))) !== false)
check('a negative level is refused', (await fails(alice, insert('e', -1))) !== false)
check('the column cannot be null', (await fails(alice, insert('f', 'null'))) !== false)

console.log(failures ? `${failures} FAILED` : 'ALL PASSED')
process.exit(failures ? 1 : 0)
