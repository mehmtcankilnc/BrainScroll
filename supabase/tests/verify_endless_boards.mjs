// Applies the real migrations to an in-memory Postgres that imitates Supabase's auth, then checks the tables of the games of the feed
// (get_game_leaderboard): who is on a table, what is left out, and the order of a series, of the word puzzle and of every other game.
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
const [alice, bob, carol, dave, erin] = [1, 2, 3, 4, 5].map(id) // dave is anonymous, erin never chose a username
await db.exec(`insert into auth.users (id, is_anonymous) values
  ('${alice}', false), ('${bob}', false), ('${carol}', false), ('${dave}', true), ('${erin}', false)`)
for (const f of readdirSync(dir).filter(f => f.endsWith('.sql')).sort()) await db.exec(readFileSync(`${dir}/${f}`, 'utf8'))

let failures = 0
const check = (name, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')); if (!ok) failures++ }
const as = async (user, sql) => {
  await db.exec(`set role authenticated; select set_config('request.jwt.claim.sub', '${user ?? ''}', false)`)
  try { return await db.query(sql) } finally { await db.exec('reset role') }
}
const codeOf = async (user, sql) => { try { await as(user, sql); return null } catch (e) { return e.code ?? e.message } }
const owner = async (sql) => (await db.query(sql)).rows
const today = (await owner('select public.istanbul_day(now()) as d'))[0].d

// Usernames are chosen the way the app does it.
await as(alice, `select public.set_username('alice_99')`)
await as(bob, `select public.set_username('bob_42')`)
await as(carol, `select public.set_username('carol_7')`)

let n = 0
const put = (user, game, { guesses, outcome = 'WON', duration = 30000, skipped = false, difficulty = 1, day = 5, language = 'EN', mode = 'ENDLESS' }) =>
  db.exec(`insert into public.game_result (user_id, id, game, mode, language, answer, guesses, outcome, was_skipped, finished_at, day_index, duration_ms, difficulty)
    values ('${user}', 'r${++n}', '${game}', '${mode}', '${language}', 'X', '${guesses}', '${outcome}', ${skipped}, 1000, ${day}, ${duration}, ${difficulty})`)
const board = async (user, game, scope = 'ALL_TIME', friends = false, difficulty = 1, language = null) =>
  (await as(user, `select public.get_game_leaderboard('${game}', '${scope}', ${friends}, ${difficulty}, ${language === null ? 'null' : `'${language}'`}) as r`)).rows[0].r
const names = (b) => b.rows.map(r => r.username).join(',')

// ---- a game played for time: shortest time first, then fewest moves ----
await put(alice, 'sudoku', { guesses: '40', duration: 90000 })
await put(bob, 'sudoku', { guesses: '35', duration: 60000 })
await put(carol, 'sudoku', { guesses: '30', duration: 60000 })
let b = await board(alice, 'sudoku')
check('the shortest time is first, then the fewest moves', names(b) === 'carol_7,bob_42,alice_99', names(b))
check('the rows carry rank, moves and time', b.rows[0].rank === 1 && b.rows[0].moves === 30 && b.rows[0].duration_ms === 60000)
check('my own row is given as me', b.me.username === 'alice_99' && b.me.rank === 3 && b.me.is_me === true)
check('rows are marked for who is me', b.rows.filter(r => r.is_me).length === 1)

// ---- what is left out ----
await put(alice, 'sudoku', { guesses: '10', duration: 5000, skipped: true })
await put(bob, 'sudoku', { guesses: '10', duration: 5000, outcome: 'LOST' })
await put(carol, 'sudoku', { guesses: '10', duration: 5000, difficulty: 0 })
await put(erin, 'sudoku', { guesses: '10', duration: 1000 })
await put(dave, 'sudoku', { guesses: '10', duration: 1000 })
await put(bob, 'sudoku', { guesses: '10', duration: 500 })
await put(bob, 'sudoku', { guesses: '10', duration: 90000000 })
b = await board(alice, 'sudoku')
check('a result with the guide, a lost one, another level, a player with no username, an anonymous player and an impossible time are all left out', names(b) === 'carol_7,bob_42,alice_99', names(b))
b = await board(alice, 'sudoku', 'ALL_TIME', false, 0)
check('the easy table has only its own result', names(b) === 'carol_7' && b.rows[0].duration_ms === 5000, names(b))
check('a table with no result at all is empty and has no me', (await board(alice, 'sudoku', 'ALL_TIME', false, 2)).rows.length === 0 && (await board(alice, 'sudoku', 'ALL_TIME', false, 2)).me === null)

// ---- today and all time ----
await put(alice, 'hanoi', { guesses: '20', duration: 40000, day: today })
await put(bob, 'hanoi', { guesses: '15', duration: 20000, day: today - 3 })
check('all time has both days', names(await board(alice, 'hanoi')) === 'bob_42,alice_99')
check('today has only today', names(await board(alice, 'hanoi', 'TODAY')) === 'alice_99')

// ---- a series: the most rounds first, then the shortest time, lost ones count ----
await put(alice, 'cubefold', { guesses: '5', outcome: 'LOST', duration: 80000 })
await put(bob, 'cubefold', { guesses: '5', outcome: 'LOST', duration: 50000 })
await put(carol, 'cubefold', { guesses: '8', outcome: 'WON', duration: 99000 })
await put(erin, 'cubefold', { guesses: '8', duration: 1000 })
await put(alice, 'cubefold', { guesses: '0', outcome: 'LOST', duration: 4000 })
b = await board(alice, 'cubefold')
check('the series with the most rounds is first, then the shorter time', names(b) === 'carol_7,bob_42,alice_99', names(b))
check('a row of a series says its rounds', b.rows[0].moves === 8 && b.rows[1].moves === 5)
check('a series with no round at all is not a result', !(await board(alice, 'cubefold')).rows.some(r => r.moves === 0))

// ---- the word puzzle: fewest guesses first, then time; one language at a time ----
await put(alice, 'word', { guesses: 'SLATE,CRANE', duration: 30000, language: 'EN' })
await put(bob, 'word', { guesses: 'CRANE', duration: 90000, language: 'EN' })
await put(carol, 'word', { guesses: 'SLATE,CRANE', duration: 20000, language: 'EN' })
await put(bob, 'word', { guesses: 'KALEM', duration: 15000, language: 'TR' })
await put(alice, 'word', { guesses: 'AAA,BBB,CCC', duration: 9000, language: 'EN', mode: 'DAILY' })
b = await board(alice, 'word', 'ALL_TIME', false, 1, 'EN')
check('the fewest guesses first, then the shortest time', names(b) === 'bob_42,carol_7,alice_99' && b.rows[0].moves === 1 && b.rows[2].moves === 2, names(b))
check('the other language has its own table', names(await board(alice, 'word', 'ALL_TIME', false, 1, 'TR')) === 'bob_42')

// ---- a memory result counts the pairs of flips ----
await put(alice, 'memory', { guesses: 'a,b,c,d,e,f', duration: 30000 })
check('memory moves are half the flips', (await board(alice, 'memory')).rows[0].moves === 3)

// ---- friends only ----
await as(alice, `select public.set_username('alice_99')`).catch(() => {})
const friendCode = (await as(bob, `select public.get_my_profile() as r`)).rows[0].r.invite_code
await as(alice, `select public.add_friend_by_code('${friendCode}')`)
b = await board(alice, 'sudoku', 'ALL_TIME', true)
check('friends only: me and my friend, not everyone', names(b) === 'bob_42,alice_99', names(b))

// ---- guards ----
check('a visitor without a session is refused', (await codeOf(null, `select public.get_game_leaderboard('sudoku','ALL_TIME',false,1,null)`)) !== null)
check('a daily game of another kind is refused here (line has its own table)', (await codeOf(alice, `select public.get_game_leaderboard('line','ALL_TIME',false,1,null)`)) === '22023')
check('a bad game id is refused', (await codeOf(alice, `select public.get_game_leaderboard('Not A Game','ALL_TIME',false,1,null)`)) === '22023')
check('a bad scope is refused', (await codeOf(alice, `select public.get_game_leaderboard('sudoku','YESTERDAY',false,1,null)`)) === '22023')
check('a bad level is refused', (await codeOf(alice, `select public.get_game_leaderboard('sudoku','ALL_TIME',false,3,null)`)) === '22023')
check('a bad language is refused', (await codeOf(alice, `select public.get_game_leaderboard('word','ALL_TIME',false,1,'FR')`)) === '22023')

console.log(failures ? `${failures} FAILED` : 'ALL PASSED')
process.exit(failures ? 1 : 0)
