/**
 * Obrázek k úloze – jen data. Kreslí ho drawFig() v practice/drawFig.ts. Úhly ve stupních; „φ“ = od osy +x
 * proti směru hodinových ručiček (matematicky, osa y nahoru). Shodné s aplikací (practice/Fig.kt).
 */
export type TriSide = 'adj' | 'opp' | 'hyp';
export type AngleFrom = 'horizontal' | 'vertical' | 'plusX';

export interface TriFig {
  kind: 'tri';
  adj: number;
  opp: number;
  rot: number;
  mirror: boolean;
  labels: Partial<Record<TriSide, string>>;
  highlight: TriSide | null;
  angle: string | null;
}

export interface ForceFig {
  kind: 'force';
  f: number;
  phi: number;
  angle: number;
  from: AngleFrom;
  label: string;
  components: boolean;
  unit: string;
}

export type Fig =
  | TriFig
  | ForceFig
  | { kind: 'comps'; fx: number; fy: number; solved: boolean }
  | { kind: 'slope'; rise: number; run: number; riseLabel: string | null; runLabel: string | null; angle: string | null }
  | { kind: 'lever'; r: number; alpha: number; f: number; ccw: boolean; arm: boolean }
  | { kind: 'rafter'; len: number; alpha: number; projections: boolean }
  | { kind: 'sum'; forces: ForceFig[] }
  | { kind: 'calc'; expr: string; value: string; mode: string };

export const Fig = {
  tri: (adj: number, opp: number, o: { rot?: number; mirror?: boolean; labels?: Partial<Record<TriSide, string>>; highlight?: TriSide | null; angle?: string | null } = {}): TriFig => ({
    kind: 'tri',
    adj,
    opp,
    rot: o.rot ?? 0,
    mirror: o.mirror ?? false,
    labels: o.labels ?? {},
    highlight: o.highlight ?? null,
    angle: o.angle === undefined ? 'α' : o.angle,
  }),
  force: (f: number, phi: number, angle: number, o: { from?: AngleFrom; label?: string; components?: boolean; unit?: string } = {}): ForceFig => ({
    kind: 'force',
    f,
    phi,
    angle,
    from: o.from ?? 'horizontal',
    label: o.label ?? 'F',
    components: o.components ?? false,
    unit: o.unit ?? 'kN',
  }),
  comps: (fx: number, fy: number, solved = false): Fig => ({ kind: 'comps', fx, fy, solved }),
  slope: (rise: number, run: number, riseLabel: string | null = null, runLabel: string | null = null, angle: string | null = 'α'): Fig => ({ kind: 'slope', rise, run, riseLabel, runLabel, angle }),
  lever: (r: number, alpha: number, f: number, ccw = true, arm = false): Fig => ({ kind: 'lever', r, alpha, f, ccw, arm }),
  rafter: (len: number, alpha: number, projections = true): Fig => ({ kind: 'rafter', len, alpha, projections }),
  sum: (forces: ForceFig[]): Fig => ({ kind: 'sum', forces }),
  calc: (expr: string, value: string, mode: string): Fig => ({ kind: 'calc', expr, value, mode }),
};

export const TRI_SIDES: TriSide[] = ['adj', 'opp', 'hyp'];
