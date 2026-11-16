package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.model.fmt
import cz.usharik.statika.practice.Trig
import kotlin.math.abs
import kotlin.math.max

/*
 * Společné pomůcky typů úloh procvičování (texty čísel, pravoúhlé trojice, názvy funkcí, porovnání).
 */

/** úhel ve stupních pro texty („35°“) */
internal fun deg(a: Double): String = fmt(a, 0) + "°"

/** číslo pro texty (desetinná čárka, bez zbytečných nul) */
internal fun n(v: Double, d: Int = 2): String = fmt(v, d)

/** Pythagorejské trojice (odvěsna, odvěsna, přepona) podle úrovně – kulatá čísla bez kalkulačky. */
internal val TRIPLES_EASY = listOf(Triple(3, 4, 5), Triple(6, 8, 10), Triple(5, 12, 13))
internal val TRIPLES_MORE = TRIPLES_EASY + listOf(Triple(8, 15, 17), Triple(7, 24, 25), Triple(9, 12, 15), Triple(20, 21, 29))

/** goniometrická funkce podle jména („sin“, „cos“, „tg“) */
internal enum class Fn(val label: String) {
    SIN("sin"), COS("cos"), TG("tg");

    fun deg(a: Double): Double = when (this) {
        SIN -> Trig.sinD(a)
        COS -> Trig.cosD(a)
        TG -> Trig.tgD(a)
    }

    fun rad(a: Double): Double = when (this) {
        SIN -> Trig.sinRad(a)
        COS -> Trig.cosRad(a)
        TG -> Trig.tgRad(a)
    }

    fun grad(a: Double): Double = when (this) {
        SIN -> Trig.sinGrad(a)
        COS -> Trig.cosGrad(a)
        TG -> Trig.sinGrad(a) / Trig.cosGrad(a)
    }

    /** obrácená funkce ve stupních */
    fun inv(v: Double): Double = when (this) {
        SIN -> Trig.asinD(v)
        COS -> Trig.acosD(v)
        TG -> Trig.atanD(v)
    }

    /** na kalkulačce: sin⁻¹ / cos⁻¹ / tan⁻¹ */
    val calcInv: String get() = when (this) {
        SIN -> "sin⁻¹"
        COS -> "cos⁻¹"
        TG -> "tan⁻¹"
    }

    val arc: String get() = when (this) {
        SIN -> "arcsin"
        COS -> "arccos"
        TG -> "arctg"
    }
}

/** „skoro stejné“ pro malá čísla (podíl, úhel v radiánech) – relativně, ne ±0,5° */
internal fun nearRel(v: Double?, w: Double): Boolean = v != null && abs(v - w) <= max(0.004, abs(w) * 0.02)

/** stejná tolerance jako u sil (1 % / 0,02) */
internal fun close2(v: Double?, w: Double): Boolean = v != null && abs(v - w) <= max(0.02, abs(w) * 0.01)

/** „= 0,5“, nebo „≈ 0,385“, když se číslo při zaokrouhlení na [d] míst změní (učitel: u zaokrouhlených hodnot ≈) */
internal fun approx(v: Double, d: Int = 2): String {
    val k = 10.0.let { var x = 1.0; repeat(d) { x *= it }; x }
    return if (abs(v * k - kotlin.math.round(v * k)) < 1e-6) "= ${n(v, d)}" else "≈ ${n(v, d)}"
}

/** „(sin 30° = 0,5; cos 30° ≈ 0,866)“ – hodnoty do zadání pro úroveň bez kalkulačky */
internal fun given(a: Double): L {
    val s = Trig.sinD(a)
    val c = Trig.cosD(a)
    fun eq(v: Double) = if (abs(v * 1000 - kotlin.math.round(v * 1000)) < 1e-9) "= ${n(v, 3)}" else "≈ ${n(v, 3)}"
    val t = " (sin ${deg(a)} ${eq(s)}; cos ${deg(a)} ${eq(c)})"
    return L(t, t)
}

/**
 * „To je teprve podíl stran“ – u malých úhlů dá stejné číslo i kalkulačka v RAD (arctg 0,1 rad ≈ 0,0997),
 * proto rada zmíní obojí (učitel, bod 8).
 */
internal fun ratioOrRad(q: Double, ans: Double, fn: Fn): cz.usharik.statika.practice.TrapText {
    val radLike = nearRel(q, ans * kotlin.math.PI / 180)
    if (!radLike) return cz.usharik.statika.practice.Traps.RATIO
    val key = if (fn == Fn.TG) "tan" else fn.label
    return cz.usharik.statika.practice.Traps.RATIO.with(
        L(
            "To je teprve podíl stran (${fn.label} α) – nebo je kalkulačka v RAD. Úhel dostaneš přes $key⁻¹ v režimu DEG.",
            "Это пока отношение сторон (${fn.label} α) — или калькулятор в RAD. Угол получишь через $key⁻¹ в режиме DEG.",
        ),
    )
}

/** kde najít goniometrické funkce na telefonu (do první nápovědy úloh s kalkulačkou) */
internal val PHONE_CALC = L(
    " Na telefonu: otoč Kalkulačku na šířku (iPhone) nebo rozbal panel funkcí (Android) – objeví se sin, cos, tan a přepínač Deg/Rad; tan⁻¹ je pod 2nd (iPhone) nebo INV (Android).",
    " На телефоне: поверни Калькулятор горизонтально (iPhone) или разверни панель функций (Android) — появятся sin, cos, tan и переключатель Deg/Rad; tan⁻¹ — под 2nd (iPhone) или INV (Android).",
)

/** odkazy na karty kurzu trigonometrie (lekce 101–108, číslo karty od 0) */
internal object Cards {
    val NAME_SIDES = 101 to 2
    val THREE_RATIOS = 102 to 0
    val BUILD = 102 to 1
    val CHECK345 = 102 to 2
    val LADDER = 102 to 4
    val CALC_MODE = 103 to 1
    val SQRT_TABLE = 104 to 2
    val HALF_TRI = 104 to 1
    val PYTHAGORAS = 105 to 0
    val UNIT_HYP = 105 to 1
    val TANGENT = 105 to 2
    val COMPLEMENT = 105 to 3
    val BACK = 106 to 0
    val WHICH_INV = 106 to 1
    val SLOPE_RATIO = 106 to 3
    val SLOPE_PCT = 106 to 4
    val UNIT_CIRCLE = 107 to 0
    val QUADRANTS = 107 to 1
    val SIGNS = 107 to 2
    val FROM_VERT = 108 to 0
    val RESULTANT = 108 to 1
    val SUM = 108 to 2
    val ARM = 108 to 3
    val RAFTER = 108 to 4
}
