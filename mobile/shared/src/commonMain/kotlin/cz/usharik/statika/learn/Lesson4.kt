package cz.usharik.statika.learn

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

/**
 * Karta 0: zarezlá matice. Ruka táhne kolmo ke klíči silou 200 N (nebo slabší 100 N); matice povolí až při momentu
 * 60 N·m. Pod touto hranicí se nehne nic (drží ji tření), nad ní se klíč s maticí rozjedou a pootočí. Krátké rameno
 * nestačí, delší ano, slabší ruka potřebuje trubku na prodloužení.
 */
@Composable
private fun CardScope.Wrench() {
    val tt = tr()
    val px = 46.0
    val py = 172.0
    val unit = 330.0 // 1 m = 330 jednotek
    val wrenchLen = 0.5 // m
    var r by remember { mutableDoubleStateOf(0.2) }
    var f by remember { mutableDoubleStateOf(200.0) }
    var pipe by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var pulling by remember { mutableStateOf(false) }
    var angle by remember { mutableDoubleStateOf(0.0) }
    var alpha by remember { mutableStateOf(1f) }
    var last by remember { mutableStateOf<Boolean?>(null) } // výsledek posledního pokusu: povolila?
    var failed by remember { mutableStateOf(false) }
    var minArm by remember { mutableStateOf(false) }
    var weak by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val maxR = if (pipe) 1.0 else wrenchLen
    val m = f * r
    fun setR(v: Double) {
        if (running) return
        r = (round(v * 20) / 20).coerceIn(0.1, maxR)
        last = null
    }
    fun pull() {
        if (running) return
        running = true
        val ok = m >= BREAK_NM - 1e-9
        val ff = f
        val rr = r
        scope.launch {
            pulling = true
            last = null
            if (!ok) {
                // tření v závitu drží: nic se nepohne
                delay(1100)
                pulling = false
                last = false
                failed = true
                delay(300)
            } else {
                // přebytek momentu nad třením rozjíždí klíč i matici – úhel roste čím dál rychleji
                val acc = 40.0 + (f * r - BREAK_NM) * 4 // °/s²
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
            if (start) dragging = !running && abs(pt.y - py) < 50 && pt.x > px + 15
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
            handle(xr, py, c, dragging)
            // rameno
            line(px, py + 30, xr, py + 30, c.accent, 1.4)
            line(px, py + 23, px, py + 37, c.accent, 1.4)
            line(xr, py + 23, xr, py + 37, c.accent, 1.4)
            text((px + xr) / 2, py + 48, " = ${num(r)} m", c.accent, 13.0, sym = "r")
        }
        when (last) {
            false -> text(200.0, 30.0, tt.pick(L("nic – matice drží (M < 60 N·m)", "ничего — гайка держит (M < 60 Н·м)")), c.load, 14.0, bold = true)
            true -> text(200.0, 30.0, tt.pick(L("povolila! ✓", "открутилась! ✓")), c.accent, 15.0, bold = true)
            null -> text(200.0, 30.0, tt.pick(L("matice povolí až při momentu 60 N·m", "гайка поддастся только при моменте 60 Н·м")), c.inkSoft, 12.0)
        }
    }
    CardText(L(
        "Síla umí těleso i **otočit**. Zarezlá matice povolí, až když ji otáčí **moment** aspoň 60 N·m. Moment síly [[M]] = síla × **rameno** [[r]] (vzdálenost síly od osy matice). Posuň ruku po klíči (kroužek nebo posuvník) a zatáhni.",
        "Сила может тело ещё и **повернуть**. Прикипевшая гайка поддастся, только когда её поворачивает **момент** не меньше 60 Н·м. Момент силы [[M]] = сила × **плечо** [[r]] (rameno — расстояние от силы до оси гайки). Сдвинь руку вдоль ключа (кружком или ползунком) и потяни.",
    ))
    Todo(
        L("Zatáhni silou 200 N na krátkém rameni – povolí matice?", "Потяни с силой 200 Н на коротком плече — поддастся ли гайка?") to failed,
        L("Najdi **nejkratší** rameno, kterým matici silou 200 N povolíš", "Найди **самое короткое** плечо, которым открутишь гайку силой 200 Н") to minArm,
        L("Slabší ruka, jen 100 N: povol matici i tak (klíč je dlouhý jen 0,5 m – co s tím?)", "Рука слабее, всего 100 Н: открути гайку и так (ключ длиной всего 0,5 м — что делать?)") to weak,
    )
    ValueSlider(
        L("Rameno [[r]] = ${num(r)} m", "Плечо [[r]] = ${num(r)} м").t(),
        r.toFloat(), 0.1f..maxR.toFloat(), steps = ((maxR - 0.1) * 20).roundToInt() - 1,
    ) { setR(it.toDouble()) }
    Chips(listOf(L("ruka 200 N", "рука 200 Н"), L("slabší ruka 100 N", "рука слабее 100 Н")), if (f == 200.0) 0 else 1) {
        if (!running) {
            f = if (it == 0) 200.0 else 100.0
            last = null
        }
    }
    Chips(listOf(if (pipe) L("Sundej trubku", "Снять трубу") else L("Nasaď trubku (prodloužení na 1 m)", "Надеть трубу (удлинение до 1 м)")), if (pipe) 0 else null) {
        if (!running) {
            pipe = !pipe
            if (!pipe) r = r.coerceAtMost(wrenchLen)
            last = null
        }
    }
    Readout("[[M]] = [[F]] · [[r]] = ${num(f, 0)} · ${num(r)} = ${num(m, 0)} N·m", ok = m >= BREAK_NM - 1e-9)
    ActionButton(L("Zatáhni", "Потянуть"), enabled = !running) { pull() }
    if (minArm && failed) SmallText(L(
        "200 N · 0,3 m = 60 N·m – právě dost. Na kratším rameni ani silnější tah nepomůže tolik jako delší klíč.",
        "200 Н · 0,3 м = 60 Н·м — как раз достаточно. На коротком плече даже сильный рывок помогает меньше, чем длинный ключ.",
    ))
    if (weak) Note(Tone.OK, L(
        "Poloviční síla potřebuje dvojnásobné rameno: 100 N · 0,6 m = 60 N·m. Proto mechanici nasazují na klíč trubku. U nosníků budou síly v kN a ramena v metrech – moment pak vyjde v kNm (1 kNm = 1000 N·m).",
        "Вдвое меньшей силе нужно вдвое большее плечо: 100 Н · 0,6 м = 60 Н·м. Поэтому механики надевают на ключ трубу. У балок силы будут в кН, плечи в метрах — момент получится в кН·м (1 кН·м = 1000 Н·м).",
    ))
}

/**
 * Karta 1: šikmá síla – rameno je kolmá vzdálenost k nositelce, ne délka klíče. Stejná zarezlá matice (60 N·m),
 * ruka 200 N na konci klíče 0,4 m: povolí jen dost „kolmý“ tah (sin α ≥ 0,75).
 */
@Composable
private fun CardScope.PerpendicularArm() {
    val tt = tr()
    val px = 70.0
    val py = 130.0
    val len = 0.4 // m
    val unit = 525.0 // 0,4 m = 210 jednotek
    val ex = px + len * unit
    val ey = py
    val f = 200.0
    var alpha by remember { mutableDoubleStateOf(45.0) } // úhel mezi silou a klíčem
    var dragging by remember { mutableStateOf(false) }
    var zero by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var pulling by remember { mutableStateOf(false) }
    var angle by remember { mutableDoubleStateOf(0.0) }
    var fade by remember { mutableStateOf(1f) }
    var last by remember { mutableStateOf<Boolean?>(null) }
    val okAt = remember { mutableStateListOf<Double>() }
    val failAt = remember { mutableStateListOf<Double>() }
    val scope = rememberCoroutineScope()
    val a = alpha * PI / 180
    // síla na konci klíče: 90° = kolmo nahoru, malý úhel = skoro podél klíče k ose, 0°/180° = přesně podél
    val ux = -cos(a)
    val uy = -sin(a)
    val rPerp = len * sin(a)
    val m = f * rPerp
    fun setAlpha(v: Double) {
        if (running) return
        val na = (round(v / 15) * 15).coerceIn(0.0, 180.0)
        if (na != alpha) {
            alpha = na
            last = null
        }
    }
    LaunchedEffect(alpha) {
        if (alpha == 0.0 || alpha == 180.0) {
            delay(500)
            zero = true
        }
    }
    // hranice: povolilo při úhlu, o 15° „méně kolmém“ to nešlo (z kterékoli strany od 90°)
    val limit = okAt.any { o -> failAt.any { fl -> abs(o - fl) == 15.0 && abs(fl - 90) > abs(o - 90) } }
    fun pull() {
        if (running) return
        running = true
        val ok = m >= BREAK_NM - 1e-6
        val at = alpha
        scope.launch {
            pulling = true
            last = null
            if (!ok) {
                delay(1000)
                pulling = false
                last = false
                if (at !in failAt) failAt += at
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
                if (at !in okAt) okAt += at
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
            if (start) dragging = !running && pt.dist(tailX, tailY) < 60
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
            handle(tailX, tailY, c, dragging)
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
    Todo(
        L("Natoč sílu tak, aby moment byl **nulový** – i když ruka táhne plnou silou", "Поверни силу так, чтобы момент был **нулевым** — хотя рука тянет в полную силу") to zero,
        L("Najdi hranici: úhel, při kterém matice ještě povolí, když o 15° šikměji už ne (zkoušej tlačítkem „Zatáhni“)", "Найди границу: угол, при котором гайка ещё поддаётся, а на 15° наклоннее — уже нет (пробуй кнопкой «Потянуть»)") to limit,
    )
    ValueSlider(L("Úhel síly ke klíči α = ${num(alpha, 0)}°", "Угол силы к ключу α = ${num(alpha, 0)}°").t(), alpha.toFloat(), 0f..180f, steps = 11) { setAlpha(it.toDouble()) }
    Readout("[[M]] = [[F]] · [[r_⊥]] = 200 · ${num(rPerp, 3)} = ${num(m, 0)} N·m", ok = m >= BREAK_NM - 1e-6)
    ActionButton(L("Zatáhni", "Потянуть"), enabled = !running) { pull() }
    if (zero) SmallText(L(
        "Při 0° nebo 180° míří síla podél klíče a její nositelka prochází osou – rameno 0, moment 0. Táhneš, ale nic neotočíš.",
        "При 0° или 180° сила направлена вдоль ключа, её линия действия проходит через ось — плечо 0, момент 0. Тянешь, но ничего не поворачиваешь.",
    ))
    if (limit) Note(Tone.OK, L(
        "Hranice je mezi 45° a 60°: 200 · 0,4 · sin 60° = 69 N·m stačí, 200 · 0,4 · sin 45° = 57 N·m už ne. Největší moment (80 N·m) dá tah kolmo, α = 90°.",
        "Граница между 45° и 60°: 200 · 0,4 · sin 60° = 69 Н·м хватает, 200 · 0,4 · sin 45° = 57 Н·м — уже нет. Наибольший момент (80 Н·м) даёт тяга перпендикулярно, α = 90°.",
    ))
    LearnJump(108, 3, L("📖 Jak spočítat rameno šikmé síly? → Trigonometrie: rameno = r · sin α", "📖 Как найти плечо наклонной силы? → Тригонометрия: плечо = r · sin α"))
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
 * U pantu je rameno 0: nehne se nic.
 */
@Composable
private fun CardScope.Door() {
    val tt = tr()
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
            fun far(i: Int): Double {
                val (sx, sy) = turn(hx + spots[i] * len, hy + 30, hx, hy, -theta)
                return pt.dist(sx, sy)
            }
            val i = spots.indices.minByOrNull { far(it) } ?: return@Stage
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
            } else if (!running) {
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
        "Samozavírací dveře shora: v pantu je pružina, která se brání tím víc, čím víc dveře otevřeš (její moment je k·θ). Tlačíš vždy stejnou silou 20 N – dveře se otevřou tak daleko, až pružina vyrovná moment tvé ruky. Pustíš – pružina je zase zavře.",
        "Самозакрывающаяся дверь сверху: в петле пружина, которая сопротивляется тем сильнее, чем шире открыта дверь (её момент k·θ). Толкаешь всегда с одной силой 20 Н — дверь откроется настолько, пока пружина не уравновесит момент твоей руки. Отпустишь — пружина её закроет.",
    ))
    Quiz(
        question = L("Předpověz: kde stejně velká síla otevře dveře **nejvíc**?", "Предскажи: где одинаковая сила откроет дверь **шире всего**?"),
        options = listOf(
            Opt(L("u pantu", "у петли"), why = L("U pantu je síla nejblíž ose otáčení. Jaké tam má rameno?", "У петли сила ближе всего к оси вращения. Какое у неё там плечо?")),
            Opt(L("uprostřed", "посередине"), why = L("Je uprostřed rameno největší? Kde je bod nejdál od pantu?", "Разве посередине плечо наибольшее? Где точка дальше всего от петли?")),
            Opt(L("u kliky", "у ручки"), correct = true),
            Opt(L("všude stejně – síla je stejná", "везде одинаково — сила ведь та же"), why = L("Otáčení nezávisí jen na síle. Na čem ještě? Vzpomeň na klíč a matici.", "Поворот зависит не только от силы. От чего ещё? Вспомни ключ и гайку.")),
        ),
        explain = L(
            "U kliky je rameno nejdelší, a tak i moment F · r – pružina se musí natočit nejvíc, než ho vyrovná.",
            "У ручки плечо самое длинное, а значит, и момент F · r — пружине нужно закрутиться сильнее всего, чтобы его уравновесить.",
        ),
        columns = 2,
    )
    Todo(
        L("Zatlač přímo **u pantu** (čárkovaný kroužek nebo tlačítko)", "Толкни прямо **у петли** (пунктирный кружок или кнопка)") to (0 in tried),
        L("Zatlač **uprostřed** dveří", "Толкни **посередине** двери") to (1 in tried),
        L("Zatlač **u kliky**", "Толкни **у ручки**") to (2 in tried),
    )
    Chips(listOf(L("u pantu", "у петли"), L("uprostřed", "посередине"), L("u kliky", "у ручки")), if (running) pushed else null) { push(it) }
    pushed?.let { i ->
        val r = spots[i] * doorM
        Readout("[[M]] = 20 · ${num(r)} = ${num(f * r, 1)} N·m → θ = ${num(f * r / k, 0)}°")
    }
    when (if (running) null else pushed) {
        0 -> Note(Tone.INFO, L(
            "Nic! Nositelka síly prochází pantem (osou otáčení) – rameno 0, moment 0. Takhle dveře neotevřeš, ať tlačíš jakkoli silně.",
            "Ничего! Линия действия силы проходит через петлю (ось вращения) — плечо 0, момент 0. Так дверь не открыть, как сильно ни толкай.",
        ))
        1 -> Note(Tone.INFO, L(
            "Uprostřed je rameno asi poloviční než u kliky – moment i úhel otevření jsou taky zhruba poloviční.",
            "Посередине плечо примерно вдвое короче, чем у ручки, — момент и угол открытия тоже примерно вдвое меньше.",
        ))
        2 -> Note(Tone.OK, L(
            "Nejdelší rameno = největší moment = dveře nejvíc otevřené. Proto je klika na kraji dveří, co nejdál od pantu.",
            "Самое длинное плечо = самый большой момент = дверь открыта шире всего. Поэтому ручка на краю двери, как можно дальше от петли.",
        ))
        else -> {}
    }
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
    var k by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableStateOf(false) }
    var lastOk by remember { mutableStateOf<Int?>(null) }
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
        lastOk?.let { i ->
            val q = forces[i]
            if (q.sign != 0) moment(ox, oy, 34.0, q.sign > 0, c.accent, 2.0)
            else text(ox, oy + 36, "M = 0", c.accent, 14.0, bold = true)
        }
        if (k in forces.indices && wrong) {
            val q = forces[k]
            if (q.sign != 0) moment(ox, oy, 34.0, q.sign > 0, c.derived, 1.6)
            else text(ox, oy + 36, "r = 0", c.derived, 14.0, bold = true)
        }
    }
    if (k < forces.size) CardText(L(
        "Deska je v bodě O přišpendlená a může se kolem něj otáčet. Na kterou stranu by ji otočila síla [[${forces[k].label}]]? Rozhoduje **nositelka** (čárkovaně): na které straně od O vede a kam míří. Moment proti směru hodinových ručiček ↺ počítáme +, po směru ↻ −.",
        "Пластина приколота в точке O и может вокруг неё вращаться. В какую сторону её повернула бы сила [[${forces[k].label}]]? Решает **линия действия** (пунктир): с какой стороны от O она проходит и куда направлена. Момент против часовой стрелки ↺ считаем со знаком +, по часовой ↻ — со знаком −.",
    ))
    Todo(L("Urči smysl otáčení postupně pro všech šest sil (${minOf(k, forces.size)}/${forces.size})", "Определи направление вращения по очереди для всех шести сил (${minOf(k, forces.size)}/${forces.size})") to (k >= forces.size))
    if (k < forces.size) {
        val q = forces[k]
        Chips(listOf(L("↺ proti směru hodinových ručiček (+)", "↺ против часовой стрелки (+)"), L("↻ po směru hodinových ručiček (−)", "↻ по часовой стрелке (−)"), L("neotočí (M = 0)", "не повернёт (M = 0)"))) { i ->
            val answer = when (i) { 0 -> 1; 1 -> -1; else -> 0 }
            if (answer == q.sign) {
                wrong = false
                lastOk = k
                k++
            } else wrong = true
        }
        if (wrong) Note(Tone.NUDGE, if (q.sign == 0) L(
            "Prodluž nositelku síly [[${q.label}]] – kudy vede? Když prochází přímo bodem O, je rameno nulové.",
            "Продли линию действия силы [[${q.label}]] — где она проходит? Если прямо через точку O, плечо равно нулю.",
        ) else L(
            "Představ si spojnici O s nositelkou jako ručičku připíchnutou v O: síla [[${q.label}]] tlačí na její konec. Kam se pootočí? Správný smysl teď ukazuje oblouk u bodu O.",
            "Представь отрезок от O до линии действия как стрелку часов, приколотую в O: сила [[${q.label}]] толкает её конец. Куда она повернётся? Верное направление теперь показывает дуга у точки O.",
        ))
    } else {
        Note(Tone.OK, L(
            "Výborně! ↺ bereme kladně, ↻ záporně – stejně jako v trenažéru. A síla, jejíž nositelka prochází bodem, k němu moment nemá (rameno 0) – to se nám u nosníků bude hodit.",
            "Отлично! ↺ берём со знаком плюс, ↻ — со знаком минус, как и в тренажёре. А у силы, линия действия которой проходит через точку, момента относительно неё нет (плечо 0) — это нам пригодится у балок.",
        ))
        SmallText(L(
            "Který smysl je kladný, je jen dohoda – důležité je držet se jí v celém výpočtu.",
            "Какое направление считать положительным — лишь договорённость; главное — держаться её во всём расчёте.",
        ))
    }
}

/**
 * Karta 4: dvojice sil – jen otáčí. Ruční kolo ventilu (průměr 0,5 m), každá ruka 20 N. Krátký záběr kolo roztočí,
 * ruce pustí a tření ve ventilu ho zastaví. Dvojici nahrazujeme momentem M = F · d (oblouk); k libovolnému bodu stejný.
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
    var asMoment by remember { mutableStateOf(false) }
    var point by remember { mutableStateOf<Int?>(null) }
    var tried by remember { mutableStateOf(setOf<Int>()) }
    var turned by remember { mutableStateOf(false) }
    var replaced by remember { mutableStateOf(false) }
    val ps = listOf(-0.25, 0.25, 0.75) // poloha bodu od levé síly [m]
    val xl = cx - rad
    val xr = cx + rad
    val tt = tr()
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
    Stage(h = 230.0, description = L("Ruční kolo ventilu otáčené dvojicí sil", "Маховик вентиля, который поворачивает пара сил").t()) { c ->
        circle(cx, cy, rad, null, c.ink, 5.0)
        for (i in 0 until 3) {
            val a = (spin + i * 120 - 90) * PI / 180
            line(cx, cy, cx + rad * cos(a), cy + rad * sin(a), c.ink, 3.0)
        }
        circle(cx, cy, 10.0, c.subtle, c.ink, 1.5)
        val fa = if (asMoment) 0.18f else if (running && !pushing) 0.25f else 1f
        force(xl, cy - 62, xl, cy - 7, c.load, glow = pushing, alpha = fa)
        force(xr, cy + 62, xr, cy + 7, c.load, glow = pushing, alpha = fa)
        text(xl - 8, cy - 46, " = 20 N", c.load, 13.0, Anchor.END, sym = "F", alpha = fa)
        text(xr + 8, cy + 52, " = 20 N", c.load, 13.0, Anchor.START, sym = "F", alpha = fa)
        if (running && !pushing) text(12.0, 20.0, tt.pick(L("ruce pustily – tření ve ventilu kolo zastaví", "руки отпустили — трение в вентиле остановит колесо")), c.inkSoft, 12.0, Anchor.START)
        if (asMoment) {
            moment(cx, cy, 34.0, true, c.load, 2.6)
            text(cx, cy - rad - 12, " = 10 N·m", c.load, 15.0, sym = "M", bold = true)
        }
        val yd = cy + 105
        val pt = point
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
        "Ruce na kole ventilu: dvě stejně velké síly (20 N) opačného smyslu na rovnoběžných nositelkách 0,5 m od sebe. Taková **dvojice sil** jen otáčí, nikam neposouvá (síly se v součtu vyruší) – nahrazujeme ji momentem [[M]] a kreslíme obloukem.",
        "Руки на маховике вентиля: две равные силы (20 Н) противоположного направления на параллельных линиях действия в 0,5 м друг от друга. Такая **пара сил** только поворачивает и никуда не сдвигает (в сумме силы взаимно уничтожаются) — её заменяем моментом [[M]] и рисуем дугой.",
    ))
    Todo(
        L("Klepni na „Zaber za kolo“ – dvojice kolo roztočí", "Нажми «Повернуть маховик» — пара сил его раскрутит") to turned,
        L("Klepni na „Nahraď momentem M“", "Нажми «Заменить моментом M»") to replaced,
        L("Klepni postupně na „bod vlevo“, „střed“ i „bod vpravo“ a porovnej moment dvojice k bodu P (${tried.size}/3)", "Нажми по очереди «точка слева», «центр» и «точка справа» и сравни момент пары относительно точки P (${tried.size}/3)") to (tried.size >= 3),
    )
    Chips(listOf(L("Zaber za kolo", "Повернуть маховик"))) { turn() }
    Chips(listOf(if (asMoment) L("Ukaž zase síly", "Снова показать силы") else L("Nahraď momentem M", "Заменить моментом M"))) {
        asMoment = !asMoment
        replaced = true
    }
    SmallText(L("Bod P, ke kterému moment dvojice počítáme:", "Точка P, относительно которой считаем момент пары:"))
    Chips(listOf(L("bod vlevo", "точка слева"), L("střed", "центр"), L("bod vpravo", "точка справа")), point) { i ->
        point = i
        tried = tried + i
    }
    point?.let { i ->
        val p = ps[i]
        fun part(v: Double, arm: Double, first: Boolean): String {
            val glyph = if (v >= 0) "↺" else "↻"
            val sign = if (v < 0) (if (first) "−" else " − ") else if (first) "" else " + "
            return "${sign}20·${num(arm)} $glyph"
        }
        val left = 20 * p
        val right = 20 * (0.5 - p)
        Readout("[[M]] = ${part(left, abs(p), true)}${part(right, abs(0.5 - p), false)} = 10 N·m", ok = true)
    }
    if (tried.size >= 3) {
        Note(Tone.OK, L(
            "Ke každému bodu vyjde stejně: [[M]] = [[F]] · [[d]] = 20 · 0,5 = 10 N·m, kde [[d]] je vzdálenost nositelek obou sil.",
            "Относительно любой точки выходит одно и то же: [[M]] = [[F]] · [[d]] = 20 · 0,5 = 10 Н·м, где [[d]] — расстояние между линиями действия сил.",
        ))
        SmallText(L(
            "Proto moment dvojice přičítáš rovnou jako [[M]], bez ramene – ať počítáš ke kterémukoli bodu. Do součtu sil dvojice nepřispívá: její síly se navzájem vyruší.",
            "Поэтому момент пары прибавляешь сразу как [[M]], без плеча, — относительно какой бы точки ты ни считал. В сумму сил пара ничего не добавляет: её силы уравновешивают друг друга.",
        ))
    }
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
    Quiz(
        question = L("Která síla má k bodu A **největší moment** (co do velikosti, bez ohledu na znaménko)?", "У какой силы **самый большой по величине** момент относительно точки A (без учёта знака)?"),
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
