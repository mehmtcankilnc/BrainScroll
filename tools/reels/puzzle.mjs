// Target number puzzle generator, ported from the app's TargetGenerator.kt / TargetGame.kt rules:
//  - a move takes two different numbers and an operation, puts the result in their place
//  - the result must be a whole number above zero (a - b needs a > b, a / b needs a % b == 0)
//  - the target is two digits (10..99), is not one of the starting numbers, and needs at least `minSteps` operations
// The reels use a 4 number puzzle (3 operations, like the app) and a 6 number variant (5 operations, reel only).
// The seeds are NOT the app's seeds (Kotlin's Random differs), the rules are the same.

export const OPS = ['+', '-', '*', '/'];

export function apply(op, a, b) {
  switch (op) {
    case '+': return a + b;
    case '-': return a > b ? a - b : null;
    case '*': return a * b;
    case '/': return b !== 0 && a % b === 0 ? a / b : null;
  }
}

// mulberry32, small seedable PRNG
export function rng(seed) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const key = (arr) => arr.join(',');

/**
 * Breadth first search over what is left on the table. Returns Map value -> fewest operations that make it
 * (the starting numbers are not in the result). Stops after `maxDepth` levels.
 */
export function reachable(numbers, maxDepth = numbers.length - 1) {
  const best = new Map();
  let frontier = [[...numbers].sort((a, b) => a - b)];
  const seen = new Set([key(frontier[0])]);
  for (let depth = 1; depth <= maxDepth && frontier.length; depth++) {
    const next = [];
    for (const state of frontier) {
      const n = state.length;
      for (let i = 0; i < n; i++) {
        for (let j = 0; j < n; j++) {
          if (i === j) continue;
          for (const op of OPS) {
            if ((op === '+' || op === '*') && i > j) continue;
            const r = apply(op, state[i], state[j]);
            if (r === null) continue;
            if (!best.has(r)) best.set(r, depth);
            if (depth < maxDepth) {
              const rest = [];
              for (let k = 0; k < n; k++) if (k !== i && k !== j) rest.push(state[k]);
              rest.push(r);
              rest.sort((a, b) => a - b);
              const k2 = key(rest);
              if (!seen.has(k2)) { seen.add(k2); next.push(rest); }
            }
          }
        }
      }
    }
    frontier = next;
  }
  return best;
}

/**
 * All shortest solutions (up to `limit`) that make `target` in exactly `depth` operations: lists of steps
 * {a, op, b, result}. Depth first with a seen set per remaining multiset.
 */
export function solutions(numbers, target, depth, limit = 400) {
  const out = [];
  const dead = new Set();
  function go(state, left, path) {
    if (out.length >= limit) return;
    if (left === 0) return;
    const n = state.length;
    const id = key(state) + '|' + left;
    if (dead.has(id)) return;
    const before = out.length;
    for (let i = 0; i < n; i++) {
      for (let j = 0; j < n; j++) {
        if (i === j) continue;
        for (const op of OPS) {
          if ((op === '+' || op === '*') && i > j) continue;
          const r = apply(op, state[i], state[j]);
          if (r === null) continue;
          const step = { a: state[i], op, b: state[j], result: r };
          if (left === 1) {
            if (r === target) out.push([...path, step]);
          } else {
            const rest = [];
            for (let k = 0; k < n; k++) if (k !== i && k !== j) rest.push(state[k]);
            rest.push(r);
            rest.sort((x, y) => x - y);
            go(rest, left - 1, [...path, step]);
          }
        }
      }
    }
    if (out.length === before) dead.add(id);
  }
  go([...numbers].sort((a, b) => a - b), depth, []);
  return out;
}

/** A solution that reads nicely: small intermediate numbers, a multiplication or division in it, no step by 1. */
function score(sol) {
  const maxMid = Math.max(...sol.map((s) => s.result));
  const hasMulDiv = sol.some((s) => s.op === '*' || s.op === '/') ? 0 : 500;
  const trivial = sol.filter((s) => s.b === 1 || s.a === 1).length * 200;
  return maxMid + hasMulDiv + trivial;
}

/**
 * Makes a puzzle of `count` numbers whose target needs exactly count-1 operations (all numbers used).
 * Returns { numbers, target, par, steps } where steps is the chosen shortest solution.
 */
export function generate(seed, count, lowest = 2) {
  const rand = rng(seed);
  const need = count - 1;
  for (let draw = 0; draw < 2000; draw++) {
    const numbers = Array.from({ length: count }, () => lowest + Math.floor(rand() * (10 - lowest)));
    // Numbers `lowest`..9 (a 1 makes dull steps, so 2 by default) and at most one repeated number: repeats make ambiguous puzzles for a video.
    if (new Set(numbers).size < count - 1) continue;
    const best = reachable(numbers, need);
    const candidates = [...best.entries()]
      .filter(([v, d]) => v >= 10 && v <= 99 && d === need && !numbers.includes(v))
      .map(([v]) => v)
      .sort((a, b) => a - b);
    if (!candidates.length) continue;
    // Prefer targets that have a nicely readable solution.
    const scored = [];
    for (const target of candidates) {
      const sols = solutions(numbers, target, need);
      if (!sols.length) continue;
      sols.sort((x, y) => score(x) - score(y));
      scored.push({ target, sol: sols[0], s: score(sols[0]), count: sols.length });
    }
    if (!scored.length) continue;
    scored.sort((x, y) => x.s - y.s);
    // Among the 3 nicest, a seeded pick.
    const pick = scored[Math.floor(rand() * Math.min(3, scored.length))];
    return { numbers, target: pick.target, par: need, steps: pick.sol, draws: draw + 1, candidates: candidates.length, solutionCount: pick.count };
  }
  throw new Error('no puzzle found');
}

// CLI: node puzzle.mjs <seed> <count>
if (process.argv[1] && process.argv[1].endsWith('puzzle.mjs')) {
  const seed = Number(process.argv[2] ?? 1);
  const count = Number(process.argv[3] ?? 4);
  const t0 = Date.now();
  const p = generate(seed, count);
  console.log(JSON.stringify(p, null, 1), `\n${Date.now() - t0} ms`);
}
