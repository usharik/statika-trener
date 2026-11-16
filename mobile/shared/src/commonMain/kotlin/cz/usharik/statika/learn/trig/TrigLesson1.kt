package cz.usharik.statika.learn.trig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import cz.usharik.statika.learn.Pt
import cz.usharik.statika.learn.Readout
import cz.usharik.statika.learn.SmallText
import cz.usharik.statika.learn.Stage
import cz.usharik.statika.learn.Step
import cz.usharik.statika.learn.Steps
import cz.usharik.statika.learn.ValueSlider
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.handle
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.poly
import cz.usharik.statika.learn.quizStep
import cz.usharik.statika.learn.rightAngleMark
import cz.usharik.statika.learn.screenAngle
import cz.usharik.statika.learn.tapSpot
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
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

// ------------------------------------------------------------------ pomocníci doku pro lekce 101–104

/** Dok: volby (Chips), hlavní tlačítko [button] a pod nimi krátká zpětná vazba [msg]. */
@Composable
internal fun PickCheck(options: List<L>, selected: Int?, onSelect: (Int) -> Unit, button: L, msg: L?, onCheck: () -> Unit) {
    Chips(options, selected) { onSelect(it) }
    ActionButton(button, enabled = selected != null) { onCheck() }
    msg?.let { Note(Tone.NUDGE, it) }
}

internal val BTN_CHECK = L("Zkontrolovat", "Проверить")
internal val BTN_TEST = L("Ověřit", "Проверить")

/**
 * Krok „tipni si“: volby + „Ověřit“. Odeslaný tip se jen zapíše ([onGuess]) – správnost ukáže až pokus v dalším kroku.
 */
@Composable
internal fun guessStep(label: L, options: List<L>, guess: Int?, onGuess: (Int) -> Unit): Step {
    var sel by remember(label.cs) { mutableStateOf<Int?>(null) }
    return Step(
        label, guess != null,
        result = { guess?.let { g -> SmallText(L("Tvůj tip: " + options[g].cs, "Твой прогноз: " + options[g].ru)) } },
    ) {
        Chips(options, sel) { sel = it }
        ActionButton(BTN_TEST, enabled = sel != null) { sel?.let(onGuess) }
    }
}

/** 1 m rampy v jednotkách scény (karta 0). */
private const val RAMP_PX = 50.0

/** Karta 0: podobné trojúhelníky – podíl výška / šikmá délka nezávisí na velikosti. */
@Composable
private fun CardScope.SameAngle() {
    val t = tr()
    var len by remember { mutableStateOf(2) } // šikmá délka v m
    var guess by remember { mutableStateOf<Int?>(null) }
    var tested by remember { mutableStateOf<Int?>(null) } // délka, při které žák ověřil
    var msg by remember { mutableStateOf<L?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val guesses = listOf(L("1 m", "1 м"), L("2 m", "2 м"), L("4 m", "4 м"))
    fun setLen(v: Double) {
        if (guess == null || tested != null) return
        len = v.roundToInt().coerceIn(2, 6)
        msg = null
    }
    val r = Ramp(40.0, 205.0, RAMP_PX * len, 30.0)
    val h = len / 2.0
    Stage(
        h = 230.0,
        description = t.pick(L("Nájezdová rampa – pravoúhlý trojúhelník s úhlem 30°", "Пандус — прямоугольный треугольник с углом 30°")),
        onDrag = { p, start ->
            if (!start || p.dist(r.b.x, r.b.y) < 50) {
                dragging = true
                // průmět prstu na směr přepony
                val along = (p.x - 40.0) * Trig.cosD(30.0) - (p.y - 205.0) * Trig.sinD(30.0)
                setLen(along / RAMP_PX)
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        line(10.0, 205.0, 390.0, 205.0, c.ink, 1.4)
        if (len > 2) rampTri(Ramp(40.0, 205.0, RAMP_PX * 2, 30.0), c, null, alpha = 0.3f)
        if (guess == null) {
            // tip: rampa dlouhá 4 m čárkovaně, výška neznámá
            val far = Ramp(40.0, 205.0, RAMP_PX * 4, 30.0)
            line(r.b.x, r.b.y, far.b.x, far.b.y, c.load, 1.6, dash = true)
            line(far.c.x, far.c.y, far.b.x, far.b.y, c.derived, 1.4, dash = true)
            text(far.c.x + 8, (far.c.y + far.b.y) / 2 + 5, t.pick(L("h = ?", "h = ?")), c.derived, 14.0, Anchor.START, bold = true)
            text(far.b.x - 10, far.b.y - 8, t.pick(L("L = 4 m", "L = 4 м")), c.load, 13.0, Anchor.END)
        }
        rampTri(
            r, c, "30°",
            hypLabel = t.pick(L("L = $len m", "L = $len м")),
            oppLabel = t.pick(L("h = ${num(h)} m", "h = ${num(h)} м")),
        )
        if (guess != null && tested == null) handle(r.b.x, r.b.y, c, dragging)
    }
    CardText(L(
        "Dvě rampy se stejným sklonem mají stejný tvar, ale můžou mít různou velikost. Tahle stoupá pod úhlem 30°: je dlouhá L = 2 m a vystoupá do výšky h = 1 m.",
        "Два пандуса с одинаковым углом наклона имеют одинаковую форму, но могут различаться по размеру. Этот поднимается под углом 30°: его длина L = 2 м, высота h = 1 м.",
    ))
    Steps(
        guessStep(L("Tipni si: jak vysoko vystoupá rampa dlouhá 4 m?", "Предскажи: на какую высоту поднимется пандус длиной 4 м?"), guesses, guess) { guess = it },
        Step(
            L("Prodluž rampu aspoň na 4 m a ověř svůj tip", "Удлини пандус хотя бы до 4 м и проверь прогноз"), tested != null,
            result = {
                val l = tested ?: 4
                Note(if (guess == 1) Tone.OK else Tone.INFO, L(
                    (if (guess == 1) "Tip sedí. " else "Tip ${guesses[guess ?: 0].cs} nesedí. ") +
                        "Dvakrát delší rampa je i dvakrát vyšší: L = 4 m → h = 2 m. Při L = $l m je h = ${num(l / 2.0)} m a pořád h / L = 0,5.",
                    (if (guess == 1) "Прогноз верен. " else "Прогноз ${guesses[guess ?: 0].ru} не подтвердился. ") +
                        "Вдвое более длинный пандус и вдвое выше: L = 4 м → h = 2 м. При L = $l м высота h = ${num(l / 2.0)} м, и всё так же h / L = 0,5.",
                ))
            },
        ) {
            ValueSlider(t.pick(L("Délka rampy L = $len m", "Длина пандуса L = $len м")), len.toFloat(), 2f..6f, steps = 3) { setLen(it.toDouble()) }
            Readout("h / L = ${num(h)} / $len = 0,5")
            ActionButton(BTN_TEST) {
                if (len >= 4) tested = len else msg = L("Rampa měří jen $len m – prodluž ji aspoň na 4 m.", "Пандус пока всего $len м — удлини его хотя бы до 4 м.")
            }
            msg?.let { Note(Tone.NUDGE, it) }
        },
        quizStep(
            question = L("Co zůstalo po prodloužení rampy stejné?", "Что сохранилось после удлинения пандуса?"),
            options = listOf(
                Opt(L("Výška h", "Высота h"), why = L("Výška se zvětšila spolu s délkou. Co se nezměnilo?", "Высота выросла вместе с длиной. Что не изменилось?")),
                Opt(L("Délka L", "Длина L"), why = L("Délku jsi právě měnil(a). Podívej se na podíl h / L.", "Длину ты только что менял(а). Посмотри на отношение h / L.")),
                Opt(L("Podíl h / L", "Отношение h / L"), correct = true),
            ),
            explain = L(
                "Při dvojnásobné délce je dvojnásobná i výška: 1/2 = 2/4 = 0,5. Trojúhelníky se stejnými úhly jsou **podobné** – odpovídající strany mají stejný poměr.",
                "При удвоении длины высота тоже удваивается: 1/2 = 2/4 = 0,5. Треугольники с одинаковыми углами **подобны** — отношения соответствующих сторон у них одинаковы.",
            ),
            columns = 3,
        ),
    )
}

/** 1 m rampy v jednotkách scény (karta 1). */
private const val RAMP2_PX = 36.0

/** Karta 1: podíl závisí jen na úhlu – to je sin α. Nejdřív úhel (délka stojí), pak délka (úhel stojí). */
@Composable
private fun CardScope.AngleSetsRatio() {
    val t = tr()
    var alpha by remember { mutableStateOf(30.0) }
    var len by remember { mutableStateOf(4) }
    var guess by remember { mutableStateOf<Int?>(null) }
    var at60 by remember { mutableStateOf(false) }
    var lenChanged by remember { mutableStateOf(false) }
    var sel by remember { mutableStateOf<Int?>(null) }
    var answered by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<L?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val changes = listOf(L("Vzroste", "Увеличится"), L("Klesne", "Уменьшится"), L("Nezmění se", "Не изменится"))
    fun setAlpha(v: Double) {
        if (guess == null || at60) return
        alpha = ((v / 5).roundToInt() * 5.0).coerceIn(30.0, 60.0)
        msg = null
    }
    fun setLen(v: Double) {
        if (!at60 || answered) return
        val n = v.roundToInt().coerceIn(2, 6)
        if (n != len) lenChanged = true
        len = n
        msg = null
    }
    val r = Ramp(50.0, 215.0, RAMP2_PX * len, alpha)
    val ratio = Trig.sinD(alpha)
    val h = len * ratio
    Stage(
        h = 235.0,
        description = t.pick(L("Rampa s nastavitelným úhlem a délkou", "Пандус с изменяемым углом и длиной")),
        onDrag = { p, start ->
            if (!start || p.dist(r.b.x, r.b.y) < 50) {
                dragging = true
                setLen(hypot(p.x - 50.0, p.y - 215.0) / RAMP2_PX)
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        line(10.0, 215.0, 390.0, 215.0, c.ink, 1.4)
        rampTri(
            r, c, "${alpha.roundToInt()}°",
            hypLabel = t.pick(L("L = $len m", "L = $len м")),
            oppLabel = t.pick(L("h = ${num(h)} m", "h = ${num(h)} м")),
        )
        if (at60 && !answered) handle(r.b.x, r.b.y, c, dragging)
        text(388.0, 26.0, "h / L = ${num(ratio, 3)}", c.ink, 15.0, Anchor.END, bold = true)
    }
    CardText(L(
        "Podíl výšky rampy a její délky po svahu se jmenuje **sinus** úhlu: sin α = h / L. Na čem závisí?",
        "Отношение высоты пандуса к его длине по склону называется **синусом** угла: sin α = h / L. От чего оно зависит?",
    ))
    Steps(
        guessStep(L("Tipni si: jak se změní h / L, když úhel zvětšíš z 30° na 60°?", "Предскажи: как изменится h / L, если увеличить угол с 30° до 60°?"), changes, guess) { guess = it },
        Step(
            L("Zvětši úhel na 60° (délka zůstává 4 m) a ověř tip", "Увеличь угол до 60° (длина остаётся 4 м) и проверь прогноз"), at60,
            result = {
                Note(if (guess == 0) Tone.OK else Tone.INFO, L(
                    (if (guess == 0) "Tip sedí: " else "Je to jinak: ") + "strmější rampa je při stejné délce vyšší, h / L vzrostl z 0,5 na 0,866.",
                    (if (guess == 0) "Прогноз верен: " else "На самом деле иначе: ") + "при той же длине более крутой пандус выше, h / L вырос с 0,5 до 0,866.",
                ))
            },
        ) {
            ValueSlider(t.pick(L("Úhel α = ${alpha.roundToInt()}°", "Угол α = ${alpha.roundToInt()}°")), alpha.toFloat(), 30f..60f, steps = 5) { setAlpha(it.toDouble()) }
            ActionButton(BTN_TEST) {
                if (alpha == 60.0) at60 = true else msg = L("Úhel je zatím ${alpha.roundToInt()}° – zvětši ho na 60°.", "Угол пока ${alpha.roundToInt()}° — увеличь его до 60°.")
            }
            msg?.let { Note(Tone.NUDGE, it) }
        },
        Step(
            L("Změň délku rampy (úhel zůstává 60°) – co udělá h / L?", "Измени длину пандуса (угол остаётся 60°) — что будет с h / L?"), answered,
            result = {
                Note(Tone.OK, L(
                    "Sinus závisí na úhlu, ne na velikosti trojúhelníku: sin 30° = 0,5 a sin 60° ≈ 0,866 v každém pravoúhlém trojúhelníku.",
                    "Синус зависит от угла, а не от размера треугольника: sin 30° = 0,5 и sin 60° ≈ 0,866 в любом прямоугольном треугольнике.",
                ))
            },
        ) {
            ValueSlider(t.pick(L("Délka rampy L = $len m", "Длина пандуса L = $len м")), len.toFloat(), 2f..6f, steps = 3) { setLen(it.toDouble()) }
            PickCheck(changes, sel, { sel = it; msg = null }, BTN_CHECK, msg) {
                msg = when {
                    !lenChanged -> L("Nejdřív posuvníkem změň délku rampy.", "Сначала измени длину пандуса ползунком.")
                    sel == 2 -> { answered = true; null }
                    else -> L("Podívej se na h / L v obrázku – změnil se při jiné délce?", "Посмотри на h / L на рисунке — изменилось ли оно при другой длине?")
                }
            }
        },
    )
}

/** Trojúhelník karet o jménech stran: α u A vlevo dole, pravý úhel u C vpravo dole, β u B nahoře. */
private val NAME_A = Pt(80.0, 190.0)
private val NAME_C = Pt(300.0, 190.0)
private val NAME_B = Pt(300.0, 50.0)

private fun mid(p: Pt, q: Pt) = Pt((p.x + q.x) / 2, (p.y + q.y) / 2)

/** Karta 2: jména stran; přilehlá / protilehlá podle toho, ze kterého úhlu se díváme. */
@Composable
private fun CardScope.NameSides() {
    val t = tr()
    var hypOk by remember { mutableStateOf(false) }
    var adjOk by remember { mutableStateOf(false) }
    var betaOk by remember { mutableStateOf(false) }
    var sel by remember { mutableStateOf<Int?>(null) } // 0 AB, 1 AC, 2 BC
    var msg by remember { mutableStateOf<L?>(null) }
    val names = listOf(L("AB", "AB"), L("AC", "AC"), L("BC", "BC"))
    val ends = listOf(NAME_A to NAME_B, NAME_A to NAME_C, NAME_C to NAME_B)
    val beta = adjOk // od 3. kroku se díváme z úhlu β
    val hypMsg = L("AB je přepona – leží naproti pravému úhlu u C. Hledáme **odvěsnu**.", "AB — гипотенуза, она лежит напротив прямого угла C. Нужен **катет**.")
    fun check() {
        val s = sel ?: return
        msg = when {
            !hypOk -> if (s == 0) { hypOk = true; null } else L(
                "Tahle strana vychází z pravého úhlu u C – je to odvěsna. Která strana leží pravému úhlu naproti?",
                "Эта сторона выходит из прямого угла C — это катет. Какая сторона лежит напротив прямого угла?",
            )
            !adjOk -> when (s) {
                1 -> { adjOk = true; null }
                0 -> hypMsg
                else -> L("BC se vrcholu A vůbec nedotýká – je úhlu α protilehlá.", "BC вообще не касается вершины A — она противолежит углу α.")
            }
            else -> when (s) {
                2 -> { betaOk = true; null }
                0 -> hypMsg
                else -> L("AC se vrcholu B nedotýká – pro úhel β je protilehlá.", "AC не касается вершины B — для угла β она противолежащая.")
            }
        }
        if (msg == null) sel = null
    }
    Stage(
        h = 230.0,
        description = t.pick(L("Pravoúhlý trojúhelník ABC s pravým úhlem u C", "Прямоугольный треугольник ABC с прямым углом при C")),
        onTap = { p ->
            if (betaOk) return@Stage
            val spots = ends.map { (a, b) -> mid(a, b) }
            spots.withIndex().minByOrNull { p.dist(it.value.x, it.value.y) }?.takeIf { p.dist(it.value.x, it.value.y) < 40 }?.let { sel = it.index; msg = null }
        },
    ) { c ->
        poly(listOf(NAME_A.x to NAME_A.y, NAME_B.x to NAME_B.y, NAME_C.x to NAME_C.y), c.subtle, null, 0.0, alpha = 0.6f)
        val colors = listOf(if (hypOk) c.load else c.ink, if (adjOk) c.derived else c.ink, if (betaOk) c.derived else c.ink)
        for ((i, e) in ends.withIndex()) {
            val (p, q) = e
            line(p.x, p.y, q.x, q.y, if (sel == i) c.highlight else colors[i], if (sel == i) 4.5 else 2.6)
        }
        rightAngleMark(NAME_C, NAME_A, NAME_B, c)
        angleArc(NAME_A.x, NAME_A.y, 30.0, 0.0, screenAngle(NAME_A, NAME_B), "α", c, if (!beta) c.highlight else c.inkSoft)
        angleArc(NAME_B.x, NAME_B.y, 30.0, screenAngle(NAME_B, NAME_A), 90.0, "β", c, if (beta) c.highlight else c.inkSoft)
        text(NAME_A.x - 10, NAME_A.y + 18, "A", c.ink, 15.0, Anchor.END, bold = true)
        text(NAME_C.x + 10, NAME_C.y + 18, "C", c.ink, 15.0, Anchor.START, bold = true)
        text(NAME_B.x + 10, NAME_B.y - 4, "B", c.ink, 15.0, Anchor.START, bold = true)
        if (!betaOk) for ((a, b) in ends) mid(a, b).let { tapSpot(it.x, it.y, c) }
        // jména objevených stran (vzhledem k právě sledovanému úhlu)
        if (hypOk) text(mid(NAME_A, NAME_B).x - 26, mid(NAME_A, NAME_B).y - 14, t.pick(L("přepona", "гипотенуза")), c.load, 14.0, Anchor.END)
        val acName = if (beta) L("protilehlá k β", "противолежащий β") else L("přilehlá k α", "прилежащий к α")
        if (adjOk) text(mid(NAME_A, NAME_C).x, NAME_A.y + 26, t.pick(acName), c.derived, 14.0)
        if (betaOk) text(NAME_C.x + 20, mid(NAME_C, NAME_B).y + 5, t.pick(L("přilehlá k β", "прилежащий к β")), c.derived, 14.0, Anchor.START)
    }
    CardText(L(
        "**Přepona** leží naproti pravému úhlu. Dvě kratší strany jsou **odvěsny**: **přilehlá** vychází z vrcholu sledovaného úhlu, **protilehlá** leží naproti němu.",
        "**Гипотенуза** лежит напротив прямого угла. Две более короткие стороны — **катеты**: **прилежащий** выходит из вершины рассматриваемого угла, **противолежащий** лежит напротив него.",
    ))
    val pick: @Composable () -> Unit = { PickCheck(names, sel, { sel = it; msg = null }, BTN_CHECK, msg) { check() } }
    Steps(
        Step(
            L("Vyber přeponu", "Выбери гипотенузу"), hypOk,
            result = { Note(Tone.OK, L("AB leží naproti pravému úhlu u C – je to přepona, nejdelší strana.", "AB лежит напротив прямого угла C — это гипотенуза, самая длинная сторона.")) },
        ) { pick() },
        Step(
            L("Vyber odvěsnu **přilehlou** k úhlu α", "Выбери катет, **прилежащий** к углу α"), adjOk,
            result = { Note(Tone.OK, L("AC vychází z vrcholu A – je přilehlá k α. AB z něj vychází taky, ale je to přepona.", "AC выходит из вершины A — он прилежит к α. AB тоже выходит из A, но это гипотенуза.")) },
        ) { pick() },
        Step(
            L("Teď sleduj úhel β: vyber odvěsnu **přilehlou** k β", "Теперь смотрим на угол β: выбери катет, **прилежащий** к β"), betaOk,
            result = { Note(Tone.OK, L("Při přechodu od α k β si odvěsny vymění role. Přepona AB zůstává stejná.", "При переходе от α к β катеты меняются ролями. Гипотенуза AB остаётся той же.")) },
        ) { pick() },
    )
}

/** Karta 3: kvíz – protilehlá strana u otočeného trojúhelníku (odvěsny různě dlouhé, ať nejde hádat podle tvaru). */
@Composable
private fun CardScope.CheckOpposite() {
    val t = tr()
    // pravý úhel vlevo nahoře, α vlevo dole
    val a = Pt(120.0, 195.0)
    val c0 = Pt(120.0, 45.0)
    val b = Pt(340.0, 45.0)
    val q = quizStep(
        question = L("Která odvěsna je **protilehlá** úhlu α?", "Какой катет **противолежит** углу α?"),
        options = listOf(
            Opt(L("a", "a"), correct = true),
            Opt(L("b", "b"), why = L("Strana b vychází přímo z vrcholu úhlu α – je k němu přilehlá. Která odvěsna se vrcholu α nedotýká?", "Сторона b выходит прямо из вершины угла α — она прилежащая. Какой катет не касается вершины α?")),
            Opt(L("c", "c"), why = L("c leží naproti pravému úhlu – to je přepona, ne odvěsna.", "c лежит напротив прямого угла — это гипотенуза, а не катет.")),
        ),
        explain = L(
            "Strana a nevychází z vrcholu α, ale leží naproti němu – je protilehlá. Strana b je přilehlá odvěsna, c je přepona.",
            "Сторона a не выходит из вершины α, а лежит напротив неё — это противолежащий катет. Сторона b — прилежащий катет, c — гипотенуза.",
        ),
        columns = 3,
    )
    Stage(h = 230.0, description = t.pick(L("Otočený pravoúhlý trojúhelník se stranami a, b, c", "Повёрнутый прямоугольный треугольник со сторонами a, b, c"))) { c ->
        poly(listOf(a.x to a.y, b.x to b.y, c0.x to c0.y), c.subtle, null, 0.0, alpha = 0.6f)
        line(c0.x, c0.y, b.x, b.y, if (q.done) c.highlight else c.ink, if (q.done) 4.5 else 2.6)
        line(a.x, a.y, c0.x, c0.y, c.ink, 2.6)
        line(a.x, a.y, b.x, b.y, c.ink, 2.6)
        rightAngleMark(c0, a, b, c)
        angleArc(a.x, a.y, 30.0, -90.0, screenAngle(a, b), "α", c, c.highlight)
        text((c0.x + b.x) / 2, c0.y - 10, "a", if (q.done) c.highlight else c.ink, 16.0, bold = true)
        text(a.x - 14, (a.y + c0.y) / 2 + 5, "b", c.ink, 16.0, Anchor.END, bold = true)
        text((a.x + b.x) / 2 + 16, (a.y + b.y) / 2 + 16, "c", c.ink, 16.0, Anchor.START, bold = true)
        if (q.done) text((c0.x + b.x) / 2, c0.y + 22, t.pick(L("protilehlá k α", "противолежащий α")), c.highlight, 13.0)
    }
    CardText(L("Otočení obrázku nemění vztahy mezi stranami a úhly.", "Поворот рисунка не меняет отношения между сторонами и углами."))
    Steps(q)
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
