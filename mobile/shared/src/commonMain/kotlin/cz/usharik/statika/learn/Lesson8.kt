package cz.usharik.statika.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Lekce 8 – Spojité zatížení. q je síla na každý metr (kN/m); výslednice Q = q · ℓ působí uprostřed.
 * „Nahradit“ znamená stejný účinek na nosník: s výslednicí na správném místě vyjdou reakce stejně
 * jako se spojitým zatížením. Na konci lekce celý nosník krok za krokem.
 */

/** Hřeben šipek spojitého zatížení mezi x1 a x2 (výška = q v měřítku obrázku), se splýváním do středu ([merge] 0…1). */
private fun PrimBuilder.comb(x1: Double, x2: Double, yTop: Double, yBeam: Double, c: StatikaColors, merge: Double = 0.0, alpha: Float = 1f) {
    if (alpha <= 0.01f) return
    val xc = (x1 + x2) / 2
    val n = max(2, ((x2 - x1) / 22).roundToInt())
    rect(x1 + (xc - x1) * merge, yTop, (x2 - x1) * (1 - merge), yBeam - yTop, c.loadFill, alpha = alpha)
    line(x1 + (xc - x1) * merge, yTop, x2 - (x2 - xc) * merge, yTop, c.load, 1.6, alpha = alpha)
    for (i in 0..n) {
        val x = x1 + (x2 - x1) * i / n
        val xm = x + (xc - x) * merge
        force(xm, yTop, xm, yBeam, c.load, 1.3, 7.0, alpha = alpha)
    }
}

/** Kóta mezi x1 a x2 ve výšce y s popiskem nad čarou. */
private fun PrimBuilder.dim(x1: Double, x2: Double, y: Double, label: String, color: androidx.compose.ui.graphics.Color, alpha: Float = 1f) {
    if (alpha <= 0.01f) return
    line(x1, y, x2, y, color, 0.9, alpha = alpha)
    line(x1 - 4, y + 4, x1 + 4, y - 4, color, 1.2, alpha = alpha)
    line(x2 - 4, y + 4, x2 + 4, y - 4, color, 1.2, alpha = alpha)
    text((x1 + x2) / 2, y - 5, label, color, 12.0, alpha = alpha)
}

/** Nápověda ke špatnému odhadu výslednice Q = q · ℓ podle toho, jakou chybu žák nejspíš udělal. */
private fun qHint(v: Double, q: Double, l: Double): L = when {
    abs(v - q) < 1e-9 -> L(
        "${num(q)} kN připadají jen na **jeden** metr. Zatížených metrů je ${num(l)}.",
        "${num(q)} кН приходится только на **один** метр. Нагруженных метров — ${num(l)}.",
    )
    abs(v - l) < 1e-9 -> L(
        "${num(l)} je délka v metrech, ne síla. Každý metr přidá ${num(q)} kN.",
        "${num(l)} — это длина в метрах, а не сила. Каждый метр добавляет ${num(q)} кН.",
    )
    abs(v - (q + l)) < 1e-9 -> L(
        "kN/m a metry se sčítat nedají – jsou to různé veličiny. Násob: kN/m · m = kN.",
        "кН/м и метры складывать нельзя — это разные величины. Умножай: кН/м · м = кН.",
    )
    abs(v - q * l / 2) < 1e-9 -> L(
        "To je jen polovina zatížení. Ve středu výslednice **působí**, ale nese celé zatížení.",
        "Это только половина нагрузки. Посередине равнодействующая **приложена**, но несёт всю нагрузку.",
    )
    else -> L("Sečti to po metrech: každý metr přidá ${num(q)} kN.", "Сложи по метрам: каждый метр добавляет ${num(q)} кН.")
}

/** Police: zatížená délka a nabídka odhadů Q. */
private class Shelf(val len: Double, val options: List<Double>)

/** Karta 0: q je „na metr“ – knihy na polici. Žák nejdřív odhadne Q, teprve pak se ukáže rozpočet po metrech. */
@Composable
private fun CardScope.PerMetre() {
    val t = tr()
    val q = 2.0
    val shelves = remember { listOf(Shelf(3.0, listOf(2.0, 3.0, 5.0, 6.0)), Shelf(1.5, listOf(1.5, 2.0, 3.0, 3.5))) }
    var round by remember { mutableIntStateOf(0) }
    val picked = remember { mutableStateListOf<Int?>(null, null) }
    val ok = remember { mutableStateListOf(false, false) }
    // knihy se na polici skládají postupně, rozpočet po metrech se objeví po správném odhadu
    val books = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(round) {
        reveal.snapTo(0f)
        books.snapTo(0f)
        books.animateTo(1f, tween(900))
    }
    LaunchedEffect(ok[round]) { if (ok[round]) reveal.animateTo(1f, tween(500)) }
    val s = shelves[round]
    val y = 130.0
    val x1 = 40.0
    val m = 80.0
    Stage(h = 220.0, description = t.pick(L("Knihy na polici, 2 kN na každý metr", "Книги на полке, 2 кН на каждый метр"))) { c ->
        beam(x1, x1 + 4 * m, y, c)
        // knihy na zatížené délce
        val end = x1 + s.len * m
        val total = ((end - x1 - 4) / 13).toInt()
        for (i in 0 until total) {
            val a = (books.value * total - i).coerceIn(0f, 1f)
            if (a <= 0f) continue
            val h = 28.0 + (i * 7 % 10)
            rect(x1 + 3 + i * 13.0, y - 5 - h, 10.0, h, c.load.copy(alpha = 0.3f), c.load, 1.0, alpha = a)
        }
        text(x1 + s.len * m / 2, y - 82, " = 2 kN/m", c.load, sym = "q")
        // metry pod policí
        for (i in 0..4) line(x1 + i * m, y + 8, x1 + i * m, y + 16, c.ink, 0.9)
        for (i in 0 until 4) text(x1 + i * m + m / 2, y + 30, "1 m", c.inkSoft, 12.0)
        dim(x1, end, y + 54, "ℓ = ${num(s.len)} m", c.ink)
        // po správném odhadu: kolik kN připadá na každý (i neúplný) metr
        if (reveal.value > 0f) {
            var a = 0.0
            while (a < s.len - 1e-9) {
                val b = minOf(a + 1.0, s.len)
                val xa = x1 + a * m
                val xb = x1 + b * m
                line(xa + 3, y - 50, xb - 3, y - 50, c.derived, 1.4, alpha = reveal.value)
                text((xa + xb) / 2, y - 56, "${num(q * (b - a))} kN", c.derived, 13.0, alpha = reveal.value)
                a = b
            }
        }
    }
    CardText(L(
        "q = 2 kN/m znamená: **na každý metr** police připadají 2 kN knih. Kolik kN váží knihy na celé zatížené délce? Nejdřív odhadni, pak se podívej na rozpočet po metrech.",
        "q = 2 кН/м означает: **на каждый метр** полки приходится 2 кН книг. Сколько кН весят книги на всём нагруженном участке? Сначала оцени, потом посмотри раскладку по метрам.",
    ))
    Todo(
        L("Police s knihami na **3 m**: kolik je to kN?", "Полка с книгами на **3 м**: сколько это кН?") to ok[0],
        L("Police s knihami jen na **1,5 m**: kolik je to kN?", "Полка с книгами только на **1,5 м**: сколько это кН?") to ok[1],
    )
    Chips(s.options.map { L("${num(it)} kN", "${num(it)} кН") }, picked[round]) {
        if (ok[round]) return@Chips
        picked[round] = it
        if (abs(s.options[it] - q * s.len) < 1e-9) ok[round] = true
    }
    val p = picked[round]
    if (p != null && !ok[round]) Note(Tone.NUDGE, qHint(s.options[p], q, s.len))
    if (ok[round]) {
        Readout("Q = q · ℓ = 2 kN/m · ${num(s.len)} m = ${num(q * s.len)} kN", ok = true)
        if (round == 0) ActionButton(L("Další police →", "Следующая полка →")) { round = 1 }
        else Note(Tone.INFO, L(
            "q ještě není síla – je to síla **na metr** (kN/m). Sílu Q v kN dostaneš, až když q vynásobíš zatíženou délkou: kN/m · m = kN. Neúplný metr přidá i neúplnou část.",
            "q — это ещё не сила, а сила **на метр** (кН/м). Силу Q в кН получишь, только умножив q на нагруженную длину: кН/м · м = кН. Неполный метр добавляет и неполную часть.",
        ))
    }
}

/** Zatížení na nosníku o rozpětí 6 m: q (kN/m) od [a] (m od A) na délce [l]; [options] = nabídka odhadů Q. */
private class LoadCase(val q: Double, val a: Double, val l: Double, val options: List<Double>) {
    val big get() = q * l
    val xc get() = a + l / 2
}

private const val SPAN = 6.0

/**
 * Karta 1: nahradit = stejný účinek. Pod podporami jsou reakce od spojitého zatížení. Žák odhadne velikost Q,
 * přetáhne ji na místo, kde má působit, a nahradí jí zatížení. Reakce se nezmění, jen když Q leží uprostřed.
 */
@Composable
private fun CardScope.Resultant() {
    val t = tr()
    val cases = remember {
        listOf(
            LoadCase(2.0, 0.0, 3.0, listOf(2.0, 3.0, 5.0, 6.0)),
            LoadCase(3.0, 2.5, 3.0, listOf(3.0, 4.5, 6.0, 9.0)),
        )
    }
    var round by remember { mutableIntStateOf(0) }
    var phase by remember { mutableIntStateOf(0) } // 0 = odhad velikosti, 1 = poloha, 2 = nahrazeno
    var picked by remember { mutableStateOf<Int?>(null) }
    var xq by remember { mutableStateOf(0.0) }
    var grab by remember { mutableStateOf(false) }
    var tries by remember { mutableIntStateOf(0) }
    val done = remember { mutableStateListOf(false, false) }
    val merge = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val lc = cases[round]
    val rbDist = lc.big * lc.xc / SPAN
    val raDist = lc.big - rbDist
    val rbQ = lc.big * xq / SPAN
    val raQ = lc.big - rbQ
    val same = abs(xq - lc.xc) < 1e-9
    val y = 108.0
    val xA = 50.0
    val m = 50.0
    fun gx(v: Double) = xA + v * m
    Stage(
        h = 232.0,
        description = t.pick(L("Spojité zatížení na nosníku a jeho výslednice", "Распределённая нагрузка на балке и её равнодействующая")),
        onDrag = { pt, start ->
            if (phase != 1) return@Stage
            if (start) grab = pt.dist(gx(xq), y - 5 - lc.big * 8) < 45 || abs(pt.x - gx(xq)) < 30
            if (grab) xq = (((pt.x - xA) / m) * 2).roundToInt().coerceIn(0, 12) / 2.0
        },
        onDragEnd = { grab = false },
    ) { c ->
        val mv = merge.value
        support(SupportType.PIN, gx(0.0), y, c, "A")
        support(SupportType.ROLLER, gx(SPAN), y, c, "B")
        beam(gx(0.0) - 10, gx(SPAN) + 10, y, c)
        // spojité zatížení (výška ∝ q) – po nahrazení úplně zmizí
        comb(gx(lc.a), gx(lc.a + lc.l), y - 5 - lc.q * 12, y - 5, c, mv * 0.6, alpha = 1f - mv)
        if (mv < 0.5f) text(gx(lc.xc), y - 5 - lc.q * 12 - 8, " = ${num(lc.q)} kN/m", c.load, 14.0, sym = "q", alpha = 1f - mv * 2)
        // výslednice (délka ∝ Q): při hledání polohy čárkovaná s ručkou
        if (phase >= 1) {
            val len = lc.big * 8
            val xx = gx(xq)
            val a = if (phase == 1) 0.9f else mv.coerceAtLeast(0.3f)
            force(xx, y - 5 - len, xx, y - 5, c.derived, 2.6, alpha = a, dash = phase == 1)
            text(xx, y - 5 - len - 16, " = ${num(lc.big)} kN", c.derived, 14.0, sym = "Q", alpha = a)
            if (phase == 1) handle(xx, y - 5 - len, c, grab)
            dim(gx(0.0), xx, y + 106, "x = ${num(xq)} m", c.derived, if (xq > 0) a else 0f)
        }
        // reakce podpor: před nahrazením od spojitého zatížení, po nahrazení od Q
        val after = mv > 0.5f
        val ra = if (after) raQ else raDist
        val rb = if (after) rbQ else rbDist
        val rc = if (!after) c.react else if (same) c.accent else c.load
        force(gx(0.0), y + 62, gx(0.0), y + 6, c.react)
        force(gx(SPAN), y + 62, gx(SPAN), y + 6, c.react)
        val mark = if (!after) "" else if (same) " ✓" else " ✗"
        text(gx(0.0) + 10, y + 58, " = ${num(ra)} kN$mark", rc, 14.0, Anchor.START, sym = "R_A")
        text(gx(SPAN) - 10, y + 58, " = ${num(rb)} kN$mark", rc, 14.0, Anchor.END, sym = "R_B")
        // kóty zatížení
        if (lc.a > 0) dim(gx(0.0), gx(lc.a), y + 84, "${num(lc.a)} m", c.ink)
        dim(gx(lc.a), gx(lc.a + lc.l), y + 84, "ℓ = ${num(lc.l)} m", c.ink)
    }
    CardText(L(
        "Spojité zatížení (sníh, knihy) chceme nahradit **jedinou silou Q** – výslednicí. „Nahradit“ znamená, že to nosník nepozná: podpory pod ním musí tlačit **stejně**. Pod podporami vidíš reakce, které drží spojité zatížení.",
        "Распределённую нагрузку (снег, книги) хотим заменить **одной силой Q** — равнодействующей. «Заменить» значит, что балка разницы не заметит: опоры должны давить на неё **так же**. Под опорами — реакции, которые держат распределённую нагрузку.",
    ))
    Todo(
        L("První zatížení: najdi velikost i polohu Q tak, aby reakce zůstaly stejné", "Первая нагрузка: найди величину и положение Q так, чтобы реакции не изменились") to done[0],
        L("Druhé zatížení (začíná 2,5 m od A): totéž", "Вторая нагрузка (начинается в 2,5 м от A): то же самое") to done[1],
    )
    when (phase) {
        0 -> {
            CardText(L("Jak velká je výslednice Q?", "Какова величина равнодействующей Q?"))
            Chips(lc.options.map { L("${num(it)} kN", "${num(it)} кН") }, picked) {
                picked = it
                if (abs(lc.options[it] - lc.big) < 1e-9) {
                    phase = 1
                    xq = lc.a // začne na začátku zatížení – tam to není
                    picked = null
                }
            }
            picked?.let { Note(Tone.NUDGE, qHint(lc.options[it], lc.q, lc.l)) }
        }
        1 -> {
            Readout("Q = ${num(lc.q)} · ${num(lc.l)} = ${num(lc.big)} kN")
            CardText(L(
                "Teď **kam**? Přetáhni fialovou šipku Q za kroužek na místo, kde má působit, a nahraď jí zatížení.",
                "Теперь **куда**? Перетащи фиолетовую стрелку Q за кружок туда, где она должна действовать, и замени ею нагрузку.",
            ))
            Chips(listOf(L("◀ 0,5 m", "◀ 0,5 м"), L("0,5 m ▶", "0,5 м ▶"))) { i -> xq = (xq + if (i == 0) -0.5 else 0.5).coerceIn(0.0, SPAN) }
            ActionButton(L("Nahradit zatížení silou Q", "Заменить нагрузку силой Q")) {
                phase = 2
                tries++
                val ok = abs(xq - lc.xc) < 1e-9
                scope.launch {
                    merge.animateTo(1f, tween(900))
                    if (ok) done[round] = true
                }
            }
        }
        else -> {
            if (merge.value > 0.5f && same) {
                Note(Tone.OK, L(
                    "Reakce se **nezměnily** – Q = ${num(lc.big)} kN uprostřed zatížené délky (${num(lc.a)} + ${num(lc.l)}/2 = ${num(lc.xc)} m od A) má na nosník stejný účinek jako celé spojité zatížení.",
                    "Реакции **не изменились** — Q = ${num(lc.big)} кН посередине нагруженного участка (${num(lc.a)} + ${num(lc.l)}/2 = ${num(lc.xc)} м от A) действует на балку так же, как вся распределённая нагрузка.",
                ))
                if (round == 0) ActionButton(L("Další zatížení →", "Следующая нагрузка →")) {
                    round = 1
                    phase = 0
                    picked = null
                    scope.launch { merge.snapTo(0f) }
                }
                else SmallText(L(
                    "Výslednice nahrazuje zatížení jen pro rovnováhu celého nosníku (reakce). Jak se nosník uvnitř prohýbá, to už je jiná otázka.",
                    "Равнодействующая заменяет нагрузку только для равновесия всей балки (реакций). Как балка изгибается внутри — это уже другой вопрос.",
                ))
            } else if (merge.value > 0.5f) {
                Note(Tone.NUDGE, L(
                    "Reakce vyšly **jinak** – síla Q v ${num(xq)} m má jiný účinek než zatížení. Rovnoměrné zatížení je kolem svého středu rozložené souměrně. Kde je střed zatížené délky?",
                    "Реакции получились **другими** — сила Q в ${num(xq)} м действует иначе, чем нагрузка. Равномерная нагрузка распределена симметрично вокруг своей середины. Где середина нагруженного участка?",
                ))
                ActionButton(L("Zkusit jinou polohu", "Попробовать другое положение")) {
                    phase = 1
                    scope.launch { merge.animateTo(0f, tween(500)) }
                }
            }
        }
    }
    SmallText(L(
        "Výška hřebenu odpovídá q, délka šipky Q odpovídá velikosti Q. Šipky reakcí nejsou v měřítku – rozhodují čísla.",
        "Высота гребёнки соответствует q, длина стрелки Q — величине Q. Стрелки реакций не в масштабе — важны числа.",
    ))
}

/** Karta 2: kvíz – výslednice a její rameno, když zatížení nezačíná v podpoře. */
@Composable
private fun CardScope.CheckQ() {
    val solved = solvedHere
    val y = 100.0
    val xA = 70.0
    val m = 52.0 // rozpětí 5 m
    Stage(h = 200.0, description = L("Spojité zatížení 2 kN/m na délce 3 m, začíná 1 m od podpory A", "Распределённая нагрузка 2 кН/м на длине 3 м, начинается в 1 м от опоры A").t()) { c ->
        support(SupportType.PIN, xA, y, c, "A")
        support(SupportType.ROLLER, xA + 5 * m, y, c, "B")
        beam(xA - 15, xA + 5 * m + 15, y, c)
        if (!solved) {
            comb(xA + m, xA + 4 * m, y - 5 - 30, y - 5, c)
            text(xA + 2.5 * m, y - 44, " = 2 kN/m", c.load, sym = "q")
        } else {
            // po vyřešení: zatížení nahradila výslednice s ramenem k A
            force(xA + 2.5 * m, y - 5 - 48, xA + 2.5 * m, y - 5, c.derived, 2.6)
            text(xA + 2.5 * m + 8, y - 40, " = 6 kN", c.derived, 14.0, Anchor.START, sym = "Q")
            dim(xA, xA + 2.5 * m, y + 80, "2,5 m", c.derived)
        }
        dim(xA, xA + m, y + 54, "1 m", c.ink)
        dim(xA + m, xA + 4 * m, y + 54, "3 m", c.ink)
    }
    Quiz(
        question = L("Čím tohle spojité zatížení nahradíš?", "Чем заменишь эту распределённую нагрузку?"),
        options = listOf(
            Opt(L("Q = 6 kN uprostřed zatížení", "Q = 6 кН посередине нагрузки"), correct = true),
            Opt(L("Q = 2 kN uprostřed zatížení", "Q = 2 кН посередине нагрузки"), why = L("2 kN připadají jen na jeden metr. Kolik metrů je zatíženo?", "2 кН приходится только на один метр. Сколько метров нагружено?")),
            Opt(L("Q = 6 kN na konci zatížení", "Q = 6 кН на конце нагрузки"), why = L("Zatížení je všude stejné, takže vlevo od Q ho musí být stejně jako vpravo. Kde bys rovnoměrně naloženou polici podepřel jedním prstem?", "Нагрузка везде одинакова, значит, слева от Q её должно быть столько же, сколько справа. Где бы ты подпёр равномерно загруженную полку одним пальцем?")),
        ),
        explain = L(
            "Q = q · ℓ = 2 · 3 = 6 kN. Působí uprostřed zatížené délky, tedy 1,5 m od jejího začátku.",
            "Q = q · ℓ = 2 · 3 = 6 кН. Приложена посередине нагруженного участка, то есть в 1,5 м от его начала.",
        ),
    )
    Quiz(
        question = L("Jaké **rameno** má Q k bodu A?", "Какое **плечо** у Q относительно точки A?"),
        options = listOf(
            Opt(L("1,5 m", "1,5 м"), why = L("1,5 m je od **začátku zatížení**. Zatížení ale nezačíná v A – začíná až o 1 m dál.", "1,5 м — это от **начала нагрузки**. Но нагрузка начинается не в A, а на 1 м дальше.")),
            Opt(L("2,5 m", "2,5 м"), correct = true),
            Opt(L("3 m", "3 м"), why = L("3 m je délka zatížení ℓ, ne vzdálenost výslednice od A.", "3 м — это длина нагрузки ℓ, а не расстояние от равнодействующей до A.")),
            Opt(L("4 m", "4 м"), why = L("4 m od A zatížení končí. Výslednice působí uprostřed zatížené délky.", "В 4 м от A нагрузка заканчивается. Равнодействующая приложена посередине нагруженного участка.")),
        ),
        explain = L(
            "Rameno = 1 m (od A k začátku zatížení) + ℓ/2 = 1 + 1,5 = **2,5 m**. Moment Q k bodu A: 6 · 2,5 = 15 kNm. Častá chyba je zapomenout na ten první metr.",
            "Плечо = 1 м (от A до начала нагрузки) + ℓ/2 = 1 + 1,5 = **2,5 м**. Момент Q относительно A: 6 · 2,5 = 15 кН·м. Частая ошибка — забыть про этот первый метр.",
        ),
        columns = 2,
    )
}

/** Krok řešení na kartě „Celý nosník“: otázka, volby, správná volba, nápovědy ke špatným volbám a zápis po vyřešení. */
private class SolveStep(val todo: L, val question: L, val options: List<L>, val correct: Int, val hints: Map<Int, L>, val result: L)

/**
 * Karta 3: celý nosník od začátku do konce – uvolnění, výslednice, složky šikmé síly, momentové rovnice
 * a kontrola. Prostý nosník 6 m: q = 2 kN/m na 0–3 m, F = 5 kN (sklon 3 : 4) ve 4,5 m.
 * R_B = 4,5 kN, R_Ay = 5,5 kN, R_Ax = −3 kN.
 */
@Composable
private fun CardScope.WholeBeam() {
    val t = tr()
    val steps = remember {
        listOf(
            SolveStep(
                L("Uvolni nosník: čím nahradíš podpory?", "Освободи балку: чем заменишь опоры?"),
                L("1. Uvolnění: co nahradí podpory A a B?", "1. Освобождение: чем заменить опоры A и B?"),
                listOf(
                    L("A: [[R_Ay]] · B: [[R_B]]", "A: [[R_Ay]] · B: [[R_B]]"),
                    L("A: [[R_Ax]], [[R_Ay]] · B: [[R_B]]", "A: [[R_Ax]], [[R_Ay]] · B: [[R_B]]"),
                    L("A: [[R_Ax]], [[R_Ay]] · B: [[R_Bx]], [[R_By]]", "A: [[R_Ax]], [[R_Ay]] · B: [[R_Bx]], [[R_By]]"),
                ),
                1,
                mapOf(
                    0 to L("Pevná kloubová podpora A nedovolí ani vodorovný posun – a šikmá síla F nosník do strany táhne.", "Шарнирно-неподвижная опора A не даёт и горизонтального сдвига — а наклонная сила F тянет балку вбок."),
                    2 to L("Posuvná podpora B dovolí vodorovný posun – vodorovnou reakci nemá.", "Шарнирно-подвижная опора B допускает горизонтальный сдвиг — горизонтальной реакции у неё нет."),
                ),
                L("Neznámé: [[R_Ax]], [[R_Ay]], [[R_B]] – n = 3 = r.", "Неизвестные: [[R_Ax]], [[R_Ay]], [[R_B]] — n = 3 = r."),
            ),
            SolveStep(
                L("Nahraď spojité zatížení výslednicí", "Замени распределённую нагрузку равнодействующей"),
                L("2. Výslednice spojitého zatížení q = 2 kN/m na 3 m?", "2. Равнодействующая нагрузки q = 2 кН/м на 3 м?"),
                listOf(
                    L("Q = 6 kN, 1,5 m od A", "Q = 6 кН, 1,5 м от A"),
                    L("Q = 6 kN, 3 m od A", "Q = 6 кН, 3 м от A"),
                    L("Q = 2 kN, 1,5 m od A", "Q = 2 кН, 1,5 м от A"),
                ),
                0,
                mapOf(
                    1 to L("3 m od A zatížení končí. Výslednice působí uprostřed zatížené délky.", "В 3 м от A нагрузка заканчивается. Равнодействующая приложена посередине нагруженного участка."),
                    2 to L("2 kN je jen na jeden metr: Q = q · ℓ.", "2 кН — только на один метр: Q = q · ℓ."),
                ),
                L("Q = 2 · 3 = 6 kN, 1,5 m od A.", "Q = 2 · 3 = 6 кН, 1,5 м от A."),
            ),
            SolveStep(
                L("Rozlož šikmou sílu F na složky", "Разложи наклонную силу F на составляющие"),
                L("3. F = 5 kN se sklonem 3 : 4 (3 vodorovně, 4 svisle). Složky?", "3. F = 5 кН с наклоном 3 : 4 (3 по горизонтали, 4 по вертикали). Составляющие?"),
                listOf(
                    L("[[F_x]] = 4 kN, [[F_y]] = 3 kN", "[[F_x]] = 4 кН, [[F_y]] = 3 кН"),
                    L("[[F_x]] = 3 kN →, [[F_y]] = 4 kN ↓", "[[F_x]] = 3 кН →, [[F_y]] = 4 кН ↓"),
                    L("[[F_x]] = 5 kN, [[F_y]] = 5 kN", "[[F_x]] = 5 кН, [[F_y]] = 5 кН"),
                ),
                1,
                mapOf(
                    0 to L("Prohozené. Vodorovně je to 3 dílky, svisle 4 – jako trojúhelník 3-4-5 z lekce 3.", "Перепутано. По горизонтали 3 деления, по вертикали 4 — как треугольник 3-4-5 из урока 3."),
                    2 to L("Složky nejsou kopie síly: 5 je přepona, složky jsou odvěsny 3 a 4.", "Составляющие — не копии силы: 5 — гипотенуза, составляющие — катеты 3 и 4."),
                ),
                L("[[F_x]] = 5 · 3/5 = 3 kN →, [[F_y]] = 5 · 4/5 = 4 kN ↓.", "[[F_x]] = 5 · 3/5 = 3 кН →, [[F_y]] = 5 · 4/5 = 4 кН ↓."),
            ),
            SolveStep(
                L("Spočítej [[R_B]] z momentové rovnice", "Найди [[R_B]] из уравнения моментов"),
                L("4. Chceš rovnou [[R_B]]. Ke kterému bodu sečteš momenty?", "4. Хочешь сразу [[R_B]]. Относительно какой точки сложишь моменты?"),
                listOf(
                    L("k bodu A", "относительно A"),
                    L("k bodu B", "относительно B"),
                    L("ke středu nosníku", "относительно середины балки"),
                ),
                0,
                mapOf(
                    1 to L("K bodu B má [[R_B]] rameno 0 – z rovnice by zmizela právě ta reakce, kterou hledáš.", "Относительно B у [[R_B]] плечо 0 — из уравнения исчезнет как раз та реакция, которую ищешь."),
                    2 to L("Ke středu by v rovnici zůstaly [[R_Ay]] i [[R_B]] – dvě neznámé.", "Относительно середины в уравнении останутся и [[R_Ay]], и [[R_B]] — две неизвестные."),
                ),
                L("Σ[[M_A]]: [[R_B]]·6 − 6·1,5 − 4·4,5 = 0 → [[R_B]] = 27/6 = **4,5 kN**", "Σ[[M_A]]: [[R_B]]·6 − 6·1,5 − 4·4,5 = 0 → [[R_B]] = 27/6 = **4,5 кН**"),
            ),
            SolveStep(
                L("Spočítej [[R_Ay]] z momentů k bodu B", "Найди [[R_Ay]] из моментов относительно B"),
                L("5. Σ[[M_B]]: −[[R_Ay]]·6 + 6·4,5 + 4·1,5 = 0. Kolik je [[R_Ay]]?", "5. Σ[[M_B]]: −[[R_Ay]]·6 + 6·4,5 + 4·1,5 = 0. Чему равна [[R_Ay]]?"),
                listOf(
                    L("4,5 kN", "4,5 кН"),
                    L("5,5 kN", "5,5 кН"),
                    L("10 kN", "10 кН"),
                ),
                1,
                mapOf(
                    0 to L("To je [[R_B]]. Dosaď: 6·4,5 + 4·1,5 = 27 + 6 = 33, a to vyděl ramenem 6 m.", "Это [[R_B]]. Подставь: 6·4,5 + 4·1,5 = 27 + 6 = 33, и раздели на плечо 6 м."),
                    2 to L("10 kN je součet zatížení – nesou ho obě podpory dohromady.", "10 кН — сумма нагрузок, её несут обе опоры вместе."),
                ),
                L("[[R_Ay]] = 33/6 = **5,5 kN**. Ze Σ[[F_x]]: [[R_Ax]] + 3 = 0 → [[R_Ax]] = **−3 kN** (ve skutečnosti ←).", "[[R_Ay]] = 33/6 = **5,5 кН**. Из Σ[[F_x]]: [[R_Ax]] + 3 = 0 → [[R_Ax]] = **−3 кН** (на самом деле ←)."),
            ),
            SolveStep(
                L("Zkontroluj výsledek: Σ[[F_y]] = 0", "Проверь результат: Σ[[F_y]] = 0"),
                L("6. Kontrola: Σ[[F_y]] = 5,5 + 4,5 − 6 − 4 = ?", "6. Проверка: Σ[[F_y]] = 5,5 + 4,5 − 6 − 4 = ?"),
                listOf(L("0", "0"), L("20", "20"), L("−1", "−1")),
                0,
                mapOf(
                    1 to L("Zatížení míří dolů, takže se odečítají: 10 − 10.", "Нагрузки направлены вниз, поэтому вычитаются: 10 − 10."),
                    2 to L("Sečti znovu: 5,5 + 4,5 = 10 a 6 + 4 = 10.", "Сложи ещё раз: 5,5 + 4,5 = 10 и 6 + 4 = 10."),
                ),
                L("Σ[[F_y]] = 0 ✓ – svislá rovnováha sedí, reakce jsou spočítané správně.", "Σ[[F_y]] = 0 ✓ — вертикальное равновесие сходится, реакции найдены верно."),
            ),
        )
    }
    var step by remember { mutableIntStateOf(0) } // počet hotových kroků
    var picked by remember { mutableStateOf<Int?>(null) }
    val prog = remember { Animatable(0f) }
    LaunchedEffect(step) { prog.animateTo(step.toFloat(), tween(700)) }
    fun ph(k: Int) = (prog.value - (k - 1)).coerceIn(0f, 1f) // průběh přechodu po kroku k (1…6)
    val y = 118.0
    val xA = 50.0
    val m = 50.0
    fun gx(v: Double) = xA + v * m
    Stage(h = 240.0, description = t.pick(L("Prostý nosník se spojitým zatížením a šikmou silou", "Простая балка с распределённой нагрузкой и наклонной силой"))) { c ->
        val p1 = ph(1)
        val p2 = ph(2)
        val p3 = ph(3)
        val p4 = ph(4)
        val p5 = ph(5)
        // podpory → reakce (kladné smysly → ↑ ↑)
        if (p1 < 1f) {
            support(SupportType.PIN, gx(0.0), y, c, "A", ghost = p1 > 0f)
            support(SupportType.ROLLER, gx(6.0), y, c, "B", ghost = p1 > 0f)
        }
        beam(gx(0.0) - 10, gx(6.0) + 10, y, c)
        if (p1 > 0f) {
            force(gx(0.0) - 44, y, gx(0.0) - 12, y, c.react, alpha = p1)
            force(gx(0.0), y + 48, gx(0.0), y + 6, c.react, alpha = p1)
            force(gx(6.0), y + 48, gx(6.0), y + 6, c.react, alpha = p1)
            val va = if (p5 > 0.5f) " = −3" else ""
            text(gx(0.0) - 28, y - 10, va, c.react, 13.0, sym = "R_Ax", alpha = p1)
            text(gx(0.0) + 8, y + 44, if (p5 > 0.5f) " = 5,5 kN" else null, c.react, 13.0, Anchor.START, sym = "R_Ay", alpha = p1)
            text(gx(6.0) - 8, y + 44, if (p4 > 0.5f) " = 4,5 kN" else null, c.react, 13.0, Anchor.END, sym = "R_B", alpha = p1)
            if (p5 > 0.5f) {
                // skutečný smysl R_Ax čárkovaně
                force(gx(0.0) - 12, y + 16, gx(0.0) - 44, y + 16, c.react, 1.4, 8.0, alpha = p5 * 0.7f, dash = true)
            }
        }
        // spojité zatížení → výslednice Q
        comb(gx(0.0), gx(3.0), y - 5 - 24, y - 5, c, p2 * 0.6, alpha = 1f - p2)
        if (p2 < 0.5f) text(gx(1.5), y - 38, " = 2 kN/m", c.load, 14.0, sym = "q", alpha = 1f - p2 * 2)
        if (p2 > 0f) {
            force(gx(1.5), y - 5 - 48, gx(1.5), y - 5, c.derived, 2.6, alpha = p2)
            text(gx(1.5) + 8, y - 44, " = 6 kN", c.derived, 14.0, Anchor.START, sym = "Q", alpha = p2)
        }
        // šikmá síla F (3 : 4) → složky ve stejném působišti
        val px = gx(4.5)
        val py = y - 5
        if (p3 < 1f) {
            force(px - 36, py - 48, px, py, c.load, 2.6, alpha = 1f - p3)
            text(px - 40, py - 52, " = 5 kN", c.load, 14.0, Anchor.END, sym = "F", alpha = 1f - p3)
            // sklon 3 : 4 tence vedle šipky
            line(px - 36, py - 48, px, py - 48, c.inkSoft, 0.8, dash = true, alpha = 1f - p3)
            line(px, py - 48, px, py, c.inkSoft, 0.8, dash = true, alpha = 1f - p3)
            text(px - 18, py - 53, "3", c.inkSoft, 11.0, alpha = 1f - p3)
            text(px + 7, py - 20, "4", c.inkSoft, 11.0, Anchor.START, alpha = 1f - p3)
        }
        if (p3 > 0f) {
            force(px, py - 48, px, py, c.load, 2.4, alpha = p3)
            text(px + 8, py - 34, " = 4 kN", c.load, 13.0, Anchor.START, sym = "F_y", alpha = p3)
            force(px - 36, py - 2, px, py - 2, c.load, 2.4, alpha = p3)
            text(px - 40, py - 12, " = 3 kN", c.load, 13.0, Anchor.END, sym = "F_x", alpha = p3)
        }
        // momentový bod A (krok 4), B (krok 5)
        if (p4 > 0f && p5 < 1f) circle(gx(0.0), y, 12.0, c.accent.copy(alpha = 0.15f), c.accent, 1.6, alpha = p4 * (1 - p5))
        if (p5 > 0f && step < 6) circle(gx(6.0), y, 12.0, c.accent.copy(alpha = 0.15f), c.accent, 1.6, alpha = p5)
        // kóty
        val yd = y + 76
        val marks = listOf(0.0, 1.5, 3.0, 4.5, 6.0)
        line(gx(0.0), yd, gx(6.0), yd, c.ink, 0.9)
        for (mk in marks) line(gx(mk) - 4, yd + 4, gx(mk) + 4, yd - 4, c.ink, 1.2)
        for (i in 0 until marks.size - 1) text((gx(marks[i]) + gx(marks[i + 1])) / 2, yd - 5, num(marks[i + 1] - marks[i]), c.ink, 12.0)
        text(gx(6.0) + 8, yd + 4, "m", c.inkSoft, 11.0, Anchor.START)
        if (step >= 6) text(200.0, yd + 30, t.pick(L("Σ F_y = 0 ✓   Σ M = 0 ✓", "Σ F_y = 0 ✓   Σ M = 0 ✓")), c.accent, 15.0, bold = true)
    }
    CardText(L(
        "Teď všechno dohromady na jednom nosníku: spojité zatížení **i** šikmá síla. Projdi řešení krok za krokem tak, jak ho budeš dělat v trenažéru.",
        "Теперь всё вместе на одной балке: распределённая нагрузка **и** наклонная сила. Пройди решение шаг за шагом — так же, как будешь решать в тренажёре.",
    ))
    Todo(*steps.mapIndexed { i, s -> s.todo to (step > i) }.toTypedArray())
    // zápis dosavadního řešení
    for (i in 0 until step) Readout(steps[i].result.t(), ok = i == 5)
    if (step < steps.size) {
        val s = steps[step]
        CardText(s.question)
        Chips(s.options, picked) {
            if (it == s.correct) {
                picked = null
                step++
            } else picked = it
        }
        picked?.let { p -> s.hints[p]?.let { Note(Tone.NUDGE, it) } }
    } else {
        Note(Tone.OK, L(
            "Hotovo: [[R_Ax]] = −3 kN (←), [[R_Ay]] = 5,5 kN, [[R_B]] = 4,5 kN. Postup: uvolnit → nahradit q výslednicí → rozložit šikmé síly → momenty k bodu, kde zmizí co nejvíc neznámých → kontrola.",
            "Готово: [[R_Ax]] = −3 кН (←), [[R_Ay]] = 5,5 кН, [[R_B]] = 4,5 кН. Порядок: освободить → заменить q равнодействующей → разложить наклонные силы → моменты относительно точки, где исчезает больше всего неизвестных → проверка.",
        ))
    }
}

internal fun lesson8() = Lesson(
    id = 8,
    title = L("Spojité zatížení", "Распределённая нагрузка"),
    summary = L(
        "Q = q · ℓ uprostřed zatížené délky – a celý nosník od uvolnění po kontrolu.",
        "Q = q · ℓ посередине нагруженного участка — и вся балка от освобождения до проверки.",
    ),
    cards = listOf(
        Card { PerMetre() },
        Card { Resultant() },
        Card { CheckQ() },
        Card { WholeBeam() },
    ),
)
