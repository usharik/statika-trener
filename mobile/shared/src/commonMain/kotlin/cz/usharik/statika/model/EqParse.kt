/*
 * Rozbor rovnice zapsané žákem a její porovnání se správnou rovnicí.
 *
 * Rovnice se převede na lineární tvar  Σ a·(značka) + c = 0.  Značky jsou neznámé reakce
 * i známá zatížení (F_1, Q, q, M …) – díky tomu lze každý člen přiřadit konkrétní síle.
 * Správnost se posuzuje až na nenulový násobek (znaménková konvence, vynásobení rovnice).
 */
package cz.usharik.statika.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

// ---------------------------------------------------------------- značky

data class SymDef(
    /** kanonický zápis, např. R_Ax */
    val name: String,
    val isUnknown: Boolean,
    /** id neznámé (u reakcí) */
    val unknownId: String? = null,
    /** hodnota známé veličiny */
    val value: Double? = null,
    val unit: String,
    /** k čemu značka patří: id položek (pro F_1 u šikmé síly obě složky) */
    val items: List<String>,
)

private fun norm(s: String) = s.replace("_", "")

fun symbolsOf(p: Problem, items: List<Item>): List<SymDef> {
    val out = mutableListOf<SymDef>()
    for (it in items) {
        val unit = if (it.kind == Dof.M) "kNm" else "kN"
        out += if (it.unknownId != null) SymDef(it.sym, true, unknownId = it.unknownId, unit = unit, items = listOf(it.id))
        else SymDef(it.sym, false, value = it.value, unit = unit, items = listOf(it.id))
    }
    for (l in p.loads) {
        if (l is PointForce && l.alpha != null && l.alpha != 0)
            out += SymDef(l.label, false, value = l.f, unit = "kN", items = items.filter { it.loadId == l.id }.map { it.id })
        if (l is Udl) out += SymDef(l.label, false, value = l.q, unit = "kN/m", items = items.filter { it.loadId == l.id }.map { it.id })
    }
    return out
}

/** Značka podle zápisu: přesně, jinak bez ohledu na velikost písmen (je-li jednoznačná). */
fun lookupSym(defs: List<SymDef>, raw: String): SymDef? {
    val n = norm(raw)
    defs.firstOrNull { norm(it.name) == n }?.let { return it }
    val ci = defs.filter { norm(it.name).lowercase() == n.lowercase() }
    return if (ci.size == 1) ci[0] else null
}

// ---------------------------------------------------------------- lexer

sealed interface Tok {
    val s: Int
    val e: Int

    data class Num(val v: Double, override val s: Int, override val e: Int) : Tok
    data class Id(val v: String, override val s: Int, override val e: Int) : Tok
    data class Op(val v: Char, override val s: Int, override val e: Int) : Tok
}

data class Tokens(val toks: List<Tok>, val bad: Int? = null)

private val OPS = mapOf(
    '+' to '+', '-' to '-', '−' to '-', '–' to '-', '*' to '*', '·' to '*', '⋅' to '*', '×' to '*',
    '/' to '/', ':' to '/', '(' to '(', ')' to ')', '=' to '=',
)

private fun Char.isAsciiDigit() = this in '0'..'9'
private fun Char.isAsciiLetter() = this in 'A'..'Z' || this in 'a'..'z'

fun tokenize(src: String): Tokens {
    val toks = mutableListOf<Tok>()
    var i = 0
    while (i < src.length) {
        val ch = src[i]
        if (ch.isWhitespace() || ch == '\uFEFF' || ch == '°') {
            i++
            continue
        }
        // číslo: 12, 4,5, 4.5, ,5
        val numEnd = scanNumber(src, i)
        if (numEnd > i) {
            toks += Tok.Num(src.substring(i, numEnd).replace(',', '.').toDouble(), i, numEnd)
            i = numEnd
            continue
        }
        if (ch.isAsciiLetter()) {
            var j = i + 1
            while (j < src.length && (src[j].isAsciiLetter() || src[j].isAsciiDigit() || src[j] == '_')) j++
            toks += Tok.Id(src.substring(i, j), i, j)
            i = j
            continue
        }
        val op = OPS[ch]
        if (op != null) {
            toks += Tok.Op(op, i, i + 1)
            i++
            continue
        }
        return Tokens(toks, i)
    }
    return Tokens(toks)
}

private fun scanNumber(src: String, i: Int): Int {
    var j = i
    if (j < src.length && src[j].isAsciiDigit()) {
        while (j < src.length && src[j].isAsciiDigit()) j++
        if (j + 1 < src.length && (src[j] == '.' || src[j] == ',') && src[j + 1].isAsciiDigit()) {
            j++
            while (j < src.length && src[j].isAsciiDigit()) j++
        }
        return j
    }
    if (j + 1 < src.length && (src[j] == '.' || src[j] == ',') && src[j + 1].isAsciiDigit()) {
        j++
        while (j < src.length && src[j].isAsciiDigit()) j++
        return j
    }
    return i
}

// ---------------------------------------------------------------- parser

/** Lineární výraz: klíče 'u:<id neznámé>' a 'k:<značka>' + konstanta. */
data class Lin(val a: Map<String, Double>, val c: Double)

data class TopTerm(
    val lin: Lin,
    /** rozsah v textu včetně znaménka před členem */
    val s: Int,
    val e: Int,
    /** číselné činitele přímo v členu (pro přiřazení čísel k silám) */
    val nums: List<Double>,
)

sealed interface ParseError {
    val at: Int

    data class Syntax(override val at: Int) : ParseError
    data class UnknownSym(val name: String, override val at: Int) : ParseError
    data class Nonlinear(override val at: Int) : ParseError
    data class Div0(override val at: Int) : ParseError
    data class TwoEq(override val at: Int) : ParseError
    /** výsledek není číslo (odmocnina ze záporného čísla, tan 90° …) */
    data class BadValue(override val at: Int) : ParseError
}

data class Parsed(val terms: List<TopTerm>, val hasEq: Boolean, val error: ParseError? = null)

private class Fail(val err: ParseError) : Exception()

private val ZERO = Lin(emptyMap(), 0.0)
private fun isConst(l: Lin) = l.a.values.all { abs(it) < 1e-12 }
private fun scale(l: Lin, k: Double) = Lin(l.a.mapValues { it.value * k }, l.c * k)
private fun add(x: Lin, y: Lin): Lin {
    val a = LinkedHashMap(x.a)
    for ((k, v) in y.a) a[k] = (a[k] ?: 0.0) + v
    return Lin(a, x.c + y.c)
}

private val FUNCS: Map<String, (Double) -> Double> = mapOf(
    "sin" to { x -> sin(x * PI / 180) },
    "cos" to { x -> cos(x * PI / 180) },
    "tan" to { x -> tan(x * PI / 180) },
    "tg" to { x -> tan(x * PI / 180) },
    "sqrt" to { x -> sqrt(x) },
)

fun isFunctionName(s: String) = s.lowercase() in FUNCS

private class Factor(val lin: Lin, val num: Double? = null)

private class Parser(val src: String, val toks: List<Tok>, val defs: List<SymDef>) {
    var i = 0
    fun peek(): Tok? = toks.getOrNull(i)
    fun isOp(v: Char): Boolean = (peek() as? Tok.Op)?.v == v
    fun end(): Int = if (i < toks.size) toks[i].s else src.length

    fun factor(): Factor {
        val tk = peek() ?: throw Fail(ParseError.Syntax(src.length))
        if (tk is Tok.Op && (tk.v == '-' || tk.v == '+')) {
            i++
            val f = factor()
            return if (tk.v == '-') Factor(scale(f.lin, -1.0), f.num?.let { -it }) else f
        }
        if (tk is Tok.Num) {
            i++
            return Factor(Lin(emptyMap(), tk.v), tk.v)
        }
        if (tk is Tok.Id) {
            i++
            val fn = FUNCS[tk.v.lowercase()]
            if (fn != null && isOp('(')) {
                i++
                val inner = expr(null, 1.0)
                if (!isOp(')')) throw Fail(ParseError.Syntax(end()))
                i++
                if (!isConst(inner)) throw Fail(ParseError.Nonlinear(tk.s))
                val v = fn(inner.c)
                if (!v.isFinite() || kotlin.math.abs(v) > 1e12) throw Fail(ParseError.BadValue(tk.s))
                return Factor(Lin(emptyMap(), v))
            }
            val d = lookupSym(defs, tk.v) ?: throw Fail(ParseError.UnknownSym(tk.v, tk.s))
            val key = if (d.isUnknown) "u:" + d.unknownId else "k:" + d.name
            return Factor(Lin(mapOf(key to 1.0), 0.0))
        }
        if (tk is Tok.Op && tk.v == '(') {
            i++
            val inner = expr(null, 1.0)
            if (!isOp(')')) throw Fail(ParseError.Syntax(end()))
            i++
            return Factor(inner)
        }
        throw Fail(ParseError.Syntax(tk.s))
    }

    fun term(): Pair<Lin, List<Double>> {
        val first = factor()
        var lin = first.lin
        val nums = mutableListOf<Double>()
        first.num?.let { nums += abs(it) }
        while (true) {
            val tk = peek() ?: break
            val implicit = tk is Tok.Num || tk is Tok.Id || (tk is Tok.Op && tk.v == '(')
            val mulDiv = tk is Tok.Op && (tk.v == '*' || tk.v == '/')
            if (mulDiv) i++ else if (!implicit) break
            val at = tk.s
            val f = factor()
            if (tk is Tok.Op && tk.v == '/') {
                if (!isConst(f.lin)) throw Fail(ParseError.Nonlinear(at))
                if (abs(f.lin.c) < 1e-12) throw Fail(ParseError.Div0(at))
                lin = scale(lin, 1 / f.lin.c)
            } else {
                lin = when {
                    isConst(lin) -> add(scale(f.lin, lin.c), ZERO)
                    isConst(f.lin) -> scale(lin, f.lin.c)
                    else -> throw Fail(ParseError.Nonlinear(at))
                }
                f.num?.let { nums += abs(it) }
            }
        }
        return lin to nums
    }

    fun expr(top: MutableList<TopTerm>?, side: Double): Lin {
        var sum = ZERO
        var first = true
        while (true) {
            val s = end()
            var sign = 1.0
            if (isOp('+') || isOp('-')) {
                sign = if ((peek() as Tok.Op).v == '-') -1.0 else 1.0
                i++
            } else if (!first) break
            val (tl, nums) = term()
            val lin = scale(tl, sign)
            sum = add(sum, lin)
            top?.add(TopTerm(scale(lin, side), s, end(), nums))
            first = false
            if (!(isOp('+') || isOp('-'))) break
        }
        return sum
    }
}

fun parseEquation(src: String, defs: List<SymDef>): Parsed {
    val (toks, bad) = tokenize(src)
    if (bad != null) return Parsed(emptyList(), false, ParseError.Syntax(bad))
    val terms = mutableListOf<TopTerm>()
    var hasEq = false
    if (toks.isEmpty()) return Parsed(terms, hasEq)
    val ps = Parser(src, toks, defs)
    try {
        if (!ps.isOp('=')) ps.expr(terms, 1.0)
        if (ps.isOp('=')) {
            hasEq = true
            ps.i++
            if (ps.i < toks.size) ps.expr(terms, -1.0)
        }
        if (ps.isOp('=')) throw Fail(ParseError.TwoEq(ps.peek()!!.s))
        if (ps.i < toks.size) throw Fail(ParseError.Syntax(ps.peek()!!.s))
    } catch (f: Fail) {
        return Parsed(terms, hasEq, f.err)
    }
    return Parsed(terms, hasEq)
}

// ---------------------------------------------------------------- porovnání

enum class IssueKind { EXTRA, MISSING, SIGN, MAG }

data class Issue(
    val kind: IssueKind,
    val item: Item,
    /** rameno, které žák (patrně) použil */
    val impliedArm: Double? = null,
)

data class Span(val s: Int, val e: Int)

data class Analysis(
    val ok: Boolean,
    val empty: Boolean,
    /** násobek: rovnice žáka = λ · správná rovnice */
    val lambda: Double?,
    val issues: List<Issue>,
    /** čísla, která nejde přiřadit žádné síle */
    val stray: List<Double>,
    /** rozsahy těchto čísel v textu */
    val straySpans: List<Span> = emptyList(),
    /** položka → rozsahy členů v textu (pro nápovědu, která člen nahradí) */
    val spans: Map<String, List<Span>>,
    val spec: EqSpec,
)

private fun near(a: Double, b: Double, tol: Double = 0.015) = abs(a - b) <= tol * max(abs(a), abs(b)) + 1e-6

/** Pro šikmou sílu F_1: v ΣFx patří k vodorovné složce, jinde ke svislé. */
private fun pickItem(d: SymDef, eq: EqSpec, items: List<Item>): Item? {
    val cands = d.items.mapNotNull { id -> items.firstOrNull { it.id == id } }
    if (cands.size == 1) return cands[0]
    val want = if (eq == EqSpec.Fx) Dof.FX else Dof.FY
    return cands.firstOrNull { it.kind == want } ?: cands.firstOrNull()
}

fun analyze(parsed: Parsed, spec: EqSpec, items: List<Item>, defs: List<SymDef>): Analysis {
    val coef = LinkedHashMap<String, Double>()
    val spans = LinkedHashMap<String, MutableList<Span>>()
    val stray = mutableListOf<Double>()
    val straySpans = mutableListOf<Span>()
    fun addTo(id: String, v: Double, sp: Span) {
        coef[id] = (coef[id] ?: 0.0) + v
        val list = spans.getOrPut(id) { mutableListOf() }
        // člen „(F_1 + 1)·7“ přispívá k téže síle dvakrát – rozsah stačí jednou
        if (sp !in list) list += sp
    }

    for (tm in parsed.terms) {
        val sp = Span(tm.s, tm.e)
        for ((key, k) in tm.lin.a) {
            if (abs(k) < 1e-12) continue
            if (key.startsWith("u:")) {
                val it = items.first { it.unknownId == key.substring(2) }
                addTo(it.id, k, sp)
            } else {
                val d = defs.first { "k:" + it.name == key }
                val it = pickItem(d, spec, items)!!
                addTo(it.id, k * d.value!! / it.value!!, sp)
            }
        }
        // čistě číselný člen – zkusíme ho přiřadit síle podle činitele
        if (abs(tm.lin.c) > 1e-9) {
            val cands = mutableListOf<Pair<Item, Double>>()
            for (n in tm.nums) for (d in defs) {
                if (!d.isUnknown && near(n, d.value!!, 0.01)) {
                    val it = pickItem(d, spec, items)
                    if (it != null) cands += it to it.value!!
                }
            }
            // přednost má síla, u které by člen dal přesně očekávaný koeficient (tj. ostatní činitele jsou
            // její rameno – „1·12“, kde 1 je rameno a 12 síla, nesmí připadnout zatížení q = 1),
            // pak síla, která do rovnice patří a ještě nemá člen
            val c0 = tm.lin.c
            fun score(c: Pair<Item, Double>): Int {
                val want = kotlin.math.abs(coef(expected(c.first, spec)))
                val fits = want > 0 && near(kotlin.math.abs(c0 / c.second), want)
                return (if (fits) 4 else 0) + (if (coef[c.first.id].let { it != null && it != 0.0 }) 0 else 2) +
                    (if (expected(c.first, spec).sign != 0) 1 else 0)
            }
            val sorted = cands.sortedByDescending(::score)
            if (sorted.isNotEmpty()) addTo(sorted[0].first.id, tm.lin.c / sorted[0].second, sp)
            else {
                stray += tm.lin.c
                straySpans += sp
            }
        }
    }

    val exp = items.associate { it.id to coef(expected(it, spec)) }
    fun user(id: String): Double {
        val v = coef[id] ?: 0.0
        return if (abs(v) < 1e-9) 0.0 else v
    }
    val empty = items.all { user(it.id) == 0.0 } && stray.isEmpty()

    // λ: poměr, se kterým souhlasí nejvíc členů. Běžně žák píše rovnici bez násobení (λ = ±1),
    // proto ±1 vyhrává při shodě; když se neshodnou ani dva členy, bereme ±1 podle většiny znamének.
    val ratios = mutableListOf(1.0 to false, -1.0 to false)
    var signVote = 0.0
    for (it in items) {
        val u = user(it.id)
        val e = exp.getValue(it.id)
        if (u == 0.0 || e == 0.0) continue
        ratios += (u / e) to (it.unknownId != null)
        signVote += (u / e).sign
    }
    var lambda: Double? = null
    var bestScore = -1.0
    for ((r, unk) in ratios) {
        var sc = 0.0
        for (j in items) {
            val u = user(j.id)
            val e = exp.getValue(j.id)
            if (u != 0.0 && e != 0.0 && near(u, r * e)) sc += if (j.unknownId != null) 1.01 else 1.0
        }
        sc += if (abs(abs(r) - 1) < 1e-9) 0.5 else if (unk) 0.02 else 0.0
        if (sc > bestScore) {
            bestScore = sc
            lambda = r
        }
    }
    if (bestScore < 2 && ratios.size > 2) lambda = if (signVote < 0) -1.0 else 1.0
    if (ratios.size == 2) lambda = null

    val issues = mutableListOf<Issue>()
    for (it in items) {
        val u = user(it.id)
        val e = exp.getValue(it.id)
        if (u == 0.0 && e == 0.0) continue
        if (u != 0.0 && e == 0.0) issues += Issue(IssueKind.EXTRA, it)
        else if (u == 0.0) issues += Issue(IssueKind.MISSING, it)
        else if (lambda != null && !near(u, lambda * e)) {
            issues += if (u.sign != (lambda * e).sign) Issue(IssueKind.SIGN, it)
            else Issue(IssueKind.MAG, it, impliedArm = abs(u / lambda))
        }
    }
    // chybějící členy až na konec – nejdřív to, co je napsané
    val sortedIssues = issues.sortedBy { if (it.kind == IssueKind.MISSING) 1 else 0 }
    return Analysis(
        ok = !empty && sortedIssues.isEmpty() && stray.isEmpty() && lambda != null,
        empty = empty,
        lambda = lambda,
        issues = sortedIssues,
        stray = stray,
        straySpans = straySpans,
        spans = spans,
        spec = spec,
    )
}

/** Pro kloub zkusí obě části nosníku a vezme tu, která k zápisu sedí lépe. */
fun analyzeBest(parsed: Parsed, specs: List<EqSpec>, items: List<Item>, defs: List<SymDef>): Analysis {
    val res = specs.map { analyze(parsed, it, items, defs) }
    fun cost(a: Analysis) = if (a.ok) -1 else a.issues.size + a.stray.size
    return res.reduce { b, a -> if (cost(a) < cost(b)) a else b }
}

/** Text správného členu pro nápovědu, např. „- F_1y*2“. */
fun termText(it: Item, spec: EqSpec, lambdaSign: Int, first: Boolean, scale: Double = 1.0): String {
    val e = expected(it, spec)
    val sgn = e.sign * lambdaSign
    val arm = if (e.arm != null && e.arm > EPS && it.kind == Dof.FY) "*" + plain(e.arm, 3).replace('.', ',') else ""
    // rovnice vynásobená číslem (2·R_A + 2·R_B …) – vložený člen musí mít stejný násobek
    val k = if (kotlin.math.abs(scale - 1) > 1e-9) plain(scale, 3).replace('.', ',') + "*" else ""
    val body = k + it.sym + arm
    if (first) return (if (sgn < 0) "-" else "") + body
    return (if (sgn < 0) " - " else " + ") + body
}
