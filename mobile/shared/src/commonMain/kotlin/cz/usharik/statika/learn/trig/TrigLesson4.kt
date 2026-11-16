package cz.usharik.statika.learn.trig

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import cz.usharik.statika.learn.ActionButton
import cz.usharik.statika.learn.Card
import cz.usharik.statika.learn.Chips
import cz.usharik.statika.learn.CardScope
import cz.usharik.statika.learn.CardText
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
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.force
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.poly
import cz.usharik.statika.learn.rect
import cz.usharik.statika.learn.rightAngleMark
import cz.usharik.statika.learn.screenAngle
import cz.usharik.statika.learn.tapSpot
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import kotlinx.coroutines.launch

/*
 * Kurz trigonometrie – lekce 4 (id 104): úhly 30°, 45°, 60° zpaměti – půlka čtverce, půlka rovnostranného
 * trojúhelníku a pomůcka √0/2 … √4/2.
 */

/** Karta 0: rozpůlený čtverec → trojúhelník 45°–45°–90°. */
@Composable
private fun CardScope.HalfSquare() {
    val t = tr()
    var cut by remember { mutableStateOf(false) }
    val anim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val x0 = 130.0
    val y0 = 40.0
    val s = 150.0
    Stage(h = 230.0, description = t.pick(L("Čtverec o straně 1 a jeho úhlopříčka", "Квадрат со стороной 1 и его диагональ"))) { c ->
        val p = anim.value
        // horní trojúhelník po rozpůlení zbledne
        poly(listOf(x0 to y0, x0 + s to y0, x0 to y0 + s), c.subtle, null, 0.0, alpha = 1f - 0.7f * p)
        line(x0, y0, x0 + s, y0, c.ink, 2.0, alpha = 1f - 0.7f * p)
        line(x0, y0, x0, y0 + s, c.ink, 2.0, alpha = 1f - 0.7f * p)
        poly(listOf(x0 + s to y0, x0 + s to y0 + s, x0 to y0 + s), c.subtle, null, 0.0, alpha = 0.8f)
        line(x0, y0 + s, x0 + s, y0 + s, c.derived, 2.6)
        line(x0 + s, y0 + s, x0 + s, y0, c.derived, 2.6)
        if (p > 0f) line(x0, y0 + s, x0 + (s * p).toDouble(), y0 + s - (s * p).toDouble(), c.load, 2.8)
        text(x0 + s / 2, y0 + s + 22, "1", c.derived, 15.0, bold = true)
        text(x0 + s + 14, y0 + s / 2 + 5, "1", c.derived, 15.0, Anchor.START, bold = true)
        if (p >= 1f) {
            rightAngleMark(Pt(x0 + s, y0 + s), Pt(x0, y0 + s), Pt(x0 + s, y0), c)
            angleArc(x0, y0 + s, 30.0, 0.0, -45.0, "45°", c)
            angleArc(x0 + s, y0, 30.0, 90.0, 135.0, "45°", c)
            text(x0 + s / 2 - 16, y0 + s / 2 - 8, "√2 ≈ 1,41", c.load, 14.0, Anchor.END, bold = true)
        }
    }
    CardText(L(
        "Čtverec o straně 1 rozpůlený úhlopříčkou dá trojúhelník 45°–45°–90°. Úhlopříčka měří √2 ≈ 1,41.",
        "Квадрат со стороной 1, разрезанный по диагонали, даёт треугольник 45°–45°–90°. Диагональ равна √2 ≈ 1,41.",
    ))
    SmallText(L(
        "√ je **odmocnina**: √25 = 5, protože 5 · 5 = 25. A √2 ≈ 1,41, protože 1,41 · 1,41 ≈ 2. Proč úhlopříčka vyjde právě √2, ukáže lekce 5.",
        "√ — **квадратный корень**: √25 = 5, потому что 5 · 5 = 25. А √2 ≈ 1,41, так как 1,41 · 1,41 ≈ 2. Почему диагональ равна именно √2, покажет урок 5.",
    ))
    Todo(L("Klepni na „Rozpůl čtverec“", "Нажми «Разрежь квадрат»") to cut)
    if (!cut) ActionButton(L("Rozpůl čtverec", "Разрежь квадрат")) {
        scope.launch {
            anim.animateTo(1f, tween(700))
            cut = true
        }
    }
    Quiz(
        question = L("Kolik je tg 45°?", "Чему равен tg 45°?"),
        options = listOf(
            Opt(L("1", "1"), correct = true),
            Opt(L("0,707", "0,707"), why = L("0,707 = 1 / 1,41 je odvěsna dělená přeponou, tedy sin 45°. Čím dělí tangens?", "0,707 = 1 / 1,41 — катет, делённый на гипотенузу, то есть sin 45°. На что делит тангенс?")),
            Opt(L("0,5", "0,5"), why = L("Obě odvěsny jsou stejně dlouhé. Kolik vyjde, když číslo vydělíš stejným číslem?", "Оба катета одинаковы. Сколько получится, если число разделить на такое же?")),
            Opt(L("1,41", "1,41"), why = L("√2 je délka přepony. V tangentu přepona není.", "√2 — длина гипотенузы. В тангенсе гипотенузы нет.")),
        ),
        explain = L(
            "Odvěsny jsou stejné, takže tg 45° = 1. A sin 45° = cos 45° = 1 / 1,41 ≈ 0,707 (v tabulkách se píše √2/2 – je to totéž číslo).",
            "Катеты равны, поэтому tg 45° = 1. А sin 45° = cos 45° = 1 / 1,41 ≈ 0,707 (в таблицах пишут √2/2 — это то же число).",
        ),
        columns = 2,
    )
}

/** Karta 1: rozpůlený rovnostranný trojúhelník → 30°–60°–90° se stranami 1, √3, 2. */
@Composable
private fun CardScope.HalfTriangle() {
    val t = tr()
    var cut by remember { mutableStateOf(false) }
    var sel by remember { mutableStateOf<Int?>(null) } // 30 nebo 60
    var saw30 by remember { mutableStateOf(false) }
    var saw60 by remember { mutableStateOf(false) }
    val anim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val L0 = Pt(110.0, 205.0)
    val R0 = Pt(290.0, 205.0)
    val top = Pt(200.0, 205.0 - 180.0 * Trig.sinD(60.0))
    val foot = Pt(200.0, 205.0)
    // kroužky na klepnutí přesně na popiscích úhlů (popisek leží v ose oblouku, 16 jednotek za ním)
    fun labelAt(v: Pt, a1: Double, a2: Double, r: Double): Pt {
        var sweep = ((a2 - a1) % 360 + 540) % 360 - 180
        val m = (a1 + sweep / 2) * kotlin.math.PI / 180
        return Pt(v.x + (r + 16) * kotlin.math.cos(m), v.y + (r + 16) * kotlin.math.sin(m))
    }
    val spot30 = labelAt(top, 90.0, screenAngle(top, L0), 30.0)
    val spot60 = labelAt(L0, 0.0, screenAngle(L0, top), 22.0)
    Stage(
        h = 230.0,
        description = t.pick(L("Rovnostranný trojúhelník o straně 2 a jeho výška", "Равносторонний треугольник со стороной 2 и его высота")),
        onTap = { p ->
            if (!cut) return@Stage
            if (p.dist(spot30.x, spot30.y) < 36) { sel = 30; saw30 = true } else if (p.dist(spot60.x, spot60.y) < 36) { sel = 60; saw60 = true }
        },
    ) { c ->
        val pr = anim.value
        // pravá polovina po rozpůlení zbledne
        poly(listOf(top.x to top.y, R0.x to R0.y, foot.x to foot.y), c.subtle, null, 0.0, alpha = 0.8f - 0.6f * pr)
        poly(listOf(top.x to top.y, L0.x to L0.y, foot.x to foot.y), c.subtle, null, 0.0, alpha = 0.8f)
        line(top.x, top.y, R0.x, R0.y, c.ink, 2.0, alpha = 1f - 0.7f * pr)
        line(foot.x, foot.y, R0.x, R0.y, c.ink, 2.0, alpha = 1f - 0.7f * pr)
        line(L0.x, L0.y, top.x, top.y, if (cut) c.load else c.ink, 2.6)
        line(L0.x, L0.y, foot.x, foot.y, if (cut) c.derived else c.ink, 2.6)
        text(L0.x - 4 + (top.x - L0.x) / 2 - 12, (L0.y + top.y) / 2 - 4, "2", if (cut) c.load else c.ink, 15.0, Anchor.END, bold = true)
        if (!cut) {
            text((top.x + R0.x) / 2 + 14, (top.y + R0.y) / 2, "2", c.ink, 15.0, Anchor.START, bold = true)
            text((L0.x + R0.x) / 2 + 45, L0.y + 22, "2", c.ink, 15.0, bold = true)
            angleArc(L0.x, L0.y, 30.0, 0.0, -60.0, "60°", c)
            angleArc(R0.x, R0.y, 30.0, 180.0, 240.0, "60°", c)
        }
        if (pr > 0f) line(top.x, top.y, top.x, top.y + (foot.y - top.y) * pr, c.derived, 2.6)
        if (cut) {
            rightAngleMark(foot, L0, top, c)
            text((L0.x + foot.x) / 2, L0.y + 22, "1", c.derived, 15.0, bold = true)
            text(foot.x + 10, (foot.y + top.y) / 2 + 5, "√3 ≈ 1,73", c.derived, 14.0, Anchor.START, bold = true)
            tapSpot(spot30.x, spot30.y, c, sel == 30, 17.0)
            tapSpot(spot60.x, spot60.y, c, sel == 60, 17.0)
            angleArc(top.x, top.y, 30.0, 90.0, screenAngle(top, L0), "30°", c, if (sel == 30) c.highlight else c.inkSoft)
            angleArc(L0.x, L0.y, 22.0, 0.0, screenAngle(L0, top), "60°", c, if (sel == 60) c.highlight else c.inkSoft)
        }
    }
    CardText(L(
        "Rovnostranný trojúhelník o straně 2 má všechny úhly 60°. Výška ho rozpůlí na trojúhelník 30°–60°–90° se stranami 1, √3 ≈ 1,73 a 2.",
        "У равностороннего треугольника со стороной 2 все углы по 60°. Высота делит его на треугольник 30°–60°–90° со сторонами 1, √3 ≈ 1,73 и 2.",
    ))
    Todo(
        L("Klepni na „Rozpůl trojúhelník“", "Нажми «Разрежь треугольник»") to cut,
        L("Klepni na úhel 30° (kroužek nahoře) a přečti sin a cos", "Нажми на угол 30° (кружок наверху) и прочитай sin и cos") to saw30,
        L("Klepni na úhel 60° (kroužek vlevo dole)", "Нажми на угол 60° (кружок слева внизу)") to saw60,
    )
    if (!cut) ActionButton(L("Rozpůl trojúhelník", "Разрежь треугольник")) {
        scope.launch {
            anim.animateTo(1f, tween(700))
            cut = true
        }
    }
    when (sel) {
        30 -> Readout("sin 30° = 1/2 = 0,5 · cos 30° = √3/2 ≈ 0,866")
        60 -> Readout("sin 60° = √3/2 ≈ 0,866 · cos 60° = 1/2 = 0,5")
    }
    if (saw30) SmallText(L(
        "Zapamatuj si: naproti 30° leží **polovina přepony**. Proto sin 30° = 0,5.",
        "Запомни: напротив угла 30° лежит **половина гипотенузы**. Поэтому sin 30° = 0,5.",
    ))
}

private val TABLE_ANGLES = listOf(0, 30, 45, 60, 90)
private val TABLE_VALUES = listOf("0", "0,5", "0,707", "0,866", "1")

/** Karta 2: tabulka (√0)/2 … (√4)/2 – žák u každého úhlu vybere hodnotu, pak se z ní „otočením“ udělá řádek cos. */
@Composable
private fun CardScope.SqrtTable() {
    val t = tr()
    var filled by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    var wrong by remember { mutableStateOf<L?>(null) }
    val anim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val x0 = 70.0
    val cw = 64.0
    val rowH = 62.0
    val yHead = 20.0
    val ySin = 52.0
    val yCos = ySin + rowH
    Stage(h = 190.0, description = t.pick(L("Tabulka hodnot sin a cos pro 0°, 30°, 45°, 60°, 90°", "Таблица значений sin и cos для 0°, 30°, 45°, 60°, 90°"))) { c ->
        for ((i, a) in TABLE_ANGLES.withIndex()) text(x0 + cw * i + cw / 2, yHead + 18, "$a°", c.inkSoft, 14.0, bold = true)
        text(x0 - 10, ySin + rowH / 2 + 5, "sin", c.ink, 15.0, Anchor.END, bold = true)
        text(x0 - 10, yCos + rowH / 2 + 5, "cos", c.ink, 15.0, Anchor.END, bold = true)
        for (i in 0 until 5) {
            rect(x0 + cw * i, ySin, cw, rowH, if (i < filled) c.subtle else c.paper, if (i == filled) c.highlight else c.line, if (i == filled) 2.0 else 1.0)
            rect(x0 + cw * i, yCos, cw, rowH, c.paper, c.line, 1.0)
            if (i < filled) {
                text(x0 + cw * i + cw / 2, ySin + 26, "(√$i)/2", c.derived, 14.0, bold = true)
                text(x0 + cw * i + cw / 2, ySin + 48, TABLE_VALUES[i], c.inkSoft, 12.0)
            } else if (i == filled) {
                text(x0 + cw * i + cw / 2, ySin + rowH / 2 + 6, "?", c.highlight, 18.0, bold = true)
            }
            val j = 4 - i
            if (anim.value * 5 > i) {
                text(x0 + cw * i + cw / 2, yCos + 26, "(√$j)/2", c.derived, 14.0, bold = true)
                text(x0 + cw * i + cw / 2, yCos + 48, TABLE_VALUES[j], c.inkSoft, 12.0)
            }
        }
    }
    CardText(L(
        "Pomůcka: sin 0°, 30°, 45°, 60°, 90° je **(√0)/2, (√1)/2, (√2)/2, (√3)/2, (√4)/2** – odmocnina jen nahoře, pod ní počítáš 0, 1, 2, 3, 4. Protože √0 = 0, √1 = 1 a √4 = 2, začíná řada 0, pak 0,5 a končí 1.",
        "Приём: sin 0°, 30°, 45°, 60°, 90° — это **(√0)/2, (√1)/2, (√2)/2, (√3)/2, (√4)/2** — корень только сверху, под ним считаешь 0, 1, 2, 3, 4. Так как √0 = 0, √1 = 1 и √4 = 2, ряд начинается с 0, потом 0,5 и заканчивается 1.",
    ))
    Todo(
        L("Doplň řádek sin: u každého úhlu vyber správnou hodnotu (${filled.coerceAtMost(5)}/5)", "Заполни строку sin: для каждого угла выбери верное значение (${filled.coerceAtMost(5)}/5)") to (filled == 5),
        L("Klepni na „Otoč pořadí“ – vznikne řádek cos", "Нажми «Переверни порядок» — получится строка cos") to flipped,
    )
    if (filled < 5) {
        val i = filled
        // tři možnosti: správná a dvě jiné hodnoty z tabulky (pořadí podle úhlu, ať se nedá jen klepat na první)
        val opts = listOf(i, (i + 2) % 5, (i + 4) % 5).sortedBy { (it * 7 + i * 3) % 5 }
        Readout("sin ${TABLE_ANGLES[i]}° = (√$i)/2 = ?")
        Chips(opts.map { L(TABLE_VALUES[it], TABLE_VALUES[it]) }) { k ->
            if (opts[k] == i) {
                filled++
                wrong = null
            } else wrong = L(
                "To je hodnota pro ${TABLE_ANGLES[opts[k]]}°. Pod odmocninou je teď $i: √$i ${if (i == 2) "≈ 1,41" else if (i == 3) "≈ 1,73" else "= ${listOf(0, 1, 0, 0, 2)[i]}"}, a to ještě vyděl dvěma.",
                "Это значение для ${TABLE_ANGLES[opts[k]]}°. Под корнем сейчас $i: √$i ${if (i == 2) "≈ 1,41" else if (i == 3) "≈ 1,73" else "= ${listOf(0, 1, 0, 0, 2)[i]}"}, и это ещё раздели на два.",
            )
        }
    }
    wrong?.let { Note(Tone.NUDGE, it) }
    if (!flipped) ActionButton(L("Otoč pořadí", "Переверни порядок"), enabled = filled == 5) {
        scope.launch {
            anim.animateTo(1f, tween(900))
            flipped = true
        }
    }
    if (filled < 5) SmallText(L("„Otoč pořadí“ jde použít, až bude řádek sin celý.", "«Переверни порядок» сработает, когда строка sin будет заполнена."))
    if (flipped) {
        SmallText(L(
            "Kosinus má stejná čísla pozpátku. Vyčísleno – sin: 0 – 0,5 – 0,707 – 0,866 – 1; cos: 1 – 0,866 – 0,707 – 0,5 – 0.",
            "У косинуса те же числа в обратном порядке. В десятичных — sin: 0 – 0,5 – 0,707 – 0,866 – 1; cos: 1 – 0,866 – 0,707 – 0,5 – 0.",
        ))
        SmallText(L(
            "A tangens = sin / cos: tg 30° ≈ 0,577, tg 45° = 1, tg 60° ≈ 1,732 (tg 90° není definován).",
            "А тангенс = sin / cos: tg 30° ≈ 0,577, tg 45° = 1, tg 60° ≈ 1,732 (tg 90° не определён).",
        ))
    }
}

/** Karta 3: kvíz – cos 60°. */
@Composable
private fun CardScope.CheckCos60() {
    Quiz(
        question = L("Kolik je cos 60°?", "Чему равен cos 60°?"),
        options = listOf(
            Opt(L("0,5", "0,5"), correct = true),
            Opt(L("0,866", "0,866"), why = L("0,866 = √3/2 je sin 60°. Kosinus má tabulku pozpátku – co je v ní pod 60°?", "0,866 = √3/2 — это sin 60°. У косинуса таблица в обратном порядке — что в ней под 60°?")),
            Opt(L("0,707", "0,707"), why = L("0,707 patří k 45°, kde jsou sin a cos stejné.", "0,707 относится к 45°, где sin и cos равны.")),
            Opt(L("1", "1"), why = L("cos je 1 jen pro 0° – když přilehlá odvěsna splyne s přeponou.", "cos равен 1 только при 0° — когда прилежащий катет сливается с гипотенузой.")),
        ),
        explain = L(
            "cos 60° = sin 30° = 0,5: u úhlu 60° leží kratší odvěsna, polovina přepony.",
            "cos 60° = sin 30° = 0,5: у угла 60° лежит короткий катет, половина гипотенузы.",
        ),
        columns = 2,
    )
}

/** Karta 4: složky síly bez kalkulačky (20 kN, 30°). */
@Composable
private fun CardScope.NoCalc() {
    val t = tr()
    val tip = Pt(250.0, 170.0)
    val len = 150.0
    val a = 30.0
    val tail = Pt(tip.x - len * Trig.cosD(a), tip.y - len * Trig.sinD(a))
    Stage(h = 220.0, description = t.pick(L("Šikmá síla 20 kN pod úhlem 30° k nosníku", "Наклонная сила 20 кН под углом 30° к балке"))) { c ->
        rect(30.0, tip.y, 340.0, 12.0, c.beam, c.ink, 1.4)
        if (solvedHere) {
            line(tail.x, tail.y, tail.x, tip.y, c.derived, 1.0, dash = true)
            line(tail.x, tail.y, tip.x, tail.y, c.derived, 1.0, dash = true)
            force(tail.x, tail.y - 8, tip.x, tail.y - 8, c.derived, 2.4)
            force(tail.x - 8, tail.y, tail.x - 8, tip.y, c.derived, 2.4)
            text((tail.x + tip.x) / 2, tail.y - 18, " = 17,32 kN", c.derived, 13.0, sym = "F_x")
            text(tail.x - 16, (tail.y + tip.y) / 2 + 5, " = 10 kN", c.derived, 13.0, Anchor.END, sym = "F_y")
        }
        force(tail.x, tail.y, tip.x, tip.y, c.load, 3.0)
        text((tail.x + tip.x) / 2 + 8, (tail.y + tip.y) / 2 - 16, " = 20 kN", c.load, 14.0, Anchor.START, sym = "F")
        angleArc(tip.x, tip.y, 40.0, 180.0, screenAngle(tip, tail), "30°", c)
    }
    NumAnswer(
        question = L(
            "Šikmou sílu [[F]] = 20 kN nahradíme dvěma: [[F_x]] podél nosníku (vodorovně) a [[F_y]] kolmo na něj (svisle). Síla svírá s nosníkem 30°. Jak velké jsou [[F_x]] a [[F_y]]? (bez kalkulačky)",
            "Наклонную силу [[F]] = 20 кН заменяем двумя: [[F_x]] — вдоль балки (по горизонтали) и [[F_y]] — поперёк (по вертикали). Сила составляет с балкой 30°. Чему равны [[F_x]] и [[F_y]]? (без калькулятора)",
        ),
        fields = listOf(
            NumSpec(L("[[F_x]]", "[[F_x]]"), 20 * Trig.cosD(30.0), "kN"),
            NumSpec(L("[[F_y]]", "[[F_y]]"), 10.0, "kN"),
        ),
        explain = L(
            "[[F_y]] leží naproti úhlu 30° – je to polovina síly: 20 · 0,5 = 10 kN. [[F_x]] ≈ 20 · 0,866 ≈ 17,32 kN.",
            "[[F_y]] лежит напротив угла 30° — это половина силы: 20 · 0,5 = 10 кН. [[F_x]] ≈ 20 · 0,866 ≈ 17,32 кН.",
        ),
        traps = listOf(
            NumTrap.near(L("Složky jsou prohozené. Která z nich leží u úhlu 30°?", "Составляющие перепутаны местами. Какая из них лежит у угла 30°?"), 10.0, 17.32),
            NumTrap(L("Složka nemůže být stejně velká jako celá síla – je to jen její část. Sílu vynásob sinem nebo kosinem.", "Составляющая не может равняться всей силе — это только её часть. Умножь силу на синус или косинус.")) { v ->
                v.any { it != null && kotlin.math.abs(kotlin.math.abs(it) - 20.0) < 0.2 }
            },
        ),
        nudge = L("Pomůže tabulka: sin 30° = 0,5, cos 30° ≈ 0,866. Která složka leží u úhlu?", "Поможет таблица: sin 30° = 0,5, cos 30° ≈ 0,866. Какая составляющая лежит у угла?"),
    )
}

internal fun trigLesson4() = Lesson(
    id = 104,
    title = L("Úhly, které se vyplatí umět zpaměti", "Углы, которые стоит помнить"),
    summary = L("30°, 45°, 60° bez kalkulačky – ze dvou trojúhelníků.", "30°, 45°, 60° без калькулятора — из двух треугольников."),
    cards = listOf(
        Card { HalfSquare() },
        Card { HalfTriangle() },
        Card { SqrtTable() },
        Card { CheckCos60() },
        Card { NoCalc() },
    ),
)
