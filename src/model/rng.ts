/** Deterministický generátor (mulberry32) – stejné číslo úlohy = stejná úloha. */
export class Rng {
  private s: number;
  constructor(seed: number) {
    // promíchání, aby sousední čísla úloh dávala nesouvisející úlohy
    let h = Math.imul(seed ^ 0x9e3779b9, 0x85ebca6b);
    h = Math.imul(h ^ (h >>> 13), 0xc2b2ae35);
    this.s = (h ^ (h >>> 16)) >>> 0;
  }
  next(): number {
    this.s = (this.s + 0x6d2b79f5) >>> 0;
    let t = this.s;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  }
  int(min: number, max: number): number {
    return min + Math.floor(this.next() * (max - min + 1));
  }
  pick<T>(arr: readonly T[]): T {
    return arr[Math.floor(this.next() * arr.length)];
  }
  chance(p: number): boolean {
    return this.next() < p;
  }
  /** násobek kroku step v intervalu <min, max> */
  grid(min: number, max: number, step: number): number {
    const n = Math.floor((max - min) / step + 1e-9);
    return round(min + this.int(0, n) * step);
  }
  shuffle<T>(arr: T[]): T[] {
    const a = [...arr];
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(this.next() * (i + 1));
      [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
  }
}

export const round = (v: number, d = 6) => Math.round(v * 10 ** d) / 10 ** d;

export const randomSeed = () => 1 + Math.floor(Math.random() * 999_999);
