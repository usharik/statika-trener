package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.exercise
import kotlin.math.sqrt

/** E15 – převýšení (a délka po sklonu) z procent a vodorovné délky. */
val E15 = ExerciseType("E15", L("Převýšení ze sklonu", "Превышение по уклону"), Cards.SLOPE_PCT) { rng, level, seed ->
    // úroveň 1: rampa a silnice (jen převýšení); od úrovně 2 i délka po svahu – ta má smysl jen u strmějších svahů
    // (u 10 % je šikmá délka skoro stejná jako vodorovná a „10“ by prošlo i bez Pythagorovy věty)
    val two = level >= 2
    val p = rng.pick(if (two) listOf(20.0, 25.0, 30.0, 40.0, 50.0, 60.0, 75.0) else listOf(5.0, 8.0, 10.0, 12.0, 15.0, 20.0))
    val d = rng.pick(if (two) listOf(2.0, 3.0, 4.0, 5.0, 6.0, 8.0) else listOf(2.0, 4.0, 5.0, 6.0, 10.0, 12.0, 20.0))
    val h = d * p / 100
    val s = sqrt(d * d + h * h)
    exercise(
        "E15", seed, level,
        prompt = L(
            if (two) "Svah má sklon ${n(p)} % a vodorovnou délku ${n(d)} m. O kolik stoupá a jak dlouhý je po šikmé ploše?"
            else "Rampa má sklon ${n(p)} % a vodorovnou délku ${n(d)} m. O kolik vystoupá?",
            if (two) "Уклон откоса ${n(p)} %, горизонтальная длина ${n(d)} м. На сколько он поднимается и какова его длина по склону?"
            else "Уклон пандуса ${n(p)} %, горизонтальная длина ${n(d)} м. На сколько он поднимается?",
        ),
        fig = Fig.Slope(h, d, "?", "${n(d)} m", "${n(p)} %"),
        // převýšení bývá malé (0,10 m) – tolerance 1 % bez spodní hranice 0,02 m
        fields = listOf(Field(L("převýšení", "превышение"), h, "m", tol = Tol.REL, digits = 3)) +
            (if (two) listOf(Field(L("šikmá délka", "длина по склону"), s, "m", tol = Tol.REL, digits = 3)) else emptyList()),
        hints = listOf(
            L("${n(p)} % znamená ${n(p)} m výšky na 100 m délky: převýšení = délka · ${n(p)}/100." + (if (two) " Šikmá délka je přepona: √(d² + h²)." else ""),
                "${n(p)} % означает ${n(p)} м высоты на 100 м длины: превышение = длина · ${n(p)}/100." + (if (two) " Длина по склону — гипотенуза: √(d² + h²)." else "")),
            L("h = ${n(d)} · ${n(p / 100, 2)}" + (if (two) "; s = √(${n(d)}² + h²)" else ""), "h = ${n(d)} · ${n(p / 100, 2)}" + (if (two) "; s = √(${n(d)}² + h²)" else "")),
        ),
        traps = buildList {
            add(Traps.PCT_100.at(d * p))
            if (two) add(Traps.SUM.at(null, d + h))
        },
        ok = L("Správně: převýšení ${approx(h, 3)} m" + (if (two) ", šikmá délka ${approx(s, 3)} m." else "."), "Верно: превышение ${approx(h, 3)} м" + (if (two) ", длина по склону ${approx(s, 3)} м." else ".")),
    )
}
