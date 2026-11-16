package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.TrapText
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.exercise
import kotlin.math.PI

/** E14 – sklon v procentech ↔ úhel (A: % → °, B: ° → %). */
val E14 = ExerciseType("E14", L("Sklon v procentech", "Уклон в процентах"), Cards.SLOPE_PCT) { rng, level, seed ->
    val toAngle = level == 1 || rng.chance(0.5)
    if (toAngle) {
        val p = rng.pick(if (level < 3) listOf(5.0, 8.0, 10.0, 12.0, 15.0, 20.0, 25.0, 50.0, 100.0) else listOf(2.0, 6.0, 30.0, 75.0, 150.0))
        val q = p / 100
        val ans = Fn.TG.inv(q)
        exercise(
            "E14", seed, level,
            prompt = slopeSubject(p).let { (cs, ru) ->
                L("$cs má sklon ${n(p)} %. Jaký úhel svírá s vodorovnou? (na 1 desetinné místo)", "$ru ${n(p)} %. Какому углу к горизонтали это соответствует? (до 1 знака после запятой)")
            },
            fig = Fig.Slope(p, 100.0, "${n(p)} m", "100 m", "α = ?"),
            fields = listOf(Field(L("α", "α"), ans, "°", tol = Tol.ANGLE, digits = 1)),
            hints = listOf(
                L("Sklon v % = výška na 100 m vodorovně = tg α · 100. Procenta převeď na číslo (vyděl 100) a použij tan⁻¹ (SHIFT/2nd/INV a tan)." + PHONE_CALC.cs, "Уклон в % = высота на 100 м по горизонтали = tg α · 100. Переведи проценты в число (раздели на 100) и примени tan⁻¹ (SHIFT/2nd/INV и tan)." + PHONE_CALC.ru),
                L("α = arctg ${n(q, 3)}", "α = arctg ${n(q, 3)}"),
            ),
            traps = listOf(
                Traps.PCT_DEG.at(p),
                Traps.PCT_90.at(0.9 * p),
                Traps.PCT_100.at(Fn.TG.inv(p)),
                Traps.RAD.where(listOf(ans * PI / 180)) { v -> nearRel(v[0], ans * PI / 180) },
            ),
            ok = L("Správně: tg α = ${n(q, 3)}, α ≈ ${n(ans, 1)}°.", "Верно: tg α = ${n(q, 3)}, α ≈ ${n(ans, 1)}°."),
        )
    } else {
        val a = rng.pick(if (level == 2) listOf(5.0, 10.0, 15.0, 20.0, 30.0, 45.0) else listOf(3.0, 8.0, 35.0, 60.0))
        val ans = Fn.TG.deg(a) * 100
        exercise(
            "E14", seed, level,
            // rampa jen do 20°, strmější je střecha
            prompt = if (a <= 20) L("Rampa stoupá pod úhlem ${deg(a)}. Jaký je její sklon v procentech? (na 1 desetinné místo)", "Пандус поднимается под углом ${deg(a)}. Каков его уклон в процентах? (до 1 знака после запятой)")
            else L("Střecha svírá s vodorovnou úhel ${deg(a)}. Jaký je její sklon v procentech? (na 1 desetinné místo)", "Скат крыши составляет с горизонталью угол ${deg(a)}. Каков его уклон в процентах? (до 1 знака после запятой)"),
            fig = Fig.Slope(Fn.TG.deg(a), 1.0, null, null, deg(a)),
            fields = listOf(Field(L("sklon", "уклон"), ans, "%", tol = Tol.PERCENT, digits = 1)),
            hints = listOf(
                L("Sklon v % = tg α · 100 (výška na 100 m vodorovné délky).", "Уклон в % = tg α · 100 (высота на 100 м горизонтальной длины)."),
                L("tg ${deg(a)} · 100 = ${n(Fn.TG.deg(a), 3)} · 100", "tg ${deg(a)} · 100 = ${n(Fn.TG.deg(a), 3)} · 100"),
            ),
            traps = listOf(
                Traps.PCT_DEG.at(a),
                TrapText("SINPCT", L("Sklon se měří k **vodorovné** délce – to je tangens, ne sinus.", "Уклон измеряют относительно **горизонтальной** длины — это тангенс, а не синус."), Cards.SLOPE_PCT).at(Fn.SIN.deg(a) * 100),
                Traps.PCT_90.at(a / 0.9),
            ),
            ok = L("Správně: tg ${deg(a)} · 100 ≈ ${n(ans, 1)} %.", "Верно: tg ${deg(a)} · 100 ≈ ${n(ans, 1)} %."),
        )
    }
}

/** kdo má takový sklon: silnice do 15 %, svah do 40 %, strmější střecha („Silnice má sklon 8 %“ / «Уклон дороги 8 %») */
private fun slopeSubject(p: Double): Pair<String, String> = when {
    p <= 15 -> "Silnice" to "Уклон дороги"
    p <= 40 -> "Svah" to "Уклон откоса"
    else -> "Střecha" to "Уклон крыши"
}
