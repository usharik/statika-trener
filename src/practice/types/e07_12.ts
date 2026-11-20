/* Typy úloh E07–E12 (shodné s aplikací – practice/types/E07.kt … E12.kt). */
import type { Trap } from '../exercise';
import type { TriSide } from '../fig';
import { Cards, Fig, FNS, Fn, L, PHONE_CALC, TRIPLES_EASY, TRIPLES_MORE, TrapText, Traps, approx, exerciseType, exercise, field, n, nearRel, opt, ratioOrRad, type FnName } from './common';

const key = (fn: FnName) => (fn === 'tg' ? 'tan' : fn);

/** E07 – hodnota funkce na kalkulačce (pozor na režim DEG). */
export const E07 = exerciseType('E07', L('Funkce na kalkulačce', 'Функция на калькуляторе'), Cards.CALC_MODE, (rng, level, seed) => {
  const fn = rng.pick(FNS);
  const special = [30, 45, 60];
  let a: number;
  do a = level === 1 ? rng.int(1, 17) * 5 : rng.int(1, 89);
  while (special.includes(a) || (fn === 'tg' && a > 80));
  const ans = Fn.deg(fn, a);
  const traps: Trap[] = [Traps.RAD.at(Fn.rad(fn, a)), Traps.GRAD.at(Fn.grad(fn, a))];
  for (const g of FNS)
    if (g !== fn) traps.push(new TrapText('OTHERFN', L(`To je ${g} ${a}° – jiná funkce. Zkontroluj, která klávesa byla stisknutá.`, `Это ${g} ${a}° — другая функция. Проверь, какая клавиша нажата.`), Cards.THREE_RATIOS).at(Fn.deg(g, a)));
  return exercise('E07', seed, level, {
    prompt: L(`Spočítej na kalkulačce ${fn} ${a}° (na 3 desetinná místa).`, `Посчитай на калькуляторе ${fn} ${a}° (до 3 знаков после запятой).`),
    fig: Fig.calc(`${key(fn)}(${a})`, '?', 'DEG'),
    fields: [field(L(`${fn} ${a}°`, `${fn} ${a}°`), ans, '', { tol: 'ratio', digits: 3 })],
    hints: [
      L(
        'Nejdřív zkontroluj režim **DEG**: sin 30 musí dát 0,5.' + (fn === 'tg' ? ' Tangens je na kalkulačce **tan**.' : '') + PHONE_CALC.cs,
        'Сначала проверь режим **DEG**: sin 30 должен дать 0,5.' + (fn === 'tg' ? ' Тангенс на калькуляторе — **tan**.' : '') + PHONE_CALC.ru,
      ),
      L(`Stiskni ${key(fn)} a ${a} (nebo ${a} a ${key(fn)} – podle kalkulačky).`, `Нажми ${key(fn)} и ${a} (или ${a} и ${key(fn)} — зависит от калькулятора).`),
    ],
    traps,
    ok: L(`Správně: ${fn} ${a}° ≈ ${n(ans, 3)}.`, `Верно: ${fn} ${a}° ≈ ${n(ans, 3)}.`),
  });
});

/** E08 – v jakém režimu je kalkulačka podle výsledku známého výrazu. */
export const E08 = exerciseType('E08', L('Režim kalkulačky', 'Режим калькулятора'), Cards.CALC_MODE, (rng, level, seed) => {
  const [fn, a] = rng.pick<[FnName, number]>([
    ['sin', 30],
    ['cos', 60],
    ['sin', 90],
    ['tg', 45],
    ['sin', 60],
  ]);
  const vals: Record<string, number> = { DEG: Fn.deg(fn, a), RAD: Fn.rad(fn, a), GRAD: Fn.grad(fn, a) };
  const mode = level === 1 ? rng.pick(['RAD', 'DEG']) : rng.pick(['DEG', 'RAD', 'GRAD']);
  const expr = `${key(fn)} ${n(a, 0)}`;
  const shown = n(vals[mode], 3);
  const why = (m: string): L =>
    m === 'DEG'
      ? L(`Ve stupních (DEG) by ${expr} dalo ${n(vals.DEG, 3)}. Na displeji je ale ${shown}.`, `В градусах (DEG) ${expr} дал бы ${n(vals.DEG, 3)}. А на дисплее ${shown}.`)
      : m === 'RAD'
        ? L(`V režimu RAD by ${expr} dalo ${n(vals.RAD, 3)}.`, `В режиме RAD ${expr} дал бы ${n(vals.RAD, 3)}.`)
        : L(`V režimu GRAD (pravý úhel = 100 gradů, gonů) by ${expr} dalo ${n(vals.GRAD, 3)}.`, `В режиме GRAD (прямой угол = 100 град, гон) ${expr} дал бы ${n(vals.GRAD, 3)}.`);
  const modes = ['DEG', 'RAD', 'GRAD'];
  return exercise('E08', seed, level, {
    prompt: L(`Kalkulačka ukázala ${expr} = ${shown}. V jakém je režimu?`, `Калькулятор показал ${expr} = ${shown}. В каком он режиме?`),
    fig: Fig.calc(expr, shown, ''),
    choice: modes.map((m) => opt(L(m, m), m === mode, m === mode ? null : why(m))),
    hints: [
      L(`Kolik má ${expr} vyjít ve stupních? Pamatuješ si to z tabulky známých úhlů.`, `Сколько должно получиться для ${expr} в градусах? Это есть в таблице известных углов.`),
      L(
        `V RAD bere kalkulačka ${n(a, 0)} jako ${n(a, 0)} radiánů (několik celých otáček); v GRAD má pravý úhel 100 gradů (gonů).`,
        `В RAD калькулятор считает ${n(a, 0)} радианами (несколько полных оборотов); в GRAD прямой угол равен 100 град (гон).`,
      ),
    ],
    ok: L(`Správně – režim ${mode}.` + (mode !== 'DEG' ? ' Před počítáním přepni na DEG.' : ''), `Верно — режим ${mode}.` + (mode !== 'DEG' ? ' Перед расчётом переключи на DEG.' : '')),
  });
});

/** u celých výsledků zkouška násobením */
function sqCheck(v: number): string {
  const r = Math.round(v);
  return Math.abs(v - r) < 1e-9 ? ` (${n(r, 0)} · ${n(r, 0)} = ${n(r * r, 0)})` : '';
}

/** E09 – Pythagorova věta. */
export const E09 = exerciseType('E09', L('Pythagorova věta', 'Теорема Пифагора'), Cards.PYTHAGORAS, (rng, level, seed) => {
  const hypMode = level === 1 || rng.chance(0.5);
  let a: number, b: number, c: number;
  if (level === 1 || level === 2) {
    const [p, q, h] = rng.pick(level === 1 ? TRIPLES_EASY : TRIPLES_MORE);
    const k = level === 1 && h !== 13 ? rng.int(1, 2) : 1;
    a = p * k;
    b = q * k;
    c = h * k;
  } else if (hypMode) {
    a = rng.int(2, 12);
    b = rng.int(2, 12);
    c = Math.sqrt(a * a + b * b);
  } else {
    c = rng.int(6, 15);
    a = rng.int(2, c - 1);
    b = Math.sqrt(c * c - a * a);
  }
  if (hypMode)
    return exercise('E09', seed, level, {
      prompt: L(`Odvěsny pravoúhlého trojúhelníku měří ${n(a)} m a ${n(b)} m. Jak dlouhá je přepona?`, `Катеты прямоугольного треугольника равны ${n(a)} м и ${n(b)} м. Какова длина гипотенузы?`),
      fig: Fig.tri(a, b, { labels: { adj: n(a), opp: n(b), hyp: '?' }, highlight: 'hyp', angle: null }),
      fields: [field(L('přepona c', 'гипотенуза c'), c, 'm')],
      hints: [
        L('a² + b² = c². Hledáš přeponu: sečti druhé mocniny odvěsen a odmocni.', 'a² + b² = c². Ищешь гипотенузу: сложи квадраты катетов и извлеки корень.'),
        L(`c = √(${n(a)}² + ${n(b)}²) = √${n(a * a + b * b)}` + sqCheck(c), `c = √(${n(a)}² + ${n(b)}²) = √${n(a * a + b * b)}` + sqCheck(c)),
      ],
      traps: [Traps.SUM.at(a + b), Traps.NOSQRT.at(a * a + b * b)],
      ok: L(`Správně: c = √${n(a * a + b * b)} ${approx(c)} m.`, `Верно: c = √${n(a * a + b * b)} ${approx(c)} м.`),
    });
  return exercise('E09', seed, level, {
    prompt: L(`Přepona měří ${n(c)} m a jedna odvěsna ${n(a)} m. Jak dlouhá je druhá odvěsna?`, `Гипотенуза равна ${n(c)} м, один катет — ${n(a)} м. Какова длина второго катета?`),
    fig: Fig.tri(a, b, { labels: { adj: n(a), opp: '?', hyp: n(c) }, highlight: 'opp', angle: null }),
    fields: [field(L('odvěsna b', 'катет b'), b, 'm')],
    hints: [
      L('a² + b² = c², takže b² = c² − a². Odvěsna je kratší než přepona – tady se **odečítá**.', 'a² + b² = c², значит b² = c² − a². Катет короче гипотенузы — здесь **вычитают**.'),
      L(`b = √(${n(c)}² − ${n(a)}²) = √${n(c * c - a * a)}` + sqCheck(b), `b = √(${n(c)}² − ${n(a)}²) = √${n(c * c - a * a)}` + sqCheck(b)),
    ],
    traps: [
      new TrapText('ADDSQ', L('Odvěsna je kratší než přepona – druhé mocniny se tady odečítají, ne sčítají.', 'Катет короче гипотенузы — квадраты здесь вычитают, а не складывают.'), Cards.PYTHAGORAS).at(Math.sqrt(c * c + a * a)),
      Traps.SUM.with(L('Strany se neodečítají přímo – odečítají se jejich druhé mocniny: b² = c² − a².', 'Стороны не вычитают напрямую — вычитают их квадраты: b² = c² − a².')).at(c - a),
      Traps.NOSQRT.at(c * c - a * a),
    ],
    ok: L(`Správně: b = √${n(c * c - a * a)} ${approx(b)} m.`, `Верно: b = √${n(c * c - a * a)} ${approx(b)} м.`),
  });
});

/** E10 – druhá funkce z první: sin² α + cos² α = 1. */
export const E10 = exerciseType('E10', L('sin² + cos² = 1', 'sin² + cos² = 1'), Cards.UNIT_HYP, (rng, level, seed) => {
  const s = rng.pick(level === 1 ? [0.6, 0.8] : level === 2 ? [0.6, 0.8, 0.28, 0.96] : [0.6, 0.8, 0.28, 0.96, 0.5, 0.385, 0.923]);
  const fromSin = rng.chance(0.5);
  const giv = fromSin ? 'sin' : 'cos';
  const wanted = fromSin ? 'cos' : 'sin';
  const ans = Math.sqrt(1 - s * s);
  const knownSide: TriSide = fromSin ? 'opp' : 'adj';
  const askSide: TriSide = fromSin ? 'adj' : 'opp';
  return exercise('E10', seed, level, {
    prompt: L(`α je ostrý úhel a ${giv} α = ${n(s, 3)}. Kolik je ${wanted} α?`, `α — острый угол, ${giv} α = ${n(s, 3)}. Чему равен ${wanted} α?`),
    fig: Fig.tri(fromSin ? ans : s, fromSin ? s : ans, { labels: { hyp: '1', [knownSide]: n(s, 3), [askSide]: '?' }, angle: 'α' }),
    fields: [field(L(`${wanted} α`, `${wanted} α`), ans, '', { tol: 'ratio', digits: 3 })],
    hints: [
      L(`sin² α + cos² α = 1, takže ${wanted}² α = 1 − ${giv}² α.`, `sin² α + cos² α = 1, значит ${wanted}² α = 1 − ${giv}² α.`),
      L(`${wanted} α = √(1 − ${n(s, 3)}²) = √${n(1 - s * s, 4)}`, `${wanted} α = √(1 − ${n(s, 3)}²) = √${n(1 - s * s, 4)}`),
    ],
    traps: [
      new TrapText('LINEAR', L(`1 − ${giv} by platilo, kdyby se sčítaly sin a cos. Sčítají se ale jejich **druhé mocniny**.`, `1 − ${giv} было бы верно, если бы складывались sin и cos. Но складываются их **квадраты**.`), Cards.UNIT_HYP).at(1 - s),
      Traps.NOSQRT.with(L(`To je teprve ${wanted}² α. Ještě odmocni.`, `Это пока ${wanted}² α. Осталось извлечь корень.`)).at(1 - s * s),
    ],
    ok: L(`Správně: ${wanted} α = √${n(1 - s * s, 4)} ${approx(ans, 3)}.`, `Верно: ${wanted} α = √${n(1 - s * s, 4)} ${approx(ans, 3)}.`),
  });
});

/** E11 – doplňkový úhel: sin α = cos (90° − α). */
export const E11 = exerciseType('E11', L('Doplňkový úhel', 'Дополнительный угол'), Cards.COMPLEMENT, (rng, level, seed) => {
  let a: number;
  do a = level === 3 ? rng.int(5, 85) : rng.int(1, 17) * 5;
  while (a === 45);
  const sinToCos = level === 1 || rng.chance(0.5);
  const [f1, f2] = sinToCos ? ['sin', 'cos'] : ['cos', 'sin'];
  const ans = 90 - a;
  return exercise('E11', seed, level, {
    prompt: L(`Doplň úhel: ${f1} ${a}° = ${f2} ?°`, `Найди угол: ${f1} ${a}° = ${f2} ?°`),
    fig: Fig.tri(Math.cos((a * Math.PI) / 180), Math.sin((a * Math.PI) / 180), { angle: `${a}°` }),
    fields: [field(L('úhel', 'угол'), ans, '°', { tol: 'angle', digits: 0 })],
    hints: [
      L(
        'Oba ostré úhly pravoúhlého trojúhelníku dávají dohromady 90°. Co je pro jeden úhel protilehlá odvěsna, je pro druhý přilehlá.',
        'Два острых угла прямоугольного треугольника вместе дают 90°. Катет, противолежащий одному углу, прилежит к другому.',
      ),
      L(`90° − ${a}°`, `90° − ${a}°`),
    ],
    traps: [
      new TrapText('SAME', L(`${f1} a ${f2} téhož úhlu se rovnají jen při 45°. Hledáš druhý ostrý úhel.`, `${f1} и ${f2} одного угла равны только при 45°. Ищем второй острый угол.`), Cards.COMPLEMENT).at(a),
      new TrapText('C180', L('Doplněk je do 90°, ne do 180°.', 'Дополнение — до 90°, а не до 180°.'), Cards.COMPLEMENT).at(180 - a),
    ],
    ok: L(`Správně: ${f1} ${a}° = ${f2} ${90 - a}°.`, `Верно: ${f1} ${a}° = ${f2} ${90 - a}°.`),
  });
});

/** E12 – úhel ze dvou stran (arcsin / arccos / arctg). */
export const E12 = exerciseType('E12', L('Úhel ze stran', 'Угол по сторонам'), Cards.WHICH_INV, (rng, level, seed) => {
  let adj: number, opp: number;
  if (level === 1) {
    const [p, q] = rng.pick(TRIPLES_EASY);
    const sw = rng.chance(0.5);
    adj = sw ? q : p;
    opp = sw ? p : q;
  } else {
    adj = rng.int(2, 15);
    opp = rng.int(2, 15);
  }
  const hyp = Math.sqrt(adj * adj + opp * opp);
  const fn: FnName = level === 1 ? 'tg' : rng.pick(FNS);
  const h = fn === 'tg' || level === 1 ? hyp : Math.max(Math.round(hyp), Math.max(adj, opp) + 1);
  const [x, y] = fn === 'sin' ? [opp, h] : fn === 'cos' ? [adj, h] : [opp, adj];
  const q = x / y;
  const ans = Fn.inv(fn, q);
  const labels: Partial<Record<TriSide, string>> =
    fn === 'sin' ? { opp: `${n(opp)} m`, hyp: `${n(h)} m` } : fn === 'cos' ? { adj: `${n(adj)} m`, hyp: `${n(h)} m` } : { opp: `${n(opp)} m`, adj: `${n(adj)} m` };
  const figAdj = Fn.deg('cos', ans);
  const figOpp = Fn.deg('sin', ans);
  const ctx = level === 3 ? rng.int(0, 2) : 3;
  const prompt =
    ctx === 0
      ? L(
          'Žebřík (přepona) a zeď tvoří pravoúhlý trojúhelník se stranami podle obrázku. Jaký úhel α svírá žebřík se zemí? (na 1 desetinné místo)',
          'Лестница (гипотенуза) и стена образуют прямоугольный треугольник со сторонами по рисунку. Какой угол α лестница составляет с землёй? (до 1 знака после запятой)',
        )
      : ctx === 1
        ? L('Lano je napnuté mezi body podle obrázku. Jaký úhel α svírá s vodorovnou? (na 1 desetinné místo)', 'Трос натянут между точками по рисунку. Какой угол α он составляет с горизонталью? (до 1 знака после запятой)')
        : ctx === 2
          ? L('Střecha má rozměry podle obrázku. Jaký úhel α svírá s vodorovnou? (na 1 desetinné místo)', 'Размеры крыши — по рисунку. Какой угол α она составляет с горизонталью? (до 1 знака после запятой)')
          : L('Známe dvě strany podle obrázku. Jak velký je úhel α? (na 1 desetinné místo)', 'Известны две стороны (по рисунку). Чему равен угол α? (до 1 знака после запятой)');
  const xn = fn === 'sin' ? L('protilehlá a přepona', 'противолежащий и гипотенуза') : fn === 'cos' ? L('přilehlá a přepona', 'прилежащий и гипотенуза') : L('obě odvěsny', 'оба катета');
  const traps: Trap[] = [
    fn === 'tg'
      ? Traps.COMPL.at(90 - ans)
      : Traps.COMPL.with(
          L(
            `To je druhý ostrý úhel. Buď je ve zlomku druhá odvěsna, nebo jiná funkce: ${fn === 'sin' ? 'protilehlá a přepona → sin⁻¹' : 'přilehlá a přepona → cos⁻¹'}.`,
            `Это второй острый угол. Либо в дроби другой катет, либо не та функция: ${fn === 'sin' ? 'противолежащий и гипотенуза → sin⁻¹' : 'прилежащий и гипотенуза → cos⁻¹'}.`,
          ),
        ).at(90 - ans),
  ];
  for (const g of FNS) if (g !== fn && (g === 'tg' || q <= 1)) traps.push(Traps.WRONGINV.at(Fn.inv(g, q)));
  traps.push(ratioOrRad(q, ans, fn).where([q], (v) => nearRel(v[0], q)));
  traps.push(Traps.FWD.where([Fn.deg(fn, q)], (v) => nearRel(v[0], Fn.deg(fn, q))));
  traps.push(Traps.RAD.where([(ans * Math.PI) / 180], (v) => nearRel(v[0], (ans * Math.PI) / 180)));
  traps.push(Traps.GRAD.at((ans * 10) / 9));
  return exercise('E12', seed, level, {
    prompt,
    fig: Fig.tri(figAdj, figOpp, { labels, angle: 'α' }),
    fields: [field(L('α', 'α'), ans, '°', { tol: 'angle', digits: 1 })],
    hints: [
      L(
        `Znáš ${xn.cs} → funkce ${fn}. Úhel = ${Fn.arc(fn)} (podíl), na kalkulačce ${Fn.calcInv(fn)}: SHIFT (2nd, INV) a ${key(fn)}.` + PHONE_CALC.cs,
        `Известны ${xn.ru} → функция ${fn}. Угол = ${Fn.arc(fn)} (отношение), на калькуляторе ${Fn.calcInv(fn)}: SHIFT (2nd, INV) и ${key(fn)}.` + PHONE_CALC.ru,
      ),
      L(`α = ${Fn.arc(fn)} (${n(x)} / ${n(y)}) = ${Fn.arc(fn)} ${n(q, 3)}`, `α = ${Fn.arc(fn)} (${n(x)} / ${n(y)}) = ${Fn.arc(fn)} ${n(q, 3)}`),
    ],
    traps,
    ok: L(`Správně: α = ${Fn.arc(fn)} ${n(q, 3)} ≈ ${n(ans, 1)}°.`, `Верно: α = ${Fn.arc(fn)} ${n(q, 3)} ≈ ${n(ans, 1)}°.`),
  });
});
