/*
 * Úloha procvičování: pole, typické chyby, kontrola odpovědi (čistá logika). Shodné s aplikací (practice/Exercise.kt).
 */
import { L } from '../i18n';
import { fmt, parseNum } from '../format';
import type { Rng } from '../model/rng';
import { accept, near, parseRatio, type Tol } from './tol';
import type { Fig } from './fig';

export type Tone = 'nudge' | 'ok' | 'info' | 'hint';
export type FieldState = 'ok' | 'nudge' | null;
export type Learn = [number, number];

/** Zpráva žákovi v procvičování (dvojjazyčný text, volitelný odkaz do výkladu). */
export interface PMsg {
  tone: Tone;
  text: L;
  learn?: Learn | null;
}

export interface Opt {
  label: L;
  correct: boolean;
  why: L | null;
}
export const opt = (label: L, correct = false, why: L | null = null): Opt => ({ label, correct, why });

/** Číselné pole úlohy (viz Field v aplikaci). */
export interface Field {
  label: L;
  answer: number;
  unit: string;
  signed: boolean;
  tol: Tol;
  digits: number;
  scale: number | null;
}
export const field = (label: L, answer: number, unit = '', o: { signed?: boolean; tol?: Tol; digits?: number; scale?: number | null } = {}): Field => ({
  label,
  answer,
  unit,
  signed: o.signed ?? false,
  tol: o.tol ?? 'value',
  digits: o.digits ?? 2,
  scale: o.scale ?? null,
});

/** Typická chyba: [wrong] = hodnoty, které ta chyba dá (null = na poli nezáleží), [pred] = složitější podmínka. */
export interface Trap {
  code: string;
  msg: L;
  learn: Learn | null;
  wrong: (number | null)[];
  pred?: (v: (number | null)[]) => boolean;
}

export interface Exercise {
  type: string;
  seed: number;
  level: number;
  prompt: L;
  fig: Fig | null;
  fields: Field[];
  choice: Opt[] | null;
  hints: L[];
  traps: Trap[];
  generic: L;
  ok: L;
  solvedFig: Fig | null;
}

const GENERIC = L('Ještě to nesedí. Zkus nápovědu.', 'Пока не сходится. Попробуй подсказку.');
const OK = L('Správně!', 'Верно!');
const NEAR = L(
  'Skoro! Počítej s nezaokrouhlenými čísly a výsledek zapiš na 2 desetinná místa (úhel na 1).',
  'Почти! Считай с неокруглёнными числами, а ответ запиши с 2 знаками после запятой (угол — с 1).',
);
const EMPTY = L('Doplň všechna pole.', 'Заполни все поля.');
const NOT_NUMBER = L('Zapiš jen číslo, např. 0,6 (bez zlomku a jednotky).', 'Введи только число, например 0,6 (без дроби и единиц).');

/** jednotky v ruském textu */
const RU_UNITS: Record<string, string> = { kN: 'кН', kNm: 'кН·м', m: 'м', 'kN/m': 'кН/м' };

function answerText(f: Field, ru: boolean): string {
  const m = /\[\[[^\]]+]]/.exec(f.label.cs);
  const sym = m ? f.label.cs.slice(0, m.index + m[0].length) : f.label.cs.length <= 3 && f.label.cs === f.label.ru ? f.label.cs : null;
  const unit = ru ? (RU_UNITS[f.unit] ?? f.unit) : f.unit;
  const sep = unit === '°' || unit === '%' || unit === '' ? '' : ' ';
  const v = fmt(f.answer, f.digits) + sep + unit;
  return sym !== null ? `${sym} = ${v}` : v;
}

export function fieldAccepts(f: Field, input: string, v: number): boolean {
  const value = f.signed ? v : Math.abs(v);
  const ans = f.signed ? f.answer : Math.abs(f.answer);
  return accept(input, value, ans, f.tol, f.scale);
}

/** chyba, kterou nejde odlišit od správné odpovědi */
function collides(t: Trap, fields: Field[]): boolean {
  const pairs: [Field, number][] = [];
  t.wrong.forEach((w, i) => {
    if (w !== null && i < fields.length) pairs.push([fields[i], w]);
  });
  if (!pairs.length) return false;
  return pairs.every(([f, w]) => {
    if (fieldAccepts(f, fmt(w, 3).replace('−', '-'), w)) return true;
    const okIn = fmt(f.answer, Math.max(2, f.digits)).replace('−', '-');
    const v = parseNum(okIn) ?? f.answer;
    return f.signed ? accept(okIn, v, w, f.tol, f.scale) : accept(okIn, Math.abs(v), Math.abs(w), f.tol, f.scale);
  });
}

/** Sestaví úlohu; třetí nápověda („Výsledek: …“) se doplní z odpovědí, kolidující chyby se vyřadí. */
export function exercise(
  type: string,
  seed: number,
  level: number,
  o: { prompt: L; fig: Fig | null; fields?: Field[]; choice?: Opt[] | null; hints: L[]; traps?: Trap[]; generic?: L; ok?: L; solvedFig?: Fig | null },
): Exercise {
  const fields = o.fields ?? [];
  const choice = o.choice ?? null;
  const hints = o.hints;
  let h3 = hints[2];
  if (!h3) {
    const ans = choice !== null ? choice.find((c) => c.correct)!.label : L(fields.map((f) => answerText(f, false)).join(', '), fields.map((f) => answerText(f, true)).join(', '));
    h3 = L(`Výsledek: ${ans.cs}`, `Ответ: ${ans.ru}`);
  }
  const kept = (o.traps ?? []).filter((t) => !collides(t, fields));
  return {
    type,
    seed,
    level,
    prompt: o.prompt,
    fig: o.fig,
    fields,
    choice,
    hints: [hints[0], hints[1], h3],
    traps: kept,
    generic: o.generic ?? GENERIC,
    ok: o.ok ?? OK,
    solvedFig: o.solvedFig ?? null,
  };
}

/** Typ úlohy v rejstříku. */
export interface ExerciseType {
  id: string;
  title: L;
  learn: Learn | null;
  levels: [number, number];
  make: (rng: Rng, level: number, seed: number) => Exercise;
}
export const exerciseType = (id: string, title: L, learn: Learn | null, make: ExerciseType['make'], levels: [number, number] = [1, 3]): ExerciseType => ({ id, title, learn, levels, make });

export interface CheckResult {
  states: FieldState[];
  msgs: PMsg[];
  solved: boolean;
  trap?: string | null;
  counted: boolean;
}

/** přečte pole: u podílů se smí zapsat i zlomek „3/5“ */
export const readField = (f: Field, s: string) => (f.tol === 'ratio' ? parseRatio(s) : parseNum(s));

const pm = (tone: Tone, text: L, learn: Learn | null = null): PMsg => ({ tone, text, learn });

export function matches(t: Trap, fields: Field[], inputs: string[], values: (number | null)[]): boolean {
  if (t.pred) return t.pred(values);
  let any = false;
  for (let i = 0; i < t.wrong.length; i++) {
    const w = t.wrong[i];
    if (w === null || i >= fields.length) continue;
    const v = values[i];
    if (v === null) return false;
    const f = fields[i];
    const ok = f.signed ? accept(inputs[i], v, w, f.tol, f.scale) : accept(inputs[i], Math.abs(v), Math.abs(w), f.tol, f.scale);
    if (!ok) return false;
    any = true;
  }
  return any;
}

/** Kontrola odpovědi. [typeLearn] = karta výkladu k typu (u obecné zprávy). */
export function check(ex: Exercise, inputs: string[], typeLearn: Learn | null = null): CheckResult {
  const choice = ex.choice;
  if (choice !== null) {
    const raw = inputs[0];
    const i = raw !== undefined && /^\d+$/.test(raw) ? Number(raw) : null;
    if (i === null || !choice[i]) return { states: [null], msgs: [], solved: false, counted: false };
    const o = choice[i];
    return o.correct
      ? { states: ['ok'], msgs: [pm('ok', ex.ok)], solved: true, counted: true }
      : { states: ['nudge'], msgs: [pm('nudge', o.why ?? ex.generic, typeLearn)], solved: false, trap: `OPT${i}`, counted: true };
  }
  const values = ex.fields.map((f, i) => readField(f, inputs[i] ?? ''));
  const states: FieldState[] = ex.fields.map((f, i) => {
    const v = values[i];
    if (v === null) return 'nudge';
    return fieldAccepts(f, inputs[i], v) ? 'ok' : 'nudge';
  });
  if (states.every((s) => s === 'ok')) return { states, msgs: [pm('ok', ex.ok)], solved: true, counted: true };
  if (values.some((v, i) => v === null && !(inputs[i] ?? '').trim())) return { states, msgs: [pm('nudge', EMPTY)], solved: false, counted: false };
  if (values.some((v) => v === null)) return { states, msgs: [pm('nudge', NOT_NUMBER)], solved: false, trap: 'NOTNUM', counted: false };
  for (const t of ex.traps) if (matches(t, ex.fields, inputs, values)) return { states, msgs: [pm('nudge', t.msg, t.learn)], solved: false, trap: t.code, counted: true };
  const nearAll = ex.fields.every((f, i) => states[i] === 'ok' || near(f.signed ? values[i]! : Math.abs(values[i]!), f.signed ? f.answer : Math.abs(f.answer)));
  if (nearAll) return { states, msgs: [pm('nudge', NEAR)], solved: false, trap: 'NEAR', counted: true };
  return { states, msgs: [pm('nudge', ex.generic, typeLearn)], solved: false, counted: true };
}

/** Správné vstupy pro úlohu (testy, nápověda 3. stupně). */
export const correctInputs = (ex: Exercise): string[] =>
  ex.choice ? [String(ex.choice.findIndex((o) => o.correct))] : ex.fields.map((f) => fmt(f.answer, Math.max(2, f.digits)).replace('−', '-'));
