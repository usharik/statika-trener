package cz.usharik.statika.practice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.usharik.statika.learn.pick
import cz.usharik.statika.model.Rng
import cz.usharik.statika.platform.KeyValueStore
import cz.usharik.statika.trainer.FieldState
import cz.usharik.statika.trainer.Msg
import cz.usharik.statika.trainer.Tone

/** Události procvičování pro analytiku. */
interface PracticeEvents {
    fun onStart(set: String, level: Int, seed: Int) {}
    fun onTask(type: String, ok: Boolean, hints: Int, trap: String?) {}
    fun onDone(set: String, level: Int, score: Int, size: Int) {}
}

object NoPracticeEvents : PracticeEvents

/**
 * Výsledek jedné úlohy série. [point] = bod: bez výsledku z nápovědy, u volby jen napoprvé, u čísel nejvýš po [MAX_MISSES] chybách.
 * [firstTry] = správně hned při první kontrole (bez výsledku z nápovědy).
 */
data class TaskResult(val type: String, val point: Boolean, val hints: Int, val misses: Int, val firstTry: Boolean = point && misses == 0)

/** kolik chybných kontrol číselné úlohy ještě nebere bod (aby se nedalo hádat donekonečna) */
const val MAX_MISSES = 2

/** kolikrát smí jeden typ úlohy přijít v jedné sérii (jinak by např. názvy stran byly nudné) */
private val MAX_PER_SERIES = mapOf("E01" to 2)

/** hvězda za sérii: aspoň 3/4 bodů */
fun starFor(score: Int, size: Int): Boolean = score * 4 >= size * 3

/** Postup v procvičování uložený v zařízení (klíče `practice.tg.*`, návrh 3.2). */
class PracticeStore(private val store: KeyValueStore, private val course: String = "tg") {
    fun level(set: String): Int = store.getInt("practice.$course.$set.level", 1).coerceIn(1, 3)
    fun setLevel(set: String, level: Int) = store.putInt("practice.$course.$set.level", level)
    fun best(set: String): Int = store.getInt("practice.$course.$set.best", 0)

    /** hvězda za úroveň; starší verze měla jednu hvězdu na soubor – ta platí jako hvězda za lehkou úroveň */
    fun star(set: String, level: Int): Boolean =
        store.getInt("practice.$course.$set.star.$level", 0) == 1 || (level == 1 && store.getInt("practice.$course.$set.star", 0) == 1)

    /** hvězda aspoň za jednu úroveň */
    fun star(set: String): Boolean = (1..3).any { star(set, it) }

    /** počet hvězd (za všechny úrovně) */
    fun stars(set: String): Int = (1..3).count { star(set, it) }
    fun runs(set: String): Int = store.getInt("practice.$course.$set.runs", 0)
    fun misses(type: String): Int = store.getInt("practice.$course.miss.$type", 0)

    internal fun addMiss(type: String) = store.putInt("practice.$course.miss.$type", misses(type) + 1)

    internal fun finish(set: String, level: Int, score: Int, size: Int) {
        if (score > best(set)) store.putInt("practice.$course.$set.best", score)
        if (starFor(score, size)) store.putInt("practice.$course.$set.star.$level", 1)
        store.putInt("practice.$course.$set.runs", runs(set) + 1)
    }
}

/**
 * Úlohy série: deterministické podle čísla série [seed], souboru a úrovně (učitel zadá „série 4127“).
 * Typy se losují podle vah; stejný typ dvakrát za sebou jen když jiný není.
 */
fun seriesFor(set: PracticeSet, level: Int, seed: Int): List<Exercise> {
    val types = set.available(level)
    if (types.isEmpty()) return emptyList()
    // vlastní hash (ne String.hashCode) – stejné číslo série musí dát stejné úlohy na Androidu i iOS
    val rng = Rng(seed * 31 + level * 7919 + set.id.fold(0) { h, ch -> h * 31 + ch.code })
    val total = types.sumOf { it.second }
    val out = mutableListOf<Exercise>()
    var last: String? = null
    val count = mutableMapOf<String, Int>()
    repeat(set.size) {
        var t: ExerciseType
        var guard = 0
        do {
            var r = rng.int(1, total)
            t = types.first { (_, w) -> r -= w; r <= 0 }.first
            guard++
            val capped = (count[t.id] ?: 0) >= (MAX_PER_SERIES[t.id] ?: Int.MAX_VALUE)
        } while ((capped || (t.id == last && types.size > 1)) && guard < 20)
        last = t.id
        count[t.id] = (count[t.id] ?: 0) + 1
        val s = rng.int(1, 999_999)
        out += t.make(Rng(s), level, s)
    }
    return out
}

/**
 * Průchod sérií: aktuální úloha, vstupy, zprávy a stupeň nápovědy (stav pro Compose, logika bez UI – testuje se v commonTest).
 * Nápověda 3. stupně doplní výsledek – úloha je pak „vyřešená“, ale bez bodu.
 */
class PracticeSession(
    val set: PracticeSet,
    val level: Int,
    val seed: Int,
    private val store: PracticeStore,
    private val events: PracticeEvents = NoPracticeEvents,
) {
    val tasks: List<Exercise> = seriesFor(set, level, seed)

    var index by mutableIntStateOf(0)
        private set
    var inputs by mutableStateOf(emptyInputs(0))
        private set
    var states by mutableStateOf<List<FieldState?>>(List(fieldCount(0)) { null })
        private set
    var msgs by mutableStateOf<List<Msg>>(emptyList())
        private set
    var hintLevel by mutableIntStateOf(0)
        private set
    var solved by mutableStateOf(false)
        private set
    var results by mutableStateOf<List<TaskResult>>(emptyList())
        private set
    var finished by mutableStateOf(false)
        private set
    private var misses = 0

    val current: Exercise? get() = tasks.getOrNull(index)
    val score: Int get() = results.count { it.point }
    val firstTries: Int get() = results.count { it.firstTry }

    /** začala už série (aby změna úrovně nezahodila práci bez potvrzení)? */
    val started: Boolean get() = index > 0 || results.isNotEmpty() || hintLevel > 0 || misses > 0 || inputs.any { it.isNotBlank() }

    init {
        if (tasks.isNotEmpty()) events.onStart(set.id, level, seed)
    }

    private fun fieldCount(i: Int) = tasks.getOrNull(i)?.let { if (it.choice != null) 1 else it.fields.size } ?: 0
    private fun emptyInputs(i: Int) = List(fieldCount(i)) { "" }

    fun setInput(i: Int, v: String) {
        if (solved) return
        inputs = inputs.toMutableList().also { it[i] = v }
        // opravené pole přestane svítit jako chybné
        if (states.getOrNull(i) == FieldState.NUDGE) states = states.toMutableList().also { it[i] = null }
    }

    /** volba u úlohy s možnostmi – rovnou se kontroluje */
    fun choose(option: Int) {
        if (solved) return
        inputs = listOf(option.toString())
        check()
    }

    fun check() {
        val ex = current ?: return
        if (solved) return
        val r = check(ex, inputs)
        states = r.states
        msgs = r.msgs
        if (r.solved) finishTask(point = if (ex.choice != null) misses == 0 else misses <= MAX_MISSES, trap = null)
        else if (r.counted) {
            misses++
            store.addMiss(ex.type)
            events.onTask(ex.type, false, hintLevel, r.trap)
        }
    }

    fun hint() {
        val ex = current ?: return
        if (solved || hintLevel >= 3) return
        hintLevel++
        val h = ex.hints[hintLevel - 1]
        val m = Msg(Tone.HINT) { tr -> tr.pick(h) }
        // po výsledku z nápovědy už staré „ještě to nesedí“ nedává smysl
        msgs = msgs.filter { it.tone != Tone.HINT && (hintLevel < 3 || it.tone != Tone.NUDGE) } + m
        if (hintLevel == 3) {
            inputs = correctInputs(ex)
            states = List(inputs.size) { FieldState.OK }
            finishTask(point = false, trap = null)
        }
    }

    private fun finishTask(point: Boolean, trap: String?) {
        val ex = current ?: return
        solved = true
        results = results + TaskResult(ex.type, point, hintLevel, misses, firstTry = point && misses == 0 && hintLevel < 3)
        events.onTask(ex.type, point, hintLevel, trap)
    }

    fun next() {
        if (!solved) return
        if (index + 1 >= tasks.size) {
            finished = true
            store.finish(set.id, level, score, tasks.size)
            events.onDone(set.id, level, score, tasks.size)
            return
        }
        index++
        inputs = emptyInputs(index)
        states = List(fieldCount(index)) { null }
        msgs = emptyList()
        hintLevel = 0
        solved = false
        misses = 0
    }

    /** typy, u kterých žák chyboval nebo potřeboval výsledek z nápovědy (nejvýš 3, nejhorší první) */
    fun weakTypes(): List<ExerciseType> = results.filter { !it.point || it.misses > 0 }
        .groupBy { it.type }.entries.sortedByDescending { (_, l) -> l.sumOf { it.misses + if (it.point) 0 else 2 } }
        .mapNotNull { Exercises.byId[it.key] }.take(3)
}
