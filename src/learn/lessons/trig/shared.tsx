/*
 * Společné kousky kurzu trigonometrie (z learn/trig/TrigLesson1.kt v aplikaci): rampa, volba s kontrolou a tip.
 */
import { ActionButton, Chips, L, Note, SmallText, pt, useKit, type Colors, type PrimBuilder, type Pt, type Step } from '../../kit';
import { Trig } from '../../../practice/trig';

/** Pravoúhlý trojúhelník „rampa“: úhel α vlevo dole v (x0, y0), přepona délky [hyp] (jednotky scény). */
export class Ramp {
  readonly a: Pt;
  readonly c: Pt;
  readonly b: Pt;
  readonly alpha: number;
  constructor(x0: number, y0: number, hyp: number, alpha: number) {
    this.alpha = alpha;
    this.a = pt(x0, y0);
    this.c = pt(x0 + hyp * Trig.cosD(alpha), y0);
    this.b = pt(this.c.x, y0 - hyp * Trig.sinD(alpha));
  }
}

/** Nakreslí rampu: plocha, přepona červeně, odvěsny fialově, značka pravého úhlu, oblouk α. */
export function rampTri(b: PrimBuilder, r: Ramp, c: Colors, angleLabel: string | null, alpha = 1, labels = true, hypLabel: string | null = null, oppLabel: string | null = null) {
  b.poly(
    [
      [r.a.x, r.a.y],
      [r.b.x, r.b.y],
      [r.c.x, r.c.y],
    ],
    c.subtle,
    null,
    0,
    0.6 * alpha,
  );
  b.line(r.a.x, r.a.y, r.c.x, r.c.y, c.derived, 2.4, false, alpha);
  b.line(r.c.x, r.c.y, r.b.x, r.b.y, c.derived, 2.4, false, alpha);
  b.line(r.a.x, r.a.y, r.b.x, r.b.y, c.load, 2.6, false, alpha);
  if (alpha < 0.9) return;
  if (Math.abs(r.b.y - r.c.y) > 14) b.rightAngleMark(r.c, r.a, r.b);
  if (angleLabel !== null) b.angleArc(r.a.x, r.a.y, 30, 0, -r.alpha, angleLabel);
  if (labels) {
    if (oppLabel !== null) b.text(r.c.x + 8, (r.c.y + r.b.y) / 2 + 5, oppLabel, c.derived, 13, 'start');
    if (hypLabel !== null) {
      // popisek přepony nad její střed, odsunutý kolmo ven (doleva nahoru)
      const mx = (r.a.x + r.b.x) / 2;
      const my = (r.a.y + r.b.y) / 2;
      const s = Trig.sinD(r.alpha);
      const co = Trig.cosD(r.alpha);
      b.text(mx - 24 * s - 6, my - 24 * co, hypLabel, c.load, 13, 'end');
    }
  }
}

/** Dok: volby (Chips), hlavní tlačítko [button] a pod nimi krátká zpětná vazba [msg]. */
export function PickCheck({ options, selected, onSelect, button, msg, onCheck }: { options: L[]; selected: number | null; onSelect: (i: number) => void; button: L; msg: L | null; onCheck: () => void }) {
  return (
    <>
      <Chips options={options} selected={selected} onPick={onSelect} />
      <ActionButton text={button} enabled={selected !== null} onClick={onCheck} />
      {msg && <Note tone="nudge" text={msg} />}
    </>
  );
}

export const BTN_CHECK = L('Zkontrolovat', 'Проверить');
export const BTN_TEST = L('Ověřit', 'Проверить');

/**
 * Krok „tipni si“: volby + „Ověřit“. Odeslaný tip se jen zapíše ([onGuess]) – správnost ukáže až pokus v dalším kroku.
 */
export function useGuessStep(label: L, options: L[], guess: number | null, onGuess: (i: number) => void): Step {
  const kit = useKit();
  const sel = kit.mut<number | null>(null);
  return {
    label,
    done: guess !== null,
    result: () => (guess !== null ? <SmallText text={L('Tvůj tip: ' + options[guess].cs, 'Твой прогноз: ' + options[guess].ru)} /> : null),
    controls: () => (
      <>
        <Chips options={options} selected={sel.v} onPick={(i) => (sel.v = i)} />
        <ActionButton text={BTN_TEST} enabled={sel.v !== null} onClick={() => sel.v !== null && onGuess(sel.v)} />
      </>
    ),
  };
}
