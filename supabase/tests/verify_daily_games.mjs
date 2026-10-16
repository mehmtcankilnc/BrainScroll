// Applies the real migrations to an in-memory Postgres that imitates Supabase's auth, then checks the daily results of
// the games other than the word puzzle: submitting a result, what is refused, one result a day, the day streak and the
// speed table of one game.
// Run: npm install && node verify_daily_games.mjs   (from this folder)
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
const [alice, bob, carol] = [1, 2, 3].map(id)
await db.exec(`insert into auth.users (id, is_anonymous) values ('${alice}', false), ('${bob}', false), ('${carol}', false)`)
for (const f of readdirSync(dir).filter(f => f.endsWith('.sql')).sort()) await db.exec(readFileSync(`${dir}/${f}`, 'utf8'))

let failures = 0
const check = (name, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')); if (!ok) failures++ }
const as = async (user, sql) => {
  await db.exec(`set role authenticated; select set_config('request.jwt.claim.sub', '${user ?? ''}', false)`)
  try { return await db.query(sql) } finally { await db.exec('reset role') }
}
const codeOf = async (user, sql) => { try { await as(user, sql); return null } catch (e) { return e.code ?? e.message } }
const owner = async (sql) => (await db.query(sql)).rows
const submit = (user, game, outcome, moves, ms, lang = 'EN') =>
  codeOf(user, `select public.submit_daily_game_result('${game}', '${outcome}', ${moves}, ${ms}, '${lang}')`)
const board = async (user, game, scope, friends = false) =>
  (await as(user, `select public.get_daily_game_leaderboard('${game}', '${scope}', ${friends}) as r`)).rows[0].r
const today = (await owner('select public.istanbul_day(now()) as d'))[0].d

for (const [user, name] of [[alice, 'alice'], [bob, 'bobby'], [carol, 'carol']])
  await as(user, `select public.set_username('${name}')`)

// ---- who may call ----
check('a visitor without a session cannot submit', (await submit(null, 'mines', 'WON', 20, 20000)) !== null)
check('a visitor without a session cannot ask for the table', (await codeOf(null, `select public.get_daily_game_leaderboard('mines','TODAY',false)`)) !== null)

// ---- refused results ----
check('the word puzzle has its own flow', (await submit(alice, 'word', 'WON', 6, 20000)) === '22023')
check('an unknown game is refused', (await submit(alice, 'chess', 'WON', 6, 20000)) === '22023')
check('an unknown outcome is refused', (await submit(alice, 'mines', 'DRAW', 6, 20000)) === '22023')
check('a time that is too quick is refused', (await submit(alice, 'mines', 'WON', 20, 800)) === '22023')
check('a time longer than a day is refused', (await submit(alice, 'mines', 'WON', 20, 90000000)) === '22023')
check('no moves is refused', (await submit(alice, 'mines', 'WON', 0, 20000)) === '22023')
check('nothing was stored by the refused ones', (await owner(`select count(*)::int as n from public.game_result`))[0].n === 0)

// ---- a result ----
check('a plausible result is accepted', (await submit(alice, 'mines', 'WON', 40, 20000)) === null)
let rows = await owner(`select * from public.game_result where user_id = '${alice}'`)
check('it is stored as a daily result of today', rows.length === 1 && rows[0].mode === 'DAILY' && rows[0].day_index === today && rows[0].game === 'mines')
check('with its time and its moves', rows[0].duration_ms == 20000 && rows[0].guesses === '40' && rows[0].outcome === 'WON')
check('and it moves the day streak', (await owner(`select streak from public.streak_stat where user_id = '${alice}'`))[0]?.streak === 1)
check('a second result the same day changes nothing', (await submit(alice, 'sudoku', 'WON', 50, 30000)) === null
  && (await owner(`select count(*)::int as n from public.game_result where user_id = '${alice}'`))[0].n === 1)
check('the same result twice changes nothing', (await submit(alice, 'mines', 'WON', 20, 10000)) === null
  && (await owner(`select duration_ms from public.game_result where user_id = '${alice}'`))[0].duration_ms == 20000)
check('a loss still counts as a finished daily puzzle', (await submit(carol, 'mines', 'LOST', 12, 9000)) === null
  && (await owner(`select streak from public.streak_stat where user_id = '${carol}'`))[0]?.streak === 1)

// ---- clients cannot write daily results themselves ----
check('a client cannot insert a daily result directly', (await codeOf(bob,
  `insert into public.game_result (id, game, mode, language, answer, guesses, outcome, was_skipped, finished_at, day_index, duration_ms)
   values ('x', 'mines', 'DAILY', 'EN', 'daily', '5', 'WON', false, 1, ${today}, 2000)`)) !== null)

// ---- the table ----
check('bob finishes faster than alice', (await submit(bob, 'mines', 'WON', 35, 15000)) === null)
let t = await board(alice, 'mines', 'TODAY')
check('the table is shortest time first', t.rows.length === 2 && t.rows[0].username === 'bobby' && t.rows[0].rank === 1 && t.rows[1].username === 'alice')
check('a row has moves and time', t.rows[0].moves === 35 && t.rows[0].duration_ms === 15000)
check('and says which one is me', t.rows[1].is_me === true && t.rows[0].is_me === false && t.me.username === 'alice' && t.me.rank === 2)
check('a lost game is not on the table', !t.rows.some(r => r.username === 'carol'))
check('another game has its own table', (await board(alice, 'sudoku', 'TODAY')).rows.length === 0)
check('all time shows the best of every player', (await board(alice, 'mines', 'ALL_TIME')).rows.length === 2)
await owner(`update public.game_result set day_index = ${today - 3} where user_id = '${bob}'`)
check('today leaves out earlier days', (await board(alice, 'mines', 'TODAY')).rows.length === 1)
check('all time keeps them', (await board(alice, 'mines', 'ALL_TIME')).rows.length === 2)
check('friends only: alone, only me', (await board(alice, 'mines', 'ALL_TIME', true)).rows.length === 1)
check('the word puzzle is not a table of this kind', (await codeOf(alice, `select public.get_daily_game_leaderboard('word','TODAY',false)`)) === '22023')

// ---- One Line (migration 20261014090000_line_daily.sql) ----
// The number sent as moves is a score code: 1 + undos + 100 * hints. The table is ordered by stars, then time, and a row's
// moves is its stars.
await owner(`delete from public.game_result`)
check('line: the stars of a score code', JSON.stringify((await owner(`select public.line_stars(c) as s from unnest(array[1,2,4,5,101,102,105,201,999]) as c`)).map(r => r.s)) === JSON.stringify([3, 2, 2, 1, 2, 1, 0, 2, 0]))
check('line: a result is accepted', (await submit(alice, 'line', 'WON', 1, 30000)) === null)
check('line: stored like the others, the code as its guess', (await owner(`select guesses, game, mode from public.game_result where user_id = '${alice}'`))[0].guesses === '1')
check('line: a result with a hint is accepted', (await submit(bob, 'line', 'WON', 101, 20000)) === null)
check('line: a faster three-star result', (await submit(carol, 'line', 'WON', 1, 25000)) === null)
let lt = await board(alice, 'line', 'TODAY')
check('line: more stars first, then the shorter time', lt.rows.map(r => r.username).join() === 'carol,alice,bobby', lt.rows.map(r => r.username).join())
check('line: a row carries its stars as moves', lt.rows.map(r => r.moves).join() === '3,3,2', lt.rows.map(r => r.moves).join())
check('line: and the ranks follow', lt.rows.map(r => r.rank).join() === '1,2,3' && lt.me.rank === 2 && lt.me.moves === 3)
check('line: all time shows the best of every player', (await board(alice, 'line', 'ALL_TIME')).rows.length === 3)
check('line: friends only: alone, only me', (await board(alice, 'line', 'TODAY', true)).rows.length === 1)
check('line: a result with no score code is refused', (await submit(alice, 'line', 'WON', 0, 30000)) === '22023')
check('line: a time that is too quick is refused', (await submit(alice, 'line', 'WON', 1, 800)) === '22023')
check('line: the table of another game is not mixed in', (await board(alice, 'mines', 'TODAY')).rows.length === 0)

// ---- Daily crossword (migration 20261016090000_crossword_daily.sql) ----
// The number sent as moves is a score code: 1 + the mistakes. The table is ordered by stars, then time, and a row's moves is its stars.
await owner(`delete from public.game_result`)
check('crossword: the stars of a score code', JSON.stringify((await owner(`select public.crossword_stars(c) as s from unnest(array[1,2,4,5,11,12,13,999]) as c`)).map(r => r.s)) === JSON.stringify([3, 3, 3, 2, 2, 1, 1, 1]))
check('crossword: a result is accepted', (await submit(alice, 'crossword', 'WON', 1, 600000)) === null)
check('crossword: stored like the others, the code as its guess', (await owner(`select guesses, game, mode from public.game_result where user_id = '${alice}'`))[0].guesses === '1')
check('crossword: a result with many mistakes is accepted', (await submit(bob, 'crossword', 'WON', 13, 500000)) === null)
check('crossword: a faster three-star result', (await submit(carol, 'crossword', 'WON', 3, 550000)) === null)
let ct = await board(alice, 'crossword', 'TODAY')
check('crossword: more stars first, then the shorter time', ct.rows.map(r => r.username).join() === 'carol,alice,bobby', ct.rows.map(r => r.username).join())
check('crossword: a row carries its stars as moves', ct.rows.map(r => r.moves).join() === '3,3,1', ct.rows.map(r => r.moves).join())
check('crossword: and the ranks follow', ct.rows.map(r => r.rank).join() === '1,2,3' && ct.me.rank === 2 && ct.me.moves === 3)
check('crossword: all time shows the best of every player', (await board(alice, 'crossword', 'ALL_TIME')).rows.length === 3)
check('crossword: a result with no score code is refused', (await submit(alice, 'crossword', 'WON', 0, 600000)) === '22023')
check('crossword: a time that is too quick is refused', (await submit(alice, 'crossword', 'WON', 1, 800)) === '22023')
check('crossword: the table of another game is not mixed in', (await board(alice, 'line', 'TODAY')).rows.length === 0)

console.log(failures === 0 ? '\nAll checks passed.' : `\n${failures} check(s) failed.`)
process.exit(failures === 0 ? 0 : 1)
