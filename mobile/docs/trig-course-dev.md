# Kurz „Základy trigonometrie“ – průvodce pro vývojáře

Obsah kurzu (lekce, karty, typy úloh, texty cs/ru, chyby) je v návrhu kurzu (úkol „Design trigonometry course“).
Tady je **jak** ho do aplikace psát: kostra je hotová (fáze 1), fáze 2 doplňuje karty lekcí a typy úloh.

## Mapa kódu (`shared/src/commonMain/kotlin/cz/usharik/statika/`)

| Kde | Co |
|---|---|
| `learn/Lessons.kt` | `Course`, `Courses.statics` / `Courses.trig` / `Courses.all`, `Lessons.all` / `byId` / `courseOf` |
| `learn/Learn.kt` | `Lesson` (`id` globálně jednoznačné, `no` = číslo v kurzu), `CardScope`, `LearnNav` (`Open.returnTo`, `Practice`), `LocalLearnHere`, `LearnProgress` (`course`, `next(course)`, `practice`) |
| `learn/trig/TrigLessonN.kt` | lekce 101–108 – **zatím jedna zástupná karta** (`Placeholder`); fáze 2 sem píše karty |
| `learn/trig/TrigGlossary.kt` | slovníček kurzu (druhý oddíl obrazovky Slovníček) |
| `learn/TrigKit.kt` | geometrie a kreslení: `TriGeom` / `triGeom`, `rightTriangle`, `rightAngleMark`, `angleArc`, `screenAngle`, `protractor`, `grid`, `axes`, `dimLine`, `bar`, `fig(Fig)`, `inclinedForce` (síla na nosník z lekce 3) |
| `learn/NumAnswer.kt` | `NumAnswer` (číselná odpověď v kartě), `NumSpec`, `NumTrap`, `LearnJump` (odkaz na jinou kartu s návratem) |
| `learn/PracticeScreen.kt` | obrazovka procvičování (seznam sérií, úloha, výsledek) |
| `practice/` | čistý Kotlin: `Trig` (přesné sin/cos/tg ve stupních), `Tol` / `accept` / `parseRatio`, `Fig`, `Field` / `Trap` / `Exercise` / `exercise(...)` / `check(...)`, `ExerciseType`, `Exercises` (rejstřík), `Traps` (katalog chyb), `PracticeSet` / `PracticeSets`, `PracticeSession`, `PracticeStore`, `seriesFor` |
| `practice/types/Exx.kt` | typy úloh; hotové vzory **E03** (odvěsna z přepony) a **E17** (složky síly) |

## Konvence

- **Id lekcí**: statika 1–9, trigonometrie 101–108. Odkazy `learn(lesson, card)`, `LearnLink`, `LearnJump` i klíče postupu
  (`learn.seen.101`, `learn.card.101.0` …) používají globální id. Žákovi se ukazuje `lesson.no`.
- **Texty**: vždy `L(cs, ru)`, čeština první. Značky `[[F_x]]` (index), `**tučně**`. Krátce: ≤ 2 věty na blok.
  V ruštině: величина, наклон (směr), направление (smysl), линия действия (nositelka), плечо (rameno), сила тяжести;
  jednotky v textu кН, кН·м, м; v obrázcích a výsledcích latinkou (kN, m).
- **Barvy** (`StatikaColors`): přepona / síla `c.load` (červeně), odvěsny / složky `c.derived` (fialově),
  úhly a pomocné čáry `c.inkSoft`, zvýrazněná strana `c.highlight`, rameno / „správně“ `c.accent`. `c.react` (modrá) jen pro reakce.
- **Scéna**: `Stage(h = …)` je 400 jednotek široká, osa y dolů. Úhly pro `angleArc` / `Prim.Arc` jsou „na obrazovce“
  (po směru hodin od +x); `Fig` a `Trig` počítají matematicky (φ od +x proti směru hodin) – převod `screen = -φ`.
- **Přesné hodnoty**: ve výkladu a procvičování vždy `Trig.sinD/cosD/tgD` (sin 30° = 0,5 přesně). Nepoužívej
  `Mechanics.forceComponents` (ta má bitově shodné hodnoty s webem trenažéru).

## Jak napsat kartu

```kotlin
@Composable
private fun CardScope.SameAngle() {
    val t = tr()
    var k by remember { mutableStateOf(1.0) }
    var grown by remember { mutableStateOf(false) }
    Stage(h = 230.0, description = t.pick(L("Rampa", "Пандус")), onDrag = { p, _ -> /* … */ }) { c ->
        val g = triGeom(4.0 * k, 2.31 * k, fit = listOf(60.0, 20.0, 280.0, 190.0))
        rightTriangle(g, c, labels = mapOf(TriSide.OPP to "výška", TriSide.HYP to "šikmá délka"), angle = "30°")
    }
    CardText(L("…", "…"))
    Todo(L("Prodluž rampu …", "Удлини пандус …") to grown)   // povinné úkoly – bez nich je „Dál“ zamčené
    ValueSlider(/* dublér tahu */) { k = it.toDouble(); if (k >= 2) grown = true }
    Readout("výška / šikmá délka = ${num(0.5)}")
}
```

- Každá karta musí mít **Todo**, **Quiz** nebo **NumAnswer** a na začátku nesmí být nic splněné – `CardGateTest`
  projde všechny karty `Lessons.all` sám.
- Každý tah má dublér (posuvník nebo čipy) – na malém telefonu se tahá špatně.
- Řešení na obrázku ukazuj až po splnění (`solvedHere`).
- **Číselná odpověď**: `NumAnswer(question, listOf(NumSpec(L("výška","высота"), 4.33, "m")), explain, traps = listOf(NumTrap.near(why, 2.5)), nudge = …)`.
  `NumSpec.tol`: `Tol.VALUE` (síly, délky), `ANGLE` (±0,5°), `RATIO` (sin/cos/tg ±0,006), `PERCENT`. `signed = true` → pole s ±.
- **Odkaz na jinou kartu** (i do statiky): `LearnJump(4, 1)` – po zavření cílové lekce se žák vrátí na tuto kartu.
- Lekce přepiš v `TrigLessonN.kt` (`cards = listOf(Card { SameAngle() }, …)`), `Placeholder` smaž. Až budou všechny
  lekce hotové, zpřísni v `LearnTest` minimum karet na 3 a kontrolu karet u odkazů na lekce ≥ 100.

## Jak přidat typ úlohy

1. Soubor `practice/types/Exx.kt`:

```kotlin
val E12 = ExerciseType("E12", L("Úhel ze stran", "Угол по сторонам"), learn = 106 to 1, levels = 1..3) { rng, level, seed ->
    // generuj z rng (deterministicky!) – „hezká“ čísla podle úrovně
    exercise(
        "E12", seed, level,
        prompt = L("…", "…"),
        fig = Fig.Tri(adj = 4.0, opp = 3.0, labels = mapOf(TriSide.ADJ to "4", TriSide.OPP to "3"), angle = "?"),
        fields = listOf(Field(L("α", "α"), Trig.atanD(0.75), "°", tol = Tol.ANGLE, digits = 1)),
        hints = listOf(L("princip …", "…"), L("vzorec s čísly …", "…")),   // 3. „Výsledek: …“ se doplní sám
        traps = listOf(Traps.COMPL.at(90 - ans), Traps.RAD.at(…), Traps.RATIO.at(0.75)),
        ok = L("Správně: …", "Верно: …"),
    )
}
```

2. Přidej `E12` do `Exercises.all` (practice/Exercises.kt). Soubory sérií (`PracticeSets`) už id typů znají –
   série se samy „odemknou“, jakmile v nich je aspoň jeden existující typ.
3. Úloha s volbou: `choice = listOf(Opt(L(…), correct = true), Opt(L(…), why = L(…)), …)` místo `fields`.

Pravidla:
- **Chyby (traps)** ber z katalogu `Traps` (texty a odkazy jsou tam). `Traps.X.at(hodnoty…)` – hodnoty, které ta chyba
  dá, `null` = pole nehraje roli. Složitější podmínka: `Traps.X.where(příklad) { values -> … }`.
  Konkrétnější text: `Traps.FULL.with(L(…)).at(…)`. Pořadí v seznamu = priorita (první, která sedí, vyhraje).
- Chyba, kterou nejde odlišit od správné odpovědi (sin 45° = cos 45°), se v `exercise()` **sama vyřadí**.
- Pole bez znaménka (`signed = false`) znaménko ignorují – chyby se pak porovnávají v absolutní hodnotě.
- Odpověď nezaokrouhluj – kontrola přijme 2–3 desetinná místa i správné zaokrouhlení na méně míst (`Tol`).

`ExerciseHarnessTest` projde **každý registrovaný typ** na všech jeho úrovních a 300 číslech úloh:
platné texty cs/ru bez nedosazených `{}` a nespárovaných `[[ ]]`, 3 nápovědy, determinismus, správná odpověď projde
(i s tečkou, i s jiným znaménkem u polí bez znaménka), každá chyba se pozná a neprojde, žádná chyba nesedí na správnou odpověď.
Nové testy pro typ psát netřeba – stačí `./gradlew :shared:jvmTest`.

## Procvičování – chování

- Série = `PracticeSet` (8 úloh, mix 12) × úroveň × **číslo série** (`seriesFor(set, level, seed)` je deterministické na
  všech platformách – učitel může zadat „série 4127“).
- Kontrola: vše správně → první typická chyba, která sedí (zpráva + odkaz do kurzu) → „skoro“ (do 5 %) → obecná zpráva.
- Nápověda: 1. princip, 2. vzorec s čísly, 3. výsledek (doplní pole, úloha bez bodu). ★ za ≥ 75 % bodů.
- Uložené klíče: `practice.tg.<set>.level|best|star|runs`, `practice.tg.miss.<typ>`; kurz `learn.course`.
- Analytika: `practice_start`, `practice_task` (type, ok, hints, trap), `practice_done`.

## Ověření

```bash
./gradlew :shared:jvmTest                      # vč. CardGateTest, ExerciseHarnessTest, NumAnswerTest
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :shared:iosSimulatorArm64Test
./gradlew :app:assembleDebug
```
