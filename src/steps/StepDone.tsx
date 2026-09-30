import type { Problem } from '../model/types';
import { allItems, linearize, namedPoints, unknownsOf } from '../model/mechanics';
import { useT } from '../i18n';
import { Sym } from '../components/Sym';
import { fmt } from '../format';

interface Props {
  problem: Problem;
  results: Record<string, number>;
  hintsUsed: number;
  onNext: () => void;
  onRetry: () => void;
}

export function StepDone({ problem: p, results, hintsUsed, onNext, onRetry }: Props) {
  const t = useT();
  const us = unknownsOf(p);
  // kontrola momentovou podmínkou k jinému bodu, než se obvykle volí
  const pts = namedPoints(p);
  const P = pts[pts.length - 1];
  const lin = linearize({ kind: 'm', point: P.label, x: P.x }, allItems(p));
  const residual = Object.entries(lin.a).reduce((s, [k, v]) => s + v * results[k], lin.c);
  const stars = hintsUsed === 0 ? 3 : hintsUsed <= 3 ? 2 : 1;

  return (
    <section className="panel done">
      <div className="stars" aria-label={`${stars}/3`}>
        {[1, 2, 3].map((i) => (
          <span key={i} className={i <= stars ? 'star on' : 'star'}>★</span>
        ))}
      </div>
      <h2>{t('s5Title')}</h2>
      <p className="lead">{t('s5Text')}</p>
      <div className="results">
        {us.map((u) => (
          <div key={u.id} className="result">
            <Sym s={u.sym} /> = <strong>{fmt(results[u.id])}</strong> {u.kind === 'm' ? 'kNm' : 'kN'}
          </div>
        ))}
      </div>
      <p className="small muted">
        {t('s5Check')}: Σ<Sym s={`M_${P.label}`} /> = {fmt(residual, 3)} ≈ 0 ✓ · {t('hintsUsed')}: {hintsUsed}
      </p>
      <div className="actions center">
        <button className="btn" onClick={onRetry}>{t('retry')}</button>
        <button className="btn btn-primary" onClick={onNext} autoFocus>{t('nextProblem')} →</button>
      </div>
    </section>
  );
}
