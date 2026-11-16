package cz.usharik.statika.learn.trig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cz.usharik.statika.learn.Card
import cz.usharik.statika.learn.CardScope
import cz.usharik.statika.learn.CardText
import cz.usharik.statika.learn.Chips
import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Lesson
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.learn.Quiz
import cz.usharik.statika.learn.Readout
import cz.usharik.statika.learn.SmallText
import cz.usharik.statika.learn.Stage
import cz.usharik.statika.learn.Todo
import cz.usharik.statika.learn.ValueSlider
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.circle
import cz.usharik.statika.learn.handle
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.protractor
import cz.usharik.statika.learn.rect
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.Prim
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Kurz trigonometrie – lekce 3 (id 103): stupně, úhloměr, kalkulačka (DEG / RAD / GRAD), radiány.
 */

/** Displej kalkulačky: výraz, hodnota a (volitelně) indikátor režimu. */
internal fun PrimBuilder.calcDisplay(expr: String, value: String, mode: String?, c: StatikaColors, valueColor: androidx.compose.ui.graphics.Color = c.ink) {
    rect(80.0, 25.0, 240.0, 150.0, c.subtle, c.ink, 2.0)
    rect(95.0, 40.0, 210.0, 90.0, c.paper, c.inkSoft, 1.0)
    if (mode != null) text(110.0, 58.0, mode, c.inkSoft, 11.0, Anchor.START, bold = true)
    text(290.0, 78.0, expr, c.ink, 15.0, Anchor.END)
    text(290.0, 116.0, value, valueColor, 24.0, Anchor.END, bold = true)
    // pár tlačítek pro dojem kalkulačky
    for (i in 0..4) rect(98.0 + i * 42, 140.0, 34.0, 24.0, c.paper, c.line, 1.0)
    text(115.0, 157.0, "sin", c.inkSoft, 11.0)
    text(157.0, 157.0, "cos", c.inkSoft, 11.0)
    text(199.0, 157.0, "tan", c.inkSoft, 11.0)
    text(241.0, 157.0, "SHIFT", c.inkSoft, 9.0)
    text(283.0, 157.0, "=", c.inkSoft, 12.0)
}

/** Karta 0: úhloměr – stupně, pravý a přímý úhel. */
@Composable
private fun CardScope.Protractor() {
    val t = tr()
    var deg by remember { mutableStateOf(30.0) }
    var r90 by remember { mutableStateOf(false) }
    var r45 by remember { mutableStateOf(false) }
    var r180 by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val cx = 200.0
    val cy = 200.0
    val rr = 160.0
    fun set(v: Double) {
        deg = ((v / 5).roundToInt() * 5.0).coerceIn(0.0, 180.0)
        when (deg) {
            90.0 -> r90 = true
            45.0 -> r45 = true
            180.0 -> r180 = true
        }
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
                set(atan2(cy - p.y, p.x - cx) * 180 / PI)
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        protractor(cx, cy, rr, c)
        line(cx, cy, cx + rr - 55, cy, c.ink, 2.6)
        line(cx, cy, hx, hy, c.load, 2.8)
        if (deg > 0) angleArc(cx, cy, 44.0, 0.0, -deg, "${deg.roundToInt()}°", c, c.load)
        circle(cx, cy, 4.0, c.ink)
        handle(hx, hy, c, dragging)
    }
    CardText(L(
        "Úhly měříme ve stupních: plný úhel (celá otáčka) má 360°, pravý úhel 90°. V pravoúhlém trojúhelníku dávají oba ostré úhly (menší než 90°) dohromady 90°.",
        "Углы измеряем в градусах: полный угол (оборот) — 360°, прямой угол — 90°. В прямоугольном треугольнике два острых угла (меньше 90°) вместе дают 90°.",
    ))
    Todo(
        L("Nastav pravý úhel, 90° (kroužkem nebo posuvníkem „Úhel“)", "Поставь прямой угол, 90° (кружком или ползунком «Угол»)") to r90,
        L("Nastav 45° – polovinu pravého úhlu", "Поставь 45° — половину прямого угла") to r45,
        L("Nastav 180° – přímý úhel", "Поставь 180° — развёрнутый угол") to r180,
    )
    ValueSlider(t.pick(L("Úhel = ${deg.roundToInt()}°", "Угол = ${deg.roundToInt()}°")), deg.toFloat(), 0f..180f, steps = 35) { set(it.toDouble()) }
}

/** Karta 1: past režimu kalkulačky. */
@Composable
private fun CardScope.CalcMode() {
    val t = tr()
    var mode by remember { mutableStateOf(0) } // 0 DEG, 1 RAD, 2 GRAD
    var rad by remember { mutableStateOf(false) }
    var grad by remember { mutableStateOf(false) }
    var back by remember { mutableStateOf(false) }
    val modes = listOf("DEG", "RAD", "GRAD")
    val values = listOf(0.5, sin(30.0), Trig.sinD(27.0))
    Stage(h = 190.0, description = t.pick(L("Displej kalkulačky se sin 30", "Дисплей калькулятора с sin 30"))) { c ->
        calcDisplay("sin(30)", num(values[mode], 3), modes[mode], c, if (mode == 0) c.accent else c.nudge)
    }
    CardText(L(
        "Kalkulačka bere úhel třemi způsoby: **DEG** = stupně (ty chceme), **RAD** = radiány (jiná jednotka, viz kartu 4), **GRAD** = grady (pravý úhel = 100 gradů; nepoužíváme). Stejné číslo 30 pak znamená něco jiného.",
        "Калькулятор понимает угол тремя способами: **DEG** — градусы (нам нужны они), **RAD** — радианы (другая единица, см. карточку 4), **GRAD** — грады (прямой угол = 100 град; не используем). Одно и то же число 30 тогда значит разное.",
    ))
    Todo(
        L("Přepni na „RAD“ – co ukáže sin 30?", "Переключи на «RAD» — что покажет sin 30?") to rad,
        L("Přepni na „GRAD“", "Переключи на «GRAD»") to grad,
        L("Vrať kalkulačku na „DEG“", "Верни калькулятор на «DEG»") to back,
    )
    Chips(listOf(L("DEG", "DEG"), L("RAD", "RAD"), L("GRAD", "GRAD")), mode) { i ->
        mode = i
        when (i) {
            1 -> rad = true
            2 -> grad = true
            else -> if (rad && grad) back = true
        }
    }
    Quiz(
        question = L("Který výsledek sin 30 je správně pro **stupně**?", "Какой результат sin 30 верен для **градусов**?"),
        options = listOf(
            Opt(L("0,5", "0,5"), correct = true),
            Opt(L("−0,988", "−0,988"), why = L("Tohle ukázal režim RAD – bere 30 jako 30 radiánů (skoro pět otáček).", "Это показал режим RAD — он считает 30 как 30 радиан (почти пять оборотов).")),
            Opt(L("0,454", "0,454"), why = L("Tohle ukázal režim GRAD – 30 gradů je jen 27°.", "Это показал режим GRAD — 30 град — это всего 27°.")),
        ),
        explain = L(
            "Ve stupních je sin 30° = 0,5. Rychlá zkouška před počítáním: zadej sin 30 – když nevyjde 0,5, je kalkulačka ve špatném režimu.",
            "В градусах sin 30° = 0,5. Быстрая проверка перед расчётом: набери sin 30 — если получилось не 0,5, калькулятор не в том режиме.",
        ),
        columns = 3,
    )
    SmallText(L(
        "Na telefonu: otevři Kalkulačku a otoč telefon na šířku (iPhone) nebo rozbal panel funkcí (Android) – objeví se sin, cos, tan. Režim přepíná tlačítko Deg/Rad. Někde se píše nejdřív číslo a pak sin, jinde obráceně – vyzkoušej si to.",
        "На телефоне: открой Калькулятор и поверни телефон горизонтально (iPhone) или разверни панель функций (Android) — появятся sin, cos, tan. Режим меняет кнопка Deg/Rad. Где-то сначала вводят число, а потом sin, где-то наоборот — попробуй.",
    ))
}

/** Karta 2: kvíz – záporný výsledek u ostrého úhlu. */
@Composable
private fun CardScope.CheckRad() {
    val t = tr()
    Stage(h = 190.0, description = t.pick(L("Kalkulačka ukazuje 10 · sin 30 = −9,88", "Калькулятор показывает 10 · sin 30 = −9,88"))) { c ->
        calcDisplay("10 × sin(30)", "−9,88", null, c, c.nudge)
    }
    Quiz(
        question = L("Počítáš 10 · sin 30° a kalkulačka ukáže −9,88. Co se stalo?", "Ты считаешь 10 · sin 30°, а калькулятор показывает −9,88. Что случилось?"),
        options = listOf(
            Opt(L("Kalkulačka je v režimu RAD", "Калькулятор в режиме RAD"), correct = true),
            Opt(L("Nic, sinus bývá záporný", "Ничего, синус бывает отрицательным"), why = L("Pro úhly mezi 0° a 90° je sinus vždy mezi 0 a 1. Kolik má vyjít sin 30°?", "Для углов от 0° до 90° синус всегда между 0 и 1. Сколько должно получиться для sin 30°?")),
            Opt(L("Mělo se počítat s cos", "Надо было считать через cos"), why = L("10 · cos 30° = 8,66 – taky kladné číslo. Co na kalkulačce rozhoduje, jak rozumí číslu 30?", "10 · cos 30° = 8,66 — тоже положительное число. Что на калькуляторе решает, как он понимает число 30?")),
            Opt(L("Chyba je v násobení deseti", "Ошибка в умножении на десять"), why = L("Zkus jen sin 30: vyjde −0,988 místo 0,5. Chyba je už tam.", "Попробуй просто sin 30: выйдет −0,988 вместо 0,5. Ошибка уже там.")),
        ),
        explain = L(
            "Záporný nebo podivný výsledek u ostrého úhlu skoro vždy znamená režim RAD. Přepni na DEG a ověř, že sin 30 = 0,5.",
            "Отрицательный или странный результат для острого угла почти всегда означает режим RAD. Переключи на DEG и проверь, что sin 30 = 0,5.",
        ),
    )
}

/** Karta 3 (volitelná): radián jako délka oblouku jednotkové kružnice. */
@Composable
private fun CardScope.Radians() {
    val t = tr()
    var deg by remember { mutableStateOf(60.0) }
    var r180 by remember { mutableStateOf(false) }
    val cx = 200.0
    val cy = 115.0
    val rr = 85.0
    val rad = deg * PI / 180
    Stage(h = 230.0, description = t.pick(L("Kružnice s poloměrem 1 a vyznačeným obloukem", "Окружность радиуса 1 с выделенной дугой"))) { c ->
        circle(cx, cy, rr, null, c.inkSoft, 1.4)
        line(cx, cy, cx + rr, cy, c.ink, 2.0)
        line(cx, cy, cx + rr * cos(rad), cy - rr * sin(rad), c.ink, 2.0)
        if (deg > 0) prims += Prim.Arc(cx, cy, rr, 0.0, -deg, c.derived, 5.0)
        if (deg > 0) angleArc(cx, cy, 24.0, 0.0, -deg, null, c)
        text(cx + rr / 2, cy + 18, "1", c.ink, 14.0, bold = true)
        val mid = -deg / 2 * PI / 180
        if (deg > 0) text(cx + (rr + 24) * cos(mid), cy + (rr + 24) * sin(mid) + 5, "${num(rad)}", c.derived, 14.0, bold = true)
        circle(cx, cy, 3.5, c.ink)
    }
    CardText(L(
        "Radián je jednotka **obloukové míry**: úhel měříme délkou oblouku na kružnici s poloměrem 1 – kolik poloměrů ujdeš po obvodu. Půlkružnice má délku π ≈ 3,14 (π je číslo z obvodu kruhu), proto 180° = π rad.",
        "Радиан — единица **радианной меры**: угол измеряют длиной дуги на окружности радиуса 1 — сколько радиусов пройдёшь по окружности. Полуокружность имеет длину π ≈ 3,14 (π — число из длины окружности), поэтому 180° = π рад.",
    ))
    Todo(L("Nastav posuvníkem „Úhel“ 180° – kolik je to radiánů?", "Поставь ползунком «Угол» 180° — сколько это радиан?") to r180)
    ValueSlider(t.pick(L("Úhel = ${deg.roundToInt()}°", "Угол = ${deg.roundToInt()}°")), deg.toFloat(), 0f..360f, steps = 23) {
        deg = ((it / 15).roundToInt() * 15.0).coerceIn(0.0, 360.0)
        if (deg == 180.0) r180 = true
    }
    Readout(if (deg == 180.0) "180° = π rad ≈ 3,14 rad" else "${deg.roundToInt()}° = ${num(rad)} rad")
    SmallText(L(
        "Ve statice na průmyslovce počítáme ve stupních. Radiány stačí poznat – a umět je na kalkulačce vypnout.",
        "В статике в техникуме считаем в градусах. Радианы достаточно узнавать — и уметь выключить их на калькуляторе.",
    ))
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
