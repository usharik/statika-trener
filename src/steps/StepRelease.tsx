import { useState } from 'react';
import type { Problem, SupportType } from '../model/types';
import { reactionSetOf, unknownsOf, type ReactionSet } from '../model/mechanics';
import { useT, type TKey } from '../i18n';
import { HintButton, NumInput, Notes, type Msg } from '../components/ui';
import { Rich } from '../components/Sym';
import { parseNum } from '../format';

interface Props {
  problem: Problem;
  released: Set<string>;
  onRelease: (id: string) => void;
  onActive: (id: string | null) => void;
  onHint: () => void;
  onDone: () => void;
}

type Task = { kind: 'support'; id: string; label: string; type: SupportType } | { kind: 'hinge'; id: string; label: string } | { kind: 'count' };

const SETS: ReactionSet[] = ['xy', 'y', 'x', 'xym'];
type HingeAns = 'forces' | 'all' | 'none';

export function StepRelease({ problem: p, onRelease, onActive, onHint, onDone }: Props) {
  const t = useT();
  const tasks: Task[] = [
    ...p.supports.map((s) => ({ kind: 'support' as const, id: s.id, label: s.label, type: s.type })),
    ...p.hinges.map((h) => ({ kind: 'hinge' as const, id: h.id, label: h.label })),
    { kind: 'count' as const },
  ];
  const [idx, setIdx] = useState(0);
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [hintLevel, setHintLevel] = useState(0);
  const [solved, setSolved] = useState(false);
  const [picked, setPicked] = useState<string | null>(null);
  const [nU, setNU] = useState('');
  const [nE, setNE] = useState('');
  const [log, setLog] = useState<Msg[]>([]);

  const task = tasks[idx];
  const nUnknowns = unknownsOf(p).length;
  const nEqs = 3 + p.hinges.length;

  const advance = () => {
    const next = idx + 1;
    if (next >= tasks.length) return onDone();
    const ok = msgs.find((m) => m.tone === 'ok');
    if (ok) setLog((l) => [...l, ok]);
    setIdx(next);
    setMsgs([]);
    setHintLevel(0);
    setSolved(false);
    setPicked(null);
    const nt = tasks[next];
    onActive(nt.kind === 'support' ? nt.id : null);
  };

  const pickSet = (set: ReactionSet) => {
    if (task.kind !== 'support' || solved) return;
    setPicked(set);
    const correct = reactionSetOf({ id: '', label: '', x: 0, type: task.type });
    if (set === correct) {
      setSolved(true);
      onRelease(task.id);
      setMsgs([{ tone: 'ok', text: t(`s1_ok_${task.type}` as TKey) }]);
    } else {
      setMsgs([{ tone: 'nudge', text: t(`s1_${task.type}_${set}` as TKey, { p: task.label }) }]);
    }
  };

  const pickHinge = (a: HingeAns) => {
    if (task.kind !== 'hinge' || solved) return;
    setPicked(a);
    if (a === 'forces') {
      setSolved(true);
      setMsgs([{ tone: 'ok', text: t('s1_hinge_ok', { p: task.label }) }]);
    } else setMsgs([{ tone: 'nudge', text: t(a === 'all' ? 's1_hinge_all' : 's1_hinge_none') }]);
  };

  const checkCount = () => {
    const u = parseNum(nU);
    const e = parseNum(nE);
    const out: Msg[] = [];
    if (u !== nUnknowns) out.push({ tone: 'nudge', text: t('count_u') });
    if (e !== nEqs) out.push({ tone: 'nudge', text: t(p.hinges.length && e === 3 ? 'count_eh' : 'count_e3') });
    if (!out.length) {
      setSolved(true);
      out.push({ tone: 'ok', text: t('count_ok', { n: nUnknowns }) });
    }
    setMsgs(out);
  };

  const hint = () => {
    onHint();
    const lvl = hintLevel + 1;
    setHintLevel(lvl);
    let text = '';
    if (task.kind === 'support') {
      const setName = t(`set_${reactionSetOf({ id: '', label: '', x: 0, type: task.type })}` as TKey);
      text = lvl === 1 ? t('s1_h1', { p: task.label }) : lvl === 2 ? t(`s1_h2_${task.type}` as TKey) : t('s1_h3', { p: task.label, set: setName });
    } else if (task.kind === 'hinge') {
      text = lvl === 1 ? t('s1_hinge_h1') : t('s1_h3', { p: task.label, set: t('hinge_forces') });
    } else {
      text = lvl === 1 ? t('count_h1') : t('count_h2', { n: nUnknowns, r: nEqs });
    }
    setMsgs((m) => [...m.filter((x) => x.tone !== 'hint'), { tone: 'hint', text }]);
  };
  const maxHint = task.kind === 'support' ? 3 : 2;

  return (
    <section className="panel">
      <h2>{t('s1Title')}</h2>
      <p className="lead">{t('s1Intro')}</p>
      <p className="small muted"><Rich text={t('s1Convention')} /></p>

      {log.length > 0 && (
        <ul className="log">
          {log.map((m, i) => (
            <li key={i}><span className="check">✓</span> <Rich text={m.text} /></li>
          ))}
        </ul>
      )}

      <div className="task">
        {task.kind === 'support' && (
          <>
            <h3>{t('supportN', { p: task.label })}</h3>
            <div className="choices">
              {SETS.map((set) => (
                <button key={set} className={'choice' + (picked === set ? (solved ? ' choice-ok' : ' choice-tried') : '')} onClick={() => pickSet(set)} disabled={solved && picked !== set}>
                  <SetIcon set={set} />
                  <span className="choice-title"><Rich text={setLabel(set, task.label)} /></span>
                  <span className="choice-sub">{t(`set_${set}` as TKey)}</span>
                </button>
              ))}
            </div>
          </>
        )}
        {task.kind === 'hinge' && (
          <>
            <h3>{t('hingeN', { p: task.label })}</h3>
            <p>{t('hingeQ')}</p>
            <div className="choices">
              {(['forces', 'all', 'none'] as HingeAns[]).map((a) => (
                <button key={a} className={'choice choice-text' + (picked === a ? (solved ? ' choice-ok' : ' choice-tried') : '')} onClick={() => pickHinge(a)} disabled={solved && picked !== a}>
                  <span className="choice-title">{t(`hinge_${a}` as TKey)}</span>
                </button>
              ))}
            </div>
          </>
        )}
        {task.kind === 'count' && (
          <>
            <h3>{t('countTitle')}</h3>
            <div className="row gap">
              <NumInput label={<>{t('countUnknowns')} <i>n</i> =</>} value={nU} onChange={setNU} width={60} onEnter={checkCount} />
              <NumInput label={<>{t('countEqs')} <i>r</i> =</>} value={nE} onChange={setNE} width={60} onEnter={checkCount} />
              {!solved && <button className="btn" onClick={checkCount}>{t('check')}</button>}
            </div>
          </>
        )}

        <Notes msgs={msgs} />

        <div className="actions">
          {!solved && <HintButton level={hintLevel} onClick={hint} disabled={hintLevel >= maxHint} />}
          {solved && (
            <button className="btn btn-primary" onClick={advance} autoFocus>
              {t('next')} →
            </button>
          )}
        </div>
      </div>
    </section>
  );
}

function setLabel(set: ReactionSet, label: string): string {
  const parts: Record<ReactionSet, string[]> = {
    xy: [`[[R_${label}x]]`, `[[R_${label}y]]`],
    y: [`[[R_${label}]]`],
    x: [`[[R_${label}x]]`],
    xym: [`[[R_${label}x]]`, `[[R_${label}y]]`, `[[M_${label}]]`],
  };
  return parts[set].join(', ');
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
