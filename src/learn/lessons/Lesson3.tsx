/*
 * Lekce 3 – Jedna šipka = dvě. Šikmou sílu nahradíme vodorovnou a svislou složkou:
 * F_x = F · cos α (strana u úhlu), F_y = F · sin α. Úhel α měříme od osy nosníku jako v trenažéru.
 * (Přepis z aplikace: learn/Lesson3.kt.)
 */
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
  inclined,
  num,
  numSpec,
  numTrap,
  opt,
  step,
  tipStep,
  trapNear,
  useCardState,
  useKit,
  useLaunched,
  useNumStep,
  useQuizStep,
  useTr,
  type Colors,
  type PrimBuilder,
} from '../kit';

const F_KN = 10;
const SCALE = 14; // jednotek na 1 kN

/** Karta 3: posuvník úhlu – složky se mění živě. */
function AngleComponents() {
  const t = useTr();
  const kit = useKit();
  const alpha = kit.mut(60);
  const dragging = kit.mut(false);
  const half = kit.mut(false);
  const equal = kit.mut(false);
  const tipHalf = kit.mut<number | null>(null); // tip: 0 = 30°, 1 = 45°, 2 = 60°
  const showing = kit.mut(false); // šipka se sama sklápí na 0° a zpět (potvrzení kvízu)
  const zero = useQuizStep(
    L('Síla leží podél nosníku (α = 0°). Která složka je nulová?', 'Сила лежит вдоль балки (α = 0°). Какая составляющая равна нулю?'),
    [
      opt(L('[[F_y]] (svislá)', '[[F_y]] (вертикальная)'), true),
      opt(L('[[F_x]] (vodorovná)', '[[F_x]] (горизонтальная)'), false, L('Síla podél nosníku míří vodorovně. Zbude jí něco, co tlačí dolů?', 'Сила вдоль балки направлена горизонтально. Останется ли у неё что-то, что давит вниз?')),
      opt(
        L('žádná – obě jsou 5 kN', 'никакая — обе по 5 кН'),
        false,
        L('Napůl se síla dělí jen při jednom úhlu, a ne při 0°. Co zbude ze svislé složky, když síla leží na nosníku?', 'Пополам сила делится лишь при одном угле, и не при 0°. Что останется от вертикальной составляющей, если сила лежит на балке?'),
      ),
    ],
    L('Při α = 0° je celá síla vodorovná: [[F_x]] = 10 kN, [[F_y]] = 0. Při α = 90° naopak [[F_x]] = 0.', 'При α = 0° вся сила горизонтальна: [[F_x]] = 10 кН, [[F_y]] = 0. При α = 90°, наоборот, [[F_x]] = 0.'),
  );
  // potvrzení: šipka se sklopí na 0° (F_y zmizí) a vrátí na 60°
  useLaunched([zero.done], async (job) => {
    if (!zero.done) return;
    showing.v = true;
    await job.delay(300);
    for (let a = 55; a >= 0; a -= 5) {
      alpha.v = a;
      await job.delay(45);
    }
    await job.delay(1100);
    for (let a = 5; a <= 60; a += 5) {
      alpha.v = a;
      await job.delay(45);
    }
    showing.v = false;
  });
  // cíl se počítá, až když na něm úhel chvíli zůstane – ne když posuvník přes něj jen přejede
  useLaunched([alpha.v, tipHalf.v, showing.v], async (job) => {
    if (showing.v || tipHalf.v === null) return;
    if (alpha.v === 30 || (alpha.v === 45 && half.v)) {
      await job.delay(500);
      if (alpha.v === 30) half.v = true;
      if (alpha.v === 45 && half.v) equal.v = true;
    }
  });
  const tx = 310;
  const ty = 196;
  const canTurn = zero.done && !showing.v;
  const setAlpha = (v: number) => {
    if (!canTurn) return;
    alpha.v = clamp(Math.round(v / 5) * 5, 0, 90);
  };
  const turn = () => {
    const a = (alpha.v * Math.PI) / 180;
    return (
      <>
        <ValueSlider label={`α = ${Math.round(alpha.v)}°`} value={alpha.v} min={0} max={90} steps={17} onChange={setAlpha} />
        <Readout text={`[[F_x]] = 10 · cos ${Math.round(alpha.v)}° = ${num(F_KN * Math.cos(a))} kN\n[[F_y]] = 10 · sin ${Math.round(alpha.v)}° = ${num(F_KN * Math.sin(a))} kN`} />
      </>
    );
  };
  const tips = [L('30°', '30°'), L('45°', '45°'), L('60°', '60°')];
  return (
    <>
      <Stage
        h={240}
        description={t.pick(L('Šikmá síla a její složky', 'Наклонная сила и её составляющие'))}
        onDrag={(p, start) => {
          dragging.v = canTurn;
          // úhel podle polohy prstu vůči působišti (konec šipky je vlevo nahoře)
          if (!start || p.y < ty) setAlpha((Math.atan2(ty - p.y, tx - p.x) * 180) / Math.PI);
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b) => {
          b.beam(20, 380, ty + 5);
          inclined(b, tx, ty, alpha.v, 1, 1, true, true);
          const a = (alpha.v * Math.PI) / 180;
          // ručka kousek za koncem šipky, aby nezakryla popisek ani šipku
          const len = F_KN * SCALE + 16;
          if (canTurn) b.handle(tx - len * Math.cos(a), ty - len * Math.sin(a), dragging.v);
        }}
      />
      <CardText
        text={L(
          'Složky šikmé síly určuje úhel α mezi silou a nosníkem. Síla má pořád 10 kN – mění se jen to, jak se mezi složky „rozdělí“.',
          'Составляющие наклонной силы определяет угол α между силой и балкой. Сила всё время 10 кН — меняется только то, как она «делится» между составляющими.',
        )}
      />
      <Steps
        steps={[
          zero,
          tipStep(L('Tipni si: při jakém úhlu je [[F_y]] přesně polovina síly?', 'Как думаешь: при каком угле [[F_y]] равна ровно половине силы?'), tips, tipHalf.v, (i) => (tipHalf.v = i)),
          step(
            L('Najdi úhel, při kterém je [[F_y]] = 5 kN', 'Найди угол, при котором [[F_y]] = 5 кН'),
            half.v,
            turn,
            () =>
              tipHalf.v === 0 ? (
                <Note tone="ok" text={L('Tip sedí: sin 30° = 0,5, takže [[F_y]] = 10 · 0,5 = 5 kN.', 'Прогноз верный: sin 30° = 0,5, значит [[F_y]] = 10 · 0,5 = 5 кН.')} />
              ) : (
                <Note
                  tone="info"
                  text={L(`Polovina síly není při ${tips[tipHalf.v ?? 1].cs}, ale při 30°: sin 30° = 0,5.`, `Половина силы — не при ${tips[tipHalf.v ?? 1].ru}, а при 30°: sin 30° = 0,5.`)}
                />
              ),
          ),
          step(L('Najdi úhel, při kterém jsou obě složky stejně velké', 'Найди угол, при котором обе составляющие равны'), equal.v, turn, () => (
            <SmallText
              text={L(
                'Při 45° jsou složky stejné: 7,07 + 7,07 = 14,1 kN – víc než 10! Složky se nesčítají jako čísla, ale jako šipky (7,07² + 7,07² = 10²).',
                'При 45° составляющие равны: 7,07 + 7,07 = 14,1 кН — больше 10! Составляющие складываются не как числа, а как стрелки (7,07² + 7,07² = 10²).',
              )}
            />
          )),
        ]}
        after={turn}
      />
      <SmallText
        text={L(
          'Pomůcka: složka **u úhlu** = [[F]] · cos α, složka **naproti** úhlu = [[F]] · sin α. Tady α měříme od vodorovné, proto je u úhlu [[F_x]].',
          'Подсказка: составляющая, **прилежащая к углу**, = [[F]] · cos α, **противолежащая** = [[F]] · sin α. Здесь α отсчитываем от горизонтали, поэтому к углу прилежит [[F_x]].',
        )}
      />
      {/* nepovinný odkaz – odkud se sin a cos berou (kurz trigonometrie) */}
      <LearnJump lesson={102} card={0} label={L('Proč zrovna cos a sin? → Trigonometrie: Sinus, kosinus, tangens', 'Почему именно cos и sin? → Тригонометрия: синус, косинус, тангенс')} />
    </>
  );
}

/** Sáně shora: korba, dvě lyžiny přečnívající dopředu (doprava). */
function sled(b: PrimBuilder, cx: number, cy: number, c: Colors, a = 1) {
  b.line(cx - 24, cy - 16, cx + 30, cy - 16, c.ink, 2.0, false, a);
  b.line(cx - 24, cy + 16, cx + 30, cy + 16, c.ink, 2.0, false, a);
  b.rect(cx - 20, cy - 13, 40, 26, c.beam, c.ink, 1.6, a);
  b.circle(cx, cy, 3, c.ink, null, 1.5, a);
}

const SLED_KG = 250;
const SLED_M = 20; // jednotek na 1 m
const ROPE_K = 0.25; // jednotek na 1 N

/** Úhel 0…360° (osa y dolů) zaokrouhlený na 5°; blízko přesné rovnováhy se k ní „přichytí“. */
function snapAngle(deg: number, exact: number): number {
  let d = deg % 360;
  if (d < 0) d += 360;
  let diff = Math.abs(d - exact);
  if (diff > 180) diff = 360 - diff;
  return diff < 3 ? exact : (Math.round(d / 5) * 5) % 360;
}

/**
 * Karta 0: dvě lana. Sáně na ledě (shora) táhnou dvě lana – 400 N doprava, 300 N nahoru. Žák si tipne, kam se
 * rozjedou, pustí je: zrychlují po úhlopříčce rovnoběžníku, výslednice má 500 N (ne 700). Pak přidá třetí lano tak,
 * aby sáně zůstaly stát – síla stejně velká jako výslednice, opačného smyslu.
 */
function TwoRopes() {
  const t = useTr();
  const kit = useKit();
  const sx0 = 170;
  const sy0 = 140;
  // síly v N, osa y dolů
  const f1x = 400;
  const f1y = 0;
  const f2x = 0;
  const f2y = -300;
  const exact = (Math.atan2(-(f1y + f2y), -(f1x + f2x)) * 180) / Math.PI; // směr síly, která výslednici vyruší (≈ 143°)
  const pred = kit.mut<number | null>(null);
  const ran = kit.mut(false);
  const still = kit.mut(false);
  const f3 = kit.mut(300);
  const a3 = kit.mut(180);
  const dragging = kit.mut(false);
  const running = kit.mut(false);
  const px = kit.mut(sx0);
  const py = kit.mut(sy0);
  const alpha = kit.mut(1);
  const stoodStill = kit.mut(false);
  const trail = kit.list<[number, number]>([]);
  const lastLeft = kit.mut<number | null>(null); // Σ F posledního pokusu se třetím lanem (až po puštění)
  const rq = useQuizStep(
    L('Jak velká je výslednice [[R]], která sáně rozjíždí?', 'Чему равна равнодействующая [[R]], которая разгоняет санки?'),
    [
      opt(
        L('700 N', '700 Н'),
        false,
        L('400 + 300 by platilo, kdyby obě lana táhla stejným směrem. Míří ale jinam – změř fialovou úhlopříčku.', '400 + 300 было бы, если бы обе верёвки тянули в одну сторону. Но они направлены по-разному — оцени фиолетовую диагональ.'),
      ),
      opt(L('500 N', '500 Н'), true),
      opt(L('100 N', '100 Н'), false, L('Odečítat by se síly mohly, jen kdyby táhly proti sobě. Tady svírají pravý úhel.', 'Вычитать силы можно, только если они тянут навстречу друг другу. Здесь они под прямым углом.')),
    ],
    L(
      'Síly se sčítají jako **šipky**, ne jako čísla: šipky dáš za sebe (nebo doplníš rovnoběžník) a výslednice je úhlopříčka. Tady pravoúhlý trojúhelník 3-4-5: √(400² + 300²) = 500 N.',
      'Силы складываются как **стрелки**, а не как числа: стрелки ставишь одну за другой (или достраиваешь параллелограмм), равнодействующая — диагональ. Здесь прямоугольный треугольник 3-4-5: √(400² + 300²) = 500 Н.',
    ),
    null,
    3,
  );
  // třetí lano přijde až po otázce na výslednici – její velikost je k němu klíčem
  const use3 = rq.done;
  const net = (): [number, number] => {
    let x = f1x + f2x;
    let y = f1y + f2y;
    if (use3) {
      x += f3.v * Math.cos((a3.v * Math.PI) / 180);
      y += f3.v * Math.sin((a3.v * Math.PI) / 180);
    }
    return [x, y];
  };
  const release = () => {
    if (running.v || pred.v === null) return;
    running.v = true;
    stoodStill.v = false;
    trail.clear();
    const [nx, ny] = net();
    const withThird = use3;
    lastLeft.v = withThird ? Math.hypot(nx, ny) : null;
    kit.scope.launch(async (job) => {
      const ax = nx / SLED_KG;
      const ay = ny / SLED_KG;
      let time = 0;
      let last = 0;
      let nextGhost = 0;
      px.v = sx0;
      py.v = sy0;
      alpha.v = 1;
      // pozorujeme nejvýš 3,5 s nebo do kraje kluziště
      while (time < 3.5 && px.v >= 35 && px.v <= 365 && py.v >= 30 && py.v <= 222) {
        const now = await job.frame();
        const dt = last === 0 ? 0 : (now - last) / 1000;
        last = now;
        time += dt;
        px.v = sx0 + 0.5 * ax * time * time * SLED_M;
        py.v = sy0 + 0.5 * ay * time * time * SLED_M;
        if (time >= nextGhost) {
          trail.add([px.v, py.v]);
          nextGhost += 0.4;
        }
      }
      if (Math.hypot(nx, ny) < 1) {
        stoodStill.v = true;
        if (withThird) still.v = true;
      }
      ran.v = true;
      await job.delay(500);
      // pokus skončil: sáně zmizí a na startu se objeví nové (nikdy necouvají zpátky)
      if (px.v !== sx0 || py.v !== sy0) {
        for (let i = 1; i <= 8; i++) {
          alpha.v = 1 - i / 8;
          await job.delay(30);
        }
        px.v = sx0;
        py.v = sy0;
        for (let i = 1; i <= 8; i++) {
          alpha.v = i / 8;
          await job.delay(30);
        }
      }
      running.v = false;
    });
  };
  const tip3x = sx0 + f3.v * ROPE_K * Math.cos((a3.v * Math.PI) / 180);
  const tip3y = sy0 + f3.v * ROPE_K * Math.sin((a3.v * Math.PI) / 180);
  const releaseBtn = () => <ActionButton text={L('Pusť sáně', 'Отпустить санки')} enabled={!running.v} onClick={release} />;
  return (
    <>
      <Stage
        h={240}
        description={t.pick(L('Sáně na ledě shora, táhnou je dvě lana', 'Санки на льду сверху, их тянут две верёвки'))}
        onDrag={(p, start) => {
          if (start) dragging.v = use3 && !running.v && dist(p, tip3x, tip3y) < 40;
          if (dragging.v && dist(p, sx0, sy0) > 20) {
            a3.v = snapAngle((Math.atan2(p.y - sy0, p.x - sx0) * 180) / Math.PI, exact);
            f3.v = clamp(Math.round(Math.hypot(p.x - sx0, p.y - sy0) / ROPE_K / 50) * 50, 50, 700);
            lastLeft.v = null;
          }
        }}
        onDragEnd={() => (dragging.v = false)}
        draw={(b, c) => {
          b.rect(10, 10, 380, 220, withAlpha(c.react, 0.05), c.inkSoft, 1.2);
          // stopa posledního pokusu
          for (const [gx, gy] of trail.v.slice(0, -1)) b.circle(gx, gy, 3, c.inkSoft, null, 1.5, 0.6);
          const ox = px.v;
          const oy = py.v;
          const showForces = alpha.v > 0.5;
          if (ran.v) {
            // rovnoběžník: výslednice R je jeho úhlopříčka
            const rx = ox + (f1x + f2x) * ROPE_K;
            const ry = oy + (f1y + f2y) * ROPE_K;
            b.line(ox + f1x * ROPE_K, oy + f1y * ROPE_K, rx, ry, c.derived, 1.0, true, 0.7);
            b.line(ox + f2x * ROPE_K, oy + f2y * ROPE_K, rx, ry, c.derived, 1.0, true, 0.7);
            b.force(ox, oy, rx, ry, c.derived, 2.6, 11, false, alpha.v);
            b.text(rx + 6, ry - 6, use3 ? ' = 500 N' : ' = ?', c.derived, 13, 'start', 'R', false, alpha.v);
          }
          if (showForces) {
            b.force(ox + 24, oy, ox + f1x * ROPE_K, oy + f1y * ROPE_K, c.load, 3.0, 11, false, alpha.v);
            b.text(ox + f1x * ROPE_K - 4, oy + 22, ' = 400 N', c.load, 13, 'end', 'F_1', false, alpha.v);
            b.force(ox, oy - 14, ox + f2x * ROPE_K, oy + f2y * ROPE_K, c.load, 3.0, 11, false, alpha.v);
            b.text(ox - 8, oy + f2y * ROPE_K + 14, ' = 300 N', c.load, 13, 'end', 'F_2', false, alpha.v);
            if (use3) {
              const tx = ox + f3.v * ROPE_K * Math.cos((a3.v * Math.PI) / 180);
              const ty = oy + f3.v * ROPE_K * Math.sin((a3.v * Math.PI) / 180);
              b.force(ox, oy, tx, ty, c.react, 3.0, 11, false, alpha.v);
              b.text(tx + (tx < ox ? -16 : 16), ty + 5, ` = ${num(f3.v, 0)} N`, c.react, 13, tx < ox ? 'end' : 'start', 'F_3', false, alpha.v);
              if (!running.v) b.handle(tx, ty, dragging.v);
            }
          }
          sled(b, ox, oy, c, alpha.v);
          if (running.v && !stoodStill.v && trail.size > 1) b.text(390 - 8, 226, t.pick(L('tečky: poloha každých 0,4 s', 'точки: положение каждые 0,4 с')), c.inkSoft, 11, 'end');
          if (stoodStill.v) b.text(200, 30, t.pick(L('Σ F = 0 – sáně stojí', 'Σ F = 0 — санки стоят')), c.accent, 15, 'middle', null, true);
        }}
      />
      <CardText
        text={L(
          'Sáně (250 kg) na hladkém ledě, pohled shora. Jedno lano táhne silou 400 N doprava, druhé 300 N nahoru.',
          'Санки (250 кг) на гладком льду, вид сверху. Одна верёвка тянет с силой 400 Н вправо, другая — 300 Н вверх.',
        )}
      />
      <Steps
        steps={[
          tipStep(
            L('Tipni si: kam se sáně po puštění rozjedou?', 'Как думаешь: куда поедут санки, если их отпустить?'),
            [L('→ doprava, za silnějším lanem', '→ вправо, за более сильной верёвкой'), L('↗ šikmo mezi lany', '↗ наискосок, между верёвками'), L('nikam – lana se přetahují', 'никуда — верёвки перетягивают друг друга')],
            pred.v,
            (i) => (pred.v = i),
          ),
          step(L('Pusť sáně a sleduj jejich stopu', 'Отпусти санки и следи за их следом'), ran.v, releaseBtn, () =>
            pred.v === 1 ? (
              <Note
                tone="ok"
                text={L('Správně: šikmo, po úhlopříčce rovnoběžníku ze dvou lan – a blíž k silnějšímu lanu.', 'Верно: наискосок, по диагонали параллелограмма из двух верёвок — и ближе к более сильной верёвке.')}
              />
            ) : (
              <Note
                tone="info"
                text={L(
                  'Sáně jely šikmo – po úhlopříčce rovnoběžníku, který tvoří obě síly. Každé lano „přidá“ svůj kus pohybu.',
                  'Санки поехали наискосок — по диагонали параллелограмма, который образуют обе силы. Каждая верёвка «добавляет» свою часть движения.',
                )}
              />
            ),
          ),
          rq,
          step(
            L('Přidej třetí lano [[F_3]] tak, aby sáně po puštění zůstaly stát', 'Добавь третью верёвку [[F_3]] так, чтобы отпущенные санки остались на месте'),
            still.v,
            () => (
              <>
                <div className="krow">
                  <div className="grow">
                    <Chips
                      options={[L('↺ 5°', '↺ 5°'), L('↻ 5°', '↻ 5°')]}
                      onPick={(i) => {
                        if (!running.v) {
                          a3.v = snapAngle(a3.v + (i === 0 ? -5 : 5), exact);
                          lastLeft.v = null;
                        }
                      }}
                    />
                  </div>
                  <div className="grow">
                    <Chips
                      options={[L('− 50 N', '− 50 Н'), L('+ 50 N', '+ 50 Н')]}
                      onPick={(i) => {
                        if (!running.v) {
                          f3.v = clamp(f3.v + (i === 0 ? -50 : 50), 50, 700);
                          lastLeft.v = null;
                        }
                      }}
                    />
                  </div>
                </div>
                {releaseBtn()}
                {lastLeft.v !== null && lastLeft.v >= 1 && !running.v && (
                  <SmallText
                    text={L(
                      `Zbylo Σ[[F]] = ${num(lastLeft.v, 0)} N – sáně se rozjely. Kam musí [[F_3]] mířit a jak velká musí být, aby výslednici vyrušila?`,
                      `Осталось Σ[[F]] = ${num(lastLeft.v, 0)} Н — санки поехали. Куда должна быть направлена [[F_3]] и какой величины, чтобы уравновесить равнодействующую?`,
                    )}
                  />
                )}
              </>
            ),
            () => (
              <Note
                tone="ok"
                text={L(
                  'Třetí lano je stejně velké jako výslednice (500 N), na stejné přímce, ale opačného smyslu. Tři síly se vyrušily: Σ[[F]] = 0 – přesně tohle budeme hledat u nosníků.',
                  'Третья верёвка равна равнодействующей (500 Н), лежит на той же прямой, но направлена в обратную сторону. Три силы уравновесились: Σ[[F]] = 0 — именно это мы будем искать у балок.',
                )}
              />
            ),
          ),
        ]}
      />
    </>
  );
}

/** Bedna na vzduchovém polštáři: 0 = F, 1 = F_x + F_y, 2 = jen F_x, 3 = F + F_x + F_y (síly v kN, osa y dolů). */
const TRIALS: [number, number][] = [
  [8, -6],
  [8, -6],
  [8, 0],
  [16, -12],
];

/**
 * Karta 1: složky nahradí sílu. Bedna 2 t na vzduchovém polštáři (pohled shora) dostane krátké strčení 0,5 s a pak klouže.
 * Síla F = 10 kN šikmo, nebo obě složky najednou – bedna dojede na stejné místo. Jen F_x – jinam.
 * F i složky dohromady – dvakrát dál (síla započtená dvakrát).
 */
function ReplaceToggle() {
  const t = useTr();
  const kit = useKit();
  const x0 = 60;
  const y0 = 195;
  const perM = 30;
  const k = 7; // jednotek šipky na 1 kN
  const mass = 2; // t
  const tau = 0.5; // s strčení
  const total = 2.0; // s pozorování
  const modeM = kit.mut(0);
  const running = kit.mut(false);
  const time = kit.mut(0);
  const alpha = kit.mut(1);
  const ends = kit.mut<Record<number, [number, number]>>({});
  const tipSame = kit.mut<number | null>(null); // tip pro složky: 0 stejné místo, 1 dál, 2 jinam
  // pokus, na který čeká aktuální krok
  const next = [0, 1, 2, 3].find((i) => !(i in ends.v)) ?? 3;
  const posAt = (m: number, tt: number): [number, number] => {
    const [fx, fy] = TRIALS[m];
    const s = tt < tau ? 0.5 * tt * tt : 0.5 * tau * tau + tau * (tt - tau); // m na 1 m/s²
    return [x0 + (fx / mass) * s * perM, y0 + (fy / mass) * s * perM];
  };
  const run = (m: number) => {
    if (running.v) return;
    modeM.v = m;
    running.v = true;
    kit.scope.launch(async (job) => {
      time.v = 0;
      alpha.v = 1;
      let last = 0;
      while (time.v < total) {
        const now = await job.frame();
        const dt = last === 0 ? 0 : (now - last) / 1000;
        last = now;
        time.v = Math.min(time.v + dt, total);
      }
      ends.v = { ...ends.v, [m]: posAt(m, total) };
      await job.delay(700);
      for (let i = 1; i <= 8; i++) {
        alpha.v = 1 - i / 8;
        await job.delay(30);
      }
      time.v = 0;
      for (let i = 1; i <= 8; i++) {
        alpha.v = i / 8;
        await job.delay(30);
      }
      running.v = false;
    });
  };
  const twice = useQuizStep(
    L('Na výkresu zůstane [[F]] i obě složky. Co se stane ve výpočtu?', 'На чертеже остались [[F]] и обе составляющие. Что будет в расчёте?'),
    [
      opt(L('nic – složky jsou jen jiný zápis', 'ничего — составляющие лишь другая запись'), false, L('Zápis to je – ale jen když sílu **nahradí**. Kolikrát pak započítáš 10 kN?', 'Это запись — но только если они силу **заменяют**. Сколько раз тогда учтёшь 10 кН?')),
      opt(L('sílu započítáš dvakrát – účinek bude dvojnásobný', 'сила будет учтена дважды — действие удвоится'), true),
      opt(L('složky se se silou vyruší', 'составляющие уравновесят силу'), false, L('Vyrušit by se mohly jen síly opačného smyslu. Míří složky proti [[F]]?', 'Уравновеситься могут только противоположно направленные силы. Разве составляющие направлены против [[F]]?')),
    ],
    L(
      'Složky sílu **nahrazují**, nepřidávají se k ní. Když složky nakreslíš, původní šipku přeškrtni (nebo smaž) – jinak ji započítáš dvakrát.',
      'Составляющие силу **заменяют**, а не добавляются к ней. Нарисовал составляющие — зачеркни (или сотри) исходную стрелку, иначе учтёшь её дважды.',
    ),
  );
  // po odpovědi se pokus F + F_x + F_y spustí sám – ukáže, co by udělal dvakrát započtený výkres
  useLaunched([twice.done], async (job) => {
    if (!twice.done || 3 in ends.v) return;
    await job.delay(700);
    run(3);
  });
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Bedna na vzduchovém polštáři, strčená silou nebo jejími složkami', 'Ящик на воздушной подушке, толкаемый силой или её составляющими'))}
        draw={(b, c) => {
          b.rect(10, 10, 380, 210, withAlpha(c.react, 0.04), c.inkSoft, 1.0);
          // dojezdy předchozích pokusů
          for (const [ex, ey] of Object.values(ends.v)) {
            b.line(x0, y0, ex, ey, c.inkSoft, 1.0, true, 0.6);
            b.line(ex - 6, ey - 6, ex + 6, ey + 6, c.ink, 1.6);
            b.line(ex - 6, ey + 6, ex + 6, ey - 6, c.ink, 1.6);
          }
          // popisky křížků: F a F_x + F_y padnou na stejné místo – jeden společný popisek
          const e0 = ends.v[0];
          const e1 = ends.v[1];
          if (e0 || e1) {
            const [ex, ey] = (e0 ?? e1)!;
            const lbl =
              e0 && e1 ? t.pick(L('F i složky: stejné místo ✓', 'F и составляющие: то же место ✓')) : e0 ? t.pick(L('síla F', 'сила F')) : t.pick(L('složky', 'составляющие'));
            b.text(ex + 10, ey - 8, lbl, c.ink, 12, 'start');
          }
          if (ends.v[2]) b.text(ends.v[2][0], ends.v[2][1] + 22, t.pick(L(' sama', ' одна')), c.ink, 12, 'middle', 'F_x');
          if (ends.v[3]) b.text(ends.v[3][0] - 10, ends.v[3][1] + 4, t.pick(L('F + složky', 'F + составляющие')), c.ink, 12, 'end');
          const mode = running.v ? modeM.v : next;
          const [bx, by] = running.v ? posAt(mode, time.v) : [x0, y0];
          b.rect(bx - 18, by - 14, 36, 28, c.beam, c.ink, 1.6, alpha.v);
          b.circle(bx, by, 2.5, c.ink, null, 1.5, alpha.v);
          // síly působí v těžišti bedny (všechny v jednom bodě) jen během strčení
          const fa = !running.v ? alpha.v : time.v < tau ? 1 : clamp(1 - (time.v - tau) / 0.25, 0, 1) * alpha.v;
          if (fa > 0.02) {
            const showF = mode === 0 || mode === 3;
            const showC = mode === 1 || mode === 2 || mode === 3;
            if (mode === 1 || mode === 3) {
              b.line(bx + 8 * k, by, bx + 8 * k, by - 6 * k, c.derived, 1.0, true, fa * 0.6);
              b.line(bx, by - 6 * k, bx + 8 * k, by - 6 * k, c.derived, 1.0, true, fa * 0.6);
            }
            if (showC) {
              b.force(bx, by, bx + 8 * k, by, c.derived, 2.6, 11, running.v, fa);
              b.text(bx + 4 * k, by + 31, ' = 8 kN', c.derived, 12, 'middle', 'F_x', false, fa);
              if (mode !== 2) {
                b.force(bx, by, bx, by - 6 * k, c.derived, 2.6, 11, running.v, fa);
                b.text(bx - 6, by - 3 * k, ' = 6 kN', c.derived, 12, 'end', 'F_y', false, fa);
              }
            }
            if (showF) {
              b.force(bx, by, bx + 8 * k, by - 6 * k, c.load, 3.0, 11, running.v, fa);
              b.text(bx + 8 * k + 6, by - 6 * k + 4, ' = 10 kN', c.load, 13, 'start', 'F', false, fa);
            }
          }
          if (running.v && time.v > tau) b.text(380, 214, t.pick(L('síla skončila – bedna klouže sama', 'сила кончилась — ящик скользит сам')), c.inkSoft, 11, 'end');
          b.text(20, 28, t.pick(L('pozorujeme vždy 2 s', 'наблюдаем всегда 2 с')), c.inkSoft, 11, 'start');
        }}
      />
      <CardText
        text={L(
          'Šikmou sílu smíme nahradit dvěma **složkami** – vodorovnou [[F_x]] a svislou [[F_y]], které působí **ve stejném bodě**. Ověříme to pokusem: bedna (2 t) na vzduchovém polštáři dostane krátké strčení a my sledujeme, kam za 2 s dojede.',
          'Наклонную силу можно заменить двумя **составляющими** — горизонтальной [[F_x]] и вертикальной [[F_y]], приложенными **в той же точке**. Проверим опытом: ящик (2 т) на воздушной подушке получает короткий толчок, а мы смотрим, куда он доедет за 2 с.',
        )}
      />
      <Steps
        steps={[
          step(L('Strč do bedny šikmou silou [[F]] = 10 kN', 'Толкни ящик наклонной силой [[F]] = 10 кН'), 0 in ends.v, () => (
            <ActionButton text={L('Strč silou F', 'Толкнуть силой F')} enabled={!running.v} onClick={() => run(0)} />
          )),
          tipStep(
            L('Místo [[F]] strčíme složkami [[F_x]] a [[F_y]] najednou. Kam bedna dojede?', 'Вместо [[F]] толкнём обеими составляющими сразу. Куда доедет ящик?'),
            [L('na stejné místo', 'в то же место'), L('dál', 'дальше'), L('jinam – jiným směrem', 'в другую сторону')],
            tipSame.v,
            (i) => (tipSame.v = i),
          ),
          step(
            L('Strč oběma složkami a porovnej s první značkou', 'Толкни обеими составляющими и сравни с первой меткой'),
            1 in ends.v,
            () => <ActionButton text={L('Strč oběma složkami', 'Толкнуть составляющими')} enabled={!running.v} onClick={() => run(1)} />,
            () => (
              <SmallText
                text={L(
                  (tipSame.v === 0 ? 'Tip sedí. ' : '') + 'Stejné místo: složky dohromady dělají přesně totéž co celá síla. Jsou to odvěsny pravoúhlého trojúhelníku, síla je přepona: 8 a 6 kN z 10 kN.',
                  (tipSame.v === 0 ? 'Прогноз верный. ' : '') + 'То же место: составляющие вместе действуют точно так же, как вся сила. Это катеты прямоугольного треугольника, сила — гипотенуза: 8 и 6 кН из 10 кН.',
                )}
              />
            ),
          ),
          step(
            L('Strč jen složkou [[F_x]] – stačí jedna složka?', 'Толкни только составляющей [[F_x]] — хватит ли одной?'),
            2 in ends.v,
            () => <ActionButton text={L('Strč jen vodorovnou', 'Толкнуть только горизонтальной')} enabled={!running.v} onClick={() => run(2)} />,
            () => (
              <SmallText
                text={L('Jen [[F_x]] nestačí – bedna jede vodorovně a mine cíl. Sílu nahradí až **obě** složky.', 'Одной [[F_x]] мало — ящик едет горизонтально и проходит мимо. Силу заменяют только **обе** составляющие.')}
              />
            ),
          ),
          twice,
          step(
            L('Sleduj: [[F]] i složky najednou – bedna dojede dvakrát dál', 'Смотри: [[F]] и составляющие вместе — ящик уедет вдвое дальше'),
            3 in ends.v,
            () => <SmallText text={L('Pokus [[F]] + [[F_x]] + [[F_y]] se spustí sám…', 'Опыт [[F]] + [[F_x]] + [[F_y]] запустится сам…')} />,
            () => <SmallText text={L('Dvakrát dál: s [[F]] i se složkami je ve výpočtu 20 kN místo 10 kN.', 'Вдвое дальше: с [[F]] и составляющими в расчёте 20 кН вместо 10 кН.')} />,
          ),
        ]}
      />
    </>
  );
}

/** Karta 2: síla na čtverečkovaném papíře – trojúhelník 3-4-5 (složky ze stejného bodu jako síla). */
function Grid345() {
  const t = useTr();
  const card = useCardState();
  const solved = card.solvedHere;
  const s = 36;
  const ox = 110;
  const oy = 200;
  const ns = useNumStep(
    L('Spočítej čtverečky: kolik dílků má [[F_x]] a kolik [[F_y]]?', 'Посчитай клетки: сколько делений у [[F_x]] и сколько у [[F_y]]?'),
    [numSpec(L('[[F_x]]', '[[F_x]]'), 4), numSpec(L('[[F_y]]', '[[F_y]]'), 3)],
    L(
      '4 dílky doprava a 3 nahoru. Pozor: 4 + 3 ≠ 5 – složky se silou tvoří pravoúhlý trojúhelník, platí 4² + 3² = 5².',
      '4 деления вправо и 3 вверх. Внимание: 4 + 3 ≠ 5 — составляющие и сила образуют прямоугольный треугольник: 4² + 3² = 5².',
    ),
    [
      trapNear(L('[[F_x]] je **vodorovná** složka. Spočítej znovu: o kolik dílků míří šipka doprava?', '[[F_x]] — **горизонтальная** составляющая. Посчитай ещё раз: на сколько делений стрелка уходит вправо?'), 3, 4),
      numTrap(L('Může být složka stejně dlouhá jako celá šikmá šipka? Spočítej čtverečky pod ní a vedle ní.', 'Может ли составляющая быть такой же длины, как вся наклонная стрелка? Посчитай клетки под ней и сбоку.'), (v) =>
        v.some((it) => it !== null && Math.abs(it) >= 5),
      ),
    ],
    L('Počítej strany čtverečků pod šipkou a vedle ní.', 'Считай стороны клеток под стрелкой и сбоку от неё.'),
  );
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Síla na čtverečkované síti', 'Сила на клетчатой сетке'))}
        draw={(b, c) => {
          for (let i = 0; i <= 5; i++) b.line(ox + i * s, oy - 4 * s, ox + i * s, oy + 0.5 * s, c.line, 1.0);
          for (let j = -4; j <= 0; j++) b.line(ox - 0.5 * s, oy + j * s, ox + 5 * s, oy + j * s, c.line, 1.0);
          if (solved) {
            // složky začínají tam co síla – strany obdélníku
            b.force(ox, oy + 6, ox + 4 * s, oy + 6, c.derived, 2.4);
            b.force(ox - 6, oy, ox - 6, oy - 3 * s, c.derived, 2.4);
            b.text(ox + 2 * s, oy + 26, ' = 4', c.derived, 14, 'middle', 'F_x');
            b.text(ox - 14, oy - 1.5 * s + 5, ' = 3', c.derived, 14, 'end', 'F_y');
          }
          b.force(ox, oy, ox + 4 * s, oy - 3 * s, c.load, 3.0);
          b.text(ox + 2 * s + 12, oy - 1.5 * s - 14, ' = 5', c.load, 15, 'end', 'F');
        }}
      />
      <CardText text={L('Šipka síly je dlouhá 5 dílků (dílek = strana čtverečku).', 'Стрелка силы длиной 5 делений (деление = сторона клетки).')} />
      <Steps steps={[ns]} />
      {card.done && <LearnJump lesson={105} card={0} label={L('Proč 4² + 3² = 5²? → Trigonometrie: Pythagorova věta', 'Почему 4² + 3² = 5²? → Тригонометрия: теорема Пифагора')} />}
    </>
  );
}

/** Karta 4: kvíz – F = 10 kN pod 30° (F_x?), pak tentýž úhel od svislice (F_y?). */
function CheckCos() {
  const t = useTr();
  const solved = useCardState().solvedHere;
  const q1 = useQuizStep(
    L('[[F]] svírá s nosníkem 30°. Jak velká je vodorovná složka [[F_x]]?', '[[F]] составляет с балкой 30°. Чему равна горизонтальная [[F_x]]?'),
    [
      opt(L('8,66 kN', '8,66 кН'), true),
      opt(
        L('5 kN', '5 кН'),
        false,
        L('To je 10 · sin 30° – složka **naproti** úhlu, tedy svislá [[F_y]]. Kterou funkcí spočítáš složku u úhlu?', 'Это 10 · sin 30° — составляющая **напротив** угла, то есть вертикальная [[F_y]]. Какой функцией найти составляющую, прилежащую к углу?'),
      ),
      opt(L('10 kN', '10 кН'), false, L('Síla je šikmá, takže vodorovně míří jen její část. Vyjde víc, nebo míň než 10 kN?', 'Сила наклонная, поэтому горизонтально действует только её часть. Получится больше или меньше 10 кН?')),
      opt(L('11,5 kN', '11,5 кН'), false, L('Složka nikdy není větší než celá síla. Zkus 10 · cos 30°.', 'Составляющая никогда не больше всей силы. Попробуй 10 · cos 30°.')),
    ],
    L(
      '[[F_x]] leží u úhlu, proto kosinus: [[F_x]] = 10 · cos 30° = 8,66 kN. Složka naproti úhlu by šla přes sinus.',
      '[[F_x]] прилежит к углу, поэтому косинус: [[F_x]] = 10 · cos 30° = 8,66 кН. Составляющая напротив угла — через синус.',
    ),
    null,
    2,
  );
  const q2 = useQuizStep(
    L('Teď je úhel 60° měřený od **svislice**. Jak velká je svislá [[F_y]]?', 'Теперь угол 60° отсчитан от **вертикали**. Чему равна вертикальная [[F_y]]?'),
    [
      opt(L('5 kN', '5 кН'), true),
      opt(L('8,66 kN', '8,66 кН'), false, L('To je 10 · sin 60°. Která složka leží **u** úhlu 60° – svislá, nebo vodorovná?', 'Это 10 · sin 60°. Какая составляющая **прилежит** к углу 60° — вертикальная или горизонтальная?')),
      opt(L('10 kN', '10 кН'), false, L('Síla je šikmá, svisle míří jen její část.', 'Сила наклонная, вертикально действует лишь её часть.')),
    ],
    L(
      'Úhel 60° leží u svislé složky, proto kosinus: [[F_y]] = 10 · cos 60° = 5 kN – vyšlo totéž co 10 · sin 30°. Nepamatuj si „x = cos“: cos patří vždy ke složce **u úhlu**, ať je úhel měřený odkudkoli.',
      'Угол 60° прилежит к вертикальной составляющей, поэтому косинус: [[F_y]] = 10 · cos 60° = 5 кН — то же, что 10 · sin 30°. Не запоминай «x = cos»: cos всегда относится к составляющей, **прилежащей к углу**, откуда бы угол ни отсчитывали.',
    ),
    null,
    3,
  );
  return (
    <>
      <Stage
        h={210}
        description={t.pick(L('Síla 10 kN pod úhlem 30° k nosníku', 'Сила 10 кН под углом 30° к балке'))}
        draw={(b, c) => {
          b.beam(20, 380, 175);
          inclined(b, 300, 170, 30, 1, solved ? 1 : 0, true, true);
          // tentýž úhel měřený od svislice: 60° – ukáže se až u druhé otázky
          if (q1.done) {
            const tx = 300;
            const ty = 170;
            b.line(tx, ty, tx, ty - 120, c.inkSoft, 1.0, true);
            b.arc(tx, ty, 50, -90, -60, c.inkSoft, 1.0);
            b.text(tx - 22, ty - 62, '60°', c.inkSoft, 12, 'end');
          }
        }}
      />
      <CardText text={L('[[F]] = 10 kN. Hodnoty: cos 30° ≈ 0,866 · sin 30° = 0,5 · cos 60° = 0,5 · sin 60° ≈ 0,866.', '[[F]] = 10 кН. Значения: cos 30° ≈ 0,866 · sin 30° = 0,5 · cos 60° = 0,5 · sin 60° ≈ 0,866.')} />
      <Steps steps={[q1, q2]} />
    </>
  );
}

export const cards = [TwoRopes, ReplaceToggle, Grid345, AngleComponents, CheckCos];
