import { Chips, L, SmallText, type Step } from './card';

/**
 * Tip před pokusem: krok se splní jakoukoli volbou, správnost ukáže až následující pokus.
 * Ve výkladu pak zůstane „Tvůj tip: …“.
 */
export function tipStep(label: L, options: L[], pick: number | null, onPick: (i: number) => void): Step {
  return {
    label,
    done: pick !== null,
    result: () => (pick !== null ? <SmallText text={L(`Tvůj tip: **${options[pick].cs}**`, `Твой прогноз: **${options[pick].ru}**`)} /> : null),
    controls: () => <Chips options={options} selected={pick} onPick={(i) => pick === null && onPick(i)} />,
  };
}
