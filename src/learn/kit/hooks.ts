/*
 * Obdoba stavu a korutin z Compose pro karty výkladu (aby šly přepsat z aplikace věrně):
 *  - useMut = remember { mutableStateOf(…) } – hodnota se čte a mění přes .v, změna komponentu překreslí,
 *    a asynchronní kód vždy čte aktuální hodnotu (žádné „zastaralé“ uzávěry)
 *  - useAnim = remember { Animatable(…) } – animateTo / snapTo jako v Compose
 *  - useScope = rememberCoroutineScope(); launch, delay; po odchodu z karty se vše zruší
 *  - useLaunched = LaunchedEffect(klíče) { … }
 */
import { useEffect, useReducer, useRef } from 'react';

/** Výjimka zrušení (odchod z karty, nová animace téže hodnoty, nový běh LaunchedEffect). */
export class Cancelled extends Error {
  constructor() {
    super('cancelled');
  }
}

const isCancel = (e: unknown) => e instanceof Cancelled;

/** Vlastník stavu: jedna komponenta (karta). */
class Owner {
  alive = true;
  rerender: () => void;
  private pending = false;
  constructor(rerender: () => void) {
    this.rerender = rerender;
  }
  /** překreslit nejvýš jednou za snímek */
  invalidate() {
    if (!this.alive || this.pending) return;
    this.pending = true;
    queueMicrotask(() => {
      this.pending = false;
      if (this.alive) this.rerender();
    });
  }
}

function useOwner(): Owner {
  const [, force] = useReducer((x: number) => x + 1, 0);
  const ref = useRef<Owner | null>(null);
  if (!ref.current) ref.current = new Owner(force);
  ref.current.rerender = force;
  useEffect(() => {
    const o = ref.current!;
    o.alive = true;
    return () => {
      o.alive = false;
    };
  }, []);
  return ref.current;
}

export class Mut<T> {
  private val: T;
  private owner: Owner;
  constructor(v: T, owner: Owner) {
    this.val = v;
    this.owner = owner;
  }
  get v(): T {
    return this.val;
  }
  set v(x: T) {
    if (Object.is(x, this.val)) return;
    this.val = x;
    this.owner.invalidate();
  }
}

/** Seznam se změnami po prvcích (mutableStateListOf). */
export class MutList<T> {
  private arr: T[];
  private owner: Owner;
  constructor(v: T[], owner: Owner) {
    this.arr = [...v];
    this.owner = owner;
  }
  get v(): readonly T[] {
    return this.arr;
  }
  get size() {
    return this.arr.length;
  }
  get(i: number): T {
    return this.arr[i];
  }
  set(i: number, x: T) {
    if (Object.is(this.arr[i], x)) return;
    this.arr = [...this.arr];
    this.arr[i] = x;
    this.owner.invalidate();
  }
  setAll(v: T[]) {
    this.arr = [...v];
    this.owner.invalidate();
  }
  add(x: T) {
    this.arr = [...this.arr, x];
    this.owner.invalidate();
  }
  clear() {
    this.arr = [];
    this.owner.invalidate();
  }
  indexOfFirst(f: (x: T) => boolean) {
    return this.arr.findIndex(f);
  }
  all(f: (x: T) => boolean) {
    return this.arr.every(f);
  }
  count(f: (x: T) => boolean) {
    return this.arr.filter(f).length;
  }
}

// ------------------------------------------------------------- časování

export type Easing = (f: number) => number;

function bezier(x1: number, y1: number, x2: number, y2: number): Easing {
  const cx = 3 * x1,
    bx = 3 * (x2 - x1) - cx,
    ax = 1 - cx - bx;
  const cy = 3 * y1,
    by = 3 * (y2 - y1) - cy,
    ay = 1 - cy - by;
  const sx = (t: number) => ((ax * t + bx) * t + cx) * t;
  const sy = (t: number) => ((ay * t + by) * t + cy) * t;
  const dx = (t: number) => (3 * ax * t + 2 * bx) * t + cx;
  return (x) => {
    if (x <= 0) return 0;
    if (x >= 1) return 1;
    let t = x;
    for (let i = 0; i < 8; i++) {
      const e = sx(t) - x;
      const d = dx(t);
      if (Math.abs(e) < 1e-5 || Math.abs(d) < 1e-6) break;
      t -= e / d;
    }
    t = Math.min(1, Math.max(0, t));
    return sy(t);
  };
}

export const FastOutSlowInEasing = bezier(0.4, 0, 0.2, 1);
export const LinearOutSlowInEasing = bezier(0, 0, 0.2, 1);
export const FastOutLinearInEasing = bezier(0.4, 0, 1, 1);
export const LinearEasing: Easing = (f) => f;
export const EaseInOut = bezier(0.42, 0, 0.58, 1);
export const EaseOut = bezier(0, 0, 0.58, 1);
export const EaseIn = bezier(0.42, 0, 1, 1);

export interface AnimSpec {
  ms: number;
  easing: Easing;
  delay?: number;
  /** fyzikální pružina (spring(dampingRatio, stiffness) z Compose) – pak se ms a easing nepoužijí */
  spring?: { damping: number; stiffness: number };
}

/** tween(ms, easing) z Compose; výchozí easing FastOutSlowIn. */
export const tween = (ms = 300, easing: Easing = FastOutSlowInEasing, delay = 0): AnimSpec => ({ ms, easing, delay });

/** Pružina jako spring(dampingRatio, stiffness) v Compose (výchozí: bez překmitu, StiffnessMedium). */
export const spring = (dampingRatio = 1, stiffness = 1500): AnimSpec => ({ ms: 0, easing: LinearEasing, spring: { damping: dampingRatio, stiffness } });

const prefersReduced = () => typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;

/** Animovaná hodnota (Animatable). */
export class Anim {
  private val: number;
  target: number;
  private owner: Owner;
  private token = 0;
  /** rychlost (jednotek za sekundu) – navazuje na ni další pružina */
  velocity = 0;
  private reject: ((e: unknown) => void) | null = null;
  isRunning = false;
  constructor(v: number, owner: Owner) {
    this.val = v;
    this.target = v;
    this.owner = owner;
  }
  get value() {
    return this.val;
  }
  private cancelRunning() {
    this.token++;
    if (this.reject) {
      const r = this.reject;
      this.reject = null;
      r(new Cancelled());
    }
    this.isRunning = false;
  }
  async snapTo(v: number) {
    this.cancelRunning();
    this.val = v;
    this.target = v;
    this.owner.invalidate();
  }
  /** Animace k [target]; předchozí animace téže hodnoty se zruší (její await vyhodí Cancelled). */
  animateTo(target: number, spec: AnimSpec = tween()): Promise<void> {
    this.cancelRunning();
    if (!this.owner.alive) return Promise.reject(new Cancelled());
    const my = ++this.token;
    const from = this.val;
    this.target = target;
    const ms = prefersReduced() ? Math.min(spec.ms, 1) : spec.ms;
    this.isRunning = true;
    if (spec.spring && !prefersReduced()) return this.runSpring(target, spec.spring, my);
    this.velocity = 0;
    return new Promise<void>((resolve, reject) => {
      this.reject = reject;
      let t0: number | null = null;
      const frame = (now: number) => {
        if (my !== this.token) return;
        if (!this.owner.alive) {
          this.reject = null;
          this.isRunning = false;
          reject(new Cancelled());
          return;
        }
        if (t0 === null) t0 = now + (spec.delay ?? 0);
        const f = ms <= 0 ? 1 : Math.min(1, Math.max(0, (now - t0) / ms));
        this.val = from + (target - from) * spec.easing(f);
        this.owner.invalidate();
        if (f >= 1) {
          this.val = target;
          this.reject = null;
          this.isRunning = false;
          resolve();
        } else requestAnimationFrame(frame);
      };
      requestAnimationFrame(frame);
    });
  }
  private runSpring(target: number, sp: { damping: number; stiffness: number }, my: number): Promise<void> {
    const k = sp.stiffness;
    const c = 2 * sp.damping * Math.sqrt(k);
    const range = Math.max(Math.abs(target - this.val), 1e-3);
    return new Promise<void>((resolve, reject) => {
      this.reject = reject;
      let last: number | null = null;
      const frame = (now: number) => {
        if (my !== this.token) return;
        if (!this.owner.alive) {
          this.reject = null;
          this.isRunning = false;
          reject(new Cancelled());
          return;
        }
        const dt = last === null ? 1 / 60 : Math.min(0.05, (now - last) / 1000);
        last = now;
        // několik menších kroků pro stabilitu
        const n = 4;
        for (let i = 0; i < n; i++) {
          const h = dt / n;
          const a = -k * (this.val - target) - c * this.velocity;
          this.velocity += a * h;
          this.val += this.velocity * h;
        }
        this.owner.invalidate();
        if (Math.abs(this.val - target) < Math.min(0.01, range * 0.01) && Math.abs(this.velocity) < Math.min(0.1, range * 0.1)) {
          this.val = target;
          this.velocity = 0;
          this.reject = null;
          this.isRunning = false;
          resolve();
        } else requestAnimationFrame(frame);
      };
      requestAnimationFrame(frame);
    });
  }

  stop() {
    this.cancelRunning();
  }
}

export interface Scope {
  /** spustí asynchronní práci (vrátí Job, který jde zrušit); zrušení (i odchod z karty) se tiše spolkne */
  launch(fn: (job: Job) => Promise<unknown>): Job;
  /** čekání; po odchodu z karty vyhodí Cancelled */
  delay(ms: number): Promise<void>;
  /** další snímek (withFrameNanos) – vrátí čas v ms */
  frame(): Promise<number>;
  readonly alive: boolean;
}

function makeScope(owner: Owner): Scope {
  return {
    launch(fn) {
      const job = new Job(owner);
      fn(job).catch((e) => {
        if (!isCancel(e)) console.error(e);
      });
      return job;
    },
    delay(ms) {
      return new Promise((resolve, reject) => {
        if (!owner.alive) return reject(new Cancelled());
        setTimeout(() => (owner.alive ? resolve() : reject(new Cancelled())), prefersReduced() ? Math.min(ms, 50) : ms);
      });
    },
    frame() {
      return new Promise((resolve, reject) => {
        if (!owner.alive) return reject(new Cancelled());
        requestAnimationFrame((t) => (owner.alive ? resolve(t) : reject(new Cancelled())));
      });
    },
    get alive() {
      return owner.alive;
    },
  };
}

/** Kontext jedné karty: stav, animace a korutiny. Volat jednou na začátku komponenty karty. */
export function useKit() {
  const owner = useOwner();
  const ref = useRef<{ scope: Scope; idx: number; slots: unknown[] } | null>(null);
  if (!ref.current) ref.current = { scope: makeScope(owner), idx: 0, slots: [] };
  const box = ref.current;
  box.idx = 0;
  const slot = <T,>(make: () => T): T => {
    const i = box.idx++;
    if (i >= box.slots.length) box.slots.push(make());
    return box.slots[i] as T;
  };
  return {
    scope: box.scope,
    /** remember { mutableStateOf(init) } */
    mut<T>(init: T | (() => T)): Mut<T> {
      return slot(() => new Mut<T>(typeof init === 'function' ? (init as () => T)() : init, owner));
    },
    /** remember { mutableStateListOf(…) } */
    list<T>(init: T[]): MutList<T> {
      return slot(() => new MutList<T>(init, owner));
    },
    /** remember { Animatable(init) } */
    anim(init: number): Anim {
      return slot(() => new Anim(init, owner));
    },
    /** remember { … } – hodnota spočítaná jednou */
    remember<T>(make: () => T): T {
      return slot(make);
    },
  };
}

export type Kit = ReturnType<typeof useKit>;

/**
 * LaunchedEffect(klíče) { … }: při první zobrazení a při každé změně klíčů spustí [fn];
 * předchozí běh se zruší (job.delay / job.active).
 */
export function useLaunched(keys: unknown[], fn: (job: Job) => Promise<unknown>) {
  const ref = useRef(fn);
  ref.current = fn;
  useEffect(() => {
    const job = new Job();
    ref.current(job).catch((e) => {
      if (!isCancel(e)) console.error(e);
    });
    return () => job.cancel();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, keys);
}

export class Job {
  active = true;
  private owner: Owner | null;
  private timers = new Set<ReturnType<typeof setTimeout>>();
  private rejects = new Set<(e: unknown) => void>();
  constructor(owner: Owner | null = null) {
    this.owner = owner;
  }
  private get live() {
    return this.active && (this.owner?.alive ?? true);
  }
  cancel() {
    this.active = false;
    for (const t of this.timers) clearTimeout(t);
    for (const r of this.rejects) r(new Cancelled());
    this.timers.clear();
    this.rejects.clear();
  }
  delay(ms: number): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.live) return reject(new Cancelled());
      const t = setTimeout(
        () => {
          this.timers.delete(t);
          this.rejects.delete(reject);
          if (this.live) resolve();
          else reject(new Cancelled());
        },
        prefersReduced() ? Math.min(ms, 50) : ms,
      );
      this.timers.add(t);
      this.rejects.add(reject);
    });
  }
  /** další snímek (withFrameNanos) – vrátí čas v ms */
  frame(): Promise<number> {
    return new Promise((resolve, reject) => {
      if (!this.live) return reject(new Cancelled());
      requestAnimationFrame((t) => (this.live ? resolve(t) : reject(new Cancelled())));
    });
  }
  /** po await animace: když byl běh zrušen, skončit */
  check() {
    if (!this.live) throw new Cancelled();
  }
  /** await animace v rámci běhu: po zrušení běhu se dál nepokračuje */
  async run(p: Promise<unknown>) {
    await p;
    this.check();
  }
}

/** animateFloatAsState: plynulý přechod k [target]. */
export function useAnimatedValue(target: number, ms = 300, easing: Easing = FastOutSlowInEasing): number {
  const kit = useKit();
  const a = kit.anim(target);
  useEffect(() => {
    if (a.target !== target) a.animateTo(target, tween(ms, easing)).catch(() => {});
  });
  return a.value;
}

export const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v));
