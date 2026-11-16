package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.exercise
import kotlin.math.PI

/** E13 – úhel ze sklonu zadaného poměrem výška : vodorovná délka. */
val E13 = ExerciseType("E13", L("Sklon poměrem", "Уклон отношением"), Cards.SLOPE_RATIO) { rng, level, seed ->
    val (m, nn) = rng.pick(
        when (level) {
            1 -> listOf(1.0 to 1.0, 1.0 to 2.0, 3.0 to 4.0, 4.0 to 3.0)
            2 -> listOf(1.0 to 3.0, 1.0 to 4.0, 1.0 to 5.0, 2.0 to 3.0, 5.0 to 12.0, 1.0 to 10.0)
            else -> listOf(1.0 to 1.5, 1.0 to 2.5, 1.0 to 20.0, 2.0 to 5.0, 3.0 to 8.0)
        },
    )
    val q = m / nn
    val ans = Fn.TG.inv(q)
    val r = "${n(m)} : ${n(nn)}"
    exercise(
        "E13", seed, level,
        prompt = L("Sklon je $r (výška : vodorovná délka). Jaký úhel svírá s vodorovnou? (na 1 desetinné místo)", "Уклон $r (высота : горизонтальная длина). Какой угол он составляет с горизонталью? (до 1 знака после запятой)"),
        fig = Fig.Slope(m, nn, n(m), n(nn), "α = ?"),
        fields = listOf(Field(L("α", "α"), ans, "°", tol = Tol.ANGLE, digits = 1)),
        hints = listOf(
            L("Poměr výška : vodorovná délka je přímo **tg α**. Úhel pak dá arctg – na kalkulačce tan⁻¹: SHIFT (2nd, INV) a tan." + PHONE_CALC.cs, "Отношение высота : горизонтальная длина — это и есть **tg α**. Угол даёт arctg — на калькуляторе tan⁻¹: SHIFT (2nd, INV) и tan." + PHONE_CALC.ru),
            L("α = arctg (${n(m)} / ${n(nn)}) = arctg ${n(q, 3)}", "α = arctg (${n(m)} / ${n(nn)}) = arctg ${n(q, 3)}"),
        ),
        traps = buildList {
            add(Traps.COMPL.with(L("To je úhel od svislice – vyjde, když se dělí délka výškou. Nahoře ve zlomku je výška.", "Это угол от вертикали — так выходит, если делить длину на высоту. Сверху в дроби стоит высота.")).at(90 - ans))
            add(ratioOrRad(q, ans, Fn.TG).where(listOf(q)) { v -> nearRel(v[0], q) })
            add(Traps.PCT_DEG.with(L("${n(100 * q, 1)} je sklon v procentech, ne úhel. Úhel = arctg ${n(q, 3)}.", "${n(100 * q, 1)} — это уклон в процентах, а не угол. Угол = arctg ${n(q, 3)}.")).at(100 * q))
            if (q <= 1) add(Traps.WRONGINV.with(L("Poměr výška : délka je tangens, ne sinus – použij tan⁻¹.", "Отношение высота : длина — это тангенс, а не синус: нужен tan⁻¹.")).at(Fn.SIN.inv(q)))
            add(Traps.RAD.where(listOf(ans * PI / 180)) { v -> nearRel(v[0], ans * PI / 180) })
        },
        ok = L("Správně: α = arctg ${n(q, 3)} ≈ ${n(ans, 1)}°.", "Верно: α = arctg ${n(q, 3)} ≈ ${n(ans, 1)}°."),
    )
}
