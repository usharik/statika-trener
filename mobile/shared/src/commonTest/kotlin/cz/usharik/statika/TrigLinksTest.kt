package cz.usharik.statika

import androidx.compose.ui.text.input.TextFieldValue
import cz.usharik.statika.i18n.Lang
import cz.usharik.statika.i18n.Tr
import cz.usharik.statika.model.BeamType
import cz.usharik.statika.model.EqSpec
import cz.usharik.statika.model.GenOptions
import cz.usharik.statika.model.PointForce
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.Udl
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.forceComponents
import cz.usharik.statika.model.generate
import cz.usharik.statika.model.reactionSetOf
import cz.usharik.statika.trainer.HingeAnswer
import cz.usharik.statika.trainer.LoadsStep
import cz.usharik.statika.trainer.ReleaseTask
import cz.usharik.statika.trainer.STEP_EQUATIONS
import cz.usharik.statika.trainer.TrainerSession
import cz.usharik.statika.trainer.Tone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Odkazy z trenažéru do kurzu trigonometrie podle typu chyby u šikmé síly. */
class TrigLinksTest {
    private val tr = Tr(Lang.CS)

    /** úlohy se šikmou silou, jejíž složky se liší (ne 45°) */
    private fun problems(): List<Pair<Problem, PointForce>> = (1..400).mapNotNull { seed ->
        val p = generate(seed * 7919, GenOptions(2, BeamType.entries.toList()))
        val f = p.loads.filterIsInstance<PointForce>().firstOrNull { it.alpha != null && it.alpha != 45 }
        if (f == null) null else p to f
    }.take(25)

    private fun toLoads(s: TrainerSession) {
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
            r.advance()
        }
    }

    /** posune krok 2 na zadanou sílu (ostatní zatížení vyřeší správně) */
    private fun LoadsStep.goTo(target: PointForce) {
        while (task != null && task != target) {
            when (val t = task) {
                is Udl -> { a = fmt(t.q * (t.x2 - t.x1), 4); b = fmt((t.x2 - t.x1) / 2, 4) }
                is PointForce -> forceComponents(t.f, t.angle).let { a = fmt(abs(it.fx), 4); b = fmt(abs(it.fy), 4) }
                else -> error("?")
            }
            check(); advance()
        }
    }

    @Test
    fun kalkulackaVRadianechMaVlastniZpravuAOdkaz() {
        assertTrue(problems().isNotEmpty())
        for ((p, f) in problems()) {
            val s = TrainerSession(p)
            toLoads(s)
            val l = s.loads!!
            l.goTo(f)
            val al = f.alpha!! * 1.0
            // obě složky spočítané v režimu RAD
            l.a = fmt(abs(f.f * cos(al)), 3)
            l.b = fmt(abs(f.f * sin(al)), 3)
            l.check()
            val m = l.msgs.single { it.tone == Tone.NUDGE }
            val c = forceComponents(f.f, f.angle)
            // když RAD náhodou vyjde jako správná/prohozená hodnota, platí jiná zpráva – ta úloha se přeskočí
            if (m.text(tr) != tr("f_rad")) {
                assertTrue(abs(abs(f.f * cos(al)) - abs(c.fx)) < 0.05 || abs(abs(f.f * cos(al)) - abs(c.fy)) < 0.05, "úloha ${p.seed}: ${m.text(tr)}")
                continue
            }
            assertEquals(103 to 1, m.learn)
        }
    }

    @Test
    fun prohozeneACeleSlozkyVedouDoTrigonometrie() {
        for ((p, f) in problems()) {
            val c = forceComponents(f.f, f.angle)
            val s = TrainerSession(p)
            toLoads(s)
            val l = s.loads!!
            l.goTo(f)
            l.a = fmt(abs(c.fy), 4); l.b = fmt(abs(c.fx), 4)
            l.check()
            assertEquals(102 to 1, l.msgs.single { it.tone == Tone.NUDGE }.learn, "prohozené, úloha ${p.seed}")
            l.a = fmt(f.f, 4); l.b = fmt(abs(c.fy), 4)
            l.check()
            assertEquals(102 to 2, l.msgs.single { it.tone == Tone.NUDGE }.learn, "celá síla, úloha ${p.seed}")
            l.a = "1"; l.b = "1"
            l.check()
            assertEquals(3 to 3, l.msgs.single { it.tone == Tone.NUDGE }.learn, "obecná chyba, úloha ${p.seed}")
            // první nápověda odkazuje na výklad sin/cos
            l.hint()
            assertEquals(102 to 0, l.msgs.single { it.tone == Tone.HINT }.learn)
        }
    }

    @Test
    fun rovniceSeSlozkouZRadianuOdkazujeNaKalkulacku() {
        var tested = 0
        for ((p, f) in problems()) {
            val c = forceComponents(f.f, f.angle)
            if (abs(c.fx) < 0.5 || abs(c.fy) < 0.5) continue
            val s = TrainerSession(p)
            toLoads(s)
            val l = s.loads!!
            while (l.task != null) {
                when (val t = l.task) {
                    is Udl -> { l.a = fmt(t.q * (t.x2 - t.x1), 4); l.b = fmt((t.x2 - t.x1) / 2, 4) }
                    is PointForce -> forceComponents(t.f, t.angle).let { l.a = fmt(abs(it.fx), 4); l.b = fmt(abs(it.fy), 4) }
                    else -> error("?")
                }
                l.check(); l.advance()
            }
            l.done()
            assertEquals(STEP_EQUATIONS, s.step)
            val eq = s.equations!!
            val i = eq.rows.indexOfFirst { it.spec == EqSpec.Fx }
            if (i < 0) continue
            eq.focusRow(i)
            var n = 0
            while (!eq.rows[i].done && n < 25) { eq.hint(); eq.check(i); n++ }
            val good = eq.rows[i].text
            // rovnice píše složky značkami (F_1x); žák místo F_1x dosadí číslo spočítané v režimu RAD
            // (prohozenou složku rozbor rovnice přiřadí rovnou F_1y – na to má statika vlastní zprávu)
            val rad = abs(f.f * cos(f.alpha!! * 1.0))
            if (listOf(c.fx, c.fy, f.f).any { abs(abs(it) - rad) < 0.1 } || rad < 0.3) continue
            val sym = f.label + "x"
            if (!Regex("\\b" + Regex.escape(sym) + "\\b").containsMatchIn(good)) continue
            eq.onValueChange(i, TextFieldValue(good.replace(Regex("\\b" + Regex.escape(sym) + "\\b"), fmt(rad))))
            eq.check(i)
            val learn = eq.rows[i].msgs.mapNotNull { it.learn }
            assertTrue(103 to 1 in learn, "úloha ${p.seed}: „${eq.rows[i].text}“ ${eq.rows[i].msgs.map { it.text(tr) to it.learn }}")
            tested++
        }
        assertTrue(tested >= 3, "otestováno jen $tested úloh")
    }

    @Test
    fun chybyRezimuKalkulackyNejsouSpravnaHodnota() {
        // RAD i GRAD dávají pro běžné úhly jiné číslo než DEG
        for (al in listOf(15, 20, 30, 60, 75)) {
            val m = cz.usharik.statika.trainer.calcModeMistakesForTest(10.0, al.toDouble())
            val deg = listOf(abs(10 * cos(al * PI / 180)), abs(10 * sin(al * PI / 180)))
            assertTrue(m.none { x -> deg.any { abs(it - x) < 1e-6 } }, "úhel $al")
        }
    }
}
