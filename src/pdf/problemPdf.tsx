/*
 * Úloha jako PDF (A4): zadání s výkresem, volitelně celé vzorové řešení po krocích trenažéru.
 * Shodné s aplikací (ui/ProblemPdf.kt). Stránky se kreslí na plátno (písma prohlížeče – čeština i azbuka)
 * a zapíšou se jako obrázky do PDF (pdfWriter.ts).
 */
import { flushSync } from 'react-dom';
import { createRoot } from 'react-dom/client';
import type { Problem } from '../model/types';
import { allItems, forceComponents, linearize, namedPoints, unknownsOf, unknownsOfSupport } from '../model/mechanics';
import { fmt } from '../format';
import type { TKey, Tr } from '../i18n';
import { BeamDrawing, type DrawingProps } from '../components/BeamDrawing';
import { fullSolution } from '../trainer/solution';
import { eqTitle } from '../trainer/equations';
import { resultantSym } from '../trainer/loads';
import { unitOf, type Term } from '../trainer/solve';
import { A4, buildPdf, type PdfImagePage } from './pdfWriter';
import { RichBuilder, drawLaid, layout, type Laid, type Run } from './richCanvas';

export const WEB_URL = 'https://usharik.github.io/statika-trener/';

const MARGIN = 48;
const CONTENT_W = A4.w - 2 * MARGIN;
const FOOTER_H = 26;
/** pixelů na bod */
const S = 3;

/** Světlé barvy (PDF je vždy světlé) – shodné s :root v index.css. */
const LIGHT: Record<string, string> = {
  bg: '#f5f6f8',
  paper: '#ffffff',
  ink: '#1b2330',
  'ink-soft': '#6b7482',
  line: '#e3e6eb',
  ext: '#c9ced6',
  beam: '#eef1f5',
  load: '#d1432b',
  'load-fill': 'rgba(209, 67, 43, 0.06)',
  react: '#2459d6',
  derived: '#7c3aed',
  accent: '#0f9d6e',
};

interface TextStyle {
  size: number;
  lineH: number;
  color: string;
  bold?: boolean;
}

const BODY: TextStyle = { size: 10.5, lineH: 15, color: LIGHT.ink };
const SMALL: TextStyle = { size: 8.5, lineH: 11, color: LIGHT['ink-soft'] };

type Block =
  | { kind: 'text'; runs: Run[]; style: TextStyle; before: number; indent: number; keepWithNext: boolean }
  | { kind: 'figure'; img: HTMLImageElement; ratio: number; before: number; maxHeight: number };

type Placed = { kind: 'text'; laid: Laid; style: TextStyle; x: number; y: number } | { kind: 'figure'; img: HTMLImageElement; x: number; y: number; w: number; h: number };

class Doc {
  blocks: Block[] = [];
  private ctx: CanvasRenderingContext2D;

  constructor(ctx: CanvasRenderingContext2D) {
    this.ctx = ctx;
  }

  text(runs: Run[], style: TextStyle, before: number, indent = 0, keepWithNext = false) {
    this.blocks.push({ kind: 'text', runs: style.bold ? runs.map((r) => ({ ...r, bold: true })) : runs, style, before, indent, keepWithNext });
  }

  title(text: string) {
    this.text([{ text }], { size: 20, lineH: 26, color: LIGHT.ink, bold: true }, 0);
  }

  subtitle(text: string) {
    this.text([{ text }], { size: 10, lineH: 14, color: LIGHT['ink-soft'] }, 3);
  }

  heading(text: string, big = false) {
    this.text([{ text }], { size: big ? 15 : 12.5, lineH: big ? 20 : 17, color: LIGHT.ink, bold: true }, big ? 22 : 16, 0, true);
  }

  para(runs: Run[], before = 6, indent = 0, keepWithNext = false) {
    this.text(runs, BODY, before, indent, keepWithNext);
  }

  note(runs: Run[], before = 4) {
    this.text(runs, { size: 9.5, lineH: 13, color: LIGHT['ink-soft'] }, before);
  }

  figure(img: HTMLImageElement, ratio: number) {
    this.blocks.push({ kind: 'figure', img, ratio, before: 8, maxHeight: 230 });
  }

  private measure(b: Extract<Block, { kind: 'text' }>): Laid {
    // měří se v bodech; kreslí se pak se zvětšením S
    return layout(this.ctx, b.runs, CONTENT_W - b.indent, b.style.size, b.style.lineH);
  }

  private figureSize(b: Extract<Block, { kind: 'figure' }>): [number, number] {
    const w = Math.min(CONTENT_W, b.maxHeight / b.ratio);
    return [w, w * b.ratio];
  }

  /** Rozložení bloků na stránky; nadpis nezůstane sám na konci stránky. */
  paginate(): Placed[][] {
    const bottom = A4.h - MARGIN - FOOTER_H;
    const measured = this.blocks.map((b) => (b.kind === 'text' ? this.measure(b) : null));
    const heights = this.blocks.map((b, i) => (b.kind === 'text' ? measured[i]!.height : this.figureSize(b)[1]));
    const pages: Placed[][] = [[]];
    let y = MARGIN;
    this.blocks.forEach((b, i) => {
      const h = heights[i];
      // blok s keepWithNext se drží s celým řetězcem následujících (nadpis rovnice + její řádky)
      let need = b.before + h;
      let j = i;
      while (this.blocks[j].kind === 'text' && (this.blocks[j] as { keepWithNext: boolean }).keepWithNext && j + 1 < this.blocks.length) {
        j++;
        need += this.blocks[j].before + heights[j];
      }
      if (y + need > bottom && pages[pages.length - 1].length) {
        pages.push([]);
        y = MARGIN;
      }
      const page = pages[pages.length - 1];
      if (page.length) y += b.before;
      if (b.kind === 'text') page.push({ kind: 'text', laid: measured[i]!, style: b.style, x: MARGIN + b.indent, y });
      else {
        const [w, fh] = this.figureSize(b);
        page.push({ kind: 'figure', img: b.img, x: MARGIN + (CONTENT_W - w) / 2, y, w, h: fh });
      }
      y += h;
    });
    return pages;
  }
}

// ---------------------------------------------------------------- výkres

const DRAWING_CSS = `
.svg-label{font-family:Cambria,'Times New Roman',serif}
.pt-label{font-size:16px;font-weight:600;fill:${LIGHT.ink};font-family:system-ui,-apple-system,'Segoe UI',Roboto,Arial,sans-serif}
.ext{stroke:${LIGHT.ext};stroke-width:.8}
.dim{stroke:${LIGHT.ink};stroke-width:.9}
.dim-text{font-size:13px;fill:${LIGHT.ink};font-family:system-ui,-apple-system,'Segoe UI',Roboto,Arial,sans-serif}
.dim-note{font-size:11px;fill:${LIGHT['ink-soft']};font-family:system-ui,-apple-system,Arial,sans-serif}
text{font-family:system-ui,-apple-system,'Segoe UI',Roboto,Arial,sans-serif}
`;

/** Výkres úlohy jako obrázek (SVG z komponenty BeamDrawing, s barvami světlého tématu). */
async function drawingImage(props: DrawingProps): Promise<{ img: HTMLImageElement; ratio: number }> {
  const host = document.createElement('div');
  const root = createRoot(host);
  flushSync(() => root.render(<BeamDrawing {...props} compact={false} />));
  const svg = host.querySelector('svg')!;
  const vb = svg.getAttribute('viewBox')!.split(' ').map(Number);
  svg.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
  svg.setAttribute('width', String(vb[2] * 2));
  svg.setAttribute('height', String(vb[3] * 2));
  const style = document.createElementNS('http://www.w3.org/2000/svg', 'style');
  style.textContent = DRAWING_CSS;
  svg.insertBefore(style, svg.firstChild);
  let src = new XMLSerializer().serializeToString(svg);
  root.unmount();
  src = src.replace(/var\(--([a-z-]+)\)/g, (m, name: string) => LIGHT[name] ?? m);
  const img = new Image();
  img.decoding = 'sync';
  await new Promise<void>((resolve, reject) => {
    img.onload = () => resolve();
    img.onerror = () => reject(new Error('drawing'));
    img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(src);
  });
  return { img, ratio: vb[3] / vb[2] };
}

// ---------------------------------------------------------------- obsah

const arrowX = (v: number) => (v > 0 ? '→' : '←');
const arrowY = (v: number) => (v > 0 ? '↑' : '↓');

function symbolicEq(b: RichBuilder, terms: Term[]) {
  terms.forEach((x, i) => {
    b.append(x.c < 0 ? (i > 0 ? ' − ' : '−') : i > 0 ? ' + ' : '');
    b.sym(x.it.sym, x.it.unknownId ? LIGHT.react : undefined);
    if (x.arm !== null) b.append('·' + fmt(Math.abs(x.c)));
  });
  b.append(' = 0');
}

function numericEq(b: RichBuilder, terms: Term[]) {
  terms.forEach((x, i) => {
    b.append(x.c < 0 ? (i > 0 ? ' − ' : '−') : i > 0 ? ' + ' : '');
    if (x.it.unknownId) b.sym(x.it.sym, LIGHT.react);
    else b.append(fmt(x.it.value!));
    if (x.arm !== null) b.append('·' + fmt(Math.abs(x.c)));
  });
  b.append(' = 0');
}

const R = () => new RichBuilder();
const runsText = (runs: Run[]) => runs.map((r) => r.text).join('');

export { problemPdfName } from './pdfName';

async function solutionBlocks(d: Doc, p: Problem, tr: Tr) {
  const sol = fullSolution(p);
  const unknowns = unknownsOf(p);

  d.heading(tr('pdfSolution'), true);

  // ---------- 1. uvolnění ----------
  d.heading('1. ' + tr('s1Title'));
  for (const s of p.supports) {
    const b = R().bold(s.label).append(' — ' + tr(`name_${s.type}` as TKey) + ': ');
    unknownsOfSupport(s).forEach((u, i) => {
      if (i > 0) b.append(', ');
      b.sym(u.sym, LIGHT.react);
    });
    d.para(b.runs, 3, 10);
  }
  for (const h of p.hinges) d.para(R().bold(h.label).append(' — ' + tr('pdfHinge') + ': ').sym(`M_${h.label}`).append(' = 0').runs, 3, 10);
  d.para(R().rich(tr('s1Convention')).runs);
  d.para(R().rich(tr('count_h2', { n: unknowns.length, r: sol.eqs.length }) + ' ' + tr('count_ok', { n: unknowns.length })).runs, 4);

  // ---------- 2. zatížení ----------
  d.heading('2. ' + tr('s2Title'));
  const tasks = p.loads.filter((l) => l.kind === 'udl' || (l.kind === 'force' && !!l.alpha));
  if (!tasks.length) d.para([{ text: tr('pdfNoLoads') }]);
  for (const l of tasks) {
    if (l.kind === 'udl') {
      const len = l.x2 - l.x1;
      const q = resultantSym(l.label);
      d.para(
        R()
          .sym(q)
          .append(' = ')
          .sym(l.label)
          .append(` · l = ${fmt(l.q)} · ${fmt(len)} = `)
          .bold(`${fmt(l.q * len)} kN`)
          .append(' ↓ — ' + tr('pdfUdlPos', { a: fmt(len / 2) })).runs,
        3,
        10,
      );
    } else if (l.kind === 'force') {
      const { fx, fy } = forceComponents(l.F, l.angle);
      const a = l.alpha!;
      d.para(
        R()
          .sym(l.label + 'x')
          .append(' = ')
          .sym(l.label)
          .append(` · cos ${a}° = ${fmt(l.F)} · cos ${a}° = `)
          .bold(`${fmt(Math.abs(fx))} kN`)
          .append(' ' + arrowX(fx)).runs,
        3,
        10,
      );
      d.para(
        R()
          .sym(l.label + 'y')
          .append(' = ')
          .sym(l.label)
          .append(` · sin ${a}° = ${fmt(l.F)} · sin ${a}° = `)
          .bold(`${fmt(Math.abs(fy))} kN`)
          .append(' ' + arrowY(fy)).runs,
        2,
        10,
      );
    }
  }
  const released = new Set(p.supports.map((s) => s.id));
  const f2 = await drawingImage({ problem: p, released, resolved: new Set(tasks.map((l) => l.id)) });
  d.figure(f2.img, f2.ratio);

  // ---------- 3. rovnice ----------
  d.heading('3. ' + tr('s3Title'));
  d.para([{ text: tr('positive') + ': → ↑ ↺' }], 4, 0, true);
  sol.eqs.forEach((e, i) => {
    const head = R();
    head.with({ bold: true }, () => head.rich(eqTitle(e.spec)));
    if (e.spec.kind === 'hinge') head.append('  (' + tr('hingeSide').toLowerCase() + ': ' + tr(e.spec.side === 'left' ? 'left' : 'right') + ')');
    d.para(head.runs, 8, 0, true);
    const symbolic = R();
    symbolicEq(symbolic, sol.terms[i]);
    const numeric = R();
    numericEq(numeric, sol.terms[i]);
    // bez zatížení (např. ΣFx = 0 jen s reakcí) by byl číselný řádek stejný jako značkový
    const both = runsText(numeric.runs) !== runsText(symbolic.runs);
    d.para(symbolic.runs, 2, 10, both);
    if (both) d.para(numeric.runs, 1, 10);
  });

  // ---------- 4. výpočet ----------
  d.heading('4. ' + tr('s4Title'));
  for (const line of sol.lines) {
    const b = R();
    b.with({ color: LIGHT['ink-soft'] }, () => {
      b.append(tr('pdfFrom') + ' ');
      b.rich(eqTitle(sol.eqs[line.eq].spec));
      b.append(':  ');
    });
    b.sym(line.u.sym, LIGHT.react).append(' = ');
    if (line.expr !== fmt(line.value)) b.append(line.expr + ' = ');
    b.bold(fmt(line.value) + ' ' + unitOf(line.u));
    d.para(b.runs, 4);
  }
  if (sol.coupled.length) {
    const b = R().append(tr('pdfCoupled') + ' ');
    sol.coupled.forEach((u, i) => {
      if (i > 0) b.append(', ');
      b.sym(u.sym, LIGHT.react).append(' = ').bold(fmt(sol.values[u.id]) + ' ' + unitOf(u));
    });
    d.para(b.runs, 4);
  }

  // ---------- 5. výsledek ----------
  d.heading('5. ' + tr('pdfResult'));
  const res = R();
  unknowns.forEach((u, i) => {
    if (i > 0) res.append(',   ');
    res.sym(u.sym, LIGHT.react).append(' = ').bold(fmt(sol.values[u.id]) + ' ' + unitOf(u));
  });
  d.para(res.runs);
  for (const u of unknowns) {
    const v = sol.values[u.id];
    if (v < 0 && fmt(v) !== '0') d.note(R().rich(tr(u.kind === 'm' ? 'v_negM' : 'v_neg', { u: u.sym })).runs);
  }
  const f5 = await drawingImage({ problem: p, released, results: sol.values });
  d.figure(f5.img, f5.ratio);
  // kontrola k bodu, ke kterému v řešení ještě žádná momentová podmínka nebyla
  const usedPoints = new Set(sol.eqs.map((e) => (e.spec.kind === 'm' ? e.spec.point : null)).filter(Boolean));
  const pts = namedPoints(p);
  const point = pts.find((q) => !usedPoints.has(q.label)) ?? pts[pts.length - 1];
  const lin = linearize({ kind: 'm', point: point.label, x: point.x }, allItems(p));
  const residual = Object.entries(lin.a).reduce((acc, [k, v]) => acc + v * sol.values[k], lin.c);
  d.note(
    R()
      .append(tr('s5Check') + ': Σ')
      .sym(`M_${point.label}`)
      .append(` = ${fmt(residual, 3)} ≈ 0 ✓`).runs,
  );
}

/** Úloha jako PDF: zadání, s [withSolution] i celé řešení (uvolnění, zatížení, rovnice, výpočet, výsledek). */
export async function problemPdf(p: Problem, level: number, withSolution: boolean, tr: Tr): Promise<Blob> {
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(A4.w * S);
  canvas.height = Math.round(A4.h * S);
  const ctx = canvas.getContext('2d')!;
  const d = new Doc(ctx);

  // ---------- zadání ----------
  d.title(tr('problemNo') + ' ' + p.seed);
  d.subtitle(tr(`type_${p.type}` as TKey) + ' · ' + tr('level') + ': ' + tr(`level${level}` as TKey).toLowerCase() + ' · ' + tr('appTitle'));
  d.heading(tr('pdfTask'), true);
  d.para(R().rich(tr('pdfAssignment')).runs);
  const f1 = await drawingImage({ problem: p });
  d.figure(f1.img, f1.ratio);
  const given = R().append(tr('pdfGiven') + ': ');
  p.loads.forEach((l, i) => {
    if (i > 0) given.append(', ');
    given.sym(l.label);
    if (l.kind === 'force') {
      given.append(` = ${fmt(l.F)} kN`);
      if (l.alpha) given.append(` (α = ${l.alpha}°)`);
    } else if (l.kind === 'udl') given.append(` = ${fmt(l.q)} kN/m`);
    else given.append(` = ${fmt(l.M)} kNm`);
  });
  given.append('; ' + tr('lengthsNote') + '.');
  d.para(given.runs);

  if (withSolution) await solutionBlocks(d, p, tr);
  else d.note([{ text: tr('pdfTryApp', { n: p.seed }) }], 14);

  const pages = d.paginate();
  const footer = tr('appTitle') + ' · ' + tr('problemNo') + ' ' + p.seed + ' · ' + WEB_URL + '#' + p.seed;
  const out: PdfImagePage[] = [];
  for (const [i, placed] of pages.entries()) {
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.setTransform(S, 0, 0, S, 0, 0);
    for (const it of placed) {
      if (it.kind === 'text') drawLaid(ctx, it.laid, it.x, it.y, it.style.size, it.style.lineH, it.style.color);
      else ctx.drawImage(it.img, it.x, it.y, it.w, it.h);
    }
    const fy = A4.h - MARGIN + 6;
    ctx.strokeStyle = LIGHT.line;
    ctx.lineWidth = 0.6;
    ctx.beginPath();
    ctx.moveTo(MARGIN, fy - 6);
    ctx.lineTo(A4.w - MARGIN, fy - 6);
    ctx.stroke();
    const fl = layout(ctx, [{ text: footer }], CONTENT_W, SMALL.size, SMALL.lineH);
    drawLaid(ctx, fl, MARGIN, fy, SMALL.size, SMALL.lineH, SMALL.color);
    const num = `${i + 1} / ${pages.length}`;
    const nl = layout(ctx, [{ text: num }], CONTENT_W, SMALL.size, SMALL.lineH);
    const nw = nl.lines[0].pieces.reduce((s, x) => s + x.w, 0);
    drawLaid(ctx, nl, A4.w - MARGIN - nw, fy, SMALL.size, SMALL.lineH, SMALL.color);
    const blob = await new Promise<Blob>((resolve, reject) => canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('jpeg'))), 'image/jpeg', 0.9));
    out.push({ jpeg: new Uint8Array(await blob.arrayBuffer()), width: canvas.width, height: canvas.height });
  }
  const bytes = buildPdf(out, tr('problemNo') + ' ' + p.seed);
  return new Blob([bytes as BlobPart], { type: 'application/pdf' });
}

/** Stáhne PDF (na telefonu nabídne sdílení, je-li k dispozici). */
export async function savePdf(blob: Blob, name: string) {
  const file = new File([blob], name, { type: 'application/pdf' });
  const nav = navigator as Navigator & { canShare?: (d: ShareData) => boolean };
  const touch = window.matchMedia?.('(pointer: coarse)').matches;
  if (touch && nav.canShare?.({ files: [file] })) {
    try {
      await nav.share({ files: [file], title: name });
      return;
    } catch (e) {
      if ((e as Error).name === 'AbortError') return;
    }
  }
  const url = URL.createObjectURL(blob);
  const a = Object.assign(document.createElement('a'), { href: url, download: name });
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 30_000);
}
