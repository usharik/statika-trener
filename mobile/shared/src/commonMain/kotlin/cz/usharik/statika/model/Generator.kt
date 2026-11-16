package cz.usharik.statika.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class GenOptions(val level: Int, val types: List<BeamType>)

private data class LaySupport(val x: Double, val type: SupportType)
private data class Layout(val length: Double, val supports: List<LaySupport>, val hinges: List<Double>)

// Pořadí volání rng musí přesně odpovídat webové verzi (src/model/generator.ts).
private fun layout(type: BeamType, rng: Rng, step: Double): Layout {
    when (type) {
        BeamType.SIMPLE -> {
            val l = rng.grid(3.0, 9.0, step)
            return Layout(l, listOf(LaySupport(0.0, SupportType.PIN), LaySupport(l, SupportType.ROLLER)), emptyList())
        }
        BeamType.OVERHANG -> {
            val l = rng.grid(5.0, 10.0, step)
            val both = rng.chance(0.35)
            val a = if (both) rng.grid(1.0, 2.0, step) else 0.0
            val b = l - rng.grid(1.0, 2.5, step)
            return Layout(l, listOf(LaySupport(a, SupportType.PIN), LaySupport(b, SupportType.ROLLER)), emptyList())
        }
        BeamType.CANTILEVER -> {
            val l = rng.grid(3.0, 6.0, step)
            return Layout(l, listOf(LaySupport(0.0, SupportType.FIXED)), emptyList())
        }
        BeamType.GERBER -> {
            val v = rng.int(0, 2)
            if (v == 0) {
                // vetknutí – kloub – posuvná podpora (případně s převislým koncem)
                val l = rng.grid(6.0, 10.0, step)
                val k = rng.grid(2.0, l - 3, step)
                val b = if (rng.chance(0.3)) l - rng.grid(1.0, 1.5, step) else l
                if (b - k < 2) return layout(type, rng, step)
                return Layout(l, listOf(LaySupport(0.0, SupportType.FIXED), LaySupport(b, SupportType.ROLLER)), listOf(k))
            }
            if (v == 1) {
                // A pevná – B posuvná – K kloub – C posuvná (převislý konec nese zavěšené pole)
                val l = rng.grid(8.0, 12.0, step)
                val b = rng.grid(3.0, l - 5, step)
                val k = rng.grid(b + 1, min(b + 2.5, l - 3), step)
                return Layout(l, listOf(LaySupport(0.0, SupportType.PIN), LaySupport(b, SupportType.ROLLER), LaySupport(l, SupportType.ROLLER)), listOf(k))
            }
            // A pevná – K kloub – B posuvná – C posuvná (zavěšené pole vlevo)
            val l = rng.grid(8.0, 12.0, step)
            val k = rng.grid(3.0, 5.0, step)
            val b = rng.grid(k + 1, min(k + 2.5, l - 3), step)
            return Layout(l, listOf(LaySupport(0.0, SupportType.PIN), LaySupport(b, SupportType.ROLLER), LaySupport(l, SupportType.ROLLER)), listOf(k))
        }
    }
}

private fun makeLoads(lay: Layout, rng: Rng, level: Int, step: Double): List<Load>? {
    val l = lay.length
    val blocked = (lay.supports.map { it.x } + lay.hinges).toSet()
    val loads = mutableListOf<Load>()
    val used = mutableListOf<Double>()
    var udlRange: Pair<Double, Double>? = null

    val wantUdl = if (level >= 2) rng.chance(if (level == 2) 0.55 else 0.6) else rng.chance(0.2)
    if (wantUdl) {
        // spojité zatížení nesmí přecházet přes kloub
        val bounds = listOf(0.0) + lay.hinges + listOf(l)
        val seg = rng.int(0, bounds.size - 2)
        val s0 = bounds[seg]
        val s1 = bounds[seg + 1]
        if (s1 - s0 >= 1) {
            val whole = rng.chance(0.35)
            val x1 = if (whole) s0 else rng.grid(s0, s1 - 1, step)
            val x2 = if (whole) s1 else rng.grid(x1 + 1, s1, step)
            val q = rng.int(1, if (level == 1) 5 else 8)
            loads += Udl("u1", "q", x1, x2, q.toDouble())
            udlRange = x1 to x2
        }
    }

    val nPoint = when (level) {
        1 -> rng.int(1, 2)
        2 -> rng.int(1, 2) + (if (udlRange != null) 0 else 1)
        else -> rng.int(2, 3)
    }
    val candidates = mutableListOf<Double>()
    var x = step
    while (x <= l + EPS) {
        val xr = round6(x)
        val skip = xr in blocked || (udlRange != null && xr > udlRange.first - EPS && xr < udlRange.second + EPS)
        if (!skip) candidates += xr
        x += step
    }
    val positions = rng.shuffle(candidates)

    // minimální rozestup osamělých zatížení, aby výkres zůstal čitelný
    val gap = max(step, if (l >= 6) 1.0 else 0.5)
    var momentUsed = false
    for (i in 0 until nPoint) {
        val px = positions.firstOrNull { p -> used.all { u -> abs(u - p) > gap - EPS } } ?: break
        used += px
        val r = rng.next()
        if (level == 3 && !momentUsed && r < 0.3) {
            momentUsed = true
            val m = rng.int(2, 15)
            val ccw = rng.chance(0.5)
            loads += PointMoment("m$i", "M", px, m.toDouble(), ccw)
            continue
        }
        val inclined = level >= 2 && rng.chance(if (level == 2) 0.45 else 0.5)
        val f = rng.int(2, if (level == 1) 12 else 20).toDouble()
        if (inclined) {
            val alpha = rng.pick(listOf(30, 45, 60))
            val angle = if (rng.chance(0.5)) 360 - alpha else 180 + alpha
            loads += PointForce("f$i", "F", px, f, angle.toDouble(), alpha)
        } else {
            val up = level == 3 && rng.chance(0.12)
            loads += PointForce("f$i", "F", px, f, if (up) 90.0 else 270.0)
        }
    }
    if (loads.none { it !is PointMoment }) return null
    return loads
}

private fun mirror(p: Problem): Problem {
    val len = p.length
    fun m(x: Double) = round6(len - x)
    return p.copy(
        supports = p.supports.map { it.copy(x = m(it.x)) },
        hinges = p.hinges.map { it.copy(x = m(it.x)) },
        loads = p.loads.map { l ->
            when (l) {
                is PointForce -> l.copy(x = m(l.x), angle = (540 - l.angle) % 360)
                is PointMoment -> l.copy(x = m(l.x), ccw = !l.ccw)
                is Udl -> l.copy(x1 = m(l.x2), x2 = m(l.x1))
            }
        },
    )
}

private fun posOf(l: Load): Double = when (l) {
    is PointForce -> l.x
    is PointMoment -> l.x
    is Udl -> l.x1
}

/** Popisky podle polohy zleva doprava: podpory A, B, C…, klouby K, síly F1, F2… */
private fun relabel(p: Problem): Problem {
    val supports = p.supports.sortedBy { it.x }.mapIndexed { i, s -> s.copy(id = "s$i", label = "ABCDE"[i].toString()) }
    val hinges = p.hinges.sortedBy { it.x }.mapIndexed { i, h -> h.copy(id = "h$i", label = if (p.hinges.size > 1) "K${i + 1}" else "K") }
    val sorted = p.loads.sortedBy(::posOf)
    val counts = sorted.groupingBy { it::class }.eachCount()
    val idx = mutableMapOf<Any, Int>()
    val loads = sorted.map { l ->
        val i = (idx[l::class] ?: 0) + 1
        idx[l::class] = i
        when (l) {
            is PointForce -> l.copy(id = "f$i", label = "F_$i")
            is PointMoment -> l.copy(id = "m$i", label = if (counts.getValue(l::class) > 1) "M_$i" else "M")
            is Udl -> l.copy(id = "u$i", label = if (counts.getValue(l::class) > 1) "q_$i" else "q")
        }
    }
    return p.copy(supports = supports, hinges = hinges, loads = loads)
}

private fun validate(p: Problem): Boolean {
    if (unknownsOf(p).size != 3 + p.hinges.size) return false
    val r = solveProblem(p) ?: return false
    for (u in unknownsOf(p)) {
        val v = r.getValue(u.id)
        if (!v.isFinite() || abs(v) > 400) return false
        // nulové svislé reakce a momenty působí zmateně – vynecháme
        if (u.kind != Dof.FX && abs(v) < 0.05) return false
    }
    return true
}

fun generate(seed: Int, opts: GenOptions): Problem {
    val rng = Rng(seed)
    val types = opts.types.ifEmpty { listOf(BeamType.SIMPLE) }
    val step = if (opts.level == 1) 1.0 else 0.5
    // typ se volí jen jednou – jinak by typy s více odmítnutými pokusy (Gerber) vycházely vzácněji
    val type = rng.pick(types)
    repeat(400) {
        val lay = layout(type, rng, step)
        val loads = makeLoads(lay, rng, opts.level, step) ?: return@repeat
        val supports = lay.supports.mapIndexed { i, s -> Support("s$i", "", round6(s.x), s.type) }
        val hinges = lay.hinges.mapIndexed { i, x -> Hinge("h$i", "", round6(x)) }
        var p = Problem(seed, type, round6(lay.length), supports, hinges, loads)
        // zrcadlení: pevná podpora / vetknutí se objeví i vpravo
        if (rng.chance(0.5)) p = mirror(p)
        p = relabel(p)
        if (validate(p)) return p
    }
    error("Nepodařilo se vygenerovat úlohu")
}
