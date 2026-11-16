package cz.usharik.statika.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/*
 * Drobná pravidla společná pro Android i iOS (odkazy, jazyk, verze, parametry analytiky),
 * aby se platformy nemohly rozejít.
 */

/** Nejvyšší číslo úlohy (stejně jako „Úloha podle čísla“ v nastavení). */
const val MAX_SEED = 999_999

/**
 * Číslo úlohy z odkazu: `statika://12345`, `statika://open/12345`, `statika:12345`,
 * `https://usharik.github.io/statika-trener/#12345` nebo `…?seed=12345`. Jinak null.
 */
fun seedFromLink(url: String?): Int? {
    val u = url?.trim().orEmpty()
    if (u.isEmpty()) return null
    val candidates = buildList {
        add(u.substringAfter('#', ""))
        Regex("""[?&]seed=(\d+)""").find(u)?.let { add(it.groupValues[1]) }
        if (u.startsWith("statika:", ignoreCase = true)) add(u.substringAfter(':').substringBefore('?').substringBefore('#').trim('/').substringAfterLast('/'))
    }
    return candidates.firstNotNullOfOrNull { c -> c.toIntOrNull()?.takeIf { it in 1..MAX_SEED } }
}

/** Jedno otevření úlohy odkazem; [id] odliší opakované otevření stejného čísla. */
data class LinkRequest(val seed: Int, val id: Int)

/**
 * Odkazy na úlohy jako jednorázové události: i druhé otevření stejného odkazu (po přechodu na jinou úlohu)
 * úlohu znovu otevře. Volá se na hlavním vlákně.
 */
class LinkRequests {
    var current by mutableStateOf<LinkRequest?>(null)
        private set
    private var next = 0

    /** Otevře úlohu z adresy; false, když v ní žádné číslo úlohy není. */
    fun open(url: String?): Boolean = seedFromLink(url)?.let { open(it) } ?: false

    fun open(seed: Int): Boolean {
        if (seed !in 1..MAX_SEED) return false
        current = LinkRequest(seed, ++next)
        return true
    }

    /** Úloha z odkazu je otevřená – po znovuvytvoření obrazovky se už znovu neotevře. */
    fun consume(r: LinkRequest) {
        if (current == r) current = null
    }
}

/** Jazyk systému jako dvoupísmenný kód („ru-CZ“, „ru_RU“, „RU“ → „ru“). */
fun systemLanguageCode(tag: String?): String? =
    tag?.trim()?.substringBefore('-')?.substringBefore('_')?.lowercase()?.takeIf { it.length in 2..3 }

/** Verze v nastavení: „1.7 (36ea322)“. */
fun versionLabel(name: String, gitHash: String?): String =
    if (gitHash.isNullOrBlank() || gitHash == "unknown") name else "$name ($gitHash)"

/**
 * Událost analytiky upravená na pravidla Firebase (stejná na obou platformách):
 * název a klíče jen [A-Za-z0-9_], začínají písmenem, nejvýš 40 znaků; nejvýš 25 parametrů;
 * hodnoty jen Long, Double nebo String (nejvýš 100 znaků); Boolean jako „true“/„false“.
 */
object AnalyticsParams {
    const val MAX_NAME = 40
    const val MAX_PARAMS = 25
    const val MAX_VALUE = 100

    fun name(raw: String): String {
        val s = raw.replace(Regex("[^A-Za-z0-9_]"), "_").trimStart('_')
        val n = if (s.firstOrNull()?.isLetter() == true) s else "e_$s"
        return n.take(MAX_NAME)
    }

    fun params(raw: Map<String, Any>): Map<String, Any> = raw.entries.take(MAX_PARAMS).associate { (k, v) ->
        name(k) to when (v) {
            is Int -> v.toLong()
            is Long -> v
            is Short -> v.toLong()
            is Byte -> v.toLong()
            is Float -> v.toDouble()
            is Double -> v
            else -> v.toString().take(MAX_VALUE)
        }
    }
}

/**
 * Analytika s úpravou názvů a parametrů; platforma dodá jen [sink] (volání Firebase).
 * Události před prvním rozhodnutím o souhlasu se podrží (nejvýš [BUFFER]) – po souhlasu se odešlou,
 * po odmítnutí zahodí. Jinak by se ztratilo např. první „problem_start“ (souhlas přijde až po startu UI).
 */
class NormalizedAnalytics(private val sink: AnalyticsSink) : Analytics {
    private var decided: AnalyticsConsent? = null
    private val pending = ArrayDeque<Pair<String, Map<String, Any>>>()

    override fun event(name: String, params: Map<String, Any>) {
        val e = AnalyticsParams.name(name) to AnalyticsParams.params(params)
        val c = decided
        when {
            c == null -> {
                if (pending.size >= BUFFER) pending.removeFirst()
                pending.addLast(e)
            }
            c.analytics -> sink.log(e.first, e.second)
        }
    }

    override fun setConsent(consent: AnalyticsConsent) {
        decided = consent
        sink.setConsent(consent)
        if (consent.analytics) while (pending.isNotEmpty()) pending.removeFirst().let { (n, p) -> sink.log(n, p) }
        pending.clear()
    }

    companion object {
        const val BUFFER = 50
    }
}

/** Jen volání SDK analytiky; hodnoty parametrů jsou už Long, Double nebo String. */
interface AnalyticsSink {
    fun log(name: String, params: Map<String, Any>)

    /** Souhlas (UMP + TCF): sběr jen při [AnalyticsConsent.analytics]; odvolání ho zase vypne. */
    fun setConsent(consent: AnalyticsConsent)
}
