// Builds the game reels: Ball Slide, Mini Nonogram and Topic Groups, played in the app's own screens (see PLAN-oyun-reelleri.md).
//   node play.mjs ballslide|nonogram|topics            full video   -> out/<kind>/brainscroll_<kind>.mp4
//   node play.mjs ballslide|nonogram|topics stills     a few PNG stills
// The screens in play.html follow the app's code (ui/<game>/*Page.kt). The boards are the app's hard level.
import fs from 'node:fs';
import path from 'node:path';
import { here, FPS, mascotSvg, synthVoice, synth, mixAudio, renderVideo } from './lib.mjs';
import { slide as ballPath } from './ballslide.mjs';
import { lineClues, deduce } from './nonogram.mjs';

const kind = process.argv[2];
const stillsOnly = process.argv.includes('stills');
if (!['ballslide', 'nonogram', 'topics'].includes(kind)) throw new Error('usage: node play.mjs ballslide|nonogram|topics [stills]');
const out = path.join(here, 'out', kind);
fs.mkdirSync(out, { recursive: true });

const PHONE_IN = 0.25;                     // the phone slides in; scene time = video time - PHONE_IN
const SCALE = [0, 2, 4, 7, 9];             // major pentatonic: any run of notes sounds right
const note = (idx, base = 60) => base + SCALE[idx % 5] + 12 * Math.floor(idx / 5);
const mmss = (s) => `${Math.floor(s / 60)}:${String(Math.round(s % 60)).padStart(2, '0')}`;
const easeFOS = (x) => { // the app's FastOutSlowIn (0.4, 0, 0.2, 1)
  if (x <= 0) return 0; if (x >= 1) return 1;
  let lo = 0, hi = 1, s = x; const X = (u) => 3 * 0.4 * u * (1 - u) * (1 - u) + 3 * 0.2 * u * u * (1 - u) + u ** 3, Y = (u) => 3 * 0 * u * (1 - u) * (1 - u) + 3 * 1 * u * u * (1 - u) + u ** 3;
  for (let i = 0; i < 40; i++) { const v = X(s); if (Math.abs(v - x) < 1e-7) break; if (v < x) lo = s; else hi = s; s = (lo + hi) / 2; }
  return Y(s);
};

let timeline, events = [], sounds = [], music, voiceFiles = {}, stills;

// =====================================================================================================================
if (kind === 'ballslide') {
  // The board: the app's hard level (6 x 6, 6-10 walls, 8-12 slides), found with the app's solver: 7 walls, 29 cells, ONE shortest solution of 11 slides.
  const n = 6, wallsStr = '000001000001000000000010100010110000', start = 7, par = 11, way = [2, 1, 0, 1, 2, 1, 2, 3, 0, 3, 2];
  const walls = [...wallsStr].map((c) => c === '1');
  const open = walls.filter((w) => !w).length;
  // replay the solution with the same rules: every cell must end painted, in exactly `par` slides
  const painted = new Set([start]); let ball = start; const slides = [];
  for (const d of way) { const path = ballPath(n, walls, ball, d); if (!path.length) throw new Error('solution hits a wall'); slides.push({ dir: d, from: ball, path }); path.forEach((c) => painted.add(c)); ball = path[path.length - 1]; }
  if (painted.size !== open || slides.length !== par) throw new Error(`solution check failed: painted ${painted.size}/${open}, ${slides.length} slides`);
  console.log(`ball slide: ${open} cells, ${par} slides, all painted ✓`);

  const capA = 0.3, thinkFrom = 3.4, thinkSecs = 9, thinkTo = thinkFrom + thinkSecs;
  let t = thinkTo + 0.45; const gaps = [0.5, 0.45, 0.6, 0.5, 0.7, 0.45, 0.5, 0.6, 0.5, 0.55, 0];
  const dur = (len) => Math.min(450, Math.max(150, len * 70)) / 1000;
  const firstSwipe = t;
  slides.forEach((s, i) => { s.tG = t; s.t0 = t - PHONE_IN; t += dur(s.path.length) + gaps[i]; });
  const lastS = slides[slides.length - 1];
  const solveSecs = lastS.tG + dur(lastS.path.length) - firstSwipe + 1.0;       // a second of looking before the first swipe
  const timeText = mmss(Math.round(solveSecs));
  const win = lastS.tG + 0.55;                    // global time the result card starts
  const endStart = win + 3.4, total = endStart + 3.0;
  timeline = {
    kind, fps: FPS,
    t: { phoneIn: PHONE_IN, endStart, total },
    caps: [
      { from: capA, to: 3.3, html: 'Paint <span class="g">every cell</span>.' },
      { from: 3.3, to: thinkTo + 0.1, html: `Can you do it in<br><span class="y">${par} slides</span>?` },
      { from: thinkTo + 0.1, to: win + 0.1, html: 'Here is <span class="g">one way</span>.' },
      { from: win + 0.1, to: endStart, html: `<span class="g">${par} slides.</span> ${timeText}.<br>Beat that.` },
    ],
    timer: { from: thinkFrom, to: thinkTo },
    data: { n, walls, start, par, streakBefore: 5, timeText, slides: slides.map((s) => ({ dir: s.dir, from: s.from, path: s.path, t0: s.t0 })) },
  };
  // ---- sound ----
  const used = new Set();
  const ev = [];
  for (let k = 0; k < thinkSecs; k++) { const hi = k >= thinkSecs - 3; ev.push({ s: hi ? 'tickHi' : 'tick', at: thinkFrom + k + 0.02, vol: hi ? 0.5 : 0.38 }); }
  ev.push({ s: 'boop', at: thinkTo, vol: 0.45 });
  slides.forEach((s, k) => {
    ev.push({ s: 'whoosh', at: s.tG - 0.05, vol: 0.5 });
    const d = dur(s.path.length), base = Math.floor(k * 0.7);
    s.path.forEach((c, j) => {      // the ball passes cell j when the eased progress reaches j (+ a bit)
      let lo = 0, hi = 1; for (let i = 0; i < 30; i++) { const m = (lo + hi) / 2; if (easeFOS(m) * s.path.length > j + 0.35) hi = m; else lo = m; }
      const m = note(base + j); used.add(m);
      ev.push({ s: 'n' + m, at: s.tG + hi * d, vol: 0.34 + 0.02 * j });
    });
  });
  ev.push({ s: 'chord', at: win - 0.15, vol: 0.55 }, { s: 'sweep', at: win - 0.05, vol: 0.4 }, { s: 'endchime', at: endStart + 0.05, vol: 0.4 });
  sounds = [
    { name: 'tick', kind: 'tick', midi: 96, dur: 0.1, vol: 0.5 }, { name: 'tickHi', kind: 'tick', midi: 103, dur: 0.12, vol: 0.6 },
    { name: 'boop', kind: 'boop', midi: 50, dur: 0.4, vol: 0.5 }, { name: 'whoosh', kind: 'whoosh', dur: 0.32, vol: 0.5 },
    { name: 'chord', kind: 'chord', midi: 72, dur: 1.8, vol: 0.5 }, { name: 'sweep', kind: 'sweep', midi: 72, dur: 1.0, vol: 0.4 }, { name: 'endchime', kind: 'chime', midi: 84, dur: 1.2, vol: 0.4 },
    ...[...used].map((m) => ({ name: 'n' + m, kind: 'pluck', midi: m, dur: 0.6, vol: 0.5 })),
  ];
  music = { name: 'music', seconds: total, bpm: 92, root: 57, mode: 'major', seed: 11, vol: 0.5 };
  events = ev;
  stills = [0.8, 2.0, thinkFrom + 1.0, thinkTo - 0.6, slides[0].tG + 0.2, slides[3].tG + 0.5, slides[7].tG + 0.4, lastS.tG + 0.3, win + 0.1, win + 1.2, endStart + 1.2];
}

// =====================================================================================================================
if (kind === 'nonogram') {
  const R = JSON.parse(fs.readFileSync(path.join(here, 'rocket.json'), 'utf8'));
  // the board: the app's hard level (10 x 10), the "rocket" picture of NonogramPictures.kt; the app's solver must work it out by logic alone
  const n = R.n, cells = [...R.S].map((c) => c === '#');
  const rows = [...Array(n)].map((_, r) => cells.slice(r * n, r * n + n)), cols = [...Array(n)].map((_, c) => rows.map((r) => r[c]));
  const rowClues = rows.map(lineClues), colClues = cols.map(lineClues);
  const d = deduce(n, rowClues, colClues); if (!d.ok) throw new Error('picture is not solvable by logic');
  console.log(`nonogram: rocket, ${R.strokes.length} strokes, solvable by logic ✓`);
  const perCell = 0.08, firstAt = 2.9;
  let t = firstAt; const strokes = [];
  R.strokes.forEach(([from, to], i) => {
    const horiz = Math.floor(from / n) === Math.floor(to / n), len = (to - from) / (horiz ? 1 : n) + 1;
    const dur = (len - 1) * perCell, gap = len > 3 ? 0.34 : 0.4;
    strokes.push({ from, to, len, tG: t, t0: t - PHONE_IN });
    t += dur + gap;
  });
  const lastS = strokes[strokes.length - 1], lastFill = lastS.tG + (lastS.len - 1) * perCell, winG = lastFill + perCell, settleG = winG + 0.45;
  const timeText = mmss(Math.round(lastFill - firstAt + 1.0));
  const endStart = settleG + 3.6, total = endStart + 3.0;
  timeline = {
    kind, fps: FPS,
    t: { phoneIn: PHONE_IN, endStart, total },
    caps: [
      { from: 0.3, to: 2.8, html: 'Fill the <span class="g">numbers</span>.' },
      { from: 2.8, to: settleG + 0.2, html: 'What is <span class="y">hiding</span><br>in this grid?' },
      { from: settleG + 0.3, to: endStart, html: 'It is a <span class="b">rocket</span>!' },
    ],
    timer: null,
    data: { n, picture: R.S, rowClues, colClues, strokes: strokes.map((s) => ({ from: s.from, to: s.to, t0: s.t0 })), perCell, accent: '#4DB8FF', name: 'rocket', timeText, streakBefore: 3 },
  };
  // ---- sound: a marimba note for every cell, a brighter bell when a line is complete ----
  const used = new Set(), usedC = new Set(), ev = [];
  strokes.forEach((s, i) => {
    const horiz = Math.floor(s.from / n) === Math.floor(s.to / n), step = horiz ? 1 : n;
    for (let k = 0; k < s.len; k++) { const m = note(Math.floor(i / 3) + k, 55); used.add(m); ev.push({ s: 'n' + m, at: s.tG + k * perCell, vol: 0.3 }); }
  });
  // line done times (same rule as the page)
  const fillT = new Array(n * n).fill(null);
  strokes.forEach((s) => { const horiz = Math.floor(s.from / n) === Math.floor(s.to / n), step = horiz ? 1 : n; for (let k = 0; k < s.len; k++) fillT[s.from + k * step] = s.tG + k * perCell; });
  const runs = (arr) => { const o = []; let r = 0; for (const v of arr) { if (v) r++; else if (r) { o.push(r); r = 0; } } if (r) o.push(r); return o; };
  const doneTimes = [];
  const lineDone = (cs, clues) => { const ts = cs.map((c) => fillT[c]).sort((a, b) => a - b); for (const tt of ts) if (JSON.stringify(runs(cs.map((c) => (fillT[c] <= tt && cells[c]) ? 1 : 0))) === JSON.stringify(clues)) return tt; return null; };
  for (let i = 0; i < n; i++) {
    const a = lineDone([...Array(n)].map((_, c) => i * n + c), rowClues[i]), b = lineDone([...Array(n)].map((_, r) => r * n + i), colClues[i]);
    if (a != null) doneTimes.push(a); if (b != null) doneTimes.push(b);
  }
  doneTimes.sort((a, b) => a - b);
  let lastT = -1, lc = 0;
  doneTimes.forEach((tt) => { if (tt - lastT < 0.12) return; lastT = tt; const m = note(lc++ % 8, 79); usedC.add(m); ev.push({ s: 'c' + m, at: tt + 0.03, vol: 0.3 }); });
  ev.push({ s: 'chord', at: winG, vol: 0.55 }, { s: 'sweep', at: winG - 0.1, vol: 0.45 }, { s: 'endchime', at: endStart + 0.05, vol: 0.4 });
  sounds = [
    { name: 'chord', kind: 'chord', midi: 69, dur: 2.2, vol: 0.5 }, { name: 'sweep', kind: 'sweep', midi: 69, dur: 1.2, vol: 0.4 }, { name: 'endchime', kind: 'chime', midi: 84, dur: 1.2, vol: 0.4 },
    ...[...used].map((m) => ({ name: 'n' + m, kind: 'pluck', midi: m, dur: 0.5, vol: 0.5 })),
    ...[...usedC].map((m) => ({ name: 'c' + m, kind: 'chime', midi: m, dur: 0.8, vol: 0.45 })),
  ];
  music = { name: 'music', seconds: total, bpm: 80, root: 53, mode: 'major', seed: 5, vol: 0.5 };
  events = ev;
  stills = [0.6, 2.3, firstAt + 1.0, strokes[6].tG + 0.3, strokes[12].tG + 0.4, strokes[18].tG + 0.4, lastFill - 0.1, winG + 0.2, settleG + 0.1, settleG + 1.4, endStart + 1.2];
}

// =====================================================================================================================
if (kind === 'topics') {
  // The board: the app's hard level picks two topics of one family (fruits, vegetables) and two others, one of them from tier 2. Words are from TopicGroupsData.kt (English).
  const groups = [
    { title: 'Gemstones', words: ['DIAMOND', 'EMERALD', 'OPAL', 'PEARL'] },
    { title: 'Fruits', words: ['APPLE', 'GRAPE', 'PEACH', 'LEMON'] },
    { title: 'Composers', words: ['MOZART', 'BACH', 'CHOPIN', 'VIVALDI'] },
    { title: 'Vegetables', words: ['CARROT', 'ONION', 'SPINACH', 'CELERY'] },
  ];
  // word w = group * 4 + k; two layouts where no row holds two words of a group
  const W = (g, k) => g * 4 + k;
  const orderInit = [W(1, 0), W(2, 0), W(3, 0), W(0, 0), W(2, 1), W(3, 1), W(0, 1), W(1, 1), W(3, 2), W(0, 2), W(1, 2), W(2, 2), W(0, 3), W(1, 3), W(2, 3), W(3, 3)];
  const orderShuffled = [W(3, 3), W(0, 3), W(2, 2), W(1, 1), W(1, 2), W(2, 3), W(3, 0), W(0, 0), W(0, 1), W(3, 2), W(1, 3), W(2, 0), W(2, 1), W(1, 0), W(0, 2), W(3, 1)];
  if (new Set(orderInit).size !== 16 || new Set(orderShuffled).size !== 16) throw new Error('layouts must hold 16 different words');
  const lines = [
    { id: 'intro', text: 'Sixteen words. Four hidden groups. Can you find them all?' },
    ...groups.map((g, i) => ({ id: 'g' + i, text: g.title + '.' })),
    { id: 'end', text: 'BrainScroll. Brain games of every kind. One feed.' },
  ];
  const dur = synthVoice(out, lines);
  const introAt = 0.35, thinkFrom = introAt + dur.intro + 0.35, thinkSecs = 12, thinkTo = thinkFrom + thinkSecs, shuffleG = thinkFrom + 5.5;
  const order = [0, 2, 1];                       // Gemstones, Composers, Fruits; Vegetables comes by itself with the last one
  const dt = 0.34;
  let t = thinkTo + 0.5; const picks = [], solves = [], voiceAt = {};
  order.forEach((g, i) => {
    const t0 = t, solveAt = t0 + 3 * dt + 0.1;
    picks.push({ words: [0, 1, 2, 3].map((k) => W(g, k)), t0: t0 - PHONE_IN, dt, solveAt: solveAt - PHONE_IN, tG: t0 });
    solves.push({ g, t: solveAt - PHONE_IN, tG: solveAt });
    voiceAt['g' + g] = solveAt + 0.34;
    t = solveAt + (i === 2 ? 0 : Math.max(1.5, dur['g' + g] + 0.9));
  });
  const lastSolve = solves[2];
  solves.push({ g: 3, t: lastSolve.t, tG: lastSolve.tG });               // the last group is found by itself, a moment later it shows
  voiceAt.g3 = lastSolve.tG + Math.max(0.34 + dur['g' + order[2]] + 0.2, 0.48 + 0.3);
  const winG = lastSolve.tG, settleG = winG + 1.01;
  const timeText = mmss(Math.round(winG - thinkFrom));
  const endStart = settleG + 2.4, total = endStart + Math.max(2.8, dur.end + 1.0);
  timeline = {
    kind, fps: FPS,
    t: { phoneIn: PHONE_IN, endStart, total },
    caps: [
      { from: 0.3, to: thinkFrom, html: '16 words.<br><span class="g">4 hidden groups.</span>' },
      { from: thinkFrom, to: thinkTo + 0.1, html: 'Can you find<br><span class="y">all of them</span>?' },
      { from: thinkTo + 0.1, to: settleG + 0.2, html: 'Here is <span class="g">the answer</span>.' },
      { from: settleG + 0.3, to: endStart, html: '<span class="g">4 groups.</span><br>0 mistakes.' },
    ],
    timer: { from: thinkFrom, to: thinkTo },
    data: { groups, orderInit, orderShuffled, shuffleAt: shuffleG - PHONE_IN, picks, solves, timeText, streakBefore: 7 },
  };
  const ev = [{ file: path.join(out, 'intro.wav'), at: introAt, vol: 1 }];
  const push = (s, at, vol) => ev.push({ s, at, vol });
  for (let k = 0; k < thinkSecs; k++) { const hi = k >= thinkSecs - 3; push(hi ? 'tickHi' : 'tick', thinkFrom + k + 0.02, hi ? 0.5 : 0.38); }
  push('boop', thinkTo, 0.45); push('whoosh', shuffleG - 0.05, 0.45); push('tick', shuffleG, 0.5);
  picks.forEach((p, i) => { p.words.forEach((w, k) => push('pop' + k, p.tG + k * dt, 0.45)); push('chord', solves[i].tG, 0.5); });
  push('chord', winG + 0.5, 0.4);
  Object.entries(voiceAt).forEach(([id, at]) => ev.push({ file: path.join(out, id + '.wav'), at, vol: 1 }));
  push('sweep', winG + 0.9, 0.35);
  push('endchime', endStart + 0.05, 0.4);
  ev.push({ file: path.join(out, 'end.wav'), at: endStart + 0.35, vol: 1 });
  sounds = [
    { name: 'tick', kind: 'tick', midi: 96, dur: 0.1, vol: 0.5 }, { name: 'tickHi', kind: 'tick', midi: 103, dur: 0.12, vol: 0.6 }, { name: 'boop', kind: 'boop', midi: 50, dur: 0.4, vol: 0.5 },
    { name: 'whoosh', kind: 'whoosh', dur: 0.4, vol: 0.5 },
    ...[0, 1, 2, 3].map((k) => ({ name: 'pop' + k, kind: 'pluck', midi: note(k * 1 + 2, 60), dur: 0.4, vol: 0.5 })),
    { name: 'chord', kind: 'chord', midi: 67, dur: 1.4, vol: 0.45 }, { name: 'sweep', kind: 'sweep', midi: 72, dur: 1.0, vol: 0.4 }, { name: 'endchime', kind: 'chime', midi: 84, dur: 1.2, vol: 0.4 },
  ];
  music = { name: 'music', seconds: total, bpm: 100, root: 55, mode: 'major', seed: 3, vol: 0.5 };
  events = ev;
  stills = [0.5, thinkFrom + 1.0, shuffleG + 0.15, shuffleG + 0.9, thinkTo - 0.6, picks[0].tG + 0.55, picks[0].tG + 1.7, solves[1].tG + 0.9, solves[2].tG + 0.2, solves[2].tG + 1.2, settleG + 0.1, settleG + 1.2, endStart + 1.2];
}

// ---- common: synth, mix, render ----------------------------------------------------------------------------------------
console.log(`timeline: total ${timeline.t.total.toFixed(2)} s, end card ${timeline.t.endStart.toFixed(2)} s`);
fs.writeFileSync(path.join(out, 'timeline.json'), JSON.stringify(timeline, null, 1));
const files = synth(out, { sounds, music });
const mix = events.map((e) => (e.file ? e : { file: files[e.s], at: e.at, vol: e.vol }));
mix.forEach((e) => { if (!e.file) throw new Error('missing sound'); });
mix.push({ file: files.music, at: 0, vol: 0.55 });
mixAudio(out, mix, timeline.t.total);
await renderVideo({ out, pageFile: 'play.html', init: ['initPlay', timeline, mascotSvg()], total: timeline.t.total, mp4: path.join(out, `brainscroll_${kind}.mp4`), stills: stillsOnly ? stills : null });
