package cz.usharik.statika.learn.trig

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cz.usharik.statika.learn.Card
import cz.usharik.statika.learn.CardScope
import cz.usharik.statika.learn.CardText
import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Lesson
import cz.usharik.statika.learn.Note
import cz.usharik.statika.learn.NumSpec
import cz.usharik.statika.learn.NumTrap
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.learn.Pt
import cz.usharik.statika.learn.SmallText
import cz.usharik.statika.learn.Stage
import cz.usharik.statika.learn.Step
import cz.usharik.statika.learn.Steps
import cz.usharik.statika.learn.angleArc
import cz.usharik.statika.learn.force
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.numStep
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.poly
import cz.usharik.statika.learn.quizStep
import cz.usharik.statika.learn.rect
import cz.usharik.statika.learn.rightAngleMark
import cz.usharik.statika.learn.screenAngle
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import kotlinx.coroutines.delay

/*
 * Kurz trigonometrie – lekce 4 (id 104): úhly 30°, 45°, 60° zpaměti – půlka čtverce, půlka rovnostranného
 * trojúhelníku a tabulka √0/2 … √4/2.
 */

/** Karta 0: rozpůlený čtverec → trojúhelník 45°–45°–90°. */
@Composable
private fun CardScope.HalfSquare() {
    val t = tr()
    val anim = remember { Animatable(0f) }
    var cut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(700)
        anim.animateTo(1f, tween(900))
        delay(300)
        cut = true
    }
    val tgQ = quizStep(
        question = L("Kolik je tg 45°?", "Чему равен tg 45°?"),
        options = listOf(
            Opt(L("1", "1"), correct = true),
            Opt(L("0,707", "0,707"), why = L("0,707 je odvěsna dělená přeponou, tedy sin 45°. Tangens dělí odvěsnu odvěsnou.", "0,707 — катет, делённый на гипотенузу, то есть sin 45°. Тангенс делит катет на катет.")),
            Opt(L("0,5", "0,5"), why = L("Obě odvěsny měří 1. Kolik je 1 / 1?", "Оба катета равны 1. Сколько будет 1 / 1?")),
            Opt(L("√2", "√2"), why = L("√2 je délka přepony. V tangentu přepona není.", "√2 — длина гипотенузы. В тангенсе гипотенузы нет.")),
        ),
        explain = L(
            "Obě odvěsny jsou stejné, takže tg 45° = 1 / 1 = 1. Přeponu – úhlopříčku d – dá Pythagorova věta (lekce 5): d² = 1² + 1² = 2, tedy d = √2 ≈ 1,41.",
            "Катеты равны, поэтому tg 45° = 1 / 1 = 1. Гипотенузу — диагональ d — даёт теорема Пифагора (урок 5): d² = 1² + 1² = 2, значит d = √2 ≈ 1,41.",
        ),
        columns = 4,
    )
    val sinQ = quizStep(
        question = L("Přepona měří √2. Jaký podíl je sin 45°?", "Гипотенуза равна √2. Какое отношение — sin 45°?"),
        options = listOf(
            Opt(L("1/√2", "1/√2"), correct = true),
            Opt(L("√2/1", "√2/1"), why = L("Obrácený zlomek: přepona patří dolů. Sinus nikdy nepřekročí 1, a √2 ≈ 1,41.", "Перевёрнутая дробь: гипотенуза стоит снизу. Синус никогда не больше 1, а √2 ≈ 1,41.")),
            Opt(L("1/2", "1/2"), why = L("1/2 by vyšlo, kdyby přepona měřila 2. Tady měří √2.", "1/2 получилось бы, если бы гипотенуза была 2. Здесь она √2.")),
        ),
        explain = L(
            "sin 45° = cos 45° = 1/√2 = √2/2 ≈ 0,707.",
            "sin 45° = cos 45° = 1/√2 = √2/2 ≈ 0,707.",
        ),
        columns = 3,
    )
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
            text(x0 + s / 2 - 16, y0 + s / 2 - 8, if (tgQ.done) "d = √2 ≈ 1,41" else "d", c.load, 14.0, Anchor.END, bold = true)
        }
    }
    CardText(L(
        "Úhlopříčka rozdělí čtverec na dva shodné pravoúhlé trojúhelníky. Strana čtverce je 1.",
        "Диагональ делит квадрат на два равных прямоугольных треугольника. Сторона квадрата равна 1.",
    ))
    Steps(
        Step(
            L("Sleduj, jak úhlopříčka rozdělí čtverec", "Проследи, как диагональ делит квадрат"), cut,
            result = { Note(Tone.INFO, L("Vznikl trojúhelník 45°–45°–90° se dvěma odvěsnami délky 1.", "Получился треугольник 45°–45°–90° с двумя катетами длины 1.")) },
        ) {
            SmallText(L("Úhlopříčka půlí čtverec i jeho pravé úhly…", "Диагональ делит пополам квадрат и его прямые углы…"))
        },
        tgQ,
        sinQ,
    )
}

/** Karta 1: rozpůlený rovnostranný trojúhelník → 30°–60°–90° se stranami 1, √3, 2. */
@Composable
private fun CardScope.HalfTriangle() {
    val t = tr()
    val anim = remember { Animatable(0f) }
    var cut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(700)
        anim.animateTo(1f, tween(900))
        delay(300)
        cut = true
    }
    val values = listOf("1/2", "√3/2", "1/√3")
    val sinQ = quizStep(
        question = L("Kolik je sin 30°?", "Чему равен sin 30°?"),
        options = listOf(
            Opt(L(values[0], values[0]), correct = true),
            Opt(L(values[1], values[1]), why = L("√3 je odvěsna, která z vrcholu 30° vychází – přilehlá. To by byl cos 30°.", "√3 — катет, выходящий из вершины угла 30°, прилежащий. Это был бы cos 30°.")),
            Opt(L(values[2], values[2]), why = L("Odvěsna dělená odvěsnou je tangens. Sinus dělí přeponou 2.", "Катет, делённый на катет, — это тангенс. Синус делит на гипотенузу 2.")),
        ),
        explain = L("Naproti 30° leží odvěsna 1, přepona je 2: sin 30° = 1/2.", "Напротив 30° лежит катет 1, гипотенуза равна 2: sin 30° = 1/2."),
        columns = 3,
    )
    val cosQ = quizStep(
        question = L("Ve stejném trojúhelníku: kolik je cos 60°?", "В том же треугольнике: чему равен cos 60°?"),
        options = listOf(
            Opt(L(values[0], values[0]), correct = true),
            Opt(L(values[1], values[1]), why = L("√3 leží naproti 60° – √3/2 je sin 60°. Kosinus bere odvěsnu, která z vrcholu 60° vychází.", "√3 лежит напротив 60° — √3/2 это sin 60°. Косинус берёт катет, выходящий из вершины угла 60°.")),
            Opt(L(values[2], values[2]), why = L("Kosinus dělí přeponou (2), ne odvěsnou.", "Косинус делит на гипотенузу (2), а не на катет.")),
        ),
        explain = L(
            "Strana délky 1 leží naproti 30° a zároveň u úhlu 60°. Proto sin 30° = cos 60° = 1/2. Druhá odvěsna √3 dává sin 60° = cos 30° = √3/2 ≈ 0,866.",
            "Сторона длины 1 лежит напротив 30° и одновременно прилежит к 60°. Поэтому sin 30° = cos 60° = 1/2. Другой катет √3 даёт sin 60° = cos 30° = √3/2 ≈ 0,866.",
        ),
        columns = 3,
    )
    val focus = if (!cut) null else if (!sinQ.done) 30 else 60
    val l0 = Pt(110.0, 205.0)
    val r0 = Pt(290.0, 205.0)
    val top = Pt(200.0, 205.0 - 180.0 * Trig.sinD(60.0))
    val foot = Pt(200.0, 205.0)
    Stage(h = 230.0, description = t.pick(L("Rovnostranný trojúhelník o straně 2 a jeho výška", "Равносторонний треугольник со стороной 2 и его высота"))) { c ->
        val pr = anim.value
        // pravá polovina po rozpůlení zbledne, ale zůstane vidět
        poly(listOf(top.x to top.y, r0.x to r0.y, foot.x to foot.y), c.subtle, null, 0.0, alpha = 0.8f - 0.6f * pr)
        poly(listOf(top.x to top.y, l0.x to l0.y, foot.x to foot.y), c.subtle, null, 0.0, alpha = 0.8f)
        line(top.x, top.y, r0.x, r0.y, c.ink, 2.0, alpha = 1f - 0.6f * pr)
        line(foot.x, foot.y, r0.x, r0.y, c.ink, 2.0, alpha = 1f - 0.6f * pr)
        line(l0.x, l0.y, top.x, top.y, if (cut) c.load else c.ink, 2.6)
        line(l0.x, l0.y, foot.x, foot.y, if (cut) c.derived else c.ink, 2.6)
        text(l0.x - 4 + (top.x - l0.x) / 2 - 12, (l0.y + top.y) / 2 - 4, "2", if (cut) c.load else c.ink, 15.0, Anchor.END, bold = true)
        if (!cut) {
            text((top.x + r0.x) / 2 + 14, (top.y + r0.y) / 2, "2", c.ink, 15.0, Anchor.START, bold = true)
            text((l0.x + r0.x) / 2 + 45, l0.y + 22, "2", c.ink, 15.0, bold = true)
            angleArc(l0.x, l0.y, 30.0, 0.0, -60.0, "60°", c)
            angleArc(r0.x, r0.y, 30.0, 180.0, 240.0, "60°", c)
        }
        if (pr > 0f) line(top.x, top.y, top.x, top.y + (foot.y - top.y) * pr, c.derived, 2.6)
        if (cut) {
            rightAngleMark(foot, l0, top, c)
            text((l0.x + foot.x) / 2, l0.y + 22, "1", c.derived, 15.0, bold = true)
            text(foot.x + 10, (foot.y + top.y) / 2 + 5, "√3 ≈ 1,73", c.derived, 14.0, Anchor.START, bold = true)
            angleArc(top.x, top.y, 30.0, 90.0, screenAngle(top, l0), "30°", c, if (focus == 30) c.highlight else c.inkSoft)
            angleArc(l0.x, l0.y, 22.0, 0.0, screenAngle(l0, top), "60°", c, if (focus == 60) c.highlight else c.inkSoft)
        }
    }
    CardText(L(
        "Rovnostranný trojúhelník o straně 2 má tři úhly po 60°. Jeho výška rozdělí základnu i horní úhel na poloviny.",
        "В равностороннем треугольнике со стороной 2 каждый угол равен 60°. Высота делит основание и угол при вершине пополам.",
    ))
    Steps(
        Step(
            L("Sleduj, jak výška rozdělí trojúhelník", "Проследи, как высота делит треугольник"), cut,
            result = {
                Note(Tone.INFO, L(
                    "Vznikl trojúhelník 30°–60°–90°: přepona 2, kratší odvěsna 1 (půlka základny), delší √(2² − 1²) = √3 ≈ 1,73.",
                    "Получился треугольник 30°–60°–90°: гипотенуза 2, короткий катет 1 (половина основания), длинный √(2² − 1²) = √3 ≈ 1,73.",
                ))
            },
        ) {
            SmallText(L("Výška půlí základnu 2 na 1 a 1…", "Высота делит основание 2 на 1 и 1…"))
        },
        sinQ,
        cosQ,
    )
}

private val TABLE_ANGLES = listOf(0, 30, 45, 60, 90)
private val TABLE_EXACT = listOf("0", "1/2", "√2/2", "√3/2", "1")
private val TABLE_DEC = listOf("0", "0,5", "≈ 0,707", "≈ 0,866", "1")

/** Karta 2: tabulka sin pro 0°…90° po jednom políčku, pak řádek cos jako tytéž hodnoty pozpátku. */
@Composable
private fun CardScope.SqrtTable() {
    val t = tr()
    val anim = remember { Animatable(0f) }
    val hint = L(
        "Pomůcka: sin 0°, 30°, 45°, 60°, 90° = √0/2, √1/2, √2/2, √3/2, √4/2 – pod odmocninou počítáš 0, 1, 2, 3, 4.",
        "Подсказка: sin 0°, 30°, 45°, 60°, 90° = √0/2, √1/2, √2/2, √3/2, √4/2 — под корнем считаешь 0, 1, 2, 3, 4.",
    )
    fun o(i: Int, why: L? = null) = Opt(L(TABLE_EXACT[i] + if (i in 2..3) " ≈ " + TABLE_DEC[i].removePrefix("≈ ") else "", TABLE_EXACT[i] + if (i in 2..3) " ≈ " + TABLE_DEC[i].removePrefix("≈ ") else ""), why == null, why)
    val q0 = quizStep(
        L("Doplň sin 0°", "Заполни sin 0°"),
        listOf(
            o(0),
            o(1, L("1/2 je sin 30°. Při 0° leží úsečka vodorovně – její svislý průmět je nulový.", "1/2 — это sin 30°. При 0° отрезок лежит горизонтально — его вертикальная проекция нулевая.")),
            o(4, L("1 je sin 90°, úsečka svisle. Při 0° leží vodorovně.", "1 — это sin 90°, отрезок вертикален. При 0° он лежит горизонтально.")),
        ),
        L("sin 0° = 0: protilehlá odvěsna se smrskla na nulu.", "sin 0° = 0: противолежащий катет сжался до нуля."), hint, columns = 3,
    )
    val q1 = quizStep(
        L("Doplň sin 30°", "Заполни sin 30°"),
        listOf(
            o(0, L("0 je sin 0°. Vzpomeň si na půlku rovnostranného trojúhelníku.", "0 — это sin 0°. Вспомни половину равностороннего треугольника.")),
            o(1),
            o(3, L("√3/2 je sin 60°. Naproti 30° leží kratší odvěsna – polovina přepony.", "√3/2 — это sin 60°. Напротив 30° лежит короткий катет — половина гипотенузы.")),
        ),
        L("sin 30° = 1/2: naproti 30° leží polovina přepony.", "sin 30° = 1/2: напротив 30° лежит половина гипотенузы."), hint, columns = 3,
    )
    val q2 = quizStep(
        L("Doplň sin 45°", "Заполни sin 45°"),
        listOf(
            o(1, L("1/2 je sin 30°. U 45° jsou obě odvěsny 1 a přepona √2.", "1/2 — это sin 30°. При 45° оба катета равны 1, гипотенуза √2.")),
            o(2),
            o(3, L("√3/2 je sin 60°. U 45° jsou obě odvěsny 1 a přepona √2.", "√3/2 — это sin 60°. При 45° оба катета равны 1, гипотенуза √2.")),
        ),
        L("sin 45° = 1/√2 = √2/2 ≈ 0,707 – půlka čtverce.", "sin 45° = 1/√2 = √2/2 ≈ 0,707 — половина квадрата."), hint, columns = 1,
    )
    val q3 = quizStep(
        L("Doplň sin 60°", "Заполни sin 60°"),
        listOf(
            o(1, L("1/2 je sin 30°. Naproti 60° leží delší odvěsna √3, přepona je 2.", "1/2 — это sin 30°. Напротив 60° лежит длинный катет √3, гипотенуза 2.")),
            o(2, L("√2/2 patří k 45°, kde jsou odvěsny stejné.", "√2/2 относится к 45°, где катеты равны.")),
            o(3),
        ),
        L("sin 60° = √3/2 ≈ 0,866: naproti 60° leží odvěsna √3, přepona je 2.", "sin 60° = √3/2 ≈ 0,866: напротив 60° лежит катет √3, гипотенуза 2."), hint, columns = 1,
    )
    val q4 = quizStep(
        L("Doplň sin 90°", "Заполни sin 90°"),
        listOf(
            o(0, L("0 je sin 0°. Při 90° stojí úsečka svisle – svislý průmět je celá její délka.", "0 — это sin 0°. При 90° отрезок стоит вертикально — вертикальная проекция равна всей длине.")),
            o(1, L("1/2 je sin 30°. Při 90° stojí úsečka svisle.", "1/2 — это sin 30°. При 90° отрезок стоит вертикально.")),
            o(4),
        ),
        L(
            "sin 90° = 1. Vidíš vzor? Řádek sin je √0/2, √1/2, √2/2, √3/2, √4/2.",
            "sin 90° = 1. Видишь закономерность? Строка sin — это √0/2, √1/2, √2/2, √3/2, √4/2.",
        ), hint, columns = 3,
    )
    val q5 = quizStep(
        L("Jak jsou seřazené tytéž hodnoty v řádku cos?", "Как расположены те же значения в строке cos?"),
        listOf(
            Opt(L("Stejně jako u sin", "Так же, как у sin"), why = L("Pak by cos 0° = 0. Při 0° je ale vodorovný průmět celá úsečka.", "Тогда cos 0° = 0. Но при 0° горизонтальная проекция — весь отрезок.")),
            Opt(L("Obráceně", "В обратном порядке"), correct = true),
            Opt(L("Všude 1", "Везде 1"), why = L("cos 90° = 0: svislá úsečka nemá vodorovný průmět.", "cos 90° = 0: у вертикального отрезка нет горизонтальной проекции.")),
        ),
        L(
            "Kosinus má stejné hodnoty v opačném pořadí, protože cos α = sin (90° − α): přilehlá odvěsna k α je protilehlá k druhému ostrému úhlu.",
            "У косинуса те же значения в обратном порядке, поскольку cos α = sin (90° − α): катет, прилежащий к α, противолежит другому острому углу.",
        ),
        columns = 3,
    )
    val qs = listOf(q0, q1, q2, q3, q4)
    val filled = qs.indexOfFirst { !it.done }.let { if (it < 0) 5 else it }
    LaunchedEffect(q5.done) { if (q5.done) anim.animateTo(1f, tween(900)) }
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
            rect(x0 + cw * i, yCos, cw, rowH, c.paper, if (filled == 5 && !q5.done) c.highlight else c.line, if (filled == 5 && !q5.done) 2.0 else 1.0)
            if (i < filled) {
                text(x0 + cw * i + cw / 2, ySin + 28, TABLE_EXACT[i], c.derived, 15.0, bold = true)
                if (i in 2..3) text(x0 + cw * i + cw / 2, ySin + 48, TABLE_DEC[i], c.inkSoft, 11.0)
            } else if (i == filled) {
                text(x0 + cw * i + cw / 2, ySin + rowH / 2 + 6, "?", c.highlight, 18.0, bold = true)
            }
            val j = 4 - i
            if (anim.value * 5 > i) {
                text(x0 + cw * i + cw / 2, yCos + 28, TABLE_EXACT[j], c.derived, 15.0, bold = true)
                if (j in 2..3) text(x0 + cw * i + cw / 2, yCos + 48, TABLE_DEC[j], c.inkSoft, 11.0)
            }
        }
    }
    CardText(L(
        "Hodnoty pro 0°, 30°, 45°, 60° a 90° tvoří užitečnou tabulku. Půlka čtverce a půlka rovnostranného trojúhelníku ti ji pomůžou znovu sestavit.",
        "Значения для 0°, 30°, 45°, 60° и 90° образуют полезную таблицу. Половина квадрата и половина равностороннего треугольника помогут её восстановить.",
    ))
    Steps(q0, q1, q2, q3, q4, q5)
    if (q5.done) SmallText(L(
        "Pro zajímavost: tg = sin / cos, takže tg 30° = 1/√3 ≈ 0,577, tg 45° = 1, tg 60° = √3 ≈ 1,732 (tg 90° není definován).",
        "Для справки: tg = sin / cos, поэтому tg 30° = 1/√3 ≈ 0,577, tg 45° = 1, tg 60° = √3 ≈ 1,732 (tg 90° не определён).",
    ))
}

/** Karta 3: kvíz – cos 60° zpaměti. */
@Composable
private fun CardScope.CheckCos60() {
    CardText(L("Známé hodnoty umožňují rychlou kontrolu výpočtu.", "Знакомые значения помогают быстро проверить вычисление."))
    Steps(
        quizStep(
            question = L("Kolik je cos 60°? Zpaměti, bez tabulky.", "Чему равен cos 60°? Наизусть, без таблицы."),
            options = listOf(
                Opt(L("0,5", "0,5"), correct = true),
                Opt(L("0,866", "0,866"), why = L("0,866 = √3/2 je sin 60°. Řádek cos má tytéž hodnoty pozpátku – co je v něm pod 60°?", "0,866 = √3/2 — это sin 60°. В строке cos те же значения в обратном порядке — что в ней под 60°?")),
                Opt(L("0,707", "0,707"), why = L("0,707 patří k 45°, kde jsou sin a cos stejné.", "0,707 относится к 45°, где sin и cos равны.")),
                Opt(L("1", "1"), why = L("Mezi 0° a 90° má kosinus hodnotu 1 jen při 0°. U 60° je přilehlá odvěsna mnohem kratší než přepona.", "Между 0° и 90° косинус равен 1 только при 0°. При 60° прилежащий катет намного короче гипотенузы.")),
            ),
            explain = L(
                "cos 60° = 1/2. V trojúhelníku 30°–60°–90° je odvěsna u úhlu 60° poloviční než přepona.",
                "cos 60° = 1/2. В треугольнике 30°–60°–90° прилежащий к 60° катет вдвое короче гипотенузы.",
            ),
            columns = 2,
        ),
    )
}

/** Karta 4: velikosti složek síly bez kalkulačky (20 kN, 30° od vodorovné). */
@Composable
private fun CardScope.NoCalc() {
    val t = tr()
    val fy = numStep(
        question = L("Kolik je |[[F_y]]|, velikost svislé složky? Počítej zpaměti.", "Чему равен |[[F_y]]|, модуль вертикальной составляющей? Считай устно."),
        fields = listOf(NumSpec(L("|[[F_y]]|", "|[[F_y]]|"), 10.0, "kN")),
        explain = L(
            "Svislá složka leží naproti úhlu 30°: |[[F_y]]| = 20 · sin 30° = 20 · 1/2 = 10 kN.",
            "Вертикальная составляющая лежит напротив угла 30°: |[[F_y]]| = 20 · sin 30° = 20 · 1/2 = 10 кН.",
        ),
        traps = listOf(
            NumTrap.near(L("≈ 17,32 je 20 · cos 30° – složka **u** úhlu 30°, tedy vodorovná. Svislá leží naproti.", "≈ 17,32 — это 20 · cos 30°, составляющая **у** угла 30°, то есть горизонтальная. Вертикальная лежит напротив."), 17.32),
            NumTrap.near(L("To je celá síla. Šikmá síla má obě složky menší než sama je.", "Это вся сила. У наклонной силы обе составляющие меньше её самой."), 20.0),
        ),
        nudge = L("Která funkce patří k odvěsně naproti úhlu 30°? A kolik je sin 30°?", "Какая функция относится к катету напротив угла 30°? И чему равен sin 30°?"),
    )
    val fx = quizStep(
        question = L("Jaká je přesná velikost vodorovné složky |[[F_x]]|?", "Каков точный модуль горизонтальной составляющей |[[F_x]]|?"),
        options = listOf(
            Opt(L("10√3 kN", "10√3 кН"), correct = true),
            Opt(L("10 kN", "10 кН"), why = L("10 kN je svislá složka, naproti 30°. Vodorovná leží u úhlu – patří k ní kosinus.", "10 кН — вертикальная составляющая, напротив 30°. Горизонтальная лежит у угла — к ней относится косинус.")),
            Opt(L("20√3 kN", "20√3 кН"), why = L("20√3 ≈ 34,6 kN je víc než celá síla 20 kN. Nezapomeň na /2 v cos 30° = √3/2.", "20√3 ≈ 34,6 кН больше всей силы 20 кН. Не забудь /2 в cos 30° = √3/2.")),
        ),
        explain = L(
            "|[[F_x]]| = 20 · cos 30° = 20 · √3/2 = 10√3 kN ≈ 17,32 kN. Obě složky šikmé síly jsou menší než ona sama; směr složek ukazují šipky.",
            "|[[F_x]]| = 20 · cos 30° = 20 · √3/2 = 10√3 кН ≈ 17,32 кН. Обе составляющие наклонной силы меньше её самой; направления показаны стрелками.",
        ),
        columns = 3,
    )
    val tail = Pt(90.0, 50.0)
    val len = 220.0
    val tip = Pt(tail.x + len * Trig.cosD(30.0), tail.y + len * Trig.sinD(30.0))
    val corner = Pt(tip.x, tail.y)
    Stage(h = 220.0, description = t.pick(L("Síla 20 kN skloněná o 30° pod vodorovnou a její dvě složky", "Сила 20 кН под углом 30° ниже горизонтали и её две составляющие"))) { c ->
        line(tail.x - 30, tail.y, corner.x + 40, corner.y, c.inkSoft, 1.0, dash = true)
        // složky: čárkovaně, dokud nejsou spočítané
        if (fx.done) force(tail.x, tail.y, corner.x, corner.y, c.derived, 2.4)
        else line(tail.x, tail.y, corner.x, corner.y, c.derived, 1.4, dash = true)
        if (fy.done) force(corner.x, corner.y, tip.x, tip.y, c.derived, 2.4)
        else line(corner.x, corner.y, tip.x, tip.y, c.derived, 1.4, dash = true)
        rightAngleMark(corner, tail, tip, c)
        force(tail.x, tail.y, tip.x, tip.y, c.load, 3.0)
        text((tail.x + tip.x) / 2 - 6, (tail.y + tip.y) / 2 + 22, " = 20 kN", c.load, 14.0, Anchor.END, sym = "F")
        angleArc(tail.x, tail.y, 46.0, 0.0, screenAngle(tail, tip), "30°", c)
        text((tail.x + corner.x) / 2, tail.y - 12, if (fx.done) " = 10√3 ≈ 17,32 kN" else " = ?", c.derived, 13.0, sym = "F_x")
        text(corner.x + 10, (corner.y + tip.y) / 2 + 5, if (fy.done) " = 10 kN" else " = ?", c.derived, 13.0, Anchor.START, sym = "F_y")
    }
    CardText(L(
        "Síla 20 kN svírá s vodorovnou úhel 30°. Nahradíme ji vodorovnou složkou [[F_x]] a svislou [[F_y]]. Zatím určujeme jen jejich velikosti.",
        "Сила 20 кН составляет с горизонталью угол 30°. Заменим её горизонтальной составляющей [[F_x]] и вертикальной [[F_y]]. Пока определяем только их модули.",
    ))
    Steps(fy, fx)
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
