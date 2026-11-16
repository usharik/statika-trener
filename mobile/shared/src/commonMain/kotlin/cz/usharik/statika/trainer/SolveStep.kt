package cz.usharik.statika.trainer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.usharik.statika.model.Dof
import cz.usharik.statika.model.Item
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.Unknown
import cz.usharik.statika.model.allItems
import cz.usharik.statika.model.close
import cz.usharik.statika.model.closeTyped
import cz.usharik.statika.model.expected
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.parseNum
import cz.usharik.statika.model.solve
import cz.usharik.statika.model.unknownsOf
import kotlin.math.abs

/** Člen rovnice s koeficientem (znaménko × rameno) podle konvence žáka. */
data class Term(val it: Item, val c: Double, val arm: Double?)

fun termsOf(eq: UserEq, items: List<Item>): List<Term> {
    // rovnici ukazujeme se stejným znaménkem, jak ji žák napsal
    val k = if (eq.cw) -1 else 1
    return items.map { item ->
        val e = expected(item, eq.spec)
        Term(item, (e.sign * k) * (e.arm ?: 1.0), e.arm)
    }.filter { it.c != 0.0 }
}

fun unitOf(u: Unknown) = if (u.kind == Dof.M) "kNm" else "kN"

fun paren(v: Double) = if (v < 0) "(${fmt(v)})" else fmt(v)

/** Krok 4: výpočet reakcí ze sestavených rovnic. */
class SolveStep(private val p: Problem, val eqs: List<UserEq>, private val session: TrainerSession) {
    val items = allItems(p)
    val unknowns = unknownsOf(p)
    val answer: Map<String, Double> = solve(eqs.map { it.spec }, items, unknowns)!!
    val eqTerms: List<List<Term>> = eqs.map { termsOf(it, items) }

    var vals by mutableStateOf(mapOf<String, String>())
        private set
    var status by mutableStateOf(mapOf<String, FieldState>())
        private set
    var msgs by mutableStateOf(listOf<Msg>())
        private set
    private var hintState by mutableStateOf<Pair<String, Int>?>(null)
    val hinted: Boolean get() = hintState != null

    val solvedSet: Set<String> get() = status.filterValues { it == FieldState.OK }.keys
    val allOk: Boolean get() = unknowns.all { it.id in solvedSet }

    fun setVal(id: String, v: String) {
        // už správně spočítaná reakce se nemění
        if (status[id] == FieldState.OK) return
        vals = vals + (id to v)
        if (status[id] == FieldState.NUDGE) status = status - id
    }

    private data class Best(val i: Int, val others: Int)

    /** Nejvhodnější rovnice pro neznámou (nejméně dalších nevyřešených neznámých). */
    private fun bestEq(uid: String, solved: Set<String>): Best {
        var best = -1
        var bestScore = Int.MAX_VALUE
        eqTerms.forEachIndexed { i, terms ->
            if (terms.none { it.it.unknownId == uid }) return@forEachIndexed
            val others = terms.count { it.it.unknownId != null && it.it.unknownId != uid && it.it.unknownId !in solved }
            val score = others * 100 + terms.count { it.it.unknownId != null }
            if (score < bestScore) {
                bestScore = score
                best = i
            }
        }
        return Best(best, bestScore / 100)
    }

    /** Další neznámá, kterou lze spočítat hned. */
    private fun nextTarget(): Unknown? {
        val solved = solvedSet
        val open = unknowns.filter { it.id !in solved }
        return open.firstOrNull { bestEq(it.id, solved).others == 0 } ?: open.firstOrNull()
    }

    private fun eqLabel(i: Int) = eqTitle(eqs[i].spec)

    fun check() {
        val st = status.toMutableMap()
        val out = mutableListOf<Msg>()
        val nowSolved = solvedSet.toMutableSet()
        for (u in unknowns) {
            if (st[u.id] == FieldState.OK) continue
            val v = parseNum(vals[u.id] ?: "")
            if (v == null) {
                st.remove(u.id)
                continue
            }
            if (closeTyped(vals[u.id] ?: "", v, answer.getValue(u.id))) {
                st[u.id] = FieldState.OK
                nowSolved += u.id
            } else st[u.id] = FieldState.NUDGE
        }
        for (u in unknowns) {
            if (st[u.id] != FieldState.NUDGE || out.size >= 2) continue
            val v = parseNum(vals[u.id] ?: "")!!
            val b = bestEq(u.id, nowSolved)
            val eq = if (b.i >= 0) eqLabel(b.i) else ""
            val ans = answer.getValue(u.id)
            if (close(-v, ans)) out += Msg(Tone.NUDGE) { tr -> tr("v_sign") }.learn(7, 4)
            else if (abs(v - ans) <= 0.05 * abs(ans)) out += Msg(Tone.NUDGE) { tr -> tr("near") }
            else if (b.others > 0) {
                val other = eqTerms[b.i].first { it.it.unknownId != null && it.it.unknownId != u.id && it.it.unknownId !in nowSolved }.it.sym
                out += Msg(Tone.NUDGE) { tr -> tr("v_dep", "u" to u.sym, "eq" to eq, "o" to other) }
            } else out += Msg(Tone.NUDGE) { tr -> tr("v_gen", "u" to u.sym, "eq" to eq) }
        }
        if (unknowns.all { st[it.id] == FieldState.OK }) {
            for (u in unknowns) if (answer.getValue(u.id) < 0) out += Msg(Tone.INFO) { tr -> tr(if (u.kind == Dof.M) "v_negM" else "v_neg", "u" to u.sym) }.learn(7, 4)
        }
        status = st
        msgs = out
    }

    /** Výraz pro neznámou z rovnice i, např. „(8,66·2 + 6·4,5) / 6“; null když obsahuje nespočítané neznámé. */
    fun expression(uid: String, i: Int): String? {
        val terms = eqTerms[i]
        val self = terms.first { it.it.unknownId == uid }
        val f = if (self.c > 0) -1 else 1
        val solved = solvedSet
        val parts = mutableListOf<String>()
        for (x in terms) {
            if (x === self) continue
            val c = x.c * f
            val text = if (x.it.unknownId != null) {
                if (x.it.unknownId !in solved) return null
                val v = answer.getValue(x.it.unknownId)
                if (x.arm != null) "${fmt(abs(x.c))}·${paren(v)}" else paren(v)
            } else {
                val v = x.it.value!!
                if (x.arm != null) "${fmt(v)}·${fmt(abs(x.c))}" else fmt(v)
            }
            parts += (if (c < 0) (if (parts.isNotEmpty()) " − " else "−") else if (parts.isNotEmpty()) " + " else "") + text
        }
        val d = abs(self.c)
        val num = if (parts.isNotEmpty()) parts.joinToString("") else "0"
        if (abs(d - 1) < 1e-9) return num
        return (if (parts.size > 1) "($num)" else num) + " / ${fmt(d)}"
    }

    fun hint() {
        session.hint()
        val target = nextTarget() ?: return
        val level = hintState?.takeIf { it.first == target.id }?.second?.plus(1) ?: 1
        hintState = target.id to level
        val solved = solvedSet
        val b = bestEq(target.id, solved)
        val coupled = unknowns.filter { it.id !in solved }.all { bestEq(it.id, solved).others > 0 }
        val value = fmt(answer.getValue(target.id))
        val unit = unitOf(target)
        val sym = target.sym
        val msg: Msg
        if (coupled && level == 1) {
            val list = unknowns.filter { it.id !in solved && it.kind != Dof.FX }.joinToString(", ") { "[[${it.sym}]]" }
            msg = Msg(Tone.HINT) { tr -> tr("v_system", "list" to list) }
        } else if (b.others > 0 && !coupled) {
            msg = Msg(Tone.HINT) { tr -> tr("v_coupled", "u" to sym) }
        } else if (coupled || level >= 3) {
            msg = Msg(Tone.HINT) { tr -> tr("v_h3", "u" to sym, "val" to value, "unit" to unit) }
            vals = vals + (target.id to value)
        } else if (level == 1) {
            val eq = eqLabel(b.i)
            msg = Msg(Tone.HINT) { tr -> tr("v_h1", "u" to sym, "eq" to eq) }
        } else {
            val expr = expression(target.id, b.i) ?: ""
            msg = Msg(Tone.HINT) { tr -> tr("v_h2", "u" to sym, "expr" to expr) }
        }
        session.highlight = "r_" + target.id
        msgs = msgs.filter { it.tone != Tone.HINT } + msg
    }

    fun done() = session.solveDone(answer)
}
