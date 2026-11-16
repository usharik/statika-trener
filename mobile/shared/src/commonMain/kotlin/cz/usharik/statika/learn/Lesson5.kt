package cz.usharik.statika.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.Anim
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Lekce 5 – Rovnováha otáčení. Těleso v klidu se neposouvá ANI neotáčí: ΣF_x = 0, ΣF_y = 0, ΣM = 0.
 * Momentovou rovnici smíme psát k libovolnému bodu – výhodný je ten, kterým prochází nejvíc neznámých.
 */

/** Bod (x, y) otočený kolem (px, py) o [deg] stupňů (osa y dolů). */
private fun turn(x: Double, y: Double, px: Double, py: Double, deg: Double): Pair<Double, Double> {
    val a = deg * PI / 180
    val dx = x - px
    val dy = y - py
    return (px + dx * cos(a) - dy * sin(a)) to (py + dx * sin(a) + dy * cos(a))
}

/** Krabice závaží stojící na otočené houpačce v poloze [p] metrů od osy. */
private fun PrimBuilder.weight(p: Double, label: String, px: Double, py: Double, unit: Double, deg: Double, c: StatikaColors, movable: Boolean, alpha: Float = 1f) {
    val x0 = px + p * unit
    val w = 34.0
    val h = 28.0
    val pts = listOf(x0 - w / 2 to py - 5 - h, x0 + w / 2 to py - 5 - h, x0 + w / 2 to py - 5, x0 - w / 2 to py - 5)
        .map { (x, y) -> turn(x, y, px, py, deg) }
    poly(pts, if (movable) c.focusBg else c.subtle, c.load, 1.6, alpha)
    val (tx, ty) = turn(x0, py - 5 - h / 2 + 5, px, py, deg)
    text(tx, ty, label, c.load, 13.0, bold = true, alpha = alpha)
}

/** Stav houpačky: podepřená, padá, dosedla na zem, po puštění zůstala vodorovně. */
private enum class SeePhase { HELD, FALLING, DOWN, LEVEL }

/**
 * Karta 0: houpačka. Podpěry ji drží vodorovně, žák posouvá závaží a tipne, kam se nakloní; „Pusť“ podpěry odstraní.
 * Nevyrovnaný moment houpačku roztáčí (úhel roste se čtvercem času), dokud konec nedosedne na zem.
 * Nový pokus = houpačka zmizí a objeví se znovu podepřená (nikdy se sama nevrací).
 */
@Composable
private fun CardScope.Seesaw() {
    val t = tr()
    val px = 200.0
    val py = 140.0
    val unit = 70.0 // 1 m = 70 jednotek
    val half = 2.6 * unit
    val ground = py + 40
    val thMax = asin((ground - py - 5) / half) * 180 / PI // úhel, kdy konec dosedne na zem
    // kolo 0: vlevo 6 kN v 1 m, posouvá se 3 kN; kolo 1: vlevo 3 kN ve 2 m, posouvá se 4 kN
    var round by remember { mutableIntStateOf(0) }
    val leftF = if (round == 0) 6.0 else 3.0
    val leftR = if (round == 0) 1.0 else 2.0
    val rightF = if (round == 0) 3.0 else 4.0
    var p by remember { mutableDoubleStateOf(0.5) }
    var dragging by remember { mutableStateOf(false) }
    var guess by remember { mutableStateOf<Int?>(null) } // 0 = levý konec dolů, 1 = vodorovně, 2 = pravý konec dolů
    var needGuess by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(SeePhase.HELD) }
    var theta by remember { mutableDoubleStateOf(0.0) } // stupně, + = po směru hodinek (pravý konec dolů)
    var runId by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<Pair<Double, Boolean>?>(null) } // ΣM posledního pokusu, tip sedí
    var tried by remember { mutableStateOf(false) }
    var level0 by remember { mutableStateOf(false) }
    var level1 by remember { mutableStateOf(false) }
    val fade = remember { Animatable(1f) }
    var resetting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun sumM(pp: Double) = leftF * leftR - rightF * pp // ↺ kladně
    /** nový pokus: stará houpačka zmizí, nová se objeví podepřená */
    fun fresh(then: () -> Unit = {}) {
        if (phase == SeePhase.HELD || resetting) {
            then()
            return
        }
        resetting = true
        scope.launch {
            fade.animateTo(0f, tween(180))
            theta = 0.0
            phase = SeePhase.HELD
            result = null
            guess = null
            then()
            resetting = false
            fade.animateTo(1f, tween(260))
        }
    }
    fun setP(v: Double) {
        val np = (round(v * 2) / 2).coerceIn(0.5, 2.5)
        if (np != p) fresh { p = np }
    }
    LaunchedEffect(runId) {
        if (runId == 0) return@LaunchedEffect
        val sum = sumM(p)
        val outcome = if (abs(sum) < 1e-9) 1 else if (sum > 0) 0 else 2
        val ok = guess == outcome
        tried = true
        if (outcome == 1) {
            // bez podpěr zůstane vodorovně – momenty se vyruší
            delay(900)
            phase = SeePhase.LEVEL
            result = sum to ok
            if (round == 0) level0 = true else level1 = true
            return@LaunchedEffect
        }
        // úhlové zrychlení úměrné ΣM (kladné ΣM = ↺ = levý konec dolů = záporný úhel)
        val acc = -sum * 9.0
        var time = 0.0
        var last = 0L
        while (abs(theta) < thMax) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.0 else (now - last) / 1e9
                last = now
                time += dt
                theta = (0.5 * acc * time * time).coerceIn(-thMax, thMax)
            }
        }
        theta = sign(acc) * thMax
        phase = SeePhase.DOWN
        result = sum to ok
    }
    Stage(
        h = 230.0,
        description = L("Houpačka se dvěma závažími, podepřená podpěrami", "Качели с двумя грузами на подпорках").t(),
        onDrag = { pt, start ->
            val (wx, wy) = turn(px + p * unit, py - 20, px, py, theta)
            if (start) dragging = phase != SeePhase.FALLING && pt.dist(wx, wy) < 55
            if (dragging) setP((pt.x - px) / unit)
        },
        onDragEnd = { dragging = false },
    ) { c ->
        val d = theta
        val a = fade.value
        // zem s měkkými podložkami pod konci
        hatch(10.0, 390.0, ground, c, Anim.None, 1f)
        for (sx in listOf(px - half + 12, px + half - 12)) rect(sx - 14, ground - 4, 28.0, 4.0, c.subtle, c.inkSoft, 1.0)
        // podstavec
        poly(listOf(px to py + 5, px - 18 to ground, px + 18 to ground), c.paper, c.ink, 1.5)
        // podpěry drží houpačku vodorovně, dokud ji žák nepustí
        if (phase == SeePhase.HELD) {
            // mezi značkami 2 m a 2,5 m, aby nezakrývaly popisky
            for (sx in listOf(px - 2.25 * unit, px + 2.25 * unit)) rect(sx - 4, py + 5, 8.0, ground - py - 5, c.subtle, c.ink, 1.3, alpha = a)
        }
        val beamPts = listOf(px - half to py - 5, px + half to py - 5, px + half to py + 5, px - half to py + 5)
            .map { (x, y) -> turn(x, y, px, py, d) }
        poly(beamPts, c.beam, c.ink, 1.6, a)
        // značky poloh: vpravo po 0,5 m, vlevo poloha pevného závaží
        for (i in 1..5) {
            val s = i * 0.5
            val (mx, my) = turn(px + s * unit, py + 18, px, py, d)
            circle(mx, my - 13, 3.0, c.inkSoft, alpha = a)
            text(mx, my + 8, "${num(s)} m", c.inkSoft, 12.0, alpha = a)
        }
        val (lx, ly) = turn(px - leftR * unit, py + 18, px, py, d)
        text(lx, ly + 8, "${num(leftR)} m", c.inkSoft, 12.0, alpha = a)
        weight(-leftR, "${num(leftF)} kN", px, py, unit, d, c, false, a)
        weight(p, "${num(rightF)} kN", px, py, unit, d, c, true, a)
        if (phase != SeePhase.FALLING) {
            val (hx, hy) = turn(px + p * unit, py - 20, px, py, d)
            handle(hx, hy - 30, c, dragging)
        }
        val head = when (phase) {
            SeePhase.HELD -> L("podepřeno", "на подпорках")
            SeePhase.FALLING -> L("puštěno…", "отпустили…")
            SeePhase.DOWN -> L("ΣM ≠ 0 → naklonila se až na zem", "ΣM ≠ 0 → наклонились до земли")
            SeePhase.LEVEL -> L("ΣM = 0 ✓ zůstala vodorovně", "ΣM = 0 ✓ остались горизонтально")
        }
        text(px, 22.0, t.pick(head), if (phase == SeePhase.LEVEL) c.accent else c.inkSoft, 14.0, bold = phase == SeePhase.LEVEL)
    }
    CardText(L(
        "Houpačku zatím drží **podpěry**. Posuň závaží vpravo, tipni, kam se houpačka nakloní, a pusť ji. Rozhoduje **součet momentů** ΣM k ose (↺ +, ↻ −): když není nulový, houpačka se roztočí a dosedne koncem na zem.",
        "Качели пока держат **подпорки**. Передвинь правый груз, угадай, куда наклонятся качели, и отпусти. Решает **сумма моментов** ΣM относительно оси (↺ +, ↻ −): если она не равна нулю, качели начнут поворачиваться и опустятся концом на землю.",
    ))
    Todo(
        L("Tipni, kam se houpačka nakloní, a klepni na „Pusť“", "Угадай, куда наклонятся качели, и нажми «Отпустить»") to tried,
        L("Kolo 1: najdi polohu závaží 3 kN, ve které houpačka po puštění zůstane vodorovně", "Раунд 1: найди положение груза 3 кН, при котором качели после отпускания останутся горизонтально") to level0,
        L("Kolo 2: vlevo je 3 kN ve 2 m – kam patří závaží 4 kN?", "Раунд 2: слева 3 кН на 2 м — куда поставить груз 4 кН?") to level1,
    )
    Chips(listOf(L("0,5 m", "0,5 м"), L("1 m", "1 м"), L("1,5 m", "1,5 м"), L("2 m", "2 м"), L("2,5 m", "2,5 м")), ((p - 0.5) * 2).toInt()) { setP(0.5 + it * 0.5) }
    SmallText(L("Tvůj tip – po puštění se houpačka:", "Твой прогноз — после отпускания качели:"))
    Chips(
        listOf(L("↙ nakloní doleva", "↙ наклонятся влево"), L("— zůstane vodorovně", "— останутся горизонтально"), L("↘ nakloní doprava", "↘ наклонятся вправо")),
        guess,
    ) { g ->
        if (phase == SeePhase.FALLING) return@Chips
        fresh { guess = g; needGuess = false }
    }
    ActionButton(L("Pusť (odstraň podpěry)", "Отпустить (убрать подпорки)")) {
        when {
            phase != SeePhase.HELD -> fresh()
            guess == null -> needGuess = true
            else -> {
                phase = SeePhase.FALLING
                runId++
            }
        }
    }
    if (needGuess && guess == null) SmallText(L("Nejdřív tipni, co houpačka udělá.", "Сначала угадай, что сделают качели."))
    result?.let { (sum, ok) ->
        Readout("ΣM = ${num(leftF)} · ${num(leftR)} − ${num(rightF)} · ${num(p)} = ${num(sum)} kNm", ok = abs(sum) < 1e-9)
        if (abs(sum) < 1e-9) Note(Tone.OK, if (round == 0) L(
            "Rovnováha! Poloviční síla potřebuje dvojnásobné rameno: 6 · 1 = 3 · 2. Momenty obou závaží jsou stejně velké a opačné, takže houpačka zůstane, jak je.",
            "Равновесие! Вдвое меньшей силе нужно вдвое большее плечо: 6 · 1 = 3 · 2. Моменты грузов равны по величине и противоположны, поэтому качели остаются как есть.",
        ) else L(
            "Ano: 3 · 2 = 4 · 1,5. Těžší závaží stačí kratší rameno – rozhoduje součin síla × rameno, ne síla sama.",
            "Да: 3 · 2 = 4 · 1,5. Более тяжёлому грузу хватает меньшего плеча — решает произведение сила × плечо, а не сама сила.",
        ))
        else Note(if (ok) Tone.INFO else Tone.NUDGE, L(
            (if (ok) "Tip sedí. " else "Tip nesedí. ") + "ΣM není nula – " + (if (sum > 0) "převážil moment levého závaží (↺)" else "převážil moment pravého závaží (↻)") + ", houpačka se roztáčela čím dál rychleji, až dosedla na zem. Posuň závaží a zkus to znovu.",
            (if (ok) "Прогноз верный. " else "Прогноз неверный. ") + "ΣM не равна нулю — " + (if (sum > 0) "перевесил момент левого груза (↺)" else "перевесил момент правого груза (↻)") + ", качели поворачивались всё быстрее, пока не опустились на землю. Передвинь груз и попробуй снова.",
        ))
    }
    if (level0 && round == 0) ActionButton(L("Kolo 2 →", "Раунд 2 →")) {
        fresh {
            round = 1
            p = 0.5
        }
    }
}

/** Vor na klidné vodě shora: jeden nebo dva lodní motory, motor 2 jde posunout na jinou nositelku. */
@Composable
private fun CardScope.Raft() {
    val t = tr()
    val cx0 = 130.0
    val cy0 = 112.0
    val w = 110.0
    val h = 60.0
    val perM = 20.0 // jednotek na metr
    val force = 0.5 // kN na motor
    var both by remember { mutableStateOf(false) }
    var dm by remember { mutableDoubleStateOf(1.2) } // posun nositelky motoru 2 pod těžiště, m
    var guess by remember { mutableStateOf<Int?>(null) } // 0 stojí, 1 jede, 2 točí se
    var dragging by remember { mutableStateOf(false) }
    // stav pokusu
    var running by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var dx by remember { mutableDoubleStateOf(0.0) }
    var ang by remember { mutableDoubleStateOf(0.0) }
    val ghosts = remember { mutableStateListOf<Pair<Double, Double>>() }
    var runId by remember { mutableIntStateOf(0) }
    val fade = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var ranOne by remember { mutableStateOf(false) }
    var ranCouple by remember { mutableStateOf(false) }
    var ranRest by remember { mutableStateOf(false) }
    var lastKind by remember { mutableStateOf<Int?>(null) } // 0 jede, 1 točí se, 2 stojí
    var needGuess by remember { mutableStateOf(false) }
    val couple = both && abs(dm) > 1e-9
    fun fresh(then: () -> Unit = {}) {
        if (!ended && !running) {
            then()
            return
        }
        scope.launch {
            fade.animateTo(0f, tween(180))
            running = false
            ended = false
            dx = 0.0
            ang = 0.0
            ghosts.clear()
            then()
            fade.animateTo(1f, tween(260))
        }
    }
    LaunchedEffect(runId) {
        if (runId == 0) return@LaunchedEffect
        val lin = if (both) 0.0 else 50.0 // zrychlení těžiště, jednotek/s²
        val rot = if (both) 4.0 * dm * perM else 0.0 // úhlové zrychlení, °/s² (moment F·d)
        var time = 0.0
        var last = 0L
        var nextGhost = 0.0
        while (time < 2.6) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.0 else (now - last) / 1e9
                last = now
                time += dt
                dx = 0.5 * lin * time * time
                ang = 0.5 * rot * time * time
                if (time >= nextGhost) {
                    ghosts += dx to ang
                    nextGhost += 0.4
                }
            }
        }
        running = false
        ended = true
        val kind = if (!both) 0 else if (couple) 1 else 2
        lastKind = kind
        when (kind) {
            0 -> ranOne = true
            1 -> ranCouple = true
            else -> if (ranCouple) ranRest = true
        }
        // pokus skončil: vor pomalu zmizí, stopa zůstane
        fade.animateTo(0.3f, tween(600))
    }
    fun bodyPt(bx: Double, by: Double, sx: Double, sa: Double): Pair<Double, Double> {
        val (rx, ry) = turn(cx0 + bx, cy0 + by, cx0, cy0, sa)
        return (rx + sx) to ry
    }
    Stage(
        h = 230.0,
        description = t.pick(L("Vor na vodě shora se dvěma lodními motory", "Плот на воде, вид сверху, с двумя лодочными моторами")),
        onDrag = { p, start ->
            if (start) {
                dragging = both && !running && p.dist(cx0 + w / 2 + 84, cy0 + dm * perM) < 40
            }
            if (dragging) {
                val nd = (round((p.y - cy0) / perM / 0.6) * 0.6).coerceIn(-1.2, 1.2)
                if (abs(nd - dm) > 1e-9) fresh { dm = nd; guess = null }
            }
        },
        onDragEnd = { dragging = false },
    ) { c ->
        // voda
        rect(8.0, 8.0, 384.0, 204.0, c.react.copy(alpha = 0.06f), null)
        for (k in 0..3) line(30.0 + k * 95, 196.0, 70.0 + k * 95, 196.0, c.react.copy(alpha = 0.3f), 1.0)
        val a = fade.value
        // nositelky motorů (pevně ve světě, než se vor rozjede)
        if (!running && !ended) {
            line(10.0, cy0, 390.0, cy0, c.inkSoft, 1.0, dash = true)
            if (both) line(10.0, cy0 + dm * perM, 390.0, cy0 + dm * perM, c.inkSoft, 1.0, dash = true)
        }
        // stopa: obrysy voru po 0,4 s
        for ((gx, ga) in ghosts) {
            val pts = listOf(-w / 2 to -h / 2, w / 2 to -h / 2, w / 2 to h / 2, -w / 2 to h / 2).map { (x, y) -> bodyPt(x, y, gx, ga) }
            poly(pts, null, c.inkSoft, 0.8, 0.45f)
            val (tx, ty) = bodyPt(0.0, 0.0, gx, ga)
            circle(tx, ty, 2.0, c.inkSoft, alpha = 0.6f)
        }
        // vor: prkna a těžiště T
        val pts = listOf(-w / 2 to -h / 2, w / 2 to -h / 2, w / 2 to h / 2, -w / 2 to h / 2).map { (x, y) -> bodyPt(x, y, dx, ang) }
        poly(pts, c.beam, c.ink, 1.6, a)
        for (k in 1..3) {
            val yy = -h / 2 + k * h / 4
            val (x1, y1) = bodyPt(-w / 2, yy, dx, ang)
            val (x2, y2) = bodyPt(w / 2, yy, dx, ang)
            line(x1, y1, x2, y2, c.inkSoft, 0.8, alpha = a * 0.6f)
        }
        val (tx, ty) = bodyPt(0.0, 0.0, dx, ang)
        line(tx - 7, ty, tx + 7, ty, c.ink, 1.4, alpha = a)
        line(tx, ty - 7, tx, ty + 7, c.ink, 1.4, alpha = a)
        text(tx - 6, ty - 6, "T", c.ink, 12.0, Anchor.END, bold = true, alpha = a)
        // motor 1: vlevo, tlačí doprava po nositelce přes T
        val (m1x, m1y) = bodyPt(-w / 2 - 44, 0.0, dx, ang)
        val (m1t, m1u) = bodyPt(-w / 2, 0.0, dx, ang)
        val (b1x, b1y) = bodyPt(-w / 2 - 52, 0.0, dx, ang)
        circle(b1x, b1y, 7.0, c.ink, alpha = a)
        force(m1x, m1y, m1t, m1u, c.load, width = 3.0, alpha = a)
        text(m1x + 4, m1y - 10, "1", c.load, 12.0, Anchor.START, bold = true, alpha = a)
        if (both) {
            // motor 2: vpravo, tlačí doleva po nositelce posunuté o d
            val yy = dm * perM
            val (m2x, m2y) = bodyPt(w / 2 + 44, yy, dx, ang)
            val (m2t, m2u) = bodyPt(w / 2, yy, dx, ang)
            val (b2x, b2y) = bodyPt(w / 2 + 52, yy, dx, ang)
            circle(b2x, b2y, 7.0, c.ink, alpha = a)
            force(m2x, m2y, m2t, m2u, c.load, width = 3.0, alpha = a)
            text(m2x - 4, m2y - 10, "2", c.load, 12.0, Anchor.END, bold = true, alpha = a)
            if (!running && !ended) {
                handle(w / 2 + cx0 + 84, cy0 + yy, c, dragging)
                if (abs(dm) > 1e-9) {
                    // vzdálenost nositelek d
                    val xd = cx0 + w / 2 + 120
                    line(xd, cy0, xd, cy0 + yy, c.derived, 1.4)
                    line(xd - 5, cy0, xd + 5, cy0, c.derived, 1.4)
                    line(xd - 5, cy0 + yy, xd + 5, cy0 + yy, c.derived, 1.4)
                    text(xd + 8, cy0 + yy / 2 + 5, "d = ${num(abs(dm))} m", c.derived, 12.0, Anchor.START)
                }
            }
            if (couple && (running || ended) && a > 0.25f) moment(tx, ty, 20.0, dm < 0, c.derived)
        }
        if (ended) text(200.0, 26.0, t.pick(L("konec pokusu – stopa po 0,4 s", "конец опыта — след через 0,4 с")), c.inkSoft, 12.0)
    }
    CardText(L(
        "Vor na klidné vodě (odpor vody zanedbáme), shora. Lodní motor tlačí silou 0,5 kN. Spusť nejdřív jeden motor, pak přidej druhý, stejně silný, proti němu – a sleduj, jestli ΣF = 0 stačí k tomu, aby vor zůstal v klidu.",
        "Плот на спокойной воде (сопротивлением воды пренебрегаем), вид сверху. Лодочный мотор толкает с силой 0,5 кН. Запусти сначала один мотор, потом добавь второй, такой же сильный, навстречу — и проверь, хватает ли ΣF = 0, чтобы плот оставался в покое.",
    ))
    Todo(
        L("Spusť **jen motor 1** – co udělá vor?", "Запусти **только мотор 1** — что сделает плот?") to ranOne,
        L("Zapni **oba motory** (ΣF = 0), tipni, co vor udělá, a spusť je", "Включи **оба мотора** (ΣF = 0), угадай, что сделает плот, и запусти") to ranCouple,
        L("Posuň motor 2 (kroužek nebo tlačítka) tak, aby vor zůstal stát, a ověř to", "Передвинь мотор 2 (кружок или кнопки) так, чтобы плот стоял, и проверь") to ranRest,
    )
    Chips(listOf(L("jen motor 1", "только мотор 1"), L("oba motory", "оба мотора")), if (both) 1 else 0) { i ->
        if (running) return@Chips
        fresh { both = i == 1; guess = null }
    }
    if (both) {
        Chips(listOf(L("motor 2 výš", "мотор 2 выше"), L("motor 2 níž", "мотор 2 ниже"))) { i ->
            if (running) return@Chips
            val nd = (dm + if (i == 0) -0.6 else 0.6).coerceIn(-1.2, 1.2)
            fresh { dm = nd; guess = null }
        }
        // moment prozradí až pokus – jinak by tip nebyl tipem
        Readout("ΣF = 0,5 − 0,5 = 0 ✓" + if (ended && lastKind != 0) "    M = 0,5 · ${num(abs(dm))} = ${num(force * abs(dm))} kNm" else "")
        SmallText(L("Tvůj tip – vor:", "Твой прогноз — плот:"))
        Chips(listOf(L("zůstane stát", "останется на месте"), L("pojede rovně", "поедет прямо"), L("bude se točit", "будет вращаться")), guess) { g ->
            if (running) return@Chips
            fresh { guess = g; needGuess = false }
        }
    }
    ActionButton(L("Zapni motory", "Включить моторы")) {
        if (running) return@ActionButton
        if (both && guess == null) {
            needGuess = true
            return@ActionButton
        }
        fresh {
            running = true
            ended = false
            runId++
        }
    }
    if (needGuess && both && guess == null) SmallText(L("Nejdřív tipni, co vor udělá.", "Сначала угадай, что сделает плот."))
    if (ended) when (lastKind) {
        0 -> Note(Tone.INFO, L(
            "Jedna nevyrovnaná síla vor **rozjíždí** – obrysy se od sebe vzdalují čím dál víc. Její nositelka prochází těžištěm T, proto se vor netočí.",
            "Одна неуравновешенная сила **разгоняет** плот — контуры расходятся всё сильнее. Её линия действия проходит через центр тяжести T, поэтому плот не вращается.",
        ))
        1 -> Note(if (guess == 2) Tone.OK else Tone.NUDGE, L(
            "ΣF = 0, takže těžiště T zůstalo na místě – ale síly leží na **různých** nositelkách. Tvoří dvojici s momentem M = F · d a vor se roztáčí čím dál rychleji. **ΣF = 0 nestačí.**",
            "ΣF = 0, поэтому центр тяжести T остался на месте — но силы лежат на **разных** линиях действия. Они образуют пару с моментом M = F · d, и плот вращается всё быстрее. **ΣF = 0 недостаточно.**",
        ))
        else -> Note(Tone.OK, L(
            "Teď leží obě síly na **jedné** nositelce: ΣF = 0 i ΣM = 0 – vor se nehne.",
            "Теперь обе силы лежат на **одной** линии действия: ΣF = 0 и ΣM = 0 — плот не двигается.",
        ))
    }
    if (ranOne && ranCouple && ranRest) {
        Readout("Σ[[F_x]] = 0     Σ[[F_y]] = 0     ΣM = 0", ok = true)
        Note(Tone.OK, L(
            "Těleso v rovině se může posunout vodorovně, posunout svisle a otočit. Aby zůstalo v klidu, musí platit všechny tři **rovnice rovnováhy** – nulový součet sil ještě neznamená nulový součet momentů.",
            "Тело на плоскости может сдвинуться по горизонтали, по вертикали и повернуться. Чтобы оно оставалось в покое, должны выполняться все три **уравнения равновесия** — нулевая сумма сил ещё не означает нулевую сумму моментов.",
        ))
    }
}

/** Karta 2: výhodný bod pro momentovou rovnici – nejdřív [[R_B]], pak [[R_Ay]]. */
@Composable
private fun CardScope.ChoosePoint() {
    val y = 100.0
    val xa = 80.0
    val u = 60.0
    val xb = xa + 4 * u
    val xs = xa + 2 * u
    var pick by remember { mutableStateOf<Int?>(null) }
    var gotB by remember { mutableStateOf(false) }
    var gotA by remember { mutableStateOf(false) }
    // co právě hledáme: 0 = R_B, 1 = R_Ay
    val goal = if (gotB) 1 else 0
    val pxs = listOf(xa, xs, xb)
    fun choose(i: Int) {
        pick = i
        if (goal == 0 && i == 0) gotB = true
        else if (goal == 1 && i == 2) gotA = true
    }
    Stage(
        h = 220.0,
        description = L("Prostý nosník s reakcemi a volbou bodu", "Простая балка с реакциями и выбором точки").t(),
        onTap = { pt ->
            val i = pxs.indices.minByOrNull { abs(pt.x - pxs[it]) } ?: return@Stage
            if (abs(pt.x - pxs[i]) < 40 && abs(pt.y - y) < 60) choose(i)
        },
    ) { c ->
        // uvolněný nosník: podpory jsou nahrazené reakcemi, proto je nekreslíme
        beam(xa - 10, xb + 10, y, c)
        force(xa + u, y - 66, xa + u, y - 6, c.load)
        text(xa + u + 8, y - 50, " = 8 kN", c.load, 14.0, Anchor.START, sym = "F")
        // reakce; ty, jejichž nositelka prochází zvoleným bodem, zeslábnou (rameno 0)
        val fadeAy = pick == 0
        val fadeB = pick == 2
        val fadeAx = pick != null
        force(xa - 58, y, xa - 6, y, c.react, alpha = if (fadeAx) 0.25f else 1f)
        text(xa - 34, y - 10, null, c.react, 13.0, sym = "R_Ax", alpha = if (fadeAx) 0.25f else 1f)
        force(xa, y + 62, xa, y + 6, c.react, alpha = if (fadeAy) 0.25f else 1f)
        text(xa + 8, y + 50, null, c.react, 13.0, Anchor.START, sym = "R_Ay", alpha = if (fadeAy) 0.25f else 1f, bold = goal == 1)
        force(xb, y + 62, xb, y + 6, c.react, alpha = if (fadeB) 0.25f else 1f)
        text(xb + 8, y + 50, null, c.react, 13.0, Anchor.START, sym = "R_B", alpha = if (fadeB) 0.25f else 1f, bold = goal == 0)
        // body k volbě
        val names = listOf("A", "S", "B")
        pxs.forEachIndexed { i, x ->
            if (pick == i) {
                circle(x, y, 10.0, c.accent.copy(alpha = 0.15f), c.accent, 1.6)
                line(x - 14, y, x + 14, y, c.accent, 1.3)
                line(x, y - 14, x, y + 14, c.accent, 1.3)
            } else tapSpot(x, y, c)
            text(x + (if (i == 0) 16.0 else 0.0), y - 20, names[i], c.ink, 14.0, if (i == 0) Anchor.START else Anchor.MIDDLE, bold = true)
        }
        // kóty
        val yd = y + 92
        line(xa, yd, xb, yd, c.ink, 0.9)
        for (x in listOf(xa, xa + u, xs, xb)) line(x - 4, yd + 4, x + 4, yd - 4, c.ink, 1.3)
        text(xa + u / 2, yd - 5, "1 m", c.ink, 13.0)
        text(xa + 1.5 * u, yd - 5, "1 m", c.ink, 13.0)
        text(xa + 3 * u, yd - 5, "2 m", c.ink, 13.0)
    }
    CardText(L(
        "Modré šipky jsou **neznámé síly**, kterými podpory drží nosník (víc v lekcích 6–7). Rovnici ΣM = 0 smíš napsat k libovolnému bodu. Síla, jejíž nositelka bodem prochází, má k němu rameno 0 – a z rovnice vypadne.",
        "Синие стрелки — **неизвестные силы**, которыми опоры держат балку (подробнее в уроках 6–7). Уравнение ΣM = 0 можно записать относительно любой точки. Сила, линия действия которой проходит через точку, имеет относительно неё плечо 0 — и выпадает из уравнения.",
    ))
    Todo(
        L("Chceš hned [[R_B]]: klepni na bod, ke kterému v rovnici zbude jen [[R_B]]", "Нужна сразу [[R_B]]: нажми на точку, относительно которой в уравнении останется только [[R_B]]") to gotB,
        L("Teď chceš hned [[R_Ay]]: který bod vybereš?", "Теперь нужна сразу [[R_Ay]]: какую точку выберешь?") to gotA,
    )
    Chips(listOf(L("bod A", "точка A"), L("střed S", "середина S"), L("bod B", "точка B")), pick) { choose(it) }
    when (pick) {
        0 -> Readout("Σ[[M_A]] = 0:  [[R_Ax]]·0 + [[R_Ay]]·0 − 8·1 + [[R_B]]·4 = 0  →  [[R_B]] = 2 kN", ok = true)
        1 -> Readout("Σ[[M_S]] = 0:  [[R_Ax]]·0 − [[R_Ay]]·2 + 8·1 + [[R_B]]·2 = 0")
        2 -> Readout("Σ[[M_B]] = 0:  [[R_Ax]]·0 − [[R_Ay]]·4 + 8·3 + [[R_B]]·0 = 0  →  [[R_Ay]] = 6 kN", ok = true)
        else -> {}
    }
    val p = pick
    if (p != null) {
        val good = (goal == 0 && p == 0) || (goal == 1 && p == 2) || (goal == 1 && gotA && p == 2)
        when {
            gotA && p == 2 -> Note(Tone.OK, L(
                "Nositelka [[R_B]] prochází bodem B, takže [[R_B]] z rovnice vypadne. Zbyla jen [[R_Ay]] = 6 kN. Kontrola: [[R_Ay]] + [[R_B]] = 6 + 2 = 8 kN = F ✓",
                "Линия действия [[R_B]] проходит через B, поэтому [[R_B]] из уравнения выпадает. Осталась только [[R_Ay]] = 6 кН. Проверка: [[R_Ay]] + [[R_B]] = 6 + 2 = 8 кН = F ✓",
            ))
            gotB && p == 0 -> Note(Tone.OK, L(
                "Nositelky [[R_Ax]] i [[R_Ay]] procházejí bodem A – mají rameno 0 a z rovnice vypadnou. Zbyla jediná neznámá [[R_B]] = 2 kN.",
                "Линии действия [[R_Ax]] и [[R_Ay]] проходят через A — их плечо 0, и из уравнения они выпадают. Осталась одна неизвестная [[R_B]] = 2 кН.",
            ))
            p == 1 -> Note(Tone.NUDGE, L(
                "Ke středu mají rameno [[R_Ay]] i [[R_B]] – dvě neznámé v jedné rovnici, z ní samotné nic nespočítáš.",
                "Относительно середины плечо есть и у [[R_Ay]], и у [[R_B]] — две неизвестные в одном уравнении, из него одного ничего не найти.",
            ))
            !good && goal == 0 -> Note(Tone.NUDGE, L(
                "K bodu B vypadne právě [[R_B]] (má rameno 0) – a ta je to, co hledáš. Který bod vyřadí obě ostatní neznámé?",
                "Относительно B выпадает как раз [[R_B]] (её плечо 0) — а её ты и ищешь. Какая точка уберёт обе другие неизвестные?",
            ))
            !good -> Note(Tone.NUDGE, L(
                "K bodu A vypadne právě [[R_Ay]] – a tu hledáš. Kterým bodem prochází nositelka druhé svislé neznámé?",
                "Относительно A выпадает как раз [[R_Ay]] — а её ты ищешь. Через какую точку проходит линия действия другой вертикальной неизвестной?",
            ))
        }
    }
}

/** Karta 3: kontrolní otázka – jiná úloha než na kartě 2: tentokrát chceme hned [[R_Ay]]. */
@Composable
private fun CardScope.Check() {
    val y = 80.0
    val xa = 60.0
    val u = 70.0
    val xb = xa + 4 * u
    Stage(h = 190.0, description = L("Prostý nosník se silou F ve 3 m od podpory A", "Простая балка с силой F в 3 м от опоры A").t()) { c ->
        beam(xa - 10, xb + 10, y, c)
        support(SupportType.PIN, xa, y, c, "A")
        support(SupportType.ROLLER, xb, y, c, "B")
        force(xa + 3 * u, y - 58, xa + 3 * u, y - 6, c.load)
        text(xa + 3 * u - 8, y - 44, " = 6 kN", c.load, 14.0, Anchor.END, sym = "F")
        // kóty
        val yd = y + 82
        line(xa, yd, xb, yd, c.ink, 0.9)
        for (x in listOf(xa, xa + 3 * u, xb)) line(x - 4, yd + 4, x + 4, yd - 4, c.ink, 1.3)
        text(xa + 1.5 * u, yd - 5, "3 m", c.ink, 13.0)
        text(xa + 3.5 * u, yd - 5, "1 m", c.ink, 13.0)
    }
    Quiz(
        question = L(
            "Ke kterému bodu napíšeš ΣM = 0, aby z rovnice hned vyšla svislá síla v podpoře A, [[R_Ay]]?",
            "Относительно какой точки записать ΣM = 0, чтобы из уравнения сразу получить вертикальную силу в опоре A, [[R_Ay]]?",
        ),
        options = listOf(
            Opt(
                L("k bodu A", "относительно точки A"),
                why = L("Nositelka [[R_Ay]] prochází bodem A, k němu má rameno 0. Objeví se pak [[R_Ay]] v rovnici vůbec?", "Линия действия [[R_Ay]] проходит через точку A, её плечо там 0. Войдёт ли тогда [[R_Ay]] в уравнение вообще?"),
            ),
            Opt(L("k bodu B", "относительно точки B"), correct = true),
            Opt(
                L("ke středu nosníku", "относительно середины балки"),
                why = L("Ke středu má rameno [[R_Ay]] i síla v podpoře B – v rovnici by zůstaly dvě neznámé. Který bod jednu z nich vyřadí?", "Относительно середины плечо есть и у [[R_Ay]], и у силы в опоре B — в уравнении останутся две неизвестные. Какая точка уберёт одну из них?"),
            ),
        ),
        explain = L(
            "K bodu B: síla v podpoře B jím prochází, má rameno 0 a z rovnice vypadne. Zbude −[[R_Ay]]·4 + 6·1 = 0, tedy [[R_Ay]] = 1,5 kN.",
            "Относительно B: сила в опоре B проходит через эту точку, её плечо 0, и из уравнения она выпадает. Остаётся −[[R_Ay]]·4 + 6·1 = 0, то есть [[R_Ay]] = 1,5 кН.",
        ),
    )
}

internal fun lesson5() = Lesson(
    id = 5,
    title = L("Rovnováha otáčení", "Равновесие моментов"),
    summary = L("ΣM = 0, proč ΣF = 0 nestačí a jak vybrat výhodný bod.", "ΣM = 0, почему ΣF = 0 недостаточно и как выбрать удобную точку."),
    cards = listOf(
        Card { Seesaw() },
        Card { Raft() },
        Card { ChoosePoint() },
        Card { Check() },
    ),
)
