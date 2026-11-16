package cz.usharik.statika.learn

import cz.usharik.statika.learn.trig.TRIG_GLOSSARY
import cz.usharik.statika.learn.trig.trigLesson1
import cz.usharik.statika.learn.trig.trigLesson2
import cz.usharik.statika.learn.trig.trigLesson3
import cz.usharik.statika.learn.trig.trigLesson4
import cz.usharik.statika.learn.trig.trigLesson5
import cz.usharik.statika.learn.trig.trigLesson6
import cz.usharik.statika.learn.trig.trigLesson7
import cz.usharik.statika.learn.trig.trigLesson8
import cz.usharik.statika.practice.PracticeSet
import cz.usharik.statika.practice.PracticeSets

/**
 * Kurz výkladu: vlastní lekce, slovníček a (volitelně) procvičování.
 * Id lekcí jsou jednoznačná přes všechny kurzy, takže odkazy `learn(lesson, card)` i klíče postupu se nepletou.
 */
class Course(
    /** „st“ / „tg“ – ukládá se jako vybraný kurz */
    val id: String,
    val title: L,
    /** podtitulek úvodní obrazovky („9 krátkých lekcí · …“) */
    val sub: L,
    val lessons: List<Lesson>,
    val glossary: List<Pair<L, L>>,
    val practice: List<PracticeSet> = emptyList(),
)

object Courses {
    val statics: Course by lazy {
        Course(
            "st", L("Základy statiky", "Основы статики"),
            L("9 krátkých lekcí · asi 3–5 minut každá", "9 коротких уроков · примерно по 3–5 минут"),
            listOf(lesson1(), lesson2(), lesson3(), lesson4(), lesson5(), lesson6(), lesson7(), lesson8(), lesson9()),
            GLOSSARY,
        )
    }

    val trig: Course by lazy {
        Course(
            "tg", L("Základy trigonometrie", "Основы тригонометрии"),
            L("8 krátkých lekcí · asi 4–6 minut každá", "8 коротких уроков · примерно по 4–6 минут"),
            listOf(trigLesson1(), trigLesson2(), trigLesson3(), trigLesson4(), trigLesson5(), trigLesson6(), trigLesson7(), trigLesson8()),
            TRIG_GLOSSARY,
            PracticeSets.all,
        )
    }

    val all: List<Course> by lazy { listOf(statics, trig) }

    fun byId(id: String): Course = all.firstOrNull { it.id == id } ?: statics
}

/** Všechny lekce všech kurzů (kurz po kurzu, v pořadí, ve kterém na sebe navazují). */
object Lessons {
    val all: List<Lesson> by lazy { Courses.all.flatMap { it.lessons } }

    fun byId(id: Int): Lesson = all.first { it.id == id }

    fun courseOf(id: Int): Course = Courses.all.first { c -> c.lessons.any { it.id == id } }
}
