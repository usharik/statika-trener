package cz.usharik.statika.learn.trig

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cz.usharik.statika.learn.t
import cz.usharik.statika.ui.LocalColors
import cz.usharik.statika.learn.ActionButton
import cz.usharik.statika.learn.Card
import cz.usharik.statika.learn.CardScope
import cz.usharik.statika.learn.CardText
import cz.usharik.statika.learn.Chips
import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Lesson
import cz.usharik.statika.learn.Note
import cz.usharik.statika.learn.NumSpec
import cz.usharik.statika.learn.NumTrap
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.learn.Pt
import cz.usharik.statika.learn.Readout
import cz.usharik.statika.learn.SmallText
import cz.usharik.statika.learn.Stage
import cz.usharik.statika.learn.Step
import cz.usharik.statika.learn.Steps
import cz.usharik.statika.learn.ValueSlider
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.bar
import cz.usharik.statika.learn.grid
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.numStep
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.poly
import cz.usharik.statika.learn.quizStep
import cz.usharik.statika.learn.rect
import cz.usharik.statika.learn.rightAngleMark
import cz.usharik.statika.learn.rightTriangle
import cz.usharik.statika.learn.screenAngle
import cz.usharik.statika.learn.tapSpot
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.learn.triGeom
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/*
 * Kurz trigonometrie – lekce 2 (id 102): sinus, kosinus, tangens jako podíly stran; odvěsna = přepona · sin/cos.
 */

private val SIDE_NAME = mapOf(
    TriSide.HYP to L("přepona", "гипотенуза"),
    TriSide.ADJ to L("přilehlá", "прилежащий"),
    TriSide.OPP to L("protilehlá", "противолежащий"),
)

/** (čitatel, jmenovatel) pro sin, cos, tg */
private val PARTS = listOf(TriSide.OPP to TriSide.HYP, TriSide.ADJ to TriSide.HYP, TriSide.OPP to TriSide.ADJ)
private val FN_NAMES = listOf("sin α", "cos α", "tg α")

/** Zlomek „fn = čitatel / jmenovatel“ vpravo nahoře ve scéně; null = otazník. */
private fun PrimBuilder.fraction(fn: String, top: String?, bottom: String?, c: StatikaColors, topColor: Color, bottomColor: Color, cx: Double = 338.0, y: Double = 62.0) {
    text(cx - 50, y + 13, "$fn =", c.ink, 16.0, Anchor.END, bold = true)
    text(cx, y, top ?: "?", if (top == null) c.inkSoft else topColor, 13.0, bold = true)
    line(cx - 46, y + 8, cx + 46, y + 8, c.ink, 1.4)
    text(cx, y + 28, bottom ?: "?", if (bottom == null) c.inkSoft else bottomColor, 13.0, bold = true)
}

/** Karta 0: tři podíly stran, tři jména. Geometrie stojí, mění se jen to, který podíl sledujeme. */
@Composable
private fun CardScope.ThreeRatios() {
    val t = tr()
    val alpha = 35.0
    val demo = remember { Animatable(0f) }
    var demoDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(700)
        demo.animateTo(1f, tween(900))
        delay(500)
        demo.animateTo(2f, tween(900))
        delay(300)
        demoDone = true
    }
    val r = Ramp(60.0, 200.0, 190.0, alpha)
    val opp = 10 * Trig.sinD(alpha)
    val adj = 10 * Trig.cosD(alpha)
    val cosQ = quizStep(
        question = L("Který podíl je **cos α**?", "Какое отношение — **cos α**?"),
        options = listOf(
            Opt(L("přilehlá / přepona", "прилежащий катет / гипотенуза"), correct = true),
            Opt(L("protilehlá / přepona", "противолежащий катет / гипотенуза"), why = L("To je sinus. Kosinus bere tu druhou odvěsnu – tu, která z vrcholu α vychází.", "Это синус. Косинус берёт другой катет — тот, что выходит из вершины α.")),
            Opt(L("protilehlá / přilehlá", "противолежащий катет / прилежащий катет"), why = L("Tady se dělí odvěsna odvěsnou – to bude tangens. Sinus i kosinus dělí přeponou.", "Здесь катет делится на катет — это будет тангенс. И синус, и косинус делят на гипотенузу.")),
        ),
        explain = L(
            "cos α = přilehlá / přepona = ${num(adj)} / 10 ≈ ${num(adj / 10, 3)}.",
            "cos α = прилежащий катет / гипотенуза = ${num(adj)} / 10 ≈ ${num(adj / 10, 3)}.",
        ),
    )
    val tgQ = quizStep(
        question = L("Který podíl obou odvěsen je **tg α**?", "Какое отношение катетов — **tg α**?"),
        options = listOf(
            Opt(L("protilehlá / přilehlá", "противолежащий катет / прилежащий катет"), correct = true),
            Opt(L("přilehlá / protilehlá", "прилежащий катет / противолежащий катет"), why = L("Obrácený zlomek. Nahoře je, stejně jako u sinu, odvěsna naproti úhlu α.", "Перевёрнутая дробь. Сверху, как и у синуса, катет напротив угла α.")),
            Opt(L("protilehlá / přepona", "противолежащий катет / гипотенуза"), why = L("To je sinus. Tangens dělí odvěsnu odvěsnou – přepona v něm není.", "Это синус. Тангенс делит катет на катет — гипотенузы в нём нет.")),
        ),
        explain = L(
            "tg α = protilehlá / přilehlá = ${num(opp)} / ${num(adj)} ≈ ${num(opp / adj, 3)}. Shrnutí: sin α = protilehlá / přepona; cos α = přilehlá / přepona; tg α = protilehlá / přilehlá. Na kalkulačce se tangens jmenuje **tan**.",
            "tg α = противолежащий / прилежащий = ${num(opp)} / ${num(adj)} ≈ ${num(opp / adj, 3)}. Итого: sin α = противолежащий катет / гипотенуза; cos α = прилежащий катет / гипотенуза; tg α = противолежащий катет / прилежащий катет. На калькуляторе тангенс называется **tan**.",
        ),
    )
    // který podíl ukazuje obrázek a jestli už prozrazený
    val fn = if (!demoDone) 0 else if (!cosQ.done) 1 else 2
    val shown = when (fn) { 0 -> true; 1 -> false; else -> tgQ.done }
    Stage(h = 230.0, description = t.pick(L("Pravoúhlý trojúhelník s přeponou 10 dílků a úhlem 35°", "Прямоугольный треугольник с гипотенузой 10 делений и углом 35°"))) { c ->
        line(20.0, 200.0, 290.0, 200.0, c.line, 1.0)
        rampTri(r, c, "35°", hypLabel = t.pick(L("přepona 10", "гипотенуза 10")), oppLabel = num(opp))
        text(r.c.x + 8, (r.c.y + r.b.y) / 2 + 22, t.pick(L("protilehlá", "противолеж.")), c.derived, 12.0, Anchor.START)
        text((r.a.x + r.c.x) / 2, r.a.y + 20, t.pick(L("přilehlá ${num(adj)}", "прилежащий ${num(adj)}")), c.derived, 13.0)
        fun side(s: TriSide) = when (s) {
            TriSide.ADJ -> r.a to r.c
            TriSide.OPP -> r.c to r.b
            TriSide.HYP -> r.a to r.b
        }
        val (n, d) = PARTS[fn]
        val pTop = if (fn == 0) demo.value.coerceIn(0f, 1f) else if (shown) 1f else 0f
        val pBot = if (fn == 0) (demo.value - 1f).coerceIn(0f, 1f) else if (shown) 1f else 0f
        if (pTop > 0f) side(n).let { (p, q) -> line(p.x, p.y, q.x, q.y, c.highlight, 5.0, alpha = pTop) }
        if (pBot > 0f) side(d).let { (p, q) -> line(p.x, p.y, q.x, q.y, c.accent, 5.0, alpha = pBot) }
        fraction(
            FN_NAMES[fn],
            if (pTop > 0.5f) t.pick(SIDE_NAME.getValue(n)) else null,
            if (pBot > 0.5f) t.pick(SIDE_NAME.getValue(d)) else null,
            c, c.highlight, c.accent,
        )
        if (pTop > 0.5f || pBot > 0.5f) text(392.0, 130.0, t.pick(L("žlutě nahoře, zeleně dole", "жёлтое сверху, зелёное снизу")), c.inkSoft, 11.0, Anchor.END)
    }
    CardText(L(
        "Sinus, kosinus a tangens jsou podíly stran pravoúhlého trojúhelníku. Podíl nemá jednotku.",
        "Синус, косинус и тангенс — отношения сторон прямоугольного треугольника. У отношения нет единицы измерения.",
    ))
    Steps(
        Step(
            L("Sleduj, které strany tvoří sinus", "Проследи, какие стороны образуют синус"), demoDone,
            result = { Note(Tone.INFO, L("sin α = protilehlá / přepona = ${num(opp)} / 10 ≈ ${num(opp / 10, 3)}.", "sin α = противолежащий катет / гипотенуза = ${num(opp)} / 10 ≈ ${num(opp / 10, 3)}.")) },
        ) {
            SmallText(L("Nahoru do zlomku jde odvěsna naproti úhlu α, dolů přepona…", "Вверх в дробь идёт катет напротив угла α, вниз — гипотенуза…"))
        },
        cosQ,
        tgQ,
    )
}

/** Strany trojúhelníku ABC (A = α, C = pravý úhel) v pořadí voleb AB, AC, BC. */
private val ABC = listOf(TriSide.HYP, TriSide.ADJ, TriSide.OPP)
private val ABC_NAMES = listOf("AB", "AC", "BC")

/** Řádek doku „Nahoře / Dole“ s volbou strany AB / AC / BC. */
@Composable
private fun SlotRow(label: L, selected: Int?, onPick: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label.t(), style = MaterialTheme.typography.bodyMedium, color = LocalColors.current.inkSoft, modifier = Modifier.width(64.dp).padding(top = 8.dp))
        Chips(ABC_NAMES.map { L(it, it) }, selected, Modifier.weight(1f)) { onPick(it) }
    }
}

/** Karta 1: slož zlomek sám – strany podle písmen, trojúhelník se každé kolo otočí. */
@Composable
private fun CardScope.BuildFraction() {
    val t = tr()
    var round by remember { mutableIntStateOf(0) } // 0 sin, 1 cos, 2 tg, 3 hotovo
    var top by remember { mutableStateOf<Int?>(null) } // index v ABC
    var bottom by remember { mutableStateOf<Int?>(null) }
    var msg by remember { mutableStateOf<L?>(null) }
    val g = when (round.coerceAtMost(2)) {
        0 -> triGeom(4.0, 2.8, fit = listOf(20.0, 30.0, 195.0, 160.0))
        1 -> triGeom(4.0, 2.8, rot = 1, mirror = true, fit = listOf(30.0, 25.0, 180.0, 175.0))
        else -> triGeom(4.0, 2.8, rot = 2, fit = listOf(20.0, 30.0, 195.0, 160.0))
    }
    fun ends(i: Int) = g.side(ABC[i])
    fun check() {
        if (round > 2) return
        val tp = top ?: return
        val bt = bottom ?: run { msg = L("Vyber i jmenovatel (dole).", "Выбери и знаменатель (снизу)."); return }
        val (n, d) = PARTS[round]
        val sn = ABC[tp]
        val sd = ABC[bt]
        if (sn == n && sd == d) {
            round++
            top = null
            bottom = null
            msg = null
            return
        }
        msg = when {
            tp == bt -> L("Nahoře i dole máš stejnou stranu – to je vždy 1.", "Сверху и снизу одна и та же сторона — это всегда 1.")
            round < 2 && sn == d && sd == n -> L("Obráceně: přepona je nejdelší, patří dolů – jinak by vyšlo víc než 1.", "Наоборот: гипотенуза самая длинная, она стоит снизу — иначе получится больше 1.")
            round < 2 && sd != TriSide.HYP -> L("Sinus i kosinus dělí přeponou – stranou naproti pravému úhlu u C.", "И синус, и косинус делят на гипотенузу — сторону напротив прямого угла C.")
            round == 0 -> L("AC vychází z vrcholu A, je přilehlá. Sinus bere odvěsnu **naproti** úhlu α.", "AC выходит из вершины A, он прилежащий. Синус берёт катет **напротив** угла α.")
            round == 1 -> L("BC leží naproti α. Kosinus bere odvěsnu, která z vrcholu α **vychází**.", "BC лежит напротив α. Косинус берёт катет, который **выходит** из вершины α.")
            sn == TriSide.HYP || sd == TriSide.HYP -> L("V tangentu přepona není – jen obě odvěsny.", "В тангенсе гипотенузы нет — только два катета.")
            else -> L("Obráceně: nahoře je, stejně jako u sinu, odvěsna naproti α.", "Наоборот: сверху, как и у синуса, катет напротив α.")
        }
    }
    fun tap(i: Int) {
        if (round > 2) return
        msg = null
        when {
            top == null -> top = i
            bottom == null -> bottom = i
            else -> { top = i; bottom = null }
        }
    }
    Stage(
        h = 230.0,
        description = t.pick(L("Pravoúhlý trojúhelník ABC s úhlem α u A a pravým úhlem u C", "Прямоугольный треугольник ABC с углом α при A и прямым углом при C")),
        onTap = { p ->
            (0..2).minByOrNull { i -> ends(i).let { (a, b) -> p.dist((a.x + b.x) / 2, (a.y + b.y) / 2) } }
                ?.takeIf { i -> ends(i).let { (a, b) -> p.dist((a.x + b.x) / 2, (a.y + b.y) / 2) } < 40 }
                ?.let { tap(it) }
        },
    ) { c ->
        rightTriangle(g, c, hyp = c.ink, legs = c.ink)
        top?.let { i -> ends(i).let { (p, q) -> line(p.x, p.y, q.x, q.y, c.highlight, 5.0) } }
        bottom?.let { i -> ends(i).let { (p, q) -> line(p.x, p.y, q.x, q.y, c.accent, 5.0) } }
        // písmena vrcholů, odsunutá od těžiště
        for ((pt, name) in listOf(g.a to "A", g.b to "B", g.c to "C")) {
            val dx = pt.x - g.centroid.x
            val dy = pt.y - g.centroid.y
            val d = hypot(dx, dy).coerceAtLeast(1e-9)
            text(pt.x + dx / d * 16, pt.y + dy / d * 16 + 5, name, c.ink, 15.0, bold = true)
        }
        for (i in 0..2) ends(i).let { (a, b) -> tapSpot((a.x + b.x) / 2, (a.y + b.y) / 2, c, i == top || i == bottom, 11.0) }
        if (round <= 2) fraction(FN_NAMES[round], top?.let { ABC_NAMES[it] }, bottom?.let { ABC_NAMES[it] }, c, c.highlight, c.accent, cx = 344.0, y = 90.0)
    }
    CardText(L(
        "Pořadí stran ve zlomku rozhoduje – obrácený podíl obvykle dává jiné číslo. Úhel α je u vrcholu A, pravý úhel u C.",
        "Порядок сторон в дроби важен — обратное отношение обычно даёт другое число. Угол α — при вершине A, прямой угол — при C.",
    ))
    val pickers: @Composable () -> Unit = {
        SlotRow(L("Nahoře", "Сверху"), top) { top = it; msg = null }
        SlotRow(L("Dole", "Снизу"), bottom) { bottom = it; msg = null }
        ActionButton(BTN_CHECK, enabled = top != null && bottom != null) { check() }
        msg?.let { Note(Tone.NUDGE, it) }
    }
    Steps(
        Step(
            L("Slož zlomek pro sin α", "Составь дробь для sin α"), round > 0,
            result = { Note(Tone.OK, L("sin α = BC / AB = protilehlá / přepona", "sin α = BC / AB = противолежащий катет / гипотенуза")) },
        ) { pickers() },
        Step(
            L("Trojúhelník se otočil. Slož zlomek pro cos α", "Треугольник повернулся. Составь дробь для cos α"), round > 1,
            result = { Note(Tone.OK, L("cos α = AC / AB = přilehlá / přepona", "cos α = AC / AB = прилежащий катет / гипотенуза")) },
        ) { pickers() },
        Step(
            L("A znovu jinak natočený: slož zlomek pro tg α", "И снова повёрнут иначе: составь дробь для tg α"), round > 2,
            result = {
                Note(Tone.OK, L("tg α = BC / AC = protilehlá / přilehlá", "tg α = BC / AC = противолежащий катет / прилежащий катет"))
                SmallText(L(
                    "Sinus a kosinus porovnávají odvěsnu s přeponou. Tangens porovnává protilehlou odvěsnu s přilehlou. Na natočení obrázku nezáleží.",
                    "Синус и косинус сравнивают катет с гипотенузой. Тангенс сравнивает противолежащий катет с прилежащим. Как повёрнут рисунок, неважно.",
                ))
            },
        ) { pickers() },
    )
}

/** Karta 2: kvíz – sin α v trojúhelníku 3-4-5. Strany neutrální, ať barva neprozradí přeponu. */
@Composable
private fun CardScope.Check345() {
    val t = tr()
    Stage(h = 200.0, description = t.pick(L("Trojúhelník 3-4-5 na čtverečkované síti", "Треугольник 3-4-5 на клетчатой сетке"))) { c ->
        val s = 40.0
        val ox = 110.0
        val oy = 30.0
        grid(ox, oy, s, 4, 3, c)
        val a = Pt(ox, oy + 3 * s)
        val cc = Pt(ox + 4 * s, oy + 3 * s)
        val b = Pt(ox + 4 * s, oy)
        poly(listOf(a.x to a.y, b.x to b.y, cc.x to cc.y), c.subtle, null, 0.0, alpha = 0.5f)
        line(a.x, a.y, cc.x, cc.y, c.ink, 2.6)
        line(cc.x, cc.y, b.x, b.y, c.ink, 2.6)
        line(a.x, a.y, b.x, b.y, c.ink, 2.6)
        rightAngleMark(cc, a, b, c)
        angleArc(a.x, a.y, 30.0, 0.0, screenAngle(a, b), "α", c)
        text((a.x + cc.x) / 2, a.y + 22, "4", c.ink, 15.0, bold = true)
        text(cc.x + 14, (cc.y + b.y) / 2 + 5, "3", c.ink, 15.0, Anchor.START, bold = true)
        text((a.x + b.x) / 2 - 14, (a.y + b.y) / 2 - 12, "5", c.ink, 15.0, Anchor.END, bold = true)
    }
    CardText(L(
        "K výpočtu poměru stran nemusíme znát úhel ve stupních.",
        "Чтобы вычислить отношение сторон, не обязательно знать угол в градусах.",
    ))
    Steps(
        quizStep(
            question = L("Kolik je sin α?", "Чему равен sin α?"),
            options = listOf(
                Opt(L("3/5 = 0,6", "3/5 = 0,6"), correct = true),
                Opt(L("4/5 = 0,8", "4/5 = 0,8"), why = L("4 je odvěsna u úhlu α. Která funkce bere přilehlou odvěsnu?", "4 — катет у угла α. Какая функция берёт прилежащий катет?")),
                Opt(L("3/4 = 0,75", "3/4 = 0,75"), why = L("Tady se dělí odvěsna odvěsnou – to je tangens. Čím dělí sinus?", "Здесь катет делится на катет — это тангенс. На что делит синус?")),
                Opt(L("5/3 ≈ 1,67", "5/3 ≈ 1,67"), why = L("Přepona je nejdelší strana, takže sinus nikdy nevyjde větší než 1. Co patří ve zlomku nahoru?", "Гипотенуза — самая длинная сторона, поэтому синус никогда не больше 1. Что в дроби должно быть сверху?")),
            ),
            explain = L(
                "Protilehlá odvěsna má délku 3 a přepona 5. Proto sin α = 3/5 = 0,6.",
                "Противолежащий катет имеет длину 3, гипотенуза — 5. Поэтому sin α = 3/5 = 0,6.",
            ),
            columns = 2,
        ),
    )
}

/** Karta 3: průměty úsečky délky 1 – sin roste od 0 do 1, cos klesá od 1 do 0. */
@Composable
private fun CardScope.Range() {
    val t = tr()
    var alpha by remember { mutableStateOf(20.0) }
    var guess by remember { mutableStateOf<Int?>(null) }
    var swept by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<L?>(null) }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(guess) {
        if (guess == null || swept) return@LaunchedEffect
        delay(400)
        sweep.snapTo(0f)
        alpha = 0.0
        delay(500)
        sweep.animateTo(90f, tween(3600, easing = LinearEasing)) { alpha = value.toDouble() }
        delay(400)
        swept = true
        alpha = 20.0
    }
    val s = Trig.sinD(alpha)
    val co = Trig.cosD(alpha)
    val proj = listOf(L("Vodorovný", "Горизонтальная"), L("Svislý", "Вертикальная"), L("Oba", "Обе"))
    Stage(h = 230.0, description = t.pick(L("Úsečka délky 1 a její vodorovný a svislý průmět", "Отрезок длины 1 и его горизонтальная и вертикальная проекции"))) { c ->
        val a = Pt(40.0, 205.0)
        val len = 170.0
        val b = Pt(a.x + len * co, a.y - len * s)
        val cc = Pt(b.x, a.y)
        line(20.0, 205.0, 250.0, 205.0, c.line, 1.0)
        if (s > 0.01 && co > 0.01) poly(listOf(a.x to a.y, b.x to b.y, cc.x to cc.y), c.subtle, null, 0.0, alpha = 0.6f)
        if (s > 0.01) line(cc.x, cc.y, b.x, b.y, c.derived, 3.0, dash = co > 0.01)
        if (co > 0.01) line(a.x, a.y, cc.x, cc.y, c.react, 3.0)
        line(a.x, a.y, b.x, b.y, c.load, 2.6)
        if (s > 0.1 && co > 0.1) rightAngleMark(cc, a, b, c)
        angleArc(a.x, a.y, 28.0, 0.0, -alpha, "${alpha.roundToInt()}°", c)
        text((a.x + b.x) / 2 - 12 * s - 6, (a.y + b.y) / 2 - 12 * co, "1", c.load, 14.0, Anchor.END, bold = true)
        // sloupce: svislý průmět (sin) a vodorovný průmět (cos)
        bar(290.0, 30.0, 32.0, 160.0, s, c.derived, c)
        bar(345.0, 30.0, 32.0, 160.0, co, c.react, c)
        text(306.0, 210.0, "sin", c.derived, 13.0, bold = true)
        text(361.0, 210.0, "cos", c.react, 13.0, bold = true)
        if (guess != null) {
            text(306.0, 24.0, num(s, 2), c.derived, 12.0)
            text(361.0, 24.0, num(co, 2), c.react, 12.0)
        }
        text(286.0, 36.0, "1", c.inkSoft, 11.0, Anchor.END)
        text(286.0, 194.0, "0", c.inkSoft, 11.0, Anchor.END)
    }
    CardText(L(
        "Úsečka délky 1 má vodorovný průmět **cos α** a svislý průmět **sin α**.",
        "У отрезка длины 1 горизонтальная проекция равна **cos α**, а вертикальная — **sin α**.",
    ))
    Steps(
        guessStep(L("Tipni si: který průmět roste, když úsečku zvedáš k 90°?", "Предскажи: какая проекция растёт, когда отрезок поднимается к 90°?"), proj, guess) { guess = it },
        Step(
            L("Sleduj oba průměty od 0° do 90°", "Проследи за обеими проекциями от 0° до 90°"), swept,
            result = {
                Note(if (guess == 1) Tone.OK else Tone.INFO, L(
                    (if (guess == 1) "Tip sedí: " else "Je to jinak: ") + "svislý průmět (sin) roste z 0 na 1, vodorovný (cos) klesá z 1 na 0. Při 0° a 90° už to není trojúhelník – jen úsečka a její průmět.",
                    (if (guess == 1) "Прогноз верен: " else "На самом деле иначе: ") + "вертикальная проекция (sin) растёт от 0 до 1, горизонтальная (cos) убывает от 1 до 0. При 0° и 90° это уже не треугольник — только отрезок и его проекция.",
                ))
            },
        ) {
            SmallText(L("Úsečka se zvedá od 0° k 90°…", "Отрезок поднимается от 0° до 90°…"))
            Readout("sin = ${num(s, 3)} · cos = ${num(co, 3)}")
        },
        Step(
            L("Najdi úhel, při kterém jsou oba průměty stejné", "Найди угол, при котором обе проекции равны"), found,
            result = {
                Note(Tone.OK, L(
                    "Od 0° do 90° sinus roste z 0 na 1 a kosinus klesá z 1 na 0. Při 45° jsou oba √2/2 ≈ 0,707.",
                    "От 0° до 90° синус растёт от 0 до 1, а косинус убывает от 1 до 0. При 45° оба равны √2/2 ≈ 0,707.",
                ))
            },
        ) {
            ValueSlider(t.pick(L("Úhel α = ${alpha.roundToInt()}°", "Угол α = ${alpha.roundToInt()}°")), alpha.toFloat(), 0f..90f, steps = 89) {
                alpha = it.roundToInt().toDouble().coerceIn(0.0, 90.0)
                msg = null
            }
            Readout("sin = ${num(s, 3)} · cos = ${num(co, 3)}")
            ActionButton(BTN_CHECK) {
                msg = when {
                    alpha == 45.0 -> { found = true; null }
                    s < co -> L("Svislý průmět je ještě kratší – zvedni úsečku výš.", "Вертикальная проекция ещё короче — подними отрезок выше.")
                    else -> L("Svislý průmět je už delší – sklop úsečku níž.", "Вертикальная проекция уже длиннее — опусти отрезок ниже.")
                }
            }
            msg?.let { Note(Tone.NUDGE, it) }
        },
    )
}

/** Karta 4: neznámou odvěsnu vyjádříme z definice sinu – h = přepona · sin α (žebřík). */
@Composable
private fun CardScope.Ladder() {
    val t = tr()
    val expr = quizStep(
        question = L("Kterým výrazem spočítáš výšku h horního konce?", "Каким выражением найти высоту h верхнего конца?"),
        options = listOf(
            Opt(L("5 · sin 60°", "5 · sin 60°"), correct = true),
            Opt(L("5 · cos 60°", "5 · cos 60°"), why = L("Kosinus patří k odvěsně **u** úhlu 60° – to je vzdálenost paty žebříku od zdi. Výška leží naproti.", "Косинус относится к катету **у** угла 60° — это расстояние от низа лестницы до стены. Высота лежит напротив.")),
            Opt(L("5 / sin 60°", "5 / sin 60°"), why = L("Ze sin 60° = h / 5 dostaneš h vynásobením pěti, ne dělením. Výška musí vyjít menší než 5 m.", "Из sin 60° = h / 5 высота получается умножением на 5, а не делением. Высота должна быть меньше 5 м.")),
        ),
        explain = L(
            "Výška leží naproti úhlu 60°, takže sin 60° = h / 5. Obě strany vynásobíme pěti: h = 5 · sin 60°. Obecně: protilehlá = přepona · sin α, přilehlá = přepona · cos α.",
            "Высота лежит напротив угла 60°, поэтому sin 60° = h / 5. Умножаем обе части на 5: h = 5 · sin 60°. В общем виде: противолежащий катет = гипотенуза · sin α, прилежащий = гипотенуза · cos α.",
        ),
    )
    val calc = numStep(
        question = L("Vypočítej h na dvě desetinná místa. sin 60° ≈ 0,866", "Вычисли h до двух знаков после запятой. sin 60° ≈ 0,866"),
        fields = listOf(NumSpec(L("h", "h"), 5 * Trig.sinD(60.0), "m")),
        explain = L(
            "h = 5 · sin 60° ≈ 5 · 0,866 ≈ 4,33 m. Výška je menší než délka žebříku – jak má být.",
            "h = 5 · sin 60° ≈ 5 · 0,866 ≈ 4,33 м. Высота меньше длины лестницы — как и должно быть.",
        ),
        traps = listOf(
            NumTrap.near(L("To je 5 · cos 60° – vzdálenost paty žebříku od zdi, odvěsna **u** úhlu. Výška leží naproti.", "Это 5 · cos 60° — расстояние от низа лестницы до стены, катет **у** угла. Высота лежит напротив."), 2.5),
            NumTrap(L("Výška nemůže být větší než žebřík. Přeponu sinem násob, neděl.", "Высота не может быть больше лестницы. Гипотенузу на синус умножай, а не дели.")) { v -> v[0]?.let { abs(it) > 5.0 } ?: false },
        ),
        nudge = L("Dosaď do vybraného výrazu: 5 · 0,866 = ?", "Подставь в выбранное выражение: 5 · 0,866 = ?"),
    )
    Stage(h = 230.0, description = t.pick(L("Žebřík dlouhý 5 m opřený o zeď pod úhlem 60°", "Лестница длиной 5 м, прислонённая к стене под углом 60°"))) { c ->
        val wallX = 300.0
        val ground = 205.0
        val len = 170.0
        val foot = Pt(wallX - len * Trig.cosD(60.0), ground)
        val top = Pt(wallX, ground - len * Trig.sinD(60.0))
        rect(wallX, 15.0, 22.0, ground - 15, c.subtle, c.ink, 1.4)
        line(20.0, ground, 380.0, ground, c.ink, 1.6)
        // žebřík: štěpina a příčky
        line(foot.x, foot.y, top.x, top.y, c.load, 3.0)
        for (i in 1..6) {
            val k = i / 7.0
            val x = foot.x + (top.x - foot.x) * k
            val y = foot.y + (top.y - foot.y) * k
            line(x - 7, y - 4, x + 7, y + 4, c.load, 1.6)
        }
        angleArc(foot.x, foot.y, 30.0, 0.0, -60.0, "60°", c)
        text((foot.x + top.x) / 2 - 16, (foot.y + top.y) / 2, "5 m", c.load, 14.0, Anchor.END, bold = true)
        line(wallX - 6, ground, wallX - 6, top.y, c.derived, 1.4, dash = true)
        text(wallX + 30, (ground + top.y) / 2 + 5, if (calc.done) "h ≈ 4,33 m" else "h = ?", c.derived, 15.0, Anchor.START, bold = true)
    }
    CardText(L(
        "Žebřík dlouhý 5 m svírá se zemí 60°. Výška jeho horního konce je jedna z odvěsen.",
        "Лестница длиной 5 м составляет с землёй угол 60°. Высота её верхнего конца — один из катетов.",
    ))
    Steps(expr, calc)
}

internal fun trigLesson2() = Lesson(
    id = 102,
    title = L("Sinus, kosinus, tangens", "Синус, косинус, тангенс"),
    summary = L("Tři podíly stran – a jak z přepony spočítat odvěsnu.", "Три отношения сторон — и как по гипотенузе найти катет."),
    cards = listOf(
        Card { ThreeRatios() },
        Card { BuildFraction() },
        Card { Check345() },
        Card { Range() },
        Card { Ladder() },
    ),
)
