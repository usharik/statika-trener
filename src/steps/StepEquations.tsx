import { useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import type { Problem } from '../model/types';
import { EPS, allItems, expected, namedPoints, preferredMomentPoint, preferredSide, type EqSpec, type Item } from '../model/mechanics';
import { analyze, analyzeBest, parseEquation, symbolsOf, termText, tokenize, type Analysis, type Issue, type SymDef } from '../model/eqparse';
import { useT, type TKey } from '../i18n';
import { HintButton, Notes, type Msg } from '../components/ui';
import { Rich, Sym } from '../components/Sym';
import { close, fmt } from '../format';

export type Conv = 'ccw' | 'cw';

/** Rovnice předaná do výpočtu; conv 'cw' = rovnice vynásobená −1 (tak ji žák napsal). */
export interface UserEq {
  spec: EqSpec;
  conv: Conv;
}

type View = {
  momentPoint: { label: string; x: number } | null;
  side: { x: number; side: 'left' | 'right' } | null;
  arm: { from: number; to: number } | null;
};

interface Props {
  problem: Problem;
  onHighlight: (id: string | null) => void;
  onView: (v: View) => void;
  onHint: () => void;
  onDone: (eqs: UserEq[]) => void;
}

interface Row {
  spec: EqSpec;
  text: string;
  done: boolean;
  lambda: number | null;
  msgs: Msg[];
  hintLevel: number;
}

const isMoment = (e: EqSpec) => e.kind === 'm' || e.kind === 'hinge';
const pointOf = (e: EqSpec) => (isMoment(e) ? (e as { point: string; x: number }) : null);

export function eqTitle(e: EqSpec): string {
  if (e.kind === 'fx') return 'Σ[[F_x]] = 0';
  if (e.kind === 'fy') return 'Σ[[F_y]] = 0';
  if (e.kind === 'm') return `Σ[[M_${e.point}]] = 0`;
  return `[[M_${e.point}]] = 0`;
}

export function StepEquations({ problem: p, onHighlight, onView, onHint, onDone }: Props) {
  const t = useT();
  const items = useMemo(() => allItems(p), [p]);
  const defs = useMemo(() => symbolsOf(p, items), [p, items]);
  const points = namedPoints(p);
  const best = preferredMomentPoint(p);
  const inputs = useRef<(HTMLInputElement | null)[]>([]);
  // true při programovém vrácení fokusu z palety – pak se stránkou nehýbe
  const silentFocus = useRef(false);

  const [rows, setRows] = useState<Row[]>(() =>
    [
      { kind: 'fx' } as EqSpec,
      { kind: 'fy' } as EqSpec,
      { kind: 'm', point: best.label, x: best.x } as EqSpec,
      ...p.hinges.map((h) => ({ kind: 'hinge' as const, hingeId: h.id, point: h.label, x: h.x, side: preferredSide(p, h.x) })),
    ].map((spec) => ({ spec, text: '', done: false, lambda: null, msgs: [], hintLevel: 0 })),
  );
  const [active, setActive] = useState(0);
  // na dotykových zařízeních bez systémové klávesnice – píše se paletou a výkres zůstane vidět
  const [sysKbd, setSysKbd] = useState(() => !window.matchMedia?.('(pointer: coarse)').matches);
  const [slot] = useState(() => document.getElementById('palette-slot'));

  const show = (i: number, arm: View['arm'] = null, list = rows) => {
    const r = list[i];
    const pt = pointOf(r.spec);
    onView({
      momentPoint: pt ? { label: pt.point, x: pt.x } : null,
      side: r.spec.kind === 'hinge' && r.done ? { x: r.spec.x, side: r.spec.side } : null,
      arm,
    });
  };

  useEffect(() => {
    show(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const patch = (i: number, pt: Partial<Row>) => setRows((all) => all.map((r, j) => (j === i ? { ...r, ...pt } : r)));

  const focusRow = (i: number) => {
    if (i !== active) {
      setActive(i);
      show(i);
    }
  };

  /** Vložení textu z palety na místo kurzoru v aktivním poli. */
  const insert = (snippet: string) => {
    const el = inputs.current[active];
    const r = rows[active];
    const s = el?.selectionStart ?? r.text.length;
    const e = el?.selectionEnd ?? r.text.length;
    // značka vložená hned za písmeno/číslo by s ním splynula (F + R_A → FR_A) – oddělíme mezerou
    if (/^[A-Za-z]/.test(snippet) && /[A-Za-z0-9_)]$/.test(r.text.slice(0, s))) snippet = ' ' + snippet;
    const text = r.text.slice(0, s) + snippet + r.text.slice(e);
    patch(active, { text, done: false });
    requestAnimationFrame(() => {
      // vrátit kurzor do pole bez posunu stránky
      silentFocus.current = true;
      el?.focus({ preventScroll: true });
      silentFocus.current = false;
      el?.setSelectionRange(s + snippet.length, s + snippet.length);
    });
  };

  /** ⌫ z palety: smaže výběr, jinak znak před kurzorem (i s mezerami okolo). */
  const backspace = () => {
    const el = inputs.current[active];
    const r = rows[active];
    let s = el?.selectionStart ?? r.text.length;
    const e = el?.selectionEnd ?? r.text.length;
    if (s === e) {
      while (s > 0 && r.text[s - 1] === ' ') s--;
      // značku (R_Ax, F_1 …) i funkci „sin(“ mažeme celou, ostatní po znaku
      const tok = /[A-Za-z][A-Za-z0-9_]*\(?$/.exec(r.text.slice(0, s));
      s = tok ? s - tok[0].length : Math.max(0, s - 1);
      while (s > 0 && r.text[s - 1] === ' ') s--;
    }
    patch(active, { text: r.text.slice(0, s) + r.text.slice(e), done: false });
    requestAnimationFrame(() => {
      // vrátit kurzor do pole bez posunu stránky
      silentFocus.current = true;
      el?.focus({ preventScroll: true });
      silentFocus.current = false;
      el?.setSelectionRange(s, s);
    });
  };

  const specsFor = (spec: EqSpec): EqSpec[] =>
    spec.kind === 'hinge' ? (['left', 'right'] as const).map((side) => ({ ...spec, side })) : [spec];

  const errorText = (e: NonNullable<ReturnType<typeof parseEquation>['error']>, text: string): string => {
    const ex = `[[${defs.find((d) => d.kind === 'unknown')!.name}]]`;
    if (e.kind === 'unknownSym') return t('p_unknownSym', { s: e.name, ex });
    if (e.kind === 'nonlinear') return t('p_nonlinear');
    if (e.kind === 'div0') return t('p_div0');
    if (e.kind === 'twoEq') return t('p_twoEq');
    return t('p_syntax', { at: text.slice(e.at, e.at + 8) || '…' });
  };

  const run = (r: Row): { a?: Analysis; err?: Msg } => {
    const parsed = parseEquation(r.text, defs);
    if (parsed.error) return { err: { tone: 'nudge', text: errorText(parsed.error, r.text) } };
    return { a: r.spec.kind === 'hinge' ? analyzeBest(parsed, specsFor(r.spec), items, defs, p) : analyze(parsed, r.spec, items, defs, p) };
  };

  const loadOf = (it: Item) => p.loads.find((l) => l.id === it.loadId);

  const issueText = (is: Issue, a: Analysis): { text: string; arm?: boolean } => {
    const it = is.item;
    const s = it.sym;
    const spec = a.spec;
    const P = pointOf(spec)?.point ?? '';
    const pos = (a.lambda ?? 1) > 0;
    if (is.kind === 'extra') {
      const e = expected(it, spec);
      if (e.excluded && spec.kind === 'hinge') return { text: t('r_otherSide', { s, P, side: t(spec.side === 'left' ? 'rightPart' : 'leftPart') }) };
      if (spec.kind === 'fx') return { text: t(it.kind === 'm' ? 'r_coupleInF' : 'r_vertInX', { s }) };
      if (spec.kind === 'fy') return { text: t(it.kind === 'm' ? 'r_coupleInF' : 'r_horInY', { s }) };
      return { text: t(it.kind === 'fx' ? 'r_horInM' : 'r_atPoint', { s, P }) };
    }
    if (is.kind === 'missing') {
      if (spec.kind === 'fx') return { text: t('r_forgotX', { s }) };
      if (spec.kind === 'fy') return { text: t('r_forgotY', { s }) };
      if (it.kind === 'm') return { text: t('r_forgotCouple', { s }) };
      return { text: t('r_forgotM', { s, P }), arm: true };
    }
    if (is.kind === 'sign') {
      if (!isMoment(spec)) return { text: t('p_signF', { s, pos: spec.kind === 'fx' ? (pos ? '→' : '←') : pos ? '↑' : '↓' }) };
      return { text: t('p_signM', { s, P, pos: pos ? '↺' : '↻' }) };
    }
    // nesedí velikost
    if (!isMoment(spec)) {
      const l = loadOf(it);
      const derived = !!l && l.kind !== 'moment' && it.sym !== l.label;
      return { text: t(derived ? 'p_compMag' : 'p_forceMag', { s }) };
    }
    if (it.kind === 'm') return { text: t('p_coupleMag', { s }) };
    if (pointOf(spec)!.x > EPS && is.impliedArm !== undefined && close(is.impliedArm, it.x)) return { text: t('r_armFromEnd', { s, P }), arm: true };
    if (loadOf(it)?.kind === 'udl') return { text: t('r_armQ', { s, P }), arm: true };
    return { text: t('r_arm', { s, P }), arm: true };
  };

  const strayText = (n: number) => t('p_number', { n: fmt(Math.abs(n)), ex: defs.find((d) => d.kind === 'known')?.name ?? 'F_1' });

  const check = (i: number, quiet = false) => {
    const r = rows[i];
    if (!r.text.trim()) {
      if (!quiet) patch(i, { msgs: [{ tone: 'nudge', text: t('p_empty') }] });
      return;
    }
    const { a, err } = run(r);
    if (err) return patch(i, { msgs: [err], done: false });
    if (!a) return;
    if (a.ok) {
      const side = a.spec.kind === 'hinge' ? ' (' + t('p_sideFound', { side: t(a.spec.side) }) + ')' : '';
      const next = rows.map((x, j) => (j === i ? { ...x, spec: a.spec, done: true, lambda: a.lambda, msgs: [{ tone: 'ok' as const, text: t('eqDone') + side }] } : x));
      setRows(next);
      show(i, null, next);
      return;
    }
    const msgs: Msg[] = [];
    if (a.empty) msgs.push({ tone: 'nudge', text: t('p_empty') });
    for (const is of a.issues.slice(0, 2)) {
      const text = issueText(is, a).text;
      // každá zpráva musí říct, o kterou sílu jde
      msgs.push({ tone: 'nudge', text: text.includes(`[[${is.item.sym}]]`) ? text : t('e_hRow', { s: is.item.sym }) + ' ' + text });
    }
    if (a.stray.length && msgs.length < 3) msgs.push({ tone: 'nudge', text: strayText(a.stray[0]) });
    patch(i, { msgs, done: false });
  };

  const checkAll = () => rows.forEach((r, i) => !r.done && check(i, true));

  const hint = () => {
    onHint();
    const i = !rows[active].done ? active : rows.findIndex((r) => !r.done);
    if (i < 0) return;
    const r = rows[i];
    const lvl = r.hintLevel + 1;
    const spec = r.spec;
    const P = pointOf(spec)?.point ?? '';
    let msg: string;
    let text = r.text;
    let arm: View['arm'] = null;
    const perr = parseEquation(r.text, defs).error;
    const a = !perr && r.text.trim() ? run(r).a : undefined;
    const aSpec = a?.spec ?? spec;
    const first = a?.issues[0];
    if (lvl === 1) {
      const k: TKey = spec.kind === 'fx' ? 'e_h1_fx' : spec.kind === 'fy' ? 'e_h1_fy' : spec.kind === 'm' ? 'e_h1_m' : 'e_h1_hinge';
      msg = t(k, { P, best: best.label, side: spec.kind === 'hinge' ? t(preferredSide(p, spec.x)) : '' });
      if (spec.kind === 'hinge') msg += ' ' + t('p_hingeInfo', { P });
    } else if (perr) {
      msg = errorText(perr, r.text);
      // od 3. úrovně neznámou značku rovnou nahradíme nejpodobnější z palety
      if (lvl >= 3 && perr.kind === 'unknownSym') {
        const d = closestSym(perr.name, defs);
        text = text.slice(0, perr.at) + d.name + text.slice(perr.at + perr.name.length);
        msg = t('p_replaced', { from: perr.name, to: d.name });
      }
    } else if (lvl === 2 && a && (first || a.stray.length)) {
      if (first) {
        const it = issueText(first, a);
        msg = t('e_hRow', { s: first.item.sym }) + ' ' + it.text;
        if (it.arm && pointOf(aSpec)) arm = { from: pointOf(aSpec)!.x, to: first.item.x };
        onHighlight(first.item.id);
      } else msg = strayText(a.stray[0]);
    } else {
      // vložit nebo opravit jeden člen rovnice
      const target: Item | undefined =
        first?.item ??
        [...items]
          .sort((x, y) => Number(!!y.unknownId) - Number(!!x.unknownId))
          .find((it) => expected(it, aSpec).sign !== 0 && !a?.spans[it.id]);
      if (!target) msg = a?.stray.length ? strayText(a.stray[0]) : t('e_hAllOk');
      else {
        for (const sp of [...(a?.spans[target.id] ?? [])].sort((x, y) => y.s - x.s)) text = text.slice(0, sp.s) + text.slice(sp.e);
        const extra = first?.kind === 'extra';
        let term = '';
        if (!extra) {
          const eqAt = text.indexOf('=');
          const lhs = eqAt >= 0 ? text.slice(0, eqAt) : text;
          const rest = eqAt >= 0 ? text.slice(eqAt) : '';
          term = termText(target, aSpec, Math.sign(a?.lambda ?? 1) || 1, !lhs.trim());
          text = lhs.trimEnd() + term + (rest ? ' ' + rest.trim() : '');
        }
        text = text.replace(/^\s*\+\s*/, '').replace(/\s+/g, ' ').replace(/=\s*$/, '= 0').trim();
        msg = t('p_insert', { term: extra ? '− ' + target.sym : term.trim() });
        onHighlight(target.id);
        if (isMoment(aSpec) && target.kind === 'fy') arm = { from: pointOf(aSpec)!.x, to: target.x };
      }
    }
    patch(i, { hintLevel: lvl, text, done: false, msgs: [...r.msgs.filter((m) => m.tone !== 'hint'), { tone: 'hint', text: msg }] });
    setActive(i);
    show(i, arm);
  };

  const setPoint = (i: number, label: string, x: number) => {
    const next = rows.map((r, j) => (j === i ? { ...r, spec: { kind: 'm' as const, point: label, x }, done: false, msgs: [] } : r));
    setRows(next);
    setActive(i);
    show(i, null, next);
  };

  const allDone = rows.every((r) => r.done);
  const unknownDefs = defs.filter((d) => d.kind === 'unknown');
  const knownDefs = defs.filter((d) => d.kind === 'known').sort((a, b) => a.name.localeCompare(b.name, 'en', { numeric: true }));
  const noFocus = (e: { preventDefault: () => void }) => e.preventDefault(); // pole si ponechá kurzor

  const btnProps = (fn: () => void) => ({ onPointerDown: noFocus, onMouseDown: noFocus, onClick: fn });
  const palette = (
    <div className="palette">
      <div className="pal-target small">
        <span className="muted">{t('writingTo')}</span> <Rich text={eqTitle(rows[active].spec)} />
        <button className={'chip chip-op kbd-toggle' + (sysKbd ? ' on' : '')} title={t('kbdToggle')} {...btnProps(() => setSysKbd((k) => !k))}>
          ⌨
        </button>
      </div>
      <div className="pal-group">
        <span className="pal-label">{t('paletteUnknowns')}</span>
        {unknownDefs.map((d) => (
          <button key={d.name} className="chip chip-react" {...btnProps(() => insert(d.name))} onMouseEnter={() => onHighlight(d.items[0])} onMouseLeave={() => onHighlight(null)}>
            <Sym s={d.name} />
          </button>
        ))}
      </div>
      <div className="pal-group">
        <span className="pal-label">{t('paletteLoads')}</span>
        {knownDefs.map((d) => (
          <button key={d.name} className="chip chip-load" {...btnProps(() => insert(d.name))} onMouseEnter={() => onHighlight(d.items[0])} onMouseLeave={() => onHighlight(null)}>
            <Sym s={d.name} />
            <span className="chip-val">
              {fmt(d.value!)} {d.unit}
            </span>
          </button>
        ))}
      </div>
      <div className="pal-group">
        <span className="pal-label">{t('paletteOps')}</span>
        <div className="keypad">
          {[
            [' + ', '+'],
            [' - ', '−'],
            ['*', '·'],
            ['/', '/'],
            ['(', '('],
            [')', ')'],
            [' = ', '='],
            ['sin(', 'sin'],
            ['cos(', 'cos'],
          ].map(([ins, label]) => (
            <button key={label} className={'chip chip-op' + (label.length > 1 ? ' chip-fn' : '')} {...btnProps(() => insert(ins))}>
              {label}
            </button>
          ))}
          <button className="chip chip-op chip-bs" title={t('backspace')} {...btnProps(backspace)}>
            ⌫
          </button>
          <button className="chip chip-op chip-fn chip-clear" title={t('clearAll')} {...btnProps(() => patch(active, { text: '', done: false }))}>
            {t('clearShort')}
          </button>
          {['1', '2', '3', '4', '5', '6', '7', '8', '9', '0', ','].map((d) => (
            <button key={d} className="chip chip-op chip-digit" {...btnProps(() => insert(d))}>
              {d}
            </button>
          ))}
        </div>
      </div>
      <div className="pal-example small muted">
        {t('exampleLabel')}: <code>{exampleOf(defs)}</code>
      </div>
    </div>
  );

  return (
    <section className="panel">
      {slot && createPortal(palette, slot)}
      <h2>{t('s3Title')}</h2>
      <p className="lead small">{t('s3Intro')}</p>

      <div className="eq-rows">
        {rows.map((r, i) => (
          <div key={i} className={'eq-row' + (i === active ? ' eq-row-active' : '') + (r.done ? ' eq-row-done' : '')} onClick={() => focusRow(i)}>
            <div className="eq-row-head">
              <span className="eq-row-name">
                {r.done && <span className="check">✓ </span>}
                <Rich text={eqTitle(r.spec)} />
                {r.spec.kind === 'hinge' && r.done && <span className="muted small"> · {t('p_sideFound', { side: t(r.spec.side) })}</span>}
              </span>
              {r.spec.kind === 'm' && (
                <span className="mini-seg">
                  <span className="mini-label">{t('momentPoint')}</span>
                  {points.map((q) => (
                    <button key={q.label} className={(r.spec as { point: string }).point === q.label ? 'on' : ''} onClick={() => setPoint(i, q.label, q.x)}>
                      {q.label}
                    </button>
                  ))}
                </span>
              )}
            </div>
            <div className="eq-input-row">
              <input
                ref={(el) => {
                  inputs.current[i] = el;
                }}
                className={'eq-input' + (r.done ? ' in-ok' : '')}
                value={r.text}
                placeholder={t('eqPlaceholder')}
                spellCheck={false}
                autoCapitalize="off"
                autoComplete="off"
                autoCorrect="off"
                inputMode={sysKbd ? 'text' : 'none'}
                onFocus={(e) => {
                  focusRow(i);
                  if (silentFocus.current) return;
                  // řádek, do kterého se píše, nesmí zůstat schovaný pod výkresem a paletou
                  const row = e.currentTarget.closest('.eq-row');
                  setTimeout(() => row?.scrollIntoView({ block: 'nearest', behavior: 'smooth' }), 50);
                }}
                onChange={(e) => patch(i, { text: e.target.value, done: false })}
                onKeyDown={(e) => e.key === 'Enter' && check(i)}
              />
              <button className="btn btn-small" onClick={() => check(i)}>
                {t('checkEq')}
              </button>
            </div>
            <div className="eq-render">
              <EqRender text={r.text} defs={defs} />
            </div>
            {r.msgs.length > 0 && <Notes msgs={r.msgs} />}
          </div>
        ))}
      </div>

      <div className="actions">
        {!allDone && <button className="btn" onClick={checkAll}>{t('checkAll')}</button>}
        {!allDone && <HintButton level={rows[active]?.hintLevel ?? 0} onClick={hint} />}
        {allDone && (
          <button className="btn btn-primary" onClick={() => onDone(rows.map((r) => ({ spec: r.spec, conv: (r.lambda ?? 1) < 0 ? 'cw' : 'ccw' })))} autoFocus>
            {t('toSolve')} →
          </button>
        )}
      </div>
    </section>
  );
}

function lev(a: string, b: string): number {
  const d = Array.from({ length: a.length + 1 }, (_, i) => [i, ...Array(b.length).fill(0)]);
  for (let j = 1; j <= b.length; j++) d[0][j] = j;
  for (let i = 1; i <= a.length; i++)
    for (let j = 1; j <= b.length; j++) d[i][j] = Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1));
  return d[a.length][b.length];
}

/** Nejpodobnější značka z palety (pro opravu překlepu). */
function closestSym(raw: string, defs: SymDef[]): SymDef {
  const n = (s: string) => s.replace(/_/g, '').toLowerCase();
  return defs.reduce((b, d) => (lev(n(raw), n(d.name)) < lev(n(raw), n(b.name)) ? d : b));
}

function exampleOf(defs: SymDef[]): string {
  const u = defs.find((d) => d.kind === 'unknown' && !/x$/.test(d.name)) ?? defs[0];
  const k = defs.find((d) => d.kind === 'known' && !/^q/.test(d.name));
  return `${u.name.replace('_', '')}*4${k ? ` - ${k.name.replace('_', '')}*1,5` : ''} = 0`;
}

/** Živé vykreslení zápisu: značky s indexy, · místo *, neznámé značky zvýrazněné. */
function EqRender({ text, defs }: { text: string; defs: SymDef[] }) {
  if (!text.trim()) return <span className="muted eq">… = 0</span>;
  const { toks, bad } = tokenize(text);
  const hasEq = toks.some((x) => x.t === 'op' && x.v === '=');
  const n = (s: string) => s.replace(/_/g, '');
  const find = (raw: string) => defs.find((d) => n(d.name) === n(raw)) ?? defs.filter((d) => n(d.name).toLowerCase() === n(raw).toLowerCase())[0];
  return (
    <span className="eq">
      {toks.map((tk, i) => {
        if (tk.t === 'num') return <span key={i}>{fmt(tk.v, 4)}</span>;
        if (tk.t === 'id') {
          if (/^(sin|cos|tan|tg|sqrt)$/i.test(tk.v)) return <span key={i} className="fn">{tk.v.toLowerCase()}</span>;
          const d = find(tk.v);
          return d ? <Sym key={i} s={d.name} /> : <span key={i} className="unknown-sym">{tk.v}</span>;
        }
        const v = tk.v === '*' ? '·' : tk.v === '-' ? '−' : tk.v;
        return <span key={i}>{v === '+' || v === '−' || v === '=' ? ` ${v} ` : v}</span>;
      })}
      {bad !== undefined && <span className="unknown-sym">{text.slice(bad)}</span>}
      {!hasEq && ' = 0'}
    </span>
  );
}

export function EqName({ spec }: { spec: EqSpec }) {
  if (spec.kind === 'hinge')
    return (
      <span>
        <Sym s={`M_${spec.point}`} />
        <sup className="side-sup">{spec.side === 'left' ? '←' : '→'}</sup> = 0
      </span>
    );
  return <Rich text={eqTitle(spec)} />;
}
