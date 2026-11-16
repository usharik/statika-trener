package cz.usharik.statika.trainer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.usharik.statika.i18n.Lang
import cz.usharik.statika.model.BeamType
import cz.usharik.statika.platform.KeyValueStore

enum class ThemeMode(val key: String) { SYSTEM("system"), LIGHT("light"), DARK("dark") }

/** Nastavení a statistika uložené v zařízení. */
class SettingsRepository(private val store: KeyValueStore, systemLang: String? = null) {
    var level by mutableIntStateOf(store.getInt(K_LEVEL, 1).coerceIn(1, 3))
        private set
    var types by mutableStateOf(
        store.getString(K_TYPES)?.split(',')?.mapNotNull(BeamType::of)?.takeIf { it.isNotEmpty() } ?: BeamType.entries.toList(),
    )
        private set
    var lang by mutableStateOf(Lang.of(store.getString(K_LANG)) ?: if (systemLang == "ru") Lang.RU else Lang.CS)
        private set
    var theme by mutableStateOf(ThemeMode.entries.firstOrNull { it.key == store.getString(K_THEME) } ?: ThemeMode.SYSTEM)
        private set
    var solved by mutableIntStateOf(store.getInt(K_SOLVED, 0))
        private set

    /** číslo rozpracované úlohy – po spuštění aplikace se k ní vrátíme */
    var seed: Int
        get() = store.getInt(K_SEED, 0)
        set(v) = store.putInt(K_SEED, v)

    /** nastavení generátoru rozpracované úlohy, pokud se liší od aktuálního (otevřená z oblíbených) */
    var problemOpts: String?
        get() = store.getString(K_PROBLEM_OPTS)
        set(v) = store.putString(K_PROBLEM_OPTS, v)

    /** vyřešené úlohy od poslední mezistránkové reklamy */
    var sinceAd: Int
        get() = store.getInt(K_SINCE_AD, 0)
        set(v) = store.putInt(K_SINCE_AD, v)

    fun updateLevel(v: Int) {
        level = v
        store.putInt(K_LEVEL, v)
    }

    /** Zapne/vypne typ nosníku; poslední zapnutý typ vypnout nejde. */
    fun toggleType(t: BeamType) {
        val next = if (t in types) types - t else types + t
        if (next.isEmpty()) return
        types = BeamType.entries.filter { it in next }
        store.putString(K_TYPES, types.joinToString(",") { it.key })
    }

    fun updateLang(v: Lang) {
        lang = v
        store.putString(K_LANG, v.code)
    }

    fun updateTheme(v: ThemeMode) {
        theme = v
        store.putString(K_THEME, v.key)
    }

    fun addSolved() {
        solved += 1
        store.putInt(K_SOLVED, solved)
    }

    private companion object {
        const val K_LEVEL = "level"
        const val K_TYPES = "types"
        const val K_LANG = "lang"
        const val K_THEME = "theme"
        const val K_SOLVED = "solved"
        const val K_SEED = "seed"
        const val K_SINCE_AD = "sinceAd"
        const val K_PROBLEM_OPTS = "problemOpts"
    }
}
