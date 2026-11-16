package cz.usharik.statika

import cz.usharik.statika.model.BeamType
import cz.usharik.statika.model.GenOptions
import cz.usharik.statika.model.generate
import cz.usharik.statika.platform.InMemoryKeyValueStore
import cz.usharik.statika.trainer.FavoritesRepository
import cz.usharik.statika.trainer.decodeOpts
import cz.usharik.statika.trainer.encode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FavoritesTest {
    private val a = GenOptions(2, listOf(BeamType.SIMPLE, BeamType.GERBER))
    private val b = GenOptions(1, BeamType.entries.toList())

    @Test
    fun pridaniOdebraniAUlozeni() {
        val store = InMemoryKeyValueStore()
        val f = FavoritesRepository(store)
        assertTrue(f.toggle(123, a, solved = false))
        assertTrue(f.toggle(456, b, solved = true))
        assertTrue(f.isFavorite(123, a))
        // stejné číslo s jiným nastavením je jiná úloha
        assertFalse(f.isFavorite(123, b))
        // nejnovější první, po znovunačtení stejné
        val again = FavoritesRepository(store)
        assertEquals(listOf(456, 123), again.items.map { it.seed })
        assertEquals(a, again.items[1].opts)
        assertTrue(again.items[0].solved)
        assertFalse(again.toggle(123, a, solved = false))
        assertEquals(listOf(456), FavoritesRepository(store).items.map { it.seed })
    }

    @Test
    fun vyresenaDostaneFajfku() {
        val store = InMemoryKeyValueStore()
        val f = FavoritesRepository(store)
        f.toggle(7, a, solved = false)
        f.markSolved(7, GenOptions(2, listOf(BeamType.GERBER, BeamType.SIMPLE)))
        assertTrue(FavoritesRepository(store).items.single().solved)
    }

    @Test
    fun ulozenaUlohaSeOtevreStejna() {
        // stejné číslo + uložené nastavení = přesně stejná úloha (i když se nastavení aplikace změní)
        val p1 = generate(4242, a)
        val p2 = generate(4242, decodeOpts(a.encode())!!)
        assertEquals(p1, p2)
    }

    @Test
    fun poskozenaDataSeIgnoruji() {
        assertNull(decodeOpts("9:simple"))
        assertNull(decodeOpts("2:"))
        assertNull(decodeOpts("abc"))
        val store = InMemoryKeyValueStore()
        store.putString("favorites", "x;12,2:simple,0;;5,bad,1")
        assertEquals(listOf(12), FavoritesRepository(store).items.map { it.seed })
    }
}
