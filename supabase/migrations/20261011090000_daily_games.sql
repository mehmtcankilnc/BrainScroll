-- BrainScroll daily puzzle, every game (docs/decisions.md "Günün bulmacası: oyun rotasyonu").
--
-- The daily puzzle is not only the word puzzle any more: the app picks a game for each day from the day number (every
-- game once before any comes again) and builds the same board for everyone from that number. The word puzzle keeps its
-- own server-side flow (secret answer, server clock). For the other games there is nothing secret to keep, and the
-- time is measured on the device, so the server only does two things here:
--
--   1. takes a finished result, checks that it is plausible, and stores it in game_result as a DAILY result.
--      That is also what moves the day streak (the trigger on game_result) and puts the player on the speed table.
--   2. answers the speed table of one game, today or of all time.
--
-- Because the time is the device's word, a result that is too quick to be real is refused, and a player gets one result
-- of this kind per day. Friends' tables are where this matters most, and there it is a game between friends.

-- ---------------------------------------------------------------------------------------------------------
-- Tables: the speed tables read finished daily results of one game.
-- ---------------------------------------------------------------------------------------------------------
create index game_result_daily_game on public.game_result (game, day_index)
    where mode = 'DAILY' and outcome = 'WON';

-- ---------------------------------------------------------------------------------------------------------
-- Submit a finished daily result of a game other than the word puzzle.
-- ---------------------------------------------------------------------------------------------------------
-- p_moves is the number of moves, p_duration_ms the time the player took (measured by the app). Asking twice on the
-- same day changes nothing. Returns nothing; refused results raise 22023.
create function public.submit_daily_game_result(
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
    if p_game not in ('memory', 'tubes', 'mines', 'sudoku', 'lights', 'slide', 'target', 'pipes')
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

-- ---------------------------------------------------------------------------------------------------------
-- The speed table of one game: only won puzzles, the shortest time first, then the fewest moves.
-- ---------------------------------------------------------------------------------------------------------
-- p_scope is 'TODAY' or 'ALL_TIME' (a player's best single result ever in that game). p_friends limits the table to
-- the player and their friends. Same shape as get_daily_leaderboard, with moves instead of guesses:
-- {"rows": [top 50], "me": my row or null}; a row is {rank, username, moves, duration_ms, is_me}.
create function public.get_daily_game_leaderboard(p_game text, p_scope text, p_friends boolean)
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
    if p_game not in ('memory', 'tubes', 'mines', 'sudoku', 'lights', 'slide', 'target', 'pipes')
       or p_scope not in ('TODAY', 'ALL_TIME') then
        raise exception 'unknown game or scope' using errcode = '22023';
    end if;
    if p_friends then
        uid := public.current_member();
    end if;

    with eligible as (
        select r.user_id, p.username,
               (case when r.guesses ~ '^[0-9]+$' then r.guesses::integer end) as moves, r.duration_ms
        from public.game_result r
        join public.profile p on p.user_id = r.user_id
        where r.mode = 'DAILY' and r.outcome = 'WON' and r.game = p_game
          and r.duration_ms is not null and r.guesses ~ '^[0-9]+$'
          and (p_scope = 'ALL_TIME' or r.day_index = public.istanbul_day(now()))
          and (not p_friends or r.user_id in (select f.user_id from public.friend_scope(uid) f))
    ),
    best as (
        select distinct on (user_id) * from eligible order by user_id, duration_ms, moves
    ),
    ranked as (
        select rank() over (order by duration_ms, moves) as rnk, b.*, (b.user_id = uid) as is_me
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
-- Who may call what
-- ---------------------------------------------------------------------------------------------------------
revoke all on function
    public.submit_daily_game_result(text, text, integer, bigint, text),
    public.get_daily_game_leaderboard(text, text, boolean)
from public, anon, authenticated;

grant execute on function
    public.submit_daily_game_result(text, text, integer, bigint, text),
    public.get_daily_game_leaderboard(text, text, boolean)
to authenticated;
