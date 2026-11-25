-- BrainScroll hint packs, docs/pro-plan.md section 11 (Asama B).
--
-- A hint pack is a consumable purchase, so the store cannot give it back after a reinstall: the balance lives here, and
-- this is the only place that knows it. The app can only SPEND (one at a time); only the server adds, when RevenueCat's
-- webhook reports a purchase (edge function `revenuecat-webhook`, which calls credit_hints with the service role).
--
--   hint_balance  one row per player: how many pack hints are left. Never below zero.
--   hint_credit   one row per store transaction already added. Its key makes the webhook idempotent: the same
--                 purchase reported twice adds once.
--
-- Both tables cascade with the account (delete_my_account): the purchased hints go with it.

create table public.hint_balance (
    user_id    uuid primary key references auth.users (id) on delete cascade,
    balance    integer not null default 0 check (balance >= 0),
    updated_at timestamptz not null default now()
);

create table public.hint_credit (
    transaction_id text primary key,
    user_id        uuid not null references auth.users (id) on delete cascade,
    amount         integer not null check (amount > 0),
    created_at     timestamptz not null default now()
);

-- App users reach both only through the functions below.
alter table public.hint_balance enable row level security;
alter table public.hint_credit enable row level security;
revoke all on public.hint_balance, public.hint_credit from anon, authenticated;

-- How many pack hints the caller has. 0 for someone who never bought a pack.
create function public.my_hint_balance()
    returns integer
    language plpgsql
    stable
    security definer
    set search_path = ''
as $$
declare
    uid uuid := auth.uid();
begin
    if uid is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    return coalesce((select b.balance from public.hint_balance b where b.user_id = uid), 0);
end;
$$;

-- Spends one pack hint, atomically. Returns what is left, or NULL when there was none (nothing is spent then).
create function public.spend_hint()
    returns integer
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid  uuid := auth.uid();
    left_ integer;
begin
    if uid is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    update public.hint_balance
       set balance = balance - 1, updated_at = now()
     where user_id = uid and balance > 0
    returning balance into left_;
    return left_;
end;
$$;

-- Adds a purchased pack to a player's balance. Only the webhook calls this (service role).
-- Returns {status, balance}: 'credited', 'duplicate' (this transaction was added before: nothing changes) or
-- 'unknown_user' (the account is gone, for example deleted: nothing is kept).
create function public.credit_hints(p_user uuid, p_transaction text, p_amount integer)
    returns jsonb
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    new_balance integer;
begin
    if p_amount is null or p_amount <= 0 or coalesce(p_transaction, '') = '' then
        raise exception 'bad credit' using errcode = '22023';
    end if;
    -- Seen before wins over everything else, so a replay after a merge or a deletion is quiet too.
    if exists (select 1 from public.hint_credit c where c.transaction_id = p_transaction) then
        return jsonb_build_object('status', 'duplicate',
            'balance', (select b.balance from public.hint_balance b where b.user_id = p_user));
    end if;
    if not exists (select 1 from auth.users u where u.id = p_user) then
        return jsonb_build_object('status', 'unknown_user');
    end if;

    insert into public.hint_credit (transaction_id, user_id, amount) values (p_transaction, p_user, p_amount)
    on conflict (transaction_id) do nothing;
    if not found then
        -- Two reports of the same purchase raced; the other one added it.
        return jsonb_build_object('status', 'duplicate');
    end if;

    insert into public.hint_balance as b (user_id, balance) values (p_user, p_amount)
    on conflict (user_id) do update set balance = b.balance + excluded.balance, updated_at = now()
    returning balance into new_balance;
    return jsonb_build_object('status', 'credited', 'balance', new_balance);
end;
$$;

revoke all on function public.my_hint_balance(), public.spend_hint(), public.credit_hints(uuid, text, integer)
    from public, anon, authenticated;
grant execute on function public.my_hint_balance(), public.spend_hint() to authenticated;
-- credit_hints stays with the owner and the service role only (the webhook).

-- The account merge now takes the anonymous account's hints along: a pack bought before signing in with Apple or Google
-- must not be lost. This is complete_account_merge of 20261010090000 with the hint part added before the old account goes.
create or replace function public.complete_account_merge(p_token text)
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
    n_hints    integer;
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

    -- The bought hints are added together. The credit rows follow, so a replayed webhook for one of them stays a duplicate.
    select coalesce(b.balance, 0) into n_hints from public.hint_balance b where b.user_id = src;
    n_hints := coalesce(n_hints, 0);
    if n_hints > 0 then
        insert into public.hint_balance as b (user_id, balance) values (uid, n_hints)
        on conflict (user_id) do update set balance = b.balance + excluded.balance, updated_at = now();
    end if;
    update public.hint_credit set user_id = uid where user_id = src;

    -- The streak depends on the order of the days, so it is rebuilt from all the daily results now there.
    delete from public.streak_stat where user_id = uid;
    for r in select distinct g.day_index from public.game_result g where g.user_id = uid and g.mode = 'DAILY'
             order by g.day_index loop
        perform public.streak_apply(uid, r.day_index);
    end loop;

    -- Everything else of the anonymous account goes with it (on delete cascade).
    delete from auth.users where id = src;

    return jsonb_build_object('sessions', n_sessions, 'results', n_results, 'favorites', n_favs, 'hints', n_hints);
end;
$$;

revoke all on function public.complete_account_merge(text) from public, anon, authenticated;
grant execute on function public.complete_account_merge(text) to authenticated;
