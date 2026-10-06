-- BrainScroll account deletion, phase 8.
--
-- App stores require that an app which creates accounts lets people delete them from inside the app. Every table
-- that holds something about a player references auth.users with "on delete cascade", so deleting the auth user
-- removes ALL of it in one step: results, favorites, daily sessions, username, friendships and friend requests,
-- and the streak. This function is the only way an app user can trigger that, and only for themselves.
--
-- The caller's session token stops working once the user is gone. The app signs out locally right after.

create function public.delete_my_account()
    returns void
    language plpgsql
    security definer
    set search_path = ''
as $$
declare
    uid uuid := auth.uid();
begin
    if uid is null then
        raise exception 'not signed in' using errcode = '28000';
    end if;
    delete from auth.users where id = uid;
end;
$$;

revoke all on function public.delete_my_account() from public, anon, authenticated;
grant execute on function public.delete_my_account() to authenticated;
