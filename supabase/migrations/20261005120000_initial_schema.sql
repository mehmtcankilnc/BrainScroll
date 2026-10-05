-- BrainScroll cloud schema, phase 5.
--
-- The database on the phone is the source of truth. These tables are a per-user backup, so a second device
-- (or a reinstall) can restore the history. Every row belongs to one user and Row Level Security makes sure
-- a user can only ever see and write their own rows. Anonymous users are real users with the `authenticated`
-- role, so the same policies apply to them.

-- ---------------------------------------------------------------------------------------------------------
-- Finished puzzles. Append-only: there is no update and no delete policy.
-- ---------------------------------------------------------------------------------------------------------
create table public.game_result (
    user_id     uuid    not null default auth.uid() references auth.users (id) on delete cascade,
    -- Random id created on the device, so uploading the same result twice never creates a duplicate.
    id          text    not null check (char_length(id) between 1 and 64),
    game        text    not null check (char_length(game) between 1 and 32),
    mode        text    not null check (mode in ('ENDLESS', 'DAILY')),
    language    text    not null check (language in ('EN', 'TR')),
    answer      text    not null check (char_length(answer) between 1 and 32),
    -- Accepted guesses, comma separated.
    guesses     text    not null check (char_length(guesses) <= 256),
    outcome     text    not null check (outcome in ('WON', 'LOST')),
    was_skipped boolean not null,
    -- Epoch milliseconds as measured on the device, and the Istanbul day index derived from it.
    finished_at bigint  not null,
    day_index   integer not null,
    created_at  timestamptz not null default now(),
    primary key (user_id, id)
);

alter table public.game_result enable row level security;

create policy "Users read their own results"
    on public.game_result for select to authenticated
    using (user_id = (select auth.uid()));

-- Clients may only upload endless results. Daily results are written by the server (phase 6), because their
-- time and score must not be trusted from the client.
create policy "Users add their own endless results"
    on public.game_result for insert to authenticated
    with check (user_id = (select auth.uid()) and mode = 'ENDLESS');

-- ---------------------------------------------------------------------------------------------------------
-- Favorites. Last writer wins: a row is never deleted, `is_favorite` is flipped and `updated_at` decides.
-- ---------------------------------------------------------------------------------------------------------
create table public.favorite (
    user_id     uuid    not null default auth.uid() references auth.users (id) on delete cascade,
    result_id   text    not null check (char_length(result_id) between 1 and 64),
    is_favorite boolean not null,
    -- Epoch milliseconds on the device when the heart was tapped.
    updated_at  bigint  not null,
    primary key (user_id, result_id)
);

alter table public.favorite enable row level security;

create policy "Users read their own favorites"
    on public.favorite for select to authenticated
    using (user_id = (select auth.uid()));

create policy "Users add their own favorites"
    on public.favorite for insert to authenticated
    with check (user_id = (select auth.uid()));

create policy "Users change their own favorites"
    on public.favorite for update to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

-- An older write that arrives late (a device that was offline) must not overwrite a newer one.
create function public.favorite_keep_newest()
    returns trigger
    language plpgsql
    set search_path = ''
as $$
begin
    if new.updated_at < old.updated_at then
        return old; -- keep the stored row, ignore the stale update
    end if;
    return new;
end;
$$;

create trigger favorite_keep_newest
    before update on public.favorite
    for each row execute function public.favorite_keep_newest();

-- ---------------------------------------------------------------------------------------------------------
-- Privileges. Row Level Security above decides which rows; these grants decide which operations exist at all
-- for a logged-in user (anonymous users included). Visitors without a session (`anon`) get nothing. They are
-- spelled out so the tables work whether or not the project exposes new tables to the API automatically.
-- ---------------------------------------------------------------------------------------------------------
revoke all on public.game_result, public.favorite from anon;
grant select, insert on public.game_result to authenticated;
grant select, insert, update on public.favorite to authenticated;
