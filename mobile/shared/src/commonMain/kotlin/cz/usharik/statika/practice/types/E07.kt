package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.TrapText
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.exercise

/** E07 – hodnota funkce na kalkulačce (pozor na režim DEG). */
val E07 = ExerciseType("E07", L("Funkce na kalkulačce", "Функция на калькуляторе"), Cards.CALC_MODE) { rng, level, seed ->
    val fn = rng.pick(Fn.entries)
    val special = setOf(30, 45, 60)
    var a: Int
    do {
        a = when (level) {
            1 -> rng.int(1, 17) * 5
            else -> rng.int(1, 89)
        }
    } while (a in special || (fn == Fn.TG && a > 80))
    val ad = a.toDouble()
    val ans = fn.deg(ad)
    exercise(
        "E07", seed, level,
        prompt = L(
            "Spočítej na kalkulačce ${fn.label} $a° (na 3 desetinná místa).",
            "Посчитай на калькуляторе ${fn.label} $a° (до 3 знаков после запятой).",
        ),
        fig = Fig.Calc("${if (fn == Fn.TG) "tan" else fn.label}($a)", "?", "DEG"),
        fields = listOf(Field(L("${fn.label} $a°", "${fn.label} $a°"), ans, tol = Tol.RATIO, digits = 3)),
        hints = listOf(
            L(
                "Nejdřív zkontroluj režim **DEG**: sin 30 musí dát 0,5." + (if (fn == Fn.TG) " Tangens je na kalkulačce **tan**." else "") + PHONE_CALC.cs,
                "Сначала проверь режим **DEG**: sin 30 должен дать 0,5." + (if (fn == Fn.TG) " Тангенс на калькуляторе — **tan**." else "") + PHONE_CALC.ru,
            ),
            L("Stiskni ${if (fn == Fn.TG) "tan" else fn.label} a $a (nebo $a a ${if (fn == Fn.TG) "tan" else fn.label} – podle kalkulačky).", "Нажми ${if (fn == Fn.TG) "tan" else fn.label} и $a (или $a и ${if (fn == Fn.TG) "tan" else fn.label} — зависит от калькулятора)."),
        ),
        traps = buildList {
            add(Traps.RAD.at(fn.rad(ad)))
            add(Traps.GRAD.at(fn.grad(ad)))
            for (g in Fn.entries) if (g != fn) add(
                TrapText("OTHERFN", L("To je ${g.label} $a° – jiná funkce. Zkontroluj, která klávesa byla stisknutá.", "Это ${g.label} $a° — другая функция. Проверь, какая клавиша нажата."), Cards.THREE_RATIOS).at(g.deg(ad)),
            )
        },
        ok = L("Správně: ${fn.label} $a° ≈ ${n(ans, 3)}.", "Верно: ${fn.label} $a° ≈ ${n(ans, 3)}."),
    )
}
