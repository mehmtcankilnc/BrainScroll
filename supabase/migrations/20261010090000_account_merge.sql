-- BrainScroll account merge, phase 8.
--
-- The problem: someone plays anonymously, then signs in with a Google or Apple account that already exists (for
-- example from another phone). The app then switches to that existing account, and what the anonymous account
-- holds on the server would be left behind: above all the daily results, which only the server can write.
--
-- The solution is a two-step handover that proves the same person is behind both accounts:
--   1. While still signed in anonymously, the app asks start_account_merge() for a one-time ticket (a long random
--      secret, valid for 30 minutes). Only a hash of it is stored.
--   2. After signing in to the existing account, the app hands the ticket to complete_account_merge(). The server
--      then moves everything from the anonymous account into the signed-in one and deletes the anonymous one.
-- Without the ticket nobody can take over an anonymous account, and a ticket works once.

create table public.merge_ticket (
    token_hash text primary key,
    from_user  uuid not null references auth.users (id) on delete cascade,
    expires_at timestamptz not null
);

alter table public.merge_ticket enable row level security;
revoke all on public.merge_ticket from anon, authenticated;

-- Step 1: called by the anonymous account. Returns the secret, once.
create function public.start_account_merge()
    returns text
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid     uuid := auth.uid();
    anon    boolean;
    secret  text;
begin
    if uid is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    select u.is_anonymous into anon from auth.users u where u.id = uid;
    if not coalesce(anon, false) then
        raise exception 'only an anonymous account can be merged' using errcode = 'PT403';
    end if;

    -- One open ticket per account, and no old ones lying around.
    delete from public.merge_ticket where from_user = uid or expires_at < now();

    secret := replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', '');
    insert into public.merge_ticket (token_hash, from_user, expires_at)
    values (encode(sha256(convert_to(secret, 'UTF8')), 'hex'), uid, now() + interval '30 minutes');
    return secret;
end;
$$;

-- Step 2: called by the account the person signed in to. Moves the anonymous account's data into it.
-- Errors: PT403 the caller is anonymous, PT404 the ticket is unknown, used or expired.
-- Where both accounts have a daily result for the same day and language, the signed-in account's one is kept.
create function public.complete_account_merge(p_token text)
    returns jsonb
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid        uuid := public.current_member();
    ticket     public.merge_ticket;
    src        uuid;
    n_sessions integer;
    n_results  integer;
    n_favs     integer;
    r          record;
begin
    select * into ticket from public.merge_ticket
    where token_hash = encode(sha256(convert_to(coalesce(p_token, ''), 'UTF8')), 'hex') and expires_at > now()
    for update;
    if not found then
        raise exception 'no such merge ticket' using errcode = 'PT404';
    end if;
    -- A ticket is used once, whatever happens next.
    delete from public.merge_ticket where token_hash = ticket.token_hash;

    src := ticket.from_user;
    if src = uid then
        raise exception 'that is the same account' using errcode = '22023';
    end if;
    if not exists (select 1 from auth.users u where u.id = src and u.is_anonymous) then
        raise exception 'no such merge ticket' using errcode = 'PT404';
    end if;

    insert into public.daily_session (user_id, day_index, language, started_at, guesses, feedback, finished_at, outcome, result_id)
    select uid, s.day_index, s.language, s.started_at, s.guesses, s.feedback, s.finished_at, s.outcome, s.result_id
    from public.daily_session s
    where s.user_id = src
      -- Not where the signed-in account already has a result for that day and language: its own one is kept.
      and not exists (
            select 1 from public.game_result t
            where t.user_id = uid and t.mode = 'DAILY' and t.language = s.language and t.day_index = s.day_index)
    on conflict (user_id, day_index, language) do nothing;
    get diagnostics n_sessions = row_count;

    insert into public.game_result (user_id, id, game, mode, language, answer, guesses, outcome, was_skipped, finished_at, day_index, duration_ms, created_at)
    select uid, g.id, g.game, g.mode, g.language, g.answer, g.guesses, g.outcome, g.was_skipped, g.finished_at, g.day_index, g.duration_ms, g.created_at
    from public.game_result g
    where g.user_id = src
      and (g.mode = 'ENDLESS' or not exists (
            select 1 from public.game_result t
            where t.user_id = uid and t.mode = 'DAILY' and t.language = g.language and t.day_index = g.day_index))
    on conflict (user_id, id) do nothing;
    get diagnostics n_results = row_count;

    insert into public.favorite (user_id, result_id, is_favorite, updated_at)
    select uid, f.result_id, f.is_favorite, f.updated_at from public.favorite f where f.user_id = src
    on conflict (user_id, result_id) do update
        set is_favorite = excluded.is_favorite, updated_at = excluded.updated_at
        where excluded.updated_at > public.favorite.updated_at;
    get diagnostics n_favs = row_count;

    -- The streak depends on the order of the days, so it is rebuilt from all the daily results now there.
    delete from public.streak_stat where user_id = uid;
    for r in select distinct g.day_index from public.game_result g where g.user_id = uid and g.mode = 'DAILY'
             order by g.day_index loop
        perform public.streak_apply(uid, r.day_index);
    end loop;

    -- Everything else of the anonymous account goes with it (on delete cascade).
    delete from auth.users where id = src;

    return jsonb_build_object('sessions', n_sessions, 'results', n_results, 'favorites', n_favs);
end;
$$;

revoke all on function public.start_account_merge(), public.complete_account_merge(text) from public, anon, authenticated;
grant execute on function public.start_account_merge(), public.complete_account_merge(text) to authenticated;
