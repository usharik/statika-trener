/* Společné pomůcky typů úloh (shodné s aplikací – practice/types/Common.kt). */
import { L } from '../../i18n';
import { fmt } from '../../format';
import { Trig } from '../trig';
import { TrapText, Traps } from '../traps';
import type { Learn } from '../exercise';

export { L, fmt, Trig, Traps, TrapText };
export { Fig } from '../fig';
export { exercise, exerciseType, field, opt } from '../exercise';

/** úhel ve stupních pro texty („35°“) */
export const deg = (a: number) => fmt(a, 0) + '°';
/** číslo pro texty (desetinná čárka, bez zbytečných nul) */
export const n = (v: number, d = 2) => fmt(v, d);

export type Triple = [number, number, number];
export const TRIPLES_EASY: Triple[] = [
  [3, 4, 5],
  [6, 8, 10],
  [5, 12, 13],
];
export const TRIPLES_MORE: Triple[] = [...TRIPLES_EASY, [8, 15, 17], [7, 24, 25], [9, 12, 15], [20, 21, 29]];

export type FnName = 'sin' | 'cos' | 'tg';
export const FNS: FnName[] = ['sin', 'cos', 'tg'];
export const Fn = {
  deg: (f: FnName, a: number) => (f === 'sin' ? Trig.sinD(a) : f === 'cos' ? Trig.cosD(a) : Trig.tgD(a)),
  rad: (f: FnName, a: number) => (f === 'sin' ? Trig.sinRad(a) : f === 'cos' ? Trig.cosRad(a) : Trig.tgRad(a)),
  grad: (f: FnName, a: number) => (f === 'sin' ? Trig.sinGrad(a) : f === 'cos' ? Trig.cosGrad(a) : Trig.sinGrad(a) / Trig.cosGrad(a)),
  inv: (f: FnName, v: number) => (f === 'sin' ? Trig.asinD(v) : f === 'cos' ? Trig.acosD(v) : Trig.atanD(v)),
  calcInv: (f: FnName) => (f === 'sin' ? 'sin⁻¹' : f === 'cos' ? 'cos⁻¹' : 'tan⁻¹'),
  arc: (f: FnName) => (f === 'sin' ? 'arcsin' : f === 'cos' ? 'arccos' : 'arctg'),
};

/** „skoro stejné“ pro malá čísla – relativně */
export const nearRel = (v: number | null | undefined, w: number) => v != null && Math.abs(v - w) <= Math.max(0.004, Math.abs(w) * 0.02);
/** stejná tolerance jako u sil (1 % / 0,02) */
export const close2 = (v: number | null | undefined, w: number) => v != null && Math.abs(v - w) <= Math.max(0.02, Math.abs(w) * 0.01);
export const absN = (v: number | null | undefined) => (v == null ? null : Math.abs(v));

/** „= 0,5“, nebo „≈ 0,385“, když se číslo při zaokrouhlení na d míst změní */
export function approx(v: number, d = 2): string {
  const k = 10 ** d;
  return Math.abs(v * k - Math.round(v * k)) < 1e-6 ? `= ${n(v, d)}` : `≈ ${n(v, d)}`;
}

/** „(sin 30° = 0,5; cos 30° ≈ 0,866)“ – hodnoty do zadání pro úroveň bez kalkulačky */
export function given(a: number): L {
  const s = Trig.sinD(a);
  const c = Trig.cosD(a);
  const eq = (v: number) => (Math.abs(v * 1000 - Math.round(v * 1000)) < 1e-9 ? `= ${n(v, 3)}` : `≈ ${n(v, 3)}`);
  const t = ` (sin ${deg(a)} ${eq(s)}; cos ${deg(a)} ${eq(c)})`;
  return L(t, t);
}

/** „To je teprve podíl stran“ – u malých úhlů dá stejné číslo i kalkulačka v RAD */
export function ratioOrRad(q: number, ans: number, fn: FnName): TrapText {
  const radLike = nearRel(q, (ans * Math.PI) / 180);
  if (!radLike) return Traps.RATIO;
  const key = fn === 'tg' ? 'tan' : fn;
  return Traps.RATIO.with(
    L(
      `To je teprve podíl stran (${fn} α) – nebo je kalkulačka v RAD. Úhel dostaneš přes ${key}⁻¹ v režimu DEG.`,
      `Это пока отношение сторон (${fn} α) — или калькулятор в RAD. Угол получишь через ${key}⁻¹ в режиме DEG.`,
    ),
  );
}

/** kde najít goniometrické funkce (do první nápovědy úloh s kalkulačkou) */
export const PHONE_CALC = L(
  ' Na telefonu: otoč Kalkulačku na šířku (iPhone) nebo rozbal panel funkcí (Android) – objeví se sin, cos, tan a přepínač Deg/Rad; tan⁻¹ je pod 2nd (iPhone) nebo INV (Android).',
  ' На телефоне: поверни Калькулятор горизонтально (iPhone) или разверни панель функций (Android) — появятся sin, cos, tan и переключатель Deg/Rad; tan⁻¹ — под 2nd (iPhone) или INV (Android).',
);

/** odkazy na karty kurzu trigonometrie */
export const Cards: Record<string, Learn> = {
  NAME_SIDES: [101, 2],
  THREE_RATIOS: [102, 0],
  BUILD: [102, 1],
  CHECK345: [102, 2],
  LADDER: [102, 4],
  CALC_MODE: [103, 1],
  SQRT_TABLE: [104, 2],
  HALF_TRI: [104, 1],
  PYTHAGORAS: [105, 0],
  UNIT_HYP: [105, 1],
  TANGENT: [105, 2],
  COMPLEMENT: [105, 3],
  BACK: [106, 0],
  WHICH_INV: [106, 1],
  SLOPE_RATIO: [106, 3],
  SLOPE_PCT: [106, 4],
  UNIT_CIRCLE: [107, 0],
  QUADRANTS: [107, 1],
  SIGNS: [107, 2],
  FROM_VERT: [108, 0],
  RESULTANT: [108, 1],
  SUM: [108, 2],
  ARM: [108, 3],
  RAFTER: [108, 4],
};
