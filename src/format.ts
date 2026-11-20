/*
 * Čísla: zobrazení s desetinnou čárkou a tolerantní čtení zápisu žáka. Shodné s mobilní aplikací (model/Format.kt).
 */

const POW10 = [1, 10, 100, 1000, 10000, 100000, 1000000];

/** Číslo bez zbytečných nul s desetinnou tečkou (např. 4.5, 2). */
export function plain(v: number, d = 3): string {
  if (!Number.isFinite(v)) return String(v);
  const s = v.toFixed(d);
  const r = s.includes('.') ? s.replace(/0+$/, '').replace(/\.$/, '') : s;
  return r === '-0' ? '0' : r;
}

/** Číslo s desetinnou čárkou, max. `d` desetinných míst, bez zbytečných nul. */
export function fmt(value: number, d = 2): string {
  const v = Math.abs(value) < 0.5 / POW10[d] ? 0 : value;
  return plain(v, d).replace('.', ',').replace('-', '−');
}

const NUM = /^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?$/;

/** Tolerantní čtení čísla: čárka i tečka, různé znaky minus, mezery. */
export function parseNum(s: string): number | null {
  const t = s.trim().replace(/\s/g, '').replace(',', '.').replace(/[−–]/g, '-');
  if (t === '' || t === '-' || t === '.') return null;
  if (!NUM.test(t)) return null;
  const v = Number(t);
  return Number.isFinite(v) ? v : null;
}

export const close = (a: number, b: number) => Math.abs(a - b) <= Math.max(0.02, Math.abs(b) * 0.01);

/**
 * Kontrola zadaného čísla: v toleranci [close], nebo správně zaokrouhlené na tolik desetinných míst,
 * kolik jich žák napsal (3,2 pro 3,167 je v pořádku – 3,2 je správné zaokrouhlení).
 */
export function closeTyped(input: string, v: number, ans: number): boolean {
  if (close(v, ans)) return true;
  const t = input.trim().replace(',', '.');
  const dot = t.indexOf('.');
  const k = dot < 0 ? 0 : t.length - dot - 1;
  if (k < 1 || k > 6) return false;
  return Math.abs(v - ans) <= 0.5 / POW10[k] + 1e-9;
}

/** Přepne znaménko zapsaného čísla („3,5“ ↔ „−3,5“); minus pro klávesnice, které ho nemají. */
export function toggleSign(value: string): string {
  const t = value.trimStart();
  return /^[−\-–]/.test(t) ? t.slice(1) : '−' + t;
}
