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
import cz.usharik.statika.learn.Note
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.learn.Pt
import cz.usharik.statika.learn.Quiz
import cz.usharik.statika.learn.Readout
import cz.usharik.statika.learn.SmallText
import cz.usharik.statika.learn.Stage
import cz.usharik.statika.learn.Todo
import cz.usharik.statika.learn.ValueSlider
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.handle
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.poly
import cz.usharik.statika.learn.rightAngleMark
import cz.usharik.statika.learn.screenAngle
import cz.usharik.statika.learn.tapSpot
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/*
 * Kurz trigonometrie – lekce 1 (id 101): odkud se vzal sinus. Podobné trojúhelníky → podíl stran závisí jen na úhlu;
 * jména stran pravoúhlého trojúhelníku.
 */

/** Pravoúhlý trojúhelník „rampa“: úhel α vlevo dole v (x0, y0), přepona délky [hyp] (jednotky scény). */
internal class Ramp(val x0: Double, val y0: Double, val hyp: Double, val alpha: Double) {
    val a = Pt(x0, y0)
    val c = Pt(x0 + hyp * Trig.cosD(alpha), y0)
    val b = Pt(c.x, y0 - hyp * Trig.sinD(alpha))
}

/** Nakreslí rampu: plocha, přepona červeně, odvěsny fialově, značka pravého úhlu, oblouk α. */
internal fun PrimBuilder.rampTri(r: Ramp, c: StatikaColors, angleLabel: String?, alpha: Float = 1f, labels: Boolean = true, hypLabel: String? = null, oppLabel: String? = null) {
    poly(listOf(r.a.x to r.a.y, r.b.x to r.b.y, r.c.x to r.c.y), c.subtle, null, 0.0, alpha = 0.6f * alpha)
    line(r.a.x, r.a.y, r.c.x, r.c.y, c.derived, 2.4, alpha = alpha)
    line(r.c.x, r.c.y, r.b.x, r.b.y, c.derived, 2.4, alpha = alpha)
    line(r.a.x, r.a.y, r.b.x, r.b.y, c.load, 2.6, alpha = alpha)
    if (alpha < 0.9f) return
    if (abs(r.b.y - r.c.y) > 14) rightAngleMark(r.c, r.a, r.b, c)
    if (angleLabel != null) angleArc(r.a.x, r.a.y, 30.0, 0.0, -r.alpha, angleLabel, c)
    if (labels) {
        oppLabel?.let { text(r.c.x + 8, (r.c.y + r.b.y) / 2 + 5, it, c.derived, 13.0, Anchor.START) }
        hypLabel?.let {
            // popisek přepony nad její střed, odsunutý kolmo ven (doleva nahoru)
            val mx = (r.a.x + r.b.x) / 2
            val my = (r.a.y + r.b.y) / 2
            val s = Trig.sinD(r.alpha)
            val co = Trig.cosD(r.alpha)
            text(mx - 24 * s - 6, my - 24 * co, it, c.load, 13.0, Anchor.END)
        }
    }
}

private const val RAMP_PX = 100.0

/** Karta 0: podobné trojúhelníky – podíl výška / šikmá délka nezávisí na velikosti. */
@Composable
private fun CardScope.SameAngle() {
    val t = tr()
    var k by remember { mutableStateOf(1.0) }
    var grown by remember { mutableStateOf(false) }
    var shrunk by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    fun setK(v: Double) {
        k = ((v * 2).roundToInt() / 2.0).coerceIn(1.0, 3.0)
        if (k >= 2.0) grown = true
        if (grown && k == 1.0) shrunk = true
    }
    val r = Ramp(40.0, 205.0, RAMP_PX * k, 30.0)
    Stage(
        h = 230.0,
        description = t.pick(L("Nájezdová rampa – pravoúhlý trojúhelník s úhlem 30°", "Пандус — прямоугольный треугольник с углом 30°")),
        onDrag = { p, start ->
            if (!start || p.dist(r.b.x, r.b.y) < 50) {
                dragging = true
                // průmět prstu na směr přepony
                val along = (p.x - 40.0) * Trig.cosD(30.0) - (p.y - 205.0) * Trig.sinD(30.0)
                setK(along / RAMP_PX)
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        line(10.0, 205.0, 390.0, 205.0, c.ink, 1.4)
        if (k > 1.0) rampTri(Ramp(40.0, 205.0, RAMP_PX, 30.0), c, null, alpha = 0.3f)
        rampTri(
            r, c, "30°",
            hypLabel = t.pick(L("šikmá délka ${num(2 * k)} m", "длина по склону ${num(2 * k)} м")),
            oppLabel = t.pick(L("výška ${num(k)} m", "высота ${num(k)} м")),
        )
        handle(r.b.x, r.b.y, c, dragging)
    }
    CardText(L(
        "Nájezdová rampa stoupá pod úhlem 30°. Táhni za kroužek: rampa se prodlužuje, úhel zůstává.",
        "Пандус поднимается под углом 30°. Потяни за кружок: пандус удлиняется, а угол остаётся прежним.",
    ))
    Todo(
        L("Prodluž rampu (kroužkem nebo posuvníkem „Délka rampy“) aspoň na dvojnásobek a sleduj podíl **výška / šikmá délka**",
            "Удлини пандус (кружком или ползунком «Длина пандуса») хотя бы вдвое и следи за отношением **высота / длина по склону**") to grown,
        L("Zkrať ji zpátky na nejkratší – změnil se podíl?", "Укороти его обратно до самого короткого — изменилось ли отношение?") to shrunk,
    )
    ValueSlider(t.pick(L("Délka rampy", "Длина пандуса")), k.toFloat(), 1f..3f, steps = 3) { setK(it.toDouble()) }
    Readout(t.pick(L("výška / šikmá délka", "высота / длина по склону")) + " = ${num(k)} / ${num(2 * k)} = 0,5")
    if (grown && shrunk) SmallText(L(
        "Trojúhelníky se stejnými úhly jsou **podobné**: všechny strany se zvětší stejněkrát, takže jejich podíl se nezmění.",
        "Треугольники с одинаковыми углами **подобны**: все стороны увеличиваются в одно и то же число раз, поэтому их отношение не меняется.",
    ))
}

/** Karta 1: podíl závisí jen na úhlu – to je sin α. */
@Composable
private fun CardScope.AngleSetsRatio() {
    val t = tr()
    var alpha by remember { mutableStateOf(20.0) }
    var k by remember { mutableStateOf(1.0) }
    var at30 by remember { mutableStateOf(false) }
    var at60len by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    fun setAlpha(v: Double) {
        alpha = ((v / 5).roundToInt() * 5.0).coerceIn(10.0, 80.0)
        if (alpha == 30.0) at30 = true
    }
    fun setK(v: Double) {
        val nk = ((v * 2).roundToInt() / 2.0).coerceIn(1.0, 2.5)
        if (nk != k && alpha == 60.0) at60len = true
        k = nk
    }
    val r = Ramp(50.0, 210.0, 75.0 * k, alpha)
    val ratio = Trig.sinD(alpha)
    Stage(
        h = 230.0,
        description = t.pick(L("Rampa s nastavitelným úhlem a délkou", "Пандус с изменяемым углом и длиной")),
        onDrag = { p, start ->
            if (!start || p.dist(r.b.x, r.b.y) < 50) {
                dragging = true
                setK(hypot(p.x - 50.0, p.y - 210.0) / 75.0)
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        line(10.0, 210.0, 390.0, 210.0, c.ink, 1.4)
        rampTri(
            r, c, "${alpha.roundToInt()}°",
            hypLabel = t.pick(L("${num(2 * k)} m", "${num(2 * k)} м")),
            oppLabel = t.pick(L("výška ${num(2 * k * ratio)} m", "высота ${num(2 * k * ratio)} м")),
        )
        handle(r.b.x, r.b.y, c, dragging)
    }
    CardText(L(
        "Teď měň úhel. Podíl výška / šikmá délka závisí **jen na úhlu** – ne na tom, jak je rampa velká.",
        "Теперь меняй угол. Отношение высота / длина по склону зависит **только от угла**, а не от размера пандуса.",
    ))
    Todo(
        L("Nastav posuvníkem „Úhel α“ 30° – jaký je podíl?", "Поставь ползунком «Угол α» 30° — какое получилось отношение?") to at30,
        L("Nastav α = 60° a změň délku rampy (kroužkem nebo posuvníkem „Délka rampy“) – podíl se nezmění",
            "Поставь α = 60° и измени длину пандуса (кружком или ползунком «Длина пандуса») — отношение не сдвинется") to at60len,
    )
    ValueSlider(t.pick(L("Úhel α = ${alpha.roundToInt()}°", "Угол α = ${alpha.roundToInt()}°")), alpha.toFloat(), 10f..80f, steps = 13) { setAlpha(it.toDouble()) }
    ValueSlider(t.pick(L("Délka rampy", "Длина пандуса")), k.toFloat(), 1f..2.5f, steps = 2) { setK(it.toDouble()) }
    Readout(t.pick(L("výška / šikmá délka", "высота / длина по склону")) + " = ${num(2 * k * ratio)} / ${num(2 * k)} = ${num(ratio, 3)}")
    if (at30 && at60len) SmallText(L(
        "Tomuhle číslu se říká **sinus** úhlu α, píšeme sin α. Sinus 30° je 0,5 v každém pravoúhlém trojúhelníku na světě.",
        "Это число называют **синусом** угла α, пишут sin α. Синус 30° равен 0,5 в любом прямоугольном треугольнике на свете.",
    ))
}

/** Trojúhelník karet o jménech stran: α vlevo dole, pravý úhel vpravo dole. */
private val NAME_A = Pt(80.0, 190.0)
private val NAME_C = Pt(300.0, 190.0)
private val NAME_B = Pt(300.0, 50.0)

private fun mid(p: Pt, q: Pt) = Pt((p.x + q.x) / 2, (p.y + q.y) / 2)

/** Karta 2: jména stran; přilehlá / protilehlá podle toho, ze kterého úhlu se díváme. */
@Composable
private fun CardScope.NameSides() {
    val t = tr()
    var beta by remember { mutableStateOf(false) }
    var hypOk by remember { mutableStateOf(false) }
    var adjOk by remember { mutableStateOf(false) }
    var betaOk by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<L?>(null) }
    val spots = mapOf(
        TriSide.HYP to mid(NAME_A, NAME_B),
        TriSide.ADJ to mid(NAME_A, NAME_C),
        TriSide.OPP to mid(NAME_C, NAME_B),
    )
    val wrongHyp = L("To je přepona – leží naproti pravému úhlu. Odvěsny jsou ty dvě kratší strany.", "Это гипотенуза — она лежит напротив прямого угла. Катеты — две более короткие стороны.")
    val wrongOpp = L("Tahle odvěsna se úhlu nedotýká – je protilehlá. Která odvěsna z vrcholu úhlu vychází?", "Этот катет угла не касается — он противолежащий. Какой катет выходит из вершины угла?")
    fun tap(s: TriSide) {
        when {
            !hypOk -> if (s == TriSide.HYP) { hypOk = true; msg = null } else msg = L(
                "Přepona je nejdelší strana. Která strana leží naproti pravému úhlu?",
                "Гипотенуза — самая длинная сторона. Какая сторона лежит напротив прямого угла?",
            )
            !adjOk -> when {
                beta -> msg = L("Teď hledáme odvěsnu u úhlu α – přepni na „Úhel α“.", "Сейчас ищем катет у угла α — переключи на «Угол α».")
                s == TriSide.ADJ -> { adjOk = true; msg = null }
                s == TriSide.HYP -> msg = wrongHyp
                else -> msg = wrongOpp
            }
            !betaOk -> when {
                !beta -> msg = L("Nejdřív přepni na „Úhel β“.", "Сначала переключи на «Угол β».")
                s == TriSide.OPP -> { betaOk = true; msg = null }
                s == TriSide.HYP -> msg = wrongHyp
                else -> msg = wrongOpp
            }
        }
    }
    Stage(
        h = 230.0,
        description = t.pick(L("Pravoúhlý trojúhelník se třemi stranami, na které se dá klepnout", "Прямоугольный треугольник, на стороны которого можно нажать")),
        onTap = { p -> spots.entries.minByOrNull { p.dist(it.value.x, it.value.y) }?.takeIf { p.dist(it.value.x, it.value.y) < 40 }?.let { tap(it.key) } },
    ) { c ->
        poly(listOf(NAME_A.x to NAME_A.y, NAME_B.x to NAME_B.y, NAME_C.x to NAME_C.y), c.subtle, null, 0.0, alpha = 0.6f)
        line(NAME_A.x, NAME_A.y, NAME_C.x, NAME_C.y, if (adjOk) c.derived else c.ink, 2.6)
        line(NAME_C.x, NAME_C.y, NAME_B.x, NAME_B.y, if (betaOk) c.derived else c.ink, 2.6)
        line(NAME_A.x, NAME_A.y, NAME_B.x, NAME_B.y, if (hypOk) c.load else c.ink, 2.6)
        rightAngleMark(NAME_C, NAME_A, NAME_B, c)
        angleArc(NAME_A.x, NAME_A.y, 30.0, 0.0, screenAngle(NAME_A, NAME_B), "α", c, if (!beta) c.highlight else c.inkSoft)
        angleArc(NAME_B.x, NAME_B.y, 30.0, screenAngle(NAME_B, NAME_A), 90.0, "β", c, if (beta) c.highlight else c.inkSoft)
        for (p in spots.values) tapSpot(p.x, p.y, c)
        // jména objevených stran (vzhledem k právě vybranému úhlu)
        if (hypOk) text(mid(NAME_A, NAME_B).x - 26, mid(NAME_A, NAME_B).y - 14, t.pick(L("přepona", "гипотенуза")), c.load, 14.0, Anchor.END)
        val adjName = if (beta) L("protilehlá", "противолежащий") else L("přilehlá", "прилежащий")
        val oppName = if (beta) L("přilehlá", "прилежащий") else L("protilehlá", "противолежащий")
        if (adjOk) text(mid(NAME_A, NAME_C).x, NAME_A.y + 24, t.pick(adjName), c.derived, 14.0)
        if (adjOk && betaOk) text(NAME_C.x + 20, mid(NAME_C, NAME_B).y + 5, t.pick(oppName), c.derived, 14.0, Anchor.START)
    }
    CardText(L(
        "Strany pravoúhlého trojúhelníku mají jména. **Přepona** leží naproti pravému úhlu; **odvěsny** jsou dvě: **přilehlá** (u úhlu) a **protilehlá** (naproti úhlu).",
        "У сторон прямоугольного треугольника есть названия. **Гипотенуза** (přepona) лежит напротив прямого угла; **катетов** (odvěsny) два: **прилежащий** (přilehlá) — у угла и **противолежащий** (protilehlá) — напротив угла.",
    ))
    Todo(
        L("Klepni na **přeponu** – nejdelší stranu, naproti pravému úhlu", "Нажми на **гипотенузу** — самую длинную сторону, напротив прямого угла") to hypOk,
        L("Klepni na odvěsnu **přilehlou** k úhlu α", "Нажми на катет, **прилежащий** к углу α") to adjOk,
        L("Přepni na „Úhel β“ a klepni na odvěsnu přilehlou k úhlu β", "Переключи на «Угол β» и нажми на катет, прилежащий к углу β") to betaOk,
    )
    Chips(listOf(L("Úhel α", "Угол α"), L("Úhel β", "Угол β")), if (beta) 1 else 0) { beta = it == 1; msg = null }
    msg?.let { Note(Tone.NUDGE, it) }
    if (betaOk) SmallText(L(
        "Přilehlá a protilehlá si vymění role, když se díváš z druhého úhlu. Přepona zůstává přeponou.",
        "Прилежащий и противолежащий меняются ролями, если смотреть из другого угла. Гипотенуза остаётся гипотенузой.",
    ))
}

/** Karta 3: kvíz – protilehlá strana u otočeného trojúhelníku. */
@Composable
private fun CardScope.CheckOpposite() {
    val t = tr()
    // pravý úhel vlevo nahoře, α vlevo dole
    val a = Pt(140.0, 200.0)
    val c0 = Pt(140.0, 40.0)
    val b = Pt(300.0, 40.0)
    Stage(h = 230.0, description = t.pick(L("Otočený pravoúhlý trojúhelník se stranami a, b, c", "Повёрнутый прямоугольный треугольник со сторонами a, b, c"))) { c ->
        poly(listOf(a.x to a.y, b.x to b.y, c0.x to c0.y), c.subtle, null, 0.0, alpha = 0.6f)
        line(c0.x, c0.y, b.x, b.y, c.ink, 2.6)
        line(a.x, a.y, c0.x, c0.y, c.ink, 2.6)
        line(a.x, a.y, b.x, b.y, c.ink, 2.6)
        rightAngleMark(c0, a, b, c)
        angleArc(a.x, a.y, 30.0, -90.0, screenAngle(a, b), "α", c)
        text((c0.x + b.x) / 2, c0.y - 10, "a", c.ink, 16.0, bold = true)
        text(a.x - 14, (a.y + c0.y) / 2 + 5, "b", c.ink, 16.0, Anchor.END, bold = true)
        text((a.x + b.x) / 2 + 16, (a.y + b.y) / 2 + 16, "c", c.ink, 16.0, Anchor.START, bold = true)
    }
    Quiz(
        question = L("Která strana je **protilehlá** úhlu α?", "Какая сторона — **противолежащая** углу α?"),
        options = listOf(
            Opt(L("a", "a"), correct = true),
            Opt(L("b", "b"), why = L("Strana b vychází přímo z vrcholu úhlu α – dotýká se ho. Jak se říká odvěsně u úhlu?", "Сторона b выходит прямо из вершины угла α — она его касается. Как называется катет у угла?")),
            Opt(L("c", "c"), why = L("c leží naproti pravému úhlu – to je přepona. Která odvěsna se úhlu α vůbec nedotýká?", "c лежит напротив прямого угла — это гипотенуза. Какой катет вообще не касается угла α?")),
        ),
        explain = L(
            "Protilehlá odvěsna se vrcholu úhlu nedotýká – leží naproti. Nezáleží na tom, jak je trojúhelník otočený.",
            "Противолежащий катет не касается вершины угла — он лежит напротив. Как повёрнут треугольник, неважно.",
        ),
        columns = 3,
    )
}

internal fun trigLesson1() = Lesson(
    id = 101,
    title = L("Odkud se vzal sinus", "Откуда взялся синус"),
    summary = L("V pravoúhlých trojúhelnících se stejným úhlem je podíl stran vždy stejný.", "В прямоугольных треугольниках с одинаковым углом отношение сторон всегда одно и то же."),
    cards = listOf(
        Card { SameAngle() },
        Card { AngleSetsRatio() },
        Card { NameSides() },
        Card { CheckOpposite() },
    ),
)
