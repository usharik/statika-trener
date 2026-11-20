import { L } from '../i18n';

/**
 * Soubor úloh (série). [types] = id typu → váha při losování. [afterLesson] = doporučená lekce před sérií,
 * [lessons] = lekce, ke kterým série patří. Shodné s aplikací (practice/Exercises.kt PracticeSets).
 */
export interface PracticeSet {
  id: string;
  title: L;
  types: [string, number][];
  size: number;
  afterLesson: number;
  lessons: [number, number];
}

const set = (id: string, title: L, types: [string, number][], size: number, afterLesson: number, lessons: [number, number] = [afterLesson, afterLesson]): PracticeSet => ({
  id,
  title,
  types,
  size,
  afterLesson,
  lessons,
});

/** „K lekcím 1–2“ / „K lekci 5“ (čísla lekcí v kurzu) */
export function lessonsLabel(s: PracticeSet): L {
  const a = s.lessons[0] % 100;
  const b = s.lessons[1] % 100;
  return a === b ? L(`K lekci ${a}`, `К уроку ${a}`) : L(`K lekcím ${a}–${b}`, `К урокам ${a}–${b}`);
}

export const PRACTICE_SETS: PracticeSet[] = [
  set('p1', L('Strany a podíly', 'Стороны и отношения'), [['E01', 1], ['E02', 1], ['E03', 1], ['E04', 1], ['E05', 1]], 8, 102, [101, 102]),
  set('p2', L('Kalkulačka a známé úhly', 'Калькулятор и известные углы'), [['E06', 1], ['E07', 1], ['E08', 1]], 8, 104, [103, 104]),
  set('p3', L('Pythagoras a vztahy', 'Пифагор и связи'), [['E09', 2], ['E10', 2], ['E11', 1]], 8, 105),
  set('p4', L('Úhel a sklon', 'Угол и уклон'), [['E12', 1], ['E13', 1], ['E14', 1], ['E15', 1]], 8, 106),
  // po lekci o znaménkách: složky vždy se znaménkem (E17S), i na lehké úrovni
  set('p5', L('Znaménka složek', 'Знаки составляющих'), [['E16', 1], ['E17S', 1], ['E18', 1]], 8, 107),
  set('p6', L('Trigonometrie ve statice', 'Тригонометрия в статике'), [['E17', 1], ['E19', 1], ['E20', 1], ['E21', 1], ['E22', 1], ['E23', 1]], 8, 108),
  set(
    'mix',
    L('Závěrečný mix', 'Итоговый микс'),
    [
      ['E01', 1], ['E02', 1], ['E03', 1], ['E04', 1], ['E05', 1], ['E06', 1], ['E07', 1], ['E08', 1], ['E09', 1], ['E10', 1],
      ['E11', 1], ['E12', 2], ['E13', 1], ['E14', 1], ['E15', 1], ['E16', 1], ['E17', 3], ['E18', 1], ['E19', 2], ['E20', 1],
      ['E21', 2], ['E22', 1], ['E23', 1],
    ],
    12,
    108,
    [101, 108],
  ),
];

export const practiceSetById = (id: string) => PRACTICE_SETS.find((s) => s.id === id);

/** série k procvičení hned po lekci [lesson] (bez závěrečného mixu) */
export const practiceAfterLesson = (lesson: number) => PRACTICE_SETS.find((s) => s.id !== 'mix' && s.afterLesson === lesson);
