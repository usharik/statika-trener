/*
 * Kurz trigonometrie – lekce 1 (id 101): odkud se vzal sinus. Podobné trojúhelníky → podíl stran závisí jen na úhlu;
 * jména stran pravoúhlého trojúhelníku. (Přepis z aplikace: learn/trig/TrigLesson1.kt.)
 */
import { CardText, L, Note, Readout, Stage, Steps, ValueSlider, ActionButton, clamp, dist, num, opt, pt, step, useKit, useQuizStep, useTr, type Pt } from '../../kit';
import { Trig } from '../../../practice/trig';
import { BTN_CHECK, BTN_TEST, PickCheck, Ramp, rampTri, useGuessStep } from './shared';

/** 1 m rampy v jednotkách scény (karta 0). */
const RAMP_PX = 50;

/** Karta 0: podobné trojúhelníky – podíl výška / šikmá délka nezávisí na velikosti. */
function SameAngle() {
  const t = useTr();
  const kit = useKit();
  const len = kit.mut(2); // šikmá délka v m
  const guess = kit.mut<number | null>(null);
  const tested = kit.mut<number | null>(null); // délka, při které žák ověřil
  const msg = kit.mut<L | null>(null);
  const dragging = kit.mut(false);
  const guesses = [L('1 m', '1 м'), L('2 m', '2 м'), L('4 m', '4 м')];
  const setLen = (v: number) => {
    if (guess.v === null || tested.v !== null) return;
    len.v = clamp(Math.round(v), 2, 6);
    msg.v = null;
  };
  const r = new Ramp(40, 205, RAMP_PX * len.v, 30);
  const h = len.v / 2;
  const g = useGuessStep(L('Tipni si: jak vysoko vystoupá rampa dlouhá 4 m?', 'Предскажи: на какую высоту поднимется пандус длиной 4 м?'), guesses, guess.v, (i) => (guess.v = i));
  const q = useQuizStep(
    L('Co zůstalo po prodloužení rampy stejné?', 'Что сохранилось после удлинения пандуса?'),
    [
      opt(L('Výška h', 'Высота h'), false, L('Výška se zvětšila spolu s délkou. Co se nezměnilo?', 'Высота выросла вместе с длиной. Что не изменилось?')),
      opt(L('Délka L', 'Длина L'), false, L('Délku jsi právě měnil(a). Podívej se na podíl h / L.', 'Длину ты только что менял(а). Посмотри на отношение h / L.')),
      opt(L('Podíl h / L', 'Отношение h / L'), true),
    ],
    L(
      'Při dvojnásobné délce je dvojnásobná i výška: 1/2 = 2/4 = 0,5. Trojúhelníky se stejnými úhly jsou **podobné** – odpovídající strany mají stejný poměr.',
      'При удвоении длины высота тоже удваивается: 1/2 = 2/4 = 0,5. Треугольники с одинаковыми углами **подобны** — отношения соответствующих сторон у них одинаковы.',
    ),
    null,
    3,
  );
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Nájezdová rampa – pravoúhlý trojúhelník s úhlem 30°', 'Пандус — прямоугольный треугольник с углом 30°'))}
        onDrag={(p, start) => {
          if (!start || dist(p, r.b.x, r.b.y) < 50) {
            dragging.v = true;
            // průmět prstu na směr přepony
            const along = (p.x - 40) * Trig.cosD(30) - (p.y - 205) * Trig.sinD(30);
            setLen(along / RAMP_PX);
          }
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b, c) => {
          b.line(10, 205, 390, 205, c.ink, 1.4);
          if (len.v > 2) rampTri(b, new Ramp(40, 205, RAMP_PX * 2, 30), c, null, 0.3);
          if (guess.v === null) {
            // tip: rampa dlouhá 4 m čárkovaně, výška neznámá
            const far = new Ramp(40, 205, RAMP_PX * 4, 30);
            b.line(r.b.x, r.b.y, far.b.x, far.b.y, c.load, 1.6, true);
            b.line(far.c.x, far.c.y, far.b.x, far.b.y, c.derived, 1.4, true);
            b.text(far.c.x + 8, (far.c.y + far.b.y) / 2 + 5, t.pick(L('h = ?', 'h = ?')), c.derived, 14, 'start', null, true);
            b.text(far.b.x - 10, far.b.y - 8, t.pick(L('L = 4 m', 'L = 4 м')), c.load, 13, 'end');
          }
          rampTri(b, r, c, '30°', 1, true, t.pick(L(`L = ${len.v} m`, `L = ${len.v} м`)), t.pick(L(`h = ${num(h)} m`, `h = ${num(h)} м`)));
          if (guess.v !== null && tested.v === null) b.handle(r.b.x, r.b.y, dragging.v);
        }}
      />
      <CardText
        text={L(
          'Dvě rampy se stejným sklonem mají stejný tvar, ale můžou mít různou velikost. Tahle stoupá pod úhlem 30°: je dlouhá L = 2 m a vystoupá do výšky h = 1 m.',
          'Два пандуса с одинаковым углом наклона имеют одинаковую форму, но могут различаться по размеру. Этот поднимается под углом 30°: его длина L = 2 м, высота h = 1 м.',
        )}
      />
      <Steps
        steps={[
          g,
          step(
            L('Prodluž rampu aspoň na 4 m a ověř svůj tip', 'Удлини пандус хотя бы до 4 м и проверь прогноз'),
            tested.v !== null,
            () => (
              <>
                <ValueSlider label={t.pick(L(`Délka rampy L = ${len.v} m`, `Длина пандуса L = ${len.v} м`))} value={len.v} min={2} max={6} steps={3} onChange={setLen} />
                <Readout text={`h / L = ${num(h)} / ${len.v} = 0,5`} />
                <ActionButton
                  text={BTN_TEST}
                  onClick={() => {
                    if (len.v >= 4) tested.v = len.v;
                    else msg.v = L(`Rampa měří jen ${len.v} m – prodluž ji aspoň na 4 m.`, `Пандус пока всего ${len.v} м — удлини его хотя бы до 4 м.`);
                  }}
                />
                {msg.v && <Note tone="nudge" text={msg.v} />}
              </>
            ),
            () => {
              const l = tested.v ?? 4;
              const gi = guess.v ?? 0;
              return (
                <Note
                  tone={guess.v === 1 ? 'ok' : 'info'}
                  text={L(
                    (guess.v === 1 ? 'Tip sedí. ' : `Tip ${guesses[gi].cs} nesedí. `) + `Dvakrát delší rampa je i dvakrát vyšší: L = 4 m → h = 2 m. Při L = ${l} m je h = ${num(l / 2)} m a pořád h / L = 0,5.`,
                    (guess.v === 1 ? 'Прогноз верен. ' : `Прогноз ${guesses[gi].ru} не подтвердился. `) +
                      `Вдвое более длинный пандус и вдвое выше: L = 4 м → h = 2 м. При L = ${l} м высота h = ${num(l / 2)} м, и всё так же h / L = 0,5.`,
                  )}
                />
              );
            },
          ),
          q,
        ]}
      />
    </>
  );
}

/** 1 m rampy v jednotkách scény (karta 1). */
const RAMP2_PX = 36;

/** Karta 1: podíl závisí jen na úhlu – to je sin α. Nejdřív úhel (délka stojí), pak délka (úhel stojí). */
function AngleSetsRatio() {
  const t = useTr();
  const kit = useKit();
  const alpha = kit.mut(30);
  const len = kit.mut(4);
  const guess = kit.mut<number | null>(null);
  const at60 = kit.mut(false);
  const lenChanged = kit.mut(false);
  const sel = kit.mut<number | null>(null);
  const answered = kit.mut(false);
  const msg = kit.mut<L | null>(null);
  const dragging = kit.mut(false);
  const changes = [L('Vzroste', 'Увеличится'), L('Klesne', 'Уменьшится'), L('Nezmění se', 'Не изменится')];
  const setAlpha = (v: number) => {
    if (guess.v === null || at60.v) return;
    alpha.v = clamp(Math.round(v / 5) * 5, 30, 60);
    msg.v = null;
  };
  const setLen = (v: number) => {
    if (!at60.v || answered.v) return;
    const n = clamp(Math.round(v), 2, 6);
    if (n !== len.v) lenChanged.v = true;
    len.v = n;
    msg.v = null;
  };
  const r = new Ramp(50, 215, RAMP2_PX * len.v, alpha.v);
  const ratio = Trig.sinD(alpha.v);
  const h = len.v * ratio;
  const g = useGuessStep(L('Tipni si: jak se změní h / L, když úhel zvětšíš z 30° na 60°?', 'Предскажи: как изменится h / L, если увеличить угол с 30° до 60°?'), changes, guess.v, (i) => (guess.v = i));
  return (
    <>
      <Stage
        h={235}
        description={t.pick(L('Rampa s nastavitelným úhlem a délkou', 'Пандус с изменяемым углом и длиной'))}
        onDrag={(p, start) => {
          if (!start || dist(p, r.b.x, r.b.y) < 50) {
            dragging.v = true;
            setLen(Math.hypot(p.x - 50, p.y - 215) / RAMP2_PX);
          }
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b, c) => {
          b.line(10, 215, 390, 215, c.ink, 1.4);
          rampTri(b, r, c, `${Math.round(alpha.v)}°`, 1, true, t.pick(L(`L = ${len.v} m`, `L = ${len.v} м`)), t.pick(L(`h = ${num(h)} m`, `h = ${num(h)} м`)));
          if (at60.v && !answered.v) b.handle(r.b.x, r.b.y, dragging.v);
          b.text(388, 26, `h / L = ${num(ratio, 3)}`, c.ink, 15, 'end', null, true);
        }}
      />
      <CardText
        text={L(
          'Podíl výšky rampy a její délky po svahu se jmenuje **sinus** úhlu: sin α = h / L. Na čem závisí?',
          'Отношение высоты пандуса к его длине по склону называется **синусом** угла: sin α = h / L. От чего оно зависит?',
        )}
      />
      <Steps
        steps={[
          g,
          step(
            L('Zvětši úhel na 60° (délka zůstává 4 m) a ověř tip', 'Увеличь угол до 60° (длина остаётся 4 м) и проверь прогноз'),
            at60.v,
            () => (
              <>
                <ValueSlider label={t.pick(L(`Úhel α = ${Math.round(alpha.v)}°`, `Угол α = ${Math.round(alpha.v)}°`))} value={alpha.v} min={30} max={60} steps={5} onChange={setAlpha} />
                <ActionButton
                  text={BTN_TEST}
                  onClick={() => {
                    if (alpha.v === 60) at60.v = true;
                    else msg.v = L(`Úhel je zatím ${Math.round(alpha.v)}° – zvětši ho na 60°.`, `Угол пока ${Math.round(alpha.v)}° — увеличь его до 60°.`);
                  }}
                />
                {msg.v && <Note tone="nudge" text={msg.v} />}
              </>
            ),
            () => (
              <Note
                tone={guess.v === 0 ? 'ok' : 'info'}
                text={L(
                  (guess.v === 0 ? 'Tip sedí: ' : 'Je to jinak: ') + 'strmější rampa je při stejné délce vyšší, h / L vzrostl z 0,5 na 0,866.',
                  (guess.v === 0 ? 'Прогноз верен: ' : 'На самом деле иначе: ') + 'при той же длине более крутой пандус выше, h / L вырос с 0,5 до 0,866.',
                )}
              />
            ),
          ),
          step(
            L('Změň délku rampy (úhel zůstává 60°) – co udělá h / L?', 'Измени длину пандуса (угол остаётся 60°) — что будет с h / L?'),
            answered.v,
            () => (
              <>
                <ValueSlider label={t.pick(L(`Délka rampy L = ${len.v} m`, `Длина пандуса L = ${len.v} м`))} value={len.v} min={2} max={6} steps={3} onChange={setLen} />
                <PickCheck
                  options={changes}
                  selected={sel.v}
                  onSelect={(i) => {
                    sel.v = i;
                    msg.v = null;
                  }}
                  button={BTN_CHECK}
                  msg={msg.v}
                  onCheck={() => {
                    if (!lenChanged.v) msg.v = L('Nejdřív posuvníkem změň délku rampy.', 'Сначала измени длину пандуса ползунком.');
                    else if (sel.v === 2) {
                      answered.v = true;
                      msg.v = null;
                    } else msg.v = L('Podívej se na h / L v obrázku – změnil se při jiné délce?', 'Посмотри на h / L на рисунке — изменилось ли оно при другой длине?');
                  }}
                />
              </>
            ),
            () => (
              <Note
                tone="ok"
                text={L(
                  'Sinus závisí na úhlu, ne na velikosti trojúhelníku: sin 30° = 0,5 a sin 60° ≈ 0,866 v každém pravoúhlém trojúhelníku.',
                  'Синус зависит от угла, а не от размера треугольника: sin 30° = 0,5 и sin 60° ≈ 0,866 в любом прямоугольном треугольнике.',
                )}
              />
            ),
          ),
        ]}
      />
    </>
  );
}

/** Trojúhelník karet o jménech stran: α u A vlevo dole, pravý úhel u C vpravo dole, β u B nahoře. */
const NAME_A = pt(80, 190);
const NAME_C = pt(300, 190);
const NAME_B = pt(300, 50);
const mid = (p: Pt, q: Pt) => pt((p.x + q.x) / 2, (p.y + q.y) / 2);

/** Karta 2: jména stran; přilehlá / protilehlá podle toho, ze kterého úhlu se díváme. */
function NameSides() {
  const t = useTr();
  const kit = useKit();
  const hypOk = kit.mut(false);
  const adjOk = kit.mut(false);
  const betaOk = kit.mut(false);
  const sel = kit.mut<number | null>(null); // 0 AB, 1 AC, 2 BC
  const msg = kit.mut<L | null>(null);
  const names = [L('AB', 'AB'), L('AC', 'AC'), L('BC', 'BC')];
  const ends: [Pt, Pt][] = [
    [NAME_A, NAME_B],
    [NAME_A, NAME_C],
    [NAME_C, NAME_B],
  ];
  const beta = adjOk.v; // od 3. kroku se díváme z úhlu β
  const hypMsg = L('AB je přepona – leží naproti pravému úhlu u C. Hledáme **odvěsnu**.', 'AB — гипотенуза, она лежит напротив прямого угла C. Нужен **катет**.');
  const check = () => {
    const s = sel.v;
    if (s === null) return;
    let m: L | null = null;
    if (!hypOk.v) {
      if (s === 0) hypOk.v = true;
      else m = L('Tahle strana vychází z pravého úhlu u C – je to odvěsna. Která strana leží pravému úhlu naproti?', 'Эта сторона выходит из прямого угла C — это катет. Какая сторона лежит напротив прямого угла?');
    } else if (!adjOk.v) {
      if (s === 1) adjOk.v = true;
      else if (s === 0) m = hypMsg;
      else m = L('BC se vrcholu A vůbec nedotýká – je úhlu α protilehlá.', 'BC вообще не касается вершины A — она противолежит углу α.');
    } else {
      if (s === 2) betaOk.v = true;
      else if (s === 0) m = hypMsg;
      else m = L('AC se vrcholu B nedotýká – pro úhel β je protilehlá.', 'AC не касается вершины B — для угла β она противолежащая.');
    }
    msg.v = m;
    if (m === null) sel.v = null;
  };
  const pick = () => (
    <PickCheck
      options={names}
      selected={sel.v}
      onSelect={(i) => {
        sel.v = i;
        msg.v = null;
      }}
      button={BTN_CHECK}
      msg={msg.v}
      onCheck={check}
    />
  );
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Pravoúhlý trojúhelník ABC s pravým úhlem u C', 'Прямоугольный треугольник ABC с прямым углом при C'))}
        onTap={(p) => {
          if (betaOk.v) return;
          const spots = ends.map(([a, b]) => mid(a, b));
          let best = -1;
          let bd = Infinity;
          spots.forEach((s, i) => {
            const d = dist(p, s.x, s.y);
            if (d < bd) {
              bd = d;
              best = i;
            }
          });
          if (best >= 0 && bd < 40) {
            sel.v = best;
            msg.v = null;
          }
        }}
        draw={(b, c) => {
          b.poly(
            [
              [NAME_A.x, NAME_A.y],
              [NAME_B.x, NAME_B.y],
              [NAME_C.x, NAME_C.y],
            ],
            c.subtle,
            null,
            0,
            0.6,
          );
          const colors = [hypOk.v ? c.load : c.ink, adjOk.v ? c.derived : c.ink, betaOk.v ? c.derived : c.ink];
          ends.forEach(([p, q], i) => b.line(p.x, p.y, q.x, q.y, sel.v === i ? c.highlight : colors[i], sel.v === i ? 4.5 : 2.6));
          b.rightAngleMark(NAME_C, NAME_A, NAME_B);
          b.angleArc(NAME_A.x, NAME_A.y, 30, 0, (Math.atan2(NAME_B.y - NAME_A.y, NAME_B.x - NAME_A.x) * 180) / Math.PI, 'α', !beta ? c.highlight : c.inkSoft);
          b.angleArc(NAME_B.x, NAME_B.y, 30, (Math.atan2(NAME_A.y - NAME_B.y, NAME_A.x - NAME_B.x) * 180) / Math.PI, 90, 'β', beta ? c.highlight : c.inkSoft);
          b.text(NAME_A.x - 10, NAME_A.y + 18, 'A', c.ink, 15, 'end', null, true);
          b.text(NAME_C.x + 10, NAME_C.y + 18, 'C', c.ink, 15, 'start', null, true);
          b.text(NAME_B.x + 10, NAME_B.y - 4, 'B', c.ink, 15, 'start', null, true);
          if (!betaOk.v) for (const [a, bb] of ends) b.tapSpot(mid(a, bb).x, mid(a, bb).y);
          // jména objevených stran (vzhledem k právě sledovanému úhlu)
          if (hypOk.v) b.text(mid(NAME_A, NAME_B).x - 26, mid(NAME_A, NAME_B).y - 14, t.pick(L('přepona', 'гипотенуза')), c.load, 14, 'end');
          const acName = beta ? L('protilehlá k β', 'противолежащий β') : L('přilehlá k α', 'прилежащий к α');
          if (adjOk.v) b.text(mid(NAME_A, NAME_C).x, NAME_A.y + 26, t.pick(acName), c.derived, 14);
          if (betaOk.v) b.text(NAME_C.x + 20, mid(NAME_C, NAME_B).y + 5, t.pick(L('přilehlá k β', 'прилежащий к β')), c.derived, 14, 'start');
        }}
      />
      <CardText
        text={L(
          '**Přepona** leží naproti pravému úhlu. Dvě kratší strany jsou **odvěsny**: **přilehlá** vychází z vrcholu sledovaného úhlu, **protilehlá** leží naproti němu.',
          '**Гипотенуза** лежит напротив прямого угла. Две более короткие стороны — **катеты**: **прилежащий** выходит из вершины рассматриваемого угла, **противолежащий** лежит напротив него.',
        )}
      />
      <Steps
        steps={[
          step(L('Vyber přeponu', 'Выбери гипотенузу'), hypOk.v, pick, () => (
            <Note tone="ok" text={L('AB leží naproti pravému úhlu u C – je to přepona, nejdelší strana.', 'AB лежит напротив прямого угла C — это гипотенуза, самая длинная сторона.')} />
          )),
          step(L('Vyber odvěsnu **přilehlou** k úhlu α', 'Выбери катет, **прилежащий** к углу α'), adjOk.v, pick, () => (
            <Note tone="ok" text={L('AC vychází z vrcholu A – je přilehlá k α. AB z něj vychází taky, ale je to přepona.', 'AC выходит из вершины A — он прилежит к α. AB тоже выходит из A, но это гипотенуза.')} />
          )),
          step(L('Teď sleduj úhel β: vyber odvěsnu **přilehlou** k β', 'Теперь смотрим на угол β: выбери катет, **прилежащий** к β'), betaOk.v, pick, () => (
            <Note tone="ok" text={L('Při přechodu od α k β si odvěsny vymění role. Přepona AB zůstává stejná.', 'При переходе от α к β катеты меняются ролями. Гипотенуза AB остаётся той же.')} />
          )),
        ]}
      />
    </>
  );
}

/** Karta 3: kvíz – protilehlá strana u otočeného trojúhelníku. */
function CheckOpposite() {
  const t = useTr();
  // pravý úhel vlevo nahoře, α vlevo dole
  const a = pt(120, 195);
  const c0 = pt(120, 45);
  const bb = pt(340, 45);
  const q = useQuizStep(
    L('Která odvěsna je **protilehlá** úhlu α?', 'Какой катет **противолежит** углу α?'),
    [
      opt(L('a', 'a'), true),
      opt(
        L('b', 'b'),
        false,
        L('Strana b vychází přímo z vrcholu úhlu α – je k němu přilehlá. Která odvěsna se vrcholu α nedotýká?', 'Сторона b выходит прямо из вершины угла α — она прилежащая. Какой катет не касается вершины α?'),
      ),
      opt(L('c', 'c'), false, L('c leží naproti pravému úhlu – to je přepona, ne odvěsna.', 'c лежит напротив прямого угла — это гипотенуза, а не катет.')),
    ],
    L(
      'Strana a nevychází z vrcholu α, ale leží naproti němu – je protilehlá. Strana b je přilehlá odvěsna, c je přepona.',
      'Сторона a не выходит из вершины α, а лежит напротив неё — это противолежащий катет. Сторона b — прилежащий катет, c — гипотенуза.',
    ),
    null,
    3,
  );
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Otočený pravoúhlý trojúhelník se stranami a, b, c', 'Повёрнутый прямоугольный треугольник со сторонами a, b, c'))}
        draw={(b, c) => {
          b.poly(
            [
              [a.x, a.y],
              [bb.x, bb.y],
              [c0.x, c0.y],
            ],
            c.subtle,
            null,
            0,
            0.6,
          );
          b.line(c0.x, c0.y, bb.x, bb.y, q.done ? c.highlight : c.ink, q.done ? 4.5 : 2.6);
          b.line(a.x, a.y, c0.x, c0.y, c.ink, 2.6);
          b.line(a.x, a.y, bb.x, bb.y, c.ink, 2.6);
          b.rightAngleMark(c0, a, bb);
          b.angleArc(a.x, a.y, 30, -90, (Math.atan2(bb.y - a.y, bb.x - a.x) * 180) / Math.PI, 'α', c.highlight);
          b.text((c0.x + bb.x) / 2, c0.y - 10, 'a', q.done ? c.highlight : c.ink, 16, 'middle', null, true);
          b.text(a.x - 14, (a.y + c0.y) / 2 + 5, 'b', c.ink, 16, 'end', null, true);
          b.text((a.x + bb.x) / 2 + 16, (a.y + bb.y) / 2 + 16, 'c', c.ink, 16, 'start', null, true);
          if (q.done) b.text((c0.x + bb.x) / 2, c0.y + 22, t.pick(L('protilehlá k α', 'противолежащий α')), c.highlight, 13);
        }}
      />
      <CardText text={L('Otočení obrázku nemění vztahy mezi stranami a úhly.', 'Поворот рисунка не меняет отношения между сторонами и углами.')} />
      <Steps steps={[q]} />
    </>
  );
}

export const cards = [SameAngle, AngleSetsRatio, NameSides, CheckOpposite];
