import { useEffect, useLayoutEffect, useState } from 'react';
import { Store } from '../trainer/store';
import { storage } from '../trainer/settings';
import { useT, type TKey } from '../i18n';
import { Rich } from '../components/Sym';

/** Místa obrazovky, na která prohlídka ukazuje. */
export type TourSpot = 'drawing' | 'star' | 'stepper' | 'work' | 'new' | 'favorites' | 'learn' | 'settings';

/** Krok prohlídky: klíč textů a místo, které se zvýrazní (null = celá obrazovka). */
const STEPS: { key: string; spot: TourSpot | null }[] = [
  { key: 'tourWelcome', spot: null },
  { key: 'tourDrawing', spot: 'drawing' },
  { key: 'tourStepper', spot: 'stepper' },
  { key: 'tourWork', spot: 'work' },
  { key: 'tourStar', spot: 'star' },
  { key: 'tourFavorites', spot: 'favorites' },
  { key: 'tourNew', spot: 'new' },
  { key: 'tourLearn', spot: 'learn' },
  { key: 'tourSettings', spot: 'settings' },
  { key: 'tourEnd', spot: null },
];

const K_DONE = 'statika.tour.done';

/**
 * Prohlídka aplikace při prvním spuštění (shodná s aplikací): postupně zvýrazní části obrazovky a řekne, k čemu jsou.
 * Poslední krok nabídne výklad základů.
 */
export class TourState extends Store {
  readonly spots = new Map<TourSpot, HTMLElement>();
  index: number | null;

  constructor() {
    super();
    // kdo už na webu úlohy řešil, toho prohlídkou nezdržujeme (spustí si ji v nastavení)
    const solvedBefore = storage.json<{ solved: number }>('statika.stats', { solved: 0 }).solved > 0;
    this.index = storage.get(K_DONE) === '1' || solvedBefore ? null : 0;
  }

  get step() {
    return this.index === null ? null : STEPS[this.index];
  }

  get running() {
    return this.index !== null;
  }

  start() {
    this.index = 0;
    this.emit();
  }

  next() {
    if (this.index === null) return;
    if (this.index + 1 < STEPS.length) this.index++;
    else return this.finish();
    this.emit();
  }

  back() {
    if (this.index === null || this.index === 0) return;
    this.index--;
    this.emit();
  }

  finish() {
    this.index = null;
    storage.set(K_DONE, '1');
    this.emit();
  }
}

/** Ref, kterým se prvek přihlásí jako místo prohlídky. */
export const tourSpot = (tour: TourState, spot: TourSpot) => (el: HTMLElement | null) => {
  if (el) tour.spots.set(spot, el);
  else tour.spots.delete(spot);
};

/**
 * Překryv s prohlídkou. Zbytek obrazovky ztmaví, zvýrazněné místo nechá vidět a pod/nad ně dá bublinu s textem.
 * [onLearn] = „Ano, ukaž mi základy“ v posledním kroku.
 */
export function TourOverlay({ tour, onLearn, onFinish }: { tour: TourState; onLearn: () => void; onFinish: () => void }) {
  const t = useT();
  const step = tour.step;
  const i = tour.index ?? 0;
  const [rect, setRect] = useState<DOMRect | null>(null);

  useLayoutEffect(() => {
    const el = step?.spot ? tour.spots.get(step.spot) : null;
    if (!el) {
      setRect(null);
      return;
    }
    const r0 = el.getBoundingClientRect();
    // místo mimo obrazovku – posunout do pohledu (vysoké místo stačí ukázat nahoře)
    if (r0.top < 0 || r0.bottom > window.innerHeight) el.scrollIntoView({ block: r0.height > window.innerHeight * 0.6 ? 'start' : 'center' });
    const upd = () => setRect(el.getBoundingClientRect());
    upd();
    window.addEventListener('resize', upd);
    window.addEventListener('scroll', upd, true);
    return () => {
      window.removeEventListener('resize', upd);
      window.removeEventListener('scroll', upd, true);
    };
  }, [step, tour]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        tour.finish();
        onFinish();
      } else if (e.key === 'ArrowRight' && step?.key !== 'tourEnd') tour.next();
      else if (e.key === 'ArrowLeft') tour.back();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [tour, step, onFinish]);

  if (!step) return null;
  const pad = 6;
  const vh = window.innerHeight;
  const hole = rect && rect.width > 0 && rect.height > 0 ? rect : null;
  // bublina tam, kde je víc místa
  const below = hole ? hole.top + hole.height / 2 < vh / 2 : false;
  const cardPos: React.CSSProperties = !hole
    ? { top: '50%', transform: 'translate(-50%, -50%)' }
    : below
      ? { top: Math.min(hole.bottom + pad + 12, vh - 220) }
      : { bottom: Math.min(vh - hole.top + pad + 12, vh - 220) };

  return (
    <div className="tour" role="dialog" aria-modal="true" aria-labelledby="tour-title">
      {hole ? (
        <div className="tour-hole" style={{ left: hole.left - pad, top: hole.top - pad, width: hole.width + 2 * pad, height: hole.height + 2 * pad }} />
      ) : (
        <div className="tour-dim" />
      )}
      <div className="tour-card" style={cardPos}>
        <h3 id="tour-title" aria-live="polite">
          {t((step.key + 'Title') as TKey)}
        </h3>
        <p>
          <Rich text={t((step.key + 'Text') as TKey)} />
        </p>
        <div className="tour-dots">
          {STEPS.map((_, k) => (
            <span key={k} className={'dot' + (k <= i ? ' on' : '') + (k === i ? ' cur' : '')} />
          ))}
          <span className="small muted">{t('tourCount', { n: i + 1, of: STEPS.length })}</span>
        </div>
        {step.key === 'tourEnd' ? (
          <div className="tour-end">
            <button
              className="btn btn-primary"
              autoFocus
              onClick={() => {
                tour.finish();
                onLearn();
              }}
            >
              {t('firstRunYes')}
            </button>
            <div className="row between">
              <button className="btn btn-ghost" onClick={() => tour.back()}>
                {t('tourBack')}
              </button>
              <button
                className="btn btn-ghost"
                onClick={() => {
                  tour.finish();
                  onFinish();
                }}
              >
                {t('firstRunNo')}
              </button>
            </div>
          </div>
        ) : (
          <div className="row between">
            <button
              className="btn btn-ghost muted"
              onClick={() => {
                tour.finish();
                onFinish();
              }}
            >
              {t('tourSkip')}
            </button>
            <span className="row gap-s">
              {i > 0 && (
                <button className="btn btn-ghost" onClick={() => tour.back()}>
                  {t('tourBack')}
                </button>
              )}
              <button className="btn btn-primary" autoFocus onClick={() => tour.next()}>
                {t('tourNext')}
              </button>
            </span>
          </div>
        )}
      </div>
    </div>
  );
}
