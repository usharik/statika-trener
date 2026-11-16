package cz.usharik.statika.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cz.usharik.statika.trainer.Favorite
import cz.usharik.statika.trainer.FavoritesRepository
import cz.usharik.statika.trainer.decodeOpts
import cz.usharik.statika.trainer.encode
import kotlinx.coroutines.launch
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.usharik.statika.i18n.Lang
import cz.usharik.statika.learn.LearnNav
import cz.usharik.statika.learn.LearnOpener
import cz.usharik.statika.learn.LearnProgress
import cz.usharik.statika.learn.LearnRoot
import cz.usharik.statika.learn.LocalLearn
import cz.usharik.statika.i18n.LocalTr
import cz.usharik.statika.i18n.Tr
import cz.usharik.statika.model.BeamType
import cz.usharik.statika.model.GenOptions
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.generate
import cz.usharik.statika.model.randomSeed
import cz.usharik.statika.platform.AppGraph
import cz.usharik.statika.platform.LocalAppGraph
import cz.usharik.statika.platform.PRIVACY_URL
import cz.usharik.statika.platform.WEB_URL
import cz.usharik.statika.trainer.STEP_DONE
import cz.usharik.statika.trainer.STEP_EQUATIONS
import cz.usharik.statika.trainer.STEP_LOADS
import cz.usharik.statika.trainer.STEP_RELEASE
import cz.usharik.statika.trainer.STEP_SOLVE
import cz.usharik.statika.trainer.SessionEvents
import cz.usharik.statika.trainer.SettingsRepository
import cz.usharik.statika.trainer.ThemeMode
import cz.usharik.statika.trainer.TrainerSession

/** Po kolika vyřešených úlohách nejdřív ukázat mezistránkovou reklamu. */
private const val AD_EVERY = 3

/** Úloha pro číslo a nastavení; kdyby generátor selhal, zkusí se jiné číslo. */
private fun problemFor(seed: Int, level: Int, types: List<BeamType>): Problem {
    var s = seed
    repeat(20) {
        runCatching { return generate(s, GenOptions(level, types)) }
        s = randomSeed()
    }
    return generate(1, GenOptions(1, listOf(BeamType.SIMPLE)))
}

@Composable
fun StatikaApp(
    graph: AppGraph, systemDark: Boolean, systemLang: String?,
    tour: TourState = remember { TourState(graph.store) },
    onDarkTheme: (Boolean) -> Unit = {},
) {
    val settings = remember { SettingsRepository(graph.store, systemLang) }
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    LaunchedEffect(dark) { onDarkTheme(dark) }
    val tr = remember(settings.lang) { Tr(settings.lang) }
    val learnProgress = remember { LearnProgress(graph.store) }
    var learnNav by remember { mutableStateOf<LearnNav?>(null) }
    val opener = remember { LearnOpener { nav -> learnNav = nav } }
    CompositionLocalProvider(LocalAppGraph provides graph, LocalTr provides tr, LocalLearn provides opener, LocalTour provides tour) {
        StatikaTheme(dark) {
            // výklad se kreslí přes trenažér – úloha pod ním zůstane rozpracovaná
            Box(Modifier.fillMaxSize()) {
                Trainer(graph, settings)
                learnNav?.let { nav ->
                    Box(Modifier.fillMaxSize().background(LocalColors.current.bg).safeDrawingPadding()) {
                        LearnRoot(nav, learnProgress, onNav = { next ->
                            if (next == null && nav != LearnNav.Home) graph.analytics.event("learn_close")
                            learnNav = next
                        }, analytics = graph.analytics)
                    }
                }
            }
            // při prvním spuštění prohlídka aplikace; poslední krok nabídne výklad základů
            if (learnNav == null && tour.running) {
                TourOverlay(tour, onLearn = {
                    learnProgress.firstRunAsked = true
                    learnNav = LearnNav.Open(1)
                }, onEvent = { name, params ->
                    learnProgress.firstRunAsked = true
                    graph.analytics.event(name, params)
                })
            }
        }
    }
}

@Composable
private fun Trainer(graph: AppGraph, settings: SettingsRepository) {
    val tr = LocalTr.current
    val c = LocalColors.current
    var seed by remember { mutableIntStateOf(settings.seed.takeIf { it > 0 } ?: randomSeed()) }
    var attempt by remember { mutableIntStateOf(0) }
    // úloha otevřená z oblíbených má vlastní nastavení generátoru (úroveň, typy) – jinak platí nastavení aplikace
    var fixed by remember { mutableStateOf(settings.problemOpts?.let(::decodeOpts)) }
    // úloha z odkazu (statika://12345) – každé otevření je samostatná událost, i se stejným číslem
    val link = graph.links.current
    LaunchedEffect(link) {
        if (link == null) return@LaunchedEffect
        if (link.seed != seed || fixed != null) {
            seed = link.seed
            fixed = null
            attempt = 0
        }
        graph.links.consume(link)
    }
    val favorites = remember { FavoritesRepository(graph.store) }
    val opts = fixed ?: GenOptions(settings.level, settings.types)
    val problem = remember(seed, opts) { problemFor(seed, opts.level, opts.types) }
    LaunchedEffect(problem.seed) { settings.seed = problem.seed }
    LaunchedEffect(fixed) { settings.problemOpts = fixed?.encode() }
    // změna úrovně nebo typů v nastavení = zase úlohy podle nastavení
    val optsNow = settings.level to settings.types
    val optsSeen = remember { arrayOf(optsNow) }
    LaunchedEffect(optsNow) {
        if (optsSeen[0] != optsNow) {
            optsSeen[0] = optsNow
            fixed = null
        }
    }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showFavorites by remember { mutableStateOf(false) }

    val events = remember(problem) {
        val o = opts
        object : SessionEvents {
            val base = mapOf("level" to o.level, "type" to problem.type.key)
            override fun onHint(step: Int) = graph.analytics.event("hint", base + ("step" to step))
            override fun onStepDone(step: Int, hints: Int) = graph.analytics.event("step_done", base + mapOf("step" to step, "hints" to hints))
            override fun onSolved(hints: Int) {
                settings.addSolved()
                favorites.markSolved(problem.seed, o)
                settings.sinceAd += 1
                graph.analytics.event("problem_solved", base + ("hints" to hints))
            }
        }
    }
    val session = remember(problem, attempt) {
        graph.analytics.event("problem_start", mapOf("level" to opts.level, "type" to problem.type.key, "retry" to (attempt > 0).toString()))
        TrainerSession(problem, events)
    }
    val adFree by graph.billing.isAdFree.collectAsState()
    var sysKbd by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    fun newProblem() {
        seed = randomSeed()
        fixed = null
        attempt = 0
    }
    // po několika vyřešených úlohách mezistránková reklama (ne po každé)
    // dvojí klepnutí na „Další úloha“ během reklamy nesmí přeskočit úlohu
    var advancing by remember(problem, attempt) { mutableStateOf(false) }
    fun nextAfterSolved() {
        if (advancing) return
        advancing = true
        if (!adFree && settings.sinceAd >= AD_EVERY) {
            graph.ads.showInterstitial { shown ->
                if (shown) settings.sinceAd = 0
                newProblem()
            }
        } else newProblem()
    }
    // rozpracovanou úlohu nezahodit jedním omylem
    var confirmNew by remember { mutableStateOf(false) }
    fun started() = session.step in STEP_LOADS..STEP_SOLVE || (session.step == STEP_RELEASE && session.release.idx > 0)
    fun askNewProblem() {
        if (started()) confirmNew = true else newProblem()
    }
    var confirmFav by remember { mutableStateOf<Favorite?>(null) }
    fun openFavorite(f: Favorite) {
        showFavorites = false
        confirmFav = null
        if (f.same(problem.seed, opts)) return
        graph.analytics.event("fav_open", mapOf("level" to f.opts.level))
        seed = f.seed
        fixed = f.opts.takeIf { it.level != settings.level || it.types.toSet() != settings.types.toSet() }
        attempt = 0
    }
    fun toggleFavorite() {
        val on = favorites.toggle(problem.seed, opts, solved = session.step == STEP_DONE)
        graph.analytics.event(if (on) "fav_add" else "fav_remove", mapOf("level" to opts.level, "type" to problem.type.key))
        scope.launch {
            snack.currentSnackbarData?.dismiss()
            snack.showSnackbar(tr(if (on) "favAdded" else "favRemoved"), duration = SnackbarDuration.Short)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        // dvě kolonky na tabletu a na telefonu naležato (jinak by na výšku nezbylo místo)
        // tablet na výšku: jedna kolonka s velkým výkresem je lepší než dva úzké sloupce
        val wide = maxWidth > maxHeight && maxWidth >= 560.dp
        val compactDrawing = (if (wide) maxWidth * 0.55f else maxWidth) < 600.dp
        val step = session.step
        val scroll = remember(session, step) { ScrollState(0) }

        val drawing: @Composable () -> Unit = {
            Box(Modifier.dismissKeyboardOnTap()) {
                DrawingCard(session, problem, compactDrawing, attempt, favorites.isFavorite(problem.seed, opts), ::toggleFavorite)
            }
        }
        val content: @Composable () -> Unit = {
            Column(Modifier.padding(bottom = 16.dp).tourSpot(TourSpot.WORK)) {
                when (step) {
                    STEP_RELEASE -> StepReleaseView(session.release)
                    STEP_LOADS -> session.loads?.let { StepLoadsView(it) }
                    STEP_EQUATIONS -> session.equations?.let { StepEquationsView(it, sysKbd) }
                    STEP_SOLVE -> session.solve?.let { StepSolveView(it) }
                    STEP_DONE -> session.results?.let { r ->
                        StepDoneView(problem, r, session.hints, onRetry = { attempt += 1 }, onNext = ::nextAfterSolved) {
                            if (!adFree) graph.ads.Banner(Modifier.fillMaxWidth().padding(top = 32.dp))
                        }
                    }
                }
                Text(
                    tr("appSubtitle") + " · " + tr("level${opts.level}"),
                    style = MaterialTheme.typography.bodySmall, color = c.inkSoft,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }
        }
        val palette: @Composable (Modifier) -> Unit = { m ->
            val eq = session.equations
            if (step == STEP_EQUATIONS && eq != null && !eq.allDone) Palette(eq, sysKbd, { sysKbd = !sysKbd }, m)
        }

        Column(Modifier.fillMaxSize()) {
            TopBar(Modifier.dismissKeyboardOnTap(), settings.solved, favorites.items.size, onNew = ::askNewProblem, onFavorites = { showFavorites = true }, onSettings = { showSettings = true })
            if (wide) {
                Row(Modifier.weight(1f).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1.15f).verticalScroll(rememberScrollState())) {
                        drawing()
                        Spacer(Modifier.height(8.dp))
                        palette(Modifier.fillMaxWidth())
                    }
                    Column(Modifier.weight(1f)) {
                        Stepper(step, Modifier.dismissKeyboardOnTap().tourSpot(TourSpot.STEPPER))
                        Column(Modifier.weight(1f).verticalScroll(scroll).imePadding()) { content() }
                    }
                }
            } else {
                Stepper(step, Modifier.dismissKeyboardOnTap().padding(horizontal = 12.dp).tourSpot(TourSpot.STEPPER))
                // na telefonu se systémovou klávesnicí by se pole pro psaní pod výkres nevešlo – výkres se schová
                val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                if (!imeOpen) Box(Modifier.padding(horizontal = 12.dp)) { drawing() }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp).verticalScroll(scroll).imePadding().padding(top = 8.dp)) { content() }
                palette(Modifier.fillMaxWidth().imePadding())
            }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(12.dp))
    }

    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            title = { Text(tr("confirmNewTitle")) },
            text = { Text(tr("confirmNewText")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmNew = false
                    newProblem()
                }) { Text(tr("newProblem")) }
            },
            dismissButton = { TextButton(onClick = { confirmNew = false }) { Text(tr("cancel")) } },
        )
    }

    if (showFavorites) FavoritesSheet(
        favorites, current = problem.seed to opts, onDismiss = { showFavorites = false },
        onOpen = { f -> if (started() && !f.same(problem.seed, opts)) confirmFav = f else openFavorite(f) },
        onDelete = { f ->
            favorites.remove(f)
            graph.analytics.event("fav_remove", mapOf("level" to f.opts.level))
        },
    )
    confirmFav?.let { f ->
        AlertDialog(
            onDismissRequest = { confirmFav = null },
            title = { Text(tr("favOpenTitle")) },
            text = { Text(tr("favOpenText")) },
            confirmButton = { TextButton(onClick = { openFavorite(f) }) { Text(tr("open")) } },
            dismissButton = { TextButton(onClick = { confirmFav = null }) { Text(tr("cancel")) } },
        )
    }

    val tour = LocalTour.current
    if (showSettings) SettingsSheet(graph, settings, problem, opts.level, onDismiss = { showSettings = false }, onOpen = { n ->
        seed = n
        fixed = null
        attempt = 0
        showSettings = false
    }, onTour = {
        showSettings = false
        tour?.start()
    })
}

@Composable
private fun TopBar(modifier: Modifier, solved: Int, favCount: Int, onNew: () -> Unit, onFavorites: () -> Unit, onSettings: () -> Unit) {
    val opener = LocalLearn.current
    val tr = LocalTr.current
    val c = LocalColors.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
    // na telefonu (do 480 dp, tj. i Pro Max) se „Nová úloha“ zkrátí na ikonu, aby se vešel název i hvězdička oblíbených
    val narrow = maxWidth < 480.dp
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(tr("appTitle"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1)
            Text(tr("solved") + ": " + solved, style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
        }
        if (narrow) {
            IconButton(onClick = onNew, modifier = Modifier.tourSpot(TourSpot.NEW)) { Icon(Icons.Default.Refresh, contentDescription = tr("newProblem"), tint = c.react) }
        } else {
            TextButton(onClick = onNew, modifier = Modifier.tourSpot(TourSpot.NEW)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(tr("newProblem"), maxLines = 1)
            }
        }
        val favLabel = tr("favorites")
        IconButton(onClick = onFavorites, modifier = Modifier.tourSpot(TourSpot.FAVORITES).semantics { contentDescription = favLabel }) {
            Text(if (favCount > 0) "★" else "☆", fontSize = 22.sp, color = if (favCount > 0) c.highlight else c.ink)
        }
        IconButton(onClick = { opener.open(LearnNav.Home) }, modifier = Modifier.tourSpot(TourSpot.LEARN)) { Icon(BookIcon, contentDescription = tr("learnOpen"), tint = c.ink) }
        IconButton(onClick = onSettings, modifier = Modifier.tourSpot(TourSpot.SETTINGS)) { Icon(Icons.Default.Settings, contentDescription = tr("settings"), tint = c.ink) }
    }
    }
}

@Composable
private fun Stepper(step: Int, modifier: Modifier = Modifier) {
    val tr = LocalTr.current
    val c = LocalColors.current
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0..4) {
            val cur = i == step
            val past = i < step
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(24.dp).background(if (cur) c.ink else if (past) c.accent else c.line, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (past) "✓" else "${i + 1}", color = if (cur || past) c.paper else c.inkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    tr("step${i + 1}"), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (cur) c.ink else c.inkSoft, fontWeight = if (cur) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun DrawingCard(session: TrainerSession, problem: Problem, compact: Boolean, attempt: Int, favorite: Boolean, onFavorite: () -> Unit) {
    val tr = LocalTr.current
    val c = LocalColors.current
    Surface(shape = CardShape, color = c.paper, border = BorderStroke(1.dp, c.line), modifier = Modifier.fillMaxWidth().tourSpot(TourSpot.DRAWING)) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tr("problemNo") + " " + problem.seed + " · " + tr("type_${problem.type.key}"),
                    style = MaterialTheme.typography.bodySmall, color = c.inkSoft, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(tr("lengthsNote"), style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                // hvězdička vpravo v záhlaví výkresu – uložit do oblíbených
                Box(
                    Modifier.padding(start = 6.dp).size(36.dp).tourSpot(TourSpot.STAR).clip(CircleShape)
                        .clickable(onClickLabel = tr(if (favorite) "favRemove" else "favAdd"), onClick = onFavorite)
                        .semantics { contentDescription = tr(if (favorite) "favRemove" else "favAdd") },
                    contentAlignment = Alignment.Center,
                ) { Text(if (favorite) "★" else "☆", fontSize = 22.sp, color = if (favorite) c.highlight else c.ink) }
            }
            BeamCanvas(
                problem = problem,
                released = session.released,
                resolved = session.resolved,
                highlight = session.highlight,
                view = if (session.step == STEP_EQUATIONS) session.view else cz.usharik.statika.trainer.DrawingView(),
                results = if (session.step == STEP_DONE) session.results else null,
                activeSupport = if (session.step == STEP_RELEASE) session.activeSupport else null,
                compact = compact,
                animKey = problem.seed to attempt,
                description = tr("drawing"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsSheet(graph: AppGraph, settings: SettingsRepository, problem: Problem, problemLevel: Int, onDismiss: () -> Unit, onOpen: (Int) -> Unit, onTour: () -> Unit) {
    val tr = LocalTr.current
    val c = LocalColors.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val adFree by graph.billing.isAdFree.collectAsState()
    val price by graph.billing.removeAdsPrice.collectAsState()
    val privacy by graph.ads.privacyOptionsRequired.collectAsState()

    @Composable
    fun Section(title: String, content: @Composable () -> Unit) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = c.inkSoft, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        content()
    }

    @Composable
    fun Chip(label: String, on: Boolean, onClick: () -> Unit) = FilterChip(
        selected = on, onClick = onClick, label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.ink, selectedLabelColor = c.paper),
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = c.paper) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            Text(tr("settings"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Section(tr("level")) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (l in 1..3) Chip(tr("level$l"), settings.level == l) {
                        settings.updateLevel(l)
                        graph.analytics.event("settings_level", mapOf("level" to l))
                    }
                }
            }
            Section(tr("types")) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (t in BeamType.entries) Chip(tr("type_${t.key}"), t in settings.types) { settings.toggleType(t) }
                }
            }
            Section(tr("language")) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Čeština", settings.lang == Lang.CS) { settings.updateLang(Lang.CS) }
                    Chip("Русский", settings.lang == Lang.RU) { settings.updateLang(Lang.RU) }
                }
            }
            Section(tr("theme")) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(tr("themeSystem"), settings.theme == ThemeMode.SYSTEM) { settings.updateTheme(ThemeMode.SYSTEM) }
                    Chip(tr("themeLight"), settings.theme == ThemeMode.LIGHT) { settings.updateTheme(ThemeMode.LIGHT) }
                    Chip(tr("themeDark"), settings.theme == ThemeMode.DARK) { settings.updateTheme(ThemeMode.DARK) }
                }
            }
            Section(tr("openProblem")) {
                var num by remember { mutableStateOf("") }
                val n = num.trim().toIntOrNull()?.takeIf { it in 1..999_999 }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = num, onValueChange = { v -> num = v.filter(Char::isDigit).take(6) },
                        singleLine = true, placeholder = { Text("12345") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { if (n != null) onOpen(n) }),
                        modifier = Modifier.widthIn(max = 160.dp), shape = RoundedCornerShape(9.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    PrimaryButton(tr("open"), { if (n != null) onOpen(n) })
                }
                Text(tr("openProblemHint"), style = MaterialTheme.typography.bodySmall, color = c.inkSoft, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = {
                graph.analytics.event("share_problem")
                graph.platform.shareText(tr("shareText", "n" to problem.seed, "level" to tr("level$problemLevel"), "url" to WEB_URL + "#" + problem.seed))
            }) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("share"))
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = c.line)
            if (adFree) {
                Text(tr("adFree"), color = c.accent, fontWeight = FontWeight.SemiBold)
            } else {
                Text(tr("removeAdsDesc"), style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton(tr("removeAds") + (price?.let { " · $it" } ?: ""), {
                        graph.analytics.event("remove_ads_click")
                        graph.billing.purchaseRemoveAds()
                    })
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = graph.billing::restorePurchases) { Text(tr("restorePurchases")) }
                }
            }
            if (privacy) TextButton(onClick = graph.ads::showPrivacyOptions) { Text(tr("privacyOptions")) }
            TextButton(onClick = { graph.platform.openUrl(PRIVACY_URL) }) { Text(tr("privacyPolicy")) }
            TextButton(onClick = {
                graph.analytics.event("tour_restart")
                onTour()
            }) { Text(tr("tourRestart")) }
            Text(tr("version") + " " + graph.platform.versionName, style = MaterialTheme.typography.bodySmall, color = c.inkSoft, modifier = Modifier.padding(start = 12.dp, top = 4.dp))
        }
    }
}

/**
 * Klepnutí sem skryje klávesnici. Jen na místech bez textových polí (horní lišta, kroky, výkres):
 * číselná klávesnice iOS nemá „Hotovo“ a naležato zakryje skoro celou obrazovku.
 */
@Composable
private fun Modifier.dismissKeyboardOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    return pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
}

/** Otevřená kniha – vstup do výkladu (statika i trigonometrie) a procvičování; ⓘ se pletlo s „o aplikaci“. */
private val BookIcon: ImageVector by lazy {
    ImageVector.Builder("Book", 24.dp, 24.dp, 24f, 24f).apply {
        // levá a pravá stránka rozevřené knihy, mezi nimi hřbet
        path(fill = SolidColor(Color.Black)) {
            moveTo(11f, 5.5f)
            curveTo(9f, 4.2f, 5.8f, 3.8f, 2.5f, 4.5f)
            verticalLineTo(19f)
            curveTo(5.8f, 18.3f, 9f, 18.7f, 11f, 20f)
            close()
            moveTo(13f, 5.5f)
            curveTo(15f, 4.2f, 18.2f, 3.8f, 21.5f, 4.5f)
            verticalLineTo(19f)
            curveTo(18.2f, 18.3f, 15f, 18.7f, 13f, 20f)
            close()
        }
    }.build()
}
