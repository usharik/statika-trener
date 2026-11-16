package cz.usharik.statika.trainer

import cz.usharik.statika.model.EqSpec
import cz.usharik.statika.model.NamedPoint
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.allItems
import cz.usharik.statika.model.linearize
import cz.usharik.statika.model.namedPoints

data class DoneSummary(val point: NamedPoint, val residual: Double, val stars: Int)

/** Kontrola momentovou podmínkou k jinému bodu, než se obvykle volí, a hodnocení podle nápověd. */
fun doneSummary(p: Problem, results: Map<String, Double>, hints: Int): DoneSummary {
    val pts = namedPoints(p)
    val pt = pts.last()
    val lin = linearize(EqSpec.M(pt.label, pt.x), allItems(p))
    val residual = lin.a.entries.fold(lin.c) { s, (k, v) -> s + v * results.getValue(k) }
    val stars = if (hints == 0) 3 else if (hints <= 3) 2 else 1
    return DoneSummary(pt, residual, stars)
}
