package cz.usharik.statika.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
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
 * Pak „uvolnění“ jako model: nový obrázek, podpory nahradí žák správnými reakcemi (F = 12 kN v ¼ rozpětí → 9 a 3 kN).
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
    // 0 = stojí, 1 = padá, 2 = leží na zemi, 3 = model (uvolnění)
    var phase by remember { mutableStateOf(0) }
    var tilt by remember { mutableDoubleStateOf(0.0) }
    val modelIn = remember { Animatable(0f) }
    var pickA by remember { mutableStateOf<Int?>(null) }
    var pickB by remember { mutableStateOf<Int?>(null) }
    val growA = remember { Animatable(0f) }
    val growB = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val aOk = pickA == 1
    val bOk = pickB == 0
    val both = aOk && bOk
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
            support(SupportType.PIN, xa, y, c, "A", ghost = aOk)
            support(SupportType.ROLLER, xb, y, c, "B", ghost = bOk)
            beam(xa - 20, xb + 20, y, c, a)
            force(xf, y - 56, xf, y - 5, c.load, alpha = a)
            text(xf + 8, y - 42, " = 12 kN", c.load, 14.0, Anchor.START, sym = "F", alpha = a)
            // reakce v A
            if (pickA != null) {
                val g = growA.value
                val gd = g.toDouble()
                force(xa, y + 6 + 55 * gd, xa, y + 6, c.react, alpha = g)
                text(xa + 10, y + 56, if (both) " = 9 kN" else null, c.react, 14.0, Anchor.START, sym = "R_Ay", alpha = g)
                if (pickA == 0) {
                    // chybí vodorovná reakce – naznačit, kde
                    force(xa - 66, y, xa - 22, y, c.load, dash = true, alpha = g)
                    text(xa - 44, y - 10, "?", c.load, 16.0, bold = true, alpha = g)
                } else {
                    force(xa - 22 - 44 * gd, y, xa - 22, y, c.react, alpha = g)
                    text(xa - 44, y - 10, if (both) " = 0" else null, c.react, 14.0, sym = "R_Ax", alpha = g)
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
                text(xb - 10, y + 56, if (both) " = 3 kN" else null, c.react, 14.0, Anchor.END, sym = if (pickB == 1) "R_By" else "R_B", alpha = g)
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
        "Podpora drží nosník jen tím, že na něj **působí silou**. Odstraň podporu B a sleduj, co se stane. Pak nosník **uvolni**: na novém obrázku nahraď podpory silami, kterými na nosník působí.",
        "Опора держит балку только тем, что **действует на неё силой**. Убери опору B и посмотри, что будет. Потом **освободи** балку: на новом рисунке замени опоры силами, с которыми они действуют на балку.",
    ))
    Todo(
        L("Odstraň podporu B a sleduj, co udělá nosník", "Убери опору B и посмотри, что сделает балка") to (phase >= 2),
        L("V modelu nahraď podporu **A** správnými reakcemi", "В модели замени опору **A** правильными реакциями") to aOk,
        L("Nahraď podporu **B** správnou reakcí", "Замени опору **B** правильной реакцией") to bOk,
    )
    when (phase) {
        0 -> ActionButton(L("Odstraň podporu B", "Убрать опору B")) {
            phase = 1
            scope.launch {
                // F i tíha táhnou pravý konec dolů – otáčí se kolem A čím dál rychleji
                swing(4.0, landDeg) { tilt = it }
                phase = 2
            }
        }
        1 -> {}
        2 -> {
            Note(Tone.NUDGE, L(
                "Bez podpory B se nosník otočil kolem A **čím dál rychleji** a dopadl na zem. Podpora B ho držela – tlačila na něj zespodu silou.",
                "Без опоры B балка повернулась вокруг A **всё быстрее** и упала на землю. Опора B её держала — давила на неё снизу силой.",
            ))
            ActionButton(L("Nakresli model – uvolni nosník", "Нарисовать модель — освободить балку")) {
                phase = 3
                scope.launch { modelIn.animateTo(1f, tween(600)) }
            }
        }
        else -> {
            SmallText(L("Podpora **A** (pevná kloubová) – co nakreslíš místo ní?", "Опора **A** (шарнирно-неподвижная) — что нарисуешь вместо неё?"))
            Chips(listOf(L("↑ jen [[R_Ay]]", "↑ только [[R_Ay]]"), L("→ ↑ [[R_Ax]] a [[R_Ay]]", "→ ↑ [[R_Ax]] и [[R_Ay]]"), L("→ ↑ ↺ [[R_Ax]], [[R_Ay]], [[M_A]]", "→ ↑ ↺ [[R_Ax]], [[R_Ay]], [[M_A]]")), pickA) {
                if (aOk) return@Chips
                pickA = it
                scope.launch {
                    growA.snapTo(0f)
                    growA.animateTo(1f, tween(500))
                }
            }
            when (pickA) {
                0 -> Note(Tone.NUDGE, L(
                    "Něco chybí (červený otazník). Pevná kloubová podpora nedovolí ani posun **do strany** – i ve vodorovném směru na nosník může působit silou.",
                    "Чего-то не хватает (красный вопрос). Шарнирно-неподвижная опора не даёт сдвинуться и **вбок** — и в горизонтальном направлении она может действовать на балку силой.",
                ))
                2 -> Note(Tone.NUDGE, L(
                    "Moment navíc! Kloub otáčení dovolí, takže momentem nebrání. Moment by kreslilo jen vetknutí.",
                    "Лишний момент! Шарнир поворачиваться позволяет, значит, моментом не препятствует. Момент рисуют только у заделки.",
                ))
                else -> {}
            }
            SmallText(L("Podpora **B** (posuvná kloubová) – co nakreslíš místo ní?", "Опора **B** (шарнирно-подвижная) — что нарисуешь вместо неё?"))
            Chips(listOf(L("↑ jen [[R_B]]", "↑ только [[R_B]]"), L("← ↑ [[R_Bx]] a [[R_By]]", "← ↑ [[R_Bx]] и [[R_By]]")), pickB) {
                if (bOk) return@Chips
                pickB = it
                scope.launch {
                    growB.snapTo(0f)
                    growB.animateTo(1f, tween(500))
                }
            }
            if (pickB == 1) Note(Tone.NUDGE, L(
                "Vodorovná síla navíc. Válečky se do strany volně kutálejí – vodorovně podpora B nebrání, takže tam žádnou sílu nemá.",
                "Лишняя горизонтальная сила. Катки свободно катятся вбок — по горизонтали опора B не препятствует, значит, силы там нет.",
            ))
            if (both) {
                Readout("Σ[[M_A]] = 0:  [[R_B]]·4 − 12·1 = 0  →  [[R_B]] = 3 kN")
                Readout("Σ[[F_y]] = 0:  [[R_Ay]] + [[R_B]] − 12 = 0  →  [[R_Ay]] = 9 kN")
                Readout("Σ[[F_x]] = 0:  [[R_Ax]] = 0")
                Note(Tone.OK, L(
                    "Tohle je **uvolnění**: nosník nikam nepadá – podpory jsme na papíře nahradili silami, kterými na nosník působí. F je blíž k A, proto A nese víc (9 kN) než B (3 kN).",
                    "Это и есть **освобождение от связей**: балка никуда не падает — опоры мы на бумаге заменили силами, с которыми они действуют на балку. F ближе к A, поэтому A несёт больше (9 кН), чем B (3 кН).",
                ))
            }
        }
    }
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
 * na zem; moc → zvedá se k zarážce; přesně 4 kN → stojí. Pak se ruka vymění za podporu se stejnou reakcí.
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
    var returned by remember { mutableStateOf(false) }
    var tries by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val job = remember { arrayOfNulls<Job>(1) }
    // dopad pravého konce na podlahu / nadzvednutí k zarážce (stupně)
    val downDeg = asin((floor - y - 5) / (xEnd - xa)) * 180 / PI
    val upDeg = asin(30.0 / (xEnd - xa)) * 180 / PI

    fun release() {
        if (running || returned) return
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
        if (returned) {
            support(SupportType.ROLLER, xb, y, c, "B")
            rect(xb - 18, y + 36, 36.0, floor - y - 36, c.subtle, c.inkSoft, 1.2)
            force(xb + 34, y + 62, xb + 34, y + 7, c.react, glow = true)
            line(xb, y + 5, xb + 34, y + 5, c.react, 1.0, dash = true)
            text(xb + 44, y + 50, " = 4 kN", c.react, 14.0, Anchor.START, sym = "R_B")
        } else {
            hand(hx, hy + 1, c, fade * 0.7f)
            force(hx, hy + 58, hx, hy + 1, c.react, width = 3.2, alpha = fade)
            text(hx - 34, hy + 40, " = ${num(p.toDouble(), 1)} kN", c.react, 14.0, Anchor.END, sym = "P", alpha = fade)
        }
        // podlaha zakryje ruku, která klesla pod ni
        rect(0.0, floor, 400.0, 40.0, c.paper)
        ground(10.0, 390.0, floor, c)
        val msg = when {
            running && result == null && abs(tilt) < 0.01 -> L("držím…", "держу…")
            result == -1 -> L("málo – konec klesl až na zem", "мало — конец опустился до земли")
            result == 1 -> L("moc – nosník se zvedl k zarážce", "много — балка поднялась до упора")
            result == 0 -> L("drží! nosník stojí vodorovně", "держит! балка стоит горизонтально")
            else -> null
        }
        if (msg != null) text(200.0, floor + 24, t.pick(msg), if (result == 0) c.accent else c.inkSoft, 14.0, bold = result == 0)
    }
    CardText(L(
        "Podpora B zmizela – nosník teď držíš **ty**: ruka tlačí na konec silou [[P]]. Nastav sílu a pusť. Když je moc malá, konec klesne; moc velká – nosník se zvedne. Najdi sílu, při které zůstane **vodorovně**.",
        "Опоры B нет — балку теперь держишь **ты**: рука давит на конец силой [[P]]. Задай силу и отпусти. Если она слишком мала, конец опустится; слишком велика — балка поднимется. Найди силу, при которой она останется **горизонтальной**.",
    ))
    Todo(
        L("Najdi sílu ruky [[P]], při které nosník zůstane vodorovně", "Найди силу руки [[P]], при которой балка останется горизонтальной") to held,
        L("Vrať podporu B a podívej se na její reakci", "Верни опору B и посмотри на её реакцию") to returned,
    )
    if (!returned) {
        ValueSlider(t.pick(L("Ruka tlačí [[P]] = ${num(p.toDouble(), 1)} kN", "Рука давит [[P]] = ${num(p.toDouble(), 1)} кН")), p, 0f..8f, steps = 15) {
            if (!running) {
                p = (it * 2).roundToInt() / 2f
                if (result != 0) result = null
            }
        }
        ActionButton(L("Pusť – drž jen rukou", "Отпустить — держать только рукой"), enabled = !running && !held) { release() }
    }
    if (tries >= 3 && !held) SmallText(L(
        "Tip: F leží přesně uprostřed mezi A a B. Kolik z ní asi nese kloub A a kolik ruka?",
        "Подсказка: F лежит ровно посередине между A и B. Сколько из неё примерно несёт шарнир A, а сколько рука?",
    ))
    if (held && !returned) {
        Note(Tone.OK, L(
            "[[P]] = 4 kN – přesně polovina F. Druhou polovinu nese kloub A.",
            "[[P]] = 4 кН — ровно половина F. Вторую половину несёт шарнир A.",
        ))
        ActionButton(L("Vrať podporu B", "Вернуть опору B")) { returned = true }
    }
    if (returned) Note(Tone.OK, L(
        "Podpora tlačí na nosník stejně jako tvoje ruka: [[R_B]] = 4 kN. Reakce je **právě ta síla, kterou rovnováha potřebuje** – a tu budeš počítat.",
        "Опора давит на балку так же, как твоя рука: [[R_B]] = 4 кН. Реакция — **ровно та сила, которая нужна для равновесия**, — её ты и будешь вычислять.",
    ))
}

/**
 * Karta 3: trám na dvou vahách, pod ním visí náklad 12 kN. Posun nákladu do strany mění údaje vah (součet je
 * vždy 12 kN), změna délky lana nemění nic – síla se posunula po své nositelce.
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
    var xm by remember { mutableStateOf(2.0) } // poloha nákladu od A, m
    var rope by remember { mutableStateOf(40.0) }
    var dragging by remember { mutableStateOf(false) }
    var ropeTried by remember { mutableStateOf(false) }
    var ropeStart by remember { mutableStateOf(40.0) }
    var hitNine by remember { mutableStateOf(false) }
    val ra = g * (4 - xm) / 4
    val rb = g * xm / 4
    LaunchedEffect(ra) { if (abs(ra - 9.0) < 1e-6) hitNine = true }
    val bx = xa + xm * m
    Stage(
        h = 230.0,
        description = t.pick(L("Trám na dvou vahách, pod ním na laně náklad 12 kN", "Брус на двух весах, под ним на тросе груз 12 кН")),
        onDrag = { p, start ->
            if (start) {
                dragging = p.dist(bx, y + 5 + rope + 20) < 40
                ropeStart = rope
            }
            if (dragging) {
                xm = (((p.x - xa) / m) * 4).roundToInt() / 4.0
                xm = xm.coerceIn(0.5, 3.5)
                rope = (p.y - y - 5 - 20).coerceIn(14.0, 92.0)
                if (abs(rope - ropeStart) > 24) ropeTried = true
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        // kozy s vahami
        for ((x, v, name) in listOf(Triple(xa, ra, "A"), Triple(xb, rb, "B"))) {
            line(x - 22, y + 50, x - 34, floor, c.inkSoft, 2.0)
            line(x + 22, y + 50, x + 34, floor, c.inkSoft, 2.0)
            rect(x - 30, y + 13, 60.0, 37.0, c.subtle, c.ink, 1.4)
            rect(x - 26, y + 5, 52.0, 8.0, c.beam, c.ink, 1.2)
            rect(x - 22, y + 19, 44.0, 20.0, c.paper, c.inkSoft, 1.0)
            text(x, y + 34, num(v, 1), c.react, 14.0, bold = true)
            text(x, y + 48, "kN", c.inkSoft, 9.0)
            text(if (name == "A") x - 36 else x + 36, y + 30, name, c.ink, 15.0, if (name == "A") Anchor.END else Anchor.START, bold = true)
        }
        ground(10.0, 390.0, floor, c)
        // trám (vytárované váhy – počítá se jen náklad)
        rect(xa - 30, y - 5, xb - xa + 60, 10.0, c.beam, c.ink, 1.6)
        // nositelka tíhy nákladu: svislá přímka přes lano
        line(bx, y - 40, bx, floor, c.inkSoft, 1.0, dash = true)
        // lano a náklad (big bag s pískem)
        line(bx, y + 5, bx, y + 5 + rope, c.ink, 1.6)
        val top = y + 5 + rope
        poly(listOf(bx - 20 to top + 4, bx + 20 to top + 4, bx + 24 to top + 40, bx - 24 to top + 40), c.subtle, c.ink, 1.5)
        line(bx, top, bx - 14, top + 5, c.ink, 1.2)
        line(bx, top, bx + 14, top + 5, c.ink, 1.2)
        text(bx, top + 27, "12 kN", c.load, 12.0, bold = true)
        handle(bx + 34, top + 22, c, dragging)
    }
    CardText(L(
        "Trám leží na dvou vahách (vytárovaných – ukazují jen náklad). Pod ním visí na laně big bag s pískem: 12 kN. Táhni náklad za kroužek **do strany** i **nahoru a dolů** a sleduj váhy. Váhy ukazují přesně reakce podpor.",
        "Брус лежит на двух весах (обнулённых — показывают только груз). Под ним на тросе висит биг-бэг с песком: 12 кН. Тяни груз за кружок **вбок**, а также **вверх и вниз** и следи за весами. Весы показывают ровно реакции опор.",
    ))
    Todo(
        L("Posuň náklad tak, aby levá váha A ukazovala **9 kN**", "Сдвинь груз так, чтобы левые весы A показывали **9 кН**") to hitNine,
        L("Změň délku lana (táhni nahoru/dolů) – změní se údaje vah?", "Измени длину троса (тяни вверх/вниз) — изменятся ли показания весов?") to ropeTried,
    )
    Readout("A + B = ${num(ra, 2)} + ${num(rb, 2)} = 12 kN")
    SmallText(L(
        "Momenty k A: [[R_B]] · 4 m = 12 kN · ${num(xm, 2)} m → [[R_B]] = ${num(rb, 2)} kN. Čím blíž je náklad k váze, tím víc z něj nese.",
        "Моменты относительно A: [[R_B]] · 4 м = 12 кН · ${num(xm, 2)} м → [[R_B]] = ${num(rb, 2)} кН. Чем ближе груз к весам, тем больше они его несут.",
    ))
    if (ropeTried) Note(Tone.INFO, L(
        "Délka lana nic nemění: tíha nákladu se jen posunula **po své nositelce** (čárkovaná svislice). Záleží jen na tom, kde nositelka protíná trám.",
        "Длина троса ничего не меняет: сила тяжести груза лишь сдвинулась **вдоль своей линии действия** (пунктирная вертикаль). Важно только, где линия действия пересекает брус.",
    ))
    Quiz(
        question = L("Náklad posuneme 3 m od A (1 m od B). Co ukáže váha **B**?", "Груз сдвинем на 3 м от A (1 м от B). Что покажут весы **B**?"),
        options = listOf(
            Opt(L("3 kN", "3 кН"), why = L("To by platilo, kdyby byl náklad blíž k A. Která váha je mu teď blíž?", "Так было бы, если бы груз был ближе к A. Какие весы теперь ближе к нему?")),
            Opt(L("9 kN", "9 кН"), correct = true),
            Opt(L("12 kN", "12 кН"), why = L("Celých 12 kN by B nesla, jen kdyby náklad visel přímo nad ní.", "Все 12 кН весы B несли бы, только если бы груз висел прямо над ними.")),
            Opt(L("6 kN", "6 кН"), why = L("Napůl se dělí jen uprostřed. Tady je náklad blíž k B.", "Пополам делится только посередине. Здесь груз ближе к B.")),
        ),
        explain = L(
            "[[R_B]] · 4 = 12 · 3 → [[R_B]] = 9 kN, a na A zbude 3 kN. Zrcadlově k úkolu s 9 kN na A.",
            "[[R_B]] · 4 = 12 · 3 → [[R_B]] = 9 кН, а на A останется 3 кН. Зеркально к заданию с 9 кН на A.",
        ),
        columns = 4,
    )
}

/**
 * Karta 4: akce a reakce – rozložený obrázek. Obě síly na jedné nositelce, každá na jiném tělese;
 * žák vybere síly, které působí na nosník.
 */
@Composable
private fun CardScope.ActionReaction() {
    val t = tr()
    val lift = remember { Animatable(0f) }
    var split by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(setOf<Int>()) }
    val scope = rememberCoroutineScope()
    val y = 150.0
    val xb = 200.0
    val correct = picked == setOf(0, 1)
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
        "Nosník tlačí na podporu dolů a podpora na nosník **stejně velkou** silou nahoru. Abychom obě síly viděli, nakreslíme tělesa **od sebe** – jen na obrázku. Počítáme nosník, proto nás zajímají jen síly, které působí **na nosník**.",
        "Балка давит на опору вниз, а опора на балку — **такой же по величине** силой вверх. Чтобы увидеть обе силы, нарисуем тела **отдельно** — только на рисунке. Мы рассчитываем балку, поэтому нас интересуют только силы, действующие **на балку**.",
    ))
    Todo(
        L("Rozlož obrázek a prohlédni si obě síly ve styku", "Разнеси рисунок и рассмотри обе силы в контакте") to split,
        L("Vyber všechny síly, které působí **na nosník**", "Выбери все силы, которые действуют **на балку**") to correct,
    )
    ActionButton(if (lift.targetValue > 0.5f) L("Slož zpátky", "Собрать обратно") else L("Rozlož obrázek", "Разнести рисунок")) {
        val target = lift.targetValue < 0.5f
        scope.launch {
            lift.animateTo(if (target) 1f else 0f, tween(700))
            if (target) split = true
        }
    }
    if (split) {
        SmallText(L("Které síly působí na nosník? (klepni na všechny)", "Какие силы действуют на балку? (нажми на все)"))
        Chips(
            listOf(L("zatížení [[F]]", "нагрузка [[F]]"), L("podpora → nosník", "опора → балка"), L("nosník → podpora", "балка → опора")),
            selectedSet = picked,
        ) { i -> picked = if (i in picked) picked - i else picked + i }
        when {
            correct -> Note(Tone.OK, L(
                "Ano. **Akce a reakce** jsou dvě stejně velké opačné síly na jedné nositelce – každá ale působí na **jiné** těleso. Proto se v rovnicích nosníku nikdy navzájem nevyruší: šedá do nich vůbec nepatří.",
                "Да. **Действие и противодействие** — две равные противоположные силы на одной линии действия, но каждая действует на **своё** тело. Поэтому в уравнениях балки они никогда не сокращаются: серая туда вообще не входит.",
            ))
            2 in picked -> Note(Tone.NUDGE, L(
                "Šedá šipka tlačí **na podporu** (míří do ní). Na nosník nepůsobí – do jeho rovnic nepatří.",
                "Серая стрелка давит **на опору** (направлена в неё). На балку она не действует — в её уравнения не входит.",
            ))
            picked.isNotEmpty() -> SmallText(L("Ještě něco chybí?", "Чего-то ещё не хватает?"))
            else -> {}
        }
    }
}

/** Kotvy pod podporou: šrouby do základu – podpora umí nosník i **držet dole**. */
private fun PrimBuilder.anchors(x: Double, y: Double, c: StatikaColors) {
    for (dx in listOf(-12.0, 12.0)) {
        line(x + dx, y + 30, x + dx, y + 50, c.ink, 2.0)
        line(x + dx - 4, y + 50, x + dx + 4, y + 50, c.ink, 2.0)
    }
}

/** Karta 5: smysl reakce si zvolíš; záporný výsledek jen otočí šipku. „Co kdyby A nebyla?“ ukáže proč. */
@Composable
private fun CardScope.GuessSense() {
    val t = tr()
    var guess by remember { mutableStateOf<Int?>(null) } // 0 = ↑, 1 = ↓
    var computed by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    val flip = remember { Animatable(0f) }
    // „co kdyby A nebyla“: nosník se otočí kolem B, levý konec se zvedne k zarážce
    var whatIf by remember { mutableStateOf(false) }
    var whatIfSeen by remember { mutableStateOf(false) }
    var tilt by remember { mutableDoubleStateOf(0.0) }
    var fade by remember { mutableStateOf(1f) }
    val scope = rememberCoroutineScope()
    val y = 100.0
    // A v 0 m, B ve 2 m, F = 4 kN na konci převislého konce ve 4 m; 1 m = 70 jednotek
    val x0 = 60.0
    val m = 70.0
    val xB = x0 + 2 * m
    val up = guess != 1
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
        if (guess != null) {
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
        "Kam míří reakce v A, předem nevíš. Smysl si proto zvol – výpočet ti řekne, jestli sis tipl dobře. Podpora A je **ukotvená** šrouby do základu, takže umí nosník tlačit nahoru i držet dolů.",
        "Куда направлена реакция в A, заранее неизвестно. Поэтому направление (smysl) выбери сам — расчёт покажет, угадал ли ты. Опора A **закреплена** анкерами в фундаменте, поэтому может и толкать балку вверх, и держать её вниз.",
    ))
    Todo(
        L("Zvol smysl reakce: klepni na „[[R_A]] nahoru ↑“, nebo „[[R_A]] dolů ↓“", "Выбери направление реакции: нажми «[[R_A]] вверх ↑» или «[[R_A]] вниз ↓»") to (guess != null),
        L("Klepni na „Spočítej reakce“", "Нажми «Посчитать реакции»") to shown,
    )
    Chips(listOf(L("[[R_A]] nahoru ↑", "[[R_A]] вверх ↑"), L("[[R_A]] dolů ↓", "[[R_A]] вниз ↓")), guess) {
        if (!computed) guess = it
    }
    if (guess != null && !computed) {
        ActionButton(L("Spočítej reakce", "Посчитать реакции")) {
            computed = true
            scope.launch {
                if (up) {
                    delay(1200)
                    flip.animateTo(1f, tween(700))
                }
                shown = true
            }
        }
    }
    if (computed) {
        SmallText(L("Momenty k bodu B, ↺ je plus:", "Моменты относительно B, ↺ — плюс:"))
        if (up) {
            Readout("Σ[[M_B]] = 0:  −[[R_A]]·2 − 4·2 = 0  →  [[R_A]] = −4 kN")
            Readout("Σ[[F_y]] = 0:  [[R_A]] + [[R_B]] − 4 = 0  →  [[R_B]] = 8 kN")
            if (flip.value > 0.5f) Note(Tone.OK, L(
                "Minus není chyba! Jen říká, že reakce ve skutečnosti míří **opačně**, než sis zvolil: podpora A táhne nosník dolů (brání mu v nadzvednutí).",
                "Минус — не ошибка! Он только говорит, что на самом деле реакция направлена **противоположно** выбранному: опора A тянет балку вниз (не даёт ей приподняться).",
            ))
        } else {
            Readout("Σ[[M_B]] = 0:  [[R_A]]·2 − 4·2 = 0  →  [[R_A]] = 4 kN")
            Readout("Σ[[F_y]] = 0:  −[[R_A]] + [[R_B]] − 4 = 0  →  [[R_B]] = 8 kN")
            Note(Tone.OK, L(
                "Vyšlo kladně – tipl sis dobře, podpora A táhne nosník dolů. Kdybys zvolil nahoru, vyšlo by −4 kN: stejná síla, jen s opačným znaménkem.",
                "Получилось положительное число — ты угадал: опора A тянет балку вниз. Если бы ты выбрал «вверх», вышло бы −4 кН: та же сила, только с другим знаком.",
            ))
        }
        if (shown) SmallText(L(
            "V trenažéru proto kreslíme všechny reakce v kladném smyslu (→ ↑ ↺). Znaménko výsledku pak řekne, kam opravdu míří.",
            "Поэтому в тренажёре все реакции рисуем в положительном направлении (→ ↑ ↺). Знак результата покажет, куда они направлены на самом деле.",
        ))
    }
    ActionButton(L("Co kdyby A nebyla?", "А если бы опоры A не было?"), enabled = !whatIf) {
        whatIf = true
        scope.launch {
            tilt = 0.0
            fade = 1f
            // F na převislém konci otáčí nosník kolem B: pravý konec dolů, levý nahoru – čím dál rychleji
            swing(3.0, liftDeg) { tilt = it }
            whatIfSeen = true
            delay(1500)
            for (i in 1..10) {
                fade = 1f - i / 10f
                delay(30)
            }
            whatIf = false
            tilt = 0.0
            fade = 1f
        }
    }
    if (whatIfSeen) SmallText(L(
        "Bez A by se levý konec zvedl. Kotvená podpora A mu v tom brání – proto ho drží **dolů**: [[R_A]] míří dolů.",
        "Без A левый конец поднялся бы. Закреплённая опора A этому мешает — поэтому держит его **вниз**: [[R_A]] направлена вниз.",
    ))
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
    Quiz(
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
