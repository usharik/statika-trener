import { closeTyped, parseNum } from '../format';

/**
 * Jak přísně kontrolovat číslo. Jedna logika pro karty výkladu (NumAnswer) i procvičování.
 * - value: délka, síla, moment – closeTyped (1 % / 0,02, nebo správné zaokrouhlení na zapsaná místa)
 * - angle: úhel ve stupních ±0,5°, u malých úhlů přísněji (3 % úhlu, aspoň ±0,1°)
 * - ratio: hodnota sin / cos / tg ±0,006 (0,87 i 0,866 pro √3/2); u velkých tangent 0,5 %
 * - percent: sklon v % ±0,5 (nad 50 % ±1 %)
 * - rel: malé délky – jen 1 % (bez spodní hranice 0,02), nebo správné zaokrouhlení na aspoň 2 místa
 */
export type Tol = 'value' | 'angle' | 'ratio' | 'percent' | 'rel';

/** tolerance úhlu: ±0,5°, u malých úhlů 3 % (aspoň ±0,1°) */
export const angleTol = (ans: number) => Math.min(0.5, Math.max(0.1, Math.abs(ans) * 0.03));

/**
 * Je zapsaná hodnota [v] (text [input]) správná odpověď [ans]? [scale] = velikost, ke které se vztahuje 1 % u value.
 */
export function accept(input: string, v: number, ans: number, tol: Tol, scale?: number | null): boolean {
  switch (tol) {
    case 'value':
      return closeTyped(input, v, ans) || (scale != null && Math.abs(v - ans) <= Math.max(0.02, Math.abs(scale) * 0.01) + 1e-9);
    case 'rel': {
      if (Math.abs(v - ans) <= Math.abs(ans) * 0.01 + 1e-9) return true;
      const t = input.trim().replace(',', '.');
      const dot = t.indexOf('.');
      const k = dot < 0 ? 0 : t.length - dot - 1;
      return k >= 2 && k <= 6 && Math.abs(v - ans) <= 0.5 / 10 ** k + 1e-9;
    }
    case 'angle':
      return Math.abs(v - ans) <= angleTol(ans) + 1e-9;
    case 'ratio':
      return Math.abs(v - ans) <= Math.max(0.006, Math.abs(ans) * 0.005) + 1e-9;
    case 'percent':
      return Math.abs(v - ans) <= (Math.abs(ans) > 50 ? Math.abs(ans) * 0.01 : 0.5) + 1e-9;
  }
}

/** Hodnota je „skoro“ správně (do 5 %), ale mimo toleranci – typicky zaokrouhlení mezivýsledku. */
export const near = (v: number, ans: number) => Math.abs(v - ans) <= Math.max(0.05, Math.abs(ans) * 0.05);

/** Číslo nebo zlomek („3/5“, „3 : 5“) – podíl stran se dá zapsat i jako zlomek. */
export function parseRatio(s: string): number | null {
  const t = s.trim();
  const sep = [...t].findIndex((ch) => ch === '/' || ch === ':');
  if (sep < 0) return parseNum(t);
  const a = parseNum(t.slice(0, sep));
  const b = parseNum(t.slice(sep + 1));
  if (a === null || b === null || b === 0) return null;
  return a / b;
}
