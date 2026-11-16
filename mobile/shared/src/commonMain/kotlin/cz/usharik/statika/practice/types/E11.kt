package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.TrapText
import cz.usharik.statika.practice.exercise

/** E11 – doplňkový úhel: sin α = cos (90° − α). */
val E11 = ExerciseType("E11", L("Doplňkový úhel", "Дополнительный угол"), Cards.COMPLEMENT) { rng, level, seed ->
    var a: Int
    do a = if (level == 3) rng.int(5, 85) else rng.int(1, 17) * 5 while (a == 45)
    val sinToCos = level == 1 || rng.chance(0.5)
    val (f1, f2) = if (sinToCos) "sin" to "cos" else "cos" to "sin"
    val ans = 90.0 - a
    exercise(
        "E11", seed, level,
        prompt = L("Doplň úhel: $f1 $a° = $f2 ?°", "Найди угол: $f1 $a° = $f2 ?°"),
        fig = Fig.Tri(kotlin.math.cos(a * kotlin.math.PI / 180), kotlin.math.sin(a * kotlin.math.PI / 180), angle = "$a°"),
        fields = listOf(Field(L("úhel", "угол"), ans, "°", tol = Tol.ANGLE, digits = 0)),
        hints = listOf(
            L("Oba ostré úhly pravoúhlého trojúhelníku dávají dohromady 90°. Co je pro jeden úhel protilehlá odvěsna, je pro druhý přilehlá.", "Два острых угла прямоугольного треугольника вместе дают 90°. Катет, противолежащий одному углу, прилежит к другому."),
            L("90° − $a°", "90° − $a°"),
        ),
        traps = listOf(
            TrapText("SAME", L("$f1 a $f2 téhož úhlu se rovnají jen při 45°. Hledáš druhý ostrý úhel.", "$f1 и $f2 одного угла равны только при 45°. Ищем второй острый угол."), Cards.COMPLEMENT).at(a.toDouble()),
            TrapText("C180", L("Doplněk je do 90°, ne do 180°.", "Дополнение — до 90°, а не до 180°."), Cards.COMPLEMENT).at(180.0 - a),
        ),
        ok = L("Správně: $f1 $a° = $f2 ${90 - a}°.", "Верно: $f1 $a° = $f2 ${90 - a}°."),
    )
}
