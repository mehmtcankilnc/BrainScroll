-- BrainScroll: the table of every game of the feed (docs/siralama-plani.md).
--
-- Until now the speed tables only knew the daily puzzle (mode DAILY): a game played in the feed (mode ENDLESS) was saved and backed up but
-- never ranked, so every table but the one of the daily puzzle said "no one is on this table yet". The results of the feed are already
-- on the server (the backup writes them to game_result with their time, their level and whether the guide was used), so the tables can
-- simply read them. Nothing new is sent by the app.
--
-- What a table is, for the game p_game at the level p_difficulty (0 easy, 1 medium, 2 hard):
--   * only players who have a username (a row of public.profile), the best result of each player;
--   * never a result in which the guide was used (was_skipped);
--   * a result needs a time (between one second and a day), so a result with no time or a made-up one is left out;
--   * the games of a series (flashgrid, oddone, colorseq, cubefold): the most rounds first, then the shortest time. Any result counts, won
--     or lost, as long as it has at least one round (a lost series is ranked by how far it got);
--   * the word puzzle (word): the fewest guesses first, then the shortest time, won puzzles only, in the language p_language;
--   * every other game: the shortest time first, then the fewest moves, won puzzles only.
-- Results of both modes count (a daily result is a result). p_scope TODAY looks at the Istanbul day of now(), ALL_TIME at everything.
-- The shape of the answer is the one of get_daily_game_leaderboard: {"rows": [top 50], "me": my row or null}; a row is
-- {rank, username, moves, duration_ms, is_me} where `moves` is the rounds of a series, the guesses of the word puzzle, else the moves.

create or replace function public.get_game_leaderboard(p_game text, p_scope text, p_friends boolean, p_difficulty integer, p_language text default null)
    returns jsonb
    language plpgsql
    stable
    security definer
    set search_path = ''
as $$
declare
    uid    uuid := auth.uid();
    result jsonb;
    series boolean := p_game in ('flashgrid', 'oddone', 'colorseq', 'cubefold');
begin
    if uid is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    if p_game !~ '^[a-z]{2,32}$'
       or p_game in ('line', 'crossword')
       or p_scope not in ('TODAY', 'ALL_TIME')
       or p_difficulty not between 0 and 2
       or (p_language is not null and p_language not in ('EN', 'TR')) then
        raise exception 'unknown game or scope' using errcode = '22023';
    end if;
    if p_friends then
        uid := public.current_member();
    end if;

    with raw as (
        select r.user_id, p.username, r.duration_ms, r.outcome,
               (case p_game
                    when 'word' then cardinality(string_to_array(r.guesses, ','))
                    when 'memory' then cardinality(string_to_array(r.guesses, ',')) / 2
                    else (case when split_part(r.guesses, ',', 1) ~ '^[0-9]{1,9}$' then split_part(r.guesses, ',', 1)::integer end)
                end) as moves
        from public.game_result r
        join public.profile p on p.user_id = r.user_id
        where r.game = p_game
          and not r.was_skipped
          and r.difficulty = p_difficulty
          and r.duration_ms between 1000 and 86400000
          and (p_language is null or r.language = p_language)
          and (p_scope = 'ALL_TIME' or r.day_index = public.istanbul_day(now()))
          and (not p_friends or r.user_id in (select f.user_id from public.friend_scope(uid) f))
    ),
    eligible as (
        select user_id, username, duration_ms, moves,
               -- the two sort keys, smaller is better
               (case when series then -moves when p_game = 'word' then moves else duration_ms end)::bigint as k1,
               (case when series or p_game = 'word' then duration_ms else moves end)::bigint as k2
        from raw
        where moves is not null
          and (case when series then moves >= 1 else outcome = 'WON' end)
    ),
    best as (
        select distinct on (user_id) * from eligible order by user_id, k1, k2
    ),
    ranked as (
        select rank() over (order by k1, k2) as rnk, b.*, (b.user_id = uid) as is_me
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

-- An index so the table of one game at one level reads only its rows (the table grows with every puzzle played).
create index if not exists game_result_board_idx on public.game_result (game, difficulty, day_index);

-- ---------------------------------------------------------------------------------------------------------
revoke all on function public.get_game_leaderboard(text, text, boolean, integer, text) from public, anon, authenticated;
grant execute on function public.get_game_leaderboard(text, text, boolean, integer, text) to authenticated;
