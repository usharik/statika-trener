package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.TrapText
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.exercise
import kotlin.math.abs

/** E05 – odvěsna z druhé odvěsny a úhlu (tangens): výška sloupu ze stínu, stoupání rampy. */
val E05 = ExerciseType("E05", L("Odvěsna z odvěsny (tangens)", "Катет по катету (тангенс)"), Cards.TANGENT, levels = 2..3) { rng, level, seed ->
    val b = if (level == 2) rng.int(2, 20).toDouble() else rng.int(4, 40) / 2.0
    val ctx = rng.int(0, 1)
    // rampa je ve skutečnosti mírná (nejvýš 20°); stín sloupu může být i pod velkým úhlem slunce
    val al = (if (ctx == 1) rng.int(1, 4) * 5 else rng.int(2, 14) * 5).toDouble()
    val tv = Fn.TG.deg(al)
    val ans = b * tv
    val q = if (ctx == 0) L(
        "Sloup vrhá na rovné zemi stín dlouhý ${n(b)} m. Slunce svítí pod úhlem ${deg(al)} nad obzorem. Jak vysoký je sloup?",
        "Столб отбрасывает на ровную землю тень длиной ${n(b)} м. Солнце светит под углом ${deg(al)} над горизонтом. Какой высоты столб?",
    ) else L(
        "Rampa je vodorovně dlouhá ${n(b)} m a stoupá pod úhlem ${deg(al)}. Do jaké výšky vede?",
        "Пандус имеет горизонтальную длину ${n(b)} м и поднимается под углом ${deg(al)}. На какую высоту он ведёт?",
    )
    val radV = Fn.TG.rad(al)
    exercise(
        "E05", seed, level, q,
        fig = Fig.Tri(b, ans, labels = mapOf(TriSide.ADJ to "${n(b)} m", TriSide.OPP to "?"), highlight = TriSide.OPP, angle = deg(al)),
        fields = listOf(Field(L("výška", "высота"), ans, "m")),
        hints = listOf(
            L("Přepona tu nehraje roli – obě strany jsou odvěsny, tedy **tangens**: tg α = protilehlá / přilehlá.", "Гипотенуза здесь не участвует — обе стороны катеты, значит **тангенс**: tg α = противолежащий / прилежащий."),
            L("${n(b)} · tg ${deg(al)} = ${n(b)} · ${n(tv, 3)}", "${n(b)} · tg ${deg(al)} = ${n(b)} · ${n(tv, 3)}"),
        ),
        traps = buildList {
            add(TrapText("DIVTG", L("Tangens = protilehlá / přilehlá. Hledáš tu nahoře – násob, neděl.", "Тангенс = противолежащий / прилежащий. Ищешь тот, что сверху, — умножай, а не дели."), Cards.TANGENT).at(b / tv))
            add(TrapText("NOHYP", L("Sinus i kosinus potřebují přeponu – ta tu ale není. Obě známé strany jsou odvěsny, tedy tangens.", "Синусу и косинусу нужна гипотенуза, а её здесь нет. Обе известные стороны — катеты, значит тангенс."), Cards.TANGENT).at(b * Fn.SIN.deg(al)))
            if (abs(radV) in 0.05..200.0) add(Traps.RAD.at(b * radV))
        },
        ok = L("Správně: ${n(b)} · tg ${deg(al)} ${approx(ans)} m.", "Верно: ${n(b)} · tg ${deg(al)} ${approx(ans)} м."),
    )
}
