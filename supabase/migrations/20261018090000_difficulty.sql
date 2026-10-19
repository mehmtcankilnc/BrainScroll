-- BrainScroll difficulty levels.
--
-- Every finished puzzle carries the level its board was made for: 0 easy, 1 medium, 2 hard. Results from before levels existed
-- (and uploads from an older app, which does not send the column) are medium.

alter table public.game_result
    add column difficulty smallint not null default 1 check (difficulty between 0 and 2);
