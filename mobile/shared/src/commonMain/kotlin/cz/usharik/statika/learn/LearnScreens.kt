package cz.usharik.statika.learn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.usharik.statika.platform.Analytics
import cz.usharik.statika.platform.NoAnalytics
import cz.usharik.statika.ui.CardShape
import cz.usharik.statika.ui.LocalColors
import cz.usharik.statika.ui.PrimaryButton
import cz.usharik.statika.ui.SecondaryButton
import cz.usharik.statika.ui.rich

/**
 * Výklad přes celou obrazovku nad trenažérem (trenažér zůstává pod ním i se rozpracovanou úlohou).
 * Systémové „zpět“: z lekce na seznam (nebo rovnou k úloze, když byla otevřena z ní), ze seznamu do trenažéru.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LearnRoot(nav: LearnNav, progress: LearnProgress, onNav: (LearnNav?) -> Unit, analytics: Analytics = NoAnalytics) {
    val c = LocalColors.current
    val back: () -> Unit = {
        when (nav) {
            is LearnNav.Open -> onNav(nav.returnTo ?: if (nav.fromTrainer) null else LearnNav.Home)
            is LearnNav.Practice -> onNav(nav.returnTo ?: if (nav.set != null) LearnNav.Practice() else LearnNav.Home)
            LearnNav.Glossary -> onNav(LearnNav.Home)
            LearnNav.Home -> onNav(null)
        }
    }
    // lekce otevřená odkazem přepne kurz, aby „zpět“ vedlo do správného seznamu
    LaunchedEffect(nav) {
        if (nav is LearnNav.Open) progress.course = Lessons.courseOf(nav.lesson).id
        if (nav is LearnNav.Practice) progress.course = Courses.trig.id
    }
    BackHandler(onBack = back)
    Box(Modifier.fillMaxSize().background(c.bg)) {
        CompositionLocalProvider(LocalLearnHere provides nav) {
            when (nav) {
                LearnNav.Home -> LearnHome(progress, onNav)
                LearnNav.Glossary -> Glossary(progress, back)
                is LearnNav.Open -> LessonScreen(nav, progress, onNav, back)
                is LearnNav.Practice -> PracticeScreen(nav, progress, onNav, back, analytics)
            }
        }
    }
}

@Composable
internal fun Header(title: String, sub: String?, onClose: () -> Unit) {
    val c = LocalColors.current
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = L("Zavřít", "Закрыть").t(), tint = c.ink) }
        Column(Modifier.weight(1f)) {
            Text(rich(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LearnHome(progress: LearnProgress, onNav: (LearnNav?) -> Unit) {
    val c = LocalColors.current
    @Suppress("UNUSED_VARIABLE") val v = progress.version // překreslit po změně postupu
    val course = Courses.byId(progress.course)
    Column(Modifier.fillMaxSize()) {
        Header(course.title.t(), course.sub.t()) { onNav(null) }
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // přepínač kurzů
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (k in Courses.all) {
                    val on = k.id == course.id
                    Surface(
                        onClick = { progress.course = k.id },
                        shape = RoundedCornerShape(18.dp), color = if (on) c.ink else c.paper,
                        border = BorderStroke(1.dp, if (on) c.ink else c.line),
                    ) {
                        Text(k.title.t(), color = if (on) c.paper else c.ink, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                    }
                }
            }
            val next = progress.next(course)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    (if (course.lessons.all { progress.isDone(it.id) }) L("Začít znovu", "Начать заново") else L("Pokračovat", "Продолжить")).t() + " →",
                    { onNav(LearnNav.Open(next)) },
                )
                if (course.practice.isNotEmpty()) SecondaryButton(L("Procvičování", "Практика").t(), { onNav(LearnNav.Practice()) })
                SecondaryButton(L("Slovníček", "Словарик").t(), { onNav(LearnNav.Glossary) })
            }
            for (lesson in course.lessons) {
                val done = progress.isDone(lesson.id)
                val seen = progress.seen(lesson.id)
                Surface(
                    onClick = { onNav(LearnNav.Open(lesson.id)) },
                    shape = CardShape, color = c.paper, border = BorderStroke(1.dp, if (lesson.id == next && !done) c.activeBorder else c.line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(34.dp).background(if (done) c.accent else if (seen > 0) c.focusBg else c.subtle, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (done) "✓" else "${lesson.no}", color = if (done) c.paper else c.ink, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(lesson.title.t(), fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text(rich(lesson.summary.t()), style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                        }
                        if (!done && seen > 0) Text("$seen/${lesson.cards.size}", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                    }
                }
            }
            if (course.practice.isNotEmpty()) PracticeSetList(course, progress, onNav)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LessonScreen(nav: LearnNav.Open, progress: LearnProgress, onNav: (LearnNav?) -> Unit, back: () -> Unit) {
    val c = LocalColors.current
    val lesson = Lessons.byId(nav.lesson)
    var index by remember(nav) { mutableIntStateOf(nav.card.coerceIn(0, lesson.cards.lastIndex)) }
    val last = index == lesson.cards.lastIndex
    LaunchedEffect(lesson.id, index) { progress.markSeen(lesson.id, index) }
    // odemčená je jen karta, jejíž úkoly žák opravdu splnil (ne jen „viděl“ nebo přeskočil odkazem z úlohy)
    val scope = remember(lesson.id, index) { CardScope(alreadyDone = progress.cardDone(lesson.id, index)) }
    LaunchedEffect(scope, scope.solvedHere) { if (scope.solvedHere) progress.markCardDone(lesson.id, index) }
    Column(Modifier.fillMaxSize()) {
        Header("${lesson.no} · ${lesson.title.t()}", "${index + 1} / ${lesson.cards.size}", back)
        // tečky postupu v lekci
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in lesson.cards.indices) {
                Box(Modifier.weight(1f).height(4.dp).background(if (i <= index) c.react else c.line, RoundedCornerShape(2.dp)))
            }
        }
        val sticky = remember { StickyHost() }
        Box(
            Modifier.weight(1f).fillMaxWidth().clipToBounds()
                .onGloballyPositioned { sticky.top = it.positionInRoot().y; sticky.height = it.size.height.toFloat() },
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(remember(lesson.id, index) { androidx.compose.foundation.ScrollState(0) })
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                // odkazy z karty (LearnJump) se vrátí právě sem
                CompositionLocalProvider(LocalLearnHere provides nav.copy(card = index), LocalSticky provides sticky) {
                    androidx.compose.runtime.key(lesson.id, index) { lesson.cards[index].content(scope) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        // co chybí – ukáže se, když žák zkusí jít dál dřív, než splní úkoly
        var missing by remember(lesson.id, index) { mutableStateOf<List<L>>(emptyList()) }
        if (missing.isNotEmpty() && !scope.done) {
            val now = scope.pending.ifEmpty { missing }
            val t = tr()
            Surface(color = c.nudgeBg, modifier = Modifier.fillMaxWidth()) {
                Text(
                    // krátce – podrobnosti jsou v rámečku úkolů, který se právě zvýraznil a posunul do pohledu
                    rich(
                        if (now.size == 1) L("Ještě zbývá: ", "Ещё осталось: ").t() + t.pick(now[0])
                        else L("Nejdřív splň všechny úkoly v rámečku ↑ (zbývá ${now.size})", "Сначала выполни все задания в рамке ↑ (осталось ${now.size})").t(),
                    ),
                    color = c.ink, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        // po poslední kartě lekce rovnou nabídnout procvičení (série k téhle lekci)
        val practiceSet = if (last && scope.done) cz.usharik.statika.practice.PracticeSets.afterLesson(lesson.id) else null
        if (practiceSet != null) {
            Surface(color = c.focusBg, modifier = Modifier.fillMaxWidth()) {
                androidx.compose.material3.TextButton(
                    onClick = {
                        progress.markDone(lesson.id)
                        onNav(LearnNav.Practice(practiceSet.id, returnTo = nav.returnTo))
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(L("Procvičit: ${practiceSet.title.cs} →", "Закрепить: ${practiceSet.title.ru} →").t(), color = c.react, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(c.paper).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (index > 0) SecondaryButton("← " + L("Zpět", "Назад").t(), { index-- }) else Spacer(Modifier.size(1.dp))
            val label = (if (last) L("Hotovo ✓", "Готово ✓") else L("Dál", "Дальше")).t() + if (last) "" else " →"
            if (scope.done) {
                PrimaryButton(label, {
                    if (last) {
                        progress.markDone(lesson.id)
                        onNav(nav.returnTo ?: if (nav.fromTrainer) null else LearnNav.Home)
                    } else index++
                })
            } else {
                // zamčené: vypadá neaktivně, ale klepnutí řekne, co ještě chybí, a ukáže to na kartě
                OutlinedButton(
                    onClick = {
                        missing = scope.pending.ifEmpty { listOf(L("Vyzkoušej obrázek nahoře", "Попробуй рисунок выше")) }
                        scope.nag()
                    },
                    shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, c.line),
                ) { Text("🔒 $label", color = c.inkSoft, maxLines = 1) }
            }
        }
    }
}

/** Slovníček pojmů (česky – rusky). */
@Composable
private fun Glossary(progress: LearnProgress, back: () -> Unit) {
    val c = LocalColors.current
    // nejdřív oddíl kurzu, ze kterého žák přišel
    val current = Courses.byId(progress.course)
    val sections = listOf(current) + Courses.all.filter { it != current }
    Column(Modifier.fillMaxSize()) {
        Header(L("Slovníček", "Словарик").t(), null, back)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (course in sections) {
                Text(
                    (if (course.id == Courses.statics.id) L("Statika", "Статика") else L("Trigonometrie", "Тригонометрия")).t(),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.padding(top = 8.dp),
                )
                for ((term, def) in course.glossary) {
                    Column {
                        Text(rich(term.t()), fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text(rich(def.t()), style = MaterialTheme.typography.bodyMedium, color = c.inkSoft)
                    }
                }
            }
        }
    }
}

/** Pojem → vysvětlení. */
val GLOSSARY: List<Pair<L, L>> = listOf(
    L("Síla", "Сила") to L("To, co tlačí nebo táhne. Kreslíme ji šipkou, měříme v kN (1 kN ≈ tíha tělesa o hmotnosti 100 kg).", "То, что толкает или тянет. Рисуем стрелкой, измеряем в кН (1 кН ≈ сила тяжести тела массой 100 кг)."),
    L("Tíha G", "Сила тяжести G (tíha)") to L("Síla, kterou Země táhne těleso dolů. Působí na těleso samo.", "Сила, с которой Земля притягивает тело вниз. Приложена к самому телу."),
    L("Vektor", "Вектор") to L("Veličina, u které záleží na velikosti, směru i smyslu – proto ji kreslíme šipkou.", "То, у чего важны и величина, и наклон, и направление, — поэтому рисуем стрелкой."),
    L("Působiště", "Точка приложения (působiště)") to L("Bod, kde síla na těleso působí.", "Точка, в которой сила действует на тело."),
    L("Nositelka", "Линия действия (nositelka)") to L("Přímka, na které síla leží. Po ní smíš sílu posunout – účinek na tuhé těleso se nezmění.", "Прямая, на которой лежит сила. Вдоль неё силу можно сдвигать — действие на твёрдое тело не меняется."),
    L("Velikost, směr, smysl", "Величина (velikost), наклон (směr), направление (smysl)") to L("Jak velká síla je, jaký sklon má její nositelka a kam míří hrot.", "Насколько велика сила (её модуль), какой наклон у её линии действия и куда смотрит остриё стрелки."),
    L("Složky síly [[F_x]], [[F_y]]", "Составляющие силы [[F_x]], [[F_y]] (složky)") to L("Vodorovná a svislá část šikmé síly. Spolu dělají totéž co celá síla.", "Горизонтальная и вертикальная части наклонной силы. Вместе действуют так же, как вся сила."),
    L("Výslednice", "Равнодействующая (výslednice)") to L("Jedna síla, která nahradí několik sil se stejným účinkem.", "Одна сила, заменяющая несколько сил с тем же действием."),
    L("Σ (součet)", "Σ (сумма)") to L("Σ[[F_x]] = 0 čti „součet všech vodorovných sil je nula“.", "Σ[[F_x]] = 0 читается так: «сумма всех горизонтальных сил равна нулю»."),
    L("Nosník", "Балка (nosník)") to L("Vodorovný trám, který nese zatížení a leží na podporách.", "Горизонтальный брус, который несёт нагрузку и лежит на опорах."),
    L("Moment síly M", "Момент силы M") to L("Jak moc síla otáčí: M = F · r, v kNm (kilonewtonmetr). Kladný ↺, záporný ↻.", "Насколько сила поворачивает: M = F · r, в кН·м (килоньютон-метр). Положительный ↺, отрицательный ↻."),
    L("Rameno r", "Плечо r (rameno)") to L("Kolmá vzdálenost bodu od nositelky síly – ne vzdálenost k působišti.", "Кратчайшее (по перпендикуляру) расстояние от точки до линии действия силы — не расстояние до точки приложения."),
    L("Dvojice sil", "Пара сил (dvojice sil)") to L("Dvě stejně velké rovnoběžné síly opačného smyslu na různých nositelkách – jen otáčejí.", "Две равные по модулю параллельные противоположно направленные силы, не лежащие на одной прямой, — только поворачивают."),
    L("Rovnováha", "Равновесие") to L("Těleso se neposouvá ani neotáčí: Σ[[F_x]] = 0, Σ[[F_y]] = 0, ΣM = 0.", "Тело не смещается и не поворачивается: Σ[[F_x]] = 0, Σ[[F_y]] = 0, ΣM = 0."),
    L("Podpora", "Опора (podpora)") to L("Místo, kde nosník leží nebo je uchycený – jeden druh vazby.", "Место, где балка лежит или закреплена, — один из видов связи."),
    L("Vazba", "Связь (vazba)") to L("To, co tělesu brání v pohybu (podpora, kloub, zeď). Každý pohyb, který vazba nedovolí, znamená jednu reakci.", "То, что ограничивает движение тела (опора, шарнир, стена). Каждое движение, которое связь не допускает, даёт одну реакцию."),
    L("Reakce", "Реакция (reakce)") to L("Síla (nebo moment), kterou vazba působí na těleso – proti pohybu, který nedovolí. Kreslíme ji modře.", "Сила (или момент), с которой связь действует на тело, — против движения, которое она не допускает. Рисуем синим."),
    L("Uvolnění", "Освобождение от связей (uvolnění)") to L("Vazby odstraníme a nahradíme je reakcemi. Na nosníku zůstanou jen zatížení a reakce.", "Связи убираем и заменяем их реакциями. На балке остаются только нагрузки и реакции."),
    L("Akce a reakce", "Действие и противодействие (akce a reakce)") to L("Nosník tlačí na podporu a podpora stejně velkou silou opačně na nosník. Každá z těch sil působí na jiné těleso.", "Балка давит на опору, а опора с такой же по величине силой — в обратную сторону на балку. Каждая из этих сил действует на своё тело."),
    L("Neznámá", "Неизвестная (neznámá)") to L("Reakce, jejíž velikost zatím neznáme – spočítáme ji z rovnic rovnováhy. Počet neznámých značíme n.", "Реакция, величину которой мы пока не знаем, — находим её из уравнений равновесия. Число неизвестных обозначаем n."),
    L("Podložka", "Опорная поверхность (podložka)") to L("Plocha, po které jezdí válečky posuvné podpory.", "Поверхность, по которой катятся катки шарнирно-подвижной опоры."),
    L("Pevná kloubová podpora", "Шарнирно-неподвижная опора (pevná kloubová)") to L("Nedovolí posun, dovolí natočení: dvě reakce, [[R_x]] a [[R_y]].", "Не допускает смещения, но допускает поворот: две реакции, [[R_x]] и [[R_y]]."),
    L("Posuvná kloubová podpora", "Шарнирно-подвижная опора (posuvná kloubová)") to L("Na válečcích: dovolí pohyb podél podložky i natočení, brání jen pohybu kolmo k podložce – jedna reakce.", "На катках: допускает сдвиг вдоль опорной поверхности и поворот, препятствует только смещению перпендикулярно опорной поверхности — одна реакция."),
    L("Vetknutí", "Жёсткая заделка (vetknutí)") to L("Konec nosníku pevně zapuštěný do zdi: nedovolí posun ani natočení – tři reakce, [[R_x]], [[R_y]] a moment M.", "Конец балки, неподвижно заделанный в стену: не допускает ни смещения, ни поворота — три реакции, [[R_x]], [[R_y]] и момент M."),
    L("Vnitřní kloub", "Промежуточный шарнир (vnitřní kloub)") to L("Spojuje dvě části nosníku: přenese sílu, ale ne moment. Proto [[M_K]] = 0 – jedna rovnice navíc.", "Соединяет две части балки: передаёт силу, но не момент. Поэтому [[M_K]] = 0 — одно дополнительное уравнение."),
    L("Spojité zatížení q", "Распределённая нагрузка q (spojité zatížení)") to L("Zatížení rozložené po délce, v kN/m. U rovnoměrného je výslednice Q = q · ℓ uprostřed zatížené délky.", "Нагрузка, распределённая по длине, в кН/м. У равномерной равнодействующая Q = q · ℓ приложена посередине нагруженного участка."),
    L("Staticky určitý nosník", "Статически определимая балка (staticky určitý nosník)") to L("Počet neznámých reakcí n = počet rovnic r (3 + jedna za každý vnitřní kloub) a podpory nedovolí žádný pohyb – reakce spočítáme jen z rovnic rovnováhy.", "Число неизвестных реакций n равно числу уравнений r (3 + по одному за каждый промежуточный шарнир), и опоры не допускают никакого движения — реакции находим из одних только уравнений равновесия."),
)
