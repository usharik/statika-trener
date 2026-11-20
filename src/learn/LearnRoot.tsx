import { useEffect, useMemo, useRef, useState, type ComponentType } from 'react';
import { L, useLang, pick } from '../i18n';
import { Rich } from '../components/Sym';
import { useStore } from '../trainer/store';
import { LearnContext, useLearn, type LearnNav } from './nav';
import type { LearnProgress } from './progress';
import { COURSES, LESSON_META, courseOf, type CourseMeta } from './titles';
import { CardContext, CardScope, DockContext } from './kit/card';
import { ColorsContext, DARK, LIGHT } from './kit/colors';
import { GLOSSARY, TRIG_GLOSSARY } from './glossary';
import { LESSON_CARDS } from './lessons';
import { PRACTICE_SETS, practiceAfterLesson } from '../practice/sets';
import { PracticeScreen, PracticeSetList } from '../practice/PracticeScreen';
import { DonatePrompt } from '../components/Donate';

/**
 * Výklad přes celou obrazovku nad trenažérem (trenažér zůstává pod ním i s rozpracovanou úlohou).
 * „Zpět“: z lekce na seznam (nebo rovnou k úloze, když byla otevřena z ní), ze seznamu do trenažéru.
 */
export default function LearnRoot({ nav, progress, onNav }: { nav: LearnNav; progress: LearnProgress; onNav: (n: LearnNav | null) => void }) {
  useStore(progress);
  const dark = document.documentElement.dataset.theme === 'dark';
  const back = () => {
    if (nav.kind === 'open') onNav(nav.returnTo ?? (nav.fromTrainer ? null : { kind: 'home' }));
    else if (nav.kind === 'practice') onNav(nav.returnTo ?? (nav.set ? { kind: 'practice' } : { kind: 'home' }));
    else if (nav.kind === 'glossary') onNav({ kind: 'home' });
    else onNav(null);
  };
  const backRef = useRef(back);
  backRef.current = back;

  // lekce otevřená odkazem přepne kurz, aby „zpět“ vedlo do správného seznamu
  useEffect(() => {
    if (nav.kind === 'open') progress.course = courseOf(nav.lesson).id;
    if (nav.kind === 'practice') progress.course = 'tg';
  }, [nav, progress]);

  // tlačítko / gesto „zpět“ v prohlížeči zavírá výklad krok po kroku
  const active = useRef(false);
  useEffect(() => {
    active.current = true;
    if (!history.state?.learn) history.pushState({ learn: true }, '', location.href);
    const on = (e: PopStateEvent) => {
      if (e.state?.learn) return;
      backRef.current();
      setTimeout(() => {
        if (active.current && !history.state?.learn) history.pushState({ learn: true }, '', location.href);
      }, 0);
    };
    window.addEventListener('popstate', on);
    return () => {
      active.current = false;
      window.removeEventListener('popstate', on);
      setTimeout(() => {
        if (!active.current && history.state?.learn) history.back();
      }, 0);
    };
  }, []);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !document.querySelector('.modal-back')) backRef.current();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, [nav]);

  const api = useMemo(() => ({ open: (n: LearnNav) => onNav(n), here: nav }), [nav, onNav]);
  return (
    <ColorsContext.Provider value={dark ? DARK : LIGHT}>
      <LearnContext.Provider value={api}>
        <div className="learn">
          {nav.kind === 'home' && <LearnHome progress={progress} onNav={onNav} />}
          {nav.kind === 'glossary' && <Glossary progress={progress} back={back} />}
          {nav.kind === 'open' && <LessonScreen key={nav.lesson + ':' + nav.card} nav={nav} progress={progress} onNav={onNav} back={back} />}
          {nav.kind === 'practice' && <PracticeScreen nav={nav} progress={progress} onNav={onNav} back={back} />}
        </div>
      </LearnContext.Provider>
    </ColorsContext.Provider>
  );
}

export function Header({ title, sub, onClose }: { title: string; sub?: string | null; onClose: () => void }) {
  const lang = useLang();
  return (
    <div className="learn-head">
      <button className="icon-btn" onClick={onClose} aria-label={pick(lang, L('Zavřít', 'Закрыть'))} title={pick(lang, L('Zavřít', 'Закрыть'))}>
        ✕
      </button>
      <div className="learn-head-text">
        <h1>
          <Rich text={title} />
        </h1>
        {sub && <div className="small muted">{sub}</div>}
      </div>
    </div>
  );
}

function LearnHome({ progress, onNav }: { progress: LearnProgress; onNav: (n: LearnNav | null) => void }) {
  const lang = useLang();
  const p = (l: L) => pick(lang, l);
  const course = progress.courseMeta;
  const next = progress.next(course);
  const allDone = course.lessons.every((l) => progress.isDone(l));
  const hasPractice = course.id === 'tg';
  return (
    <div className="learn-page">
      <Header title={p(course.title)} sub={p(course.sub)} onClose={() => onNav(null)} />
      <div className="learn-body">
        <div className="chips">
          {COURSES.map((k) => (
            <button key={k.id} className={'seg-btn course-chip' + (k.id === course.id ? ' seg-on' : '')} onClick={() => (progress.course = k.id)}>
              {p(k.title)}
            </button>
          ))}
        </div>
        <div className="actions">
          <button className="btn btn-primary" onClick={() => onNav({ kind: 'open', lesson: next, card: 0 })}>
            {p(allDone ? L('Začít znovu', 'Начать заново') : L('Pokračovat', 'Продолжить'))} →
          </button>
          {hasPractice && (
            <button className="btn" onClick={() => onNav({ kind: 'practice' })}>
              {p(L('Procvičování', 'Практика'))}
            </button>
          )}
          <button className="btn" onClick={() => onNav({ kind: 'glossary' })}>
            {p(L('Slovníček', 'Словарик'))}
          </button>
        </div>
        <div className="lesson-list">
          {course.lessons.map((id) => {
            const done = progress.isDone(id);
            const seen = progress.seen(id);
            const meta = LESSON_META[id];
            const total = LESSON_CARDS[id]?.length ?? 0;
            return (
              <button key={id} className={'lesson-item' + (id === next && !done ? ' next' : '')} onClick={() => onNav({ kind: 'open', lesson: id, card: 0 })}>
                <span className={'lesson-no' + (done ? ' done' : seen > 0 ? ' seen' : '')}>{done ? '✓' : id % 100}</span>
                <span className="lesson-text">
                  <strong>{p(meta.title)}</strong>
                  <span className="small muted">
                    <Rich text={p(meta.summary)} />
                  </span>
                </span>
                {!done && seen > 0 && total > 0 && (
                  <span className="small muted">
                    {seen}/{total}
                  </span>
                )}
              </button>
            );
          })}
        </div>
        {hasPractice && <PracticeSetList progress={progress} onNav={onNav} sets={PRACTICE_SETS} />}
        {allDone && <DonatePrompt text={p(L('Celý kurz máš za sebou – gratuluju! Výklad je zdarma a bez reklamy; jestli ti pomohl, můžeš podpořit autora.', 'Весь курс пройден — поздравляю! Уроки бесплатны и без рекламы; если они помогли, можно поддержать автора.'))} />}
      </div>
    </div>
  );
}

function LessonScreen({ nav, progress, onNav, back }: { nav: Extract<LearnNav, { kind: 'open' }>; progress: LearnProgress; onNav: (n: LearnNav | null) => void; back: () => void }) {
  const lang = useLang();
  const p = (l: L) => pick(lang, l);
  const learn = useLearn();
  const cards: ComponentType[] = LESSON_CARDS[nav.lesson] ?? [];
  const meta = LESSON_META[nav.lesson];
  const [index, setIndex] = useState(() => Math.max(0, Math.min(nav.card, cards.length - 1)));
  const last = index === cards.length - 1;
  const scope = useMemo(() => new CardScope(progress.cardDone(nav.lesson, index)), [progress, nav.lesson, index]);
  useStore(scope);
  const [dock, setDock] = useState<HTMLDivElement | null>(null);
  const [missing, setMissing] = useState<L[]>([]);
  const scroll = useRef<HTMLDivElement>(null);
  useEffect(() => progress.markSeen(nav.lesson, index), [progress, nav.lesson, index]);
  useEffect(() => {
    if (scope.solvedHere) progress.markCardDone(nav.lesson, index);
  }, [scope, scope.solvedHere, progress, nav.lesson, index]);
  useEffect(() => {
    setMissing([]);
    scroll.current?.scrollTo({ top: 0 });
  }, [index]);
  const course: CourseMeta = courseOf(nav.lesson);
  const Card = cards[index];
  // po poslední kartě lekce rovnou nabídnout procvičení (série k téhle lekci)
  const practiceSet = last && scope.done ? practiceAfterLesson(nav.lesson) : undefined;
  const courseDone = last && scope.done && course.lessons[course.lessons.length - 1] === nav.lesson;
  const finish = () => {
    progress.markDone(nav.lesson);
    onNav(nav.returnTo ?? (nav.fromTrainer ? null : { kind: 'home' }));
  };
  const now = scope.pending.length ? scope.pending : missing;
  const here = useMemo(() => ({ open: learn.open, here: { ...nav, card: index } as LearnNav }), [learn.open, nav, index]);
  const label = last ? p(L('Hotovo ✓', 'Готово ✓')) : p(L('Dál', 'Дальше')) + ' →';
  return (
    <div className="lesson">
      <Header title={`${nav.lesson % 100} · ${p(meta.title)}`} sub={`${index + 1} / ${cards.length}`} onClose={back} />
      <div className="lesson-dots" aria-hidden>
        {cards.map((_, i) => (
          <span key={i} className={i <= index ? 'on' : ''} />
        ))}
      </div>
      <div className="lesson-scroll" ref={scroll}>
        <div className="lesson-card">
          <LearnContext.Provider value={here}>
            <DockContext.Provider value={dock}>
              <CardContext.Provider value={scope}>{Card ? <Card key={nav.lesson + ':' + index} /> : <p className="muted">…</p>}</CardContext.Provider>
            </DockContext.Provider>
          </LearnContext.Provider>
          {courseDone && <DonatePrompt text={p(L('Kurz je za tebou! Výklad je zdarma a bez reklamy – jestli ti pomohl, můžeš podpořit autora.', 'Курс пройден! Уроки бесплатны и без рекламы — если они помогли, можно поддержать автора.'))} />}
        </div>
      </div>
      <div className="dock-wrap">
        <div ref={setDock} className="dock" />
      </div>
      {missing.length > 0 && !scope.done && (
        <div className="lesson-missing">
          <Rich text={now.length === 1 ? p(L('Ještě zbývá: ', 'Ещё осталось: ')) + p(now[0]) : p(L(`Nejdřív splň všechny úkoly (zbývá ${now.length})`, `Сначала выполни все задания (осталось ${now.length})`))} />
        </div>
      )}
      {practiceSet && (
        <div className="lesson-practice">
          <button
            className="link-btn"
            onClick={() => {
              progress.markDone(nav.lesson);
              onNav({ kind: 'practice', set: practiceSet.id, returnTo: nav.returnTo });
            }}
          >
            {p(L(`Procvičit: ${practiceSet.title.cs} →`, `Закрепить: ${practiceSet.title.ru} →`))}
          </button>
        </div>
      )}
      <div className="lesson-nav">
        {index > 0 ? (
          <button className="btn" onClick={() => setIndex(index - 1)}>
            ← {p(L('Zpět', 'Назад'))}
          </button>
        ) : (
          <span />
        )}
        {scope.done ? (
          <button className="btn btn-primary" onClick={() => (last ? finish() : setIndex(index + 1))}>
            {label}
          </button>
        ) : (
          // zamčené: vypadá neaktivně, ale klepnutí řekne, co ještě chybí, a ukáže to na kartě
          <button
            className="btn btn-locked"
            onClick={() => {
              setMissing(scope.pending.length ? scope.pending : [L('Vyzkoušej obrázek nahoře', 'Попробуй рисунок выше')]);
              scope.doNag();
            }}
          >
            🔒 {label}
          </button>
        )}
      </div>
    </div>
  );
}

/** Slovníček pojmů (česky – rusky). */
function Glossary({ progress, back }: { progress: LearnProgress; back: () => void }) {
  const lang = useLang();
  const p = (l: L) => pick(lang, l);
  // nejdřív oddíl kurzu, ze kterého žák přišel
  const sections = progress.course === 'tg' ? (['tg', 'st'] as const) : (['st', 'tg'] as const);
  return (
    <div className="learn-page">
      <Header title={p(L('Slovníček', 'Словарик'))} onClose={back} />
      <div className="learn-body glossary">
        {sections.map((id) => (
          <section key={id}>
            <h2>{p(id === 'st' ? L('Statika', 'Статика') : L('Trigonometrie', 'Тригонометрия'))}</h2>
            <dl>
              {(id === 'st' ? GLOSSARY : TRIG_GLOSSARY).map(([term, def], i) => (
                <div key={i}>
                  <dt>
                    <Rich text={p(term)} />
                  </dt>
                  <dd>
                    <Rich text={p(def)} />
                  </dd>
                </div>
              ))}
            </dl>
          </section>
        ))}
      </div>
    </div>
  );
}
