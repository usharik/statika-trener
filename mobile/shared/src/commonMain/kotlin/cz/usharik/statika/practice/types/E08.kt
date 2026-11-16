package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Opt
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.exercise

/** E08 – v jakém režimu je kalkulačka (DEG / RAD / GRAD) podle výsledku známého výrazu. */
val E08 = ExerciseType("E08", L("Režim kalkulačky", "Режим калькулятора"), Cards.CALC_MODE) { rng, level, seed ->
    val (fn, a) = rng.pick(listOf(Fn.SIN to 30.0, Fn.COS to 60.0, Fn.SIN to 90.0, Fn.TG to 45.0, Fn.SIN to 60.0))
    val vals = mapOf("DEG" to fn.deg(a), "RAD" to fn.rad(a), "GRAD" to fn.grad(a))
    val mode = if (level == 1) rng.pick(listOf("RAD", "DEG")) else rng.pick(listOf("DEG", "RAD", "GRAD"))
    val key = if (fn == Fn.TG) "tan" else fn.label
    val expr = "$key ${n(a, 0)}"
    val shown = n(vals.getValue(mode), 3)
    fun why(m: String): L = when (m) {
        "DEG" -> L("Ve stupních (DEG) by $expr dalo ${n(vals.getValue("DEG"), 3)}. Na displeji je ale $shown.", "В градусах (DEG) $expr дал бы ${n(vals.getValue("DEG"), 3)}. А на дисплее $shown.")
        "RAD" -> L("V režimu RAD by $expr dalo ${n(vals.getValue("RAD"), 3)}.", "В режиме RAD $expr дал бы ${n(vals.getValue("RAD"), 3)}.")
        else -> L("V režimu GRAD (pravý úhel = 100 gradů, gonů) by $expr dalo ${n(vals.getValue("GRAD"), 3)}.", "В режиме GRAD (прямой угол = 100 град, гон) $expr дал бы ${n(vals.getValue("GRAD"), 3)}.")
    }
    // vždy všechny tři režimy – u dvou možností by se dalo hádat
    val modes = listOf("DEG", "RAD", "GRAD")
    exercise(
        "E08", seed, level,
        prompt = L("Kalkulačka ukázala $expr = $shown. V jakém je režimu?", "Калькулятор показал $expr = $shown. В каком он режиме?"),
        fig = Fig.Calc(expr, shown, ""),
        choice = modes.map { m -> Opt(L(m, m), correct = m == mode, why = if (m == mode) null else why(m)) },
        hints = listOf(
            L("Kolik má $expr vyjít ve stupních? Pamatuješ si to z tabulky známých úhlů.", "Сколько должно получиться для $expr в градусах? Это есть в таблице известных углов."),
            L("V RAD bere kalkulačka ${n(a, 0)} jako ${n(a, 0)} radiánů (několik celých otáček); v GRAD má pravý úhel 100 gradů (gonů).", "В RAD калькулятор считает ${n(a, 0)} радианами (несколько полных оборотов); в GRAD прямой угол равен 100 град (гон)."),
        ),
        ok = L("Správně – režim $mode." + (if (mode != "DEG") " Před počítáním přepni na DEG." else ""), "Верно — режим $mode." + (if (mode != "DEG") " Перед расчётом переключи на DEG." else "")),
    )
}
