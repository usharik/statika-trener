import { useState } from 'react';
import type { PointForce, Problem, UDL } from '../model/types';
import { forceComponents } from '../model/mechanics';
import { useT } from '../i18n';
import { HintButton, NumInput, Notes, type Msg } from '../components/ui';
import { Rich } from '../components/Sym';
import { close, fmt, parseNum } from '../format';

interface Props {
  problem: Problem;
  onResolve: (loadId: string) => void;
  onHighlight: (id: string | null) => void;
  onHint: () => void;
  onDone: () => void;
}

export function StepLoads({ problem: p, onResolve, onHighlight, onHint, onDone }: Props) {
  const t = useT();
  const tasks = p.loads.filter((l): l is UDL | PointForce => l.kind === 'udl' || (l.kind === 'force' && !!l.alpha));
  const moments = p.loads.filter((l) => l.kind === 'moment');
  const [idx, setIdx] = useState(0);
  const [a, setA] = useState('');
  const [b, setB] = useState('');
  const [state, setState] = useState<[('ok' | 'nudge' | null), ('ok' | 'nudge' | null)]>([null, null]);
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [hintLevel, setHintLevel] = useState(0);
  const [solved, setSolved] = useState(false);
  const [log, setLog] = useState<Msg[]>([]);

  const task = tasks[idx];

  if (!task) {
    return (
      <section className="panel">
        <h2>{t('s2Title')}</h2>
        {log.length > 0 ? (
          <ul className="log">
            {log.map((m, i) => (
              <li key={i}><span className="check">✓</span> <Rich text={m.text} /></li>
            ))}
          </ul>
        ) : (
          <p className="lead">{t('s2Nothing')}</p>
        )}
        {moments.map((m) => (
          <p key={m.id} className="small muted"><Rich text={t('s2Moment', { m: m.label })} /></p>
        ))}
        <div className="actions">
          <button className="btn btn-primary" onClick={onDone} autoFocus>{t('next')} →</button>
        </div>
      </section>
    );
  }

  const Q = task.kind === 'udl' ? task.label.replace(/^q/, 'Q') : '';
  const len = task.kind === 'udl' ? task.x2 - task.x1 : 0;
  const comps = task.kind === 'force' ? forceComponents(task.F, task.angle) : { fx: 0, fy: 0 };
  const ansA = task.kind === 'udl' ? task.q * len : Math.abs(comps.fx);
  const ansB = task.kind === 'udl' ? len / 2 : Math.abs(comps.fy);

  const check = () => {
    const va = parseNum(a);
    const vb = parseNum(b);
    const okA = va !== null && close(Math.abs(va), ansA);
    const okB = vb !== null && close(Math.abs(vb), ansB);
    setState([okA ? 'ok' : 'nudge', okB ? 'ok' : 'nudge']);
    const out: Msg[] = [];
    if (task.kind === 'udl') {
      if (!okA) out.push({ tone: 'nudge', text: va !== null && close(va, task.q) ? t('udl_q') : t('udl_Qgen') });
      if (!okB) out.push({ tone: 'nudge', text: vb !== null && (close(vb, len) || vb > len) ? t('udl_posL') : t('udl_posGen') });
    } else {
      if (!okA || !okB) {
        const swapped = va !== null && vb !== null && close(va, ansB) && close(vb, ansA);
        const full = (va !== null && close(va, task.F)) || (vb !== null && close(vb, task.F));
        out.push({ tone: 'nudge', text: swapped ? t('f_swap') : full ? t('f_full') : t('f_gen') });
      }
    }
    if (okA && okB) {
      setSolved(true);
      onResolve(task.id);
      out.push({ tone: 'ok', text: task.kind === 'udl' ? t('udl_ok', { Q, Qv: fmt(ansA) }) : t('f_ok') });
    }
    setMsgs(out);
  };

  const hint = () => {
    onHint();
    const lvl = hintLevel + 1;
    setHintLevel(lvl);
    let text: string;
    if (task.kind === 'udl') {
      const v = { Q, q: fmt(task.q), l: fmt(len), Qv: fmt(ansA), h: fmt(ansB) };
      text = t(lvl === 1 ? 'udl_h1' : lvl === 2 ? 'udl_h2' : 'udl_h3', v);
    } else {
      const v = { F: task.label, v: fmt(task.F), a: task.alpha!, x: fmt(ansA), y: fmt(ansB) };
      text = t(lvl === 1 ? 'f_h1' : lvl === 2 ? 'f_h2' : 'f_h3', v);
    }
    setMsgs((m) => [...m.filter((x) => x.tone !== 'hint'), { tone: 'hint', text }]);
  };

  const advance = () => {
    const ok = msgs.find((m) => m.tone === 'ok');
    if (ok) setLog((l) => [...l, ok]);
    setIdx(idx + 1);
    setA('');
    setB('');
    setState([null, null]);
    setMsgs([]);
    setHintLevel(0);
    setSolved(false);
    onHighlight(tasks[idx + 1]?.id ?? null);
  };

  return (
    <section className="panel">
      <h2>{t('s2Title')}</h2>
      <p className="lead">{t('s2Intro')}</p>
      {log.length > 0 && (
        <ul className="log">
          {log.map((m, i) => (
            <li key={i}><span className="check">✓</span> <Rich text={m.text} /></li>
          ))}
        </ul>
      )}
      <div className="task" onMouseEnter={() => onHighlight(task.id)}>
        <p>
          <Rich text={task.kind === 'udl' ? t('udlTask', { q: fmt(task.q), l: fmt(len), Q }) : t('forceTask', { F: task.label, v: fmt(task.F), a: task.alpha! })} />
        </p>
        <div className="row gap">
          <NumInput
            label={<Rich text={task.kind === 'udl' ? t('udlQ', { Q }) : t('fx', { F: task.label })} />}
            value={a}
            onChange={setA}
            state={state[0]}
            onEnter={check}
          />
          <NumInput
            label={<Rich text={task.kind === 'udl' ? t('udlPos', { Q }) : t('fy', { F: task.label })} />}
            value={b}
            onChange={setB}
            state={state[1]}
            onEnter={check}
          />
        </div>
        <Notes msgs={msgs} />
        <div className="actions">
          {!solved && <button className="btn" onClick={check}>{t('check')}</button>}
          {!solved && <HintButton level={hintLevel} onClick={hint} disabled={hintLevel >= 3} />}
          {solved && <button className="btn btn-primary" onClick={advance} autoFocus>{t('next')} →</button>}
        </div>
      </div>
    </section>
  );
}
