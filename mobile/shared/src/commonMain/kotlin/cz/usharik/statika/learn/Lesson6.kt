package cz.usharik.statika.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.Anim
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Lekce 6 – Vazby brání pohybu. Myšlenka: pohybu, který vazba nedovolí, brání reakcí (proti pokusu).
 * Každý zakázaný pohyb (posun ⟷, zvednutí ↕, otočení ↻) = jedna neznámá reakce.
 */

private enum class Motion { SLIDE, LIFT, ROTATE }

private val TYPES = listOf(SupportType.PIN, SupportType.ROLLER, SupportType.FIXED)

/** Které pohyby vazba nedovolí (oboustranná vazba: zvednutí brání i válečky). */
private fun blocks(t: SupportType, m: Motion): Boolean = when (t) {
    SupportType.PIN -> m != Motion.ROTATE
    SupportType.ROLLER -> m == Motion.LIFT
    SupportType.FIXED -> true
}

private fun rot6(x: Double, y: Double, px: Double, py: Double, deg: Double): Pair<Double, Double> {
    val a = deg * PI / 180
    val dx = x - px
    val dy = y - py
    return (px + dx * cos(a) - dy * sin(a)) to (py + dx * sin(a) + dy * cos(a))
}

private fun PrimBuilder.movedBeam(x1: Double, x2: Double, y: Double, dx: Double, dy: Double, px: Double, deg: Double, c: StatikaColors) {
    val pts = listOf(x1 to y - 5, x2 to y - 5, x2 to y + 5, x1 to y + 5).map { (x, yy) ->
        val (rx, ry) = rot6(x, yy, px, y, deg)
        (rx + dx) to (ry + dy)
    }
    poly(pts, c.beam, c.ink, 1.6)
}

/**
 * Posuvná podpora kreslená zvlášť: trojúhelník s válečky na [x], kolejnice (pevně na [gx]) a pod ní ground.
 * Válečky jsou ve vedení zajištěné sponami pod kolejnicí – nosník po kolejnici jezdí, ale nadzvednout nejde.
 */
private fun PrimBuilder.rollerAt(x: Double, gx: Double, y: Double, c: StatikaColors, label: String, alpha: Float = 1f) {
    val top = y + 5
    val base = top + 4 + 20
    // kolejnice a podklad
    rect(gx - 70, base + 7, 120.0, 4.0, c.subtle, c.ink, 1.2, alpha)
    hatch(gx - 70, gx + 50, base + 22, c, Anim.None, alpha)
    for (sx in listOf(gx - 55, gx + 35)) line(sx, base + 11, sx, base + 22, c.ink, 1.2, alpha = alpha)
    // vozík: trojúhelník, válečky na kolejnici, spony pod ní
    poly(listOf(x to top + 4, x - 13 to base, x + 13 to base), c.paper, c.ink, 1.5, alpha)
    circle(x, top + 4, 4.0, c.paper, c.ink, 1.5, alpha)
    circle(x - 7, base + 3.5, 3.2, c.paper, c.ink, 1.3, alpha)
    circle(x + 7, base + 3.5, 3.2, c.paper, c.ink, 1.3, alpha)
    for (sgn in listOf(-1.0, 1.0)) {
        val ex = x + sgn * 13
        line(ex, base, ex, base + 15, c.ink, 1.4, alpha = alpha)
        line(ex, base + 15, ex - sgn * 7, base + 15, c.ink, 1.4, alpha = alpha)
    }
    if (label.isNotEmpty()) text(x + 22, top + 22, label, c.ink, 16.0, Anchor.START, bold = true, alpha = alpha)
}

private val MOTION_NAMES = listOf(L("⟷ posun vodorovně", "⟷ сдвиг по горизонтали"), L("↕ posun svisle", "↕ сдвиг по вертикали"), L("↺ pootočení", "↺ поворот"))

/**
 * Karta 0: zkouška pohybu. Žák u každé podpory nejdřív tipne, které pohyby nedovolí, pak „Vyzkoušej“ provede
 * tři zkoušky: posun a zvednutí silou procházející podporou, pootočení čistým momentem. Dovolený pohyb ruka
 * provede a nosník tak zůstane; před další zkouškou se objeví nový. Zakázaný pohyb jen cukne a objeví se reakce.
 */
@Composable
private fun CardScope.PushTest() {
    val t = tr()
    var type by remember { mutableIntStateOf(0) }
    var pred by remember { mutableStateOf(mapOf<Int, Set<Motion>>()) }
    var motion by remember { mutableStateOf<Motion?>(null) } // právě zkoušený pohyb
    var pushing by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf(setOf<Motion>()) } // zakázané pohyby zjištěné při posledním běhu
    var shown by remember { mutableStateOf<Int?>(null) } // u které podpory se zkouška dokončila
    var okTypes by remember { mutableStateOf(setOf<Int>()) }
    var lastOk by remember { mutableStateOf<Boolean?>(null) }
    // posunutí nosníku po dovoleném pohybu (zůstane, dokud se neobjeví nový nosník)
    var disp by remember { mutableStateOf(Triple(0.0, 0.0, 0.0)) }
    val p = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    val st = TYPES[type]
    val fixed = st == SupportType.FIXED
    val y = 112.0
    val xs = 100.0
    val x1 = if (fixed) xs else xs - 10
    suspend fun freshBeam() {
        if (disp != Triple(0.0, 0.0, 0.0)) {
            fade.animateTo(0f, tween(160))
            disp = Triple(0.0, 0.0, 0.0)
            fade.animateTo(1f, tween(220))
        }
    }
    fun reset(then: () -> Unit) {
        job?.cancel()
        running = false
        pushing = false
        motion = null
        then()
        job = scope.launch {
            p.snapTo(0f)
            freshBeam()
            fade.snapTo(1f)
        }
    }
    fun run() {
        job?.cancel()
        val mine = pred[type] ?: emptySet()
        val tt = type
        running = true
        found = emptySet()
        shown = null
        lastOk = null
        job = scope.launch {
            for (m in Motion.entries) {
                freshBeam()
                motion = m
                pushing = true
                p.snapTo(0f)
                p.animateTo(1f, tween(850))
                pushing = false
                if (blocks(TYPES[tt], m)) found = found + m
                else disp = when (m) {
                    Motion.SLIDE -> Triple(-34.0, 0.0, 0.0)
                    Motion.LIFT -> Triple(0.0, -26.0, 0.0)
                    Motion.ROTATE -> Triple(0.0, 0.0, -14.0)
                }
                delay(450)
            }
            motion = null
            running = false
            shown = tt
            val actual = Motion.entries.filter { blocks(TYPES[tt], it) }.toSet()
            val ok = mine == actual
            lastOk = ok
            if (ok) okTypes = okTypes + tt
        }
    }

    Stage(h = 230.0, description = t.pick(L("Nosník na podpoře, zkouška pohybu", "Балка на опоре, проверка движения"))) { c ->
        val m = motion
        val v = p.value.toDouble()
        val a = fade.value
        val blocked = m != null && blocks(st, m)
        // zakázaný pohyb jen „cukne“, dovolený ruka plynule provede (rozjezd i dojezd)
        val shake = if (blocked && pushing) sin(v * PI * 5) * 3 * (1 - v) else 0.0
        val e = v * v * (3 - 2 * v)
        var dx = disp.first
        var dy = disp.second
        var deg = disp.third
        if (m != null && pushing) when (m) {
            Motion.SLIDE -> dx = if (blocked) -shake else -34 * e
            Motion.LIFT -> dy = if (blocked) -shake else -26 * e
            Motion.ROTATE -> deg = if (blocked) shake * 0.6 else -14 * e
        }
        when (st) {
            SupportType.ROLLER -> rollerAt(xs + dx, xs, y, c, "A", a) // vozík jede s nosníkem, kolejnice stojí
            SupportType.FIXED -> {
                support(st, xs, y, c, "", wallLeft = true)
                text(xs - 14, y - 42, "A", c.ink, 16.0, Anchor.END, bold = true)
            }
            else -> support(st, xs, y, c, "A")
        }
        val pts = listOf(x1 to y - 5, 340.0 to y - 5, 340.0 to y + 5, x1 to y + 5).map { (x, yy) ->
            val (rx, ry) = rot6(x, yy, xs, y, deg)
            (rx + dx) to (ry + dy)
        }
        poly(pts, c.beam, c.ink, 1.6, a)
        // červeně = zkouška (ruka): síla nositelkou přes podporu, nebo čistý moment
        if (m != null && pushing) {
            when (m) {
                Motion.SLIDE -> force(394.0 + dx, y, 346.0 + dx, y, c.load) // tlačí doleva v ose nosníku
                Motion.LIFT -> force(xs + if (fixed) 6.0 else 0.0, y - 7 + dy, xs + if (fixed) 6.0 else 0.0, y - 62 + dy, c.load) // táhne nahoru přímo nad podporou
                Motion.ROTATE -> moment(xs + 70, y + dy, 22.0, true, c.load) // natáčí ↺ (dvojice sil)
            }
        }
        // modře = reakce u zakázaných pohybů: proti zkoušce
        for (f in found) {
            when (f) {
                Motion.SLIDE -> if (fixed) {
                    force(xs + 4, y + 22, xs + 58, y + 22, c.react)
                    text(xs + 64, y + 27, null, c.react, 14.0, Anchor.START, sym = "R_x")
                } else {
                    force(x1 - 58, y, x1 - 2, y, c.react)
                    text(x1 - 32, y - 10, null, c.react, 14.0, sym = "R_x")
                }
                Motion.LIFT -> {
                    val lx = xs + if (fixed) 6.0 else 0.0
                    force(lx, y - 64, lx, y - 7, c.react)
                    if (fixed) text(lx + 8, y - 52, null, c.react, 14.0, Anchor.START, sym = "R_y")
                    else text(lx - 8, y - 50, null, c.react, 14.0, Anchor.END, sym = "R_y")
                }
                Motion.ROTATE -> {
                    moment(xs + 70, y, 22.0, false, c.react)
                    text(xs + 70, y - 32, null, c.react, 14.0, sym = "M")
                }
            }
        }
        text(392.0, 220.0, t.pick(L("zkouška pohybu – zatížení vynecháno", "проверка движения — нагрузка не показана")), c.inkSoft, 11.0, Anchor.END)
    }
    CardText(L(
        "Podpora nosníku něco dovolí a něco ne. Tipni, které pohyby tahle podpora **nedovolí**, a pak to vyzkoušej: ruka nosníkem posune, zvedne (vždy silou přímo přes podporu) a pootočí. Pohyb, který podpora nedovolí, zastaví silou – **reakcí** (modře), proti tvému pokusu.",
        "Опора что-то балке позволяет, а что-то нет. Угадай, какие движения эта опора **не допускает**, и проверь: рука сдвинет балку, поднимет её (всегда силой прямо через опору) и повернёт. Движение, которое опора не допускает, она останавливает силой — **реакцией** (синяя), против твоей попытки.",
    ))
    if (type == 1) SmallText(L(
        "Válečky posuvné podpory jsou v kolejnici zajištěné sponami (jako šuplík ve výsuvu): jezdí sem a tam, ale nahoru nosník nepustí.",
        "Катки подвижной опоры закреплены в рельсе скобами (как ящик стола в направляющих): ездят туда-сюда, но вверх балку не отпускают.",
    ))
    Todo(
        L("**Pevná kloubová** podpora: tip, který se při zkoušce potvrdí", "**Шарнирно-неподвижная** опора: прогноз, который подтвердится проверкой") to (0 in okTypes),
        L("**Posuvná kloubová** podpora: tip, který se potvrdí", "**Шарнирно-подвижная** опора: прогноз, который подтвердится") to (1 in okTypes),
        L("**Vetknutí**: tip, který se potvrdí", "**Жёсткая заделка**: прогноз, который подтвердится") to (2 in okTypes),
    )
    Chips(
        listOf(L("pevná kloubová", "шарнирно-неподвижная"), L("posuvná kloubová", "шарнирно-подвижная"), L("vetknutí", "жёсткая заделка")),
        type,
    ) { k ->
        reset {
            type = k
            found = emptySet()
            shown = null
            lastOk = null
        }
    }
    SmallText(L("Které pohyby podpora nedovolí? (vyber všechny)", "Какие движения опора не допускает? (выбери все)"))
    val mine = pred[type] ?: emptySet()
    Chips(MOTION_NAMES, selectedSet = mine.map { it.ordinal }.toSet()) { i ->
        if (running) return@Chips
        val m = Motion.entries[i]
        val now = if (m in mine) mine - m else mine + m
        reset {
            pred = pred + (type to now)
            found = emptySet()
            shown = null
            lastOk = null
        }
    }
    ActionButton(L("Vyzkoušej", "Проверить"), enabled = !running) { run() }
    if (shown == type && !running) {
        val actual = Motion.entries.filter { blocks(st, it) }
        Readout(t.pick(L("Zakázané pohyby: ${actual.size} → reakce: ${actual.size}", "Запрещено движений: ${actual.size} → реакций: ${actual.size}")), ok = lastOk == true)
        if (lastOk == true) Note(Tone.OK, when (type) {
            0 -> L("Tip sedí. Kloub nedovolí posun ani zvednutí – vzniknou dvě reakce [[R_x]], [[R_y]]. Pootočení dovolí, reakční moment tu není.", "Прогноз верный. Шарнир не допускает ни сдвига, ни подъёма — возникают две реакции [[R_x]], [[R_y]]. Поворот допускает, реактивного момента нет.")
            1 -> L("Tip sedí. Válečky jezdí po kolejnici a kloub se natočí – brání jen pohybu kolmo ke kolejnici: jedna reakce.", "Прогноз верный. Катки ездят по рельсу, шарнир поворачивается — опора препятствует только движению перпендикулярно рельсу: одна реакция.")
            else -> L("Tip sedí. Zeď nedovolí nic – dvě síly a reakční **moment** [[M]].", "Прогноз верный. Стена не допускает ничего — две силы и реактивный **момент** [[M]].")
        })
        else Note(Tone.NUDGE, L(
            "Tip nesedí – podívej se, u kterých zkoušek se nosník pohnul a kde se objevila modrá reakce. Oprav tip a vyzkoušej znovu.",
            "Прогноз не совпал — посмотри, при каких проверках балка сдвинулась, а где появилась синяя реакция. Исправь прогноз и проверь снова.",
        ))
    }
    if (okTypes.size == 3) SmallText(L(
        "Pevná kloubová podpora: 2 reakce. Posuvná kloubová: 1. Vetknutí: 3 (dvě síly a moment). Podporám se společně říká **vazby**.",
        "Шарнирно-неподвижная опора: 2 реакции. Шарнирно-подвижная: 1. Жёсткая заделка (vetknutí): 3 (две силы и момент). Все опоры вместе называют **связями** (vazby).",
    ))
}

/** Karta 1: pravidlo a přiřazení sad reakcí ke značkám podpor. */
@Composable
private fun CardScope.MatchSets() {
    var i by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var ok by remember { mutableStateOf(false) }
    var solved by remember { mutableStateOf(setOf<Int>()) }
    val st = TYPES[i.coerceAtMost(2)]
    // správná sada: 0 = R_x, R_y; 1 = R; 2 = R_x, R_y, M
    val correct = i
    Stage(h = 170.0, description = L("Značka podpory", "Условное обозначение опоры").t()) { c ->
        support(st, if (st == SupportType.FIXED) 140.0 else 200.0, 80.0, c, "A", wallLeft = true)
        beam(if (st == SupportType.FIXED) 140.0 else 150.0, 330.0, 80.0, c)
    }
    CardText(L(
        "Pravidlo: **každý zakázaný pohyb = jedna reakce**. Jaké reakce má tahle podpora?",
        "Правило: **каждое запрещённое движение = одна реакция**. Какие реакции у этой опоры?",
    ))
    Todo(
        L("Vyber správné reakce **pevné kloubové** podpory (na obrázku)", "Выбери верные реакции **шарнирно-неподвижной** опоры (на рисунке)") to (0 in solved),
        L("Klepni na „Další podpora →“ a vyber reakce **posuvné kloubové** podpory", "Нажми «Следующая опора →» и выбери реакции **шарнирно-подвижной** опоры") to (1 in solved),
        L("Znovu klepni na „Další podpora →“ a vyber reakce **vetknutí**", "Снова нажми «Следующая опора →» и выбери реакции **жёсткой заделки**") to (2 in solved),
    )
    Chips(
        listOf(L("[[R_Ax]], [[R_Ay]]", "[[R_Ax]], [[R_Ay]]"), L("jen [[R_A]] (kolmo k podložce)", "только [[R_A]] (перпендикулярно опорной поверхности)"), L("[[R_Ax]], [[R_Ay]], [[M_A]]", "[[R_Ax]], [[R_Ay]], [[M_A]]")),
        picked,
    ) { k ->
        if (ok || i > 2) return@Chips
        picked = k
        ok = k == correct
        if (ok) solved = solved + i
    }
    val p = picked
    if (i <= 2 && p != null) {
        if (ok) {
            Note(Tone.OK, when (i) {
                0 -> L("Ano – pevná kloubová podpora nedovolí posun vodorovně ani svisle, natočení ale dovolí.", "Да — шарнирно-неподвижная опора не допускает смещения ни по горизонтали, ни по вертикали, но поворот допускает.")
                1 -> L("Ano – válečky dovolí pohyb podél podložky (plochy, po které jezdí) i natočení. Brání jen pohybu kolmo k podložce.", "Да — катки допускают сдвиг вдоль опорной поверхности (по ней они катятся) и поворот. Препятствуют только смещению перпендикулярно опорной поверхности.")
                else -> L("Ano – vetknutí nedovolí žádný pohyb: dvě síly a moment.", "Да — жёсткая заделка не допускает никакого движения: две силы и момент.")
            })
            if (i < 2) ActionButton(L("Další podpora →", "Следующая опора →")) { i++; picked = null; ok = false }
        } else {
            Note(Tone.NUDGE, when (i) {
                0 -> L("Zkus nosníkem v duchu posunout, zvednout a natočit. Co z toho podpora nedovolí?", "Мысленно попробуй сдвинуть, поднять и повернуть балку. Что из этого опора не допустит?")
                1 -> L("Všimni si válečků: kterým směrem se po nich nosník volně posune?", "Обрати внимание на катки: в каком направлении балка по ним свободно сдвигается?")
                else -> L("Nosník je zapuštěný do zdi. Může se v ní natočit?", "Балка заделана в стену. Может ли она в ней повернуться?")
            })
        }
    }
}

/** Karta 2: kvíz – která vazba brání otočení. */
@Composable
private fun CardScope.WhichRotation() {
    Stage(h = 250.0, description = L("Tři druhy podpor a nosník na dvou podporách", "Три вида опор и балка на двух опорах").t()) { c ->
        val y = 78.0
        // každá podpora na krátkém kousku nosníku, číslo nad ní
        beam(40.0, 110.0, y, c)
        support(SupportType.PIN, 75.0, y, c, "")
        text(75.0, y - 22, "1", c.ink, 17.0, bold = true)
        beam(165.0, 235.0, y, c)
        support(SupportType.ROLLER, 200.0, y, c, "")
        text(200.0, y - 22, "2", c.ink, 17.0, bold = true)
        support(SupportType.FIXED, 300.0, y, c, "", wallLeft = true)
        beam(300.0, 370.0, y, c)
        text(335.0, y - 22, "3", c.ink, 17.0, bold = true)
        // druhá otázka: celý nosník na dvou podporách
        val yb = 200.0
        line(20.0, 150.0, 380.0, 150.0, c.line, 1.0)
        beam(70.0, 330.0, yb, c)
        support(SupportType.PIN, 80.0, yb, c, "A")
        support(SupportType.ROLLER, 320.0, yb, c, "B")
        force(200.0, yb - 40, 200.0, yb - 6, c.load)
        text(208.0, yb - 26, null, c.load, 14.0, Anchor.START, sym = "F")
    }
    Quiz(
        question = L("Která podpora (vazba) nedovolí nosníku se **natočit**?", "Какая опора (связь, vazba) не даёт балке **повернуться**?"),
        options = listOf(
            Opt(L("1 – pevná kloubová podpora", "1 — шарнирно-неподвижная опора"), why = L("Je to kloub – a kloub natočení dovolí. Proto nemá reakční moment.", "Это шарнир — а шарнир поворот допускает. Поэтому реактивного момента нет.")),
            Opt(L("2 – posuvná kloubová podpora", "2 — шарнирно-подвижная опора"), why = L("Válečky a kloub: dovolí pohyb podél podložky i natočení. Brání jen pohybu kolmo k podložce.", "Катки и шарнир: допускают сдвиг вдоль опорной поверхности и поворот. Препятствуют только смещению перпендикулярно ей.")),
            Opt(L("3 – vetknutí", "3 — жёсткая заделка"), correct = true),
        ),
        explain = L(
            "Vetknutí drží nosník pevně ve zdi – proto má kromě dvou sil i reakční moment [[M]].",
            "Жёсткая заделка неподвижно держит балку в стене — поэтому кроме двух сил у неё есть ещё реактивный момент [[M]].",
        ),
    )
    Quiz(
        question = L(
            "Nosník dole má vlevo pevnou kloubovou podporu A a vpravo posuvnou B. Kolik **neznámých reakcí** má celkem?",
            "У нижней балки слева шарнирно-неподвижная опора A, справа шарнирно-подвижная B. Сколько у неё всего **неизвестных реакций**?",
        ),
        options = listOf(
            Opt(L("2", "2"), why = L("Kolik reakcí má samotná pevná kloubová podpora? A k tomu ještě B.", "Сколько реакций у одной только шарнирно-неподвижной опоры? И к ним ещё B.")),
            Opt(L("3", "3"), correct = true),
            Opt(L("4", "4"), why = L("Posuvná podpora B brání jen jednomu pohybu – kolik reakcí tedy má?", "Подвижная опора B препятствует только одному движению — сколько у неё реакций?")),
            Opt(L("5", "5"), why = L("Žádná z podpor nebrání natočení – reakční moment tu není.", "Ни одна из опор не препятствует повороту — реактивного момента нет.")),
        ),
        explain = L(
            "A: [[R_Ax]], [[R_Ay]] (2) + B: [[R_B]] (1) = **3 neznámé**. A v rovině máme právě 3 rovnice rovnováhy (lekce 5) – proto takový nosník spočítáš.",
            "A: [[R_Ax]], [[R_Ay]] (2) + B: [[R_B]] (1) = **3 неизвестные**. А на плоскости у нас ровно 3 уравнения равновесия (урок 5) — поэтому такую балку можно рассчитать.",
        ),
        columns = 4,
    )
}

internal fun lesson6() = Lesson(
    id = 6,
    title = L("Vazby brání pohybu", "Связи ограничивают движение"),
    summary = L("Pevná a posuvná kloubová podpora, vetknutí – co dovolí a co ne.", "Шарнирно-неподвижная и шарнирно-подвижная опоры, жёсткая заделка — что допускают и что нет."),
    cards = listOf(
        Card { PushTest() },
        Card { MatchSets() },
        Card { WhichRotation() },
    ),
)
