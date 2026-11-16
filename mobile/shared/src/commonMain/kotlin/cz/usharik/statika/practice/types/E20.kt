package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.exercise
import kotlin.math.abs

/** E20 – složky síly v prutu se sklonem zadaným poměrem (bez kalkulačky, přes přeponu sklonového trojúhelníku). */
val E20 = ExerciseType("E20", L("Složky ze sklonu", "Составляющие по уклону"), Cards.SLOPE_RATIO) { rng, level, seed ->
    val (m, nn, c) = rng.pick(listOf(Triple(3, 4, 5), Triple(4, 3, 5), Triple(5, 12, 13), Triple(8, 15, 17)).let { if (level == 1) it.take(2) else it })
    val f = if (level < 3) c * rng.int(1, 5).toDouble() else c * rng.int(1, 4) + rng.int(1, c - 1).toDouble()
    val fx = f * nn / c
    val fy = f * m / c
    exercise(
        "E20", seed, level,
        prompt = L(
            "Síla [[F]] = ${n(f)} kN působí v prutu se sklonem $m : $nn (výška : vodorovná délka). Urči velikosti vodorovné a svislé složky – " +
                if (level < 3) "bez kalkulačky." else "bez počítání úhlu (na 2 desetinná místa).",
            "Сила [[F]] = ${n(f)} кН действует в стержне с уклоном $m : $nn (высота : горизонтальная длина). Найди величины горизонтальной и вертикальной составляющих — " +
                if (level < 3) "без калькулятора." else "не вычисляя угол (до 2 знаков после запятой).",
        ),
        fig = Fig.Slope(m.toDouble(), nn.toDouble(), "$m", "$nn", null),
        fields = listOf(Field(L("[[F_x]]", "[[F_x]]"), fx, "kN"), Field(L("[[F_y]]", "[[F_y]]"), fy, "kN")),
        hints = listOf(
            L(
                "Úhel nepotřebuješ. Sklon $m : $nn je trojúhelník s odvěsnami $m a $nn – dopočítej jeho přeponu a z ní sin α a cos α.",
                "Угол не нужен. Уклон $m : $nn — это треугольник с катетами $m и $nn: найди его гипотенузу, а по ней sin α и cos α.",
            ),
            L(
                "přepona = √($m² + $nn²) = $c; cos α = $nn/$c, sin α = $m/$c → [[F_x]] = ${n(f)} · $nn/$c; [[F_y]] = ${n(f)} · $m/$c",
                "гипотенуза = √($m² + $nn²) = $c; cos α = $nn/$c, sin α = $m/$c → [[F_x]] = ${n(f)} · $nn/$c; [[F_y]] = ${n(f)} · $m/$c",
            ),
        ),
        traps = listOf(
            Traps.SWAP.with(L("Složky jsou prohozené: vodorovná složka patří k vodorovné délce ($nn), svislá k výšce ($m).", "Составляющие перепутаны: горизонтальная относится к горизонтальной длине ($nn), вертикальная — к высоте ($m).")).at(fy, fx),
            Traps.TGRATIO.at(f * nn / m, f * m / nn),
            Traps.FULL.where(listOf(f, f)) { v -> v.any { it != null && close2(abs(it), f) } },
        ),
        ok = L("Správně: [[F_x]] ${approx(fx)} kN, [[F_y]] ${approx(fy)} kN.", "Верно: [[F_x]] ${approx(fx)} кН, [[F_y]] ${approx(fy)} кН."),
    )
}
