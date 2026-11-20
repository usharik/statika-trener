import { L } from '../i18n';

/**
 * Názvy a jednovětá shrnutí lekcí obou kurzů (shodné s aplikací). Id jsou jednoznačná přes kurzy:
 * statika 1–9, trigonometrie 101–108 – podle nich míří odkazy `learn(lesson, card)` a ukládá se postup.
 * Samostatně, aby odkazy z trenažéru nemusely načítat obsah lekcí.
 */
export const LESSON_META: Record<number, { title: L; summary: L }> = {
  1: { title: L("Síla je šipka", "Сила — это стрелка"), summary: L("Síla jako vektor: velikost, směr, smysl a působiště. Proč stůl tlačí a jak sílu změřit.", "Сила как вектор: величина, наклон, направление и точка приложения. Почему стол давит и как измерить силу.") },
  2: { title: L("Rovnováha sil", "Равновесие сил"), summary: L("Nevyrovnaná síla rozjíždí, vyrovnané síly nemění pohyb. Když se nic nehýbe, síly se vyruší.", "Неуравновешенная сила разгоняет, уравновешенные — не меняют движения. Если ничего не движется, силы уравновешены.") },
  3: { title: L("Jedna šipka = dvě", "Одна стрелка = две"), summary: L("Síly se sčítají jako šipky. Šikmou sílu nahradíš vodorovnou a svislou složkou.", "Силы складываются как стрелки. Наклонную силу заменишь горизонтальной и вертикальной составляющими.") },
  4: { title: L("Moment síly", "Момент силы"), summary: L("Jak moc síla otáčí: síla × kolmé rameno. Kdy moment povolí matici a kdy je nulový.", "Насколько сила поворачивает: сила × плечо по перпендикуляру. Когда момент открутит гайку и когда он равен нулю.") },
  5: { title: L("Rovnováha otáčení", "Равновесие моментов"), summary: L("ΣM = 0, Archimédova páka, proč ΣF = 0 nestačí a jak vybrat výhodný bod.", "ΣM = 0, рычаг Архимеда, почему ΣF = 0 недостаточно и как выбрать удобную точку.") },
  6: { title: L("Vazby brání pohybu", "Связи ограничивают движение"), summary: L("Pevná a posuvná kloubová podpora, vetknutí – co dovolí a co ne.", "Шарнирно-неподвижная и шарнирно-подвижная опоры, жёсткая заделка — что допускают и что нет.") },
  7: { title: L("Uvolnění", "Освобождение от связей"), summary: L("Bez podpory nosník spadne. Podporu nahradíš silou – reakcí; najdeš ji rukou i na vahách.", "Без опоры балка падает. Опору заменяешь силой — реакцией; найдёшь её рукой и на весах.") },
  8: { title: L("Spojité zatížení", "Распределённая нагрузка"), summary: L("Q = q · ℓ uprostřed zatížené délky – a celý nosník od uvolnění po kontrolu.", "Q = q · ℓ посередине нагруженного участка — и вся балка от освобождения до проверки.") },
  9: { title: L("Kloub a statická určitost", "Шарнир и статическая определимость"), summary: L("[[M_K]] = 0 z jedné části; n = r – a podpory musí bránit každému pohybu.", "[[M_K]] = 0 по одной части; n = r — и опоры должны мешать любому движению.") },
  101: { title: L("Odkud se vzal sinus", "Откуда взялся синус"), summary: L("V pravoúhlých trojúhelnících se stejným úhlem je podíl stran vždy stejný.", "В прямоугольных треугольниках с одинаковым углом отношение сторон всегда одно и то же.") },
  102: { title: L("Sinus, kosinus, tangens", "Синус, косинус, тангенс"), summary: L("Tři podíly stran – a jak z přepony spočítat odvěsnu.", "Три отношения сторон — и как по гипотенузе найти катет.") },
  103: { title: L("Stupně a kalkulačka", "Градусы и калькулятор"), summary: L("Jak se měří úhel a proč musí na kalkulačce svítit DEG.", "Как измеряют угол и почему на калькуляторе должно гореть DEG.") },
  104: { title: L("Úhly, které se vyplatí umět zpaměti", "Углы, которые стоит помнить"), summary: L("30°, 45°, 60° bez kalkulačky – ze dvou trojúhelníků.", "30°, 45°, 60° без калькулятора — из двух треугольников.") },
  105: { title: L("Vztahy mezi funkcemi", "Связи между функциями"), summary: L("Pythagorova věta, sin² + cos² = 1, tg = sin / cos a druhý úhel.", "Теорема Пифагора, sin² + cos² = 1, tg = sin / cos и второй угол.") },
  106: { title: L("Od stran zpátky k úhlu", "От сторон обратно к углу"), summary: L("arcsin, arccos, arctg a sklon zadaný poměrem nebo v procentech.", "arcsin, arccos, arctg и уклон, заданный отношением или в процентах.") },
  107: { title: L("Jednotková kružnice a znaménka", "Единичная окружность и знаки"), summary: L("Kdy je složka kladná a kdy záporná – podle směru šipky.", "Когда составляющая положительна, а когда отрицательна — по направлению стрелки.") },
  108: { title: L("Trigonometrie ve statice", "Тригонометрия в статике"), summary: L("Složky, výslednice, její úhel a rameno momentu – všechno z jednoho trojúhelníku.", "Составляющие, равнодействующая, её угол и плечо момента — всё из одного треугольника.") },
};

export const lessonTitle = (id: number): L => LESSON_META[id]?.title ?? L('', '');

export interface CourseMeta {
  id: 'st' | 'tg';
  title: L;
  /** podtitulek úvodní obrazovky */
  sub: L;
  lessons: number[];
}

export const COURSES: CourseMeta[] = [
  { id: 'st', title: L('Základy statiky', 'Основы статики'), sub: L('9 krátkých lekcí · asi 3–5 minut každá', '9 коротких уроков · примерно по 3–5 минут'), lessons: [1, 2, 3, 4, 5, 6, 7, 8, 9] },
  { id: 'tg', title: L('Základy trigonometrie', 'Основы тригонометрии'), sub: L('8 krátkých lekcí · asi 4–6 minut každá', '8 коротких уроков · примерно по 4–6 минут'), lessons: [101, 102, 103, 104, 105, 106, 107, 108] },
];

export const courseOf = (lesson: number): CourseMeta => COURSES.find((c) => c.lessons.includes(lesson)) ?? COURSES[0];
