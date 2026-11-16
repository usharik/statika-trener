package cz.usharik.statika.practice

import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.learn.pick
import cz.usharik.statika.model.Rng
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.parseNum
import cz.usharik.statika.trainer.FieldState
import cz.usharik.statika.trainer.Msg
import cz.usharik.statika.trainer.Tone

/**
 * Číselné pole úlohy. [label] může obsahovat značky ([[F_x]]), [unit] je přípona (kN, m, °), [signed] = pole se znaménkem
 * (tlačítko ±; jinak se znaménko ignoruje jako v trenažéru), [digits] = na kolik míst ukázat výsledek v nápovědě,
 * [scale] = velikost pro 1 % tolerance u součtů (viz [accept]).
 */
class Field(
    val label: L,
    val answer: Double,
    val unit: String = "",
    val signed: Boolean = false,
    val tol: Tol = Tol.VALUE,
    val digits: Int = 2,
    val scale: Double? = null,
)

/**
 * Typická chyba. [wrong] = hodnoty, které ta chyba dá (null = na poli nezáleží) – podle nich se chyba pozná
 * (v toleranci pole) a zároveň slouží testům jako příklad. Složitější podmínku dá [pred] (dostane přečtené hodnoty polí).
 * [learn] = karta výkladu, která chybu vysvětluje.
 */
class Trap(
    val code: String,
    val msg: L,
    val learn: Pair<Int, Int>?,
    val wrong: List<Double?>,
    val pred: ((List<Double?>) -> Boolean)? = null,
)

/** Jedna vygenerovaná úloha procvičování. Vytvářej ji přes [exercise] (doplní 3. nápovědu a vyřadí kolidující chyby). */
class Exercise internal constructor(
    val type: String,
    val seed: Int,
    val level: Int,
    val prompt: L,
    val fig: Fig?,
    /** číselná pole; prázdné, když jde o volbu ([choice]) */
    val fields: List<Field>,
    val choice: List<Opt>?,
    /** přesně 3 stupně nápovědy: princip → vzorec s čísly → výsledek */
    val hints: List<L>,
    val traps: List<Trap>,
    /** obecná zpráva, když se chyba nepozná */
    val generic: L,
    /** vysvětlení po správné odpovědi */
    val ok: L,
    /** obrázek po vyřešení (např. se složkami); null = stejný */
    val solvedFig: Fig? = null,
)

/**
 * Sestaví úlohu. [hints] = 2 nebo 3 texty; třetí („Výsledek: …“) se doplní z odpovědí, není-li zadán.
 * Chyby, jejichž hodnoty by kontrola uznala za správné (např. prohozený sin a cos u 45°), se vyřadí.
 */
fun exercise(
    type: String,
    seed: Int,
    level: Int,
    prompt: L,
    fig: Fig?,
    fields: List<Field> = emptyList(),
    choice: List<Opt>? = null,
    hints: List<L>,
    traps: List<Trap> = emptyList(),
    generic: L = GENERIC,
    ok: L = OK,
    solvedFig: Fig? = null,
): Exercise {
    require(hints.size in 2..3) { "$type: nápovědy musí být 2 nebo 3" }
    require((choice == null) != fields.isEmpty()) { "$type: buď pole, nebo volba" }
    val h3 = hints.getOrNull(2) ?: run {
        val ans = if (choice != null) choice.first { it.correct }.label
        else L(fields.joinToString(", ") { f -> answerText(f, ru = false) }, fields.joinToString(", ") { f -> answerText(f, ru = true) })
        L("Výsledek: ${ans.cs}", "Ответ: ${ans.ru}")
    }
    val kept = traps.filterNot { t -> collides(t, fields) }
    return Exercise(type, seed, level, prompt, fig, fields, choice, hints.take(2) + h3, kept, generic, ok, solvedFig)
}

private fun answerText(f: Field, ru: Boolean): String {
    // název veličiny: značka ([[F_x]]), nebo krátký popisek, který je v obou jazycích stejný („α“, „R“, „M“)
    val sym = Regex("\\[\\[[^]]+]]").find(f.label.cs)?.let { m -> f.label.cs.substring(0, m.range.last + 1) }
        ?: f.label.cs.takeIf { it.length <= 3 && it == f.label.ru }
    val unit = if (ru) RU_UNITS[f.unit] ?: f.unit else f.unit
    val sep = if (unit == "°" || unit == "%" || unit.isEmpty()) "" else " "
    val v = fmt(f.answer, f.digits) + sep + unit
    return if (sym != null) "$sym = $v" else v
}

/** jednotky v ruském textu (obrázky zůstávají latinkou) */
private val RU_UNITS = mapOf("kN" to "кН", "kNm" to "кН·м", "m" to "м", "kN/m" to "кН/м")

/** chyba, kterou nejde odlišit od správné odpovědi (všechny její hodnoty by prošly jako správné) */
private fun collides(t: Trap, fields: List<Field>): Boolean {
    val pairs = t.wrong.mapIndexedNotNull { i, w -> if (w == null || i >= fields.size) null else fields[i] to w }
    if (pairs.isEmpty()) return false
    // obousměrně: chybná hodnota by prošla jako správná, nebo správná odpověď (zaokrouhlená) by vypadala jako ta chyba
    return pairs.all { (f, w) ->
        fieldAccepts(f, fmt(w, 3).replace('−', '-'), w) ||
            run {
                val okIn = fmt(f.answer, f.digits.coerceAtLeast(2)).replace('−', '-')
                val v = cz.usharik.statika.model.parseNum(okIn) ?: f.answer
                if (f.signed) accept(okIn, v, w, f.tol, f.scale) else accept(okIn, kotlin.math.abs(v), kotlin.math.abs(w), f.tol, f.scale)
            }
    }
}

internal fun fieldAccepts(f: Field, input: String, v: Double): Boolean {
    val value = if (f.signed) v else kotlin.math.abs(v)
    val ans = if (f.signed) f.answer else kotlin.math.abs(f.answer)
    return accept(input, value, ans, f.tol, f.scale)
}

/** Typ úlohy v rejstříku: [id] = „E03“, [title] do přehledu slabých míst, [learn] = karta výkladu k tématu. */
class ExerciseType(
    val id: String,
    val title: L,
    val learn: Pair<Int, Int>?,
    /** úrovně, které typ umí (ostatní se v sérii přeskočí) */
    val levels: IntRange = 1..3,
    val make: (rng: Rng, level: Int, seed: Int) -> Exercise,
)

/**
 * Výsledek kontroly. [trap] = kód poznané chyby (pro statistiku a testy).
 * [counted] = false u prázdného nebo nečitelného zápisu – to není chyba v počítání, nepočítá se jako pokus.
 */
class CheckResult(val states: List<FieldState?>, val msgs: List<Msg>, val solved: Boolean, val trap: String? = null, val counted: Boolean = true)

private val GENERIC = L("Ještě to nesedí. Zkus nápovědu.", "Пока не сходится. Попробуй подсказку.")
private val OK = L("Správně!", "Верно!")
private val NEAR = L(
    "Skoro! Počítej s nezaokrouhlenými čísly a výsledek zapiš na 2 desetinná místa (úhel na 1).",
    "Почти! Считай с неокруглёнными числами, а ответ запиши с 2 знаками после запятой (угол — с 1).",
)
private val EMPTY = L("Doplň všechna pole.", "Заполни все поля.")
private val NOT_NUMBER = L("Zapiš jen číslo, např. 0,6 (bez zlomku a jednotky).", "Введи только число, например 0,6 (без дроби и единиц).")

/** přečte pole: u podílů (sin, cos, tg) se smí zapsat i zlomek „3/5“ */
internal fun readField(f: Field, s: String): Double? = if (f.tol == Tol.RATIO) parseRatio(s) else parseNum(s)

private fun msg(tone: Tone, l: L, learn: Pair<Int, Int>? = null): Msg {
    val m = Msg(tone) { tr -> tr.pick(l) }
    return if (learn != null) m.learn(learn.first, learn.second) else m
}

/**
 * Kontrola odpovědi (čistá funkce). [inputs] = texty polí; u volby jediný prvek – index vybrané možnosti.
 * Pořadí: vše správně → typická chyba (první, která sedí) → „skoro“ → obecná zpráva.
 */
fun check(ex: Exercise, inputs: List<String>): CheckResult {
    val choice = ex.choice
    if (choice != null) {
        val i = inputs.firstOrNull()?.toIntOrNull() ?: return CheckResult(listOf(null), emptyList(), false, counted = false)
        val o = choice.getOrNull(i) ?: return CheckResult(listOf(null), emptyList(), false, counted = false)
        return if (o.correct) CheckResult(listOf(FieldState.OK), listOf(msg(Tone.OK, ex.ok)), true)
        else CheckResult(listOf(FieldState.NUDGE), listOf(msg(Tone.NUDGE, o.why ?: ex.generic, typeLearn(ex))), false, "OPT$i")
    }
    val values = ex.fields.mapIndexed { i, f -> readField(f, inputs.getOrElse(i) { "" }) }
    val states = ex.fields.mapIndexed { i, f ->
        val v = values[i] ?: return@mapIndexed FieldState.NUDGE
        if (fieldAccepts(f, inputs[i], v)) FieldState.OK else FieldState.NUDGE
    }
    if (states.all { it == FieldState.OK }) return CheckResult(states, listOf(msg(Tone.OK, ex.ok)), true)
    if (values.indices.any { values[it] == null && inputs.getOrElse(it) { "" }.isBlank() }) return CheckResult(states, listOf(msg(Tone.NUDGE, EMPTY)), false, counted = false)
    if (values.any { it == null }) return CheckResult(states, listOf(msg(Tone.NUDGE, NOT_NUMBER)), false, "NOTNUM", counted = false)
    for (t in ex.traps) {
        if (matches(t, ex.fields, inputs, values)) return CheckResult(states, listOf(msg(Tone.NUDGE, t.msg, t.learn)), false, t.code)
    }
    val nearAll = ex.fields.indices.all { i -> states[i] == FieldState.OK || near(if (ex.fields[i].signed) values[i]!! else kotlin.math.abs(values[i]!!), if (ex.fields[i].signed) ex.fields[i].answer else kotlin.math.abs(ex.fields[i].answer)) }
    if (nearAll) return CheckResult(states, listOf(msg(Tone.NUDGE, NEAR)), false, "NEAR")
    return CheckResult(states, listOf(msg(Tone.NUDGE, ex.generic, typeLearn(ex))), false)
}

private fun typeLearn(ex: Exercise): Pair<Int, Int>? = Exercises.byId[ex.type]?.learn

internal fun matches(t: Trap, fields: List<Field>, inputs: List<String>, values: List<Double?>): Boolean {
    t.pred?.let { return it(values) }
    var any = false
    for ((i, w) in t.wrong.withIndex()) {
        if (w == null || i >= fields.size) continue
        val v = values[i] ?: return false
        val f = fields[i]
        // chyba se pozná se stejnou tolerancí jako správná odpověď (a u polí bez znaménka bez ohledu na znaménko)
        val ok = if (f.signed) accept(inputs[i], v, w, f.tol, f.scale) else accept(inputs[i], kotlin.math.abs(v), kotlin.math.abs(w), f.tol, f.scale)
        if (!ok) return false
        any = true
    }
    return any
}

/** Správné vstupy pro úlohu (testy, nápověda 3. stupně). */
fun correctInputs(ex: Exercise): List<String> =
    ex.choice?.let { listOf(it.indexOfFirst { o -> o.correct }.toString()) }
        ?: ex.fields.map { fmt(it.answer, it.digits.coerceAtLeast(2)).replace('−', '-') }
