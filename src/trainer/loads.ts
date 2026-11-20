import type { Load, PointForce, Problem, PointMoment, UDL } from '../model/types';
import { forceComponents } from '../model/mechanics';
import { close, closeTyped, fmt, parseNum } from '../format';
import { msg, type Msg, type TrainerSession } from './session';

export type FieldState = 'ok' | 'nudge';

/** Q z q (výslednice spojitého zatížení). */
export const resultantSym = (label: string) => (label.startsWith('q') ? 'Q' + label.slice(1) : label);

/**
 * Hodnoty, které dá kalkulačka ve špatném režimu: |F·cos α| a |F·sin α| s α braným v radiánech (RAD)
 * nebo v gradech (GRAD, 1 grad = 0,9°). Slouží k rozpoznání chyby „kalkulačka není v DEG“.
 */
export function calcModeMistakes(f: number, alphaDeg: number): number[] {
  const rad = alphaDeg;
  const grad = (alphaDeg * 0.9 * Math.PI) / 180;
  return [Math.abs(f * Math.cos(rad)), Math.abs(f * Math.sin(rad)), Math.abs(f * Math.cos(grad)), Math.abs(f * Math.sin(grad))];
}

/** Krok 2: náhrada spojitého zatížení výslednicí a rozklad šikmých sil. */
export class LoadsStep {
  readonly tasks: (UDL | PointForce)[];
  readonly moments: PointMoment[];
  idx = 0;
  a = '';
  b = '';
  state: [FieldState | null, FieldState | null] = [null, null];
  msgs: Msg[] = [];
  hintLevel = 0;
  solved = false;
  log: Msg[] = [];

  private session: TrainerSession;

  constructor(p: Problem, session: TrainerSession) {
    this.session = session;
    this.tasks = p.loads.filter((l): l is UDL | PointForce => l.kind === 'udl' || (l.kind === 'force' && !!l.alpha));
    this.moments = p.loads.filter((l): l is PointMoment => l.kind === 'moment');
    // na dotykovém displeji není „najetí myší“ – zpracovávané zatížení zvýrazníme hned
    session.highlight = this.tasks[0]?.id ?? null;
  }

  get task(): UDL | PointForce | undefined {
    return this.tasks[this.idx];
  }

  get resultant(): string {
    const t = this.task;
    return t?.kind === 'udl' ? resultantSym(t.label) : '';
  }

  private get len(): number {
    const t = this.task;
    return t?.kind === 'udl' ? t.x2 - t.x1 : 0;
  }

  /** správné odpovědi: Q a poloha, nebo |Fx| a |Fy| */
  get answers(): [number, number] {
    const t = this.task;
    if (t?.kind === 'udl') return [t.q * this.len, this.len / 2];
    if (t?.kind === 'force') {
      const c = forceComponents(t.F, t.angle);
      return [Math.abs(c.fx), Math.abs(c.fy)];
    }
    return [0, 0];
  }

  private changed() {
    this.session.emit();
  }

  setA(v: string) {
    this.a = v;
    this.changed();
  }

  setB(v: string) {
    this.b = v;
    this.changed();
  }

  done() {
    this.session.loadsDone();
  }

  check() {
    if (this.solved) return;
    const t = this.task;
    if (!t) return;
    const [ansA, ansB] = this.answers;
    const va = parseNum(this.a);
    const vb = parseNum(this.b);
    const okA = va !== null && closeTyped(this.a.replace(/-/g, ''), Math.abs(va), ansA);
    const okB = vb !== null && closeTyped(this.b.replace(/-/g, ''), Math.abs(vb), ansB);
    this.state = [okA ? 'ok' : 'nudge', okB ? 'ok' : 'nudge'];
    const out: Msg[] = [];
    // skoro správně – jen hrubé zaokrouhlení, ne chyba v principu
    const nearly = (v: number | null, ans: number) => v !== null && !close(Math.abs(v), ans) && Math.abs(Math.abs(v) - ans) <= 0.05 * ans;
    if ((!okA && nearly(va, ansA)) || (!okB && nearly(vb, ansB))) {
      this.msgs = [msg('nudge', (tr) => tr('near'))];
      return this.changed();
    }
    if (t.kind === 'udl') {
      const l = this.len;
      const kA = va !== null && close(va, t.q) ? 'udl_q' : 'udl_Qgen';
      const kB = vb !== null && (close(vb, l) || vb > l) ? 'udl_posL' : 'udl_posGen';
      if (!okA) out.push(msg('nudge', (tr) => tr(kA), [8, 1]));
      if (!okB) out.push(msg('nudge', (tr) => tr(kB), [8, 1]));
    } else if (!okA || !okB) {
      const swapped = va !== null && vb !== null && close(va, ansB) && close(vb, ansA);
      const full = (va !== null && close(va, t.F)) || (vb !== null && close(vb, t.F));
      // kalkulačka v RAD/GRAD – výsledek „sin/cos úhlu“, ale úhel brán v radiánech nebo gradech
      let wrongMode = false;
      if (t.alpha) {
        const bad = calcModeMistakes(t.F, t.alpha).filter((m) => !close(m, ansA) && !close(m, ansB));
        wrongMode = (!okA && va !== null && bad.some((m) => close(Math.abs(va), m))) || (!okB && vb !== null && bad.some((m) => close(Math.abs(vb), m)));
      }
      // odkazy do trigonometrie podle typu chyby (obecná rada zůstává u statiky, lekce 3)
      const key = swapped ? 'f_swap' : full ? 'f_full' : wrongMode ? 'f_rad' : 'f_gen';
      const link: [number, number] = key === 'f_swap' ? [102, 1] : key === 'f_full' ? [102, 2] : key === 'f_rad' ? [103, 1] : [3, 3];
      out.push(msg('nudge', (tr) => tr(key), link));
    }
    if (okA && okB) {
      this.solved = true;
      this.session.resolve(t.id);
      const q = this.resultant;
      out.push(msg('ok', (tr) => (t.kind === 'udl' ? tr('udl_ok', { Q: q, Qv: fmt(ansA) }) : tr('f_ok'))));
    }
    this.msgs = out;
    this.changed();
  }

  hint() {
    const t = this.task;
    if (!t) return;
    this.session.hint();
    const lvl = ++this.hintLevel;
    const [ansA, ansB] = this.answers;
    const q = this.resultant;
    const l = this.len;
    const m = msg('hint', (tr) =>
      t.kind === 'udl'
        ? tr(lvl === 1 ? 'udl_h1' : lvl === 2 ? 'udl_h2' : 'udl_h3', { Q: q, q: fmt(t.q), l: fmt(l), Qv: fmt(ansA), h: fmt(ansB) })
        : tr(lvl === 1 ? 'f_h1' : lvl === 2 ? 'f_h2' : 'f_h3', { F: t.label, v: fmt(t.F), a: t.alpha ?? 0, x: fmt(ansA), y: fmt(ansB) }),
    );
    // první nápověda vede k výkladu, odkud se sin a cos berou
    if (lvl === 1 && t.kind === 'force') m.learn = [102, 0];
    this.msgs = [...this.msgs.filter((x) => x.tone !== 'hint'), m];
    this.changed();
  }

  advance() {
    const ok = this.msgs.find((m) => m.tone === 'ok');
    if (ok) this.log = [...this.log, ok];
    this.idx++;
    this.a = '';
    this.b = '';
    this.state = [null, null];
    this.msgs = [];
    this.hintLevel = 0;
    this.solved = false;
    this.session.highlight = this.tasks[this.idx]?.id ?? null;
    this.changed();
  }
}

export type { Load };
