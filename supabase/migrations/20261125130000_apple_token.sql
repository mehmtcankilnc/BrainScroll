-- Sign in with Apple: the refresh token Apple gave when the player signed in, kept only so the account can be disconnected from
-- Apple when it is deleted. App Store guideline 5.1.1(v) says an app that lets players sign in with Apple must revoke that sign-in
-- when it deletes the account. The app sends the one-time authorization code to the apple-token Edge Function after signing in;
-- the function turns it into this token (a call to Apple, which needs our Apple key) and, on deletion, revokes it at Apple first.
--
-- Nobody but that function reads this: RLS is on with no policy and the app roles have no rights, so only the service role (the
-- Edge Function) can. The row goes away with the account (on delete cascade), and the function removes it after revoking.
create table public.apple_token (
    user_id       uuid primary key references auth.users (id) on delete cascade,
    refresh_token text not null,
    created_at    timestamptz not null default now()
);

alter table public.apple_token enable row level security;
revoke all on public.apple_token from anon, authenticated;
