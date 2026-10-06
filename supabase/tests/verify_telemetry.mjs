// Applies the real migrations to an in-memory Postgres that imitates Supabase's auth, then checks the telemetry:
// only known events, only adding, no way to read back, and no column that could identify a player.
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
  await db.exec(`set role authenticated; select set_config('request.jwt.claim.sub', '${user ?? ''}', false)`)
  try { return await db.query(sql) } finally { await db.exec('reset role') }
}
const codeOf = async (user, sql) => { try { await as(user, sql); return null } catch (e) { return e.code ?? e.message } }
const owner = async (sql) => (await db.query(sql)).rows
const events = (list) => `select public.log_events('android', '1.0.3', '${JSON.stringify(list)}'::jsonb)`

// ---- adding events ----
check('a batch of known events is stored', (await codeOf(alice, events([
  { name: 'app_open' }, { name: 'daily_finished', props: { outcome: 'WON', guesses: 3 } }, { name: 'tab_viewed', props: { tab: 'ranks' } }]))) === null)
const rows = await owner(`select name, platform, app_version, props from public.app_event order by id`)
check('with platform, version and the small values', rows.length === 3 && rows[1].platform === 'android' && rows[1].app_version === '1.0.3' && rows[1].props.guesses === 3, JSON.stringify(rows[1]))
check('an event without values gets an empty object', JSON.stringify(rows[0].props) === '{}')

check('an unknown event name refuses the whole batch', (await codeOf(alice, events([{ name: 'app_open' }, { name: 'secret_tracking' }]))) !== null
  && Number((await owner(`select count(*) c from public.app_event`))[0].c) === 3)
check('an unknown platform is refused', (await codeOf(alice, `select public.log_events('toaster', '1', '[{"name":"app_open"}]'::jsonb)`)) !== null)
check('values that are not an object are refused', (await codeOf(alice, events([{ name: 'app_open', props: [1, 2] }]))) !== null)
check('values that are too long are refused', (await codeOf(alice, events([{ name: 'app_open', props: { x: 'y'.repeat(400) } }]))) !== null)
check('a batch over 50 events is refused (22023)', (await codeOf(alice, events(Array.from({ length: 51 }, () => ({ name: 'app_open' }))))) === '22023')
check('50 events are fine', (await codeOf(alice, events(Array.from({ length: 50 }, () => ({ name: 'app_open' }))))) === null)
check('a version string that is too long is refused', (await codeOf(alice, `select public.log_events('ios', '${'9'.repeat(40)}', '[{"name":"app_open"}]'::jsonb)`)) !== null)

// ---- crashes ----
check('a crash report is stored', (await codeOf(alice, `select public.log_crash('ios', '1.0.3', 'IllegalStateException', 'boom', 'at a.b.C.d(C.kt:1)')`)) === null)
check('long texts are cut, not refused', (await codeOf(alice, `select public.log_crash('ios', '1.0.3', '${'k'.repeat(300)}', '${'m'.repeat(900)}', '${'s'.repeat(9000)}')`)) === null)
const crash = (await owner(`select kind, message, stack from public.crash_report order by id desc limit 1`))[0]
check('... to 200, 500 and 8000 characters', crash.kind.length === 200 && crash.message.length === 500 && crash.stack.length === 8000)
check('an unknown platform in a crash is refused', (await codeOf(alice, `select public.log_crash('toaster', '1', 'k', 'm', 's')`)) !== null)

// ---- nothing identifies a player, nothing can be read back ----
const columns = (await owner(`select table_name, column_name from information_schema.columns where table_schema = 'public' and table_name in ('app_event', 'crash_report')`))
check('no column that could hold a user or device id', !columns.some(c => /user|device|uid|ip|account|email/i.test(c.column_name)), columns.map(c => c.column_name).join())
for (const sql of ['select * from public.app_event', 'select * from public.crash_report', 'select * from public.event_name',
  `insert into public.app_event (name, platform, app_version) values ('app_open','ios','1')`, `delete from public.app_event`, `update public.app_event set name = 'tab_viewed'`,
  `insert into public.event_name values ('mine')`])
  check('app user blocked: ' + sql.slice(0, 50), (await codeOf(alice, sql)) !== null)
check('visitors without a session cannot log events (28000)', (await codeOf(null, events([{ name: 'app_open' }]))) === '28000')
check('... or crashes (28000)', (await codeOf(null, `select public.log_crash('ios','1','k','m','s')`)) === '28000')
await db.exec('set role anon')
let anonBlocked = true
try { await db.query(events([{ name: 'app_open' }])); anonBlocked = false } catch { /* expected */ }
await db.exec('reset role')
check('the anon role cannot log either', anonBlocked)
check('purge_telemetry is not callable by app users', (await codeOf(alice, `select public.purge_telemetry()`)) !== null)

// ---- the 90 day rule ----
await owner(`update public.app_event set at = now() - interval '91 days' where id in (select id from public.app_event order by id limit 5)`)
await owner(`update public.crash_report set at = now() - interval '100 days' where id = (select min(id) from public.crash_report)`)
const beforeEvents = Number((await owner(`select count(*) c from public.app_event`))[0].c)
await owner(`select public.purge_telemetry()`)
check('purge removes what is older than 90 days and keeps the rest',
  Number((await owner(`select count(*) c from public.app_event`))[0].c) === beforeEvents - 5
  && Number((await owner(`select count(*) c from public.crash_report`))[0].c) === 1)

console.log(failures === 0 ? '\nALL CHECKS PASSED' : `\n${failures} CHECK(S) FAILED`)
process.exit(failures === 0 ? 0 : 1)
