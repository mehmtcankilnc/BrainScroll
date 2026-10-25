-- BrainScroll Pro: the anonymous counts around the purchase screen (docs/pro-plan.md section 9 and 15).
--
-- Only when the player allows usage counts, like every other event. They carry the name of the screen that opened the
-- purchase screen (guide, profile, streak, theme, streak_rescue) and, for a failed purchase, a short error name. Nothing
-- about the purchase itself (price, store account) is sent.

insert into public.event_name (name) values
    ('paywall_shown'), ('purchase_started'), ('purchase_succeeded'),
    ('purchase_cancelled'), ('purchase_failed'), ('purchase_restored')
on conflict (name) do nothing;
