export type SupportType = 'pin' | 'roller' | 'fixed';

export interface Support {
  id: string;
  label: string;
  x: number;
  type: SupportType;
}

export interface Hinge {
  id: string;
  label: string;
  x: number;
}

/** Osamělá síla. angle = směr síly ve stupních od osy +x, proti směru hodinových ručiček (dolů = 270). */
export interface PointForce {
  kind: 'force';
  id: string;
  label: string;
  x: number;
  F: number;
  angle: number;
  /** úhel od osy nosníku, který se kótuje ve výkresu (jen šikmé síly) */
  alpha?: number;
}

export interface PointMoment {
  kind: 'moment';
  id: string;
  label: string;
  x: number;
  M: number;
  ccw: boolean;
}

/** Spojité rovnoměrné zatížení působící svisle dolů. */
export interface UDL {
  kind: 'udl';
  id: string;
  label: string;
  x1: number;
  x2: number;
  q: number;
}

export type Load = PointForce | PointMoment | UDL;

export type BeamType = 'simple' | 'overhang' | 'cantilever' | 'gerber';

export interface Problem {
  seed: number;
  type: BeamType;
  L: number;
  supports: Support[];
  hinges: Hinge[];
  loads: Load[];
}

export type Level = 1 | 2 | 3;
