// Applies the real migrations to an in-memory Postgres that imitates Supabase's auth, then checks the daily puzzle:
// coloring rule (same cases as the app's tests), hidden answers, server clock, no way around the three functions.
// Run: npm install && npm test   (from this folder)
import { PGlite } from '@electric-sql/pglite'
import { readFileSync } from 'node:fs'

const dir = process.argv[2] ?? new URL('../migrations', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')
const db = new PGlite()
console.log((await db.query('select version()')).rows[0].version.slice(0, 40))

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
await db.exec(`insert into auth.users values ('${alice}'), ('${bob}')`)
await db.exec(readFileSync(`${dir}/20261005120000_initial_schema.sql`, 'utf8'))
await db.exec(readFileSync(`${dir}/20261006090000_daily_puzzle.sql`, 'utf8'))
await db.exec(readFileSync(`${dir}/20261006120000_daily_error_codes.sql`, 'utf8'))

let failures = 0
const check = (name, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')); if (!ok) failures++ }
const as = async (user, sql, params) => {
  await db.exec(`set role authenticated; select set_config('request.jwt.claim.sub', '${user ?? ''}', false)`)
  try { return await db.query(sql, params) } finally { await db.exec('reset role') }
}
const codeOf = async (user, sql) => { try { await as(user, sql); return null } catch (e) { return e.code } }
const errorOf = async (user, sql) => { try { await as(user, sql); return null } catch (e) { return e.message } }
const call = async (user, fn, ...args) => (await as(user, `select public.${fn}(${args.map(a => `'${a}'`).join(', ')}) as r`)).rows[0].r
const owner = async (sql) => (await db.query(sql)).rows

// ---- the coloring rule, with the same cases as the app's GuessEvaluatorTest ----
const fb = async (g, a) => (await owner(`select public.daily_feedback('${g}', '${a}') as r`))[0].r
const vectors = [
  ['CRANE', 'CRANE', 'CCCCC'], ['CRANE', 'MOGUL', 'AAAAA'], ['ABCDE', 'BCDEA', 'PPPPP'],
  ['LLABC', 'XYLZW', 'PAAAA'], ['BABBY', 'ABBEY', 'PPCAC'], ['EXXXE', 'ABCDE', 'AAAAC'],
  ['KİTAP', 'KITAP', 'CACCC'], ['ÇAĞRI', 'CAGRI', 'ACACC'],
]
for (const [g, a, want] of vectors) { const got = await fb(g, a); check(`feedback ${g} vs ${a} = ${want}`, got === want, got) }

// ---- letter rules and the Istanbul day ----
const norm = async (l, t) => (await owner(`select public.daily_normalize('${l}', '${t}') as r`))[0].r
check('TR: dotted i becomes İ', (await norm('TR', 'işçi')) === 'İŞÇİ', await norm('TR', 'işçi'))
check('TR: dotless ı becomes I', (await norm('TR', 'ışık')) === 'IŞIK', await norm('TR', 'ışık'))
check('TR: ç ğ ö ü kept distinct', (await norm('TR', 'çğöüş')) === 'ÇĞÖÜŞ')
check('EN: i becomes I', (await norm('EN', 'crane')) === 'CRANE')
const day = async (iso) => (await owner(`select public.istanbul_day('${iso}'::timestamptz) as r`))[0].r
check('Istanbul day of the epoch is 0', (await day('1970-01-01T00:00:00Z')) === 0)
check('Istanbul midnight starts a new day', (await day('2026-10-04T21:00:00Z')) === 20731 && (await day('2026-10-04T20:59:59.999Z')) === 20730)
check('before the epoch rounds down', (await day('1969-12-31T20:59:59Z')) === -1)

// ---- locked down: app users have no way in except the three functions ----
for (const sql of ['select * from public.daily_puzzle', 'select * from public.answer_pool', 'select * from public.daily_session',
  `insert into public.daily_puzzle values (1, 'EN', 'CRANE')`, `update public.daily_session set outcome = 'WON'`])
  check('app user blocked: ' + sql.slice(0, 48), (await errorOf(alice, sql)) !== null)
for (const sql of [`select public.daily_feedback('CRANE','CRANE')`, `select public.ensure_daily_puzzle(1,'EN')`, `select public.istanbul_day(now())`])
  check('helper not callable: ' + sql.slice(0, 44), (await errorOf(alice, sql)) !== null)
check('visitor without a session cannot start', (await errorOf(null, `select public.start_daily('EN')`)) !== null)
check('anon role cannot call start_daily', await (async () => { await db.exec('set role anon'); try { await db.query(`select public.start_daily('EN')`); return false } catch { return true } finally { await db.exec('reset role') } })())
check('answer pool has both languages', Number((await owner(`select count(*) n from public.answer_pool where language='EN'`))[0].n) > 1500 && Number((await owner(`select count(*) n from public.answer_pool where language='TR'`))[0].n) > 900)

// ---- the daily flow ----
const before = await call(alice, 'get_daily_state', 'EN')
check('state before starting is NOT_STARTED and shows no answer', before.status === 'NOT_STARTED' && before.answer === null)
check('submitting before starting is refused', /not started/.test(await errorOf(alice, `select public.submit_daily_guess('EN','CRANE')`)))
check('... with the code PT412 (HTTP 412 in PostgREST)', (await codeOf(alice, `select public.submit_daily_guess('EN','CRANE')`)) === 'PT412')

const started = await call(alice, 'start_daily', 'EN')
check('start returns PLAYING, 6 attempts, no answer', started.status === 'PLAYING' && started.max_attempts === 6 && started.answer === null && started.guesses.length === 0)
check('server clock is included', typeof started.server_now_ms === 'number' && started.started_at_ms <= started.server_now_ms)
const again = await call(alice, 'start_daily', 'EN')
check('starting again does not restart the clock', again.started_at_ms === started.started_at_ms)
const todayEN = (await owner(`select * from public.daily_puzzle where language='EN'`))
check('one puzzle exists for today in English', todayEN.length === 1)
const answer = todayEN[0].answer
await call(bob, 'start_daily', 'EN')
check('another player gets the same puzzle (still one row)', (await owner(`select count(*) n from public.daily_puzzle where language='EN'`))[0].n === 1)
await call(alice, 'start_daily', 'TR')
check('Turkish has its own puzzle', (await owner(`select count(*) n from public.daily_puzzle`))[0].n === 2)

// invalid guesses cost nothing
for (const bad of ['CRAN', 'CRANES', 'CR4NE', 'ÇAĞRI', ''])
  check(`invalid guess "${bad}" refused`, /invalid guess/.test(await errorOf(alice, `select public.submit_daily_guess('EN','${bad}')`) ?? ''))
check('nothing was counted for the refused guesses', (await call(alice, 'get_daily_state', 'EN')).guesses.length === 0)

// a wrong guess: colored by the database, answer still hidden
const wrong = (await owner(`select word from public.answer_pool where language='EN' and word <> '${answer}' limit 1`))[0].word
const s1 = await call(alice, 'submit_daily_guess', 'EN', wrong.toLowerCase()) // lowercase on purpose
check('wrong guess: PLAYING, one guess, colors from the database',
  s1.status === 'PLAYING' && s1.guesses.length === 1 && s1.guesses[0].word === wrong && s1.guesses[0].feedback === await fb(wrong, answer))
check('answer stays hidden while playing', s1.answer === null && !JSON.stringify(s1).includes(answer))

// the winning guess
const won = await call(alice, 'submit_daily_guess', 'EN', answer)
check('correct guess wins and reveals the answer', won.status === 'WON' && won.answer === answer && won.guesses.length === 2)
check('duration is measured by the database', Number.isInteger(won.duration_ms) && won.duration_ms >= 0 && won.result_id)
check('the finish time is reported and matches start + duration', won.finished_at_ms - won.started_at_ms === won.duration_ms)
check('an unfinished attempt has no finish time or duration', started.finished_at_ms === null && started.duration_ms === null)
const rows = (await as(alice, `select id, mode, outcome, duration_ms, day_index, user_id from public.game_result where mode='DAILY'`)).rows
check('the result was recorded as DAILY with the time', rows.length === 1 && rows[0].id === won.result_id && rows[0].outcome === 'WON' && Number(rows[0].duration_ms) === won.duration_ms && rows[0].user_id === alice)
check('the result carries the puzzle day', rows[0].day_index === won.day_index)
check('bob cannot see alice\'s result', (await as(bob, `select * from public.game_result where mode='DAILY'`)).rows.length === 0)
check('guessing after finishing is refused', /already finished/.test(await errorOf(alice, `select public.submit_daily_guess('EN','${wrong}')`) ?? ''))
check('... with the code PT409 (HTTP 409 in PostgREST)', (await codeOf(alice, `select public.submit_daily_guess('EN','${wrong}')`)) === 'PT409')
const final = await call(alice, 'get_daily_state', 'EN')
check('state afterwards still shows the result', final.status === 'WON' && final.answer === answer && final.duration_ms === won.duration_ms)
check('start after finishing returns the result, no new attempt', (await call(alice, 'start_daily', 'EN')).status === 'WON')
check('a client cannot write a daily result itself',
  (await errorOf(alice, `insert into public.game_result(id,game,mode,language,answer,guesses,outcome,was_skipped,finished_at,day_index,duration_ms)
                         values ('cheat','word','DAILY','EN','CRANE','CRANE','WON',false,1,1,1)`)) !== null)

// losing after six wrong guesses (Turkish, uppercase rules with i / ı)
const trAnswer = (await owner(`select answer from public.daily_puzzle where language='TR'`))[0].answer
const trWrong = (await owner(`select word from public.answer_pool where language='TR' and word <> '${trAnswer}' limit 6`)).map(r => r.word)
let last
for (let i = 0; i < 6; i++) last = await call(alice, 'submit_daily_guess', 'TR', trWrong[i])
check('six wrong guesses lose, and the answer is revealed', last.status === 'LOST' && last.guesses.length === 6 && last.answer === trAnswer)
check('the lost result is recorded', (await as(alice, `select outcome from public.game_result where mode='DAILY' and language='TR'`)).rows[0].outcome === 'LOST')

// Turkish typed in lowercase wins
await call(bob, 'start_daily', 'TR')
const lower = [...trAnswer].map(c => c === 'İ' ? 'i' : c === 'I' ? 'ı' : c.toLowerCase()).join('')
const trWin = await call(bob, 'submit_daily_guess', 'TR', lower)
check(`Turkish lowercase "${lower}" matches ${trAnswer} (i/ı rules)`, trWin.status === 'WON', trWin.status)

// the schedule: no word repeats until the whole pool has been used. Today's puzzle already used one word,
// so the 1985 other words fill the next 1985 days, each exactly once and none of them today's answer.
await owner(`do $$ begin for d in 100000..(100000 + 1984) loop perform public.ensure_daily_puzzle(d, 'EN'); end loop; end $$`)
const distinct = (await owner(`select count(distinct answer) n, count(*) c, count(*) filter (where answer = '${answer}') as today_again from public.daily_puzzle where language='EN' and day_index >= 100000`))[0]
check('the next 1985 days use 1985 different words, none repeating the answer of today', distinct.c === 1985 && distinct.n === 1985 && distinct.today_again === 0, JSON.stringify(distinct))
await owner(`select public.ensure_daily_puzzle(200000, 'EN')`)
check('after the pool is used up a new cycle starts without error', (await owner(`select count(*) n from public.daily_puzzle where day_index = 200000`))[0].n === 1)

console.log(failures === 0 ? '\nALL CHECKS PASSED' : `\n${failures} CHECK(S) FAILED`)
process.exit(failures === 0 ? 0 : 1)
