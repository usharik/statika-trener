package cz.usharik.statika.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.Prim
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Lekce 4 – Moment síly. Síla umí těleso nejen posunout, ale i otočit. Jak moc otáčí, říká moment
 * M = F · r, kde r je KOLMÁ vzdálenost bodu od nositelky (ne vzdálenost k působišti podél nosníku).
 */

/** Bod (x, y) otočený kolem (px, py) o [deg] stupňů (osa y dolů → kladný úhel = po směru hodinek). */
private fun turn(x: Double, y: Double, px: Double, py: Double, deg: Double): Pair<Double, Double> {
    val a = deg * PI / 180
    val dx = x - px
    val dy = y - py
    return (px + dx * cos(a) - dy * sin(a)) to (py + dx * sin(a) + dy * cos(a))
}

/** Šestihranná matice (pohled shora) s ryskou, která se otáčí. */
internal fun PrimBuilder.bolt(cx: Double, cy: Double, r: Double, spin: Double, c: StatikaColors) {
    val pts = (0 until 6).map { i -> val a = (i * 60.0 + 30) * PI / 180; (cx + r * cos(a)) to (cy + r * sin(a)) }
    poly(pts, c.subtle, c.ink, 1.6)
    circle(cx, cy, r * 0.45, c.paper, c.ink, 1.3)
    val a = spin * PI / 180
    line(cx, cy, cx + r * 0.8 * cos(a), cy + r * 0.8 * sin(a), c.accent, 3.0)
}

/** Moment, při kterém zarezlá matice povolí [N·m]. */
private const val BREAK_NM = 60.0

/** Klíč (pohled shora) otočený kolem matice o [deg] (záporně = proti směru hodinových ručiček). */
private fun PrimBuilder.wrenchBody(px: Double, py: Double, len: Double, w: Double, deg: Double, c: StatikaColors, alpha: Float = 1f, pipeFrom: Double? = null) {
    val body = listOf(px to py - w / 2, px + len to py - w / 2, px + len to py + w / 2, px to py + w / 2).map { (x, y) -> turn(x, y, px, py, deg) }
    poly(body, c.beam, c.ink, 1.6, alpha)
    if (pipeFrom != null) {
        // nasazená trubka – širší, šedá
        val pipe = listOf(px + pipeFrom to py - w / 2 - 4, px + len + 175 to py - w / 2 - 4, px + len + 175 to py + w / 2 + 4, px + pipeFrom to py + w / 2 + 4)
            .map { (x, y) -> turn(x, y, px, py, deg) }
        poly(pipe, c.subtle, c.inkSoft, 1.4, alpha)
    }
}


/** Tip před pokusem: krok se splní jakoukoli volbou, správnost ukáže až následující pokus. */
private fun tipStep(label: L, options: List<L>, pick: Int?, onPick: (Int) -> Unit): Step =
    Step(label, pick != null, result = { pick?.let { SmallText(L("Tvůj tip: **${options[it].cs}**", "Твой прогноз: **${options[it].ru}**")) } }) {
        Chips(options, pick) { if (pick == null) onPick(it) }
    }

/** Krok s doplněným výsledkem (např. poznámka navíc pod vysvětlením kvízu). */
private fun Step.plusResult(extra: @Composable () -> Unit) = Step(label, done, { result?.invoke(); extra() }, controls)

/**
 * Karta 0: zarezlá matice. Ruka táhne kolmo ke klíči silou 200 N (později slabší 100 N); matice povolí až při momentu
 * 60 N·m. Pod touto hranicí se nehne nic (drží ji tření), nad ní se klíč s maticí rozjedou a pootočí. Krátké rameno
 * nestačí, delší ano; slabší ruka si nejdřív spočítá potřebné rameno a pak klíč prodlouží trubkou.
 */
@Composable
private fun CardScope.Wrench() {
    val tt = tr()
    val nm = tt.pick(L("N·m", "Н·м"))
    val px = 46.0
    val py = 172.0
    val unit = 330.0 // 1 m = 330 jednotek
    val wrenchLen = 0.5 // m
    var r by remember { mutableDoubleStateOf(0.2) }
    var pipe by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var pulling by remember { mutableStateOf(false) }
    var angle by remember { mutableDoubleStateOf(0.0) }
    var alpha by remember { mutableStateOf(1f) }
    var last by remember { mutableStateOf<Boolean?>(null) } // výsledek posledního pokusu: povolila?
    var tryM by remember { mutableStateOf<Triple<Double, Double, Boolean>?>(null) } // poslední pokus: F, r, povolila
    var failed by remember { mutableStateOf(false) }
    var minArm by remember { mutableStateOf(false) }
    var weak by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val calc = numStep(
        question = L("Slabší ruka má jen 100 N. Jaké rameno potřebuje?", "Рука слабее, всего 100 Н. Какое плечо ей нужно?"),
        fields = listOf(NumSpec(L("[[r]]", "[[r]]"), 0.6, tt.pick(L("m", "м")))),
        explain = L(
            "100 · [[r]] = 60, tedy [[r]] = 0,6 m. Poloviční síla potřebuje dvojnásobné rameno.",
            "100 · [[r]] = 60, то есть [[r]] = 0,6 м. Вдвое меньшей силе нужно вдвое большее плечо.",
        ),
        traps = listOf(
            NumTrap.near(L(
                "To stačilo pro 200 N. Síla je poloviční – co musí udělat rameno, aby součin zůstal 60 N·m?",
                "Этого хватало для 200 Н. Сила вдвое меньше — что должно стать с плечом, чтобы произведение осталось 60 Н·м?",
            ), 0.3),
            NumTrap.near(L("Moment je síla × rameno, ne síla / rameno: 100 · [[r]] = 60.", "Момент — это сила × плечо, а не сила / плечо: 100 · [[r]] = 60."), 1.67),
        ),
        nudge = L("Moment musí dosáhnout 60 N·m: 100 · [[r]] = 60.", "Момент должен дойти до 60 Н·м: 100 · [[r]] = 60."),
    )
    // 0 = první pokus, 1 = nejkratší rameno, 2 = výpočet pro 100 N, 3 = trubka, 4 = hotovo
    val stage = when {
        !failed -> 0
        !minArm -> 1
        !calc.done -> 2
        !weak -> 3
        else -> 4
    }
    val f = if (stage >= 2) 100.0 else 200.0
    val canMove = stage == 1 || stage >= 3
    LaunchedEffect(stage) { tryM = null }
    val maxR = if (pipe) 1.0 else wrenchLen
    fun setR(v: Double) {
        if (running || !canMove) return
        r = (round(v * 20) / 20).coerceIn(0.1, maxR)
        last = null
    }
    fun pull() {
        if (running) return
        running = true
        val ff = f
        val rr = r
        val ok = ff * rr >= BREAK_NM - 1e-9
        scope.launch {
            pulling = true
            last = null
            tryM = null
            if (!ok) {
                // tření v závitu drží: nic se nepohne
                delay(1100)
                pulling = false
                last = false
                tryM = Triple(ff, rr, false)
                failed = true
                delay(300)
            } else {
                // přebytek momentu nad třením rozjíždí klíč i matici – úhel roste čím dál rychleji
                val acc = 40.0 + (ff * rr - BREAK_NM) * 4 // °/s²
                var time = 0.0
                var lastT = 0L
                while (angle > -22.0) {
                    withFrameNanos { now ->
                        val dt = if (lastT == 0L) 0.0 else (now - lastT) / 1e9
                        lastT = now
                        time += dt
                        angle = (-0.5 * acc * time * time).coerceAtLeast(-22.0)
                    }
                }
                pulling = false
                last = true
                tryM = Triple(ff, rr, true)
                if (ff == 200.0 && abs(rr - 0.3) < 1e-9) minArm = true
                if (ff == 100.0) weak = true
                delay(1300)
                // povolená matice zmizí, na její místo přijde nová zarezlá (klíč se nevrací zpátky)
                for (i in 1..8) {
                    alpha = 1f - i / 8f
                    delay(30)
                }
                angle = 0.0
                for (i in 1..8) {
                    alpha = i / 8f
                    delay(30)
                }
            }
            running = false
        }
    }
    val xr = px + r * unit
    Stage(
        h = 240.0,
        description = tt.pick(L("Klíč na zarezlé matici, ruka táhne v nastavitelné vzdálenosti od osy", "Ключ на прикипевшей гайке, рука тянет на изменяемом расстоянии от оси")),
        onDrag = { pt, start ->
            if (start) dragging = !running && canMove && abs(pt.y - py) < 50 && pt.x > px + 15
            if (dragging) setR((pt.x - px) / unit)
        },
        onDragEnd = { dragging = false },
    ) { c ->
        wrenchBody(px, py, wrenchLen * unit, 18.0, angle, c, alpha, if (pipe) wrenchLen * unit - 15 else null)
        bolt(px, py, 24.0, angle - 90, c)
        // ruka táhne kolmo ke klíči (otáčí se s ním)
        val (ax, ay) = turn(xr, py - 9, px, py, angle)
        val (bx, by) = turn(xr, py - 82, px, py, angle)
        if (pulling || !running) force(ax, ay, bx, by, c.load, 3.0, glow = pulling, alpha = alpha)
        if (!running) {
            val right = xr > 300
            text(if (right) xr - 8 else xr + 8, py - 60, " = ${num(f, 0)} N", c.load, 14.0, if (right) Anchor.END else Anchor.START, sym = "F")
            if (canMove) handle(xr, py, c, dragging) else circle(xr, py, 4.0, c.ink)
            // rameno
            line(px, py + 30, xr, py + 30, c.accent, 1.4)
            line(px, py + 23, px, py + 37, c.accent, 1.4)
            line(xr, py + 23, xr, py + 37, c.accent, 1.4)
            text((px + xr) / 2, py + 48, " = ${num(r)} m", c.accent, 13.0, sym = "r")
        }
        when (last) {
            false -> text(200.0, 30.0, tt.pick(L("nic – matice drží", "ничего — гайка держит")), c.load, 14.0, bold = true)
            true -> text(200.0, 30.0, tt.pick(L("povolila! ✓", "открутилась! ✓")), c.accent, 15.0, bold = true)
            null -> text(200.0, 30.0, tt.pick(L("matice povolí až při momentu 60 N·m", "гайка поддастся только при моменте 60 Н·м")), c.inkSoft, 12.0)
        }
    }
    CardText(L(
        "Síla umí těleso i **otočit**. Zarezlá matice povolí, až když ji otáčí **moment** aspoň 60 N·m. Moment síly [[M]] = síla × **rameno** [[r]] (vzdálenost síly od osy matice).",
        "Сила может тело ещё и **повернуть**. Прикипевшая гайка поддастся, только когда её поворачивает **момент** не меньше 60 Н·м. Момент силы [[M]] = сила × **плечо** [[r]] (rameno — расстояние от силы до оси гайки).",
    ))
    // výsledek posledního zatažení – součin se ukáže až po pokusu
    val tryLine: @Composable () -> Unit = {
        tryM?.let { (ff, rr, ok) ->
            Readout("[[M]] = ${num(ff, 0)} · ${num(rr)} = ${num(ff * rr, 0)} $nm " + if (ok) "≥ 60 ✓" else "< 60", ok = ok)
            if (ok && stage == 1) SmallText(L("Povolila – ale nejde to i kratším ramenem?", "Поддалась — а нельзя ли плечом покороче?"))
        }
    }
    val slider: @Composable () -> Unit = {
        ValueSlider(
            L("Rameno [[r]] = ${num(r)} m", "Плечо [[r]] = ${num(r)} м").t(),
            r.toFloat(), 0.1f..maxR.toFloat(), steps = ((maxR - 0.1) * 20).roundToInt() - 1,
        ) { setR(it.toDouble()) }
    }
    Steps(
        Step(
            L("Zatáhni silou 200 N na rameni 0,2 m – povolí matice?", "Потяни с силой 200 Н на плече 0,2 м — поддастся ли гайка?"), failed,
            result = { Note(Tone.INFO, L(
                "Nehnula se: [[M]] = 200 · 0,2 = 40 N·m, a to je míň než 60 N·m. Tření v závitu drží.",
                "Не сдвинулась: [[M]] = 200 · 0,2 = 40 Н·м, а это меньше 60 Н·м. Трение в резьбе держит.",
            )) },
        ) { ActionButton(L("Zatáhni", "Потянуть"), enabled = !running) { pull() } },
        Step(
            L("Najdi **nejkratší** rameno, kterým matici silou 200 N povolíš", "Найди **самое короткое** плечо, которым открутишь гайку силой 200 Н"), minArm,
            result = { SmallText(L(
                "200 N · 0,3 m = 60 N·m – právě dost. O kousek kratší rameno (0,25 m) dá jen 50 N·m.",
                "200 Н · 0,3 м = 60 Н·м — как раз достаточно. Чуть короче (0,25 м) — и выйдет только 50 Н·м.",
            )) },
        ) {
            slider()
            ActionButton(L("Zatáhni", "Потянуть"), enabled = !running) { pull() }
            tryLine()
        },
        calc,
        Step(
            L("Klíč má jen 0,5 m. Prodluž ho trubkou a matici povol", "Ключ всего 0,5 м. Удлини его трубой и открути гайку"), weak,
            result = { Note(Tone.OK, L(
                "Poloviční síla potřebuje dvojnásobné rameno: 100 N · 0,6 m = 60 N·m. Proto mechanici nasazují na klíč trubku. U nosníků budou síly v kN a ramena v metrech – moment pak vyjde v kNm (1 kNm = 1000 N·m).",
                "Вдвое меньшей силе нужно вдвое большее плечо: 100 Н · 0,6 м = 60 Н·м. Поэтому механики надевают на ключ трубу. У балок силы будут в кН, плечи в метрах — момент получится в кН·м (1 кН·м = 1000 Н·м).",
            )) },
        ) {
            Chips(listOf(if (pipe) L("Sundej trubku", "Снять трубу") else L("Nasaď trubku (prodloužení na 1 m)", "Надеть трубу (удлинение до 1 м)")), if (pipe) 0 else null) {
                if (!running) {
                    pipe = !pipe
                    if (!pipe) r = r.coerceAtMost(wrenchLen)
                    last = null
                }
            }
            slider()
            ActionButton(L("Zatáhni", "Потянуть"), enabled = !running) { pull() }
            tryLine()
        },
    )
}

/**
 * Karta 1: šikmá síla – rameno je kolmá vzdálenost k nositelce, ne délka klíče. Stejná zarezlá matice (60 N·m),
 * ruka 200 N na konci klíče 0,4 m: povolí jen dost „kolmý“ tah (sin α ≥ 0,75). Nejdřív tip pro 45° a pokus,
 * pak tah s nulovým momentem a hledání hranice.
 */
@Composable
private fun CardScope.PerpendicularArm() {
    val tt = tr()
    val nm = tt.pick(L("N·m", "Н·м"))
    val px = 70.0
    val py = 130.0
    val len = 0.4 // m
    val unit = 525.0 // 0,4 m = 210 jednotek
    val ex = px + len * unit
    val ey = py
    val f = 200.0
    var alpha by remember { mutableDoubleStateOf(45.0) } // úhel mezi silou a klíčem
    var dragging by remember { mutableStateOf(false) }
    var tip by remember { mutableStateOf<Int?>(null) } // 0 = povolí, 1 = nepovolí
    var tested by remember { mutableStateOf(false) }
    var zero by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var pulling by remember { mutableStateOf(false) }
    var angle by remember { mutableDoubleStateOf(0.0) }
    var fade by remember { mutableStateOf(1f) }
    var last by remember { mutableStateOf<Boolean?>(null) }
    var tryRes by remember { mutableStateOf<Pair<Double, Boolean>?>(null) } // poslední pokus: úhel, povolila
    val okAt = remember { mutableStateListOf<Double>() }
    val failAt = remember { mutableStateListOf<Double>() }
    val scope = rememberCoroutineScope()
    val a = alpha * PI / 180
    // síla na konci klíče: 90° = kolmo nahoru, malý úhel = skoro podél klíče k ose, 0°/180° = přesně podél
    val ux = -cos(a)
    val uy = -sin(a)
    val rPerp = len * sin(a)
    val m = f * rPerp
    // hranice: povolilo při úhlu, o 15° „méně kolmém“ to nešlo (z kterékoli strany od 90°)
    val limit = okAt.any { o -> failAt.any { fl -> abs(o - fl) == 15.0 && abs(fl - 90) > abs(o - 90) } }
    // 0 = tip, 1 = pokus při 45°, 2 = nulový moment, 3 = hranice
    val stage = when {
        tip == null -> 0
        !tested -> 1
        !zero -> 2
        !limit -> 3
        else -> 4
    }
    val canTurn = stage >= 2
    LaunchedEffect(stage) { tryRes = null }
    fun setAlpha(v: Double) {
        if (running || !canTurn) return
        val na = (round(v / 15) * 15).coerceIn(0.0, 180.0)
        if (na != alpha) {
            alpha = na
            last = null
        }
    }
    fun pull() {
        if (running) return
        running = true
        val ok = m >= BREAK_NM - 1e-6
        val at = alpha
        val st = stage
        val noArm = abs(rPerp) < 1e-6
        scope.launch {
            pulling = true
            last = null
            tryRes = null
            if (!ok) {
                delay(1000)
                pulling = false
                last = false
                tryRes = at to false
                if (at !in failAt) failAt += at
                if (st == 1) tested = true
                if (st >= 2 && noArm) zero = true
            } else {
                var time = 0.0
                var lastT = 0L
                val acc = 40.0 + (m - BREAK_NM) * 5
                while (angle > -18.0) {
                    withFrameNanos { now ->
                        val dt = if (lastT == 0L) 0.0 else (now - lastT) / 1e9
                        lastT = now
                        time += dt
                        angle = (-0.5 * acc * time * time).coerceAtLeast(-18.0)
                    }
                }
                pulling = false
                last = true
                tryRes = at to true
                // hranice se hledá až ve 4. kroku (ať se nesplní mimochodem)
                if (st >= 3 && at !in okAt) okAt += at
                delay(1100)
                for (i in 1..8) {
                    fade = 1f - i / 8f
                    delay(30)
                }
                angle = 0.0
                for (i in 1..8) {
                    fade = i / 8f
                    delay(30)
                }
            }
            running = false
        }
    }
    val tipX = ex - ux * 7
    val tipY = ey - uy * 7
    val tailX = ex - ux * 80
    val tailY = ey - uy * 80
    Stage(
        h = 250.0,
        description = tt.pick(L("Šikmá síla na konci klíče a kolmé rameno", "Наклонная сила на конце ключа и её плечо")),
        onDrag = { pt, start ->
            if (start) dragging = !running && canTurn && pt.dist(tailX, tailY) < 60
            if (dragging) {
                // směr síly = od prstu k hrotu na konci klíče
                val dx = pt.x - ex
                val dy = pt.y - ey
                if (hypot(dx, dy) > 8) {
                    var deg = atan2(dy, dx) * 180 / PI
                    if (deg < 0) deg = if (deg < -90) 180.0 else 0.0
                    setAlpha(deg)
                }
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        wrenchBody(px, py, len * unit + 8, 14.0, angle, c, fade)
        bolt(px, py, 22.0, angle - 90, c)
        if (!running || pulling) {
            val (tx0, ty0) = turn(tailX, tailY, px, py, angle)
            val (tx1, ty1) = turn(tipX, tipY, px, py, angle)
            force(tx0, ty0, tx1, ty1, c.load, 3.0, glow = pulling, alpha = fade)
        }
        if (!running) {
            // nositelka síly (čárkovaně) a prodloužení klíče, od kterého měříme úhel
            line(ex - ux * 400, ey - uy * 400, ex + ux * 400, ey + uy * 400, c.inkSoft, 1.0, dash = true)
            line(ex, ey, ex + 46, ey, c.inkSoft, 1.0, dash = true)
            if (alpha > 0.0) prims += Prim.Arc(ex, ey, 30.0, 0.0, alpha, c.inkSoft, 1.0)
            val mid = alpha / 2 * PI / 180
            text(ex + 44 * cos(mid) + 4, ey + 44 * sin(mid) + 5, "${num(alpha, 0)}°", c.inkSoft, 12.0, Anchor.START)
            // pata kolmice z osy na nositelku
            val k = (px - ex) * ux + (py - ey) * uy
            val fx = ex + ux * k
            val fy = ey + uy * k
            line(px, py, fx, fy, c.accent, 2.0, dash = true)
            val vl = hypot(px - fx, py - fy)
            if (vl > 20) {
                val vx = (px - fx) / vl
                val vy = (py - fy) / vl
                val s = 9.0
                val sgn = if ((ex - fx) * ux + (ey - fy) * uy > 0) 1.0 else -1.0
                val ax = fx + ux * s * sgn
                val ay = fy + uy * s * sgn
                line(ax, ay, ax + vx * s, ay + vy * s, c.accent, 1.2)
                line(fx + vx * s, fy + vy * s, ax + vx * s, ay + vy * s, c.accent, 1.2)
                text((px + fx) / 2 - 10, (py + fy) / 2 + (if (fy < py) -4.0 else 16.0), " = ${num(rPerp, 3)} m", c.accent, 14.0, Anchor.END, sym = "r_⊥")
            } else text(px, py + 40, tt.pick(L("nositelka prochází osou: r⊥ = 0", "линия действия проходит через ось: r⊥ = 0")), c.accent, 13.0)
            if (alpha in 1.0..179.0) {
                if (alpha < 90.0) text(tailX - 16, tailY + (if (alpha < 45.0) 22.0 else 5.0), " = 200 N", c.load, 14.0, Anchor.END, sym = "F")
                else text(tailX + 16, tailY + 5, " = 200 N", c.load, 14.0, Anchor.START, sym = "F")
            } else text(tailX, tailY - 14, " = 200 N", c.load, 14.0, sym = "F")
            if (canTurn) handle(tailX, tailY, c, dragging)
        }
        when (last) {
            false -> text(200.0, 238.0, tt.pick(L("nic – matice drží", "ничего — гайка держит")), c.load, 14.0, bold = true)
            true -> text(200.0, 238.0, tt.pick(L("povolila! ✓", "открутилась! ✓")), c.accent, 15.0, bold = true)
            null -> {}
        }
    }
    CardText(L(
        "Stejná zarezlá matice (povolí při 60 N·m), klíč 0,4 m, ruka 200 N na jeho konci – ale táhne **šikmo**. Rameno [[r_⊥]] není délka klíče, ale **kolmá** vzdálenost osy od nositelky síly (zelená čárkovaná).",
        "Та же прикипевшая гайка (поддаётся при 60 Н·м), ключ 0,4 м, рука 200 Н на его конце — но тянет **наискосок**. Плечо [[r_⊥]] — не длина ключа, а расстояние **по перпендикуляру** от оси до линии действия силы (зелёный пунктир).",
    ))
    fun mText(at: Double): String {
        val rp = len * sin(at * PI / 180)
        return "[[M]] = 200 · ${num(rp, 3)} = ${num(f * rp, 0)} $nm"
    }
    val pullButton: @Composable () -> Unit = { ActionButton(L("Zatáhni", "Потянуть"), enabled = !running) { pull() } }
    val slider: @Composable () -> Unit = {
        ValueSlider(L("Úhel síly ke klíči α = ${num(alpha, 0)}°", "Угол силы к ключу α = ${num(alpha, 0)}°").t(), alpha.toFloat(), 0f..180f, steps = 11) { setAlpha(it.toDouble()) }
    }
    val tipOpts = listOf(L("ano, povolí", "да, поддастся"), L("ne, nepovolí", "нет, не поддастся"))
    Steps(
        tipStep(L("Tipni si: povolí matice, když ruka táhne pod úhlem 45°?", "Как думаешь: поддастся ли гайка, если тянуть под углом 45°?"), tipOpts, tip) { tip = it },
        Step(
            L("Zatáhni pod úhlem 45° a ověř svůj tip", "Потяни под углом 45° и проверь свой прогноз"), tested,
            result = { Note(if (tip == 1) Tone.OK else Tone.INFO, L(
                (if (tip == 1) "Tip sedí. " else "Tip nesedí. ") + "Matice nepovolila: šikmý tah má kratší rameno [[r_⊥]] = 0,4 · sin 45° = 0,283 m, takže [[M]] = 200 · 0,283 = 57 N·m < 60.",
                (if (tip == 1) "Прогноз верный. " else "Прогноз неверный. ") + "Гайка не поддалась: у наклонной тяги плечо короче, [[r_⊥]] = 0,4 · sin 45° = 0,283 м, поэтому [[M]] = 200 · 0,283 = 57 Н·м < 60.",
            )) },
        ) { pullButton() },
        Step(
            L("Natoč sílu tak, aby matici **vůbec neotáčela**, a zatáhni", "Поверни силу так, чтобы она гайку **совсем не крутила**, и потяни"), zero,
            result = { SmallText(L(
                "Při 0° nebo 180° míří síla podél klíče a její nositelka prochází osou – rameno 0, moment 0. Táhneš, ale nic neotočíš.",
                "При 0° или 180° сила направлена вдоль ключа, её линия действия проходит через ось — плечо 0, момент 0. Тянешь, но ничего не поворачиваешь.",
            )) },
        ) {
            slider()
            pullButton()
            tryRes?.let { (at, ok) ->
                if (ok) SmallText(L("Povolila (${mText(at)}). Hledáš ale tah, který neotáčí vůbec.", "Поддалась (${mText(at)}). А нужна тяга, которая совсем не крутит."))
                else SmallText(L("Nepovolila, ale otáčet se ji snažíš: ${mText(at)}. Kdy bude rameno nulové?", "Не поддалась, но крутить её ты всё же пытаешься: ${mText(at)}. Когда плечо станет нулевым?"))
            }
        },
        Step(
            L("Najdi **nejšikmější** tah, kterým matice ještě povolí", "Найди **самый наклонный** рывок, при котором гайка ещё поддаётся"), limit,
            result = {
                Note(Tone.OK, L(
                    "Hranice je mezi 45° a 60°: 200 · 0,4 · sin 60° = 69 N·m stačí, 200 · 0,4 · sin 45° = 57 N·m už ne. Největší moment (80 N·m) dá tah kolmo, α = 90°.",
                    "Граница между 45° и 60°: 200 · 0,4 · sin 60° = 69 Н·м хватает, 200 · 0,4 · sin 45° = 57 Н·м — уже нет. Наибольший момент (80 Н·м) даёт тяга перпендикулярно, α = 90°.",
                ))
                LearnJump(108, 3, L("Jak spočítat rameno šikmé síly? → Trigonometrie: rameno = r · sin α", "Как найти плечо наклонной силы? → Тригонометрия: плечо = r · sin α"))
            },
        ) {
            slider()
            pullButton()
            tryRes?.let { (at, ok) ->
                if (ok) SmallText(L("Povolila při ${num(at, 0)}° (${mText(at)}) – zkus táhnout ještě šikměji.", "Поддалась при ${num(at, 0)}° (${mText(at)}) — попробуй тянуть ещё наклоннее."))
                else SmallText(L("Při ${num(at, 0)}° ne: ${mText(at)} < 60.", "При ${num(at, 0)}° — нет: ${mText(at)} < 60."))
            }
        },
    )
}

/** Spirálová pružina v pantu (pohled shora); čím víc jsou dveře otevřené, tím víc je natočená. */
private fun PrimBuilder.hingeSpring(hx: Double, hy: Double, deg: Double, c: StatikaColors) {
    val pts = mutableListOf<Pair<Double, Double>>()
    val turns = 2.2 * 360 + abs(deg) * 2
    var a = 0.0
    while (a <= turns) {
        val rr = 9.0 + 9.0 * a / turns
        val ang = (a - deg) * PI / 180
        pts += (hx + rr * cos(ang)) to (hy + rr * sin(ang))
        a += 12.0
    }
    for (i in 1 until pts.size) line(pts[i - 1].first, pts[i - 1].second, pts[i].first, pts[i].second, c.react, 1.3)
}

/**
 * Karta 2: dveře shora se pružinou v pantu (samozavírací). Stejná síla 20 N na třech místech: pružina se natočí,
 * až její moment k·θ vyrovná moment ruky F·r – dveře se otevřou úměrně ramenu a drží. Pustíš → pružina je zavře.
 * U pantu je rameno 0: nehne se nic. Nejdřív předpověď, pak pokus u kliky; uprostřed a u pantu vždy tip → pokus.
 */
@Composable
private fun CardScope.Door() {
    val tt = tr()
    val nm = tt.pick(L("N·m", "Н·м"))
    val hx = 90.0
    val hy = 150.0
    val len = 220.0 // 0,9 m
    val doorM = 0.9
    val spots = listOf(0.0, 0.5, 0.93) // u pantu, uprostřed, u kliky (podíl šířky dveří)
    val f = 20.0 // N
    val k = 0.48 // N·m na 1° natočení pružiny
    var pushed by remember { mutableStateOf<Int?>(null) }
    var tried by remember { mutableStateOf(setOf<Int>()) }
    var running by remember { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    var theta by remember { mutableDoubleStateOf(0.0) } // ° otevření (kladně = otevřené)
    val scope = rememberCoroutineScope()
    val most = quizStep(
        question = L("Kde stejně velká síla otevře dveře **nejvíc**?", "Где одинаковая сила откроет дверь **шире всего**?"),
        options = listOf(
            Opt(L("u pantu", "у петли"), why = L("U pantu je síla nejblíž ose otáčení. Jaké tam má rameno?", "У петли сила ближе всего к оси вращения. Какое у неё там плечо?")),
            Opt(L("uprostřed", "посередине"), why = L("Je uprostřed rameno největší? Kde je bod nejdál od pantu?", "Разве посередине плечо наибольшее? Где точка дальше всего от петли?")),
            Opt(L("u kliky", "у ручки"), correct = true),
            Opt(L("všude stejně", "везде одинаково"), why = L("Otáčení nezávisí jen na síle. Na čem ještě? Vzpomeň na klíč a matici.", "Поворот зависит не только от силы. От чего ещё? Вспомни ключ и гайку.")),
        ),
        explain = L(
            "U kliky je rameno nejdelší, a tak i moment F · r – pružina se musí natočit nejvíc, než ho vyrovná.",
            "У ручки плечо самое длинное, а значит, и момент F · r — пружине нужно закрутиться сильнее всего, чтобы его уравновесить.",
        ),
        columns = 2,
    )
    val qMid = rememberQuiz(
        L("U kliky to bylo 35°. O kolik se dveře otevřou, když zatlačíš uprostřed?", "У ручки вышло 35°. На сколько откроется дверь, если толкнуть посередине?"),
        listOf(
            Opt(L("taky asi o 35°", "тоже примерно на 35°"), why = L("Síla je stejná, ale rameno? Uprostřed je pant blíž.", "Сила та же, а плечо? Посередине петля ближе.")),
            Opt(L("asi o polovinu", "примерно вдвое меньше"), correct = true),
            Opt(L("skoro vůbec", "почти не откроется"), why = L("Rameno uprostřed není nulové – je zhruba poloviční. Co to udělá s momentem?", "Плечо посередине не нулевое — оно примерно вдвое меньше. Что это сделает с моментом?")),
        ),
        L(
            "Rameno je zhruba poloviční, a tak i moment F · r. Moment pružiny k·θ roste s úhlem – vyrovná ho tedy už zhruba poloviční natočení.",
            "Плечо примерно вдвое меньше, значит, и момент F · r. Момент пружины k·θ растёт с углом — значит, его уравновесит уже примерно вдвое меньший поворот.",
        ),
    )
    val qHinge = rememberQuiz(
        L("A když zatlačíš přímo u pantu?", "А если толкнуть прямо у петли?"),
        listOf(
            Opt(L("otevřou se jen trochu", "откроется чуть-чуть"), why = L("Kudy vede nositelka síly? Jaké má k ose pantu rameno?", "Где проходит линия действия силы? Какое у неё плечо относительно оси петли?")),
            Opt(L("nehnou se vůbec", "не сдвинется вообще"), correct = true),
        ),
        L(
            "Nositelka síly prochází osou pantu – rameno 0, moment 0. Pružina se nemusí natočit vůbec.",
            "Линия действия силы проходит через ось петли — плечо 0, момент 0. Пружине вообще не нужно закручиваться.",
        ),
    )
    // místo, kde má žák právě zatlačit (null = teď se netlačí)
    val want: Int? = when {
        !most.done -> null
        2 !in tried -> 2
        !qMid.solved -> null
        1 !in tried -> 1
        !qHinge.solved -> null
        0 !in tried -> 0
        else -> -1 // hotovo: smí se tlačit kdekoli
    }
    fun push(i: Int) {
        if (running) return
        running = true
        pushed = i
        scope.launch {
            val target = f * spots[i] * doorM / k
            val w0 = 2 * PI / 1.1
            val zeta = 0.45
            var om = 0.0
            var time = 0.0
            var last = 0L
            holding = true
            // ruka tlačí 2,2 s, pak pustí a pružina dveře zavře
            while (time < 4.6) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0.0 else ((now - last) / 1e9).coerceAtMost(0.05)
                    last = now
                    time += dt
                    holding = time < 2.2
                    val goal = if (holding) target else 0.0
                    val acc = w0 * w0 * (goal - theta) - 2 * zeta * w0 * om
                    om += acc * dt
                    theta = (theta + om * dt).coerceAtLeast(0.0) // zárubeň nedovolí otevřít dovnitř
                    if (theta == 0.0 && om < 0) om = 0.0
                }
            }
            theta = 0.0
            holding = false
            tried = tried + i
            running = false
        }
    }
    Stage(
        h = 230.0,
        description = tt.pick(L("Samozavírací dveře viděné shora, pant s pružinou vlevo", "Самозакрывающаяся дверь сверху, петля с пружиной слева")),
        onTap = { pt ->
            val w = want ?: return@Stage
            fun far(i: Int): Double {
                val (sx, sy) = turn(hx + spots[i] * len, hy + 30, hx, hy, -theta)
                return pt.dist(sx, sy)
            }
            val i = (if (w >= 0) listOf(w) else spots.indices.toList()).minByOrNull { far(it) } ?: return@Stage
            if (far(i) < 50) push(i)
        },
    ) { c ->
        rect(8.0, hy - 7, hx - 16, 14.0, c.subtle, c.inkSoft, 1.0)
        rect(hx + len + 8, hy - 7, 400 - (hx + len + 8) - 8, 14.0, c.subtle, c.inkSoft, 1.0)
        text(30.0, hy + 26, tt.pick(L("zeď", "стена")), c.inkSoft, 12.0, Anchor.START)
        val d = -theta
        hingeSpring(hx, hy, theta, c)
        val pts = listOf(hx to hy - 5, hx + len to hy - 5, hx + len to hy + 5, hx to hy + 5).map { (x, y) -> turn(x, y, hx, hy, d) }
        poly(pts, c.beam, c.ink, 1.6)
        circle(hx, hy, 6.0, c.paper, c.ink, 1.8)
        text(hx - 12, hy - 40, tt.pick(L("pant s pružinou", "петля с пружиной")), c.react, 12.0, Anchor.END)
        val (kx, ky) = turn(hx + len - 14, hy + 12, hx, hy, d)
        circle(kx, ky, 5.0, c.ink)
        for (i in spots.indices) {
            val (sx, sy) = turn(hx + spots[i] * len, hy + 6, hx, hy, d)
            val (bx, by) = turn(hx + spots[i] * len, hy + 66, hx, hy, d)
            if (pushed == i && holding) {
                force(bx, by, sx, sy, c.load, glow = true)
                text(bx + 8, by - 6, " = 20 N", c.load, 13.0, Anchor.START, sym = "F")
            } else if (!running && (want == i || want == -1)) {
                val (tx, ty) = turn(hx + spots[i] * len, hy + 30, hx, hy, d)
                tapSpot(tx, ty, c)
            }
        }
        if (theta > 2) {
            // pružina vrací dveře: moment proti otevírání
            moment(hx, hy, 30.0, false, c.react, 1.8)
            text(hx + 36, hy + 40, "k·θ = ${num(k * theta, 0)} N·m", c.react, 12.0, Anchor.START)
        }
        if (running || theta > 0.5) text(390.0, 24.0, "θ = ${num(theta, 0)}°", c.ink, 14.0, Anchor.END, bold = true)
    }
    CardText(L(
        "Samozavírací dveře shora: pružina v pantu se brání tím víc, čím víc dveře otevřeš. Tlačíme vždy stejnou silou 20 N – dveře se otevřou tak daleko, až pružina moment ruky vyrovná.",
        "Самозакрывающаяся дверь сверху: пружина в петле сопротивляется тем сильнее, чем шире открыта дверь. Толкаем всегда с силой 20 Н — дверь открывается, пока пружина не уравновесит момент руки.",
    ))
    fun readout(i: Int): String {
        val r = spots[i] * doorM
        return "[[M]] = 20 · ${num(r)} = ${num(f * r, 1)} $nm → θ = ${num(f * r / k, 0)}°"
    }
    /** předpověď (kvíz) a hned pokus tlačítkem: krok je hotový, až dveře pokus dokončí */
    fun predictThenPush(q: QuizState, i: Int, button: L, note: @Composable () -> Unit) = Step(
        q.question, q.solved && i in tried,
        result = {
            q.Feedback()
            Readout(readout(i))
            note()
        },
    ) {
        if (!q.solved) {
            q.Options()
            q.Feedback()
        } else ActionButton(button, enabled = !running) { push(i) }
    }
    Steps(
        most,
        Step(
            L("Zatlač u kliky a sleduj úhel otevření", "Толкни у ручки и следи за углом открытия"), 2 in tried,
            result = {
                Readout(readout(2))
                Note(Tone.OK, L(
                    "Nejdelší rameno = největší moment = dveře nejvíc otevřené. Proto je klika na kraji dveří, co nejdál od pantu.",
                    "Самое длинное плечо = самый большой момент = дверь открыта шире всего. Поэтому ручка на краю двери, как можно дальше от петли.",
                ))
            },
        ) { ActionButton(L("Zatlač u kliky", "Толкнуть у ручки"), enabled = !running) { push(2) } },
        predictThenPush(qMid, 1, L("Zatlač uprostřed a ověř", "Толкнуть посередине и проверить")) {
            Note(Tone.INFO, L(
                "Uprostřed je rameno asi poloviční než u kliky – moment i úhel otevření jsou taky zhruba poloviční.",
                "Посередине плечо примерно вдвое короче, чем у ручки, — момент и угол открытия тоже примерно вдвое меньше.",
            ))
        },
        predictThenPush(qHinge, 0, L("Zatlač u pantu a ověř", "Толкнуть у петли и проверить")) {
            Note(Tone.INFO, L(
                "Nic! Nositelka síly prochází pantem (osou otáčení) – rameno 0, moment 0. Takhle dveře neotevřeš, ať tlačíš jakkoli silně.",
                "Ничего! Линия действия силы проходит через петлю (ось вращения) — плечо 0, момент 0. Так дверь не открыть, как сильно ни толкай.",
            ))
        },
    )
}

/** Karta 3: smysl momentu – ↺ kladný, ↻ záporný, a síla, jejíž nositelka prochází bodem, neotočí vůbec. */
@Composable
private fun CardScope.Sign() {
    val ox = 200.0
    val oy = 120.0
    // (působiště x, y, směr x, y, smysl: +1 ↺, −1 ↻, 0 = neotočí)
    data class Fq(val x: Double, val y: Double, val dx: Double, val dy: Double, val sign: Int, val label: String)
    val forces = remember {
        listOf(
            Fq(300.0, 120.0, 0.0, -1.0, 1, "F_1"),  // vpravo od bodu, nahoru → ↺
            Fq(100.0, 120.0, 0.0, -1.0, -1, "F_2"), // vlevo od bodu, nahoru → ↻
            Fq(200.0, 40.0, 1.0, 0.0, -1, "F_3"),   // nad bodem, doprava → ↻
            Fq(200.0, 200.0, 1.0, 0.0, 1, "F_4"),   // pod bodem, doprava → ↺
            Fq(300.0, 160.0, -0.92848, -0.37139, 0, "F_5"), // z pravého okraje přímo k bodu O → neotočí
            Fq(100.0, 170.0, 0.8, -0.6, -1, "F_6"), // šikmo; nositelka míjí O nad ním a míří doprava → ↻
        )
    }
    val turnHint = { q: Fq ->
        L(
            "Představ si spojnici O s nositelkou jako ručičku připíchnutou v O (zelená čárkovaná): síla [[${q.label}]] tlačí na její konec. Kam se pootočí?",
            "Представь отрезок от O до линии действия как стрелку, приколотую в O (зелёный пунктир): сила [[${q.label}]] толкает её конец. Куда она повернётся?",
        )
    }
    val throughHint = { q: Fq ->
        L(
            "Prodluž nositelku síly [[${q.label}]] – kudy vede? Když prochází přímo bodem O, je rameno nulové.",
            "Продли линию действия силы [[${q.label}]] — где она проходит? Если прямо через точку O, плечо равно нулю.",
        )
    }
    val quizzes = forces.map { q ->
        val notZero = L("Nositelka [[${q.label}]] bodem O neprochází (je od něj vidět zelená kolmice) – rameno není nulové.", "Линия действия [[${q.label}]] не проходит через O (видно зелёный перпендикуляр от O) — плечо не нулевое.")
        rememberQuiz(
            L("Kam by desku otočila síla [[${q.label}]]?", "В какую сторону повернула бы пластину сила [[${q.label}]]?"),
            listOf(
                Opt(L("↺ (+)", "↺ (+)"), correct = q.sign == 1, why = if (q.sign == 0) throughHint(q) else turnHint(q)),
                Opt(L("↻ (−)", "↻ (−)"), correct = q.sign == -1, why = if (q.sign == 0) throughHint(q) else turnHint(q)),
                Opt(L("M = 0", "M = 0"), correct = q.sign == 0, why = notZero),
            ),
            when (q.sign) {
                1 -> L("[[${q.label}]] otáčí deskou proti směru hodinových ručiček: ↺, moment kladný.", "[[${q.label}]] поворачивает пластину против часовой стрелки: ↺, момент положительный.")
                -1 -> L("[[${q.label}]] otáčí deskou po směru hodinových ručiček: ↻, moment záporný.", "[[${q.label}]] поворачивает пластину по часовой стрелке: ↻, момент отрицательный.")
                else -> L("Nositelka [[${q.label}]] prochází bodem O – rameno 0, moment 0. Deska se neotočí.", "Линия действия [[${q.label}]] проходит через O — плечо 0, момент 0. Пластина не повернётся.")
            },
        )
    }
    val k = quizzes.indexOfFirst { !it.solved }.let { if (it < 0) forces.size else it }
    // po chybě ukáže obrázek „ručičku“ – kolmici z O na nositelku (prozradí rameno, ne smysl)
    val wrong = k < forces.size && quizzes[k].misses > 0
    Stage(h = 240.0, description = L("Deska přišpendlená v bodě O a šest sil na jejích okrajích", "Пластина, приколотая в точке O, и шесть сил на её краях").t()) { c ->
        rect(100.0, 40.0, 200.0, 160.0, c.subtle, c.line, 1.2, alpha = 0.8f)
        circle(ox, oy, 9.0, c.accent.copy(alpha = 0.12f), c.accent, 1.5)
        line(ox - 13, oy, ox + 13, oy, c.accent, 1.2)
        line(ox, oy - 13, ox, oy + 13, c.accent, 1.2)
        text(ox + 6, oy - 16, "O", c.ink, 15.0, Anchor.START, bold = true)
        forces.forEachIndexed { i, q ->
            val cur = i == k
            val done = i < k
            val a = if (cur) 1f else if (done) 0.55f else 0.25f
            if (cur) {
                // nositelka – podle ní se rozhoduje (u F_5 prochází bodem O)
                line(q.x - q.dx * 400, q.y - q.dy * 400, q.x + q.dx * 400, q.y + q.dy * 400, c.inkSoft, 0.9, dash = true)
                if (wrong && q.sign != 0) {
                    val s = (ox - q.x) * q.dx + (oy - q.y) * q.dy
                    line(ox, oy, q.x + q.dx * s, q.y + q.dy * s, c.accent, 2.0, dash = true)
                }
            }
            val tx = q.x - q.dx * 55
            val ty = q.y - q.dy * 55
            force(tx, ty, q.x, q.y, c.load, glow = cur, alpha = a)
            val lx = if (q.dx < -0.1) tx + 6 else tx + 8
            val ly = ty + (if (abs(q.dy) > 0.5) 14.0 else -8.0)
            text(lx, ly, null, c.load, 14.0, Anchor.START, sym = q.label, alpha = a)
            if (done) {
                val mark = when (q.sign) { 1 -> "↺ +"; -1 -> "↻ −"; else -> "0" }
                text(q.x + (if (q.x > 250) -18.0 else 14.0), q.y + (if (q.y > 180) -10.0 else 18.0), mark, c.accent, 14.0, if (q.x > 250) Anchor.END else Anchor.START, bold = true)
            }
        }
        // potvrzení správné odpovědi: oblouk momentu u O (nebo „M = 0“)
        if (k > 0) {
            val q = forces[k - 1]
            if (q.sign != 0) moment(ox, oy, 34.0, q.sign > 0, c.accent, 2.0)
            else text(ox, oy + 36, "M = 0", c.accent, 14.0, bold = true)
        }
    }
    CardText(L(
        "Deska je v bodě O přišpendlená a může se kolem něj otáčet. Rozhoduje **nositelka** síly (čárkovaně): na které straně od O vede a kam míří. Moment proti směru hodinových ručiček ↺ počítáme +, po směru ↻ −.",
        "Пластина приколота в точке O и может вокруг неё вращаться. Решает **линия действия** силы (пунктир): с какой стороны от O она проходит и куда направлена. Момент против часовой стрелки ↺ считаем со знаком +, по часовой ↻ — со знаком −.",
    ))
    Steps(
        *quizzes.mapIndexed { i, q ->
            Step(q.question, q.solved, result = {
                q.Feedback()
                if (i == quizzes.lastIndex) {
                    Note(Tone.OK, L(
                        "Výborně! ↺ bereme kladně, ↻ záporně – stejně jako v trenažéru. A síla, jejíž nositelka prochází bodem, k němu moment nemá (rameno 0) – to se nám u nosníků bude hodit.",
                        "Отлично! ↺ берём со знаком плюс, ↻ — со знаком минус, как и в тренажёре. А у силы, линия действия которой проходит через точку, момента относительно неё нет (плечо 0) — это нам пригодится у балок.",
                    ))
                    SmallText(L(
                        "Který smysl je kladný, je jen dohoda – důležité je držet se jí v celém výpočtu.",
                        "Какое направление считать положительным — лишь договорённость; главное — держаться её во всём расчёте.",
                    ))
                }
            }) {
                q.Options(columns = 3)
                if (!q.solved) q.Feedback()
            }
        }.toTypedArray(),
    )
}

/**
 * Karta 4: dvojice sil – jen otáčí. Ruční kolo ventilu (průměr 0,5 m), každá ruka 20 N. Krátký záběr kolo roztočí,
 * ruce pustí a tření ve ventilu ho zastaví. Součet sil 0, moment ke středu a pak k bodu mimo kolo – vyjde stejně.
 * Nakonec dvojici nahradíme momentem M = F · d (oblouk).
 */
@Composable
private fun CardScope.Couple() {
    val cx = 200.0
    val cy = 105.0
    val rad = 70.0 // d = 0,5 m ↔ 140 jednotek
    val perM = 280.0
    var spin by remember { mutableDoubleStateOf(0.0) }
    var pushing by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val morph = remember { Animatable(0f) } // 0 = síly, 1 = oblouk M
    var point by remember { mutableStateOf(0) }
    var turned by remember { mutableStateOf(false) }
    var tip by remember { mutableStateOf<Int?>(null) }
    var replaced by remember { mutableStateOf(false) }
    val ps = listOf(-0.25, 0.25, 0.75) // poloha bodu od levé síly [m]
    val xl = cx - rad
    val xr = cx + rad
    val tt = tr()
    val nm = tt.pick(L("N·m", "Н·м"))
    fun turn() {
        if (running) return
        running = true
        scope.launch {
            var om = 0.0
            var time = 0.0
            var last = 0L
            // záběr 0,8 s: dvojice (10 N·m) přemáhá tření (4 N·m) – kolo zrychluje; pak ruce pustí a tření ho zastaví
            while (time < 0.8 || om < 0) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0.0 else ((now - last) / 1e9).coerceAtMost(0.05)
                    last = now
                    time += dt
                    pushing = time < 0.8
                    om += (if (pushing) -240.0 else 160.0) * dt // °/s², ↺ = záporně
                    if (!pushing && om > 0) om = 0.0
                    spin += om * dt
                }
            }
            pushing = false
            turned = true
            running = false
        }
    }
    val sum = quizStep(
        question = L("Levá síla 20 N míří dolů, pravá 20 N nahoru. Kolik je jejich součet?", "Левая сила 20 Н направлена вниз, правая 20 Н — вверх. Чему равна их сумма?"),
        label = L("Kolik je součet obou sil?", "Чему равна сумма обеих сил?"),
        options = listOf(
            Opt(L("40 N", "40 Н"), why = L("Míří proti sobě – ve svislém směru se odečtou.", "Они направлены навстречу — по вертикали вычитаются.")),
            Opt(L("0", "0"), correct = true),
            Opt(L("20 N", "20 Н"), why = L("Obě síly jsou stejně velké a míří proti sobě. Co zbude?", "Обе силы равны и направлены навстречу. Что останется?")),
        ),
        explain = L(
            "Součet je 0 – proto dvojice **nikam neposouvá** (střed kola zůstal na místě), jen otáčí.",
            "Сумма равна 0 — поэтому пара **никуда не сдвигает** (центр маховика остался на месте), а только поворачивает.",
        ),
        columns = 3,
    )
    val center = numStep(
        question = L("Spočítej moment dvojice ke středu kola (ramena 0,25 m)", "Найди момент пары относительно центра маховика (плечи 0,25 м)"),
        fields = listOf(NumSpec(L("[[M]]", "[[M]]"), 10.0, nm)),
        explain = L(
            "Obě síly otáčejí kolem středu stejným smyslem ↺: [[M]] = 20 · 0,25 + 20 · 0,25 = 10 N·m.",
            "Обе силы поворачивают вокруг центра в одну сторону ↺: [[M]] = 20 · 0,25 + 20 · 0,25 = 10 Н·м.",
        ),
        traps = listOf(
            NumTrap.near(L("To je moment jedné síly. Druhá otáčí stejným smyslem – přičti ji.", "Это момент одной силы. Вторая крутит в ту же сторону — прибавь её."), 5.0),
            NumTrap.near(L("Síly se vyruší v součtu **sil**. Kam ale každá z nich kolo otáčí?", "Силы уничтожаются в сумме **сил**. Но в какую сторону каждая из них крутит маховик?"), 0.0),
        ),
        nudge = L("Moment každé síly = 20 N × 0,25 m. Obě točí kolem středu stejně (↺).", "Момент каждой силы = 20 Н × 0,25 м. Обе крутят вокруг центра одинаково (↺)."),
    )
    val tipOpts = listOf(L("větší", "больше"), L("menší", "меньше"), L("stejný", "тот же"))
    val atP = numStep(
        question = L("Spočítej moment dvojice k bodu P (ramena na obrázku)", "Найди момент пары относительно точки P (плечи на рисунке)"),
        fields = listOf(NumSpec(L("[[M]]", "[[M]]"), 10.0, nm)),
        explain = L(
            "Kolem P otáčí levá síla ↻ a pravá ↺: [[M]] = −20 · 0,25 + 20 · 0,75 = −5 + 15 = 10 N·m – stejně jako ke středu.",
            "Вокруг P левая сила крутит ↻, а правая ↺: [[M]] = −20 · 0,25 + 20 · 0,75 = −5 + 15 = 10 Н·м — как и относительно центра.",
        ),
        traps = listOf(
            NumTrap.near(L("Kolem P neotáčejí obě síly stejně: levá ↻, pravá ↺ – jeden moment odečti.", "Вокруг P силы крутят по-разному: левая ↻, правая ↺ — один момент вычти."), 20.0),
            NumTrap.near(L("To je jen levá síla (20 · 0,25). Přidej i pravou – otáčí opačně.", "Это только левая сила (20 · 0,25). Добавь и правую — она крутит в обратную сторону."), 5.0),
            NumTrap.near(L("To je jen pravá síla (20 · 0,75). Přidej i levou – otáčí opačně.", "Это только правая сила (20 · 0,75). Добавь и левую — она крутит в обратную сторону."), 15.0),
        ),
        nudge = L("Každá síla × její rameno k P, se znaménkem: ↺ +, ↻ −.", "Каждая сила × её плечо до P, со знаком: ↺ +, ↻ −."),
    )
    // 0 záběr, 1 součet, 2 moment ke středu, 3 tip, 4 moment k P, 5 náhrada momentem, 6 hotovo
    val stage = when {
        !turned -> 0
        !sum.done -> 1
        !center.done -> 2
        tip == null -> 3
        !atP.done -> 4
        !replaced -> 5
        else -> 6
    }
    LaunchedEffect(stage == 5) {
        if (stage != 5) return@LaunchedEffect
        delay(900)
        morph.animateTo(1f, tween(1600))
        replaced = true
    }
    val shownPoint: Int? = when {
        stage < 3 -> null
        stage < 6 -> 0
        else -> point
    }
    Stage(h = 230.0, description = L("Ruční kolo ventilu otáčené dvojicí sil", "Маховик вентиля, который поворачивает пара сил").t()) { c ->
        circle(cx, cy, rad, null, c.ink, 5.0)
        for (i in 0 until 3) {
            val a = (spin + i * 120 - 90) * PI / 180
            line(cx, cy, cx + rad * cos(a), cy + rad * sin(a), c.ink, 3.0)
        }
        circle(cx, cy, 10.0, c.subtle, c.ink, 1.5)
        val mv = morph.value
        val fa = (if (running && !pushing) 0.25f else 1f) * (1f - 0.82f * mv)
        force(xl, cy - 62, xl, cy - 7, c.load, glow = pushing, alpha = fa)
        force(xr, cy + 62, xr, cy + 7, c.load, glow = pushing, alpha = fa)
        text(xl - 8, cy - 46, " = 20 N", c.load, 13.0, Anchor.END, sym = "F", alpha = fa)
        text(xr + 8, cy + 52, " = 20 N", c.load, 13.0, Anchor.START, sym = "F", alpha = fa)
        if (running && !pushing) text(12.0, 20.0, tt.pick(L("ruce pustily – tření ve ventilu kolo zastaví", "руки отпустили — трение в вентиле остановит колесо")), c.inkSoft, 12.0, Anchor.START)
        if (mv > 0.05f) {
            moment(cx, cy, 34.0, true, c.load.copy(alpha = mv), 2.6)
            text(cx, cy - rad - 12, " = 10 N·m", c.load, 15.0, sym = "M", bold = true, alpha = mv)
        }
        val yd = cy + 105
        val pt = shownPoint
        if (pt == null) {
            line(xl, yd, xr, yd, c.accent, 1.2)
            line(xl, yd - 6, xl, yd + 6, c.accent, 1.2)
            line(xr, yd - 6, xr, yd + 6, c.accent, 1.2)
            text(cx, yd - 6, " = 0,5 m", c.accent, 13.0, sym = "d")
        } else {
            val x = xl + ps[pt] * perM
            circle(x, cy, 8.0, c.accent.copy(alpha = 0.15f), c.accent, 1.5)
            line(x - 11, cy, x + 11, cy, c.accent, 1.2)
            line(x, cy - 11, x, cy + 11, c.accent, 1.2)
            text(x, cy + 26, "P", c.accent, 14.0, bold = true)
            line(x, cy + 14, x, yd + 6, c.accent, 0.8, dash = true)
            for ((fxp, ly) in listOf(xl to yd - 14, xr to yd)) {
                line(minOf(x, fxp), ly, maxOf(x, fxp), ly, c.accent, 1.2)
                line(fxp, ly - 5, fxp, ly + 5, c.accent, 1.2)
                text((x + fxp) / 2, ly - 4, "${num(abs(fxp - x) / perM)} m", c.accent, 12.0)
            }
        }
    }
    CardText(L(
        "Ruce na kole ventilu: dvě stejně velké síly (20 N) opačného smyslu na rovnoběžných nositelkách 0,5 m od sebe. Takovým dvěma silám se říká **dvojice sil**.",
        "Руки на маховике вентиля: две равные силы (20 Н) противоположного направления на параллельных линиях действия в 0,5 м друг от друга. Такие две силы называют **парой сил**.",
    ))
    Steps(
        Step(
            L("Zaber za kolo oběma rukama a sleduj, co udělá", "Поверни маховик обеими руками и посмотри, что будет"), turned,
            result = { Note(Tone.INFO, L(
                "Kolo se roztočilo, ale jeho střed zůstal na místě. Když ruce pustily, tření ve ventilu ho zastavilo.",
                "Маховик закрутился, но его центр остался на месте. Когда руки отпустили, трение в вентиле его остановило.",
            )) },
        ) { ActionButton(L("Zaber za kolo", "Повернуть маховик"), enabled = !running) { turn() } },
        sum,
        center,
        tipStep(L("Bod P posuneme vlevo mimo kolo. Jaký bude moment dvojice k P?", "Сдвинем точку P влево за маховик. Каким будет момент пары относительно P?"), tipOpts, tip) { tip = it },
        atP.plusResult {
            Note(if (tip == 2) Tone.OK else Tone.INFO, L(
                (if (tip == 2) "Tip sedí. " else "Tip nesedí. ") + "Ke každému bodu vyjde stejně: [[M]] = [[F]] · [[d]] = 20 · 0,5 = 10 N·m, kde [[d]] je vzdálenost nositelek obou sil.",
                (if (tip == 2) "Прогноз верный. " else "Прогноз неверный. ") + "Относительно любой точки выходит одно и то же: [[M]] = [[F]] · [[d]] = 20 · 0,5 = 10 Н·м, где [[d]] — расстояние между линиями действия сил.",
            ))
        },
        Step(
            L("Sleduj: dvojici nahradíme momentem M a kreslíme ji obloukem", "Смотри: пару заменяем моментом M и рисуем дугой"), replaced,
            result = { SmallText(L(
                "Proto moment dvojice přičítáš rovnou jako [[M]], bez ramene – ať počítáš ke kterémukoli bodu. Do součtu sil dvojice nepřispívá: její síly se navzájem vyruší.",
                "Поэтому момент пары прибавляешь сразу как [[M]], без плеча, — относительно какой бы точки ты ни считал. В сумму сил пара ничего не добавляет: её силы уравновешивают друг друга.",
            )) },
        ) { SmallText(L("Síly se promění v oblouk…", "Силы превращаются в дугу…")) },
        after = {
            SmallText(L("Bod P, ke kterému moment dvojice počítáš:", "Точка P, относительно которой считаешь момент пары:"))
            Chips(listOf(L("vlevo", "слева"), L("střed", "центр"), L("vpravo", "справа")), point) { point = it }
            val p = ps[point]
            fun part(v: Double, arm: Double, first: Boolean): String {
                val glyph = if (v >= 0) "↺" else "↻"
                val sign = if (v < 0) (if (first) "−" else " − ") else if (first) "" else " + "
                return "${sign}20·${num(arm)} $glyph"
            }
            Readout("[[M]] = ${part(20 * p, abs(p), true)}${part(20 * (0.5 - p), abs(0.5 - p), false)} = 10 $nm")
        },
    )
}

/** Karta 5: kontrolní otázka – která síla má k bodu A největší moment? */
@Composable
private fun CardScope.Check() {
    val y = 110.0
    val xa = 50.0
    val u = 65.0
    val xe = xa + 4 * u
    Stage(h = 220.0, description = L("Nosník se třemi silami a bodem A", "Балка с тремя силами и точкой A").t()) { c ->
        beam(xa - 10, xe, y, c)
        support(SupportType.PIN, xa, y, c, "A", ghost = true)
        force(xa + u, y - 82, xa + u, y - 6, c.load)
        text(xa + u + 8, y - 64, " = 10 kN", c.load, 14.0, Anchor.START, sym = "F_1")
        force(xa + 3 * u, y - 58, xa + 3 * u, y - 6, c.load)
        text(xa + 3 * u + 8, y - 44, " = 4 kN", c.load, 14.0, Anchor.START, sym = "F_2")
        // vodorovná síla tlačí na konec nosníku v jeho ose
        force(xe + 62, y, xe + 1, y, c.load)
        text(xe + 31, y - 14, " = 8 kN", c.load, 14.0, sym = "F_3")
        // kóty
        val yd = y + 70
        line(xa, yd, xe, yd, c.ink, 0.9)
        for (x in listOf(xa, xa + u, xa + 3 * u, xe)) line(x - 4, yd + 4, x + 4, yd - 4, c.ink, 1.3)
        text(xa + u / 2, yd - 5, "1 m", c.ink, 13.0)
        text(xa + 2 * u, yd - 5, "2 m", c.ink, 13.0)
        text(xa + 3.5 * u, yd - 5, "1 m", c.ink, 13.0)
    }
    CardText(L("Na nosník působí tři síly. Porovnej jejich momenty k bodu A.", "На балку действуют три силы. Сравни их моменты относительно точки A."))
    Steps(
        quizStep(
            question = L("Která síla má k bodu A **největší moment** (co do velikosti)?", "У какой силы **самый большой по величине** момент относительно A?"),
            options = listOf(
                Opt(
                    L("[[F_1]]", "[[F_1]]"),
                    why = L("[[F_1]] je největší síla, ale jak dlouhé má rameno? Spočítej [[F]] · [[r]] pro [[F_1]] i [[F_2]].", "[[F_1]] — самая большая сила, но какое у неё плечо? Посчитай [[F]] · [[r]] для [[F_1]] и [[F_2]]."),
                ),
                Opt(L("[[F_2]]", "[[F_2]]"), correct = true),
                Opt(
                    L("[[F_3]]", "[[F_3]]"),
                    why = L("[[F_3]] je sice nejdál, ale míří podél nosníku – její nositelka prochází bodem A. Jaké má rameno?", "[[F_3]] дальше всех, но направлена вдоль балки — её линия действия проходит через точку A. Какое у неё плечо?"),
                ),
            ),
            nudge = L("Moment = síla × rameno, tedy kolmá vzdálenost bodu A od nositelky síly. Vzdálenost po nosníku je ramenem jen u sil kolmých k nosníku.", "Момент = сила × плечо, то есть расстояние по перпендикуляру от точки A до линии действия силы. Расстояние вдоль балки — плечо только для сил, перпендикулярных балке."),
            explain = L(
                "[[F_2]]: velikost 4 · 3 = 12 kNm, [[F_1]]: 10 · 1 = 10 kNm. Obě otáčejí kolem A po směru hodinových ručiček ↻, takže se znaménkem: [[M_1]] = −10 kNm, [[M_2]] = −12 kNm. Nositelka [[F_3]] prochází bodem A – rameno 0, moment 0. Ani největší síla, ani nejvzdálenější působiště nerozhoduje – rozhoduje součin.",
                "[[F_2]]: по величине 4 · 3 = 12 кН·м, [[F_1]]: 10 · 1 = 10 кН·м. Обе поворачивают вокруг A по часовой стрелке ↻, поэтому со знаком: [[M_1]] = −10 кН·м, [[M_2]] = −12 кН·м. Линия действия [[F_3]] проходит через точку A — плечо 0, момент 0. Решает не самая большая сила и не самая дальняя точка, а произведение.",
            ),
            columns = 3,
        ),
    )
}

internal fun lesson4() = Lesson(
    id = 4,
    title = L("Moment síly", "Момент силы"),
    summary = L("Jak moc síla otáčí: síla × kolmé rameno. Kdy moment povolí matici a kdy je nulový.", "Насколько сила поворачивает: сила × плечо по перпендикуляру. Когда момент открутит гайку и когда он равен нулю."),
    cards = listOf(
        Card { Wrench() },
        Card { PerpendicularArm() },
        Card { Door() },
        Card { Sign() },
        Card { Couple() },
        Card { Check() },
    ),
)
