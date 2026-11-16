package cz.usharik.statika.learn.trig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cz.usharik.statika.learn.Card
import cz.usharik.statika.learn.CardScope
import cz.usharik.statika.learn.CardText
import cz.usharik.statika.learn.Chips
import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Lesson
import cz.usharik.statika.learn.NumAnswer
import cz.usharik.statika.learn.NumSpec
import cz.usharik.statika.learn.NumTrap
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.learn.Pt
import cz.usharik.statika.learn.Quiz
import cz.usharik.statika.learn.Readout
import cz.usharik.statika.learn.SmallText
import cz.usharik.statika.learn.Stage
import cz.usharik.statika.learn.Todo
import cz.usharik.statika.learn.TriGeom
import cz.usharik.statika.learn.ValueSlider
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.dimLine
import cz.usharik.statika.learn.grid
import cz.usharik.statika.learn.handle
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.poly
import cz.usharik.statika.learn.rightAngleMark
import cz.usharik.statika.learn.rightTriangle
import cz.usharik.statika.learn.tapSpot
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.learn.triGeom
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.ui.Anchor
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Kurz trigonometrie – lekce 6 (id 106): od stran zpátky k úhlu – arcsin, arccos, arctg, sklon poměrem a v procentech.
 */

/** Karta 0: znáš strany, hledáš úhel – tg α = výška / základna. */
@Composable
private fun CardScope.BackToAngle() {
    val t = tr()
    var h by remember { mutableIntStateOf(2) }
    var at3 by remember { mutableStateOf(false) }
    var at4 by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val step = 30.0
    val ox = 80.0
    val oy = 210.0
    val base = 4
    fun setH(v: Int) {
        h = v.coerceIn(0, 6)
        if (h == 3) at3 = true
        if (h == 4) at4 = true
    }
    val alpha = Trig.atanD(h.toDouble() / base)
    val tipX = ox + base * step
    val tipY = oy - h * step
    Stage(
        h = 230.0,
        description = t.pick(L("Trojúhelník na mřížce s nastavitelnou výškou", "Треугольник на сетке с изменяемой высотой")),
        onDrag = { p, start ->
            if (start) dragging = p.dist(tipX, tipY) < 45 || abs(p.x - tipX) < 30
            if (dragging) setH(((oy - p.y) / step).roundToInt())
        },
        onDragEnd = { dragging = false },
    ) { c ->
        grid(ox, oy - 6 * step, step, base + 1, 6, c)
        if (h > 0) {
            rightTriangle(TriGeom(Pt(ox, oy), Pt(tipX, tipY), Pt(tipX, oy)), c, mapOf(TriSide.ADJ to "$base", TriSide.OPP to "$h"), angle = null)
            angleArc(ox, oy, 34.0, 0.0, -alpha, "${num(alpha, 1)}°", c)
        } else {
            line(ox, oy, tipX, oy, c.derived, 2.4)
        }
        handle(tipX, tipY, c, dragging)
        text(392.0, 40.0, "tg α = $h / $base = ${num(h.toDouble() / base, 2)}", c.ink, 14.0, Anchor.END)
        text(392.0, 64.0, "α = ${num(alpha, 1)}°", c.load, 15.0, Anchor.END, bold = true)
    }
    CardText(L(
        "Zatím se z úhlu počítal podíl stran. Teď obráceně: strany známe a hledáme **úhel**.",
        "До сих пор по углу находили отношение сторон. Теперь наоборот: стороны известны, ищем **угол**.",
    ))
    Todo(
        L("Nastav výšku 3 dílky (kroužkem nebo posuvníkem „Výška“) – jaký úhel vyjde?", "Поставь высоту 3 деления (кружком или ползунком «Высота») — какой получится угол?") to at3,
        L("Nastav výšku 4 dílky, stejnou jako základnu", "Поставь высоту 4 деления, как основание") to at4,
    )
    ValueSlider(t.pick(L("Výška = $h", "Высота = $h")), h.toFloat(), 0f..6f, steps = 5) { setH(it.roundToInt()) }
    Readout("tg α = $h / $base = ${num(h.toDouble() / base, 2)} → α = ${num(alpha, 1)}°")
    SmallText(L(
        "Otázka „který úhel má tangens 0,75?“ se zapisuje α = **arctg** 0,75. Na kalkulačce: **SHIFT** (nebo **2nd**, v telefonu **2nd**/**INV**) a **tan** – nad tlačítkem je tan⁻¹.",
        "Вопрос «у какого угла тангенс равен 0,75?» записывают так: α = **arctg** 0,75. На калькуляторе: **SHIFT** (или **2nd**, в телефоне **2nd**/**INV**) и **tan** — над кнопкой написано tan⁻¹.",
    ))
}

/** Karta 1: kterou obrácenou funkci – podle dvou známých stran. */
@Composable
private fun CardScope.WhichInverse() {
    val t = tr()
    // vybrané strany (nejvýš dvě, nejstarší vypadne)
    var picked by remember { mutableStateOf(listOf<TriSide>()) }
    var oppHyp by remember { mutableStateOf(false) }
    var adjHyp by remember { mutableStateOf(false) }
    var legs by remember { mutableStateOf(false) }
    fun toggle(s: TriSide) {
        picked = if (s in picked) picked - s else (picked + s).takeLast(2)
        val set = picked.toSet()
        if (set == setOf(TriSide.OPP, TriSide.HYP)) oppHyp = true
        if (set == setOf(TriSide.ADJ, TriSide.HYP)) adjHyp = true
        if (set == setOf(TriSide.ADJ, TriSide.OPP)) legs = true
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
    val set = picked.toSet()
    val result: Pair<String, String>? = when (set) {
        setOf(TriSide.OPP, TriSide.HYP) -> t.pick(L("α = arcsin (protilehlá / přepona)", "α = arcsin (противолежащий / гипотенуза)")) to "sin⁻¹"
        setOf(TriSide.ADJ, TriSide.HYP) -> t.pick(L("α = arccos (přilehlá / přepona)", "α = arccos (прилежащий / гипотенуза)")) to "cos⁻¹"
        setOf(TriSide.ADJ, TriSide.OPP) -> t.pick(L("α = arctg (protilehlá / přilehlá)", "α = arctg (противолежащий / прилежащий)")) to "tan⁻¹"
        else -> null
    }
    Stage(
        h = 258.0,
        description = t.pick(L("Trojúhelník, na jehož strany se dá klepnout", "Треугольник, на стороны которого можно нажать")),
        onTap = { p -> TriSide.entries.minByOrNull { mid(it).dist(p.x, p.y) }?.let { s -> if (mid(s).dist(p.x, p.y) < 50) toggle(s) } },
    ) { c ->
        rightTriangle(g, c, angle = "α")
        for (s in picked) {
            val (p, q) = g.side(s)
            line(p.x, p.y, q.x, q.y, c.highlight, 5.0)
        }
        for (s in TriSide.entries) {
            val m = mid(s)
            tapSpot(m.x, m.y, c, s in picked)
        }
        // popisky stran dál od kroužků
        val hm = mid(TriSide.HYP)
        text(hm.x - 26, hm.y - 18, t.pick(names.getValue(TriSide.HYP)), if (TriSide.HYP in picked) c.highlight else c.load, 14.0, Anchor.END)
        val om = mid(TriSide.OPP)
        text(om.x + 22, om.y + 5, t.pick(names.getValue(TriSide.OPP)), if (TriSide.OPP in picked) c.highlight else c.derived, 14.0, Anchor.START)
        val am = mid(TriSide.ADJ)
        text(am.x, am.y + 34, t.pick(names.getValue(TriSide.ADJ)), if (TriSide.ADJ in picked) c.highlight else c.derived, 14.0)
        if (result != null) {
            text(200.0, 228.0, result.first, c.ink, 14.0, bold = true)
            text(200.0, 247.0, t.pick(L("kalkulačka: ${result.second}", "калькулятор: ${result.second}")), c.inkSoft, 13.0)
        } else {
            text(200.0, 236.0, t.pick(L("vyber dvě strany, které znáš", "выбери две известные стороны")), c.inkSoft, 13.0)
        }
    }
    CardText(L(
        "Která **inverzní** (obrácená) funkce? Ta, jejíž podíl složíš ze dvou stran, které znáš.",
        "Какая обратная (inverzní) функция? Та, отношение которой можно составить из двух известных сторон.",
    ))
    Todo(
        L("Vyber protilehlou odvěsnu a přeponu (klepni na kroužky nebo na tlačítka)", "Выбери противолежащий катет и гипотенузу (нажми на кружки или на кнопки)") to oppHyp,
        L("Vyber přilehlou odvěsnu a přeponu", "Выбери прилежащий катет и гипотенузу") to adjHyp,
        L("Vyber obě odvěsny", "Выбери оба катета") to legs,
    )
    val order = listOf(TriSide.HYP, TriSide.ADJ, TriSide.OPP)
    // dublér klepnutí do obrázku – stejné přepínače jako tečky na stranách
    Chips(order.map { names.getValue(it) }, null, selectedSet = order.indices.filter { order[it] in picked }.toSet()) { i -> toggle(order[i]) }
    SmallText(L("Vybrané jsou vždy dvě strany – třetí klepnutí nahradí tu dřívější.", "Выбраны всегда две стороны — третье нажатие заменит более раннюю."))
    SmallText(L(
        "arcsin, arccos, arctg jsou na kalkulačce sin⁻¹, cos⁻¹, tan⁻¹. Výsledek je úhel ve stupních – jen v režimu DEG.",
        "arcsin, arccos, arctg на калькуляторе — это sin⁻¹, cos⁻¹, tan⁻¹. Результат — угол в градусах, но только в режиме DEG.",
    ))
    SmallText(L(
        "Pozor: sin⁻¹ neznamená 1/sin. Je to jen značka „zpátky k úhlu“.",
        "Внимание: sin⁻¹ не означает 1/sin. Это просто значок «обратно к углу».",
    ))
}

/** Karta 2: kvíz – přilehlá a přepona → arccos. */
@Composable
private fun CardScope.CheckArccos() {
    val t = tr()
    Stage(h = 210.0, description = t.pick(L("Trojúhelník: přilehlá 4 m, přepona 5 m, úhel α neznámý", "Треугольник: прилежащий катет 4 м, гипотенуза 5 м, угол α неизвестен"))) { c ->
        val g = triGeom(4.0, 3.0, fit = listOf(80.0, 25.0, 230.0, 160.0))
        rightTriangle(g, c, mapOf(TriSide.ADJ to "4 m", TriSide.HYP to "5 m"), null, "α = ?")
    }
    Quiz(
        question = L("Znáš přilehlou odvěsnu 4 m a přeponu 5 m. Jak spočítáš úhel α?", "Известны прилежащий катет 4 м и гипотенуза 5 м. Как найти угол α?"),
        options = listOf(
            Opt(L("α = arccos (4/5) = 36,9°", "α = arccos (4/5) = 36,9°"), correct = true),
            Opt(L("α = arcsin (4/5) = 53,1°", "α = arcsin (4/5) = 53,1°"), why = L("Sinus potřebuje odvěsnu **naproti** úhlu. Strana 4 m se ale úhlu α dotýká – jak se jí říká?", "Синусу нужен катет **напротив** угла. А сторона 4 м касается угла α — как она называется?")),
            Opt(L("α = arctg (4/5) = 38,7°", "α = arctg (4/5) = 38,7°"), why = L("Tangens dělí odvěsnu odvěsnou. Tady je jedna ze známých stran přepona.", "Тангенс делит катет на катет. А здесь одна из известных сторон — гипотенуза.")),
            Opt(L("α = cos (4/5)", "α = cos (4/5)"), why = L("cos udělá z úhlu podíl. My máme podíl a chceme úhel – která funkce jde opačným směrem?", "cos превращает угол в отношение. У нас есть отношение, а нужен угол — какая функция идёт в обратную сторону?")),
        ),
        explain = L(
            "Přilehlá a přepona → kosinus: cos α = 4/5, tedy α = arccos 0,8 = 36,9°. Úhel 53,1° je ten druhý ostrý.",
            "Прилежащий и гипотенуза → косинус: cos α = 4/5, значит α = arccos 0,8 = 36,9°. Угол 53,1° — это второй острый.",
        ),
    )
}

/** Karta 3: sklon zadaný poměrem výška : vodorovná délka = tg α. */
@Composable
private fun CardScope.SlopeRatio() {
    val t = tr()
    val ratios = listOf(1 to 1, 1 to 2, 3 to 4)
    var sel by remember { mutableStateOf<Int?>(null) }
    var seen by remember { mutableStateOf(setOf<Int>()) }
    Stage(h = 220.0, description = t.pick(L("Sklon na mřížce podle poměru", "Уклон на сетке по отношению"))) { c ->
        val step = 34.0
        val ox = 60.0
        val oy = 190.0
        grid(ox, oy - 4 * step, step, 4, 4, c)
        val i = sel
        if (i == null) {
            text(392.0, 110.0, t.pick(L("vyber sklon tlačítkem", "выбери уклон кнопкой")), c.inkSoft, 14.0, Anchor.END)
        } else {
            val (m, n) = ratios[i]
            // vodorovná délka n dílků → přes celou mřížku (4 dílky)
            val k = 4.0 / n
            val run = n * k * step
            val rise = m * k * step
            val g = TriGeom(Pt(ox, oy), Pt(ox + run, oy - rise), Pt(ox + run, oy))
            rightTriangle(g, c, mapOf(TriSide.ADJ to "$n", TriSide.OPP to "$m"), null, null, hyp = c.ink)
            val a = Trig.atanD(m.toDouble() / n)
            angleArc(ox, oy, 32.0, 0.0, -a, "${num(a, 1)}°", c)
            text(392.0, 40.0, t.pick(L("sklon $m : $n", "уклон $m : $n")), c.ink, 15.0, Anchor.END, bold = true)
            text(392.0, 64.0, "tg α = $m/$n = ${num(m.toDouble() / n, 2)}", c.ink, 14.0, Anchor.END)
            text(392.0, 88.0, "α = ${num(a, 1)}°", c.load, 15.0, Anchor.END, bold = true)
            if (m == 3 && n == 4) text(392.0, 112.0, t.pick(L("přepona = 5", "гипотенуза = 5")), c.inkSoft, 13.0, Anchor.END)
        }
    }
    CardText(L(
        "Sklon se na výkresu zadává poměrem **výška : vodorovná délka**, nejčastěji 1 : n (svah 1 : 2). U prutů bývá nakreslen trojúhelníček, třeba 3 : 4. Ten poměr je přímo tg α.",
        "Уклон на чертеже задают отношением **высота : горизонтальная длина**, чаще всего 1 : n (откос 1 : 2). У стержней рисуют треугольничек, например 3 : 4. Это отношение и есть tg α.",
    ))
    Todo(
        L("Klepni na „3 : 4“", "Нажми «3 : 4»") to (2 in seen),
        L("Klepni na „1 : 2“", "Нажми «1 : 2»") to (1 in seen),
        L("Klepni na „1 : 1“", "Нажми «1 : 1»") to (0 in seen),
    )
    Chips(ratios.map { (m, n) -> L("$m : $n", "$m : $n") }, sel) { i ->
        sel = i
        seen = seen + i
    }
    sel?.let { i ->
        val (m, n) = ratios[i]
        Readout(t.pick(L("sklon $m : $n → tg α = $m/$n → α = ${num(Trig.atanD(m.toDouble() / n), 1)}°", "уклон $m : $n → tg α = $m/$n → α = ${num(Trig.atanD(m.toDouble() / n), 1)}°")))
    }
    SmallText(L(
        "U sklonu 3 : 4 úhel ani nepotřebuješ: přepona je 5, takže sin α = 3/5 a cos α = 4/5.",
        "При уклоне 3 : 4 угол даже не нужен: гипотенуза равна 5, значит sin α = 3/5, а cos α = 4/5.",
    ))
    SmallText(L(
        "Vždy si ověř, co je v poměru první – výška, nebo délka. Nakresli si trojúhelníček.",
        "Всегда проверяй, что в отношении стоит первым — высота или длина. Нарисуй себе треугольничек.",
    ))
}

/** Karta 4: sklon v procentech = tg α · 100. */
@Composable
private fun CardScope.SlopePercent() {
    val t = tr()
    var p by remember { mutableIntStateOf(30) }
    var at10 by remember { mutableStateOf(false) }
    var at100 by remember { mutableStateOf(false) }
    val tg = p / 100.0
    val a = Trig.atanD(tg)
    Stage(h = 230.0, description = t.pick(L("Silnice se sklonem v procentech", "Дорога с уклоном в процентах"))) { c ->
        val ox = 70.0
        val oy = 185.0
        val run = 170.0 // 100 m
        val rise = run * tg
        line(20.0, oy, 380.0, oy, c.ink, 1.2)
        poly(listOf(ox to oy, ox + run to oy, ox + run to oy - rise), c.subtle, null, alpha = 0.7f)
        // silnice
        line(ox, oy, ox + run, oy - rise, c.ink, 6.0)
        line(ox, oy, ox + run, oy - rise, c.beam, 3.5)
        line(ox + run, oy, ox + run, oy - rise, c.derived, 1.4, dash = true)
        if (rise > 1) rightAngleMark(Pt(ox + run, oy), Pt(ox, oy), Pt(ox + run, oy - rise), c)
        dimLine(ox, oy + 14, ox + run, oy + 14, "100 m", c, color = c.derived)
        text(ox + run + 10, oy - rise / 2 + 5, "$p m", c.derived, 14.0, Anchor.START, bold = true)
        if (p > 0) {
            angleArc(ox, oy, 36.0, 0.0, -a, null, c)
            text(ox - 8, oy - 6, "α = ${num(a, 1)}°", c.inkSoft, 13.0, Anchor.END)
        }
        text(392.0, 36.0, "$p %", c.ink, 18.0, Anchor.END, bold = true)
    }
    CardText(L(
        "Sklon v procentech je tentýž podíl výška / vodorovná délka, jen vynásobený stem. 10 % znamená stoupání 10 m na 100 m.",
        "Уклон в процентах — то же отношение высота / горизонтальная длина, только умноженное на сто. 10 % означает подъём 10 м на 100 м.",
    ))
    Todo(
        L("Nastav posuvníkem „Sklon v %“ 10 %", "Поставь ползунком «Уклон в %» 10 %") to at10,
        L("Nastav sklon 100 % – kolik je to stupňů?", "Поставь уклон 100 % — сколько это градусов?") to at100,
    )
    ValueSlider(t.pick(L("Sklon v % = $p %", "Уклон в % = $p %")), p.toFloat(), 0f..100f, steps = 19) {
        p = (it / 5).roundToInt() * 5
        if (p == 10) at10 = true
        if (p == 100) at100 = true
    }
    Readout("$p % → tg α = ${num(tg, 2)} → α = ${num(a, 1)}°")
    Quiz(
        question = L("Střecha má sklon 100 %. Jaký je to úhel?", "Уклон крыши 100 %. Какой это угол?"),
        options = listOf(
            Opt(L("45°", "45°"), correct = true),
            Opt(L("90°", "90°"), why = L("90° je svislá stěna. Sklon 100 % znamená 100 m výšky na 100 m délky – jak takový trojúhelník vypadá?", "90° — это вертикальная стена. Уклон 100 % означает 100 м высоты на 100 м длины — как выглядит такой треугольник?")),
            Opt(L("100°", "100°"), why = L("Procenta nejsou stupně – jsou to setiny tangentu.", "Проценты — не градусы, это сотые доли тангенса.")),
            Opt(L("60°", "60°"), why = L("Spočítej to: tg α = 100/100 = 1. Který úhel má tangens 1?", "Посчитай: tg α = 100/100 = 1. У какого угла тангенс равен 1?")),
        ),
        explain = L("Sklon v % = tg α · 100. 100 % → tg α = 1 → α = 45°.", "Уклон в % = tg α · 100. 100 % → tg α = 1 → α = 45°."),
        columns = 2,
    )
}

/** Karta 5: úloha – úhel rampy ze stoupání a délky. */
@Composable
private fun CardScope.RampAngle() {
    val t = tr()
    Stage(h = 190.0, description = t.pick(L("Rampa: 10 m vodorovně, 1,2 m nahoru", "Пандус: 10 м по горизонтали, 1,2 м вверх"))) { c ->
        val ox = 70.0
        val oy = 140.0
        val run = 280.0
        val rise = run * 0.12
        line(20.0, oy, 380.0, oy, c.ink, 1.2)
        poly(listOf(ox to oy, ox + run to oy, ox + run to oy - rise), c.subtle, null, alpha = 0.7f)
        line(ox, oy, ox + run, oy - rise, c.ink, 3.0)
        line(ox + run, oy, ox + run, oy - rise, c.derived, 2.0)
        dimLine(ox, oy + 16, ox + run, oy + 16, "10 m", c, color = c.derived)
        text(ox + run + 8, oy - rise / 2 + 5, "1,2 m", c.derived, 14.0, Anchor.START)
        angleArc(ox, oy, 60.0, 0.0, -Trig.atanD(0.12), null, c)
        text(ox - 8, oy - 6, "α = ?", c.ink, 14.0, Anchor.END, bold = true)
    }
    NumAnswer(
        question = L("Rampa vystoupá 1,2 m na vodorovné délce 10 m. Jaký úhel svírá se zemí?", "Пандус поднимается на 1,2 м на горизонтальной длине 10 м. Какой угол он составляет с землёй?"),
        fields = listOf(NumSpec(L("α", "α"), Trig.atanD(0.12), "°", tol = cz.usharik.statika.practice.Tol.ANGLE)),
        explain = L("tg α = 1,2 / 10 = 0,12, α = arctg 0,12 ≈ 6,8°.", "tg α = 1,2 / 10 = 0,12, α = arctg 0,12 ≈ 6,8°."),
        traps = listOf(
            NumTrap.near(L("12 je sklon v procentech, ne úhel. Ještě použij tan⁻¹.", "12 — это уклон в процентах, а не угол. Ещё нужно применить tan⁻¹."), 12.0),
            NumTrap.near(L("0,12 je teprve tg α (nebo je kalkulačka v RAD). Úhel dostaneš přes tan⁻¹ v režimu DEG.", "0,12 — это пока tg α (или калькулятор в RAD). Угол получишь через tan⁻¹ в режиме DEG."), 0.12),
            NumTrap.near(L("To je úhel u svislice – vyjde, když se dělí délka výškou. Co patří ve zlomku nahoru?", "Это угол у вертикали — так выходит, если делить длину на высоту. Что должно быть в дроби сверху?"), 90.0 - Trig.atanD(0.12)),
        ),
    )
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
