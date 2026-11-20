import { angleMark, num, type PrimBuilder } from './prims';

const F_KN = 10;
const SCALE = 14; // jednotek na 1 kN

/**
 * Šikmá síla 10 kN shora na nosník s hrotem v (tx, ty), úhel [alpha] od nosníku (z lekce 3; používá ji i trigonometrie).
 * Složky začínají u konce šipky a jsou o kousek odsunuté, aby je síla nikdy nepřekryla (ani při 0° a 90°).
 * [atTip] = složky působí ve stejném bodě jako síla (hroty v působišti) – tak je kreslíme jako zatížení nosníku.
 * [fa] = průhlednost celé síly, [ca] = průhlednost složek.
 */
export function inclined(b: PrimBuilder, tx: number, ty: number, alpha: number, fa: number, ca: number, labels = true, atTip = false) {
  const c = b.c;
  const a = (alpha * Math.PI) / 180;
  const ux = Math.cos(a);
  const uy = Math.sin(a);
  const len = F_KN * SCALE;
  const sx = tx - len * ux;
  const sy = ty - len * uy;
  const off = 8;
  if (ca > 0 && atTip) {
    // složky působí ve stejném bodě jako síla: obě končí hrotem v působišti (tx, ty)
    b.line(sx, sy, tx, sy, c.derived, 1.0, true, ca * 0.6);
    b.line(sx, sy, sx, ty, c.derived, 1.0, true, ca * 0.6);
    if (ux > 0.02) b.force(sx, ty - 4, tx, ty - 4, c.derived, 2.4, 11, false, ca);
    if (uy > 0.02) b.force(tx, sy, tx, ty, c.derived, 2.4, 11, false, ca);
    if (labels) {
      // pod nosníkem – nad ním je oblouk úhlu
      if (ux > 0.02) b.text((sx + tx) / 2, ty + 28, ` = ${num(F_KN * ux)} kN`, c.derived, 13, 'middle', 'F_x', false, ca);
      if (uy > 0.02) b.text(tx + 8, (sy + ty) / 2 + 5, ` = ${num(F_KN * uy)} kN`, c.derived, 13, 'start', 'F_y', false, ca);
    }
  } else if (ca > 0) {
    // doplňkový obdélník čárkovaně
    b.line(sx, ty, tx, ty, c.derived, 1.0, true, ca * 0.6);
    b.line(tx, sy, tx, ty, c.derived, 1.0, true, ca * 0.6);
    // F_x nad horní stranou obdélníku, F_y vlevo od levé strany
    if (ux > 0.02) b.force(sx, sy - off, tx, sy - off, c.derived, 2.4, 11, false, ca);
    if (uy > 0.02) b.force(sx - off, sy, sx - off, ty, c.derived, 2.4, 11, false, ca);
    if (labels) {
      if (ux > 0.02) b.text((sx + tx) / 2, sy - off - 10, ` = ${num(F_KN * ux)} kN`, c.derived, 13, 'middle', 'F_x', false, ca);
      if (uy > 0.02) b.text(sx - off - 8, (sy + ty) / 2 + 5, ` = ${num(F_KN * uy)} kN`, c.derived, 13, 'end', 'F_y', false, ca);
    }
  }
  if (fa > 0) {
    b.force(sx, sy, tx, ty, c.load, 3.0, 11, false, fa);
    if (labels && atTip) {
      // u konce šipky – u hrotu jsou popisky složek
      b.text(sx + 8, sy - 12, ` = ${num(F_KN)} kN`, c.load, 14, 'start', 'F', false, fa);
    } else if (labels) {
      if (alpha < 20) b.text(tx + 8, ty - 8, ` = ${num(F_KN)} kN`, c.load, 14, 'start', 'F', false, fa);
      else b.text((sx + tx) / 2 + 12 * uy + 8, (sy + ty) / 2 - 12 * ux + 18, ` = ${num(F_KN)} kN`, c.load, 14, 'start', 'F', false, fa);
    }
    if (alpha >= 1 && alpha <= 89) angleMark(b, [tx, ty], ux, uy, Math.round(alpha));
  }
}
