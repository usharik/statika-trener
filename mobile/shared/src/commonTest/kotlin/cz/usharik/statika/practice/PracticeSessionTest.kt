package cz.usharik.statika.practice

import cz.usharik.statika.platform.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeSessionTest {
    private val set = PracticeSet("t", cz.usharik.statika.learn.L("Test", "Тест"), listOf("E03" to 1, "E17" to 1), 4, 102)

    @Test
    fun vseSpravneDaHvezdu() {
        val kv = InMemoryKeyValueStore()
        val store = PracticeStore(kv)
        val s = PracticeSession(set, 2, 99, store)
        assertEquals(4, s.tasks.size)
        repeat(4) {
            val ex = s.current!!
            correctInputs(ex).forEachIndexed { i, v -> s.setInput(i, v) }
            s.check()
            assertTrue(s.solved)
            s.next()
        }
        assertTrue(s.finished)
        assertEquals(4, s.score)
        assertEquals(4, store.best("t"))
        assertTrue(store.star("t", 2))
        assertTrue(!store.star("t", 1))
        assertEquals(1, store.runs("t"))
        assertTrue(s.weakTypes().isEmpty())
    }

    @Test
    fun vysledekZNapovedyNeniBod() {
        val store = PracticeStore(InMemoryKeyValueStore())
        val s = PracticeSession(set, 1, 7, store)
        val type = s.current!!.type
        s.setInput(0, "0")
        s.check()
        assertFalse(s.solved)
        assertEquals(1, store.misses(type))
        repeat(3) { s.hint() }
        assertTrue(s.solved)
        assertEquals(3, s.hintLevel)
        assertEquals(correctInputs(s.current!!), s.inputs)
        // po výsledku z nápovědy už nevisí stará zpráva „ještě to nesedí“
        assertTrue(s.msgs.none { it.tone == cz.usharik.statika.trainer.Tone.NUDGE })
        s.next()
        repeat(3) {
            val ex = s.current!!
            correctInputs(ex).forEachIndexed { i, v -> s.setInput(i, v) }
            s.check()
            s.next()
        }
        assertTrue(s.finished)
        assertEquals(3, s.score) // 3 ze 4 = 75 % → hvězda
        assertTrue(store.star("t", 1))
        assertEquals(listOf(type), s.weakTypes().map { it.id }.take(1))
    }

    @Test
    fun cisloSerieUrcujeUlohy() {
        val a = PracticeSession(set, 2, 4127, PracticeStore(InMemoryKeyValueStore()))
        val b = PracticeSession(set, 2, 4127, PracticeStore(InMemoryKeyValueStore()))
        assertEquals(a.tasks.map { it.prompt }, b.tasks.map { it.prompt })
        val c = PracticeSession(set, 3, 4127, PracticeStore(InMemoryKeyValueStore()))
        assertTrue(a.tasks.map { it.prompt } != c.tasks.map { it.prompt })
    }

    @Test
    fun volbaDaBodJenNapoprve() {
        val sel = PracticeSet("v", cz.usharik.statika.learn.L("V", "V"), listOf("E16" to 1), 2, 107)
        val s = PracticeSession(sel, 1, 3, PracticeStore(InMemoryKeyValueStore()))
        val ex = s.current!!
        val wrong = ex.choice!!.indexOfFirst { !it.correct }
        s.choose(wrong)
        assertFalse(s.solved)
        s.choose(ex.choice!!.indexOfFirst { it.correct })
        assertTrue(s.solved)
        assertEquals(0, s.score)
        s.next()
        s.choose(s.current!!.choice!!.indexOfFirst { it.correct })
        assertEquals(1, s.score)
        assertEquals(1, s.firstTries)
    }

    @Test
    fun cislaDajiBodNejvysPoDvouChybach() {
        val sel = PracticeSet("n", cz.usharik.statika.learn.L("N", "N"), listOf("E03" to 1), 2, 102)
        val s = PracticeSession(sel, 2, 11, PracticeStore(InMemoryKeyValueStore()))
        repeat(2) { s.setInput(0, "0,001"); s.check() }
        correctInputs(s.current!!).forEachIndexed { i, v -> s.setInput(i, v) }
        s.check()
        assertEquals(1, s.score)
        assertEquals(0, s.firstTries)
        s.next()
        repeat(3) { s.setInput(0, "0,001"); s.check() }
        correctInputs(s.current!!).forEachIndexed { i, v -> s.setInput(i, v) }
        s.check()
        assertTrue(s.solved)
        assertEquals(1, s.score)
    }

    @Test
    fun prazdnyANecitelnyZapisNeniChyba() {
        val sel = PracticeSet("e", cz.usharik.statika.learn.L("E", "E"), listOf("E03" to 1), 1, 102)
        val s = PracticeSession(sel, 2, 11, PracticeStore(InMemoryKeyValueStore()))
        s.check()
        s.setInput(0, "5 m"); s.check()
        correctInputs(s.current!!).forEachIndexed { i, v -> s.setInput(i, v) }
        s.check()
        assertEquals(1, s.firstTries)
    }

    @Test
    fun hvezdyPodleUrovniAStaraHvezda() {
        val kv = InMemoryKeyValueStore()
        // starší verze: jedna hvězda na soubor → platí jako lehká úroveň
        kv.putInt("practice.tg.p1.star", 1)
        val store = PracticeStore(kv)
        assertTrue(store.star("p1", 1))
        assertFalse(store.star("p1", 2))
        assertEquals(1, store.stars("p1"))
    }

    @Test
    fun nazvyStranNejvysDvakratZaSerii() {
        val p1 = PracticeSets.byId("p1")!!
        for (seed in 1..200) for (level in 1..3) {
            val n = seriesFor(p1, level, seed).count { it.type == "E01" }
            assertTrue(n <= 2, "p1/$level/$seed: E01 ${n}×")
        }
    }
}
