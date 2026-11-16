package cz.usharik.statika.trainer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.ReactionSet
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.model.parseNum
import cz.usharik.statika.model.reactionSetOf
import cz.usharik.statika.model.unknownsOf

sealed interface ReleaseTask {
    data class SupportTask(val id: String, val label: String, val type: SupportType) : ReleaseTask
    data class HingeTask(val id: String, val label: String) : ReleaseTask
    data object Count : ReleaseTask
}

enum class HingeAnswer(val key: String) { FORCES("forces"), ALL("all"), NONE("none") }

/** Krok 1: uvolnění – u každé vazby výběr reakcí, u kloubu co přenáší, nakonec statická určitost. */
class ReleaseStep(private val p: Problem, private val session: TrainerSession) {
    val tasks: List<ReleaseTask> =
        p.supports.map { ReleaseTask.SupportTask(it.id, it.label, it.type) } +
            p.hinges.map { ReleaseTask.HingeTask(it.id, it.label) } +
            ReleaseTask.Count

    var idx by mutableIntStateOf(0)
        private set
    var msgs by mutableStateOf(listOf<Msg>())
        private set
    var hintLevel by mutableIntStateOf(0)
        private set
    var solved by mutableStateOf(false)
        private set
    /** vybraná odpověď (ReactionSet nebo HingeAnswer) */
    var picked by mutableStateOf<Any?>(null)
        private set
    var nU by mutableStateOf("")
    var nE by mutableStateOf("")
    var log by mutableStateOf(listOf<Msg>())
        private set

    val task: ReleaseTask get() = tasks[idx]
    val nUnknowns = unknownsOf(p).size
    val nEqs = 3 + p.hinges.size
    val maxHint: Int get() = if (task is ReleaseTask.SupportTask) 3 else 2

    fun advance() {
        val next = idx + 1
        if (next >= tasks.size) return session.releaseDone()
        msgs.firstOrNull { it.tone == Tone.OK }?.let { log = log + it }
        idx = next
        msgs = emptyList()
        hintLevel = 0
        solved = false
        picked = null
        session.activeSupport = (tasks[next] as? ReleaseTask.SupportTask)?.id
    }

    fun pickSet(set: ReactionSet) {
        val t = task as? ReleaseTask.SupportTask ?: return
        if (solved) return
        picked = set
        if (set == reactionSetOf(t.type)) {
            solved = true
            session.release(t.id)
            msgs = listOf(Msg(Tone.OK) { tr -> tr("s1_ok_${t.type.key}") })
        } else {
            msgs = listOf(Msg(Tone.NUDGE) { tr -> tr("s1_${t.type.key}_${set.key}", "p" to t.label) }.learn(6, 1))
        }
    }

    fun pickHinge(a: HingeAnswer) {
        val t = task as? ReleaseTask.HingeTask ?: return
        if (solved) return
        picked = a
        if (a == HingeAnswer.FORCES) {
            solved = true
            msgs = listOf(Msg(Tone.OK) { tr -> tr("s1_hinge_ok", "p" to t.label) })
        } else {
            msgs = listOf(Msg(Tone.NUDGE) { tr -> tr(if (a == HingeAnswer.ALL) "s1_hinge_all" else "s1_hinge_none") }.learn(9, 0))
        }
    }

    fun checkCount() {
        if (solved) return
        val u = parseNum(nU)
        val e = parseNum(nE)
        val out = mutableListOf<Msg>()
        if (u != nUnknowns.toDouble()) out += Msg(Tone.NUDGE) { tr -> tr("count_u") }.learn(9, 2)
        if (e != nEqs.toDouble()) out += Msg(Tone.NUDGE) { tr -> tr(if (p.hinges.isNotEmpty() && e == 3.0) "count_eh" else "count_e3") }.learn(9, 2)
        if (out.isEmpty()) {
            solved = true
            out += Msg(Tone.OK) { tr -> tr("count_ok", "n" to nUnknowns) }
        }
        msgs = out
    }

    fun hint() {
        session.hint()
        val lvl = hintLevel + 1
        hintLevel = lvl
        val text: Msg = when (val t = task) {
            is ReleaseTask.SupportTask -> Msg(Tone.HINT) { tr ->
                val setName = tr("set_${reactionSetOf(t.type).key}")
                when (lvl) {
                    1 -> tr("s1_h1", "p" to t.label)
                    2 -> tr("s1_h2_${t.type.key}")
                    else -> tr("s1_h3", "p" to t.label, "set" to setName)
                }
            }
            is ReleaseTask.HingeTask -> Msg(Tone.HINT) { tr ->
                if (lvl == 1) tr("s1_hinge_h1") else tr("s1_h3", "p" to t.label, "set" to tr("hinge_forces"))
            }
            ReleaseTask.Count -> Msg(Tone.HINT) { tr ->
                if (lvl == 1) tr("count_h1") else tr("count_h2", "n" to nUnknowns, "r" to nEqs)
            }
        }
        msgs = msgs.filter { it.tone != Tone.HINT } + text
    }
}

/** Popisek sady reakcí, např. „[[R_Ax]], [[R_Ay]]“. */
fun setLabel(set: ReactionSet, label: String): String = when (set) {
    ReactionSet.XY -> listOf("[[R_${label}x]]", "[[R_${label}y]]")
    ReactionSet.Y -> listOf("[[R_$label]]")
    ReactionSet.X -> listOf("[[R_${label}x]]")
    ReactionSet.XYM -> listOf("[[R_${label}x]]", "[[R_${label}y]]", "[[M_$label]]")
}.joinToString(", ")
