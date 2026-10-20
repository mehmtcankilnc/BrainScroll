// Helpers shared by promo.html and play.html: the app's easings, colors, and small html builders for the feed screens (units are dp).
// ---- easing: the app's tokens (docs/design.md section 6) and Compose's FastOutSlowIn ----------------------------------
function bezier(x1, y1, x2, y2) {
  const cx = 3 * x1, bx = 3 * (x2 - x1) - cx, ax = 1 - cx - bx;
  const cy = 3 * y1, by = 3 * (y2 - y1) - cy, ay = 1 - cy - by;
  const X = (s) => ((ax * s + bx) * s + cx) * s, Y = (s) => ((ay * s + by) * s + cy) * s;
  return (x) => {
    if (x <= 0) return 0; if (x >= 1) return 1;
    let lo = 0, hi = 1, s = x;
    for (let i = 0; i < 28; i++) { const v = X(s); if (Math.abs(v - x) < 1e-6) break; if (v < x) lo = s; else hi = s; s = (lo + hi) / 2; }
    return Y(s);
  };
}
const standard = bezier(0.2, 0, 0, 1), overshoot = bezier(0.34, 1.56, 0.64, 1), fastOutSlowIn = bezier(0.4, 0, 0.2, 1);
const linear = (x) => Math.max(0, Math.min(1, x));
const clamp01 = linear;
const lerp = (a, b, p) => a + (b - a) * p;
const $ = (id) => document.getElementById(id);
const C = { page: '#121212', surface: '#1F1F1F', raised: '#2A2A2A', strong: '#8C8C8C', subtle: 'rgba(58,58,58,.55)', green: '#7ED957', greenOn: '#10240A', yellow: '#FFC93C', blue: '#4DB8FF', red: '#FF5A5A', orange: '#FF9F43', text: '#F5F5F5', dim: '#8C8C8C', disabled: '#6B6B6B',
  purple: '#9B7BFF', pink: '#FF7AA8', teal: '#3DD6C6' };
const mix = (a, b, p) => { const h = (c) => [1, 3, 5].map((i) => parseInt(c.slice(i, i + 2), 16)); const x = h(a), y = h(b); return '#' + x.map((v, i) => Math.round(lerp(v, y[i], p)).toString(16).padStart(2, '0')).join(''); };

// ---- a value that glides to a new target each time an event sets one (what Compose's animate*AsState does) --------
function seq(events, init, dur, ease) {
  return (l) => {
    let from = init, to = init, ts = -1e9;
    for (const e of events) { if (e.t > l) break; from = from + (to - from) * ease(clamp01((e.t - ts) / dur)); to = e.v; ts = e.t; }
    return from + (to - from) * ease(clamp01((l - ts) / dur));
  };
}
function rng(seed) { let a = seed >>> 0; return () => { a = (a + 0x6D2B79F5) >>> 0; let t = a; t = Math.imul(t ^ (t >>> 15), t | 1); t ^= t + Math.imul(t ^ (t >>> 7), t | 61); return ((t ^ (t >>> 14)) >>> 0) / 4294967296; }; }

// ---- small html builders (units are dp) ---------------------------------------------------------------------------
const f1 = (n) => Math.round(n * 100) / 100;
const box = (x, y, w, h, style, inner = '') => `<div style="position:absolute;left:${f1(x)}px;top:${f1(y)}px;width:${f1(w)}px;height:${f1(h)}px;${style}">${inner}</div>`;
const txt = (x, y, w, h, s, font, color, extra = '') => box(x, y, w, h, `display:flex;align-items:center;justify-content:center;font:${font};color:${color};white-space:nowrap;${extra}`, s);
const chip = (s, color, icon = '') => `<div style="display:inline-flex;align-items:center;gap:4px;background:${C.surface};border-radius:999px;padding:6px 12px;font:500 14px/20px Nunito;color:${color};white-space:nowrap">${icon}${s}</div>`;
const BOLT = `<svg width="14" height="14" viewBox="0 0 14 14"><path d="M8.4 0 L2.1 7.7 L6.6 7.7 L5.3 14 L11.9 5.9 L7.4 5.9 Z" fill="${C.blue}"/></svg>`;
const BULB = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="${C.yellow}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z"/></svg>`;
const chipsRow = (...chips) => `<div style="position:absolute;left:0;top:64px;width:360px;display:flex;justify-content:center;gap:8px">${chips.join('')}</div>`;

// the bar on top of every page (Hud.kt): back chevron, title, "?", bulb, answer streak with its "+1"
function hud(title, st) {
  return `<div style="position:absolute;left:0;top:0;width:360px;height:48px">
    <svg style="position:absolute;left:23px;top:17px" width="14" height="14" viewBox="0 0 14 14"><path d="M9.8 1.4 L3.5 7 L9.8 12.6" fill="none" stroke="${C.text}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
    <div style="position:absolute;left:56px;top:0;height:48px;display:flex;align-items:center">
      <span style="font:500 18px/24px Nunito;color:${C.text}">${title}</span>
      <span style="width:40px;height:40px;display:flex;align-items:center;justify-content:center"><span style="width:18px;height:18px;border:1.5px solid ${C.dim};border-radius:50%;display:flex;align-items:center;justify-content:center;font:400 12px/1 Nunito;color:${C.dim}">?</span></span>
    </div>
    <div style="position:absolute;right:8px;top:0;height:48px;display:flex;align-items:center">
      <span style="width:40px;height:40px;display:flex;align-items:center;justify-content:center">${BULB}</span>
      <span style="position:relative;display:inline-block;transform:scale(${st.scale})">${chip(st.shown, C.blue, BOLT)}
        <span style="position:absolute;left:0;top:0;width:100%;height:100%;display:flex;align-items:center;justify-content:center;font:700 22px/28px Nunito;color:${C.blue};opacity:${st.plusAlpha};transform:translateY(${st.plusLift}px)">+1</span></span>
    </div></div>`;
}
// the win streak chip: a "+1" rises over the old number, then the number turns into the new one with a pop (Hud.kt)
function streak(l, win, before) {
  if (win == null || l < win) return { shown: before, plusAlpha: 0, plusLift: 0, scale: 1 };
  const t = l - win;
  const p = t < 0.35 ? 0.6 * standard(t / 0.35) : t < 0.65 ? 0.6 + 0.4 * standard((t - 0.35) / 0.3) : 0;
  const alpha = t >= 0.65 ? 0 : p <= 0.6 ? p / 0.6 : (1 - p) / 0.4;
  let scale = 1;
  if (t >= 0.35) { const u = t - 0.35; scale = u < 0.1 ? lerp(1, 1.3, standard(u / 0.1)) : u < 0.35 ? lerp(1.3, 1, overshoot((u - 0.1) / 0.25)) : 1; }
  return { shown: t >= 0.35 ? before + 1 : before, plusAlpha: Math.max(0, Math.min(1, alpha)), plusLift: -22 * p, scale };
}
// the circle buttons (RoundIconButton.kt) and their icons
function arcPath(cx, cy, r, a0, sweep) {
  const rad = (d) => d * Math.PI / 180, p = (d) => [cx + r * Math.cos(rad(d)), cy + r * Math.sin(rad(d))];
  const s = p(a0), e = p(a0 + sweep);
  return `M${f1(s[0])} ${f1(s[1])} A${r} ${r} 0 ${sweep > 180 ? 1 : 0} 1 ${f1(e[0])} ${f1(e[1])}`;
}
const ICON = {
  restart: (col) => { const s = 28, a = (-50 + 285) * Math.PI / 180, r = s * 0.36, c = s / 2, tx = c + r * Math.cos(a), ty = c + r * Math.sin(a), tg = [-Math.sin(a), Math.cos(a)], nm = [Math.cos(a), Math.sin(a)], k = s * 0.2;
    return `<svg width="28" height="28" viewBox="0 0 28 28"><path d="${arcPath(c, c, r, -50, 285)}" fill="none" stroke="${col}" stroke-width="2.8" stroke-linecap="round"/><polygon points="${f1(tx + tg[0] * k)},${f1(ty + tg[1] * k)} ${f1(tx + nm[0] * k)},${f1(ty + nm[1] * k)} ${f1(tx - nm[0] * k)},${f1(ty - nm[1] * k)}" fill="${col}"/></svg>`; },
  undo: (col) => `<svg width="28" height="28" viewBox="0 0 28 28"><polygon points="2.8,10.08 10.08,3.92 10.08,16.24" fill="${col}"/><path d="M8.4 10.08 L16.24 10.08 A6.16 6.16 0 0 1 16.24 22.4 L6.16 22.4" fill="none" stroke="${col}" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
  pencil: (col) => `<svg width="28" height="28" viewBox="0 0 28 28" fill="none" stroke="${col}" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3.9 24.1 L5.6 16.8 L17.9 4.5 L23.5 10.1 L11.2 22.4 Z"/><path d="M15.1 7.3 L20.7 12.9"/></svg>`,
  erase: (col) => `<svg width="28" height="28" viewBox="0 0 28 28" fill="none" stroke="${col}" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M1.1 14 L8.4 5 L26.9 5 L26.9 23 L8.4 23 Z"/><path d="M13.4 10.6 L21.3 17.4 M21.3 10.6 L13.4 17.4"/></svg>`,
};
const roundBtn = (x, y, icon, { sel = false, enabled = true } = {}) =>
  box(x, y, 56, 56, `border-radius:50%;background:${sel ? C.raised : C.surface};border:${sel ? 2.5 : 1.5}px solid ${sel ? C.yellow : 'rgba(140,140,140,.6)'};display:flex;align-items:center;justify-content:center;opacity:${enabled ? 1 : 0.35}`, icon);
// the card that replaces the buttons when a puzzle is solved
const HEART = `<svg width="24" height="24" viewBox="0 0 24 24"><path d="M12 21.6 C0.5 13.2 1.9 1.9 12 7.2 C22.1 1.9 23.5 13.2 12 21.6 Z" fill="none" stroke="${C.dim}" stroke-width="2"/></svg>`;
function resultCard(a, title, line) {
  if (a <= 0) return '';
  return box(16, 496 + (1 - a) * 16, 328, 112, `background:${C.surface};border-radius:16px;opacity:${a}`,
    txt(0, 16, 328, 28, title, '700 22px Nunito', C.green) + txt(0, 48, 328, 22, line, '400 16px Nunito', C.text) + txt(0, 74, 328, 16, 'Swipe up for the next puzzle', '400 12px Nunito', C.dim)
    + box(280, 0, 48, 48, 'display:flex;align-items:center;justify-content:center', HEART));
}
const hint = (y, lines, s) => txt(16, y, 328, lines * 22, s, '400 16px/22px Nunito', C.dim, 'white-space:normal;text-align:center');
// a finger touch: a soft circle that grows and fades at the tapped place
function ripples(l, taps) {
  return taps.map((tp) => {
    const t = l - tp.t; if (t < 0 || t > 0.4) return '';
    const p = standard(t / 0.4);
    return box(tp.x - 20, tp.y - 20, 40, 40, `border-radius:50%;background:rgba(255,255,255,${f1(0.34 * (1 - p))});transform:scale(${f1(0.45 + 0.75 * p)});z-index:50`);
  }).join('');
}
const countUntil = (taps, l) => taps.filter((t) => t <= l).length;

