import type { Problem } from '../model/types';
import { allItems, expected, solve, unknownsOf, type Item, type Unknown } from '../model/mechanics';
import { close, closeTyped, fmt, parseNum } from '../format';
import { eqTitle } from './equations';
import type { FieldState } from './loads';
import { msg, type Msg, type TrainerSession, type UserEq } from './session';

/** Člen rovnice s koeficientem (znaménko × rameno) podle konvence žáka. */
export interface Term {
  it: Item;
  c: number;
  arm: number | null;
}

export function termsOf(eq: UserEq, items: Item[]): Term[] {
  // rovnici ukazujeme se stejným znaménkem, jak ji žák napsal
  const k = eq.cw ? -1 : 1;
  return items
    .map((it) => {
      const e = expected(it, eq.spec);
      return { it, c: e.sign * k * (e.arm ?? 1), arm: e.arm };
    })
    .filter((x) => x.c !== 0);
}

export const unitOf = (u: Unknown) => (u.kind === 'm' ? 'kNm' : 'kN');
export const paren = (v: number) => (v < 0 ? `(${fmt(v)})` : fmt(v));

/**
 * Výraz pro neznámou [uid] z rovnice [terms], např. „(8,66·2 + 6·4,5) / 6“.
 * null, když rovnice obsahuje ještě nespočítané neznámé (mimo [solved]).
 */
export function expressionFor(terms: Term[], uid: string, solved: Record<string, number>): string | null {
  const self = terms.find((x) => x.it.unknownId === uid)!;
  const f = self.c > 0 ? -1 : 1;
  const parts: string[] = [];
  for (const x of terms) {
    if (x === self) continue;
    const c = x.c * f;
    let text: string;
    if (x.it.unknownId) {
      const v = solved[x.it.unknownId];
      if (v === undefined) return null;
      text = x.arm !== null ? `${fmt(Math.abs(x.c))}·${paren(v)}` : paren(v);
    } else {
      const v = x.it.value!;
      text = x.arm !== null ? `${fmt(v)}·${fmt(Math.abs(x.c))}` : fmt(v);
    }
    parts.push((c < 0 ? (parts.length ? ' − ' : '−') : parts.length ? ' + ' : '') + text);
  }
  const d = Math.abs(self.c);
  const num = parts.length ? parts.join('') : '0';
  if (Math.abs(d - 1) < 1e-9) return num;
  return (parts.length > 1 ? `(${num})` : num) + ` / ${fmt(d)}`;
}

/** Krok 4: výpočet reakcí ze sestavených rovnic. */
export class SolveStep {
  readonly items: Item[];
  readonly unknowns: Unknown[];
  readonly answer: Record<string, number>;
  readonly eqTerms: Term[][];
  vals: Record<string, string> = {};
  status: Record<string, FieldState> = {};
  msgs: Msg[] = [];
  private hintState: { target: string; level: number } | null = null;

  readonly eqs: UserEq[];
  private session: TrainerSession;

  constructor(p: Problem, eqs: UserEq[], session: TrainerSession) {
    this.eqs = eqs;
    this.session = session;
    this.items = allItems(p);
    this.unknowns = unknownsOf(p);
    this.answer = solve(
      eqs.map((e) => e.spec),
      this.items,
      this.unknowns,
    )!;
    this.eqTerms = eqs.map((e) => termsOf(e, this.items));
  }

  get hinted() {
    return this.hintState !== null;
  }

  get solvedSet(): Set<string> {
    return new Set(Object.keys(this.status).filter((k) => this.status[k] === 'ok'));
  }

  get allOk(): boolean {
    const s = this.solvedSet;
    return this.unknowns.every((u) => s.has(u.id));
  }

  setVal(id: string, v: string) {
    // už správně spočítaná reakce se nemění
    if (this.status[id] === 'ok') return;
    this.vals = { ...this.vals, [id]: v };
    if (this.status[id] === 'nudge') {
      const st = { ...this.status };
      delete st[id];
      this.status = st;
    }
    this.session.emit();
  }

  /** Nejvhodnější rovnice pro neznámou (nejméně dalších nevyřešených neznámých). */
  private bestEq(uid: string, solved: Set<string>) {
    let best = -1;
    let bestScore = Infinity;
    this.eqTerms.forEach((terms, i) => {
      if (!terms.some((x) => x.it.unknownId === uid)) return;
      const others = terms.filter((x) => x.it.unknownId && x.it.unknownId !== uid && !solved.has(x.it.unknownId)).length;
      const score = others * 100 + terms.filter((x) => x.it.unknownId).length;
      if (score < bestScore) {
        bestScore = score;
        best = i;
      }
    });
    return { i: best, others: Math.floor(bestScore / 100) };
  }

  /** Další neznámá, kterou lze spočítat hned. */
  private nextTarget(): Unknown | undefined {
    const solved = this.solvedSet;
    const open = this.unknowns.filter((u) => !solved.has(u.id));
    return open.find((u) => this.bestEq(u.id, solved).others === 0) ?? open[0];
  }

  private eqLabel(i: number) {
    return eqTitle(this.eqs[i].spec);
  }

  check() {
    const st: Record<string, FieldState> = { ...this.status };
    const out: Msg[] = [];
    const nowSolved = new Set(this.solvedSet);
    for (const u of this.unknowns) {
      if (st[u.id] === 'ok') continue;
      const raw = this.vals[u.id] ?? '';
      const v = parseNum(raw);
      if (v === null) {
        delete st[u.id];
        continue;
      }
      if (closeTyped(raw, v, this.answer[u.id])) {
        st[u.id] = 'ok';
        nowSolved.add(u.id);
      } else st[u.id] = 'nudge';
    }
    for (const u of this.unknowns) {
      if (st[u.id] !== 'nudge' || out.length >= 2) continue;
      const v = parseNum(this.vals[u.id] ?? '')!;
      const b = this.bestEq(u.id, nowSolved);
      const eq = b.i >= 0 ? this.eqLabel(b.i) : '';
      const ans = this.answer[u.id];
      if (close(-v, ans)) out.push(msg('nudge', (tr) => tr('v_sign'), [7, 4]));
      else if (Math.abs(v - ans) <= 0.05 * Math.abs(ans)) out.push(msg('nudge', (tr) => tr('near')));
      else if (b.others > 0) {
        const other = this.eqTerms[b.i].find((x) => x.it.unknownId && x.it.unknownId !== u.id && !nowSolved.has(x.it.unknownId))!.it.sym;
        out.push(msg('nudge', (tr) => tr('v_dep', { u: u.sym, eq, o: other })));
      } else out.push(msg('nudge', (tr) => tr('v_gen', { u: u.sym, eq })));
    }
    if (this.unknowns.every((u) => st[u.id] === 'ok')) {
      for (const u of this.unknowns)
        if (this.answer[u.id] < 0) out.push(msg('info', (tr) => tr(u.kind === 'm' ? 'v_negM' : 'v_neg', { u: u.sym }), [7, 4]));
    }
    this.status = st;
    this.msgs = out;
    this.session.emit();
  }

  /** Výraz pro neznámou z rovnice i; null když obsahuje nespočítané neznámé. */
  expression(uid: string, i: number): string | null {
    const solved: Record<string, number> = {};
    for (const k of this.solvedSet) solved[k] = this.answer[k];
    return expressionFor(this.eqTerms[i], uid, solved);
  }

  hint() {
    this.session.hint();
    const target = this.nextTarget();
    if (!target) return;
    const level = this.hintState?.target === target.id ? this.hintState.level + 1 : 1;
    this.hintState = { target: target.id, level };
    const solved = this.solvedSet;
    const b = this.bestEq(target.id, solved);
    const coupled = this.unknowns.filter((u) => !solved.has(u.id)).every((u) => this.bestEq(u.id, solved).others > 0);
    const value = fmt(this.answer[target.id]);
    const unit = unitOf(target);
    const sym = target.sym;
    let m: Msg;
    if (coupled && level === 1) {
      const list = this.unknowns
        .filter((u) => !solved.has(u.id) && u.kind !== 'fx')
        .map((u) => `[[${u.sym}]]`)
        .join(', ');
      m = msg('hint', (tr) => tr('v_system', { list }));
    } else if (b.others > 0 && !coupled) {
      m = msg('hint', (tr) => tr('v_coupled', { u: sym }));
    } else if (coupled || level >= 3) {
      m = msg('hint', (tr) => tr('v_h3', { u: sym, val: value, unit }));
      this.vals = { ...this.vals, [target.id]: value };
    } else if (level === 1) {
      const eq = this.eqLabel(b.i);
      m = msg('hint', (tr) => tr('v_h1', { u: sym, eq }));
    } else {
      const expr = this.expression(target.id, b.i) ?? '';
      m = msg('hint', (tr) => tr('v_h2', { u: sym, expr }));
    }
    this.session.highlight = 'r_' + target.id;
    this.msgs = [...this.msgs.filter((x) => x.tone !== 'hint'), m];
    this.session.emit();
  }

  done() {
    this.session.solveDone(this.answer);
  }
}
