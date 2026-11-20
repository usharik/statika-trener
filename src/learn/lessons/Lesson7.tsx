/*
 * Lekce 7 – Uvolnění. Hlavní myšlenka: podpora drží nosník jen tím, že na něj působí silou.
 * Bez ní nosník opravdu spadne (zrychluje, dokud nedopadne). Uvolnění je pak model na papíře:
 * podporu nahradíme silou (reakcí) a nosník „nic nepozná“. (Přepis z aplikace: learn/Lesson7.kt.)
 */
import type { ReactNode } from 'react';
import { Rich } from '../../components/Sym';
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
  ValueSlider,
  clamp,
  dist,
  num,
  numSpec,
  numTrap,
  opt,
  step,
  trapNear,
  tween,
  useCardState,
  useKit,
  useLaunched,
  useNum,
  useQuiz,
  useQuizStep,
  useTr,
  type Colors,
  type Job,
  type NumState,
  type PrimBuilder,
  type QuizState,
  type Step,
} from '../kit';

// ------------------------------------------------------------------ kroky se stavem venku (obrázek čte volbu)

/** Kvíz jako krok; stav drží karta, takže obrázek může reagovat na picked / solved. */
function quizStep7(q: QuizState, pickL: (l: L) => string, columns = 1, label: L = q.question, more?: () => ReactNode): Step {
  return {
    label,
    done: q.solved,
    result: () => (
      <>
        {q.Feedback()}
        {more?.()}
      </>
    ),
    controls: () => (
      <>
        {label.cs !== q.question.cs && (
          <p className="dock-q">
            <Rich text={pickL(q.question)} />
          </p>
        )}
        {q.Options(columns)}
        {!q.solved && q.Feedback()}
      </>
    ),
  };
}

/** Číselná odpověď jako krok; [show] = co má žák během počítání vidět v doku (rovnice). */
function numStep7(n: NumState, label: L, show?: () => ReactNode, more?: () => ReactNode): Step {
  return {
    label,
    done: n.solved,
    result: () => (
      <>
        {n.Feedback()}
        {more?.()}
      </>
    ),
    controls: () => (
      <>
        {show?.()}
        {n.Fields()}
        {n.Buttons()}
        {!n.solved && n.Feedback()}
      </>
    ),
  };
}

/** Tip před pokusem: volba + potvrzení. Správnost ukáže až pokus v dalším kroku. */
function useTip7(label: L, options: L[], guess: number | null, onGuess: (i: number) => void): Step {
  const kit = useKit();
  const sel = kit.mut<number | null>(null);
  return {
    label,
    done: guess !== null,
    result: () => (guess !== null ? <SmallText text={L('Tvůj tip: ' + options[guess].cs, 'Твой прогноз: ' + options[guess].ru)} /> : null),
    controls: () => (
      <>
        <Chips options={options} selected={sel.v} onPick={(i) => (sel.v = i)} />
        <ActionButton text={L('Potvrdit tip', 'Подтвердить прогноз')} enabled={sel.v !== null} onClick={() => sel.v !== null && onGuess(sel.v)} />
      </>
    ),
  };
}

/** Otočení bodu (x, y) kolem (px, py) o úhel [deg] (stupně, ve směru osy y dolů). */
function rot(x: number, y: number, px: number, py: number, deg: number): [number, number] {
  const a = (deg * Math.PI) / 180;
  const dx = x - px;
  const dy = y - py;
  return [px + dx * Math.cos(a) - dy * Math.sin(a), py + dx * Math.sin(a) + dy * Math.cos(a)];
}

/** Nosník jako otočený obdélník (aby mohl padat). */
function tiltedBeam(b: PrimBuilder, x1: number, x2: number, y: number, px: number, deg: number, c: Colors, a = 1) {
  const pts = (
    [
      [x1, y - 5],
      [x2, y - 5],
      [x2, y + 5],
      [x1, y + 5],
    ] as [number, number][]
  ).map(([x, yy]) => rot(x, yy, px, y, deg));
  b.poly(pts, c.beam, c.ink, 1.6, a);
}

/** Podlaha: čára se šrafami. */
function ground(b: PrimBuilder, x1: number, x2: number, y: number, c: Colors, a = 1) {
  b.line(x1, y, x2, y, c.ink, 1.5, false, a);
  for (let x = x1 + 6; x <= x2; x += 8) b.line(x, y, x - 6, y + 7, c.inkSoft, 1.0, false, a);
}

/**
 * Otáčení z klidu se stálým úhlovým zrychlením [alpha] (rad/s², znaménko = smysl), dokud úhel nedosáhne
 * [limitDeg] (dopad na podlahu / zarážku). [onDeg] dostává úhel ve stupních každý snímek.
 */
async function swing(job: Job, alpha: number, limitDeg: number, onDeg: (d: number) => void) {
  const limit = (Math.abs(limitDeg) * Math.PI) / 180;
  let th = 0;
  let w = 0;
  let last = 0;
  while (Math.abs(th) < limit) {
    const now = await job.frame();
    const dt = last === 0 ? 0 : Math.min((now - last) / 1000, 0.05);
    last = now;
    w += alpha * dt;
    th += w * dt;
    onDeg((clamp(th, -limit, limit) * 180) / Math.PI);
  }
}

/**
 * Karta 1: odstraň podporu B → nosník se kolem A otáčí čím dál rychleji a dopadne na zem.
 * Pak „uvolnění“ jako model: podpory nahradí žák správnými reakcemi a spočítá je (F = 12 kN v ¼ rozpětí → 9 a 3 kN).
 */
function CutItFree() {
  const t = useTr();
  const kit = useKit();
  const y = 78;
  const floor = 200;
  const xa = 70;
  const xb = 330;
  const m = (xb - xa) / 4; // rozpětí 4 m
  const xf = xa + m; // F = 12 kN 1 m od A
  const kn = t.pick(L('kN', 'кН'));
  // 0 = stojí, 1 = padá, 2 = leží na zemi, 3 = model (uvolnění)
  const phase = kit.mut(0);
  const tilt = kit.mut(0);
  const modelIn = kit.anim(0);
  const modelDone = kit.mut(false);
  const growA = kit.anim(0);
  const growB = kit.anim(0);
  const qA = useQuiz(
    L('Podpora A je pevná kloubová. Co nakreslíš místo ní?', 'Опора A — шарнирно-неподвижная. Что нарисуешь вместо неё?'),
    [
      opt(
        L('↑ jen [[R_Ay]]', '↑ только [[R_Ay]]'),
        false,
        L(
          'Něco chybí (červený otazník). Pevná kloubová podpora nedovolí ani posun **do strany** – i ve vodorovném směru na nosník může působit silou.',
          'Чего-то не хватает (красный вопрос). Шарнирно-неподвижная опора не даёт сдвинуться и **вбок** — и в горизонтальном направлении она может действовать на балку силой.',
        ),
      ),
      opt(L('→ ↑ [[R_Ax]] a [[R_Ay]]', '→ ↑ [[R_Ax]] и [[R_Ay]]'), true),
      opt(
        L('→ ↑ ↺ [[R_Ax]], [[R_Ay]], [[M_A]]', '→ ↑ ↺ [[R_Ax]], [[R_Ay]], [[M_A]]'),
        false,
        L(
          'Moment navíc! Kloub otáčení dovolí, takže momentem nebrání. Moment by kreslilo jen vetknutí.',
          'Лишний момент! Шарнир поворачиваться позволяет, значит, моментом не препятствует. Момент рисуют только у заделки.',
        ),
      ),
    ],
    L(
      'Ano: kloub nepustí nosník do strany ani nahoru či dolů – brání mu dvěma silami [[R_Ax]] a [[R_Ay]]. Otáčet se nechá, takže moment nemá.',
      'Да: шарнир не пускает балку ни вбок, ни вверх-вниз — препятствует двумя силами [[R_Ax]] и [[R_Ay]]. Поворачиваться позволяет, поэтому момента нет.',
    ),
  );
  const qB = useQuiz(
    L('Podpora B je posuvná kloubová. Co nakreslíš místo ní?', 'Опора B — шарнирно-подвижная. Что нарисуешь вместо неё?'),
    [
      opt(L('↑ jen [[R_B]]', '↑ только [[R_B]]'), true),
      opt(
        L('← ↑ [[R_Bx]] a [[R_By]]', '← ↑ [[R_Bx]] и [[R_By]]'),
        false,
        L(
          'Vodorovná síla navíc. Válečky se do strany volně kutálejí – vodorovně podpora B nebrání, takže tam žádnou sílu nemá.',
          'Лишняя горизонтальная сила. Катки свободно катятся вбок — по горизонтали опора B не препятствует, значит, силы там нет.',
        ),
      ),
    ],
    L(
      'Tohle je **uvolnění**: nosník nikam nepadá – podpory jsme na papíře nahradili silami, kterými na nosník působí. Zbývá zjistit, jak velké ty síly jsou.',
      'Это и есть **освобождение от связей**: балка никуда не падает — опоры мы на бумаге заменили силами, с которыми они действуют на балку. Осталось узнать, чему равны эти силы.',
    ),
  );
  const nR = useNum(
    L('Spočítej reakce', 'Найди реакции'),
    [numSpec(L('[[R_Ay]]', '[[R_Ay]]'), 9, kn), numSpec(L('[[R_B]]', '[[R_B]]'), 3, kn)],
    L(
      'Σ[[M_A]] = 0: [[R_B]] · 4 − 12 · 1 = 0 → [[R_B]] = 3 kN. Σ[[F_y]] = 0: [[R_Ay]] + 3 − 12 = 0 → [[R_Ay]] = 9 kN. Σ[[F_x]] = 0: [[R_Ax]] = 0. F je blíž k A, proto A nese víc.',
      'Σ[[M_A]] = 0: [[R_B]] · 4 − 12 · 1 = 0 → [[R_B]] = 3 кН. Σ[[F_y]] = 0: [[R_Ay]] + 3 − 12 = 0 → [[R_Ay]] = 9 кН. Σ[[F_x]] = 0: [[R_Ax]] = 0. F ближе к A, поэтому A несёт больше.',
    ),
    [
      trapNear(L('Prohozené: F je blíž k A – víc nese podpora, která je k síle blíž.', 'Перепутано: F ближе к A — больше несёт опора, которая ближе к силе.'), 3, 9),
      trapNear(L('Napůl by se F dělila, jen kdyby byla uprostřed. Tady je 1 m od A.', 'Пополам F делилась бы, только если бы была посередине. Здесь она в 1 м от A.'), 6, 6),
      trapNear(L('Rameno [[R_B]] k bodu A je celé rozpětí 4 m, ne 3 m.', 'Плечо [[R_B]] относительно A — весь пролёт 4 м, а не 3 м.'), null, 4),
    ],
    L('Momenty k bodu A: [[R_B]] má rameno 4 m, F rameno 1 m. Pak Σ[[F_y]] = 0.', 'Моменты относительно A: у [[R_B]] плечо 4 м, у F — 1 м. Потом Σ[[F_y]] = 0.'),
  );
  const pickA = qA.solved ? 1 : qA.picked;
  const pickB = qB.solved ? 0 : qB.picked;
  useLaunched([pickA], async () => {
    if (pickA !== null) {
      await growA.snapTo(0);
      await growA.animateTo(1, tween(500));
    }
  });
  useLaunched([pickB], async () => {
    if (pickB !== null) {
      await growB.snapTo(0);
      await growB.animateTo(1, tween(500));
    }
  });
  // po dopadu se nakreslí model (nový obrázek – skutečný nosník leží dál na zemi, model nic nepadá)
  useLaunched([phase.v], async (job) => {
    if (phase.v === 2) {
      await job.delay(1600);
      phase.v = 3;
      await job.run(modelIn.animateTo(1, tween(700)));
      await job.delay(300);
      modelDone.v = true;
    }
  });
  const solved = nR.solved;
  // dopad: pravý dolní roh nosníku (o 260 + 20 od A) se dotkne podlahy
  const landDeg = (Math.asin((floor - y - 5) / (xb + 20 - xa)) * 180) / Math.PI;
  return (
    <>
      <Stage
        h={222}
        description={t.pick(L('Nosník na dvou podporách se silou F; podpora B jde odstranit', 'Балка на двух опорах с силой F; опору B можно убрать'))}
        draw={(b, c) => {
          if (phase.v < 3) {
            // skutečnost: podpory stojí na betonových blocích
            const gone = phase.v > 0;
            b.rect(xa - 18, y + 33, 36, floor - y - 33, c.subtle, c.inkSoft, 1.2);
            b.rect(xb - 18, y + 36, 36, floor - y - 36, c.subtle, c.inkSoft, 1.2, gone ? 0.15 : 1);
            ground(b, 10, 390, floor, c);
            b.support('pin', xa, y, 'A');
            b.support('roller', xb, y, 'B', gone);
            tiltedBeam(b, xa - 20, xb + 20, y, xa, tilt.v, c);
            const [fx, fy] = rot(xf, y - 5, xa, y, tilt.v);
            b.force(fx, fy - 56, fx, fy, c.load);
            b.text(fx + 8, fy - 42, ' = 12 kN', c.load, 14, 'start', 'F');
            if (phase.v === 2) b.text(380, floor - 14, t.pick(L('bum!', 'бум!')), c.inkSoft, 13, 'end');
            return;
          }
          // model: nic nepadá, jen kreslíme síly, které na nosník působí
          const a = modelIn.value;
          b.text(14, 210, t.pick(L('MODEL – uvolněný nosník (jen síly, nic nepadá)', 'МОДЕЛЬ — освобождённая балка (только силы, ничего не падает)')), c.derived, 12, 'start', null, true, a);
          b.support('pin', xa, y, 'A', qA.solved);
          b.support('roller', xb, y, 'B', qB.solved);
          b.beam(xa - 20, xb + 20, y, a);
          b.force(xf, y - 56, xf, y - 5, c.load, 2.4, 11, false, a);
          b.text(xf + 8, y - 42, ' = 12 kN', c.load, 14, 'start', 'F', false, a);
          // dokud žák nevybral, místo reakce otazník
          if (pickA === null) b.text(xa + 14, y + 40, '?', c.react, 18, 'middle', null, true, a);
          if (pickB === null) b.text(xb - 14, y + 40, '?', c.react, 18, 'middle', null, true, a);
          // reakce v A
          if (pickA !== null) {
            const g = growA.value;
            b.force(xa, y + 6 + 55 * g, xa, y + 6, c.react, 2.4, 11, false, g);
            b.text(xa + 10, y + 56, solved ? ' = 9 kN' : null, c.react, 14, 'start', 'R_Ay', false, g);
            if (pickA === 0) {
              // chybí vodorovná reakce – naznačit, kde
              b.force(xa - 66, y, xa - 22, y, c.load, 2.4, 11, false, g, true);
              b.text(xa - 44, y - 10, '?', c.load, 16, 'middle', null, true, g);
            } else {
              b.force(xa - 22 - 44 * g, y, xa - 22, y, c.react, 2.4, 11, false, g);
              b.text(xa - 44, y - 10, solved ? ' = 0' : null, c.react, 14, 'middle', 'R_Ax', false, g);
            }
            if (pickA === 2) {
              b.moment(xa, y, 24, true, c.load);
              b.text(xa + 30, y - 22, '✗', c.load, 16, 'middle', null, true);
            }
          }
          // reakce v B
          if (pickB !== null) {
            const g = growB.value;
            b.force(xb, y + 6 + 55 * g, xb, y + 6, c.react, 2.4, 11, false, g);
            b.text(xb - 10, y + 56, solved ? ' = 3 kN' : null, c.react, 14, 'end', pickB === 1 ? 'R_By' : 'R_B', false, g);
            if (pickB === 1) {
              b.force(xb + 22 + 44 * g, y, xb + 22, y, c.load, 2.4, 11, false, g, true);
              b.text(xb + 44, y - 10, '✗', c.load, 16, 'middle', null, true, g);
            }
          }
          // kóty
          const yd = y + 92;
          b.line(xa, yd, xb, yd, c.ink, 0.9, false, a);
          for (const x of [xa, xf, xb]) b.line(x - 4, yd + 4, x + 4, yd - 4, c.ink, 1.2, false, a);
          b.text((xa + xf) / 2, yd - 5, '1 m', c.ink, 13, 'middle', null, false, a);
          b.text((xf + xb) / 2, yd - 5, '3 m', c.ink, 13, 'middle', null, false, a);
        }}
      />
      <CardText text={L('Podpora drží nosník jen tím, že na něj **působí silou**. Co se stane, když jednu odstraníme?', 'Опора держит балку только тем, что **действует на неё силой**. Что будет, если одну убрать?')} />
      <Steps
        steps={[
          step(
            L('Odstraň podporu B a sleduj, co udělá nosník', 'Убери опору B и посмотри, что сделает балка'),
            phase.v >= 2,
            () => (
              <ActionButton
                text={L('Odstraň podporu B', 'Убрать опору B')}
                enabled={phase.v === 0}
                onClick={() => {
                  phase.v = 1;
                  kit.scope.launch(async (job) => {
                    // F i tíha táhnou pravý konec dolů – otáčí se kolem A čím dál rychleji
                    await swing(job, 4.0, landDeg, (d) => (tilt.v = d));
                    phase.v = 2;
                  });
                }}
              />
            ),
            () => (
              <Note
                tone="info"
                text={L(
                  'Bez podpory B se nosník otočil kolem A **čím dál rychleji** a dopadl na zem. Podpora B ho držela – tlačila na něj zespodu silou.',
                  'Без опоры B балка повернулась вокруг A **всё быстрее** и упала на землю. Опора B её держала — давила на неё снизу силой.',
                )}
              />
            ),
          ),
          step(L('Sleduj: kreslíme model – nosník bez podpor, jen se silami', 'Смотри: рисуем модель — балка без опор, только с силами'), modelDone.v, () => (
            <SmallText text={L('Model je obrázek na papíře: podpory zmizí a na jejich místě budou síly.', 'Модель — рисунок на бумаге: опоры исчезнут, а на их месте будут силы.')} />
          )),
          quizStep7(qA, t.pick),
          quizStep7(qB, t.pick),
          numStep7(nR, L('F je 1 m od A, rozpětí 4 m. Spočítej obě reakce', 'F в 1 м от A, пролёт 4 м. Найди обе реакции')),
        ]}
      />
    </>
  );
}

/** Jednoduchá ruka zespodu: dlaň, čtyři prsty a palec. */
function hand(b: PrimBuilder, x: number, yTop: number, c: Colors, a = 1) {
  for (let i = 0; i <= 3; i++) b.rect(x - 15 + i * 7.5, yTop, 6, 22, c.subtle, c.inkSoft, 1.2, a);
  b.rect(x - 16, yTop + 20, 32, 30, c.subtle, c.inkSoft, 1.4, a);
  b.poly(
    [
      [x + 16, yTop + 30],
      [x + 28, yTop + 18],
      [x + 32, yTop + 22],
      [x + 16, yTop + 42],
    ],
    c.subtle,
    c.inkSoft,
    1.2,
    a,
  );
  // předloktí dolů
  b.rect(x - 12, yTop + 50, 24, 40, c.subtle, c.inkSoft, 1.2, a);
}

/**
 * Karta 2: „Buď podporou“. Podpora B chybí, nosník drží žákova ruka silou P. Málo → konec klesá až na zem;
 * moc → zvedá se k zarážce; přesně 4 kN → stojí. Pak žák odhadne, jak silně bude tlačit vrácená podpora.
 */
function BeSupport() {
  const t = useTr();
  const kit = useKit();
  const y = 80;
  const floor = 192;
  const xa = 60;
  const xb = 260;
  const xEnd = 300;
  const need = 4;
  const p = kit.mut(2);
  const running = kit.mut(false);
  const tilt = kit.mut(0);
  const fade = kit.mut(1);
  // výsledek posledního pokusu: null = žádný, -1 = klesl, 1 = zvedl se, 0 = drží
  const result = kit.mut<number | null>(null);
  const held = kit.mut(false);
  const tries = kit.mut(0);
  // výměna ruky za podporu B (0 = ruka, 1 = podpora)
  const swap = kit.anim(0);
  const qBack = useQuiz(
    L('Vrátíme podporu B. Jak velkou silou bude na nosník tlačit?', 'Вернём опору B. С какой силой она будет давить на балку?'),
    [
      opt(L('4 kN – stejně jako ruka', '4 кН — как рука'), true),
      opt(L('8 kN – celou sílu F', '8 кН — всю силу F'), false, L('Polovinu F nese kloub A – to se vrácením podpory nezmění.', 'Половину F несёт шарнир A — возвращение опоры этого не меняет.')),
      opt(L('nelze říct předem', 'заранее не скажешь'), false, L('Nosník se nezměnil, zatížení taky ne. Co musí podpora udělat, aby stál?', 'Балка не изменилась, нагрузка тоже. Что должна делать опора, чтобы балка стояла?')),
    ],
    L(
      'Podpora tlačí na nosník stejně jako tvoje ruka: [[R_B]] = 4 kN. Reakce je **právě ta síla, kterou rovnováha potřebuje** – a tu budeš počítat.',
      'Опора давит на балку так же, как твоя рука: [[R_B]] = 4 кН. Реакция — **ровно та сила, которая нужна для равновесия**, — её ты и будешь вычислять.',
    ),
  );
  const returned = qBack.solved;
  useLaunched([returned], async () => {
    if (returned) await swap.animateTo(1, tween(700));
  });
  // dopad pravého konce na podlahu / nadzvednutí k zarážce (stupně)
  const downDeg = (Math.asin((floor - y - 5) / (xEnd - xa)) * 180) / Math.PI;
  const upDeg = (Math.asin(30 / (xEnd - xa)) * 180) / Math.PI;
  const release = () => {
    if (running.v || held.v) return;
    running.v = true;
    result.v = null;
    tries.v++;
    const force = p.v;
    kit.scope.launch(async (job) => {
      const diff = need - force;
      if (Math.abs(diff) < 0.01) {
        await job.delay(1500);
        result.v = 0;
        held.v = true;
        running.v = false;
        return;
      }
      // nevyrovnaný moment k A → úhlové zrychlení úměrné rozdílu
      await swing(job, (2.4 * diff) / need, diff > 0 ? downDeg : upDeg, (d) => (tilt.v = d));
      result.v = diff > 0 ? -1 : 1;
      await job.delay(1100);
      // nový pokus: starý nosník zmizí, nový se objeví vodorovně
      for (let i = 1; i <= 10; i++) {
        fade.v = 1 - i / 10;
        await job.delay(30);
      }
      tilt.v = 0;
      for (let i = 1; i <= 10; i++) {
        fade.v = i / 10;
        await job.delay(30);
      }
      running.v = false;
    });
  };
  return (
    <>
      <Stage
        h={222}
        description={t.pick(L('Nosník s kloubem A, místo podpory B ho drží ruka', 'Балка с шарниром A, вместо опоры B её держит рука'))}
        draw={(b, c) => {
          // zarážka nad pravým koncem
          b.line(340, floor, 340, y - 44, c.inkSoft, 2.0);
          b.line(340, y - 44, 284, y - 44, c.inkSoft, 2.0);
          b.text(300, y - 52, t.pick(L('zarážka', 'упор')), c.inkSoft, 11);
          b.rect(xa - 18, y + 33, 36, floor - y - 33, c.subtle, c.inkSoft, 1.2);
          b.support('pin', xa, y, 'A');
          tiltedBeam(b, xa - 20, xEnd, y, xa, tilt.v, c, fade.v);
          const [fx, fy] = rot(160, y - 5, xa, y, tilt.v);
          b.force(fx, fy - 56, fx, fy, c.load, 2.4, 11, false, fade.v);
          b.text(fx + 8, fy - 42, ' = 8 kN', c.load, 14, 'start', 'F', false, fade.v);
          const [hx, hy] = rot(xb, y + 5, xa, y, tilt.v);
          const sw = swap.value;
          if (sw > 0.5) {
            // ruka se prolnula v podporu – ta tlačí stejně
            const a = (sw - 0.5) * 2;
            b.support('roller', xb, y, 'B');
            b.rect(xb - 18, y + 36, 36, floor - y - 36, c.subtle, c.inkSoft, 1.2, a);
            b.force(xb + 34, y + 62, xb + 34, y + 7, c.react, 2.4, 11, true, a);
            b.line(xb, y + 5, xb + 34, y + 5, c.react, 1.0, true, a);
            b.text(xb + 44, y + 50, ' = 4 kN', c.react, 14, 'start', 'R_B', false, a);
          } else {
            const a = fade.v * (1 - sw * 2);
            hand(b, hx, hy + 1, c, a * 0.7);
            b.force(hx, hy + 58, hx, hy + 1, c.react, 3.2, 11, false, a);
            b.text(hx - 34, hy + 40, ` = ${num(p.v, 1)} kN`, c.react, 14, 'end', 'P', false, a);
          }
          // podlaha zakryje ruku, která klesla pod ni
          b.rect(0, floor, 400, 40, c.paper);
          ground(b, 10, 390, floor, c);
          const msg = returned
            ? null
            : running.v && result.v === null && Math.abs(tilt.v) < 0.01
              ? L('držím…', 'держу…')
              : result.v === -1
                ? L('málo – konec klesl až na zem', 'мало — конец опустился до земли')
                : result.v === 1
                  ? L('moc – nosník se zvedl k zarážce', 'много — балка поднялась до упора')
                  : result.v === 0
                    ? L('drží! nosník stojí vodorovně', 'держит! балка стоит горизонтально')
                    : null;
          if (msg) b.text(200, floor + 24, t.pick(msg), result.v === 0 ? c.accent : c.inkSoft, 14, 'middle', null, result.v === 0);
        }}
      />
      <CardText text={L('Podpora B zmizela – nosník teď držíš **ty**: ruka tlačí na jeho konec silou [[P]].', 'Опоры B нет — балку теперь держишь **ты**: рука давит на её конец силой [[P]].')} />
      <Steps
        steps={[
          step(
            L('Nastav sílu ruky tak, aby nosník po puštění zůstal vodorovně', 'Подбери силу руки так, чтобы отпущенная балка осталась горизонтальной'),
            held.v,
            () => (
              <>
                <ValueSlider
                  label={t.pick(L(`Ruka tlačí [[P]] = ${num(p.v, 1)} kN`, `Рука давит [[P]] = ${num(p.v, 1)} кН`))}
                  value={p.v}
                  min={0}
                  max={8}
                  steps={15}
                  onChange={(it) => {
                    if (!running.v) {
                      p.v = Math.round(it * 2) / 2;
                      if (result.v !== 0) result.v = null;
                    }
                  }}
                />
                <ActionButton text={L('Pusť – drž jen rukou', 'Отпустить — держать только рукой')} enabled={!running.v} onClick={release} />
                {tries.v >= 3 && (
                  <SmallText
                    text={L(
                      'Tip: F leží přesně uprostřed mezi A a B. Kolik z ní asi nese kloub A a kolik ruka?',
                      'Подсказка: F лежит ровно посередине между A и B. Сколько из неё примерно несёт шарнир A, а сколько рука?',
                    )}
                  />
                )}
              </>
            ),
            () => <Note tone="ok" text={L('[[P]] = 4 kN – přesně polovina F. Druhou polovinu nese kloub A.', '[[P]] = 4 кН — ровно половина F. Вторую половину несёт шарнир A.')} />,
          ),
          quizStep7(qBack, t.pick),
        ]}
      />
    </>
  );
}

/**
 * Karta 3: trám na dvou vahách, pod ním visí náklad 12 kN. Posun nákladu mění údaje vah (součet je vždy 12 kN),
 * změna délky lana nemění nic. Nakonec žák spočítá údaj váhy pro novou polohu.
 */
function TwoScales() {
  const t = useTr();
  const kit = useKit();
  const y = 64;
  const floor = 214;
  const xa = 60;
  const xb = 340;
  const m = (xb - xa) / 4; // 70 jednotek na metr, rozpětí 4 m
  const g = 12;
  const kn = t.pick(L('kN', 'кН'));
  const xm = kit.mut(2.0); // poloha nákladu od A, m
  const rope = kit.mut(40);
  const dragging = kit.mut(false);
  const hitNine = kit.mut(false);
  const ropeGuess = kit.mut<number | null>(null);
  const ropeRef = kit.mut(40); // délka lana na začátku zkoušky
  const ropeTried = kit.mut(false);
  const scene = kit.anim(1);
  const nB = useNum(
    L('Kolik ukáže váha B?', 'Сколько покажут весы B?'),
    [numSpec(L('B', 'B'), 9, kn)],
    L('[[R_B]] · 4 = 12 · 3 → [[R_B]] = 9 kN, a na A zbude 3 kN. Zrcadlově k úkolu s 9 kN na A.', '[[R_B]] · 4 = 12 · 3 → [[R_B]] = 9 кН, а на A останется 3 кН. Зеркально к заданию с 9 кН на A.'),
    [
      trapNear(L('To by platilo, kdyby byl náklad blíž k A. Která váha je mu teď blíž?', 'Так было бы, если бы груз был ближе к A. Какие весы теперь ближе к нему?'), 3),
      trapNear(L('Celých 12 kN by B nesla, jen kdyby náklad visel přímo nad ní.', 'Все 12 кН весы B несли бы, только если бы груз висел прямо над ними.'), 12),
      trapNear(L('Napůl se dělí jen uprostřed. Tady je náklad blíž k B.', 'Пополам делится только посередине. Здесь груз ближе к B.'), 6),
    ],
    L('Momenty k A: [[R_B]] · 4 m = 12 kN · 3 m.', 'Моменты относительно A: [[R_B]] · 4 м = 12 кН · 3 м.'),
  );
  // poslední krok: náklad je 3 m od A a váhy jsou zakryté, dokud žák neodpoví
  const asking = ropeTried.v && !nB.solved;
  const ra = (g * (4 - xm.v)) / 4;
  const rb = (g * xm.v) / 4;
  useLaunched([ra, hitNine.v], async (job) => {
    if (!hitNine.v && Math.abs(ra - 9) < 1e-6) {
      await job.delay(500);
      hitNine.v = true;
    }
  });
  useLaunched([ropeTried.v], async (job) => {
    if (ropeTried.v) {
      await job.delay(1500);
      // nové zadání: starý obrázek zmizí, nový (náklad 3 m od A) se objeví
      await job.run(scene.animateTo(0, tween(250)));
      xm.v = 3;
      rope.v = 40;
      await scene.animateTo(1, tween(300));
    }
  });
  const moveX = (v: number) => {
    if (asking) return;
    xm.v = clamp(v, 0.5, 3.5);
  };
  const moveRope = (v: number) => {
    if (asking || ropeGuess.v === null) return;
    rope.v = clamp(v, 14, 92);
    if (Math.abs(rope.v - ropeRef.v) > 25) ropeTried.v = true;
  };
  const bx = xa + xm.v * m;
  const ropeOpts = [L('totéž', 'то же самое'), L('víc', 'больше'), L('míň', 'меньше')];
  const sum = () => <Readout text={`A + B = ${num(ra, 2)} + ${num(rb, 2)} = 12 kN`} />;
  const tip = useTip7(L('Prodloužíme lano – náklad visí níž. Co ukážou váhy?', 'Удлиним трос — груз повиснет ниже. Что покажут весы?'), ropeOpts, ropeGuess.v, (i) => {
    ropeGuess.v = i;
    ropeRef.v = rope.v;
  });
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Trám na dvou vahách, pod ním na laně náklad 12 kN', 'Брус на двух весах, под ним на тросе груз 12 кН'))}
        onDrag={(p, start) => {
          if (start) dragging.v = !asking && dist(p, bx, y + 5 + rope.v + 20) < 40;
          if (dragging.v) {
            moveX(Math.round(((p.x - xa) / m) * 4) / 4);
            moveRope(p.y - y - 5 - 20);
          }
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b, c) => {
          const sa = scene.value;
          // kozy s vahami
          for (const [x, v, name] of [
            [xa, ra, 'A'],
            [xb, rb, 'B'],
          ] as [number, number, string][]) {
            b.line(x - 22, y + 50, x - 34, floor, c.inkSoft, 2.0);
            b.line(x + 22, y + 50, x + 34, floor, c.inkSoft, 2.0);
            b.rect(x - 30, y + 13, 60, 37, c.subtle, c.ink, 1.4);
            b.rect(x - 26, y + 5, 52, 8, c.beam, c.ink, 1.2);
            b.rect(x - 22, y + 19, 44, 20, c.paper, c.inkSoft, 1.0);
            b.text(x, y + 34, asking ? '?' : num(v, 1), c.react, 14, 'middle', null, true, sa);
            b.text(x, y + 48, 'kN', c.inkSoft, 9);
            b.text(name === 'A' ? x - 36 : x + 36, y + 30, name, c.ink, 15, name === 'A' ? 'end' : 'start', null, true);
          }
          ground(b, 10, 390, floor, c);
          // trám (vytárované váhy – počítá se jen náklad)
          b.rect(xa - 30, y - 5, xb - xa + 60, 10, c.beam, c.ink, 1.6);
          // nositelka tíhy nákladu: svislá přímka přes lano
          b.line(bx, y - 40, bx, floor, c.inkSoft, 1.0, true, sa);
          // lano a náklad (pytel s pískem)
          b.line(bx, y + 5, bx, y + 5 + rope.v, c.ink, 1.6, false, sa);
          const top = y + 5 + rope.v;
          b.poly(
            [
              [bx - 20, top + 4],
              [bx + 20, top + 4],
              [bx + 24, top + 40],
              [bx - 24, top + 40],
            ],
            c.subtle,
            c.ink,
            1.5,
            sa,
          );
          b.line(bx, top, bx - 14, top + 5, c.ink, 1.2, false, sa);
          b.line(bx, top, bx + 14, top + 5, c.ink, 1.2, false, sa);
          b.text(bx, top + 27, '12 kN', c.load, 12, 'middle', null, true, sa);
          if (!asking) b.handle(bx + 34, top + 22, dragging.v);
          if (ropeTried.v) {
            // poloha nákladu pro poslední otázku
            const yd = floor - 8;
            b.line(xa, yd, xb, yd, c.ink, 0.9, false, sa);
            for (const x of [xa, bx, xb]) b.line(x - 4, yd + 4, x + 4, yd - 4, c.ink, 1.2, false, sa);
            b.text((xa + bx) / 2, yd - 5, `${num(xm.v)} m`, c.ink, 12, 'middle', null, false, sa);
            b.text((bx + xb) / 2, yd - 5, `${num(4 - xm.v)} m`, c.ink, 12, 'middle', null, false, sa);
          }
        }}
      />
      <CardText
        text={L(
          'Trám leží na dvou vahách (vytárovaných – ukazují jen náklad). Pod ním visí na laně pytel s pískem: 12 kN. Váhy ukazují přesně reakce podpor.',
          'Брус лежит на двух весах (обнулённых — показывают только груз). Под ним на тросе висит мешок с песком: 12 кН. Весы показывают ровно реакции опор.',
        )}
      />
      <Steps
        steps={[
          step(
            L('Posuň náklad tak, aby levá váha A ukazovala 9 kN', 'Сдвинь груз так, чтобы левые весы A показывали 9 кН'),
            hitNine.v,
            () => (
              <>
                <Chips options={[L('◀ 0,25 m', '◀ 0,25 м'), L('0,25 m ▶', '0,25 м ▶')]} onPick={(i) => moveX(xm.v + (i === 0 ? -0.25 : 0.25))} />
                {sum()}
              </>
            ),
            () => (
              <Note
                tone="ok"
                text={L(
                  'Momenty k A: [[R_B]] · 4 m = 12 kN · 1 m → [[R_B]] = 3 kN, na A zbude 9 kN. Čím blíž je náklad k váze, tím víc z něj nese.',
                  'Моменты относительно A: [[R_B]] · 4 м = 12 кН · 1 м → [[R_B]] = 3 кН, на A остаётся 9 кН. Чем ближе груз к весам, тем больше они несут.',
                )}
              />
            ),
          ),
          tip,
          step(
            L('Změň délku lana a sleduj obě váhy', 'Измени длину троса и следи за весами'),
            ropeTried.v,
            () => (
              <>
                <Chips options={[L('lano kratší', 'трос короче'), L('lano delší', 'трос длиннее')]} onPick={(i) => moveRope(rope.v + (i === 0 ? -13 : 13))} />
                {sum()}
              </>
            ),
            () => (
              <Note
                tone={ropeGuess.v === 0 ? 'ok' : 'info'}
                text={L(
                  (ropeGuess.v === 0 ? 'Tip sedí. ' : '') +
                    'Délka lana nic nemění: tíha nákladu se jen posunula **po své nositelce** (čárkovaná svislice). Záleží jen na tom, kde nositelka protíná trám.',
                  (ropeGuess.v === 0 ? 'Прогноз верен. ' : '') +
                    'Длина троса ничего не меняет: сила тяжести груза лишь сдвинулась **вдоль своей линии действия** (пунктирная вертикаль). Важно только, где линия действия пересекает брус.',
                )}
              />
            ),
          ),
          numStep7(nB, L('Náklad je teď 3 m od A (1 m od B). Kolik ukáže váha B?', 'Груз теперь в 3 м от A (1 м от B). Сколько покажут весы B?')),
        ]}
      />
    </>
  );
}

/**
 * Karta 4: akce a reakce – rozložený obrázek. Obě síly na jedné nositelce, každá na jiném tělese;
 * žák vybere síly, které působí na nosník, a výběr nechá zkontrolovat.
 */
function ActionReaction() {
  const t = useTr();
  const kit = useKit();
  const lift = kit.anim(0);
  const split = kit.mut(false);
  const picked = kit.mut<Set<number>>(new Set());
  const checked = kit.mut<Set<number> | null>(null);
  const y = 150;
  const xb = 200;
  const correct = !!checked.v && checked.v.size === 2 && checked.v.has(0) && checked.v.has(1);
  const ch = checked.v;
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Nosník a podpora tlačí na sebe navzájem – rozložený obrázek', 'Балка и опора давят друг на друга — разнесённый рисунок'))}
        draw={(b, c) => {
          const up = 88 * lift.value;
          const by = y - up;
          b.support('pin', xb, y, '');
          b.beam(80, 320, by);
          b.force(xb, by - 50, xb, by - 6, c.load, 2.4, 11, picked.v.has(0));
          b.text(xb + 8, by - 34, ' = 6 kN', c.load, 14, 'start', 'F');
          if (lift.value > 0.5) {
            const a = clamp((lift.value - 0.5) / 0.5, 0, 1);
            const contact = y + 9;
            const bottom = by + 5;
            // obě síly na téže svislé nositelce x = xb; každá u „svého“ tělesa
            b.force(xb, bottom + 36, xb, bottom + 1, c.react, 2.4, 11, picked.v.has(1), a);
            b.text(xb + 10, bottom + 26, t.pick(L('podpora → nosník: 6 kN', 'опора → балка: 6 кН')), c.react, 13, 'start', null, false, a);
            b.force(xb, contact - 36, xb, contact, c.inkSoft, 2.4, 11, picked.v.has(2), a);
            b.text(xb + 10, contact - 16, t.pick(L('nosník → podpora: 6 kN', 'балка → опора: 6 кН')), c.inkSoft, 13, 'start', null, false, a);
            b.line(xb, bottom + 36, xb, contact - 36, c.inkSoft, 1.0, true, a);
            b.text(12, 222, t.pick(L('rozložený obrázek – ve skutečnosti se tělesa dotýkají', 'разнесённый рисунок — на самом деле тела соприкасаются')), c.derived, 12, 'start', null, false, a);
          }
        }}
      />
      <CardText
        text={L(
          'Nosník tlačí na podporu dolů a podpora na nosník **stejně velkou** silou nahoru. Ve styku jsou obě síly schované – abychom je viděli, nakreslíme tělesa **od sebe**.',
          'Балка давит на опору вниз, а опора на балку — **такой же по величине** силой вверх. В месте контакта обе силы не видны — чтобы их увидеть, нарисуем тела **отдельно**.',
        )}
      />
      <Steps
        steps={[
          step(L('Rozlož obrázek a prohlédni si obě síly ve styku', 'Разнеси рисунок и рассмотри обе силы в месте контакта'), split.v, () => (
            <ActionButton
              text={L('Rozlož obrázek', 'Разнести рисунок')}
              enabled={!lift.isRunning}
              onClick={() =>
                kit.scope.launch(async () => {
                  await lift.animateTo(1, tween(700));
                  split.v = true;
                })
              }
            />
          )),
          step(
            L('Počítáme nosník. Označ všechny síly, které působí **na něj**', 'Считаем балку. Отметь все силы, которые действуют **на неё**'),
            correct,
            () => (
              <>
                <Chips
                  options={[L('zatížení [[F]]', 'нагрузка [[F]]'), L('podpora → nosník', 'опора → балка'), L('nosník → podpora', 'балка → опора')]}
                  selectedSet={picked.v}
                  onPick={(i) => {
                    const n = new Set(picked.v);
                    if (n.has(i)) n.delete(i);
                    else n.add(i);
                    picked.v = n;
                    checked.v = null;
                  }}
                />
                <ActionButton text={L(`Zkontrolovat (vybráno ${picked.v.size})`, `Проверить (выбрано ${picked.v.size})`)} enabled={picked.v.size > 0} onClick={() => (checked.v = new Set(picked.v))} />
                {ch === null ? null : ch.has(2) ? (
                  <Note
                    tone="nudge"
                    text={L(
                      'Šedá šipka tlačí **na podporu** (míří do ní). Na nosník nepůsobí – do jeho rovnic nepatří.',
                      'Серая стрелка давит **на опору** (направлена в неё). На балку она не действует — в её уравнения не входит.',
                    )}
                  />
                ) : !ch.has(0) ? (
                  <Note tone="nudge" text={L('Něco chybí: co tlačí na nosník **shora**?', 'Чего-то не хватает: что давит на балку **сверху**?')} />
                ) : !ch.has(1) ? (
                  <Note tone="nudge" text={L('Něco chybí: co drží nosník **zespodu**?', 'Чего-то не хватает: что держит балку **снизу**?')} />
                ) : null}
              </>
            ),
            () => (
              <Note
                tone="ok"
                text={L(
                  '**Akce a reakce** jsou dvě stejně velké opačné síly na jedné nositelce – každá ale působí na **jiné** těleso. Proto se v rovnicích nosníku nikdy navzájem nevyruší: šedá do nich vůbec nepatří.',
                  '**Действие и противодействие** — две равные противоположные силы на одной линии действия, но каждая действует на **своё** тело. Поэтому в уравнениях балки они никогда не сокращаются: серая туда вообще не входит.',
                )}
              />
            ),
          ),
        ]}
      />
    </>
  );
}

/** Kotvy pod podporou: šrouby do základu – podpora umí nosník i držet dole. */
function anchors(b: PrimBuilder, x: number, y: number, c: Colors) {
  for (const dx of [-12, 12]) {
    b.line(x + dx, y + 30, x + dx, y + 50, c.ink, 2.0);
    b.line(x + dx - 4, y + 50, x + dx + 4, y + 50, c.ink, 2.0);
  }
}

/**
 * Karta 5: smysl reakce si zvolíš, R_A spočítá žák; záporný výsledek jen otočí šipku.
 * Nakonec pokus „bez A“ ukáže proč: levý konec by se zvedl.
 */
function GuessSense() {
  const t = useTr();
  const kit = useKit();
  const kn = t.pick(L('kN', 'кН'));
  const sel = kit.mut<number | null>(null); // vybraný smysl před potvrzením
  const guess = kit.mut<number | null>(null); // 0 = ↑, 1 = ↓
  const flip = kit.anim(0);
  // „co kdyby A nebyla“: nosník se otočí kolem B, levý konec se zvedne k zarážce
  const whatIf = kit.mut(false);
  const whatIfSeen = kit.mut(false);
  const tilt = kit.mut(0);
  const fade = kit.mut(1);
  const up = (guess.v ?? sel.v) !== 1;
  const nA = useNum(
    L(`R_A, smysl ${guess.v ?? -1}`, `R_A, smysl ${guess.v ?? -1}`),
    [numSpec(L('[[R_A]]', '[[R_A]]'), guess.v === 1 ? 4 : -4, kn, true)],
    guess.v === 1 ? L('[[R_A]]·2 − 4·2 = 0 → [[R_A]] = 4 kN.', '[[R_A]]·2 − 4·2 = 0 → [[R_A]] = 4 кН.') : L('−[[R_A]]·2 − 4·2 = 0 → [[R_A]] = −8/2 = −4 kN.', '−[[R_A]]·2 − 4·2 = 0 → [[R_A]] = −8/2 = −4 кН.'),
    [
      guess.v === 1
        ? numTrap(L('Dosaď −4 zpátky: (−4)·2 − 8 = −16, ne 0. Jaké znaménko musí [[R_A]] mít?', 'Подставь −4 обратно: (−4)·2 − 8 = −16, а не 0. Какой знак должен быть у [[R_A]]?'), (v) => v[0] != null && Math.abs(v[0] + 4) < 0.02)
        : numTrap(
            L('Velikost sedí, ale dosaď zpátky: −4·2 − 8 = −16, ne 0. Jaké znaménko musí [[R_A]] mít?', 'Величина верная, но подставь обратно: −4·2 − 8 = −16, а не 0. Какой знак должен быть у [[R_A]]?'),
            (v) => v[0] != null && Math.abs(v[0] - 4) < 0.02,
          ),
      trapNear(L('8 kNm je moment F k bodu B (4 · 2). Ještě vyděl ramenem [[R_A]], 2 m.', '8 кН·м — это момент F относительно B (4 · 2). Ещё раздели на плечо [[R_A]], 2 м.'), 8),
    ],
    L('[[R_A]] i F mají k bodu B rameno 2 m. Vyřeš rovnici pro [[R_A]].', 'У [[R_A]] и у F плечо относительно B — 2 м. Реши уравнение относительно [[R_A]].'),
  );
  const computed = nA.solved;
  const qUp = useQuiz(
    L('Vyšlo [[R_A]] = −4 kN. Co ten minus znamená?', 'Получилось [[R_A]] = −4 кН. Что означает минус?'),
    [
      opt(L('chybu ve výpočtu', 'ошибку в расчёте'), false, L('Rovnice platí, výpočet je v pořádku. Minus nese informaci o **smyslu**.', 'Уравнение выполняется, расчёт верный. Минус несёт информацию о **направлении**.')),
      opt(L('reakce míří opačně – dolů', 'реакция направлена наоборот — вниз'), true),
      opt(L('reakce je nulová', 'реакция равна нулю'), false, L('Nulová by vyšla 0. Velikost je 4 kN – jen znaménko je záporné.', 'Нулевая дала бы 0. Величина 4 кН — отрицателен только знак.')),
    ],
    L(
      'Minus není chyba! Jen říká, že reakce ve skutečnosti míří **opačně**, než sis zvolil: podpora A táhne nosník dolů (brání mu v nadzvednutí).',
      'Минус — не ошибка! Он только говорит, что на самом деле реакция направлена **противоположно** выбранному: опора A тянет балку вниз (не даёт ей приподняться).',
    ),
  );
  const qDown = useQuiz(
    L('Vyšlo +4 kN. Co by vyšlo, kdybys zvolil smysl nahoru?', 'Получилось +4 кН. Что получилось бы, выбери ты направление вверх?'),
    [
      opt(L('+4 kN', '+4 кН'), false, L('Se smyslem nahoru se v rovnici otočí znaménko členu s [[R_A]]: −[[R_A]]·2 − 8 = 0.', 'При направлении вверх в уравнении меняется знак члена с [[R_A]]: −[[R_A]]·2 − 8 = 0.')),
      opt(L('−4 kN', '−4 кН'), true),
      opt(L('0', '0'), false, L('Podpora pořád drží stejně – velikost zůstane 4 kN.', 'Опора держит так же — величина останется 4 кН.')),
    ],
    L(
      'Vyšlo kladně – tipl sis dobře, podpora A táhne nosník dolů. Kdybys zvolil nahoru, vyšlo by −4 kN: stejná síla, jen s opačným znaménkem.',
      'Получилось положительное число — ты угадал: опора A тянет балку вниз. Если бы ты выбрал «вверх», вышло бы −4 кН: та же сила, только с другим знаком.',
    ),
  );
  useLaunched([qUp.solved], async (job) => {
    if (qUp.solved && guess.v === 0) {
      await job.delay(300);
      await flip.animateTo(1, tween(700));
    }
  });
  const y = 100;
  // A v 0 m, B ve 2 m, F = 4 kN na konci převislého konce ve 4 m; 1 m = 70 jednotek
  const x0 = 60;
  const m = 70;
  const xB = x0 + 2 * m;
  const liftDeg = (Math.asin(34 / (2 * m + 10)) * 180) / Math.PI;
  const dirs = [L('[[R_A]] nahoru ↑', '[[R_A]] вверх ↑'), L('[[R_A]] dolů ↓', '[[R_A]] вниз ↓')];
  return (
    <>
      <Stage
        h={220}
        description={t.pick(L('Nosník s převislým koncem, směr reakce v bodě A si volíš', 'Балка с консолью, направление реакции в точке A выбираешь сам'))}
        draw={(b, c) => {
          if (whatIf.v) {
            // zarážka nad levým koncem
            b.line(x0 - 30, y - 44, x0 + 10, y - 44, c.inkSoft, 2.0);
            b.line(x0 - 30, y - 44, x0 - 30, y - 70, c.inkSoft, 2.0);
            b.support('pin', x0, y, 'A', true);
            b.support('roller', xB, y, 'B');
            tiltedBeam(b, x0 - 10, x0 + 4 * m + 10, y, xB, tilt.v, c, fade.v);
            const [fx, fy] = rot(x0 + 4 * m, y - 5, xB, y, tilt.v);
            b.force(fx, fy - 56, fx, fy, c.load, 2.4, 11, false, fade.v);
            b.text(fx - 8, fy - 42, ' = 4 kN', c.load, 14, 'end', 'F', false, fade.v);
            if (Math.abs(tilt.v) > 1) b.text(x0 + 30, y - 52, t.pick(L('levý konec se zvedá!', 'левый конец поднимается!')), c.load, 13, 'start', null, false, fade.v);
            return;
          }
          b.beam(x0 - 10, x0 + 4 * m + 10, y);
          b.support('pin', x0, y, 'A', true);
          anchors(b, x0, y, c);
          b.text(x0 - 18, y + 50, t.pick(L('kotvy', 'анкеры')), c.inkSoft, 11, 'end');
          b.support('roller', xB, y, 'B', true);
          b.force(x0 + 4 * m, y - 64, x0 + 4 * m, y - 5, c.load);
          b.text(x0 + 4 * m - 8, y - 50, ' = 4 kN', c.load, 15, 'end', 'F');
          b.force(xB, y + 62, xB, y + 6, c.react);
          b.text(xB + 12, y + 50, computed ? ' = 8 kN' : null, c.react, 14, 'start', 'R_B');
          if (guess.v !== null || sel.v !== null) {
            const f = flip.value;
            const drawUp = up && f < 0.5;
            if (drawUp) b.force(x0, y + 62, x0, y + 6, c.react);
            else b.force(x0, y + 6, x0, y + 62, c.react, 2.4, 11, up && f > 0.5);
            const v = !computed ? ' = ?' : up && f > 0.5 ? ' = 4 kN ↓' : up ? ' = −4 kN' : ' = 4 kN';
            b.text(x0 + 12, y + 50, v, c.react, 14, 'start', 'R_A');
          }
          const yd = y + 84;
          b.line(x0, yd, x0 + 4 * m, yd, c.ink, 0.9);
          for (let k = 0; k <= 2; k++) b.line(x0 + k * 2 * m - 4, yd + 4, x0 + k * 2 * m + 4, yd - 4, c.ink, 1.2);
          b.text(x0 + m, yd - 5, '2 m', c.ink, 13);
          b.text(x0 + 3 * m, yd - 5, '2 m', c.ink, 13);
        }}
      />
      <CardText
        text={L(
          'Kam míří reakce v A, předem nevíš. Smysl si proto **zvol** – výpočet ukáže, jestli byl správný. Podpora A je ukotvená šrouby do základu: umí nosník tlačit nahoru i držet dolů.',
          'Куда направлена реакция в A, заранее неизвестно. Поэтому направление **выбери сам** — расчёт покажет, было ли оно верным. Опора A закреплена анкерами в фундаменте: может и толкать балку вверх, и удерживать её вниз.',
        )}
      />
      <Steps
        steps={[
          step(
            L('Zvol smysl reakce [[R_A]] – kam ji nakreslíš?', 'Выбери направление реакции [[R_A]] — куда её нарисуешь?'),
            guess.v !== null,
            () => (
              <>
                <Chips options={dirs} selected={sel.v} onPick={(i) => (sel.v = i)} />
                <ActionButton text={L('Nakreslit šipku', 'Нарисовать стрелку')} enabled={sel.v !== null} onClick={() => (guess.v = sel.v)} />
              </>
            ),
            () => (guess.v !== null ? <SmallText text={L('Tvoje volba: ' + dirs[guess.v].cs, 'Твой выбор: ' + dirs[guess.v].ru)} /> : null),
          ),
          numStep7(
            nA,
            L('Momenty k bodu B (↺ je +): kolik vyjde [[R_A]]?', 'Моменты относительно B (↺ — плюс): чему равна [[R_A]]?'),
            () => <Readout text={guess.v === 1 ? 'Σ[[M_B]] = 0:  [[R_A]]·2 − 4·2 = 0' : 'Σ[[M_B]] = 0:  −[[R_A]]·2 − 4·2 = 0'} />,
            () => <Readout text={guess.v === 1 ? 'Σ[[F_y]] = 0:  −[[R_A]] + [[R_B]] − 4 = 0  →  [[R_B]] = 8 kN' : 'Σ[[F_y]] = 0:  [[R_A]] + [[R_B]] − 4 = 0  →  [[R_B]] = 8 kN'} />,
          ),
          quizStep7(guess.v === 1 ? qDown : qUp, t.pick, guess.v === 1 ? 3 : 1),
          step(
            L('Ověř to pokusem: odeber podporu A a sleduj levý konec', 'Проверь опытом: убери опору A и следи за левым концом'),
            whatIfSeen.v,
            () => (
              <ActionButton
                text={L('Odeber podporu A', 'Убрать опору A')}
                enabled={!whatIf.v}
                onClick={() => {
                  whatIf.v = true;
                  kit.scope.launch(async (job) => {
                    tilt.v = 0;
                    fade.v = 1;
                    // F na převislém konci otáčí nosník kolem B: pravý konec dolů, levý nahoru – čím dál rychleji
                    await swing(job, 3.0, liftDeg, (d) => (tilt.v = d));
                    await job.delay(1500);
                    whatIfSeen.v = true;
                    // pokus skončil: zvednutý nosník zmizí, model se vrátí
                    for (let i = 1; i <= 10; i++) {
                      fade.v = 1 - i / 10;
                      await job.delay(30);
                    }
                    whatIf.v = false;
                    tilt.v = 0;
                    fade.v = 1;
                  });
                }}
              />
            ),
            () => (
              <>
                <Note
                  tone="ok"
                  text={L(
                    'Bez A by se levý konec zvedl. Kotvená podpora A mu v tom brání – proto ho drží **dolů**: [[R_A]] míří dolů.',
                    'Без A левый конец поднялся бы. Закреплённая опора A этому мешает — поэтому держит его **вниз**: [[R_A]] направлена вниз.',
                  )}
                />
                <SmallText
                  text={L(
                    'V trenažéru proto kreslíme všechny reakce v kladném smyslu (→ ↑ ↺). Znaménko výsledku pak řekne, kam opravdu míří.',
                    'Поэтому в тренажёре все реакции рисуем в положительном направлении (→ ↑ ↺). Знак результата покажет, куда они направлены на самом деле.',
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

/** Karta 6: kontrolní otázka. */
function Check() {
  const t = useTr();
  const solved = useCardState().solvedHere;
  const q = useQuizStep(
    L('Co nakreslíš místo posuvné kloubové podpory B?', 'Что нарисуешь вместо шарнирно-подвижной опоры B?'),
    [
      opt(
        L('dvě síly [[R_Bx]] a [[R_By]]', 'две силы [[R_Bx]] и [[R_By]]'),
        false,
        L('Válečky se kutálejí vodorovně. Může podpora v tom směru bránit pohybu?', 'Катки катятся по горизонтали. Может ли опора препятствовать движению в этом направлении?'),
      ),
      opt(L('jednu sílu [[R_B]] kolmo k podložce', 'одну силу [[R_B]] перпендикулярно опорной поверхности'), true),
      opt(L('nic – podpora prostě zmizí', 'ничего — опора просто исчезает'), false, L('A co pak drží pravý konec nahoře? Vzpomeň si na padající nosník.', 'А что тогда будет держать правый конец? Вспомни падающую балку.')),
    ],
    L(
      'Posuvná kloubová podpora brání jen posunu kolmo k podložce – proto jediná síla [[R_B]]. Takhle uvolníš každou vazbu: místo ní nakreslíš právě ty reakce, kterými brání pohybu.',
      'Шарнирно-подвижная опора препятствует только смещению перпендикулярно опорной поверхности — поэтому одна сила [[R_B]]. Так освобождают от любой связи: вместо неё рисуют именно те реакции, которыми она препятствует движению.',
    ),
  );
  return (
    <>
      <Stage
        h={170}
        description={t.pick(L('Posuvná kloubová podpora B', 'Шарнирно-подвижная опора B'))}
        draw={(b, c) => {
          b.beam(60, 340, 80);
          b.support('pin', 80, 80, 'A', true);
          b.support('roller', 320, 80, 'B', solved);
          // po správné odpovědi místo podpory její reakce
          if (solved) {
            b.force(320, 80 + 60, 320, 80 + 6, c.react);
            b.text(312, 80 + 50, null, c.react, 14, 'end', 'R_B');
          }
        }}
      />
      <Steps steps={[q]} />
    </>
  );
}

export const cards = [CutItFree, BeSupport, TwoScales, ActionReaction, GuessSense, Check];
