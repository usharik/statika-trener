import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import type { EquationsStep } from '../trainer/equations';
import { eqTitle } from '../trainer/equations';
import { lookupSym, isFunctionName, tokenize, type SymDef } from '../model/eqparse';
import { useT } from '../i18n';
import { Actions, HintButton, Notes, StepPanel } from '../components/ui';
import { Rich, Sym } from '../components/Sym';
import { fmt } from '../format';

interface Props {
  st: EquationsStep;
  onHighlight: (id: string | null) => void;
}

export function StepEquations({ st, onHighlight }: Props) {
  const t = useT();
  const inputs = useRef<(HTMLInputElement | null)[]>([]);
  // true při programovém vrácení fokusu z palety – pak se stránkou nehýbe
  const silentFocus = useRef(false);
  // na dotykových zařízeních bez systémové klávesnice – píše se paletou a výkres zůstane vidět
  const [sysKbd, setSysKbd] = useState(() => !window.matchMedia?.('(pointer: coarse)').matches);
  const [slot] = useState(() => document.getElementById('palette-slot'));
  const revs = useRef<number[]>([]);
  const fromPalette = useRef(false);

  // text změněný programem (paleta, nápověda) → kurzor na místo vložení
  useEffect(() => {
    st.rows.forEach((r, i) => {
      if (revs.current[i] === r.rev) return;
      const first = revs.current[i] === undefined;
      revs.current[i] = r.rev;
      if (first) return;
      const el = inputs.current[i];
      if (!el) return;
      if (fromPalette.current && i === st.active) {
        silentFocus.current = true;
        el.focus({ preventScroll: true });
        silentFocus.current = false;
      }
      if (document.activeElement === el) el.setSelectionRange(r.sel[0], r.sel[1]);
    });
    fromPalette.current = false;
  });

  const active = st.rows[st.active];
  const learn: [number, number] = !active ? [4, 0] : active.spec.kind === 'fx' || active.spec.kind === 'fy' ? [2, 3] : active.spec.kind === 'm' ? [5, 3] : [9, 1];

  const noFocus = (e: { preventDefault: () => void }) => e.preventDefault(); // pole si ponechá kurzor
  const btnProps = (fn: () => void) => ({
    onPointerDown: noFocus,
    onMouseDown: noFocus,
    onClick: () => {
      fromPalette.current = true;
      fn();
    },
  });

  const palette = !st.allDone && (
    <div className="palette">
      <div className="pal-target small">
        <span className="muted">{t('writingTo')}</span> <Rich text={eqTitle(active.spec)} />
        <button className="chip chip-op chip-fn pal-check" {...btnProps(() => st.check(st.active))}>
          {t('checkEq')}
        </button>
        <button className={'chip chip-op kbd-toggle' + (sysKbd ? ' on' : '')} title={t('kbdToggle')} aria-pressed={sysKbd} {...btnProps(() => setSysKbd((k) => !k))}>
          ⌨
        </button>
      </div>
      <div className="pal-group">
        <span className="pal-label">{t('paletteUnknowns')}</span>
        {st.unknownDefs.map((d) => (
          <button key={d.name} className="chip chip-react" {...btnProps(() => st.insert(d.name))} onMouseEnter={() => onHighlight(d.items[0])} onMouseLeave={() => onHighlight(null)}>
            <Sym s={d.name} />
          </button>
        ))}
      </div>
      <div className="pal-group">
        <span className="pal-label">{t('paletteLoads')}</span>
        {st.knownDefs.map((d) => (
          <button key={d.name} className="chip chip-load" {...btnProps(() => st.insert(d.name))} onMouseEnter={() => onHighlight(d.items[0])} onMouseLeave={() => onHighlight(null)}>
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
            <button key={label} className={'chip chip-op' + (label.length > 1 ? ' chip-fn' : '')} {...btnProps(() => st.insert(ins))}>
              {label}
            </button>
          ))}
          <button className="chip chip-op chip-bs" title={t('backspace')} aria-label={t('backspace')} {...btnProps(() => st.backspace())}>
            ⌫
          </button>
          <button className="chip chip-op chip-fn chip-clear" title={t('clearAll')} {...btnProps(() => st.clear())}>
            {t('clearShort')}
          </button>
          {['1', '2', '3', '4', '5', '6', '7', '8', '9', '0', ','].map((d) => (
            <button key={d} className="chip chip-op chip-digit" {...btnProps(() => st.insert(d))}>
              {d}
            </button>
          ))}
        </div>
      </div>
      <div className="pal-example small muted">
        {t('exampleLabel')}: <code>{st.example}</code>
      </div>
    </div>
  );

  const selOf = (el: HTMLInputElement): [number, number] => [el.selectionStart ?? el.value.length, el.selectionEnd ?? el.value.length];

  return (
    <StepPanel title={t('s3Title')} intro={t('s3Intro')} learn={learn}>
      {slot && palette && createPortal(palette, slot)}
      <div className="eq-rows">
        {st.rows.map((r, i) => (
          <div key={i} className={'eq-row' + (i === st.active ? ' eq-row-active' : '') + (r.done ? ' eq-row-done' : '')} onClick={() => st.focusRow(i)}>
            <div className="eq-row-head">
              <span className="eq-row-name">
                {r.done && <span className="check">✓ </span>}
                <Rich text={eqTitle(r.spec)} />
                {r.spec.kind === 'hinge' && r.done && <span className="muted small"> · {t('p_sideFound', { side: t(r.spec.side) })}</span>}
              </span>
              {r.spec.kind === 'm' && (
                <span className="mini-seg">
                  <span className="mini-label">{t('momentPoint')}</span>
                  {st.points.map((q) => (
                    <button key={q.label} className={r.spec.kind === 'm' && r.spec.point === q.label ? 'on' : ''} onClick={() => st.setPoint(i, q.label, q.x)}>
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
                aria-label={eqTitle(r.spec).replace(/\[\[|\]\]/g, '')}
                onFocus={(e) => {
                  st.focusRow(i);
                  if (silentFocus.current) return;
                  // řádek, do kterého se píše, nesmí zůstat schovaný pod výkresem a paletou
                  const row = e.currentTarget.closest('.eq-row');
                  setTimeout(() => row?.scrollIntoView({ block: 'nearest', behavior: 'smooth' }), 50);
                }}
                onChange={(e) => st.onValueChange(i, e.target.value, selOf(e.target))}
                onSelect={(e) => st.onSelect(i, selOf(e.currentTarget))}
                onKeyDown={(e) => e.key === 'Enter' && st.check(i)}
              />
              <button className="btn btn-small" onClick={() => st.check(i)}>
                {t('checkEq')}
              </button>
            </div>
            <div className="eq-render">
              <EqRender text={r.text} defs={st.defs} />
            </div>
            {r.msgs.length > 0 && <Notes msgs={r.msgs} />}
          </div>
        ))}
      </div>

      <Actions reveal={st.allDone}>
        {!st.allDone && (
          <button className="btn" onClick={() => st.checkAll()}>
            {t('checkAll')}
          </button>
        )}
        {!st.allDone && <HintButton level={active?.hintLevel ?? 0} onClick={() => st.hint()} />}
        {st.allDone && (
          <button className="btn btn-primary" onClick={() => st.done()} autoFocus>
            {t('toSolve')} →
          </button>
        )}
      </Actions>
    </StepPanel>
  );
}

/** Živé vykreslení zápisu: značky s indexy, · místo *, neznámé značky zvýrazněné. */
function EqRender({ text, defs }: { text: string; defs: SymDef[] }) {
  if (!text.trim()) return <span className="muted eq">… = 0</span>;
  const { toks, bad } = tokenize(text);
  const hasEq = toks.some((x) => x.t === 'op' && x.v === '=');
  return (
    <span className="eq">
      {toks.map((tk, i) => {
        if (tk.t === 'num') return <span key={i}>{fmt(tk.v, 4)}</span>;
        if (tk.t === 'id') {
          if (isFunctionName(tk.v))
            return (
              <span key={i} className="fn">
                {tk.v.toLowerCase()}
              </span>
            );
          const d = lookupSym(defs, tk.v);
          return d ? (
            <span key={i} className={d.kind === 'unknown' ? 'react-sym' : 'load-sym'}>
              <Sym s={d.name} />
            </span>
          ) : (
            <span key={i} className="unknown-sym">
              {tk.v}
            </span>
          );
        }
        const v = tk.v === '*' ? '·' : tk.v === '-' ? '−' : tk.v;
        return <span key={i}>{v === '+' || v === '−' || v === '=' ? ` ${v} ` : v}</span>;
      })}
      {bad !== undefined && <span className="unknown-sym">{text.slice(bad)}</span>}
      {!hasEq && ' = 0'}
    </span>
  );
}
