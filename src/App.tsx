import { lazy, Suspense, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { generate, type GenOptions } from './model/generator';
import { randomSeed } from './model/rng';
import type { BeamType, Level, Problem } from './model/types';
import { BeamDrawing } from './components/BeamDrawing';
import { LangContext, trFor, type Lang, type TKey, type Tr } from './i18n';
import { StepRelease } from './steps/StepRelease';
import { StepLoads } from './steps/StepLoads';
import { StepEquations } from './steps/StepEquations';
import { StepSolve } from './steps/StepSolve';
import { StepDone } from './steps/StepDone';
import { useStore } from './trainer/store';
import { STEP_DONE, STEP_EQUATIONS, STEP_RELEASE, TrainerSession } from './trainer/session';
import { ALL_TYPES, SettingsRepository, decodeOpts, encodeOpts, type ThemeMode } from './trainer/settings';
import { FavoritesRepository, sameFav, type Favorite } from './trainer/favorites';
import { Chips, Modal, useToast } from './components/ui';
import { DonateWidget } from './components/Donate';
import { LearnContext, type LearnNav } from './learn/nav';
import { LearnProgress } from './learn/progress';
import { TourOverlay, TourState, tourSpot } from './tour/Tour';

const LearnRoot = lazy(() => import('./learn/LearnRoot'));

export const WEB_URL = 'https://usharik.github.io/statika-trener/';
export const PRIVACY_URL = 'https://usharik.github.io/statika-trener/privacy.html';
export const PLAY_URL = 'https://play.google.com/store/apps/details?id=cz.usharik.statika';
const VERSION = '1.13';

/** Úloha pro číslo a nastavení; kdyby generátor selhal, zkusí se jiné číslo. */
function problemFor(seed: number, opts: GenOptions): Problem {
  let s = seed;
  for (let i = 0; i < 20; i++) {
    try {
      return generate(s, opts);
    } catch {
      s = randomSeed();
    }
  }
  return generate(1, { level: 1, types: ['simple'] });
}

function useMedia(q: string): boolean {
  const [m, setM] = useState(() => window.matchMedia?.(q).matches ?? false);
  useEffect(() => {
    const mq = window.matchMedia(q);
    const on = () => setM(mq.matches);
    mq.addEventListener('change', on);
    return () => mq.removeEventListener('change', on);
  }, [q]);
  return m;
}

const seedFromHash = () => {
  const n = Number(location.hash.slice(1));
  return Number.isInteger(n) && n >= 1 && n <= 999_999 ? n : null;
};

/** Po kolika vyřešených úlohách nabídnout podporu autora (první po 2., pak po každé 5.). */
const askDonateAfter = (solved: number) => solved >= 2 && (solved - 2) % 5 === 0;

export default function App() {
  const settings = useMemo(() => new SettingsRepository(), []);
  const favorites = useMemo(() => new FavoritesRepository(), []);
  const progress = useMemo(() => new LearnProgress(), []);
  const tour = useMemo(() => new TourState(), []);
  useStore(settings);
  useStore(favorites);
  useStore(tour);
  const lang = settings.lang;
  const t = useMemo(() => trFor(lang), [lang]);

  // číslo úlohy v adrese (#12345) – úlohu lze sdílet nebo zopakovat
  const [seed, setSeed] = useState(() => seedFromHash() ?? randomSeed());
  const [attempt, setAttempt] = useState(0);
  // úloha otevřená z oblíbených má vlastní nastavení generátoru (úroveň, typy) – jinak platí nastavení aplikace
  const [fixed, setFixed] = useState<GenOptions | null>(() => (seedFromHash() ? decodeOpts(settings.problemOpts) : null));
  const optsKey = `${settings.level}:${settings.types.join('+')}`;
  const opts: GenOptions = useMemo(() => fixed ?? { level: settings.level, types: settings.types }, [fixed, optsKey]); // eslint-disable-line react-hooks/exhaustive-deps
  const problem = useMemo(() => problemFor(seed, opts), [seed, opts]);

  // změna úrovně nebo typů v nastavení = zase úlohy podle nastavení
  const optsSeen = useRef(optsKey);
  useEffect(() => {
    if (optsSeen.current !== optsKey) {
      optsSeen.current = optsKey;
      setFixed(null);
    }
  }, [optsKey]);
  useEffect(() => settings.setProblemOpts(fixed ? encodeOpts(fixed) : null), [fixed, settings]);

  const [askDonate, setAskDonate] = useState(false);
  const session = useMemo(
    () =>
      new TrainerSession(problem, {
        onSolved: () => {
          settings.addSolved();
          favorites.markSolved(problem.seed, opts);
          setAskDonate(askDonateAfter(settings.solved));
        },
      }),
    // nová úloha nebo nový pokus
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [problem, attempt],
  );
  useStore(session);
  if (import.meta.env.DEV) (window as unknown as { __problem: unknown; __session: unknown }).__problem = problem;
  if (import.meta.env.DEV) (window as unknown as { __session: unknown }).__session = session;

  const [learnNav, setLearnNav] = useState<LearnNav | null>(null);
  const [showSettings, setShowSettings] = useState(false);
  const [showFavorites, setShowFavorites] = useState(false);
  const [showDonate, setShowDonate] = useState(false);
  const [askPdf, setAskPdf] = useState(false);
  const [confirmNew, setConfirmNew] = useState(false);
  const [confirmFav, setConfirmFav] = useState<Favorite | null>(null);
  const toast = useToast();

  const compact = useMedia('(max-width: 640px)');
  const systemDark = useMedia('(prefers-color-scheme: dark)');
  const theme = settings.theme === 'system' ? (systemDark ? 'dark' : 'light') : settings.theme;
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#0f1216' : '#f5f6f8');
  }, [theme]);
  useEffect(() => {
    document.documentElement.lang = lang;
    document.title = t('appTitle');
  }, [lang, t]);

  // výška přilepeného výkresu → CSS proměnná (aby se obsah při posunu nezasouval pod něj)
  const stickyRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const el = stickyRef.current;
    if (!el) return;
    const ro = new ResizeObserver(() => document.documentElement.style.setProperty('--sticky-h', el.offsetHeight + 'px'));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  // Safari (iPad/iPhone) při fokusu pole nebo zoomu posune „viditelnou oblast“ uvnitř stránky;
  // přilepený výkres by pak zajel pod horní okraj – posuneme ho s ní.
  useEffect(() => {
    const vv = window.visualViewport;
    if (!vv) return;
    const upd = () => document.documentElement.style.setProperty('--vv-top', Math.max(0, vv.offsetTop) + 'px');
    upd();
    vv.addEventListener('resize', upd);
    vv.addEventListener('scroll', upd);
    return () => {
      vv.removeEventListener('resize', upd);
      vv.removeEventListener('scroll', upd);
    };
  }, []);

  useEffect(() => history.replaceState(null, '', '#' + problem.seed), [problem.seed]);
  // úloha z odkazu (ruční změna adresy, odkaz od spolužáka)
  useEffect(() => {
    const on = () => {
      const n = seedFromHash();
      if (n && n !== problem.seed) {
        setSeed(n);
        setFixed(null);
        setAttempt(0);
      }
    };
    window.addEventListener('hashchange', on);
    return () => window.removeEventListener('hashchange', on);
  }, [problem.seed]);

  const newProblem = () => {
    setSeed(randomSeed());
    setFixed(null);
    setAttempt(0);
    setAskDonate(false);
    window.scrollTo({ top: 0 });
  };
  // rozpracovanou úlohu nezahodit jedním omylem
  const askNewProblem = () => (session.started ? setConfirmNew(true) : newProblem());
  const openSeed = (n: number) => {
    setSeed(n);
    setFixed(null);
    setAttempt(0);
  };
  const openFavorite = (f: Favorite) => {
    setShowFavorites(false);
    setConfirmFav(null);
    if (sameFav(f, problem.seed, opts)) return;
    setSeed(f.seed);
    setFixed(f.opts.level !== settings.level || f.opts.types.length !== settings.types.length || !f.opts.types.every((x) => settings.types.includes(x)) ? f.opts : null);
    setAttempt(0);
  };
  const toggleFavorite = () => {
    const on = favorites.toggle(problem.seed, opts, session.step === STEP_DONE);
    toast.show(t(on ? 'favAdded' : 'favRemoved'));
  };
  const share = async () => {
    const text = t('shareText', { n: problem.seed, level: t(`level${opts.level}` as TKey), url: WEB_URL + '#' + problem.seed });
    try {
      if (navigator.share) await navigator.share({ text });
      else {
        await navigator.clipboard.writeText(text);
        toast.show(t('shareCopied'));
      }
    } catch {
      /* zrušeno */
    }
  };

  const [pdfBusy, setPdfBusy] = useState(false);
  const exportPdf = async (withSolution: boolean) => {
    setAskPdf(false);
    setPdfBusy(true);
    try {
      const { problemPdf, problemPdfName, savePdf } = await import('./pdf/problemPdf');
      const blob = await problemPdf(problem, opts.level, withSolution, t);
      await savePdf(blob, problemPdfName(problem, withSolution, t));
    } catch (e) {
      console.error(e);
      toast.show(t('pdfFailed'));
    } finally {
      setPdfBusy(false);
    }
  };

  const learnApi = useMemo(() => ({ open: (nav: LearnNav) => setLearnNav(nav), here: null }), []);
  const closeLearn = useCallback((next: LearnNav | null) => setLearnNav(next), []);

  const step = session.step;
  const isFav = favorites.isFavorite(problem.seed, opts);

  return (
    <LangContext.Provider value={lang}>
      <LearnContext.Provider value={learnApi}>
        <div className="app" hidden={!!learnNav} aria-hidden={!!learnNav}>
          <header className="top">
            <div className="brand">
              <div className="logo" aria-hidden>
                <svg viewBox="0 0 32 32">
                  <rect x="3" y="11" width="26" height="4" rx="1" />
                  <path d="M7 15 L3 23 L11 23 Z" />
                  <path d="M25 15 L21 22 L29 22 Z" />
                  <line x1="20" y1="25" x2="30" y2="25" />
                </svg>
              </div>
              <div>
                <h1>{t('appTitle')}</h1>
                <div className="sub">
                  {t('solved')}: <strong>{settings.solved}</strong>
                </div>
              </div>
            </div>
            <div className="top-actions">
              <button className="btn btn-ghost top-new" onClick={askNewProblem} ref={tourSpot(tour, 'new')} title={t('newProblem')}>
                <span aria-hidden>↻</span> <span className="lbl">{t('newProblem')}</span>
              </button>
              <button className="icon-btn" onClick={() => setShowFavorites(true)} ref={tourSpot(tour, 'favorites')} title={t('favorites')} aria-label={t('favorites')}>
                <span className={favorites.items.length ? 'star-on' : ''}>{favorites.items.length ? '★' : '☆'}</span>
              </button>
              <button className="icon-btn" onClick={() => setLearnNav({ kind: 'home' })} ref={tourSpot(tour, 'learn')} title={t('learnOpen')} aria-label={t('learnOpen')}>
                <BookIcon />
              </button>
              <button className="icon-btn" onClick={() => setShowSettings(true)} ref={tourSpot(tour, 'settings')} title={t('settings')} aria-label={t('settings')}>
                <GearIcon />
              </button>
              <button className="btn btn-donate btn-small top-donate" onClick={() => setShowDonate(true)} title={t('donate')}>
                ♥ <span className="lbl">{t('donate')}</span>
              </button>
            </div>
          </header>

          <ol className="stepper" ref={tourSpot(tour, 'stepper')}>
            {[0, 1, 2, 3, 4].map((i) => (
              <li key={i} className={i === step ? 'cur' : i < step ? 'past' : ''}>
                <span className="num">{i < step ? '✓' : i + 1}</span>
                <span className="lbl">{t(`step${i + 1}` as TKey)}</span>
              </li>
            ))}
          </ol>

          <div ref={stickyRef} className={'sticky-top' + (step === STEP_EQUATIONS ? ' sticky-eq' : '')}>
            <div className="card drawing-card" ref={tourSpot(tour, 'drawing')}>
              <div className="card-head">
                <span className="muted small">
                  {t('problemNo')} {problem.seed} · {t(`type_${problem.type}` as TKey)}
                </span>
                <span className="card-tools">
                  <span className="muted small lengths">{t('lengthsNote')}</span>
                  {/* PDF úlohy (zadání / s řešením) – přímo u výkresu */}
                  <button className="icon-btn icon-sm" onClick={() => setAskPdf(true)} title={t('pdfExport')} aria-label={t('pdfExport')} disabled={pdfBusy}>
                    {pdfBusy ? <span className="spinner" /> : <PdfIcon />}
                  </button>
                  <button className="icon-btn icon-sm" onClick={toggleFavorite} ref={tourSpot(tour, 'star')} title={t(isFav ? 'favRemove' : 'favAdd')} aria-label={t(isFav ? 'favRemove' : 'favAdd')} aria-pressed={isFav}>
                    <span className={isFav ? 'star-on' : ''}>{isFav ? '★' : '☆'}</span>
                  </button>
                </span>
              </div>
              <BeamDrawing
                key={`${problem.seed}-${attempt}`}
                compact={compact}
                problem={problem}
                released={session.released}
                resolved={session.resolved}
                highlight={session.highlight}
                activeSupport={step === STEP_RELEASE ? session.activeSupport : null}
                results={step === STEP_DONE ? session.results : null}
                {...(step === STEP_EQUATIONS ? session.view : {})}
              />
            </div>
            {/* sem se na kroku „Rovnice“ vkládá paleta (portal) */}
            <div id="palette-slot" />
          </div>

          <div ref={tourSpot(tour, 'work')}>
            {step === 0 && <StepRelease st={session.release} />}
            {step === 1 && session.loads && <StepLoads st={session.loads} />}
            {step === 2 && session.equations && <StepEquations st={session.equations} onHighlight={(id) => session.setHighlight(id)} />}
            {step === 3 && session.solve && <StepSolve st={session.solve} onHighlight={(id) => session.setHighlight(id)} />}
            {step === 4 && session.results && (
              <StepDone
                key={`${problem.seed}-${attempt}`}
                problem={problem}
                results={session.results}
                hints={session.hints}
                askDonate={askDonate}
                onNext={newProblem}
                onRetry={() => setAttempt((a) => a + 1)}
                onPdf={() => setAskPdf(true)}
              />
            )}
          </div>

          <footer className="foot muted small">
            {t('appSubtitle')} · {t(`level${opts.level}` as TKey)} ·{' '}
            <button className="link-btn" onClick={() => setShowDonate(true)}>
              ♥ {t('donate')}
            </button>
          </footer>
        </div>

        {learnNav && (
          <Suspense fallback={<div className="learn-loading">…</div>}>
            <LearnRoot nav={learnNav} progress={progress} onNav={closeLearn} />
          </Suspense>
        )}

        {confirmNew && (
          <Modal title={t('confirmNewTitle')} onClose={() => setConfirmNew(false)}>
            <p>{t('confirmNewText')}</p>
            <div className="actions end">
              <button className="btn" onClick={() => setConfirmNew(false)}>
                {t('cancel')}
              </button>
              <button
                className="btn btn-primary"
                autoFocus
                onClick={() => {
                  setConfirmNew(false);
                  newProblem();
                }}
              >
                {t('newProblem')}
              </button>
            </div>
          </Modal>
        )}

        {showFavorites && (
          <FavoritesModal
            favorites={favorites}
            t={t}
            current={[problem.seed, opts]}
            onClose={() => setShowFavorites(false)}
            onOpen={(f) => (session.started && !sameFav(f, problem.seed, opts) ? setConfirmFav(f) : openFavorite(f))}
          />
        )}
        {confirmFav && (
          <Modal title={t('favOpenTitle')} onClose={() => setConfirmFav(null)}>
            <p>{t('favOpenText')}</p>
            <div className="actions end">
              <button className="btn" onClick={() => setConfirmFav(null)}>
                {t('cancel')}
              </button>
              <button className="btn btn-primary" autoFocus onClick={() => openFavorite(confirmFav)}>
                {t('open')}
              </button>
            </div>
          </Modal>
        )}

        {askPdf && (
          <Modal title={t('pdfExportTitle')} onClose={() => setAskPdf(false)}>
            <p className="muted">{t('pdfExportText')}</p>
            <div className="pdf-choices">
              <button className="btn" onClick={() => exportPdf(false)}>
                {t('pdfOnlyTask')}
              </button>
              <button className="btn btn-primary" onClick={() => exportPdf(true)}>
                {t('pdfWithSolution')}
              </button>
            </div>
            <p className="small muted">
              {t('pdfSolutionPaid')}{' '}
              <button
                className="link-btn"
                onClick={() => {
                  setAskPdf(false);
                  setShowDonate(true);
                }}
              >
                ♥ {t('donate')}
              </button>
            </p>
          </Modal>
        )}

        {showDonate && (
          <Modal title={t('donateTitle')} onClose={() => setShowDonate(false)}>
            <p>{t('donateText')}</p>
            <DonateWidget />
          </Modal>
        )}

        {showSettings && (
          <SettingsModal
            settings={settings}
            onClose={() => setShowSettings(false)}
            onOpen={(n) => {
              openSeed(n);
              setShowSettings(false);
            }}
            onShare={share}
            onTour={() => {
              setShowSettings(false);
              tour.start();
            }}
            onDonate={() => {
              setShowSettings(false);
              setShowDonate(true);
            }}
          />
        )}

        {/* při prvním spuštění prohlídka aplikace; poslední krok nabídne výklad základů */}
        {!learnNav && tour.running && (
          <TourOverlay
            tour={tour}
            onLearn={() => {
              progress.setFirstRunAsked();
              setLearnNav({ kind: 'open', lesson: 1, card: 0 });
            }}
            onFinish={() => progress.setFirstRunAsked()}
          />
        )}

        {toast.node}
        {pdfBusy && (
          <div className="toast" role="status">
            {t('pdfBusy')}
          </div>
        )}
      </LearnContext.Provider>
    </LangContext.Provider>
  );
}

function SettingsModal({
  settings,
  onClose,
  onOpen,
  onShare,
  onTour,
  onDonate,
}: {
  settings: SettingsRepository;
  onClose: () => void;
  onOpen: (n: number) => void;
  onShare: () => void;
  onTour: () => void;
  onDonate: () => void;
}) {
  useStore(settings);
  const t = trFor(settings.lang);
  const [num, setNum] = useState('');
  const n = Number(num.trim());
  const valid = Number.isInteger(n) && n >= 1 && n <= 999_999;
  return (
    <Modal title={t('settings')} onClose={onClose}>
      <div className="set-section">
        <h3>{t('level')}</h3>
        <Chips<Level> value={settings.level} options={([1, 2, 3] as Level[]).map((l) => ({ v: l, label: t(`level${l}` as TKey) }))} onChange={(l) => settings.updateLevel(l)} />
      </div>
      <div className="set-section">
        <h3>{t('types')}</h3>
        <Chips<BeamType> multi value={settings.types} options={ALL_TYPES.map((x) => ({ v: x, label: t(`type_${x}` as TKey) }))} onChange={(x) => settings.toggleType(x)} />
      </div>
      <div className="set-section">
        <h3>{t('language')}</h3>
        <Chips<Lang>
          value={settings.lang}
          options={[
            { v: 'cs', label: 'Čeština' },
            { v: 'ru', label: 'Русский' },
          ]}
          onChange={(l) => settings.updateLang(l)}
        />
      </div>
      <div className="set-section">
        <h3>{t('theme')}</h3>
        <Chips<ThemeMode>
          value={settings.theme}
          options={[
            { v: 'system', label: t('themeSystem') },
            { v: 'light', label: t('themeLight') },
            { v: 'dark', label: t('themeDark') },
          ]}
          onChange={(v) => settings.updateTheme(v)}
        />
      </div>
      <div className="set-section">
        <h3>{t('openProblem')}</h3>
        <form
          className="row gap-s"
          onSubmit={(e) => {
            e.preventDefault();
            if (valid) onOpen(n);
          }}
        >
          <input className="text-in" inputMode="numeric" placeholder="12345" value={num} onChange={(e) => setNum(e.target.value.replace(/\D/g, '').slice(0, 6))} aria-label={t('openProblem')} />
          <button className="btn btn-primary" disabled={!valid}>
            {t('open')}
          </button>
        </form>
        <p className="small muted">{t('openProblemHint')}</p>
      </div>
      <div className="set-links">
        <button className="link-btn" onClick={onShare}>
          ↗ {t('share')}
        </button>
        <button className="link-btn" onClick={onTour}>
          {t('tourRestart')}
        </button>
        <a className="link-btn" href={PLAY_URL} target="_blank" rel="noopener noreferrer">
          {t('getApp')}
        </a>
        <a className="link-btn" href={PRIVACY_URL} target="_blank" rel="noopener noreferrer">
          {t('privacyPolicy')}
        </a>
      </div>
      <p className="small muted">{t('getAppText')}</p>
      <div className="set-donate">
        <p className="small">{t('donateText')}</p>
        <button className="btn btn-donate" onClick={onDonate}>
          ♥ {t('donate')}
        </button>
      </div>
      <p className="small muted">
        {t('version')} {VERSION}
      </p>
    </Modal>
  );
}

function FavoritesModal({ favorites, t, current, onClose, onOpen }: { favorites: FavoritesRepository; t: Tr; current: [number, GenOptions]; onClose: () => void; onOpen: (f: Favorite) => void }) {
  useStore(favorites);
  return (
    <Modal title={t('favorites') + (favorites.items.length ? ` (${favorites.items.length})` : '')} onClose={onClose} wide>
      {!favorites.items.length && <p className="muted">{t('favEmpty')}</p>}
      <ul className="fav-list">
        {favorites.items.map((f) => {
          let p: Problem | null = null;
          try {
            p = generate(f.seed, f.opts);
          } catch {
            p = null;
          }
          const isCurrent = sameFav(f, current[0], current[1]);
          const status = [f.solved ? '✓ ' + t('favSolved') : null, isCurrent ? t('favCurrent') : null].filter(Boolean).join(' · ');
          return (
            <li key={f.seed + encodeOpts(f.opts)} className={'fav' + (isCurrent ? ' fav-current' : '')}>
              <button className="fav-open" onClick={() => onOpen(f)}>
                {p && (
                  <span className="fav-thumb">
                    <BeamDrawing problem={p} compact />
                  </span>
                )}
                <span className="fav-text">
                  <strong>
                    {t('problemNo')} {f.seed}
                  </strong>
                  <span className="small muted">{(p ? t(`type_${p.type}` as TKey) + ' · ' : '') + t(`level${f.opts.level}` as TKey)}</span>
                  {status && <span className={'small ' + (f.solved ? 'ok-text' : 'muted')}>{status}</span>}
                </span>
              </button>
              <button className="icon-btn" onClick={() => favorites.remove(f)} title={t('favDelete')} aria-label={t('favDelete')}>
                🗑
              </button>
            </li>
          );
        })}
      </ul>
    </Modal>
  );
}

/** Otevřená kniha – vstup do výkladu (statika i trigonometrie) a procvičování. */
function BookIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden>
      <path d="M11 5.5C9 4.2 5.8 3.8 2.5 4.5V19c3.3-.7 6.5-.3 8.5 1z M13 5.5c2-1.3 5.2-1.7 8.5-1V19c-3.3-.7-6.5-.3-8.5 1z" />
    </svg>
  );
}

function GearIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden>
      <path d="M19.14 12.94c.04-.31.06-.62.06-.94s-.02-.63-.06-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.61-.22l-2.39.96a7.03 7.03 0 0 0-1.63-.94l-.36-2.54A.5.5 0 0 0 13.9 2h-3.84a.5.5 0 0 0-.5.42l-.36 2.54c-.59.24-1.13.56-1.63.94l-2.39-.96a.5.5 0 0 0-.61.22L2.65 8.48a.5.5 0 0 0 .12.64l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94l-2.03 1.58a.5.5 0 0 0-.12.64l1.92 3.32c.13.22.39.3.61.22l2.39-.96c.5.38 1.04.7 1.63.94l.36 2.54c.05.24.25.42.5.42h3.84c.25 0 .45-.18.5-.42l.36-2.54c.59-.24 1.13-.56 1.63-.94l2.39.96c.22.08.48 0 .61-.22l1.92-3.32a.5.5 0 0 0-.12-.64zM12 15.5A3.5 3.5 0 1 1 12 8.5a3.5 3.5 0 0 1 0 7z" />
    </svg>
  );
}

/** List papíru s ohnutým rohem – export úlohy do PDF. */
function PdfIcon() {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden>
      <path fillRule="evenodd" d="M5 2h9.5L20 7.5V22H5z M7 4v16h11V9h-5V4z" />
      <path d="M9 12h7v1.6H9z M9 15.5h7v1.6H9z" />
    </svg>
  );
}
