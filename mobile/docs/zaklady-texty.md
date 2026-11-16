# Základy statiky — texty k vyčtení / тексты для вычитки

Vygenerováno z `mobile/shared/src/commonMain/kotlin/cz/usharik/statika/learn/`. Opravy pište přímo sem nebo do kódu; 
značky: `[[F_x]]` = symbol s indexem, `**…**` = tučně.

## Lekce 1: Síla je šipka / Сила — это стрелка

### DragArrow

| čeština | русский |
|---|---|
| Šipka síly, kterou můžeš natáhnout a natočit | Стрелка силы, которую можно растянуть и повернуть |
| působiště | точка приложения |
| čárkovaně: nositelka – přímka, na které síla leží | пунктир: линия действия — прямая, где лежит сила |
| Sílu kreslíme **šipkou**. Tečka je **působiště** (kde síla působí), čárkovaná přímka **nositelka**. Zatáhni za kroužek u hrotu a vyzkoušej, co všechno se na šipce dá změnit. | Силу рисуем **стрелкой**. Точка — **точка приложения** (působiště), пунктир — **линия действия** (nositelka). Потяни за кружок у острия и проверь, что в стрелке можно менять. |
| **Velikost** = délka šipky: tahem za kroužek šipku prodluž nebo zkrať aspoň o 1 kN | **Величина** (velikost) = длина стрелки: потяни за кружок и удлини или укороти стрелку хотя бы на 1 кН |
| **Směr** = sklon nositelky: natoč šipku šikmo (tahem za kroužek nebo tlačítkem „↗“) | **Наклон** (směr) = наклон линии действия: поверни стрелку наискосок (кружком или кнопкой «↗») |
| **Smysl** = kam míří hrot: otoč šipku vodorovně doleva (tahem za kroužek nebo tlačítkem „←“) | **Направление** (smysl) = куда смотрит остриё: разверни стрелку горизонтально влево (кружком или кнопкой «←») |
| → | → |
| ↗ | ↗ |
| ↑ | ↑ |
| ← | ← |
| ↓ | ↓ |

### BookTable

| čeština | русский |
|---|---|
| Bedna na stole | Ящик на столе |
|  (tíha) |  (сила тяжести) |
| stůl tlačil nahoru | стол давил вверх |
| Bedna (asi 100 kg) leží na stole a nehýbe se. Tlačí stůl na bednu nějakou silou? | Ящик (около 100 кг) лежит на столе и не двигается. Давит ли стол на ящик какой-то силой? |
| Tipni si: klepni na „Ano“, nebo na „Ne, nic se nehýbe“ | Сначала предположи: нажми «Да» или «Нет, ничего не движется» |
| Ověř to pokusem: klepni na „Odstraň stůl“ (tlačítko se objeví po tipu) | Проверь на опыте: нажми «Убрать стол» (кнопка появится после ответа) |
| Ano | Да |
| Ne, nic se nehýbe | Нет, ничего не движется |
| Odstraň stůl | Убрать стол |
| Přesně! Bez stolu bedna spadne – stůl ji tedy celou dobu tlačil **nahoru**. Síla působí, i když se nic nehýbe. | Точно! Без стола ящик падает — значит, стол всё время давил на него **вверх**. Сила действует, даже когда ничего не движется. |
| A přece tlačí: bez stolu bedna spadla. Stůl ji tedy celou dobu tlačil **nahoru** – síla působí, i když se nic nehýbe. | И всё-таки давит: без стола ящик упал. Значит, стол всё время давил на него **вверх** — сила действует, даже когда ничего не движется. |

### LineOfAction

| čeština | русский |
|---|---|
| Bedna tlačená silou po nositelce | Ящик, толкаемый силой вдоль линии действия |
| Sílu smíš posunout **po její nositelce** (čárkované přímce) – účinek na bednu se nezmění. Vyzkoušej to a pak sílu přesuň na jinou nositelku, výš. | Силу можно сдвигать **вдоль её линии действия** (пунктира) — действие на ящик не меняется. Проверь это, а потом перенеси силу на другую линию действия, выше. |
| Klepni na „Tlačí zleva“ (nebo posuň šipku kroužkem doleva a pusť) a sleduj bednu | Нажми «Толкает слева» (или сдвинь стрелку кружком влево и отпусти) и следи за ящиком |
| Klepni na „Táhne zprava“ (nebo posuň šipku doprava) – jede bedna stejně? | Нажми «Тянет справа» (или сдвинь стрелку вправо) — так же ли едет ящик? |
| Klepni na „Posuň výš“ – co se změní? | Нажми «Сдвинуть выше» — что изменится? |
| Tlačí zleva | Толкает слева |
| Táhne zprava | Тянет справа |
| Posuň výš | Сдвинуть выше |
| Tlačit zleva, nebo táhnout zprava po stejné nositelce – bedna jede stejně. | Толкать слева или тянуть справа вдоль одной линии действия — ящик едет одинаково. |
| Výš je to už **jiná** (rovnoběžná) nositelka. Velikost, směr i smysl jsou stejné, ale síla teď bednu i převrací – tomu se říká moment (lekce 4). | Выше — уже **другая** (параллельная) линия действия. Величина, наклон и направление те же, но теперь сила ящик ещё и опрокидывает — это называется момент (урок 4). |

### Units

| čeština | русский |
|---|---|
| Závaží po 100 kg znázorňující tíhu v kN | Гири по 100 кг и их сила тяжести в кН |
| ≈ malé osobní auto | ≈ небольшой автомобиль |
| Síly měříme v kilonewtonech (kN). **1 kN ≈ tíha závaží o hmotnosti 100 kg** – tedy síla, kterou ho Země táhne dolů. | Силы измеряем в килоньютонах (кН). **1 кН ≈ сила тяжести гири массой 100 кг** — с такой силой Земля тянет её вниз. |
| Přidávej závaží tlačítkem „+ 1 kN“, až tíha [[G]] dosáhne 10 kN – tolik má malé osobní auto (asi 1 t) | Добавляй гири кнопкой «+ 1 кН», пока сила тяжести [[G]] не дойдёт до 10 кН — как у небольшого автомобиля (около 1 т) |
| − 1 kN | − 1 кН |
| + 1 kN | + 1 кН |

### CheckSense

| čeština | русский |
|---|---|
| Síla F a tři další šipky A, B, C | Сила F и ещё три стрелки A, B, C |
| Která šipka má stejný směr (sklon) jako [[F]], ale opačný smysl (hrot na druhou stranu)? | У какой стрелки тот же наклон (směr), что у [[F]], но противоположное направление (smysl) — остриё в другую сторону? |
| A | A |
| B | B |
| B má jiný sklon, tedy jiný směr. Která šipka je s [[F]] rovnoběžná, ale hrot má na druhé straně? | У B другой наклон. Какая стрелка параллельна [[F]], но остриё у неё с другой стороны? |
| C | C |
| C má stejný sklon, ale i stejný smysl jako [[F]] – je jen posunutá. Která šipka míří hrotem na opačnou stranu? | У C тот же наклон, но и направление то же, что у [[F]], — она просто сдвинута. У какой стрелки остриё смотрит в обратную сторону? |
| Směr = sklon nositelky, takže rovnoběžné šipky mají stejný směr. Smysl = kam míří hrot: A je rovnoběžná s [[F]], ale míří opačně. | Наклон (směr) у параллельных стрелок одинаковый. Направление (smysl) — куда смотрит остриё: A параллельна [[F]], но направлена противоположно. |
| Síla je šipka | Сила — это стрелка |
| Velikost, směr, smysl a působiště síly. | Величина, наклон, направление и точка приложения силы. |

## Lekce 2: Rovnováha sil / Равновесие сил

### TugOfWar

| čeština | русский |
|---|---|
| Bedna tažená dvěma silami proti sobě | Ящик, который тянут две силы в разные стороны |
| převaha ${num(abs(res).toDouble(), 1)} kN doprava – bedna se rozjede | перевес ${num(abs(res).toDouble(), 1)} кН вправо — ящик поедет |
| převaha ${num(abs(res).toDouble(), 1)} kN doleva – bedna se rozjede | перевес ${num(abs(res).toDouble(), 1)} кН влево — ящик поедет |
| rovnováha – bedna stojí | равновесие — ящик стоит |
| Dva týmy táhnou bednu každý na svou stranu. Σ[[F_x]] čti „součet vodorovných sil“: doprava počítáme +, doleva −. Nastav síly tak, aby bedna **stála**. | Две команды тянут ящик каждая в свою сторону. Σ[[F_x]] читается «сумма горизонтальных сил»: вправо считаем +, влево −. Подбери силы так, чтобы ящик **стоял**. |
| Nastav posuvníky „Levý tým“ a „Pravý tým“ tak, aby bedna zůstala **stát** (Σ[[F_x]] = 0) | Поставь ползунки «Левая команда» и «Правая команда» так, чтобы ящик **остановился** (Σ[[F_x]] = 0) |
| Levý tým [[F_1]] = ${num(left.toDouble(), 1)} kN | Левая команда [[F_1]] = ${num(left.toDouble(), 1)} кН |
| Pravý tým [[F_2]] = ${num(right.toDouble(), 1)} kN | Правая команда [[F_2]] = ${num(right.toDouble(), 1)} кН |
| Tomu se říká **rovnováha**: síly působí dál, jen se navzájem vyruší. Rovnováha ≠ žádné síly. | Это и есть **равновесие**: силы по-прежнему действуют, просто уравновешивают друг друга. Равновесие ≠ отсутствие сил. |

### TableForce

| čeština | русский |
|---|---|
| Bedna na stole: tíha dolů a síla stolu nahoru | Ящик на столе: сила тяжести вниз и сила стола вверх |
| Bedna leží na stole, její tíha je [[G]] = 5 kN. Jak velkou silou [[N]] tlačí stůl bednu nahoru? | Ящик лежит на столе, его сила тяжести [[G]] = 5 кН. С какой силой [[N]] стол давит на ящик вверх? |
| 0 kN | 0 кН |
| Kdyby stůl netlačil, bedna by spadla (jako v lekci 1). Jak velká síla ji udrží? | Если бы стол не давил, ящик упал бы (как в уроке 1). Какая сила его удержит? |
| 5 kN | 5 кН |
| 10 kN | 10 кН |
| Kdyby stůl tlačil víc, než je tíha bedny, bedna by nadskočila. Ona ale stojí – jaké tedy musí být síly nahoru a dolů? | Если бы стол давил сильнее силы тяжести ящика, ящик бы подпрыгнул. Но он стоит — какими же должны быть силы вверх и вниз? |
| Bedna stojí, takže součet svislých sil Σ[[F_y]] = 0 (nahoru +, dolů −): [[N]] − 5 = 0, tedy [[N]] = 5 kN. Síla stolu je stejně velká jako tíha, jen míří opačně. | Ящик стоит, значит, сумма вертикальных сил Σ[[F_y]] = 0 (вверх +, вниз −): [[N]] − 5 = 0, то есть [[N]] = 5 кН. Сила стола по величине равна силе тяжести, но направлена противоположно. |

### TwoAxes

| čeština | русский |
|---|---|
| Bedna se silami ve dvou směrech a jedna síla chybí | Ящик с силами в двух направлениях, одной силы не хватает |
| Bedna má zůstat stát. Jaká síla chybí na místě otazníku? | Ящик должен остаться на месте. Какой силы не хватает там, где стоит знак вопроса? |
| 4 kN ← | 4 кН ← |
| 4 kN → | 4 кН → |
| Pak by obě vodorovné síly táhly doprava. Na kterou stranu musí chybějící síla mířit, aby se vyrušily? | Тогда обе горизонтальные силы тянули бы вправо. В какую сторону должна быть направлена недостающая сила, чтобы они уравновесились? |
| 3 kN ↓ | 3 кН ↓ |
| Svisle už je rovnováha: 3 nahoru, 3 dolů. Co ještě nesedí ve vodorovném směru? | По вертикали уже равновесие: 3 вверх, 3 вниз. Что ещё не сходится по горизонтали? |
| 10 kN ← | 10 кН ← |
| Svislé síly (3 a 3) se už vyrušily, ty sem nepočítej. Jak velká vodorovná síla vyrovná [[F_1]] = 4 kN? | Вертикальные силы (3 и 3) уже уравновешены, их сюда не прибавляй. Какая горизонтальная сила уравновесит [[F_1]] = 4 кН? |
| Vodorovně a svisle počítáme **zvlášť**. Σ[[F_y]] = 3 − 3 = 0 už platí; aby vyšlo i Σ[[F_x]] = 4 − [[F_4]] = 0, musí [[F_4]] = 4 kN mířit doleva. | По горизонтали и по вертикали считаем **отдельно**. Σ[[F_y]] = 3 − 3 = 0 уже выполняется; чтобы и Σ[[F_x]] = 4 − [[F_4]] = 0, нужна [[F_4]] = 4 кН влево. |

### CheckStill

| čeština | русский |
|---|---|
| Lano přetahované dvěma týmy, které stojí | Канат, который перетягивают две команды, не двигается |
| tým 1 | команда 1 |
| tým 2 | команда 2 |
| značka na laně se nehýbe | метка на канате не двигается |
| Oba týmy táhnou, lano se ale nehýbe. Co platí? | Обе команды тянут, но канат не двигается. Что верно? |
| na lano nepůsobí žádné síly | на канат не действуют никакие силы |
| Týmy táhnou ze všech sil – síly tam jsou. Co se stane, když jsou stejně velké a míří proti sobě? | Команды тянут изо всех сил — силы есть. Что будет, если они равны по величине и направлены в противоположные стороны? |
| síly působí, ale vyruší se | силы действуют, но уравновешивают друг друга |
| působí jen síla silnějšího týmu | действует только сила той команды, что сильнее |
| Kdyby byla jedna síla větší, lano by se rozjelo. Ono ale stojí. | Если бы одна сила была больше, канат бы поехал. А он стоит. |
| Rovnováha neznamená „žádné síly“: síly působí, ale jejich součet je nula – Σ[[F_x]] = 3 − 3 = 0. | Равновесие не значит «нет сил»: силы действуют, но их сумма равна нулю — Σ[[F_x]] = 3 − 3 = 0. |
| Rovnováha sil | Равновесие сил |
| Když se nic nehýbe, síly se navzájem vyruší. | Если ничего не движется, силы уравновешивают друг друга. |

## Lekce 3: Jedna šipka = dvě / Одна стрелка = две

### AngleComponents

| čeština | русский |
|---|---|
| Šikmá síla a její složky | Наклонная сила и её составляющие |
| Složky šikmé síly spočítáš z úhlu α mezi silou a nosníkem. Měň α a sleduj, jak se obě složky mění. | Составляющие наклонной силы найдёшь по углу α между силой и балкой. Меняй α и следи, как меняются обе составляющие. |
| Nastav α = 0° (posuvníkem „α“ nebo tahem za kroužek) a podívej se, která složka zmizí | Поставь α = 0° (ползунком «α» или кружком) и посмотри, какая составляющая исчезнет |
| Nastav α = 90° – která složka zmizí teď? | Поставь α = 90° — какая составляющая исчезнет теперь? |
| Při α = 0° je celá síla vodorovná: [[F_x]] = [[F]], svislá složka [[F_y]] = 0. | При α = 0° вся сила горизонтальна: [[F_x]] = [[F]], вертикальная составляющая [[F_y]] = 0. |
| Při α = 90° je celá síla svislá: [[F_y]] = [[F]], vodorovná složka [[F_x]] = 0. | При α = 90° вся сила вертикальна: [[F_y]] = [[F]], горизонтальная составляющая [[F_x]] = 0. |
| Pomůcka: složka **u úhlu** = [[F]] · cos α, složka **naproti** úhlu = [[F]] · sin α. Tady α měříme od vodorovné, proto je u úhlu [[F_x]]. | Подсказка: составляющая, **прилежащая к углу**, = [[F]] · cos α, **противолежащая** = [[F]] · sin α. Здесь α отсчитываем от горизонтали, поэтому к углу прилежит [[F_x]]. |
| 📖 Proč zrovna cos a sin? → Trigonometrie: Sinus, kosinus, tangens | 📖 Почему именно cos и sin? → Тригонометрия: синус, косинус, тангенс |

### ReplaceToggle

| čeština | русский |
|---|---|
| Síla nebo její dvě složky | Сила или две её составляющие |
| Šikmá síla tlačí na **nosník** (vodorovný trám). Smíme ji nahradit dvěma **složkami** – vodorovnou [[F_x]] a svislou [[F_y]]. Dohromady dělají přesně totéž co celá síla. | Наклонная сила давит на **балку** (nosník — горизонтальный брус). Её можно заменить двумя **составляющими** (složky) — горизонтальной [[F_x]] и вертикальной [[F_y]]. Вместе они действуют точно так же, как вся сила. |
| Klepni na „Složky [[F_x]], [[F_y]]“ – síla se nahradí složkami | Нажми «Составляющие [[F_x]], [[F_y]]» — сила заменится составляющими |
| Klepni na „Síla [[F]]“ – složky se zase složí v jednu sílu | Нажми «Сила [[F]]» — составляющие снова сложатся в одну силу |
| Síla [[F]] | Сила [[F]] |
| Složky [[F_x]], [[F_y]] | Составляющие [[F_x]], [[F_y]] |
| Složka nikdy není větší než celá síla. Složky jsou odvěsny pravoúhlého trojúhelníku, síla je jeho nejdelší strana (přepona): tady 8 a 6 kN z 10 kN. | Составляющая никогда не больше всей силы. Составляющие — катеты прямоугольного треугольника, сила — его самая длинная сторона (гипотенуза): здесь 8 и 6 кН из 10 кН. |

### Grid345

| čeština | русский |
|---|---|
| Síla na čtverečkované síti | Сила на клетчатой сетке |
| Šipka síly je dlouhá 5 dílků (dílek = strana čtverečku). Spočítej čtverečky: o kolik dílků míří doprava a o kolik nahoru? | Стрелка силы длиной 5 делений (деление = сторона клетки). Посчитай клетки: на сколько делений она уходит вправо и на сколько вверх? |
| Jak velké jsou složky [[F_x]] (vodorovná) a [[F_y]] (svislá)? | Чему равны составляющие [[F_x]] (горизонтальная) и [[F_y]] (вертикальная)? |
| [[F_x]] = 4, [[F_y]] = 3 | [[F_x]] = 4, [[F_y]] = 3 |
| [[F_x]] = 3, [[F_y]] = 4 | [[F_x]] = 3, [[F_y]] = 4 |
| [[F_x]] je **vodorovná** složka. Spočítej znovu: o kolik dílků míří šipka doprava? | [[F_x]] — **горизонтальная** составляющая. Посчитай ещё раз: на сколько делений стрелка уходит вправо? |
| [[F_x]] = 5, [[F_y]] = 5 | [[F_x]] = 5, [[F_y]] = 5 |
| Může být složka stejně dlouhá jako celá šikmá šipka? Spočítej čtverečky pod ní a vedle ní. | Может ли составляющая быть такой же длины, как вся наклонная стрелка? Посчитай клетки под ней и сбоку. |
| 4 dílky doprava a 3 nahoru. Pozor: 4 + 3 ≠ 5 – složky se silou tvoří pravoúhlý trojúhelník, platí 4² + 3² = 5². | 4 деления вправо и 3 вверх. Внимание: 4 + 3 ≠ 5 — составляющие и сила образуют прямоугольный треугольник: 4² + 3² = 5². |
| 📖 Proč 4² + 3² = 5²? → Trigonometrie: Pythagorova věta | 📖 Почему 4² + 3² = 5²? → Тригонометрия: теорема Пифагора |

### CheckCos

| čeština | русский |
|---|---|
| Síla 10 kN pod úhlem 30° k nosníku | Сила 10 кН под углом 30° к балке |
| Síla [[F]] = 10 kN svírá s nosníkem 30° (cos 30° ≈ 0,87; sin 30° = 0,5). Jak velká je vodorovná složka [[F_x]]? | Сила [[F]] = 10 кН составляет с балкой 30° (cos 30° ≈ 0,87; sin 30° = 0,5). Чему равна горизонтальная составляющая [[F_x]]? |
| 8,66 kN | 8,66 кН |
| 5 kN | 5 кН |
| To je 10 · sin 30° – složka **naproti** úhlu, tedy svislá [[F_y]]. Kterou funkcí spočítáš složku u úhlu? | Это 10 · sin 30° — составляющая **напротив** угла, то есть вертикальная [[F_y]]. Какой функцией найти составляющую, прилежащую к углу? |
| 10 kN | 10 кН |
| Síla je šikmá, takže vodorovně míří jen její část. Vyjde víc, nebo míň než 10 kN? | Сила наклонная, поэтому горизонтально действует только её часть. Получится больше или меньше 10 кН? |
| 11,5 kN | 11,5 кН |
| Složka nikdy není větší než celá síla. Zkus 10 · cos 30°. | Составляющая никогда не больше всей силы. Попробуй 10 · cos 30°. |
| [[F_x]] leží u úhlu, proto kosinus: [[F_x]] = 10 · cos 30° = 8,66 kN. Svislá [[F_y]] = 10 · sin 30° = 5 kN. | [[F_x]] прилежит к углу, поэтому косинус: [[F_x]] = 10 · cos 30° = 8,66 кН. Вертикальная [[F_y]] = 10 · sin 30° = 5 кН. |
| Jedna šipka = dvě | Одна стрелка = две |
| Šikmou sílu nahradíš vodorovnou a svislou složkou. | Наклонную силу заменишь горизонтальной и вертикальной составляющими. |

## Lekce 4: Moment síly / Момент силы

### Wrench

| čeština | русский |
|---|---|
| Klíč na matici, síla v nastavitelné vzdálenosti od osy | Ключ на гайке, сила на изменяемом расстоянии от оси |
| Síla umí těleso i **otočit** – tady klíč s maticí. Otáčivý účinek je **moment síly** [[M]] = síla × **rameno** [[r]] (vzdálenost síly od osy matice). Posouvej sílu po klíči a sleduj [[M]]. | Сила может тело ещё и **повернуть** — здесь ключ с гайкой. Вращающее действие — **момент силы** [[M]] = сила × **плечо** [[r]] (rameno — расстояние от силы до оси гайки). Двигай силу вдоль ключа и следи за [[M]]. |
| Posuň sílu až na konec klíče, [[r]] = 2 m (tahem za kroužek nebo posuvníkem „Vzdálenost od osy“) | Сдвинь силу на самый конец ключа, [[r]] = 2 м (кружком или ползунком «Расстояние от оси») |
| Posuň ji co nejblíž k ose, [[r]] = 0,5 m, a porovnej oba momenty | Сдвинь её как можно ближе к оси, [[r]] = 0,5 м, и сравни оба момента |
| Vzdálenost od osy [[r]] = ${num(r)} m | Расстояние от оси [[r]] = ${num(r)} м |
| Jednotka momentu je kNm (kilonewtonmetr). Síla tu působí kolmo ke klíči, takže rameno je prostě vzdálenost od osy. | Единица момента — кН·м (килоньютон-метр). Здесь сила перпендикулярна ключу, поэтому плечо — просто расстояние от оси. |
| Stejná síla, čtyřikrát kratší rameno → čtyřikrát menší moment: 20 kNm na konci, 5 kNm u osy. Proto se matice povoluje dlouhým klíčem. | Та же сила, плечо вчетверо короче → момент вчетверо меньше: 20 кН·м на конце, 5 кН·м у оси. Поэтому гайку откручивают длинным ключом. |

### PerpendicularArm

| čeština | русский |
|---|---|
| Šikmá síla na konci klíče a kolmé rameno | Наклонная сила на конце ключа и её плечо |
| Teď síla tlačí na konec klíče šikmo. Rameno [[r_⊥]] není délka klíče, ale **kolmá** (nejkratší) vzdálenost osy od nositelky síly – zelená čárkovaná úsečka. | Теперь сила давит на конец ключа наклонно. Плечо [[r_⊥]] — не длина ключа, а **кратчайшее** (по перпендикуляру) расстояние от оси до линии действия силы — зелёный пунктирный отрезок. |
| Natoč sílu **kolmo** ke klíči, α = 90° (tahem za kroužek nebo posuvníkem „Úhel síly ke klíči“), a nech ji tam | Поверни силу **перпендикулярно** ключу, α = 90° (кружком или ползунком «Угол силы к ключу»), и оставь её так |
| Natoč ji skoro **podél** klíče (α = 15° nebo 165°) a sleduj rameno [[r_⊥]] | Поверни её почти **вдоль** ключа (α = 15° или 165°) и следи за плечом [[r_⊥]] |
| Úhel síly ke klíči α = ${num(alpha, 0)}° | Угол силы к ключу α = ${num(alpha, 0)}° |
| Rameno se rovná délce klíče (2 m) jen při α = 90° – tehdy je moment největší. | Плечо равно длине ключа (2 м) только при α = 90° — тогда момент наибольший. |
| Čím víc síla míří podél klíče, tím kratší je rameno – a menší moment. Kdyby nositelka procházela osou, rameno i moment by byly 0. | Чем больше сила направлена вдоль ключа, тем короче плечо — и меньше момент. Если бы линия действия проходила через ось, плечо и момент были бы равны 0. |
| 📖 Jak spočítat rameno šikmé síly? → Trigonometrie: rameno = r · sin α | 📖 Как найти плечо наклонной силы? → Тригонометрия: плечо = r · sin α |

### Door

| čeština | русский |
|---|---|
| Dveře viděné shora, pant vlevo ve zdi | Дверь сверху, петля слева в стене |
| zeď | стена |
| pant | петля |
| Dveře při pohledu shora, otáčejí se kolem pantu. Tlačíš pořád stejně velkou silou – záleží na tom, **kde**? Vyzkoušej všechna tři místa. | Дверь, вид сверху; она поворачивается вокруг петли. Толкаешь всё время с одинаковой силой — важно ли, **где**? Попробуй все три места. |
| Zatlač přímo **u pantu**: klepni na čárkovaný kroužek u dveří nebo na tlačítko „u pantu“ | Толкни прямо **у петли**: нажми на пунктирный кружок у двери или на кнопку «у петли» |
| Zatlač **uprostřed** dveří: kroužek nebo tlačítko „uprostřed“ | Толкни **посередине** двери: кружок или кнопка «посередине» |
| Zatlač **u kliky**: kroužek nebo tlačítko „u kliky“ | Толкни **у ручки**: кружок или кнопка «у ручки» |
| u pantu | у петли |
| uprostřed | посередине |
| u kliky | у ручки |
| Nic! Nositelka síly prochází pantem (osou otáčení) – rameno je 0, moment je 0. Takhle dveře neotevřeš. | Ничего! Линия действия силы проходит через петлю (ось вращения) — плечо 0, момент 0. Так дверь не открыть. |
| Dveře se pootevřou, ale málo – rameno je jen poloviční než u kliky, a moment taky. | Дверь приоткрывается, но слабо — плечо вдвое короче, чем у ручки, и момент тоже вдвое меньше. |
| Nejdelší rameno = největší moment. Proto je klika na kraji dveří, co nejdál od pantu. | Самое длинное плечо = самый большой момент. Поэтому ручка на краю двери, как можно дальше от петли. |

### Sign

| čeština | русский |
|---|---|
| Deska přišpendlená v bodě O a čtyři síly na jejích okrajích | Пластина, приколотая в точке O, и четыре силы на её краях |
| Deska je v bodě O přišpendlená a může se kolem něj otáčet. Na kterou stranu by ji otočila síla [[${forces[k].label}]]? Moment proti směru hodinových ručiček ↺ počítáme +, po směru ↻ −. | Пластина приколота булавкой в точке O и может вокруг неё вращаться. В какую сторону её повернула бы сила [[${forces[k].label}]]? Момент против часовой стрелки ↺ считаем со знаком +, по часовой ↻ — со знаком −. |
| Urči smysl otáčení postupně pro všechny čtyři síly – tlačítkem „↺ …“ nebo „↻ …“ (${minOf(k, forces.size)}/${forces.size}) | Определи направление вращения по очереди для всех четырёх сил — кнопкой «↺ …» или «↻ …» (${minOf(k, forces.size)}/${forces.size}) |
| ↺ proti směru hodinových ručiček (+) | ↺ против часовой стрелки (+) |
| ↻ po směru hodinových ručiček (−) | ↻ по часовой стрелке (−) |
| Představ si čárkovanou spojnici jako ručičku připíchnutou v O: síla [[${q.label}]] tlačí na její konec. Kam se pootočí? Správný smysl teď ukazuje oblouk u bodu O. | Представь пунктирную линию как стрелку часов, приколотую в O: сила [[${q.label}]] толкает её конец. Куда она повернётся? Верное направление теперь показывает дуга у точки O. |
| Výborně! Moment proti směru hodinových ručiček ↺ bereme kladně, po směru ↻ záporně – stejně jako v trenažéru. | Отлично! Момент против часовой стрелки ↺ берём со знаком плюс, по часовой ↻ — со знаком минус, как и в тренажёре. |
| Který smysl je kladný, je jen dohoda – důležité je držet se jí v celém výpočtu. | Какое направление считать положительным — лишь договорённость; главное — держаться её во всём расчёте. |

### Couple

| čeština | русский |
|---|---|
| Volant otáčený dvojicí sil | Руль, который поворачивает пара сил |
| Ruce na volantu: dvě stejně velké síly opačného smyslu na rovnoběžných nositelkách. Taková **dvojice sil** jen otáčí, nikam neposouvá – nahrazujeme ji momentem [[M]] a kreslíme obloukem. | Руки на руле: две равные по величине противоположно направленные силы на параллельных линиях действия. Такая **пара сил** только поворачивает и никуда не сдвигает — её заменяем моментом [[M]] и рисуем дугой. |
| Klepni na „Otoč volantem“ – dvojice volant roztočí | Нажми «Повернуть руль» — пара сил повернёт руль |
| Klepni na „Nahraď momentem M“ | Нажми «Заменить моментом M» |
| Klepni postupně na „bod vlevo“, „střed“ i „bod vpravo“ a porovnej moment dvojice k bodu P (${tried.size}/3) | Нажми по очереди «точка слева», «центр» и «точка справа» и сравни момент пары относительно точки P (${tried.size}/3) |
| Otoč volantem | Повернуть руль |
| Ukaž zase síly | Снова показать силы |
| Nahraď momentem M | Заменить моментом M |
| Bod P, ke kterému moment dvojice počítáme: | Точка P, относительно которой считаем момент пары: |
| bod vlevo | точка слева |
| střed | центр |
| bod vpravo | точка справа |
| Ke každému bodu vyjde stejně: [[M]] = [[F]] · [[d]] = 10 · 0,5 = 5 kNm, kde [[d]] je vzdálenost nositelek obou sil. | Относительно любой точки выходит одно и то же: [[M]] = [[F]] · [[d]] = 10 · 0,5 = 5 кН·м, где [[d]] — расстояние между линиями действия сил. |
| Proto moment dvojice přičítáš rovnou jako [[M]], bez ramene – ať počítáš ke kterémukoli bodu. Do součtu sil dvojice nepřispívá: její síly se navzájem vyruší. | Поэтому момент пары прибавляешь сразу как [[M]], без плеча, — относительно какой бы точки ты ни считал. В сумму сил пара ничего не добавляет: её силы уравновешивают друг друга. |

### Check

| čeština | русский |
|---|---|
| Nosník se třemi silami a bodem A | Балка с тремя силами и точкой A |
| Která síla má k bodu A největší moment? | У какой силы самый большой момент относительно точки A? |
| [[F_1]] | [[F_1]] |
| [[F_1]] je největší síla, ale jak dlouhé má rameno? Spočítej [[F]] · [[r]] pro [[F_1]] i [[F_2]]. | [[F_1]] — самая большая сила, но какое у неё плечо? Посчитай [[F]] · [[r]] для [[F_1]] и [[F_2]]. |
| [[F_2]] | [[F_2]] |
| [[F_3]] | [[F_3]] |
| [[F_3]] je sice nejdál, ale míří podél nosníku – její nositelka prochází bodem A. Jaké má rameno? | [[F_3]] дальше всех, но направлена вдоль балки — её линия действия проходит через точку A. Какое у неё плечо? |
| Moment = síla × rameno, tedy kolmá vzdálenost bodu A od nositelky síly. Vzdálenost po nosníku je ramenem jen u sil kolmých k nosníku. | Момент = сила × плечо, то есть расстояние по перпендикуляру от точки A до линии действия силы. Расстояние вдоль балки — плечо только для сил, перпендикулярных балке. |
| [[F_2]]: 4 · 3 = 12 kNm, [[F_1]]: 10 · 1 = 10 kNm. Nositelka [[F_3]] prochází bodem A – rameno 0, moment 0. | [[F_2]]: 4 · 3 = 12 кН·м, [[F_1]]: 10 · 1 = 10 кН·м. Линия действия [[F_3]] проходит через точку A — плечо 0, момент 0. |
| Moment síly | Момент силы |
| Jak moc síla otáčí: síla × rameno. | Насколько сила поворачивает: сила × плечо. |

## Lekce 5: Rovnováha otáčení / Равновесие моментов

### Seesaw

| čeština | русский |
|---|---|
| Houpačka se dvěma závažími | Качели с двумя грузами |
| Na houpačce leží závaží 6 kN a poloviční závaží 3 kN. ΣM je **součet momentů** obou závaží k ose houpačky (↺ +, ↻ −). Kam musí lehčí závaží, aby houpačka zůstala vodorovně? | На качелях лежат груз 6 кН и вдвое меньший груз 3 кН. ΣM — **сумма моментов** обоих грузов относительно оси качелей (↺ +, ↻ −). Куда поставить лёгкий груз, чтобы качели встали горизонтально? |
| Posuň závaží 3 kN (tahem za kroužek nad ním nebo tlačítky „0,5 m“ … „2,5 m“) tam, kde vyjde ΣM = 0 | Передвинь груз 3 кН (кружком над ним или кнопками «0,5 м» … «2,5 м») туда, где получится ΣM = 0 |
| 0,5 m | 0,5 м |
| 1 m | 1 м |
| 1,5 m | 1,5 м |
| 2 m | 2 м |
| 2,5 m | 2,5 м |
| Rovnováha! Poloviční síla potřebuje dvojnásobné rameno: 6 · 1 = 3 · 2. Momenty obou závaží jsou stejně velké a opačné. | Равновесие! Вдвое меньшей силе нужно вдвое большее плечо: 6 · 1 = 3 · 2. Моменты обоих грузов равны по величине и противоположны. |
| Teď je to moc daleko – převážil moment lehčího závaží a houpačka se naklonila na druhou stranu. | Теперь слишком далеко — перевесил момент лёгкого груза, и качели наклонились в другую сторону. |

### ThreeLocks

| čeština | русский |
|---|---|
| Těleso, které se může posunout a otočit | Тело, которое может сдвигаться и поворачиваться |
| Těleso v rovině se může pohnout třemi způsoby: posunout vodorovně, posunout svisle a otočit. Zamkni postupně všechny tři pohyby a sleduj, která podmínka každý z nich hlídá. | Тело на плоскости может двигаться тремя способами: сдвинуться по горизонтали, сдвинуться по вертикали и повернуться. Запри по очереди все три движения и следи, какое условие отвечает за каждое. |
| Zamkni **posun vodorovně** – tlačítko „Zamknout ⟷“ | Запри **сдвиг по горизонтали** — кнопка «Запереть ⟷» |
| Zamkni **posun svisle** – tlačítko „Zamknout ↕“ | Запри **сдвиг по вертикали** — кнопка «Запереть ↕» |
| Zamkni **otáčení** – tlačítko „Zamknout ↺“ | Запри **поворот** — кнопка «Запереть ↺» |
| ✓ ⟷ zamčeno | ✓ ⟷ заперто |
| Zamknout ⟷ | Запереть ⟷ |
| ✓ ↕ zamčeno | ✓ ↕ заперто |
| Zamknout ↕ | Запереть ↕ |
| ✓ ↺ zamčeno | ✓ ↺ заперто |
| Zamknout ↺ | Запереть ↺ |
| Těleso je v **rovnováze**, když se neposouvá ani neotáčí: vodorovné síly, svislé síly i momenty se vyruší. Tři možné pohyby = tři **rovnice rovnováhy**. | Тело в **равновесии**, когда оно не сдвигается и не поворачивается: горизонтальные силы, вертикальные силы и моменты уравновешены. Три возможных движения = три **уравнения равновесия**. |

### ChoosePoint

| čeština | русский |
|---|---|
| Prostý nosník s reakcemi a volbou bodu | Простая балка с реакциями и выбором точки |
| Modré šipky jsou **neznámé síly**, kterými podpory drží nosník (víc v lekcích 6–7). Rovnici ΣM = 0 smíš napsat k libovolnému bodu – ke kterému v ní zbude nejméně neznámých? | Синие стрелки — **неизвестные силы**, которыми опоры держат балку (подробнее в уроках 6–7). Уравнение ΣM = 0 можно записать относительно любой точки — относительно какой в нём останется меньше всего неизвестных? |
| Klepni na bod **A** (čárkovaný kroužek na nosníku nebo tlačítko „bod A“) – ukáže se rovnice ΣM = 0 k bodu A | Нажми на точку **A** (пунктирный кружок на балке или кнопка «точка A») — появится уравнение ΣM = 0 относительно A |
| Klepni na střed **S** (kroužek nebo tlačítko „střed S“) | Нажми на середину **S** (кружок или кнопка «середина S») |
| Klepni na bod **B** (kroužek nebo tlačítko „bod B“) | Нажми на точку **B** (кружок или кнопка «точка B») |
| bod A | точка A |
| střed S | середина S |
| bod B | точка B |
| Nositelky [[R_Ax]] i [[R_Ay]] procházejí bodem A – mají rameno 0 a z rovnice vypadnou. Zbyla jediná neznámá [[R_B]] a hned ji spočítáš. | Линии действия [[R_Ax]] и [[R_Ay]] проходят через A — их плечо 0, и из уравнения они выпадают. Осталась одна неизвестная [[R_B]], её сразу находишь. |
| Dvě neznámé v jedné rovnici – z ní samotné nic nespočítáš. Výhodnější je bod, kterým prochází nositelka neznámé síly: ta k němu má rameno 0 a z rovnice vypadne. | Две неизвестные в одном уравнении — из него одного ничего не найти. Выгоднее точка, через которую проходит линия действия неизвестной силы: её плечо там 0, и она выпадает из уравнения. |
| Nositelka [[R_B]] prochází bodem B, takže [[R_B]] z rovnice vypadne. Zbyla jen [[R_Ay]]. | Линия действия [[R_B]] проходит через B, поэтому [[R_B]] из уравнения выпадает. Осталась только [[R_Ay]]. |
| Vodorovná [[R_Ax]] leží v ose nosníku: její nositelka prochází body A, S i B, proto k nim má rameno 0. | Горизонтальная [[R_Ax]] лежит на оси балки: её линия действия проходит через A, S и B, поэтому её плечо относительно них равно 0. |

### Check

| čeština | русский |
|---|---|
| Prostý nosník se silou F ve 3 m od podpory A | Простая балка с силой F в 3 м от опоры A |
| Ke kterému bodu napíšeš ΣM = 0, aby z rovnice hned vyšla svislá síla v podpoře A, [[R_Ay]]? | Относительно какой точки записать ΣM = 0, чтобы из уравнения сразу получить вертикальную силу в опоре A, [[R_Ay]]? |
| k bodu A | относительно точки A |
| Nositelka [[R_Ay]] prochází bodem A, k němu má rameno 0. Objeví se pak [[R_Ay]] v rovnici vůbec? | Линия действия [[R_Ay]] проходит через точку A, её плечо там 0. Войдёт ли тогда [[R_Ay]] в уравнение вообще? |
| k bodu B | относительно точки B |
| ke středu nosníku | относительно середины балки |
| Ke středu má rameno [[R_Ay]] i síla v podpoře B – v rovnici by zůstaly dvě neznámé. Který bod jednu z nich vyřadí? | Относительно середины плечо есть и у [[R_Ay]], и у силы в опоре B — в уравнении останутся две неизвестные. Какая точка уберёт одну из них? |
| K bodu B: síla v podpoře B jím prochází, má rameno 0 a z rovnice vypadne. Zbude −[[R_Ay]]·4 + 6·1 = 0, tedy [[R_Ay]] = 1,5 kN. | Относительно B: сила в опоре B проходит через эту точку, её плечо 0, и из уравнения она выпадает. Остаётся −[[R_Ay]]·4 + 6·1 = 0, то есть [[R_Ay]] = 1,5 кН. |
| Rovnováha otáčení | Равновесие моментов |
| ΣM = 0 – a jak vybrat výhodný bod. | ΣM = 0 — и как выбрать удобную точку. |

## Lekce 6: Vazby brání pohybu / Связи ограничивают движение

### PushTest

| čeština | русский |
|---|---|
| Nosník na podpoře, zkouška pohybu | Балка на опоре, проверка движения |
| Zkus nosníkem pohnout. Pohyb, který podpora nedovolí, zastaví silou – **reakcí** (modrá šipka); ta míří vždy proti tvému pokusu. | Попробуй сдвинуть балку. Движение, которое опора не допускает, она останавливает силой — **реакцией** (синяя стрелка); реакция всегда направлена против твоей попытки. |
| U podpory „pevná kloubová“ klepni postupně na „⟷ posunout“, „↑ zvednout“ a „↺ natočit“ (${n(0)}/3) | Для опоры «шарнирно-неподвижная» нажми по очереди «⟷ сдвинуть», «↑ поднять» и «↺ повернуть» (${n(0)}/3) |
| Přepni na „posuvná kloubová“ a vyzkoušej znovu všechny tři pohyby (${n(1)}/3) | Переключи на «шарнирно-подвижная» и снова попробуй все три движения (${n(1)}/3) |
| Přepni na „vetknutí“ a vyzkoušej všechny tři pohyby (${n(2)}/3) | Переключи на «жёсткая заделка» и попробуй все три движения (${n(2)}/3) |
| pevná kloubová | шарнирно-неподвижная |
| posuvná kloubová | шарнирно-подвижная |
| vetknutí | жёсткая заделка |
| ⟷ posunout | ⟷ сдвинуть |
| ↑ zvednout | ↑ поднять |
| ↺ natočit | ↺ повернуть |
| Zakázané pohyby: $nBlocked → reakce: $nBlocked | Запрещено движений: $nBlocked → реакций: $nBlocked |
| Nejde to. Podpora drží nosník i proti zvednutí – jako kolejničky šuplík: jezdí sem a tam, ale nahoru ho nevytáhneš. | Не получается. Опора держит балку и от подъёма — как направляющие держат ящик стола: он ездит туда-сюда, а вверх его не вынуть. |
| Nejde to – zeď nosníku natočit nedovolí. Tady reakcí není síla, ale **moment** [[M]], a míří proti tvému pokusu. | Не получается — стена не даёт балке повернуться. Здесь реакция — не сила, а **момент** [[M]], и направлен он против твоей попытки. |
| Nejde to – podpora působí zpátky, **proti** tvému pokusu. Tady vzniká reakce. | Не получается — опора действует в ответ, **против** твоей попытки. Здесь возникает реакция. |
| Jde to – tenhle pohyb podpora dovolí, reakce tu není. | Получается — это движение опора допускает, реакции здесь нет. |
| Pevná kloubová podpora: 2 reakce. Posuvná kloubová: 1. Vetknutí: 3 (dvě síly a moment). Podporám se společně říká **vazby**. | Шарнирно-неподвижная опора: 2 реакции. Шарнирно-подвижная: 1. Жёсткая заделка (vetknutí): 3 (две силы и момент). Все опоры вместе называют **связями** (vazby). |

### MatchSets

| čeština | русский |
|---|---|
| Značka podpory | Условное обозначение опоры |
| Pravidlo: **každý zakázaný pohyb = jedna reakce**. Jaké reakce má tahle podpora? | Правило: **каждое запрещённое движение = одна реакция**. Какие реакции у этой опоры? |
| Vyber správné reakce **pevné kloubové** podpory (na obrázku) | Выбери верные реакции **шарнирно-неподвижной** опоры (на рисунке) |
| Klepni na „Další podpora →“ a vyber reakce **posuvné kloubové** podpory | Нажми «Следующая опора →» и выбери реакции **шарнирно-подвижной** опоры |
| Znovu klepni na „Další podpora →“ a vyber reakce **vetknutí** | Снова нажми «Следующая опора →» и выбери реакции **жёсткой заделки** |
| [[R_Ax]], [[R_Ay]] | [[R_Ax]], [[R_Ay]] |
| jen [[R_A]] (kolmo k podložce) | только [[R_A]] (перпендикулярно опорной поверхности) |
| [[R_Ax]], [[R_Ay]], [[M_A]] | [[R_Ax]], [[R_Ay]], [[M_A]] |
| Ano – pevná kloubová podpora nedovolí posun vodorovně ani svisle, natočení ale dovolí. | Да — шарнирно-неподвижная опора не допускает смещения ни по горизонтали, ни по вертикали, но поворот допускает. |
| Ano – válečky dovolí pohyb podél podložky (plochy, po které jezdí) i natočení. Brání jen pohybu kolmo k podložce. | Да — катки допускают сдвиг вдоль опорной поверхности (по ней они катятся) и поворот. Препятствуют только смещению перпендикулярно опорной поверхности. |
| Ano – vetknutí nedovolí žádný pohyb: dvě síly a moment. | Да — жёсткая заделка не допускает никакого движения: две силы и момент. |
| Další podpora → | Следующая опора → |
| Zkus nosníkem v duchu posunout, zvednout a natočit. Co z toho podpora nedovolí? | Мысленно попробуй сдвинуть, поднять и повернуть балку. Что из этого опора не допустит? |
| Všimni si válečků: kterým směrem se po nich nosník volně posune? | Обрати внимание на катки: в каком направлении балка по ним свободно сдвигается? |
| Nosník je zapuštěný do zdi. Může se v ní natočit? | Балка заделана в стену. Может ли она в ней повернуться? |

### WhichRotation

| čeština | русский |
|---|---|
| Tři druhy podpor | Три вида опор |
| Která podpora (vazba) nedovolí nosníku se **natočit**? | Какая опора (связь, vazba) не даёт балке **повернуться**? |
| 1 – pevná kloubová podpora | 1 — шарнирно-неподвижная опора |
| Je to kloub – a kloub natočení dovolí. Proto nemá reakční moment. | Это шарнир — а шарнир поворот допускает. Поэтому реактивного момента нет. |
| 2 – posuvná kloubová podpora | 2 — шарнирно-подвижная опора |
| Válečky a kloub: dovolí pohyb podél podložky i natočení. Brání jen pohybu kolmo k podložce. | Катки и шарнир: допускают сдвиг вдоль опорной поверхности и поворот. Препятствуют только смещению перпендикулярно ей. |
| 3 – vetknutí | 3 — жёсткая заделка |
| Vetknutí drží nosník pevně ve zdi – proto má kromě dvou sil i reakční moment [[M]]. | Жёсткая заделка неподвижно держит балку в стене — поэтому кроме двух сил у неё есть ещё реактивный момент [[M]]. |
| Vazby brání pohybu | Связи ограничивают движение |
| Pevná a posuvná kloubová podpora, vetknutí – co dovolí a co ne. | Шарнирно-неподвижная и шарнирно-подвижная опоры, жёсткая заделка — что допускают и что нет. |

## Lekce 7: Uvolnění / Освобождение от связей

### CutItFree

| čeština | русский |
|---|---|
| Nosník na dvou podporách se silou F | Балка на двух опорах с силой F |
| Podpora drží nosník jen tím, že na něj **působí silou**. Odstraň ji a sleduj, co se stane – pak ji nahraď její reakcí. | Опора держит балку только тем, что **действует на неё силой**. Убери опору и посмотри, что будет, — а потом замени её реакцией. |
| Klepni na „Odstraň podporu A“ | Нажми «Убрать опору A» |
| Klepni na „Nahraď podporu A reakcemi“ | Нажми «Заменить опору A реакциями» |
| Klepni na „Odstraň podporu B“ | Нажми «Убрать опору B» |
| Klepni na „Nahraď podporu B reakcí“ | Нажми «Заменить опору B реакцией» |
| Nahraď podporu A reakcemi | Заменить опору A реакциями |
| Nahraď podporu B reakcí | Заменить опору B реакцией |
| Odstraň podporu A | Убрать опору A |
| Odstraň podporu B | Убрать опору B |
| Bez podpory se nosník otočí a spadne. Chybí mu síla, kterou na něj podpora působila – vrať mu ji tlačítkem „Nahraď…“. | Без опоры балка поворачивается и падает. Ей не хватает силы, с которой на неё действовала опора, — верни её кнопкой «Заменить…». |
| Podpory jsou pryč, a nosník přesto drží: modré šipky (reakce) dělají přesně to, co dělaly podpory. Téhle záměně se říká **uvolnění**. | Опор больше нет, а балка держится: синие стрелки (реакции) делают ровно то же, что делали опоры. Такая замена называется **освобождение от связей** (uvolnění). |
| [[R_Ax]] tu vyjde 0, protože do strany nic netlačí. Kreslíme ji ale vždy – pevná kloubová podpora brání i vodorovnému posunu. | [[R_Ax]] здесь получится 0, потому что вбок ничего не давит. Но рисуем её всегда — шарнирно-неподвижная опора препятствует и горизонтальному смещению. |
| Drží! Reakce nahradila podporu. Udělej totéž s druhou podporou. | Держится! Реакция заменила опору. Сделай то же самое со второй опорой. |

### WhoHolds

| čeština | русский |
|---|---|
| Nosník podepřený podporou, rukou nebo silou | Балка на опоре, на руке или на силе |
| ruka | рука |
| Nosníku je jedno, **kdo** ho drží – podpora, ruka, nebo jen síla nakreslená šipkou. Záleží jen na tom, jak velká ta síla je a kam míří. | Балке всё равно, **кто** её держит — опора, рука или просто сила, нарисованная стрелкой. Важно только, какой величины эта сила и куда она направлена. |
| Klepni na „Ruka“ – změnila se síla [[R_B]]? | Нажми «Рука» — изменилась ли сила [[R_B]]? |
| Klepni na „Jen síla“ | Нажми «Только сила» |
| Podpora | Опора |
| Ruka | Рука |
| Jen síla | Только сила |
| Pokaždé [[R_B]] = 4 kN. Proto smíme podporu na obrázku „vystřihnout“ a nakreslit místo ní reakci – výpočet se nezmění. | Каждый раз [[R_B]] = 4 кН. Поэтому на рисунке опору можно «вырезать» и нарисовать вместо неё реакцию — расчёт не изменится. |

### ActionReaction

| čeština | русский |
|---|---|
| Nosník a podpora tlačí na sebe navzájem | Балка и опора давят друг на друга |
| podpora → nosník: 6 kN | опора → балка: 6 кН |
| nosník → podpora: 6 kN | балка → опора: 6 кН |
| Nosník tlačí na podporu dolů a podpora na nosník **stejně velkou** silou nahoru. Počítáme nosník, proto kreslíme jen sílu, která působí **na nosník** (modrou). | Балка давит на опору вниз, а опора на балку — **такой же по величине** силой вверх. Мы рассчитываем балку, поэтому рисуем только ту силу, которая действует **на балку** (синюю). |
| Klepni na „Rozpoj nosník a podporu“ a prohlédni si obě síly | Нажми «Разъединить балку и опору» и рассмотри обе силы |
| Spoj zpátky | Соединить обратно |
| Rozpoj nosník a podporu | Разъединить балку и опору |
| Tomu se říká **akce a reakce**: dvě stejně velké, opačné síly – každá ale působí na **jiné** těleso. | Это называют **действием и противодействием** (akce a reakce): две равные по величине противоположные силы — но каждая действует на **своё** тело. |

### GuessSense

| čeština | русский |
|---|---|
| Nosník s převislým koncem, směr reakce v bodě A si volíš | Балка с консолью, направление реакции в точке A выбираешь сам |
| Kam míří reakce v A, předem nevíš. Smysl si proto zvol – výpočet ti řekne, jestli sis tipl dobře. | Куда направлена реакция в A, заранее неизвестно. Поэтому направление (smysl) выбери сам — расчёт покажет, угадал ли ты. |
| Zvol smysl reakce: klepni na „[[R_A]] nahoru ↑“, nebo „[[R_A]] dolů ↓“ | Выбери направление реакции: нажми «[[R_A]] вверх ↑» или «[[R_A]] вниз ↓» |
| Klepni na „Spočítej reakce“ | Нажми «Посчитать реакции» |
| [[R_A]] nahoru ↑ | [[R_A]] вверх ↑ |
| [[R_A]] dolů ↓ | [[R_A]] вниз ↓ |
| Spočítej reakce | Посчитать реакции |
| Momenty k bodu B, ↺ je plus: | Моменты относительно B, ↺ — плюс: |
| Minus není chyba! Jen říká, že reakce ve skutečnosti míří **opačně**, než sis zvolil: podpora A táhne nosník dolů (brání mu v nadzvednutí). | Минус — не ошибка! Он только говорит, что на самом деле реакция направлена **противоположно** выбранному: опора A тянет балку вниз (не даёт ей приподняться). |
| Vyšlo kladně – tipl sis dobře, podpora A táhne nosník dolů. Kdybys zvolil nahoru, vyšlo by −4 kN: stejná síla, jen s opačným znaménkem. | Получилось положительное число — ты угадал: опора A тянет балку вниз. Если бы ты выбрал «вверх», вышло бы −4 кН: та же сила, только с другим знаком. |
| V trenažéru proto kreslíme všechny reakce v kladném smyslu (→ ↑ ↺). Znaménko výsledku pak řekne, kam opravdu míří. | Поэтому в тренажёре все реакции рисуем в положительном направлении (→ ↑ ↺). Знак результата покажет, куда они направлены на самом деле. |

### Check

| čeština | русский |
|---|---|
| Posuvná kloubová podpora B | Шарнирно-подвижная опора B |
| Co nakreslíš místo posuvné kloubové podpory B? | Что нарисуешь вместо шарнирно-подвижной опоры B? |
| dvě síly [[R_Bx]] a [[R_By]] | две силы [[R_Bx]] и [[R_By]] |
| Válečky se kutálejí vodorovně. Může podpora v tom směru bránit pohybu? | Катки катятся по горизонтали. Может ли опора препятствовать движению в этом направлении? |
| jednu sílu [[R_B]] kolmo k podložce | одну силу [[R_B]] перпендикулярно опорной поверхности |
| nic – podpora prostě zmizí | ничего — опора просто исчезает |
| A co pak drží pravý konec nahoře? Vzpomeň si na padající nosník. | А что тогда будет держать правый конец? Вспомни падающую балку. |
| Posuvná kloubová podpora brání jen posunu kolmo k podložce – proto jediná síla [[R_B]]. Takhle uvolníš každou vazbu: místo ní nakreslíš právě ty reakce, kterými brání pohybu. | Шарнирно-подвижная опора препятствует только смещению перпендикулярно опорной поверхности — поэтому одна сила [[R_B]]. Так освобождают от любой связи: вместо неё рисуют именно те реакции, которыми она препятствует движению. |
| Uvolnění | Освобождение от связей |
| Proč smíme podporu nahradit silami – reakcemi. | Почему опору можно заменить силами — реакциями. |

## Lekce 8: Spojité zatížení / Распределённая нагрузка

### Resultant

| čeština | русский |
|---|---|
| Spojité zatížení na nosníku | Распределённая нагрузка на балке |
| Spojité zatížení je rozložené po délce jako sníh na střeše: **q** říká, kolik kN připadá na každý metr, **ℓ** je zatížená délka. Celé ho nahradí jediná síla – **výslednice** Q. | Распределённая нагрузка лежит по всей длине, как снег на крыше: **q** — сколько кН приходится на каждый метр, **ℓ** — длина нагруженного участка. Всю её заменяет одна сила — **равнодействующая** (výslednice) Q. |
| Změň **q** – zatáhni za prostřední kroužek na obrázku nebo za posuvník „q“ | Измени **q** — потяни за средний кружок на рисунке или за ползунок «q» |
| Změň délku **ℓ** – zatáhni za pravý kroužek nebo za posuvník „ℓ“ | Измени длину **ℓ** — потяни за правый кружок или за ползунок «ℓ» |
| Klepni na „Nahradit výslednicí Q“ | Нажми «Заменить равнодействующей Q» |
| Zpátky na zatížení (měnit q a ℓ) | Обратно к нагрузке (менять q и ℓ) |
| Nahradit výslednicí Q | Заменить равнодействующей Q |
| U **rovnoměrného** zatížení (všude stejné q) je výslednice Q = q · ℓ – plocha obdélníku zatížení. Působí **uprostřed** zatížené délky. | У **равномерной** нагрузки (q везде одинаково) равнодействующая Q = q · ℓ — это площадь прямоугольника нагрузки. Приложена она **посередине** нагруженного участка. |
| Ještě se vrať tlačítkem „Zpátky na zatížení“ a změň q i ℓ. | Вернись кнопкой «Обратно к нагрузке» и измени ещё q и ℓ. |

### PerMetre

| čeština | русский |
|---|---|
| Knihy na polici, 2 kN na každý metr | Книги на полке, 2 кН на каждый метр |
| q = 2 kN/m znamená: **na každý metr** police připadají 2 kN knih. Kolik kN je to na celé zatížené délce? | q = 2 кН/м означает: **на каждый метр** полки приходится 2 кН книг. Сколько кН получится на всём нагруженном участке? |
| Vyber zatíženou délku – klepni na jedno z tlačítek „1 m“ až „4 m“ | Выбери длину нагруженного участка — нажми одну из кнопок от «1 м» до «4 м» |
| Klepni na jinou délku a porovnej, jak se změnilo Q | Нажми другую длину и сравни, как изменилась Q |
| 1 m | 1 м |
| 2 m | 2 м |
| 3 m | 3 м |
| 4 m | 4 м |
| q ještě není síla – je to síla **na metr** (kN/m). Sílu Q v kN dostaneš, až když q vynásobíš zatíženou délkou. | q — это ещё не сила, а сила **на метр** (кН/м). Силу Q в кН получишь, только умножив q на длину нагруженного участка. |

### CheckQ

| čeština | русский |
|---|---|
| Spojité zatížení 2 kN/m na délce 3 m | Распределённая нагрузка 2 кН/м на длине 3 м |
| Čím tohle spojité zatížení nahradíš? | Чем заменишь эту распределённую нагрузку? |
| Q = 6 kN uprostřed zatížení | Q = 6 кН посередине нагрузки |
| Q = 2 kN uprostřed zatížení | Q = 2 кН посередине нагрузки |
| 2 kN připadají jen na jeden metr. Kolik metrů je zatíženo? | 2 кН приходится только на один метр. Сколько метров нагружено? |
| Q = 6 kN na konci zatížení | Q = 6 кН на конце нагрузки |
| Zatížení je všude stejné, takže vlevo od Q ho musí být stejně jako vpravo. Kde bys rovnoměrně naloženou polici podepřel jedním prstem? | Нагрузка везде одинакова, значит, слева от Q её должно быть столько же, сколько справа. Где бы ты подпёр равномерно загруженную полку одним пальцем? |
| Q = q · ℓ = 2 · 3 = 6 kN. Působí uprostřed zatížené délky, tedy 1,5 m od jejího začátku. | Q = q · ℓ = 2 · 3 = 6 кН. Приложена посередине нагруженного участка, то есть в 1,5 м от его начала. |
| Spojité zatížení | Распределённая нагрузка |
| Rovnoměrné zatížení: Q = q · ℓ uprostřed zatížené délky. | Равномерная нагрузка: Q = q · ℓ посередине нагруженного участка. |

## Lekce 9: Kloub a statická určitost / Шарнир и статическая определимость

### HingeTest

| čeština | русский |
|---|---|
| Nosník s vnitřním kloubem K | Балка с промежуточным шарниром K |
| tuhý spoj | жёсткое соединение |
| **Vnitřní kloub** K spojuje dvě části nosníku jako pant dveří: části se v něm mohou vůči sobě volně natočit. Odeber podporu C a porovnej kloub s tuhým spojem. | **Промежуточный шарнир** K (vnitřní kloub) соединяет две части балки, как дверная петля: части могут свободно поворачиваться в нём друг относительно друга. Убери опору C и сравни шарнир с жёстким соединением. |
| Nech zvolený „kloub“ a klepni na „Odeber podporu C“ | Оставь выбранным «шарнир» и нажми «Убрать опору C» |
| Přepni na „tuhý spoj“ a znovu klepni na „Odeber podporu C“ | Переключи на «жёсткое соединение» и снова нажми «Убрать опору C» |
| kloub | шарнир |
| tuhý spoj | жёсткое соединение |
| Vrať podporu C | Вернуть опору C |
| Odeber podporu C | Убрать опору C |
| Pravá část se v kloubu otočí a spadne: kloub ji podrží (přenese **sílu**), ale otáčení nezabrání (**moment** nepřenese). | Правая часть поворачивается в шарнире и падает: шарнир её удерживает (передаёт **силу**), но повороту не мешает (**момент** не передаёт). |
| Tuhý spoj přenese i moment – nosník drží, pravá část se jen nepatrně prohne. | Жёсткое соединение передаёт и момент — балка держится, правая часть лишь слегка прогибается. |
| Kloub moment nepřenese. Proto platí [[M_K]] = 0: součet momentů všech sil na **jedné** části nosníku k bodu K je nula. | Шарнир не передаёт момент. Поэтому [[M_K]] = 0: сумма моментов всех сил, действующих на **одну** часть балки, относительно точки K равна нулю. |

### OnePart

| čeština | русский |
|---|---|
| Uvolněný nosník s kloubem, momenty k bodu K | Освобождённая балка с шарниром, моменты относительно K |
| Rovnice kloubu [[M_K]] = 0: sečti momenty k bodu K, ale jen od sil na **jedné** části nosníku. Prohlédni si rovnici pro každou část. | Уравнение шарнира [[M_K]] = 0: сложи моменты относительно точки K, но только от сил на **одной** части балки. Посмотри уравнение для каждой части. |
| Klepni na „levá část“ a přečti si rovnici | Нажми «левая часть» и прочитай уравнение |
| Klepni na „pravá část“ a porovnej, kolik je v rovnici neznámých | Нажми «правая часть» и сравни, сколько в уравнении неизвестных |
| levá část | левая часть |
| pravá část | правая часть |
| Momenty ↺ bereme kladně. Síly, jejichž nositelka prochází bodem K (síly v kloubu a [[R_Ax]] v ose nosníku), mají rameno 0 – v rovnici proto nejsou. | Моменты ↺ считаем положительными. У сил, линия действия которых проходит через точку K (силы в шарнире и [[R_Ax]] на оси балки), плечо равно 0 — поэтому в уравнении их нет. |
| Vpravo je jen jedna neznámá ([[R_C]]), vlevo dvě – proto ber pravou část. Tahle rovnice je **navíc** ke třem rovnicím pro celý nosník. | Справа только одна неизвестная ([[R_C]]), слева две — поэтому бери правую часть. Это уравнение — **дополнительное** к трём уравнениям для всей балки. |

### CountUnknowns

| čeština | русский |
|---|---|
| Nosník s kloubem: počítání neznámých reakcí | Балка с шарниром: подсчёт неизвестных реакций |
| Rovnic máme r = 4: tři pro celý nosník a jednu za kloub. Uvolni podpory: každá modrá šipka je jedna neznámá reakce, jejich počet označíme n. | Уравнений у нас r = 4: три для всей балки и одно за шарнир. Освободи балку от опор: каждая синяя стрелка — одна неизвестная реакция, их число обозначим n. |
| Klepni na „uvolni A“, „uvolni B“ i „uvolni C“ (${released.size}/3) | Нажми «освободить A», «освободить B» и «освободить C» (${released.size}/3) |
| Spočítej modré šipky a vyber správné n | Посчитай синие стрелки и выбери верное n |
| Kolik je neznámých reakcí (počet n)? | Сколько неизвестных реакций (число n)? |
| n = 3 | n = 3 |
| n = 4 | n = 4 |
| n = 5 | n = 5 |
| Neznámých je stejně jako rovnic a podpory nedovolí žádný pohyb – nosník je **staticky určitý**. Všechny reakce spočítáš jen z rovnic rovnováhy. | Неизвестных столько же, сколько уравнений, и опоры не допускают никакого движения — балка **статически определима**. Все реакции находятся из одних только уравнений равновесия. |
| Kdyby C byla pevná kloubová podpora (2 reakce), bylo by n = 5 > r = 4. Nosník by byl staticky neurčitý – rovnice rovnováhy by nestačily. | Если бы C была шарнирно-неподвижной опорой (2 реакции), было бы n = 5 > r = 4. Балка стала бы статически неопределимой — уравнений равновесия не хватило бы. |
| Spočítej modré šipky na obrázku: každá je jedna neznámá. Pevná kloubová podpora A má dvě. | Посчитай синие стрелки на рисунке: каждая — одна неизвестная. У шарнирно-неподвижной опоры A их две. |

### HowMany

| čeština | русский |
|---|---|
| Nosník s jedním vnitřním kloubem | Балка с одним промежуточным шарниром |
| Kolik rovnic (počet r) máš pro nosník s jedním vnitřním kloubem? | Сколько уравнений (число r) у балки с одним промежуточным шарниром? |
| 3 | 3 |
| Tři rovnice platí pro celý nosník. Nepřidává jednu i kloub? | Три уравнения — для всей балки. А не добавляет ли ещё одно шарнир? |
| 4 | 4 |
| 5 | 5 |
| Každý kloub přidá jen jednu rovnici – [[M_K]] = 0. | Каждый шарнир добавляет только одно уравнение — [[M_K]] = 0. |
| 3 rovnice pro celý nosník + [[M_K]] = 0 za kloub = 4 rovnice. Neznámé jsou tu také 4 (vetknutí 3 + posuvná kloubová podpora 1), takže n = r. | 3 уравнения для всей балки + [[M_K]] = 0 за шарнир = 4 уравнения. Неизвестных здесь тоже 4 (жёсткая заделка 3 + шарнирно-подвижная опора 1), значит, n = r. |
| Kloub a statická určitost | Шарнир и статическая определимость |
| [[M_K]] = 0 z jedné části; počet neznámých n = počet rovnic r. | [[M_K]] = 0 по одной части; число неизвестных n = числу уравнений r. |

## Slovníček / Словарик

| čeština | русский |
|---|---|
| Síla | Сила |
| To, co tlačí nebo táhne. Kreslíme ji šipkou, měříme v kN (1 kN ≈ tíha tělesa o hmotnosti 100 kg). | То, что толкает или тянет. Рисуем стрелкой, измеряем в кН (1 кН ≈ сила тяжести тела массой 100 кг). |
| Tíha G | Сила тяжести G (tíha) |
| Síla, kterou Země táhne těleso dolů. Působí na těleso samo. | Сила, с которой Земля притягивает тело вниз. Приложена к самому телу. |
| Vektor | Вектор |
| Veličina, u které záleží na velikosti, směru i smyslu – proto ji kreslíme šipkou. | То, у чего важны и величина, и наклон, и направление, — поэтому рисуем стрелкой. |
| Působiště | Точка приложения (působiště) |
| Bod, kde síla na těleso působí. | Точка, в которой сила действует на тело. |
| Nositelka | Линия действия (nositelka) |
| Přímka, na které síla leží. Po ní smíš sílu posunout – účinek na tuhé těleso se nezmění. | Прямая, на которой лежит сила. Вдоль неё силу можно сдвигать — действие на твёрдое тело не меняется. |
| Velikost, směr, smysl | Величина (velikost), наклон (směr), направление (smysl) |
| Jak velká síla je, jaký sklon má její nositelka a kam míří hrot. | Насколько велика сила (её модуль), какой наклон у её линии действия и куда смотрит остриё стрелки. |
| Složky síly [[F_x]], [[F_y]] | Составляющие силы [[F_x]], [[F_y]] (složky) |
| Vodorovná a svislá část šikmé síly. Spolu dělají totéž co celá síla. | Горизонтальная и вертикальная части наклонной силы. Вместе действуют так же, как вся сила. |
| Výslednice | Равнодействующая (výslednice) |
| Jedna síla, která nahradí několik sil se stejným účinkem. | Одна сила, заменяющая несколько сил с тем же действием. |
| Σ (součet) | Σ (сумма) |
| Σ[[F_x]] = 0 čti „součet všech vodorovných sil je nula“. | Σ[[F_x]] = 0 читается так: «сумма всех горизонтальных сил равна нулю». |
| Nosník | Балка (nosník) |
| Vodorovný trám, který nese zatížení a leží na podporách. | Горизонтальный брус, который несёт нагрузку и лежит на опорах. |
| Moment síly M | Момент силы M |
| Jak moc síla otáčí: M = F · r, v kNm (kilonewtonmetr). Kladný ↺, záporný ↻. | Насколько сила поворачивает: M = F · r, в кН·м (килоньютон-метр). Положительный ↺, отрицательный ↻. |
| Rameno r | Плечо r (rameno) |
| Kolmá vzdálenost bodu od nositelky síly – ne vzdálenost k působišti. | Кратчайшее (по перпендикуляру) расстояние от точки до линии действия силы — не расстояние до точки приложения. |
| Dvojice sil | Пара сил (dvojice sil) |
| Dvě stejně velké rovnoběžné síly opačného smyslu na různých nositelkách – jen otáčejí. | Две равные по модулю параллельные противоположно направленные силы, не лежащие на одной прямой, — только поворачивают. |
| Rovnováha | Равновесие |
| Těleso se neposouvá ani neotáčí: Σ[[F_x]] = 0, Σ[[F_y]] = 0, ΣM = 0. | Тело не смещается и не поворачивается: Σ[[F_x]] = 0, Σ[[F_y]] = 0, ΣM = 0. |
| Podpora | Опора (podpora) |
| Místo, kde nosník leží nebo je uchycený – jeden druh vazby. | Место, где балка лежит или закреплена, — один из видов связи. |
| Vazba | Связь (vazba) |
| To, co tělesu brání v pohybu (podpora, kloub, zeď). Každý pohyb, který vazba nedovolí, znamená jednu reakci. | То, что ограничивает движение тела (опора, шарнир, стена). Каждое движение, которое связь не допускает, даёт одну реакцию. |
| Reakce | Реакция (reakce) |
| Síla (nebo moment), kterou vazba působí na těleso – proti pohybu, který nedovolí. Kreslíme ji modře. | Сила (или момент), с которой связь действует на тело, — против движения, которое она не допускает. Рисуем синим. |
| Uvolnění | Освобождение от связей (uvolnění) |
| Vazby odstraníme a nahradíme je reakcemi. Na nosníku zůstanou jen zatížení a reakce. | Связи убираем и заменяем их реакциями. На балке остаются только нагрузки и реакции. |
| Akce a reakce | Действие и противодействие (akce a reakce) |
| Nosník tlačí na podporu a podpora stejně velkou silou opačně na nosník. Každá z těch sil působí na jiné těleso. | Балка давит на опору, а опора с такой же по величине силой — в обратную сторону на балку. Каждая из этих сил действует на своё тело. |
| Neznámá | Неизвестная (neznámá) |
| Reakce, jejíž velikost zatím neznáme – spočítáme ji z rovnic rovnováhy. Počet neznámých značíme n. | Реакция, величину которой мы пока не знаем, — находим её из уравнений равновесия. Число неизвестных обозначаем n. |
| Podložka | Опорная поверхность (podložka) |
| Plocha, po které jezdí válečky posuvné podpory. | Поверхность, по которой катятся катки шарнирно-подвижной опоры. |
| Pevná kloubová podpora | Шарнирно-неподвижная опора (pevná kloubová) |
| Nedovolí posun, dovolí natočení: dvě reakce, [[R_x]] a [[R_y]]. | Не допускает смещения, но допускает поворот: две реакции, [[R_x]] и [[R_y]]. |
| Posuvná kloubová podpora | Шарнирно-подвижная опора (posuvná kloubová) |
| Na válečcích: dovolí pohyb podél podložky i natočení, brání jen pohybu kolmo k podložce – jedna reakce. | На катках: допускает сдвиг вдоль опорной поверхности и поворот, препятствует только смещению перпендикулярно опорной поверхности — одна реакция. |
| Vetknutí | Жёсткая заделка (vetknutí) |
| Konec nosníku pevně zapuštěný do zdi: nedovolí posun ani natočení – tři reakce, [[R_x]], [[R_y]] a moment M. | Конец балки, неподвижно заделанный в стену: не допускает ни смещения, ни поворота — три реакции, [[R_x]], [[R_y]] и момент M. |
| Vnitřní kloub | Промежуточный шарнир (vnitřní kloub) |
| Spojuje dvě části nosníku: přenese sílu, ale ne moment. Proto [[M_K]] = 0 – jedna rovnice navíc. | Соединяет две части балки: передаёт силу, но не момент. Поэтому [[M_K]] = 0 — одно дополнительное уравнение. |
| Spojité zatížení q | Распределённая нагрузка q (spojité zatížení) |
| Zatížení rozložené po délce, v kN/m. U rovnoměrného je výslednice Q = q · ℓ uprostřed zatížené délky. | Нагрузка, распределённая по длине, в кН/м. У равномерной равнодействующая Q = q · ℓ приложена посередине нагруженного участка. |
| Staticky určitý nosník | Статически определимая балка (staticky určitý nosník) |
| Počet neznámých reakcí n = počet rovnic r (3 + jedna za každý vnitřní kloub) a podpory nedovolí žádný pohyb – reakce spočítáme jen z rovnic rovnováhy. | Число неизвестных реакций n равно числу уравнений r (3 + по одному за каждый промежуточный шарнир), и опоры не допускают никакого движения — реакции находим из одних только уравнений равновесия. |
