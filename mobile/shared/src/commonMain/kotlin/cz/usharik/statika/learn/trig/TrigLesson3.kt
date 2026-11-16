package cz.usharik.statika.learn.trig

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cz.usharik.statika.learn.ActionButton
import cz.usharik.statika.learn.Card
import cz.usharik.statika.learn.CardScope
import cz.usharik.statika.learn.CardText
import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Lesson
import cz.usharik.statika.learn.Note
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.learn.SmallText
import cz.usharik.statika.learn.Stage
import cz.usharik.statika.learn.Step
import cz.usharik.statika.learn.Steps
import cz.usharik.statika.learn.ValueSlider
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.circle
import cz.usharik.statika.learn.handle
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.protractor
import cz.usharik.statika.learn.quizStep
import cz.usharik.statika.learn.rect
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Kurz trigonometrie – lekce 3 (id 103): stupně, úhloměr, kalkulačka (DEG / RAD / GRAD), radiány.
 */

/** Displej kalkulačky: výraz, hodnota a (volitelně) indikátor režimu. Bez tlačítek – nic z toho se nemačká. */
internal fun PrimBuilder.calcDisplay(expr: String, value: String, mode: String?, c: StatikaColors, valueColor: androidx.compose.ui.graphics.Color = c.ink) {
    rect(80.0, 20.0, 240.0, 120.0, c.subtle, c.ink, 2.0)
    rect(95.0, 35.0, 210.0, 90.0, c.paper, c.inkSoft, 1.0)
    if (mode != null) text(110.0, 53.0, mode, c.inkSoft, 11.0, Anchor.START, bold = true)
    text(290.0, 73.0, expr, c.ink, 15.0, Anchor.END)
    text(290.0, 111.0, value, valueColor, 24.0, Anchor.END, bold = true)
}

private val MODES = listOf("DEG", "RAD", "GRAD")

/** sin / cos čísla [x] tak, jak ho chápe kalkulačka v režimu [mode] (0 DEG, 1 RAD, 2 GRAD) */
private fun calcSin(x: Double, mode: Int) = when (mode) { 0 -> Trig.sinD(x); 1 -> sin(x); else -> Trig.sinD(x * 0.9) }

/** Karta 0: úhloměr – úhel jako otočení od výchozího ramene. */
@Composable
private fun CardScope.Protractor() {
    val t = tr()
    var deg by remember { mutableStateOf(30.0) }
    var r90 by remember { mutableStateOf(false) }
    var r45 by remember { mutableStateOf(false) }
    var r180 by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<L?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val cx = 200.0
    val cy = 200.0
    val rr = 160.0
    fun set(v: Double) {
        deg = ((v / 5).roundToInt() * 5.0).coerceIn(0.0, 180.0)
        msg = null
    }
    val a = deg * PI / 180
    val hx = cx + (rr - 55) * cos(a)
    val hy = cy - (rr - 55) * sin(a)
    Stage(
        h = 225.0,
        description = t.pick(L("Úhloměr s ramenem, které se dá natočit", "Транспортир с поворачивающимся лучом")),
        onDrag = { p, start ->
            if (!start || p.dist(hx, hy) < 50) {
                dragging = true
                // pod základnou: vlevo = 180°, vpravo = 0° (žádný skok z 180° na 0°)
                val raw = atan2(cy - p.y, p.x - cx) * 180 / PI
                set(if (raw < -90) 180.0 else if (raw < 0) 0.0 else raw)
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        protractor(cx, cy, rr, c)
        line(cx, cy, cx + rr - 55, cy, c.ink, 3.0)
        text(cx + rr - 50, cy + 18, t.pick(L("výchozí rameno (0°)", "начальный луч (0°)")), c.ink, 12.0, Anchor.MIDDLE)
        line(cx, cy, hx, hy, c.load, 2.8)
        if (deg > 0 && deg < 180) angleArc(cx, cy, 44.0, 0.0, -deg, "${deg.roundToInt()}°", c, c.load)
        if (deg == 180.0) {
            prims += Prim.Arc(cx, cy, 44.0, 0.0, -180.0, c.load, 1.4)
            text(cx, cy - 52, "180°", c.load, 13.0)
        }
        circle(cx, cy, 4.0, c.ink)
        handle(hx, hy, c, dragging)
    }
    CardText(L(
        "Úhel měří, o kolik se rameno otočilo od výchozího ramene. Celá otáčka má 360°.",
        "Угол показывает, насколько луч повернулся от начального луча. Полный оборот равен 360°.",
    ))
    val tools: @Composable (target: Double, onOk: () -> Unit, miss: L) -> Unit = { target, onOk, miss ->
        ValueSlider(t.pick(L("Úhel = ${deg.roundToInt()}°", "Угол = ${deg.roundToInt()}°")), deg.toFloat(), 0f..180f, steps = 35) { set(it.toDouble()) }
        ActionButton(BTN_CHECK) { if (deg == target) onOk() else msg = miss }
        msg?.let { Note(Tone.NUDGE, it) }
    }
    Steps(
        Step(
            L("Otoč rameno o čtvrt otáčky", "Поверни луч на четверть оборота"), r90,
            result = { Note(Tone.OK, L("Čtvrt otáčky = 360° : 4 = **90°**, pravý úhel. Rameno stojí kolmo k výchozímu.", "Четверть оборота = 360° : 4 = **90°**, прямой угол. Луч перпендикулярен начальному.")) },
        ) {
            tools(90.0, { r90 = true; msg = null }, L("Celá otáčka má 360°. Kolik je její čtvrtina? Rameno pak stojí svisle.", "Полный оборот — 360°. Сколько его четверть? Луч тогда стоит вертикально."))
        },
        Step(
            L("Najdi polovinu pravého úhlu", "Найди половину прямого угла"), r45,
            result = { Note(Tone.OK, L("**45°** – rameno je přesně uprostřed mezi 0° a 90°.", "**45°** — луч ровно посередине между 0° и 90°.")) },
        ) {
            tools(45.0, { r45 = true; msg = null }, L("Pravý úhel má 90°. Rameno má být přesně v půli mezi 0° a 90°.", "Прямой угол — 90°. Луч должен быть ровно посередине между 0° и 90°."))
        },
        Step(
            L("Otoč rameno do opačného směru, než míří výchozí", "Поверни луч в сторону, противоположную начальному"), r180,
            result = { Note(Tone.OK, L("Půl otáčky = **180°**, přímý úhel. Obě ramena leží na jedné přímce.", "Половина оборота = **180°**, развёрнутый угол. Оба луча лежат на одной прямой.")) },
        ) {
            tools(180.0, { r180 = true; msg = null }, L("Opačný směr je půl otáčky – rameno míří přesně doleva.", "Противоположное направление — это пол-оборота: луч смотрит ровно влево."))
        },
    )
}

/** Karta 1: past režimu kalkulačky – nejdřív tip, pak zkouška v DEG / RAD / GRAD. */
@Composable
private fun CardScope.CalcMode() {
    val t = tr()
    var guess by remember { mutableStateOf<Int?>(null) }
    var mode by remember { mutableStateOf(1) } // kalkulačka začíná v RAD
    var degOk by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<L?>(null) }
    val guesses = listOf(L("0,5", "0,5"), L("−0,988", "−0,988"), L("0,454", "0,454"))
    val cosQ = quizStep(
        question = L("Kalkulačka je v DEG. Co ukáže cos 60?", "Калькулятор в DEG. Что он покажет для cos 60?"),
        options = listOf(
            Opt(L("0,5", "0,5"), correct = true),
            Opt(L("0,866", "0,866"), why = L("0,866 je sin 60° – výška strmé rampy z lekce 1. Kosinus 60° patří k vodorovné odvěsně, a ta je kratší.", "0,866 — это sin 60°, высота крутого пандуса из урока 1. Косинус 60° относится к горизонтальному катету, а он короче.")),
            Opt(L("−0,952", "−0,952"), why = L("Tohle by ukázal režim RAD (60 radiánů). Kosinus ostrého úhlu je kladný.", "Это показал бы режим RAD (60 радиан). Косинус острого угла положителен.")),
        ),
        explain = L(
            "cos 60° = 0,5. Pro úhly zadané ve stupních používej DEG. RAD ani GRAD nejsou chybné režimy – jen používají jiné jednotky úhlu. Rychlá zkouška: sin 30 musí dát 0,5.",
            "cos 60° = 0,5. Для углов в градусах нужен DEG. RAD и GRAD — не «неправильные» режимы: они просто используют другие единицы угла. Быстрая проверка: sin 30 должен дать 0,5.",
        ),
        columns = 3,
    )
    Stage(h = 160.0, description = t.pick(L("Displej kalkulačky", "Дисплей калькулятора"))) { c ->
        when {
            guess == null -> calcDisplay("sin(30)", "?", null, c, c.inkSoft)
            !degOk -> calcDisplay("sin(30)", num(calcSin(30.0, mode), 3), MODES[mode], c, if (mode == 0) c.accent else c.nudge)
            else -> calcDisplay("cos(60)", if (cosQ.done) "0,5" else "?", "DEG", c, if (cosQ.done) c.accent else c.inkSoft)
        }
    }
    CardText(L(
        "Číslo 30 samo neurčuje jednotku úhlu. Kalkulačka ji převezme z režimu: **DEG** = stupně, **RAD** = radiány, **GRAD** = grady (pravý úhel = 100 gradů).",
        "Само число 30 не задаёт единицу угла. Калькулятор берёт её из режима: **DEG** — градусы, **RAD** — радианы, **GRAD** — грады (прямой угол = 100 град).",
    ))
    Steps(
        guessStep(L("Tipni si: kolik má vyjít sin 30°?", "Предскажи: чему должен быть равен sin 30°?"), guesses, guess) { guess = it },
        Step(
            L("Přepínej režimy a vyber ten, který počítá ve stupních", "Переключай режимы и выбери тот, что считает в градусах"), degOk,
            result = {
                Note(if (guess == 0) Tone.OK else Tone.INFO, L(
                    "V DEG je sin 30° = 0,5. " + if (guess == 0) "Tip sedí." else "Tip ${guesses[guess ?: 1].cs} ukazuje kalkulačka v jiném režimu: RAD bere 30 jako 30 radiánů, GRAD jako 30 gradů = 27°.",
                    "В DEG sin 30° = 0,5. " + if (guess == 0) "Прогноз верен." else "Значение ${guesses[guess ?: 1].ru} калькулятор показывает в другом режиме: RAD понимает 30 как 30 радиан, GRAD — как 30 град = 27°.",
                ))
            },
        ) {
            PickCheck(MODES.map { L(it, it) }, mode, { mode = it; msg = null }, BTN_TEST, msg) {
                msg = when (mode) {
                    0 -> { degOk = true; null }
                    1 -> L("RAD bere 30 jako 30 radiánů – skoro pět otáček. Proto −0,988.", "RAD понимает 30 как 30 радиан — почти пять оборотов. Отсюда −0,988.")
                    else -> L("GRAD: pravý úhel má 100 gradů, takže 30 gradů je jen 27°. Proto 0,454.", "GRAD: прямой угол — 100 град, поэтому 30 град — это всего 27°. Отсюда 0,454.")
                }
            }
        },
        cosQ,
    )
}

/** Karta 2: záporný výsledek u ostrého úhlu – najdi příčinu a oprav ji. */
@Composable
private fun CardScope.CheckRad() {
    val t = tr()
    var sel by remember { mutableStateOf(1) } // 0 DEG, 1 RAD
    var shown by remember { mutableStateOf(1) } // režim, ve kterém kalkulačka naposled počítala
    var fixed by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<L?>(null) }
    val why = quizStep(
        question = L("Počítáš 10 · sin 30° a kalkulačka ukáže −9,88. Co se stalo?", "Ты считаешь 10 · sin 30°, а калькулятор показывает −9,88. Что случилось?"),
        options = listOf(
            Opt(L("Režim RAD", "Режим RAD"), correct = true),
            Opt(L("Je to správně", "Всё верно"), why = L("Pro úhly mezi 0° a 90° je sinus vždy mezi 0 a 1. Kolik má vyjít sin 30°?", "Для углов от 0° до 90° синус всегда между 0 и 1. Сколько должно получиться для sin 30°?")),
            Opt(L("Mělo být cos", "Нужен был cos"), why = L("10 · cos 30° ≈ 8,66 – taky kladné číslo. Co na kalkulačce rozhoduje, jak rozumí číslu 30?", "10 · cos 30° ≈ 8,66 — тоже положительное число. Что на калькуляторе решает, как он понимает число 30?")),
            Opt(L("Chyba v násobení", "Ошибка в умножении"), why = L("Zkus jen sin 30: vyjde −0,988 místo 0,5. Chyba je už tam.", "Попробуй просто sin 30: выйдет −0,988 вместо 0,5. Ошибка уже там.")),
        ),
        explain = L(
            "Kalkulačka četla 30 jako 30 radiánů: sin(30 rad) ≈ −0,988, krát 10 je −9,88.",
            "Калькулятор понял 30 как 30 радиан: sin(30 рад) ≈ −0,988, умножить на 10 — −9,88.",
        ),
        columns = 2,
    )
    Stage(h = 160.0, description = t.pick(L("Kalkulačka ukazuje 10 · sin 30 = −9,88", "Калькулятор показывает 10 · sin 30 = −9,88"))) { c ->
        val v = 10 * calcSin(30.0, shown)
        calcDisplay("10 × sin(30)", num(v, 2), if (why.done) MODES[shown] else null, c, if (shown == 0) c.accent else c.nudge)
    }
    CardText(L(
        "Pro ostrý úhel je sinus kladný. Záporný výsledek je důvod zkontrolovat zadání i jednotku úhlu.",
        "Синус острого угла положителен. Отрицательный результат — повод проверить ввод и единицу угла.",
    ))
    Steps(
        why,
        Step(
            L("Oprav režim a přepočítej výraz", "Исправь режим и пересчитай выражение"), fixed,
            result = {
                Note(Tone.OK, L(
                    "Ve stupních je 10 · sin 30° = 10 · 0,5 = 5. Když výsledek nesedí s odhadem, kontroluj také znaménka, závorky a to, co jsi opravdu zadal(a).",
                    "В градусах 10 · sin 30° = 10 · 0,5 = 5. Если результат не сходится с ожиданием, проверяй также знаки, скобки и то, что ты на самом деле ввёл(а).",
                ))
            },
        ) {
            PickCheck(listOf(L("DEG", "DEG"), L("RAD", "RAD")), sel, { sel = it; msg = null }, L("Přepočítat", "Пересчитать"), msg) {
                shown = sel
                if (sel == 0) { fixed = true; msg = null } else msg = L("Pořád RAD – výsledek je zase −9,88.", "Всё ещё RAD — снова −9,88.")
            }
        },
    )
}

/** Karta 3: radián – oblouk dlouhý jako poloměr; půl otáčky = π, celá = 2π. */
@Composable
private fun CardScope.Radians() {
    val t = tr()
    val wrap = remember { Animatable(0f) }
    var demoDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(700)
        wrap.animateTo(1f, tween(2200, easing = FastOutSlowInEasing))
        delay(400)
        demoDone = true
    }
    val halfQ = quizStep(
        question = L("Kolik radiánů má půl otáčky (180°)?", "Сколько радиан в половине оборота (180°)?"),
        options = listOf(
            Opt(L("π", "π"), correct = true),
            Opt(L("2π", "2π"), why = L("Na půlkružnici se vejde asi 3,14 poloměru – spočítej dílky na obrázku. 2π ≈ 6,28 by byla celá kružnice.", "На полуокружность укладывается примерно 3,14 радиуса — посчитай деления на рисунке. 2π ≈ 6,28 — это целая окружность.")),
            Opt(L("π/2", "π/2"), why = L("π/2 ≈ 1,57 je jen čtvrt otáčky. Kolik poloměrů se vejde na půlkružnici?", "π/2 ≈ 1,57 — это только четверть оборота. Сколько радиусов укладывается на полуокружность?")),
        ),
        explain = L("Na půlkružnici se vejde π ≈ 3,14 poloměru, takže 180° = π rad.", "На полуокружность укладывается π ≈ 3,14 радиуса, поэтому 180° = π рад."),
        columns = 3,
    )
    val fullQ = quizStep(
        question = L("Kolik radiánů má celá otáčka (360°)?", "Сколько радиан в полном обороте (360°)?"),
        options = listOf(
            Opt(L("π", "π"), why = L("π je jen půl otáčky. Celá otáčka jsou dvě půlky.", "π — это только половина оборота. Полный оборот — две половины.")),
            Opt(L("2π", "2π"), correct = true),
            Opt(L("360", "360"), why = L("360 je počet stupňů. V radiánech počítáme poloměry na oblouku.", "360 — это число градусов. В радианах считаем радиусы на дуге.")),
        ),
        explain = L(
            "Celá otáčka je 2π rad ≈ 6,28 rad = 360°. Jednotka úhlu a režim kalkulačky musí souhlasit.",
            "Полный оборот — 2π рад ≈ 6,28 рад = 360°. Единица угла должна соответствовать режиму калькулятора.",
        ),
        columns = 3,
    )
    val cx = 200.0
    val cy = 118.0
    val rr = 80.0
    // jak velký oblouk ukázat: ukázka 1 rad, pak půl otáčky, pak celá
    val sweepDeg = when {
        !demoDone -> wrap.value * 180 / PI
        !halfQ.done -> 180.0
        else -> 360.0
    }
    Stage(h = 235.0, description = t.pick(L("Kružnice a oblouk měřený poloměry", "Окружность и дуга, измеренная радиусами"))) { c ->
        circle(cx, cy, rr, null, c.line, 1.2)
        line(cx, cy, cx + rr, cy, c.ink, 2.0)
        text(cx + rr / 2, cy + 18, "r", c.ink, 14.0, bold = true)
        if (sweepDeg > 0.5) prims += Prim.Arc(cx, cy, rr, 0.0, -sweepDeg, c.derived, 5.0)
        val endA = sweepDeg * PI / 180
        if (!demoDone) {
            // zbytek poloměru, který se teprve „namotá“: rovně po tečně z konce oblouku
            val ex = cx + rr * cos(endA)
            val ey = cy - rr * sin(endA)
            val rest = rr * (1 - wrap.value)
            line(ex, ey, ex - rest * sin(endA), ey - rest * cos(endA), c.derived, 5.0)
            if (wrap.value >= 0.999f) {
                line(cx, cy, ex, ey, c.ink, 2.0)
                angleArc(cx, cy, 22.0, 0.0, -sweepDeg, null, c)
                text(cx + (rr + 22) * cos(endA / 2), cy - (rr + 22) * sin(endA / 2), t.pick(L("s = r", "s = r")), c.derived, 14.0, Anchor.START, bold = true)
                text(cx, cy + rr + 28, t.pick(L("1 rad ≈ 57,3°", "1 рад ≈ 57,3°")), c.derived, 15.0, bold = true)
            }
        } else {
            if (sweepDeg < 359.0) line(cx, cy, cx - rr, cy, c.ink, 2.0)
            // dílky po 1 rad – kolik poloměrů se vejde na oblouk
            var k = 1
            while (k * 180 / PI <= sweepDeg) {
                val ang = k.toDouble()
                line(cx + (rr - 7) * cos(ang), cy - (rr - 7) * sin(ang), cx + (rr + 7) * cos(ang), cy - (rr + 7) * sin(ang), c.ink, 1.6)
                text(cx + (rr + 18) * cos(ang), cy - (rr + 18) * sin(ang) + 5, "$k", c.inkSoft, 12.0)
                k++
            }
            val half = !halfQ.done
            val solved = if (half) halfQ.done else fullQ.done
            val label = if (half) (if (solved) "180° = π rad" else "180° = ? rad") else (if (solved) "360° = 2π rad" else "360° = ? rad")
            text(cx, cy + rr + 30, label, c.derived, 15.0, bold = true)
        }
        circle(cx, cy, 3.5, c.ink)
    }
    CardText(L(
        "V radiánech měříme úhel poměrem délky oblouku s k poloměru r: φ = s / r. Kolik poloměrů se vejde na oblouk, tolik je radiánů.",
        "В радианах угол измеряют отношением длины дуги s к радиусу r: φ = s / r. Сколько радиусов укладывается на дугу, столько и радиан.",
    ))
    Steps(
        Step(
            L("Sleduj oblouk dlouhý přesně jako poloměr", "Проследи за дугой длиной ровно в один радиус"), demoDone,
            result = { Note(Tone.INFO, L("Oblouk dlouhý jako poloměr odpovídá úhlu **1 rad ≈ 57,3°**.", "Дуге длиной в радиус соответствует угол **1 рад ≈ 57,3°**.")) },
        ) {
            SmallText(L("Poloměr se pokládá na kružnici…", "Радиус укладывается на окружность…"))
        },
        halfQ,
        fullQ,
    )
}

internal fun trigLesson3() = Lesson(
    id = 103,
    title = L("Stupně a kalkulačka", "Градусы и калькулятор"),
    summary = L("Jak se měří úhel a proč musí na kalkulačce svítit DEG.", "Как измеряют угол и почему на калькуляторе должно гореть DEG."),
    cards = listOf(
        Card { Protractor() },
        Card { CalcMode() },
        Card { CheckRad() },
        Card { Radians() },
    ),
)
