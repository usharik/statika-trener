/**
 * Minimální zápis PDF: každá stránka je jeden obrázek JPEG přes celou stránku A4 (stejně jako PDF na iOS).
 * Bez knihoven – PDF z obrázků je jednoduchý formát (objekty, tabulka xref, trailer).
 */

export interface PdfImagePage {
  /** JPEG */
  jpeg: Uint8Array;
  width: number;
  height: number;
}

export const A4 = { w: 595.28, h: 841.89 };

const enc = new TextEncoder();

/** Text do PDF řetězce (UTF-16BE s BOM, aby prošla i diakritika a azbuka). */
function pdfString(s: string): string {
  let hex = 'FEFF';
  for (let i = 0; i < s.length; i++) hex += s.charCodeAt(i).toString(16).padStart(4, '0').toUpperCase();
  return `<${hex}>`;
}

export function buildPdf(pages: PdfImagePage[], title: string): Uint8Array {
  const chunks: Uint8Array[] = [];
  let size = 0;
  const offsets: number[] = [];
  const push = (b: Uint8Array | string) => {
    const u = typeof b === 'string' ? enc.encode(b) : b;
    chunks.push(u);
    size += u.length;
  };
  const n = pages.length;
  // čísla objektů: 1 katalog, 2 stránky, 3 info, pak pro každou stránku: stránka, obrázek, obsah
  const pageObj = (i: number) => 4 + i * 3;
  const total = 3 + n * 3;
  const obj = (num: number, body: string) => {
    offsets[num] = size;
    push(`${num} 0 obj\n${body}\nendobj\n`);
  };

  push('%PDF-1.4\n%âãÏÓ\n');
  obj(1, '<< /Type /Catalog /Pages 2 0 R >>');
  obj(2, `<< /Type /Pages /Kids [${pages.map((_, i) => `${pageObj(i)} 0 R`).join(' ')}] /Count ${n} >>`);
  obj(3, `<< /Title ${pdfString(title)} /Producer ${pdfString('Statika · nosníky')} >>`);
  pages.forEach((p, i) => {
    const po = pageObj(i);
    const W = A4.w.toFixed(2);
    const H = A4.h.toFixed(2);
    obj(po, `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${W} ${H}] /Resources << /XObject << /Im0 ${po + 1} 0 R >> >> /Contents ${po + 2} 0 R >>`);
    offsets[po + 1] = size;
    push(`${po + 1} 0 obj\n<< /Type /XObject /Subtype /Image /Width ${p.width} /Height ${p.height} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${p.jpeg.length} >>\nstream\n`);
    push(p.jpeg);
    push('\nendstream\nendobj\n');
    const content = `q ${W} 0 0 ${H} 0 0 cm /Im0 Do Q`;
    obj(po + 2, `<< /Length ${content.length} >>\nstream\n${content}\nendstream`);
  });
  const xref = size;
  let x = `xref\n0 ${total + 1}\n0000000000 65535 f \n`;
  for (let i = 1; i <= total; i++) x += String(offsets[i]).padStart(10, '0') + ' 00000 n \n';
  push(x);
  push(`trailer\n<< /Size ${total + 1} /Root 1 0 R /Info 3 0 R >>\nstartxref\n${xref}\n%%EOF\n`);

  const out = new Uint8Array(size);
  let o = 0;
  for (const c of chunks) {
    out.set(c, o);
    o += c.length;
  }
  return out;
}
