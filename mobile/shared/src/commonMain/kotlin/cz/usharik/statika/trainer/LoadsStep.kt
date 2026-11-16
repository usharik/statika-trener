package cz.usharik.statika.trainer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.usharik.statika.model.Load
import cz.usharik.statika.model.PointForce
import cz.usharik.statika.model.PointMoment
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.Udl
import cz.usharik.statika.model.close
import cz.usharik.statika.model.closeTyped
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.forceComponents
import cz.usharik.statika.model.parseNum
import cz.usharik.statika.model.resultantSym
import kotlin.math.abs

enum class FieldState { OK, NUDGE }

/** Krok 2: náhrada spojitého zatížení výslednicí a rozklad šikmých sil. */
class LoadsStep(private val p: Problem, private val session: TrainerSession) {
    val tasks: List<Load> = p.loads.filter { it is Udl || (it is PointForce && it.alpha != null) }
    val moments: List<PointMoment> = p.loads.filterIsInstance<PointMoment>()

    var idx by mutableIntStateOf(0)
        private set
    var a by mutableStateOf("")
    var b by mutableStateOf("")
    var state by mutableStateOf<Pair<FieldState?, FieldState?>>(null to null)
        private set
    var msgs by mutableStateOf(listOf<Msg>())
        private set
    var hintLevel by mutableIntStateOf(0)
        private set
    var solved by mutableStateOf(false)
        private set
    var log by mutableStateOf(listOf<Msg>())
        private set

    init {
        // na dotykovém displeji není „najetí myší“ – zpracovávané zatížení zvýrazníme hned
        session.highlight = tasks.firstOrNull()?.id
    }

    val task: Load? get() = tasks.getOrNull(idx)

    val resultant: String get() = (task as? Udl)?.let { resultantSym(it.label) } ?: ""
    private val len: Double get() = (task as? Udl)?.let { it.x2 - it.x1 } ?: 0.0

    /** správné odpovědi: Q a poloha, nebo |Fx| a |Fy| */
    val answers: Pair<Double, Double>
        get() = when (val t = task) {
            is Udl -> t.q * len to len / 2
            is PointForce -> forceComponents(t.f, t.angle).let { abs(it.fx) to abs(it.fy) }
            else -> 0.0 to 0.0
        }

    fun done() = session.loadsDone()

    fun check() {
        if (solved) return
        val t = task ?: return
        val (ansA, ansB) = answers
        val va = parseNum(a)
        val vb = parseNum(b)
        val okA = va != null && closeTyped(a.replace("-", ""), abs(va), ansA)
        val okB = vb != null && closeTyped(b.replace("-", ""), abs(vb), ansB)
        state = (if (okA) FieldState.OK else FieldState.NUDGE) to (if (okB) FieldState.OK else FieldState.NUDGE)
        val out = mutableListOf<Msg>()
        // skoro správně – jen hrubé zaokrouhlení, ne chyba v principu
        fun nearly(v: Double?, ans: Double) = v != null && !close(abs(v), ans) && abs(abs(v) - ans) <= 0.05 * ans
        if ((!okA && nearly(va, ansA)) || (!okB && nearly(vb, ansB))) {
            msgs = listOf(Msg(Tone.NUDGE) { tr -> tr("near") })
            return
        }
        if (t is Udl) {
            val l = len
            val kA = if (va != null && close(va, t.q)) "udl_q" else "udl_Qgen"
            val kB = if (vb != null && (close(vb, l) || vb > l)) "udl_posL" else "udl_posGen"
            if (!okA) out += Msg(Tone.NUDGE) { tr -> tr(kA) }.learn(8, 1)
            if (!okB) out += Msg(Tone.NUDGE) { tr -> tr(kB) }.learn(8, 1)
        } else if (t is PointForce && (!okA || !okB)) {
            val swapped = va != null && vb != null && close(va, ansB) && close(vb, ansA)
            val full = (va != null && close(va, t.f)) || (vb != null && close(vb, t.f))
            // kalkulačka v RAD/GRAD – výsledek „sin/cos úhlu“, ale úhel brán v radiánech nebo gradech
            val wrongMode = t.alpha?.let { al ->
                val bad = calcModeMistakes(t.f, al.toDouble()).filter { m -> !close(m, ansA) && !close(m, ansB) }
                (!okA && va != null && bad.any { close(abs(va), it) }) || (!okB && vb != null && bad.any { close(abs(vb), it) })
            } ?: false
            // odkazy do trigonometrie podle typu chyby (obecná rada zůstává u statiky, lekce 3)
            val key = if (swapped) "f_swap" else if (full) "f_full" else if (wrongMode) "f_rad" else "f_gen"
            val link = when (key) {
                "f_swap" -> 102 to 1
                "f_full" -> 102 to 2
                "f_rad" -> 103 to 1
                else -> 3 to 3
            }
            out += Msg(Tone.NUDGE) { tr -> tr(key) }.learn(link.first, link.second)
        }
        if (okA && okB) {
            solved = true
            session.resolve(t.id)
            val q = resultant
            out += Msg(Tone.OK) { tr -> if (t is Udl) tr("udl_ok", "Q" to q, "Qv" to fmt(ansA)) else tr("f_ok") }
        }
        msgs = out
    }

    fun hint() {
        val t = task ?: return
        session.hint()
        val lvl = hintLevel + 1
        hintLevel = lvl
        val (ansA, ansB) = answers
        val q = resultant
        val l = len
        val msg = Msg(Tone.HINT) { tr ->
            if (t is Udl) {
                tr(
                    if (lvl == 1) "udl_h1" else if (lvl == 2) "udl_h2" else "udl_h3",
                    "Q" to q, "q" to fmt(t.q), "l" to fmt(l), "Qv" to fmt(ansA), "h" to fmt(ansB),
                )
            } else {
                val f = t as PointForce
                tr(
                    if (lvl == 1) "f_h1" else if (lvl == 2) "f_h2" else "f_h3",
                    "F" to f.label, "v" to fmt(f.f), "a" to (f.alpha ?: 0), "x" to fmt(ansA), "y" to fmt(ansB),
                )
            }
        }
        // první nápověda vede k výkladu, odkud se sin a cos berou
        msgs = msgs.filter { it.tone != Tone.HINT } + (if (lvl == 1 && t is PointForce) msg.learn(102, 0) else msg)
    }

    fun advance() {
        msgs.firstOrNull { it.tone == Tone.OK }?.let { log = log + it }
        idx += 1
        a = ""
        b = ""
        state = null to null
        msgs = emptyList()
        hintLevel = 0
        solved = false
        session.highlight = tasks.getOrNull(idx)?.id
    }
}

/**
 * Hodnoty, které dá kalkulačka ve špatném režimu: |F·cos α| a |F·sin α| s α braným v radiánech (RAD)
 * nebo v gradech (GRAD, 1 grad = 0,9°). Slouží k rozpoznání chyby „kalkulačka není v DEG“.
 */
internal fun calcModeMistakes(f: Double, alphaDeg: Double): List<Double> {
    val rad = alphaDeg
    val grad = alphaDeg * 0.9 * kotlin.math.PI / 180
    return listOf(
        abs(f * kotlin.math.cos(rad)), abs(f * kotlin.math.sin(rad)),
        abs(f * kotlin.math.cos(grad)), abs(f * kotlin.math.sin(grad)),
    )
}

/** jen pro testy */
internal fun calcModeMistakesForTest(f: Double, alphaDeg: Double) = calcModeMistakes(f, alphaDeg)
