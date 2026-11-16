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
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.learn.Readout
import cz.usharik.statika.learn.SmallText
import cz.usharik.statika.learn.Stage
import cz.usharik.statika.learn.Step
import cz.usharik.statika.learn.Steps
import cz.usharik.statika.learn.ValueSlider
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.axes
import cz.usharik.statika.learn.circle
import cz.usharik.statika.learn.force
import cz.usharik.statika.learn.handle
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.quizStep
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.Prim
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Kurz trigonometrie – lekce 7 (id 107): jednotková kružnice, kvadranty a znaménka složek podle směru šipky.
 */

private const val CX = 200.0
private const val CY = 135.0
private const val R = 100.0
private const val CIRCLE_H = 270.0

/** Tip jako krok: volby a „Ověřit“ v doku. Tip se jen zaznamená – vyhodnotí ho až další krok. */
private fun predictStep(label: L, options: List<L>, guess: Int?, onGuess: (Int) -> Unit): Step =
    Step(label, guess != null, result = { if (guess != null) SmallText(L("Tvůj tip: ${options[guess].cs}", "Твой прогноз: ${options[guess].ru}")) }) {
        var sel by remember { mutableStateOf<Int?>(null) }
        Chips(options, sel) { sel = it }
        ActionButton(L("Ověřit", "Проверить"), enabled = sel != null) { sel?.let(onGuess) }
    }

/** Osy s popisky kladných směrů +x (doprava) a +y (nahoru). */
private fun PrimBuilder.signedAxes(cx: Double, cy: Double, len: Double, c: StatikaColors) {
    axes(cx, cy, len, c, labels = false)
    text(cx + len - 4, cy + 18, "+x", c.inkSoft, 13.0, Anchor.END)
    text(cx + 10, cy - len * 0.8 + 10, "+y", c.inkSoft, 13.0, Anchor.START)
}

/**
 * Jednotková kružnice s bodem pod úhlem [phi] (stupně, proti směru hodin od +x): poloměr červeně,
 * průměty fialově (x = cos vodorovně, y = sin svisle), oblouk φ. [numbers] = popisky hodnot u průmětů,
 * [ring] = kreslit kružnici (ukázka ji přidá až po trojúhelníku).
 */
private fun PrimBuilder.unitCircle(
    phi: Int, c: StatikaColors, numbers: Boolean = true, dragging: Boolean = false, showHandle: Boolean = true,
    phiLabel: String = "φ", ring: Boolean = true,
) {
    signedAxes(CX, CY, R + 40, c)
    if (ring) {
        prims += Prim.Arc(CX, CY, R, 0.0, 360.0, c.inkSoft, 1.2)
        text(CX + R + 6, CY + 16, "1", c.inkSoft, 12.0, Anchor.START)
        text(CX - R - 6, CY + 16, "−1", c.inkSoft, 12.0, Anchor.END)
        text(CX - 6, CY - R - 4, "1", c.inkSoft, 12.0, Anchor.END)
        text(CX - 6, CY + R + 14, "−1", c.inkSoft, 12.0, Anchor.END)
    }
    val co = Trig.cosD(phi.toDouble())
    val s = Trig.sinD(phi.toDouble())
    val px = CX + R * co
    val py = CY - R * s
    // průměty
    line(px, py, px, CY, c.derived, 1.0, dash = true)
    line(px, py, CX, py, c.derived, 1.0, dash = true)
    if (abs(co) > 0.01) line(CX, CY, px, CY, c.derived, 4.0)
    if (abs(s) > 0.01) line(px, CY, px, py, c.derived, 4.0)
    line(CX, CY, px, py, c.load, 2.4)
    if (phi in 1..359) {
        prims += Prim.Arc(CX, CY, 24.0, 0.0, -phi.toDouble(), c.inkSoft, 1.4)
        val mid = -phi / 2.0 * PI / 180
        text(CX + 42 * cos(mid), CY + 42 * sin(mid) + 5, phiLabel, c.inkSoft, 13.0)
    }
    if (numbers) {
        // nulová souřadnice se ukáže taky – bod leží na ose
        text((CX + px) / 2 + (if (abs(co) < 0.01) 22.0 else 0.0), CY + (if (s >= 0) 18.0 else -8.0), "x = ${num(co, 2)}", c.derived, 12.0)
        text(px + (if (co >= 0) 8.0 else -8.0), (CY + py) / 2 + 5 + (if (abs(s) < 0.01) -14.0 else 0.0), "y = ${num(s, 2)}", c.derived, 12.0, if (co >= 0) Anchor.START else Anchor.END)
    }
    if (showHandle) handle(px, py, c, dragging) else circle(px, py, 5.0, c.load)
}

/** úhel bodu pod prstem (stupně 0–359, po 5°) */
private fun phiAt(x: Double, y: Double): Int {
    var d = atan2(-(y - CY), x - CX) * 180 / PI
    if (d < 0) d += 360
    return ((d / 5).roundToInt() * 5) % 360
}

/** Karta 0: jednotková kružnice – bod (cos φ, sin φ). Od délek stran k souřadnicím. */
@Composable
private fun CardScope.UnitCircle() {
    val t = tr()
    var phase by remember { mutableIntStateOf(0) } // ukázka: 0 nic, 1 trojúhelník, 2 kružnice, 3 souřadnice
    var demoDone by remember { mutableStateOf(false) }
    var guess by remember { mutableStateOf<Int?>(null) }
    var phi by remember { mutableIntStateOf(30) }
    var placed by remember { mutableStateOf(false) }
    var miss by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        for (p in 1..3) {
            delay(800)
            phase = p
        }
        delay(900)
        demoDone = true
    }
    val s1 = Step(
        L("Sleduj přechod od trojúhelníku ke kružnici", "Проследи переход от треугольника к окружности"), demoDone,
        result = {
            Note(Tone.INFO, L(
                "Trojúhelník 30° s přeponou 1 má odvěsny cos 30° ≈ 0,87 a sin 30° = 0,5 – to jsou přesně souřadnice x a y jeho vrcholu na kružnici.",
                "У треугольника 30° с гипотенузой 1 катеты cos 30° ≈ 0,87 и sin 30° = 0,5 — это и есть координаты x и y его вершины на окружности.",
            ))
        },
    ) { SmallText(L("Sleduj obrázek…", "Смотри на рисунок…")) }
    val s2 = predictStep(
        L("Odhadni znaménko cos φ, když bod leží vlevo od osy y", "Предскажи знак cos φ, когда точка слева от оси y"),
        listOf(L("+", "+"), L("0", "0"), L("−", "−")), guess,
    ) { guess = it }
    val s3 = Step(
        L("Přesuň bod vlevo nahoru a ověř souřadnice", "Перемести точку влево вверх и проверь координаты"), placed,
        result = {
            Note(if (guess == 2) Tone.OK else Tone.INFO, L(
                (if (guess == 2) "Tip sedí: " else "") + "při φ = $phi° je x = cos φ ≈ ${num(Trig.cosD(phi.toDouble()), 2)} – vlevo od osy y je x záporné.",
                (if (guess == 2) "Прогноз верен: " else "") + "при φ = $phi° x = cos φ ≈ ${num(Trig.cosD(phi.toDouble()), 2)} — слева от оси y x отрицателен.",
            ))
        },
    ) {
        ValueSlider(t.pick(L("Úhel φ = $phi°", "Угол φ = $phi°")), phi.toFloat(), 0f..355f) {
            phi = ((it / 5).roundToInt() * 5).coerceIn(0, 355)
            miss = false
        }
        Readout("cos $phi° = ${num(Trig.cosD(phi.toDouble()), 2)} · sin $phi° = ${num(Trig.sinD(phi.toDouble()), 2)}")
        ActionButton(L("Ověřit", "Проверить")) { if (phi in 91..179) placed = true else miss = true }
        if (miss) Note(Tone.NUDGE, L("Vlevo nahoru je mezi 90° a 180° – nad osou x a vlevo od osy y.", "Влево вверх — это между 90° и 180°: над осью x и слева от оси y."))
    }
    val s4 = quizStep(
        question = L("Bod leží vlevo nahoře. Která jeho souřadnice je záporná?", "Точка слева вверху. Какая её координата отрицательна?"),
        label = L("Vyber souřadnici, která je zde záporná", "Выбери координату, которая здесь отрицательна"),
        options = listOf(
            Opt(L("x (cos φ)", "x (cos φ)"), correct = true),
            Opt(L("y (sin φ)", "y (sin φ)"), why = L("Bod leží nad osou x – y je tedy kladné.", "Точка лежит над осью x — значит, y положителен.")),
            Opt(L("obě", "обе"), why = L("Nad osou x je y kladné. Záporná je jen jedna.", "Над осью x y положителен. Отрицательна только одна.")),
            Opt(L("žádná", "ни одна"), why = L("Vlevo od osy y je x záporné.", "Слева от оси y x отрицателен.")),
        ),
        explain = L(
            "Vlevo je x záporné, proto cos φ < 0. Nahoře je y kladné, proto sin φ > 0. Už nejde o délky stran, ale o **souřadnice** – ty záporné být mohou.",
            "Слева x отрицателен, поэтому cos φ < 0. Сверху y положителен, поэтому sin φ > 0. Это уже не длины сторон, а **координаты** — они бывают отрицательными.",
        ),
        columns = 2,
    )
    val steps = listOf(s1, s2, s3, s4)
    val cur = steps.indexOfFirst { !it.done }
    val free = cur == 2
    Stage(
        h = CIRCLE_H,
        description = t.pick(L("Jednotková kružnice s pohyblivým bodem", "Единичная окружность с подвижной точкой")),
        onDrag = { p, start ->
            if (start) dragging = free && p.dist(CX, CY) > R * 0.5
            if (dragging) {
                phi = phiAt(p.x, p.y)
                miss = false
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        if (cur == 0) {
            if (phase == 0) return@Stage
            // trojúhelník 30° s přeponou 1 – pak kolem něj kružnice a souřadnice
            unitCircle(30, c, numbers = phase >= 3, showHandle = false, phiLabel = "30°", ring = phase >= 2)
            return@Stage
        }
        val shown = if (cur == 1) 30 else phi
        unitCircle(shown, c, numbers = cur != 1, dragging = dragging, showHandle = free)
    }
    CardText(L(
        "Na **jednotkové kružnici** (poloměr 1) má bod souřadnice **x = cos φ** a **y = sin φ**. Úhel φ měříme od +x proti směru hodinových ručiček.",
        "На **единичной окружности** (радиус 1) координаты точки: **x = cos φ**, **y = sin φ**. Угол φ отсчитываем от +x против часовой стрелки.",
    ))
    Steps(*steps.toTypedArray())
}

/** Volby znamének (cos, sin) pro kvíz kvadrantů; [want] = index správné dvojice. */
private fun signOpts(want: Int): List<Opt> {
    val pairs = listOf(1 to 1, -1 to 1, -1 to -1, 1 to -1)
    val (wc, ws) = pairs[want]
    return pairs.mapIndexed { i, (pc, ps) ->
        fun s(v: Int) = if (v > 0) "+" else "−"
        val why = when {
            i == want -> null
            pc != wc -> if (wc > 0) L("Bod leží vpravo od osy y – jaké znaménko má x = cos φ?", "Точка справа от оси y — какой знак у x = cos φ?")
            else L("Bod leží vlevo od osy y – jaké znaménko má x = cos φ?", "Точка слева от оси y — какой знак у x = cos φ?")
            else -> if (ws > 0) L("Bod leží nad osou x – jaké znaménko má y = sin φ?", "Точка над осью x — какой знак у y = sin φ?")
            else L("Bod leží pod osou x – jaké znaménko má y = sin φ?", "Точка под осью x — какой знак у y = sin φ?")
        }
        Opt(L("cos ${s(pc)}, sin ${s(ps)}", "cos ${s(pc)}, sin ${s(ps)}"), correct = i == want, why = why)
    }
}

/** Karta 1: čtyři kvadranty a znaménka – odvodit z polohy bodu, ne obejít kružnici. */
@Composable
private fun CardScope.Quadrants() {
    val t = tr()
    var phi by remember { mutableIntStateOf(45) }
    var dragging by remember { mutableStateOf(false) }
    val roman = listOf("I.", "II.", "III.", "IV.")
    val at = listOf(30, 150, 210, 330)
    val qs = (0..3).map { q ->
        quizStep(
            question = L(
                "Bod je na ${at[q]}° (${roman[q]} kvadrant). Jaká znaménka mají cos φ a sin φ?",
                "Точка на ${at[q]}° (${roman[q]} квадрант). Какие знаки у cos φ и sin φ?",
            ),
            label = L("Urči znaménka cos φ a sin φ v ${roman[q]} kvadrantu", "Определи знаки cos φ и sin φ в ${roman[q]} квадранте"),
            options = signOpts(q),
            explain = L(
                listOf("Vpravo nahoře: x > 0, y > 0 – cos i sin kladné.", "Vlevo nahoře: x < 0, y > 0 – cos záporný, sin kladný.", "Vlevo dole: x < 0, y < 0 – cos i sin záporné.", "Vpravo dole: x > 0, y < 0 – cos kladný, sin záporný.")[q],
                listOf("Справа вверху: x > 0, y > 0 — cos и sin положительны.", "Слева вверху: x < 0, y > 0 — cos отрицателен, sin положителен.", "Слева внизу: x < 0, y < 0 — cos и sin отрицательны.", "Справа внизу: x > 0, y < 0 — cos положителен, sin отрицателен.")[q],
            ),
            columns = 2,
        )
    }
    val s5 = quizStep(
        question = L("Bod je na 90°, přímo nahoře na ose y. Kolik je cos 90°?", "Точка на 90°, прямо вверху на оси y. Чему равен cos 90°?"),
        label = L("Urči cos 90°", "Определи cos 90°"),
        options = listOf(
            Opt(L("−1", "−1"), why = L("−1 by byl bod úplně vlevo (180°).", "−1 — это точка в крайнем левом положении (180°).")),
            Opt(L("0", "0"), correct = true),
            Opt(L("1", "1"), why = L("1 je sin 90° – výška bodu. Jak daleko je bod vpravo od osy y?", "1 — это sin 90°, высота точки. Насколько точка правее оси y?")),
        ),
        explain = L(
            "Na ose y je x = 0, proto cos 90° = 0. **cos φ má znaménko souřadnice x, sin φ souřadnice y.** Na osách je jedna souřadnice nulová – bod tehdy neleží v žádném kvadrantu.",
            "На оси y x = 0, поэтому cos 90° = 0. **Знак cos φ совпадает со знаком x, знак sin φ — со знаком y.** На осях одна координата равна нулю — такая точка не лежит ни в одном квадранте.",
        ),
        columns = 3,
    )
    val steps = qs + s5
    val cur = steps.indexOfFirst { !it.done }
    val shown = when (cur) {
        in 0..3 -> at[cur]
        4 -> 90
        else -> phi
    }
    val signs = listOf("+" to "+", "−" to "+", "−" to "−", "+" to "−")
    Stage(
        h = CIRCLE_H,
        description = t.pick(L("Jednotková kružnice se čtyřmi kvadranty", "Единичная окружность с четырьмя квадрантами")),
        onDrag = { p, start ->
            if (start) dragging = cur < 0 && p.dist(CX, CY) > R * 0.5
            if (dragging) phi = phiAt(p.x, p.y)
        },
        onDragEnd = { dragging = false },
    ) { c ->
        unitCircle(shown, c, numbers = cur < 0, dragging = dragging, showHandle = cur < 0)
        val pos = listOf(330.0 to 30.0, 70.0 to 30.0, 70.0 to 235.0, 330.0 to 235.0)
        for (q in 0..3) {
            val (x, y) = pos[q]
            val here = cur == q || (cur < 0 && quadrantOf(phi) == q)
            text(x, y, roman[q], if (here) c.ink else c.inkSoft, 15.0, bold = true)
            // znaménka se odkrývají postupně – jen u vyřešených kvadrantů
            if (qs[q].done) text(x, y + 20, "cos ${signs[q].first}, sin ${signs[q].second}", c.derived, 13.0)
        }
    }
    CardText(L(
        "Osy dělí rovinu na čtyři **kvadranty**. Znaménka cos φ a sin φ určuje poloha bodu – vlevo/vpravo a nahoře/dole.",
        "Оси делят плоскость на четыре **квадранта** (четверти). Знаки cos φ и sin φ определяются положением точки — слева/справа и сверху/снизу.",
    ))
    Steps(*steps.toTypedArray(), after = {
        ValueSlider(t.pick(L("Úhel φ = $phi°", "Угол φ = $phi°")), phi.toFloat(), 0f..355f) { phi = ((it / 5).roundToInt() * 5).coerceIn(0, 355) }
    })
}

private fun quadrantOf(p: Int): Int? = when (p) {
    in 1..89 -> 0
    in 91..179 -> 1
    in 181..269 -> 2
    in 271..359 -> 3
    else -> null
}

/** Karta 2: velikost složek z ostrého úhlu, znaménko podle šipky. Směr se po každém kole změní sám. */
@Composable
private fun CardScope.AcuteAndSign() {
    val t = tr()
    // kola: ↖ 150°, ↙ 210°, ↘ 330° (úhel φ od +x)
    val phis = listOf(150, 210, 330)
    val names = listOf(L("vlevo nahoru", "влево вверх"), L("vlevo dolů", "влево вниз"), L("vpravo dolů", "вправо вниз"))
    var solved by remember { mutableIntStateOf(0) } // kolik kol je hotových
    var sx by remember { mutableStateOf<Int?>(null) } // 0 = +, 1 = −
    var sy by remember { mutableStateOf<Int?>(null) }
    var miss by remember { mutableStateOf<L?>(null) }
    val fx = 10 * Trig.cosD(30.0)
    val fy = 10 * Trig.sinD(30.0)
    fun sgn(s: Int?) = when (s) { 0 -> "+"; 1 -> "−"; else -> "?" }
    fun want(k: Int) = (if (Trig.cosD(phis[k].toDouble()) > 0) 0 else 1) to (if (Trig.sinD(phis[k].toDouble()) > 0) 0 else 1)
    val steps = phis.indices.map { k ->
        val (wx, wy) = want(k)
        Step(
            L("Doplň znaménka složek síly mířící ${names[k].cs}", "Укажи знаки составляющих силы, направленной ${names[k].ru}"), solved > k,
            result = {
                SmallText(L(
                    "${names[k].cs}: [[F_x]] = ${if (wx == 0) "+" else "−"}${num(fx, 2)} kN, [[F_y]] = ${if (wy == 0) "+" else "−"}${num(fy, 2)} kN",
                    "${names[k].ru}: [[F_x]] = ${if (wx == 0) "+" else "−"}${num(fx, 2)} кН, [[F_y]] = ${if (wy == 0) "+" else "−"}${num(fy, 2)} кН",
                ))
            },
        ) {
            Chips(listOf(L("[[F_x]]: +", "[[F_x]]: +"), L("[[F_x]]: −", "[[F_x]]: −")), sx) { sx = it; miss = null }
            Chips(listOf(L("[[F_y]]: +", "[[F_y]]: +"), L("[[F_y]]: −", "[[F_y]]: −")), sy) { sy = it; miss = null }
            Readout(t.pick(L(
                "[[F_x]] = ${sgn(sx)}${num(fx, 2)} kN · [[F_y]] = ${sgn(sy)}${num(fy, 2)} kN",
                "[[F_x]] = ${sgn(sx)}${num(fx, 2)} кН · [[F_y]] = ${sgn(sy)}${num(fy, 2)} кН",
            )))
            ActionButton(L("Zkontrolovat", "Проверить")) {
                miss = when {
                    sx == null || sy == null -> L("Vyber znaménko u obou složek.", "Выбери знак у обеих составляющих.")
                    sx != wx -> L("Kam míří hrot šipky vodorovně – doprava (+), nebo doleva (−)?", "Куда смотрит остриё по горизонтали — вправо (+) или влево (−)?")
                    sy != wy -> L("Míří šipka nahoru (+), nebo dolů (−)?", "Стрелка направлена вверх (+) или вниз (−)?")
                    else -> null
                }
                if (miss == null) {
                    solved = k + 1
                    sx = null
                    sy = null
                }
            }
            miss?.let { Note(Tone.NUDGE, it) }
        }
    }
    val cur = steps.indexOfFirst { !it.done }
    val k = if (cur < 0) phis.lastIndex else cur
    val phi = phis[k]
    Stage(h = 250.0, description = t.pick(L("Síla 10 kN pod úhlem 30° k vodorovné ve směru ${names[k].cs}", "Сила 10 кН под углом 30° к горизонтали, направленная ${names[k].ru}"))) { c ->
        val cx = 200.0
        val cy = 130.0
        signedAxes(cx, cy, 150.0, c)
        val len = 110.0
        val co = Trig.cosD(phi.toDouble())
        val s = Trig.sinD(phi.toDouble())
        val tx = cx + len * co
        val ty = cy - len * s
        if (cur < 0) {
            line(tx, cy, tx, ty, c.derived, 1.0, dash = true)
            line(cx, ty, tx, ty, c.derived, 1.0, dash = true)
            force(cx, cy, tx, cy, c.derived, 2.4)
            force(cx, cy, cx, ty, c.derived, 2.4)
            text((cx + tx) / 2, cy + (if (s > 0) 18.0 else -8.0), " = +${num(fx, 2)}", c.derived, 13.0, sym = "F_x")
            text(cx + (if (co > 0) -10.0 else 10.0), (cy + ty) / 2 + 5, " = −${num(fy, 2)}", c.derived, 13.0, if (co > 0) Anchor.END else Anchor.START, sym = "F_y")
        }
        force(cx, cy, tx, ty, c.load, 3.0)
        text(tx + (if (co > 0) 8.0 else -8.0), ty + (if (s > 0) -6.0 else 16.0), t.pick(L(" = 10 kN", " = 10 кН")), c.load, 14.0, if (co > 0) Anchor.START else Anchor.END, sym = "F")
        val base = if (co > 0) 0.0 else 180.0
        angleArc(cx, cy, 40.0, base, -phi.toDouble(), "30°", c)
    }
    CardText(L(
        "Pro sílu 10 kN pod úhlem 30° k vodorovné jsou **velikosti** složek 8,66 kN a 5 kN. **Znaménka** závisejí na směru šipky.",
        "Для силы 10 кН под углом 30° к горизонтали **модули** составляющих равны 8,66 кН и 5 кН. **Знаки** зависят от направления стрелки.",
    ))
    Steps(*steps.toTypedArray())
    if (cur < 0) Note(Tone.OK, L(
        "Doprava znamená [[F_x]] > 0, doleva [[F_x]] < 0; nahoru [[F_y]] > 0, dolů [[F_y]] < 0. Velikosti zůstaly stejné, změnily se jen směry.",
        "Вправо — [[F_x]] > 0, влево — [[F_x]] < 0; вверх — [[F_y]] > 0, вниз — [[F_y]] < 0. Модули не изменились, изменились только направления.",
    ))
}

/** Karta 3: kvíz – síla dolů doprava pod 60°. */
@Composable
private fun CardScope.CheckDownRight() {
    val t = tr()
    val q = quizStep(
        question = L("Síla 10 kN míří vpravo dolů, 60° od vodorovné. Která dvojice ([[F_x]]; [[F_y]]) v kN platí?", "Сила 10 кН направлена вправо вниз, 60° от горизонтали. Какая пара ([[F_x]]; [[F_y]]) в кН верна?"),
        label = L("Vyber správnou dvojici složek [[F_x]] a [[F_y]]", "Выбери правильную пару составляющих [[F_x]] и [[F_y]]"),
        options = listOf(
            Opt(L("(+5; −8,66)", "(+5; −8,66)"), correct = true),
            Opt(L("(+8,66; −5)", "(+8,66; −5)"), why = L("Znaménka sedí, ale velikosti jsou prohozené. Která složka leží u úhlu 60°?", "Знаки верны, но модули перепутаны. Какая составляющая лежит у угла 60°?")),
            Opt(L("(+5; +8,66)", "(+5; +8,66)"), why = L("Velikosti sedí. Míří šipka nahoru, nebo dolů?", "Модули верны. Стрелка направлена вверх или вниз?")),
            Opt(L("(−5; −8,66)", "(−5; −8,66)"), why = L("Svisle je to dobře. Kam míří hrot vodorovně?", "По вертикали верно. Куда смотрит остриё по горизонтали?")),
        ),
        explain = L(
            "[[F_x]] = 10 · cos 60° = +5 kN (doprava), [[F_y]] = −10 · sin 60° ≈ −8,66 kN (dolů).",
            "[[F_x]] = 10 · cos 60° = +5 кН (вправо), [[F_y]] = −10 · sin 60° ≈ −8,66 кН (вниз).",
        ),
        columns = 2,
    )
    val shown = q.done
    Stage(h = 220.0, description = t.pick(L("Síla 10 kN šikmo dolů doprava, 60° k vodorovné", "Сила 10 кН наклонно вниз вправо, 60° к горизонтали"))) { c ->
        val cx = 170.0
        val cy = 60.0
        val len = 140.0
        val co = Trig.cosD(60.0)
        val s = Trig.sinD(60.0)
        val tx = cx + len * co
        val ty = cy + len * s
        line(40.0, cy, 360.0, cy, c.inkSoft, 1.0, dash = true)
        // kladné směry
        force(330.0, 190.0, 370.0, 190.0, c.inkSoft, 1.4, head = 7.0)
        force(330.0, 190.0, 330.0, 150.0, c.inkSoft, 1.4, head = 7.0)
        text(374.0, 194.0, "+x", c.inkSoft, 12.0, Anchor.START)
        text(330.0, 142.0, "+y", c.inkSoft, 12.0)
        if (shown) {
            line(tx, cy, tx, ty, c.derived, 1.0, dash = true)
            line(cx, ty, tx, ty, c.derived, 1.0, dash = true)
            force(cx, cy, tx, cy, c.derived, 2.4)
            force(cx, cy, cx, ty, c.derived, 2.4)
            text((cx + tx) / 2, cy - 8, t.pick(L(" = +5 kN", " = +5 кН")), c.derived, 13.0, sym = "F_x")
            text(cx - 10, (cy + ty) / 2 + 5, t.pick(L(" = −8,66 kN", " = −8,66 кН")), c.derived, 13.0, Anchor.END, sym = "F_y")
        }
        force(cx, cy, tx, ty, c.load, 3.0)
        text(tx + 8, ty, t.pick(L(" = 10 kN", " = 10 кН")), c.load, 14.0, Anchor.START, sym = "F")
        angleArc(cx, cy, 40.0, 0.0, 60.0, "60°", c)
    }
    CardText(L(
        "Síla 10 kN míří vpravo dolů pod úhlem 60° k vodorovné. Kladné směry jsou doprava a nahoru.",
        "Сила 10 кН направлена вправо вниз под углом 60° к горизонтали. Положительные направления — вправо и вверх.",
    ))
    Steps(q)
}

/** Karta 4 (volitelná): kvíz – cos 150° přes vztažný úhel 30°. */
@Composable
private fun CardScope.CheckCos150() {
    val t = tr()
    val q = quizStep(
        question = L("Kolik je cos 150°?", "Чему равен cos 150°?"),
        label = L("Vyber hodnotu cos 150°", "Выбери значение cos 150°"),
        options = listOf(
            Opt(L("−0,866", "−0,866"), correct = true),
            Opt(L("+0,866", "+0,866"), why = L("Velikost sedí – vztažný úhel je 30°. Na které straně od osy y bod leží?", "Модуль верен — опорный угол 30°. С какой стороны от оси y лежит точка?")),
            Opt(L("−0,5", "−0,5"), why = L("0,5 je výška bodu, tedy sin 150°. Která souřadnice je cos?", "0,5 — высота точки, то есть sin 150°. Какая координата — cos?")),
            Opt(L("+0,5", "+0,5"), why = L("To je sin 150°. Kosinus je vodorovná souřadnice – a bod leží vlevo.", "Это sin 150°. Косинус — горизонтальная координата, а точка лежит слева.")),
        ),
        explain = L(
            "Bod leží vlevo, takže kosinus je záporný. cos 150° = −cos 30° = −√3/2 ≈ −0,866.",
            "Точка лежит слева, поэтому косинус отрицателен. cos 150° = −cos 30° = −√3/2 ≈ −0,866.",
        ),
        columns = 2,
    )
    Stage(h = CIRCLE_H, description = t.pick(L("Jednotková kružnice s bodem na 150° a vztažným úhlem 30°", "Единичная окружность с точкой на 150° и опорным углом 30°"))) { c ->
        unitCircle(150, c, numbers = false, showHandle = false, phiLabel = "150°")
        // vztažný úhel 30° mezi poloměrem a zápornou poloosou x – jiná barva než směrový úhel 150°
        prims += Prim.Arc(CX, CY, 52.0, -150.0, -30.0, c.accent, 2.0)
        val mid = -165.0 * PI / 180
        text(CX + 70 * cos(mid), CY + 70 * sin(mid) + 5, "30°", c.accent, 13.0, bold = true)
    }
    CardText(L(
        "Úhel 150° leží ve II. kvadrantu. Mezi poloměrem a zápornou poloosou x zbývá **30°**.",
        "Угол 150° находится во II квадранте. Между радиусом и отрицательной полуосью x — **30°**.",
    ))
    Steps(q)
}

internal fun trigLesson7() = Lesson(
    id = 107,
    title = L("Jednotková kružnice a znaménka", "Единичная окружность и знаки"),
    summary = L("Kdy je složka kladná a kdy záporná – podle směru šipky.", "Когда составляющая положительна, а когда отрицательна — по направлению стрелки."),
    cards = listOf(
        Card { UnitCircle() },
        Card { Quadrants() },
        Card { AcuteAndSign() },
        Card { CheckDownRight() },
        Card { CheckCos150() },
    ),
)
