import type { ReleaseStep } from '../trainer/release';
import { REACTION_SETS, HINGE_ANSWERS, setLabel } from '../trainer/release';
import { reactionSetOf, type ReactionSet } from '../model/mechanics';
import { useT, type TKey } from '../i18n';
import { Actions, DoneLog, HintButton, NumInput, Notes, StepPanel } from '../components/ui';
import { Rich } from '../components/Sym';

export function StepRelease({ st }: { st: ReleaseStep }) {
  const t = useT();
  const task = st.task;
  const learn: [number, number] = task.kind === 'support' ? [6, 0] : task.kind === 'hinge' ? [9, 0] : [9, 2];
  return (
    <StepPanel title={t('s1Title')} intro={t('s1Intro')} learn={learn}>
      <p className="small muted">
        <Rich text={t('s1Convention')} />
      </p>
      <DoneLog log={st.log} />
      <div className="task">
        {task.kind === 'support' && (
          <>
            <h3>{t('supportN', { p: task.label })}</h3>
            <div className="choices">
              {REACTION_SETS.map((set) => {
                const correct = reactionSetOf({ id: '', label: '', x: 0, type: task.type });
                return (
                  <button
                    key={set}
                    className={'choice' + (st.picked === set ? (set === correct ? ' choice-ok' : ' choice-tried') : '')}
                    onClick={() => st.pickSet(set)}
                    disabled={st.solved && st.picked !== set}
                  >
                    <SetIcon set={set} />
                    <span className="choice-title">
                      <Rich text={setLabel(set, task.label)} />
                    </span>
                    <span className="choice-sub">{t(`set_${set}` as TKey)}</span>
                  </button>
                );
              })}
            </div>
          </>
        )}
        {task.kind === 'hinge' && (
          <>
            <h3>{t('hingeN', { p: task.label })}</h3>
            <p>{t('hingeQ')}</p>
            <div className="choices choices-text">
              {HINGE_ANSWERS.map((a) => (
                <button
                  key={a}
                  className={'choice choice-text' + (st.picked === a ? (a === 'forces' ? ' choice-ok' : ' choice-tried') : '')}
                  onClick={() => st.pickHinge(a)}
                  disabled={st.solved && st.picked !== a}
                >
                  <span className="choice-title">{t(`hinge_${a}` as TKey)}</span>
                </button>
              ))}
            </div>
          </>
        )}
        {task.kind === 'count' && (
          <>
            <h3>{t('countTitle')}</h3>
            <div className="row gap wrap">
              <NumInput
                label={
                  <>
                    {t('countUnknowns')} <i>n</i> =
                  </>
                }
                value={st.nU}
                onChange={(v) => st.setNU(v)}
                width={64}
                onEnter={() => st.checkCount()}
                state={st.solved ? 'ok' : null}
              />
              <NumInput
                label={
                  <>
                    {t('countEqs')} <i>r</i> =
                  </>
                }
                value={st.nE}
                onChange={(v) => st.setNE(v)}
                width={64}
                onEnter={() => st.checkCount()}
                state={st.solved ? 'ok' : null}
              />
            </div>
          </>
        )}

        <Notes msgs={st.msgs} />

        <Actions reveal={`${st.solved}-${st.idx}`}>
          {!st.solved && st.hintLevel < st.maxHint && <HintButton level={st.hintLevel} onClick={() => st.hint()} />}
          {!st.solved && task.kind === 'count' && (
            <button className="btn btn-primary" onClick={() => st.checkCount()}>
              {t('check')}
            </button>
          )}
          {st.solved && (
            <button className="btn btn-primary" onClick={() => st.advance()} autoFocus>
              {t('next')} →
            </button>
          )}
        </Actions>
      </div>
    </StepPanel>
  );
}

/** Malý piktogram sady reakcí. */
function SetIcon({ set }: { set: ReactionSet }) {
  const c = 'var(--react)';
  const hasX = set !== 'y';
  const hasY = set !== 'x';
  const hasM = set === 'xym';
  return (
    <svg viewBox="0 0 80 56" className="set-icon" aria-hidden>
      <line x1={14} y1={22} x2={74} y2={22} stroke="var(--ink)" strokeWidth={5} strokeLinecap="round" opacity={0.18} />
      <circle cx={44} cy={22} r={3.5} fill="var(--ink)" />
      {hasX && (
        <g stroke={c} fill={c} strokeWidth={2.2}>
          <line x1={10} y1={22} x2={33} y2={22} />
          <path d="M40,22 L31,17.5 L31,26.5 Z" stroke="none" />
        </g>
      )}
      {hasY && (
        <g stroke={c} fill={c} strokeWidth={2.2}>
          <line x1={44} y1={52} x2={44} y2={33} />
          <path d="M44,26 L39.5,35 L48.5,35 Z" stroke="none" />
        </g>
      )}
      {hasM && (
        <g stroke={c} fill="none" strokeWidth={2}>
          <path d="M56,26 A13,13 0 1 0 31,14" />
          <path d="M28.5,19.5 L29,10 L36,14.5 Z" fill={c} stroke="none" />
        </g>
      )}
    </svg>
  );
}
