// Shared steps of every reel: voice (kokoro), sound effects, audio mix, frame render and mp4.
// build.mjs (target number) and promo.mjs (app promo) both use this.
import { spawnSync, spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

export const here = path.dirname(fileURLToPath(import.meta.url));
export const FPS = 30;
// `npm install` in this folder; PLAYWRIGHT_MODULE can point to another install of playwright.
const { chromium } = createRequire(import.meta.url)(process.env.PLAYWRIGHT_MODULE || 'playwright');
const PYTHON = process.env.PYTHON || (process.platform === 'win32' ? 'python' : 'python3');

/** The app's mascot, read from the app's docs folder. */
export function mascotSvg() {
  return fs.readFileSync(path.join(here, '..', '..', 'docs', 'assets', 'logo-mascot-draft.svg'), 'utf8');
}

/** Voice lines -> out/<id>.wav. Returns { id: seconds }. lines: [{ id, text }] */
export function synthVoice(out, lines, { voice = 'af_heart', speed = 1.28 } = {}) {
  fs.writeFileSync(path.join(out, 'voice.json'), JSON.stringify({ voice, speed, lines }, null, 1));
  const r = spawnSync(PYTHON, [path.join(here, 'tts.py'), path.join(out, 'voice.json'), out], { encoding: 'utf8' });
  if (r.status !== 0) throw new Error('tts failed: ' + r.stderr);
  process.stdout.write(r.stdout);
  return JSON.parse(fs.readFileSync(path.join(out, 'durations.json'), 'utf8'));
}

/** Sound effects and a music bed from synth.py. spec: { sounds: [{ name, kind, midi, dur, vol }], music: { name, seconds, bpm, root, mode, seed, vol } }. Returns { name: file }. */
export function synth(out, spec) {
  fs.writeFileSync(path.join(out, 'synth.json'), JSON.stringify(spec));
  const r = spawnSync(PYTHON, [path.join(here, 'synth.py'), path.join(out, 'synth.json'), out], { encoding: 'utf8' });
  if (r.status !== 0) throw new Error('synth failed: ' + r.stderr);
  const files = {};
  for (const s of spec.sounds || []) files[s.name] = path.join(out, s.name + '.wav');
  if (spec.music) files[spec.music.name] = path.join(out, spec.music.name + '.wav');
  return files;
}

/** Short sine blips. specs: { name: [frequency, seconds, volume] }. Returns { name: file }. */
export function makeTones(out, specs) {
  const files = {};
  for (const [name, [freq, d, vol]] of Object.entries(specs)) {
    const f = path.join(out, `sfx_${name}.wav`);
    const rr = spawnSync('ffmpeg', ['-y', '-v', 'error', '-f', 'lavfi', '-i', `sine=frequency=${freq}:duration=${d}:sample_rate=44100`, '-af', `afade=t=in:d=0.004,afade=t=out:st=${(d * 0.35).toFixed(3)}:d=${(d * 0.65).toFixed(3)},volume=${vol}`, f]);
    if (rr.status !== 0) throw new Error('sfx failed');
    files[name] = f;
  }
  return files;
}

/** Mixes audio files at given times into out/audio.wav. events: [{ file, at (s), vol }] */
export function mixAudio(out, events, total) {
  const args = ['-y', '-v', 'error'];
  events.forEach((e) => args.push('-i', e.file));
  const chains = events.map((e, i) => `[${i}:a]aformat=sample_rates=44100:channel_layouts=stereo,adelay=${Math.round(e.at * 1000)}|${Math.round(e.at * 1000)},volume=${e.vol}[a${i}]`);
  const mix = `${events.map((_, i) => `[a${i}]`).join('')}amix=inputs=${events.length}:normalize=0:dropout_transition=0,apad=whole_dur=${total.toFixed(3)},atrim=0:${total.toFixed(3)},alimiter=limit=0.95[m]`;
  args.push('-filter_complex', chains.join(';') + ';' + mix, '-map', '[m]', '-ar', '44100', '-ac', '2', path.join(out, 'audio.wav'));
  const r = spawnSync('ffmpeg', args, { encoding: 'utf8' });
  if (r.status !== 0) throw new Error('mix failed: ' + r.stderr);
  console.log('audio mixed');
}

/**
 * Draws the page frame by frame (window.render(t)) and encodes out/audio.wav with it into an mp4.
 * pageFile: html next to this file. init: [functionName, ...args] called once after load.
 * stills: array of times -> PNGs in `out` instead of a video.
 */
export async function renderVideo({ out, pageFile, init, total, mp4, stills = null }) {
  const browser = await chromium.launch({ args: ['--no-sandbox', '--font-render-hinting=none'] });
  const page = await browser.newPage({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 1 });
  page.on('pageerror', (e) => { console.error('page error:', e.message); process.exitCode = 1; });
  await page.goto(pathToFileURL(path.join(here, pageFile)).href);
  await page.evaluate(() => document.fonts.ready);
  await page.evaluate(([fn, ...args]) => window[fn](...args), init);
  await page.evaluate(() => document.fonts.ready);

  if (stills) {
    for (const [i, t] of stills.entries()) {
      await page.evaluate((tt) => window.render(tt), t);
      await page.screenshot({ path: path.join(out, `still_${i}_${t.toFixed(2)}.png`) });
    }
    await browser.close();
    console.log('stills written to', out);
    return;
  }

  const ff = spawn('ffmpeg', ['-y', '-v', 'error', '-f', 'image2pipe', '-c:v', 'mjpeg', '-framerate', String(FPS), '-i', 'pipe:0', '-i', path.join(out, 'audio.wav'),
    '-c:v', 'libx264', '-preset', 'medium', '-crf', '18', '-pix_fmt', 'yuv420p', '-r', String(FPS), '-c:a', 'aac', '-b:a', '192k', '-movflags', '+faststart', '-shortest', mp4], { stdio: ['pipe', 'inherit', 'inherit'] });
  const done = new Promise((res) => ff.on('close', res));
  const frames = Math.ceil(total * FPS);
  const t0 = Date.now();
  for (let i = 0; i < frames; i++) {
    await page.evaluate((tt) => window.render(tt), i / FPS);
    const buf = await page.screenshot({ type: 'jpeg', quality: 95 });
    if (!ff.stdin.write(buf)) await new Promise((res) => ff.stdin.once('drain', res));
    if (i % 120 === 0) console.log(`frame ${i}/${frames}`);
  }
  ff.stdin.end();
  const code = await done;
  await browser.close();
  console.log(`done in ${((Date.now() - t0) / 1000).toFixed(0)} s, ffmpeg exit ${code}: ${mp4}`);
}
