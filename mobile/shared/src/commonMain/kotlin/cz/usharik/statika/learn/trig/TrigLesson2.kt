package cz.usharik.statika.learn.trig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cz.usharik.statika.learn.Card
import cz.usharik.statika.learn.CardScope
import cz.usharik.statika.learn.CardText
import cz.usharik.statika.learn.Chips
import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Lesson
import cz.usharik.statika.learn.Note
import cz.usharik.statika.learn.NumAnswer
import cz.usharik.statika.learn.NumSpec
import cz.usharik.statika.learn.NumTrap
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.learn.Pt
import cz.usharik.statika.learn.Quiz
import cz.usharik.statika.learn.Readout
import cz.usharik.statika.learn.SmallText
import cz.usharik.statika.learn.Stage
import cz.usharik.statika.learn.Todo
import cz.usharik.statika.learn.ValueSlider
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.bar
import cz.usharik.statika.learn.grid
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.poly
import cz.usharik.statika.learn.rect
import cz.usharik.statika.learn.rightAngleMark
import cz.usharik.statika.learn.screenAngle
import cz.usharik.statika.learn.tapSpot
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Kurz trigonometrie – lekce 2 (id 102): sinus, kosinus, tangens jako podíly stran; odvěsna = přepona · sin/cos.
 */

private val SIDE_NAME = mapOf(
    TriSide.HYP to L("přepona", "гипотенуза"),
    TriSide.ADJ to L("přilehlá", "прилежащий"),
    TriSide.OPP to L("protilehlá", "противолежащий"),
)

/** Karta 0: tři podíly stran, tři jména. */
@Composable
private fun CardScope.ThreeRatios() {
    val t = tr()
    var alpha by remember { mutableStateOf(35.0) }
    var fn by remember { mutableStateOf<Int?>(null) }
    val seen = remember { mutableStateOf(setOf<Int>()) }
    val r = Ramp(70.0, 200.0, 190.0, alpha)
    val opp = 10 * Trig.sinD(alpha)
    val adj = 10 * Trig.cosD(alpha)
    // (čitatel, jmenovatel) pro sin, cos, tg
    val parts = listOf(TriSide.OPP to TriSide.HYP, TriSide.ADJ to TriSide.HYP, TriSide.OPP to TriSide.ADJ)
    Stage(h = 230.0, description = t.pick(L("Pravoúhlý trojúhelník s přeponou 10 dílků", "Прямоугольный треугольник с гипотенузой 10 делений"))) { c ->
        line(20.0, 200.0, 380.0, 200.0, c.line, 1.0)
        rampTri(r, c, "${alpha.roundToInt()}°", hypLabel = "10", oppLabel = num(opp))
        text((r.a.x + r.c.x) / 2, r.a.y + 22, num(adj), c.derived, 13.0)
        fn?.let { i ->
            val (n, d) = parts[i]
            fun side(s: TriSide) = when (s) {
                TriSide.ADJ -> r.a to r.c
                TriSide.OPP -> r.c to r.b
                TriSide.HYP -> r.a to r.b
            }
            val (p1, q1) = side(n)
            line(p1.x, p1.y, q1.x, q1.y, c.highlight, 5.0)
            val (p2, q2) = side(d)
            line(p2.x, p2.y, q2.x, q2.y, c.accent, 5.0)
        }
    }
    CardText(L(
        "Ze tří stran jdou složit tři užitečné podíly – **goniometrické funkce** úhlu α. Každá má jméno a závisí jen na úhlu.",
        "Из трёх сторон можно составить три полезных отношения — **тригонометрические функции** угла α (goniometrické funkce). У каждой есть имя, и каждая зависит только от угла.",
    ))
    Todo(
        L("Klepni na „sin α“ – které dvě strany se dělí?", "Нажми «sin α» — какие две стороны делятся?") to (0 in seen.value),
        L("Klepni na „cos α“", "Нажми «cos α»") to (1 in seen.value),
        L("Klepni na „tg α“", "Нажми «tg α»") to (2 in seen.value),
    )
    Chips(listOf(L("sin α", "sin α"), L("cos α", "cos α"), L("tg α", "tg α")), fn) { i ->
        fn = i
        seen.value = seen.value + i
    }
    ValueSlider(t.pick(L("Úhel α = ${alpha.roundToInt()}°", "Угол α = ${alpha.roundToInt()}°")), alpha.toFloat(), 10f..80f, steps = 13) {
        alpha = ((it / 5).roundToInt() * 5.0).coerceIn(10.0, 80.0)
    }
    fn?.let { i ->
        val names = listOf("sin", "cos", "tg")
        val (n, d) = parts[i]
        val vn = when (n) { TriSide.OPP -> opp; TriSide.ADJ -> adj; TriSide.HYP -> 10.0 }
        val vd = when (d) { TriSide.OPP -> opp; TriSide.ADJ -> adj; TriSide.HYP -> 10.0 }
        Readout("${names[i]} ${alpha.roundToInt()}° = ${t.pick(SIDE_NAME.getValue(n))} / ${t.pick(SIDE_NAME.getValue(d))} = ${num(vn)} / ${num(vd)} = ${num(vn / vd, 3)}")
        SmallText(L("Žlutě čitatel (nahoře ve zlomku), zeleně jmenovatel (dole).", "Жёлтым — числитель (сверху в дроби), зелёным — знаменатель (снизу)."))
    }
    if (seen.value.size == 3) SmallText(L(
        "sin α = protilehlá / přepona · cos α = přilehlá / přepona · tg α = protilehlá / přilehlá",
        "sin α = противолежащий / гипотенуза · cos α = прилежащий / гипотенуза · tg α = противолежащий / прилежащий",
    ))
    SmallText(L("Na kalkulačce se tangens jmenuje **tan**.", "На калькуляторе тангенс называется **tan**."))
}

/** Karta 1: slož zlomek sám – klepnutím na strany. */
@Composable
private fun CardScope.BuildFraction() {
    val t = tr()
    var round by remember { mutableIntStateOf(0) } // 0 sin, 1 cos, 2 tg, 3 hotovo
    var num0 by remember { mutableStateOf<TriSide?>(null) }
    var msg by remember { mutableStateOf<L?>(null) }
    val targets = listOf(TriSide.OPP to TriSide.HYP, TriSide.ADJ to TriSide.HYP, TriSide.OPP to TriSide.ADJ)
    val names = listOf("sin α", "cos α", "tg α")
    val r = Ramp(80.0, 190.0, 220.0, 35.0)
    val spots = mapOf(
        TriSide.HYP to Pt((r.a.x + r.b.x) / 2, (r.a.y + r.b.y) / 2),
        TriSide.ADJ to Pt((r.a.x + r.c.x) / 2, r.a.y),
        TriSide.OPP to Pt(r.c.x, (r.c.y + r.b.y) / 2),
    )
    fun tap(s: TriSide) {
        if (round > 2) return
        val (n, d) = targets[round]
        if (num0 == null) {
            if (s == n) { num0 = s; msg = null } else msg = when {
                round == 0 && s == TriSide.ADJ -> L("Sinus bere odvěsnu **naproti** úhlu. Která strana se úhlu α nedotýká?", "Синус берёт катет **напротив** угла. Какая сторона не касается угла α?")
                round == 1 && s == TriSide.OPP -> L("Kosinus bere odvěsnu **u** úhlu. Která odvěsna z úhlu α vychází?", "Косинус берёт катет **у** угла. Какой катет выходит из угла α?")
                round == 2 && s == TriSide.HYP -> L("V tangentu přepona vůbec není – jen obě odvěsny.", "В тангенсе гипотенузы нет вообще — только два катета.")
                else -> L("Přepona patří ve zlomku dolů – nahoru dej odvěsnu.", "Гипотенуза в дроби стоит снизу — сверху поставь катет.")
            }
        } else {
            if (s == d) {
                round++
                num0 = null
                msg = null
            } else msg = when {
                round < 2 -> L("Sinus i kosinus dělí přeponou – nejdelší stranou.", "И синус, и косинус делят на гипотенузу — самую длинную сторону.")
                s == TriSide.HYP -> L("V tangentu přepona vůbec není – jen obě odvěsny.", "В тангенсе гипотенузы нет вообще — только два катета.")
                else -> L("Tuhle stranu už máš nahoře. Dole je ta druhá odvěsna.", "Эта сторона уже стоит сверху. Снизу — другой катет.")
            }
        }
    }
    Stage(
        h = 230.0,
        description = t.pick(L("Trojúhelník s pojmenovanými stranami, na které se dá klepnout", "Треугольник с подписанными сторонами, на которые можно нажать")),
        onTap = { p -> spots.entries.minByOrNull { p.dist(it.value.x, it.value.y) }?.takeIf { p.dist(it.value.x, it.value.y) < 40 }?.let { tap(it.key) } },
    ) { c ->
        rampTri(r, c, "α")
        text(spots.getValue(TriSide.HYP).x - 18, spots.getValue(TriSide.HYP).y - 16, t.pick(SIDE_NAME.getValue(TriSide.HYP)), c.load, 13.0, Anchor.END)
        text(spots.getValue(TriSide.ADJ).x, r.a.y + 30, t.pick(SIDE_NAME.getValue(TriSide.ADJ)), c.derived, 13.0)
        text(r.c.x + 20, spots.getValue(TriSide.OPP).y + 5, t.pick(SIDE_NAME.getValue(TriSide.OPP)), c.derived, 13.0, Anchor.START)
        num0?.let { s ->
            val (p, q) = when (s) { TriSide.ADJ -> r.a to r.c; TriSide.OPP -> r.c to r.b; TriSide.HYP -> r.a to r.b }
            line(p.x, p.y, q.x, q.y, c.highlight, 5.0)
        }
        for ((s, p) in spots) tapSpot(p.x, p.y, c, selected = s == num0)
    }
    CardText(L(
        "Slož každý podíl sám: klepni nejdřív na stranu, která je ve zlomku **nahoře**, pak na tu **dole**.",
        "Собери каждое отношение сам: сначала нажми на сторону, которая в дроби **сверху**, потом на ту, что **снизу**.",
    ))
    Todo(
        L("Slož sin α", "Собери sin α") to (round > 0),
        L("Slož cos α", "Собери cos α") to (round > 1),
        L("Slož tg α", "Собери tg α") to (round > 2),
    )
    val done = (0 until minOf(round, 3)).joinToString("   ") { i ->
        val (n, d) = targets[i]
        "${names[i]} = ${t.pick(SIDE_NAME.getValue(n))} / ${t.pick(SIDE_NAME.getValue(d))}"
    }
    if (done.isNotEmpty()) SmallText(L(done, done))
    if (round <= 2) Readout("${names[round]} = ${num0?.let { t.pick(SIDE_NAME.getValue(it)) } ?: "▢"} / ▢")
    msg?.let { Note(Tone.NUDGE, it) }
    if (round > 2) SmallText(L(
        "Pomůcka: **ko**sinus – odvěsna v **ko**ntaktu s úhlem (přilehlá). Sinus – ta, co stojí naproti.",
        "Подсказка: **ко**синус — катет, который **ка**сается угла (прилежащий). Синус — тот, что напротив.",
    ))
}

/** Karta 2: kvíz – sin α v trojúhelníku 3-4-5. */
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
        line(a.x, a.y, cc.x, cc.y, c.derived, 2.6)
        line(cc.x, cc.y, b.x, b.y, c.derived, 2.6)
        line(a.x, a.y, b.x, b.y, c.load, 2.6)
        rightAngleMark(cc, a, b, c)
        angleArc(a.x, a.y, 30.0, 0.0, screenAngle(a, b), "α", c)
        text((a.x + cc.x) / 2, a.y + 22, "4", c.derived, 15.0, bold = true)
        text(cc.x + 14, (cc.y + b.y) / 2 + 5, "3", c.derived, 15.0, Anchor.START, bold = true)
        text((a.x + b.x) / 2 - 14, (a.y + b.y) / 2 - 12, "5", c.load, 15.0, Anchor.END, bold = true)
    }
    Quiz(
        question = L("Kolik je sin α?", "Чему равен sin α?"),
        options = listOf(
            Opt(L("3/5 = 0,6", "3/5 = 0,6"), correct = true),
            Opt(L("4/5 = 0,8", "4/5 = 0,8"), why = L("4 je odvěsna u úhlu α. Která funkce bere přilehlou odvěsnu?", "4 — катет у угла α. Какая функция берёт прилежащий катет?")),
            Opt(L("3/4 = 0,75", "3/4 = 0,75"), why = L("Tady se dělí odvěsna odvěsnou – to je tangens. Čím dělí sinus?", "Здесь катет делится на катет — это тангенс. На что делит синус?")),
            Opt(L("5/3 ≈ 1,67", "5/3 ≈ 1,67"), why = L("Přepona je nejdelší strana, takže sinus nikdy nevyjde větší než 1. Co patří ve zlomku nahoru?", "Гипотенуза — самая длинная сторона, поэтому синус никогда не больше 1. Что в дроби должно быть сверху?")),
        ),
        explain = L(
            "sin α = protilehlá / přepona = 3/5 = 0,6. Tentýž trojúhelník dává cos α = 4/5 = 0,8 a tg α = 3/4 = 0,75.",
            "sin α = противолежащий / гипотенуза = 3/5 = 0,6. Тот же треугольник даёт cos α = 4/5 = 0,8 и tg α = 3/4 = 0,75.",
        ),
        columns = 2,
    )
}

/** Karta 3: sin roste od 0 do 1, cos klesá od 1 do 0. */
@Composable
private fun CardScope.Range() {
    val t = tr()
    var alpha by remember { mutableStateOf(20.0) }
    var at0 by remember { mutableStateOf(false) }
    var at90 by remember { mutableStateOf(false) }
    var at45 by remember { mutableStateOf(false) }
    val s = Trig.sinD(alpha)
    val co = Trig.cosD(alpha)
    Stage(h = 230.0, description = t.pick(L("Trojúhelník s přeponou 1 a sloupce sin a cos", "Треугольник с гипотенузой 1 и столбики sin и cos"))) { c ->
        val a = Pt(40.0, 205.0)
        val len = 170.0
        val b = Pt(a.x + len * co, a.y - len * s)
        val cc = Pt(b.x, a.y)
        line(20.0, 205.0, 250.0, 205.0, c.line, 1.0)
        poly(listOf(a.x to a.y, b.x to b.y, cc.x to cc.y), c.subtle, null, 0.0, alpha = 0.6f)
        if (s > 0.01) line(cc.x, cc.y, b.x, b.y, c.derived, 3.0)
        if (co > 0.01) line(a.x, a.y, cc.x, cc.y, c.derived, 3.0)
        line(a.x, a.y, b.x, b.y, c.load, 2.6)
        if (s > 0.1 && co > 0.1) rightAngleMark(cc, a, b, c)
        angleArc(a.x, a.y, 28.0, 0.0, -alpha, "${alpha.roundToInt()}°", c)
        text((a.x + b.x) / 2 - 12 * s - 6, (a.y + b.y) / 2 - 12 * co, "1", c.load, 14.0, Anchor.END, bold = true)
        // sloupce
        bar(290.0, 30.0, 32.0, 160.0, s, c.derived, c)
        bar(345.0, 30.0, 32.0, 160.0, co, c.derived, c)
        text(306.0, 210.0, "sin", c.derived, 13.0)
        text(361.0, 210.0, "cos", c.derived, 13.0)
        text(286.0, 36.0, "1", c.inkSoft, 11.0, Anchor.END)
        text(286.0, 194.0, "0", c.inkSoft, 11.0, Anchor.END)
    }
    CardText(L(
        "Když má přepona délku 1, je protilehlá odvěsna přímo sin α a přilehlá cos α. Sleduj je od 0° do 90°.",
        "Если гипотенуза равна 1, то противолежащий катет — это прямо sin α, а прилежащий — cos α. Проследи за ними от 0° до 90°.",
    ))
    Todo(
        L("Nastav α = 0° – kolik je sin a kolik cos?", "Поставь α = 0° — чему равны sin и cos?") to at0,
        L("Nastav α = 90°", "Поставь α = 90°") to at90,
        L("Najdi úhel, kde je sin α stejný jako cos α", "Найди угол, при котором sin α равен cos α") to at45,
    )
    ValueSlider(t.pick(L("Úhel α = ${alpha.roundToInt()}°", "Угол α = ${alpha.roundToInt()}°")), alpha.toFloat(), 0f..90f, steps = 17) {
        alpha = ((it / 5).roundToInt() * 5.0).coerceIn(0.0, 90.0)
        when (alpha) {
            0.0 -> at0 = true
            90.0 -> at90 = true
            45.0 -> at45 = true
        }
    }
    Readout("sin ${alpha.roundToInt()}° = ${num(s, 3)} · cos ${alpha.roundToInt()}° = ${num(co, 3)}", ok = alpha == 45.0)
    if (at0 && at90 && at45) SmallText(L(
        "Sinus s úhlem roste od 0 do 1, kosinus klesá od 1 do 0. Ani jeden nikdy nepřekročí 1.",
        "Синус с ростом угла растёт от 0 до 1, косинус убывает от 1 до 0. Ни один из них никогда не превышает 1.",
    ))
}

/** Karta 4: vzorec obráceně – odvěsna = přepona · sin α (žebřík). */
@Composable
private fun CardScope.Ladder() {
    val t = tr()
    Stage(h = 230.0, description = t.pick(L("Žebřík opřený o zeď pod úhlem 60°", "Лестница, прислонённая к стене под углом 60°"))) { c ->
        val wallX = 300.0
        val ground = 205.0
        val len = 170.0
        val foot = Pt(wallX - len * Trig.cosD(60.0), ground)
        val top = Pt(wallX, ground - len * Trig.sinD(60.0))
        rect(wallX, 15.0, 22.0, ground - 15, c.subtle, c.ink, 1.4)
        line(20.0, ground, 380.0, ground, c.ink, 1.6)
        // žebřík: dvě štěpiny a příčky
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
        text(wallX + 30, (ground + top.y) / 2 + 5, if (solvedHere) "4,33 m" else "?", c.derived, 15.0, Anchor.START, bold = true)
    }
    CardText(L(
        "Vzorec jde obrátit: **protilehlá = přepona · sin α**, **přilehlá = přepona · cos α**. Přesně takhle se počítají složky síly.",
        "Формулу можно перевернуть: **противолежащий = гипотенуза · sin α**, **прилежащий = гипотенуза · cos α**. Именно так считают составляющие силы.",
    ))
    NumAnswer(
        question = L(
            "Žebřík dlouhý 5 m svírá se zemí 60° (sin 60° ≈ 0,866; cos 60° = 0,5). Jak vysoko na zdi končí?",
            "Лестница длиной 5 м составляет с землёй 60° (sin 60° ≈ 0,866; cos 60° = 0,5). На какой высоте она упирается в стену?",
        ),
        fields = listOf(NumSpec(L("výška", "высота"), 5 * Trig.sinD(60.0), "m")),
        explain = L(
            "Výška leží naproti úhlu 60°, proto sinus: 5 · sin 60° ≈ 5 · 0,866 ≈ 4,33 m.",
            "Высота лежит напротив угла 60°, поэтому синус: 5 · sin 60° ≈ 5 · 0,866 ≈ 4,33 м.",
        ),
        traps = listOf(
            NumTrap.near(L("To je 5 · cos 60° – vzdálenost paty žebříku od zdi, odvěsna **u** úhlu. Výška leží naproti.", "Это 5 · cos 60° — расстояние от низа лестницы до стены, катет **у** угла. Высота лежит напротив."), 2.5),
            NumTrap(L("Výška nemůže být větší než žebřík. Přeponu sinem násob, neděl.", "Высота не может быть больше лестницы. Гипотенузу на синус умножай, а не дели.")) { v -> v[0]?.let { abs(it) > 5.0 } ?: false },
        ),
        nudge = L("Výška je odvěsna naproti úhlu 60° – která funkce k ní patří?", "Высота — катет напротив угла 60°. Какая функция к нему относится?"),
    )
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
