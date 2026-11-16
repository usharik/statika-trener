package cz.usharik.statika.learn

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LearnTest {
    @Test
    fun lekceJsouUplne() {
        assertEquals((1..9).toList() + (101..108).toList(), Lessons.all.map { it.id })
        assertEquals((1..9).toList(), Courses.statics.lessons.map { it.id })
        assertEquals((101..108).toList(), Courses.trig.lessons.map { it.id })
        for (l in Lessons.all) {
            assertTrue(l.cards.size in 3..8, "lekce ${l.id}: ${l.cards.size} karet")
            assertTrue(l.title.cs.isNotBlank() && l.title.ru.isNotBlank())
            assertTrue(l.no in 1..9)
        }
        // id jednoznačná přes kurzy
        assertEquals(Lessons.all.size, Lessons.all.map { it.id }.toSet().size)
    }

    /** Odkazy z katalogu chyb procvičování a z typů úloh vedou na existující karty. */
    @Test
    fun odkazyProcvicovaniVedouNaExistujiciKarty() {
        val refs = cz.usharik.statika.practice.Traps.all.mapNotNull { it.learn } +
            cz.usharik.statika.practice.Exercises.all.mapNotNull { it.learn }
        for ((l, c) in refs) {
            val lesson = Lessons.all.firstOrNull { it.id == l }
            assertTrue(lesson != null, "odkaz na neexistující lekci $l")
            assertTrue(c < lesson.cards.size, "odkaz na lekci $l kartu $c")
        }
    }

    /** Každý odkaz z trenažéru (learn(l, c), „l to c“ u StepPanel a IssueText) musí vést na existující kartu. */
    @Test
    fun odkazyZTrenazeruVedouNaExistujiciKarty() {
        val src = File("src/commonMain/kotlin/cz/usharik/statika")
        val files = listOf("trainer", "ui").flatMap { File(src, it).walk().filter { f -> f.extension == "kt" }.toList() }
        val refs = mutableSetOf<Pair<Int, Int>>()
        val patterns = listOf(Regex("""\.learn\((\d+), (\d+)\)"""), Regex("""learn = (\d+) to (\d+)"""), Regex("""(\d+) to (\d+)\)? \{|-> (\d+) to (\d+)"""))
        for (f in files) {
            val t = f.readText()
            for (p in patterns) for (m in p.findAll(t)) {
                val g = m.groupValues.drop(1).filter { it.isNotEmpty() }.map { it.toInt() }
                if (g.size == 2 && (g[0] in 1..9 || g[0] in 101..108)) refs += g[0] to g[1]
            }
        }
        // odkazy uvnitř výkladu (LearnJump) – i mezi kurzy
        val jump = Regex("""LearnJump\((\d+), (\d+)""")
        for (f in File(src, "learn").walk().filter { it.extension == "kt" }) for (m in jump.findAll(f.readText())) {
            val (l, c) = m.destructured
            assertTrue(Lessons.all.any { it.id == l.toInt() }, "LearnJump na neexistující lekci $l (${f.name})")
            refs += l.toInt() to c.toInt()
        }
        assertTrue(refs.size >= 10, "nalezeno jen $refs")
        for ((l, c) in refs) assertTrue(c < Lessons.byId(l).cards.size, "odkaz na lekci $l kartu $c, lekce má ${Lessons.byId(l).cards.size} karet")
    }
}
