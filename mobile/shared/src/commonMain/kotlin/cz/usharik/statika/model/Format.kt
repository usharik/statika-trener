package cz.usharik.statika.model

import kotlin.math.abs
import kotlin.math.max

private val POW10 = doubleArrayOf(1.0, 10.0, 100.0, 1000.0, 10000.0, 100000.0, 1000000.0)

/** Number.toFixed z JavaScriptu (pro zobrazení stačí zaokrouhlení součinu). */
fun toFixed(v: Double, d: Int): String {
    if (!v.isFinite()) return v.toString()
    val m = POW10[d]
    if (kotlin.math.abs(v) * m >= 9e18) return v.toString()
    val r = jsRound(abs(v) * m).toLong()
    val ip = r / m.toLong()
    val fp = r % m.toLong()
    val sign = if (v < 0) "-" else ""
    if (d == 0) return sign + ip
    return sign + ip + "." + fp.toString().padStart(d, '0')
}

/** Číslo bez zbytečných nul s desetinnou tečkou (např. 4.5, 2). */
fun plain(v: Double, d: Int = 3): String {
    val s = toFixed(v, d)
    val r = if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    return if (r == "-0") "0" else r
}

/** Číslo s desetinnou čárkou, max. `d` desetinných míst, bez zbytečných nul. */
fun fmt(value: Double, d: Int = 2): String {
    val v = if (abs(value) < 0.5 / POW10[d]) 0.0 else value
    return plain(v, d).replace('.', ',').replace('-', '−')
}


private val NUM = Regex("^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$")

/** Tolerantní čtení čísla: čárka i tečka, různé znaky minus, mezery. */
fun parseNum(s: String): Double? {
    val t = s.trim().filterNot { it.isWhitespace() }.replaceFirst(',', '.').replace('−', '-').replace('–', '-')
    if (t == "" || t == "-" || t == ".") return null
    if (!NUM.matches(t)) return null
    val v = t.toDoubleOrNull() ?: return null
    return if (v.isFinite()) v else null
}

fun close(a: Double, b: Double): Boolean = abs(a - b) <= max(0.02, abs(b) * 0.01)

/**
 * Kontrola zadaného čísla: v toleranci [close], nebo správně zaokrouhlené na tolik desetinných míst,
 * kolik jich žák napsal (3,2 pro 3,167 je v pořádku – 3,2 je správné zaokrouhlení).
 */
fun closeTyped(input: String, v: Double, ans: Double): Boolean {
    if (close(v, ans)) return true
    val t = input.trim().replace(',', '.')
    val dot = t.indexOf('.')
    val k = if (dot < 0) 0 else t.length - dot - 1
    if (k < 1 || k > 6) return false
    return abs(v - ans) <= 0.5 / POW10[k] + 1e-9
}
