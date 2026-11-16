package cz.usharik.statika.practice

import cz.usharik.statika.model.closeTyped
import cz.usharik.statika.model.parseNum
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Jak přísně kontrolovat číslo. Jedna logika pro karty výkladu (NumAnswer) i procvičování.
 * - VALUE: délka, síla, moment – [closeTyped] (1 % / 0,02, nebo správné zaokrouhlení na zapsaná místa)
 * - ANGLE: úhel ve stupních ±0,5°, u malých úhlů přísněji (3 % úhlu, aspoň ±0,1°) – zadání chce 1 desetinné místo
 * - RATIO: hodnota sin / cos / tg ±0,006 (0,87 i 0,866 pro √3/2); u velkých tangent 0,5 %
 * - PERCENT: sklon v % ±0,5 (nad 50 % ±1 %)
 * - REL: malé délky (převýšení 0,12 m) – jen 1 % (bez spodní hranice 0,02), nebo správné zaokrouhlení na aspoň 2 místa
 */
enum class Tol { VALUE, ANGLE, RATIO, PERCENT, REL }

/**
 * Je zapsaná hodnota [v] (text [input]) správná odpověď [ans]? [scale] = velikost, ke které se vztahuje 1 % u VALUE
 * (součet složek: chyba z mezivýsledků roste s velikostí sčítanců, ne s výsledkem).
 */
fun accept(input: String, v: Double, ans: Double, tol: Tol, scale: Double? = null): Boolean = when (tol) {
    Tol.VALUE -> closeTyped(input, v, ans) || (scale != null && abs(v - ans) <= max(0.02, abs(scale) * 0.01) + 1e-9)
    Tol.REL -> abs(v - ans) <= abs(ans) * 0.01 + 1e-9 || run {
        val t = input.trim().replace(',', '.')
        val dot = t.indexOf('.')
        val k = if (dot < 0) 0 else t.length - dot - 1
        k in 2..6 && abs(v - ans) <= 0.5 / 10.0.pow(k) + 1e-9
    }
    Tol.ANGLE -> abs(v - ans) <= angleTol(ans) + 1e-9
    Tol.RATIO -> abs(v - ans) <= max(0.006, abs(ans) * 0.005) + 1e-9
    Tol.PERCENT -> abs(v - ans) <= (if (abs(ans) > 50) abs(ans) * 0.01 else 0.5) + 1e-9
}

/** tolerance úhlu: ±0,5°, u malých úhlů 3 % (aspoň ±0,1°) – jinak by u sklonu 2 % (1,15°) prošlo 0,65°–1,65° */
fun angleTol(ans: Double): Double = min(0.5, max(0.1, abs(ans) * 0.03))

/** Hodnota je „skoro“ správně (do 5 %), ale mimo toleranci – typicky zaokrouhlení mezivýsledku. */
fun near(v: Double, ans: Double): Boolean = abs(v - ans) <= max(0.05, abs(ans) * 0.05)

/** Číslo nebo zlomek („3/5“, „3 : 5“) – podíl stran se dá zapsat i jako zlomek. */
fun parseRatio(s: String): Double? {
    val t = s.trim()
    val sep = t.indexOfFirst { it == '/' || it == ':' }
    if (sep < 0) return parseNum(t)
    val a = parseNum(t.substring(0, sep)) ?: return null
    val b = parseNum(t.substring(sep + 1)) ?: return null
    if (b == 0.0) return null
    return a / b
}
