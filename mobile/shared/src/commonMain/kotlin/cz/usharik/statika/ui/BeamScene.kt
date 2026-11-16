package cz.usharik.statika.ui

import androidx.compose.ui.graphics.Color
import cz.usharik.statika.model.Dof
import cz.usharik.statika.model.PointForce
import cz.usharik.statika.model.PointMoment
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.Side
import cz.usharik.statika.model.Support
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.model.Udl
import cz.usharik.statika.model.Unknown
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.loadItems
import cz.usharik.statika.model.unknownsOfSupport
import cz.usharik.statika.trainer.DrawingView
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Technický výkres nosníku – geometrie převzatá z webové verze (src/components/BeamDrawing.tsx).
 * Výkres se nejdřív sestaví jako seznam primitiv v souřadnicích SVG (šířka W), pak se vykreslí v Canvasu.
 */

enum class Anchor { START, MIDDLE, END }

/** Jak se prvek objeví: časování vstupní animace, případně podle id (reakce, složky). */
sealed interface Anim {
    data object None : Anim
    data class Fade(val delay: Int) : Anim
    data class Rise(val delay: Int) : Anim
    data class Drop(val delay: Int) : Anim
    data object Beam : Anim
    /** prvek, který přibyl později (reakce, složka síly); id určuje jeho vlastní animaci */
    data class Keyed(val key: String, val kind: Grow) : Anim
}

enum class Grow { FADE, UP, DOWN, RIGHT, LEFT }

sealed interface Prim {
    val anim: Anim
    val hl: Boolean

    data class Line(val x1: Double, val y1: Double, val x2: Double, val y2: Double, val color: Color, val width: Double,
                    val dash: Pair<Double, Double>? = null, override val anim: Anim = Anim.None, override val hl: Boolean = false, val alpha: Float = 1f) : Prim
    data class Poly(val pts: List<Pair<Double, Double>>, val fill: Color?, val stroke: Color? = null, val width: Double = 0.0,
                    override val anim: Anim = Anim.None, override val hl: Boolean = false, val alpha: Float = 1f) : Prim
    data class Rect(val x: Double, val y: Double, val w: Double, val h: Double, val fill: Color?, val stroke: Color? = null, val width: Double = 0.0,
                    override val anim: Anim = Anim.None, override val hl: Boolean = false, val alpha: Float = 1f) : Prim
    data class Circle(val cx: Double, val cy: Double, val r: Double, val fill: Color?, val stroke: Color? = null, val width: Double = 0.0,
                      val dash: Pair<Double, Double>? = null, override val anim: Anim = Anim.None, override val hl: Boolean = false,
                      val alpha: Float = 1f, val pulse: Boolean = false) : Prim
    /** oblouk se středem (cx, cy); úhly ve stupních po směru hodinových ručiček od osy +x (osa y dolů) */
    data class Arc(val cx: Double, val cy: Double, val r: Double, val start: Double, val sweep: Double, val color: Color, val width: Double,
                   override val anim: Anim = Anim.None, override val hl: Boolean = false, val alpha: Float = 1f) : Prim
    data class Label(val x: Double, val y: Double, val sym: String?, val text: String?, val anchor: Anchor, val color: Color, val size: Double,
                     val bold: Boolean = false, override val anim: Anim = Anim.None, override val hl: Boolean = false, val alpha: Float = 1f) : Prim
}

class Scene(val width: Double, val top: Double, val bottom: Double, val prims: List<Prim>)

private const val Y = 178.0
private const val HB = 5.0 // polovina výšky nosníku
private const val H = 358.0

private data class Box(val l: Double, val r: Double, val t: Double, val b: Double)

private fun labelWidth(sym: String, value: String?, size: Double) =
    (sym.replace("_", "").length + (if (value != null) value.length + 3 else 0)) * size * 0.6 + 6

private data class Pos(val x: Double, val y: Double, val anchor: Anchor)

/** Jednoduché rozmísťování popisků: posouvá je nahoru, dokud se nepřekrývají. */
private class Placer(obstacles: List<Box>, val w: Double) {
    val boxes = obstacles.toMutableList()

    private fun left(x: Double, wd: Double, a: Anchor) = when (a) {
        Anchor.START -> x
        Anchor.END -> x - wd
        Anchor.MIDDLE -> x - wd / 2
    }

    fun place(x: Double, y: Double, sym: String, value: String?, anchor: Anchor, size: Double = 15.0): Double {
        val wd = labelWidth(sym, value, size)
        val l = left(x, wd, anchor)
        var yy = y
        fun hit(b: Box) = l < b.r && l + wd > b.l && yy - size < b.b && yy + 4 > b.t
        var k = 0
        while (k < 8 && boxes.any(::hit)) {
            yy -= size + 3
            k++
        }
        boxes += Box(l, l + wd, yy - size, yy + 4)
        return yy
    }

    fun free(x: Double, y: Double, sym: String, value: String?, anchor: Anchor, size: Double = 15.0): Boolean {
        val wd = labelWidth(sym, value, size)
        val l = left(x, wd, anchor)
        return l > 4 && l + wd < w - 4 && boxes.none { b -> l < b.r && l + wd > b.l && y - size < b.b && y + 4 > b.t }
    }

    /** první volná z kandidátních poloh, jinak první posunutá nahoru */
    fun placeAny(cands: List<Pos>, sym: String, value: String? = null): Pos {
        val c = cands.firstOrNull { free(it.x, it.y, sym, value, it.anchor) } ?: cands[0]
        return c.copy(y = place(c.x, c.y, sym, value, c.anchor))
    }
}

/** Stavitel primitiv výkresu – používá ho i interaktivní výklad (učebnice). */
internal class PrimBuilder(val w: Double) {
    val prims = mutableListOf<Prim>()

    fun arrow(x1: Double, y1: Double, x2: Double, y2: Double, color: Color, width: Double = 2.0, dash: Pair<Double, Double>? = null,
              head: Double = 10.0, anim: Anim = Anim.None, hl: Boolean = false, alpha: Float = 1f) {
        val ang = atan2(y2 - y1, x2 - x1)
        val a1 = ang + PI - 0.32
        val a2 = ang + PI + 0.32
        val bx = x2 - cos(ang) * head * 0.8
        val by = y2 - sin(ang) * head * 0.8
        prims += Prim.Line(x1, y1, bx, by, color, width, dash, anim, hl, alpha)
        prims += Prim.Poly(listOf(x2 to y2, x2 + cos(a1) * head to y2 + sin(a1) * head, x2 + cos(a2) * head to y2 + sin(a2) * head), color, anim = anim, hl = hl, alpha = alpha)
    }

    /** Oblouk momentu kolem bodu (cx, cy), nad nosníkem; ccw (↺) má hrot vlevo. */
    fun momentArc(cx: Double, cy: Double, r: Double, ccw: Boolean, color: Color, width: Double = 2.0, anim: Anim, hl: Boolean = false) {
        val a0 = 200 * PI / 180
        val a1 = -20 * PI / 180
        fun p(a: Double) = (cx + r * cos(a)) to (cy - r * sin(a))
        val (sx, sy) = p(a0)
        val (ex, ey) = p(a1)
        prims += Prim.Arc(cx, cy, r, 160.0, 220.0, color, width, anim, hl)
        val (tx, ty) = if (ccw) sx to sy else ex to ey
        val tang = if (ccw) a0 + PI / 2 else a1 - PI / 2
        val dx = cos(tang)
        val dy = -sin(tang)
        val h = 9.0
        val nx = -dy
        val ny = dx
        prims += Prim.Poly(
            listOf(
                (tx + dx * h * 0.6) to (ty + dy * h * 0.6),
                (tx - dx * h * 0.6 + nx * h * 0.45) to (ty - dy * h * 0.6 + ny * h * 0.45),
                (tx - dx * h * 0.6 - nx * h * 0.45) to (ty - dy * h * 0.6 - ny * h * 0.45),
            ),
            color, anim = anim, hl = hl,
        )
    }

    fun hatch(x1: Double, x2: Double, y: Double, c: StatikaColors, anim: Anim, alpha: Float) {
        prims += Prim.Line(x1, y, x2, y, c.ink, 1.5, anim = anim, alpha = alpha)
        var x = x1 + 4
        while (x <= x2) {
            prims += Prim.Line(x, y, x - 6, y + 7, c.inkSoft, 1.0, anim = anim, alpha = alpha)
            x += 6
        }
    }

    /** Popisek se spodním indexem; drží se uvnitř výkresu. */
    fun label(x0: Double, y: Double, sym: String, value: String?, anchor: Anchor, color: Color, size: Double = 15.0, anim: Anim) {
        var x = x0
        val wd = labelWidth(sym, value, size)
        val l = when (anchor) {
            Anchor.START -> x
            Anchor.END -> x - wd
            Anchor.MIDDLE -> x - wd / 2
        }
        if (l < 6) x += 6 - l
        else if (l + wd > w - 6) x -= l + wd - (w - 6)
        prims += Prim.Label(x, y, sym, value?.let { " = $it" }, anchor, color, size, anim = anim)
    }
}

fun buildScene(
    p: Problem,
    released: Set<String>,
    resolved: Set<String>,
    highlight: String?,
    view: DrawingView,
    results: Map<String, Double>?,
    compact: Boolean,
    activeSupport: String?,
    c: StatikaColors,
): Scene {
    val w = if (compact) 560.0 else 880.0
    val m = if (compact) 72.0 else 130.0
    val s = (w - 2 * m) / p.length
    fun X(x: Double) = m + x * s
    fun hl(id: String?) = id != null && highlight == id
    val items = loadItems(p)
    val b = PrimBuilder(w)

    // ---------- kóty ----------
    val keys = mutableSetOf(0.0, p.length)
    p.supports.forEach { keys += it.x }
    p.hinges.forEach { keys += it.x }
    p.loads.forEach { l ->
        when (l) {
            is Udl -> { keys += l.x1; keys += l.x2 }
            is PointForce -> keys += l.x
            is PointMoment -> keys += l.x
        }
    }
    val xs = keys.sorted()
    val yDim = Y + 88
    val yTot = Y + 122
    val dimAnim = Anim.Fade(380)
    val dims = mutableListOf<Prim>()
    xs.forEach { x -> dims += Prim.Line(X(x), Y + 16, X(x), yTot + 6, c.ext, 0.8, anim = dimAnim) }
    fun tick(x: Double, y: Double) = Prim.Line(X(x) - 4, y + 4, X(x) + 4, y - 4, c.ink, 1.3, anim = dimAnim)
    for (i in 0 until xs.size - 1) {
        val a = xs[i]
        val bb = xs[i + 1]
        dims += Prim.Line(X(a), yDim, X(bb), yDim, c.ink, 0.9, anim = dimAnim)
        dims += tick(a, yDim)
        dims += Prim.Label((X(a) + X(bb)) / 2, yDim - 5, null, fmt(bb - a), Anchor.MIDDLE, c.ink, 13.0, anim = dimAnim)
    }
    dims += tick(p.length, yDim)
    if (xs.size > 2) {
        dims += Prim.Line(X(0.0), yTot, X(p.length), yTot, c.ink, 0.9, anim = dimAnim)
        dims += tick(0.0, yTot)
        dims += tick(p.length, yTot)
        dims += Prim.Label((X(0.0) + X(p.length)) / 2, yTot - 5, null, fmt(p.length), Anchor.MIDDLE, c.ink, 13.0, anim = dimAnim)
    }
    dims += Prim.Label(w - 8, yTot + 4, null, "[m]", Anchor.END, c.inkSoft, 11.0, anim = dimAnim)
    b.prims += dims

    // ---------- podpory ----------
    for ((si, sp) in p.supports.withIndex()) {
        val isReleased = sp.id in released || results != null
        val xx = X(sp.x)
        if (activeSupport == sp.id) b.prims += Prim.Circle(xx, Y + 24, 34.0, c.react.copy(alpha = 0.07f), c.react.copy(alpha = 0.35f), 1.0, dash = 4.0 to 4.0, pulse = true)
        supportSymbol(b, sp, xx, p.length, isReleased, Anim.Rise(150 + si * 80), c)
    }

    // ---------- nosník ----------
    val cuts = listOf(0.0) + p.hinges.map { it.x } + listOf(p.length)
    for (i in 0 until cuts.size - 1) {
        val a = X(cuts[i]) + if (i > 0) 6 else 0
        val bb = X(cuts[i + 1]) - if (i < cuts.size - 2) 6 else 0
        b.prims += Prim.Rect(a, Y - HB, bb - a, 2 * HB, c.beam, c.ink, 1.6, anim = Anim.Beam)
    }
    for (h in p.hinges) {
        b.prims += Prim.Circle(X(h.x), Y, 6.5, c.paper, c.ink, 1.8, anim = Anim.Beam)
        b.prims += Prim.Label(X(h.x), Y + 30, null, h.label, Anchor.MIDDLE, c.ink, 16.0, bold = true, anim = Anim.Beam)
    }

    // ---------- překážky pro popisky ----------
    val obstacles = mutableListOf<Box>()
    for (l in p.loads) {
        when (l) {
            is PointForce -> {
                val r = l.angle * PI / 180
                val ux = cos(r)
                val x0 = X(l.x)
                val x1 = x0 - (if (sin(r) < 0) ux * 64 else -ux * 64)
                obstacles += Box(min(x0, x1) - 3, max(x0, x1) + 3, Y - HB - 64 * abs(sin(r)) + 6, Y - HB)
            }
            is PointMoment -> obstacles += Box(X(l.x) - 24, X(l.x) + 24, Y - 26, Y)
            is Udl -> obstacles += Box(X(l.x1), X(l.x2), Y - HB - 34, Y - HB)
        }
    }
    for (sp in p.supports) {
        val xl = if (sp.type == SupportType.FIXED) X(sp.x) + (if (sp.x < p.length / 2) -20 else 20) - 6 else X(sp.x) + 20
        val yl = if (sp.type == SupportType.FIXED) Y + 58 else Y + HB + 22
        obstacles += Box(xl, xl + 14, yl - 14, yl + 2)
        if (sp.id !in released && results == null) continue
        val xsx = X(sp.x)
        for (u in unknownsOfSupport(sp)) {
            obstacles += when (u.kind) {
                Dof.FY -> Box(xsx - 3, xsx + 3, Y + HB, Y + HB + 60)
                Dof.FX -> if (sp.x < p.length / 2) Box(xsx - 60, xsx, Y - 4, Y + 4) else Box(xsx, xsx + 60, Y - 4, Y + 4)
                Dof.M -> Box(xsx - 28, xsx + 28, Y - 30, Y)
            }
        }
    }
    val placer = Placer(obstacles, w)

    // ---------- zatížení ----------
    for ((li, l) in p.loads.withIndex()) {
        val isHl = hl(l.id) || items.any { it.loadId == l.id && hl(it.id) }
        val fade = l.id in resolved
        val anim = Anim.Drop(320 + li * 90)
        when (l) {
            is PointForce -> {
                val r = l.angle * PI / 180
                val ux = cos(r)
                val uy = -sin(r)
                val len = 64.0
                val down = uy > 0
                val tip = if (down) X(l.x) to (Y - HB - 1) else (X(l.x) + ux * len) to (Y - HB - len)
                val tail = if (down) (tip.first - ux * len) to (tip.second - uy * len) else X(l.x) to (Y - HB - 1)
                val col = if (fade) c.inkSoft else c.load
                val anc = if (ux > 0.3) Anchor.END else if (ux < -0.3) Anchor.START else Anchor.MIDDLE
                val lx = tail.first + (if (ux > 0.3) -4.0 else if (ux < -0.3) 4.0 else 0.0)
                val alpha = if (fade) 0.45f else 1f
                b.arrow(tail.first, tail.second, tip.first, tip.second, col, if (isHl) 3.0 else 2.0, if (fade) 5.0 to 4.0 else null, anim = anim, hl = isHl && !fade, alpha = alpha)
                if (l.alpha != null && !fade) angleMark(b, tip, ux, uy, l.alpha, c, anim)
                if (!fade) {
                    val v = "${fmt(l.f)} kN"
                    b.label(lx, placer.place(lx, min(tail.second, tip.second) - 8, l.label, v, anc), l.label, v, anc, col, anim = anim)
                }
            }
            is PointMoment -> {
                val v = "${fmt(l.m)} kNm"
                val lp = placer.placeAny(
                    listOf(Pos(X(l.x), Y - 34, Anchor.MIDDLE), Pos(X(l.x) + 26, Y - 22, Anchor.START), Pos(X(l.x) - 26, Y - 22, Anchor.END)),
                    l.label, v,
                )
                b.momentArc(X(l.x), Y, 22.0, l.ccw, c.load, if (isHl) 3.0 else 2.0, Anim.Fade(320 + li * 90), isHl)
                b.label(lp.x, lp.y, l.label, v, lp.anchor, c.load, anim = Anim.Fade(420 + li * 90))
            }
            is Udl -> {
                val a = X(l.x1)
                val bb = X(l.x2)
                val top = Y - HB - 34
                val n = max(2, ((bb - a) / 22).roundToInt())
                val alpha = if (fade) 0.35f else 1f
                val col = if (fade) c.inkSoft else c.load
                if (!fade) b.prims += Prim.Rect(a, top, bb - a, Y - HB - top, c.loadFill, anim = anim)
                b.prims += Prim.Line(a, top, bb, top, col, 1.6, anim = anim, alpha = alpha)
                for (i in 0..n) {
                    val x = a + (bb - a) * i / n
                    b.arrow(x, top, x, Y - HB - 1, col, 1.3, head = 7.0, anim = anim, alpha = alpha)
                }
                if (!fade) {
                    val v = "${fmt(l.q)} kN/m"
                    b.label((a + bb) / 2, placer.place((a + bb) / 2, top - 8, l.label, v, Anchor.MIDDLE), l.label, v, Anchor.MIDDLE, c.load, anim = anim)
                }
            }
        }
    }

    // ---------- složky a výslednice ----------
    for (it in items) {
        val l = p.loads.first { q -> q.id == it.loadId }
        if (l.id !in resolved || l is PointMoment) continue
        val isHl = hl(it.id)
        val col = c.derived
        val wd = if (isHl) 3.0 else 2.2
        val v = "${fmt(it.value!!)} kN"
        if (it.kind == Dof.FY) {
            val len = if (l is Udl) 70.0 else 50.0
            val tipY = if (it.dir < 0) Y - HB - 1 else Y - HB - len
            val tailY = if (it.dir < 0) Y - HB - len else Y - HB - 1
            val anim = Anim.Keyed("d_" + it.id, Grow.FADE)
            b.arrow(X(it.x), tailY, X(it.x), tipY, col, wd, anim = anim, hl = isHl)
            val pos = placer.placeAny(
                listOf(Pos(X(it.x) + 7, Y - HB - len + 6, Anchor.START), Pos(X(it.x) - 7, Y - HB - len + 6, Anchor.END), Pos(X(it.x), Y - HB - len - 6, Anchor.MIDDLE)),
                it.sym, v,
            )
            b.label(pos.x, pos.y, it.sym, v, pos.anchor, col, 14.0, anim)
        } else {
            val len = 52.0
            val tipX = X(it.x)
            // vodorovná složka kreslená nad osou nosníku, šipka končí v působišti
            val tailX = tipX - it.dir * len
            val anim = Anim.Keyed("d_" + it.id, Grow.FADE)
            b.arrow(tailX, Y - 16, tipX, Y - 16, col, wd, anim = anim, hl = isHl)
            val pos = placer.placeAny(
                listOf(Pos(tailX - it.dir * 4, Y - 11, if (it.dir > 0) Anchor.END else Anchor.START), Pos((tailX + tipX) / 2, Y - 24, Anchor.MIDDLE)),
                it.sym, v,
            )
            b.label(pos.x, pos.y, it.sym, v, pos.anchor, col, 14.0, anim)
        }
    }

    // ---------- reakce ----------
    for (sp in p.supports) {
        val isReleased = sp.id in released || results != null
        if (!isReleased) continue
        for (u in unknownsOfSupport(sp)) reaction(b, placer, u, X(u.x), p.length, hl("r_" + u.id) || hl(u.id), results?.get(u.id), results != null, sp.type == SupportType.FIXED, c)
    }

    // ---------- bod momentu, strana, rameno ----------
    view.side?.let { sv ->
        val xa = if (sv.side == Side.LEFT) X(sv.x) + 8 else 0.0
        val xb = if (sv.side == Side.LEFT) w else X(sv.x) - 8
        b.prims += Prim.Rect(xa, 0.0, xb - xa, yDim - 16, c.paper.copy(alpha = 0.75f), anim = Anim.Fade(0))
    }
    view.arm?.let { arm ->
        if (abs(arm.to - arm.from) > 1e-6) {
            val ya = Y - 70
            val anim = Anim.Keyed("arm_${arm.from}_${arm.to}", Grow.FADE)
            b.prims += Prim.Line(X(arm.from), Y, X(arm.from), ya - 6, c.accent, 1.0, 3.0 to 3.0, anim = anim)
            b.prims += Prim.Line(X(arm.to), Y - 10, X(arm.to), ya - 6, c.accent, 1.0, 3.0 to 3.0, anim = anim)
            val mid = (X(arm.from) + X(arm.to)) / 2
            b.arrow(mid, ya, X(arm.to), ya, c.accent, 1.5, head = 8.0, anim = anim)
            b.arrow(mid, ya, X(arm.from), ya, c.accent, 1.5, head = 8.0, anim = anim)
            b.prims += Prim.Label(mid, ya - 6, null, "${fmt(abs(arm.to - arm.from))} m", Anchor.MIDDLE, c.accent, 13.0, bold = true, anim = anim)
        }
    }
    view.momentPoint?.let { (label, x) ->
        val anim = Anim.Keyed("mp_$label", Grow.FADE)
        b.prims += Prim.Circle(X(x), Y, 9.0, c.accent.copy(alpha = 0.12f), c.accent, 1.5, anim = anim)
        b.prims += Prim.Line(X(x) - 13, Y, X(x) + 13, Y, c.accent, 1.2, anim = anim)
        b.prims += Prim.Line(X(x), Y - 13, X(x), Y + 13, c.accent, 1.2, anim = anim)
    }

    // ořez prázdného místa nad a pod výkresem; horní mez počítá i s místem pro kótu ramene
    val vTop = min(Y - 100, placer.boxes.minOf { it.t }) - 10
    val vBottom = min(H, yTot + 14)
    return Scene(w, vTop, vBottom, b.prims)
}

/** Značka podpory; [y] = osa nosníku, vetknutí má zeď na straně bližšího konce (s.x < len/2 → vlevo). */
internal fun supportSymbol(b: PrimBuilder, s: Support, xx: Double, len: Double, ghost: Boolean, anim: Anim, c: StatikaColors, y: Double = Y) {
    val op = if (ghost) 0.16f else 1f
    val top = y + HB
    if (s.type == SupportType.FIXED) {
        val left = s.x < len / 2
        val d = if (left) -1 else 1
        var yy = y - 34
        while (yy <= y + 34) {
            b.prims += Prim.Line(xx, yy, xx + d * 8, yy + 8, c.inkSoft, 1.3, anim = anim, alpha = op)
            yy += 7
        }
        b.prims += Prim.Line(xx, y - 36, xx, y + 38, c.ink, 2.2, anim = anim, alpha = op)
        b.prims += Prim.Label(xx + d * 20, y + 58, null, s.label, Anchor.MIDDLE, c.ink, 16.0, bold = true, anim = anim)
        return
    }
    val triH = if (s.type == SupportType.PIN) 24 else 20
    val base = top + 4 + triH
    b.prims += Prim.Poly(listOf(xx to top + 4, xx - 13 to base, xx + 13 to base), c.paper, c.ink, 1.5, anim = anim, alpha = op)
    b.prims += Prim.Circle(xx, top + 4, 4.0, c.paper, c.ink, 1.5, anim = anim, alpha = op)
    if (s.type == SupportType.PIN) {
        b.hatch(xx - 20, xx + 20, base, c, anim, op)
    } else {
        b.prims += Prim.Circle(xx - 7, base + 3.5, 3.2, c.paper, c.ink, 1.3, anim = anim, alpha = op)
        b.prims += Prim.Circle(xx + 7, base + 3.5, 3.2, c.paper, c.ink, 1.3, anim = anim, alpha = op)
        b.hatch(xx - 20, xx + 20, base + 7, c, anim, op)
    }
    b.prims += Prim.Label(xx + 22, top + 22, null, s.label, Anchor.START, c.ink, 16.0, bold = true, anim = anim)
}

/** Úhel mezi osou nosníku a nositelkou síly, u působiště. */
internal fun angleMark(b: PrimBuilder, tip: Pair<Double, Double>, ux: Double, uy: Double, alpha: Int, c: StatikaColors, anim: Anim) {
    val r = 30.0
    val hx = if (ux > 0) -1.0 else 1.0 // vodorovná polopřímka na straně konce síly
    val (tx, ty) = tip
    b.prims += Prim.Line(tx, ty, tx + hx * (r + 12), ty, c.inkSoft, 1.0, 3.0 to 3.0, anim = anim)
    val a0 = if (hx > 0) 0.0 else 180.0
    val a1 = atan2(-uy, -ux) * 180 / PI
    var d = a1 - a0
    while (d > 180) d -= 360
    while (d <= -180) d += 360
    b.prims += Prim.Arc(tx, ty, r, a0, d, c.inkSoft, 1.0, anim = anim)
    b.prims += Prim.Label(tx + hx * (r + 6) - (if (hx < 0) 16 else -2), ty - 8, null, "$alpha°", Anchor.START, c.inkSoft, 12.0, anim = anim)
}

/** Reakce v předpokládaném smyslu (nebo ve skutečném, když jsou známé výsledky). */
private fun reaction(b: PrimBuilder, placer: Placer, u: Unknown, xx: Double, len: Double, hl: Boolean, value: Double?, res: Boolean, fixed: Boolean, c: StatikaColors) {
    val neg = value != null && value < 0
    // nulová reakce ve výsledku: jen popisek, šipka by naznačovala směr, který neexistuje
    val zero = res && value != null && abs(value) < 1e-9
    val col = c.react
    val wd = if (hl) 3.2 else 2.4
    val v = value?.let { "${fmt(abs(it))} ${if (u.kind == Dof.M) "kNm" else "kN"}" }
    val key = "r_" + u.id + if (res) "-res" else ""
    when (u.kind) {
        Dof.FY -> {
            val l = 58.0
            val up = !neg
            // šipka zespodu: nahoru končí na spodní hraně, dolů začíná na spodní hraně
            val y1 = if (up) Y + HB + l else Y + HB + 2
            val y2 = if (up) Y + HB + 2 else Y + HB + l
            if (!zero) b.arrow(xx, y1, xx, y2, col, wd, anim = Anim.Keyed(key, if (up) Grow.UP else Grow.DOWN), hl = hl)
            val pos = placer.placeAny(
                listOf(Pos(xx + 8, Y + HB + l - 2, Anchor.START), Pos(xx - 8, Y + HB + l - 2, Anchor.END), Pos(xx + 8, Y + HB + l - 20, Anchor.START)),
                u.sym, v,
            )
            b.label(pos.x, pos.y, u.sym, v, pos.anchor, col, 14.0, Anim.Keyed(key, Grow.FADE))
        }
        Dof.FX -> {
            val l = 52.0
            val atLeft = u.x < len / 2 || u.x == 0.0
            val right = !neg
            // kreslíme na vnější straně podpory; u vetknutí je vně zeď – tam šipku pod nosník na straně nosníku
            val o = if (atLeft xor fixed) -1 else 1
            val ay = if (fixed) Y + HB + 12 else Y
            val far = xx + o * (l + 6)
            val near = xx + o * 6
            val (x1, x2) = if (right == (o < 0)) far to near else near to far
            if (!zero) b.arrow(x1, ay, x2, ay, col, wd, anim = Anim.Keyed(key, if (x2 > x1) Grow.RIGHT else Grow.LEFT), hl = hl)
            val atLeftLabel = o < 0
            val pos = placer.placeAny(
                listOf(
                    Pos(far, ay - 10, if (atLeftLabel) Anchor.START else Anchor.END),
                    Pos(far + o * 4, ay + 5, if (atLeftLabel) Anchor.END else Anchor.START),
                    Pos(xx + o * 10, ay + 24, if (atLeftLabel) Anchor.END else Anchor.START),
                    Pos(far, ay + 24, if (atLeftLabel) Anchor.START else Anchor.END),
                ),
                u.sym, v,
            )
            b.label(pos.x, pos.y, u.sym, v, pos.anchor, col, 14.0, Anim.Keyed(key, Grow.FADE))
        }
        Dof.M -> {
            val atLeft = u.x < len / 2
            if (!zero) b.momentArc(xx, Y, 26.0, !neg, col, wd, Anim.Keyed(key, Grow.FADE), hl)
            val pos = placer.placeAny(
                listOf(
                    Pos(xx + (if (atLeft) 18 else -18), Y - 34, if (atLeft) Anchor.START else Anchor.END),
                    Pos(xx + (if (atLeft) -18 else 18), Y - 34, if (atLeft) Anchor.END else Anchor.START),
                    Pos(xx, Y - 44, Anchor.MIDDLE),
                ),
                u.sym, v,
            )
            b.label(pos.x, pos.y, u.sym, v, pos.anchor, col, 14.0, Anim.Keyed(key, Grow.FADE))
        }
    }
}
