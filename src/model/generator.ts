import { Rng, round } from './rng';
import type { BeamType, Hinge, Level, Load, Problem, Support, SupportType } from './types';
import { EPS, solveProblem, unknownsOf } from './mechanics';

export interface GenOptions {
  level: Level;
  types: BeamType[];
}

interface Layout {
  L: number;
  supports: { x: number; type: SupportType }[];
  hinges: number[];
}

function layout(type: BeamType, rng: Rng, step: number): Layout {
  switch (type) {
    case 'simple': {
      const L = rng.grid(3, 9, step);
      return { L, supports: [{ x: 0, type: 'pin' }, { x: L, type: 'roller' }], hinges: [] };
    }
    case 'overhang': {
      const L = rng.grid(5, 10, step);
      const both = rng.chance(0.35);
      const a = both ? rng.grid(1, 2, step) : 0;
      const b = L - rng.grid(1, 2.5, step);
      return { L, supports: [{ x: a, type: 'pin' }, { x: b, type: 'roller' }], hinges: [] };
    }
    case 'cantilever': {
      const L = rng.grid(3, 6, step);
      return { L, supports: [{ x: 0, type: 'fixed' }], hinges: [] };
    }
    case 'gerber': {
      const v = rng.int(0, 2);
      if (v === 0) {
        // vetknutí – kloub – posuvná podpora (případně s převislým koncem)
        const L = rng.grid(6, 10, step);
        const k = rng.grid(2, L - 3, step);
        const b = rng.chance(0.3) ? L - rng.grid(1, 1.5, step) : L;
        if (b - k < 2) return layout(type, rng, step);
        return { L, supports: [{ x: 0, type: 'fixed' }, { x: b, type: 'roller' }], hinges: [k] };
      }
      if (v === 1) {
        // A pevná – B posuvná – K kloub – C posuvná (převislý konec nese zavěšené pole)
        const L = rng.grid(8, 12, step);
        const b = rng.grid(3, L - 5, step);
        const k = rng.grid(b + 1, Math.min(b + 2.5, L - 3), step);
        return { L, supports: [{ x: 0, type: 'pin' }, { x: b, type: 'roller' }, { x: L, type: 'roller' }], hinges: [k] };
      }
      // A pevná – K kloub – B posuvná – C posuvná (zavěšené pole vlevo)
      const L = rng.grid(8, 12, step);
      const k = rng.grid(3, 5, step);
      const b = rng.grid(k + 1, Math.min(k + 2.5, L - 3), step);
      return { L, supports: [{ x: 0, type: 'pin' }, { x: b, type: 'roller' }, { x: L, type: 'roller' }], hinges: [k] };
    }
  }
}

function makeLoads(lay: Layout, rng: Rng, level: Level, step: number): Load[] | null {
  const { L } = lay;
  const blocked = new Set<number>([...lay.supports.map((s) => s.x), ...lay.hinges]);
  const loads: Load[] = [];
  const used: number[] = [];
  let udlRange: [number, number] | null = null;

  const wantUdl = level >= 2 ? rng.chance(level === 2 ? 0.55 : 0.6) : rng.chance(0.2);
  if (wantUdl) {
    // spojité zatížení nesmí přecházet přes kloub
    const bounds = [0, ...lay.hinges, L];
    const seg = rng.int(0, bounds.length - 2);
    const s0 = bounds[seg];
    const s1 = bounds[seg + 1];
    if (s1 - s0 >= 1) {
      const whole = rng.chance(0.35);
      const x1 = whole ? s0 : rng.grid(s0, s1 - 1, step);
      const x2 = whole ? s1 : rng.grid(x1 + 1, s1, step);
      const q = rng.int(1, level === 1 ? 5 : 8);
      loads.push({ kind: 'udl', id: 'u1', label: 'q', x1, x2, q });
      udlRange = [x1, x2];
    }
  }

  const nPoint = level === 1 ? rng.int(1, 2) : level === 2 ? rng.int(1, 2) + (udlRange ? 0 : 1) : rng.int(2, 3);
  const candidates: number[] = [];
  for (let x = step; x <= L + EPS; x += step) {
    const xr = round(x);
    if (blocked.has(xr)) continue;
    if (udlRange && xr > udlRange[0] - EPS && xr < udlRange[1] + EPS) continue;
    candidates.push(xr);
  }
  const positions = rng.shuffle(candidates);

  // minimální rozestup osamělých zatížení, aby výkres zůstal čitelný
  const gap = Math.max(step, L >= 6 ? 1 : 0.5);
  let momentUsed = false;
  for (let i = 0; i < nPoint; i++) {
    const x = positions.find((p) => used.every((u) => Math.abs(u - p) > gap - EPS));
    if (x === undefined) break;
    used.push(x);
    const r = rng.next();
    if (level === 3 && !momentUsed && r < 0.3) {
      momentUsed = true;
      loads.push({ kind: 'moment', id: 'm' + i, label: 'M', x, M: rng.int(2, 15), ccw: rng.chance(0.5) });
      continue;
    }
    const inclined = level >= 2 && rng.chance(level === 2 ? 0.45 : 0.5);
    const F = rng.int(2, level === 1 ? 12 : 20);
    if (inclined) {
      const alpha = rng.pick([30, 45, 60]);
      const angle = rng.chance(0.5) ? 360 - alpha : 180 + alpha;
      loads.push({ kind: 'force', id: 'f' + i, label: 'F', x, F, angle, alpha });
    } else {
      const up = level === 3 && rng.chance(0.12);
      loads.push({ kind: 'force', id: 'f' + i, label: 'F', x, F, angle: up ? 90 : 270 });
    }
  }
  if (!loads.some((l) => l.kind !== 'moment')) return null;
  return loads;
}

function mirror(p: Problem): Problem {
  const L = p.L;
  const m = (x: number) => round(L - x);
  return {
    ...p,
    supports: p.supports.map((s) => ({ ...s, x: m(s.x) })),
    hinges: p.hinges.map((h) => ({ ...h, x: m(h.x) })),
    loads: p.loads.map((l) => {
      if (l.kind === 'force') return { ...l, x: m(l.x), angle: (540 - l.angle) % 360 };
      if (l.kind === 'moment') return { ...l, x: m(l.x), ccw: !l.ccw };
      return { ...l, x1: m(l.x2), x2: m(l.x1) };
    }),
  };
}

/** Popisky podle polohy zleva doprava: podpory A, B, C…, klouby K, síly F1, F2… */
function relabel(p: Problem): Problem {
  const supports = [...p.supports].sort((a, b) => a.x - b.x).map((s, i) => ({ ...s, id: 's' + i, label: 'ABCDE'[i] }));
  const hinges = [...p.hinges].sort((a, b) => a.x - b.x).map((h, i) => ({ ...h, id: 'h' + i, label: p.hinges.length > 1 ? 'K' + (i + 1) : 'K' }));
  const pos = (l: Load) => (l.kind === 'udl' ? l.x1 : l.x);
  const sorted = [...p.loads].sort((a, b) => pos(a) - pos(b));
  const count = (k: Load['kind']) => sorted.filter((l) => l.kind === k).length;
  const idx: Record<string, number> = { force: 0, moment: 0, udl: 0 };
  const loads = sorted.map((l) => {
    const i = ++idx[l.kind];
    const base = l.kind === 'force' ? 'F' : l.kind === 'moment' ? 'M' : 'q';
    const label = l.kind === 'force' || count(l.kind) > 1 ? `${base}_${i}` : base;
    return { ...l, id: l.kind[0] + i, label } as Load;
  });
  return { ...p, supports, hinges, loads };
}

function validate(p: Problem): boolean {
  if (unknownsOf(p).length !== 3 + p.hinges.length) return false;
  const r = solveProblem(p);
  if (!r) return false;
  const us = unknownsOf(p);
  for (const u of us) {
    const v = r[u.id];
    if (!Number.isFinite(v) || Math.abs(v) > 400) return false;
    // nulové svislé reakce a momenty působí zmateně – vynecháme
    if (u.kind !== 'fx' && Math.abs(v) < 0.05) return false;
  }
  return true;
}

export function generate(seed: number, opts: GenOptions): Problem {
  const rng = new Rng(seed);
  const types = opts.types.length ? opts.types : (['simple'] as BeamType[]);
  const step = opts.level === 1 ? 1 : 0.5;
  // typ se volí jen jednou – jinak by typy s více odmítnutými pokusy (Gerber) vycházely vzácněji
  const type = rng.pick(types);
  for (let attempt = 0; attempt < 400; attempt++) {
    const lay = layout(type, rng, step);
    const loads = makeLoads(lay, rng, opts.level, step);
    if (!loads) continue;
    const supports: Support[] = lay.supports.map((s, i) => ({ id: 's' + i, label: '', x: round(s.x), type: s.type }));
    const hinges: Hinge[] = lay.hinges.map((x, i) => ({ id: 'h' + i, label: '', x: round(x) }));
    let p: Problem = { seed, type, L: round(lay.L), supports, hinges, loads };
    // zrcadlení: pevná podpora / vetknutí se objeví i vpravo
    if (rng.chance(0.5)) p = mirror(p);
    p = relabel(p);
    if (validate(p)) return p;
  }
  throw new Error('Nepodařilo se vygenerovat úlohu');
}
