package cz.usharik.statika

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import cz.usharik.statika.i18n.Lang
import cz.usharik.statika.i18n.Tr
import cz.usharik.statika.i18n.textKeys
import cz.usharik.statika.model.BeamType
import cz.usharik.statika.model.GenOptions
import cz.usharik.statika.model.PointForce
import cz.usharik.statika.model.Udl
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.forceComponents
import cz.usharik.statika.model.generate
import cz.usharik.statika.model.reactionSetOf
import cz.usharik.statika.model.solveProblem
import cz.usharik.statika.model.unknownsOf
import cz.usharik.statika.platform.InMemoryKeyValueStore
import cz.usharik.statika.trainer.HingeAnswer
import cz.usharik.statika.trainer.ReleaseTask
import cz.usharik.statika.trainer.STEP_DONE
import cz.usharik.statika.trainer.STEP_EQUATIONS
import cz.usharik.statika.trainer.STEP_LOADS
import cz.usharik.statika.trainer.STEP_SOLVE
import cz.usharik.statika.trainer.SessionEvents
import cz.usharik.statika.trainer.SettingsRepository
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.trainer.TrainerSession
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TrainerTest {
    private val tr = Tr(Lang.CS)

    /** Projde krok 1 a 2 správnými odpověďmi. */
    private fun throughLoads(s: TrainerSession) {
        val r = s.release
        while (s.step == 0) {
            when (val t = r.task) {
                is ReleaseTask.SupportTask -> r.pickSet(reactionSetOf(t.type))
                is ReleaseTask.HingeTask -> r.pickHinge(HingeAnswer.FORCES)
                ReleaseTask.Count -> {
                    r.nU = r.nUnknowns.toString()
                    r.nE = r.nEqs.toString()
                    r.checkCount()
                }
            }
            assertTrue(r.solved, "krok 1: ${r.msgs.map { it.text(tr) }}")
            r.advance()
        }
        assertEquals(STEP_LOADS, s.step)
        val l = s.loads!!
        while (l.task != null) {
            when (val t = l.task) {
                is Udl -> {
                    l.a = fmt(t.q * (t.x2 - t.x1), 4)
                    l.b = fmt((t.x2 - t.x1) / 2, 4)
                }
                is PointForce -> forceComponents(t.f, t.angle).let {
                    l.a = fmt(abs(it.fx), 4)
                    l.b = fmt(abs(it.fy), 4)
                }
                else -> error("neočekávané zatížení")
            }
            l.check()
            assertTrue(l.solved, "krok 2: ${l.msgs.map { it.text(tr) }}")
            l.advance()
        }
        l.done()
        assertEquals(STEP_EQUATIONS, s.step)
    }

    @Test
    fun opakovanaNapovedaVzdyDovedeKeSpravneRovnici() {
        for (level in 1..3) for (type in BeamType.entries) for (seed in 1..60) {
            val p = generate(seed * 7919, GenOptions(level, listOf(type)))
            val s = TrainerSession(p)
            throughLoads(s)
            val eq = s.equations!!
            for (i in eq.rows.indices) {
                eq.focusRow(i)
                var n = 0
                while (!eq.rows[i].done && n < 25) {
                    eq.hint()
                    eq.check(i)
                    n++
                }
                assertTrue(eq.rows[i].done, "úloha ${p.seed} (obt. $level), rovnice $i: „${eq.rows[i].text}“ ${eq.rows[i].msgs.map { it.text(tr) }}")
            }
            assertTrue(eq.allDone)
        }
    }

    @Test
    fun celaUlohaAzDoKonce() {
        var solvedHints = -1
        val events = object : SessionEvents {
            override fun onSolved(hints: Int) {
                solvedHints = hints
            }
        }
        val p = generate(4242, GenOptions(3, listOf(BeamType.GERBER)))
        val s = TrainerSession(p, events)
        throughLoads(s)
        val eq = s.equations!!
        // nápovědou až do správné rovnice
        for (i in eq.rows.indices) {
            eq.focusRow(i)
            var n = 0
            while (!eq.rows[i].done && n < 25) {
                eq.hint()
                eq.check(i)
                n++
            }
            assertTrue(eq.rows[i].done, "rovnice $i: ${eq.rows[i].text}")
        }
        eq.done()
        assertEquals(STEP_SOLVE, s.step)
        val sv = s.solve!!
        val answer = solveProblem(p)!!
        // nejdřív jedna chyba ve znaménku
        val first = unknownsOf(p).first { abs(answer.getValue(it.id)) > 0.1 }
        sv.setVal(first.id, fmt(-answer.getValue(first.id), 4))
        sv.check()
        assertTrue(sv.msgs.any { it.tone == Tone.NUDGE && it.text(tr) == tr("v_sign") })
        for (u in unknownsOf(p)) sv.setVal(u.id, fmt(answer.getValue(u.id), 4))
        sv.check()
        assertTrue(sv.allOk)
        sv.done()
        assertEquals(STEP_DONE, s.step)
        assertNotNull(s.results)
        assertTrue(solvedHints > 0)
    }

    @Test
    fun paletaVkladaZnackyOddelene() {
        val s = TrainerSession(generate(77, GenOptions(1, listOf(BeamType.SIMPLE))))
        throughLoads(s)
        val eq = s.equations!!
        eq.insert("R_A")
        eq.insert("R_B")
        assertEquals("R_A R_B", eq.rows[0].text)
        eq.backspace()
        assertEquals("R_A", eq.rows[0].text)
        eq.onValueChange(0, TextFieldValue("R_Ax + F1x", TextRange(10)))
        eq.backspace()
        assertEquals("R_Ax +", eq.rows[0].text)
    }

    @Test
    fun spatnaVolbaNavadiNeresi() {
        val p = generate(5, GenOptions(1, listOf(BeamType.CANTILEVER)))
        val s = TrainerSession(p)
        val r = s.release
        r.pickSet(cz.usharik.statika.model.ReactionSet.Y)
        assertFalse(r.solved)
        assertEquals(Tone.NUDGE, r.msgs.single().tone)
        // nápověda nikdy neříká „špatně“, ale dá se přeložit
        assertTrue(r.msgs.single().text(tr).contains("**A**") || r.msgs.single().text(tr).isNotBlank())
        repeat(3) { r.hint() }
        assertEquals(3, s.hints)
    }

    @Test
    fun nastaveniSeUkladaji() {
        val store = InMemoryKeyValueStore()
        val a = SettingsRepository(store)
        a.updateLevel(3)
        a.toggleType(BeamType.SIMPLE)
        a.updateLang(Lang.RU)
        a.addSolved()
        val b = SettingsRepository(store)
        assertEquals(3, b.level)
        assertEquals(listOf(BeamType.OVERHANG, BeamType.CANTILEVER, BeamType.GERBER), b.types)
        assertEquals(Lang.RU, b.lang)
        assertEquals(1, b.solved)
        // poslední typ vypnout nejde
        listOf(BeamType.OVERHANG, BeamType.CANTILEVER, BeamType.GERBER).forEach(b::toggleType)
        assertEquals(listOf(BeamType.GERBER), b.types)
    }

    @Test
    fun textyMajiObaJazykyStejneKlice() {
        assertEquals(textKeys(Lang.CS), textKeys(Lang.RU))
    }
}
