package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.exercise

/** E01 – název zvýrazněné strany vzhledem k vyznačenému úhlu (trojúhelník různě otočený). */
val E01 = ExerciseType("E01", L("Názvy stran", "Названия сторон"), Cards.NAME_SIDES) { rng, level, seed ->
    // tvary, ze kterých se název strany nedá uhodnout podle délky
    val (adj, opp) = rng.pick(listOf(3.0 to 4.0, 4.0 to 3.0, 5.0 to 12.0, 12.0 to 5.0, 2.0 to 5.0, 4.0 to 4.0))
    val rot = if (level == 1) 0 else rng.int(0, 3)
    val mirror = rng.chance(0.5)
    val target = rng.pick(TriSide.entries)
    val names = mapOf(
        TriSide.HYP to L("přepona", "гипотенуза"),
        TriSide.ADJ to L("přilehlá odvěsna", "прилежащий катет"),
        TriSide.OPP to L("protilehlá odvěsna", "противолежащий катет"),
    )
    fun why(chosen: TriSide): L = when {
        chosen == TriSide.HYP -> L(
            "Přepona leží naproti pravému úhlu. Zvýrazněná strana ale k pravému úhlu přiléhá – je to odvěsna.",
            "Гипотенуза лежит напротив прямого угла. А выделенная сторона прилегает к прямому углу — это катет.",
        )
        target == TriSide.HYP -> L(
            "Zvýrazněná strana leží naproti pravému úhlu (značka ⦜) a je nejdelší. Jak se jí říká?",
            "Выделенная сторона лежит напротив прямого угла (значок ⦜) и она самая длинная. Как она называется?",
        )
        target == TriSide.ADJ -> L(
            "Zvýrazněná strana vychází přímo z vrcholu úhlu α – dotýká se ho. Je u úhlu, ne naproti.",
            "Выделенная сторона выходит прямо из вершины угла α — она его касается. Она у угла, а не напротив.",
        )
        else -> L(
            "Zvýrazněná strana se vrcholu úhlu α nedotýká – leží naproti němu.",
            "Выделенная сторона не касается вершины угла α — она лежит напротив него.",
        )
    }
    // pořadí možností vždy zamíchané – jinak by se dalo pamatovat „vždy ta prostřední“
    val order = rng.shuffle(TriSide.entries.toList())
    exercise(
        "E01", seed, level,
        prompt = L(
            "Jak se jmenuje **zvýrazněná** strana vzhledem k vyznačenému úhlu α?",
            "Как называется **выделенная** сторона по отношению к отмеченному углу α?",
        ),
        fig = Fig.Tri(adj, opp, rot, mirror, highlight = target, angle = "α"),
        choice = order.map { s -> Opt(names.getValue(s), correct = s == target, why = if (s == target) null else why(s)) },
        hints = listOf(
            L("Najdi pravý úhel – naproti němu je přepona. Zbylé dvě strany jsou odvěsny.", "Найди прямой угол — напротив него гипотенуза. Две остальные стороны — катеты."),
            L(
                "Je zvýrazněná strana odvěsna (svírá pravý úhel)? Pak: vychází-li z vrcholu úhlu α, je přilehlá; jinak protilehlá. Strana naproti pravému úhlu je přepona.",
                "Выделенная сторона — катет (образует прямой угол)? Тогда: если она выходит из вершины угла α — прилежащий, иначе противолежащий. Сторона напротив прямого угла — гипотенуза.",
            ),
        ),
        ok = L("Správně – je to ${names.getValue(target).cs}.", "Верно — это ${names.getValue(target).ru}."),
    )
}
