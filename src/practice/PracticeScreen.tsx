/*
 * Procvičování kurzu trigonometrie: seznam sérií a průchod sérií (shodné s aplikací – learn/PracticeScreen.kt).
 */
import { useState } from 'react';
import { L, pick, useLang } from '../i18n';
import { Rich } from '../components/Sym';
import { HintButton, LearnLink, Modal, Notes, NumInput } from '../components/ui';
import { DonatePrompt } from '../components/Donate';
import { useStore } from '../trainer/store';
import { msg, type Msg } from '../trainer/session';
import { randomSeed } from '../model/rng';
import { Stage } from '../learn/kit/Stage';
import { Header } from '../learn/LearnRoot';
import type { LearnNav } from '../learn/nav';
import type { LearnProgress } from '../learn/progress';
import { PRACTICE_SETS, lessonsLabel, practiceSetById, type PracticeSet } from './sets';
import { MAX_MISSES, PracticeSession, available, practiceSessions, practiceStore, starFor } from './session';
import { FIG_H, drawFig } from './drawFig';
import type { Exercise, PMsg } from './exercise';

const LEVELS = [L('Lehká', 'Лёгкая'), L('Střední', 'Средняя'), L('Těžká', 'Трудная')];
const PRACTICE = L('Procvičování', 'Практика');
const LEVEL_NOTE = L(
  'Lehká: kulatá čísla, kalkulačka jen někde · Střední: s kalkulačkou · Těžká: úhly od svislice, znaménka',
  'Лёгкая: круглые числа, калькулятор нужен редко · Средняя: с калькулятором · Трудная: углы от вертикали, знаки',
);
const RU_SUFFIX: Record<string, string> = { kN: 'кН', kNm: 'кН·м', m: 'м', 'kN/m': 'кН/м' };

function usePick() {
  const lang = useLang();
  return (l: L) => pick(lang, l);
}

const starsLine = (set: string, p: (l: L) => string) => LEVELS.map((l, i) => p(l) + ' ' + (practiceStore.star(set, i + 1) ? '★' : '☆')).join(' · ');

const inProgress = (set: string) => {
  const s = practiceSessions.get(set);
  return s && !s.finished && s.tasks.length > 0 && s.started ? s : undefined;
};

const totalStars = () => PRACTICE_SETS.reduce((s, x) => s + practiceStore.stars(x.id), 0);

/** Seznam sérií procvičování (na úvodní obrazovce kurzu a v procvičování). */
export function PracticeSetList({ progress, onNav, sets, title = true }: { progress: LearnProgress; onNav: (n: LearnNav | null) => void; sets: PracticeSet[]; title?: boolean }) {
  const p = usePick();
  return (
    <div className="practice-list">
      {title && (
        <h2 className="practice-title">
          {p(PRACTICE)} · ★ {totalStars()} {p(L('z', 'из'))} {sets.length * 3}
        </h2>
      )}
      {sets.map((set) => {
        const ready = available(set, 1).length > 0 || available(set, 2).length > 0;
        const running = inProgress(set.id);
        return (
          <button key={set.id} className={'practice-item' + (running ? ' running' : '')} disabled={!ready} onClick={() => onNav({ kind: 'practice', set: set.id })}>
            <span className="lesson-text">
              <strong>{p(set.title)}</strong>
              <span className={'small' + (running ? '' : ' muted')}>
                {p(lessonsLabel(set))} · {p(L(`${set.size} úloh`, `${set.size} задач`))}
                {running && ' · ' + p(L(`Rozpracováno: úloha ${running.index + 1}/${running.tasks.length}`, `В процессе: задача ${running.index + 1}/${running.tasks.length}`))}
              </span>
              <span className="small muted">{starsLine(set.id, p)}</span>
              {!progress.isDone(set.afterLesson) && practiceStore.best(set.id) === 0 && (
                <span className="small muted">{p(L(`Doporučujeme nejdřív lekci ${set.afterLesson % 100}`, `Советуем сначала урок ${set.afterLesson % 100}`))}</span>
              )}
            </span>
            {practiceStore.star(set.id) && <span className="practice-star">★</span>}
          </button>
        );
      })}
    </div>
  );
}

/** Obrazovka procvičování: bez souboru seznam sérií, se souborem průchod sérií a výsledek. */
export function PracticeScreen({ nav, progress, onNav, back }: { nav: Extract<LearnNav, { kind: 'practice' }>; progress: LearnProgress; onNav: (n: LearnNav | null) => void; back: () => void }) {
  const p = usePick();
  const set = nav.set ? practiceSetById(nav.set) : undefined;
  if (!set)
    return (
      <div className="learn-page">
        <Header title={`${p(PRACTICE)} · ★ ${totalStars()} ${p(L('z', 'из'))} ${PRACTICE_SETS.length * 3}`} sub={p(L('Trigonometrie', 'Тригонометрия'))} onClose={back} />
        <div className="learn-body">
          <PracticeSetList progress={progress} onNav={onNav} sets={PRACTICE_SETS} title={false} />
        </div>
      </div>
    );
  return <SeriesScreen key={set.id} set={set} onNav={onNav} back={back} />;
}

function SeriesScreen({ set, onNav, back }: { set: PracticeSet; onNav: (n: LearnNav | null) => void; back: () => void }) {
  const p = usePick();
  const [, setVersion] = useState(0);
  let session = practiceSessions.get(set.id);
  if (!session) {
    session = new PracticeSession(set, practiceStore.level(set.id), randomSeed(), practiceStore);
    practiceSessions.set(set.id, session);
  }
  useStore(session);
  const restart = (level: number, seed = randomSeed()) => {
    practiceStore.setLevel(set.id, level);
    practiceSessions.set(set.id, new PracticeSession(set, level, seed, practiceStore));
    setVersion((v) => v + 1);
    window.scrollTo({ top: 0 });
  };
  const [askLevel, setAskLevel] = useState<number | null>(null);
  const ex = session.current;
  const s = session;
  return (
    <div className="learn-page">
      <Header
        title={`${p(PRACTICE)} · ${p(set.title)}`}
        sub={
          p(L(`Série ${s.seed}`, `Серия ${s.seed}`)) + ' · ' + p(LEVELS[s.level - 1]) + (!s.finished && ex ? ` · ${p(L('úloha', 'задача'))} ${s.index + 1}/${s.tasks.length}` : '')
        }
        onClose={back}
      />
      <div className="learn-body practice-body">
        {!ex ? (
          <p className="muted">{p(L('Tahle série se připravuje.', 'Эта серия готовится.'))}</p>
        ) : s.finished ? (
          <PracticeResult session={s} onAgain={() => restart(s.level)} onHarder={() => restart(Math.min(3, s.level + 1))} onList={() => onNav({ kind: 'practice' })} />
        ) : (
          <>
            <div className="chips">
              {LEVELS.map((l, i) => (
                <button
                  key={i}
                  className={'seg-btn' + (i + 1 === s.level ? ' seg-on' : '')}
                  onClick={() => {
                    if (i + 1 === s.level) return;
                    if (s.started) setAskLevel(i + 1);
                    else restart(i + 1);
                  }}
                >
                  {p(l)}
                </button>
              ))}
            </div>
            <p className="small muted">{p(LEVEL_NOTE)}</p>
            <PracticeTask key={s.seed + ':' + s.index} session={s} ex={ex} />
            <SeriesNumber current={s.seed} onStart={(n) => restart(s.level, n)} />
          </>
        )}
      </div>
      {askLevel !== null && (
        <Modal title={p(L('Začít novou sérii?', 'Начать новую серию?'))} onClose={() => setAskLevel(null)}>
          <p>{p(L(`Začít novou sérii na úrovni ${LEVELS[askLevel - 1].cs}? Rozdělaná série se zahodí.`, `Начать новую серию на уровне «${LEVELS[askLevel - 1].ru}»? Текущая серия пропадёт.`))}</p>
          <div className="actions">
            <button
              className="btn btn-primary"
              onClick={() => {
                const lv = askLevel;
                setAskLevel(null);
                restart(lv);
              }}
            >
              {p(L('Začít novou', 'Начать новую'))}
            </button>
            <button className="btn" onClick={() => setAskLevel(null)}>
              {p(L('Zůstat', 'Остаться'))}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}

/** číslo série: učitel zadá „série 4127“, žák ji tu spustí (stejné úlohy jako v aplikaci) */
function SeriesNumber({ current, onStart }: { current: number; onStart: (n: number) => void }) {
  const p = usePick();
  const [open, setOpen] = useState(false);
  const [text, setText] = useState('');
  const n = /^\d{1,6}$/.test(text.trim()) ? Number(text.trim()) : null;
  const ok = n !== null && n >= 1;
  const go = () => {
    if (!ok) return;
    setOpen(false);
    setText('');
    onStart(n!);
  };
  if (!open)
    return (
      <button className="btn btn-ghost series-no" onClick={() => setOpen(true)}>
        {p(L(`Série ${current} · zadat jiné číslo série`, `Серия ${current} · ввести другой номер серии`))}
      </button>
    );
  return (
    <div className="series-no">
      <div className="actions">
        <NumInput label={p(L('Číslo série', 'Номер серии'))} value={text} onChange={(v) => setText(v.replace(/\D/g, '').slice(0, 6))} onEnter={go} autoFocus width={110} />
        <button className="btn" onClick={go} disabled={!ok}>
          {p(L('Spustit', 'Запустить'))}
        </button>
      </div>
      <p className="small muted">{p(L('Stejné číslo = stejné úlohy (třeba od učitele).', 'Один номер — одни и те же задачи (например, от учителя).'))}</p>
    </div>
  );
}

const toMsgs = (ms: PMsg[], lang: 'cs' | 'ru'): Msg[] => ms.map((m) => msg(m.tone, () => pick(lang, m.text), m.learn ?? undefined));

function PracticeTask({ session, ex }: { session: PracticeSession; ex: Exercise }) {
  const lang = useLang();
  const p = (l: L) => pick(lang, l);
  const fig = session.solved ? (ex.solvedFig ?? ex.fig) : ex.fig;
  const choice = ex.choice;
  const picked = session.inputs[0] !== undefined && /^\d+$/.test(session.inputs[0]) ? Number(session.inputs[0]) : null;
  const signed = ex.fields.some((f) => f.signed);
  return (
    <div className="practice-task">
      {fig && (
        <div className="practice-fig">
          <Stage h={FIG_H} description={p(ex.prompt)} draw={(b, c) => drawFig(b, fig, c)} />
        </div>
      )}
      <p className="practice-prompt">
        <Rich text={p(ex.prompt)} />
      </p>
      {choice ? (
        <>
          <div className="practice-choices">
            {choice.map((o, i) => {
              const correctShown = session.solved && o.correct;
              const on = picked === i || correctShown;
              const cls = on ? (o.correct ? ' choice-ok' : ' choice-tried') : '';
              return (
                <button key={i} className={'choice choice-text' + cls} disabled={session.solved} onClick={() => session.choose(i)}>
                  <span>
                    <Rich text={p(o.label)} />
                  </span>
                </button>
              );
            })}
          </div>
          <p className="small muted">{p(L('Bod je jen za první pokus.', 'Балл — только с первой попытки.'))}</p>
        </>
      ) : (
        <>
          <div className="practice-fields">
            {ex.fields.map((f, i) => (
              <NumInput
                key={i}
                label={<Rich text={p(f.label)} />}
                value={session.inputs[i] ?? ''}
                onChange={(v) => session.setInput(i, v)}
                state={session.states[i] ?? null}
                suffix={f.unit ? p(L(f.unit, RU_SUFFIX[f.unit] ?? f.unit)) : undefined}
                signed={f.signed}
                onEnter={() => session.check()}
                autoFocus={i === 0}
              />
            ))}
          </div>
          <p className="small muted">
            {p(L('Zaokrouhli na 2 desetinná místa (úhly na 1). Čárka i tečka fungují.', 'Округляй до 2 знаков после запятой (углы — до 1). Можно и запятую, и точку.')) +
              (signed ? p(L(' Znaménko přepneš tlačítkem ± v poli.', ' Знак переключается кнопкой ± в поле.')) : '')}
          </p>
        </>
      )}
      <Notes msgs={toMsgs(session.msgs, lang)} />
      <div className="actions">
        {!session.solved ? (
          <>
            {!choice && (
              <button className="btn btn-primary" onClick={() => session.check()}>
                {p(L('Zkontrolovat', 'Проверить'))}
              </button>
            )}
            <HintButton level={session.hintLevel} onClick={() => session.hint()} lastLabel={p(L('Ukázat výsledek (bez bodu)', 'Показать ответ (без балла)'))} />
          </>
        ) : (
          <button className="btn btn-primary" autoFocus onClick={() => session.next()}>
            {p(L('Další', 'Дальше'))} →
          </button>
        )}
      </div>
    </div>
  );
}

function PracticeResult({ session, onAgain, onHarder, onList }: { session: PracticeSession; onAgain: () => void; onHarder: () => void; onList: () => void }) {
  const p = usePick();
  const size = session.tasks.length;
  const score = session.score;
  const hints = session.results.reduce((s, r) => s + r.hints, 0);
  const star = starFor(score, size);
  const weak = session.weakTypes();
  const levelName = LEVELS[session.level - 1];
  return (
    <div className="practice-result">
      <div className={'practice-big-star' + (star ? ' on' : '')}>{star ? '★' : '☆'}</div>
      <h2>{p(L(`Vyřešeno ${score} z ${size} · napoprvé ${session.firstTries} · nápovědy: ${hints}`, `Решено ${score} из ${size} · с первой попытки ${session.firstTries} · подсказок: ${hints}`))}</h2>
      <p className="muted">
        {p(
          star
            ? L(`Zvládnuto: ${levelName.cs} ★`, `Освоено: ${levelName.ru} ★`)
            : L(`Na ★ je potřeba aspoň ${Math.floor((size * 3 + 3) / 4)} úloh za bod.`, `Для ★ нужно не меньше ${Math.floor((size * 3 + 3) / 4)} задач с баллом.`),
        )}
      </p>
      <p className="small muted">
        {p(
          L(
            `Bod je za úlohu bez výsledku z nápovědy – u volby jen napoprvé, u čísel nejvýš po ${MAX_MISSES} chybách.`,
            `Балл — за задачу без готового ответа из подсказки: в выборе — только с первой попытки, в числах — не больше ${MAX_MISSES} ошибок.`,
          ),
        )}
      </p>
      <p className="small muted">{starsLine(session.set.id, p)}</p>
      {weak.length > 0 && (
        <div className="practice-weak">
          <h3>{p(L('Na co se ještě podívat', 'Что стоит повторить'))}</h3>
          {weak.map((t) => (
            <div key={t.id}>
              <div>• {p(t.title)}</div>
              {t.learn && <LearnLink lesson={t.learn[0]} card={t.learn[1]} />}
            </div>
          ))}
        </div>
      )}
      <div className="actions">
        {star && session.level < 3 ? (
          <>
            <button className="btn btn-primary" onClick={onHarder}>
              {p(L('Těžší úroveň', 'Уровень сложнее'))} →
            </button>
            <button className="btn" onClick={onAgain}>
              {p(L('Další série', 'Ещё серия'))}
            </button>
          </>
        ) : star ? (
          <button className="btn btn-primary" onClick={onAgain}>
            {p(L('Další série', 'Ещё серия'))}
          </button>
        ) : (
          <button className="btn btn-primary" onClick={onAgain}>
            {p(L(`Zkusit znovu (${levelName.cs})`, `Ещё раз (${levelName.ru})`))}
          </button>
        )}
        <button className="btn" onClick={onList}>
          {p(L('Seznam sérií', 'Список серий'))}
        </button>
      </div>
      {star && (
        <DonatePrompt
          text={p(
            L(
              'Hvězda je tvoje! Procvičování je zdarma a bez reklamy – jestli ti pomáhá, můžeš podpořit jeho další vývoj.',
              'Звезда твоя! Практика бесплатна и без рекламы — если она помогает, можно поддержать её развитие.',
            ),
          )}
        />
      )}
    </div>
  );
}
