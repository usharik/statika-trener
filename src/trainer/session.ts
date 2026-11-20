import type { Problem } from '../model/types';
import type { EqSpec } from '../model/mechanics';
import type { Tr } from '../i18n';
import { Store } from './store';
import { ReleaseStep } from './release';
import { LoadsStep } from './loads';
import { EquationsStep } from './equations';
import { SolveStep } from './solve';

export type Tone = 'nudge' | 'ok' | 'info' | 'hint';

/** Zpráva žákovi; text se překládá až při vykreslení. [learn] = odkaz do výkladu (lekce, karta). */
export interface Msg {
  tone: Tone;
  text: (t: Tr) => string;
  learn?: [number, number];
}

export const msg = (tone: Tone, text: (t: Tr) => string, learn?: [number, number]): Msg => ({ tone, text, learn });

export interface ArmView {
  from: number;
  to: number;
}

export interface SideView {
  x: number;
  side: 'left' | 'right';
}

/** Co navíc ukázat ve výkresu na kroku „Rovnice“. */
export interface DrawingView {
  momentPoint?: { label: string; x: number } | null;
  side?: SideView | null;
  arm?: ArmView | null;
}

/** Rovnice předaná do výpočtu; cw = rovnice vynásobená −1 (tak ji žák napsal). */
export interface UserEq {
  spec: EqSpec;
  cw: boolean;
}

/** Události pro statistiku. */
export interface SessionEvents {
  onHint?(step: number): void;
  onStepDone?(step: number, hints: number): void;
  onSolved?(hints: number): void;
}

export const STEP_RELEASE = 0;
export const STEP_LOADS = 1;
export const STEP_EQUATIONS = 2;
export const STEP_SOLVE = 3;
export const STEP_DONE = 4;

/** Průchod jednou úlohou: krok, co je už v obrázku nahrazené, a stav aktuálního kroku. */
export class TrainerSession extends Store {
  step = STEP_RELEASE;
  released = new Set<string>();
  resolved = new Set<string>();
  highlight: string | null = null;
  activeSupport: string | null;
  view: DrawingView = {};
  results: Record<string, number> | null = null;
  hints = 0;

  readonly release: ReleaseStep;
  loads: LoadsStep | null = null;
  equations: EquationsStep | null = null;
  solve: SolveStep | null = null;

  readonly problem: Problem;
  private events: SessionEvents;

  constructor(problem: Problem, events: SessionEvents = {}) {
    super();
    this.problem = problem;
    this.events = events;
    this.activeSupport = problem.supports[0]?.id ?? null;
    this.release = new ReleaseStep(problem, this);
  }

  setHighlight(id: string | null) {
    if (this.highlight === id) return;
    this.highlight = id;
    this.emit();
  }

  hint() {
    this.hints++;
    this.events.onHint?.(this.step);
  }

  releaseSupport(id: string) {
    this.released = new Set(this.released).add(id);
  }

  resolve(id: string) {
    this.resolved = new Set(this.resolved).add(id);
  }

  releaseDone() {
    if (this.step !== STEP_RELEASE) return;
    this.activeSupport = null;
    this.highlight = null;
    this.events.onStepDone?.(STEP_RELEASE, this.hints);
    this.loads = new LoadsStep(this.problem, this);
    this.step = STEP_LOADS;
    this.emit();
  }

  loadsDone() {
    if (this.step !== STEP_LOADS) return;
    this.highlight = null;
    this.events.onStepDone?.(STEP_LOADS, this.hints);
    this.equations = new EquationsStep(this.problem, this);
    this.step = STEP_EQUATIONS;
    this.emit();
  }

  equationsDone(eqs: UserEq[]) {
    if (this.step !== STEP_EQUATIONS) return;
    this.highlight = null;
    this.view = {};
    this.events.onStepDone?.(STEP_EQUATIONS, this.hints);
    this.solve = new SolveStep(this.problem, eqs, this);
    this.step = STEP_SOLVE;
    this.emit();
  }

  solveDone(r: Record<string, number>) {
    if (this.step !== STEP_SOLVE) return;
    this.highlight = null;
    this.results = r;
    this.events.onStepDone?.(STEP_SOLVE, this.hints);
    this.events.onSolved?.(this.hints);
    this.step = STEP_DONE;
    this.emit();
  }

  /** úloha je rozpracovaná (nechceme ji zahodit jedním omylem) */
  get started(): boolean {
    return (this.step >= STEP_LOADS && this.step <= STEP_SOLVE) || (this.step === STEP_RELEASE && this.release.idx > 0);
  }
}
