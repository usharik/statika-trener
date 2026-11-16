package cz.usharik.statika.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.LocalColors
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import cz.usharik.statika.ui.rich
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Lekce 7 – Uvolnění. Hlavní myšlenka: podpora drží nosník jen tím, že na něj působí silou.
 * Bez ní nosník opravdu spadne (zrychluje, dokud nedopadne). Uvolnění je pak **model na papíře**:
 * podporu nahradíme silou (reakcí) a nosník „nic nepozná“. Kolik ta síla je, najde žák nejdřív rukou
 * a na vahách, pak výpočtem.
 */

// ------------------------------------------------------------------ kroky se stavem venku (obrázek čte volbu)

/** Kvíz jako krok; stav drží karta, takže obrázek může reagovat na [QuizState.picked] / [QuizState.solved]. */
private fun QuizState.step7(columns: Int = 1, label: L = question, more: (@Composable () -> Unit)? = null): Step =
    Step(label, solved, result = { Feedback(); more?.invoke() }) {
        if (label != question) Text(rich(question.t()), style = MaterialTheme.typography.bodyMedium, color = LocalColors.current.ink, modifier = Modifier.padding(top = 4.dp))
        Options(columns)
        if (!solved) Feedback()
    }

/** Číselná odpověď jako krok; [show] = co má žák během počítání vidět v doku (rovnice). */
private fun NumState.step7(label: L, show: (@Composable () -> Unit)? = null, more: (@Composable () -> Unit)? = null): Step =
    Step(label, solved, result = { Feedback(); more?.invoke() }) {
        show?.invoke()
        Fields()
        Buttons()
        if (!solved) Feedback()
    }

/** Tip před pokusem: volba + potvrzení. Správnost ukáže až pokus v dalším kroku. */
@Composable
private fun tip7(label: L, options: List<L>, guess: Int?, onGuess: (Int) -> Unit): Step {
    var sel by remember(label.cs) { mutableStateOf<Int?>(null) }
    return Step(label, guess != null, result = { guess?.let { SmallText(L("Tvůj tip: " + options[it].cs, "Твой прогноз: " + options[it].ru)) } }) {
        Chips(options, sel) { sel = it }
        ActionButton(L("Potvrdit tip", "Подтвердить прогноз"), enabled = sel != null) { sel?.let(onGuess) }
    }
}

/** Otočení bodu (x, y) kolem (px, py) o úhel [deg] (stupně, ve směru osy y dolů). */
private fun rot(x: Double, y: Double, px: Double, py: Double, deg: Double): Pair<Double, Double> {
    val a = deg * PI / 180
    val dx = x - px
    val dy = y - py
    return (px + dx * cos(a) - dy * sin(a)) to (py + dx * sin(a) + dy * cos(a))
}

/** Nosník jako otočený obdélník (aby mohl padat). */
private fun PrimBuilder.tiltedBeam(x1: Double, x2: Double, y: Double, px: Double, deg: Double, c: StatikaColors, alpha: Float = 1f) {
    val pts = listOf(x1 to y - 5, x2 to y - 5, x2 to y + 5, x1 to y + 5).map { (x, yy) -> rot(x, yy, px, y, deg) }
    poly(pts, c.beam, c.ink, 1.6, alpha)
}

/** Podlaha: čára se šrafami. */
private fun PrimBuilder.ground(x1: Double, x2: Double, y: Double, c: StatikaColors, alpha: Float = 1f) {
    line(x1, y, x2, y, c.ink, 1.5, alpha = alpha)
    var x = x1 + 6
    while (x <= x2) {
        line(x, y, x - 6, y + 7, c.inkSoft, 1.0, alpha = alpha)
        x += 8
    }
}

/**
 * Otáčení z klidu se stálým úhlovým zrychlením [alpha] (rad/s², znaménko = smysl), dokud úhel nedosáhne
 * [limitDeg] (dopad na podlahu / zarážku). [onDeg] dostává úhel ve stupních každý snímek.
 */
private suspend fun swing(alpha: Double, limitDeg: Double, onDeg: (Double) -> Unit) {
    val limit = abs(limitDeg) * PI / 180
    var th = 0.0
    var w = 0.0
    var last = 0L
    while (abs(th) < limit) {
        withFrameNanos { now ->
            val dt = if (last == 0L) 0.0 else ((now - last) / 1e9).coerceAtMost(0.05)
            last = now
            w += alpha * dt
            th += w * dt
        }
        onDeg(th.coerceIn(-limit, limit) * 180 / PI)
    }
}

/**
 * Karta 1: odstraň podporu B → nosník se kolem A otáčí čím dál rychleji a dopadne na zem.
 * Pak „uvolnění“ jako model: nový obrázek, podpory nahradí žák správnými reakcemi a spočítá je
 * (F = 12 kN v ¼ rozpětí → 9 a 3 kN).
 */
@Composable
private fun CardScope.CutItFree() {
    val t = tr()
    val y = 78.0
    val floor = 200.0
    val xa = 70.0
    val xb = 330.0
    val m = (xb - xa) / 4 // rozpětí 4 m
    val xf = xa + m // F = 12 kN 1 m od A
    val kn = t.pick(L("kN", "кН"))
    // 0 = stojí, 1 = padá, 2 = leží na zemi, 3 = model (uvolnění)
    var phase by remember { mutableStateOf(0) }
    var tilt by remember { mutableDoubleStateOf(0.0) }
    val modelIn = remember { Animatable(0f) }
    var modelDone by remember { mutableStateOf(false) }
    val growA = remember { Animatable(0f) }
    val growB = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val qA = rememberQuiz(
        L("Podpora A je pevná kloubová. Co nakreslíš místo ní?", "Опора A — шарнирно-неподвижная. Что нарисуешь вместо неё?"),
        listOf(
            Opt(
                L("↑ jen [[R_Ay]]", "↑ только [[R_Ay]]"),
                why = L(
                    "Něco chybí (červený otazník). Pevná kloubová podpora nedovolí ani posun **do strany** – i ve vodorovném směru na nosník může působit silou.",
                    "Чего-то не хватает (красный вопрос). Шарнирно-неподвижная опора не даёт сдвинуться и **вбок** — и в горизонтальном направлении она может действовать на балку силой.",
                ),
            ),
            Opt(L("→ ↑ [[R_Ax]] a [[R_Ay]]", "→ ↑ [[R_Ax]] и [[R_Ay]]"), correct = true),
            Opt(
                L("→ ↑ ↺ [[R_Ax]], [[R_Ay]], [[M_A]]", "→ ↑ ↺ [[R_Ax]], [[R_Ay]], [[M_A]]"),
                why = L(
                    "Moment navíc! Kloub otáčení dovolí, takže momentem nebrání. Moment by kreslilo jen vetknutí.",
                    "Лишний момент! Шарнир поворачиваться позволяет, значит, моментом не препятствует. Момент рисуют только у заделки.",
                ),
            ),
        ),
        explain = L(
            "Ano: kloub nepustí nosník do strany ani nahoru či dolů – brání mu dvěma silami [[R_Ax]] a [[R_Ay]]. Otáčet se nechá, takže moment nemá.",
            "Да: шарнир не пускает балку ни вбок, ни вверх-вниз — препятствует двумя силами [[R_Ax]] и [[R_Ay]]. Поворачиваться позволяет, поэтому момента нет.",
        ),
    )
    val qB = rememberQuiz(
        L("Podpora B je posuvná kloubová. Co nakreslíš místo ní?", "Опора B — шарнирно-подвижная. Что нарисуешь вместо неё?"),
        listOf(
            Opt(L("↑ jen [[R_B]]", "↑ только [[R_B]]"), correct = true),
            Opt(
                L("← ↑ [[R_Bx]] a [[R_By]]", "← ↑ [[R_Bx]] и [[R_By]]"),
                why = L(
                    "Vodorovná síla navíc. Válečky se do strany volně kutálejí – vodorovně podpora B nebrání, takže tam žádnou sílu nemá.",
                    "Лишняя горизонтальная сила. Катки свободно катятся вбок — по горизонтали опора B не препятствует, значит, силы там нет.",
                ),
            ),
        ),
        explain = L(
            "Tohle je **uvolnění**: nosník nikam nepadá – podpory jsme na papíře nahradili silami, kterými na nosník působí. Zbývá zjistit, jak velké ty síly jsou.",
            "Это и есть **освобождение от связей**: балка никуда не падает — опоры мы на бумаге заменили силами, с которыми они действуют на балку. Осталось узнать, чему равны эти силы.",
        ),
    )
    val nR = rememberNum(
        L("Spočítej reakce", "Найди реакции"),
        listOf(NumSpec(L("[[R_Ay]]", "[[R_Ay]]"), 9.0, kn), NumSpec(L("[[R_B]]", "[[R_B]]"), 3.0, kn)),
        explain = L(
            "Σ[[M_A]] = 0: [[R_B]] · 4 − 12 · 1 = 0 → [[R_B]] = 3 kN. Σ[[F_y]] = 0: [[R_Ay]] + 3 − 12 = 0 → [[R_Ay]] = 9 kN. Σ[[F_x]] = 0: [[R_Ax]] = 0. F je blíž k A, proto A nese víc.",
            "Σ[[M_A]] = 0: [[R_B]] · 4 − 12 · 1 = 0 → [[R_B]] = 3 кН. Σ[[F_y]] = 0: [[R_Ay]] + 3 − 12 = 0 → [[R_Ay]] = 9 кН. Σ[[F_x]] = 0: [[R_Ax]] = 0. F ближе к A, поэтому A несёт больше.",
        ),
        traps = listOf(
            NumTrap.near(L("Prohozené: F je blíž k A – víc nese podpora, která je k síle blíž.", "Перепутано: F ближе к A — больше несёт опора, которая ближе к силе."), 3.0, 9.0),
            NumTrap.near(L("Napůl by se F dělila, jen kdyby byla uprostřed. Tady je 1 m od A.", "Пополам F делилась бы, только если бы была посередине. Здесь она в 1 м от A."), 6.0, 6.0),
            NumTrap.near(L("Rameno [[R_B]] k bodu A je celé rozpětí 4 m, ne 3 m.", "Плечо [[R_B]] относительно A — весь пролёт 4 м, а не 3 м."), null, 4.0),
        ),
        nudge = L(
            "Momenty k bodu A: [[R_B]] má rameno 4 m, F rameno 1 m. Pak Σ[[F_y]] = 0.",
            "Моменты относительно A: у [[R_B]] плечо 4 м, у F — 1 м. Потом Σ[[F_y]] = 0.",
        ),
    )
    val pickA = if (qA.solved) 1 else qA.picked
    val pickB = if (qB.solved) 0 else qB.picked
    LaunchedEffect(pickA) {
        if (pickA != null) {
            growA.snapTo(0f)
            growA.animateTo(1f, tween(500))
        }
    }
    LaunchedEffect(pickB) {
        if (pickB != null) {
            growB.snapTo(0f)
            growB.animateTo(1f, tween(500))
        }
    }
    // po dopadu se nakreslí model (nový obrázek – skutečný nosník leží dál na zemi, model nic nepadá)
    LaunchedEffect(phase) {
        if (phase == 2) {
            delay(1600)
            phase = 3
            modelIn.animateTo(1f, tween(700))
            delay(300)
            modelDone = true
        }
    }
    val solved = nR.solved
    // dopad: pravý dolní roh nosníku (o 260 + 20 od A) se dotkne podlahy
    val landDeg = asin((floor - y - 5) / (xb + 20 - xa)) * 180 / PI

    Stage(h = 222.0, description = t.pick(L("Nosník na dvou podporách se silou F; podpora B jde odstranit", "Балка на двух опорах с силой F; опору B можно убрать"))) { c ->
        if (phase < 3) {
            // skutečnost: podpory stojí na betonových blocích
            val gone = phase > 0
            rect(xa - 18, y + 33, 36.0, floor - y - 33, c.subtle, c.inkSoft, 1.2)
            rect(xb - 18, y + 36, 36.0, floor - y - 36, c.subtle, c.inkSoft, 1.2, alpha = if (gone) 0.15f else 1f)
            ground(10.0, 390.0, floor, c)
            support(SupportType.PIN, xa, y, c, "A")
            support(SupportType.ROLLER, xb, y, c, "B", ghost = gone)
            tiltedBeam(xa - 20, xb + 20, y, xa, tilt, c)
            val (fx, fy) = rot(xf, y - 5, xa, y, tilt)
            force(fx, fy - 56, fx, fy, c.load)
            text(fx + 8, fy - 42, " = 12 kN", c.load, 14.0, Anchor.START, sym = "F")
            if (phase == 2) text(380.0, floor - 14, t.pick(L("bum!", "бум!")), c.inkSoft, 13.0, Anchor.END)
        } else {
            // model: nic nepadá, jen kreslíme síly, které na nosník působí
            val a = modelIn.value
            text(14.0, 210.0, t.pick(L("MODEL – uvolněný nosník (jen síly, nic nepadá)", "МОДЕЛЬ — освобождённая балка (только силы, ничего не падает)")), c.derived, 12.0, Anchor.START, bold = true, alpha = a)
            support(SupportType.PIN, xa, y, c, "A", ghost = qA.solved)
            support(SupportType.ROLLER, xb, y, c, "B", ghost = qB.solved)
            beam(xa - 20, xb + 20, y, c, a)
            force(xf, y - 56, xf, y - 5, c.load, alpha = a)
            text(xf + 8, y - 42, " = 12 kN", c.load, 14.0, Anchor.START, sym = "F", alpha = a)
            // dokud žák nevybral, místo reakce otazník
            if (pickA == null) text(xa + 14, y + 40, "?", c.react, 18.0, bold = true, alpha = a)
            if (pickB == null) text(xb - 14, y + 40, "?", c.react, 18.0, bold = true, alpha = a)
            // reakce v A
            if (pickA != null) {
                val g = growA.value
                val gd = g.toDouble()
                force(xa, y + 6 + 55 * gd, xa, y + 6, c.react, alpha = g)
                text(xa + 10, y + 56, if (solved) " = 9 kN" else null, c.react, 14.0, Anchor.START, sym = "R_Ay", alpha = g)
                if (pickA == 0) {
                    // chybí vodorovná reakce – naznačit, kde
                    force(xa - 66, y, xa - 22, y, c.load, dash = true, alpha = g)
                    text(xa - 44, y - 10, "?", c.load, 16.0, bold = true, alpha = g)
                } else {
                    force(xa - 22 - 44 * gd, y, xa - 22, y, c.react, alpha = g)
                    text(xa - 44, y - 10, if (solved) " = 0" else null, c.react, 14.0, sym = "R_Ax", alpha = g)
                }
                if (pickA == 2) {
                    moment(xa, y, 24.0, true, c.load)
                    text(xa + 30, y - 22, "✗", c.load, 16.0, bold = true)
                }
            }
            // reakce v B
            if (pickB != null) {
                val g = growB.value
                val gd = g.toDouble()
                force(xb, y + 6 + 55 * gd, xb, y + 6, c.react, alpha = g)
                text(xb - 10, y + 56, if (solved) " = 3 kN" else null, c.react, 14.0, Anchor.END, sym = if (pickB == 1) "R_By" else "R_B", alpha = g)
                if (pickB == 1) {
                    force(xb + 22 + 44 * gd, y, xb + 22, y, c.load, dash = true, alpha = g)
                    text(xb + 44, y - 10, "✗", c.load, 16.0, bold = true, alpha = g)
                }
            }
            // kóty
            val yd = y + 92
            line(xa, yd, xb, yd, c.ink, 0.9, alpha = a)
            for (x in listOf(xa, xf, xb)) line(x - 4, yd + 4, x + 4, yd - 4, c.ink, 1.2, alpha = a)
            text((xa + xf) / 2, yd - 5, "1 m", c.ink, 13.0, alpha = a)
            text((xf + xb) / 2, yd - 5, "3 m", c.ink, 13.0, alpha = a)
        }
    }
    CardText(L(
        "Podpora drží nosník jen tím, že na něj **působí silou**. Co se stane, když jednu odstraníme?",
        "Опора держит балку только тем, что **действует на неё силой**. Что будет, если одну убрать?",
    ))
    Steps(
        Step(
            L("Odstraň podporu B a sleduj, co udělá nosník", "Убери опору B и посмотри, что сделает балка"), phase >= 2,
            result = {
                Note(Tone.INFO, L(
                    "Bez podpory B se nosník otočil kolem A **čím dál rychleji** a dopadl na zem. Podpora B ho držela – tlačila na něj zespodu silou.",
                    "Без опоры B балка повернулась вокруг A **всё быстрее** и упала на землю. Опора B её держала — давила на неё снизу силой.",
                ))
            },
        ) {
            ActionButton(L("Odstraň podporu B", "Убрать опору B"), enabled = phase == 0) {
                phase = 1
                scope.launch {
                    // F i tíha táhnou pravý konec dolů – otáčí se kolem A čím dál rychleji
                    swing(4.0, landDeg) { tilt = it }
                    phase = 2
                }
            }
        },
        Step(L("Sleduj: kreslíme model – nosník bez podpor, jen se silami", "Смотри: рисуем модель — балка без опор, только с силами"), modelDone) {
            SmallText(L("Model je obrázek na papíře: podpory zmizí a na jejich místě budou síly.", "Модель — рисунок на бумаге: опоры исчезнут, а на их месте будут силы."))
        },
        qA.step7(),
        qB.step7(),
        nR.step7(L("F je 1 m od A, rozpětí 4 m. Spočítej obě reakce", "F в 1 м от A, пролёт 4 м. Найди обе реакции")),
    )
}

/** Jednoduchá ruka zespodu: dlaň, čtyři prsty a palec. */
private fun PrimBuilder.hand(x: Double, yTop: Double, c: StatikaColors, alpha: Float = 1f) {
    for (i in 0..3) rect(x - 15 + i * 7.5, yTop, 6.0, 22.0, c.subtle, c.inkSoft, 1.2, alpha)
    rect(x - 16, yTop + 20, 32.0, 30.0, c.subtle, c.inkSoft, 1.4, alpha)
    poly(listOf(x + 16 to yTop + 30, x + 28 to yTop + 18, x + 32 to yTop + 22, x + 16 to yTop + 42), c.subtle, c.inkSoft, 1.2, alpha)
    // předloktí dolů
    rect(x - 12, yTop + 50, 24.0, 40.0, c.subtle, c.inkSoft, 1.2, alpha)
}

/**
 * Karta 2: „Buď podporou“. Podpora B chybí, nosník drží žákova ruka silou P. Málo → konec klesá (zrychluje) až
 * na zem; moc → zvedá se k zarážce; přesně 4 kN → stojí. Pak žák odhadne, jak silně bude tlačit vrácená podpora.
 */
@Composable
private fun CardScope.BeSupport() {
    val t = tr()
    val y = 80.0
    val floor = 192.0
    val xa = 60.0
    val xb = 260.0
    val xEnd = 300.0
    val need = 4.0
    var p by remember { mutableStateOf(2f) }
    var running by remember { mutableStateOf(false) }
    var tilt by remember { mutableDoubleStateOf(0.0) }
    var fade by remember { mutableStateOf(1f) }
    // výsledek posledního pokusu: null = žádný, -1 = klesl, 1 = zvedl se, 0 = drží
    var result by remember { mutableStateOf<Int?>(null) }
    var held by remember { mutableStateOf(false) }
    var tries by remember { mutableStateOf(0) }
    // výměna ruky za podporu B (0 = ruka, 1 = podpora)
    val swap = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val job = remember { arrayOfNulls<Job>(1) }
    val qBack = rememberQuiz(
        L("Vrátíme podporu B. Jak velkou silou bude na nosník tlačit?", "Вернём опору B. С какой силой она будет давить на балку?"),
        listOf(
            Opt(L("4 kN – stejně jako ruka", "4 кН — как рука"), correct = true),
            Opt(
                L("8 kN – celou sílu F", "8 кН — всю силу F"),
                why = L("Polovinu F nese kloub A – to se vrácením podpory nezmění.", "Половину F несёт шарнир A — возвращение опоры этого не меняет."),
            ),
            Opt(
                L("nelze říct předem", "заранее не скажешь"),
                why = L("Nosník se nezměnil, zatížení taky ne. Co musí podpora udělat, aby stál?", "Балка не изменилась, нагрузка тоже. Что должна делать опора, чтобы балка стояла?"),
            ),
        ),
        explain = L(
            "Podpora tlačí na nosník stejně jako tvoje ruka: [[R_B]] = 4 kN. Reakce je **právě ta síla, kterou rovnováha potřebuje** – a tu budeš počítat.",
            "Опора давит на балку так же, как твоя рука: [[R_B]] = 4 кН. Реакция — **ровно та сила, которая нужна для равновесия**, — её ты и будешь вычислять.",
        ),
    )
    val returned = qBack.solved
    LaunchedEffect(returned) { if (returned) swap.animateTo(1f, tween(700)) }
    // dopad pravého konce na podlahu / nadzvednutí k zarážce (stupně)
    val downDeg = asin((floor - y - 5) / (xEnd - xa)) * 180 / PI
    val upDeg = asin(30.0 / (xEnd - xa)) * 180 / PI

    fun release() {
        if (running || held) return
        running = true
        result = null
        tries++
        val force = p.toDouble()
        job[0] = scope.launch {
            val diff = need - force
            if (abs(diff) < 0.01) {
                delay(1500)
                result = 0
                held = true
                running = false
                return@launch
            }
            // nevyrovnaný moment k A → úhlové zrychlení úměrné rozdílu
            swing(2.4 * diff / need, if (diff > 0) downDeg else upDeg) { tilt = it }
            result = if (diff > 0) -1 else 1
            delay(1100)
            // nový pokus: starý nosník zmizí, nový se objeví vodorovně
            for (i in 1..10) {
                fade = 1f - i / 10f
                delay(30)
            }
            tilt = 0.0
            for (i in 1..10) {
                fade = i / 10f
                delay(30)
            }
            running = false
        }
    }

    Stage(h = 222.0, description = t.pick(L("Nosník s kloubem A, místo podpory B ho drží ruka", "Балка с шарниром A, вместо опоры B её держит рука"))) { c ->
        // zarážka nad pravým koncem (aby se nosník při přetlačení nezvedl donekonečna)
        line(340.0, floor, 340.0, y - 44, c.inkSoft, 2.0)
        line(340.0, y - 44, 284.0, y - 44, c.inkSoft, 2.0)
        text(300.0, y - 52, t.pick(L("zarážka", "упор")), c.inkSoft, 11.0)
        rect(xa - 18, y + 33, 36.0, floor - y - 33, c.subtle, c.inkSoft, 1.2)
        support(SupportType.PIN, xa, y, c, "A")
        tiltedBeam(xa - 20, xEnd, y, xa, tilt, c, fade)
        val (fx, fy) = rot(160.0, y - 5, xa, y, tilt)
        force(fx, fy - 56, fx, fy, c.load, alpha = fade)
        text(fx + 8, fy - 42, " = 8 kN", c.load, 14.0, Anchor.START, sym = "F", alpha = fade)
        val (hx, hy) = rot(xb, y + 5, xa, y, tilt)
        val sw = swap.value
        if (sw > 0.5f) {
            // ruka se prolnula v podporu – ta tlačí stejně
            val a = (sw - 0.5f) * 2
            support(SupportType.ROLLER, xb, y, c, "B")
            rect(xb - 18, y + 36, 36.0, floor - y - 36, c.subtle, c.inkSoft, 1.2, alpha = a)
            force(xb + 34, y + 62, xb + 34, y + 7, c.react, glow = true, alpha = a)
            line(xb, y + 5, xb + 34, y + 5, c.react, 1.0, dash = true, alpha = a)
            text(xb + 44, y + 50, " = 4 kN", c.react, 14.0, Anchor.START, sym = "R_B", alpha = a)
        } else {
            val a = fade * (1f - sw * 2)
            hand(hx, hy + 1, c, a * 0.7f)
            force(hx, hy + 58, hx, hy + 1, c.react, width = 3.2, alpha = a)
            text(hx - 34, hy + 40, " = ${num(p.toDouble(), 1)} kN", c.react, 14.0, Anchor.END, sym = "P", alpha = a)
        }
        // podlaha zakryje ruku, která klesla pod ni
        rect(0.0, floor, 400.0, 40.0, c.paper)
        ground(10.0, 390.0, floor, c)
        val msg = when {
            returned -> null
            running && result == null && abs(tilt) < 0.01 -> L("držím…", "держу…")
            result == -1 -> L("málo – konec klesl až na zem", "мало — конец опустился до земли")
            result == 1 -> L("moc – nosník se zvedl k zarážce", "много — балка поднялась до упора")
            result == 0 -> L("drží! nosník stojí vodorovně", "держит! балка стоит горизонтально")
            else -> null
        }
        if (msg != null) text(200.0, floor + 24, t.pick(msg), if (result == 0) c.accent else c.inkSoft, 14.0, bold = result == 0)
    }
    CardText(L(
        "Podpora B zmizela – nosník teď držíš **ty**: ruka tlačí na jeho konec silou [[P]].",
        "Опоры B нет — балку теперь держишь **ты**: рука давит на её конец силой [[P]].",
    ))
    Steps(
        Step(
            L("Nastav sílu ruky tak, aby nosník po puštění zůstal vodorovně", "Подбери силу руки так, чтобы отпущенная балка осталась горизонтальной"), held,
            result = {
                Note(Tone.OK, L(
                    "[[P]] = 4 kN – přesně polovina F. Druhou polovinu nese kloub A.",
                    "[[P]] = 4 кН — ровно половина F. Вторую половину несёт шарнир A.",
                ))
            },
        ) {
            ValueSlider(t.pick(L("Ruka tlačí [[P]] = ${num(p.toDouble(), 1)} kN", "Рука давит [[P]] = ${num(p.toDouble(), 1)} кН")), p, 0f..8f, steps = 15) {
                if (!running) {
                    p = (it * 2).roundToInt() / 2f
                    if (result != 0) result = null
                }
            }
            ActionButton(L("Pusť – drž jen rukou", "Отпустить — держать только рукой"), enabled = !running) { release() }
            if (tries >= 3) SmallText(L(
                "Tip: F leží přesně uprostřed mezi A a B. Kolik z ní asi nese kloub A a kolik ruka?",
                "Подсказка: F лежит ровно посередине между A и B. Сколько из неё примерно несёт шарнир A, а сколько рука?",
            ))
        },
        qBack.step7(),
    )
}

/**
 * Karta 3: trám na dvou vahách, pod ním visí náklad 12 kN. Posun nákladu do strany mění údaje vah (součet je
 * vždy 12 kN), změna délky lana nemění nic – síla se posunula po své nositelce. Nakonec žák spočítá údaj váhy
 * pro novou polohu (váhy mezitím ukazují „?“).
 */
@Composable
private fun CardScope.TwoScales() {
    val t = tr()
    val y = 64.0
    val floor = 214.0
    val xa = 60.0
    val xb = 340.0
    val m = (xb - xa) / 4 // 70 jednotek na metr, rozpětí 4 m
    val g = 12.0
    val kn = t.pick(L("kN", "кН"))
    var xm by remember { mutableStateOf(2.0) } // poloha nákladu od A, m
    var rope by remember { mutableStateOf(40.0) }
    var dragging by remember { mutableStateOf(false) }
    var hitNine by remember { mutableStateOf(false) }
    var ropeGuess by remember { mutableStateOf<Int?>(null) }
    var ropeRef by remember { mutableStateOf(40.0) } // délka lana na začátku zkoušky
    var ropeTried by remember { mutableStateOf(false) }
    val scene = remember { Animatable(1f) }
    val nB = rememberNum(
        L("Kolik ukáže váha B?", "Сколько покажут весы B?"),
        listOf(NumSpec(L("B", "B"), 9.0, kn)),
        explain = L(
            "[[R_B]] · 4 = 12 · 3 → [[R_B]] = 9 kN, a na A zbude 3 kN. Zrcadlově k úkolu s 9 kN na A.",
            "[[R_B]] · 4 = 12 · 3 → [[R_B]] = 9 кН, а на A останется 3 кН. Зеркально к заданию с 9 кН на A.",
        ),
        traps = listOf(
            NumTrap.near(L("To by platilo, kdyby byl náklad blíž k A. Která váha je mu teď blíž?", "Так было бы, если бы груз был ближе к A. Какие весы теперь ближе к нему?"), 3.0),
            NumTrap.near(L("Celých 12 kN by B nesla, jen kdyby náklad visel přímo nad ní.", "Все 12 кН весы B несли бы, только если бы груз висел прямо над ними."), 12.0),
            NumTrap.near(L("Napůl se dělí jen uprostřed. Tady je náklad blíž k B.", "Пополам делится только посередине. Здесь груз ближе к B."), 6.0),
        ),
        nudge = L("Momenty k A: [[R_B]] · 4 m = 12 kN · 3 m.", "Моменты относительно A: [[R_B]] · 4 м = 12 кН · 3 м."),
    )
    // poslední krok: náklad je 3 m od A a váhy jsou zakryté, dokud žák neodpoví
    val asking = ropeTried && !nB.solved
    val ra = g * (4 - xm) / 4
    val rb = g * xm / 4
    LaunchedEffect(ra, hitNine) {
        if (!hitNine && abs(ra - 9.0) < 1e-6) {
            delay(500)
            hitNine = true
        }
    }
    LaunchedEffect(ropeTried) {
        if (ropeTried) {
            delay(1500)
            // nové zadání: starý obrázek zmizí, nový (náklad 3 m od A) se objeví
            scene.animateTo(0f, tween(250))
            xm = 3.0
            rope = 40.0
            scene.animateTo(1f, tween(300))
        }
    }
    fun moveX(v: Double) {
        if (asking) return
        xm = v.coerceIn(0.5, 3.5)
    }
    fun moveRope(v: Double) {
        if (asking || ropeGuess == null) return
        rope = v.coerceIn(14.0, 92.0)
        if (abs(rope - ropeRef) > 25) ropeTried = true
    }
    val bx = xa + xm * m
    Stage(
        h = 230.0,
        description = t.pick(L("Trám na dvou vahách, pod ním na laně náklad 12 kN", "Брус на двух весах, под ним на тросе груз 12 кН")),
        onDrag = { p, start ->
            if (start) dragging = !asking && p.dist(bx, y + 5 + rope + 20) < 40
            if (dragging) {
                moveX((((p.x - xa) / m) * 4).roundToInt() / 4.0)
                moveRope(p.y - y - 5 - 20)
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        val sa = scene.value
        // kozy s vahami
        for ((x, v, name) in listOf(Triple(xa, ra, "A"), Triple(xb, rb, "B"))) {
            line(x - 22, y + 50, x - 34, floor, c.inkSoft, 2.0)
            line(x + 22, y + 50, x + 34, floor, c.inkSoft, 2.0)
            rect(x - 30, y + 13, 60.0, 37.0, c.subtle, c.ink, 1.4)
            rect(x - 26, y + 5, 52.0, 8.0, c.beam, c.ink, 1.2)
            rect(x - 22, y + 19, 44.0, 20.0, c.paper, c.inkSoft, 1.0)
            text(x, y + 34, if (asking) "?" else num(v, 1), c.react, 14.0, bold = true, alpha = sa)
            text(x, y + 48, "kN", c.inkSoft, 9.0)
            text(if (name == "A") x - 36 else x + 36, y + 30, name, c.ink, 15.0, if (name == "A") Anchor.END else Anchor.START, bold = true)
        }
        ground(10.0, 390.0, floor, c)
        // trám (vytárované váhy – počítá se jen náklad)
        rect(xa - 30, y - 5, xb - xa + 60, 10.0, c.beam, c.ink, 1.6)
        // nositelka tíhy nákladu: svislá přímka přes lano
        line(bx, y - 40, bx, floor, c.inkSoft, 1.0, dash = true, alpha = sa)
        // lano a náklad (pytel s pískem)
        line(bx, y + 5, bx, y + 5 + rope, c.ink, 1.6, alpha = sa)
        val top = y + 5 + rope
        poly(listOf(bx - 20 to top + 4, bx + 20 to top + 4, bx + 24 to top + 40, bx - 24 to top + 40), c.subtle, c.ink, 1.5, sa)
        line(bx, top, bx - 14, top + 5, c.ink, 1.2, alpha = sa)
        line(bx, top, bx + 14, top + 5, c.ink, 1.2, alpha = sa)
        text(bx, top + 27, "12 kN", c.load, 12.0, bold = true, alpha = sa)
        if (!asking) handle(bx + 34, top + 22, c, dragging)
        if (ropeTried) {
            // poloha nákladu pro poslední otázku
            val yd = floor - 8
            line(xa, yd, xb, yd, c.ink, 0.9, alpha = sa)
            for (x in listOf(xa, bx, xb)) line(x - 4, yd + 4, x + 4, yd - 4, c.ink, 1.2, alpha = sa)
            text((xa + bx) / 2, yd - 5, "${num(xm)} m", c.ink, 12.0, alpha = sa)
            text((bx + xb) / 2, yd - 5, "${num(4 - xm)} m", c.ink, 12.0, alpha = sa)
        }
    }
    CardText(L(
        "Trám leží na dvou vahách (vytárovaných – ukazují jen náklad). Pod ním visí na laně pytel s pískem: 12 kN. Váhy ukazují přesně reakce podpor.",
        "Брус лежит на двух весах (обнулённых — показывают только груз). Под ним на тросе висит мешок с песком: 12 кН. Весы показывают ровно реакции опор.",
    ))
    val ropeOpts = listOf(L("totéž", "то же самое"), L("víc", "больше"), L("míň", "меньше"))
    val sum: @Composable () -> Unit = { Readout("A + B = ${num(ra, 2)} + ${num(rb, 2)} = 12 kN") }
    Steps(
        Step(
            L("Posuň náklad tak, aby levá váha A ukazovala 9 kN", "Сдвинь груз так, чтобы левые весы A показывали 9 кН"), hitNine,
            result = {
                Note(Tone.OK, L(
                    "Momenty k A: [[R_B]] · 4 m = 12 kN · 1 m → [[R_B]] = 3 kN, na A zbude 9 kN. Čím blíž je náklad k váze, tím víc z něj nese.",
                    "Моменты относительно A: [[R_B]] · 4 м = 12 кН · 1 м → [[R_B]] = 3 кН, на A остаётся 9 кН. Чем ближе груз к весам, тем больше они несут.",
                ))
            },
        ) {
            Chips(listOf(L("◀ 0,25 m", "◀ 0,25 м"), L("0,25 m ▶", "0,25 м ▶"))) { i -> moveX(xm + if (i == 0) -0.25 else 0.25) }
            sum()
        },
        tip7(L("Prodloužíme lano – náklad visí níž. Co ukážou váhy?", "Удлиним трос — груз повиснет ниже. Что покажут весы?"), ropeOpts, ropeGuess) {
            ropeGuess = it
            ropeRef = rope
        },
        Step(
            L("Změň délku lana a sleduj obě váhy", "Измени длину троса и следи за весами"), ropeTried,
            result = {
                Note(if (ropeGuess == 0) Tone.OK else Tone.INFO, L(
                    (if (ropeGuess == 0) "Tip sedí. " else "") + "Délka lana nic nemění: tíha nákladu se jen posunula **po své nositelce** (čárkovaná svislice). Záleží jen na tom, kde nositelka protíná trám.",
                    (if (ropeGuess == 0) "Прогноз верен. " else "") + "Длина троса ничего не меняет: сила тяжести груза лишь сдвинулась **вдоль своей линии действия** (пунктирная вертикаль). Важно только, где линия действия пересекает брус.",
                ))
            },
        ) {
            Chips(listOf(L("lano kratší", "трос короче"), L("lano delší", "трос длиннее"))) { i -> moveRope(rope + if (i == 0) -13.0 else 13.0) }
            sum()
        },
        nB.step7(L("Náklad je teď 3 m od A (1 m od B). Kolik ukáže váha B?", "Груз теперь в 3 м от A (1 м от B). Сколько покажут весы B?")),
    )
}

/**
 * Karta 4: akce a reakce – rozložený obrázek. Obě síly na jedné nositelce, každá na jiném tělese;
 * žák vybere síly, které působí na nosník, a výběr nechá zkontrolovat.
 */
@Composable
private fun CardScope.ActionReaction() {
    val t = tr()
    val lift = remember { Animatable(0f) }
    var split by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(setOf<Int>()) }
    var checked by remember { mutableStateOf<Set<Int>?>(null) }
    val scope = rememberCoroutineScope()
    val y = 150.0
    val xb = 200.0
    val correct = checked == setOf(0, 1)
    Stage(h = 230.0, description = t.pick(L("Nosník a podpora tlačí na sebe navzájem – rozložený obrázek", "Балка и опора давят друг на друга — разнесённый рисунок"))) { c ->
        val up = 88.0 * lift.value
        val by = y - up
        support(SupportType.PIN, xb, y, c, "")
        beam(80.0, 320.0, by, c)
        force(xb, by - 50, xb, by - 6, c.load, glow = 0 in picked)
        text(xb + 8, by - 34, " = 6 kN", c.load, 14.0, Anchor.START, sym = "F")
        if (lift.value > 0.5f) {
            val a = ((lift.value - 0.5f) / 0.5f).coerceIn(0f, 1f)
            val contact = y + 9.0
            val bottom = by + 5
            // obě síly na téže svislé nositelce x = xb; každá u „svého“ tělesa
            force(xb, bottom + 36, xb, bottom + 1, c.react, alpha = a, glow = 1 in picked)
            text(xb + 10, bottom + 26, t.pick(L("podpora → nosník: 6 kN", "опора → балка: 6 кН")), c.react, 13.0, Anchor.START, alpha = a)
            force(xb, contact - 36, xb, contact, c.inkSoft, alpha = a, glow = 2 in picked)
            text(xb + 10, contact - 16, t.pick(L("nosník → podpora: 6 kN", "балка → опора: 6 кН")), c.inkSoft, 13.0, Anchor.START, alpha = a)
            line(xb, bottom + 36, xb, contact - 36, c.inkSoft, 1.0, dash = true, alpha = a)
            text(12.0, 222.0, t.pick(L("rozložený obrázek – ve skutečnosti se tělesa dotýkají", "разнесённый рисунок — на самом деле тела соприкасаются")), c.derived, 12.0, Anchor.START, alpha = a)
        }
    }
    CardText(L(
        "Nosník tlačí na podporu dolů a podpora na nosník **stejně velkou** silou nahoru. Ve styku jsou obě síly schované – abychom je viděli, nakreslíme tělesa **od sebe**.",
        "Балка давит на опору вниз, а опора на балку — **такой же по величине** силой вверх. В месте контакта обе силы не видны — чтобы их увидеть, нарисуем тела **отдельно**.",
    ))
    Steps(
        Step(L("Rozlož obrázek a prohlédni si obě síly ve styku", "Разнеси рисунок и рассмотри обе силы в месте контакта"), split) {
            ActionButton(L("Rozlož obrázek", "Разнести рисунок"), enabled = !lift.isRunning) {
                scope.launch {
                    lift.animateTo(1f, tween(700))
                    split = true
                }
            }
        },
        Step(
            L("Počítáme nosník. Označ všechny síly, které působí **na něj**", "Считаем балку. Отметь все силы, которые действуют **на неё**"), correct,
            result = {
                Note(Tone.OK, L(
                    "**Akce a reakce** jsou dvě stejně velké opačné síly na jedné nositelce – každá ale působí na **jiné** těleso. Proto se v rovnicích nosníku nikdy navzájem nevyruší: šedá do nich vůbec nepatří.",
                    "**Действие и противодействие** — две равные противоположные силы на одной линии действия, но каждая действует на **своё** тело. Поэтому в уравнениях балки они никогда не сокращаются: серая туда вообще не входит.",
                ))
            },
        ) {
            Chips(
                listOf(L("zatížení [[F]]", "нагрузка [[F]]"), L("podpora → nosník", "опора → балка"), L("nosník → podpora", "балка → опора")),
                selectedSet = picked,
            ) { i ->
                picked = if (i in picked) picked - i else picked + i
                checked = null
            }
            ActionButton(L("Zkontrolovat (vybráno ${picked.size})", "Проверить (выбрано ${picked.size})"), enabled = picked.isNotEmpty()) { checked = picked }
            val ch = checked
            when {
                ch == null -> {}
                2 in ch -> Note(Tone.NUDGE, L(
                    "Šedá šipka tlačí **na podporu** (míří do ní). Na nosník nepůsobí – do jeho rovnic nepatří.",
                    "Серая стрелка давит **на опору** (направлена в неё). На балку она не действует — в её уравнения не входит.",
                ))
                0 !in ch -> Note(Tone.NUDGE, L("Něco chybí: co tlačí na nosník **shora**?", "Чего-то не хватает: что давит на балку **сверху**?"))
                1 !in ch -> Note(Tone.NUDGE, L("Něco chybí: co drží nosník **zespodu**?", "Чего-то не хватает: что держит балку **снизу**?"))
            }
        },
    )
}

/** Kotvy pod podporou: šrouby do základu – podpora umí nosník i **držet dole**. */
private fun PrimBuilder.anchors(x: Double, y: Double, c: StatikaColors) {
    for (dx in listOf(-12.0, 12.0)) {
        line(x + dx, y + 30, x + dx, y + 50, c.ink, 2.0)
        line(x + dx - 4, y + 50, x + dx + 4, y + 50, c.ink, 2.0)
    }
}

/**
 * Karta 5: smysl reakce si zvolíš, R_A spočítá žák; záporný výsledek jen otočí šipku.
 * Nakonec pokus „bez A“ ukáže proč: levý konec by se zvedl.
 */
@Composable
private fun CardScope.GuessSense() {
    val t = tr()
    val kn = t.pick(L("kN", "кН"))
    var sel by remember { mutableStateOf<Int?>(null) } // vybraný smysl před potvrzením
    var guess by remember { mutableStateOf<Int?>(null) } // 0 = ↑, 1 = ↓
    val flip = remember { Animatable(0f) }
    // „co kdyby A nebyla“: nosník se otočí kolem B, levý konec se zvedne k zarážce
    var whatIf by remember { mutableStateOf(false) }
    var whatIfSeen by remember { mutableStateOf(false) }
    var tilt by remember { mutableDoubleStateOf(0.0) }
    var fade by remember { mutableStateOf(1f) }
    val scope = rememberCoroutineScope()
    val up = (guess ?: sel) != 1
    val nA = rememberNum(
        L("R_A, smysl ${guess ?: -1}", "R_A, smysl ${guess ?: -1}"),
        listOf(NumSpec(L("[[R_A]]", "[[R_A]]"), if (guess == 1) 4.0 else -4.0, kn, signed = true)),
        explain = if (guess == 1) L(
            "[[R_A]]·2 − 4·2 = 0 → [[R_A]] = 4 kN.",
            "[[R_A]]·2 − 4·2 = 0 → [[R_A]] = 4 кН.",
        ) else L(
            "−[[R_A]]·2 − 4·2 = 0 → [[R_A]] = −8/2 = −4 kN.",
            "−[[R_A]]·2 − 4·2 = 0 → [[R_A]] = −8/2 = −4 кН.",
        ),
        traps = listOf(
            if (guess == 1) NumTrap(L(
                "Dosaď −4 zpátky: (−4)·2 − 8 = −16, ne 0. Jaké znaménko musí [[R_A]] mít?",
                "Подставь −4 обратно: (−4)·2 − 8 = −16, а не 0. Какой знак должен быть у [[R_A]]?",
            )) { v -> v[0]?.let { abs(it + 4) < 0.02 } == true }
            else NumTrap(L(
                "Velikost sedí, ale dosaď zpátky: −4·2 − 8 = −16, ne 0. Jaké znaménko musí [[R_A]] mít?",
                "Величина верная, но подставь обратно: −4·2 − 8 = −16, а не 0. Какой знак должен быть у [[R_A]]?",
            )) { v -> v[0]?.let { abs(it - 4) < 0.02 } == true },
            NumTrap.near(L("8 kNm je moment F k bodu B (4 · 2). Ještě vyděl ramenem [[R_A]], 2 m.", "8 кН·м — это момент F относительно B (4 · 2). Ещё раздели на плечо [[R_A]], 2 м."), 8.0),
        ),
        nudge = L("[[R_A]] i F mají k bodu B rameno 2 m. Vyřeš rovnici pro [[R_A]].", "У [[R_A]] и у F плечо относительно B — 2 м. Реши уравнение относительно [[R_A]]."),
    )
    val computed = nA.solved
    val qUp = rememberQuiz(
        L("Vyšlo [[R_A]] = −4 kN. Co ten minus znamená?", "Получилось [[R_A]] = −4 кН. Что означает минус?"),
        listOf(
            Opt(L("chybu ve výpočtu", "ошибку в расчёте"), why = L("Rovnice platí, výpočet je v pořádku. Minus nese informaci o **smyslu**.", "Уравнение выполняется, расчёт верный. Минус несёт информацию о **направлении**.")),
            Opt(L("reakce míří opačně – dolů", "реакция направлена наоборот — вниз"), correct = true),
            Opt(L("reakce je nulová", "реакция равна нулю"), why = L("Nulová by vyšla 0. Velikost je 4 kN – jen znaménko je záporné.", "Нулевая дала бы 0. Величина 4 кН — отрицателен только знак.")),
        ),
        explain = L(
            "Minus není chyba! Jen říká, že reakce ve skutečnosti míří **opačně**, než sis zvolil: podpora A táhne nosník dolů (brání mu v nadzvednutí).",
            "Минус — не ошибка! Он только говорит, что на самом деле реакция направлена **противоположно** выбранному: опора A тянет балку вниз (не даёт ей приподняться).",
        ),
    )
    val qDown = rememberQuiz(
        L("Vyšlo +4 kN. Co by vyšlo, kdybys zvolil smysl nahoru?", "Получилось +4 кН. Что получилось бы, выбери ты направление вверх?"),
        listOf(
            Opt(L("+4 kN", "+4 кН"), why = L("Se smyslem nahoru se v rovnici otočí znaménko členu s [[R_A]]: −[[R_A]]·2 − 8 = 0.", "При направлении вверх в уравнении меняется знак члена с [[R_A]]: −[[R_A]]·2 − 8 = 0.")),
            Opt(L("−4 kN", "−4 кН"), correct = true),
            Opt(L("0", "0"), why = L("Podpora pořád drží stejně – velikost zůstane 4 kN.", "Опора держит так же — величина останется 4 кН.")),
        ),
        explain = L(
            "Vyšlo kladně – tipl sis dobře, podpora A táhne nosník dolů. Kdybys zvolil nahoru, vyšlo by −4 kN: stejná síla, jen s opačným znaménkem.",
            "Получилось положительное число — ты угадал: опора A тянет балку вниз. Если бы ты выбрал «вверх», вышло бы −4 кН: та же сила, только с другим знаком.",
        ),
    )
    LaunchedEffect(qUp.solved) {
        if (qUp.solved && guess == 0) {
            delay(300)
            flip.animateTo(1f, tween(700))
        }
    }
    val y = 100.0
    // A v 0 m, B ve 2 m, F = 4 kN na konci převislého konce ve 4 m; 1 m = 70 jednotek
    val x0 = 60.0
    val m = 70.0
    val xB = x0 + 2 * m
    val liftDeg = asin(34.0 / (2 * m + 10)) * 180 / PI
    Stage(h = 220.0, description = t.pick(L("Nosník s převislým koncem, směr reakce v bodě A si volíš", "Балка с консолью, направление реакции в точке A выбираешь сам"))) { c ->
        if (whatIf) {
            // zarážka nad levým koncem
            line(x0 - 30, y - 44, x0 + 10, y - 44, c.inkSoft, 2.0)
            line(x0 - 30, y - 44, x0 - 30, y - 70, c.inkSoft, 2.0)
            support(SupportType.PIN, x0, y, c, "A", ghost = true)
            support(SupportType.ROLLER, xB, y, c, "B")
            tiltedBeam(x0 - 10, x0 + 4 * m + 10, y, xB, tilt, c, fade)
            val (fx, fy) = rot(x0 + 4 * m, y - 5, xB, y, tilt)
            force(fx, fy - 56, fx, fy, c.load, alpha = fade)
            text(fx - 8, fy - 42, " = 4 kN", c.load, 14.0, Anchor.END, sym = "F", alpha = fade)
            if (abs(tilt) > 1) text(x0 + 30, y - 52, t.pick(L("levý konec se zvedá!", "левый конец поднимается!")), c.load, 13.0, Anchor.START, alpha = fade)
            return@Stage
        }
        beam(x0 - 10, x0 + 4 * m + 10, y, c)
        support(SupportType.PIN, x0, y, c, "A", ghost = true)
        anchors(x0, y, c)
        text(x0 - 18, y + 50, t.pick(L("kotvy", "анкеры")), c.inkSoft, 11.0, Anchor.END)
        support(SupportType.ROLLER, xB, y, c, "B", ghost = true)
        force(x0 + 4 * m, y - 64, x0 + 4 * m, y - 5, c.load)
        text(x0 + 4 * m - 8, y - 50, " = 4 kN", c.load, anchor = Anchor.END, sym = "F")
        force(xB, y + 62, xB, y + 6, c.react)
        text(xB + 12, y + 50, if (computed) " = 8 kN" else null, c.react, 14.0, Anchor.START, sym = "R_B")
        if (guess != null || sel != null) {
            val f = flip.value.toDouble()
            val drawUp = up && f < 0.5
            if (drawUp) force(x0, y + 62, x0, y + 6, c.react)
            else force(x0, y + 6, x0, y + 62, c.react, glow = up && f > 0.5)
            val v = when {
                !computed -> " = ?"
                up && f > 0.5 -> " = 4 kN ↓"
                up -> " = −4 kN"
                else -> " = 4 kN"
            }
            text(x0 + 12, y + 50, v, c.react, 14.0, Anchor.START, sym = "R_A")
        }
        val yd = y + 84
        line(x0, yd, x0 + 4 * m, yd, c.ink, 0.9)
        for (k in 0..2) line(x0 + k * 2 * m - 4, yd + 4, x0 + k * 2 * m + 4, yd - 4, c.ink, 1.2)
        text(x0 + m, yd - 5, "2 m", c.ink, 13.0)
        text(x0 + 3 * m, yd - 5, "2 m", c.ink, 13.0)
    }
    CardText(L(
        "Kam míří reakce v A, předem nevíš. Smysl si proto **zvol** – výpočet ukáže, jestli byl správný. Podpora A je ukotvená šrouby do základu: umí nosník tlačit nahoru i držet dolů.",
        "Куда направлена реакция в A, заранее неизвестно. Поэтому направление **выбери сам** — расчёт покажет, было ли оно верным. Опора A закреплена анкерами в фундаменте: может и толкать балку вверх, и удерживать её вниз.",
    ))
    val dirs = listOf(L("[[R_A]] nahoru ↑", "[[R_A]] вверх ↑"), L("[[R_A]] dolů ↓", "[[R_A]] вниз ↓"))
    Steps(
        Step(
            L("Zvol smysl reakce [[R_A]] – kam ji nakreslíš?", "Выбери направление реакции [[R_A]] — куда её нарисуешь?"), guess != null,
            result = { guess?.let { SmallText(L("Tvoje volba: " + dirs[it].cs, "Твой выбор: " + dirs[it].ru)) } },
        ) {
            Chips(dirs, sel) { sel = it }
            ActionButton(L("Nakreslit šipku", "Нарисовать стрелку"), enabled = sel != null) { guess = sel }
        },
        nA.step7(
            L("Momenty k bodu B (↺ je +): kolik vyjde [[R_A]]?", "Моменты относительно B (↺ — плюс): чему равна [[R_A]]?"),
            show = { Readout(if (guess == 1) "Σ[[M_B]] = 0:  [[R_A]]·2 − 4·2 = 0" else "Σ[[M_B]] = 0:  −[[R_A]]·2 − 4·2 = 0") },
            more = {
                Readout(
                    if (guess == 1) "Σ[[F_y]] = 0:  −[[R_A]] + [[R_B]] − 4 = 0  →  [[R_B]] = 8 kN"
                    else "Σ[[F_y]] = 0:  [[R_A]] + [[R_B]] − 4 = 0  →  [[R_B]] = 8 kN",
                )
            },
        ),
        (if (guess == 1) qDown else qUp).step7(columns = if (guess == 1) 3 else 1),
        Step(
            L("Ověř to pokusem: odeber podporu A a sleduj levý konec", "Проверь опытом: убери опору A и следи за левым концом"), whatIfSeen,
            result = {
                Note(Tone.OK, L(
                    "Bez A by se levý konec zvedl. Kotvená podpora A mu v tom brání – proto ho drží **dolů**: [[R_A]] míří dolů.",
                    "Без A левый конец поднялся бы. Закреплённая опора A этому мешает — поэтому держит его **вниз**: [[R_A]] направлена вниз.",
                ))
                SmallText(L(
                    "V trenažéru proto kreslíme všechny reakce v kladném smyslu (→ ↑ ↺). Znaménko výsledku pak řekne, kam opravdu míří.",
                    "Поэтому в тренажёре все реакции рисуем в положительном направлении (→ ↑ ↺). Знак результата покажет, куда они направлены на самом деле.",
                ))
            },
        ) {
            ActionButton(L("Odeber podporu A", "Убрать опору A"), enabled = !whatIf) {
                whatIf = true
                scope.launch {
                    tilt = 0.0
                    fade = 1f
                    // F na převislém konci otáčí nosník kolem B: pravý konec dolů, levý nahoru – čím dál rychleji
                    swing(3.0, liftDeg) { tilt = it }
                    delay(1500)
                    whatIfSeen = true
                    // pokus skončil: zvednutý nosník zmizí, model se vrátí
                    for (i in 1..10) {
                        fade = 1f - i / 10f
                        delay(30)
                    }
                    whatIf = false
                    tilt = 0.0
                    fade = 1f
                }
            }
        },
    )
}

/** Karta 6: kontrolní otázka. */
@Composable
private fun CardScope.Check() {
    val solved = solvedHere
    Stage(h = 170.0, description = L("Posuvná kloubová podpora B", "Шарнирно-подвижная опора B").t()) { c ->
        beam(60.0, 340.0, 80.0, c)
        support(SupportType.PIN, 80.0, 80.0, c, "A", ghost = true)
        support(SupportType.ROLLER, 320.0, 80.0, c, "B", ghost = solved)
        // po správné odpovědi místo podpory její reakce
        if (solved) {
            force(320.0, 80.0 + 60, 320.0, 80.0 + 6, c.react)
            text(312.0, 80.0 + 50, null, c.react, 14.0, Anchor.END, sym = "R_B")
        }
    }
    Steps(
        quizStep(
            question = L("Co nakreslíš místo posuvné kloubové podpory B?", "Что нарисуешь вместо шарнирно-подвижной опоры B?"),
            options = listOf(
                Opt(
                    L("dvě síly [[R_Bx]] a [[R_By]]", "две силы [[R_Bx]] и [[R_By]]"),
                    why = L("Válečky se kutálejí vodorovně. Může podpora v tom směru bránit pohybu?", "Катки катятся по горизонтали. Может ли опора препятствовать движению в этом направлении?"),
                ),
                Opt(L("jednu sílu [[R_B]] kolmo k podložce", "одну силу [[R_B]] перпендикулярно опорной поверхности"), correct = true),
                Opt(
                    L("nic – podpora prostě zmizí", "ничего — опора просто исчезает"),
                    why = L("A co pak drží pravý konec nahoře? Vzpomeň si na padající nosník.", "А что тогда будет держать правый конец? Вспомни падающую балку."),
                ),
            ),
            explain = L(
                "Posuvná kloubová podpora brání jen posunu kolmo k podložce – proto jediná síla [[R_B]]. Takhle uvolníš každou vazbu: místo ní nakreslíš právě ty reakce, kterými brání pohybu.",
                "Шарнирно-подвижная опора препятствует только смещению перпендикулярно опорной поверхности — поэтому одна сила [[R_B]]. Так освобождают от любой связи: вместо неё рисуют именно те реакции, которыми она препятствует движению.",
            ),
        ),
    )
}

internal fun lesson7() = Lesson(
    id = 7,
    title = L("Uvolnění", "Освобождение от связей"),
    summary = L(
        "Bez podpory nosník spadne. Podporu nahradíš silou – reakcí; najdeš ji rukou i na vahách.",
        "Без опоры балка падает. Опору заменяешь силой — реакцией; найдёшь её рукой и на весах.",
    ),
    cards = listOf(
        Card { CutItFree() },
        Card { BeSupport() },
        Card { TwoScales() },
        Card { ActionReaction() },
        Card { GuessSense() },
        Card { Check() },
    ),
)
