import type { SolveStep, Term } from '../trainer/solve';
import { paren, unitOf } from '../trainer/solve';
import { useT } from '../i18n';
import { Actions, HintButton, NumInput, Notes, StepPanel } from '../components/ui';
import { Sym } from '../components/Sym';
import { fmt } from '../format';
import { EqName } from './EqName';

export function StepSolve({ st, onHighlight }: { st: SolveStep; onHighlight: (id: string | null) => void }) {
  const t = useT();
  const solvedSet = st.solvedSet;
  const solved = Object.fromEntries([...solvedSet].map((k) => [k, st.answer[k]]));
  const allOk = st.allOk;
  return (
    <StepPanel title={t('s4Title')} intro={t('s4Intro')} learn={[7, 4]}>
      <div className="eq-list">
        {st.eqs.map((e, i) => (
          <div key={i} className="eq-line">
            <span className="eq-name">
              <EqName spec={e.spec} />
            </span>
            <NumericEq terms={st.eqTerms[i]} solved={solved} />
          </div>
        ))}
      </div>

      <div className="task">
        <div className="answers">
          {st.unknowns.map((u) => (
            <div key={u.id} className="answer" onMouseEnter={() => onHighlight('r_' + u.id)} onMouseLeave={() => onHighlight(null)}>
              <NumInput
                label={
                  <>
                    <Sym s={u.sym} /> =
                  </>
                }
                value={st.vals[u.id] ?? ''}
                onChange={(v) => st.setVal(u.id, v)}
                state={st.status[u.id] ?? null}
                onEnter={() => st.check()}
                suffix={unitOf(u)}
                signed
              />
            </div>
          ))}
        </div>

        <Notes msgs={st.msgs} />

        <Actions reveal={allOk}>
          {!allOk && <HintButton level={st.hinted ? 1 : 0} onClick={() => st.hint()} />}
          {!allOk && (
            <button className="btn btn-primary" onClick={() => st.check()}>
              {t('check')}
            </button>
          )}
          {allOk && (
            <button className="btn btn-primary" onClick={() => st.done()} autoFocus>
              {t('next')} →
            </button>
          )}
        </Actions>
      </div>
    </StepPanel>
  );
}

/** Rovnice s čísly; už spočítané neznámé se zobrazí dosazené (zeleně). */
export function NumericEq({ terms, solved }: { terms: Term[]; solved: Record<string, number> }) {
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
              x.it.unknownId in solved ? (
                <span className="subst" title={x.it.sym}>
                  {paren(solved[x.it.unknownId])}
                </span>
              ) : (
                <span className="react-sym">
                  <Sym s={x.it.sym} />
                </span>
              )
            ) : (
              fmt(x.it.value!)
            )}
            {arm !== null && <>·{fmt(arm)}</>}
          </span>
        );
      })}{' '}
      = 0
    </span>
  );
}
