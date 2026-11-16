package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.TrapText
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.exercise
import kotlin.math.sqrt

/** E09 – Pythagorova věta: přepona ze dvou odvěsen (A), nebo odvěsna z přepony a odvěsny (B). */
val E09 = ExerciseType("E09", L("Pythagorova věta", "Теорема Пифагора"), Cards.PYTHAGORAS) { rng, level, seed ->
    val hypMode = level == 1 || rng.chance(0.5)
    val a: Double
    val b: Double
    val c: Double
    when (level) {
        1, 2 -> {
            val (p, q, h) = rng.pick(if (level == 1) TRIPLES_EASY else TRIPLES_MORE)
            // lehká úroveň: odmocnina zpaměti (√25, √100, √169, √400) – ne √676 z 10-24-26
            val k = if (level == 1 && h != 13) rng.int(1, 2) else 1
            a = p * k.toDouble(); b = q * k.toDouble(); c = h * k.toDouble()
        }
        else -> if (hypMode) {
            a = rng.int(2, 12).toDouble(); b = rng.int(2, 12).toDouble(); c = sqrt(a * a + b * b)
        } else {
            c = rng.int(6, 15).toDouble(); a = rng.int(2, c.toInt() - 1).toDouble(); b = sqrt(c * c - a * a)
        }
    }
    val unit = "m"
    if (hypMode) exercise(
        "E09", seed, level,
        prompt = L("Odvěsny pravoúhlého trojúhelníku měří ${n(a)} m a ${n(b)} m. Jak dlouhá je přepona?", "Катеты прямоугольного треугольника равны ${n(a)} м и ${n(b)} м. Какова длина гипотенузы?"),
        fig = Fig.Tri(a, b, labels = mapOf(TriSide.ADJ to n(a), TriSide.OPP to n(b), TriSide.HYP to "?"), highlight = TriSide.HYP, angle = null),
        fields = listOf(Field(L("přepona c", "гипотенуза c"), c, unit)),
        hints = listOf(
            L("a² + b² = c². Hledáš přeponu: sečti druhé mocniny odvěsen a odmocni.", "a² + b² = c². Ищешь гипотенузу: сложи квадраты катетов и извлеки корень."),
            L("c = √(${n(a)}² + ${n(b)}²) = √${n(a * a + b * b)}" + sqCheck(c), "c = √(${n(a)}² + ${n(b)}²) = √${n(a * a + b * b)}" + sqCheck(c)),
        ),
        traps = listOf(Traps.SUM.at(a + b), Traps.NOSQRT.at(a * a + b * b)),
        ok = L("Správně: c = √${n(a * a + b * b)} ${approx(c)} m.", "Верно: c = √${n(a * a + b * b)} ${approx(c)} м."),
    ) else exercise(
        "E09", seed, level,
        prompt = L("Přepona měří ${n(c)} m a jedna odvěsna ${n(a)} m. Jak dlouhá je druhá odvěsna?", "Гипотенуза равна ${n(c)} м, один катет — ${n(a)} м. Какова длина второго катета?"),
        fig = Fig.Tri(a, b, labels = mapOf(TriSide.ADJ to n(a), TriSide.OPP to "?", TriSide.HYP to n(c)), highlight = TriSide.OPP, angle = null),
        fields = listOf(Field(L("odvěsna b", "катет b"), b, unit)),
        hints = listOf(
            L("a² + b² = c², takže b² = c² − a². Odvěsna je kratší než přepona – tady se **odečítá**.", "a² + b² = c², значит b² = c² − a². Катет короче гипотенузы — здесь **вычитают**."),
            L("b = √(${n(c)}² − ${n(a)}²) = √${n(c * c - a * a)}" + sqCheck(b), "b = √(${n(c)}² − ${n(a)}²) = √${n(c * c - a * a)}" + sqCheck(b)),
        ),
        traps = listOf(
            TrapText("ADDSQ", L("Odvěsna je kratší než přepona – druhé mocniny se tady odečítají, ne sčítají.", "Катет короче гипотенузы — квадраты здесь вычитают, а не складывают."), Cards.PYTHAGORAS).at(sqrt(c * c + a * a)),
            Traps.SUM.with(L("Strany se neodečítají přímo – odečítají se jejich druhé mocniny: b² = c² − a².", "Стороны не вычитают напрямую — вычитают их квадраты: b² = c² − a².")).at(c - a),
            Traps.NOSQRT.at(c * c - a * a),
        ),
        ok = L("Správně: b = √${n(c * c - a * a)} ${approx(b)} m.", "Верно: b = √${n(c * c - a * a)} ${approx(b)} м."),
    )
}

/** u celých výsledků zkouška násobením („ (13 · 13 = 169)“) – odmocnina se pak dá najít zpaměti */
private fun sqCheck(v: Double): String {
    val r = kotlin.math.round(v)
    return if (kotlin.math.abs(v - r) < 1e-9) " (${n(r, 0)} · ${n(r, 0)} = ${n(r * r, 0)})" else ""
}
