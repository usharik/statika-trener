import type { Problem, Support } from './types';

export const EPS = 1e-7;

export type Dof = 'fx' | 'fy' | 'm';

/** Neznámá reakce. Kladný (předpokládaný) smysl: →, ↑, ↺. */
export interface Unknown {
  id: string;
  sym: string;
  kind: Dof;
  x: number;
  supportId: string;
}

/** Jedna „položka“ v rovnicích rovnováhy – složka síly, výslednice, moment nebo reakce. */
export interface Item {
  id: string;
  sym: string;
  kind: Dof;
  x: number;
  /** smysl vzhledem ke kladné ose (+1 = →, ↑, ↺) */
  dir: 1 | -1;
  /** velikost (jen známé zatížení) */
  value?: number;
  unknownId?: string;
  source: 'reaction' | 'load';
  loadId?: string;
}

export type EqSpec =
  | { kind: 'fx' }
  | { kind: 'fy' }
  | { kind: 'm'; point: string; x: number }
  | { kind: 'hinge'; hingeId: string; point: string; x: number; side: 'left' | 'right' };

export interface Expect {
  sign: -1 | 0 | 1;
  /** rameno (null = bez ramene – silová rovnice nebo dvojice) */
  arm: number | null;
  /** položka do rovnice vůbec nepatří (leží na druhé části nosníku) */
  excluded?: boolean;
}

export const reactionSetOf = (s: Support): ReactionSet =>
  s.type === 'pin' ? 'xy' : s.type === 'roller' ? 'y' : 'xym';

export type ReactionSet = 'xy' | 'y' | 'x' | 'xym';

export function unknownsOfSupport(s: Support): Unknown[] {
  const base = { x: s.x, supportId: s.id };
  if (s.type === 'roller') return [{ ...base, id: s.id + 'y', sym: `R_${s.label}`, kind: 'fy' }];
  const u: Unknown[] = [
    { ...base, id: s.id + 'x', sym: `R_${s.label}x`, kind: 'fx' },
    { ...base, id: s.id + 'y', sym: `R_${s.label}y`, kind: 'fy' },
  ];
  if (s.type === 'fixed') u.push({ ...base, id: s.id + 'm', sym: `M_${s.label}`, kind: 'm' });
  return u;
}

export const unknownsOf = (p: Problem): Unknown[] => p.supports.flatMap(unknownsOfSupport);

export function reactionItems(p: Problem): Item[] {
  return unknownsOf(p).map((u) => ({
    id: 'r_' + u.id,
    sym: u.sym,
    kind: u.kind,
    x: u.x,
    dir: 1,
    unknownId: u.id,
    source: 'reaction',
  }));
}

export interface ForceComponents {
  fx: number;
  fy: number;
}

export function forceComponents(F: number, angle: number): ForceComponents {
  const r = (angle * Math.PI) / 180;
  let fx = F * Math.cos(r);
  let fy = F * Math.sin(r);
  if (Math.abs(fx) < EPS) fx = 0;
  if (Math.abs(fy) < EPS) fy = 0;
  return { fx, fy };
}

const sgn = (v: number): 1 | -1 => (v >= 0 ? 1 : -1);

export function loadItems(p: Problem): Item[] {
  const out: Item[] = [];
  for (const l of p.loads) {
    if (l.kind === 'force') {
      const { fx, fy } = forceComponents(l.F, l.angle);
      const both = fx !== 0 && fy !== 0;
      if (fx !== 0)
        out.push({ id: l.id + 'x', sym: both ? l.label + 'x' : l.label, kind: 'fx', x: l.x, dir: sgn(fx), value: Math.abs(fx), source: 'load', loadId: l.id });
      if (fy !== 0)
        out.push({ id: l.id + 'y', sym: both ? l.label + 'y' : l.label, kind: 'fy', x: l.x, dir: sgn(fy), value: Math.abs(fy), source: 'load', loadId: l.id });
    } else if (l.kind === 'udl') {
      out.push({
        id: l.id + 'Q',
        sym: l.label.replace(/^q/, 'Q'),
        kind: 'fy',
        x: (l.x1 + l.x2) / 2,
        dir: -1,
        value: l.q * (l.x2 - l.x1),
        source: 'load',
        loadId: l.id,
      });
    } else {
      out.push({ id: l.id, sym: l.label, kind: 'm', x: l.x, dir: l.ccw ? 1 : -1, value: l.M, source: 'load', loadId: l.id });
    }
  }
  return out;
}

export const allItems = (p: Problem): Item[] => [...reactionItems(p), ...loadItems(p)];

/** Očekávaný příspěvek položky do rovnice (kladný smysl momentů ↺). */
export function expected(item: Item, eq: EqSpec): Expect {
  if (eq.kind === 'fx') return { sign: item.kind === 'fx' ? item.dir : 0, arm: null };
  if (eq.kind === 'fy') return { sign: item.kind === 'fy' ? item.dir : 0, arm: null };
  if (eq.kind === 'hinge') {
    const onSide = eq.side === 'left' ? item.x < eq.x - EPS : item.x > eq.x + EPS;
    if (!onSide) return { sign: 0, arm: null, excluded: true };
  }
  if (item.kind === 'm') return { sign: item.dir, arm: null };
  if (item.kind === 'fx') return { sign: 0, arm: 0 };
  const d = item.x - eq.x;
  if (Math.abs(d) < EPS) return { sign: 0, arm: 0 };
  return { sign: (item.dir * Math.sign(d)) as 1 | -1, arm: Math.abs(d) };
}

export const coef = (e: Expect) => e.sign * (e.arm ?? 1);

/** Gaussova eliminace s částečnou pivotací. null = singulární soustava. */
export function gauss(A: number[][], b: number[]): number[] | null {
  const n = b.length;
  const M = A.map((row, i) => [...row, b[i]]);
  for (let c = 0; c < n; c++) {
    let p = c;
    for (let r = c + 1; r < n; r++) if (Math.abs(M[r][c]) > Math.abs(M[p][c])) p = r;
    if (Math.abs(M[p][c]) < 1e-9) return null;
    [M[c], M[p]] = [M[p], M[c]];
    for (let r = 0; r < n; r++) {
      if (r === c) continue;
      const f = M[r][c] / M[c][c];
      for (let k = c; k <= n; k++) M[r][k] -= f * M[c][k];
    }
  }
  return M.map((row, i) => row[n] / row[i]);
}

export interface LinEq {
  /** koeficienty u neznámých */
  a: Record<string, number>;
  /** součet známých členů (rovnice: Σ a·u + c = 0) */
  c: number;
}

export function linearize(eq: EqSpec, items: Item[]): LinEq {
  const a: Record<string, number> = {};
  let c = 0;
  for (const it of items) {
    const k = coef(expected(it, eq));
    if (k === 0) continue;
    if (it.unknownId) a[it.unknownId] = (a[it.unknownId] ?? 0) + k;
    else c += k * (it.value ?? 0);
  }
  return { a, c };
}

export function solve(eqs: EqSpec[], items: Item[], unknowns: Unknown[]): Record<string, number> | null {
  if (eqs.length !== unknowns.length) return null;
  const lin = eqs.map((e) => linearize(e, items));
  const A = lin.map((l) => unknowns.map((u) => l.a[u.id] ?? 0));
  const b = lin.map((l) => -l.c);
  const x = gauss(A, b);
  if (!x) return null;
  const res: Record<string, number> = {};
  unknowns.forEach((u, i) => (res[u.id] = Math.abs(x[i]) < 1e-9 ? 0 : x[i]));
  return res;
}

export interface NamedPoint {
  label: string;
  x: number;
  kind: 'support' | 'hinge';
}

export const namedPoints = (p: Problem): NamedPoint[] =>
  [
    ...p.supports.map((s) => ({ label: s.label, x: s.x, kind: 'support' as const })),
    ...p.hinges.map((h) => ({ label: h.label, x: h.x, kind: 'hinge' as const })),
  ].sort((a, b) => a.x - b.x);

/** Strana kloubu s menším počtem neznámých – vhodnější pro podmínku M_K = 0. */
export function preferredSide(p: Problem, hingeX: number): 'left' | 'right' {
  // vodorovné reakce leží v ose nosníku – do momentové podmínky nevstupují
  const us = unknownsOf(p).filter((u) => u.kind !== 'fx');
  const left = us.filter((u) => u.x < hingeX).length;
  const right = us.filter((u) => u.x > hingeX).length;
  return right <= left ? 'right' : 'left';
}

/** Bod pro momentovou podmínku, kde se protíná nejvíc neznámých. */
export function preferredMomentPoint(p: Problem): NamedPoint {
  const us = unknownsOf(p);
  const pts = namedPoints(p).filter((q) => q.kind === 'support');
  let best = pts[0];
  let bestN = -1;
  for (const q of pts) {
    const n = us.filter((u) => Math.abs(u.x - q.x) < EPS && u.kind !== 'm').length;
    if (n > bestN) {
      best = q;
      bestN = n;
    }
  }
  return best;
}

export function defaultEquations(p: Problem): EqSpec[] {
  const P = preferredMomentPoint(p);
  return [
    { kind: 'fx' },
    { kind: 'fy' },
    { kind: 'm', point: P.label, x: P.x },
    ...p.hinges.map((h) => ({ kind: 'hinge' as const, hingeId: h.id, point: h.label, x: h.x, side: preferredSide(p, h.x) })),
  ];
}

export function solveProblem(p: Problem): Record<string, number> | null {
  return solve(defaultEquations(p), allItems(p), unknownsOf(p));
}
