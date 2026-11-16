package cz.usharik.statika.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.model.parseNum
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.LocalColors
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import cz.usharik.statika.ui.rich
import kotlinx.coroutines.delay
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

// ------------------------------------------------------------------ kroky se stavem venku (obrázek čte stav)

/** Kvíz jako krok; stav drží karta. [line] = co se po vyřešení zapíše do výkladu místo vysvětlení. */
private fun QuizState.step8(columns: Int = 1, label: L = question, line: (@Composable () -> Unit)? = null): Step =
    Step(label, solved, result = line ?: { Feedback() }) {
        if (label != question) Text(rich(question.t()), style = MaterialTheme.typography.bodyMedium, color = LocalColors.current.ink, modifier = Modifier.padding(top = 4.dp))
        Options(columns)
        if (!solved) Feedback()
    }

/** Číselná odpověď jako krok; [show] = rovnice v doku, [line] = zápis do výkladu místo vysvětlení, [more] = navíc. */
private fun NumState.step8(label: L, show: (@Composable () -> Unit)? = null, line: (@Composable () -> Unit)? = null, more: (@Composable () -> Unit)? = null): Step =
    Step(label, solved, result = { (line ?: { Feedback() })(); more?.invoke() }) {
        show?.invoke()
        Fields()
        Buttons()
        if (!solved) Feedback()
    }

/** Řádek zápisu řešení (karta „Celý nosník“). */
@Composable
private fun SolLine(text: L, ok: Boolean = false) {
    val c = LocalColors.current
    Text(
        rich(text.t()), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
        color = if (ok) c.accent else c.ink, modifier = Modifier.padding(start = 30.dp),
    )
}

/** Volby odhadu výslednice: správná je q · ℓ, ke špatným cílená nápověda. */
private fun qOpts(options: List<Double>, q: Double, l: Double): List<Opt> = options.map { v ->
    if (abs(v - q * l) < 1e-9) Opt(L("${num(v)} kN", "${num(v)} кН"), correct = true)
    else Opt(L("${num(v)} kN", "${num(v)} кН"), why = qHint(v, q, l))
}

/** Pasti pro číselný odhad výslednice (stejné nápovědy jako u voleb). */
private fun qTraps(wrong: List<Double>, q: Double, l: Double): List<NumTrap> = wrong.map { NumTrap.near(qHint(it, q, l), it) }

/** Karta 0: q je „na metr“ – knihy na polici. Žák nejdřív odhadne Q, teprve pak se ukáže rozpočet po metrech. */
@Composable
private fun CardScope.PerMetre() {
    val t = tr()
    val q = 2.0
    val lens = listOf(3.0, 1.5)
    val kn = t.pick(L("kN", "кН"))
    var round by remember { mutableIntStateOf(0) }
    // knihy se na polici skládají postupně, rozpočet po metrech se objeví po správném odhadu
    val books = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }
    val q1 = rememberQuiz(
        L("Knihy zabírají 3 m police. Kolik kN váží dohromady?", "Книги занимают 3 м полки. Сколько кН они весят вместе?"),
        qOpts(listOf(2.0, 3.0, 5.0, 6.0), q, 3.0),
        explain = L("Q = q · ℓ = 2 kN/m · 3 m = 6 kN – po metrech 2 + 2 + 2.", "Q = q · ℓ = 2 кН/м · 3 м = 6 кН — по метрам 2 + 2 + 2."),
    )
    val n2 = rememberNum(
        L("Knihy na 1,5 m: kolik kN?", "Книги на 1,5 м: сколько кН?"),
        listOf(NumSpec(L("Q", "Q"), 3.0, kn)),
        explain = L(
            "Q = q · ℓ = 2 kN/m · 1,5 m = 3 kN – celý metr 2 kN a půl metru 1 kN.",
            "Q = q · ℓ = 2 кН/м · 1,5 м = 3 кН — целый метр 2 кН и полметра 1 кН.",
        ),
        traps = qTraps(listOf(2.0, 1.5, 3.5), q, 1.5),
        nudge = L("Na každý metr připadají 2 kN. Kolik připadne na půl metru?", "На каждый метр приходится 2 кН. Сколько придётся на полметра?"),
    )
    LaunchedEffect(round) {
        reveal.snapTo(0f)
        books.snapTo(0f)
        books.animateTo(1f, tween(900))
    }
    LaunchedEffect(q1.solved) {
        if (q1.solved && round == 0) {
            reveal.animateTo(1f, tween(500))
            delay(1400)
            round = 1
        }
    }
    LaunchedEffect(n2.solved) { if (n2.solved) reveal.animateTo(1f, tween(500)) }
    val len = lens[round]
    val y = 130.0
    val x1 = 40.0
    val m = 80.0
    Stage(h = 220.0, description = t.pick(L("Knihy na polici, 2 kN na každý metr", "Книги на полке, 2 кН на каждый метр"))) { c ->
        beam(x1, x1 + 4 * m, y, c)
        // knihy na zatížené délce
        val end = x1 + len * m
        val total = ((end - x1 - 4) / 13).toInt()
        for (i in 0 until total) {
            val a = (books.value * total - i).coerceIn(0f, 1f)
            if (a <= 0f) continue
            val h = 28.0 + (i * 7 % 10)
            rect(x1 + 3 + i * 13.0, y - 5 - h, 10.0, h, c.load.copy(alpha = 0.3f), c.load, 1.0, alpha = a)
        }
        text(x1 + len * m / 2, y - 82, " = 2 kN/m", c.load, sym = "q")
        // metry pod policí
        for (i in 0..4) line(x1 + i * m, y + 8, x1 + i * m, y + 16, c.ink, 0.9)
        for (i in 0 until 4) text(x1 + i * m + m / 2, y + 30, "1 m", c.inkSoft, 12.0)
        dim(x1, end, y + 54, "ℓ = ${num(len)} m", c.ink)
        // po správném odhadu: kolik kN připadá na každý (i neúplný) metr
        if (reveal.value > 0f) {
            var a = 0.0
            while (a < len - 1e-9) {
                val b = minOf(a + 1.0, len)
                val xa = x1 + a * m
                val xb = x1 + b * m
                line(xa + 3, y - 50, xb - 3, y - 50, c.derived, 1.4, alpha = reveal.value)
                text((xa + xb) / 2, y - 56, "${num(q * (b - a))} kN", c.derived, 13.0, alpha = reveal.value)
                a = b
            }
        }
    }
    CardText(L(
        "[[q]] = 2 kN/m znamená: **na každý metr** police připadají 2 kN knih.",
        "[[q]] = 2 кН/м означает: **на каждый метр** полки приходится 2 кН книг.",
    ))
    Steps(
        q1.step8(columns = 4),
        n2.step8(
            L("Teď knihy zabírají jen 1,5 m. Kolik kN váží?", "Теперь книги занимают только 1,5 м. Сколько кН они весят?"),
            more = {
                Note(Tone.INFO, L(
                    "q ještě není síla – je to síla **na metr** (kN/m). Sílu Q v kN dostaneš, až když q vynásobíš zatíženou délkou: kN/m · m = kN. Neúplný metr přidá i neúplnou část.",
                    "q — это ещё не сила, а сила **на метр** (кН/м). Силу Q в кН получишь, только умножив q на нагруженную длину: кН/м · м = кН. Неполный метр добавляет и неполную часть.",
                ))
            },
        ),
    )
}

/** Zatížení na nosníku o rozpětí 6 m: q (kN/m) od [a] (m od A) na délce [l]. */
private class LoadCase(val q: Double, val a: Double, val l: Double) {
    val big get() = q * l
    val xc get() = a + l / 2
}

private const val SPAN = 6.0

/**
 * Karta 1: nahradit = stejný účinek. Pod podporami jsou reakce od spojitého zatížení. Žák určí velikost Q,
 * postaví ji na místo, kde má působit, a nahradí jí zatížení. Reakce se nezmění, jen když Q leží uprostřed.
 * U druhého zatížení místo zapíše číslem – špatné místo se pozná na reakcích stejně.
 */
@Composable
private fun CardScope.Resultant() {
    val t = tr()
    val kn = t.pick(L("kN", "кН"))
    val cases = remember { listOf(LoadCase(2.0, 0.0, 3.0), LoadCase(3.0, 2.5, 3.0)) }
    var round by remember { mutableIntStateOf(0) }
    var placed by remember { mutableStateOf(false) } // první zatížení správně nahrazeno
    var xq by remember { mutableStateOf(0.0) }
    var grab by remember { mutableStateOf(false) }
    val merge = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val q1 = rememberQuiz(
        L("Zatížení 2 kN/m na 3 m. Jak velká je výslednice Q?", "Нагрузка 2 кН/м на 3 м. Какова величина равнодействующей Q?"),
        qOpts(listOf(2.0, 3.0, 5.0, 6.0), 2.0, 3.0),
        explain = L("Q = q · ℓ = 2 · 3 = 6 kN.", "Q = q · ℓ = 2 · 3 = 6 кН."),
    )
    val n3 = rememberNum(
        L("Druhé zatížení: velikost Q", "Вторая нагрузка: величина Q"),
        listOf(NumSpec(L("Q", "Q"), 9.0, kn)),
        explain = L("Q = q · ℓ = 3 · 3 = 9 kN.", "Q = q · ℓ = 3 · 3 = 9 кН."),
        traps = qTraps(listOf(3.0, 6.0, 4.5), 3.0, 3.0),
    )
    val n4 = rememberNum(
        L("Druhé zatížení: poloha Q", "Вторая нагрузка: положение Q"),
        listOf(NumSpec(L("x", "x"), 4.0, t.pick(L("m", "м")))),
        explain = L(
            "x = 2,5 + 3/2 = 4 m – uprostřed zatížené délky, měřeno od A.",
            "x = 2,5 + 3/2 = 4 м — посередине нагруженного участка, считая от A.",
        ),
        traps = listOf(
            NumTrap.near(L("1,5 m je od **začátku zatížení**. To ale nezačíná v A.", "1,5 м — это от **начала нагрузки**. Но она начинается не в A."), 1.5),
            NumTrap.near(L("Tam zatížení začíná. Výslednice je v jeho středu.", "Там нагрузка начинается. Равнодействующая — в её середине."), 2.5),
            NumTrap.near(L("Tam zatížení končí. Výslednice je v jeho středu.", "Там нагрузка заканчивается. Равнодействующая — в её середине."), 5.5),
        ),
        nudge = L("Střed zatížené délky = začátek zatížení + polovina jeho délky.", "Середина нагруженного участка = начало нагрузки + половина её длины."),
    )
    val lc = cases[round]
    // Q je na obrázku: u prvního zatížení po odhadu velikosti, u druhého po prvním zapsaném místě
    val showQ = if (round == 0) q1.solved else (n4.misses > 0 || n4.solved)
    val placing = round == 0 && q1.solved && !placed
    val sizeKnown = if (round == 0) q1.solved else n3.solved

    fun moveQ(v: Double) {
        if (!placing || merge.isRunning) return
        xq = v.coerceIn(0.0, SPAN)
        // posunutá Q už zatížení nenahrazuje – hřeben se vrátí
        if (merge.value > 0f) scope.launch { merge.animateTo(0f, tween(400)) }
    }
    fun replace() {
        if (!placing || merge.isRunning) return
        val ok = abs(xq - lc.xc) < 1e-9
        scope.launch {
            merge.animateTo(1f, tween(900))
            if (ok) placed = true
        }
    }
    // po prvním zatížení: nový obrázek s druhým zatížením
    LaunchedEffect(placed) {
        if (placed && round == 0) {
            delay(1800)
            round = 1
            xq = 0.0
            merge.snapTo(0f)
        }
    }
    // druhé zatížení: zapsané místo → Q se tam postaví a nahradí zatížení; reakce ukážou, jestli sedí
    LaunchedEffect(n4.misses, n4.solved) {
        if (round != 1 || !(n4.misses > 0 || n4.solved)) return@LaunchedEffect
        val v = if (n4.solved) 4.0 else (parseNum(n4.values[0]) ?: return@LaunchedEffect)
        merge.snapTo(0f)
        xq = v.coerceIn(0.0, SPAN)
        merge.animateTo(1f, tween(900))
    }
    // přepsané číslo: zatížení se vrátí, dokud žák znovu nezkontroluje
    LaunchedEffect(n4.values) { if (round == 1 && !n4.solved && merge.value > 0f) merge.animateTo(0f, tween(400)) }

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
            if (!placing) return@Stage
            if (start) grab = pt.dist(gx(xq), y - 5 - lc.big * 8) < 45 || abs(pt.x - gx(xq)) < 30
            if (grab) moveQ((((pt.x - xA) / m) * 2).roundToInt() / 2.0)
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
        if (showQ) {
            val len = lc.big * 8
            val xx = gx(xq)
            val a = if (mv < 0.05f) 0.9f else mv.coerceAtLeast(0.3f)
            force(xx, y - 5 - len, xx, y - 5, c.derived, 2.6, alpha = a, dash = mv < 0.5f)
            text(xx, y - 5 - len - 16, " = ${num(lc.big)} kN", c.derived, 14.0, sym = "Q", alpha = a)
            if (placing) handle(xx, y - 5 - len, c, grab)
            dim(gx(0.0), xx, y + 106, "x = ${num(xq)} m", c.derived, if (xq > 0) a else 0f)
        }
        // reakce podpor: před nahrazením od spojitého zatížení, po nahrazení od Q (dokud žák neurčí Q, jen „?“)
        val after = mv > 0.5f
        val ra = if (after) raQ else raDist
        val rb = if (after) rbQ else rbDist
        val rc = if (!after) c.react else if (same) c.accent else c.load
        force(gx(0.0), y + 62, gx(0.0), y + 6, c.react)
        force(gx(SPAN), y + 62, gx(SPAN), y + 6, c.react)
        val mark = if (!after) "" else if (same) " ✓" else " ✗"
        text(gx(0.0) + 10, y + 58, if (sizeKnown) " = ${num(ra)} kN$mark" else " = ?", rc, 14.0, Anchor.START, sym = "R_A")
        text(gx(SPAN) - 10, y + 58, if (sizeKnown) " = ${num(rb)} kN$mark" else " = ?", rc, 14.0, Anchor.END, sym = "R_B")
        // kóty zatížení
        if (lc.a > 0) dim(gx(0.0), gx(lc.a), y + 84, "${num(lc.a)} m", c.ink)
        dim(gx(lc.a), gx(lc.a + lc.l), y + 84, "ℓ = ${num(lc.l)} m", c.ink)
    }
    CardText(L(
        "Spojité zatížení (sníh, knihy) chceme nahradit **jedinou silou Q** – výslednicí. „Nahradit“ znamená, že to nosník nepozná: podpory musí tlačit **stejně**.",
        "Распределённую нагрузку (снег, книги) хотим заменить **одной силой Q** — равнодействующей. «Заменить» значит, что балка разницы не заметит: опоры должны давить **так же**.",
    ))
    SmallText(L(
        "Pod podporami jsou reakce, které drží spojité zatížení – čísla uvidíš, až určíš velikost Q. Šipky reakcí nejsou v měřítku.",
        "Под опорами — реакции, которые держат распределённую нагрузку; числа появятся, когда найдёшь величину Q. Стрелки реакций не в масштабе.",
    ))
    fun sameNote(k: LoadCase) = L(
        "Reakce se **nezměnily** – Q = ${num(k.big)} kN uprostřed zatížené délky (${num(k.a)} + ${num(k.l)}/2 = ${num(k.xc)} m od A) má na nosník stejný účinek jako celé spojité zatížení.",
        "Реакции **не изменились** — Q = ${num(k.big)} кН посередине нагруженного участка (${num(k.a)} + ${num(k.l)}/2 = ${num(k.xc)} м от A) действует на балку так же, как вся распределённая нагрузка.",
    )
    Steps(
        q1.step8(columns = 4),
        Step(
            L("Postav Q tam, kde má působit, a nahraď jí zatížení", "Поставь Q туда, где она должна действовать, и замени ею нагрузку"), placed,
            result = { Note(Tone.OK, sameNote(cases[0])) },
        ) {
            Chips(listOf(L("◀ 0,5 m", "◀ 0,5 м"), L("0,5 m ▶", "0,5 м ▶"))) { i -> moveQ(xq + if (i == 0) -0.5 else 0.5) }
            Readout(t.pick(L("Q je ${num(xq)} m od A", "Q в ${num(xq)} м от A")))
            ActionButton(L("Nahradit zatížení silou Q", "Заменить нагрузку силой Q"), enabled = merge.value == 0f && !merge.isRunning) { replace() }
            if (merge.value > 0.5f && !same && !merge.isRunning) Note(Tone.NUDGE, L(
                "Reakce vyšly **jinak** – Q v ${num(xq)} m má jiný účinek než zatížení. Rovnoměrné zatížení je kolem svého středu souměrné. Posuň Q a zkus to znovu.",
                "Реакции получились **другими** — Q в ${num(xq)} м действует иначе, чем нагрузка. Равномерная нагрузка симметрична относительно своей середины. Сдвинь Q и попробуй снова.",
            ))
        },
        n3.step8(L("Druhé zatížení: 3 kN/m na 3 m. Jak velká je Q?", "Вторая нагрузка: 3 кН/м на 3 м. Какова величина Q?")),
        n4.step8(
            L("Zatížení začíná 2,5 m od A. Jak daleko od A působí Q?", "Нагрузка начинается в 2,5 м от A. На каком расстоянии от A действует Q?"),
            line = { Note(Tone.OK, sameNote(cases[1])) },
            more = {
                SmallText(L(
                    "Výslednice nahrazuje zatížení jen pro rovnováhu celého nosníku (reakce). Jak se nosník uvnitř prohýbá, to už je jiná otázka.",
                    "Равнодействующая заменяет нагрузку только для равновесия всей балки (реакций). Как балка изгибается внутри — это уже другой вопрос.",
                ))
            },
        ),
    )
}

/** Karta 2: kontrola – výslednice a její rameno, když zatížení nezačíná v podpoře. */
@Composable
private fun CardScope.CheckQ() {
    val t = tr()
    val q1 = rememberQuiz(
        L("Čím tohle spojité zatížení nahradíš?", "Чем заменишь эту распределённую нагрузку?"),
        listOf(
            Opt(L("Q = 6 kN uprostřed zatížení", "Q = 6 кН посередине нагрузки"), correct = true),
            Opt(L("Q = 2 kN uprostřed zatížení", "Q = 2 кН посередине нагрузки"), why = L("2 kN připadají jen na jeden metr. Kolik metrů je zatíženo?", "2 кН приходится только на один метр. Сколько метров нагружено?")),
            Opt(L("Q = 6 kN na konci zatížení", "Q = 6 кН на конце нагрузки"), why = L("Zatížení je všude stejné, takže vlevo od Q ho musí být stejně jako vpravo. Kde bys rovnoměrně naloženou polici podepřel jedním prstem?", "Нагрузка везде одинакова, значит, слева от Q её должно быть столько же, сколько справа. Где бы ты подпёр равномерно загруженную полку одним пальцем?")),
        ),
        explain = L(
            "Q = q · ℓ = 2 · 3 = 6 kN. Působí uprostřed zatížené délky, tedy 1,5 m od jejího začátku.",
            "Q = q · ℓ = 2 · 3 = 6 кН. Приложена посередине нагруженного участка, то есть в 1,5 м от его начала.",
        ),
    )
    val n2 = rememberNum(
        L("Rameno Q k bodu A", "Плечо Q относительно A"),
        listOf(NumSpec(L("r", "r"), 2.5, t.pick(L("m", "м")))),
        explain = L(
            "Rameno = 1 m (od A k začátku zatížení) + ℓ/2 = 1 + 1,5 = **2,5 m**. Moment Q k bodu A: 6 · 2,5 = 15 kNm. Častá chyba je zapomenout na ten první metr.",
            "Плечо = 1 м (от A до начала нагрузки) + ℓ/2 = 1 + 1,5 = **2,5 м**. Момент Q относительно A: 6 · 2,5 = 15 кН·м. Частая ошибка — забыть про этот первый метр.",
        ),
        traps = listOf(
            NumTrap.near(L("1,5 m je od **začátku zatížení**. Zatížení ale nezačíná v A – začíná až o 1 m dál.", "1,5 м — это от **начала нагрузки**. Но нагрузка начинается не в A, а на 1 м дальше."), 1.5),
            NumTrap.near(L("3 m je délka zatížení ℓ, ne vzdálenost výslednice od A.", "3 м — это длина нагрузки ℓ, а не расстояние от равнодействующей до A."), 3.0),
            NumTrap.near(L("4 m od A zatížení končí. Výslednice působí uprostřed zatížené délky.", "В 4 м от A нагрузка заканчивается. Равнодействующая приложена посередине нагруженного участка."), 4.0),
        ),
    )
    val y = 100.0
    val xA = 70.0
    val m = 52.0 // rozpětí 5 m
    Stage(h = 200.0, description = L("Spojité zatížení 2 kN/m na délce 3 m, začíná 1 m od podpory A", "Распределённая нагрузка 2 кН/м на длине 3 м, начинается в 1 м от опоры A").t()) { c ->
        support(SupportType.PIN, xA, y, c, "A")
        support(SupportType.ROLLER, xA + 5 * m, y, c, "B")
        beam(xA - 15, xA + 5 * m + 15, y, c)
        if (!q1.solved) {
            comb(xA + m, xA + 4 * m, y - 5 - 30, y - 5, c)
            text(xA + 2.5 * m, y - 44, " = 2 kN/m", c.load, sym = "q")
        } else {
            // po vyřešení: zatížení nahradila výslednice
            force(xA + 2.5 * m, y - 5 - 48, xA + 2.5 * m, y - 5, c.derived, 2.6)
            text(xA + 2.5 * m + 8, y - 40, " = 6 kN", c.derived, 14.0, Anchor.START, sym = "Q")
            dim(xA, xA + 2.5 * m, y + 80, if (n2.solved) "2,5 m" else "r = ?", c.derived)
        }
        dim(xA, xA + m, y + 54, "1 m", c.ink)
        dim(xA + m, xA + 4 * m, y + 54, "3 m", c.ink)
    }
    CardText(L(
        "Zatížení [[q]] = 2 kN/m začíná 1 m od podpory A a je dlouhé 3 m.",
        "Нагрузка [[q]] = 2 кН/м начинается в 1 м от опоры A и имеет длину 3 м.",
    ))
    Steps(
        q1.step8(),
        n2.step8(L("Jaké rameno má Q k bodu A?", "Какое плечо у Q относительно точки A?")),
    )
}

/**
 * Karta 3: celý nosník od začátku do konce – uvolnění, výslednice, složky šikmé síly, momentové rovnice
 * a kontrola. Prostý nosník 6 m: q = 2 kN/m na 0–3 m, F = 5 kN (sklon 3 : 4) ve 4,5 m.
 * R_B = 4,5 kN, R_Ay = 5,5 kN, R_Ax = −3 kN. Volby dělá žák v kvízech, reakce počítá sám.
 */
@Composable
private fun CardScope.WholeBeam() {
    val t = tr()
    val kn = t.pick(L("kN", "кН"))
    val q1 = rememberQuiz(
        L("Uvolni nosník: čím nahradíš podpory A a B?", "Освободи балку: чем заменишь опоры A и B?"),
        listOf(
            Opt(L("A: [[R_Ay]] · B: [[R_B]]", "A: [[R_Ay]] · B: [[R_B]]"), why = L("Pevná kloubová podpora A nedovolí ani vodorovný posun – a šikmá síla F nosník do strany táhne.", "Шарнирно-неподвижная опора A не даёт и горизонтального сдвига — а наклонная сила F тянет балку вбок.")),
            Opt(L("A: [[R_Ax]], [[R_Ay]] · B: [[R_B]]", "A: [[R_Ax]], [[R_Ay]] · B: [[R_B]]"), correct = true),
            Opt(L("A: [[R_Ax]], [[R_Ay]] · B: [[R_Bx]], [[R_By]]", "A: [[R_Ax]], [[R_Ay]] · B: [[R_Bx]], [[R_By]]"), why = L("Posuvná podpora B dovolí vodorovný posun – vodorovnou reakci nemá.", "Шарнирно-подвижная опора B допускает горизонтальный сдвиг — горизонтальной реакции у неё нет.")),
        ),
        explain = L("Neznámé: [[R_Ax]], [[R_Ay]], [[R_B]] – n = 3 = r.", "Неизвестные: [[R_Ax]], [[R_Ay]], [[R_B]] — n = 3 = r."),
    )
    val q2 = rememberQuiz(
        L("Čím nahradíš zatížení q = 2 kN/m na prvních 3 m?", "Чем заменишь нагрузку q = 2 кН/м на первых 3 м?"),
        listOf(
            Opt(L("Q = 6 kN, 1,5 m od A", "Q = 6 кН, 1,5 м от A"), correct = true),
            Opt(L("Q = 6 kN, 3 m od A", "Q = 6 кН, 3 м от A"), why = L("3 m od A zatížení končí. Výslednice působí uprostřed zatížené délky.", "В 3 м от A нагрузка заканчивается. Равнодействующая приложена посередине нагруженного участка.")),
            Opt(L("Q = 2 kN, 1,5 m od A", "Q = 2 кН, 1,5 м от A"), why = L("2 kN je jen na jeden metr: Q = q · ℓ.", "2 кН — только на один метр: Q = q · ℓ.")),
        ),
        explain = L("Q = 2 · 3 = 6 kN, 1,5 m od A.", "Q = 2 · 3 = 6 кН, 1,5 м от A."),
    )
    val q3 = rememberQuiz(
        L("F = 5 kN má sklon 3 : 4. Jaké jsou její složky?", "F = 5 кН с наклоном 3 : 4. Каковы её составляющие?"),
        listOf(
            Opt(L("[[F_x]] = 4 kN, [[F_y]] = 3 kN", "[[F_x]] = 4 кН, [[F_y]] = 3 кН"), why = L("Prohozené. Vodorovně je to 3 dílky, svisle 4 – jako trojúhelník 3-4-5 z lekce 3.", "Перепутано. По горизонтали 3 деления, по вертикали 4 — как треугольник 3-4-5 из урока 3.")),
            Opt(L("[[F_x]] = 3 kN →, [[F_y]] = 4 kN ↓", "[[F_x]] = 3 кН →, [[F_y]] = 4 кН ↓"), correct = true),
            Opt(L("[[F_x]] = 5 kN, [[F_y]] = 5 kN", "[[F_x]] = 5 кН, [[F_y]] = 5 кН"), why = L("Složky nejsou kopie síly: 5 je přepona, složky jsou odvěsny 3 a 4.", "Составляющие — не копии силы: 5 — гипотенуза, составляющие — катеты 3 и 4.")),
        ),
        explain = L("[[F_x]] = 5 · 3/5 = 3 kN →, [[F_y]] = 5 · 4/5 = 4 kN ↓.", "[[F_x]] = 5 · 3/5 = 3 кН →, [[F_y]] = 5 · 4/5 = 4 кН ↓."),
    )
    val q4 = rememberQuiz(
        L("Chceš rovnou [[R_B]]. Ke kterému bodu sečteš momenty?", "Хочешь сразу [[R_B]]. Относительно какой точки сложишь моменты?"),
        listOf(
            Opt(L("k bodu A", "относительно A"), correct = true),
            Opt(L("k bodu B", "относительно B"), why = L("K bodu B má [[R_B]] rameno 0 – z rovnice by zmizela právě ta reakce, kterou hledáš.", "Относительно B у [[R_B]] плечо 0 — из уравнения исчезнет как раз та реакция, которую ищешь.")),
            Opt(L("ke středu nosníku", "к середине балки"), why = L("Ke středu by v rovnici zůstaly [[R_Ay]] i [[R_B]] – dvě neznámé.", "Относительно середины в уравнении останутся и [[R_Ay]], и [[R_B]] — две неизвестные.")),
        ),
        explain = L("K bodu A zmizí [[R_Ax]] i [[R_Ay]] – zbude jen [[R_B]].", "Относительно A исчезают [[R_Ax]] и [[R_Ay]] — остаётся только [[R_B]]."),
    )
    val n5 = rememberNum(
        L("R_B z momentů k A", "R_B из моментов относительно A"),
        listOf(NumSpec(L("[[R_B]]", "[[R_B]]"), 4.5, kn)),
        explain = L("[[R_B]] = (6 · 1,5 + 4 · 4,5) / 6 = 27/6 = 4,5 kN.", "[[R_B]] = (6 · 1,5 + 4 · 4,5) / 6 = 27/6 = 4,5 кН."),
        traps = listOf(
            NumTrap.near(L("27 kNm je součet momentů zatížení. Ještě vyděl ramenem 6 m.", "27 кН·м — сумма моментов нагрузок. Ещё раздели на плечо 6 м."), 27.0),
            NumTrap.near(L("Q má rameno 1,5 m, ne 3 m: (6 · 1,5 + 4 · 4,5) / 6.", "У Q плечо 1,5 м, а не 3 м: (6 · 1,5 + 4 · 4,5) / 6."), 6.0),
        ),
        nudge = L("Převeď oba záporné členy na pravou stranu a vyděl 6.", "Перенеси оба отрицательных члена вправо и раздели на 6."),
    )
    val n6 = rememberNum(
        L("R_Ay z momentů k B", "R_Ay из моментов относительно B"),
        listOf(NumSpec(L("[[R_Ay]]", "[[R_Ay]]"), 5.5, kn)),
        explain = L("[[R_Ay]] = (6 · 4,5 + 4 · 1,5) / 6 = 33/6 = 5,5 kN.", "[[R_Ay]] = (6 · 4,5 + 4 · 1,5) / 6 = 33/6 = 5,5 кН."),
        traps = listOf(
            NumTrap.near(L("To je [[R_B]]. Dosaď: 6 · 4,5 + 4 · 1,5 = 27 + 6 = 33, a to vyděl ramenem 6 m.", "Это [[R_B]]. Подставь: 6 · 4,5 + 4 · 1,5 = 27 + 6 = 33, и раздели на плечо 6 м."), 4.5),
            NumTrap.near(L("10 kN je součet zatížení – nesou ho obě podpory dohromady.", "10 кН — сумма нагрузок, её несут обе опоры вместе."), 10.0),
            NumTrap.near(L("33 kNm je součet momentů. Ještě vyděl ramenem 6 m.", "33 кН·м — сумма моментов. Ещё раздели на плечо 6 м."), 33.0),
        ),
        nudge = L("K bodu B: Q má rameno 4,5 m, [[F_y]] rameno 1,5 m, [[R_Ay]] rameno 6 m.", "Относительно B: у Q плечо 4,5 м, у [[F_y]] — 1,5 м, у [[R_Ay]] — 6 м."),
    )
    val n7 = rememberNum(
        L("R_Ax ze sil vodorovně", "R_Ax из сил по горизонтали"),
        listOf(NumSpec(L("[[R_Ax]]", "[[R_Ax]]"), -3.0, kn, signed = true)),
        explain = L("[[R_Ax]] = −3 kN: reakce ve skutečnosti míří doleva ←.", "[[R_Ax]] = −3 кН: на самом деле реакция направлена влево ←."),
        traps = listOf(
            NumTrap(L(
                "Složka [[F_x]] táhne doprava. Kam musí mířit reakce, aby ji vyrušila – a jaké to dá znaménko?",
                "Составляющая [[F_x]] тянет вправо. Куда должна быть направлена реакция, чтобы её уравновесить, — и какой это даёт знак?",
            )) { v -> v[0]?.let { abs(it - 3) < 0.02 } == true },
        ),
        nudge = L("Převeď 3 na druhou stranu rovnice. Znaménko nech, jak vyjde.", "Перенеси 3 в другую часть уравнения. Знак оставь таким, как получится."),
    )
    val n8 = rememberNum(
        L("Kontrola ΣF_y", "Проверка ΣF_y"),
        listOf(NumSpec(L("Σ[[F_y]]", "Σ[[F_y]]"), 0.0, kn, signed = true)),
        explain = L("5,5 + 4,5 − 6 − 4 = 10 − 10 = 0.", "5,5 + 4,5 − 6 − 4 = 10 − 10 = 0."),
        traps = listOf(
            NumTrap.near(L("Zatížení míří dolů, takže se odečítají: 10 − 10.", "Нагрузки направлены вниз, поэтому вычитаются: 10 − 10."), 20.0),
        ),
        nudge = L("Sečti zvlášť nahoru (5,5 + 4,5) a dolů (6 + 4).", "Сложи отдельно вверх (5,5 + 4,5) и вниз (6 + 4)."),
    )
    val solved = listOf(q1.solved, q2.solved, q3.solved, q4.solved, n5.solved, n6.solved, n7.solved, n8.solved)
    val step = solved.indexOfFirst { !it }.let { if (it < 0) solved.size else it } // počet hotových kroků
    val prog = remember { Animatable(0f) }
    LaunchedEffect(step) { prog.animateTo(step.toFloat(), tween(700)) }
    fun ph(k: Int) = (prog.value - (k - 1)).coerceIn(0f, 1f) // průběh přechodu po kroku k (1…8)
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
        val p6 = ph(6)
        val p7 = ph(7)
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
            text(gx(0.0) - 28, y - 10, if (p7 > 0.5f) " = −3" else "", c.react, 13.0, sym = "R_Ax", alpha = p1)
            text(gx(0.0) + 8, y + 44, if (p6 > 0.5f) " = 5,5 kN" else null, c.react, 13.0, Anchor.START, sym = "R_Ay", alpha = p1)
            text(gx(6.0) - 8, y + 44, if (p5 > 0.5f) " = 4,5 kN" else null, c.react, 13.0, Anchor.END, sym = "R_B", alpha = p1)
            if (p7 > 0.5f) {
                // skutečný smysl R_Ax čárkovaně
                force(gx(0.0) - 12, y + 16, gx(0.0) - 44, y + 16, c.react, 1.4, 8.0, alpha = p7 * 0.7f, dash = true)
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
        // momentový bod: A pro krok 5, B pro krok 6
        if (p4 > 0f && p5 < 1f) circle(gx(0.0), y, 12.0, c.accent.copy(alpha = 0.15f), c.accent, 1.6, alpha = p4 * (1 - p5))
        if (p5 > 0f && p6 < 1f) circle(gx(6.0), y, 12.0, c.accent.copy(alpha = 0.15f), c.accent, 1.6, alpha = p5 * (1 - p6))
        // kóty
        val yd = y + 76
        val marks = listOf(0.0, 1.5, 3.0, 4.5, 6.0)
        line(gx(0.0), yd, gx(6.0), yd, c.ink, 0.9)
        for (mk in marks) line(gx(mk) - 4, yd + 4, gx(mk) + 4, yd - 4, c.ink, 1.2)
        for (i in 0 until marks.size - 1) text((gx(marks[i]) + gx(marks[i + 1])) / 2, yd - 5, num(marks[i + 1] - marks[i]), c.ink, 12.0)
        text(gx(6.0) + 8, yd + 4, "m", c.inkSoft, 11.0, Anchor.START)
        if (step >= 8) text(200.0, yd + 30, t.pick(L("Σ F_y = 0 ✓   Σ M = 0 ✓", "Σ F_y = 0 ✓   Σ M = 0 ✓")), c.accent, 15.0, bold = true)
    }
    CardText(L(
        "Teď všechno dohromady: spojité zatížení **i** šikmá síla na jednom nosníku. Postupuj tak, jak budeš řešit úlohy v trenažéru.",
        "Теперь всё вместе: распределённая нагрузка **и** наклонная сила на одной балке. Действуй так же, как будешь решать задачи в тренажёре.",
    ))
    Steps(
        q1.step8(line = { SolLine(L("Neznámé: [[R_Ax]], [[R_Ay]], [[R_B]] – n = 3 = r", "Неизвестные: [[R_Ax]], [[R_Ay]], [[R_B]] — n = 3 = r")) }),
        q2.step8(line = { SolLine(L("Q = 2 · 3 = 6 kN, 1,5 m od A", "Q = 2 · 3 = 6 кН, 1,5 м от A")) }),
        q3.step8(line = { SolLine(L("[[F_x]] = 3 kN →,  [[F_y]] = 4 kN ↓", "[[F_x]] = 3 кН →,  [[F_y]] = 4 кН ↓")) }),
        q4.step8(line = { SolLine(L("Σ[[M_A]]: [[R_B]]·6 − 6·1,5 − 4·4,5 = 0", "Σ[[M_A]]: [[R_B]]·6 − 6·1,5 − 4·4,5 = 0")) }),
        n5.step8(
            L("Vyřeš rovnici Σ[[M_A]] = 0: kolik je [[R_B]]?", "Реши уравнение Σ[[M_A]] = 0: чему равна [[R_B]]?"),
            show = { Readout("[[R_B]]·6 − 6·1,5 − 4·4,5 = 0") },
            line = { SolLine(L("[[R_B]] = 27/6 = 4,5 kN", "[[R_B]] = 27/6 = 4,5 кН")) },
        ),
        n6.step8(
            L("Momenty k bodu B: kolik je [[R_Ay]]?", "Моменты относительно B: чему равна [[R_Ay]]?"),
            show = { Readout("−[[R_Ay]]·6 + 6·4,5 + 4·1,5 = 0") },
            line = { SolLine(L("Σ[[M_B]]: [[R_Ay]] = 33/6 = 5,5 kN", "Σ[[M_B]]: [[R_Ay]] = 33/6 = 5,5 кН")) },
        ),
        n7.step8(
            L("Vodorovně: [[R_Ax]] + 3 = 0. Kolik je [[R_Ax]] (se znaménkem)?", "По горизонтали: [[R_Ax]] + 3 = 0. Чему равна [[R_Ax]] (со знаком)?"),
            line = { SolLine(L("Σ[[F_x]]: [[R_Ax]] = −3 kN (ve skutečnosti ←)", "Σ[[F_x]]: [[R_Ax]] = −3 кН (на самом деле ←)")) },
        ),
        n8.step8(
            L("Kontrola: kolik vyjde Σ[[F_y]] = 5,5 + 4,5 − 6 − 4?", "Проверка: чему равна Σ[[F_y]] = 5,5 + 4,5 − 6 − 4?"),
            line = { SolLine(L("Σ[[F_y]] = 0 ✓", "Σ[[F_y]] = 0 ✓"), ok = true) },
            more = {
                Note(Tone.OK, L(
                    "Hotovo: [[R_Ax]] = −3 kN (←), [[R_Ay]] = 5,5 kN, [[R_B]] = 4,5 kN. Postup: uvolnit → nahradit q výslednicí → rozložit šikmé síly → momenty k bodu, kde zmizí co nejvíc neznámých → kontrola.",
                    "Готово: [[R_Ax]] = −3 кН (←), [[R_Ay]] = 5,5 кН, [[R_B]] = 4,5 кН. Порядок: освободить → заменить q равнодействующей → разложить наклонные силы → моменты относительно точки, где исчезает больше всего неизвестных → проверка.",
                ))
            },
        ),
    )
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
