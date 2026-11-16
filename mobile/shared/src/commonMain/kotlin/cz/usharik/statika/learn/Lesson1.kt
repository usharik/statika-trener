package cz.usharik.statika.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
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
    fun set(a: Double, k: Double) {
        if (flying || demo) return
        ang = a
        kn = min((k * 2).roundToInt() / 2.0, floor(maxKn(a) * 2) / 2.0).coerceAtLeast(0.5)
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
                // nejbližší nesplněná branka, na kterou šipka míří (stejný směr i smysl)
                val aim = goals.withIndex().filter { !hit[it.index] }.firstOrNull { (_, gg) ->
                    val d = hypot(gg.x - px, gg.y - py)
                    ((gg.x - px) * ux() + (gg.y - py) * uy()) / d > 0.985
                }
                why = if (aim == null) 0 else if (hypot(ex - px, ey - py) < hypot(aim.value.x - px, aim.value.y - py)) 1 else 2
            }
            last = g
            nextPuck(ex to ey)
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
        "Síla není jen číslo – záleží i na tom, **kam** působí. Proto ji kreslíme **šipkou**: délka = **velikost**, sklon nositelky = **směr**, hrot = **smysl**, tečka = **působiště**. Nastav šipku (táhni za kroužek) a strč do puku tak, aby trefil všechny tři branky.",
        "Сила — не просто число: важно и то, **куда** она действует. Поэтому её рисуют **стрелкой**: длина — **величина** (velikost), наклон линии действия — **наклон** (směr), остриё — **направление** (smysl), точка — **точка приложения** (působiště). Настрой стрелку (тяни за кружок) и толкни шайбу так, чтобы она попала во все трое ворот.",
    ))
    if (demo || (demoSeen && last == null && hit.none { it })) Note(Tone.INFO, L(
        "**Ukázka:** šipka 30 N doprava → puk jede po nositelce doprava a dojede 3 dílky daleko. Hokejka tlačí vždy po stejně krátké dráze, takže co 10 N, to jeden dílek. Teď ty: nastav šipku (táhni za kroužek nebo použij tlačítka) a strč – tlačítkem nebo klepnutím na puk. Zvýrazněná branka je na řadě.",
        "**Пример:** стрелка 30 Н вправо → шайба едет вдоль линии действия вправо и проезжает 3 деления. Клюшка всегда толкает на одном и том же коротком пути, поэтому каждые 10 Н — одно деление. Теперь ты: настрой стрелку (тяни за кружок или жми кнопки) и толкни — кнопкой или касанием шайбы. Выделенные ворота — следующие.",
    ))
    Todo(
        L("Branka **A** je šikmo: natoč šipku (změň **směr**) a tref ji", "Ворота **A** — наискосок: поверни стрелку (измени **наклон**) и попади в них") to hit[0],
        L("Branka **B** je za pukem: otoč hrot na druhou stranu (změň **smysl**)", "Ворота **B** — позади шайбы: разверни остриё в другую сторону (измени **направление**)") to hit[1],
        L("Branka **C** je daleko: prodluž šipku (zvětši **velikost**) – silnější strčení, puk jede rychleji a dál", "Ворота **C** — далеко: удлини стрелку (увеличь **величину**) — толчок сильнее, шайба едет быстрее и дальше") to hit[2],
    )
    Chips(listOf(L("→", "→"), L("↗", "↗"), L("↑", "↑"), L("←", "←"), L("↓", "↓"))) { i ->
        set(listOf(0.0, -45.0, -90.0, 180.0, 90.0)[i], kn)
    }
    Chips(listOf(L("− 5 N", "− 5 Н"), L("+ 5 N", "+ 5 Н"))) { i -> set(ang, kn + if (i == 0) -0.5 else 0.5) }
    Readout("[[F]] = ${num(kn * 10, 0)} N")
    ActionButton(L("Strč do puku", "Толкнуть шайбу"), enabled = !flying) { shoot() }
    when (last) {
        null -> {}
        -1 -> SmallText(when (why) {
            0 -> L(
                "Vedle – šipka nemíří na žádnou branku. Puk jede vždy po nositelce síly, tam, kam ukazuje hrot.",
                "Мимо — стрелка не смотрит ни на одни ворота. Шайба всегда едет по линии действия силы туда, куда показывает остриё.",
            )
            1 -> L("Směr je dobrý, ale puk nedojel – síla je malá. Prodluž šipku.", "Направление верное, но шайба не доехала — сила мала. Удлини стрелку.")
            else -> L("Směr je dobrý, ale puk přejel – síla je velká. Zkrať šipku.", "Направление верное, но шайба проскочила — сила велика. Укороти стрелку.")
        })
        0 -> Note(Tone.OK, L("Trefa! **Směr** síly určuje, po které přímce puk pojede.", "Попал! **Наклон** силы определяет, по какой прямой поедет шайба."))
        1 -> Note(Tone.OK, L("Trefa! Stejná nositelka, opačný **smysl** – puk jede opačně.", "Попал! Та же линия действия, обратное **направление** — шайба едет в обратную сторону."))
        else -> Note(Tone.OK, L("Trefa! Větší **velikost** síly = větší rychlost, puk dojede dál.", "Попал! Больше **величина** силы — больше скорость, шайба уезжает дальше."))
    }
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
        "Bedna leží na stole a nehýbe se. Tlačí stůl na bednu nějakou silou? A jak to „ví“, jak velkou?",
        "Ящик лежит на столе и не двигается. Давит ли стол на ящик какой-то силой? И откуда стол «знает», с какой?",
    ))
    Todo(
        L("Tipni si: klepni na „Ano“, nebo na „Ne, nic se nehýbe“", "Сначала предположи: нажми «Да» или «Нет, ничего не движется»") to (guess != null),
        L("Polož na stůl bednu (100 kg ≈ 1 kN) a sleduj desku i sílu stolu [[N]]", "Положи на стол ящик (100 кг ≈ 1 кН) и следи за столешницей и силой стола [[N]]") to seenOne,
        L("Přidej druhou bednu – kolik teď stůl tlačí?", "Добавь второй ящик — с какой силой теперь давит стол?") to seenTwo,
    )
    Chips(listOf(L("Ano", "Да"), L("Ne, nic se nehýbe", "Нет, ничего не движется")), guess) { if (guess == null) guess = it }
    if (guess != null) Chips(listOf(L("Polož bednu", "Положить ящик"), L("Odeber bednu", "Убрать ящик"))) { i ->
        if (i == 0 && crates < 2) place(crates + 1)
        if (i == 1 && crates > 0) place(crates - 1)
    }
    if (settled >= 1) {
        if (guess == 1 && !seenTwo) Note(Tone.INFO, L(
            "A přece tlačí: deska se prohnula a pruží zpátky nahoru – jinak by bedna propadla.",
            "И всё-таки давит: столешница прогнулась и пружинит обратно вверх — иначе ящик провалился бы.",
        ))
        SmallText(L(
            "Deska funguje jako **pružina**: čím víc se prohne, tím víc tlačí. Chvíli kmitá (pak tlačí víc i míň než tíha) a ustálí se, až [[N]] = [[G]]. Síla působí, i když se nic nehýbe.",
            "Столешница работает как **пружина**: чем сильнее прогиб, тем сильнее она давит. Сначала колеблется (сила то больше, то меньше веса) и успокаивается, когда [[N]] = [[G]]. Сила действует, даже когда ничего не движется.",
        ))
    }
    if (seenTwo && settled == 2) Note(Tone.OK, L(
        "Dvojnásobná tíha → dvojnásobné prohnutí → stůl tlačí 2 kN. Podpory nosníků se chovají stejně.",
        "Вдвое больше вес → вдвое больше прогиб → стол давит с силой 2 кН. Опоры балок ведут себя так же.",
    ))
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
    var mode by remember { mutableStateOf<Int?>(null) } // 0 tlačí zezadu, 1 táhne zepředu, 2 jiná nositelka
    var guess by remember { mutableStateOf<Int?>(null) } // tip pro jinou nositelku
    var time by remember { mutableDoubleStateOf(-1.0) } // -1 = deska stojí na startu
    var alpha by remember { mutableStateOf(1f) }
    var runMode by remember { mutableStateOf(0) }
    val ghosts = remember { mutableStateListOf<Pose>() }
    val prev = remember { mutableStateListOf<Pose>() } // stopa minulého pokusu (šedě)
    val done = remember { mutableStateListOf(false, false, false) }
    var lastDone by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val running = time >= 0
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
        if (running || (m == 2 && guess == null)) return
        runMode = m
        lastDone = null
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
            lastDone = m
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
        "Deska leží na **vzduchovém stole** – klouže bez tření, takže je dobře vidět, co s ní udělá jedno krátké strčení. Síla je vždy stejně velká a působí stejně dlouho; měníš jen, **kde** působí. Zkus ji posunout **po její nositelce** (čárkované přímce) a pak na **jinou, rovnoběžnou** nositelku.",
        "Пластина лежит на **воздушном столе** — скользит без трения, поэтому хорошо видно, что с ней делает один короткий толчок. Сила всегда одинаковая и действует одинаково долго; меняешь только, **где** она приложена. Сдвинь её **вдоль её линии действия** (пунктира), а потом на **другую, параллельную** линию.",
    ))
    Todo(
        L("Strč do desky **zezadu** po nositelce, která jde těžištěm T", "Толкни пластину **сзади** вдоль линии, проходящей через центр тяжести T") to done[0],
        L("Posuň sílu po téže nositelce **dopředu** a táhni lanem – jede deska jinak?", "Сдвинь силу вдоль той же линии **вперёд** и тяни тросом — едет ли пластина иначе?") to done[1],
        L("Tipni si, co udělá síla na **jiné** (rovnoběžné) nositelce, a vyzkoušej to", "Предположи, что сделает сила на **другой** (параллельной) линии, и проверь") to done[2],
    )
    Chips(
        listOf(L("Tlačí zezadu", "Толкает сзади"), L("Táhne lanem zepředu", "Тянет тросом спереди"), L("Jiná nositelka (výš)", "Другая линия (выше)")),
        mode,
    ) { if (!running) mode = it }
    if (mode == 2) {
        SmallText(L("Co udělá deska, když síla nepůjde těžištěm?", "Что сделает пластина, если сила не проходит через центр тяжести?"))
        Chips(
            listOf(L("Pojede stejně", "Поедет так же"), L("Pojede a točí se ↻", "Поедет и закрутится ↻"), L("Pojede a točí se ↺", "Поедет и закрутится ↺")),
            guess,
        ) { if (!running) guess = it }
    }
    ActionButton(L("Strč", "Толкнуть"), enabled = mode != null && !running && (mode != 2 || guess != null)) { run() }
    when (lastDone) {
        1 -> Note(Tone.OK, L(
            "Úplně stejná stopa jako při tlačení zezadu. Sílu smíš posunout **po její nositelce** – na pohyb tělesa to nemá vliv.",
            "Точно такой же след, как при толчке сзади. Силу можно сдвигать **вдоль её линии действия** — на движение тела это не влияет.",
        ))
        2 -> Note(if (guess == 1) Tone.OK else Tone.INFO, L(
            (if (guess == 1) "Správně. " else "Ve skutečnosti se točí ↻. ") +
                "Těžiště T jede pořád stejně (křížky dopadají na šedé tečky), ale deska se navíc roztočí. Stejná síla na jiné nositelce má navíc **otáčivý účinek – moment** [[M]] = [[F]] · d (lekce 4).",
            (if (guess == 1) "Верно. " else "На самом деле крутится ↻. ") +
                "Центр тяжести T едет так же (крестики попадают на серые точки), но пластина ещё и закручивается. Та же сила на другой линии действия даёт ещё и **вращающее действие — момент** [[M]] = [[F]] · d (урок 4).",
        ))
        0 -> SmallText(L(
            "Síla jde těžištěm: deska se během strčení rozjíždí (záblesky se rozestupují) a pak jede stálou rychlostí – nic ji nebrzdí.",
            "Сила проходит через центр тяжести: во время толчка пластина разгоняется (вспышки расходятся), а потом едет с постоянной скоростью — её ничто не тормозит.",
        ))
        else -> {}
    }
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
        "Síly měříme **siloměrem** – pružinou, která se natáhne tím víc, čím větší síla za ni tahá. Jednotka je newton (N); ve statice počítáme v **kilonewtonech**: 1 kN = 1000 N. Než začneš věšet: **který předmět má podle tebe tíhu asi 1 kN?**",
        "Силы измеряют **динамометром** — пружиной, которая растягивается тем сильнее, чем больше сила. Единица — ньютон (Н); в статике считаем в **килоньютонах**: 1 кН = 1000 Н. Прежде чем подвешивать: **какой предмет, по-твоему, весит около 1 кН?**",
    ))
    Todo(
        L("Tipni si, co má tíhu asi 1 kN", "Предположи, что весит около 1 кН") to (guess != null),
        L("Zavěs jablko – ručička se skoro nehne", "Подвесь яблоко — стрелка почти не сдвинется") to tried[0],
        L("Najdi předmět, jehož tíha je asi **1 kN**", "Найди предмет, сила тяжести которого около **1 кН**") to tried[3],
        L("Zavěs malé auto (asi 1 t)", "Подвесь маленькую машину (около 1 т)") to tried[4],
    )
    if (guess == null) {
        Chips(things.map { it.name }, null) { guess = it }
    } else {
        SmallText(L(
            "Tvůj tip: **${things[guess!!].name.cs}**. Teď věš předměty na siloměr a ověř to.",
            "Твоё предположение: **${things[guess!!].name.ru}**. Теперь подвешивай предметы и проверь.",
        ))
        Chips(things.map { it.name }, pick) { i ->
            pick = i
            tried[i] = true
            // pružina se ustálí s mírným zákmitem (silně tlumená, aby nevyskočila ze stupnice)
            scope.launch { stretch.animateTo((things[i].kg / 100).toFloat(), spring(dampingRatio = 0.5f, stiffness = 60f)) }
        }
    }
    pick?.let { i ->
        val kn = things[i].kg / 100
        Readout(if (kn < 0.01) "[[G]] ≈ ${num(kn * 1000, 0)} N = ${num(kn, 3)} kN" else "[[G]] ≈ ${num(kn, 2)} kN")
    }
    if (tried[3] && guess != null && guess != 3) Note(Tone.INFO, L(
        "Tip nevyšel – nevadí, odhad sil je potřeba vycvičit. Asi 1 kN má sud se 100 litry vody.",
        "Не угадал — ничего, чувство силы нужно натренировать. Около 1 кН весит бочка со 100 литрами воды.",
    ))
    if (tried[3]) SmallText(L(
        "Pamatuj: **1 kN ≈ tíha 100 kg** (přesně 0,98 kN). Jablko má asi 1 N, malé auto asi 10 kN.",
        "Запомни: **1 кН ≈ сила тяжести 100 кг** (точно 0,98 кН). Яблоко — около 1 Н, маленькая машина — около 10 кН.",
    ))
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
    Quiz(
        question = L("Která šipka má stejný směr (sklon) jako [[F]], ale opačný smysl (hrot na druhou stranu)?", "У какой стрелки тот же наклон (směr), что у [[F]], но противоположное направление (smysl) — остриё в другую сторону?"),
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
    )
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
