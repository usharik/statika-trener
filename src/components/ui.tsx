import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Rich } from './Sym';
import { useT, useLang, pick } from '../i18n';
import type { Msg, Tone } from '../trainer/session';
import { toggleSign } from '../format';
import { useLearn } from '../learn/nav';
import { lessonTitle } from '../learn/titles';

const icons: Record<Tone, string> = { nudge: '→', ok: '✓', info: 'i', hint: '?' };

export function Note({ tone, children }: { tone: Tone; children: ReactNode }) {
  return (
    <div className={`note note-${tone}`}>
      <span className="note-icon" aria-hidden>
        {icons[tone]}
      </span>
      <div>{children}</div>
    </div>
  );
}

/** Odkaz „📖 Název lekce“ – otevře výklad nad úlohou (úloha zůstane rozpracovaná). */
export function LearnLink({ lesson, card = 0 }: { lesson: number; card?: number }) {
  const t = useT();
  const lang = useLang();
  const learn = useLearn();
  const title = pick(lang, lessonTitle(lesson));
  return (
    <button
      type="button"
      className="learn-link"
      onClick={() =>
        // uvnitř výkladu (procvičování, karta) se odkaz po zavření lekce vrátí tam; z trenažéru zpět k úloze
        learn.open(learn.here ? { kind: 'open', lesson, card, returnTo: learn.here } : { kind: 'open', lesson, card, fromTrainer: true })
      }
    >
      📖 {t(lesson >= 100 ? 'learnLinkTrig' : 'learnLink', { title })}
    </button>
  );
}

export function Notes({ msgs }: { msgs: Msg[] }) {
  const t = useT();
  const ref = useReveal(msgs);
  return (
    <div ref={ref}>
      {msgs.map((m, i) => (
        <Note key={i + m.tone + m.text(t)} tone={m.tone}>
          <Rich text={m.text(t)} />
          {m.learn && (
            <div>
              <LearnLink lesson={m.learn[0]} card={m.learn[1]} />
            </div>
          )}
        </Note>
      ))}
    </div>
  );
}

/** Když se [key] změní (nová zpráva), posune se obsah tak, aby byl prvek vidět. Při prvním zobrazení se nic neposouvá. */
export function useReveal<T extends HTMLElement = HTMLDivElement>(key: unknown) {
  const ref = useRef<T>(null);
  const first = useRef(true);
  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    const el = ref.current;
    if (!el) return;
    const id = setTimeout(() => el.scrollIntoView({ block: 'nearest', behavior: 'smooth' }), 80);
    return () => clearTimeout(id);
  }, [key]);
  return ref;
}

/** Seznam už vyřešených dílčích úkolů. */
export function DoneLog({ log }: { log: Msg[] }) {
  const t = useT();
  if (!log.length) return null;
  return (
    <ul className="log">
      {log.map((m, i) => (
        <li key={i}>
          <span className="check">✓</span> <Rich text={m.text(t)} />
        </li>
      ))}
    </ul>
  );
}

export function HintButton({ level, onClick, disabled, lastLabel }: { level: number; onClick: () => void; disabled?: boolean; lastLabel?: string }) {
  const t = useT();
  return (
    <button className="btn btn-ghost" onClick={onClick} disabled={disabled}>
      <span className="bulb" aria-hidden>
        ?
      </span>{' '}
      {level === 0 ? t('hint') : level >= 2 && lastLabel ? lastLabel : t('moreHint')}
    </button>
  );
}

export function NumInput({
  value,
  onChange,
  state,
  width = 90,
  label,
  onEnter,
  suffix,
  signed,
  autoFocus,
}: {
  value: string;
  onChange: (v: string) => void;
  state?: 'ok' | 'nudge' | null;
  width?: number;
  label?: ReactNode;
  onEnter?: () => void;
  suffix?: string;
  /** tlačítko „±“: číselná klávesnice na telefonu často nemá minus */
  signed?: boolean;
  autoFocus?: boolean;
}) {
  const t = useT();
  const ok = state === 'ok';
  return (
    <label className="num">
      {label && <span className="num-label">{label}</span>}
      <span className="num-row">
        <span className={'num-field' + (state ? ' in-' + state : '')}>
          <input
            inputMode="decimal"
            value={value}
            style={{ width }}
            readOnly={ok}
            autoFocus={autoFocus}
            autoComplete="off"
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && onEnter?.()}
          />
          {signed && !ok && (
            <button type="button" className="sign-btn" title={t('signToggle')} aria-label={t('signToggle')} onClick={() => onChange(toggleSign(value))}>
              ±
            </button>
          )}
        </span>
        {suffix && <span className="unit">{suffix}</span>}
        {ok && <span className="check">✓</span>}
      </span>
    </label>
  );
}

/** Panel kroku: nadpis, úvod, odkaz do výkladu a obsah. */
export function StepPanel({ title, intro, learn, children, className }: { title: string; intro?: string; learn?: [number, number]; children: ReactNode; className?: string }) {
  return (
    <section className={'panel' + (className ? ' ' + className : '')}>
      <h2>{title}</h2>
      {intro && (
        <p className="lead">
          <Rich text={intro} />
        </p>
      )}
      {learn && <LearnLink lesson={learn[0]} card={learn[1]} />}
      {children}
    </section>
  );
}

/** Řádek tlačítek dole v kroku; po změně [reveal] se posune do pohledu. */
export function Actions({ children, reveal, center }: { children: ReactNode; reveal?: unknown; center?: boolean }) {
  const ref = useReveal(reveal);
  return (
    <div ref={ref} className={'actions' + (center ? ' center' : '')}>
      {children}
    </div>
  );
}

/** Dialog (na telefonu jako list zespodu). Esc a klepnutí mimo ho zavřou. */
export function Modal({ title, onClose, children, wide, className }: { title?: ReactNode; onClose: () => void; children: ReactNode; wide?: boolean; className?: string }) {
  const t = useT();
  const box = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    const prev = document.activeElement as HTMLElement | null;
    box.current?.focus({ preventScroll: true });
    document.body.classList.add('modal-open');
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.classList.remove('modal-open');
      prev?.focus?.({ preventScroll: true });
    };
  }, [onClose]);
  return (
    <div className="modal-back" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div ref={box} className={'modal' + (wide ? ' modal-wide' : '') + (className ? ' ' + className : '')} role="dialog" aria-modal="true" tabIndex={-1}>
        <div className="modal-head">
          {title && <h2>{title}</h2>}
          <button className="icon-btn modal-x" onClick={onClose} aria-label={t('close')} title={t('close')}>
            ✕
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}

/** Krátké oznámení dole na obrazovce. */
export function useToast() {
  const [text, setText] = useState<string | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const show = (s: string) => {
    setText(s);
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => setText(null), 3200);
  };
  const node = text ? (
    <div className="toast" role="status" onClick={() => setText(null)}>
      {text}
    </div>
  ) : null;
  return { show, node };
}

/** Přepínač (čipy): vybraná hodnota zvýrazněná. */
export function Chips<T extends string | number>({ value, options, onChange, multi }: { value: T | T[]; options: { v: T; label: ReactNode }[]; onChange: (v: T) => void; multi?: boolean }) {
  const on = (v: T) => (multi ? (value as T[]).includes(v) : value === v);
  return (
    <div className="chips">
      {options.map((o) => (
        <button key={String(o.v)} type="button" className={'seg-btn' + (on(o.v) ? ' seg-on' : '')} aria-pressed={on(o.v)} onClick={() => onChange(o.v)}>
          {o.label}
        </button>
      ))}
    </div>
  );
}
