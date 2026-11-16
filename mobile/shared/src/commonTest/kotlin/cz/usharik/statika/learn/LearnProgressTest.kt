package cz.usharik.statika.learn

import androidx.compose.runtime.snapshots.Snapshot
import cz.usharik.statika.platform.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LearnProgressTest {
    /** Odpověď „Ne“ v dialogu prvního spuštění musí změnit pozorovaný stav, jinak dialog zůstane viset. */
    @Test
    fun odpovedNaPrvniSpusteniJePozorovatelna() {
        val store = InMemoryKeyValueStore()
        val p = LearnProgress(store)
        assertFalse(p.firstRunAsked)
        val changed = mutableListOf<Any>()
        val handle = Snapshot.registerApplyObserver { set, _ -> changed.addAll(set) }
        try {
            p.firstRunAsked = true
            Snapshot.sendApplyNotifications()
        } finally {
            handle.dispose()
        }
        assertTrue(changed.isNotEmpty(), "firstRunAsked není stav Compose")
        assertTrue(p.firstRunAsked)
        assertEquals(1, store.getInt("learn.firstRun", 0))
        assertTrue(LearnProgress(store).firstRunAsked)
    }

    /** Postup lekcí trigonometrie (id 101+) se neplete se statikou; vybraný kurz se pamatuje. */
    @Test
    fun kurzyMajiOddelenyPostup() {
        val store = InMemoryKeyValueStore()
        val p = LearnProgress(store)
        assertEquals("st", p.course)
        assertEquals(1, p.next(Courses.statics))
        assertEquals(101, p.next(Courses.trig))
        p.markDone(101)
        assertEquals(102, p.next(Courses.trig))
        assertEquals(1, p.next(Courses.statics))
        assertFalse(p.isDone(1))
        p.course = "tg"
        assertEquals(102, p.next())
        assertEquals("tg", LearnProgress(store).course)
        // neznámý kurz v úložišti → statika
        store.putString("learn.course", "xx")
        assertEquals("st", LearnProgress(store).course)
        assertEquals(1, Lessons.byId(101).no)
        assertEquals(Courses.trig, Lessons.courseOf(105))
    }
}
