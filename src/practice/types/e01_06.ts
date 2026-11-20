/* Typy úloh E01–E06 (shodné s aplikací – practice/types/E01.kt … E06.kt). */
import type { Opt, Trap } from '../exercise';
import { TRI_SIDES, type TriSide } from '../fig';
import { Cards, Fig, FNS, Fn, L, TRIPLES_EASY, TRIPLES_MORE, TrapText, Traps, Trig, approx, deg, exercise, exerciseType, field, fmt, given, n, opt, type FnName } from './common';

/** E01 – název zvýrazněné strany vzhledem k vyznačenému úhlu. */
export const E01 = exerciseType('E01', L('Názvy stran', 'Названия сторон'), Cards.NAME_SIDES, (rng, level, seed) => {
  const [adj, opp] = rng.pick<[number, number]>([
    [3, 4],
    [4, 3],
    [5, 12],
    [12, 5],
    [2, 5],
    [4, 4],
  ]);
  const rot = level === 1 ? 0 : rng.int(0, 3);
  const mirror = rng.chance(0.5);
  const target = rng.pick(TRI_SIDES);
  const names: Record<TriSide, L> = { hyp: L('přepona', 'гипотенуза'), adj: L('přilehlá odvěsna', 'прилежащий катет'), opp: L('protilehlá odvěsna', 'противолежащий катет') };
  const why = (chosen: TriSide): L =>
    chosen === 'hyp'
      ? L('Přepona leží naproti pravému úhlu. Zvýrazněná strana ale k pravému úhlu přiléhá – je to odvěsna.', 'Гипотенуза лежит напротив прямого угла. А выделенная сторона прилегает к прямому углу — это катет.')
      : target === 'hyp'
        ? L('Zvýrazněná strana leží naproti pravému úhlu (značka ⦜) a je nejdelší. Jak se jí říká?', 'Выделенная сторона лежит напротив прямого угла (значок ⦜) и она самая длинная. Как она называется?')
        : target === 'adj'
          ? L('Zvýrazněná strana vychází přímo z vrcholu úhlu α – dotýká se ho. Je u úhlu, ne naproti.', 'Выделенная сторона выходит прямо из вершины угла α — она его касается. Она у угла, а не напротив.')
          : L('Zvýrazněná strana se vrcholu úhlu α nedotýká – leží naproti němu.', 'Выделенная сторона не касается вершины угла α — она лежит напротив него.');
  const order = rng.shuffle(TRI_SIDES);
  return exercise('E01', seed, level, {
    prompt: L('Jak se jmenuje **zvýrazněná** strana vzhledem k vyznačenému úhlu α?', 'Как называется **выделенная** сторона по отношению к отмеченному углу α?'),
    fig: Fig.tri(adj, opp, { rot, mirror, highlight: target, angle: 'α' }),
    choice: order.map((s) => opt(names[s], s === target, s === target ? null : why(s))),
    hints: [
      L('Najdi pravý úhel – naproti němu je přepona. Zbylé dvě strany jsou odvěsny.', 'Найди прямой угол — напротив него гипотенуза. Две остальные стороны — катеты.'),
      L(
        'Je zvýrazněná strana odvěsna (svírá pravý úhel)? Pak: vychází-li z vrcholu úhlu α, je přilehlá; jinak protilehlá. Strana naproti pravému úhlu je přepona.',
        'Выделенная сторона — катет (образует прямой угол)? Тогда: если она выходит из вершины угла α — прилежащий, иначе противолежащий. Сторона напротив прямого угла — гипотенуза.',
      ),
    ],
    ok: L(`Správně – je to ${names[target].cs}.`, `Верно — это ${names[target].ru}.`),
  });
});

/** 2. nápověda u skryté strany: nejdřív ji dopočítat */
function hiddenHint(hidden: TriSide | null, adj: number, opp: number, hyp: number): L {
  if (hidden === null) return L('', '');
  if (hidden === 'hyp') {
    const t = `√(${n(adj)}² + ${n(opp)}²) = √${n(adj * adj + opp * opp)} = ${n(hyp)}`;
    return L(`Chybějící přepona: ${t}. Pak `, `Недостающая гипотенуза: ${t}. Тогда `);
  }
  const t = hidden === 'adj' ? `√(${n(hyp)}² − ${n(opp)}²) = √${n(hyp * hyp - opp * opp)} = ${n(adj)}` : `√(${n(hyp)}² − ${n(adj)}²) = √${n(hyp * hyp - adj * adj)} = ${n(opp)}`;
  return L(`Chybějící odvěsna: ${t}. Pak `, `Недостающий катет: ${t}. Тогда `);
}

/** E02 – hodnota sin / cos / tg ze stran trojúhelníku. */
export const E02 = exerciseType('E02', L('Podíl stran', 'Отношение сторон'), Cards.THREE_RATIOS, (rng, level, seed) => {
  const [p, q, h] = rng.pick(level === 1 ? TRIPLES_EASY.slice(0, 2) : TRIPLES_MORE);
  const k = level === 1 ? 1 : rng.int(1, 2);
  const swap = rng.chance(0.5);
  const adj = (swap ? q : p) * k;
  const opp = (swap ? p : q) * k;
  const hyp = h * k;
  const fn: FnName = level === 1 ? rng.pick<FnName>(['sin', 'cos']) : rng.pick(FNS);
  const ans = fn === 'sin' ? opp / hyp : fn === 'cos' ? adj / hyp : opp / adj;
  const needed: TriSide[] = fn === 'sin' ? ['opp', 'hyp'] : fn === 'cos' ? ['adj', 'hyp'] : ['opp', 'adj'];
  const hidden = level === 3 ? rng.pick(needed) : null;
  const labels: Partial<Record<TriSide, string>> = {};
  if (hidden !== 'adj') labels.adj = n(adj);
  if (hidden !== 'opp') labels.opp = n(opp);
  if (hidden !== 'hyp') labels.hyp = n(hyp);
  const [num, den] =
    fn === 'sin'
      ? [L('protilehlá', 'противолежащий'), L('přepona', 'гипотенуза')]
      : fn === 'cos'
        ? [L('přilehlá', 'прилежащий'), L('přepona', 'гипотенуза')]
        : [L('protilehlá', 'противолежащий'), L('přilehlá', 'прилежащий')];
  const [a, b] = fn === 'sin' ? [opp, hyp] : fn === 'cos' ? [adj, hyp] : [opp, adj];
  const rot = level === 1 ? 0 : rng.int(0, 3);
  const mirror = rng.chance(0.5);
  const traps: Trap[] = [];
  if (fn === 'sin') traps.push(Traps.SWAP.at(adj / hyp));
  else if (fn === 'cos') traps.push(Traps.SWAP.at(opp / hyp));
  else traps.push(Traps.SWAP.with(L('Obrácený zlomek: tangens je **protilehlá / přilehlá** – nahoře odvěsna naproti úhlu.', 'Перевёрнутая дробь: тангенс — **противолежащий / прилежащий**, сверху катет напротив угла.')).at(adj / opp));
  if (fn !== 'tg') traps.push(Traps.INV.at(1 / ans));
  if (fn !== 'tg') traps.push(new TrapText('OTHERFN', L('Tady se dělí odvěsna odvěsnou – to je tangens. Sinus i kosinus dělí přeponou.', 'Здесь катет делится на катет — это тангенс. И синус, и косинус делят на гипотенузу.'), Cards.CHECK345).at(opp / adj));
  if (fn === 'tg') traps.push(new TrapText('OTHERFN', L('Dělíš přeponou – to je sinus. V tangentu přepona vůbec není.', 'Ты делишь на гипотенузу — это синус. В тангенсе гипотенузы нет.'), Cards.CHECK345).at(opp / hyp));
  const pre = hiddenHint(hidden, adj, opp, hyp);
  return exercise('E02', seed, level, {
    prompt: L(
      `Kolik je ${fn} α? Zapiš desetinným číslem (stačí 2 desetinná místa) nebo zlomkem.` + (hidden !== null ? ' Jednu stranu musíš nejdřív dopočítat.' : ''),
      `Чему равен ${fn} α? Запиши десятичным числом (достаточно 2 знаков после запятой) или дробью.` + (hidden !== null ? ' Одну сторону сначала нужно найти.' : ''),
    ),
    fig: Fig.tri(adj, opp, { rot, mirror, labels, angle: 'α' }),
    fields: [field(L(`${fn} α`, `${fn} α`), ans, '', { tol: 'ratio', digits: 3 })],
    hints: [
      L(
        `${fn} α = ${num.cs} / ${den.cs}` + (hidden !== null ? '. Chybějící stranu dopočítej: a² + b² = c².' : '') + ' Najdi ty dvě strany na obrázku.',
        `${fn} α = ${num.ru} / ${den.ru}` + (hidden !== null ? '. Недостающую сторону найди: a² + b² = c².' : '') + ' Найди эти две стороны на рисунке.',
      ),
      L(pre.cs + `${fn} α = ${n(a)} / ${n(b)}`, pre.ru + `${fn} α = ${n(a)} / ${n(b)}`),
    ],
    traps,
    ok: L(`Správně: ${fn} α = ${n(a)} / ${n(b)} ${approx(ans, 3)}.`, `Верно: ${fn} α = ${n(a)} / ${n(b)} ${approx(ans, 3)}.`),
  });
});

/** E03 – odvěsna z přepony a úhlu (žebřík, lano, rampa, trojúhelník). */
export const E03 = exerciseType('E03', L('Odvěsna z přepony', 'Катет по гипотенузе'), [102, 4], (rng, level, seed) => {
  const ctx = rng.int(0, 3);
  const c =
    ctx === 0
      ? level === 1
        ? rng.pick([4, 6, 8])
        : level === 2
          ? rng.int(3, 8)
          : rng.int(6, 16) / 2
      : level === 1
        ? rng.pick([4, 6, 8, 10, 12, 20])
        : level === 2
          ? rng.int(2, 20)
          : rng.int(4, 40) / 2;
  const a =
    ctx === 0
      ? level === 1
        ? 60
        : level === 2
          ? rng.int(10, 16) * 5
          : rng.int(50, 80)
      : ctx === 2 && level === 1
        ? rng.pick([30, 45])
        : ctx === 2
          ? level === 2
            ? rng.int(1, 4) * 5
            : rng.int(4, 20)
          : level === 1
            ? rng.pick([30, 45, 60])
            : level === 2
              ? rng.int(3, 15) * 5
              : rng.int(10, 80);
  const opposite = rng.chance(0.5);
  const s = Trig.sinD(a);
  const co = Trig.cosD(a);
  const ans = c * (opposite ? s : co);
  const other = c * (opposite ? co : s);
  const cs = fmt(c);
  const al = fmt(a, 0);
  let q: string[];
  if (ctx === 0)
    q = [
      `Žebřík dlouhý ${cs} m svírá se zemí ${al}°. ` + (opposite ? 'Jak vysoko na zdi končí?' : 'Jak daleko od zdi stojí jeho pata?'),
      `Лестница длиной ${cs} м составляет с землёй ${al}°. ` + (opposite ? 'На какой высоте она упирается в стену?' : 'На каком расстоянии от стены её нижний конец?'),
      opposite ? 'výška' : 'vzdálenost od zdi',
      opposite ? 'высота' : 'расстояние до стены',
    ];
  else if (ctx === 1)
    q = [
      `Lano dlouhé ${cs} m je napnuté pod úhlem ${al}° k vodorovné. ` + (opposite ? 'O kolik je horní konec výš než dolní?' : 'Jak daleko jsou konce od sebe vodorovně?'),
      `Трос длиной ${cs} м натянут под углом ${al}° к горизонтали. ` + (opposite ? 'На сколько верхний конец выше нижнего?' : 'Каково расстояние между концами по горизонтали?'),
      opposite ? 'převýšení' : 'vodorovná vzdálenost',
      opposite ? 'превышение' : 'расстояние по горизонтали',
    ];
  else if (ctx === 2)
    q =
      level === 1
        ? [
            `Krokev dlouhá ${cs} m má sklon ${al}°. ` + (opposite ? 'O kolik je horní konec výš než dolní?' : 'Jaká je její půdorysná délka?'),
            `Стропило длиной ${cs} м наклонено под углом ${al}°. ` + (opposite ? 'На сколько верхний конец выше нижнего?' : 'Какова его горизонтальная проекция?'),
            opposite ? 'převýšení' : 'půdorysná délka',
            opposite ? 'превышение' : 'проекция',
          ]
        : [
            `Rampa měří po šikmé ploše ${cs} m a stoupá pod úhlem ${al}°. ` + (opposite ? 'Do jaké výšky vede?' : 'Jaká je její vodorovná délka?'),
            `Пандус длиной ${cs} м по склону поднимается под углом ${al}°. ` + (opposite ? 'На какую высоту он ведёт?' : 'Какова его горизонтальная длина?'),
            opposite ? 'výška' : 'vodorovná délka',
            opposite ? 'высота' : 'горизонтальная длина',
          ];
  else
    q = [
      `Přepona měří ${cs} m, úhel α = ${al}°. Jak dlouhá je odvěsna ` + (opposite ? '**protilehlá**' : '**přilehlá**') + ' k úhlu α?',
      `Гипотенуза равна ${cs} м, угол α = ${al}°. Какова длина катета, ` + (opposite ? '**противолежащего**' : '**прилежащего**') + ' углу α?',
      'odvěsna',
      'катет',
    ];
  const giv = level === 1 ? given(a).cs : '';
  const target: TriSide = opposite ? 'opp' : 'adj';
  const fn = opposite ? 'sin' : 'cos';
  return exercise('E03', seed, level, {
    prompt: L(q[0] + giv, q[1] + giv),
    fig: Fig.tri(c * co, c * s, { labels: { hyp: `${cs} m`, [target]: '?' }, highlight: target, angle: `${al}°` }),
    fields: [field(L(q[2], q[3]), ans, 'm')],
    hints: [
      opposite
        ? L('Hledaná strana leží **naproti** úhlu → sin. Odvěsna = přepona · sin α.', 'Искомая сторона лежит **напротив** угла → sin. Катет = гипотенуза · sin α.')
        : L('Hledaná strana leží **u** úhlu → cos. Odvěsna = přepona · cos α.', 'Искомая сторона лежит **у** угла → cos. Катет = гипотенуза · cos α.'),
      L(`${cs} · ${fn} ${al}° = ${cs} · ${fmt(opposite ? s : co, 3)}`, `${cs} · ${fn} ${al}° = ${cs} · ${fmt(opposite ? s : co, 3)}`),
    ],
    traps: [
      Traps.SWAP.at(other),
      Traps.DIV.at(c / (opposite ? s : co)),
      Traps.FULL.with(L('To je délka přepony. Odvěsna je vždy kratší – vynásob ji sin nebo cos.', 'Это длина гипотенузы. Катет всегда короче — умножь её на sin или cos.')).at(c),
      Traps.RAD.at(c * (opposite ? Trig.sinRad(a) : Trig.cosRad(a))),
      Traps.GRAD.at(c * (opposite ? Trig.sinGrad(a) : Trig.cosGrad(a))),
    ],
    ok: L(`Správně: ${cs} · ${fn} ${al}° ${approx(ans)} m.`, `Верно: ${cs} · ${fn} ${al}° ${approx(ans)} м.`),
  });
});

/** E04 – přepona z odvěsny a úhlu (dělí se funkcí). */
export const E04 = exerciseType(
  'E04',
  L('Přepona z odvěsny', 'Гипотенуза по катету'),
  Cards.LADDER,
  (rng, level, seed) => {
    const a = level === 2 ? rng.int(2, 15) : rng.int(4, 30) / 2;
    const al = rng.int(4, 14) * 5;
    const opposite = rng.chance(0.5);
    const fn: FnName = opposite ? 'sin' : 'cos';
    const fv = Fn.deg(fn, al);
    const ans = a / fv;
    const adj = opposite ? a / Fn.deg('tg', al) : a;
    const opp = opposite ? a : a * Fn.deg('tg', al);
    const side: TriSide = opposite ? 'opp' : 'adj';
    const sideCs = opposite ? 'protilehlá (naproti úhlu)' : 'přilehlá (u úhlu)';
    const sideRu = opposite ? 'противолежащий (напротив угла)' : 'прилежащий (у угла)';
    const radV = Fn.rad(fn, al);
    const rot = rng.int(0, 3);
    const mirror = rng.chance(0.5);
    const traps: Trap[] = [
      Traps.MUL.at(a * fv),
      Traps.SWAP.with(L(`Dělíš jinou funkcí: známá odvěsna leží ${opposite ? 'naproti úhlu → sin' : 'u úhlu → cos'}.`, `Ты делишь не на ту функцию: известный катет лежит ${opposite ? 'напротив угла → sin' : 'у угла → cos'}.`)).at(a / Fn.deg(opposite ? 'cos' : 'sin', al)),
    ];
    if (Math.abs(radV) > 0.05) traps.push(Traps.RAD.at(a / radV));
    return exercise('E04', seed, level, {
      prompt: L(`Odvěsna ${sideCs} měří ${n(a)} m, úhel α = ${deg(al)}. Jak dlouhá je přepona?`, `Катет ${sideRu} равен ${n(a)} м, угол α = ${deg(al)}. Какова длина гипотенузы?`),
      fig: Fig.tri(adj, opp, { rot, mirror, labels: { [side]: `${n(a)} m`, hyp: '?' }, highlight: 'hyp', angle: deg(al) }),
      fields: [field(L('přepona', 'гипотенуза'), ans, 'm')],
      hints: [
        L(`${fn} α = odvěsna / přepona, takže **přepona = odvěsna / ${fn} α**.`, `${fn} α = катет / гипотенуза, значит **гипотенуза = катет / ${fn} α**.`),
        L(`${n(a)} / ${fn} ${deg(al)} = ${n(a)} / ${n(fv, 3)}`, `${n(a)} / ${fn} ${deg(al)} = ${n(a)} / ${n(fv, 3)}`),
      ],
      traps,
      ok: L(`Správně: ${n(a)} / ${fn} ${deg(al)} ${approx(ans)} m.`, `Верно: ${n(a)} / ${fn} ${deg(al)} ${approx(ans)} м.`),
    });
  },
  [2, 3],
);

/** E05 – odvěsna z druhé odvěsny a úhlu (tangens). */
export const E05 = exerciseType(
  'E05',
  L('Odvěsna z odvěsny (tangens)', 'Катет по катету (тангенс)'),
  Cards.TANGENT,
  (rng, level, seed) => {
    const b = level === 2 ? rng.int(2, 20) : rng.int(4, 40) / 2;
    const ctx = rng.int(0, 1);
    const al = ctx === 1 ? rng.int(1, 4) * 5 : rng.int(2, 14) * 5;
    const tv = Fn.deg('tg', al);
    const ans = b * tv;
    const q =
      ctx === 0
        ? L(
            `Sloup vrhá na rovné zemi stín dlouhý ${n(b)} m. Slunce svítí pod úhlem ${deg(al)} nad obzorem. Jak vysoký je sloup?`,
            `Столб отбрасывает на ровную землю тень длиной ${n(b)} м. Солнце светит под углом ${deg(al)} над горизонтом. Какой высоты столб?`,
          )
        : L(`Rampa je vodorovně dlouhá ${n(b)} m a stoupá pod úhlem ${deg(al)}. Do jaké výšky vede?`, `Пандус имеет горизонтальную длину ${n(b)} м и поднимается под углом ${deg(al)}. На какую высоту он ведёт?`);
    const radV = Fn.rad('tg', al);
    const traps: Trap[] = [
      new TrapText('DIVTG', L('Tangens = protilehlá / přilehlá. Hledáš tu nahoře – násob, neděl.', 'Тангенс = противолежащий / прилежащий. Ищешь тот, что сверху, — умножай, а не дели.'), Cards.TANGENT).at(b / tv),
      new TrapText('NOHYP', L('Sinus i kosinus potřebují přeponu – ta tu ale není. Obě známé strany jsou odvěsny, tedy tangens.', 'Синусу и косинусу нужна гипотенуза, а её здесь нет. Обе известные стороны — катеты, значит тангенс.'), Cards.TANGENT).at(b * Fn.deg('sin', al)),
    ];
    if (Math.abs(radV) >= 0.05 && Math.abs(radV) <= 200) traps.push(Traps.RAD.at(b * radV));
    return exercise('E05', seed, level, {
      prompt: q,
      fig: Fig.tri(b, ans, { labels: { adj: `${n(b)} m`, opp: '?' }, highlight: 'opp', angle: deg(al) }),
      fields: [field(L('výška', 'высота'), ans, 'm')],
      hints: [
        L('Přepona tu nehraje roli – obě strany jsou odvěsny, tedy **tangens**: tg α = protilehlá / přilehlá.', 'Гипотенуза здесь не участвует — обе стороны катеты, значит **тангенс**: tg α = противолежащий / прилежащий.'),
        L(`${n(b)} · tg ${deg(al)} = ${n(b)} · ${n(tv, 3)}`, `${n(b)} · tg ${deg(al)} = ${n(b)} · ${n(tv, 3)}`),
      ],
      traps,
      ok: L(`Správně: ${n(b)} · tg ${deg(al)} ${approx(ans)} m.`, `Верно: ${n(b)} · tg ${deg(al)} ${approx(ans)} м.`),
    });
  },
  [2, 3],
);

/** hodnoty jako značky; „X“ = neexistuje */
const VALUES = ['0', '1/2', '√2/2', '√3/2', '1', '√3/3', '√3', 'X'];
const VAL: Record<FnName, Record<number, string>> = {
  sin: { 0: '0', 30: '1/2', 45: '√2/2', 60: '√3/2', 90: '1' },
  cos: { 0: '1', 30: '√3/2', 45: '√2/2', 60: '1/2', 90: '0' },
  tg: { 0: '0', 30: '√3/3', 45: '1', 60: '√3', 90: 'X' },
};
const TEXT: Record<string, L> = {
  X: L('není definován', 'не определён'),
  '1/2': L('1/2 = 0,5', '1/2 = 0,5'),
  '√2/2': L('√2/2 ≈ 0,707', '√2/2 ≈ 0,707'),
  '√3/2': L('√3/2 ≈ 0,866', '√3/2 ≈ 0,866'),
  '√3/3': L('√3/3 ≈ 0,577', '√3/3 ≈ 0,577'),
  '√3': L('√3 ≈ 1,732', '√3 ≈ 1,732'),
};
const text = (v: string) => TEXT[v] ?? L(v, v);

/** E06 – hodnota funkce pro známý úhel bez kalkulačky – volba z možností. */
export const E06 = exerciseType('E06', L('Známé úhly', 'Известные углы'), Cards.SQRT_TABLE, (rng, level, seed) => {
  const fn: FnName = level === 1 ? rng.pick<FnName>(['sin', 'cos']) : rng.pick(FNS);
  const angles = fn === 'tg' && level < 3 ? [0, 30, 45, 60] : [0, 30, 45, 60, 90];
  const a = rng.pick(angles);
  const correct = VAL[fn][a];
  const comp = VAL[fn][90 - a];
  const pool = rng.shuffle(VALUES.filter((v) => v !== correct && v !== comp));
  const labels = [correct, ...(comp !== correct ? [comp] : []), ...pool].slice(0, 4);
  const opts: Opt[] = rng.shuffle(labels).map((v) => {
    let why: L | null;
    if (v === correct) why = null;
    else if (v === comp)
      why =
        fn !== 'tg'
          ? L(`To je hodnota pro ${90 - a}°. Tabulky sin a cos jsou navzájem pozpátku: sin ${a}° = cos ${90 - a}°.`, `Это значение для ${90 - a}°. Таблицы sin и cos зеркальны: sin ${a}° = cos ${90 - a}°.`)
          : L(`To je tg ${90 - a}°. U tangentu se hodnoty s rostoucím úhlem zvětšují: tg 0° = 0, tg 45° = 1.`, `Это tg ${90 - a}°. Тангенс с ростом угла растёт: tg 0° = 0, tg 45° = 1.`);
    else if (fn === 'tg')
      why = L(
        'Tahle hodnota patří k jinému úhlu. Tangens roste: tg 0° = 0, tg 30° ≈ 0,577, tg 45° = 1, tg 60° ≈ 1,732, tg 90° není definován.',
        'Это значение другого угла. Тангенс растёт: tg 0° = 0, tg 30° ≈ 0,577, tg 45° = 1, tg 60° ≈ 1,732, tg 90° не определён.',
      );
    else
      why = L(
        'Tahle hodnota patří k jinému úhlu. Najdi úhel v řadě 0 – 0,5 – 0,707 – 0,866 – 1 (sin pro 0°, 30°, 45°, 60°, 90°; cos pozpátku).',
        'Это значение другого угла. Найди угол в ряду 0 – 0,5 – 0,707 – 0,866 – 1 (sin для 0°, 30°, 45°, 60°, 90°; cos — в обратном порядке).',
      );
    return opt(text(v), v === correct, why);
  });
  const inner = a >= 1 && a <= 89;
  return exercise('E06', seed, level, {
    prompt: L(`Bez kalkulačky: kolik je ${fn} ${a}°?`, `Без калькулятора: чему равен ${fn} ${a}°?`),
    fig: inner ? Fig.tri(Math.cos((a * Math.PI) / 180), Math.sin((a * Math.PI) / 180), { angle: `${a}°` }) : null,
    choice: opts,
    hints: [
      fn === 'tg'
        ? L('tg α = sin α / cos α. V trojúhelníku 45°–45°–90° jsou odvěsny stejné, v 30°–60°–90° mají strany 1, √3 a 2.', 'tg α = sin α / cos α. В треугольнике 45°–45°–90° катеты равны, в 30°–60°–90° стороны 1, √3 и 2.')
        : L(
            'Řada (√0)/2, (√1)/2, (√2)/2, (√3)/2, (√4)/2 patří k 0°, 30°, 45°, 60°, 90° – pro sin zleva, pro cos zprava. √0 = 0, √1 = 1, √4 = 2.',
            'Ряд (√0)/2, (√1)/2, (√2)/2, (√3)/2, (√4)/2 соответствует 0°, 30°, 45°, 60°, 90° — для sin слева, для cos справа. √0 = 0, √1 = 1, √4 = 2.',
          ),
      a === 0 || a === 90
        ? L(
            'Při 0° trojúhelník „slehne“: protilehlá odvěsna je 0 a přilehlá splyne s přeponou. Při 90° je to obráceně.' + (fn === 'tg' && a === 90 ? ' Tangens by dělil nulou – není definován.' : ''),
            'При 0° треугольник „складывается“: противолежащий катет равен 0, а прилежащий сливается с гипотенузой. При 90° — наоборот.' + (fn === 'tg' && a === 90 ? ' Тангенс делил бы на ноль — он не определён.' : ''),
          )
        : fn === 'tg'
          ? L(
              'tg 30° = kratší odvěsna / delší = 1/√3 ≈ 0,577 (píše se i √3/3); tg 45° = 1; tg 60° = delší / kratší = √3 ≈ 1,732.',
              'tg 30° = короткий катет / длинный = 1/√3 ≈ 0,577 (пишут и √3/3); tg 45° = 1; tg 60° = длинный / короткий = √3 ≈ 1,732.',
            )
          : L('V trojúhelníku 30°–60°–90° jsou strany 1, √3, 2; v 45°–45°–90° jsou 1, 1, √2.', 'В треугольнике 30°–60°–90° стороны 1, √3, 2; в 45°–45°–90° — 1, 1, √2.'),
    ],
    ok: L(`Správně: ${fn} ${a}° = ${text(correct).cs}.`, `Верно: ${fn} ${a}° = ${text(correct).ru}.`),
  });
});
