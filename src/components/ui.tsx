import type { ReactNode } from 'react';
import { Rich } from './Sym';
import { useT } from '../i18n';

export type Tone = 'nudge' | 'ok' | 'info' | 'hint';

export interface Msg {
  tone: Tone;
  text: string;
}

const icons: Record<Tone, string> = { nudge: '→', ok: '✓', info: 'i', hint: '?' };

export function Note({ tone, children }: { tone: Tone; children: ReactNode }) {
  return (
    <div className={`note note-${tone}`}>
      <span className="note-icon" aria-hidden>{icons[tone]}</span>
      <div>{children}</div>
    </div>
  );
}

export function Notes({ msgs }: { msgs: Msg[] }) {
  return (
    <>
      {msgs.map((m, i) => (
        <Note key={i + m.text} tone={m.tone}>
          <Rich text={m.text} />
        </Note>
      ))}
    </>
  );
}

export function HintButton({ level, onClick, disabled }: { level: number; onClick: () => void; disabled?: boolean }) {
  const t = useT();
  return (
    <button className="btn btn-ghost" onClick={onClick} disabled={disabled}>
      <span className="bulb" aria-hidden>?</span> {level === 0 ? t('hint') : t('moreHint')}
    </button>
  );
}

export function NumInput({ value, onChange, state, width = 90, label, onEnter }: { value: string; onChange: (v: string) => void; state?: 'ok' | 'nudge' | null; width?: number; label?: ReactNode; onEnter?: () => void }) {
  return (
    <label className="num">
      {label && <span className="num-label">{label}</span>}
      <input
        inputMode="decimal"
        value={value}
        style={{ width }}
        className={state ? 'in-' + state : ''}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => e.key === 'Enter' && onEnter?.()}
      />
    </label>
  );
}
