/* Kreslení obrázků procvičování (shodné s aplikací – learn/TrigKit.kt fig / forceFig / compsFig). */
import { fmt } from '../format';
import { pt, screenAngle, triGeom, type PrimBuilder } from '../learn/kit/prims';
import type { Colors } from '../learn/kit/colors';
import { Trig } from './trig';
import type { Fig, ForceFig } from './fig';

/** výška obrázku procvičování */
export const FIG_H = 230;

export function drawFig(b: PrimBuilder, f: Fig, c: Colors) {
  switch (f.kind) {
    case 'tri': {
      const g = triGeom(f.adj, f.opp, { rot: f.rot, mirror: f.mirror, fit: [70, 30, 260, FIG_H - 70] });
      b.rightTriangle(g, f.labels, f.highlight, f.angle);
      break;
    }
    case 'force':
      forceFig(b, f, 200, FIG_H / 2 + 5, c);
      break;
    case 'sum':
      b.axes(200, FIG_H / 2 + 5, 150);
      for (const x of f.forces) {
        const othersRight = f.forces.some((o) => o !== x && Math.cos((o.phi * Math.PI) / 180) > 0.1);
        forceFig(b, x, 200, FIG_H / 2 + 5, c, false, othersRight);
      }
      break;
    case 'slope': {
      const g = triGeom(f.run, f.rise, { fit: [80, 30, 260, FIG_H - 70] });
      b.line(20, g.c.y, 380, g.c.y, c.ink, 1.4);
      b.rightTriangle(g, {}, null, null, c.ink, c.derived);
      if (f.runLabel !== null) b.text((g.a.x + g.c.x) / 2, g.c.y + 20, f.runLabel, c.derived, 14);
      if (f.riseLabel !== null) b.text(g.c.x + 10, (g.c.y + g.b.y) / 2 + 5, f.riseLabel, c.derived, 14, 'start');
      if (f.angle !== null) {
        b.angleArc(g.a.x, g.a.y, 26, screenAngle(g.a, g.c), screenAngle(g.a, g.b), null);
        b.text(g.a.x - 8, g.a.y - 8, f.angle, c.inkSoft, 13, 'end');
      }
      break;
    }
    case 'lever': {
      const ox = 70;
      const oy = FIG_H / 2 + 20;
      const len = 220;
      const ex = ox + len;
      b.circle(ox, oy, 6, c.paper, c.ink, 2.0);
      b.text(ox - 10, oy + 22, 'O', c.ink, 14, 'middle', null, true);
      b.rect(ox, oy - 6, len, 12, c.beam, c.ink, 1.4);
      const dirScreen = f.ccw ? -f.alpha : f.alpha;
      const a = (dirScreen * Math.PI) / 180;
      const l = 90;
      const ux = Math.cos(a);
      const uy = Math.sin(a);
      const tx = ex + l * ux;
      const ty = oy + l * uy;
      b.line(ex - 200 * ux, oy - 200 * uy, ex + 200 * ux, oy + 200 * uy, c.inkSoft, 1.0, true, 0.6);
      b.force(ex, oy, tx, ty, c.load, 3.0);
      b.text(tx + 6, ty + (f.ccw ? -4 : 14), ` = ${fmt(f.f)} kN`, c.load, 13, 'start', 'F');
      b.angleArc(ex, oy, 26, 0, dirScreen, 'α');
      const dimY = f.ccw ? oy - 34 : oy + 30;
      b.dimLine(ox, dimY, ex, dimY, `r = ${fmt(f.r)} m`);
      if (f.arm) {
        const t = (ox - ex) * ux;
        const px = ex + ux * t;
        const py = oy + uy * t;
        b.line(ox, oy, px, py, c.accent, 2.2, true);
        b.rightAngleMark(pt(px, py), pt(ox, oy), pt(ex, oy), 8);
        b.text((ox + px) / 2 - 8, (oy + py) / 2, null, c.accent, 14, 'end', 'r_⊥');
      }
      break;
    }
    case 'rafter': {
      const x0 = 70;
      const y0 = FIG_H - 40;
      const len = Math.min(270 / Math.max(Trig.cosD(f.alpha), 1e-3), (y0 - 22) / Math.max(Trig.sinD(f.alpha), 1e-3));
      const ex = x0 + len * Trig.cosD(f.alpha);
      const ey = y0 - len * Trig.sinD(f.alpha);
      b.line(20, y0, 380, y0, c.ink, 1.2);
      b.line(x0, y0, ex, ey, c.ink, 9.0);
      b.line(x0, y0, ex, ey, c.beam, 6.0);
      b.text((x0 + ex) / 2 - 10, (y0 + ey) / 2 - 10, `L = ${fmt(f.len)} m`, c.ink, 13, 'end');
      b.angleArc(x0, y0, 30, 0, screenAngle(pt(x0, y0), pt(ex, ey)), `${fmt(f.alpha, 0)}°`);
      if (f.projections) {
        b.dimLine(x0, y0 + 16, ex, y0 + 16, '?', 14, c.derived);
        b.dimLine(ex + 16, y0, ex + 16, ey, '?', 14, c.derived);
        b.line(ex, ey, ex, y0, c.derived, 1.0, true);
      }
      break;
    }
    case 'comps':
      compsFig(b, f.fx, f.fy, f.solved, c);
      break;
    case 'calc':
      b.rect(90, 40, 220, 120, c.subtle, c.ink, 2.0);
      b.rect(105, 55, 190, 80, c.paper, c.inkSoft, 1.0);
      if (f.mode) b.text(285, 72, f.mode, f.mode === 'DEG' ? c.accent : c.nudge, 11, 'end', null, true);
      b.text(115, 92, f.expr, c.ink, 15, 'start');
      b.text(285, 124, f.value, c.ink, 22, 'end', null, true);
      break;
  }
}

function forceFig(b: PrimBuilder, f: ForceFig, cx: number, cy: number, c: Colors, withAxes = true, verticalLabelLeft = false) {
  if (withAxes) b.axes(cx, cy, 150);
  const len = 95;
  const a = (f.phi * Math.PI) / 180;
  const tx = cx + len * Math.cos(a);
  const ty = cy - len * Math.sin(a);
  if (f.components) {
    const fx = Trig.cosD(f.phi);
    const fy = Trig.sinD(f.phi);
    b.line(tx, cy, tx, ty, c.derived, 1.0, true);
    b.line(cx, ty, tx, ty, c.derived, 1.0, true);
    if (Math.abs(fx) > 0.02) b.force(cx, cy, cx + len * fx, cy, c.derived, 2.4);
    if (Math.abs(fy) > 0.02) b.force(cx, cy, cx, cy - len * fy, c.derived, 2.4);
    const us = f.label.indexOf('_');
    const sub = us < 0 ? '' : f.label.slice(us + 1);
    if (Math.abs(fx) > 0.02) {
      if (f.from === 'plusX') b.text(cx + len * fx + (fx > 0 ? 8 : -8), cy - 8, null, c.derived, 13, fx > 0 ? 'start' : 'end', `F_${sub}x`);
      else b.text(cx + (len * fx) / 2, cy + (fy > 0 ? 18 : -8), null, c.derived, 13, 'middle', `F_${sub}x`);
    }
    if (Math.abs(fy) > 0.02) b.text(cx + (fx > 0 ? -12 : 12), cy - (len * fy) / 2 + 5, null, c.derived, 13, fx > 0 ? 'end' : 'start', `F_${sub}y`);
  }
  b.force(cx, cy, tx, ty, c.load, 3.0);
  const lx = tx + 10 * Math.cos(a);
  const ly = ty - 10 * Math.sin(a);
  const vertical = Math.abs(Math.cos(a)) < 0.25;
  const labelX = lx + (vertical ? (verticalLabelLeft ? -12 : 12) : 0);
  const labelY = Math.sin(a) > 0 ? ly - 4 : Math.min(ly + 14, FIG_H - 6);
  b.text(
    labelX,
    vertical ? (Math.sin(a) > 0 ? ty + 16 : Math.min(ty + 4, FIG_H - 6)) : labelY,
    ` = ${fmt(f.f)}` + (f.unit ? ` ${f.unit}` : ''),
    c.load,
    14,
    vertical ? (verticalLabelLeft ? 'end' : 'start') : Math.cos(a) >= 0 ? 'start' : 'end',
    f.label,
  );
  const arrowScreen = -f.phi;
  if (f.angle !== 0) {
    if (f.from === 'plusX') {
      b.arc(cx, cy, 28, 0, -f.phi, c.inkSoft, 1.4);
      const mid = ((-f.phi / 2) * Math.PI) / 180;
      b.text(cx + 46 * Math.cos(mid), cy + 46 * Math.sin(mid) + 5, `φ = ${fmt(f.angle, 0)}°`, c.inkSoft, 13);
    } else {
      const base = f.from === 'horizontal' ? (Math.cos(a) >= 0 ? 0 : 180) : Math.sin(a) >= 0 ? -90 : 90;
      b.angleArc(cx, cy, 34, base, arrowScreen, null);
      angleLabel(b, cx, cy, base, arrowScreen, `${fmt(f.angle, 0)}°`, tx, ty, f.components, c);
    }
  }
}

/** Popisek vyznačeného úhlu v ose oblouku; u dokreslených složek se posune tam, kde je volno. */
function angleLabel(b: PrimBuilder, cx: number, cy: number, a1: number, a2: number, label: string, tx: number, ty: number, components: boolean, c: Colors) {
  const sweep = ((((a2 - a1) % 360) + 540) % 360) - 180;
  if (Math.abs(sweep) < 0.5) return;
  const mid = ((a1 + sweep / 2) * Math.PI) / 180;
  const w = label.length * 13 * 0.58 + 4;
  let r = 50;
  if (components) {
    for (const cand of [50, 62, 74, 86, 98, 112, 126]) {
      const x = cx + cand * Math.cos(mid);
      const y = cy + cand * Math.sin(mid);
      const hitV = Math.abs(x - tx) < w / 2 + 4 && y >= Math.min(cy, ty) - 8 && y <= Math.max(cy, ty) + 8;
      const hitH = Math.abs(y - ty) < 12 && x >= Math.min(cx, tx) - w / 2 && x <= Math.max(cx, tx) + w / 2;
      const hitAxes = Math.abs(y - cy) < 12 || Math.abs(x - cx) < w / 2 + 4;
      r = cand;
      if (!hitV && !hitH && !hitAxes) break;
    }
  }
  b.text(cx + r * Math.cos(mid), cy + r * Math.sin(mid) + 5, label, c.inkSoft, 13);
}

/** Složky (se znaménky) a z nich výslednice R; před vyřešením „R = ?“ a „α = ?“. */
function compsFig(b: PrimBuilder, fx: number, fy: number, solved: boolean, c: Colors) {
  const cx = 200;
  const cy = FIG_H / 2 + 5;
  b.axes(cx, cy, 150);
  const k = 85 / Math.max(Math.max(Math.abs(fx), Math.abs(fy)), 1e-9);
  const tx = cx + fx * k;
  const ty = cy - fy * k;
  b.line(tx, cy, tx, ty, c.derived, 1.0, true);
  b.line(cx, ty, tx, ty, c.derived, 1.0, true);
  b.force(cx, cy, tx, cy, c.derived, 2.6);
  b.force(cx, cy, cx, ty, c.derived, 2.6);
  b.text((cx + tx) / 2, cy + (fy > 0 ? 20 : -10), ` = ${fmt(fx)}`, c.derived, 13, 'middle', 'F_x');
  b.text(cx + (fx > 0 ? -10 : 10), (cy + ty) / 2 + 5, ` = ${fmt(fy)}`, c.derived, 13, fx > 0 ? 'end' : 'start', 'F_y');
  const r = Math.hypot(fx, fy);
  b.force(cx, cy, tx, ty, c.load, 3.0, 11, false, 1, !solved);
  b.text(tx + (fx > 0 ? 8 : -8), ty + (fy > 0 ? -6 : 16), ' = ' + (solved ? `${fmt(r)} kN` : '?'), c.load, 14, fx > 0 ? 'start' : 'end', 'R');
  const base = fx >= 0 ? 0 : 180;
  const arrow = screenAngle(pt(cx, cy), pt(tx, ty));
  b.angleArc(cx, cy, 30, base, arrow, null);
  const a = Trig.atanD(Math.abs(fy) / Math.max(Math.abs(fx), 1e-9));
  angleLabel(b, cx, cy, base, arrow, 'α = ' + (solved ? `${fmt(a, 1)}°` : '?'), tx, ty, true, c);
}
