/**
 * Goniometrické funkce ve stupních s přesnými hodnotami pro „známé“ úhly (sin 30° = 0,5 přesně, ne 0,49999…).
 * Žák počítá s 0,5 – a kontrola i texty (fmt) musí vidět totéž číslo. Jen pro výklad a procvičování
 * (model trenažéru má vlastní tabulku shodnou s generátorem).
 */
const S3 = Math.sqrt(3) / 2;
const S2 = Math.sqrt(2) / 2;

function exactSin(deg: number): number | null {
  switch (deg) {
    case 0:
      return 0;
    case 30:
      return 0.5;
    case 45:
      return S2;
    case 60:
      return S3;
    case 90:
      return 1;
    default:
      return null;
  }
}

export const Trig = {
  /** sin úhlu ve stupních (libovolný úhel; násobky 30° a 45° přesně) */
  sinD(deg: number): number {
    if (deg === Math.floor(deg)) {
      const d = ((deg % 360) + 360) % 360;
      const [ref, sign] = d <= 90 ? [d, 1] : d <= 180 ? [180 - d, 1] : d <= 270 ? [d - 180, -1] : [360 - d, -1];
      const e = exactSin(ref);
      if (e !== null) return e === 0 ? 0 : sign * e;
    }
    return Math.sin((deg * Math.PI) / 180);
  },
  cosD(deg: number): number {
    return Trig.sinD(90 - deg);
  },
  /** tangens; pro 90° (+k·180°) vrací NaN – „neexistuje“ */
  tgD(deg: number): number {
    const c = Trig.cosD(deg);
    if (Math.abs(c) < 1e-12) return NaN;
    if (deg === Math.floor(deg) && ((deg % 180) + 180) % 180 === 45) return Trig.sinD(deg) * c > 0 ? 1 : -1;
    return Trig.sinD(deg) / c;
  },
  asinD: (v: number) => (Math.asin(Math.min(1, Math.max(-1, v))) * 180) / Math.PI,
  acosD: (v: number) => (Math.acos(Math.min(1, Math.max(-1, v))) * 180) / Math.PI,
  atanD: (v: number) => (Math.atan(v) * 180) / Math.PI,
  /** co by ukázala kalkulačka v režimu RAD (číslo ve stupních bere jako radiány) */
  sinRad: (deg: number) => Math.sin(deg),
  cosRad: (deg: number) => Math.cos(deg),
  tgRad: (deg: number) => Math.tan(deg),
  /** co by ukázala kalkulačka v režimu GRAD (pravý úhel = 100 gradů) */
  sinGrad: (deg: number) => Math.sin((deg * Math.PI) / 200),
  cosGrad: (deg: number) => Math.cos((deg * Math.PI) / 200),
};
