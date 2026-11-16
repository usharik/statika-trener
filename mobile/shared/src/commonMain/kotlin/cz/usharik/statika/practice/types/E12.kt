package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.exercise
import kotlin.math.PI
import kotlin.math.sqrt

/** E12 – úhel ze dvou stran (arcsin / arccos / arctg). */
val E12 = ExerciseType("E12", L("Úhel ze stran", "Угол по сторонам"), Cards.WHICH_INV) { rng, level, seed ->
    val adj: Double
    val opp: Double
    if (level == 1) {
        val (p, q, _) = rng.pick(TRIPLES_EASY)
        val sw = rng.chance(0.5)
        adj = (if (sw) q else p).toDouble(); opp = (if (sw) p else q).toDouble()
    } else {
        adj = rng.int(2, 15).toDouble(); opp = rng.int(2, 15).toDouble()
    }
    val hyp = sqrt(adj * adj + opp * opp)
    // která dvojice stran je zadaná; u přepony je třeba celé číslo → u úrovně 2+ ji zaokrouhlíme a dopočítáme úhel z ní
    val fn = if (level == 1) Fn.TG else rng.pick(Fn.entries)
    val h = if (fn == Fn.TG) hyp else if (level == 1) hyp else kotlin.math.round(hyp).coerceAtLeast(kotlin.math.max(adj, opp) + 1)
    val (x, y) = when (fn) {
        Fn.SIN -> opp to h
        Fn.COS -> adj to h
        Fn.TG -> opp to adj
    }
    val q = x / y
    val ans = fn.inv(q)
    val labels = when (fn) {
        Fn.SIN -> mapOf(TriSide.OPP to "${n(opp)} m", TriSide.HYP to "${n(h)} m")
        Fn.COS -> mapOf(TriSide.ADJ to "${n(adj)} m", TriSide.HYP to "${n(h)} m")
        Fn.TG -> mapOf(TriSide.OPP to "${n(opp)} m", TriSide.ADJ to "${n(adj)} m")
    }
    // obrázek podle skutečného úhlu (u zaokrouhlené přepony se úhel trochu liší od adj/opp)
    val figAdj = Fn.COS.deg(ans)
    val figOpp = Fn.SIN.deg(ans)
    val ctx = if (level == 3) rng.int(0, 2) else 3
    val prompt = when (ctx) {
        0 -> L("Žebřík (přepona) a zeď tvoří pravoúhlý trojúhelník se stranami podle obrázku. Jaký úhel α svírá žebřík se zemí? (na 1 desetinné místo)", "Лестница (гипотенуза) и стена образуют прямоугольный треугольник со сторонами по рисунку. Какой угол α лестница составляет с землёй? (до 1 знака после запятой)")
        1 -> L("Lano je napnuté mezi body podle obrázku. Jaký úhel α svírá s vodorovnou? (na 1 desetinné místo)", "Трос натянут между точками по рисунку. Какой угол α он составляет с горизонталью? (до 1 знака после запятой)")
        2 -> L("Střecha má rozměry podle obrázku. Jaký úhel α svírá s vodorovnou? (na 1 desetinné místo)", "Размеры крыши — по рисунку. Какой угол α она составляет с горизонталью? (до 1 знака после запятой)")
        else -> L("Známe dvě strany podle obrázku. Jak velký je úhel α? (na 1 desetinné místo)", "Известны две стороны (по рисунку). Чему равен угол α? (до 1 знака после запятой)")
    }
    val (xn, yn) = when (fn) {
        Fn.SIN -> L("protilehlá a přepona", "противолежащий и гипотенуза") to "sin"
        Fn.COS -> L("přilehlá a přepona", "прилежащий и гипотенуза") to "cos"
        Fn.TG -> L("obě odvěsny", "оба катета") to "tg"
    }
    exercise(
        "E12", seed, level, prompt,
        fig = Fig.Tri(figAdj, figOpp, labels = labels, angle = "α"),
        fields = listOf(Field(L("α", "α"), ans, "°", tol = Tol.ANGLE, digits = 1)),
        hints = listOf(
            L(
                "Znáš ${xn.cs} → funkce $yn. Úhel = ${fn.arc} (podíl), na kalkulačce ${fn.calcInv}: SHIFT (2nd, INV) a ${if (fn == Fn.TG) "tan" else fn.label}." + PHONE_CALC.cs,
                "Известны ${xn.ru} → функция $yn. Угол = ${fn.arc} (отношение), на калькуляторе ${fn.calcInv}: SHIFT (2nd, INV) и ${if (fn == Fn.TG) "tan" else fn.label}." + PHONE_CALC.ru,
            ),
            L("α = ${fn.arc} (${n(x)} / ${n(y)}) = ${fn.arc} ${n(q, 3)}", "α = ${fn.arc} (${n(x)} / ${n(y)}) = ${fn.arc} ${n(q, 3)}"),
        ),
        traps = buildList {
            // u sin/cos dá druhý ostrý úhel i jiná obrácená funkce (cos⁻¹ místo sin⁻¹) – rada musí pokrýt obojí
            add(
                if (fn == Fn.TG) Traps.COMPL.at(90 - ans)
                else Traps.COMPL.with(
                    L(
                        "To je druhý ostrý úhel. Buď je ve zlomku druhá odvěsna, nebo jiná funkce: ${if (fn == Fn.SIN) "protilehlá a přepona → sin⁻¹" else "přilehlá a přepona → cos⁻¹"}.",
                        "Это второй острый угол. Либо в дроби другой катет, либо не та функция: ${if (fn == Fn.SIN) "противолежащий и гипотенуза → sin⁻¹" else "прилежащий и гипотенуза → cos⁻¹"}.",
                    ),
                ).at(90 - ans),
            )
            for (g in Fn.entries) if (g != fn && (g == Fn.TG || q <= 1)) add(Traps.WRONGINV.at(g.inv(q)))
            add(ratioOrRad(q, ans, fn).where(listOf(q)) { v -> nearRel(v[0], q) })
            add(Traps.FWD.where(listOf(fn.deg(q))) { v -> nearRel(v[0], fn.deg(q)) })
            add(Traps.RAD.where(listOf(ans * PI / 180)) { v -> nearRel(v[0], ans * PI / 180) })
            add(Traps.GRAD.at(ans * 10 / 9))
        },
        ok = L("Správně: α = ${fn.arc} ${n(q, 3)} ≈ ${n(ans, 1)}°.", "Верно: α = ${fn.arc} ${n(q, 3)} ≈ ${n(ans, 1)}°."),
    )
}
