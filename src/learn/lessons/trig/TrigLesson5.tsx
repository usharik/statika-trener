/*
 * Kurz trigonometrie – lekce 5 (id 105): Pythagorova věta, sin² + cos² = 1, tg = sin / cos a doplňkový úhel.
 * (Přepis z aplikace: learn/trig/TrigLesson5.kt.)
 */
import type { ReactNode } from 'react';
import {
  ActionButton,
  CardText,
  L,
  Note,
  Readout,
  SmallText,
  Stage,
  Steps,
  ValueSlider,
  alpha as withAlpha,
  num,
  numSpec,
  opt,
  pt,
  screenAngle,
  step,
  trapNear,
  triGeom,
  useKit,
  useLaunched,
  useNumStep,
  useQuizStep,
  useTr,
  type Colors,
  type Pt,
  type PrimBuilder,
  type Step,
} from '../../kit';
import { Trig } from '../../../practice/trig';
import { useGuessStep } from './shared';

/** Tip jako krok (volby a „Ověřit“ v doku); volitelně vlastní výsledek. */
function usePredictStep(label: L, options: L[], guess: number | null, onGuess: (i: number) => void, result?: () => ReactNode): Step {
  const s = useGuessStep(label, options, guess, onGuess);
  return result ? { ...s, result } : s;
}

/** Čtverec nad úsečkou p→q (na stranu nx, ny = jednotková normála) s mřížkou n × n dílků. */
function areaSquare(b: PrimBuilder, p: Pt, q: Pt, nx: number, ny: number, n: number, color: string, a = 1) {
  const len = Math.hypot(q.x - p.x, q.y - p.y);
  const p3 = pt(q.x + nx * len, q.y + ny * len);
  const p4 = pt(p.x + nx * len, p.y + ny * len);
  b.poly([[p.x, p.y], [q.x, q.y], [p3.x, p3.y], [p4.x, p4.y]], withAlpha(color, 0.16 * a), color, 1.2, a);
  if (n >= 2 && n <= 12)
    for (let i = 1; i < n; i++) {
      const t = i / n;
      b.line(p.x + (q.x - p.x) * t, p.y + (q.y - p.y) * t, p4.x + (p3.x - p4.x) * t, p4.y + (p3.y - p4.y) * t, color, 0.6, false, 0.45 * a);
      b.line(p.x + nx * len * t, p.y + ny * len * t, q.x + nx * len * t, q.y + ny * len * t, color, 0.6, false, 0.45 * a);
    }
}

/** Karta 0: Pythagorova věta – čtverce nad stranami. Tip přepony 3-4, ukázka ploch, pak lano 12 m × 5 m. */
function Pythagoras() {
  const t = useTr();
  const kit = useKit();
  const guess = kit.mut<number | null>(null);
  const phase = kit.mut(0); // ukázka: 0 nic, 1 a², 2 b², 3 c², 4 c
  const demoDone = kit.mut(false);
  const a = kit.mut(3);
  const bb = kit.mut(4);
  const explored = kit.mut(false);
  useLaunched([guess.v !== null], async (job) => {
    if (guess.v === null) return;
    for (let p = 1; p <= 4; p++) {
      await job.delay(800);
      phase.v = p;
    }
    await job.delay(500);
    demoDone.v = true;
  });
  const tips = [L('c = 5', 'c = 5'), L('c = 7', 'c = 7'), L('c = 25', 'c = 25')];
  const s1 = usePredictStep(L('Odhadni přeponu při odvěsnách 3 a 4', 'Предскажи гипотенузу при катетах 3 и 4'), tips, guess.v, (i) => (guess.v = i));
  const s2 = step(
    L('Porovnej plochy čtverců nad stranami', 'Сравни площади квадратов на сторонах'),
    demoDone.v,
    () => <SmallText text={L('Sleduj obrázek: kolik čtverečků je nad každou stranou…', 'Смотри на рисунок: сколько клеток на каждой стороне…')} />,
    () =>
      guess.v === 0 ? (
        <Note tone="ok" text={L('Tip sedí: 9 + 16 = 25 čtverečků, takže c = √25 = 5.', 'Прогноз верен: 9 + 16 = 25 клеток, значит c = √25 = 5.')} />
      ) : guess.v === 1 ? (
        <Note
          tone="info"
          text={L(
            '7 = 3 + 4 by vyšlo, kdyby se sčítaly délky. Sčítají se ale **plochy** čtverců: 9 + 16 = 25, tedy c = 5.',
            '7 = 3 + 4 получилось бы, если бы складывались длины. Но складываются **площади** квадратов: 9 + 16 = 25, значит c = 5.',
          )}
        />
      ) : (
        <Note tone="info" text={L('25 je plocha čtverce nad přeponou, tedy c². Délka je c = √25 = 5.', '25 — площадь квадрата на гипотенузе, то есть c². Длина c = √25 = 5.')} />
      ),
  );
  const s3 = useNumStep(
    L('Lano vede mezi body vzdálenými 12 m vodorovně a 5 m svisle. Jak je dlouhé?', 'Трос натянут между точками, разнесёнными на 12 м по горизонтали и 5 м по вертикали. Какова его длина?'),
    [numSpec(L('c', 'c'), 13, t.pick(L('m', 'м')))],
    L(
      'c² = 5² + 12² = 25 + 144 = 169 m², tedy c = √169 = 13 m. Sčítáme plochy čtverců, ne délky stran.',
      'c² = 5² + 12² = 25 + 144 = 169 м², значит c = √169 = 13 м. Складываются площади квадратов, а не длины сторон.',
    ),
    [
      trapNear(L('Sčítají se **druhé mocniny**, ne strany. Kolik je 5² + 12²?', 'Складываются **квадраты**, а не сами стороны. Сколько будет 5² + 12²?'), 17),
      trapNear(L('169 m² je teprve c² – plocha. Co ještě zbývá udělat?', '169 м² — это пока c², площадь. Что ещё осталось сделать?'), 169),
    ],
    L('c² = 12² + 5². Nejdřív sečti druhé mocniny, pak odmocni.', 'c² = 12² + 5². Сначала сложи квадраты, потом извлеки корень.'),
    L('Vypočítej délku lana mezi vyznačenými body', 'Вычисли длину троса между отмеченными точками'),
  );
  const steps = [s1, s2, s3];
  const cur = steps.findIndex((s) => !s.done);
  const rope = cur === 2 || (cur < 0 && !explored.v);
  return (
    <>
      <Stage
        h={260}
        description={t.pick(L('Pravoúhlý trojúhelník se čtverci nad stranami', 'Прямоугольный треугольник с квадратами на сторонах'))}
        draw={(b, c) => {
          if (rope) {
            const u = 24;
            const ax = 50;
            const ay = 200;
            const bx = ax + 12 * u;
            const by = ay - 5 * u;
            b.line(20, ay, 380, ay, c.ink, 1.2);
            b.rect(bx, by - 20, 10, ay - by + 20, c.beam, c.ink, 1.2);
            b.line(ax, ay, bx, ay, c.derived, 1.0, true);
            b.line(bx, ay, bx, by, c.derived, 1.0, true);
            b.rightAngleMark(pt(bx, ay), pt(ax, ay), pt(bx, by));
            b.line(ax, ay, bx, by, c.load, 2.4);
            b.circle(ax, ay, 5, c.ink);
            b.circle(bx, by, 5, c.ink);
            b.dimLine(ax, ay + 18, bx, ay + 18, t.pick(L('12 m', '12 м')), 14, c.derived);
            b.text(bx - 10, (ay + by) / 2 + 5, t.pick(L('5 m', '5 м')), c.derived, 14, 'end', null, true);
            b.text((ax + bx) / 2 - 10, (ay + by) / 2 - 14, cur < 0 ? t.pick(L('c = 13 m', 'c = 13 м')) : 'c = ?', c.load, 15, 'end', null, true);
            return;
          }
          const la = cur < 0 ? a.v : 3;
          const lb = cur < 0 ? bb.v : 4;
          const show = cur < 0 ? 4 : phase.v;
          const c2 = la * la + lb * lb;
          const cLen = Math.sqrt(c2);
          const r = Math.round(cLen);
          const cInt = r * r === c2 ? r : null;
          const box = 236;
          const wU = la + 2 * lb;
          const hU = 2 * la + lb;
          const u = Math.min(box / wU, box / hU, 26);
          const ax = 12 + (box - wU * u) / 2 + lb * u;
          const ay = 12 + (box - hU * u) / 2 + (la + lb) * u;
          const pa = pt(ax, ay);
          const pc = pt(ax + la * u, ay);
          const pb = pt(ax + la * u, ay - lb * u);
          areaSquare(b, pa, pc, 0, 1, la, c.derived, show === 1 || show >= 3 ? 1 : 0.55);
          areaSquare(b, pc, pb, 1, 0, lb, c.derived, show >= 2 ? 1 : 0.55);
          const nx = -(pb.y - pa.y) / (cLen * u);
          const ny = (pb.x - pa.x) / (cLen * u);
          areaSquare(b, pa, pb, -nx, -ny, show >= 3 ? (cInt ?? 0) : 0, c.load, show >= 3 ? 1 : 0.5);
          b.rightTriangle({ a: pa, b: pb, c: pc }, {}, null, null);
          b.text((pa.x + pc.x) / 2, ay + (la * u) / 2 + 5, `a² = ${la * la}`, c.derived, 13, 'middle', null, true);
          b.text(pc.x + lb * u + 6, (pc.y + pb.y) / 2 + 5, `b² = ${lb * lb}`, c.derived, 13, 'start', null, true);
          b.text((pa.x + pb.x) / 2 - 8, (pa.y + pb.y) / 2 - 4, 'c', c.load, 15, 'end', null, true);
          b.text(392, 40, `a = ${la}, b = ${lb}`, c.ink, 14, 'end');
          if (show >= 3) b.text(392, 64, `c² = ${la * la} + ${lb * lb} = ${c2}`, c.ink, 14, 'end');
          b.text(392, 88, show < 4 ? 'c = ?' : cInt !== null ? `c = ${cInt}` : `c = √${c2} ≈ ${num(cLen, 2)}`, c.load, 15, 'end', null, true);
        }}
      />
      <CardText text={L('V pravoúhlém trojúhelníku platí **a² + b² = c²**, kde c je přepona. Druhá mocnina délky odpovídá ploše čtverce.', 'В прямоугольном треугольнике **a² + b² = c²**, где c — гипотенуза. Квадрат длины соответствует площади квадрата.')} />
      <SmallText text={L('a² znamená a · a, třeba 3² = 9. Zpátky vede odmocnina: √25 = 5, protože 5 · 5 = 25.', 'a² означает a · a, например 3² = 9. Обратно ведёт корень: √25 = 5, потому что 5 · 5 = 25.')} />
      <Steps
        steps={steps}
        after={() => (
          <>
            <ValueSlider
              label={t.pick(L(`Odvěsna a = ${a.v}`, `Катет a = ${a.v}`))}
              value={a.v}
              min={1}
              max={8}
              steps={6}
              onChange={(v) => {
                a.v = Math.round(v);
                explored.v = true;
              }}
            />
            <ValueSlider
              label={t.pick(L(`Odvěsna b = ${bb.v}`, `Катет b = ${bb.v}`))}
              value={bb.v}
              min={1}
              max={6}
              steps={4}
              onChange={(v) => {
                bb.v = Math.round(v);
                explored.v = true;
              }}
            />
          </>
        )}
      />
    </>
  );
}

function rect2(b: PrimBuilder, x: number, y: number, w: number, h: number, color: string, a: number) {
  if (w > 0.2) b.rect(x, y, w, h, color, null, 1.5, a);
}

/** Karta 1: přepona 1 → sin² α + cos² α = 1. */
function UnitHyp() {
  const t = useTr();
  const kit = useKit();
  const guess = kit.mut<number | null>(null);
  const alpha = kit.mut(45);
  const tested = kit.mut(false);
  const s1 = usePredictStep(
    L('Odhadni: změní se sin² α + cos² α, když změníš úhel?', 'Предскажи: изменится ли sin² α + cos² α при смене угла?'),
    [L('Změní se', 'Изменится'), L('Nezmění se', 'Не изменится')],
    guess.v,
    (i) => (guess.v = i),
  );
  const far = Math.abs(alpha.v - 45) >= 15;
  const s2 = step(
    L('Změň úhel aspoň o 15° a sleduj součet sin² α + cos² α', 'Измени угол хотя бы на 15° и следи за суммой sin² α + cos² α'),
    tested.v,
    () => {
      const s = Trig.sinD(alpha.v);
      const co = Trig.cosD(alpha.v);
      return (
        <>
          <ValueSlider label={t.pick(L(`Úhel α = ${alpha.v}°`, `Угол α = ${alpha.v}°`))} value={alpha.v} min={10} max={80} steps={13} onChange={(v) => (alpha.v = Math.round(v / 5) * 5)} />
          <Readout text={`sin² ${alpha.v}° + cos² ${alpha.v}° = ${num(s * s, 2)} + ${num(co * co, 2)} = ${num(s * s + co * co, 2)}`} />
          <ActionButton text={L('Ověřit', 'Проверить')} enabled={far} onClick={() => (tested.v = true)} />
          {!far && <SmallText text={L('Posuň úhel aspoň o 15° od 45°.', 'Сдвинь угол хотя бы на 15° от 45°.')} />}
        </>
      );
    },
    () => (
      <Note
        tone={guess.v === 1 ? 'ok' : 'info'}
        text={L(
          (guess.v === 1 ? 'Tip sedí. ' : 'Součet se nezměnil. ') + 'Jedna část pruhu roste, druhá stejně ubývá – sin² α + cos² α = 1 pro každý úhel.',
          (guess.v === 1 ? 'Прогноз верен. ' : 'Сумма не изменилась. ') + 'Одна часть полосы растёт, другая настолько же убывает — sin² α + cos² α = 1 для любого угла.',
        )}
      />
    ),
  );
  const s3 = useQuizStep(
    L('sin α = 0,6 a α je ostrý úhel. Kolik je cos α?', 'sin α = 0,6, α — острый угол. Чему равен cos α?'),
    [
      opt(L('0,8', '0,8'), true),
      opt(L('0,4', '0,4'), false, L('1 − 0,6 by platilo, kdyby se sčítaly sin a cos. Sčítají se ale jejich **druhé mocniny**.', '1 − 0,6 было бы верно, если бы складывались sin и cos. Но складываются их **квадраты**.')),
      opt(L('0,64', '0,64'), false, L('0,64 = 1 − 0,6² je teprve cos² α. Co ještě zbývá?', '0,64 = 1 − 0,6² — это пока cos² α. Что ещё осталось?')),
      opt(L('1,6', '1,6'), false, L('Odvěsna nemůže být delší než přepona 1 – kosinus ostrého úhlu je menší než 1.', 'Катет не может быть длиннее гипотенузы 1 — косинус острого угла меньше 1.')),
    ],
    L(
      'cos² α = 1 − 0,6² = 0,64. Úhel α je ostrý, takže cos α je kladný: cos α = 0,8. Je to trojúhelník 3-4-5 zmenšený na přeponu 1.',
      'cos² α = 1 − 0,6² = 0,64. Угол α острый, поэтому cos α положителен: cos α = 0,8. Это треугольник 3-4-5, уменьшенный до гипотенузы 1.',
    ),
    null,
    2,
    L('Urči cos α, když sin α = 0,6 a α je ostrý', 'Найди cos α, если sin α = 0,6 и α — острый угол'),
  );
  const steps = [s1, s2, s3];
  const cur = steps.findIndex((s) => !s.done);
  const quiz = cur === 2 || cur < 0;
  const ad = quiz ? Trig.asinD(0.6) : alpha.v;
  return (
    <>
      <Stage
        h={250}
        description={t.pick(L('Trojúhelník s přeponou 1 a pruh sin² + cos²', 'Треугольник с гипотенузой 1 и полоса sin² + cos²'))}
        draw={(b, c) => {
          const s = Trig.sinD(ad);
          const co = Trig.cosD(ad);
          const r = 190;
          const ax = 60;
          const ay = 160;
          const pa = pt(ax, ay);
          const pc = pt(ax + r * co, ay);
          const pb = pt(ax + r * co, ay - r * s);
          b.rightTriangle({ a: pa, b: pb, c: pc }, { hyp: '1' }, null, quiz ? 'α' : `${Math.round(ad)}°`);
          if (quiz) {
            b.text((pa.x + pc.x) / 2, ay + 18, cur < 0 ? 'cos α = 0,8' : 'cos α = ?', c.derived, 13, 'middle', null, true);
            b.text(pc.x + 8, (pc.y + pb.y) / 2 + 5, 'sin α = 0,6', c.derived, 13, 'start', null, true);
            return;
          }
          b.text((pa.x + pc.x) / 2, ay + 18, `cos α = ${num(co, 3)}`, c.derived, 13);
          b.text(pc.x + 8, (pc.y + pb.y) / 2 + 5, `sin α = ${num(s, 3)}`, c.derived, 13, 'start');
          if (cur === 0) return; // při tipu pruh nenapovídá
          const bx = 40;
          const by = 205;
          const bw = 320;
          rect2(b, bx, by, bw * s * s, 22, c.derived, 0.9);
          rect2(b, bx + bw * s * s, by, bw * co * co, 22, c.derived, 0.4);
          b.line(bx, by, bx + bw, by, c.ink, 1.0);
          b.line(bx, by + 22, bx + bw, by + 22, c.ink, 1.0);
          b.line(bx, by, bx, by + 22, c.ink, 1.0);
          b.line(bx + bw, by, bx + bw, by + 22, c.ink, 1.0);
          if (s * s > 0.12) b.text(bx + (bw * s * s) / 2, by + 16, 'sin²', c.paper, 12, 'middle', null, true);
          if (co * co > 0.12) b.text(bx + bw * s * s + (bw * co * co) / 2, by + 16, 'cos²', c.ink, 12, 'middle', null, true);
          b.text(bx + bw + 6, by + 16, '= 1', c.ink, 13, 'start', null, true);
        }}
      />
      <CardText text={L('Při přeponě 1 mají odvěsny délky sin α a cos α. Pythagorova věta proto spojuje jejich druhé mocniny.', 'При гипотенузе 1 длины катетов равны sin α и cos α. Поэтому теорема Пифагора связывает их квадраты.')} />
      <SmallText text={L('sin² α znamená (sin α)² – nejdřív sinus, pak na druhou. Není to sin (α²).', 'sin² α означает (sin α)² — сначала синус, потом в квадрат. Это не sin (α²).')} />
      <Steps steps={steps} />
    </>
  );
}

/** Karta 2: tangens = sklon – převýšení h na vodorovnou délku d = 1 m. Obě osy ve stejném měřítku. */
function TangentSlope() {
  const t = useTr();
  const kit = useKit();
  const alpha = kit.mut(20);
  const at45 = kit.mut(false);
  const miss1 = kit.mut<L | null>(null);
  const guess = kit.mut<number | null>(null);
  const steep = kit.mut(false);
  const miss3 = kit.mut<L | null>(null);
  const tg = Trig.tgD(alpha.v);
  const hText = t.pick(L(`h = ${num(tg, 2)} m`, `h = ${num(tg, 2)} м`));
  const s1 = step(
    L('Najdi úhel, při kterém stoupneš o 1 m na 1 m vodorovně', 'Найди угол, при котором подъём 1 м на 1 м по горизонтали'),
    at45.v,
    () => (
      <>
        <ValueSlider
          label={t.pick(L(`Úhel α = ${alpha.v}°`, `Угол α = ${alpha.v}°`))}
          value={alpha.v}
          min={0}
          max={80}
          onChange={(v) => {
            alpha.v = Math.round(v);
            miss1.v = null;
          }}
        />
        <Readout text={hText} />
        <ActionButton
          text={L('Zkontrolovat', 'Проверить')}
          onClick={() => {
            if (alpha.v === 45) at45.v = true;
            else
              miss1.v =
                tg < 1
                  ? L(`h = ${num(tg, 2)} m – ještě méně než 1 m. Zvětši úhel.`, `h = ${num(tg, 2)} м — пока меньше 1 м. Увеличь угол.`)
                  : L(`h = ${num(tg, 2)} m – už víc než 1 m. Zmenši úhel.`, `h = ${num(tg, 2)} м — уже больше 1 м. Уменьши угол.`);
          }}
        />
        {miss1.v && <Note tone="nudge" text={miss1.v} />}
      </>
    ),
    () => <Note tone="ok" text={L('Při 45° je h = d, tedy tg 45° = 1 m / 1 m = 1.', 'При 45° h = d, значит tg 45° = 1 м / 1 м = 1.')} />,
  );
  const s2 = usePredictStep(L('Odhadni: může být tg α větší než 5?', 'Предскажи: может ли tg α быть больше 5?'), [L('Ano', 'Да'), L('Ne, nejvýš 1', 'Нет, не больше 1')], guess.v, (i) => (guess.v = i));
  const s3 = step(
    L('Najdi úhel, při kterém je tg α větší než 5', 'Найди угол, при котором tg α больше 5'),
    steep.v,
    () => (
      <>
        <ValueSlider
          label={t.pick(L(`Úhel α = ${alpha.v}°`, `Угол α = ${alpha.v}°`))}
          value={Math.min(85, Math.max(45, alpha.v))}
          min={45}
          max={85}
          onChange={(v) => {
            alpha.v = Math.round(v);
            miss3.v = null;
          }}
        />
        <Readout text={`${hText} · tg α = ${num(tg, 2)}`} />
        <ActionButton
          text={L('Zkontrolovat', 'Проверить')}
          onClick={() => {
            if (tg > 5) steep.v = true;
            else miss3.v = L(`tg α = ${num(tg, 2)} – ještě to nestačí. Strměji!`, `tg α = ${num(tg, 2)} — пока мало. Круче!`);
          }}
        />
        {miss3.v && <Note tone="nudge" text={miss3.v} />}
      </>
    ),
    () => (
      <Note
        tone={guess.v === 0 ? 'ok' : 'info'}
        text={L(
          (guess.v === 0 ? 'Tip sedí. ' : 'Na rozdíl od sinu a kosinu tangens 1 překročit může. ') +
            `Při ${alpha.v}° je tg α ≈ ${num(tg, 2)}. Když se úhel blíží 90°, d zůstává 1 m a h roste nade všechny meze. tg 90° není definován.`,
          (guess.v === 0 ? 'Прогноз верен. ' : 'В отличие от синуса и косинуса, тангенс может быть больше 1. ') +
            `При ${alpha.v}° tg α ≈ ${num(tg, 2)}. Когда угол приближается к 90°, d остаётся 1 м, а h растёт неограниченно. tg 90° не определён.`,
        )}
      />
    ),
  );
  return (
    <>
      <Stage
        h={250}
        description={t.pick(L('Sklon: převýšení h na vodorovné délce 1 m', 'Уклон: подъём h на горизонтальной длине 1 м'))}
        draw={(b, c) => {
          const ax = 70;
          const ay = 215;
          const top = 22;
          const unit = Math.min(110, (ay - top) / Math.max(tg, 1e-9));
          const hx = ax + unit;
          const rise = tg * unit;
          b.line(20, ay, 380, ay, c.ink, 1.2);
          b.poly([[ax, ay], [hx, ay], [hx, ay - rise]], c.subtle, null, 1.5, 0.6);
          b.line(ax, ay, hx, ay, c.derived, 2.4);
          b.line(hx, ay, hx, ay - rise, c.derived, 2.4);
          b.line(ax, ay, hx, ay - rise, c.load, 2.4);
          if (rise > 12) b.rightAngleMark(pt(hx, ay), pt(ax, ay), pt(hx, ay - rise));
          if (alpha.v > 0) b.angleArc(ax, ay, 26, 0, -alpha.v, `${alpha.v}°`);
          b.text((ax + hx) / 2, ay + 20, t.pick(L('d = 1 m', 'd = 1 м')), c.derived, 13, 'middle', null, true);
          b.text(hx + 10, ay - rise / 2 + 5, hText, c.derived, 14, 'start', null, true);
          b.text(392, 36, `tg α = h / d = ${num(tg, 2)}`, c.ink, 15, 'end', null, true);
          if (unit < 109) b.text(392, 58, t.pick(L('měřítko zmenšeno', 'масштаб уменьшен')), c.inkSoft, 12, 'end');
        }}
      />
      <CardText
        text={L(
          'Sklon je poměr převýšení h k vodorovné délce d: **tg α = h / d** (protilehlá ku přilehlé). Je to číslo bez jednotky.',
          'Уклон — отношение подъёма h к горизонтальной длине d: **tg α = h / d** (противолежащий к прилежащему). Это число без единиц.',
        )}
      />
      <SmallText text={L('Také platí tg α = sin α / cos α.', 'Также tg α = sin α / cos α.')} />
      <Steps steps={[s1, s2, s3]} />
    </>
  );
}

/** Karta 3: doplňkový úhel – sin α = cos (90° − α). */
function Complement() {
  const t = useTr();
  const kit = useKit();
  const phase = kit.mut(0); // ukázka: 0 z α, 1 z β, 2 obojí
  const demoDone = kit.mut(false);
  const s1 = useQuizStep(
    L('Odvěsna a leží naproti úhlu α. Jakou roli má vůči úhlu β?', 'Катет a лежит напротив угла α. Какова его роль относительно угла β?'),
    [
      opt(L('Přilehlá', 'Прилежащий'), true),
      opt(L('Protilehlá', 'Противолежащий'), false, L('Naproti β leží jiná odvěsna – ta vodorovná. Dotýká se a vrcholu s úhlem β?', 'Напротив β лежит другой катет — горизонтальный. Касается ли a вершины с углом β?')),
      opt(L('Přepona', 'Гипотенуза'), false, L('Přepona leží naproti pravému úhlu a je nejdelší. Tady je to c.', 'Гипотенуза лежит напротив прямого угла и она самая длинная. Здесь это c.')),
    ],
    L('Odvěsna a se dotýká vrcholu β, je k němu přilehlá. Vůči α je protilehlá.', 'Катет a касается вершины β — он прилежащий к β. Относительно α он противолежащий.'),
    null,
    3,
    L('Urči roli zvýrazněné odvěsny a vůči úhlu β', 'Определи роль выделенного катета a относительно угла β'),
  );
  useLaunched([s1.done], async (job) => {
    if (!s1.done) return;
    await job.delay(700);
    phase.v = 1;
    await job.delay(1400);
    phase.v = 2;
    await job.delay(900);
    demoDone.v = true;
  });
  const s2 = step(
    L('Sleduj stejný poměr a / c z obou úhlů', 'Посмотри на одно отношение a / c с обоих углов'),
    demoDone.v,
    () => <SmallText text={L('Sleduj obrázek…', 'Смотри на рисунок…')} />,
    () => (
      <Note
        tone="ok"
        text={L(
          'a / c je pro α sinus a pro β kosinus: **sin α = cos β**. Protože β = 90° − α, platí sin α = cos (90° − α).',
          'a / c для α — синус, а для β — косинус: **sin α = cos β**. Так как β = 90° − α, sin α = cos (90° − α).',
        )}
      />
    ),
  );
  const s3 = useQuizStep(
    L('sin 20° je stejné číslo jako…', 'sin 20° — то же число, что и…'),
    [
      opt(L('cos 70°', 'cos 70°'), true),
      opt(L('cos 20°', 'cos 20°'), false, L('sin a cos téhož úhlu se rovnají jen při 45°. Jak velký je druhý ostrý úhel?', 'sin и cos одного угла равны только при 45°. Чему равен второй острый угол?')),
      opt(L('sin 70°', 'sin 70°'), false, L('To je sinus druhého úhlu – patří k druhé odvěsně. Která funkce úhlu 70° bere odvěsnu naproti 20°?', 'Это синус второго угла — он относится к другому катету. Какая функция угла 70° берёт катет напротив 20°?')),
      opt(L('1 − sin 70°', '1 − sin 70°'), false, L('Do jedné se sčítají druhé mocniny sin a cos téhož úhlu, ne siny dvou úhlů.', 'До единицы складываются квадраты sin и cos одного угла, а не синусы двух углов.')),
    ],
    L(
      'β = 90° − 20° = 70° a odvěsna naproti 20° je přilehlá k 70°. Proto sin 20° = cos 70°. Ve statice pozor: je-li úhel síly zadán od **svislice**, sin a cos si vymění místa.',
      'β = 90° − 20° = 70°, и катет напротив 20° прилежит к 70°. Поэтому sin 20° = cos 70°. В статике осторожно: если угол силы задан от **вертикали**, sin и cos меняются местами.',
    ),
    null,
    2,
    L('Vyber výraz rovný sin 20°', 'Выбери выражение, равное sin 20°'),
  );
  const steps = [s1, s2, s3];
  const cur = steps.findIndex((s) => !s.done);
  const last = cur === 2 || cur < 0;
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Trojúhelník s úhly α a β', 'Треугольник с углами α и β'))}
        draw={(b, c: Colors) => {
          const g = last ? triGeom(Trig.cosD(20), Trig.sinD(20), { fit: [30, 90, 230, 110] }) : triGeom(4, 3, { fit: [30, 40, 190, 165] });
          const aName = last ? '20°' : 'α';
          const bName = last ? '70°' : 'β';
          b.rightTriangle(g, { opp: 'a', hyp: 'c' }, 'opp', null);
          const fromA = cur === 1 && phase.v === 0;
          const fromB = cur === 0 || (cur === 1 && phase.v === 1);
          const both = (cur === 1 && phase.v === 2) || last;
          b.angleArc(g.a.x, g.a.y, 28, screenAngle(g.a, g.c), screenAngle(g.a, g.b), aName, fromA || both ? c.ink : c.inkSoft);
          b.angleArc(g.b.x, g.b.y, 26, screenAngle(g.b, g.a), screenAngle(g.b, g.c), bName, fromB || both ? c.ink : c.inkSoft);
          const lines =
            cur === 0
              ? [t.pick(L('a je protilehlá k α', 'a — противолежащий к α')), t.pick(L('vůči β je a …?', 'для β катет a …?'))]
              : cur === 1 && phase.v === 0
                ? [t.pick(L('z úhlu α:', 'с угла α:')), 'sin α = a / c']
                : cur === 1 && phase.v === 1
                  ? [t.pick(L('z úhlu β:', 'с угла β:')), 'cos β = a / c']
                  : cur === 1
                    ? ['sin α = cos β', 'β = 90° − α']
                    : ['sin 20° = a / c', 'cos 70° = a / c'];
          lines.forEach((s, i) => b.text(392, 30 + 24 * i, s, i === 0 ? c.highlight : c.ink, 14, 'end', null, true));
        }}
      />
      <CardText text={L('Dva ostré úhly pravoúhlého trojúhelníku dávají dohromady 90°. Stejná odvěsna má vůči každému z nich jinou roli.', 'Два острых угла прямоугольного треугольника в сумме дают 90°. Один и тот же катет играет для них разные роли.')} />
      <Steps steps={steps} />
    </>
  );
}

export const cards = [Pythagoras, UnitHyp, TangentSlope, Complement];
