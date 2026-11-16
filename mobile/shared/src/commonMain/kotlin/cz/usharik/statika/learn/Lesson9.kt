package cz.usharik.statika.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.trainer.Tone
import cz.usharik.statika.ui.Anchor
import cz.usharik.statika.ui.LocalColors
import cz.usharik.statika.ui.PrimBuilder
import cz.usharik.statika.ui.StatikaColors
import cz.usharik.statika.ui.rich
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * Lekce 9 – Vnitřní kloub a statická určitost. Kloub přenese sílu, ale ne moment → M_K = 0 z jedné části;
 * rovnic je r = 3 + počet kloubů, neznámých n. n = r je nutná podmínka – podpory ale musí taky zabránit
 * každému pohybu (tři posuvné podpory mají n = 3, a přesto nosník vodorovně ujede).
 */

private fun rot9(x: Double, y: Double, px: Double, py: Double, deg: Double): Pair<Double, Double> {
    val a = deg * PI / 180
    val dx = x - px
    val dy = y - py
    return (px + dx * cos(a) - dy * sin(a)) to (py + dx * sin(a) + dy * cos(a))
}

private fun PrimBuilder.part(x1: Double, x2: Double, y: Double, px: Double, deg: Double, c: StatikaColors, alpha: Float = 1f) {
    val pts = listOf(x1 to y - 5, x2 to y - 5, x2 to y + 5, x1 to y + 5).map { (x, yy) -> rot9(x, yy, px, y, deg) }
    poly(pts, c.beam, c.ink, 1.6, alpha)
}

private fun PrimBuilder.hinge(x: Double, y: Double, c: StatikaColors, label: String = "K", alpha: Float = 1f) {
    circle(x, y, 6.5, c.paper, c.ink, 1.8, alpha)
    // popisek nad nosníkem – dole jsou podpory a jejich písmena
    if (label.isNotEmpty()) text(x, y - 14, label, c.ink, 16.0, bold = true, alpha = alpha)
}

/** Spojitý nosník, který za podporou [xB] plynule poklesne o [d] na konci (pevný spoj bez podpory C). */
private fun PrimBuilder.droopBeam(x1: Double, x2: Double, y: Double, xB: Double, d: Double, c: StatikaColors, alpha: Float = 1f) {
    val n = 30
    val xs = (0..n).map { x1 + (x2 - x1) * it / n }
    fun off(x: Double) = if (x <= xB) 0.0 else d * ((x - xB) / (x2 - xB)).let { it * it }
    val topPts = xs.map { it to y - 5 + off(it) }
    val botPts = xs.reversed().map { it to y + 5 + off(it) }
    poly(topPts + botPts, c.beam, c.ink, 1.6, alpha)
}

// Gerberův nosník: A (0 m, pevná), B (3 m, posuvná), K (4 m, kloub), C (6 m, posuvná); 1 m = 52 jednotek
private const val X0 = 50.0
private const val MM = 52.0
private fun gx(m: Double) = X0 + m * MM

// ------------------------------------------------------------------ kroky se stavem venku (obrázek čte stav)

/** Kvíz jako krok; stav drží karta, takže obrázek může reagovat na volbu. [more] = navíc do výkladu po vyřešení. */
private fun QuizState.step9(columns: Int = 1, label: L = question, more: (@Composable () -> Unit)? = null): Step =
    Step(label, solved, result = { Feedback(); more?.invoke() }) {
        if (label != question) Text(rich(question.t()), style = MaterialTheme.typography.bodyMedium, color = LocalColors.current.ink, modifier = Modifier.padding(top = 4.dp))
        Options(columns)
        if (!solved) Feedback()
    }

/** Číselná odpověď jako krok. */
private fun NumState.step9(label: L): Step =
    Step(label, solved, result = { Feedback() }) {
        Fields()
        Buttons()
        if (!solved) Feedback()
    }

/** Tip před pokusem: volba + potvrzení. Správnost ukáže až pokus v dalším kroku. */
@Composable
private fun tip9(label: L, options: List<L>, guess: Int?, onGuess: (Int) -> Unit): Step {
    var sel by remember(label.cs) { mutableStateOf<Int?>(null) }
    return Step(label, guess != null, result = { guess?.let { SmallText(L("Tvůj tip: " + options[it].cs, "Твой прогноз: " + options[it].ru)) } }) {
        Chips(options, sel) { sel = it }
        ActionButton(L("Potvrdit tip", "Подтвердить прогноз"), enabled = sel != null) { sel?.let(onGuess) }
    }
}

/** Rovnice ve výkladu (bez velkého písma Readoutu, aby se dlouhá vešla na řádek); [dim] = ta „horší“ varianta. */
@Composable
private fun EqLine(text: String, note: L, dim: Boolean = false) {
    val c = LocalColors.current
    Text(rich(text), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (dim) c.inkSoft else c.ink, modifier = Modifier.padding(start = 30.dp, top = 4.dp))
    Text(rich(note.t()), style = MaterialTheme.typography.bodySmall, color = if (dim) c.inkSoft else c.accent, modifier = Modifier.padding(start = 30.dp))
}

/**
 * Karta 0: kloub přenese sílu, ale ne moment. Po odebrání C se pravá část s kloubem otočí kolem K čím dál
 * rychleji a skončí svisle pověšená (tlumené kyvadlo). S tuhým spojem se nosník jen pružně prohne.
 * Před každým pokusem tip.
 */
@Composable
private fun CardScope.HingeTest() {
    val t = tr()
    var rigid by remember { mutableIntStateOf(0) } // 0 = kloub, 1 = pevný spoj
    var guess by remember { mutableStateOf<Int?>(null) }
    var guess2 by remember { mutableStateOf<Int?>(null) }
    var removed by remember { mutableStateOf(false) }
    var settled by remember { mutableStateOf(false) }
    var tried by remember { mutableStateOf(setOf<Int>()) }
    // kloub: úhel pravé části (rad, dolů kladně); tuhý spoj: poměrný průhyb 0…1 (kmitá kolem 1)
    var th by remember { mutableDoubleStateOf(0.0) }
    var droop by remember { mutableDoubleStateOf(0.0) }
    val alpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(removed, rigid) {
        if (!removed) return@LaunchedEffect
        settled = false
        var w = 0.0
        var v = 0.0
        var last = 0L
        var calm = 0.0
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.0 else min((now - last) / 1e9, 0.05)
                last = now
                // malé kroky: stabilní integrace i při trhaném snímkování
                repeat(8) {
                    val h = dt / 8
                    if (rigid == 0) {
                        // tíha + F k bodu K: moment ∝ cos θ; tlumení v kloubu
                        val acc = 14.0 * cos(th) - 1.6 * w
                        w += acc * h
                        th += w * h
                    } else {
                        val acc = 90.0 * (1.0 - droop) - 4.0 * v
                        v += acc * h
                        droop += v * h
                    }
                }
                calm = if ((rigid == 0 && abs(w) < 0.05 && abs(th - PI / 2) < 0.03) || (rigid == 1 && abs(v) < 0.02 && abs(droop - 1) < 0.01)) calm + dt else 0.0
            }
            if (calm > 0.3) {
                settled = true
                tried = tried + rigid
                break
            }
        }
    }
    /** nový pokus: starý stav zmizí, nový (s podporou C) se objeví – nic se „nevrací“ samo */
    fun reset(newRigid: Int = rigid) {
        scope.launch {
            alpha.animateTo(0f, tween(250))
            removed = false
            settled = false
            rigid = newRigid
            th = 0.0
            droop = 0.0
            alpha.animateTo(1f, tween(300))
        }
    }
    // po pokusu s kloubem: nový obrázek s tuhým spojem a podporou C (pro druhý tip a pokus)
    LaunchedEffect(0 in tried) {
        if (0 in tried && 1 !in tried && rigid == 0) {
            delay(1500)
            reset(1)
        }
    }
    val y = 80.0
    Stage(h = 240.0, description = t.pick(L("Nosník s vnitřním kloubem K", "Балка с промежуточным шарниром K"))) { c ->
        val a = alpha.value
        support(SupportType.PIN, gx(0.0), y, c, "A")
        support(SupportType.ROLLER, gx(3.0), y, c, "B")
        if (!removed) support(SupportType.ROLLER, gx(6.0), y, c, "C")
        val fx: Double
        val fy: Double
        val deg = th * 180 / PI
        if (rigid == 0) {
            part(gx(0.0) - 10, gx(4.0) - 6, y, gx(4.0), 0.0, c, a)
            // pravá část se otáčí kolem K
            part(gx(4.0) + 6, gx(6.0) + 10, y, gx(4.0), deg, c, a)
            hinge(gx(4.0), y, c, alpha = a)
            val r = rot9(gx(5.0), y, gx(4.0), y, deg)
            fx = r.first
            fy = r.second
        } else {
            // pevný spoj: nosník jen pružně prohne převislý konec za B, žádný zlom v K
            val d = 14.0 * droop
            droopBeam(gx(0.0) - 10, gx(6.0) + 10, y, gx(3.0), d, c, a)
            text(gx(5.0) - 12, y - 14, t.pick(L("tuhý spoj", "жёсткое соединение")), c.inkSoft, 13.0, Anchor.END, alpha = a)
            if (removed) text(gx(5.0), y + 46, t.pick(L("průhyb zvětšen", "прогиб увеличен")), c.inkSoft, 11.0, alpha = a)
            fx = gx(5.0)
            fy = y + d * ((gx(5.0) - gx(3.0)) / (gx(6.0) + 10 - gx(3.0))).let { it * it }
        }
        // svislá síla F působí stále svisle; s natočením části se mění jen působiště
        force(fx, fy - 5 - 50, fx, fy - 5, c.load, alpha = a)
        text(fx + 12, fy - 44, null, c.load, sym = "F", alpha = a)
        if (rigid == 0 && removed && th > 0.3) {
            text(gx(4.0) + 70, y + 120, t.pick(L("visí na kloubu", "висит на шарнире")), c.inkSoft, 12.0, alpha = if (settled) a else 0f)
        }
    }
    CardText(L(
        "**Vnitřní kloub** K spojuje dvě části nosníku jako pant dveří: části se v něm mohou vůči sobě volně natočit.",
        "**Промежуточный шарнир** K (vnitřní kloub) соединяет две части балки, как дверная петля: части могут свободно поворачиваться в нём друг относительно друга.",
    ))
    val removeC: @Composable (Int) -> Unit = { want ->
        ActionButton(L("Odeber podporu C", "Убрать опору C"), enabled = !removed && rigid == want && !alpha.isRunning) { removed = true }
        if (removed && !settled) SmallText(L("Sleduj pravou část…", "Следи за правой частью…"))
    }
    Steps(
        tip9(
            L("Odebereme podporu C. Co udělá pravá část za kloubem?", "Уберём опору C. Что сделает правая часть за шарниром?"),
            listOf(L("zůstane vodorovně", "останется горизонтально"), L("trochu se prohne", "немного прогнётся"), L("otočí se v K a spadne", "повернётся в K и упадёт")),
            guess,
        ) { guess = it },
        Step(
            L("Odeber podporu C a počkej, až se pravá část uklidní", "Убери опору C и подожди, пока правая часть успокоится"), 0 in tried,
            result = {
                Note(if (guess == 2) Tone.OK else Tone.INFO, L(
                    (if (guess == 2) "Tip sedí. " else "") + "Pravá část se v kloubu otáčí **čím dál rychleji** – moment síly F a tíhy k bodu K nemá nic, co by ho vyrovnalo. Uklidní se, až visí svisle: pak síly míří přímo do K a jejich rameno je 0. Kloub ji udrží (přenese **sílu**), ale otáčení nezabrání (**moment** nepřenese).",
                    (if (guess == 2) "Прогноз верен. " else "") + "Правая часть поворачивается в шарнире **всё быстрее** — моменту силы F и веса относительно K нечем противостоять. Успокаивается, только когда висит вертикально: тогда силы направлены прямо в K и плечо у них 0. Шарнир её удерживает (передаёт **силу**), но повороту не мешает (**момент** не передаёт).",
                ))
            },
        ) { removeC(0) },
        tip9(
            L("Kloub vyměníme za tuhý spoj. Co se stane bez podpory C teď?", "Заменим шарнир жёстким соединением. Что будет без опоры C теперь?"),
            listOf(L("spadne stejně", "упадёт так же"), L("jen se trochu prohne", "лишь немного прогнётся"), L("nestane se vůbec nic", "ничего не произойдёт")),
            guess2,
        ) { guess2 = it },
        Step(
            L("Odeber podporu C u tuhého spoje a porovnej", "Убери опору C при жёстком соединении и сравни"), 1 in tried,
            result = {
                Note(if (guess2 == 1) Tone.OK else Tone.INFO, L(
                    (if (guess2 == 1) "Tip sedí. " else "") + "Tuhý spoj přenese i moment – nosník drží, převislý konec se jen pružně prohne (na obrázku zvětšeně) a ustálí se.",
                    (if (guess2 == 1) "Прогноз верен. " else "") + "Жёсткое соединение передаёт и момент — балка держится, консоль лишь упруго прогибается (на рисунке увеличено) и успокаивается.",
                ))
                SmallText(L(
                    "Kloub moment nepřenese. Proto u stojícího nosníku platí [[M_K]] = 0: součet momentů všech sil na **jedné** části nosníku k bodu K je nula.",
                    "Шарнир не передаёт момент. Поэтому у неподвижной балки [[M_K]] = 0: сумма моментов всех сил, действующих на **одну** часть балки, относительно точки K равна нулю.",
                ))
            },
        ) { removeC(1) },
        after = {
            Chips(listOf(L("kloub", "шарнир"), L("tuhý spoj", "жёсткое соединение")), rigid) { if (it != rigid) reset(it) }
            if (!removed) ActionButton(L("Odeber podporu C", "Убрать опору C"), enabled = !alpha.isRunning) { removed = true }
            else ActionButton(L("Nový pokus (s podporou C)", "Новая попытка (с опорой C)"), enabled = settled) { reset() }
        },
    )
}

/**
 * Karta 1: M_K = 0 jen z jedné části. Části jsou nakreslené odděleně se silami v kloubu (K_x, K_y stejně velké,
 * opačné). Žák vybere část, ze které vyjde rovnice s jednou neznámou, a spočítá R_C.
 */
@Composable
private fun CardScope.OnePart() {
    val t = tr()
    val left = "[[M_K]] = −[[R_Ay]]·4 − [[R_B]]·1 + [[F_1]]·2,5 = 0"
    val right = "[[M_K]] = [[R_C]]·2 − 6·1 = 0"
    val q1 = rememberQuiz(
        L("Ze které části vyjde rovnice s jedinou neznámou?", "Из какой части получится уравнение с одной неизвестной?"),
        listOf(
            Opt(
                L("z levé části", "из левой части"),
                why = L(
                    "Levá část: $left. Zůstaly by dvě neznámé – [[R_Ay]] i [[R_B]] mají k bodu K nenulové rameno.",
                    "Левая часть: $left. Остались бы две неизвестные — у [[R_Ay]] и [[R_B]] плечо относительно K не нулевое.",
                ),
            ),
            Opt(L("z pravé části", "из правой части"), correct = true),
        ),
        explain = L(
            "Na pravé části je jediná neznámá reakce [[R_C]] – sílu [[F_2]] = 6 kN známe.",
            "На правой части одна неизвестная реакция [[R_C]] — силу [[F_2]] = 6 кН мы знаем.",
        ),
    )
    val nC = rememberNum(
        L("R_C z pravé části", "R_C из правой части"),
        listOf(NumSpec(L("[[R_C]]", "[[R_C]]"), 3.0, t.pick(L("kN", "кН")))),
        explain = L(
            "[[R_C]]·2 = 6·1 → [[R_C]] = 3 kN. Tahle rovnice je **navíc** ke třem rovnicím pro celý nosník – díky ní spočítáš všechny čtyři reakce.",
            "[[R_C]]·2 = 6·1 → [[R_C]] = 3 кН. Это уравнение — **дополнительное** к трём уравнениям для всей балки — благодаря нему находятся все четыре реакции.",
        ),
        traps = listOf(
            NumTrap.near(L("[[F_2]] má rameno 1 m, [[R_C]] ale 2 m. Momenty se musí rovnat, ne síly.", "У [[F_2]] плечо 1 м, а у [[R_C]] — 2 м. Равны должны быть моменты, а не силы."), 6.0),
            NumTrap.near(L("Ramena jsi prohodil: [[R_C]] je od K 2 m daleko, [[F_2]] jen 1 m.", "Плечи перепутаны: [[R_C]] находится в 2 м от K, а [[F_2]] — в 1 м."), 12.0),
        ),
    )
    val side = if (q1.solved) 1 else q1.picked
    // vlastní rozvržení: části od sebe, ať jsou vidět síly v kloubu
    val y = 100.0
    val s = 44.0
    val xa = 46.0
    val gap = 56.0
    fun lx(m: Double) = xa + m * s // levá část 0…4 m
    fun rx(m: Double) = xa + m * s + gap // pravá část 4…6 m
    Stage(h = 240.0, description = t.pick(L("Části nosníku s kloubem odděleně, síly v kloubu a momenty k bodu K", "Части балки с шарниром по отдельности, силы в шарнире и моменты относительно K"))) { c ->
        val la = if (side == 1) 0.22f else 1f
        val ra = if (side == 0) 0.22f else 1f
        val kl = lx(4.0)
        val kr = rx(4.0)
        // levá část
        part(lx(0.0) - 10, kl, y, 0.0, 0.0, c, la)
        hinge(kl, y, c, "", la)
        force(4.0, y, lx(0.0) - 12, y, c.react, alpha = la)
        text(18.0, y - 10, null, c.react, 13.0, sym = "R_Ax", alpha = la)
        force(lx(0.0), y + 56, lx(0.0), y + 6, c.react, alpha = la)
        text(lx(0.0) + 10, y + 50, null, c.react, 13.0, Anchor.START, sym = "R_Ay", alpha = la)
        force(lx(3.0), y + 56, lx(3.0), y + 6, c.react, alpha = la)
        text(lx(3.0) + 10, y + 50, null, c.react, 13.0, Anchor.START, sym = "R_B", alpha = la)
        force(lx(1.5), y - 58, lx(1.5), y - 5, c.load, alpha = la)
        text(lx(1.5) + 8, y - 46, null, c.load, 13.0, Anchor.START, sym = "F_1", alpha = la)
        // pravá část
        part(kr, rx(6.0) + 8, y, 0.0, 0.0, c, ra)
        hinge(kr, y, c, "", ra)
        force(rx(5.0), y - 58, rx(5.0), y - 5, c.load, alpha = ra)
        text(rx(5.0) + 8, y - 46, " = 6 kN", c.load, 13.0, Anchor.START, sym = "F_2", alpha = ra)
        force(rx(6.0), y + 56, rx(6.0), y + 6, c.react, alpha = ra)
        text(rx(6.0) - 8, y + 50, if (nC.solved) " = 3 kN" else null, c.react, 13.0, Anchor.END, sym = "R_C", alpha = ra)
        // síly v kloubu: na každou část stejně velké a opačné
        val mid = (kl + kr) / 2
        force(mid, y, kl + 8, y, c.derived, 2.0, 9.0, alpha = la)
        force(kl, y - 40, kl, y - 8, c.derived, 2.0, 9.0, alpha = la)
        text(kl - 6, y - 30, null, c.derived, 12.0, Anchor.END, sym = "K_y", alpha = la)
        force(mid, y, kr - 8, y, c.derived, 2.0, 9.0, alpha = ra)
        force(kr, y + 40, kr, y + 8, c.derived, 2.0, 9.0, alpha = ra)
        text(kr + 6, y + 34, null, c.derived, 12.0, Anchor.START, sym = "K_y", alpha = ra)
        text(mid, y + 18, null, c.derived, 12.0, sym = "K_x")
        text(mid, y - 22, "K", c.ink, 15.0, bold = true)
        // body K (momentový bod) na obou částech
        circle(kl, y, 11.0, c.accent.copy(alpha = 0.12f), c.accent, 1.3, alpha = la)
        circle(kr, y, 11.0, c.accent.copy(alpha = 0.12f), c.accent, 1.3, alpha = ra)
        // kóty v metrech
        val yd = y + 84
        val lm = listOf(0.0, 1.5, 3.0, 4.0)
        line(lx(0.0), yd, lx(4.0), yd, c.ink, 0.9, alpha = la)
        for (mk in lm) line(lx(mk) - 4, yd + 4, lx(mk) + 4, yd - 4, c.ink, 1.2, alpha = la)
        for (i in 0 until lm.size - 1) text((lx(lm[i]) + lx(lm[i + 1])) / 2, yd - 5, num(lm[i + 1] - lm[i]), c.ink, 12.0, alpha = la)
        val rm = listOf(4.0, 5.0, 6.0)
        line(rx(4.0), yd, rx(6.0), yd, c.ink, 0.9, alpha = ra)
        for (mk in rm) line(rx(mk) - 4, yd + 4, rx(mk) + 4, yd - 4, c.ink, 1.2, alpha = ra)
        for (i in 0 until rm.size - 1) text((rx(rm[i]) + rx(rm[i + 1])) / 2, yd - 5, num(rm[i + 1] - rm[i]), c.ink, 12.0, alpha = ra)
        text(rx(6.0) + 6, yd + 4, "m", c.inkSoft, 11.0, Anchor.START)
    }
    CardText(L(
        "Rovnice kloubu [[M_K]] = 0: sečti momenty k bodu K, ale jen od sil na **jedné** části nosníku. Části jsou nakreslené odděleně; v kloubu na sebe působí silami [[K_x]], [[K_y]] (stejně velkými, opačnými).",
        "Уравнение шарнира [[M_K]] = 0: сложи моменты относительно K, но только от сил на **одной** части балки. Части нарисованы отдельно; в шарнире они действуют друг на друга силами [[K_x]], [[K_y]] (равными и противоположными).",
    ))
    Steps(
        q1.step9(columns = 2) {
            EqLine(left, L("levá část: 2 neznámé", "левая часть: 2 неизвестные"), dim = true)
            EqLine(right, L("pravá část: 1 neznámá", "правая часть: 1 неизвестная"))
            SmallText(L(
                "Momenty ↺ bereme kladně. Síly v kloubu [[K_x]], [[K_y]] **působí**, jen k bodu K mají rameno 0 – proto v rovnici nejsou. Stejně tak [[R_Ax]] v ose nosníku.",
                "Моменты ↺ считаем положительными. Силы в шарнире [[K_x]], [[K_y]] **действуют**, просто плечо у них относительно K равно 0 — поэтому в уравнении их нет. Так же и [[R_Ax]] на оси балки.",
            ))
        },
        nC.step9(L("Pravá část: [[R_C]] · 2 − 6 · 1 = 0. Kolik je [[R_C]]?", "Правая часть: [[R_C]] · 2 − 6 · 1 = 0. Чему равна [[R_C]]?")),
    )
}

/** Karta 2: n neznámých vs r rovnic – žák spočítá neznámé ze značek podpor, pak uvidí uvolnění a posoudí, jestli nosník drží. */
@Composable
private fun CardScope.CountUnknowns() {
    val t = tr()
    var released by remember { mutableStateOf(setOf<Int>()) }
    var demoDone by remember { mutableStateOf(false) }
    val qN = rememberQuiz(
        L("Kolik neznámých reakcí n dají podpory A, B a C dohromady?", "Сколько неизвестных реакций n дают опоры A, B и C вместе?"),
        listOf(
            Opt(L("n = 3", "n = 3"), why = L("Pevná kloubová podpora A má dvě reakce, ne jednu.", "У шарнирно-неподвижной опоры A две реакции, а не одна.")),
            Opt(L("n = 4", "n = 4"), correct = true),
            Opt(L("n = 5", "n = 5"), why = L("Posuvné podpory B a C mají každá jen jednu reakci.", "У подвижных опор B и C по одной реакции.")),
        ),
        explain = L("A: 2 ([[R_Ax]], [[R_Ay]]), B: 1, C: 1 → n = 4 = r.", "A: 2 ([[R_Ax]], [[R_Ay]]), B: 1, C: 1 → n = 4 = r."),
    )
    val qS = rememberQuiz(
        L("Počet sedí. Může se přesto některá část nosníku pohnout?", "Число сходится. Может ли всё же какая-то часть балки сдвинуться?"),
        listOf(
            Opt(L("ne – každému pohybu něco brání", "нет — каждому движению что-то мешает"), correct = true),
            Opt(
                L("ano – celý nosník ujede vodorovně", "да — вся балка уедет по горизонтали"),
                why = L("Pevná kloubová podpora A má i vodorovnou reakci [[R_Ax]] – posunu brání.", "У шарнирно-неподвижной опоры A есть и горизонтальная реакция [[R_Ax]] — она мешает сдвигу."),
            ),
            Opt(
                L("ano – pravá část se otočí kolem K", "да — правая часть повернётся вокруг K"),
                why = L("Pravou část drží podpora C – bez ní by se otočila (vzpomeň si na první kartu).", "Правую часть держит опора C — без неё она бы повернулась (вспомни первую карточку)."),
            ),
        ),
        explain = L(
            "Ano, drží: A brání posunu vodorovně i svisle, B brání otočení levé části kolem A a C otočení pravé části kolem K. Neznámých je stejně jako rovnic **a** podpory nedovolí žádný pohyb – nosník je **staticky určitý**.",
            "Да, держится: A не даёт сдвига ни по горизонтали, ни по вертикали, B мешает повороту левой части вокруг A, а C — повороту правой части вокруг K. Неизвестных столько же, сколько уравнений, **и** опоры не допускают никакого движения — балка **статически определима**.",
        ),
    )
    // po správném počtu: podpory se postupně promění v šipky
    LaunchedEffect(qN.solved) {
        if (!qN.solved) return@LaunchedEffect
        for (i in 0..2) {
            delay(800)
            released = released + i
        }
        delay(900)
        demoDone = true
    }
    val y = 105.0
    Stage(h = 210.0, description = t.pick(L("Nosník s kloubem: počítání neznámých reakcí", "Балка с шарниром: подсчёт неизвестных реакций"))) { c ->
        support(SupportType.PIN, gx(0.0), y, c, "A", ghost = 0 in released)
        support(SupportType.ROLLER, gx(3.0), y, c, "B", ghost = 1 in released)
        support(SupportType.ROLLER, gx(6.0), y, c, "C", ghost = 2 in released)
        part(gx(0.0) - 10, gx(4.0) - 6, y, 0.0, 0.0, c)
        part(gx(4.0) + 6, gx(6.0) + 10, y, 0.0, 0.0, c)
        hinge(gx(4.0), y, c)
        if (0 in released) {
            force(gx(0.0) - 46, y, gx(0.0) - 12, y, c.react)
            text(gx(0.0) - 30, y - 10, null, c.react, 14.0, sym = "R_Ax")
            force(gx(0.0), y + 60, gx(0.0), y + 6, c.react)
            text(gx(0.0) + 12, y + 52, null, c.react, 14.0, Anchor.START, sym = "R_Ay")
        }
        if (1 in released) {
            force(gx(3.0), y + 60, gx(3.0), y + 6, c.react)
            text(gx(3.0) + 12, y + 52, null, c.react, 14.0, Anchor.START, sym = "R_B")
        }
        if (2 in released) {
            force(gx(6.0), y + 60, gx(6.0), y + 6, c.react)
            text(gx(6.0) - 8, y + 52, null, c.react, 14.0, Anchor.END, sym = "R_C")
        }
        if (released.size == 3) text(200.0, y + 92, "n = 4 = r", c.accent, 15.0, bold = true)
    }
    CardText(L(
        "Rovnic máme r = 4: tři pro celý nosník a jednu za kloub. Počet neznámých reakcí označíme n.",
        "Уравнений у нас r = 4: три для всей балки и одно за шарнир. Число неизвестных реакций обозначим n.",
    ))
    Steps(
        qN.step9(columns = 3),
        Step(L("Sleduj: uvolníme A, B a C – každá modrá šipka je jedna neznámá", "Смотри: освобождаем A, B и C — каждая синяя стрелка — одна неизвестная"), demoDone) {
            SmallText(L("Sleduj obrázek…", "Смотри на рисунок…"))
        },
        qS.step9 {
            SmallText(L(
                "Kdyby C byla pevná kloubová podpora (2 reakce), bylo by n = 5 > r = 4. Nosník by byl staticky neurčitý – rovnice rovnováhy by nestačily.",
                "Если бы C была шарнирно-неподвижной опорой (2 реакции), было бы n = 5 > r = 4. Балка стала бы статически неопределимой — уравнений равновесия не хватило бы.",
            ))
        },
    )
}

/**
 * Karta 3: kolik je nezávislých rovnic. Žák sestaví soustavu: rovnice celku a momentové rovnice částí k K.
 * Levá + pravá část = celý nosník, takže všechny tři momentové rovnice nezávislé nejsou – stačí kterékoli dvě.
 */
@Composable
private fun CardScope.HowMany() {
    val t = tr()
    var pick by remember { mutableStateOf(setOf<Int>()) }
    var checked by remember { mutableStateOf<Set<Int>?>(null) }
    var ok by remember { mutableStateOf(false) }
    val qSide = rememberQuiz(
        L("Ze které části vezmeš [[M_K]] = 0, aby v ní byla jen [[R_B]]?", "Из какой части возьмёшь [[M_K]] = 0, чтобы в нём была только [[R_B]]?"),
        listOf(
            Opt(L("z levé", "из левой"), why = L("Na levé části je vetknutí A se třemi neznámými.", "На левой части — заделка A с тремя неизвестными.")),
            Opt(L("z pravé", "из правой"), correct = true),
        ),
        explain = L(
            "Na pravé části je jen [[R_B]] (síly v kloubu mají k K rameno 0). Obvykle se bere [[M_K]] = 0 z té části, kde zůstane nejméně neznámých.",
            "На правой части только [[R_B]] (у сил в шарнире плечо относительно K равно 0). Обычно [[M_K]] = 0 берут для той части, где остаётся меньше всего неизвестных.",
        ),
    )
    Stage(h = 170.0, description = L("Nosník s jedním vnitřním kloubem", "Балка с одним промежуточным шарниром").t()) { c ->
        val y = 80.0
        support(SupportType.FIXED, gx(0.0), y, c, "A", wallLeft = true)
        support(SupportType.ROLLER, gx(6.0), y, c, "B")
        part(gx(0.0), gx(3.0) - 6, y, 0.0, 0.0, c)
        part(gx(3.0) + 6, gx(6.0) + 10, y, 0.0, 0.0, c)
        hinge(gx(3.0), y, c)
        force(gx(1.5), y - 54, gx(1.5), y - 5, c.load)
        force(gx(4.5), y - 54, gx(4.5), y - 5, c.load)
        text(gx(6.0) - 10, y + 62, t.pick(L("n = 3 + 1 = 4 neznámé", "n = 3 + 1 = 4 неизвестных")), c.react, 13.0, Anchor.END)
    }
    CardText(L(
        "Vetknutí A má 3 neznámé reakce, posuvná podpora B jednu – celkem **4**. K dispozici jsou rovnice celého nosníku (Σ[[F_x]], Σ[[F_y]], Σ[[M_A]]) a rovnice kloubu [[M_K]] = 0 pro levou i pravou část.",
        "У заделки A 3 неизвестные реакции, у подвижной опоры B одна — всего **4**. В распоряжении уравнения всей балки (Σ[[F_x]], Σ[[F_y]], Σ[[M_A]]) и уравнение шарнира [[M_K]] = 0 для левой и для правой части.",
    ))
    val opts = listOf(
        L("Σ[[F_x]] = 0", "Σ[[F_x]] = 0"),
        L("Σ[[F_y]] = 0", "Σ[[F_y]] = 0"),
        L("Σ[[M_A]] = 0", "Σ[[M_A]] = 0"),
        L("[[M_K]] = 0 levá", "[[M_K]] = 0 левая"),
        L("[[M_K]] = 0 pravá", "[[M_K]] = 0 правая"),
    )
    Steps(
        Step(
            L("Vyber rovnice, ze kterých spočítáš všechny 4 neznámé", "Выбери уравнения, из которых найдёшь все 4 неизвестные"), ok,
            result = {
                Note(Tone.OK, L(
                    "r = 4 nezávislé rovnice na 4 neznámé: obě silové rovnice celku a **dvě** momentové. Třetí momentová by byla jen součtem ostatních (levá + pravá část = celek).",
                    "r = 4 независимых уравнения на 4 неизвестные: оба уравнения сил для всей балки и **два** уравнения моментов. Третье уравнение моментов было бы лишь суммой остальных (левая + правая часть = вся балка).",
                ))
            },
        ) {
            Chips(opts, selectedSet = pick) {
                if (ok) return@Chips
                pick = if (it in pick) pick - it else pick + it
                checked = null
            }
            ActionButton(L("Zkontrolovat (vybráno ${pick.size})", "Проверить (выбрано ${pick.size})"), enabled = pick.isNotEmpty()) {
                checked = pick
                val moments = pick.count { it >= 2 }
                ok = pick.size == 4 && 0 in pick && 1 in pick && moments == 2
            }
            val ch = checked
            if (ch != null && !ok) {
                val moments = ch.count { it >= 2 }
                Note(Tone.NUDGE, when {
                    moments == 3 -> L(
                        "Levá + pravá část = celý nosník. Momenty obou částí k K dohromady dají momentovou rovnici celku – jedna ze tří momentových rovnic je jen součtem ostatních. Nezávislé jsou jen **dvě**.",
                        "Левая + правая часть = вся балка. Моменты обеих частей относительно K вместе дают уравнение моментов всей балки — одно из трёх лишь сумма остальных. Независимых только **два**.",
                    )
                    0 !in ch || 1 !in ch -> L(
                        "Bez silových rovnic celku Σ[[F_x]] a Σ[[F_y]] nespočítáš vodorovnou ani svislou reakci vetknutí – momentové rovnice je nenahradí.",
                        "Без уравнений сил для всей балки Σ[[F_x]] и Σ[[F_y]] не найти горизонтальную и вертикальную реакции заделки — уравнения моментов их не заменят.",
                    )
                    else -> L(
                        "Neznámé jsou 4, rovnic potřebuješ taky 4. Přidej momentovou rovnici – k bodu A nebo z kloubu.",
                        "Неизвестных 4, значит, и уравнений нужно 4. Добавь уравнение моментов — относительно A или от шарнира.",
                    )
                })
            }
        },
        qSide.step9(columns = 2),
    )
}

/**
 * Karta 4: n = r nestačí. Nosník na pevné + posuvné podpoře a nosník na třech posuvných podporách mají oba
 * 3 neznámé. Vodorovný náraz: horní drží (R_Ax), dolní se rozjede a bez tření ujede – nic mu v tom nebrání.
 */
@Composable
private fun CardScope.EnoughButHolds() {
    val t = tr()
    var guess by remember { mutableStateOf<Int?>(null) }
    var pushed by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var x by remember { mutableDoubleStateOf(0.0) } // posun dolního nosníku (jednotky obrázku)
    var push by remember { mutableStateOf(false) } // právě působí síla H
    val alpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val qWhy = rememberQuiz(
        L("Co dolnímu nosníku chybí?", "Чего не хватает нижней балке?"),
        listOf(
            Opt(L("vodorovná reakce – žádná podpora nebrání posunu", "горизонтальная реакция — ни одна опора не мешает сдвигу"), correct = true),
            Opt(
                L("další svislá podpora", "ещё одна вертикальная опора"),
                why = L("Čtvrtá posuvná podpora by přidala zase jen svislou reakci. Vodorovně by nosník ujel stejně.", "Четвёртая подвижная опора добавила бы опять только вертикальную реакцию. По горизонтали балка уехала бы так же."),
            ),
            Opt(
                L("nic – jen je moc lehký", "ничего — она просто слишком лёгкая"),
                why = L("Těžší nosník by se rozjížděl pomaleji, ale rozjel by se taky – žádná síla mu nebrání.", "Более тяжёлая балка разгонялась бы медленнее, но всё равно поехала бы — ей ничто не мешает."),
            ),
        ),
        explain = L(
            "n = r je **nutná** podmínka, ne postačující. Podpory musí navíc zabránit každému pohybu: posunu vodorovně, svisle i otočení. Tři posuvné podpory mají tři neznámé, a přesto je nosník pohyblivý – takové uložení je **výjimkové**.",
            "n = r — условие **необходимое**, но не достаточное. Опоры должны ещё не допускать никакого движения: ни горизонтального, ни вертикального сдвига, ни поворота. У трёх подвижных опор три неизвестные, и всё же балка подвижна — такое закрепление **геометрически изменяемо**.",
        ),
    )
    fun run() {
        if (running) return
        running = true
        scope.launch {
            x = 0.0
            alpha.snapTo(1f)
            var time = 0.0
            var last = 0L
            val tp = 0.6 // náraz trvá 0,6 s
            val a = 120.0 // jednotek/s² během nárazu
            push = true
            while (x < 90) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0.0 else min((now - last) / 1e9, 0.05)
                    last = now
                    time += dt
                    x = if (time <= tp) 0.5 * a * time * time else 0.5 * a * tp * tp + a * tp * (time - tp)
                    push = time <= tp
                }
            }
            push = false
            // dolní nosník ujel – zmizí a objeví se nový na místě
            alpha.animateTo(0f, tween(350))
            pushed = true
            x = 0.0
            alpha.animateTo(1f, tween(300))
            running = false
        }
    }
    val y1 = 70.0
    val y2 = 170.0
    Stage(h = 235.0, description = t.pick(L("Dva nosníky se třemi neznámými reakcemi a vodorovná síla", "Две балки с тремя неизвестными реакциями и горизонтальная сила"))) { c ->
        // horní: pevná + posuvná
        support(SupportType.PIN, 70.0, y1, c, "A")
        support(SupportType.ROLLER, 330.0, y1, c, "B")
        beam(60.0, 340.0, y1, c)
        text(200.0, y1 - 22, t.pick(L("n = 3 (A: 2, B: 1)", "n = 3 (A: 2, B: 1)")), c.inkSoft, 12.0)
        if (push) {
            force(340.0, y1, 384.0, y1, c.load, 2.6)
            text(384.0, y1 - 10, null, c.load, 14.0, Anchor.END, sym = "H")
            // pevná podpora sílu zachytí
            force(64.0, y1, 22.0, y1, c.react, 2.4)
            text(22.0, y1 - 10, null, c.react, 13.0, Anchor.START, sym = "R_Ax")
        }
        // dolní: tři posuvné podpory
        for ((i, sx) in listOf(70.0, 200.0, 330.0).withIndex()) support(SupportType.ROLLER, sx, y2, c, "${i + 1}")
        val a = alpha.value
        beam(60.0 + x, 340.0 + x, y2, c, a)
        text(200.0, y2 - 22, "n = 3 (1 + 1 + 1)", c.inkSoft, 12.0, alpha = a)
        if (push) {
            force(340.0 + x, y2, 384.0 + x, y2, c.load, 2.6, alpha = a)
            text(min(384.0 + x, 396.0), y2 - 10, null, c.load, 14.0, Anchor.END, sym = "H", alpha = a)
        }
        if (x > 2) text(200.0, y2 + 52, t.pick(L("posuvné podpory vodorovně nedrží → zrychluje", "подвижные опоры не держат по горизонтали → разгоняется")), c.derived, 12.0)
    }
    CardText(L(
        "Oba nosníky mají **3** neznámé reakce – stejně jako jsou 3 rovnice rovnováhy: n = r. Do obou krátce strčíme vodorovnou silou H.",
        "У обеих балок **3** неизвестные реакции — столько же, сколько уравнений равновесия: n = r. Обе коротко толкнём горизонтальной силой H.",
    ))
    Steps(
        tip9(
            L("Tipni si: který nosník sílu H udrží?", "Как думаешь: какая балка удержит силу H?"),
            listOf(L("oba – oba mají n = r", "обе — у обеих n = r"), L("jen horní", "только верхняя"), L("jen dolní", "только нижняя"), L("žádný", "ни одна")),
            guess,
        ) { guess = it },
        Step(
            L("Strč do obou nosníků vodorovnou silou H", "Толкни обе балки горизонтальной силой H"), pushed,
            result = {
                Note(if (guess == 1) Tone.OK else Tone.INFO, L(
                    (if (guess == 1) "Tip sedí. " else "") + "Horní nosník drží – pevná podpora A má vodorovnou reakci. Dolní se rozjel: posuvné podpory umějí tlačit jen svisle, takže vodorovné síle nic nebrání, a bez tření jede dál.",
                    (if (guess == 1) "Прогноз верен. " else "") + "Верхняя балка держится — у неподвижной опоры A есть горизонтальная реакция. Нижняя разогналась: подвижные опоры давят только вертикально, горизонтальной силе ничто не мешает, и без трения балка едет дальше.",
                ))
            },
        ) {
            ActionButton(L("Strč silou H", "Толкнуть силой H"), enabled = !running) { run() }
        },
        qWhy.step9(),
    )
}

internal fun lesson9() = Lesson(
    id = 9,
    title = L("Kloub a statická určitost", "Шарнир и статическая определимость"),
    summary = L(
        "[[M_K]] = 0 z jedné části; n = r – a podpory musí bránit každému pohybu.",
        "[[M_K]] = 0 по одной части; n = r — и опоры должны мешать любому движению.",
    ),
    cards = listOf(
        Card { HingeTest() },
        Card { OnePart() },
        Card { CountUnknowns() },
        Card { HowMany() },
        Card { EnoughButHolds() },
    ),
)
