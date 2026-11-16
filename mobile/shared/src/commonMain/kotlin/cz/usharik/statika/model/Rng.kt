package cz.usharik.statika.model

import kotlin.math.floor
import kotlin.random.Random

/**
 * Deterministický generátor (mulberry32) – stejné číslo úlohy = stejná úloha.
 * Bit po bitu shodný s webovou verzí (src/model/rng.ts), aby čísla úloh platila v obou.
 */
class Rng(seed: Int) {
    private var s: Int

    init {
        // promíchání, aby sousední čísla úloh dávala nesouvisející úlohy
        var h = (seed xor 0x9e3779b9.toInt()) * 0x85ebca6b.toInt()
        h = (h xor (h ushr 13)) * 0xc2b2ae35.toInt()
        s = h xor (h ushr 16)
    }

    fun next(): Double {
        s += 0x6d2b79f5
        var t = s
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        return ((t xor (t ushr 14)).toLong() and 0xffffffffL).toDouble() / 4294967296.0
    }

    fun int(min: Int, max: Int): Int = min + floor(next() * (max - min + 1)).toInt()

    fun <T> pick(list: List<T>): T = list[floor(next() * list.size).toInt()]

    fun chance(p: Double): Boolean = next() < p

    /** násobek kroku step v intervalu <min, max> */
    fun grid(min: Double, max: Double, step: Double): Double {
        val n = floor((max - min) / step + 1e-9).toInt()
        return round6(min + int(0, n) * step)
    }

    fun <T> shuffle(list: List<T>): List<T> {
        val a = list.toMutableList()
        for (i in a.size - 1 downTo 1) {
            val j = floor(next() * (i + 1)).toInt()
            val tmp = a[i]
            a[i] = a[j]
            a[j] = tmp
        }
        return a
    }
}

/** Math.round z JavaScriptu (polovina nahoru). */
fun jsRound(v: Double): Double = floor(v + 0.5)

fun round6(v: Double): Double = jsRound(v * 1_000_000.0) / 1_000_000.0

fun randomSeed(): Int = 1 + Random.nextInt(999_999)
