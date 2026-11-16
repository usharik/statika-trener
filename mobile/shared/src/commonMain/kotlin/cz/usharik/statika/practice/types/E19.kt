package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.exercise
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

/** E19 – výslednice ze složek: velikost (Pythagoras) a ostrý úhel k vodorovné (arctg). */
val E19 = ExerciseType("E19", L("Výslednice ze složek", "Равнодействующая по составляющим"), Cards.RESULTANT) { rng, level, seed ->
    var x: Double
    var y: Double
    when (level) {
        1 -> {
            val (p, q, _) = rng.pick(listOf(Triple(3, 4, 5), Triple(6, 8, 10), Triple(4, 3, 5), Triple(8, 6, 10)))
            x = p.toDouble(); y = q.toDouble()
        }
        2 -> {
            val (p, q, _) = rng.pick(listOf(Triple(3, 4, 5), Triple(4, 3, 5), Triple(6, 8, 10), Triple(5, 12, 13), Triple(12, 5, 13), Triple(8, 15, 17)))
            x = p * (if (rng.chance(0.5)) -1.0 else 1.0); y = q * (if (rng.chance(0.5)) -1.0 else 1.0)
        }
        else -> {
            do {
                x = rng.int(-12, 12).toDouble(); y = rng.int(-12, 12).toDouble()
            } while (x == 0.0 || y == 0.0 || abs(x) == abs(y))
        }
    }
    val ax = abs(x)
    val ay = abs(y)
    val r = sqrt(x * x + y * y)
    val a = Fn.TG.inv(ay / ax)
    val dir = when {
        x > 0 && y > 0 -> L("doprava nahoru", "вправо вверх")
        x < 0 && y > 0 -> L("doleva nahoru", "влево вверх")
        x < 0 -> L("doleva dolů", "влево вниз")
        else -> L("doprava dolů", "вправо вниз")
    }
    exercise(
        "E19", seed, level,
        prompt = L(
            "Složky síly jsou [[F_x]] = ${n(x)} kN a [[F_y]] = ${n(y)} kN. Jak velká je celá síla R a jaký ostrý úhel α svírá s vodorovnou? (úhel na 1 desetinné místo)",
            "Составляющие силы: [[F_x]] = ${n(x)} кН и [[F_y]] = ${n(y)} кН. Какова величина всей силы R и какой острый угол α она составляет с горизонталью? (угол — до 1 знака после запятой)",
        ),
        fig = Fig.Comps(x, y),
        fields = listOf(
            Field(L("R", "R"), r, "kN"),
            // ostrý úhel je vždy kladný – pole bez znaménka (záporný úhel pozná chyba NEGANG níž)
            Field(L("α", "α"), a, "°", tol = Tol.ANGLE, digits = 1),
        ),
        hints = listOf(
            L("Velikost: Pythagorova věta R = √([[F_x]]² + [[F_y]]²). Úhel: tg α = |[[F_y]]| / |[[F_x]]| – bez znamének.", "Величина: теорема Пифагора R = √([[F_x]]² + [[F_y]]²). Угол: tg α = |[[F_y]]| / |[[F_x]]| — без знаков."),
            L("R = √(${n(ax)}² + ${n(ay)}²); α = arctg (${n(ay)} / ${n(ax)})", "R = √(${n(ax)}² + ${n(ay)}²); α = arctg (${n(ay)} / ${n(ax)})"),
        ),
        traps = listOf(
            Traps.SUM.with(L("Složky se nesčítají přímo – jsou na sebe kolmé. Použij Pythagorovu větu.", "Составляющие нельзя просто сложить — они перпендикулярны. Нужна теорема Пифагора.")).at(ax + ay, null),
            Traps.NOSQRT.at(x * x + y * y, null),
            Traps.COMPL.with(L("To je úhel od svislice – vyjde, když se dělí |[[F_x]]| / |[[F_y]]|. Naproti úhlu u vodorovné leží [[F_y]].", "Это угол от вертикали — так выходит, если делить |[[F_x]]| / |[[F_y]]|. Напротив угла у горизонтали лежит [[F_y]].")).at(null, 90 - a),
            // úhel měřený od osy +x (tupý, nebo přes 180°) místo ostrého úhlu
            Traps.NEGANG.with(NEGANG_TEXT).where(listOf(null, 180 - a)) { v -> v.getOrNull(1)?.let { abs(abs(it) - (180 - a)) <= 0.5 } == true },
            Traps.NEGANG.with(NEGANG_TEXT).where(listOf(null, 180 + a)) { v -> v.getOrNull(1)?.let { abs(abs(it) - (180 + a)) <= 0.5 } == true },
            Traps.NEGANG.with(NEGANG_TEXT).where(listOf(null, 360 - a)) { v -> v.getOrNull(1)?.let { abs(abs(it) - (360 - a)) <= 0.5 } == true },
            Traps.RAD.where(listOf(null, a * PI / 180)) { v -> nearRel(v.getOrNull(1), a * PI / 180) },
        ),
        ok = L("Správně: R ${approx(r)} kN, α ≈ ${n(a, 1)}°; síla míří ${dir.cs}.", "Верно: R ${approx(r)} кН, α ≈ ${n(a, 1)}°; сила направлена ${dir.ru}."),
        solvedFig = Fig.Comps(x, y, solved = true),
    )
}

private val NEGANG_TEXT = L(
    "Do arctg dej velikosti bez znamének – hledáme ostrý (kladný) úhel k vodorovné, ne úhel od osy +x. Směr síly je vidět ze znamének složek.",
    "В arctg подставляй величины без знаков — ищем острый (положительный) угол к горизонтали, а не угол от оси +x. Направление силы видно по знакам составляющих.",
)
