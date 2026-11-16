package cz.usharik.statika.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import cz.usharik.statika.ui.LocalColors
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
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
import cz.usharik.statika.model.parseNum
import cz.usharik.statika.trainer.FieldState
import cz.usharik.statika.ui.NumField
import cz.usharik.statika.ui.rich
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
import kotlin.math.roundToInt
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

/** Tip před pokusem: krok se splní jakoukoli volbou, správnost ukáže až následující pokus. */
private fun tipStep(label: L, options: List<L>, pick: Int?, onPick: (Int) -> Unit): Step =
    Step(label, pick != null, result = { pick?.let { SmallText(L("Tvůj tip: **${options[it].cs}**", "Твой прогноз: **${options[it].ru}**")) } }) {
        Chips(options, pick) { if (pick == null) onPick(it) }
    }

/**
 * Karta 0: houpačka. Podpěry ji drží vodorovně; žák nejdřív tipne, kam se nakloní, pak „Pusť“ podpěry odstraní.
 * Nevyrovnaný moment houpačku roztáčí (úhel roste se čtvercem času), dokud konec nedosedne na zem.
 * Nový pokus = houpačka zmizí a objeví se znovu podepřená při změně polohy závaží (nikdy se sama nevrací);
 * tlačítko „Pusť“ jen pouští. Kolo 2: žák polohu spočítá, závaží se tam postaví a houpačka se pustí.
 */
@Composable
private fun CardScope.Seesaw() {
    val t = tr()
    val knm = t.pick(L("kNm", "кН·м"))
    val px = 200.0
    val py = 140.0
    val unit = 70.0 // 1 m = 70 jednotek
    val half = 2.6 * unit
    val ground = py + 40
    val thMax = asin((ground - py - 5) / half) * 180 / PI // úhel, kdy konec dosedne na zem
    // kolo 0: vlevo 6 kN v 1 m, posouvá se 3 kN; kolo 1: vlevo 3 kN ve 2 m, závaží 4 kN
    var round by remember { mutableIntStateOf(0) }
    val leftF = if (round == 0) 6.0 else 3.0
    val leftR = if (round == 0) 1.0 else 2.0
    val rightF = if (round == 0) 3.0 else 4.0
    var p by remember { mutableDoubleStateOf(0.5) }
    var dragging by remember { mutableStateOf(false) }
    var tip by remember { mutableStateOf<Int?>(null) } // 0 = levý konec dolů, 1 = vodorovně, 2 = pravý konec dolů
    var phase by remember { mutableStateOf(SeePhase.HELD) }
    var theta by remember { mutableDoubleStateOf(0.0) } // stupně, + = po směru hodinek (pravý konec dolů)
    var runId by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<Double?>(null) } // ΣM posledního pokusu
    var tried by remember { mutableStateOf(false) }
    var level0 by remember { mutableStateOf(false) }
    var level1 by remember { mutableStateOf(false) }
    // kolo 2: zadaná vzdálenost
    var input by remember { mutableStateOf("") }
    var inState by remember { mutableStateOf<FieldState?>(null) }
    var inMsg by remember { mutableStateOf<L?>(null) }
    var misses by remember { mutableIntStateOf(0) }
    val fade = remember { Animatable(1f) }
    var resetting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // 0 tip, 1 první puštění, 2 hledání polohy, 3 kolo 2, 4 hotovo
    val stage = when {
        tip == null -> 0
        !tried -> 1
        !level0 -> 2
        !level1 -> 3
        else -> 4
    }
    val canMove = stage == 2
    fun sumM(pp: Double) = leftF * leftR - rightF * pp // ↺ kladně
    /** nový pokus: stará houpačka zmizí, nová se objeví podepřená */
    suspend fun renew(then: () -> Unit) {
        if (phase == SeePhase.HELD) {
            then()
            return
        }
        resetting = true
        fade.animateTo(0f, tween(180))
        theta = 0.0
        phase = SeePhase.HELD
        result = null
        then()
        resetting = false
        fade.animateTo(1f, tween(260))
    }
    fun fresh(then: () -> Unit = {}) {
        if (resetting) return
        if (phase == SeePhase.HELD) then() else scope.launch { renew(then) }
    }
    fun setP(v: Double) {
        if (!canMove || phase == SeePhase.FALLING) return
        val np = (round(v * 2) / 2).coerceIn(0.5, 2.5)
        if (np != p) fresh { p = np }
    }
    fun release() {
        if (phase != SeePhase.HELD || resetting) return
        phase = SeePhase.FALLING
        runId++
    }
    /** kolo 2: postav závaží do spočítané vzdálenosti a pusť */
    fun place() {
        if (phase == SeePhase.FALLING || resetting) return
        val v = parseNum(input)
        if (v == null || v <= 0.0 || v > 2.5) {
            inState = FieldState.NUDGE
            inMsg = L("Zadej vzdálenost od osy – houpačka má na každou stranu 2,5 m.", "Введи расстояние от оси — у качелей по 2,5 м в каждую сторону.")
            return
        }
        inMsg = null
        scope.launch {
            renew { p = v }
            delay(450)
            release()
        }
    }
    // kolo 2 začne samo, až žák uvidí vyrovnanou houpačku z kola 1
    LaunchedEffect(level0) {
        if (!level0 || round != 0) return@LaunchedEffect
        delay(1800)
        renew {
            round = 1
            p = 0.5
        }
    }
    LaunchedEffect(runId) {
        if (runId == 0) return@LaunchedEffect
        val sum = sumM(p)
        if (abs(sum) < 1e-6) {
            // bez podpěr zůstane vodorovně – momenty se vyruší
            delay(900)
            phase = SeePhase.LEVEL
            result = 0.0
            tried = true
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
        result = sum
        tried = true
        if (round == 1) {
            misses++
            inState = FieldState.NUDGE
        }
    }
    Stage(
        h = 230.0,
        description = L("Houpačka se dvěma závažími, podepřená podpěrami", "Качели с двумя грузами на подпорках").t(),
        onDrag = { pt, start ->
            val (wx, wy) = turn(px + p * unit, py - 20, px, py, theta)
            if (start) dragging = canMove && phase != SeePhase.FALLING && pt.dist(wx, wy) < 55
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
        weight(p, "${num(rightF)} kN", px, py, unit, d, c, canMove, a)
        if (canMove && phase != SeePhase.FALLING) {
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
        "Houpačku zatím drží **podpěry**. Až je odstraníme, rozhodne **součet momentů** ΣM k ose (↺ +, ↻ −): když není nulový, houpačka se roztočí a dosedne koncem na zem.",
        "Качели пока держат **подпорки**. Когда их уберём, всё решит **сумма моментов** ΣM относительно оси (↺ +, ↻ −): если она не равна нулю, качели начнут поворачиваться и опустятся концом на землю.",
    ))
    fun sumText(s: Double) = "ΣM = ${num(leftF)} · ${num(leftR)} − ${num(rightF)} · ${num(p)} = ${num(s)} $knm"
    val tipOpts = listOf(L("levý konec dolů", "левый конец вниз"), L("zůstane vodorovně", "останутся горизонтально"), L("pravý konec dolů", "правый конец вниз"))
    val releaseButton: @Composable () -> Unit = {
        ActionButton(L("Pusť (odstraň podpěry)", "Отпустить (убрать подпорки)"), enabled = phase == SeePhase.HELD && !resetting) { release() }
    }
    Steps(
        tipStep(L("Vlevo 6 kN v 1 m, vpravo 3 kN v 0,5 m. Kam se houpačka nakloní?", "Слева 6 кН на 1 м, справа 3 кН на 0,5 м. Куда наклонятся качели?"), tipOpts, tip) { tip = it },
        Step(
            L("Odstraň podpěry a ověř svůj tip", "Убери подпорки и проверь свой прогноз"), tried,
            result = { Note(if (tip == 0) Tone.OK else Tone.INFO, L(
                (if (tip == 0) "Tip sedí. " else "Tip nesedí. ") + "ΣM = 6 · 1 − 3 · 0,5 = 4,5 kNm ↺ – převážil moment levého závaží. Houpačka se roztáčela čím dál rychleji, až levým koncem dosedla na zem.",
                (if (tip == 0) "Прогноз верный. " else "Прогноз неверный. ") + "ΣM = 6 · 1 − 3 · 0,5 = 4,5 кН·м ↺ — перевесил момент левого груза. Качели поворачивались всё быстрее, пока левым концом не опустились на землю.",
            )) },
        ) { releaseButton() },
        Step(
            L("Najdi polohu závaží 3 kN, při které houpačka zůstane vodorovně", "Найди положение груза 3 кН, при котором качели останутся горизонтальными"), level0,
            result = { Note(Tone.OK, L(
                "Rovnováha! Poloviční síla potřebuje dvojnásobné rameno: 6 · 1 = 3 · 2. Momenty obou závaží jsou stejně velké a opačné, takže houpačka zůstane, jak je.",
                "Равновесие! Вдвое меньшей силе нужно вдвое большее плечо: 6 · 1 = 3 · 2. Моменты грузов равны по величине и противоположны, поэтому качели остаются как есть.",
            )) },
        ) {
            Chips(listOf(L("0,5 m", "0,5 м"), L("1 m", "1 м"), L("1,5 m", "1,5 м"), L("2 m", "2 м"), L("2,5 m", "2,5 м")), ((p - 0.5) * 2).roundToInt()) { setP(0.5 + it * 0.5) }
            releaseButton()
            val s = result
            if (s != null && phase == SeePhase.DOWN) {
                Readout(sumText(s))
                SmallText(if (s > 0) L("Převážilo levé závaží (↺). Posuň závaží a zkus to znovu.", "Перевесил левый груз (↺). Передвинь груз и попробуй снова.")
                else L("Převážilo pravé závaží (↻). Posuň závaží a zkus to znovu.", "Перевесил правый груз (↻). Передвинь груз и попробуй снова."))
            } else if (phase == SeePhase.DOWN || phase == SeePhase.LEVEL) {
                SmallText(L("Posuň závaží – objeví se nová podepřená houpačka.", "Передвинь груз — появятся новые качели на подпорках."))
            }
        },
        Step(
            L("Kolo 2: vlevo 3 kN ve 2 m. Kam od osy patří závaží 4 kN?", "Раунд 2: слева 3 кН на 2 м. На каком расстоянии от оси ставить груз 4 кН?"), level1,
            result = { Note(Tone.OK, L(
                "Ano: 3 · 2 = 4 · 1,5. Těžší závaží stačí kratší rameno – rozhoduje součin síla × rameno, ne síla sama.",
                "Да: 3 · 2 = 4 · 1,5. Более тяжёлому грузу хватает меньшего плеча — решает произведение сила × плечо, а не сама сила.",
            )) },
        ) {
            NumField(
                label = rich("[[r]]"),
                value = input,
                onChange = { v ->
                    input = v
                    inState = null
                    inMsg = null
                    // jiná hodnota = nový pokus: nová podepřená houpačka
                    if (phase == SeePhase.DOWN) fresh()
                },
                state = inState,
                suffix = t.pick(L("m", "м")),
                onDone = { place() },
            )
            ActionButton(L("Postav a pusť", "Поставить и отпустить"), enabled = round == 1 && phase != SeePhase.FALLING && !resetting) { place() }
            inMsg?.let { SmallText(it) }
            val s = result
            if (s != null && phase == SeePhase.DOWN && round == 1) {
                Readout(sumText(s))
                SmallText(
                    if (misses >= 2) L("Momenty se mají rovnat: 3 · 2 = 4 · [[r]].", "Моменты должны быть равны: 3 · 2 = 4 · [[r]].")
                    else if (s > 0) L("Převážilo levé závaží – rameno vpravo je krátké.", "Перевесил левый груз — правое плечо коротковато.")
                    else L("Převážilo pravé závaží – rameno vpravo je dlouhé.", "Перевесил правый груз — правое плечо длинновато."),
                )
            }
        },
    )
}

/** Stav páky: leží (kámen na zemi), zvedá se, dosedla (dlouhý konec na zemi). */
private enum class LeverPhase { REST, LIFTING, UP }

/**
 * Karta 1: Archimédova páka. Kámen 4 kN leží na zemi, pod ním konec tyče 5,5 m, podpěra jde posunout.
 * Archimédés tlačí na dlouhý konec 500 N. Když jeho moment k podpěře nestačí, nic se nehne (kámen drží zem);
 * když stačí, páka se roztáčí, dokud dlouhý konec nedosedne na zem. Pak zlaté pravidlo (síla × dráha)
 * a nakonec Země: poměr ramen 10²³ a cena za něj ve vzdálenosti.
 */
@Composable
private fun CardScope.Lever() {
    val t = tr()
    val knm = t.pick(L("kNm", "кН·м"))
    val len = 5.5 // délka tyče, m
    val unit = 65.0 // px na metr vodorovně
    val vz = 2.0 // svislé zvětšení (výšky jsou jen desítky cm)
    val x0 = 20.0
    val gy = 198.0
    val hf = 0.1 / 1.1 // výška podpěry: při rameni 0,5 m se kámen zvedne právě o 10 cm
    val stoneF = 4.0
    val handF = 0.5
    var a by remember { mutableDoubleStateOf(2.0) } // rameno kamene (od konce pod kamenem k podpěře)
    var tip by remember { mutableStateOf<Int?>(null) }
    var phase by remember { mutableStateOf(LeverPhase.REST) }
    var s by remember { mutableDoubleStateOf(0.0) } // 0 = kámen na zemi, 1 = dlouhý konec na zemi
    var pushed by remember { mutableStateOf<Double?>(null) } // a posledního neúspěšného zatlačení
    var lifted by remember { mutableStateOf(false) }
    var runId by remember { mutableIntStateOf(0) }
    val fade = remember { Animatable(1f) }
    var resetting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val b = len - a
    val ms = stoneF * a
    val mh = handF * b
    val arm = rememberNum(
        L("Kámen 4 kN na rameni 0,5 m. Jak dlouhé rameno potřebuje ruka 500 N, aby páku udržela?", "Камень 4 кН на плече 0,5 м. Какое плечо нужно руке 500 Н, чтобы удержать рычаг?"),
        listOf(NumSpec(L("[[r]]", "[[r]]"), 4.0, t.pick(L("m", "м")))),
        L("4 · 0,5 = 0,5 · [[r]] → [[r]] = 2 / 0,5 = **4 m**. Síla je 8× menší, takže rameno musí být 8× delší. S 5 m už ruka kámen zvedne.",
            "4 · 0,5 = 0,5 · [[r]] → [[r]] = 2 / 0,5 = **4 м**. Сила в 8 раз меньше, значит плечо должно быть в 8 раз длиннее. С 5 м рука уже поднимает камень."),
        listOf(
            NumTrap.near(L("To je jen poměr sil 4 / 0,5. Rovnají se ale **momenty**: 4 · 0,5 = 0,5 · [[r]].", "Это только отношение сил 4 / 0,5. Равны же **моменты**: 4 · 0,5 = 0,5 · [[r]]."), 8.0),
            NumTrap.near(L("Obráceně – menší síla potřebuje **delší** rameno.", "Наоборот — меньшей силе нужно **более длинное** плечо."), 0.0625),
        ),
        L("Momenty k podpěře se musí rovnat: síla kamene × jeho rameno = síla ruky × [[r]].", "Моменты относительно опоры должны быть равны: сила камня × его плечо = сила руки × [[r]]."),
    )
    val gold = rememberQuiz(
        L("Kámen se zvedl o 10 cm. O kolik musela ruka sjet dolů?", "Камень поднялся на 10 см. На сколько должна была опуститься рука?"),
        listOf(
            Opt(L("o 10 cm", "на 10 см"), why = L("Konec u ruky je 10× dál od podpěry – otáčí se o stejný úhel, ale opisuje 10× delší oblouk.", "Конец у руки в 10 раз дальше от опоры — поворачивается на тот же угол, но проходит дугу в 10 раз длиннее.")),
            Opt(L("o 1 m", "на 1 м"), correct = true),
            Opt(L("o 1 cm", "на 1 см"), why = L("Ruka tlačí 8× slaběji – něco za to musí zaplatit. Platí dráhou, ne úsporou dráhy.", "Рука давит в 8 раз слабее — за это надо чем-то платить. Платит путём, а не экономией пути.")),
        ),
        L("Rameno ruky (5 m) je 10× delší než rameno kamene (0,5 m), takže ruka ujede 10× delší dráhu: **1 m**. To je **zlaté pravidlo mechaniky**: co páka ušetří na síle, to zaplatíš dráhou. Síla × dráha (práce) vyjde na obou stranách skoro stejně.",
            "Плечо руки (5 м) в 10 раз длиннее плеча камня (0,5 м), поэтому рука проходит путь в 10 раз длиннее: **1 м**. Это **золотое правило механики**: что рычаг даёт в силе, то теряешь в пути. Сила × путь (работа) с обеих сторон почти одинаковы."),
    )
    val earth = rememberQuiz(
        L("Země by na zemském povrchu vážila asi 6·10²⁵ N, Archimédés tlačí 500 N. Kolikrát delší musí mít rameno než Země?", "Земля на поверхности Земли весила бы около 6·10²⁵ Н, Архимед давит с силой 500 Н. Во сколько раз его плечо должно быть длиннее, чем у Земли?"),
        listOf(
            Opt(L("asi 1 000×", "примерно в 1 000 раз"), why = L("Poměr ramen = poměr sil: 6·10²⁵ / 500. Kolik je to řádů?", "Отношение плеч = отношению сил: 6·10²⁵ / 500. Сколько это порядков?")),
            Opt(L("asi 10¹²×", "примерно в 10¹² раз"), why = L("Ještě víc: 6·10²⁵ / 5·10² = 1,2·10²³.", "Ещё больше: 6·10²⁵ / 5·10² = 1,2·10²³.")),
            Opt(L("asi 10²³×", "примерно в 10²³ раз"), correct = true),
        ),
        L("6·10²⁵ / 500 ≈ **10²³**. S ramenem Země 1 m by Archimédovo rameno měřilo asi 10²³ m – zhruba **13 milionů světelných let**, dál než galaxie v Andromedě. A aby Zemi zvedl o 1 cm, musel by konec páky stlačit o 10²¹ m: i rychlostí světla by to trvalo přes **100 000 let**. Princip platí – páka dá libovolně velkou sílu –, jen ji zaplatíš obrovskou dráhou. (Země ve vesmíru „neváží“ nic; počítáme, jako by ji zvedal na zemském povrchu.)",
            "6·10²⁵ / 500 ≈ **10²³**. При плече Земли 1 м плечо Архимеда было бы около 10²³ м — примерно **13 миллионов световых лет**, дальше галактики Андромеды. А чтобы поднять Землю на 1 см, ему пришлось бы опустить конец рычага на 10²¹ м: даже со скоростью света это заняло бы больше **100 000 лет**. Принцип верен — рычаг даёт сколь угодно большую силу, — но платишь за неё огромным путём. (В космосе Земля ничего «не весит»; считаем, будто он поднимает её у поверхности Земли.)"),
    )
    val space = arm.solved && gold.solved
    fun setA(v: Double) {
        if (phase == LeverPhase.LIFTING || resetting || v == a) return
        pushed = null
        if (phase == LeverPhase.REST) {
            a = v
            return
        }
        // zvednutá páka nezmizí sama od sebe: stará zmizí, nová leží s kamenem na zemi
        scope.launch {
            resetting = true
            fade.animateTo(0f, tween(180))
            s = 0.0
            phase = LeverPhase.REST
            a = v
            resetting = false
            fade.animateTo(1f, tween(260))
        }
    }
    fun push() {
        if (phase != LeverPhase.REST || resetting) return
        if (mh <= ms) {
            pushed = a
            return
        }
        pushed = null
        phase = LeverPhase.LIFTING
        runId++
    }
    LaunchedEffect(runId) {
        if (runId == 0) return@LaunchedEffect
        // úhlové zrychlení úměrné převaze momentu ruky; konec se zastaví o zem
        val k = (mh - ms) * 2.0
        var time = 0.0
        var last = 0L
        while (s < 1.0) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.0 else (now - last) / 1e9
                last = now
                time += dt
                s = (0.5 * k * time * time).coerceAtMost(1.0)
            }
        }
        phase = LeverPhase.UP
        if (a == 0.5) lifted = true
    }
    Stage(h = 230.0, description = L("Páka s podpěrou, kámen a Archimédés", "Рычаг с опорой, камень и Архимед").t()) { c ->
        if (space) {
            // vesmír: Země na krátkém rameni, dlouhé rameno mizí za okrajem
            rect(0.0, 0.0, 400.0, 230.0, Color(0xFF0B1324))
            for (i in 0 until 40) circle((i * 97 % 400).toDouble(), (i * 53 % 230).toDouble(), if (i % 7 == 0) 1.6 else 0.9, Color.White, alpha = 0.7f)
            circle(70.0, 118.0, 34.0, Color(0xFF2E7BD6), Color(0xFF8CC4FF), 1.2)
            poly(listOf(64.0 to 100.0, 80.0 to 96.0, 86.0 to 112.0, 72.0 to 124.0, 60.0 to 116.0), Color(0xFF3FA45B))
            poly(listOf(108.0 to 158.0, 98.0 to 178.0, 118.0 to 178.0), Color(0xFFB0B8C8), Color.White, 1.0)
            line(70.0, 154.0, 400.0, 160.0, Color(0xFFB0B8C8), 3.0)
            line(330.0, 160.0, 400.0, 161.0, Color(0xFF0B1324), 3.2, dash = true)
            text(110.0, 196.0, t.pick(L("pevný bod", "точка опоры")), Color.White, 12.0)
            text(70.0, 66.0, t.pick(L("Země 6·10²⁵ N", "Земля 6·10²⁵ Н")), Color.White, 13.0)
            text(250.0, 142.0, t.pick(if (earth.solved) L("→ rameno ≈ 10²³ m → … Archimédés", "→ плечо ≈ 10²³ м → … Архимед") else L("→ rameno Archiméda = ?", "→ плечо Архимеда = ?")), Color(0xFFFFD27A), 13.0)
            text(200.0, 24.0, t.pick(L("„Dejte mi pevný bod a pohnu Zemí.“", "«Дайте мне точку опоры, и я сдвину Землю».")), Color.White, 14.0, bold = true)
            return@Stage
        }
        val al = fade.value
        val xf = x0 + a * unit
        val xr = x0 + len * unit
        fun py(hm: Double) = gy - hm * unit * vz
        // tan sklonu: od kamene na zemi (s = 0) po dlouhý konec na zemi (s = 1)
        val tg = hf / a + s * (-hf / b - hf / a)
        val hl = hf - a * tg
        val hr = hf + b * tg
        hatch(0.0, 400.0, gy, c, Anim.None, 1f)
        // podpěra
        poly(listOf(xf to py(hf), xf - 11 to gy, xf + 11 to gy), c.paper, c.ink, 1.5, al)
        // tyč
        line(x0, py(hl), xr, py(hr), c.ink, 5.0, alpha = al)
        line(x0, py(hl), xr, py(hr), c.beam, 3.0, alpha = al)
        // kámen leží na krátkém konci (nebo na zemi)
        val sb = py(hl) - 2
        poly(listOf(x0 - 4 to sb, x0 + 50 to sb, x0 + 46 to sb - 30, x0 + 30 to sb - 40, x0 + 6 to sb - 34), c.subtle, c.ink, 1.6, al)
        text(x0 + 24, sb - 14, "4 kN", c.load, 13.0, bold = true, alpha = al)
        // ruka tlačí na dlouhý konec
        val hy = py(hr)
        force(xr - 4, hy - 48, xr - 4, hy - 6, c.load, alpha = al)
        text(xr - 10, hy - 36, "500 N", c.load, 13.0, Anchor.END, alpha = al)
        // ramena
        text((x0 + xf) / 2, gy + 20, "${num(a)} m", c.react, 12.0, alpha = al)
        text((xf + xr) / 2, gy + 20, "${num(b)} m", c.react, 12.0, alpha = al)
        if (phase == LeverPhase.UP && a == 0.5) {
            text(x0 + 60, sb - 22, t.pick(L("↑ 10 cm", "↑ 10 см")), c.accent, 13.0, Anchor.START, bold = true)
            text(xr - 14, gy - 12, t.pick(if (gold.solved) L("↓ 1 m", "↓ 1 м") else L("↓ ?", "↓ ?")), c.accent, 13.0, Anchor.END, bold = true)
        }
        val head = when {
            phase == LeverPhase.LIFTING -> L("zvedá se…", "поднимается…")
            phase == LeverPhase.UP -> L("kámen zvednut – konec páky dosedl na zem", "камень поднят — конец рычага упёрся в землю")
            pushed == a -> L("ani se nehne – kámen drží zem", "даже не шелохнулся — камень лежит на земле")
            else -> L("svisle 2× zvětšeno", "по вертикали увеличено в 2 раза")
        }
        text(200.0, 18.0, t.pick(head), if (phase == LeverPhase.UP) c.accent else c.inkSoft, 13.0, bold = phase == LeverPhase.UP)
    }
    CardText(L(
        "Archimédés prý řekl: **„Dejte mi pevný bod a pohnu Zemí.“** Zkusme menší úkol: tlačí na **páku** silou 500 N a chce zvednout kámen 4 kN – osmkrát těžší, než sám zvládne.",
        "Архимед, по преданию, сказал: **«Дайте мне точку опоры, и я сдвину Землю».** Начнём с задачи поменьше: он давит на **рычаг** с силой 500 Н и хочет поднять камень 4 кН — в восемь раз тяжелее, чем осилит сам.",
    ))
    SmallText(L("Páka je tuhá tyč otočná kolem podpěry. Rozhoduje moment k podpěře: síla × rameno.", "Рычаг — жёсткий стержень, поворачивающийся вокруг опоры. Решает момент относительно опоры: сила × плечо."))
    val tipOpts = listOf(L("blízko kamene", "ближе к камню"), L("doprostřed", "посередине"), L("blízko ruky", "ближе к руке"))
    Steps(
        tipStep(L("Kam dát podpěru, aby 500 N kámen zvedlo?", "Куда поставить опору, чтобы 500 Н подняли камень?"), tipOpts, tip) { tip = it },
        Step(
            L("Posouvej podpěru a zatlač, dokud kámen nezvedneš", "Передвигай опору и дави, пока не поднимешь камень"), lifted,
            result = { Note(if (tip == 0) Tone.OK else Tone.INFO, L(
                (if (tip == 0) "Tip sedí. " else "") + "Podpěra 0,5 m od kamene: kámen 4 · 0,5 = 2 kNm, ruka 0,5 · 5 = 2,5 kNm. Moment ruky převážil – i když je síla 8× menší, má 10× delší rameno.",
                (if (tip == 0) "Прогноз верный. " else "") + "Опора в 0,5 м от камня: камень 4 · 0,5 = 2 кН·м, рука 0,5 · 5 = 2,5 кН·м. Момент руки перевесил — сила в 8 раз меньше, зато плечо в 10 раз длиннее.",
            )) },
        ) {
            Chips(listOf(L("podpěra 2 m od kamene", "опора в 2 м от камня"), L("1 m", "1 м"), L("0,5 m", "0,5 м")), when (a) { 2.0 -> 0; 1.0 -> 1; else -> 2 }) { setA(listOf(2.0, 1.0, 0.5)[it]) }
            ActionButton(L("Zatlač 500 N", "Надавить 500 Н"), enabled = phase == LeverPhase.REST && !resetting) { push() }
            if (pushed == a && phase == LeverPhase.REST) {
                Readout("${num(handF)} · ${num(b)} = ${num(mh)} < ${num(stoneF)} · ${num(a)} = ${num(ms)} $knm")
                SmallText(L("Moment ruky je menší než moment kamene. Zkrať rameno kamene.", "Момент руки меньше момента камня. Укороти плечо камня."))
            }
        },
        Step(L("Spočítej rameno, které ruce stačí", "Посчитай плечо, которого руке достаточно"), arm.solved, result = { arm.Feedback() }) {
            Text(rich(L("Kámen 4 kN na rameni 0,5 m. Jak dlouhé rameno potřebuje ruka 500 N, aby páku udržela?", "Камень 4 кН на плече 0,5 м. Какое плечо нужно руке 500 Н, чтобы удержать рычаг?").t()), style = MaterialTheme.typography.bodyMedium, color = LocalColors.current.ink)
            arm.Fields()
            arm.Buttons()
            if (!arm.solved) arm.Feedback()
        },
        Step(L("Zlaté pravidlo: o kolik sjela ruka?", "Золотое правило: на сколько опустилась рука?"), gold.solved, result = { gold.Feedback() }) {
            Text(rich(gold.question.t()), style = MaterialTheme.typography.bodyMedium, color = LocalColors.current.ink)
            gold.Options(3)
            if (!gold.solved) gold.Feedback()
        },
        Step(L("A teď Země: jak dlouhou páku by potřeboval?", "А теперь Земля: какой длины нужен рычаг?"), earth.solved, result = { earth.Feedback() }) {
            Text(rich(earth.question.t()), style = MaterialTheme.typography.bodyMedium, color = LocalColors.current.ink)
            earth.Options(1)
            if (!earth.solved) earth.Feedback()
        },
    )
}

/** Vor na klidné vodě shora: jeden nebo dva lodní motory, motor 2 jde posunout na jinou nositelku. */
@Composable
private fun CardScope.Raft() {
    val t = tr()
    val knm = t.pick(L("kNm", "кН·м"))
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
    var lastD by remember { mutableDoubleStateOf(0.0) }
    val couple = both && abs(dm) > 1e-9
    // 0 jeden motor, 1 tip, 2 oba motory, 3 jedna nositelka, 4 hotovo
    val stage = when {
        !ranOne -> 0
        guess == null -> 1
        !ranCouple -> 2
        !ranRest -> 3
        else -> 4
    }
    val canMove = stage >= 3
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
    // po prvním pokusu se k voru přidá motor 2 (nový vor prolnutím)
    LaunchedEffect(ranOne) {
        if (!ranOne || both) return@LaunchedEffect
        delay(1200)
        fresh { both = true }
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
        lastD = dm
        when (kind) {
            0 -> ranOne = true
            1 -> ranCouple = true
            else -> if (ranCouple) ranRest = true
        }
        // pokus skončil: vor pomalu zmizí, stopa zůstane
        fade.animateTo(0.3f, tween(600))
    }
    fun start() {
        if (running) return
        fresh {
            running = true
            ended = false
            runId++
        }
    }
    fun moveMotor(nd: Double) {
        if (running || !canMove) return
        val v = nd.coerceIn(-1.2, 1.2)
        if (abs(v - dm) > 1e-9) fresh { dm = v }
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
                dragging = both && canMove && !running && p.dist(cx0 + w / 2 + 84, cy0 + dm * perM) < 40
            }
            if (dragging) moveMotor(round((p.y - cy0) / perM / 0.6) * 0.6)
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
                if (canMove) handle(w / 2 + cx0 + 84, cy0 + yy, c, dragging)
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
        "Vor na klidné vodě, pohled shora; odpor vody zanedbáme. Každý lodní motor tlačí silou 0,5 kN. Stačí ΣF = 0 k tomu, aby vor zůstal v klidu?",
        "Плот на спокойной воде, вид сверху; сопротивлением воды пренебрегаем. Каждый лодочный мотор толкает с силой 0,5 кН. Достаточно ли ΣF = 0, чтобы плот оставался в покое?",
    ))
    val momentLine = "M = 0,5 · ${num(abs(lastD))} = ${num(force * abs(lastD))} $knm"
    val tipOpts = listOf(L("zůstane stát", "останется на месте"), L("pojede rovně", "поедет прямо"), L("bude se točit", "будет вращаться"))
    Steps(
        Step(
            L("Zapni jen motor 1 a sleduj, co vor udělá", "Включи только мотор 1 и посмотри, что сделает плот"), ranOne,
            result = { Note(Tone.INFO, L(
                "Jedna nevyrovnaná síla vor **rozjíždí** – obrysy se od sebe vzdalují čím dál víc. Její nositelka prochází těžištěm T, proto se vor netočí.",
                "Одна неуравновешенная сила **разгоняет** плот — контуры расходятся всё сильнее. Её линия действия проходит через центр тяжести T, поэтому плот не вращается.",
            )) },
        ) { ActionButton(L("Zapni motor 1", "Включить мотор 1"), enabled = !running) { start() } },
        Step(
            L("Přidáme stejný motor 2 proti prvnímu, ale níž. Co vor udělá?", "Добавим такой же мотор 2 навстречу первому, но ниже. Что сделает плот?"), guess != null,
            result = { guess?.let { SmallText(L("Tvůj tip: **${tipOpts[it].cs}**", "Твой прогноз: **${tipOpts[it].ru}**")) } },
        ) {
            Readout("ΣF = 0,5 − 0,5 = 0")
            Chips(tipOpts, guess) { if (guess == null) guess = it }
        },
        Step(
            L("Zapni oba motory a ověř svůj tip", "Включи оба мотора и проверь свой прогноз"), ranCouple,
            result = {
                Note(if (guess == 2) Tone.OK else Tone.INFO, L(
                    (if (guess == 2) "Tip sedí. " else "Tip nesedí. ") + "ΣF = 0, takže těžiště T zůstalo na místě – ale síly leží na **různých** nositelkách. Tvoří dvojici s momentem M = 0,5 · 1,2 = 0,6 kNm a vor se roztáčí čím dál rychleji. **ΣF = 0 nestačí.**",
                    (if (guess == 2) "Прогноз верный. " else "Прогноз неверный. ") + "ΣF = 0, поэтому центр тяжести T остался на месте — но силы лежат на **разных** линиях действия. Они образуют пару с моментом M = 0,5 · 1,2 = 0,6 кН·м, и плот вращается всё быстрее. **ΣF = 0 недостаточно.**",
                ))
            },
        ) { ActionButton(L("Zapni oba motory", "Включить оба мотора"), enabled = !running && both) { start() } },
        Step(
            L("Posuň motor 2 tak, aby vor zůstal stát, a zapni motory", "Передвинь мотор 2 так, чтобы плот стоял, и включи моторы"), ranRest,
            result = {
                Note(Tone.OK, L(
                    "Teď leží obě síly na **jedné** nositelce: ΣF = 0 i ΣM = 0 – vor se nehne.",
                    "Теперь обе силы лежат на **одной** линии действия: ΣF = 0 и ΣM = 0 — плот не двигается.",
                ))
                Readout("Σ[[F_x]] = 0     Σ[[F_y]] = 0     ΣM = 0", ok = true)
                Note(Tone.OK, L(
                    "Těleso v rovině se může posunout vodorovně, posunout svisle a otočit. Aby zůstalo v klidu, musí platit všechny tři **rovnice rovnováhy** – nulový součet sil ještě neznamená nulový součet momentů.",
                    "Тело на плоскости может сдвинуться по горизонтали, по вертикали и повернуться. Чтобы оно оставалось в покое, должны выполняться все три **уравнения равновесия** — нулевая сумма сил ещё не означает нулевую сумму моментов.",
                ))
            },
        ) {
            Chips(listOf(L("motor 2 výš", "мотор 2 выше"), L("motor 2 níž", "мотор 2 ниже"))) { i -> moveMotor(dm + if (i == 0) -0.6 else 0.6) }
            ActionButton(L("Zapni oba motory", "Включить оба мотора"), enabled = !running) { start() }
            if (ended && lastKind == 1 && stage == 3) SmallText(L(
                "Pořád se točí: $momentLine. Nositelky jsou pořád od sebe.",
                "Всё ещё вращается: $momentLine. Линии действия всё ещё не совпадают.",
            ))
        },
    )
}

/** Karta 2: výhodný bod pro momentovou rovnici – nejdřív [[R_B]], pak [[R_Ay]]; rovnici pak žák sám vyřeší. */
@Composable
private fun CardScope.ChoosePoint() {
    val t = tr()
    val kn = t.pick(L("kN", "кН"))
    val y = 100.0
    val xa = 80.0
    val u = 60.0
    val xb = xa + 4 * u
    val xs = xa + 2 * u
    val opts = { good: Int, whyA: L?, whyS: L, whyB: L? ->
        listOf(
            Opt(L("bod A", "точка A"), correct = good == 0, why = whyA),
            Opt(L("střed S", "середина S"), why = whyS),
            Opt(L("bod B", "точка B"), correct = good == 2, why = whyB),
        )
    }
    val qB = rememberQuiz(
        L("Chceš hned [[R_B]]. Ke kterému bodu napíšeš ΣM = 0?", "Нужна сразу [[R_B]]. Относительно какой точки запишешь ΣM = 0?"),
        opts(
            0, null,
            L("Σ[[M_S]]: −[[R_Ay]]·2 + 8·1 + [[R_B]]·2 = 0 – dvě neznámé v jedné rovnici, z ní samotné nic nespočítáš.", "Σ[[M_S]]: −[[R_Ay]]·2 + 8·1 + [[R_B]]·2 = 0 — две неизвестные в одном уравнении, из него одного ничего не найти."),
            L("Σ[[M_B]]: −[[R_Ay]]·4 + 8·3 = 0 – vypadla právě [[R_B]], kterou hledáš. Který bod vyřadí obě ostatní neznámé?", "Σ[[M_B]]: −[[R_Ay]]·4 + 8·3 = 0 — выпала как раз [[R_B]], которую ты ищешь. Какая точка уберёт обе другие неизвестные?"),
        ),
        L(
            "Σ[[M_A]] = 0:  −8 · 1 + [[R_B]] · 4 = 0. Nositelky [[R_Ax]] i [[R_Ay]] procházejí bodem A – mají rameno 0 a z rovnice vypadnou. Zbyla jediná neznámá [[R_B]].",
            "Σ[[M_A]] = 0:  −8 · 1 + [[R_B]] · 4 = 0. Линии действия [[R_Ax]] и [[R_Ay]] проходят через A — их плечо 0, и из уравнения они выпадают. Осталась одна неизвестная [[R_B]].",
        ),
    )
    val qA = rememberQuiz(
        L("Teď chceš hned [[R_Ay]]. Který bod vybereš?", "Теперь нужна сразу [[R_Ay]]. Какую точку выберешь?"),
        opts(
            2,
            L("Σ[[M_A]]: −8·1 + [[R_B]]·4 = 0 – vypadla právě [[R_Ay]], kterou hledáš. Kterým bodem prochází nositelka druhé svislé neznámé?", "Σ[[M_A]]: −8·1 + [[R_B]]·4 = 0 — выпала как раз [[R_Ay]], которую ты ищешь. Через какую точку проходит линия действия другой вертикальной неизвестной?"),
            L("Ke středu mají rameno [[R_Ay]] i [[R_B]] – dvě neznámé v jedné rovnici.", "Относительно середины плечо есть и у [[R_Ay]], и у [[R_B]] — две неизвестные в одном уравнении."),
            null,
        ),
        L(
            "Σ[[M_B]] = 0:  −[[R_Ay]] · 4 + 8 · 3 = 0. Nositelka [[R_B]] prochází bodem B, takže [[R_B]] z rovnice vypadne; [[R_Ax]] leží v ose nosníku a bodem B prochází taky.",
            "Σ[[M_B]] = 0:  −[[R_Ay]] · 4 + 8 · 3 = 0. Линия действия [[R_B]] проходит через B, поэтому [[R_B]] из уравнения выпадает; [[R_Ax]] лежит на оси балки и тоже проходит через B.",
        ),
    )
    val solveB = numStep(
        question = L("−8 · 1 + [[R_B]] · 4 = 0. Kolik je [[R_B]]?", "−8 · 1 + [[R_B]] · 4 = 0. Чему равна [[R_B]]?"),
        label = L("Vyřeš rovnici: kolik je [[R_B]]?", "Реши уравнение: чему равна [[R_B]]?"),
        fields = listOf(NumSpec(L("[[R_B]]", "[[R_B]]"), 2.0, kn)),
        explain = L("[[R_B]] · 4 = 8, tedy [[R_B]] = 2 kN.", "[[R_B]] · 4 = 8, то есть [[R_B]] = 2 кН."),
        traps = listOf(
            NumTrap.near(L("Ramenem se dělí: [[R_B]] · 4 = 8.", "На плечо нужно делить: [[R_B]] · 4 = 8."), 32.0),
            NumTrap.near(L("To je celá síla F. Rovnice ale říká [[R_B]] · 4 = 8 · 1.", "Это вся сила F. А уравнение говорит [[R_B]] · 4 = 8 · 1."), 8.0),
        ),
    )
    val solveA = numStep(
        question = L("−[[R_Ay]] · 4 + 8 · 3 = 0. Kolik je [[R_Ay]]?", "−[[R_Ay]] · 4 + 8 · 3 = 0. Чему равна [[R_Ay]]?"),
        label = L("Vyřeš rovnici: kolik je [[R_Ay]]?", "Реши уравнение: чему равна [[R_Ay]]?"),
        fields = listOf(NumSpec(L("[[R_Ay]]", "[[R_Ay]]"), 6.0, kn)),
        explain = L(
            "[[R_Ay]] · 4 = 24, tedy [[R_Ay]] = 6 kN. Kontrola: [[R_Ay]] + [[R_B]] = 6 + 2 = 8 kN = F ✓",
            "[[R_Ay]] · 4 = 24, то есть [[R_Ay]] = 6 кН. Проверка: [[R_Ay]] + [[R_B]] = 6 + 2 = 8 кН = F ✓",
        ),
        traps = listOf(
            NumTrap.near(L("Ramenem se dělí: [[R_Ay]] · 4 = 24.", "На плечо нужно делить: [[R_Ay]] · 4 = 24."), 96.0),
            NumTrap.near(L("To je moment 8 · 3. Ještě ho vyděl ramenem 4.", "Это момент 8 · 3. Его ещё нужно разделить на плечо 4."), 24.0),
            NumTrap.near(L("To je [[R_B]]. Teď hledáš svislou reakci v A.", "Это [[R_B]]. Теперь ищешь вертикальную реакцию в A."), 2.0),
        ),
    )
    // 0 bod pro R_B, 1 výpočet R_B, 2 bod pro R_Ay, 3 výpočet R_Ay
    val stage = when {
        !qB.solved -> 0
        !solveB.done -> 1
        !qA.solved -> 2
        else -> 3
    }
    val pick = if (stage <= 1) qB.picked else qA.picked
    val goal = if (stage <= 1) 0 else 1 // 0 = hledáme R_B, 1 = R_Ay
    val pxs = listOf(xa, xs, xb)
    Stage(
        h = 220.0,
        description = L("Prostý nosník s reakcemi a volbou bodu", "Простая балка с реакциями и выбором точки").t(),
        onTap = { pt ->
            val q = when (stage) {
                0 -> qB
                2 -> qA
                else -> return@Stage
            }
            val i = pxs.indices.minByOrNull { abs(pt.x - pxs[it]) } ?: return@Stage
            if (abs(pt.x - pxs[i]) < 40 && abs(pt.y - y) < 60) q.pick(i)
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
    fun quizAsStep(q: QuizState) = Step(q.question, q.solved, result = { q.Feedback() }) {
        q.Options(columns = 3)
        if (!q.solved) q.Feedback()
    }
    Steps(quizAsStep(qB), solveB, quizAsStep(qA), solveA)
}

/**
 * Karta 3: kontrola. Jiný nosník a obrácená otázka než na kartě 2: k danému bodu – která neznámá vyjde?
 * Pak žák sám (bez nabídnutého bodu i rovnice) spočítá [[R_Ay]].
 */
@Composable
private fun CardScope.Check() {
    val t = tr()
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
    CardText(L(
        "Nosník o rozpětí 4 m, síla 6 kN působí 1 m od podpory B. Podpora A má reakce [[R_Ax]], [[R_Ay]], podpora B jen [[R_B]].",
        "Балка пролётом 4 м, сила 6 кН приложена в 1 м от опоры B. У опоры A реакции [[R_Ax]], [[R_Ay]], у опоры B — только [[R_B]].",
    ))
    Steps(
        quizStep(
            question = L("Spolužák napsal ΣM = 0 k bodu A. Kterou reakci z ní hned spočítá?", "Одноклассник записал ΣM = 0 относительно точки A. Какую реакцию он из него сразу найдёт?"),
            options = listOf(
                Opt(L("[[R_Ay]]", "[[R_Ay]]"), why = L("Nositelka [[R_Ay]] prochází bodem A – rameno 0. Objeví se pak [[R_Ay]] v rovnici vůbec?", "Линия действия [[R_Ay]] проходит через точку A — плечо 0. Войдёт ли тогда [[R_Ay]] в уравнение вообще?")),
                Opt(L("[[R_B]]", "[[R_B]]"), correct = true),
                Opt(L("[[R_Ax]]", "[[R_Ax]]"), why = L("[[R_Ax]] leží v ose nosníku a bodem A prochází – rameno 0, z rovnice vypadne.", "[[R_Ax]] лежит на оси балки и проходит через A — плечо 0, из уравнения выпадает.")),
            ),
            explain = L(
                "Bodem A procházejí [[R_Ax]] i [[R_Ay]], v rovnici zbude jen [[R_B]]: −6 · 3 + [[R_B]] · 4 = 0.",
                "Через A проходят и [[R_Ax]], и [[R_Ay]], в уравнении остаётся только [[R_B]]: −6 · 3 + [[R_B]] · 4 = 0.",
            ),
            columns = 3,
        ),
        numStep(
            question = L("Teď spočítej [[R_Ay]] – bod si vyber sám", "Теперь найди [[R_Ay]] — точку выбери сам"),
            fields = listOf(NumSpec(L("[[R_Ay]]", "[[R_Ay]]"), 1.5, t.pick(L("kN", "кН")))),
            explain = L(
                "K bodu B: −[[R_Ay]] · 4 + 6 · 1 = 0, tedy [[R_Ay]] = 1,5 kN. Kontrola: [[R_B]] = 6 · 3 / 4 = 4,5 kN a 1,5 + 4,5 = 6 kN = F ✓",
                "Относительно B: −[[R_Ay]] · 4 + 6 · 1 = 0, то есть [[R_Ay]] = 1,5 кН. Проверка: [[R_B]] = 6 · 3 / 4 = 4,5 кН и 1,5 + 4,5 = 6 кН = F ✓",
            ),
            traps = listOf(
                NumTrap.near(L("To je reakce v B (z rovnice k bodu A). Hledáš ale svislou reakci v A.", "Это реакция в B (из уравнения относительно A). А ищешь вертикальную реакцию в A."), 4.5),
                NumTrap.near(L("Celou sílu nese A, jen když působí přímo nad ní. Tady je síla blíž k B – a nezapomeň dělit ramenem 4 m.", "Всю силу опора A несёт, только если та приложена прямо над ней. Здесь сила ближе к B — и не забудь разделить на плечо 4 м."), 6.0),
                NumTrap.near(L("To je moment 6 · 3 k bodu A. Ještě ho vyděl ramenem – a je to reakce v B, ne v A.", "Это момент 6 · 3 относительно A. Его ещё нужно разделить на плечо — и это реакция в B, а не в A."), 18.0),
            ),
            nudge = L(
                "Napiš ΣM = 0 k bodu B – tam vypadne [[R_B]]. Nebo dopočítej [[R_B]] z rovnice k bodu A a použij Σ[[F_y]] = 0.",
                "Запиши ΣM = 0 относительно B — там выпадет [[R_B]]. Или найди [[R_B]] из уравнения относительно A и воспользуйся Σ[[F_y]] = 0.",
            ),
        ),
    )
}


internal fun lesson5() = Lesson(
    id = 5,
    title = L("Rovnováha otáčení", "Равновесие моментов"),
    summary = L("ΣM = 0, Archimédova páka, proč ΣF = 0 nestačí a jak vybrat výhodný bod.", "ΣM = 0, рычаг Архимеда, почему ΣF = 0 недостаточно и как выбрать удобную точку."),
    cards = listOf(
        Card { Seesaw() },
        Card { Lever() },
        Card { Raft() },
        Card { ChoosePoint() },
        Card { Check() },
    ),
)
