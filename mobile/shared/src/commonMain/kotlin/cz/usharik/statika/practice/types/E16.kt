package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.practice.AngleFrom
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.exercise

/** E16 – znaménka složek podle směru šipky (volba ze čtyř). */
val E16 = ExerciseType("E16", L("Znaménka složek", "Знаки составляющих"), Cards.SIGNS) { rng, level, seed ->
    val q = rng.int(0, 3) // 0 ↗, 1 ↖, 2 ↙, 3 ↘
    val a = (if (level == 1) rng.pick(listOf(30, 45, 60)) else rng.int(2, 16) * 5).toDouble()
    val phi = listOf(a, 180 - a, 180 + a, 360 - a)[q]
    val sx = q == 0 || q == 3
    val sy = q == 0 || q == 1
    fun lab(x: Boolean, y: Boolean): L {
        val t = "[[F_x]] ${if (x) "+" else "−"}, [[F_y]] ${if (y) "+" else "−"}"
        return L(t, t)
    }
    val combos = listOf(true to true, true to false, false to true, false to false)
    val fig = if (level == 3) Fig.Force(10.0, phi, phi, AngleFrom.PLUS_X) else Fig.Force(10.0, phi, a)
    exercise(
        "E16", seed, level,
        prompt = if (level == 3) L("Směr síly určuje úhel φ = ${deg(phi)}, měřený od kladné poloosy x proti směru hodinových ručiček. Jaká znaménka mají její složky?", "Направление силы задано углом φ = ${deg(phi)}, отсчитанным от положительной полуоси x против часовой стрелки. Какие знаки у её составляющих?")
        else L("Jaká znaménka mají složky síly na obrázku?", "Какие знаки у составляющих силы на рисунке?"),
        fig = fig,
        choice = combos.map { (x, y) ->
            Opt(
                lab(x, y), correct = x == sx && y == sy,
                why = when {
                    x == sx && y == sy -> null
                    x != sx && y != sy -> L("Obě znaménka jsou obráceně. Doprava a nahoru je +, doleva a dolů −.", "Оба знака наоборот. Вправо и вверх +, влево и вниз −.")
                    x != sx -> Traps.SIGN_X.msg
                    else -> Traps.SIGN_Y.msg
                },
            )
        },
        hints = listOf(
            L("Doprava +, doleva −, nahoru +, dolů −. Na velikosti úhlu nezáleží.", "Вправо +, влево −, вверх +, вниз −. Величина угла не важна."),
            L(
                "Hrot míří ${if (sx) "doprava" else "doleva"} a ${if (sy) "nahoru" else "dolů"}.",
                "Остриё смотрит ${if (sx) "вправо" else "влево"} и ${if (sy) "вверх" else "вниз"}.",
            ),
        ),
        ok = L("Správně: ${lab(sx, sy).cs}.", "Верно: ${lab(sx, sy).ru}."),
        solvedFig = fig.copy(components = true),
    )
}
