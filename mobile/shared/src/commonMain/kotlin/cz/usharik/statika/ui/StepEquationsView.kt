package cz.usharik.statika.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.usharik.statika.i18n.LocalTr
import cz.usharik.statika.model.EqSpec
import cz.usharik.statika.model.SymDef
import cz.usharik.statika.model.Tok
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.isFunctionName
import cz.usharik.statika.model.lookupSym
import cz.usharik.statika.model.tokenize
import cz.usharik.statika.trainer.EquationsStep
import cz.usharik.statika.trainer.eqTitle

private val InputStyle @Composable get() = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepEquationsView(st: EquationsStep, sysKbd: Boolean) {
    val tr = LocalTr.current
    val c = LocalColors.current
    // odkaz podle rovnice, do které se právě píše
    val learn = when (st.rows.getOrNull(st.active)?.spec) {
        EqSpec.Fx, EqSpec.Fy -> 2 to 3
        is EqSpec.M -> 5 to 2
        is EqSpec.Hinge -> 9 to 1
        null -> 4 to 0
    }
    StepPanel(tr("s3Title"), tr("s3Intro"), learn) {
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            st.rows.forEachIndexed { i, r ->
                val isActive = i == st.active
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = if (r.done) c.okBg.copy(alpha = 0.5f) else c.paper,
                    border = BorderStroke(if (isActive) 1.5.dp else 1.dp, if (isActive) c.activeBorder else c.line),
                    // řádek, do kterého se píše, nesmí zůstat schovaný pod výkresem a paletou
                    modifier = Modifier.fillMaxWidth().then(if (isActive) Modifier.revealOn(st.active to r.msgs, initial = true) else Modifier),
                    onClick = { st.focusRow(i) },
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        FlowRow(verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                buildAnnotatedString {
                                    if (r.done) withStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.Bold)) { append("✓ ") }
                                    appendRich(eqTitle(r.spec))
                                    if (r.spec is EqSpec.Hinge && r.done) {
                                        withStyle(SpanStyle(color = c.inkSoft, fontSize = 13.sp)) { append(" · " + tr("p_sideFound", "side" to tr(r.spec.side.key))) }
                                    }
                                },
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                            if (r.spec is EqSpec.M) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tr("momentPoint"), style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                                    Spacer(Modifier.width(6.dp))
                                    for (q in st.points) {
                                        val on = r.spec.point == q.label
                                        Box(
                                            Modifier.padding(end = 4.dp)
                                                .defaultMinSize(minWidth = 44.dp, minHeight = 40.dp)
                                                .background(if (on) c.ink else c.subtle, RoundedCornerShape(8.dp))
                                                .clickable { st.setPoint(i, q.label, q.x) }
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center,
                                        ) { Text(q.label, color = if (on) c.paper else c.ink, fontWeight = FontWeight.SemiBold) }
                                    }
                                }
                            }
                        }
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            EquationInput(
                                value = r.value,
                                onChange = { st.onValueChange(i, it) },
                                active = isActive,
                                done = r.done,
                                sysKbd = sysKbd,
                                placeholder = tr("eqPlaceholder"),
                                onFocus = { st.focusRow(i) },
                                onEnter = { st.check(i) },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            SecondaryButton(tr("checkEq"), { st.check(i) })
                        }
                        Text(eqRender(r.text, st.defs, c), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp))
                        Notes(r.msgs)
                    }
                }
            }
        }
        Actions(st.allDone) {
            if (!st.allDone) {
                HintButton(st.rows.getOrNull(st.active)?.hintLevel ?: 0, st::hint)
                SecondaryButton(tr("checkAll"), st::checkAll)
            } else PrimaryButton(tr("toSolve") + " →", st::done)
        }
    }
}

/**
 * Pole pro rovnici. Bez systémové klávesnice se píše paletou: klepnutím se jen umístí kurzor,
 * takže výkres a paleta zůstanou na displeji vidět.
 */
@Composable
private fun EquationInput(
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    active: Boolean,
    done: Boolean,
    sysKbd: Boolean,
    placeholder: String,
    onFocus: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier,
) {
    val c = LocalColors.current
    val border = if (done) c.accent else if (active) c.react else c.line
    val box = modifier.heightIn(min = 46.dp)
        .border(if (active) 1.5.dp else 1.dp, border, RoundedCornerShape(9.dp))
        .background(if (done) c.okBg else c.paper, RoundedCornerShape(9.dp))
        .padding(horizontal = 12.dp, vertical = 11.dp)
    val style = InputStyle.copy(color = c.ink)
    if (sysKbd) {
        val focus = remember { FocusRequester() }
        LaunchedEffect(active) { if (active) runCatching { focus.requestFocus() } }
        BasicTextField(
            value = value,
            // Enter = ověřit, Tab nesmí vložit znak
            onValueChange = { v -> onChange(v.copy(text = v.text.replace("\n", "").replace("\t", ""))) },
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(c.react),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onEnter() }),
            modifier = box.focusRequester(focus).onFocusChanged { if (it.isFocused) onFocus() },
            decorationBox = { inner ->
                Box {
                    if (value.text.isEmpty()) Text(placeholder, style = style.copy(color = c.inkSoft))
                    inner()
                }
            },
        )
        return
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    // blikající kurzor jen v aktivním poli
    val blink = if (active) rememberInfiniteTransition().animateFloat(1f, 0f, infiniteRepeatable(tween(530), RepeatMode.Reverse)).value else 0f
    val empty = value.text.isEmpty()
    Box(box) {
        Text(
            if (empty) AnnotatedString(placeholder) else AnnotatedString(value.text),
            style = if (empty) style.copy(color = c.inkSoft) else style,
            onTextLayout = { layout = it },
            modifier = Modifier.fillMaxWidth()
                .pointerInput(value.text) {
                    detectTapGestures { pos ->
                        val l = layout
                        val o = if (empty || l == null) 0 else l.getOffsetForPosition(pos)
                        onFocus()
                        onChange(value.copy(selection = TextRange(o)))
                    }
                }
                .drawWithContent {
                    drawContent()
                    val l = layout ?: return@drawWithContent
                    if (!active || blink < 0.5f) return@drawWithContent
                    val pos = if (empty) 0 else value.selection.end.coerceIn(0, value.text.length)
                    val r = l.getCursorRect(pos)
                    val x = if (empty) 0f else r.left
                    drawLine(c.react, Offset(x, r.top), Offset(x, r.bottom), 2.dp.toPx())
                },
        )
    }
}

/** Živé vykreslení zápisu: značky s indexy, · místo *, neznámé značky zvýrazněné. */
fun eqRender(text: String, defs: List<SymDef>, c: StatikaColors): AnnotatedString = buildAnnotatedString {
    if (text.isBlank()) {
        withStyle(SpanStyle(color = c.inkSoft)) { append("… = 0") }
        return@buildAnnotatedString
    }
    val (toks, bad) = tokenize(text)
    val hasEq = toks.any { it is Tok.Op && it.v == '=' }
    val warn = SpanStyle(background = c.warnBg, color = c.ink)
    for (tk in toks) {
        when (tk) {
            is Tok.Num -> append(fmt(tk.v, 4))
            is Tok.Id -> {
                if (isFunctionName(tk.v)) append(tk.v.lowercase())
                else {
                    val d = lookupSym(defs, tk.v) ?: defs.firstOrNull { it.name.replace("_", "").lowercase() == tk.v.replace("_", "").lowercase() }
                    if (d != null) appendSym(d.name, if (d.isUnknown) c.react else c.load)
                    else withStyle(warn) { append(tk.v) }
                }
            }
            is Tok.Op -> {
                val v = when (tk.v) {
                    '*' -> '·'
                    '-' -> '−'
                    else -> tk.v
                }
                // znaménko na začátku bez mezery před ním
                append(if (v == '+' || v == '−' || v == '=') (if (length == 0) "$v " else " $v ") else v.toString())
            }
        }
    }
    if (bad != null) withStyle(warn) { append(text.substring(bad)) }
    if (!hasEq) append(" = 0")
}

/** Paleta značek a znaků – na telefonu přilepená dole jako klávesnice. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Palette(st: EquationsStep, sysKbd: Boolean, onToggleKbd: () -> Unit, modifier: Modifier = Modifier) {
    val tr = LocalTr.current
    val c = LocalColors.current
    val row = st.rows[st.active]
    Surface(color = c.paper, shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp), border = BorderStroke(1.dp, c.line), modifier = modifier) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = c.inkSoft)) { append(tr("writingTo") + " ") }
                        appendRich(eqTitle(row.spec))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                Key("?", Modifier.width(42.dp), color = c.react) { st.hint() }
                Spacer(Modifier.width(5.dp))
                Key(tr("checkEq"), Modifier.width(84.dp), small = true, on = true) { st.check(st.active) }
                Spacer(Modifier.width(5.dp))
                Key("✕", Modifier.width(42.dp)) { st.clear() }
                Spacer(Modifier.width(5.dp))
                Key("⌨", Modifier.width(44.dp), on = sysKbd, onClick = onToggleKbd)
            }
            // značky v jednom posuvném řádku – u úloh s mnoha silami by jinak paleta zakryla rovnici
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (d in st.unknownDefs) SymChip(d, c.react, c.chipReactBorder) { st.insert(d.name) }
                for (d in st.knownDefs) SymChip(d, c.load, c.chipLoadBorder) { st.insert(d.name) }
            }
            // se systémovou klávesnicí stačí značky – čísla a znaménka má klávesnice (jinak by na výšku nezbylo místo)
            if (sysKbd) return@Column
            val keys = listOf(
                listOf("7" to "7", "8" to "8", "9" to "9", " + " to "+", " - " to "−", "(" to "(", ")" to ")"),
                listOf("4" to "4", "5" to "5", "6" to "6", "*" to "·", "/" to "/", " = " to "=", "⌫" to "⌫"),
                listOf("1" to "1", "2" to "2", "3" to "3", "0" to "0", "," to ",", "sin(" to "sin", "cos(" to "cos"),
            )
            for (r in keys) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for ((ins, label) in r) {
                        val digit = label.length == 1 && (label[0].isDigit() || label == ",")
                        Key(label, Modifier.weight(1f), small = label.length > 1, digit = digit) {
                            if (ins == "⌫") st.backspace() else st.insert(ins)
                        }
                    }
                }
            }
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = c.inkSoft)) { append(tr("exampleLabel") + ": ") }
                    withStyle(SpanStyle(background = c.subtle)) { append(st.example) }
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun SymChip(d: SymDef, color: androidx.compose.ui.graphics.Color, border: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val c = LocalColors.current
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = c.paper, border = BorderStroke(1.dp, border)) {
        Text(
            buildAnnotatedString {
                appendSym(d.name, color)
                if (d.value != null) withStyle(SpanStyle(color = c.inkSoft, fontSize = 11.sp)) { append("  ${fmt(d.value)} ${d.unit}") }
            },
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun Key(label: String, modifier: Modifier, small: Boolean = false, digit: Boolean = false, on: Boolean = false, color: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    val c = LocalColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (on) c.ink else if (digit) c.paper else c.subtle,
        border = BorderStroke(1.dp, c.line),
        modifier = modifier.height(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (on) c.paper else color ?: c.ink, fontWeight = FontWeight.SemiBold, fontSize = if (small) 14.sp else 18.sp)
        }
    }
}
