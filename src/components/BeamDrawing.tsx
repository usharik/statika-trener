import type { ReactNode } from 'react';
import type { Problem, Support } from '../model/types';
import { loadItems, unknownsOfSupport, type Unknown } from '../model/mechanics';
import { fmt } from '../format';

export interface DrawingProps {
  problem: Problem;
  /** podpory, které už jsou nahrazené reakcemi */
  released?: Set<string>;
  /** id zatížení nahrazených výslednicí / složkami */
  resolved?: Set<string>;
  /** zvýraznění položky (id položky, reakce nebo zatížení) */
  highlight?: string | null;
  /** bod momentové podmínky */
  momentPoint?: { label: string; x: number } | null;
  /** zobrazit jen jednu část (podmínka v kloubu) */
  side?: { x: number; side: 'left' | 'right' } | null;
  /** zakreslit rameno síly */
  arm?: { from: number; to: number } | null;
  /** výsledky – reakce kreslené ve skutečném smyslu s hodnotami */
  results?: Record<string, number> | null;
  onSupportClick?: (id: string) => void;
  /** úzký displej: kratší souřadnicová šířka, aby popisky zůstaly čitelné */
  compact?: boolean;
  activeSupport?: string | null;
}

// šířka a okraj výkresu (v jednotkách SVG); na úzkém displeji menší šířka = větší písmo
let W = 880;
let M = 130;
const Y = 178;
const HB = 5; // polovina výšky nosníku
const H = 358;

const C = {
  ink: 'var(--ink)',
  soft: 'var(--ink-soft)',
  load: 'var(--load)',
  react: 'var(--react)',
  derived: 'var(--derived)',
  accent: 'var(--accent)',
};

/** Popisek se spodním indexem pro SVG (bez baseline-shift kvůli Firefoxu). */
function SvgLabel({ x, y, sym, value, anchor = 'middle', color = C.ink, size = 15 }: { x: number; y: number; sym: string; value?: string; anchor?: 'start' | 'middle' | 'end'; color?: string; size?: number }) {
  // udržet popisek uvnitř výkresu
  const w = labelWidth(sym, value, size);
  const left = anchor === 'start' ? x : anchor === 'end' ? x - w : x - w / 2;
  if (left < 6) x += 6 - left;
  else if (left + w > W - 6) x -= left + w - (W - 6);
  const i = sym.indexOf('_');
  const main = i < 0 ? sym : sym.slice(0, i);
  const sub = i < 0 ? '' : sym.slice(i + 1);
  return (
    <text x={x} y={y} textAnchor={anchor} fill={color} fontSize={size} className="svg-label">
      <tspan fontStyle="italic">{main}</tspan>
      {sub && (
        <tspan dy={size * 0.3} fontSize={size * 0.72}>
          {sub}
        </tspan>
      )}
      {value && <tspan dy={sub ? -size * 0.3 : 0}>{' = ' + value}</tspan>}
    </text>
  );
}

interface Box { l: number; r: number; t: number; b: number }

const labelWidth = (sym: string, value: string | undefined, size: number) => (sym.replace('_', '').length + (value ? value.length + 3 : 0)) * size * 0.6 + 6;

/** Jednoduché rozmísťování popisků: posouvá je nahoru, dokud se nepřekrývají. */
function makePlacer(obstacles: Box[]) {
  const boxes = [...obstacles];
  const place = (x: number, y: number, sym: string, value: string | undefined, anchor: 'start' | 'middle' | 'end', size = 15) => {
    const w = labelWidth(sym, value, size);
    const l = anchor === 'start' ? x : anchor === 'end' ? x - w : x - w / 2;
    let yy = y;
    const hit = (b: Box) => l < b.r && l + w > b.l && yy - size < b.b && yy + 4 > b.t;
    for (let k = 0; k < 8 && boxes.some(hit); k++) yy -= size + 3;
    boxes.push({ l, r: l + w, t: yy - size, b: yy + 4 });
    return yy;
  };
  const free = (x: number, y: number, sym: string, value: string | undefined, anchor: 'start' | 'middle' | 'end', size = 15) => {
    const w = labelWidth(sym, value, size);
    const l = anchor === 'start' ? x : anchor === 'end' ? x - w : x - w / 2;
    return l > 4 && l + w < W - 4 && !boxes.some((b) => l < b.r && l + w > b.l && y - size < b.b && y + 4 > b.t);
  };
  /** první volná z kandidátních poloh, jinak první posunutá nahoru */
  const placeAny = (cands: { x: number; y: number; anchor: 'start' | 'middle' | 'end' }[], sym: string, value?: string) => {
    const c = cands.find((k) => free(k.x, k.y, sym, value, k.anchor)) ?? cands[0];
    return { ...c, y: place(c.x, c.y, sym, value, c.anchor) };
  };
  return { place, placeAny, boxes };
}

function Arrow({ x1, y1, x2, y2, color, width = 2, dash, head = 10 }: { x1: number; y1: number; x2: number; y2: number; color: string; width?: number; dash?: string; head?: number }) {
  const ang = Math.atan2(y2 - y1, x2 - x1);
  const a1 = ang + Math.PI - 0.32;
  const a2 = ang + Math.PI + 0.32;
  const bx = x2 - Math.cos(ang) * head * 0.8;
  const by = y2 - Math.sin(ang) * head * 0.8;
  return (
    <g>
      <line x1={x1} y1={y1} x2={bx} y2={by} stroke={color} strokeWidth={width} strokeDasharray={dash} />
      <path d={`M${x2},${y2} L${x2 + Math.cos(a1) * head},${y2 + Math.sin(a1) * head} L${x2 + Math.cos(a2) * head},${y2 + Math.sin(a2) * head} Z`} fill={color} />
    </g>
  );
}

/** Oblouk momentu kolem bodu (cx, cy). */
function MomentArc({ cx, cy, r, ccw, color, width = 2 }: { cx: number; cy: number; r: number; ccw: boolean; color: string; width?: number }) {
  // oblouk nad nosníkem od -200° do 20° (v souřadnicích SVG, y dolů)
  const a0 = (200 * Math.PI) / 180;
  const a1 = (-20 * Math.PI) / 180;
  const p = (a: number) => [cx + r * Math.cos(a), cy - r * Math.sin(a)];
  const [sx, sy] = p(a0);
  const [ex, ey] = p(a1);
  // ccw (↺): šipka na levém konci (pohyb shora doleva)
  const [tx, ty] = ccw ? [sx, sy] : [ex, ey];
  const tang = ccw ? a0 + Math.PI / 2 : a1 - Math.PI / 2; // směr pohybu v hrotu
  const dx = Math.cos(tang);
  const dy = -Math.sin(tang);
  const h = 9;
  const n = [-dy, dx];
  return (
    <g>
      <path d={`M${sx},${sy} A${r},${r} 0 1 1 ${ex},${ey}`} fill="none" stroke={color} strokeWidth={width} />
      <path
        d={`M${tx + dx * h * 0.6},${ty + dy * h * 0.6} L${tx - dx * h * 0.6 + n[0] * h * 0.45},${ty - dy * h * 0.6 + n[1] * h * 0.45} L${tx - dx * h * 0.6 - n[0] * h * 0.45},${ty - dy * h * 0.6 - n[1] * h * 0.45} Z`}
        fill={color}
      />
    </g>
  );
}

function Hatch({ x1, x2, y, dir = 1 }: { x1: number; x2: number; y: number; dir?: 1 | -1 }) {
  const lines: ReactNode[] = [];
  for (let x = x1 + 4; x <= x2; x += 6) lines.push(<line key={x} x1={x} y1={y} x2={x - 6} y2={y + 7 * dir} />);
  return (
    <g stroke={C.soft} strokeWidth={1}>
      <line x1={x1} y1={y} x2={x2} y2={y} stroke={C.ink} strokeWidth={1.5} />
      {lines}
    </g>
  );
}

function SupportSymbol({ s, X, L, ghost }: { s: Support; X: number; L: number; ghost?: boolean }) {
  const op = ghost ? 0.16 : 1;
  const top = Y + HB;
  if (s.type === 'fixed') {
    const left = s.x < L / 2;
    const d = left ? -1 : 1;
    const lines: ReactNode[] = [];
    for (let y = Y - 34; y <= Y + 34; y += 7) lines.push(<line key={y} x1={X} y1={y} x2={X + d * 8} y2={y + 8} />);
    return (
      <g>
        <g style={{ opacity: op, transition: 'opacity 0.5s' }}>
          <g stroke={C.soft} strokeWidth={1}>{lines}</g>
          <line x1={X} y1={Y - 36} x2={X} y2={Y + 38} stroke={C.ink} strokeWidth={2.2} />
        </g>
        <text x={X + d * 20} y={Y + 58} textAnchor="middle" className="pt-label">{s.label}</text>
      </g>
    );
  }
  const triH = s.type === 'pin' ? 24 : 20;
  const base = top + 4 + triH;
  return (
    <g>
    <g style={{ opacity: op, transition: 'opacity 0.5s' }}>
      <path d={`M${X},${top + 4} L${X - 13},${base} L${X + 13},${base} Z`} fill="var(--paper)" stroke={C.ink} strokeWidth={1.5} strokeLinejoin="round" />
      <circle cx={X} cy={top + 4} r={4} fill="var(--paper)" stroke={C.ink} strokeWidth={1.5} />
      {s.type === 'pin' ? (
        <Hatch x1={X - 20} x2={X + 20} y={base} />
      ) : (
        <>
          <circle cx={X - 7} cy={base + 3.5} r={3.2} fill="var(--paper)" stroke={C.ink} strokeWidth={1.3} />
          <circle cx={X + 7} cy={base + 3.5} r={3.2} fill="var(--paper)" stroke={C.ink} strokeWidth={1.3} />
          <Hatch x1={X - 20} x2={X + 20} y={base + 7} />
        </>
      )}
    </g>
      <text x={X + 22} y={top + 22} className="pt-label">{s.label}</text>
    </g>
  );
}

export function BeamDrawing(props: DrawingProps) {
  W = props.compact ? 560 : 880;
  M = props.compact ? 72 : 130;
  const { problem: p, released, resolved, highlight, momentPoint, side, arm, results, onSupportClick, activeSupport } = props;
  const s = (W - 2 * M) / p.L;
  const X = (x: number) => M + x * s;
  const hl = (id: string | undefined) => !!id && highlight === id;
  const items = loadItems(p);

  // ---------- kóty ----------
  const keys = new Set<number>([0, p.L]);
  p.supports.forEach((q) => keys.add(q.x));
  p.hinges.forEach((q) => keys.add(q.x));
  p.loads.forEach((l) => {
    if (l.kind === 'udl') {
      keys.add(l.x1);
      keys.add(l.x2);
    } else keys.add(l.x);
  });
  const xs = [...keys].sort((a, b) => a - b);
  const yDim = Y + 88;
  const yTot = Y + 122;

  const dim: ReactNode[] = [];
  xs.forEach((x) => dim.push(<line key={'e' + x} x1={X(x)} y1={Y + 16} x2={X(x)} y2={yTot + 6} className="ext" />));
  const tick = (x: number, y: number, k: string) => <line key={k} x1={X(x) - 4} y1={y + 4} x2={X(x) + 4} y2={y - 4} stroke={C.ink} strokeWidth={1.3} />;
  for (let i = 0; i < xs.length - 1; i++) {
    const a = xs[i];
    const b = xs[i + 1];
    dim.push(<line key={'d' + i} x1={X(a)} y1={yDim} x2={X(b)} y2={yDim} className="dim" />);
    dim.push(tick(a, yDim, 't' + i));
    dim.push(
      <text key={'dt' + i} x={(X(a) + X(b)) / 2} y={yDim - 5} textAnchor="middle" className="dim-text">
        {fmt(b - a)}
      </text>,
    );
  }
  dim.push(tick(p.L, yDim, 'tl'));
  if (xs.length > 2) {
    dim.push(<line key="tot" x1={X(0)} y1={yTot} x2={X(p.L)} y2={yTot} className="dim" />);
    dim.push(tick(0, yTot, 'tt0'), tick(p.L, yTot, 'tt1'));
    dim.push(
      <text key="tott" x={(X(0) + X(p.L)) / 2} y={yTot - 5} textAnchor="middle" className="dim-text">
        {fmt(p.L)}
      </text>,
    );
  }

  // ---------- nosník ----------
  const cuts = [0, ...p.hinges.map((h) => h.x), p.L];
  const beam: ReactNode[] = [];
  for (let i = 0; i < cuts.length - 1; i++) {
    const a = X(cuts[i]) + (i > 0 ? 6 : 0);
    const b = X(cuts[i + 1]) - (i < cuts.length - 2 ? 6 : 0);
    beam.push(<rect key={'b' + i} x={a} y={Y - HB} width={b - a} height={2 * HB} fill="var(--beam)" stroke={C.ink} strokeWidth={1.6} />);
  }
  const hinges = p.hinges.map((h) => (
    <g key={h.id}>
      <circle cx={X(h.x)} cy={Y} r={6.5} fill="var(--paper)" stroke={C.ink} strokeWidth={1.8} />
      <text x={X(h.x)} y={Y + 30} textAnchor="middle" className="pt-label">{h.label}</text>
    </g>
  ));

  // ---------- zatížení ----------
  const obstacles: Box[] = [];
  for (const l of p.loads) {
    if (l.kind === 'force') {
      const r = (l.angle * Math.PI) / 180;
      const ux = Math.cos(r);
      const x0 = X(l.x);
      const x1 = x0 - (Math.sin(r) < 0 ? ux * 64 : -ux * 64);
      obstacles.push({ l: Math.min(x0, x1) - 3, r: Math.max(x0, x1) + 3, t: Y - HB - 64 * Math.abs(Math.sin(r)) + 6, b: Y - HB });
    } else if (l.kind === 'moment') obstacles.push({ l: X(l.x) - 24, r: X(l.x) + 24, t: Y - 26, b: Y });
    else obstacles.push({ l: X(l.x1), r: X(l.x2), t: Y - HB - 34, b: Y - HB });
  }
  for (const sp of p.supports) {
    const Xl = sp.type === 'fixed' ? X(sp.x) + (sp.x < p.L / 2 ? -20 : 20) - 6 : X(sp.x) + 20;
    const Yl = sp.type === 'fixed' ? Y + 58 : Y + HB + 22;
    obstacles.push({ l: Xl, r: Xl + 14, t: Yl - 14, b: Yl + 2 });
    if (!released?.has(sp.id) && !results) continue;
    const Xs = X(sp.x);
    for (const u of unknownsOfSupport(sp)) {
      if (u.kind === 'fy') obstacles.push({ l: Xs - 3, r: Xs + 3, t: Y + HB, b: Y + HB + 60 });
      else if (u.kind === 'fx') obstacles.push({ l: sp.x < p.L / 2 ? Xs - 60 : Xs, r: sp.x < p.L / 2 ? Xs : Xs + 60, t: Y - 4, b: Y + 4 });
      else obstacles.push({ l: Xs - 28, r: Xs + 28, t: Y - 30, b: Y });
    }
  }
  const { place, placeAny, boxes } = makePlacer(obstacles);
  const loadsG: ReactNode[] = [];
  const faded = (id: string) => !!resolved?.has(id);
  for (const [li, l] of p.loads.entries()) {
    const isHl = hl(l.id) || items.some((it) => it.loadId === l.id && hl(it.id));
    if (l.kind === 'force') {
      const r = (l.angle * Math.PI) / 180;
      const ux = Math.cos(r);
      const uy = -Math.sin(r);
      const len = 64;
      const down = uy > 0;
      const tip = down ? [X(l.x), Y - HB - 1] : [X(l.x) + ux * len, Y - HB - len];
      const tail = down ? [tip[0] - ux * len, tip[1] - uy * len] : [X(l.x), Y - HB - 1];
      const fade = faded(l.id);
      const col = fade ? C.soft : C.load;
      const anc = ux > 0.3 ? 'end' : ux < -0.3 ? 'start' : 'middle';
      const lx = tail[0] + (ux > 0.3 ? -4 : ux < -0.3 ? 4 : 0);
      loadsG.push(
        <g key={l.id} style={{ opacity: fade ? 0.45 : 1, transition: 'opacity 0.5s' }} className={isHl && !fade ? 'hl' : ''}>
          <g className="a-drop" style={{ animationDelay: `${320 + li * 90}ms` }}>
          <Arrow x1={tail[0]} y1={tail[1]} x2={tip[0]} y2={tip[1]} color={col} width={isHl ? 3 : 2} dash={fade ? '5 4' : undefined} />
          {l.alpha && !fade && <AngleMark tip={tip} ux={ux} uy={uy} alpha={l.alpha} />}
          {!fade && <SvgLabel x={lx} y={place(lx, Math.min(tail[1], tip[1]) - 8, l.label, `${fmt(l.F)} kN`, anc)} anchor={anc} sym={l.label} value={`${fmt(l.F)} kN`} color={col} />}
          </g>
        </g>,
      );
    } else if (l.kind === 'moment') {
      const v = `${fmt(l.M)} kNm`;
      const lp = placeAny(
        [
          { x: X(l.x), y: Y - 34, anchor: 'middle' },
          { x: X(l.x) + 26, y: Y - 22, anchor: 'start' },
          { x: X(l.x) - 26, y: Y - 22, anchor: 'end' },
        ],
        l.label,
        v,
      );
      loadsG.push(
        <g key={l.id} className={isHl ? 'hl' : ''}>
          <g className="a-spin" style={{ animationDelay: `${320 + li * 90}ms` }}>
            <MomentArc cx={X(l.x)} cy={Y} r={22} ccw={l.ccw} color={C.load} width={isHl ? 3 : 2} />
          </g>
          <g className="a-fade" style={{ animationDelay: `${420 + li * 90}ms` }}>
            <SvgLabel x={lp.x} y={lp.y} anchor={lp.anchor} sym={l.label} value={v} color={C.load} />
          </g>
        </g>,
      );
    } else {
      const a = X(l.x1);
      const b = X(l.x2);
      const top = Y - HB - 34;
      const n = Math.max(2, Math.round((b - a) / 22));
      const fade = faded(l.id);
      const arrows: ReactNode[] = [];
      for (let i = 0; i <= n; i++) {
        const x = a + ((b - a) * i) / n;
        arrows.push(<Arrow key={i} x1={x} y1={top} x2={x} y2={Y - HB - 1} color={fade ? C.soft : C.load} width={1.3} head={7} />);
      }
      loadsG.push(
        <g key={l.id} style={{ opacity: fade ? 0.35 : 1, transition: 'opacity 0.5s' }}>
          <g className="a-udl" style={{ animationDelay: `${320 + li * 90}ms` }}>
          <rect x={a} y={top} width={b - a} height={Y - HB - top} fill={fade ? 'none' : 'var(--load-fill)'} />
          <line x1={a} y1={top} x2={b} y2={top} stroke={fade ? C.soft : C.load} strokeWidth={1.6} />
          {arrows}
          {!fade && <SvgLabel x={(a + b) / 2} y={place((a + b) / 2, top - 8, l.label, `${fmt(l.q)} kN/m`, 'middle')} sym={l.label} value={`${fmt(l.q)} kN/m`} color={C.load} />}
          </g>
        </g>,
      );
    }
  }

  // složky a výslednice
  const derived: ReactNode[] = [];
  for (const it of items) {
      const l = p.loads.find((q) => q.id === it.loadId)!;
      if (!faded(l.id) || l.kind === 'moment') continue;
      const isHl = hl(it.id);
      const col = C.derived;
      const w = isHl ? 3 : 2.2;
      if (it.kind === 'fy') {
        const len = l.kind === 'udl' ? 70 : 50;
        const tipY = it.dir < 0 ? Y - HB - 1 : Y - HB - len;
        const tailY = it.dir < 0 ? Y - HB - len : Y - HB - 1;
        derived.push(
          <g key={it.id} className={(isHl ? 'hl ' : '') + 'a-pop'}>
            <Arrow x1={X(it.x)} y1={tailY} x2={X(it.x)} y2={tipY} color={col} width={w} />
            <PlacedLabel
              pos={placeAny(
                [
                  { x: X(it.x) + 7, y: Y - HB - len + 6, anchor: 'start' },
                  { x: X(it.x) - 7, y: Y - HB - len + 6, anchor: 'end' },
                  { x: X(it.x), y: Y - HB - len - 6, anchor: 'middle' },
                ],
                it.sym,
                `${fmt(it.value!)} kN`,
              )}
              sym={it.sym}
              value={`${fmt(it.value!)} kN`}
              color={col}
            />
          </g>,
        );
      } else {
        const len = 52;
        const tipX = X(it.x);
        // vodorovná složka kreslená v ose nosníku, šipka končí v působišti
        const tailX = tipX - it.dir * len;
        derived.push(
          <g key={it.id} className={(isHl ? 'hl ' : '') + 'a-pop'} style={{ animationDelay: '120ms' }}>
            <Arrow x1={tailX} y1={Y - 16} x2={tipX} y2={Y - 16} color={col} width={w} />
            <PlacedLabel
              pos={placeAny(
                [
                  { x: tailX - it.dir * 4, y: Y - 11, anchor: it.dir > 0 ? 'end' : 'start' },
                  { x: (tailX + tipX) / 2, y: Y - 24, anchor: 'middle' },
                ],
                it.sym,
                `${fmt(it.value!)} kN`,
              )}
              sym={it.sym}
              value={`${fmt(it.value!)} kN`}
              color={col}
            />
          </g>,
        );
      }
    }

  // ---------- podpory / reakce ----------
  const supportsG: ReactNode[] = [];
  const reactionsG: ReactNode[] = [];
  for (const sp of p.supports) {
    const isReleased = released?.has(sp.id) || !!results;
    const clickable = !!onSupportClick && !isReleased;
    supportsG.push(
      <g
        key={sp.id}
        onClick={clickable ? () => onSupportClick!(sp.id) : undefined}
        className={(clickable ? 'clickable ' : '') + (activeSupport === sp.id ? 'active-support' : '')}
      >
        {activeSupport === sp.id && <circle cx={X(sp.x)} cy={Y + 24} r={34} className="support-halo" />}
        <g className="a-rise" style={{ animationDelay: `${150 + p.supports.indexOf(sp) * 80}ms` }}>
          <SupportSymbol s={sp} X={X(sp.x)} L={p.L} ghost={isReleased} />
        </g>
      </g>,
    );
    if (isReleased) for (const u of unknownsOfSupport(sp)) reactionsG.push(<g key={u.id + (results ? '-res' : '')}>{Reaction({ u, X: X(u.x), L: p.L, hl: hl('r_' + u.id) || hl(u.id), value: results?.[u.id], placeAny })}</g>);
  }

  // ---------- bod momentu, strana, rameno ----------
  const overlays: ReactNode[] = [];
  if (side) {
    const xa = side.side === 'left' ? X(side.x) + 8 : 0;
    const xb = side.side === 'left' ? W : X(side.x) - 8;
    overlays.push(<rect key="side" x={xa} y={0} width={xb - xa} height={yDim - 16} className="side-mask" />);
  }
  if (arm && Math.abs(arm.to - arm.from) > 1e-6) {
    const ya = Y - 70;
    overlays.push(
      <g key="arm" className="arm a-fade">
        <line x1={X(arm.from)} y1={Y} x2={X(arm.from)} y2={ya - 6} strokeDasharray="3 3" />
        <line x1={X(arm.to)} y1={Y - 10} x2={X(arm.to)} y2={ya - 6} strokeDasharray="3 3" />
        <Arrow x1={(X(arm.from) + X(arm.to)) / 2} y1={ya} x2={X(arm.to)} y2={ya} color={C.accent} width={1.5} head={8} />
        <Arrow x1={(X(arm.from) + X(arm.to)) / 2} y1={ya} x2={X(arm.from)} y2={ya} color={C.accent} width={1.5} head={8} />
        <text x={(X(arm.from) + X(arm.to)) / 2} y={ya - 6} textAnchor="middle" fill={C.accent} fontSize={13} fontWeight={600}>
          {fmt(Math.abs(arm.to - arm.from))} m
        </text>
      </g>,
    );
  }
  if (momentPoint)
    overlays.push(
      <g key={'mp' + momentPoint.label} className="moment-point a-pop">
        <circle cx={X(momentPoint.x)} cy={Y} r={9} />
        <line x1={X(momentPoint.x) - 13} y1={Y} x2={X(momentPoint.x) + 13} y2={Y} />
        <line x1={X(momentPoint.x)} y1={Y - 13} x2={X(momentPoint.x)} y2={Y + 13} />
      </g>,
    );

  // ořez prázdného místa nad a pod výkresem (výkres je na obrazovce stále vidět, šetříme výšku);
  // horní mez počítá i s místem pro kótu ramene, aby výkres při nápovědě neposkakoval
  const vTop = Math.min(Y - 100, ...boxes.map((b) => b.t)) - 10;
  const vBottom = Math.min(H, yTot + 14);
  return (
    <svg viewBox={`0 ${vTop} ${W} ${vBottom - vTop}`} className="drawing" role="img">
      <g className="a-fade" style={{ animationDelay: '380ms' }}>
        {dim}
        <text x={W - 8} y={yTot + 4} textAnchor="end" className="dim-note">[m]</text>
      </g>
      {supportsG}
      <g className="a-beam">
        {beam}
        {hinges}
      </g>
      {loadsG}
      {derived}
      {reactionsG}
      {overlays}
    </svg>
  );
}

function AngleMark({ tip, ux, uy, alpha }: { tip: number[]; ux: number; uy: number; alpha: number }) {
  // úhel mezi osou nosníku a nositelkou síly, u působiště
  const r = 30;
  const hx = ux > 0 ? -1 : 1; // vodorovná polopřímka na straně konce síly
  const bx = tip[0] - ux * r;
  const by = tip[1] - uy * r;
  const sweep = ux > 0 ? 1 : 0;
  return (
    <g stroke="var(--ink-soft)" fill="none" strokeWidth={1}>
      <line x1={tip[0]} y1={tip[1]} x2={tip[0] + hx * (r + 12)} y2={tip[1]} strokeDasharray="3 3" />
      <path d={`M${tip[0] + hx * r},${tip[1]} A${r},${r} 0 0 ${sweep} ${bx},${by}`} />
      <text x={tip[0] + hx * (r + 6) - (hx < 0 ? 16 : -2)} y={tip[1] - 8} fontSize={12} fill="var(--ink-soft)" stroke="none">
        {alpha}°
      </text>
    </g>
  );
}

type PlaceAny = ReturnType<typeof makePlacer>['placeAny'];

function PlacedLabel({ pos, sym, value, color }: { pos: { x: number; y: number; anchor: 'start' | 'middle' | 'end' }; sym: string; value?: string; color: string }) {
  return (
    <g className="a-fade" style={{ animationDelay: '260ms' }}>
      <SvgLabel x={pos.x} y={pos.y} anchor={pos.anchor} sym={sym} value={value} color={color} size={14} />
    </g>
  );
}

/** Volá se jako funkce (ne jako komponenta), aby rozmísťování popisků proběhlo v pořadí kreslení. */
function Reaction({ u, X, L, hl, value, placeAny }: { u: Unknown; X: number; L: number; hl: boolean; value?: number; placeAny: PlaceAny }) {
  const neg = value !== undefined && value < 0;
  const col = C.react;
  const w = hl ? 3.2 : 2.4;
  const val = value !== undefined ? `${fmt(Math.abs(value))} ${u.kind === 'm' ? 'kNm' : 'kN'}` : undefined;
  const cls = hl ? 'hl' : '';
  if (u.kind === 'fy') {
    const len = 58;
    const up = !neg;
    // šipka zespodu: nahoru končí na spodní hraně, dolů začíná na spodní hraně
    const y1 = up ? Y + HB + len : Y + HB + 2;
    const y2 = up ? Y + HB + 2 : Y + HB + len;
    return (
      <g className={cls}>
        <g className="a-grow-top">
          <Arrow x1={X} y1={y1} x2={X} y2={y2} color={col} width={w} />
        </g>
        <PlacedLabel pos={placeAny([{ x: X + 8, y: Y + HB + len - 2, anchor: 'start' }, { x: X - 8, y: Y + HB + len - 2, anchor: 'end' }, { x: X + 8, y: Y + HB + len - 20, anchor: 'start' }], u.sym, val)} sym={u.sym} value={val} color={col} />
      </g>
    );
  }
  if (u.kind === 'fx') {
    const len = 52;
    const atLeft = u.x < L / 2 || u.x === 0;
    const right = !neg;
    // kreslíme na vnější straně podpory
    const o = atLeft ? -1 : 1;
    const far = X + o * (len + 6);
    const near = X + o * 6;
    const [x1, x2] = right === atLeft ? [far, near] : [near, far];
    return (
      <g className={cls}>
        <g className={atLeft ? 'a-grow-right' : 'a-grow-left'}>
          <Arrow x1={x1} y1={Y} x2={x2} y2={Y} color={col} width={w} />
        </g>
        <PlacedLabel pos={placeAny([{ x: far, y: Y - 10, anchor: atLeft ? 'start' : 'end' }, { x: far + o * 4, y: Y + 5, anchor: atLeft ? 'end' : 'start' }, { x: X + o * 10, y: Y + 24, anchor: atLeft ? 'end' : 'start' }, { x: far, y: Y + 24, anchor: atLeft ? 'start' : 'end' }], u.sym, val)} sym={u.sym} value={val} color={col} />
      </g>
    );
  }
  const atLeft = u.x < L / 2;
  return (
    <g className={cls}>
      <g className="a-spin">
        <MomentArc cx={X} cy={Y} r={26} ccw={!neg} color={col} width={w} />
      </g>
      <PlacedLabel pos={placeAny([{ x: X + (atLeft ? 18 : -18), y: Y - 34, anchor: atLeft ? 'start' : 'end' }, { x: X + (atLeft ? -18 : 18), y: Y - 34, anchor: atLeft ? 'end' : 'start' }, { x: X, y: Y - 44, anchor: 'middle' }], u.sym, val)} sym={u.sym} value={val} color={col} />
    </g>
  );
}
