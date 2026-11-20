import type { LoadsStep } from '../trainer/loads';
import { useT } from '../i18n';
import { Actions, DoneLog, HintButton, NumInput, Notes, StepPanel } from '../components/ui';
import { Rich } from '../components/Sym';
import { fmt } from '../format';
import { useLearn } from '../learn/nav';

export function StepLoads({ st }: { st: LoadsStep }) {
  const t = useT();
  const learn = useLearn();
  const task = st.task;

  if (!task) {
    return (
      <StepPanel title={t('s2Title')}>
        {st.log.length > 0 ? <DoneLog log={st.log} /> : <p className="lead">{t('s2Nothing')}</p>}
        {st.moments.map((m) => (
          <p key={m.id} className="small muted">
            <Rich text={t('s2Moment', { m: m.label })} />
          </p>
        ))}
        <Actions>
          <button className="btn btn-primary" onClick={() => st.done()} autoFocus>
            {t('next')} →
          </button>
        </Actions>
      </StepPanel>
    );
  }

  const Q = st.resultant;
  const taskText = task.kind === 'udl' ? t('udlTask', { q: fmt(task.q), l: fmt(task.x2 - task.x1), Q }) : t('forceTask', { F: task.label, v: fmt(task.F), a: task.alpha ?? 0 });
  const [la, lb] = task.kind === 'udl' ? [t('udlQ', { Q }), t('udlPos', { Q })] : [t('fx', { F: task.label }), t('fy', { F: task.label })];

  return (
    <StepPanel title={t('s2Title')} intro={t('s2Intro')} learn={task.kind === 'udl' ? [8, 1] : [3, 1]}>
      <DoneLog log={st.log} />
      <div className="task">
        <p>
          <Rich text={taskText} />
        </p>
        {/* šikmá síla = sin a cos: trvalý odkaz do kurzu trigonometrie (po zavření se vrátí sem) */}
        {task.kind === 'force' && task.alpha && (
          <button type="button" className="learn-link" onClick={() => learn.open({ kind: 'open', lesson: 102, card: 0, fromTrainer: true })}>
            📖 {t('trigLink')}
          </button>
        )}
        <div className="row gap wrap">
          <NumInput label={<Rich text={la} />} value={st.a} onChange={(v) => st.setA(v)} state={st.state[0]} onEnter={() => st.check()} />
          <NumInput label={<Rich text={lb} />} value={st.b} onChange={(v) => st.setB(v)} state={st.state[1]} onEnter={() => st.check()} />
        </div>
        <Notes msgs={st.msgs} />
        <Actions reveal={`${st.solved}-${st.idx}`}>
          {!st.solved && st.hintLevel < 3 && <HintButton level={st.hintLevel} onClick={() => st.hint()} />}
          {!st.solved && (
            <button className="btn btn-primary" onClick={() => st.check()}>
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
