package cz.usharik.statika.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import cz.usharik.statika.i18n.Lang
import cz.usharik.statika.i18n.Tr
import cz.usharik.statika.i18n.textKeys
import cz.usharik.statika.platform.AppGraph
import cz.usharik.statika.platform.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Prohlídka aplikace: při prvním spuštění se ukáže, projde všemi kroky, každý zvýrazní skutečně viditelné místo
 * (telefon i tablet naležato) a po dokončení se už neukáže.
 */
@OptIn(ExperimentalTestApi::class)
class TourTest {
    private val cs = Tr(Lang.CS)

    private fun SkikoComposeUiTest.frames(n: Int = 20) = repeat(n) { mainClock.advanceTimeByFrame() }

    /** Projde prohlídku tlačítkem „Dál“ a u každého kroku ověří, že zvýrazněné místo leží v okně. */
    private fun walk(width: Int, height: Int) = runDesktopComposeUiTest(width, height) {
        mainClock.autoAdvance = false
        val store = InMemoryKeyValueStore().apply { putString("lang", "cs"); putInt("seed", 1) }
        val graph = AppGraph(store)
        val tour = TourState(store)
        setContent { StatikaApp(graph, systemDark = false, systemLang = "cs", tour = tour) }
        frames()
        val window = Rect(0f, 0f, width.toFloat(), height.toFloat())
        for ((i, step) in TourState.STEPS.withIndex()) {
            assertEquals(i, tour.index)
            onNode(hasText(cs(step.key + "Title")) and hasAnyAncestor(hasTestTag("tourCard"))).assertExists()
            step.spot?.let { spot ->
                val r = tour.spots[spot]
                assertTrue(r != null && r.width > 0f && r.height > 0f, "${width}x$height: $spot nemá místo na obrazovce")
                assertTrue(r.overlaps(window) && r.top >= 0f && r.left >= 0f, "${width}x$height: $spot mimo okno: $r")
            }
            if (i < TourState.STEPS.lastIndex) onNodeWithText(cs("tourNext")).performClick()
            frames()
        }
        onNodeWithText(cs("firstRunNo")).performClick()
        frames()
        assertFalse(tour.running)
        onNodeWithTag("tour").assertDoesNotExist()
        assertEquals(1, store.getInt(TourState.K_DONE, 0))
        assertFalse(TourState(store).running, "po dokončení se prohlídka už neukáže")
    }

    @Test
    fun telefonNaVysku() = walk(400, 860)

    @Test
    fun tabletNalezato() = walk(1280, 800)

    @Test
    fun preskocitUkonci() = runDesktopComposeUiTest(400, 860) {
        mainClock.autoAdvance = false
        val store = InMemoryKeyValueStore()
        val tour = TourState(store)
        setContent { StatikaApp(AppGraph(store), systemDark = false, systemLang = "cs", tour = tour) }
        frames()
        onNodeWithText(cs("tourNext")).performClick()
        frames()
        onNodeWithText(cs("tourBack")).performClick()
        frames()
        assertEquals(0, tour.index)
        onNodeWithText(cs("tourSkip")).performClick()
        frames()
        assertNull(tour.index)
        assertEquals(1, store.getInt(TourState.K_DONE, 0))
        // znovu ze Nastavení
        tour.start()
        frames()
        onNodeWithTag("tour").assertExists()
    }

    @Test
    fun anoOtevreVyklad() = runDesktopComposeUiTest(400, 860) {
        mainClock.autoAdvance = false
        val store = InMemoryKeyValueStore()
        val tour = TourState(store)
        setContent { StatikaApp(AppGraph(store), systemDark = false, systemLang = "cs", tour = tour) }
        frames()
        repeat(TourState.STEPS.size - 1) { tour.next() }
        frames()
        onNodeWithText(cs("firstRunYes")).performClick()
        frames()
        assertFalse(tour.running)
        onNodeWithTag("tour").assertDoesNotExist()
        assertEquals(1, store.getInt("learn.firstRun", 0))
    }

    @Test
    fun textyVeVsechJazycich() {
        val keys = TourState.STEPS.flatMap { listOf(it.key + "Title", it.key + "Text") } + listOf("tourBack", "tourNext", "tourSkip", "tourCount", "tourRestart", "firstRunYes", "firstRunNo")
        for (lang in Lang.entries) for (k in keys) assertTrue(k in textKeys(lang), "$lang: chybí $k")
    }
}
