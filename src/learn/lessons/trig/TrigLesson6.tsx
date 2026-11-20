/*
 * Kurz trigonometrie – lekce 6 (id 106): od stran zpátky k úhlu – arcsin, arccos, arctg, sklon poměrem a v procentech.
 * (Přepis z aplikace: learn/trig/TrigLesson6.kt.)
 */
import {
  ActionButton,
  CardText,
  L,
  Note,
  SmallText,
  Stage,
  Steps,
  ValueSlider,
  clamp,
  dist,
  num,
  numSpec,
  opt,
  pt,
  step,
  trapNear,
  triGeom,
  triSide,
  useKit,
  useLaunched,
  useNumStep,
  useQuizStep,
  useTr,
  type Colors,
  type PrimBuilder,
  type TriSide,
} from '../../kit';
import { Trig } from '../../../practice/trig';
import { useGuessStep } from './shared';

/** Karta 0: znáš strany, hledáš úhel – tg α = výška / základna, α = arctg. */
function BackToAngle() {
  const t = useTr();
  const kit = useKit();
  const guess = kit.mut<number | null>(null);
  const phase = kit.mut(0); // ukázka: 1 poměr, 2 arctg, 3 úhel
  const demoDone = kit.mut(false);
  const h = kit.mut(2);
  const at45 = kit.mut(false);
  const tried = kit.mut<number | null>(null);
  const dragging = kit.mut(false);
  useLaunched([guess.v !== null], async (job) => {
    if (guess.v === null) return;
    for (let p = 1; p <= 3; p++) {
      await job.delay(900);
      phase.v = p;
    }
    await job.delay(600);
    demoDone.v = true;
  });
  const tips = [L('méně než 45°', 'меньше 45°'), L('45°', '45°'), L('víc než 45°', 'больше 45°')];
  const s1 = useGuessStep(L('Odhadni úhel α při výšce 3 a základně 4', 'Предскажи угол α при высоте 3 и основании 4'), tips, guess.v, (i) => (guess.v = i));
  const s2 = step(
    L('Sleduj, jak se z poměru 3/4 stane úhel', 'Проследи, как отношение 3/4 превращается в угол'),
    demoDone.v,
    () => <SmallText text={L('Sleduj obrázek: poměr → úhel…', 'Смотри на рисунок: отношение → угол…')} />,
    () => (
      <Note
        tone={guess.v === 0 ? 'ok' : 'info'}
        text={L(
          (guess.v === 0 ? 'Tip sedí. ' : 'Výška 3 je menší než základna 4, proto je úhel menší než 45°. ') +
            'α = arctg 0,75 ≈ 36,9°. Na kalkulačce (režim DEG) je arctg pod tlačítkem **tan⁻¹** (SHIFT nebo 2nd + tan).',
          (guess.v === 0 ? 'Прогноз верен. ' : 'Высота 3 меньше основания 4, поэтому угол меньше 45°. ') +
            'α = arctg 0,75 ≈ 36,9°. На калькуляторе (режим DEG) arctg — это кнопка **tan⁻¹** (SHIFT или 2nd + tan).',
        )}
      />
    ),
  );
  const s3 = step(
    L('Najdi výšku, při které je úhel 45° (základna 4)', 'Найди высоту, при которой угол 45° (основание 4)'),
    at45.v,
    () => {
      const v = tried.v;
      return (
        <>
          <ValueSlider
            label={t.pick(L(`Výška = ${h.v}`, `Высота = ${h.v}`))}
            value={h.v}
            min={0}
            max={6}
            steps={5}
            onChange={(x) => {
              h.v = Math.round(x);
              tried.v = null;
            }}
          />
          <ActionButton text={L('Zkontrolovat', 'Проверить')} onClick={() => (h.v === 4 ? (at45.v = true) : (tried.v = h.v))} />
          {v !== null && (
            <Note
              tone="nudge"
              text={L(
                `Při výšce ${v} je α = arctg (${v}/4) ≈ ${num(Trig.atanD(v / 4), 1)}°. Při jakém poměru stran je úhel přesně 45°?`,
                `При высоте ${v} угол α = arctg (${v}/4) ≈ ${num(Trig.atanD(v / 4), 1)}°. При каком отношении сторон угол ровно 45°?`,
              )}
            />
          )}
        </>
      );
    },
    () => <Note tone="ok" text={L('Výška = základna, poměr je 1 a arctg 1 = 45°.', 'Высота = основание, отношение равно 1, а arctg 1 = 45°.')} />,
  );
  const steps = [s1, s2, s3];
  const cur = steps.findIndex((s) => !s.done);
  const free = cur === 2 || cur < 0;
  const hh = free ? h.v : 3;
  const st = 30;
  const ox = 80;
  const oy = 210;
  const base = 4;
  const alpha = Trig.atanD(hh / base);
  const tipX = ox + base * st;
  const tipY = oy - hh * st;
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Trojúhelník na mřížce s nastavitelnou výškou', 'Треугольник на сетке с изменяемой высотой'))}
        onDrag={(p, start) => {
          if (start) dragging.v = free && !at45.v && (dist(p, tipX, tipY) < 45 || Math.abs(p.x - tipX) < 30);
          if (dragging.v) {
            h.v = clamp(Math.round((oy - p.y) / st), 0, 6);
            tried.v = null;
          }
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b, c) => {
          b.grid(ox, oy - 6 * st, st, base + 1, 6);
          const showAngle = (cur === 1 && phase.v >= 3) || cur < 0;
          if (hh > 0) {
            b.rightTriangle({ a: pt(ox, oy), b: pt(tipX, tipY), c: pt(tipX, oy) }, { adj: `${base}`, opp: `${hh}` }, null, null);
            b.angleArc(ox, oy, 34, 0, -alpha, showAngle ? `${num(alpha, 1)}°` : '?');
          } else b.line(ox, oy, tipX, oy, c.derived, 2.4);
          if (free && !at45.v) b.handle(tipX, tipY, dragging.v);
          const lines: string[] =
            cur === 0
              ? ['tg α = 3 / 4', 'α = ?']
              : cur === 1
                ? ['tg α = 3 / 4' + (phase.v >= 1 ? ' = 0,75' : ''), phase.v >= 2 ? 'α = arctg 0,75' : null, phase.v >= 3 ? 'α ≈ 36,9°' : null, phase.v >= 2 ? t.pick(L('kalkulačka: tan⁻¹ 0,75', 'калькулятор: tan⁻¹ 0,75')) : null].filter((x): x is string => x !== null)
                : cur === 2
                  ? [`tg α = ${hh} / ${base}`, 'α = 45° ?']
                  : ['tg α = 4 / 4 = 1', 'α = arctg 1 = 45°'];
          lines.forEach((s, i) => {
            const calc = s.includes('tan⁻¹');
            b.text(392, 40 + 24 * i, s, s.startsWith('α ≈') || s.startsWith('α = arctg 1') ? c.load : calc ? c.inkSoft : c.ink, calc ? 13 : 14, 'end', null, s.startsWith('α'));
          });
        }}
      />
      <CardText
        text={L(
          'Známe-li obě odvěsny, známe tg α. Funkce **arctg** z tohoto poměru určí úhel: poměr dovnitř, úhel ven.',
          'Если известны оба катета, известен и tg α. Функция **arctg** находит по этому отношению угол: отношение на входе, угол на выходе.',
        )}
      />
      <Steps steps={steps} />
    </>
  );
}

/** Karta 1: kterou obrácenou funkci – podle dvou známých stran. Každé kolo zvýrazní jinou dvojici. */
function WhichInverse() {
  const t = useTr();
  const fns = (right: number, why: (L | null)[]) => ['arcsin', 'arccos', 'arctg'].map((f, i) => opt(L(f, f), i === right, why[i]));
  const s1 = useQuizStep(
    L('Znáš protilehlou odvěsnu a přeponu. Která funkce dá úhel α?', 'Известны противолежащий катет и гипотенуза. Какая функция даёт угол α?'),
    fns(0, [
      null,
      L('Kosinus potřebuje přilehlou odvěsnu – tu neznáš.', 'Косинусу нужен прилежащий катет — он неизвестен.'),
      L('Tangens dělí odvěsnu odvěsnou – jedna ze známých stran je ale přepona.', 'Тангенс делит катет на катет, а одна из известных сторон — гипотенуза.'),
    ]),
    L('protilehlá / přepona = sin α, proto **α = arcsin (protilehlá / přepona)**. Na kalkulačce sin⁻¹.', 'противолежащий / гипотенуза = sin α, поэтому **α = arcsin (противолежащий / гипотенуза)**. На калькуляторе sin⁻¹.'),
    null,
    3,
    L('Vyber funkci pro protilehlou odvěsnu a přeponu', 'Выбери функцию для противолежащего катета и гипотенузы'),
  );
  const s2 = useQuizStep(
    L('Znáš přilehlou odvěsnu a přeponu. Která funkce dá úhel α?', 'Известны прилежащий катет и гипотенуза. Какая функция даёт угол α?'),
    fns(1, [
      L('Sinus potřebuje odvěsnu **naproti** úhlu. Známá odvěsna se úhlu dotýká.', 'Синусу нужен катет **напротив** угла. А известный катет прилежит к углу.'),
      null,
      L('Tangens dělí odvěsnu odvěsnou – jedna ze známých stran je ale přepona.', 'Тангенс делит катет на катет, а одна из известных сторон — гипотенуза.'),
    ]),
    L('přilehlá / přepona = cos α, proto **α = arccos (přilehlá / přepona)**. Na kalkulačce cos⁻¹.', 'прилежащий / гипотенуза = cos α, поэтому **α = arccos (прилежащий / гипотенуза)**. На калькуляторе cos⁻¹.'),
    null,
    3,
    L('Vyber funkci pro přilehlou odvěsnu a přeponu', 'Выбери функцию для прилежащего катета и гипотенузы'),
  );
  const s3 = useQuizStep(
    L('Znáš obě odvěsny. Která funkce dá úhel α?', 'Известны оба катета. Какая функция даёт угол α?'),
    fns(2, [L('Sinus potřebuje přeponu – tu neznáš.', 'Синусу нужна гипотенуза — она неизвестна.'), L('Kosinus potřebuje přeponu – tu neznáš.', 'Косинусу нужна гипотенуза — она неизвестна.'), null]),
    L(
      'protilehlá / přilehlá = tg α, proto **α = arctg (protilehlá / přilehlá)**. Na kalkulačce tan⁻¹. V režimu DEG vyjde úhel ve stupních.',
      'противолежащий / прилежащий = tg α, поэтому **α = arctg (противолежащий / прилежащий)**. На калькуляторе tan⁻¹. В режиме DEG результат получается в градусах.',
    ),
    null,
    3,
    L('Vyber funkci pro obě odvěsny', 'Выбери функцию для двух катетов'),
  );
  const steps = [s1, s2, s3];
  const cur = steps.findIndex((s) => !s.done);
  const known = new Set<TriSide>(cur === 0 ? ['opp', 'hyp'] : cur === 1 ? ['adj', 'hyp'] : cur === 2 ? ['adj', 'opp'] : []);
  const g = triGeom(4, 3, { fit: [70, 20, 210, 150] });
  const mid = (s: TriSide) => {
    const [p, q] = triSide(g, s);
    return pt((p.x + q.x) / 2, (p.y + q.y) / 2);
  };
  const names: Record<TriSide, L> = { hyp: L('přepona', 'гипотенуза'), adj: L('přilehlá', 'прилежащий'), opp: L('protilehlá', 'противолежащий') };
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Trojúhelník se zvýrazněnými známými stranami', 'Треугольник с выделенными известными сторонами'))}
        draw={(b, c) => {
          b.rightTriangle(g, {}, null, 'α');
          known.forEach((s) => {
            const [p, q] = triSide(g, s);
            b.line(p.x, p.y, q.x, q.y, c.highlight, 5.0);
          });
          const col = (s: TriSide) => (known.has(s) ? c.highlight : s === 'hyp' ? c.load : c.derived);
          const hm = mid('hyp');
          b.text(hm.x - 14, hm.y - 12, t.pick(names.hyp), col('hyp'), 14, 'end', null, known.has('hyp'));
          const om = mid('opp');
          b.text(om.x + 12, om.y + 5, t.pick(names.opp), col('opp'), 14, 'start', null, known.has('opp'));
          const am = mid('adj');
          b.text(am.x, am.y + 22, t.pick(names.adj), col('adj'), 14, 'middle', null, known.has('adj'));
          b.text(200, 218, cur >= 0 ? t.pick(L('známe zvýrazněné strany, hledáme α', 'известны выделенные стороны, ищем α')) : 'sin⁻¹ · cos⁻¹ · tan⁻¹', c.inkSoft, 13);
        }}
      />
      <CardText
        text={L(
          'Inverzní funkci volíme podle dvou známých stran. **sin⁻¹** na kalkulačce znamená arcsin, ne 1/sin.',
          'Обратную функцию выбирают по двум известным сторонам. **sin⁻¹** на калькуляторе означает arcsin, а не 1/sin.',
        )}
      />
      <Steps steps={steps} />
    </>
  );
}

/** Karta 2: přilehlá a přepona → arccos, pak výpočet. */
function CheckArccos() {
  const t = useTr();
  const s1 = useQuizStep(
    L('Znáš přilehlou odvěsnu 4 m a přeponu 5 m. Jak spočítáš úhel α?', 'Известны прилежащий катет 4 м и гипотенуза 5 м. Как найти угол α?'),
    [
      opt(L('α = arccos (4/5)', 'α = arccos (4/5)'), true),
      opt(L('α = arcsin (4/5)', 'α = arcsin (4/5)'), false, L('Sinus potřebuje odvěsnu **naproti** úhlu. Strana 4 m se ale úhlu α dotýká – jak se jí říká?', 'Синусу нужен катет **напротив** угла. А сторона 4 м касается угла α — как она называется?')),
      opt(L('α = arctg (4/5)', 'α = arctg (4/5)'), false, L('Tangens dělí odvěsnu odvěsnou. Tady je jedna ze známých stran přepona.', 'Тангенс делит катет на катет. А здесь одна из известных сторон — гипотенуза.')),
      opt(L('α = arccos (5/4)', 'α = arccos (5/4)'), false, L('5/4 je víc než 1 – takový kosinus neexistuje, odvěsna je vždy kratší než přepona. Co patří do zlomku nahoru?', '5/4 больше 1 — такого косинуса не бывает, катет всегда короче гипотенузы. Что должно быть в числителе?')),
      opt(L('α = cos (4/5)', 'α = cos (4/5)'), false, L('cos udělá z úhlu poměr. My máme poměr a chceme úhel – která funkce jde opačným směrem?', 'cos превращает угол в отношение. У нас есть отношение, а нужен угол — какая функция идёт в обратную сторону?')),
    ],
    L('Přilehlá a přepona → kosinus: cos α = 4/5 = 0,8, tedy α = arccos 0,8.', 'Прилежащий и гипотенуза → косинус: cos α = 4/5 = 0,8, значит α = arccos 0,8.'),
    null,
    2,
    L('Vyber výpočet úhlu α', 'Выбери способ вычисления угла α'),
  );
  const s2 = useNumStep(
    L('Vypočítej α = arccos 0,8 na jedno desetinné místo (kalkulačka v režimu DEG).', 'Вычисли α = arccos 0,8 с точностью до десятых (калькулятор в режиме DEG).'),
    [numSpec(L('α', 'α'), Trig.acosD(0.8), '°', false, 'angle')],
    L('cos α = 4/5 = 0,8, tedy α = arccos 0,8 ≈ 36,9°. Hodnota 53,1° patří k druhému ostrému úhlu.', 'cos α = 4/5 = 0,8, значит α = arccos 0,8 ≈ 36,9°. Значение 53,1° относится ко второму острому углу.'),
    [
      trapNear(L('To je arcsin 0,8 – úhel naproti straně 4 m. Použij cos⁻¹.', 'Это arcsin 0,8 — угол напротив стороны 4 м. Используй cos⁻¹.'), Trig.asinD(0.8)),
      trapNear(L('To je arctg 0,8. Strana 5 m je přepona – použij cos⁻¹.', 'Это arctg 0,8. Сторона 5 м — гипотенуза, используй cos⁻¹.'), Trig.atanD(0.8)),
      trapNear(L('Kalkulačka je v režimu RAD – vyšly radiány. Přepni na DEG.', 'Калькулятор в режиме RAD — получились радианы. Переключи на DEG.'), Math.acos(0.8)),
      trapNear(L('0,8 je teprve cos α. Úhel dostaneš přes cos⁻¹.', '0,8 — это пока cos α. Угол получишь через cos⁻¹.'), 0.8),
    ],
    L('Na kalkulačce v režimu DEG: SHIFT (2nd) + cos, pak 0,8.', 'На калькуляторе в режиме DEG: SHIFT (2nd) + cos, затем 0,8.'),
    L('Vypočítej α na jedno desetinné místo', 'Вычисли α с точностью до десятых'),
  );
  const solved = s2.done;
  return (
    <>
      <Stage
        h={200}
        description={t.pick(L('Trojúhelník: přilehlá 4 m, přepona 5 m, úhel α neznámý', 'Треугольник: прилежащий катет 4 м, гипотенуза 5 м, угол α неизвестен'))}
        draw={(b) => {
          const g = triGeom(4, 3, { fit: [80, 25, 230, 150] });
          b.rightTriangle(g, { adj: t.pick(L('4 m', '4 м')), hyp: t.pick(L('5 m', '5 м')) }, null, solved ? 'α ≈ 36,9°' : 'α = ?');
        }}
      />
      <CardText text={L('Lano dlouhé 5 m překonává vodorovnou vzdálenost 4 m. Hledáme jeho úhel s vodorovnou.', 'Трос длиной 5 м перекрывает горизонтальное расстояние 4 м. Ищем его угол с горизонталью.')} />
      <Steps steps={[s1, s2]} />
    </>
  );
}

/** Karta 3: sklon zadaný poměrem výška : vodorovná délka = tg α. */
function SlopeRatio() {
  const t = useTr();
  const m = t.pick(L('m', 'м'));
  const kn = t.pick(L('kN', 'кН'));
  const s1 = useQuizStep(
    L('Který z těchto sklonů je nejstrmější?', 'Какой из этих уклонов самый крутой?'),
    [
      opt(L('1 : 2', '1 : 2'), false, L('1 : 2 znamená 1 díl nahoru na 2 díly vodorovně – tg α = 0,5. Porovnej poměry jako čísla.', '1 : 2 — 1 часть вверх на 2 части по горизонтали, tg α = 0,5. Сравни отношения как числа.')),
      opt(L('3 : 4', '3 : 4'), false, L('3 : 4 je tg α = 0,75. Je to nejvíc? Spočítej i ostatní poměry.', '3 : 4 — это tg α = 0,75. Это самое большое? Посчитай и другие отношения.')),
      opt(L('1 : 1', '1 : 1'), true),
    ],
    L('Větší poměr = strmější: 1 : 1 → tg α = 1 (45°), 3 : 4 → 0,75 (36,9°), 1 : 2 → 0,5 (26,6°).', 'Больше отношение — круче: 1 : 1 → tg α = 1 (45°), 3 : 4 → 0,75 (36,9°), 1 : 2 → 0,5 (26,6°).'),
    null,
    3,
    L('Vyber nejstrmější z těchto sklonů', 'Выбери самый крутой из этих уклонов'),
  );
  const s2 = useNumStep(
    L('Svah má sklon 1 : 2 a vodorovnou délku 6 m. Jak je vysoký?', 'Откос имеет уклон 1 : 2 и горизонтальную длину 6 м. Какова его высота?'),
    [numSpec(L('h', 'h'), 3, m)],
    L('Na každé 2 m vodorovně 1 m nahoru: h = 6 · 1/2 = 3 m.', 'На каждые 2 м по горизонтали 1 м вверх: h = 6 · 1/2 = 3 м.'),
    [trapNear(L('Poměr je výška : vodorovná délka. Výška je tedy menší než 6 m.', 'Отношение — это высота : горизонтальная длина. Значит, высота меньше 6 м.'), 12)],
    L('Na každé 2 m vodorovně 1 m nahoru. Kolikrát se 2 m vejdou do 6 m?', 'На каждые 2 м по горизонтали 1 м вверх. Сколько раз 2 м укладываются в 6 м?'),
    L('Vypočítej výšku při sklonu 1 : 2 a vodorovné délce 6 m', 'Найди высоту при уклоне 1 : 2 и горизонтальной длине 6 м'),
  );
  const s3 = useNumStep(
    L('Lano táhne silou T = 10 kN ve směru se sklonem 3 : 4. Jak velká je svislá složka |[[T_y]]|?', 'Трос тянет с силой T = 10 кН в направлении с уклоном 3 : 4. Каков модуль вертикальной составляющей |[[T_y]]|?'),
    [numSpec(L('|[[T_y]]|', '|[[T_y]]|'), 6, kn)],
    L(
      'Směrový trojúhelník 3–4–5 má přeponu 5, takže |[[T_y]]| = T · 3/5 = 6 kN (a |[[T_x]]| = T · 4/5 = 8 kN). Úhel počítat nemusíš.',
      'У треугольника направлений 3–4–5 гипотенуза 5, поэтому |[[T_y]]| = T · 3/5 = 6 кН (а |[[T_x]]| = T · 4/5 = 8 кН). Угол вычислять не нужно.',
    ),
    [
      trapNear(L('To je vodorovná složka – ta leží u čtyřky. Svislá patří k trojce.', 'Это горизонтальная составляющая — она относится к четвёрке. Вертикальная — к тройке.'), 8),
      trapNear(L('3/4 je tg α, poměr dvou odvěsen. Složka = síla · (odvěsna / přepona) – jak dlouhá je přepona?', '3/4 — это tg α, отношение катетов. Составляющая = сила · (катет / гипотенуза) — какова гипотенуза?'), 7.5),
    ],
    L('Přepona směrového trojúhelníku 3-4-5 je 5. Svislá složka = T · 3/5.', 'Гипотенуза треугольника направлений 3-4-5 равна 5. Вертикальная составляющая = T · 3/5.'),
    L('Urči svislou složku tahu 10 kN ve směru 3 : 4', 'Найди вертикальную составляющую тяги 10 кН при 3 : 4'),
  );
  const steps = [s1, s2, s3];
  const cur = steps.findIndex((s) => !s.done);
  return (
    <>
      <Stage
        h={220}
        description={t.pick(L('Sklon zadaný poměrem', 'Уклон, заданный отношением'))}
        draw={(b, c) => {
          if (cur === 0) {
            const st = 36;
            const ox = 60;
            const oy = 190;
            b.grid(ox, oy - 4 * st, st, 4, 4);
            b.rightTriangle({ a: pt(ox, oy), b: pt(ox + 4 * st, oy - 3 * st), c: pt(ox + 4 * st, oy) }, { adj: t.pick(L('4 díly', '4 части')), opp: t.pick(L('3 díly', '3 части')) }, null, null, c.ink);
            b.text(392, 40, t.pick(L('sklon 3 : 4', 'уклон 3 : 4')), c.ink, 15, 'end', null, true);
            b.text(392, 64, t.pick(L('výška : vodorovně', 'высота : горизонталь')), c.inkSoft, 13, 'end');
            b.text(392, 88, 'tg α = 3/4', c.ink, 14, 'end');
          } else if (cur === 1) {
            const u = 40;
            const ox = 60;
            const oy = 170;
            b.line(20, oy, 380, oy, c.ink, 1.2);
            b.poly([[ox, oy], [ox + 6 * u, oy], [ox + 6 * u, oy - 3 * u]], c.subtle, null, 1.5, 0.7);
            b.line(ox, oy, ox + 6 * u, oy - 3 * u, c.ink, 3.0);
            b.line(ox + 6 * u, oy, ox + 6 * u, oy - 3 * u, c.derived, 2.0);
            b.rightAngleMark(pt(ox + 6 * u, oy), pt(ox, oy), pt(ox + 6 * u, oy - 3 * u));
            b.dimLine(ox, oy + 18, ox + 6 * u, oy + 18, t.pick(L('6 m vodorovně', '6 м по горизонтали')), 14, c.derived);
            b.text(ox + 6 * u + 10, oy - 1.5 * u + 5, 'h = ?', c.derived, 14, 'start', null, true);
            const px = ox + 2 * u;
            const py = oy - u;
            b.line(px, py, px + 2 * u * 0.6, py, c.inkSoft, 1.0);
            b.line(px + 2 * u * 0.6, py, px + 2 * u * 0.6, py - u * 0.6, c.inkSoft, 1.0);
            b.text(px + u * 0.6, py + 15, '2', c.inkSoft, 12);
            b.text(px + 2 * u * 0.6 + 6, py - u * 0.3 + 4, '1', c.inkSoft, 12, 'start');
          } else {
            const ox = 70;
            const oy = 195;
            b.line(ox, oy, ox + 240, oy - 180, c.ink, 2.0);
            b.circle(ox, oy, 6, c.ink);
            b.text(ox - 8, oy + 5, 'O', c.ink, 14, 'end', null, true);
            const px = 190;
            const py = oy - 120 * 0.75;
            const qx = px + 80;
            const ry = py - 60;
            b.line(px, py, qx, py, c.inkSoft, 1.0);
            b.line(qx, py, qx, ry, c.inkSoft, 1.0);
            b.text((px + qx) / 2, py + 15, '4', c.inkSoft, 13);
            b.text(qx + 6, (py + ry) / 2 + 4, '3', c.inkSoft, 13, 'start');
            b.text((px + qx) / 2 - 8, (py + ry) / 2 - 6, '5', c.inkSoft, 13, 'end');
            const tx = ox + 80;
            const ty = oy - 60;
            b.force(ox, oy, tx, ty, c.load, 3.0);
            b.text(tx - 10, ty - 8, ` = 10 ${kn}`, c.load, 14, 'end', 'T');
            if (cur < 0) {
              b.line(tx, ty, tx, oy, c.derived, 1.0, true);
              b.force(ox, oy, ox + 80, oy, c.derived, 2.2);
              b.force(ox + 80, oy, tx, ty, c.derived, 2.2);
              b.text(ox + 40, oy + 18, ` = 8 ${kn}`, c.derived, 13, 'middle', 'T_x');
              b.text(tx + 8, (oy + ty) / 2 + 5, ` = 6 ${kn}`, c.derived, 13, 'start', 'T_y', true);
            } else b.text(392, 200, ' = ?', c.derived, 15, 'end', 'T_y', true);
          }
        }}
      />
      <CardText
        text={L(
          'Poměr 3 : 4 znamená 3 díly výšky na 4 díly vodorovně – je to přímo **tg α**. Na výkresu si vždy ověř, co je v poměru první.',
          'Отношение 3 : 4 означает 3 части высоты на 4 части по горизонтали — это прямо **tg α**. На чертеже всегда проверяй, что стоит в отношении первым.',
        )}
      />
      <Steps steps={steps} />
    </>
  );
}

/** Silnice se sklonem p % na vodorovné délce runLabel; obě osy ve stejném měřítku (strmý svah se zmenší). */
function road(b: PrimBuilder, c: Colors, p: number, runLabel: string, riseLabel: string, angleLabel: string | null) {
  const tg = p / 100;
  const ox = 70;
  const oy = 185;
  const run = Math.min(170, 160 / Math.max(tg, 1e-9));
  const rise = run * tg;
  b.line(20, oy, 380, oy, c.ink, 1.2);
  b.poly([[ox, oy], [ox + run, oy], [ox + run, oy - rise]], c.subtle, null, 1.5, 0.7);
  b.line(ox, oy, ox + run, oy - rise, c.ink, 6.0);
  b.line(ox, oy, ox + run, oy - rise, c.beam, 3.5);
  b.line(ox + run, oy, ox + run, oy - rise, c.derived, 1.4, true);
  if (rise > 12) b.rightAngleMark(pt(ox + run, oy), pt(ox, oy), pt(ox + run, oy - rise));
  b.dimLine(ox, oy + 14, ox + run, oy + 14, runLabel, 14, c.derived);
  b.text(ox + run + 10, oy - rise / 2 + 5, riseLabel, c.derived, 14, 'start', null, true);
  if (p > 0 && angleLabel !== null) {
    b.angleArc(ox, oy, 36, 0, -Trig.atanD(tg), null);
    b.text(ox - 8, oy - 6, angleLabel, c.inkSoft, 13, 'end');
  }
}

/** Karta 4: sklon v procentech = 100 · h / d – a proč to nejsou stupně. */
function SlopePercent() {
  const t = useTr();
  const kit = useKit();
  const m = t.pick(L('m', 'м'));
  const guess = kit.mut<number | null>(null);
  const demoP = kit.mut(10);
  const demoDone = kit.mut(false);
  const p = kit.mut(100);
  const s1 = useNumStep(
    L('Silnice má sklon 10 %. O kolik metrů vystoupá na 100 m vodorovně?', 'Уклон дороги 10 %. На сколько метров она поднимается на 100 м по горизонтали?'),
    [numSpec(L('h', 'h'), 10, m)],
    L('10 % = 10 m převýšení na 100 m **vodorovné** délky: h = 0,10 · 100 = 10 m.', '10 % = 10 м подъёма на 100 м **горизонтальной** длины: h = 0,10 · 100 = 10 м.'),
    [trapNear(L('Sklon v % je převýšení na 100 m vodorovně. Kolik je 10 % ze 100 m?', 'Уклон в % — подъём на 100 м по горизонтали. Сколько будет 10 % от 100 м?'), 1000)],
    L('10 % = 10 m na každých 100 m vodorovně.', '10 % = 10 м на каждые 100 м по горизонтали.'),
    L('Urči převýšení při sklonu 10 % na 100 m vodorovně', 'Найди подъём при уклоне 10 % на 100 м по горизонтали'),
  );
  const tips = [L('45°', '45°'), L('60°', '60°'), L('90°', '90°'), L('100°', '100°')];
  const s2 = useGuessStep(L('Odhadni úhel svahu se sklonem 100 %', 'Предскажи угол при уклоне 100 %'), tips, guess.v, (i) => (guess.v = i));
  useLaunched([guess.v !== null], async (job) => {
    if (guess.v === null) return;
    await job.delay(500);
    for (let v = 15; v <= 100; v += 5) {
      demoP.v = v;
      await job.delay(70);
    }
    await job.delay(1200);
    demoDone.v = true;
  });
  const s3 = step(
    L('Sleduj, jak vypadá sklon 100 %', 'Посмотри, как выглядит уклон 100 %'),
    demoDone.v,
    () => <SmallText text={L('Sklon roste od 10 % ke 100 %…', 'Уклон растёт от 10 % до 100 %…')} />,
    () => (
      <Note
        tone={guess.v === 0 ? 'ok' : 'info'}
        text={L(
          (guess.v === 0 ? 'Tip sedí. ' : 'Procenta nejsou stupně. ') + '100 % znamená h = d, tg α = 1, tedy α = 45°. Strmější svah má víc než 100 %; svislá stěna (d = 0) žádný konečný sklon v % nemá.',
          (guess.v === 0 ? 'Прогноз верен. ' : 'Проценты — не градусы. ') + '100 % означает h = d, tg α = 1, то есть α = 45°. У более крутого склона больше 100 %; у вертикальной стены (d = 0) конечного уклона в % нет.',
        )}
      />
    ),
  );
  const s4 = useNumStep(
    L('Rampa stoupá o 2 m na 10 m vodorovně. Jaký má sklon v procentech?', 'Пандус поднимается на 2 м на 10 м по горизонтали. Каков его уклон в процентах?'),
    [numSpec(L('sklon', 'уклон'), 20, '%', false, 'percent')],
    L('100 · h / d = 100 · 2 / 10 = 20 %. (Úhel by byl arctg 0,2 ≈ 11,3° – jiné číslo.)', '100 · h / d = 100 · 2 / 10 = 20 %. (Угол был бы arctg 0,2 ≈ 11,3° — другое число.)'),
    [
      trapNear(L('0,2 je poměr h / d. V procentech ho ještě vynásob stem.', '0,2 — это отношение h / d. Для процентов умножь его на сто.'), 0.2),
      trapNear(L('To je úhel ve stupních. Sklon v % je 100 · h / d.', 'Это угол в градусах. Уклон в % — это 100 · h / d.'), Trig.atanD(0.2)),
      trapNear(L('Do zlomku patří převýšení nahoru: h / d.', 'В числителе подъём: h / d.'), 500),
    ],
    L('Sklon v % = 100 · h / d.', 'Уклон в % = 100 · h / d.'),
    L('Urči sklon v % pro h = 2 m a d = 10 m', 'Найди уклон в % при h = 2 м и d = 10 м'),
  );
  const steps = [s1, s2, s3, s4];
  const cur = steps.findIndex((s) => !s.done);
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Silnice se sklonem v procentech', 'Дорога с уклоном в процентах'))}
        draw={(b, c) => {
          const big = (s: string) => b.text(392, 36, s, c.ink, 18, 'end', null, true);
          if (cur === 0) {
            road(b, c, 10, t.pick(L('100 m vodorovně', '100 м по горизонтали')), 'h = ?', null);
            big('10 %');
          } else if (cur === 1) {
            road(b, c, 100, `100 ${m}`, `100 ${m}`, 'α = ?');
            big('100 %');
          } else if (cur === 2) {
            const a = Trig.atanD(demoP.v / 100);
            road(b, c, demoP.v, `100 ${m}`, `${demoP.v} ${m}`, demoP.v === 100 ? 'α = 45°' : `α ≈ ${num(a, 1)}°`);
            big(`${demoP.v} %`);
          } else if (cur === 3) {
            road(b, c, 20, `10 ${m}`, `2 ${m}`, null);
            big('? %');
          } else {
            const a = Trig.atanD(p.v / 100);
            road(b, c, p.v, `100 ${m}`, `${p.v} ${m}`, `α ≈ ${num(a, 1)}°`);
            big(`${p.v} %`);
          }
        }}
      />
      <CardText text={L('Sklon v procentech je **100 · h / d**, kde d je vodorovná délka. Procenta a stupně nejsou stejná veličina.', 'Уклон в процентах равен **100 · h / d**, где d — горизонтальная длина. Проценты и градусы — разные величины.')} />
      <Steps
        steps={steps}
        after={() => <ValueSlider label={t.pick(L(`Sklon = ${p.v} %`, `Уклон = ${p.v} %`))} value={p.v} min={0} max={300} steps={59} onChange={(v) => (p.v = Math.round(v / 5) * 5)} />}
      />
    </>
  );
}

/** Karta 5: úloha – úhel rampy ze stoupání a vodorovné délky. */
function RampAngle() {
  const t = useTr();
  const s1 = useQuizStep(
    L('Rampa vystoupá 1,2 m na vodorovné délce 10 m. Čemu se rovná tg α?', 'Пандус поднимается на 1,2 м на горизонтальной длине 10 м. Чему равен tg α?'),
    [
      opt(L('1,2 / 10', '1,2 / 10'), true),
      opt(L('10 / 1,2', '10 / 1,2'), false, L('Tangens = protilehlá / přilehlá. Která strana leží naproti úhlu u země?', 'Тангенс = противолежащий / прилежащий. Какая сторона лежит напротив угла у земли?')),
      opt(L('1,2 / √(10² + 1,2²)', '1,2 / √(10² + 1,2²)'), false, L('Dělit délkou po svahu (přeponou) dává sinus, ne tangens.', 'Деление на длину по склону (гипотенузу) даёт синус, а не тангенс.')),
    ],
    L('tg α = převýšení / vodorovná délka = 1,2 / 10 = 0,12. Délku po svahu nepotřebuješ.', 'tg α = подъём / горизонтальная длина = 1,2 / 10 = 0,12. Длина по склону не нужна.'),
    null,
    1,
    L('Vyber poměr pro tg α', 'Выбери отношение для tg α'),
  );
  const s2 = useNumStep(
    L('Vypočítej úhel rampy α ve stupních (tg α = 0,12).', 'Вычисли угол пандуса α в градусах (tg α = 0,12).'),
    [numSpec(L('α', 'α'), Trig.atanD(0.12), '°', false, 'angle')],
    L('tg α = 1,2 / 10 = 0,12, takže α = arctg 0,12 ≈ 6,8°. Stejný sklon je 12 %, nikoli 12°.', 'tg α = 1,2 / 10 = 0,12, поэтому α = arctg 0,12 ≈ 6,8°. Этот уклон равен 12 %, а не 12°.'),
    [
      trapNear(L('12 je sklon v procentech, ne úhel. Ještě použij tan⁻¹.', '12 — это уклон в процентах, а не угол. Ещё нужно применить tan⁻¹.'), 12),
      trapNear(L('Kolem 0,12: buď je to teprve tg α (ještě tan⁻¹), nebo je kalkulačka v RAD (přepni na DEG).', 'Около 0,12: либо это пока tg α (ещё tan⁻¹), либо калькулятор в RAD (переключи на DEG).'), 0.12),
      trapNear(L('To je úhel u svislice – vyjde, když se dělí délka výškou. Co patří ve zlomku nahoru?', 'Это угол у вертикали — так выходит, если делить длину на высоту. Что должно быть в дроби сверху?'), 90 - Trig.atanD(0.12)),
    ],
    L('α = tan⁻¹ 0,12 – kalkulačka v režimu DEG.', 'α = tan⁻¹ 0,12 — калькулятор в режиме DEG.'),
    L('Vypočítej úhel rampy ve stupních', 'Вычисли угол наклона пандуса в градусах'),
  );
  const solved = s2.done;
  return (
    <>
      <Stage
        h={150}
        description={t.pick(L('Rampa: 10 m vodorovně, 1,2 m nahoru', 'Пандус: 10 м по горизонтали, 1,2 м вверх'))}
        draw={(b, c) => {
          const ox = 50;
          const oy = 95;
          const run = 300;
          const rise = run * 0.12;
          b.line(20, oy, 380, oy, c.ink, 1.2);
          b.poly([[ox, oy], [ox + run, oy], [ox + run, oy - rise]], c.subtle, null, 1.5, 0.7);
          b.line(ox, oy, ox + run, oy - rise, c.ink, 3.0);
          b.line(ox + run, oy, ox + run, oy - rise, c.derived, 2.0);
          b.dimLine(ox, oy + 16, ox + run, oy + 16, t.pick(L('10 m vodorovně', '10 м по горизонтали')), 14, c.derived);
          b.text(ox + run + 8, oy - rise / 2 + 5, t.pick(L('1,2 m', '1,2 м')), c.derived, 14, 'start');
          b.angleArc(ox, oy, 70, 0, -Trig.atanD(0.12), null);
          b.text(ox + 80, oy - 22, solved ? 'α ≈ 6,8°' : 'α = ?', c.ink, 14, 'start', null, true);
        }}
      />
      <CardText text={L('Rampa překonává výšku 1,2 m na vodorovné délce 10 m. Délka po svahu není zadaná.', 'Пандус поднимается на 1,2 м при горизонтальной длине 10 м. Длина по склону не задана.')} />
      <Steps steps={[s1, s2]} />
    </>
  );
}

export const cards = [BackToAngle, WhichInverse, CheckArccos, SlopeRatio, SlopePercent, RampAngle];
