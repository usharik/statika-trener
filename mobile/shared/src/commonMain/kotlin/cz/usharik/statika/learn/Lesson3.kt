package cz.usharik.statika.learn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Prim
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.Anim
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import cz.usharik.statika.ui.angleMark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Lekce 3 – Jedna šipka = dvě. Šikmou sílu nahradíme vodorovnou a svislou složkou:
 * F_x = F · cos α (strana u úhlu), F_y = F · sin α. Úhel α měříme od osy nosníku jako v trenažéru.
 * Všude stejná konvence: obě složky začínají tam, kde začíná šipka síly (strany obdélníku, síla je úhlopříčka).
 */

private const val F_KN = 10.0
private const val SCALE = 14.0 // jednotek na 1 kN

/** Úhel trojúhelníku 3-4-5 (tg α = 3/4), aby složky vyšly přesně 8 a 6 kN. */
private val ALPHA_345 = atan2(3.0, 4.0) * 180 / PI

/**
 * Šikmá síla shora na nosník s hrotem v (tx, ty), úhel [alpha] od nosníku.
 * Složky začínají u konce šipky a jsou o kousek odsunuté, aby je síla nikdy nepřekryla (ani při 0° a 90°).
 * [atTip] = složky působí ve stejném bodě jako síla (hroty v působišti) – tak je kreslíme jako zatížení nosníku.
 * [fa] = průhlednost celé síly, [ca] = průhlednost složek.
 */
internal fun PrimBuilder.inclined(tx: Double, ty: Double, alpha: Double, c: StatikaColors, fa: Float, ca: Float, labels: Boolean = true, atTip: Boolean = false) {
    val a = alpha * PI / 180
    val ux = cos(a)
    val uy = sin(a)
    val len = F_KN * SCALE
    val sx = tx - len * ux
    val sy = ty - len * uy
    val off = 8.0
    if (ca > 0f && atTip) {
        // složky působí ve stejném bodě jako síla: obě končí hrotem v působišti (tx, ty)
        line(sx, sy, tx, sy, c.derived, 1.0, dash = true, alpha = ca * 0.6f)
        line(sx, sy, sx, ty, c.derived, 1.0, dash = true, alpha = ca * 0.6f)
        if (ux > 0.02) force(sx, ty - 4, tx, ty - 4, c.derived, 2.4, alpha = ca)
        if (uy > 0.02) force(tx, sy, tx, ty, c.derived, 2.4, alpha = ca)
        if (labels) {
            // pod nosníkem – nad ním je oblouk úhlu
            if (ux > 0.02) text((sx + tx) / 2, ty + 28, " = ${num(F_KN * ux)} kN", c.derived, 13.0, sym = "F_x", alpha = ca)
            if (uy > 0.02) text(tx + 8, (sy + ty) / 2 + 5, " = ${num(F_KN * uy)} kN", c.derived, 13.0, Anchor.START, sym = "F_y", alpha = ca)
        }
    } else if (ca > 0f) {
        // doplňkový obdélník čárkovaně
        line(sx, ty, tx, ty, c.derived, 1.0, dash = true, alpha = ca * 0.6f)
        line(tx, sy, tx, ty, c.derived, 1.0, dash = true, alpha = ca * 0.6f)
        // F_x nad horní stranou obdélníku, F_y vlevo od levé strany
        if (ux > 0.02) force(sx, sy - off, tx, sy - off, c.derived, 2.4, alpha = ca)
        if (uy > 0.02) force(sx - off, sy, sx - off, ty, c.derived, 2.4, alpha = ca)
        if (labels) {
            if (ux > 0.02) text((sx + tx) / 2, sy - off - 10, " = ${num(F_KN * ux)} kN", c.derived, 13.0, sym = "F_x", alpha = ca)
            if (uy > 0.02) text(sx - off - 8, (sy + ty) / 2 + 5, " = ${num(F_KN * uy)} kN", c.derived, 13.0, Anchor.END, sym = "F_y", alpha = ca)
        }
    }
    if (fa > 0f) {
        force(sx, sy, tx, ty, c.load, 3.0, alpha = fa)
        // popisek síly vpravo od středu šipky, mimo ručku u jejího konce
        // (u skoro vodorovné síly by popisek ležel na nosníku – tam ho dáme vpravo nad hrot)
        if (labels && atTip) {
            // u konce šipky – u hrotu jsou popisky složek
            text(sx + 8, sy - 12, " = ${num(F_KN)} kN", c.load, 14.0, Anchor.START, sym = "F", alpha = fa)
        } else if (labels) {
            if (alpha < 20.0) text(tx + 8, ty - 8, " = ${num(F_KN)} kN", c.load, 14.0, Anchor.START, sym = "F", alpha = fa)
            else text((sx + tx) / 2 + 12 * uy + 8, (sy + ty) / 2 - 12 * ux + 18, " = ${num(F_KN)} kN", c.load, 14.0, Anchor.START, sym = "F", alpha = fa)
        }
        if (alpha in 1.0..89.0) angleMark(this, tx to ty, ux, uy, alpha.roundToInt(), c, Anim.None)
    }
}

/** Karta 3: posuvník úhlu – složky se mění živě. */
@Composable
private fun CardScope.AngleComponents() {
    val t = tr()
    var alpha by remember { mutableStateOf(60.0) }
    var dragging by remember { mutableStateOf(false) }
    var half by remember { mutableStateOf(false) }
    var equal by remember { mutableStateOf(false) }
    // cíl se počítá, až když na něm úhel chvíli zůstane – ne když posuvník přes něj jen přejede
    LaunchedEffect(alpha) {
        if (alpha == 30.0 || alpha == 45.0) {
            delay(500)
            if (alpha == 30.0) half = true
            if (alpha == 45.0) equal = true
        }
    }
    val tx = 310.0
    val ty = 196.0
    fun setAlpha(v: Double) {
        alpha = ((v / 5).roundToInt() * 5.0).coerceIn(0.0, 90.0)
    }
    Stage(
        h = 240.0,
        description = t.pick(L("Šikmá síla a její složky", "Наклонная сила и её составляющие")),
        onDrag = { p, start ->
            dragging = true
            // úhel podle polohy prstu vůči působišti (konec šipky je vlevo nahoře)
            if (!start || p.y < ty) setAlpha(atan2(ty - p.y, tx - p.x) * 180 / PI)
        },
        onDragEnd = { dragging = false },
    ) { c ->
        beam(20.0, 380.0, ty + 5, c)
        inclined(tx, ty, alpha, c, 1f, 1f, atTip = true)
        val a = alpha * PI / 180
        // ručka kousek za koncem šipky, aby nezakryla popisek ani šipku
        val len = F_KN * SCALE + 16
        handle(tx - len * cos(a), ty - len * sin(a), c, dragging)
    }
    CardText(L(
        "Složky šikmé síly spočítáš z úhlu α mezi silou a nosníkem. Síla má pořád 10 kN – měň jen α (tahem za kroužek nebo posuvníkem) a čti, jak se mezi složky „rozdělí“.",
        "Составляющие наклонной силы найдёшь по углу α между силой и балкой. Сила всё время 10 кН — меняй только α (кружком или ползунком) и смотри, как она «делится» между составляющими.",
    ))
    Quiz(
        question = L("Předpověz: síla leží přesně podél nosníku (α = 0°). Která složka je nulová?", "Предскажи: сила лежит точно вдоль балки (α = 0°). Какая составляющая равна нулю?"),
        options = listOf(
            Opt(L("[[F_y]] (svislá)", "[[F_y]] (вертикальная)"), correct = true),
            Opt(L("[[F_x]] (vodorovná)", "[[F_x]] (горизонтальная)"), why = L("Síla podél nosníku míří vodorovně. Zbude jí něco, co tlačí dolů? Nastav α = 0° a podívej se.", "Сила вдоль балки направлена горизонтально. Останется ли у неё что-то, что давит вниз? Поставь α = 0° и посмотри.")),
            Opt(L("žádná – obě jsou 5 kN", "никакая — обе по 5 кН"), why = L("Napůl se síla dělí jen při jednom úhlu, a ne při 0°. Zkus α = 0° na posuvníku.", "Пополам сила делится лишь при одном угле, и не при 0°. Попробуй α = 0° на ползунке.")),
        ),
        explain = L(
            "Při α = 0° je celá síla vodorovná: [[F_x]] = 10 kN, [[F_y]] = 0. Při α = 90° naopak [[F_x]] = 0.",
            "При α = 0° вся сила горизонтальна: [[F_x]] = 10 кН, [[F_y]] = 0. При α = 90°, наоборот, [[F_x]] = 0.",
        ),
        columns = 3,
    )
    Todo(
        L("Najdi úhel, při kterém je svislá složka [[F_y]] = 5 kN – přesně polovina síly", "Найди угол, при котором вертикальная составляющая [[F_y]] = 5 кН — ровно половина силы") to half,
        L("Najdi úhel, při kterém jsou obě složky stejně velké, [[F_x]] = [[F_y]]", "Найди угол, при котором обе составляющие равны, [[F_x]] = [[F_y]]") to equal,
    )
    ValueSlider("α = ${alpha.roundToInt()}°", alpha.toFloat(), 0f..90f, steps = 17) { setAlpha(it.toDouble()) }
    val a = alpha * PI / 180
    Readout("[[F_x]] = 10 · cos ${alpha.roundToInt()}° = ${num(F_KN * cos(a))} kN")
    Readout("[[F_y]] = 10 · sin ${alpha.roundToInt()}° = ${num(F_KN * sin(a))} kN")
    if (alpha == 0.0) SmallText(L(
        "Při α = 0° je celá síla vodorovná: [[F_x]] = [[F]], svislá složka [[F_y]] = 0.",
        "При α = 0° вся сила горизонтальна: [[F_x]] = [[F]], вертикальная составляющая [[F_y]] = 0.",
    ))
    if (alpha == 90.0) SmallText(L(
        "Při α = 90° je celá síla svislá: [[F_y]] = [[F]], vodorovná složka [[F_x]] = 0.",
        "При α = 90° вся сила вертикальна: [[F_y]] = [[F]], горизонтальная составляющая [[F_x]] = 0.",
    ))
    if (alpha == 45.0) SmallText(L(
        "Při 45° jsou složky stejné: 7,07 + 7,07 = 14,1 kN – víc než 10! Složky se nesčítají jako čísla, ale jako šipky (7,07² + 7,07² = 10²).",
        "При 45° составляющие равны: 7,07 + 7,07 = 14,1 кН — больше 10! Составляющие складываются не как числа, а как стрелки (7,07² + 7,07² = 10²).",
    ))
    SmallText(L(
        "Pomůcka: složka **u úhlu** = [[F]] · cos α, složka **naproti** úhlu = [[F]] · sin α. Tady α měříme od vodorovné, proto je u úhlu [[F_x]].",
        "Подсказка: составляющая, **прилежащая к углу**, = [[F]] · cos α, **противолежащая** = [[F]] · sin α. Здесь α отсчитываем от горизонтали, поэтому к углу прилежит [[F_x]].",
    ))
    // nepovinný odkaz – odkud se sin a cos berou (kurz trigonometrie); kartu neodemyká ani nezamyká
    LearnJump(102, 0, L("📖 Proč zrovna cos a sin? → Trigonometrie: Sinus, kosinus, tangens", "📖 Почему именно cos и sin? → Тригонометрия: синус, косинус, тангенс"))
}

/** Sáně shora: korba, dvě lyžiny přečnívající dopředu (doprava). */
private fun PrimBuilder.sled(cx: Double, cy: Double, c: StatikaColors, alpha: Float = 1f) {
    line(cx - 24, cy - 16, cx + 30, cy - 16, c.ink, 2.0, alpha = alpha)
    line(cx - 24, cy + 16, cx + 30, cy + 16, c.ink, 2.0, alpha = alpha)
    rect(cx - 20, cy - 13, 40.0, 26.0, c.beam, c.ink, 1.6, alpha)
    circle(cx, cy, 3.0, c.ink, alpha = alpha)
}

private const val SLED_KG = 250.0
private const val SLED_M = 20.0 // jednotek na 1 m
private const val ROPE_K = 0.25 // jednotek na 1 N

/** Úhel 0…360° (osa y dolů) zaokrouhlený na 5°; blízko přesné rovnováhy se k ní „přichytí“. */
private fun snapAngle(deg: Double, exact: Double): Double {
    var d = deg % 360
    if (d < 0) d += 360
    var diff = abs(d - exact)
    if (diff > 180) diff = 360 - diff
    return if (diff < 3) exact else ((d / 5).roundToInt() * 5.0) % 360
}

/**
 * Karta 0: dvě lana. Sáně na ledě (shora) táhnou dvě lana – 400 N doprava, 300 N nahoru. Žák si tipne, kam se
 * rozjedou, pustí je: zrychlují po úhlopříčce rovnoběžníku, výslednice má 500 N (ne 700). Pak přidá třetí lano tak,
 * aby sáně zůstaly stát – síla stejně velká jako výslednice, opačného smyslu.
 */
@Composable
private fun CardScope.TwoRopes() {
    val t = tr()
    val sx0 = 170.0
    val sy0 = 140.0
    // síly v N, osa y dolů
    val f1x = 400.0
    val f1y = 0.0
    val f2x = 0.0
    val f2y = -300.0
    val exact = atan2(-(f1y + f2y), -(f1x + f2x)) * 180 / PI // směr síly, která výslednici vyruší (≈ 143°)
    var pred by remember { mutableStateOf<Int?>(null) }
    var ran by remember { mutableStateOf(false) }
    var still by remember { mutableStateOf(false) }
    var f3 by remember { mutableDoubleStateOf(300.0) }
    var a3 by remember { mutableDoubleStateOf(180.0) }
    var dragging by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var px by remember { mutableDoubleStateOf(sx0) }
    var py by remember { mutableDoubleStateOf(sy0) }
    var alpha by remember { mutableStateOf(1f) }
    var stoodStill by remember { mutableStateOf(false) }
    val trail = remember { mutableStateListOf<Pair<Double, Double>>() }
    val scope = rememberCoroutineScope()
    val use3 = ran
    fun net(): Pair<Double, Double> {
        var x = f1x + f2x
        var y = f1y + f2y
        if (use3) {
            x += f3 * cos(a3 * PI / 180)
            y += f3 * sin(a3 * PI / 180)
        }
        return x to y
    }
    fun release() {
        if (running || pred == null) return
        running = true
        stoodStill = false
        trail.clear()
        val (nx, ny) = net()
        val withThird = use3
        scope.launch {
            val ax = nx / SLED_KG
            val ay = ny / SLED_KG
            var time = 0.0
            var last = 0L
            var nextGhost = 0.0
            px = sx0
            py = sy0
            alpha = 1f
            // pozorujeme nejvýš 3,5 s nebo do kraje kluziště
            while (time < 3.5 && px in 35.0..365.0 && py in 30.0..222.0) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0.0 else (now - last) / 1e9
                    last = now
                    time += dt
                    px = sx0 + 0.5 * ax * time * time * SLED_M
                    py = sy0 + 0.5 * ay * time * time * SLED_M
                    if (time >= nextGhost) {
                        trail += px to py
                        nextGhost += 0.4
                    }
                }
            }
            if (hypot(nx, ny) < 1.0) {
                stoodStill = true
                if (withThird) still = true
            }
            ran = true
            delay(500)
            // pokus skončil: sáně zmizí a na startu se objeví nové (nikdy necouvají zpátky)
            if (px != sx0 || py != sy0) {
                for (i in 1..8) {
                    alpha = 1f - i / 8f
                    delay(30)
                }
                px = sx0
                py = sy0
                for (i in 1..8) {
                    alpha = i / 8f
                    delay(30)
                }
            }
            running = false
        }
    }
    val tip3x = sx0 + f3 * ROPE_K * cos(a3 * PI / 180)
    val tip3y = sy0 + f3 * ROPE_K * sin(a3 * PI / 180)
    Stage(
        h = 240.0,
        description = t.pick(L("Sáně na ledě shora, táhnou je dvě lana", "Санки на льду сверху, их тянут две верёвки")),
        onDrag = { p, start ->
            if (start) dragging = use3 && !running && p.dist(tip3x, tip3y) < 40
            if (dragging && p.dist(sx0, sy0) > 20) {
                a3 = snapAngle(atan2(p.y - sy0, p.x - sx0) * 180 / PI, exact)
                f3 = ((hypot(p.x - sx0, p.y - sy0) / ROPE_K / 50).roundToInt() * 50.0).coerceIn(50.0, 700.0)
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        rect(10.0, 10.0, 380.0, 220.0, c.react.copy(alpha = 0.05f), c.inkSoft, 1.2)
        // stopa posledního pokusu
        for ((gx, gy) in trail.dropLast(1)) circle(gx, gy, 3.0, c.inkSoft, alpha = 0.6f)
        val ox = px
        val oy = py
        val showForces = alpha > 0.5f
        if (ran) {
            // rovnoběžník: výslednice R je jeho úhlopříčka
            val rx = ox + (f1x + f2x) * ROPE_K
            val ry = oy + (f1y + f2y) * ROPE_K
            line(ox + f1x * ROPE_K, oy + f1y * ROPE_K, rx, ry, c.derived, 1.0, dash = true, alpha = 0.7f)
            line(ox + f2x * ROPE_K, oy + f2y * ROPE_K, rx, ry, c.derived, 1.0, dash = true, alpha = 0.7f)
            force(ox, oy, rx, ry, c.derived, 2.6, alpha = alpha)
            text(rx + 6, ry - 6, " = 500 N", c.derived, 13.0, Anchor.START, sym = "R", alpha = alpha)
        }
        if (showForces) {
            force(ox + 24, oy, ox + f1x * ROPE_K, oy + f1y * ROPE_K, c.load, 3.0, alpha = alpha)
            text(ox + f1x * ROPE_K - 4, oy + 22, " = 400 N", c.load, 13.0, Anchor.END, sym = "F_1", alpha = alpha)
            force(ox, oy - 14, ox + f2x * ROPE_K, oy + f2y * ROPE_K, c.load, 3.0, alpha = alpha)
            text(ox - 8, oy + f2y * ROPE_K + 14, " = 300 N", c.load, 13.0, Anchor.END, sym = "F_2", alpha = alpha)
            if (use3) {
                val tx = ox + f3 * ROPE_K * cos(a3 * PI / 180)
                val ty = oy + f3 * ROPE_K * sin(a3 * PI / 180)
                force(ox, oy, tx, ty, c.react, 3.0, alpha = alpha)
                text(tx + (if (tx < ox) -16.0 else 16.0), ty + 5, " = ${num(f3, 0)} N", c.react, 13.0, if (tx < ox) Anchor.END else Anchor.START, sym = "F_3", alpha = alpha)
                if (!running) handle(tx, ty, c, dragging)
            }
        }
        sled(ox, oy, c, alpha)
        if (running && stoodStill.not() && trail.size > 1) text(390.0 - 8, 226.0, t.pick(L("tečky: poloha každých 0,4 s", "точки: положение каждые 0,4 с")), c.inkSoft, 11.0, Anchor.END)
        if (stoodStill) text(200.0, 30.0, t.pick(L("Σ F = 0 – sáně stojí", "Σ F = 0 — санки стоят")), c.accent, 15.0, bold = true)
    }
    CardText(L(
        "Sáně (250 kg) na hladkém ledě, pohled shora. Jedno lano táhne silou 400 N doprava, druhé 300 N nahoru. Kam se sáně rozjedou? Nejdřív si tipni, pak je pusť.",
        "Санки (250 кг) на гладком льду, вид сверху. Одна верёвка тянет с силой 400 Н вправо, другая — 300 Н вверх. Куда поедут санки? Сначала предскажи, потом отпусти их.",
    ))
    Todo(
        L("Tipni si směr a pusť sáně", "Предскажи направление и отпусти санки") to ran,
        L("Přidej třetí lano [[F_3]] (modré, táhni za kroužek) tak, aby sáně po puštění **zůstaly stát**", "Добавь третью верёвку [[F_3]] (синюю, тяни за кружок) так, чтобы санки после отпускания **остались стоять**") to still,
    )
    if (!ran) {
        SmallText(L("Můj tip – sáně pojedou:", "Мой прогноз — санки поедут:"))
        Chips(
            listOf(
                L("→ doprava, za silnějším lanem", "→ вправо, за более сильной верёвкой"),
                L("↗ šikmo mezi lany", "↗ наискосок, между верёвками"),
                L("nikam – lana se přetahují", "никуда — верёвки перетягивают друг друга"),
            ),
            pred,
        ) { pred = it }
    }
    ActionButton(L("Pusť sáně", "Отпустить санки"), enabled = !running && pred != null) { release() }
    if (ran) {
        when (pred) {
            1 -> Note(Tone.OK, L(
                "Správně: šikmo, po úhlopříčce rovnoběžníku ze dvou lan – a blíž k silnějšímu lanu.",
                "Верно: наискосок, по диагонали параллелограмма из двух верёвок — и ближе к более сильной верёвке.",
            ))
            else -> Note(Tone.INFO, L(
                "Sáně jely šikmo – po úhlopříčce rovnoběžníku, který tvoří obě síly. Každé lano „přidá“ svůj kus pohybu.",
                "Санки поехали наискосок — по диагонали параллелограмма, который образуют обе силы. Каждая верёвка «добавляет» свою часть движения.",
            ))
        }
        Quiz(
            question = L("Jak velká je výsledná síla [[R]], která sáně rozjíždí?", "Чему равна равнодействующая [[R]], которая разгоняет санки?"),
            options = listOf(
                Opt(L("700 N", "700 Н"), why = L("400 + 300 by platilo, kdyby obě lana táhla stejným směrem. Míří ale jinam – změř fialovou úhlopříčku.", "400 + 300 было бы, если бы обе верёвки тянули в одну сторону. Но они направлены по-разному — оцени фиолетовую диагональ.")),
                Opt(L("500 N", "500 Н"), correct = true),
                Opt(L("100 N", "100 Н"), why = L("Odečítat by se síly mohly, jen kdyby táhly proti sobě. Tady svírají pravý úhel.", "Вычитать силы можно, только если они тянут навстречу друг другу. Здесь они под прямым углом.")),
            ),
            explain = L(
                "Síly se sčítají jako **šipky**, ne jako čísla: šipky dáš za sebe (nebo doplníš rovnoběžník) a výslednice je úhlopříčka. Tady pravoúhlý trojúhelník 3-4-5: √(400² + 300²) = 500 N.",
                "Силы складываются как **стрелки**, а не как числа: стрелки ставишь одну за другой (или достраиваешь параллелограмм), равнодействующая — диагональ. Здесь прямоугольный треугольник 3-4-5: √(400² + 300²) = 500 Н.",
            ),
            columns = 3,
        )
        val (nx, ny) = net()
        val left = hypot(nx, ny)
        Readout(
            if (left < 1) "Σ[[F]] = 0 ✓" else L("Σ[[F]] = ${num(left, 0)} N – sáně se rozjedou", "Σ[[F]] = ${num(left, 0)} Н — санки поедут").t(),
            ok = left < 1,
        )
        Chips(listOf(L("↺ 5°", "↺ 5°"), L("↻ 5°", "↻ 5°"), L("− 50 N", "− 50 Н"), L("+ 50 N", "+ 50 Н"))) { i ->
            if (running) return@Chips
            when (i) {
                0 -> a3 = snapAngle(a3 - 5, exact)
                1 -> a3 = snapAngle(a3 + 5, exact)
                2 -> f3 = (f3 - 50).coerceAtLeast(50.0)
                else -> f3 = (f3 + 50).coerceAtMost(700.0)
            }
        }
        if (still) Note(Tone.OK, L(
            "Třetí lano je stejně velké jako výslednice (500 N), na stejné přímce, ale opačného smyslu. Tři síly se vyrušily: Σ[[F]] = 0 – přesně tohle budeme hledat u nosníků.",
            "Третья верёвка равна равнодействующей (500 Н), лежит на той же прямой, но направлена в обратную сторону. Три силы уравновесились: Σ[[F]] = 0 — именно это мы будем искать у балок.",
        ))
    }
}

/** Bedna na vzduchovém polštáři: 0 = F, 1 = F_x + F_y, 2 = jen F_x, 3 = F + F_x + F_y (síly v kN, osa y dolů). */
private val TRIALS = listOf(8.0 to -6.0, 8.0 to -6.0, 8.0 to 0.0, 16.0 to -12.0)

/**
 * Karta 1: složky nahradí sílu. Bedna 2 t na vzduchovém polštáři (pohled shora) dostane krátké strčení 0,5 s a pak klouže.
 * Síla F = 10 kN šikmo, nebo obě složky F_x = 8 kN a F_y = 6 kN najednou – bedna dojede na **stejné** místo.
 * Jen F_x – jinam. F i složky dohromady – dvakrát dál (síla započtená dvakrát).
 */
@Composable
private fun CardScope.ReplaceToggle() {
    val t = tr()
    val x0 = 60.0
    val y0 = 195.0
    val perM = 30.0
    val k = 7.0 // jednotek šipky na 1 kN
    val mass = 2.0 // t
    val tau = 0.5 // s strčení
    val total = 2.0 // s pozorování
    var mode by remember { mutableStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var time by remember { mutableDoubleStateOf(0.0) }
    var alpha by remember { mutableStateOf(1f) }
    val ends = remember { mutableStateMapOf<Int, Pair<Double, Double>>() }
    val scope = rememberCoroutineScope()
    fun posAt(m: Int, tt: Double): Pair<Double, Double> {
        val (fx, fy) = TRIALS[m]
        val s = if (tt < tau) 0.5 * tt * tt else 0.5 * tau * tau + tau * (tt - tau) // m na 1 m/s²
        return (x0 + fx / mass * s * perM) to (y0 + fy / mass * s * perM)
    }
    fun run(m: Int) {
        if (running) return
        mode = m
        running = true
        scope.launch {
            time = 0.0
            alpha = 1f
            var last = 0L
            while (time < total) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0.0 else (now - last) / 1e9
                    last = now
                    time = (time + dt).coerceAtMost(total)
                }
            }
            ends[m] = posAt(m, total)
            delay(700)
            for (i in 1..8) {
                alpha = 1f - i / 8f
                delay(30)
            }
            time = 0.0
            for (i in 1..8) {
                alpha = i / 8f
                delay(30)
            }
            running = false
        }
    }
    Stage(h = 230.0, description = t.pick(L("Bedna na vzduchovém polštáři, strčená silou nebo jejími složkami", "Ящик на воздушной подушке, толкаемый силой или её составляющими"))) { c ->
        rect(10.0, 10.0, 380.0, 210.0, c.react.copy(alpha = 0.04f), c.inkSoft, 1.0)
        // dojezdy předchozích pokusů
        for (e in ends.values) {
            val (ex, ey) = e
            line(x0, y0, ex, ey, c.inkSoft, 1.0, dash = true, alpha = 0.6f)
            line(ex - 6, ey - 6, ex + 6, ey + 6, c.ink, 1.6)
            line(ex - 6, ey + 6, ex + 6, ey - 6, c.ink, 1.6)
        }
        // popisky křížků: F a F_x + F_y padnou na stejné místo – jeden společný popisek
        val e0 = ends[0]
        val e1 = ends[1]
        if (e0 != null || e1 != null) {
            val (ex, ey) = (e0 ?: e1)!!
            val lbl = if (e0 != null && e1 != null) t.pick(L("F i složky: stejné místo ✓", "F и составляющие: то же место ✓")) else if (e0 != null) t.pick(L("síla F", "сила F")) else t.pick(L("složky", "составляющие"))
            text(ex + 10, ey - 8, lbl, c.ink, 12.0, Anchor.START)
        }
        ends[2]?.let { (ex, ey) -> text(ex, ey + 22, t.pick(L(" sama", " одна")), c.ink, 12.0, sym = "F_x") }
        ends[3]?.let { (ex, ey) -> text(ex - 10, ey + 4, t.pick(L("F + složky", "F + составляющие")), c.ink, 12.0, Anchor.END) }
        val (bx, by) = if (running) posAt(mode, time) else x0 to y0
        rect(bx - 18, by - 14, 36.0, 28.0, c.beam, c.ink, 1.6, alpha)
        circle(bx, by, 2.5, c.ink, alpha = alpha)
        // síly působí v těžišti bedny (všechny v jednom bodě) jen během strčení
        val fa = if (!running) alpha else if (time < tau) 1f else (1f - ((time - tau) / 0.25).toFloat()).coerceIn(0f, 1f) * alpha
        if (fa > 0.02f) {
            val showF = mode == 0 || mode == 3
            val showC = mode == 1 || mode == 2 || mode == 3
            if (mode == 1 || mode == 3) {
                line(bx + 8 * k, by, bx + 8 * k, by - 6 * k, c.derived, 1.0, dash = true, alpha = fa * 0.6f)
                line(bx, by - 6 * k, bx + 8 * k, by - 6 * k, c.derived, 1.0, dash = true, alpha = fa * 0.6f)
            }
            if (showC) {
                force(bx, by, bx + 8 * k, by, c.derived, 2.6, glow = running, alpha = fa)
                text(bx + 4 * k, by + 31, " = 8 kN", c.derived, 12.0, sym = "F_x", alpha = fa)
                if (mode != 2) {
                    force(bx, by, bx, by - 6 * k, c.derived, 2.6, glow = running, alpha = fa)
                    text(bx - 6, by - 3 * k, " = 6 kN", c.derived, 12.0, Anchor.END, sym = "F_y", alpha = fa)
                }
            }
            if (showF) {
                force(bx, by, bx + 8 * k, by - 6 * k, c.load, 3.0, glow = running, alpha = fa)
                text(bx + 8 * k + 6, by - 6 * k + 4, " = 10 kN", c.load, 13.0, Anchor.START, sym = "F", alpha = fa)
            }
        }
        if (running && time > tau) text(380.0, 214.0, t.pick(L("síla skončila – bedna klouže sama", "сила кончилась — ящик скользит сам")), c.inkSoft, 11.0, Anchor.END)
        text(20.0, 28.0, t.pick(L("pozorujeme vždy 2 s", "наблюдаем всегда 2 с")), c.inkSoft, 11.0, Anchor.START)
    }
    CardText(L(
        "Šikmou sílu smíme nahradit dvěma **složkami** – vodorovnou [[F_x]] a svislou [[F_y]], které působí **ve stejném bodě**. Ověř to pokusem: bedna (2 t) se vznáší na vzduchovém polštáři, krátce do ní strčíš a sleduješ, kam za 2 s dojede.",
        "Наклонную силу можно заменить двумя **составляющими** — горизонтальной [[F_x]] и вертикальной [[F_y]], приложенными **в той же точке**. Проверь опытом: ящик (2 т) парит на воздушной подушке, ты коротко толкаешь его и смотришь, куда он доедет за 2 с.",
    ))
    Todo(
        L("Strč silou [[F]] = 10 kN", "Толкни силой [[F]] = 10 кН") to (0 in ends),
        L("Strč oběma složkami [[F_x]] a [[F_y]] najednou – kam bedna dojede?", "Толкни обеими составляющими [[F_x]] и [[F_y]] сразу — куда доедет ящик?") to (1 in ends),
        L("Strč jen složkou [[F_x]]", "Толкни только составляющей [[F_x]]") to (2 in ends),
    )
    Chips(listOf(L("Strč: [[F]]", "Толкнуть: [[F]]"), L("Strč: [[F_x]] + [[F_y]]", "Толкнуть: [[F_x]] + [[F_y]]"), L("Strč: jen [[F_x]]", "Толкнуть: только [[F_x]]")), if (running && mode < 3) mode else null) { run(it) }
    if (0 in ends && 1 in ends) SmallText(L(
        "Stejné místo: složky dohromady dělají přesně totéž co celá síla. Jsou to odvěsny pravoúhlého trojúhelníku, síla je přepona: 8 a 6 kN z 10 kN.",
        "То же место: составляющие вместе действуют точно так же, как вся сила. Это катеты прямоугольного треугольника, сила — гипотенуза: 8 и 6 кН из 10 кН.",
    ))
    Quiz(
        question = L("Na výkresu zůstane síla [[F]] **i** její složky [[F_x]], [[F_y]]. Co se stane, když s nimi počítáš se všemi?", "На чертеже осталась сила [[F]] **и** её составляющие [[F_x]], [[F_y]]. Что будет, если считать со всеми тремя?"),
        options = listOf(
            Opt(L("nic – složky jsou jen jiný zápis", "ничего — составляющие лишь другая запись"), why = L("Zápis to je – ale jen když sílu **nahradí**. Kolikrát pak započítáš 10 kN?", "Это запись — но только если они силу **заменяют**. Сколько раз тогда учтёшь 10 кН?")),
            Opt(L("sílu započítáš dvakrát – účinek bude dvojnásobný", "сила будет учтена дважды — действие удвоится"), correct = true),
            Opt(L("složky se se silou vyruší", "составляющие уравновесят силу"), why = L("Vyrušit by se mohly jen síly opačného smyslu. Míří složky proti [[F]]?", "Уравновеситься могут только противоположно направленные силы. Разве составляющие направлены против [[F]]?")),
        ),
        explain = L(
            "Složky sílu **nahrazují**, nepřidávají se k ní. Když složky nakreslíš, původní šipku přeškrtni (nebo smaž) – jinak ji započítáš dvakrát.",
            "Составляющие силу **заменяют**, а не добавляются к ней. Нарисовал составляющие — зачеркни (или сотри) исходную стрелку, иначе учтёшь её дважды.",
        ),
    )
    Chips(listOf(L("Ověř: strč [[F]] + [[F_x]] + [[F_y]]", "Проверь: толкни [[F]] + [[F_x]] + [[F_y]]")), if (running && mode == 3) 0 else null) { run(3) }
}

/** Karta 2: síla na čtverečkovaném papíře – trojúhelník 3-4-5 (složky ze stejného bodu jako síla). */
@Composable
private fun CardScope.Grid345() {
    val t = tr()
    val solved = solvedHere
    val s = 36.0
    val ox = 110.0
    val oy = 200.0
    Stage(h = 230.0, description = t.pick(L("Síla na čtverečkované síti", "Сила на клетчатой сетке"))) { c ->
        for (i in 0..5) line(ox + i * s, oy - 4 * s, ox + i * s, oy + 0.5 * s, c.line, 1.0)
        for (j in -4..0) line(ox - 0.5 * s, oy + j * s, ox + 5 * s, oy + j * s, c.line, 1.0)
        if (solved) {
            // složky začínají tam co síla – strany obdélníku
            force(ox, oy + 6, ox + 4 * s, oy + 6, c.derived, 2.4)
            force(ox - 6, oy, ox - 6, oy - 3 * s, c.derived, 2.4)
            text(ox + 2 * s, oy + 26, " = 4", c.derived, 14.0, sym = "F_x")
            text(ox - 14, oy - 1.5 * s + 5, " = 3", c.derived, 14.0, Anchor.END, sym = "F_y")
        }
        force(ox, oy, ox + 4 * s, oy - 3 * s, c.load, 3.0)
        text(ox + 2 * s + 12, oy - 1.5 * s - 14, " = 5", c.load, 15.0, Anchor.END, sym = "F")
    }
    CardText(L(
        "Šipka síly je dlouhá 5 dílků (dílek = strana čtverečku). Spočítej čtverečky: o kolik dílků míří doprava a o kolik nahoru?",
        "Стрелка силы длиной 5 делений (деление = сторона клетки). Посчитай клетки: на сколько делений она уходит вправо и на сколько вверх?",
    ))
    Quiz(
        question = L("Jak velké jsou složky [[F_x]] (vodorovná) a [[F_y]] (svislá)?", "Чему равны составляющие [[F_x]] (горизонтальная) и [[F_y]] (вертикальная)?"),
        options = listOf(
            Opt(L("[[F_x]] = 4, [[F_y]] = 3", "[[F_x]] = 4, [[F_y]] = 3"), correct = true),
            Opt(L("[[F_x]] = 3, [[F_y]] = 4", "[[F_x]] = 3, [[F_y]] = 4"), why = L("[[F_x]] je **vodorovná** složka. Spočítej znovu: o kolik dílků míří šipka doprava?", "[[F_x]] — **горизонтальная** составляющая. Посчитай ещё раз: на сколько делений стрелка уходит вправо?")),
            Opt(L("[[F_x]] = 5, [[F_y]] = 5", "[[F_x]] = 5, [[F_y]] = 5"), why = L("Může být složka stejně dlouhá jako celá šikmá šipka? Spočítej čtverečky pod ní a vedle ní.", "Может ли составляющая быть такой же длины, как вся наклонная стрелка? Посчитай клетки под ней и сбоку.")),
        ),
        explain = L(
            "4 dílky doprava a 3 nahoru. Pozor: 4 + 3 ≠ 5 – složky se silou tvoří pravoúhlý trojúhelník, platí 4² + 3² = 5².",
            "4 деления вправо и 3 вверх. Внимание: 4 + 3 ≠ 5 — составляющие и сила образуют прямоугольный треугольник: 4² + 3² = 5².",
        ),
    )
    if (done) LearnJump(105, 0, L("📖 Proč 4² + 3² = 5²? → Trigonometrie: Pythagorova věta", "📖 Почему 4² + 3² = 5²? → Тригонометрия: теорема Пифагора"))
}

/** Karta 4: kvíz – F = 10 kN pod 30° (F_x?), pak tentýž úhel od svislice (F_y?). */
@Composable
private fun CardScope.CheckCos() {
    val t = tr()
    val solved = solvedHere
    Stage(h = 210.0, description = t.pick(L("Síla 10 kN pod úhlem 30° k nosníku", "Сила 10 кН под углом 30° к балке"))) { c ->
        beam(20.0, 380.0, 175.0, c)
        inclined(300.0, 170.0, 30.0, c, 1f, if (solved) 1f else 0f, atTip = true)
        // tentýž úhel měřený od svislice: 60°
        val tx = 300.0
        val ty = 170.0
        line(tx, ty, tx, ty - 120, c.inkSoft, 1.0, dash = true)
        prims += Prim.Arc(tx, ty, 50.0, -90.0, -60.0, c.inkSoft, 1.0)
        text(tx - 22, ty - 62, "60°", c.inkSoft, 12.0, Anchor.END)
    }
    Quiz(
        question = L("Síla [[F]] = 10 kN svírá s nosníkem 30° (cos 30° ≈ 0,866; sin 30° = 0,5). Jak velká je vodorovná složka [[F_x]]?", "Сила [[F]] = 10 кН составляет с балкой 30° (cos 30° ≈ 0,866; sin 30° = 0,5). Чему равна горизонтальная составляющая [[F_x]]?"),
        options = listOf(
            Opt(L("8,66 kN", "8,66 кН"), correct = true),
            Opt(L("5 kN", "5 кН"), why = L("To je 10 · sin 30° – složka **naproti** úhlu, tedy svislá [[F_y]]. Kterou funkcí spočítáš složku u úhlu?", "Это 10 · sin 30° — составляющая **напротив** угла, то есть вертикальная [[F_y]]. Какой функцией найти составляющую, прилежащую к углу?")),
            Opt(L("10 kN", "10 кН"), why = L("Síla je šikmá, takže vodorovně míří jen její část. Vyjde víc, nebo míň než 10 kN?", "Сила наклонная, поэтому горизонтально действует только её часть. Получится больше или меньше 10 кН?")),
            Opt(L("11,5 kN", "11,5 кН"), why = L("Složka nikdy není větší než celá síla. Zkus 10 · cos 30°.", "Составляющая никогда не больше всей силы. Попробуй 10 · cos 30°.")),
        ),
        explain = L(
            "[[F_x]] leží u úhlu, proto kosinus: [[F_x]] = 10 · cos 30° = 8,66 kN. Svislá [[F_y]] = 10 · sin 30° = 5 kN.",
            "[[F_x]] прилежит к углу, поэтому косинус: [[F_x]] = 10 · cos 30° = 8,66 кН. Вертикальная [[F_y]] = 10 · sin 30° = 5 кН.",
        ),
        columns = 2,
    )
    Quiz(
        question = L("Teď je úhel zadaný od **svislice**: síla s ní svírá 60° (cos 60° = 0,5; sin 60° ≈ 0,866). Jak velká je svislá složka [[F_y]]?", "Теперь угол задан от **вертикали**: сила составляет с ней 60° (cos 60° = 0,5; sin 60° ≈ 0,866). Чему равна вертикальная составляющая [[F_y]]?"),
        options = listOf(
            Opt(L("5 kN", "5 кН"), correct = true),
            Opt(L("8,66 kN", "8,66 кН"), why = L("To je 10 · sin 60°. Která složka leží **u** úhlu 60° – svislá, nebo vodorovná?", "Это 10 · sin 60°. Какая составляющая **прилежит** к углу 60° — вертикальная или горизонтальная?")),
            Opt(L("10 kN", "10 кН"), why = L("Síla je šikmá, svisle míří jen její část.", "Сила наклонная, вертикально действует лишь её часть.")),
        ),
        explain = L(
            "Úhel 60° leží u svislé složky, proto kosinus: [[F_y]] = 10 · cos 60° = 5 kN – vyšlo totéž co 10 · sin 30°. Nepamatuj si „x = cos“: cos patří vždy ke složce **u úhlu**, ať je úhel měřený odkudkoli.",
            "Угол 60° прилежит к вертикальной составляющей, поэтому косинус: [[F_y]] = 10 · cos 60° = 5 кН — то же, что 10 · sin 30°. Не запоминай «x = cos»: cos всегда относится к составляющей, **прилежащей к углу**, откуда бы угол ни отсчитывали.",
        ),
        columns = 3,
    )
}

internal fun lesson3() = Lesson(
    id = 3,
    title = L("Jedna šipka = dvě", "Одна стрелка = две"),
    summary = L("Síly se sčítají jako šipky. Šikmou sílu nahradíš vodorovnou a svislou složkou.", "Силы складываются как стрелки. Наклонную силу заменишь горизонтальной и вертикальной составляющими."),
    cards = listOf(
        Card { TwoRopes() },
        Card { ReplaceToggle() },
        Card { Grid345() },
        Card { AngleComponents() },
        Card { CheckCos() },
    ),
)
