/* Obecná kontrola všech typů úloh (port ExerciseHarnessTest z aplikace) + série. */
import { describe, expect, it } from 'vitest';
import { fmt, parseNum } from '../format';
import type { L } from '../i18n';
import { check, correctInputs, matches, type Exercise, type ExerciseType } from './exercise';
import { EXERCISES, makeExercise } from './types';
import { PRACTICE_SETS } from './sets';
import { PracticeSession, PracticeStore, available, seriesFor } from './session';

const SEEDS = 300;

function each(block: (t: ExerciseType, level: number, seed: number, ex: Exercise) => void) {
  for (const t of EXERCISES)
    for (let level = t.levels[0]; level <= t.levels[1]; level++)
      for (let seed = 1; seed <= SEEDS; seed++) {
        const ex = makeExercise(t.id, seed, level);
        block(t, level, seed, ex);
      }
}

function texty(l: L, kde: string) {
  for (const s of [l.cs, l.ru]) {
    expect(s.trim().length, `${kde}: prázdný text`).toBeGreaterThan(0);
    expect(s.split('[[').length, `${kde}: nespárované [[ ]] v „${s}“`).toBe(s.split(']]').length);
    expect(/[{}]|undefined|NaN/.test(s), `${kde}: nedosazená hodnota v „${s}“`).toBe(false);
  }
}

describe('typy úloh procvičování', () => {
  it('generátory dávají platné a deterministické úlohy', () => {
    each((t, level, seed, ex) => {
      const kde = `${t.id}/${level}/${seed}`;
      expect(ex.type).toBe(t.id);
      expect(ex.hints.length).toBe(3);
      texty(ex.prompt, kde + ' zadání');
      ex.hints.forEach((h, i) => texty(h, `${kde} nápověda ${i}`));
      texty(ex.ok, kde + ' ok');
      if (ex.choice) {
        expect(ex.choice.filter((o) => o.correct).length, kde).toBe(1);
        expect(new Set(ex.choice.map((o) => o.label.cs)).size, kde).toBe(ex.choice.length);
      } else {
        expect(ex.fields.length).toBeGreaterThan(0);
        for (const f of ex.fields) {
          expect(Number.isFinite(f.answer), kde).toBe(true);
          expect(Math.abs(f.answer), kde).toBeLessThan(10_000);
        }
      }
      expect(makeExercise(t.id, seed, level).prompt).toEqual(ex.prompt);
    });
  });

  it('správná odpověď projde', () => {
    each((t, level, seed, ex) => {
      const kde = `${t.id}/${level}/${seed}`;
      expect(check(ex, correctInputs(ex)).solved, kde).toBe(true);
      if (!ex.choice) {
        expect(check(ex, ex.fields.map((f) => fmt(f.answer, 3).replace(',', '.').replace('−', '-'))).solved, kde + ' tečka').toBe(true);
        if (ex.fields.every((f) => !f.signed)) expect(check(ex, ex.fields.map((f) => '-' + fmt(Math.abs(f.answer), 2))).solved, kde + ' znaménko').toBe(true);
      }
    });
  });

  it('typické chyby se poznají a neprojdou', () => {
    each((t, level, seed, ex) => {
      for (const trap of ex.traps) {
        const inputs = ex.fields.map((f, i) => fmt(trap.wrong[i] ?? f.answer, 3).replace('−', '-'));
        const r = check(ex, inputs);
        const kde = `${t.id}/${level}/${seed}: ${trap.code} ${inputs}`;
        expect(r.solved, kde).toBe(false);
        expect(r.trap, kde).toBeTruthy();
      }
    });
  });

  it('chyba se nehlásí na správné odpovědi', () => {
    each((t, level, seed, ex) => {
      if (ex.choice) return;
      const ok = correctInputs(ex);
      const values = ok.map((s) => parseNum(s));
      for (const trap of ex.traps) expect(matches(trap, ex.fields, ok, values), `${t.id}/${level}/${seed}: ${trap.code}`).toBe(false);
    });
  });
});

describe('série', () => {
  it('pokryje každý soubor a je deterministická', () => {
    for (const set of PRACTICE_SETS)
      for (let level = 1; level <= 3; level++) {
        const s = seriesFor(set, level, 4127);
        if (!available(set, level).length) expect(s.length).toBe(0);
        else {
          expect(s.length).toBe(set.size);
          expect(seriesFor(set, level, 4127).map((e) => e.prompt)).toEqual(s.map((e) => e.prompt));
        }
      }
  });

  it('průchod sérií: nápověda 3. stupně dá výsledek bez bodu, správně = bod', () => {
    const set = PRACTICE_SETS[0];
    const s = new PracticeSession(set, 1, 4127, new PracticeStore('test'));
    const ex = s.current!;
    if (ex.choice) s.choose(ex.choice.findIndex((o) => o.correct));
    else {
      correctInputs(ex).forEach((v, i) => s.setInput(i, v));
      s.check();
    }
    expect(s.solved).toBe(true);
    expect(s.results[0].point).toBe(true);
    s.next();
    s.hint();
    s.hint();
    s.hint();
    expect(s.solved).toBe(true);
    expect(s.results[1].point).toBe(false);
  });
});
