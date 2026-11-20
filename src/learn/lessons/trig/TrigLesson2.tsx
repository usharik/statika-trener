/*
 * Kurz trigonometrie – lekce 2 (id 102): sinus, kosinus, tangens jako podíly stran; odvěsna = přepona · sin/cos.
 * (Přepis z aplikace: learn/trig/TrigLesson2.kt.)
 */
import {
  ActionButton,
  CardText,
  Chips,
  L,
  LinearEasing,
  Note,
  Readout,
  SmallText,
  Stage,
  Steps,
  ValueSlider,
  centroid,
  clamp,
  dist,
  num,
  numSpec,
  numTrap,
  opt,
  pt,
  screenAngle,
  step,
  trapNear,
  triGeom,
  triSide,
  tween,
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
import { BTN_CHECK, Ramp, rampTri, useGuessStep } from './shared';

const SIDE_NAME: Record<TriSide, L> = { hyp: L('přepona', 'гипотенуза'), adj: L('přilehlá', 'прилежащий'), opp: L('protilehlá', 'противолежащий') };

/** (čitatel, jmenovatel) pro sin, cos, tg */
const PARTS: [TriSide, TriSide][] = [
  ['opp', 'hyp'],
  ['adj', 'hyp'],
  ['opp', 'adj'],
];
const FN_NAMES = ['sin α', 'cos α', 'tg α'];

/** Zlomek „fn = čitatel / jmenovatel“ vpravo nahoře ve scéně; null = otazník. */
function fraction(b: PrimBuilder, fn: string, top: string | null, bottom: string | null, c: Colors, topColor: string, bottomColor: string, cx = 338, y = 62) {
  b.text(cx - 50, y + 13, `${fn} =`, c.ink, 16, 'end', null, true);
  b.text(cx, y, top ?? '?', top === null ? c.inkSoft : topColor, 13, 'middle', null, true);
  b.line(cx - 46, y + 8, cx + 46, y + 8, c.ink, 1.4);
  b.text(cx, y + 28, bottom ?? '?', bottom === null ? c.inkSoft : bottomColor, 13, 'middle', null, true);
}

/** Karta 0: tři podíly stran, tři jména. Geometrie stojí, mění se jen to, který podíl sledujeme. */
function ThreeRatios() {
  const t = useTr();
  const kit = useKit();
  const alpha = 35;
  const demo = kit.anim(0);
  const demoDone = kit.mut(false);
  useLaunched([], async (job) => {
    await job.delay(700);
    await job.run(demo.animateTo(1, tween(900)));
    await job.delay(500);
    await job.run(demo.animateTo(2, tween(900)));
    await job.delay(300);
    demoDone.v = true;
  });
  const r = new Ramp(60, 200, 190, alpha);
  const opp = 10 * Trig.sinD(alpha);
  const adj = 10 * Trig.cosD(alpha);
  const cosQ = useQuizStep(
    L('Který podíl je **cos α**?', 'Какое отношение — **cos α**?'),
    [
      opt(L('přilehlá / přepona', 'прилежащий катет / гипотенуза'), true),
      opt(L('protilehlá / přepona', 'противолежащий катет / гипотенуза'), false, L('To je sinus. Kosinus bere tu druhou odvěsnu – tu, která z vrcholu α vychází.', 'Это синус. Косинус берёт другой катет — тот, что выходит из вершины α.')),
      opt(L('protilehlá / přilehlá', 'противолежащий катет / прилежащий катет'), false, L('Tady se dělí odvěsna odvěsnou – to bude tangens. Sinus i kosinus dělí přeponou.', 'Здесь катет делится на катет — это будет тангенс. И синус, и косинус делят на гипотенузу.')),
    ],
    L(`cos α = přilehlá / přepona = ${num(adj)} / 10 ≈ ${num(adj / 10, 3)}.`, `cos α = прилежащий катет / гипотенуза = ${num(adj)} / 10 ≈ ${num(adj / 10, 3)}.`),
  );
  const tgQ = useQuizStep(
    L('Který podíl obou odvěsen je **tg α**?', 'Какое отношение катетов — **tg α**?'),
    [
      opt(L('protilehlá / přilehlá', 'противолежащий катет / прилежащий катет'), true),
      opt(L('přilehlá / protilehlá', 'прилежащий катет / противолежащий катет'), false, L('Obrácený zlomek. Nahoře je, stejně jako u sinu, odvěsna naproti úhlu α.', 'Перевёрнутая дробь. Сверху, как и у синуса, катет напротив угла α.')),
      opt(L('protilehlá / přepona', 'противолежащий катет / гипотенуза'), false, L('To je sinus. Tangens dělí odvěsnu odvěsnou – přepona v něm není.', 'Это синус. Тангенс делит катет на катет — гипотенузы в нём нет.')),
    ],
    L(
      `tg α = protilehlá / přilehlá = ${num(opp)} / ${num(adj)} ≈ ${num(opp / adj, 3)}. Shrnutí: sin α = protilehlá / přepona; cos α = přilehlá / přepona; tg α = protilehlá / přilehlá. Na kalkulačce se tangens jmenuje **tan**.`,
      `tg α = противолежащий / прилежащий = ${num(opp)} / ${num(adj)} ≈ ${num(opp / adj, 3)}. Итого: sin α = противолежащий катет / гипотенуза; cos α = прилежащий катет / гипотенуза; tg α = противолежащий катет / прилежащий катет. На калькуляторе тангенс называется **tan**.`,
    ),
  );
  // který podíl ukazuje obrázek a jestli už prozrazený
  const fn = !demoDone.v ? 0 : !cosQ.done ? 1 : 2;
  const shown = fn === 0 ? true : fn === 1 ? false : tgQ.done;
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Pravoúhlý trojúhelník s přeponou 10 dílků a úhlem 35°', 'Прямоугольный треугольник с гипотенузой 10 делений и углом 35°'))}
        draw={(b, c) => {
          b.line(20, 200, 290, 200, c.line, 1.0);
          rampTri(b, r, c, '35°', 1, true, t.pick(L('přepona 10', 'гипотенуза 10')), num(opp));
          b.text(r.c.x + 8, (r.c.y + r.b.y) / 2 + 22, t.pick(L('protilehlá', 'противолеж.')), c.derived, 12, 'start');
          b.text((r.a.x + r.c.x) / 2, r.a.y + 20, t.pick(L(`přilehlá ${num(adj)}`, `прилежащий ${num(adj)}`)), c.derived, 13);
          const side = (s: TriSide) => (s === 'adj' ? [r.a, r.c] : s === 'opp' ? [r.c, r.b] : [r.a, r.b]);
          const [n, d] = PARTS[fn];
          const pTop = fn === 0 ? clamp(demo.value, 0, 1) : shown ? 1 : 0;
          const pBot = fn === 0 ? clamp(demo.value - 1, 0, 1) : shown ? 1 : 0;
          if (pTop > 0) {
            const [p, q] = side(n);
            b.line(p.x, p.y, q.x, q.y, c.highlight, 5.0, false, pTop);
          }
          if (pBot > 0) {
            const [p, q] = side(d);
            b.line(p.x, p.y, q.x, q.y, c.accent, 5.0, false, pBot);
          }
          fraction(b, FN_NAMES[fn], pTop > 0.5 ? t.pick(SIDE_NAME[n]) : null, pBot > 0.5 ? t.pick(SIDE_NAME[d]) : null, c, c.highlight, c.accent);
          if (pTop > 0.5 || pBot > 0.5) b.text(392, 130, t.pick(L('žlutě nahoře, zeleně dole', 'жёлтое сверху, зелёное снизу')), c.inkSoft, 11, 'end');
        }}
      />
      <CardText text={L('Sinus, kosinus a tangens jsou podíly stran pravoúhlého trojúhelníku. Podíl nemá jednotku.', 'Синус, косинус и тангенс — отношения сторон прямоугольного треугольника. У отношения нет единицы измерения.')} />
      <Steps
        steps={[
          step(
            L('Sleduj, které strany tvoří sinus', 'Проследи, какие стороны образуют синус'),
            demoDone.v,
            () => <SmallText text={L('Nahoru do zlomku jde odvěsna naproti úhlu α, dolů přepona…', 'Вверх в дробь идёт катет напротив угла α, вниз — гипотенуза…')} />,
            () => <Note tone="info" text={L(`sin α = protilehlá / přepona = ${num(opp)} / 10 ≈ ${num(opp / 10, 3)}.`, `sin α = противолежащий катет / гипотенуза = ${num(opp)} / 10 ≈ ${num(opp / 10, 3)}.`)} />,
          ),
          cosQ,
          tgQ,
        ]}
      />
    </>
  );
}

/** Strany trojúhelníku ABC (A = α, C = pravý úhel) v pořadí voleb AB, AC, BC. */
const ABC: TriSide[] = ['hyp', 'adj', 'opp'];
const ABC_NAMES = ['AB', 'AC', 'BC'];

/** Řádek doku „Nahoře / Dole“ s volbou strany AB / AC / BC. */
function SlotRow({ label, selected, onPick }: { label: L; selected: number | null; onPick: (i: number) => void }) {
  const t = useTr();
  return (
    <div className="slot-row">
      <span className="slot-label">{t.pick(label)}</span>
      <Chips options={ABC_NAMES.map((x) => L(x, x))} selected={selected} onPick={onPick} />
    </div>
  );
}

/** Karta 1: slož zlomek sám – strany podle písmen, trojúhelník se každé kolo otočí. */
function BuildFraction() {
  const t = useTr();
  const kit = useKit();
  const round = kit.mut(0); // 0 sin, 1 cos, 2 tg, 3 hotovo
  const top = kit.mut<number | null>(null); // index v ABC
  const bottom = kit.mut<number | null>(null);
  const msg = kit.mut<L | null>(null);
  const rr = Math.min(round.v, 2);
  const g =
    rr === 0
      ? triGeom(4, 2.8, { fit: [20, 30, 195, 160] })
      : rr === 1
        ? triGeom(4, 2.8, { rot: 1, mirror: true, fit: [30, 25, 180, 175] })
        : triGeom(4, 2.8, { rot: 2, fit: [20, 30, 195, 160] });
  const ends = (i: number) => triSide(g, ABC[i]);
  const check = () => {
    if (round.v > 2) return;
    const tp = top.v;
    if (tp === null) return;
    const bt = bottom.v;
    if (bt === null) {
      msg.v = L('Vyber i jmenovatel (dole).', 'Выбери и знаменатель (снизу).');
      return;
    }
    const [n, d] = PARTS[round.v];
    const sn = ABC[tp];
    const sd = ABC[bt];
    if (sn === n && sd === d) {
      round.v++;
      top.v = null;
      bottom.v = null;
      msg.v = null;
      return;
    }
    msg.v =
      tp === bt
        ? L('Nahoře i dole máš stejnou stranu – to je vždy 1.', 'Сверху и снизу одна и та же сторона — это всегда 1.')
        : round.v < 2 && sn === d && sd === n
          ? L('Obráceně: přepona je nejdelší, patří dolů – jinak by vyšlo víc než 1.', 'Наоборот: гипотенуза самая длинная, она стоит снизу — иначе получится больше 1.')
          : round.v < 2 && sd !== 'hyp'
            ? L('Sinus i kosinus dělí přeponou – stranou naproti pravému úhlu u C.', 'И синус, и косинус делят на гипотенузу — сторону напротив прямого угла C.')
            : round.v === 0
              ? L('AC vychází z vrcholu A, je přilehlá. Sinus bere odvěsnu **naproti** úhlu α.', 'AC выходит из вершины A, он прилежащий. Синус берёт катет **напротив** угла α.')
              : round.v === 1
                ? L('BC leží naproti α. Kosinus bere odvěsnu, která z vrcholu α **vychází**.', 'BC лежит напротив α. Косинус берёт катет, который **выходит** из вершины α.')
                : sn === 'hyp' || sd === 'hyp'
                  ? L('V tangentu přepona není – jen obě odvěsny.', 'В тангенсе гипотенузы нет — только два катета.')
                  : L('Obráceně: nahoře je, stejně jako u sinu, odvěsna naproti α.', 'Наоборот: сверху, как и у синуса, катет напротив α.');
  };
  const tap = (i: number) => {
    if (round.v > 2) return;
    msg.v = null;
    if (top.v === null) top.v = i;
    else if (bottom.v === null) bottom.v = i;
    else {
      top.v = i;
      bottom.v = null;
    }
  };
  const pickers = () => (
    <>
      <SlotRow
        label={L('Nahoře', 'Сверху')}
        selected={top.v}
        onPick={(i) => {
          top.v = i;
          msg.v = null;
        }}
      />
      <SlotRow
        label={L('Dole', 'Снизу')}
        selected={bottom.v}
        onPick={(i) => {
          bottom.v = i;
          msg.v = null;
        }}
      />
      <ActionButton text={BTN_CHECK} enabled={top.v !== null && bottom.v !== null} onClick={check} />
      {msg.v && <Note tone="nudge" text={msg.v} />}
    </>
  );
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Pravoúhlý trojúhelník ABC s úhlem α u A a pravým úhlem u C', 'Прямоугольный треугольник ABC с углом α при A и прямым углом при C'))}
        onTap={(p) => {
          let best = -1;
          let bd = Infinity;
          for (let i = 0; i <= 2; i++) {
            const [a, b] = ends(i);
            const d = dist(p, (a.x + b.x) / 2, (a.y + b.y) / 2);
            if (d < bd) {
              bd = d;
              best = i;
            }
          }
          if (bd < 40) tap(best);
        }}
        draw={(b, c) => {
          b.rightTriangle(g, {}, null, 'α', c.ink, c.ink);
          if (top.v !== null) {
            const [p, q] = ends(top.v);
            b.line(p.x, p.y, q.x, q.y, c.highlight, 5.0);
          }
          if (bottom.v !== null) {
            const [p, q] = ends(bottom.v);
            b.line(p.x, p.y, q.x, q.y, c.accent, 5.0);
          }
          // písmena vrcholů, odsunutá od těžiště
          const cen = centroid(g);
          for (const [p0, name] of [
            [g.a, 'A'],
            [g.b, 'B'],
            [g.c, 'C'],
          ] as const) {
            const dx = p0.x - cen.x;
            const dy = p0.y - cen.y;
            const d = Math.max(Math.hypot(dx, dy), 1e-9);
            b.text(p0.x + (dx / d) * 16, p0.y + (dy / d) * 16 + 5, name, c.ink, 15, 'middle', null, true);
          }
          for (let i = 0; i <= 2; i++) {
            const [a, bb] = ends(i);
            b.tapSpot((a.x + bb.x) / 2, (a.y + bb.y) / 2, i === top.v || i === bottom.v, 11);
          }
          if (round.v <= 2) fraction(b, FN_NAMES[round.v], top.v !== null ? ABC_NAMES[top.v] : null, bottom.v !== null ? ABC_NAMES[bottom.v] : null, c, c.highlight, c.accent, 344, 90);
        }}
      />
      <CardText
        text={L(
          'Pořadí stran ve zlomku rozhoduje – obrácený podíl obvykle dává jiné číslo. Úhel α je u vrcholu A, pravý úhel u C.',
          'Порядок сторон в дроби важен — обратное отношение обычно даёт другое число. Угол α — при вершине A, прямой угол — при C.',
        )}
      />
      <Steps
        steps={[
          step(L('Slož zlomek pro sin α', 'Составь дробь для sin α'), round.v > 0, pickers, () => (
            <Note tone="ok" text={L('sin α = BC / AB = protilehlá / přepona', 'sin α = BC / AB = противолежащий катет / гипотенуза')} />
          )),
          step(L('Trojúhelník se otočil. Slož zlomek pro cos α', 'Треугольник повернулся. Составь дробь для cos α'), round.v > 1, pickers, () => (
            <Note tone="ok" text={L('cos α = AC / AB = přilehlá / přepona', 'cos α = AC / AB = прилежащий катет / гипотенуза')} />
          )),
          step(L('A znovu jinak natočený: slož zlomek pro tg α', 'И снова повёрнут иначе: составь дробь для tg α'), round.v > 2, pickers, () => (
            <>
              <Note tone="ok" text={L('tg α = BC / AC = protilehlá / přilehlá', 'tg α = BC / AC = противолежащий катет / прилежащий катет')} />
              <SmallText
                text={L(
                  'Sinus a kosinus porovnávají odvěsnu s přeponou. Tangens porovnává protilehlou odvěsnu s přilehlou. Na natočení obrázku nezáleží.',
                  'Синус и косинус сравнивают катет с гипотенузой. Тангенс сравнивает противолежащий катет с прилежащим. Как повёрнут рисунок, неважно.',
                )}
              />
            </>
          )),
        ]}
      />
    </>
  );
}

/** Karta 2: kvíz – sin α v trojúhelníku 3-4-5. Strany neutrální, ať barva neprozradí přeponu. */
function Check345() {
  const t = useTr();
  const q = useQuizStep(
    L('Kolik je sin α?', 'Чему равен sin α?'),
    [
      opt(L('3/5 = 0,6', '3/5 = 0,6'), true),
      opt(L('4/5 = 0,8', '4/5 = 0,8'), false, L('4 je odvěsna u úhlu α. Která funkce bere přilehlou odvěsnu?', '4 — катет у угла α. Какая функция берёт прилежащий катет?')),
      opt(L('3/4 = 0,75', '3/4 = 0,75'), false, L('Tady se dělí odvěsna odvěsnou – to je tangens. Čím dělí sinus?', 'Здесь катет делится на катет — это тангенс. На что делит синус?')),
      opt(
        L('5/3 ≈ 1,67', '5/3 ≈ 1,67'),
        false,
        L('Přepona je nejdelší strana, takže sinus nikdy nevyjde větší než 1. Co patří ve zlomku nahoru?', 'Гипотенуза — самая длинная сторона, поэтому синус никогда не больше 1. Что в дроби должно быть сверху?'),
      ),
    ],
    L('Protilehlá odvěsna má délku 3 a přepona 5. Proto sin α = 3/5 = 0,6.', 'Противолежащий катет имеет длину 3, гипотенуза — 5. Поэтому sin α = 3/5 = 0,6.'),
    null,
    2,
  );
  return (
    <>
      <Stage
        h={200}
        description={t.pick(L('Trojúhelník 3-4-5 na čtverečkované síti', 'Треугольник 3-4-5 на клетчатой сетке'))}
        draw={(b, c) => {
          const s = 40;
          const ox = 110;
          const oy = 30;
          b.grid(ox, oy, s, 4, 3);
          const a = pt(ox, oy + 3 * s);
          const cc = pt(ox + 4 * s, oy + 3 * s);
          const bb = pt(ox + 4 * s, oy);
          b.poly(
            [
              [a.x, a.y],
              [bb.x, bb.y],
              [cc.x, cc.y],
            ],
            c.subtle,
            null,
            0,
            0.5,
          );
          b.line(a.x, a.y, cc.x, cc.y, c.ink, 2.6);
          b.line(cc.x, cc.y, bb.x, bb.y, c.ink, 2.6);
          b.line(a.x, a.y, bb.x, bb.y, c.ink, 2.6);
          b.rightAngleMark(cc, a, bb);
          b.angleArc(a.x, a.y, 30, 0, screenAngle(a, bb), 'α');
          b.text((a.x + cc.x) / 2, a.y + 22, '4', c.ink, 15, 'middle', null, true);
          b.text(cc.x + 14, (cc.y + bb.y) / 2 + 5, '3', c.ink, 15, 'start', null, true);
          b.text((a.x + bb.x) / 2 - 14, (a.y + bb.y) / 2 - 12, '5', c.ink, 15, 'end', null, true);
        }}
      />
      <CardText text={L('K výpočtu poměru stran nemusíme znát úhel ve stupních.', 'Чтобы вычислить отношение сторон, не обязательно знать угол в градусах.')} />
      <Steps steps={[q]} />
    </>
  );
}

/** Karta 3: průměty úsečky délky 1 – sin roste od 0 do 1, cos klesá od 1 do 0. */
function Range() {
  const t = useTr();
  const kit = useKit();
  const alpha = kit.mut(20);
  const guess = kit.mut<number | null>(null);
  const swept = kit.mut(false);
  const found = kit.mut(false);
  const msg = kit.mut<L | null>(null);
  const sweep = kit.anim(0);
  const sweeping = kit.mut(false);
  useLaunched([guess.v], async (job) => {
    if (guess.v === null || swept.v) return;
    await job.delay(400);
    await sweep.snapTo(0);
    alpha.v = 0;
    sweeping.v = true;
    await job.delay(500);
    await job.run(sweep.animateTo(90, tween(3600, LinearEasing)));
    sweeping.v = false;
    alpha.v = 90;
    await job.delay(400);
    swept.v = true;
    alpha.v = 20;
  });
  const al = sweeping.v ? sweep.value : alpha.v;
  const s = Trig.sinD(Math.round(al * 1000) / 1000);
  const co = Trig.cosD(Math.round(al * 1000) / 1000);
  const proj = [L('Vodorovný', 'Горизонтальная'), L('Svislý', 'Вертикальная'), L('Oba', 'Обе')];
  const g = useGuessStep(L('Tipni si: který průmět roste, když úsečku zvedáš k 90°?', 'Предскажи: какая проекция растёт, когда отрезок поднимается к 90°?'), proj, guess.v, (i) => (guess.v = i));
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Úsečka délky 1 a její vodorovný a svislý průmět', 'Отрезок длины 1 и его горизонтальная и вертикальная проекции'))}
        draw={(b, c) => {
          const a = pt(40, 205);
          const len = 170;
          const bb = pt(a.x + len * co, a.y - len * s);
          const cc = pt(bb.x, a.y);
          b.line(20, 205, 250, 205, c.line, 1.0);
          if (s > 0.01 && co > 0.01)
            b.poly(
              [
                [a.x, a.y],
                [bb.x, bb.y],
                [cc.x, cc.y],
              ],
              c.subtle,
              null,
              0,
              0.6,
            );
          if (s > 0.01) b.line(cc.x, cc.y, bb.x, bb.y, c.derived, 3.0, co > 0.01);
          if (co > 0.01) b.line(a.x, a.y, cc.x, cc.y, c.react, 3.0);
          b.line(a.x, a.y, bb.x, bb.y, c.load, 2.6);
          if (s > 0.1 && co > 0.1) b.rightAngleMark(cc, a, bb);
          b.angleArc(a.x, a.y, 28, 0, -al, `${Math.round(al)}°`);
          b.text((a.x + bb.x) / 2 - 12 * s - 6, (a.y + bb.y) / 2 - 12 * co, '1', c.load, 14, 'end', null, true);
          // sloupce: svislý průmět (sin) a vodorovný průmět (cos)
          b.bar(290, 30, 32, 160, s, c.derived);
          b.bar(345, 30, 32, 160, co, c.react);
          b.text(306, 210, 'sin', c.derived, 13, 'middle', null, true);
          b.text(361, 210, 'cos', c.react, 13, 'middle', null, true);
          if (guess.v !== null) {
            b.text(306, 24, num(s, 2), c.derived, 12);
            b.text(361, 24, num(co, 2), c.react, 12);
          }
          b.text(286, 36, '1', c.inkSoft, 11, 'end');
          b.text(286, 194, '0', c.inkSoft, 11, 'end');
        }}
      />
      <CardText text={L('Úsečka délky 1 má vodorovný průmět **cos α** a svislý průmět **sin α**.', 'У отрезка длины 1 горизонтальная проекция равна **cos α**, а вертикальная — **sin α**.')} />
      <Steps
        steps={[
          g,
          step(
            L('Sleduj oba průměty od 0° do 90°', 'Проследи за обеими проекциями от 0° до 90°'),
            swept.v,
            () => (
              <>
                <SmallText text={L('Úsečka se zvedá od 0° k 90°…', 'Отрезок поднимается от 0° до 90°…')} />
                <Readout text={`sin = ${num(s, 3)} · cos = ${num(co, 3)}`} />
              </>
            ),
            () => (
              <Note
                tone={guess.v === 1 ? 'ok' : 'info'}
                text={L(
                  (guess.v === 1 ? 'Tip sedí: ' : 'Je to jinak: ') + 'svislý průmět (sin) roste z 0 na 1, vodorovný (cos) klesá z 1 na 0. Při 0° a 90° už to není trojúhelník – jen úsečka a její průmět.',
                  (guess.v === 1 ? 'Прогноз верен: ' : 'На самом деле иначе: ') +
                    'вертикальная проекция (sin) растёт от 0 до 1, горизонтальная (cos) убывает от 1 до 0. При 0° и 90° это уже не треугольник — только отрезок и его проекция.',
                )}
              />
            ),
          ),
          step(
            L('Najdi úhel, při kterém jsou oba průměty stejné', 'Найди угол, при котором обе проекции равны'),
            found.v,
            () => (
              <>
                <ValueSlider
                  label={t.pick(L(`Úhel α = ${Math.round(alpha.v)}°`, `Угол α = ${Math.round(alpha.v)}°`))}
                  value={alpha.v}
                  min={0}
                  max={90}
                  steps={89}
                  onChange={(it) => {
                    alpha.v = clamp(Math.round(it), 0, 90);
                    msg.v = null;
                  }}
                />
                <Readout text={`sin = ${num(s, 3)} · cos = ${num(co, 3)}`} />
                <ActionButton
                  text={BTN_CHECK}
                  onClick={() => {
                    if (alpha.v === 45) {
                      found.v = true;
                      msg.v = null;
                    } else msg.v = s < co ? L('Svislý průmět je ještě kratší – zvedni úsečku výš.', 'Вертикальная проекция ещё короче — подними отрезок выше.') : L('Svislý průmět je už delší – sklop úsečku níž.', 'Вертикальная проекция уже длиннее — опусти отрезок ниже.');
                  }}
                />
                {msg.v && <Note tone="nudge" text={msg.v} />}
              </>
            ),
            () => (
              <Note
                tone="ok"
                text={L(
                  'Od 0° do 90° sinus roste z 0 na 1 a kosinus klesá z 1 na 0. Při 45° jsou oba √2/2 ≈ 0,707.',
                  'От 0° до 90° синус растёт от 0 до 1, а косинус убывает от 1 до 0. При 45° оба равны √2/2 ≈ 0,707.',
                )}
              />
            ),
          ),
        ]}
      />
    </>
  );
}

/** Karta 4: neznámou odvěsnu vyjádříme z definice sinu – h = přepona · sin α (žebřík). */
function Ladder() {
  const t = useTr();
  const expr = useQuizStep(
    L('Kterým výrazem spočítáš výšku h horního konce?', 'Каким выражением найти высоту h верхнего конца?'),
    [
      opt(L('5 · sin 60°', '5 · sin 60°'), true),
      opt(
        L('5 · cos 60°', '5 · cos 60°'),
        false,
        L(
          'Kosinus patří k odvěsně **u** úhlu 60° – to je vzdálenost paty žebříku od zdi. Výška leží naproti.',
          'Косинус относится к катету **у** угла 60° — это расстояние от низа лестницы до стены. Высота лежит напротив.',
        ),
      ),
      opt(
        L('5 / sin 60°', '5 / sin 60°'),
        false,
        L('Ze sin 60° = h / 5 dostaneš h vynásobením pěti, ne dělením. Výška musí vyjít menší než 5 m.', 'Из sin 60° = h / 5 высота получается умножением на 5, а не делением. Высота должна быть меньше 5 м.'),
      ),
    ],
    L(
      'Výška leží naproti úhlu 60°, takže sin 60° = h / 5. Obě strany vynásobíme pěti: h = 5 · sin 60°. Obecně: protilehlá = přepona · sin α, přilehlá = přepona · cos α.',
      'Высота лежит напротив угла 60°, поэтому sin 60° = h / 5. Умножаем обе части на 5: h = 5 · sin 60°. В общем виде: противолежащий катет = гипотенуза · sin α, прилежащий = гипотенуза · cos α.',
    ),
  );
  const calc = useNumStep(
    L('Vypočítej h na dvě desetinná místa. sin 60° ≈ 0,866', 'Вычисли h до двух знаков после запятой. sin 60° ≈ 0,866'),
    [numSpec(L('h', 'h'), 5 * Trig.sinD(60), 'm')],
    L('h = 5 · sin 60° ≈ 5 · 0,866 ≈ 4,33 m. Výška je menší než délka žebříku – jak má být.', 'h = 5 · sin 60° ≈ 5 · 0,866 ≈ 4,33 м. Высота меньше длины лестницы — как и должно быть.'),
    [
      trapNear(L('To je 5 · cos 60° – vzdálenost paty žebříku od zdi, odvěsna **u** úhlu. Výška leží naproti.', 'Это 5 · cos 60° — расстояние от низа лестницы до стены, катет **у** угла. Высота лежит напротив.'), 2.5),
      numTrap(L('Výška nemůže být větší než žebřík. Přeponu sinem násob, neděl.', 'Высота не может быть больше лестницы. Гипотенузу на синус умножай, а не дели.'), (v) => v[0] != null && Math.abs(v[0]) > 5),
    ],
    L('Dosaď do vybraného výrazu: 5 · 0,866 = ?', 'Подставь в выбранное выражение: 5 · 0,866 = ?'),
  );
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Žebřík dlouhý 5 m opřený o zeď pod úhlem 60°', 'Лестница длиной 5 м, прислонённая к стене под углом 60°'))}
        draw={(b, c) => {
          const wallX = 300;
          const ground = 205;
          const len = 170;
          const foot = pt(wallX - len * Trig.cosD(60), ground);
          const top = pt(wallX, ground - len * Trig.sinD(60));
          b.rect(wallX, 15, 22, ground - 15, c.subtle, c.ink, 1.4);
          b.line(20, ground, 380, ground, c.ink, 1.6);
          // žebřík: štěpina a příčky
          b.line(foot.x, foot.y, top.x, top.y, c.load, 3.0);
          for (let i = 1; i <= 6; i++) {
            const k = i / 7;
            const x = foot.x + (top.x - foot.x) * k;
            const y = foot.y + (top.y - foot.y) * k;
            b.line(x - 7, y - 4, x + 7, y + 4, c.load, 1.6);
          }
          b.angleArc(foot.x, foot.y, 30, 0, -60, '60°');
          b.text((foot.x + top.x) / 2 - 16, (foot.y + top.y) / 2, '5 m', c.load, 14, 'end', null, true);
          b.line(wallX - 6, ground, wallX - 6, top.y, c.derived, 1.4, true);
          b.text(wallX + 30, (ground + top.y) / 2 + 5, calc.done ? 'h ≈ 4,33 m' : 'h = ?', c.derived, 15, 'start', null, true);
        }}
      />
      <CardText text={L('Žebřík dlouhý 5 m svírá se zemí 60°. Výška jeho horního konce je jedna z odvěsen.', 'Лестница длиной 5 м составляет с землёй угол 60°. Высота её верхнего конца — один из катетов.')} />
      <Steps steps={[expr, calc]} />
    </>
  );
}

export const cards = [ThreeRatios, BuildFraction, Check345, Range, Ladder];
