import { createContext, useContext } from 'react';

/** Kam se má výklad otevřít (shodné s aplikací – LearnNav). */
export type LearnNav =
  /** seznam lekcí aktuálního kurzu */
  | { kind: 'home' }
  | { kind: 'glossary' }
  /**
   * Lekce [lesson] od karty [card]. [fromTrainer] = otevřeno z úlohy – zavřením se vrátí rovnou k ní.
   * [returnTo] = otevřeno odkazem z jiného místa výkladu – „zpět“ i „Hotovo“ vedou tam.
   */
  | { kind: 'open'; lesson: number; card: number; fromTrainer?: boolean; returnTo?: LearnNav }
  /** Procvičování: [set] = undefined → seznam sérií, jinak série daného souboru. */
  | { kind: 'practice'; set?: string; returnTo?: LearnNav };

export interface LearnApi {
  open(nav: LearnNav): void;
  /** kde ve výkladu žák právě je (odkazy uvnitř výkladu se sem vrátí); null = mimo výklad */
  here: LearnNav | null;
}

export const LearnContext = createContext<LearnApi>({ open: () => {}, here: null });

export const useLearn = () => useContext(LearnContext);
