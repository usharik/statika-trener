package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.model.fmt
import cz.usharik.statika.practice.AngleFrom
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.Trig
import cz.usharik.statika.practice.exercise
import kotlin.math.abs
import kotlin.math.max

/**
 * E17 – složky šikmé síly (hlavní typ pro statiku). Úroveň 1: známé úhly, pole bez znaménka (jako krok „Zatížení“ v trenažéru);
 * 2: úhly po 5°, čtyři směry, se znaménkem; 3: úhel od svislice nebo φ od kladné poloosy x.
 * [E17S] = totéž, ale se znaménkem už od lehké úrovně (série „Znaménka složek“ po lekci o znaménkách).
 */
val E17 = componentsType("E17", signedFromStart = false)
val E17S = componentsType("E17S", signedFromStart = true)

private fun componentsType(id: String, signedFromStart: Boolean) = ExerciseType(
    id, L("Složky síly", "Составляющие силы"), 107 to 2,
) { rng, level, seed ->
    val f = when (level) {
        1 -> rng.pick(listOf(10.0, 20.0, 40.0))
        else -> rng.int(2, 15) * 2.0
    }
    val quadrant = rng.int(0, 3) // 0 ↗, 1 ↖, 2 ↙, 3 ↘
    val mode = if (level < 3) AngleFrom.HORIZONTAL else rng.pick(listOf(AngleFrom.VERTICAL, AngleFrom.PLUS_X))
    // a = ostrý úhel k vodorovné
    val a: Double
    val shown: Double
    val phi: Double
    when (mode) {
        AngleFrom.HORIZONTAL -> {
            a = (if (level == 1) rng.pick(listOf(30, 45, 60)) else rng.int(3, 15) * 5).toDouble()
            shown = a
            phi = listOf(a, 180 - a, 180 + a, 360 - a)[quadrant]
        }
        AngleFrom.VERTICAL -> {
            val b = (rng.int(2, 16) * 5).toDouble() // β od svislice
            shown = b
            a = 90 - b
            phi = listOf(a, 180 - a, 180 + a, 360 - a)[quadrant]
        }
        AngleFrom.PLUS_X -> {
            phi = rng.pick(listOf(120.0, 135.0, 150.0, 210.0, 225.0, 240.0, 300.0, 315.0, 330.0))
            shown = phi
            val r = phi % 180
            a = if (r > 90) 180 - r else r
        }
    }
    val fx = f * Trig.cosD(phi)
    val fy = f * Trig.sinD(phi)
    val sx = if (fx < 0) -1.0 else 1.0
    val sy = if (fy < 0) -1.0 else 1.0
    val signed = level >= 2 || signedFromStart
    val fs = fmt(f)
    val dir = listOf(L("šikmo nahoru doprava", "наклонно вверх вправо"), L("šikmo nahoru doleva", "наклонно вверх влево"), L("šikmo dolů doleva", "наклонно вниз влево"), L("šikmo dolů doprava", "наклонно вниз вправо"))[
        when {
            fx >= 0 && fy >= 0 -> 0
            fx < 0 && fy >= 0 -> 1
            fx < 0 -> 2
            else -> 3
        }
    ]
    val prompt = when (mode) {
        AngleFrom.HORIZONTAL -> L(
            "Síla [[F]] = $fs kN míří ${dir.cs} a s vodorovnou svírá ${fmt(shown, 0)}°. " + (if (signed) "Urči složky i se znaménky." else "Urči **velikosti** složek (bez znamének).") + (if (level == 1) given(a).cs else ""),
            "Сила [[F]] = $fs кН направлена ${dir.ru} и составляет с горизонталью ${fmt(shown, 0)}°. " + (if (signed) "Найди составляющие со знаками." else "Найди **величины** составляющих (без знаков).") + (if (level == 1) given(a).ru else ""),
        )
        AngleFrom.VERTICAL -> L(
            "Síla [[F]] = $fs kN míří ${dir.cs} a se **svislicí** svírá ${fmt(shown, 0)}°. Urči složky i se znaménky.",
            "Сила [[F]] = $fs кН направлена ${dir.ru} и составляет с **вертикалью** ${fmt(shown, 0)}°. Найди составляющие со знаками.",
        )
        AngleFrom.PLUS_X -> L(
            "Síla [[F]] = $fs kN. Její směr určuje úhel φ = ${fmt(shown, 0)}°, měřený od kladné poloosy x proti směru hodinových ručiček. Urči složky i se znaménky.",
            "Сила [[F]] = $fs кН. Её направление задано углом φ = ${fmt(shown, 0)}°, отсчитанным от положительной полуоси x против часовой стрелки. Найди составляющие со знаками.",
        )
    }
    val fig = Fig.Force(f, phi, shown, mode)
    val sgn = { s: Double -> if (!signed) "" else if (s < 0) "−" else "+" }
    val sinA = Trig.sinD(a)
    val cosA = Trig.cosD(a)
    exercise(
        id, seed, level, prompt, fig,
        fields = listOf(
            Field(L("[[F_x]]", "[[F_x]]"), if (signed) fx else abs(fx), "kN", signed),
            Field(L("[[F_y]]", "[[F_y]]"), if (signed) fy else abs(fy), "kN", signed),
        ),
        hints = listOf(
            when (mode) {
                AngleFrom.HORIZONTAL -> L("Složka u úhlu = F · cos α, složka naproti = F · sin α." + (if (signed) " Znaménko podle šipky: doprava a nahoru +." else ""), "Составляющая у угла = F · cos α, напротив — F · sin α." + (if (signed) " Знак — по стрелке: вправо и вверх +." else ""))
                // úhel od svislice: stejné pravidlo „u úhlu cos, naproti sin“ (lekce 8), bez přepočtu na úhel k vodorovné
                AngleFrom.VERTICAL -> L(
                    "Úhel β = ${fmt(shown, 0)}° leží u **svislice**: u úhlu je svislá složka → [[F_y]] = F · cos β, naproti vodorovná → [[F_x]] = F · sin β. Znaménka podle šipky.",
                    "Угол β = ${fmt(shown, 0)}° лежит у **вертикали**: у угла — вертикальная составляющая → [[F_y]] = F · cos β, напротив — горизонтальная → [[F_x]] = F · sin β. Знаки — по стрелке.",
                )
                AngleFrom.PLUS_X -> L(
                    "Nejdřív najdi ostrý úhel k vodorovné: rozdíl φ od nejbližší vodorovné poloosy (180° nebo 360°, u II. kvadrantu 180° − φ). Pak se podívej, kam šipka míří.",
                    "Сначала найди острый угол к горизонтали: разность φ и ближайшей горизонтальной полуоси (180° или 360°, во II четверти 180° − φ). Потом посмотри, куда направлена стрелка.",
                )
            },
            if (mode == AngleFrom.VERTICAL) L(
                "[[F_x]] = ${sgn(sx)}$fs · sin ${fmt(shown, 0)}°, [[F_y]] = ${sgn(sy)}$fs · cos ${fmt(shown, 0)}°",
                "[[F_x]] = ${sgn(sx)}$fs · sin ${fmt(shown, 0)}°, [[F_y]] = ${sgn(sy)}$fs · cos ${fmt(shown, 0)}°",
            ) else L(
                "[[F_x]] = ${sgn(sx)}$fs · cos ${fmt(a, 0)}°, [[F_y]] = ${sgn(sy)}$fs · sin ${fmt(a, 0)}°",
                "[[F_x]] = ${sgn(sx)}$fs · cos ${fmt(a, 0)}°, [[F_y]] = ${sgn(sy)}$fs · sin ${fmt(a, 0)}°",
            ),
        ),
        traps = buildList {
            if (mode == AngleFrom.VERTICAL) add(Traps.VERT.at(sx * f * sinA, sy * f * cosA))
            add(Traps.SWAP.at(sx * f * sinA, sy * f * cosA))
            if (signed) {
                add(Traps.SIGN_X.where(listOf(-fx, fy)) { v -> v[0] != null && v[1] != null && close2(v[0]!!, -fx) && close2(v[1]!!, fy) })
                add(Traps.SIGN_Y.where(listOf(fx, -fy)) { v -> v[0] != null && v[1] != null && close2(v[0]!!, fx) && close2(v[1]!!, -fy) })
                add(
                    Traps.SIGN_X.with(L("Velikosti sedí, ale obě znaménka jsou obráceně. Doprava a nahoru +, doleva a dolů −.", "Величины верны, но оба знака наоборот. Вправо и вверх +, влево и вниз −."))
                        .where(listOf(-fx, -fy)) { v -> v[0] != null && v[1] != null && close2(v[0]!!, -fx) && close2(v[1]!!, -fy) },
                )
            }
            add(Traps.FULL.where(listOf(f, f)) { v -> v.any { it != null && close2(abs(it), f) } })
            if (mode == AngleFrom.PLUS_X) add(Traps.RAD.at(f * Trig.cosRad(phi), f * Trig.sinRad(phi)))
            else add(Traps.RAD.at(sx * f * Trig.cosRad(a), sy * f * Trig.sinRad(a)))
        },
        generic = L("Složka u úhlu = F · cos α, složka naproti = F · sin α; znaménko podle toho, kam míří šipka.", "Составляющая у угла = F · cos α, напротив — F · sin α; знак — по направлению стрелки."),
        ok = L(
            "Správně: [[F_x]] ${approx(if (signed) fx else abs(fx))} kN, [[F_y]] ${approx(if (signed) fy else abs(fy))} kN.",
            "Верно: [[F_x]] ${approx(if (signed) fx else abs(fx))} кН, [[F_y]] ${approx(if (signed) fy else abs(fy))} кН.",
        ),
        solvedFig = fig.copy(components = true),
    )
}
