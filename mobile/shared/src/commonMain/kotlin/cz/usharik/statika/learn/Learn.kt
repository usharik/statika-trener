package cz.usharik.statika.learn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import cz.usharik.statika.i18n.Lang
import cz.usharik.statika.i18n.LocalTr
import cz.usharik.statika.i18n.Tr
import cz.usharik.statika.platform.KeyValueStore

/*
 * Interaktivní výklad „Základy statiky“: 9 krátkých lekcí z karet. Každá karta = jedna myšlenka,
 * jeden interaktivní obrázek a nejvýš dvě věty. Texty jsou přímo u karet (česky i rusky vedle sebe),
 * aby se daly snadno vyčíst a opravit.
 */

/** Text ve dvou jazycích. Značky: [[F_x]] = s indexem, **tučně** (jako v trenažéru). */
data class L(val cs: String, val ru: String)

fun Tr.pick(l: L): String = if (lang == Lang.RU) l.ru else l.cs

@Composable
fun L.t(): String = LocalTr.current.pick(this)

/** Jedna karta lekce. [content] kreslí obrázek, text i ovládání; kartu dokončí voláním [CardScope.complete]. */
class Card(val content: @Composable CardScope.() -> Unit)

/**
 * Lekce. [id] je jednoznačné přes všechny kurzy (statika 1–9, trigonometrie 101–108) – podle něj se ukládá postup
 * a míří odkazy `learn(lesson, card)`. Žákovi se ukazuje [no] (pořadí v kurzu).
 */
class Lesson(
    val id: Int,
    val title: L,
    /** jedna věta „o čem to je“ do seznamu lekcí */
    val summary: L,
    val cards: List<Card>,
) {
    /** číslo lekce v rámci kurzu (101 → 1) */
    val no: Int get() = id % 100
}

/**
 * Stav karty. Každá karta říká, co má žák udělat ([Todo] – seznam úkolů s fajfkami, [Quiz] – otázka);
 * „Dál“ se odemkne, až je hotové všechno (pasivní čtení nestačí). Úkoly hlásí svůj stav přes [report].
 */
class CardScope(private val alreadyDone: Boolean) {
    /** úkoly podle zdroje (Todo, Quiz…): text → splněno */
    private val parts = mutableStateMapOf<String, List<Pair<L, Boolean>>>()

    /** vše splněno právě teď (ne při dřívější návštěvě) – jen podle toho smí obrázek ukázat řešení */
    var solvedHere by mutableStateOf(false)
        private set

    /** co ještě zbývá udělat */
    val pending: List<L> get() = parts.values.flatten().filter { !it.second }.map { it.first }

    /** všechny úkoly v této návštěvě splněné */
    private val allDone: Boolean get() = parts.isNotEmpty() && parts.values.all { l -> l.all { it.second } }

    /** smí se jít dál: úkoly jednou splněné v této návštěvě (zpětné odškrtnutí už nezamyká), nebo karta splněná dřív */
    val done: Boolean get() = alreadyDone || solvedHere

    /** zvýší se, když žák zkusí jít dál dřív, než je hotovo – úkoly se zvýrazní a posunou do pohledu */
    var nag by mutableIntStateOf(0)
        private set

    fun nag() {
        nag++
    }

    fun report(key: String, items: List<Pair<L, Boolean>>) {
        if (parts[key] != items) parts[key] = items
        if (allDone) solvedHere = true
    }
}

/** Kam se má výklad otevřít. */
sealed interface LearnNav {
    /** seznam lekcí aktuálního kurzu ([LearnProgress.course]) */
    data object Home : LearnNav
    data object Glossary : LearnNav

    /**
     * Lekce [lesson] od karty [card]. [fromTrainer] = otevřeno z úlohy – zavřením se vrátí rovnou k ní.
     * [returnTo] = otevřeno odkazem z jiného místa výkladu (karta jiné lekce, procvičování) – „zpět“ i „Hotovo“ vedou tam.
     */
    data class Open(val lesson: Int, val card: Int = 0, val fromTrainer: Boolean = false, val returnTo: LearnNav? = null) : LearnNav

    /** Procvičování kurzu: [set] = null → seznam sérií, jinak série daného souboru (id z `PracticeSets`). */
    data class Practice(val set: String? = null, val returnTo: LearnNav? = null) : LearnNav
}

/** Kde ve výkladu žák právě je – odkazy uvnitř výkladu ([LearnJump], odkazy ve zprávách) se sem vrátí. Null = mimo výklad. */
val LocalLearnHere = staticCompositionLocalOf<LearnNav?> { null }

/** Otevření výkladu odkudkoli (trenažér, zprávy, nastavení). */
fun interface LearnOpener {
    fun open(nav: LearnNav)
}

val LocalLearn = staticCompositionLocalOf<LearnOpener> { LearnOpener { } }

/** Postup ve výkladu uložený v zařízení. */
class LearnProgress(private val store: KeyValueStore) {
    /** postup v procvičování (nejlepší výsledky, úroveň, ★) */
    val practice = cz.usharik.statika.practice.PracticeStore(store)

    /** rozpracované série procvičování podle souboru – přežijí odskok do lekce a zpět (ne zavření výkladu) */
    internal val sessions = mutableMapOf<String, cz.usharik.statika.practice.PracticeSession>()

    /** zvyšuje se při každé změně – UI podle něj překreslí seznam */
    var version by mutableIntStateOf(0)
        private set

    fun seen(lesson: Int): Int = store.getInt("learn.seen.$lesson", 0)
    fun isDone(lesson: Int): Boolean = store.getInt("learn.done.$lesson", 0) == 1

    fun markSeen(lesson: Int, cardIndex: Int) {
        if (cardIndex + 1 > seen(lesson)) {
            store.putInt("learn.seen.$lesson", cardIndex + 1)
            version++
        }
    }

    /** karta, jejíž úkoly žák opravdu splnil (jen ta se při další návštěvě nemusí znovu dělat) */
    fun cardDone(lesson: Int, card: Int): Boolean = store.getInt("learn.card.$lesson.$card", 0) == 1

    fun markCardDone(lesson: Int, card: Int) {
        if (!cardDone(lesson, card)) store.putInt("learn.card.$lesson.$card", 1)
    }

    fun markDone(lesson: Int) {
        if (!isDone(lesson)) {
            store.putInt("learn.done.$lesson", 1)
            version++
        }
    }

    /** první lekce kurzu, která ještě není hotová (všechny hotové → první lekce kurzu) */
    fun next(of: Course = Courses.byId(course)): Int = of.lessons.firstOrNull { !isDone(it.id) }?.id ?: of.lessons.first().id

    private var courseState by mutableStateOf(store.getString(K_COURSE)?.takeIf { id -> Courses.all.any { it.id == id } } ?: Courses.statics.id)

    /** naposledy vybraný kurz na úvodní obrazovce výkladu („st“ / „tg“) */
    var course: String
        get() = courseState
        set(v) {
            if (v == courseState) return
            courseState = v
            store.putString(K_COURSE, v)
        }

    /** stav pro Compose – dialog prvního spuštění se po odpovědi musí hned zavřít (i u „Ne“, které jinak nic nemění) */
    private var firstRunState by mutableStateOf(store.getInt("learn.firstRun", 0) == 1)

    var firstRunAsked: Boolean
        get() = firstRunState
        set(v) {
            firstRunState = v
            store.putInt("learn.firstRun", if (v) 1 else 0)
        }

    private companion object {
        const val K_COURSE = "learn.course"
    }
}
