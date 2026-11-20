/*
 * Lekce 4 – Moment síly. Síla umí těleso nejen posunout, ale i otočit. Jak moc otáčí, říká moment
 * M = F · r, kde r je KOLMÁ vzdálenost bodu od nositelky (ne vzdálenost k působišti podél nosníku).
 * (Přepis z aplikace: learn/Lesson4.kt.)
 */
import type { ReactNode } from 'react';
import {
  ActionButton,
  CardText,
  Chips,
  L,
  LearnJump,
  Note,
  Readout,
  SmallText,
  Stage,
  Steps,
  ValueSlider,
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
  useNumStep,
  useQuiz,
  useQuizStep,
  useTr,
  type Colors,
  type PrimBuilder,
  type QuizState,
  type Step,
} from '../kit';

/** Bod (x, y) otočený kolem (px, py) o [deg] stupňů (osa y dolů → kladný úhel = po směru hodinek). */
export function turn(x: number, y: number, px: number, py: number, deg: number): [number, number] {
  const a = (deg * Math.PI) / 180;
  const dx = x - px;
  const dy = y - py;
  return [px + dx * Math.cos(a) - dy * Math.sin(a), py + dx * Math.sin(a) + dy * Math.cos(a)];
}

/** Šestihranná matice (pohled shora) s ryskou, která se otáčí. */
export function bolt(b: PrimBuilder, cx: number, cy: number, r: number, spin: number, c: Colors) {
  const pts = Array.from({ length: 6 }, (_, i): [number, number] => {
    const a = ((i * 60 + 30) * Math.PI) / 180;
    return [cx + r * Math.cos(a), cy + r * Math.sin(a)];
  });
  b.poly(pts, c.subtle, c.ink, 1.6);
  b.circle(cx, cy, r * 0.45, c.paper, c.ink, 1.3);
  const a = (spin * Math.PI) / 180;
  b.line(cx, cy, cx + r * 0.8 * Math.cos(a), cy + r * 0.8 * Math.sin(a), c.accent, 3.0);
}

/** Moment, při kterém zarezlá matice povolí [N·m]. */
const BREAK_NM = 60;

/** Klíč (pohled shora) otočený kolem matice o [deg] (záporně = proti směru hodinových ručiček). */
function wrenchBody(b: PrimBuilder, px: number, py: number, len: number, w: number, deg: number, c: Colors, a = 1, pipeFrom: number | null = null) {
  const body = (
    [
      [px, py - w / 2],
      [px + len, py - w / 2],
      [px + len, py + w / 2],
      [px, py + w / 2],
    ] as [number, number][]
  ).map(([x, y]) => turn(x, y, px, py, deg));
  b.poly(body, c.beam, c.ink, 1.6, a);
  if (pipeFrom !== null) {
    // nasazená trubka – širší, šedá
    const pipe = (
      [
        [px + pipeFrom, py - w / 2 - 4],
        [px + len + 175, py - w / 2 - 4],
        [px + len + 175, py + w / 2 + 4],
        [px + pipeFrom, py + w / 2 + 4],
      ] as [number, number][]
    ).map(([x, y]) => turn(x, y, px, py, deg));
    b.poly(pipe, c.subtle, c.inkSoft, 1.4, a);
  }
}

/** Krok s doplněným výsledkem (např. poznámka navíc pod vysvětlením kvízu). */
const plusResult = (s: Step, extra: () => ReactNode): Step => ({
  ...s,
  result: () => (
    <>
      {s.result?.()}
      {extra()}
    </>
  ),
});

/**
 * Karta 0: zarezlá matice. Ruka táhne kolmo ke klíči silou 200 N (později slabší 100 N); matice povolí až při momentu
 * 60 N·m. Pod touto hranicí se nehne nic (drží ji tření), nad ní se klíč s maticí rozjedou a pootočí.
 */
function Wrench() {
  const tt = useTr();
  const kit = useKit();
  const nm = tt.pick(L('N·m', 'Н·м'));
  const px = 46;
  const py = 172;
  const unit = 330; // 1 m = 330 jednotek
  const wrenchLen = 0.5; // m
  const r = kit.mut(0.2);
  const pipe = kit.mut(false);
  const dragging = kit.mut(false);
  const running = kit.mut(false);
  const pulling = kit.mut(false);
  const angle = kit.mut(0);
  const alpha = kit.mut(1);
  const last = kit.mut<boolean | null>(null); // výsledek posledního pokusu: povolila?
  const tryM = kit.mut<[number, number, boolean] | null>(null); // poslední pokus: F, r, povolila
  const failed = kit.mut(false);
  const minArm = kit.mut(false);
  const weak = kit.mut(false);
  const calc = useNumStep(
    L('Slabší ruka má jen 100 N. Jaké rameno potřebuje?', 'Рука слабее, всего 100 Н. Какое плечо ей нужно?'),
    [numSpec(L('[[r]]', '[[r]]'), 0.6, tt.pick(L('m', 'м')))],
    L('100 · [[r]] = 60, tedy [[r]] = 0,6 m. Poloviční síla potřebuje dvojnásobné rameno.', '100 · [[r]] = 60, то есть [[r]] = 0,6 м. Вдвое меньшей силе нужно вдвое большее плечо.'),
    [
      trapNear(
        L('To stačilo pro 200 N. Síla je poloviční – co musí udělat rameno, aby součin zůstal 60 N·m?', 'Этого хватало для 200 Н. Сила вдвое меньше — что должно стать с плечом, чтобы произведение осталось 60 Н·м?'),
        0.3,
      ),
      trapNear(L('Moment je síla × rameno, ne síla / rameno: 100 · [[r]] = 60.', 'Момент — это сила × плечо, а не сила / плечо: 100 · [[r]] = 60.'), 1.67),
    ],
    L('Moment musí dosáhnout 60 N·m: 100 · [[r]] = 60.', 'Момент должен дойти до 60 Н·м: 100 · [[r]] = 60.'),
  );
  // 0 = první pokus, 1 = nejkratší rameno, 2 = výpočet pro 100 N, 3 = trubka, 4 = hotovo
  const stage = !failed.v ? 0 : !minArm.v ? 1 : !calc.done ? 2 : !weak.v ? 3 : 4;
  const f = stage >= 2 ? 100 : 200;
  const canMove = stage === 1 || stage >= 3;
  useLaunched([stage], async () => {
    tryM.v = null;
  });
  const maxR = pipe.v ? 1.0 : wrenchLen;
  const setR = (v: number) => {
    if (running.v || !canMove) return;
    r.v = clamp(Math.round(v * 20) / 20, 0.1, maxR);
    last.v = null;
  };
  const pull = () => {
    if (running.v) return;
    running.v = true;
    const ff = f;
    const rr = r.v;
    const ok = ff * rr >= BREAK_NM - 1e-9;
    kit.scope.launch(async (job) => {
      pulling.v = true;
      last.v = null;
      tryM.v = null;
      if (!ok) {
        // tření v závitu drží: nic se nepohne
        await job.delay(1100);
        pulling.v = false;
        last.v = false;
        tryM.v = [ff, rr, false];
        failed.v = true;
        await job.delay(300);
      } else {
        // přebytek momentu nad třením rozjíždí klíč i matici – úhel roste čím dál rychleji
        const acc = 40 + (ff * rr - BREAK_NM) * 4; // °/s²
        let time = 0;
        let lastT = 0;
        while (angle.v > -22) {
          const now = await job.frame();
          const dt = lastT === 0 ? 0 : (now - lastT) / 1000;
          lastT = now;
          time += dt;
          angle.v = Math.max(-0.5 * acc * time * time, -22);
        }
        pulling.v = false;
        last.v = true;
        tryM.v = [ff, rr, true];
        if (ff === 200 && Math.abs(rr - 0.3) < 1e-9) minArm.v = true;
        if (ff === 100) weak.v = true;
        await job.delay(1300);
        // povolená matice zmizí, na její místo přijde nová zarezlá (klíč se nevrací zpátky)
        for (let i = 1; i <= 8; i++) {
          alpha.v = 1 - i / 8;
          await job.delay(30);
        }
        angle.v = 0;
        for (let i = 1; i <= 8; i++) {
          alpha.v = i / 8;
          await job.delay(30);
        }
      }
      running.v = false;
    });
  };
  const xr = px + r.v * unit;
  // výsledek posledního zatažení – součin se ukáže až po pokusu
  const tryLine = () => {
    if (!tryM.v) return null;
    const [ff, rr, ok] = tryM.v;
    return (
      <>
        <Readout text={`[[M]] = ${num(ff, 0)} · ${num(rr)} = ${num(ff * rr, 0)} ${nm} ` + (ok ? '≥ 60 ✓' : '< 60')} ok={ok} />
        {ok && stage === 1 && <SmallText text={L('Povolila – ale nejde to i kratším ramenem?', 'Поддалась — а нельзя ли плечом покороче?')} />}
      </>
    );
  };
  const slider = () => (
    <ValueSlider
      label={tt.pick(L(`Rameno [[r]] = ${num(r.v)} m`, `Плечо [[r]] = ${num(r.v)} м`))}
      value={r.v}
      min={0.1}
      max={maxR}
      steps={Math.round((maxR - 0.1) * 20) - 1}
      onChange={setR}
    />
  );
  const pullBtn = () => <ActionButton text={L('Zatáhni', 'Потянуть')} enabled={!running.v} onClick={pull} />;
  return (
    <>
      <Stage
        h={240}
        description={tt.pick(L('Klíč na zarezlé matici, ruka táhne v nastavitelné vzdálenosti od osy', 'Ключ на прикипевшей гайке, рука тянет на изменяемом расстоянии от оси'))}
        onDrag={(pt, start) => {
          if (start) dragging.v = !running.v && canMove && Math.abs(pt.y - py) < 50 && pt.x > px + 15;
          if (dragging.v) setR((pt.x - px) / unit);
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b, c) => {
          wrenchBody(b, px, py, wrenchLen * unit, 18, angle.v, c, alpha.v, pipe.v ? wrenchLen * unit - 15 : null);
          bolt(b, px, py, 24, angle.v - 90, c);
          // ruka táhne kolmo ke klíči (otáčí se s ním)
          const [ax, ay] = turn(xr, py - 9, px, py, angle.v);
          const [bx, by] = turn(xr, py - 82, px, py, angle.v);
          if (pulling.v || !running.v) b.force(ax, ay, bx, by, c.load, 3.0, 11, pulling.v, alpha.v);
          if (!running.v) {
            const right = xr > 300;
            b.text(right ? xr - 8 : xr + 8, py - 60, ` = ${num(f, 0)} N`, c.load, 14, right ? 'end' : 'start', 'F');
            if (canMove) b.handle(xr, py, dragging.v);
            else b.circle(xr, py, 4, c.ink);
            // rameno
            b.line(px, py + 30, xr, py + 30, c.accent, 1.4);
            b.line(px, py + 23, px, py + 37, c.accent, 1.4);
            b.line(xr, py + 23, xr, py + 37, c.accent, 1.4);
            b.text((px + xr) / 2, py + 48, ` = ${num(r.v)} m`, c.accent, 13, 'middle', 'r');
          }
          if (last.v === false) b.text(200, 30, tt.pick(L('nic – matice drží', 'ничего — гайка держит')), c.load, 14, 'middle', null, true);
          else if (last.v === true) b.text(200, 30, tt.pick(L('povolila! ✓', 'открутилась! ✓')), c.accent, 15, 'middle', null, true);
          else b.text(200, 30, tt.pick(L('matice povolí až při momentu 60 N·m', 'гайка поддастся только при моменте 60 Н·м')), c.inkSoft, 12);
        }}
      />
      <CardText
        text={L(
          'Síla umí těleso i **otočit**. Zarezlá matice povolí, až když ji otáčí **moment** aspoň 60 N·m. Moment síly [[M]] = síla × **rameno** [[r]] (vzdálenost síly od osy matice).',
          'Сила может тело ещё и **повернуть**. Прикипевшая гайка поддастся, только когда её поворачивает **момент** не меньше 60 Н·м. Момент силы [[M]] = сила × **плечо** [[r]] (rameno — расстояние от силы до оси гайки).',
        )}
      />
      <Steps
        steps={[
          step(L('Zatáhni silou 200 N na rameni 0,2 m – povolí matice?', 'Потяни с силой 200 Н на плече 0,2 м — поддастся ли гайка?'), failed.v, pullBtn, () => (
            <Note
              tone="info"
              text={L('Nehnula se: [[M]] = 200 · 0,2 = 40 N·m, a to je míň než 60 N·m. Tření v závitu drží.', 'Не сдвинулась: [[M]] = 200 · 0,2 = 40 Н·м, а это меньше 60 Н·м. Трение в резьбе держит.')}
            />
          )),
          step(
            L('Najdi **nejkratší** rameno, kterým matici silou 200 N povolíš', 'Найди **самое короткое** плечо, которым открутишь гайку силой 200 Н'),
            minArm.v,
            () => (
              <>
                {slider()}
                {pullBtn()}
                {tryLine()}
              </>
            ),
            () => (
              <SmallText
                text={L(
                  '200 N · 0,3 m = 60 N·m – právě dost. O kousek kratší rameno (0,25 m) dá jen 50 N·m.',
                  '200 Н · 0,3 м = 60 Н·м — как раз достаточно. Чуть короче (0,25 м) — и выйдет только 50 Н·м.',
                )}
              />
            ),
          ),
          calc,
          step(
            L('Klíč má jen 0,5 m. Prodluž ho trubkou a matici povol', 'Ключ всего 0,5 м. Удлини его трубой и открути гайку'),
            weak.v,
            () => (
              <>
                <Chips
                  options={[pipe.v ? L('Sundej trubku', 'Снять трубу') : L('Nasaď trubku (prodloužení na 1 m)', 'Надеть трубу (удлинение до 1 м)')]}
                  selected={pipe.v ? 0 : null}
                  onPick={() => {
                    if (!running.v) {
                      pipe.v = !pipe.v;
                      if (!pipe.v) r.v = Math.min(r.v, wrenchLen);
                      last.v = null;
                    }
                  }}
                />
                {slider()}
                {pullBtn()}
                {tryLine()}
              </>
            ),
            () => (
              <Note
                tone="ok"
                text={L(
                  'Poloviční síla potřebuje dvojnásobné rameno: 100 N · 0,6 m = 60 N·m. Proto mechanici nasazují na klíč trubku. U nosníků budou síly v kN a ramena v metrech – moment pak vyjde v kNm (1 kNm = 1000 N·m).',
                  'Вдвое меньшей силе нужно вдвое большее плечо: 100 Н · 0,6 м = 60 Н·м. Поэтому механики надевают на ключ трубу. У балок силы будут в кН, плечи в метрах — момент получится в кН·м (1 кН·м = 1000 Н·м).',
                )}
              />
            ),
          ),
        ]}
      />
    </>
  );
}

/**
 * Karta 1: šikmá síla – rameno je kolmá vzdálenost k nositelce, ne délka klíče. Stejná zarezlá matice (60 N·m),
 * ruka 200 N na konci klíče 0,4 m: povolí jen dost „kolmý“ tah (sin α ≥ 0,75).
 */
function PerpendicularArm() {
  const tt = useTr();
  const kit = useKit();
  const nm = tt.pick(L('N·m', 'Н·м'));
  const px = 70;
  const py = 130;
  const len = 0.4; // m
  const unit = 525; // 0,4 m = 210 jednotek
  const ex = px + len * unit;
  const ey = py;
  const f = 200;
  const alpha = kit.mut(45); // úhel mezi silou a klíčem
  const dragging = kit.mut(false);
  const tip = kit.mut<number | null>(null); // 0 = povolí, 1 = nepovolí
  const tested = kit.mut(false);
  const zero = kit.mut(false);
  const running = kit.mut(false);
  const pulling = kit.mut(false);
  const angle = kit.mut(0);
  const fade = kit.mut(1);
  const last = kit.mut<boolean | null>(null);
  const tryRes = kit.mut<[number, boolean] | null>(null); // poslední pokus: úhel, povolila
  const okAt = kit.list<number>([]);
  const failAt = kit.list<number>([]);
  const a = (alpha.v * Math.PI) / 180;
  // síla na konci klíče: 90° = kolmo nahoru, malý úhel = skoro podél klíče k ose, 0°/180° = přesně podél
  const ux = -Math.cos(a);
  const uy = -Math.sin(a);
  const rPerp = len * Math.sin(a);
  const m = f * rPerp;
  // hranice: povolilo při úhlu, o 15° „méně kolmém“ to nešlo (z kterékoli strany od 90°)
  const limit = okAt.v.some((o) => failAt.v.some((fl) => Math.abs(o - fl) === 15 && Math.abs(fl - 90) > Math.abs(o - 90)));
  // 0 = tip, 1 = pokus při 45°, 2 = nulový moment, 3 = hranice
  const stage = tip.v === null ? 0 : !tested.v ? 1 : !zero.v ? 2 : !limit ? 3 : 4;
  const canTurn = stage >= 2;
  useLaunched([stage], async () => {
    tryRes.v = null;
  });
  const setAlpha = (v: number) => {
    if (running.v || !canTurn) return;
    const na = clamp(Math.round(v / 15) * 15, 0, 180);
    if (na !== alpha.v) {
      alpha.v = na;
      last.v = null;
    }
  };
  const pull = () => {
    if (running.v) return;
    running.v = true;
    const ok = m >= BREAK_NM - 1e-6;
    const at = alpha.v;
    const st = stage;
    const noArm = Math.abs(rPerp) < 1e-6;
    kit.scope.launch(async (job) => {
      pulling.v = true;
      last.v = null;
      tryRes.v = null;
      if (!ok) {
        await job.delay(1000);
        pulling.v = false;
        last.v = false;
        tryRes.v = [at, false];
        if (!failAt.v.includes(at)) failAt.add(at);
        if (st === 1) tested.v = true;
        if (st >= 2 && noArm) zero.v = true;
      } else {
        let time = 0;
        let lastT = 0;
        const acc = 40 + (m - BREAK_NM) * 5;
        while (angle.v > -18) {
          const now = await job.frame();
          const dt = lastT === 0 ? 0 : (now - lastT) / 1000;
          lastT = now;
          time += dt;
          angle.v = Math.max(-0.5 * acc * time * time, -18);
        }
        pulling.v = false;
        last.v = true;
        tryRes.v = [at, true];
        // hranice se hledá až ve 4. kroku (ať se nesplní mimochodem)
        if (st >= 3 && !okAt.v.includes(at)) okAt.add(at);
        await job.delay(1100);
        for (let i = 1; i <= 8; i++) {
          fade.v = 1 - i / 8;
          await job.delay(30);
        }
        angle.v = 0;
        for (let i = 1; i <= 8; i++) {
          fade.v = i / 8;
          await job.delay(30);
        }
      }
      running.v = false;
    });
  };
  const tipX = ex - ux * 7;
  const tipY = ey - uy * 7;
  const tailX = ex - ux * 80;
  const tailY = ey - uy * 80;
  const mText = (at: number) => {
    const rp = len * Math.sin((at * Math.PI) / 180);
    return `[[M]] = 200 · ${num(rp, 3)} = ${num(f * rp, 0)} ${nm}`;
  };
  const pullButton = () => <ActionButton text={L('Zatáhni', 'Потянуть')} enabled={!running.v} onClick={pull} />;
  const slider = () => (
    <ValueSlider label={tt.pick(L(`Úhel síly ke klíči α = ${num(alpha.v, 0)}°`, `Угол силы к ключу α = ${num(alpha.v, 0)}°`))} value={alpha.v} min={0} max={180} steps={11} onChange={setAlpha} />
  );
  const tipOpts = [L('ano, povolí', 'да, поддастся'), L('ne, nepovolí', 'нет, не поддастся')];
  return (
    <>
      <Stage
        h={250}
        description={tt.pick(L('Šikmá síla na konci klíče a kolmé rameno', 'Наклонная сила на конце ключа и её плечо'))}
        onDrag={(pt, start) => {
          if (start) dragging.v = !running.v && canTurn && dist(pt, tailX, tailY) < 60;
          if (dragging.v) {
            // směr síly = od prstu k hrotu na konci klíče
            const dx = pt.x - ex;
            const dy = pt.y - ey;
            if (Math.hypot(dx, dy) > 8) {
              let deg = (Math.atan2(dy, dx) * 180) / Math.PI;
              if (deg < 0) deg = deg < -90 ? 180 : 0;
              setAlpha(deg);
            }
          }
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b, c) => {
          wrenchBody(b, px, py, len * unit + 8, 14, angle.v, c, fade.v);
          bolt(b, px, py, 22, angle.v - 90, c);
          if (!running.v || pulling.v) {
            const [tx0, ty0] = turn(tailX, tailY, px, py, angle.v);
            const [tx1, ty1] = turn(tipX, tipY, px, py, angle.v);
            b.force(tx0, ty0, tx1, ty1, c.load, 3.0, 11, pulling.v, fade.v);
          }
          if (!running.v) {
            // nositelka síly (čárkovaně) a prodloužení klíče, od kterého měříme úhel
            b.line(ex - ux * 400, ey - uy * 400, ex + ux * 400, ey + uy * 400, c.inkSoft, 1.0, true);
            b.line(ex, ey, ex + 46, ey, c.inkSoft, 1.0, true);
            if (alpha.v > 0) b.arc(ex, ey, 30, 0, alpha.v, c.inkSoft, 1.0);
            const mid = ((alpha.v / 2) * Math.PI) / 180;
            b.text(ex + 44 * Math.cos(mid) + 4, ey + 44 * Math.sin(mid) + 5, `${num(alpha.v, 0)}°`, c.inkSoft, 12, 'start');
            // pata kolmice z osy na nositelku
            const k = (px - ex) * ux + (py - ey) * uy;
            const fx = ex + ux * k;
            const fy = ey + uy * k;
            b.line(px, py, fx, fy, c.accent, 2.0, true);
            const vl = Math.hypot(px - fx, py - fy);
            if (vl > 20) {
              const vx = (px - fx) / vl;
              const vy = (py - fy) / vl;
              const s = 9;
              const sgn = (ex - fx) * ux + (ey - fy) * uy > 0 ? 1 : -1;
              const ax = fx + ux * s * sgn;
              const ay = fy + uy * s * sgn;
              b.line(ax, ay, ax + vx * s, ay + vy * s, c.accent, 1.2);
              b.line(fx + vx * s, fy + vy * s, ax + vx * s, ay + vy * s, c.accent, 1.2);
              b.text((px + fx) / 2 - 10, (py + fy) / 2 + (fy < py ? -4 : 16), ` = ${num(rPerp, 3)} m`, c.accent, 14, 'end', 'r_⊥');
            } else b.text(px, py + 40, tt.pick(L('nositelka prochází osou: r⊥ = 0', 'линия действия проходит через ось: r⊥ = 0')), c.accent, 13);
            if (alpha.v >= 1 && alpha.v <= 179) {
              if (alpha.v < 90) b.text(tailX - 16, tailY + (alpha.v < 45 ? 22 : 5), ' = 200 N', c.load, 14, 'end', 'F');
              else b.text(tailX + 16, tailY + 5, ' = 200 N', c.load, 14, 'start', 'F');
            } else b.text(tailX, tailY - 14, ' = 200 N', c.load, 14, 'middle', 'F');
            if (canTurn) b.handle(tailX, tailY, dragging.v);
          }
          if (last.v === false) b.text(200, 238, tt.pick(L('nic – matice drží', 'ничего — гайка держит')), c.load, 14, 'middle', null, true);
          else if (last.v === true) b.text(200, 238, tt.pick(L('povolila! ✓', 'открутилась! ✓')), c.accent, 15, 'middle', null, true);
        }}
      />
      <CardText
        text={L(
          'Stejná zarezlá matice (povolí při 60 N·m), klíč 0,4 m, ruka 200 N na jeho konci – ale táhne **šikmo**. Rameno [[r_⊥]] není délka klíče, ale **kolmá** vzdálenost osy od nositelky síly (zelená čárkovaná).',
          'Та же прикипевшая гайка (поддаётся при 60 Н·м), ключ 0,4 м, рука 200 Н на его конце — но тянет **наискосок**. Плечо [[r_⊥]] — не длина ключа, а расстояние **по перпендикуляру** от оси до линии действия силы (зелёный пунктир).',
        )}
      />
      <Steps
        steps={[
          tipStep(L('Tipni si: povolí matice, když ruka táhne pod úhlem 45°?', 'Как думаешь: поддастся ли гайка, если тянуть под углом 45°?'), tipOpts, tip.v, (i) => (tip.v = i)),
          step(L('Zatáhni pod úhlem 45° a ověř svůj tip', 'Потяни под углом 45° и проверь свой прогноз'), tested.v, pullButton, () => (
            <Note
              tone={tip.v === 1 ? 'ok' : 'info'}
              text={L(
                (tip.v === 1 ? 'Tip sedí. ' : 'Tip nesedí. ') + 'Matice nepovolila: šikmý tah má kratší rameno [[r_⊥]] = 0,4 · sin 45° = 0,283 m, takže [[M]] = 200 · 0,283 = 57 N·m < 60.',
                (tip.v === 1 ? 'Прогноз верный. ' : 'Прогноз неверный. ') + 'Гайка не поддалась: у наклонной тяги плечо короче, [[r_⊥]] = 0,4 · sin 45° = 0,283 м, поэтому [[M]] = 200 · 0,283 = 57 Н·м < 60.',
              )}
            />
          )),
          step(
            L('Natoč sílu tak, aby matici **vůbec neotáčela**, a zatáhni', 'Поверни силу так, чтобы она гайку **совсем не крутила**, и потяни'),
            zero.v,
            () => (
              <>
                {slider()}
                {pullButton()}
                {tryRes.v &&
                  (tryRes.v[1] ? (
                    <SmallText text={L(`Povolila (${mText(tryRes.v[0])}). Hledáš ale tah, který neotáčí vůbec.`, `Поддалась (${mText(tryRes.v[0])}). А нужна тяга, которая совсем не крутит.`)} />
                  ) : (
                    <SmallText
                      text={L(
                        `Nepovolila, ale otáčet se ji snažíš: ${mText(tryRes.v[0])}. Kdy bude rameno nulové?`,
                        `Не поддалась, но крутить её ты всё же пытаешься: ${mText(tryRes.v[0])}. Когда плечо станет нулевым?`,
                      )}
                    />
                  ))}
              </>
            ),
            () => (
              <SmallText
                text={L(
                  'Při 0° nebo 180° míří síla podél klíče a její nositelka prochází osou – rameno 0, moment 0. Táhneš, ale nic neotočíš.',
                  'При 0° или 180° сила направлена вдоль ключа, её линия действия проходит через ось — плечо 0, момент 0. Тянешь, но ничего не поворачиваешь.',
                )}
              />
            ),
          ),
          step(
            L('Najdi **nejšikmější** tah, kterým matice ještě povolí', 'Найди **самый наклонный** рывок, при котором гайка ещё поддаётся'),
            limit,
            () => (
              <>
                {slider()}
                {pullButton()}
                {tryRes.v &&
                  (tryRes.v[1] ? (
                    <SmallText
                      text={L(
                        `Povolila při ${num(tryRes.v[0], 0)}° (${mText(tryRes.v[0])}) – zkus táhnout ještě šikměji.`,
                        `Поддалась при ${num(tryRes.v[0], 0)}° (${mText(tryRes.v[0])}) — попробуй тянуть ещё наклоннее.`,
                      )}
                    />
                  ) : (
                    <SmallText text={L(`Při ${num(tryRes.v[0], 0)}° ne: ${mText(tryRes.v[0])} < 60.`, `При ${num(tryRes.v[0], 0)}° — нет: ${mText(tryRes.v[0])} < 60.`)} />
                  ))}
              </>
            ),
            () => (
              <>
                <Note
                  tone="ok"
                  text={L(
                    'Hranice je mezi 45° a 60°: 200 · 0,4 · sin 60° = 69 N·m stačí, 200 · 0,4 · sin 45° = 57 N·m už ne. Největší moment (80 N·m) dá tah kolmo, α = 90°.',
                    'Граница между 45° и 60°: 200 · 0,4 · sin 60° = 69 Н·м хватает, 200 · 0,4 · sin 45° = 57 Н·м — уже нет. Наибольший момент (80 Н·м) даёт тяга перпендикулярно, α = 90°.',
                  )}
                />
                <LearnJump lesson={108} card={3} label={L('Jak spočítat rameno šikmé síly? → Trigonometrie: rameno = r · sin α', 'Как найти плечо наклонной силы? → Тригонометрия: плечо = r · sin α')} />
              </>
            ),
          ),
        ]}
      />
    </>
  );
}

/** Spirálová pružina v pantu (pohled shora); čím víc jsou dveře otevřené, tím víc je natočená. */
function hingeSpring(b: PrimBuilder, hx: number, hy: number, deg: number, c: Colors) {
  const pts: [number, number][] = [];
  const turns = 2.2 * 360 + Math.abs(deg) * 2;
  for (let a = 0; a <= turns; a += 12) {
    const rr = 9 + (9 * a) / turns;
    const ang = ((a - deg) * Math.PI) / 180;
    pts.push([hx + rr * Math.cos(ang), hy + rr * Math.sin(ang)]);
  }
  for (let i = 1; i < pts.length; i++) b.line(pts[i - 1][0], pts[i - 1][1], pts[i][0], pts[i][1], c.react, 1.3);
}

/**
 * Karta 2: dveře shora se pružinou v pantu (samozavírací). Stejná síla 20 N na třech místech: pružina se natočí,
 * až její moment k·θ vyrovná moment ruky F·r – dveře se otevřou úměrně ramenu a drží. Pustíš → pružina je zavře.
 */
function Door() {
  const tt = useTr();
  const kit = useKit();
  const nm = tt.pick(L('N·m', 'Н·м'));
  const hx = 90;
  const hy = 150;
  const len = 220; // 0,9 m
  const doorM = 0.9;
  const spots = [0.0, 0.5, 0.93]; // u pantu, uprostřed, u kliky (podíl šířky dveří)
  const f = 20; // N
  const k = 0.48; // N·m na 1° natočení pružiny
  const pushed = kit.mut<number | null>(null);
  const tried = kit.mut<Set<number>>(new Set());
  const running = kit.mut(false);
  const holding = kit.mut(false);
  const theta = kit.mut(0); // ° otevření
  const most = useQuizStep(
    L('Kde stejně velká síla otevře dveře **nejvíc**?', 'Где одинаковая сила откроет дверь **шире всего**?'),
    [
      opt(L('u pantu', 'у петли'), false, L('U pantu je síla nejblíž ose otáčení. Jaké tam má rameno?', 'У петли сила ближе всего к оси вращения. Какое у неё там плечо?')),
      opt(L('uprostřed', 'посередине'), false, L('Je uprostřed rameno největší? Kde je bod nejdál od pantu?', 'Разве посередине плечо наибольшее? Где точка дальше всего от петли?')),
      opt(L('u kliky', 'у ручки'), true),
      opt(L('všude stejně', 'везде одинаково'), false, L('Otáčení nezávisí jen na síle. Na čem ještě? Vzpomeň na klíč a matici.', 'Поворот зависит не только от силы. От чего ещё? Вспомни ключ и гайку.')),
    ],
    L(
      'U kliky je rameno nejdelší, a tak i moment F · r – pružina se musí natočit nejvíc, než ho vyrovná.',
      'У ручки плечо самое длинное, а значит, и момент F · r — пружине нужно закрутиться сильнее всего, чтобы его уравновесить.',
    ),
    null,
    2,
  );
  const qMid = useQuiz(
    L('U kliky to bylo 35°. O kolik se dveře otevřou, když zatlačíš uprostřed?', 'У ручки вышло 35°. На сколько откроется дверь, если толкнуть посередине?'),
    [
      opt(L('taky asi o 35°', 'тоже примерно на 35°'), false, L('Síla je stejná, ale rameno? Uprostřed je pant blíž.', 'Сила та же, а плечо? Посередине петля ближе.')),
      opt(L('asi o polovinu', 'примерно вдвое меньше'), true),
      opt(L('skoro vůbec', 'почти не откроется'), false, L('Rameno uprostřed není nulové – je zhruba poloviční. Co to udělá s momentem?', 'Плечо посередине не нулевое — оно примерно вдвое меньше. Что это сделает с моментом?')),
    ],
    L(
      'Rameno je zhruba poloviční, a tak i moment F · r. Moment pružiny k·θ roste s úhlem – vyrovná ho tedy už zhruba poloviční natočení.',
      'Плечо примерно вдвое меньше, значит, и момент F · r. Момент пружины k·θ растёт с углом — значит, его уравновесит уже примерно вдвое меньший поворот.',
    ),
  );
  const qHinge = useQuiz(
    L('A když zatlačíš přímo u pantu?', 'А если толкнуть прямо у петли?'),
    [
      opt(L('otevřou se jen trochu', 'откроется чуть-чуть'), false, L('Kudy vede nositelka síly? Jaké má k ose pantu rameno?', 'Где проходит линия действия силы? Какое у неё плечо относительно оси петли?')),
      opt(L('nehnou se vůbec', 'не сдвинется вообще'), true),
    ],
    L('Nositelka síly prochází osou pantu – rameno 0, moment 0. Pružina se nemusí natočit vůbec.', 'Линия действия силы проходит через ось петли — плечо 0, момент 0. Пружине вообще не нужно закручиваться.'),
  );
  // místo, kde má žák právě zatlačit (null = teď se netlačí, -1 = kdekoli)
  const want: number | null = !most.done ? null : !tried.v.has(2) ? 2 : !qMid.solved ? null : !tried.v.has(1) ? 1 : !qHinge.solved ? null : !tried.v.has(0) ? 0 : -1;
  const push = (i: number) => {
    if (running.v) return;
    running.v = true;
    pushed.v = i;
    kit.scope.launch(async (job) => {
      const target = (f * spots[i] * doorM) / k;
      const w0 = (2 * Math.PI) / 1.1;
      const zeta = 0.45;
      let om = 0;
      let time = 0;
      let last = 0;
      holding.v = true;
      // ruka tlačí 2,2 s, pak pustí a pružina dveře zavře
      while (time < 4.6) {
        const now = await job.frame();
        const dt = last === 0 ? 0 : Math.min((now - last) / 1000, 0.05);
        last = now;
        time += dt;
        holding.v = time < 2.2;
        const goal = holding.v ? target : 0;
        const acc = w0 * w0 * (goal - theta.v) - 2 * zeta * w0 * om;
        om += acc * dt;
        theta.v = Math.max(theta.v + om * dt, 0); // zárubeň nedovolí otevřít dovnitř
        if (theta.v === 0 && om < 0) om = 0;
      }
      theta.v = 0;
      holding.v = false;
      tried.v = new Set([...tried.v, i]);
      running.v = false;
    });
  };
  const readout = (i: number) => {
    const r = spots[i] * doorM;
    return `[[M]] = 20 · ${num(r)} = ${num(f * r, 1)} ${nm} → θ = ${num((f * r) / k, 0)}°`;
  };
  /** předpověď (kvíz) a hned pokus tlačítkem: krok je hotový, až dveře pokus dokončí */
  const predictThenPush = (q: QuizState, i: number, button: L, note: () => ReactNode): Step => ({
    label: q.question,
    done: q.solved && tried.v.has(i),
    result: () => (
      <>
        {q.Feedback()}
        <Readout text={readout(i)} />
        {note()}
      </>
    ),
    controls: () =>
      !q.solved ? (
        <>
          {q.Options()}
          {q.Feedback()}
        </>
      ) : (
        <ActionButton text={button} enabled={!running.v} onClick={() => push(i)} />
      ),
  });
  return (
    <>
      <Stage
        h={230}
        description={tt.pick(L('Samozavírací dveře viděné shora, pant s pružinou vlevo', 'Самозакрывающаяся дверь сверху, петля с пружиной слева'))}
        onTap={(pt) => {
          const w = want;
          if (w === null) return;
          const far = (i: number) => {
            const [sx, sy] = turn(hx + spots[i] * len, hy + 30, hx, hy, -theta.v);
            return dist(pt, sx, sy);
          };
          const cands = w >= 0 ? [w] : spots.map((_, i) => i);
          const i = cands.reduce((bst, x) => (far(x) < far(bst) ? x : bst), cands[0]);
          if (far(i) < 50) push(i);
        }}
        draw={(b, c) => {
          b.rect(8, hy - 7, hx - 16, 14, c.subtle, c.inkSoft, 1.0);
          b.rect(hx + len + 8, hy - 7, 400 - (hx + len + 8) - 8, 14, c.subtle, c.inkSoft, 1.0);
          b.text(30, hy + 26, tt.pick(L('zeď', 'стена')), c.inkSoft, 12, 'start');
          const d = -theta.v;
          hingeSpring(b, hx, hy, theta.v, c);
          const pts = (
            [
              [hx, hy - 5],
              [hx + len, hy - 5],
              [hx + len, hy + 5],
              [hx, hy + 5],
            ] as [number, number][]
          ).map(([x, y]) => turn(x, y, hx, hy, d));
          b.poly(pts, c.beam, c.ink, 1.6);
          b.circle(hx, hy, 6, c.paper, c.ink, 1.8);
          b.text(hx - 12, hy - 40, tt.pick(L('pant s pružinou', 'петля с пружиной')), c.react, 12, 'end');
          const [kx, ky] = turn(hx + len - 14, hy + 12, hx, hy, d);
          b.circle(kx, ky, 5, c.ink);
          spots.forEach((sp, i) => {
            const [sx, sy] = turn(hx + sp * len, hy + 6, hx, hy, d);
            const [bx, by] = turn(hx + sp * len, hy + 66, hx, hy, d);
            if (pushed.v === i && holding.v) {
              b.force(bx, by, sx, sy, c.load, 2.4, 11, true);
              b.text(bx + 8, by - 6, ' = 20 N', c.load, 13, 'start', 'F');
            } else if (!running.v && (want === i || want === -1)) {
              const [tx, ty] = turn(hx + sp * len, hy + 30, hx, hy, d);
              b.tapSpot(tx, ty);
            }
          });
          if (theta.v > 2) {
            // pružina vrací dveře: moment proti otevírání
            b.moment(hx, hy, 30, false, c.react, 1.8);
            b.text(hx + 36, hy + 40, `k·θ = ${num(k * theta.v, 0)} N·m`, c.react, 12, 'start');
          }
          if (running.v || theta.v > 0.5) b.text(390, 24, `θ = ${num(theta.v, 0)}°`, c.ink, 14, 'end', null, true);
        }}
      />
      <CardText
        text={L(
          'Samozavírací dveře shora: pružina v pantu se brání tím víc, čím víc dveře otevřeš. Tlačíme vždy stejnou silou 20 N – dveře se otevřou tak daleko, až pružina moment ruky vyrovná.',
          'Самозакрывающаяся дверь сверху: пружина в петле сопротивляется тем сильнее, чем шире открыта дверь. Толкаем всегда с силой 20 Н — дверь открывается, пока пружина не уравновесит момент руки.',
        )}
      />
      <Steps
        steps={[
          most,
          step(
            L('Zatlač u kliky a sleduj úhel otevření', 'Толкни у ручки и следи за углом открытия'),
            tried.v.has(2),
            () => <ActionButton text={L('Zatlač u kliky', 'Толкнуть у ручки')} enabled={!running.v} onClick={() => push(2)} />,
            () => (
              <>
                <Readout text={readout(2)} />
                <Note
                  tone="ok"
                  text={L(
                    'Nejdelší rameno = největší moment = dveře nejvíc otevřené. Proto je klika na kraji dveří, co nejdál od pantu.',
                    'Самое длинное плечо = самый большой момент = дверь открыта шире всего. Поэтому ручка на краю двери, как можно дальше от петли.',
                  )}
                />
              </>
            ),
          ),
          predictThenPush(qMid, 1, L('Zatlač uprostřed a ověř', 'Толкнуть посередине и проверить'), () => (
            <Note
              tone="info"
              text={L(
                'Uprostřed je rameno asi poloviční než u kliky – moment i úhel otevření jsou taky zhruba poloviční.',
                'Посередине плечо примерно вдвое короче, чем у ручки, — момент и угол открытия тоже примерно вдвое меньше.',
              )}
            />
          )),
          predictThenPush(qHinge, 0, L('Zatlač u pantu a ověř', 'Толкнуть у петли и проверить'), () => (
            <Note
              tone="info"
              text={L(
                'Nic! Nositelka síly prochází pantem (osou otáčení) – rameno 0, moment 0. Takhle dveře neotevřeš, ať tlačíš jakkoli silně.',
                'Ничего! Линия действия силы проходит через петлю (ось вращения) — плечо 0, момент 0. Так дверь не открыть, как сильно ни толкай.',
              )}
            />
          )),
        ]}
      />
    </>
  );
}

/** Síla na desce: působiště, směr, smysl momentu (+1 ↺, −1 ↻, 0 = neotočí). */
interface Fq {
  x: number;
  y: number;
  dx: number;
  dy: number;
  sign: number;
  label: string;
}

const SIGN_FORCES: Fq[] = [
  { x: 300, y: 120, dx: 0, dy: -1, sign: 1, label: 'F_1' }, // vpravo od bodu, nahoru → ↺
  { x: 100, y: 120, dx: 0, dy: -1, sign: -1, label: 'F_2' }, // vlevo od bodu, nahoru → ↻
  { x: 200, y: 40, dx: 1, dy: 0, sign: -1, label: 'F_3' }, // nad bodem, doprava → ↻
  { x: 200, y: 200, dx: 1, dy: 0, sign: 1, label: 'F_4' }, // pod bodem, doprava → ↺
  { x: 300, y: 160, dx: -0.92848, dy: -0.37139, sign: 0, label: 'F_5' }, // z pravého okraje přímo k bodu O → neotočí
  { x: 100, y: 170, dx: 0.8, dy: -0.6, sign: -1, label: 'F_6' }, // šikmo; nositelka míjí O nad ním a míří doprava → ↻
];

/** Karta 3: smysl momentu – ↺ kladný, ↻ záporný, a síla, jejíž nositelka prochází bodem, neotočí vůbec. */
function Sign() {
  const tt = useTr();
  const ox = 200;
  const oy = 120;
  const forces = SIGN_FORCES;
  const turnHint = (q: Fq) =>
    L(
      `Představ si spojnici O s nositelkou jako ručičku připíchnutou v O (zelená čárkovaná): síla [[${q.label}]] tlačí na její konec. Kam se pootočí?`,
      `Представь отрезок от O до линии действия как стрелку, приколотую в O (зелёный пунктир): сила [[${q.label}]] толкает её конец. Куда она повернётся?`,
    );
  const throughHint = (q: Fq) =>
    L(
      `Prodluž nositelku síly [[${q.label}]] – kudy vede? Když prochází přímo bodem O, je rameno nulové.`,
      `Продли линию действия силы [[${q.label}]] — где она проходит? Если прямо через точку O, плечо равно нулю.`,
    );
  // hooky v pevném počtu (6 sil)
  const quizzes = forces.map((q) => {
    const notZero = L(
      `Nositelka [[${q.label}]] bodem O neprochází (je od něj vidět zelená kolmice) – rameno není nulové.`,
      `Линия действия [[${q.label}]] не проходит через O (видно зелёный перпендикуляр от O) — плечо не нулевое.`,
    );
    // eslint-disable-next-line react-hooks/rules-of-hooks
    return useQuiz(
      L(`Kam by desku otočila síla [[${q.label}]]?`, `В какую сторону повернула бы пластину сила [[${q.label}]]?`),
      [
        opt(L('↺ (+)', '↺ (+)'), q.sign === 1, q.sign === 0 ? throughHint(q) : turnHint(q)),
        opt(L('↻ (−)', '↻ (−)'), q.sign === -1, q.sign === 0 ? throughHint(q) : turnHint(q)),
        opt(L('M = 0', 'M = 0'), q.sign === 0, notZero),
      ],
      q.sign === 1
        ? L(`[[${q.label}]] otáčí deskou proti směru hodinových ručiček: ↺, moment kladný.`, `[[${q.label}]] поворачивает пластину против часовой стрелки: ↺, момент положительный.`)
        : q.sign === -1
          ? L(`[[${q.label}]] otáčí deskou po směru hodinových ručiček: ↻, moment záporný.`, `[[${q.label}]] поворачивает пластину по часовой стрелке: ↻, момент отрицательный.`)
          : L(`Nositelka [[${q.label}]] prochází bodem O – rameno 0, moment 0. Deska se neotočí.`, `Линия действия [[${q.label}]] проходит через O — плечо 0, момент 0. Пластина не повернётся.`),
    );
  });
  const k0 = quizzes.findIndex((q) => !q.solved);
  const k = k0 < 0 ? forces.length : k0;
  // po chybě ukáže obrázek „ručičku“ – kolmici z O na nositelku (prozradí rameno, ne smysl)
  const wrong = k < forces.length && quizzes[k].misses > 0;
  return (
    <>
      <Stage
        h={240}
        description={tt.pick(L('Deska přišpendlená v bodě O a šest sil na jejích okrajích', 'Пластина, приколотая в точке O, и шесть сил на её краях'))}
        draw={(b, c) => {
          b.rect(100, 40, 200, 160, c.subtle, c.line, 1.2, 0.8);
          b.circle(ox, oy, 9, withAlpha(c.accent, 0.12), c.accent, 1.5);
          b.line(ox - 13, oy, ox + 13, oy, c.accent, 1.2);
          b.line(ox, oy - 13, ox, oy + 13, c.accent, 1.2);
          b.text(ox + 6, oy - 16, 'O', c.ink, 15, 'start', null, true);
          forces.forEach((q, i) => {
            const cur = i === k;
            const done = i < k;
            const a = cur ? 1 : done ? 0.55 : 0.25;
            if (cur) {
              // nositelka – podle ní se rozhoduje (u F_5 prochází bodem O)
              b.line(q.x - q.dx * 400, q.y - q.dy * 400, q.x + q.dx * 400, q.y + q.dy * 400, c.inkSoft, 0.9, true);
              if (wrong && q.sign !== 0) {
                const s = (ox - q.x) * q.dx + (oy - q.y) * q.dy;
                b.line(ox, oy, q.x + q.dx * s, q.y + q.dy * s, c.accent, 2.0, true);
              }
            }
            const tx = q.x - q.dx * 55;
            const ty = q.y - q.dy * 55;
            b.force(tx, ty, q.x, q.y, c.load, 2.4, 11, cur, a);
            const lx = q.dx < -0.1 ? tx + 6 : tx + 8;
            const ly = ty + (Math.abs(q.dy) > 0.5 ? 14 : -8);
            b.text(lx, ly, null, c.load, 14, 'start', q.label, false, a);
            if (done) {
              const mark = q.sign === 1 ? '↺ +' : q.sign === -1 ? '↻ −' : '0';
              b.text(q.x + (q.x > 250 ? -18 : 14), q.y + (q.y > 180 ? -10 : 18), mark, c.accent, 14, q.x > 250 ? 'end' : 'start', null, true);
            }
          });
          // potvrzení správné odpovědi: oblouk momentu u O (nebo „M = 0“)
          if (k > 0) {
            const q = forces[k - 1];
            if (q.sign !== 0) b.moment(ox, oy, 34, q.sign > 0, c.accent, 2.0);
            else b.text(ox, oy + 36, 'M = 0', c.accent, 14, 'middle', null, true);
          }
        }}
      />
      <CardText
        text={L(
          'Deska je v bodě O přišpendlená a může se kolem něj otáčet. Rozhoduje **nositelka** síly (čárkovaně): na které straně od O vede a kam míří. Moment proti směru hodinových ručiček ↺ počítáme +, po směru ↻ −.',
          'Пластина приколота в точке O и может вокруг неё вращаться. Решает **линия действия** силы (пунктир): с какой стороны от O она проходит и куда направлена. Момент против часовой стрелки ↺ считаем со знаком +, по часовой ↻ — со знаком −.',
        )}
      />
      <Steps
        steps={quizzes.map(
          (q, i): Step => ({
            label: q.question,
            done: q.solved,
            result: () => (
              <>
                {q.Feedback()}
                {i === quizzes.length - 1 && (
                  <>
                    <Note
                      tone="ok"
                      text={L(
                        'Výborně! ↺ bereme kladně, ↻ záporně – stejně jako v trenažéru. A síla, jejíž nositelka prochází bodem, k němu moment nemá (rameno 0) – to se nám u nosníků bude hodit.',
                        'Отлично! ↺ берём со знаком плюс, ↻ — со знаком минус, как и в тренажёре. А у силы, линия действия которой проходит через точку, момента относительно неё нет (плечо 0) — это нам пригодится у балок.',
                      )}
                    />
                    <SmallText
                      text={L('Který smysl je kladný, je jen dohoda – důležité je držet se jí v celém výpočtu.', 'Какое направление считать положительным — лишь договорённость; главное — держаться её во всём расчёте.')}
                    />
                  </>
                )}
              </>
            ),
            controls: () => (
              <>
                {q.Options(3)}
                {!q.solved && q.Feedback()}
              </>
            ),
          }),
        )}
      />
    </>
  );
}

/**
 * Karta 4: dvojice sil – jen otáčí. Ruční kolo ventilu (průměr 0,5 m), každá ruka 20 N. Krátký záběr kolo roztočí,
 * ruce pustí a tření ve ventilu ho zastaví. Součet sil 0, moment ke středu a pak k bodu mimo kolo – vyjde stejně.
 * Nakonec dvojici nahradíme momentem M = F · d (oblouk).
 */
function Couple() {
  const tt = useTr();
  const kit = useKit();
  const cx = 200;
  const cy = 105;
  const rad = 70; // d = 0,5 m ↔ 140 jednotek
  const perM = 280;
  const spin = kit.mut(0);
  const pushing = kit.mut(false);
  const running = kit.mut(false);
  const morph = kit.anim(0); // 0 = síly, 1 = oblouk M
  const point = kit.mut(0);
  const turned = kit.mut(false);
  const tip = kit.mut<number | null>(null);
  const replaced = kit.mut(false);
  const ps = [-0.25, 0.25, 0.75]; // poloha bodu od levé síly [m]
  const xl = cx - rad;
  const xr = cx + rad;
  const nm = tt.pick(L('N·m', 'Н·м'));
  const turnWheel = () => {
    if (running.v) return;
    running.v = true;
    kit.scope.launch(async (job) => {
      let om = 0;
      let time = 0;
      let last = 0;
      // záběr 0,8 s: dvojice (10 N·m) přemáhá tření (4 N·m) – kolo zrychluje; pak ruce pustí a tření ho zastaví
      while (time < 0.8 || om < 0) {
        const now = await job.frame();
        const dt = last === 0 ? 0 : Math.min((now - last) / 1000, 0.05);
        last = now;
        time += dt;
        pushing.v = time < 0.8;
        om += (pushing.v ? -240 : 160) * dt; // °/s², ↺ = záporně
        if (!pushing.v && om > 0) om = 0;
        spin.v = spin.v + om * dt;
      }
      pushing.v = false;
      turned.v = true;
      running.v = false;
    });
  };
  const sum = useQuizStep(
    L('Levá síla 20 N míří dolů, pravá 20 N nahoru. Kolik je jejich součet?', 'Левая сила 20 Н направлена вниз, правая 20 Н — вверх. Чему равна их сумма?'),
    [
      opt(L('40 N', '40 Н'), false, L('Míří proti sobě – ve svislém směru se odečtou.', 'Они направлены навстречу — по вертикали вычитаются.')),
      opt(L('0', '0'), true),
      opt(L('20 N', '20 Н'), false, L('Obě síly jsou stejně velké a míří proti sobě. Co zbude?', 'Обе силы равны и направлены навстречу. Что останется?')),
    ],
    L('Součet je 0 – proto dvojice **nikam neposouvá** (střed kola zůstal na místě), jen otáčí.', 'Сумма равна 0 — поэтому пара **никуда не сдвигает** (центр маховика остался на месте), а только поворачивает.'),
    null,
    3,
    L('Kolik je součet obou sil?', 'Чему равна сумма обеих сил?'),
  );
  const center = useNumStep(
    L('Spočítej moment dvojice ke středu kola (ramena 0,25 m)', 'Найди момент пары относительно центра маховика (плечи 0,25 м)'),
    [numSpec(L('[[M]]', '[[M]]'), 10, nm)],
    L('Obě síly otáčejí kolem středu stejným smyslem ↺: [[M]] = 20 · 0,25 + 20 · 0,25 = 10 N·m.', 'Обе силы поворачивают вокруг центра в одну сторону ↺: [[M]] = 20 · 0,25 + 20 · 0,25 = 10 Н·м.'),
    [
      trapNear(L('To je moment jedné síly. Druhá otáčí stejným smyslem – přičti ji.', 'Это момент одной силы. Вторая крутит в ту же сторону — прибавь её.'), 5),
      trapNear(L('Síly se vyruší v součtu **sil**. Kam ale každá z nich kolo otáčí?', 'Силы уничтожаются в сумме **сил**. Но в какую сторону каждая из них крутит маховик?'), 0),
    ],
    L('Moment každé síly = 20 N × 0,25 m. Obě točí kolem středu stejně (↺).', 'Момент каждой силы = 20 Н × 0,25 м. Обе крутят вокруг центра одинаково (↺).'),
  );
  const tipOpts = [L('větší', 'больше'), L('menší', 'меньше'), L('stejný', 'тот же')];
  const atP = useNumStep(
    L('Spočítej moment dvojice k bodu P (ramena na obrázku)', 'Найди момент пары относительно точки P (плечи на рисунке)'),
    [numSpec(L('[[M]]', '[[M]]'), 10, nm)],
    L(
      'Kolem P otáčí levá síla ↻ a pravá ↺: [[M]] = −20 · 0,25 + 20 · 0,75 = −5 + 15 = 10 N·m – stejně jako ke středu.',
      'Вокруг P левая сила крутит ↻, а правая ↺: [[M]] = −20 · 0,25 + 20 · 0,75 = −5 + 15 = 10 Н·м — как и относительно центра.',
    ),
    [
      trapNear(L('Kolem P neotáčejí obě síly stejně: levá ↻, pravá ↺ – jeden moment odečti.', 'Вокруг P силы крутят по-разному: левая ↻, правая ↺ — один момент вычти.'), 20),
      trapNear(L('To je jen levá síla (20 · 0,25). Přidej i pravou – otáčí opačně.', 'Это только левая сила (20 · 0,25). Добавь и правую — она крутит в обратную сторону.'), 5),
      trapNear(L('To je jen pravá síla (20 · 0,75). Přidej i levou – otáčí opačně.', 'Это только правая сила (20 · 0,75). Добавь и левую — она крутит в обратную сторону.'), 15),
    ],
    L('Každá síla × její rameno k P, se znaménkem: ↺ +, ↻ −.', 'Каждая сила × её плечо до P, со знаком: ↺ +, ↻ −.'),
  );
  // 0 záběr, 1 součet, 2 moment ke středu, 3 tip, 4 moment k P, 5 náhrada momentem, 6 hotovo
  const stage = !turned.v ? 0 : !sum.done ? 1 : !center.done ? 2 : tip.v === null ? 3 : !atP.done ? 4 : !replaced.v ? 5 : 6;
  useLaunched([stage === 5], async (job) => {
    if (stage !== 5) return;
    await job.delay(900);
    await job.run(morph.animateTo(1, tween(1600)));
    replaced.v = true;
  });
  const shownPoint: number | null = stage < 3 ? null : stage < 6 ? 0 : point.v;
  return (
    <>
      <Stage
        h={230}
        description={tt.pick(L('Ruční kolo ventilu otáčené dvojicí sil', 'Маховик вентиля, который поворачивает пара сил'))}
        draw={(b, c) => {
          b.circle(cx, cy, rad, null, c.ink, 5.0);
          for (let i = 0; i < 3; i++) {
            const a = ((spin.v + i * 120 - 90) * Math.PI) / 180;
            b.line(cx, cy, cx + rad * Math.cos(a), cy + rad * Math.sin(a), c.ink, 3.0);
          }
          b.circle(cx, cy, 10, c.subtle, c.ink, 1.5);
          const mv = morph.value;
          const fa = (running.v && !pushing.v ? 0.25 : 1) * (1 - 0.82 * mv);
          b.force(xl, cy - 62, xl, cy - 7, c.load, 2.4, 11, pushing.v, fa);
          b.force(xr, cy + 62, xr, cy + 7, c.load, 2.4, 11, pushing.v, fa);
          b.text(xl - 8, cy - 46, ' = 20 N', c.load, 13, 'end', 'F', false, fa);
          b.text(xr + 8, cy + 52, ' = 20 N', c.load, 13, 'start', 'F', false, fa);
          if (running.v && !pushing.v) b.text(12, 20, tt.pick(L('ruce pustily – tření ve ventilu kolo zastaví', 'руки отпустили — трение в вентиле остановит колесо')), c.inkSoft, 12, 'start');
          if (mv > 0.05) {
            b.moment(cx, cy, 34, true, c.load, 2.6, false, mv);
            b.text(cx, cy - rad - 12, ' = 10 N·m', c.load, 15, 'middle', 'M', true, mv);
          }
          const yd = cy + 105;
          const pt = shownPoint;
          if (pt === null) {
            b.line(xl, yd, xr, yd, c.accent, 1.2);
            b.line(xl, yd - 6, xl, yd + 6, c.accent, 1.2);
            b.line(xr, yd - 6, xr, yd + 6, c.accent, 1.2);
            b.text(cx, yd - 6, ' = 0,5 m', c.accent, 13, 'middle', 'd');
          } else {
            const x = xl + ps[pt] * perM;
            b.circle(x, cy, 8, withAlpha(c.accent, 0.15), c.accent, 1.5);
            b.line(x - 11, cy, x + 11, cy, c.accent, 1.2);
            b.line(x, cy - 11, x, cy + 11, c.accent, 1.2);
            b.text(x, cy + 26, 'P', c.accent, 14, 'middle', null, true);
            b.line(x, cy + 14, x, yd + 6, c.accent, 0.8, true);
            for (const [fxp, ly] of [
              [xl, yd - 14],
              [xr, yd],
            ]) {
              b.line(Math.min(x, fxp), ly, Math.max(x, fxp), ly, c.accent, 1.2);
              b.line(fxp, ly - 5, fxp, ly + 5, c.accent, 1.2);
              b.text((x + fxp) / 2, ly - 4, `${num(Math.abs(fxp - x) / perM)} m`, c.accent, 12);
            }
          }
        }}
      />
      <CardText
        text={L(
          'Ruce na kole ventilu: dvě stejně velké síly (20 N) opačného smyslu na rovnoběžných nositelkách 0,5 m od sebe. Takovým dvěma silám se říká **dvojice sil**.',
          'Руки на маховике вентиля: две равные силы (20 Н) противоположного направления на параллельных линиях действия в 0,5 м друг от друга. Такие две силы называют **парой сил**.',
        )}
      />
      <Steps
        steps={[
          step(
            L('Zaber za kolo oběma rukama a sleduj, co udělá', 'Поверни маховик обеими руками и посмотри, что будет'),
            turned.v,
            () => <ActionButton text={L('Zaber za kolo', 'Повернуть маховик')} enabled={!running.v} onClick={turnWheel} />,
            () => (
              <Note
                tone="info"
                text={L(
                  'Kolo se roztočilo, ale jeho střed zůstal na místě. Když ruce pustily, tření ve ventilu ho zastavilo.',
                  'Маховик закрутился, но его центр остался на месте. Когда руки отпустили, трение в вентиле его остановило.',
                )}
              />
            ),
          ),
          sum,
          center,
          tipStep(L('Bod P posuneme vlevo mimo kolo. Jaký bude moment dvojice k P?', 'Сдвинем точку P влево за маховик. Каким будет момент пары относительно P?'), tipOpts, tip.v, (i) => (tip.v = i)),
          plusResult(atP, () => (
            <Note
              tone={tip.v === 2 ? 'ok' : 'info'}
              text={L(
                (tip.v === 2 ? 'Tip sedí. ' : 'Tip nesedí. ') + 'Ke každému bodu vyjde stejně: [[M]] = [[F]] · [[d]] = 20 · 0,5 = 10 N·m, kde [[d]] je vzdálenost nositelek obou sil.',
                (tip.v === 2 ? 'Прогноз верный. ' : 'Прогноз неверный. ') + 'Относительно любой точки выходит одно и то же: [[M]] = [[F]] · [[d]] = 20 · 0,5 = 10 Н·м, где [[d]] — расстояние между линиями действия сил.',
              )}
            />
          )),
          step(
            L('Sleduj: dvojici nahradíme momentem M a kreslíme ji obloukem', 'Смотри: пару заменяем моментом M и рисуем дугой'),
            replaced.v,
            () => <SmallText text={L('Síly se promění v oblouk…', 'Силы превращаются в дугу…')} />,
            () => (
              <SmallText
                text={L(
                  'Proto moment dvojice přičítáš rovnou jako [[M]], bez ramene – ať počítáš ke kterémukoli bodu. Do součtu sil dvojice nepřispívá: její síly se navzájem vyruší.',
                  'Поэтому момент пары прибавляешь сразу как [[M]], без плеча, — относительно какой бы точки ты ни считал. В сумму сил пара ничего не добавляет: её силы уравновешивают друг друга.',
                )}
              />
            ),
          ),
        ]}
        after={() => {
          const p = ps[point.v];
          const part = (v: number, arm: number, first: boolean) => {
            const glyph = v >= 0 ? '↺' : '↻';
            const sign = v < 0 ? (first ? '−' : ' − ') : first ? '' : ' + ';
            return `${sign}20·${num(arm)} ${glyph}`;
          };
          return (
            <>
              <SmallText text={L('Bod P, ke kterému moment dvojice počítáš:', 'Точка P, относительно которой считаешь момент пары:')} />
              <Chips options={[L('vlevo', 'слева'), L('střed', 'центр'), L('vpravo', 'справа')]} selected={point.v} onPick={(i) => (point.v = i)} />
              <Readout text={`[[M]] = ${part(20 * p, Math.abs(p), true)}${part(20 * (0.5 - p), Math.abs(0.5 - p), false)} = 10 ${nm}`} />
            </>
          );
        }}
      />
    </>
  );
}

/** Karta 5: kontrolní otázka – která síla má k bodu A největší moment? */
function Check() {
  const tt = useTr();
  const y = 110;
  const xa = 50;
  const u = 65;
  const xe = xa + 4 * u;
  const q = useQuizStep(
    L('Která síla má k bodu A **největší moment** (co do velikosti)?', 'У какой силы **самый большой по величине** момент относительно A?'),
    [
      opt(
        L('[[F_1]]', '[[F_1]]'),
        false,
        L('[[F_1]] je největší síla, ale jak dlouhé má rameno? Spočítej [[F]] · [[r]] pro [[F_1]] i [[F_2]].', '[[F_1]] — самая большая сила, но какое у неё плечо? Посчитай [[F]] · [[r]] для [[F_1]] и [[F_2]].'),
      ),
      opt(L('[[F_2]]', '[[F_2]]'), true),
      opt(
        L('[[F_3]]', '[[F_3]]'),
        false,
        L('[[F_3]] je sice nejdál, ale míří podél nosníku – její nositelka prochází bodem A. Jaké má rameno?', '[[F_3]] дальше всех, но направлена вдоль балки — её линия действия проходит через точку A. Какое у неё плечо?'),
      ),
    ],
    L(
      '[[F_2]]: velikost 4 · 3 = 12 kNm, [[F_1]]: 10 · 1 = 10 kNm. Obě otáčejí kolem A po směru hodinových ručiček ↻, takže se znaménkem: [[M_1]] = −10 kNm, [[M_2]] = −12 kNm. Nositelka [[F_3]] prochází bodem A – rameno 0, moment 0. Ani největší síla, ani nejvzdálenější působiště nerozhoduje – rozhoduje součin.',
      '[[F_2]]: по величине 4 · 3 = 12 кН·м, [[F_1]]: 10 · 1 = 10 кН·м. Обе поворачивают вокруг A по часовой стрелке ↻, поэтому со знаком: [[M_1]] = −10 кН·м, [[M_2]] = −12 кН·м. Линия действия [[F_3]] проходит через точку A — плечо 0, момент 0. Решает не самая большая сила и не самая дальняя точка, а произведение.',
    ),
    L(
      'Moment = síla × rameno, tedy kolmá vzdálenost bodu A od nositelky síly. Vzdálenost po nosníku je ramenem jen u sil kolmých k nosníku.',
      'Момент = сила × плечо, то есть расстояние по перпендикуляру от точки A до линии действия силы. Расстояние вдоль балки — плечо только для сил, перпендикулярных балке.',
    ),
    3,
  );
  return (
    <>
      <Stage
        h={220}
        description={tt.pick(L('Nosník se třemi silami a bodem A', 'Балка с тремя силами и точкой A'))}
        draw={(b, c) => {
          b.beam(xa - 10, xe, y);
          b.support('pin', xa, y, 'A', true);
          b.force(xa + u, y - 82, xa + u, y - 6, c.load);
          b.text(xa + u + 8, y - 64, ' = 10 kN', c.load, 14, 'start', 'F_1');
          b.force(xa + 3 * u, y - 58, xa + 3 * u, y - 6, c.load);
          b.text(xa + 3 * u + 8, y - 44, ' = 4 kN', c.load, 14, 'start', 'F_2');
          // vodorovná síla tlačí na konec nosníku v jeho ose
          b.force(xe + 62, y, xe + 1, y, c.load);
          b.text(xe + 31, y - 14, ' = 8 kN', c.load, 14, 'middle', 'F_3');
          // kóty
          const yd = y + 70;
          b.line(xa, yd, xe, yd, c.ink, 0.9);
          for (const x of [xa, xa + u, xa + 3 * u, xe]) b.line(x - 4, yd + 4, x + 4, yd - 4, c.ink, 1.3);
          b.text(xa + u / 2, yd - 5, '1 m', c.ink, 13);
          b.text(xa + 2 * u, yd - 5, '2 m', c.ink, 13);
          b.text(xa + 3.5 * u, yd - 5, '1 m', c.ink, 13);
        }}
      />
      <CardText text={L('Na nosník působí tři síly. Porovnej jejich momenty k bodu A.', 'На балку действуют три силы. Сравни их моменты относительно точки A.')} />
      <Steps steps={[q]} />
    </>
  );
}

export const cards = [Wrench, PerpendicularArm, Door, Sign, Couple, Check];
