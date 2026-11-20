/*
 * Série procvičování: losování úloh, průchod sérií a postup v zařízení (shodné s aplikací – practice/PracticeSession.kt).
 */
import { Rng } from '../model/rng';
import { storage } from '../trainer/settings';
import { Store } from '../trainer/store';
import { check, correctInputs, type Exercise, type ExerciseType, type FieldState, type PMsg } from './exercise';
import { EXERCISE_BY_ID } from './types';
import type { PracticeSet } from './sets';

export interface TaskResult {
  type: string;
  point: boolean;
  hints: number;
  misses: number;
  firstTry: boolean;
}

/** kolik chybných kontrol číselné úlohy ještě nebere bod */
export const MAX_MISSES = 2;
const MAX_PER_SERIES: Record<string, number> = { E01: 2 };

/** hvězda za sérii: aspoň 3/4 bodů */
export const starFor = (score: number, size: number) => score * 4 >= size * 3;

/** Postup v procvičování (klíče jako v aplikaci `practice.tg.*`, s předponou webu). */
export class PracticeStore {
  private course: string;
  constructor(course = 'tg') {
    this.course = course;
  }
  private k(s: string) {
    return `statika.practice.${this.course}.${s}`;
  }
  private getInt(s: string, def = 0) {
    const v = storage.get(this.k(s));
    return v === null || v === undefined ? def : Number(v) || 0;
  }
  private putInt(s: string, v: number) {
    storage.set(this.k(s), String(v));
  }
  level(set: string) {
    return Math.min(3, Math.max(1, this.getInt(`${set}.level`, 1)));
  }
  setLevel(set: string, level: number) {
    this.putInt(`${set}.level`, level);
  }
  best(set: string) {
    return this.getInt(`${set}.best`);
  }
  star(set: string, level?: number): boolean {
    if (level === undefined) return [1, 2, 3].some((l) => this.star(set, l));
    return this.getInt(`${set}.star.${level}`) === 1;
  }
  stars(set: string) {
    return [1, 2, 3].filter((l) => this.star(set, l)).length;
  }
  runs(set: string) {
    return this.getInt(`${set}.runs`);
  }
  misses(type: string) {
    return this.getInt(`miss.${type}`);
  }
  addMiss(type: string) {
    this.putInt(`miss.${type}`, this.misses(type) + 1);
  }
  finish(set: string, level: number, score: number, size: number) {
    if (score > this.best(set)) this.putInt(`${set}.best`, score);
    if (starFor(score, size)) this.putInt(`${set}.star.${level}`, 1);
    this.putInt(`${set}.runs`, this.runs(set) + 1);
  }
}

/** typy, které už existují a umí danou úroveň */
export function available(set: PracticeSet, level: number): [ExerciseType, number][] {
  const out: [ExerciseType, number][] = [];
  for (const [id, w] of set.types) {
    const t = EXERCISE_BY_ID[id];
    if (t && level >= t.levels[0] && level <= t.levels[1]) out.push([t, w]);
  }
  return out;
}

/** Úlohy série: deterministické podle čísla série, souboru a úrovně (stejné jako v aplikaci). */
export function seriesFor(set: PracticeSet, level: number, seed: number): Exercise[] {
  const types = available(set, level);
  if (!types.length) return [];
  let h = 0;
  for (const ch of set.id) h = (Math.imul(h, 31) + ch.charCodeAt(0)) | 0;
  const rng = new Rng((Math.imul(seed, 31) + Math.imul(level, 7919) + h) | 0);
  const total = types.reduce((s, [, w]) => s + w, 0);
  const out: Exercise[] = [];
  let last: string | null = null;
  const count: Record<string, number> = {};
  for (let i = 0; i < set.size; i++) {
    let t: ExerciseType;
    let guard = 0;
    let capped: boolean;
    do {
      let r = rng.int(1, total);
      t = types.find(([, w]) => {
        r -= w;
        return r <= 0;
      })![0];
      guard++;
      capped = (count[t.id] ?? 0) >= (MAX_PER_SERIES[t.id] ?? Infinity);
    } while ((capped || (t.id === last && types.length > 1)) && guard < 20);
    last = t.id;
    count[t.id] = (count[t.id] ?? 0) + 1;
    const s = rng.int(1, 999_999);
    out.push(t.make(new Rng(s), level, s));
  }
  return out;
}

/** Průchod sérií: aktuální úloha, vstupy, zprávy a stupeň nápovědy. */
export class PracticeSession extends Store {
  readonly set: PracticeSet;
  readonly level: number;
  readonly seed: number;
  readonly tasks: Exercise[];
  private store: PracticeStore;
  index = 0;
  inputs: string[];
  states: FieldState[];
  msgs: PMsg[] = [];
  hintLevel = 0;
  solved = false;
  results: TaskResult[] = [];
  finished = false;
  private misses = 0;

  constructor(set: PracticeSet, level: number, seed: number, store: PracticeStore) {
    super();
    this.set = set;
    this.level = level;
    this.seed = seed;
    this.store = store;
    this.tasks = seriesFor(set, level, seed);
    this.inputs = this.emptyInputs(0);
    this.states = this.inputs.map(() => null);
  }

  get current(): Exercise | undefined {
    return this.tasks[this.index];
  }
  get score() {
    return this.results.filter((r) => r.point).length;
  }
  get firstTries() {
    return this.results.filter((r) => r.firstTry).length;
  }
  /** začala už série (aby změna úrovně nezahodila práci bez potvrzení)? */
  get started() {
    return this.index > 0 || this.results.length > 0 || this.hintLevel > 0 || this.misses > 0 || this.inputs.some((s) => s.trim() !== '');
  }

  private fieldCount(i: number) {
    const t = this.tasks[i];
    return t ? (t.choice ? 1 : t.fields.length) : 0;
  }
  private emptyInputs(i: number) {
    return Array.from({ length: this.fieldCount(i) }, () => '');
  }

  setInput(i: number, v: string) {
    if (this.solved) return;
    this.inputs = this.inputs.map((s, j) => (j === i ? v : s));
    if (this.states[i] === 'nudge') this.states = this.states.map((s, j) => (j === i ? null : s));
    this.emit();
  }

  choose(option: number) {
    if (this.solved) return;
    this.inputs = [String(option)];
    this.check();
  }

  check() {
    const ex = this.current;
    if (!ex || this.solved) return;
    const r = check(ex, this.inputs, EXERCISE_BY_ID[ex.type]?.learn ?? null);
    this.states = r.states;
    this.msgs = r.msgs;
    if (r.solved) this.finishTask(ex.choice ? this.misses === 0 : this.misses <= MAX_MISSES);
    else if (r.counted) {
      this.misses++;
      this.store.addMiss(ex.type);
    }
    this.emit();
  }

  hint() {
    const ex = this.current;
    if (!ex || this.solved || this.hintLevel >= 3) return;
    this.hintLevel++;
    const h = ex.hints[this.hintLevel - 1];
    this.msgs = [...this.msgs.filter((m) => m.tone !== 'hint' && (this.hintLevel < 3 || m.tone !== 'nudge')), { tone: 'hint', text: h }];
    if (this.hintLevel === 3) {
      this.inputs = correctInputs(ex);
      this.states = this.inputs.map(() => 'ok');
      this.finishTask(false);
    }
    this.emit();
  }

  private finishTask(point: boolean) {
    const ex = this.current;
    if (!ex) return;
    this.solved = true;
    this.results = [...this.results, { type: ex.type, point, hints: this.hintLevel, misses: this.misses, firstTry: point && this.misses === 0 && this.hintLevel < 3 }];
  }

  next() {
    if (!this.solved) return;
    if (this.index + 1 >= this.tasks.length) {
      this.finished = true;
      this.store.finish(this.set.id, this.level, this.score, this.tasks.length);
      this.emit();
      return;
    }
    this.index++;
    this.inputs = this.emptyInputs(this.index);
    this.states = this.inputs.map(() => null);
    this.msgs = [];
    this.hintLevel = 0;
    this.solved = false;
    this.misses = 0;
    this.emit();
  }

  /** typy, u kterých žák chyboval nebo potřeboval výsledek z nápovědy (nejvýš 3, nejhorší první) */
  weakTypes(): ExerciseType[] {
    const g = new Map<string, number>();
    for (const r of this.results) if (!r.point || r.misses > 0) g.set(r.type, (g.get(r.type) ?? 0) + r.misses + (r.point ? 0 : 2));
    return [...g.entries()]
      .sort((a, b) => b[1] - a[1])
      .map(([id]) => EXERCISE_BY_ID[id])
      .filter(Boolean)
      .slice(0, 3);
  }
}

/** rozpracované série (v paměti, jako v aplikaci) */
export const practiceStore = new PracticeStore();
export const practiceSessions = new Map<string, PracticeSession>();
