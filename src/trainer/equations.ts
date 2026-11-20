import type { Problem } from '../model/types';
import {
  EPS,
  allItems,
  expected,
  forceComponents,
  namedPoints,
  preferredMomentPoint,
  preferredSide,
  type EqSpec,
  type Item,
  type NamedPoint,
} from '../model/mechanics';
import { analyze, analyzeBest, parseEquation, symbolsOf, termText, type Analysis, type Issue, type ParseError, type SymDef } from '../model/eqparse';
import { close, fmt } from '../format';
import type { TKey, Tr } from '../i18n';
import { calcModeMistakes } from './loads';
import { msg, type ArmView, type Msg, type TrainerSession } from './session';

export interface EqRow {
  spec: EqSpec;
  text: string;
  /** výběr v poli (začátek, konec) – sem vkládá paleta */
  sel: [number, number];
  done: boolean;
  lambda: number | null;
  msgs: Msg[];
  hintLevel: number;
  /** zvýší se, když text změnil program (nápověda, paleta) – pole pak přesune kurzor na `sel` */
  rev: number;
}

export const isMoment = (e: EqSpec) => e.kind === 'm' || e.kind === 'hinge';
export const momentPointOf = (e: EqSpec): { label: string; x: number } | null =>
  e.kind === 'm' || e.kind === 'hinge' ? { label: e.point, x: e.x } : null;

/** Název rovnice s tokeny značek, např. „Σ[[M_A]] = 0“. */
export function eqTitle(e: EqSpec): string {
  if (e.kind === 'fx') return 'Σ[[F_x]] = 0';
  if (e.kind === 'fy') return 'Σ[[F_y]] = 0';
  if (e.kind === 'm') return `Σ[[M_${e.point}]] = 0`;
  return `[[M_${e.point}]] = 0`;
}

const DANGLING = /[\s+\-−–*·⋅×/:(=]+$/;
const TOKEN_TAIL = /[A-Za-z][A-Za-z0-9_]*\(?$/;
const LEADING_PLUS = /^\s*\+\s*/;
const SPACES = /\s+/g;
const TRAILING_EQ = /=\s*$/;

/** „Plečo …“ → „plečo …“ za dvojtečkou (ne u značek a zkratek). */
const decap = (s: string) => (s.length > 1 && /\p{Lu}/u.test(s[0]) && /\p{Ll}/u.test(s[1]) ? s[0].toLowerCase() + s.slice(1) : s);

interface IssueText {
  text: (t: Tr) => string;
  arm?: boolean;
  learn?: [number, number];
}

/** Krok 3: rovnice rovnováhy psané jako text, rozbor a nápovědy ke konkrétním členům. */
export class EquationsStep {
  readonly items: Item[];
  readonly defs: SymDef[];
  readonly points: NamedPoint[];
  private readonly best: NamedPoint;
  rows: EqRow[];
  active = 0;
  readonly unknownDefs: SymDef[];
  readonly knownDefs: SymDef[];
  readonly example: string;

  private p: Problem;
  private session: TrainerSession;

  constructor(p: Problem, session: TrainerSession) {
    this.p = p;
    this.session = session;
    this.items = allItems(p);
    this.defs = symbolsOf(p, this.items);
    this.points = namedPoints(p);
    this.best = preferredMomentPoint(p);
    const specs: EqSpec[] = [
      { kind: 'fx' },
      { kind: 'fy' },
      { kind: 'm', point: this.best.label, x: this.best.x },
      ...p.hinges.map((h) => ({ kind: 'hinge' as const, hingeId: h.id, point: h.label, x: h.x, side: preferredSide(p, h.x) })),
    ];
    this.rows = specs.map((spec) => ({ spec, text: '', sel: [0, 0], done: false, lambda: null, msgs: [], hintLevel: 0, rev: 0 }));
    this.unknownDefs = this.defs.filter((d) => d.kind === 'unknown');
    this.knownDefs = this.defs.filter((d) => d.kind === 'known').sort((a, b) => naturalCompare(a.name, b.name));
    this.example = exampleOf(this.defs);
    this.show(0, null, false);
  }

  get allDone(): boolean {
    return this.rows.every((r) => r.done);
  }

  private changed() {
    this.session.emit();
  }

  private show(i: number, arm: ArmView | null = null, emit = true) {
    const r = this.rows[i];
    this.session.view = {
      momentPoint: momentPointOf(r.spec),
      side: r.spec.kind === 'hinge' && r.done ? { x: r.spec.x, side: r.spec.side } : null,
      arm,
    };
    if (emit) this.changed();
  }

  private patch(i: number, f: (r: EqRow) => EqRow) {
    const before = this.rows[i];
    let after = f(before);
    // po úpravě textu už staré hodnocení neplatí (nápověda zůstává)
    if (after.text !== before.text && !after.done && after.msgs === before.msgs) {
      after = { ...after, msgs: after.msgs.filter((m) => m.tone === 'hint') };
      this.session.highlight = null;
    }
    this.rows = this.rows.map((r, j) => (j === i ? after : r));
  }

  focusRow(i: number) {
    if (i !== this.active) {
      this.active = i;
      this.show(i);
    }
  }

  /** Změna textu z klávesnice (nebo jen posun kurzoru). */
  onValueChange(i: number, text: string, sel: [number, number]) {
    this.focusRow(i);
    this.patch(i, (r) => (r.text === text ? { ...r, sel } : { ...r, text, sel, done: false }));
    this.changed();
  }

  /** Jen posun kurzoru (bez překreslení). */
  onSelect(i: number, sel: [number, number]) {
    const r = this.rows[i];
    r.sel = sel;
  }

  /** Vložení textu z palety na místo kurzoru v aktivním poli. */
  insert(snippet0: string) {
    const r = this.rows[this.active];
    const s = Math.min(Math.min(r.sel[0], r.sel[1]), r.text.length);
    const e = Math.min(Math.max(r.sel[0], r.sel[1]), r.text.length);
    let snippet = snippet0;
    // značka vložená hned za písmeno/číslo by s ním splynula (F + R_A → FR_A) – oddělíme mezerou
    const before = r.text.slice(0, s);
    if (/^[A-Za-z]/.test(snippet) && /[A-Za-z0-9_)]$/.test(before)) snippet = ' ' + snippet;
    const text = before + snippet + r.text.slice(e);
    const at = s + snippet.length;
    this.patch(this.active, (x) => ({ ...x, text, sel: [at, at], done: false, rev: x.rev + 1 }));
    this.changed();
  }

  /** ⌫ z palety: smaže výběr, jinak znak před kurzorem (značku nebo „sin(“ celou). */
  backspace() {
    const r = this.rows[this.active];
    const t = r.text;
    let s = Math.min(Math.min(r.sel[0], r.sel[1]), t.length);
    const e = Math.min(Math.max(r.sel[0], r.sel[1]), t.length);
    if (s === e) {
      while (s > 0 && t[s - 1] === ' ') s--;
      const tok = TOKEN_TAIL.exec(t.slice(0, s));
      s = tok ? s - tok[0].length : Math.max(0, s - 1);
      while (s > 0 && t[s - 1] === ' ') s--;
    }
    this.patch(this.active, (x) => ({ ...x, text: t.slice(0, s) + t.slice(e), sel: [s, s], done: false, rev: x.rev + 1 }));
    this.changed();
  }

  clear() {
    this.patch(this.active, (x) => ({ ...x, text: '', sel: [0, 0], done: false, rev: x.rev + 1 }));
    this.changed();
  }

  /**
   * Pro kloub obě části nosníku; zvolená (doporučená) část je první, takže při stejně dobré shodě vyhraje ona.
   * (Kdyby vyhrávala vždy levá, nápověda by vložila člen pravé části, kontrola by přepnula na levou
   * a další nápověda by ho zase smazala – dokola.)
   */
  private specsFor(spec: EqSpec): EqSpec[] {
    return spec.kind === 'hinge' ? [spec, { ...spec, side: spec.side === 'left' ? 'right' : 'left' }] : [spec];
  }

  private errorMsg(e: ParseError, text: string): (t: Tr) => string {
    switch (e.kind) {
      case 'unknownSym': {
        // jako příklad nejpodobnější značka (překlep MB → M_A), ne první v paletě
        const near = `[[${closestSym(e.name, this.defs).name}]]`;
        return (tr) => tr('p_unknownSym', { s: e.name, ex: near });
      }
      case 'nonlinear':
        return (tr) => tr('p_nonlinear');
      case 'div0':
        return (tr) => tr('p_div0');
      case 'twoEq':
        return (tr) => tr('p_twoEq');
      case 'badValue':
        return (tr) => tr('p_badValue');
      case 'syntax': {
        // chyba až na konci = visící znaménko nebo neuzavřená závorka
        if (e.at >= text.trimEnd().length) return (tr) => tr('p_dangling');
        const at = text.slice(Math.min(e.at, text.length), Math.min(e.at + 8, text.length)) || '…';
        return (tr) => tr('p_syntax', { at });
      }
    }
  }

  private run(r: EqRow): { a?: Analysis; err?: Msg } {
    const parsed = parseEquation(r.text, this.defs);
    if (parsed.error) return { err: msg('nudge', this.errorMsg(parsed.error, r.text)) };
    const a = r.spec.kind === 'hinge' ? analyzeBest(parsed, this.specsFor(r.spec), this.items, this.defs) : analyze(parsed, r.spec, this.items, this.defs);
    return { a };
  }

  private loadOf(it: Item) {
    return this.p.loads.find((l) => l.id === it.loadId);
  }

  private issueText(issue: Issue, a: Analysis): IssueText {
    const it = issue.item;
    const s = it.sym;
    const spec = a.spec;
    const P = momentPointOf(spec)?.label ?? '';
    const pos = (a.lambda ?? 1) > 0;
    switch (issue.kind) {
      case 'extra': {
        const e = expected(it, spec);
        if (e.excluded && spec.kind === 'hinge') {
          const other = spec.side === 'left' ? 'rightPart' : 'leftPart';
          return { text: (tr) => tr('r_otherSide', { s, P, side: tr(other) }), learn: [9, 1] };
        }
        if (spec.kind === 'fx') return { text: (tr) => tr(it.kind === 'm' ? 'r_coupleInF' : 'r_vertInX', { s }), learn: [2, 3] };
        if (spec.kind === 'fy') return { text: (tr) => tr(it.kind === 'm' ? 'r_coupleInF' : 'r_horInY', { s }), learn: [2, 3] };
        return { text: (tr) => tr(it.kind === 'fx' ? 'r_horInM' : 'r_atPoint', { s, P }), learn: [4, 2] };
      }
      case 'missing': {
        if (spec.kind === 'fx') return { text: (tr) => tr('r_forgotX', { s }), learn: [2, 3] };
        if (spec.kind === 'fy') return { text: (tr) => tr('r_forgotY', { s }), learn: [2, 3] };
        if (it.kind === 'm') return { text: (tr) => tr('r_forgotCouple', { s }), learn: [4, 4] };
        return { text: (tr) => tr('r_forgotM', { s, P }), arm: true, learn: [4, 1] };
      }
      case 'sign': {
        if (!isMoment(spec)) {
          const dir = spec.kind === 'fx' ? (pos ? '→' : '←') : pos ? '↑' : '↓';
          return { text: (tr) => tr('p_signF', { s, pos: dir }), learn: [2, 3] };
        }
        return { text: (tr) => tr('p_signM', { s, P, pos: pos ? '↺' : '↻' }), learn: [4, 3] };
      }
      case 'mag': {
        // nesedí velikost
        if (!isMoment(spec)) {
          const l = this.loadOf(it);
          if (l?.kind === 'udl') return { text: (tr) => tr('p_qInsteadQ', { s }), learn: [8, 1] };
          // celá šikmá síla místo její složky (F_1 místo F_1x)
          if (l?.kind === 'force' && l.alpha && issue.impliedArm !== undefined && it.value !== undefined && close(issue.impliedArm, l.F / it.value))
            return { text: (tr) => tr('p_useComp', { s, F: l.label }), learn: [3, 1] };
          const derived = !!l && l.kind !== 'moment' && it.sym !== l.label;
          // u složky šikmé síly: zaměněná složka → trigonometrie (sin/cos), kalkulačka v RAD → režimy kalkulačky
          let compLink: [number, number] | undefined;
          if (derived && l?.kind === 'force' && l.alpha && issue.impliedArm !== undefined && it.value !== undefined) {
            const typed = issue.impliedArm * it.value;
            const comps = forceComponents(l.F, l.angle);
            const other = it.kind === 'fx' ? Math.abs(comps.fy) : Math.abs(comps.fx);
            const v = it.value;
            if (close(typed, other)) compLink = [102, 1];
            else if (calcModeMistakes(l.F, l.alpha).some((m) => close(typed, m) && !close(m, Math.abs(v)))) compLink = [103, 1];
          }
          return { text: (tr) => tr(derived ? 'p_compMag' : 'p_forceMag', { s }), learn: compLink ?? [3, 3] };
        }
        if (it.kind === 'm') return { text: (tr) => tr('p_coupleMag', { s }), learn: [4, 4] };
        if (momentPointOf(spec)!.x > EPS && issue.impliedArm !== undefined && close(issue.impliedArm, it.x))
          return { text: (tr) => tr('r_armFromEnd', { s, P }), arm: true, learn: [4, 1] };
        if (this.loadOf(it)?.kind === 'udl') return { text: (tr) => tr('r_armQ', { s, P }), arm: true, learn: [8, 0] };
        return { text: (tr) => tr('r_arm', { s, P }), arm: true, learn: [4, 1] };
      }
    }
  }

  private strayFromCalcMode(n: number): boolean {
    return this.p.loads.some((l) => {
      if (l.kind !== 'force' || !l.alpha) return false;
      const c = forceComponents(l.F, l.angle);
      return calcModeMistakes(l.F, l.alpha).some((m) => close(Math.abs(n), m) && !close(m, Math.abs(c.fx)) && !close(m, Math.abs(c.fy)));
    });
  }

  private strayMsg(n: number): (t: Tr) => string {
    const ex = this.defs.find((d) => d.kind === 'known')?.name ?? 'F_1';
    return (tr) => tr('p_number', { n: fmt(Math.abs(n)), ex });
  }

  check(i: number, quiet = false) {
    this.checkRow(i, quiet);
    this.changed();
  }

  private checkRow(i: number, quiet: boolean) {
    if (!quiet) this.focusRow(i);
    const r = this.rows[i];
    if (!r.text.trim()) {
      if (!quiet) this.patch(i, (x) => ({ ...x, msgs: [msg('nudge', (tr) => tr('p_empty'))] }));
      return;
    }
    const res = this.run(r);
    if (res.err) {
      const err = res.err;
      return this.patch(i, (x) => ({ ...x, msgs: [err], done: false }));
    }
    const a = res.a;
    if (!a) return;
    if (a.ok) {
      const side = a.spec.kind === 'hinge' ? a.spec.side : null;
      this.patch(i, (x) => ({
        ...x,
        spec: a.spec,
        done: true,
        lambda: a.lambda,
        msgs: [msg('ok', (tr) => tr('eqDone') + (side ? ' (' + tr('p_sideFound', { side: tr(side) }) + ')' : ''))],
      }));
      this.show(i, null, false);
      this.session.highlight = null;
      // rovnou k další nehotové rovnici
      if (i === this.active && !quiet) {
        const order = [...this.rows.keys()].slice(i + 1).concat([...this.rows.keys()].slice(0, i));
        const next = order.find((k) => !this.rows[k].done);
        if (next !== undefined) this.focusRow(next);
      }
      return;
    }
    const msgs: Msg[] = [];
    if (a.empty) msgs.push(msg('nudge', (tr) => tr('p_empty')));
    for (const issue of a.issues.slice(0, 2)) {
      const it = this.issueText(issue, a);
      const sym = issue.item.sym;
      // každá zpráva musí říct, o kterou sílu jde
      msgs.push(
        msg(
          'nudge',
          (tr) => {
            const t = it.text(tr);
            return t.includes(`[[${sym}]]`) ? t : tr('e_hRow', { s: sym }) + ' ' + decap(t);
          },
          it.learn,
        ),
      );
    }
    if (a.stray.length && msgs.length < 3) {
      // číslo, které vyjde ze složky šikmé síly s kalkulačkou v RAD/GRAD → výklad o režimech kalkulačky
      msgs.push(msg('nudge', this.strayMsg(a.stray[0]), this.strayFromCalcMode(a.stray[0]) ? [103, 1] : undefined));
    }
    this.patch(i, (x) => ({ ...x, msgs, done: false }));
  }

  checkAll() {
    for (let i = 0; i < this.rows.length; i++) if (!this.rows[i].done) this.checkRow(i, true);
    // paleta nesmí dál psát do právě hotové rovnice
    if (this.rows[this.active].done) {
      const k = this.rows.findIndex((r) => !r.done);
      if (k >= 0) this.focusRow(k);
    }
    this.changed();
  }

  hint() {
    this.session.hint();
    const i = !this.rows[this.active].done ? this.active : this.rows.findIndex((r) => !r.done);
    if (i < 0) return;
    const r = this.rows[i];
    const lvl = r.hintLevel + 1;
    const spec = r.spec;
    const P = momentPointOf(spec)?.label ?? '';
    let m: (t: Tr) => string;
    let text = r.text;
    let arm: ArmView | null = null;
    const perr = parseEquation(r.text, this.defs).error;
    const a = !perr && r.text.trim() ? this.run(r).a : undefined;
    const aSpec = a?.spec ?? spec;
    const first = a?.issues[0];
    if (lvl === 1) {
      const k: TKey = spec.kind === 'fx' ? 'e_h1_fx' : spec.kind === 'fy' ? 'e_h1_fy' : spec.kind === 'm' ? 'e_h1_m' : 'e_h1_hinge';
      const sideKey = spec.kind === 'hinge' ? preferredSide(this.p, spec.x) : null;
      const best = this.best.label;
      m = (tr) => tr(k, { P, best, side: sideKey ? tr(sideKey) : '' }) + (spec.kind === 'hinge' ? ' ' + tr('p_hingeInfo', { P }) : '');
    } else if (perr) {
      m = this.errorMsg(perr, r.text);
      // od 3. úrovně neznámou značku rovnou nahradíme nejpodobnější z palety, jiný zápis opravíme
      if (lvl >= 3 && perr.kind === 'unknownSym') {
        const d = closestSym(perr.name, this.defs);
        text = text.slice(0, perr.at) + d.name + text.slice(perr.at + perr.name.length);
        m = (tr) => tr('p_replaced', { from: perr.name, to: d.name });
      } else if (lvl >= 3) {
        text = this.repair(text, perr);
        const shown = text;
        m = (tr) => tr('p_cut', { text: shown });
      }
    } else if (lvl === 2 && a && (first || a.stray.length)) {
      if (first) {
        const it = this.issueText(first, a);
        const sym = first.item.sym;
        m = (tr) => tr('e_hRow', { s: sym }) + ' ' + decap(it.text(tr));
        const mp = momentPointOf(aSpec);
        if (it.arm && mp) arm = { from: mp.x, to: first.item.x };
        this.session.highlight = first.item.id;
      } else m = this.strayMsg(a.stray[0]);
    } else {
      // vložit nebo opravit jeden člen rovnice
      const target: Item | undefined =
        first?.item ??
        [...this.items].sort((x, y) => Number(!!y.unknownId) - Number(!!x.unknownId)).find((it) => expected(it, aSpec).sign !== 0 && !a?.spans[it.id]);
      if (!target) {
        const sp = a?.straySpans[0];
        if (sp) {
          // číslo, které nepatří k žádné síle, odstraníme
          const term = text.slice(sp.s, Math.min(sp.e, text.length)).trim();
          text = (text.slice(0, sp.s) + text.slice(Math.min(sp.e, text.length))).replace(LEADING_PLUS, '').replace(SPACES, ' ').replace(TRAILING_EQ, '= 0').trim();
          m = (tr) => tr('p_removed', { term });
        } else m = (tr) => tr('e_hAllOk');
      } else {
        const seen = new Set<string>();
        const old = (a?.spans[target.id] ?? []).filter((sp) => {
          const k = sp.s + ':' + sp.e;
          if (seen.has(k)) return false;
          seen.add(k);
          return true;
        });
        const replaced = old.length > 0;
        for (const sp of [...old].sort((x, y) => y.s - x.s)) {
          const e = Math.min(sp.e, text.length);
          const st = Math.min(sp.s, e);
          text = text.slice(0, st) + text.slice(e);
        }
        const extra = first?.kind === 'extra';
        let term = '';
        if (!extra) {
          const eqAt = text.indexOf('=');
          const lhs = eqAt >= 0 ? text.slice(0, eqAt) : text;
          const rest = eqAt >= 0 ? text.slice(eqAt) : '';
          const ls = Math.sign(a?.lambda ?? 1) || 1;
          const sc = a?.lambda != null && Math.abs(a.lambda) > 1e-9 ? Math.abs(a.lambda) : 1;
          term = termText(target, aSpec, ls, !lhs.trim(), sc);
          text = lhs.trimEnd() + term + (rest ? ' ' + rest.trim() : '');
        }
        text = text.replace(LEADING_PLUS, '').replace(SPACES, ' ').replace(TRAILING_EQ, '= 0').trim();
        const shown = term.trim();
        const sym = target.sym;
        // pravdivě: člen přibyl, byl opraven, nebo odstraněn
        m = extra ? (tr) => tr('p_removedSym', { s: sym }) : replaced ? (tr) => tr('p_fixedTerm', { s: sym, term: shown }) : (tr) => tr('p_insert', { term: shown });
        this.session.highlight = target.id;
        const mp = momentPointOf(aSpec);
        if (isMoment(aSpec) && target.kind === 'fy' && mp) arm = { from: mp.x, to: target.x };
      }
    }
    const final = m;
    this.patch(i, (x) => ({
      ...x,
      hintLevel: lvl,
      ...(text === x.text ? {} : { text, sel: [text.length, text.length] as [number, number], rev: x.rev + 1 }),
      done: false,
      msgs: [...x.msgs.filter((mm) => mm.tone !== 'hint'), msg('hint', final)],
    }));
    this.active = i;
    this.show(i, arm);
  }

  setPoint(i: number, label: string, x: number) {
    this.patch(i, (r) => ({ ...r, spec: { kind: 'm', point: label, x }, done: false, msgs: [] }));
    this.active = i;
    this.show(i);
  }

  done() {
    this.session.equationsDone(this.rows.map((r) => ({ spec: r.spec, cw: (r.lambda ?? 1) < 0 })));
  }

  /** Oprava zápisu, kterému parser nerozumí: zkrátí ho k místu chyby, odstraní visící znaménka a doplní závorky. */
  private repair(src: string, err0: ParseError): string {
    let text = src;
    let err: ParseError | undefined = err0;
    let n = 0;
    while (err && err.kind !== 'unknownSym' && n++ < 4) {
      if (err.at < text.trimEnd().length) text = text.slice(0, err.at);
      text = text.replace(DANGLING, '');
      const open = (text.match(/\(/g)?.length ?? 0) - (text.match(/\)/g)?.length ?? 0);
      if (open > 0) text += ')'.repeat(open);
      err = parseEquation(text, this.defs).error;
    }
    return text.trim();
  }
}

function lev(a: string, b: string): number {
  const d = Array.from({ length: a.length + 1 }, (_, i) => Array.from({ length: b.length + 1 }, (_, j) => (i === 0 ? j : j === 0 ? i : 0)));
  for (let i = 1; i <= a.length; i++)
    for (let j = 1; j <= b.length; j++) d[i][j] = Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1));
  return d[a.length][b.length];
}

/** Nejpodobnější značka z palety (pro opravu překlepu). */
export function closestSym(raw: string, defs: SymDef[]): SymDef {
  const n = (s: string) => s.replace(/_/g, '').toLowerCase();
  return defs.reduce((b, d) => (lev(n(raw), n(d.name)) < lev(n(raw), n(b.name)) ? d : b));
}

export function exampleOf(defs: SymDef[]): string {
  const u = defs.find((d) => d.kind === 'unknown' && !d.name.endsWith('x')) ?? defs[0];
  const k = defs.find((d) => d.kind === 'known' && !d.name.startsWith('q'));
  return `${u.name.replace('_', '')}*4` + (k ? ` - ${k.name.replace('_', '')}*1,5` : '') + ' = 0';
}

/** Řazení jako localeCompare(…, { numeric: true }): F_2 před F_10. */
export function naturalCompare(a: string, b: string): number {
  return a.localeCompare(b, 'en', { numeric: true });
}
