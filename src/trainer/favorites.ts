import type { GenOptions } from '../model/generator';
import { Store } from './store';
import { decodeOpts, encodeOpts, storage } from './settings';

/**
 * Oblíbená úloha. Úloha je jednoznačně dána číslem a nastavením generátoru (úroveň + typy nosníků),
 * proto se ukládá obojí – po změně nastavení se otevře pořád stejná úloha.
 */
export interface Favorite {
  seed: number;
  opts: GenOptions;
  solved: boolean;
}

export const sameFav = (f: Favorite, seed: number, opts: GenOptions) =>
  f.seed === seed && f.opts.level === opts.level && f.opts.types.length === opts.types.length && f.opts.types.every((t) => opts.types.includes(t));

const KEY = 'statika.favorites';
const MAX = 200;

/** Seznam oblíbených úloh v prohlížeči (nejnovější první); formát shodný s aplikací. */
export class FavoritesRepository extends Store {
  items: Favorite[] = this.load();

  isFavorite(seed: number, opts: GenOptions) {
    return this.items.some((f) => sameFav(f, seed, opts));
  }

  /** Přidá, nebo odebere úlohu; vrátí true, je-li teď v oblíbených. */
  toggle(seed: number, opts: GenOptions, solved: boolean): boolean {
    let now: boolean;
    if (this.isFavorite(seed, opts)) {
      this.items = this.items.filter((f) => !sameFav(f, seed, opts));
      now = false;
    } else {
      this.items = [{ seed, opts, solved }, ...this.items].slice(0, MAX);
      now = true;
    }
    this.save();
    return now;
  }

  remove(f: Favorite) {
    this.items = this.items.filter((x) => x !== f);
    this.save();
  }

  /** Vyřešená oblíbená úloha dostane v seznamu fajfku. */
  markSolved(seed: number, opts: GenOptions) {
    if (!this.items.some((f) => sameFav(f, seed, opts) && !f.solved)) return;
    this.items = this.items.map((f) => (sameFav(f, seed, opts) ? { ...f, solved: true } : f));
    this.save();
  }

  private load(): Favorite[] {
    return (storage.get(KEY) ?? '')
      .split(';')
      .map((row) => {
        const p = row.split(',');
        if (p.length !== 3) return null;
        const seed = Number(p[0]);
        if (!Number.isInteger(seed) || seed <= 0) return null;
        const opts = decodeOpts(p[1]);
        if (!opts) return null;
        return { seed, opts, solved: p[2] === '1' };
      })
      .filter((f): f is Favorite => !!f);
  }

  private save() {
    storage.set(KEY, this.items.map((f) => `${f.seed},${encodeOpts(f.opts)},${f.solved ? 1 : 0}`).join(';'));
    this.emit();
  }
}
