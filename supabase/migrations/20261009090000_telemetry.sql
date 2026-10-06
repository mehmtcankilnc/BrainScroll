-- BrainScroll anonymous usage counts and crash reports, phase 8.
--
-- What is stored, and what is NOT: an event is a name from a short fixed list, the platform, the app version and at
-- most a few small values (an outcome, a number of guesses). A crash is the platform, the app version and the error
-- text with its stack. There is NO user id, no device id and no IP address in these tables, and nothing links a row
-- to a player or to another row. So they say "how many puzzles were finished today", never "who".
--
-- App users can only ADD rows, through the two functions below. They cannot read anything back. The owner reads
-- the tables in the Supabase dashboard (SQL editor). Rows older than 90 days are removed by purge_telemetry(), see the
-- privacy policy on the website.

create table public.event_name (
    name text primary key
);

-- The only events the app may send. A new kind of event is a new migration that adds a row here.
insert into public.event_name (name) values
    ('app_open'), ('tab_viewed'), ('daily_started'), ('daily_finished'), ('endless_finished'),
    ('signed_in'), ('friend_added'), ('account_deleted');

create table public.app_event (
    id          bigint generated always as identity primary key,
    at          timestamptz not null default now(),
    name        text not null references public.event_name (name),
    platform    text not null check (platform in ('android', 'ios', 'desktop')),
    app_version text not null check (char_length(app_version) <= 32),
    -- Small scalar values only, for example {"outcome": "WON", "guesses": 3}.
    props       jsonb not null default '{}'::jsonb check (jsonb_typeof(props) = 'object' and char_length(props::text) <= 300)
);
create index app_event_at on public.app_event (at);

create table public.crash_report (
    id          bigint generated always as identity primary key,
    at          timestamptz not null default now(),
    platform    text not null check (platform in ('android', 'ios', 'desktop')),
    app_version text not null check (char_length(app_version) <= 32),
    kind        text not null check (char_length(kind) <= 200),
    message     text not null check (char_length(message) <= 500),
    stack       text not null check (char_length(stack) <= 8000)
);
create index crash_report_at on public.crash_report (at);

alter table public.event_name   enable row level security;
alter table public.app_event    enable row level security;
alter table public.crash_report enable row level security;
revoke all on public.event_name, public.app_event, public.crash_report from anon, authenticated;

-- Adds a batch of events: [{"name": ..., "props": {...}}, ...], at most 50. Unknown names make the whole batch fail,
-- so a bug in the app shows up instead of silently filling the table with junk.
create function public.log_events(p_platform text, p_version text, p_events jsonb)
    returns void
    language plpgsql
    security definer
    set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    if jsonb_typeof(p_events) <> 'array' or jsonb_array_length(p_events) > 50 then
        raise exception 'invalid events' using errcode = '22023';
    end if;
    insert into public.app_event (name, platform, app_version, props)
    select e ->> 'name', p_platform, p_version, coalesce(e -> 'props', '{}'::jsonb)
    from jsonb_array_elements(p_events) as e;
end;
$$;

-- Adds one crash report. Long texts are cut to the limits instead of refused, a crash report must not fail
-- because it is long.
create function public.log_crash(p_platform text, p_version text, p_kind text, p_message text, p_stack text)
    returns void
    language plpgsql
    security definer
    set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    insert into public.crash_report (platform, app_version, kind, message, stack)
    values (p_platform, left(p_version, 32), left(coalesce(p_kind, ''), 200), left(coalesce(p_message, ''), 500), left(coalesce(p_stack, ''), 8000));
end;
$$;

-- Removes what is older than 90 days. Run it from the SQL editor now and then (or schedule it with pg_cron).
create function public.purge_telemetry()
    returns void
    language sql
    security definer
    set search_path = ''
as $$
    delete from public.app_event where at < now() - interval '90 days';
    delete from public.crash_report where at < now() - interval '90 days';
$$;

revoke all on function public.log_events(text, text, jsonb), public.log_crash(text, text, text, text, text), public.purge_telemetry()
    from public, anon, authenticated;
grant execute on function public.log_events(text, text, jsonb), public.log_crash(text, text, text, text, text) to authenticated;
