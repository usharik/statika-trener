import type { EqSpec } from '../model/mechanics';
import { eqTitle } from '../trainer/equations';
import { Rich, Sym } from '../components/Sym';

/** Název rovnice; u kloubu se šipkou zvolené části. */
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
