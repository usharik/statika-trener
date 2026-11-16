package cz.usharik.statika.trainer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.usharik.statika.i18n.Tr
import cz.usharik.statika.model.EqSpec
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.Side

enum class Tone { NUDGE, OK, INFO, HINT }

/** Zpráva žákovi; text se překládá až při vykreslení (přepnutí jazyka funguje i uprostřed úlohy). */
class Msg(val tone: Tone, val text: (Tr) -> String) {
    /** odkaz do výkladu (lekce, karta) – u typických chyb se pod zprávou nabídne „Podívej se: …“ */
    var learn: Pair<Int, Int>? = null
        private set

    fun learn(lesson: Int, card: Int = 0): Msg = apply { learn = lesson to card }
}

data class ArmView(val from: Double, val to: Double)
data class SideView(val x: Double, val side: Side)

/** Co navíc ukázat ve výkresu na kroku „Rovnice“. */
data class DrawingView(
    val momentPoint: Pair<String, Double>? = null,
    val side: SideView? = null,
    val arm: ArmView? = null,
)

/** Rovnice předaná do výpočtu; cw = rovnice vynásobená −1 (tak ji žák napsal). */
data class UserEq(val spec: EqSpec, val cw: Boolean)

/** Události pro analytiku a statistiku. */
interface SessionEvents {
    fun onHint(step: Int) {}
    fun onStepDone(step: Int, hints: Int) {}
    fun onSolved(hints: Int) {}
}

object NoEvents : SessionEvents

const val STEP_RELEASE = 0
const val STEP_LOADS = 1
const val STEP_EQUATIONS = 2
const val STEP_SOLVE = 3
const val STEP_DONE = 4

/** Průchod jednou úlohou: krok, co je už v obrázku nahrazené, a stav aktuálního kroku. */
class TrainerSession(val problem: Problem, private val events: SessionEvents = NoEvents) {
    var step by mutableIntStateOf(STEP_RELEASE)
        private set
    var released by mutableStateOf(setOf<String>())
        private set
    var resolved by mutableStateOf(setOf<String>())
        private set
    var highlight by mutableStateOf<String?>(null)
    var activeSupport by mutableStateOf(problem.supports.firstOrNull()?.id)
    var view by mutableStateOf(DrawingView())
    var results by mutableStateOf<Map<String, Double>?>(null)
        private set
    var hints by mutableIntStateOf(0)
        private set

    val release = ReleaseStep(problem, this)
    var loads by mutableStateOf<LoadsStep?>(null)
        private set
    var equations by mutableStateOf<EquationsStep?>(null)
        private set
    var solve by mutableStateOf<SolveStep?>(null)
        private set

    internal fun hint() {
        hints += 1
        events.onHint(step)
    }

    internal fun release(id: String) {
        released = released + id
    }

    internal fun resolve(id: String) {
        resolved = resolved + id
    }

    internal fun releaseDone() {
        if (step != STEP_RELEASE) return
        activeSupport = null
        highlight = null
        events.onStepDone(STEP_RELEASE, hints)
        loads = LoadsStep(problem, this)
        step = STEP_LOADS
    }

    internal fun loadsDone() {
        if (step != STEP_LOADS) return
        highlight = null
        events.onStepDone(STEP_LOADS, hints)
        equations = EquationsStep(problem, this)
        step = STEP_EQUATIONS
    }

    internal fun equationsDone(eqs: List<UserEq>) {
        if (step != STEP_EQUATIONS) return
        highlight = null
        view = DrawingView()
        events.onStepDone(STEP_EQUATIONS, hints)
        solve = SolveStep(problem, eqs, this)
        step = STEP_SOLVE
    }

    internal fun solveDone(r: Map<String, Double>) {
        if (step != STEP_SOLVE) return
        highlight = null
        results = r
        events.onStepDone(STEP_SOLVE, hints)
        events.onSolved(hints)
        step = STEP_DONE
    }
}
