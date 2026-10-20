#!/usr/bin/env python3
"""Sound effects and a music bed, synthesized with numpy (no samples, nothing to license).
usage: synth.py spec.json outdir
spec: {"sounds": [{"name": "n60", "kind": "pluck", "midi": 60, "dur": 0.5, "vol": 0.5}, ...],
       "music": {"name": "music", "seconds": 30, "bpm": 96, "root": 57, "mode": "major", "seed": 1, "vol": 0.5}}
kinds: pluck (marimba-like), chime (bell), whoosh (noise sweep), tick, boop (falling), chord (three chimes), sweep (rising shimmer)
"""
import json
import sys

import numpy as np
import soundfile as sf

SR = 44100
rng = np.random.default_rng(7)


def hz(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def t_axis(dur):
    return np.arange(int(dur * SR)) / SR


def env(t, attack=0.003, decay=6.0):
    return np.minimum(1, t / attack) * np.exp(-decay * t)


def pluck(midi, dur):
    t = t_axis(dur)
    f = hz(midi)
    y = np.sin(2 * np.pi * f * t) * np.exp(-5 * t) + 0.35 * np.sin(2 * np.pi * f * 4 * t) * np.exp(-14 * t) + 0.18 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-8 * t)
    return y * np.minimum(1, t / 0.002)


def chime(midi, dur):
    t = t_axis(dur)
    f = hz(midi)
    y = np.zeros_like(t)
    for ratio, amp, dec in [(1, 1, 3.5), (2.76, 0.45, 5.5), (5.4, 0.22, 9), (8.9, 0.1, 14)]:
        y += amp * np.sin(2 * np.pi * f * ratio * t) * np.exp(-dec * t)
    return y * np.minimum(1, t / 0.002)


def lowpass_sweep(x, start_hz, end_hz):
    """One-pole low-pass whose cutoff glides from start_hz to end_hz."""
    n = len(x)
    cut = np.geomspace(start_hz, end_hz, n)
    a = 1 - np.exp(-2 * np.pi * cut / SR)
    y = np.zeros(n)
    s = 0.0
    for i in range(n):
        s += a[i] * (x[i] - s)
        y[i] = s
    return y


def whoosh(dur, rising=True):
    n = int(dur * SR)
    noise = rng.standard_normal(n)
    y = lowpass_sweep(noise, 400, 3500) if rising else lowpass_sweep(noise, 3500, 400)
    y = y - lowpass_sweep(y, 150, 150)  # take the rumble away
    x = np.linspace(0, 1, n)
    return y * np.sin(np.pi * x) ** 1.5 * 0.6


def tick(midi, dur):
    t = t_axis(dur)
    return (np.sin(2 * np.pi * hz(midi) * t) * np.exp(-60 * t) + 0.3 * rng.standard_normal(len(t)) * np.exp(-200 * t)) * np.minimum(1, t / 0.001)


def boop(midi, dur):
    t = t_axis(dur)
    f = hz(midi) * np.exp(-1.5 * t)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * np.exp(-6 * t) * np.minimum(1, t / 0.003)


def pop(midi, dur):
    """A soft bubble: a short sine that glides down a little, no noise, so it never sounds like a hard tick."""
    t = t_axis(dur)
    f = hz(midi) * (1 - 0.35 * (1 - np.exp(-45 * t)))
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * np.exp(-38 * t) * np.minimum(1, t / 0.004)


def pour(midi, dur):
    """Liquid going from one tube to another: a soft stream of noise with bubbles that rise as they pop."""
    t = t_axis(dur)
    n = len(t)
    body = lowpass_sweep(rng.standard_normal(n), 2200, 1000) * np.sin(np.pi * t / dur) ** 1.3 * 0.5
    bubbles = np.zeros(n)
    length = int(0.08 * SR)
    tt = np.arange(length) / SR
    for _ in range(16):
        start = int(rng.uniform(0.02, 0.85) * dur * SR)
        f = rng.uniform(420, 1000) * (1 + 5 * tt)
        b = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-34 * tt) * rng.uniform(0.3, 0.6)
        end = min(n, start + length)
        bubbles[start:end] += b[: end - start]
    return body + bubbles


def boom(midi, dur):
    """One explosion: a burst of noise that closes fast, with a short low thump under it (nothing a phone cannot play)."""
    t = t_axis(dur)
    n = len(t)
    noise = lowpass_sweep(rng.standard_normal(n), 3500, 160) * np.exp(-5.5 * t)
    f = 95 * np.exp(-3 * t) + 40
    thump = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-9 * t)
    return (noise * 1.0 + thump * 0.9) * np.minimum(1, t / 0.002)


def boom_soft(midi, dur):
    """A soft cartoon boom: a falling note with a little air, no sharp crack."""
    t = t_axis(dur)
    f = 60 + 200 * np.exp(-9 * t)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-7 * t)
    air = lowpass_sweep(rng.standard_normal(len(t)), 1500, 200) * np.exp(-12 * t) * 0.5
    return (body + air) * np.minimum(1, t / 0.003)


def glide(midi, dur):
    """A soft rising glide with no noise at all: the ball runs along."""
    t = t_axis(dur)
    f = hz(midi) * 2 ** (t / dur * 1.1)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * np.linspace(0, 1, len(t))) ** 2


def thunk(midi, dur):
    """A wooden disk landing: a dull knock with a small click on top."""
    t = t_axis(dur)
    f = hz(midi)
    knock = np.sin(2 * np.pi * f * t) * np.exp(-28 * t) + 0.4 * np.sin(2 * np.pi * f * 2.3 * t) * np.exp(-45 * t)
    click = rng.standard_normal(len(t)) * np.exp(-400 * t) * 0.25
    return (knock + click) * np.minimum(1, t / 0.001)


def chord(midi, dur):
    out = np.zeros(int(dur * SR))
    for k, semis in enumerate([0, 4, 7, 12]):
        c = chime(midi + semis, dur)
        d = int(0.045 * k * SR)
        out[d:] += c[: len(out) - d] * (0.8 if k < 3 else 0.5)
    return out


def sweep(midi, dur):
    t = t_axis(dur)
    f0, f1 = hz(midi), hz(midi + 12)
    f = np.geomspace(f0, f1, len(t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    return (np.sin(ph) + 0.4 * np.sin(2 * ph)) * np.sin(np.pi * np.linspace(0, 1, len(t))) ** 2 * 0.5


KINDS = {"pluck": pluck, "chime": chime, "tick": tick, "boop": boop, "pop": pop, "pour": pour, "boom": boom, "boom_soft": boom_soft, "glide": glide, "thunk": thunk, "chord": chord, "sweep": sweep}


def make_sound(s):
    dur = s.get("dur", 0.5)
    if s["kind"] == "whoosh":
        y = whoosh(dur, s.get("rising", True))
    else:
        y = KINDS[s["kind"]](s.get("midi", 60), dur)
    y = y / (np.max(np.abs(y)) + 1e-9)
    fade = int(0.01 * SR)
    y[-fade:] *= np.linspace(1, 0, fade)
    return y * s.get("vol", 0.5)


def reverb(x, wet=0.22):
    out = x.copy()
    for delay, g in [(0.071, 0.45), (0.113, 0.33), (0.167, 0.24), (0.241, 0.16)]:
        d = int(delay * SR)
        out[d:] += x[:-d] * g * wet * 2
    return out


def music(spec):
    secs = spec["seconds"]
    bpm = spec.get("bpm", 96)
    root = spec.get("root", 57)
    minor = spec.get("mode", "major") == "minor"
    local = np.random.default_rng(spec.get("seed", 1))
    beat = 60.0 / bpm
    n = int(secs * SR)
    y = np.zeros(n)
    # I - vi - IV - V (major) or i - VI - III - VII (minor); chord tones as semitones above the root
    if minor:
        prog = [(0, [0, 3, 7]), (-4, [0, 4, 7]), (3, [0, 4, 7]), (-2, [0, 4, 7])]
    else:
        prog = [(0, [0, 4, 7]), (-3, [0, 3, 7]), (-7, [0, 4, 7]), (-5, [0, 4, 7])]
    bar = 4 * beat
    bars = int(secs / bar) + 1
    for b in range(bars):
        shift, tones = prog[b % 4]
        t0 = b * bar
        i0 = int(t0 * SR)
        if i0 >= n:
            break
        # pad: soft sines, slow attack and release
        L = int((bar + 0.6) * SR)
        t = np.arange(L) / SR
        e = np.minimum(1, t / 0.5) * np.minimum(1, np.maximum(0, (bar + 0.6 - t) / 0.6))
        pad = np.zeros(L)
        for semis in tones:
            for oct_ in (0, 12):
                f = hz(root + shift + semis + oct_)
                pad += np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * f * 2.003 * t)
        pad *= e * 0.045
        j = min(L, n - i0)
        y[i0:i0 + j] += pad[:j]
        # bass on beats 1 and 3
        for k in (0, 2):
            ib = int((t0 + k * beat) * SR)
            if ib >= n:
                continue
            nb = pluck(root + shift - 12, beat * 1.9) * 0.28
            j = min(len(nb), n - ib)
            y[ib:ib + j] += nb[:j]
        # arpeggio in eighth notes
        pattern = [0, 1, 2, 1, 0, 2, 1, 2]
        for step in range(8):
            ia = int((t0 + step * beat / 2) * SR)
            if ia >= n:
                continue
            semis = tones[pattern[step]] + (12 if step % 4 == 2 else 24)
            vel = 0.5 + 0.2 * local.random()
            note = pluck(root + shift + semis, 0.6) * 0.16 * vel
            j = min(len(note), n - ia)
            y[ia:ia + j] += note[:j]
        # soft shaker on the off-beats
        for k in range(4):
            isx = int((t0 + k * beat + beat / 2) * SR)
            if isx >= n:
                continue
            L2 = int(0.05 * SR)
            sh = local.standard_normal(L2) * np.exp(-np.arange(L2) / (0.012 * SR)) * 0.018
            sh = sh - lowpass_sweep(sh, 3000, 3000)
            j = min(L2, n - isx)
            y[isx:isx + j] += sh[:j]
    y = reverb(y, 0.35)
    fade_in, fade_out = int(0.6 * SR), int(1.6 * SR)
    y[:fade_in] *= np.linspace(0, 1, fade_in)
    y[-fade_out:] *= np.linspace(1, 0, fade_out)
    y = y / (np.max(np.abs(y)) + 1e-9)
    return y * spec.get("vol", 0.5)


def write(path, y):
    sf.write(path, np.stack([y, y], axis=1).astype(np.float32), SR)


def main():
    spec = json.load(open(sys.argv[1]))
    out = sys.argv[2]
    for s in spec.get("sounds", []):
        write(f"{out}/{s['name']}.wav", make_sound(s))
    if "music" in spec:
        m = spec["music"]
        write(f"{out}/{m['name']}.wav", music(m))
    print(f"synth: {len(spec.get('sounds', []))} sounds" + (", music" if "music" in spec else ""))


if __name__ == "__main__":
    main()
