-- More words that cannot be part of a username (App Store guideline 1.2: names other players see must be filtered).
-- Same rules as the first list (20261007090000_leaderboards_friends.sql): matched as a substring after lowercasing, removing
-- underscores and undoing digit tricks (0->o, 1->i, 3->e, 4->a, 5->s, 7->t, 8->b). Words that sit inside harmless names or
-- ordinary words are left out on purpose ("amina" is a name, "torpedo" contains "pedo", "spice" contains "spic").
insert into public.blocked_word (word) values
    -- English
    ('wanker'), ('twat'), ('bastard'), ('retard'), ('porn'), ('penis'), ('vagina'), ('kike'), ('chink'), ('tranny'),
    ('jizz'), ('blowjob'), ('handjob'), ('pedophile'), ('paedophile'), ('molest'), ('motherfucker'), ('bullshit'),
    ('dickhead'), ('cumshot'),
    -- Turkish (without Turkish letters, as usernames only have a-z)
    ('aminakoy'), ('aminakod'), ('sikerim'), ('siktim'), ('sikicem'), ('sikcem'), ('yavsak'), ('serefsiz'), ('dalyarak'),
    ('dangalak'), ('gerizekali'), ('tasak'), ('fahise'), ('vajina'), ('kodumun'), ('kodugumun'), ('ananisikeyim'),
    ('gotunu'), ('gotlek')
on conflict (word) do nothing;
