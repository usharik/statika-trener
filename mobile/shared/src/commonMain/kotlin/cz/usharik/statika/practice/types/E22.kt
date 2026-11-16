package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.exercise

/** E22 – součet vodorovných a svislých složek dvou sil (se znaménky). */
val E22 = ExerciseType("E22", L("Součet složek", "Сумма составляющих"), Cards.SUM, levels = 2..3) { rng, level, seed ->
    val f1 = rng.pick(listOf(10.0, 20.0))
    val a1 = rng.pick(listOf(30.0, 45.0, 60.0))
    val q1 = rng.int(0, 3)
    val phi1 = listOf(a1, 180 - a1, 180 + a1, 360 - a1)[q1]
    val f2 = rng.int(2, 12).toDouble()
    val phi2: Double
    val a2: Double
    if (level == 2) {
        phi2 = rng.pick(listOf(0.0, 90.0, 180.0, 270.0))
        a2 = 0.0
    } else {
        a2 = rng.pick(listOf(30.0, 45.0, 60.0))
        phi2 = listOf(a2, 180 - a2, 180 + a2, 360 - a2)[rng.int(0, 3)]
    }
    val x1 = f1 * Fn.COS.deg(phi1)
    val y1 = f1 * Fn.SIN.deg(phi1)
    val x2 = f2 * Fn.COS.deg(phi2)
    val y2 = f2 * Fn.SIN.deg(phi2)
    val sx = x1 + x2
    val sy = y1 + y2
    fun dirText(phi: Double): L = when {
        phi == 0.0 -> L("vodorovně doprava", "горизонтально вправо")
        phi == 90.0 -> L("svisle nahoru", "вертикально вверх")
        phi == 180.0 -> L("vodorovně doleva", "горизонтально влево")
        phi == 270.0 -> L("svisle dolů", "вертикально вниз")
        phi < 90 -> L("šikmo doprava nahoru", "наклонно вправо вверх")
        phi < 180 -> L("šikmo doleva nahoru", "наклонно влево вверх")
        phi < 270 -> L("šikmo doleva dolů", "наклонно влево вниз")
        else -> L("šikmo doprava dolů", "наклонно вправо вниз")
    }
    fun angleText(a: Double): L = if (a == 0.0) L("", "") else L(" pod úhlem ${deg(a)} k vodorovné", " под углом ${deg(a)} к горизонтали")
    val sgn = { v: Double -> if (v < 0) -1.0 else 1.0 }
    // 2. nápověda jako zápis do sešitu: „Σ[[F_x]] = 10 · cos 30° − 3“ (nulové členy vynechat, bez „+ (−3)“)
    fun term(f: Double, a: Double, comp: Double, fn: String): String? = when {
        kotlin.math.abs(comp) < 1e-9 -> null
        a == 0.0 -> n(kotlin.math.abs(f))
        else -> "${n(f)} · $fn ${deg(a)}"
    }
    fun sum(t1: String?, c1: Double, t2: String?, c2: Double): String {
        val parts = listOfNotNull(t1?.let { c1 to it }, t2?.let { c2 to it })
        if (parts.isEmpty()) return "0"
        return parts.mapIndexed { i, (c, t) -> (if (c < 0) (if (i == 0) "−" else " − ") else if (i == 0) "" else " + ") + t }.joinToString("")
    }
    val hx = sum(term(f1, a1, x1, "cos"), x1, term(f2, a2, x2, "cos"), x2)
    val hy = sum(term(f1, a1, y1, "sin"), y1, term(f2, a2, y2, "sin"), y2)
    exercise(
        "E22", seed, level,
        prompt = L(
            "Na bod působí [[F_1]] = ${n(f1)} kN ${dirText(phi1).cs}${angleText(a1).cs} a [[F_2]] = ${n(f2)} kN ${dirText(phi2).cs}${angleText(a2).cs}. Urči Σ[[F_x]] a Σ[[F_y]] se znaménky.",
            "На точку действуют [[F_1]] = ${n(f1)} кН ${dirText(phi1).ru}${angleText(a1).ru} и [[F_2]] = ${n(f2)} кН ${dirText(phi2).ru}${angleText(a2).ru}. Найди Σ[[F_x]] и Σ[[F_y]] со знаками.",
        ),
        fig = Fig.Sum(listOf(Fig.Force(f1, phi1, a1, label = "F_1"), Fig.Force(f2, phi2, a2, label = "F_2"))),
        fields = listOf(
            // tolerance ze součtu velikostí sčítanců: zaokrouhlené složky (8,66 vs 8,7) se nesmí trestat jen proto, že se odečítají
            Field(L("Σ[[F_x]]", "Σ[[F_x]]"), sx, "kN", signed = true, scale = kotlin.math.abs(x1) + kotlin.math.abs(x2)),
            Field(L("Σ[[F_y]]", "Σ[[F_y]]"), sy, "kN", signed = true, scale = kotlin.math.abs(y1) + kotlin.math.abs(y2)),
        ),
        hints = listOf(
            L("Rozlož každou šikmou sílu na složky (u úhlu cos, naproti sin) a sečti zvlášť vodorovné a zvlášť svislé – doprava a nahoru +.", "Разложи каждую наклонную силу на составляющие (у угла cos, напротив sin) и сложи отдельно горизонтальные и вертикальные — вправо и вверх +."),
            L("Σ[[F_x]] = $hx; Σ[[F_y]] = $hy", "Σ[[F_x]] = $hx; Σ[[F_y]] = $hy"),
        ),
        traps = buildList {
            // celá šikmá síla místo složky (v obou směrech)
            add(Traps.WHOLE.at(sgn(x1) * f1 + x2, sgn(y1) * f1 + y2))
            add(Traps.WHOLE.at(sgn(x1) * f1 + x2, null))
            add(Traps.SWAP.at(sgn(x1) * kotlin.math.abs(y1) + x2, sgn(y1) * kotlin.math.abs(x1) + y2))
            if (x2 != 0.0) add(Traps.SIGN_X.with(L("Zkontroluj znaménko vodorovné složky [[F_2]]: doprava +, doleva −.", "Проверь знак горизонтальной составляющей [[F_2]]: вправо +, влево −.")).at(x1 - x2, null))
            if (y2 != 0.0) add(Traps.SIGN_Y.with(L("Zkontroluj znaménko svislé složky [[F_2]]: nahoru +, dolů −.", "Проверь знак вертикальной составляющей [[F_2]]: вверх +, вниз −.")).at(null, y1 - y2))
            add(Traps.SIGN_X.with(L("Zkontroluj znaménko vodorovné složky [[F_1]]: doprava +, doleva −.", "Проверь знак горизонтальной составляющей [[F_1]]: вправо +, влево −.")).at(-x1 + x2, null))
            add(Traps.SIGN_Y.with(L("Zkontroluj znaménko svislé složky [[F_1]]: nahoru +, dolů −.", "Проверь знак вертикальной составляющей [[F_1]]: вверх +, вниз −.")).at(null, -y1 + y2))
        },
        ok = L("Správně: Σ[[F_x]] ${approx(sx)} kN, Σ[[F_y]] ${approx(sy)} kN.", "Верно: Σ[[F_x]] ${approx(sx)} кН, Σ[[F_y]] ${approx(sy)} кН."),
        solvedFig = Fig.Sum(listOf(Fig.Force(f1, phi1, a1, label = "F_1", components = true), Fig.Force(f2, phi2, a2, label = "F_2"))),
    )
}
