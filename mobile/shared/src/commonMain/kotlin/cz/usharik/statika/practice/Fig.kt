package cz.usharik.statika.practice

/**
 * Obrázek k úloze – jen data (čistý Kotlin, testovatelné bez Compose). Kreslí ho `PrimBuilder.fig(...)` v learn/TrigKit.kt.
 * Úhly ve stupních; „φ“ = od osy +x proti směru hodinových ručiček (matematicky, osa y nahoru).
 */
sealed interface Fig {
    /**
     * Pravoúhlý trojúhelník: odvěsna [adj] leží u vyznačeného úhlu α, [opp] naproti; přepona se dopočítá.
     * [rot] = otočení o rot × 90°, [mirror] = zrcadlení – názvy stran se nesmí dát uhodnout z polohy.
     * [labels] = popisky stran (null = strana bez popisku), [highlight] = zvýrazněná strana, [angle] = popisek úhlu.
     */
    data class Tri(
        val adj: Double,
        val opp: Double,
        val rot: Int = 0,
        val mirror: Boolean = false,
        val labels: Map<TriSide, String> = emptyMap(),
        val highlight: TriSide? = null,
        val angle: String? = "α",
    ) : Fig

    /**
     * Šikmá síla z počátku: [phi] = směr šipky (φ od +x), [angle] = úhel, který je v zadání vyznačený,
     * měřený od [from] (vodorovné / svislé / osy +x). [components] = dokreslit složky (po vyřešení).
     */
    data class Force(
        val f: Double,
        val phi: Double,
        val angle: Double,
        val from: AngleFrom = AngleFrom.HORIZONTAL,
        val label: String = "F",
        val components: Boolean = false,
        /** jednotka v popisku („kN“; prázdná = jen číslo, např. poloměr jednotkové kružnice) */
        val unit: String = "kN",
    ) : Fig

    /**
     * Složky [fx], [fy] (se znaménky) a z nich výslednice R – před vyřešením jen „R = ?“ a „α = ?“,
     * po vyřešení ([solved]) s hodnotami.
     */
    data class Comps(val fx: Double, val fy: Double, val solved: Boolean = false) : Fig

    /** Sklon: výška [rise] na vodorovné délce [run]; popisky (např. „3“, „4“ nebo „10 %“). */
    data class Slope(val rise: Double, val run: Double, val riseLabel: String? = null, val runLabel: String? = null, val angle: String? = "α") : Fig

    /** Páka délky [r] z osy O, síla [f] na konci svírá s pákou úhel [alpha]; [arm] = ukázat rameno r⊥. */
    data class Lever(val r: Double, val alpha: Double, val f: Double, val ccw: Boolean = true, val arm: Boolean = false) : Fig

    /** Šikmý nosník (krokev) délky [len] se sklonem [alpha]; [projections] = kóty půdorysu a výšky. */
    data class Rafter(val len: Double, val alpha: Double, val projections: Boolean = true) : Fig

    /** Několik sil z jednoho bodu (součet složek). */
    data class Sum(val forces: List<Force>) : Fig

    /** Displej kalkulačky: výraz, výsledek, indikátor režimu („DEG“ / „RAD“ / „GRAD“). */
    data class Calc(val expr: String, val value: String, val mode: String) : Fig
}

enum class TriSide { ADJ, OPP, HYP }

enum class AngleFrom { HORIZONTAL, VERTICAL, PLUS_X }
