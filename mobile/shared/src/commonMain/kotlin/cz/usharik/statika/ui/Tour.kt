package cz.usharik.statika.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.usharik.statika.i18n.LocalTr
import cz.usharik.statika.platform.KeyValueStore

/** Místa obrazovky, na která prohlídka ukazuje. */
enum class TourSpot { DRAWING, STAR, STEPPER, WORK, NEW, FAVORITES, LEARN, SETTINGS }

/** Krok prohlídky: nadpis a text (klíče překladu) a místo, které se zvýrazní (null = celá obrazovka). */
data class TourStep(val key: String, val spot: TourSpot?)

/**
 * Prohlídka aplikace při prvním spuštění: postupně zvýrazní části obrazovky a řekne, k čemu jsou.
 * Poslední krok nabídne výklad základů (dřívější samostatná otázka při prvním spuštění).
 */
class TourState(private val store: KeyValueStore) {
    /** Místa na obrazovce (v souřadnicích kořene), jak je hlásí [tourSpot]. */
    val spots = mutableStateMapOf<TourSpot, Rect>()

    /** Právě zobrazený krok, nebo null, když prohlídka neběží. */
    var index by mutableStateOf<Int?>(if (store.getInt(K_DONE, 0) == 1) null else 0)
        private set

    val step: TourStep? get() = index?.let(STEPS::getOrNull)
    val running get() = index != null

    fun start() {
        index = 0
    }

    fun next() {
        val i = index ?: return
        if (i + 1 < STEPS.size) index = i + 1 else finish()
    }

    fun back() {
        val i = index ?: return
        if (i > 0) index = i - 1
    }

    fun finish() {
        index = null
        store.putInt(K_DONE, 1)
    }

    companion object {
        const val K_DONE = "tour.done"

        val STEPS = listOf(
            TourStep("tourWelcome", null),
            TourStep("tourDrawing", TourSpot.DRAWING),
            TourStep("tourStepper", TourSpot.STEPPER),
            TourStep("tourWork", TourSpot.WORK),
            TourStep("tourStar", TourSpot.STAR),
            TourStep("tourFavorites", TourSpot.FAVORITES),
            TourStep("tourNew", TourSpot.NEW),
            TourStep("tourLearn", TourSpot.LEARN),
            TourStep("tourSettings", TourSpot.SETTINGS),
            TourStep("tourEnd", null),
        )
    }
}

val LocalTour = staticCompositionLocalOf<TourState?> { null }

/** Označí prvek, na který může prohlídka ukázat. */
@Composable
fun Modifier.tourSpot(spot: TourSpot): Modifier {
    val tour = LocalTour.current ?: return this
    return onGloballyPositioned { tour.spots[spot] = it.boundsInRoot() }
}

/**
 * Překryv s prohlídkou. Zbytek obrazovky ztmaví, zvýrazněné místo nechá vidět a pod/nad ně dá bublinu s textem.
 * Klepnutí mimo bublinu nic nedělá – aplikace se během prohlídky nedá omylem ovládat.
 * [onLearn] = „Ano, ukaž mi základy“ v posledním kroku.
 */
@Composable
fun TourOverlay(tour: TourState, onLearn: () -> Unit, onEvent: (String, Map<String, Any>) -> Unit) {
    val step = tour.step ?: return
    val i = tour.index ?: return
    val tr = LocalTr.current
    val c = LocalColors.current
    val density = LocalDensity.current
    val hole = step.spot?.let { tour.spots[it] }?.takeIf { it.width > 0f && it.height > 0f }
    val pad = with(density) { 6.dp.toPx() }
    val ring = c.highlight
    // plynulý přesun zvýraznění mezi kroky
    val l by animateFloatAsState(hole?.left ?: 0f, tween(250))
    val t by animateFloatAsState(hole?.top ?: 0f, tween(250))
    val r by animateFloatAsState(hole?.right ?: 0f, tween(250))
    val b by animateFloatAsState(hole?.bottom ?: 0f, tween(250))

    BoxWithConstraints(
        Modifier.fillMaxSize().testTag("tour")
            // spolkne všechna klepnutí mimo bublinu
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color.Black.copy(alpha = 0.62f))
            if (hole != null) {
                val tl = Offset(l - pad, t - pad)
                val sz = Size(r - l + 2 * pad, b - t + 2 * pad)
                val cr = CornerRadius(14.dp.toPx())
                drawRoundRect(Color.Transparent, tl, sz, cr, blendMode = BlendMode.Clear)
                drawRoundRect(ring, tl, sz, cr, style = Stroke(3.dp.toPx()))
            }
        }
        // bublina tam, kde je víc místa: pod/nad zvýrazněním, u vysokého úzkého místa (tablet) vedle něj
        val align = when {
            hole == null -> Alignment.Center
            hole.height > hPx * 0.55f && hole.width < wPx * 0.6f ->
                if (hole.center.x > wPx / 2) Alignment.CenterStart else Alignment.CenterEnd
            hole.center.y < hPx / 2 -> Alignment.BottomCenter
            else -> Alignment.TopCenter
        }
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), contentAlignment = align) {
            Surface(
                shape = CardShape, color = c.paper, border = BorderStroke(1.dp, c.line), shadowElevation = 8.dp,
                modifier = Modifier.widthIn(max = 420.dp).testTag("tourCard"),
            ) {
                Column(Modifier.padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 8.dp)) {
                    Text(
                        tr(step.key + "Title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = c.ink,
                        modifier = Modifier.padding(end = 8.dp).semantics { heading(); liveRegion = LiveRegionMode.Polite },
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(rich(tr(step.key + "Text")), style = MaterialTheme.typography.bodyMedium, color = c.ink, modifier = Modifier.padding(end = 8.dp))
                    Spacer(Modifier.size(10.dp))
                    Dots(i, TourState.STEPS.size)
                    if (step.key == "tourEnd") {
                        // poslední krok: dřívější otázka „Je pro tebe statika nová?“
                        Column(Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp)) {
                            PrimaryButton(tr("firstRunYes"), {
                                onEvent("learn_first_run", mapOf("start" to "yes"))
                                tour.finish()
                                onLearn()
                            }, Modifier.fillMaxWidth())
                            Row {
                                TextButton(onClick = tour::back) { Text(tr("tourBack")) }
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = {
                                    onEvent("learn_first_run", mapOf("start" to "no"))
                                    tour.finish()
                                }) { Text(tr("firstRunNo")) }
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                onEvent("tour_skip", mapOf("step" to i))
                                tour.finish()
                            }) { Text(tr("tourSkip"), color = c.inkSoft) }
                            Spacer(Modifier.weight(1f))
                            if (i > 0) TextButton(onClick = tour::back) { Text(tr("tourBack")) }
                            TextButton(onClick = tour::next) { Text(tr("tourNext"), fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dots(current: Int, count: Int) {
    val c = LocalColors.current
    val tr = LocalTr.current
    Row(
        Modifier.semantics(mergeDescendants = true) {}.padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        for (k in 0 until count) {
            Box(Modifier.size(if (k == current) 8.dp else 6.dp).background(if (k <= current) c.ink else c.line, CircleShape))
        }
        Spacer(Modifier.size(6.dp))
        Text(tr("tourCount", "n" to current + 1, "of" to count), style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
    }
}
