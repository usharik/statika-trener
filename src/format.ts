/** Číslo s desetinnou čárkou, max. `d` desetinných míst, bez zbytečných nul. */
export function fmt(v: number, d = 2): string {
  if (Math.abs(v) < 0.5 * 10 ** -d) v = 0;
  const s = v.toFixed(d).replace(/\.?0+$/, '');
  return s.replace('.', ',').replace('-', '−');
}

/** Tolerantní čtení čísla: čárka i tečka, různé znaky minus, mezery. */
export function parseNum(s: string): number | null {
  const t = s.trim().replace(/\s/g, '').replace(',', '.').replace(/[−–]/g, '-');
  if (t === '' || t === '-' || t === '.') return null;
  const v = Number(t);
  return Number.isFinite(v) ? v : null;
}

export const close = (a: number, b: number) => Math.abs(a - b) <= Math.max(0.02, Math.abs(b) * 0.01);
