/*
 * Kurz trigonometrie – lekce 3 (id 103): stupně, úhloměr, kalkulačka (DEG / RAD / GRAD), radiány.
 * (Přepis z aplikace: learn/trig/TrigLesson3.kt.)
 */
import type { ReactNode } from 'react';
import { ActionButton, CardText, FastOutSlowInEasing, L, Note, SmallText, Stage, Steps, ValueSlider, clamp, dist, num, opt, step, tween, useKit, useLaunched, useQuizStep, useTr, type Colors, type PrimBuilder } from '../../kit';
import { Trig } from '../../../practice/trig';
import { BTN_CHECK, BTN_TEST, PickCheck, useGuessStep } from './shared';

/** Displej kalkulačky: výraz, hodnota a (volitelně) indikátor režimu. */
export function calcDisplay(b: PrimBuilder, expr: string, value: string, mode: string | null, c: Colors, valueColor: string = c.ink) {
  b.rect(80, 20, 240, 120, c.subtle, c.ink, 2.0);
  b.rect(95, 35, 210, 90, c.paper, c.inkSoft, 1.0);
  if (mode !== null) b.text(110, 53, mode, c.inkSoft, 11, 'start', null, true);
  b.text(290, 73, expr, c.ink, 15, 'end');
  b.text(290, 111, value, valueColor, 24, 'end', null, true);
}

const MODES = ['DEG', 'RAD', 'GRAD'];

/** sin čísla [x] tak, jak ho chápe kalkulačka v režimu [mode] (0 DEG, 1 RAD, 2 GRAD) */
const calcSin = (x: number, mode: number) => (mode === 0 ? Trig.sinD(x) : mode === 1 ? Math.sin(x) : Trig.sinD(x * 0.9));

/** Karta 0: úhloměr – úhel jako otočení od výchozího ramene. */
function Protractor() {
  const t = useTr();
  const kit = useKit();
  const deg = kit.mut(30);
  const r90 = kit.mut(false);
  const r45 = kit.mut(false);
  const r180 = kit.mut(false);
  const msg = kit.mut<L | null>(null);
  const dragging = kit.mut(false);
  const cx = 200;
  const cy = 200;
  const rr = 160;
  const set = (v: number) => {
    deg.v = clamp(Math.round(v / 5) * 5, 0, 180);
    msg.v = null;
  };
  const a = (deg.v * Math.PI) / 180;
  const hx = cx + (rr - 55) * Math.cos(a);
  const hy = cy - (rr - 55) * Math.sin(a);
  const tools = (target: number, onOk: () => void, miss: L): ReactNode => (
    <>
      <ValueSlider label={t.pick(L(`Úhel = ${Math.round(deg.v)}°`, `Угол = ${Math.round(deg.v)}°`))} value={deg.v} min={0} max={180} steps={35} onChange={set} />
      <ActionButton text={BTN_CHECK} onClick={() => (deg.v === target ? onOk() : (msg.v = miss))} />
      {msg.v && <Note tone="nudge" text={msg.v} />}
    </>
  );
  return (
    <>
      <Stage
        h={225}
        description={t.pick(L('Úhloměr s ramenem, které se dá natočit', 'Транспортир с поворачивающимся лучом'))}
        onDrag={(p, start) => {
          if (!start || dist(p, hx, hy) < 50) {
            dragging.v = true;
            // pod základnou: vlevo = 180°, vpravo = 0° (žádný skok z 180° na 0°)
            const raw = (Math.atan2(cy - p.y, p.x - cx) * 180) / Math.PI;
            set(raw < -90 ? 180 : raw < 0 ? 0 : raw);
          }
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b, c) => {
          b.protractor(cx, cy, rr);
          b.line(cx, cy, cx + rr - 55, cy, c.ink, 3.0);
          b.text(cx + rr - 50, cy + 18, t.pick(L('výchozí rameno (0°)', 'начальный луч (0°)')), c.ink, 12);
          b.line(cx, cy, hx, hy, c.load, 2.8);
          if (deg.v > 0 && deg.v < 180) b.angleArc(cx, cy, 44, 0, -deg.v, `${Math.round(deg.v)}°`, c.load);
          if (deg.v === 180) {
            b.arc(cx, cy, 44, 0, -180, c.load, 1.4);
            b.text(cx, cy - 52, '180°', c.load, 13);
          }
          b.circle(cx, cy, 4, c.ink);
          b.handle(hx, hy, dragging.v);
        }}
      />
      <CardText text={L('Úhel měří, o kolik se rameno otočilo od výchozího ramene. Celá otáčka má 360°.', 'Угол показывает, насколько луч повернулся от начального луча. Полный оборот равен 360°.')} />
      <Steps
        steps={[
          step(
            L('Otoč rameno o čtvrt otáčky', 'Поверни луч на четверть оборота'),
            r90.v,
            () =>
              tools(
                90,
                () => {
                  r90.v = true;
                  msg.v = null;
                },
                L('Celá otáčka má 360°. Kolik je její čtvrtina? Rameno pak stojí svisle.', 'Полный оборот — 360°. Сколько его четверть? Луч тогда стоит вертикально.'),
              ),
            () => <Note tone="ok" text={L('Čtvrt otáčky = 360° : 4 = **90°**, pravý úhel. Rameno stojí kolmo k výchozímu.', 'Четверть оборота = 360° : 4 = **90°**, прямой угол. Луч перпендикулярен начальному.')} />,
          ),
          step(
            L('Najdi polovinu pravého úhlu', 'Найди половину прямого угла'),
            r45.v,
            () =>
              tools(
                45,
                () => {
                  r45.v = true;
                  msg.v = null;
                },
                L('Pravý úhel má 90°. Rameno má být přesně v půli mezi 0° a 90°.', 'Прямой угол — 90°. Луч должен быть ровно посередине между 0° и 90°.'),
              ),
            () => <Note tone="ok" text={L('**45°** – rameno je přesně uprostřed mezi 0° a 90°.', '**45°** — луч ровно посередине между 0° и 90°.')} />,
          ),
          step(
            L('Otoč rameno do opačného směru, než míří výchozí', 'Поверни луч в сторону, противоположную начальному'),
            r180.v,
            () =>
              tools(
                180,
                () => {
                  r180.v = true;
                  msg.v = null;
                },
                L('Opačný směr je půl otáčky – rameno míří přesně doleva.', 'Противоположное направление — это пол-оборота: луч смотрит ровно влево.'),
              ),
            () => <Note tone="ok" text={L('Půl otáčky = **180°**, přímý úhel. Obě ramena leží na jedné přímce.', 'Половина оборота = **180°**, развёрнутый угол. Оба луча лежат на одной прямой.')} />,
          ),
        ]}
      />
    </>
  );
}

/** Karta 1: past režimu kalkulačky – nejdřív tip, pak zkouška v DEG / RAD / GRAD. */
function CalcMode() {
  const t = useTr();
  const kit = useKit();
  const guess = kit.mut<number | null>(null);
  const mode = kit.mut(1); // kalkulačka začíná v RAD
  const degOk = kit.mut(false);
  const msg = kit.mut<L | null>(null);
  const guesses = [L('0,5', '0,5'), L('−0,988', '−0,988'), L('0,454', '0,454')];
  const cosQ = useQuizStep(
    L('Kalkulačka je v DEG. Co ukáže cos 60?', 'Калькулятор в DEG. Что он покажет для cos 60?'),
    [
      opt(L('0,5', '0,5'), true),
      opt(
        L('0,866', '0,866'),
        false,
        L('0,866 je sin 60° – výška strmé rampy z lekce 1. Kosinus 60° patří k vodorovné odvěsně, a ta je kratší.', '0,866 — это sin 60°, высота крутого пандуса из урока 1. Косинус 60° относится к горизонтальному катету, а он короче.'),
      ),
      opt(L('−0,952', '−0,952'), false, L('Tohle by ukázal režim RAD (60 radiánů). Kosinus ostrého úhlu je kladný.', 'Это показал бы режим RAD (60 радиан). Косинус острого угла положителен.')),
    ],
    L(
      'cos 60° = 0,5. Pro úhly zadané ve stupních používej DEG. RAD ani GRAD nejsou chybné režimy – jen používají jiné jednotky úhlu. Rychlá zkouška: sin 30 musí dát 0,5.',
      'cos 60° = 0,5. Для углов в градусах нужен DEG. RAD и GRAD — не «неправильные» режимы: они просто используют другие единицы угла. Быстрая проверка: sin 30 должен дать 0,5.',
    ),
    null,
    3,
  );
  const g = useGuessStep(L('Tipni si: kolik má vyjít sin 30°?', 'Предскажи: чему должен быть равен sin 30°?'), guesses, guess.v, (i) => (guess.v = i));
  const gi = guess.v ?? 1;
  return (
    <>
      <Stage
        h={160}
        description={t.pick(L('Displej kalkulačky', 'Дисплей калькулятора'))}
        draw={(b, c) => {
          if (guess.v === null) calcDisplay(b, 'sin(30)', '?', null, c, c.inkSoft);
          else if (!degOk.v) calcDisplay(b, 'sin(30)', num(calcSin(30, mode.v), 3), MODES[mode.v], c, mode.v === 0 ? c.accent : c.nudge);
          else calcDisplay(b, 'cos(60)', cosQ.done ? '0,5' : '?', 'DEG', c, cosQ.done ? c.accent : c.inkSoft);
        }}
      />
      <CardText
        text={L(
          'Číslo 30 samo neurčuje jednotku úhlu. Kalkulačka ji převezme z režimu: **DEG** = stupně, **RAD** = radiány, **GRAD** = grady (pravý úhel = 100 gradů).',
          'Само число 30 не задаёт единицу угла. Калькулятор берёт её из режима: **DEG** — градусы, **RAD** — радианы, **GRAD** — грады (прямой угол = 100 град).',
        )}
      />
      <Steps
        steps={[
          g,
          step(
            L('Přepínej režimy a vyber ten, který počítá ve stupních', 'Переключай режимы и выбери тот, что считает в градусах'),
            degOk.v,
            () => (
              <PickCheck
                options={MODES.map((m) => L(m, m))}
                selected={mode.v}
                onSelect={(i) => {
                  mode.v = i;
                  msg.v = null;
                }}
                button={BTN_TEST}
                msg={msg.v}
                onCheck={() => {
                  if (mode.v === 0) {
                    degOk.v = true;
                    msg.v = null;
                  } else
                    msg.v =
                      mode.v === 1
                        ? L('RAD bere 30 jako 30 radiánů – skoro pět otáček. Proto −0,988.', 'RAD понимает 30 как 30 радиан — почти пять оборотов. Отсюда −0,988.')
                        : L('GRAD: pravý úhel má 100 gradů, takže 30 gradů je jen 27°. Proto 0,454.', 'GRAD: прямой угол — 100 град, поэтому 30 град — это всего 27°. Отсюда 0,454.');
                }}
              />
            ),
            () => (
              <Note
                tone={guess.v === 0 ? 'ok' : 'info'}
                text={L(
                  'V DEG je sin 30° = 0,5. ' + (guess.v === 0 ? 'Tip sedí.' : `Tip ${guesses[gi].cs} ukazuje kalkulačka v jiném režimu: RAD bere 30 jako 30 radiánů, GRAD jako 30 gradů = 27°.`),
                  'В DEG sin 30° = 0,5. ' + (guess.v === 0 ? 'Прогноз верен.' : `Значение ${guesses[gi].ru} калькулятор показывает в другом режиме: RAD понимает 30 как 30 радиан, GRAD — как 30 град = 27°.`),
                )}
              />
            ),
          ),
          cosQ,
        ]}
      />
    </>
  );
}

/** Karta 2: záporný výsledek u ostrého úhlu – najdi příčinu a oprav ji. */
function CheckRad() {
  const t = useTr();
  const kit = useKit();
  const sel = kit.mut(1); // 0 DEG, 1 RAD
  const shown = kit.mut(1); // režim, ve kterém kalkulačka naposled počítala
  const fixed = kit.mut(false);
  const msg = kit.mut<L | null>(null);
  const why = useQuizStep(
    L('Počítáš 10 · sin 30° a kalkulačka ukáže −9,88. Co se stalo?', 'Ты считаешь 10 · sin 30°, а калькулятор показывает −9,88. Что случилось?'),
    [
      opt(L('Režim RAD', 'Режим RAD'), true),
      opt(L('Je to správně', 'Всё верно'), false, L('Pro úhly mezi 0° a 90° je sinus vždy mezi 0 a 1. Kolik má vyjít sin 30°?', 'Для углов от 0° до 90° синус всегда между 0 и 1. Сколько должно получиться для sin 30°?')),
      opt(
        L('Mělo být cos', 'Нужен был cos'),
        false,
        L('10 · cos 30° ≈ 8,66 – taky kladné číslo. Co na kalkulačce rozhoduje, jak rozumí číslu 30?', '10 · cos 30° ≈ 8,66 — тоже положительное число. Что на калькуляторе решает, как он понимает число 30?'),
      ),
      opt(L('Chyba v násobení', 'Ошибка в умножении'), false, L('Zkus jen sin 30: vyjde −0,988 místo 0,5. Chyba je už tam.', 'Попробуй просто sin 30: выйдет −0,988 вместо 0,5. Ошибка уже там.')),
    ],
    L('Kalkulačka četla 30 jako 30 radiánů: sin(30 rad) ≈ −0,988, krát 10 je −9,88.', 'Калькулятор понял 30 как 30 радиан: sin(30 рад) ≈ −0,988, умножить на 10 — −9,88.'),
    null,
    2,
  );
  return (
    <>
      <Stage
        h={160}
        description={t.pick(L('Kalkulačka ukazuje 10 · sin 30 = −9,88', 'Калькулятор показывает 10 · sin 30 = −9,88'))}
        draw={(b, c) => {
          const v = 10 * calcSin(30, shown.v);
          calcDisplay(b, '10 × sin(30)', num(v, 2), why.done ? MODES[shown.v] : null, c, shown.v === 0 ? c.accent : c.nudge);
        }}
      />
      <CardText text={L('Pro ostrý úhel je sinus kladný. Záporný výsledek je důvod zkontrolovat zadání i jednotku úhlu.', 'Синус острого угла положителен. Отрицательный результат — повод проверить ввод и единицу угла.')} />
      <Steps
        steps={[
          why,
          step(
            L('Oprav režim a přepočítej výraz', 'Исправь режим и пересчитай выражение'),
            fixed.v,
            () => (
              <PickCheck
                options={[L('DEG', 'DEG'), L('RAD', 'RAD')]}
                selected={sel.v}
                onSelect={(i) => {
                  sel.v = i;
                  msg.v = null;
                }}
                button={L('Přepočítat', 'Пересчитать')}
                msg={msg.v}
                onCheck={() => {
                  shown.v = sel.v;
                  if (sel.v === 0) {
                    fixed.v = true;
                    msg.v = null;
                  } else msg.v = L('Pořád RAD – výsledek je zase −9,88.', 'Всё ещё RAD — снова −9,88.');
                }}
              />
            ),
            () => (
              <Note
                tone="ok"
                text={L(
                  'Ve stupních je 10 · sin 30° = 10 · 0,5 = 5. Když výsledek nesedí s odhadem, kontroluj také znaménka, závorky a to, co jsi opravdu zadal(a).',
                  'В градусах 10 · sin 30° = 10 · 0,5 = 5. Если результат не сходится с ожиданием, проверяй также знаки, скобки и то, что ты на самом деле ввёл(а).',
                )}
              />
            ),
          ),
        ]}
      />
    </>
  );
}

/** Karta 3: radián – oblouk dlouhý jako poloměr; půl otáčky = π, celá = 2π. */
function Radians() {
  const t = useTr();
  const kit = useKit();
  const wrap = kit.anim(0);
  const demoDone = kit.mut(false);
  useLaunched([], async (job) => {
    await job.delay(700);
    await job.run(wrap.animateTo(1, tween(2200, FastOutSlowInEasing)));
    await job.delay(400);
    demoDone.v = true;
  });
  const halfQ = useQuizStep(
    L('Kolik radiánů má půl otáčky (180°)?', 'Сколько радиан в половине оборота (180°)?'),
    [
      opt(L('π', 'π'), true),
      opt(
        L('2π', '2π'),
        false,
        L('Na půlkružnici se vejde asi 3,14 poloměru – spočítej dílky na obrázku. 2π ≈ 6,28 by byla celá kružnice.', 'На полуокружность укладывается примерно 3,14 радиуса — посчитай деления на рисунке. 2π ≈ 6,28 — это целая окружность.'),
      ),
      opt(L('π/2', 'π/2'), false, L('π/2 ≈ 1,57 je jen čtvrt otáčky. Kolik poloměrů se vejde na půlkružnici?', 'π/2 ≈ 1,57 — это только четверть оборота. Сколько радиусов укладывается на полуокружность?')),
    ],
    L('Na půlkružnici se vejde π ≈ 3,14 poloměru, takže 180° = π rad.', 'На полуокружность укладывается π ≈ 3,14 радиуса, поэтому 180° = π рад.'),
    null,
    3,
  );
  const fullQ = useQuizStep(
    L('Kolik radiánů má celá otáčka (360°)?', 'Сколько радиан в полном обороте (360°)?'),
    [
      opt(L('π', 'π'), false, L('π je jen půl otáčky. Celá otáčka jsou dvě půlky.', 'π — это только половина оборота. Полный оборот — две половины.')),
      opt(L('2π', '2π'), true),
      opt(L('360', '360'), false, L('360 je počet stupňů. V radiánech počítáme poloměry na oblouku.', '360 — это число градусов. В радианах считаем радиусы на дуге.')),
    ],
    L('Celá otáčka je 2π rad ≈ 6,28 rad = 360°. Jednotka úhlu a režim kalkulačky musí souhlasit.', 'Полный оборот — 2π рад ≈ 6,28 рад = 360°. Единица угла должна соответствовать режиму калькулятора.'),
    null,
    3,
  );
  const cx = 200;
  const cy = 118;
  const rr = 80;
  // jak velký oblouk ukázat: ukázka 1 rad, pak půl otáčky, pak celá
  const sweepDeg = !demoDone.v ? (wrap.value * 180) / Math.PI : !halfQ.done ? 180 : 360;
  return (
    <>
      <Stage
        h={235}
        description={t.pick(L('Kružnice a oblouk měřený poloměry', 'Окружность и дуга, измеренная радиусами'))}
        draw={(b, c) => {
          b.circle(cx, cy, rr, null, c.line, 1.2);
          b.line(cx, cy, cx + rr, cy, c.ink, 2.0);
          b.text(cx + rr / 2, cy + 18, 'r', c.ink, 14, 'middle', null, true);
          if (sweepDeg > 0.5) b.arc(cx, cy, rr, 0, -sweepDeg, c.derived, 5.0);
          const endA = (sweepDeg * Math.PI) / 180;
          if (!demoDone.v) {
            // zbytek poloměru, který se teprve „namotá“: rovně po tečně z konce oblouku
            const ex = cx + rr * Math.cos(endA);
            const ey = cy - rr * Math.sin(endA);
            const rest = rr * (1 - wrap.value);
            b.line(ex, ey, ex - rest * Math.sin(endA), ey - rest * Math.cos(endA), c.derived, 5.0);
            if (wrap.value >= 0.999) {
              b.line(cx, cy, ex, ey, c.ink, 2.0);
              b.angleArc(cx, cy, 22, 0, -sweepDeg, null);
              b.text(cx + (rr + 22) * Math.cos(endA / 2), cy - (rr + 22) * Math.sin(endA / 2), t.pick(L('s = r', 's = r')), c.derived, 14, 'start', null, true);
              b.text(cx, cy + rr + 28, t.pick(L('1 rad ≈ 57,3°', '1 рад ≈ 57,3°')), c.derived, 15, 'middle', null, true);
            }
          } else {
            if (sweepDeg < 359) b.line(cx, cy, cx - rr, cy, c.ink, 2.0);
            // dílky po 1 rad – kolik poloměrů se vejde na oblouk
            for (let k = 1; (k * 180) / Math.PI <= sweepDeg; k++) {
              b.line(cx + (rr - 7) * Math.cos(k), cy - (rr - 7) * Math.sin(k), cx + (rr + 7) * Math.cos(k), cy - (rr + 7) * Math.sin(k), c.ink, 1.6);
              b.text(cx + (rr + 18) * Math.cos(k), cy - (rr + 18) * Math.sin(k) + 5, `${k}`, c.inkSoft, 12);
            }
            const half = !halfQ.done;
            const solved = half ? halfQ.done : fullQ.done;
            const label = half ? (solved ? '180° = π rad' : '180° = ? rad') : solved ? '360° = 2π rad' : '360° = ? rad';
            b.text(cx, cy + rr + 30, label, c.derived, 15, 'middle', null, true);
          }
          b.circle(cx, cy, 3.5, c.ink);
        }}
      />
      <CardText
        text={L(
          'V radiánech měříme úhel poměrem délky oblouku s k poloměru r: φ = s / r. Kolik poloměrů se vejde na oblouk, tolik je radiánů.',
          'В радианах угол измеряют отношением длины дуги s к радиусу r: φ = s / r. Сколько радиусов укладывается на дугу, столько и радиан.',
        )}
      />
      <Steps
        steps={[
          step(
            L('Sleduj oblouk dlouhý přesně jako poloměr', 'Проследи за дугой длиной ровно в один радиус'),
            demoDone.v,
            () => <SmallText text={L('Poloměr se pokládá na kružnici…', 'Радиус укладывается на окружность…')} />,
            () => <Note tone="info" text={L('Oblouk dlouhý jako poloměr odpovídá úhlu **1 rad ≈ 57,3°**.', 'Дуге длиной в радиус соответствует угол **1 рад ≈ 57,3°**.')} />,
          ),
          halfQ,
          fullQ,
        ]}
      />
    </>
  );
}

export const cards = [Protractor, CalcMode, CheckRad, Radians];
