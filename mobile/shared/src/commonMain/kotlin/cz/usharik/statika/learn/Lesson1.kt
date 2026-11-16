package cz.usharik.statika.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.usharik.statika.ui.LocalColors
import cz.usharik.statika.ui.rich
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableDoubleStateOf
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Lekce 1 – Síla je šipka. Síla je vektor: velikost, směr (sklon nositelky), smysl a působiště – puk na ledě
 * jede po nositelce. Působí i tam, kde se nic nehýbe: deska stolu se prohne a pruží zpátky. Siloměr a jednotka
 * kN ≈ tíha 100 kg.
 */

/** 1 kN = 20 jednotek délky šipky. */
private const val PER_KN = 20.0

/** Bedna jako obdélník se středem v (cx, cy). */
private fun PrimBuilder.box(cx: Double, cy: Double, w: Double, h: Double, c: StatikaColors, alpha: Float = 1f) {
    rect(cx - w / 2, cy - h / 2, w, h, c.subtle, c.ink, 1.6, alpha)
    // dvě prkna, ať je to poznat jako bedna
    line(cx - w / 2, cy, cx + w / 2, cy, c.inkSoft, 1.0, alpha = alpha)
}

/** Kolik jednotek se dá jít od (px, py) ve směru (ux, uy), než se narazí na okraj obrázku. */
private fun room(px: Double, py: Double, ux: Double, uy: Double, x0: Double, x1: Double, y0: Double, y1: Double): Double {
    var t = 1e9
    if (ux > 1e-9) t = min(t, (x1 - px) / ux)
    if (ux < -1e-9) t = min(t, (x0 - px) / ux)
    if (uy > 1e-9) t = min(t, (y1 - py) / uy)
    if (uy < -1e-9) t = min(t, (y0 - py) / uy)
    return t
}

/** Branka na kluzišti: střed a písmeno. */
private class Goal(val x: Double, val y: Double, val name: String)

/** Dojezd puku s třením: rychle vyrazí a plynule zpomalí. */
private val Slide = Easing { f -> 1f - (1f - f) * (1f - f) }

/**
 * Karta 0: puk na ledě. Šipka síly = jak do puku strčíš. Tři branky, každá chce změnit jinou vlastnost
 * síly: A směr (sklon), B smysl (hrot na druhou stranu), C velikost (delší šipka = puk jede dál).
 */
@Composable
private fun CardScope.Puck() {
    val t = tr()
    val px = 150.0
    val py = 125.0
    val arrowPer = 14.0 // délka šipky na 10 N
    val runPer = 24.0 // dojezd puku na 10 N strčení (hokejka tlačí vždy po stejně krátké dráze → dojezd ∝ síla)
    val goals = remember {
        listOf(
            // každá branka chce oproti výchozí šipce (30 N →) změnit jen jednu vlastnost
            Goal(px + 3 * runPer * cos(-PI / 4), py + 3 * runPer * sin(-PI / 4), "A"), // jen směr
            Goal(px - 3 * runPer, py, "B"), // jen smysl
            Goal(px + 8 * runPer, py, "C"), // jen velikost
        )
    }
    var kn by remember { mutableStateOf(3.0) }
    var ang by remember { mutableStateOf(0.0) } // stupně, osa y dolů
    var dragging by remember { mutableStateOf(false) }
    var grabOff by remember { mutableStateOf(0.0) }
    val hit = remember { mutableStateListOf(false, false, false) }
    // výsledek posledního strčení: null = ještě nic, -1 = vedle, jinak index branky
    var last by remember { mutableStateOf<Int?>(null) }
    // proč vedle: 0 = jiný směr, 1 = krátké, 2 = dlouhé
    var why by remember { mutableStateOf(0) }
    var flying by remember { mutableStateOf(false) }
    val run = remember { Animatable(0f) }
    // nový puk se po strčení objeví na startu; ten starý zůstane slabě vidět tam, kde zastavil
    val appear = remember { Animatable(1f) }
    var ghost by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    // ukázka na začátku: šipka 30 N doprava pošle puk do ukázkové branky, aby bylo hned jasné, co se děje
    var demo by remember { mutableStateOf(true) }
    var demoSeen by remember { mutableStateOf(false) }
    val demoGoal = Goal(px + 3 * runPer, py, "")
    val scope = rememberCoroutineScope()
    fun ux() = cos(ang * PI / 180)
    fun uy() = sin(ang * PI / 180)
    fun maxKn(a: Double) = ((room(px, py, cos(a * PI / 180), sin(a * PI / 180), 16.0, 384.0, 16.0, 204.0) - 18) / arrowPer).coerceAtMost(9.5)
    // každý krok mění jen jednu vlastnost síly: A směr, B smysl (velikost 30 N), C velikost (směr →); pak volně
    val step = hit.indexOfFirst { !it }
    fun set(a: Double, k: Double) {
        if (flying || demo) return
        ang = if (step == 2) 0.0 else a
        kn = if (step == 0 || step == 1) 3.0 else min((k * 2).roundToInt() / 2.0, floor(maxKn(ang) * 2) / 2.0).coerceAtLeast(0.5)
        last = null
    }
    /** kam puk dojede: rovně po nositelce síly, nejdál k mantinelu */
    fun end(): Pair<Double, Double> {
        val d = min(kn * runPer, room(px, py, ux(), uy(), 22.0, 378.0, 22.0, 198.0))
        return (px + ux() * d) to (py + uy() * d)
    }
    suspend fun slide(): Pair<Double, Double> {
        run.snapTo(0f)
        // rovnoměrné zpomalování třením: doba dojezdu roste s odmocninou dráhy
        run.animateTo(1f, tween((420 * sqrt(kn)).toInt(), easing = Slide))
        return end()
    }
    suspend fun nextPuck(at: Pair<Double, Double>) {
        delay(600)
        ghost = at
        appear.snapTo(0f)
        run.snapTo(0f)
        appear.animateTo(1f, tween(350))
    }
    fun shoot() {
        if (flying || demo) return
        flying = true
        last = null
        scope.launch {
            val (ex, ey) = slide()
            val g = goals.indexOfFirst { hypot(it.x - ex, it.y - ey) < 17 }
            if (g >= 0) hit[g] = true
            else {
                // míří šipka na právě hledanou branku (stejný směr i smysl)?
                val aim = goals.getOrNull(step)?.takeIf { gg ->
                    val d = hypot(gg.x - px, gg.y - py)
                    ((gg.x - px) * ux() + (gg.y - py) * uy()) / d > 0.985
                }
                why = if (aim == null) 0 else if (hypot(ex - px, ey - py) < hypot(aim.x - px, aim.y - py)) 1 else 2
            }
            last = g
            nextPuck(ex to ey)
            // po trefě A a B začíná další krok znovu od výchozí šipky 30 N → (nový puk se právě prolnul)
            if (g == 0 || g == 1) {
                ang = 0.0
                kn = 3.0
            }
            flying = false
        }
    }
    LaunchedEffect(Unit) {
        delay(900)
        flying = true
        val at = slide()
        demoSeen = true
        nextPuck(at)
        flying = false
        demo = false
    }
    val tipX = px + kn * arrowPer * ux()
    val tipY = py + kn * arrowPer * uy()
    val hx = tipX + 16 * ux()
    val hy = tipY + 16 * uy()
    Stage(
        h = 230.0,
        description = t.pick(L("Puk na ledě, šipka síly a tři branky A, B, C", "Шайба на льду, стрелка силы и трое ворот A, B, C")),
        onDrag = { p, start ->
            if (start) {
                val l = hypot(hx - px, hy - py)
                val along = (((p.x - px) * (hx - px) + (p.y - py) * (hy - py)) / (l * l)).coerceIn(0.35, 1.0)
                dragging = !flying && p.dist(px + along * (hx - px), py + along * (hy - py)) < 45
                grabOff = l - hypot(p.x - px, p.y - py)
            }
            if (dragging && p.dist(px, py) > 15) {
                val a = (atan2(p.y - py, p.x - px) * 180 / PI / 15).roundToInt() * 15.0
                set(a, (hypot(p.x - px, p.y - py) + grabOff - 16) / arrowPer)
            }
        },
        onDragEnd = { dragging = false },
        onTap = { p -> if (p.dist(px, py) < 26) shoot() },
    ) { c ->
        // kluziště
        rect(10.0, 10.0, 380.0, 200.0, c.react.copy(alpha = 0.05f), c.inkSoft, 1.2)
        if (demo) {
            // ukázková branka (jen během ukázky)
            circle(demoGoal.x, demoGoal.y, 17.0, c.inkSoft.copy(alpha = if (demoSeen) 0.25f else 0.06f), c.inkSoft, 1.4)
            text(demoGoal.x, demoGoal.y + 36, t.pick(L("ukázka", "пример")), c.inkSoft, 12.0)
            text(22.0, 30.0, t.pick(L("Ukázka: 30 N doprava → puk dojede 3 dílky doprava", "Пример: 30 Н вправо → шайба проедет 3 деления вправо")), c.ink, 13.0, Anchor.START)
        }
        val target = if (demo) -1 else hit.indexOfFirst { !it }
        for ((i, g) in goals.withIndex()) {
            if (i == target && !flying) circle(g.x, g.y, 23.0, null, c.accent, 1.0, alpha = 0.6f)
            circle(g.x, g.y, 17.0, c.accent.copy(alpha = if (hit[i]) 0.28f else 0.07f), c.accent, if (i == target) 2.6 else 1.8)
            text(g.x, g.y + 5, if (hit[i]) "✓" else g.name, c.accent, 14.0, bold = true)
        }
        val (ex, ey) = end()
        val f = run.value.toDouble()
        val bx = px + (ex - px) * f
        val by = py + (ey - py) * f
        if (flying) {
            // stopa puku – jede rovně po nositelce síly
            line(px, py, bx, by, c.inkSoft, 1.0, dash = true)
            // tření brzdí proti pohybu (hokejka už netlačí)
            if (run.value in 0.08f..0.97f) {
                force(bx - ux() * 12, by - uy() * 12, bx - ux() * 30, by - uy() * 30, c.inkSoft, width = 1.8, head = 7.0)
            }
            // šipka zůstane slabě vidět na startu – příčina pohybu
            force(px, py, tipX, tipY, c.load, width = 3.0, alpha = 0.35f)
        } else {
            val back = room(px, py, -ux(), -uy(), 12.0, 388.0, 12.0, 208.0)
            val fwd = room(px, py, ux(), uy(), 12.0, 388.0, 12.0, 208.0)
            line(px - ux() * back, py - uy() * back, px + ux() * fwd, py + uy() * fwd, c.inkSoft, 1.0, dash = true)
            // dílky na nositelce: každých 10 N = jeden dílek dojezdu
            var k = 1
            while (k * runPer <= fwd) {
                circle(px + ux() * k * runPer, py + uy() * k * runPer, 2.2, c.inkSoft, alpha = 0.7f)
                k++
            }
            force(px, py, tipX, tipY, c.load, width = 3.0)
            var nx = -uy()
            var ny = ux()
            if (ny > 0 || (abs(ny) < 1e-6 && nx > 0)) {
                nx = -nx
                ny = -ny
            }
            val anc = if (nx > 0.3) Anchor.START else if (nx < -0.3) Anchor.END else Anchor.MIDDLE
            text((px + tipX) / 2 + nx * 20, (py + tipY) / 2 + ny * 20 + 5, " = ${num(kn * 10, 0)} N", c.load, 15.0, anc, sym = "F")
            handle(hx, hy, c, dragging)
        }
        ghost?.let { (gx, gy) -> circle(gx, gy, 10.0, c.ink, alpha = 0.18f) }
        val pa = if (run.value == 0f) appear.value else 1f
        circle(bx, by, 10.0, c.ink, alpha = pa)
        circle(bx, by, 4.0, c.inkSoft, alpha = pa)
        rect(0.0, 212.0, 400.0, 18.0, c.paper)
        text(16.0, 226.0, t.pick(L("čárkovaně: nositelka – přímka, na které síla leží", "пунктир: линия действия — прямая, на которой лежит сила")), c.inkSoft, 12.0, Anchor.START)
    }
    CardText(L(
        "Síla není jen číslo – záleží i na tom, **kam** míří. Proto ji kreslíme **šipkou**: délka = **velikost**, sklon nositelky = **směr**, hrot = **smysl**, tečka = **působiště**.",
        "Сила — не просто число: важно и то, **куда** она направлена. Поэтому её рисуют **стрелкой**: длина — **величина** (velikost), наклон линии действия — **наклон** (směr), остриё — **направление** (smysl), точка — **точка приложения** (působiště).",
    ))
    SmallText(L(
        "Hokejka tlačí vždy po stejně krátké dráze, takže co 10 N, to jeden dílek dojezdu puku.",
        "Клюшка всегда толкает на одном и том же коротком пути, поэтому каждые 10 Н — одно деление пути шайбы.",
    ))
    val miss: @Composable () -> Unit = {
        if (last == -1) Note(Tone.NUDGE, when (why) {
            0 -> L("Vedle – šipka nemíří na branku. Puk jede po nositelce tam, kam ukazuje hrot.", "Мимо — стрелка не смотрит на ворота. Шайба едет по линии действия туда, куда показывает остриё.")
            1 -> L("Směr je dobrý, ale puk nedojel – síla je malá. Prodluž šipku.", "Направление верное, но шайба не доехала — сила мала. Удлини стрелку.")
            else -> L("Směr je dobrý, ale puk přejel – síla je velká. Zkrať šipku.", "Направление верное, но шайба проскочила — сила велика. Укороти стрелку.")
        })
    }
    val dirs: @Composable () -> Unit = {
        Chips(listOf(L("→", "→"), L("↗", "↗"), L("↑", "↑"), L("←", "←"), L("↓", "↓"))) { i ->
            set(listOf(0.0, -45.0, -90.0, 180.0, 90.0)[i], kn)
        }
    }
    val size: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chips(listOf(L("− 5 N", "− 5 Н"), L("+ 5 N", "+ 5 Н")), modifier = Modifier.weight(1f)) { i -> set(ang, kn + if (i == 0) -0.5 else 0.5) }
            Text(rich("[[F]] = ${num(kn * 10, 0)} N"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = LocalColors.current.ink)
        }
    }
    val shootBtn: @Composable () -> Unit = { ActionButton(L("Strč do puku", "Толкнуть шайбу"), enabled = !flying) { shoot() } }
    Steps(
        Step(L("Sleduj ukázku: 30 N doprava pošle puk o 3 dílky", "Смотри пример: 30 Н вправо — шайба проедет 3 деления"), demoSeen && !demo) {
            SmallText(L("Puk jede po nositelce síly, tam kam míří hrot…", "Шайба едет по линии действия силы, куда смотрит остриё…"))
        },
        Step(
            L("Branka **A** je šikmo: natoč šipku na ni a strč do puku", "Ворота **A** — наискосок: поверни стрелку на них и толкни шайбу"), hit[0],
            result = { Note(Tone.OK, L("Trefa! **Směr** síly určuje, po které přímce puk pojede.", "Попал! **Наклон** силы определяет, по какой прямой поедет шайба.")) },
        ) {
            dirs()
            shootBtn()
            miss()
        },
        Step(
            L("Branka **B** je za pukem: otoč **smysl** síly a strč", "Ворота **B** — позади шайбы: разверни **направление** силы и толкни"), hit[1],
            result = { Note(Tone.OK, L("Trefa! Stejná nositelka, opačný **smysl** – puk jede opačně.", "Попал! Та же линия действия, обратное **направление** — шайба едет в обратную сторону.")) },
        ) {
            Chips(listOf(L("Otoč smysl ⇄", "Развернуть ⇄"))) { set((ang + 180.0) % 360.0, kn) }
            shootBtn()
            miss()
        },
        Step(
            L("Branka **C** je daleko: nastav **velikost** síly a strč", "Ворота **C** — далеко: подбери **величину** силы и толкни"), hit[2],
            result = {
                Note(Tone.OK, L("Trefa! Větší **velikost** síly = větší rychlost, puk dojede dál.", "Попал! Больше **величина** силы — больше скорость, шайба уезжает дальше."))
                SmallText(L(
                    "Tři trefy, tři vlastnosti síly: **směr**, **smysl**, **velikost**. Čtvrtá – **působiště** – přijde na řadu o dvě karty dál.",
                    "Три попадания — три свойства силы: **наклон**, **направление**, **величина**. Четвёртое — **точка приложения** — через две карточки.",
                ))
            },
        ) {
            size()
            shootBtn()
            miss()
        },
        after = {
            dirs()
            size()
            shootBtn()
        },
    )
}

/**
 * Karta 1: proč stůl tlačí. Deska se pod bednou prohne (hodně zvětšeně) a pruží zpátky – čím víc prohnutá,
 * tím víc tlačí. Rozkmitá se a ustálí, až je síla stolu stejná jako tíha.
 */
@Composable
private fun CardScope.TableSpring() {
    val t = tr()
    var guess by remember { mutableStateOf<Int?>(null) }
    var crates by remember { mutableStateOf(0) }
    var settled by remember { mutableStateOf(0) } // kolik beden leželo, když se deska naposled ustálila
    var seenOne by remember { mutableStateOf(false) }
    var seenTwo by remember { mutableStateOf(false) }
    val sag = remember { Animatable(0f) } // prohnutí desky vyjádřené silou stolu v kN
    val drop = remember { Animatable(1f) } // pád poslední bedny: 0 = nahoře, 1 = leží
    val scope = rememberCoroutineScope()
    val running = remember { arrayOfNulls<Job>(1) }
    fun place(n: Int) {
        val added = n > crates
        crates = n
        settled = -1
        running[0]?.cancel()
        running[0] = scope.launch {
            if (added) {
                drop.snapTo(0f)
                drop.animateTo(1f, tween(320, easing = Easing { it * it }))
            }
            sag.animateTo(n.toFloat(), spring(dampingRatio = 0.22f, stiffness = 45f))
            settled = n
            if (n == 1) seenOne = true
            if (n == 2) seenTwo = true
        }
    }
    val top = 150.0 // horní plocha desky bez zatížení
    val x0 = 90.0
    val x1 = 310.0
    val perKn = 10.0 // prohnutí na 1 kN (zvětšené)
    val arrowPer = 26.0
    Stage(h = 240.0, description = t.pick(L("Stůl, jehož deska se pod bednami prohýbá", "Стол, столешница которого прогибается под ящиками"))) { c ->
        val s = sag.value * perKn
        fun y(x: Double) = top + s * sin(PI * (x - x0) / (x1 - x0))
        val floor = 228.0
        line(30.0, floor, 370.0, floor, c.ink, 1.5)
        rect(x0 + 10, top + 10, 8.0, floor - top - 10, c.beam, c.ink, 1.4)
        rect(x1 - 18, top + 10, 8.0, floor - top - 10, c.beam, c.ink, 1.4)
        // prohnutá deska: horní a dolní okraj
        val xs = (0..20).map { x0 + (x1 - x0) * it / 20 }
        poly(xs.map { it to y(it) } + xs.reversed().map { it to y(it) + 10 }, c.beam, c.ink, 1.6)
        // bedny na středu desky (poslední při pokládání padá)
        val bh = 34.0
        val mid = y(200.0)
        for (i in 0 until crates) {
            val fallOff = if (i == crates - 1) (1 - drop.value) * 70.0 else 0.0
            box(200.0, mid - bh / 2 - i * bh - fallOff, 60.0, bh, c)
            text(200.0, mid - bh / 2 - i * bh - fallOff + 13, "100 kg", c.inkSoft, 10.0)
        }
        if (crates > 0 && drop.value > 0.99f) {
            // tíha beden (červeně) a síla stolu v místě dotyku (modře)
            // obě síly leží na jedné svislé nositelce středem beden: G shora na bedny, N zdola do styku
            val g = crates.toDouble()
            val stackTop = mid - crates * bh
            force(200.0, stackTop - g * arrowPer - 4, 200.0, stackTop - 4, c.load, width = 2.6)
            text(192.0, stackTop - g * arrowPer / 2, " = ${num(g, 1)} kN", c.load, 13.0, Anchor.END, sym = "G")
            val n = sag.value.toDouble().coerceAtLeast(0.0)
            if (n > 0.05) {
                force(200.0, mid + n * arrowPer, 200.0, mid, c.react, width = 2.6, glow = true)
                text(208.0, mid + 12 + n * arrowPer / 2, " = ${num(n, 1)} kN", c.react, 13.0, Anchor.START, sym = "N")
            }
        }
        text(392.0, 20.0, t.pick(L("prohnutí zvětšeno asi 1000×", "прогиб увеличен примерно в 1000 раз")), c.inkSoft, 12.0, Anchor.END)
    }
    CardText(L(
        "Bedna leží na stole a nehýbe se. Odkud stůl „ví“, jak silně ji má držet?",
        "Ящик лежит на столе и не двигается. Откуда стол «знает», с какой силой его держать?",
    ))
    var guess2 by remember { mutableStateOf<Int?>(null) }
    val forces: @Composable () -> Unit = {
        Readout("[[N]] = ${num(sag.value.toDouble().coerceAtLeast(0.0), 1)} kN · [[G]] = ${num(crates.toDouble(), 1)} kN")
    }
    Steps(
        tipStep(
            L("Tipni si: tlačí stůl na ležící bednu nějakou silou?", "Как думаешь: давит ли стол на лежащий ящик какой-то силой?"),
            listOf(L("Ano", "Да"), L("Ne, nic se nehýbe", "Нет, ничего не движется")), guess,
        ) { guess = it },
        Step(
            L("Polož na stůl bednu (100 kg ≈ 1 kN) a sleduj desku", "Положи на стол ящик (100 кг ≈ 1 кН) и следи за столешницей"), seenOne,
            result = {
                if (guess == 1) Note(Tone.INFO, L(
                    "A přece tlačí: deska se prohnula a pruží zpátky nahoru – jinak by bedna propadla.",
                    "И всё-таки давит: столешница прогнулась и пружинит обратно вверх — иначе ящик провалился бы.",
                ))
                SmallText(L(
                    "Deska funguje jako **pružina**: čím víc se prohne, tím víc tlačí. Chvíli kmitá (pak tlačí víc i míň než tíha) a ustálí se, až [[N]] = [[G]]. Síla působí, i když se nic nehýbe.",
                    "Столешница работает как **пружина**: чем сильнее прогиб, тем сильнее она давит. Сначала колеблется (сила то больше, то меньше веса) и успокаивается, когда [[N]] = [[G]]. Сила действует, даже когда ничего не движется.",
                ))
            },
        ) {
            forces()
            ActionButton(L("Polož bednu", "Положить ящик"), enabled = crates == 0) { place(1) }
        },
        tipStep(
            L("Přibude druhá bedna. Jak velkou silou [[N]] bude stůl tlačit?", "Добавим второй ящик. С какой силой [[N]] будет давить стол?"),
            listOf(L("1 kN – pořád stejně", "1 кН — как раньше"), L("2 kN", "2 кН"), L("víc než 2 kN", "больше 2 кН")), guess2,
        ) { guess2 = it },
        Step(
            L("Polož druhou bednu a ověř svůj tip", "Положи второй ящик и проверь свой прогноз"), seenTwo,
            result = {
                Note(if (guess2 == 1) Tone.OK else Tone.INFO, L(
                    (if (guess2 == 1) "Správně. " else "") + "Dvojnásobná tíha → dvojnásobné prohnutí → stůl tlačí 2 kN. Podpory nosníků se chovají stejně." +
                        (if (guess2 == 2) " Chvíli opravdu tlačil víc – když deska překmitla. Ustálí se ale přesně na 2 kN." else ""),
                    (if (guess2 == 1) "Верно. " else "") + "Вдвое больше вес → вдвое больше прогиб → стол давит с силой 2 кН. Опоры балок ведут себя так же." +
                        (if (guess2 == 2) " На мгновение он и правда давил сильнее — пока столешница колебалась. Но успокаивается ровно на 2 кН." else ""),
                ))
            },
        ) {
            forces()
            ActionButton(L("Polož druhou bednu", "Положить второй ящик"), enabled = crates == 1 && settled == 1) { place(2) }
        },
    )
}

/**
 * Tip před pokusem: krok se splní jakoukoli volbou, správnost ukáže až následující pokus.
 * Ve výkladu pak zůstane „Tvůj tip: …“.
 */
@Composable
private fun tipStep(label: L, options: List<L>, pick: Int?, onPick: (Int) -> Unit): Step =
    Step(label, pick != null, result = {
        pick?.let { SmallText(L("Tvůj tip: **${options[it].cs}**", "Твой прогноз: **${options[it].ru}**")) }
    }) {
        Chips(options, pick) { if (pick == null) onPick(it) }
    }

/** Rohy obdélníku w × h se středem (cx, cy) otočeného o úhel a (rad, osa y dolů). */
private fun plateCorners(cx: Double, cy: Double, w: Double, h: Double, a: Double): List<Pair<Double, Double>> {
    val ca = cos(a)
    val sa = sin(a)
    return listOf(-1.0 to -1.0, 1.0 to -1.0, 1.0 to 1.0, -1.0 to 1.0).map { (sx, sy) ->
        val x = sx * w / 2
        val y = sy * h / 2
        (cx + x * ca - y * sa) to (cy + x * sa + y * ca)
    }
}

/** Poloha desky v jednom záblesku stroboskopu. */
private class Pose(val x: Double, val y: Double, val a: Double)

/**
 * Karta 2: deska na vzduchovém stole (pohled shora, bez tření). Krátké strčení stejnou silou:
 * zezadu, nebo lanem zepředu po téže nositelce → úplně stejná stopa; po rovnoběžné nositelce mimo těžiště
 * → těžiště jede stejně, ale deska se navíc roztočí (moment). Nic se nevrací: pokus dojede, zmizí, objeví se nová deska.
 */
@Composable
private fun CardScope.LineOfAction() {
    val t = tr()
    val x0 = 92.0 // start těžiště
    val y0 = 112.0
    val pw = 92.0
    val ph = 50.0
    val off = 15.0 // posun rovnoběžné nositelky
    val acc = 230.0 // zrychlení během strčení (jednotek/s²)
    val push = 0.5 // jak dlouho síla působí (s)
    val endT = 2.2
    var guessRope by remember { mutableStateOf<Int?>(null) } // tip pro lano zepředu: 0 stejně, 1 rychleji, 2 pomaleji
    var guess by remember { mutableStateOf<Int?>(null) } // tip pro jinou nositelku: 0 stejně, 1 ↻, 2 ↺
    var time by remember { mutableDoubleStateOf(-1.0) } // -1 = deska stojí na startu
    var alpha by remember { mutableStateOf(1f) }
    var runMode by remember { mutableStateOf(0) }
    val ghosts = remember { mutableStateListOf<Pose>() }
    val prev = remember { mutableStateListOf<Pose>() } // stopa minulého pokusu (šedě)
    val done = remember { mutableStateListOf(false, false, false) }
    val scope = rememberCoroutineScope()
    val running = time >= 0
    // režim určuje aktuální krok: 0 tlačí zezadu, 1 táhne lanem zepředu, 2 jiná (rovnoběžná) nositelka
    val mode: Int? = when {
        !done[0] -> 0
        !done[1] -> 1
        else -> 2
    }
    /** stav desky v čase s pro daný režim: posun s(t), úhel θ(t) */
    fun pose(s: Double, m: Int): Pose {
        val tp = min(s, push)
        val x = 0.5 * acc * tp * tp + acc * push * (s - tp)
        // úhlové zrychlení: α = F·d / I, I = m(w² + h²)/12 → α = a·d·12/(w² + h²)
        val al = if (m == 2) acc * off * 12 / (pw * pw + ph * ph) else 0.0
        val th = 0.5 * al * tp * tp + al * push * (s - tp)
        return Pose(x0 + x, y0, th)
    }
    fun run() {
        val m = mode ?: return
        if (running || (m == 1 && guessRope == null) || (m == 2 && guess == null)) return
        runMode = m
        scope.launch {
            ghosts.clear()
            alpha = 1f
            var s = 0.0
            var last = 0L
            var next = 0.0
            time = 0.0
            while (s < endT) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0.0 else (now - last) / 1e9
                    last = now
                    s = min(endT, s + dt)
                    time = s
                    if (s >= next) {
                        ghosts += pose(next, m)
                        next += 0.25
                    }
                }
            }
            // pokus skončil: deska zmizí, její stopa zůstane šedě pro srovnání, na startu se objeví nová
            for (i in 1..8) {
                alpha = 1f - i / 8f
                delay(25)
            }
            done[m] = true
            prev.clear()
            prev += ghosts
            ghosts.clear()
            time = -1.0
            for (i in 1..8) {
                alpha = i / 8f
                delay(25)
            }
        }
    }
    Stage(
        h = 230.0,
        description = t.pick(L("Deska na vzduchovém stole shora, strčená silou po různých nositelkách", "Пластина на воздушном столе сверху, толчок силой по разным линиям действия")),
        onTap = { p -> if (!running && p.dist(x0, y0) < 50) run() },
    ) { c ->
        rect(10.0, 10.0, 380.0, 200.0, c.react.copy(alpha = 0.04f), c.inkSoft, 1.2)
        // otvory vzduchového stolu
        for (gx in 0..18) for (gy in 0..9) circle(20.0 + gx * 20, 20.0 + gy * 20, 0.9, c.inkSoft, alpha = 0.35f)
        text(16.0, 224.0, t.pick(L("pohled shora · vzduchový stůl, bez tření", "вид сверху · воздушный стол, без трения")), c.inkSoft, 12.0, Anchor.START)
        val m = if (running) runMode else (mode ?: 0)
        val lineY = if (m == 2) y0 - off else y0
        // stopa minulého pokusu šedě, aktuálního barevně; křížek = těžiště
        for (g in prev) {
            poly(plateCorners(g.x, g.y, pw, ph, g.a), null, c.inkSoft, 1.0, alpha = 0.35f)
            circle(g.x, g.y, 2.5, c.inkSoft, alpha = 0.6f)
        }
        val col = if (runMode == 2) c.derived else c.react
        for (g in ghosts) {
            poly(plateCorners(g.x, g.y, pw, ph, g.a), null, col, 1.2, alpha = 0.45f)
            circle(g.x, g.y, 2.5, col)
        }
        val now = if (running) pose(time, runMode) else Pose(x0, y0, 0.0)
        if (alpha > 0.01f) {
            poly(plateCorners(now.x, now.y, pw, ph, now.a), c.subtle, c.ink, 1.6, alpha = alpha)
            line(now.x - 5, now.y, now.x + 5, now.y, c.ink, 1.4, alpha = alpha)
            line(now.x, now.y - 5, now.x, now.y + 5, c.ink, 1.4, alpha = alpha)
            if (!running) text(now.x, now.y - 8, "T", c.inkSoft, 11.0)
        }
        // nositelka a síla: kreslí se jen, dokud síla působí
        val acting = !running || time < push
        if (mode != null || running) {
            line(10.0, lineY, 390.0, lineY, c.inkSoft, 1.0, dash = true)
            if (acting && alpha > 0.5f) {
                val left = now.x - pw / 2
                val right = now.x + pw / 2
                if (m == 1) {
                    // původní šipka zezadu slabě: síla se jen posunula po své nositelce
                    if (!running) force(left - 56, lineY, left, lineY, c.load, width = 2.0, alpha = 0.25f, dash = true)
                    force(right, lineY, right + 56, lineY, c.load, width = 3.0)
                    text(right + 28, lineY - 10, t.pick(L("lano", "трос")), c.load, 11.0)
                } else {
                    force(left - 56, lineY, left, lineY, c.load, width = 3.0)
                }
                text(if (m == 1) right + 60 else left - 60, lineY + 5, null, c.load, 15.0, if (m == 1) Anchor.START else Anchor.END, sym = "F")
                if (m == 2) {
                    // rameno d mezi nositelkou a těžištěm
                    line(now.x + 14, lineY, now.x + 14, now.y, c.derived, 1.2)
                    text(now.x + 19, (lineY + now.y) / 2 + 4, "d", c.derived, 12.0, Anchor.START)
                }
            }
        }
    }
    CardText(L(
        "Deska klouže po **vzduchovém stole** bez tření. Pokaždé do ní krátce strčíme stejně velkou silou – měníme jen to, **kde** síla působí.",
        "Пластина скользит по **воздушному столу** без трения. Каждый раз коротко толкаем её одной и той же силой — меняем только то, **где** сила приложена.",
    ))
    Steps(
        Step(
            L("Strč do desky zezadu – síla míří těžištěm T", "Толкни пластину сзади — сила идёт через центр тяжести T"), done[0],
            result = {
                SmallText(L(
                    "Síla jde těžištěm: deska se během strčení rozjíždí (záblesky se rozestupují) a pak jede stálou rychlostí – nic ji nebrzdí. Stopa zůstala šedě.",
                    "Сила проходит через центр тяжести: во время толчка пластина разгоняется (вспышки расходятся), а потом едет с постоянной скоростью — её ничто не тормозит. След остался серым.",
                ))
            },
        ) { ActionButton(L("Strč", "Толкнуть"), enabled = !running) { run() } },
        tipStep(
            L("Stejná síla teď táhne lanem zepředu. Jak deska pojede?", "Та же сила теперь тянет тросом спереди. Как поедет пластина?"),
            listOf(L("úplně stejně", "точно так же"), L("rychleji", "быстрее"), L("pomaleji", "медленнее")), guessRope,
        ) { guessRope = it },
        Step(
            L("Zatáhni lanem a porovnej záblesky se šedou stopou", "Потяни тросом и сравни вспышки с серым следом"), done[1],
            result = {
                Note(if (guessRope == 0) Tone.OK else Tone.INFO, L(
                    (if (guessRope == 0) "Správně – " else "Ne – ") + "úplně stejná stopa jako při tlačení zezadu. Sílu smíš posunout **po její nositelce** – na pohyb tělesa to nemá vliv.",
                    (if (guessRope == 0) "Верно — " else "Нет — ") + "точно такой же след, как при толчке сзади. Силу можно сдвигать **вдоль её линии действия** — на движение тела это не влияет.",
                ))
            },
        ) { ActionButton(L("Zatáhni lanem", "Потянуть тросом"), enabled = !running) { run() } },
        tipStep(
            L("Sílu posuneme na rovnoběžnou nositelku nad T. Co deska udělá?", "Сдвинем силу на параллельную линию выше T. Что сделает пластина?"),
            listOf(L("Pojede stejně", "Поедет так же"), L("Pojede a točí se ↻", "Поедет и закрутится ↻"), L("Pojede a točí se ↺", "Поедет и закрутится ↺")), guess,
        ) { guess = it },
        Step(
            L("Strč po nové nositelce a sleduj křížky těžiště", "Толкни по новой линии и следи за крестиками центра тяжести"), done[2],
            result = {
                Note(if (guess == 1) Tone.OK else Tone.INFO, L(
                    (if (guess == 1) "Správně. " else "Ve skutečnosti se točí ↻. ") +
                        "Těžiště T jede pořád stejně (křížky dopadají na šedé tečky), ale deska se navíc roztočí. Stejná síla na jiné nositelce má navíc **otáčivý účinek – moment** [[M]] = [[F]] · d (lekce 4).",
                    (if (guess == 1) "Верно. " else "На самом деле крутится ↻. ") +
                        "Центр тяжести T едет так же (крестики попадают на серые точки), но пластина ещё и закручивается. Та же сила на другой линии действия даёт ещё и **вращающее действие — момент** [[M]] = [[F]] · d (урок 4).",
                ))
            },
        ) { ActionButton(L("Strč", "Толкнуть"), enabled = !running) { run() } },
    )
}

/** Předmět na siloměru: název a hmotnost v kg. */
private class Thing(val name: L, val kg: Double, val w: Double)

/** Karta 3: siloměr – kolik je 1 kN. Pružina se natáhne úměrně tíze. */
@Composable
private fun CardScope.SpringScale() {
    val t = tr()
    val things = remember {
        listOf(
            Thing(L("jablko", "яблоко"), 0.1, 40.0),
            Thing(L("kbelík vody 10 l", "ведро воды 10 л"), 10.0, 60.0),
            Thing(L("člověk 70 kg", "человек 70 кг"), 70.0, 70.0),
            Thing(L("sud vody 100 l", "бочка воды 100 л"), 100.0, 76.0),
            Thing(L("malé auto", "маленькая машина"), 1000.0, 120.0),
        )
    }
    var pick by remember { mutableStateOf<Int?>(null) }
    var guess by remember { mutableStateOf<Int?>(null) } // tip: co má tíhu asi 1 kN
    val tried = remember { mutableStateListOf(false, false, false, false, false) }
    val stretch = remember { Animatable(0f) } // údaj siloměru v kN
    val scope = rememberCoroutineScope()
    val per = 15.0 // natažení pružiny na 1 kN
    val hookX = 130.0
    val top = 18.0
    val rest = 40.0
    Stage(h = 260.0, description = t.pick(L("Siloměr s pružinou a stupnicí v kN", "Динамометр с пружиной и шкалой в кН"))) { c ->
        val len = rest + stretch.value.coerceIn(0f, 10.6f) * per
        rect(hookX - 40, 6.0, 80.0, 8.0, c.beam, c.ink, 1.4)
        // pružina: cikcak mezi top a top + len
        val n = 14
        var lx = hookX
        var ly = top
        line(hookX, 14.0, hookX, top, c.ink, 1.6)
        for (i in 1..n) {
            val yy = top + len * i / n
            val xx = if (i == n) hookX else hookX + if (i % 2 == 0) -9.0 else 9.0
            line(lx, ly, xx, yy, c.ink, 1.6)
            lx = xx
            ly = yy
        }
        val end = top + len
        // stupnice 0–10 kN a ukazatel na konci pružiny
        val sx = 230.0
        line(sx, top + rest, sx, top + rest + 10 * per, c.inkSoft, 1.2)
        for (k in 0..10) {
            val yy = top + rest + k * per
            line(sx, yy, sx + if (k % 5 == 0) 12.0 else 7.0, yy, c.inkSoft, 1.2)
            if (k % 5 == 0) text(sx + 16, yy + 4, "$k kN", c.inkSoft, 11.0, Anchor.START)
        }
        line(hookX + 12, end, sx - 2, end, c.load, 1.2, dash = true)
        force(sx - 14, end, sx - 2, end, c.load, width = 2.0, head = 7.0)
        // zavěšený předmět
        pick?.let { i ->
            val th = things[i]
            line(hookX, end, hookX, end + 8, c.ink, 1.6)
            rect(hookX - th.w / 2, end + 8, th.w, 30.0, c.subtle, c.ink, 1.5)
            text(hookX, end + 28, t.pick(th.name), c.ink, 11.0)
        }
    }
    CardText(L(
        "Síly měříme **siloměrem** – pružinou, která se natáhne tím víc, čím větší síla za ni tahá. Jednotka je newton (N); ve statice počítáme v **kilonewtonech**: 1 kN = 1000 N.",
        "Силы измеряют **динамометром** — пружиной, которая растягивается тем сильнее, чем больше сила. Единица — ньютон (Н); в статике считаем в **килоньютонах**: 1 кН = 1000 Н.",
    ))
    var hungTip by remember { mutableStateOf(false) } // tipnutý předmět visel a pružina se ustálila
    fun hang(i: Int) {
        pick = i
        tried[i] = true
        // pružina se ustálí s mírným zákmitem (silně tlumená, aby nevyskočila ze stupnice)
        scope.launch {
            stretch.animateTo((things[i].kg / 100).toFloat(), spring(dampingRatio = 0.5f, stiffness = 60f))
            if (i == guess) hungTip = true
        }
    }
    val reading: @Composable () -> Unit = {
        pick?.let { i ->
            val kn = things[i].kg / 100
            Readout(if (kn < 0.01) "[[G]] ≈ ${num(kn * 1000, 0)} N = ${num(kn, 3)} kN" else "[[G]] ≈ ${num(kn, 2)} kN")
        }
    }
    val car = quizStep(
        question = L("Malé auto má 1 tunu. Kolik ukáže siloměr?", "Маленькая машина — 1 тонна. Сколько покажет динамометр?"),
        options = listOf(
            Opt(L("1 kN", "1 кН"), why = L("1 kN je 100 kg. Kolikrát je tuna víc?", "1 кН — это 100 кг. Во сколько раз тонна больше?")),
            Opt(L("10 kN", "10 кН"), correct = true),
            Opt(L("100 kN", "100 кН"), why = L("1 t = 1000 kg = 10 × 100 kg.", "1 т = 1000 кг = 10 × 100 кг.")),
            Opt(L("1000 kN", "1000 кН"), why = L("1 t = 1000 kg = 10 × 100 kg.", "1 т = 1000 кг = 10 × 100 кг.")),
        ),
        explain = L(
            "1 t = 1000 kg = 10 × 100 kg, tedy asi **10 kN**. Jablko má asi 1 N, malé auto asi 10 kN.",
            "1 т = 1000 кг = 10 × 100 кг, то есть около **10 кН**. Яблоко — около 1 Н, маленькая машина — около 10 кН.",
        ),
        columns = 2,
    )
    // po odpovědi se auto samo zavěsí – ručička doběhne na 10 kN
    LaunchedEffect(car.done) { if (car.done && pick != 4) hang(4) }
    Steps(
        tipStep(
            L("Tipni si: který předmět má tíhu asi 1 kN?", "Как думаешь: какой предмет весит около 1 кН?"),
            things.map { it.name }, guess,
        ) { guess = it },
        Step(L("Zavěs svůj tip na siloměr a přečti stupnici", "Подвесь свой вариант на динамометр и посмотри на шкалу"), hungTip) {
            val g = guess ?: 0
            reading()
            ActionButton(L("Zavěs: ${things[g].name.cs}", "Подвесить: ${things[g].name.ru}"), enabled = pick != g) { hang(g) }
        },
        Step(
            L("Najdi předmět, který ukáže asi 1 kN", "Найди предмет, который покажет около 1 кН"), tried[3],
            result = {
                if (guess != 3) Note(Tone.INFO, L(
                    "Tip nevyšel – nevadí, odhad sil je potřeba vycvičit. Asi 1 kN má sud se 100 litry vody.",
                    "Не угадал — ничего, чувство силы нужно натренировать. Около 1 кН весит бочка со 100 литрами воды.",
                ))
                SmallText(L(
                    "Pamatuj: **1 kN ≈ tíha 100 kg** (přesně 0,98 kN).",
                    "Запомни: **1 кН ≈ сила тяжести 100 кг** (точно 0,98 кН).",
                ))
            },
        ) {
            // auto tu ještě není – jeho údaj je otázkou dalšího kroku
            Chips(things.take(4).map { it.name }, pick) { hang(it) }
            reading()
        },
        car,
        after = {
            Chips(things.map { it.name }, pick) { hang(it) }
            reading()
        },
    )
}

/** Karta 4: kvíz – stejný směr, opačný smysl. */
@Composable
private fun CardScope.CheckSense() {
    val t = tr()
    Stage(h = 190.0, description = t.pick(L("Síla F a tři další šipky A, B, C", "Сила F и ещё три стрелки A, B, C"))) { c ->
        val d = 40.0
        fun diag(x: Double, y: Double, sense: Double, col: androidx.compose.ui.graphics.Color, label: String, sym: Boolean = false) {
            force(x - sense * d, y + sense * d * 0.6, x + sense * d, y - sense * d * 0.6, col, width = 2.6)
            if (sym) text(x, y + 40, null, col, 16.0, sym = label) else text(x, y + 40, label, col, 15.0, bold = true)
        }
        diag(60.0, 95.0, 1.0, c.load, "F", sym = true)
        // A: rovnoběžná, opačný hrot
        diag(155.0, 95.0, -1.0, c.ink, "A")
        // B: jiný sklon (svislá)
        force(250.0, 130.0, 250.0, 60.0, c.ink, width = 2.6)
        text(250.0, 160.0, "B", c.ink, 15.0, bold = true)
        // C: rovnoběžná, stejný hrot (jen posunutá)
        diag(340.0, 95.0, 1.0, c.ink, "C")
    }
    CardText(L("**Směr** = sklon nositelky. **Smysl** = kam míří hrot.", "**Наклон** (směr) — наклон линии действия. **Направление** (smysl) — куда смотрит остриё."))
    Steps(quizStep(
        question = L("Která šipka má stejný směr jako [[F]], ale opačný smysl?", "У какой стрелки тот же наклон, что у [[F]], но обратное направление?"),
        options = listOf(
            Opt(L("A", "A"), correct = true),
            Opt(L("B", "B"), why = L("B má jiný sklon, tedy jiný směr. Která šipka je s [[F]] rovnoběžná, ale hrot má na druhé straně?", "У B другой наклон. Какая стрелка параллельна [[F]], но остриё у неё с другой стороны?")),
            Opt(L("C", "C"), why = L("C má stejný sklon, ale i stejný smysl jako [[F]] – je jen posunutá. Která šipka míří hrotem na opačnou stranu?", "У C тот же наклон, но и направление то же, что у [[F]], — она просто сдвинута. У какой стрелки остриё смотрит в обратную сторону?")),
        ),
        explain = L(
            "Směr = sklon nositelky, takže rovnoběžné šipky mají stejný směr. Smysl = kam míří hrot: A je rovnoběžná s [[F]], ale míří opačně.",
            "Наклон (směr) у параллельных стрелок одинаковый. Направление (smysl) — куда смотрит остриё: A параллельна [[F]], но направлена противоположно.",
        ),
        columns = 3,
    ))
}

internal fun lesson1() = Lesson(
    id = 1,
    title = L("Síla je šipka", "Сила — это стрелка"),
    summary = L("Síla jako vektor: velikost, směr, smysl a působiště. Proč stůl tlačí a jak sílu změřit.", "Сила как вектор: величина, наклон, направление и точка приложения. Почему стол давит и как измерить силу."),
    cards = listOf(
        Card { Puck() },
        Card { TableSpring() },
        Card { LineOfAction() },
        Card { SpringScale() },
        Card { CheckSense() },
    ),
)
