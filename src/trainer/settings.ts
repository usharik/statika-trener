import type { BeamType, Level } from '../model/types';
import type { GenOptions } from '../model/generator';
import type { Lang } from '../i18n';
import { Store } from './store';

export const ALL_TYPES: BeamType[] = ['simple', 'overhang', 'cantilever', 'gerber'];
export type ThemeMode = 'system' | 'light' | 'dark';

/** Úložiště v prohlížeči (soukromé okno apod. nesmí aplikaci shodit). */
export const storage = {
  get(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  },
  set(key: string, v: string) {
    try {
      localStorage.setItem(key, v);
    } catch {
      /* soukromé okno apod. */
    }
  },
  json<T>(key: string, fallback: T): T {
    try {
      const v = localStorage.getItem(key);
      return v ? { ...fallback, ...JSON.parse(v) } : fallback;
    } catch {
      return fallback;
    }
  },
};

interface Saved {
  level: Level;
  types: BeamType[];
  lang?: Lang;
  v?: number;
  /** nevyplněno = podle systému */
  theme?: 'light' | 'dark';
  /** nastavení generátoru úlohy otevřené z oblíbených (liší-li se od aktuálního) */
  problemOpts?: string | null;
}

const KEY = 'statika.settings';

function systemLang(): Lang {
  const l = (navigator.languages?.[0] ?? navigator.language ?? '').toLowerCase();
  return l.startsWith('ru') || l.startsWith('uk') || l.startsWith('be') ? 'ru' : 'cs';
}

/** Zápis nastavení generátoru do řetězce („2:simple+gerber“) a zpět – stejně jako v aplikaci. */
export const encodeOpts = (o: GenOptions) => `${o.level}:` + o.types.join('+');

export function decodeOpts(s: string | null | undefined): GenOptions | null {
  if (!s) return null;
  const parts = s.split(':');
  if (parts.length !== 2) return null;
  const level = Number(parts[0]);
  if (![1, 2, 3].includes(level)) return null;
  const types = parts[1].split('+').filter((t): t is BeamType => (ALL_TYPES as string[]).includes(t));
  if (!types.length) return null;
  return { level: level as Level, types: ALL_TYPES.filter((t) => types.includes(t)) };
}

/** Nastavení a statistika uložené v prohlížeči (klíče shodné se starší webovou verzí). */
export class SettingsRepository extends Store {
  level: Level;
  types: BeamType[];
  lang: Lang;
  theme: ThemeMode;
  solved: number;
  problemOpts: string | null;

  constructor() {
    super();
    const s = storage.json<Saved>(KEY, { level: 1, types: [...ALL_TYPES], v: 1 });
    // starší uložená nastavení neměla Gerberův nosník zapnutý – jednou ho přidáme
    let types = (s.types ?? []).filter((t) => ALL_TYPES.includes(t));
    if ((s.v ?? 1) < 2 && !types.includes('gerber')) types = [...types, 'gerber'];
    this.level = ([1, 2, 3] as Level[]).includes(s.level) ? s.level : 1;
    this.types = types.length ? ALL_TYPES.filter((t) => types.includes(t)) : [...ALL_TYPES];
    this.lang = s.lang === 'ru' || s.lang === 'cs' ? s.lang : systemLang();
    this.theme = s.theme ?? 'system';
    this.problemOpts = s.problemOpts ?? null;
    this.solved = storage.json<{ solved: number }>('statika.stats', { solved: 0 }).solved || 0;
    this.save();
  }

  private save() {
    const out: Saved = { level: this.level, types: this.types, lang: this.lang, v: 2, problemOpts: this.problemOpts };
    if (this.theme !== 'system') out.theme = this.theme;
    storage.set(KEY, JSON.stringify(out));
  }

  private changed() {
    this.save();
    this.emit();
  }

  updateLevel(v: Level) {
    this.level = v;
    this.changed();
  }

  /** Zapne/vypne typ nosníku; poslední zapnutý typ vypnout nejde. */
  toggleType(t: BeamType) {
    const next = this.types.includes(t) ? this.types.filter((x) => x !== t) : [...this.types, t];
    if (!next.length) return;
    this.types = ALL_TYPES.filter((x) => next.includes(x));
    this.changed();
  }

  updateLang(v: Lang) {
    this.lang = v;
    this.changed();
  }

  updateTheme(v: ThemeMode) {
    this.theme = v;
    this.changed();
  }

  setProblemOpts(v: string | null) {
    if (this.problemOpts === v) return;
    this.problemOpts = v;
    this.save();
  }

  addSolved() {
    this.solved++;
    storage.set('statika.stats', JSON.stringify({ solved: this.solved }));
    this.emit();
  }
}
