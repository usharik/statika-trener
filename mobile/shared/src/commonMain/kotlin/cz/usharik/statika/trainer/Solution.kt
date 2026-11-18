package cz.usharik.statika.trainer

import cz.usharik.statika.model.EqSpec
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.Side
import cz.usharik.statika.model.namedPoints
import cz.usharik.statika.model.Unknown
import cz.usharik.statika.model.allItems
import cz.usharik.statika.model.defaultEquations
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.solve
import cz.usharik.statika.model.unknownsOf
import kotlin.math.abs

/**
 * Výraz pro neznámou [uid] z rovnice [terms], např. „(8,66·2 + 6·4,5) / 6“.
 * null, když rovnice obsahuje ještě nespočítané neznámé (mimo [solved]).
 */
fun expressionFor(terms: List<Term>, uid: String, solved: Map<String, Double>): String? {
    val self = terms.first { it.it.unknownId == uid }
    val f = if (self.c > 0) -1 else 1
    val parts = mutableListOf<String>()
    for (x in terms) {
        if (x === self) continue
        val c = x.c * f
        val text = if (x.it.unknownId != null) {
            val v = solved[x.it.unknownId] ?: return null
            if (x.arm != null) "${fmt(abs(x.c))}·${paren(v)}" else paren(v)
        } else {
            val v = x.it.value!!
            if (x.arm != null) "${fmt(v)}·${fmt(abs(x.c))}" else fmt(v)
        }
        parts += (if (c < 0) (if (parts.isNotEmpty()) " − " else "−") else if (parts.isNotEmpty()) " + " else "") + text
    }
    val d = abs(self.c)
    val num = if (parts.isNotEmpty()) parts.joinToString("") else "0"
    if (abs(d - 1) < 1e-9) return num
    return (if (parts.size > 1) "($num)" else num) + " / ${fmt(d)}"
}

/** Jeden řádek výpočtu: neznámá [u] z rovnice č. [eq] (výraz [expr]). */
data class SolutionLine(val u: Unknown, val eq: Int, val expr: String, val value: Double)

/** Vzorové řešení úlohy: rovnice, pořadí výpočtu reakcí a výsledky. */
class Solution(
    val eqs: List<UserEq>,
    val terms: List<List<Term>>,
    val lines: List<SolutionLine>,
    /** neznámé, které nejde spočítat jednu po druhé – řeší se soustavou */
    val coupled: List<Unknown>,
    val values: Map<String, Double>,
)

private fun priority(e: EqSpec) = when (e) {
    is EqSpec.Hinge -> 0
    is EqSpec.M -> 1
    EqSpec.Fx -> 2
    EqSpec.Fy -> 3
}

/**
 * Řešení tak, jak by ho psal žák: v každém kroku rovnice s jedinou dosud nespočítanou neznámou.
 * Přednost mají rovnice z [defaultEquations] (ty, které trenažér nabízí v kroku 3) a momentové podmínky;
 * když by zbyla soustava (Gerberův nosník), přidá se momentová podmínka k dalšímu bodu nebo kloub z druhé strany.
 */
fun fullSolution(p: Problem): Solution {
    val items = allItems(p)
    val unknowns = unknownsOf(p)
    val defaults = defaultEquations(p)
    val extras = namedPoints(p).map { EqSpec.M(it.label, it.x) }.filter { it !in defaults } +
        p.hinges.map { h ->
            val used = defaults.filterIsInstance<EqSpec.Hinge>().first { it.hingeId == h.id }.side
            EqSpec.Hinge(h.id, h.label, h.x, if (used == Side.LEFT) Side.RIGHT else Side.LEFT)
        }
    val candidates = defaults + extras
    val values = solve(defaults, items, unknowns)!!
    val candTerms = candidates.map { termsOf(UserEq(it, cw = false), items) }
    val solved = LinkedHashMap<String, Double>()
    val steps = mutableListOf<Triple<Int, String, String>>() // kandidát, neznámá, výraz
    val usedEqs = mutableSetOf<Int>()
    while (solved.size < unknowns.size) {
        val pick = candidates.indices
            .filter { it !in usedEqs }
            .mapNotNull { i ->
                val open = candTerms[i].mapNotNull { it.it.unknownId }.distinct().filter { it !in solved }
                if (open.size == 1) i to open[0] else null
            }
            .minByOrNull { (i, _) -> (if (i < defaults.size) 0 else 1000) + priority(candidates[i]) * 100 + i } ?: break
        val (i, uid) = pick
        steps += Triple(i, uid, expressionFor(candTerms[i], uid, solved)!!)
        solved[uid] = values.getValue(uid)
        usedEqs += i
    }
    // rovnice v pořadí trenažéru (výchozí, pak doplněné); nespočítané neznámé → všechny výchozí rovnice
    val shown = (if (solved.size < unknowns.size) usedEqs + defaults.indices else usedEqs).sorted()
    val lines = steps.map { (i, uid, expr) -> SolutionLine(unknowns.first { it.id == uid }, shown.indexOf(i), expr, values.getValue(uid)) }
    return Solution(
        shown.map { UserEq(candidates[it], cw = false) },
        shown.map { candTerms[it] },
        lines,
        unknowns.filter { it.id !in solved },
        values,
    )
}
