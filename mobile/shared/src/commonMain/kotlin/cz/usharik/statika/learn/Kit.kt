package cz.usharik.statika.learn

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.zIndex
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.usharik.statika.i18n.LocalTr
import cz.usharik.statika.model.Support
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.trainer.Msg
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.Anim
import cz.usharik.statika.ui.CardShape
import cz.usharik.statika.ui.ChoiceTile
import cz.usharik.statika.ui.LocalColors
import cz.usharik.statika.ui.NoteView
import cz.usharik.statika.ui.Prim
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import cz.usharik.statika.ui.drawPrim
import cz.usharik.statika.ui.rich
import cz.usharik.statika.ui.supportSymbol
import kotlin.math.hypot

/*
 * Stavebnice karet výkladu. Obrázky se kreslí stejnými primitivy jako výkres v trenažéru
 * (PrimBuilder: šipky, oblouky momentů, podpory, šrafy, popisky), takže žák vidí od začátku stejný jazyk:
 * zatížení červeně, reakce modře, složky a výslednice fialově, „v pořádku“ zeleně.
 */

/** Bod ve souřadnicích scény (jednotky obrázku, osa y dolů). */
data class Pt(val x: Double, val y: Double) {
    fun dist(x2: Double, y2: Double) = hypot(x - x2, y - y2)
}

/**
 * Interaktivní obrázek. Souřadnice: šířka [w] jednotek, výška [h]; obsah se přepočítá při každé změně stavu,
 * který [draw] čte. [onDrag] dostává polohu prstu (start = true na začátku tahu), [onTap] klepnutí.
 * Tah se zachytí hned na obrázku, takže nekoliduje s posouváním karty.
 */
@Composable
internal fun Stage(
    w: Double = 400.0,
    h: Double = 220.0,
    description: String = "",
    modifier: Modifier = Modifier,
    onDrag: ((Pt, Boolean) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onTap: ((Pt) -> Unit)? = null,
    draw: PrimBuilder.(StatikaColors) -> Unit,
) {
    val c = LocalColors.current
    val prims = PrimBuilder(w).apply { draw(c) }.prims
    val host = LocalSticky.current
    // poloha obrázku v kořeni bez vlastního posunu (měří se vně graphicsLayer)
    var selfTop by remember { mutableFloatStateOf(0f) }
    var selfH by remember { mutableFloatStateOf(0f) }
    val measurer = rememberTextMeasurer(cacheSize = 32)
    var size by remember { mutableStateOf(1f) }
    fun toPt(o: Offset): Pt {
        val k = size / w
        return Pt(o.x / k, o.y / k)
    }
    var m = modifier.fillMaxWidth().aspectRatio((w / h).toFloat())
        .onSizeChanged { size = it.width.toFloat() }
        .semantics { contentDescription = rich(description).text } // bez značek [[F_x]] a **
    // gesta běží dál než jedno vykreslení – vždy volat aktuální obsluhu (jinak by ručka po prvním tahu
    // „zůstala“ na původním místě)
    val drag by rememberUpdatedState(onDrag)
    val dragEnd by rememberUpdatedState(onDragEnd)
    val tap by rememberUpdatedState(onTap)
    if (onDrag != null) {
        m = m.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { drag?.invoke(toPt(it), true) },
                onDragEnd = { dragEnd?.invoke() },
                onDragCancel = { dragEnd?.invoke() },
            ) { change, _ ->
                change.consume()
                drag?.invoke(toPt(change.position), false)
            }
        }
    }
    if (onTap != null) m = m.pointerInput(Unit) { detectTapGestures { tap?.invoke(toPt(it)) } }
    // obrázek se při posouvání karty „přilepí“ k hornímu okraji, aby úkol zůstal na očích;
    // jen když zabírá nejvýš ~55 % výšky (na šířku / malém displeji by jinak zakryl celý text)
    val stickMod = if (host == null) Modifier else Modifier
        .onGloballyPositioned { selfTop = it.positionInRoot().y; selfH = it.size.height.toFloat() }
        .zIndex(1f)
        .graphicsLayer {
            val shift = host.top - selfTop
            translationY = if (shift > 0f && selfH < host.height * 0.55f) shift else 0f
        }
        .drawBehind {
            val shift = host.top - selfTop
            if (shift > 0f && selfH < host.height * 0.55f) {
                // podklad, aby text pod obrázkem neprosvítal kolem zaoblených rohů a okrajů
                val pad = 16.dp.toPx()
                drawRect(c.bg, Offset(-pad, -pad), androidx.compose.ui.geometry.Size(this.size.width + 2 * pad, this.size.height + pad + 6.dp.toPx()))
                drawRect(c.line, Offset(-pad, this.size.height + 6.dp.toPx()), androidx.compose.ui.geometry.Size(this.size.width + 2 * pad, 1.dp.toPx()))
            }
        }
    Surface(modifier = stickMod, shape = CardShape, color = c.paper, border = BorderStroke(1.dp, c.line)) {
        Canvas(m) {
            val k = this.size.width / w.toFloat()
            withTransform({ scale(k, k, Offset.Zero) }) {
                for (p in prims) drawPrim(p, 1f, c, measurer, k)
            }
        }
    }
}

/** Posuvná oblast karty (horní okraj a výška v kořeni), ke které se [Stage] přilepí. */
internal class StickyHost {
    var top by mutableFloatStateOf(0f)
    var height by mutableFloatStateOf(0f)
}

internal val LocalSticky = staticCompositionLocalOf<StickyHost?> { null }

// ------------------------------------------------------------------ kreslení

internal fun PrimBuilder.line(x1: Double, y1: Double, x2: Double, y2: Double, color: Color, width: Double = 1.5, dash: Boolean = false, alpha: Float = 1f) {
    prims += Prim.Line(x1, y1, x2, y2, color, width, if (dash) 4.0 to 4.0 else null, alpha = alpha)
}

internal fun PrimBuilder.circle(cx: Double, cy: Double, r: Double, fill: Color?, stroke: Color? = null, width: Double = 1.5, alpha: Float = 1f) {
    prims += Prim.Circle(cx, cy, r, fill, stroke, width, alpha = alpha)
}

internal fun PrimBuilder.rect(x: Double, y: Double, w: Double, h: Double, fill: Color?, stroke: Color? = null, width: Double = 1.5, alpha: Float = 1f) {
    prims += Prim.Rect(x, y, w, h, fill, stroke, width, alpha = alpha)
}

internal fun PrimBuilder.poly(pts: List<Pair<Double, Double>>, fill: Color?, stroke: Color? = null, width: Double = 1.5, alpha: Float = 1f) {
    prims += Prim.Poly(pts, fill, stroke, width, alpha = alpha)
}

/** Text; [sym] se vysází jako značka s indexem (F_x), [text] obyčejně za ní. */
internal fun PrimBuilder.text(x: Double, y: Double, text: String?, color: Color, size: Double = 15.0, anchor: Anchor = Anchor.MIDDLE, sym: String? = null, bold: Boolean = false, alpha: Float = 1f) {
    // popisek nesmí vyjet z obrázku: odhad šířky jako ve výkresu trenažéru a posun dovnitř
    val wd = ((sym?.replace("_", "")?.length ?: 0) + (text?.length ?: 0)) * size * 0.58 + 4
    val left = when (anchor) {
        Anchor.START -> x
        Anchor.END -> x - wd
        Anchor.MIDDLE -> x - wd / 2
    }
    val dx = when {
        left < 4 -> 4 - left
        left + wd > w - 4 -> (w - 4) - (left + wd)
        else -> 0.0
    }
    prims += Prim.Label(x + dx, y, sym, text, anchor, color, size, bold, alpha = alpha)
}

/** Šipka síly od (x1, y1) k hrotu (x2, y2). */
internal fun PrimBuilder.force(x1: Double, y1: Double, x2: Double, y2: Double, color: Color, width: Double = 2.4, head: Double = 11.0, glow: Boolean = false, alpha: Float = 1f, dash: Boolean = false) {
    arrow(x1, y1, x2, y2, color, width, if (dash) 5.0 to 4.0 else null, head, Anim.None, glow, alpha)
}

/** Oblouk momentu kolem (cx, cy); ccw = ↺. */
internal fun PrimBuilder.moment(cx: Double, cy: Double, r: Double, ccw: Boolean, color: Color, width: Double = 2.2, glow: Boolean = false) {
    momentArc(cx, cy, r, ccw, color, width, Anim.None, glow)
}

/** Vodorovný nosník (obdélník) mezi x1 a x2 s osou v y. */
internal fun PrimBuilder.beam(x1: Double, x2: Double, y: Double, c: StatikaColors, alpha: Float = 1f) {
    rect(x1, y - 5, x2 - x1, 10.0, c.beam, c.ink, 1.6, alpha)
}

/** Značka podpory stejná jako v trenažéru; [wallLeft] = zeď vetknutí vlevo. */
internal fun PrimBuilder.support(type: SupportType, x: Double, y: Double, c: StatikaColors, label: String = "", ghost: Boolean = false, wallLeft: Boolean = true) {
    // supportSymbol rozhoduje o straně zdi podle polohy na nosníku délky len
    val len = 10.0
    val sx = if (wallLeft) 0.0 else len
    supportSymbol(this, Support("s", label, sx, type), x, len, ghost, Anim.None, c, y)
}

/** Místo, na které se dá klepnout (ne táhnout) – čárkovaný kroužek, ať se neplete s ručkou. */
internal fun PrimBuilder.tapSpot(x: Double, y: Double, c: StatikaColors, selected: Boolean = false, r: Double = 13.0) {
    prims += Prim.Circle(x, y, r, if (selected) c.react.copy(alpha = 0.18f) else c.react.copy(alpha = 0.06f), c.react, 1.6, dash = 4.0 to 3.0)
}

/** Ručka pro tah prstem – výrazný kroužek, aby bylo jasné, že se s ním dá hýbat. */
internal fun PrimBuilder.handle(x: Double, y: Double, c: StatikaColors, active: Boolean = false) {
    circle(x, y, if (active) 13.0 else 11.0, c.highlight.copy(alpha = 0.25f), c.highlight, 2.0)
    circle(x, y, 4.0, c.ink)
}

// ------------------------------------------------------------------ text a ovládání

/** Hlavní text karty – krátký, se značkami [[F_x]] a **tučně**. */
@Composable
fun CardText(text: L, modifier: Modifier = Modifier) {
    Text(rich(text.t()), style = MaterialTheme.typography.bodyLarge, color = LocalColors.current.ink, modifier = modifier.padding(top = 12.dp))
}

/** Menší doplňující text (vzorec, „kde obrázek přestává platit“). */
@Composable
fun SmallText(text: L, modifier: Modifier = Modifier) {
    Text(rich(text.t()), style = MaterialTheme.typography.bodySmall, color = LocalColors.current.inkSoft, modifier = modifier.padding(top = 6.dp))
}

/** Velký živý údaj pod obrázkem, např. „M = 20 · 0,4 = 8 kNm“. */
@Composable
fun Readout(text: String, ok: Boolean = false, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    Text(
        rich(text), fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
        color = if (ok) c.accent else c.ink, textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(top = 10.dp),
    )
}

/** Zpráva žákovi (stejný vzhled jako v trenažéru). */
@Composable
fun Note(tone: Tone, text: L) {
    val msg = remember(text, tone) { Msg(tone) { tr -> tr.pick(text) } }
    NoteView(msg)
}

/** Řada tlačítek-voleb (segmenty / akce). Velké terče, žádné drobné tahy. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Chips(options: List<L>, selected: Int? = null, modifier: Modifier = Modifier, selectedSet: Set<Int> = emptySet(), onPick: (Int) -> Unit) {
    val c = LocalColors.current
    FlowRow(modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, o ->
            val on = i == selected || i in selectedSet
            Surface(
                onClick = { onPick(i) },
                shape = RoundedCornerShape(10.dp),
                color = if (on) c.ink else c.paper,
                border = BorderStroke(1.dp, if (on) c.ink else c.line),
                modifier = Modifier.heightIn(min = 44.dp),
            ) {
                Text(
                    rich(o.t()), color = if (on) c.paper else c.ink, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                )
            }
        }
    }
}

/** Posuvník s popiskem; [steps] = počet mezikroků (přichytávání). */
@Composable
fun ValueSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int = 0, onChange: (Float) -> Unit) {
    val c = LocalColors.current
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(rich(label), style = MaterialTheme.typography.bodyMedium, color = c.inkSoft)
        Slider(
            value = value, onValueChange = onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(thumbColor = c.react, activeTrackColor = c.react),
        )
    }
}

/** Možnost v kvízu. [why] = cílená nápověda, když žák zvolí právě tuhle chybnou možnost. */
class Opt(val label: L, val correct: Boolean = false, val why: L? = null)

/**
 * Kvíz bez psaní. Nikdy jen „špatně“: 1. chyba → otázka-nápověda k té konkrétní volbě,
 * 2. chyba → [nudge], 3. chyba → ukáže správnou odpověď s vysvětlením. Po vyřešení kartu dokončí.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardScope.Quiz(question: L, options: List<Opt>, explain: L, nudge: L? = null, columns: Int = 1) {
    var misses by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var solved by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<Pair<Tone, L>?>(null) }
    var extra by remember { mutableStateOf<L?>(null) }
    val correct = options.indexOfFirst { it.correct }
    val key = "quiz:" + question.cs
    SideEffect { report(key, listOf(QUIZ_TASK to solved)) }
    Text(
        rich(question.t()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = LocalColors.current.ink,
        modifier = Modifier.padding(top = 14.dp).nagTarget(this, !solved),
    )
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = columns,
    ) {
        options.forEachIndexed { i, o ->
            val sel = picked == i || (solved && i == correct)
            ChoiceTile(sel, i == correct && sel, !solved || i == correct, onClick = {
                if (solved) return@ChoiceTile
                picked = i
                if (o.correct) {
                    solved = true
                    extra = null
                    msg = Tone.OK to explain
                } else {
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
            }, modifier = Modifier.weight(1f)) {
                Text(rich(o.label.t()), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, color = LocalColors.current.ink)
            }
        }
    }
    msg?.let { (tone, text) -> Note(tone, text) }
    extra?.let { Note(Tone.HINT, it) }
}

private val QUIZ_TASK = L("Odpověz na otázku", "Ответь на вопрос")

/**
 * Úkoly karty – výrazný rámeček „Úkol“ s fajfkami. Karta jde dokončit, až jsou splněné všechny.
 * [items] = text úkolu → splněno (stav drží karta). Když žák zkusí jít dál dřív, rámeček zabliká
 * a posune se do pohledu.
 */
@Composable
fun CardScope.Todo(vararg items: Pair<L, Boolean>) {
    val c = LocalColors.current
    val list = items.toList()
    SideEffect { report("todo", list) }
    val all = list.all { it.second }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (all) c.paper else c.focusBg,
        border = BorderStroke(if (all) 1.dp else 1.5.dp, if (all) c.accent else c.activeBorder),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).nagTarget(this, !all),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                (if (all) L("Hotovo ✓", "Готово ✓") else if (list.size > 1) L("Úkoly", "Задания") else L("Úkol", "Задание")).t(),
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = if (all) c.accent else c.ink,
            )
            for ((label, ok) in list) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.padding(top = 2.dp).size(20.dp)
                            .background(if (ok) c.accent else c.paper, RoundedCornerShape(5.dp))
                            .border(1.5.dp, if (ok) c.accent else c.inkSoft, RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center,
                    ) { if (ok) Text("✓", color = c.paper, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        rich(label.t()), style = MaterialTheme.typography.bodyMedium,
                        color = if (ok) c.inkSoft else c.ink, fontWeight = if (ok) FontWeight.Normal else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** Při [CardScope.nag] (žák chtěl dál, ale [active] úkol ještě není hotový) prvek zabliká a posune se do pohledu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.nagTarget(scope: CardScope, active: Boolean): Modifier {
    val c = LocalColors.current
    val req = remember { BringIntoViewRequester() }
    val flash = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(scope.nag) {
        if (scope.nag == 0 || !active) return@LaunchedEffect
        launch { req.bringIntoView() }
        launch {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(1400))
        }
        for (x in listOf(10f, -10f, 7f, -7f, 3f, 0f)) shake.animateTo(x, tween(45))
    }
    return this.bringIntoViewRequester(req)
        .graphicsLayer { translationX = shake.value * density }
        .then(if (flash.value > 0f) Modifier.border(3.dp, c.nudge.copy(alpha = flash.value), RoundedCornerShape(12.dp)) else Modifier)
}

/** Akční tlačítko na kartě („Odstraň podporu“, „Nahraď“ …). */
@Composable
fun ActionButton(text: L, enabled: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        cz.usharik.statika.ui.SecondaryButton(text.t(), onClick = { if (enabled) onClick() })
    }
}

/** Číslo pro popisky ve výkladu: desetinná čárka, bez zbytečných nul. */
fun num(v: Double, d: Int = 2) = cz.usharik.statika.model.fmt(v, d)

/** Překlad pro použití mimo @Composable (uvnitř kreslení). */
@Composable
fun tr() = LocalTr.current
