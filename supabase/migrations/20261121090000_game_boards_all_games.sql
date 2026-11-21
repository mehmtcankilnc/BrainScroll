-- BrainScroll: the speed table of a game, for every game (docs/yeni-oyunlar-plan-3.md).
--
-- get_daily_game_leaderboard only knew the games of the old daily rotation, plus 'line' and 'crossword'. Every game added since
-- (hanoi, queens, tents, ...) was answered with "unknown game" (22023), which the app shows as "the table can't be loaded".
-- Now any game id of the right shape is accepted. A game with no results simply has an empty table ("no one is on this table yet").
-- The function is otherwise the one of 20261016090000_crossword_daily.sql, unchanged.

-- The speed table of one game: only won puzzles. Shortest time first, then the fewest moves; for 'line' and 'crossword' the most stars
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
    if p_game !~ '^[a-z]{2,32}$'
       or p_scope not in ('TODAY', 'ALL_TIME') then
        raise exception 'unknown game or scope' using errcode = '22023';
    end if;
    if p_friends then
        uid := public.current_member();
    end if;

    with eligible as (
        select r.user_id, p.username,
               (case when p_game = 'line' then public.line_stars(r.guesses::integer)
                    when p_game = 'crossword' then public.crossword_stars(r.guesses::integer)
                    else r.guesses::integer end) as moves,
               (case when p_game = 'line' then -public.line_stars(r.guesses::integer)
                    when p_game = 'crossword' then -public.crossword_stars(r.guesses::integer)
                    else 0 end) as stars_key,
               r.duration_ms
        from public.game_result r
        join public.profile p on p.user_id = r.user_id
        where r.mode = 'DAILY' and r.outcome = 'WON' and r.game = p_game
          and r.duration_ms is not null and r.guesses ~ '^[0-9]+$' and length(r.guesses) <= 9
          and (p_game not in ('line', 'crossword') or (case when r.guesses ~ '^[0-9]+$' and length(r.guesses) <= 9 then r.guesses::integer end) >= 1)
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
revoke all on function public.get_daily_game_leaderboard(text, text, boolean) from public, anon, authenticated;
grant execute on function public.get_daily_game_leaderboard(text, text, boolean) to authenticated;
