import { useMemo, useState } from 'react';
import type { Problem } from '../model/types';
import { allItems, expected, solve, unknownsOf, type Item, type Unknown } from '../model/mechanics';
import { useT } from '../i18n';
import { HintButton, NumInput, Notes, type Msg } from '../components/ui';
import { Sym } from '../components/Sym';
import { close, fmt, parseNum } from '../format';
import { EqName, eqTitle, type UserEq } from './StepEquations';

interface Props {
  problem: Problem;
  eqs: UserEq[];
  onHighlight: (id: string | null) => void;
  onHint: () => void;
  onDone: (results: Record<string, number>) => void;
}

interface Term {
  it: Item;
  /** koeficient (znaménko × rameno) podle konvence uživatele */
  c: number;
  arm: number | null;
}

function termsOf(eq: UserEq, items: Item[]): Term[] {
  // rovnici ukazujeme se stejným znaménkem, jak ji žák napsal
  const k = eq.conv === 'cw' ? -1 : 1;
  return items
    .map((it) => {
      const e = expected(it, eq.spec);
      return { it, c: e.sign * k * (e.arm ?? 1), arm: e.arm };
    })
    .filter((x) => x.c !== 0);
}

const unitOf = (u: Unknown) => (u.kind === 'm' ? 'kNm' : 'kN');
const paren = (v: number) => (v < 0 ? `(${fmt(v)})` : fmt(v));

export function StepSolve({ problem: p, eqs, onHighlight, onHint, onDone }: Props) {
  const t = useT();
  const items = useMemo(() => allItems(p), [p]);
  const unknowns = useMemo(() => unknownsOf(p), [p]);
  const answer = useMemo(() => solve(eqs.map((e) => e.spec), items, unknowns)!, [eqs, items, unknowns]);
  const eqTerms = useMemo(() => eqs.map((e) => termsOf(e, items)), [eqs, items]);

  const [vals, setVals] = useState<Record<string, string>>({});
  const [status, setStatus] = useState<Record<string, 'ok' | 'nudge'>>({});
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [hint, setHint] = useState<{ target: string; level: number } | null>(null);

  const solvedSet = new Set(Object.keys(status).filter((k) => status[k] === 'ok'));
  const allOk = unknowns.every((u) => solvedSet.has(u.id));

  /** Nejvhodnější rovnice pro neznámou u (nejméně dalších nevyřešených neznámých). */
  const bestEq = (uid: string, solved: Set<string>) => {
    let best = -1;
    let bestScore = Infinity;
    eqTerms.forEach((terms, i) => {
      if (!terms.some((x) => x.it.unknownId === uid)) return;
      const others = terms.filter((x) => x.it.unknownId && x.it.unknownId !== uid && !solved.has(x.it.unknownId)).length;
      const score = others * 100 + terms.filter((x) => x.it.unknownId).length;
      if (score < bestScore) {
        bestScore = score;
        best = i;
      }
    });
    return { i: best, others: Math.floor(bestScore / 100) };
  };

  /** Další neznámá, kterou lze spočítat hned. */
  const nextTarget = (): Unknown | undefined => {
    const open = unknowns.filter((u) => !solvedSet.has(u.id));
    return open.find((u) => bestEq(u.id, solvedSet).others === 0) ?? open[0];
  };

  const eqLabel = (i: number) => eqTitle(eqs[i].spec);

  const check = () => {
    const st: Record<string, 'ok' | 'nudge'> = { ...status };
    const out: Msg[] = [];
    const nowSolved = new Set(solvedSet);
    for (const u of unknowns) {
      if (st[u.id] === 'ok') continue;
      const v = parseNum(vals[u.id] ?? '');
      if (v === null) {
        delete st[u.id];
        continue;
      }
      if (close(v, answer[u.id])) {
        st[u.id] = 'ok';
        nowSolved.add(u.id);
      } else st[u.id] = 'nudge';
    }
    for (const u of unknowns) {
      if (st[u.id] !== 'nudge' || out.length >= 2) continue;
      const v = parseNum(vals[u.id] ?? '')!;
      const b = bestEq(u.id, nowSolved);
      if (close(-v, answer[u.id])) out.push({ tone: 'nudge', text: t('v_sign') });
      else if (b.others > 0) {
        const other = eqTerms[b.i].find((x) => x.it.unknownId && x.it.unknownId !== u.id && !nowSolved.has(x.it.unknownId))!;
        out.push({ tone: 'nudge', text: t('v_dep', { u: u.sym, eq: eqLabel(b.i), o: other.it.sym }) });
      } else out.push({ tone: 'nudge', text: t('v_gen', { u: u.sym, eq: eqLabel(b.i) }) });
    }
    if (unknowns.every((u) => st[u.id] === 'ok'))
      for (const u of unknowns) if (answer[u.id] < 0) out.push({ tone: 'info', text: t('v_neg', { u: u.sym }) });
    setStatus(st);
    setMsgs(out);
  };

  const expression = (uid: string, i: number): string | null => {
    const terms = eqTerms[i];
    const self = terms.find((x) => x.it.unknownId === uid)!;
    const f = self.c > 0 ? -1 : 1;
    const parts: string[] = [];
    for (const x of terms) {
      if (x === self) continue;
      let text: string;
      const c = x.c * f;
      if (x.it.unknownId) {
        if (!solvedSet.has(x.it.unknownId)) return null;
        const v = answer[x.it.unknownId];
        text = x.arm !== null ? `${fmt(Math.abs(x.c))}·${paren(v)}` : paren(v);
      } else {
        const v = x.it.value!;
        text = x.arm !== null ? `${fmt(v)}·${fmt(Math.abs(x.c))}` : fmt(v);
      }
      parts.push((c < 0 ? (parts.length ? ' − ' : '−') : parts.length ? ' + ' : '') + text);
    }
    const D = Math.abs(self.c);
    const num = parts.length ? parts.join('') : '0';
    if (Math.abs(D - 1) < 1e-9) return num;
    return (parts.length > 1 ? `(${num})` : num) + ` / ${fmt(D)}`;
  };

  const giveHint = () => {
    onHint();
    const target = nextTarget();
    if (!target) return;
    const level = hint && hint.target === target.id ? hint.level + 1 : 1;
    setHint({ target: target.id, level });
    const b = bestEq(target.id, solvedSet);
    let text: string;
    const coupled = unknowns.filter((u) => !solvedSet.has(u.id)).every((u) => bestEq(u.id, solvedSet).others > 0);
    if (coupled && level === 1) {
      const list = unknowns.filter((u) => !solvedSet.has(u.id) && u.kind !== 'fx').map((u) => `[[${u.sym}]]`).join(', ');
      text = t('v_system', { list });
    } else if (b.others > 0 && !coupled) text = t('v_coupled', { u: target.sym });
    else if (coupled) {
      text = t('v_h3', { u: target.sym, val: fmt(answer[target.id]), unit: unitOf(target) });
      setVals((v) => ({ ...v, [target.id]: fmt(answer[target.id]) }));
    } else if (level === 1) text = t('v_h1', { u: target.sym, eq: eqLabel(b.i) });
    else if (level === 2) text = t('v_h2', { u: target.sym, expr: expression(target.id, b.i) ?? '' });
    else {
      text = t('v_h3', { u: target.sym, val: fmt(answer[target.id]), unit: unitOf(target) });
      setVals((v) => ({ ...v, [target.id]: fmt(answer[target.id]) }));
    }
    onHighlight('r_' + target.id);
    setMsgs((m) => [...m.filter((x) => x.tone !== 'hint'), { tone: 'hint', text }]);
  };

  return (
    <section className="panel">
      <h2>{t('s4Title')}</h2>
      <p className="lead">{t('s4Intro')}</p>

      <div className="eq-list">
        {eqs.map((e, i) => (
          <div key={i} className="eq-line">
            <span className="eq-name"><EqName spec={e.spec} /></span>
            <NumericEq terms={eqTerms[i]} solved={Object.fromEntries([...solvedSet].map((k) => [k, answer[k]]))} />
          </div>
        ))}
      </div>

      <div className="task">
        <div className="answers">
          {unknowns.map((u) => (
            <div key={u.id} className="answer" onMouseEnter={() => onHighlight('r_' + u.id)} onMouseLeave={() => onHighlight(null)}>
              <NumInput
                label={<><Sym s={u.sym} /> =</>}
                value={vals[u.id] ?? ''}
                onChange={(v) => {
                  setVals((x) => ({ ...x, [u.id]: v }));
                  if (status[u.id] === 'nudge') setStatus((s) => {
                    const n = { ...s };
                    delete n[u.id];
                    return n;
                  });
                }}
                state={status[u.id] ?? null}
                onEnter={check}
              />
              <span className="unit">{unitOf(u)}</span>
              {status[u.id] === 'ok' && <span className="check">✓</span>}
            </div>
          ))}
        </div>

        <Notes msgs={msgs} />

        <div className="actions">
          {!allOk && <button className="btn" onClick={check}>{t('check')}</button>}
          {!allOk && <HintButton level={hint ? 1 : 0} onClick={giveHint} />}
          {allOk && (
            <button className="btn btn-primary" onClick={() => onDone(answer)} autoFocus>
              {t('next')} →
            </button>
          )}
        </div>
      </div>
    </section>
  );
}

/** Rovnice s čísly; už spočítané neznámé se zobrazí dosazené (zeleně). */
function NumericEq({ terms, solved }: { terms: Term[]; solved: Record<string, number> }) {
  return (
    <span className="eq">
      {terms.map((x, i) => {
        const neg = x.c < 0;
        const sign = neg ? (i ? ' − ' : '−') : i ? ' + ' : '';
        const arm = x.arm !== null ? Math.abs(x.c) : null;
        return (
          <span key={x.it.id}>
            {sign}
            {x.it.unknownId ? (
              <>
                {x.it.unknownId in solved ? (
                  <span className="subst" title={x.it.sym}>{paren(solved[x.it.unknownId])}</span>
                ) : (
                  <Sym s={x.it.sym} />
                )}
                {arm !== null && <>·{fmt(arm)}</>}
              </>
            ) : (
              <>
                {fmt(x.it.value!)}
                {arm !== null && <>·{fmt(arm)}</>}
              </>
            )}
          </span>
        );
      })}{' '}
      = 0
    </span>
  );
}
