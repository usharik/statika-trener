package cz.usharik.statika.learn

import cz.usharik.statika.model.fmt
import cz.usharik.statika.practice.AngleFrom
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.Prim
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Kreslení pro kurz trigonometrie a procvičování. Souřadnice scény jako u Stage: šířka 400, osa y DOLŮ.
 * Barvy: přepona / síla = c.load (červeně), odvěsny / složky = c.derived (fialově), úhly a pomocné čáry = c.inkSoft,
 * zvýrazněná strana = c.highlight, rameno a „správně“ = c.accent. Modrá (c.react) je jen pro reakce – tady ne.
 */

/**
 * Pravoúhlý trojúhelník ve scéně: [a] = vrchol úhlu α, [c] = pravý úhel, [b] = třetí vrchol.
 * Odvěsna a–c je přilehlá k α, c–b protilehlá, a–b přepona.
 */
data class TriGeom(val a: Pt, val b: Pt, val c: Pt) {
    fun side(s: TriSide): Pair<Pt, Pt> = when (s) {
        TriSide.ADJ -> a to c
        TriSide.OPP -> c to b
        TriSide.HYP -> a to b
    }

    val centroid: Pt get() = Pt((a.x + b.x + c.x) / 3, (a.y + b.y + c.y) / 3)
    val minX get() = min(a.x, min(b.x, c.x))
    val maxX get() = max(a.x, max(b.x, c.x))
    val minY get() = min(a.y, min(b.y, c.y))
    val maxY get() = max(a.y, max(b.y, c.y))
}

/**
 * Geometrie trojúhelníku s odvěsnami [adj] (u α) a [opp]. Základní poloha: α vlevo dole, pravý úhel vpravo dole,
 * přepona stoupá doprava. [rot] = otočení o rot × 90° proti směru hodin, [mirror] = zrcadlení vodorovně.
 * S [fit] = (x, y, w, h) se trojúhelník zvětší/zmenší a vystředí do obdélníku; jinak [scale] a vrchol α v [origin].
 */
fun triGeom(
    adj: Double,
    opp: Double,
    origin: Pt = Pt(0.0, 0.0),
    scale: Double = 1.0,
    rot: Int = 0,
    mirror: Boolean = false,
    fit: List<Double>? = null,
): TriGeom {
    // základ v matematických souřadnicích (y nahoru)
    var pts = listOf(0.0 to 0.0, adj to opp, adj to 0.0)
    if (mirror) pts = pts.map { (x, y) -> -x to y }
    val r = ((rot % 4) + 4) % 4
    repeat(r) { pts = pts.map { (x, y) -> -y to x } }
    // na obrazovku (y dolů)
    var sp = pts.map { (x, y) -> x to -y }
    val k: Double
    val ox: Double
    val oy: Double
    if (fit != null) {
        val (fx, fy, fw, fh) = fit
        val minX = sp.minOf { it.first }
        val maxX = sp.maxOf { it.first }
        val minY = sp.minOf { it.second }
        val maxY = sp.maxOf { it.second }
        k = min(fw / max(maxX - minX, 1e-9), fh / max(maxY - minY, 1e-9))
        ox = fx + (fw - (maxX - minX) * k) / 2 - minX * k
        oy = fy + (fh - (maxY - minY) * k) / 2 - minY * k
    } else {
        k = scale
        ox = origin.x
        oy = origin.y
    }
    sp = sp.map { (x, y) -> ox + x * k to oy + y * k }
    return TriGeom(Pt(sp[0].first, sp[0].second), Pt(sp[1].first, sp[1].second), Pt(sp[2].first, sp[2].second))
}

/** Značka pravého úhlu ve vrcholu [at] mezi směry k bodům [p1] a [p2]. */
internal fun PrimBuilder.rightAngleMark(at: Pt, p1: Pt, p2: Pt, c: StatikaColors, size: Double = 11.0) {
    fun u(p: Pt): Pair<Double, Double> {
        val d = hypot(p.x - at.x, p.y - at.y).coerceAtLeast(1e-9)
        return (p.x - at.x) / d to (p.y - at.y) / d
    }
    val (ux, uy) = u(p1)
    val (vx, vy) = u(p2)
    val q1 = at.x + ux * size to at.y + uy * size
    val q2 = at.x + (ux + vx) * size to at.y + (uy + vy) * size
    val q3 = at.x + vx * size to at.y + vy * size
    line(q1.first, q1.second, q2.first, q2.second, c.inkSoft, 1.2)
    line(q2.first, q2.second, q3.first, q3.second, c.inkSoft, 1.2)
}

/** směr z [from] do [to] ve stupních na obrazovce (po směru hodin od +x, osa y dolů) */
fun screenAngle(from: Pt, to: Pt): Double = atan2(to.y - from.y, to.x - from.x) * 180 / PI

/**
 * Oblouk úhlu se středem (cx, cy) mezi směry [a1] a [a2] (stupně na obrazovce, viz [screenAngle]) – kreslí se ten kratší;
 * [label] se dá do osy oblouku kousek za něj.
 */
internal fun PrimBuilder.angleArc(cx: Double, cy: Double, r: Double, a1: Double, a2: Double, label: String?, c: StatikaColors, color: Color = c.inkSoft) {
    var sweep = ((a2 - a1) % 360 + 540) % 360 - 180 // −180…180
    if (abs(sweep) < 0.5) return
    prims += Prim.Arc(cx, cy, r, a1, sweep, color, 1.4)
    if (label != null) {
        val mid = (a1 + sweep / 2) * PI / 180
        text(cx + (r + 16) * cos(mid), cy + (r + 16) * sin(mid) + 5, label, color, 13.0)
    }
}

/** Pravoúhlý trojúhelník s popisky stran a úhlu α; [highlight] = zvýrazněná strana (tlustší, c.highlight). */
internal fun PrimBuilder.rightTriangle(
    g: TriGeom,
    c: StatikaColors,
    labels: Map<TriSide, String> = emptyMap(),
    highlight: TriSide? = null,
    angle: String? = "α",
    hyp: Color = c.load,
    legs: Color = c.derived,
) {
    poly(listOf(g.a.x to g.a.y, g.b.x to g.b.y, g.c.x to g.c.y), c.subtle, null, 0.0, alpha = 0.6f)
    for (s in TriSide.entries) {
        val (p, q) = g.side(s)
        val hl = s == highlight
        line(p.x, p.y, q.x, q.y, if (hl) c.highlight else if (s == TriSide.HYP) hyp else legs, if (hl) 4.0 else 2.4)
        labels[s]?.let { t ->
            // popisek u středu strany, odsunutý ven od těžiště
            val mx = (p.x + q.x) / 2
            val my = (p.y + q.y) / 2
            val dx = mx - g.centroid.x
            val dy = my - g.centroid.y
            val d = hypot(dx, dy).coerceAtLeast(1e-9)
            val ox = mx + dx / d * 18
            val oy = my + dy / d * 18 + 5
            val anchor = if (dx / d > 0.4) Anchor.START else if (dx / d < -0.4) Anchor.END else Anchor.MIDDLE
            text(ox, oy, t, if (hl) c.highlight else if (s == TriSide.HYP) hyp else legs, 14.0, anchor, bold = hl)
        }
    }
    rightAngleMark(g.c, g.a, g.b, c)
    if (angle != null) angleArc(g.a.x, g.a.y, 26.0, screenAngle(g.a, g.c), screenAngle(g.a, g.b), angle, c)
}

/** Půlkruhový úhloměr 0–180° se středem (cx, cy) a dílky po 10°. */
internal fun PrimBuilder.protractor(cx: Double, cy: Double, r: Double, c: StatikaColors) {
    prims += Prim.Arc(cx, cy, r, 180.0, 180.0, c.inkSoft, 1.4)
    line(cx - r, cy, cx + r, cy, c.inkSoft, 1.2)
    for (d in 0..180 step 10) {
        val a = d * PI / 180
        val l = if (d % 30 == 0) 10.0 else 5.0
        line(cx + r * cos(a), cy - r * sin(a), cx + (r - l) * cos(a), cy - (r - l) * sin(a), c.inkSoft, 1.0)
        if (d % 30 == 0) text(cx + (r - 22) * cos(a), cy - (r - 22) * sin(a) + 4, "$d", c.inkSoft, 10.0)
    }
}

/** Čtverečkovaná síť [nx] × [ny] políček o straně [step] od (ox, oy). */
internal fun PrimBuilder.grid(ox: Double, oy: Double, step: Double, nx: Int, ny: Int, c: StatikaColors) {
    for (i in 0..nx) line(ox + i * step, oy, ox + i * step, oy + ny * step, c.line, 0.8)
    for (j in 0..ny) line(ox, oy + j * step, ox + nx * step, oy + j * step, c.line, 0.8)
}

/** Osy x (doprava) a y (nahoru) s počátkem v (cx, cy). */
internal fun PrimBuilder.axes(cx: Double, cy: Double, len: Double, c: StatikaColors, labels: Boolean = true) {
    arrow(cx - len, cy, cx + len, cy, c.inkSoft, 1.2, head = 8.0)
    arrow(cx, cy + len * 0.8, cx, cy - len * 0.8, c.inkSoft, 1.2, head = 8.0)
    if (labels) {
        text(cx + len - 4, cy + 16, "x", c.inkSoft, 13.0)
        text(cx + 12, cy - len * 0.8 + 6, "y", c.inkSoft, 13.0)
    }
}

/** Kóta mezi dvěma body s textem uprostřed (odsazeným o [offset] kolmo ke kótě). */
internal fun PrimBuilder.dimLine(x1: Double, y1: Double, x2: Double, y2: Double, text: String, c: StatikaColors, offset: Double = 14.0, color: Color = c.inkSoft) {
    line(x1, y1, x2, y2, color, 1.0)
    val d = hypot(x2 - x1, y2 - y1).coerceAtLeast(1e-9)
    val nx = -(y2 - y1) / d
    val ny = (x2 - x1) / d
    for ((x, y) in listOf(x1 to y1, x2 to y2)) line(x - nx * 5, y - ny * 5, x + nx * 5, y + ny * 5, color, 1.0)
    text((x1 + x2) / 2 + nx * offset, (y1 + y2) / 2 + ny * offset + 5, text, color, 13.0)
}

/** Svislý sloupec 0…1 (např. sin α) – výška [h] odpovídá hodnotě 1. */
internal fun PrimBuilder.bar(x: Double, y: Double, w: Double, h: Double, value: Double, color: Color, c: StatikaColors) {
    rect(x, y, w, h, null, c.line, 1.0)
    val v = value.coerceIn(0.0, 1.0)
    rect(x, y + h * (1 - v), w, h * v, color, null, alpha = 0.85f)
}

/**
 * Obrázek úlohy procvičování (Stage 400 × [FIG_H]). [solved] = po vyřešení (Force dokreslí složky, je-li `components`).
 */
internal fun PrimBuilder.fig(f: Fig, c: StatikaColors) {
    when (f) {
        is Fig.Tri -> {
            val g = triGeom(f.adj, f.opp, rot = f.rot, mirror = f.mirror, fit = listOf(70.0, 30.0, 260.0, FIG_H - 70))
            rightTriangle(g, c, f.labels, f.highlight, f.angle)
        }
        is Fig.Force -> forceFig(f, 200.0, FIG_H / 2 + 5, c)
        is Fig.Sum -> {
            axes(200.0, FIG_H / 2 + 5, 150.0, c)
            for (x in f.forces) {
                // svislá síla: popisek na stranu, kde nejsou ostatní síly (jinak by se popisky překryly)
                val othersRight = f.forces.any { o -> o !== x && cos(o.phi * PI / 180) > 0.1 }
                forceFig(x, 200.0, FIG_H / 2 + 5, c, withAxes = false, verticalLabelLeft = othersRight)
            }
        }
        is Fig.Slope -> {
            val g = triGeom(f.run, f.rise, fit = listOf(80.0, 30.0, 260.0, FIG_H - 70))
            line(20.0, g.c.y, 380.0, g.c.y, c.ink, 1.4)
            // popisky ručně: u plochého sklonu by se popisky z rightTriangle (od těžiště) položily na čáry
            rightTriangle(g, c, emptyMap(), null, null, hyp = c.ink, legs = c.derived)
            f.runLabel?.let { text((g.a.x + g.c.x) / 2, g.c.y + 20, it, c.derived, 14.0) }
            f.riseLabel?.let { text(g.c.x + 10, (g.c.y + g.b.y) / 2 + 5, it, c.derived, 14.0, Anchor.START) }
            if (f.angle != null) {
                angleArc(g.a.x, g.a.y, 26.0, screenAngle(g.a, g.c), screenAngle(g.a, g.b), null, c)
                text(g.a.x - 8, g.a.y - 8, f.angle, c.inkSoft, 13.0, Anchor.END)
            }
        }
        is Fig.Lever -> {
            val ox = 70.0
            val oy = FIG_H / 2 + 20
            val len = 220.0
            val ex = ox + len
            circle(ox, oy, 6.0, c.paper, c.ink, 2.0)
            text(ox - 10, oy + 22, "O", c.ink, 14.0, bold = true)
            rect(ox, oy - 6, len, 12.0, c.beam, c.ink, 1.4)
            // síla začíná na konci páky a svírá s ní úhel alpha; ccw = otáčí proti směru hodin (míří nahoru)
            val dirScreen = if (f.ccw) -f.alpha else f.alpha
            val a = dirScreen * PI / 180
            val l = 90.0
            val ux = cos(a)
            val uy = sin(a)
            val tx = ex + l * ux
            val ty = oy + l * uy
            line(ex - 200 * ux, oy - 200 * uy, ex + 200 * ux, oy + 200 * uy, c.inkSoft, 1.0, dash = true, alpha = 0.6f)
            force(ex, oy, tx, ty, c.load, 3.0)
            text(tx + 6, ty + (if (f.ccw) -4.0 else 14.0), " = ${fmt(f.f)} kN", c.load, 13.0, Anchor.START, sym = "F")
            angleArc(ex, oy, 26.0, 0.0, dirScreen, "α", c)
            // kóta na opačné straně, než kam vede rameno (to jde k patě kolmice pod/nad pákou)
            val dimY = if (f.ccw) oy - 34 else oy + 30
            dimLine(ox, dimY, ex, dimY, "r = ${fmt(f.r)} m", c)
            if (f.arm) {
                // pata kolmice z O na nositelku síly
                val t = (ox - ex) * ux + (oy - oy) * uy
                val px = ex + ux * t
                val py = oy + uy * t
                line(ox, oy, px, py, c.accent, 2.2, dash = true)
                rightAngleMark(Pt(px, py), Pt(ox, oy), Pt(ex, oy), c, 8.0)
                text((ox + px) / 2 - 8, (oy + py) / 2, null, c.accent, 14.0, Anchor.END, sym = "r_⊥")
            }
        }
        is Fig.Rafter -> {
            val x0 = 70.0
            val y0 = FIG_H - 40
            // délka v obrázku tak, aby se krokev vešla na šířku i na výšku
            val len = min(270.0 / max(Trig.cosD(f.alpha), 1e-3), (y0 - 22) / max(Trig.sinD(f.alpha), 1e-3))
            val ex = x0 + len * Trig.cosD(f.alpha)
            val ey = y0 - len * Trig.sinD(f.alpha)
            line(20.0, y0, 380.0, y0, c.ink, 1.2)
            line(x0, y0, ex, ey, c.ink, 9.0)
            line(x0, y0, ex, ey, c.beam, 6.0)
            text((x0 + ex) / 2 - 10, (y0 + ey) / 2 - 10, "L = ${fmt(f.len)} m", c.ink, 13.0, Anchor.END)
            angleArc(x0, y0, 30.0, 0.0, screenAngle(Pt(x0, y0), Pt(ex, ey)), "${fmt(f.alpha, 0)}°", c)
            if (f.projections) {
                // popisky bez jazyka (obrázek nezná překlad): „?“ u hledaných kót
                dimLine(x0, y0 + 16, ex, y0 + 16, "?", c, color = c.derived)
                dimLine(ex + 16, y0, ex + 16, ey, "?", c, offset = 14.0, color = c.derived)
                line(ex, ey, ex, y0, c.derived, 1.0, dash = true)
            }
        }
        is Fig.Comps -> compsFig(f, c)
        is Fig.Calc -> {
            rect(90.0, 40.0, 220.0, 120.0, c.subtle, c.ink, 2.0)
            rect(105.0, 55.0, 190.0, 80.0, c.paper, c.inkSoft, 1.0)
            if (f.mode.isNotEmpty()) text(285.0, 72.0, f.mode, if (f.mode == "DEG") c.accent else c.nudge, 11.0, Anchor.END, bold = true)
            text(115.0, 92.0, f.expr, c.ink, 15.0, Anchor.START)
            text(285.0, 124.0, f.value, c.ink, 22.0, Anchor.END, bold = true)
        }
    }
}

/** výška obrázku procvičování */
const val FIG_H = 230.0

private fun PrimBuilder.forceFig(f: Fig.Force, cx: Double, cy: Double, c: StatikaColors, withAxes: Boolean = true, verticalLabelLeft: Boolean = false) {
    if (withAxes) axes(cx, cy, 150.0, c)
    val len = 95.0
    val a = f.phi * PI / 180
    val tx = cx + len * cos(a)
    val ty = cy - len * sin(a)
    if (f.components) {
        val fx = Trig.cosD(f.phi)
        val fy = Trig.sinD(f.phi)
        line(tx, cy, tx, ty, c.derived, 1.0, dash = true)
        line(cx, ty, tx, ty, c.derived, 1.0, dash = true)
        if (abs(fx) > 0.02) force(cx, cy, cx + len * fx, cy, c.derived, 2.4)
        if (abs(fy) > 0.02) force(cx, cy, cx, cy - len * fy, c.derived, 2.4)
        // popisek F_x dál od počátku, kde je oblouk úhlu φ; u více sil s indexem (F_1x)
        val sub = f.label.substringAfter('_', "")
        if (abs(fx) > 0.02) {
            if (f.from == AngleFrom.PLUS_X) {
                // u oblouku φ kolem počátku: popisek za hrot složky
                text(cx + len * fx + (if (fx > 0) 8.0 else -8.0), cy - 8, null, c.derived, 13.0, if (fx > 0) Anchor.START else Anchor.END, sym = "F_${sub}x")
            } else text(cx + len * fx / 2, cy + (if (fy > 0) 18.0 else -8.0), null, c.derived, 13.0, sym = "F_${sub}x")
        }
        if (abs(fy) > 0.02) text(cx + (if (fx > 0) -12.0 else 12.0), cy - len * fy / 2 + 5, null, c.derived, 13.0, if (fx > 0) Anchor.END else Anchor.START, sym = "F_${sub}y")
    }
    force(cx, cy, tx, ty, c.load, 3.0)
    val lx = tx + 10 * cos(a)
    val ly = ty - 10 * sin(a)
    // popisek u hrotu: nahoru → nad, dolů → pod (ale ne pod okraj obrázku); u svislé šipky kousek vedle osy y
    val vertical = abs(cos(a)) < 0.25
    val labelX = lx + if (vertical) (if (verticalLabelLeft) -12.0 else 12.0) else 0.0
    val labelY = if (sin(a) > 0) ly - 4 else min(ly + 14, FIG_H - 6)
    // svislá šipka: popisek vedle hrotu (nad ním je popisek osy y)
    text(labelX, if (vertical) (if (sin(a) > 0) ty + 16 else min(ty + 4, FIG_H - 6)) else labelY, " = ${fmt(f.f)}" + (if (f.unit.isNotEmpty()) " ${f.unit}" else ""), c.load, 14.0, if (vertical) (if (verticalLabelLeft) Anchor.END else Anchor.START) else if (cos(a) >= 0) Anchor.START else Anchor.END, sym = f.label)
    // vyznačený úhel (0 = žádný – síla ve směru osy)
    val arrowScreen = -f.phi
    if (f.angle != 0.0) when (f.from) {
        AngleFrom.HORIZONTAL, AngleFrom.VERTICAL -> {
            val base = if (f.from == AngleFrom.HORIZONTAL) (if (cos(a) >= 0) 0.0 else 180.0) else (if (sin(a) >= 0) -90.0 else 90.0)
            angleArc(cx, cy, 34.0, base, arrowScreen, null, c)
            angleLabel(cx, cy, base, arrowScreen, "${fmt(f.angle, 0)}°", tx, ty, f.components, c)
        }
        AngleFrom.PLUS_X -> {
            // φ proti směru hodin od +x – může být víc než 180°
            prims += Prim.Arc(cx, cy, 28.0, 0.0, -f.phi, c.inkSoft, 1.4)
            val mid = -f.phi / 2 * PI / 180
            text(cx + 46 * cos(mid), cy + 46 * sin(mid) + 5, "φ = ${fmt(f.angle, 0)}°", c.inkSoft, 13.0)
        }
    }
}

/**
 * Popisek vyznačeného úhlu v ose oblouku. Když jsou dokreslené složky, čárkované čáry (svislá x = [tx], vodorovná y = [ty])
 * by popisek přeškrtly – posune se po ose úhlu tam, kde je volno.
 */
private fun PrimBuilder.angleLabel(cx: Double, cy: Double, a1: Double, a2: Double, label: String, tx: Double, ty: Double, components: Boolean, c: StatikaColors) {
    val sweep = ((a2 - a1) % 360 + 540) % 360 - 180
    if (abs(sweep) < 0.5) return
    val mid = (a1 + sweep / 2) * PI / 180
    val w = label.length * 13 * 0.58 + 4
    var r = 50.0
    if (components) {
        for (cand in listOf(50.0, 62.0, 74.0, 86.0, 98.0, 112.0, 126.0)) {
            val x = cx + cand * cos(mid)
            val y = cy + cand * sin(mid)
            // obdélník popisku vs. čárkované úsečky složek (svislá od (tx, cy) do (tx, ty), vodorovná od (cx, ty) do (tx, ty))
            val hitV = abs(x - tx) < w / 2 + 4 && y in (min(cy, ty) - 8)..(max(cy, ty) + 8)
            val hitH = abs(y - ty) < 12 && x in (min(cx, tx) - w / 2)..(max(cx, tx) + w / 2)
            val hitAxes = abs(y - cy) < 12 || abs(x - cx) < w / 2 + 4
            r = cand
            if (!hitV && !hitH && !hitAxes) break
        }
    }
    text(cx + r * cos(mid), cy + r * sin(mid) + 5, label, c.inkSoft, 13.0)
}

/** Složky (se znaménky) a z nich výslednice R; před vyřešením „R = ?“ a „α = ?“. */
private fun PrimBuilder.compsFig(f: Fig.Comps, c: StatikaColors) {
    val cx = 200.0
    val cy = FIG_H / 2 + 5
    axes(cx, cy, 150.0, c)
    val k = 85.0 / max(abs(f.fx), abs(f.fy)).coerceAtLeast(1e-9)
    val tx = cx + f.fx * k
    val ty = cy - f.fy * k
    line(tx, cy, tx, ty, c.derived, 1.0, dash = true)
    line(cx, ty, tx, ty, c.derived, 1.0, dash = true)
    force(cx, cy, tx, cy, c.derived, 2.6)
    force(cx, cy, cx, ty, c.derived, 2.6)
    text((cx + tx) / 2, cy + (if (f.fy > 0) 20.0 else -10.0), " = ${fmt(f.fx)}", c.derived, 13.0, sym = "F_x")
    text(cx + (if (f.fx > 0) -10.0 else 10.0), (cy + ty) / 2 + 5, " = ${fmt(f.fy)}", c.derived, 13.0, if (f.fx > 0) Anchor.END else Anchor.START, sym = "F_y")
    val r = kotlin.math.sqrt(f.fx * f.fx + f.fy * f.fy)
    force(cx, cy, tx, ty, c.load, 3.0, dash = !f.solved)
    text(tx + (if (f.fx > 0) 8.0 else -8.0), ty + (if (f.fy > 0) -6.0 else 16.0), " = " + (if (f.solved) "${fmt(r)} kN" else "?"), c.load, 14.0, if (f.fx > 0) Anchor.START else Anchor.END, sym = "R")
    val base = if (f.fx >= 0) 0.0 else 180.0
    val arrow = screenAngle(Pt(cx, cy), Pt(tx, ty))
    angleArc(cx, cy, 30.0, base, arrow, null, c)
    val a = Trig.atanD(abs(f.fy) / abs(f.fx).coerceAtLeast(1e-9))
    angleLabel(cx, cy, base, arrow, "α = " + (if (f.solved) "${fmt(a, 1)}°" else "?"), tx, ty, true, c)
}

/** Šikmá síla na nosník (z lekce 3) – pro karty kurzu trigonometrie. */
internal fun PrimBuilder.inclinedForce(tx: Double, ty: Double, alpha: Double, c: StatikaColors, fa: Float = 1f, ca: Float = 0f, labels: Boolean = true) =
    inclined(tx, ty, alpha, c, fa, ca, labels)
