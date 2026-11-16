package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.model.fmt
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.exercise

/** E03 – odvěsna z přepony a úhlu (žebřík, lano, rampa, trojúhelník). */
val E03 = ExerciseType(
    "E03", L("Odvěsna z přepony", "Катет по гипотенузе"), 102 to 4,
) { rng, level, seed ->
    // kontext určuje rozumné rozměry: žebřík 3–8 m pod 50–80°, rampa nejvýš 20° (na lehké úrovni místo ní krokev 30°/45°)
    val ctx = rng.int(0, 3)
    val c = when {
        ctx == 0 -> when (level) {
            1 -> rng.pick(listOf(4.0, 6.0, 8.0))
            2 -> rng.int(3, 8).toDouble()
            else -> rng.int(6, 16) / 2.0
        }
        level == 1 -> rng.pick(listOf(4.0, 6.0, 8.0, 10.0, 12.0, 20.0))
        level == 2 -> rng.int(2, 20).toDouble()
        else -> rng.int(4, 40) / 2.0
    }
    val a = when {
        ctx == 0 -> when (level) {
            1 -> 60
            2 -> rng.int(10, 16) * 5
            else -> rng.int(50, 80)
        }
        ctx == 2 && level == 1 -> rng.pick(listOf(30, 45))
        ctx == 2 -> if (level == 2) rng.int(1, 4) * 5 else rng.int(4, 20)
        level == 1 -> rng.pick(listOf(30, 45, 60))
        level == 2 -> rng.int(3, 15) * 5
        else -> rng.int(10, 80)
    }.toDouble()
    val opposite = rng.chance(0.5)
    val s = Trig.sinD(a)
    val co = Trig.cosD(a)
    val ans = c * if (opposite) s else co
    val other = c * if (opposite) co else s
    val cs = fmt(c)
    val al = fmt(a, 0)
    // kontext: (cs otázka, ru otázka, popisek pole cs, ru)
    val q = when (ctx) {
        0 -> listOf(
            "Žebřík dlouhý $cs m svírá se zemí $al°. " + if (opposite) "Jak vysoko na zdi končí?" else "Jak daleko od zdi stojí jeho pata?",
            "Лестница длиной $cs м составляет с землёй $al°. " + if (opposite) "На какой высоте она упирается в стену?" else "На каком расстоянии от стены её нижний конец?",
            if (opposite) "výška" else "vzdálenost od zdi", if (opposite) "высота" else "расстояние до стены",
        )
        1 -> listOf(
            "Lano dlouhé $cs m je napnuté pod úhlem $al° k vodorovné. " + if (opposite) "O kolik je horní konec výš než dolní?" else "Jak daleko jsou konce od sebe vodorovně?",
            "Трос длиной $cs м натянут под углом $al° к горизонтали. " + if (opposite) "На сколько верхний конец выше нижнего?" else "Каково расстояние между концами по горизонтали?",
            if (opposite) "převýšení" else "vodorovná vzdálenost", if (opposite) "превышение" else "расстояние по горизонтали",
        )
        2 -> if (level == 1) listOf(
            "Krokev dlouhá $cs m má sklon $al°. " + if (opposite) "O kolik je horní konec výš než dolní?" else "Jaká je její půdorysná délka?",
            "Стропило длиной $cs м наклонено под углом $al°. " + if (opposite) "На сколько верхний конец выше нижнего?" else "Какова его горизонтальная проекция?",
            if (opposite) "převýšení" else "půdorysná délka", if (opposite) "превышение" else "проекция",
        ) else listOf(
            "Rampa měří po šikmé ploše $cs m a stoupá pod úhlem $al°. " + if (opposite) "Do jaké výšky vede?" else "Jaká je její vodorovná délka?",
            "Пандус длиной $cs м по склону поднимается под углом $al°. " + if (opposite) "На какую высоту он ведёт?" else "Какова его горизонтальная длина?",
            if (opposite) "výška" else "vodorovná délka", if (opposite) "высота" else "горизонтальная длина",
        )
        else -> listOf(
            "Přepona měří $cs m, úhel α = $al°. Jak dlouhá je odvěsna " + (if (opposite) "**protilehlá**" else "**přilehlá**") + " k úhlu α?",
            "Гипотенуза равна $cs м, угол α = $al°. Какова длина катета, " + (if (opposite) "**противолежащего**" else "**прилежащего**") + " углу α?",
            "odvěsna", "катет",
        )
    }
    val giv = if (level == 1) given(a).cs else ""
    val target = if (opposite) TriSide.OPP else TriSide.ADJ
    val fn = if (opposite) "sin" else "cos"
    exercise(
        "E03", seed, level,
        prompt = L(q[0] + giv, q[1] + giv),
        fig = Fig.Tri(adj = c * co, opp = c * s, labels = mapOf(TriSide.HYP to "$cs m", target to "?"), highlight = target, angle = "$al°"),
        fields = listOf(Field(L(q[2], q[3]), ans, "m")),
        hints = listOf(
            if (opposite) L("Hledaná strana leží **naproti** úhlu → sin. Odvěsna = přepona · sin α.", "Искомая сторона лежит **напротив** угла → sin. Катет = гипотенуза · sin α.")
            else L("Hledaná strana leží **u** úhlu → cos. Odvěsna = přepona · cos α.", "Искомая сторона лежит **у** угла → cos. Катет = гипотенуза · cos α."),
            L("$cs · $fn $al° = $cs · ${fmt(if (opposite) s else co, 3)}", "$cs · $fn $al° = $cs · ${fmt(if (opposite) s else co, 3)}"),
        ),
        traps = listOf(
            Traps.SWAP.at(other),
            Traps.DIV.at(c / (if (opposite) s else co)),
            Traps.FULL.with(L("To je délka přepony. Odvěsna je vždy kratší – vynásob ji sin nebo cos.", "Это длина гипотенузы. Катет всегда короче — умножь её на sin или cos.")).at(c),
            Traps.RAD.at(c * if (opposite) Trig.sinRad(a) else Trig.cosRad(a)),
            Traps.GRAD.at(c * if (opposite) Trig.sinGrad(a) else Trig.cosGrad(a)),
        ),
        ok = L("Správně: $cs · $fn $al° ${approx(ans)} m.", "Верно: $cs · $fn $al° ${approx(ans)} м."),
    )
}
