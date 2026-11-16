package cz.usharik.statika

import cz.usharik.statika.model.BeamType
import cz.usharik.statika.model.EqSpec
import cz.usharik.statika.model.Hinge
import cz.usharik.statika.model.IssueKind
import cz.usharik.statika.model.ParseError
import cz.usharik.statika.model.PointForce
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.Side
import cz.usharik.statika.model.Support
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.model.Udl
import cz.usharik.statika.model.allItems
import cz.usharik.statika.model.analyze
import cz.usharik.statika.model.analyzeBest
import cz.usharik.statika.model.parseEquation
import cz.usharik.statika.model.symbolsOf
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EqParseTest {
    // A pevná (0), B posuvná (6); F_1 = 10 kN pod 60° v x=2 (dolů doprava); q = 2 kN/m na <3;6>
    private val p = Problem(
        0, BeamType.SIMPLE, 6.0,
        listOf(Support("s0", "A", 0.0, SupportType.PIN), Support("s1", "B", 6.0, SupportType.ROLLER)),
        emptyList(),
        listOf(PointForce("f1", "F_1", 2.0, 10.0, 300.0, 60), Udl("u1", "q", 3.0, 6.0, 2.0)),
    )
    private val items = allItems(p)
    private val defs = symbolsOf(p, items)
    private val mA = EqSpec.M("A", 0.0)
    private fun run(src: String, spec: EqSpec) = analyze(parseEquation(src, defs), spec, items, defs)

    @Test
    fun prijmeRuzneSpravneTvary() {
        for (s in listOf(
            "RB*6 - F1y*2 - Q*4,5 = 0",
            "R_B·6 = F_1y·2 + Q·4.5",
            "-RB*6 + F1y*2 + Q*4.5",
            "RB*6 - F1*sin(60)*2 - q*3*4,5 = 0",
            "rb*6 - 8,66*2 - 6*4,5 = 0",
            "R_B*6 - 10*sin(60)*2 - 2*3*4,5 = 0",
            "6RB - 2F1y - 4,5Q = 0",
            "RB = (F1y*2 + Q*4,5)/6",
        )) assertTrue(run(s, mA).ok, s)
        assertTrue(run("RAx + F1x = 0", EqSpec.Fx).ok)
        assertTrue(run("RAy + RB - F1y - Q = 0", EqSpec.Fy).ok)
        assertTrue(run("RAy + RB = F_1*sin(60) + q*3", EqSpec.Fy).ok)
    }

    @Test
    fun pojmenujeKonkretniProblem() {
        val a = run("RB*6 + F1y*2 - Q*4,5 = 0", mA)
        assertEquals(listOf(IssueKind.SIGN to "F_1y"), a.issues.map { it.kind to it.item.sym })
        val b = run("RB*6 - F1y*2 - Q*3 = 0", mA)
        assertEquals(IssueKind.MAG, b.issues[0].kind)
        assertTrue(abs(b.issues[0].impliedArm!! - 3) < 1e-6)
        val c = run("RB*6 - F1y*2 - Q*4,5 + RAy*0 + F1x*1 = 0", mA)
        assertEquals(listOf(IssueKind.EXTRA), c.issues.map { it.kind })
        val d = run("RB*6 - Q*4,5", mA)
        assertEquals(listOf(IssueKind.MISSING to "F_1y"), d.issues.map { it.kind to it.item.sym })
        assertEquals(listOf(-10.5), run("RB*6 - 7*1,5 - Q*4,5", mA).stray)
    }

    @Test
    fun hlasiChybyZapisu() {
        assertIs<ParseError.UnknownSym>(parseEquation("RB*6 - X1", defs).error)
        assertIs<ParseError.Nonlinear>(parseEquation("RB*RAy", defs).error)
        assertIs<ParseError.Syntax>(parseEquation("RB*6 -", defs).error)
        assertIs<ParseError.TwoEq>(parseEquation("RB = 1 = 2", defs).error)
    }

    @Test
    fun kloubVyberLepsiCast() {
        val g = Problem(
            0, BeamType.GERBER, 5.0,
            listOf(Support("s0", "A", 0.0, SupportType.FIXED), Support("s1", "B", 5.0, SupportType.ROLLER)),
            listOf(Hinge("h0", "K", 3.0)),
            listOf(PointForce("f1", "F_1", 4.0, 10.0, 270.0)),
        )
        val its = allItems(g)
        val ds = symbolsOf(g, its)
        val specs = Side.entries.map { EqSpec.Hinge("h0", "K", 3.0, it) }
        val r = analyzeBest(parseEquation("RB*2 - F1*1 = 0", ds), specs, its, ds)
        assertTrue(r.ok)
        assertEquals(Side.RIGHT, (r.spec as EqSpec.Hinge).side)
        assertTrue(analyzeBest(parseEquation("MA - RAy*3 = 0", ds), specs, its, ds).ok)
    }
}
