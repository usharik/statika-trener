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
import cz.usharik.statika.learn.LearnJump
import cz.usharik.statika.learn.LearnNav
import cz.usharik.statika.learn.Lesson
import cz.usharik.statika.learn.LocalLearn
import cz.usharik.statika.learn.LocalLearnHere
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
import cz.usharik.statika.learn.axes
import cz.usharik.statika.learn.beam
import cz.usharik.statika.learn.bolt
import cz.usharik.statika.learn.circle
import cz.usharik.statika.learn.dimLine
import cz.usharik.statika.learn.force
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.moment
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.numStep
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.quizStep
import cz.usharik.statika.learn.rect
import cz.usharik.statika.learn.rightAngleMark
import cz.usharik.statika.learn.screenAngle
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Kurz trigonometrie – lekce 8 (id 108): trigonometrie ve statice – úhel od svislice, výslednice, součet sil,
 * rameno momentu, šikmý nosník a závěrečná úloha.
 */

/** Tip jako krok: volby a „Ověřit“ v doku. Tip se jen zaznamená – vyhodnotí ho až další krok. */
private fun predictStep(label: L, options: List<L>, guess: Int?, onGuess: (Int) -> Unit): Step =
    Step(label, guess != null, result = { if (guess != null) SmallText(L("Tvůj tip: ${options[guess].cs}", "Твой прогноз: ${options[guess].ru}")) }) {
        var sel by remember { mutableStateOf<Int?>(null) }
        Chips(options, sel) { sel = it }
        ActionButton(L("Ověřit", "Проверить"), enabled = sel != null) { sel?.let(onGuess) }
    }

/** Malý křížek kladných směrů +x → a +y ↑ v rohu scény (počátek v [x], [y]). */
private fun PrimBuilder.signKey(x: Double, y: Double, c: StatikaColors, moment: Boolean = false) {
    force(x, y, x + 34, y, c.inkSoft, 1.4, head = 7.0)
    force(x, y, x, y - 34, c.inkSoft, 1.4, head = 7.0)
    text(x + 38, y + 4, "+x", c.inkSoft, 12.0, Anchor.START)
    text(x, y - 40, "+y", c.inkSoft, 12.0)
    if (moment) text(x + 38, y - 24, "↺ +", c.inkSoft, 12.0, Anchor.START)
}

/** Šikmá síla F (10 kN) na nosník: hrot na nosníku v (tx, ty), úhel [alpha] k nosníku, síla míří dolů doprava. */
private fun forceTail(tx: Double, ty: Double, alpha: Double, len: Double): Pair<Double, Double> =
    (tx - len * Trig.cosD(alpha)) to (ty - len * Trig.sinD(alpha))

private fun close(v: Double?, w: Double) = v != null && abs(v - w) <= maxOf(0.03, abs(w) * 0.01)

/** Karta 0: úhel od vodorovné × od svislice – složka u úhlu → cos, naproti → sin. */
@Composable
private fun CardScope.FromVertical() {
    val t = tr()
    var guess by remember { mutableStateOf<Int?>(null) }
    var vertical by remember { mutableStateOf(false) } // ukázka: který popis je právě vidět
    var demoDone by remember { mutableStateOf(false) }
    LaunchedEffect(guess != null) {
        if (guess == null) return@LaunchedEffect
        repeat(2) {
            delay(1100)
            vertical = true
            delay(1300)
            vertical = false
        }
        delay(600)
        vertical = true
        demoDone = true
    }
    val s1 = predictStep(
        L("Odhadni: změní se složky, když úhel změříš od svislice?", "Предскажи: изменятся ли составляющие, если угол отсчитать от вертикали?"),
        listOf(L("Změní se", "Изменятся"), L("Nezmění se", "Не изменятся")), guess,
    ) { guess = it }
    val s2 = Step(
        L("Porovnej 60° od vodorovné a 30° od svislice", "Сравни 60° от горизонтали и 30° от вертикали"), demoDone,
        result = {
            Note(if (guess == 1) Tone.OK else Tone.INFO, L(
                (if (guess == 1) "Tip sedí. " else "Šipka se ani nepohnula – mění se jen popis úhlu. ") + "Složky zůstávají stejné. Mění se jen to, která složka leží **u** úhlu.",
                (if (guess == 1) "Прогноз верен. " else "Стрелка даже не сдвинулась — меняется только описание угла. ") + "Составляющие те же. Меняется лишь то, какая составляющая лежит **у** угла.",
            ))
        },
    ) { SmallText(L("Šipka stojí, mění se jen vyznačený úhel…", "Стрелка неподвижна, меняется только отмеченный угол…")) }
    val s3 = numStep(
        question = L("Síla 10 kN míří vpravo dolů a se **svislicí** svírá 30°. Kolik je [[F_x]] i se znaménkem?", "Сила 10 кН направлена вправо вниз и составляет с **вертикалью** 30°. Чему равна [[F_x]] со знаком?"),
        label = L("Vypočítej [[F_x]] z úhlu 30° od svislice", "Вычисли [[F_x]] по углу 30° от вертикали"),
        fields = listOf(NumSpec(L("[[F_x]]", "[[F_x]]"), 10 * Trig.sinD(30.0), t.pick(L("kN", "кН")), signed = true)),
        explain = L(
            "Úhel leží u svislice, vodorovná složka je naproti: [[F_x]] = 10 · sin 30° = 10 · cos 60° = +5 kN. [[F_y]] = −10 · cos 30° ≈ −8,66 kN. Znaménka určuje směr šipky.",
            "Угол лежит у вертикали, горизонтальная составляющая — напротив: [[F_x]] = 10 · sin 30° = 10 · cos 60° = +5 кН. [[F_y]] = −10 · cos 30° ≈ −8,66 кН. Знаки определяются направлением стрелки.",
        ),
        traps = listOf(
            NumTrap(L("8,66 = 10 · cos 30° je složka **u** úhlu – ta svislá. Která složka leží naproti úhlu 30°?", "8,66 = 10 · cos 30° — составляющая **у** угла, вертикальная. Какая составляющая лежит напротив угла 30°?")) { v -> close(v[0]?.let(::abs), 8.66) },
            NumTrap(L("Velikost sedí. Míří šipka doprava (+), nebo doleva (−)?", "Модуль верен. Стрелка направлена вправо (+) или влево (−)?")) { v -> close(v[0], -5.0) },
            NumTrap(L("Síla je šikmá – vodorovně působí jen její část.", "Сила наклонная — по горизонтали действует только её часть.")) { v -> close(v[0]?.let(::abs), 10.0) },
        ),
        nudge = L("Úhel je u svislice. Složka naproti úhlu → sin.", "Угол у вертикали. Составляющая напротив угла → sin."),
    )
    val steps = listOf(s1, s2, s3)
    val cur = steps.indexOfFirst { !it.done }
    val showVertical = when (cur) {
        0 -> false
        1 -> vertical
        else -> true
    }
    Stage(h = 220.0, description = t.pick(L("Šikmá síla 10 kN na nosník s vyznačeným úhlem", "Наклонная сила 10 кН на балку с отмеченным углом"))) { c ->
        val tx = 230.0
        val ty = 170.0
        val len = 140.0
        beam(30.0, 370.0, ty + 5, c)
        signKey(320.0, 120.0, c)
        val (sx, sy) = forceTail(tx, ty, 60.0, len)
        // složky čárkovaně (bez hodnot – ty jsou úkolem)
        line(sx, sy, tx, sy, c.derived, 1.0, dash = true)
        line(sx, sy, sx, ty, c.derived, 1.0, dash = true)
        if (cur < 0) {
            // složky ze společného působiště (hrot na nosníku)
            force(tx - (tx - sx), ty - 2, tx, ty - 2, c.derived, 2.2)
            force(tx, ty - (ty - sy), tx, ty, c.derived, 2.2)
            text(tx - (tx - sx) / 2, ty - 10, t.pick(L(" = +5 kN", " = +5 кН")), c.derived, 12.0, sym = "F_x")
            text(tx + 8, ty - (ty - sy) / 2, t.pick(L(" = −8,66 kN", " = −8,66 кН")), c.derived, 12.0, Anchor.START, sym = "F_y")
        }
        force(sx, sy, tx, ty, c.load, 3.0)
        text(sx - 8, sy + 4, t.pick(L(" = 10 kN", " = 10 кН")), c.load, 14.0, Anchor.END, sym = "F")
        if (!showVertical) {
            angleArc(tx, ty, 38.0, 180.0, 180.0 + 60.0, null, c, c.ink)
            text(tx - 46, ty - 14, t.pick(L("60° od vodorovné", "60° от горизонтали")), c.ink, 14.0, Anchor.END, bold = true)
        } else {
            line(tx, ty, tx, ty - 130, c.inkSoft, 1.0, dash = true)
            angleArc(tx, ty, 50.0, -90.0, 180.0 + 60.0, null, c, c.ink)
            text(tx - 30, ty - 100, t.pick(L("30° od svislice", "30° от вертикали")), c.ink, 14.0, Anchor.END, bold = true)
        }
    }
    CardText(L(
        "Stejnou sílu lze popsat úhlem od vodorovné nebo od svislice. Spolehlivé pravidlo: **složka u úhlu → cos, složka naproti → sin**.",
        "Одну силу можно задать углом от горизонтали или от вертикали. Надёжное правило: **составляющая у угла → cos, напротив → sin**.",
    ))
    Steps(*steps.toTypedArray())
}

/** Složky (fx, fy) v kN a jejich výslednice; [ghost] = dřívější výslednice pro srovnání. */
private fun PrimBuilder.resultantFig(fx: Int, fy: Int, rLabel: String?, angleLabel: String?, ghost: Pair<Int, Int>?, c: StatikaColors) {
    val cx = 200.0
    val cy = 140.0
    val u = 15.0 // 1 kN
    axes(cx, cy, 175.0, c, labels = false)
    text(cx + 171, cy + 16, "+x", c.inkSoft, 13.0, Anchor.END)
    text(cx + 10, cy - 130, "+y", c.inkSoft, 13.0, Anchor.START)
    ghost?.let { (gx, gy) -> force(cx, cy, cx + gx * u, cy - gy * u, c.load, 2.0, alpha = 0.3f, dash = true) }
    val tx = cx + fx * u
    val ty = cy - fy * u
    if (fx != 0) force(cx, cy, tx, cy, c.derived, 2.4)
    if (fy != 0) force(cx, cy, cx, ty, c.derived, 2.4)
    line(tx, cy, tx, ty, c.derived, 1.0, dash = true)
    line(cx, ty, tx, ty, c.derived, 1.0, dash = true)
    fun sg(v: Int) = if (v > 0) "+$v" else if (v < 0) "−${-v}" else "0"
    if (fx != 0) text((cx + tx) / 2, cy + (if (fy >= 0) 18.0 else -8.0), " = ${sg(fx)}", c.derived, 12.0, sym = "F_x")
    if (fy != 0) text(cx + (if (fx >= 0) -8.0 else 8.0), (cy + ty) / 2 + 5, " = ${sg(fy)}", c.derived, 12.0, if (fx >= 0) Anchor.END else Anchor.START, sym = "F_y")
    if (fx != 0 || fy != 0) {
        force(cx, cy, tx, ty, c.load, 3.0)
        if (rLabel != null) text(tx + (if (fx >= 0) 8.0 else -8.0), ty + (if (fy >= 0) -6.0 else 16.0), rLabel, c.load, 13.0, if (fx >= 0) Anchor.START else Anchor.END, sym = "R")
        if (angleLabel != null && fy != 0) angleArc(cx, cy, 30.0, 0.0, screenAngle(Pt(cx, cy), Pt(tx, ty)), angleLabel, c)
    }
}

/** Karta 1: ze složek celá síla – velikost Pythagorem, směr arctg a znaménky. */
@Composable
private fun CardScope.Resultant() {
    val t = tr()
    val kn = t.pick(L("kN", "кН"))
    var guess by remember { mutableStateOf<Int?>(null) }
    var fx by remember { mutableIntStateOf(4) }
    var tested by remember { mutableStateOf(false) }
    var ex by remember { mutableIntStateOf(6) }
    var ey by remember { mutableIntStateOf(8) }
    fun sq(v: Int) = if (v < 0) "(−${-v})²" else "$v²"
    val s1 = predictStep(
        L("Odhadni, co udělá změna [[F_x]] z +4 na −4 kN ([[F_y]] = +3 kN)", "Предскажи, что даст замена [[F_x]] с +4 на −4 кН ([[F_y]] = +3 кН)"),
        listOf(L("Jen směr", "Только направление"), L("Jen velikost", "Только модуль"), L("Obojí", "И то и другое")), guess,
    ) { guess = it }
    val s2 = Step(
        L("Změň znaménko [[F_x]] a porovnej výslednice", "Измени знак [[F_x]] и сравни равнодействующие"), tested,
        result = {
            Note(if (guess == 0) Tone.OK else Tone.INFO, L(
                (if (guess == 0) "Tip sedí. " else "") + "R = √(4² + 3²) = √((−4)² + 3²) = 5 kN – velikost se nezměnila, výslednice se jen zrcadlově otočila doleva.",
                (if (guess == 0) "Прогноз верен. " else "") + "R = √(4² + 3²) = √((−4)² + 3²) = 5 кН — модуль не изменился, равнодействующая лишь зеркально повернулась влево.",
            ))
        },
    ) {
        Chips(listOf(L("[[F_x]] = +4 kN", "[[F_x]] = +4 кН"), L("[[F_x]] = −4 kN", "[[F_x]] = −4 кН")), if (fx > 0) 0 else 1) { fx = if (it == 0) 4 else -4 }
        Readout("R = √(${sq(fx)} + 3²) = 5 $kn")
        ActionButton(L("Ověřit", "Проверить"), enabled = fx == -4) { tested = true }
    }
    val s3 = numStep(
        question = L("[[F_x]] = 6 kN, [[F_y]] = 8 kN. Jak velká je výslednice R?", "[[F_x]] = 6 кН, [[F_y]] = 8 кН. Каков модуль равнодействующей R?"),
        label = L("Vypočítej R pro [[F_x]] = 6 kN a [[F_y]] = 8 kN", "Вычисли R при [[F_x]] = 6 кН и [[F_y]] = 8 кН"),
        fields = listOf(NumSpec(L("R", "R"), 10.0, kn)),
        explain = L("R = √(6² + 8²) = √100 = 10 kN.", "R = √(6² + 8²) = √100 = 10 кН."),
        traps = listOf(
            NumTrap.near(L("Složky se nesčítají přímo – jsou na sebe kolmé. Která věta platí pro pravoúhlý trojúhelník?", "Составляющие нельзя просто сложить — они перпендикулярны. Какая теорема работает в прямоугольном треугольнике?"), 14.0),
            NumTrap.near(L("100 je teprve R². Ještě odmocni.", "100 — это пока R². Осталось извлечь корень."), 100.0),
        ),
        nudge = L("R² = [[F_x]]² + [[F_y]]².", "R² = [[F_x]]² + [[F_y]]²."),
    )
    val s4 = numStep(
        question = L("Jaký úhel svírá výslednice s kladnou osou x? (na jedno desetinné místo)", "Какой угол равнодействующая составляет с положительной осью x? (до десятых)"),
        label = L("Vypočítej úhel výslednice od kladné osy x", "Вычисли угол равнодействующей от положительной оси x"),
        fields = listOf(NumSpec(L("α", "α"), Trig.atanD(8.0 / 6.0), "°", tol = Tol.ANGLE)),
        explain = L(
            "tg α = [[F_y]] / [[F_x]] = 8/6, α = arctg (8/6) ≈ 53,1°. Obě složky jsou kladné, takže výslednice míří doprava nahoru a tohle je i její směr od +x.",
            "tg α = [[F_y]] / [[F_x]] = 8/6, α = arctg (8/6) ≈ 53,1°. Обе составляющие положительны, поэтому равнодействующая направлена вправо вверх, и это же её направление от +x.",
        ),
        traps = listOf(
            NumTrap.near(L("To je úhel od svislice – vyjde z [[F_x]] / [[F_y]]. Naproti úhlu u osy x leží [[F_y]].", "Это угол от вертикали — так выходит из [[F_x]] / [[F_y]]. Напротив угла у оси x лежит [[F_y]]."), Trig.atanD(6.0 / 8.0)),
            NumTrap.near(L("Kalkulačka je v režimu RAD – přepni na DEG.", "Калькулятор в режиме RAD — переключи на DEG."), kotlin.math.atan(8.0 / 6.0)),
            NumTrap.near(L("8/6 ≈ 1,33 je teprve tg α. Úhel dá tan⁻¹.", "8/6 ≈ 1,33 — это пока tg α. Угол даст tan⁻¹."), 8.0 / 6.0),
        ),
        nudge = L("tg α = protilehlá / přilehlá = [[F_y]] / [[F_x]].", "tg α = противолежащий / прилежащий = [[F_y]] / [[F_x]]."),
    )
    val steps = listOf(s1, s2, s3, s4)
    val cur = steps.indexOfFirst { !it.done }
    Stage(h = 280.0, description = t.pick(L("Složky a jejich výslednice v osách x, y", "Составляющие и их равнодействующая в осях x, y"))) { c ->
        when (cur) {
            0 -> resultantFig(4, 3, " = 5 $kn", null, null, c)
            1 -> resultantFig(fx, 3, " = 5 $kn", null, if (fx < 0) 4 to 3 else null, c)
            2 -> resultantFig(6, 8, " = ?", null, null, c)
            3 -> resultantFig(6, 8, " = 10 $kn", "α = ?", null, c)
            else -> {
                val r = sqrt((ex * ex + ey * ey).toDouble())
                resultantFig(ex, ey, if (r > 0) " ≈ ${num(r, 2)} $kn" else null, null, null, c)
            }
        }
    }
    CardText(L(
        "Kolmé složky určují velikost i směr výslednice. Velikost dá Pythagorova věta, směr arctg – a znaménka složek.",
        "Перпендикулярные составляющие определяют модуль и направление равнодействующей. Модуль даёт теорема Пифагора, направление — arctg и знаки составляющих.",
    ))
    Steps(*steps.toTypedArray(), after = {
        ValueSlider("[[F_x]] = $ex $kn", ex.toFloat(), -8f..8f, steps = 15) { ex = it.roundToInt() }
        ValueSlider("[[F_y]] = $ey $kn", ey.toFloat(), -8f..8f, steps = 15) { ey = it.roundToInt() }
        val r = sqrt((ex * ex + ey * ey).toDouble())
        val dir = when {
            ex == 0 && ey == 0 -> L("R = 0 – směr není určen", "R = 0 — направление не определено")
            ey == 0 -> if (ex > 0) L("vodorovně doprava", "горизонтально вправо") else L("vodorovně doleva", "горизонтально влево")
            ex == 0 -> if (ey > 0) L("svisle nahoru", "вертикально вверх") else L("svisle dolů", "вертикально вниз")
            else -> {
                var phi = atan2(ey.toDouble(), ex.toDouble()) * 180 / kotlin.math.PI
                if (phi < 0) phi += 360
                val lr = if (ex > 0) L("doprava", "вправо") else L("doleva", "влево")
                val ud = if (ey > 0) L("nahoru", "вверх") else L("dolů", "вниз")
                L("${lr.cs} ${ud.cs}, ${num(phi, 1)}° od +x", "${lr.ru} ${ud.ru}, ${num(phi, 1)}° от +x")
            }
        }
        Readout("R = √(${sq(ex)} + ${sq(ey)}) ${if (abs(r - kotlin.math.round(r)) < 1e-9) "=" else "≈"} ${num(r, 2)} $kn · ${t.pick(dir)}")
    })
}

/** Karta 2: součet dvou sil po složkách (obě působí v jednom bodě). Šipky v jednom měřítku 12 px = 1 kN. */
@Composable
private fun CardScope.SumForces() {
    val t = tr()
    val kn = t.pick(L("kN", "кН"))
    val f1x = 10 * Trig.cosD(30.0)
    val s1 = numStep(
        question = L("Kolik je vodorovná složka [[F_1x]] síly [[F_1]] = 10 kN (30° pod vodorovnou, vpravo dolů)? cos 30° ≈ 0,866", "Чему равна горизонтальная составляющая [[F_1x]] силы [[F_1]] = 10 кН (30° ниже горизонтали, вправо вниз)? cos 30° ≈ 0,866"),
        label = L("Vypočítej vodorovnou složku síly [[F_1]]", "Вычисли горизонтальную составляющую силы [[F_1]]"),
        fields = listOf(NumSpec(L("[[F_1x]]", "[[F_1x]]"), f1x, kn, signed = true)),
        explain = L("Úhel je u vodorovné, takže [[F_1x]] = 10 · cos 30° ≈ +8,66 kN (doprava).", "Угол у горизонтали, поэтому [[F_1x]] = 10 · cos 30° ≈ +8,66 кН (вправо)."),
        traps = listOf(
            NumTrap(L("5 kN = 10 · sin 30° je svislá složka – leží naproti úhlu.", "5 кН = 10 · sin 30° — вертикальная составляющая, она напротив угла.")) { v -> close(v[0]?.let(::abs), 5.0) },
            NumTrap(L("Velikost sedí. Míří [[F_1]] doprava (+), nebo doleva (−)?", "Модуль верен. [[F_1]] направлена вправо (+) или влево (−)?")) { v -> close(v[0], -f1x) },
        ),
        nudge = L("Složka u úhlu → cos.", "Составляющая у угла → cos."),
    )
    val s2 = numStep(
        question = L("Sečti všechny vodorovné složky: Σ[[F_x]] = [[F_1x]] + [[F_2x]]. [[F_2]] = 3 kN míří doleva.", "Сложи все горизонтальные составляющие: Σ[[F_x]] = [[F_1x]] + [[F_2x]]. [[F_2]] = 3 кН направлена влево."),
        label = L("Sečti všechny vodorovné složky Σ[[F_x]]", "Сложи все горизонтальные составляющие Σ[[F_x]]"),
        fields = listOf(NumSpec(L("Σ[[F_x]]", "Σ[[F_x]]"), f1x - 3, kn, signed = true)),
        explain = L("Σ[[F_x]] = +8,66 + (−3) ≈ +5,66 kN.", "Σ[[F_x]] = +8,66 + (−3) ≈ +5,66 кН."),
        traps = listOf(
            NumTrap(L("[[F_2]] míří doleva. Jaké znaménko má v součtu?", "[[F_2]] направлена влево. С каким знаком она входит в сумму?")) { v -> close(v[0]?.let(::abs), 11.66) },
            NumTrap(L("To je 10 − 3. Do vodorovného součtu patří jen vodorovná složka šikmé síly, ne celá síla.", "Это 10 − 3. В горизонтальную сумму входит только горизонтальная составляющая наклонной силы, а не вся сила.")) { v -> close(v[0]?.let(::abs), 7.0) },
            NumTrap(L("To je 5 − 3, ale 5 kN je svislá složka [[F_1]]. Vezmi [[F_1x]] z minulého kroku.", "Это 5 − 3, но 5 кН — вертикальная составляющая [[F_1]]. Возьми [[F_1x]] из прошлого шага.")) { v -> close(v[0]?.let(::abs), 2.0) },
            NumTrap(L("Velikost sedí. Kam součet míří – doprava (+), nebo doleva (−)?", "Модуль верен. Куда направлена сумма — вправо (+) или влево (−)?")) { v -> close(v[0], -(f1x - 3)) },
        ),
        nudge = L("Doleva je záporně: Σ[[F_x]] = 8,66 − 3.", "Влево — отрицательно: Σ[[F_x]] = 8,66 − 3."),
    )
    val s3 = numStep(
        question = L("Sečti všechny svislé složky Σ[[F_y]]. sin 30° = 0,5; [[F_2]] je vodorovná.", "Сложи все вертикальные составляющие Σ[[F_y]]. sin 30° = 0,5; [[F_2]] горизонтальна."),
        label = L("Sečti všechny svislé složky Σ[[F_y]]", "Сложи все вертикальные составляющие Σ[[F_y]]"),
        fields = listOf(NumSpec(L("Σ[[F_y]]", "Σ[[F_y]]"), -5.0, kn, signed = true)),
        explain = L(
            "Σ[[F_y]] = −10 · sin 30° + 0 = −5 kN. Výslednice R = (+5,66; −5) kN ≈ 7,55 kN míří vpravo dolů – stejně jako když šipky složíš za sebe.",
            "Σ[[F_y]] = −10 · sin 30° + 0 = −5 кН. Равнодействующая R = (+5,66; −5) кН ≈ 7,55 кН направлена вправо вниз — так же, как если сложить стрелки одну за другой.",
        ),
        traps = listOf(
            NumTrap(L("Velikost sedí. [[F_1]] míří dolů – jaké znaménko?", "Модуль верен. [[F_1]] направлена вниз — какой знак?")) { v -> close(v[0], 5.0) },
            NumTrap(L("[[F_2]] je vodorovná – svislou složku nemá. Svisle působí jen [[F_1]].", "[[F_2]] горизонтальна — вертикальной составляющей у неё нет. По вертикали действует только [[F_1]].")) { v -> close(v[0]?.let(::abs), 8.0) || close(v[0]?.let(::abs), 2.0) },
        ),
        nudge = L("Svislá složka [[F_1]] leží naproti úhlu 30° → sin.", "Вертикальная составляющая [[F_1]] лежит напротив угла 30° → sin."),
    )
    val steps = listOf(s1, s2, s3)
    val cur = steps.indexOfFirst { !it.done }
    val doneN = if (cur < 0) 3 else cur
    Stage(h = 200.0, description = t.pick(L("Bod se dvěma silami: šikmá F1 a vodorovná F2", "Точка с двумя силами: наклонная F1 и горизонтальная F2"))) { c ->
        val u = 12.0 // 1 kN
        val cx = 160.0
        val cy = 70.0
        signKey(40.0, 185.0, c)
        line(30.0, cy, 370.0, cy, c.inkSoft, 1.0, dash = true)
        val tx = cx + 10 * u * Trig.cosD(30.0)
        val ty = cy + 10 * u * Trig.sinD(30.0)
        if (doneN >= 1) {
            line(tx, cy, tx, ty, c.derived, 1.0, dash = true)
            force(cx, cy, tx, cy, c.derived, 2.2)
            text((cx + tx) / 2 + 10, cy - 8, " = +8,66", c.derived, 12.0, sym = "F_1x")
        }
        if (doneN >= 2) {
            force(cx, cy - 30, cx + (f1x - 3) * u, cy - 30, c.accent, 2.6)
            text(cx + 4, cy - 38, " = +5,66", c.accent, 12.0, Anchor.START, sym = "ΣF_x")
        }
        if (doneN >= 3) {
            // šipky za sebou: F1, pak F2 z jejího hrotu → výslednice
            force(tx, ty, tx - 3 * u, ty, c.load, 2.0, alpha = 0.5f)
            force(cx, cy, tx - 3 * u, ty, c.accent, 2.6, dash = true)
            text(tx - 3 * u - 8, ty + 18, " ≈ 7,55 $kn", c.accent, 13.0, Anchor.END, sym = "R")
            force(cx, cy, cx, cy + 5 * u, c.accent, 2.6)
            text(cx - 8, cy + 5 * u, " = −5", c.accent, 12.0, Anchor.END, sym = "ΣF_y")
        }
        force(cx, cy, tx, ty, c.load, 3.0)
        text(tx + 8, ty + 4, " = 10 $kn", c.load, 14.0, Anchor.START, sym = "F_1")
        angleArc(cx, cy, 40.0, 0.0, 30.0, "30°", c)
        force(cx, cy, cx - 3 * u, cy, c.load, 3.0)
        text(cx - 40, cy + 20, " = 3 $kn", c.load, 14.0, Anchor.END, sym = "F_2")
        circle(cx, cy, 6.0, c.ink)
    }
    CardText(L(
        "Síly se sčítají jako vektory. Po složkách to jde nejsnáz: zvlášť součet ve směru x a zvlášť ve směru y – se znaménky. Obě síly působí v jednom bodě.",
        "Силы складываются как векторы. Проще всего — по составляющим: отдельно сумма по x и отдельно по y, со знаками. Обе силы приложены в одной точке.",
    ))
    Steps(*steps.toTypedArray())
}

/** Karta 3: rameno momentu r⊥ = r · sin α. */
@Composable
private fun CardScope.MomentArm() {
    val t = tr()
    var guess by remember { mutableStateOf<Int?>(null) }
    var alpha by remember { mutableIntStateOf(60) }
    var maxDone by remember { mutableStateOf(false) }
    var miss2 by remember { mutableStateOf(false) }
    var zeroDone by remember { mutableStateOf(false) }
    var miss4 by remember { mutableStateOf(false) }
    val r = 2.0
    fun rp(a: Int) = r * Trig.sinD(a.toDouble())
    val mm = t.pick(L("m", "м"))
    val slider: @Composable () -> Unit = {
        ValueSlider(t.pick(L("Úhel α mezi silou a pákou = $alpha°", "Угол α между силой и рычагом = $alpha°")), alpha.toFloat(), 0f..90f, steps = 17) {
            alpha = (it / 5).roundToInt() * 5
            miss2 = false
            miss4 = false
        }
        Readout("[[r_⊥]] = 2 · sin $alpha° ${if (alpha % 90 == 0 || alpha == 30) "=" else "≈"} ${num(rp(alpha), 2)} $mm")
    }
    val s1 = predictStep(
        L("Odhadni úhel α, při kterém bude moment největší", "Предскажи угол α, при котором момент будет наибольшим"),
        listOf(L("0°", "0°"), L("30°", "30°"), L("90°", "90°")), guess,
    ) { guess = it }
    val s2 = Step(
        L("Natoč sílu tak, aby měla co největší rameno", "Поверни силу так, чтобы плечо было наибольшим"), maxDone,
        result = {
            Note(if (guess == 2) Tone.OK else Tone.INFO, L(
                (if (guess == 2) "Tip sedí. " else "") + "Při 90° je rameno celá délka páky, 2 m, a moment 10 · 2 = 20 kN·m – víc to nejde.",
                (if (guess == 2) "Прогноз верен. " else "") + "При 90° плечо равно всей длине рычага, 2 м, а момент 10 · 2 = 20 кН·м — больше не бывает.",
            ))
        },
    ) {
        slider()
        ActionButton(L("Zkontrolovat", "Проверить")) { if (alpha == 90) maxDone = true else miss2 = true }
        if (miss2) Note(Tone.NUDGE, L("Rameno ${num(rp(alpha), 2)} m – jde to víc?", "Плечо ${num(rp(alpha), 2)} м — можно больше?"))
    }
    val s3 = numStep(
        question = L("Síla 10 kN svírá s pákou 2 m úhel 30°. Jak velký je moment |M| k bodu O?", "Сила 10 кН составляет с рычагом 2 м угол 30°. Каков модуль момента |M| относительно точки O?"),
        label = L("Vypočítej velikost momentu při 30°", "Вычисли модуль момента при 30°"),
        fields = listOf(NumSpec(L("|M|", "|M|"), 10.0, t.pick(L("kN·m", "кН·м")))),
        explain = L("[[r_⊥]] = 2 · sin 30° = 1 m, |M| = F · [[r_⊥]] = 10 · 1 = 10 kN·m.", "[[r_⊥]] = 2 · sin 30° = 1 м, |M| = F · [[r_⊥]] = 10 · 1 = 10 кН·м."),
        traps = listOf(
            NumTrap.near(L("10 · 2 platí jen pro sílu kolmou k páce. Jaké je tady rameno [[r_⊥]]?", "10 · 2 верно только для силы, перпендикулярной рычагу. Каково здесь плечо [[r_⊥]]?"), 20.0),
            NumTrap.near(L("Rameno je odvěsna **naproti** α, tedy r · sin α, ne r · cos α.", "Плечо — катет **напротив** α, то есть r · sin α, а не r · cos α."), 20 * Trig.cosD(30.0)),
        ),
        nudge = L("|M| = F · [[r_⊥]], [[r_⊥]] = r · sin α.", "|M| = F · [[r_⊥]], [[r_⊥]] = r · sin α."),
    )
    val s4 = Step(
        L("Najdi směr síly, při kterém je moment nulový", "Найди направление силы, при котором момент равен нулю"), zeroDone,
        result = {
            Note(Tone.OK, L(
                "Při 0° nositelka prochází bodem O – rameno je 0, takže moment je 0, i když síla 10 kN pořád působí. Obecně |M| = F · r · sin α.",
                "При 0° линия действия проходит через O — плечо 0, значит и момент 0, хотя сила 10 кН по-прежнему действует. В общем случае |M| = F · r · sin α.",
            ))
        },
    ) {
        slider()
        ActionButton(L("Zkontrolovat", "Проверить")) { if (alpha == 0) zeroDone = true else miss4 = true }
        if (miss4) Note(Tone.NUDGE, L("Rameno je ještě ${num(rp(alpha), 2)} m. Kdy nositelka projde bodem O?", "Плечо ещё ${num(rp(alpha), 2)} м. Когда линия действия пройдёт через O?"))
    }
    val steps = listOf(s1, s2, s3, s4)
    val cur = steps.indexOfFirst { !it.done }
    val a = if (cur == 0) 60 else if (cur == 2) 30 else alpha
    val showArm = cur != 0
    val armLabel = if (cur == 2) " = ?" else " = ${num(rp(a), 2)} $mm"
    Stage(h = 250.0, description = t.pick(L("Páka 2 m, síla 10 kN na konci pod úhlem α a její rameno", "Рычаг 2 м, сила 10 кН на конце под углом α и её плечо"))) { c ->
        val ox = 70.0
        val oy = 140.0
        val unit = 115.0
        val ex = ox + r * unit
        // síla působí na konci páky (hrot v E); míří ve směru (−cos α, −sin α) na obrazovce (α = 0 → do osy, 90° → nahoru)
        val ux = -Trig.cosD(a.toDouble())
        val uy = -Trig.sinD(a.toDouble())
        val len = 80.0
        val tailX = ex - ux * len
        val tailY = oy - uy * len
        line(ex - ux * 260, oy - uy * 260, ex + ux * 260, oy + uy * 260, c.inkSoft, 1.0, dash = true)
        rect(ox, oy - 6, r * unit, 12.0, c.beam, c.ink, 1.4)
        bolt(ox, oy, 16.0, 0.0, c)
        text(ox - 4, oy + 34, "O", c.ink, 14.0, bold = true)
        force(tailX, tailY, ex, oy, c.load, 3.0)
        if (a < 20) text(tailX - 4, tailY - 14, t.pick(L(" = 10 kN", " = 10 кН")), c.load, 13.0, Anchor.END, sym = "F")
        else text(tailX - 10, tailY + 4, t.pick(L(" = 10 kN", " = 10 кН")), c.load, 13.0, Anchor.END, sym = "F")
        if (a in 1..89) angleArc(ex, oy, 30.0, 180.0, 180.0 + a.toDouble(), "α", c)
        if (a == 90) rightAngleMark(Pt(ex, oy), Pt(ox, oy), Pt(ex, oy - 30), c)
        dimLine(ox, oy + 50, ex, oy + 50, t.pick(L("r = 2 m", "r = 2 м")), c)
        if (showArm) {
            if (rp(a) > 0.02) {
                // pata kolmice z O na nositelku
                val tt = (ox - ex) * ux
                val px = ex + ux * tt
                val py = oy + uy * tt
                line(ox, oy, px, py, c.accent, 2.4, dash = true)
                if (a < 90) rightAngleMark(Pt(px, py), Pt(ox, oy), Pt(ex, oy), c, 8.0)
                text((ox + px) / 2 - 10, (oy + py) / 2, armLabel, c.accent, 13.0, Anchor.END, sym = "r_⊥")
            } else {
                text(ex - 10, oy - 22, t.pick(L("rameno 0", "плечо 0")), c.accent, 13.0, Anchor.END)
            }
        }
        text(392.0, 242.0, t.pick(L("jen geometrie síly – páku drží jiné vazby", "только геометрия силы — рычаг удерживают другие связи")), c.inkSoft, 11.0, Anchor.END)
    }
    CardText(L(
        "**Rameno** síly [[r_⊥]] je kolmá vzdálenost od bodu O k její nositelce. Nemusí být stejné jako délka páky. **Moment** |M| = F · [[r_⊥]].",
        "**Плечо** силы [[r_⊥]] — перпендикулярное расстояние от точки O до линии действия силы. Оно не обязательно равно длине рычага. **Момент** |M| = F · [[r_⊥]].",
    ))
    Steps(*steps.toTypedArray())
    LearnJump(4, 1, L("Základy statiky: Moment síly", "Основы статики: момент силы"))
}

/** Karta 4: šikmý nosník – vodorovný průmět L · cos α je rameno svislé síly. */
@Composable
private fun CardScope.InclinedBeam() {
    val t = tr()
    val mm = t.pick(L("m", "м"))
    var guess by remember { mutableStateOf<Int?>(null) }
    var alpha by remember { mutableIntStateOf(30) }
    var tested by remember { mutableStateOf(false) }
    val lm = 5.0
    val s1 = predictStep(
        L("Odhadni, jak se změní vodorovný průmět při sklonu 30° → 45°", "Предскажи, как изменится горизонтальная проекция при наклоне 30° → 45°"),
        listOf(L("Zvětší se", "Увеличится"), L("Zmenší se", "Уменьшится"), L("Nezmění se", "Не изменится")), guess,
    ) { guess = it }
    val s2 = Step(
        L("Zvětši sklon na 45° a porovnej vodorovné průměty", "Увеличь наклон до 45° и сравни горизонтальные проекции"), tested,
        result = {
            Note(if (guess == 1) Tone.OK else Tone.INFO, L(
                (if (guess == 1) "Tip sedí. " else "") + "Průmět se zkrátil z 4,33 m na 3,54 m: d = L · cos α a kosinus s rostoucím úhlem klesá. Délka nosníku zůstala 5 m.",
                (if (guess == 1) "Прогноз верен. " else "") + "Проекция сократилась с 4,33 м до 3,54 м: d = L · cos α, а косинус с ростом угла убывает. Длина балки осталась 5 м.",
            ))
        },
    ) {
        ValueSlider(t.pick(L("Sklon α = $alpha°", "Угол наклона α = $alpha°")), alpha.toFloat(), 30f..45f, steps = 2) { alpha = (it / 5).roundToInt() * 5 }
        Readout(t.pick(L("d = 5 · cos $alpha° ≈ ${num(lm * Trig.cosD(alpha.toDouble()), 2)} m", "d = 5 · cos $alpha° ≈ ${num(lm * Trig.cosD(alpha.toDouble()), 2)} м")))
        ActionButton(L("Ověřit", "Проверить"), enabled = alpha == 45) { tested = true }
    }
    val s3 = numStep(
        question = L("Krokev dlouhá 5 m má sklon 35° (cos 35° ≈ 0,8192). Jak dlouhý je její vodorovný průmět d?", "Стропило длиной 5 м наклонено под углом 35° (cos 35° ≈ 0,8192). Какова его горизонтальная проекция d?"),
        label = L("Vypočítej vodorovný průmět při sklonu 35°", "Вычисли горизонтальную проекцию при наклоне 35°"),
        fields = listOf(NumSpec(L("d", "d"), lm * Trig.cosD(35.0), mm)),
        explain = L("Průmět leží u úhlu, proto kosinus: d = 5 · cos 35° ≈ 4,10 m.", "Проекция лежит у угла, поэтому косинус: d = 5 · cos 35° ≈ 4,10 м."),
        traps = listOf(
            NumTrap.near(L("2,87 = 5 · sin 35° je výška – odvěsna naproti úhlu. Průmět leží u úhlu.", "2,87 = 5 · sin 35° — это высота, катет напротив угла. Проекция лежит у угла."), lm * Trig.sinD(35.0)),
            NumTrap.near(L("Průmět nemůže být delší než krokev sama. Násobit, nebo dělit?", "Проекция не может быть длиннее самого стропила. Умножать или делить?"), lm / Trig.cosD(35.0)),
            NumTrap.near(L("5 m by to bylo jen u vodorovného nosníku.", "5 м было бы только у горизонтальной балки."), 5.0),
        ),
        nudge = L("Vodorovný průmět je odvěsna u úhlu: d = L · cos α.", "Горизонтальная проекция — катет у угла: d = L · cos α."),
    )
    val s4 = quizStep(
        question = L("Na horním konci krokve visí svislé břemeno. Jaké má rameno k dolnímu konci A?", "На верхнем конце стропила висит вертикальный груз. Каково его плечо относительно нижнего конца A?"),
        label = L("Vyber rameno svislé síly v horním konci", "Выбери плечо вертикальной силы на верхнем конце"),
        options = listOf(
            Opt(L("4,10 m", "4,10 м"), correct = true),
            Opt(L("2,87 m", "2,87 м"), why = L("2,87 m je výška. Rameno je kolmá vzdálenost k **svislé** nositelce – ta se měří vodorovně.", "2,87 м — это высота. Плечо — перпендикулярное расстояние до **вертикальной** линии действия, оно измеряется по горизонтали.")),
            Opt(L("5 m", "5 м"), why = L("5 m je délka po krokvi. Kolmá vzdálenost od A ke svislé nositelce je kratší.", "5 м — длина вдоль стропила. Перпендикулярное расстояние от A до вертикальной линии действия короче.")),
        ),
        explain = L(
            "Nositelka svislé síly je svislá, kolmá vzdálenost k ní je vodorovná: rameno = d = L · cos α ≈ 4,10 m. (h = L · sin α ≈ 2,87 m by bylo rameno vodorovné síly.)",
            "Линия действия вертикальной силы вертикальна, расстояние до неё по перпендикуляру горизонтально: плечо = d = L · cos α ≈ 4,10 м. (h = L · sin α ≈ 2,87 м было бы плечом горизонтальной силы.)",
        ),
        columns = 3,
    )
    val steps = listOf(s1, s2, s3, s4)
    val cur = steps.indexOfFirst { !it.done }
    val a = when (cur) {
        0 -> 30
        1 -> alpha
        else -> 35
    }
    Stage(h = 250.0, description = t.pick(L("Šikmý nosník délky 5 m s vodorovným průmětem a výškou", "Наклонная балка длиной 5 м с горизонтальной проекцией и высотой"))) { c ->
        val x0 = 60.0
        val y0 = 196.0
        val unit = 40.0
        val plan = lm * Trig.cosD(a.toDouble())
        val rise = lm * Trig.sinD(a.toDouble())
        val ex = x0 + plan * unit
        val ey = y0 - rise * unit
        line(20.0, y0, 380.0, y0, c.ink, 1.2)
        if (cur == 1 && alpha != 30) {
            // pro srovnání nosník při 30°
            val gx = x0 + lm * Trig.cosD(30.0) * unit
            val gy = y0 - lm * Trig.sinD(30.0) * unit
            line(x0, y0, gx, gy, c.inkSoft, 3.0, alpha = 0.35f)
            line(gx, gy, gx, y0, c.inkSoft, 1.0, dash = true)
        }
        line(x0, y0, ex, ey, c.ink, 9.0)
        line(x0, y0, ex, ey, c.beam, 6.0)
        text((x0 + ex) / 2 - 12, (y0 + ey) / 2 - 12, t.pick(L("L = 5 m", "L = 5 м")), c.ink, 13.0, Anchor.END)
        angleArc(x0, y0, 34.0, 0.0, -a.toDouble(), "$a°", c)
        line(ex, ey, ex, y0, c.derived, 1.0, dash = true)
        val dKnown = cur != 2
        val dTxt = if (dKnown) "d ≈ ${num(plan, 2)} $mm" else "d = ?"
        dimLine(x0, y0 + 20, ex, y0 + 20, dTxt, c, color = c.derived)
        if (cur >= 3 || cur < 0) {
            text(ex + 10, (y0 + ey) / 2 + 5, "h ≈ ${num(rise, 2)} $mm", c.derived, 13.0, Anchor.START)
            // svislé břemeno na horním konci a bod A
            force(ex, ey - 60, ex, ey - 4, c.load, 3.0)
            text(ex + 8, ey - 40, null, c.load, 14.0, Anchor.START, sym = "F")
            circle(x0, y0, 5.0, c.ink)
            text(x0 - 8, y0 - 6, "A", c.ink, 14.0, Anchor.END, bold = true)
        } else {
            dimLine(ex + 18, y0, ex + 18, ey, "", c, color = c.derived)
            text(ex + 26, (y0 + ey) / 2 + 5, t.pick(L("výška ${num(rise, 2)} m", "высота ${num(rise, 2)} м")), c.derived, 13.0, Anchor.START)
        }
    }
    CardText(L(
        "Šikmý nosník (krokev, schodnice) je přepona. Jeho **vodorovný průmět** d = L · cos α je kratší než jeho délka – a právě ten určuje rameno svislé síly.",
        "Наклонная балка (стропило, косоур) — это гипотенуза. Её **горизонтальная проекция** d = L · cos α короче длины — и именно она задаёт плечо вертикальной силы.",
    ))
    Steps(*steps.toTypedArray())
}

/** Karta 5: závěrečná úloha – složky se znaménky a moment k bodu A. */
@Composable
private fun CardScope.FinalTask() {
    val t = tr()
    val kn = t.pick(L("kN", "кН"))
    val fxAns = -12 * Trig.cosD(60.0)
    val fyAns = -12 * Trig.sinD(60.0)
    val mAns = fyAns * 2
    val s1 = numStep(
        question = L("Vypočítej [[F_x]] i se znaménkem (sin 60° ≈ 0,866; cos 60° = 0,5).", "Вычисли [[F_x]] со знаком (sin 60° ≈ 0,866; cos 60° = 0,5)."),
        label = L("Vypočítej [[F_x]] včetně znaménka", "Вычисли [[F_x]] с учётом знака"),
        fields = listOf(NumSpec(L("[[F_x]]", "[[F_x]]"), fxAns, kn, signed = true)),
        explain = L("Úhel je u vodorovné a síla míří doleva: [[F_x]] = −12 · cos 60° = −6 kN.", "Угол у горизонтали, сила направлена влево: [[F_x]] = −12 · cos 60° = −6 кН."),
        traps = listOf(
            NumTrap(L("Velikost sedí. Kam míří šipka vodorovně – doprava (+), nebo doleva (−)?", "Модуль верен. Куда стрелка направлена по горизонтали — вправо (+) или влево (−)?")) { v -> close(v[0], 6.0) },
            NumTrap(L("Vypadá to na sin místo cos. Která složka leží **u** úhlu 60°?", "Похоже на sin вместо cos. Какая составляющая лежит **у** угла 60°?")) { v -> close(v[0]?.let(::abs), 10.39) },
            NumTrap(L("Složka nemůže být stejně velká jako celá síla 12 kN – je to jen její část.", "Составляющая не может равняться всей силе 12 кН — это только её часть.")) { v -> close(v[0]?.let(::abs), 12.0) },
        ),
        nudge = L("Složka u úhlu → cos; doleva je −.", "Составляющая у угла → cos; влево — это −."),
    )
    val s2 = numStep(
        question = L("Vypočítej [[F_y]] i se znaménkem (sin 60° ≈ 0,866; cos 60° = 0,5).", "Вычисли [[F_y]] со знаком (sin 60° ≈ 0,866; cos 60° = 0,5)."),
        label = L("Vypočítej [[F_y]] včetně znaménka", "Вычисли [[F_y]] с учётом знака"),
        fields = listOf(NumSpec(L("[[F_y]]", "[[F_y]]"), fyAns, kn, signed = true)),
        explain = L("Svislá složka leží naproti úhlu a míří dolů: [[F_y]] = −12 · sin 60° ≈ −10,39 kN.", "Вертикальная составляющая лежит напротив угла и направлена вниз: [[F_y]] = −12 · sin 60° ≈ −10,39 кН."),
        traps = listOf(
            NumTrap(L("Velikost sedí. Míří šipka nahoru (+), nebo dolů (−)?", "Модуль верен. Стрелка направлена вверх (+) или вниз (−)?")) { v -> close(v[0], 10.39) },
            NumTrap(L("Vypadá to na cos místo sin. Svislá složka leží **naproti** úhlu 60°.", "Похоже на cos вместо sin. Вертикальная составляющая лежит **напротив** угла 60°.")) { v -> close(v[0]?.let(::abs), 6.0) },
            NumTrap(L("Složka nemůže být stejně velká jako celá síla 12 kN – je to jen její část.", "Составляющая не может равняться всей силе 12 кН — это только её часть.")) { v -> close(v[0]?.let(::abs), 12.0) },
        ),
        nudge = L("Složka naproti úhlu → sin; dolů je −.", "Составляющая напротив угла → sin; вниз — это −."),
    )
    val s3 = numStep(
        question = L("Vypočítej moment síly k bodu A i se znaménkem (↺ kladný). P leží 2 m vpravo od A.", "Вычисли момент силы относительно точки A со знаком (↺ положительный). P лежит на 2 м правее A."),
        label = L("Vypočítej moment síly vzhledem k bodu A", "Вычисли момент силы относительно точки A"),
        fields = listOf(NumSpec(L("[[M_A]]", "[[M_A]]"), mAns, t.pick(L("kN·m", "кН·м")), signed = true)),
        explain = L(
            "K bodu A otáčí jen [[F_y]]: [[M_A]] = 2 · [[F_y]] ≈ 2 · (−10,39) ≈ −20,78 kN·m, tedy po směru hodinových ručiček (↻). Nositelka [[F_x]] prochází bodem A, proto její moment je nulový.",
            "Вокруг A вращает только [[F_y]]: [[M_A]] = 2 · [[F_y]] ≈ 2 · (−10,39) ≈ −20,78 кН·м, то есть по часовой стрелке (↻). Линия действия [[F_x]] проходит через A, поэтому её момент равен нулю.",
        ),
        traps = listOf(
            NumTrap(L("Celá síla krát vzdálenost platí jen pro kolmou sílu. Otáčí jen [[F_y]].", "Вся сила на расстояние — только для перпендикулярной силы. Вращает лишь [[F_y]].")) { v -> close(v[0]?.let(::abs), 24.0) },
            NumTrap(L("Velikost sedí. Kterým směrem síla kolem A otáčí – ↺ (+), nebo ↻ (−)?", "Модуль верен. В какую сторону сила вращает вокруг A — ↺ (+) или ↻ (−)?")) { v -> close(v[0], 20.78) },
            NumTrap(L("Nositelka [[F_x]] prochází bodem A – rameno má 0. Otáčí jen [[F_y]].", "Линия действия [[F_x]] проходит через A — плечо 0. Вращает только [[F_y]].")) { v -> close(v[0]?.let(::abs), 12.0) },
        ),
        nudge = L("[[M_A]] = (vzdálenost 2 m) · [[F_y]]; [[F_x]] leží na přímce AP.", "[[M_A]] = (расстояние 2 м) · [[F_y]]; [[F_x]] лежит на прямой AP."),
    )
    val steps = listOf(s1, s2, s3)
    val cur = steps.indexOfFirst { !it.done }
    val doneN = if (cur < 0) 3 else cur
    Stage(h = 200.0, description = t.pick(L("Přímka AP, v bodě P šikmá síla 12 kN 2 m od A", "Прямая AP, в точке P наклонная сила 12 кН в 2 м от A"))) { c ->
        val ax = 70.0
        val ay = 140.0
        val unit = 110.0
        val px = ax + 2 * unit
        signKey(30.0, 60.0, c, moment = true)
        // idealizovaná přímka AP – A je jen vztažný bod, ne podpora
        line(ax, ay, px + 40, ay, c.inkSoft, 1.0, dash = true)
        line(ax, ay, px, ay, c.ink, 2.0)
        circle(ax, ay, 5.0, c.ink)
        text(ax, ay + 22, "A", c.ink, 14.0, bold = true)
        circle(px, ay, 4.0, c.ink)
        text(px - 10, ay + 22, "P", c.ink, 14.0, bold = true)
        // síla míří dolů doleva, hrot přesně v bodě P na přímce
        val len = 100.0
        val ux = -Trig.cosD(60.0)
        val uy = Trig.sinD(60.0)
        val sx = px - ux * len
        val sy = ay - uy * len
        if (doneN >= 1) {
            force(px - ux * len, ay, px, ay, c.derived, 2.2)
            text(px + 18, ay + 20, " = −6", c.derived, 12.0, Anchor.START, sym = "F_x")
        }
        if (doneN >= 2) {
            force(px, sy, px, ay, c.derived, 2.2)
            text(px - 6, ay - 18, " = −10,39", c.derived, 12.0, Anchor.END, sym = "F_y")
        }
        if (doneN >= 3) {
            moment(ax, ay, 30.0, false, c.accent)
            text(ax + 8, ay - 40, t.pick(L(" ≈ −20,78 kN·m", " ≈ −20,78 кН·м")), c.accent, 12.0, Anchor.START, sym = "M_A")
        }
        force(sx, sy, px, ay, c.load, 3.0)
        text(px - 10, sy + 30, " = 12 $kn", c.load, 14.0, Anchor.END, sym = "F")
        angleArc(px, ay, 30.0, 0.0, -60.0, "60°", c)
        dimLine(ax, ay + 40, px, ay + 40, t.pick(L("2 m", "2 м")), c)
    }
    CardText(L(
        "Síla 12 kN působí v bodě P, 2 m vpravo od A, pod úhlem 60° k vodorovné a míří vlevo dolů. Kladné směry: +x doprava, +y nahoru, moment ↺.",
        "Сила 12 кН приложена в точке P, на 2 м правее A, под углом 60° к горизонтали и направлена влево вниз. Положительные направления: +x вправо, +y вверх, момент ↺.",
    ))
    val opener = LocalLearn.current
    val here = LocalLearnHere.current
    Steps(*steps.toTypedArray(), after = {
        ActionButton(L("Procvičování →", "Практика →")) { opener.open(LearnNav.Practice(returnTo = here)) }
    })
}

internal fun trigLesson8() = Lesson(
    id = 108,
    title = L("Trigonometrie ve statice", "Тригонометрия в статике"),
    summary = L("Složky, výslednice, její úhel a rameno momentu – všechno z jednoho trojúhelníku.", "Составляющие, равнодействующая, её угол и плечо момента — всё из одного треугольника."),
    cards = listOf(
        Card { FromVertical() },
        Card { Resultant() },
        Card { SumForces() },
        Card { MomentArm() },
        Card { InclinedBeam() },
        Card { FinalTask() },
    ),
)
