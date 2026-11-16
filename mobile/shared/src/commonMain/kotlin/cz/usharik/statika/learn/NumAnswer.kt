package cz.usharik.statika.learn

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.parseNum
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.accept
import cz.usharik.statika.trainer.FieldState
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.LocalColors
import cz.usharik.statika.ui.NumField
import cz.usharik.statika.ui.rich
import kotlin.math.abs

/**
 * Číselné pole karty. [label] může mít značky ([[F_x]]), [unit] = přípona, [signed] = se znaménkem (tlačítko ±;
 * jinak se znaménko ignoruje), [tol] = jak přísně kontrolovat (viz [Tol]).
 */
class NumSpec(val label: L, val answer: Double, val unit: String = "", val signed: Boolean = false, val tol: Tol = Tol.VALUE)

/** Typická chyba v kartě: [match] dostane přečtené hodnoty polí (null = prázdné), [why] = cílená nápověda. */
class NumTrap(val why: L, val match: (List<Double?>) -> Boolean) {
    companion object {
        /** chyba poznaná podle hodnot, které dává (±1 % / 0,02; u polí bez znaménka bez ohledu na znaménko) */
        fun near(why: L, vararg wrong: Double?): NumTrap = NumTrap(why) { v ->
            wrong.withIndex().all { (i, w) -> w == null || (v.getOrNull(i)?.let { x -> abs(abs(x) - abs(w)) <= maxOf(0.02, abs(w) * 0.01) } ?: false) }
        }
    }
}

private val NUM_TASK = L("Spočítej a zapiš výsledek", "Посчитай и запиши результат")

/**
 * Úloha s číselnou odpovědí v kartě výkladu. Stejný žebříček jako [Quiz]: 1. chyba → cílená nápověda (první [NumTrap],
 * který sedí, jinak [nudge]); 2. chyba → navíc [nudge]; 3. chyba → doplní správné hodnoty a ukáže [explain].
 * Úkol „Spočítej a zapiš výsledek“ se hlásí kartě – dokud není hotový, „Dál“ je zamčené.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardScope.NumAnswer(question: L, fields: List<NumSpec>, explain: L, traps: List<NumTrap> = emptyList(), nudge: L? = null) {
    val c = LocalColors.current
    var values by remember { mutableStateOf(List(fields.size) { "" }) }
    var states by remember { mutableStateOf<List<FieldState?>>(List(fields.size) { null }) }
    var misses by remember { mutableIntStateOf(0) }
    var solved by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<Pair<Tone, L>?>(null) }
    var extra by remember { mutableStateOf<L?>(null) }
    var gaveUp by remember { mutableIntStateOf(0) }
    val key = "num:" + question.cs
    SideEffect { report(key, listOf(NUM_TASK to solved)) }

    fun ok(i: Int, s: String): Boolean {
        val v = parseNum(s) ?: return false
        val f = fields[i]
        return if (f.signed) accept(s, v, f.answer, f.tol) else accept(s, abs(v), abs(f.answer), f.tol)
    }

    /** „Nevím“: 1. klepnutí = nápověda ([nudge]), 2. = celý postup a výsledek (úkol se tím splní) */
    fun giveUp() {
        if (solved) return
        gaveUp++
        if (gaveUp == 1 && nudge != null) {
            msg = Tone.HINT to nudge
            extra = null
            return
        }
        solved = true
        values = fields.map { fmt(it.answer, 2) }
        states = List(fields.size) { FieldState.OK }
        msg = Tone.INFO to explain
        extra = null
    }

    fun check() {
        if (solved) return
        val st = values.mapIndexed { i, s -> if (ok(i, s)) FieldState.OK else FieldState.NUDGE }
        states = st
        if (st.all { it == FieldState.OK }) {
            solved = true
            msg = Tone.OK to explain
            extra = null
            return
        }
        if (values.any { parseNum(it) == null }) {
            msg = Tone.NUDGE to L("Doplň všechna pole.", "Заполни все поля.")
            return
        }
        misses++
        val parsed = values.map { parseNum(it) }
        val why = traps.firstOrNull { it.match(parsed) }?.why ?: nudge ?: L("Ještě to nesedí – podívej se znovu na obrázek.", "Пока не сходится — посмотри ещё раз на рисунок.")
        when {
            misses >= 3 -> {
                solved = true
                values = fields.map { fmt(it.answer, 2) }
                states = List(fields.size) { FieldState.OK }
                msg = Tone.NUDGE to why
                extra = explain
            }
            misses == 2 && nudge != null && nudge != why -> {
                msg = Tone.NUDGE to why
                extra = nudge
            }
            else -> {
                msg = Tone.NUDGE to why
                extra = null
            }
        }
    }

    Text(
        rich(question.t()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = c.ink,
        modifier = Modifier.padding(top = 14.dp).nagTarget(this, !solved),
    )
    FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        fields.forEachIndexed { i, f ->
            NumField(
                label = rich(f.label.t()),
                value = values[i],
                onChange = { v ->
                    if (!solved) {
                        values = values.toMutableList().also { it[i] = v }
                        if (states[i] == FieldState.NUDGE) states = states.toMutableList().also { it[i] = null }
                    }
                },
                state = states[i],
                suffix = f.unit.ifEmpty { null },
                next = i < fields.lastIndex,
                onDone = ::check,
                signed = f.signed,
            )
        }
    }
    if (!solved) {
        ActionButton(L("Zkontrolovat", "Проверить")) { check() }
        ActionButton(if (gaveUp == 0 && nudge != null) L("Nevím – ukaž postup", "Не знаю — покажи решение") else L("Ukaž celý postup", "Покажи всё решение")) { giveUp() }
    }
    msg?.let { (tone, text) -> Note(tone, text) }
    extra?.let { Note(Tone.HINT, it) }
}

/**
 * Odkaz uvnitř výkladu na jinou kartu (i do jiného kurzu): „📖 …“. Po zavření cílové lekce se žák vrátí sem
 * (na tuto kartu nebo do procvičování) – viz [LearnNav.Open.returnTo].
 */
@Composable
fun LearnJump(lesson: Int, card: Int = 0, label: L? = null) {
    val c = LocalColors.current
    val opener = LocalLearn.current
    val here = LocalLearnHere.current
    val target = Lessons.byId(lesson)
    val course = Lessons.courseOf(lesson)
    val text = label?.t() ?: (course.title.t() + ": " + target.title.t())
    Text(
        "📖 $text",
        color = c.react, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 6.dp).clickable { opener.open(LearnNav.Open(lesson, card, returnTo = here)) }.padding(vertical = 6.dp),
    )
}

