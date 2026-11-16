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
import cz.usharik.statika.learn.Note
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.learn.Quiz
import cz.usharik.statika.learn.Readout
import cz.usharik.statika.learn.SmallText
import cz.usharik.statika.learn.Stage
import cz.usharik.statika.learn.Todo
import cz.usharik.statika.learn.ValueSlider
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.axes
import cz.usharik.statika.learn.circle
import cz.usharik.statika.learn.force
import cz.usharik.statika.learn.handle
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.Prim
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt

/*
 * Kurz trigonometrie – lekce 7 (id 107): jednotková kružnice, kvadranty a znaménka složek podle směru šipky.
 */

private const val CX = 200.0
private const val CY = 150.0
private const val R = 105.0

/**
 * Jednotková kružnice s bodem pod úhlem [phi] (stupně, proti směru hodin od +x): poloměr červeně,
 * průměty fialově (cos vodorovně, sin svisle), oblouk φ. [numbers] = popisky hodnot u průmětů.
 */
private fun PrimBuilder.unitCircle(phi: Int, c: StatikaColors, numbers: Boolean = true, dragging: Boolean = false, showHandle: Boolean = true, phiLabel: String = "φ") {
    axes(CX, CY, R + 45, c)
    prims += Prim.Arc(CX, CY, R, 0.0, 360.0, c.inkSoft, 1.2)
    text(CX + R + 6, CY + 16, "1", c.inkSoft, 12.0, Anchor.START)
    text(CX - R - 6, CY + 16, "−1", c.inkSoft, 12.0, Anchor.END)
    text(CX - 6, CY - R - 4, "1", c.inkSoft, 12.0, Anchor.END)
    text(CX - 6, CY + R + 14, "−1", c.inkSoft, 12.0, Anchor.END)
    val co = Trig.cosD(phi.toDouble())
    val s = Trig.sinD(phi.toDouble())
    val px = CX + R * co
    val py = CY - R * s
    // průměty
    line(px, py, px, CY, c.derived, 1.0, dash = true)
    line(px, py, CX, py, c.derived, 1.0, dash = true)
    if (kotlin.math.abs(co) > 0.01) line(CX, CY, px, CY, c.derived, 4.0)
    if (kotlin.math.abs(s) > 0.01) line(px, CY, px, py, c.derived, 4.0)
    line(CX, CY, px, py, c.load, 2.4)
    if (phi in 1..359) {
        prims += Prim.Arc(CX, CY, 24.0, 0.0, -phi.toDouble(), c.inkSoft, 1.4)
        val mid = -phi / 2.0 * PI / 180
        text(CX + 42 * kotlin.math.cos(mid), CY + 42 * kotlin.math.sin(mid) + 5, phiLabel, c.inkSoft, 13.0)
    }
    if (numbers) {
        if (kotlin.math.abs(co) > 0.01) text((CX + px) / 2, CY + (if (s >= 0) 18.0 else -8.0), "cos = ${num(co, 2)}", c.derived, 12.0)
        if (kotlin.math.abs(s) > 0.01) text(px + (if (co >= 0) 8.0 else -8.0), (CY + py) / 2 + 5, "sin = ${num(s, 2)}", c.derived, 12.0, if (co >= 0) Anchor.START else Anchor.END)
    }
    if (showHandle) handle(px, py, c, dragging) else circle(px, py, 5.0, c.load)
}

/** úhel bodu pod prstem (stupně 0–359, po 5°) */
private fun phiAt(x: Double, y: Double): Int {
    var d = atan2(-(y - CY), x - CX) * 180 / PI
    if (d < 0) d += 360
    return ((d / 5).roundToInt() * 5) % 360
}

/** Karta 0: jednotková kružnice – bod (cos φ, sin φ). */
@Composable
private fun CardScope.UnitCircle() {
    val t = tr()
    var phi by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var at30 by remember { mutableStateOf(false) }
    var left by remember { mutableStateOf(false) }
    fun set(v: Int) {
        phi = v
        if (v == 30) at30 = true
        if (v in 91..179) left = true
    }
    Stage(
        h = 300.0,
        description = t.pick(L("Jednotková kružnice s pohyblivým bodem", "Единичная окружность с подвижной точкой")),
        onDrag = { p, start ->
            if (start) dragging = p.dist(CX, CY) > R * 0.5
            if (dragging) set(phiAt(p.x, p.y))
        },
        onDragEnd = { dragging = false },
    ) { c -> unitCircle(phi, c, dragging = dragging) }
    CardText(L(
        "**Jednotková kružnice** má poloměr 1. Úhel φ (čti „fí“) měříme od kladné poloosy x (doprava) proti směru hodinových ručiček. Bod na kružnici je vpravo o cos φ a nahoře o sin φ.",
        "У **единичной окружности** радиус 1. Угол φ («фи») отсчитываем от положительной полуоси x (вправо) против часовой стрелки. Точка на окружности смещена вправо на cos φ и вверх на sin φ.",
    ))
    Todo(
        L("Nastav φ = 30° (tahem za bod nebo posuvníkem „Úhel φ“) – poznáváš trojúhelník z lekce 4?", "Поставь φ = 30° (перетащи точку или сдвинь ползунок «Угол φ») — узнаёшь треугольник из урока 4?") to at30,
        L("Přetáhni bod do levé horní čtvrtiny (mezi 90° a 180°) – co se stane s cos φ?", "Перетащи точку в левую верхнюю четверть (между 90° и 180°) — что произойдёт с cos φ?") to left,
    )
    ValueSlider(t.pick(L("Úhel φ = $phi°", "Угол φ = $phi°")), phi.toFloat(), 0f..355f) { set(((it / 5).roundToInt() * 5).coerceIn(0, 355)) }
    Readout("cos $phi° = ${num(Trig.cosD(phi.toDouble()), 2)} · sin $phi° = ${num(Trig.sinD(phi.toDouble()), 2)}")
    if (left) SmallText(L(
        "Vlevo od osy y je cos φ záporný – bod leží v záporných x.",
        "Слева от оси y cos φ отрицательный — точка лежит в области отрицательных x.",
    ))
    SmallText(L(
        "Takhle mají sin a cos smysl i pro úhly větší než 90° – kružnice je vlastně „hodinová ručička“, která vyšla ze 3 hodin.",
        "Так sin и cos имеют смысл и для углов больше 90° — радиус как стрелка часов, которая вышла из положения «3 часа».",
    ))
}

/** Karta 1: čtyři kvadranty a znaménka. */
@Composable
private fun CardScope.Quadrants() {
    val t = tr()
    var phi by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var visited by remember { mutableStateOf(setOf<Int>()) }
    fun quadrant(p: Int): Int? = when (p) {
        in 1..89 -> 1
        in 91..179 -> 2
        in 181..269 -> 3
        in 271..359 -> 4
        else -> null
    }
    fun set(v: Int) {
        phi = v
        quadrant(v)?.let { visited = visited + it }
    }
    val signs = mapOf(1 to ("+" to "+"), 2 to ("−" to "+"), 3 to ("−" to "−"), 4 to ("+" to "−"))
    Stage(
        h = 300.0,
        description = t.pick(L("Jednotková kružnice se čtyřmi kvadranty", "Единичная окружность с четырьмя квадрантами")),
        onDrag = { p, start ->
            if (start) dragging = p.dist(CX, CY) > R * 0.5
            if (dragging) set(phiAt(p.x, p.y))
        },
        onDragEnd = { dragging = false },
    ) { c ->
        unitCircle(phi, c, numbers = false, dragging = dragging)
        val pos = mapOf(1 to (330.0 to 40.0), 2 to (70.0 to 40.0), 3 to (70.0 to 250.0), 4 to (330.0 to 250.0))
        val roman = mapOf(1 to "I.", 2 to "II.", 3 to "III.", 4 to "IV.")
        for (q in 1..4) {
            val (x, y) = pos.getValue(q)
            val on = q in visited
            text(x, y, roman.getValue(q), if (quadrant(phi) == q) c.ink else c.inkSoft, 15.0, bold = true)
            if (on) {
                val (sc, ss) = signs.getValue(q)
                text(x, y + 20, "cos $sc, sin $ss", c.derived, 13.0)
            }
        }
    }
    CardText(L(
        "Osy dělí rovinu na čtyři **kvadranty**. Znaménko cos říká vpravo/vlevo, znaménko sin nahoře/dole.",
        "Оси делят плоскость на четыре **квадранта** (четверти). Знак cos говорит «справа/слева», знак sin — «сверху/снизу».",
    ))
    Todo(
        L("Navštiv I. kvadrant (0°–90°): přetáhni bod nebo posuň posuvník", "Зайди в I квадрант (четверть, 0°–90°): перетащи точку или сдвинь ползунок") to (1 in visited),
        L("II. kvadrant (90°–180°)", "II квадрант (90°–180°)") to (2 in visited),
        L("III. kvadrant (180°–270°)", "III квадрант (180°–270°)") to (3 in visited),
        L("IV. kvadrant (270°–360°)", "IV квадрант (270°–360°)") to (4 in visited),
    )
    ValueSlider(t.pick(L("Úhel φ = $phi°", "Угол φ = $phi°")), phi.toFloat(), 0f..355f) { set(((it / 5).roundToInt() * 5).coerceIn(0, 355)) }
    SmallText(L(
        "Vpravo je cos kladný, vlevo záporný. Nahoře je sin kladný, dole záporný.",
        "Справа cos положительный, слева отрицательный. Сверху sin положительный, снизу отрицательный.",
    ))
}

/** Karta 2: velikost složek z ostrého úhlu, znaménko podle šipky. */
@Composable
private fun CardScope.AcuteAndSign() {
    val t = tr()
    // směry: 0 ↗, 1 ↖, 2 ↙, 3 ↘ (úhel φ od +x)
    val phis = listOf(30, 150, 210, 330)
    var dir by remember { mutableIntStateOf(0) }
    var sx by remember { mutableStateOf<Int?>(null) } // 0 = +, 1 = −
    var sy by remember { mutableStateOf<Int?>(null) }
    var done by remember { mutableStateOf(setOf<Int>()) }
    val phi = phis[dir]
    val wantX = if (Trig.cosD(phi.toDouble()) > 0) 0 else 1
    val wantY = if (Trig.sinD(phi.toDouble()) > 0) 0 else 1
    val okX = sx == wantX
    val okY = sy == wantY
    val ok = okX && okY
    fun pick(nx: Int?, ny: Int?) {
        sx = nx
        sy = ny
        if (nx == wantX && ny == wantY) done = done + dir
    }
    Stage(h = 260.0, description = t.pick(L("Síla 10 kN pod úhlem 30° k vodorovné ve zvoleném směru", "Сила 10 кН под углом 30° к горизонтали в выбранном направлении"))) { c ->
        val cx = 200.0
        val cy = 135.0
        axes(cx, cy, 160.0, c)
        val len = 110.0
        val co = Trig.cosD(phi.toDouble())
        val s = Trig.sinD(phi.toDouble())
        val tx = cx + len * co
        val ty = cy - len * s
        if (ok) {
            line(tx, cy, tx, ty, c.derived, 1.0, dash = true)
            line(cx, ty, tx, ty, c.derived, 1.0, dash = true)
            force(cx, cy, tx, cy, c.derived, 2.4)
            force(cx, cy, cx, ty, c.derived, 2.4)
            text((cx + tx) / 2, cy + (if (s > 0) 18.0 else -8.0), null, c.derived, 13.0, sym = "F_x")
            text(cx + (if (co > 0) -10.0 else 10.0), (cy + ty) / 2 + 5, null, c.derived, 13.0, if (co > 0) Anchor.END else Anchor.START, sym = "F_y")
        }
        force(cx, cy, tx, ty, c.load, 3.0)
        text(tx + (if (co > 0) 8.0 else -8.0), ty + (if (s > 0) -6.0 else 16.0), " = 10 kN", c.load, 14.0, if (co > 0) Anchor.START else Anchor.END, sym = "F")
        val base = if (co > 0) 0.0 else 180.0
        angleArc(cx, cy, 40.0, base, -phi.toDouble(), "30°", c)
    }
    CardText(L(
        "Postup pro statiku: velikosti složek spočítej z **ostrého** úhlu α, znaménko urči **pohledem na šipku**. Doprava +, doleva −, nahoru +, dolů −.",
        "Приём для статики: величины составляющих считай по **острому** углу α, а знак определяй, **глядя на стрелку**. Вправо +, влево −, вверх +, вниз −.",
    ))
    Todo(
        L("Klepni na „↖“ a pak vyber znaménko u [[F_x]] i [[F_y]]", "Нажми «↖», затем выбери знак у [[F_x]] и у [[F_y]]") to (1 in done),
        L("Totéž pro „↙“", "То же для «↙»") to (2 in done),
        L("Totéž pro „↘“", "То же для «↘»") to (3 in done),
    )
    Chips(listOf(L("↗", "↗"), L("↖", "↖"), L("↙", "↙"), L("↘", "↘")), dir) { i ->
        if (i != dir) {
            dir = i
            sx = null
            sy = null
        }
    }
    Chips(listOf(L("[[F_x]]: +", "[[F_x]]: +"), L("[[F_x]]: −", "[[F_x]]: −")), sx) { pick(it, sy) }
    Chips(listOf(L("[[F_y]]: +", "[[F_y]]: +"), L("[[F_y]]: −", "[[F_y]]: −")), sy) { pick(sx, it) }
    val fx = 10 * Trig.cosD(30.0)
    val fy = 10 * Trig.sinD(30.0)
    fun sgn(s: Int?) = when (s) { 0 -> "+"; 1 -> "−"; else -> "±" }
    Readout("[[F_x]] = ${sgn(sx)}${num(fx, 2)} kN · [[F_y]] = ${sgn(sy)}${num(fy, 2)} kN", ok = ok)
    if (sx != null && !okX) Note(Tone.NUDGE, L("Kam míří hrot šipky vodorovně – doprava, nebo doleva?", "Куда смотрит остриё по горизонтали — вправо или влево?"))
    else if (sy != null && !okY) Note(Tone.NUDGE, L("Míří šipka nahoru, nebo dolů?", "Стрелка направлена вверх или вниз?"))
    SmallText(L(
        "Tak to dělá i trenažér: α je úhel mezi silou a nosníkem, vždy mezi 0° a 90°.",
        "Так делает и тренажёр: α — угол между силой и балкой, всегда от 0° до 90°.",
    ))
}

/** Karta 3: kvíz – síla dolů doprava pod 60°. */
@Composable
private fun CardScope.CheckDownRight() {
    val t = tr()
    val shown = solvedHere
    Stage(h = 240.0, description = t.pick(L("Síla 10 kN šikmo dolů doprava, 60° k vodorovné", "Сила 10 кН наклонно вниз вправо, 60° к горизонтали"))) { c ->
        val cx = 170.0
        val cy = 70.0
        val len = 140.0
        val co = Trig.cosD(60.0)
        val s = Trig.sinD(60.0)
        val tx = cx + len * co
        val ty = cy + len * s
        line(40.0, cy, 360.0, cy, c.inkSoft, 1.0, dash = true)
        if (shown) {
            line(tx, cy, tx, ty, c.derived, 1.0, dash = true)
            line(cx, ty, tx, ty, c.derived, 1.0, dash = true)
            force(cx, cy, tx, cy, c.derived, 2.4)
            force(cx, cy, cx, ty, c.derived, 2.4)
            text((cx + tx) / 2, cy - 8, " = +5 kN", c.derived, 13.0, sym = "F_x")
            text(cx - 10, (cy + ty) / 2 + 5, " = −8,66 kN", c.derived, 13.0, Anchor.END, sym = "F_y")
        }
        force(cx, cy, tx, ty, c.load, 3.0)
        text(tx + 8, ty, " = 10 kN", c.load, 14.0, Anchor.START, sym = "F")
        angleArc(cx, cy, 40.0, 0.0, 60.0, "60°", c)
    }
    Quiz(
        question = L("Síla [[F]] = 10 kN míří šikmo dolů doprava a s vodorovnou svírá 60°. Jaké jsou složky?", "Сила [[F]] = 10 кН направлена наклонно вниз вправо и составляет с горизонталью 60°. Каковы составляющие?"),
        options = listOf(
            Opt(L("[[F_x]] = +5 kN, [[F_y]] = −8,66 kN", "[[F_x]] = +5 кН, [[F_y]] = −8,66 кН"), correct = true),
            Opt(L("[[F_x]] = +8,66 kN, [[F_y]] = −5 kN", "[[F_x]] = +8,66 кН, [[F_y]] = −5 кН"), why = L("Znaménka sedí, ale velikosti jsou prohozené. Která složka leží u úhlu 60°?", "Знаки верны, но величины перепутаны. Какая составляющая лежит у угла 60°?")),
            Opt(L("[[F_x]] = +5 kN, [[F_y]] = +8,66 kN", "[[F_x]] = +5 кН, [[F_y]] = +8,66 кН"), why = L("Velikosti sedí. Míří šipka nahoru, nebo dolů?", "Величины верны. Стрелка направлена вверх или вниз?")),
            Opt(L("[[F_x]] = −5 kN, [[F_y]] = −8,66 kN", "[[F_x]] = −5 кН, [[F_y]] = −8,66 кН"), why = L("Svisle je to dobře. Kam míří hrot vodorovně?", "По вертикали верно. Куда смотрит остриё по горизонтали?")),
        ),
        explain = L(
            "[[F_x]] leží u úhlu: 10 · cos 60° = 5 kN, doprava, tedy +. [[F_y]] = 10 · sin 60° = 8,66 kN, dolů, tedy −.",
            "[[F_x]] лежит у угла: 10 · cos 60° = 5 кН, вправо, значит +. [[F_y]] = 10 · sin 60° = 8,66 кН, вниз, значит −.",
        ),
    )
}

/** Karta 4 (volitelná): kvíz – cos 150°. */
@Composable
private fun CardScope.CheckCos150() {
    val t = tr()
    Stage(h = 300.0, description = t.pick(L("Jednotková kružnice s bodem na 150°", "Единичная окружность с точкой на 150°"))) { c ->
        unitCircle(150, c, numbers = false, showHandle = false, phiLabel = "150°")
    }
    Quiz(
        question = L("Kolik je cos 150°?", "Чему равен cos 150°?"),
        options = listOf(
            Opt(L("−0,866", "−0,866"), correct = true),
            Opt(L("0,866", "0,866"), why = L("Velikost sedí – k ose x chybí 30°. Na které straně od osy y bod leží?", "Величина верна — до оси x не хватает 30°. С какой стороны от оси y лежит точка?")),
            Opt(L("−0,5", "−0,5"), why = L("0,5 je svislá souřadnice bodu, tedy sin 150°. Která souřadnice je cos?", "0,5 — вертикальная координата точки, то есть sin 150°. Какая координата — cos?")),
            Opt(L("0,5", "0,5"), why = L("To je sin 150°. Kosinus je vodorovná souřadnice – a bod leží vlevo.", "Это sin 150°. Косинус — горизонтальная координата, а точка лежит слева.")),
        ),
        explain = L(
            "150° je 30° před osou −x: velikost cos 30° ≈ 0,866, bod leží vlevo, proto −0,866.",
            "150° — это 30° до оси −x: величина cos 30° ≈ 0,866, точка слева, поэтому −0,866.",
        ),
        columns = 2,
    )
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
