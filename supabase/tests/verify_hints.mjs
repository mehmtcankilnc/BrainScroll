// Applies the real migrations to an in-memory Postgres that imitates Supabase's auth, then checks the hint packs:
// the balance only goes down for app users, a purchase is added once however often it is reported, and the account
// merge carries the bought hints along. Run: npm install && npm test   (from this folder)
import { PGlite } from '@electric-sql/pglite'
import { readdirSync, readFileSync } from 'node:fs'

const dir = process.argv[2] ?? new URL('../migrations', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')
const db = new PGlite()

await db.exec(`
  create role authenticated nologin;
  create role anon nologin;
  create role service_role nologin;
  create schema auth;
  create table auth.users (id uuid primary key, is_anonymous boolean not null default false);
  create function auth.uid() returns uuid language sql stable
    as $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
  grant usage on schema auth to authenticated, anon;
  grant execute on function auth.uid() to authenticated, anon;
  -- What Supabase does for every function made in public: the Data API roles can run it unless the migration revokes that.
  alter default privileges in schema public grant execute on functions to anon, authenticated, service_role;
`)
const anonA = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' // buys a pack as a guest
const appleB = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb' // the account the player signs in to, with a pack of its own
const anonC = 'cccccccc-cccc-cccc-cccc-cccccccccccc' // never bought anything
const ghost = 'dddddddd-dddd-dddd-dddd-dddddddddddd' // not an account at all
await db.exec(`insert into auth.users (id, is_anonymous) values ('${anonA}', true), ('${appleB}', false), ('${anonC}', true)`)
for (const f of readdirSync(dir).filter(f => f.endsWith('.sql')).sort()) await db.exec(readFileSync(`${dir}/${f}`, 'utf8'))

let failures = 0
const check = (name, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')); if (!ok) failures++ }
const as = async (user, sql, role = 'authenticated') => {
  await db.exec(`set role ${role}; select set_config('request.jwt.claim.sub', '${user ?? ''}', false)`)
  try { return await db.query(sql) } finally { await db.exec('reset role') }
}
const codeOf = async (user, sql, role) => { try { await as(user, sql, role); return null } catch (e) { return e.code ?? e.message } }
const owner = async (sql) => (await db.query(sql)).rows
const balanceOf = async (u) => (await as(u, `select public.my_hint_balance() as b`)).rows[0].b
const spend = async (u) => (await as(u, `select public.spend_hint() as b`)).rows[0].b
const credit = async (u, tx, amount, role = 'service_role') =>
  (await as(null, `select public.credit_hints('${u}', '${tx}', ${amount}) as r`, role)).rows[0].r

// ---- the webhook adds ----
check('nobody has any hints before buying', (await balanceOf(anonA)) === 0)
let r = await credit(anonA, 'tx-1', 20)
check('a purchase is added', r.status === 'credited' && r.balance === 20 && (await balanceOf(anonA)) === 20, JSON.stringify(r))
r = await credit(anonA, 'tx-1', 20)
check('the same purchase reported again adds nothing (duplicate)', r.status === 'duplicate' && (await balanceOf(anonA)) === 20, JSON.stringify(r))
r = await credit(anonA, 'tx-2', 75)
check('another purchase adds to it', r.status === 'credited' && (await balanceOf(anonA)) === 95)
check('an account that is gone gets nothing, quietly', (await credit(ghost, 'tx-ghost', 20)).status === 'unknown_user'
  && (await owner(`select count(*)::int c from public.hint_credit where transaction_id = 'tx-ghost'`))[0].c === 0)
check('a bad amount is refused (22023)', (await codeOf(null, `select public.credit_hints('${anonA}', 'tx-bad', 0)`, 'service_role')) === '22023'
  && (await codeOf(null, `select public.credit_hints('${anonA}', '', 5)`, 'service_role')) === '22023')

// ---- the app can only spend ----
check('an app user cannot add hints', (await codeOf(anonA, `select public.credit_hints('${anonA}', 'tx-self', 1000)`)) !== null
  && (await balanceOf(anonA)) === 95)
check('an anonymous user cannot add hints either', (await codeOf(anonC, `select public.credit_hints('${anonC}', 'tx-self2', 1000)`)) !== null)
check('the tables cannot be read or written by app users',
  (await codeOf(anonA, `select * from public.hint_balance`)) !== null
  && (await codeOf(anonA, `select * from public.hint_credit`)) !== null
  && (await codeOf(anonA, `update public.hint_balance set balance = 999999`)) !== null
  && (await codeOf(anonA, `insert into public.hint_balance (user_id, balance) values ('${anonC}', 50)`)) !== null)
check('a visitor without a session cannot spend or look (28000)', (await codeOf(null, `select public.spend_hint()`)) === '28000'
  && (await codeOf(null, `select public.my_hint_balance()`)) === '28000')
check('spending takes one and says what is left', (await spend(anonA)) === 94 && (await balanceOf(anonA)) === 94)
check('someone with no hints gets NULL and the balance stays 0', (await spend(anonC)) === null && (await balanceOf(anonC)) === 0)
check('spending never goes below zero', await (async () => {
  await credit(anonC, 'tx-c', 2)
  const a = await spend(anonC), b = await spend(anonC), c = await spend(anonC)
  return a === 1 && b === 0 && c === null && (await balanceOf(anonC)) === 0
})())
check('the balance cannot be made negative even by the owner', await (async () => {
  try { await owner(`update public.hint_balance set balance = -1 where user_id = '${anonC}'`); return false } catch { return true }
})())
check('one player cannot spend another\'s hints', (await balanceOf(anonA)) === 94 && (await spend(anonC)) === null && (await balanceOf(anonA)) === 94)

// ---- the account merge ----
await credit(appleB, 'tx-b', 20)
await as(appleB, `select public.set_username('bee')`)
const secret = (await as(anonA, `select public.start_account_merge() as r`)).rows[0].r
const merged = (await as(appleB, `select public.complete_account_merge('${secret}') as r`)).rows[0].r
check('the merge adds the anonymous hints to the signed-in account', (await balanceOf(appleB)) === 20 + 94 && merged.hints === 94, JSON.stringify(merged))
check('the anonymous account and its balance are gone', (await owner(`select count(*)::int c from public.hint_balance where user_id = '${anonA}'`))[0].c === 0)
r = await credit(anonA, 'tx-1', 20)
check('a webhook replay for a purchase made before the merge stays a duplicate', r.status === 'duplicate' && (await balanceOf(appleB)) === 114, JSON.stringify(r))
check('and the credit now belongs to the new account', (await owner(`select count(*)::int c from public.hint_credit where user_id = '${appleB}'`))[0].c === 3)

// A merge where the anonymous account had no pack leaves the other balance alone.
const cSecret = (await as(anonC, `select public.start_account_merge() as r`)).rows[0].r
await owner(`update public.hint_balance set balance = 0 where user_id = '${anonC}'`)
await as(appleB, `select public.complete_account_merge('${cSecret}')`)
check('an anonymous account with no hints changes nothing', (await balanceOf(appleB)) === 114)

// ---- deleting the account ----
await as(appleB, `select public.delete_my_account()`)
check('deleting the account deletes the balance and the credits', (await owner(`select count(*)::int c from public.hint_balance`))[0].c === 0
  && (await owner(`select count(*)::int c from public.hint_credit`))[0].c === 0)
check('a replay for a deleted account is quiet', (await credit(appleB, 'tx-b', 20)).status === 'unknown_user')

console.log(failures === 0 ? '\nALL CHECKS PASSED' : `\n${failures} CHECK(S) FAILED`)
process.exit(failures === 0 ? 0 : 1)
