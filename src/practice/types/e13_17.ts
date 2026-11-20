/* Typy úloh E13–E17 (shodné s aplikací – practice/types/E13.kt … E17.kt). */
import type { Opt, Trap } from '../exercise';
import type { AngleFrom } from '../fig';
import { Cards, Fig, Fn, L, PHONE_CALC, TrapText, Traps, Trig, approx, close2, deg, exercise, exerciseType, field, fmt, given, n, nearRel, opt, ratioOrRad } from './common';

/** E13 – úhel ze sklonu zadaného poměrem výška : vodorovná délka. */
export const E13 = exerciseType('E13', L('Sklon poměrem', 'Уклон отношением'), Cards.SLOPE_RATIO, (rng, level, seed) => {
  const [m, nn] = rng.pick<[number, number]>(
    level === 1
      ? [
          [1, 1],
          [1, 2],
          [3, 4],
          [4, 3],
        ]
      : level === 2
        ? [
            [1, 3],
            [1, 4],
            [1, 5],
            [2, 3],
            [5, 12],
            [1, 10],
          ]
        : [
            [1, 1.5],
            [1, 2.5],
            [1, 20],
            [2, 5],
            [3, 8],
          ],
  );
  const q = m / nn;
  const ans = Fn.inv('tg', q);
  const r = `${n(m)} : ${n(nn)}`;
  const traps: Trap[] = [
    Traps.COMPL.with(L('To je úhel od svislice – vyjde, když se dělí délka výškou. Nahoře ve zlomku je výška.', 'Это угол от вертикали — так выходит, если делить длину на высоту. Сверху в дроби стоит высота.')).at(90 - ans),
    ratioOrRad(q, ans, 'tg').where([q], (v) => nearRel(v[0], q)),
    Traps.PCT_DEG.with(L(`${n(100 * q, 1)} je sklon v procentech, ne úhel. Úhel = arctg ${n(q, 3)}.`, `${n(100 * q, 1)} — это уклон в процентах, а не угол. Угол = arctg ${n(q, 3)}.`)).at(100 * q),
  ];
  if (q <= 1) traps.push(Traps.WRONGINV.with(L('Poměr výška : délka je tangens, ne sinus – použij tan⁻¹.', 'Отношение высота : длина — это тангенс, а не синус: нужен tan⁻¹.')).at(Fn.inv('sin', q)));
  traps.push(Traps.RAD.where([(ans * Math.PI) / 180], (v) => nearRel(v[0], (ans * Math.PI) / 180)));
  return exercise('E13', seed, level, {
    prompt: L(`Sklon je ${r} (výška : vodorovná délka). Jaký úhel svírá s vodorovnou? (na 1 desetinné místo)`, `Уклон ${r} (высота : горизонтальная длина). Какой угол он составляет с горизонталью? (до 1 знака после запятой)`),
    fig: Fig.slope(m, nn, n(m), n(nn), 'α = ?'),
    fields: [field(L('α', 'α'), ans, '°', { tol: 'angle', digits: 1 })],
    hints: [
      L(
        'Poměr výška : vodorovná délka je přímo **tg α**. Úhel pak dá arctg – na kalkulačce tan⁻¹: SHIFT (2nd, INV) a tan.' + PHONE_CALC.cs,
        'Отношение высота : горизонтальная длина — это и есть **tg α**. Угол даёт arctg — на калькуляторе tan⁻¹: SHIFT (2nd, INV) и tan.' + PHONE_CALC.ru,
      ),
      L(`α = arctg (${n(m)} / ${n(nn)}) = arctg ${n(q, 3)}`, `α = arctg (${n(m)} / ${n(nn)}) = arctg ${n(q, 3)}`),
    ],
    traps,
    ok: L(`Správně: α = arctg ${n(q, 3)} ≈ ${n(ans, 1)}°.`, `Верно: α = arctg ${n(q, 3)} ≈ ${n(ans, 1)}°.`),
  });
});

const slopeSubject = (p: number): [string, string] => (p <= 15 ? ['Silnice', 'Уклон дороги'] : p <= 40 ? ['Svah', 'Уклон откоса'] : ['Střecha', 'Уклон крыши']);

/** E14 – sklon v procentech ↔ úhel. */
export const E14 = exerciseType('E14', L('Sklon v procentech', 'Уклон в процентах'), Cards.SLOPE_PCT, (rng, level, seed) => {
  const toAngle = level === 1 || rng.chance(0.5);
  if (toAngle) {
    const p = rng.pick(level < 3 ? [5, 8, 10, 12, 15, 20, 25, 50, 100] : [2, 6, 30, 75, 150]);
    const q = p / 100;
    const ans = Fn.inv('tg', q);
    const [cs, ru] = slopeSubject(p);
    return exercise('E14', seed, level, {
      prompt: L(`${cs} má sklon ${n(p)} %. Jaký úhel svírá s vodorovnou? (na 1 desetinné místo)`, `${ru} ${n(p)} %. Какому углу к горизонтали это соответствует? (до 1 знака после запятой)`),
      fig: Fig.slope(p, 100, `${n(p)} m`, '100 m', 'α = ?'),
      fields: [field(L('α', 'α'), ans, '°', { tol: 'angle', digits: 1 })],
      hints: [
        L(
          'Sklon v % = výška na 100 m vodorovně = tg α · 100. Procenta převeď na číslo (vyděl 100) a použij tan⁻¹ (SHIFT/2nd/INV a tan).' + PHONE_CALC.cs,
          'Уклон в % = высота на 100 м по горизонтали = tg α · 100. Переведи проценты в число (раздели на 100) и примени tan⁻¹ (SHIFT/2nd/INV и tan).' + PHONE_CALC.ru,
        ),
        L(`α = arctg ${n(q, 3)}`, `α = arctg ${n(q, 3)}`),
      ],
      traps: [Traps.PCT_DEG.at(p), Traps.PCT_90.at(0.9 * p), Traps.PCT_100.at(Fn.inv('tg', p)), Traps.RAD.where([(ans * Math.PI) / 180], (v) => nearRel(v[0], (ans * Math.PI) / 180))],
      ok: L(`Správně: tg α = ${n(q, 3)}, α ≈ ${n(ans, 1)}°.`, `Верно: tg α = ${n(q, 3)}, α ≈ ${n(ans, 1)}°.`),
    });
  }
  const a = rng.pick(level === 2 ? [5, 10, 15, 20, 30, 45] : [3, 8, 35, 60]);
  const ans = Fn.deg('tg', a) * 100;
  return exercise('E14', seed, level, {
    prompt:
      a <= 20
        ? L(`Rampa stoupá pod úhlem ${deg(a)}. Jaký je její sklon v procentech? (na 1 desetinné místo)`, `Пандус поднимается под углом ${deg(a)}. Каков его уклон в процентах? (до 1 знака после запятой)`)
        : L(`Střecha svírá s vodorovnou úhel ${deg(a)}. Jaký je její sklon v procentech? (na 1 desetinné místo)`, `Скат крыши составляет с горизонталью угол ${deg(a)}. Каков его уклон в процентах? (до 1 знака после запятой)`),
    fig: Fig.slope(Fn.deg('tg', a), 1, null, null, deg(a)),
    fields: [field(L('sklon', 'уклон'), ans, '%', { tol: 'percent', digits: 1 })],
    hints: [
      L('Sklon v % = tg α · 100 (výška na 100 m vodorovné délky).', 'Уклон в % = tg α · 100 (высота на 100 м горизонтальной длины).'),
      L(`tg ${deg(a)} · 100 = ${n(Fn.deg('tg', a), 3)} · 100`, `tg ${deg(a)} · 100 = ${n(Fn.deg('tg', a), 3)} · 100`),
    ],
    traps: [
      Traps.PCT_DEG.at(a),
      new TrapText('SINPCT', L('Sklon se měří k **vodorovné** délce – to je tangens, ne sinus.', 'Уклон измеряют относительно **горизонтальной** длины — это тангенс, а не синус.'), Cards.SLOPE_PCT).at(Fn.deg('sin', a) * 100),
      Traps.PCT_90.at(a / 0.9),
    ],
    ok: L(`Správně: tg ${deg(a)} · 100 ≈ ${n(ans, 1)} %.`, `Верно: tg ${deg(a)} · 100 ≈ ${n(ans, 1)} %.`),
  });
});

/** E15 – převýšení (a délka po sklonu) z procent a vodorovné délky. */
export const E15 = exerciseType('E15', L('Převýšení ze sklonu', 'Превышение по уклону'), Cards.SLOPE_PCT, (rng, level, seed) => {
  const two = level >= 2;
  const p = rng.pick(two ? [20, 25, 30, 40, 50, 60, 75] : [5, 8, 10, 12, 15, 20]);
  const d = rng.pick(two ? [2, 3, 4, 5, 6, 8] : [2, 4, 5, 6, 10, 12, 20]);
  const h = (d * p) / 100;
  const s = Math.sqrt(d * d + h * h);
  const traps: Trap[] = [Traps.PCT_100.at(d * p)];
  if (two) traps.push(Traps.SUM.at(null, d + h));
  return exercise('E15', seed, level, {
    prompt: L(
      two ? `Svah má sklon ${n(p)} % a vodorovnou délku ${n(d)} m. O kolik stoupá a jak dlouhý je po šikmé ploše?` : `Rampa má sklon ${n(p)} % a vodorovnou délku ${n(d)} m. O kolik vystoupá?`,
      two ? `Уклон откоса ${n(p)} %, горизонтальная длина ${n(d)} м. На сколько он поднимается и какова его длина по склону?` : `Уклон пандуса ${n(p)} %, горизонтальная длина ${n(d)} м. На сколько он поднимается?`,
    ),
    fig: Fig.slope(h, d, '?', `${n(d)} m`, `${n(p)} %`),
    fields: [field(L('převýšení', 'превышение'), h, 'm', { tol: 'rel', digits: 3 }), ...(two ? [field(L('šikmá délka', 'длина по склону'), s, 'm', { tol: 'rel', digits: 3 })] : [])],
    hints: [
      L(
        `${n(p)} % znamená ${n(p)} m výšky na 100 m délky: převýšení = délka · ${n(p)}/100.` + (two ? ' Šikmá délka je přepona: √(d² + h²).' : ''),
        `${n(p)} % означает ${n(p)} м высоты на 100 м длины: превышение = длина · ${n(p)}/100.` + (two ? ' Длина по склону — гипотенуза: √(d² + h²).' : ''),
      ),
      L(`h = ${n(d)} · ${n(p / 100, 2)}` + (two ? `; s = √(${n(d)}² + h²)` : ''), `h = ${n(d)} · ${n(p / 100, 2)}` + (two ? `; s = √(${n(d)}² + h²)` : '')),
    ],
    traps,
    ok: L(`Správně: převýšení ${approx(h, 3)} m` + (two ? `, šikmá délka ${approx(s, 3)} m.` : '.'), `Верно: превышение ${approx(h, 3)} м` + (two ? `, длина по склону ${approx(s, 3)} м.` : '.')),
  });
});

/** E16 – znaménka složek podle směru šipky (volba ze čtyř). */
export const E16 = exerciseType('E16', L('Znaménka složek', 'Знаки составляющих'), Cards.SIGNS, (rng, level, seed) => {
  const q = rng.int(0, 3);
  const a = level === 1 ? rng.pick([30, 45, 60]) : rng.int(2, 16) * 5;
  const phi = [a, 180 - a, 180 + a, 360 - a][q];
  const sx = q === 0 || q === 3;
  const sy = q === 0 || q === 1;
  const lab = (x: boolean, y: boolean): L => {
    const t = `[[F_x]] ${x ? '+' : '−'}, [[F_y]] ${y ? '+' : '−'}`;
    return L(t, t);
  };
  const combos: [boolean, boolean][] = [
    [true, true],
    [true, false],
    [false, true],
    [false, false],
  ];
  const fig = level === 3 ? Fig.force(10, phi, phi, { from: 'plusX' }) : Fig.force(10, phi, a);
  const choice: Opt[] = combos.map(([x, y]) =>
    opt(
      lab(x, y),
      x === sx && y === sy,
      x === sx && y === sy ? null : x !== sx && y !== sy ? L('Obě znaménka jsou obráceně. Doprava a nahoru je +, doleva a dolů −.', 'Оба знака наоборот. Вправо и вверх +, влево и вниз −.') : x !== sx ? Traps.SIGN_X.msg : Traps.SIGN_Y.msg,
    ),
  );
  return exercise('E16', seed, level, {
    prompt:
      level === 3
        ? L(
            `Směr síly určuje úhel φ = ${deg(phi)}, měřený od kladné poloosy x proti směru hodinových ručiček. Jaká znaménka mají její složky?`,
            `Направление силы задано углом φ = ${deg(phi)}, отсчитанным от положительной полуоси x против часовой стрелки. Какие знаки у её составляющих?`,
          )
        : L('Jaká znaménka mají složky síly na obrázku?', 'Какие знаки у составляющих силы на рисунке?'),
    fig,
    choice,
    hints: [
      L('Doprava +, doleva −, nahoru +, dolů −. Na velikosti úhlu nezáleží.', 'Вправо +, влево −, вверх +, вниз −. Величина угла не важна.'),
      L(`Hrot míří ${sx ? 'doprava' : 'doleva'} a ${sy ? 'nahoru' : 'dolů'}.`, `Остриё смотрит ${sx ? 'вправо' : 'влево'} и ${sy ? 'вверх' : 'вниз'}.`),
    ],
    ok: L(`Správně: ${lab(sx, sy).cs}.`, `Верно: ${lab(sx, sy).ru}.`),
    solvedFig: { ...fig, components: true },
  });
});

function componentsType(id: string, signedFromStart: boolean) {
  return exerciseType(id, L('Složky síly', 'Составляющие силы'), [107, 2], (rng, level, seed) => {
    const f = level === 1 ? rng.pick([10, 20, 40]) : rng.int(2, 15) * 2;
    const quadrant = rng.int(0, 3);
    const mode: AngleFrom = level < 3 ? 'horizontal' : rng.pick<AngleFrom>(['vertical', 'plusX']);
    let a: number, shown: number, phi: number;
    if (mode === 'horizontal') {
      a = level === 1 ? rng.pick([30, 45, 60]) : rng.int(3, 15) * 5;
      shown = a;
      phi = [a, 180 - a, 180 + a, 360 - a][quadrant];
    } else if (mode === 'vertical') {
      const b = rng.int(2, 16) * 5;
      shown = b;
      a = 90 - b;
      phi = [a, 180 - a, 180 + a, 360 - a][quadrant];
    } else {
      phi = rng.pick([120, 135, 150, 210, 225, 240, 300, 315, 330]);
      shown = phi;
      const r = phi % 180;
      a = r > 90 ? 180 - r : r;
    }
    const fx = f * Trig.cosD(phi);
    const fy = f * Trig.sinD(phi);
    const sx = fx < 0 ? -1 : 1;
    const sy = fy < 0 ? -1 : 1;
    const signed = level >= 2 || signedFromStart;
    const fs = fmt(f);
    const dirs = [L('šikmo nahoru doprava', 'наклонно вверх вправо'), L('šikmo nahoru doleva', 'наклонно вверх влево'), L('šikmo dolů doleva', 'наклонно вниз влево'), L('šikmo dolů doprava', 'наклонно вниз вправо')];
    const dir = dirs[fx >= 0 && fy >= 0 ? 0 : fx < 0 && fy >= 0 ? 1 : fx < 0 ? 2 : 3];
    const prompt =
      mode === 'horizontal'
        ? L(
            `Síla [[F]] = ${fs} kN míří ${dir.cs} a s vodorovnou svírá ${fmt(shown, 0)}°. ` + (signed ? 'Urči složky i se znaménky.' : 'Urči **velikosti** složek (bez znamének).') + (level === 1 ? given(a).cs : ''),
            `Сила [[F]] = ${fs} кН направлена ${dir.ru} и составляет с горизонталью ${fmt(shown, 0)}°. ` + (signed ? 'Найди составляющие со знаками.' : 'Найди **величины** составляющих (без знаков).') + (level === 1 ? given(a).ru : ''),
          )
        : mode === 'vertical'
          ? L(
              `Síla [[F]] = ${fs} kN míří ${dir.cs} a se **svislicí** svírá ${fmt(shown, 0)}°. Urči složky i se znaménky.`,
              `Сила [[F]] = ${fs} кН направлена ${dir.ru} и составляет с **вертикалью** ${fmt(shown, 0)}°. Найди составляющие со знаками.`,
            )
          : L(
              `Síla [[F]] = ${fs} kN. Její směr určuje úhel φ = ${fmt(shown, 0)}°, měřený od kladné poloosy x proti směru hodinových ručiček. Urči složky i se znaménky.`,
              `Сила [[F]] = ${fs} кН. Её направление задано углом φ = ${fmt(shown, 0)}°, отсчитанным от положительной полуоси x против часовой стрелки. Найди составляющие со знаками.`,
            );
    const fig = Fig.force(f, phi, shown, { from: mode });
    const sgn = (s: number) => (!signed ? '' : s < 0 ? '−' : '+');
    const sinA = Trig.sinD(a);
    const cosA = Trig.cosD(a);
    const traps: Trap[] = [];
    if (mode === 'vertical') traps.push(Traps.VERT.at(sx * f * sinA, sy * f * cosA));
    traps.push(Traps.SWAP.at(sx * f * sinA, sy * f * cosA));
    if (signed) {
      traps.push(Traps.SIGN_X.where([-fx, fy], (v) => v[0] != null && v[1] != null && close2(v[0], -fx) && close2(v[1], fy)));
      traps.push(Traps.SIGN_Y.where([fx, -fy], (v) => v[0] != null && v[1] != null && close2(v[0], fx) && close2(v[1], -fy)));
      traps.push(
        Traps.SIGN_X.with(L('Velikosti sedí, ale obě znaménka jsou obráceně. Doprava a nahoru +, doleva a dolů −.', 'Величины верны, но оба знака наоборот. Вправо и вверх +, влево и вниз −.')).where(
          [-fx, -fy],
          (v) => v[0] != null && v[1] != null && close2(v[0], -fx) && close2(v[1], -fy),
        ),
      );
    }
    traps.push(Traps.FULL.where([f, f], (v) => v.some((x) => x != null && close2(Math.abs(x), f))));
    if (mode === 'plusX') traps.push(Traps.RAD.at(f * Trig.cosRad(phi), f * Trig.sinRad(phi)));
    else traps.push(Traps.RAD.at(sx * f * Trig.cosRad(a), sy * f * Trig.sinRad(a)));
    const vx = signed ? fx : Math.abs(fx);
    const vy = signed ? fy : Math.abs(fy);
    return exercise(id, seed, level, {
      prompt,
      fig,
      fields: [field(L('[[F_x]]', '[[F_x]]'), vx, 'kN', { signed }), field(L('[[F_y]]', '[[F_y]]'), vy, 'kN', { signed })],
      hints: [
        mode === 'horizontal'
          ? L('Složka u úhlu = F · cos α, složka naproti = F · sin α.' + (signed ? ' Znaménko podle šipky: doprava a nahoru +.' : ''), 'Составляющая у угла = F · cos α, напротив — F · sin α.' + (signed ? ' Знак — по стрелке: вправо и вверх +.' : ''))
          : mode === 'vertical'
            ? L(
                `Úhel β = ${fmt(shown, 0)}° leží u **svislice**: u úhlu je svislá složka → [[F_y]] = F · cos β, naproti vodorovná → [[F_x]] = F · sin β. Znaménka podle šipky.`,
                `Угол β = ${fmt(shown, 0)}° лежит у **вертикали**: у угла — вертикальная составляющая → [[F_y]] = F · cos β, напротив — горизонтальная → [[F_x]] = F · sin β. Знаки — по стрелке.`,
              )
            : L(
                'Nejdřív najdi ostrý úhel k vodorovné: rozdíl φ od nejbližší vodorovné poloosy (180° nebo 360°, u II. kvadrantu 180° − φ). Pak se podívej, kam šipka míří.',
                'Сначала найди острый угол к горизонтали: разность φ и ближайшей горизонтальной полуоси (180° или 360°, во II четверти 180° − φ). Потом посмотри, куда направлена стрелка.',
              ),
        mode === 'vertical'
          ? L(`[[F_x]] = ${sgn(sx)}${fs} · sin ${fmt(shown, 0)}°, [[F_y]] = ${sgn(sy)}${fs} · cos ${fmt(shown, 0)}°`, `[[F_x]] = ${sgn(sx)}${fs} · sin ${fmt(shown, 0)}°, [[F_y]] = ${sgn(sy)}${fs} · cos ${fmt(shown, 0)}°`)
          : L(`[[F_x]] = ${sgn(sx)}${fs} · cos ${fmt(a, 0)}°, [[F_y]] = ${sgn(sy)}${fs} · sin ${fmt(a, 0)}°`, `[[F_x]] = ${sgn(sx)}${fs} · cos ${fmt(a, 0)}°, [[F_y]] = ${sgn(sy)}${fs} · sin ${fmt(a, 0)}°`),
      ],
      traps,
      generic: L('Složka u úhlu = F · cos α, složka naproti = F · sin α; znaménko podle toho, kam míří šipka.', 'Составляющая у угла = F · cos α, напротив — F · sin α; знак — по направлению стрелки.'),
      ok: L(`Správně: [[F_x]] ${approx(vx)} kN, [[F_y]] ${approx(vy)} kN.`, `Верно: [[F_x]] ${approx(vx)} кН, [[F_y]] ${approx(vy)} кН.`),
      solvedFig: { ...fig, components: true },
    });
  });
}

export const E17 = componentsType('E17', false);
export const E17S = componentsType('E17S', true);
