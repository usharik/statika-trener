/*
 * Kurz trigonometrie – lekce 8 (id 108): trigonometrie ve statice – úhel od svislice, výslednice, součet sil,
 * rameno momentu, šikmý nosník a závěrečná úloha. (Přepis z aplikace: learn/trig/TrigLesson8.kt.)
 */
import type { ReactNode } from 'react';
import {
  ActionButton,
  CardText,
  Chips,
  L,
  LearnJump,
  Note,
  Readout,
  SmallText,
  Stage,
  Steps,
  ValueSlider,
  num,
  numSpec,
  numTrap,
  opt,
  pt,
  screenAngle,
  step,
  trapNear,
  useKit,
  useLaunched,
  useNumStep,
  useQuizStep,
  useTr,
  type Colors,
  type PrimBuilder,
} from '../../kit';
import { useLearn } from '../../nav';
import { Trig } from '../../../practice/trig';
import { bolt } from '../Lesson4';
import { useGuessStep } from './shared';

/** Malý křížek kladných směrů +x → a +y ↑ v rohu scény (počátek v x, y). */
function signKey(b: PrimBuilder, c: Colors, x: number, y: number, moment = false) {
  b.force(x, y, x + 34, y, c.inkSoft, 1.4, 7);
  b.force(x, y, x, y - 34, c.inkSoft, 1.4, 7);
  b.text(x + 38, y + 4, '+x', c.inkSoft, 12, 'start');
  b.text(x, y - 40, '+y', c.inkSoft, 12);
  if (moment) b.text(x + 38, y - 24, '↺ +', c.inkSoft, 12, 'start');
}

/** Šikmá síla: hrot v (tx, ty), úhel alpha k nosníku, síla míří dolů doprava → počátek. */
const forceTail = (tx: number, ty: number, alpha: number, len: number): [number, number] => [tx - len * Trig.cosD(alpha), ty - len * Trig.sinD(alpha)];

const close = (v: number | null | undefined, w: number) => v != null && Math.abs(v - w) <= Math.max(0.03, Math.abs(w) * 0.01);
const absOf = (v: number | null | undefined) => (v == null ? null : Math.abs(v));

/** Karta 0: úhel od vodorovné × od svislice – složka u úhlu → cos, naproti → sin. */
function FromVertical() {
  const t = useTr();
  const kit = useKit();
  const guess = kit.mut<number | null>(null);
  const vertical = kit.mut(false);
  const demoDone = kit.mut(false);
  useLaunched([guess.v !== null], async (job) => {
    if (guess.v === null) return;
    for (let i = 0; i < 2; i++) {
      await job.delay(1100);
      vertical.v = true;
      await job.delay(1300);
      vertical.v = false;
    }
    await job.delay(600);
    vertical.v = true;
    demoDone.v = true;
  });
  const s1 = useGuessStep(
    L('Odhadni: změní se složky, když úhel změříš od svislice?', 'Предскажи: изменятся ли составляющие, если угол отсчитать от вертикали?'),
    [L('Změní se', 'Изменятся'), L('Nezmění se', 'Не изменятся')],
    guess.v,
    (i) => (guess.v = i),
  );
  const s2 = step(
    L('Porovnej 60° od vodorovné a 30° od svislice', 'Сравни 60° от горизонтали и 30° от вертикали'),
    demoDone.v,
    () => <SmallText text={L('Šipka stojí, mění se jen vyznačený úhel…', 'Стрелка неподвижна, меняется только отмеченный угол…')} />,
    () => (
      <Note
        tone={guess.v === 1 ? 'ok' : 'info'}
        text={L(
          (guess.v === 1 ? 'Tip sedí. ' : 'Šipka se ani nepohnula – mění se jen popis úhlu. ') + 'Složky zůstávají stejné. Mění se jen to, která složka leží **u** úhlu.',
          (guess.v === 1 ? 'Прогноз верен. ' : 'Стрелка даже не сдвинулась — меняется только описание угла. ') + 'Составляющие те же. Меняется лишь то, какая составляющая лежит **у** угла.',
        )}
      />
    ),
  );
  const s3 = useNumStep(
    L('Síla 10 kN míří vpravo dolů a se **svislicí** svírá 30°. Kolik je [[F_x]] i se znaménkem?', 'Сила 10 кН направлена вправо вниз и составляет с **вертикалью** 30°. Чему равна [[F_x]] со знаком?'),
    [numSpec(L('[[F_x]]', '[[F_x]]'), 10 * Trig.sinD(30), t.pick(L('kN', 'кН')), true)],
    L(
      'Úhel leží u svislice, vodorovná složka je naproti: [[F_x]] = 10 · sin 30° = 10 · cos 60° = +5 kN. [[F_y]] = −10 · cos 30° ≈ −8,66 kN. Znaménka určuje směr šipky.',
      'Угол лежит у вертикали, горизонтальная составляющая — напротив: [[F_x]] = 10 · sin 30° = 10 · cos 60° = +5 кН. [[F_y]] = −10 · cos 30° ≈ −8,66 кН. Знаки определяются направлением стрелки.',
    ),
    [
      numTrap(L('8,66 = 10 · cos 30° je složka **u** úhlu – ta svislá. Která složka leží naproti úhlu 30°?', '8,66 = 10 · cos 30° — составляющая **у** угла, вертикальная. Какая составляющая лежит напротив угла 30°?'), (v) => close(absOf(v[0]), 8.66)),
      numTrap(L('Velikost sedí. Míří šipka doprava (+), nebo doleva (−)?', 'Модуль верен. Стрелка направлена вправо (+) или влево (−)?'), (v) => close(v[0], -5)),
      numTrap(L('Síla je šikmá – vodorovně působí jen její část.', 'Сила наклонная — по горизонтали действует только её часть.'), (v) => close(absOf(v[0]), 10)),
    ],
    L('Úhel je u svislice. Složka naproti úhlu → sin.', 'Угол у вертикали. Составляющая напротив угла → sin.'),
    L('Vypočítej [[F_x]] z úhlu 30° od svislice', 'Вычисли [[F_x]] по углу 30° от вертикали'),
  );
  const steps = [s1, s2, s3];
  const cur = steps.findIndex((s) => !s.done);
  const showVertical = cur === 0 ? false : cur === 1 ? vertical.v : true;
  return (
    <>
      <Stage
        h={220}
        description={t.pick(L('Šikmá síla 10 kN na nosník s vyznačeným úhlem', 'Наклонная сила 10 кН на балку с отмеченным углом'))}
        draw={(b, c) => {
          const tx = 230;
          const ty = 170;
          const len = 140;
          b.beam(30, 370, ty + 5);
          signKey(b, c, 320, 120);
          const [sx, sy] = forceTail(tx, ty, 60, len);
          b.line(sx, sy, tx, sy, c.derived, 1.0, true);
          b.line(sx, sy, sx, ty, c.derived, 1.0, true);
          if (cur < 0) {
            b.force(sx, ty - 2, tx, ty - 2, c.derived, 2.2);
            b.force(tx, sy, tx, ty, c.derived, 2.2);
            b.text(tx - (tx - sx) / 2, ty - 10, t.pick(L(' = +5 kN', ' = +5 кН')), c.derived, 12, 'middle', 'F_x');
            b.text(tx + 8, ty - (ty - sy) / 2, t.pick(L(' = −8,66 kN', ' = −8,66 кН')), c.derived, 12, 'start', 'F_y');
          }
          b.force(sx, sy, tx, ty, c.load, 3.0);
          b.text(sx - 8, sy + 4, t.pick(L(' = 10 kN', ' = 10 кН')), c.load, 14, 'end', 'F');
          if (!showVertical) {
            b.angleArc(tx, ty, 38, 180, 240, null, c.ink);
            b.text(tx - 46, ty - 14, t.pick(L('60° od vodorovné', '60° от горизонтали')), c.ink, 14, 'end', null, true);
          } else {
            b.line(tx, ty, tx, ty - 130, c.inkSoft, 1.0, true);
            b.angleArc(tx, ty, 50, -90, 240, null, c.ink);
            b.text(tx - 30, ty - 100, t.pick(L('30° od svislice', '30° от вертикали')), c.ink, 14, 'end', null, true);
          }
        }}
      />
      <CardText
        text={L(
          'Stejnou sílu lze popsat úhlem od vodorovné nebo od svislice. Spolehlivé pravidlo: **složka u úhlu → cos, složka naproti → sin**.',
          'Одну силу можно задать углом от горизонтали или от вертикали. Надёжное правило: **составляющая у угла → cos, напротив → sin**.',
        )}
      />
      <Steps steps={steps} />
    </>
  );
}

/** Složky (fx, fy) v kN a jejich výslednice; ghost = dřívější výslednice pro srovnání. */
function resultantFig(b: PrimBuilder, c: Colors, fx: number, fy: number, rLabel: string | null, angleLabel: string | null, ghost: [number, number] | null) {
  const cx = 200;
  const cy = 140;
  const u = 15;
  b.axes(cx, cy, 175, false);
  b.text(cx + 171, cy + 16, '+x', c.inkSoft, 13, 'end');
  b.text(cx + 10, cy - 130, '+y', c.inkSoft, 13, 'start');
  if (ghost) b.force(cx, cy, cx + ghost[0] * u, cy - ghost[1] * u, c.load, 2.0, 11, false, 0.3, true);
  const tx = cx + fx * u;
  const ty = cy - fy * u;
  if (fx !== 0) b.force(cx, cy, tx, cy, c.derived, 2.4);
  if (fy !== 0) b.force(cx, cy, cx, ty, c.derived, 2.4);
  b.line(tx, cy, tx, ty, c.derived, 1.0, true);
  b.line(cx, ty, tx, ty, c.derived, 1.0, true);
  const sg = (v: number) => (v > 0 ? `+${v}` : v < 0 ? `−${-v}` : '0');
  if (fx !== 0) b.text((cx + tx) / 2, cy + (fy >= 0 ? 18 : -8), ` = ${sg(fx)}`, c.derived, 12, 'middle', 'F_x');
  if (fy !== 0) b.text(cx + (fx >= 0 ? -8 : 8), (cy + ty) / 2 + 5, ` = ${sg(fy)}`, c.derived, 12, fx >= 0 ? 'end' : 'start', 'F_y');
  if (fx !== 0 || fy !== 0) {
    b.force(cx, cy, tx, ty, c.load, 3.0);
    if (rLabel !== null) b.text(tx + (fx >= 0 ? 8 : -8), ty + (fy >= 0 ? -6 : 16), rLabel, c.load, 13, fx >= 0 ? 'start' : 'end', 'R');
    if (angleLabel !== null && fy !== 0) b.angleArc(cx, cy, 30, 0, screenAngle(pt(cx, cy), pt(tx, ty)), angleLabel);
  }
}

/** Karta 1: ze složek celá síla – velikost Pythagorem, směr arctg a znaménky. */
function Resultant() {
  const t = useTr();
  const kit = useKit();
  const kn = t.pick(L('kN', 'кН'));
  const guess = kit.mut<number | null>(null);
  const fx = kit.mut(4);
  const tested = kit.mut(false);
  const ex = kit.mut(6);
  const ey = kit.mut(8);
  const sq = (v: number) => (v < 0 ? `(−${-v})²` : `${v}²`);
  const s1 = useGuessStep(
    L('Odhadni, co udělá změna [[F_x]] z +4 na −4 kN ([[F_y]] = +3 kN)', 'Предскажи, что даст замена [[F_x]] с +4 на −4 кН ([[F_y]] = +3 кН)'),
    [L('Jen směr', 'Только направление'), L('Jen velikost', 'Только модуль'), L('Obojí', 'И то и другое')],
    guess.v,
    (i) => (guess.v = i),
  );
  const s2 = step(
    L('Změň znaménko [[F_x]] a porovnej výslednice', 'Измени знак [[F_x]] и сравни равнодействующие'),
    tested.v,
    () => (
      <>
        <Chips options={[L('[[F_x]] = +4 kN', '[[F_x]] = +4 кН'), L('[[F_x]] = −4 kN', '[[F_x]] = −4 кН')]} selected={fx.v > 0 ? 0 : 1} onPick={(i) => (fx.v = i === 0 ? 4 : -4)} />
        <Readout text={`R = √(${sq(fx.v)} + 3²) = 5 ${kn}`} />
        <ActionButton text={L('Ověřit', 'Проверить')} enabled={fx.v === -4} onClick={() => (tested.v = true)} />
      </>
    ),
    () => (
      <Note
        tone={guess.v === 0 ? 'ok' : 'info'}
        text={L(
          (guess.v === 0 ? 'Tip sedí. ' : '') + 'R = √(4² + 3²) = √((−4)² + 3²) = 5 kN – velikost se nezměnila, výslednice se jen zrcadlově otočila doleva.',
          (guess.v === 0 ? 'Прогноз верен. ' : '') + 'R = √(4² + 3²) = √((−4)² + 3²) = 5 кН — модуль не изменился, равнодействующая лишь зеркально повернулась влево.',
        )}
      />
    ),
  );
  const s3 = useNumStep(
    L('[[F_x]] = 6 kN, [[F_y]] = 8 kN. Jak velká je výslednice R?', '[[F_x]] = 6 кН, [[F_y]] = 8 кН. Каков модуль равнодействующей R?'),
    [numSpec(L('R', 'R'), 10, kn)],
    L('R = √(6² + 8²) = √100 = 10 kN.', 'R = √(6² + 8²) = √100 = 10 кН.'),
    [
      trapNear(L('Složky se nesčítají přímo – jsou na sebe kolmé. Která věta platí pro pravoúhlý trojúhelník?', 'Составляющие нельзя просто сложить — они перпендикулярны. Какая теорема работает в прямоугольном треугольнике?'), 14),
      trapNear(L('100 je teprve R². Ještě odmocni.', '100 — это пока R². Осталось извлечь корень.'), 100),
    ],
    L('R² = [[F_x]]² + [[F_y]]².', 'R² = [[F_x]]² + [[F_y]]².'),
    L('Vypočítej R pro [[F_x]] = 6 kN a [[F_y]] = 8 kN', 'Вычисли R при [[F_x]] = 6 кН и [[F_y]] = 8 кН'),
  );
  const s4 = useNumStep(
    L('Jaký úhel svírá výslednice s kladnou osou x? (na jedno desetinné místo)', 'Какой угол равнодействующая составляет с положительной осью x? (до десятых)'),
    [numSpec(L('α', 'α'), Trig.atanD(8 / 6), '°', false, 'angle')],
    L(
      'tg α = [[F_y]] / [[F_x]] = 8/6, α = arctg (8/6) ≈ 53,1°. Obě složky jsou kladné, takže výslednice míří doprava nahoru a tohle je i její směr od +x.',
      'tg α = [[F_y]] / [[F_x]] = 8/6, α = arctg (8/6) ≈ 53,1°. Обе составляющие положительны, поэтому равнодействующая направлена вправо вверх, и это же её направление от +x.',
    ),
    [
      trapNear(L('To je úhel od svislice – vyjde z [[F_x]] / [[F_y]]. Naproti úhlu u osy x leží [[F_y]].', 'Это угол от вертикали — так выходит из [[F_x]] / [[F_y]]. Напротив угла у оси x лежит [[F_y]].'), Trig.atanD(6 / 8)),
      trapNear(L('Kalkulačka je v režimu RAD – přepni na DEG.', 'Калькулятор в режиме RAD — переключи на DEG.'), Math.atan(8 / 6)),
      trapNear(L('8/6 ≈ 1,33 je teprve tg α. Úhel dá tan⁻¹.', '8/6 ≈ 1,33 — это пока tg α. Угол даст tan⁻¹.'), 8 / 6),
    ],
    L('tg α = protilehlá / přilehlá = [[F_y]] / [[F_x]].', 'tg α = противолежащий / прилежащий = [[F_y]] / [[F_x]].'),
    L('Vypočítej úhel výslednice od kladné osy x', 'Вычисли угол равнодействующей от положительной оси x'),
  );
  const steps = [s1, s2, s3, s4];
  const cur = steps.findIndex((s) => !s.done);
  const afterControls = (): ReactNode => {
    const r = Math.hypot(ex.v, ey.v);
    let dir: L;
    if (ex.v === 0 && ey.v === 0) dir = L('R = 0 – směr není určen', 'R = 0 — направление не определено');
    else if (ey.v === 0) dir = ex.v > 0 ? L('vodorovně doprava', 'горизонтально вправо') : L('vodorovně doleva', 'горизонтально влево');
    else if (ex.v === 0) dir = ey.v > 0 ? L('svisle nahoru', 'вертикально вверх') : L('svisle dolů', 'вертикально вниз');
    else {
      let phi = (Math.atan2(ey.v, ex.v) * 180) / Math.PI;
      if (phi < 0) phi += 360;
      const lr = ex.v > 0 ? L('doprava', 'вправо') : L('doleva', 'влево');
      const ud = ey.v > 0 ? L('nahoru', 'вверх') : L('dolů', 'вниз');
      dir = L(`${lr.cs} ${ud.cs}, ${num(phi, 1)}° od +x`, `${lr.ru} ${ud.ru}, ${num(phi, 1)}° от +x`);
    }
    return (
      <>
        <ValueSlider label={`[[F_x]] = ${ex.v} ${kn}`} value={ex.v} min={-8} max={8} steps={15} onChange={(v) => (ex.v = Math.round(v))} />
        <ValueSlider label={`[[F_y]] = ${ey.v} ${kn}`} value={ey.v} min={-8} max={8} steps={15} onChange={(v) => (ey.v = Math.round(v))} />
        <Readout text={`R = √(${sq(ex.v)} + ${sq(ey.v)}) ${Math.abs(r - Math.round(r)) < 1e-9 ? '=' : '≈'} ${num(r, 2)} ${kn} · ${t.pick(dir)}`} />
      </>
    );
  };
  return (
    <>
      <Stage
        h={280}
        description={t.pick(L('Složky a jejich výslednice v osách x, y', 'Составляющие и их равнодействующая в осях x, y'))}
        draw={(b, c) => {
          if (cur === 0) resultantFig(b, c, 4, 3, ` = 5 ${kn}`, null, null);
          else if (cur === 1) resultantFig(b, c, fx.v, 3, ` = 5 ${kn}`, null, fx.v < 0 ? [4, 3] : null);
          else if (cur === 2) resultantFig(b, c, 6, 8, ' = ?', null, null);
          else if (cur === 3) resultantFig(b, c, 6, 8, ` = 10 ${kn}`, 'α = ?', null);
          else {
            const r = Math.hypot(ex.v, ey.v);
            resultantFig(b, c, ex.v, ey.v, r > 0 ? ` ≈ ${num(r, 2)} ${kn}` : null, null, null);
          }
        }}
      />
      <CardText
        text={L(
          'Kolmé složky určují velikost i směr výslednice. Velikost dá Pythagorova věta, směr arctg – a znaménka složek.',
          'Перпендикулярные составляющие определяют модуль и направление равнодействующей. Модуль даёт теорема Пифагора, направление — arctg и знаки составляющих.',
        )}
      />
      <Steps steps={steps} after={afterControls} />
    </>
  );
}

/** Karta 2: součet dvou sil po složkách (obě působí v jednom bodě). Šipky v jednom měřítku 12 px = 1 kN. */
function SumForces() {
  const t = useTr();
  const kn = t.pick(L('kN', 'кН'));
  const f1x = 10 * Trig.cosD(30);
  const s1 = useNumStep(
    L('Kolik je vodorovná složka [[F_1x]] síly [[F_1]] = 10 kN (30° pod vodorovnou, vpravo dolů)? cos 30° ≈ 0,866', 'Чему равна горизонтальная составляющая [[F_1x]] силы [[F_1]] = 10 кН (30° ниже горизонтали, вправо вниз)? cos 30° ≈ 0,866'),
    [numSpec(L('[[F_1x]]', '[[F_1x]]'), f1x, kn, true)],
    L('Úhel je u vodorovné, takže [[F_1x]] = 10 · cos 30° ≈ +8,66 kN (doprava).', 'Угол у горизонтали, поэтому [[F_1x]] = 10 · cos 30° ≈ +8,66 кН (вправо).'),
    [
      numTrap(L('5 kN = 10 · sin 30° je svislá složka – leží naproti úhlu.', '5 кН = 10 · sin 30° — вертикальная составляющая, она напротив угла.'), (v) => close(absOf(v[0]), 5)),
      numTrap(L('Velikost sedí. Míří [[F_1]] doprava (+), nebo doleva (−)?', 'Модуль верен. [[F_1]] направлена вправо (+) или влево (−)?'), (v) => close(v[0], -f1x)),
    ],
    L('Složka u úhlu → cos.', 'Составляющая у угла → cos.'),
    L('Vypočítej vodorovnou složku síly [[F_1]]', 'Вычисли горизонтальную составляющую силы [[F_1]]'),
  );
  const s2 = useNumStep(
    L('Sečti všechny vodorovné složky: Σ[[F_x]] = [[F_1x]] + [[F_2x]]. [[F_2]] = 3 kN míří doleva.', 'Сложи все горизонтальные составляющие: Σ[[F_x]] = [[F_1x]] + [[F_2x]]. [[F_2]] = 3 кН направлена влево.'),
    [numSpec(L('Σ[[F_x]]', 'Σ[[F_x]]'), f1x - 3, kn, true)],
    L('Σ[[F_x]] = +8,66 + (−3) ≈ +5,66 kN.', 'Σ[[F_x]] = +8,66 + (−3) ≈ +5,66 кН.'),
    [
      numTrap(L('[[F_2]] míří doleva. Jaké znaménko má v součtu?', '[[F_2]] направлена влево. С каким знаком она входит в сумму?'), (v) => close(absOf(v[0]), 11.66)),
      numTrap(L('To je 10 − 3. Do vodorovného součtu patří jen vodorovná složka šikmé síly, ne celá síla.', 'Это 10 − 3. В горизонтальную сумму входит только горизонтальная составляющая наклонной силы, а не вся сила.'), (v) => close(absOf(v[0]), 7)),
      numTrap(L('To je 5 − 3, ale 5 kN je svislá složka [[F_1]]. Vezmi [[F_1x]] z minulého kroku.', 'Это 5 − 3, но 5 кН — вертикальная составляющая [[F_1]]. Возьми [[F_1x]] из прошлого шага.'), (v) => close(absOf(v[0]), 2)),
      numTrap(L('Velikost sedí. Kam součet míří – doprava (+), nebo doleva (−)?', 'Модуль верен. Куда направлена сумма — вправо (+) или влево (−)?'), (v) => close(v[0], -(f1x - 3))),
    ],
    L('Doleva je záporně: Σ[[F_x]] = 8,66 − 3.', 'Влево — отрицательно: Σ[[F_x]] = 8,66 − 3.'),
    L('Sečti všechny vodorovné složky Σ[[F_x]]', 'Сложи все горизонтальные составляющие Σ[[F_x]]'),
  );
  const s3 = useNumStep(
    L('Sečti všechny svislé složky Σ[[F_y]]. sin 30° = 0,5; [[F_2]] je vodorovná.', 'Сложи все вертикальные составляющие Σ[[F_y]]. sin 30° = 0,5; [[F_2]] горизонтальна.'),
    [numSpec(L('Σ[[F_y]]', 'Σ[[F_y]]'), -5, kn, true)],
    L(
      'Σ[[F_y]] = −10 · sin 30° + 0 = −5 kN. Výslednice R = (+5,66; −5) kN ≈ 7,55 kN míří vpravo dolů – stejně jako když šipky složíš za sebe.',
      'Σ[[F_y]] = −10 · sin 30° + 0 = −5 кН. Равнодействующая R = (+5,66; −5) кН ≈ 7,55 кН направлена вправо вниз — так же, как если сложить стрелки одну за другой.',
    ),
    [
      numTrap(L('Velikost sedí. [[F_1]] míří dolů – jaké znaménko?', 'Модуль верен. [[F_1]] направлена вниз — какой знак?'), (v) => close(v[0], 5)),
      numTrap(L('[[F_2]] je vodorovná – svislou složku nemá. Svisle působí jen [[F_1]].', '[[F_2]] горизонтальна — вертикальной составляющей у неё нет. По вертикали действует только [[F_1]].'), (v) => close(absOf(v[0]), 8) || close(absOf(v[0]), 2)),
    ],
    L('Svislá složka [[F_1]] leží naproti úhlu 30° → sin.', 'Вертикальная составляющая [[F_1]] лежит напротив угла 30° → sin.'),
    L('Sečti všechny svislé složky Σ[[F_y]]', 'Сложи все вертикальные составляющие Σ[[F_y]]'),
  );
  const steps = [s1, s2, s3];
  const cur = steps.findIndex((s) => !s.done);
  const doneN = cur < 0 ? 3 : cur;
  return (
    <>
      <Stage
        h={200}
        description={t.pick(L('Bod se dvěma silami: šikmá F1 a vodorovná F2', 'Точка с двумя силами: наклонная F1 и горизонтальная F2'))}
        draw={(b, c) => {
          const u = 12;
          const cx = 160;
          const cy = 70;
          signKey(b, c, 40, 185);
          b.line(30, cy, 370, cy, c.inkSoft, 1.0, true);
          const tx = cx + 10 * u * Trig.cosD(30);
          const ty = cy + 10 * u * Trig.sinD(30);
          if (doneN >= 1) {
            b.line(tx, cy, tx, ty, c.derived, 1.0, true);
            b.force(cx, cy, tx, cy, c.derived, 2.2);
            b.text((cx + tx) / 2 + 10, cy - 8, ' = +8,66', c.derived, 12, 'middle', 'F_1x');
          }
          if (doneN >= 2) {
            b.force(cx, cy - 30, cx + (f1x - 3) * u, cy - 30, c.accent, 2.6);
            b.text(cx + 4, cy - 38, ' = +5,66', c.accent, 12, 'start', 'ΣF_x');
          }
          if (doneN >= 3) {
            b.force(tx, ty, tx - 3 * u, ty, c.load, 2.0, 11, false, 0.5);
            b.force(cx, cy, tx - 3 * u, ty, c.accent, 2.6, 11, false, 1, true);
            b.text(tx - 3 * u - 8, ty + 18, ` ≈ 7,55 ${kn}`, c.accent, 13, 'end', 'R');
            b.force(cx, cy, cx, cy + 5 * u, c.accent, 2.6);
            b.text(cx - 8, cy + 5 * u, ' = −5', c.accent, 12, 'end', 'ΣF_y');
          }
          b.force(cx, cy, tx, ty, c.load, 3.0);
          b.text(tx + 8, ty + 4, ` = 10 ${kn}`, c.load, 14, 'start', 'F_1');
          b.angleArc(cx, cy, 40, 0, 30, '30°');
          b.force(cx, cy, cx - 3 * u, cy, c.load, 3.0);
          b.text(cx - 40, cy + 20, ` = 3 ${kn}`, c.load, 14, 'end', 'F_2');
          b.circle(cx, cy, 6, c.ink);
        }}
      />
      <CardText
        text={L(
          'Síly se sčítají jako vektory. Po složkách to jde nejsnáz: zvlášť součet ve směru x a zvlášť ve směru y – se znaménky. Obě síly působí v jednom bodě.',
          'Силы складываются как векторы. Проще всего — по составляющим: отдельно сумма по x и отдельно по y, со знаками. Обе силы приложены в одной точке.',
        )}
      />
      <Steps steps={steps} />
    </>
  );
}

/** Karta 3: rameno momentu r⊥ = r · sin α. */
function MomentArm() {
  const t = useTr();
  const kit = useKit();
  const guess = kit.mut<number | null>(null);
  const alpha = kit.mut(60);
  const maxDone = kit.mut(false);
  const miss2 = kit.mut(false);
  const zeroDone = kit.mut(false);
  const miss4 = kit.mut(false);
  const r = 2;
  const rp = (a: number) => r * Trig.sinD(a);
  const mm = t.pick(L('m', 'м'));
  const slider = () => (
    <>
      <ValueSlider
        label={t.pick(L(`Úhel α mezi silou a pákou = ${alpha.v}°`, `Угол α между силой и рычагом = ${alpha.v}°`))}
        value={alpha.v}
        min={0}
        max={90}
        steps={17}
        onChange={(v) => {
          alpha.v = Math.round(v / 5) * 5;
          miss2.v = false;
          miss4.v = false;
        }}
      />
      <Readout text={`[[r_⊥]] = 2 · sin ${alpha.v}° ${alpha.v % 90 === 0 || alpha.v === 30 ? '=' : '≈'} ${num(rp(alpha.v), 2)} ${mm}`} />
    </>
  );
  const s1 = useGuessStep(L('Odhadni úhel α, při kterém bude moment největší', 'Предскажи угол α, при котором момент будет наибольшим'), [L('0°', '0°'), L('30°', '30°'), L('90°', '90°')], guess.v, (i) => (guess.v = i));
  const s2 = step(
    L('Natoč sílu tak, aby měla co největší rameno', 'Поверни силу так, чтобы плечо было наибольшим'),
    maxDone.v,
    () => (
      <>
        {slider()}
        <ActionButton text={L('Zkontrolovat', 'Проверить')} onClick={() => (alpha.v === 90 ? (maxDone.v = true) : (miss2.v = true))} />
        {miss2.v && <Note tone="nudge" text={L(`Rameno ${num(rp(alpha.v), 2)} m – jde to víc?`, `Плечо ${num(rp(alpha.v), 2)} м — можно больше?`)} />}
      </>
    ),
    () => (
      <Note
        tone={guess.v === 2 ? 'ok' : 'info'}
        text={L(
          (guess.v === 2 ? 'Tip sedí. ' : '') + 'Při 90° je rameno celá délka páky, 2 m, a moment 10 · 2 = 20 kN·m – víc to nejde.',
          (guess.v === 2 ? 'Прогноз верен. ' : '') + 'При 90° плечо равно всей длине рычага, 2 м, а момент 10 · 2 = 20 кН·м — больше не бывает.',
        )}
      />
    ),
  );
  const s3 = useNumStep(
    L('Síla 10 kN svírá s pákou 2 m úhel 30°. Jak velký je moment |M| k bodu O?', 'Сила 10 кН составляет с рычагом 2 м угол 30°. Каков модуль момента |M| относительно точки O?'),
    [numSpec(L('|M|', '|M|'), 10, t.pick(L('kN·m', 'кН·м')))],
    L('[[r_⊥]] = 2 · sin 30° = 1 m, |M| = F · [[r_⊥]] = 10 · 1 = 10 kN·m.', '[[r_⊥]] = 2 · sin 30° = 1 м, |M| = F · [[r_⊥]] = 10 · 1 = 10 кН·м.'),
    [
      trapNear(L('10 · 2 platí jen pro sílu kolmou k páce. Jaké je tady rameno [[r_⊥]]?', '10 · 2 верно только для силы, перпендикулярной рычагу. Каково здесь плечо [[r_⊥]]?'), 20),
      trapNear(L('Rameno je odvěsna **naproti** α, tedy r · sin α, ne r · cos α.', 'Плечо — катет **напротив** α, то есть r · sin α, а не r · cos α.'), 20 * Trig.cosD(30)),
    ],
    L('|M| = F · [[r_⊥]], [[r_⊥]] = r · sin α.', '|M| = F · [[r_⊥]], [[r_⊥]] = r · sin α.'),
    L('Vypočítej velikost momentu při 30°', 'Вычисли модуль момента при 30°'),
  );
  const s4 = step(
    L('Najdi směr síly, při kterém je moment nulový', 'Найди направление силы, при котором момент равен нулю'),
    zeroDone.v,
    () => (
      <>
        {slider()}
        <ActionButton text={L('Zkontrolovat', 'Проверить')} onClick={() => (alpha.v === 0 ? (zeroDone.v = true) : (miss4.v = true))} />
        {miss4.v && <Note tone="nudge" text={L(`Rameno je ještě ${num(rp(alpha.v), 2)} m. Kdy nositelka projde bodem O?`, `Плечо ещё ${num(rp(alpha.v), 2)} м. Когда линия действия пройдёт через O?`)} />}
      </>
    ),
    () => (
      <Note
        tone="ok"
        text={L(
          'Při 0° nositelka prochází bodem O – rameno je 0, takže moment je 0, i když síla 10 kN pořád působí. Obecně |M| = F · r · sin α.',
          'При 0° линия действия проходит через O — плечо 0, значит и момент 0, хотя сила 10 кН по-прежнему действует. В общем случае |M| = F · r · sin α.',
        )}
      />
    ),
  );
  const steps = [s1, s2, s3, s4];
  const cur = steps.findIndex((s) => !s.done);
  const a = cur === 0 ? 60 : cur === 2 ? 30 : alpha.v;
  const showArm = cur !== 0;
  const armLabel = cur === 2 ? ' = ?' : ` = ${num(rp(a), 2)} ${mm}`;
  return (
    <>
      <Stage
        h={250}
        description={t.pick(L('Páka 2 m, síla 10 kN na konci pod úhlem α a její rameno', 'Рычаг 2 м, сила 10 кН на конце под углом α и её плечо'))}
        draw={(b, c) => {
          const ox = 70;
          const oy = 140;
          const unit = 115;
          const ex = ox + r * unit;
          const ux = -Trig.cosD(a);
          const uy = -Trig.sinD(a);
          const len = 80;
          const tailX = ex - ux * len;
          const tailY = oy - uy * len;
          b.line(ex - ux * 260, oy - uy * 260, ex + ux * 260, oy + uy * 260, c.inkSoft, 1.0, true);
          b.rect(ox, oy - 6, r * unit, 12, c.beam, c.ink, 1.4);
          bolt(b, ox, oy, 16, 0, c);
          b.text(ox - 4, oy + 34, 'O', c.ink, 14, 'middle', null, true);
          b.force(tailX, tailY, ex, oy, c.load, 3.0);
          if (a < 20) b.text(tailX - 4, tailY - 14, t.pick(L(' = 10 kN', ' = 10 кН')), c.load, 13, 'end', 'F');
          else b.text(tailX - 10, tailY + 4, t.pick(L(' = 10 kN', ' = 10 кН')), c.load, 13, 'end', 'F');
          if (a >= 1 && a <= 89) b.angleArc(ex, oy, 30, 180, 180 + a, 'α');
          if (a === 90) b.rightAngleMark(pt(ex, oy), pt(ox, oy), pt(ex, oy - 30));
          b.dimLine(ox, oy + 50, ex, oy + 50, t.pick(L('r = 2 m', 'r = 2 м')));
          if (showArm) {
            if (rp(a) > 0.02) {
              const tt = (ox - ex) * ux;
              const px = ex + ux * tt;
              const py = oy + uy * tt;
              b.line(ox, oy, px, py, c.accent, 2.4, true);
              if (a < 90) b.rightAngleMark(pt(px, py), pt(ox, oy), pt(ex, oy), 8);
              b.text((ox + px) / 2 - 10, (oy + py) / 2, armLabel, c.accent, 13, 'end', 'r_⊥');
            } else b.text(ex - 10, oy - 22, t.pick(L('rameno 0', 'плечо 0')), c.accent, 13, 'end');
          }
          b.text(392, 242, t.pick(L('jen geometrie síly – páku drží jiné vazby', 'только геометрия силы — рычаг удерживают другие связи')), c.inkSoft, 11, 'end');
        }}
      />
      <CardText
        text={L(
          '**Rameno** síly [[r_⊥]] je kolmá vzdálenost od bodu O k její nositelce. Nemusí být stejné jako délka páky. **Moment** |M| = F · [[r_⊥]].',
          '**Плечо** силы [[r_⊥]] — перпендикулярное расстояние от точки O до линии действия силы. Оно не обязательно равно длине рычага. **Момент** |M| = F · [[r_⊥]].',
        )}
      />
      <Steps steps={steps} />
      <LearnJump lesson={4} card={1} label={L('Základy statiky: Moment síly', 'Основы статики: момент силы')} />
    </>
  );
}

/** Karta 4: šikmý nosník – vodorovný průmět L · cos α je rameno svislé síly. */
function InclinedBeam() {
  const t = useTr();
  const kit = useKit();
  const mm = t.pick(L('m', 'м'));
  const guess = kit.mut<number | null>(null);
  const alpha = kit.mut(30);
  const tested = kit.mut(false);
  const lm = 5;
  const s1 = useGuessStep(
    L('Odhadni, jak se změní vodorovný průmět při sklonu 30° → 45°', 'Предскажи, как изменится горизонтальная проекция при наклоне 30° → 45°'),
    [L('Zvětší se', 'Увеличится'), L('Zmenší se', 'Уменьшится'), L('Nezmění se', 'Не изменится')],
    guess.v,
    (i) => (guess.v = i),
  );
  const s2 = step(
    L('Zvětši sklon na 45° a porovnej vodorovné průměty', 'Увеличь наклон до 45° и сравни горизонтальные проекции'),
    tested.v,
    () => (
      <>
        <ValueSlider label={t.pick(L(`Sklon α = ${alpha.v}°`, `Угол наклона α = ${alpha.v}°`))} value={alpha.v} min={30} max={45} steps={2} onChange={(v) => (alpha.v = Math.round(v / 5) * 5)} />
        <Readout text={t.pick(L(`d = 5 · cos ${alpha.v}° ≈ ${num(lm * Trig.cosD(alpha.v), 2)} m`, `d = 5 · cos ${alpha.v}° ≈ ${num(lm * Trig.cosD(alpha.v), 2)} м`))} />
        <ActionButton text={L('Ověřit', 'Проверить')} enabled={alpha.v === 45} onClick={() => (tested.v = true)} />
      </>
    ),
    () => (
      <Note
        tone={guess.v === 1 ? 'ok' : 'info'}
        text={L(
          (guess.v === 1 ? 'Tip sedí. ' : '') + 'Průmět se zkrátil z 4,33 m na 3,54 m: d = L · cos α a kosinus s rostoucím úhlem klesá. Délka nosníku zůstala 5 m.',
          (guess.v === 1 ? 'Прогноз верен. ' : '') + 'Проекция сократилась с 4,33 м до 3,54 м: d = L · cos α, а косинус с ростом угла убывает. Длина балки осталась 5 м.',
        )}
      />
    ),
  );
  const s3 = useNumStep(
    L('Krokev dlouhá 5 m má sklon 35° (cos 35° ≈ 0,8192). Jak dlouhý je její vodorovný průmět d?', 'Стропило длиной 5 м наклонено под углом 35° (cos 35° ≈ 0,8192). Какова его горизонтальная проекция d?'),
    [numSpec(L('d', 'd'), lm * Trig.cosD(35), mm)],
    L('Průmět leží u úhlu, proto kosinus: d = 5 · cos 35° ≈ 4,10 m.', 'Проекция лежит у угла, поэтому косинус: d = 5 · cos 35° ≈ 4,10 м.'),
    [
      trapNear(L('2,87 = 5 · sin 35° je výška – odvěsna naproti úhlu. Průmět leží u úhlu.', '2,87 = 5 · sin 35° — это высота, катет напротив угла. Проекция лежит у угла.'), lm * Trig.sinD(35)),
      trapNear(L('Průmět nemůže být delší než krokev sama. Násobit, nebo dělit?', 'Проекция не может быть длиннее самого стропила. Умножать или делить?'), lm / Trig.cosD(35)),
      trapNear(L('5 m by to bylo jen u vodorovného nosníku.', '5 м было бы только у горизонтальной балки.'), 5),
    ],
    L('Vodorovný průmět je odvěsna u úhlu: d = L · cos α.', 'Горизонтальная проекция — катет у угла: d = L · cos α.'),
    L('Vypočítej vodorovný průmět při sklonu 35°', 'Вычисли горизонтальную проекцию при наклоне 35°'),
  );
  const s4 = useQuizStep(
    L('Na horním konci krokve visí svislé břemeno. Jaké má rameno k dolnímu konci A?', 'На верхнем конце стропила висит вертикальный груз. Каково его плечо относительно нижнего конца A?'),
    [
      opt(L('4,10 m', '4,10 м'), true),
      opt(L('2,87 m', '2,87 м'), false, L('2,87 m je výška. Rameno je kolmá vzdálenost k **svislé** nositelce – ta se měří vodorovně.', '2,87 м — это высота. Плечо — перпендикулярное расстояние до **вертикальной** линии действия, оно измеряется по горизонтали.')),
      opt(L('5 m', '5 м'), false, L('5 m je délka po krokvi. Kolmá vzdálenost od A ke svislé nositelce je kratší.', '5 м — длина вдоль стропила. Перпендикулярное расстояние от A до вертикальной линии действия короче.')),
    ],
    L(
      'Nositelka svislé síly je svislá, kolmá vzdálenost k ní je vodorovná: rameno = d = L · cos α ≈ 4,10 m. (h = L · sin α ≈ 2,87 m by bylo rameno vodorovné síly.)',
      'Линия действия вертикальной силы вертикальна, расстояние до неё по перпендикуляру горизонтально: плечо = d = L · cos α ≈ 4,10 м. (h = L · sin α ≈ 2,87 м было бы плечом горизонтальной силы.)',
    ),
    null,
    3,
    L('Vyber rameno svislé síly v horním konci', 'Выбери плечо вертикальной силы на верхнем конце'),
  );
  const steps = [s1, s2, s3, s4];
  const cur = steps.findIndex((s) => !s.done);
  const a = cur === 0 ? 30 : cur === 1 ? alpha.v : 35;
  return (
    <>
      <Stage
        h={250}
        description={t.pick(L('Šikmý nosník délky 5 m s vodorovným průmětem a výškou', 'Наклонная балка длиной 5 м с горизонтальной проекцией и высотой'))}
        draw={(b, c) => {
          const x0 = 60;
          const y0 = 196;
          const unit = 40;
          const plan = lm * Trig.cosD(a);
          const rise = lm * Trig.sinD(a);
          const ex = x0 + plan * unit;
          const ey = y0 - rise * unit;
          b.line(20, y0, 380, y0, c.ink, 1.2);
          if (cur === 1 && alpha.v !== 30) {
            const gx = x0 + lm * Trig.cosD(30) * unit;
            const gy = y0 - lm * Trig.sinD(30) * unit;
            b.line(x0, y0, gx, gy, c.inkSoft, 3.0, false, 0.35);
            b.line(gx, gy, gx, y0, c.inkSoft, 1.0, true);
          }
          b.line(x0, y0, ex, ey, c.ink, 9.0);
          b.line(x0, y0, ex, ey, c.beam, 6.0);
          b.text((x0 + ex) / 2 - 12, (y0 + ey) / 2 - 12, t.pick(L('L = 5 m', 'L = 5 м')), c.ink, 13, 'end');
          b.angleArc(x0, y0, 34, 0, -a, `${a}°`);
          b.line(ex, ey, ex, y0, c.derived, 1.0, true);
          const dTxt = cur !== 2 ? `d ≈ ${num(plan, 2)} ${mm}` : 'd = ?';
          b.dimLine(x0, y0 + 20, ex, y0 + 20, dTxt, 14, c.derived);
          if (cur >= 3 || cur < 0) {
            b.text(ex + 10, (y0 + ey) / 2 + 5, `h ≈ ${num(rise, 2)} ${mm}`, c.derived, 13, 'start');
            b.force(ex, ey - 60, ex, ey - 4, c.load, 3.0);
            b.text(ex + 8, ey - 40, null, c.load, 14, 'start', 'F');
            b.circle(x0, y0, 5, c.ink);
            b.text(x0 - 8, y0 - 6, 'A', c.ink, 14, 'end', null, true);
          } else {
            b.dimLine(ex + 18, y0, ex + 18, ey, '', 14, c.derived);
            b.text(ex + 26, (y0 + ey) / 2 + 5, t.pick(L(`výška ${num(rise, 2)} m`, `высота ${num(rise, 2)} м`)), c.derived, 13, 'start');
          }
        }}
      />
      <CardText
        text={L(
          'Šikmý nosník (krokev, schodnice) je přepona. Jeho **vodorovný průmět** d = L · cos α je kratší než jeho délka – a právě ten určuje rameno svislé síly.',
          'Наклонная балка (стропило, косоур) — это гипотенуза. Её **горизонтальная проекция** d = L · cos α короче длины — и именно она задаёт плечо вертикальной силы.',
        )}
      />
      <Steps steps={steps} />
    </>
  );
}

/** Karta 5: závěrečná úloha – složky se znaménky a moment k bodu A. */
function FinalTask() {
  const t = useTr();
  const learn = useLearn();
  const kn = t.pick(L('kN', 'кН'));
  const fxAns = -12 * Trig.cosD(60);
  const fyAns = -12 * Trig.sinD(60);
  const mAns = fyAns * 2;
  const s1 = useNumStep(
    L('Vypočítej [[F_x]] i se znaménkem (sin 60° ≈ 0,866; cos 60° = 0,5).', 'Вычисли [[F_x]] со знаком (sin 60° ≈ 0,866; cos 60° = 0,5).'),
    [numSpec(L('[[F_x]]', '[[F_x]]'), fxAns, kn, true)],
    L('Úhel je u vodorovné a síla míří doleva: [[F_x]] = −12 · cos 60° = −6 kN.', 'Угол у горизонтали, сила направлена влево: [[F_x]] = −12 · cos 60° = −6 кН.'),
    [
      numTrap(L('Velikost sedí. Kam míří šipka vodorovně – doprava (+), nebo doleva (−)?', 'Модуль верен. Куда стрелка направлена по горизонтали — вправо (+) или влево (−)?'), (v) => close(v[0], 6)),
      numTrap(L('Vypadá to na sin místo cos. Která složka leží **u** úhlu 60°?', 'Похоже на sin вместо cos. Какая составляющая лежит **у** угла 60°?'), (v) => close(absOf(v[0]), 10.39)),
      numTrap(L('Složka nemůže být stejně velká jako celá síla 12 kN – je to jen její část.', 'Составляющая не может равняться всей силе 12 кН — это только её часть.'), (v) => close(absOf(v[0]), 12)),
    ],
    L('Složka u úhlu → cos; doleva je −.', 'Составляющая у угла → cos; влево — это −.'),
    L('Vypočítej [[F_x]] včetně znaménka', 'Вычисли [[F_x]] с учётом знака'),
  );
  const s2 = useNumStep(
    L('Vypočítej [[F_y]] i se znaménkem (sin 60° ≈ 0,866; cos 60° = 0,5).', 'Вычисли [[F_y]] со знаком (sin 60° ≈ 0,866; cos 60° = 0,5).'),
    [numSpec(L('[[F_y]]', '[[F_y]]'), fyAns, kn, true)],
    L('Svislá složka leží naproti úhlu a míří dolů: [[F_y]] = −12 · sin 60° ≈ −10,39 kN.', 'Вертикальная составляющая лежит напротив угла и направлена вниз: [[F_y]] = −12 · sin 60° ≈ −10,39 кН.'),
    [
      numTrap(L('Velikost sedí. Míří šipka nahoru (+), nebo dolů (−)?', 'Модуль верен. Стрелка направлена вверх (+) или вниз (−)?'), (v) => close(v[0], 10.39)),
      numTrap(L('Vypadá to na cos místo sin. Svislá složka leží **naproti** úhlu 60°.', 'Похоже на cos вместо sin. Вертикальная составляющая лежит **напротив** угла 60°.'), (v) => close(absOf(v[0]), 6)),
      numTrap(L('Složka nemůže být stejně velká jako celá síla 12 kN – je to jen její část.', 'Составляющая не может равняться всей силе 12 кН — это только её часть.'), (v) => close(absOf(v[0]), 12)),
    ],
    L('Složka naproti úhlu → sin; dolů je −.', 'Составляющая напротив угла → sin; вниз — это −.'),
    L('Vypočítej [[F_y]] včetně znaménka', 'Вычисли [[F_y]] с учётом знака'),
  );
  const s3 = useNumStep(
    L('Vypočítej moment síly k bodu A i se znaménkem (↺ kladný). P leží 2 m vpravo od A.', 'Вычисли момент силы относительно точки A со знаком (↺ положительный). P лежит на 2 м правее A.'),
    [numSpec(L('[[M_A]]', '[[M_A]]'), mAns, t.pick(L('kN·m', 'кН·м')), true)],
    L(
      'K bodu A otáčí jen [[F_y]]: [[M_A]] = 2 · [[F_y]] ≈ 2 · (−10,39) ≈ −20,78 kN·m, tedy po směru hodinových ručiček (↻). Nositelka [[F_x]] prochází bodem A, proto její moment je nulový.',
      'Вокруг A вращает только [[F_y]]: [[M_A]] = 2 · [[F_y]] ≈ 2 · (−10,39) ≈ −20,78 кН·м, то есть по часовой стрелке (↻). Линия действия [[F_x]] проходит через A, поэтому её момент равен нулю.',
    ),
    [
      numTrap(L('Celá síla krát vzdálenost platí jen pro kolmou sílu. Otáčí jen [[F_y]].', 'Вся сила на расстояние — только для перпендикулярной силы. Вращает лишь [[F_y]].'), (v) => close(absOf(v[0]), 24)),
      numTrap(L('Velikost sedí. Kterým směrem síla kolem A otáčí – ↺ (+), nebo ↻ (−)?', 'Модуль верен. В какую сторону сила вращает вокруг A — ↺ (+) или ↻ (−)?'), (v) => close(v[0], 20.78)),
      numTrap(L('Nositelka [[F_x]] prochází bodem A – rameno má 0. Otáčí jen [[F_y]].', 'Линия действия [[F_x]] проходит через A — плечо 0. Вращает только [[F_y]].'), (v) => close(absOf(v[0]), 12)),
    ],
    L('[[M_A]] = (vzdálenost 2 m) · [[F_y]]; [[F_x]] leží na přímce AP.', '[[M_A]] = (расстояние 2 м) · [[F_y]]; [[F_x]] лежит на прямой AP.'),
    L('Vypočítej moment síly vzhledem k bodu A', 'Вычисли момент силы относительно точки A'),
  );
  const steps = [s1, s2, s3];
  const cur = steps.findIndex((s) => !s.done);
  const doneN = cur < 0 ? 3 : cur;
  return (
    <>
      <Stage
        h={200}
        description={t.pick(L('Přímka AP, v bodě P šikmá síla 12 kN 2 m od A', 'Прямая AP, в точке P наклонная сила 12 кН в 2 м от A'))}
        draw={(b, c) => {
          const ax = 70;
          const ay = 140;
          const unit = 110;
          const px = ax + 2 * unit;
          signKey(b, c, 30, 60, true);
          b.line(ax, ay, px + 40, ay, c.inkSoft, 1.0, true);
          b.line(ax, ay, px, ay, c.ink, 2.0);
          b.circle(ax, ay, 5, c.ink);
          b.text(ax, ay + 22, 'A', c.ink, 14, 'middle', null, true);
          b.circle(px, ay, 4, c.ink);
          b.text(px - 10, ay + 22, 'P', c.ink, 14, 'middle', null, true);
          const len = 100;
          const ux = -Trig.cosD(60);
          const uy = Trig.sinD(60);
          const sx = px - ux * len;
          const sy = ay - uy * len;
          if (doneN >= 1) {
            b.force(px - ux * len, ay, px, ay, c.derived, 2.2);
            b.text(px + 18, ay + 20, ' = −6', c.derived, 12, 'start', 'F_x');
          }
          if (doneN >= 2) {
            b.force(px, sy, px, ay, c.derived, 2.2);
            b.text(px - 6, ay - 18, ' = −10,39', c.derived, 12, 'end', 'F_y');
          }
          if (doneN >= 3) {
            b.moment(ax, ay, 30, false, c.accent);
            b.text(ax + 8, ay - 40, t.pick(L(' ≈ −20,78 kN·m', ' ≈ −20,78 кН·м')), c.accent, 12, 'start', 'M_A');
          }
          b.force(sx, sy, px, ay, c.load, 3.0);
          b.text(px - 10, sy + 30, ` = 12 ${kn}`, c.load, 14, 'end', 'F');
          b.angleArc(px, ay, 30, 0, -60, '60°');
          b.dimLine(ax, ay + 40, px, ay + 40, t.pick(L('2 m', '2 м')));
        }}
      />
      <CardText
        text={L(
          'Síla 12 kN působí v bodě P, 2 m vpravo od A, pod úhlem 60° k vodorovné a míří vlevo dolů. Kladné směry: +x doprava, +y nahoru, moment ↺.',
          'Сила 12 кН приложена в точке P, на 2 м правее A, под углом 60° к горизонтали и направлена влево вниз. Положительные направления: +x вправо, +y вверх, момент ↺.',
        )}
      />
      <Steps
        steps={steps}
        after={() => <ActionButton text={L('Procvičování →', 'Практика →')} onClick={() => learn.open({ kind: 'practice', returnTo: learn.here ?? undefined })} />}
      />
    </>
  );
}

export const cards = [FromVertical, Resultant, SumForces, MomentArm, InclinedBeam, FinalTask];
