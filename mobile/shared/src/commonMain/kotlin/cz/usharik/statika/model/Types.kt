package cz.usharik.statika.model

enum class SupportType(val key: String) { PIN("pin"), ROLLER("roller"), FIXED("fixed") }

data class Support(val id: String, val label: String, val x: Double, val type: SupportType)

data class Hinge(val id: String, val label: String, val x: Double)

sealed interface Load {
    val id: String
    val label: String
}

/** Osamělá síla. angle = směr síly ve stupních od osy +x, proti směru hodinových ručiček (dolů = 270). */
data class PointForce(
    override val id: String,
    override val label: String,
    val x: Double,
    val f: Double,
    val angle: Double,
    /** úhel od osy nosníku, který se kótuje ve výkresu (jen šikmé síly) */
    val alpha: Int? = null,
) : Load

data class PointMoment(
    override val id: String,
    override val label: String,
    val x: Double,
    val m: Double,
    val ccw: Boolean,
) : Load

/** Spojité rovnoměrné zatížení působící svisle dolů. */
data class Udl(
    override val id: String,
    override val label: String,
    val x1: Double,
    val x2: Double,
    val q: Double,
) : Load

enum class BeamType(val key: String) {
    SIMPLE("simple"), OVERHANG("overhang"), CANTILEVER("cantilever"), GERBER("gerber");

    companion object {
        fun of(key: String): BeamType? = entries.firstOrNull { it.key == key }
    }
}

data class Problem(
    val seed: Int,
    val type: BeamType,
    val length: Double,
    val supports: List<Support>,
    val hinges: List<Hinge>,
    val loads: List<Load>,
)
