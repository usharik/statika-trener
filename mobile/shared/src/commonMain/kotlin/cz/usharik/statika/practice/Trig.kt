package cz.usharik.statika.practice

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Goniometrické funkce ve stupních s přesnými hodnotami pro „známé“ úhly (sin 30° = 0,5 přesně, ne 0,49999…).
 * Žák počítá s 0,5 – a kontrola i texty (fmt) musí vidět totéž číslo.
 *
 * Pozor: model trenažéru (Mechanics.forceComponents) má vlastní tabulku shodnou bit po bitu s webem – tady ne,
 * tahle se používá jen ve výkladu a procvičování.
 */
object Trig {
    private val S3 = sqrt(3.0) / 2
    private val S2 = sqrt(2.0) / 2

    /** sin pro úhly 0…90 po 15° (přesné hodnoty); jinde null */
    private fun exactSin(deg: Int): Double? = when (deg) {
        0 -> 0.0
        30 -> 0.5
        45 -> S2
        60 -> S3
        90 -> 1.0
        else -> null
    }

    /** sin úhlu ve stupních (libovolný úhel; násobky 30° a 45° přesně) */
    fun sinD(deg: Double): Double {
        if (deg == floor(deg)) {
            val d = ((deg.toInt() % 360) + 360) % 360
            val (ref, sign) = when {
                d <= 90 -> d to 1.0
                d <= 180 -> (180 - d) to 1.0
                d <= 270 -> (d - 180) to -1.0
                else -> (360 - d) to -1.0
            }
            exactSin(ref)?.let { return if (it == 0.0) 0.0 else sign * it }
        }
        return sin(deg * PI / 180)
    }

    fun cosD(deg: Double): Double = sinD(90.0 - deg)

    /** tangens; pro 90° (+k·180°) vrací NaN – „neexistuje“ */
    fun tgD(deg: Double): Double {
        val c = cosD(deg)
        if (abs(c) < 1e-12) return Double.NaN
        if (deg == floor(deg) && (((deg.toInt() % 180) + 180) % 180) == 45) return if (sinD(deg) * c > 0) 1.0 else -1.0
        return sinD(deg) / c
    }

    fun asinD(v: Double): Double = asin(v.coerceIn(-1.0, 1.0)) * 180 / PI
    fun acosD(v: Double): Double = acos(v.coerceIn(-1.0, 1.0)) * 180 / PI
    fun atanD(v: Double): Double = atan(v) * 180 / PI

    /** co by ukázala kalkulačka v režimu RAD (číslo ve stupních bere jako radiány) */
    fun sinRad(deg: Double): Double = sin(deg)
    fun cosRad(deg: Double): Double = cos(deg)
    fun tgRad(deg: Double): Double = tan(deg)

    /** co by ukázala kalkulačka v režimu GRAD (pravý úhel = 100 gradů) */
    fun sinGrad(deg: Double): Double = sin(deg * PI / 200)
    fun cosGrad(deg: Double): Double = cos(deg * PI / 200)
}
