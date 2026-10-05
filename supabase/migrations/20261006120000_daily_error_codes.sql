-- Cleaner error codes for the daily puzzle.
--
-- PostgREST answers an unknown SQLSTATE (P0003, P0004 in the first version) with HTTP 500, which looks like a
-- server failure. Codes of the form PTxyz are the way to ask PostgREST for the HTTP status xyz instead, so now:
--   "already finished"  ->  PT409 (HTTP 409 Conflict)
--   "not started"       ->  PT412 (HTTP 412 Precondition Failed)
-- Only the two RAISE lines changed. `create or replace` keeps the function's grants, so nothing else is needed.

create or replace function public.submit_daily_guess(p_language text, p_guess text)
    returns jsonb
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid      uuid := auth.uid();
    guess    text;
    sess  public.daily_session;
    ans      text;
    fb       text;
    done     boolean;
    outcome_ text;
    pattern  text;
begin
    if uid is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    if p_language not in ('EN', 'TR') then
        raise exception 'unknown language' using errcode = '22023';
    end if;

    guess := public.daily_normalize(p_language, coalesce(p_guess, ''));
    pattern := case p_language
        when 'TR' then '^[ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ]{5}$'
        else '^[ABCDEFGHIJKLMNOPQRSTUVWXYZ]{5}$'
    end;
    if guess !~ pattern then
        raise exception 'invalid guess' using errcode = '22023';
    end if;

    -- Lock the row: two phones guessing at the same time are handled one after the other.
    select * into sess from public.daily_session
    where user_id = uid and language = p_language and finished_at is null
    order by day_index desc
    limit 1
    for update;
    if not found then
        -- Either never started, or already finished: tell them apart for a clear message.
        if exists (select 1 from public.daily_session
                   where user_id = uid and language = p_language and day_index = public.istanbul_day(now())) then
            raise exception 'already finished' using errcode = 'PT409';
        end if;
        raise exception 'not started' using errcode = 'PT412';
    end if;

    select answer into ans from public.daily_puzzle where day_index = sess.day_index and language = p_language;
    fb := public.daily_feedback(guess, ans);
    done := (guess = ans) or (coalesce(array_length(sess.guesses, 1), 0) + 1 >= 6);
    outcome_ := case when not done then null when guess = ans then 'WON' else 'LOST' end;

    update public.daily_session set
        guesses = guesses || guess,
        feedback = feedback || fb,
        finished_at = case when done then now() end,
        outcome = outcome_,
        result_id = case when done then gen_random_uuid()::text end
    where user_id = uid and day_index = sess.day_index and language = p_language
    returning * into sess;

    if done then
        -- The result is written here, by the database, with the database's clock. Clients cannot write daily
        -- results themselves (the policy on game_result only allows endless ones).
        insert into public.game_result
            (user_id, id, game, mode, language, answer, guesses, outcome, was_skipped, finished_at, day_index, duration_ms)
        values
            (uid, sess.result_id, 'word', 'DAILY', p_language, ans, array_to_string(sess.guesses, ','),
             outcome_, false, floor(extract(epoch from sess.finished_at) * 1000)::bigint, sess.day_index,
             floor(extract(epoch from (sess.finished_at - sess.started_at)) * 1000)::bigint);
    end if;

    return public.daily_state_json(sess, ans);
end;
$$;
