import { Fragment, type ReactNode } from 'react';

/** Značka typu „R_Ax“ → R s indexem Ax. */
export function Sym({ s }: { s: string }) {
  const i = s.indexOf('_');
  if (i < 0) return <i className="sym">{s}</i>;
  return (
    <span className="sym">
      <i>{s.slice(0, i)}</i>
      <sub>{s.slice(i + 1)}</sub>
    </span>
  );
}

/** Text s tokeny [[značka]] a **tučně**. */
export function Rich({ text }: { text: string }) {
  const parts = text.split(/(\[\[[^\]]+\]\]|\*\*[^*]+\*\*)/g);
  return (
    <>
      {parts.map((p, i) => {
        if (p.startsWith('[[')) return <Sym key={i} s={p.slice(2, -2)} />;
        if (p.startsWith('**')) return <strong key={i}><Rich text={p.slice(2, -2)} /></strong>;
        return <Fragment key={i}>{p}</Fragment>;
      })}
    </>
  );
}

export const rich = (text: string): ReactNode => <Rich text={text} />;
