/*
 * Kurz trigonometrie – lekce 4 (id 104): úhly 30°, 45°, 60° zpaměti – půlka čtverce, půlka rovnostranného
 * trojúhelníku a tabulka √0/2 … √4/2. (Přepis z aplikace: learn/trig/TrigLesson4.kt.)
 */
import { CardText, L, Note, SmallText, Stage, Steps, numSpec, opt, pt, screenAngle, step, trapNear, tween, useKit, useLaunched, useNumStep, useQuizStep, useTr, type Opt } from '../../kit';
import { Trig } from '../../../practice/trig';

/** Ukázka „rozpůlení“: po 700 ms animace 0 → 1, pak příznak cut. */
function useCutDemo() {
  const kit = useKit();
  const anim = kit.anim(0);
  const cut = kit.mut(false);
  useLaunched([], async (job) => {
    await job.delay(700);
    await job.run(anim.animateTo(1, tween(900)));
    await job.delay(300);
    cut.v = true;
  });
  return { anim, cut: cut.v };
}

/** Karta 0: rozpůlený čtverec → trojúhelník 45°–45°–90°. */
function HalfSquare() {
  const t = useTr();
  const { anim, cut } = useCutDemo();
  const tgQ = useQuizStep(
    L('Kolik je tg 45°?', 'Чему равен tg 45°?'),
    [
      opt(L('1', '1'), true),
      opt(L('0,707', '0,707'), false, L('0,707 je odvěsna dělená přeponou, tedy sin 45°. Tangens dělí odvěsnu odvěsnou.', '0,707 — катет, делённый на гипотенузу, то есть sin 45°. Тангенс делит катет на катет.')),
      opt(L('0,5', '0,5'), false, L('Obě odvěsny měří 1. Kolik je 1 / 1?', 'Оба катета равны 1. Сколько будет 1 / 1?')),
      opt(L('√2', '√2'), false, L('√2 je délka přepony. V tangentu přepona není.', '√2 — длина гипотенузы. В тангенсе гипотенузы нет.')),
    ],
    L(
      'Obě odvěsny jsou stejné, takže tg 45° = 1 / 1 = 1. Přeponu – úhlopříčku d – dá Pythagorova věta (lekce 5): d² = 1² + 1² = 2, tedy d = √2 ≈ 1,41.',
      'Катеты равны, поэтому tg 45° = 1 / 1 = 1. Гипотенузу — диагональ d — даёт теорема Пифагора (урок 5): d² = 1² + 1² = 2, значит d = √2 ≈ 1,41.',
    ),
    null,
    4,
  );
  const sinQ = useQuizStep(
    L('Přepona měří √2. Jaký podíl je sin 45°?', 'Гипотенуза равна √2. Какое отношение — sin 45°?'),
    [
      opt(L('1/√2', '1/√2'), true),
      opt(L('√2/1', '√2/1'), false, L('Obrácený zlomek: přepona patří dolů. Sinus nikdy nepřekročí 1, a √2 ≈ 1,41.', 'Перевёрнутая дробь: гипотенуза стоит снизу. Синус никогда не больше 1, а √2 ≈ 1,41.')),
      opt(L('1/2', '1/2'), false, L('1/2 by vyšlo, kdyby přepona měřila 2. Tady měří √2.', '1/2 получилось бы, если бы гипотенуза была 2. Здесь она √2.')),
    ],
    L('sin 45° = cos 45° = 1/√2 = √2/2 ≈ 0,707.', 'sin 45° = cos 45° = 1/√2 = √2/2 ≈ 0,707.'),
    null,
    3,
  );
  const x0 = 130;
  const y0 = 40;
  const s = 150;
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Čtverec o straně 1 a jeho úhlopříčka', 'Квадрат со стороной 1 и его диагональ'))}
        draw={(b, c) => {
          const p = anim.value;
          b.poly([[x0, y0], [x0 + s, y0], [x0, y0 + s]], c.subtle, null, 0, 1 - 0.7 * p);
          b.line(x0, y0, x0 + s, y0, c.ink, 2.0, false, 1 - 0.7 * p);
          b.line(x0, y0, x0, y0 + s, c.ink, 2.0, false, 1 - 0.7 * p);
          b.poly([[x0 + s, y0], [x0 + s, y0 + s], [x0, y0 + s]], c.subtle, null, 0, 0.8);
          b.line(x0, y0 + s, x0 + s, y0 + s, c.derived, 2.6);
          b.line(x0 + s, y0 + s, x0 + s, y0, c.derived, 2.6);
          if (p > 0) b.line(x0, y0 + s, x0 + s * p, y0 + s - s * p, c.load, 2.8);
          b.text(x0 + s / 2, y0 + s + 22, '1', c.derived, 15, 'middle', null, true);
          b.text(x0 + s + 14, y0 + s / 2 + 5, '1', c.derived, 15, 'start', null, true);
          if (p >= 1) {
            b.rightAngleMark(pt(x0 + s, y0 + s), pt(x0, y0 + s), pt(x0 + s, y0));
            b.angleArc(x0, y0 + s, 30, 0, -45, '45°');
            b.angleArc(x0 + s, y0, 30, 90, 135, '45°');
            b.text(x0 + s / 2 - 16, y0 + s / 2 - 8, tgQ.done ? 'd = √2 ≈ 1,41' : 'd', c.load, 14, 'end', null, true);
          }
        }}
      />
      <CardText text={L('Úhlopříčka rozdělí čtverec na dva shodné pravoúhlé trojúhelníky. Strana čtverce je 1.', 'Диагональ делит квадрат на два равных прямоугольных треугольника. Сторона квадрата равна 1.')} />
      <Steps
        steps={[
          step(
            L('Sleduj, jak úhlopříčka rozdělí čtverec', 'Проследи, как диагональ делит квадрат'),
            cut,
            () => <SmallText text={L('Úhlopříčka půlí čtverec i jeho pravé úhly…', 'Диагональ делит пополам квадрат и его прямые углы…')} />,
            () => <Note tone="info" text={L('Vznikl trojúhelník 45°–45°–90° se dvěma odvěsnami délky 1.', 'Получился треугольник 45°–45°–90° с двумя катетами длины 1.')} />,
          ),
          tgQ,
          sinQ,
        ]}
      />
    </>
  );
}

/** Karta 1: rozpůlený rovnostranný trojúhelník → 30°–60°–90° se stranami 1, √3, 2. */
function HalfTriangle() {
  const t = useTr();
  const { anim, cut } = useCutDemo();
  const values = ['1/2', '√3/2', '1/√3'];
  const sinQ = useQuizStep(
    L('Kolik je sin 30°?', 'Чему равен sin 30°?'),
    [
      opt(L(values[0], values[0]), true),
      opt(L(values[1], values[1]), false, L('√3 je odvěsna, která z vrcholu 30° vychází – přilehlá. To by byl cos 30°.', '√3 — катет, выходящий из вершины угла 30°, прилежащий. Это был бы cos 30°.')),
      opt(L(values[2], values[2]), false, L('Odvěsna dělená odvěsnou je tangens. Sinus dělí přeponou 2.', 'Катет, делённый на катет, — это тангенс. Синус делит на гипотенузу 2.')),
    ],
    L('Naproti 30° leží odvěsna 1, přepona je 2: sin 30° = 1/2.', 'Напротив 30° лежит катет 1, гипотенуза равна 2: sin 30° = 1/2.'),
    null,
    3,
  );
  const cosQ = useQuizStep(
    L('Ve stejném trojúhelníku: kolik je cos 60°?', 'В том же треугольнике: чему равен cos 60°?'),
    [
      opt(L(values[0], values[0]), true),
      opt(L(values[1], values[1]), false, L('√3 leží naproti 60° – √3/2 je sin 60°. Kosinus bere odvěsnu, která z vrcholu 60° vychází.', '√3 лежит напротив 60° — √3/2 это sin 60°. Косинус берёт катет, выходящий из вершины угла 60°.')),
      opt(L(values[2], values[2]), false, L('Kosinus dělí přeponou (2), ne odvěsnou.', 'Косинус делит на гипотенузу (2), а не на катет.')),
    ],
    L(
      'Strana délky 1 leží naproti 30° a zároveň u úhlu 60°. Proto sin 30° = cos 60° = 1/2. Druhá odvěsna √3 dává sin 60° = cos 30° = √3/2 ≈ 0,866.',
      'Сторона длины 1 лежит напротив 30° и одновременно прилежит к 60°. Поэтому sin 30° = cos 60° = 1/2. Другой катет √3 даёт sin 60° = cos 30° = √3/2 ≈ 0,866.',
    ),
    null,
    3,
  );
  const focus = !cut ? null : !sinQ.done ? 30 : 60;
  const l0 = pt(110, 205);
  const r0 = pt(290, 205);
  const top = pt(200, 205 - 180 * Trig.sinD(60));
  const foot = pt(200, 205);
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Rovnostranný trojúhelník o straně 2 a jeho výška', 'Равносторонний треугольник со стороной 2 и его высота'))}
        draw={(b, c) => {
          const pr = anim.value;
          b.poly([[top.x, top.y], [r0.x, r0.y], [foot.x, foot.y]], c.subtle, null, 0, 0.8 - 0.6 * pr);
          b.poly([[top.x, top.y], [l0.x, l0.y], [foot.x, foot.y]], c.subtle, null, 0, 0.8);
          b.line(top.x, top.y, r0.x, r0.y, c.ink, 2.0, false, 1 - 0.6 * pr);
          b.line(foot.x, foot.y, r0.x, r0.y, c.ink, 2.0, false, 1 - 0.6 * pr);
          b.line(l0.x, l0.y, top.x, top.y, cut ? c.load : c.ink, 2.6);
          b.line(l0.x, l0.y, foot.x, foot.y, cut ? c.derived : c.ink, 2.6);
          b.text(l0.x - 4 + (top.x - l0.x) / 2 - 12, (l0.y + top.y) / 2 - 4, '2', cut ? c.load : c.ink, 15, 'end', null, true);
          if (!cut) {
            b.text((top.x + r0.x) / 2 + 14, (top.y + r0.y) / 2, '2', c.ink, 15, 'start', null, true);
            b.text((l0.x + r0.x) / 2 + 45, l0.y + 22, '2', c.ink, 15, 'middle', null, true);
            b.angleArc(l0.x, l0.y, 30, 0, -60, '60°');
            b.angleArc(r0.x, r0.y, 30, 180, 240, '60°');
          }
          if (pr > 0) b.line(top.x, top.y, top.x, top.y + (foot.y - top.y) * pr, c.derived, 2.6);
          if (cut) {
            b.rightAngleMark(foot, l0, top);
            b.text((l0.x + foot.x) / 2, l0.y + 22, '1', c.derived, 15, 'middle', null, true);
            b.text(foot.x + 10, (foot.y + top.y) / 2 + 5, '√3 ≈ 1,73', c.derived, 14, 'start', null, true);
            b.angleArc(top.x, top.y, 30, 90, screenAngle(top, l0), '30°', focus === 30 ? c.highlight : c.inkSoft);
            b.angleArc(l0.x, l0.y, 22, 0, screenAngle(l0, top), '60°', focus === 60 ? c.highlight : c.inkSoft);
          }
        }}
      />
      <CardText
        text={L(
          'Rovnostranný trojúhelník o straně 2 má tři úhly po 60°. Jeho výška rozdělí základnu i horní úhel na poloviny.',
          'В равностороннем треугольнике со стороной 2 каждый угол равен 60°. Высота делит основание и угол при вершине пополам.',
        )}
      />
      <Steps
        steps={[
          step(
            L('Sleduj, jak výška rozdělí trojúhelník', 'Проследи, как высота делит треугольник'),
            cut,
            () => <SmallText text={L('Výška půlí základnu 2 na 1 a 1…', 'Высота делит основание 2 на 1 и 1…')} />,
            () => (
              <Note
                tone="info"
                text={L(
                  'Vznikl trojúhelník 30°–60°–90°: přepona 2, kratší odvěsna 1 (půlka základny), delší √(2² − 1²) = √3 ≈ 1,73.',
                  'Получился треугольник 30°–60°–90°: гипотенуза 2, короткий катет 1 (половина основания), длинный √(2² − 1²) = √3 ≈ 1,73.',
                )}
              />
            ),
          ),
          sinQ,
          cosQ,
        ]}
      />
    </>
  );
}

const TABLE_ANGLES = [0, 30, 45, 60, 90];
const TABLE_EXACT = ['0', '1/2', '√2/2', '√3/2', '1'];
const TABLE_DEC = ['0', '0,5', '≈ 0,707', '≈ 0,866', '1'];

/** Karta 2: tabulka sin pro 0°…90° po jednom políčku, pak řádek cos jako tytéž hodnoty pozpátku. */
function SqrtTable() {
  const t = useTr();
  const kit = useKit();
  const anim = kit.anim(0);
  const hint = L(
    'Pomůcka: sin 0°, 30°, 45°, 60°, 90° = √0/2, √1/2, √2/2, √3/2, √4/2 – pod odmocninou počítáš 0, 1, 2, 3, 4.',
    'Подсказка: sin 0°, 30°, 45°, 60°, 90° = √0/2, √1/2, √2/2, √3/2, √4/2 — под корнем считаешь 0, 1, 2, 3, 4.',
  );
  const o = (i: number, why: L | null = null): Opt => {
    const s = TABLE_EXACT[i] + (i >= 2 && i <= 3 ? ' ≈ ' + TABLE_DEC[i].replace(/^≈ /, '') : '');
    return opt(L(s, s), why === null, why);
  };
  const q0 = useQuizStep(
    L('Doplň sin 0°', 'Заполни sin 0°'),
    [
      o(0),
      o(1, L('1/2 je sin 30°. Při 0° leží úsečka vodorovně – její svislý průmět je nulový.', '1/2 — это sin 30°. При 0° отрезок лежит горизонтально — его вертикальная проекция нулевая.')),
      o(4, L('1 je sin 90°, úsečka svisle. Při 0° leží vodorovně.', '1 — это sin 90°, отрезок вертикален. При 0° он лежит горизонтально.')),
    ],
    L('sin 0° = 0: protilehlá odvěsna se smrskla na nulu.', 'sin 0° = 0: противолежащий катет сжался до нуля.'),
    hint,
    3,
  );
  const q1 = useQuizStep(
    L('Doplň sin 30°', 'Заполни sin 30°'),
    [
      o(0, L('0 je sin 0°. Vzpomeň si na půlku rovnostranného trojúhelníku.', '0 — это sin 0°. Вспомни половину равностороннего треугольника.')),
      o(1),
      o(3, L('√3/2 je sin 60°. Naproti 30° leží kratší odvěsna – polovina přepony.', '√3/2 — это sin 60°. Напротив 30° лежит короткий катет — половина гипотенузы.')),
    ],
    L('sin 30° = 1/2: naproti 30° leží polovina přepony.', 'sin 30° = 1/2: напротив 30° лежит половина гипотенузы.'),
    hint,
    3,
  );
  const q2 = useQuizStep(
    L('Doplň sin 45°', 'Заполни sin 45°'),
    [
      o(1, L('1/2 je sin 30°. U 45° jsou obě odvěsny 1 a přepona √2.', '1/2 — это sin 30°. При 45° оба катета равны 1, гипотенуза √2.')),
      o(2),
      o(3, L('√3/2 je sin 60°. U 45° jsou obě odvěsny 1 a přepona √2.', '√3/2 — это sin 60°. При 45° оба катета равны 1, гипотенуза √2.')),
    ],
    L('sin 45° = 1/√2 = √2/2 ≈ 0,707 – půlka čtverce.', 'sin 45° = 1/√2 = √2/2 ≈ 0,707 — половина квадрата.'),
    hint,
    1,
  );
  const q3 = useQuizStep(
    L('Doplň sin 60°', 'Заполни sin 60°'),
    [
      o(1, L('1/2 je sin 30°. Naproti 60° leží delší odvěsna √3, přepona je 2.', '1/2 — это sin 30°. Напротив 60° лежит длинный катет √3, гипотенуза 2.')),
      o(2, L('√2/2 patří k 45°, kde jsou odvěsny stejné.', '√2/2 относится к 45°, где катеты равны.')),
      o(3),
    ],
    L('sin 60° = √3/2 ≈ 0,866: naproti 60° leží odvěsna √3, přepona je 2.', 'sin 60° = √3/2 ≈ 0,866: напротив 60° лежит катет √3, гипотенуза 2.'),
    hint,
    1,
  );
  const q4 = useQuizStep(
    L('Doplň sin 90°', 'Заполни sin 90°'),
    [
      o(0, L('0 je sin 0°. Při 90° stojí úsečka svisle – svislý průmět je celá její délka.', '0 — это sin 0°. При 90° отрезок стоит вертикально — вертикальная проекция равна всей длине.')),
      o(1, L('1/2 je sin 30°. Při 90° stojí úsečka svisle.', '1/2 — это sin 30°. При 90° отрезок стоит вертикально.')),
      o(4),
    ],
    L('sin 90° = 1. Vidíš vzor? Řádek sin je √0/2, √1/2, √2/2, √3/2, √4/2.', 'sin 90° = 1. Видишь закономерность? Строка sin — это √0/2, √1/2, √2/2, √3/2, √4/2.'),
    hint,
    3,
  );
  const q5 = useQuizStep(
    L('Jak jsou seřazené tytéž hodnoty v řádku cos?', 'Как расположены те же значения в строке cos?'),
    [
      opt(L('Stejně jako u sin', 'Так же, как у sin'), false, L('Pak by cos 0° = 0. Při 0° je ale vodorovný průmět celá úsečka.', 'Тогда cos 0° = 0. Но при 0° горизонтальная проекция — весь отрезок.')),
      opt(L('Obráceně', 'В обратном порядке'), true),
      opt(L('Všude 1', 'Везде 1'), false, L('cos 90° = 0: svislá úsečka nemá vodorovný průmět.', 'cos 90° = 0: у вертикального отрезка нет горизонтальной проекции.')),
    ],
    L(
      'Kosinus má stejné hodnoty v opačném pořadí, protože cos α = sin (90° − α): přilehlá odvěsna k α je protilehlá k druhému ostrému úhlu.',
      'У косинуса те же значения в обратном порядке, поскольку cos α = sin (90° − α): катет, прилежащий к α, противолежит другому острому углу.',
    ),
    null,
    3,
  );
  const qs = [q0, q1, q2, q3, q4];
  const fi = qs.findIndex((q) => !q.done);
  const filled = fi < 0 ? 5 : fi;
  useLaunched([q5.done], async (job) => {
    if (q5.done) await job.run(anim.animateTo(1, tween(900)));
  });
  const x0 = 70;
  const cw = 64;
  const rowH = 62;
  const yHead = 20;
  const ySin = 52;
  const yCos = ySin + rowH;
  return (
    <>
      <Stage
        h={190}
        description={t.pick(L('Tabulka hodnot sin a cos pro 0°, 30°, 45°, 60°, 90°', 'Таблица значений sin и cos для 0°, 30°, 45°, 60°, 90°'))}
        draw={(b, c) => {
          TABLE_ANGLES.forEach((a, i) => b.text(x0 + cw * i + cw / 2, yHead + 18, `${a}°`, c.inkSoft, 14, 'middle', null, true));
          b.text(x0 - 10, ySin + rowH / 2 + 5, 'sin', c.ink, 15, 'end', null, true);
          b.text(x0 - 10, yCos + rowH / 2 + 5, 'cos', c.ink, 15, 'end', null, true);
          for (let i = 0; i < 5; i++) {
            b.rect(x0 + cw * i, ySin, cw, rowH, i < filled ? c.subtle : c.paper, i === filled ? c.highlight : c.line, i === filled ? 2.0 : 1.0);
            const cosActive = filled === 5 && !q5.done;
            b.rect(x0 + cw * i, yCos, cw, rowH, c.paper, cosActive ? c.highlight : c.line, cosActive ? 2.0 : 1.0);
            if (i < filled) {
              b.text(x0 + cw * i + cw / 2, ySin + 28, TABLE_EXACT[i], c.derived, 15, 'middle', null, true);
              if (i >= 2 && i <= 3) b.text(x0 + cw * i + cw / 2, ySin + 48, TABLE_DEC[i], c.inkSoft, 11);
            } else if (i === filled) {
              b.text(x0 + cw * i + cw / 2, ySin + rowH / 2 + 6, '?', c.highlight, 18, 'middle', null, true);
            }
            const j = 4 - i;
            if (anim.value * 5 > i) {
              b.text(x0 + cw * i + cw / 2, yCos + 28, TABLE_EXACT[j], c.derived, 15, 'middle', null, true);
              if (j >= 2 && j <= 3) b.text(x0 + cw * i + cw / 2, yCos + 48, TABLE_DEC[j], c.inkSoft, 11);
            }
          }
        }}
      />
      <CardText
        text={L(
          'Hodnoty pro 0°, 30°, 45°, 60° a 90° tvoří užitečnou tabulku. Půlka čtverce a půlka rovnostranného trojúhelníku ti ji pomůžou znovu sestavit.',
          'Значения для 0°, 30°, 45°, 60° и 90° образуют полезную таблицу. Половина квадрата и половина равностороннего треугольника помогут её восстановить.',
        )}
      />
      <Steps
        steps={[q0, q1, q2, q3, q4, q5]}
        after={() =>
          q5.done && (
            <SmallText
              text={L(
                'Pro zajímavost: tg = sin / cos, takže tg 30° = 1/√3 ≈ 0,577, tg 45° = 1, tg 60° = √3 ≈ 1,732 (tg 90° není definován).',
                'Для справки: tg = sin / cos, поэтому tg 30° = 1/√3 ≈ 0,577, tg 45° = 1, tg 60° = √3 ≈ 1,732 (tg 90° не определён).',
              )}
            />
          )
        }
      />
    </>
  );
}

/** Karta 3: kvíz – cos 60° zpaměti. */
function CheckCos60() {
  const q = useQuizStep(
    L('Kolik je cos 60°? Zpaměti, bez tabulky.', 'Чему равен cos 60°? Наизусть, без таблицы.'),
    [
      opt(L('0,5', '0,5'), true),
      opt(L('0,866', '0,866'), false, L('0,866 = √3/2 je sin 60°. Řádek cos má tytéž hodnoty pozpátku – co je v něm pod 60°?', '0,866 = √3/2 — это sin 60°. В строке cos те же значения в обратном порядке — что в ней под 60°?')),
      opt(L('0,707', '0,707'), false, L('0,707 patří k 45°, kde jsou sin a cos stejné.', '0,707 относится к 45°, где sin и cos равны.')),
      opt(L('1', '1'), false, L('Mezi 0° a 90° má kosinus hodnotu 1 jen při 0°. U 60° je přilehlá odvěsna mnohem kratší než přepona.', 'Между 0° и 90° косинус равен 1 только при 0°. При 60° прилежащий катет намного короче гипотенузы.')),
    ],
    L('cos 60° = 1/2. V trojúhelníku 30°–60°–90° je odvěsna u úhlu 60° poloviční než přepona.', 'cos 60° = 1/2. В треугольнике 30°–60°–90° прилежащий к 60° катет вдвое короче гипотенузы.'),
    null,
    2,
  );
  return (
    <>
      <CardText text={L('Známé hodnoty umožňují rychlou kontrolu výpočtu.', 'Знакомые значения помогают быстро проверить вычисление.')} />
      <Steps steps={[q]} />
    </>
  );
}

/** Karta 4: velikosti složek síly bez kalkulačky (20 kN, 30° od vodorovné). */
function NoCalc() {
  const t = useTr();
  const fy = useNumStep(
    L('Kolik je |[[F_y]]|, velikost svislé složky? Počítej zpaměti.', 'Чему равен |[[F_y]]|, модуль вертикальной составляющей? Считай устно.'),
    [numSpec(L('|[[F_y]]|', '|[[F_y]]|'), 10, 'kN')],
    L('Svislá složka leží naproti úhlu 30°: |[[F_y]]| = 20 · sin 30° = 20 · 1/2 = 10 kN.', 'Вертикальная составляющая лежит напротив угла 30°: |[[F_y]]| = 20 · sin 30° = 20 · 1/2 = 10 кН.'),
    [
      trapNear(L('≈ 17,32 je 20 · cos 30° – složka **u** úhlu 30°, tedy vodorovná. Svislá leží naproti.', '≈ 17,32 — это 20 · cos 30°, составляющая **у** угла 30°, то есть горизонтальная. Вертикальная лежит напротив.'), 17.32),
      trapNear(L('To je celá síla. Šikmá síla má obě složky menší než sama je.', 'Это вся сила. У наклонной силы обе составляющие меньше её самой.'), 20),
    ],
    L('Která funkce patří k odvěsně naproti úhlu 30°? A kolik je sin 30°?', 'Какая функция относится к катету напротив угла 30°? И чему равен sin 30°?'),
  );
  const fx = useQuizStep(
    L('Jaká je přesná velikost vodorovné složky |[[F_x]]|?', 'Каков точный модуль горизонтальной составляющей |[[F_x]]|?'),
    [
      opt(L('10√3 kN', '10√3 кН'), true),
      opt(L('10 kN', '10 кН'), false, L('10 kN je svislá složka, naproti 30°. Vodorovná leží u úhlu – patří k ní kosinus.', '10 кН — вертикальная составляющая, напротив 30°. Горизонтальная лежит у угла — к ней относится косинус.')),
      opt(L('20√3 kN', '20√3 кН'), false, L('20√3 ≈ 34,6 kN je víc než celá síla 20 kN. Nezapomeň na /2 v cos 30° = √3/2.', '20√3 ≈ 34,6 кН больше всей силы 20 кН. Не забудь /2 в cos 30° = √3/2.')),
    ],
    L(
      '|[[F_x]]| = 20 · cos 30° = 20 · √3/2 = 10√3 kN ≈ 17,32 kN. Obě složky šikmé síly jsou menší než ona sama; směr složek ukazují šipky.',
      '|[[F_x]]| = 20 · cos 30° = 20 · √3/2 = 10√3 кН ≈ 17,32 кН. Обе составляющие наклонной силы меньше её самой; направления показаны стрелками.',
    ),
    null,
    3,
  );
  const tail = pt(90, 50);
  const len = 220;
  const tip = pt(tail.x + len * Trig.cosD(30), tail.y + len * Trig.sinD(30));
  const corner = pt(tip.x, tail.y);
  return (
    <>
      <Stage
        h={220}
        description={t.pick(L('Síla 20 kN skloněná o 30° pod vodorovnou a její dvě složky', 'Сила 20 кН под углом 30° ниже горизонтали и её две составляющие'))}
        draw={(b, c) => {
          b.line(tail.x - 30, tail.y, corner.x + 40, corner.y, c.inkSoft, 1.0, true);
          if (fx.done) b.force(tail.x, tail.y, corner.x, corner.y, c.derived, 2.4);
          else b.line(tail.x, tail.y, corner.x, corner.y, c.derived, 1.4, true);
          if (fy.done) b.force(corner.x, corner.y, tip.x, tip.y, c.derived, 2.4);
          else b.line(corner.x, corner.y, tip.x, tip.y, c.derived, 1.4, true);
          b.rightAngleMark(corner, tail, tip);
          b.force(tail.x, tail.y, tip.x, tip.y, c.load, 3.0);
          b.text((tail.x + tip.x) / 2 - 6, (tail.y + tip.y) / 2 + 22, ' = 20 kN', c.load, 14, 'end', 'F');
          b.angleArc(tail.x, tail.y, 46, 0, screenAngle(tail, tip), '30°');
          b.text((tail.x + corner.x) / 2, tail.y - 12, fx.done ? ' = 10√3 ≈ 17,32 kN' : ' = ?', c.derived, 13, 'middle', 'F_x');
          b.text(corner.x + 10, (corner.y + tip.y) / 2 + 5, fy.done ? ' = 10 kN' : ' = ?', c.derived, 13, 'start', 'F_y');
        }}
      />
      <CardText
        text={L(
          'Síla 20 kN svírá s vodorovnou úhel 30°. Nahradíme ji vodorovnou složkou [[F_x]] a svislou [[F_y]]. Zatím určujeme jen jejich velikosti.',
          'Сила 20 кН составляет с горизонталью угол 30°. Заменим её горизонтальной составляющей [[F_x]] и вертикальной [[F_y]]. Пока определяем только их модули.',
        )}
      />
      <Steps steps={[fy, fx]} />
    </>
  );
}

export const cards = [HalfSquare, HalfTriangle, SqrtTable, CheckCos60, NoCalc];
