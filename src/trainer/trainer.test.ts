/*
 * Průchod úlohou přes stav trenažéru (bez UI) – shodně s TrainerTest a ReviewRegressionTest v aplikaci.
 */
import { describe, expect, it } from 'vitest';
import { generate } from '../model/generator';
import { allItems, expected, forceComponents, reactionSetOf, type EqSpec } from '../model/mechanics';
import { analyze, parseEquation, symbolsOf } from '../model/eqparse';
import type { BeamType, Level } from '../model/types';
import { fmt, plain, toggleSign, parseNum } from '../format';
import { trFor } from '../i18n';
import { TrainerSession, STEP_DONE, STEP_EQUATIONS, STEP_LOADS, STEP_SOLVE } from './session';
import { fullSolution } from './solution';
import { problemPdfName } from '../pdf/pdfName';

const tr = trFor('cs');
const TYPES: BeamType[] = ['simple', 'overhang', 'cantilever', 'gerber'];

function throughLoads(s: TrainerSession) {
  const r = s.release;
  while (s.step === 0) {
    const t = r.task;
    if (t.kind === 'support') r.pickSet(reactionSetOf({ id: '', label: '', x: 0, type: t.type }));
    else if (t.kind === 'hinge') r.pickHinge('forces');
    else {
      r.setNU(String(r.nUnknowns));
      r.setNE(String(r.nEqs));
      r.checkCount();
    }
    expect(r.solved, r.msgs.map((m) => m.text(tr)).join('; ')).toBe(true);
    r.advance();
  }
  expect(s.step).toBe(STEP_LOADS);
  const l = s.loads!;
  while (l.task) {
    const t = l.task;
    if (t.kind === 'udl') {
      l.setA(fmt(t.q * (t.x2 - t.x1), 4));
      l.setB(fmt((t.x2 - t.x1) / 2, 4));
    } else {
      const c = forceComponents(t.F, t.angle);
      l.setA(fmt(Math.abs(c.fx), 4));
      l.setB(fmt(Math.abs(c.fy), 4));
    }
    l.check();
    expect(l.solved, l.msgs.map((m) => m.text(tr)).join('; ')).toBe(true);
    l.advance();
  }
  l.done();
  expect(s.step).toBe(STEP_EQUATIONS);
}

describe('trenažér', () => {
  it('opakovaná nápověda vždy dovede ke správné rovnici a k výsledku', () => {
    for (const level of [1, 2, 3] as Level[])
      for (const type of TYPES)
        for (let seed = 1; seed <= 25; seed++) {
          const s = new TrainerSession(generate(seed * 7919, { level, types: [type] }));
          throughLoads(s);
          const eq = s.equations!;
          for (let i = 0; i < eq.rows.length; i++) {
            eq.focusRow(i);
            let n = 0;
            while (!eq.rows[i].done && n++ < 40) {
              eq.hint();
              eq.check(i);
            }
            expect(eq.rows[i].done, `${seed}/${level}/${type} řádek ${i}: „${eq.rows[i].text}“`).toBe(true);
          }
          eq.done();
          expect(s.step).toBe(STEP_SOLVE);
          const sv = s.solve!;
          for (const u of sv.unknowns) sv.setVal(u.id, fmt(sv.answer[u.id], 4));
          sv.check();
          expect(sv.allOk).toBe(true);
          sv.done();
          expect(s.step).toBe(STEP_DONE);
        }
  });

  it('nápověda se nezasekne na špatném začátku ani nespadne na členu se závorkou', () => {
    for (const level of [1, 2, 3] as Level[])
      for (const type of TYPES)
        for (let seed = 1; seed <= 12; seed++) {
          const s = new TrainerSession(generate(((seed * 104729) % 999999) + 1, { level, types: [type] }));
          throughLoads(s);
          const eq = s.equations!;
          const u = eq.unknownDefs[0].name;
          const k = eq.knownDefs[0].name;
          const starts = [`${u} + `, `${u} + (`, `${u} = 0 = 0`, `${u} + 5`, `2*${u} + 2*${k}`, `(${k} + 1)*7`, `${u}*sqrt(-1)`];
          starts.forEach((start, i) => {
            const row = i % eq.rows.length;
            if (eq.rows[row].done) return;
            eq.onValueChange(row, start, [start.length, start.length]);
            let n = 0;
            while (!eq.rows[row].done && n++ < 30) {
              eq.hint();
              eq.check(row);
            }
            expect(eq.rows[row].done, `${seed}/${level}/${type} „${start}“ → „${eq.rows[row].text}“`).toBe(true);
          });
        }
  });

  it('číslo ramene se nepřiřadí zatížení se stejnou hodnotou', () => {
    let tried = 0;
    for (let seed = 1; seed <= 400; seed++) {
      const p = generate(seed, { level: 1, types: TYPES });
      const items = allItems(p);
      const defs = symbolsOf(p, items);
      const a = p.supports[0];
      const spec: EqSpec = { kind: 'm', point: a.label, x: a.x };
      const parts: string[] = [];
      for (const it of items) {
        const e = expected(it, spec);
        if (e.sign === 0) continue;
        const sg = e.sign < 0 ? '-' : '+';
        const armTxt = e.arm !== null ? plain(e.arm, 4).replace('.', ',') : null;
        parts.push(it.unknownId ? `${sg} ${it.sym}` + (armTxt ? `*${armTxt}` : '') : `${sg} ` + (armTxt ? `${armTxt}*` : '') + plain(it.value!, 6).replace('.', ','));
      }
      if (!parts.length) continue;
      const text = parts.join(' ') + ' = 0';
      const res = analyze(parseEquation(text, defs), spec, items, defs);
      tried++;
      expect(res.ok, `úloha ${seed}: „${text}“`).toBe(true);
    }
    expect(tried).toBeGreaterThan(300);
  });

  it('formát čísel a znaménko', () => {
    expect(fmt(0.005)).toBe('0,01');
    expect(fmt(0.0049)).toBe('0');
    expect(fmt(-2.5)).toBe('−2,5');
    expect(fmt(100, 0)).toBe('100');
    expect(parseNum('0x10')).toBeNull();
    expect(parseNum('1e3')).toBe(1000);
    expect(parseNum('−3,5')).toBe(-3.5);
    expect(toggleSign('3,5')).toBe('−3,5');
    expect(toggleSign('−3,5')).toBe('3,5');
    expect(toggleSign('')).toBe('−');
    expect(parseNum(toggleSign('4'))).toBe(-4);
  });

  it('odmocnina ze záporného čísla není platný zápis', () => {
    const p = generate(3199, { level: 3, types: ['overhang'] });
    const defs = symbolsOf(p, allItems(p));
    expect(parseEquation('R_Ax + sqrt(-1)', defs).error?.kind).toBe('badValue');
  });

  it('vzorové řešení spočítá všechny reakce', () => {
    for (const type of TYPES)
      for (let seed = 1; seed <= 200; seed++) {
        const p = generate(seed, { level: 3, types: [type] });
        const sol = fullSolution(p);
        const n = sol.lines.length + sol.coupled.length;
        expect(n).toBe(Object.keys(sol.values).length);
        for (const l of sol.lines) expect(Math.abs(l.value - sol.values[l.u.id])).toBeLessThan(1e-9);
      }
    expect(problemPdfName(generate(5, { level: 1, types: ['simple'] }), true, tr)).toBe('statika-5-reseni.pdf');
  });
});
