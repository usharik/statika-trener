import { useEffect, useMemo, useRef, useState } from 'react';
import { generate } from './model/generator';
import { randomSeed } from './model/rng';
import type { BeamType, Level } from './model/types';
import { BeamDrawing, type DrawingProps } from './components/BeamDrawing';
import { LangContext, translate, type Lang, type TKey } from './i18n';
import { StepRelease } from './steps/StepRelease';
import { StepLoads } from './steps/StepLoads';
import { StepEquations, type UserEq } from './steps/StepEquations';
import { StepSolve } from './steps/StepSolve';
import { StepDone } from './steps/StepDone';

interface Settings {
  level: Level;
  types: BeamType[];
  lang: Lang;
  v?: number;
}

const ALL_TYPES: BeamType[] = ['simple', 'overhang', 'cantilever', 'gerber'];
const DEFAULTS: Settings = { level: 1, types: ['simple', 'overhang', 'cantilever', 'gerber'], lang: 'cs', v: 2 };

/** Starší uložená nastavení neměla Gerberův nosník zapnutý – jednou ho přidáme. */
function migrate(s: Settings): Settings {
  if ((s.v ?? 1) < 2 && !s.types.includes('gerber')) return { ...s, types: [...s.types, 'gerber'], v: 2 };
  return { ...s, v: 2 };
}

function load<T>(key: string, fallback: T): T {
  try {
    const v = localStorage.getItem(key);
    return v ? { ...fallback, ...JSON.parse(v) } : fallback;
  } catch {
    return fallback;
  }
}

function save(key: string, v: unknown) {
  try {
    localStorage.setItem(key, JSON.stringify(v));
  } catch {
    /* soukromé okno apod. */
  }
}

type View = Pick<DrawingProps, 'momentPoint' | 'side' | 'arm'>;

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

export default function App() {
  const [settings, setSettings] = useState<Settings>(() => migrate(load('statika.settings', { ...DEFAULTS, v: 1 })));
  const [stats, setStats] = useState(() => load('statika.stats', { solved: 0 }));
  // číslo úlohy v adrese (#12345) – úlohu lze sdílet nebo zopakovat
  const [seed, setSeed] = useState(() => Number(location.hash.slice(1)) || randomSeed());
  const [attempt, setAttempt] = useState(0);
  const [step, setStep] = useState(0);
  const [released, setReleased] = useState<Set<string>>(new Set());
  const [resolved, setResolved] = useState<Set<string>>(new Set());
  const [highlight, setHighlight] = useState<string | null>(null);
  const [activeSupport, setActiveSupport] = useState<string | null>(null);
  const [view, setView] = useState<View>({});
  const [eqs, setEqs] = useState<UserEq[]>([]);
  const [results, setResults] = useState<Record<string, number> | null>(null);
  const [hints, setHints] = useState(0);
  const [showSettings, setShowSettings] = useState(false);

  const t = (k: TKey) => translate(settings.lang, k);
  const compact = useMedia('(max-width: 640px)');

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
  const problem = useMemo(() => generate(seed, { level: settings.level, types: settings.types }), [seed, settings.level, settings.types]);

  useEffect(() => save('statika.settings', settings), [settings]);
  if (import.meta.env.DEV) (window as unknown as { __problem: unknown }).__problem = problem;
  useEffect(() => save('statika.stats', stats), [stats]);
  useEffect(() => history.replaceState(null, '', '#' + seed), [seed]);
  useEffect(() => {
    document.documentElement.lang = settings.lang;
    document.title = translate(settings.lang, 'appTitle');
  }, [settings.lang]);

  const reset = () => {
    setStep(0);
    setReleased(new Set());
    setResolved(new Set());
    setHighlight(null);
    setView({});
    setEqs([]);
    setResults(null);
    setHints(0);
    setActiveSupport(problem.supports[0]?.id ?? null);
    setAttempt((a) => a + 1);
  };

  // nová úloha → vše od začátku
  useEffect(() => {
    reset();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [problem]);

  const newProblem = () => setSeed(randomSeed());
  const hint = () => setHints((h) => h + 1);
  const key = `${seed}-${attempt}`;

  const steps = [t('step1'), t('step2'), t('step3'), t('step4'), t('step5')];

  const toggleType = (tp: BeamType) => {
    const has = settings.types.includes(tp);
    const types = has ? settings.types.filter((x) => x !== tp) : [...settings.types, tp];
    if (!types.length) return;
    setSettings({ ...settings, types: ALL_TYPES.filter((x) => types.includes(x)) });
  };

  return (
    <LangContext.Provider value={settings.lang}>
      <div className="app">
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
              <div className="sub">{t('appSubtitle')}</div>
            </div>
          </div>
          <div className="top-actions">
            <span className="stat" title={t('solved')}>
              {t('solved')}: <strong>{stats.solved}</strong>
            </span>
            <div className="seg lang">
              {(['cs', 'ru'] as Lang[]).map((l) => (
                <button key={l} className={'seg-btn' + (settings.lang === l ? ' seg-on' : '')} onClick={() => setSettings({ ...settings, lang: l })}>
                  {l.toUpperCase()}
                </button>
              ))}
            </div>
            <button className="btn btn-ghost" onClick={() => setShowSettings((s) => !s)} aria-expanded={showSettings}>
              {t('settings')}
            </button>
            <button className="btn btn-primary" onClick={newProblem}>
              {t('newProblem')}
            </button>
          </div>
        </header>

        {showSettings && (
          <div className="settings">
            <div className="seg">
              <span className="seg-label">{t('level')}</span>
              {([1, 2, 3] as Level[]).map((l) => (
                <button key={l} className={'seg-btn' + (settings.level === l ? ' seg-on' : '')} onClick={() => setSettings({ ...settings, level: l })}>
                  {t(`level${l}` as TKey)}
                </button>
              ))}
            </div>
            <div className="seg wrap">
              <span className="seg-label">{t('types')}</span>
              {ALL_TYPES.map((tp) => (
                <button key={tp} className={'seg-btn' + (settings.types.includes(tp) ? ' seg-on' : '')} onClick={() => toggleType(tp)}>
                  {t(`type_${tp}` as TKey)}
                </button>
              ))}
            </div>
          </div>
        )}

        <ol className="stepper">
          {steps.map((s, i) => (
            <li key={i} className={i === step ? 'cur' : i < step ? 'past' : ''}>
              <span className="num">{i < step ? '✓' : i + 1}</span>
              <span className="lbl">{s}</span>
            </li>
          ))}
        </ol>

        <div ref={stickyRef} className={'sticky-top' + (step === 2 ? ' sticky-eq' : '')}>
          <div className="card drawing-card">
            <div className="card-head">
              <span className="muted small">
                {t('problemNo')} {seed} · {t(`type_${problem.type}` as TKey)}
              </span>
              <span className="muted small">{t('lengthsNote')}</span>
            </div>
            <BeamDrawing
              compact={compact}
              problem={problem}
              released={released}
              resolved={resolved}
              highlight={highlight}
              activeSupport={step === 0 ? activeSupport : null}
              results={step === 4 ? results : null}
              {...(step === 2 ? view : {})}
            />
          </div>
          {/* sem se na kroku „Rovnice“ vkládá paleta (portal) */}
          <div id="palette-slot" />
        </div>

        {step === 0 && (
          <StepRelease
            key={key}
            problem={problem}
            released={released}
            onRelease={(id) => setReleased((s) => new Set(s).add(id))}
            onActive={setActiveSupport}
            onHint={hint}
            onDone={() => {
              setActiveSupport(null);
              setStep(1);
              setHighlight(null);
            }}
          />
        )}
        {step === 1 && (
          <StepLoads
            key={key}
            problem={problem}
            onResolve={(id) => setResolved((s) => new Set(s).add(id))}
            onHighlight={setHighlight}
            onHint={hint}
            onDone={() => {
              setStep(2);
              setHighlight(null);
            }}
          />
        )}
        {step === 2 && (
          <StepEquations
            key={key}
            problem={problem}
            onHighlight={setHighlight}
            onView={setView}
            onHint={hint}
            onDone={(e) => {
              setEqs(e);
              setStep(3);
              setHighlight(null);
            }}
          />
        )}
        {step === 3 && (
          <StepSolve
            key={key}
            problem={problem}
            eqs={eqs}
            onHighlight={setHighlight}
            onHint={hint}
            onDone={(r) => {
              setResults(r);
              setStep(4);
              setHighlight(null);
              setStats((s) => ({ ...s, solved: s.solved + 1 }));
            }}
          />
        )}
        {step === 4 && results && <StepDone problem={problem} results={results} hintsUsed={hints} onNext={newProblem} onRetry={reset} />}

        <footer className="foot muted small">
          {t('appSubtitle')} · {settings.level === 1 ? t('level1') : settings.level === 2 ? t('level2') : t('level3')}
        </footer>
      </div>
    </LangContext.Provider>
  );
}
