package cz.usharik.statika.learn

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.delay
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.parseNum
import cz.usharik.statika.practice.accept
import cz.usharik.statika.trainer.FieldState
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.ChoiceTile
import cz.usharik.statika.ui.LocalColors
import cz.usharik.statika.ui.NumField
import cz.usharik.statika.ui.rich
import kotlin.math.abs

/*
 * Spodní panel karty („dok“): co má žák udělat PRÁVĚ TEĎ a jen ovládání k tomu. Obrázek je přilepený nahoře,
 * výklad se posouvá uprostřed, dok je přilepený dole nad „Zpět / Dál“ – nástroje jsou vždy pod palcem.
 * Kroky karty ([Steps]) se v doku střídají po jednom; hotové kroky zůstávají ve výkladu jako ✓.
 */

/** Místo pro obsah doku na obrazovce lekce. Karta ho plní přes [Steps] / [Dock]. */
internal class DockHost {
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)
}

internal val LocalDock = staticCompositionLocalOf<DockHost?> { null }

/** true uvnitř doku – akční tlačítko je tam hlavní (plné), ve výkladu vedlejší */
internal val LocalInDock = staticCompositionLocalOf { false }

/**
 * Obsah doku. Mimo obrazovku lekce (testy, náhledy) se vykreslí na místě. Obsah se čte při každém překreslení
 * karty, takže smí číst stav karty; vlastní `remember` uvnitř drží jen do změny [key].
 */
@Composable
fun Dock(key: Any = Unit, content: @Composable () -> Unit) {
    val host = LocalDock.current
    if (host == null) {
        CompositionLocalProvider(LocalInDock provides true) { content() }
        return
    }
    val wrapped: @Composable () -> Unit = { key(key) { CompositionLocalProvider(LocalInDock provides true) { content() } } }
    SideEffect { host.content = wrapped }
    DisposableEffect(host) { onDispose { host.content = null } }
}

/**
 * Jeden krok karty: [label] = krátký rozkaz (co přesně udělat, ≤ ~70 znaků), [done] = splněno,
 * [controls] = ovládání v doku jen pro tento krok, [result] = co se po splnění ukáže ve výkladu (vysvětlení).
 */
class Step(
    val label: L,
    val done: Boolean,
    val result: (@Composable () -> Unit)? = null,
    val controls: @Composable () -> Unit = {},
)

/**
 * Kroky karty po jednom. Ve výkladu: hotové kroky ✓ (s výsledkem) a aktuální krok zvýrazněný.
 * V doku: „Krok 2 ze 3“, rozkaz a ovládání aktuálního kroku. Po splnění všech dok ukáže [after]
 * (např. ovládání k dalšímu zkoušení), jinak jen „Hotovo“. Karta jde dokončit, až jsou hotové všechny kroky.
 */
@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun CardScope.Steps(vararg steps: Step, after: (@Composable () -> Unit)? = null) {
    val c = LocalColors.current
    val list = steps.toList()
    SideEffect { report("steps", list.map { it.label to it.done }) }
    val cur = list.indexOfFirst { !it.done }
    val all = cur < 0
    // výklad: hotové kroky a jejich výsledky + aktuální krok
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        list.forEachIndexed { i, s ->
            if (!all && i > cur) return@forEachIndexed
            if (s.done) {
                StepLine(i + 1, s.label, true, current = false)
                s.result?.invoke()
            } else {
                // nový aktuální krok (a výsledek předchozího nad ním) posunout do pohledu
                val req = remember { BringIntoViewRequester() }
                LaunchedEffect(cur) {
                    if (cur > 0) {
                        delay(350)
                        req.bringIntoView()
                    }
                }
                Box(Modifier.bringIntoViewRequester(req)) { StepLine(i + 1, s.label, false, current = true) }
            }
        }
        if (!all && cur < list.lastIndex) {
            Text(
                (list.size - cur - 1).let { n ->
                    L(
                        "… a pak ještě $n ${if (n == 1) "krok" else if (n in 2..4) "kroky" else "kroků"}",
                        "… и потом ещё $n ${if (n % 10 == 1 && n % 100 != 11) "шаг" else if (n % 10 in 2..4 && n % 100 !in 12..14) "шага" else "шагов"}",
                    )
                }.t(),
                style = MaterialTheme.typography.bodySmall, color = c.inkSoft, modifier = Modifier.padding(start = 30.dp),
            )
        }
    }
    val nagScope = this
    Dock(if (all) "after" else cur) {
        Column(Modifier.fillMaxWidth().nagTarget(nagScope, !all)) {
            AnimatedContent(
                targetState = cur,
                transitionSpec = { (slideInVertically(tween(260)) { it / 3 } + fadeIn(tween(260))) togetherWith fadeOut(tween(120)) },
            ) { k ->
                Column(Modifier.fillMaxWidth()) {
                    if (k < 0) {
                        Text(
                            L("Hotovo ✓ – pokračuj tlačítkem Dál", "Готово ✓ — жми «Дальше»").t(),
                            color = c.accent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall,
                        )
                        after?.invoke()
                    } else {
                        val s = list[k]
                        if (list.size > 1) {
                            Text(
                                L("KROK ${k + 1} ZE ${list.size}", "ШАГ ${k + 1} ИЗ ${list.size}").t(),
                                color = c.react, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.8.sp,
                            )
                        }
                        Text(rich(s.label.t()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = c.ink)
                        s.controls()
                    }
                }
            }
        }
    }
}

@Composable
private fun StepLine(no: Int, label: L, done: Boolean, current: Boolean) {
    val c = LocalColors.current
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.padding(top = 1.dp).size(22.dp)
                .background(if (done) c.accent else if (current) c.react else c.paper, RoundedCornerShape(11.dp))
                .border(1.5.dp, if (done) c.accent else if (current) c.react else c.inkSoft, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (done) "✓" else "$no", color = c.paper, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            rich(label.t() + if (current) L("  ↓ dole", "  ↓ внизу").t() else ""),
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) c.inkSoft else c.ink, fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ------------------------------------------------------------------ kvíz a číselná odpověď jako kroky

/** Stav kvízu ([Quiz] na místě nebo [quizStep] v doku). Žebříček nápověd: viz [Quiz]. */
class QuizState(val question: L, options: List<Opt>, explain: L, nudge: L?) {
    // texty se obnovují při každém překreslení (živé hodnoty ve „why“), stav odpovědi zůstává
    var options = options
    var explain = explain
    var nudge = nudge
    var misses by mutableIntStateOf(0)
    var picked by mutableStateOf<Int?>(null)
    var solved by mutableStateOf(false)
    var msg by mutableStateOf<Pair<Tone, L>?>(null)
    var extra by mutableStateOf<L?>(null)
    val correct get() = options.indexOfFirst { it.correct }

    fun pick(i: Int) {
        if (solved) return
        picked = i
        val o = options[i]
        if (o.correct) {
            solved = true
            extra = null
            msg = Tone.OK to explain
            return
        }
        misses++
        // vždy nejdřív nápověda k právě zvolené chybné možnosti
        val why = o.why ?: nudge ?: L("Podívej se ještě jednou na obrázek.", "Посмотри ещё раз на рисунок.")
        when {
            misses >= 3 -> {
                solved = true
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

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun Options(columns: Int = 1) {
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = columns,
        ) {
            options.forEachIndexed { i, o ->
                val sel = picked == i || (solved && i == correct)
                ChoiceTile(sel, i == correct && sel, !solved || i == correct, onClick = { pick(i) }, modifier = Modifier.weight(1f)) {
                    Text(rich(o.label.t()), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, color = LocalColors.current.ink)
                }
            }
        }
    }

    /** zpětná vazba k poslední volbě (nápověda / vysvětlení) */
    @Composable
    fun Feedback() {
        msg?.let { (tone, text) -> Note(tone, text) }
        extra?.let { Note(Tone.HINT, it) }
    }
}

@Composable
fun rememberQuiz(question: L, options: List<Opt>, explain: L, nudge: L? = null): QuizState =
    remember(question.cs) { QuizState(question, options, explain, nudge) }.also {
        it.options = options
        it.explain = explain
        it.nudge = nudge
    }

/**
 * Kvíz jako krok: otázka a volby v doku (s nápovědou k chybné volbě), po vyřešení vysvětlení ve výkladu.
 * [label] = text kroku ve výkladu (výchozí = otázka).
 */
@Composable
fun quizStep(question: L, options: List<Opt>, explain: L, nudge: L? = null, columns: Int = 1, label: L = question): Step {
    val q = rememberQuiz(question, options, explain, nudge)
    return Step(label, q.solved, result = { q.Feedback() }) {
        if (label != question) Text(rich(question.t()), style = MaterialTheme.typography.bodyMedium, color = LocalColors.current.ink, modifier = Modifier.padding(top = 4.dp))
        q.Options(columns)
        if (!q.solved) q.Feedback()
    }
}

/** Stav číselné odpovědi ([NumAnswer] na místě nebo [numStep] v doku). */
class NumState(val fields: List<NumSpec>, explain: L, traps: List<NumTrap>, nudge: L?) {
    var explain = explain
    var traps = traps
    var nudge = nudge
    var values by mutableStateOf(List(fields.size) { "" })
    var states by mutableStateOf<List<FieldState?>>(List(fields.size) { null })
    var misses by mutableIntStateOf(0)
    var solved by mutableStateOf(false)
    var msg by mutableStateOf<Pair<Tone, L>?>(null)
    var extra by mutableStateOf<L?>(null)
    var gaveUp by mutableIntStateOf(0)

    private fun ok(i: Int, s: String): Boolean {
        val v = parseNum(s) ?: return false
        val f = fields[i]
        return if (f.signed) accept(s, v, f.answer, f.tol) else accept(s, abs(v), abs(f.answer), f.tol)
    }

    /** „Nevím“: 1. klepnutí = nápověda ([nudge]), 2. = celý postup a výsledek (úkol se tím splní) */
    fun giveUp() {
        if (solved) return
        gaveUp++
        val n = nudge
        if (gaveUp == 1 && n != null) {
            msg = Tone.HINT to n
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

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun Fields() {
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
    }

    /** Zkontrolovat + Nevím vedle sebe */
    @Composable
    fun Buttons() {
        if (solved) return
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
            cz.usharik.statika.ui.PrimaryButton(L("Zkontrolovat", "Проверить").t(), { check() })
            cz.usharik.statika.ui.SecondaryButton(
                (if (gaveUp == 0 && nudge != null) L("Nevím", "Не знаю") else L("Ukaž postup", "Покажи решение")).t(), { giveUp() },
            )
        }
    }

    @Composable
    fun Feedback() {
        msg?.let { (tone, text) -> Note(tone, text) }
        extra?.let { Note(Tone.HINT, it) }
    }
}

@Composable
fun rememberNum(key: L, fields: List<NumSpec>, explain: L, traps: List<NumTrap> = emptyList(), nudge: L? = null): NumState =
    remember(key.cs) { NumState(fields, explain, traps, nudge) }.also {
        it.explain = explain
        it.traps = traps
        it.nudge = nudge
    }

/** Číselná odpověď jako krok: otázka, pole a tlačítka v doku, po vyřešení postup ve výkladu. */
@Composable
fun numStep(question: L, fields: List<NumSpec>, explain: L, traps: List<NumTrap> = emptyList(), nudge: L? = null, label: L = question): Step {
    val n = rememberNum(question, fields, explain, traps, nudge)
    return Step(label, n.solved, result = { n.Feedback() }) {
        if (label != question) Text(rich(question.t()), style = MaterialTheme.typography.bodyMedium, color = LocalColors.current.ink, modifier = Modifier.padding(top = 4.dp))
        n.Fields()
        n.Buttons()
        if (!n.solved) n.Feedback()
    }
}

/** Tenký rámeček doku na obrazovce lekce. */
@Composable
internal fun DockFrame(host: DockHost, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val content = host.content ?: return
    Surface(color = c.focusBg, border = BorderStroke(1.dp, c.activeBorder), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), modifier = modifier.fillMaxWidth()) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) { content() }
    }
}
