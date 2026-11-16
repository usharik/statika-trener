package cz.usharik.statika.trainer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import cz.usharik.statika.i18n.Tr
import cz.usharik.statika.model.Analysis
import cz.usharik.statika.model.Dof
import cz.usharik.statika.model.EPS
import cz.usharik.statika.model.EqSpec
import cz.usharik.statika.model.Issue
import cz.usharik.statika.model.IssueKind
import cz.usharik.statika.model.Item
import cz.usharik.statika.model.ParseError
import cz.usharik.statika.model.PointForce
import cz.usharik.statika.model.PointMoment
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.Side
import cz.usharik.statika.model.SymDef
import cz.usharik.statika.model.Udl
import cz.usharik.statika.model.allItems
import cz.usharik.statika.model.analyze
import cz.usharik.statika.model.analyzeBest
import cz.usharik.statika.model.close
import cz.usharik.statika.model.forceComponents
import cz.usharik.statika.model.expected
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.isMoment
import cz.usharik.statika.model.momentPoint
import cz.usharik.statika.model.namedPoints
import cz.usharik.statika.model.parseEquation
import cz.usharik.statika.model.preferredMomentPoint
import cz.usharik.statika.model.preferredSide
import cz.usharik.statika.model.symbolsOf
import cz.usharik.statika.model.termText
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

data class EqRow(
    val spec: EqSpec,
    val value: TextFieldValue = TextFieldValue(""),
    val done: Boolean = false,
    val lambda: Double? = null,
    val msgs: List<Msg> = emptyList(),
    val hintLevel: Int = 0,
) {
    val text: String get() = value.text
}

/** Název rovnice s tokeny značek, např. „Σ[[M_A]] = 0“. */
fun eqTitle(e: EqSpec): String = when (e) {
    EqSpec.Fx -> "Σ[[F_x]] = 0"
    EqSpec.Fy -> "Σ[[F_y]] = 0"
    is EqSpec.M -> "Σ[[M_${e.point}]] = 0"
    is EqSpec.Hinge -> "[[M_${e.point}]] = 0"
}

/** Krok 3: rovnice rovnováhy psané jako text, rozbor a nápovědy ke konkrétním členům. */
class EquationsStep(private val p: Problem, private val session: TrainerSession) {
    val items: List<Item> = allItems(p)
    val defs: List<SymDef> = symbolsOf(p, items)
    val points = namedPoints(p)
    private val best = preferredMomentPoint(p)

    val rows = mutableStateListOf<EqRow>().apply {
        val specs = listOf(EqSpec.Fx, EqSpec.Fy, EqSpec.M(best.label, best.x)) +
            p.hinges.map { EqSpec.Hinge(it.id, it.label, it.x, preferredSide(p, it.x)) }
        addAll(specs.map { EqRow(it) })
    }
    var active by mutableIntStateOf(0)
        private set

    val unknownDefs: List<SymDef> = defs.filter { it.isUnknown }
    val knownDefs: List<SymDef> = defs.filter { !it.isUnknown }.sortedWith { a, b -> naturalCompare(a.name, b.name) }
    val example: String = exampleOf(defs)
    val allDone: Boolean get() = rows.all { it.done }

    init {
        show(0)
    }

    private fun show(i: Int, arm: ArmView? = null) {
        val r = rows[i]
        session.view = DrawingView(
            momentPoint = r.spec.momentPoint,
            side = (r.spec as? EqSpec.Hinge)?.takeIf { r.done }?.let { SideView(it.x, it.side) },
            arm = arm,
        )
    }

    private fun patch(i: Int, f: (EqRow) -> EqRow) {
        val before = rows[i]
        var after = f(before)
        // po úpravě textu už staré hodnocení neplatí (nápověda zůstává)
        if (after.text != before.text && !after.done && after.msgs === before.msgs) {
            after = after.copy(msgs = after.msgs.filter { it.tone == Tone.HINT })
            session.highlight = null
        }
        rows[i] = after
    }

    fun focusRow(i: Int) {
        if (i != active) {
            active = i
            show(i)
        }
    }

    /** Změna textu z klávesnice (nebo jen posun kurzoru). */
    fun onValueChange(i: Int, v: TextFieldValue) {
        focusRow(i)
        patch(i) { r -> if (r.text == v.text) r.copy(value = v) else r.copy(value = v, done = false) }
    }

    /** Vložení textu z palety na místo kurzoru v aktivním poli. */
    fun insert(snippet0: String) {
        val r = rows[active]
        val s = min(r.value.selection.min, r.text.length)
        val e = min(r.value.selection.max, r.text.length)
        var snippet = snippet0
        // značka vložená hned za písmeno/číslo by s ním splynula (F + R_A → FR_A) – oddělíme mezerou
        val before = r.text.substring(0, s)
        if (snippet.first().isLetter() && before.isNotEmpty() && (before.last().isLetterOrDigit() || before.last() == '_' || before.last() == ')')) snippet = " $snippet"
        val text = before + snippet + r.text.substring(e)
        patch(active) { it.copy(value = TextFieldValue(text, TextRange(s + snippet.length)), done = false) }
    }

    /** ⌫ z palety: smaže výběr, jinak znak před kurzorem (značku nebo „sin(“ celou). */
    fun backspace() {
        val r = rows[active]
        val t = r.text
        var s = min(r.value.selection.min, t.length)
        val e = min(r.value.selection.max, t.length)
        if (s == e) {
            while (s > 0 && t[s - 1] == ' ') s--
            val tok = TOKEN_TAIL.find(t.substring(0, s))
            s = if (tok != null) s - tok.value.length else maxOf(0, s - 1)
            while (s > 0 && t[s - 1] == ' ') s--
        }
        patch(active) { it.copy(value = TextFieldValue(t.substring(0, s) + t.substring(e), TextRange(s)), done = false) }
    }

    fun clear() = patch(active) { it.copy(value = TextFieldValue(""), done = false) }

    /**
     * Pro kloub obě části nosníku; zvolená (doporučená) část je první, takže při stejně dobré shodě vyhraje ona.
     * (Kdyby vyhrávala vždy levá, nápověda by vložila člen pravé části, kontrola by přepnula na levou
     * a další nápověda by ho zase smazala – dokola.)
     */
    private fun specsFor(spec: EqSpec): List<EqSpec> =
        if (spec is EqSpec.Hinge) listOf(spec, spec.copy(side = if (spec.side == Side.LEFT) Side.RIGHT else Side.LEFT)) else listOf(spec)

    private fun errorMsg(e: ParseError, text: String): (Tr) -> String {
        val ex = "[[${defs.first { it.isUnknown }.name}]]"
        return when (e) {
            is ParseError.UnknownSym -> {
                // jako příklad nejpodobnější značka (překlep MB → M_A), ne první v paletě
                val near = "[[${closestSym(e.name, defs).name}]]"
                return { tr -> tr("p_unknownSym", "s" to e.name, "ex" to near) }
            }
            is ParseError.Nonlinear -> { tr -> tr("p_nonlinear") }
            is ParseError.Div0 -> { tr -> tr("p_div0") }
            is ParseError.TwoEq -> { tr -> tr("p_twoEq") }
            is ParseError.BadValue -> { tr -> tr("p_badValue") }
            is ParseError.Syntax -> {
                // chyba až na konci = visící znaménko nebo neuzavřená závorka
                if (e.at >= text.trimEnd().length) return { tr -> tr("p_dangling") }
                val at = text.substring(min(e.at, text.length), min(e.at + 8, text.length)).ifEmpty { "…" }
                return { tr -> tr("p_syntax", "at" to at) }
            }
        }
    }

    private class Run(val a: Analysis? = null, val err: Msg? = null)

    private fun run(r: EqRow): Run {
        val parsed = parseEquation(r.text, defs)
        parsed.error?.let { return Run(err = Msg(Tone.NUDGE, errorMsg(it, r.text))) }
        val a = if (r.spec is EqSpec.Hinge) analyzeBest(parsed, specsFor(r.spec), items, defs) else analyze(parsed, r.spec, items, defs)
        return Run(a = a)
    }

    private fun loadOf(it: Item) = p.loads.firstOrNull { l -> l.id == it.loadId }

    /** text zprávy, zda ukázat rameno ve výkresu, a odkaz do výkladu (lekce, karta) */
    private class IssueText(val text: (Tr) -> String, val arm: Boolean = false, val learn: Pair<Int, Int>? = null)

    private fun issueText(issue: Issue, a: Analysis): IssueText {
        val it = issue.item
        val s = it.sym
        val spec = a.spec
        val pt = spec.momentPoint?.first ?: ""
        val pos = (a.lambda ?: 1.0) > 0
        when (issue.kind) {
            IssueKind.EXTRA -> {
                val e = expected(it, spec)
                if (e.excluded && spec is EqSpec.Hinge) {
                    val other = if (spec.side == Side.LEFT) "rightPart" else "leftPart"
                    return IssueText({ tr -> tr("r_otherSide", "s" to s, "P" to pt, "side" to tr(other)) }, learn = 9 to 1)
                }
                if (spec == EqSpec.Fx) return IssueText({ tr -> tr(if (it.kind == Dof.M) "r_coupleInF" else "r_vertInX", "s" to s) }, learn = 2 to 3)
                if (spec == EqSpec.Fy) return IssueText({ tr -> tr(if (it.kind == Dof.M) "r_coupleInF" else "r_horInY", "s" to s) }, learn = 2 to 3)
                return IssueText({ tr -> tr(if (it.kind == Dof.FX) "r_horInM" else "r_atPoint", "s" to s, "P" to pt) }, learn = 4 to 2)
            }
            IssueKind.MISSING -> {
                if (spec == EqSpec.Fx) return IssueText({ tr -> tr("r_forgotX", "s" to s) }, learn = 2 to 3)
                if (spec == EqSpec.Fy) return IssueText({ tr -> tr("r_forgotY", "s" to s) }, learn = 2 to 3)
                if (it.kind == Dof.M) return IssueText({ tr -> tr("r_forgotCouple", "s" to s) }, learn = 4 to 4)
                return IssueText({ tr -> tr("r_forgotM", "s" to s, "P" to pt) }, arm = true, learn = 4 to 1)
            }
            IssueKind.SIGN -> {
                if (!spec.isMoment) {
                    val dir = if (spec == EqSpec.Fx) (if (pos) "→" else "←") else if (pos) "↑" else "↓"
                    return IssueText({ tr -> tr("p_signF", "s" to s, "pos" to dir) }, learn = 2 to 3)
                }
                return IssueText({ tr -> tr("p_signM", "s" to s, "P" to pt, "pos" to if (pos) "↺" else "↻") }, learn = 4 to 3)
            }
            IssueKind.MAG -> {
                // nesedí velikost
                if (!spec.isMoment) {
                    val l = loadOf(it)
                    if (l is Udl) return IssueText({ tr -> tr("p_qInsteadQ", "s" to s) }, learn = 8 to 1)
                    // celá šikmá síla místo její složky (F_1 místo F_1x)
                    if (l is PointForce && l.alpha != null && issue.impliedArm != null && it.value != null &&
                        close(issue.impliedArm, l.f / it.value)
                    ) return IssueText({ tr -> tr("p_useComp", "s" to s, "F" to l.label) }, learn = 3 to 1)
                    val derived = l != null && l !is PointMoment && it.sym != l.label
                    // u složky šikmé síly: zaměněná složka → trigonometrie (sin/cos), kalkulačka v RAD → režimy kalkulačky
                    val compLink = if (derived && l is PointForce && l.alpha != null && issue.impliedArm != null && it.value != null) {
                        val typed = issue.impliedArm * it.value
                        val comps = forceComponents(l.f, l.angle)
                        val other = if (it.kind == Dof.FX) abs(comps.fy) else abs(comps.fx)
                        when {
                            close(typed, other) -> 102 to 1
                            calcModeMistakes(l.f, l.alpha.toDouble()).any { m -> close(typed, m) && !close(m, abs(it.value)) } -> 103 to 1
                            else -> null
                        }
                    } else null
                    return IssueText({ tr -> tr(if (derived) "p_compMag" else "p_forceMag", "s" to s) }, learn = compLink ?: (3 to 3))
                }
                if (it.kind == Dof.M) return IssueText({ tr -> tr("p_coupleMag", "s" to s) }, learn = 4 to 4)
                if (spec.momentPoint!!.second > EPS && issue.impliedArm != null && close(issue.impliedArm, it.x)) {
                    return IssueText({ tr -> tr("r_armFromEnd", "s" to s, "P" to pt) }, arm = true, learn = 4 to 1)
                }
                if (loadOf(it) is Udl) return IssueText({ tr -> tr("r_armQ", "s" to s, "P" to pt) }, arm = true, learn = 8 to 0)
                return IssueText({ tr -> tr("r_arm", "s" to s, "P" to pt) }, arm = true, learn = 4 to 1)
            }
        }
    }

    private fun strayFromCalcMode(n: Double): Boolean = p.loads.filterIsInstance<PointForce>().any { l ->
        val al = l.alpha ?: return@any false
        val c = forceComponents(l.f, l.angle)
        calcModeMistakes(l.f, al.toDouble()).any { m -> close(abs(n), m) && !close(m, abs(c.fx)) && !close(m, abs(c.fy)) }
    }

    private fun strayMsg(n: Double): (Tr) -> String {
        val ex = defs.firstOrNull { !it.isUnknown }?.name ?: "F_1"
        return { tr -> tr("p_number", "n" to fmt(abs(n)), "ex" to ex) }
    }

    fun check(i: Int, quiet: Boolean = false) {
        if (!quiet) focusRow(i)
        val r = rows[i]
        if (r.text.isBlank()) {
            if (!quiet) patch(i) { it.copy(msgs = listOf(Msg(Tone.NUDGE) { tr -> tr("p_empty") })) }
            return
        }
        val res = run(r)
        res.err?.let { err -> return patch(i) { it.copy(msgs = listOf(err), done = false) } }
        val a = res.a ?: return
        if (a.ok) {
            val side = (a.spec as? EqSpec.Hinge)?.side
            patch(i) {
                it.copy(
                    spec = a.spec, done = true, lambda = a.lambda,
                    msgs = listOf(Msg(Tone.OK) { tr -> tr("eqDone") + (side?.let { s -> " (" + tr("p_sideFound", "side" to tr(s.key)) + ")" } ?: "") }),
                )
            }
            show(i)
            session.highlight = null
            // na telefonu rovnou k další nehotové rovnici
            if (i == active && !quiet) {
                val next = (rows.indices.drop(i + 1) + rows.indices.take(i)).firstOrNull { !rows[it].done }
                if (next != null) focusRow(next)
            }
            return
        }
        val msgs = mutableListOf<Msg>()
        if (a.empty) msgs += Msg(Tone.NUDGE) { tr -> tr("p_empty") }
        for (issue in a.issues.take(2)) {
            val it = issueText(issue, a)
            val text = it.text
            val sym = issue.item.sym
            // každá zpráva musí říct, o kterou sílu jde
            val m = Msg(Tone.NUDGE) { tr ->
                val t = text(tr)
                if (t.contains("[[$sym]]")) t else tr("e_hRow", "s" to sym) + " " + t.decap()
            }
            it.learn?.let { (l, c) -> m.learn(l, c) }
            msgs += m
        }
        if (a.stray.isNotEmpty() && msgs.size < 3) {
            val m = Msg(Tone.NUDGE, strayMsg(a.stray[0]))
            // číslo, které vyjde ze složky šikmé síly s kalkulačkou v RAD/GRAD → výklad o režimech kalkulačky
            if (strayFromCalcMode(a.stray[0])) m.learn(103, 1)
            msgs += m
        }
        patch(i) { it.copy(msgs = msgs, done = false) }
    }

    fun checkAll() {
        for (i in rows.indices) if (!rows[i].done) check(i, quiet = true)
        // paleta nesmí dál psát do právě hotové rovnice
        if (rows[active].done) rows.indexOfFirst { !it.done }.takeIf { it >= 0 }?.let(::focusRow)
    }

    fun hint() {
        session.hint()
        val i = if (!rows[active].done) active else rows.indexOfFirst { !it.done }
        if (i < 0) return
        val r = rows[i]
        val lvl = r.hintLevel + 1
        val spec = r.spec
        val pt = spec.momentPoint?.first ?: ""
        var msg: (Tr) -> String
        var text = r.text
        var arm: ArmView? = null
        val perr = parseEquation(r.text, defs).error
        val a = if (perr == null && r.text.isNotBlank()) run(r).a else null
        val aSpec = a?.spec ?: spec
        val first = a?.issues?.firstOrNull()
        if (lvl == 1) {
            val k = when (spec) {
                EqSpec.Fx -> "e_h1_fx"
                EqSpec.Fy -> "e_h1_fy"
                is EqSpec.M -> "e_h1_m"
                is EqSpec.Hinge -> "e_h1_hinge"
            }
            val sideKey = if (spec is EqSpec.Hinge) preferredSide(p, spec.x).key else null
            msg = { tr ->
                tr(k, "P" to pt, "best" to best.label, "side" to (sideKey?.let { tr(it) } ?: "")) +
                    (if (spec is EqSpec.Hinge) " " + tr("p_hingeInfo", "P" to pt) else "")
            }
        } else if (perr != null) {
            msg = errorMsg(perr, r.text)
            // od 3. úrovně neznámou značku rovnou nahradíme nejpodobnější z palety, jiný zápis opravíme
            if (lvl >= 3 && perr is ParseError.UnknownSym) {
                val d = closestSym(perr.name, defs)
                text = text.substring(0, perr.at) + d.name + text.substring(perr.at + perr.name.length)
                msg = { tr -> tr("p_replaced", "from" to perr.name, "to" to d.name) }
            } else if (lvl >= 3) {
                text = repair(text, perr)
                val shown = text
                msg = { tr -> tr("p_cut", "text" to shown) }
            }
        } else if (lvl == 2 && a != null && (first != null || a.stray.isNotEmpty())) {
            if (first != null) {
                val it = issueText(first, a)
                val sym = first.item.sym
                msg = { tr -> tr("e_hRow", "s" to sym) + " " + it.text(tr).decap() }
                val mp = aSpec.momentPoint
                if (it.arm && mp != null) arm = ArmView(mp.second, first.item.x)
                session.highlight = first.item.id
            } else {
                msg = strayMsg(a.stray[0])
            }
        } else {
            // vložit nebo opravit jeden člen rovnice
            val target: Item? = first?.item
                ?: items.sortedBy { if (it.unknownId != null) 0 else 1 }
                    .firstOrNull { expected(it, aSpec).sign != 0 && a?.spans?.get(it.id) == null }
            if (target == null) {
                val sp = a?.straySpans?.firstOrNull()
                if (sp != null) {
                    // číslo, které nepatří k žádné síle, odstraníme
                    val term = text.substring(sp.s, minOf(sp.e, text.length)).trim()
                    text = (text.substring(0, sp.s) + text.substring(minOf(sp.e, text.length)))
                        .replace(LEADING_PLUS, "").replace(SPACES, " ").replace(TRAILING_EQ, "= 0").trim()
                    msg = { tr -> tr("p_removed", "term" to term) }
                } else msg = { tr -> tr("e_hAllOk") }
            } else {
                val old = (a?.spans?.get(target.id) ?: emptyList()).distinct()
                val replaced = old.isNotEmpty()
                for (sp in old.sortedByDescending { it.s }) {
                    val e = minOf(sp.e, text.length)
                    val st = minOf(sp.s, e)
                    text = text.substring(0, st) + text.substring(e)
                }
                val extra = first?.kind == IssueKind.EXTRA
                var term = ""
                if (!extra) {
                    val eqAt = text.indexOf('=')
                    val lhs = if (eqAt >= 0) text.substring(0, eqAt) else text
                    val rest = if (eqAt >= 0) text.substring(eqAt) else ""
                    val ls = (a?.lambda ?: 1.0).sign.toInt().let { if (it == 0) 1 else it }
                    val scale = a?.lambda?.let { kotlin.math.abs(it) }?.takeIf { it > 1e-9 } ?: 1.0
                    term = termText(target, aSpec, ls, lhs.isBlank(), scale)
                    text = lhs.trimEnd() + term + (if (rest.isNotEmpty()) " " + rest.trim() else "")
                }
                text = text.replace(LEADING_PLUS, "").replace(SPACES, " ").replace(TRAILING_EQ, "= 0").trim()
                val shown = term.trim()
                val sym = target.sym
                // pravdivě: člen přibyl, byl opraven, nebo odstraněn
                msg = when {
                    extra -> { tr -> tr("p_removedSym", "s" to sym) }
                    replaced -> { tr -> tr("p_fixedTerm", "s" to sym, "term" to shown) }
                    else -> { tr -> tr("p_insert", "term" to shown) }
                }
                session.highlight = target.id
                val mp = aSpec.momentPoint
                if (aSpec.isMoment && target.kind == Dof.FY && mp != null) arm = ArmView(mp.second, target.x)
            }
        }
        val finalMsg = msg
        patch(i) {
            it.copy(
                hintLevel = lvl,
                value = if (text == it.text) it.value else TextFieldValue(text, TextRange(text.length)),
                done = false,
                msgs = it.msgs.filter { m -> m.tone != Tone.HINT } + Msg(Tone.HINT, finalMsg),
            )
        }
        active = i
        show(i, arm)
    }

    fun setPoint(i: Int, label: String, x: Double) {
        patch(i) { it.copy(spec = EqSpec.M(label, x), done = false, msgs = emptyList()) }
        active = i
        show(i)
    }

    fun done() = session.equationsDone(rows.map { UserEq(it.spec, (it.lambda ?: 1.0) < 0) })

    /** Oprava zápisu, kterému parser nerozumí: zkrátí ho k místu chyby, odstraní visící znaménka a doplní závorky. */
    private fun repair(src: String, err0: ParseError): String {
        var text = src
        var err: ParseError? = err0
        var n = 0
        while (err != null && err !is ParseError.UnknownSym && n++ < 4) {
            if (err.at < text.trimEnd().length) text = text.substring(0, err.at)
            text = text.replace(DANGLING, "")
            val open = text.count { it == '(' } - text.count { it == ')' }
            if (open > 0) text += ")".repeat(open)
            err = parseEquation(text, defs).error
        }
        return text.trim()
    }

    private companion object {
        val DANGLING = Regex("[\\s+\\-−–*·⋅×/:(=]+$")
        val TOKEN_TAIL = Regex("[A-Za-z][A-Za-z0-9_]*\\(?$")
        val LEADING_PLUS = Regex("^\\s*\\+\\s*")
        val SPACES = Regex("\\s+")
        val TRAILING_EQ = Regex("=\\s*$")
    }
}

/** „Plečo …“ → „plečo …“ za dvojtečkou (ne u značek a zkratek). */
private fun String.decap(): String =
    if (length > 1 && this[0].isUpperCase() && this[1].isLowerCase()) this[0].lowercase() + substring(1) else this

private fun lev(a: String, b: String): Int {
    val d = Array(a.length + 1) { i -> IntArray(b.length + 1) { j -> if (i == 0) j else if (j == 0) i else 0 } }
    for (i in 1..a.length) for (j in 1..b.length) {
        d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
    }
    return d[a.length][b.length]
}

/** Nejpodobnější značka z palety (pro opravu překlepu). */
fun closestSym(raw: String, defs: List<SymDef>): SymDef {
    fun n(s: String) = s.replace("_", "").lowercase()
    return defs.reduce { b, d -> if (lev(n(raw), n(d.name)) < lev(n(raw), n(b.name))) d else b }
}

fun exampleOf(defs: List<SymDef>): String {
    val u = defs.firstOrNull { it.isUnknown && !it.name.endsWith("x") } ?: defs[0]
    val k = defs.firstOrNull { !it.isUnknown && !it.name.startsWith("q") }
    return "${u.name.replaceFirst("_", "")}*4" + (k?.let { " - ${it.name.replaceFirst("_", "")}*1,5" } ?: "") + " = 0"
}

/** Řazení jako localeCompare(…, { numeric: true }): F_2 před F_10, malá písmena před velkými. */
fun naturalCompare(a: String, b: String): Int {
    val ra = Regex("\\d+|\\D+").findAll(a).map { it.value }.toList()
    val rb = Regex("\\d+|\\D+").findAll(b).map { it.value }.toList()
    for (k in 0 until minOf(ra.size, rb.size)) {
        val x = ra[k]
        val y = rb[k]
        val c = if (x[0].isDigit() && y[0].isDigit()) x.toLong().compareTo(y.toLong()) else x.lowercase().compareTo(y.lowercase())
        if (c != 0) return c
    }
    if (ra.size != rb.size) return ra.size - rb.size
    return -a.compareTo(b)
}
