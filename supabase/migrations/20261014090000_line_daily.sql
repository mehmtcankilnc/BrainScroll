-- BrainScroll daily puzzle: One Line (docs/tek-cizgi-plan.md).
--
-- One Line is now the daily puzzle every day. Its result goes through the same two functions as the other games, with two
-- differences:
--
--   * the number the app sends as `moves` is not a move count but a score code: 1 + undos + 100 * hints (see LineScoring
--     in the app: undos up to 99, hints up to 9). It is what lets the server work out the stars the same way the app does.
--   * the speed table of 'line' is ordered by stars first (more is better), then by time; the number a row carries as
--     `moves` is its stars (0 to 3).
--
-- The two functions of 20261011090000_daily_games.sql are replaced, not added to, so everything else stays as it was.

-- Stars from a score code: 3 without an undo, 2 with up to three, 1 with more; a hint takes one off, never below 0.
create function public.line_stars(p_code integer)
    returns integer
    language sql
    immutable
    set search_path = ''
as $$
    select greatest(0,
        (case when (p_code - 1) % 100 = 0 then 3 when (p_code - 1) % 100 <= 3 then 2 else 1 end)
        - (case when (p_code - 1) / 100 > 0 then 1 else 0 end))
$$;

create or replace function public.submit_daily_game_result(
    p_game text, p_outcome text, p_moves integer, p_duration_ms bigint, p_language text)
    returns void
    language plpgsql
    volatile
    security definer
    set search_path = ''
as $$
declare
    uid   uuid := auth.uid();
    today integer := public.istanbul_day(now());
begin
    if uid is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    if p_game not in ('memory', 'tubes', 'mines', 'sudoku', 'lights', 'slide', 'target', 'pipes', 'line')
       or p_outcome not in ('WON', 'LOST')
       or p_language not in ('EN', 'TR') then
        raise exception 'unknown game, outcome or language' using errcode = '22023';
    end if;
    -- A real game takes at least a second or two and some moves; a day has 24 hours.
    if p_moves is null or p_moves < 1 or p_moves > 5000
       or p_duration_ms is null or p_duration_ms < 1500 or p_duration_ms > 86400000 then
        raise exception 'implausible result' using errcode = '22023';
    end if;

    -- One result of a game other than the word puzzle per player and day.
    if exists (select 1 from public.game_result r
               where r.user_id = uid and r.mode = 'DAILY' and r.day_index = today and r.game <> 'word') then
        return;
    end if;

    insert into public.game_result
        (user_id, id, game, mode, language, answer, guesses, outcome, was_skipped, finished_at, day_index, duration_ms)
    values
        (uid, 'daily-' || today || '-' || p_game, p_game, 'DAILY', p_language, 'daily', p_moves::text, p_outcome,
         false, floor(extract(epoch from now()) * 1000)::bigint, today, p_duration_ms)
    on conflict do nothing;
end;
$$;

-- The speed table of one game: only won puzzles. Shortest time first, then the fewest moves; for 'line' the most stars
-- first, then the shortest time (and a row's `moves` is its stars). Same result shape as before.
create or replace function public.get_daily_game_leaderboard(p_game text, p_scope text, p_friends boolean)
    returns jsonb
    language plpgsql
    stable
    security definer
    set search_path = ''
as $$
declare
    uid    uuid := auth.uid();
    result jsonb;
begin
    if uid is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    if p_game not in ('memory', 'tubes', 'mines', 'sudoku', 'lights', 'slide', 'target', 'pipes', 'line')
       or p_scope not in ('TODAY', 'ALL_TIME') then
        raise exception 'unknown game or scope' using errcode = '22023';
    end if;
    if p_friends then
        uid := public.current_member();
    end if;

    with eligible as (
        select r.user_id, p.username,
               (case when p_game = 'line' then public.line_stars(r.guesses::integer) else r.guesses::integer end) as moves,
               (case when p_game = 'line' then -public.line_stars(r.guesses::integer) else 0 end) as stars_key,
               r.duration_ms
        from public.game_result r
        join public.profile p on p.user_id = r.user_id
        where r.mode = 'DAILY' and r.outcome = 'WON' and r.game = p_game
          and r.duration_ms is not null and r.guesses ~ '^[0-9]+$' and length(r.guesses) <= 9
          and (p_game <> 'line' or (case when r.guesses ~ '^[0-9]+$' and length(r.guesses) <= 9 then r.guesses::integer end) >= 1)
          and (p_scope = 'ALL_TIME' or r.day_index = public.istanbul_day(now()))
          and (not p_friends or r.user_id in (select f.user_id from public.friend_scope(uid) f))
    ),
    best as (
        select distinct on (user_id) * from eligible order by user_id, stars_key, duration_ms, moves
    ),
    ranked as (
        select rank() over (order by stars_key, duration_ms, moves) as rnk, b.*, (b.user_id = uid) as is_me
        from best b
    )
    select jsonb_build_object(
        'rows', coalesce((select jsonb_agg(jsonb_build_object(
                    'rank', rnk, 'username', username, 'moves', moves, 'duration_ms', duration_ms, 'is_me', is_me)
                    order by rnk, username)
                from (select * from ranked order by rnk, username limit 50) top), '[]'::jsonb),
        'me', (select jsonb_build_object(
                    'rank', rnk, 'username', username, 'moves', moves, 'duration_ms', duration_ms, 'is_me', true)
                from ranked where is_me)
    ) into result;
    return result;
end;
$$;

-- ---------------------------------------------------------------------------------------------------------
-- Who may call what (the replaced functions keep their grants; this makes it explicit)
-- ---------------------------------------------------------------------------------------------------------
revoke all on function public.line_stars(integer) from public, anon, authenticated;
revoke all on function
    public.submit_daily_game_result(text, text, integer, bigint, text),
    public.get_daily_game_leaderboard(text, text, boolean)
from public, anon, authenticated;

grant execute on function
    public.submit_daily_game_result(text, text, integer, bigint, text),
    public.get_daily_game_leaderboard(text, text, boolean)
to authenticated;
