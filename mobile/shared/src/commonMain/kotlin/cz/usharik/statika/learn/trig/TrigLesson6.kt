package cz.usharik.statika.learn.trig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cz.usharik.statika.learn.ActionButton
import cz.usharik.statika.learn.Card
import cz.usharik.statika.learn.CardScope
import cz.usharik.statika.learn.CardText
import cz.usharik.statika.learn.Chips
import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Lesson
import cz.usharik.statika.learn.Note
import cz.usharik.statika.learn.NumSpec
import cz.usharik.statika.learn.NumTrap
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.learn.Pt
import cz.usharik.statika.learn.SmallText
import cz.usharik.statika.learn.Stage
import cz.usharik.statika.learn.Step
import cz.usharik.statika.learn.Steps
import cz.usharik.statika.learn.TriGeom
import cz.usharik.statika.learn.ValueSlider
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.circle
import cz.usharik.statika.learn.dimLine
import cz.usharik.statika.learn.force
import cz.usharik.statika.learn.grid
import cz.usharik.statika.learn.handle
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.numStep
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.poly
import cz.usharik.statika.learn.quizStep
import cz.usharik.statika.learn.rightAngleMark
import cz.usharik.statika.learn.rightTriangle
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.learn.triGeom
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Kurz trigonometrie – lekce 6 (id 106): od stran zpátky k úhlu – arcsin, arccos, arctg, sklon poměrem a v procentech.
 */

/** Tip jako krok: volby a „Ověřit“ v doku. Tip se jen zaznamená – vyhodnotí ho až další krok. */
private fun predictStep(label: L, options: List<L>, guess: Int?, onGuess: (Int) -> Unit): Step =
    Step(label, guess != null, result = { if (guess != null) SmallText(L("Tvůj tip: ${options[guess].cs}", "Твой прогноз: ${options[guess].ru}")) }) {
        var sel by remember { mutableStateOf<Int?>(null) }
        Chips(options, sel) { sel = it }
        ActionButton(L("Ověřit", "Проверить"), enabled = sel != null) { sel?.let(onGuess) }
    }

/** Karta 0: znáš strany, hledáš úhel – tg α = výška / základna, α = arctg. */
@Composable
private fun CardScope.BackToAngle() {
    val t = tr()
    var guess by remember { mutableStateOf<Int?>(null) }
    var phase by remember { mutableIntStateOf(0) } // ukázka: 1 poměr, 2 arctg, 3 úhel
    var demoDone by remember { mutableStateOf(false) }
    var h by remember { mutableIntStateOf(2) }
    var at45 by remember { mutableStateOf(false) }
    var tried by remember { mutableStateOf<Int?>(null) } // výška při posledním chybném pokusu
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(guess != null) {
        if (guess == null) return@LaunchedEffect
        for (p in 1..3) {
            delay(900)
            phase = p
        }
        delay(600)
        demoDone = true
    }
    val tips = listOf(L("méně než 45°", "меньше 45°"), L("45°", "45°"), L("víc než 45°", "больше 45°"))
    val s1 = predictStep(L("Odhadni úhel α při výšce 3 a základně 4", "Предскажи угол α при высоте 3 и основании 4"), tips, guess) { guess = it }
    val s2 = Step(
        L("Sleduj, jak se z poměru 3/4 stane úhel", "Проследи, как отношение 3/4 превращается в угол"), demoDone,
        result = {
            Note(if (guess == 0) Tone.OK else Tone.INFO, L(
                (if (guess == 0) "Tip sedí. " else "Výška 3 je menší než základna 4, proto je úhel menší než 45°. ") +
                    "α = arctg 0,75 ≈ 36,9°. Na kalkulačce (režim DEG) je arctg pod tlačítkem **tan⁻¹** (SHIFT nebo 2nd + tan).",
                (if (guess == 0) "Прогноз верен. " else "Высота 3 меньше основания 4, поэтому угол меньше 45°. ") +
                    "α = arctg 0,75 ≈ 36,9°. На калькуляторе (режим DEG) arctg — это кнопка **tan⁻¹** (SHIFT или 2nd + tan).",
            ))
        },
    ) { SmallText(L("Sleduj obrázek: poměr → úhel…", "Смотри на рисунок: отношение → угол…")) }
    val s3 = Step(
        L("Najdi výšku, při které je úhel 45° (základna 4)", "Найди высоту, при которой угол 45° (основание 4)"), at45,
        result = { Note(Tone.OK, L("Výška = základna, poměr je 1 a arctg 1 = 45°.", "Высота = основание, отношение равно 1, а arctg 1 = 45°.")) },
    ) {
        ValueSlider(t.pick(L("Výška = $h", "Высота = $h")), h.toFloat(), 0f..6f, steps = 5) {
            h = it.roundToInt()
            tried = null
        }
        ActionButton(L("Zkontrolovat", "Проверить")) { if (h == 4) at45 = true else tried = h }
        tried?.let { v ->
            val a = Trig.atanD(v / 4.0)
            Note(Tone.NUDGE, L(
                "Při výšce $v je α = arctg ($v/4) ≈ ${num(a, 1)}°. Při jakém poměru stran je úhel přesně 45°?",
                "При высоте $v угол α = arctg ($v/4) ≈ ${num(a, 1)}°. При каком отношении сторон угол ровно 45°?",
            ))
        }
    }
    val steps = listOf(s1, s2, s3)
    val cur = steps.indexOfFirst { !it.done }
    val free = cur == 2 || cur < 0
    val hh = if (free) h else 3
    val step = 30.0
    val ox = 80.0
    val oy = 210.0
    val base = 4
    val alpha = Trig.atanD(hh.toDouble() / base)
    val tipX = ox + base * step
    val tipY = oy - hh * step
    Stage(
        h = 230.0,
        description = t.pick(L("Trojúhelník na mřížce s nastavitelnou výškou", "Треугольник на сетке с изменяемой высотой")),
        onDrag = { p, start ->
            if (start) dragging = free && !at45 && (p.dist(tipX, tipY) < 45 || abs(p.x - tipX) < 30)
            if (dragging) {
                h = ((oy - p.y) / step).roundToInt().coerceIn(0, 6)
                tried = null
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        grid(ox, oy - 6 * step, step, base + 1, 6, c)
        // úhel se ukáže až po ukázce / po správné odpovědi
        val showAngle = (cur == 1 && phase >= 3) || cur < 0
        if (hh > 0) {
            rightTriangle(TriGeom(Pt(ox, oy), Pt(tipX, tipY), Pt(tipX, oy)), c, mapOf(TriSide.ADJ to "$base", TriSide.OPP to "$hh"), angle = null)
            angleArc(ox, oy, 34.0, 0.0, -alpha, if (showAngle) "${num(alpha, 1)}°" else "?", c)
        } else {
            line(ox, oy, tipX, oy, c.derived, 2.4)
        }
        if (free && !at45) handle(tipX, tipY, c, dragging)
        val lines = when {
            cur == 0 -> listOf("tg α = 3 / 4", "α = ?")
            cur == 1 -> listOfNotNull(
                "tg α = 3 / 4" + if (phase >= 1) " = 0,75" else "",
                if (phase >= 2) "α = arctg 0,75" else null,
                if (phase >= 3) "α ≈ 36,9°" else null,
                if (phase >= 2) t.pick(L("kalkulačka: tan⁻¹ 0,75", "калькулятор: tan⁻¹ 0,75")) else null,
            )
            cur == 2 -> listOf("tg α = $hh / $base", "α = 45° ?")
            else -> listOf("tg α = 4 / 4 = 1", "α = arctg 1 = 45°")
        }
        lines.forEachIndexed { i, s ->
            text(392.0, 40.0 + 24 * i, s, if (s.startsWith("α ≈") || s.startsWith("α = arctg 1")) c.load else if (s.contains("tan⁻¹")) c.inkSoft else c.ink, if (s.contains("tan⁻¹")) 13.0 else 14.0, Anchor.END, bold = s.startsWith("α"))
        }
    }
    CardText(L(
        "Známe-li obě odvěsny, známe tg α. Funkce **arctg** z tohoto poměru určí úhel: poměr dovnitř, úhel ven.",
        "Если известны оба катета, известен и tg α. Функция **arctg** находит по этому отношению угол: отношение на входе, угол на выходе.",
    ))
    Steps(*steps.toTypedArray())
}

/** Karta 1: kterou obrácenou funkci – podle dvou známých stran. Každé kolo zvýrazní jinou dvojici. */
@Composable
private fun CardScope.WhichInverse() {
    val t = tr()
    val fns = { right: Int, why: List<L?> ->
        listOf("arcsin", "arccos", "arctg").mapIndexed { i, f -> Opt(L(f, f), correct = i == right, why = why[i]) }
    }
    val s1 = quizStep(
        question = L("Znáš protilehlou odvěsnu a přeponu. Která funkce dá úhel α?", "Известны противолежащий катет и гипотенуза. Какая функция даёт угол α?"),
        label = L("Vyber funkci pro protilehlou odvěsnu a přeponu", "Выбери функцию для противолежащего катета и гипотенузы"),
        options = fns(0, listOf(
            null,
            L("Kosinus potřebuje přilehlou odvěsnu – tu neznáš.", "Косинусу нужен прилежащий катет — он неизвестен."),
            L("Tangens dělí odvěsnu odvěsnou – jedna ze známých stran je ale přepona.", "Тангенс делит катет на катет, а одна из известных сторон — гипотенуза."),
        )),
        explain = L("protilehlá / přepona = sin α, proto **α = arcsin (protilehlá / přepona)**. Na kalkulačce sin⁻¹.", "противолежащий / гипотенуза = sin α, поэтому **α = arcsin (противолежащий / гипотенуза)**. На калькуляторе sin⁻¹."),
        columns = 3,
    )
    val s2 = quizStep(
        question = L("Znáš přilehlou odvěsnu a přeponu. Která funkce dá úhel α?", "Известны прилежащий катет и гипотенуза. Какая функция даёт угол α?"),
        label = L("Vyber funkci pro přilehlou odvěsnu a přeponu", "Выбери функцию для прилежащего катета и гипотенузы"),
        options = fns(1, listOf(
            L("Sinus potřebuje odvěsnu **naproti** úhlu. Známá odvěsna se úhlu dotýká.", "Синусу нужен катет **напротив** угла. А известный катет прилежит к углу."),
            null,
            L("Tangens dělí odvěsnu odvěsnou – jedna ze známých stran je ale přepona.", "Тангенс делит катет на катет, а одна из известных сторон — гипотенуза."),
        )),
        explain = L("přilehlá / přepona = cos α, proto **α = arccos (přilehlá / přepona)**. Na kalkulačce cos⁻¹.", "прилежащий / гипотенуза = cos α, поэтому **α = arccos (прилежащий / гипотенуза)**. На калькуляторе cos⁻¹."),
        columns = 3,
    )
    val s3 = quizStep(
        question = L("Znáš obě odvěsny. Která funkce dá úhel α?", "Известны оба катета. Какая функция даёт угол α?"),
        label = L("Vyber funkci pro obě odvěsny", "Выбери функцию для двух катетов"),
        options = fns(2, listOf(
            L("Sinus potřebuje přeponu – tu neznáš.", "Синусу нужна гипотенуза — она неизвестна."),
            L("Kosinus potřebuje přeponu – tu neznáš.", "Косинусу нужна гипотенуза — она неизвестна."),
            null,
        )),
        explain = L(
            "protilehlá / přilehlá = tg α, proto **α = arctg (protilehlá / přilehlá)**. Na kalkulačce tan⁻¹. V režimu DEG vyjde úhel ve stupních.",
            "противолежащий / прилежащий = tg α, поэтому **α = arctg (противолежащий / прилежащий)**. На калькуляторе tan⁻¹. В режиме DEG результат получается в градусах.",
        ),
        columns = 3,
    )
    val steps = listOf(s1, s2, s3)
    val cur = steps.indexOfFirst { !it.done }
    val known = when (cur) {
        0 -> setOf(TriSide.OPP, TriSide.HYP)
        1 -> setOf(TriSide.ADJ, TriSide.HYP)
        2 -> setOf(TriSide.ADJ, TriSide.OPP)
        else -> emptySet()
    }
    val g = triGeom(4.0, 3.0, fit = listOf(70.0, 20.0, 210.0, 150.0))
    fun mid(s: TriSide): Pt {
        val (p, q) = g.side(s)
        return Pt((p.x + q.x) / 2, (p.y + q.y) / 2)
    }
    val names = mapOf(
        TriSide.HYP to L("přepona", "гипотенуза"),
        TriSide.ADJ to L("přilehlá", "прилежащий"),
        TriSide.OPP to L("protilehlá", "противолежащий"),
    )
    Stage(h = 230.0, description = t.pick(L("Trojúhelník se zvýrazněnými známými stranami", "Треугольник с выделенными известными сторонами"))) { c ->
        rightTriangle(g, c, angle = "α")
        for (s in known) {
            val (p, q) = g.side(s)
            line(p.x, p.y, q.x, q.y, c.highlight, 5.0)
        }
        fun col(s: TriSide) = if (s in known) c.highlight else if (s == TriSide.HYP) c.load else c.derived
        val hm = mid(TriSide.HYP)
        text(hm.x - 14, hm.y - 12, t.pick(names.getValue(TriSide.HYP)), col(TriSide.HYP), 14.0, Anchor.END, bold = TriSide.HYP in known)
        val om = mid(TriSide.OPP)
        text(om.x + 12, om.y + 5, t.pick(names.getValue(TriSide.OPP)), col(TriSide.OPP), 14.0, Anchor.START, bold = TriSide.OPP in known)
        val am = mid(TriSide.ADJ)
        text(am.x, am.y + 22, t.pick(names.getValue(TriSide.ADJ)), col(TriSide.ADJ), 14.0, bold = TriSide.ADJ in known)
        text(200.0, 218.0, if (cur >= 0) t.pick(L("známe zvýrazněné strany, hledáme α", "известны выделенные стороны, ищем α")) else "sin⁻¹ · cos⁻¹ · tan⁻¹", c.inkSoft, 13.0)
    }
    CardText(L(
        "Inverzní funkci volíme podle dvou známých stran. **sin⁻¹** na kalkulačce znamená arcsin, ne 1/sin.",
        "Обратную функцию выбирают по двум известным сторонам. **sin⁻¹** на калькуляторе означает arcsin, а не 1/sin.",
    ))
    Steps(*steps.toTypedArray())
}

/** Karta 2: přilehlá a přepona → arccos, pak výpočet. */
@Composable
private fun CardScope.CheckArccos() {
    val t = tr()
    val s1 = quizStep(
        question = L("Znáš přilehlou odvěsnu 4 m a přeponu 5 m. Jak spočítáš úhel α?", "Известны прилежащий катет 4 м и гипотенуза 5 м. Как найти угол α?"),
        label = L("Vyber výpočet úhlu α", "Выбери способ вычисления угла α"),
        options = listOf(
            Opt(L("α = arccos (4/5)", "α = arccos (4/5)"), correct = true),
            Opt(L("α = arcsin (4/5)", "α = arcsin (4/5)"), why = L("Sinus potřebuje odvěsnu **naproti** úhlu. Strana 4 m se ale úhlu α dotýká – jak se jí říká?", "Синусу нужен катет **напротив** угла. А сторона 4 м касается угла α — как она называется?")),
            Opt(L("α = arctg (4/5)", "α = arctg (4/5)"), why = L("Tangens dělí odvěsnu odvěsnou. Tady je jedna ze známých stran přepona.", "Тангенс делит катет на катет. А здесь одна из известных сторон — гипотенуза.")),
            Opt(L("α = arccos (5/4)", "α = arccos (5/4)"), why = L("5/4 je víc než 1 – takový kosinus neexistuje, odvěsna je vždy kratší než přepona. Co patří do zlomku nahoru?", "5/4 больше 1 — такого косинуса не бывает, катет всегда короче гипотенузы. Что должно быть в числителе?")),
            Opt(L("α = cos (4/5)", "α = cos (4/5)"), why = L("cos udělá z úhlu poměr. My máme poměr a chceme úhel – která funkce jde opačným směrem?", "cos превращает угол в отношение. У нас есть отношение, а нужен угол — какая функция идёт в обратную сторону?")),
        ),
        explain = L("Přilehlá a přepona → kosinus: cos α = 4/5 = 0,8, tedy α = arccos 0,8.", "Прилежащий и гипотенуза → косинус: cos α = 4/5 = 0,8, значит α = arccos 0,8."),
        columns = 2,
    )
    val s2 = numStep(
        question = L("Vypočítej α = arccos 0,8 na jedno desetinné místo (kalkulačka v režimu DEG).", "Вычисли α = arccos 0,8 с точностью до десятых (калькулятор в режиме DEG)."),
        label = L("Vypočítej α na jedno desetinné místo", "Вычисли α с точностью до десятых"),
        fields = listOf(NumSpec(L("α", "α"), Trig.acosD(0.8), "°", tol = Tol.ANGLE)),
        explain = L(
            "cos α = 4/5 = 0,8, tedy α = arccos 0,8 ≈ 36,9°. Hodnota 53,1° patří k druhému ostrému úhlu.",
            "cos α = 4/5 = 0,8, значит α = arccos 0,8 ≈ 36,9°. Значение 53,1° относится ко второму острому углу.",
        ),
        traps = listOf(
            NumTrap.near(L("To je arcsin 0,8 – úhel naproti straně 4 m. Použij cos⁻¹.", "Это arcsin 0,8 — угол напротив стороны 4 м. Используй cos⁻¹."), Trig.asinD(0.8)),
            NumTrap.near(L("To je arctg 0,8. Strana 5 m je přepona – použij cos⁻¹.", "Это arctg 0,8. Сторона 5 м — гипотенуза, используй cos⁻¹."), Trig.atanD(0.8)),
            NumTrap.near(L("Kalkulačka je v režimu RAD – vyšly radiány. Přepni na DEG.", "Калькулятор в режиме RAD — получились радианы. Переключи на DEG."), kotlin.math.acos(0.8)),
            NumTrap.near(L("0,8 je teprve cos α. Úhel dostaneš přes cos⁻¹.", "0,8 — это пока cos α. Угол получишь через cos⁻¹."), 0.8),
        ),
        nudge = L("Na kalkulačce v režimu DEG: SHIFT (2nd) + cos, pak 0,8.", "На калькуляторе в режиме DEG: SHIFT (2nd) + cos, затем 0,8."),
    )
    val solved = s2.done
    Stage(h = 200.0, description = t.pick(L("Trojúhelník: přilehlá 4 m, přepona 5 m, úhel α neznámý", "Треугольник: прилежащий катет 4 м, гипотенуза 5 м, угол α неизвестен"))) { c ->
        val g = triGeom(4.0, 3.0, fit = listOf(80.0, 25.0, 230.0, 150.0))
        rightTriangle(g, c, mapOf(TriSide.ADJ to t.pick(L("4 m", "4 м")), TriSide.HYP to t.pick(L("5 m", "5 м"))), null, if (solved) "α ≈ 36,9°" else "α = ?")
    }
    CardText(L(
        "Lano dlouhé 5 m překonává vodorovnou vzdálenost 4 m. Hledáme jeho úhel s vodorovnou.",
        "Трос длиной 5 м перекрывает горизонтальное расстояние 4 м. Ищем его угол с горизонталью.",
    ))
    Steps(s1, s2)
}

/** Karta 3: sklon zadaný poměrem výška : vodorovná délka = tg α. */
@Composable
private fun CardScope.SlopeRatio() {
    val t = tr()
    val m = t.pick(L("m", "м"))
    val kn = t.pick(L("kN", "кН"))
    val s1 = quizStep(
        question = L("Který z těchto sklonů je nejstrmější?", "Какой из этих уклонов самый крутой?"),
        label = L("Vyber nejstrmější z těchto sklonů", "Выбери самый крутой из этих уклонов"),
        options = listOf(
            Opt(L("1 : 2", "1 : 2"), why = L("1 : 2 znamená 1 díl nahoru na 2 díly vodorovně – tg α = 0,5. Porovnej poměry jako čísla.", "1 : 2 — 1 часть вверх на 2 части по горизонтали, tg α = 0,5. Сравни отношения как числа.")),
            Opt(L("3 : 4", "3 : 4"), why = L("3 : 4 je tg α = 0,75. Je to nejvíc? Spočítej i ostatní poměry.", "3 : 4 — это tg α = 0,75. Это самое большое? Посчитай и другие отношения.")),
            Opt(L("1 : 1", "1 : 1"), correct = true),
        ),
        explain = L(
            "Větší poměr = strmější: 1 : 1 → tg α = 1 (45°), 3 : 4 → 0,75 (36,9°), 1 : 2 → 0,5 (26,6°).",
            "Больше отношение — круче: 1 : 1 → tg α = 1 (45°), 3 : 4 → 0,75 (36,9°), 1 : 2 → 0,5 (26,6°).",
        ),
        columns = 3,
    )
    val s2 = numStep(
        question = L("Svah má sklon 1 : 2 a vodorovnou délku 6 m. Jak je vysoký?", "Откос имеет уклон 1 : 2 и горизонтальную длину 6 м. Какова его высота?"),
        label = L("Vypočítej výšku při sklonu 1 : 2 a vodorovné délce 6 m", "Найди высоту при уклоне 1 : 2 и горизонтальной длине 6 м"),
        fields = listOf(NumSpec(L("h", "h"), 3.0, m)),
        explain = L("Na každé 2 m vodorovně 1 m nahoru: h = 6 · 1/2 = 3 m.", "На каждые 2 м по горизонтали 1 м вверх: h = 6 · 1/2 = 3 м."),
        traps = listOf(NumTrap.near(L("Poměr je výška : vodorovná délka. Výška je tedy menší než 6 m.", "Отношение — это высота : горизонтальная длина. Значит, высота меньше 6 м."), 12.0)),
        nudge = L("Na každé 2 m vodorovně 1 m nahoru. Kolikrát se 2 m vejdou do 6 m?", "На каждые 2 м по горизонтали 1 м вверх. Сколько раз 2 м укладываются в 6 м?"),
    )
    val s3 = numStep(
        question = L("Lano táhne silou T = 10 kN ve směru se sklonem 3 : 4. Jak velká je svislá složka |[[T_y]]|?", "Трос тянет с силой T = 10 кН в направлении с уклоном 3 : 4. Каков модуль вертикальной составляющей |[[T_y]]|?"),
        label = L("Urči svislou složku tahu 10 kN ve směru 3 : 4", "Найди вертикальную составляющую тяги 10 кН при 3 : 4"),
        fields = listOf(NumSpec(L("|[[T_y]]|", "|[[T_y]]|"), 6.0, kn)),
        explain = L(
            "Směrový trojúhelník 3–4–5 má přeponu 5, takže |[[T_y]]| = T · 3/5 = 6 kN (a |[[T_x]]| = T · 4/5 = 8 kN). Úhel počítat nemusíš.",
            "У треугольника направлений 3–4–5 гипотенуза 5, поэтому |[[T_y]]| = T · 3/5 = 6 кН (а |[[T_x]]| = T · 4/5 = 8 кН). Угол вычислять не нужно.",
        ),
        traps = listOf(
            NumTrap.near(L("To je vodorovná složka – ta leží u čtyřky. Svislá patří k trojce.", "Это горизонтальная составляющая — она относится к четвёрке. Вертикальная — к тройке."), 8.0),
            NumTrap.near(L("3/4 je tg α, poměr dvou odvěsen. Složka = síla · (odvěsna / přepona) – jak dlouhá je přepona?", "3/4 — это tg α, отношение катетов. Составляющая = сила · (катет / гипотенуза) — какова гипотенуза?"), 7.5),
        ),
        nudge = L("Přepona směrového trojúhelníku 3-4-5 je 5. Svislá složka = T · 3/5.", "Гипотенуза треугольника направлений 3-4-5 равна 5. Вертикальная составляющая = T · 3/5."),
    )
    val steps = listOf(s1, s2, s3)
    val cur = steps.indexOfFirst { !it.done }
    Stage(h = 220.0, description = t.pick(L("Sklon zadaný poměrem", "Уклон, заданный отношением"))) { c ->
        when (cur) {
            0 -> {
                // co znamená 3 : 4 – stejné dílky vodorovně i svisle
                val step = 36.0
                val ox = 60.0
                val oy = 190.0
                grid(ox, oy - 4 * step, step, 4, 4, c)
                rightTriangle(TriGeom(Pt(ox, oy), Pt(ox + 4 * step, oy - 3 * step), Pt(ox + 4 * step, oy)), c, mapOf(TriSide.ADJ to t.pick(L("4 díly", "4 части")), TriSide.OPP to t.pick(L("3 díly", "3 части"))), null, null, hyp = c.ink)
                text(392.0, 40.0, t.pick(L("sklon 3 : 4", "уклон 3 : 4")), c.ink, 15.0, Anchor.END, bold = true)
                text(392.0, 64.0, t.pick(L("výška : vodorovně", "высота : горизонталь")), c.inkSoft, 13.0, Anchor.END)
                text(392.0, 88.0, "tg α = 3/4", c.ink, 14.0, Anchor.END)
            }
            1 -> {
                val u = 40.0
                val ox = 60.0
                val oy = 170.0
                line(20.0, oy, 380.0, oy, c.ink, 1.2)
                poly(listOf(ox to oy, ox + 6 * u to oy, ox + 6 * u to oy - 3 * u), c.subtle, null, alpha = 0.7f)
                line(ox, oy, ox + 6 * u, oy - 3 * u, c.ink, 3.0)
                line(ox + 6 * u, oy, ox + 6 * u, oy - 3 * u, c.derived, 2.0)
                rightAngleMark(Pt(ox + 6 * u, oy), Pt(ox, oy), Pt(ox + 6 * u, oy - 3 * u), c)
                dimLine(ox, oy + 18, ox + 6 * u, oy + 18, t.pick(L("6 m vodorovně", "6 м по горизонтали")), c, color = c.derived)
                text(ox + 6 * u + 10, oy - 1.5 * u + 5, "h = ?", c.derived, 14.0, Anchor.START, bold = true)
                // trojúhelníček 1 : 2 na svahu
                val px = ox + 2 * u
                val py = oy - u
                line(px, py, px + 2 * u * 0.6, py, c.inkSoft, 1.0)
                line(px + 2 * u * 0.6, py, px + 2 * u * 0.6, py - u * 0.6, c.inkSoft, 1.0)
                text(px + u * 0.6, py + 15, "2", c.inkSoft, 12.0)
                text(px + 2 * u * 0.6 + 6, py - u * 0.3 + 4, "1", c.inkSoft, 12.0, Anchor.START)
            }
            else -> {
                // lano z bodu O šikmo vzhůru doprava ve směru 3 : 4, tah T = 10 kN
                val ox = 70.0
                val oy = 195.0
                line(ox, oy, ox + 240.0, oy - 180.0, c.ink, 2.0)
                circle(ox, oy, 6.0, c.ink)
                text(ox - 8, oy + 5, "O", c.ink, 14.0, Anchor.END, bold = true)
                // směrový trojúhelníček 4 – 3 – 5
                val px = 190.0
                val py = oy - 120 * 0.75
                val qx = px + 80
                val ry = py - 60
                line(px, py, qx, py, c.inkSoft, 1.0)
                line(qx, py, qx, ry, c.inkSoft, 1.0)
                text((px + qx) / 2, py + 15, "4", c.inkSoft, 13.0)
                text(qx + 6, (py + ry) / 2 + 4, "3", c.inkSoft, 13.0, Anchor.START)
                text((px + qx) / 2 - 8, (py + ry) / 2 - 6, "5", c.inkSoft, 13.0, Anchor.END)
                // síla 10 kN = 100 px po laně
                val tx = ox + 80.0
                val ty = oy - 60.0
                force(ox, oy, tx, ty, c.load, 3.0)
                text(tx - 10, ty - 8, " = 10 $kn", c.load, 14.0, Anchor.END, sym = "T")
                if (cur < 0) {
                    line(tx, ty, tx, oy, c.derived, 1.0, dash = true)
                    force(ox, oy, ox + 80.0, oy, c.derived, 2.2)
                    force(ox + 80.0, oy, tx, ty, c.derived, 2.2)
                    text(ox + 40, oy + 18, " = 8 $kn", c.derived, 13.0, sym = "T_x")
                    text(tx + 8, (oy + ty) / 2 + 5, " = 6 $kn", c.derived, 13.0, Anchor.START, sym = "T_y", bold = true)
                } else {
                    text(392.0, 200.0, " = ?", c.derived, 15.0, Anchor.END, sym = "T_y", bold = true)
                }
            }
        }
    }
    CardText(L(
        "Poměr 3 : 4 znamená 3 díly výšky na 4 díly vodorovně – je to přímo **tg α**. Na výkresu si vždy ověř, co je v poměru první.",
        "Отношение 3 : 4 означает 3 части высоты на 4 части по горизонтали — это прямо **tg α**. На чертеже всегда проверяй, что стоит в отношении первым.",
    ))
    Steps(*steps.toTypedArray())
}

/** Silnice se sklonem [p] % na vodorovné délce [runLabel]; obě osy ve stejném měřítku (strmý svah se zmenší). */
private fun PrimBuilder.road(p: Double, runLabel: String, riseLabel: String, angleLabel: String?, c: StatikaColors) {
    val tg = p / 100
    val ox = 70.0
    val oy = 185.0
    val run = minOf(170.0, 160.0 / tg.coerceAtLeast(1e-9))
    val rise = run * tg
    line(20.0, oy, 380.0, oy, c.ink, 1.2)
    poly(listOf(ox to oy, ox + run to oy, ox + run to oy - rise), c.subtle, null, alpha = 0.7f)
    line(ox, oy, ox + run, oy - rise, c.ink, 6.0)
    line(ox, oy, ox + run, oy - rise, c.beam, 3.5)
    line(ox + run, oy, ox + run, oy - rise, c.derived, 1.4, dash = true)
    if (rise > 12) rightAngleMark(Pt(ox + run, oy), Pt(ox, oy), Pt(ox + run, oy - rise), c)
    dimLine(ox, oy + 14, ox + run, oy + 14, runLabel, c, color = c.derived)
    text(ox + run + 10, oy - rise / 2 + 5, riseLabel, c.derived, 14.0, Anchor.START, bold = true)
    if (p > 0 && angleLabel != null) {
        angleArc(ox, oy, 36.0, 0.0, -Trig.atanD(tg), null, c)
        text(ox - 8, oy - 6, angleLabel, c.inkSoft, 13.0, Anchor.END)
    }
}

/** Karta 4: sklon v procentech = 100 · h / d – a proč to nejsou stupně. */
@Composable
private fun CardScope.SlopePercent() {
    val t = tr()
    val m = t.pick(L("m", "м"))
    var guess by remember { mutableStateOf<Int?>(null) }
    var demoP by remember { mutableIntStateOf(10) }
    var demoDone by remember { mutableStateOf(false) }
    var p by remember { mutableIntStateOf(100) }
    val s1 = numStep(
        question = L("Silnice má sklon 10 %. O kolik metrů vystoupá na 100 m vodorovně?", "Уклон дороги 10 %. На сколько метров она поднимается на 100 м по горизонтали?"),
        label = L("Urči převýšení při sklonu 10 % na 100 m vodorovně", "Найди подъём при уклоне 10 % на 100 м по горизонтали"),
        fields = listOf(NumSpec(L("h", "h"), 10.0, m)),
        explain = L("10 % = 10 m převýšení na 100 m **vodorovné** délky: h = 0,10 · 100 = 10 m.", "10 % = 10 м подъёма на 100 м **горизонтальной** длины: h = 0,10 · 100 = 10 м."),
        traps = listOf(NumTrap.near(L("Sklon v % je převýšení na 100 m vodorovně. Kolik je 10 % ze 100 m?", "Уклон в % — подъём на 100 м по горизонтали. Сколько будет 10 % от 100 м?"), 1000.0)),
        nudge = L("10 % = 10 m na každých 100 m vodorovně.", "10 % = 10 м на каждые 100 м по горизонтали."),
    )
    val tips = listOf(L("45°", "45°"), L("60°", "60°"), L("90°", "90°"), L("100°", "100°"))
    val s2 = predictStep(L("Odhadni úhel svahu se sklonem 100 %", "Предскажи угол при уклоне 100 %"), tips, guess) { guess = it }
    LaunchedEffect(guess != null) {
        if (guess == null) return@LaunchedEffect
        delay(500)
        for (v in 15..100 step 5) {
            demoP = v
            delay(70)
        }
        delay(1200)
        demoDone = true
    }
    val s3 = Step(
        L("Sleduj, jak vypadá sklon 100 %", "Посмотри, как выглядит уклон 100 %"), demoDone,
        result = {
            Note(if (guess == 0) Tone.OK else Tone.INFO, L(
                (if (guess == 0) "Tip sedí. " else "Procenta nejsou stupně. ") + "100 % znamená h = d, tg α = 1, tedy α = 45°. Strmější svah má víc než 100 %; svislá stěna (d = 0) žádný konečný sklon v % nemá.",
                (if (guess == 0) "Прогноз верен. " else "Проценты — не градусы. ") + "100 % означает h = d, tg α = 1, то есть α = 45°. У более крутого склона больше 100 %; у вертикальной стены (d = 0) конечного уклона в % нет.",
            ))
        },
    ) { SmallText(L("Sklon roste od 10 % ke 100 %…", "Уклон растёт от 10 % до 100 %…")) }
    val s4 = numStep(
        question = L("Rampa stoupá o 2 m na 10 m vodorovně. Jaký má sklon v procentech?", "Пандус поднимается на 2 м на 10 м по горизонтали. Каков его уклон в процентах?"),
        label = L("Urči sklon v % pro h = 2 m a d = 10 m", "Найди уклон в % при h = 2 м и d = 10 м"),
        fields = listOf(NumSpec(L("sklon", "уклон"), 20.0, "%", tol = Tol.PERCENT)),
        explain = L("100 · h / d = 100 · 2 / 10 = 20 %. (Úhel by byl arctg 0,2 ≈ 11,3° – jiné číslo.)", "100 · h / d = 100 · 2 / 10 = 20 %. (Угол был бы arctg 0,2 ≈ 11,3° — другое число.)"),
        traps = listOf(
            NumTrap.near(L("0,2 je poměr h / d. V procentech ho ještě vynásob stem.", "0,2 — это отношение h / d. Для процентов умножь его на сто."), 0.2),
            NumTrap.near(L("To je úhel ve stupních. Sklon v % je 100 · h / d.", "Это угол в градусах. Уклон в % — это 100 · h / d."), Trig.atanD(0.2)),
            NumTrap.near(L("Do zlomku patří převýšení nahoru: h / d.", "В числителе подъём: h / d."), 500.0),
        ),
        nudge = L("Sklon v % = 100 · h / d.", "Уклон в % = 100 · h / d."),
    )
    val steps = listOf(s1, s2, s3, s4)
    val cur = steps.indexOfFirst { !it.done }
    Stage(h = 230.0, description = t.pick(L("Silnice se sklonem v procentech", "Дорога с уклоном в процентах"))) { c ->
        when (cur) {
            0 -> {
                road(10.0, t.pick(L("100 m vodorovně", "100 м по горизонтали")), "h = ?", null, c)
                text(392.0, 36.0, "10 %", c.ink, 18.0, Anchor.END, bold = true)
            }
            1 -> {
                road(100.0, "100 $m", "100 $m", "α = ?", c)
                text(392.0, 36.0, "100 %", c.ink, 18.0, Anchor.END, bold = true)
            }
            2 -> {
                val a = Trig.atanD(demoP / 100.0)
                road(demoP.toDouble(), "100 $m", "$demoP $m", if (demoP == 100) "α = 45°" else "α ≈ ${num(a, 1)}°", c)
                text(392.0, 36.0, "$demoP %", c.ink, 18.0, Anchor.END, bold = true)
            }
            3 -> {
                road(20.0, "10 $m", "2 $m", null, c)
                text(392.0, 36.0, "? %", c.ink, 18.0, Anchor.END, bold = true)
            }
            else -> {
                val a = Trig.atanD(p / 100.0)
                road(p.toDouble(), "100 $m", "$p $m", "α ≈ ${num(a, 1)}°", c)
                text(392.0, 36.0, "$p %", c.ink, 18.0, Anchor.END, bold = true)
            }
        }
    }
    CardText(L(
        "Sklon v procentech je **100 · h / d**, kde d je vodorovná délka. Procenta a stupně nejsou stejná veličina.",
        "Уклон в процентах равен **100 · h / d**, где d — горизонтальная длина. Проценты и градусы — разные величины.",
    ))
    Steps(*steps.toTypedArray(), after = {
        ValueSlider(t.pick(L("Sklon = $p %", "Уклон = $p %")), p.toFloat(), 0f..300f, steps = 59) { p = (it / 5).roundToInt() * 5 }
    })
}

/** Karta 5: úloha – úhel rampy ze stoupání a vodorovné délky. */
@Composable
private fun CardScope.RampAngle() {
    val t = tr()
    val s1 = quizStep(
        question = L("Rampa vystoupá 1,2 m na vodorovné délce 10 m. Čemu se rovná tg α?", "Пандус поднимается на 1,2 м на горизонтальной длине 10 м. Чему равен tg α?"),
        label = L("Vyber poměr pro tg α", "Выбери отношение для tg α"),
        options = listOf(
            Opt(L("1,2 / 10", "1,2 / 10"), correct = true),
            Opt(L("10 / 1,2", "10 / 1,2"), why = L("Tangens = protilehlá / přilehlá. Která strana leží naproti úhlu u země?", "Тангенс = противолежащий / прилежащий. Какая сторона лежит напротив угла у земли?")),
            Opt(L("1,2 / √(10² + 1,2²)", "1,2 / √(10² + 1,2²)"), why = L("Dělit délkou po svahu (přeponou) dává sinus, ne tangens.", "Деление на длину по склону (гипотенузу) даёт синус, а не тангенс.")),
        ),
        explain = L("tg α = převýšení / vodorovná délka = 1,2 / 10 = 0,12. Délku po svahu nepotřebuješ.", "tg α = подъём / горизонтальная длина = 1,2 / 10 = 0,12. Длина по склону не нужна."),
    )
    val s2 = numStep(
        question = L("Vypočítej úhel rampy α ve stupních (tg α = 0,12).", "Вычисли угол пандуса α в градусах (tg α = 0,12)."),
        label = L("Vypočítej úhel rampy ve stupních", "Вычисли угол наклона пандуса в градусах"),
        fields = listOf(NumSpec(L("α", "α"), Trig.atanD(0.12), "°", tol = Tol.ANGLE)),
        explain = L("tg α = 1,2 / 10 = 0,12, takže α = arctg 0,12 ≈ 6,8°. Stejný sklon je 12 %, nikoli 12°.", "tg α = 1,2 / 10 = 0,12, поэтому α = arctg 0,12 ≈ 6,8°. Этот уклон равен 12 %, а не 12°."),
        traps = listOf(
            NumTrap.near(L("12 je sklon v procentech, ne úhel. Ještě použij tan⁻¹.", "12 — это уклон в процентах, а не угол. Ещё нужно применить tan⁻¹."), 12.0),
            NumTrap.near(L("Kolem 0,12: buď je to teprve tg α (ještě tan⁻¹), nebo je kalkulačka v RAD (přepni na DEG).", "Около 0,12: либо это пока tg α (ещё tan⁻¹), либо калькулятор в RAD (переключи на DEG)."), 0.12),
            NumTrap.near(L("To je úhel u svislice – vyjde, když se dělí délka výškou. Co patří ve zlomku nahoru?", "Это угол у вертикали — так выходит, если делить длину на высоту. Что должно быть в дроби сверху?"), 90.0 - Trig.atanD(0.12)),
        ),
        nudge = L("α = tan⁻¹ 0,12 – kalkulačka v režimu DEG.", "α = tan⁻¹ 0,12 — калькулятор в режиме DEG."),
    )
    val solved = s2.done
    Stage(h = 150.0, description = t.pick(L("Rampa: 10 m vodorovně, 1,2 m nahoru", "Пандус: 10 м по горизонтали, 1,2 м вверх"))) { c ->
        val ox = 50.0
        val oy = 95.0
        val run = 300.0
        val rise = run * 0.12
        line(20.0, oy, 380.0, oy, c.ink, 1.2)
        poly(listOf(ox to oy, ox + run to oy, ox + run to oy - rise), c.subtle, null, alpha = 0.7f)
        line(ox, oy, ox + run, oy - rise, c.ink, 3.0)
        line(ox + run, oy, ox + run, oy - rise, c.derived, 2.0)
        dimLine(ox, oy + 16, ox + run, oy + 16, t.pick(L("10 m vodorovně", "10 м по горизонтали")), c, color = c.derived)
        text(ox + run + 8, oy - rise / 2 + 5, t.pick(L("1,2 m", "1,2 м")), c.derived, 14.0, Anchor.START)
        angleArc(ox, oy, 70.0, 0.0, -Trig.atanD(0.12), null, c)
        text(ox + 80, oy - 22, if (solved) "α ≈ 6,8°" else "α = ?", c.ink, 14.0, Anchor.START, bold = true)
    }
    CardText(L(
        "Rampa překonává výšku 1,2 m na vodorovné délce 10 m. Délka po svahu není zadaná.",
        "Пандус поднимается на 1,2 м при горизонтальной длине 10 м. Длина по склону не задана.",
    ))
    Steps(s1, s2)
}

internal fun trigLesson6() = Lesson(
    id = 106,
    title = L("Od stran zpátky k úhlu", "От сторон обратно к углу"),
    summary = L("arcsin, arccos, arctg a sklon zadaný poměrem nebo v procentech.", "arcsin, arccos, arctg и уклон, заданный отношением или в процентах."),
    cards = listOf(
        Card { BackToAngle() },
        Card { WhichInverse() },
        Card { CheckArccos() },
        Card { SlopeRatio() },
        Card { SlopePercent() },
        Card { RampAngle() },
    ),
)
