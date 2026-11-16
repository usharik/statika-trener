package cz.usharik.statika.learn

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import cz.usharik.statika.ui.StatikaTheme
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NumAnswerTest {
    private val fields = listOf(NumSpec(L("[[F_x]]", "[[F_x]]"), 17.32, "kN"), NumSpec(L("[[F_y]]", "[[F_y]]"), 10.0, "kN"))
    private val swap = L("Prohozené složky", "Перепутаны")

    @Test
    fun zamcenoDokudNeniSpravne() = runComposeUiTest {
        val scope = CardScope(alreadyDone = false)
        setContent {
            StatikaTheme(dark = false) {
                Column { scope.NumAnswer(L("Kolik?", "Сколько?"), fields, L("Vysvětlení", "Объяснение"), listOf(NumTrap.near(swap, 10.0, 17.32))) }
            }
        }
        waitForIdle()
        assertFalse(scope.done)
        val inputs = onAllNodes(hasSetTextAction())
        inputs[0].performTextInput("10")
        inputs[1].performTextInput("17,32")
        onNodeWithText("Zkontrolovat").performClick()
        waitForIdle()
        onNodeWithText("Prohozené složky").assertExists()
        assertFalse(scope.done)
        inputs[0].performTextClearance()
        inputs[0].performTextInput("17,3")
        inputs[1].performTextClearance()
        inputs[1].performTextInput("10")
        onNodeWithText("Zkontrolovat").performClick()
        waitForIdle()
        assertTrue(scope.done)
        onNodeWithText("Vysvětlení").assertExists()
    }

    @Test
    fun tretiChybaUkazeVysledek() = runComposeUiTest {
        val scope = CardScope(alreadyDone = false)
        setContent {
            StatikaTheme(dark = false) {
                Column { scope.NumAnswer(L("Kolik?", "Сколько?"), fields, L("Vysvětlení", "Объяснение")) }
            }
        }
        val inputs = onAllNodes(hasSetTextAction())
        inputs[0].performTextInput("1")
        inputs[1].performTextInput("1")
        repeat(3) {
            onNodeWithText("Zkontrolovat").performClick()
            waitForIdle()
        }
        assertTrue(scope.done)
        assertTrue(onAllNodesWithText("Vysvětlení").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun nevimNejdrivNapovedaPakPostup() = runComposeUiTest {
        val scope = CardScope(alreadyDone = false)
        setContent {
            StatikaTheme(dark = false) {
                Column { scope.NumAnswer(L("Kolik?", "Сколько?"), fields, L("Vysvětlení", "Объяснение"), nudge = L("Nápověda k úloze", "Подсказка")) }
            }
        }
        waitForIdle()
        onNodeWithText("Nevím – ukaž postup").performClick()
        waitForIdle()
        onNodeWithText("Nápověda k úloze").assertExists()
        assertFalse(scope.done)
        onNodeWithText("Ukaž celý postup").performClick()
        waitForIdle()
        assertTrue(scope.done)
        onNodeWithText("Vysvětlení").assertExists()
    }
}
