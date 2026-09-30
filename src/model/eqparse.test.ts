import { describe, expect, it } from 'vitest';
import { allItems, type EqSpec } from './mechanics';
import { analyze, analyzeBest, parseEquation, symbolsOf } from './eqparse';
import type { Problem } from './types';

// A pevná (0), B posuvná (6); F_1 = 10 kN pod 60° v x=2 (dolů doprava); q = 2 kN/m na <3;6>
const p: Problem = {
  seed: 0, type: 'simple', L: 6, hinges: [],
  supports: [{ id: 's0', label: 'A', x: 0, type: 'pin' }, { id: 's1', label: 'B', x: 6, type: 'roller' }],
  loads: [
    { kind: 'force', id: 'f1', label: 'F_1', x: 2, F: 10, angle: 300, alpha: 60 },
    { kind: 'udl', id: 'u1', label: 'q', x1: 3, x2: 6, q: 2 },
  ],
};
const items = allItems(p);
const defs = symbolsOf(p, items);
const mA: EqSpec = { kind: 'm', point: 'A', x: 0 };
const run = (src: string, spec: EqSpec) => analyze(parseEquation(src, defs), spec, items, defs, p);

describe('zápis rovnic', () => {
  it('přijme různé správné tvary', () => {
    for (const s of [
      'RB*6 - F1y*2 - Q*4,5 = 0',
      'R_B·6 = F_1y·2 + Q·4.5',
      '-RB*6 + F1y*2 + Q*4.5',
      'RB*6 - F1*sin(60)*2 - q*3*4,5 = 0',
      'rb*6 - 8,66*2 - 6*4,5 = 0',
      'R_B*6 - 10*sin(60)*2 - 2*3*4,5 = 0',
      '6RB - 2F1y - 4,5Q = 0',
      'RB = (F1y*2 + Q*4,5)/6',
    ])
      expect(run(s, mA).ok, s).toBe(true);
    expect(run('RAx + F1x = 0', { kind: 'fx' }).ok).toBe(true);
    expect(run('RAy + RB - F1y - Q = 0', { kind: 'fy' }).ok).toBe(true);
    expect(run('RAy + RB = F_1*sin(60) + q*3', { kind: 'fy' }).ok).toBe(true);
  });

  it('pojmenuje konkrétní problém', () => {
    const a = run('RB*6 + F1y*2 - Q*4,5 = 0', mA);
    expect(a.issues.map((i) => [i.kind, i.item.sym])).toEqual([['sign', 'F_1y']]);
    const b = run('RB*6 - F1y*2 - Q*3 = 0', mA);
    expect(b.issues[0].kind).toBe('mag');
    expect(b.issues[0].impliedArm).toBeCloseTo(3);
    const c = run('RB*6 - F1y*2 - Q*4,5 + RAy*0 + F1x*1 = 0', mA);
    expect(c.issues.map((i) => i.kind)).toEqual(['extra']);
    const d = run('RB*6 - Q*4,5', mA);
    expect(d.issues.map((i) => [i.kind, i.item.sym])).toEqual([['missing', 'F_1y']]);
    expect(run('RB*6 - 7*1,5 - Q*4,5', mA).stray).toEqual([-10.5]);
  });

  it('hlásí chyby zápisu', () => {
    expect(parseEquation('RB*6 - X1', defs).error?.kind).toBe('unknownSym');
    expect(parseEquation('RB*RAy', defs).error?.kind).toBe('nonlinear');
    expect(parseEquation('RB*6 -', defs).error?.kind).toBe('syntax');
    expect(parseEquation('RB = 1 = 2', defs).error?.kind).toBe('twoEq');
  });

  it('kloub: vybere lepší část', () => {
    const g: Problem = {
      seed: 0, type: 'gerber', L: 5,
      supports: [{ id: 's0', label: 'A', x: 0, type: 'fixed' }, { id: 's1', label: 'B', x: 5, type: 'roller' }],
      hinges: [{ id: 'h0', label: 'K', x: 3 }],
      loads: [{ kind: 'force', id: 'f1', label: 'F_1', x: 4, F: 10, angle: 270 }],
    };
    const its = allItems(g);
    const ds = symbolsOf(g, its);
    const specs: EqSpec[] = (['left', 'right'] as const).map((side) => ({ kind: 'hinge', hingeId: 'h0', point: 'K', x: 3, side }));
    const r = analyzeBest(parseEquation('RB*2 - F1*1 = 0', ds), specs, its, ds, g);
    expect(r.ok).toBe(true);
    expect(r.spec.kind === 'hinge' && r.spec.side).toBe('right');
    const l = analyzeBest(parseEquation('MA - RAy*3 = 0', ds), specs, its, ds, g);
    expect(l.ok).toBe(true);
  });
});
