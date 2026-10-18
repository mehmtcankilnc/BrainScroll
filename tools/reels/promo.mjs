// Builds the app promo reel: hook headline -> wall of apps -> "one feed" -> six games played in the app's own screens -> end card.
//   node promo.mjs            full video      node promo.mjs stills   a few PNG stills
// The screens in promo.html follow the app's code (ui/<game>/*Page.kt): same sizes, colors, tap order and animations.
import fs from 'node:fs';
import path from 'node:path';
import { here, FPS, mascotSvg, synthVoice, makeTones, mixAudio, renderVideo } from './lib.mjs';

const stillsOnly = process.argv.includes('stills');
const out = path.join(here, 'out', 'promo');
fs.mkdirSync(out, { recursive: true });

// Times inside each game page, in seconds after the page starts to slide in. `taps` are the finger taps in order.
const SCRIPT = {
  target: { taps: [0.5, 0.74, 0.98, 1.22, 1.46, 1.70, 1.94, 2.18, 2.42], win: 2.5, dur: 3.5 },
  sudoku: { taps: [0.55, 0.95, 1.45, 1.85], win: null, dur: 3.3 },
  memory: { taps: [0.55, 0.88, 1.21, 1.54, 1.87, 2.20], win: 2.65, dur: 3.5 },
  tubes: { taps: [0.5, 0.9, 1.75, 2.1], pourMs: 700, win: 2.85, dur: 3.6 },
  block: { taps: [0.5, 0.85, 1.2, 1.55, 1.9, 2.25], win: 2.55, dur: 3.4 },
  pipes: { taps: [0.55, 0.9, 1.25, 1.6], win: 1.95, dur: 3.7, more: 2.45 },
};
const GAMES = [['target', 'Target number'], ['sudoku', 'Sudoku'], ['memory', 'Memory match'], ['tubes', 'Color tubes'], ['block', 'Block Out'], ['pipes', 'Pipes']];

const dur = synthVoice(out, [
  { id: 'hook', text: 'Are you tired of downloading countless apps for various brain games?' },
  { id: 'pivot', text: 'What if they all lived in one feed?' },
  ...GAMES.map(([key, name]) => ({ id: 'g_' + key, text: name + '.' })),
  { id: 'more', text: 'And so on.' },
  { id: 'end', text: 'BrainScroll. Fifteen brain games. One feed.' },
]);

const hookAt = 0.3;
const pivotStart = Math.max(hookAt + dur.hook + 0.5, 4.4);
const pivotVoice = pivotStart + 0.3;
const feedStart = pivotVoice + dur.pivot + 0.35;
const pivotEnd = feedStart;
let cursor = feedStart;
const cards = GAMES.map(([key]) => { const c = { key, start: cursor, dur: SCRIPT[key].dur }; cursor += c.dur; return c; });
const endStart = cursor + 0.1;
const total = endStart + Math.max(2.6, dur.end + 1.0);
const timeline = { fps: FPS, script: SCRIPT, cards, t: { pivotStart, pivotEnd, feedStart, endStart, total } };
fs.writeFileSync(path.join(out, 'timeline.json'), JSON.stringify(timeline, null, 1));
console.log(`hook ${dur.hook.toFixed(2)}s, pivot ${pivotStart.toFixed(2)}, feed ${feedStart.toFixed(2)}, end ${endStart.toFixed(2)}, total ${total.toFixed(2)} s`);

const sfx = makeTones(out, {
  pop: [740, 0.1, 0.45], whoosh: [260, 0.3, 0.5], tick: [1100, 0.05, 0.35], c1: [523, 0.15, 0.5], c2: [659, 0.15, 0.5], c3: [784, 0.3, 0.55], end: [880, 0.25, 0.45], badge: [1250, 0.06, 0.4],
});
const ev = [];
ev.push({ file: path.join(out, 'hook.wav'), at: hookAt, vol: 1 });
for (let i = 0; i < 16; i++) ev.push({ file: i % 4 === 3 ? sfx.badge : sfx.pop, at: 0.4 + i * 0.17 + 0.1, vol: 0.35 });
ev.push({ file: sfx.whoosh, at: pivotStart, vol: 0.5 });
ev.push({ file: path.join(out, 'pivot.wav'), at: pivotVoice, vol: 1 });
ev.push({ file: sfx.whoosh, at: feedStart - 0.05, vol: 0.4 });
cards.forEach((c, k) => {
  const s = SCRIPT[c.key];
  if (k > 0) ev.push({ file: sfx.whoosh, at: c.start, vol: 0.3 });
  ev.push({ file: path.join(out, `g_${c.key}.wav`), at: c.start + (k === 0 ? 0.1 : 0.2), vol: 1 });
  s.taps.forEach((t) => ev.push({ file: sfx.tick, at: c.start + t, vol: 0.5 }));
  if (s.more != null) ev.push({ file: path.join(out, 'more.wav'), at: c.start + s.more, vol: 1 });
  if (c.key === 'sudoku') ev.push({ file: sfx.pop, at: c.start + s.taps[3] + 0.05, vol: 0.5 });
  if (s.win != null) ev.push({ file: sfx.c1, at: c.start + s.win, vol: 0.4 }, { file: sfx.c2, at: c.start + s.win + 0.1, vol: 0.4 }, { file: sfx.c3, at: c.start + s.win + 0.2, vol: 0.45 });
});
ev.push({ file: sfx.end, at: endStart + 0.05, vol: 0.4 });
ev.push({ file: path.join(out, 'end.wav'), at: endStart + 0.35, vol: 1 });
mixAudio(out, ev, total);

const c = (k, dt) => cards[k].start + dt;
const stills = stillsOnly
  ? [pivotStart - 0.2, c(0, 0.6), c(0, 1.5), c(0, 2.9), c(1, 1.0), c(1, 2.4), c(2, 1.2), c(2, 3.0), c(3, 1.2), c(3, 2.4), c(3, 3.1), c(4, 0.7), c(4, 1.9), c(4, 3.0), c(5, 0.9), c(5, 2.6), endStart + 1.2]
  : null;
await renderVideo({ out, pageFile: 'promo.html', init: ['initPromo', timeline, mascotSvg()], total, mp4: path.join(out, 'brainscroll_promo.mp4'), stills });
