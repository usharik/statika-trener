package cz.usharik.statika

import cz.usharik.statika.model.BeamType
import cz.usharik.statika.model.EqSpec
import cz.usharik.statika.model.GenOptions
import cz.usharik.statika.model.PointForce
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.Support
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.model.Udl
import cz.usharik.statika.model.Hinge
import cz.usharik.statika.model.allItems
import cz.usharik.statika.model.defaultEquations
import cz.usharik.statika.model.generate
import cz.usharik.statika.model.linearize
import cz.usharik.statika.model.namedPoints
import cz.usharik.statika.model.solve
import cz.usharik.statika.model.solveProblem
import cz.usharik.statika.model.unknownsOf
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private fun assertClose(expected: Double, actual: Double?, msg: String = "") {
    assertNotNull(actual, msg)
    assertTrue(abs(expected - actual) < 1e-6, "$msg: expected $expected, got $actual")
}

class GeneratorTest {
    private val types = BeamType.entries

    @Test
    fun vytvariStatickyUrciteAResitelneUlohy() {
        val seen = HashSet<Problem>()
        for (level in 1..3) for (t in types) for (seed in 1..1500) {
            val p = generate(seed, GenOptions(level, listOf(t)))
            assertEquals(t, p.type)
            assertEquals(3 + p.hinges.size, unknownsOf(p).size)
            val r = solveProblem(p)
            assertNotNull(r)
            // řešení splňuje rovnováhu k libovolnému bodu
            val items = allItems(p)
            for (pt in namedPoints(p)) {
                val l = linearize(EqSpec.M(pt.label, pt.x), items)
                val sum = l.a.entries.fold(l.c) { s, (k, v) -> s + v * r.getValue(k) }
                assertTrue(abs(sum) < 1e-6)
            }
            seen += p.copy(seed = 0)
        }
        assertTrue(seen.size > 15000, "jen ${seen.size} různých úloh")
    }

    @Test
    fun prostyNosnik() {
        val p = generate(1, GenOptions(1, listOf(BeamType.SIMPLE)))
        assertNotNull(solve(defaultEquations(p), allItems(p), unknownsOf(p)))
    }

    @Test
    fun rucniKontrola() {
        val r = solveProblem(
            Problem(
                0, BeamType.SIMPLE, 6.0,
                listOf(Support("s0", "A", 0.0, SupportType.PIN), Support("s1", "B", 6.0, SupportType.ROLLER)),
                emptyList(),
                listOf(PointForce("f1", "F_1", 2.0, 10.0, 300.0, 60), Udl("u1", "q", 3.0, 6.0, 2.0)),
            ),
        )!!
        val fy = 10 * sin(PI / 3)
        assertClose(-5.0, r["s0x"])
        assertClose((fy * 2 + 6 * 4.5) / 6, r["s1y"])
        assertClose(fy + 6, r.getValue("s0y") + r.getValue("s1y"))
        // vetknutí 0 – kloub 3 – posuvná 5, síla 10 kN v x=4
        val g = solveProblem(
            Problem(
                0, BeamType.GERBER, 5.0,
                listOf(Support("s0", "A", 0.0, SupportType.FIXED), Support("s1", "B", 5.0, SupportType.ROLLER)),
                listOf(Hinge("h0", "K", 3.0)),
                listOf(PointForce("f1", "F_1", 4.0, 10.0, 270.0)),
            ),
        )!!
        assertClose(5.0, g["s1y"])
        assertClose(5.0, g["s0y"])
        assertClose(15.0, g["s0m"]) // ↺ kladně: M_A + 5·5 − 10·4 = 0
    }

    @Test
    fun vsechnyTypyZhrubaStejneCasto() {
        val count = mutableMapOf<BeamType, Int>()
        for (seed in 1..2000) {
            val p = generate(seed, GenOptions(1, types))
            count[p.type] = (count[p.type] ?: 0) + 1
        }
        for (t in types) assertTrue((count[t] ?: 0) > 400, "$t: ${count[t]}")
    }
}
