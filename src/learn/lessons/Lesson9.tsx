/*
 * Lekce 9 – Vnitřní kloub a statická určitost. Kloub přenese sílu, ale ne moment → M_K = 0 z jedné části;
 * rovnic je r = 3 + počet kloubů, neznámých n. n = r je nutná podmínka – podpory ale musí taky zabránit
 * každému pohybu. (Přepis z aplikace: learn/Lesson9.kt.)
 */
import type { ReactNode } from 'react';
import { Rich } from '../../components/Sym';
import {
  ActionButton,
  CardText,
  Chips,
  L,
  Note,
  SmallText,
  Stage,
  Steps,
  alpha as withAlpha,
  num,
  numSpec,
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
  type PrimBuilder,
  type QuizState,
  type Step,
} from '../kit';

function rot9(x: number, y: number, px: number, py: number, deg: number): [number, number] {
  const a = (deg * Math.PI) / 180;
  const dx = x - px;
  const dy = y - py;
  return [px + dx * Math.cos(a) - dy * Math.sin(a), py + dx * Math.sin(a) + dy * Math.cos(a)];
}

function part(b: PrimBuilder, x1: number, x2: number, y: number, px: number, deg: number, c: Colors, a = 1) {
  const pts = (
    [
      [x1, y - 5],
      [x2, y - 5],
      [x2, y + 5],
      [x1, y + 5],
    ] as [number, number][]
  ).map(([x, yy]) => rot9(x, yy, px, y, deg));
  b.poly(pts, c.beam, c.ink, 1.6, a);
}

function hinge(b: PrimBuilder, x: number, y: number, c: Colors, label = 'K', a = 1) {
  b.circle(x, y, 6.5, c.paper, c.ink, 1.8, a);
  // popisek nad nosníkem – dole jsou podpory a jejich písmena
  if (label) b.text(x, y - 14, label, c.ink, 16, 'middle', null, true, a);
}

/** Spojitý nosník, který za podporou [xB] plynule poklesne o [d] na konci (pevný spoj bez podpory C). */
function droopBeam(b: PrimBuilder, x1: number, x2: number, y: number, xB: number, d: number, c: Colors, a = 1) {
  const n = 30;
  const xs = Array.from({ length: n + 1 }, (_, i) => x1 + ((x2 - x1) * i) / n);
  const off = (x: number) => (x <= xB ? 0 : d * ((x - xB) / (x2 - xB)) ** 2);
  const topPts = xs.map((x): [number, number] => [x, y - 5 + off(x)]);
  const botPts = [...xs].reverse().map((x): [number, number] => [x, y + 5 + off(x)]);
  b.poly([...topPts, ...botPts], c.beam, c.ink, 1.6, a);
}

// Gerberův nosník: A (0 m, pevná), B (3 m, posuvná), K (4 m, kloub), C (6 m, posuvná); 1 m = 52 jednotek
const X0 = 50;
const MM = 52;
const gx = (m: number) => X0 + m * MM;

/** Kvíz jako krok; [more] = navíc do výkladu po vyřešení. */
function quizStep9(q: QuizState, pickL: (l: L) => string, columns = 1, label: L = q.question, more?: () => ReactNode): Step {
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

/** Číselná odpověď jako krok. */
function numStep9(n: NumState, label: L): Step {
  return {
    label,
    done: n.solved,
    result: () => n.Feedback(),
    controls: () => (
      <>
        {n.Fields()}
        {n.Buttons()}
        {!n.solved && n.Feedback()}
      </>
    ),
  };
}

/** Tip před pokusem: volba + potvrzení. */
function useTip9(label: L, options: L[], guess: number | null, onGuess: (i: number) => void): Step {
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

/** Rovnice ve výkladu; [dim] = ta „horší“ varianta. */
function EqLine({ text, note, dim = false }: { text: string; note: L; dim?: boolean }) {
  const t = useTr();
  return (
    <div className={'eq-note' + (dim ? ' dim' : '')}>
      <div className="eq-note-eq">
        <Rich text={text} />
      </div>
      <div className="eq-note-sub">
        <Rich text={t.pick(note)} />
      </div>
    </div>
  );
}

/**
 * Karta 0: kloub přenese sílu, ale ne moment. Po odebrání C se pravá část s kloubem otočí kolem K čím dál
 * rychleji a skončí svisle pověšená (tlumené kyvadlo). S tuhým spojem se nosník jen pružně prohne.
 */
function HingeTest() {
  const t = useTr();
  const kit = useKit();
  const rigid = kit.mut(0); // 0 = kloub, 1 = pevný spoj
  const guess = kit.mut<number | null>(null);
  const guess2 = kit.mut<number | null>(null);
  const removed = kit.mut(false);
  const settled = kit.mut(false);
  const tried = kit.mut<Set<number>>(new Set());
  // kloub: úhel pravé části (rad, dolů kladně); tuhý spoj: poměrný průhyb 0…1 (kmitá kolem 1)
  const th = kit.mut(0);
  const droop = kit.mut(0);
  const alpha = kit.anim(1);
  useLaunched([removed.v, rigid.v], async (job) => {
    if (!removed.v) return;
    settled.v = false;
    let w = 0;
    let v = 0;
    let last = 0;
    let calm = 0;
    const rg = rigid.v;
    for (;;) {
      const now = await job.frame();
      const dt = last === 0 ? 0 : Math.min((now - last) / 1000, 0.05);
      last = now;
      // malé kroky: stabilní integrace i při trhaném snímkování
      let thv = th.v;
      let dr = droop.v;
      for (let i = 0; i < 8; i++) {
        const h = dt / 8;
        if (rg === 0) {
          // tíha + F k bodu K: moment ∝ cos θ; tlumení v kloubu
          const acc = 14 * Math.cos(thv) - 1.6 * w;
          w += acc * h;
          thv += w * h;
        } else {
          const acc = 90 * (1 - dr) - 4 * v;
          v += acc * h;
          dr += v * h;
        }
      }
      th.v = thv;
      droop.v = dr;
      calm = (rg === 0 && Math.abs(w) < 0.05 && Math.abs(thv - Math.PI / 2) < 0.03) || (rg === 1 && Math.abs(v) < 0.02 && Math.abs(dr - 1) < 0.01) ? calm + dt : 0;
      if (calm > 0.3) {
        settled.v = true;
        tried.v = new Set([...tried.v, rg]);
        break;
      }
    }
  });
  /** nový pokus: starý stav zmizí, nový (s podporou C) se objeví – nic se „nevrací“ samo */
  const reset = (newRigid: number = rigid.v) => {
    kit.scope.launch(async () => {
      await alpha.animateTo(0, tween(250));
      removed.v = false;
      settled.v = false;
      rigid.v = newRigid;
      th.v = 0;
      droop.v = 0;
      await alpha.animateTo(1, tween(300));
    });
  };
  // po pokusu s kloubem: nový obrázek s tuhým spojem a podporou C (pro druhý tip a pokus)
  useLaunched([tried.v.has(0)], async (job) => {
    if (tried.v.has(0) && !tried.v.has(1) && rigid.v === 0) {
      await job.delay(1500);
      reset(1);
    }
  });
  const y = 80;
  const removeC = (want: number) => (
    <>
      <ActionButton text={L('Odeber podporu C', 'Убрать опору C')} enabled={!removed.v && rigid.v === want && !alpha.isRunning} onClick={() => (removed.v = true)} />
      {removed.v && !settled.v && <SmallText text={L('Sleduj pravou část…', 'Следи за правой частью…')} />}
    </>
  );
  const tip1 = useTip9(
    L('Odebereme podporu C. Co udělá pravá část za kloubem?', 'Уберём опору C. Что сделает правая часть за шарниром?'),
    [L('zůstane vodorovně', 'останется горизонтально'), L('trochu se prohne', 'немного прогнётся'), L('otočí se v K a spadne', 'повернётся в K и упадёт')],
    guess.v,
    (i) => (guess.v = i),
  );
  const tip2 = useTip9(
    L('Kloub vyměníme za tuhý spoj. Co se stane bez podpory C teď?', 'Заменим шарнир жёстким соединением. Что будет без опоры C теперь?'),
    [L('spadne stejně', 'упадёт так же'), L('jen se trochu prohne', 'лишь немного прогнётся'), L('nestane se vůbec nic', 'ничего не произойдёт')],
    guess2.v,
    (i) => (guess2.v = i),
  );
  return (
    <>
      <Stage
        h={240}
        description={t.pick(L('Nosník s vnitřním kloubem K', 'Балка с промежуточным шарниром K'))}
        draw={(b, c) => {
          const a = alpha.value;
          b.support('pin', gx(0), y, 'A');
          b.support('roller', gx(3), y, 'B');
          if (!removed.v) b.support('roller', gx(6), y, 'C');
          let fx: number;
          let fy: number;
          const deg = (th.v * 180) / Math.PI;
          if (rigid.v === 0) {
            part(b, gx(0) - 10, gx(4) - 6, y, gx(4), 0, c, a);
            // pravá část se otáčí kolem K
            part(b, gx(4) + 6, gx(6) + 10, y, gx(4), deg, c, a);
            hinge(b, gx(4), y, c, 'K', a);
            [fx, fy] = rot9(gx(5), y, gx(4), y, deg);
          } else {
            // pevný spoj: nosník jen pružně prohne převislý konec za B, žádný zlom v K
            const d = 14 * droop.v;
            droopBeam(b, gx(0) - 10, gx(6) + 10, y, gx(3), d, c, a);
            b.text(gx(5) - 12, y - 14, t.pick(L('tuhý spoj', 'жёсткое соединение')), c.inkSoft, 13, 'end', null, false, a);
            if (removed.v) b.text(gx(5), y + 46, t.pick(L('průhyb zvětšen', 'прогиб увеличен')), c.inkSoft, 11, 'middle', null, false, a);
            fx = gx(5);
            fy = y + d * ((gx(5) - gx(3)) / (gx(6) + 10 - gx(3))) ** 2;
          }
          // svislá síla F působí stále svisle; s natočením části se mění jen působiště
          b.force(fx, fy - 5 - 50, fx, fy - 5, c.load, 2.4, 11, false, a);
          b.text(fx + 12, fy - 44, null, c.load, 15, 'middle', 'F', false, a);
          if (rigid.v === 0 && removed.v && th.v > 0.3) b.text(gx(4) + 70, y + 120, t.pick(L('visí na kloubu', 'висит на шарнире')), c.inkSoft, 12, 'middle', null, false, settled.v ? a : 0);
        }}
      />
      <CardText
        text={L(
          '**Vnitřní kloub** K spojuje dvě části nosníku jako pant dveří: části se v něm mohou vůči sobě volně natočit.',
          '**Промежуточный шарнир** K (vnitřní kloub) соединяет две части балки, как дверная петля: части могут свободно поворачиваться в нём друг относительно друга.',
        )}
      />
      <Steps
        steps={[
          tip1,
          step(
            L('Odeber podporu C a počkej, až se pravá část uklidní', 'Убери опору C и подожди, пока правая часть успокоится'),
            tried.v.has(0),
            () => removeC(0),
            () => (
              <Note
                tone={guess.v === 2 ? 'ok' : 'info'}
                text={L(
                  (guess.v === 2 ? 'Tip sedí. ' : '') +
                    'Pravá část se v kloubu otáčí **čím dál rychleji** – moment síly F a tíhy k bodu K nemá nic, co by ho vyrovnalo. Uklidní se, až visí svisle: pak síly míří přímo do K a jejich rameno je 0. Kloub ji udrží (přenese **sílu**), ale otáčení nezabrání (**moment** nepřenese).',
                  (guess.v === 2 ? 'Прогноз верен. ' : '') +
                    'Правая часть поворачивается в шарнире **всё быстрее** — моменту силы F и веса относительно K нечем противостоять. Успокаивается, только когда висит вертикально: тогда силы направлены прямо в K и плечо у них 0. Шарнир её удерживает (передаёт **силу**), но повороту не мешает (**момент** не передаёт).',
                )}
              />
            ),
          ),
          tip2,
          step(
            L('Odeber podporu C u tuhého spoje a porovnej', 'Убери опору C при жёстком соединении и сравни'),
            tried.v.has(1),
            () => removeC(1),
            () => (
              <>
                <Note
                  tone={guess2.v === 1 ? 'ok' : 'info'}
                  text={L(
                    (guess2.v === 1 ? 'Tip sedí. ' : '') + 'Tuhý spoj přenese i moment – nosník drží, převislý konec se jen pružně prohne (na obrázku zvětšeně) a ustálí se.',
                    (guess2.v === 1 ? 'Прогноз верен. ' : '') + 'Жёсткое соединение передаёт и момент — балка держится, консоль лишь упруго прогибается (на рисунке увеличено) и успокаивается.',
                  )}
                />
                <SmallText
                  text={L(
                    'Kloub moment nepřenese. Proto u stojícího nosníku platí [[M_K]] = 0: součet momentů všech sil na **jedné** části nosníku k bodu K je nula.',
                    'Шарнир не передаёт момент. Поэтому у неподвижной балки [[M_K]] = 0: сумма моментов всех сил, действующих на **одну** часть балки, относительно точки K равна нулю.',
                  )}
                />
              </>
            ),
          ),
        ]}
        after={() => (
          <>
            <Chips options={[L('kloub', 'шарнир'), L('tuhý spoj', 'жёсткое соединение')]} selected={rigid.v} onPick={(i) => i !== rigid.v && reset(i)} />
            {!removed.v ? (
              <ActionButton text={L('Odeber podporu C', 'Убрать опору C')} enabled={!alpha.isRunning} onClick={() => (removed.v = true)} />
            ) : (
              <ActionButton text={L('Nový pokus (s podporou C)', 'Новая попытка (с опорой C)')} enabled={settled.v} onClick={() => reset()} />
            )}
          </>
        )}
      />
    </>
  );
}

/**
 * Karta 1: M_K = 0 jen z jedné části. Části jsou nakreslené odděleně se silami v kloubu (K_x, K_y stejně velké,
 * opačné). Žák vybere část, ze které vyjde rovnice s jednou neznámou, a spočítá R_C.
 */
function OnePart() {
  const t = useTr();
  const left = '[[M_K]] = −[[R_Ay]]·4 − [[R_B]]·1 + [[F_1]]·2,5 = 0';
  const right = '[[M_K]] = [[R_C]]·2 − 6·1 = 0';
  const q1 = useQuiz(
    L('Ze které části vyjde rovnice s jedinou neznámou?', 'Из какой части получится уравнение с одной неизвестной?'),
    [
      opt(
        L('z levé části', 'из левой части'),
        false,
        L(
          `Levá část: ${left}. Zůstaly by dvě neznámé – [[R_Ay]] i [[R_B]] mají k bodu K nenulové rameno.`,
          `Левая часть: ${left}. Остались бы две неизвестные — у [[R_Ay]] и [[R_B]] плечо относительно K не нулевое.`,
        ),
      ),
      opt(L('z pravé části', 'из правой части'), true),
    ],
    L('Na pravé části je jediná neznámá reakce [[R_C]] – sílu [[F_2]] = 6 kN známe.', 'На правой части одна неизвестная реакция [[R_C]] — силу [[F_2]] = 6 кН мы знаем.'),
  );
  const nC = useNum(
    L('R_C z pravé části', 'R_C из правой части'),
    [numSpec(L('[[R_C]]', '[[R_C]]'), 3, t.pick(L('kN', 'кН')))],
    L(
      '[[R_C]]·2 = 6·1 → [[R_C]] = 3 kN. Tahle rovnice je **navíc** ke třem rovnicím pro celý nosník – díky ní spočítáš všechny čtyři reakce.',
      '[[R_C]]·2 = 6·1 → [[R_C]] = 3 кН. Это уравнение — **дополнительное** к трём уравнениям для всей балки — благодаря нему находятся все четыре реакции.',
    ),
    [
      trapNear(L('[[F_2]] má rameno 1 m, [[R_C]] ale 2 m. Momenty se musí rovnat, ne síly.', 'У [[F_2]] плечо 1 м, а у [[R_C]] — 2 м. Равны должны быть моменты, а не силы.'), 6),
      trapNear(L('Ramena jsi prohodil: [[R_C]] je od K 2 m daleko, [[F_2]] jen 1 m.', 'Плечи перепутаны: [[R_C]] находится в 2 м от K, а [[F_2]] — в 1 м.'), 12),
    ],
  );
  const side = q1.solved ? 1 : q1.picked;
  // vlastní rozvržení: části od sebe, ať jsou vidět síly v kloubu
  const y = 100;
  const s = 44;
  const xa = 46;
  const gap = 56;
  const lx = (m: number) => xa + m * s; // levá část 0…4 m
  const rx = (m: number) => xa + m * s + gap; // pravá část 4…6 m
  return (
    <>
      <Stage
        h={240}
        description={t.pick(L('Části nosníku s kloubem odděleně, síly v kloubu a momenty k bodu K', 'Части балки с шарниром по отдельности, силы в шарнире и моменты относительно K'))}
        draw={(b, c) => {
          const la = side === 1 ? 0.22 : 1;
          const ra = side === 0 ? 0.22 : 1;
          const kl = lx(4);
          const kr = rx(4);
          // levá část
          part(b, lx(0) - 10, kl, y, 0, 0, c, la);
          hinge(b, kl, y, c, '', la);
          b.force(4, y, lx(0) - 12, y, c.react, 2.4, 11, false, la);
          b.text(18, y - 10, null, c.react, 13, 'middle', 'R_Ax', false, la);
          b.force(lx(0), y + 56, lx(0), y + 6, c.react, 2.4, 11, false, la);
          b.text(lx(0) + 10, y + 50, null, c.react, 13, 'start', 'R_Ay', false, la);
          b.force(lx(3), y + 56, lx(3), y + 6, c.react, 2.4, 11, false, la);
          b.text(lx(3) + 10, y + 50, null, c.react, 13, 'start', 'R_B', false, la);
          b.force(lx(1.5), y - 58, lx(1.5), y - 5, c.load, 2.4, 11, false, la);
          b.text(lx(1.5) + 8, y - 46, null, c.load, 13, 'start', 'F_1', false, la);
          // pravá část
          part(b, kr, rx(6) + 8, y, 0, 0, c, ra);
          hinge(b, kr, y, c, '', ra);
          b.force(rx(5), y - 58, rx(5), y - 5, c.load, 2.4, 11, false, ra);
          b.text(rx(5) + 8, y - 46, ' = 6 kN', c.load, 13, 'start', 'F_2', false, ra);
          b.force(rx(6), y + 56, rx(6), y + 6, c.react, 2.4, 11, false, ra);
          b.text(rx(6) - 8, y + 50, nC.solved ? ' = 3 kN' : null, c.react, 13, 'end', 'R_C', false, ra);
          // síly v kloubu: na každou část stejně velké a opačné
          const mid = (kl + kr) / 2;
          b.force(mid, y, kl + 8, y, c.derived, 2.0, 9, false, la);
          b.force(kl, y - 40, kl, y - 8, c.derived, 2.0, 9, false, la);
          b.text(kl - 6, y - 30, null, c.derived, 12, 'end', 'K_y', false, la);
          b.force(mid, y, kr - 8, y, c.derived, 2.0, 9, false, ra);
          b.force(kr, y + 40, kr, y + 8, c.derived, 2.0, 9, false, ra);
          b.text(kr + 6, y + 34, null, c.derived, 12, 'start', 'K_y', false, ra);
          b.text(mid, y + 18, null, c.derived, 12, 'middle', 'K_x');
          b.text(mid, y - 22, 'K', c.ink, 15, 'middle', null, true);
          // body K (momentový bod) na obou částech
          b.circle(kl, y, 11, withAlpha(c.accent, 0.12), c.accent, 1.3, la);
          b.circle(kr, y, 11, withAlpha(c.accent, 0.12), c.accent, 1.3, ra);
          // kóty v metrech
          const yd = y + 84;
          const lm = [0, 1.5, 3, 4];
          b.line(lx(0), yd, lx(4), yd, c.ink, 0.9, false, la);
          for (const mk of lm) b.line(lx(mk) - 4, yd + 4, lx(mk) + 4, yd - 4, c.ink, 1.2, false, la);
          for (let i = 0; i < lm.length - 1; i++) b.text((lx(lm[i]) + lx(lm[i + 1])) / 2, yd - 5, num(lm[i + 1] - lm[i]), c.ink, 12, 'middle', null, false, la);
          const rm = [4, 5, 6];
          b.line(rx(4), yd, rx(6), yd, c.ink, 0.9, false, ra);
          for (const mk of rm) b.line(rx(mk) - 4, yd + 4, rx(mk) + 4, yd - 4, c.ink, 1.2, false, ra);
          for (let i = 0; i < rm.length - 1; i++) b.text((rx(rm[i]) + rx(rm[i + 1])) / 2, yd - 5, num(rm[i + 1] - rm[i]), c.ink, 12, 'middle', null, false, ra);
          b.text(rx(6) + 6, yd + 4, 'm', c.inkSoft, 11, 'start');
        }}
      />
      <CardText
        text={L(
          'Rovnice kloubu [[M_K]] = 0: sečti momenty k bodu K, ale jen od sil na **jedné** části nosníku. Části jsou nakreslené odděleně; v kloubu na sebe působí silami [[K_x]], [[K_y]] (stejně velkými, opačnými).',
          'Уравнение шарнира [[M_K]] = 0: сложи моменты относительно K, но только от сил на **одной** части балки. Части нарисованы отдельно; в шарнире они действуют друг на друга силами [[K_x]], [[K_y]] (равными и противоположными).',
        )}
      />
      <Steps
        steps={[
          quizStep9(q1, t.pick, 2, q1.question, () => (
            <>
              <EqLine text={left} note={L('levá část: 2 neznámé', 'левая часть: 2 неизвестные')} dim />
              <EqLine text={right} note={L('pravá část: 1 neznámá', 'правая часть: 1 неизвестная')} />
              <SmallText
                text={L(
                  'Momenty ↺ bereme kladně. Síly v kloubu [[K_x]], [[K_y]] **působí**, jen k bodu K mají rameno 0 – proto v rovnici nejsou. Stejně tak [[R_Ax]] v ose nosníku.',
                  'Моменты ↺ считаем положительными. Силы в шарнире [[K_x]], [[K_y]] **действуют**, просто плечо у них относительно K равно 0 — поэтому в уравнении их нет. Так же и [[R_Ax]] на оси балки.',
                )}
              />
            </>
          )),
          numStep9(nC, L('Pravá část: [[R_C]] · 2 − 6 · 1 = 0. Kolik je [[R_C]]?', 'Правая часть: [[R_C]] · 2 − 6 · 1 = 0. Чему равна [[R_C]]?')),
        ]}
      />
    </>
  );
}

/** Karta 2: n neznámých vs r rovnic – žák spočítá neznámé ze značek podpor, pak uvidí uvolnění a posoudí, jestli nosník drží. */
function CountUnknowns() {
  const t = useTr();
  const kit = useKit();
  const released = kit.mut<Set<number>>(new Set());
  const demoDone = kit.mut(false);
  const qN = useQuiz(
    L('Kolik neznámých reakcí n dají podpory A, B a C dohromady?', 'Сколько неизвестных реакций n дают опоры A, B и C вместе?'),
    [
      opt(L('n = 3', 'n = 3'), false, L('Pevná kloubová podpora A má dvě reakce, ne jednu.', 'У шарнирно-неподвижной опоры A две реакции, а не одна.')),
      opt(L('n = 4', 'n = 4'), true),
      opt(L('n = 5', 'n = 5'), false, L('Posuvné podpory B a C mají každá jen jednu reakci.', 'У подвижных опор B и C по одной реакции.')),
    ],
    L('A: 2 ([[R_Ax]], [[R_Ay]]), B: 1, C: 1 → n = 4 = r.', 'A: 2 ([[R_Ax]], [[R_Ay]]), B: 1, C: 1 → n = 4 = r.'),
  );
  const qS = useQuiz(
    L('Počet sedí. Může se přesto některá část nosníku pohnout?', 'Число сходится. Может ли всё же какая-то часть балки сдвинуться?'),
    [
      opt(L('ne – každému pohybu něco brání', 'нет — каждому движению что-то мешает'), true),
      opt(
        L('ano – celý nosník ujede vodorovně', 'да — вся балка уедет по горизонтали'),
        false,
        L('Pevná kloubová podpora A má i vodorovnou reakci [[R_Ax]] – posunu brání.', 'У шарнирно-неподвижной опоры A есть и горизонтальная реакция [[R_Ax]] — она мешает сдвигу.'),
      ),
      opt(
        L('ano – pravá část se otočí kolem K', 'да — правая часть повернётся вокруг K'),
        false,
        L('Pravou část drží podpora C – bez ní by se otočila (vzpomeň si na první kartu).', 'Правую часть держит опора C — без неё она бы повернулась (вспомни первую карточку).'),
      ),
    ],
    L(
      'Ano, drží: A brání posunu vodorovně i svisle, B brání otočení levé části kolem A a C otočení pravé části kolem K. Neznámých je stejně jako rovnic **a** podpory nedovolí žádný pohyb – nosník je **staticky určitý**.',
      'Да, держится: A не даёт сдвига ни по горизонтали, ни по вертикали, B мешает повороту левой части вокруг A, а C — повороту правой части вокруг K. Неизвестных столько же, сколько уравнений, **и** опоры не допускают никакого движения — балка **статически определима**.',
    ),
  );
  // po správném počtu: podpory se postupně promění v šipky
  useLaunched([qN.solved], async (job) => {
    if (!qN.solved) return;
    for (let i = 0; i <= 2; i++) {
      await job.delay(800);
      released.v = new Set([...released.v, i]);
    }
    await job.delay(900);
    demoDone.v = true;
  });
  const y = 105;
  return (
    <>
      <Stage
        h={210}
        description={t.pick(L('Nosník s kloubem: počítání neznámých reakcí', 'Балка с шарниром: подсчёт неизвестных реакций'))}
        draw={(b, c) => {
          const r = released.v;
          b.support('pin', gx(0), y, 'A', r.has(0));
          b.support('roller', gx(3), y, 'B', r.has(1));
          b.support('roller', gx(6), y, 'C', r.has(2));
          part(b, gx(0) - 10, gx(4) - 6, y, 0, 0, c);
          part(b, gx(4) + 6, gx(6) + 10, y, 0, 0, c);
          hinge(b, gx(4), y, c);
          if (r.has(0)) {
            b.force(gx(0) - 46, y, gx(0) - 12, y, c.react);
            b.text(gx(0) - 30, y - 10, null, c.react, 14, 'middle', 'R_Ax');
            b.force(gx(0), y + 60, gx(0), y + 6, c.react);
            b.text(gx(0) + 12, y + 52, null, c.react, 14, 'start', 'R_Ay');
          }
          if (r.has(1)) {
            b.force(gx(3), y + 60, gx(3), y + 6, c.react);
            b.text(gx(3) + 12, y + 52, null, c.react, 14, 'start', 'R_B');
          }
          if (r.has(2)) {
            b.force(gx(6), y + 60, gx(6), y + 6, c.react);
            b.text(gx(6) - 8, y + 52, null, c.react, 14, 'end', 'R_C');
          }
          if (r.size === 3) b.text(200, y + 92, 'n = 4 = r', c.accent, 15, 'middle', null, true);
        }}
      />
      <CardText text={L('Rovnic máme r = 4: tři pro celý nosník a jednu za kloub. Počet neznámých reakcí označíme n.', 'Уравнений у нас r = 4: три для всей балки и одно за шарнир. Число неизвестных реакций обозначим n.')} />
      <Steps
        steps={[
          quizStep9(qN, t.pick, 3),
          step(L('Sleduj: uvolníme A, B a C – každá modrá šipka je jedna neznámá', 'Смотри: освобождаем A, B и C — каждая синяя стрелка — одна неизвестная'), demoDone.v, () => (
            <SmallText text={L('Sleduj obrázek…', 'Смотри на рисунок…')} />
          )),
          quizStep9(qS, t.pick, 1, qS.question, () => (
            <SmallText
              text={L(
                'Kdyby C byla pevná kloubová podpora (2 reakce), bylo by n = 5 > r = 4. Nosník by byl staticky neurčitý – rovnice rovnováhy by nestačily.',
                'Если бы C была шарнирно-неподвижной опорой (2 реакции), было бы n = 5 > r = 4. Балка стала бы статически неопределимой — уравнений равновесия не хватило бы.',
              )}
            />
          )),
        ]}
      />
    </>
  );
}

/**
 * Karta 3: kolik je nezávislých rovnic. Levá + pravá část = celý nosník, takže všechny tři momentové rovnice
 * nezávislé nejsou – stačí kterékoli dvě.
 */
function HowMany() {
  const t = useTr();
  const kit = useKit();
  const pick = kit.mut<Set<number>>(new Set());
  const checked = kit.mut<Set<number> | null>(null);
  const ok = kit.mut(false);
  const qSide = useQuiz(
    L('Ze které části vezmeš [[M_K]] = 0, aby v ní byla jen [[R_B]]?', 'Из какой части возьмёшь [[M_K]] = 0, чтобы в нём была только [[R_B]]?'),
    [opt(L('z levé', 'из левой'), false, L('Na levé části je vetknutí A se třemi neznámými.', 'На левой части — заделка A с тремя неизвестными.')), opt(L('z pravé', 'из правой'), true)],
    L(
      'Na pravé části je jen [[R_B]] (síly v kloubu mají k K rameno 0). Obvykle se bere [[M_K]] = 0 z té části, kde zůstane nejméně neznámých.',
      'На правой части только [[R_B]] (у сил в шарнире плечо относительно K равно 0). Обычно [[M_K]] = 0 берут для той части, где остаётся меньше всего неизвестных.',
    ),
  );
  const opts = [L('Σ[[F_x]] = 0', 'Σ[[F_x]] = 0'), L('Σ[[F_y]] = 0', 'Σ[[F_y]] = 0'), L('Σ[[M_A]] = 0', 'Σ[[M_A]] = 0'), L('[[M_K]] = 0 levá', '[[M_K]] = 0 левая'), L('[[M_K]] = 0 pravá', '[[M_K]] = 0 правая')];
  const ch = checked.v;
  let wrong: L | null = null;
  if (ch && !ok.v) {
    const moments = [...ch].filter((x) => x >= 2).length;
    wrong =
      moments === 3
        ? L(
            'Levá + pravá část = celý nosník. Momenty obou částí k K dohromady dají momentovou rovnici celku – jedna ze tří momentových rovnic je jen součtem ostatních. Nezávislé jsou jen **dvě**.',
            'Левая + правая часть = вся балка. Моменты обеих частей относительно K вместе дают уравнение моментов всей балки — одно из трёх лишь сумма остальных. Независимых только **два**.',
          )
        : !ch.has(0) || !ch.has(1)
          ? L(
              'Bez silových rovnic celku Σ[[F_x]] a Σ[[F_y]] nespočítáš vodorovnou ani svislou reakci vetknutí – momentové rovnice je nenahradí.',
              'Без уравнений сил для всей балки Σ[[F_x]] и Σ[[F_y]] не найти горизонтальную и вертикальную реакции заделки — уравнения моментов их не заменят.',
            )
          : L(
              'Neznámé jsou 4, rovnic potřebuješ taky 4. Přidej momentovou rovnici – k bodu A nebo z kloubu.',
              'Неизвестных 4, значит, и уравнений нужно 4. Добавь уравнение моментов — относительно A или от шарнира.',
            );
  }
  return (
    <>
      <Stage
        h={170}
        description={t.pick(L('Nosník s jedním vnitřním kloubem', 'Балка с одним промежуточным шарниром'))}
        draw={(b, c) => {
          const y = 80;
          b.support('fixed', gx(0), y, 'A', false, true);
          b.support('roller', gx(6), y, 'B');
          part(b, gx(0), gx(3) - 6, y, 0, 0, c);
          part(b, gx(3) + 6, gx(6) + 10, y, 0, 0, c);
          hinge(b, gx(3), y, c);
          b.force(gx(1.5), y - 54, gx(1.5), y - 5, c.load);
          b.force(gx(4.5), y - 54, gx(4.5), y - 5, c.load);
          b.text(gx(6) - 10, y + 62, t.pick(L('n = 3 + 1 = 4 neznámé', 'n = 3 + 1 = 4 неизвестных')), c.react, 13, 'end');
        }}
      />
      <CardText
        text={L(
          'Vetknutí A má 3 neznámé reakce, posuvná podpora B jednu – celkem **4**. K dispozici jsou rovnice celého nosníku (Σ[[F_x]], Σ[[F_y]], Σ[[M_A]]) a rovnice kloubu [[M_K]] = 0 pro levou i pravou část.',
          'У заделки A 3 неизвестные реакции, у подвижной опоры B одна — всего **4**. В распоряжении уравнения всей балки (Σ[[F_x]], Σ[[F_y]], Σ[[M_A]]) и уравнение шарнира [[M_K]] = 0 для левой и для правой части.',
        )}
      />
      <Steps
        steps={[
          step(
            L('Vyber rovnice, ze kterých spočítáš všechny 4 neznámé', 'Выбери уравнения, из которых найдёшь все 4 неизвестные'),
            ok.v,
            () => (
              <>
                <Chips
                  options={opts}
                  selectedSet={pick.v}
                  onPick={(i) => {
                    if (ok.v) return;
                    const n = new Set(pick.v);
                    if (n.has(i)) n.delete(i);
                    else n.add(i);
                    pick.v = n;
                    checked.v = null;
                  }}
                />
                <ActionButton
                  text={L(`Zkontrolovat (vybráno ${pick.v.size})`, `Проверить (выбрано ${pick.v.size})`)}
                  enabled={pick.v.size > 0}
                  onClick={() => {
                    checked.v = new Set(pick.v);
                    const moments = [...pick.v].filter((x) => x >= 2).length;
                    ok.v = pick.v.size === 4 && pick.v.has(0) && pick.v.has(1) && moments === 2;
                  }}
                />
                {wrong && <Note tone="nudge" text={wrong} />}
              </>
            ),
            () => (
              <Note
                tone="ok"
                text={L(
                  'r = 4 nezávislé rovnice na 4 neznámé: obě silové rovnice celku a **dvě** momentové. Třetí momentová by byla jen součtem ostatních (levá + pravá část = celek).',
                  'r = 4 независимых уравнения на 4 неизвестные: оба уравнения сил для всей балки и **два** уравнения моментов. Третье уравнение моментов было бы лишь суммой остальных (левая + правая часть = вся балка).',
                )}
              />
            ),
          ),
          quizStep9(qSide, t.pick, 2),
        ]}
      />
    </>
  );
}

/**
 * Karta 4: n = r nestačí. Nosník na pevné + posuvné podpoře a nosník na třech posuvných podporách mají oba
 * 3 neznámé. Vodorovný náraz: horní drží (R_Ax), dolní se rozjede a bez tření ujede.
 */
function EnoughButHolds() {
  const t = useTr();
  const kit = useKit();
  const guess = kit.mut<number | null>(null);
  const pushed = kit.mut(false);
  const running = kit.mut(false);
  const x = kit.mut(0); // posun dolního nosníku (jednotky obrázku)
  const push = kit.mut(false); // právě působí síla H
  const alpha = kit.anim(1);
  const qWhy = useQuiz(
    L('Co dolnímu nosníku chybí?', 'Чего не хватает нижней балке?'),
    [
      opt(L('vodorovná reakce – žádná podpora nebrání posunu', 'горизонтальная реакция — ни одна опора не мешает сдвигу'), true),
      opt(
        L('další svislá podpora', 'ещё одна вертикальная опора'),
        false,
        L('Čtvrtá posuvná podpora by přidala zase jen svislou reakci. Vodorovně by nosník ujel stejně.', 'Четвёртая подвижная опора добавила бы опять только вертикальную реакцию. По горизонтали балка уехала бы так же.'),
      ),
      opt(
        L('nic – jen je moc lehký', 'ничего — она просто слишком лёгкая'),
        false,
        L('Těžší nosník by se rozjížděl pomaleji, ale rozjel by se taky – žádná síla mu nebrání.', 'Более тяжёлая балка разгонялась бы медленнее, но всё равно поехала бы — ей ничто не мешает.'),
      ),
    ],
    L(
      'n = r je **nutná** podmínka, ne postačující. Podpory musí navíc zabránit každému pohybu: posunu vodorovně, svisle i otočení. Tři posuvné podpory mají tři neznámé, a přesto je nosník pohyblivý – takové uložení je **výjimkové**.',
      'n = r — условие **необходимое**, но не достаточное. Опоры должны ещё не допускать никакого движения: ни горизонтального, ни вертикального сдвига, ни поворота. У трёх подвижных опор три неизвестные, и всё же балка подвижна — такое закрепление **геометрически изменяемо**.',
    ),
  );
  const run = () => {
    if (running.v) return;
    running.v = true;
    kit.scope.launch(async (job) => {
      x.v = 0;
      await alpha.snapTo(1);
      let time = 0;
      let last = 0;
      const tp = 0.6; // náraz trvá 0,6 s
      const a = 120; // jednotek/s² během nárazu
      push.v = true;
      while (x.v < 90) {
        const now = await job.frame();
        const dt = last === 0 ? 0 : Math.min((now - last) / 1000, 0.05);
        last = now;
        time += dt;
        x.v = time <= tp ? 0.5 * a * time * time : 0.5 * a * tp * tp + a * tp * (time - tp);
        push.v = time <= tp;
      }
      push.v = false;
      // dolní nosník ujel – zmizí a objeví se nový na místě
      await job.run(alpha.animateTo(0, tween(350)));
      pushed.v = true;
      x.v = 0;
      await alpha.animateTo(1, tween(300));
      running.v = false;
    });
  };
  const tip = useTip9(
    L('Tipni si: který nosník sílu H udrží?', 'Как думаешь: какая балка удержит силу H?'),
    [L('oba – oba mají n = r', 'обе — у обеих n = r'), L('jen horní', 'только верхняя'), L('jen dolní', 'только нижняя'), L('žádný', 'ни одна')],
    guess.v,
    (i) => (guess.v = i),
  );
  const y1 = 70;
  const y2 = 170;
  return (
    <>
      <Stage
        h={235}
        description={t.pick(L('Dva nosníky se třemi neznámými reakcemi a vodorovná síla', 'Две балки с тремя неизвестными реакциями и горизонтальная сила'))}
        draw={(b, c) => {
          // horní: pevná + posuvná
          b.support('pin', 70, y1, 'A');
          b.support('roller', 330, y1, 'B');
          b.beam(60, 340, y1);
          b.text(200, y1 - 22, t.pick(L('n = 3 (A: 2, B: 1)', 'n = 3 (A: 2, B: 1)')), c.inkSoft, 12);
          if (push.v) {
            b.force(340, y1, 384, y1, c.load, 2.6);
            b.text(384, y1 - 10, null, c.load, 14, 'end', 'H');
            // pevná podpora sílu zachytí
            b.force(64, y1, 22, y1, c.react, 2.4);
            b.text(22, y1 - 10, null, c.react, 13, 'start', 'R_Ax');
          }
          // dolní: tři posuvné podpory
          [70, 200, 330].forEach((sx, i) => b.support('roller', sx, y2, `${i + 1}`));
          const a = alpha.value;
          b.beam(60 + x.v, 340 + x.v, y2, a);
          b.text(200, y2 - 22, 'n = 3 (1 + 1 + 1)', c.inkSoft, 12, 'middle', null, false, a);
          if (push.v) {
            b.force(340 + x.v, y2, 384 + x.v, y2, c.load, 2.6, 11, false, a);
            b.text(Math.min(384 + x.v, 396), y2 - 10, null, c.load, 14, 'end', 'H', false, a);
          }
          if (x.v > 2) b.text(200, y2 + 52, t.pick(L('posuvné podpory vodorovně nedrží → zrychluje', 'подвижные опоры не держат по горизонтали → разгоняется')), c.derived, 12);
        }}
      />
      <CardText
        text={L(
          'Oba nosníky mají **3** neznámé reakce – stejně jako jsou 3 rovnice rovnováhy: n = r. Do obou krátce strčíme vodorovnou silou H.',
          'У обеих балок **3** неизвестные реакции — столько же, сколько уравнений равновесия: n = r. Обе коротко толкнём горизонтальной силой H.',
        )}
      />
      <Steps
        steps={[
          tip,
          step(
            L('Strč do obou nosníků vodorovnou silou H', 'Толкни обе балки горизонтальной силой H'),
            pushed.v,
            () => <ActionButton text={L('Strč silou H', 'Толкнуть силой H')} enabled={!running.v} onClick={run} />,
            () => (
              <Note
                tone={guess.v === 1 ? 'ok' : 'info'}
                text={L(
                  (guess.v === 1 ? 'Tip sedí. ' : '') +
                    'Horní nosník drží – pevná podpora A má vodorovnou reakci. Dolní se rozjel: posuvné podpory umějí tlačit jen svisle, takže vodorovné síle nic nebrání, a bez tření jede dál.',
                  (guess.v === 1 ? 'Прогноз верен. ' : '') +
                    'Верхняя балка держится — у неподвижной опоры A есть горизонтальная реакция. Нижняя разогналась: подвижные опоры давят только вертикально, горизонтальной силе ничто не мешает, и без трения балка едет дальше.',
                )}
              />
            ),
          ),
          quizStep9(qWhy, t.pick),
        ]}
      />
    </>
  );
}

export const cards = [HingeTest, OnePart, CountUnknowns, HowMany, EnoughButHolds];
