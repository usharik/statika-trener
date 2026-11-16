package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.AngleFrom
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.TrapText
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.exercise

/** E18 – sin / cos libovolného úhlu (jednotková kružnice, znaménko podle kvadrantu). Jen úroveň 3. */
val E18 = ExerciseType("E18", L("Úhly přes 90°", "Углы больше 90°"), Cards.QUADRANTS, levels = 3..3) { rng, level, seed ->
    val phi = rng.pick(listOf(120.0, 135.0, 150.0, 210.0, 225.0, 240.0, 300.0, 315.0, 330.0))
    val fn = rng.pick(listOf(Fn.SIN, Fn.COS))
    val other = if (fn == Fn.SIN) Fn.COS else Fn.SIN
    val ans = fn.deg(phi)
    val r = phi % 180
    val ref = if (r > 90) 180 - r else r
    val sign = if (ans < 0) "−" else "+"
    exercise(
        "E18", seed, level,
        prompt = L("Kolik je ${fn.label} ${deg(phi)}? (i se znaménkem)", "Чему равен ${fn.label} ${deg(phi)}? (со знаком)"),
        fig = Fig.Force(1.0, phi, phi, AngleFrom.PLUS_X, label = "r", unit = ""),
        fields = listOf(Field(L("${fn.label} ${deg(phi)}", "${fn.label} ${deg(phi)}"), ans, signed = true, tol = Tol.RATIO, digits = 3)),
        hints = listOf(
            L("Najdi ostrý úhel k ose x (tady ${deg(ref)}) a znaménko podle kvadrantu: cos je vodorovná souřadnice, sin svislá.", "Найди острый угол к оси x (здесь ${deg(ref)}) и знак по четверти: cos — горизонтальная координата, sin — вертикальная."),
            L("${fn.label} ${deg(phi)} = $sign${fn.label} ${deg(ref)}", "${fn.label} ${deg(phi)} = $sign${fn.label} ${deg(ref)}"),
        ),
        traps = listOf(
            TrapText("SIGNQ", L("Velikost sedí. Na které straně osy leží bod na kružnici? Vpravo/nahoře +, vlevo/dole −.", "Величина верна. С какой стороны от оси лежит точка на окружности? Справа/сверху +, слева/снизу −."), Cards.QUADRANTS).at(-ans),
            Traps.SWAP.with(L("To je ${other.label} ${deg(phi)} – druhá souřadnice bodu. ${if (fn == Fn.COS) "Kosinus je vodorovná souřadnice." else "Sinus je svislá souřadnice."}", "Это ${other.label} ${deg(phi)} — другая координата точки. ${if (fn == Fn.COS) "Косинус — горизонтальная координата." else "Синус — вертикальная координата."}")).at(other.deg(phi)),
        ),
        ok = L("Správně: ${fn.label} ${deg(phi)} ${approx(ans, 3)}.", "Верно: ${fn.label} ${deg(phi)} ${approx(ans, 3)}."),
        solvedFig = Fig.Force(1.0, phi, phi, AngleFrom.PLUS_X, label = "r", components = true, unit = ""),
    )
}
