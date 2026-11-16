package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.exercise
import kotlin.math.abs

/** E04 – přepona z odvěsny a úhlu (dělí se funkcí). */
val E04 = ExerciseType("E04", L("Přepona z odvěsny", "Гипотенуза по катету"), Cards.LADDER, levels = 2..3) { rng, level, seed ->
    val a = if (level == 2) rng.int(2, 15).toDouble() else rng.int(4, 30) / 2.0
    val al = (rng.int(4, 14) * 5).toDouble()
    val opposite = rng.chance(0.5)
    val fn = if (opposite) Fn.SIN else Fn.COS
    val fv = fn.deg(al)
    val ans = a / fv
    val adj = if (opposite) a / Fn.TG.deg(al) else a
    val opp = if (opposite) a else a * Fn.TG.deg(al)
    val side = if (opposite) TriSide.OPP else TriSide.ADJ
    val sideCs = if (opposite) "protilehlá (naproti úhlu)" else "přilehlá (u úhlu)"
    val sideRu = if (opposite) "противолежащий (напротив угла)" else "прилежащий (у угла)"
    val radV = fn.rad(al)
    exercise(
        "E04", seed, level,
        prompt = L(
            "Odvěsna $sideCs měří ${n(a)} m, úhel α = ${deg(al)}. Jak dlouhá je přepona?",
            "Катет $sideRu равен ${n(a)} м, угол α = ${deg(al)}. Какова длина гипотенузы?",
        ),
        fig = Fig.Tri(adj, opp, rot = rng.int(0, 3), mirror = rng.chance(0.5), labels = mapOf(side to "${n(a)} m", TriSide.HYP to "?"), highlight = TriSide.HYP, angle = deg(al)),
        fields = listOf(Field(L("přepona", "гипотенуза"), ans, "m")),
        hints = listOf(
            L("${fn.label} α = odvěsna / přepona, takže **přepona = odvěsna / ${fn.label} α**.", "${fn.label} α = катет / гипотенуза, значит **гипотенуза = катет / ${fn.label} α**."),
            L("${n(a)} / ${fn.label} ${deg(al)} = ${n(a)} / ${n(fv, 3)}", "${n(a)} / ${fn.label} ${deg(al)} = ${n(a)} / ${n(fv, 3)}"),
        ),
        traps = buildList {
            add(Traps.MUL.at(a * fv))
            add(Traps.SWAP.with(L("Dělíš jinou funkcí: známá odvěsna leží ${if (opposite) "naproti úhlu → sin" else "u úhlu → cos"}.", "Ты делишь не на ту функцию: известный катет лежит ${if (opposite) "напротив угла → sin" else "у угла → cos"}.")).at(a / (if (opposite) Fn.COS else Fn.SIN).deg(al)))
            if (abs(radV) > 0.05) add(Traps.RAD.at(a / radV))
        },
        ok = L("Správně: ${n(a)} / ${fn.label} ${deg(al)} ${approx(ans)} m.", "Верно: ${n(a)} / ${fn.label} ${deg(al)} ${approx(ans)} м."),
    )
}
