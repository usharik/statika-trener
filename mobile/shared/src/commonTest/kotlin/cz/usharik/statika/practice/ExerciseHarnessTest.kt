package cz.usharik.statika.practice

import cz.usharik.statika.model.fmt
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Obecná kontrola VŠECH typů úloh v [Exercises] (i těch, které přibudou): každý typ × úroveň × mnoho čísel úloh.
 * Nový typ se sem nepřidává – projde se sám.
 */
class ExerciseHarnessTest {
    private val seeds = 1..500

    private fun each(block: (ExerciseType, Int, Int, Exercise) -> Unit) {
        assertTrue(Exercises.all.isNotEmpty())
        for (t in Exercises.all) for (level in t.levels) for (seed in seeds) {
            val ex = try {
                Exercises.make(t.id, seed, level)
            } catch (e: Throwable) {
                fail("${t.id} úroveň $level seed $seed: generátor spadl: $e")
            }
            block(t, level, seed, ex)
        }
    }

    private fun texty(l: cz.usharik.statika.learn.L, kde: String) {
        assertTrue(l.cs.isNotBlank() && l.ru.isNotBlank(), "$kde: prázdný text")
        for (s in listOf(l.cs, l.ru)) {
            assertEquals(s.split("[[").size, s.split("]]").size, "$kde: nespárované [[ ]] v „$s“")
            assertTrue(!s.contains("{") && !s.contains("}"), "$kde: nedosazená proměnná v „$s“")
        }
    }

    @Test
    fun generatoryDavajiPlatneUlohy() = each { t, level, seed, ex ->
        val kde = "${t.id}/$level/$seed"
        assertEquals(t.id, ex.type, kde)
        assertEquals(3, ex.hints.size, kde)
        texty(ex.prompt, "$kde zadání")
        ex.hints.forEachIndexed { i, h -> texty(h, "$kde nápověda $i") }
        texty(ex.ok, "$kde ok")
        texty(ex.generic, "$kde generic")
        val choice = ex.choice
        if (choice != null) {
            assertEquals(1, choice.count { it.correct }, "$kde: právě jedna správná možnost")
            assertEquals(choice.size, choice.map { it.label.cs }.toSet().size, "$kde: duplicitní možnosti")
            assertTrue(choice.size in 2..6, kde)
        } else {
            assertTrue(ex.fields.isNotEmpty(), kde)
            for (f in ex.fields) {
                assertTrue(f.answer.isFinite(), "$kde: odpověď ${f.answer}")
                assertTrue(abs(f.answer) < 10_000, "$kde: odpověď ${f.answer}")
                texty(f.label, "$kde pole")
                if (f.tol == Tol.ANGLE) assertTrue(f.answer > -360 && f.answer < 360, "$kde: úhel ${f.answer}")
                if (f.tol == Tol.RATIO) assertTrue(f.answer.isFinite(), kde)
            }
        }
        // deterministické
        val again = Exercises.make(t.id, seed, level)
        assertEquals(ex.prompt, again.prompt, "$kde: stejné číslo = stejná úloha")
        assertEquals(ex.fields.map { it.answer }, again.fields.map { it.answer }, kde)
    }

    @Test
    fun spravnaOdpovedProjde() = each { t, level, seed, ex ->
        val kde = "${t.id}/$level/$seed"
        val ok = correctInputs(ex)
        assertTrue(check(ex, ok).solved, "$kde: správná odpověď $ok neprošla")
        if (ex.choice == null) {
            // tečka místo čárky, 3 desetinná místa
            assertTrue(check(ex, ex.fields.map { fmt(it.answer, 3).replace(',', '.').replace('−', '-') }).solved, "$kde: zápis s tečkou")
            // u polí bez znaménka nevadí opačné znaménko
            if (ex.fields.none { it.signed }) assertTrue(check(ex, ex.fields.map { "-" + fmt(abs(it.answer), 2) }).solved, "$kde: znaménko u pole bez znaménka")
        }
    }

    @Test
    fun typickeChybySePoznajiANeprojdou() = each { t, level, seed, ex ->
        val kde = "${t.id}/$level/$seed"
        for (trap in ex.traps) {
            val inputs = ex.fields.mapIndexed { i, f ->
                val w = trap.wrong.getOrNull(i)
                fmt(w ?: f.answer, 3).replace('−', '-')
            }
            val r = check(ex, inputs)
            assertTrue(!r.solved, "$kde: chyba ${trap.code} ($inputs) prošla jako správná")
            assertTrue(r.trap != null, "$kde: chyba ${trap.code} ($inputs) se nepoznala")
        }
    }

    @Test
    fun chybaNeseHlasiNaSpravneOdpovedi() = each { t, level, seed, ex ->
        if (ex.choice != null) return@each
        val ok = correctInputs(ex)
        val values = ok.map { cz.usharik.statika.model.parseNum(it) }
        for (trap in ex.traps) {
            assertTrue(!matches(trap, ex.fields, ok, values), "${t.id}/$level/$seed: chyba ${trap.code} sedí i na správnou odpověď")
        }
    }

    @Test
    fun seriePokryjeKazdySoubor() {
        for (set in PracticeSets.all) for (level in 1..3) {
            val s = seriesFor(set, level, 4127)
            if (set.available(level).isEmpty()) assertTrue(s.isEmpty()) else {
                assertEquals(set.size, s.size, "${set.id}/$level")
                assertEquals(s.map { it.prompt }, seriesFor(set, level, 4127).map { it.prompt }, "série je deterministická")
            }
        }
    }
}
