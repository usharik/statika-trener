/* Typy úloh E18–E23 (shodné s aplikací – practice/types/E18.kt … E23.kt). */
import type { Trap } from '../exercise';
import { Cards, Fig, Fn, L, TrapText, Traps, approx, close2, deg, exercise, exerciseType, field, given, n, nearRel, type FnName } from './common';

/** E18 – sin / cos libovolného úhlu (jednotková kružnice). Jen úroveň 3. */
export const E18 = exerciseType(
  'E18',
  L('Úhly přes 90°', 'Углы больше 90°'),
  Cards.QUADRANTS,
  (rng, level, seed) => {
    const phi = rng.pick([120, 135, 150, 210, 225, 240, 300, 315, 330]);
    const fn = rng.pick<FnName>(['sin', 'cos']);
    const other: FnName = fn === 'sin' ? 'cos' : 'sin';
    const ans = Fn.deg(fn, phi);
    const r = phi % 180;
    const ref = r > 90 ? 180 - r : r;
    const sign = ans < 0 ? '−' : '+';
    return exercise('E18', seed, level, {
      prompt: L(`Kolik je ${fn} ${deg(phi)}? (i se znaménkem)`, `Чему равен ${fn} ${deg(phi)}? (со знаком)`),
      fig: Fig.force(1, phi, phi, { from: 'plusX', label: 'r', unit: '' }),
      fields: [field(L(`${fn} ${deg(phi)}`, `${fn} ${deg(phi)}`), ans, '', { signed: true, tol: 'ratio', digits: 3 })],
      hints: [
        L(
          `Najdi ostrý úhel k ose x (tady ${deg(ref)}) a znaménko podle kvadrantu: cos je vodorovná souřadnice, sin svislá.`,
          `Найди острый угол к оси x (здесь ${deg(ref)}) и знак по четверти: cos — горизонтальная координата, sin — вертикальная.`,
        ),
        L(`${fn} ${deg(phi)} = ${sign}${fn} ${deg(ref)}`, `${fn} ${deg(phi)} = ${sign}${fn} ${deg(ref)}`),
      ],
      traps: [
        new TrapText('SIGNQ', L('Velikost sedí. Na které straně osy leží bod na kružnici? Vpravo/nahoře +, vlevo/dole −.', 'Величина верна. С какой стороны от оси лежит точка на окружности? Справа/сверху +, слева/снизу −.'), Cards.QUADRANTS).at(-ans),
        Traps.SWAP.with(
          L(
            `To je ${other} ${deg(phi)} – druhá souřadnice bodu. ${fn === 'cos' ? 'Kosinus je vodorovná souřadnice.' : 'Sinus je svislá souřadnice.'}`,
            `Это ${other} ${deg(phi)} — другая координата точки. ${fn === 'cos' ? 'Косинус — горизонтальная координата.' : 'Синус — вертикальная координата.'}`,
          ),
        ).at(Fn.deg(other, phi)),
      ],
      ok: L(`Správně: ${fn} ${deg(phi)} ${approx(ans, 3)}.`, `Верно: ${fn} ${deg(phi)} ${approx(ans, 3)}.`),
      solvedFig: Fig.force(1, phi, phi, { from: 'plusX', label: 'r', components: true, unit: '' }),
    });
  },
  [3, 3],
);

const NEGANG_TEXT = L(
  'Do arctg dej velikosti bez znamének – hledáme ostrý (kladný) úhel k vodorovné, ne úhel od osy +x. Směr síly je vidět ze znamének složek.',
  'В arctg подставляй величины без знаков — ищем острый (положительный) угол к горизонтали, а не угол от оси +x. Направление силы видно по знакам составляющих.',
);

/** E19 – výslednice ze složek: velikost a ostrý úhel k vodorovné. */
export const E19 = exerciseType('E19', L('Výslednice ze složek', 'Равнодействующая по составляющим'), Cards.RESULTANT, (rng, level, seed) => {
  let x: number, y: number;
  if (level === 1) {
    const [p, q] = rng.pick<[number, number, number]>([
      [3, 4, 5],
      [6, 8, 10],
      [4, 3, 5],
      [8, 6, 10],
    ]);
    x = p;
    y = q;
  } else if (level === 2) {
    const [p, q] = rng.pick<[number, number, number]>([
      [3, 4, 5],
      [4, 3, 5],
      [6, 8, 10],
      [5, 12, 13],
      [12, 5, 13],
      [8, 15, 17],
    ]);
    x = p * (rng.chance(0.5) ? -1 : 1);
    y = q * (rng.chance(0.5) ? -1 : 1);
  } else {
    do {
      x = rng.int(-12, 12);
      y = rng.int(-12, 12);
    } while (x === 0 || y === 0 || Math.abs(x) === Math.abs(y));
  }
  const ax = Math.abs(x);
  const ay = Math.abs(y);
  const r = Math.sqrt(x * x + y * y);
  const a = Fn.inv('tg', ay / ax);
  const dir = x > 0 && y > 0 ? L('doprava nahoru', 'вправо вверх') : x < 0 && y > 0 ? L('doleva nahoru', 'влево вверх') : x < 0 ? L('doleva dolů', 'влево вниз') : L('doprava dolů', 'вправо вниз');
  const negAt = (w: number) => Traps.NEGANG.with(NEGANG_TEXT).where([null, w], (v) => v[1] != null && Math.abs(Math.abs(v[1]) - w) <= 0.5);
  return exercise('E19', seed, level, {
    prompt: L(
      `Složky síly jsou [[F_x]] = ${n(x)} kN a [[F_y]] = ${n(y)} kN. Jak velká je celá síla R a jaký ostrý úhel α svírá s vodorovnou? (úhel na 1 desetinné místo)`,
      `Составляющие силы: [[F_x]] = ${n(x)} кН и [[F_y]] = ${n(y)} кН. Какова величина всей силы R и какой острый угол α она составляет с горизонталью? (угол — до 1 знака после запятой)`,
    ),
    fig: Fig.comps(x, y),
    fields: [field(L('R', 'R'), r, 'kN'), field(L('α', 'α'), a, '°', { tol: 'angle', digits: 1 })],
    hints: [
      L('Velikost: Pythagorova věta R = √([[F_x]]² + [[F_y]]²). Úhel: tg α = |[[F_y]]| / |[[F_x]]| – bez znamének.', 'Величина: теорема Пифагора R = √([[F_x]]² + [[F_y]]²). Угол: tg α = |[[F_y]]| / |[[F_x]]| — без знаков.'),
      L(`R = √(${n(ax)}² + ${n(ay)}²); α = arctg (${n(ay)} / ${n(ax)})`, `R = √(${n(ax)}² + ${n(ay)}²); α = arctg (${n(ay)} / ${n(ax)})`),
    ],
    traps: [
      Traps.SUM.with(L('Složky se nesčítají přímo – jsou na sebe kolmé. Použij Pythagorovu větu.', 'Составляющие нельзя просто сложить — они перпендикулярны. Нужна теорема Пифагора.')).at(ax + ay, null),
      Traps.NOSQRT.at(x * x + y * y, null),
      Traps.COMPL.with(L('To je úhel od svislice – vyjde, když se dělí |[[F_x]]| / |[[F_y]]|. Naproti úhlu u vodorovné leží [[F_y]].', 'Это угол от вертикали — так выходит, если делить |[[F_x]]| / |[[F_y]]|. Напротив угла у горизонтали лежит [[F_y]].')).at(null, 90 - a),
      negAt(180 - a),
      negAt(180 + a),
      negAt(360 - a),
      Traps.RAD.where([null, (a * Math.PI) / 180], (v) => nearRel(v[1], (a * Math.PI) / 180)),
    ],
    ok: L(`Správně: R ${approx(r)} kN, α ≈ ${n(a, 1)}°; síla míří ${dir.cs}.`, `Верно: R ${approx(r)} кН, α ≈ ${n(a, 1)}°; сила направлена ${dir.ru}.`),
    solvedFig: Fig.comps(x, y, true),
  });
});

/** E20 – složky síly v prutu se sklonem zadaným poměrem. */
export const E20 = exerciseType('E20', L('Složky ze sklonu', 'Составляющие по уклону'), Cards.SLOPE_RATIO, (rng, level, seed) => {
  const all: [number, number, number][] = [
    [3, 4, 5],
    [4, 3, 5],
    [5, 12, 13],
    [8, 15, 17],
  ];
  const [m, nn, c] = rng.pick(level === 1 ? all.slice(0, 2) : all);
  let f: number;
  if (level < 3) f = c * rng.int(1, 5);
  else {
    const k = rng.int(1, 4);
    f = c * k + rng.int(1, c - 1);
  }
  const fx = (f * nn) / c;
  const fy = (f * m) / c;
  return exercise('E20', seed, level, {
    prompt: L(
      `Síla [[F]] = ${n(f)} kN působí v prutu se sklonem ${m} : ${nn} (výška : vodorovná délka). Urči velikosti vodorovné a svislé složky – ` + (level < 3 ? 'bez kalkulačky.' : 'bez počítání úhlu (na 2 desetinná místa).'),
      `Сила [[F]] = ${n(f)} кН действует в стержне с уклоном ${m} : ${nn} (высота : горизонтальная длина). Найди величины горизонтальной и вертикальной составляющих — ` + (level < 3 ? 'без калькулятора.' : 'не вычисляя угол (до 2 знаков после запятой).'),
    ),
    fig: Fig.slope(m, nn, `${m}`, `${nn}`, null),
    fields: [field(L('[[F_x]]', '[[F_x]]'), fx, 'kN'), field(L('[[F_y]]', '[[F_y]]'), fy, 'kN')],
    hints: [
      L(
        `Úhel nepotřebuješ. Sklon ${m} : ${nn} je trojúhelník s odvěsnami ${m} a ${nn} – dopočítej jeho přeponu a z ní sin α a cos α.`,
        `Угол не нужен. Уклон ${m} : ${nn} — это треугольник с катетами ${m} и ${nn}: найди его гипотенузу, а по ней sin α и cos α.`,
      ),
      L(
        `přepona = √(${m}² + ${nn}²) = ${c}; cos α = ${nn}/${c}, sin α = ${m}/${c} → [[F_x]] = ${n(f)} · ${nn}/${c}; [[F_y]] = ${n(f)} · ${m}/${c}`,
        `гипотенуза = √(${m}² + ${nn}²) = ${c}; cos α = ${nn}/${c}, sin α = ${m}/${c} → [[F_x]] = ${n(f)} · ${nn}/${c}; [[F_y]] = ${n(f)} · ${m}/${c}`,
      ),
    ],
    traps: [
      Traps.SWAP.with(L(`Složky jsou prohozené: vodorovná složka patří k vodorovné délce (${nn}), svislá k výšce (${m}).`, `Составляющие перепутаны: горизонтальная относится к горизонтальной длине (${nn}), вертикальная — к высоте (${m}).`)).at(fy, fx),
      Traps.TGRATIO.at((f * nn) / m, (f * m) / nn),
      Traps.FULL.where([f, f], (v) => v.some((x) => x != null && close2(Math.abs(x), f))),
    ],
    ok: L(`Správně: [[F_x]] ${approx(fx)} kN, [[F_y]] ${approx(fy)} kN.`, `Верно: [[F_x]] ${approx(fx)} кН, [[F_y]] ${approx(fy)} кН.`),
  });
});

/** E21 – rameno a moment šikmé síly na páce. */
export const E21 = exerciseType('E21', L('Rameno a moment', 'Плечо и момент'), Cards.ARM, (rng, level, seed) => {
  const r = level === 1 ? rng.int(1, 4) : rng.int(2, 8) / 2;
  const f = level === 1 ? rng.pick([5, 10, 20]) : rng.int(2, 20);
  const a = level === 1 ? rng.pick([30, 90]) : level === 2 ? rng.pick([30, 45, 60]) : rng.pick([20, 40, 50, 70]);
  const ccw = level < 3 || rng.chance(0.5);
  const signed = level === 3;
  const arm = r * Fn.deg('sin', a);
  const m = f * arm * (signed && !ccw ? -1 : 1);
  const traps: Trap[] = [
    Traps.ARM_COS.at(r * Fn.deg('cos', a), null),
    Traps.ARM_FULL.at(null, signed && !ccw ? -f * r : f * r),
    Traps.DIV.with(L('Rameno nemůže být delší než páka – r násob sinem, neděl.', 'Плечо не может быть длиннее рычага — r умножай на синус, а не дели.')).at(r / Fn.deg('sin', a), null),
  ];
  if (signed) traps.push(new TrapText('SIGNM', L('Velikost sedí. Kterým směrem síla kolem O otáčí – ↺ (+), nebo ↻ (−)?', 'Величина верна. В какую сторону сила вращает вокруг O — ↺ (+) или ↻ (−)?'), Cards.ARM).at(null, -m));
  return exercise('E21', seed, level, {
    prompt: L(
      `Na konci páky dlouhé ${n(r)} m působí síla ${n(f)} kN pod úhlem ${deg(a)} k páce. Jak velké je rameno a moment k bodu O?` + (signed ? ' Moment se znaménkem (↺ kladně).' : ''),
      `На конце рычага длиной ${n(r)} м действует сила ${n(f)} кН под углом ${deg(a)} к рычагу. Каковы плечо и момент относительно точки O?` + (signed ? ' Момент со знаком (↺ положительный).' : ''),
    ),
    fig: Fig.lever(r, a, f, ccw),
    fields: [field(L('[[r_⊥]]', '[[r_⊥]]'), arm, 'm'), field(L('M', 'M'), m, 'kNm', { signed })],
    hints: [
      L(
        'Rameno je kolmá vzdálenost od bodu O k přímce, po které síla působí: [[r_⊥]] = r · sin α. Moment M = F · [[r_⊥]].' + (signed ? ' Znaménko: otáčí síla ↺ (+), nebo ↻ (−)?' : ''),
        'Плечо — расстояние по перпендикуляру от оси до линии действия силы: [[r_⊥]] = r · sin α. Момент M = F · [[r_⊥]].' + (signed ? ' Знак: сила вращает ↺ (+) или ↻ (−)?' : ''),
      ),
      L(`[[r_⊥]] = ${n(r)} · sin ${deg(a)} = ${n(arm, 3)} m; M = ${n(f)} · ${n(arm, 3)}`, `[[r_⊥]] = ${n(r)} · sin ${deg(a)} = ${n(arm, 3)} м; M = ${n(f)} · ${n(arm, 3)}`),
    ],
    traps,
    ok: L(`Správně: [[r_⊥]] ${approx(arm, 3)} m, M ${approx(m)} kNm.`, `Верно: [[r_⊥]] ${approx(arm, 3)} м, M ${approx(m)} кН·м.`),
    solvedFig: Fig.lever(r, a, f, ccw, true),
  });
});

/** E22 – součet vodorovných a svislých složek dvou sil (se znaménky). */
export const E22 = exerciseType(
  'E22',
  L('Součet složek', 'Сумма составляющих'),
  Cards.SUM,
  (rng, level, seed) => {
    const f1 = rng.pick([10, 20]);
    const a1 = rng.pick([30, 45, 60]);
    const q1 = rng.int(0, 3);
    const phi1 = [a1, 180 - a1, 180 + a1, 360 - a1][q1];
    const f2 = rng.int(2, 12);
    let phi2: number, a2: number;
    if (level === 2) {
      phi2 = rng.pick([0, 90, 180, 270]);
      a2 = 0;
    } else {
      a2 = rng.pick([30, 45, 60]);
      phi2 = [a2, 180 - a2, 180 + a2, 360 - a2][rng.int(0, 3)];
    }
    const x1 = f1 * Fn.deg('cos', phi1);
    const y1 = f1 * Fn.deg('sin', phi1);
    const x2 = f2 * Fn.deg('cos', phi2);
    const y2 = f2 * Fn.deg('sin', phi2);
    const sx = x1 + x2;
    const sy = y1 + y2;
    const dirText = (phi: number): L =>
      phi === 0
        ? L('vodorovně doprava', 'горизонтально вправо')
        : phi === 90
          ? L('svisle nahoru', 'вертикально вверх')
          : phi === 180
            ? L('vodorovně doleva', 'горизонтально влево')
            : phi === 270
              ? L('svisle dolů', 'вертикально вниз')
              : phi < 90
                ? L('šikmo doprava nahoru', 'наклонно вправо вверх')
                : phi < 180
                  ? L('šikmo doleva nahoru', 'наклонно влево вверх')
                  : phi < 270
                    ? L('šikmo doleva dolů', 'наклонно влево вниз')
                    : L('šikmo doprava dolů', 'наклонно вправо вниз');
    const angleText = (a: number): L => (a === 0 ? L('', '') : L(` pod úhlem ${deg(a)} k vodorovné`, ` под углом ${deg(a)} к горизонтали`));
    const sgn = (v: number) => (v < 0 ? -1 : 1);
    const term = (f: number, a: number, comp: number, fn: string): string | null => (Math.abs(comp) < 1e-9 ? null : a === 0 ? n(Math.abs(f)) : `${n(f)} · ${fn} ${deg(a)}`);
    const sum = (t1: string | null, c1: number, t2: string | null, c2: number): string => {
      const parts: [number, string][] = [];
      if (t1 !== null) parts.push([c1, t1]);
      if (t2 !== null) parts.push([c2, t2]);
      if (!parts.length) return '0';
      return parts.map(([c, t], i) => (c < 0 ? (i === 0 ? '−' : ' − ') : i === 0 ? '' : ' + ') + t).join('');
    };
    const hx = sum(term(f1, a1, x1, 'cos'), x1, term(f2, a2, x2, 'cos'), x2);
    const hy = sum(term(f1, a1, y1, 'sin'), y1, term(f2, a2, y2, 'sin'), y2);
    const traps: Trap[] = [Traps.WHOLE.at(sgn(x1) * f1 + x2, sgn(y1) * f1 + y2), Traps.WHOLE.at(sgn(x1) * f1 + x2, null), Traps.SWAP.at(sgn(x1) * Math.abs(y1) + x2, sgn(y1) * Math.abs(x1) + y2)];
    if (x2 !== 0) traps.push(Traps.SIGN_X.with(L('Zkontroluj znaménko vodorovné složky [[F_2]]: doprava +, doleva −.', 'Проверь знак горизонтальной составляющей [[F_2]]: вправо +, влево −.')).at(x1 - x2, null));
    if (y2 !== 0) traps.push(Traps.SIGN_Y.with(L('Zkontroluj znaménko svislé složky [[F_2]]: nahoru +, dolů −.', 'Проверь знак вертикальной составляющей [[F_2]]: вверх +, вниз −.')).at(null, y1 - y2));
    traps.push(Traps.SIGN_X.with(L('Zkontroluj znaménko vodorovné složky [[F_1]]: doprava +, doleva −.', 'Проверь знак горизонтальной составляющей [[F_1]]: вправо +, влево −.')).at(-x1 + x2, null));
    traps.push(Traps.SIGN_Y.with(L('Zkontroluj znaménko svislé složky [[F_1]]: nahoru +, dolů −.', 'Проверь знак вертикальной составляющей [[F_1]]: вверх +, вниз −.')).at(null, -y1 + y2));
    return exercise('E22', seed, level, {
      prompt: L(
        `Na bod působí [[F_1]] = ${n(f1)} kN ${dirText(phi1).cs}${angleText(a1).cs} a [[F_2]] = ${n(f2)} kN ${dirText(phi2).cs}${angleText(a2).cs}. Urči Σ[[F_x]] a Σ[[F_y]] se znaménky.`,
        `На точку действуют [[F_1]] = ${n(f1)} кН ${dirText(phi1).ru}${angleText(a1).ru} и [[F_2]] = ${n(f2)} кН ${dirText(phi2).ru}${angleText(a2).ru}. Найди Σ[[F_x]] и Σ[[F_y]] со знаками.`,
      ),
      fig: Fig.sum([Fig.force(f1, phi1, a1, { label: 'F_1' }), Fig.force(f2, phi2, a2, { label: 'F_2' })]),
      fields: [
        field(L('Σ[[F_x]]', 'Σ[[F_x]]'), sx, 'kN', { signed: true, scale: Math.abs(x1) + Math.abs(x2) }),
        field(L('Σ[[F_y]]', 'Σ[[F_y]]'), sy, 'kN', { signed: true, scale: Math.abs(y1) + Math.abs(y2) }),
      ],
      hints: [
        L(
          'Rozlož každou šikmou sílu na složky (u úhlu cos, naproti sin) a sečti zvlášť vodorovné a zvlášť svislé – doprava a nahoru +.',
          'Разложи каждую наклонную силу на составляющие (у угла cos, напротив sin) и сложи отдельно горизонтальные и вертикальные — вправо и вверх +.',
        ),
        L(`Σ[[F_x]] = ${hx}; Σ[[F_y]] = ${hy}`, `Σ[[F_x]] = ${hx}; Σ[[F_y]] = ${hy}`),
      ],
      traps,
      ok: L(`Správně: Σ[[F_x]] ${approx(sx)} kN, Σ[[F_y]] ${approx(sy)} kN.`, `Верно: Σ[[F_x]] ${approx(sx)} кН, Σ[[F_y]] ${approx(sy)} кН.`),
      solvedFig: Fig.sum([Fig.force(f1, phi1, a1, { label: 'F_1', components: true }), Fig.force(f2, phi2, a2, { label: 'F_2' })]),
    });
  },
  [2, 3],
);

/** E23 – šikmý nosník (krokev): půdorys L · cos α a výška L · sin α. */
export const E23 = exerciseType('E23', L('Šikmý nosník', 'Наклонная балка'), Cards.RAFTER, (rng, level, seed) => {
  const len = level === 1 ? rng.int(2, 8) : rng.int(6, 16) / 2;
  const a = level === 1 ? rng.pick([30, 45, 60]) : level === 2 ? rng.int(5, 9) * 5 : rng.int(20, 60);
  const px = len * Fn.deg('cos', a);
  const h = len * Fn.deg('sin', a);
  const traps: Trap[] = [Traps.SWAP.at(h, px), Traps.DIV.at(len / Fn.deg('cos', a), len / Fn.deg('sin', a))];
  const rx = len * Fn.rad('cos', a);
  const ry = len * Fn.rad('sin', a);
  if (Math.abs(rx) > 0.05 && Math.abs(ry) > 0.05) traps.push(Traps.RAD.at(rx, ry));
  return exercise('E23', seed, level, {
    prompt: L(
      `Krokev dlouhá ${n(len)} m má sklon ${deg(a)}. Jaká je její půdorysná délka a o kolik je horní konec výš?` + (level === 1 ? given(a).cs : ''),
      `Стропило длиной ${n(len)} м наклонено под углом ${deg(a)}. Какова его горизонтальная проекция и на сколько выше верхний конец?` + (level === 1 ? given(a).ru : ''),
    ),
    fig: Fig.rafter(len, a),
    fields: [field(L('půdorys', 'проекция'), px, 'm'), field(L('výška', 'высота'), h, 'm')],
    hints: [L('Krokev je přepona: půdorys leží u úhlu (cos), výška naproti (sin).', 'Стропило — гипотенуза: проекция лежит у угла (cos), высота напротив (sin).'), L(`${n(len)} · cos ${deg(a)}; ${n(len)} · sin ${deg(a)}`, `${n(len)} · cos ${deg(a)}; ${n(len)} · sin ${deg(a)}`)],
    traps,
    ok: L(`Správně: půdorys ${approx(px)} m, výška ${approx(h)} m.`, `Верно: проекция ${approx(px)} м, высота ${approx(h)} м.`),
  });
});
