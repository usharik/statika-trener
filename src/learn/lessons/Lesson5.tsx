/*
 * Lekce 5 – Rovnováha otáčení. Těleso v klidu se neposouvá ANI neotáčí: ΣF_x = 0, ΣF_y = 0, ΣM = 0.
 * Momentovou rovnici smíme psát k libovolnému bodu – výhodný je ten, kterým prochází nejvíc neznámých.
 * (Přepis z aplikace: learn/Lesson5.kt.)
 */
import { Rich } from '../../components/Sym';
import { NumInput } from '../../components/ui';
import { parseNum } from '../../format';
import {
  ActionButton,
  CardText,
  Chips,
  L,
  Note,
  Readout,
  SmallText,
  Stage,
  Steps,
  alpha as withAlpha,
  clamp,
  dist,
  num,
  numSpec,
  opt,
  step,
  tipStep,
  trapNear,
  tween,
  useKit,
  useLaunched,
  useNum,
  useNumStep,
  useQuiz,
  useQuizStep,
  useTr,
  type Colors,
  type Opt,
  type PrimBuilder,
  type QuizState,
  type Step,
} from '../kit';
import { turn } from './Lesson4';

/** Krabice závaží stojící na otočené houpačce v poloze [p] metrů od osy. */
function weight(b: PrimBuilder, p: number, label: string, px: number, py: number, unit: number, deg: number, c: Colors, movable: boolean, a = 1) {
  const x0 = px + p * unit;
  const w = 34;
  const h = 28;
  const pts = (
    [
      [x0 - w / 2, py - 5 - h],
      [x0 + w / 2, py - 5 - h],
      [x0 + w / 2, py - 5],
      [x0 - w / 2, py - 5],
    ] as [number, number][]
  ).map(([x, y]) => turn(x, y, px, py, deg));
  b.poly(pts, movable ? c.focusBg : c.subtle, c.load, 1.6, a);
  const [tx, ty] = turn(x0, py - 5 - h / 2 + 5, px, py, deg);
  b.text(tx, ty, label, c.load, 13, 'middle', null, true, a);
}

/** Stav houpačky: podepřená, padá, dosedla na zem, po puštění zůstala vodorovně. */
type SeePhase = 'held' | 'falling' | 'down' | 'level';

/**
 * Karta 0: houpačka. Podpěry ji drží vodorovně; žák nejdřív tipne, kam se nakloní, pak „Pusť“ podpěry odstraní.
 * Nevyrovnaný moment houpačku roztáčí, dokud konec nedosedne na zem. Kolo 2: žák polohu spočítá a ověří.
 */
function Seesaw() {
  const t = useTr();
  const kit = useKit();
  const knm = t.pick(L('kNm', 'кН·м'));
  const px = 200;
  const py = 140;
  const unit = 70; // 1 m = 70 jednotek
  const half = 2.6 * unit;
  const ground = py + 40;
  const thMax = (Math.asin((ground - py - 5) / half) * 180) / Math.PI; // úhel, kdy konec dosedne na zem
  // kolo 0: vlevo 6 kN v 1 m, posouvá se 3 kN; kolo 1: vlevo 3 kN ve 2 m, závaží 4 kN
  const round = kit.mut(0);
  const leftF = round.v === 0 ? 6 : 3;
  const leftR = round.v === 0 ? 1 : 2;
  const rightF = round.v === 0 ? 3 : 4;
  const p = kit.mut(0.5);
  const dragging = kit.mut(false);
  const tip = kit.mut<number | null>(null); // 0 = levý konec dolů, 1 = vodorovně, 2 = pravý konec dolů
  const phase = kit.mut<SeePhase>('held');
  const theta = kit.mut(0); // stupně, + = po směru hodinek (pravý konec dolů)
  const runId = kit.mut(0);
  const result = kit.mut<number | null>(null); // ΣM posledního pokusu
  const tried = kit.mut(false);
  const level0 = kit.mut(false);
  const level1 = kit.mut(false);
  // kolo 2: zadaná vzdálenost
  const input = kit.mut('');
  const inState = kit.mut<'ok' | 'nudge' | null>(null);
  const inMsg = kit.mut<L | null>(null);
  const misses = kit.mut(0);
  const fade = kit.anim(1);
  const resetting = kit.mut(false);
  // 0 tip, 1 první puštění, 2 hledání polohy, 3 kolo 2, 4 hotovo
  const stage = tip.v === null ? 0 : !tried.v ? 1 : !level0.v ? 2 : !level1.v ? 3 : 4;
  const canMove = stage === 2;
  const sumM = (pp: number) => leftF * leftR - rightF * pp; // ↺ kladně
  /** nový pokus: stará houpačka zmizí, nová se objeví podepřená */
  const renew = async (then: () => void) => {
    if (phase.v === 'held') {
      then();
      return;
    }
    resetting.v = true;
    await fade.animateTo(0, tween(180));
    theta.v = 0;
    phase.v = 'held';
    result.v = null;
    then();
    resetting.v = false;
    await fade.animateTo(1, tween(260));
  };
  const fresh = (then: () => void = () => {}) => {
    if (resetting.v) return;
    if (phase.v === 'held') then();
    else kit.scope.launch(() => renew(then));
  };
  const setP = (v: number) => {
    if (!canMove || phase.v === 'falling') return;
    const np = clamp(Math.round(v * 2) / 2, 0.5, 2.5);
    if (np !== p.v) fresh(() => (p.v = np));
  };
  const release = () => {
    if (phase.v !== 'held' || resetting.v) return;
    phase.v = 'falling';
    runId.v++;
  };
  /** kolo 2: postav závaží do spočítané vzdálenosti a pusť */
  const place = () => {
    if (phase.v === 'falling' || resetting.v) return;
    const v = parseNum(input.v);
    if (v === null || v <= 0 || v > 2.5) {
      inState.v = 'nudge';
      inMsg.v = L('Zadej vzdálenost od osy – houpačka má na každou stranu 2,5 m.', 'Введи расстояние от оси — у качелей по 2,5 м в каждую сторону.');
      return;
    }
    inMsg.v = null;
    kit.scope.launch(async () => {
      await renew(() => (p.v = v));
      await kit.scope.delay(450);
      release();
    });
  };
  // kolo 2 začne samo, až žák uvidí vyrovnanou houpačku z kola 1
  useLaunched([level0.v], async (job) => {
    if (!level0.v || round.v !== 0) return;
    await job.delay(1800);
    await renew(() => {
      round.v = 1;
      p.v = 0.5;
    });
  });
  useLaunched([runId.v], async (job) => {
    if (runId.v === 0) return;
    const sum = sumM(p.v);
    if (Math.abs(sum) < 1e-6) {
      // bez podpěr zůstane vodorovně – momenty se vyruší
      await job.delay(900);
      phase.v = 'level';
      result.v = 0;
      tried.v = true;
      if (round.v === 0) level0.v = true;
      else level1.v = true;
      return;
    }
    // úhlové zrychlení úměrné ΣM (kladné ΣM = ↺ = levý konec dolů = záporný úhel)
    const acc = -sum * 9;
    let time = 0;
    let last = 0;
    while (Math.abs(theta.v) < thMax) {
      const now = await job.frame();
      const dt = last === 0 ? 0 : (now - last) / 1000;
      last = now;
      time += dt;
      theta.v = clamp(0.5 * acc * time * time, -thMax, thMax);
    }
    theta.v = Math.sign(acc) * thMax;
    phase.v = 'down';
    result.v = sum;
    tried.v = true;
    if (round.v === 1) {
      misses.v++;
      inState.v = 'nudge';
    }
  });
  const sumText = (s: number) => `ΣM = ${num(leftF)} · ${num(leftR)} − ${num(rightF)} · ${num(p.v)} = ${num(s)} ${knm}`;
  const tipOpts = [L('levý konec dolů', 'левый конец вниз'), L('zůstane vodorovně', 'останутся горизонтально'), L('pravý konec dolů', 'правый конец вниз')];
  const releaseButton = () => <ActionButton text={L('Pusť (odstraň podpěry)', 'Отпустить (убрать подпорки)')} enabled={phase.v === 'held' && !resetting.v} onClick={release} />;
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Houpačka se dvěma závažími, podepřená podpěrami', 'Качели с двумя грузами на подпорках'))}
        onDrag={(pt, start) => {
          const [wx, wy] = turn(px + p.v * unit, py - 20, px, py, theta.v);
          if (start) dragging.v = canMove && phase.v !== 'falling' && dist(pt, wx, wy) < 55;
          if (dragging.v) setP((pt.x - px) / unit);
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b, c) => {
          const d = theta.v;
          const a = fade.value;
          // zem s měkkými podložkami pod konci
          b.hatch(10, 390, ground, 1);
          for (const sx of [px - half + 12, px + half - 12]) b.rect(sx - 14, ground - 4, 28, 4, c.subtle, c.inkSoft, 1.0);
          // podstavec
          b.poly(
            [
              [px, py + 5],
              [px - 18, ground],
              [px + 18, ground],
            ],
            c.paper,
            c.ink,
            1.5,
          );
          // podpěry drží houpačku vodorovně, dokud ji žák nepustí (mezi značkami 2 m a 2,5 m)
          if (phase.v === 'held') for (const sx of [px - 2.25 * unit, px + 2.25 * unit]) b.rect(sx - 4, py + 5, 8, ground - py - 5, c.subtle, c.ink, 1.3, a);
          const beamPts = (
            [
              [px - half, py - 5],
              [px + half, py - 5],
              [px + half, py + 5],
              [px - half, py + 5],
            ] as [number, number][]
          ).map(([x, y]) => turn(x, y, px, py, d));
          b.poly(beamPts, c.beam, c.ink, 1.6, a);
          // značky poloh: vpravo po 0,5 m, vlevo poloha pevného závaží
          for (let i = 1; i <= 5; i++) {
            const s = i * 0.5;
            const [mx, my] = turn(px + s * unit, py + 18, px, py, d);
            b.circle(mx, my - 13, 3, c.inkSoft, null, 1.5, a);
            b.text(mx, my + 8, `${num(s)} m`, c.inkSoft, 12, 'middle', null, false, a);
          }
          const [lx, ly] = turn(px - leftR * unit, py + 18, px, py, d);
          b.text(lx, ly + 8, `${num(leftR)} m`, c.inkSoft, 12, 'middle', null, false, a);
          weight(b, -leftR, `${num(leftF)} kN`, px, py, unit, d, c, false, a);
          weight(b, p.v, `${num(rightF)} kN`, px, py, unit, d, c, canMove, a);
          if (canMove && phase.v !== 'falling') {
            const [hx, hy] = turn(px + p.v * unit, py - 20, px, py, d);
            b.handle(hx, hy - 30, dragging.v);
          }
          const head =
            phase.v === 'held'
              ? L('podepřeno', 'на подпорках')
              : phase.v === 'falling'
                ? L('puštěno…', 'отпустили…')
                : phase.v === 'down'
                  ? L('ΣM ≠ 0 → naklonila se až na zem', 'ΣM ≠ 0 → наклонились до земли')
                  : L('ΣM = 0 ✓ zůstala vodorovně', 'ΣM = 0 ✓ остались горизонтально');
          b.text(px, 22, t.pick(head), phase.v === 'level' ? c.accent : c.inkSoft, 14, 'middle', null, phase.v === 'level');
        }}
      />
      <CardText
        text={L(
          'Houpačku zatím drží **podpěry**. Až je odstraníme, rozhodne **součet momentů** ΣM k ose (↺ +, ↻ −): když není nulový, houpačka se roztočí a dosedne koncem na zem.',
          'Качели пока держат **подпорки**. Когда их уберём, всё решит **сумма моментов** ΣM относительно оси (↺ +, ↻ −): если она не равна нулю, качели начнут поворачиваться и опустятся концом на землю.',
        )}
      />
      <Steps
        steps={[
          tipStep(L('Vlevo 6 kN v 1 m, vpravo 3 kN v 0,5 m. Kam se houpačka nakloní?', 'Слева 6 кН на 1 м, справа 3 кН на 0,5 м. Куда наклонятся качели?'), tipOpts, tip.v, (i) => (tip.v = i)),
          step(L('Odstraň podpěry a ověř svůj tip', 'Убери подпорки и проверь свой прогноз'), tried.v, releaseButton, () => (
            <Note
              tone={tip.v === 0 ? 'ok' : 'info'}
              text={L(
                (tip.v === 0 ? 'Tip sedí. ' : 'Tip nesedí. ') +
                  'ΣM = 6 · 1 − 3 · 0,5 = 4,5 kNm ↺ – převážil moment levého závaží. Houpačka se roztáčela čím dál rychleji, až levým koncem dosedla na zem.',
                (tip.v === 0 ? 'Прогноз верный. ' : 'Прогноз неверный. ') +
                  'ΣM = 6 · 1 − 3 · 0,5 = 4,5 кН·м ↺ — перевесил момент левого груза. Качели поворачивались всё быстрее, пока левым концом не опустились на землю.',
              )}
            />
          )),
          step(
            L('Najdi polohu závaží 3 kN, při které houpačka zůstane vodorovně', 'Найди положение груза 3 кН, при котором качели останутся горизонтальными'),
            level0.v,
            () => {
              const s = result.v;
              return (
                <>
                  <Chips
                    options={[L('0,5 m', '0,5 м'), L('1 m', '1 м'), L('1,5 m', '1,5 м'), L('2 m', '2 м'), L('2,5 m', '2,5 м')]}
                    selected={Math.round((p.v - 0.5) * 2)}
                    onPick={(i) => setP(0.5 + i * 0.5)}
                  />
                  {releaseButton()}
                  {s !== null && phase.v === 'down' ? (
                    <>
                      <Readout text={sumText(s)} />
                      <SmallText
                        text={
                          s > 0
                            ? L('Převážilo levé závaží (↺). Posuň závaží a zkus to znovu.', 'Перевесил левый груз (↺). Передвинь груз и попробуй снова.')
                            : L('Převážilo pravé závaží (↻). Posuň závaží a zkus to znovu.', 'Перевесил правый груз (↻). Передвинь груз и попробуй снова.')
                        }
                      />
                    </>
                  ) : phase.v === 'down' || phase.v === 'level' ? (
                    <SmallText text={L('Posuň závaží – objeví se nová podepřená houpačka.', 'Передвинь груз — появятся новые качели на подпорках.')} />
                  ) : null}
                </>
              );
            },
            () => (
              <Note
                tone="ok"
                text={L(
                  'Rovnováha! Poloviční síla potřebuje dvojnásobné rameno: 6 · 1 = 3 · 2. Momenty obou závaží jsou stejně velké a opačné, takže houpačka zůstane, jak je.',
                  'Равновесие! Вдвое меньшей силе нужно вдвое большее плечо: 6 · 1 = 3 · 2. Моменты грузов равны по величине и противоположны, поэтому качели остаются как есть.',
                )}
              />
            ),
          ),
          step(
            L('Kolo 2: vlevo 3 kN ve 2 m. Kam od osy patří závaží 4 kN?', 'Раунд 2: слева 3 кН на 2 м. На каком расстоянии от оси ставить груз 4 кН?'),
            level1.v,
            () => {
              const s = result.v;
              return (
                <>
                  <NumInput
                    label={<Rich text="[[r]]" />}
                    value={input.v}
                    onChange={(v) => {
                      input.v = v;
                      inState.v = null;
                      inMsg.v = null;
                      // jiná hodnota = nový pokus: nová podepřená houpačka
                      if (phase.v === 'down') fresh();
                    }}
                    state={inState.v}
                    suffix={t.pick(L('m', 'м'))}
                    onEnter={place}
                  />
                  <ActionButton text={L('Postav a pusť', 'Поставить и отпустить')} enabled={round.v === 1 && phase.v !== 'falling' && !resetting.v} onClick={place} />
                  {inMsg.v && <SmallText text={inMsg.v} />}
                  {s !== null && phase.v === 'down' && round.v === 1 && (
                    <>
                      <Readout text={sumText(s)} />
                      <SmallText
                        text={
                          misses.v >= 2
                            ? L('Momenty se mají rovnat: 3 · 2 = 4 · [[r]].', 'Моменты должны быть равны: 3 · 2 = 4 · [[r]].')
                            : s > 0
                              ? L('Převážilo levé závaží – rameno vpravo je krátké.', 'Перевесил левый груз — правое плечо коротковато.')
                              : L('Převážilo pravé závaží – rameno vpravo je dlouhé.', 'Перевесил правый груз — правое плечо длинновато.')
                        }
                      />
                    </>
                  )}
                </>
              );
            },
            () => (
              <Note
                tone="ok"
                text={L(
                  'Ano: 3 · 2 = 4 · 1,5. Těžší závaží stačí kratší rameno – rozhoduje součin síla × rameno, ne síla sama.',
                  'Да: 3 · 2 = 4 · 1,5. Более тяжёлому грузу хватает меньшего плеча — решает произведение сила × плечо, а не сама сила.',
                )}
              />
            ),
          ),
        ]}
      />
    </>
  );
}

/** Stav páky: leží (kámen na zemi), zvedá se, dosedla (dlouhý konec na zemi). */
type LeverPhase = 'rest' | 'lifting' | 'up';

/**
 * Karta 1: Archimédova páka. Kámen 4 kN leží na zemi, pod ním konec tyče 5,5 m, podpěra jde posunout.
 * Archimédés tlačí na dlouhý konec 500 N. Pak zlaté pravidlo (síla × dráha) a nakonec Země.
 */
function Lever() {
  const t = useTr();
  const kit = useKit();
  const knm = t.pick(L('kNm', 'кН·м'));
  const len = 5.5; // délka tyče, m
  const unit = 65; // px na metr vodorovně
  const vz = 2.0; // svislé zvětšení
  const x0 = 20;
  const gy = 198;
  const hf = 0.1 / 1.1; // výška podpěry: při rameni 0,5 m se kámen zvedne právě o 10 cm
  const stoneF = 4;
  const handF = 0.5;
  const a = kit.mut(2.0); // rameno kamene
  const tip = kit.mut<number | null>(null);
  const phase = kit.mut<LeverPhase>('rest');
  const s = kit.mut(0); // 0 = kámen na zemi, 1 = dlouhý konec na zemi
  const pushed = kit.mut<number | null>(null); // a posledního neúspěšného zatlačení
  const lifted = kit.mut(false);
  const runId = kit.mut(0);
  const fade = kit.anim(1);
  const resetting = kit.mut(false);
  const bArm = len - a.v;
  const ms = stoneF * a.v;
  const mh = handF * bArm;
  const armQ = L(
    'Kámen 4 kN na rameni 0,5 m. Jak dlouhé rameno potřebuje ruka 500 N, aby páku udržela?',
    'Камень 4 кН на плече 0,5 м. Какое плечо нужно руке 500 Н, чтобы удержать рычаг?',
  );
  const arm = useNum(
    armQ,
    [numSpec(L('[[r]]', '[[r]]'), 4, t.pick(L('m', 'м')))],
    L(
      '4 · 0,5 = 0,5 · [[r]] → [[r]] = 2 / 0,5 = **4 m**. Síla je 8× menší, takže rameno musí být 8× delší. S 5 m už ruka kámen zvedne.',
      '4 · 0,5 = 0,5 · [[r]] → [[r]] = 2 / 0,5 = **4 м**. Сила в 8 раз меньше, значит плечо должно быть в 8 раз длиннее. С 5 м рука уже поднимает камень.',
    ),
    [
      trapNear(L('To je jen poměr sil 4 / 0,5. Rovnají se ale **momenty**: 4 · 0,5 = 0,5 · [[r]].', 'Это только отношение сил 4 / 0,5. Равны же **моменты**: 4 · 0,5 = 0,5 · [[r]].'), 8),
      trapNear(L('Obráceně – menší síla potřebuje **delší** rameno.', 'Наоборот — меньшей силе нужно **более длинное** плечо.'), 0.0625),
    ],
    L(
      'Momenty k podpěře se musí rovnat: síla kamene × jeho rameno = síla ruky × [[r]].',
      'Моменты относительно опоры должны быть равны: сила камня × его плечо = сила руки × [[r]].',
    ),
  );
  const gold = useQuiz(
    L('Kámen se zvedl o 10 cm. O kolik musela ruka sjet dolů?', 'Камень поднялся на 10 см. На сколько должна была опуститься рука?'),
    [
      opt(
        L('o 10 cm', 'на 10 см'),
        false,
        L('Konec u ruky je 10× dál od podpěry – otáčí se o stejný úhel, ale opisuje 10× delší oblouk.', 'Конец у руки в 10 раз дальше от опоры — поворачивается на тот же угол, но проходит дугу в 10 раз длиннее.'),
      ),
      opt(L('o 1 m', 'на 1 м'), true),
      opt(L('o 1 cm', 'на 1 см'), false, L('Ruka tlačí 8× slaběji – něco za to musí zaplatit. Platí dráhou, ne úsporou dráhy.', 'Рука давит в 8 раз слабее — за это надо чем-то платить. Платит путём, а не экономией пути.')),
    ],
    L(
      'Rameno ruky (5 m) je 10× delší než rameno kamene (0,5 m), takže ruka ujede 10× delší dráhu: **1 m**. To je **zlaté pravidlo mechaniky**: co páka ušetří na síle, to zaplatíš dráhou. Síla × dráha (práce) vyjde na obou stranách skoro stejně.',
      'Плечо руки (5 м) в 10 раз длиннее плеча камня (0,5 м), поэтому рука проходит путь в 10 раз длиннее: **1 м**. Это **золотое правило механики**: что рычаг даёт в силе, то теряешь в пути. Сила × путь (работа) с обеих сторон почти одинаковы.',
    ),
  );
  const earth = useQuiz(
    L(
      'Země by na zemském povrchu vážila asi 6·10²⁵ N, Archimédés tlačí 500 N. Kolikrát delší musí mít rameno než Země?',
      'Земля на поверхности Земли весила бы около 6·10²⁵ Н, Архимед давит с силой 500 Н. Во сколько раз его плечо должно быть длиннее, чем у Земли?',
    ),
    [
      opt(L('asi 1 000×', 'примерно в 1 000 раз'), false, L('Poměr ramen = poměr sil: 6·10²⁵ / 500. Kolik je to řádů?', 'Отношение плеч = отношению сил: 6·10²⁵ / 500. Сколько это порядков?')),
      opt(L('asi 10¹²×', 'примерно в 10¹² раз'), false, L('Ještě víc: 6·10²⁵ / 5·10² = 1,2·10²³.', 'Ещё больше: 6·10²⁵ / 5·10² = 1,2·10²³.')),
      opt(L('asi 10²³×', 'примерно в 10²³ раз'), true),
    ],
    L(
      '6·10²⁵ / 500 ≈ **10²³**. S ramenem Země 1 m by Archimédovo rameno měřilo asi 10²³ m – zhruba **13 milionů světelných let**, dál než galaxie v Andromedě. A aby Zemi zvedl o 1 cm, musel by konec páky stlačit o 10²¹ m: i rychlostí světla by to trvalo přes **100 000 let**. Princip platí – páka dá libovolně velkou sílu –, jen ji zaplatíš obrovskou dráhou. (Země ve vesmíru „neváží“ nic; počítáme, jako by ji zvedal na zemském povrchu.)',
      '6·10²⁵ / 500 ≈ **10²³**. При плече Земли 1 м плечо Архимеда было бы около 10²³ м — примерно **13 миллионов световых лет**, дальше галактики Андромеды. А чтобы поднять Землю на 1 см, ему пришлось бы опустить конец рычага на 10²¹ м: даже со скоростью света это заняло бы больше **100 000 лет**. Принцип верен — рычаг даёт сколь угодно большую силу, — но платишь за неё огромным путём. (В космосе Земля ничего «не весит»; считаем, будто он поднимает её у поверхности Земли.)',
    ),
  );
  const space = arm.solved && gold.solved;
  const setA = (v: number) => {
    if (phase.v === 'lifting' || resetting.v || v === a.v) return;
    pushed.v = null;
    if (phase.v === 'rest') {
      a.v = v;
      return;
    }
    // zvednutá páka nezmizí sama od sebe: stará zmizí, nová leží s kamenem na zemi
    kit.scope.launch(async () => {
      resetting.v = true;
      await fade.animateTo(0, tween(180));
      s.v = 0;
      phase.v = 'rest';
      a.v = v;
      resetting.v = false;
      await fade.animateTo(1, tween(260));
    });
  };
  const push = () => {
    if (phase.v !== 'rest' || resetting.v) return;
    if (mh <= ms) {
      pushed.v = a.v;
      return;
    }
    pushed.v = null;
    phase.v = 'lifting';
    runId.v++;
  };
  useLaunched([runId.v], async (job) => {
    if (runId.v === 0) return;
    // úhlové zrychlení úměrné převaze momentu ruky; konec se zastaví o zem
    const k = (mh - ms) * 2.0;
    let time = 0;
    let last = 0;
    while (s.v < 1) {
      const now = await job.frame();
      const dt = last === 0 ? 0 : (now - last) / 1000;
      last = now;
      time += dt;
      s.v = Math.min(0.5 * k * time * time, 1);
    }
    phase.v = 'up';
    if (a.v === 0.5) lifted.v = true;
  });
  const tipOpts = [L('blízko kamene', 'ближе к камню'), L('doprostřed', 'посередине'), L('blízko ruky', 'ближе к руке')];
  const quizStepOf = (q: QuizState, label: L, cols: number): Step => ({
    label,
    done: q.solved,
    result: () => q.Feedback(),
    controls: () => (
      <>
        <p className="dock-q">
          <Rich text={t.pick(q.question)} />
        </p>
        {q.Options(cols)}
        {!q.solved && q.Feedback()}
      </>
    ),
  });
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Páka s podpěrou, kámen a Archimédés', 'Рычаг с опорой, камень и Архимед'))}
        draw={(b, c) => {
          if (space) {
            // vesmír: Země na krátkém rameni, dlouhé rameno mizí za okrajem
            const W = '#ffffff';
            b.rect(0, 0, 400, 230, '#0b1324');
            for (let i = 0; i < 40; i++) b.circle((i * 97) % 400, (i * 53) % 230, i % 7 === 0 ? 1.6 : 0.9, W, null, 1.5, 0.7);
            b.circle(70, 118, 34, '#2e7bd6', '#8cc4ff', 1.2);
            b.poly(
              [
                [64, 100],
                [80, 96],
                [86, 112],
                [72, 124],
                [60, 116],
              ],
              '#3fa45b',
            );
            b.poly(
              [
                [108, 158],
                [98, 178],
                [118, 178],
              ],
              '#b0b8c8',
              W,
              1.0,
            );
            b.line(70, 154, 400, 160, '#b0b8c8', 3.0);
            b.line(330, 160, 400, 161, '#0b1324', 3.2, true);
            b.text(110, 196, t.pick(L('pevný bod', 'точка опоры')), W, 12);
            b.text(70, 66, t.pick(L('Země 6·10²⁵ N', 'Земля 6·10²⁵ Н')), W, 13);
            b.text(250, 142, t.pick(earth.solved ? L('→ rameno ≈ 10²³ m → … Archimédés', '→ плечо ≈ 10²³ м → … Архимед') : L('→ rameno Archiméda = ?', '→ плечо Архимеда = ?')), '#ffd27a', 13);
            b.text(200, 24, t.pick(L('„Dejte mi pevný bod a pohnu Zemí.“', '«Дайте мне точку опоры, и я сдвину Землю».')), W, 14, 'middle', null, true);
            return;
          }
          const al = fade.value;
          const xf = x0 + a.v * unit;
          const xr = x0 + len * unit;
          const py = (hm: number) => gy - hm * unit * vz;
          // tan sklonu: od kamene na zemi (s = 0) po dlouhý konec na zemi (s = 1)
          const tg = hf / a.v + s.v * (-hf / bArm - hf / a.v);
          const hl = hf - a.v * tg;
          const hr = hf + bArm * tg;
          b.hatch(0, 400, gy, 1);
          // podpěra
          b.poly(
            [
              [xf, py(hf)],
              [xf - 11, gy],
              [xf + 11, gy],
            ],
            c.paper,
            c.ink,
            1.5,
            al,
          );
          // tyč
          b.line(x0, py(hl), xr, py(hr), c.ink, 5.0, false, al);
          b.line(x0, py(hl), xr, py(hr), c.beam, 3.0, false, al);
          // kámen leží na krátkém konci (nebo na zemi)
          const sb = py(hl) - 2;
          b.poly(
            [
              [x0 - 4, sb],
              [x0 + 50, sb],
              [x0 + 46, sb - 30],
              [x0 + 30, sb - 40],
              [x0 + 6, sb - 34],
            ],
            c.subtle,
            c.ink,
            1.6,
            al,
          );
          b.text(x0 + 24, sb - 14, '4 kN', c.load, 13, 'middle', null, true, al);
          // ruka tlačí na dlouhý konec
          const hy = py(hr);
          b.force(xr - 4, hy - 48, xr - 4, hy - 6, c.load, 2.4, 11, false, al);
          b.text(xr - 10, hy - 36, '500 N', c.load, 13, 'end', null, false, al);
          // ramena
          b.text((x0 + xf) / 2, gy + 20, `${num(a.v)} m`, c.react, 12, 'middle', null, false, al);
          b.text((xf + xr) / 2, gy + 20, `${num(bArm)} m`, c.react, 12, 'middle', null, false, al);
          if (phase.v === 'up' && a.v === 0.5) {
            b.text(x0 + 60, sb - 22, t.pick(L('↑ 10 cm', '↑ 10 см')), c.accent, 13, 'start', null, true);
            b.text(xr - 14, gy - 12, t.pick(gold.solved ? L('↓ 1 m', '↓ 1 м') : L('↓ ?', '↓ ?')), c.accent, 13, 'end', null, true);
          }
          const head =
            phase.v === 'lifting'
              ? L('zvedá se…', 'поднимается…')
              : phase.v === 'up'
                ? L('kámen zvednut – konec páky dosedl na zem', 'камень поднят — конец рычага упёрся в землю')
                : pushed.v === a.v
                  ? L('ani se nehne – kámen drží zem', 'даже не шелохнулся — камень лежит на земле')
                  : L('svisle 2× zvětšeno', 'по вертикали увеличено в 2 раза');
          b.text(200, 18, t.pick(head), phase.v === 'up' ? c.accent : c.inkSoft, 13, 'middle', null, phase.v === 'up');
        }}
      />
      <CardText
        text={L(
          'Archimédés prý řekl: **„Dejte mi pevný bod a pohnu Zemí.“** Zkusme menší úkol: tlačí na **páku** silou 500 N a chce zvednout kámen 4 kN – osmkrát těžší, než sám zvládne.',
          'Архимед, по преданию, сказал: **«Дайте мне точку опоры, и я сдвину Землю».** Начнём с задачи поменьше: он давит на **рычаг** с силой 500 Н и хочет поднять камень 4 кН — в восемь раз тяжелее, чем осилит сам.',
        )}
      />
      <SmallText
        text={L('Páka je tuhá tyč otočná kolem podpěry. Rozhoduje moment k podpěře: síla × rameno.', 'Рычаг — жёсткий стержень, поворачивающийся вокруг опоры. Решает момент относительно опоры: сила × плечо.')}
      />
      <Steps
        steps={[
          tipStep(L('Kam dát podpěru, aby 500 N kámen zvedlo?', 'Куда поставить опору, чтобы 500 Н подняли камень?'), tipOpts, tip.v, (i) => (tip.v = i)),
          step(
            L('Posouvej podpěru a zatlač, dokud kámen nezvedneš', 'Передвигай опору и дави, пока не поднимешь камень'),
            lifted.v,
            () => (
              <>
                <Chips
                  options={[L('podpěra 2 m od kamene', 'опора в 2 м от камня'), L('1 m', '1 м'), L('0,5 m', '0,5 м')]}
                  selected={a.v === 2 ? 0 : a.v === 1 ? 1 : 2}
                  onPick={(i) => setA([2.0, 1.0, 0.5][i])}
                />
                <ActionButton text={L('Zatlač 500 N', 'Надавить 500 Н')} enabled={phase.v === 'rest' && !resetting.v} onClick={push} />
                {pushed.v === a.v && phase.v === 'rest' && (
                  <>
                    <Readout text={`${num(handF)} · ${num(bArm)} = ${num(mh)} < ${num(stoneF)} · ${num(a.v)} = ${num(ms)} ${knm}`} />
                    <SmallText text={L('Moment ruky je menší než moment kamene. Zkrať rameno kamene.', 'Момент руки меньше момента камня. Укороти плечо камня.')} />
                  </>
                )}
              </>
            ),
            () => (
              <Note
                tone={tip.v === 0 ? 'ok' : 'info'}
                text={L(
                  (tip.v === 0 ? 'Tip sedí. ' : '') +
                    'Podpěra 0,5 m od kamene: kámen 4 · 0,5 = 2 kNm, ruka 0,5 · 5 = 2,5 kNm. Moment ruky převážil – i když je síla 8× menší, má 10× delší rameno.',
                  (tip.v === 0 ? 'Прогноз верный. ' : '') +
                    'Опора в 0,5 м от камня: камень 4 · 0,5 = 2 кН·м, рука 0,5 · 5 = 2,5 кН·м. Момент руки перевесил — сила в 8 раз меньше, зато плечо в 10 раз длиннее.',
                )}
              />
            ),
          ),
          {
            label: L('Spočítej rameno, které ruce stačí', 'Посчитай плечо, которого руке достаточно'),
            done: arm.solved,
            result: () => arm.Feedback(),
            controls: () => (
              <>
                <p className="dock-q">
                  <Rich text={t.pick(armQ)} />
                </p>
                {arm.Fields()}
                {arm.Buttons()}
                {!arm.solved && arm.Feedback()}
              </>
            ),
          },
          quizStepOf(gold, L('Zlaté pravidlo: o kolik sjela ruka?', 'Золотое правило: на сколько опустилась рука?'), 3),
          quizStepOf(earth, L('A teď Země: jak dlouhou páku by potřeboval?', 'А теперь Земля: какой длины нужен рычаг?'), 1),
        ]}
      />
    </>
  );
}

/** Vor na klidné vodě shora: jeden nebo dva lodní motory, motor 2 jde posunout na jinou nositelku. */
function Raft() {
  const t = useTr();
  const kit = useKit();
  const knm = t.pick(L('kNm', 'кН·м'));
  const cx0 = 130;
  const cy0 = 112;
  const w = 110;
  const h = 60;
  const perM = 20; // jednotek na metr
  const force = 0.5; // kN na motor
  const both = kit.mut(false);
  const dm = kit.mut(1.2); // posun nositelky motoru 2 pod těžiště, m
  const guess = kit.mut<number | null>(null); // 0 stojí, 1 jede, 2 točí se
  const dragging = kit.mut(false);
  const running = kit.mut(false);
  const ended = kit.mut(false);
  const dx = kit.mut(0);
  const ang = kit.mut(0);
  const ghosts = kit.list<[number, number]>([]);
  const runId = kit.mut(0);
  const fade = kit.anim(1);
  const ranOne = kit.mut(false);
  const ranCouple = kit.mut(false);
  const ranRest = kit.mut(false);
  const lastKind = kit.mut<number | null>(null); // 0 jede, 1 točí se, 2 stojí
  const lastD = kit.mut(0);
  const couple = both.v && Math.abs(dm.v) > 1e-9;
  // 0 jeden motor, 1 tip, 2 oba motory, 3 jedna nositelka, 4 hotovo
  const stage = !ranOne.v ? 0 : guess.v === null ? 1 : !ranCouple.v ? 2 : !ranRest.v ? 3 : 4;
  const canMove = stage >= 3;
  const fresh = (then: () => void = () => {}) => {
    if (!ended.v && !running.v) {
      then();
      return;
    }
    kit.scope.launch(async () => {
      await fade.animateTo(0, tween(180));
      running.v = false;
      ended.v = false;
      dx.v = 0;
      ang.v = 0;
      ghosts.clear();
      then();
      await fade.animateTo(1, tween(260));
    });
  };
  // po prvním pokusu se k voru přidá motor 2 (nový vor prolnutím)
  useLaunched([ranOne.v], async (job) => {
    if (!ranOne.v || both.v) return;
    await job.delay(1200);
    fresh(() => (both.v = true));
  });
  useLaunched([runId.v], async (job) => {
    if (runId.v === 0) return;
    const lin = both.v ? 0 : 50; // zrychlení těžiště, jednotek/s²
    const rot = both.v ? 4.0 * dm.v * perM : 0; // úhlové zrychlení, °/s² (moment F·d)
    let time = 0;
    let last = 0;
    let nextGhost = 0;
    while (time < 2.6) {
      const now = await job.frame();
      const dt = last === 0 ? 0 : (now - last) / 1000;
      last = now;
      time += dt;
      dx.v = 0.5 * lin * time * time;
      ang.v = 0.5 * rot * time * time;
      if (time >= nextGhost) {
        ghosts.add([dx.v, ang.v]);
        nextGhost += 0.4;
      }
    }
    running.v = false;
    ended.v = true;
    const kind = !both.v ? 0 : couple ? 1 : 2;
    lastKind.v = kind;
    lastD.v = dm.v;
    if (kind === 0) ranOne.v = true;
    else if (kind === 1) ranCouple.v = true;
    else if (ranCouple.v) ranRest.v = true;
    // pokus skončil: vor pomalu zmizí, stopa zůstane
    await job.run(fade.animateTo(0.3, tween(600)));
  });
  const start = () => {
    if (running.v) return;
    fresh(() => {
      running.v = true;
      ended.v = false;
      runId.v++;
    });
  };
  const moveMotor = (nd: number) => {
    if (running.v || !canMove) return;
    const v = clamp(nd, -1.2, 1.2);
    if (Math.abs(v - dm.v) > 1e-9) fresh(() => (dm.v = v));
  };
  const bodyPt = (bx: number, by: number, sx: number, sa: number): [number, number] => {
    const [rx, ry] = turn(cx0 + bx, cy0 + by, cx0, cy0, sa);
    return [rx + sx, ry];
  };
  const momentLine = `M = 0,5 · ${num(Math.abs(lastD.v))} = ${num(force * Math.abs(lastD.v))} ${knm}`;
  const tipOpts = [L('zůstane stát', 'останется на месте'), L('pojede rovně', 'поедет прямо'), L('bude se točit', 'будет вращаться')];
  const corners: [number, number][] = [
    [-w / 2, -h / 2],
    [w / 2, -h / 2],
    [w / 2, h / 2],
    [-w / 2, h / 2],
  ];
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Vor na vodě shora se dvěma lodními motory', 'Плот на воде, вид сверху, с двумя лодочными моторами'))}
        onDrag={(p, start) => {
          if (start) dragging.v = both.v && canMove && !running.v && dist(p, cx0 + w / 2 + 84, cy0 + dm.v * perM) < 40;
          if (dragging.v) moveMotor(Math.round((p.y - cy0) / perM / 0.6) * 0.6);
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b, c) => {
          // voda
          b.rect(8, 8, 384, 204, withAlpha(c.react, 0.06), null);
          for (let k = 0; k <= 3; k++) b.line(30 + k * 95, 196, 70 + k * 95, 196, withAlpha(c.react, 0.3), 1.0);
          const a = fade.value;
          // nositelky motorů (pevně ve světě, než se vor rozjede)
          if (!running.v && !ended.v) {
            b.line(10, cy0, 390, cy0, c.inkSoft, 1.0, true);
            if (both.v) b.line(10, cy0 + dm.v * perM, 390, cy0 + dm.v * perM, c.inkSoft, 1.0, true);
          }
          // stopa: obrysy voru po 0,4 s
          for (const [gx, ga] of ghosts.v) {
            b.poly(
              corners.map(([x, y]) => bodyPt(x, y, gx, ga)),
              null,
              c.inkSoft,
              0.8,
              0.45,
            );
            const [tx, ty] = bodyPt(0, 0, gx, ga);
            b.circle(tx, ty, 2, c.inkSoft, null, 1.5, 0.6);
          }
          // vor: prkna a těžiště T
          b.poly(
            corners.map(([x, y]) => bodyPt(x, y, dx.v, ang.v)),
            c.beam,
            c.ink,
            1.6,
            a,
          );
          for (let k = 1; k <= 3; k++) {
            const yy = -h / 2 + (k * h) / 4;
            const [x1, y1] = bodyPt(-w / 2, yy, dx.v, ang.v);
            const [x2, y2] = bodyPt(w / 2, yy, dx.v, ang.v);
            b.line(x1, y1, x2, y2, c.inkSoft, 0.8, false, a * 0.6);
          }
          const [tx, ty] = bodyPt(0, 0, dx.v, ang.v);
          b.line(tx - 7, ty, tx + 7, ty, c.ink, 1.4, false, a);
          b.line(tx, ty - 7, tx, ty + 7, c.ink, 1.4, false, a);
          b.text(tx - 6, ty - 6, 'T', c.ink, 12, 'end', null, true, a);
          // motor 1: vlevo, tlačí doprava po nositelce přes T
          const [m1x, m1y] = bodyPt(-w / 2 - 44, 0, dx.v, ang.v);
          const [m1t, m1u] = bodyPt(-w / 2, 0, dx.v, ang.v);
          const [b1x, b1y] = bodyPt(-w / 2 - 52, 0, dx.v, ang.v);
          b.circle(b1x, b1y, 7, c.ink, null, 1.5, a);
          b.force(m1x, m1y, m1t, m1u, c.load, 3.0, 11, false, a);
          b.text(m1x + 4, m1y - 10, '1', c.load, 12, 'start', null, true, a);
          if (both.v) {
            // motor 2: vpravo, tlačí doleva po nositelce posunuté o d
            const yy = dm.v * perM;
            const [m2x, m2y] = bodyPt(w / 2 + 44, yy, dx.v, ang.v);
            const [m2t, m2u] = bodyPt(w / 2, yy, dx.v, ang.v);
            const [b2x, b2y] = bodyPt(w / 2 + 52, yy, dx.v, ang.v);
            b.circle(b2x, b2y, 7, c.ink, null, 1.5, a);
            b.force(m2x, m2y, m2t, m2u, c.load, 3.0, 11, false, a);
            b.text(m2x - 4, m2y - 10, '2', c.load, 12, 'end', null, true, a);
            if (!running.v && !ended.v) {
              if (canMove) b.handle(w / 2 + cx0 + 84, cy0 + yy, dragging.v);
              if (Math.abs(dm.v) > 1e-9) {
                // vzdálenost nositelek d
                const xd = cx0 + w / 2 + 120;
                b.line(xd, cy0, xd, cy0 + yy, c.derived, 1.4);
                b.line(xd - 5, cy0, xd + 5, cy0, c.derived, 1.4);
                b.line(xd - 5, cy0 + yy, xd + 5, cy0 + yy, c.derived, 1.4);
                b.text(xd + 8, cy0 + yy / 2 + 5, `d = ${num(Math.abs(dm.v))} m`, c.derived, 12, 'start');
              }
            }
            if (couple && (running.v || ended.v) && a > 0.25) b.moment(tx, ty, 20, dm.v < 0, c.derived);
          }
          if (ended.v) b.text(200, 26, t.pick(L('konec pokusu – stopa po 0,4 s', 'конец опыта — след через 0,4 с')), c.inkSoft, 12);
        }}
      />
      <CardText
        text={L(
          'Vor na klidné vodě, pohled shora; odpor vody zanedbáme. Každý lodní motor tlačí silou 0,5 kN. Stačí ΣF = 0 k tomu, aby vor zůstal v klidu?',
          'Плот на спокойной воде, вид сверху; сопротивлением воды пренебрегаем. Каждый лодочный мотор толкает с силой 0,5 кН. Достаточно ли ΣF = 0, чтобы плот оставался в покое?',
        )}
      />
      <Steps
        steps={[
          step(
            L('Zapni jen motor 1 a sleduj, co vor udělá', 'Включи только мотор 1 и посмотри, что сделает плот'),
            ranOne.v,
            () => <ActionButton text={L('Zapni motor 1', 'Включить мотор 1')} enabled={!running.v} onClick={start} />,
            () => (
              <Note
                tone="info"
                text={L(
                  'Jedna nevyrovnaná síla vor **rozjíždí** – obrysy se od sebe vzdalují čím dál víc. Její nositelka prochází těžištěm T, proto se vor netočí.',
                  'Одна неуравновешенная сила **разгоняет** плот — контуры расходятся всё сильнее. Её линия действия проходит через центр тяжести T, поэтому плот не вращается.',
                )}
              />
            ),
          ),
          step(
            L('Přidáme stejný motor 2 proti prvnímu, ale níž. Co vor udělá?', 'Добавим такой же мотор 2 навстречу первому, но ниже. Что сделает плот?'),
            guess.v !== null,
            () => (
              <>
                <Readout text="ΣF = 0,5 − 0,5 = 0" />
                <Chips options={tipOpts} selected={guess.v} onPick={(i) => guess.v === null && (guess.v = i)} />
              </>
            ),
            () => (guess.v !== null ? <SmallText text={L(`Tvůj tip: **${tipOpts[guess.v].cs}**`, `Твой прогноз: **${tipOpts[guess.v].ru}**`)} /> : null),
          ),
          step(
            L('Zapni oba motory a ověř svůj tip', 'Включи оба мотора и проверь свой прогноз'),
            ranCouple.v,
            () => <ActionButton text={L('Zapni oba motory', 'Включить оба мотора')} enabled={!running.v && both.v} onClick={start} />,
            () => (
              <Note
                tone={guess.v === 2 ? 'ok' : 'info'}
                text={L(
                  (guess.v === 2 ? 'Tip sedí. ' : 'Tip nesedí. ') +
                    'ΣF = 0, takže těžiště T zůstalo na místě – ale síly leží na **různých** nositelkách. Tvoří dvojici s momentem M = 0,5 · 1,2 = 0,6 kNm a vor se roztáčí čím dál rychleji. **ΣF = 0 nestačí.**',
                  (guess.v === 2 ? 'Прогноз верный. ' : 'Прогноз неверный. ') +
                    'ΣF = 0, поэтому центр тяжести T остался на месте — но силы лежат на **разных** линиях действия. Они образуют пару с моментом M = 0,5 · 1,2 = 0,6 кН·м, и плот вращается всё быстрее. **ΣF = 0 недостаточно.**',
                )}
              />
            ),
          ),
          step(
            L('Posuň motor 2 tak, aby vor zůstal stát, a zapni motory', 'Передвинь мотор 2 так, чтобы плот стоял, и включи моторы'),
            ranRest.v,
            () => (
              <>
                <Chips options={[L('motor 2 výš', 'мотор 2 выше'), L('motor 2 níž', 'мотор 2 ниже')]} onPick={(i) => moveMotor(dm.v + (i === 0 ? -0.6 : 0.6))} />
                <ActionButton text={L('Zapni oba motory', 'Включить оба мотора')} enabled={!running.v} onClick={start} />
                {ended.v && lastKind.v === 1 && stage === 3 && (
                  <SmallText text={L(`Pořád se točí: ${momentLine}. Nositelky jsou pořád od sebe.`, `Всё ещё вращается: ${momentLine}. Линии действия всё ещё не совпадают.`)} />
                )}
              </>
            ),
            () => (
              <>
                <Note tone="ok" text={L('Teď leží obě síly na **jedné** nositelce: ΣF = 0 i ΣM = 0 – vor se nehne.', 'Теперь обе силы лежат на **одной** линии действия: ΣF = 0 и ΣM = 0 — плот не двигается.')} />
                <Readout text="Σ[[F_x]] = 0     Σ[[F_y]] = 0     ΣM = 0" ok />
                <Note
                  tone="ok"
                  text={L(
                    'Těleso v rovině se může posunout vodorovně, posunout svisle a otočit. Aby zůstalo v klidu, musí platit všechny tři **rovnice rovnováhy** – nulový součet sil ještě neznamená nulový součet momentů.',
                    'Тело на плоскости может сдвинуться по горизонтали, по вертикали и повернуться. Чтобы оно оставалось в покое, должны выполняться все три **уравнения равновесия** — нулевая сумма сил ещё не означает нулевую сумму моментов.',
                  )}
                />
              </>
            ),
          ),
        ]}
      />
    </>
  );
}

/** Karta 2: výhodný bod pro momentovou rovnici – nejdřív [[R_B]], pak [[R_Ay]]; rovnici pak žák sám vyřeší. */
function ChoosePoint() {
  const t = useTr();
  const kn = t.pick(L('kN', 'кН'));
  const y = 100;
  const xa = 80;
  const u = 60;
  const xb = xa + 4 * u;
  const xs = xa + 2 * u;
  const opts = (good: number, whyA: L | null, whyS: L, whyB: L | null): Opt[] => [
    opt(L('bod A', 'точка A'), good === 0, whyA),
    opt(L('střed S', 'середина S'), false, whyS),
    opt(L('bod B', 'точка B'), good === 2, whyB),
  ];
  const qB = useQuiz(
    L('Chceš hned [[R_B]]. Ke kterému bodu napíšeš ΣM = 0?', 'Нужна сразу [[R_B]]. Относительно какой точки запишешь ΣM = 0?'),
    opts(
      0,
      null,
      L(
        'Σ[[M_S]]: −[[R_Ay]]·2 + 8·1 + [[R_B]]·2 = 0 – dvě neznámé v jedné rovnici, z ní samotné nic nespočítáš.',
        'Σ[[M_S]]: −[[R_Ay]]·2 + 8·1 + [[R_B]]·2 = 0 — две неизвестные в одном уравнении, из него одного ничего не найти.',
      ),
      L(
        'Σ[[M_B]]: −[[R_Ay]]·4 + 8·3 = 0 – vypadla právě [[R_B]], kterou hledáš. Který bod vyřadí obě ostatní neznámé?',
        'Σ[[M_B]]: −[[R_Ay]]·4 + 8·3 = 0 — выпала как раз [[R_B]], которую ты ищешь. Какая точка уберёт обе другие неизвестные?',
      ),
    ),
    L(
      'Σ[[M_A]] = 0:  −8 · 1 + [[R_B]] · 4 = 0. Nositelky [[R_Ax]] i [[R_Ay]] procházejí bodem A – mají rameno 0 a z rovnice vypadnou. Zbyla jediná neznámá [[R_B]].',
      'Σ[[M_A]] = 0:  −8 · 1 + [[R_B]] · 4 = 0. Линии действия [[R_Ax]] и [[R_Ay]] проходят через A — их плечо 0, и из уравнения они выпадают. Осталась одна неизвестная [[R_B]].',
    ),
  );
  const qA = useQuiz(
    L('Teď chceš hned [[R_Ay]]. Který bod vybereš?', 'Теперь нужна сразу [[R_Ay]]. Какую точку выберешь?'),
    opts(
      2,
      L(
        'Σ[[M_A]]: −8·1 + [[R_B]]·4 = 0 – vypadla právě [[R_Ay]], kterou hledáš. Kterým bodem prochází nositelka druhé svislé neznámé?',
        'Σ[[M_A]]: −8·1 + [[R_B]]·4 = 0 — выпала как раз [[R_Ay]], которую ты ищешь. Через какую точку проходит линия действия другой вертикальной неизвестной?',
      ),
      L('Ke středu mají rameno [[R_Ay]] i [[R_B]] – dvě neznámé v jedné rovnici.', 'Относительно середины плечо есть и у [[R_Ay]], и у [[R_B]] — две неизвестные в одном уравнении.'),
      null,
    ),
    L(
      'Σ[[M_B]] = 0:  −[[R_Ay]] · 4 + 8 · 3 = 0. Nositelka [[R_B]] prochází bodem B, takže [[R_B]] z rovnice vypadne; [[R_Ax]] leží v ose nosníku a bodem B prochází taky.',
      'Σ[[M_B]] = 0:  −[[R_Ay]] · 4 + 8 · 3 = 0. Линия действия [[R_B]] проходит через B, поэтому [[R_B]] из уравнения выпадает; [[R_Ax]] лежит на оси балки и тоже проходит через B.',
    ),
  );
  const solveB = useNumStep(
    L('−8 · 1 + [[R_B]] · 4 = 0. Kolik je [[R_B]]?', '−8 · 1 + [[R_B]] · 4 = 0. Чему равна [[R_B]]?'),
    [numSpec(L('[[R_B]]', '[[R_B]]'), 2, kn)],
    L('[[R_B]] · 4 = 8, tedy [[R_B]] = 2 kN.', '[[R_B]] · 4 = 8, то есть [[R_B]] = 2 кН.'),
    [
      trapNear(L('Ramenem se dělí: [[R_B]] · 4 = 8.', 'На плечо нужно делить: [[R_B]] · 4 = 8.'), 32),
      trapNear(L('To je celá síla F. Rovnice ale říká [[R_B]] · 4 = 8 · 1.', 'Это вся сила F. А уравнение говорит [[R_B]] · 4 = 8 · 1.'), 8),
    ],
    null,
    L('Vyřeš rovnici: kolik je [[R_B]]?', 'Реши уравнение: чему равна [[R_B]]?'),
  );
  const solveA = useNumStep(
    L('−[[R_Ay]] · 4 + 8 · 3 = 0. Kolik je [[R_Ay]]?', '−[[R_Ay]] · 4 + 8 · 3 = 0. Чему равна [[R_Ay]]?'),
    [numSpec(L('[[R_Ay]]', '[[R_Ay]]'), 6, kn)],
    L('[[R_Ay]] · 4 = 24, tedy [[R_Ay]] = 6 kN. Kontrola: [[R_Ay]] + [[R_B]] = 6 + 2 = 8 kN = F ✓', '[[R_Ay]] · 4 = 24, то есть [[R_Ay]] = 6 кН. Проверка: [[R_Ay]] + [[R_B]] = 6 + 2 = 8 кН = F ✓'),
    [
      trapNear(L('Ramenem se dělí: [[R_Ay]] · 4 = 24.', 'На плечо нужно делить: [[R_Ay]] · 4 = 24.'), 96),
      trapNear(L('To je moment 8 · 3. Ještě ho vyděl ramenem 4.', 'Это момент 8 · 3. Его ещё нужно разделить на плечо 4.'), 24),
      trapNear(L('To je [[R_B]]. Teď hledáš svislou reakci v A.', 'Это [[R_B]]. Теперь ищешь вертикальную реакцию в A.'), 2),
    ],
    null,
    L('Vyřeš rovnici: kolik je [[R_Ay]]?', 'Реши уравнение: чему равна [[R_Ay]]?'),
  );
  // 0 bod pro R_B, 1 výpočet R_B, 2 bod pro R_Ay, 3 výpočet R_Ay
  const stage = !qB.solved ? 0 : !solveB.done ? 1 : !qA.solved ? 2 : 3;
  const pick = stage <= 1 ? qB.picked : qA.picked;
  const goal = stage <= 1 ? 0 : 1; // 0 = hledáme R_B, 1 = R_Ay
  const pxs = [xa, xs, xb];
  const quizAsStep = (q: QuizState): Step => ({
    label: q.question,
    done: q.solved,
    result: () => q.Feedback(),
    controls: () => (
      <>
        {q.Options(3)}
        {!q.solved && q.Feedback()}
      </>
    ),
  });
  return (
    <>
      <Stage
        h={220}
        description={t.pick(L('Prostý nosník s reakcemi a volbou bodu', 'Простая балка с реакциями и выбором точки'))}
        onTap={(pt) => {
          const q = stage === 0 ? qB : stage === 2 ? qA : null;
          if (!q) return;
          const i = pxs.reduce((bst, x, k) => (Math.abs(pt.x - x) < Math.abs(pt.x - pxs[bst]) ? k : bst), 0);
          if (Math.abs(pt.x - pxs[i]) < 40 && Math.abs(pt.y - y) < 60) q.pick(i);
        }}
        draw={(b, c) => {
          // uvolněný nosník: podpory jsou nahrazené reakcemi, proto je nekreslíme
          b.beam(xa - 10, xb + 10, y);
          b.force(xa + u, y - 66, xa + u, y - 6, c.load);
          b.text(xa + u + 8, y - 50, ' = 8 kN', c.load, 14, 'start', 'F');
          // reakce; ty, jejichž nositelka prochází zvoleným bodem, zeslábnou (rameno 0)
          const fadeAy = pick === 0;
          const fadeB = pick === 2;
          const fadeAx = pick !== null;
          b.force(xa - 58, y, xa - 6, y, c.react, 2.4, 11, false, fadeAx ? 0.25 : 1);
          b.text(xa - 34, y - 10, null, c.react, 13, 'middle', 'R_Ax', false, fadeAx ? 0.25 : 1);
          b.force(xa, y + 62, xa, y + 6, c.react, 2.4, 11, false, fadeAy ? 0.25 : 1);
          b.text(xa + 8, y + 50, null, c.react, 13, 'start', 'R_Ay', goal === 1, fadeAy ? 0.25 : 1);
          b.force(xb, y + 62, xb, y + 6, c.react, 2.4, 11, false, fadeB ? 0.25 : 1);
          b.text(xb + 8, y + 50, null, c.react, 13, 'start', 'R_B', goal === 0, fadeB ? 0.25 : 1);
          // body k volbě
          const names = ['A', 'S', 'B'];
          pxs.forEach((x, i) => {
            if (pick === i) {
              b.circle(x, y, 10, withAlpha(c.accent, 0.15), c.accent, 1.6);
              b.line(x - 14, y, x + 14, y, c.accent, 1.3);
              b.line(x, y - 14, x, y + 14, c.accent, 1.3);
            } else b.tapSpot(x, y);
            b.text(x + (i === 0 ? 16 : 0), y - 20, names[i], c.ink, 14, i === 0 ? 'start' : 'middle', null, true);
          });
          // kóty
          const yd = y + 92;
          b.line(xa, yd, xb, yd, c.ink, 0.9);
          for (const x of [xa, xa + u, xs, xb]) b.line(x - 4, yd + 4, x + 4, yd - 4, c.ink, 1.3);
          b.text(xa + u / 2, yd - 5, '1 m', c.ink, 13);
          b.text(xa + 1.5 * u, yd - 5, '1 m', c.ink, 13);
          b.text(xa + 3 * u, yd - 5, '2 m', c.ink, 13);
        }}
      />
      <CardText
        text={L(
          'Modré šipky jsou **neznámé síly**, kterými podpory drží nosník (víc v lekcích 6–7). Rovnici ΣM = 0 smíš napsat k libovolnému bodu. Síla, jejíž nositelka bodem prochází, má k němu rameno 0 – a z rovnice vypadne.',
          'Синие стрелки — **неизвестные силы**, которыми опоры держат балку (подробнее в уроках 6–7). Уравнение ΣM = 0 можно записать относительно любой точки. Сила, линия действия которой проходит через точку, имеет относительно неё плечо 0 — и выпадает из уравнения.',
        )}
      />
      <Steps steps={[quizAsStep(qB), solveB, quizAsStep(qA), solveA]} />
    </>
  );
}

/**
 * Karta 3: kontrola. Jiný nosník a obrácená otázka než na kartě 2: k danému bodu – která neznámá vyjde?
 * Pak žák sám spočítá [[R_Ay]].
 */
function Check() {
  const t = useTr();
  const y = 80;
  const xa = 60;
  const u = 70;
  const xb = xa + 4 * u;
  const q = useQuizStep(
    L('Spolužák napsal ΣM = 0 k bodu A. Kterou reakci z ní hned spočítá?', 'Одноклассник записал ΣM = 0 относительно точки A. Какую реакцию он из него сразу найдёт?'),
    [
      opt(
        L('[[R_Ay]]', '[[R_Ay]]'),
        false,
        L('Nositelka [[R_Ay]] prochází bodem A – rameno 0. Objeví se pak [[R_Ay]] v rovnici vůbec?', 'Линия действия [[R_Ay]] проходит через точку A — плечо 0. Войдёт ли тогда [[R_Ay]] в уравнение вообще?'),
      ),
      opt(L('[[R_B]]', '[[R_B]]'), true),
      opt(L('[[R_Ax]]', '[[R_Ax]]'), false, L('[[R_Ax]] leží v ose nosníku a bodem A prochází – rameno 0, z rovnice vypadne.', '[[R_Ax]] лежит на оси балки и проходит через A — плечо 0, из уравнения выпадает.')),
    ],
    L(
      'Bodem A procházejí [[R_Ax]] i [[R_Ay]], v rovnici zbude jen [[R_B]]: −6 · 3 + [[R_B]] · 4 = 0.',
      'Через A проходят и [[R_Ax]], и [[R_Ay]], в уравнении остаётся только [[R_B]]: −6 · 3 + [[R_B]] · 4 = 0.',
    ),
    null,
    3,
  );
  const n = useNumStep(
    L('Teď spočítej [[R_Ay]] – bod si vyber sám', 'Теперь найди [[R_Ay]] — точку выбери сам'),
    [numSpec(L('[[R_Ay]]', '[[R_Ay]]'), 1.5, t.pick(L('kN', 'кН')))],
    L(
      'K bodu B: −[[R_Ay]] · 4 + 6 · 1 = 0, tedy [[R_Ay]] = 1,5 kN. Kontrola: [[R_B]] = 6 · 3 / 4 = 4,5 kN a 1,5 + 4,5 = 6 kN = F ✓',
      'Относительно B: −[[R_Ay]] · 4 + 6 · 1 = 0, то есть [[R_Ay]] = 1,5 кН. Проверка: [[R_B]] = 6 · 3 / 4 = 4,5 кН и 1,5 + 4,5 = 6 кН = F ✓',
    ),
    [
      trapNear(L('To je reakce v B (z rovnice k bodu A). Hledáš ale svislou reakci v A.', 'Это реакция в B (из уравнения относительно A). А ищешь вертикальную реакцию в A.'), 4.5),
      trapNear(
        L(
          'Celou sílu nese A, jen když působí přímo nad ní. Tady je síla blíž k B – a nezapomeň dělit ramenem 4 m.',
          'Всю силу опора A несёт, только если та приложена прямо над ней. Здесь сила ближе к B — и не забудь разделить на плечо 4 м.',
        ),
        6,
      ),
      trapNear(
        L('To je moment 6 · 3 k bodu A. Ještě ho vyděl ramenem – a je to reakce v B, ne v A.', 'Это момент 6 · 3 относительно A. Его ещё нужно разделить на плечо — и это реакция в B, а не в A.'),
        18,
      ),
    ],
    L(
      'Napiš ΣM = 0 k bodu B – tam vypadne [[R_B]]. Nebo dopočítej [[R_B]] z rovnice k bodu A a použij Σ[[F_y]] = 0.',
      'Запиши ΣM = 0 относительно B — там выпадет [[R_B]]. Или найди [[R_B]] из уравнения относительно A и воспользуйся Σ[[F_y]] = 0.',
    ),
  );
  return (
    <>
      <Stage
        h={190}
        description={t.pick(L('Prostý nosník se silou F ve 3 m od podpory A', 'Простая балка с силой F в 3 м от опоры A'))}
        draw={(b, c) => {
          b.beam(xa - 10, xb + 10, y);
          b.support('pin', xa, y, 'A');
          b.support('roller', xb, y, 'B');
          b.force(xa + 3 * u, y - 58, xa + 3 * u, y - 6, c.load);
          b.text(xa + 3 * u - 8, y - 44, ' = 6 kN', c.load, 14, 'end', 'F');
          // kóty
          const yd = y + 82;
          b.line(xa, yd, xb, yd, c.ink, 0.9);
          for (const x of [xa, xa + 3 * u, xb]) b.line(x - 4, yd + 4, x + 4, yd - 4, c.ink, 1.3);
          b.text(xa + 1.5 * u, yd - 5, '3 m', c.ink, 13);
          b.text(xa + 3.5 * u, yd - 5, '1 m', c.ink, 13);
        }}
      />
      <CardText
        text={L(
          'Nosník o rozpětí 4 m, síla 6 kN působí 1 m od podpory B. Podpora A má reakce [[R_Ax]], [[R_Ay]], podpora B jen [[R_B]].',
          'Балка пролётом 4 м, сила 6 кН приложена в 1 м от опоры B. У опоры A реакции [[R_Ax]], [[R_Ay]], у опоры B — только [[R_B]].',
        )}
      />
      <Steps steps={[q, n]} />
    </>
  );
}

export const cards = [Seesaw, Lever, Raft, ChoosePoint, Check];
