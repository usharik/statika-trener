/*
 * Lekce 8 – Spojité zatížení. q je síla na každý metr (kN/m); výslednice Q = q · ℓ působí uprostřed.
 * „Nahradit“ znamená stejný účinek na nosník: s výslednicí na správném místě vyjdou reakce stejně
 * jako se spojitým zatížením. Na konci lekce celý nosník krok za krokem. (Přepis z aplikace: learn/Lesson8.kt.)
 */
import type { ReactNode } from 'react';
import { Rich } from '../../components/Sym';
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
  numTrap,
  opt,
  step,
  trapNear,
  tween,
  useKit,
  useLaunched,
  useNum,
  useQuiz,
  useTr,
  type Colors,
  type NumState,
  type NumTrap,
  type Opt,
  type PrimBuilder,
  type QuizState,
  type Step,
} from '../kit';

/** Hřeben šipek spojitého zatížení mezi x1 a x2 (výška = q v měřítku obrázku), se splýváním do středu ([merge] 0…1). */
function comb(b: PrimBuilder, x1: number, x2: number, yTop: number, yBeam: number, c: Colors, merge = 0, a = 1) {
  if (a <= 0.01) return;
  const xc = (x1 + x2) / 2;
  const n = Math.max(2, Math.round((x2 - x1) / 22));
  b.rect(x1 + (xc - x1) * merge, yTop, (x2 - x1) * (1 - merge), yBeam - yTop, c.loadFill, null, 1.5, a);
  b.line(x1 + (xc - x1) * merge, yTop, x2 - (x2 - xc) * merge, yTop, c.load, 1.6, false, a);
  for (let i = 0; i <= n; i++) {
    const x = x1 + ((x2 - x1) * i) / n;
    const xm = x + (xc - x) * merge;
    b.force(xm, yTop, xm, yBeam, c.load, 1.3, 7, false, a);
  }
}

/** Kóta mezi x1 a x2 ve výšce y s popiskem nad čarou. */
function dim(b: PrimBuilder, x1: number, x2: number, y: number, label: string, color: string, a = 1) {
  if (a <= 0.01) return;
  b.line(x1, y, x2, y, color, 0.9, false, a);
  b.line(x1 - 4, y + 4, x1 + 4, y - 4, color, 1.2, false, a);
  b.line(x2 - 4, y + 4, x2 + 4, y - 4, color, 1.2, false, a);
  b.text((x1 + x2) / 2, y - 5, label, color, 12, 'middle', null, false, a);
}

/** Nápověda ke špatnému odhadu výslednice Q = q · ℓ podle toho, jakou chybu žák nejspíš udělal. */
function qHint(v: number, q: number, l: number): L {
  if (Math.abs(v - q) < 1e-9) return L(`${num(q)} kN připadají jen na **jeden** metr. Zatížených metrů je ${num(l)}.`, `${num(q)} кН приходится только на **один** метр. Нагруженных метров — ${num(l)}.`);
  if (Math.abs(v - l) < 1e-9) return L(`${num(l)} je délka v metrech, ne síla. Každý metr přidá ${num(q)} kN.`, `${num(l)} — это длина в метрах, а не сила. Каждый метр добавляет ${num(q)} кН.`);
  if (Math.abs(v - (q + l)) < 1e-9) return L('kN/m a metry se sčítat nedají – jsou to různé veličiny. Násob: kN/m · m = kN.', 'кН/м и метры складывать нельзя — это разные величины. Умножай: кН/м · м = кН.');
  if (Math.abs(v - (q * l) / 2) < 1e-9)
    return L('To je jen polovina zatížení. Ve středu výslednice **působí**, ale nese celé zatížení.', 'Это только половина нагрузки. Посередине равнодействующая **приложена**, но несёт всю нагрузку.');
  return L(`Sečti to po metrech: každý metr přidá ${num(q)} kN.`, `Сложи по метрам: каждый метр добавляет ${num(q)} кН.`);
}

/** Kvíz jako krok; [line] = co se po vyřešení zapíše do výkladu místo vysvětlení. */
function quizStep8(q: QuizState, pickL: (l: L) => string, columns = 1, label: L = q.question, line?: () => ReactNode): Step {
  return {
    label,
    done: q.solved,
    result: line ?? (() => q.Feedback()),
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

/** Číselná odpověď jako krok; [show] = rovnice v doku, [line] = zápis do výkladu místo vysvětlení, [more] = navíc. */
function numStep8(n: NumState, label: L, show?: () => ReactNode, line?: () => ReactNode, more?: () => ReactNode): Step {
  return {
    label,
    done: n.solved,
    result: () => (
      <>
        {line ? line() : n.Feedback()}
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

/** Řádek zápisu řešení (karta „Celý nosník“). */
function SolLine({ text, ok = false }: { text: L; ok?: boolean }) {
  const t = useTr();
  return (
    <div className={'sol-line' + (ok ? ' ok' : '')}>
      <Rich text={t.pick(text)} />
    </div>
  );
}

/** Volby odhadu výslednice: správná je q · ℓ, ke špatným cílená nápověda. */
const qOpts = (options: number[], q: number, l: number): Opt[] =>
  options.map((v) => (Math.abs(v - q * l) < 1e-9 ? opt(L(`${num(v)} kN`, `${num(v)} кН`), true) : opt(L(`${num(v)} kN`, `${num(v)} кН`), false, qHint(v, q, l))));

/** Pasti pro číselný odhad výslednice (stejné nápovědy jako u voleb). */
const qTraps = (wrong: number[], q: number, l: number): NumTrap[] => wrong.map((w) => trapNear(qHint(w, q, l), w));

/** Karta 0: q je „na metr“ – knihy na polici. Žák nejdřív odhadne Q, teprve pak se ukáže rozpočet po metrech. */
function PerMetre() {
  const t = useTr();
  const kit = useKit();
  const q = 2;
  const lens = [3.0, 1.5];
  const kn = t.pick(L('kN', 'кН'));
  const round = kit.mut(0);
  // knihy se na polici skládají postupně, rozpočet po metrech se objeví po správném odhadu
  const books = kit.anim(0);
  const reveal = kit.anim(0);
  const q1 = useQuiz(L('Knihy zabírají 3 m police. Kolik kN váží dohromady?', 'Книги занимают 3 м полки. Сколько кН они весят вместе?'), qOpts([2, 3, 5, 6], q, 3), L('Q = q · ℓ = 2 kN/m · 3 m = 6 kN – po metrech 2 + 2 + 2.', 'Q = q · ℓ = 2 кН/м · 3 м = 6 кН — по метрам 2 + 2 + 2.'));
  const n2 = useNum(
    L('Knihy na 1,5 m: kolik kN?', 'Книги на 1,5 м: сколько кН?'),
    [numSpec(L('Q', 'Q'), 3, kn)],
    L('Q = q · ℓ = 2 kN/m · 1,5 m = 3 kN – celý metr 2 kN a půl metru 1 kN.', 'Q = q · ℓ = 2 кН/м · 1,5 м = 3 кН — целый метр 2 кН и полметра 1 кН.'),
    qTraps([2, 1.5, 3.5], q, 1.5),
    L('Na každý metr připadají 2 kN. Kolik připadne na půl metru?', 'На каждый метр приходится 2 кН. Сколько придётся на полметра?'),
  );
  useLaunched([round.v], async () => {
    await reveal.snapTo(0);
    await books.snapTo(0);
    await books.animateTo(1, tween(900));
  });
  useLaunched([q1.solved], async (job) => {
    if (q1.solved && round.v === 0) {
      await job.run(reveal.animateTo(1, tween(500)));
      await job.delay(1400);
      round.v = 1;
    }
  });
  useLaunched([n2.solved], async () => {
    if (n2.solved) await reveal.animateTo(1, tween(500));
  });
  const len = lens[round.v];
  const y = 130;
  const x1 = 40;
  const m = 80;
  return (
    <>
      <Stage
        h={220}
        description={t.pick(L('Knihy na polici, 2 kN na každý metr', 'Книги на полке, 2 кН на каждый метр'))}
        draw={(b, c) => {
          b.beam(x1, x1 + 4 * m, y);
          // knihy na zatížené délce
          const end = x1 + len * m;
          const total = Math.floor((end - x1 - 4) / 13);
          for (let i = 0; i < total; i++) {
            const a = clamp(books.value * total - i, 0, 1);
            if (a <= 0) continue;
            const h = 28 + ((i * 7) % 10);
            b.rect(x1 + 3 + i * 13, y - 5 - h, 10, h, withAlpha(c.load, 0.3), c.load, 1.0, a);
          }
          b.text(x1 + (len * m) / 2, y - 82, ' = 2 kN/m', c.load, 15, 'middle', 'q');
          // metry pod policí
          for (let i = 0; i <= 4; i++) b.line(x1 + i * m, y + 8, x1 + i * m, y + 16, c.ink, 0.9);
          for (let i = 0; i < 4; i++) b.text(x1 + i * m + m / 2, y + 30, '1 m', c.inkSoft, 12);
          dim(b, x1, end, y + 54, `ℓ = ${num(len)} m`, c.ink);
          // po správném odhadu: kolik kN připadá na každý (i neúplný) metr
          if (reveal.value > 0) {
            let a = 0;
            while (a < len - 1e-9) {
              const bb = Math.min(a + 1, len);
              const xa = x1 + a * m;
              const xb = x1 + bb * m;
              b.line(xa + 3, y - 50, xb - 3, y - 50, c.derived, 1.4, false, reveal.value);
              b.text((xa + xb) / 2, y - 56, `${num(q * (bb - a))} kN`, c.derived, 13, 'middle', null, false, reveal.value);
              a = bb;
            }
          }
        }}
      />
      <CardText text={L('[[q]] = 2 kN/m znamená: **na každý metr** police připadají 2 kN knih.', '[[q]] = 2 кН/м означает: **на каждый метр** полки приходится 2 кН книг.')} />
      <Steps
        steps={[
          quizStep8(q1, t.pick, 4),
          numStep8(n2, L('Teď knihy zabírají jen 1,5 m. Kolik kN váží?', 'Теперь книги занимают только 1,5 м. Сколько кН они весят?'), undefined, undefined, () => (
            <Note
              tone="info"
              text={L(
                'q ještě není síla – je to síla **na metr** (kN/m). Sílu Q v kN dostaneš, až když q vynásobíš zatíženou délkou: kN/m · m = kN. Neúplný metr přidá i neúplnou část.',
                'q — это ещё не сила, а сила **на метр** (кН/м). Силу Q в кН получишь, только умножив q на нагруженную длину: кН/м · м = кН. Неполный метр добавляет и неполную часть.',
              )}
            />
          )),
        ]}
      />
    </>
  );
}

/** Zatížení na nosníku o rozpětí 6 m: q (kN/m) od [a] (m od A) na délce [l]. */
interface LoadCase {
  q: number;
  a: number;
  l: number;
}
const big = (k: LoadCase) => k.q * k.l;
const xcOf = (k: LoadCase) => k.a + k.l / 2;
const CASES: LoadCase[] = [
  { q: 2, a: 0, l: 3 },
  { q: 3, a: 2.5, l: 3 },
];
const SPAN = 6;

/**
 * Karta 1: nahradit = stejný účinek. Pod podporami jsou reakce od spojitého zatížení. Žák určí velikost Q,
 * postaví ji na místo, kde má působit, a nahradí jí zatížení. Reakce se nezmění, jen když Q leží uprostřed.
 */
function Resultant() {
  const t = useTr();
  const kit = useKit();
  const kn = t.pick(L('kN', 'кН'));
  const round = kit.mut(0);
  const placed = kit.mut(false); // první zatížení správně nahrazeno
  const xq = kit.mut(0);
  const grab = kit.mut(false);
  const merge = kit.anim(0);
  const q1 = useQuiz(L('Zatížení 2 kN/m na 3 m. Jak velká je výslednice Q?', 'Нагрузка 2 кН/м на 3 м. Какова величина равнодействующей Q?'), qOpts([2, 3, 5, 6], 2, 3), L('Q = q · ℓ = 2 · 3 = 6 kN.', 'Q = q · ℓ = 2 · 3 = 6 кН.'));
  const n3 = useNum(L('Druhé zatížení: velikost Q', 'Вторая нагрузка: величина Q'), [numSpec(L('Q', 'Q'), 9, kn)], L('Q = q · ℓ = 3 · 3 = 9 kN.', 'Q = q · ℓ = 3 · 3 = 9 кН.'), qTraps([3, 6, 4.5], 3, 3));
  const n4 = useNum(
    L('Druhé zatížení: poloha Q', 'Вторая нагрузка: положение Q'),
    [numSpec(L('x', 'x'), 4, t.pick(L('m', 'м')))],
    L('x = 2,5 + 3/2 = 4 m – uprostřed zatížené délky, měřeno od A.', 'x = 2,5 + 3/2 = 4 м — посередине нагруженного участка, считая от A.'),
    [
      trapNear(L('1,5 m je od **začátku zatížení**. To ale nezačíná v A.', '1,5 м — это от **начала нагрузки**. Но она начинается не в A.'), 1.5),
      trapNear(L('Tam zatížení začíná. Výslednice je v jeho středu.', 'Там нагрузка начинается. Равнодействующая — в её середине.'), 2.5),
      trapNear(L('Tam zatížení končí. Výslednice je v jeho středu.', 'Там нагрузка заканчивается. Равнодействующая — в её середине.'), 5.5),
    ],
    L('Střed zatížené délky = začátek zatížení + polovina jeho délky.', 'Середина нагруженного участка = начало нагрузки + половина её длины.'),
  );
  const lc = CASES[round.v];
  // Q je na obrázku: u prvního zatížení po odhadu velikosti, u druhého po prvním zapsaném místě
  const showQ = round.v === 0 ? q1.solved : n4.misses > 0 || n4.solved;
  const placing = round.v === 0 && q1.solved && !placed.v;
  const sizeKnown = round.v === 0 ? q1.solved : n3.solved;
  const moveQ = (v: number) => {
    if (!placing || merge.isRunning) return;
    xq.v = clamp(v, 0, SPAN);
    // posunutá Q už zatížení nenahrazuje – hřeben se vrátí
    if (merge.value > 0) kit.scope.launch(() => merge.animateTo(0, tween(400)));
  };
  const replace = () => {
    if (!placing || merge.isRunning) return;
    const ok = Math.abs(xq.v - xcOf(lc)) < 1e-9;
    kit.scope.launch(async () => {
      await merge.animateTo(1, tween(900));
      if (ok) placed.v = true;
    });
  };
  // po prvním zatížení: nový obrázek s druhým zatížením
  useLaunched([placed.v], async (job) => {
    if (placed.v && round.v === 0) {
      await job.delay(1800);
      round.v = 1;
      xq.v = 0;
      await merge.snapTo(0);
    }
  });
  // druhé zatížení: zapsané místo → Q se tam postaví a nahradí zatížení; reakce ukážou, jestli sedí
  useLaunched([n4.misses, n4.solved], async () => {
    if (round.v !== 1 || !(n4.misses > 0 || n4.solved)) return;
    const v = n4.solved ? 4 : parseNum(n4.values[0]);
    if (v === null) return;
    await merge.snapTo(0);
    xq.v = clamp(v, 0, SPAN);
    await merge.animateTo(1, tween(900));
  });
  // přepsané číslo: zatížení se vrátí, dokud žák znovu nezkontroluje
  useLaunched([n4.values], async () => {
    if (round.v === 1 && !n4.solved && merge.value > 0) await merge.animateTo(0, tween(400));
  });
  const rbDist = (big(lc) * xcOf(lc)) / SPAN;
  const raDist = big(lc) - rbDist;
  const rbQ = (big(lc) * xq.v) / SPAN;
  const raQ = big(lc) - rbQ;
  const same = Math.abs(xq.v - xcOf(lc)) < 1e-9;
  const y = 108;
  const xA = 50;
  const m = 50;
  const gx = (v: number) => xA + v * m;
  const sameNote = (k: LoadCase) =>
    L(
      `Reakce se **nezměnily** – Q = ${num(big(k))} kN uprostřed zatížené délky (${num(k.a)} + ${num(k.l)}/2 = ${num(xcOf(k))} m od A) má na nosník stejný účinek jako celé spojité zatížení.`,
      `Реакции **не изменились** — Q = ${num(big(k))} кН посередине нагруженного участка (${num(k.a)} + ${num(k.l)}/2 = ${num(xcOf(k))} м от A) действует на балку так же, как вся распределённая нагрузка.`,
    );
  return (
    <>
      <Stage
        h={232}
        description={t.pick(L('Spojité zatížení na nosníku a jeho výslednice', 'Распределённая нагрузка на балке и её равнодействующая'))}
        onDrag={(pt, start) => {
          if (!placing) return;
          if (start) grab.v = dist(pt, gx(xq.v), y - 5 - big(lc) * 8) < 45 || Math.abs(pt.x - gx(xq.v)) < 30;
          if (grab.v) moveQ(Math.round(((pt.x - xA) / m) * 2) / 2);
        }}
        onDragEnd={() => (grab.v = false)}
        draw={(b, c) => {
          const mv = merge.value;
          b.support('pin', gx(0), y, 'A');
          b.support('roller', gx(SPAN), y, 'B');
          b.beam(gx(0) - 10, gx(SPAN) + 10, y);
          // spojité zatížení (výška ∝ q) – po nahrazení úplně zmizí
          comb(b, gx(lc.a), gx(lc.a + lc.l), y - 5 - lc.q * 12, y - 5, c, mv * 0.6, 1 - mv);
          if (mv < 0.5) b.text(gx(xcOf(lc)), y - 5 - lc.q * 12 - 8, ` = ${num(lc.q)} kN/m`, c.load, 14, 'middle', 'q', false, 1 - mv * 2);
          // výslednice (délka ∝ Q): při hledání polohy čárkovaná s ručkou
          if (showQ) {
            const len = big(lc) * 8;
            const xx = gx(xq.v);
            const a = mv < 0.05 ? 0.9 : Math.max(mv, 0.3);
            b.force(xx, y - 5 - len, xx, y - 5, c.derived, 2.6, 11, false, a, mv < 0.5);
            b.text(xx, y - 5 - len - 16, ` = ${num(big(lc))} kN`, c.derived, 14, 'middle', 'Q', false, a);
            if (placing) b.handle(xx, y - 5 - len, grab.v);
            dim(b, gx(0), xx, y + 106, `x = ${num(xq.v)} m`, c.derived, xq.v > 0 ? a : 0);
          }
          // reakce podpor: před nahrazením od spojitého zatížení, po nahrazení od Q
          const after = mv > 0.5;
          const ra = after ? raQ : raDist;
          const rb = after ? rbQ : rbDist;
          const rc = !after ? c.react : same ? c.accent : c.load;
          b.force(gx(0), y + 62, gx(0), y + 6, c.react);
          b.force(gx(SPAN), y + 62, gx(SPAN), y + 6, c.react);
          const mark = !after ? '' : same ? ' ✓' : ' ✗';
          b.text(gx(0) + 10, y + 58, sizeKnown ? ` = ${num(ra)} kN${mark}` : ' = ?', rc, 14, 'start', 'R_A');
          b.text(gx(SPAN) - 10, y + 58, sizeKnown ? ` = ${num(rb)} kN${mark}` : ' = ?', rc, 14, 'end', 'R_B');
          // kóty zatížení
          if (lc.a > 0) dim(b, gx(0), gx(lc.a), y + 84, `${num(lc.a)} m`, c.ink);
          dim(b, gx(lc.a), gx(lc.a + lc.l), y + 84, `ℓ = ${num(lc.l)} m`, c.ink);
        }}
      />
      <CardText
        text={L(
          'Spojité zatížení (sníh, knihy) chceme nahradit **jedinou silou Q** – výslednicí. „Nahradit“ znamená, že to nosník nepozná: podpory musí tlačit **stejně**.',
          'Распределённую нагрузку (снег, книги) хотим заменить **одной силой Q** — равнодействующей. «Заменить» значит, что балка разницы не заметит: опоры должны давить **так же**.',
        )}
      />
      <SmallText
        text={L(
          'Pod podporami jsou reakce, které drží spojité zatížení – čísla uvidíš, až určíš velikost Q. Šipky reakcí nejsou v měřítku.',
          'Под опорами — реакции, которые держат распределённую нагрузку; числа появятся, когда найдёшь величину Q. Стрелки реакций не в масштабе.',
        )}
      />
      <Steps
        steps={[
          quizStep8(q1, t.pick, 4),
          step(
            L('Postav Q tam, kde má působit, a nahraď jí zatížení', 'Поставь Q туда, где она должна действовать, и замени ею нагрузку'),
            placed.v,
            () => (
              <>
                <Chips options={[L('◀ 0,5 m', '◀ 0,5 м'), L('0,5 m ▶', '0,5 м ▶')]} onPick={(i) => moveQ(xq.v + (i === 0 ? -0.5 : 0.5))} />
                <Readout text={t.pick(L(`Q je ${num(xq.v)} m od A`, `Q в ${num(xq.v)} м от A`))} />
                <ActionButton text={L('Nahradit zatížení silou Q', 'Заменить нагрузку силой Q')} enabled={merge.value === 0 && !merge.isRunning} onClick={replace} />
                {merge.value > 0.5 && !same && !merge.isRunning && (
                  <Note
                    tone="nudge"
                    text={L(
                      `Reakce vyšly **jinak** – Q v ${num(xq.v)} m má jiný účinek než zatížení. Rovnoměrné zatížení je kolem svého středu souměrné. Posuň Q a zkus to znovu.`,
                      `Реакции получились **другими** — Q в ${num(xq.v)} м действует иначе, чем нагрузка. Равномерная нагрузка симметрична относительно своей середины. Сдвинь Q и попробуй снова.`,
                    )}
                  />
                )}
              </>
            ),
            () => <Note tone="ok" text={sameNote(CASES[0])} />,
          ),
          numStep8(n3, L('Druhé zatížení: 3 kN/m na 3 m. Jak velká je Q?', 'Вторая нагрузка: 3 кН/м на 3 м. Какова величина Q?')),
          numStep8(
            n4,
            L('Zatížení začíná 2,5 m od A. Jak daleko od A působí Q?', 'Нагрузка начинается в 2,5 м от A. На каком расстоянии от A действует Q?'),
            undefined,
            () => <Note tone="ok" text={sameNote(CASES[1])} />,
            () => (
              <SmallText
                text={L(
                  'Výslednice nahrazuje zatížení jen pro rovnováhu celého nosníku (reakce). Jak se nosník uvnitř prohýbá, to už je jiná otázka.',
                  'Равнодействующая заменяет нагрузку только для равновесия всей балки (реакций). Как балка изгибается внутри — это уже другой вопрос.',
                )}
              />
            ),
          ),
        ]}
      />
    </>
  );
}

/** Karta 2: kontrola – výslednice a její rameno, když zatížení nezačíná v podpoře. */
function CheckQ() {
  const t = useTr();
  const q1 = useQuiz(
    L('Čím tohle spojité zatížení nahradíš?', 'Чем заменишь эту распределённую нагрузку?'),
    [
      opt(L('Q = 6 kN uprostřed zatížení', 'Q = 6 кН посередине нагрузки'), true),
      opt(L('Q = 2 kN uprostřed zatížení', 'Q = 2 кН посередине нагрузки'), false, L('2 kN připadají jen na jeden metr. Kolik metrů je zatíženo?', '2 кН приходится только на один метр. Сколько метров нагружено?')),
      opt(
        L('Q = 6 kN na konci zatížení', 'Q = 6 кН на конце нагрузки'),
        false,
        L(
          'Zatížení je všude stejné, takže vlevo od Q ho musí být stejně jako vpravo. Kde bys rovnoměrně naloženou polici podepřel jedním prstem?',
          'Нагрузка везде одинакова, значит, слева от Q её должно быть столько же, сколько справа. Где бы ты подпёр равномерно загруженную полку одним пальцем?',
        ),
      ),
    ],
    L('Q = q · ℓ = 2 · 3 = 6 kN. Působí uprostřed zatížené délky, tedy 1,5 m od jejího začátku.', 'Q = q · ℓ = 2 · 3 = 6 кН. Приложена посередине нагруженного участка, то есть в 1,5 м от его начала.'),
  );
  const n2 = useNum(
    L('Rameno Q k bodu A', 'Плечо Q относительно A'),
    [numSpec(L('r', 'r'), 2.5, t.pick(L('m', 'м')))],
    L(
      'Rameno = 1 m (od A k začátku zatížení) + ℓ/2 = 1 + 1,5 = **2,5 m**. Moment Q k bodu A: 6 · 2,5 = 15 kNm. Častá chyba je zapomenout na ten první metr.',
      'Плечо = 1 м (от A до начала нагрузки) + ℓ/2 = 1 + 1,5 = **2,5 м**. Момент Q относительно A: 6 · 2,5 = 15 кН·м. Частая ошибка — забыть про этот первый метр.',
    ),
    [
      trapNear(L('1,5 m je od **začátku zatížení**. Zatížení ale nezačíná v A – začíná až o 1 m dál.', '1,5 м — это от **начала нагрузки**. Но нагрузка начинается не в A, а на 1 м дальше.'), 1.5),
      trapNear(L('3 m je délka zatížení ℓ, ne vzdálenost výslednice od A.', '3 м — это длина нагрузки ℓ, а не расстояние от равнодействующей до A.'), 3),
      trapNear(L('4 m od A zatížení končí. Výslednice působí uprostřed zatížené délky.', 'В 4 м от A нагрузка заканчивается. Равнодействующая приложена посередине нагруженного участка.'), 4),
    ],
  );
  const y = 100;
  const xA = 70;
  const m = 52; // rozpětí 5 m
  return (
    <>
      <Stage
        h={200}
        description={t.pick(L('Spojité zatížení 2 kN/m na délce 3 m, začíná 1 m od podpory A', 'Распределённая нагрузка 2 кН/м на длине 3 м, начинается в 1 м от опоры A'))}
        draw={(b, c) => {
          b.support('pin', xA, y, 'A');
          b.support('roller', xA + 5 * m, y, 'B');
          b.beam(xA - 15, xA + 5 * m + 15, y);
          if (!q1.solved) {
            comb(b, xA + m, xA + 4 * m, y - 5 - 30, y - 5, c);
            b.text(xA + 2.5 * m, y - 44, ' = 2 kN/m', c.load, 15, 'middle', 'q');
          } else {
            // po vyřešení: zatížení nahradila výslednice
            b.force(xA + 2.5 * m, y - 5 - 48, xA + 2.5 * m, y - 5, c.derived, 2.6);
            b.text(xA + 2.5 * m + 8, y - 40, ' = 6 kN', c.derived, 14, 'start', 'Q');
            dim(b, xA, xA + 2.5 * m, y + 80, n2.solved ? '2,5 m' : 'r = ?', c.derived);
          }
          dim(b, xA, xA + m, y + 54, '1 m', c.ink);
          dim(b, xA + m, xA + 4 * m, y + 54, '3 m', c.ink);
        }}
      />
      <CardText text={L('Zatížení [[q]] = 2 kN/m začíná 1 m od podpory A a je dlouhé 3 m.', 'Нагрузка [[q]] = 2 кН/м начинается в 1 м от опоры A и имеет длину 3 м.')} />
      <Steps steps={[quizStep8(q1, t.pick), numStep8(n2, L('Jaké rameno má Q k bodu A?', 'Какое плечо у Q относительно точки A?'))]} />
    </>
  );
}

/**
 * Karta 3: celý nosník od začátku do konce – uvolnění, výslednice, složky šikmé síly, momentové rovnice
 * a kontrola. Prostý nosník 6 m: q = 2 kN/m na 0–3 m, F = 5 kN (sklon 3 : 4) ve 4,5 m.
 * R_B = 4,5 kN, R_Ay = 5,5 kN, R_Ax = −3 kN.
 */
function WholeBeam() {
  const t = useTr();
  const kit = useKit();
  const kn = t.pick(L('kN', 'кН'));
  const q1 = useQuiz(
    L('Uvolni nosník: čím nahradíš podpory A a B?', 'Освободи балку: чем заменишь опоры A и B?'),
    [
      opt(
        L('A: [[R_Ay]] · B: [[R_B]]', 'A: [[R_Ay]] · B: [[R_B]]'),
        false,
        L('Pevná kloubová podpora A nedovolí ani vodorovný posun – a šikmá síla F nosník do strany táhne.', 'Шарнирно-неподвижная опора A не даёт и горизонтального сдвига — а наклонная сила F тянет балку вбок.'),
      ),
      opt(L('A: [[R_Ax]], [[R_Ay]] · B: [[R_B]]', 'A: [[R_Ax]], [[R_Ay]] · B: [[R_B]]'), true),
      opt(
        L('A: [[R_Ax]], [[R_Ay]] · B: [[R_Bx]], [[R_By]]', 'A: [[R_Ax]], [[R_Ay]] · B: [[R_Bx]], [[R_By]]'),
        false,
        L('Posuvná podpora B dovolí vodorovný posun – vodorovnou reakci nemá.', 'Шарнирно-подвижная опора B допускает горизонтальный сдвиг — горизонтальной реакции у неё нет.'),
      ),
    ],
    L('Neznámé: [[R_Ax]], [[R_Ay]], [[R_B]] – n = 3 = r.', 'Неизвестные: [[R_Ax]], [[R_Ay]], [[R_B]] — n = 3 = r.'),
  );
  const q2 = useQuiz(
    L('Čím nahradíš zatížení q = 2 kN/m na prvních 3 m?', 'Чем заменишь нагрузку q = 2 кН/м на первых 3 м?'),
    [
      opt(L('Q = 6 kN, 1,5 m od A', 'Q = 6 кН, 1,5 м от A'), true),
      opt(L('Q = 6 kN, 3 m od A', 'Q = 6 кН, 3 м от A'), false, L('3 m od A zatížení končí. Výslednice působí uprostřed zatížené délky.', 'В 3 м от A нагрузка заканчивается. Равнодействующая приложена посередине нагруженного участка.')),
      opt(L('Q = 2 kN, 1,5 m od A', 'Q = 2 кН, 1,5 м от A'), false, L('2 kN je jen na jeden metr: Q = q · ℓ.', '2 кН — только на один метр: Q = q · ℓ.')),
    ],
    L('Q = 2 · 3 = 6 kN, 1,5 m od A.', 'Q = 2 · 3 = 6 кН, 1,5 м от A.'),
  );
  const q3 = useQuiz(
    L('F = 5 kN má sklon 3 : 4. Jaké jsou její složky?', 'F = 5 кН с наклоном 3 : 4. Каковы её составляющие?'),
    [
      opt(
        L('[[F_x]] = 4 kN, [[F_y]] = 3 kN', '[[F_x]] = 4 кН, [[F_y]] = 3 кН'),
        false,
        L('Prohozené. Vodorovně je to 3 dílky, svisle 4 – jako trojúhelník 3-4-5 z lekce 3.', 'Перепутано. По горизонтали 3 деления, по вертикали 4 — как треугольник 3-4-5 из урока 3.'),
      ),
      opt(L('[[F_x]] = 3 kN →, [[F_y]] = 4 kN ↓', '[[F_x]] = 3 кН →, [[F_y]] = 4 кН ↓'), true),
      opt(L('[[F_x]] = 5 kN, [[F_y]] = 5 kN', '[[F_x]] = 5 кН, [[F_y]] = 5 кН'), false, L('Složky nejsou kopie síly: 5 je přepona, složky jsou odvěsny 3 a 4.', 'Составляющие — не копии силы: 5 — гипотенуза, составляющие — катеты 3 и 4.')),
    ],
    L('[[F_x]] = 5 · 3/5 = 3 kN →, [[F_y]] = 5 · 4/5 = 4 kN ↓.', '[[F_x]] = 5 · 3/5 = 3 кН →, [[F_y]] = 5 · 4/5 = 4 кН ↓.'),
  );
  const q4 = useQuiz(
    L('Chceš rovnou [[R_B]]. Ke kterému bodu sečteš momenty?', 'Хочешь сразу [[R_B]]. Относительно какой точки сложишь моменты?'),
    [
      opt(L('k bodu A', 'относительно A'), true),
      opt(L('k bodu B', 'относительно B'), false, L('K bodu B má [[R_B]] rameno 0 – z rovnice by zmizela právě ta reakce, kterou hledáš.', 'Относительно B у [[R_B]] плечо 0 — из уравнения исчезнет как раз та реакция, которую ищешь.')),
      opt(L('ke středu nosníku', 'к середине балки'), false, L('Ke středu by v rovnici zůstaly [[R_Ay]] i [[R_B]] – dvě neznámé.', 'Относительно середины в уравнении останутся и [[R_Ay]], и [[R_B]] — две неизвестные.')),
    ],
    L('K bodu A zmizí [[R_Ax]] i [[R_Ay]] – zbude jen [[R_B]].', 'Относительно A исчезают [[R_Ax]] и [[R_Ay]] — остаётся только [[R_B]].'),
  );
  const n5 = useNum(
    L('R_B z momentů k A', 'R_B из моментов относительно A'),
    [numSpec(L('[[R_B]]', '[[R_B]]'), 4.5, kn)],
    L('[[R_B]] = (6 · 1,5 + 4 · 4,5) / 6 = 27/6 = 4,5 kN.', '[[R_B]] = (6 · 1,5 + 4 · 4,5) / 6 = 27/6 = 4,5 кН.'),
    [
      trapNear(L('27 kNm je součet momentů zatížení. Ještě vyděl ramenem 6 m.', '27 кН·м — сумма моментов нагрузок. Ещё раздели на плечо 6 м.'), 27),
      trapNear(L('Q má rameno 1,5 m, ne 3 m: (6 · 1,5 + 4 · 4,5) / 6.', 'У Q плечо 1,5 м, а не 3 м: (6 · 1,5 + 4 · 4,5) / 6.'), 6),
    ],
    L('Převeď oba záporné členy na pravou stranu a vyděl 6.', 'Перенеси оба отрицательных члена вправо и раздели на 6.'),
  );
  const n6 = useNum(
    L('R_Ay z momentů k B', 'R_Ay из моментов относительно B'),
    [numSpec(L('[[R_Ay]]', '[[R_Ay]]'), 5.5, kn)],
    L('[[R_Ay]] = (6 · 4,5 + 4 · 1,5) / 6 = 33/6 = 5,5 kN.', '[[R_Ay]] = (6 · 4,5 + 4 · 1,5) / 6 = 33/6 = 5,5 кН.'),
    [
      trapNear(L('To je [[R_B]]. Dosaď: 6 · 4,5 + 4 · 1,5 = 27 + 6 = 33, a to vyděl ramenem 6 m.', 'Это [[R_B]]. Подставь: 6 · 4,5 + 4 · 1,5 = 27 + 6 = 33, и раздели на плечо 6 м.'), 4.5),
      trapNear(L('10 kN je součet zatížení – nesou ho obě podpory dohromady.', '10 кН — сумма нагрузок, её несут обе опоры вместе.'), 10),
      trapNear(L('33 kNm je součet momentů. Ještě vyděl ramenem 6 m.', '33 кН·м — сумма моментов. Ещё раздели на плечо 6 м.'), 33),
    ],
    L('K bodu B: Q má rameno 4,5 m, [[F_y]] rameno 1,5 m, [[R_Ay]] rameno 6 m.', 'Относительно B: у Q плечо 4,5 м, у [[F_y]] — 1,5 м, у [[R_Ay]] — 6 м.'),
  );
  const n7 = useNum(
    L('R_Ax ze sil vodorovně', 'R_Ax из сил по горизонтали'),
    [numSpec(L('[[R_Ax]]', '[[R_Ax]]'), -3, kn, true)],
    L('[[R_Ax]] = −3 kN: reakce ve skutečnosti míří doleva ←.', '[[R_Ax]] = −3 кН: на самом деле реакция направлена влево ←.'),
    [
      numTrap(
        L('Složka [[F_x]] táhne doprava. Kam musí mířit reakce, aby ji vyrušila – a jaké to dá znaménko?', 'Составляющая [[F_x]] тянет вправо. Куда должна быть направлена реакция, чтобы её уравновесить, — и какой это даёт знак?'),
        (v) => v[0] != null && Math.abs(v[0] - 3) < 0.02,
      ),
    ],
    L('Převeď 3 na druhou stranu rovnice. Znaménko nech, jak vyjde.', 'Перенеси 3 в другую часть уравнения. Знак оставь таким, как получится.'),
  );
  const n8 = useNum(
    L('Kontrola ΣF_y', 'Проверка ΣF_y'),
    [numSpec(L('Σ[[F_y]]', 'Σ[[F_y]]'), 0, kn, true)],
    L('5,5 + 4,5 − 6 − 4 = 10 − 10 = 0.', '5,5 + 4,5 − 6 − 4 = 10 − 10 = 0.'),
    [trapNear(L('Zatížení míří dolů, takže se odečítají: 10 − 10.', 'Нагрузки направлены вниз, поэтому вычитаются: 10 − 10.'), 20)],
    L('Sečti zvlášť nahoru (5,5 + 4,5) a dolů (6 + 4).', 'Сложи отдельно вверх (5,5 + 4,5) и вниз (6 + 4).'),
  );
  const solved = [q1.solved, q2.solved, q3.solved, q4.solved, n5.solved, n6.solved, n7.solved, n8.solved];
  const k0 = solved.indexOf(false);
  const stepNo = k0 < 0 ? solved.length : k0; // počet hotových kroků
  const prog = kit.anim(0);
  useLaunched([stepNo], async () => {
    await prog.animateTo(stepNo, tween(700));
  });
  const ph = (k: number) => clamp(prog.value - (k - 1), 0, 1); // průběh přechodu po kroku k (1…8)
  const y = 118;
  const xA = 50;
  const m = 50;
  const gx = (v: number) => xA + v * m;
  return (
    <>
      <Stage
        h={240}
        description={t.pick(L('Prostý nosník se spojitým zatížením a šikmou silou', 'Простая балка с распределённой нагрузкой и наклонной силой'))}
        draw={(b, c) => {
          const p1 = ph(1);
          const p2 = ph(2);
          const p3 = ph(3);
          const p4 = ph(4);
          const p5 = ph(5);
          const p6 = ph(6);
          const p7 = ph(7);
          // podpory → reakce (kladné smysly → ↑ ↑)
          if (p1 < 1) {
            b.support('pin', gx(0), y, 'A', p1 > 0);
            b.support('roller', gx(6), y, 'B', p1 > 0);
          }
          b.beam(gx(0) - 10, gx(6) + 10, y);
          if (p1 > 0) {
            b.force(gx(0) - 44, y, gx(0) - 12, y, c.react, 2.4, 11, false, p1);
            b.force(gx(0), y + 48, gx(0), y + 6, c.react, 2.4, 11, false, p1);
            b.force(gx(6), y + 48, gx(6), y + 6, c.react, 2.4, 11, false, p1);
            b.text(gx(0) - 28, y - 10, p7 > 0.5 ? ' = −3' : '', c.react, 13, 'middle', 'R_Ax', false, p1);
            b.text(gx(0) + 8, y + 44, p6 > 0.5 ? ' = 5,5 kN' : null, c.react, 13, 'start', 'R_Ay', false, p1);
            b.text(gx(6) - 8, y + 44, p5 > 0.5 ? ' = 4,5 kN' : null, c.react, 13, 'end', 'R_B', false, p1);
            // skutečný smysl R_Ax čárkovaně
            if (p7 > 0.5) b.force(gx(0) - 12, y + 16, gx(0) - 44, y + 16, c.react, 1.4, 8, false, p7 * 0.7, true);
          }
          // spojité zatížení → výslednice Q
          comb(b, gx(0), gx(3), y - 5 - 24, y - 5, c, p2 * 0.6, 1 - p2);
          if (p2 < 0.5) b.text(gx(1.5), y - 38, ' = 2 kN/m', c.load, 14, 'middle', 'q', false, 1 - p2 * 2);
          if (p2 > 0) {
            b.force(gx(1.5), y - 5 - 48, gx(1.5), y - 5, c.derived, 2.6, 11, false, p2);
            b.text(gx(1.5) + 8, y - 44, ' = 6 kN', c.derived, 14, 'start', 'Q', false, p2);
          }
          // šikmá síla F (3 : 4) → složky ve stejném působišti
          const px = gx(4.5);
          const py = y - 5;
          if (p3 < 1) {
            b.force(px - 36, py - 48, px, py, c.load, 2.6, 11, false, 1 - p3);
            b.text(px - 40, py - 52, ' = 5 kN', c.load, 14, 'end', 'F', false, 1 - p3);
            // sklon 3 : 4 tence vedle šipky
            b.line(px - 36, py - 48, px, py - 48, c.inkSoft, 0.8, true, 1 - p3);
            b.line(px, py - 48, px, py, c.inkSoft, 0.8, true, 1 - p3);
            b.text(px - 18, py - 53, '3', c.inkSoft, 11, 'middle', null, false, 1 - p3);
            b.text(px + 7, py - 20, '4', c.inkSoft, 11, 'start', null, false, 1 - p3);
          }
          if (p3 > 0) {
            b.force(px, py - 48, px, py, c.load, 2.4, 11, false, p3);
            b.text(px + 8, py - 34, ' = 4 kN', c.load, 13, 'start', 'F_y', false, p3);
            b.force(px - 36, py - 2, px, py - 2, c.load, 2.4, 11, false, p3);
            b.text(px - 40, py - 12, ' = 3 kN', c.load, 13, 'end', 'F_x', false, p3);
          }
          // momentový bod: A pro krok 5, B pro krok 6
          if (p4 > 0 && p5 < 1) b.circle(gx(0), y, 12, withAlpha(c.accent, 0.15), c.accent, 1.6, p4 * (1 - p5));
          if (p5 > 0 && p6 < 1) b.circle(gx(6), y, 12, withAlpha(c.accent, 0.15), c.accent, 1.6, p5 * (1 - p6));
          // kóty
          const yd = y + 76;
          const marks = [0, 1.5, 3, 4.5, 6];
          b.line(gx(0), yd, gx(6), yd, c.ink, 0.9);
          for (const mk of marks) b.line(gx(mk) - 4, yd + 4, gx(mk) + 4, yd - 4, c.ink, 1.2);
          for (let i = 0; i < marks.length - 1; i++) b.text((gx(marks[i]) + gx(marks[i + 1])) / 2, yd - 5, num(marks[i + 1] - marks[i]), c.ink, 12);
          b.text(gx(6) + 8, yd + 4, 'm', c.inkSoft, 11, 'start');
          if (stepNo >= 8) b.text(200, yd + 30, t.pick(L('Σ F_y = 0 ✓   Σ M = 0 ✓', 'Σ F_y = 0 ✓   Σ M = 0 ✓')), c.accent, 15, 'middle', null, true);
        }}
      />
      <CardText
        text={L(
          'Teď všechno dohromady: spojité zatížení **i** šikmá síla na jednom nosníku. Postupuj tak, jak budeš řešit úlohy v trenažéru.',
          'Теперь всё вместе: распределённая нагрузка **и** наклонная сила на одной балке. Действуй так же, как будешь решать задачи в тренажёре.',
        )}
      />
      <Steps
        steps={[
          quizStep8(q1, t.pick, 1, q1.question, () => <SolLine text={L('Neznámé: [[R_Ax]], [[R_Ay]], [[R_B]] – n = 3 = r', 'Неизвестные: [[R_Ax]], [[R_Ay]], [[R_B]] — n = 3 = r')} />),
          quizStep8(q2, t.pick, 1, q2.question, () => <SolLine text={L('Q = 2 · 3 = 6 kN, 1,5 m od A', 'Q = 2 · 3 = 6 кН, 1,5 м от A')} />),
          quizStep8(q3, t.pick, 1, q3.question, () => <SolLine text={L('[[F_x]] = 3 kN →,  [[F_y]] = 4 kN ↓', '[[F_x]] = 3 кН →,  [[F_y]] = 4 кН ↓')} />),
          quizStep8(q4, t.pick, 1, q4.question, () => <SolLine text={L('Σ[[M_A]]: [[R_B]]·6 − 6·1,5 − 4·4,5 = 0', 'Σ[[M_A]]: [[R_B]]·6 − 6·1,5 − 4·4,5 = 0')} />),
          numStep8(
            n5,
            L('Vyřeš rovnici Σ[[M_A]] = 0: kolik je [[R_B]]?', 'Реши уравнение Σ[[M_A]] = 0: чему равна [[R_B]]?'),
            () => <Readout text="[[R_B]]·6 − 6·1,5 − 4·4,5 = 0" />,
            () => <SolLine text={L('[[R_B]] = 27/6 = 4,5 kN', '[[R_B]] = 27/6 = 4,5 кН')} />,
          ),
          numStep8(
            n6,
            L('Momenty k bodu B: kolik je [[R_Ay]]?', 'Моменты относительно B: чему равна [[R_Ay]]?'),
            () => <Readout text="−[[R_Ay]]·6 + 6·4,5 + 4·1,5 = 0" />,
            () => <SolLine text={L('Σ[[M_B]]: [[R_Ay]] = 33/6 = 5,5 kN', 'Σ[[M_B]]: [[R_Ay]] = 33/6 = 5,5 кН')} />,
          ),
          numStep8(n7, L('Vodorovně: [[R_Ax]] + 3 = 0. Kolik je [[R_Ax]] (se znaménkem)?', 'По горизонтали: [[R_Ax]] + 3 = 0. Чему равна [[R_Ax]] (со знаком)?'), undefined, () => (
            <SolLine text={L('Σ[[F_x]]: [[R_Ax]] = −3 kN (ve skutečnosti ←)', 'Σ[[F_x]]: [[R_Ax]] = −3 кН (на самом деле ←)')} />
          )),
          numStep8(
            n8,
            L('Kontrola: kolik vyjde Σ[[F_y]] = 5,5 + 4,5 − 6 − 4?', 'Проверка: чему равна Σ[[F_y]] = 5,5 + 4,5 − 6 − 4?'),
            undefined,
            () => <SolLine text={L('Σ[[F_y]] = 0 ✓', 'Σ[[F_y]] = 0 ✓')} ok />,
            () => (
              <Note
                tone="ok"
                text={L(
                  'Hotovo: [[R_Ax]] = −3 kN (←), [[R_Ay]] = 5,5 kN, [[R_B]] = 4,5 kN. Postup: uvolnit → nahradit q výslednicí → rozložit šikmé síly → momenty k bodu, kde zmizí co nejvíc neznámých → kontrola.',
                  'Готово: [[R_Ax]] = −3 кН (←), [[R_Ay]] = 5,5 кН, [[R_B]] = 4,5 кН. Порядок: освободить → заменить q равнодействующей → разложить наклонные силы → моменты относительно точки, где исчезает больше всего неизвестных → проверка.',
                )}
              />
            ),
          ),
        ]}
      />
    </>
  );
}

export const cards = [PerMetre, Resultant, CheckQ, WholeBeam];
