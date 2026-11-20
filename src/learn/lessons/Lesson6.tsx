/*
 * Lekce 6 – Vazby brání pohybu. Myšlenka: pohybu, který vazba nedovolí, brání reakcí (proti pokusu).
 * Každý zakázaný pohyb (posun ⟷, zvednutí ↑↓, otočení ↻) = jedna neznámá reakce.
 * (Přepis z aplikace: learn/Lesson6.kt.)
 */
import type { ReactNode } from 'react';
import type { SupportType } from '../../model/types';
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
  opt,
  step,
  tween,
  useKit,
  useLaunched,
  useQuiz,
  useQuizStep,
  useTr,
  type Colors,
  type Job,
  type PrimBuilder,
  type Step,
} from '../kit';

type Motion = 0 | 1 | 2; // posun, zvednutí, pootočení
const MOTIONS: Motion[] = [0, 1, 2];
const SLIDE = 0;
const LIFT = 1;
const ROTATE = 2;

const TYPES: SupportType[] = ['pin', 'roller', 'fixed'];

/** Které pohyby vazba nedovolí (oboustranná vazba: zvednutí brání i válečky). */
const blocks = (t: SupportType, m: Motion) => (t === 'pin' ? m !== ROTATE : t === 'roller' ? m === LIFT : true);

function rot6(x: number, y: number, px: number, py: number, deg: number): [number, number] {
  const a = (deg * Math.PI) / 180;
  const dx = x - px;
  const dy = y - py;
  return [px + dx * Math.cos(a) - dy * Math.sin(a), py + dx * Math.sin(a) + dy * Math.cos(a)];
}

/**
 * Posuvná podpora kreslená zvlášť: trojúhelník s válečky na [x], kolejnice (pevně na [gx]) a pod ní podklad.
 * Válečky jsou ve vedení zajištěné sponami pod kolejnicí – nosník po kolejnici jezdí, ale nadzvednout nejde.
 */
function rollerAt(b: PrimBuilder, x: number, gx: number, y: number, c: Colors, label: string, a = 1) {
  const top = y + 5;
  const base = top + 4 + 20;
  // kolejnice a podklad
  b.rect(gx - 70, base + 7, 120, 4, c.subtle, c.ink, 1.2, a);
  b.hatch(gx - 70, gx + 50, base + 22, a);
  for (const sx of [gx - 55, gx + 35]) b.line(sx, base + 11, sx, base + 22, c.ink, 1.2, false, a);
  // vozík: trojúhelník, válečky na kolejnici, spony pod ní
  b.poly(
    [
      [x, top + 4],
      [x - 13, base],
      [x + 13, base],
    ],
    c.paper,
    c.ink,
    1.5,
    a,
  );
  b.circle(x, top + 4, 4, c.paper, c.ink, 1.5, a);
  b.circle(x - 7, base + 3.5, 3.2, c.paper, c.ink, 1.3, a);
  b.circle(x + 7, base + 3.5, 3.2, c.paper, c.ink, 1.3, a);
  for (const sgn of [-1, 1]) {
    const ex = x + sgn * 13;
    b.line(ex, base, ex, base + 15, c.ink, 1.4, false, a);
    b.line(ex, base + 15, ex - sgn * 7, base + 15, c.ink, 1.4, false, a);
  }
  if (label) b.text(x + 22, top + 22, label, c.ink, 16, 'start', null, true, a);
}

const MOTION_NAMES = [L('⟷ posun vodorovně', '⟷ сдвиг по горизонтали'), L('↑↓ posun svisle', '↑↓ сдвиг по вертикали'), L('↺ pootočení', '↺ поворот')];
const MOTION_SHORT = ['⟷', '↑↓', '↺'];
const DISP: Record<Motion, [number, number, number]> = { 0: [-34, 0, 0], 1: [0, -26, 0], 2: [0, 0, -14] };

/**
 * Karta 0: zkouška pohybu. Nejdřív ukázka: volný nosník ruka posune, zvedne a pootočí. Pak žák u každé podpory tipne,
 * které pohyby nedovolí, a „Vyzkoušej“ provede tři zkoušky. Dovolený pohyb ruka provede, zakázaný jen cukne
 * a objeví se reakce.
 */
function PushTest() {
  const t = useTr();
  const kit = useKit();
  const demo = kit.mut(true); // ukázka s volným nosníkem
  const demoDone = kit.mut(false);
  const type = kit.mut(0);
  const pred = kit.mut<Record<number, Set<Motion>>>({});
  const motion = kit.mut<Motion | null>(null); // právě zkoušený pohyb
  const pushing = kit.mut(false);
  const running = kit.mut(false);
  const found = kit.mut<Set<Motion>>(new Set()); // zakázané pohyby zjištěné při posledním běhu
  const shown = kit.mut<number | null>(null); // u které podpory se zkouška dokončila
  const okTypes = kit.mut<Set<number>>(new Set());
  const lastOk = kit.mut<boolean | null>(null);
  const lastPred = kit.mut<Set<Motion>>(new Set());
  // posunutí nosníku po dovoleném pohybu (zůstane, dokud se neobjeví nový nosník)
  const disp = kit.mut<[number, number, number]>([0, 0, 0]);
  const p = kit.anim(0);
  const fade = kit.anim(1);
  const jobBox = kit.remember<{ job: Job | null }>(() => ({ job: null }));
  // 0 ukázka, 1 pevná kloubová, 2 posuvná kloubová, 3 vetknutí, 4 hotovo
  const stage = !demoDone.v ? 0 : !okTypes.v.has(0) ? 1 : !okTypes.v.has(1) ? 2 : !okTypes.v.has(2) ? 3 : 4;
  const wantType = Math.min(2, Math.max(0, stage - 1));
  const st = TYPES[type.v];
  const fixed = st === 'fixed' && !demo.v;
  const y = 112;
  const xs = 100;
  const x1 = fixed ? xs : xs - 10;
  const x2 = 340;
  // volný nosník se otáčí kolem svého středu, podepřený kolem podpory
  const pivot = demo.v ? (x1 + x2) / 2 : xs;
  const moved = () => disp.v[0] !== 0 || disp.v[1] !== 0 || disp.v[2] !== 0;
  const freshBeam = async () => {
    if (moved()) {
      await fade.animateTo(0, tween(160));
      disp.v = [0, 0, 0];
      await fade.animateTo(1, tween(220));
    }
  };
  const reset = (then: () => void) => {
    jobBox.job?.cancel();
    running.v = false;
    pushing.v = false;
    motion.v = null;
    then();
    jobBox.job = kit.scope.launch(async () => {
      await p.snapTo(0);
      await freshBeam();
      await fade.snapTo(1);
    });
  };
  useLaunched([], async (job) => {
    // ukázka: volný nosník jde posunout, zvednout i pootočit (tíhu vynecháme)
    await job.delay(800);
    running.v = true;
    for (const m of MOTIONS) {
      await freshBeam();
      motion.v = m;
      pushing.v = true;
      await p.snapTo(0);
      await job.run(p.animateTo(1, tween(900)));
      pushing.v = false;
      disp.v = DISP[m];
      await job.delay(600);
    }
    motion.v = null;
    running.v = false;
    await job.run(fade.animateTo(0, tween(200)));
    disp.v = [0, 0, 0];
    demo.v = false;
    demoDone.v = true;
    await fade.animateTo(1, tween(260));
  });
  // po splnění podpory se obrázek prolne na další
  useLaunched([wantType], async (job) => {
    if (type.v === wantType) return;
    await job.delay(1400);
    jobBox.job?.cancel();
    running.v = false;
    pushing.v = false;
    motion.v = null;
    await job.run(fade.animateTo(0, tween(200)));
    type.v = wantType;
    disp.v = [0, 0, 0];
    found.v = new Set();
    shown.v = null;
    lastOk.v = null;
    await fade.animateTo(1, tween(260));
  });
  const run = () => {
    jobBox.job?.cancel();
    const mine = pred.v[type.v] ?? new Set<Motion>();
    const tt = type.v;
    running.v = true;
    found.v = new Set();
    shown.v = null;
    lastOk.v = null;
    jobBox.job = kit.scope.launch(async (job) => {
      for (const m of MOTIONS) {
        await freshBeam();
        job.check();
        motion.v = m;
        pushing.v = true;
        await p.snapTo(0);
        await job.run(p.animateTo(1, tween(850)));
        pushing.v = false;
        if (blocks(TYPES[tt], m)) found.v = new Set([...found.v, m]);
        else disp.v = DISP[m];
        await job.delay(450);
      }
      motion.v = null;
      running.v = false;
      shown.v = tt;
      const actual = MOTIONS.filter((x) => blocks(TYPES[tt], x));
      lastPred.v = mine;
      const ok = actual.length === mine.size && actual.every((x) => mine.has(x));
      lastOk.v = ok;
      if (ok) okTypes.v = new Set([...okTypes.v, tt]);
    });
  };
  const testStep = (ti: number, label: L, note: L, extra: () => ReactNode = () => null): Step => ({
    label,
    done: okTypes.v.has(ti),
    result: () => {
      const n = MOTIONS.filter((m) => blocks(TYPES[ti], m)).length;
      return (
        <>
          <Readout text={t.pick(L(`Zakázané pohyby: ${n} → reakce: ${n}`, `Запрещено движений: ${n} → реакций: ${n}`))} ok />
          <Note tone="ok" text={note} />
          {extra()}
        </>
      );
    },
    controls: () => {
      const mine = pred.v[ti] ?? new Set<Motion>();
      let wrong: ReactNode = null;
      if (shown.v === ti && lastOk.v === false && !running.v) {
        const actual = new Set(MOTIONS.filter((m) => blocks(TYPES[ti], m)));
        const stopped = [...actual].filter((m) => !lastPred.v.has(m)).map((m) => MOTION_SHORT[m]);
        const movedM = [...lastPred.v].filter((m) => !actual.has(m)).map((m) => MOTION_SHORT[m]);
        const cs = [stopped.length ? `${stopped.join(', ')} podpora nedovolila (modrá reakce)` : null, movedM.length ? `${movedM.join(', ')} se nosník pohnul` : null].filter(Boolean).join('; ');
        const ru = [stopped.length ? `${stopped.join(', ')} опора не допустила (синяя реакция)` : null, movedM.length ? `${movedM.join(', ')} балка сдвинулась` : null].filter(Boolean).join('; ');
        wrong = <SmallText text={L(`Tip nesedí: ${cs}. Oprav tip a vyzkoušej znovu.`, `Прогноз не совпал: ${ru}. Исправь прогноз и проверь снова.`)} />;
      }
      return (
        <>
          <Chips
            options={MOTION_NAMES}
            selectedSet={new Set([...mine])}
            onPick={(i) => {
              if (running.v || type.v !== ti) return;
              const mm = i as Motion;
              const now = new Set(mine);
              if (now.has(mm)) now.delete(mm);
              else now.add(mm);
              reset(() => {
                pred.v = { ...pred.v, [ti]: now };
                found.v = new Set();
                shown.v = null;
                lastOk.v = null;
              });
            }}
          />
          <ActionButton text={L('Vyzkoušej', 'Проверить')} enabled={!running.v && type.v === ti} onClick={run} />
          {wrong}
        </>
      );
    },
  });
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Nosník na podpoře, zkouška pohybu', 'Балка на опоре, проверка движения'))}
        draw={(b, c) => {
          const m = motion.v;
          const v = p.value;
          const a = fade.value;
          const blocked = m !== null && !demo.v && blocks(st, m);
          // zakázaný pohyb jen „cukne“, dovolený ruka plynule provede (rozjezd i dojezd)
          const shake = blocked && pushing.v ? Math.sin(v * Math.PI * 5) * 3 * (1 - v) : 0;
          const e = v * v * (3 - 2 * v);
          let [dx, dy, deg] = disp.v;
          if (m !== null && pushing.v) {
            if (m === SLIDE) dx = blocked ? -shake : -34 * e;
            else if (m === LIFT) dy = blocked ? -shake : -26 * e;
            else deg = blocked ? shake * 0.6 : -14 * e;
          }
          if (!demo.v) {
            if (st === 'roller') rollerAt(b, xs + dx, xs, y, c, 'A', a);
            // vozík jede s nosníkem, kolejnice stojí
            else if (st === 'fixed') {
              b.support('fixed', xs, y, '', false, true);
              b.text(xs - 14, y - 42, 'A', c.ink, 16, 'end', null, true);
            } else b.support(st, xs, y, 'A');
          }
          const pts = (
            [
              [x1, y - 5],
              [x2, y - 5],
              [x2, y + 5],
              [x1, y + 5],
            ] as [number, number][]
          ).map(([x, yy]): [number, number] => {
            const [rx, ry] = rot6(x, yy, pivot, y, deg);
            return [rx + dx, ry + dy];
          });
          b.poly(pts, c.beam, c.ink, 1.6, a);
          // červeně = zkouška (ruka): síla nositelkou přes podporu (u volného nosníku přes střed), nebo čistý moment
          if (m !== null && pushing.v) {
            const lx = demo.v ? pivot : xs + (fixed ? 6 : 0);
            if (m === SLIDE) b.force(394 + dx, y, 346 + dx, y, c.load);
            else if (m === LIFT) b.force(lx, y - 7 + dy, lx, y - 62 + dy, c.load);
            else b.moment(demo.v ? pivot : xs + 70, y + dy, 22, true, c.load);
          }
          if (demo.v) {
            if (m !== null) b.text(200, 30, t.pick([L('⟷ posun', '⟷ сдвиг'), L('↑↓ zvednutí', '↑↓ подъём'), L('↺ pootočení', '↺ поворот')][m]), c.load, 15, 'middle', null, true);
            b.text(392, 220, t.pick(L('volný nosník bez podpory – tíhu vynecháme', 'свободная балка без опоры — вес не учитываем')), c.inkSoft, 11, 'end');
            return;
          }
          // modře = reakce u zakázaných pohybů: proti zkoušce
          for (const f of found.v) {
            if (f === SLIDE) {
              if (fixed) {
                b.force(xs + 4, y + 22, xs + 58, y + 22, c.react);
                b.text(xs + 64, y + 27, null, c.react, 14, 'start', 'R_x');
              } else {
                b.force(x1 - 58, y, x1 - 2, y, c.react);
                b.text(x1 - 32, y - 10, null, c.react, 14, 'middle', 'R_x');
              }
            } else if (f === LIFT) {
              const lx = xs + (fixed ? 6 : 0);
              b.force(lx, y - 64, lx, y - 7, c.react);
              if (fixed) b.text(lx + 8, y - 52, null, c.react, 14, 'start', 'R_y');
              else b.text(lx - 8, y - 50, null, c.react, 14, 'end', 'R_y');
            } else {
              b.moment(xs + 70, y, 22, false, c.react);
              b.text(xs + 70, y - 32, null, c.react, 14, 'middle', 'M');
            }
          }
          b.text(392, 220, t.pick(L('zkouška pohybu – zatížení vynecháno', 'проверка движения — нагрузка не показана')), c.inkSoft, 11, 'end');
        }}
      />
      <CardText
        text={L(
          'Podpora nosníku některé pohyby dovolí a jiné ne. Pohyb, který nedovolí, zastaví silou – **reakcí** (modře), která míří proti pokusu.',
          'Опора одни движения балки допускает, а другие нет. Движение, которое она не допускает, она останавливает силой — **реакцией** (синяя), направленной против попытки.',
        )}
      />
      <Steps
        steps={[
          step(L('Sleduj: volný nosník jde posunout, zvednout i pootočit', 'Смотри: свободную балку можно сдвинуть, поднять и повернуть'), demoDone.v, () => (
            <SmallText text={L('Ruka zkouší tři pohyby…', 'Рука пробует три движения…')} />
          )),
          testStep(
            0,
            L('Pevná kloubová podpora: označ pohyby, které **nedovolí**', 'Шарнирно-неподвижная опора: отметь движения, которые она **не допускает**'),
            L(
              'Tip sedí. Kloub nedovolí posun ani zvednutí – vzniknou dvě reakce [[R_x]], [[R_y]]. Pootočení dovolí, reakční moment tu není.',
              'Прогноз верный. Шарнир не допускает ни сдвига, ни подъёма — возникают две реакции [[R_x]], [[R_y]]. Поворот допускает, реактивного момента нет.',
            ),
          ),
          testStep(
            1,
            L('Posuvná kloubová podpora: označ pohyby, které **nedovolí**', 'Шарнирно-подвижная опора: отметь движения, которые она **не допускает**'),
            L(
              'Tip sedí. Válečky jezdí po kolejnici a kloub se natočí – brání jen pohybu kolmo ke kolejnici: jedna reakce.',
              'Прогноз верный. Катки ездят по рельсу, шарнир поворачивается — опора препятствует только движению перпендикулярно рельсу: одна реакция.',
            ),
          ),
          testStep(
            2,
            L('Vetknutí: označ pohyby, které **nedovolí**', 'Жёсткая заделка: отметь движения, которые она **не допускает**'),
            L('Tip sedí. Zeď nedovolí nic – dvě síly a reakční **moment** [[M]].', 'Прогноз верный. Стена не допускает ничего — две силы и реактивный **момент** [[M]].'),
            () => (
              <SmallText
                text={L(
                  'Pevná kloubová podpora: 2 reakce. Posuvná kloubová: 1. Vetknutí: 3 (dvě síly a moment). Podporám se společně říká **vazby**.',
                  'Шарнирно-неподвижная опора: 2 реакции. Шарнирно-подвижная: 1. Жёсткая заделка (vetknutí): 3 (две силы и момент). Все опоры вместе называют **связями** (vazby).',
                )}
              />
            ),
          ),
        ]}
      />
      {/* bez spon by „zvednutí“ u válečků byl chyták – vysvětlení hned při vstupu do kroku */}
      {stage >= 2 && (
        <SmallText
          text={L(
            'Válečky posuvné podpory jsou v kolejnici zajištěné sponami (jako šuplík ve výsuvu): jezdí sem a tam, ale nahoru nosník nepustí.',
            'Катки подвижной опоры закреплены в рельсе скобами (как ящик стола в направляющих): ездят туда-сюда, но вверх балку не отпускают.',
          )}
        />
      )}
    </>
  );
}

const ORDER = [2, 1, 0];

/**
 * Karta 1: pravidlo a přiřazení sad reakcí ke značkám podpor. Tři podpory vedle sebe,
 * u vyřešené narostou modré šipky reakcí.
 */
function MatchSets() {
  const tt = useTr();
  const short = [L('pevná kloubová', 'шарн.-неподв.'), L('posuvná kloubová', 'шарн.-подв.'), L('vetknutí', 'заделка')];
  const opts = [L('[[R_Ax]], [[R_Ay]]', '[[R_Ax]], [[R_Ay]]'), L('jen [[R_A]] (kolmo k podložce)', 'только [[R_A]] (перпендикулярно опорной поверхности)'), L('[[R_Ax]], [[R_Ay]], [[M_A]]', '[[R_Ax]], [[R_Ay]], [[M_A]]')];
  const quizzes = ORDER.map((ti) =>
    // eslint-disable-next-line react-hooks/rules-of-hooks
    useQuiz(
      ti === 0
        ? L('Jaké reakce má pevná kloubová podpora?', 'Какие реакции у шарнирно-неподвижной опоры?')
        : ti === 1
          ? L('Jaké reakce má posuvná kloubová podpora?', 'Какие реакции у шарнирно-подвижной опоры?')
          : L('Jaké reakce má vetknutí?', 'Какие реакции у жёсткой заделки?'),
      opts.map((o, i) => opt(o, i === ti)),
      ti === 0
        ? L('Ano – pevná kloubová podpora nedovolí posun vodorovně ani svisle, natočení ale dovolí.', 'Да — шарнирно-неподвижная опора не допускает смещения ни по горизонтали, ни по вертикали, но поворот допускает.')
        : ti === 1
          ? L(
              'Ano – válečky dovolí pohyb podél podložky (plochy, po které jezdí) i natočení. Brání jen pohybu kolmo k podložce.',
              'Да — катки допускают сдвиг вдоль опорной поверхности (по ней они катятся) и поворот. Препятствуют только смещению перпендикулярно опорной поверхности.',
            )
          : L('Ano – vetknutí nedovolí žádný pohyb: dvě síly a moment.', 'Да — жёсткая заделка не допускает никакого движения: две силы и момент.'),
      ti === 0
        ? L('Zkus nosníkem v duchu posunout, zvednout a natočit. Co z toho podpora nedovolí?', 'Мысленно попробуй сдвинуть, поднять и повернуть балку. Что из этого опора не допустит?')
        : ti === 1
          ? L('Všimni si válečků: kterým směrem se po nich nosník volně posune?', 'Обрати внимание на катки: в каком направлении балка по ним свободно сдвигается?')
          : L('Nosník je zapuštěný do zdi. Může se v ní natočit?', 'Балка заделана в стену. Может ли она в ней повернуться?'),
    ),
  );
  const cur = quizzes.findIndex((q) => !q.solved);
  return (
    <>
      <Stage
        h={170}
        description={tt.pick(L('Tři značky podpor vedle sebe', 'Три условных обозначения опор рядом'))}
        draw={(b, c) => {
          const y = 78;
          ORDER.forEach((ti, k) => {
            const x0 = 12 + k * 128;
            const active = k === cur;
            const solved = quizzes[k].solved;
            const future = cur >= 0 && k > cur;
            if (active) b.rect(x0 - 4, 10, 124, 150, withAlpha(c.react, 0.06), c.react, 1.0);
            const ba = future ? 0.35 : 1;
            const tp = TYPES[ti];
            if (tp === 'fixed') {
              const xs = x0 + 22;
              b.support('fixed', xs, y, '', future, true);
              b.beam(xs, x0 + 112, y, ba);
              if (solved) {
                b.force(xs + 6, y - 56, xs + 6, y - 7, c.react);
                b.text(xs + 14, y - 44, null, c.react, 13, 'start', 'R_Ay');
                b.force(xs + 4, y + 22, xs + 46, y + 22, c.react);
                b.text(xs + 25, y + 42, null, c.react, 13, 'middle', 'R_Ax');
                b.moment(xs + 70, y, 16, false, c.react);
                b.text(xs + 70, y - 24, null, c.react, 13, 'middle', 'M_A');
              }
            } else if (tp === 'roller') {
              const xs = x0 + 50;
              b.support('roller', xs, y, '', future);
              b.beam(x0 + 14, x0 + 112, y, ba);
              if (solved) {
                b.force(xs, y - 56, xs, y - 7, c.react);
                b.text(xs + 8, y - 44, null, c.react, 13, 'start', 'R_A');
              }
            } else {
              const xs = x0 + 46;
              b.support('pin', xs, y, '', future);
              b.beam(xs - 6, x0 + 116, y, ba);
              if (solved) {
                b.force(xs, y - 56, xs, y - 7, c.react);
                b.text(xs + 8, y - 44, null, c.react, 13, 'start', 'R_Ay');
                b.force(xs - 40, y, xs - 8, y, c.react);
                b.text(xs - 24, y - 10, null, c.react, 13, 'middle', 'R_Ax');
              }
            }
            b.text(x0 + 58, 150, tt.pick(short[ti]), active ? c.ink : c.inkSoft, 12, 'middle', null, active);
          });
        }}
      />
      <CardText text={L('Pravidlo: **každý zakázaný pohyb = jedna reakce**.', 'Правило: **каждое запрещённое движение = одна реакция**.')} />
      <Steps
        steps={quizzes.map(
          (q): Step => ({
            label: q.question,
            done: q.solved,
            result: () => q.Feedback(),
            controls: () => (
              <>
                {q.Options()}
                {!q.solved && q.Feedback()}
              </>
            ),
          }),
        )}
      />
    </>
  );
}

/** Karta 2: kvíz – která vazba brání otočení, a kolik neznámých reakcí má prostý nosník. */
function WhichRotation() {
  const tt = useTr();
  const q1 = useQuizStep(
    L('Která podpora nedovolí nosníku se **natočit**?', 'Какая опора не даёт балке **повернуться**?'),
    [
      opt(L('1 – pevná kloubová', '1 — шарнирно-неподвижная'), false, L('Je to kloub – a kloub natočení dovolí. Proto nemá reakční moment.', 'Это шарнир — а шарнир поворот допускает. Поэтому реактивного момента нет.')),
      opt(
        L('2 – posuvná kloubová', '2 — шарнирно-подвижная'),
        false,
        L('Válečky a kloub: dovolí pohyb podél podložky i natočení. Brání jen pohybu kolmo k podložce.', 'Катки и шарнир: допускают сдвиг вдоль опорной поверхности и поворот. Препятствуют только смещению перпендикулярно ей.'),
      ),
      opt(L('3 – vetknutí', '3 — жёсткая заделка'), true),
    ],
    L('Vetknutí drží nosník pevně ve zdi – proto má kromě dvou sil i reakční moment [[M]].', 'Жёсткая заделка неподвижно держит балку в стене — поэтому кроме двух сил у неё есть ещё реактивный момент [[M]].'),
  );
  const q2 = useQuizStep(
    L('Kolik **neznámých reakcí** má dolní nosník celkem?', 'Сколько всего **неизвестных реакций** у нижней балки?'),
    [
      opt(L('2', '2'), false, L('Kolik reakcí má samotná pevná kloubová podpora? A k tomu ještě B.', 'Сколько реакций у одной только шарнирно-неподвижной опоры? И к ним ещё B.')),
      opt(L('3', '3'), true),
      opt(L('4', '4'), false, L('Posuvná podpora B brání jen jednomu pohybu – kolik reakcí tedy má?', 'Подвижная опора B препятствует только одному движению — сколько у неё реакций?')),
      opt(L('5', '5'), false, L('Žádná z podpor nebrání natočení – reakční moment tu není.', 'Ни одна из опор не препятствует повороту — реактивного момента нет.')),
    ],
    L(
      'A: [[R_Ax]], [[R_Ay]] (2) + B: [[R_B]] (1) = **3 neznámé**. A v rovině máme právě 3 rovnice rovnováhy (lekce 5) – proto takový nosník spočítáš.',
      'A: [[R_Ax]], [[R_Ay]] (2) + B: [[R_B]] (1) = **3 неизвестные**. А на плоскости у нас ровно 3 уравнения равновесия (урок 5) — поэтому такую балку можно рассчитать.',
    ),
    null,
    4,
  );
  const second = q1.done;
  return (
    <>
      <Stage
        h={250}
        description={tt.pick(L('Tři druhy podpor a nosník na dvou podporách', 'Три вида опор и балка на двух опорах'))}
        draw={(b, c) => {
          const y = 78;
          const ta = second ? 0.35 : 1;
          // každá podpora na krátkém kousku nosníku, číslo nad ní
          b.beam(40, 110, y, ta);
          b.support('pin', 75, y, '', second);
          b.text(75, y - 22, '1', c.ink, 17, 'middle', null, true, ta);
          b.beam(165, 235, y, ta);
          b.support('roller', 200, y, '', second);
          b.text(200, y - 22, '2', c.ink, 17, 'middle', null, true, ta);
          b.support('fixed', 300, y, '', second, true);
          b.beam(300, 370, y, ta);
          b.text(335, y - 22, '3', c.ink, 17, 'middle', null, true, ta);
          // druhá otázka: celý nosník na dvou podporách
          const yb = 200;
          b.line(20, 150, 380, 150, c.line, 1.0);
          if (second) b.rect(40, 156, 320, 88, withAlpha(c.react, 0.06), c.react, 1.0);
          const ba = second ? 1 : 0.5;
          b.beam(70, 330, yb, ba);
          b.support('pin', 80, yb, 'A', !second);
          b.support('roller', 320, yb, 'B', !second);
          b.force(200, yb - 40, 200, yb - 6, c.load, 2.4, 11, false, ba);
          b.text(208, yb - 26, null, c.load, 14, 'start', 'F', false, ba);
        }}
      />
      <CardText text={L('Nahoře tři podpory, dole nosník na pevné kloubové podpoře A a posuvné B.', 'Вверху три опоры, внизу балка на шарнирно-неподвижной опоре A и подвижной B.')} />
      <Steps steps={[q1, q2]} />
    </>
  );
}

export const cards = [PushTest, MatchSets, WhichRotation];
