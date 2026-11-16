package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.exercise
import kotlin.math.abs

/** E23 – šikmý nosník (krokev): půdorys L · cos α a výška L · sin α. */
val E23 = ExerciseType("E23", L("Šikmý nosník", "Наклонная балка"), Cards.RAFTER) { rng, level, seed ->
    val len = if (level == 1) rng.int(2, 8).toDouble() else rng.int(6, 16) / 2.0
    val a = when (level) {
        1 -> rng.pick(listOf(30.0, 45.0, 60.0))
        2 -> rng.int(5, 9) * 5.0
        else -> rng.int(20, 60).toDouble()
    }
    val px = len * Fn.COS.deg(a)
    val h = len * Fn.SIN.deg(a)
    exercise(
        "E23", seed, level,
        prompt = L(
            "Krokev dlouhá ${n(len)} m má sklon ${deg(a)}. Jaká je její půdorysná délka a o kolik je horní konec výš?" + (if (level == 1) given(a).cs else ""),
            "Стропило длиной ${n(len)} м наклонено под углом ${deg(a)}. Какова его горизонтальная проекция и на сколько выше верхний конец?" + (if (level == 1) given(a).ru else ""),
        ),
        fig = Fig.Rafter(len, a),
        fields = listOf(Field(L("půdorys", "проекция"), px, "m"), Field(L("výška", "высота"), h, "m")),
        hints = listOf(
            L("Krokev je přepona: půdorys leží u úhlu (cos), výška naproti (sin).", "Стропило — гипотенуза: проекция лежит у угла (cos), высота напротив (sin)."),
            L("${n(len)} · cos ${deg(a)}; ${n(len)} · sin ${deg(a)}", "${n(len)} · cos ${deg(a)}; ${n(len)} · sin ${deg(a)}"),
        ),
        traps = buildList {
            add(Traps.SWAP.at(h, px))
            add(Traps.DIV.at(len / Fn.COS.deg(a), len / Fn.SIN.deg(a)))
            val rx = len * Fn.COS.rad(a)
            val ry = len * Fn.SIN.rad(a)
            if (abs(rx) > 0.05 && abs(ry) > 0.05) add(Traps.RAD.at(rx, ry))
        },
        ok = L("Správně: půdorys ${approx(px)} m, výška ${approx(h)} m.", "Верно: проекция ${approx(px)} м, высота ${approx(h)} м."),
    )
}
