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
import cz.usharik.statika.learn.ValueSlider
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.poly
import cz.usharik.statika.learn.rect
import cz.usharik.statika.learn.rightAngleMark
import cz.usharik.statika.learn.rightTriangle
import cz.usharik.statika.learn.screenAngle
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.learn.triGeom
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Kurz trigonometrie – lekce 5 (id 105): Pythagorova věta, sin² + cos² = 1, tg = sin / cos a doplňkový úhel.
 */

/** Čtverec nad úsečkou p→q (na stranu [nx], [ny] = jednotková normála) s mřížkou n × n dílků. */
private fun PrimBuilder.areaSquare(p: Pt, q: Pt, nx: Double, ny: Double, n: Int, color: Color, c: StatikaColors) {
    val len = sqrt((q.x - p.x) * (q.x - p.x) + (q.y - p.y) * (q.y - p.y))
    val p3 = Pt(q.x + nx * len, q.y + ny * len)
    val p4 = Pt(p.x + nx * len, p.y + ny * len)
    poly(listOf(p.x to p.y, q.x to q.y, p3.x to p3.y, p4.x to p4.y), color.copy(alpha = 0.16f), color, 1.2)
    // mřížka jen u celočíselné strany (počet dílků = plocha)
    if (n in 2..12) for (i in 1 until n) {
        val t = i.toDouble() / n
        line(p.x + (q.x - p.x) * t, p.y + (q.y - p.y) * t, p4.x + (p3.x - p4.x) * t, p4.y + (p3.y - p4.y) * t, color, 0.6, alpha = 0.45f)
        line(p.x + nx * len * t, p.y + ny * len * t, q.x + nx * len * t, q.y + ny * len * t, color, 0.6, alpha = 0.45f)
    }
}

/** Karta 0: Pythagorova věta – čtverce nad stranami. */
@Composable
private fun CardScope.Pythagoras() {
    val t = tr()
    var a by remember { mutableIntStateOf(5) }
    var b by remember { mutableIntStateOf(2) }
    var hit by remember { mutableStateOf(false) }
    fun set(na: Int, nb: Int) {
        a = na
        b = nb
        if (a == 3 && b == 4) hit = true
    }
    val c2 = a * a + b * b
    val cLen = sqrt(c2.toDouble())
    val cInt = cLen.roundToInt().takeIf { it * it == c2 }
    Stage(h = 260.0, description = t.pick(L("Pravoúhlý trojúhelník se čtverci nad stranami", "Прямоугольный треугольник с квадратами на сторонах"))) { c ->
        // obrázek (trojúhelník + tři čtverce) vždy vyplní levou část scény
        val box = 236.0
        val wU = (a + 2 * b).toDouble()
        val hU = (2 * a + b).toDouble()
        val u = minOf(box / wU, box / hU, 26.0)
        val ax = 12.0 + (box - wU * u) / 2 + b * u
        val ay = 12.0 + (box - hU * u) / 2 + (a + b) * u
        val pa = Pt(ax, ay)
        val pc = Pt(ax + a * u, ay)
        val pb = Pt(ax + a * u, ay - b * u)
        areaSquare(pa, pc, 0.0, 1.0, a, c.derived, c)
        areaSquare(pc, pb, 1.0, 0.0, b, c.derived, c)
        // přepona: normála ven (doleva nahoru)
        val nx = -(pb.y - pa.y) / (cLen * u)
        val ny = (pb.x - pa.x) / (cLen * u)
        areaSquare(pa, pb, nx * -1, ny * -1, cInt ?: 0, c.load, c)
        val g = cz.usharik.statika.learn.TriGeom(pa, pb, pc)
        rightTriangle(g, c, angle = null)
        text((pa.x + pc.x) / 2, ay + a * u / 2 + 5, "a² = ${a * a}", c.derived, 13.0, bold = true)
        text(pc.x + b * u + 6, (pc.y + pb.y) / 2 + 5, "b² = ${b * b}", c.derived, 13.0, Anchor.START, bold = true)
        text((pa.x + pb.x) / 2 - 8, (pa.y + pb.y) / 2 - 4, "c", c.load, 15.0, Anchor.END, bold = true)
        // vpravo shrnutí
        text(392.0, 40.0, "a = $a, b = $b", c.ink, 14.0, Anchor.END)
        text(392.0, 64.0, "c² = ${a * a} + ${b * b} = $c2", c.ink, 14.0, Anchor.END)
        text(392.0, 88.0, if (cInt != null) "c = $cInt" else "c = √$c2 ≈ ${num(cLen, 2)}", c.load, 15.0, Anchor.END, bold = true)
    }
    CardText(L(
        "**Pythagorova věta**: a² + b² = c², kde c je přepona. Ze dvou stran pravoúhlého trojúhelníku tak dopočítáš třetí.",
        "**Теорема Пифагора**: a² + b² = c², где c — гипотенуза. По двум сторонам прямоугольного треугольника находишь третью.",
    ))
    SmallText(L(
        "a² (druhá mocnina) znamená a · a, třeba 3² = 9. Zpátky vede odmocnina: √25 = 5, protože 5 · 5 = 25.",
        "a² (квадрат) означает a · a, например 3² = 9. Обратно ведёт корень: √25 = 5, потому что 5 · 5 = 25.",
    ))
    Todo(L("Nastav posuvníky „Odvěsna a“ = 3 a „Odvěsna b“ = 4 – jak dlouhá je přepona?", "Поставь ползунки «Катет a» = 3 и «Катет b» = 4 — какой длины гипотенуза?") to hit)
    ValueSlider(t.pick(L("Odvěsna a = $a", "Катет a = $a")), a.toFloat(), 1f..8f, steps = 6) { set(it.roundToInt(), b) }
    ValueSlider(t.pick(L("Odvěsna b = $b", "Катет b = $b")), b.toFloat(), 1f..6f, steps = 4) { set(a, it.roundToInt()) }
    Readout("a² + b² = ${a * a} + ${b * b} = $c2 → c " + (cInt?.let { "= $it" } ?: "≈ ${num(cLen, 2)}"))
    if (hit) SmallText(L(
        "Plocha čtverce nad přeponou je stejná jako obou menších čtverců dohromady: 9 + 16 = 25 čtverečků.",
        "Площадь квадрата на гипотенузе равна площади двух меньших квадратов вместе: 9 + 16 = 25 клеток.",
    ))
    NumAnswer(
        question = L("Odvěsny měří 5 m a 12 m. Jak dlouhá je přepona?", "Катеты равны 5 м и 12 м. Какой длины гипотенуза?"),
        fields = listOf(NumSpec(L("c", "c"), 13.0, "m")),
        explain = L("c² = 25 + 144 = 169, c = √169 = 13 m.", "c² = 25 + 144 = 169, c = √169 = 13 м."),
        traps = listOf(
            NumTrap.near(L("Sčítají se **druhé mocniny**, ne strany. Kolik je 5² + 12²?", "Складываются **квадраты**, а не сами стороны. Сколько будет 5² + 12²?"), 17.0),
            NumTrap.near(L("169 je teprve c². Co ještě zbývá udělat?", "169 — это пока c². Что ещё осталось сделать?"), 169.0),
        ),
    )
}

/** Karta 1: přepona 1 → sin² α + cos² α = 1. */
@Composable
private fun CardScope.UnitHyp() {
    val t = tr()
    var alpha by remember { mutableIntStateOf(45) }
    var at30 by remember { mutableStateOf(false) }
    var at60 by remember { mutableStateOf(false) }
    val s = Trig.sinD(alpha.toDouble())
    val co = Trig.cosD(alpha.toDouble())
    Stage(h = 250.0, description = t.pick(L("Trojúhelník s přeponou 1 a pruh sin² + cos²", "Треугольник с гипотенузой 1 и полоса sin² + cos²"))) { c ->
        val r = 190.0
        val ax = 60.0
        val ay = 160.0
        val pa = Pt(ax, ay)
        val pc = Pt(ax + r * co, ay)
        val pb = Pt(ax + r * co, ay - r * s)
        if (alpha in 1..89) {
            rightTriangle(cz.usharik.statika.learn.TriGeom(pa, pb, pc), c, mapOf(TriSide.HYP to "1"), angle = "$alpha°")
            text((pa.x + pc.x) / 2, ay + 18, "cos α = ${num(co, 3)}", c.derived, 13.0)
            text(pc.x + 8, (pc.y + pb.y) / 2 + 5, "sin α = ${num(s, 3)}", c.derived, 13.0, Anchor.START)
        } else {
            line(pa.x, pa.y, pb.x, pb.y, c.load, 2.4)
            text(pb.x + 8, pb.y + 14, "1", c.load, 14.0, Anchor.START)
        }
        // pruh délky 1: sin² (plně) + cos² (světleji)
        val bx = 40.0
        val by = 205.0
        val bw = 320.0
        rect2(bx, by, bw * s * s, 22.0, c.derived, 0.9f)
        rect2(bx + bw * s * s, by, bw * co * co, 22.0, c.derived, 0.4f)
        line(bx, by, bx + bw, by, c.ink, 1.0)
        line(bx, by + 22, bx + bw, by + 22, c.ink, 1.0)
        line(bx, by, bx, by + 22, c.ink, 1.0)
        line(bx + bw, by, bx + bw, by + 22, c.ink, 1.0)
        if (s * s > 0.12) text(bx + bw * s * s / 2, by + 16, "sin²", c.paper, 12.0, bold = true)
        if (co * co > 0.12) text(bx + bw * s * s + bw * co * co / 2, by + 16, "cos²", c.ink, 12.0, bold = true)
        text(bx + bw + 6, by + 16, "= 1", c.ink, 13.0, Anchor.START, bold = true)
    }
    CardText(L(
        "Při přeponě 1 jsou odvěsny přímo sin α a cos α (lekce 2). Dosaď je do a² + b² = c²: **sin² α + cos² α = 1**. Zápis sin² α znamená (sin α)².",
        "При гипотенузе 1 катеты — это прямо sin α и cos α (урок 2). Подставь их в a² + b² = c²: **sin² α + cos² α = 1**. Запись sin² α означает (sin α)².",
    ))
    Todo(
        L("Nastav posuvníkem „Úhel α“ 30°", "Поставь ползунком «Угол α» 30°") to at30,
        L("Nastav α = 60° – změnil se součet?", "Поставь α = 60° — изменилась ли сумма?") to at60,
    )
    ValueSlider(t.pick(L("Úhel α = $alpha°", "Угол α = $alpha°")), alpha.toFloat(), 0f..90f, steps = 17) {
        alpha = (it / 5).roundToInt() * 5
        if (alpha == 30) at30 = true
        if (alpha == 60) at60 = true
    }
    Readout("sin² $alpha° + cos² $alpha° = ${num(s * s, 2)} + ${num(co * co, 2)} = 1")
    Quiz(
        question = L("sin α = 0,6 a α je ostrý úhel. Kolik je cos α?", "sin α = 0,6, α — острый угол. Чему равен cos α?"),
        options = listOf(
            Opt(L("0,8", "0,8"), correct = true),
            Opt(L("0,4", "0,4"), why = L("1 − 0,6 by platilo, kdyby se sčítaly sin a cos. Sčítají se ale jejich **druhé mocniny**.", "1 − 0,6 было бы верно, если бы складывались sin и cos. Но складываются их **квадраты**.")),
            Opt(L("0,64", "0,64"), why = L("0,64 = 1 − 0,6² je teprve cos² α. Co ještě zbývá?", "0,64 = 1 − 0,6² — это пока cos² α. Что ещё осталось?")),
            Opt(L("1,6", "1,6"), why = L("Kosinus nikdy není větší než 1.", "Косинус никогда не больше 1.")),
        ),
        explain = L(
            "cos² α = 1 − 0,36 = 0,64, tedy cos α = 0,8. Je to trojúhelník 3-4-5 zmenšený na přeponu 1.",
            "cos² α = 1 − 0,36 = 0,64, значит cos α = 0,8. Это треугольник 3-4-5, уменьшенный до гипотенузы 1.",
        ),
        columns = 2,
    )
}

private fun PrimBuilder.rect2(x: Double, y: Double, w: Double, h: Double, color: Color, alpha: Float) {
    if (w > 0.2) rect(x, y, w, h, color, null, alpha = alpha)
}

/** Karta 2: tangens = sklon – přilehlá odvěsna 1 m, výška = tg α. */
@Composable
private fun CardScope.TangentSlope() {
    val t = tr()
    var alpha by remember { mutableIntStateOf(20) }
    var at45 by remember { mutableStateOf(false) }
    var steep by remember { mutableStateOf(false) }
    val s = Trig.sinD(alpha.toDouble())
    val co = Trig.cosD(alpha.toDouble())
    val tg = Trig.tgD(alpha.toDouble())
    Stage(h = 250.0, description = t.pick(L("Sklon: na 1 m vodorovně vystoupáš o tg α", "Уклон: на 1 м по горизонтали поднимаешься на tg α"))) { c ->
        val unit = 110.0 // 1 m
        val ax = 70.0
        val ay = 220.0
        val top = 18.0
        val hx = ax + unit
        val rise = tg * unit
        val shownTop = (ay - rise).coerceAtLeast(top)
        line(20.0, ay, 380.0, ay, c.ink, 1.2)
        poly(listOf(ax to ay, hx to ay, hx to shownTop), c.subtle, null, alpha = 0.6f)
        line(ax, ay, hx, ay, c.derived, 2.4)
        line(hx, ay, hx, shownTop, c.derived, 2.4)
        // přepona – u velkého úhlu useknutá u horního okraje
        val hypEndX = if (ay - rise < top) ax + (ay - top) / tg else hx
        line(ax, ay, hypEndX, shownTop, c.load, 2.4)
        if (ay - rise < top) text(hx + 10, top + 12, "↑ " + t.pick(L("pokračuje", "продолжается")), c.inkSoft, 12.0, Anchor.START)
        rightAngleMark(Pt(hx, ay), Pt(ax, ay), Pt(hx, shownTop), c)
        if (alpha > 0) angleArc(ax, ay, 30.0, 0.0, -alpha.toDouble(), "$alpha°", c)
        text((ax + hx) / 2, ay + 18, "1 m", c.derived, 13.0)
        text(hx + 10, (ay + shownTop) / 2 + 5, "tg α = ${num(tg, 2)} m", c.derived, 14.0, Anchor.START, bold = true)
    }
    CardText(L(
        "**tg α = sin α / cos α**, tedy protilehlá ku přilehlé. Říká, o kolik se stoupne na 1 metr vodorovně – je to **sklon**.",
        "**tg α = sin α / cos α**, то есть противолежащий к прилежащему. Он показывает, на сколько поднимаешься на 1 метр по горизонтали, — это **уклон** (sklon).",
    ))
    Todo(
        L("Nastav posuvníkem „Úhel α“ 45° – tangens je přesně 1", "Поставь ползунком «Угол α» 45° — тангенс ровно 1") to at45,
        L("Zvětšuj α, dokud tg α nepřekročí 5", "Увеличивай α, пока tg α не превысит 5") to steep,
    )
    ValueSlider(t.pick(L("Úhel α = $alpha°", "Угол α = $alpha°")), alpha.toFloat(), 0f..85f, steps = 16) {
        alpha = (it / 5).roundToInt() * 5
        if (alpha == 45) at45 = true
        if (Trig.tgD(alpha.toDouble()) > 5) steep = true
    }
    Readout("tg $alpha° = sin $alpha° / cos $alpha° = ${num(s, 3)} / ${num(co, 3)} = ${num(tg, 2)}")
    SmallText(L(
        "Na rozdíl od sin a cos může být tangens větší než 1. U 90° roste nade všechny meze – tg 90° není definován.",
        "В отличие от sin и cos, тангенс может быть больше 1. У 90° он растёт без предела — tg 90° не определён.",
    ))
}

/** Karta 3: doplňkový úhel – sin α = cos (90° − α). */
@Composable
private fun CardScope.Complement() {
    val t = tr()
    var fromBeta by remember { mutableStateOf(false) }
    var sawBeta by remember { mutableStateOf(false) }
    var back by remember { mutableStateOf(false) }
    Stage(h = 230.0, description = t.pick(L("Trojúhelník s úhly α a β", "Треугольник с углами α и β"))) { c ->
        val g = triGeom(4.0, 3.0, fit = listOf(50.0, 40.0, 220.0, 165.0))
        val labels = mapOf(
            TriSide.OPP to "a",
            TriSide.ADJ to t.pick(if (!fromBeta) L("přilehlá k α", "прилежащий к α") else L("protilehlá k β", "противолежащий к β")),
            TriSide.HYP to "c",
        )
        text(392.0, 30.0, t.pick(if (!fromBeta) L("a je protilehlá k α", "a — противолежащий к α") else L("a je přilehlá k β", "a — прилежащий к β")), c.highlight, 14.0, Anchor.END, bold = true)
        rightTriangle(g, c, labels, TriSide.OPP, angle = null)
        // úhel α u vrcholu a, úhel β u vrcholu b; ten, ze kterého se díváš, je výrazný
        angleArc(g.a.x, g.a.y, 28.0, screenAngle(g.a, g.c), screenAngle(g.a, g.b), "α", c, if (!fromBeta) c.ink else c.inkSoft)
        angleArc(g.b.x, g.b.y, 26.0, screenAngle(g.b, g.a), screenAngle(g.b, g.c), "β", c, if (fromBeta) c.ink else c.inkSoft)
    }
    CardText(L(
        "Druhý ostrý úhel je β = 90° − α. Co je pro α protilehlá odvěsna, je pro β přilehlá: **sin α = cos (90° − α)**.",
        "Второй острый угол β = 90° − α. Катет, противолежащий для α, для β — прилежащий: **sin α = cos (90° − α)**.",
    ))
    Todo(
        L("Klepni na „Dívám se z úhlu β“", "Нажми «Смотрю из угла β»") to sawBeta,
        L("Vrať se na „Dívám se z úhlu α“", "Вернись на «Смотрю из угла α»") to back,
    )
    Chips(listOf(L("Dívám se z úhlu α", "Смотрю из угла α"), L("Dívám se z úhlu β", "Смотрю из угла β")), if (fromBeta) 1 else 0) { i ->
        fromBeta = i == 1
        if (fromBeta) sawBeta = true else if (sawBeta) back = true
    }
    Readout(if (!fromBeta) "sin α = a / c" else "cos β = a / c")
    Quiz(
        question = L("sin 20° je stejné číslo jako…", "sin 20° — то же число, что и…"),
        options = listOf(
            Opt(L("cos 70°", "cos 70°"), correct = true),
            Opt(L("cos 20°", "cos 20°"), why = L("sin a cos téhož úhlu se rovnají jen při 45°. Jak velký je druhý ostrý úhel?", "sin и cos одного угла равны только при 45°. Чему равен второй острый угол?")),
            Opt(L("sin 70°", "sin 70°"), why = L("To je sinus druhého úhlu – patří k druhé odvěsně. Která funkce úhlu 70° bere odvěsnu naproti 20°?", "Это синус второго угла — он относится к другому катету. Какая функция угла 70° берёт катет напротив 20°?")),
            Opt(L("1 − sin 70°", "1 − sin 70°"), why = L("Do jedné se sčítají druhé mocniny sin a cos téhož úhlu, ne siny dvou úhlů.", "До единицы складываются квадраты sin и cos одного угла, а не синусы двух углов.")),
        ),
        explain = L(
            "Oba ostré úhly dávají 90°. Odvěsna naproti 20° je zároveň přilehlá k 70°, proto sin 20° = cos 70°.",
            "Два острых угла дают 90°. Катет напротив 20° одновременно прилежит к 70°, поэтому sin 20° = cos 70°.",
        ),
        columns = 2,
    )
    SmallText(L(
        "Ve statice je to past: když je úhel síly zadaný od **svislice** místo od vodorovné, sin a cos si vymění místa.",
        "В статике это ловушка: если угол силы задан от **вертикали**, а не от горизонтали, sin и cos меняются местами.",
    ))
}

internal fun trigLesson5() = Lesson(
    id = 105,
    title = L("Vztahy mezi funkcemi", "Связи между функциями"),
    summary = L("Pythagorova věta, sin² + cos² = 1, tg = sin / cos a druhý úhel.", "Теорема Пифагора, sin² + cos² = 1, tg = sin / cos и второй угол."),
    cards = listOf(
        Card { Pythagoras() },
        Card { UnitHyp() },
        Card { TangentSlope() },
        Card { Complement() },
    ),
)
