package cz.usharik.statika.trainer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.usharik.statika.model.BeamType
import cz.usharik.statika.model.GenOptions
import cz.usharik.statika.platform.KeyValueStore

/**
 * Oblíbená úloha. Úloha je jednoznačně dána číslem a nastavením generátoru (úroveň + typy nosníků),
 * proto se ukládá obojí – po změně nastavení se otevře pořád stejná úloha.
 */
data class Favorite(val seed: Int, val opts: GenOptions, val solved: Boolean = false) {
    fun same(seed: Int, opts: GenOptions) = this.seed == seed && this.opts.level == opts.level && this.opts.types.toSet() == opts.types.toSet()
}

/** Zápis nastavení generátoru do řetězce („2:simple+gerber“) a zpět. */
fun GenOptions.encode(): String = "$level:" + types.joinToString("+") { it.key }

fun decodeOpts(s: String): GenOptions? {
    val parts = s.split(':')
    if (parts.size != 2) return null
    val level = parts[0].toIntOrNull()?.takeIf { it in 1..3 } ?: return null
    val types = parts[1].split('+').mapNotNull(BeamType::of).distinct()
    if (types.isEmpty()) return null
    return GenOptions(level, BeamType.entries.filter { it in types })
}

/** Seznam oblíbených úloh v zařízení (nejnovější první). */
class FavoritesRepository(private val store: KeyValueStore) {
    var items by mutableStateOf(load())
        private set

    fun isFavorite(seed: Int, opts: GenOptions) = items.any { it.same(seed, opts) }

    /** Přidá, nebo odebere úlohu; vrátí true, je-li teď v oblíbených. */
    fun toggle(seed: Int, opts: GenOptions, solved: Boolean): Boolean {
        val now = if (isFavorite(seed, opts)) {
            items = items.filterNot { it.same(seed, opts) }
            false
        } else {
            items = (listOf(Favorite(seed, opts, solved)) + items).take(MAX)
            true
        }
        save()
        return now
    }

    fun remove(f: Favorite) {
        items = items - f
        save()
    }

    /** Vyřešená oblíbená úloha dostane v seznamu fajfku. */
    fun markSolved(seed: Int, opts: GenOptions) {
        if (items.none { it.same(seed, opts) && !it.solved }) return
        items = items.map { if (it.same(seed, opts)) it.copy(solved = true) else it }
        save()
    }

    private fun load(): List<Favorite> = store.getString(KEY).orEmpty().split(';').mapNotNull { row ->
        val p = row.split(',')
        if (p.size != 3) return@mapNotNull null
        val seed = p[0].toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        val opts = decodeOpts(p[1]) ?: return@mapNotNull null
        Favorite(seed, opts, p[2] == "1")
    }

    private fun save() {
        store.putString(KEY, items.joinToString(";") { "${it.seed},${it.opts.encode()},${if (it.solved) 1 else 0}" })
    }

    private companion object {
        const val KEY = "favorites"
        const val MAX = 200
    }
}
