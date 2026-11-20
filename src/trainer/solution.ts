import type { Problem } from '../model/types';
import { allItems, defaultEquations, linearize, namedPoints, solve, unknownsOf, type EqSpec, type NamedPoint, type Unknown } from '../model/mechanics';
import { expressionFor, termsOf, type Term } from './solve';
import type { UserEq } from './session';

/** Jeden řádek výpočtu: neznámá [u] z rovnice č. [eq] (výraz [expr]). */
export interface SolutionLine {
  u: Unknown;
  eq: number;
  expr: string;
  value: number;
}

/** Vzorové řešení úlohy: rovnice, pořadí výpočtu reakcí a výsledky. */
export interface Solution {
  eqs: UserEq[];
  terms: Term[][];
  lines: SolutionLine[];
  /** neznámé, které nejde spočítat jednu po druhé – řeší se soustavou */
  coupled: Unknown[];
  values: Record<string, number>;
}

const priority = (e: EqSpec) => (e.kind === 'hinge' ? 0 : e.kind === 'm' ? 1 : e.kind === 'fx' ? 2 : 3);

const sameSpec = (a: EqSpec, b: EqSpec) => JSON.stringify(a) === JSON.stringify(b);

/**
 * Řešení tak, jak by ho psal žák: v každém kroku rovnice s jedinou dosud nespočítanou neznámou.
 * Přednost mají rovnice z [defaultEquations] (ty, které trenažér nabízí v kroku 3) a momentové podmínky;
 * když by zbyla soustava (Gerberův nosník), přidá se momentová podmínka k dalšímu bodu nebo kloub z druhé strany.
 */
export function fullSolution(p: Problem): Solution {
  const items = allItems(p);
  const unknowns = unknownsOf(p);
  const defaults = defaultEquations(p);
  const extras: EqSpec[] = [
    ...namedPoints(p)
      .map((q): EqSpec => ({ kind: 'm', point: q.label, x: q.x }))
      .filter((e) => !defaults.some((d) => sameSpec(d, e))),
    ...p.hinges.map((h): EqSpec => {
      const used = defaults.find((d) => d.kind === 'hinge' && d.hingeId === h.id) as Extract<EqSpec, { kind: 'hinge' }>;
      return { kind: 'hinge', hingeId: h.id, point: h.label, x: h.x, side: used.side === 'left' ? 'right' : 'left' };
    }),
  ];
  const candidates = [...defaults, ...extras];
  const values = solve(defaults, items, unknowns)!;
  const candTerms = candidates.map((c) => termsOf({ spec: c, cw: false }, items));
  const solved: Record<string, number> = {};
  const nSolved = () => Object.keys(solved).length;
  const steps: [number, string, string][] = [];
  const usedEqs = new Set<number>();
  while (nSolved() < unknowns.length) {
    let pick = null as [number, string] | null;
    let pickScore = Infinity;
    candidates.forEach((_, i) => {
      if (usedEqs.has(i)) return;
      const open = [...new Set(candTerms[i].map((t) => t.it.unknownId).filter((u): u is string => !!u))].filter((u) => !(u in solved));
      if (open.length !== 1) return;
      const score = (i < defaults.length ? 0 : 1000) + priority(candidates[i]) * 100 + i;
      if (score < pickScore) {
        pickScore = score;
        pick = [i, open[0]];
      }
    });
    if (!pick) break;
    const [i, uid] = pick as [number, string];
    steps.push([i, uid, expressionFor(candTerms[i], uid, solved)!]);
    solved[uid] = values[uid];
    usedEqs.add(i);
  }
  // rovnice v pořadí trenažéru (výchozí, pak doplněné); nespočítané neznámé → všechny výchozí rovnice
  const shownSet = new Set(usedEqs);
  if (nSolved() < unknowns.length) defaults.forEach((_, i) => shownSet.add(i));
  const shown = [...shownSet].sort((a, b) => a - b);
  return {
    eqs: shown.map((i) => ({ spec: candidates[i], cw: false })),
    terms: shown.map((i) => candTerms[i]),
    lines: steps.map(([i, uid, expr]) => ({ u: unknowns.find((u) => u.id === uid)!, eq: shown.indexOf(i), expr, value: values[uid] })),
    coupled: unknowns.filter((u) => !(u.id in solved)),
    values,
  };
}

export interface DoneSummary {
  point: NamedPoint;
  residual: number;
  stars: number;
}

/** Kontrola momentovou podmínkou k jinému bodu, než se obvykle volí, a hodnocení podle nápověd. */
export function doneSummary(p: Problem, results: Record<string, number>, hints: number): DoneSummary {
  const pts = namedPoints(p);
  const pt = pts[pts.length - 1];
  const lin = linearize({ kind: 'm', point: pt.label, x: pt.x }, allItems(p));
  const residual = Object.entries(lin.a).reduce((s, [k, v]) => s + v * results[k], lin.c);
  const stars = hints === 0 ? 3 : hints <= 3 ? 2 : 1;
  return { point: pt, residual, stars };
}
