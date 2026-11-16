package cz.usharik.statika.learn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.PrimBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import cz.usharik.statika.trainer.Tone
import kotlin.math.abs

/*
 * Lekce 2 – Rovnováha sil. Nevyrovnaná síla těleso rozjíždí se zrychlením (a = ΣF / m), při ΣF = 0 se rychlost
 * nemění. Když těleso stojí, síly na něj působí, ale navzájem se vyruší: ΣF_x = 0 a ΣF_y = 0, každý směr zvlášť.
 */

/** 12 jednotek na 1 kN – při 8 kN šipka zůstane v obrázku. */
private const val K = 12.0

/** 1 m dráhy = 20 jednotek obrázku. */
private const val M = 20.0

/** Vozík na kolečkách (tření zanedbáme): obdélník se středem cx a horní hranou top. */
private fun PrimBuilder.cart(cx: Double, top: Double, w: Double, h: Double, c: cz.usharik.statika.ui.StatikaColors, alpha: Float = 1f, ghost: Boolean = false) {
    if (ghost) {
        rect(cx - w / 2, top, w, h, null, c.inkSoft, 1.0, alpha)
        return
    }
    rect(cx - w / 2, top, w, h, c.subtle, c.ink, 1.6, alpha)
    circle(cx - w / 3, top + h + 6, 6.0, c.paper, c.ink, 1.5, alpha)
    circle(cx + w / 3, top + h + 6, 6.0, c.paper, c.ink, 1.5, alpha)
}

/**
 * Karta 0: přetahovaná. Vyrovnané síly → vozík stojí. Nevyrovnané → vozík se rozjíždí **se zrychlením**:
 * záblesky (stroboskop) po 0,5 s jsou čím dál dál od sebe a šipka rychlosti roste. a = ΣF / m.
 */
@Composable
private fun CardScope.TugOfWar() {
    val t = tr()
    var left by remember { mutableStateOf(3f) }
    var right by remember { mutableStateOf(5f) }
    var reached by remember { mutableStateOf(false) }
    var sawRun by remember { mutableStateOf(false) }
    val res = right - left
    val balanced = abs(res) < 0.01f
    val mass = 2.0 // t
    val acc = res / mass // m/s²
    // poloha a rychlost vozíku (m, m/s), záblesky stroboskopu (m)
    var x by remember { mutableDoubleStateOf(0.0) }
    var v by remember { mutableDoubleStateOf(0.0) }
    var fade by remember { mutableStateOf(1f) }
    val ghosts = remember { mutableStateListOf<Double>() }
    // pokus: síly se nastaví u stojícího vozíku, „Pusť vozík“ ho pustí z klidu (žádný skrytý restart při posunu posuvníku)
    var runId by remember { mutableStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var lastRes by remember { mutableStateOf<Float?>(null) } // Σ F_x posledního puštění (ukáže se až po puštění)
    var leftSet by remember { mutableStateOf(false) } // krok 2: levý tým přešel na 4,5 kN
    var stillRun by remember { mutableStateOf(false) } // poslední pokus byl vyrovnaný a vozík opravdu stál
    LaunchedEffect(runId) {
        if (runId == 0) return@LaunchedEffect
        val a = acc
        lastRes = res
        running = true
        stillRun = false
        x = 0.0
        v = 0.0
        ghosts.clear()
        for (i in 1..6) {
            fade = i / 6f
            delay(25)
        }
        var time = 0.0
        var last = 0L
        var nextGhost = 0.0
        while (abs(x) * M <= 150 && time < 3.0) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.0 else (now - last) / 1e9
                last = now
                time += dt
                x = 0.5 * a * time * time
                v = a * time
                if (time >= nextGhost) {
                    ghosts += x
                    nextGhost += 0.5
                }
            }
        }
        if (abs(a) < 0.01) {
            // vyrovnané síly: 3 s a záblesky pořád na jednom místě
            stillRun = true
            reached = true
        } else {
            sawRun = true
            // dojel na kraj obrázku: pokus skončil, vozík zmizí (stopa zůstane, dokud nezačne další pokus)
            for (i in 1..10) {
                fade = 1f - i / 10f
                delay(30)
            }
            // nový vozík na startu; stopa minulého pokusu zůstane pro srovnání
            delay(250)
            x = 0.0
            v = 0.0
            for (i in 1..6) {
                fade = i / 6f
                delay(25)
            }
        }
        running = false
    }
    Stage(h = 210.0, description = t.pick(L("Vozík tažený dvěma silami proti sobě", "Тележка, которую тянут две силы в разные стороны"))) { c ->
        val cy = 92.0
        val floor = cy + 36
        line(10.0, floor, 390.0, floor, c.ink, 1.5)
        // stroboskop: kde byl vozík každou půlsekundu
        for (g in if (running) ghosts.dropLast(1) else ghosts) {
            cart(200.0 + g * M, cy - 24, 60.0, 48.0, c, 0.3f, ghost = true)
            line(200.0 + g * M, floor + 4, 200.0 + g * M, floor + 12, c.inkSoft, 1.2)
        }
        val cx = 200.0 + x * M
        cart(cx, cy - 24, 60.0, 48.0, c, fade)
        // síly táhnou ven z vozíku
        val lEnd = cx - 30 - left * K
        val rEnd = cx + 30 + right * K
        force(cx - 30, cy, lEnd, cy, c.load, width = 3.0, alpha = fade)
        text(lEnd, cy - 34, " = ${num(left.toDouble(), 1)} kN", c.load, 14.0, Anchor.START, sym = "F_1", alpha = fade)
        force(cx + 30, cy, rEnd, cy, c.load, width = 3.0, alpha = fade)
        text(rEnd, cy - 34, " = ${num(right.toDouble(), 1)} kN", c.load, 14.0, Anchor.END, sym = "F_2", alpha = fade)
        // rychlost: zelená šipka nad vozíkem, roste
        if (abs(v) > 0.05) {
            force(cx, cy - 46, cx + v * 8, cy - 46, c.accent, width = 2.4, alpha = fade)
            text(cx + v * 4, cy - 56, "v = ${num(abs(v), 1)} m/s", c.accent, 12.0, alpha = fade)
        }
        val shown = running || ghosts.isNotEmpty()
        if (shown && !balanced) {
            text(
                200.0, floor + 34,
                t.pick(
                    if (res > 0) L("převaha ${num(abs(res).toDouble(), 1)} kN doprava → vozík **zrychluje**", "перевес ${num(abs(res).toDouble(), 1)} кН вправо → тележка **разгоняется**")
                    else L("převaha ${num(abs(res).toDouble(), 1)} kN doleva → vozík **zrychluje**", "перевес ${num(abs(res).toDouble(), 1)} кН влево → тележка **разгоняется**"),
                ).replace("**", ""),
                c.derived, 13.0,
            )
            text(200.0, floor + 54, t.pick(L("čárky: kde byl vozík každou půlsekundu", "штрихи: где была тележка каждые полсекунды")), c.inkSoft, 11.0)
        } else if (shown) {
            text(200.0, floor + 40, t.pick(if (stillRun) L("rovnováha – vozík pořád stojí", "равновесие — тележка всё ещё стоит") else L("síly jsou stejné", "силы одинаковые")), c.accent, 16.0, bold = true)
            if (stillRun) text(200.0, floor + 58, t.pick(L("všechny záblesky na jednom místě", "все вспышки в одном месте")), c.inkSoft, 11.0)
        }
    }
    CardText(L(
        "Dva týmy táhnou vozík (2 t, kolečka bez tření) každý na svou stranu. Záblesky ukazují, kde byl vozík každou půlsekundu. Σ[[F_x]] = součet vodorovných sil: doprava +, doleva −.",
        "Две команды тянут тележку (2 т, колёса без трения) каждая в свою сторону. Вспышки показывают, где была тележка каждые полсекунды. Σ[[F_x]] — сумма горизонтальных сил: вправо +, влево −.",
    ))
    fun fresh() {
        // nové nastavení = nový pokus: starý vozík i stopa zmizí, nový stojí na startu
        stillRun = false
        lastRes = null
        x = 0.0
        v = 0.0
        ghosts.clear()
        fade = 1f
    }
    // krok 2: po prvním pokusu (až dojede a objeví se nový vozík) táhne levý tým 4,5 kN
    LaunchedEffect(sawRun, running) {
        if (sawRun && !running && !leftSet) {
            delay(400)
            left = 4.5f
            leftSet = true
            fresh()
        }
    }
    val sumLine: @Composable () -> Unit = {
        lastRes?.let { r ->
            if (abs(r) < 0.01f) Readout("Σ[[F_x]] = ${num(right.toDouble(), 1)} − ${num(left.toDouble(), 1)} = 0", ok = true)
            else Readout("Σ[[F_x]] = ${num(right.toDouble(), 1)} − ${num(left.toDouble(), 1)} = ${num(r.toDouble(), 1)} kN → a = ${num(abs(r / mass), 2)} m/s²")
        }
    }
    val release: @Composable () -> Unit = { ActionButton(L("Pusť vozík", "Отпустить тележку"), enabled = !running) { runId++ } }
    val rightSlider: @Composable () -> Unit = {
        ValueSlider(t.pick(L("Pravý tým [[F_2]] = ${num(right.toDouble(), 1)} kN", "Правая команда [[F_2]] = ${num(right.toDouble(), 1)} кН")), right, 1f..8f, steps = 13) {
            if (!running) {
                right = it
                fresh()
            }
        }
    }
    Steps(
        Step(
            L("Pusť vozík: vpravo táhne 5 kN, vlevo 3 kN", "Отпусти тележку: вправо тянут 5 кН, влево — 3 кН"), sawRun,
            result = {
                SmallText(L(
                    "Zrychlení a = Σ[[F]] / m: převaha 5 − 3 = 2 kN rozjíždí dvoutunový vozík o 2 / 2 = 1 m/s každou sekundu. Proto jsou čárky čím dál dál od sebe.",
                    "Ускорение a = Σ[[F]] / m: перевес 5 − 3 = 2 кН разгоняет двухтонную тележку на 2 / 2 = 1 м/с каждую секунду. Поэтому штрихи всё дальше друг от друга.",
                ))
            },
        ) {
            sumLine()
            release()
        },
        Step(L("Vlevo táhne 4,5 kN. Nastav pravý tým tak, aby vozík zůstal stát", "Слева тянут 4,5 кН. Подбери силу правой команды, чтобы тележка стояла"), reached) {
            rightSlider()
            sumLine()
            release()
            lastRes?.let { r ->
                if (!running && abs(r) >= 0.01f) SmallText(
                    if (r > 0) L("Převaha doprava – vozík se rozjel doprava. Zmenši [[F_2]].", "Перевес вправо — тележка поехала вправо. Уменьши [[F_2]].")
                    else L("Převaha doleva – vozík se rozjel doleva. Zvětši [[F_2]].", "Перевес влево — тележка поехала влево. Увеличь [[F_2]]."),
                )
            }
        },
        quizStep(
            question = L("Vozík stojí. Působí na něj teď nějaké síly?", "Тележка стоит. Действуют ли на неё сейчас какие-то силы?"),
            options = listOf(
                Opt(L("ne, žádné", "нет, никаких"), why = L("Podívej se na lana – oba týmy pořád táhnou silou 4,5 kN.", "Посмотри на верёвки — обе команды по-прежнему тянут с силой 4,5 кН.")),
                Opt(L("ano, dvě stejně velké proti sobě", "да, две равные навстречу"), correct = true),
                Opt(L("jen ta větší", "только бóльшая"), why = L("Žádná není větší – obě mají 4,5 kN. Kdyby jedna převažovala, vozík by se rozjel.", "Ни одна не больше — обе по 4,5 кН. Если бы одна перевешивала, тележка бы поехала.")),
            ),
            explain = L(
                "Tomu se říká **rovnováha**: síly působí dál, jen se navzájem vyruší. Rovnováha ≠ žádné síly.",
                "Это и есть **равновесие**: силы по-прежнему действуют, просто уравновешивают друг друга. Равновесие ≠ отсутствие сил.",
            ),
        ),
        after = {
            ValueSlider(t.pick(L("Levý tým [[F_1]] = ${num(left.toDouble(), 1)} kN", "Левая команда [[F_1]] = ${num(left.toDouble(), 1)} кН")), left, 1f..8f, steps = 13) {
                if (!running) {
                    left = it
                    fresh()
                }
            }
            rightSlider()
            sumLine()
            release()
        },
    )
}

/**
 * Karta 1: rovnováha neznamená jen „stojí“. Vozík na ledě se 1,5 s rozjíždí silou, pak síla zmizí a vozík jede
 * dál **stejně rychle** – záblesky jsou stejně daleko od sebe. ΣF = 0 ⇔ rychlost se nemění. Statika řeší klid.
 */
@Composable
private fun CardScope.Coast() {
    val t = tr()
    var ran by remember { mutableStateOf(false) }
    var guess by remember { mutableStateOf<Int?>(null) } // tip před pokusem: 0 zastaví se, 1 stejně rychle, 2 zrychluje
    var running by remember { mutableStateOf(false) }
    var x by remember { mutableDoubleStateOf(0.0) }
    var v by remember { mutableDoubleStateOf(0.0) }
    var pushing by remember { mutableStateOf(false) }
    val ghosts = remember { mutableStateListOf<Pair<Double, Boolean>>() } // poloha a „ještě tlačil“
    val scope = rememberCoroutineScope()
    val push = 1.5 // s
    val acc = 2.0 // m/s² (2 kN na 1 t)
    val x0 = 40.0
    fun start() {
        if (running) return
        running = true
        scope.launch {
            x = 0.0
            v = 0.0
            ghosts.clear()
            var time = 0.0
            var last = 0L
            var nextGhost = 0.0
            // vozík jede, dokud nezmizí za pravým okrajem (nikde se sám nezastaví)
            while (x0 + x * M < 400) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0.0 else (now - last) / 1e9
                    last = now
                    time += dt
                    pushing = time < push
                    if (pushing) {
                        x = 0.5 * acc * time * time
                        v = acc * time
                    } else {
                        v = acc * push
                        x = 0.5 * acc * push * push + v * (time - push)
                    }
                    if (time >= nextGhost) {
                        ghosts += x to pushing
                        nextGhost += 0.25
                    }
                }
            }
            pushing = false
            ran = true
            delay(600)
            running = false
        }
    }
    Stage(h = 180.0, description = t.pick(L("Vozík na ledě: nejdřív ho síla rozjíždí, pak jede sám", "Тележка на льду: сначала сила её разгоняет, потом она едет сама"))) { c ->
        val cy = 70.0
        val floor = cy + 30
        rect(10.0, floor, 380.0, 6.0, c.react.copy(alpha = 0.12f), null)
        line(10.0, floor, 390.0, floor, c.ink, 1.5)
        for ((g, p) in ghosts) {
            val gx = x0 + g * M
            line(gx, floor + 8, gx, floor + 18, if (p) c.load else c.accent, 1.6)
        }
        val cx = x0 + x * M
        // u pravého okraje se vozík rozplyne – jede dál, jen už mimo obrázek
        val al = ((392 - cx) / 40).coerceIn(0.0, 1.0).toFloat()
        if (running || !ran) cart(cx, cy - 20, 44.0, 40.0, c, al)
        if (pushing) {
            force(cx - 22 - 40, cy, cx - 22, cy, c.load, width = 3.0)
            text(cx - 44, cy - 14, " = 2 kN", c.load, 13.0, sym = "F")
        }
        if (v > 0.05 && al > 0.3f && running) {
            force(cx, cy - 32, cx + v * 12, cy - 32, c.accent, width = 2.2, alpha = al)
            text(cx + v * 6, cy - 40, "v = ${num(v, 1)} m/s", c.accent, 12.0, alpha = al)
        }
        if (ran && !running) text(380.0, cy, t.pick(L("…a jede dál →", "…и едет дальше →")), c.accent, 12.0, Anchor.END)
        if (ghosts.isNotEmpty()) {
            text(16.0, floor + 38, t.pick(L("červeně: síla působí – čárky se vzdalují", "красные: сила действует — штрихи раздвигаются")), c.load, 11.0, Anchor.START)
            text(16.0, floor + 54, t.pick(L("zeleně: žádná síla – čárky stejně daleko", "зелёные: силы нет — штрихи на равных расстояниях")), c.accent, 11.0, Anchor.START)
        }
    }
    CardText(L(
        "Vozík (1 t) stojí na hladkém ledě, tření zanedbáme. Síla 2 kN ho 1,5 s rozjíždí a pak přestane působit. Čárky ukazují, kde byl každou čtvrtsekundu.",
        "Тележка (1 т) стоит на гладком льду, трением пренебрегаем. Сила 2 кН разгоняет её 1,5 с и перестаёт действовать. Штрихи показывают, где она была каждую четверть секунды.",
    ))
    val options = listOf(
        L("postupně se zastaví", "постепенно остановится"),
        L("pojede dál stejně rychle", "поедет дальше с той же скоростью"),
        L("bude dál zrychlovat", "будет и дальше разгоняться"),
    )
    Steps(
        Step(
            L("Tipni si: co vozík udělá, až síla přestane působit?", "Как думаешь: что сделает тележка, когда сила перестанет действовать?"), guess != null,
            result = { guess?.let { SmallText(L("Tvůj tip: **${options[it].cs}**", "Твой прогноз: **${options[it].ru}**")) } },
        ) {
            Chips(options, guess) { if (guess == null) guess = it }
        },
        Step(
            L("Spusť pokus a sleduj rozestupy čárek po 1,5 s", "Запусти опыт и следи за расстоянием между штрихами после 1,5 с"), ran,
            result = {
                val g = guess ?: 1
                Note(if (g == 1) Tone.OK else Tone.INFO, L(
                    when (g) {
                        1 -> "Správně – zelené čárky jsou stejně daleko od sebe: rychlost se nemění."
                        0 -> "Nezastavil se. Zastavit by ho musela síla proti pohybu – tření nebo brzda. Tady žádná není, zelené čárky jsou stejně daleko od sebe."
                        else -> "Nezrychluje. Zrychlovat ho může jen nevyrovnaná síla – a ta zmizela. Zelené čárky jsou stejně daleko od sebe."
                    } + " Když je Σ[[F]] = 0, rychlost se **nemění**: těleso stojí, nebo jede rovnoměrně přímočaře (1. Newtonův zákon). Statika řeší ten klidný případ: všechno stojí a Σ[[F]] = 0.",
                    when (g) {
                        1 -> "Верно — зелёные штрихи на равных расстояниях: скорость не меняется."
                        0 -> "Не остановилась. Чтобы её остановить, нужна сила против движения — трение или тормоз. Здесь её нет, зелёные штрихи на равных расстояниях."
                        else -> "Не разгоняется. Разгонять её может только неуравновешенная сила — а она исчезла. Зелёные штрихи на равных расстояниях."
                    } + " Когда Σ[[F]] = 0, скорость **не меняется**: тело стоит или движется равномерно и прямолинейно (1-й закон Ньютона). Статика изучает спокойный случай: всё стоит и Σ[[F]] = 0.",
                ))
            },
        ) {
            ActionButton(L("Spusť pokus", "Запустить опыт"), enabled = !running) { start() }
        },
        after = {
            ActionButton(L("Přehrát znovu", "Повторить"), enabled = !running) { start() }
        },
    )
}

/**
 * Karta 2: bedna na stole a ruka, která ji shora přitlačuje. Síla stolu není „tíha“ – je taková,
 * jakou vyžaduje rovnováha: N = G + P. Všechny síly leží na jedné svislé nositelce.
 */
@Composable
private fun CardScope.TableForce() {
    val t = tr()
    val solved = solvedHere
    Stage(h = 230.0, description = t.pick(L("Bedna na stole, shora ji tlačí ruka; tíha a ruka dolů, stůl nahoru", "Ящик на столе, сверху его давит рука; сила тяжести и рука вниз, стол вверх"))) { c ->
        val k = 8.0 // délka šipky na 1 kN
        val top = 140.0 // horní plocha stolu
        val floor = 222.0
        line(40.0, floor, 360.0, floor, c.ink, 1.5)
        rect(90.0, top, 220.0, 10.0, c.beam, c.ink, 1.6)
        rect(102.0, top + 10, 8.0, floor - top - 10, c.beam, c.ink, 1.4)
        rect(290.0, top + 10, 8.0, floor - top - 10, c.beam, c.ink, 1.4)
        rect(165.0, top - 80, 70.0, 80.0, c.subtle, c.ink, 1.5)
        circle(200.0, top - 40, 2.5, c.ink)
        // ruka shora
        rect(180.0, top - 80 - 3 * k - 22, 40.0, 14.0, c.paper, c.ink, 1.3)
        for (f in 0..3) line(184.0 + f * 10, top - 80 - 3 * k - 22, 184.0 + f * 10, top - 80 - 3 * k - 30, c.ink, 1.3)
        // jedna svislá nositelka x = 200: P shora na bednu, G z těžiště dolů, N zdola do styku
        force(200.0, top - 80 - 3 * k - 6, 200.0, top - 80, c.load, width = 2.6)
        text(210.0, top - 80 - 1.5 * k, " = 3 kN", c.load, 13.0, Anchor.START, sym = "P")
        force(200.0, top - 40, 200.0, top - 40 + 5 * k, c.load, width = 2.6)
        text(210.0, top - 18, " = 5 kN", c.load, 13.0, Anchor.START, sym = "G")
        force(200.0, top + 8 * k, 200.0, top, c.react, width = 2.6)
        text(210.0, top + 4 * k + 10, if (solved) " = 8 kN" else " = ?", c.react, 14.0, Anchor.START, sym = "N")
    }
    CardText(L(
        "Bedna (tíha [[G]] = 5 kN) leží na stole a ruka ji shora přitlačuje silou [[P]] = 3 kN. Bedna stojí.",
        "Ящик (сила тяжести [[G]] = 5 кН) лежит на столе, рука прижимает его сверху силой [[P]] = 3 кН. Ящик стоит.",
    ))
    Steps(numStep(
        question = L("Jak velkou silou [[N]] tlačí stůl bednu nahoru?", "С какой силой [[N]] стол давит на ящик вверх?"),
        fields = listOf(NumSpec(L("[[N]]", "[[N]]"), 8.0, t.pick(L("kN", "кН")))),
        explain = L(
            "Bedna stojí, takže Σ[[F_y]] = 0 (nahoru +): [[N]] − 5 − 3 = 0, tedy [[N]] = 8 kN. Síla stolu (podpory) **není automaticky rovna tíze** – je přesně taková, jakou vyžaduje rovnováha. Přesně tak budeme počítat reakce nosníků.",
            "Ящик стоит, значит Σ[[F_y]] = 0 (вверх +): [[N]] − 5 − 3 = 0, то есть [[N]] = 8 кН. Сила стола (опоры) **не обязательно равна весу** — она ровно такая, какую требует равновесие. Именно так мы будем считать реакции балок.",
        ),
        traps = listOf(
            NumTrap.near(L("To je jen tíha. Ruka ale tlačí bednu dolů taky – stůl se prohne víc a víc tlačí.", "Это только сила тяжести. Но рука тоже давит ящик вниз — стол прогнётся сильнее и будет давить сильнее."), 5.0),
            NumTrap.near(L("Ruka tlačí dolů, ne nahoru – síly dolů se sčítají.", "Рука давит вниз, а не вверх — силы вниз складываются."), 2.0),
            NumTrap.near(L("Stůl nese i tíhu bedny, nejen ruku.", "Стол держит и вес ящика, а не только руку."), 3.0),
        ),
        nudge = L("Bedna stojí: síly nahoru = síly dolů. Které síly tlačí bednu dolů?", "Ящик стоит: силы вверх = силы вниз. Какие силы давят ящик вниз?"),
    ))
}

/** Karta 3: vodorovně a svisle zvlášť – doplň chybějící sílu. */
@Composable
private fun CardScope.TwoAxes() {
    val t = tr()
    val solved = solvedHere
    Stage(h = 230.0, description = t.pick(L("Bedna se silami ve dvou směrech a jedna síla chybí", "Ящик с силами в двух направлениях, одной силы не хватает"))) { c ->
        val cx = 200.0
        val cy = 115.0
        rect(cx - 32, cy - 32, 64.0, 64.0, c.subtle, c.ink, 1.6)
        force(cx + 32, cy, cx + 32 + 4 * 18.0, cy, c.load)
        text(cx + 40, cy - 10, " = 4 kN", c.load, 13.0, Anchor.START, sym = "F_1")
        force(cx, cy - 32, cx, cy - 32 - 3 * 18.0, c.load)
        text(cx + 8, cy - 70, " = 3 kN", c.load, 13.0, Anchor.START, sym = "F_2")
        force(cx, cy + 32, cx, cy + 32 + 3 * 18.0, c.load)
        text(cx + 8, cy + 78, " = 3 kN", c.load, 13.0, Anchor.START, sym = "F_3")
        if (solved) {
            // doplněná síla 4 kN doleva
            force(cx - 32, cy, cx - 32 - 4 * 18.0, cy, c.react)
            text(cx - 40, cy - 10, " = 4 kN", c.react, 13.0, Anchor.END, sym = "F_4")
        } else {
            line(cx - 110, cy, cx - 34, cy, c.react, 1.4, dash = true)
            text(cx - 70, cy - 12, "?", c.react, 24.0, bold = true)
        }
    }
    CardText(L("Bedna má zůstat stát. Jedna síla na obrázku chybí.", "Ящик должен остаться на месте. Одной силы на рисунке не хватает."))
    Steps(quizStep(
        question = L("Jaká síla patří na místo otazníku?", "Какая сила должна стоять на месте знака вопроса?"),
        options = listOf(
            Opt(L("4 kN ←", "4 кН ←"), correct = true),
            Opt(L("4 kN →", "4 кН →"), why = L("Pak by obě vodorovné síly táhly doprava. Na kterou stranu musí chybějící síla mířit, aby se vyrušily?", "Тогда обе горизонтальные силы тянули бы вправо. В какую сторону должна быть направлена недостающая сила, чтобы они уравновесились?")),
            Opt(L("3 kN ↓", "3 кН ↓"), why = L("Svisle už je rovnováha: 3 nahoru, 3 dolů. Co ještě nesedí ve vodorovném směru?", "По вертикали уже равновесие: 3 вверх, 3 вниз. Что ещё не сходится по горизонтали?")),
            Opt(L("10 kN ←", "10 кН ←"), why = L("Svislé síly (3 a 3) se už vyrušily, ty sem nepočítej. Jak velká vodorovná síla vyrovná [[F_1]] = 4 kN?", "Вертикальные силы (3 и 3) уже уравновешены, их сюда не прибавляй. Какая горизонтальная сила уравновесит [[F_1]] = 4 кН?")),
        ),
        explain = L(
            "Vodorovně a svisle počítáme **zvlášť**. Σ[[F_y]] = 3 − 3 = 0 už platí; aby vyšlo i Σ[[F_x]] = 4 − [[F_4]] = 0, musí [[F_4]] = 4 kN mířit doleva.",
            "По горизонтали и по вертикали считаем **отдельно**. Σ[[F_y]] = 3 − 3 = 0 уже выполняется; чтобы и Σ[[F_x]] = 4 − [[F_4]] = 0, нужна [[F_4]] = 4 кН влево.",
        ),
        columns = 2,
    ))
}

/**
 * Karta 4: kvíz – výtah jede nahoru stálou rychlostí. Záblesky stejně daleko od sebe → Σ F = 0 → lano táhne
 * stejně, jako je tíha. Ověří, že „pohyb“ ≠ „převaha síly“.
 */
@Composable
private fun CardScope.CheckStill() {
    val t = tr()
    val solved = solvedHere
    // kabina jede pořád dokola nahoru stálou rychlostí; záblesky každou sekundu
    var y by remember { mutableDoubleStateOf(0.0) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.0 else (now - last) / 1e9
                last = now
                y = (y + dt * 22) % 110
            }
        }
    }
    Stage(h = 230.0, description = t.pick(L("Výtah jede stálou rychlostí nahoru, lano a tíha", "Лифт едет вверх с постоянной скоростью, трос и сила тяжести"))) { c ->
        // šachta
        line(140.0, 8.0, 140.0, 222.0, c.inkSoft, 1.2)
        line(260.0, 8.0, 260.0, 222.0, c.inkSoft, 1.2)
        // záblesky: stejně daleko od sebe
        for (k in 0..4) {
            val gy = 190.0 - k * 22
            line(126.0, gy, 138.0, gy, c.accent, 1.6)
        }
        text(132.0, 212.0, t.pick(L("záblesky po 1 s", "вспышки через 1 с")), c.accent, 11.0, Anchor.END)
        val ky = 150.0 - y
        val alpha = when {
            y < 10 -> (y / 10).toFloat()
            y > 100 -> ((110 - y) / 10).toFloat()
            else -> 1f
        }
        line(200.0, 8.0, 200.0, ky - 30, c.ink, 1.6, alpha = alpha)
        rect(165.0, ky - 30, 70.0, 60.0, c.subtle, c.ink, 1.5, alpha = alpha)
        // síly na jedné svislé nositelce: lano nahoru z horní hrany, tíha dolů z těžiště
        force(200.0, ky - 30, 200.0, ky - 30 - 40, c.react, width = 2.6, alpha = alpha)
        text(208.0, ky - 52, if (solved) " = 8 kN" else " = ?", c.react, 13.0, Anchor.START, sym = "S", alpha = alpha)
        force(200.0, ky, 200.0, ky + 40, c.load, width = 2.6, alpha = alpha)
        text(208.0, ky + 46, " = 8 kN", c.load, 13.0, Anchor.START, sym = "G", alpha = alpha)
        force(282.0, ky + 15, 282.0, ky - 15, c.accent, width = 2.0, alpha = alpha)
        text(290.0, ky + 5, t.pick(L("v = stálá", "v = const")), c.accent, 12.0, Anchor.START, alpha = alpha)
    }
    CardText(L(
        "Výtah (tíha [[G]] = 8 kN) jede nahoru **stálou rychlostí** – záblesky jsou stejně daleko od sebe.",
        "Лифт (сила тяжести [[G]] = 8 кН) едет вверх с **постоянной скоростью** — вспышки на равных расстояниях.",
    ))
    Steps(quizStep(
        question = L("Jak velkou silou [[S]] táhne lano?", "С какой силой [[S]] тянет трос?"),
        options = listOf(
            Opt(L("víc než 8 kN – jinak by nejel nahoru", "больше 8 кН — иначе он не ехал бы вверх"), why = L("Převaha síly by výtah **zrychlovala** – záblesky by se rozestupovaly. Jede ale stále stejně rychle.", "Перевес силы **разгонял** бы лифт — вспышки расходились бы. А он едет с постоянной скоростью.")),
            Opt(L("přesně 8 kN", "ровно 8 кН"), correct = true),
            Opt(L("méně než 8 kN", "меньше 8 кН"), why = L("Pak by převažovala tíha a výtah by zpomaloval.", "Тогда перевешивала бы сила тяжести, и лифт бы тормозил.")),
        ),
        explain = L(
            "Stálá rychlost = rovnováha: Σ[[F_y]] = [[S]] − 8 = 0, tedy [[S]] = 8 kN. Síla je potřeba ke **změně** rychlosti, ne k samotnému pohybu. Rovnováha neznamená „žádné síly“ ani „nic se nehýbe“ – znamená, že se síly vyruší.",
            "Постоянная скорость = равновесие: Σ[[F_y]] = [[S]] − 8 = 0, то есть [[S]] = 8 кН. Сила нужна, чтобы **менять** скорость, а не чтобы просто двигаться. Равновесие не значит «нет сил» или «ничего не движется» — значит, что силы уравновешены.",
        ),
    ))
}

internal fun lesson2() = Lesson(
    id = 2,
    title = L("Rovnováha sil", "Равновесие сил"),
    summary = L("Nevyrovnaná síla rozjíždí, vyrovnané síly nemění pohyb. Když se nic nehýbe, síly se vyruší.", "Неуравновешенная сила разгоняет, уравновешенные — не меняют движения. Если ничего не движется, силы уравновешены."),
    cards = listOf(
        Card { TugOfWar() },
        Card { Coast() },
        Card { TableForce() },
        Card { TwoAxes() },
        Card { CheckStill() },
    ),
)
