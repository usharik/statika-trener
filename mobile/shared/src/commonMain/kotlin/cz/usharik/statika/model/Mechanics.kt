package cz.usharik.statika.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

const val EPS = 1e-7

enum class Dof { FX, FY, M }

/** Neznámá reakce. Kladný (předpokládaný) smysl: →, ↑, ↺. */
data class Unknown(val id: String, val sym: String, val kind: Dof, val x: Double, val supportId: String)

/** Jedna „položka“ v rovnicích rovnováhy – složka síly, výslednice, moment nebo reakce. */
data class Item(
    val id: String,
    val sym: String,
    val kind: Dof,
    val x: Double,
    /** smysl vzhledem ke kladné ose (+1 = →, ↑, ↺) */
    val dir: Int,
    /** velikost (jen známé zatížení) */
    val value: Double? = null,
    val unknownId: String? = null,
    val isReaction: Boolean,
    val loadId: String? = null,
)

enum class Side(val key: String) { LEFT("left"), RIGHT("right") }

sealed interface EqSpec {
    data object Fx : EqSpec
    data object Fy : EqSpec
    data class M(val point: String, val x: Double) : EqSpec
    data class Hinge(val hingeId: String, val point: String, val x: Double, val side: Side) : EqSpec
}

val EqSpec.isMoment: Boolean get() = this is EqSpec.M || this is EqSpec.Hinge

/** Bod momentové podmínky (jen momentové rovnice). */
val EqSpec.momentPoint: Pair<String, Double>?
    get() = when (this) {
        is EqSpec.M -> point to x
        is EqSpec.Hinge -> point to x
        else -> null
    }

data class Expect(
    val sign: Int,
    /** rameno (null = bez ramene – silová rovnice nebo dvojice) */
    val arm: Double?,
    /** položka do rovnice vůbec nepatří (leží na druhé části nosníku) */
    val excluded: Boolean = false,
)

enum class ReactionSet(val key: String) { XY("xy"), Y("y"), X("x"), XYM("xym") }

fun reactionSetOf(t: SupportType): ReactionSet = when (t) {
    SupportType.PIN -> ReactionSet.XY
    SupportType.ROLLER -> ReactionSet.Y
    SupportType.FIXED -> ReactionSet.XYM
}

fun unknownsOfSupport(s: Support): List<Unknown> {
    if (s.type == SupportType.ROLLER) return listOf(Unknown(s.id + "y", "R_${s.label}", Dof.FY, s.x, s.id))
    val u = mutableListOf(
        Unknown(s.id + "x", "R_${s.label}x", Dof.FX, s.x, s.id),
        Unknown(s.id + "y", "R_${s.label}y", Dof.FY, s.x, s.id),
    )
    if (s.type == SupportType.FIXED) u += Unknown(s.id + "m", "M_${s.label}", Dof.M, s.x, s.id)
    return u
}

fun unknownsOf(p: Problem): List<Unknown> = p.supports.flatMap(::unknownsOfSupport)

fun reactionItems(p: Problem): List<Item> = unknownsOf(p).map {
    Item(id = "r_" + it.id, sym = it.sym, kind = it.kind, x = it.x, dir = 1, unknownId = it.id, isReaction = true)
}

data class ForceComponents(val fx: Double, val fy: Double)

/**
 * cos a sin úhlů, které generátor používá, s přesně stejnými bity jako Math.cos/Math.sin ve V8 (web).
 * Knihovny jednotlivých platforem (JVM, ART, iOS) se mohou lišit v posledním bitu – a validace úlohy
 * (prahy 0,05 a 400) by pak u několika čísel úloh vybrala jinou úlohu než web.
 */
private val TRIG: Map<Int, Pair<Double, Double>> = mapOf(
    90 to (0x3c91a62633145c07UL to 0x3ff0000000000000UL),
    210 to (0xbfebb67ae8584caaUL to 0xbfe0000000000001UL),
    225 to (0xbfe6a09e667f3bceUL to 0xbfe6a09e667f3bccUL),
    240 to (0xbfe0000000000004UL to 0xbfebb67ae8584ca9UL),
    270 to (0xbcaa79394c9e8a0aUL to 0xbff0000000000000UL),
    300 to (0x3fe0000000000001UL to 0xbfebb67ae8584caaUL),
    315 to (0x3fe6a09e667f3bcbUL to 0xbfe6a09e667f3bceUL),
    330 to (0x3febb67ae8584ca8UL to 0xbfe0000000000004UL),
).mapValues { (_, v) -> Double.fromBits(v.first.toLong()) to Double.fromBits(v.second.toLong()) }

fun forceComponents(f: Double, angle: Double): ForceComponents {
    val r = angle * PI / 180
    val t = if (angle == kotlin.math.floor(angle)) TRIG[angle.toInt()] else null
    var fx = f * (t?.first ?: cos(r))
    var fy = f * (t?.second ?: sin(r))
    if (abs(fx) < EPS) fx = 0.0
    if (abs(fy) < EPS) fy = 0.0
    return ForceComponents(fx, fy)
}

private fun sgn(v: Double) = if (v >= 0) 1 else -1

/** Q z q (výslednice spojitého zatížení). */
fun resultantSym(label: String): String = if (label.startsWith("q")) "Q" + label.substring(1) else label

fun loadItems(p: Problem): List<Item> {
    val out = mutableListOf<Item>()
    for (l in p.loads) {
        when (l) {
            is PointForce -> {
                val (fx, fy) = forceComponents(l.f, l.angle)
                val both = fx != 0.0 && fy != 0.0
                if (fx != 0.0) out += Item(l.id + "x", if (both) l.label + "x" else l.label, Dof.FX, l.x, sgn(fx), abs(fx), isReaction = false, loadId = l.id)
                if (fy != 0.0) out += Item(l.id + "y", if (both) l.label + "y" else l.label, Dof.FY, l.x, sgn(fy), abs(fy), isReaction = false, loadId = l.id)
            }
            is Udl -> out += Item(l.id + "Q", resultantSym(l.label), Dof.FY, (l.x1 + l.x2) / 2, -1, l.q * (l.x2 - l.x1), isReaction = false, loadId = l.id)
            is PointMoment -> out += Item(l.id, l.label, Dof.M, l.x, if (l.ccw) 1 else -1, l.m, isReaction = false, loadId = l.id)
        }
    }
    return out
}

fun allItems(p: Problem): List<Item> = reactionItems(p) + loadItems(p)

/** Očekávaný příspěvek položky do rovnice (kladný smysl momentů ↺). */
fun expected(item: Item, eq: EqSpec): Expect {
    when (eq) {
        EqSpec.Fx -> return Expect(if (item.kind == Dof.FX) item.dir else 0, null)
        EqSpec.Fy -> return Expect(if (item.kind == Dof.FY) item.dir else 0, null)
        is EqSpec.Hinge -> {
            val onSide = if (eq.side == Side.LEFT) item.x < eq.x - EPS else item.x > eq.x + EPS
            if (!onSide) return Expect(0, null, excluded = true)
        }
        is EqSpec.M -> {}
    }
    val px = eq.momentPoint!!.second
    if (item.kind == Dof.M) return Expect(item.dir, null)
    if (item.kind == Dof.FX) return Expect(0, 0.0)
    val d = item.x - px
    if (abs(d) < EPS) return Expect(0, 0.0)
    return Expect(item.dir * d.sign.toInt(), abs(d))
}

fun coef(e: Expect): Double = e.sign * (e.arm ?: 1.0)

/** Gaussova eliminace s částečnou pivotací. null = singulární soustava. */
fun gauss(a: List<DoubleArray>, b: DoubleArray): DoubleArray? {
    val n = b.size
    val m = Array(n) { i -> DoubleArray(n + 1) { k -> if (k < n) a[i][k] else b[i] } }
    for (c in 0 until n) {
        var p = c
        for (r in c + 1 until n) if (abs(m[r][c]) > abs(m[p][c])) p = r
        if (abs(m[p][c]) < 1e-9) return null
        val tmp = m[c]; m[c] = m[p]; m[p] = tmp
        for (r in 0 until n) {
            if (r == c) continue
            val f = m[r][c] / m[c][c]
            for (k in c..n) m[r][k] -= f * m[c][k]
        }
    }
    return DoubleArray(n) { i -> m[i][n] / m[i][i] }
}

/** Lineární rovnice Σ a·u + c = 0 (a = koeficienty u neznámých). */
data class LinEq(val a: Map<String, Double>, val c: Double)

fun linearize(eq: EqSpec, items: List<Item>): LinEq {
    val a = LinkedHashMap<String, Double>()
    var c = 0.0
    for (it in items) {
        val k = coef(expected(it, eq))
        if (k == 0.0) continue
        if (it.unknownId != null) a[it.unknownId] = (a[it.unknownId] ?: 0.0) + k
        else c += k * (it.value ?: 0.0)
    }
    return LinEq(a, c)
}

fun solve(eqs: List<EqSpec>, items: List<Item>, unknowns: List<Unknown>): Map<String, Double>? {
    if (eqs.size != unknowns.size) return null
    val lin = eqs.map { linearize(it, items) }
    val a = lin.map { l -> DoubleArray(unknowns.size) { j -> l.a[unknowns[j].id] ?: 0.0 } }
    val b = DoubleArray(lin.size) { -lin[it].c }
    val x = gauss(a, b) ?: return null
    return unknowns.withIndex().associate { (i, u) -> u.id to (if (abs(x[i]) < 1e-9) 0.0 else x[i]) }
}

data class NamedPoint(val label: String, val x: Double, val isSupport: Boolean)

fun namedPoints(p: Problem): List<NamedPoint> =
    (p.supports.map { NamedPoint(it.label, it.x, true) } + p.hinges.map { NamedPoint(it.label, it.x, false) }).sortedBy { it.x }

/** Strana kloubu s menším počtem neznámých – vhodnější pro podmínku M_K = 0. */
fun preferredSide(p: Problem, hingeX: Double): Side {
    // vodorovné reakce leží v ose nosníku – do momentové podmínky nevstupují
    val us = unknownsOf(p).filter { it.kind != Dof.FX }
    val left = us.count { it.x < hingeX }
    val right = us.count { it.x > hingeX }
    return if (right <= left) Side.RIGHT else Side.LEFT
}

/** Bod pro momentovou podmínku, kde se protíná nejvíc neznámých. */
fun preferredMomentPoint(p: Problem): NamedPoint {
    val us = unknownsOf(p)
    val pts = namedPoints(p).filter { it.isSupport }
    var best = pts[0]
    var bestN = -1
    for (q in pts) {
        val n = us.count { abs(it.x - q.x) < EPS && it.kind != Dof.M }
        if (n > bestN) {
            best = q
            bestN = n
        }
    }
    return best
}

fun defaultEquations(p: Problem): List<EqSpec> {
    val pt = preferredMomentPoint(p)
    return listOf(EqSpec.Fx, EqSpec.Fy, EqSpec.M(pt.label, pt.x)) +
        p.hinges.map { EqSpec.Hinge(it.id, it.label, it.x, preferredSide(p, it.x)) }
}

fun solveProblem(p: Problem): Map<String, Double>? = solve(defaultEquations(p), allItems(p), unknownsOf(p))
