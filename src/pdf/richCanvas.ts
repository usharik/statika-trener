/*
 * Sazba bohatého textu (tučně, značky se spodním indexem, barvy) na plátno – pro PDF.
 */

export interface Run {
  text: string;
  bold?: boolean;
  italic?: boolean;
  /** spodní index */
  sub?: boolean;
  color?: string;
  serif?: boolean;
}

/** Skladání textu po kouscích (obdoba AnnotatedString.Builder v aplikaci). */
export class RichBuilder {
  runs: Run[] = [];
  private stack: Partial<Run>[] = [];

  private style(): Partial<Run> {
    return Object.assign({}, ...this.stack);
  }

  append(text: string, st: Partial<Run> = {}) {
    if (text) this.runs.push({ ...this.style(), ...st, text });
    return this;
  }

  with(st: Partial<Run>, f: () => void) {
    this.stack.push(st);
    f();
    this.stack.pop();
    return this;
  }

  bold(text: string) {
    return this.append(text, { bold: true });
  }

  /** Značka „R_Ax“ → kurzívou R se spodním indexem Ax. */
  sym(s: string, color?: string) {
    const i = s.indexOf('_');
    const main = i < 0 ? s : s.slice(0, i);
    const sub = i < 0 ? '' : s.slice(i + 1);
    this.append(main, { italic: true, serif: true, ...(color ? { color } : {}) });
    if (sub) this.append(sub, { sub: true, serif: true, italic: false, ...(color ? { color } : {}) });
    return this;
  }

  /** Text s tokeny [[značka]] a **tučně**. */
  rich(text: string) {
    const re = /\[\[[^\]]+\]\]|\*\*[^*]+\*\*/g;
    let pos = 0;
    for (const m of text.matchAll(re)) {
      this.append(text.slice(pos, m.index));
      const v = m[0];
      if (v.startsWith('[[')) this.sym(v.slice(2, -2));
      else this.with({ bold: true }, () => this.rich(v.slice(2, -2)));
      pos = m.index! + v.length;
    }
    this.append(text.slice(pos));
    return this;
  }
}

export const richText = (text: string) => new RichBuilder().rich(text).runs;

const SANS = "system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif";
const SERIF = "Cambria, 'Times New Roman', Times, serif";

export function fontOf(r: Run, size: number): string {
  const s = r.sub ? size * 0.75 : size;
  return `${r.italic ? 'italic ' : ''}${r.bold ? '600 ' : ''}${s}px ${r.serif ? SERIF : SANS}`;
}

interface Piece {
  run: Run;
  text: string;
  w: number;
}

export interface Laid {
  lines: { pieces: (Piece & { x: number })[]; y: number }[];
  height: number;
}

/** Zalomení do řádků šířky [maxW]; [size] a [lineH] v jednotkách plátna. */
export function layout(ctx: CanvasRenderingContext2D, runs: Run[], maxW: number, size: number, lineH: number): Laid {
  // slova = nepřerušené úseky bez mezer (i přes hranice úseků stylu – značka s indexem se nerozdělí)
  const words: { pieces: Piece[]; space: number }[] = [];
  let cur: Piece[] = [];
  const flush = (space: number) => {
    words.push({ pieces: cur, space });
    cur = [];
  };
  for (const r of runs) {
    ctx.font = fontOf(r, size);
    const spaceW = ctx.measureText(' ').width;
    const parts = r.text.split(/( +)/);
    for (const part of parts) {
      if (!part) continue;
      if (/^ +$/.test(part)) {
        flush(spaceW * part.length);
        continue;
      }
      cur.push({ run: r, text: part, w: ctx.measureText(part).width });
    }
  }
  flush(0);

  const lines: Laid['lines'] = [];
  let line: (Piece & { x: number })[] = [];
  let x = 0;
  let pendingSpace = 0;
  const newLine = () => {
    lines.push({ pieces: line, y: lines.length * lineH });
    line = [];
    x = 0;
    pendingSpace = 0;
  };
  for (const w of words) {
    const ww = w.pieces.reduce((s, p) => s + p.w, 0);
    if (line.length && x + pendingSpace + ww > maxW) newLine();
    else x += pendingSpace;
    for (const p of w.pieces) {
      line.push({ ...p, x });
      x += p.w;
    }
    pendingSpace = w.space;
  }
  if (line.length || !lines.length) newLine();
  return { lines, height: lines.length * lineH };
}

export function drawLaid(ctx: CanvasRenderingContext2D, laid: Laid, x0: number, y0: number, size: number, lineH: number, color: string) {
  ctx.textBaseline = 'alphabetic';
  for (const ln of laid.lines) {
    const base = y0 + ln.y + (lineH + size * 0.72) / 2;
    for (const p of ln.pieces) {
      ctx.font = fontOf(p.run, size);
      ctx.fillStyle = p.run.color ?? color;
      ctx.fillText(p.text, x0 + p.x, base + (p.run.sub ? size * 0.22 : 0));
    }
  }
}
