// Builds one target-number reel: puzzle -> voice lines -> TTS -> timeline -> sound effects -> frames -> mp4.
//   node build.mjs A                 full video (A: 4 numbers, 3 operations; B: 6 numbers, 5 operations)
//   node build.mjs A stills          only a few PNG stills (to look at the layout)
//   node build.mjs B --seed=12       another puzzle (the seed picks the numbers; the app's seeds are not used)
import fs from 'node:fs';
import path from 'node:path';
import { generate } from './puzzle.mjs';
import { here, FPS, mascotSvg, synthVoice, makeTones, mixAudio, renderVideo } from './lib.mjs';

// A: the app's own puzzle (4 numbers, 3 operations). B: reel-only variant (6 numbers, 5 operations).
const CONFIG = {
  A: { seed: 1, count: 4, lowest: 2, think: 6, spokenCount: 'four' },
  B: { seed: 3, count: 6, lowest: 1, think: 12, spokenCount: 'six' },
};
const kind = process.argv[2];
const stillsOnly = process.argv.includes('stills');
if (!CONFIG[kind]) throw new Error('usage: node build.mjs A|B [stills] [--seed=N]');
const cfg = { ...CONFIG[kind] };
const seedArg = process.argv.find((a) => a.startsWith('--seed='));
if (seedArg) cfg.seed = Number(seedArg.slice(7));
const tag = seedArg ? `${kind}-seed${cfg.seed}` : kind;
const out = path.join(here, 'out', tag);
fs.mkdirSync(out, { recursive: true });

// ---- numbers in words, for the voice ---------------------------------------------------------------------------
const ones = ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'eleven', 'twelve', 'thirteen', 'fourteen', 'fifteen', 'sixteen', 'seventeen', 'eighteen', 'nineteen'];
const tens = ['', '', 'twenty', 'thirty', 'forty', 'fifty', 'sixty', 'seventy', 'eighty', 'ninety'];
function words(n) {
  if (n < 20) return ones[n];
  if (n < 100) return tens[Math.floor(n / 10)] + (n % 10 ? '-' + ones[n % 10] : '');
  if (n < 1000) return ones[Math.floor(n / 100)] + ' hundred' + (n % 100 ? ' ' + words(n % 100) : '');
  throw new Error('number too big for the voice: ' + n);
}
const opWord = { '+': 'plus', '-': 'minus', '*': 'times', '/': 'divided by' };

// ---- 1. puzzle -------------------------------------------------------------------------------------------------
const puzzle = generate(cfg.seed, cfg.count, cfg.lowest);
fs.writeFileSync(path.join(out, 'puzzle.json'), JSON.stringify(puzzle, null, 1));
console.log(`${kind}: ${puzzle.numbers.join(' ')} -> ${puzzle.target}  ` + puzzle.steps.map((s) => `${s.a}${s.op}${s.b}=${s.result}`).join(' '));

// ---- 2. voice --------------------------------------------------------------------------------------------------
const lines = [
  { id: 'intro', text: `Reach ${words(puzzle.target)} with these ${cfg.spokenCount} numbers.` },
  ...puzzle.steps.map((s, i) => ({ id: 'step' + i, text: `${words(s.a)} ${opWord[s.op]} ${words(s.b)}, ${words(s.result)}${i === puzzle.steps.length - 1 ? '!' : '.'}` })),
  { id: 'end', text: 'BrainScroll. Quick brain games.' },
];
const dur = synthVoice(out, lines);

// ---- 3. timeline -----------------------------------------------------------------------------------------------
const introAt = 0.25;
const thinkStart = introAt + dur.intro + 0.3;
const thinkEnd = thinkStart + cfg.think;
let cursor = thinkEnd + 0.45;
const steps = puzzle.steps.map((s, i) => {
  const d = Math.max(1.2, dur['step' + i] + 0.3);
  const o = { start: cursor, end: cursor + d, voiceAt: cursor + 0.15 };
  cursor += d;
  return o;
});
const endStart = cursor + 0.8;
const total = endStart + Math.max(1.9, dur.end + 0.65);
const timeline = { fps: FPS, puzzle, t: { thinkStart, thinkEnd, steps, endStart, total } };
fs.writeFileSync(path.join(out, 'timeline.json'), JSON.stringify(timeline, null, 1));
console.log(`timeline: think ${thinkStart.toFixed(2)}-${thinkEnd.toFixed(2)}, reveal ${steps[0].start.toFixed(2)}-${cursor.toFixed(2)}, end ${endStart.toFixed(2)}, total ${total.toFixed(2)} s`);

// ---- 4. sound effects and mix ----------------------------------------------------------------------------------
const sfx = makeTones(out, {
  tick: [1100, 0.07, 0.5], tickHi: [1700, 0.09, 0.6], boop: [320, 0.28, 0.7],
  pop: [740, 0.13, 0.55], c1: [523, 0.18, 0.55], c2: [659, 0.18, 0.55], c3: [784, 0.32, 0.6], end: [880, 0.25, 0.45],
});
const events = []; // {file, at, vol}
events.push({ file: path.join(out, 'intro.wav'), at: introAt, vol: 1.0 });
for (let s = 0; s < cfg.think; s++) events.push({ file: s >= cfg.think - 3 ? sfx.tickHi : sfx.tick, at: thinkStart + s, vol: 0.6 });
events.push({ file: sfx.boop, at: thinkEnd, vol: 0.6 });
steps.forEach((s, i) => {
  events.push({ file: path.join(out, `step${i}.wav`), at: s.voiceAt, vol: 1.0 });
  events.push({ file: sfx.pop, at: s.start + 0.72, vol: 0.5 });
});
const win = steps[steps.length - 1].start + 0.75;
events.push({ file: sfx.c1, at: win, vol: 0.6 }, { file: sfx.c2, at: win + 0.11, vol: 0.6 }, { file: sfx.c3, at: win + 0.22, vol: 0.6 });
events.push({ file: path.join(out, 'end.wav'), at: endStart + 0.35, vol: 1.0 });
events.push({ file: sfx.end, at: endStart + 0.05, vol: 0.4 });
mixAudio(out, events, total);

// ---- 5. frames -------------------------------------------------------------------------------------------------
const stills = stillsOnly
  ? [0.1, thinkStart + 1.2, thinkEnd - 0.7, steps[0].start + 0.3, steps[0].start + 1.0, steps[Math.floor(steps.length / 2)].start + 0.5, steps[steps.length - 1].start + 1.2, endStart + 1.0]
  : null;
await renderVideo({ out, pageFile: 'page.html', init: ['initReel', timeline, mascotSvg()], total, mp4: path.join(out, `brainscroll_target_${tag}.mp4`), stills });
