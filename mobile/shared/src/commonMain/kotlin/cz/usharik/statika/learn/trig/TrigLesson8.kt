package cz.usharik.statika.learn.trig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cz.usharik.statika.learn.ActionButton
import cz.usharik.statika.learn.Card
import cz.usharik.statika.learn.CardScope
import cz.usharik.statika.learn.CardText
import cz.usharik.statika.learn.Chips
import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.LearnJump
import cz.usharik.statika.learn.LearnNav
import cz.usharik.statika.learn.Lesson
import cz.usharik.statika.learn.LocalLearn
import cz.usharik.statika.learn.LocalLearnHere
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
import cz.usharik.statika.learn.axes
import cz.usharik.statika.learn.beam
import cz.usharik.statika.learn.bolt
import cz.usharik.statika.learn.circle
import cz.usharik.statika.learn.dimLine
import cz.usharik.statika.learn.force
import cz.usharik.statika.learn.line
import cz.usharik.statika.learn.moment
import cz.usharik.statika.learn.num
import cz.usharik.statika.learn.pick
import cz.usharik.statika.learn.rect
import cz.usharik.statika.learn.rightAngleMark
import cz.usharik.statika.learn.support
import cz.usharik.statika.learn.text
import cz.usharik.statika.learn.tr
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.ui.Anchor
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Kurz trigonometrie – lekce 8 (id 108): trigonometrie ve statice – úhel od svislice, výslednice, součet sil,
 * rameno momentu, šikmý nosník a závěrečná úloha.
 */

/** Šikmá síla F (10 kN) na nosník: hrot na nosníku v (tx, ty), úhel [alpha] k nosníku, síla míří dolů doprava. */
private fun forceTail(tx: Double, ty: Double, alpha: Double, len: Double): Pair<Double, Double> =
    (tx - len * Trig.cosD(alpha)) to (ty - len * Trig.sinD(alpha))

/** Karta 0: úhel od nosníku × od svislice – u úhlu cos, naproti sin. */
@Composable
private fun CardScope.FromVertical() {
    val t = tr()
    var vertical by remember { mutableStateOf(false) }
    var sawVertical by remember { mutableStateOf(false) }
    var back by remember { mutableStateOf(false) }
    Stage(h = 220.0, description = t.pick(L("Šikmá síla 10 kN na nosník s vyznačeným úhlem", "Наклонная сила 10 кН на балку с отмеченным углом"))) { c ->
        val tx = 250.0
        val ty = 170.0
        val len = 140.0
        beam(30.0, 370.0, ty + 5, c)
        val (sx, sy) = forceTail(tx, ty, 60.0, len)
        // složky čárkovaně
        line(sx, sy, tx, sy, c.derived, 1.0, dash = true)
        line(sx, sy, sx, ty, c.derived, 1.0, dash = true)
        force(sx, sy, tx, ty, c.load, 3.0)
        text(sx - 8, sy + 4, " = 10 kN", c.load, 14.0, Anchor.END, sym = "F")
        if (!vertical) {
            // úhel u nosníku (u hrotu síly)
            angleArc(tx, ty, 38.0, 180.0, 180.0 + 60.0, null, c, c.ink)
            text(sx - 8, ty - 14, "α = 60°", c.ink, 14.0, Anchor.END, bold = true)
        } else {
            line(tx, ty, tx, ty - 130, c.inkSoft, 1.0, dash = true)
            angleArc(tx, ty, 50.0, -90.0, 180.0 + 60.0, null, c, c.ink)
            text(tx + 8, ty - 64, "β = 30°", c.ink, 14.0, Anchor.START, bold = true)
        }
    }
    CardText(L(
        "Pravidlo „cos patří k [[F_x]]“ platí jen pro úhel měřený od vodorovné. Spolehlivé je jiné: **složka u úhlu → cos, složka naproti → sin**.",
        "Правило «cos — это для [[F_x]]» верно только для угла от горизонтали. Надёжно другое: **составляющая у угла → cos, напротив → sin**.",
    ))
    Todo(
        L("Klepni na „Úhel od svislice (β)“", "Нажми «Угол от вертикали (β)»") to sawVertical,
        L("Vrať se na „Úhel od nosníku (α)“ – změnily se složky?", "Вернись на «Угол от балки (α)» — изменились ли составляющие?") to back,
    )
    Chips(listOf(L("Úhel od nosníku (α)", "Угол от балки (α)"), L("Úhel od svislice (β)", "Угол от вертикали (β)")), if (vertical) 1 else 0) { i ->
        vertical = i == 1
        if (vertical) sawVertical = true else if (sawVertical) back = true
    }
    Readout(if (!vertical) "[[F_x]] = 10 · cos 60° = 5 kN" else "[[F_x]] = 10 · sin 30° = 5 kN")
    Quiz(
        question = L("Síla 10 kN svírá se **svislicí** 30°. Kolik je vodorovná složka [[F_x]]?", "Сила 10 кН составляет с **вертикалью** 30°. Чему равна горизонтальная составляющая [[F_x]]?"),
        options = listOf(
            Opt(L("5 kN", "5 кН"), correct = true),
            Opt(L("8,66 kN", "8,66 кН"), why = L("8,66 = 10 · cos 30° je složka **u úhlu**. U které osy je tady úhel nakreslený?", "8,66 = 10 · cos 30° — составляющая **у угла**. У какой оси здесь нарисован угол?")),
            Opt(L("10 kN", "10 кН"), why = L("Síla je šikmá – vodorovně míří jen její část.", "Сила наклонная — по горизонтали действует только её часть.")),
        ),
        explain = L(
            "Úhel leží u svislice, takže u úhlu je [[F_y]] = 10 · cos 30° = 8,66 kN. Vodorovná složka je naproti: [[F_x]] = 10 · sin 30° = 5 kN.",
            "Угол лежит у вертикали, поэтому у угла — [[F_y]] = 10 · cos 30° = 8,66 кН. Горизонтальная составляющая напротив: [[F_x]] = 10 · sin 30° = 5 кН.",
        ),
        columns = 3,
    )
}

/** Karta 1: ze složek celá síla – velikost Pythagorem, úhel arctg. */
@Composable
private fun CardScope.Resultant() {
    val t = tr()
    var fx by remember { mutableIntStateOf(2) }
    var fy by remember { mutableIntStateOf(5) }
    var at43 by remember { mutableStateOf(false) }
    var flipped by remember { mutableStateOf(false) }
    fun set(nx: Int, ny: Int) {
        fx = nx
        fy = ny
        if (fx == 4 && fy == 3) at43 = true
        if (at43 && fx == -4) flipped = true
    }
    val r = sqrt((fx * fx + fy * fy).toDouble())
    val alpha = when {
        fy == 0 -> 0.0
        fx == 0 -> 90.0
        else -> Trig.atanD(abs(fy).toDouble() / abs(fx))
    }
    val where = when {
        fx == 0 && fy == 0 -> L("", "")
        fx >= 0 && fy >= 0 -> L("míří doprava nahoru", "направлена вправо вверх")
        fx < 0 && fy >= 0 -> L("míří doleva nahoru", "направлена влево вверх")
        fx < 0 -> L("míří doleva dolů", "направлена влево вниз")
        else -> L("míří doprava dolů", "направлена вправо вниз")
    }
    Stage(h = 280.0, description = t.pick(L("Složky a jejich výslednice v osách x, y", "Составляющие и их равнодействующая в осях x, y"))) { c ->
        val cx = 200.0
        val cy = 140.0
        val u = 15.0 // 1 kN
        axes(cx, cy, 175.0, c)
        val tx = cx + fx * u
        val ty = cy - fy * u
        if (fx != 0) force(cx, cy, tx, cy, c.derived, 2.4)
        if (fy != 0) force(cx, cy, cx, ty, c.derived, 2.4)
        line(tx, cy, tx, ty, c.derived, 1.0, dash = true)
        line(cx, ty, tx, ty, c.derived, 1.0, dash = true)
        if (fx != 0) text((cx + tx) / 2, cy + (if (fy >= 0) 18.0 else -8.0), " = $fx", c.derived, 12.0, sym = "F_x")
        if (fy != 0) text(cx + (if (fx >= 0) -8.0 else 8.0), (cy + ty) / 2 + 5, " = $fy", c.derived, 12.0, if (fx >= 0) Anchor.END else Anchor.START, sym = "F_y")
        if (r > 0.01) {
            force(cx, cy, tx, ty, c.load, 3.0)
            text(tx + (if (fx >= 0) 8.0 else -8.0), ty + (if (fy >= 0) -6.0 else 16.0), " = ${num(r, 2)} kN", c.load, 13.0, if (fx >= 0) Anchor.START else Anchor.END, sym = "R")
            if (fx != 0 && fy != 0) {
                val base = if (fx > 0) 0.0 else 180.0
                angleArc(cx, cy, 30.0, base, cz.usharik.statika.learn.screenAngle(Pt(cx, cy), Pt(tx, ty)), "${num(alpha, 1)}°", c)
            }
        }
    }
    CardText(L(
        "Cesta zpátky: ze složek celá síla. Velikost dá Pythagorova věta, úhel arctg: **R = √([[F_x]]² + [[F_y]]²)**, **tg α = |[[F_y]]| / |[[F_x]]|**.",
        "Обратный путь: из составляющих — вся сила. Величину даёт теорема Пифагора, угол — arctg: **R = √([[F_x]]² + [[F_y]]²)**, **tg α = |[[F_y]]| / |[[F_x]]|**.",
    ))
    Todo(
        L("Nastav posuvníky [[F_x]] = 4 a [[F_y]] = 3", "Поставь ползунки [[F_x]] = 4 и [[F_y]] = 3") to at43,
        L("Pak změň [[F_x]] na −4 – co se stane s velikostí a co se směrem?", "Потом измени [[F_x]] на −4 — что будет с величиной и что с направлением?") to flipped,
    )
    ValueSlider("[[F_x]] = $fx kN", fx.toFloat(), -8f..8f, steps = 15) { set(it.roundToInt(), fy) }
    ValueSlider("[[F_y]] = $fy kN", fy.toFloat(), -8f..8f, steps = 15) { set(fx, it.roundToInt()) }
    if (r > 0.01) Readout("R = √(${fx}² + ${fy}²) ${if (kotlin.math.abs(r - kotlin.math.round(r)) < 1e-9) "=" else "≈"} ${num(r, 2)} kN · α ≈ ${num(alpha, 1)}°" + if (where.cs.isNotEmpty()) " · " + t.pick(where) else "")
    NumAnswer(
        question = L("[[F_x]] = 6 kN, [[F_y]] = 8 kN. Jak velká je výslednice R (celá síla složená z obou složek) a jaký ostrý úhel svírá s vodorovnou?", "[[F_x]] = 6 кН, [[F_y]] = 8 кН. Какова равнодействующая R (вся сила, сложенная из двух составляющих) и какой острый угол она составляет с горизонталью?"),
        fields = listOf(NumSpec(L("R", "R"), 10.0, "kN"), NumSpec(L("α", "α"), Trig.atanD(8.0 / 6.0), "°", tol = cz.usharik.statika.practice.Tol.ANGLE)),
        explain = L("R = √(36 + 64) = 10 kN; tg α = 8/6, α ≈ 53,1°.", "R = √(36 + 64) = 10 кН; tg α = 8/6, α ≈ 53,1°."),
        traps = listOf(
            NumTrap.near(L("Složky se nesčítají přímo – jsou na sebe kolmé. Která věta platí pro pravoúhlý trojúhelník?", "Составляющие нельзя просто сложить — они перпендикулярны. Какая теорема работает в прямоугольном треугольнике?"), 14.0, null),
            NumTrap.near(L("100 je teprve R². Ještě odmocni.", "100 — это пока R². Осталось извлечь корень."), 100.0, null),
            NumTrap.near(L("To je úhel od svislice – vyjde, když se dělí [[F_x]] / [[F_y]]. Naproti úhlu u vodorovné leží [[F_y]].", "Это угол от вертикали — так выходит, если делить [[F_x]] / [[F_y]]. Напротив угла у горизонтали лежит [[F_y]]."), null, Trig.atanD(6.0 / 8.0)),
        ),
    )
    SmallText(L(
        "Do arctg dávej velikosti bez znamének – vyjde ostrý úhel. Kam síla míří, poznáš ze znamének složek.",
        "В arctg подставляй величины без знаков — получится острый угол. Куда направлена сила, видно по знакам составляющих.",
    ))
}

/** Karta 2: součet dvou sil po složkách. */
@Composable
private fun CardScope.SumForces() {
    val t = tr()
    val shown = solvedHere
    Stage(h = 190.0, description = t.pick(L("Bod se dvěma silami: šikmá F1 a vodorovná F2", "Точка с двумя силами: наклонная F1 и горизонтальная F2"))) { c ->
        val cx = 200.0
        val cy = 70.0
        line(30.0, cy, 370.0, cy, c.inkSoft, 1.0, dash = true)
        val len = 120.0
        val tx = cx + len * Trig.cosD(30.0)
        val ty = cy + len * Trig.sinD(30.0)
        if (shown) {
            line(tx, cy, tx, ty, c.derived, 1.0, dash = true)
            force(cx, cy, tx, cy, c.derived, 2.2)
            text((cx + tx) / 2, cy - 10, " = 8,66 kN", c.derived, 12.0, sym = "F_1x")
            force(cx, cy - 26, cx + (8.66 - 3) * 12, cy - 26, c.accent, 2.6)
            text(cx + 4, cy - 36, "Σ = 5,66 kN", c.accent, 13.0, Anchor.START, sym = "F_x")
        }
        force(cx, cy, tx, ty, c.load, 3.0)
        text(tx + 8, ty + 4, " = 10 kN", c.load, 14.0, Anchor.START, sym = "F_1")
        angleArc(cx, cy, 40.0, 0.0, 30.0, "30°", c)
        force(cx, cy, cx - 36.0 * 1.4, cy, c.load, 3.0)
        text(cx - 56, cy + 22, " = 3 kN", c.load, 14.0, Anchor.END, sym = "F_2")
        circle(cx, cy, 6.0, c.ink)
    }
    CardText(L(
        "Síly různých směrů se nesčítají „šipka plus šipka“. Každou rozlož na složky a sečti zvlášť vodorovné a zvlášť svislé – se znaménky.",
        "Силы разных направлений не складывают «стрелка плюс стрелка». Каждую разложи на составляющие и сложи отдельно горизонтальные и отдельно вертикальные — со знаками.",
    ))
    Quiz(
        question = L("Kolik je Σ[[F_x]] – součet všech vodorovných složek (Σ = „součet“)? (cos 30° ≈ 0,866; sin 30° = 0,5)", "Чему равна Σ[[F_x]] — сумма всех горизонтальных составляющих (Σ — «сумма»)? (cos 30° ≈ 0,866; sin 30° = 0,5)"),
        options = listOf(
            Opt(L("5,66 kN", "5,66 кН"), correct = true),
            Opt(L("11,66 kN", "11,66 кН"), why = L("[[F_2]] míří doleva. Jaké znaménko má v součtu?", "[[F_2]] направлена влево. С каким знаком она входит в сумму?")),
            Opt(L("7 kN", "7 кН"), why = L("To je 10 − 3. Do vodorovného součtu nepatří celá šikmá síla, jen její vodorovná složka.", "Это 10 − 3. В горизонтальную сумму входит не вся наклонная сила, а только её горизонтальная составляющая.")),
            Opt(L("2 kN", "2 кН"), why = L("To je 5 − 3, ale 5 kN je svislá složka [[F_1]] (naproti úhlu 30°). Která složka je vodorovná?", "Это 5 − 3, но 5 кН — вертикальная составляющая [[F_1]] (напротив угла 30°). Какая составляющая горизонтальная?")),
        ),
        explain = L(
            "Nejdřív rozlož šikmou sílu: [[F_1x]] = 10 · cos 30° = 8,66 kN doprava. Pak sečti se znaménky: 8,66 − 3 = 5,66 kN.",
            "Сначала разложи наклонную силу: [[F_1x]] = 10 · cos 30° = 8,66 кН вправо. Потом сложи со знаками: 8,66 − 3 = 5,66 кН.",
        ),
        columns = 2,
    )
}

/** Karta 3: rameno momentu r⊥ = r · sin α. */
@Composable
private fun CardScope.MomentArm() {
    val t = tr()
    var alpha by remember { mutableIntStateOf(60) }
    var at90 by remember { mutableStateOf(false) }
    var at30 by remember { mutableStateOf(false) }
    var at0 by remember { mutableStateOf(false) }
    // „nastaveno“ se počítá, až když úhel chvíli zůstane (ne když posuvník jen přejede)
    LaunchedEffect(alpha) {
        delay(400)
        when (alpha) {
            90 -> at90 = true
            30 -> at30 = true
            0 -> at0 = true
        }
    }
    val r = 2.0
    val f = 10.0
    val rp = r * Trig.sinD(alpha.toDouble())
    val m = f * rp
    Stage(h = 250.0, description = t.pick(L("Páka 2 m, síla 10 kN na konci pod úhlem α a její rameno", "Рычаг 2 м, сила 10 кН на конце под углом α и её плечо"))) { c ->
        val ox = 70.0
        val oy = 140.0
        val unit = 115.0
        val ex = ox + r * unit
        // síla působí na konci páky, hrot v E; míří do směru (−cos α, −sin α) na obrazovce (α = 0 → do osy, 90° → nahoru)
        val ux = -Trig.cosD(alpha.toDouble())
        val uy = -Trig.sinD(alpha.toDouble())
        val len = 80.0
        val tailX = ex - ux * len
        val tailY = oy - uy * len
        // nositelka
        line(ex - ux * 260, oy - uy * 260, ex + ux * 260, oy + uy * 260, c.inkSoft, 1.0, dash = true)
        rect(ox, oy - 6, r * unit, 12.0, c.beam, c.ink, 1.4)
        bolt(ox, oy, 16.0, 0.0, c)
        text(ox - 4, oy + 34, "O", c.ink, 14.0, bold = true)
        force(tailX, tailY, ex, oy, c.load, 3.0)
        if (alpha < 20) text(tailX - 4, tailY - 14, " = 10 kN", c.load, 13.0, Anchor.END, sym = "F")
        else text(tailX - 10, tailY + 4, " = 10 kN", c.load, 13.0, Anchor.END, sym = "F")
        if (alpha in 1..89) angleArc(ex, oy, 30.0, 180.0, 180.0 + alpha.toDouble(), "α", c)
        if (alpha == 90) rightAngleMark(Pt(ex, oy), Pt(ox, oy), Pt(ex, oy - 30), c)
        dimLine(ox, oy + 50, ex, oy + 50, "r = 2 m", c)
        if (rp > 0.02) {
            // pata kolmice z O na nositelku
            val tt = (ox - ex) * ux + (oy - oy) * uy
            val px = ex + ux * tt
            val py = oy + uy * tt
            line(ox, oy, px, py, c.accent, 2.4, dash = true)
            if (alpha < 90) rightAngleMark(Pt(px, py), Pt(ox, oy), Pt(ex, oy), c, 8.0)
            text((ox + px) / 2 - 10, (oy + py) / 2, " = ${num(rp, 2)} m", c.accent, 13.0, Anchor.END, sym = "r_⊥")
        } else {
            text(ex - 10, oy - 22, t.pick(L("rameno 0", "плечо 0")), c.accent, 13.0, Anchor.END)
        }
    }
    CardText(L(
        "**Moment** = jak silně síla otáčí = síla × rameno. **Rameno** [[r_⊥]] je kolmá vzdálenost od osy k přímce síly (⊥ = kolmo) – v trojúhelníku s pákou jako přeponou je to odvěsna naproti α: **[[r_⊥]] = r · sin α**.",
        "**Момент** — насколько сильно сила вращает: сила × плечо. **Плечо** [[r_⊥]] — расстояние по перпендикуляру от оси до линии силы (⊥ — перпендикулярно); в треугольнике с рычагом-гипотенузой это катет напротив α: **[[r_⊥]] = r · sin α**.",
    ))
    Todo(
        L("Nastav posuvníkem „Úhel α“ 90° – rameno je celá délka páky", "Поставь ползунком «Угол α» 90° — плечо равно всей длине рычага") to at90,
        L("Nastav α = 30° – rameno je polovina", "Поставь α = 30° — плечо вдвое короче") to at30,
        L("Nastav α = 0° – nositelka síly prochází bodem otáčení, rameno je 0", "Поставь α = 0° — линия действия силы проходит через точку вращения, плечо равно 0") to at0,
    )
    ValueSlider(t.pick(L("Úhel α mezi silou a pákou = $alpha°", "Угол α между силой и рычагом = $alpha°")), alpha.toFloat(), 0f..90f, steps = 17) {
        alpha = (it / 5).roundToInt() * 5
    }
    val eqR = if (alpha % 90 == 0 || alpha == 30) "=" else "≈"
    Readout("[[r_⊥]] = 2 · sin $alpha° $eqR ${num(rp, 2)} m · M = 10 · ${num(rp, 2)} $eqR ${num(m, 2)} kNm")
    SmallText(L(
        "Stejný moment vyjde i jinak: otáčí jen složka kolmá k páce, M = (F · sin α) · r.",
        "Тот же момент получается и иначе: вращает только составляющая, перпендикулярная рычагу, M = (F · sin α) · r.",
    ))
    LearnJump(4, 1, L("Základy statiky: Moment síly", "Основы статики: момент силы"))
}

/** Karta 4: šikmý nosník – půdorys L · cos α, výška L · sin α. */
@Composable
private fun CardScope.InclinedBeam() {
    val t = tr()
    var alpha by remember { mutableIntStateOf(20) }
    var at30 by remember { mutableStateOf(false) }
    var at45 by remember { mutableStateOf(false) }
    val lm = 5.0
    val plan = lm * Trig.cosD(alpha.toDouble())
    val rise = lm * Trig.sinD(alpha.toDouble())
    Stage(h = 250.0, description = t.pick(L("Šikmý nosník délky 5 m s půdorysem a výškou", "Наклонная балка длиной 5 м с проекцией и высотой"))) { c ->
        val x0 = 70.0
        val y0 = 196.0
        val unit = 40.0
        val ex = x0 + plan * unit
        val ey = y0 - rise * unit
        line(20.0, y0, 380.0, y0, c.ink, 1.2)
        // krokev
        line(x0, y0, ex, ey, c.ink, 9.0)
        line(x0, y0, ex, ey, c.beam, 6.0)
        text((x0 + ex) / 2 - 12, (y0 + ey) / 2 - 12, "L = 5 m", c.ink, 13.0, Anchor.END)
        if (alpha > 0) {
            angleArc(x0, y0, 34.0, 0.0, -alpha.toDouble(), "$alpha°", c)
            line(ex, ey, ex, y0, c.derived, 1.0, dash = true)
            dimLine(ex + 18, y0, ex + 18, ey, "", c, color = c.derived)
            text(ex + 26, (y0 + ey) / 2 + 5, t.pick(L("výška ${num(rise, 2)} m", "высота ${num(rise, 2)} м")), c.derived, 13.0, Anchor.START)
        }
        dimLine(x0, y0 + 20, ex, y0 + 20, t.pick(L("půdorys ${num(plan, 2)} m", "проекция ${num(plan, 2)} м")), c, color = c.derived)
    }
    CardText(L(
        "Šikmý nosník (krokev, schodiště) je přepona. Jeho **půdorysná délka** (vodorovný průmět) je L · cos α a **převýšení** L · sin α.",
        "Наклонная балка (стропило, лестничный марш) — это гипотенуза. Её **горизонтальная проекция** (půdorys) равна L · cos α, а **высота** — L · sin α.",
    ))
    Todo(
        L("Nastav posuvníkem „Sklon α“ 30° – jak vysoko je horní konec?", "Поставь ползунком «Угол наклона α» 30° — на какой высоте верхний конец?") to at30,
        L("Nastav sklon 45°", "Поставь угол наклона 45°") to at45,
    )
    ValueSlider(t.pick(L("Sklon α = $alpha°", "Угол наклона α = $alpha°")), alpha.toFloat(), 0f..60f, steps = 11) {
        alpha = (it / 5).roundToInt() * 5
        if (alpha == 30) at30 = true
        if (alpha == 45) at45 = true
    }
    Readout(t.pick(L(
        "půdorysná délka = 5 · cos $alpha° ≈ ${num(plan, 2)} m · převýšení = 5 · sin $alpha° ≈ ${num(rise, 2)} m",
        "проекция = 5 · cos $alpha° ≈ ${num(plan, 2)} м · высота = 5 · sin $alpha° ≈ ${num(rise, 2)} м",
    )))
    Quiz(
        question = L("Krokev dlouhá 5 m má sklon 35° (sin 35° ≈ 0,57; cos 35° ≈ 0,82). Jaká je její půdorysná délka?", "Стропило длиной 5 м наклонено под углом 35° (sin 35° ≈ 0,57; cos 35° ≈ 0,82). Какова длина его горизонтальной проекции?"),
        options = listOf(
            Opt(L("4,10 m", "4,10 м"), correct = true),
            Opt(L("2,87 m", "2,87 м"), why = L("2,87 = 5 · sin 35° je výška – odvěsna naproti úhlu. Půdorys leží u úhlu.", "2,87 = 5 · sin 35° — это высота, катет напротив угла. Проекция лежит у угла.")),
            Opt(L("6,10 m", "6,10 м"), why = L("Půdorys nemůže být delší než krokev sama. Násobit, nebo dělit?", "Проекция не может быть длиннее самого стропила. Умножать или делить?")),
            Opt(L("5 m", "5 м"), why = L("5 m by to bylo jen u vodorovného nosníku.", "5 м было бы только у горизонтальной балки.")),
        ),
        explain = L("Půdorys leží u úhlu, proto kosinus: 5 · cos 35° ≈ 4,10 m.", "Проекция лежит у угла, поэтому косинус: 5 · cos 35° ≈ 4,10 м."),
        columns = 2,
    )
    SmallText(L(
        "Tíha působí svisle dolů, takže její rameno je vodorovná vzdálenost – měř ji v půdorysu, ne po šikmé krokvi.",
        "Вес действует вертикально вниз, поэтому его плечо — горизонтальное расстояние: измеряй его по проекции, а не вдоль наклонного стропила.",
    ))
}

/** Karta 5: závěrečná úloha – složky se znaménky a moment k bodu A. */
@Composable
private fun CardScope.FinalTask() {
    val t = tr()
    val shown = solvedHere
    val fxAns = -12 * Trig.cosD(60.0)
    val fyAns = -12 * Trig.sinD(60.0)
    val mAns = fyAns * 2
    Stage(h = 200.0, description = t.pick(L("Nosník s kloubovou podporou A a šikmou silou 12 kN ve vzdálenosti 2 m", "Балка с шарнирной опорой A и наклонной силой 12 кН на расстоянии 2 м"))) { c ->
        val ax = 70.0
        val by = 120.0
        val unit = 110.0
        val px = ax + 2 * unit
        beam(40.0, 370.0, by, c)
        support(SupportType.PIN, ax, by + 5, c, "A")
        // síla míří dolů doleva, hrot na horní hraně nosníku v bodě px
        val len = 100.0
        val ux = -Trig.cosD(60.0)
        val uy = Trig.sinD(60.0)
        val tx = px
        val ty = by - 5
        val sx = tx - ux * len
        val sy = ty - uy * len
        if (shown) {
            line(sx, sy, tx, sy, c.derived, 1.0, dash = true)
            line(tx, sy, tx, ty, c.derived, 1.0, dash = true)
            force(sx, sy - 10, tx, sy - 10, c.derived, 2.2)
            text((sx + tx) / 2, sy - 18, " = −6 kN", c.derived, 12.0, sym = "F_x")
            force(tx + 12, sy, tx + 12, ty, c.derived, 2.2)
            text(tx + 18, (sy + ty) / 2 + 5, " = −10,39 kN", c.derived, 12.0, Anchor.START, sym = "F_y")
            moment(ax, by, 34.0, false, c.accent)
            text(ax, by - 44, " = −20,78 kNm", c.accent, 12.0, sym = "M_A")
        }
        force(sx, sy, tx, ty, c.load, 3.0)
        text(sx - 10, sy + 6, " = 12 kN", c.load, 14.0, Anchor.END, sym = "F")
        angleArc(tx, ty, 34.0, 0.0, -60.0, "60°", c)
        dimLine(ax, by + 48, px, by + 48, "2 m", c)
    }
    NumAnswer(
        question = L(
            "Síla [[F]] = 12 kN působí 2 m od bodu A, míří šikmo dolů doleva a s nosníkem svírá 60° (sin 60° ≈ 0,866; cos 60° = 0,5). Urči složky i se znaménky a moment k bodu A (↺ kladný). Znaménko přepneš tlačítkem ± v poli.",
            "Сила [[F]] = 12 кН приложена в 2 м от точки A, направлена наклонно вниз влево и составляет с балкой 60° (sin 60° ≈ 0,866; cos 60° = 0,5). Найди составляющие со знаками и момент относительно точки A (↺ положительный). Знак переключается кнопкой ± в поле.",
        ),
        fields = listOf(
            NumSpec(L("[[F_x]]", "[[F_x]]"), fxAns, "kN", signed = true),
            NumSpec(L("[[F_y]]", "[[F_y]]"), fyAns, "kN", signed = true),
            NumSpec(L("[[M_A]]", "[[M_A]]"), mAns, "kNm", signed = true),
        ),
        explain = L(
            "[[F_x]] = −12 · cos 60° = −6 kN, [[F_y]] = −12 · sin 60° ≈ −10,39 kN. K bodu A otáčí jen [[F_y]]: [[M_A]] ≈ −10,39 · 2 ≈ −20,78 kNm (↻); [[F_x]] leží v ose nosníku a rameno nemá.",
            "[[F_x]] = −12 · cos 60° = −6 кН, [[F_y]] = −12 · sin 60° ≈ −10,39 кН. Вокруг A вращает только [[F_y]]: [[M_A]] ≈ −10,39 · 2 ≈ −20,78 кН·м (↻); [[F_x]] лежит на оси балки, плеча у неё нет.",
        ),
        traps = listOf(
            NumTrap(L("Vypadá to na prohozený sin a cos. Která složka leží **u** úhlu 60°?", "Похоже, sin и cos перепутаны. Какая составляющая лежит **у** угла 60°?")) { v ->
                close(v[0]?.let(::abs), 10.39) && close(v[1]?.let(::abs), 6.0)
            },
            NumTrap(L("Velikost [[F_x]] sedí. Kam míří šipka vodorovně – doprava (+), nebo doleva (−)?", "Величина [[F_x]] верна. Куда стрелка направлена по горизонтали — вправо (+) или влево (−)?")) { v -> close(v[0], 6.0) },
            NumTrap(L("Velikost [[F_y]] sedí. Míří šipka nahoru (+), nebo dolů (−)?", "Величина [[F_y]] верна. Стрелка направлена вверх (+) или вниз (−)?")) { v -> close(v[1], 10.39) },
            NumTrap(L("Složka nemůže být stejně velká jako celá síla 12 kN – je to jen její část. Rozlož sílu pomocí cos a sin.", "Составляющая не может равняться всей силе 12 кН — это только её часть. Разложи силу через cos и sin.")) { v -> close(v[0]?.let(::abs), 12.0) || close(v[1]?.let(::abs), 12.0) },
            NumTrap(L("Celá síla krát vzdálenost platí jen pro kolmou sílu. Otáčí jen [[F_y]] – nebo vezmi rameno 2 · sin 60°.", "Вся сила на расстояние — только для перпендикулярной силы. Вращает лишь [[F_y]] — или возьми плечо 2 · sin 60°.")) { v -> close(v[2]?.let(::abs), 24.0) },
            NumTrap(L("Velikost momentu sedí. Kterým směrem síla kolem A otáčí – ↺, nebo ↻?", "Величина момента верна. В какую сторону сила вращает вокруг A — ↺ или ↻?")) { v -> close(v[2], 20.78) },
        ),
    )
    if (shown) {
        val opener = LocalLearn.current
        val here = LocalLearnHere.current
        ActionButton(L("Procvičování →", "Практика →")) { opener.open(LearnNav.Practice(returnTo = here)) }
    }
}

private fun close(v: Double?, w: Double) = v != null && abs(v - w) <= maxOf(0.03, abs(w) * 0.01)

internal fun trigLesson8() = Lesson(
    id = 108,
    title = L("Trigonometrie ve statice", "Тригонометрия в статике"),
    summary = L("Složky, výslednice, její úhel a rameno momentu – všechno z jednoho trojúhelníku.", "Составляющие, равнодействующая, её угол и плечо момента — всё из одного треугольника."),
    cards = listOf(
        Card { FromVertical() },
        Card { Resultant() },
        Card { SumForces() },
        Card { MomentArm() },
        Card { InclinedBeam() },
        Card { FinalTask() },
    ),
)
