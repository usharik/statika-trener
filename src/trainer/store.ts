import { useSyncExternalStore } from 'react';

/**
 * Jednoduchý pozorovatelný stav: objekt se mění na místě a po změně zavolá `emit()`;
 * komponenty se přes `useStore` překreslí. (Obdoba mutableStateOf v Compose u mobilní aplikace.)
 */
export class Store {
  private listeners = new Set<() => void>();
  private ver = 0;

  subscribe = (l: () => void) => {
    this.listeners.add(l);
    return () => {
      this.listeners.delete(l);
    };
  };

  getVersion = () => this.ver;

  emit() {
    this.ver++;
    for (const l of [...this.listeners]) l();
  }
}

/** Překreslí komponentu při každé změně [store]. */
export function useStore(store: Store | null | undefined): void {
  useSyncExternalStore(store?.subscribe ?? noopSubscribe, store?.getVersion ?? zero);
}

const noopSubscribe = () => () => {};
const zero = () => 0;
