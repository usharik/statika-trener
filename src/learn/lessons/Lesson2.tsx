/*
 * Lekce 2 – Rovnováha sil. Nevyrovnaná síla těleso rozjíždí se zrychlením (a = ΣF / m), při ΣF = 0 se rychlost
 * nemění. Když těleso stojí, síly na něj působí, ale navzájem se vyruší: ΣF_x = 0 a ΣF_y = 0, každý směr zvlášť.
 * (Přepis z aplikace: learn/Lesson2.kt.)
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
  ValueSlider,
  alpha,
  clamp,
  num,
  numSpec,
  opt,
  step,
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

/** 12 jednotek na 1 kN – při 8 kN šipka zůstane v obrázku. */
const K = 12;
/** 1 m dráhy = 20 jednotek obrázku. */
const M = 20;

/** Vozík na kolečkách (tření zanedbáme): obdélník se středem cx a horní hranou top. */
function cart(b: PrimBuilder, cx: number, top: number, w: number, h: number, c: Colors, a = 1, ghost = false) {
  if (ghost) {
    b.rect(cx - w / 2, top, w, h, null, c.inkSoft, 1.0, a);
    return;
  }
  b.rect(cx - w / 2, top, w, h, c.subtle, c.ink, 1.6, a);
  b.circle(cx - w / 3, top + h + 6, 6, c.paper, c.ink, 1.5, a);
  b.circle(cx + w / 3, top + h + 6, 6, c.paper, c.ink, 1.5, a);
}

/**
 * Karta 0: přetahovaná. Vyrovnané síly → vozík stojí. Nevyrovnané → vozík se rozjíždí se zrychlením:
 * záblesky (stroboskop) po 0,5 s jsou čím dál dál od sebe a šipka rychlosti roste. a = ΣF / m.
 */
function TugOfWar() {
  const t = useTr();
  const kit = useKit();
  const left = kit.mut(3);
  const right = kit.mut(5);
  const reached = kit.mut(false);
  const sawRun = kit.mut(false);
  const res = right.v - left.v;
  const balanced = Math.abs(res) < 0.01;
  const mass = 2; // t
  const acc = res / mass; // m/s²
  const x = kit.mut(0);
  const v = kit.mut(0);
  const fade = kit.mut(1);
  const ghosts = kit.list<number>([]);
  const runId = kit.mut(0);
  const running = kit.mut(false);
  const lastRes = kit.mut<number | null>(null); // Σ F_x posledního puštění
  const leftSet = kit.mut(false); // krok 2: levý tým přešel na 4,5 kN
  const stillRun = kit.mut(false); // poslední pokus byl vyrovnaný a vozík opravdu stál
  useLaunched([runId.v], async (job) => {
    if (runId.v === 0) return;
    const a = acc;
    lastRes.v = res;
    running.v = true;
    stillRun.v = false;
    x.v = 0;
    v.v = 0;
    ghosts.clear();
    for (let i = 1; i <= 6; i++) {
      fade.v = i / 6;
      await job.delay(25);
    }
    let time = 0;
    let last = 0;
    let nextGhost = 0;
    while (Math.abs(x.v) * M <= 150 && time < 3.0) {
      const now = await job.frame();
      const dt = last === 0 ? 0 : (now - last) / 1000;
      last = now;
      time += dt;
      x.v = 0.5 * a * time * time;
      v.v = a * time;
      if (time >= nextGhost) {
        ghosts.add(x.v);
        nextGhost += 0.5;
      }
    }
    if (Math.abs(a) < 0.01) {
      // vyrovnané síly: 3 s a záblesky pořád na jednom místě
      stillRun.v = true;
      reached.v = true;
    } else {
      sawRun.v = true;
      // dojel na kraj obrázku: pokus skončil, vozík zmizí (stopa zůstane, dokud nezačne další pokus)
      for (let i = 1; i <= 10; i++) {
        fade.v = 1 - i / 10;
        await job.delay(30);
      }
      await job.delay(250);
      x.v = 0;
      v.v = 0;
      for (let i = 1; i <= 6; i++) {
        fade.v = i / 6;
        await job.delay(25);
      }
    }
    running.v = false;
  });
  const fresh = () => {
    // nové nastavení = nový pokus: starý vozík i stopa zmizí, nový stojí na startu
    stillRun.v = false;
    lastRes.v = null;
    x.v = 0;
    v.v = 0;
    ghosts.clear();
    fade.v = 1;
  };
  // krok 2: po prvním pokusu (až dojede a objeví se nový vozík) táhne levý tým 4,5 kN
  useLaunched([sawRun.v, running.v], async (job) => {
    if (sawRun.v && !running.v && !leftSet.v) {
      await job.delay(400);
      left.v = 4.5;
      leftSet.v = true;
      fresh();
    }
  });
  const sumLine = () => {
    const r = lastRes.v;
    if (r === null) return null;
    return Math.abs(r) < 0.01 ? (
      <Readout text={`Σ[[F_x]] = ${num(right.v, 1)} − ${num(left.v, 1)} = 0`} ok />
    ) : (
      <Readout text={`Σ[[F_x]] = ${num(right.v, 1)} − ${num(left.v, 1)} = ${num(r, 1)} kN → a = ${num(Math.abs(r / mass), 2)} m/s²`} />
    );
  };
  const release = () => <ActionButton text={L('Pusť vozík', 'Отпустить тележку')} enabled={!running.v} onClick={() => runId.v++} />;
  const rightSlider = () => (
    <ValueSlider
      label={t.pick(L(`Pravý tým [[F_2]] = ${num(right.v, 1)} kN`, `Правая команда [[F_2]] = ${num(right.v, 1)} кН`))}
      value={right.v}
      min={1}
      max={8}
      steps={13}
      onChange={(it) => {
        if (!running.v) {
          right.v = it;
          fresh();
        }
      }}
    />
  );
  const quiz = useQuizStep(
    L('Vozík stojí. Působí na něj teď nějaké síly?', 'Тележка стоит. Действуют ли на неё сейчас какие-то силы?'),
    [
      opt(L('ne, žádné', 'нет, никаких'), false, L('Podívej se na lana – oba týmy pořád táhnou silou 4,5 kN.', 'Посмотри на верёвки — обе команды по-прежнему тянут с силой 4,5 кН.')),
      opt(L('ano, dvě stejně velké proti sobě', 'да, две равные навстречу'), true),
      opt(L('jen ta větší', 'только бóльшая'), false, L('Žádná není větší – obě mají 4,5 kN. Kdyby jedna převažovala, vozík by se rozjel.', 'Ни одна не больше — обе по 4,5 кН. Если бы одна перевешивала, тележка бы поехала.')),
    ],
    L(
      'Tomu se říká **rovnováha**: síly působí dál, jen se navzájem vyruší. Rovnováha ≠ žádné síly.',
      'Это и есть **равновесие**: силы по-прежнему действуют, просто уравновешивают друг друга. Равновесие ≠ отсутствие сил.',
    ),
  );
  return (
    <>
      <Stage
        h={210}
        description={t.pick(L('Vozík tažený dvěma silami proti sobě', 'Тележка, которую тянут две силы в разные стороны'))}
        draw={(b, c) => {
          const cy = 92;
          const floor = cy + 36;
          b.line(10, floor, 390, floor, c.ink, 1.5);
          // stroboskop: kde byl vozík každou půlsekundu
          const gs = running.v ? ghosts.v.slice(0, -1) : ghosts.v;
          for (const g of gs) {
            cart(b, 200 + g * M, cy - 24, 60, 48, c, 0.3, true);
            b.line(200 + g * M, floor + 4, 200 + g * M, floor + 12, c.inkSoft, 1.2);
          }
          const cx = 200 + x.v * M;
          cart(b, cx, cy - 24, 60, 48, c, fade.v);
          // síly táhnou ven z vozíku
          const lEnd = cx - 30 - left.v * K;
          const rEnd = cx + 30 + right.v * K;
          b.force(cx - 30, cy, lEnd, cy, c.load, 3.0, 11, false, fade.v);
          b.text(lEnd, cy - 34, ` = ${num(left.v, 1)} kN`, c.load, 14, 'start', 'F_1', false, fade.v);
          b.force(cx + 30, cy, rEnd, cy, c.load, 3.0, 11, false, fade.v);
          b.text(rEnd, cy - 34, ` = ${num(right.v, 1)} kN`, c.load, 14, 'end', 'F_2', false, fade.v);
          // rychlost: zelená šipka nad vozíkem, roste
          if (Math.abs(v.v) > 0.05) {
            b.force(cx, cy - 46, cx + v.v * 8, cy - 46, c.accent, 2.4, 11, false, fade.v);
            b.text(cx + v.v * 4, cy - 56, `v = ${num(Math.abs(v.v), 1)} m/s`, c.accent, 12, 'middle', null, false, fade.v);
          }
          const shown = running.v || ghosts.size > 0;
          if (shown && !balanced) {
            b.text(
              200,
              floor + 34,
              t
                .pick(
                  res > 0
                    ? L(`převaha ${num(Math.abs(res), 1)} kN doprava → vozík **zrychluje**`, `перевес ${num(Math.abs(res), 1)} кН вправо → тележка **разгоняется**`)
                    : L(`převaha ${num(Math.abs(res), 1)} kN doleva → vozík **zrychluje**`, `перевес ${num(Math.abs(res), 1)} кН влево → тележка **разгоняется**`),
                )
                .replace(/\*\*/g, ''),
              c.derived,
              13,
            );
            b.text(200, floor + 54, t.pick(L('čárky: kde byl vozík každou půlsekundu', 'штрихи: где была тележка каждые полсекунды')), c.inkSoft, 11);
          } else if (shown) {
            b.text(200, floor + 40, t.pick(stillRun.v ? L('rovnováha – vozík pořád stojí', 'равновесие — тележка всё ещё стоит') : L('síly jsou stejné', 'силы одинаковые')), c.accent, 16, 'middle', null, true);
            if (stillRun.v) b.text(200, floor + 58, t.pick(L('všechny záblesky na jednom místě', 'все вспышки в одном месте')), c.inkSoft, 11);
          }
        }}
      />
      <CardText
        text={L(
          'Dva týmy táhnou vozík (2 t, kolečka bez tření) každý na svou stranu. Záblesky ukazují, kde byl vozík každou půlsekundu. Σ[[F_x]] = součet vodorovných sil: doprava +, doleva −.',
          'Две команды тянут тележку (2 т, колёса без трения) каждая в свою сторону. Вспышки показывают, где была тележка каждые полсекунды. Σ[[F_x]] — сумма горизонтальных сил: вправо +, влево −.',
        )}
      />
      <Steps
        steps={[
          step(
            L('Pusť vozík: vpravo táhne 5 kN, vlevo 3 kN', 'Отпусти тележку: вправо тянут 5 кН, влево — 3 кН'),
            sawRun.v,
            () => (
              <>
                {sumLine()}
                {release()}
              </>
            ),
            () => (
              <SmallText
                text={L(
                  'Zrychlení a = Σ[[F]] / m: převaha 5 − 3 = 2 kN rozjíždí dvoutunový vozík o 2 / 2 = 1 m/s každou sekundu. Proto jsou čárky čím dál dál od sebe.',
                  'Ускорение a = Σ[[F]] / m: перевес 5 − 3 = 2 кН разгоняет двухтонную тележку на 2 / 2 = 1 м/с каждую секунду. Поэтому штрихи всё дальше друг от друга.',
                )}
              />
            ),
          ),
          step(L('Vlevo táhne 4,5 kN. Nastav pravý tým tak, aby vozík zůstal stát', 'Слева тянут 4,5 кН. Подбери силу правой команды, чтобы тележка стояла'), reached.v, () => (
            <>
              {rightSlider()}
              {sumLine()}
              {release()}
              {lastRes.v !== null && !running.v && Math.abs(lastRes.v) >= 0.01 && (
                <SmallText
                  text={
                    lastRes.v > 0
                      ? L('Převaha doprava – vozík se rozjel doprava. Zmenši [[F_2]].', 'Перевес вправо — тележка поехала вправо. Уменьши [[F_2]].')
                      : L('Převaha doleva – vozík se rozjel doleva. Zvětši [[F_2]].', 'Перевес влево — тележка поехала влево. Увеличь [[F_2]].')
                  }
                />
              )}
            </>
          )),
          quiz,
        ]}
        after={() => (
          <>
            <ValueSlider
              label={t.pick(L(`Levý tým [[F_1]] = ${num(left.v, 1)} kN`, `Левая команда [[F_1]] = ${num(left.v, 1)} кН`))}
              value={left.v}
              min={1}
              max={8}
              steps={13}
              onChange={(it) => {
                if (!running.v) {
                  left.v = it;
                  fresh();
                }
              }}
            />
            {rightSlider()}
            {sumLine()}
            {release()}
          </>
        )}
      />
    </>
  );
}

/**
 * Karta 1: rovnováha neznamená jen „stojí“. Vozík na ledě se 1,5 s rozjíždí silou, pak síla zmizí a vozík jede
 * dál stejně rychle – záblesky jsou stejně daleko od sebe. ΣF = 0 ⇔ rychlost se nemění. Statika řeší klid.
 */
function Coast() {
  const t = useTr();
  const kit = useKit();
  const ran = kit.mut(false);
  const guess = kit.mut<number | null>(null); // tip před pokusem: 0 zastaví se, 1 stejně rychle, 2 zrychluje
  const running = kit.mut(false);
  const x = kit.mut(0);
  const v = kit.mut(0);
  const pushing = kit.mut(false);
  const ghosts = kit.list<[number, boolean]>([]); // poloha a „ještě tlačil“
  const push = 1.5; // s
  const acc = 2.0; // m/s² (2 kN na 1 t)
  const x0 = 40;
  const start = () => {
    if (running.v) return;
    running.v = true;
    kit.scope.launch(async (job) => {
      x.v = 0;
      v.v = 0;
      ghosts.clear();
      let time = 0;
      let last = 0;
      let nextGhost = 0;
      // vozík jede, dokud nezmizí za pravým okrajem (nikde se sám nezastaví)
      while (x0 + x.v * M < 400) {
        const now = await job.frame();
        const dt = last === 0 ? 0 : (now - last) / 1000;
        last = now;
        time += dt;
        pushing.v = time < push;
        if (pushing.v) {
          x.v = 0.5 * acc * time * time;
          v.v = acc * time;
        } else {
          v.v = acc * push;
          x.v = 0.5 * acc * push * push + v.v * (time - push);
        }
        if (time >= nextGhost) {
          ghosts.add([x.v, pushing.v]);
          nextGhost += 0.25;
        }
      }
      pushing.v = false;
      ran.v = true;
      await job.delay(600);
      running.v = false;
    });
  };
  const options = [L('postupně se zastaví', 'постепенно остановится'), L('pojede dál stejně rychle', 'поедет дальше с той же скоростью'), L('bude dál zrychlovat', 'будет и дальше разгоняться')];
  const g = guess.v ?? 1;
  return (
    <>
      <Stage
        h={180}
        description={t.pick(L('Vozík na ledě: nejdřív ho síla rozjíždí, pak jede sám', 'Тележка на льду: сначала сила её разгоняет, потом она едет сама'))}
        draw={(b, c) => {
          const cy = 70;
          const floor = cy + 30;
          b.rect(10, floor, 380, 6, alpha(c.react, 0.12), null);
          b.line(10, floor, 390, floor, c.ink, 1.5);
          for (const [gp, p] of ghosts.v) {
            const gx = x0 + gp * M;
            b.line(gx, floor + 8, gx, floor + 18, p ? c.load : c.accent, 1.6);
          }
          const cx = x0 + x.v * M;
          // u pravého okraje se vozík rozplyne – jede dál, jen už mimo obrázek
          const al = clamp((392 - cx) / 40, 0, 1);
          if (running.v || !ran.v) cart(b, cx, cy - 20, 44, 40, c, al);
          if (pushing.v) {
            b.force(cx - 22 - 40, cy, cx - 22, cy, c.load, 3.0);
            b.text(cx - 44, cy - 14, ' = 2 kN', c.load, 13, 'middle', 'F');
          }
          if (v.v > 0.05 && al > 0.3 && running.v) {
            b.force(cx, cy - 32, cx + v.v * 12, cy - 32, c.accent, 2.2, 11, false, al);
            b.text(cx + v.v * 6, cy - 40, `v = ${num(v.v, 1)} m/s`, c.accent, 12, 'middle', null, false, al);
          }
          if (ran.v && !running.v) b.text(380, cy, t.pick(L('…a jede dál →', '…и едет дальше →')), c.accent, 12, 'end');
          if (ghosts.size > 0) {
            b.text(16, floor + 38, t.pick(L('červeně: síla působí – čárky se vzdalují', 'красные: сила действует — штрихи раздвигаются')), c.load, 11, 'start');
            b.text(16, floor + 54, t.pick(L('zeleně: žádná síla – čárky stejně daleko', 'зелёные: силы нет — штрихи на равных расстояниях')), c.accent, 11, 'start');
          }
        }}
      />
      <CardText
        text={L(
          'Vozík (1 t) stojí na hladkém ledě, tření zanedbáme. Síla 2 kN ho 1,5 s rozjíždí a pak přestane působit. Čárky ukazují, kde byl každou čtvrtsekundu.',
          'Тележка (1 т) стоит на гладком льду, трением пренебрегаем. Сила 2 кН разгоняет её 1,5 с и перестаёт действовать. Штрихи показывают, где она была каждую четверть секунды.',
        )}
      />
      <Steps
        steps={[
          step(
            L('Tipni si: co vozík udělá, až síla přestane působit?', 'Как думаешь: что сделает тележка, когда сила перестанет действовать?'),
            guess.v !== null,
            () => <Chips options={options} selected={guess.v} onPick={(i) => guess.v === null && (guess.v = i)} />,
            () => (guess.v !== null ? <SmallText text={L(`Tvůj tip: **${options[guess.v].cs}**`, `Твой прогноз: **${options[guess.v].ru}**`)} /> : null),
          ),
          step(
            L('Spusť pokus a sleduj rozestupy čárek po 1,5 s', 'Запусти опыт и следи за расстоянием между штрихами после 1,5 с'),
            ran.v,
            () => <ActionButton text={L('Spusť pokus', 'Запустить опыт')} enabled={!running.v} onClick={start} />,
            () => (
              <Note
                tone={g === 1 ? 'ok' : 'info'}
                text={L(
                  (g === 1
                    ? 'Správně – zelené čárky jsou stejně daleko od sebe: rychlost se nemění.'
                    : g === 0
                      ? 'Nezastavil se. Zastavit by ho musela síla proti pohybu – tření nebo brzda. Tady žádná není, zelené čárky jsou stejně daleko od sebe.'
                      : 'Nezrychluje. Zrychlovat ho může jen nevyrovnaná síla – a ta zmizela. Zelené čárky jsou stejně daleko od sebe.') +
                    ' Když je Σ[[F]] = 0, rychlost se **nemění**: těleso stojí, nebo jede rovnoměrně přímočaře (1. Newtonův zákon). Statika řeší ten klidný případ: všechno stojí a Σ[[F]] = 0.',
                  (g === 1
                    ? 'Верно — зелёные штрихи на равных расстояниях: скорость не меняется.'
                    : g === 0
                      ? 'Не остановилась. Чтобы её остановить, нужна сила против движения — трение или тормоз. Здесь её нет, зелёные штрихи на равных расстояниях.'
                      : 'Не разгоняется. Разгонять её может только неуравновешенная сила — а она исчезла. Зелёные штрихи на равных расстояниях.') +
                    ' Когда Σ[[F]] = 0, скорость **не меняется**: тело стоит или движется равномерно и прямолинейно (1-й закон Ньютона). Статика изучает спокойный случай: всё стоит и Σ[[F]] = 0.',
                )}
              />
            ),
          ),
        ]}
        after={() => <ActionButton text={L('Přehrát znovu', 'Повторить')} enabled={!running.v} onClick={start} />}
      />
    </>
  );
}

/**
 * Karta 2: bedna na stole a ruka, která ji shora přitlačuje. Síla stolu není „tíha“ – je taková,
 * jakou vyžaduje rovnováha: N = G + P. Všechny síly leží na jedné svislé nositelce.
 */
function TableForce() {
  const t = useTr();
  const solved = useCardState().solvedHere;
  const ns = useNumStep(
    L('Jak velkou silou [[N]] tlačí stůl bednu nahoru?', 'С какой силой [[N]] стол давит на ящик вверх?'),
    [numSpec(L('[[N]]', '[[N]]'), 8, t.pick(L('kN', 'кН')))],
    L(
      'Bedna stojí, takže Σ[[F_y]] = 0 (nahoru +): [[N]] − 5 − 3 = 0, tedy [[N]] = 8 kN. Síla stolu (podpory) **není automaticky rovna tíze** – je přesně taková, jakou vyžaduje rovnováha. Přesně tak budeme počítat reakce nosníků.',
      'Ящик стоит, значит Σ[[F_y]] = 0 (вверх +): [[N]] − 5 − 3 = 0, то есть [[N]] = 8 кН. Сила стола (опоры) **не обязательно равна весу** — она ровно такая, какую требует равновесие. Именно так мы будем считать реакции балок.',
    ),
    [
      trapNear(L('To je jen tíha. Ruka ale tlačí bednu dolů taky – stůl se prohne víc a víc tlačí.', 'Это только сила тяжести. Но рука тоже давит ящик вниз — стол прогнётся сильнее и будет давить сильнее.'), 5),
      trapNear(L('Ruka tlačí dolů, ne nahoru – síly dolů se sčítají.', 'Рука давит вниз, а не вверх — силы вниз складываются.'), 2),
      trapNear(L('Stůl nese i tíhu bedny, nejen ruku.', 'Стол держит и вес ящика, а не только руку.'), 3),
    ],
    L('Bedna stojí: síly nahoru = síly dolů. Které síly tlačí bednu dolů?', 'Ящик стоит: силы вверх = силы вниз. Какие силы давят ящик вниз?'),
  );
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Bedna na stole, shora ji tlačí ruka; tíha a ruka dolů, stůl nahoru', 'Ящик на столе, сверху его давит рука; сила тяжести и рука вниз, стол вверх'))}
        draw={(b, c) => {
          const k = 8; // délka šipky na 1 kN
          const top = 140; // horní plocha stolu
          const floor = 222;
          b.line(40, floor, 360, floor, c.ink, 1.5);
          b.rect(90, top, 220, 10, c.beam, c.ink, 1.6);
          b.rect(102, top + 10, 8, floor - top - 10, c.beam, c.ink, 1.4);
          b.rect(290, top + 10, 8, floor - top - 10, c.beam, c.ink, 1.4);
          b.rect(165, top - 80, 70, 80, c.subtle, c.ink, 1.5);
          b.circle(200, top - 40, 2.5, c.ink);
          // ruka shora
          b.rect(180, top - 80 - 3 * k - 22, 40, 14, c.paper, c.ink, 1.3);
          for (let f = 0; f <= 3; f++) b.line(184 + f * 10, top - 80 - 3 * k - 22, 184 + f * 10, top - 80 - 3 * k - 30, c.ink, 1.3);
          // jedna svislá nositelka x = 200: P shora na bednu, G z těžiště dolů, N zdola do styku
          b.force(200, top - 80 - 3 * k - 6, 200, top - 80, c.load, 2.6);
          b.text(210, top - 80 - 1.5 * k, ' = 3 kN', c.load, 13, 'start', 'P');
          b.force(200, top - 40, 200, top - 40 + 5 * k, c.load, 2.6);
          b.text(210, top - 18, ' = 5 kN', c.load, 13, 'start', 'G');
          b.force(200, top + 8 * k, 200, top, c.react, 2.6);
          b.text(210, top + 4 * k + 10, solved ? ' = 8 kN' : ' = ?', c.react, 14, 'start', 'N');
        }}
      />
      <CardText text={L('Bedna (tíha [[G]] = 5 kN) leží na stole a ruka ji shora přitlačuje silou [[P]] = 3 kN. Bedna stojí.', 'Ящик (сила тяжести [[G]] = 5 кН) лежит на столе, рука прижимает его сверху силой [[P]] = 3 кН. Ящик стоит.')} />
      <Steps steps={[ns]} />
    </>
  );
}

/** Karta 3: vodorovně a svisle zvlášť – doplň chybějící sílu. */
function TwoAxes() {
  const t = useTr();
  const solved = useCardState().solvedHere;
  const q = useQuizStep(
    L('Jaká síla patří na místo otazníku?', 'Какая сила должна стоять на месте знака вопроса?'),
    [
      opt(L('4 kN ←', '4 кН ←'), true),
      opt(
        L('4 kN →', '4 кН →'),
        false,
        L('Pak by obě vodorovné síly táhly doprava. Na kterou stranu musí chybějící síla mířit, aby se vyrušily?', 'Тогда обе горизонтальные силы тянули бы вправо. В какую сторону должна быть направлена недостающая сила, чтобы они уравновесились?'),
      ),
      opt(L('3 kN ↓', '3 кН ↓'), false, L('Svisle už je rovnováha: 3 nahoru, 3 dolů. Co ještě nesedí ve vodorovném směru?', 'По вертикали уже равновесие: 3 вверх, 3 вниз. Что ещё не сходится по горизонтали?')),
      opt(
        L('10 kN ←', '10 кН ←'),
        false,
        L('Svislé síly (3 a 3) se už vyrušily, ty sem nepočítej. Jak velká vodorovná síla vyrovná [[F_1]] = 4 kN?', 'Вертикальные силы (3 и 3) уже уравновешены, их сюда не прибавляй. Какая горизонтальная сила уравновесит [[F_1]] = 4 кН?'),
      ),
    ],
    L(
      'Vodorovně a svisle počítáme **zvlášť**. Σ[[F_y]] = 3 − 3 = 0 už platí; aby vyšlo i Σ[[F_x]] = 4 − [[F_4]] = 0, musí [[F_4]] = 4 kN mířit doleva.',
      'По горизонтали и по вертикали считаем **отдельно**. Σ[[F_y]] = 3 − 3 = 0 уже выполняется; чтобы и Σ[[F_x]] = 4 − [[F_4]] = 0, нужна [[F_4]] = 4 кН влево.',
    ),
    null,
    2,
  );
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Bedna se silami ve dvou směrech a jedna síla chybí', 'Ящик с силами в двух направлениях, одной силы не хватает'))}
        draw={(b, c) => {
          const cx = 200;
          const cy = 115;
          b.rect(cx - 32, cy - 32, 64, 64, c.subtle, c.ink, 1.6);
          b.force(cx + 32, cy, cx + 32 + 4 * 18, cy, c.load);
          b.text(cx + 40, cy - 10, ' = 4 kN', c.load, 13, 'start', 'F_1');
          b.force(cx, cy - 32, cx, cy - 32 - 3 * 18, c.load);
          b.text(cx + 8, cy - 70, ' = 3 kN', c.load, 13, 'start', 'F_2');
          b.force(cx, cy + 32, cx, cy + 32 + 3 * 18, c.load);
          b.text(cx + 8, cy + 78, ' = 3 kN', c.load, 13, 'start', 'F_3');
          if (solved) {
            // doplněná síla 4 kN doleva
            b.force(cx - 32, cy, cx - 32 - 4 * 18, cy, c.react);
            b.text(cx - 40, cy - 10, ' = 4 kN', c.react, 13, 'end', 'F_4');
          } else {
            b.line(cx - 110, cy, cx - 34, cy, c.react, 1.4, true);
            b.text(cx - 70, cy - 12, '?', c.react, 24, 'middle', null, true);
          }
        }}
      />
      <CardText text={L('Bedna má zůstat stát. Jedna síla na obrázku chybí.', 'Ящик должен остаться на месте. Одной силы на рисунке не хватает.')} />
      <Steps steps={[q]} />
    </>
  );
}

/**
 * Karta 4: kvíz – výtah jede nahoru stálou rychlostí. Záblesky stejně daleko od sebe → Σ F = 0 → lano táhne
 * stejně, jako je tíha. Ověří, že „pohyb“ ≠ „převaha síly“.
 */
function CheckStill() {
  const t = useTr();
  const kit = useKit();
  const solved = useCardState().solvedHere;
  // kabina jede pořád dokola nahoru stálou rychlostí; záblesky každou sekundu
  const y = kit.mut(0);
  useLaunched([], async (job) => {
    let last = 0;
    for (;;) {
      const now = await job.frame();
      const dt = last === 0 ? 0 : (now - last) / 1000;
      last = now;
      y.v = (y.v + dt * 22) % 110;
    }
  });
  const q = useQuizStep(
    L('Jak velkou silou [[S]] táhne lano?', 'С какой силой [[S]] тянет трос?'),
    [
      opt(
        L('víc než 8 kN – jinak by nejel nahoru', 'больше 8 кН — иначе он не ехал бы вверх'),
        false,
        L('Převaha síly by výtah **zrychlovala** – záblesky by se rozestupovaly. Jede ale stále stejně rychle.', 'Перевес силы **разгонял** бы лифт — вспышки расходились бы. А он едет с постоянной скоростью.'),
      ),
      opt(L('přesně 8 kN', 'ровно 8 кН'), true),
      opt(L('méně než 8 kN', 'меньше 8 кН'), false, L('Pak by převažovala tíha a výtah by zpomaloval.', 'Тогда перевешивала бы сила тяжести, и лифт бы тормозил.')),
    ],
    L(
      'Stálá rychlost = rovnováha: Σ[[F_y]] = [[S]] − 8 = 0, tedy [[S]] = 8 kN. Síla je potřeba ke **změně** rychlosti, ne k samotnému pohybu. Rovnováha neznamená „žádné síly“ ani „nic se nehýbe“ – znamená, že se síly vyruší.',
      'Постоянная скорость = равновесие: Σ[[F_y]] = [[S]] − 8 = 0, то есть [[S]] = 8 кН. Сила нужна, чтобы **менять** скорость, а не чтобы просто двигаться. Равновесие не значит «нет сил» или «ничего не движется» — значит, что силы уравновешены.',
    ),
  );
  return (
    <>
      <Stage
        h={230}
        description={t.pick(L('Výtah jede stálou rychlostí nahoru, lano a tíha', 'Лифт едет вверх с постоянной скоростью, трос и сила тяжести'))}
        draw={(b, c) => {
          // šachta
          b.line(140, 8, 140, 222, c.inkSoft, 1.2);
          b.line(260, 8, 260, 222, c.inkSoft, 1.2);
          // záblesky: stejně daleko od sebe
          for (let k = 0; k <= 4; k++) {
            const gy = 190 - k * 22;
            b.line(126, gy, 138, gy, c.accent, 1.6);
          }
          b.text(132, 212, t.pick(L('záblesky po 1 s', 'вспышки через 1 с')), c.accent, 11, 'end');
          const ky = 150 - y.v;
          const a = y.v < 10 ? y.v / 10 : y.v > 100 ? (110 - y.v) / 10 : 1;
          b.line(200, 8, 200, ky - 30, c.ink, 1.6, false, a);
          b.rect(165, ky - 30, 70, 60, c.subtle, c.ink, 1.5, a);
          // síly na jedné svislé nositelce: lano nahoru z horní hrany, tíha dolů z těžiště
          b.force(200, ky - 30, 200, ky - 30 - 40, c.react, 2.6, 11, false, a);
          b.text(208, ky - 52, solved ? ' = 8 kN' : ' = ?', c.react, 13, 'start', 'S', false, a);
          b.force(200, ky, 200, ky + 40, c.load, 2.6, 11, false, a);
          b.text(208, ky + 46, ' = 8 kN', c.load, 13, 'start', 'G', false, a);
          b.force(282, ky + 15, 282, ky - 15, c.accent, 2.0, 11, false, a);
          b.text(290, ky + 5, t.pick(L('v = stálá', 'v = const')), c.accent, 12, 'start', null, false, a);
        }}
      />
      <CardText
        text={L(
          'Výtah (tíha [[G]] = 8 kN) jede nahoru **stálou rychlostí** – záblesky jsou stejně daleko od sebe.',
          'Лифт (сила тяжести [[G]] = 8 кН) едет вверх с **постоянной скоростью** — вспышки на равных расстояниях.',
        )}
      />
      <Steps steps={[q]} />
    </>
  );
}

export const cards = [TugOfWar, Coast, TableForce, TwoAxes, CheckStill];
