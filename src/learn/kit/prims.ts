/*
 * Primitiva obrázků výkladu – shodná s aplikací (ui/BeamScene.kt PrimBuilder, learn/Kit.kt, learn/TrigKit.kt).
 * Souřadnice scény: šířka w jednotek, osa y dolů. Obrázek se pak vykreslí jako SVG (Stage.tsx).
 */
import type { SupportType } from '../../model/types';
import { fmt } from '../../format';
import type { Colors } from './colors';
import { alpha as withAlpha } from './colors';

export type Anchor = 'start' | 'middle' | 'end';

export type Prim =
  | { k: 'line'; x1: number; y1: number; x2: number; y2: number; color: string; width: number; dash?: [number, number] | null; hl?: boolean; alpha: number; cap?: boolean }
  | { k: 'poly'; pts: [number, number][]; fill: string | null; stroke: string | null; width: number; hl?: boolean; alpha: number }
  | { k: 'rect'; x: number; y: number; w: number; h: number; fill: string | null; stroke: string | null; width: number; alpha: number }
  | { k: 'circle'; cx: number; cy: number; r: number; fill: string | null; stroke: string | null; width: number; dash?: [number, number] | null; alpha: number }
  /** oblouk se středem (cx, cy); úhly ve stupních po směru hodinových ručiček od osy +x (osa y dolů) */
  | { k: 'arc'; cx: number; cy: number; r: number; start: number; sweep: number; color: string; width: number; hl?: boolean; alpha: number }
  | { k: 'label'; x: number; y: number; sym: string | null; text: string | null; anchor: Anchor; color: string; size: number; bold: boolean; alpha: number };

export interface Pt {
  x: number;
  y: number;
}

export const pt = (x: number, y: number): Pt => ({ x, y });
export const dist = (p: Pt, x2: number, y2: number) => Math.hypot(p.x - x2, p.y - y2);

export class PrimBuilder {
  readonly prims: Prim[] = [];
  readonly w: number;
  readonly c: Colors;

  constructor(w: number, c: Colors) {
    this.w = w;
    this.c = c;
  }

  // ------------------------------------------------------------- základ (BeamScene.kt)

  arrow(x1: number, y1: number, x2: number, y2: number, color: string, width = 2, dash: [number, number] | null = null, head = 10, hl = false, alpha = 1) {
    const ang = Math.atan2(y2 - y1, x2 - x1);
    const a1 = ang + Math.PI - 0.32;
    const a2 = ang + Math.PI + 0.32;
    const bx = x2 - Math.cos(ang) * head * 0.8;
    const by = y2 - Math.sin(ang) * head * 0.8;
    this.prims.push({ k: 'line', x1, y1, x2: bx, y2: by, color, width, dash, hl, alpha });
    this.prims.push({
      k: 'poly',
      pts: [
        [x2, y2],
        [x2 + Math.cos(a1) * head, y2 + Math.sin(a1) * head],
        [x2 + Math.cos(a2) * head, y2 + Math.sin(a2) * head],
      ],
      fill: color,
      stroke: null,
      width: 0,
      hl,
      alpha,
    });
  }

  /** Oblouk momentu kolem bodu (cx, cy), nad nosníkem; ccw (↺) má hrot vlevo. */
  momentArc(cx: number, cy: number, r: number, ccw: boolean, color: string, width = 2, hl = false, alpha = 1) {
    const a0 = (200 * Math.PI) / 180;
    const a1 = (-20 * Math.PI) / 180;
    const p = (a: number): [number, number] => [cx + r * Math.cos(a), cy - r * Math.sin(a)];
    const [sx, sy] = p(a0);
    const [ex, ey] = p(a1);
    this.prims.push({ k: 'arc', cx, cy, r, start: 160, sweep: 220, color, width, hl, alpha });
    const [tx, ty] = ccw ? [sx, sy] : [ex, ey];
    const tang = ccw ? a0 + Math.PI / 2 : a1 - Math.PI / 2;
    const dx = Math.cos(tang);
    const dy = -Math.sin(tang);
    const h = 9;
    const nx = -dy;
    const ny = dx;
    this.prims.push({
      k: 'poly',
      pts: [
        [tx + dx * h * 0.6, ty + dy * h * 0.6],
        [tx - dx * h * 0.6 + nx * h * 0.45, ty - dy * h * 0.6 + ny * h * 0.45],
        [tx - dx * h * 0.6 - nx * h * 0.45, ty - dy * h * 0.6 - ny * h * 0.45],
      ],
      fill: color,
      stroke: null,
      width: 0,
      hl,
      alpha,
    });
  }

  hatch(x1: number, x2: number, y: number, alpha = 1) {
    this.prims.push({ k: 'line', x1, y1: y, x2, y2: y, color: this.c.ink, width: 1.5, alpha });
    for (let x = x1 + 4; x <= x2; x += 6) this.prims.push({ k: 'line', x1: x, y1: y, x2: x - 6, y2: y + 7, color: this.c.inkSoft, width: 1, alpha });
  }

  // ------------------------------------------------------------- stavebnice karet (Kit.kt)

  line(x1: number, y1: number, x2: number, y2: number, color: string, width = 1.5, dash = false, alpha = 1) {
    this.prims.push({ k: 'line', x1, y1, x2, y2, color, width, dash: dash ? [4, 4] : null, alpha });
  }

  circle(cx: number, cy: number, r: number, fill: string | null, stroke: string | null = null, width = 1.5, alpha = 1, dash: [number, number] | null = null) {
    this.prims.push({ k: 'circle', cx, cy, r, fill, stroke, width, alpha, dash });
  }

  rect(x: number, y: number, w: number, h: number, fill: string | null, stroke: string | null = null, width = 1.5, alpha = 1) {
    this.prims.push({ k: 'rect', x, y, w, h, fill, stroke, width, alpha });
  }

  poly(pts: [number, number][], fill: string | null, stroke: string | null = null, width = 1.5, alpha = 1) {
    this.prims.push({ k: 'poly', pts, fill, stroke, width, alpha });
  }

  arc(cx: number, cy: number, r: number, start: number, sweep: number, color: string, width: number, alpha = 1) {
    this.prims.push({ k: 'arc', cx, cy, r, start, sweep, color, width, alpha });
  }

  /** Text; [sym] se vysází jako značka s indexem (F_x), [text] obyčejně za ní. Popisek nesmí vyjet z obrázku. */
  text(x: number, y: number, text: string | null, color: string, size = 15, anchor: Anchor = 'middle', sym: string | null = null, bold = false, alpha = 1) {
    const wd = ((sym ? sym.replace('_', '').length : 0) + (text ? text.length : 0)) * size * 0.58 + 4;
    const left = anchor === 'start' ? x : anchor === 'end' ? x - wd : x - wd / 2;
    const dx = left < 4 ? 4 - left : left + wd > this.w - 4 ? this.w - 4 - (left + wd) : 0;
    this.prims.push({ k: 'label', x: x + dx, y, sym, text, anchor, color, size, bold, alpha });
  }

  /** Šipka síly od (x1, y1) k hrotu (x2, y2). */
  force(x1: number, y1: number, x2: number, y2: number, color: string, width = 2.4, head = 11, glow = false, alpha = 1, dash = false) {
    this.arrow(x1, y1, x2, y2, color, width, dash ? [5, 4] : null, head, glow, alpha);
  }

  /** Oblouk momentu kolem (cx, cy); ccw = ↺. */
  moment(cx: number, cy: number, r: number, ccw: boolean, color: string, width = 2.2, glow = false, alpha = 1) {
    this.momentArc(cx, cy, r, ccw, color, width, glow, alpha);
  }

  /** Vodorovný nosník (obdélník) mezi x1 a x2 s osou v y. */
  beam(x1: number, x2: number, y: number, alpha = 1) {
    this.rect(x1, y - 5, x2 - x1, 10, this.c.beam, this.c.ink, 1.6, alpha);
  }

  /** Značka podpory stejná jako v trenažéru; [wallLeft] = zeď vetknutí vlevo. */
  support(type: SupportType, x: number, y: number, label = '', ghost = false, wallLeft = true) {
    supportSymbol(this, type, wallLeft, label, x, ghost, y);
  }

  /** Místo, na které se dá klepnout (ne táhnout) – čárkovaný kroužek, ať se neplete s ručkou. */
  tapSpot(x: number, y: number, selected = false, r = 13) {
    const c = this.c;
    this.prims.push({ k: 'circle', cx: x, cy: y, r, fill: withAlpha(c.react, selected ? 0.18 : 0.06), stroke: c.react, width: 1.6, alpha: 1, dash: [4, 3] });
  }

  /** Ručka pro tah prstem – výrazný kroužek, aby bylo jasné, že se s ním dá hýbat. */
  handle(x: number, y: number, active = false) {
    this.circle(x, y, active ? 13 : 11, withAlpha('#ffba00', 0.25), this.c.highlight, 2);
    this.circle(x, y, 4, this.c.ink);
  }

  // ------------------------------------------------------------- trigonometrie (TrigKit.kt)

  /** Značka pravého úhlu ve vrcholu [at] mezi směry k bodům [p1] a [p2]. */
  rightAngleMark(at: Pt, p1: Pt, p2: Pt, size = 11) {
    const u = (p: Pt): [number, number] => {
      const d = Math.max(Math.hypot(p.x - at.x, p.y - at.y), 1e-9);
      return [(p.x - at.x) / d, (p.y - at.y) / d];
    };
    const [ux, uy] = u(p1);
    const [vx, vy] = u(p2);
    const q1 = [at.x + ux * size, at.y + uy * size];
    const q2 = [at.x + (ux + vx) * size, at.y + (uy + vy) * size];
    const q3 = [at.x + vx * size, at.y + vy * size];
    this.line(q1[0], q1[1], q2[0], q2[1], this.c.inkSoft, 1.2);
    this.line(q2[0], q2[1], q3[0], q3[1], this.c.inkSoft, 1.2);
  }

  /** Oblouk úhlu se středem (cx, cy) mezi směry [a1] a [a2] (stupně na obrazovce) – kreslí se ten kratší. */
  angleArc(cx: number, cy: number, r: number, a1: number, a2: number, label: string | null, color: string = this.c.inkSoft) {
    const sweep = ((((a2 - a1) % 360) + 540) % 360) - 180;
    if (Math.abs(sweep) < 0.5) return;
    this.prims.push({ k: 'arc', cx, cy, r, start: a1, sweep, color, width: 1.4, alpha: 1 });
    if (label !== null) {
      const mid = ((a1 + sweep / 2) * Math.PI) / 180;
      this.text(cx + (r + 16) * Math.cos(mid), cy + (r + 16) * Math.sin(mid) + 5, label, color, 13);
    }
  }

  /** Pravoúhlý trojúhelník s popisky stran a úhlu α; [highlight] = zvýrazněná strana. */
  rightTriangle(g: TriGeom, labels: Partial<Record<TriSide, string>> = {}, highlight: TriSide | null = null, angle: string | null = 'α', hyp: string = this.c.load, legs: string = this.c.derived) {
    const c = this.c;
    this.poly(
      [
        [g.a.x, g.a.y],
        [g.b.x, g.b.y],
        [g.c.x, g.c.y],
      ],
      c.subtle,
      null,
      0,
      0.6,
    );
    const cen = centroid(g);
    for (const s of TRI_SIDES) {
      const [p, q] = triSide(g, s);
      const hl = s === highlight;
      this.line(p.x, p.y, q.x, q.y, hl ? c.highlight : s === 'hyp' ? hyp : legs, hl ? 4 : 2.4);
      const t = labels[s];
      if (t !== undefined) {
        const mx = (p.x + q.x) / 2;
        const my = (p.y + q.y) / 2;
        const dx = mx - cen.x;
        const dy = my - cen.y;
        const d = Math.max(Math.hypot(dx, dy), 1e-9);
        const ox = mx + (dx / d) * 18;
        const oy = my + (dy / d) * 18 + 5;
        const anchor: Anchor = dx / d > 0.4 ? 'start' : dx / d < -0.4 ? 'end' : 'middle';
        this.text(ox, oy, t, hl ? c.highlight : s === 'hyp' ? hyp : legs, 14, anchor, null, hl);
      }
    }
    this.rightAngleMark(g.c, g.a, g.b);
    if (angle !== null) this.angleArc(g.a.x, g.a.y, 26, screenAngle(g.a, g.c), screenAngle(g.a, g.b), angle);
  }

  /** Půlkruhový úhloměr 0–180° se středem (cx, cy) a dílky po 10°. */
  protractor(cx: number, cy: number, r: number) {
    const c = this.c;
    this.prims.push({ k: 'arc', cx, cy, r, start: 180, sweep: 180, color: c.inkSoft, width: 1.4, alpha: 1 });
    this.line(cx - r, cy, cx + r, cy, c.inkSoft, 1.2);
    for (let d = 0; d <= 180; d += 10) {
      const a = (d * Math.PI) / 180;
      const l = d % 30 === 0 ? 10 : 5;
      this.line(cx + r * Math.cos(a), cy - r * Math.sin(a), cx + (r - l) * Math.cos(a), cy - (r - l) * Math.sin(a), c.inkSoft, 1);
      if (d % 30 === 0) this.text(cx + (r - 22) * Math.cos(a), cy - (r - 22) * Math.sin(a) + 4, `${d}`, c.inkSoft, 10);
    }
  }

  /** Čtverečkovaná síť [nx] × [ny] políček o straně [step] od (ox, oy). */
  grid(ox: number, oy: number, step: number, nx: number, ny: number) {
    for (let i = 0; i <= nx; i++) this.line(ox + i * step, oy, ox + i * step, oy + ny * step, this.c.line, 0.8);
    for (let j = 0; j <= ny; j++) this.line(ox, oy + j * step, ox + nx * step, oy + j * step, this.c.line, 0.8);
  }

  /** Osy x (doprava) a y (nahoru) s počátkem v (cx, cy). */
  axes(cx: number, cy: number, len: number, labels = true) {
    const c = this.c;
    this.arrow(cx - len, cy, cx + len, cy, c.inkSoft, 1.2, null, 8);
    this.arrow(cx, cy + len * 0.8, cx, cy - len * 0.8, c.inkSoft, 1.2, null, 8);
    if (labels) {
      this.text(cx + len - 4, cy + 16, 'x', c.inkSoft, 13);
      this.text(cx + 12, cy - len * 0.8 + 6, 'y', c.inkSoft, 13);
    }
  }

  /** Kóta mezi dvěma body s textem uprostřed (odsazeným o [offset] kolmo ke kótě). */
  dimLine(x1: number, y1: number, x2: number, y2: number, text: string, offset = 14, color: string = this.c.inkSoft) {
    this.line(x1, y1, x2, y2, color, 1);
    const d = Math.max(Math.hypot(x2 - x1, y2 - y1), 1e-9);
    const nx = -(y2 - y1) / d;
    const ny = (x2 - x1) / d;
    for (const [x, y] of [
      [x1, y1],
      [x2, y2],
    ])
      this.line(x - nx * 5, y - ny * 5, x + nx * 5, y + ny * 5, color, 1);
    this.text((x1 + x2) / 2 + nx * offset, (y1 + y2) / 2 + ny * offset + 5, text, color, 13);
  }

  /** Svislý sloupec 0…1 (např. sin α) – výška [h] odpovídá hodnotě 1. */
  bar(x: number, y: number, w: number, h: number, value: number, color: string) {
    this.rect(x, y, w, h, null, this.c.line, 1);
    const v = Math.min(1, Math.max(0, value));
    this.rect(x, y + h * (1 - v), w, h * v, color, null, 1.5, 0.85);
  }
}

/** Značka podpory (shodná s trenažérem); [wallLeft] = zeď vetknutí vlevo. */
export function supportSymbol(b: PrimBuilder, type: SupportType, wallLeft: boolean, label: string, xx: number, ghost: boolean, y: number) {
  const c = b.c;
  const op = ghost ? 0.16 : 1;
  const HB = 5;
  const top = y + HB;
  if (type === 'fixed') {
    const d = wallLeft ? -1 : 1;
    for (let yy = y - 34; yy <= y + 34; yy += 7) b.prims.push({ k: 'line', x1: xx, y1: yy, x2: xx + d * 8, y2: yy + 8, color: c.inkSoft, width: 1.3, alpha: op });
    b.prims.push({ k: 'line', x1: xx, y1: y - 36, x2: xx, y2: y + 38, color: c.ink, width: 2.2, alpha: op });
    if (label) b.prims.push({ k: 'label', x: xx + d * 20, y: y + 58, sym: null, text: label, anchor: 'middle', color: c.ink, size: 16, bold: true, alpha: 1 });
    return;
  }
  const triH = type === 'pin' ? 24 : 20;
  const base = top + 4 + triH;
  b.prims.push({
    k: 'poly',
    pts: [
      [xx, top + 4],
      [xx - 13, base],
      [xx + 13, base],
    ],
    fill: c.paper,
    stroke: c.ink,
    width: 1.5,
    alpha: op,
  });
  b.prims.push({ k: 'circle', cx: xx, cy: top + 4, r: 4, fill: c.paper, stroke: c.ink, width: 1.5, alpha: op });
  if (type === 'pin') b.hatch(xx - 20, xx + 20, base, op);
  else {
    b.prims.push({ k: 'circle', cx: xx - 7, cy: base + 3.5, r: 3.2, fill: c.paper, stroke: c.ink, width: 1.3, alpha: op });
    b.prims.push({ k: 'circle', cx: xx + 7, cy: base + 3.5, r: 3.2, fill: c.paper, stroke: c.ink, width: 1.3, alpha: op });
    b.hatch(xx - 20, xx + 20, base + 7, op);
  }
  if (label) b.prims.push({ k: 'label', x: xx + 22, y: top + 22, sym: null, text: label, anchor: 'start', color: c.ink, size: 16, bold: true, alpha: 1 });
}

/** Úhel mezi osou nosníku a nositelkou síly, u působiště (BeamScene.kt angleMark). */
export function angleMark(b: PrimBuilder, tip: [number, number], ux: number, uy: number, alpha: number) {
  const c = b.c;
  const r = 30;
  const hx = ux > 0 ? -1 : 1;
  const [tx, ty] = tip;
  b.prims.push({ k: 'line', x1: tx, y1: ty, x2: tx + hx * (r + 12), y2: ty, color: c.inkSoft, width: 1, dash: [3, 3], alpha: 1 });
  const a0 = hx > 0 ? 0 : 180;
  const a1 = (Math.atan2(-uy, -ux) * 180) / Math.PI;
  let d = a1 - a0;
  while (d > 180) d -= 360;
  while (d <= -180) d += 360;
  b.prims.push({ k: 'arc', cx: tx, cy: ty, r, start: a0, sweep: d, color: c.inkSoft, width: 1, alpha: 1 });
  b.prims.push({ k: 'label', x: tx + hx * (r + 6) - (hx < 0 ? 16 : -2), y: ty - 8, sym: null, text: `${alpha}°`, anchor: 'start', color: c.inkSoft, size: 12, bold: false, alpha: 1 });
}

// ------------------------------------------------------------- trojúhelník (TrigKit.kt)

export type TriSide = 'adj' | 'opp' | 'hyp';
export const TRI_SIDES: TriSide[] = ['adj', 'opp', 'hyp'];

/** Pravoúhlý trojúhelník: [a] = vrchol úhlu α, [c] = pravý úhel, [b] = třetí vrchol. */
export interface TriGeom {
  a: Pt;
  b: Pt;
  c: Pt;
}

export function triSide(g: TriGeom, s: TriSide): [Pt, Pt] {
  return s === 'adj' ? [g.a, g.c] : s === 'opp' ? [g.c, g.b] : [g.a, g.b];
}

export const centroid = (g: TriGeom): Pt => pt((g.a.x + g.b.x + g.c.x) / 3, (g.a.y + g.b.y + g.c.y) / 3);
export const triMinX = (g: TriGeom) => Math.min(g.a.x, g.b.x, g.c.x);
export const triMaxX = (g: TriGeom) => Math.max(g.a.x, g.b.x, g.c.x);
export const triMinY = (g: TriGeom) => Math.min(g.a.y, g.b.y, g.c.y);
export const triMaxY = (g: TriGeom) => Math.max(g.a.y, g.b.y, g.c.y);

/**
 * Geometrie trojúhelníku s odvěsnami [adj] (u α) a [opp]. Základní poloha: α vlevo dole, pravý úhel vpravo dole.
 * [rot] = otočení o rot × 90° proti směru hodin, [mirror] = zrcadlení vodorovně.
 * S [fit] = [x, y, w, h] se trojúhelník vystředí do obdélníku; jinak [scale] a vrchol α v [origin].
 */
export function triGeom(adj: number, opp: number, opts: { origin?: Pt; scale?: number; rot?: number; mirror?: boolean; fit?: [number, number, number, number] | null } = {}): TriGeom {
  const { origin = pt(0, 0), scale = 1, rot = 0, mirror = false, fit = null } = opts;
  let pts: [number, number][] = [
    [0, 0],
    [adj, opp],
    [adj, 0],
  ];
  if (mirror) pts = pts.map(([x, y]) => [-x, y]);
  const r = ((rot % 4) + 4) % 4;
  for (let i = 0; i < r; i++) pts = pts.map(([x, y]) => [-y, x]);
  let sp: [number, number][] = pts.map(([x, y]) => [x, -y]);
  let k: number, ox: number, oy: number;
  if (fit) {
    const [fx, fy, fw, fh] = fit;
    const minX = Math.min(...sp.map((p) => p[0]));
    const maxX = Math.max(...sp.map((p) => p[0]));
    const minY = Math.min(...sp.map((p) => p[1]));
    const maxY = Math.max(...sp.map((p) => p[1]));
    k = Math.min(fw / Math.max(maxX - minX, 1e-9), fh / Math.max(maxY - minY, 1e-9));
    ox = fx + (fw - (maxX - minX) * k) / 2 - minX * k;
    oy = fy + (fh - (maxY - minY) * k) / 2 - minY * k;
  } else {
    k = scale;
    ox = origin.x;
    oy = origin.y;
  }
  sp = sp.map(([x, y]) => [ox + x * k, oy + y * k]);
  return { a: pt(sp[0][0], sp[0][1]), b: pt(sp[1][0], sp[1][1]), c: pt(sp[2][0], sp[2][1]) };
}

/** směr z [from] do [to] ve stupních na obrazovce (po směru hodin od +x, osa y dolů) */
export const screenAngle = (from: Pt, to: Pt) => (Math.atan2(to.y - from.y, to.x - from.x) * 180) / Math.PI;

/** Číslo pro popisky ve výkladu: desetinná čárka, bez zbytečných nul. */
export const num = (v: number, d = 2) => fmt(v, d);
