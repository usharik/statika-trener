/*
 * Lekce 1 – Síla je šipka. Síla je vektor: velikost, směr (sklon nositelky), smysl a působiště – puk na ledě
 * jede po nositelce. Působí i tam, kde se nic nehýbe: deska stolu se prohne a pruží zpátky. Siloměr a jednotka
 * kN ≈ tíha 100 kg. (Přepis z aplikace: learn/Lesson1.kt.)
 */
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
  alpha,
  clamp,
  dist,
  num,
  opt,
  spring,
  step,
  tipStep,
  tween,
  useKit,
  useLaunched,
  useQuizStep,
  useTr,
  type Job,
  type PrimBuilder,
  type Colors,
} from '../kit';

/** Bedna jako obdélník se středem (cx, cy). */
function box(b: PrimBuilder, cx: number, cy: number, w: number, h: number, c: Colors, a = 1) {
  b.rect(cx - w / 2, cy - h / 2, w, h, c.subtle, c.ink, 1.6, a);
  // dvě prkna, ať je to poznat jako bedna
  b.line(cx - w / 2, cy, cx + w / 2, cy, c.inkSoft, 1.0, false, a);
}

/** Kolik jednotek se dá jít od (px, py) ve směru (ux, uy), než se narazí na okraj obrázku. */
function room(px: number, py: number, ux: number, uy: number, x0: number, x1: number, y0: number, y1: number): number {
  let t = 1e9;
  if (ux > 1e-9) t = Math.min(t, (x1 - px) / ux);
  if (ux < -1e-9) t = Math.min(t, (x0 - px) / ux);
  if (uy > 1e-9) t = Math.min(t, (y1 - py) / uy);
  if (uy < -1e-9) t = Math.min(t, (y0 - py) / uy);
  return t;
}

interface Goal {
  x: number;
  y: number;
  name: string;
}

/** Dojezd puku s třením: rychle vyrazí a plynule zpomalí. */
const Slide = (f: number) => 1 - (1 - f) * (1 - f);

/**
 * Karta 0: puk na ledě. Šipka síly = jak do puku strčíš. Tři branky, každá chce změnit jinou vlastnost
 * síly: A směr (sklon), B smysl (hrot na druhou stranu), C velikost (delší šipka = puk jede dál).
 */
function Puck() {
  const t = useTr();
  const kit = useKit();
  const px = 150;
  const py = 125;
  const arrowPer = 14; // délka šipky na 10 N
  const runPer = 24; // dojezd puku na 10 N strčení
  const goals: Goal[] = kit.remember(() => [
    // každá branka chce oproti výchozí šipce (30 N →) změnit jen jednu vlastnost
    { x: px + 3 * runPer * Math.cos(-Math.PI / 4), y: py + 3 * runPer * Math.sin(-Math.PI / 4), name: 'A' }, // jen směr
    { x: px - 3 * runPer, y: py, name: 'B' }, // jen smysl
    { x: px + 8 * runPer, y: py, name: 'C' }, // jen velikost
  ]);
  const kn = kit.mut(3.0);
  const ang = kit.mut(0.0); // stupně, osa y dolů
  const dragging = kit.mut(false);
  const grabOff = kit.mut(0.0);
  const hit = kit.list([false, false, false]);
  // výsledek posledního strčení: null = ještě nic, -1 = vedle, jinak index branky
  const last = kit.mut<number | null>(null);
  // proč vedle: 0 = jiný směr, 1 = krátké, 2 = dlouhé
  const why = kit.mut(0);
  const flying = kit.mut(false);
  const run = kit.anim(0);
  // nový puk se po strčení objeví na startu; ten starý zůstane slabě vidět tam, kde zastavil
  const appear = kit.anim(1);
  const ghost = kit.mut<[number, number] | null>(null);
  // ukázka na začátku: šipka 30 N doprava pošle puk do ukázkové branky
  const demo = kit.mut(true);
  const demoSeen = kit.mut(false);
  const demoGoal = { x: px + 3 * runPer, y: py, name: '' };
  const scope = kit.scope;
  const ux = () => Math.cos((ang.v * Math.PI) / 180);
  const uy = () => Math.sin((ang.v * Math.PI) / 180);
  const maxKn = (a: number) => Math.min((room(px, py, Math.cos((a * Math.PI) / 180), Math.sin((a * Math.PI) / 180), 16, 384, 16, 204) - 18) / arrowPer, 9.5);
  // každý krok mění jen jednu vlastnost síly: A směr, B smysl (velikost 30 N), C velikost (směr →); pak volně
  const stepNo = hit.indexOfFirst((x) => !x);
  const set = (a: number, k: number) => {
    if (flying.v || demo.v) return;
    ang.v = stepNo === 2 ? 0 : a;
    kn.v = stepNo === 0 || stepNo === 1 ? 3 : Math.max(Math.min(Math.round(k * 2) / 2, Math.floor(maxKn(ang.v) * 2) / 2), 0.5);
    last.v = null;
  };
  /** kam puk dojede: rovně po nositelce síly, nejdál k mantinelu */
  const end = (): [number, number] => {
    const d = Math.min(kn.v * runPer, room(px, py, ux(), uy(), 22, 378, 22, 198));
    return [px + ux() * d, py + uy() * d];
  };
  const slide = async (): Promise<[number, number]> => {
    await run.snapTo(0);
    // rovnoměrné zpomalování třením: doba dojezdu roste s odmocninou dráhy
    await run.animateTo(1, tween(Math.round(420 * Math.sqrt(kn.v)), Slide));
    return end();
  };
  const nextPuck = async (at: [number, number]) => {
    await scope.delay(600);
    ghost.v = at;
    await appear.snapTo(0);
    await run.snapTo(0);
    await appear.animateTo(1, tween(350));
  };
  const shoot = () => {
    if (flying.v || demo.v) return;
    flying.v = true;
    last.v = null;
    scope.launch(async () => {
      const [ex, ey] = await slide();
      const g = goals.findIndex((it) => Math.hypot(it.x - ex, it.y - ey) < 17);
      if (g >= 0) hit.set(g, true);
      else {
        // míří šipka na právě hledanou branku (stejný směr i smysl)?
        const gg = goals[stepNo];
        const aim = gg && ((gg.x - px) * ux() + (gg.y - py) * uy()) / Math.hypot(gg.x - px, gg.y - py) > 0.985 ? gg : null;
        why.v = !aim ? 0 : Math.hypot(ex - px, ey - py) < Math.hypot(aim.x - px, aim.y - py) ? 1 : 2;
      }
      last.v = g;
      await nextPuck([ex, ey]);
      // po trefě A a B začíná další krok znovu od výchozí šipky 30 N →
      if (g === 0 || g === 1) {
        ang.v = 0;
        kn.v = 3;
      }
      flying.v = false;
    });
  };
  useLaunched([], async () => {
    await scope.delay(900);
    flying.v = true;
    const at = await slide();
    demoSeen.v = true;
    await nextPuck(at);
    flying.v = false;
    demo.v = false;
  });
  const tipX = px + kn.v * arrowPer * ux();
  const tipY = py + kn.v * arrowPer * uy();
  const hx = tipX + 16 * ux();
  const hy = tipY + 16 * uy();
  const miss = () =>
    last.v === -1 ? (
      <Note
        tone="nudge"
        text={
          why.v === 0
            ? L('Vedle – šipka nemíří na branku. Puk jede po nositelce tam, kam ukazuje hrot.', 'Мимо — стрелка не смотрит на ворота. Шайба едет по линии действия туда, куда показывает остриё.')
            : why.v === 1
              ? L('Směr je dobrý, ale puk nedojel – síla je malá. Prodluž šipku.', 'Направление верное, но шайба не доехала — сила мала. Удлини стрелку.')
              : L('Směr je dobrý, ale puk přejel – síla je velká. Zkrať šipku.', 'Направление верное, но шайба проскочила — сила велика. Укороти стрелку.')
        }
      />
    ) : null;
  const dirs = () => <Chips options={[L('→', '→'), L('↗', '↗'), L('↑', '↑'), L('←', '←'), L('↓', '↓')]} onPick={(i) => set([0, -45, -90, 180, 90][i], kn.v)} />;
  const size = () => (
    <div className="krow">
      <div className="grow">
        <Chips options={[L('− 5 N', '− 5 Н'), L('+ 5 N', '+ 5 Н')]} onPick={(i) => set(ang.v, kn.v + (i === 0 ? -0.5 : 0.5))} />
      </div>
      <Readout text={`[[F]] = ${num(kn.v * 10, 0)} N`} />
    </div>
  );
  const shootBtn = () => <ActionButton text={L('Strč do puku', 'Толкнуть шайбу')} enabled={!flying.v} onClick={shoot} />;
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Puk na ledě, šipka síly a tři branky A, B, C', 'Шайба на льду, стрелка силы и трое ворот A, B, C'))}
        onDrag={(p, start) => {
          if (start) {
            const l = Math.hypot(hx - px, hy - py);
            const along = clamp(((p.x - px) * (hx - px) + (p.y - py) * (hy - py)) / (l * l), 0.35, 1.0);
            dragging.v = !flying.v && dist(p, px + along * (hx - px), py + along * (hy - py)) < 45;
            grabOff.v = l - Math.hypot(p.x - px, p.y - py);
          }
          if (dragging.v && dist(p, px, py) > 15) {
            const a = Math.round((Math.atan2(p.y - py, p.x - px) * 180) / Math.PI / 15) * 15;
            set(a, (Math.hypot(p.x - px, p.y - py) + grabOff.v - 16) / arrowPer);
          }
        }}
        onDragEnd={() => (dragging.v = false)}
        onTap={(p) => {
          if (dist(p, px, py) < 26) shoot();
        }}
        draw={(b, c) => {
          // kluziště
          b.rect(10, 10, 380, 200, alpha(c.react, 0.05), c.inkSoft, 1.2);
          if (demo.v) {
            // ukázková branka (jen během ukázky)
            b.circle(demoGoal.x, demoGoal.y, 17, alpha(c.inkSoft, demoSeen.v ? 0.25 : 0.06), c.inkSoft, 1.4);
            b.text(demoGoal.x, demoGoal.y + 36, t.pick(L('ukázka', 'пример')), c.inkSoft, 12);
            b.text(22, 30, t.pick(L('Ukázka: 30 N doprava → puk dojede 3 dílky doprava', 'Пример: 30 Н вправо → шайба проедет 3 деления вправо')), c.ink, 13, 'start');
          }
          const target = demo.v ? -1 : hit.indexOfFirst((x) => !x);
          goals.forEach((g, i) => {
            if (i === target && !flying.v) b.circle(g.x, g.y, 23, null, c.accent, 1.0, 0.6);
            b.circle(g.x, g.y, 17, alpha(c.accent, hit.get(i) ? 0.28 : 0.07), c.accent, i === target ? 2.6 : 1.8);
            b.text(g.x, g.y + 5, hit.get(i) ? '✓' : g.name, c.accent, 14, 'middle', null, true);
          });
          const [ex, ey] = end();
          const f = run.value;
          const bx = px + (ex - px) * f;
          const by = py + (ey - py) * f;
          if (flying.v) {
            // stopa puku – jede rovně po nositelce síly
            b.line(px, py, bx, by, c.inkSoft, 1.0, true);
            // tření brzdí proti pohybu (hokejka už netlačí)
            if (run.value >= 0.08 && run.value <= 0.97) b.force(bx - ux() * 12, by - uy() * 12, bx - ux() * 30, by - uy() * 30, c.inkSoft, 1.8, 7);
            // šipka zůstane slabě vidět na startu – příčina pohybu
            b.force(px, py, tipX, tipY, c.load, 3.0, 11, false, 0.35);
          } else {
            const back = room(px, py, -ux(), -uy(), 12, 388, 12, 208);
            const fwd = room(px, py, ux(), uy(), 12, 388, 12, 208);
            b.line(px - ux() * back, py - uy() * back, px + ux() * fwd, py + uy() * fwd, c.inkSoft, 1.0, true);
            // dílky na nositelce: každých 10 N = jeden dílek dojezdu
            for (let k = 1; k * runPer <= fwd; k++) b.circle(px + ux() * k * runPer, py + uy() * k * runPer, 2.2, c.inkSoft, null, 1.5, 0.7);
            b.force(px, py, tipX, tipY, c.load, 3.0);
            let nx = -uy();
            let ny = ux();
            if (ny > 0 || (Math.abs(ny) < 1e-6 && nx > 0)) {
              nx = -nx;
              ny = -ny;
            }
            const anc = nx > 0.3 ? 'start' : nx < -0.3 ? 'end' : 'middle';
            b.text((px + tipX) / 2 + nx * 20, (py + tipY) / 2 + ny * 20 + 5, ` = ${num(kn.v * 10, 0)} N`, c.load, 15, anc, 'F');
            b.handle(hx, hy, dragging.v);
          }
          if (ghost.v) b.circle(ghost.v[0], ghost.v[1], 10, c.ink, null, 1.5, 0.18);
          const pa = run.value === 0 ? appear.value : 1;
          b.circle(bx, by, 10, c.ink, null, 1.5, pa);
          b.circle(bx, by, 4, c.inkSoft, null, 1.5, pa);
          b.rect(0, 212, 400, 18, c.paper);
          b.text(16, 226, t.pick(L('čárkovaně: nositelka – přímka, na které síla leží', 'пунктир: линия действия — прямая, на которой лежит сила')), c.inkSoft, 12, 'start');
        }}
      />
      <CardText
        text={L(
          'Síla není jen číslo – záleží i na tom, **kam** míří. Proto ji kreslíme **šipkou**: délka = **velikost**, sklon nositelky = **směr**, hrot = **smysl**, tečka = **působiště**.',
          'Сила — не просто число: важно и то, **куда** она направлена. Поэтому её рисуют **стрелкой**: длина — **величина** (velikost), наклон линии действия — **наклон** (směr), остриё — **направление** (smysl), точка — **точка приложения** (působiště).',
        )}
      />
      <SmallText
        text={L(
          'Hokejka tlačí vždy po stejně krátké dráze, takže co 10 N, to jeden dílek dojezdu puku.',
          'Клюшка всегда толкает на одном и том же коротком пути, поэтому каждые 10 Н — одно деление пути шайбы.',
        )}
      />
      <Steps
        steps={[
          step(L('Sleduj ukázku: 30 N doprava pošle puk o 3 dílky', 'Смотри пример: 30 Н вправо — шайба проедет 3 деления'), demoSeen.v && !demo.v, () => (
            <SmallText text={L('Puk jede po nositelce síly, tam kam míří hrot…', 'Шайба едет по линии действия силы, куда смотрит остриё…')} />
          )),
          step(
            L('Branka **A** je šikmo: natoč šipku na ni a strč do puku', 'Ворота **A** — наискосок: поверни стрелку на них и толкни шайбу'),
            hit.get(0),
            () => (
              <>
                {dirs()}
                {shootBtn()}
                {miss()}
              </>
            ),
            () => <Note tone="ok" text={L('Trefa! **Směr** síly určuje, po které přímce puk pojede.', 'Попал! **Наклон** силы определяет, по какой прямой поедет шайба.')} />,
          ),
          step(
            L('Branka **B** je za pukem: otoč **smysl** síly a strč', 'Ворота **B** — позади шайбы: разверни **направление** силы и толкни'),
            hit.get(1),
            () => (
              <>
                <Chips options={[L('Otoč smysl ⇄', 'Развернуть ⇄')]} onPick={() => set((ang.v + 180) % 360, kn.v)} />
                {shootBtn()}
                {miss()}
              </>
            ),
            () => <Note tone="ok" text={L('Trefa! Stejná nositelka, opačný **smysl** – puk jede opačně.', 'Попал! Та же линия действия, обратное **направление** — шайба едет в обратную сторону.')} />,
          ),
          step(
            L('Branka **C** je daleko: nastav **velikost** síly a strč', 'Ворота **C** — далеко: подбери **величину** силы и толкни'),
            hit.get(2),
            () => (
              <>
                {size()}
                {shootBtn()}
                {miss()}
              </>
            ),
            () => (
              <>
                <Note tone="ok" text={L('Trefa! Větší **velikost** síly = větší rychlost, puk dojede dál.', 'Попал! Больше **величина** силы — больше скорость, шайба уезжает дальше.')} />
                <SmallText
                  text={L(
                    'Tři trefy, tři vlastnosti síly: **směr**, **smysl**, **velikost**. Čtvrtá – **působiště** – přijde na řadu o dvě karty dál.',
                    'Три попадания — три свойства силы: **наклон**, **направление**, **величина**. Четвёртое — **точка приложения** — через две карточки.',
                  )}
                />
              </>
            ),
          ),
        ]}
        after={() => (
          <>
            {dirs()}
            {size()}
            {shootBtn()}
          </>
        )}
      />
    </>
  );
}

/**
 * Karta 1: proč stůl tlačí. Deska se pod bednou prohne (hodně zvětšeně) a pruží zpátky – čím víc prohnutá,
 * tím víc tlačí. Rozkmitá se a ustálí, až je síla stolu stejná jako tíha.
 */
function TableSpring() {
  const t = useTr();
  const kit = useKit();
  const guess = kit.mut<number | null>(null);
  const crates = kit.mut(0);
  const settled = kit.mut(0); // kolik beden leželo, když se deska naposled ustálila
  const seenOne = kit.mut(false);
  const seenTwo = kit.mut(false);
  const sag = kit.anim(0); // prohnutí desky vyjádřené silou stolu v kN
  const drop = kit.anim(1); // pád poslední bedny: 0 = nahoře, 1 = leží
  const running = kit.remember<{ job: Job | null }>(() => ({ job: null }));
  const guess2 = kit.mut<number | null>(null);
  const place = (n: number) => {
    const added = n > crates.v;
    crates.v = n;
    settled.v = -1;
    running.job?.cancel();
    running.job = kit.scope.launch(async (job) => {
      if (added) {
        await drop.snapTo(0);
        await job.run(drop.animateTo(1, tween(320, (x) => x * x)));
      }
      await job.run(sag.animateTo(n, spring(0.22, 45)));
      settled.v = n;
      if (n === 1) seenOne.v = true;
      if (n === 2) seenTwo.v = true;
    });
  };
  const top = 150; // horní plocha desky bez zatížení
  const x0 = 90;
  const x1 = 310;
  const perKn = 10; // prohnutí na 1 kN (zvětšené)
  const arrowPer = 26;
  const forces = () => <Readout text={`[[N]] = ${num(Math.max(sag.value, 0), 1)} kN · [[G]] = ${num(crates.v, 1)} kN`} />;
  return (
    <>
      <Stage
        h={240}
        description={t.pick(L('Stůl, jehož deska se pod bednami prohýbá', 'Стол, столешница которого прогибается под ящиками'))}
        draw={(b, c) => {
          const s = sag.value * perKn;
          const y = (x: number) => top + s * Math.sin((Math.PI * (x - x0)) / (x1 - x0));
          const floor = 228;
          b.line(30, floor, 370, floor, c.ink, 1.5);
          b.rect(x0 + 10, top + 10, 8, floor - top - 10, c.beam, c.ink, 1.4);
          b.rect(x1 - 18, top + 10, 8, floor - top - 10, c.beam, c.ink, 1.4);
          // prohnutá deska: horní a dolní okraj
          const xs = Array.from({ length: 21 }, (_, i) => x0 + ((x1 - x0) * i) / 20);
          b.poly([...xs.map((x): [number, number] => [x, y(x)]), ...[...xs].reverse().map((x): [number, number] => [x, y(x) + 10])], c.beam, c.ink, 1.6);
          // bedny na středu desky (poslední při pokládání padá)
          const bh = 34;
          const mid = y(200);
          for (let i = 0; i < crates.v; i++) {
            const fallOff = i === crates.v - 1 ? (1 - drop.value) * 70 : 0;
            box(b, 200, mid - bh / 2 - i * bh - fallOff, 60, bh, c);
            b.text(200, mid - bh / 2 - i * bh - fallOff + 13, '100 kg', c.inkSoft, 10);
          }
          if (crates.v > 0 && drop.value > 0.99) {
            // tíha beden (červeně) a síla stolu v místě dotyku (modře) na jedné svislé nositelce
            const g = crates.v;
            const stackTop = mid - crates.v * bh;
            b.force(200, stackTop - g * arrowPer - 4, 200, stackTop - 4, c.load, 2.6);
            b.text(192, stackTop - (g * arrowPer) / 2, ` = ${num(g, 1)} kN`, c.load, 13, 'end', 'G');
            const n = Math.max(sag.value, 0);
            if (n > 0.05) {
              b.force(200, mid + n * arrowPer, 200, mid, c.react, 2.6, 11, true);
              b.text(208, mid + 12 + (n * arrowPer) / 2, ` = ${num(n, 1)} kN`, c.react, 13, 'start', 'N');
            }
          }
          b.text(392, 20, t.pick(L('prohnutí zvětšeno asi 1000×', 'прогиб увеличен примерно в 1000 раз')), c.inkSoft, 12, 'end');
        }}
      />
      <CardText text={L('Bedna leží na stole a nehýbe se. Odkud stůl „ví“, jak silně ji má držet?', 'Ящик лежит на столе и не двигается. Откуда стол «знает», с какой силой его держать?')} />
      <Steps
        steps={[
          tipStep(L('Tipni si: tlačí stůl na ležící bednu nějakou silou?', 'Как думаешь: давит ли стол на лежащий ящик какой-то силой?'), [L('Ano', 'Да'), L('Ne, nic se nehýbe', 'Нет, ничего не движется')], guess.v, (i) => (guess.v = i)),
          step(
            L('Polož na stůl bednu (100 kg ≈ 1 kN) a sleduj desku', 'Положи на стол ящик (100 кг ≈ 1 кН) и следи за столешницей'),
            seenOne.v,
            () => (
              <>
                {forces()}
                <ActionButton text={L('Polož bednu', 'Положить ящик')} enabled={crates.v === 0} onClick={() => place(1)} />
              </>
            ),
            () => (
              <>
                {guess.v === 1 && (
                  <Note
                    tone="info"
                    text={L(
                      'A přece tlačí: deska se prohnula a pruží zpátky nahoru – jinak by bedna propadla.',
                      'И всё-таки давит: столешница прогнулась и пружинит обратно вверх — иначе ящик провалился бы.',
                    )}
                  />
                )}
                <SmallText
                  text={L(
                    'Deska funguje jako **pružina**: čím víc se prohne, tím víc tlačí. Chvíli kmitá (pak tlačí víc i míň než tíha) a ustálí se, až [[N]] = [[G]]. Síla působí, i když se nic nehýbe.',
                    'Столешница работает как **пружина**: чем сильнее прогиб, тем сильнее она давит. Сначала колеблется (сила то больше, то меньше веса) и успокаивается, когда [[N]] = [[G]]. Сила действует, даже когда ничего не движется.',
                  )}
                />
              </>
            ),
          ),
          tipStep(
            L('Přibude druhá bedna. Jak velkou silou [[N]] bude stůl tlačit?', 'Добавим второй ящик. С какой силой [[N]] будет давить стол?'),
            [L('1 kN – pořád stejně', '1 кН — как раньше'), L('2 kN', '2 кН'), L('víc než 2 kN', 'больше 2 кН')],
            guess2.v,
            (i) => (guess2.v = i),
          ),
          step(
            L('Polož druhou bednu a ověř svůj tip', 'Положи второй ящик и проверь свой прогноз'),
            seenTwo.v,
            () => (
              <>
                {forces()}
                <ActionButton text={L('Polož druhou bednu', 'Положить второй ящик')} enabled={crates.v === 1 && settled.v === 1} onClick={() => place(2)} />
              </>
            ),
            () => (
              <Note
                tone={guess2.v === 1 ? 'ok' : 'info'}
                text={L(
                  (guess2.v === 1 ? 'Správně. ' : '') +
                    'Dvojnásobná tíha → dvojnásobné prohnutí → stůl tlačí 2 kN. Podpory nosníků se chovají stejně.' +
                    (guess2.v === 2 ? ' Chvíli opravdu tlačil víc – když deska překmitla. Ustálí se ale přesně na 2 kN.' : ''),
                  (guess2.v === 1 ? 'Верно. ' : '') +
                    'Вдвое больше вес → вдвое больше прогиб → стол давит с силой 2 кН. Опоры балок ведут себя так же.' +
                    (guess2.v === 2 ? ' На мгновение он и правда давил сильнее — пока столешница колебалась. Но успокаивается ровно на 2 кН.' : ''),
                )}
              />
            ),
          ),
        ]}
      />
    </>
  );
}

/** Rohy obdélníku w × h se středem (cx, cy) otočeného o úhel a (rad, osa y dolů). */
function plateCorners(cx: number, cy: number, w: number, h: number, a: number): [number, number][] {
  const ca = Math.cos(a);
  const sa = Math.sin(a);
  return [
    [-1, -1],
    [1, -1],
    [1, 1],
    [-1, 1],
  ].map(([sx, sy]) => {
    const x = (sx * w) / 2;
    const y = (sy * h) / 2;
    return [cx + x * ca - y * sa, cy + x * sa + y * ca];
  });
}

/** Poloha desky v jednom záblesku stroboskopu. */
interface Pose {
  x: number;
  y: number;
  a: number;
}

/**
 * Karta 2: deska na vzduchovém stole (pohled shora, bez tření). Krátké strčení stejnou silou:
 * zezadu, nebo lanem zepředu po téže nositelce → úplně stejná stopa; po rovnoběžné nositelce mimo těžiště
 * → těžiště jede stejně, ale deska se navíc roztočí (moment).
 */
function LineOfAction() {
  const t = useTr();
  const kit = useKit();
  const x0 = 92; // start těžiště
  const y0 = 112;
  const pw = 92;
  const ph = 50;
  const off = 15; // posun rovnoběžné nositelky
  const acc = 230; // zrychlení během strčení (jednotek/s²)
  const push = 0.5; // jak dlouho síla působí (s)
  const endT = 2.2;
  const guessRope = kit.mut<number | null>(null); // tip pro lano zepředu
  const guess = kit.mut<number | null>(null); // tip pro jinou nositelku
  const time = kit.mut(-1.0); // -1 = deska stojí na startu
  const alphaV = kit.mut(1);
  const runMode = kit.mut(0);
  const ghosts = kit.list<Pose>([]);
  const prev = kit.list<Pose>([]); // stopa minulého pokusu (šedě)
  const done = kit.list([false, false, false]);
  const running = time.v >= 0;
  // režim určuje aktuální krok: 0 tlačí zezadu, 1 táhne lanem zepředu, 2 jiná (rovnoběžná) nositelka
  const mode: number | null = !done.get(0) ? 0 : !done.get(1) ? 1 : 2;
  /** stav desky v čase s pro daný režim: posun s(t), úhel θ(t) */
  const pose = (s: number, m: number): Pose => {
    const tp = Math.min(s, push);
    const x = 0.5 * acc * tp * tp + acc * push * (s - tp);
    // úhlové zrychlení: α = F·d / I, I = m(w² + h²)/12 → α = a·d·12/(w² + h²)
    const al = m === 2 ? (acc * off * 12) / (pw * pw + ph * ph) : 0;
    const th = 0.5 * al * tp * tp + al * push * (s - tp);
    return { x: x0 + x, y: y0, a: th };
  };
  const run = () => {
    const m = mode;
    if (m === null) return;
    if (time.v >= 0 || (m === 1 && guessRope.v === null) || (m === 2 && guess.v === null)) return;
    runMode.v = m;
    kit.scope.launch(async (job) => {
      ghosts.clear();
      alphaV.v = 1;
      let s = 0;
      let lastT = 0;
      let next = 0;
      time.v = 0;
      while (s < endT) {
        const now = await job.frame();
        const dt = lastT === 0 ? 0 : (now - lastT) / 1000;
        lastT = now;
        s = Math.min(endT, s + dt);
        time.v = s;
        if (s >= next) {
          ghosts.add(pose(next, m));
          next += 0.25;
        }
      }
      // pokus skončil: deska zmizí, její stopa zůstane šedě pro srovnání, na startu se objeví nová
      for (let i = 1; i <= 8; i++) {
        alphaV.v = 1 - i / 8;
        await job.delay(25);
      }
      done.set(m, true);
      prev.setAll([...ghosts.v]);
      ghosts.clear();
      time.v = -1;
      for (let i = 1; i <= 8; i++) {
        alphaV.v = i / 8;
        await job.delay(25);
      }
    });
  };
  const tipOpts = (cs: string[], ru: string[]) => cs.map((x, i) => L(x, ru[i]));
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Deska na vzduchovém stole shora, strčená silou po různých nositelkách', 'Пластина на воздушном столе сверху, толчок силой по разным линиям действия'))}
        onTap={(p) => {
          if (!running && dist(p, x0, y0) < 50) run();
        }}
        draw={(b, c) => {
          b.rect(10, 10, 380, 200, alpha(c.react, 0.04), c.inkSoft, 1.2);
          // otvory vzduchového stolu
          for (let gx = 0; gx <= 18; gx++) for (let gy = 0; gy <= 9; gy++) b.circle(20 + gx * 20, 20 + gy * 20, 0.9, c.inkSoft, null, 1.5, 0.35);
          b.text(16, 224, t.pick(L('pohled shora · vzduchový stůl, bez tření', 'вид сверху · воздушный стол, без трения')), c.inkSoft, 12, 'start');
          const m = running ? runMode.v : (mode ?? 0);
          const lineY = m === 2 ? y0 - off : y0;
          // stopa minulého pokusu šedě, aktuálního barevně; křížek = těžiště
          for (const g of prev.v) {
            b.poly(plateCorners(g.x, g.y, pw, ph, g.a), null, c.inkSoft, 1.0, 0.35);
            b.circle(g.x, g.y, 2.5, c.inkSoft, null, 1.5, 0.6);
          }
          const col = runMode.v === 2 ? c.derived : c.react;
          for (const g of ghosts.v) {
            b.poly(plateCorners(g.x, g.y, pw, ph, g.a), null, col, 1.2, 0.45);
            b.circle(g.x, g.y, 2.5, col);
          }
          const now = running ? pose(time.v, runMode.v) : { x: x0, y: y0, a: 0 };
          if (alphaV.v > 0.01) {
            b.poly(plateCorners(now.x, now.y, pw, ph, now.a), c.subtle, c.ink, 1.6, alphaV.v);
            b.line(now.x - 5, now.y, now.x + 5, now.y, c.ink, 1.4, false, alphaV.v);
            b.line(now.x, now.y - 5, now.x, now.y + 5, c.ink, 1.4, false, alphaV.v);
            if (!running) b.text(now.x, now.y - 8, 'T', c.inkSoft, 11);
          }
          // nositelka a síla: kreslí se jen, dokud síla působí
          const acting = !running || time.v < push;
          if (mode !== null || running) {
            b.line(10, lineY, 390, lineY, c.inkSoft, 1.0, true);
            if (acting && alphaV.v > 0.5) {
              const left = now.x - pw / 2;
              const right = now.x + pw / 2;
              if (m === 1) {
                // původní šipka zezadu slabě: síla se jen posunula po své nositelce
                if (!running) b.force(left - 56, lineY, left, lineY, c.load, 2.0, 11, false, 0.25, true);
                b.force(right, lineY, right + 56, lineY, c.load, 3.0);
                b.text(right + 28, lineY - 10, t.pick(L('lano', 'трос')), c.load, 11);
              } else b.force(left - 56, lineY, left, lineY, c.load, 3.0);
              b.text(m === 1 ? right + 60 : left - 60, lineY + 5, null, c.load, 15, m === 1 ? 'start' : 'end', 'F');
              if (m === 2) {
                // rameno d mezi nositelkou a těžištěm
                b.line(now.x + 14, lineY, now.x + 14, now.y, c.derived, 1.2);
                b.text(now.x + 19, (lineY + now.y) / 2 + 4, 'd', c.derived, 12, 'start');
              }
            }
          }
        }}
      />
      <CardText
        text={L(
          'Deska klouže po **vzduchovém stole** bez tření. Pokaždé do ní krátce strčíme stejně velkou silou – měníme jen to, **kde** síla působí.',
          'Пластина скользит по **воздушному столу** без трения. Каждый раз коротко толкаем её одной и той же силой — меняем только то, **где** сила приложена.',
        )}
      />
      <Steps
        steps={[
          step(
            L('Strč do desky zezadu – síla míří těžištěm T', 'Толкни пластину сзади — сила идёт через центр тяжести T'),
            done.get(0),
            () => <ActionButton text={L('Strč', 'Толкнуть')} enabled={!running} onClick={run} />,
            () => (
              <SmallText
                text={L(
                  'Síla jde těžištěm: deska se během strčení rozjíždí (záblesky se rozestupují) a pak jede stálou rychlostí – nic ji nebrzdí. Stopa zůstala šedě.',
                  'Сила проходит через центр тяжести: во время толчка пластина разгоняется (вспышки расходятся), а потом едет с постоянной скоростью — её ничто не тормозит. След остался серым.',
                )}
              />
            ),
          ),
          tipStep(
            L('Stejná síla teď táhne lanem zepředu. Jak deska pojede?', 'Та же сила теперь тянет тросом спереди. Как поедет пластина?'),
            tipOpts(['úplně stejně', 'rychleji', 'pomaleji'], ['точно так же', 'быстрее', 'медленнее']),
            guessRope.v,
            (i) => (guessRope.v = i),
          ),
          step(
            L('Zatáhni lanem a porovnej záblesky se šedou stopou', 'Потяни тросом и сравни вспышки с серым следом'),
            done.get(1),
            () => <ActionButton text={L('Zatáhni lanem', 'Потянуть тросом')} enabled={!running} onClick={run} />,
            () => (
              <Note
                tone={guessRope.v === 0 ? 'ok' : 'info'}
                text={L(
                  (guessRope.v === 0 ? 'Správně – ' : 'Ne – ') + 'úplně stejná stopa jako při tlačení zezadu. Sílu smíš posunout **po její nositelce** – na pohyb tělesa to nemá vliv.',
                  (guessRope.v === 0 ? 'Верно — ' : 'Нет — ') + 'точно такой же след, как при толчке сзади. Силу можно сдвигать **вдоль её линии действия** — на движение тела это не влияет.',
                )}
              />
            ),
          ),
          tipStep(
            L('Sílu posuneme na rovnoběžnou nositelku nad T. Co deska udělá?', 'Сдвинем силу на параллельную линию выше T. Что сделает пластина?'),
            tipOpts(['Pojede stejně', 'Pojede a točí se ↻', 'Pojede a točí se ↺'], ['Поедет так же', 'Поедет и закрутится ↻', 'Поедет и закрутится ↺']),
            guess.v,
            (i) => (guess.v = i),
          ),
          step(
            L('Strč po nové nositelce a sleduj křížky těžiště', 'Толкни по новой линии и следи за крестиками центра тяжести'),
            done.get(2),
            () => <ActionButton text={L('Strč', 'Толкнуть')} enabled={!running} onClick={run} />,
            () => (
              <Note
                tone={guess.v === 1 ? 'ok' : 'info'}
                text={L(
                  (guess.v === 1 ? 'Správně. ' : 'Ve skutečnosti se točí ↻. ') +
                    'Těžiště T jede pořád stejně (křížky dopadají na šedé tečky), ale deska se navíc roztočí. Stejná síla na jiné nositelce má navíc **otáčivý účinek – moment** [[M]] = [[F]] · d (lekce 4).',
                  (guess.v === 1 ? 'Верно. ' : 'На самом деле крутится ↻. ') +
                    'Центр тяжести T едет так же (крестики попадают на серые точки), но пластина ещё и закручивается. Та же сила на другой линии действия даёт ещё и **вращающее действие — момент** [[M]] = [[F]] · d (урок 4).',
                )}
              />
            ),
          ),
        ]}
      />
    </>
  );
}

/** Předmět na siloměru: název a hmotnost v kg. */
interface Thing {
  name: L;
  kg: number;
  w: number;
}

const THINGS: Thing[] = [
  { name: L('jablko', 'яблоко'), kg: 0.1, w: 40 },
  { name: L('kbelík vody 10 l', 'ведро воды 10 л'), kg: 10, w: 60 },
  { name: L('člověk 70 kg', 'человек 70 кг'), kg: 70, w: 70 },
  { name: L('sud vody 100 l', 'бочка воды 100 л'), kg: 100, w: 76 },
  { name: L('malé auto', 'маленькая машина'), kg: 1000, w: 120 },
];

/** Karta 3: siloměr – kolik je 1 kN. Pružina se natáhne úměrně tíze. */
function SpringScale() {
  const t = useTr();
  const kit = useKit();
  const things = THINGS;
  const pick = kit.mut<number | null>(null);
  const guess = kit.mut<number | null>(null); // tip: co má tíhu asi 1 kN
  const tried = kit.list([false, false, false, false, false]);
  const stretch = kit.anim(0); // údaj siloměru v kN
  const hungTip = kit.mut(false); // tipnutý předmět visel a pružina se ustálila
  const per = 15; // natažení pružiny na 1 kN
  const hookX = 130;
  const top = 18;
  const rest = 40;
  const hang = (i: number) => {
    pick.v = i;
    tried.set(i, true);
    // pružina se ustálí s mírným zákmitem (silně tlumená, aby nevyskočila ze stupnice)
    kit.scope.launch(async () => {
      await stretch.animateTo(things[i].kg / 100, spring(0.5, 60));
      if (i === guess.v) hungTip.v = true;
    });
  };
  const reading = () => {
    if (pick.v === null) return null;
    const kn = things[pick.v].kg / 100;
    return <Readout text={kn < 0.01 ? `[[G]] ≈ ${num(kn * 1000, 0)} N = ${num(kn, 3)} kN` : `[[G]] ≈ ${num(kn, 2)} kN`} />;
  };
  const car = useQuizStep(
    L('Malé auto má 1 tunu. Kolik ukáže siloměr?', 'Маленькая машина — 1 тонна. Сколько покажет динамометр?'),
    [
      opt(L('1 kN', '1 кН'), false, L('1 kN je 100 kg. Kolikrát je tuna víc?', '1 кН — это 100 кг. Во сколько раз тонна больше?')),
      opt(L('10 kN', '10 кН'), true),
      opt(L('100 kN', '100 кН'), false, L('1 t = 1000 kg = 10 × 100 kg.', '1 т = 1000 кг = 10 × 100 кг.')),
      opt(L('1000 kN', '1000 кН'), false, L('1 t = 1000 kg = 10 × 100 kg.', '1 т = 1000 кг = 10 × 100 кг.')),
    ],
    L('1 t = 1000 kg = 10 × 100 kg, tedy asi **10 kN**. Jablko má asi 1 N, malé auto asi 10 kN.', '1 т = 1000 кг = 10 × 100 кг, то есть около **10 кН**. Яблоко — около 1 Н, маленькая машина — около 10 кН.'),
    null,
    2,
  );
  // po odpovědi se auto samo zavěsí – ručička doběhne na 10 kN
  useLaunched([car.done], async () => {
    if (car.done && pick.v !== 4) hang(4);
  });
  const g = guess.v ?? 0;
  return (
    <>
      <Stage
        h={260}
        description={t.pick(L('Siloměr s pružinou a stupnicí v kN', 'Динамометр с пружиной и шкалой в кН'))}
        draw={(b, c) => {
          const len = rest + clamp(stretch.value, 0, 10.6) * per;
          b.rect(hookX - 40, 6, 80, 8, c.beam, c.ink, 1.4);
          // pružina: cikcak mezi top a top + len
          const n = 14;
          let lx = hookX;
          let ly = top;
          b.line(hookX, 14, hookX, top, c.ink, 1.6);
          for (let i = 1; i <= n; i++) {
            const yy = top + (len * i) / n;
            const xx = i === n ? hookX : hookX + (i % 2 === 0 ? -9 : 9);
            b.line(lx, ly, xx, yy, c.ink, 1.6);
            lx = xx;
            ly = yy;
          }
          const end = top + len;
          // stupnice 0–10 kN a ukazatel na konci pružiny
          const sx = 230;
          b.line(sx, top + rest, sx, top + rest + 10 * per, c.inkSoft, 1.2);
          for (let k = 0; k <= 10; k++) {
            const yy = top + rest + k * per;
            b.line(sx, yy, sx + (k % 5 === 0 ? 12 : 7), yy, c.inkSoft, 1.2);
            if (k % 5 === 0) b.text(sx + 16, yy + 4, `${k} kN`, c.inkSoft, 11, 'start');
          }
          b.line(hookX + 12, end, sx - 2, end, c.load, 1.2, true);
          b.force(sx - 14, end, sx - 2, end, c.load, 2.0, 7);
          // zavěšený předmět
          if (pick.v !== null) {
            const th = things[pick.v];
            b.line(hookX, end, hookX, end + 8, c.ink, 1.6);
            b.rect(hookX - th.w / 2, end + 8, th.w, 30, c.subtle, c.ink, 1.5);
            b.text(hookX, end + 28, t.pick(th.name), c.ink, 11);
          }
        }}
      />
      <CardText
        text={L(
          'Síly měříme **siloměrem** – pružinou, která se natáhne tím víc, čím větší síla za ni tahá. Jednotka je newton (N); ve statice počítáme v **kilonewtonech**: 1 kN = 1000 N.',
          'Силы измеряют **динамометром** — пружиной, которая растягивается тем сильнее, чем больше сила. Единица — ньютон (Н); в статике считаем в **килоньютонах**: 1 кН = 1000 Н.',
        )}
      />
      <Steps
        steps={[
          tipStep(
            L('Tipni si: který předmět má tíhu asi 1 kN?', 'Как думаешь: какой предмет весит около 1 кН?'),
            things.map((x) => x.name),
            guess.v,
            (i) => (guess.v = i),
          ),
          step(L('Zavěs svůj tip na siloměr a přečti stupnici', 'Подвесь свой вариант на динамометр и посмотри на шкалу'), hungTip.v, () => (
            <>
              {reading()}
              <ActionButton text={L(`Zavěs: ${things[g].name.cs}`, `Подвесить: ${things[g].name.ru}`)} enabled={pick.v !== g} onClick={() => hang(g)} />
            </>
          )),
          step(
            L('Najdi předmět, který ukáže asi 1 kN', 'Найди предмет, который покажет около 1 кН'),
            tried.get(3),
            () => (
              <>
                {/* auto tu ještě není – jeho údaj je otázkou dalšího kroku */}
                <Chips options={things.slice(0, 4).map((x) => x.name)} selected={pick.v} onPick={hang} />
                {reading()}
              </>
            ),
            () => (
              <>
                {guess.v !== 3 && (
                  <Note
                    tone="info"
                    text={L(
                      'Tip nevyšel – nevadí, odhad sil je potřeba vycvičit. Asi 1 kN má sud se 100 litry vody.',
                      'Не угадал — ничего, чувство силы нужно натренировать. Около 1 кН весит бочка со 100 литрами воды.',
                    )}
                  />
                )}
                <SmallText text={L('Pamatuj: **1 kN ≈ tíha 100 kg** (přesně 0,98 kN).', 'Запомни: **1 кН ≈ сила тяжести 100 кг** (точно 0,98 кН).')} />
              </>
            ),
          ),
          car,
        ]}
        after={() => (
          <>
            <Chips options={things.map((x) => x.name)} selected={pick.v} onPick={hang} />
            {reading()}
          </>
        )}
      />
    </>
  );
}

/** Karta 4: kvíz – stejný směr, opačný smysl. */
function CheckSense() {
  const t = useTr();
  const q = useQuizStep(
    L('Která šipka má stejný směr jako [[F]], ale opačný smysl?', 'У какой стрелки тот же наклон, что у [[F]], но обратное направление?'),
    [
      opt(L('A', 'A'), true),
      opt(L('B', 'B'), false, L('B má jiný sklon, tedy jiný směr. Která šipka je s [[F]] rovnoběžná, ale hrot má na druhé straně?', 'У B другой наклон. Какая стрелка параллельна [[F]], но остриё у неё с другой стороны?')),
      opt(
        L('C', 'C'),
        false,
        L(
          'C má stejný sklon, ale i stejný smysl jako [[F]] – je jen posunutá. Která šipka míří hrotem na opačnou stranu?',
          'У C тот же наклон, но и направление то же, что у [[F]], — она просто сдвинута. У какой стрелки остриё смотрит в обратную сторону?',
        ),
      ),
    ],
    L(
      'Směr = sklon nositelky, takže rovnoběžné šipky mají stejný směr. Smysl = kam míří hrot: A je rovnoběžná s [[F]], ale míří opačně.',
      'Наклон (směr) у параллельных стрелок одинаковый. Направление (smysl) — куда смотрит остриё: A параллельна [[F]], но направлена противоположно.',
    ),
    null,
    3,
  );
  return (
    <>
      <Stage
        h={190}
        description={t.pick(L('Síla F a tři další šipky A, B, C', 'Сила F и ещё три стрелки A, B, C'))}
        draw={(b, c) => {
          const d = 40;
          const diag = (x: number, y: number, sense: number, col: string, label: string, sym = false) => {
            b.force(x - sense * d, y + sense * d * 0.6, x + sense * d, y - sense * d * 0.6, col, 2.6);
            if (sym) b.text(x, y + 40, null, col, 16, 'middle', label);
            else b.text(x, y + 40, label, col, 15, 'middle', null, true);
          };
          diag(60, 95, 1, c.load, 'F', true);
          // A: rovnoběžná, opačný hrot
          diag(155, 95, -1, c.ink, 'A');
          // B: jiný sklon (svislá)
          b.force(250, 130, 250, 60, c.ink, 2.6);
          b.text(250, 160, 'B', c.ink, 15, 'middle', null, true);
          // C: rovnoběžná, stejný hrot (jen posunutá)
          diag(340, 95, 1, c.ink, 'C');
        }}
      />
      <CardText text={L('**Směr** = sklon nositelky. **Smysl** = kam míří hrot.', '**Наклон** (směr) — наклон линии действия. **Направление** (smysl) — куда смотрит остриё.')} />
      <Steps steps={[q]} />
    </>
  );
}

export const cards = [Puck, TableSpring, LineOfAction, SpringScale, CheckSense];
