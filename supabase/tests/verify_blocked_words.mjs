// Applies the real migrations to an in-memory Postgres and checks the username filter: bad words are refused whatever the
// capitals, underscores or digit tricks, and ordinary names (and names that only contain a bad word's letters) are not.
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
  alter default privileges in schema public grant execute on functions to anon, authenticated, service_role;
`)
for (const f of readdirSync(dir).filter(f => f.endsWith('.sql')).sort()) await db.exec(readFileSync(`${dir}/${f}`, 'utf8'))

let failures = 0
const check = (name, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')); if (!ok) failures++ }
const blocked = async (name) => (await db.query(`select public.username_blocked('${name}') as b`)).rows[0].b

// the first list still works
for (const n of ['fuck', 'SH1T', 'f_u_c_k', 'siktir', 'AMK', 'Hitler99', 'admin', 'BrainScroll']) check(`blocked: ${n}`, (await blocked(n)) === true)
// the new words
for (const n of ['wanker', 'Bastard1', 'porn_star', 'yavsak_42', 'dangalak', 'S1kerim', 'aminakoyim', 'dalyarak', 'pedophile', 'blowjob'])
  check(`blocked (new): ${n}`, (await blocked(n)) === true)
// ordinary names must pass: names, and words that only share letters with a bad one
for (const n of ['carol', 'Amina', 'Torpedo', 'peninsula', 'grape', 'Capital', 'epic_player', 'Spice', 'ali_veli', 'Zeynep', 'mehmet_42', 'Pedro', 'bastille'])
  check(`allowed: ${n}`, (await blocked(n)) === false)

console.log(failures ? `\n${failures} CHECK(S) FAILED` : '\nALL CHECKS PASSED')
process.exit(failures ? 1 : 0)
