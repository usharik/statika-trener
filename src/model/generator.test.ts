import { describe, expect, it } from 'vitest';
import { generate } from './generator';
import { allItems, defaultEquations, linearize, namedPoints, solve, solveProblem, unknownsOf } from './mechanics';
import type { BeamType, Level } from './types';

const types: BeamType[] = ['simple', 'overhang', 'cantilever', 'gerber'];

describe('generator', () => {
  it('vytváří staticky určité a řešitelné úlohy', () => {
    const seen = new Set<string>();
    for (const level of [1, 2, 3] as Level[])
      for (const t of types)
        for (let seed = 1; seed <= 1500; seed++) {
          const p = generate(seed, { level, types: [t] });
          expect(p.type).toBe(t);
          expect(unknownsOf(p).length).toBe(3 + p.hinges.length);
          const r = solveProblem(p)!;
          expect(r).not.toBeNull();
          // řešení splňuje rovnováhu k libovolnému bodu
          const items = allItems(p);
          for (const pt of namedPoints(p)) {
            const l = linearize({ kind: 'm', point: pt.label, x: pt.x }, items);
            const sum = Object.entries(l.a).reduce((s, [k, v]) => s + v * r[k], l.c);
            expect(Math.abs(sum)).toBeLessThan(1e-6);
          }
          seen.add(JSON.stringify({ ...p, seed: 0 }));
        }
    expect(seen.size).toBeGreaterThan(15000);
  });

  it('prostý nosník: známý výsledek', () => {
    const p = generate(1, { level: 1, types: ['simple'] });
    const eqs = defaultEquations(p);
    expect(solve(eqs, allItems(p), unknownsOf(p))).not.toBeNull();
  });
});

describe('mechanika', () => {
  it('ruční kontrola: prostý nosník, šikmá síla, spojité zatížení, Gerber', () => {
    const r = solveProblem({
      seed: 0, type: 'simple', L: 6, hinges: [],
      supports: [{ id: 's0', label: 'A', x: 0, type: 'pin' }, { id: 's1', label: 'B', x: 6, type: 'roller' }],
      loads: [
        { kind: 'force', id: 'f1', label: 'F_1', x: 2, F: 10, angle: 300, alpha: 60 },
        { kind: 'udl', id: 'u1', label: 'q', x1: 3, x2: 6, q: 2 },
      ],
    })!;
    const fy = 10 * Math.sin(Math.PI / 3);
    expect(r.s0x).toBeCloseTo(-5);
    expect(r.s1y).toBeCloseTo((fy * 2 + 6 * 4.5) / 6);
    expect(r.s0y + r.s1y).toBeCloseTo(fy + 6);
    // vetknutí 0 – kloub 3 – posuvná 5, síla 10 kN v x=4
    const g = solveProblem({
      seed: 0, type: 'gerber', L: 5,
      supports: [{ id: 's0', label: 'A', x: 0, type: 'fixed' }, { id: 's1', label: 'B', x: 5, type: 'roller' }],
      hinges: [{ id: 'h0', label: 'K', x: 3 }],
      loads: [{ kind: 'force', id: 'f1', label: 'F_1', x: 4, F: 10, angle: 270 }],
    })!;
    expect(g.s1y).toBeCloseTo(5);
    expect(g.s0y).toBeCloseTo(5);
    expect(g.s0m).toBeCloseTo(15); // ↺ kladně: M_A + 5·5 − 10·4 = 0
  });
});

describe('rozložení typů', () => {
  it('všechny typy vycházejí zhruba stejně často', () => {
    const count: Record<string, number> = {};
    for (let seed = 1; seed <= 2000; seed++) {
      const p = generate(seed, { level: 1, types });
      count[p.type] = (count[p.type] ?? 0) + 1;
    }
    for (const t of types) expect(count[t]).toBeGreaterThan(400);
  });
});
