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
import cz.usharik.statika.learn.Readout
import cz.usharik.statika.learn.SmallText
import cz.usharik.statika.learn.Stage
import cz.usharik.statika.learn.Step
import cz.usharik.statika.learn.Steps
import cz.usharik.statika.learn.ValueSlider
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.circle
import cz.usharik.statika.learn.dimLine
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.numStep
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.poly
import cz.usharik.statika.learn.quizStep
import cz.usharik.statika.learn.rect
import cz.usharik.statika.learn.rightAngleMark
import cz.usharik.statika.learn.rightTriangle
import cz.usharik.statika.learn.screenAngle
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.learn.triGeom
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Kurz trigonometrie – lekce 5 (id 105): Pythagorova věta, sin² + cos² = 1, tg = sin / cos a doplňkový úhel.
 */

/**
 * Tip jako krok: volby a „Ověřit“ v doku. Tip se jen zaznamená (není to známka) – vyhodnotí ho až další krok.
 * [guess] = zvolený tip (null = ještě nic), [result] = co se ukáže ve výkladu po odeslání.
 */
private fun predictStep(label: L, options: List<L>, guess: Int?, onGuess: (Int) -> Unit, result: (@Composable () -> Unit)? = null): Step =
    Step(label, guess != null, result = result ?: { if (guess != null) SmallText(L("Tvůj tip: ${options[guess].cs}", "Твой прогноз: ${options[guess].ru}")) }) {
        var sel by remember { mutableStateOf<Int?>(null) }
        Chips(options, sel) { sel = it }
        ActionButton(L("Ověřit", "Проверить"), enabled = sel != null) { sel?.let(onGuess) }
    }

/** Čtverec nad úsečkou p→q (na stranu [nx], [ny] = jednotková normála) s mřížkou n × n dílků. */
private fun PrimBuilder.areaSquare(p: Pt, q: Pt, nx: Double, ny: Double, n: Int, color: Color, c: StatikaColors, alpha: Float = 1f) {
    val len = sqrt((q.x - p.x) * (q.x - p.x) + (q.y - p.y) * (q.y - p.y))
    val p3 = Pt(q.x + nx * len, q.y + ny * len)
    val p4 = Pt(p.x + nx * len, p.y + ny * len)
    poly(listOf(p.x to p.y, q.x to q.y, p3.x to p3.y, p4.x to p4.y), color.copy(alpha = 0.16f * alpha), color, 1.2, alpha = alpha)
    // mřížka jen u celočíselné strany (počet dílků = plocha)
    if (n in 2..12) for (i in 1 until n) {
        val t = i.toDouble() / n
        line(p.x + (q.x - p.x) * t, p.y + (q.y - p.y) * t, p4.x + (p3.x - p4.x) * t, p4.y + (p3.y - p4.y) * t, color, 0.6, alpha = 0.45f * alpha)
        line(p.x + nx * len * t, p.y + ny * len * t, q.x + nx * len * t, q.y + ny * len * t, color, 0.6, alpha = 0.45f * alpha)
    }
}

/**
 * Karta 0: Pythagorova věta – čtverce nad stranami. Tip přepony 3-4, ukázka ploch, pak lano 12 m × 5 m.
 * Po dokončení se dá s odvěsnami volně hýbat.
 */
@Composable
private fun CardScope.Pythagoras() {
    val t = tr()
    var guess by remember { mutableStateOf<Int?>(null) }
    var phase by remember { mutableIntStateOf(0) } // ukázka: 0 nic, 1 a², 2 b², 3 c², 4 c
    var demoDone by remember { mutableStateOf(false) }
    var a by remember { mutableIntStateOf(3) }
    var b by remember { mutableIntStateOf(4) }
    var explored by remember { mutableStateOf(false) }
    LaunchedEffect(guess != null) {
        if (guess == null) return@LaunchedEffect
        for (p in 1..4) {
            delay(800)
            phase = p
        }
        delay(500)
        demoDone = true
    }
    val tips = listOf(L("c = 5", "c = 5"), L("c = 7", "c = 7"), L("c = 25", "c = 25"))
    val s1 = predictStep(L("Odhadni přeponu při odvěsnách 3 a 4", "Предскажи гипотенузу при катетах 3 и 4"), tips, guess, { guess = it })
    val s2 = Step(
        L("Porovnej plochy čtverců nad stranami", "Сравни площади квадратов на сторонах"), demoDone,
        result = {
            when (guess) {
                0 -> Note(Tone.OK, L("Tip sedí: 9 + 16 = 25 čtverečků, takže c = √25 = 5.", "Прогноз верен: 9 + 16 = 25 клеток, значит c = √25 = 5."))
                1 -> Note(Tone.INFO, L("7 = 3 + 4 by vyšlo, kdyby se sčítaly délky. Sčítají se ale **plochy** čtverců: 9 + 16 = 25, tedy c = 5.", "7 = 3 + 4 получилось бы, если бы складывались длины. Но складываются **площади** квадратов: 9 + 16 = 25, значит c = 5."))
                else -> Note(Tone.INFO, L("25 je plocha čtverce nad přeponou, tedy c². Délka je c = √25 = 5.", "25 — площадь квадрата на гипотенузе, то есть c². Длина c = √25 = 5."))
            }
        },
    ) { SmallText(L("Sleduj obrázek: kolik čtverečků je nad každou stranou…", "Смотри на рисунок: сколько клеток на каждой стороне…")) }
    val s3 = numStep(
        question = L("Lano vede mezi body vzdálenými 12 m vodorovně a 5 m svisle. Jak je dlouhé?", "Трос натянут между точками, разнесёнными на 12 м по горизонтали и 5 м по вертикали. Какова его длина?"),
        label = L("Vypočítej délku lana mezi vyznačenými body", "Вычисли длину троса между отмеченными точками"),
        fields = listOf(NumSpec(L("c", "c"), 13.0, t.pick(L("m", "м")))),
        explain = L(
            "c² = 5² + 12² = 25 + 144 = 169 m², tedy c = √169 = 13 m. Sčítáme plochy čtverců, ne délky stran.",
            "c² = 5² + 12² = 25 + 144 = 169 м², значит c = √169 = 13 м. Складываются площади квадратов, а не длины сторон.",
        ),
        traps = listOf(
            NumTrap.near(L("Sčítají se **druhé mocniny**, ne strany. Kolik je 5² + 12²?", "Складываются **квадраты**, а не сами стороны. Сколько будет 5² + 12²?"), 17.0),
            NumTrap.near(L("169 m² je teprve c² – plocha. Co ještě zbývá udělat?", "169 м² — это пока c², площадь. Что ещё осталось сделать?"), 169.0),
        ),
        nudge = L("c² = 12² + 5². Nejdřív sečti druhé mocniny, pak odmocni.", "c² = 12² + 5². Сначала сложи квадраты, потом извлеки корень."),
    )
    val steps = listOf(s1, s2, s3)
    val cur = steps.indexOfFirst { !it.done }
    val rope = cur == 2 || (cur < 0 && !explored)
    Stage(h = 260.0, description = t.pick(L("Pravoúhlý trojúhelník se čtverci nad stranami", "Прямоугольный треугольник с квадратами на сторонах"))) { c ->
        if (rope) {
            // lano mezi bodem A na zemi a bodem B na stožáru: 12 m vodorovně, 5 m svisle
            val u = 24.0
            val ax = 50.0
            val ay = 200.0
            val bx = ax + 12 * u
            val by = ay - 5 * u
            line(20.0, ay, 380.0, ay, c.ink, 1.2)
            rect(bx, by - 20, 10.0, ay - by + 20, c.beam, c.ink, 1.2)
            line(ax, ay, bx, ay, c.derived, 1.0, dash = true)
            line(bx, ay, bx, by, c.derived, 1.0, dash = true)
            rightAngleMark(Pt(bx, ay), Pt(ax, ay), Pt(bx, by), c)
            line(ax, ay, bx, by, c.load, 2.4)
            circle(ax, ay, 5.0, c.ink)
            circle(bx, by, 5.0, c.ink)
            dimLine(ax, ay + 18, bx, ay + 18, t.pick(L("12 m", "12 м")), c, color = c.derived)
            text(bx - 10, (ay + by) / 2 + 5, t.pick(L("5 m", "5 м")), c.derived, 14.0, Anchor.END, bold = true)
            val done = cur < 0
            text((ax + bx) / 2 - 10, (ay + by) / 2 - 14, if (done) t.pick(L("c = 13 m", "c = 13 м")) else "c = ?", c.load, 15.0, Anchor.END, bold = true)
            return@Stage
        }
        val la = if (cur < 0) a else 3
        val lb = if (cur < 0) b else 4
        val show = if (cur < 0) 4 else phase
        val c2 = la * la + lb * lb
        val cLen = sqrt(c2.toDouble())
        val cInt = cLen.roundToInt().takeIf { it * it == c2 }
        // obrázek (trojúhelník + tři čtverce) vždy vyplní levou část scény
        val box = 236.0
        val wU = (la + 2 * lb).toDouble()
        val hU = (2 * la + lb).toDouble()
        val u = minOf(box / wU, box / hU, 26.0)
        val ax = 12.0 + (box - wU * u) / 2 + lb * u
        val ay = 12.0 + (box - hU * u) / 2 + (la + lb) * u
        val pa = Pt(ax, ay)
        val pc = Pt(ax + la * u, ay)
        val pb = Pt(ax + la * u, ay - lb * u)
        areaSquare(pa, pc, 0.0, 1.0, la, c.derived, c, if (show == 1 || show >= 3) 1f else 0.55f)
        areaSquare(pc, pb, 1.0, 0.0, lb, c.derived, c, if (show >= 2) 1f else 0.55f)
        // přepona: normála ven (doleva nahoru); mřížka se ukáže až v ukázce
        val nx = -(pb.y - pa.y) / (cLen * u)
        val ny = (pb.x - pa.x) / (cLen * u)
        areaSquare(pa, pb, -nx, -ny, if (show >= 3) cInt ?: 0 else 0, c.load, c, if (show >= 3) 1f else 0.5f)
        rightTriangle(cz.usharik.statika.learn.TriGeom(pa, pb, pc), c, angle = null)
        text((pa.x + pc.x) / 2, ay + la * u / 2 + 5, "a² = ${la * la}", c.derived, 13.0, bold = true)
        text(pc.x + lb * u + 6, (pc.y + pb.y) / 2 + 5, "b² = ${lb * lb}", c.derived, 13.0, Anchor.START, bold = true)
        text((pa.x + pb.x) / 2 - 8, (pa.y + pb.y) / 2 - 4, "c", c.load, 15.0, Anchor.END, bold = true)
        // vpravo shrnutí – výsledek až po ukázce
        text(392.0, 40.0, "a = $la, b = $lb", c.ink, 14.0, Anchor.END)
        if (show >= 3) text(392.0, 64.0, "c² = ${la * la} + ${lb * lb} = $c2", c.ink, 14.0, Anchor.END)
        text(392.0, 88.0, if (show < 4) "c = ?" else if (cInt != null) "c = $cInt" else "c = √$c2 ≈ ${num(cLen, 2)}", c.load, 15.0, Anchor.END, bold = true)
    }
    CardText(L(
        "V pravoúhlém trojúhelníku platí **a² + b² = c²**, kde c je přepona. Druhá mocnina délky odpovídá ploše čtverce.",
        "В прямоугольном треугольнике **a² + b² = c²**, где c — гипотенуза. Квадрат длины соответствует площади квадрата.",
    ))
    SmallText(L(
        "a² znamená a · a, třeba 3² = 9. Zpátky vede odmocnina: √25 = 5, protože 5 · 5 = 25.",
        "a² означает a · a, например 3² = 9. Обратно ведёт корень: √25 = 5, потому что 5 · 5 = 25.",
    ))
    Steps(*steps.toTypedArray(), after = {
        ValueSlider(t.pick(L("Odvěsna a = $a", "Катет a = $a")), a.toFloat(), 1f..8f, steps = 6) { a = it.roundToInt(); explored = true }
        ValueSlider(t.pick(L("Odvěsna b = $b", "Катет b = $b")), b.toFloat(), 1f..6f, steps = 4) { b = it.roundToInt(); explored = true }
    })
}

/** Karta 1: přepona 1 → sin² α + cos² α = 1. */
@Composable
private fun CardScope.UnitHyp() {
    val t = tr()
    var guess by remember { mutableStateOf<Int?>(null) }
    var alpha by remember { mutableIntStateOf(45) }
    var tested by remember { mutableStateOf(false) }
    val s1 = predictStep(
        L("Odhadni: změní se sin² α + cos² α, když změníš úhel?", "Предскажи: изменится ли sin² α + cos² α при смене угла?"),
        listOf(L("Změní se", "Изменится"), L("Nezmění se", "Не изменится")), guess, { guess = it },
    )
    val s2 = Step(
        L("Změň úhel aspoň o 15° a sleduj součet sin² α + cos² α", "Измени угол хотя бы на 15° и следи за суммой sin² α + cos² α"), tested,
        result = {
            Note(if (guess == 1) Tone.OK else Tone.INFO, L(
                (if (guess == 1) "Tip sedí. " else "Součet se nezměnil. ") + "Jedna část pruhu roste, druhá stejně ubývá – sin² α + cos² α = 1 pro každý úhel.",
                (if (guess == 1) "Прогноз верен. " else "Сумма не изменилась. ") + "Одна часть полосы растёт, другая настолько же убывает — sin² α + cos² α = 1 для любого угла.",
            ))
        },
    ) {
        val s = Trig.sinD(alpha.toDouble())
        val co = Trig.cosD(alpha.toDouble())
        ValueSlider(t.pick(L("Úhel α = $alpha°", "Угол α = $alpha°")), alpha.toFloat(), 10f..80f, steps = 13) { alpha = (it / 5).roundToInt() * 5 }
        Readout("sin² $alpha° + cos² $alpha° = ${num(s * s, 2)} + ${num(co * co, 2)} = ${num(s * s + co * co, 2)}")
        ActionButton(L("Ověřit", "Проверить"), enabled = abs(alpha - 45) >= 15) { tested = true }
        if (abs(alpha - 45) < 15) SmallText(L("Posuň úhel aspoň o 15° od 45°.", "Сдвинь угол хотя бы на 15° от 45°."))
    }
    val s3 = quizStep(
        question = L("sin α = 0,6 a α je ostrý úhel. Kolik je cos α?", "sin α = 0,6, α — острый угол. Чему равен cos α?"),
        label = L("Urči cos α, když sin α = 0,6 a α je ostrý", "Найди cos α, если sin α = 0,6 и α — острый угол"),
        options = listOf(
            Opt(L("0,8", "0,8"), correct = true),
            Opt(L("0,4", "0,4"), why = L("1 − 0,6 by platilo, kdyby se sčítaly sin a cos. Sčítají se ale jejich **druhé mocniny**.", "1 − 0,6 было бы верно, если бы складывались sin и cos. Но складываются их **квадраты**.")),
            Opt(L("0,64", "0,64"), why = L("0,64 = 1 − 0,6² je teprve cos² α. Co ještě zbývá?", "0,64 = 1 − 0,6² — это пока cos² α. Что ещё осталось?")),
            Opt(L("1,6", "1,6"), why = L("Odvěsna nemůže být delší než přepona 1 – kosinus ostrého úhlu je menší než 1.", "Катет не может быть длиннее гипотенузы 1 — косинус острого угла меньше 1.")),
        ),
        explain = L(
            "cos² α = 1 − 0,6² = 0,64. Úhel α je ostrý, takže cos α je kladný: cos α = 0,8. Je to trojúhelník 3-4-5 zmenšený na přeponu 1.",
            "cos² α = 1 − 0,6² = 0,64. Угол α острый, поэтому cos α положителен: cos α = 0,8. Это треугольник 3-4-5, уменьшенный до гипотенузы 1.",
        ),
        columns = 2,
    )
    val steps = listOf(s1, s2, s3)
    val cur = steps.indexOfFirst { !it.done }
    val quiz = cur == 2 || cur < 0
    val ad = if (quiz) Trig.asinD(0.6) else alpha.toDouble()
    Stage(h = 250.0, description = t.pick(L("Trojúhelník s přeponou 1 a pruh sin² + cos²", "Треугольник с гипотенузой 1 и полоса sin² + cos²"))) { c ->
        val s = Trig.sinD(ad)
        val co = Trig.cosD(ad)
        val r = 190.0
        val ax = 60.0
        val ay = 160.0
        val pa = Pt(ax, ay)
        val pc = Pt(ax + r * co, ay)
        val pb = Pt(ax + r * co, ay - r * s)
        rightTriangle(cz.usharik.statika.learn.TriGeom(pa, pb, pc), c, mapOf(TriSide.HYP to "1"), angle = if (quiz) "α" else "${ad.roundToInt()}°")
        if (quiz) {
            text((pa.x + pc.x) / 2, ay + 18, if (cur < 0) "cos α = 0,8" else "cos α = ?", c.derived, 13.0, bold = true)
            text(pc.x + 8, (pc.y + pb.y) / 2 + 5, "sin α = 0,6", c.derived, 13.0, Anchor.START, bold = true)
            return@Stage
        }
        text((pa.x + pc.x) / 2, ay + 18, "cos α = ${num(co, 3)}", c.derived, 13.0)
        text(pc.x + 8, (pc.y + pb.y) / 2 + 5, "sin α = ${num(s, 3)}", c.derived, 13.0, Anchor.START)
        if (cur == 0) return@Stage // při tipu pruh nenapovídá
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
        "Při přeponě 1 mají odvěsny délky sin α a cos α. Pythagorova věta proto spojuje jejich druhé mocniny.",
        "При гипотенузе 1 длины катетов равны sin α и cos α. Поэтому теорема Пифагора связывает их квадраты.",
    ))
    SmallText(L(
        "sin² α znamená (sin α)² – nejdřív sinus, pak na druhou. Není to sin (α²).",
        "sin² α означает (sin α)² — сначала синус, потом в квадрат. Это не sin (α²).",
    ))
    Steps(*steps.toTypedArray())
}

private fun PrimBuilder.rect2(x: Double, y: Double, w: Double, h: Double, color: Color, alpha: Float) {
    if (w > 0.2) rect(x, y, w, h, color, null, alpha = alpha)
}

/** Karta 2: tangens = sklon – převýšení h na vodorovnou délku d = 1 m. Obě osy ve stejném měřítku. */
@Composable
private fun CardScope.TangentSlope() {
    val t = tr()
    var alpha by remember { mutableIntStateOf(20) }
    var at45 by remember { mutableStateOf(false) }
    var miss1 by remember { mutableStateOf<L?>(null) }
    var guess by remember { mutableStateOf<Int?>(null) }
    var steep by remember { mutableStateOf(false) }
    var miss3 by remember { mutableStateOf<L?>(null) }
    val tg = Trig.tgD(alpha.toDouble())
    val hText = t.pick(L("h = ${num(tg, 2)} m", "h = ${num(tg, 2)} м"))
    val s1 = Step(
        L("Najdi úhel, při kterém stoupneš o 1 m na 1 m vodorovně", "Найди угол, при котором подъём 1 м на 1 м по горизонтали"), at45,
        result = { Note(Tone.OK, L("Při 45° je h = d, tedy tg 45° = 1 m / 1 m = 1.", "При 45° h = d, значит tg 45° = 1 м / 1 м = 1.")) },
    ) {
        ValueSlider(t.pick(L("Úhel α = $alpha°", "Угол α = $alpha°")), alpha.toFloat(), 0f..80f) {
            alpha = it.roundToInt()
            miss1 = null
        }
        Readout(hText)
        ActionButton(L("Zkontrolovat", "Проверить")) {
            if (alpha == 45) at45 = true
            else miss1 = if (tg < 1) L("h = ${num(tg, 2)} m – ještě méně než 1 m. Zvětši úhel.", "h = ${num(tg, 2)} м — пока меньше 1 м. Увеличь угол.")
            else L("h = ${num(tg, 2)} m – už víc než 1 m. Zmenši úhel.", "h = ${num(tg, 2)} м — уже больше 1 м. Уменьши угол.")
        }
        miss1?.let { Note(Tone.NUDGE, it) }
    }
    val s2 = predictStep(
        L("Odhadni: může být tg α větší než 5?", "Предскажи: может ли tg α быть больше 5?"),
        listOf(L("Ano", "Да"), L("Ne, nejvýš 1", "Нет, не больше 1")), guess, { guess = it },
    )
    val s3 = Step(
        L("Najdi úhel, při kterém je tg α větší než 5", "Найди угол, при котором tg α больше 5"), steep,
        result = {
            Note(if (guess == 0) Tone.OK else Tone.INFO, L(
                (if (guess == 0) "Tip sedí. " else "Na rozdíl od sinu a kosinu tangens 1 překročit může. ") +
                    "Při ${alpha}° je tg α ≈ ${num(tg, 2)}. Když se úhel blíží 90°, d zůstává 1 m a h roste nade všechny meze. tg 90° není definován.",
                (if (guess == 0) "Прогноз верен. " else "В отличие от синуса и косинуса, тангенс может быть больше 1. ") +
                    "При ${alpha}° tg α ≈ ${num(tg, 2)}. Когда угол приближается к 90°, d остаётся 1 м, а h растёт неограниченно. tg 90° не определён.",
            ))
        },
    ) {
        ValueSlider(t.pick(L("Úhel α = $alpha°", "Угол α = $alpha°")), alpha.coerceIn(45, 85).toFloat(), 45f..85f) {
            alpha = it.roundToInt()
            miss3 = null
        }
        Readout("$hText · tg α = ${num(tg, 2)}")
        ActionButton(L("Zkontrolovat", "Проверить")) {
            if (tg > 5) steep = true
            else miss3 = L("tg α = ${num(tg, 2)} – ještě to nestačí. Strměji!", "tg α = ${num(tg, 2)} — пока мало. Круче!")
        }
        miss3?.let { Note(Tone.NUDGE, it) }
    }
    Stage(h = 250.0, description = t.pick(L("Sklon: převýšení h na vodorovné délce 1 m", "Уклон: подъём h на горизонтальной длине 1 м"))) { c ->
        val ax = 70.0
        val ay = 215.0
        val top = 22.0
        // obě osy ve stejném měřítku: u strmého svahu se zmenší celý trojúhelník, ne jen výška
        val unit = minOf(110.0, (ay - top) / tg.coerceAtLeast(1e-9))
        val hx = ax + unit
        val rise = tg * unit
        line(20.0, ay, 380.0, ay, c.ink, 1.2)
        poly(listOf(ax to ay, hx to ay, hx to ay - rise), c.subtle, null, alpha = 0.6f)
        line(ax, ay, hx, ay, c.derived, 2.4)
        line(hx, ay, hx, ay - rise, c.derived, 2.4)
        line(ax, ay, hx, ay - rise, c.load, 2.4)
        if (rise > 12) rightAngleMark(Pt(hx, ay), Pt(ax, ay), Pt(hx, ay - rise), c)
        if (alpha > 0) angleArc(ax, ay, 26.0, 0.0, -alpha.toDouble(), "$alpha°", c)
        text((ax + hx) / 2, ay + 20, t.pick(L("d = 1 m", "d = 1 м")), c.derived, 13.0, bold = true)
        text(hx + 10, ay - rise / 2 + 5, hText, c.derived, 14.0, Anchor.START, bold = true)
        text(392.0, 36.0, "tg α = h / d = ${num(tg, 2)}", c.ink, 15.0, Anchor.END, bold = true)
        if (unit < 109.0) text(392.0, 58.0, t.pick(L("měřítko zmenšeno", "масштаб уменьшен")), c.inkSoft, 12.0, Anchor.END)
    }
    CardText(L(
        "Sklon je poměr převýšení h k vodorovné délce d: **tg α = h / d** (protilehlá ku přilehlé). Je to číslo bez jednotky.",
        "Уклон — отношение подъёма h к горизонтальной длине d: **tg α = h / d** (противолежащий к прилежащему). Это число без единиц.",
    ))
    SmallText(L("Také platí tg α = sin α / cos α.", "Также tg α = sin α / cos α."))
    Steps(s1, s2, s3)
}

/** Karta 3: doplňkový úhel – sin α = cos (90° − α). */
@Composable
private fun CardScope.Complement() {
    val t = tr()
    var phase by remember { mutableIntStateOf(0) } // ukázka: 0 z α, 1 z β, 2 obojí
    var demoDone by remember { mutableStateOf(false) }
    val s1 = quizStep(
        question = L("Odvěsna a leží naproti úhlu α. Jakou roli má vůči úhlu β?", "Катет a лежит напротив угла α. Какова его роль относительно угла β?"),
        label = L("Urči roli zvýrazněné odvěsny a vůči úhlu β", "Определи роль выделенного катета a относительно угла β"),
        options = listOf(
            Opt(L("Přilehlá", "Прилежащий"), correct = true),
            Opt(L("Protilehlá", "Противолежащий"), why = L("Naproti β leží jiná odvěsna – ta vodorovná. Dotýká se a vrcholu s úhlem β?", "Напротив β лежит другой катет — горизонтальный. Касается ли a вершины с углом β?")),
            Opt(L("Přepona", "Гипотенуза"), why = L("Přepona leží naproti pravému úhlu a je nejdelší. Tady je to c.", "Гипотенуза лежит напротив прямого угла и она самая длинная. Здесь это c.")),
        ),
        explain = L("Odvěsna a se dotýká vrcholu β, je k němu přilehlá. Vůči α je protilehlá.", "Катет a касается вершины β — он прилежащий к β. Относительно α он противолежащий."),
        columns = 3,
    )
    LaunchedEffect(s1.done) {
        if (!s1.done) return@LaunchedEffect
        delay(700)
        phase = 1
        delay(1400)
        phase = 2
        delay(900)
        demoDone = true
    }
    val s2 = Step(
        L("Sleduj stejný poměr a / c z obou úhlů", "Посмотри на одно отношение a / c с обоих углов"), demoDone,
        result = {
            Note(Tone.OK, L(
                "a / c je pro α sinus a pro β kosinus: **sin α = cos β**. Protože β = 90° − α, platí sin α = cos (90° − α).",
                "a / c для α — синус, а для β — косинус: **sin α = cos β**. Так как β = 90° − α, sin α = cos (90° − α).",
            ))
        },
    ) { SmallText(L("Sleduj obrázek…", "Смотри на рисунок…")) }
    val s3 = quizStep(
        question = L("sin 20° je stejné číslo jako…", "sin 20° — то же число, что и…"),
        label = L("Vyber výraz rovný sin 20°", "Выбери выражение, равное sin 20°"),
        options = listOf(
            Opt(L("cos 70°", "cos 70°"), correct = true),
            Opt(L("cos 20°", "cos 20°"), why = L("sin a cos téhož úhlu se rovnají jen při 45°. Jak velký je druhý ostrý úhel?", "sin и cos одного угла равны только при 45°. Чему равен второй острый угол?")),
            Opt(L("sin 70°", "sin 70°"), why = L("To je sinus druhého úhlu – patří k druhé odvěsně. Která funkce úhlu 70° bere odvěsnu naproti 20°?", "Это синус второго угла — он относится к другому катету. Какая функция угла 70° берёт катет напротив 20°?")),
            Opt(L("1 − sin 70°", "1 − sin 70°"), why = L("Do jedné se sčítají druhé mocniny sin a cos téhož úhlu, ne siny dvou úhlů.", "До единицы складываются квадраты sin и cos одного угла, а не синусы двух углов.")),
        ),
        explain = L(
            "β = 90° − 20° = 70° a odvěsna naproti 20° je přilehlá k 70°. Proto sin 20° = cos 70°. Ve statice pozor: je-li úhel síly zadán od **svislice**, sin a cos si vymění místa.",
            "β = 90° − 20° = 70°, и катет напротив 20° прилежит к 70°. Поэтому sin 20° = cos 70°. В статике осторожно: если угол силы задан от **вертикали**, sin и cos меняются местами.",
        ),
        columns = 2,
    )
    val steps = listOf(s1, s2, s3)
    val cur = steps.indexOfFirst { !it.done }
    val last = cur == 2 || cur < 0
    Stage(h = 230.0, description = t.pick(L("Trojúhelník s úhly α a β", "Треугольник с углами α и β"))) { c ->
        val g = if (last) triGeom(Trig.cosD(20.0), Trig.sinD(20.0), fit = listOf(30.0, 90.0, 230.0, 110.0)) else triGeom(4.0, 3.0, fit = listOf(30.0, 40.0, 190.0, 165.0))
        val aName = if (last) "20°" else "α"
        val bName = if (last) "70°" else "β"
        rightTriangle(g, c, mapOf(TriSide.OPP to "a", TriSide.HYP to "c"), TriSide.OPP, angle = null)
        // úhel α u vrcholu a, úhel β u vrcholu b; ten, ze kterého se právě díváš, je výrazný
        val fromA = cur == 1 && phase == 0
        val fromB = cur == 0 || (cur == 1 && phase == 1)
        angleArc(g.a.x, g.a.y, 28.0, screenAngle(g.a, g.c), screenAngle(g.a, g.b), aName, c, if (fromA || cur == 1 && phase == 2 || last) c.ink else c.inkSoft)
        angleArc(g.b.x, g.b.y, 26.0, screenAngle(g.b, g.a), screenAngle(g.b, g.c), bName, c, if (fromB || cur == 1 && phase == 2 || last) c.ink else c.inkSoft)
        val lines = when {
            cur == 0 -> listOf(t.pick(L("a je protilehlá k α", "a — противолежащий к α")), t.pick(L("vůči β je a …?", "для β катет a …?")))
            cur == 1 && phase == 0 -> listOf(t.pick(L("z úhlu α:", "с угла α:")), "sin α = a / c")
            cur == 1 && phase == 1 -> listOf(t.pick(L("z úhlu β:", "с угла β:")), "cos β = a / c")
            cur == 1 -> listOf("sin α = cos β", "β = 90° − α")
            else -> listOf("sin 20° = a / c", "cos 70° = a / c")
        }
        lines.forEachIndexed { i, s -> text(392.0, 30.0 + 24 * i, s, if (i == 0) c.highlight else c.ink, 14.0, Anchor.END, bold = true) }
    }
    CardText(L(
        "Dva ostré úhly pravoúhlého trojúhelníku dávají dohromady 90°. Stejná odvěsna má vůči každému z nich jinou roli.",
        "Два острых угла прямоугольного треугольника в сумме дают 90°. Один и тот же катет играет для них разные роли.",
    ))
    Steps(*steps.toTypedArray())
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
