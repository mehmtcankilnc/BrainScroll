# Supabase

Project: `lxrfnaxecvofmgkdtvrn` (Frankfurt). The anon/publishable key and the URL are public and live in the app code;
nothing secret belongs in this folder. Never commit the service-role key or the database password.

## Apply the migrations (once per new migration)

```bash
npx supabase login
npx supabase link --project-ref lxrfnaxecvofmgkdtvrn   # asks for the database password
npx supabase db push                                    # shows what it will run, asks to confirm
```

`db push` only runs migrations the project has not seen yet. To look at the schema afterwards, open
Dashboard -> Table Editor, or run `npx supabase db diff --linked` to compare it with the files.

## Rules

- A migration is never edited after it has been pushed. A change is a new file in `migrations/`.
- Every table has Row Level Security on and policies for `authenticated` only (anonymous users are `authenticated`).
- `config.toml` mirrors the dashboard's auth settings (site URL, redirect URL, anonymous sign-ins, manual linking).
  The dashboard is the real setting; we do not run `supabase config push`.

## Testing the SQL without a database

`supabase/tests` runs the real migrations in a Postgres engine that needs no Docker and no project (PGlite), and
checks the rules: who can see what, the hidden daily answer, the server clock, the coloring of guesses.

```bash
cd supabase/tests
npm install
npm test
```

Run it after every change to a migration, before `db push`.

## Live check of the daily puzzle

After pushing a migration that touches the daily puzzle, run the real-project smoke test (it creates anonymous test users):

```bash
PYTHONUTF8=1 python supabase/tests/live_daily_check.py
```

## Live check of the leaderboards

After pushing the leaderboards migration, run the real-project smoke test (it signs in as new anonymous users, so it checks
what an anonymous player may and may not do; the full rules are tested offline by `npm test`):

```bash
PYTHONUTF8=1 python supabase/tests/live_social_check.py
```
