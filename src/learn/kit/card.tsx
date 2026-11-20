/*
 * Stavebnice karet výkladu (learn/Kit.kt, learn/Dock.kt, learn/NumAnswer.kt v aplikaci):
 * stav karty (úkoly a zámek „Dál“), kroky v doku dole, kvízy, číselné odpovědi a texty.
 */
import { createContext, useContext, useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { L, pick, useLang, type Lang } from '../../i18n';
import { Rich } from '../../components/Sym';
import { Store, useStore } from '../../trainer/store';
import type { Tone } from '../../trainer/session';
import { fmt, parseNum, toggleSign } from '../../format';
import { accept, type Tol } from '../../practice/tol';
import { useKit } from './hooks';
import { useLearn } from '../nav';
import { COURSES, LESSON_META, courseOf } from '../titles';

export { L };

// ------------------------------------------------------------- jazyk

/** Překlad L → text (obdoba `tr().pick(…)` v aplikaci). */
export function useTr() {
  const lang = useLang();
  return { lang, pick: (l: L) => pick(lang, l) };
}

export const pickL = (lang: Lang, l: L) => pick(lang, l);

// ------------------------------------------------------------- stav karty

type Item = [L, boolean];

/**
 * Stav karty. Každá karta říká, co má žák udělat (kroky, úkoly, otázky); „Dál“ se odemkne, až je hotové všechno.
 * Úkoly hlásí svůj stav přes [report].
 */
export class CardScope extends Store {
  private parts = new Map<string, Item[]>();
  readonly alreadyDone: boolean;
  /** vše splněno právě teď (ne při dřívější návštěvě) – jen podle toho smí obrázek ukázat řešení */
  solvedHere = false;
  /** zvýší se, když žák zkusí jít dál dřív, než je hotovo */
  nag = 0;

  constructor(alreadyDone: boolean) {
    super();
    this.alreadyDone = alreadyDone;
  }

  get pending(): L[] {
    return [...this.parts.values()].flat().filter((x) => !x[1]).map((x) => x[0]);
  }

  private get allDone() {
    return this.parts.size > 0 && [...this.parts.values()].every((l) => l.every((x) => x[1]));
  }

  /** smí se jít dál: úkoly jednou splněné v této návštěvě, nebo karta splněná dřív */
  get done() {
    return this.alreadyDone || this.solvedHere;
  }

  report(key: string, items: Item[]) {
    const prev = this.parts.get(key);
    const same = prev && prev.length === items.length && prev.every((x, i) => x[1] === items[i][1] && x[0].cs === items[i][0].cs);
    if (!same) this.parts.set(key, items);
    if (this.allDone && !this.solvedHere) {
      this.solvedHere = true;
      this.emit();
    } else if (!same) this.emit();
  }

  unreport(key: string) {
    if (this.parts.delete(key)) this.emit();
  }

  doNag() {
    this.nag++;
    this.emit();
  }
}

export const CardContext = createContext<CardScope>(new CardScope(false));
export const useCard = () => useContext(CardContext);

/** Stav karty s překreslením při změně (pro `solvedHere` – obrázek smí ukázat řešení až po splnění). */
export function useCardState(): CardScope {
  const s = useContext(CardContext);
  useStore(s);
  return s;
}

/** Místo pro dok (spodní panel) na obrazovce lekce; mimo lekci (testy) se dok vykreslí na místě. */
export const DockContext = createContext<HTMLElement | null>(null);
/** true uvnitř doku – akční tlačítko je tam hlavní (plné), ve výkladu vedlejší */
const InDockContext = createContext(false);

/** Hlásí kartě stav úkolů (SideEffect { report(…) }). */
function useReport(key: string, items: Item[]) {
  const scope = useCard();
  const sig = items.map((x) => x[0].cs + (x[1] ? '1' : '0')).join('|');
  useLayoutEffect(() => {
    scope.report(key, items);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [scope, key, sig]);
  useEffect(() => () => scope.unreport(key), [scope, key]);
}

/** Při nag (žák chtěl dál, ale [active] úkol ještě není hotový) prvek zabliká a posune se do pohledu. */
export function useNagTarget<T extends HTMLElement>(active: boolean) {
  const scope = useCard();
  const ref = useRef<T>(null);
  const seen = useRef(scope.nag);
  const [flash, setFlash] = useState(0);
  useEffect(() => {
    const on = () => {
      if (scope.nag === seen.current) return;
      seen.current = scope.nag;
      if (!active) return;
      ref.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
      setFlash((f) => f + 1);
    };
    return scope.subscribe(on);
  }, [scope, active]);
  return { ref, className: flash ? `nag nag-${flash % 2}` : '' };
}

// ------------------------------------------------------------- dok a kroky

/** Obsah doku (Dock.kt Dock). */
export function Dock({ children }: { children: ReactNode }) {
  const host = useContext(DockContext);
  const content = <InDockContext.Provider value={true}>{children}</InDockContext.Provider>;
  if (!host) return content;
  return createPortal(content, host);
}

/**
 * Jeden krok karty: [label] = krátký rozkaz, [done] = splněno, [controls] = ovládání v doku jen pro tento krok,
 * [result] = co se po splnění ukáže ve výkladu (vysvětlení).
 */
export interface Step {
  label: L;
  done: boolean;
  result?: () => ReactNode;
  controls?: () => ReactNode;
}

export const step = (label: L, done: boolean, controls?: () => ReactNode, result?: () => ReactNode): Step => ({ label, done, controls, result });

const plural = (n: number, lang: Lang) =>
  lang === 'cs'
    ? `… a pak ještě ${n} ${n === 1 ? 'krok' : n >= 2 && n <= 4 ? 'kroky' : 'kroků'}`
    : `… и потом ещё ${n} ${n % 10 === 1 && n % 100 !== 11 ? 'шаг' : n % 10 >= 2 && n % 10 <= 4 && !(n % 100 >= 12 && n % 100 <= 14) ? 'шага' : 'шагов'}`;

/**
 * Kroky karty po jednom. Ve výkladu: hotové kroky ✓ (s výsledkem) a aktuální krok zvýrazněný.
 * V doku: „Krok 2 ze 3“, rozkaz a ovládání aktuálního kroku. Po splnění všech dok ukáže [after].
 */
export function Steps({ steps, after }: { steps: Step[]; after?: () => ReactNode }) {
  const { lang, pick: p } = useTr();
  useReport(
    'steps',
    steps.map((s) => [s.label, s.done] as Item),
  );
  const cur = steps.findIndex((s) => !s.done);
  const all = cur < 0;
  const curRef = useRef<HTMLDivElement>(null);
  const first = useRef(true);
  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    if (cur > 0) {
      const id = setTimeout(() => curRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' }), 350);
      return () => clearTimeout(id);
    }
  }, [cur]);
  const nag = useNagTarget<HTMLDivElement>(!all);
  return (
    <>
      <div className="steps">
        {steps.map((s, i) => {
          if (!all && i > cur) return null;
          if (s.done)
            return (
              <div key={i}>
                <StepLine no={i + 1} label={s.label} done current={false} />
                {s.result?.()}
              </div>
            );
          return (
            <div key={i} ref={curRef}>
              <StepLine no={i + 1} label={s.label} done={false} current />
            </div>
          );
        })}
        {!all && cur < steps.length - 1 && <div className="steps-more small muted">{plural(steps.length - cur - 1, lang)}</div>}
      </div>
      <Dock>
        <div ref={nag.ref} className={'dock-step ' + nag.className} key={all ? 'after' : cur}>
          {all ? (
            <>
              <div className="dock-done">{p(L('Hotovo ✓ – pokračuj tlačítkem Dál', 'Готово ✓ — жми «Дальше»'))}</div>
              {after?.()}
            </>
          ) : (
            <>
              {steps.length > 1 && <div className="dock-count">{p(L(`KROK ${cur + 1} ZE ${steps.length}`, `ШАГ ${cur + 1} ИЗ ${steps.length}`))}</div>}
              <div className="dock-label">
                <Rich text={p(steps[cur].label)} />
              </div>
              {steps[cur].controls?.()}
            </>
          )}
        </div>
      </Dock>
    </>
  );
}

function StepLine({ no, label, done, current }: { no: number; label: L; done: boolean; current: boolean }) {
  const { pick: p } = useTr();
  return (
    <div className={'step-line' + (done ? ' done' : '') + (current ? ' current' : '')}>
      <span className="step-no">{done ? '✓' : no}</span>
      <span className="step-text">
        <Rich text={p(label) + (current ? p(L('  ↓ dole', '  ↓ внизу')) : '')} />
      </span>
    </div>
  );
}

// ------------------------------------------------------------- texty a ovládání

/** Hlavní text karty – krátký, se značkami [[F_x]] a **tučně**. */
export function CardText({ text }: { text: L }) {
  const { pick: p } = useTr();
  return (
    <p className="card-text">
      <Rich text={p(text)} />
    </p>
  );
}

/** Menší doplňující text. */
export function SmallText({ text }: { text: L }) {
  const { pick: p } = useTr();
  return (
    <p className="card-small">
      <Rich text={p(text)} />
    </p>
  );
}

/** Velký živý údaj pod obrázkem, např. „M = 20 · 0,4 = 8 kNm“. */
export function Readout({ text, ok = false }: { text: string; ok?: boolean }) {
  return (
    <div className={'readout' + (ok ? ' ok' : '')}>
      <Rich text={text} />
    </div>
  );
}

const icons: Record<Tone, string> = { nudge: '→', ok: '✓', info: 'i', hint: '?' };

/** Zpráva žákovi (stejný vzhled jako v trenažéru). */
export function Note({ tone, text }: { tone: Tone; text: L }) {
  const { pick: p } = useTr();
  return (
    <div className={`note note-${tone}`}>
      <span className="note-icon" aria-hidden>
        {icons[tone]}
      </span>
      <div>
        <Rich text={p(text)} />
      </div>
    </div>
  );
}

/** Řada tlačítek-voleb. Velké terče, žádné drobné tahy. */
export function Chips({ options, selected = null, selectedSet, onPick }: { options: L[]; selected?: number | null; selectedSet?: Set<number>; onPick: (i: number) => void }) {
  const { pick: p } = useTr();
  return (
    <div className="kchips">
      {options.map((o, i) => {
        const on = i === selected || !!selectedSet?.has(i);
        return (
          <button key={i} type="button" className={'kchip' + (on ? ' on' : '')} aria-pressed={on} onClick={() => onPick(i)}>
            <Rich text={p(o)} />
          </button>
        );
      })}
    </div>
  );
}

/** Posuvník s popiskem; [steps] = počet mezikroků (přichytávání). */
export function ValueSlider({ label, value, min, max, steps = 0, onChange }: { label: string; value: number; min: number; max: number; steps?: number; onChange: (v: number) => void }) {
  const st = steps > 0 ? (max - min) / (steps + 1) : (max - min) / 1000;
  return (
    <label className="kslider">
      <span>
        <Rich text={label} />
      </span>
      <input type="range" min={min} max={max} step={st} value={value} onChange={(e) => onChange(Number(e.target.value))} />
    </label>
  );
}

/** Akční tlačítko na kartě („Odstraň podporu“, „Nahraď“ …); v doku hlavní, ve výkladu vedlejší. */
export function ActionButton({ text, enabled = true, onClick }: { text: L; enabled?: boolean; onClick: () => void }) {
  const inDock = useContext(InDockContext);
  const { pick: p } = useTr();
  return (
    <div className="kaction">
      <button type="button" className={'btn' + (inDock ? ' btn-primary' : '')} style={{ opacity: enabled ? 1 : 0.4 }} onClick={() => enabled && onClick()}>
        {p(text)}
      </button>
    </div>
  );
}

/**
 * Úkoly karty – výrazný rámeček „Úkol“ s fajfkami. Karta jde dokončit, až jsou splněné všechny.
 */
export function Todo({ items }: { items: Item[] }) {
  const { pick: p } = useTr();
  useReport('todo', items);
  const all = items.every((x) => x[1]);
  const nag = useNagTarget<HTMLDivElement>(!all);
  return (
    <div ref={nag.ref} className={'todo' + (all ? ' all' : '') + ' ' + nag.className}>
      <div className="todo-title">{p(all ? L('Hotovo ✓', 'Готово ✓') : items.length > 1 ? L('Úkoly', 'Задания') : L('Úkol', 'Задание'))}</div>
      {items.map(([label, ok], i) => (
        <div key={i} className={'todo-item' + (ok ? ' ok' : '')}>
          <span className="todo-box">{ok ? '✓' : ''}</span>
          <span>
            <Rich text={p(label)} />
          </span>
        </div>
      ))}
    </div>
  );
}

// ------------------------------------------------------------- kvíz

function Feedback({ msg, extra }: { msg: [Tone, L] | null; extra: L | null }) {
  return (
    <>
      {msg && <Note tone={msg[0]} text={msg[1]} />}
      {extra && <Note tone="hint" text={extra} />}
    </>
  );
}

/** Možnost v kvízu. [why] = cílená nápověda, když žák zvolí právě tuhle chybnou možnost. */
export interface Opt {
  label: L;
  correct?: boolean;
  why?: L | null;
}

export const opt = (label: L, correct = false, why: L | null = null): Opt => ({ label, correct, why });

const LOOK_AGAIN = L('Podívej se ještě jednou na obrázek.', 'Посмотри ещё раз на рисунок.');

/** Stav kvízu. Žebříček: 1. chyba → nápověda k té volbě, 2. → navíc [nudge], 3. → ukáže odpověď s vysvětlením. */
export function useQuiz(question: L, options: Opt[], explain: L, nudge: L | null = null) {
  const kit = useKit();
  const misses = kit.mut(0);
  const picked = kit.mut<number | null>(null);
  const solved = kit.mut(false);
  const msg = kit.mut<[Tone, L] | null>(null);
  const extra = kit.mut<L | null>(null);
  // nová otázka (jiný klíč) = nový stav, jako remember(question.cs) v aplikaci
  const keyBox = kit.remember(() => ({ key: question.cs }));
  if (keyBox.key !== question.cs) {
    keyBox.key = question.cs;
    misses.v = 0;
    picked.v = null;
    solved.v = false;
    msg.v = null;
    extra.v = null;
  }
  const correct = options.findIndex((o) => o.correct);
  const pickOpt = (i: number) => {
    if (solved.v) return;
    picked.v = i;
    const o = options[i];
    if (o.correct) {
      solved.v = true;
      extra.v = null;
      msg.v = ['ok', explain];
      return;
    }
    misses.v++;
    // vždy nejdřív nápověda k právě zvolené chybné možnosti
    const why = o.why ?? nudge ?? LOOK_AGAIN;
    if (misses.v >= 3) {
      solved.v = true;
      msg.v = ['nudge', why];
      extra.v = explain;
    } else if (misses.v === 2 && nudge && nudge.cs !== why.cs) {
      msg.v = ['nudge', why];
      extra.v = nudge;
    } else {
      msg.v = ['nudge', why];
      extra.v = null;
    }
  };
  return {
    question,
    get solved() {
      return solved.v;
    },
    get picked() {
      return picked.v;
    },
    get misses() {
      return misses.v;
    },
    correct,
    pick: pickOpt,
    Options: (columns = 1) => <QuizOptions options={options} picked={picked.v} solved={solved.v} correct={correct} columns={columns} onPick={pickOpt} />,
    Feedback: () => <Feedback msg={msg.v} extra={extra.v} />,
  };
}

export type QuizState = ReturnType<typeof useQuiz>;

function QuizOptions({ options, picked, solved, correct, columns, onPick }: { options: Opt[]; picked: number | null; solved: boolean; correct: number; columns: number; onPick: (i: number) => void }) {
  const { pick: p } = useTr();
  return (
    <div className="quiz-opts" style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }}>
      {options.map((o, i) => {
        const sel = picked === i || (solved && i === correct);
        const ok = i === correct && sel;
        const enabled = !solved || i === correct;
        return (
          <button key={i} type="button" className={'choice quiz-opt' + (sel ? (ok ? ' choice-ok' : ' choice-tried') : '')} disabled={!enabled} onClick={() => onPick(i)}>
            <span>
              <Rich text={p(o.label)} />
            </span>
          </button>
        );
      })}
    </div>
  );
}

/** Kvíz jako krok: otázka a volby v doku, po vyřešení vysvětlení ve výkladu. */
export function useQuizStep(question: L, options: Opt[], explain: L, nudge: L | null = null, columns = 1, label: L = question): Step {
  const q = useQuiz(question, options, explain, nudge);
  const { pick: p } = useTr();
  return {
    label,
    done: q.solved,
    result: () => q.Feedback(),
    controls: () => (
      <>
        {label.cs !== question.cs && (
          <p className="dock-q">
            <Rich text={p(question)} />
          </p>
        )}
        {q.Options(columns)}
        {!q.solved && q.Feedback()}
      </>
    ),
  };
}

const QUIZ_TASK = L('Odpověz na otázku', 'Ответь на вопрос');

/** Kvíz přímo ve výkladu (Kit.kt Quiz). */
export function Quiz({ question, options, explain, nudge = null, columns = 1 }: { question: L; options: Opt[]; explain: L; nudge?: L | null; columns?: number }) {
  const q = useQuiz(question, options, explain, nudge);
  const { pick: p } = useTr();
  useReport('quiz:' + question.cs, [[QUIZ_TASK, q.solved]]);
  const nag = useNagTarget<HTMLDivElement>(!q.solved);
  return (
    <>
      <div ref={nag.ref} className={'quiz-q ' + nag.className}>
        <Rich text={p(question)} />
      </div>
      {q.Options(columns)}
      {q.Feedback()}
    </>
  );
}

// ------------------------------------------------------------- číselná odpověď

/** Číselné pole karty. [signed] = se znaménkem (tlačítko ±; jinak se znaménko ignoruje), [tol] = jak přísně kontrolovat. */
export interface NumSpec {
  label: L;
  answer: number;
  unit: string;
  signed: boolean;
  tol: Tol;
}

export const numSpec = (label: L, answer: number, unit = '', signed = false, tol: Tol = 'value'): NumSpec => ({ label, answer, unit, signed, tol });

/** Typická chyba v kartě: [match] dostane přečtené hodnoty polí (null = prázdné), [why] = cílená nápověda. */
export interface NumTrap {
  why: L;
  match: (v: (number | null)[]) => boolean;
}

export const numTrap = (why: L, match: (v: (number | null)[]) => boolean): NumTrap => ({ why, match });

/** chyba poznaná podle hodnot, které dává (±1 % / 0,02; bez ohledu na znaménko) */
export const trapNear = (why: L, ...wrong: (number | null)[]): NumTrap => ({
  why,
  match: (v) => wrong.every((w, i) => w === null || (v[i] != null && Math.abs(Math.abs(v[i]!) - Math.abs(w)) <= Math.max(0.02, Math.abs(w) * 0.01))),
});

/** Stav číselné odpovědi. Žebříček jako u kvízu; „Nevím“: 1. = nápověda, 2. = celý postup a výsledek. */
export function useNum(key: L, fields: NumSpec[], explain: L, traps: NumTrap[] = [], nudge: L | null = null) {
  const kit = useKit();
  const values = kit.mut<string[]>(() => fields.map(() => ''));
  const states = kit.mut<('ok' | 'nudge' | null)[]>(() => fields.map(() => null));
  const misses = kit.mut(0);
  const solved = kit.mut(false);
  const msg = kit.mut<[Tone, L] | null>(null);
  const extra = kit.mut<L | null>(null);
  const gaveUp = kit.mut(0);
  // nová otázka (jiný klíč) = nový stav, jako remember(key.cs) v aplikaci
  const keyBox = kit.remember(() => ({ key: key.cs }));
  if (keyBox.key !== key.cs) {
    keyBox.key = key.cs;
    values.v = fields.map(() => '');
    states.v = fields.map(() => null);
    misses.v = 0;
    solved.v = false;
    msg.v = null;
    extra.v = null;
    gaveUp.v = 0;
  }

  const ok = (i: number, s: string) => {
    const v = parseNum(s);
    if (v === null) return false;
    const f = fields[i];
    return f.signed ? accept(s, v, f.answer, f.tol) : accept(s, Math.abs(v), Math.abs(f.answer), f.tol);
  };
  const reveal = () => {
    values.v = fields.map((f) => fmt(f.answer, 2));
    states.v = fields.map(() => 'ok');
  };
  const giveUp = () => {
    if (solved.v) return;
    gaveUp.v++;
    if (gaveUp.v === 1 && nudge) {
      msg.v = ['hint', nudge];
      extra.v = null;
      return;
    }
    solved.v = true;
    reveal();
    msg.v = ['info', explain];
    extra.v = null;
  };
  const check = () => {
    if (solved.v) return;
    const st = values.v.map((s, i) => (ok(i, s) ? 'ok' : 'nudge') as 'ok' | 'nudge');
    states.v = st;
    if (st.every((x) => x === 'ok')) {
      solved.v = true;
      msg.v = ['ok', explain];
      extra.v = null;
      return;
    }
    if (values.v.some((x) => parseNum(x) === null)) {
      msg.v = ['nudge', L('Doplň všechna pole.', 'Заполни все поля.')];
      return;
    }
    misses.v++;
    const parsed = values.v.map((x) => parseNum(x));
    const why = traps.find((t) => t.match(parsed))?.why ?? nudge ?? L('Ještě to nesedí – podívej se znovu na obrázek.', 'Пока не сходится — посмотри ещё раз на рисунок.');
    if (misses.v >= 3) {
      solved.v = true;
      reveal();
      msg.v = ['nudge', why];
      extra.v = explain;
    } else if (misses.v === 2 && nudge && nudge.cs !== why.cs) {
      msg.v = ['nudge', why];
      extra.v = nudge;
    } else {
      msg.v = ['nudge', why];
      extra.v = null;
    }
  };
  const setValue = (i: number, v: string) => {
    if (solved.v) return;
    const nv = [...values.v];
    nv[i] = v;
    values.v = nv;
    if (states.v[i] === 'nudge') {
      const ns = [...states.v];
      ns[i] = null;
      states.v = ns;
    }
  };
  return {
    get solved() {
      return solved.v;
    },
    get values() {
      return values.v;
    },
    get misses() {
      return misses.v;
    },
    check,
    giveUp,
    Fields: () => <NumFields fields={fields} values={values.v} states={states.v} onChange={setValue} onDone={check} />,
    Buttons: () => (solved.v ? null : <NumButtons onCheck={check} onGiveUp={giveUp} canNudge={gaveUp.v === 0 && !!nudge} />),
    Feedback: () => <Feedback msg={msg.v} extra={extra.v} />,
  };
}

export type NumState = ReturnType<typeof useNum>;

function NumFields({ fields, values, states, onChange, onDone }: { fields: NumSpec[]; values: string[]; states: ('ok' | 'nudge' | null)[]; onChange: (i: number, v: string) => void; onDone: () => void }) {
  const { pick: p } = useTr();
  const lang = useLang();
  return (
    <div className="num-fields">
      {fields.map((f, i) => {
        const st = states[i];
        return (
          <label key={i} className="num">
            <span className="num-label">
              <Rich text={p(f.label)} />
            </span>
            <span className="num-row">
              <span className={'num-field' + (st ? ' in-' + st : '')}>
                <input
                  inputMode="decimal"
                  autoComplete="off"
                  value={values[i]}
                  readOnly={st === 'ok'}
                  style={{ width: 110 }}
                  onChange={(e) => onChange(i, e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key !== 'Enter') return;
                    const next = (e.currentTarget.closest('.num-fields')?.querySelectorAll('input')[i + 1] as HTMLInputElement | undefined) ?? null;
                    if (next) next.focus();
                    else onDone();
                  }}
                />
                {f.signed && st !== 'ok' && (
                  <button type="button" className="sign-btn" aria-label={lang === 'ru' ? 'Сменить знак' : 'Změnit znaménko'} onClick={() => onChange(i, toggleSign(values[i]))}>
                    ±
                  </button>
                )}
              </span>
              {f.unit && <span className="unit">{f.unit}</span>}
              {st === 'ok' && <span className="check">✓</span>}
            </span>
          </label>
        );
      })}
    </div>
  );
}

function NumButtons({ onCheck, onGiveUp, canNudge }: { onCheck: () => void; onGiveUp: () => void; canNudge: boolean }) {
  const { pick: p } = useTr();
  return (
    <div className="num-buttons">
      <button type="button" className="btn btn-primary" onClick={onCheck}>
        {p(L('Zkontrolovat', 'Проверить'))}
      </button>
      <button type="button" className="btn" onClick={onGiveUp}>
        {p(canNudge ? L('Nevím', 'Не знаю') : L('Ukaž postup', 'Покажи решение'))}
      </button>
    </div>
  );
}

/** Číselná odpověď jako krok: otázka, pole a tlačítka v doku, po vyřešení postup ve výkladu. */
export function useNumStep(question: L, fields: NumSpec[], explain: L, traps: NumTrap[] = [], nudge: L | null = null, label: L = question): Step {
  const n = useNum(question, fields, explain, traps, nudge);
  const { pick: p } = useTr();
  return {
    label,
    done: n.solved,
    result: () => n.Feedback(),
    controls: () => (
      <>
        {label.cs !== question.cs && (
          <p className="dock-q">
            <Rich text={p(question)} />
          </p>
        )}
        {n.Fields()}
        {n.Buttons()}
        {!n.solved && n.Feedback()}
      </>
    ),
  };
}

const NUM_TASK = L('Spočítej a zapiš výsledek', 'Посчитай и запиши результат');

/** Úloha s číselnou odpovědí přímo ve výkladu (NumAnswer.kt). */
export function NumAnswer({ question, fields, explain, traps = [], nudge = null }: { question: L; fields: NumSpec[]; explain: L; traps?: NumTrap[]; nudge?: L | null }) {
  const n = useNum(question, fields, explain, traps, nudge);
  const { pick: p } = useTr();
  useReport('num:' + question.cs, [[NUM_TASK, n.solved]]);
  const nag = useNagTarget<HTMLDivElement>(!n.solved);
  return (
    <>
      <div ref={nag.ref} className={'quiz-q ' + nag.className}>
        <Rich text={p(question)} />
      </div>
      {n.Fields()}
      {n.Buttons()}
      {n.Feedback()}
    </>
  );
}

/**
 * Odkaz uvnitř výkladu na jinou kartu (i do jiného kurzu): „📖 …“. Po zavření cílové lekce se žák vrátí sem.
 */
export function LearnJump({ lesson, card = 0, label }: { lesson: number; card?: number; label?: L }) {
  const learn = useLearn();
  const { pick: p } = useTr();
  const meta = LESSON_META[lesson];
  const course = courseOf(lesson);
  const text = label ? p(label) : p(course.title) + ': ' + (meta ? p(meta.title) : '');
  return (
    <button type="button" className="learn-link" onClick={() => learn.open({ kind: 'open', lesson, card, returnTo: learn.here ?? undefined })}>
      📖 {text}
    </button>
  );
}

export { COURSES };
