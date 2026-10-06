-- BrainScroll leaderboards and friends, phase 7.
--
-- What is public here: a username and a result's score (guesses, time). Nothing else about a player leaves the
-- database. Like the daily puzzle, everything goes through functions; the tables have Row Level Security on, no
-- policy and no privileges for app users.
--
-- Rules (docs/decisions.md): only players signed in with Google or Apple can take a username and appear in a
-- table or a friend list. Anonymous players can look at the tables, not appear in them.

-- ---------------------------------------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------------------------------------

create table public.profile (
    user_id     uuid primary key references auth.users (id) on delete cascade,
    -- As typed (3-16 letters, digits, underscore). Unique ignoring case, see the index below.
    username    text not null check (username ~ '^[A-Za-z0-9_]{3,16}$'),
    -- What a friend types or opens as a link to become friends instantly.
    invite_code text not null unique,
    created_at  timestamptz not null default now()
);
create unique index profile_username_lower on public.profile (lower(username));

-- One row per pair, the smaller id first, so a pair can only exist once.
create table public.friendship (
    user_a     uuid not null references auth.users (id) on delete cascade,
    user_b     uuid not null references auth.users (id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_a, user_b),
    check (user_a < user_b)
);
create index friendship_user_b on public.friendship (user_b);

-- A request sent by username, waiting for the other side to accept.
create table public.friend_request (
    from_user  uuid not null references auth.users (id) on delete cascade,
    to_user    uuid not null references auth.users (id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (from_user, to_user),
    check (from_user <> to_user)
);
create index friend_request_to_user on public.friend_request (to_user);

-- The day streak, kept up to date by the database every time a daily result is written. Same rules as the app's
-- DayStreaks: finishing any language's puzzle counts, every 7th day earns a freeze (one kept at most), and a
-- missed day is bridged by a freeze automatically.
create table public.streak_stat (
    user_id  uuid primary key references auth.users (id) on delete cascade,
    last_day integer not null,
    streak   integer not null,
    best     integer not null,
    freezes  integer not null
);

-- Words that cannot be part of a username. Small on purpose: easy to read and to extend with a new migration.
-- Matched as a substring after lowercasing, removing underscores and undoing common digit tricks (0->o, 1->i...).
create table public.blocked_word (
    word text primary key check (word = lower(word))
);

alter table public.profile        enable row level security;
alter table public.friendship     enable row level security;
alter table public.friend_request enable row level security;
alter table public.streak_stat    enable row level security;
alter table public.blocked_word   enable row level security;
revoke all on public.profile, public.friendship, public.friend_request, public.streak_stat, public.blocked_word
    from anon, authenticated;

insert into public.blocked_word (word) values
    -- English
    ('fuck'), ('shit'), ('bitch'), ('cunt'), ('dick'), ('cock'), ('pussy'), ('nigger'), ('nigga'), ('faggot'),
    ('whore'), ('slut'), ('rapist'), ('nazi'), ('hitler'), ('asshole'),
    -- Turkish (without Turkish letters, as usernames only have a-z)
    ('amk'), ('orospu'), ('siktir'), ('sikik'), ('sikeyim'), ('yarrak'), ('yarak'), ('gotveren'), ('ibne'),
    ('pezevenk'), ('amcik'), ('kahpe'), ('gavat'), ('surtuk'),
    -- Names that would pass for the app or its staff
    ('brainscroll'), ('moderator'), ('admin');

-- Leaderboards read finished daily results a lot.
create index game_result_daily on public.game_result (language, day_index)
    where mode = 'DAILY' and outcome = 'WON';

-- ---------------------------------------------------------------------------------------------------------
-- Streaks
-- ---------------------------------------------------------------------------------------------------------

-- Records that a player finished the daily puzzle on a day. Days must come in ascending order; a day that is
-- not newer than the last one changes nothing (the second language of the same day, for example).
create function public.streak_apply(p_user uuid, p_day integer)
    returns void
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    s       public.streak_stat;
    missed  integer;
    new_s   integer;
    new_f   integer;
begin
    select * into s from public.streak_stat where user_id = p_user for update;
    if not found then
        insert into public.streak_stat values (p_user, p_day, 1, 1, 0);
        return;
    end if;
    if p_day <= s.last_day then
        return;
    end if;

    missed := p_day - s.last_day - 1;
    if missed = 0 then
        new_s := s.streak + 1;
        new_f := s.freezes;
    elsif missed <= s.freezes then
        new_s := s.streak + 1;
        new_f := s.freezes - missed;
    else
        new_s := 1;      -- too many missed days: a new streak starts
        new_f := s.freezes;
    end if;
    if new_s % 7 = 0 then
        new_f := least(new_f + 1, 1);
    end if;

    update public.streak_stat set
        last_day = p_day,
        streak = new_s,
        best = greatest(best, new_s),
        freezes = new_f
    where user_id = p_user;
end;
$$;

create function public.streak_on_daily_result()
    returns trigger
    language plpgsql
    security definer
    set search_path = ''
as $$
begin
    perform public.streak_apply(new.user_id, new.day_index);
    return new;
end;
$$;

create trigger streak_on_daily_result
    after insert on public.game_result
    for each row when (new.mode = 'DAILY')
    execute function public.streak_on_daily_result();

-- Players who already finished daily puzzles get their streak from the existing results.
do $$
declare
    r record;
begin
    for r in select distinct user_id, day_index from public.game_result where mode = 'DAILY'
             order by user_id, day_index loop
        perform public.streak_apply(r.user_id, r.day_index);
    end loop;
end;
$$;

-- ---------------------------------------------------------------------------------------------------------
-- Helpers (not callable by app users)
-- ---------------------------------------------------------------------------------------------------------

-- The signed-in user, who must not be anonymous. PT403 becomes HTTP 403 in PostgREST.
create function public.current_member()
    returns uuid
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid uuid := auth.uid();
    anon boolean;
begin
    if uid is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    select u.is_anonymous into anon from auth.users u where u.id = uid;
    if coalesce(anon, true) then
        raise exception 'sign in with Google or Apple first' using errcode = 'PT403';
    end if;
    return uid;
end;
$$;

create function public.username_blocked(p_name text)
    returns boolean
    language sql
    stable
    security definer
    set search_path = ''
as $$
    select exists (
        select 1 from public.blocked_word w
        where position(w.word in translate(replace(lower(p_name), '_', ''), '0134578', 'oieastb')) > 0
    )
$$;

-- The people whose results a "friends" table shows: the player and their friends.
create function public.friend_scope(p_user uuid)
    returns table (user_id uuid)
    language sql
    stable
    security definer
    set search_path = ''
as $$
    select p_user
    union
    select case when f.user_a = p_user then f.user_b else f.user_a end
    from public.friendship f
    where f.user_a = p_user or f.user_b = p_user
$$;

create function public.new_invite_code()
    returns text
    language plpgsql
    volatile
    set search_path = ''
as $$
declare
    alphabet constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    code text;
begin
    loop
        code := '';
        for i in 1..8 loop
            code := code || substr(alphabet, 1 + floor(random() * 32)::integer, 1);
        end loop;
        exit when not exists (select 1 from public.profile where invite_code = code);
    end loop;
    return code;
end;
$$;

-- ---------------------------------------------------------------------------------------------------------
-- Profile
-- ---------------------------------------------------------------------------------------------------------

-- Who am I? username and invite_code are null until a username is chosen.
create function public.get_my_profile()
    returns jsonb
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid uuid := auth.uid();
    anon boolean;
    p   public.profile;
begin
    if uid is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    select u.is_anonymous into anon from auth.users u where u.id = uid;
    select * into p from public.profile where user_id = uid;
    return jsonb_build_object(
        'is_member', not coalesce(anon, true),
        'username', p.username,
        'invite_code', p.invite_code
    );
end;
$$;

-- Takes a username, or changes it. Errors: 22023 invalid, PT422 not allowed, PT409 taken, PT403 anonymous.
create function public.set_username(p_name text)
    returns jsonb
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid  uuid := public.current_member();
    name text := btrim(coalesce(p_name, ''));
begin
    if name !~ '^[A-Za-z0-9_]{3,16}$' then
        raise exception 'invalid username' using errcode = '22023';
    end if;
    if public.username_blocked(name) then
        raise exception 'username not allowed' using errcode = 'PT422';
    end if;

    begin
        insert into public.profile (user_id, username, invite_code)
        values (uid, name, public.new_invite_code())
        on conflict (user_id) do update set username = excluded.username;
    exception when unique_violation then
        raise exception 'username taken' using errcode = 'PT409';
    end;

    return public.get_my_profile();
end;
$$;

-- ---------------------------------------------------------------------------------------------------------
-- Leaderboards
-- ---------------------------------------------------------------------------------------------------------

-- Daily speed: only won puzzles, fewest guesses first, then the shortest time. p_scope is 'TODAY' or 'ALL_TIME'
-- (a player's best single result ever). p_friends limits the table to the player and their friends.
-- Returns {"rows": [top 50], "me": my row or null}; a row is {rank, username, guesses, duration_ms, is_me}.
create function public.get_daily_leaderboard(p_language text, p_scope text, p_friends boolean)
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
    if p_language not in ('EN', 'TR') or p_scope not in ('TODAY', 'ALL_TIME') then
        raise exception 'unknown language or scope' using errcode = '22023';
    end if;
    if p_friends then
        uid := public.current_member();
    end if;

    with eligible as (
        select r.user_id, p.username,
               cardinality(string_to_array(r.guesses, ',')) as guesses,
               r.duration_ms
        from public.game_result r
        join public.profile p on p.user_id = r.user_id
        where r.mode = 'DAILY' and r.outcome = 'WON' and r.language = p_language
          and r.duration_ms is not null
          and (p_scope = 'ALL_TIME' or r.day_index = public.istanbul_day(now()))
          and (not p_friends or r.user_id in (select f.user_id from public.friend_scope(uid) f))
    ),
    best as (
        select distinct on (user_id) * from eligible order by user_id, guesses, duration_ms
    ),
    ranked as (
        select rank() over (order by guesses, duration_ms) as rnk, b.*, (b.user_id = uid) as is_me
        from best b
    )
    select jsonb_build_object(
        'rows', coalesce((select jsonb_agg(jsonb_build_object(
                    'rank', rnk, 'username', username, 'guesses', guesses, 'duration_ms', duration_ms, 'is_me', is_me)
                    order by rnk, username)
                from (select * from ranked order by rnk, username limit 50) top), '[]'::jsonb),
        'me', (select jsonb_build_object(
                    'rank', rnk, 'username', username, 'guesses', guesses, 'duration_ms', duration_ms, 'is_me', true)
                from ranked where is_me)
    ) into result;
    return result;
end;
$$;

-- Streaks: p_kind is 'CURRENT' or 'LONGEST'. Not split by language, because a streak counts either puzzle.
-- Returns {"rows": [top 50], "me": my row or null}; a row is {rank, username, value, is_me}.
create function public.get_streak_leaderboard(p_kind text, p_friends boolean)
    returns jsonb
    language plpgsql
    stable
    security definer
    set search_path = ''
as $$
declare
    uid    uuid := auth.uid();
    today  integer := public.istanbul_day(now());
    result jsonb;
begin
    if uid is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    if p_kind not in ('CURRENT', 'LONGEST') then
        raise exception 'unknown kind' using errcode = '22023';
    end if;
    if p_friends then
        uid := public.current_member();
    end if;

    with eligible as (
        select s.user_id, p.username,
               case p_kind
                   -- Same rule as the app: the open day is not missed yet, missed days need freezes.
                   when 'CURRENT' then case when greatest(0, today - s.last_day - 1) <= s.freezes then s.streak else 0 end
                   else s.best
               end as value
        from public.streak_stat s
        join public.profile p on p.user_id = s.user_id
        where (not p_friends or s.user_id in (select f.user_id from public.friend_scope(uid) f))
    ),
    ranked as (
        select rank() over (order by value desc) as rnk, e.*, (e.user_id = uid) as is_me
        from eligible e
        where e.value > 0
    )
    select jsonb_build_object(
        'rows', coalesce((select jsonb_agg(jsonb_build_object(
                    'rank', rnk, 'username', username, 'value', value, 'is_me', is_me) order by rnk, username)
                from (select * from ranked order by rnk, username limit 50) top), '[]'::jsonb),
        'me', (select jsonb_build_object('rank', rnk, 'username', username, 'value', value, 'is_me', true)
                from ranked where is_me)
    ) into result;
    return result;
end;
$$;

-- ---------------------------------------------------------------------------------------------------------
-- Friends
-- ---------------------------------------------------------------------------------------------------------

create function public.make_friends(p_a uuid, p_b uuid)
    returns void
    language sql
    security definer
    set search_path = ''
as $$
    insert into public.friendship (user_a, user_b) values (least(p_a, p_b), greatest(p_a, p_b))
    on conflict do nothing;
    delete from public.friend_request
    where (from_user = p_a and to_user = p_b) or (from_user = p_b and to_user = p_a);
$$;

-- Using someone's invite code makes both of you friends right away. Returns the new friend's username.
-- Errors: PT404 no such code, 22023 it is your own code, PT403 anonymous or no username yet.
create function public.add_friend_by_code(p_code text)
    returns jsonb
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid   uuid := public.current_member();
    other public.profile;
begin
    if not exists (select 1 from public.profile where user_id = uid) then
        raise exception 'choose a username first' using errcode = 'PT403';
    end if;
    select * into other from public.profile where invite_code = upper(btrim(coalesce(p_code, '')));
    if not found then
        raise exception 'no such invite code' using errcode = 'PT404';
    end if;
    if other.user_id = uid then
        raise exception 'that is your own code' using errcode = '22023';
    end if;
    perform public.make_friends(uid, other.user_id);
    return jsonb_build_object('username', other.username);
end;
$$;

-- Asks a player, by username, to be friends. If they already asked you, you become friends right away.
-- Returns {"username": ..., "status": "FRIENDS" | "REQUESTED"}.
-- Errors: PT404 no such player, 22023 it is you.
create function public.send_friend_request(p_username text)
    returns jsonb
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid   uuid := public.current_member();
    other public.profile;
begin
    if not exists (select 1 from public.profile where user_id = uid) then
        raise exception 'choose a username first' using errcode = 'PT403';
    end if;
    select * into other from public.profile where lower(username) = lower(btrim(coalesce(p_username, '')));
    if not found then
        raise exception 'no such player' using errcode = 'PT404';
    end if;
    if other.user_id = uid then
        raise exception 'that is you' using errcode = '22023';
    end if;

    if exists (select 1 from public.friendship
               where user_a = least(uid, other.user_id) and user_b = greatest(uid, other.user_id))
       or exists (select 1 from public.friend_request where from_user = other.user_id and to_user = uid) then
        perform public.make_friends(uid, other.user_id);
        return jsonb_build_object('username', other.username, 'status', 'FRIENDS');
    end if;

    insert into public.friend_request (from_user, to_user) values (uid, other.user_id) on conflict do nothing;
    return jsonb_build_object('username', other.username, 'status', 'REQUESTED');
end;
$$;

-- Accepts or declines a request someone sent you.
create function public.respond_friend_request(p_username text, p_accept boolean)
    returns void
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid   uuid := public.current_member();
    other uuid;
begin
    select p.user_id into other from public.profile p where lower(p.username) = lower(btrim(coalesce(p_username, '')));
    if other is null or not exists (select 1 from public.friend_request where from_user = other and to_user = uid) then
        raise exception 'no such request' using errcode = 'PT404';
    end if;
    if p_accept then
        perform public.make_friends(uid, other);
    else
        delete from public.friend_request where from_user = other and to_user = uid;
    end if;
end;
$$;

create function public.remove_friend(p_username text)
    returns void
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid   uuid := public.current_member();
    other uuid;
begin
    select p.user_id into other from public.profile p where lower(p.username) = lower(btrim(coalesce(p_username, '')));
    if other is not null then
        delete from public.friendship
        where user_a = least(uid, other) and user_b = greatest(uid, other);
    end if;
end;
$$;

-- Cancels a request you sent.
create function public.cancel_friend_request(p_username text)
    returns void
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid   uuid := public.current_member();
begin
    delete from public.friend_request r
    using public.profile p
    where r.from_user = uid and r.to_user = p.user_id and lower(p.username) = lower(btrim(coalesce(p_username, '')));
end;
$$;

-- Friends and open requests. {"friends": [username], "incoming": [username], "outgoing": [username]}.
create function public.get_friends()
    returns jsonb
    language plpgsql
    stable
    security definer
    set search_path = ''
as $$
declare
    uid uuid := public.current_member();
begin
    return jsonb_build_object(
        'friends', coalesce((select jsonb_agg(p.username order by lower(p.username))
                             from public.friend_scope(uid) f join public.profile p on p.user_id = f.user_id
                             where f.user_id <> uid), '[]'::jsonb),
        'incoming', coalesce((select jsonb_agg(p.username order by lower(p.username))
                              from public.friend_request r join public.profile p on p.user_id = r.from_user
                              where r.to_user = uid), '[]'::jsonb),
        'outgoing', coalesce((select jsonb_agg(p.username order by lower(p.username))
                              from public.friend_request r join public.profile p on p.user_id = r.to_user
                              where r.from_user = uid), '[]'::jsonb)
    );
end;
$$;

-- ---------------------------------------------------------------------------------------------------------
-- Who may call what
-- ---------------------------------------------------------------------------------------------------------
revoke all on function
    public.streak_apply(uuid, integer),
    public.streak_on_daily_result(),
    public.current_member(),
    public.username_blocked(text),
    public.friend_scope(uuid),
    public.new_invite_code(),
    public.make_friends(uuid, uuid),
    public.get_my_profile(),
    public.set_username(text),
    public.get_daily_leaderboard(text, text, boolean),
    public.get_streak_leaderboard(text, boolean),
    public.add_friend_by_code(text),
    public.send_friend_request(text),
    public.respond_friend_request(text, boolean),
    public.remove_friend(text),
    public.cancel_friend_request(text),
    public.get_friends()
from public, anon, authenticated;

grant execute on function
    public.get_my_profile(),
    public.set_username(text),
    public.get_daily_leaderboard(text, text, boolean),
    public.get_streak_leaderboard(text, boolean),
    public.add_friend_by_code(text),
    public.send_friend_request(text),
    public.respond_friend_request(text, boolean),
    public.remove_friend(text),
    public.cancel_friend_request(text),
    public.get_friends()
to authenticated;
