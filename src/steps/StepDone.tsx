import { useMemo, useState } from 'react';
import type { Problem } from '../model/types';
import { unknownsOf } from '../model/mechanics';
import { doneSummary } from '../trainer/solution';
import { unitOf } from '../trainer/solve';
import { useT } from '../i18n';
import { Actions, StepPanel } from '../components/ui';
import { Sym } from '../components/Sym';
import { fmt } from '../format';
import { DonatePrompt } from '../components/Donate';

interface Props {
  problem: Problem;
  results: Record<string, number>;
  hints: number;
  /** ukázat výzvu k podpoře (ne po každé úloze) */
  askDonate: boolean;
  onNext: () => void;
  onRetry: () => void;
  onPdf: () => void;
}

export function StepDone({ problem: p, results, hints, askDonate, onNext, onRetry, onPdf }: Props) {
  const t = useT();
  const sum = useMemo(() => doneSummary(p, results, hints), [p, results, hints]);
  const [donateOpen, setDonateOpen] = useState(askDonate);
  return (
    <StepPanel title={t('s5Title')} className="done">
      <div className="stars" aria-label={`${sum.stars}/3`}>
        {[1, 2, 3].map((i) => (
          <span key={i} className={i <= sum.stars ? 'star on' : 'star'}>
            ★
          </span>
        ))}
      </div>
      <p className="lead">{t('s5Text')}</p>
      <div className="results">
        {unknownsOf(p).map((u) => (
          <div key={u.id} className="result">
            <span className="react-sym">
              <Sym s={u.sym} />
            </span>{' '}
            = <strong>{fmt(results[u.id])}</strong> {unitOf(u)}
          </div>
        ))}
      </div>
      <p className="small muted">
        {t('s5Check')}: Σ<Sym s={`M_${sum.point.label}`} /> = {fmt(sum.residual, 3)} ≈ 0 ✓ · {t('hintsUsed')}: {hints}
      </p>
      <Actions center>
        <button className="btn" onClick={onRetry}>
          {t('retry')}
        </button>
        <button className="btn" onClick={onPdf}>
          {t('pdfExport')}
        </button>
        <button className="btn btn-primary" onClick={onNext} autoFocus>
          {t('nextProblem')} →
        </button>
      </Actions>
      {donateOpen && <DonatePrompt text={t('donateDone')} onClose={() => setDonateOpen(false)} />}
    </StepPanel>
  );
}
