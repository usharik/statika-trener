package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.TrapText
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.exercise

/** E21 – rameno a moment šikmé síly na páce: r⊥ = r · sin α, M = F · r⊥. */
val E21 = ExerciseType("E21", L("Rameno a moment", "Плечо и момент"), Cards.ARM) { rng, level, seed ->
    val r = if (level == 1) rng.int(1, 4).toDouble() else rng.int(2, 8) / 2.0
    val f = when (level) {
        1 -> rng.pick(listOf(5.0, 10.0, 20.0))
        else -> rng.int(2, 20).toDouble()
    }
    val a = when (level) {
        1 -> rng.pick(listOf(30.0, 90.0))
        2 -> rng.pick(listOf(30.0, 45.0, 60.0))
        else -> rng.pick(listOf(20.0, 40.0, 50.0, 70.0))
    }
    val ccw = level < 3 || rng.chance(0.5)
    val signed = level == 3
    val arm = r * Fn.SIN.deg(a)
    val m = f * arm * (if (signed && !ccw) -1 else 1)
    val mAbs = f * arm
    exercise(
        "E21", seed, level,
        prompt = L(
            "Na konci páky dlouhé ${n(r)} m působí síla ${n(f)} kN pod úhlem ${deg(a)} k páce. Jak velké je rameno a moment k bodu O?" + (if (signed) " Moment se znaménkem (↺ kladně)." else ""),
            "На конце рычага длиной ${n(r)} м действует сила ${n(f)} кН под углом ${deg(a)} к рычагу. Каковы плечо и момент относительно точки O?" + (if (signed) " Момент со знаком (↺ положительный)." else ""),
        ),
        fig = Fig.Lever(r, a, f, ccw),
        fields = listOf(
            Field(L("[[r_⊥]]", "[[r_⊥]]"), arm, "m"),
            Field(L("M", "M"), m, "kNm", signed = signed),
        ),
        hints = listOf(
            L("Rameno je kolmá vzdálenost od bodu O k přímce, po které síla působí: [[r_⊥]] = r · sin α. Moment M = F · [[r_⊥]]." + (if (signed) " Znaménko: otáčí síla ↺ (+), nebo ↻ (−)?" else ""), "Плечо — расстояние по перпендикуляру от оси до линии действия силы: [[r_⊥]] = r · sin α. Момент M = F · [[r_⊥]]." + (if (signed) " Знак: сила вращает ↺ (+) или ↻ (−)?" else "")),
            L("[[r_⊥]] = ${n(r)} · sin ${deg(a)} = ${n(arm, 3)} m; M = ${n(f)} · ${n(arm, 3)}", "[[r_⊥]] = ${n(r)} · sin ${deg(a)} = ${n(arm, 3)} м; M = ${n(f)} · ${n(arm, 3)}"),
        ),
        traps = buildList {
            add(Traps.ARM_COS.at(r * Fn.COS.deg(a), null))
            add(Traps.ARM_FULL.at(null, if (signed && !ccw) -f * r else f * r))
            add(Traps.DIV.with(L("Rameno nemůže být delší než páka – r násob sinem, neděl.", "Плечо не может быть длиннее рычага — r умножай на синус, а не дели.")).at(r / Fn.SIN.deg(a), null))
            if (signed) add(TrapText("SIGNM", L("Velikost sedí. Kterým směrem síla kolem O otáčí – ↺ (+), nebo ↻ (−)?", "Величина верна. В какую сторону сила вращает вокруг O — ↺ (+) или ↻ (−)?"), Cards.ARM).at(null, -m))
        },
        ok = L("Správně: [[r_⊥]] ${approx(arm, 3)} m, M ${approx(m)} kNm.", "Верно: [[r_⊥]] ${approx(arm, 3)} м, M ${approx(m)} кН·м."),
        solvedFig = Fig.Lever(r, a, f, ccw, arm = true),
    )
}
