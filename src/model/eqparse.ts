/*
 * Rozbor rovnice zapsané žákem a její porovnání se správnou rovnicí.
 *
 * Rovnice se převede na lineární tvar  Σ a·(značka) + c = 0.  Značky jsou neznámé reakce
 * i známá zatížení (F_1, Q, q, M …) – díky tomu lze každý člen přiřadit konkrétní síle.
 * Správnost se posuzuje až na nenulový násobek (znaménková konvence, vynásobení rovnice).
 */
import { EPS, expected, type EqSpec, type Item } from './mechanics';
import type { Problem } from './types';

// ---------------------------------------------------------------- značky

export interface SymDef {
  /** kanonický zápis, např. R_Ax */
  name: string;
  kind: 'unknown' | 'known';
  /** id neznámé (u reakcí) */
  unknownId?: string;
  /** hodnota známé veličiny */
  value?: number;
  unit?: string;
  /** k čemu značka patří: id položek (pro F_1 u šikmé síly obě složky) */
  items: string[];
}

const norm = (s: string) => s.replace(/_/g, '');

export function symbolsOf(p: Problem, items: Item[]): SymDef[] {
  const out: SymDef[] = [];
  for (const it of items)
    out.push(
      it.unknownId
        ? { name: it.sym, kind: 'unknown', unknownId: it.unknownId, items: [it.id], unit: it.kind === 'm' ? 'kNm' : 'kN' }
        : { name: it.sym, kind: 'known', value: it.value, items: [it.id], unit: it.kind === 'm' ? 'kNm' : 'kN' },
    );
  for (const l of p.loads) {
    if (l.kind === 'force' && l.alpha)
      out.push({ name: l.label, kind: 'known', value: l.F, unit: 'kN', items: items.filter((i) => i.loadId === l.id).map((i) => i.id) });
    if (l.kind === 'udl') out.push({ name: l.label, kind: 'known', value: l.q, unit: 'kN/m', items: items.filter((i) => i.loadId === l.id).map((i) => i.id) });
  }
  return out;
}

function lookup(defs: SymDef[], raw: string): SymDef | undefined {
  const n = norm(raw);
  const exact = defs.find((d) => norm(d.name) === n);
  if (exact) return exact;
  const ci = defs.filter((d) => norm(d.name).toLowerCase() === n.toLowerCase());
  return ci.length === 1 ? ci[0] : undefined;
}

// ---------------------------------------------------------------- lexer

export type Tok =
  | { t: 'num'; v: number; s: number; e: number }
  | { t: 'id'; v: string; s: number; e: number }
  | { t: 'op'; v: string; s: number; e: number };

const OPS: Record<string, string> = { '+': '+', '-': '-', '−': '-', '–': '-', '*': '*', '·': '*', '⋅': '*', '×': '*', '/': '/', ':': '/', '(': '(', ')': ')', '=': '=' };

export function tokenize(src: string): { toks: Tok[]; bad?: number } {
  const toks: Tok[] = [];
  let i = 0;
  while (i < src.length) {
    const ch = src[i];
    if (/\s|°/.test(ch)) {
      i++;
      continue;
    }
    const num = /^\d+(?:[.,]\d+)?|^[.,]\d+/.exec(src.slice(i));
    if (num) {
      toks.push({ t: 'num', v: Number(num[0].replace(',', '.')), s: i, e: i + num[0].length });
      i += num[0].length;
      continue;
    }
    const id = /^[A-Za-z][A-Za-z0-9_]*/.exec(src.slice(i));
    if (id) {
      toks.push({ t: 'id', v: id[0], s: i, e: i + id[0].length });
      i += id[0].length;
      continue;
    }
    if (OPS[ch]) {
      toks.push({ t: 'op', v: OPS[ch], s: i, e: i + 1 });
      i++;
      continue;
    }
    return { toks, bad: i };
  }
  return { toks };
}

// ---------------------------------------------------------------- parser

/** Lineární výraz: klíče 'u:<id neznámé>' a 'k:<značka>' + konstanta. */
export interface Lin {
  a: Record<string, number>;
  c: number;
}

export interface TopTerm {
  lin: Lin;
  /** rozsah v textu včetně znaménka před členem */
  s: number;
  e: number;
  /** číselné činitele přímo v členu (pro přiřazení čísel k silám) */
  nums: number[];
}

export type ParseError =
  | { kind: 'syntax'; at: number }
  | { kind: 'unknownSym'; name: string; at: number }
  | { kind: 'nonlinear'; at: number }
  | { kind: 'div0'; at: number }
  | { kind: 'twoEq'; at: number };

export interface Parsed {
  terms: TopTerm[];
  hasEq: boolean;
  error?: ParseError;
}

class Fail extends Error {
  err: ParseError;
  constructor(err: ParseError) {
    super(err.kind);
    this.err = err;
  }
}

const isConst = (l: Lin) => Object.values(l.a).every((v) => Math.abs(v) < 1e-12);
const scale = (l: Lin, k: number): Lin => ({ a: Object.fromEntries(Object.entries(l.a).map(([x, v]) => [x, v * k])), c: l.c * k });
const add = (x: Lin, y: Lin): Lin => {
  const a = { ...x.a };
  for (const [k, v] of Object.entries(y.a)) a[k] = (a[k] ?? 0) + v;
  return { a, c: x.c + y.c };
};

const FUNCS: Record<string, (x: number) => number> = {
  sin: (x) => Math.sin((x * Math.PI) / 180),
  cos: (x) => Math.cos((x * Math.PI) / 180),
  tan: (x) => Math.tan((x * Math.PI) / 180),
  tg: (x) => Math.tan((x * Math.PI) / 180),
  sqrt: Math.sqrt,
};

export function parseEquation(src: string, defs: SymDef[]): Parsed {
  const { toks, bad } = tokenize(src);
  if (bad !== undefined) return { terms: [], hasEq: false, error: { kind: 'syntax', at: bad } };
  let i = 0;
  const peek = () => toks[i];
  const isOp = (v: string) => peek()?.t === 'op' && peek()!.v === v;
  const end = () => (i < toks.length ? toks[i].s : src.length);

  function factor(): { lin: Lin; num?: number } {
    const tk = peek();
    if (!tk) throw new Fail({ kind: 'syntax', at: src.length });
    if (tk.t === 'op' && (tk.v === '-' || tk.v === '+')) {
      i++;
      const f = factor();
      return tk.v === '-' ? { lin: scale(f.lin, -1), num: f.num !== undefined ? -f.num : undefined } : f;
    }
    if (tk.t === 'num') {
      i++;
      return { lin: { a: {}, c: tk.v }, num: tk.v };
    }
    if (tk.t === 'id') {
      i++;
      const fn = FUNCS[tk.v.toLowerCase()];
      if (fn && isOp('(')) {
        i++;
        const inner = expr();
        if (!isOp(')')) throw new Fail({ kind: 'syntax', at: end() });
        i++;
        if (!isConst(inner)) throw new Fail({ kind: 'nonlinear', at: tk.s });
        return { lin: { a: {}, c: fn(inner.c) } };
      }
      const d = lookup(defs, tk.v);
      if (!d) throw new Fail({ kind: 'unknownSym', name: tk.v, at: tk.s });
      const key = d.kind === 'unknown' ? 'u:' + d.unknownId : 'k:' + d.name;
      return { lin: { a: { [key]: 1 }, c: 0 } };
    }
    if (tk.t === 'op' && tk.v === '(') {
      i++;
      const inner = expr();
      if (!isOp(')')) throw new Fail({ kind: 'syntax', at: end() });
      i++;
      return { lin: inner };
    }
    throw new Fail({ kind: 'syntax', at: tk.s });
  }

  function term(): { lin: Lin; nums: number[] } {
    const first = factor();
    let lin = first.lin;
    const nums: number[] = first.num !== undefined ? [Math.abs(first.num)] : [];
    for (;;) {
      const tk = peek();
      if (!tk) break;
      const implicit = tk.t === 'num' || tk.t === 'id' || (tk.t === 'op' && tk.v === '(');
      if (tk.t === 'op' && (tk.v === '*' || tk.v === '/')) i++;
      else if (!implicit) break;
      const at = tk.s;
      const f = factor();
      if (tk.t === 'op' && tk.v === '/') {
        if (!isConst(f.lin)) throw new Fail({ kind: 'nonlinear', at });
        if (Math.abs(f.lin.c) < 1e-12) throw new Fail({ kind: 'div0', at });
        lin = scale(lin, 1 / f.lin.c);
      } else {
        if (isConst(lin)) lin = add(scale(f.lin, lin.c), { a: {}, c: 0 });
        else if (isConst(f.lin)) lin = scale(lin, f.lin.c);
        else throw new Fail({ kind: 'nonlinear', at });
        if (f.num !== undefined) nums.push(Math.abs(f.num));
      }
    }
    return { lin, nums };
  }

  function expr(top?: TopTerm[], side = 1): Lin {
    let sum: Lin = { a: {}, c: 0 };
    let first = true;
    for (;;) {
      const s = end();
      let sign = 1;
      if (isOp('+') || isOp('-')) {
        sign = peek()!.v === '-' ? -1 : 1;
        i++;
      } else if (!first) break;
      const tm = term();
      const lin = scale(tm.lin, sign);
      sum = add(sum, lin);
      top?.push({ lin: scale(lin, side), s, e: end(), nums: tm.nums });
      first = false;
      if (!(isOp('+') || isOp('-'))) break;
    }
    return sum;
  }

  const terms: TopTerm[] = [];
  let hasEq = false;
  try {
    if (!toks.length) return { terms, hasEq };
    if (!isOp('=')) expr(terms, 1);
    if (isOp('=')) {
      hasEq = true;
      i++;
      if (i < toks.length) expr(terms, -1);
    }
    if (isOp('=')) throw new Fail({ kind: 'twoEq', at: peek()!.s });
    if (i < toks.length) throw new Fail({ kind: 'syntax', at: peek()!.s });
  } catch (e) {
    if (e instanceof Fail) return { terms, hasEq, error: e.err };
    throw e;
  }
  return { terms, hasEq };
}

// ---------------------------------------------------------------- porovnání

export type IssueKind = 'extra' | 'missing' | 'sign' | 'mag';

export interface Issue {
  kind: IssueKind;
  item: Item;
  /** rameno, které žák (patrně) použil */
  impliedArm?: number;
}

export interface Analysis {
  ok: boolean;
  empty: boolean;
  /** násobek: rovnice žáka = λ · správná rovnice */
  lambda: number | null;
  issues: Issue[];
  /** čísla, která nejde přiřadit žádné síle */
  stray: number[];
  /** položka → rozsahy členů v textu (pro nápovědu, která člen nahradí) */
  spans: Record<string, { s: number; e: number }[]>;
  spec: EqSpec;
}

const near = (a: number, b: number, tol = 0.015) => Math.abs(a - b) <= tol * Math.max(Math.abs(a), Math.abs(b)) + 1e-6;

/** Pro šikmou sílu F_1: v ΣFx patří k vodorovné složce, jinde ke svislé. */
function pickItem(d: SymDef, eq: EqSpec, items: Item[]): Item | undefined {
  const cands = d.items.map((id) => items.find((i) => i.id === id)!).filter(Boolean);
  if (cands.length === 1) return cands[0];
  return cands.find((c) => c.kind === (eq.kind === 'fx' ? 'fx' : 'fy')) ?? cands[0];
}

export function analyze(parsed: Parsed, spec: EqSpec, items: Item[], defs: SymDef[], p: Problem): Analysis {
  const coef: Record<string, number> = {};
  const spans: Record<string, { s: number; e: number }[]> = {};
  const stray: number[] = [];
  const addTo = (id: string, v: number, sp: { s: number; e: number }) => {
    coef[id] = (coef[id] ?? 0) + v;
    (spans[id] ??= []).push(sp);
  };

  for (const tm of parsed.terms) {
    const sp = { s: tm.s, e: tm.e };
    for (const [key, k] of Object.entries(tm.lin.a)) {
      if (Math.abs(k) < 1e-12) continue;
      if (key.startsWith('u:')) {
        const it = items.find((x) => x.unknownId === key.slice(2))!;
        addTo(it.id, k, sp);
      } else {
        const d = defs.find((x) => 'k:' + x.name === key)!;
        const it = pickItem(d, spec, items)!;
        addTo(it.id, (k * d.value!) / it.value!, sp);
      }
    }
    // čistě číselný člen – zkusíme ho přiřadit síle podle činitele
    if (Math.abs(tm.lin.c) > 1e-9) {
      const cands: { it: Item; base: number }[] = [];
      for (const n of tm.nums)
        for (const d of defs)
          if (d.kind === 'known' && near(n, d.value!, 0.01)) {
            const it = pickItem(d, spec, items);
            if (it) cands.push({ it, base: it.value! });
          }
      // přednost má síla, která do rovnice patří a ještě nemá člen
      const score = (c: { it: Item }) => (coef[c.it.id] ? 0 : 2) + (expected(c.it, spec).sign !== 0 ? 1 : 0);
      cands.sort((a, b) => score(b) - score(a));
      if (cands.length) addTo(cands[0].it.id, tm.lin.c / cands[0].base, sp);
      else stray.push(tm.lin.c);
    }
  }

  const exp: Record<string, number> = {};
  for (const it of items) {
    const e = expected(it, spec);
    exp[it.id] = e.sign * (e.arm ?? 1);
  }
  const user = (id: string) => (Math.abs(coef[id] ?? 0) < 1e-9 ? 0 : coef[id]);
  const empty = items.every((it) => user(it.id) === 0) && !stray.length;

  // λ: poměr, se kterým souhlasí nejvíc členů. Běžně žák píše rovnici bez násobení (λ = ±1),
  // proto ±1 vyhrává při shodě; když se neshodnou ani dva členy, bereme ±1 podle většiny znamének.
  const ratios: { r: number; unk: boolean }[] = [{ r: 1, unk: false }, { r: -1, unk: false }];
  let signVote = 0;
  for (const it of items) {
    const u = user(it.id);
    const e = exp[it.id];
    if (!u || !e) continue;
    ratios.push({ r: u / e, unk: !!it.unknownId });
    signVote += Math.sign(u / e);
  }
  let lambda: number | null = null;
  let bestScore = -1;
  for (const { r, unk } of ratios) {
    let sc = 0;
    for (const j of items) if (user(j.id) && exp[j.id] && near(user(j.id), r * exp[j.id])) sc += j.unknownId ? 1.01 : 1;
    sc += Math.abs(Math.abs(r) - 1) < 1e-9 ? 0.5 : unk ? 0.02 : 0;
    if (sc > bestScore) {
      bestScore = sc;
      lambda = r;
    }
  }
  if (bestScore < 2 && ratios.length > 2) lambda = signVote < 0 ? -1 : 1;
  if (ratios.length === 2) lambda = null;

  const issues: Issue[] = [];
  for (const it of items) {
    const u = user(it.id);
    const e = exp[it.id];
    if (!u && !e) continue;
    if (u && !e) issues.push({ kind: 'extra', item: it });
    else if (!u && e) issues.push({ kind: 'missing', item: it });
    else if (lambda !== null && !near(u, lambda * e)) {
      if (Math.sign(u) !== Math.sign(lambda * e)) issues.push({ kind: 'sign', item: it });
      else issues.push({ kind: 'mag', item: it, impliedArm: Math.abs(u / lambda) });
    }
  }
  // chybějící členy až na konec – nejdřív to, co je napsané
  issues.sort((a, b) => Number(a.kind === 'missing') - Number(b.kind === 'missing'));
  void p;
  return { ok: !empty && !issues.length && !stray.length && lambda !== null, empty, lambda, issues, stray, spans, spec };
}

/** Pro kloub zkusí obě části nosníku a vezme tu, která k zápisu sedí lépe. */
export function analyzeBest(parsed: Parsed, specs: EqSpec[], items: Item[], defs: SymDef[], p: Problem): Analysis {
  const res = specs.map((s) => analyze(parsed, s, items, defs, p));
  const cost = (a: Analysis) => (a.ok ? -1 : a.issues.length + a.stray.length);
  return res.reduce((b, a) => (cost(a) < cost(b) ? a : b));
}

/** Text správného členu pro nápovědu, např. „- F_1y*2“. */
export function termText(it: Item, spec: EqSpec, lambdaSign: number, first: boolean): string {
  const e = expected(it, spec);
  const sgn = e.sign * lambdaSign;
  const arm = e.arm !== null && e.arm > EPS && it.kind === 'fy' ? '*' + String(+e.arm.toFixed(3)).replace('.', ',') : '';
  const body = it.sym + arm;
  if (first) return (sgn < 0 ? '-' : '') + body;
  return (sgn < 0 ? ' - ' : ' + ') + body;
}
