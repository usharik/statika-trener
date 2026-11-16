package cz.usharik.statika.learn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import cz.usharik.statika.ui.StatikaTheme
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Každá karta výkladu musí být při první návštěvě zamčená: „Dál“ se odemkne až po splnění úkolů.
 * (Karta bez Todo/Quiz, nebo s úkolem splněným hned na začátku, by žáka pustila dál bez práce.)
 */
@OptIn(ExperimentalTestApi::class)
class CardGateTest {
    @Test
    fun kazdaKartaJeNaZacatkuZamcena() = runComposeUiTest {
        val problems = mutableListOf<String>()
        // některé karty mají nekonečné animace – hodiny ručně, bez čekání na „idle“
        mainClock.autoAdvance = false
        val all = Lessons.all.flatMap { l -> l.cards.indices.map { Triple(l, it, CardScope(alreadyDone = false)) } }
        var current by mutableStateOf(0)
        setContent {
            val (lesson, i, scope) = all[current]
            StatikaTheme(dark = false) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    key(lesson.id, i) { lesson.cards[i].content(scope) }
                }
            }
        }
        for (n in all.indices) {
            current = n
            repeat(5) { mainClock.advanceTimeByFrame() }
            val (lesson, i, scope) = all[n]
            if (scope.done) problems += "lekce ${lesson.id} karta $i: odemčená bez práce"
            if (scope.pending.isEmpty()) problems += "lekce ${lesson.id} karta $i: žádný úkol"
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun drivejsiSplneniKartuOdemkne() {
        assertTrue(CardScope(alreadyDone = true).done)
        assertFalse(CardScope(alreadyDone = false).done)
    }
}
