/// <reference types="node" />
/*
 * Export úloh pro kontrolu, že mobilní aplikace (Kotlin) generuje stejné úlohy jako web.
 * Normálně se přeskakuje; nový soubor:  GOLDEN=1 npx vitest run src/model/golden.test.ts
 * → mobile/shared/src/jvmTest/resources/golden.txt (čte ho GoldenTest v Kotlinu).
 */
import { it } from 'vitest';
import { writeFileSync } from 'node:fs';
import { generate } from './generator';
import { solveProblem } from './mechanics';
import type { BeamType, Level, Problem } from './types';

const r6 = (v: number) => Math.round(v * 1e6);
function line(p: Problem, level: number, types: string): string {
  const sup = p.supports.map((s) => `${s.id}:${s.label}:${r6(s.x)}:${s.type}`).join(',');
  const hin = p.hinges.map((h) => `${h.id}:${h.label}:${r6(h.x)}`).join(',');
  const loads = p.loads
    .map((l) =>
      l.kind === 'force'
        ? `force:${l.id}:${l.label}:${r6(l.x)}:${r6(l.F)}:${r6(l.angle)}:${l.alpha ?? ''}`
        : l.kind === 'moment'
          ? `moment:${l.id}:${l.label}:${r6(l.x)}:${r6(l.M)}:${l.ccw}`
          : `udl:${l.id}:${l.label}:${r6(l.x1)}:${r6(l.x2)}:${r6(l.q)}`,
    )
    .join(',');
  const r = solveProblem(p)!;
  const res = Object.keys(r).sort().map((k) => `${k}=${Math.round(r[k] * 1000)}`).join(',');
  return [p.seed, level, types, p.type, r6(p.L), sup, hin, loads, res].join('|');
}

it.skipIf(!process.env.GOLDEN)('export úloh pro mobilní aplikaci', () => {
  const out: string[] = [];
  const all: BeamType[] = ['simple', 'overhang', 'cantilever', 'gerber'];
  const sets: BeamType[][] = [all, ['simple'], ['overhang'], ['cantilever'], ['gerber'], ['simple', 'gerber']];
  for (const level of [1, 2, 3] as Level[])
    for (const types of sets)
      for (let seed = 1; seed <= 400; seed++) out.push(line(generate(((seed * 2473) % 999999) + 1, { level, types }), level, types.join('+')));
  writeFileSync('mobile/shared/src/jvmTest/resources/golden.txt', out.join('\n') + '\n');
});
