package cz.usharik.statika.practice.types

import cz.usharik.statika.learn.L
import cz.usharik.statika.practice.ExerciseType
import cz.usharik.statika.practice.Field
import cz.usharik.statika.practice.Fig
import cz.usharik.statika.practice.Tol
import cz.usharik.statika.practice.TrapText
import cz.usharik.statika.practice.Traps
import cz.usharik.statika.practice.TriSide
import cz.usharik.statika.practice.exercise

/** E02 – hodnota sin / cos / tg ze stran trojúhelníku (podíl jako desetinné číslo). */
val E02 = ExerciseType("E02", L("Podíl stran", "Отношение сторон"), Cards.THREE_RATIOS) { rng, level, seed ->
    // lehká úroveň: jen trojúhelníky, kde podíl vyjde zpaměti (3/5 = 0,6), ne 5/13
    val (p, q, h) = rng.pick(if (level == 1) TRIPLES_EASY.take(2) else TRIPLES_MORE)
    val k = if (level == 1) 1 else rng.int(1, 2)
    val swap = rng.chance(0.5)
    val adj = (if (swap) q else p) * k.toDouble()
    val opp = (if (swap) p else q) * k.toDouble()
    val hyp = h * k.toDouble()
    val fn = if (level == 1) rng.pick(listOf(Fn.SIN, Fn.COS)) else rng.pick(Fn.entries)
    val ans = when (fn) {
        Fn.SIN -> opp / hyp
        Fn.COS -> adj / hyp
        Fn.TG -> opp / adj
    }
    // úroveň 3: jedna ze stran, které do podílu patří, chybí – dopočítá se Pythagorovou větou
    val needed = when (fn) {
        Fn.SIN -> listOf(TriSide.OPP, TriSide.HYP)
        Fn.COS -> listOf(TriSide.ADJ, TriSide.HYP)
        Fn.TG -> listOf(TriSide.OPP, TriSide.ADJ)
    }
    val hidden = if (level == 3) rng.pick(needed) else null
    val labels = buildMap {
        if (hidden != TriSide.ADJ) put(TriSide.ADJ, n(adj))
        if (hidden != TriSide.OPP) put(TriSide.OPP, n(opp))
        if (hidden != TriSide.HYP) put(TriSide.HYP, n(hyp))
    }
    val (num, den) = when (fn) {
        Fn.SIN -> L("protilehlá", "противолежащий") to L("přepona", "гипотенуза")
        Fn.COS -> L("přilehlá", "прилежащий") to L("přepona", "гипотенуза")
        Fn.TG -> L("protilehlá", "противолежащий") to L("přilehlá", "прилежащий")
    }
    val (a, b) = when (fn) {
        Fn.SIN -> opp to hyp
        Fn.COS -> adj to hyp
        Fn.TG -> opp to adj
    }
    exercise(
        "E02", seed, level,
        prompt = L(
            "Kolik je ${fn.label} α? Zapiš desetinným číslem (stačí 2 desetinná místa) nebo zlomkem." + (if (hidden != null) " Jednu stranu musíš nejdřív dopočítat." else ""),
            "Чему равен ${fn.label} α? Запиши десятичным числом (достаточно 2 знаков после запятой) или дробью." + (if (hidden != null) " Одну сторону сначала нужно найти." else ""),
        ),
        fig = Fig.Tri(adj, opp, rot = if (level == 1) 0 else rng.int(0, 3), mirror = rng.chance(0.5), labels = labels, angle = "α"),
        fields = listOf(Field(L("${fn.label} α", "${fn.label} α"), ans, tol = Tol.RATIO, digits = 3)),
        hints = listOf(
            L("${fn.label} α = ${num.cs} / ${den.cs}" + (if (hidden != null) ". Chybějící stranu dopočítej: a² + b² = c²." else "") + " Najdi ty dvě strany na obrázku.",
                "${fn.label} α = ${num.ru} / ${den.ru}" + (if (hidden != null) ". Недостающую сторону найди: a² + b² = c²." else "") + " Найди эти две стороны на рисунке."),
            hiddenHint(hidden, adj, opp, hyp).let { pre -> L(pre.cs + "${fn.label} α = ${n(a)} / ${n(b)}", pre.ru + "${fn.label} α = ${n(a)} / ${n(b)}") },
        ),
        traps = buildList {
            when (fn) {
                Fn.SIN -> add(Traps.SWAP.at(adj / hyp))
                Fn.COS -> add(Traps.SWAP.at(opp / hyp))
                Fn.TG -> add(Traps.SWAP.with(L("Obrácený zlomek: tangens je **protilehlá / přilehlá** – nahoře odvěsna naproti úhlu.", "Перевёрнутая дробь: тангенс — **противолежащий / прилежащий**, сверху катет напротив угла.")).at(adj / opp))
            }
            if (fn != Fn.TG) add(Traps.INV.at(1 / ans))
            if (fn != Fn.TG) add(TrapText("OTHERFN", L("Tady se dělí odvěsna odvěsnou – to je tangens. Sinus i kosinus dělí přeponou.", "Здесь катет делится на катет — это тангенс. И синус, и косинус делят на гипотенузу."), Cards.CHECK345).at(opp / adj))
            if (fn == Fn.TG) add(TrapText("OTHERFN", L("Dělíš přeponou – to je sinus. V tangentu přepona vůbec není.", "Ты делишь на гипотенузу — это синус. В тангенсе гипотенузы нет."), Cards.CHECK345).at(opp / hyp))
        },
        ok = L("Správně: ${fn.label} α = ${n(a)} / ${n(b)} ${approx(ans, 3)}.", "Верно: ${fn.label} α = ${n(a)} / ${n(b)} ${approx(ans, 3)}."),
    )
}

/** 2. nápověda u skryté strany: nejdřív ji dopočítat („Chybějící strana: √(8² + 15²) = √289 = 17. Pak …“) */
private fun hiddenHint(hidden: TriSide?, adj: Double, opp: Double, hyp: Double): L = when (hidden) {
    null -> L("", "")
    TriSide.HYP -> {
        val t = "√(${n(adj)}² + ${n(opp)}²) = √${n(adj * adj + opp * opp)} = ${n(hyp)}"
        L("Chybějící přepona: $t. Pak ", "Недостающая гипотенуза: $t. Тогда ")
    }
    TriSide.ADJ -> {
        val t = "√(${n(hyp)}² − ${n(opp)}²) = √${n(hyp * hyp - opp * opp)} = ${n(adj)}"
        L("Chybějící odvěsna: $t. Pak ", "Недостающий катет: $t. Тогда ")
    }
    TriSide.OPP -> {
        val t = "√(${n(hyp)}² − ${n(adj)}²) = √${n(hyp * hyp - adj * adj)} = ${n(opp)}"
        L("Chybějící odvěsna: $t. Pak ", "Недостающий катет: $t. Тогда ")
    }
}
