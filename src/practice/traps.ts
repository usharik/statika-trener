/* Katalog typických chyb (shodné s aplikací – practice/Traps.kt). */
import { L } from '../i18n';
import type { Learn, Trap } from './exercise';

export class TrapText {
  code: string;
  msg: L;
  learn: Learn | null;
  constructor(code: string, msg: L, learn: Learn | null) {
    this.code = code;
    this.msg = msg;
    this.learn = learn;
  }
  /** chyba poznaná podle hodnot, které dává (null = pole nehraje roli) */
  at(...wrong: (number | null)[]): Trap {
    return { code: this.code, msg: this.msg, learn: this.learn, wrong };
  }
  /** chyba se složitější podmínkou; [example] = hodnoty, které ji vyvolají */
  where(example: (number | null)[], pred: (v: (number | null)[]) => boolean): Trap {
    return { code: this.code, msg: this.msg, learn: this.learn, wrong: example, pred };
  }
  /** stejná chyba s jiným textem */
  with(msg: L): TrapText {
    return new TrapText(this.code, msg, this.learn);
  }
}

const T = (code: string, cs: string, ru: string, learn: Learn | null) => new TrapText(code, L(cs, ru), learn);

export const Traps = {
  SWAP: T('SWAP', 'Vypadá to na prohozený sin a cos. Která strana leží **u** úhlu?', 'Похоже, sin и cos перепутаны. Какая сторона лежит **у** угла?', [102, 1]),
  RAD: T('RAD', 'Tohle vyjde, když je kalkulačka v režimu RAD. Přepni na DEG a ověř, že sin 30 = 0,5.', 'Так получается, когда калькулятор в режиме RAD. Переключи на DEG и проверь, что sin 30 = 0,5.', [103, 1]),
  GRAD: T('GRAD', 'Kalkulačka je nejspíš v režimu GRAD (na displeji G). Přepni na DEG.', 'Калькулятор, скорее всего, в режиме GRAD (на дисплее G). Переключи на DEG.', [103, 1]),
  FULL: T('FULL', 'Složka nemůže být stejně velká jako celá síla – je to jen její část. Rozlož sílu pomocí cos a sin.', 'Составляющая не может равняться всей силе — это только её часть. Разложи силу через cos и sin.', [102, 2]),
  DIV: T('DIV', 'Odvěsna nemůže být delší než přepona. Přeponu funkcí násob, neděl.', 'Катет не может быть длиннее гипотенузы. Гипотенузу на функцию умножай, а не дели.', [102, 4]),
  MUL: T('MUL', 'Přepona je nejdelší strana – musí vyjít víc než odvěsna. Dělit, nebo násobit?', 'Гипотенуза — самая длинная сторона, она должна получиться больше катета. Делить или умножать?', [102, 4]),
  INV: T('INV', 'Sinus ani kosinus nejsou nikdy větší než 1 – přepona patří ve zlomku dolů.', 'Ни синус, ни косинус не бывают больше 1 — гипотенуза в дроби стоит снизу.', [102, 2]),
  SUM: T('SUM', 'Sčítají se druhé mocniny, ne strany.', 'Складываются квадраты, а не стороны.', [105, 0]),
  NOSQRT: T('NOSQRT', 'To je teprve druhá mocnina výsledku. Ještě odmocni.', 'Это пока квадрат результата. Осталось извлечь корень.', [105, 0]),
  COMPL: T('COMPL', 'To je druhý ostrý úhel (u druhé odvěsny). Zkontroluj, co je ve zlomku nahoře.', 'Это второй острый угол (у другого катета). Проверь, что в дроби сверху.', [105, 3]),
  FWD: T('FWD', 'To není úhel – tohle vyjde po sin/cos/tan místo sin⁻¹/cos⁻¹/tan⁻¹ (SHIFT, 2nd nebo INV).', 'Это не угол — так выходит, если нажать sin/cos/tan вместо sin⁻¹/cos⁻¹/tan⁻¹ (SHIFT, 2nd или INV).', [106, 1]),
  RATIO: T('RATIO', 'To je teprve podíl stran. Úhel z něj dostaneš obrácenou funkcí.', 'Это пока отношение сторон. Угол из него получают обратной функцией.', [106, 0]),
  WRONGINV: T(
    'WRONGINV',
    'Tohle vyjde s jinou funkcí, než jaká patří k těmto dvěma stranám. Které strany znáš – odvěsny, nebo i přeponu?',
    'Так выходит с другой функцией, не той, что соответствует этим двум сторонам. Какие стороны известны — катеты или ещё и гипотенуза?',
    [106, 1],
  ),
  PCT_DEG: T('PCT_DEG', 'Procenta nejsou stupně. Sklon v % = tg α · 100.', 'Проценты — не градусы. Уклон в % = tg α · 100.', [106, 4]),
  PCT_90: T('PCT_90', '100 % není 90°, ale 45°. Procenta se nepřepočítávají přímou úměrou – použij tangens.', '100 % — это не 90°, а 45°. Проценты не переводятся пропорцией — нужен тангенс.', [106, 4]),
  PCT_100: T('PCT_100', 'Nejdřív převeď procenta na číslo: vyděl stem.', 'Сначала переведи проценты в число: раздели на сто.', [106, 4]),
  SIGN_X: T('SIGN_X', 'Velikost [[F_x]] sedí. Kam míří šipka vodorovně – doprava (+), nebo doleva (−)?', 'Величина [[F_x]] верна. Куда стрелка направлена по горизонтали — вправо (+) или влево (−)?', [107, 2]),
  SIGN_Y: T('SIGN_Y', 'Velikost [[F_y]] sedí. Míří šipka nahoru (+), nebo dolů (−)?', 'Величина [[F_y]] верна. Стрелка направлена вверх (+) или вниз (−)?', [107, 2]),
  VERT: T('VERT', 'Úhel je tady měřený od **svislice**. Která složka leží u úhlu teď?', 'Здесь угол отсчитан от **вертикали**. Какая составляющая теперь лежит у угла?', [108, 0]),
  NEGANG: T(
    'NEGANG',
    'Do arctg dej velikosti bez znamének – hledáme ostrý (kladný) úhel. Směr síly je vidět ze znamének složek.',
    'В arctg подставляй величины без знаков — ищем острый (положительный) угол. Направление силы видно по знакам составляющих.',
    [108, 1],
  ),
  TGRATIO: T('TGRATIO', 'Tady se dělí druhou odvěsnou – to je tangens. Pro složku je potřeba přepona: √(m² + n²).', 'Здесь делят на другой катет — это тангенс. Для составляющей нужна гипотенуза: √(m² + n²).', [106, 3]),
  ARM_FULL: T('ARM_FULL', 'F · r platí jen pro sílu kolmou k páce. Rameno je [[r_⊥]] = r · sin α.', 'F · r верно только для силы, перпендикулярной рычагу. Плечо — [[r_⊥]] = r · sin α.', [108, 3]),
  ARM_COS: T('ARM_COS', 'Rameno leží **naproti** úhlu α (mezi silou a pákou) – proto sinus.', 'Плечо лежит **напротив** угла α (между силой и рычагом) — поэтому синус.', [108, 3]),
  WHOLE: T('WHOLE', 'Do součtu patří jen složka šikmé síly, ne celá síla.', 'В сумму входит только составляющая наклонной силы, а не вся сила.', [108, 2]),
};
