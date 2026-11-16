package cz.usharik.statika

import cz.usharik.statika.model.BeamType
import cz.usharik.statika.model.GenOptions
import cz.usharik.statika.model.PointForce
import cz.usharik.statika.model.PointMoment
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.Udl
import cz.usharik.statika.model.generate
import cz.usharik.statika.model.jsRound
import cz.usharik.statika.model.solveProblem
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Stejné číslo úlohy musí dát stejnou úlohu jako webová verze.
 * golden.txt vytváří webový generátor (viz mobile/README.md – „Kontrola shody s webem“).
 */
class GoldenTest {
    private fun r6(v: Double) = jsRound(v * 1e6).toLong()

    private fun line(p: Problem, level: Int, types: String): String {
        val sup = p.supports.joinToString(",") { "${it.id}:${it.label}:${r6(it.x)}:${it.type.key}" }
        val hin = p.hinges.joinToString(",") { "${it.id}:${it.label}:${r6(it.x)}" }
        val loads = p.loads.joinToString(",") { l ->
            when (l) {
                is PointForce -> "force:${l.id}:${l.label}:${r6(l.x)}:${r6(l.f)}:${r6(l.angle)}:${l.alpha ?: ""}"
                is PointMoment -> "moment:${l.id}:${l.label}:${r6(l.x)}:${r6(l.m)}:${l.ccw}"
                is Udl -> "udl:${l.id}:${l.label}:${r6(l.x1)}:${r6(l.x2)}:${r6(l.q)}"
            }
        }
        val r = solveProblem(p)!!
        val res = r.keys.sorted().joinToString(",") { "$it=${jsRound(r.getValue(it) * 1000).toLong()}" }
        return listOf(p.seed, level, types, p.type.key, r6(p.length), sup, hin, loads, res).joinToString("|")
    }

    @Test
    fun shodaSWebem() {
        val golden = javaClass.getResource("/golden.txt")!!.readText().trimEnd().lines()
        var n = 0
        for (want in golden) {
            val parts = want.split("|")
            val seed = parts[0].toInt()
            val level = parts[1].toInt()
            val types = parts[2].split("+").map { BeamType.of(it)!! }
            val p = generate(seed, GenOptions(level, types))
            assertEquals(want, line(p, level, parts[2]), "úloha $seed, obtížnost $level")
            n++
        }
        assertEquals(7200, n)
    }
}
