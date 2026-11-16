package cz.usharik.statika.practice

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.types.*

/**
 * Rejstřík typů úloh procvičování. Nový typ: soubor v practice/types/ s `val Exx = ExerciseType(...)` a řádek sem.
 * Testy (ExerciseHarnessTest) pak typ samy projdou na všech úrovních a mnoha číslech úloh.
 */
object Exercises {
    val all: List<ExerciseType> by lazy {
        listOf(
            E01, E02, E03, E04, E05, E06, E07, E08, E09, E10, E11, E12,
            E13, E14, E15, E16, E17, E17S, E18, E19, E20, E21, E22, E23,
        )
    }

    val byId: Map<String, ExerciseType> by lazy { all.associateBy { it.id } }

    /** úloha typu [id] pro číslo [seed] a úroveň [level] (deterministicky) */
    fun make(id: String, seed: Int, level: Int): Exercise = byId.getValue(id).make(cz.usharik.statika.model.Rng(seed), level, seed)
}

/**
 * Soubor úloh (série). [types] = id typu → váha při losování; typy, které ještě nejsou v [Exercises], se přeskočí.
 * [afterLesson] = doporučená lekce před sérií (jen nápis, nic se nezamyká), [lessons] = lekce, ke kterým série patří.
 */
class PracticeSet(
    val id: String,
    val title: L,
    val types: List<Pair<String, Int>>,
    val size: Int,
    val afterLesson: Int,
    val lessons: IntRange = afterLesson..afterLesson,
) {
    /** „K lekcím 1–2“ / „K lekci 5“ (čísla lekcí v kurzu) */
    val lessonsLabel: L
        get() {
            val a = lessons.first % 100
            val b = lessons.last % 100
            return if (a == b) L("K lekci $a", "К уроку $a") else L("K lekcím $a–$b", "К урокам $a–$b")
        }

    /** typy, které už existují a umí danou úroveň */
    fun available(level: Int): List<Pair<ExerciseType, Int>> =
        types.mapNotNull { (id, w) -> Exercises.byId[id]?.takeIf { level in it.levels }?.let { it to w } }
}

/** Soubory procvičování kurzu trigonometrie (návrh, oddíl 2.2). */
object PracticeSets {
    val all: List<PracticeSet> = listOf(
        PracticeSet("p1", L("Strany a podíly", "Стороны и отношения"), listOf("E01" to 1, "E02" to 1, "E03" to 1, "E04" to 1, "E05" to 1), 8, 102, 101..102),
        PracticeSet("p2", L("Kalkulačka a známé úhly", "Калькулятор и известные углы"), listOf("E06" to 1, "E07" to 1, "E08" to 1), 8, 104, 103..104),
        PracticeSet("p3", L("Pythagoras a vztahy", "Пифагор и связи"), listOf("E09" to 2, "E10" to 2, "E11" to 1), 8, 105),
        PracticeSet("p4", L("Úhel a sklon", "Угол и уклон"), listOf("E12" to 1, "E13" to 1, "E14" to 1, "E15" to 1), 8, 106),
        // po lekci o znaménkách: složky vždy se znaménkem (E17S), i na lehké úrovni
        PracticeSet("p5", L("Znaménka složek", "Знаки составляющих"), listOf("E16" to 1, "E17S" to 1, "E18" to 1), 8, 107),
        PracticeSet("p6", L("Trigonometrie ve statice", "Тригонометрия в статике"), listOf("E17" to 1, "E19" to 1, "E20" to 1, "E21" to 1, "E22" to 1, "E23" to 1), 8, 108),
        PracticeSet(
            "mix", L("Závěrečný mix", "Итоговый микс"),
            listOf(
                "E01" to 1, "E02" to 1, "E03" to 1, "E04" to 1, "E05" to 1, "E06" to 1, "E07" to 1, "E08" to 1, "E09" to 1, "E10" to 1,
                "E11" to 1, "E12" to 2, "E13" to 1, "E14" to 1, "E15" to 1, "E16" to 1, "E17" to 3, "E18" to 1, "E19" to 2, "E20" to 1,
                "E21" to 2, "E22" to 1, "E23" to 1,
            ),
            12, 108, 101..108,
        ),
    )

    fun byId(id: String): PracticeSet? = all.firstOrNull { it.id == id }

    /** série k procvičení hned po lekci [lesson] (bez závěrečného mixu) */
    fun afterLesson(lesson: Int): PracticeSet? = all.firstOrNull { it.id != "mix" && it.afterLesson == lesson }
}
