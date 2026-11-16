package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.exercise

/** E06 – hodnota funkce pro známý úhel (0°, 30°, 45°, 60°, 90°) bez kalkulačky – volba z možností. */
val E06 = ExerciseType("E06", L("Známé úhly", "Известные углы"), Cards.SQRT_TABLE) { rng, level, seed ->
    val fn = when (level) {
        1 -> rng.pick(listOf(Fn.SIN, Fn.COS))
        else -> rng.pick(Fn.entries)
    }
    val angles = if (fn == Fn.TG && level < 3) listOf(0, 30, 45, 60) else listOf(0, 30, 45, 60, 90)
    val a = rng.pick(angles)
    val correct = value(fn, a)
    val comp = value(fn, 90 - a)
    val pool = rng.shuffle(VALUES.filter { it != correct && it != comp })
    val labels = (listOf(correct) + (if (comp != correct) listOf(comp) else emptyList()) + pool).take(4)
    val opts = rng.shuffle(labels).map { v ->
        Opt(
            text(v), correct = v == correct,
            why = when {
                v == correct -> null
                v == comp -> L(
                    "To je hodnota pro ${90 - a}°. Tabulky sin a cos jsou navzájem pozpátku: sin $a° = cos ${90 - a}°.",
                    "Это значение для ${90 - a}°. Таблицы sin и cos зеркальны: sin $a° = cos ${90 - a}°.",
                ).takeIf { fn != Fn.TG } ?: L("To je tg ${90 - a}°. U tangentu se hodnoty s rostoucím úhlem zvětšují: tg 0° = 0, tg 45° = 1.", "Это tg ${90 - a}°. Тангенс с ростом угла растёт: tg 0° = 0, tg 45° = 1.")
                fn == Fn.TG -> L(
                    "Tahle hodnota patří k jinému úhlu. Tangens roste: tg 0° = 0, tg 30° ≈ 0,577, tg 45° = 1, tg 60° ≈ 1,732, tg 90° není definován.",
                    "Это значение другого угла. Тангенс растёт: tg 0° = 0, tg 30° ≈ 0,577, tg 45° = 1, tg 60° ≈ 1,732, tg 90° не определён.",
                )
                else -> L(
                    "Tahle hodnota patří k jinému úhlu. Najdi úhel v řadě 0 – 0,5 – 0,707 – 0,866 – 1 (sin pro 0°, 30°, 45°, 60°, 90°; cos pozpátku).",
                    "Это значение другого угла. Найди угол в ряду 0 – 0,5 – 0,707 – 0,866 – 1 (sin для 0°, 30°, 45°, 60°, 90°; cos — в обратном порядке).",
                )
            },
        )
    }
    exercise(
        "E06", seed, level,
        prompt = L("Bez kalkulačky: kolik je ${fn.label} $a°?", "Без калькулятора: чему равен ${fn.label} $a°?"),
        fig = Fig.Tri(if (a in 1..89) kotlin.math.cos(a * kotlin.math.PI / 180) else 1.0, if (a in 1..89) kotlin.math.sin(a * kotlin.math.PI / 180) else 0.6, angle = "$a°").takeIf { a in 1..89 },
        choice = opts,
        hints = listOf(
            if (fn == Fn.TG) L("tg α = sin α / cos α. V trojúhelníku 45°–45°–90° jsou odvěsny stejné, v 30°–60°–90° mají strany 1, √3 a 2.", "tg α = sin α / cos α. В треугольнике 45°–45°–90° катеты равны, в 30°–60°–90° стороны 1, √3 и 2.")
            else L("Řada (√0)/2, (√1)/2, (√2)/2, (√3)/2, (√4)/2 patří k 0°, 30°, 45°, 60°, 90° – pro sin zleva, pro cos zprava. √0 = 0, √1 = 1, √4 = 2.", "Ряд (√0)/2, (√1)/2, (√2)/2, (√3)/2, (√4)/2 соответствует 0°, 30°, 45°, 60°, 90° — для sin слева, для cos справа. √0 = 0, √1 = 1, √4 = 2."),
            when {
                // 0° a 90°: trojúhelník „slehne“
                a == 0 || a == 90 -> L(
                    "Při 0° trojúhelník „slehne“: protilehlá odvěsna je 0 a přilehlá splyne s přeponou. Při 90° je to obráceně." + (if (fn == Fn.TG && a == 90) " Tangens by dělil nulou – není definován." else ""),
                    "При 0° треугольник „складывается“: противолежащий катет равен 0, а прилежащий сливается с гипотенузой. При 90° — наоборот." + (if (fn == Fn.TG && a == 90) " Тангенс делил бы на ноль — он не определён." else ""),
                )
                fn == Fn.TG -> L(
                    "tg 30° = kratší odvěsna / delší = 1/√3 ≈ 0,577 (píše se i √3/3); tg 45° = 1; tg 60° = delší / kratší = √3 ≈ 1,732.",
                    "tg 30° = короткий катет / длинный = 1/√3 ≈ 0,577 (пишут и √3/3); tg 45° = 1; tg 60° = длинный / короткий = √3 ≈ 1,732.",
                )
                else -> L("V trojúhelníku 30°–60°–90° jsou strany 1, √3, 2; v 45°–45°–90° jsou 1, 1, √2.", "В треугольнике 30°–60°–90° стороны 1, √3, 2; в 45°–45°–90° — 1, 1, √2.")
            },
        ),
        ok = L("Správně: ${fn.label} $a° = ${text(correct).cs}.", "Верно: ${fn.label} $a° = ${text(correct).ru}."),
    )
}

/** hodnoty jako značky; „X“ = neexistuje */
private val VALUES = listOf("0", "1/2", "√2/2", "√3/2", "1", "√3/3", "√3", "X")

private fun value(fn: Fn, a: Int): String = when (fn) {
    Fn.SIN -> mapOf(0 to "0", 30 to "1/2", 45 to "√2/2", 60 to "√3/2", 90 to "1").getValue(a)
    Fn.COS -> mapOf(0 to "1", 30 to "√3/2", 45 to "√2/2", 60 to "1/2", 90 to "0").getValue(a)
    Fn.TG -> mapOf(0 to "0", 30 to "√3/3", 45 to "1", 60 to "√3", 90 to "X").getValue(a)
}

private fun text(v: String): L = when (v) {
    "X" -> L("není definován", "не определён")
    "1/2" -> L("1/2 = 0,5", "1/2 = 0,5")
    "√2/2" -> L("√2/2 ≈ 0,707", "√2/2 ≈ 0,707")
    "√3/2" -> L("√3/2 ≈ 0,866", "√3/2 ≈ 0,866")
    "√3/3" -> L("√3/3 ≈ 0,577", "√3/3 ≈ 0,577")
    "√3" -> L("√3 ≈ 1,732", "√3 ≈ 1,732")
    else -> L(v, v)
}
