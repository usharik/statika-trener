package cz.usharik.statika.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.usharik.statika.i18n.LocalTr
import cz.usharik.statika.learn.LearnNav
import cz.usharik.statika.learn.Lessons
import cz.usharik.statika.learn.LocalLearn
import cz.usharik.statika.learn.LocalLearnHere
import cz.usharik.statika.learn.t
import cz.usharik.statika.trainer.FieldState
import cz.usharik.statika.trainer.Msg
import cz.usharik.statika.trainer.Tone
import kotlinx.coroutines.delay

val CardShape = RoundedCornerShape(12.dp)

@Composable
fun NoteView(msg: Msg, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val tr = LocalTr.current
    val (bg, iconBg, icon) = when (msg.tone) {
        Tone.NUDGE -> Triple(c.nudgeBg, c.nudge, "→")
        Tone.OK -> Triple(c.okBg, c.accent, "✓")
        Tone.HINT -> Triple(c.hintBg, c.react, "?")
        Tone.INFO -> Triple(c.infoBg, c.infoIcon, "i")
    }
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp).background(bg, RoundedCornerShape(9.dp)).padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.padding(top = 1.dp).size(20.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
            Text(icon, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column {
            Text(rich(msg.text(tr)), style = MaterialTheme.typography.bodyMedium, color = c.ink)
            // u typické chyby nenápadný odkaz do výkladu
            msg.learn?.let { (lesson, card) -> LearnLink(lesson, card) }
        }
    }
}

/** Odkaz „📖 Název lekce“ – otevře výklad nad úlohou (úloha zůstane rozpracovaná). */
@Composable
fun LearnLink(lesson: Int, card: Int = 0, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val opener = LocalLearn.current
    // uvnitř výkladu (procvičování, karta) se odkaz po zavření lekce vrátí tam; z trenažéru zpět k úloze
    val here = LocalLearnHere.current
    val title = Lessons.byId(lesson).title.t()
    Text(
        "📖 " + LocalTr.current(if (lesson >= 100) "learnLinkTrig" else "learnLink", "title" to title),
        color = c.react, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
        modifier = modifier.padding(top = 4.dp).clickable {
            opener.open(if (here != null) LearnNav.Open(lesson, card, returnTo = here) else LearnNav.Open(lesson, card, fromTrainer = true))
        }.padding(vertical = 6.dp),
    )
}

@Composable
fun Notes(msgs: List<Msg>) {
    Column(Modifier.revealOn(msgs)) {
        for (m in msgs) {
            // nová zpráva se jemně objeví
            val state = remember(m) { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(visibleState = state, enter = fadeIn() + expandVertically()) { NoteView(m) }
        }
    }
}

/**
 * Na telefonu je pod výkresem málo místa – když se [key] změní (nová zpráva, vyřešený úkol),
 * posune se obsah tak, aby byl prvek vidět. Při prvním zobrazení se nic neposouvá.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.revealOn(key: Any?, initial: Boolean = false): Modifier {
    val req = remember { BringIntoViewRequester() }
    var first by remember { mutableStateOf(!initial) }
    LaunchedEffect(key) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        // počkat na rozbalení zprávy; po zavření klávesnice se navíc vrátí výkres a obsah se posune – proto víckrát
        for (d in longArrayOf(120, 380, 600)) {
            delay(d)
            req.bringIntoView()
        }
    }
    return this.bringIntoViewRequester(req)
}

/** Seznam už vyřešených dílčích úkolů. */
@Composable
fun DoneLog(log: List<Msg>) {
    if (log.isEmpty()) return
    val c = LocalColors.current
    val tr = LocalTr.current
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (m in log) {
            Row {
                Text("✓ ", color = c.accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text(rich(m.text(tr)), color = c.inkSoft, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun HintButton(level: Int, onClick: () -> Unit, enabled: Boolean = true, lastLabel: String? = null) {
    val tr = LocalTr.current
    val c = LocalColors.current
    TextButton(onClick = onClick, enabled = enabled) {
        Box(Modifier.size(20.dp).background(if (enabled) c.react else c.inkSoft, CircleShape), contentAlignment = Alignment.Center) {
            Text("?", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        // [lastLabel]: poslední stupeň ukáže výsledek (v procvičování „Ukázat výsledek (bez bodu)“) – žák to musí vědět předem
        Text(if (level == 0) tr("hint") else if (level >= 2 && lastLabel != null) lastLabel else tr("moreHint"))
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(10.dp)) { Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, LocalColors.current.line)) {
        Text(text, color = LocalColors.current.ink, maxLines = 1)
    }
}

/** Číselné pole s popiskem; po kontrole zeleně/žlutě orámované. */
@Composable
fun NumField(
    label: AnnotatedString,
    value: String,
    onChange: (String) -> Unit,
    state: FieldState? = null,
    width: Dp = 120.dp,
    onDone: () -> Unit = {},
    suffix: String? = null,
    /** klávesa na klávesnici přejde na další pole (místo kontroly) */
    next: Boolean = false,
    /**
     * tlačítko „±“ u pole: číselná klávesnice iOS (a některé na Androidu) nemá minus,
     * záporný výsledek by jinak nešel napsat
     */
    signed: Boolean = false,
) {
    val c = LocalColors.current
    val focus = LocalFocusManager.current
    val signToggle = signed && state != FieldState.OK
    val border = when (state) {
        FieldState.OK -> c.accent
        FieldState.NUDGE -> c.nudge
        null -> c.line
    }
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.inkSoft, modifier = Modifier.padding(bottom = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                readOnly = state == FieldState.OK,
                modifier = Modifier.width(if (signToggle) width + 36.dp else width),
                // „±“ uvnitř pole (vedle by se pole na telefonu nevešla dvě vedle sebe)
                trailingIcon = if (signToggle) {
                    {
                        val label = LocalTr.current("signToggle")
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).clickable { onChange(toggleSign(value)) }
                                .semantics { contentDescription = label },
                            contentAlignment = Alignment.Center,
                        ) { Text("±", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.react) }
                    }
                } else null,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = if (next) ImeAction.Next else ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onNext = { focus.moveFocus(FocusDirection.Next) },
                    onDone = {
                        onDone()
                        focus.clearFocus()
                    },
                ),
                shape = RoundedCornerShape(9.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = border,
                    focusedBorderColor = if (state == null) c.react else border,
                    unfocusedContainerColor = if (state == FieldState.OK) c.okBg else Color.Transparent,
                    focusedContainerColor = if (state == FieldState.OK) c.okBg else Color.Transparent,
                ),
            )
            if (suffix != null) Text(suffix, color = c.inkSoft, modifier = Modifier.padding(start = 6.dp))
            if (state == FieldState.OK) Text(" ✓", color = c.accent, fontWeight = FontWeight.Bold)
        }
    }
}

/** Panel kroku: nadpis, úvod a obsah. */
@Composable
fun StepPanel(title: String, intro: String? = null, learn: Pair<Int, Int>? = null, content: @Composable () -> Unit) {
    val c = LocalColors.current
    Surface(shape = CardShape, color = c.paper, border = BorderStroke(1.dp, c.line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = c.ink)
            if (intro != null) Text(rich(intro), style = MaterialTheme.typography.bodyMedium, color = c.inkSoft, modifier = Modifier.padding(top = 4.dp))
            learn?.let { (l, k) -> LearnLink(l, k) }
            content()
        }
    }
}

/** Řádek tlačítek dole v kroku. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Actions(revealKey: Any? = null, content: @Composable () -> Unit) {
    // na úzkém displeji se tlačítka zalomí, místo aby se mačkala
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 14.dp).revealOn(revealKey),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/** Volba (dlaždice) – vybraná správně zeleně, vybraná špatně žlutě. */
@Composable
fun ChoiceTile(
    selected: Boolean,
    correct: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = LocalColors.current
    val border = when {
        selected && correct -> c.accent
        selected -> c.nudge
        else -> c.line
    }
    val bg = when {
        selected && correct -> c.okBg
        selected -> c.nudgeBg
        else -> c.paper
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(11.dp),
        color = bg,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, border),
        modifier = modifier.alpha(if (!enabled && !selected) 0.45f else 1f),
    ) {
        Box(Modifier.padding(horizontal = 10.dp, vertical = 12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun rememberRich(text: String) = remember(text) { rich(text) }

/** Přepne znaménko zapsaného čísla („3,5“ ↔ „−3,5“); minus pro klávesnice, které ho nemají. */
fun toggleSign(value: String): String {
    val t = value.trimStart()
    return when {
        t.startsWith("−") || t.startsWith("-") || t.startsWith("–") -> t.drop(1)
        else -> "−$t"
    }
}
