/*
 * Kurz trigonometrie – lekce 7 (id 107): jednotková kružnice, kvadranty a znaménka složek podle směru šipky.
 * (Přepis z aplikace: learn/trig/TrigLesson7.kt.)
 */
import { ActionButton, CardText, Chips, L, Note, Readout, SmallText, Stage, Steps, ValueSlider, clamp, dist, num, opt, step, useKit, useLaunched, useQuizStep, useTr, type Colors, type Opt, type PrimBuilder, type Step } from '../../kit';
import { Trig } from '../../../practice/trig';
import { useGuessStep } from './shared';

const CX = 200;
const CY = 135;
const R = 100;
const CIRCLE_H = 270;

/** Osy s popisky kladných směrů +x (doprava) a +y (nahoru). */
function signedAxes(b: PrimBuilder, c: Colors, cx: number, cy: number, len: number) {
  b.axes(cx, cy, len, false);
  b.text(cx + len - 4, cy + 18, '+x', c.inkSoft, 13, 'end');
  b.text(cx + 10, cy - len * 0.8 + 10, '+y', c.inkSoft, 13, 'start');
}

/** Jednotková kružnice s bodem pod úhlem phi (stupně, proti směru hodin od +x). */
function unitCircle(b: PrimBuilder, c: Colors, phi: number, o: { numbers?: boolean; dragging?: boolean; showHandle?: boolean; phiLabel?: string; ring?: boolean } = {}) {
  const { numbers = true, dragging = false, showHandle = true, phiLabel = 'φ', ring = true } = o;
  signedAxes(b, c, CX, CY, R + 40);
  if (ring) {
    b.arc(CX, CY, R, 0, 360, c.inkSoft, 1.2);
    b.text(CX + R + 6, CY + 16, '1', c.inkSoft, 12, 'start');
    b.text(CX - R - 6, CY + 16, '−1', c.inkSoft, 12, 'end');
    b.text(CX - 6, CY - R - 4, '1', c.inkSoft, 12, 'end');
    b.text(CX - 6, CY + R + 14, '−1', c.inkSoft, 12, 'end');
  }
  const co = Trig.cosD(phi);
  const s = Trig.sinD(phi);
  const px = CX + R * co;
  const py = CY - R * s;
  b.line(px, py, px, CY, c.derived, 1.0, true);
  b.line(px, py, CX, py, c.derived, 1.0, true);
  if (Math.abs(co) > 0.01) b.line(CX, CY, px, CY, c.derived, 4.0);
  if (Math.abs(s) > 0.01) b.line(px, CY, px, py, c.derived, 4.0);
  b.line(CX, CY, px, py, c.load, 2.4);
  if (phi >= 1 && phi <= 359) {
    b.arc(CX, CY, 24, 0, -phi, c.inkSoft, 1.4);
    const mid = ((-phi / 2) * Math.PI) / 180;
    b.text(CX + 42 * Math.cos(mid), CY + 42 * Math.sin(mid) + 5, phiLabel, c.inkSoft, 13);
  }
  if (numbers) {
    b.text((CX + px) / 2 + (Math.abs(co) < 0.01 ? 22 : 0), CY + (s >= 0 ? 18 : -8), `x = ${num(co, 2)}`, c.derived, 12);
    b.text(px + (co >= 0 ? 8 : -8), (CY + py) / 2 + 5 + (Math.abs(s) < 0.01 ? -14 : 0), `y = ${num(s, 2)}`, c.derived, 12, co >= 0 ? 'start' : 'end');
  }
  if (showHandle) b.handle(px, py, dragging);
  else b.circle(px, py, 5, c.load);
}

/** úhel bodu pod prstem (stupně 0–359, po 5°) */
function phiAt(x: number, y: number) {
  let d = (Math.atan2(-(y - CY), x - CX) * 180) / Math.PI;
  if (d < 0) d += 360;
  return (Math.round(d / 5) * 5) % 360;
}

/** Karta 0: jednotková kružnice – bod (cos φ, sin φ). Od délek stran k souřadnicím. */
function UnitCircle() {
  const t = useTr();
  const kit = useKit();
  const phase = kit.mut(0);
  const demoDone = kit.mut(false);
  const guess = kit.mut<number | null>(null);
  const phi = kit.mut(30);
  const placed = kit.mut(false);
  const miss = kit.mut(false);
  const dragging = kit.mut(false);
  useLaunched([], async (job) => {
    for (let p = 1; p <= 3; p++) {
      await job.delay(800);
      phase.v = p;
    }
    await job.delay(900);
    demoDone.v = true;
  });
  const s1 = step(
    L('Sleduj přechod od trojúhelníku ke kružnici', 'Проследи переход от треугольника к окружности'),
    demoDone.v,
    () => <SmallText text={L('Sleduj obrázek…', 'Смотри на рисунок…')} />,
    () => (
      <Note
        tone="info"
        text={L(
          'Trojúhelník 30° s přeponou 1 má odvěsny cos 30° ≈ 0,87 a sin 30° = 0,5 – to jsou přesně souřadnice x a y jeho vrcholu na kružnici.',
          'У треугольника 30° с гипотенузой 1 катеты cos 30° ≈ 0,87 и sin 30° = 0,5 — это и есть координаты x и y его вершины на окружности.',
        )}
      />
    ),
  );
  const s2 = useGuessStep(L('Odhadni znaménko cos φ, když bod leží vlevo od osy y', 'Предскажи знак cos φ, когда точка слева от оси y'), [L('+', '+'), L('0', '0'), L('−', '−')], guess.v, (i) => (guess.v = i));
  const s3 = step(
    L('Přesuň bod vlevo nahoru a ověř souřadnice', 'Перемести точку влево вверх и проверь координаты'),
    placed.v,
    () => (
      <>
        <ValueSlider
          label={t.pick(L(`Úhel φ = ${phi.v}°`, `Угол φ = ${phi.v}°`))}
          value={phi.v}
          min={0}
          max={355}
          onChange={(v) => {
            phi.v = clamp(Math.round(v / 5) * 5, 0, 355);
            miss.v = false;
          }}
        />
        <Readout text={`cos ${phi.v}° = ${num(Trig.cosD(phi.v), 2)} · sin ${phi.v}° = ${num(Trig.sinD(phi.v), 2)}`} />
        <ActionButton text={L('Ověřit', 'Проверить')} onClick={() => (phi.v > 90 && phi.v < 180 ? (placed.v = true) : (miss.v = true))} />
        {miss.v && <Note tone="nudge" text={L('Vlevo nahoru je mezi 90° a 180° – nad osou x a vlevo od osy y.', 'Влево вверх — это между 90° и 180°: над осью x и слева от оси y.')} />}
      </>
    ),
    () => (
      <Note
        tone={guess.v === 2 ? 'ok' : 'info'}
        text={L(
          (guess.v === 2 ? 'Tip sedí: ' : '') + `při φ = ${phi.v}° je x = cos φ ≈ ${num(Trig.cosD(phi.v), 2)} – vlevo od osy y je x záporné.`,
          (guess.v === 2 ? 'Прогноз верен: ' : '') + `при φ = ${phi.v}° x = cos φ ≈ ${num(Trig.cosD(phi.v), 2)} — слева от оси y x отрицателен.`,
        )}
      />
    ),
  );
  const s4 = useQuizStep(
    L('Bod leží vlevo nahoře. Která jeho souřadnice je záporná?', 'Точка слева вверху. Какая её координата отрицательна?'),
    [
      opt(L('x (cos φ)', 'x (cos φ)'), true),
      opt(L('y (sin φ)', 'y (sin φ)'), false, L('Bod leží nad osou x – y je tedy kladné.', 'Точка лежит над осью x — значит, y положителен.')),
      opt(L('obě', 'обе'), false, L('Nad osou x je y kladné. Záporná je jen jedna.', 'Над осью x y положителен. Отрицательна только одна.')),
      opt(L('žádná', 'ни одна'), false, L('Vlevo od osy y je x záporné.', 'Слева от оси y x отрицателен.')),
    ],
    L(
      'Vlevo je x záporné, proto cos φ < 0. Nahoře je y kladné, proto sin φ > 0. Už nejde o délky stran, ale o **souřadnice** – ty záporné být mohou.',
      'Слева x отрицателен, поэтому cos φ < 0. Сверху y положителен, поэтому sin φ > 0. Это уже не длины сторон, а **координаты** — они бывают отрицательными.',
    ),
    null,
    2,
    L('Vyber souřadnici, která je zde záporná', 'Выбери координату, которая здесь отрицательна'),
  );
  const steps = [s1, s2, s3, s4];
  const cur = steps.findIndex((s) => !s.done);
  const free = cur === 2;
  return (
    <>
      <Stage
        h={CIRCLE_H}
        description={t.pick(L('Jednotková kružnice s pohyblivým bodem', 'Единичная окружность с подвижной точкой'))}
        onDrag={(p, start) => {
          if (start) dragging.v = free && dist(p, CX, CY) > R * 0.5;
          if (dragging.v) {
            phi.v = phiAt(p.x, p.y);
            miss.v = false;
          }
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b, c) => {
          if (cur === 0) {
            if (phase.v === 0) return;
            unitCircle(b, c, 30, { numbers: phase.v >= 3, showHandle: false, phiLabel: '30°', ring: phase.v >= 2 });
            return;
          }
          unitCircle(b, c, cur === 1 ? 30 : phi.v, { numbers: cur !== 1, dragging: dragging.v, showHandle: free });
        }}
      />
      <CardText
        text={L(
          'Na **jednotkové kružnici** (poloměr 1) má bod souřadnice **x = cos φ** a **y = sin φ**. Úhel φ měříme od +x proti směru hodinových ručiček.',
          'На **единичной окружности** (радиус 1) координаты точки: **x = cos φ**, **y = sin φ**. Угол φ отсчитываем от +x против часовой стрелки.',
        )}
      />
      <Steps steps={steps} />
    </>
  );
}

/** Volby znamének (cos, sin) pro kvíz kvadrantů; want = index správné dvojice. */
function signOpts(want: number): Opt[] {
  const pairs: [number, number][] = [
    [1, 1],
    [-1, 1],
    [-1, -1],
    [1, -1],
  ];
  const [wc, ws] = pairs[want];
  const s = (v: number) => (v > 0 ? '+' : '−');
  return pairs.map(([pc, ps], i) => {
    const why =
      i === want
        ? null
        : pc !== wc
          ? wc > 0
            ? L('Bod leží vpravo od osy y – jaké znaménko má x = cos φ?', 'Точка справа от оси y — какой знак у x = cos φ?')
            : L('Bod leží vlevo od osy y – jaké znaménko má x = cos φ?', 'Точка слева от оси y — какой знак у x = cos φ?')
          : ws > 0
            ? L('Bod leží nad osou x – jaké znaménko má y = sin φ?', 'Точка над осью x — какой знак у y = sin φ?')
            : L('Bod leží pod osou x – jaké znaménko má y = sin φ?', 'Точка под осью x — какой знак у y = sin φ?');
    const label = `cos ${s(pc)}, sin ${s(ps)}`;
    return opt(L(label, label), i === want, why);
  });
}

const quadrantOf = (p: number): number | null => (p >= 1 && p <= 89 ? 0 : p >= 91 && p <= 179 ? 1 : p >= 181 && p <= 269 ? 2 : p >= 271 && p <= 359 ? 3 : null);

const ROMAN = ['I.', 'II.', 'III.', 'IV.'];
const AT = [30, 150, 210, 330];
const QUAD_EXPLAIN_CS = ['Vpravo nahoře: x > 0, y > 0 – cos i sin kladné.', 'Vlevo nahoře: x < 0, y > 0 – cos záporný, sin kladný.', 'Vlevo dole: x < 0, y < 0 – cos i sin záporné.', 'Vpravo dole: x > 0, y < 0 – cos kladný, sin záporný.'];
const QUAD_EXPLAIN_RU = ['Справа вверху: x > 0, y > 0 — cos и sin положительны.', 'Слева вверху: x < 0, y > 0 — cos отрицателен, sin положителен.', 'Слева внизу: x < 0, y < 0 — cos и sin отрицательны.', 'Справа внизу: x > 0, y < 0 — cos положителен, sin отрицателен.'];

function useQuadQuiz(q: number) {
  return useQuizStep(
    L(`Bod je na ${AT[q]}° (${ROMAN[q]} kvadrant). Jaká znaménka mají cos φ a sin φ?`, `Точка на ${AT[q]}° (${ROMAN[q]} квадрант). Какие знаки у cos φ и sin φ?`),
    signOpts(q),
    L(QUAD_EXPLAIN_CS[q], QUAD_EXPLAIN_RU[q]),
    null,
    2,
    L(`Urči znaménka cos φ a sin φ v ${ROMAN[q]} kvadrantu`, `Определи знаки cos φ и sin φ в ${ROMAN[q]} квадранте`),
  );
}

/** Karta 1: čtyři kvadranty a znaménka – odvodit z polohy bodu, ne obejít kružnici. */
function Quadrants() {
  const t = useTr();
  const kit = useKit();
  const phi = kit.mut(45);
  const dragging = kit.mut(false);
  const qs = [useQuadQuiz(0), useQuadQuiz(1), useQuadQuiz(2), useQuadQuiz(3)];
  const s5 = useQuizStep(
    L('Bod je na 90°, přímo nahoře na ose y. Kolik je cos 90°?', 'Точка на 90°, прямо вверху на оси y. Чему равен cos 90°?'),
    [
      opt(L('−1', '−1'), false, L('−1 by byl bod úplně vlevo (180°).', '−1 — это точка в крайнем левом положении (180°).')),
      opt(L('0', '0'), true),
      opt(L('1', '1'), false, L('1 je sin 90° – výška bodu. Jak daleko je bod vpravo od osy y?', '1 — это sin 90°, высота точки. Насколько точка правее оси y?')),
    ],
    L(
      'Na ose y je x = 0, proto cos 90° = 0. **cos φ má znaménko souřadnice x, sin φ souřadnice y.** Na osách je jedna souřadnice nulová – bod tehdy neleží v žádném kvadrantu.',
      'На оси y x = 0, поэтому cos 90° = 0. **Знак cos φ совпадает со знаком x, знак sin φ — со знаком y.** На осях одна координата равна нулю — такая точка не лежит ни в одном квадранте.',
    ),
    null,
    3,
    L('Urči cos 90°', 'Определи cos 90°'),
  );
  const steps = [...qs, s5];
  const cur = steps.findIndex((s) => !s.done);
  const shown = cur >= 0 && cur <= 3 ? AT[cur] : cur === 4 ? 90 : phi.v;
  const signs = [
    ['+', '+'],
    ['−', '+'],
    ['−', '−'],
    ['+', '−'],
  ];
  return (
    <>
      <Stage
        h={CIRCLE_H}
        description={t.pick(L('Jednotková kružnice se čtyřmi kvadranty', 'Единичная окружность с четырьмя квадрантами'))}
        onDrag={(p, start) => {
          if (start) dragging.v = cur < 0 && dist(p, CX, CY) > R * 0.5;
          if (dragging.v) phi.v = phiAt(p.x, p.y);
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b, c) => {
          unitCircle(b, c, shown, { numbers: cur < 0, dragging: dragging.v, showHandle: cur < 0 });
          const pos = [
            [330, 30],
            [70, 30],
            [70, 235],
            [330, 235],
          ];
          for (let q = 0; q < 4; q++) {
            const [x, y] = pos[q];
            const here = cur === q || (cur < 0 && quadrantOf(phi.v) === q);
            b.text(x, y, ROMAN[q], here ? c.ink : c.inkSoft, 15, 'middle', null, true);
            if (qs[q].done) b.text(x, y + 20, `cos ${signs[q][0]}, sin ${signs[q][1]}`, c.derived, 13);
          }
        }}
      />
      <CardText
        text={L(
          'Osy dělí rovinu na čtyři **kvadranty**. Znaménka cos φ a sin φ určuje poloha bodu – vlevo/vpravo a nahoře/dole.',
          'Оси делят плоскость на четыре **квадранта** (четверти). Знаки cos φ и sin φ определяются положением точки — слева/справа и сверху/снизу.',
        )}
      />
      <Steps
        steps={steps}
        after={() => <ValueSlider label={t.pick(L(`Úhel φ = ${phi.v}°`, `Угол φ = ${phi.v}°`))} value={phi.v} min={0} max={355} onChange={(v) => (phi.v = clamp(Math.round(v / 5) * 5, 0, 355))} />}
      />
    </>
  );
}

/** Karta 2: velikost složek z ostrého úhlu, znaménko podle šipky. Směr se po každém kole změní sám. */
function AcuteAndSign() {
  const t = useTr();
  const kit = useKit();
  const phis = [150, 210, 330];
  const names = [L('vlevo nahoru', 'влево вверх'), L('vlevo dolů', 'влево вниз'), L('vpravo dolů', 'вправо вниз')];
  const solved = kit.mut(0);
  const sx = kit.mut<number | null>(null);
  const sy = kit.mut<number | null>(null);
  const miss = kit.mut<L | null>(null);
  const fx = 10 * Trig.cosD(30);
  const fy = 10 * Trig.sinD(30);
  const sgn = (s: number | null) => (s === 0 ? '+' : s === 1 ? '−' : '?');
  const want = (k: number): [number, number] => [Trig.cosD(phis[k]) > 0 ? 0 : 1, Trig.sinD(phis[k]) > 0 ? 0 : 1];
  const steps: Step[] = phis.map((_, k) => {
    const [wx, wy] = want(k);
    const sX = wx === 0 ? '+' : '−';
    const sY = wy === 0 ? '+' : '−';
    return step(
      L(`Doplň znaménka složek síly mířící ${names[k].cs}`, `Укажи знаки составляющих силы, направленной ${names[k].ru}`),
      solved.v > k,
      () => (
        <>
          <Chips
            options={[L('[[F_x]]: +', '[[F_x]]: +'), L('[[F_x]]: −', '[[F_x]]: −')]}
            selected={sx.v}
            onPick={(i) => {
              sx.v = i;
              miss.v = null;
            }}
          />
          <Chips
            options={[L('[[F_y]]: +', '[[F_y]]: +'), L('[[F_y]]: −', '[[F_y]]: −')]}
            selected={sy.v}
            onPick={(i) => {
              sy.v = i;
              miss.v = null;
            }}
          />
          <Readout text={t.pick(L(`[[F_x]] = ${sgn(sx.v)}${num(fx, 2)} kN · [[F_y]] = ${sgn(sy.v)}${num(fy, 2)} kN`, `[[F_x]] = ${sgn(sx.v)}${num(fx, 2)} кН · [[F_y]] = ${sgn(sy.v)}${num(fy, 2)} кН`))} />
          <ActionButton
            text={L('Zkontrolovat', 'Проверить')}
            onClick={() => {
              const m =
                sx.v === null || sy.v === null
                  ? L('Vyber znaménko u obou složek.', 'Выбери знак у обеих составляющих.')
                  : sx.v !== wx
                    ? L('Kam míří hrot šipky vodorovně – doprava (+), nebo doleva (−)?', 'Куда смотрит остриё по горизонтали — вправо (+) или влево (−)?')
                    : sy.v !== wy
                      ? L('Míří šipka nahoru (+), nebo dolů (−)?', 'Стрелка направлена вверх (+) или вниз (−)?')
                      : null;
              miss.v = m;
              if (m === null) {
                solved.v = k + 1;
                sx.v = null;
                sy.v = null;
              }
            }}
          />
          {miss.v && <Note tone="nudge" text={miss.v} />}
        </>
      ),
      () => (
        <SmallText
          text={L(
            `${names[k].cs}: [[F_x]] = ${sX}${num(fx, 2)} kN, [[F_y]] = ${sY}${num(fy, 2)} kN`,
            `${names[k].ru}: [[F_x]] = ${sX}${num(fx, 2)} кН, [[F_y]] = ${sY}${num(fy, 2)} кН`,
          )}
        />
      ),
    );
  });
  const cur = steps.findIndex((s) => !s.done);
  const k = cur < 0 ? phis.length - 1 : cur;
  const phi = phis[k];
  return (
    <>
      <Stage
        h={250}
        description={t.pick(L(`Síla 10 kN pod úhlem 30° k vodorovné ve směru ${names[k].cs}`, `Сила 10 кН под углом 30° к горизонтали, направленная ${names[k].ru}`))}
        draw={(b, c) => {
          const cx = 200;
          const cy = 130;
          signedAxes(b, c, cx, cy, 150);
          const len = 110;
          const co = Trig.cosD(phi);
          const s = Trig.sinD(phi);
          const tx = cx + len * co;
          const ty = cy - len * s;
          if (cur < 0) {
            b.line(tx, cy, tx, ty, c.derived, 1.0, true);
            b.line(cx, ty, tx, ty, c.derived, 1.0, true);
            b.force(cx, cy, tx, cy, c.derived, 2.4);
            b.force(cx, cy, cx, ty, c.derived, 2.4);
            b.text((cx + tx) / 2, cy + (s > 0 ? 18 : -8), ` = +${num(fx, 2)}`, c.derived, 13, 'middle', 'F_x');
            b.text(cx + (co > 0 ? -10 : 10), (cy + ty) / 2 + 5, ` = −${num(fy, 2)}`, c.derived, 13, co > 0 ? 'end' : 'start', 'F_y');
          }
          b.force(cx, cy, tx, ty, c.load, 3.0);
          b.text(tx + (co > 0 ? 8 : -8), ty + (s > 0 ? -6 : 16), t.pick(L(' = 10 kN', ' = 10 кН')), c.load, 14, co > 0 ? 'start' : 'end', 'F');
          const base = co > 0 ? 0 : 180;
          b.angleArc(cx, cy, 40, base, -phi, '30°');
        }}
      />
      <CardText
        text={L(
          'Pro sílu 10 kN pod úhlem 30° k vodorovné jsou **velikosti** složek 8,66 kN a 5 kN. **Znaménka** závisejí na směru šipky.',
          'Для силы 10 кН под углом 30° к горизонтали **модули** составляющих равны 8,66 кН и 5 кН. **Знаки** зависят от направления стрелки.',
        )}
      />
      <Steps
        steps={steps}
        after={() => (
          <Note
            tone="ok"
            text={L(
              'Doprava znamená [[F_x]] > 0, doleva [[F_x]] < 0; nahoru [[F_y]] > 0, dolů [[F_y]] < 0. Velikosti zůstaly stejné, změnily se jen směry.',
              'Вправо — [[F_x]] > 0, влево — [[F_x]] < 0; вверх — [[F_y]] > 0, вниз — [[F_y]] < 0. Модули не изменились, изменились только направления.',
            )}
          />
        )}
      />
    </>
  );
}

/** Karta 3: kvíz – síla dolů doprava pod 60°. */
function CheckDownRight() {
  const t = useTr();
  const q = useQuizStep(
    L('Síla 10 kN míří vpravo dolů, 60° od vodorovné. Která dvojice ([[F_x]]; [[F_y]]) v kN platí?', 'Сила 10 кН направлена вправо вниз, 60° от горизонтали. Какая пара ([[F_x]]; [[F_y]]) в кН верна?'),
    [
      opt(L('(+5; −8,66)', '(+5; −8,66)'), true),
      opt(L('(+8,66; −5)', '(+8,66; −5)'), false, L('Znaménka sedí, ale velikosti jsou prohozené. Která složka leží u úhlu 60°?', 'Знаки верны, но модули перепутаны. Какая составляющая лежит у угла 60°?')),
      opt(L('(+5; +8,66)', '(+5; +8,66)'), false, L('Velikosti sedí. Míří šipka nahoru, nebo dolů?', 'Модули верны. Стрелка направлена вверх или вниз?')),
      opt(L('(−5; −8,66)', '(−5; −8,66)'), false, L('Svisle je to dobře. Kam míří hrot vodorovně?', 'По вертикали верно. Куда смотрит остриё по горизонтали?')),
    ],
    L('[[F_x]] = 10 · cos 60° = +5 kN (doprava), [[F_y]] = −10 · sin 60° ≈ −8,66 kN (dolů).', '[[F_x]] = 10 · cos 60° = +5 кН (вправо), [[F_y]] = −10 · sin 60° ≈ −8,66 кН (вниз).'),
    null,
    2,
    L('Vyber správnou dvojici složek [[F_x]] a [[F_y]]', 'Выбери правильную пару составляющих [[F_x]] и [[F_y]]'),
  );
  const shown = q.done;
  return (
    <>
      <Stage
        h={220}
        description={t.pick(L('Síla 10 kN šikmo dolů doprava, 60° k vodorovné', 'Сила 10 кН наклонно вниз вправо, 60° к горизонтали'))}
        draw={(b, c) => {
          const cx = 170;
          const cy = 60;
          const len = 140;
          const tx = cx + len * Trig.cosD(60);
          const ty = cy + len * Trig.sinD(60);
          b.line(40, cy, 360, cy, c.inkSoft, 1.0, true);
          b.force(330, 190, 370, 190, c.inkSoft, 1.4, 7);
          b.force(330, 190, 330, 150, c.inkSoft, 1.4, 7);
          b.text(374, 194, '+x', c.inkSoft, 12, 'start');
          b.text(330, 142, '+y', c.inkSoft, 12);
          if (shown) {
            b.line(tx, cy, tx, ty, c.derived, 1.0, true);
            b.line(cx, ty, tx, ty, c.derived, 1.0, true);
            b.force(cx, cy, tx, cy, c.derived, 2.4);
            b.force(cx, cy, cx, ty, c.derived, 2.4);
            b.text((cx + tx) / 2, cy - 8, t.pick(L(' = +5 kN', ' = +5 кН')), c.derived, 13, 'middle', 'F_x');
            b.text(cx - 10, (cy + ty) / 2 + 5, t.pick(L(' = −8,66 kN', ' = −8,66 кН')), c.derived, 13, 'end', 'F_y');
          }
          b.force(cx, cy, tx, ty, c.load, 3.0);
          b.text(tx + 8, ty, t.pick(L(' = 10 kN', ' = 10 кН')), c.load, 14, 'start', 'F');
          b.angleArc(cx, cy, 40, 0, 60, '60°');
        }}
      />
      <CardText text={L('Síla 10 kN míří vpravo dolů pod úhlem 60° k vodorovné. Kladné směry jsou doprava a nahoru.', 'Сила 10 кН направлена вправо вниз под углом 60° к горизонтали. Положительные направления — вправо и вверх.')} />
      <Steps steps={[q]} />
    </>
  );
}

/** Karta 4 (volitelná): kvíz – cos 150° přes vztažný úhel 30°. */
function CheckCos150() {
  const t = useTr();
  const q = useQuizStep(
    L('Kolik je cos 150°?', 'Чему равен cos 150°?'),
    [
      opt(L('−0,866', '−0,866'), true),
      opt(L('+0,866', '+0,866'), false, L('Velikost sedí – vztažný úhel je 30°. Na které straně od osy y bod leží?', 'Модуль верен — опорный угол 30°. С какой стороны от оси y лежит точка?')),
      opt(L('−0,5', '−0,5'), false, L('0,5 je výška bodu, tedy sin 150°. Která souřadnice je cos?', '0,5 — высота точки, то есть sin 150°. Какая координата — cos?')),
      opt(L('+0,5', '+0,5'), false, L('To je sin 150°. Kosinus je vodorovná souřadnice – a bod leží vlevo.', 'Это sin 150°. Косинус — горизонтальная координата, а точка лежит слева.')),
    ],
    L('Bod leží vlevo, takže kosinus je záporný. cos 150° = −cos 30° = −√3/2 ≈ −0,866.', 'Точка лежит слева, поэтому косинус отрицателен. cos 150° = −cos 30° = −√3/2 ≈ −0,866.'),
    null,
    2,
    L('Vyber hodnotu cos 150°', 'Выбери значение cos 150°'),
  );
  return (
    <>
      <Stage
        h={CIRCLE_H}
        description={t.pick(L('Jednotková kružnice s bodem na 150° a vztažným úhlem 30°', 'Единичная окружность с точкой на 150° и опорным углом 30°'))}
        draw={(b, c) => {
          unitCircle(b, c, 150, { numbers: false, showHandle: false, phiLabel: '150°' });
          b.arc(CX, CY, 52, -150, -30, c.accent, 2.0);
          const mid = (-165 * Math.PI) / 180;
          b.text(CX + 70 * Math.cos(mid), CY + 70 * Math.sin(mid) + 5, '30°', c.accent, 13, 'middle', null, true);
        }}
      />
      <CardText text={L('Úhel 150° leží ve II. kvadrantu. Mezi poloměrem a zápornou poloosou x zbývá **30°**.', 'Угол 150° находится во II квадранте. Между радиусом и отрицательной полуосью x — **30°**.')} />
      <Steps steps={[q]} />
    </>
  );
}

export const cards = [UnitCircle, Quadrants, AcuteAndSign, CheckDownRight, CheckCos150];
