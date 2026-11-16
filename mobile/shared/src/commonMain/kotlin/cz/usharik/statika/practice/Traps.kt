package cz.usharik.statika.practice

import cz.usharik.statika.learn.L

/** Text typické chyby z katalogu: [code], zpráva a karta výkladu, která ji vysvětluje. */
class TrapText(val code: String, val msg: L, val learn: Pair<Int, Int>?) {
    /** chyba poznaná podle hodnot, které dává (null = pole nehraje roli) */
    fun at(vararg wrong: Double?): Trap = Trap(code, msg, learn, wrong.toList())

    /** chyba se složitější podmínkou; [example] = hodnoty, které ji spolehlivě vyvolají (pro testy) */
    fun where(example: List<Double?>, pred: (List<Double?>) -> Boolean): Trap = Trap(code, msg, learn, example, pred)

    /** stejná chyba s jiným (konkrétnějším) textem – kód a odkaz zůstanou */
    fun with(msg: L): TrapText = TrapText(code, msg, learn)
}

/**
 * Katalog typických chyb (návrh kurzu, oddíl 2.3). Každý typ úlohy si z něj bere, co se k němu hodí, a dá k tomu
 * hodnoty, které ta chyba vyrobí (`SWAP.at(a, b)`). Odkazy vedou na karty kurzu trigonometrie (lekce 101–108).
 */
object Traps {
    val SWAP = TrapText("SWAP", L("Vypadá to na prohozený sin a cos. Která strana leží **u** úhlu?", "Похоже, sin и cos перепутаны. Какая сторона лежит **у** угла?"), 102 to 1)
    val RAD = TrapText("RAD", L("Tohle vyjde, když je kalkulačka v režimu RAD. Přepni na DEG a ověř, že sin 30 = 0,5.", "Так получается, когда калькулятор в режиме RAD. Переключи на DEG и проверь, что sin 30 = 0,5."), 103 to 1)
    val GRAD = TrapText("GRAD", L("Kalkulačka je nejspíš v režimu GRAD (na displeji G). Přepni na DEG.", "Калькулятор, скорее всего, в режиме GRAD (на дисплее G). Переключи на DEG."), 103 to 1)
    val FULL = TrapText("FULL", L("Složka nemůže být stejně velká jako celá síla – je to jen její část. Rozlož sílu pomocí cos a sin.", "Составляющая не может равняться всей силе — это только её часть. Разложи силу через cos и sin."), 102 to 2)
    val DIV = TrapText("DIV", L("Odvěsna nemůže být delší než přepona. Přeponu funkcí násob, neděl.", "Катет не может быть длиннее гипотенузы. Гипотенузу на функцию умножай, а не дели."), 102 to 4)
    val MUL = TrapText("MUL", L("Přepona je nejdelší strana – musí vyjít víc než odvěsna. Dělit, nebo násobit?", "Гипотенуза — самая длинная сторона, она должна получиться больше катета. Делить или умножать?"), 102 to 4)
    val INV = TrapText("INV", L("Sinus ani kosinus nejsou nikdy větší než 1 – přepona patří ve zlomku dolů.", "Ни синус, ни косинус не бывают больше 1 — гипотенуза в дроби стоит снизу."), 102 to 2)
    val SUM = TrapText("SUM", L("Sčítají se druhé mocniny, ne strany.", "Складываются квадраты, а не стороны."), 105 to 0)
    val NOSQRT = TrapText("NOSQRT", L("To je teprve druhá mocnina výsledku. Ještě odmocni.", "Это пока квадрат результата. Осталось извлечь корень."), 105 to 0)
    val COMPL = TrapText("COMPL", L("To je druhý ostrý úhel (u druhé odvěsny). Zkontroluj, co je ve zlomku nahoře.", "Это второй острый угол (у другого катета). Проверь, что в дроби сверху."), 105 to 3)
    val FWD = TrapText("FWD", L("To není úhel – tohle vyjde po sin/cos/tan místo sin⁻¹/cos⁻¹/tan⁻¹ (SHIFT, 2nd nebo INV).", "Это не угол — так выходит, если нажать sin/cos/tan вместо sin⁻¹/cos⁻¹/tan⁻¹ (SHIFT, 2nd или INV)."), 106 to 1)
    val RATIO = TrapText("RATIO", L("To je teprve podíl stran. Úhel z něj dostaneš obrácenou funkcí.", "Это пока отношение сторон. Угол из него получают обратной функцией."), 106 to 0)
    val WRONGINV = TrapText("WRONGINV", L("Tohle vyjde s jinou funkcí, než jaká patří k těmto dvěma stranám. Které strany znáš – odvěsny, nebo i přeponu?", "Так выходит с другой функцией, не той, что соответствует этим двум сторонам. Какие стороны известны — катеты или ещё и гипотенуза?"), 106 to 1)
    val PCT_DEG = TrapText("PCT_DEG", L("Procenta nejsou stupně. Sklon v % = tg α · 100.", "Проценты — не градусы. Уклон в % = tg α · 100."), 106 to 4)
    val PCT_90 = TrapText("PCT_90", L("100 % není 90°, ale 45°. Procenta se nepřepočítávají přímou úměrou – použij tangens.", "100 % — это не 90°, а 45°. Проценты не переводятся пропорцией — нужен тангенс."), 106 to 4)
    val PCT_100 = TrapText("PCT_100", L("Nejdřív převeď procenta na číslo: vyděl stem.", "Сначала переведи проценты в число: раздели на сто."), 106 to 4)
    val SIGN_X = TrapText("SIGN_X", L("Velikost [[F_x]] sedí. Kam míří šipka vodorovně – doprava (+), nebo doleva (−)?", "Величина [[F_x]] верна. Куда стрелка направлена по горизонтали — вправо (+) или влево (−)?"), 107 to 2)
    val SIGN_Y = TrapText("SIGN_Y", L("Velikost [[F_y]] sedí. Míří šipka nahoru (+), nebo dolů (−)?", "Величина [[F_y]] верна. Стрелка направлена вверх (+) или вниз (−)?"), 107 to 2)
    val VERT = TrapText("VERT", L("Úhel je tady měřený od **svislice**. Která složka leží u úhlu teď?", "Здесь угол отсчитан от **вертикали**. Какая составляющая теперь лежит у угла?"), 108 to 0)
    val NEGANG = TrapText("NEGANG", L("Do arctg dej velikosti bez znamének – hledáme ostrý (kladný) úhel. Směr síly je vidět ze znamének složek.", "В arctg подставляй величины без знаков — ищем острый (положительный) угол. Направление силы видно по знакам составляющих."), 108 to 1)
    val TGRATIO = TrapText("TGRATIO", L("Tady se dělí druhou odvěsnou – to je tangens. Pro složku je potřeba přepona: √(m² + n²).", "Здесь делят на другой катет — это тангенс. Для составляющей нужна гипотенуза: √(m² + n²)."), 106 to 3)
    val ARM_FULL = TrapText("ARM_FULL", L("F · r platí jen pro sílu kolmou k páce. Rameno je [[r_⊥]] = r · sin α.", "F · r верно только для силы, перпендикулярной рычагу. Плечо — [[r_⊥]] = r · sin α."), 108 to 3)
    val ARM_COS = TrapText("ARM_COS", L("Rameno leží **naproti** úhlu α (mezi silou a pákou) – proto sinus.", "Плечо лежит **напротив** угла α (между силой и рычагом) — поэтому синус."), 108 to 3)
    val WHOLE = TrapText("WHOLE", L("Do součtu patří jen složka šikmé síly, ne celá síla.", "В сумму входит только составляющая наклонной силы, а не вся сила."), 108 to 2)

    val all: List<TrapText> = listOf(SWAP, RAD, GRAD, FULL, DIV, MUL, INV, SUM, NOSQRT, COMPL, FWD, RATIO, WRONGINV, PCT_DEG, PCT_90, PCT_100, SIGN_X, SIGN_Y, VERT, NEGANG, TGRATIO, ARM_FULL, ARM_COS, WHOLE)
}
