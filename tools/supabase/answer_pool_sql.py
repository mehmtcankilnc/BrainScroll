#!/usr/bin/env python3
"""Prints SQL that puts the app's answer words into `public.answer_pool` (the pool the daily puzzle draws from).

    python tools/supabase/answer_pool_sql.py > pool.sql

The words are the same ones the app already ships for the endless feed (shared/.../files/words/*_answers.txt), so
they are not secret. What IS secret is WHICH word is on WHICH day: that is chosen by the database, at random, when
the first player of a day asks for it, and it is never written to this repository.

Words are uppercased with the language's own rules (Turkish i -> İ, ı -> I), exactly like the app does.
Use the output in a NEW migration when the pool changes. Never edit a migration that was already pushed.
"""
import os

WORDS = os.path.join(os.path.dirname(__file__), "..", "..", "shared", "src", "commonMain", "composeResources", "files", "words")
UPPER_TR = str.maketrans("abcçdefgğhıijklmnoöprsştuüvyz", "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ")


def read(name):
    with open(os.path.join(WORDS, name), encoding="utf-8") as f:
        return [w.strip() for w in f if w.strip() and not w.startswith("#")]


def main():
    en = sorted({w.upper() for w in read("en_answers.txt")})
    tr = sorted({w.translate(UPPER_TR) for w in read("tr_answers.txt")})
    for language, words in (("EN", en), ("TR", tr)):
        rows = ",\n".join(f"    ('{language}', '{w}')" for w in words)
        print(f"insert into public.answer_pool (language, word) values\n{rows}\non conflict do nothing;\n")


if __name__ == "__main__":
    main()
