package cz.usharik.statika

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import cz.usharik.statika.i18n.Lang
import cz.usharik.statika.i18n.Tr
import cz.usharik.statika.model.BeamType
import cz.usharik.statika.model.EqSpec
import cz.usharik.statika.model.GenOptions
import cz.usharik.statika.model.ParseError
import cz.usharik.statika.model.PointForce
import cz.usharik.statika.model.Udl
import cz.usharik.statika.model.allItems
import cz.usharik.statika.model.analyze
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.forceComponents
import cz.usharik.statika.model.generate
import cz.usharik.statika.model.parseEquation
import cz.usharik.statika.model.reactionSetOf
import cz.usharik.statika.model.symbolsOf
import cz.usharik.statika.trainer.EquationsStep
import cz.usharik.statika.trainer.HingeAnswer
import cz.usharik.statika.trainer.ReleaseTask
import cz.usharik.statika.trainer.TrainerSession
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Případy nalezené při revizi kódu. */
class ReviewRegressionTest {
    private val tr = Tr(Lang.CS)

    private fun toEquations(seed: Int, level: Int, types: List<BeamType>): EquationsStep {
        val s = TrainerSession(generate(seed, GenOptions(level, types)))
        val r = s.release
        while (s.step == 0) {
            when (val t = r.task) {
                is ReleaseTask.SupportTask -> r.pickSet(reactionSetOf(t.type))
                is ReleaseTask.HingeTask -> r.pickHinge(HingeAnswer.FORCES)
                ReleaseTask.Count -> {
                    r.nU = r.nUnknowns.toString(); r.nE = r.nEqs.toString(); r.checkCount()
                }
            }
            r.advance()
        }
        val l = s.loads!!
        while (l.task != null) {
            when (val t = l.task) {
                is Udl -> { l.a = fmt(t.q * (t.x2 - t.x1), 4); l.b = fmt((t.x2 - t.x1) / 2, 4) }
                is PointForce -> forceComponents(t.f, t.angle).let { l.a = fmt(abs(it.fx), 4); l.b = fmt(abs(it.fy), 4) }
                else -> {}
            }
            l.check(); l.advance()
        }
        l.done()
        return s.equations!!
    }

    @Test
    fun napovedaNepadaNaClenuSeZavorkou() {
        val eq = toEquations(1, 1, listOf(BeamType.SIMPLE))
        eq.onValueChange(1, TextFieldValue("(F_1 + 1)*7", TextRange(11)))
        repeat(4) { eq.hint() } // dřív StringIndexOutOfBoundsException
    }

    @Test
    fun napovedaSeNezasekneNaSpatnemZacatku() {
        for (level in 1..3) for (type in BeamType.entries) for (seed in 1..25) {
            val eq = toEquations(seed * 104729 % 999999 + 1, level, listOf(type))
            val u = eq.unknownDefs.first().name
            val k = eq.knownDefs.first().name
            val starts = listOf("$u + ", "$u + (", "$u = 0 = 0", "$u + 5", "2*$u + 2*$k", "($k + 1)*7", "$u*sqrt(-1)")
            for ((i, start) in starts.withIndex()) {
                val row = i % eq.rows.size
                if (eq.rows[row].done) continue
                eq.onValueChange(row, TextFieldValue(start, TextRange(start.length)))
                var n = 0
                while (!eq.rows[row].done && n < 30) {
                    eq.hint(); eq.check(row); n++
                }
                assertTrue(eq.rows[row].done, "úloha $seed/$level/$type, začátek „$start“ → „${eq.rows[row].text}“ ${eq.rows[row].msgs.map { it.text(tr) }}")
            }
        }
    }

    @Test
    fun cisloRamenaSeNepriradiZatizeniSeStejnouHodnotou() {
        // ramena napsaná jako čísla před silou: „1*12“ – 1 je rameno, ne zatížení q = 1
        var tried = 0
        for (seed in 1..400) {
            val p = generate(seed, GenOptions(1, BeamType.entries))
            val items = allItems(p)
            val defs = symbolsOf(p, items)
            val a = p.supports.first()
            val spec = EqSpec.M(a.label, a.x)
            // správná rovnice: reakce značkami, zatížení „rameno*velikost“
            val parts = mutableListOf<String>()
            for (it in items) {
                val e = cz.usharik.statika.model.expected(it, spec)
                if (e.sign == 0) continue
                val sg = if (e.sign < 0) "-" else "+"
                val armTxt = e.arm?.let { fmt(it, 4) }
                parts += if (it.unknownId != null) "$sg ${it.sym}" + (armTxt?.let { "*$it" } ?: "")
                else "$sg " + (armTxt?.let { "$it*" } ?: "") + fmt(it.value!!, 6)
            }
            if (parts.isEmpty()) continue
            val text = parts.joinToString(" ") + " = 0"
            val parsed = parseEquation(text, defs)
            val res = analyze(parsed, spec, items, defs)
            tried++
            assertTrue(res.ok, "úloha $seed: „$text“ → ${res.issues.map { it.kind to it.item.sym }} stray=${res.stray}")
        }
        assertTrue(tried > 300)
    }

    @Test
    fun formatCisel() {
        assertEquals("0,01", fmt(0.005))
        assertEquals("0", fmt(0.0049))
        assertEquals("−2,5", fmt(-2.5))
        assertTrue(fmt(1e15, 4).contains("E"), "velké číslo nesmí přetéct přes Long")
        assertTrue(fmt(Double.NaN).isNotEmpty())
    }

    @Test
    fun odmocninaZeZapornehoNeniPlatnyZapis() {
        val p = generate(3199, GenOptions(3, listOf(BeamType.OVERHANG)))
        val defs = symbolsOf(p, allItems(p))
        assertIs<ParseError.BadValue>(parseEquation("R_Ax + sqrt(-1)", defs).error)
    }
}
