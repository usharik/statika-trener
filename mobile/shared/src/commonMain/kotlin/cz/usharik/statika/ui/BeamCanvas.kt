package cz.usharik.statika.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import cz.usharik.statika.model.Problem
import cz.usharik.statika.trainer.DrawingView
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/** Průběh 0…1 s náběhem a doběhem. */
private fun ease(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return 1 - (1 - x) * (1 - x) * (1 - x)
}

/** Technický výkres nosníku s animovaným nástupem prvků. */
@Composable
fun BeamCanvas(
    problem: Problem,
    released: Set<String>,
    resolved: Set<String>,
    highlight: String?,
    view: DrawingView,
    results: Map<String, Double>?,
    activeSupport: String?,
    compact: Boolean,
    animKey: Any,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColors.current
    val scene = remember(problem, released, resolved, highlight, view, results, compact, activeSupport, colors) {
        buildScene(problem, released, resolved, highlight, view, results, compact, activeSupport, colors)
    }
    val measurer = rememberTextMeasurer(cacheSize = 64)

    // vstupní animace celé úlohy (čas v ms)
    val clock = remember(animKey) { Animatable(0f) }
    LaunchedEffect(animKey) { clock.animateTo(1400f, tween(1400, easing = LinearEasing)) }

    // prvky, které přibydou později (reakce, složky, bod momentu …) – každý má vlastní náběh
    val keyed = remember(animKey) { mutableStateMapOf<String, Animatable<Float, *>>() }
    val keys = scene.prims.mapNotNull { (it.anim as? Anim.Keyed)?.key }.toSet()
    // animace běží v oboru composable, ne efektu – změna klíčů ji nesmí přerušit v půlce
    val scope = rememberCoroutineScope()
    LaunchedEffect(keys, keyed) {
        for (k in keys) if (k !in keyed) {
            val a = Animatable(0f)
            keyed[k] = a
            scope.launch { a.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        }
    }
    // pulzující kruh jen u aktivní podpory – jinak se výkres nepřekresluje v každém snímku
    val pulse: State<Float> = if (scene.prims.any { it is Prim.Circle && it.pulse }) {
        rememberInfiniteTransition().animateFloat(0.55f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse))
    } else remember { mutableFloatStateOf(1f) }

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio((scene.width / (scene.bottom - scene.top)).toFloat())
            .semantics { contentDescription = description },
    ) {
        val k = size.width / scene.width.toFloat()
        val t = clock.value
        for (p in scene.prims) {
            val (alpha, dx, dy, grow) = animState(p.anim, t, keyed)
            if (alpha <= 0.001f) continue
            val a = alpha * (if (p is Prim.Circle && p.pulse) pulse.value else 1f)
            withTransform({
                scale(k, k, Offset.Zero)
                translate(dx, dy - scene.top.toFloat())
                if (p.anim == Anim.Beam && grow < 1f) scale(grow, 1f, Offset(scene.width.toFloat() / 2, 0f))
                growTransform(p, grow)
            }) {
                drawPrim(p, a, colors, measurer, k)
            }
        }
    }
}

private data class AnimState(val alpha: Float, val dx: Float, val dy: Float, val grow: Float)

private fun animState(anim: Anim, t: Float, keyed: Map<String, Animatable<Float, *>>): AnimState = when (anim) {
    Anim.None -> AnimState(1f, 0f, 0f, 1f)
    is Anim.Fade -> AnimState(ease((t - anim.delay) / 400f), 0f, 0f, 1f)
    is Anim.Rise -> ease((t - anim.delay) / 450f).let { AnimState(it, 0f, 14f * (1 - it), 1f) }
    is Anim.Drop -> ease((t - anim.delay) / 450f).let { AnimState(it, 0f, -20f * (1 - it), 1f) }
    Anim.Beam -> ease(t / 550f).let { AnimState(min(1f, it * 1.5f), 0f, 0f, it) }
    is Anim.Keyed -> (keyed[anim.key]?.value ?: 0f).let { v ->
        if (anim.kind == Grow.FADE) AnimState(v, 0f, 0f, 1f) else AnimState(min(1f, v * 2), 0f, 0f, max(0.001f, v))
    }
}

/** Šipky reakcí „vyrůstají“ od svého konce. */
private fun androidx.compose.ui.graphics.drawscope.DrawTransform.growTransform(p: Prim, grow: Float) {
    val kind = ((p.anim as? Anim.Keyed)?.kind) ?: return
    if (kind == Grow.FADE || grow >= 1f) return
    val (ox, oy) = when (p) {
        is Prim.Line -> p.x1 to p.y1
        is Prim.Poly -> p.pts[0]
        else -> return
    }
    val pivot = Offset(ox.toFloat(), oy.toFloat())
    when (kind) {
        Grow.UP, Grow.DOWN -> scale(1f, grow, pivot)
        Grow.LEFT, Grow.RIGHT -> scale(grow, 1f, pivot)
        Grow.FADE -> {}
    }
}

private fun off(x: Double, y: Double) = Offset(x.toFloat(), y.toFloat())

internal fun DrawScope.drawPrim(p: Prim, alpha: Float, c: StatikaColors, measurer: TextMeasurer, k: Float) {
    val glow = c.highlight.copy(alpha = 0.55f)
    when (p) {
        is Prim.Line -> {
            val a = alpha * p.alpha
            val effect = p.dash?.let { PathEffect.dashPathEffect(floatArrayOf(it.first.toFloat(), it.second.toFloat())) }
            if (p.hl) drawLine(glow, off(p.x1, p.y1), off(p.x2, p.y2), (p.width + 6).toFloat(), StrokeCap.Round, alpha = a)
            drawLine(p.color, off(p.x1, p.y1), off(p.x2, p.y2), p.width.toFloat(), pathEffect = effect, alpha = a)
        }
        is Prim.Poly -> {
            val path = Path().apply {
                moveTo(p.pts[0].first.toFloat(), p.pts[0].second.toFloat())
                for (q in p.pts.drop(1)) lineTo(q.first.toFloat(), q.second.toFloat())
                close()
            }
            val a = alpha * p.alpha
            if (p.hl) drawPath(path, glow, alpha = a, style = Stroke(6f))
            p.fill?.let { drawPath(path, it, alpha = a, style = Fill) }
            p.stroke?.let { drawPath(path, it, alpha = a, style = Stroke(p.width.toFloat())) }
        }
        is Prim.Rect -> {
            val a = alpha * p.alpha
            p.fill?.let { drawRect(it, off(p.x, p.y), Size(p.w.toFloat(), p.h.toFloat()), alpha = a) }
            p.stroke?.let { drawRect(it, off(p.x, p.y), Size(p.w.toFloat(), p.h.toFloat()), alpha = a, style = Stroke(p.width.toFloat())) }
        }
        is Prim.Circle -> {
            val a = alpha * p.alpha
            p.fill?.let { drawCircle(it, p.r.toFloat(), off(p.cx, p.cy), alpha = a) }
            val effect = p.dash?.let { PathEffect.dashPathEffect(floatArrayOf(it.first.toFloat(), it.second.toFloat())) }
            p.stroke?.let { drawCircle(it, p.r.toFloat(), off(p.cx, p.cy), alpha = a, style = Stroke(p.width.toFloat(), pathEffect = effect)) }
        }
        is Prim.Arc -> {
            val a = alpha * p.alpha
            val tl = off(p.cx - p.r, p.cy - p.r)
            val sz = Size((2 * p.r).toFloat(), (2 * p.r).toFloat())
            if (p.hl) drawArc(glow, p.start.toFloat(), p.sweep.toFloat(), false, tl, sz, alpha = a, style = Stroke((p.width + 6).toFloat(), cap = StrokeCap.Round))
            drawArc(p.color, p.start.toFloat(), p.sweep.toFloat(), false, tl, sz, alpha = a, style = Stroke(p.width.toFloat()))
        }
        is Prim.Label -> {
            val text = buildAnnotatedString {
                p.sym?.let { appendSym(it) }
                p.text?.let { append(it) }
            }
            // písmo v jednotkách výkresu → pixely nezávislé na systémovém zvětšení písma
            val px = (p.size * k).toFloat()
            val style = TextStyle(
                color = p.color,
                fontSize = (px / density / fontScale).sp,
                fontWeight = if (p.bold) FontWeight.SemiBold else FontWeight.Normal,
            )
            val layout = measurer.measure(text, style)
            val wUnits = layout.size.width / k
            val x0 = when (p.anchor) {
                Anchor.START -> p.x
                Anchor.MIDDLE -> p.x - wUnits / 2
                Anchor.END -> p.x - wUnits
            }
            val baseline = layout.firstBaseline / k
            // měřený text je v pixelech – kreslíme ho bez měřítka výkresu
            withTransform({ scale(1 / k, 1 / k, off(x0, p.y - baseline)) }) {
                drawText(layout, topLeft = off(x0, p.y - baseline), alpha = alpha * p.alpha)
            }
        }
    }
}
