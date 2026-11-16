package cz.usharik.statika.learn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import cz.usharik.statika.practice.MAX_MISSES
import cz.usharik.statika.practice.starFor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.usharik.statika.model.randomSeed
import cz.usharik.statika.platform.Analytics
import cz.usharik.statika.practice.Exercise
import cz.usharik.statika.practice.PracticeEvents
import cz.usharik.statika.practice.PracticeSession
import cz.usharik.statika.practice.PracticeSets
import cz.usharik.statika.ui.CardShape
import cz.usharik.statika.ui.ChoiceTile
import cz.usharik.statika.ui.HintButton
import cz.usharik.statika.ui.LearnLink
import cz.usharik.statika.ui.LocalColors
import cz.usharik.statika.ui.Notes
import cz.usharik.statika.ui.NumField
import cz.usharik.statika.ui.PrimaryButton
import cz.usharik.statika.ui.SecondaryButton
import cz.usharik.statika.ui.rich

private val LEVELS = listOf(L("Lehká", "Лёгкая"), L("Střední", "Средняя"), L("Těžká", "Трудная"))

/** „Procvičování“ – v ruštině „Практика“ (ne „Тренировка“: plete se s trenažérem) */
private val PRACTICE = L("Procvičování", "Практика")

/** co znamenají úrovně (hlavně: kdy je potřeba kalkulačka) */
private val LEVEL_NOTE = L(
    "Lehká: kulatá čísla, kalkulačka jen někde · Střední: s kalkulačkou · Těžká: úhly od svislice, znaménka",
    "Лёгкая: круглые числа, калькулятор нужен редко · Средняя: с калькулятором · Трудная: углы от вертикали, знаки",
)

/** „Lehká ★ · Střední ☆ · Těžká ☆“ */
@Composable
private fun starsLine(progress: LearnProgress, set: String): String =
    LEVELS.mapIndexed { i, l -> l.t() + " " + if (progress.practice.star(set, i + 1)) "★" else "☆" }.joinToString(" · ")

/** rozpracovaná série (aby žák v seznamu viděl, že se může vrátit) */
private fun inProgress(progress: LearnProgress, set: String): PracticeSession? =
    progress.sessions[set]?.takeIf { !it.finished && it.tasks.isNotEmpty() && it.started }

/** Seznam sérií procvičování (na úvodní obrazovce kurzu a v procvičování). */
@Composable
internal fun PracticeSetList(course: Course, progress: LearnProgress, onNav: (LearnNav?) -> Unit, title: Boolean = true) {
    val c = LocalColors.current
    if (title) {
        val got = course.practice.sumOf { progress.practice.stars(it.id) }
        Text(
            PRACTICE.t() + " · ★ $got " + L("z", "из").t() + " ${course.practice.size * 3}",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.padding(top = 10.dp),
        )
    }
    for (set in course.practice) {
        val ready = set.available(1).isNotEmpty() || set.available(2).isNotEmpty()
        val running = inProgress(progress, set.id)
        Surface(
            onClick = { if (ready) onNav(LearnNav.Practice(set.id)) },
            enabled = ready,
            shape = CardShape, color = c.paper, border = BorderStroke(1.dp, if (running != null) c.activeBorder else c.line),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(set.title.t(), fontWeight = FontWeight.SemiBold, color = if (ready) c.ink else c.inkSoft)
                    // vždy: ke kterým lekcím série patří a kolik má úloh
                    val sub = set.lessonsLabel.t() + " · " + L("${set.size} úloh", "${set.size} задач").t() +
                        (running?.let { r -> " · " + L("Rozpracováno: úloha ${r.index + 1}/${r.tasks.size}", "В процессе: задача ${r.index + 1}/${r.tasks.size}").t() } ?: "")
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = if (running != null) c.ink else c.inkSoft)
                    Text(starsLine(progress, set.id), style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                    if (!progress.isDone(set.afterLesson) && progress.practice.best(set.id) == 0) {
                        Text(L("Doporučujeme nejdřív lekci ${set.afterLesson % 100}", "Советуем сначала урок ${set.afterLesson % 100}").t(), style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                    }
                }
                if (progress.practice.star(set.id)) Text("★", color = c.highlight, fontSize = 22.sp)
            }
        }
    }
}

/** Obrazovka procvičování: bez souboru seznam sérií, se souborem průchod sérií a výsledek. */
@Composable
internal fun PracticeScreen(nav: LearnNav.Practice, progress: LearnProgress, onNav: (LearnNav?) -> Unit, back: () -> Unit, analytics: Analytics) {
    val set = nav.set?.let { PracticeSets.byId(it) }
    if (set == null) {
        Column(Modifier.fillMaxSize()) {
            val got = Courses.trig.practice.sumOf { progress.practice.stars(it.id) }
            Header(PRACTICE.t() + " · ★ $got " + L("z", "из").t() + " ${Courses.trig.practice.size * 3}", Courses.trig.title.t(), back)
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PracticeSetList(Courses.trig, progress, onNav, title = false)
                Spacer(Modifier.height(16.dp))
            }
        }
        return
    }
    val events = remember(analytics) { practiceEvents(analytics) }
    var version by remember { mutableIntStateOf(0) }
    @Suppress("UNUSED_EXPRESSION") version
    val session = progress.sessions.getOrPut(set.id) { PracticeSession(set, progress.practice.level(set.id), randomSeed(), progress.practice, events) }
    fun restart(level: Int, seed: Int = randomSeed()) {
        progress.practice.setLevel(set.id, level)
        progress.sessions[set.id] = PracticeSession(set, level, seed, progress.practice, events)
        version++
    }
    // změna úrovně uprostřed série ji zahodí – nejdřív se zeptat
    var askLevel by remember { mutableStateOf<Int?>(null) }
    val ex = session.current
    Column(Modifier.fillMaxSize()) {
        Header(
            PRACTICE.t() + " · " + set.title.t(),
            L("Série ${session.seed}", "Серия ${session.seed}").t() + " · " + LEVELS[session.level - 1].t() +
                if (!session.finished && ex != null) " · " + L("úloha", "задача").t() + " ${session.index + 1}/${session.tasks.size}" else "",
            back,
        )
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(remember(session, session.index, session.finished) { androidx.compose.foundation.ScrollState(0) })
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                when {
                    ex == null -> Text(L("Tahle série se připravuje.", "Эта серия готовится.").t(), color = LocalColors.current.inkSoft)
                    session.finished -> PracticeResult(
                        session, progress,
                        onAgain = { restart(session.level) },
                        onHarder = { restart((session.level + 1).coerceAtMost(3)) },
                        onList = { onNav(LearnNav.Practice()) },
                    )
                    else -> {
                        Chips(LEVELS, session.level - 1) { i ->
                            if (i + 1 == session.level) return@Chips
                            if (session.started) askLevel = i + 1 else restart(i + 1)
                        }
                        Text(LEVEL_NOTE.t(), style = MaterialTheme.typography.bodySmall, color = LocalColors.current.inkSoft, modifier = Modifier.padding(top = 6.dp))
                        PracticeTask(session, ex)
                        SeriesNumber(session.seed) { n -> restart(session.level, n) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    askLevel?.let { lv ->
        AlertDialog(
            onDismissRequest = { askLevel = null },
            title = { Text(L("Začít novou sérii?", "Начать новую серию?").t()) },
            text = {
                Text(
                    L("Začít novou sérii na úrovni ${LEVELS[lv - 1].cs}? Rozdělaná série se zahodí.", "Начать новую серию на уровне «${LEVELS[lv - 1].ru}»? Текущая серия пропадёт.").t(),
                )
            },
            confirmButton = { TextButton(onClick = { askLevel = null; restart(lv) }) { Text(L("Začít novou", "Начать новую").t()) } },
            dismissButton = { TextButton(onClick = { askLevel = null }) { Text(L("Zůstat", "Остаться").t()) } },
        )
    }
}

/** číslo série: učitel zadá „série 4127“, žák ji tu spustí (stejné úlohy na Androidu i iOS) */
@Composable
private fun SeriesNumber(current: Int, onStart: (Int) -> Unit) {
    val c = LocalColors.current
    var open by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    if (!open) {
        TextButton(onClick = { open = true }, modifier = Modifier.padding(top = 14.dp)) {
            Text(L("Série $current · zadat jiné číslo série", "Серия $current · ввести другой номер серии").t(), color = c.inkSoft)
        }
        return
    }
    val n = text.trim().toIntOrNull()?.takeIf { it in 1..999_999 }
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumField(
            label = rich(L("Číslo série", "Номер серии").t()),
            value = text,
            onChange = { v -> text = v.filter(Char::isDigit).take(6) },
            onDone = { if (n != null) { open = false; onStart(n) } },
        )
        SecondaryButton(L("Spustit", "Запустить").t(), { if (n != null) { open = false; text = ""; onStart(n) } })
    }
    Text(L("Stejné číslo = stejné úlohy (třeba od učitele).", "Один номер — одни и те же задачи (например, от учителя).").t(), style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeTask(session: PracticeSession, ex: Exercise) {
    val c = LocalColors.current
    val fig = if (session.solved) ex.solvedFig ?: ex.fig else ex.fig
    if (fig != null) Box10 { Stage(h = FIG_H, description = ex.prompt.t()) { cc -> fig(fig, cc) } }
    Text(rich(ex.prompt.t()), style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.padding(top = 12.dp))
    val choice = ex.choice
    if (choice != null) {
        val picked = session.inputs.firstOrNull()?.toIntOrNull()
        FlowRow(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
            choice.forEachIndexed { i, o ->
                val correctShown = session.solved && o.correct
                ChoiceTile(picked == i || correctShown, o.correct && (picked == i || correctShown), !session.solved, onClick = { session.choose(i) }, modifier = Modifier.weight(1f)) {
                    Text(rich(o.label.t()), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, color = c.ink)
                }
            }
        }
        // hádání se nevyplatí
        Text(L("Bod je jen za první pokus.", "Балл — только с первой попытки.").t(), style = MaterialTheme.typography.bodySmall, color = c.inkSoft, modifier = Modifier.padding(top = 6.dp))
    } else {
        FlowRow(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ex.fields.forEachIndexed { i, f ->
                NumField(
                    label = rich(f.label.t()),
                    value = session.inputs.getOrElse(i) { "" },
                    onChange = { v -> session.setInput(i, v) },
                    state = session.states.getOrNull(i),
                    suffix = f.unit.ifEmpty { null }?.let { u -> L(u, RU_SUFFIX[u] ?: u).t() },
                    next = i < ex.fields.lastIndex,
                    onDone = { session.check() },
                    signed = f.signed,
                )
            }
        }
        // jak zapsat číslo (zaokrouhlení, čárka, znaménko)
        val signed = ex.fields.any { it.signed }
        Text(
            (L("Zaokrouhli na 2 desetinná místa (úhly na 1). Čárka i tečka fungují.", "Округляй до 2 знаков после запятой (углы — до 1). Можно и запятую, и точку.").t() +
                if (signed) L(" Znaménko přepneš tlačítkem ± v poli.", " Знак переключается кнопкой ± в поле.").t() else ""),
            style = MaterialTheme.typography.bodySmall, color = c.inkSoft, modifier = Modifier.padding(top = 6.dp),
        )
    }
    Notes(session.msgs)
    // FlowRow: „Ukázat výsledek (bez bodu)“ se vedle „Zkontrolovat“ na úzkém telefonu nevejde
    FlowRow(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!session.solved) {
            if (choice == null) PrimaryButton(L("Zkontrolovat", "Проверить").t(), { session.check() })
            HintButton(session.hintLevel, { session.hint() }, lastLabel = L("Ukázat výsledek (bez bodu)", "Показать ответ (без балла)").t())
        } else {
            PrimaryButton(L("Další", "Дальше").t() + " →", { session.next() })
        }
    }
}

/** jednotky polí v ruském rozhraní (obrázky zůstávají latinkou) */
private val RU_SUFFIX = mapOf("kN" to "кН", "kNm" to "кН·м", "m" to "м", "kN/m" to "кН/м")

@Composable
private fun Box10(content: @Composable () -> Unit) = Column(Modifier.fillMaxWidth().padding(top = 8.dp)) { content() }

@Composable
private fun PracticeResult(session: PracticeSession, progress: LearnProgress, onAgain: () -> Unit, onHarder: () -> Unit, onList: () -> Unit) {
    val c = LocalColors.current
    val size = session.tasks.size
    val score = session.score
    val hints = session.results.sumOf { it.hints }
    val star = starFor(score, size)
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (star) "★" else "☆", fontSize = 48.sp, color = if (star) c.highlight else c.inkSoft)
        Text(
            L("Vyřešeno $score z $size · napoprvé ${session.firstTries} · nápovědy: $hints", "Решено $score из $size · с первой попытки ${session.firstTries} · подсказок: $hints").t(),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = c.ink, textAlign = TextAlign.Center,
        )
        Text(
            (if (star) L("Zvládnuto: ${LEVELS[session.level - 1].cs} ★", "Освоено: ${LEVELS[session.level - 1].ru} ★")
            else L("Na ★ je potřeba aspoň ${(size * 3 + 3) / 4} úloh za bod.", "Для ★ нужно не меньше ${(size * 3 + 3) / 4} задач с баллом.")).t(),
            color = c.inkSoft, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            L(
                "Bod je za úlohu bez výsledku z nápovědy – u volby jen napoprvé, u čísel nejvýš po $MAX_MISSES chybách.",
                "Балл — за задачу без готового ответа из подсказки: в выборе — только с первой попытки, в числах — не больше $MAX_MISSES ошибок.",
            ).t(),
            style = MaterialTheme.typography.bodySmall, color = c.inkSoft, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
        )
        Text(starsLine(progress, session.set.id), style = MaterialTheme.typography.bodySmall, color = c.inkSoft, modifier = Modifier.padding(top = 6.dp))
    }
    val weak = session.weakTypes()
    if (weak.isNotEmpty()) {
        Text(L("Na co se ještě podívat", "Что стоит повторить").t(), fontWeight = FontWeight.SemiBold, color = c.ink, modifier = Modifier.padding(top = 18.dp))
        for (t in weak) {
            Text("• " + t.title.t(), color = c.ink, modifier = Modifier.padding(top = 6.dp))
            t.learn?.let { (l, k) -> LearnLink(l, k) }
        }
    }
    // s hvězdou dál (těžší úroveň), bez ní ještě jednou stejná úroveň
    val levelName = LEVELS[session.level - 1]
    FlowRow(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (star && session.level < 3) {
            PrimaryButton(L("Těžší úroveň", "Уровень сложнее").t() + " →", onHarder)
            SecondaryButton(L("Další série", "Ещё серия").t(), onAgain)
        } else if (star) {
            PrimaryButton(L("Další série", "Ещё серия").t(), onAgain)
        } else {
            PrimaryButton(L("Zkusit znovu (${levelName.cs})", "Ещё раз (${levelName.ru})").t(), onAgain)
        }
    }
    SecondaryButton(L("Seznam sérií", "Список серий").t(), onList, Modifier.padding(top = 8.dp))
}

private fun practiceEvents(a: Analytics) = object : PracticeEvents {
    override fun onStart(set: String, level: Int, seed: Int) = a.event("practice_start", mapOf("set" to set, "level" to level))
    override fun onTask(type: String, ok: Boolean, hints: Int, trap: String?) =
        a.event("practice_task", buildMap { put("type", type); put("ok", ok.toString()); put("hints", hints); trap?.let { put("trap", it) } })
    override fun onDone(set: String, level: Int, score: Int, size: Int) = a.event("practice_done", mapOf("set" to set, "level" to level, "score" to score, "size" to size))
}

