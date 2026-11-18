package cz.usharik.statika.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import cz.usharik.statika.i18n.Tr
import cz.usharik.statika.model.Dof
import cz.usharik.statika.model.EqSpec
import cz.usharik.statika.model.PointForce
import cz.usharik.statika.model.PointMoment
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.Side
import cz.usharik.statika.model.Udl
import cz.usharik.statika.model.allItems
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.linearize
import cz.usharik.statika.model.namedPoints
import cz.usharik.statika.model.forceComponents
import cz.usharik.statika.model.resultantSym
import cz.usharik.statika.model.unknownsOf
import cz.usharik.statika.model.unknownsOfSupport
import cz.usharik.statika.platform.PdfDoc
import cz.usharik.statika.platform.WEB_URL
import cz.usharik.statika.trainer.DrawingView
import cz.usharik.statika.trainer.Term
import cz.usharik.statika.trainer.eqTitle
import cz.usharik.statika.trainer.fullSolution
import cz.usharik.statika.trainer.unitOf
import kotlin.math.abs
import kotlin.math.min

/*
 * Úloha jako PDF (A4): zadání s výkresem, volitelně celé vzorové řešení po krocích trenažéru.
 * Stránky se kreslí přes DrawScope v bodech (1/72″) – Android je zapíše vektorově (PdfDocument),
 * iOS jako obrázek ([cz.usharik.statika.platform.rasterize]).
 */

private const val MARGIN = 48f
private const val CONTENT_W = PdfDoc.WIDTH - 2 * MARGIN
private const val FOOTER_H = 26f

private const val S = PdfDoc.SCALE

/** Měření textu pro PDF – hustota [PdfDoc.SCALE] bez ohledu na displej a systémové zvětšení písma. */
fun pdfTextMeasurer(resolver: androidx.compose.ui.text.font.FontFamily.Resolver) =
    TextMeasurer(resolver, Density(S), LayoutDirection.Ltr, cacheSize = 0)

private sealed interface Block {
    val before: Float

    class Text(
        val text: AnnotatedString,
        val style: TextStyle,
        override val before: Float,
        val indent: Float = 0f,
        val keepWithNext: Boolean = false,
    ) : Block

    class Figure(val scene: Scene, override val before: Float, val maxHeight: Float = 230f) : Block
}

private sealed interface Placed {
    class Text(val layout: TextLayoutResult, val x: Float, val y: Float) : Placed
    class Figure(val scene: Scene, val x: Float, val y: Float, val width: Float) : Placed
}

private class Doc(val measurer: TextMeasurer) {
    private val c = LightColors
    val body = TextStyle(color = c.ink, fontSize = 10.5.sp, lineHeight = 15.sp)
    val small = TextStyle(color = c.inkSoft, fontSize = 8.5.sp, lineHeight = 11.sp)
    val blocks = mutableListOf<Block>()

    fun title(text: String) {
        blocks += Block.Text(AnnotatedString(text), TextStyle(color = c.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold), 0f)
    }

    fun subtitle(text: String) {
        blocks += Block.Text(AnnotatedString(text), TextStyle(color = c.inkSoft, fontSize = 10.sp), 3f)
    }

    fun heading(text: String, big: Boolean = false) {
        val style = TextStyle(color = c.ink, fontSize = if (big) 15.sp else 12.5.sp, fontWeight = FontWeight.SemiBold)
        blocks += Block.Text(AnnotatedString(text), style, if (big) 22f else 16f, keepWithNext = true)
    }

    fun para(text: AnnotatedString, before: Float = 6f, indent: Float = 0f, keepWithNext: Boolean = false) {
        blocks += Block.Text(text, body, before, indent, keepWithNext)
    }

    fun note(text: AnnotatedString, before: Float = 4f) {
        blocks += Block.Text(text, body.copy(color = c.inkSoft, fontSize = 9.5.sp, lineHeight = 13.sp), before)
    }

    fun figure(scene: Scene) {
        blocks += Block.Figure(scene, 8f)
    }

    private fun measure(b: Block.Text): TextLayoutResult = measurer.measure(
        b.text, b.style, constraints = Constraints(maxWidth = ((CONTENT_W - b.indent) * S).toInt()),
    )

    private fun figureSize(b: Block.Figure): Pair<Float, Float> {
        val ratio = ((b.scene.bottom - b.scene.top) / b.scene.width).toFloat()
        val w = min(CONTENT_W, b.maxHeight / ratio)
        return w to w * ratio
    }

    /** Rozložení bloků na stránky; nadpis nezůstane sám na konci stránky. */
    fun paginate(): List<List<Placed>> {
        val bottom = PdfDoc.HEIGHT - MARGIN - FOOTER_H
        val heights = blocks.map { b ->
            when (b) {
                is Block.Text -> measure(b).let { it to it.size.height / S }
                is Block.Figure -> null to figureSize(b).second
            }
        }
        val pages = mutableListOf(mutableListOf<Placed>())
        var y = MARGIN
        for ((i, b) in blocks.withIndex()) {
            val h = heights[i].second
            // blok s keepWithNext se drží s celým řetězcem následujících (nadpis rovnice + její řádky)
            var need = b.before + h
            var j = i
            while ((blocks[j] as? Block.Text)?.keepWithNext == true && j + 1 < blocks.size) {
                j++
                need += blocks[j].before + heights[j].second
            }
            if (y + need > bottom && pages.last().isNotEmpty()) {
                pages += mutableListOf<Placed>()
                y = MARGIN
            }
            if (pages.last().isNotEmpty()) y += b.before
            when (b) {
                is Block.Text -> pages.last() += Placed.Text(heights[i].first!!, MARGIN + b.indent, y)
                is Block.Figure -> {
                    val (w, _) = figureSize(b)
                    pages.last() += Placed.Figure(b.scene, MARGIN + (CONTENT_W - w) / 2, y, w)
                }
            }
            y += h
        }
        return pages
    }
}

// Rozvržení je v bodech, kreslí se v jednotkách stránky (S na bod).
private fun DrawScope.drawFigure(f: Placed.Figure, measurer: TextMeasurer) {
    val k = f.width * S / f.scene.width.toFloat()
    withTransform({
        translate(f.x * S, f.y * S)
        scale(k, k, Offset.Zero)
        translate(0f, -f.scene.top.toFloat())
    }) {
        for (p in f.scene.prims) drawPrim(p, 1f, LightColors, measurer, k)
    }
}

/** Člen rovnice se značkou: „− [[F_1y]]·2“. */
private fun AnnotatedString.Builder.symbolicEq(terms: List<Term>) {
    terms.forEachIndexed { i, x ->
        append(if (x.c < 0) (if (i > 0) " − " else "−") else if (i > 0) " + " else "")
        appendSym(x.it.sym, if (x.it.unknownId != null) LightColors.react else null)
        if (x.arm != null) append("·" + fmt(abs(x.c)))
    }
    append(" = 0")
}

private fun AnnotatedString.Builder.numericEq(terms: List<Term>) {
    terms.forEachIndexed { i, x ->
        append(if (x.c < 0) (if (i > 0) " − " else "−") else if (i > 0) " + " else "")
        if (x.it.unknownId != null) appendSym(x.it.sym, LightColors.react) else append(fmt(x.it.value!!))
        if (x.arm != null) append("·" + fmt(abs(x.c)))
    }
    append(" = 0")
}

private fun AnnotatedString.Builder.bold(text: String) = withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(text) }

private fun arrowX(v: Double) = if (v > 0) "→" else "←"
private fun arrowY(v: Double) = if (v > 0) "↑" else "↓"

/** Název souboru PDF podle čísla úlohy a obsahu. */
fun problemPdfName(p: Problem, withSolution: Boolean, tr: Tr): String =
    "statika-${p.seed}-${tr(if (withSolution) "pdfFileSolution" else "pdfFileTask")}.pdf"

/** Úloha jako PDF: zadání, s [withSolution] i celé řešení (uvolnění, zatížení, rovnice, výpočet, výsledek). */
fun problemPdf(p: Problem, level: Int, withSolution: Boolean, tr: Tr, measurer: TextMeasurer): PdfDoc {
    val c = LightColors
    val d = Doc(measurer)

    // ---------- zadání ----------
    d.title(tr("problemNo") + " " + p.seed)
    d.subtitle(tr("type_${p.type.key}") + " · " + tr("level") + ": " + tr("level$level").lowercase() + " · " + tr("appTitle"))
    d.heading(tr("pdfTask"), big = true)
    d.para(rich(tr("pdfAssignment")))
    d.figure(buildScene(p, emptySet(), emptySet(), null, DrawingView(), null, false, null, c))
    d.para(
        buildAnnotatedString {
            append(tr("pdfGiven") + ": ")
            p.loads.forEachIndexed { i, l ->
                if (i > 0) append(", ")
                appendSym(l.label)
                when (l) {
                    is PointForce -> {
                        append(" = ${fmt(l.f)} kN")
                        if (l.alpha != null) append(" (α = ${l.alpha}°)")
                    }
                    is Udl -> append(" = ${fmt(l.q)} kN/m")
                    is PointMoment -> append(" = ${fmt(l.m)} kNm")
                }
            }
            append("; " + tr("lengthsNote") + ".")
        },
    )

    if (withSolution) solutionBlocks(d, p, tr)
    else d.note(AnnotatedString(tr("pdfTryApp", "n" to p.seed)), before = 14f)

    val pages = d.paginate()
    val footer = tr("appTitle") + " · " + tr("problemNo") + " " + p.seed + " · " + WEB_URL + "#" + p.seed
    val draws = pages.mapIndexed { i, placed ->
        val num = "${i + 1} / ${pages.size}"
        val fl = measurer.measure(AnnotatedString(footer), d.small)
        val nl = measurer.measure(AnnotatedString(num), d.small)
        val draw: DrawScope.() -> Unit = {
            for (it in placed) when (it) {
                is Placed.Text -> drawText(it.layout, topLeft = Offset(it.x * S, it.y * S))
                is Placed.Figure -> drawFigure(it, measurer)
            }
            val fy = PdfDoc.HEIGHT - MARGIN + 6f
            drawLine(c.line, Offset(MARGIN * S, (fy - 6f) * S), Offset((PdfDoc.WIDTH - MARGIN) * S, (fy - 6f) * S), 0.6f * S)
            drawText(fl, topLeft = Offset(MARGIN * S, fy * S))
            drawText(nl, topLeft = Offset((PdfDoc.WIDTH - MARGIN) * S - nl.size.width, fy * S))
        }
        draw
    }
    return PdfDoc(problemPdfName(p, withSolution, tr), tr("problemNo") + " " + p.seed, draws)
}

private fun solutionBlocks(d: Doc, p: Problem, tr: Tr) {
    val c = LightColors
    val sol = fullSolution(p)
    val unknowns = unknownsOf(p)

    d.heading(tr("pdfSolution"), big = true)

    // ---------- 1. uvolnění ----------
    d.heading("1. " + tr("s1Title"))
    for (s in p.supports) {
        d.para(
            buildAnnotatedString {
                bold(s.label)
                append(" — " + tr("name_${s.type.key}") + ": ")
                unknownsOfSupport(s).forEachIndexed { i, u ->
                    if (i > 0) append(", ")
                    appendSym(u.sym, c.react)
                }
            },
            before = 3f, indent = 10f,
        )
    }
    for (h in p.hinges) {
        d.para(
            buildAnnotatedString {
                bold(h.label)
                append(" — " + tr("pdfHinge") + ": ")
                appendSym("M_${h.label}")
                append(" = 0")
            },
            before = 3f, indent = 10f,
        )
    }
    d.para(rich(tr("s1Convention")))
    d.para(rich(tr("count_h2", "n" to unknowns.size, "r" to sol.eqs.size) + " " + tr("count_ok", "n" to unknowns.size)), before = 4f)

    // ---------- 2. zatížení ----------
    d.heading("2. " + tr("s2Title"))
    val tasks = p.loads.filter { it is Udl || (it is PointForce && it.alpha != null) }
    if (tasks.isEmpty()) d.para(AnnotatedString(tr("pdfNoLoads")))
    for (l in tasks) {
        when (l) {
            is Udl -> {
                val len = l.x2 - l.x1
                val q = resultantSym(l.label)
                d.para(
                    buildAnnotatedString {
                        appendSym(q)
                        append(" = ")
                        appendSym(l.label)
                        append(" · l = ${fmt(l.q)} · ${fmt(len)} = ")
                        bold("${fmt(l.q * len)} kN")
                        append(" ↓ — " + tr("pdfUdlPos", "a" to fmt(len / 2)))
                    },
                    before = 3f, indent = 10f,
                )
            }
            is PointForce -> {
                val (fx, fy) = forceComponents(l.f, l.angle)
                val a = l.alpha!!
                d.para(
                    buildAnnotatedString {
                        appendSym(l.label + "x")
                        append(" = ")
                        appendSym(l.label)
                        append(" · cos $a° = ${fmt(l.f)} · cos $a° = ")
                        bold("${fmt(abs(fx))} kN")
                        append(" " + arrowX(fx))
                    },
                    before = 3f, indent = 10f,
                )
                d.para(
                    buildAnnotatedString {
                        appendSym(l.label + "y")
                        append(" = ")
                        appendSym(l.label)
                        append(" · sin $a° = ${fmt(l.f)} · sin $a° = ")
                        bold("${fmt(abs(fy))} kN")
                        append(" " + arrowY(fy))
                    },
                    before = 2f, indent = 10f,
                )
            }
            is PointMoment -> {}
        }
    }
    val released = p.supports.map { it.id }.toSet()
    d.figure(buildScene(p, released, tasks.map { it.id }.toSet(), null, DrawingView(), null, false, null, c))

    // ---------- 3. rovnice ----------
    d.heading("3. " + tr("s3Title"))
    d.para(AnnotatedString(tr("positive") + ": → ↑ ↺"), before = 4f)
    sol.eqs.forEachIndexed { i, e ->
        d.para(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { appendRich(eqTitle(e.spec)) }
                val spec = e.spec
                if (spec is EqSpec.Hinge) {
                    append("  (" + tr("hingeSide").lowercase() + ": " + tr(if (spec.side == Side.LEFT) "left" else "right") + ")")
                }
            },
            before = 8f, keepWithNext = true,
        )
        val symbolic = buildAnnotatedString { symbolicEq(sol.terms[i]) }
        val numeric = buildAnnotatedString { numericEq(sol.terms[i]) }
        // bez zatížení (např. ΣFx = 0 jen s reakcí) by byl číselný řádek stejný jako značkový
        val both = numeric.text != symbolic.text
        d.para(symbolic, before = 2f, indent = 10f, keepWithNext = both)
        if (both) d.para(numeric, before = 1f, indent = 10f)
    }

    // ---------- 4. výpočet ----------
    d.heading("4. " + tr("s4Title"))
    for (line in sol.lines) {
        d.para(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.inkSoft)) {
                    append(tr("pdfFrom") + " ")
                    appendRich(eqTitle(sol.eqs[line.eq].spec))
                    append(":  ")
                }
                appendSym(line.u.sym, c.react)
                append(" = ")
                if (line.expr != fmt(line.value)) append(line.expr + " = ")
                bold(fmt(line.value) + " " + unitOf(line.u))
            },
            before = 4f,
        )
    }
    if (sol.coupled.isNotEmpty()) {
        d.para(
            buildAnnotatedString {
                append(tr("pdfCoupled") + " ")
                sol.coupled.forEachIndexed { i, u ->
                    if (i > 0) append(", ")
                    appendSym(u.sym, c.react)
                    append(" = ")
                    bold(fmt(sol.values.getValue(u.id)) + " " + unitOf(u))
                }
            },
            before = 4f,
        )
    }

    // ---------- 5. výsledek ----------
    d.heading("5. " + tr("pdfResult"))
    d.para(
        buildAnnotatedString {
            unknowns.forEachIndexed { i, u ->
                if (i > 0) append(",   ")
                appendSym(u.sym, c.react)
                append(" = ")
                bold(fmt(sol.values.getValue(u.id)) + " " + unitOf(u))
            }
        },
    )
    for (u in unknowns) {
        val v = sol.values.getValue(u.id)
        if (v < 0 && fmt(v) != "0") d.note(rich(tr(if (u.kind == Dof.M) "v_negM" else "v_neg", "u" to u.sym)))
    }
    d.figure(buildScene(p, released, emptySet(), null, DrawingView(), sol.values, false, null, c))
    // kontrola k bodu, ke kterému v řešení ještě žádná momentová podmínka nebyla
    val usedPoints = sol.eqs.mapNotNull { (it.spec as? EqSpec.M)?.point }.toSet()
    val point = namedPoints(p).firstOrNull { it.label !in usedPoints } ?: namedPoints(p).last()
    val lin = linearize(EqSpec.M(point.label, point.x), allItems(p))
    val residual = lin.a.entries.fold(lin.c) { acc, (k, v) -> acc + v * sol.values.getValue(k) }
    d.note(
        buildAnnotatedString {
            append(tr("s5Check") + ": Σ")
            appendSym("M_${point.label}")
            append(" = ${fmt(residual, 3)} ≈ 0 ✓")
        },
    )
}
