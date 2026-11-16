package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.TrapText
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.exercise
import kotlin.math.sqrt

/** E10 – druhá funkce z první: sin² α + cos² α = 1 (ostrý úhel). */
val E10 = ExerciseType("E10", L("sin² + cos² = 1", "sin² + cos² = 1"), Cards.UNIT_HYP, ) { rng, level, seed ->
    // lehká úroveň: 0,6 / 0,8 – odmocnina zpaměti (√0,64 = 0,8)
    val s = rng.pick(
        when (level) {
            1 -> listOf(0.6, 0.8)
            2 -> listOf(0.6, 0.8, 0.28, 0.96)
            else -> listOf(0.6, 0.8, 0.28, 0.96, 0.5, 0.385, 0.923)
        },
    )
    val fromSin = rng.chance(0.5)
    val given = if (fromSin) "sin" else "cos"
    val wanted = if (fromSin) "cos" else "sin"
    val ans = sqrt(1 - s * s)
    exercise(
        "E10", seed, level,
        prompt = L("α je ostrý úhel a $given α = ${n(s, 3)}. Kolik je $wanted α?", "α — острый угол, $given α = ${n(s, 3)}. Чему равен $wanted α?"),
        fig = Fig.Tri(
            if (fromSin) ans else s, if (fromSin) s else ans,
            labels = mapOf(TriSide.HYP to "1", (if (fromSin) TriSide.OPP else TriSide.ADJ) to n(s, 3), (if (fromSin) TriSide.ADJ else TriSide.OPP) to "?"),
            angle = "α",
        ),
        fields = listOf(Field(L("$wanted α", "$wanted α"), ans, tol = Tol.RATIO, digits = 3)),
        hints = listOf(
            L("sin² α + cos² α = 1, takže $wanted² α = 1 − $given² α.", "sin² α + cos² α = 1, значит $wanted² α = 1 − $given² α."),
            L("$wanted α = √(1 − ${n(s, 3)}²) = √${n(1 - s * s, 4)}", "$wanted α = √(1 − ${n(s, 3)}²) = √${n(1 - s * s, 4)}"),
        ),
        traps = listOf(
            TrapText("LINEAR", L("1 − $given by platilo, kdyby se sčítaly sin a cos. Sčítají se ale jejich **druhé mocniny**.", "1 − $given было бы верно, если бы складывались sin и cos. Но складываются их **квадраты**."), Cards.UNIT_HYP).at(1 - s),
            Traps.NOSQRT.with(L("To je teprve $wanted² α. Ještě odmocni.", "Это пока $wanted² α. Осталось извлечь корень.")).at(1 - s * s),
        ),
        ok = L("Správně: $wanted α = √${n(1 - s * s, 4)} ${approx(ans, 3)}.", "Верно: $wanted α = √${n(1 - s * s, 4)} ${approx(ans, 3)}."),
    )
}
