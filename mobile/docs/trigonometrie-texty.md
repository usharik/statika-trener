# Základy trigonometrie — texty k vyčtení / тексты для вычитки

Vygenerováno skriptem `scripts/export_trig_texts.py` z `learn/trig/` a `practice/`. Opravy pište sem nebo do kódu; 
značky: `[[F_x]]` = symbol s indexem, `**…**` = tučně, `{x}` = doplní se číslo.

## Lekce 1: Odkud se vzal sinus / Откуда взялся синус

### SameAngle

| čeština | русский |
|---|---|
| Nájezdová rampa – pravoúhlý trojúhelník s úhlem 30° | Пандус — прямоугольный треугольник с углом 30° |
| šikmá délka ${num(2 * k)} m | длина по склону ${num(2 * k)} м |
| výška ${num(k)} m | высота ${num(k)} м |
| Nájezdová rampa stoupá pod úhlem 30°. Táhni za kroužek: rampa se prodlužuje, úhel zůstává. | Пандус поднимается под углом 30°. Потяни за кружок: пандус удлиняется, а угол остаётся прежним. |
| Prodluž rampu (kroužkem nebo posuvníkem „Délka rampy“) aspoň na dvojnásobek a sleduj podíl **výška / šikmá délka** | Удлини пандус (кружком или ползунком «Длина пандуса») хотя бы вдвое и следи за отношением **высота / длина по склону** |
| Zkrať ji zpátky na nejkratší – změnil se podíl? | Укороти его обратно до самого короткого — изменилось ли отношение? |
| Délka rampy | Длина пандуса |
| výška / šikmá délka | высота / длина по склону |
| Trojúhelníky se stejnými úhly jsou **podobné**: všechny strany se zvětší stejněkrát, takže jejich podíl se nezmění. | Треугольники с одинаковыми углами **подобны**: все стороны увеличиваются в одно и то же число раз, поэтому их отношение не меняется. |

### AngleSetsRatio

| čeština | русский |
|---|---|
| Rampa s nastavitelným úhlem a délkou | Пандус с изменяемым углом и длиной |
| ${num(2 * k)} m | ${num(2 * k)} м |
| výška ${num(2 * k * ratio)} m | высота ${num(2 * k * ratio)} м |
| Teď měň úhel. Podíl výška / šikmá délka závisí **jen na úhlu** – ne na tom, jak je rampa velká. | Теперь меняй угол. Отношение высота / длина по склону зависит **только от угла**, а не от размера пандуса. |
| Nastav posuvníkem „Úhel α“ 30° – jaký je podíl? | Поставь ползунком «Угол α» 30° — какое получилось отношение? |
| Nastav α = 60° a změň délku rampy (kroužkem nebo posuvníkem „Délka rampy“) – podíl se nezmění | Поставь α = 60° и измени длину пандуса (кружком или ползунком «Длина пандуса») — отношение не сдвинется |
| Úhel α = ${alpha.roundToInt()}° | Угол α = ${alpha.roundToInt()}° |
| Délka rampy | Длина пандуса |
| výška / šikmá délka | высота / длина по склону |
| Tomuhle číslu se říká **sinus** úhlu α, píšeme sin α. Sinus 30° je 0,5 v každém pravoúhlém trojúhelníku na světě. | Это число называют **синусом** угла α, пишут sin α. Синус 30° равен 0,5 в любом прямоугольном треугольнике на свете. |

### NameSides

| čeština | русский |
|---|---|
| To je přepona – leží naproti pravému úhlu. Odvěsny jsou ty dvě kratší strany. | Это гипотенуза — она лежит напротив прямого угла. Катеты — две более короткие стороны. |
| Tahle odvěsna se úhlu nedotýká – je protilehlá. Která odvěsna z vrcholu úhlu vychází? | Этот катет угла не касается — он противолежащий. Какой катет выходит из вершины угла? |
| Přepona je nejdelší strana. Která strana leží naproti pravému úhlu? | Гипотенуза — самая длинная сторона. Какая сторона лежит напротив прямого угла? |
| Teď hledáme odvěsnu u úhlu α – přepni na „Úhel α“. | Сейчас ищем катет у угла α — переключи на «Угол α». |
| Nejdřív přepni na „Úhel β“. | Сначала переключи на «Угол β». |
| Pravoúhlý trojúhelník se třemi stranami, na které se dá klepnout | Прямоугольный треугольник, на стороны которого можно нажать |
| přepona | гипотенуза |
| protilehlá | противолежащий |
| přilehlá | прилежащий |
| přilehlá | прилежащий |
| protilehlá | противолежащий |
| Strany pravoúhlého trojúhelníku mají jména. **Přepona** leží naproti pravému úhlu; **odvěsny** jsou dvě: **přilehlá** (u úhlu) a **protilehlá** (naproti úhlu). | У сторон прямоугольного треугольника есть названия. **Гипотенуза** (přepona) лежит напротив прямого угла; **катетов** (odvěsny) два: **прилежащий** (přilehlá) — у угла и **противолежащий** (protilehlá) — напротив угла. |
| Klepni na **přeponu** – nejdelší stranu, naproti pravému úhlu | Нажми на **гипотенузу** — самую длинную сторону, напротив прямого угла |
| Klepni na odvěsnu **přilehlou** k úhlu α | Нажми на катет, **прилежащий** к углу α |
| Přepni na „Úhel β“ a klepni na odvěsnu přilehlou k úhlu β | Переключи на «Угол β» и нажми на катет, прилежащий к углу β |
| Úhel α | Угол α |
| Úhel β | Угол β |
| Přilehlá a protilehlá si vymění role, když se díváš z druhého úhlu. Přepona zůstává přeponou. | Прилежащий и противолежащий меняются ролями, если смотреть из другого угла. Гипотенуза остаётся гипотенузой. |

### CheckOpposite

| čeština | русский |
|---|---|
| Otočený pravoúhlý trojúhelník se stranami a, b, c | Повёрнутый прямоугольный треугольник со сторонами a, b, c |
| Která strana je **protilehlá** úhlu α? | Какая сторона — **противолежащая** углу α? |
| a | a |
| b | b |
| Strana b vychází přímo z vrcholu úhlu α – dotýká se ho. Jak se říká odvěsně u úhlu? | Сторона b выходит прямо из вершины угла α — она его касается. Как называется катет у угла? |
| c | c |
| c leží naproti pravému úhlu – to je přepona. Která odvěsna se úhlu α vůbec nedotýká? | c лежит напротив прямого угла — это гипотенуза. Какой катет вообще не касается угла α? |
| Protilehlá odvěsna se vrcholu úhlu nedotýká – leží naproti. Nezáleží na tom, jak je trojúhelník otočený. | Противолежащий катет не касается вершины угла — он лежит напротив. Как повёрнут треугольник, неважно. |
| Odkud se vzal sinus | Откуда взялся синус |
| V pravoúhlých trojúhelnících se stejným úhlem je podíl stran vždy stejný. | В прямоугольных треугольниках с одинаковым углом отношение сторон всегда одно и то же. |

## Lekce 2: Sinus, kosinus, tangens / Синус, косинус, тангенс

### ThreeRatios

| čeština | русский |
|---|---|
| Pravoúhlý trojúhelník s přeponou 10 dílků | Прямоугольный треугольник с гипотенузой 10 делений |
| Ze tří stran jdou složit tři užitečné podíly – **goniometrické funkce** úhlu α. Každá má jméno a závisí jen na úhlu. | Из трёх сторон можно составить три полезных отношения — **тригонометрические функции** угла α (goniometrické funkce). У каждой есть имя, и каждая зависит только от угла. |
| Klepni na „sin α“ – které dvě strany se dělí? | Нажми «sin α» — какие две стороны делятся? |
| Klepni na „cos α“ | Нажми «cos α» |
| Klepni na „tg α“ | Нажми «tg α» |
| sin α | sin α |
| cos α | cos α |
| tg α | tg α |
| Úhel α = ${alpha.roundToInt()}° | Угол α = ${alpha.roundToInt()}° |
| Žlutě čitatel (nahoře ve zlomku), zeleně jmenovatel (dole). | Жёлтым — числитель (сверху в дроби), зелёным — знаменатель (снизу). |
| sin α = protilehlá / přepona · cos α = přilehlá / přepona · tg α = protilehlá / přilehlá | sin α = противолежащий / гипотенуза · cos α = прилежащий / гипотенуза · tg α = противолежащий / прилежащий |
| Na kalkulačce se tangens jmenuje **tan**. | На калькуляторе тангенс называется **tan**. |

### BuildFraction

| čeština | русский |
|---|---|
| Sinus bere odvěsnu **naproti** úhlu. Která strana se úhlu α nedotýká? | Синус берёт катет **напротив** угла. Какая сторона не касается угла α? |
| Kosinus bere odvěsnu **u** úhlu. Která odvěsna z úhlu α vychází? | Косинус берёт катет **у** угла. Какой катет выходит из угла α? |
| V tangentu přepona vůbec není – jen obě odvěsny. | В тангенсе гипотенузы нет вообще — только два катета. |
| Přepona patří ve zlomku dolů – nahoru dej odvěsnu. | Гипотенуза в дроби стоит снизу — сверху поставь катет. |
| Sinus i kosinus dělí přeponou – nejdelší stranou. | И синус, и косинус делят на гипотенузу — самую длинную сторону. |
| V tangentu přepona vůbec není – jen obě odvěsny. | В тангенсе гипотенузы нет вообще — только два катета. |
| Tuhle stranu už máš nahoře. Dole je ta druhá odvěsna. | Эта сторона уже стоит сверху. Снизу — другой катет. |
| Trojúhelník s pojmenovanými stranami, na které se dá klepnout | Треугольник с подписанными сторонами, на которые можно нажать |
| Slož každý podíl sám: klepni nejdřív na stranu, která je ve zlomku **nahoře**, pak na tu **dole**. | Собери каждое отношение сам: сначала нажми на сторону, которая в дроби **сверху**, потом на ту, что **снизу**. |
| Slož sin α | Собери sin α |
| Slož cos α | Собери cos α |
| Slož tg α | Собери tg α |
| Pomůcka: **ko**sinus – odvěsna v **ko**ntaktu s úhlem (přilehlá). Sinus – ta, co stojí naproti. | Подсказка: **ко**синус — катет, который **ка**сается угла (прилежащий). Синус — тот, что напротив. |

### Check345

| čeština | русский |
|---|---|
| Trojúhelník 3-4-5 na čtverečkované síti | Треугольник 3-4-5 на клетчатой сетке |
| Kolik je sin α? | Чему равен sin α? |
| 3/5 = 0,6 | 3/5 = 0,6 |
| 4/5 = 0,8 | 4/5 = 0,8 |
| 4 je odvěsna u úhlu α. Která funkce bere přilehlou odvěsnu? | 4 — катет у угла α. Какая функция берёт прилежащий катет? |
| 3/4 = 0,75 | 3/4 = 0,75 |
| Tady se dělí odvěsna odvěsnou – to je tangens. Čím dělí sinus? | Здесь катет делится на катет — это тангенс. На что делит синус? |
| 5/3 ≈ 1,67 | 5/3 ≈ 1,67 |
| Přepona je nejdelší strana, takže sinus nikdy nevyjde větší než 1. Co patří ve zlomku nahoru? | Гипотенуза — самая длинная сторона, поэтому синус никогда не больше 1. Что в дроби должно быть сверху? |
| sin α = protilehlá / přepona = 3/5 = 0,6. Tentýž trojúhelník dává cos α = 4/5 = 0,8 a tg α = 3/4 = 0,75. | sin α = противолежащий / гипотенуза = 3/5 = 0,6. Тот же треугольник даёт cos α = 4/5 = 0,8 и tg α = 3/4 = 0,75. |

### Range

| čeština | русский |
|---|---|
| Trojúhelník s přeponou 1 a sloupce sin a cos | Треугольник с гипотенузой 1 и столбики sin и cos |
| Když má přepona délku 1, je protilehlá odvěsna přímo sin α a přilehlá cos α. Sleduj je od 0° do 90°. | Если гипотенуза равна 1, то противолежащий катет — это прямо sin α, а прилежащий — cos α. Проследи за ними от 0° до 90°. |
| Nastav α = 0° – kolik je sin a kolik cos? | Поставь α = 0° — чему равны sin и cos? |
| Nastav α = 90° | Поставь α = 90° |
| Najdi úhel, kde je sin α stejný jako cos α | Найди угол, при котором sin α равен cos α |
| Úhel α = ${alpha.roundToInt()}° | Угол α = ${alpha.roundToInt()}° |
| Sinus s úhlem roste od 0 do 1, kosinus klesá od 1 do 0. Ani jeden nikdy nepřekročí 1. | Синус с ростом угла растёт от 0 до 1, косинус убывает от 1 до 0. Ни один из них никогда не превышает 1. |

### Ladder

| čeština | русский |
|---|---|
| Žebřík opřený o zeď pod úhlem 60° | Лестница, прислонённая к стене под углом 60° |
| Vzorec jde obrátit: **protilehlá = přepona · sin α**, **přilehlá = přepona · cos α**. Přesně takhle se počítají složky síly. | Формулу можно перевернуть: **противолежащий = гипотенуза · sin α**, **прилежащий = гипотенуза · cos α**. Именно так считают составляющие силы. |
| Žebřík dlouhý 5 m svírá se zemí 60° (sin 60° ≈ 0,866; cos 60° = 0,5). Jak vysoko na zdi končí? | Лестница длиной 5 м составляет с землёй 60° (sin 60° ≈ 0,866; cos 60° = 0,5). На какой высоте она упирается в стену? |
| výška | высота |
| Výška leží naproti úhlu 60°, proto sinus: 5 · sin 60° ≈ 5 · 0,866 ≈ 4,33 m. | Высота лежит напротив угла 60°, поэтому синус: 5 · sin 60° ≈ 5 · 0,866 ≈ 4,33 м. |
| To je 5 · cos 60° – vzdálenost paty žebříku od zdi, odvěsna **u** úhlu. Výška leží naproti. | Это 5 · cos 60° — расстояние от низа лестницы до стены, катет **у** угла. Высота лежит напротив. |
| Výška nemůže být větší než žebřík. Přeponu sinem násob, neděl. | Высота не может быть больше лестницы. Гипотенузу на синус умножай, а не дели. |
| Výška je odvěsna naproti úhlu 60° – která funkce k ní patří? | Высота — катет напротив угла 60°. Какая функция к нему относится? |
| Sinus, kosinus, tangens | Синус, косинус, тангенс |
| Tři podíly stran – a jak z přepony spočítat odvěsnu. | Три отношения сторон — и как по гипотенузе найти катет. |

## Lekce 3: Stupně a kalkulačka / Градусы и калькулятор

### Protractor

| čeština | русский |
|---|---|
| Úhloměr s ramenem, které se dá natočit | Транспортир с поворачивающимся лучом |
| Úhly měříme ve stupních: plný úhel (celá otáčka) má 360°, pravý úhel 90°. V pravoúhlém trojúhelníku dávají oba ostré úhly (menší než 90°) dohromady 90°. | Углы измеряем в градусах: полный угол (оборот) — 360°, прямой угол — 90°. В прямоугольном треугольнике два острых угла (меньше 90°) вместе дают 90°. |
| Nastav pravý úhel, 90° (kroužkem nebo posuvníkem „Úhel“) | Поставь прямой угол, 90° (кружком или ползунком «Угол») |
| Nastav 45° – polovinu pravého úhlu | Поставь 45° — половину прямого угла |
| Nastav 180° – přímý úhel | Поставь 180° — развёрнутый угол |
| Úhel = ${deg.roundToInt()}° | Угол = ${deg.roundToInt()}° |

### CalcMode

| čeština | русский |
|---|---|
| Displej kalkulačky se sin 30 | Дисплей калькулятора с sin 30 |
| Kalkulačka bere úhel třemi způsoby: **DEG** = stupně (ty chceme), **RAD** = radiány (jiná jednotka, viz kartu 4), **GRAD** = grady (pravý úhel = 100 gradů; nepoužíváme). Stejné číslo 30 pak znamená něco jiného. | Калькулятор понимает угол тремя способами: **DEG** — градусы (нам нужны они), **RAD** — радианы (другая единица, см. карточку 4), **GRAD** — грады (прямой угол = 100 град; не используем). Одно и то же число 30 тогда значит разное. |
| Přepni na „RAD“ – co ukáže sin 30? | Переключи на «RAD» — что покажет sin 30? |
| Přepni na „GRAD“ | Переключи на «GRAD» |
| Vrať kalkulačku na „DEG“ | Верни калькулятор на «DEG» |
| DEG | DEG |
| RAD | RAD |
| GRAD | GRAD |
| Který výsledek sin 30 je správně pro **stupně**? | Какой результат sin 30 верен для **градусов**? |
| 0,5 | 0,5 |
| −0,988 | −0,988 |
| Tohle ukázal režim RAD – bere 30 jako 30 radiánů (skoro pět otáček). | Это показал режим RAD — он считает 30 как 30 радиан (почти пять оборотов). |
| 0,454 | 0,454 |
| Tohle ukázal režim GRAD – 30 gradů je jen 27°. | Это показал режим GRAD — 30 град — это всего 27°. |
| Ve stupních je sin 30° = 0,5. Rychlá zkouška před počítáním: zadej sin 30 – když nevyjde 0,5, je kalkulačka ve špatném režimu. | В градусах sin 30° = 0,5. Быстрая проверка перед расчётом: набери sin 30 — если получилось не 0,5, калькулятор не в том режиме. |
| Na telefonu: otevři Kalkulačku a otoč telefon na šířku (iPhone) nebo rozbal panel funkcí (Android) – objeví se sin, cos, tan. Režim přepíná tlačítko Deg/Rad. Někde se píše nejdřív číslo a pak sin, jinde obráceně – vyzkoušej si to. | На телефоне: открой Калькулятор и поверни телефон горизонтально (iPhone) или разверни панель функций (Android) — появятся sin, cos, tan. Режим меняет кнопка Deg/Rad. Где-то сначала вводят число, а потом sin, где-то наоборот — попробуй. |

### CheckRad

| čeština | русский |
|---|---|
| Kalkulačka ukazuje 10 · sin 30 = −9,88 | Калькулятор показывает 10 · sin 30 = −9,88 |
| Počítáš 10 · sin 30° a kalkulačka ukáže −9,88. Co se stalo? | Ты считаешь 10 · sin 30°, а калькулятор показывает −9,88. Что случилось? |
| Kalkulačka je v režimu RAD | Калькулятор в режиме RAD |
| Nic, sinus bývá záporný | Ничего, синус бывает отрицательным |
| Pro úhly mezi 0° a 90° je sinus vždy mezi 0 a 1. Kolik má vyjít sin 30°? | Для углов от 0° до 90° синус всегда между 0 и 1. Сколько должно получиться для sin 30°? |
| Mělo se počítat s cos | Надо было считать через cos |
| 10 · cos 30° = 8,66 – taky kladné číslo. Co na kalkulačce rozhoduje, jak rozumí číslu 30? | 10 · cos 30° = 8,66 — тоже положительное число. Что на калькуляторе решает, как он понимает число 30? |
| Chyba je v násobení deseti | Ошибка в умножении на десять |
| Zkus jen sin 30: vyjde −0,988 místo 0,5. Chyba je už tam. | Попробуй просто sin 30: выйдет −0,988 вместо 0,5. Ошибка уже там. |
| Záporný nebo podivný výsledek u ostrého úhlu skoro vždy znamená režim RAD. Přepni na DEG a ověř, že sin 30 = 0,5. | Отрицательный или странный результат для острого угла почти всегда означает режим RAD. Переключи на DEG и проверь, что sin 30 = 0,5. |

### Radians

| čeština | русский |
|---|---|
| Kružnice s poloměrem 1 a vyznačeným obloukem | Окружность радиуса 1 с выделенной дугой |
| Radián je jednotka **obloukové míry**: úhel měříme délkou oblouku na kružnici s poloměrem 1 – kolik poloměrů ujdeš po obvodu. Půlkružnice má délku π ≈ 3,14 (π je číslo z obvodu kruhu), proto 180° = π rad. | Радиан — единица **радианной меры**: угол измеряют длиной дуги на окружности радиуса 1 — сколько радиусов пройдёшь по окружности. Полуокружность имеет длину π ≈ 3,14 (π — число из длины окружности), поэтому 180° = π рад. |
| Nastav posuvníkem „Úhel“ 180° – kolik je to radiánů? | Поставь ползунком «Угол» 180° — сколько это радиан? |
| Úhel = ${deg.roundToInt()}° | Угол = ${deg.roundToInt()}° |
| Ve statice na průmyslovce počítáme ve stupních. Radiány stačí poznat – a umět je na kalkulačce vypnout. | В статике в техникуме считаем в градусах. Радианы достаточно узнавать — и уметь выключить их на калькуляторе. |
| Stupně a kalkulačka | Градусы и калькулятор |
| Jak se měří úhel a proč musí na kalkulačce svítit DEG. | Как измеряют угол и почему на калькуляторе должно гореть DEG. |

## Lekce 4: Úhly, které se vyplatí umět zpaměti / Углы, которые стоит помнить

### HalfSquare

| čeština | русский |
|---|---|
| Čtverec o straně 1 a jeho úhlopříčka | Квадрат со стороной 1 и его диагональ |
| Čtverec o straně 1 rozpůlený úhlopříčkou dá trojúhelník 45°–45°–90°. Úhlopříčka měří √2 ≈ 1,41. | Квадрат со стороной 1, разрезанный по диагонали, даёт треугольник 45°–45°–90°. Диагональ равна √2 ≈ 1,41. |
| √ je **odmocnina**: √25 = 5, protože 5 · 5 = 25. A √2 ≈ 1,41, protože 1,41 · 1,41 ≈ 2. Proč úhlopříčka vyjde právě √2, ukáže lekce 5. | √ — **квадратный корень**: √25 = 5, потому что 5 · 5 = 25. А √2 ≈ 1,41, так как 1,41 · 1,41 ≈ 2. Почему диагональ равна именно √2, покажет урок 5. |
| Klepni na „Rozpůl čtverec“ | Нажми «Разрежь квадрат» |
| Rozpůl čtverec | Разрежь квадрат |
| Kolik je tg 45°? | Чему равен tg 45°? |
| 1 | 1 |
| 0,707 | 0,707 |
| 0,707 = 1 / 1,41 je odvěsna dělená přeponou, tedy sin 45°. Čím dělí tangens? | 0,707 = 1 / 1,41 — катет, делённый на гипотенузу, то есть sin 45°. На что делит тангенс? |
| 0,5 | 0,5 |
| Obě odvěsny jsou stejně dlouhé. Kolik vyjde, když číslo vydělíš stejným číslem? | Оба катета одинаковы. Сколько получится, если число разделить на такое же? |
| 1,41 | 1,41 |
| √2 je délka přepony. V tangentu přepona není. | √2 — длина гипотенузы. В тангенсе гипотенузы нет. |
| Odvěsny jsou stejné, takže tg 45° = 1. A sin 45° = cos 45° = 1 / 1,41 ≈ 0,707 (v tabulkách se píše √2/2 – je to totéž číslo). | Катеты равны, поэтому tg 45° = 1. А sin 45° = cos 45° = 1 / 1,41 ≈ 0,707 (в таблицах пишут √2/2 — это то же число). |

### HalfTriangle

| čeština | русский |
|---|---|
| Rovnostranný trojúhelník o straně 2 a jeho výška | Равносторонний треугольник со стороной 2 и его высота |
| Rovnostranný trojúhelník o straně 2 má všechny úhly 60°. Výška ho rozpůlí na trojúhelník 30°–60°–90° se stranami 1, √3 ≈ 1,73 a 2. | У равностороннего треугольника со стороной 2 все углы по 60°. Высота делит его на треугольник 30°–60°–90° со сторонами 1, √3 ≈ 1,73 и 2. |
| Klepni na „Rozpůl trojúhelník“ | Нажми «Разрежь треугольник» |
| Klepni na úhel 30° (kroužek nahoře) a přečti sin a cos | Нажми на угол 30° (кружок наверху) и прочитай sin и cos |
| Klepni na úhel 60° (kroužek vlevo dole) | Нажми на угол 60° (кружок слева внизу) |
| Rozpůl trojúhelník | Разрежь треугольник |
| Zapamatuj si: naproti 30° leží **polovina přepony**. Proto sin 30° = 0,5. | Запомни: напротив угла 30° лежит **половина гипотенузы**. Поэтому sin 30° = 0,5. |

### SqrtTable

| čeština | русский |
|---|---|
| Tabulka hodnot sin a cos pro 0°, 30°, 45°, 60°, 90° | Таблица значений sin и cos для 0°, 30°, 45°, 60°, 90° |
| Pomůcka: sin 0°, 30°, 45°, 60°, 90° je **(√0)/2, (√1)/2, (√2)/2, (√3)/2, (√4)/2** – odmocnina jen nahoře, pod ní počítáš 0, 1, 2, 3, 4. Protože √0 = 0, √1 = 1 a √4 = 2, začíná řada 0, pak 0,5 a končí 1. | Приём: sin 0°, 30°, 45°, 60°, 90° — это **(√0)/2, (√1)/2, (√2)/2, (√3)/2, (√4)/2** — корень только сверху, под ним считаешь 0, 1, 2, 3, 4. Так как √0 = 0, √1 = 1 и √4 = 2, ряд начинается с 0, потом 0,5 и заканчивается 1. |
| Doplň řádek sin: u každého úhlu vyber správnou hodnotu (${filled.coerceAtMost(5)}/5) | Заполни строку sin: для каждого угла выбери верное значение (${filled.coerceAtMost(5)}/5) |
| Klepni na „Otoč pořadí“ – vznikne řádek cos | Нажми «Переверни порядок» — получится строка cos |
| Otoč pořadí | Переверни порядок |
| „Otoč pořadí“ jde použít, až bude řádek sin celý. | «Переверни порядок» сработает, когда строка sin будет заполнена. |
| Kosinus má stejná čísla pozpátku. Vyčísleno – sin: 0 – 0,5 – 0,707 – 0,866 – 1; cos: 1 – 0,866 – 0,707 – 0,5 – 0. | У косинуса те же числа в обратном порядке. В десятичных — sin: 0 – 0,5 – 0,707 – 0,866 – 1; cos: 1 – 0,866 – 0,707 – 0,5 – 0. |
| A tangens = sin / cos: tg 30° ≈ 0,577, tg 45° = 1, tg 60° ≈ 1,732 (tg 90° není definován). | А тангенс = sin / cos: tg 30° ≈ 0,577, tg 45° = 1, tg 60° ≈ 1,732 (tg 90° не определён). |

### CheckCos60

| čeština | русский |
|---|---|
| Kolik je cos 60°? | Чему равен cos 60°? |
| 0,5 | 0,5 |
| 0,866 | 0,866 |
| 0,866 = √3/2 je sin 60°. Kosinus má tabulku pozpátku – co je v ní pod 60°? | 0,866 = √3/2 — это sin 60°. У косинуса таблица в обратном порядке — что в ней под 60°? |
| 0,707 | 0,707 |
| 0,707 patří k 45°, kde jsou sin a cos stejné. | 0,707 относится к 45°, где sin и cos равны. |
| 1 | 1 |
| cos je 1 jen pro 0° – když přilehlá odvěsna splyne s přeponou. | cos равен 1 только при 0° — когда прилежащий катет сливается с гипотенузой. |
| cos 60° = sin 30° = 0,5: u úhlu 60° leží kratší odvěsna, polovina přepony. | cos 60° = sin 30° = 0,5: у угла 60° лежит короткий катет, половина гипотенузы. |

### NoCalc

| čeština | русский |
|---|---|
| Šikmá síla 20 kN pod úhlem 30° k nosníku | Наклонная сила 20 кН под углом 30° к балке |
| Šikmou sílu [[F]] = 20 kN nahradíme dvěma: [[F_x]] podél nosníku (vodorovně) a [[F_y]] kolmo na něj (svisle). Síla svírá s nosníkem 30°. Jak velké jsou [[F_x]] a [[F_y]]? (bez kalkulačky) | Наклонную силу [[F]] = 20 кН заменяем двумя: [[F_x]] — вдоль балки (по горизонтали) и [[F_y]] — поперёк (по вертикали). Сила составляет с балкой 30°. Чему равны [[F_x]] и [[F_y]]? (без калькулятора) |
| [[F_x]] | [[F_x]] |
| [[F_y]] | [[F_y]] |
| [[F_y]] leží naproti úhlu 30° – je to polovina síly: 20 · 0,5 = 10 kN. [[F_x]] ≈ 20 · 0,866 ≈ 17,32 kN. | [[F_y]] лежит напротив угла 30° — это половина силы: 20 · 0,5 = 10 кН. [[F_x]] ≈ 20 · 0,866 ≈ 17,32 кН. |
| Složky jsou prohozené. Která z nich leží u úhlu 30°? | Составляющие перепутаны местами. Какая из них лежит у угла 30°? |
| Složka nemůže být stejně velká jako celá síla – je to jen její část. Sílu vynásob sinem nebo kosinem. | Составляющая не может равняться всей силе — это только её часть. Умножь силу на синус или косинус. |
| Pomůže tabulka: sin 30° = 0,5, cos 30° ≈ 0,866. Která složka leží u úhlu? | Поможет таблица: sin 30° = 0,5, cos 30° ≈ 0,866. Какая составляющая лежит у угла? |
| Úhly, které se vyplatí umět zpaměti | Углы, которые стоит помнить |
| 30°, 45°, 60° bez kalkulačky – ze dvou trojúhelníků. | 30°, 45°, 60° без калькулятора — из двух треугольников. |

## Lekce 5: Vztahy mezi funkcemi / Связи между функциями

### Pythagoras

| čeština | русский |
|---|---|
| Pravoúhlý trojúhelník se čtverci nad stranami | Прямоугольный треугольник с квадратами на сторонах |
| **Pythagorova věta**: a² + b² = c², kde c je přepona. Ze dvou stran pravoúhlého trojúhelníku tak dopočítáš třetí. | **Теорема Пифагора**: a² + b² = c², где c — гипотенуза. По двум сторонам прямоугольного треугольника находишь третью. |
| a² (druhá mocnina) znamená a · a, třeba 3² = 9. Zpátky vede odmocnina: √25 = 5, protože 5 · 5 = 25. | a² (квадрат) означает a · a, например 3² = 9. Обратно ведёт корень: √25 = 5, потому что 5 · 5 = 25. |
| Nastav posuvníky „Odvěsna a“ = 3 a „Odvěsna b“ = 4 – jak dlouhá je přepona? | Поставь ползунки «Катет a» = 3 и «Катет b» = 4 — какой длины гипотенуза? |
| Odvěsna a = $a | Катет a = $a |
| Odvěsna b = $b | Катет b = $b |
| Plocha čtverce nad přeponou je stejná jako obou menších čtverců dohromady: 9 + 16 = 25 čtverečků. | Площадь квадрата на гипотенузе равна площади двух меньших квадратов вместе: 9 + 16 = 25 клеток. |
| Odvěsny měří 5 m a 12 m. Jak dlouhá je přepona? | Катеты равны 5 м и 12 м. Какой длины гипотенуза? |
| c | c |
| c² = 25 + 144 = 169, c = √169 = 13 m. | c² = 25 + 144 = 169, c = √169 = 13 м. |
| Sčítají se **druhé mocniny**, ne strany. Kolik je 5² + 12²? | Складываются **квадраты**, а не сами стороны. Сколько будет 5² + 12²? |
| 169 je teprve c². Co ještě zbývá udělat? | 169 — это пока c². Что ещё осталось сделать? |

### UnitHyp

| čeština | русский |
|---|---|
| Trojúhelník s přeponou 1 a pruh sin² + cos² | Треугольник с гипотенузой 1 и полоса sin² + cos² |
| Při přeponě 1 jsou odvěsny přímo sin α a cos α (lekce 2). Dosaď je do a² + b² = c²: **sin² α + cos² α = 1**. Zápis sin² α znamená (sin α)². | При гипотенузе 1 катеты — это прямо sin α и cos α (урок 2). Подставь их в a² + b² = c²: **sin² α + cos² α = 1**. Запись sin² α означает (sin α)². |
| Nastav posuvníkem „Úhel α“ 30° | Поставь ползунком «Угол α» 30° |
| Nastav α = 60° – změnil se součet? | Поставь α = 60° — изменилась ли сумма? |
| Úhel α = $alpha° | Угол α = $alpha° |
| sin α = 0,6 a α je ostrý úhel. Kolik je cos α? | sin α = 0,6, α — острый угол. Чему равен cos α? |
| 0,8 | 0,8 |
| 0,4 | 0,4 |
| 1 − 0,6 by platilo, kdyby se sčítaly sin a cos. Sčítají se ale jejich **druhé mocniny**. | 1 − 0,6 было бы верно, если бы складывались sin и cos. Но складываются их **квадраты**. |
| 0,64 | 0,64 |
| 0,64 = 1 − 0,6² je teprve cos² α. Co ještě zbývá? | 0,64 = 1 − 0,6² — это пока cos² α. Что ещё осталось? |
| 1,6 | 1,6 |
| Kosinus nikdy není větší než 1. | Косинус никогда не больше 1. |
| cos² α = 1 − 0,36 = 0,64, tedy cos α = 0,8. Je to trojúhelník 3-4-5 zmenšený na přeponu 1. | cos² α = 1 − 0,36 = 0,64, значит cos α = 0,8. Это треугольник 3-4-5, уменьшенный до гипотенузы 1. |

### TangentSlope

| čeština | русский |
|---|---|
| Sklon: na 1 m vodorovně vystoupáš o tg α | Уклон: на 1 м по горизонтали поднимаешься на tg α |
| pokračuje | продолжается |
| **tg α = sin α / cos α**, tedy protilehlá ku přilehlé. Říká, o kolik se stoupne na 1 metr vodorovně – je to **sklon**. | **tg α = sin α / cos α**, то есть противолежащий к прилежащему. Он показывает, на сколько поднимаешься на 1 метр по горизонтали, — это **уклон** (sklon). |
| Nastav posuvníkem „Úhel α“ 45° – tangens je přesně 1 | Поставь ползунком «Угол α» 45° — тангенс ровно 1 |
| Zvětšuj α, dokud tg α nepřekročí 5 | Увеличивай α, пока tg α не превысит 5 |
| Úhel α = $alpha° | Угол α = $alpha° |
| Na rozdíl od sin a cos může být tangens větší než 1. U 90° roste nade všechny meze – tg 90° není definován. | В отличие от sin и cos, тангенс может быть больше 1. У 90° он растёт без предела — tg 90° не определён. |

### Complement

| čeština | русский |
|---|---|
| Trojúhelník s úhly α a β | Треугольник с углами α и β |
| přilehlá k α | прилежащий к α |
| protilehlá k β | противолежащий к β |
| a je protilehlá k α | a — противолежащий к α |
| a je přilehlá k β | a — прилежащий к β |
| Druhý ostrý úhel je β = 90° − α. Co je pro α protilehlá odvěsna, je pro β přilehlá: **sin α = cos (90° − α)**. | Второй острый угол β = 90° − α. Катет, противолежащий для α, для β — прилежащий: **sin α = cos (90° − α)**. |
| Klepni na „Dívám se z úhlu β“ | Нажми «Смотрю из угла β» |
| Vrať se na „Dívám se z úhlu α“ | Вернись на «Смотрю из угла α» |
| Dívám se z úhlu α | Смотрю из угла α |
| Dívám se z úhlu β | Смотрю из угла β |
| sin 20° je stejné číslo jako… | sin 20° — то же число, что и… |
| cos 70° | cos 70° |
| cos 20° | cos 20° |
| sin a cos téhož úhlu se rovnají jen při 45°. Jak velký je druhý ostrý úhel? | sin и cos одного угла равны только при 45°. Чему равен второй острый угол? |
| sin 70° | sin 70° |
| To je sinus druhého úhlu – patří k druhé odvěsně. Která funkce úhlu 70° bere odvěsnu naproti 20°? | Это синус второго угла — он относится к другому катету. Какая функция угла 70° берёт катет напротив 20°? |
| 1 − sin 70° | 1 − sin 70° |
| Do jedné se sčítají druhé mocniny sin a cos téhož úhlu, ne siny dvou úhlů. | До единицы складываются квадраты sin и cos одного угла, а не синусы двух углов. |
| Oba ostré úhly dávají 90°. Odvěsna naproti 20° je zároveň přilehlá k 70°, proto sin 20° = cos 70°. | Два острых угла дают 90°. Катет напротив 20° одновременно прилежит к 70°, поэтому sin 20° = cos 70°. |
| Ve statice je to past: když je úhel síly zadaný od **svislice** místo od vodorovné, sin a cos si vymění místa. | В статике это ловушка: если угол силы задан от **вертикали**, а не от горизонтали, sin и cos меняются местами. |
| Vztahy mezi funkcemi | Связи между функциями |
| Pythagorova věta, sin² + cos² = 1, tg = sin / cos a druhý úhel. | Теорема Пифагора, sin² + cos² = 1, tg = sin / cos и второй угол. |

## Lekce 6: Od stran zpátky k úhlu / От сторон обратно к углу

### BackToAngle

| čeština | русский |
|---|---|
| Trojúhelník na mřížce s nastavitelnou výškou | Треугольник на сетке с изменяемой высотой |
| Zatím se z úhlu počítal podíl stran. Teď obráceně: strany známe a hledáme **úhel**. | До сих пор по углу находили отношение сторон. Теперь наоборот: стороны известны, ищем **угол**. |
| Nastav výšku 3 dílky (kroužkem nebo posuvníkem „Výška“) – jaký úhel vyjde? | Поставь высоту 3 деления (кружком или ползунком «Высота») — какой получится угол? |
| Nastav výšku 4 dílky, stejnou jako základnu | Поставь высоту 4 деления, как основание |
| Výška = $h | Высота = $h |
| Otázka „který úhel má tangens 0,75?“ se zapisuje α = **arctg** 0,75. Na kalkulačce: **SHIFT** (nebo **2nd**, v telefonu **2nd**/**INV**) a **tan** – nad tlačítkem je tan⁻¹. | Вопрос «у какого угла тангенс равен 0,75?» записывают так: α = **arctg** 0,75. На калькуляторе: **SHIFT** (или **2nd**, в телефоне **2nd**/**INV**) и **tan** — над кнопкой написано tan⁻¹. |

### WhichInverse

| čeština | русский |
|---|---|
| přepona | гипотенуза |
| přilehlá | прилежащий |
| protilehlá | противолежащий |
| α = arcsin (protilehlá / přepona) | α = arcsin (противолежащий / гипотенуза) |
| α = arccos (přilehlá / přepona) | α = arccos (прилежащий / гипотенуза) |
| α = arctg (protilehlá / přilehlá) | α = arctg (противолежащий / прилежащий) |
| Trojúhelník, na jehož strany se dá klepnout | Треугольник, на стороны которого можно нажать |
| kalkulačka: ${result.second} | калькулятор: ${result.second} |
| vyber dvě strany, které znáš | выбери две известные стороны |
| Která **inverzní** (obrácená) funkce? Ta, jejíž podíl složíš ze dvou stran, které znáš. | Какая обратная (inverzní) функция? Та, отношение которой можно составить из двух известных сторон. |
| Vyber protilehlou odvěsnu a přeponu (klepni na kroužky nebo na tlačítka) | Выбери противолежащий катет и гипотенузу (нажми на кружки или на кнопки) |
| Vyber přilehlou odvěsnu a přeponu | Выбери прилежащий катет и гипотенузу |
| Vyber obě odvěsny | Выбери оба катета |
| Vybrané jsou vždy dvě strany – třetí klepnutí nahradí tu dřívější. | Выбраны всегда две стороны — третье нажатие заменит более раннюю. |
| arcsin, arccos, arctg jsou na kalkulačce sin⁻¹, cos⁻¹, tan⁻¹. Výsledek je úhel ve stupních – jen v režimu DEG. | arcsin, arccos, arctg на калькуляторе — это sin⁻¹, cos⁻¹, tan⁻¹. Результат — угол в градусах, но только в режиме DEG. |
| Pozor: sin⁻¹ neznamená 1/sin. Je to jen značka „zpátky k úhlu“. | Внимание: sin⁻¹ не означает 1/sin. Это просто значок «обратно к углу». |

### CheckArccos

| čeština | русский |
|---|---|
| Trojúhelník: přilehlá 4 m, přepona 5 m, úhel α neznámý | Треугольник: прилежащий катет 4 м, гипотенуза 5 м, угол α неизвестен |
| Znáš přilehlou odvěsnu 4 m a přeponu 5 m. Jak spočítáš úhel α? | Известны прилежащий катет 4 м и гипотенуза 5 м. Как найти угол α? |
| α = arccos (4/5) = 36,9° | α = arccos (4/5) = 36,9° |
| α = arcsin (4/5) = 53,1° | α = arcsin (4/5) = 53,1° |
| Sinus potřebuje odvěsnu **naproti** úhlu. Strana 4 m se ale úhlu α dotýká – jak se jí říká? | Синусу нужен катет **напротив** угла. А сторона 4 м касается угла α — как она называется? |
| α = arctg (4/5) = 38,7° | α = arctg (4/5) = 38,7° |
| Tangens dělí odvěsnu odvěsnou. Tady je jedna ze známých stran přepona. | Тангенс делит катет на катет. А здесь одна из известных сторон — гипотенуза. |
| α = cos (4/5) | α = cos (4/5) |
| cos udělá z úhlu podíl. My máme podíl a chceme úhel – která funkce jde opačným směrem? | cos превращает угол в отношение. У нас есть отношение, а нужен угол — какая функция идёт в обратную сторону? |
| Přilehlá a přepona → kosinus: cos α = 4/5, tedy α = arccos 0,8 = 36,9°. Úhel 53,1° je ten druhý ostrý. | Прилежащий и гипотенуза → косинус: cos α = 4/5, значит α = arccos 0,8 = 36,9°. Угол 53,1° — это второй острый. |

### SlopeRatio

| čeština | русский |
|---|---|
| Sklon na mřížce podle poměru | Уклон на сетке по отношению |
| vyber sklon tlačítkem | выбери уклон кнопкой |
| sklon $m : $n | уклон $m : $n |
| přepona = 5 | гипотенуза = 5 |
| Sklon se na výkresu zadává poměrem **výška : vodorovná délka**, nejčastěji 1 : n (svah 1 : 2). U prutů bývá nakreslen trojúhelníček, třeba 3 : 4. Ten poměr je přímo tg α. | Уклон на чертеже задают отношением **высота : горизонтальная длина**, чаще всего 1 : n (откос 1 : 2). У стержней рисуют треугольничек, например 3 : 4. Это отношение и есть tg α. |
| Klepni na „3 : 4“ | Нажми «3 : 4» |
| Klepni na „1 : 2“ | Нажми «1 : 2» |
| Klepni na „1 : 1“ | Нажми «1 : 1» |
| $m : $n | $m : $n |
| sklon $m : $n → tg α = $m/$n → α = ${num(Trig.atanD(m.toDouble() / n), 1)}° | уклон $m : $n → tg α = $m/$n → α = ${num(Trig.atanD(m.toDouble() / n), 1)}° |
| U sklonu 3 : 4 úhel ani nepotřebuješ: přepona je 5, takže sin α = 3/5 a cos α = 4/5. | При уклоне 3 : 4 угол даже не нужен: гипотенуза равна 5, значит sin α = 3/5, а cos α = 4/5. |
| Vždy si ověř, co je v poměru první – výška, nebo délka. Nakresli si trojúhelníček. | Всегда проверяй, что в отношении стоит первым — высота или длина. Нарисуй себе треугольничек. |

### SlopePercent

| čeština | русский |
|---|---|
| Silnice se sklonem v procentech | Дорога с уклоном в процентах |
| Sklon v procentech je tentýž podíl výška / vodorovná délka, jen vynásobený stem. 10 % znamená stoupání 10 m na 100 m. | Уклон в процентах — то же отношение высота / горизонтальная длина, только умноженное на сто. 10 % означает подъём 10 м на 100 м. |
| Nastav posuvníkem „Sklon v %“ 10 % | Поставь ползунком «Уклон в %» 10 % |
| Nastav sklon 100 % – kolik je to stupňů? | Поставь уклон 100 % — сколько это градусов? |
| Sklon v % = $p % | Уклон в % = $p % |
| Střecha má sklon 100 %. Jaký je to úhel? | Уклон крыши 100 %. Какой это угол? |
| 45° | 45° |
| 90° | 90° |
| 90° je svislá stěna. Sklon 100 % znamená 100 m výšky na 100 m délky – jak takový trojúhelník vypadá? | 90° — это вертикальная стена. Уклон 100 % означает 100 м высоты на 100 м длины — как выглядит такой треугольник? |
| 100° | 100° |
| Procenta nejsou stupně – jsou to setiny tangentu. | Проценты — не градусы, это сотые доли тангенса. |
| 60° | 60° |
| Spočítej to: tg α = 100/100 = 1. Který úhel má tangens 1? | Посчитай: tg α = 100/100 = 1. У какого угла тангенс равен 1? |
| Sklon v % = tg α · 100. 100 % → tg α = 1 → α = 45°. | Уклон в % = tg α · 100. 100 % → tg α = 1 → α = 45°. |

### RampAngle

| čeština | русский |
|---|---|
| Rampa: 10 m vodorovně, 1,2 m nahoru | Пандус: 10 м по горизонтали, 1,2 м вверх |
| Rampa vystoupá 1,2 m na vodorovné délce 10 m. Jaký úhel svírá se zemí? | Пандус поднимается на 1,2 м на горизонтальной длине 10 м. Какой угол он составляет с землёй? |
| α | α |
| tg α = 1,2 / 10 = 0,12, α = arctg 0,12 ≈ 6,8°. | tg α = 1,2 / 10 = 0,12, α = arctg 0,12 ≈ 6,8°. |
| 12 je sklon v procentech, ne úhel. Ještě použij tan⁻¹. | 12 — это уклон в процентах, а не угол. Ещё нужно применить tan⁻¹. |
| 0,12 je teprve tg α (nebo je kalkulačka v RAD). Úhel dostaneš přes tan⁻¹ v režimu DEG. | 0,12 — это пока tg α (или калькулятор в RAD). Угол получишь через tan⁻¹ в режиме DEG. |
| To je úhel u svislice – vyjde, když se dělí délka výškou. Co patří ve zlomku nahoru? | Это угол у вертикали — так выходит, если делить длину на высоту. Что должно быть в дроби сверху? |
| Od stran zpátky k úhlu | От сторон обратно к углу |
| arcsin, arccos, arctg a sklon zadaný poměrem nebo v procentech. | arcsin, arccos, arctg и уклон, заданный отношением или в процентах. |

## Lekce 7: Jednotková kružnice a znaménka / Единичная окружность и знаки

### UnitCircle

| čeština | русский |
|---|---|
| Jednotková kružnice s pohyblivým bodem | Единичная окружность с подвижной точкой |
| **Jednotková kružnice** má poloměr 1. Úhel φ (čti „fí“) měříme od kladné poloosy x (doprava) proti směru hodinových ručiček. Bod na kružnici je vpravo o cos φ a nahoře o sin φ. | У **единичной окружности** радиус 1. Угол φ («фи») отсчитываем от положительной полуоси x (вправо) против часовой стрелки. Точка на окружности смещена вправо на cos φ и вверх на sin φ. |
| Nastav φ = 30° (tahem za bod nebo posuvníkem „Úhel φ“) – poznáváš trojúhelník z lekce 4? | Поставь φ = 30° (перетащи точку или сдвинь ползунок «Угол φ») — узнаёшь треугольник из урока 4? |
| Přetáhni bod do levé horní čtvrtiny (mezi 90° a 180°) – co se stane s cos φ? | Перетащи точку в левую верхнюю четверть (между 90° и 180°) — что произойдёт с cos φ? |
| Úhel φ = $phi° | Угол φ = $phi° |
| Vlevo od osy y je cos φ záporný – bod leží v záporných x. | Слева от оси y cos φ отрицательный — точка лежит в области отрицательных x. |
| Takhle mají sin a cos smysl i pro úhly větší než 90° – kružnice je vlastně „hodinová ručička“, která vyšla ze 3 hodin. | Так sin и cos имеют смысл и для углов больше 90° — радиус как стрелка часов, которая вышла из положения «3 часа». |

### Quadrants

| čeština | русский |
|---|---|
| Jednotková kružnice se čtyřmi kvadranty | Единичная окружность с четырьмя квадрантами |
| Osy dělí rovinu na čtyři **kvadranty**. Znaménko cos říká vpravo/vlevo, znaménko sin nahoře/dole. | Оси делят плоскость на четыре **квадранта** (четверти). Знак cos говорит «справа/слева», знак sin — «сверху/снизу». |
| Navštiv I. kvadrant (0°–90°): přetáhni bod nebo posuň posuvník | Зайди в I квадрант (четверть, 0°–90°): перетащи точку или сдвинь ползунок |
| II. kvadrant (90°–180°) | II квадрант (90°–180°) |
| III. kvadrant (180°–270°) | III квадрант (180°–270°) |
| IV. kvadrant (270°–360°) | IV квадрант (270°–360°) |
| Úhel φ = $phi° | Угол φ = $phi° |
| Vpravo je cos kladný, vlevo záporný. Nahoře je sin kladný, dole záporný. | Справа cos положительный, слева отрицательный. Сверху sin положительный, снизу отрицательный. |

### AcuteAndSign

| čeština | русский |
|---|---|
| Síla 10 kN pod úhlem 30° k vodorovné ve zvoleném směru | Сила 10 кН под углом 30° к горизонтали в выбранном направлении |
| Postup pro statiku: velikosti složek spočítej z **ostrého** úhlu α, znaménko urči **pohledem na šipku**. Doprava +, doleva −, nahoru +, dolů −. | Приём для статики: величины составляющих считай по **острому** углу α, а знак определяй, **глядя на стрелку**. Вправо +, влево −, вверх +, вниз −. |
| Klepni na „↖“ a pak vyber znaménko u [[F_x]] i [[F_y]] | Нажми «↖», затем выбери знак у [[F_x]] и у [[F_y]] |
| Totéž pro „↙“ | То же для «↙» |
| Totéž pro „↘“ | То же для «↘» |
| ↗ | ↗ |
| ↖ | ↖ |
| ↙ | ↙ |
| ↘ | ↘ |
| [[F_x]]: + | [[F_x]]: + |
| [[F_x]]: − | [[F_x]]: − |
| [[F_y]]: + | [[F_y]]: + |
| [[F_y]]: − | [[F_y]]: − |
| Kam míří hrot šipky vodorovně – doprava, nebo doleva? | Куда смотрит остриё по горизонтали — вправо или влево? |
| Míří šipka nahoru, nebo dolů? | Стрелка направлена вверх или вниз? |
| Tak to dělá i trenažér: α je úhel mezi silou a nosníkem, vždy mezi 0° a 90°. | Так делает и тренажёр: α — угол между силой и балкой, всегда от 0° до 90°. |

### CheckDownRight

| čeština | русский |
|---|---|
| Síla 10 kN šikmo dolů doprava, 60° k vodorovné | Сила 10 кН наклонно вниз вправо, 60° к горизонтали |
| Síla [[F]] = 10 kN míří šikmo dolů doprava a s vodorovnou svírá 60°. Jaké jsou složky? | Сила [[F]] = 10 кН направлена наклонно вниз вправо и составляет с горизонталью 60°. Каковы составляющие? |
| [[F_x]] = +5 kN, [[F_y]] = −8,66 kN | [[F_x]] = +5 кН, [[F_y]] = −8,66 кН |
| [[F_x]] = +8,66 kN, [[F_y]] = −5 kN | [[F_x]] = +8,66 кН, [[F_y]] = −5 кН |
| Znaménka sedí, ale velikosti jsou prohozené. Která složka leží u úhlu 60°? | Знаки верны, но величины перепутаны. Какая составляющая лежит у угла 60°? |
| [[F_x]] = +5 kN, [[F_y]] = +8,66 kN | [[F_x]] = +5 кН, [[F_y]] = +8,66 кН |
| Velikosti sedí. Míří šipka nahoru, nebo dolů? | Величины верны. Стрелка направлена вверх или вниз? |
| [[F_x]] = −5 kN, [[F_y]] = −8,66 kN | [[F_x]] = −5 кН, [[F_y]] = −8,66 кН |
| Svisle je to dobře. Kam míří hrot vodorovně? | По вертикали верно. Куда смотрит остриё по горизонтали? |
| [[F_x]] leží u úhlu: 10 · cos 60° = 5 kN, doprava, tedy +. [[F_y]] = 10 · sin 60° = 8,66 kN, dolů, tedy −. | [[F_x]] лежит у угла: 10 · cos 60° = 5 кН, вправо, значит +. [[F_y]] = 10 · sin 60° = 8,66 кН, вниз, значит −. |

### CheckCos150

| čeština | русский |
|---|---|
| Jednotková kružnice s bodem na 150° | Единичная окружность с точкой на 150° |
| Kolik je cos 150°? | Чему равен cos 150°? |
| −0,866 | −0,866 |
| 0,866 | 0,866 |
| Velikost sedí – k ose x chybí 30°. Na které straně od osy y bod leží? | Величина верна — до оси x не хватает 30°. С какой стороны от оси y лежит точка? |
| −0,5 | −0,5 |
| 0,5 je svislá souřadnice bodu, tedy sin 150°. Která souřadnice je cos? | 0,5 — вертикальная координата точки, то есть sin 150°. Какая координата — cos? |
| 0,5 | 0,5 |
| To je sin 150°. Kosinus je vodorovná souřadnice – a bod leží vlevo. | Это sin 150°. Косинус — горизонтальная координата, а точка лежит слева. |
| 150° je 30° před osou −x: velikost cos 30° ≈ 0,866, bod leží vlevo, proto −0,866. | 150° — это 30° до оси −x: величина cos 30° ≈ 0,866, точка слева, поэтому −0,866. |
| Jednotková kružnice a znaménka | Единичная окружность и знаки |
| Kdy je složka kladná a kdy záporná – podle směru šipky. | Когда составляющая положительна, а когда отрицательна — по направлению стрелки. |

## Lekce 8: Trigonometrie ve statice / Тригонометрия в статике

### FromVertical

| čeština | русский |
|---|---|
| Šikmá síla 10 kN na nosník s vyznačeným úhlem | Наклонная сила 10 кН на балку с отмеченным углом |
| Pravidlo „cos patří k [[F_x]]“ platí jen pro úhel měřený od vodorovné. Spolehlivé je jiné: **složka u úhlu → cos, složka naproti → sin**. | Правило «cos — это для [[F_x]]» верно только для угла от горизонтали. Надёжно другое: **составляющая у угла → cos, напротив → sin**. |
| Klepni na „Úhel od svislice (β)“ | Нажми «Угол от вертикали (β)» |
| Vrať se na „Úhel od nosníku (α)“ – změnily se složky? | Вернись на «Угол от балки (α)» — изменились ли составляющие? |
| Úhel od nosníku (α) | Угол от балки (α) |
| Úhel od svislice (β) | Угол от вертикали (β) |
| Síla 10 kN svírá se **svislicí** 30°. Kolik je vodorovná složka [[F_x]]? | Сила 10 кН составляет с **вертикалью** 30°. Чему равна горизонтальная составляющая [[F_x]]? |
| 5 kN | 5 кН |
| 8,66 kN | 8,66 кН |
| 8,66 = 10 · cos 30° je složka **u úhlu**. U které osy je tady úhel nakreslený? | 8,66 = 10 · cos 30° — составляющая **у угла**. У какой оси здесь нарисован угол? |
| 10 kN | 10 кН |
| Síla je šikmá – vodorovně míří jen její část. | Сила наклонная — по горизонтали действует только её часть. |
| Úhel leží u svislice, takže u úhlu je [[F_y]] = 10 · cos 30° = 8,66 kN. Vodorovná složka je naproti: [[F_x]] = 10 · sin 30° = 5 kN. | Угол лежит у вертикали, поэтому у угла — [[F_y]] = 10 · cos 30° = 8,66 кН. Горизонтальная составляющая напротив: [[F_x]] = 10 · sin 30° = 5 кН. |

### Resultant

| čeština | русский |
|---|---|
|  |  |
| míří doprava nahoru | направлена вправо вверх |
| míří doleva nahoru | направлена влево вверх |
| míří doleva dolů | направлена влево вниз |
| míří doprava dolů | направлена вправо вниз |
| Složky a jejich výslednice v osách x, y | Составляющие и их равнодействующая в осях x, y |
| Cesta zpátky: ze složek celá síla. Velikost dá Pythagorova věta, úhel arctg: **R = √([[F_x]]² + [[F_y]]²)**, **tg α = \|[[F_y]]\| / \|[[F_x]]\|**. | Обратный путь: из составляющих — вся сила. Величину даёт теорема Пифагора, угол — arctg: **R = √([[F_x]]² + [[F_y]]²)**, **tg α = \|[[F_y]]\| / \|[[F_x]]\|**. |
| Nastav posuvníky [[F_x]] = 4 a [[F_y]] = 3 | Поставь ползунки [[F_x]] = 4 и [[F_y]] = 3 |
| Pak změň [[F_x]] na −4 – co se stane s velikostí a co se směrem? | Потом измени [[F_x]] на −4 — что будет с величиной и что с направлением? |
| [[F_x]] = 6 kN, [[F_y]] = 8 kN. Jak velká je výslednice R (celá síla složená z obou složek) a jaký ostrý úhel svírá s vodorovnou? | [[F_x]] = 6 кН, [[F_y]] = 8 кН. Какова равнодействующая R (вся сила, сложенная из двух составляющих) и какой острый угол она составляет с горизонталью? |
| R | R |
| α | α |
| R = √(36 + 64) = 10 kN; tg α = 8/6, α ≈ 53,1°. | R = √(36 + 64) = 10 кН; tg α = 8/6, α ≈ 53,1°. |
| Složky se nesčítají přímo – jsou na sebe kolmé. Která věta platí pro pravoúhlý trojúhelník? | Составляющие нельзя просто сложить — они перпендикулярны. Какая теорема работает в прямоугольном треугольнике? |
| 100 je teprve R². Ještě odmocni. | 100 — это пока R². Осталось извлечь корень. |
| To je úhel od svislice – vyjde, když se dělí [[F_x]] / [[F_y]]. Naproti úhlu u vodorovné leží [[F_y]]. | Это угол от вертикали — так выходит, если делить [[F_x]] / [[F_y]]. Напротив угла у горизонтали лежит [[F_y]]. |
| Do arctg dávej velikosti bez znamének – vyjde ostrý úhel. Kam síla míří, poznáš ze znamének složek. | В arctg подставляй величины без знаков — получится острый угол. Куда направлена сила, видно по знакам составляющих. |

### SumForces

| čeština | русский |
|---|---|
| Bod se dvěma silami: šikmá F1 a vodorovná F2 | Точка с двумя силами: наклонная F1 и горизонтальная F2 |
| Síly různých směrů se nesčítají „šipka plus šipka“. Každou rozlož na složky a sečti zvlášť vodorovné a zvlášť svislé – se znaménky. | Силы разных направлений не складывают «стрелка плюс стрелка». Каждую разложи на составляющие и сложи отдельно горизонтальные и отдельно вертикальные — со знаками. |
| Kolik je Σ[[F_x]] – součet všech vodorovných složek (Σ = „součet“)? (cos 30° ≈ 0,866; sin 30° = 0,5) | Чему равна Σ[[F_x]] — сумма всех горизонтальных составляющих (Σ — «сумма»)? (cos 30° ≈ 0,866; sin 30° = 0,5) |
| 5,66 kN | 5,66 кН |
| 11,66 kN | 11,66 кН |
| [[F_2]] míří doleva. Jaké znaménko má v součtu? | [[F_2]] направлена влево. С каким знаком она входит в сумму? |
| 7 kN | 7 кН |
| To je 10 − 3. Do vodorovného součtu nepatří celá šikmá síla, jen její vodorovná složka. | Это 10 − 3. В горизонтальную сумму входит не вся наклонная сила, а только её горизонтальная составляющая. |
| 2 kN | 2 кН |
| To je 5 − 3, ale 5 kN je svislá složka [[F_1]] (naproti úhlu 30°). Která složka je vodorovná? | Это 5 − 3, но 5 кН — вертикальная составляющая [[F_1]] (напротив угла 30°). Какая составляющая горизонтальная? |
| Nejdřív rozlož šikmou sílu: [[F_1x]] = 10 · cos 30° = 8,66 kN doprava. Pak sečti se znaménky: 8,66 − 3 = 5,66 kN. | Сначала разложи наклонную силу: [[F_1x]] = 10 · cos 30° = 8,66 кН вправо. Потом сложи со знаками: 8,66 − 3 = 5,66 кН. |

### MomentArm

| čeština | русский |
|---|---|
| Páka 2 m, síla 10 kN na konci pod úhlem α a její rameno | Рычаг 2 м, сила 10 кН на конце под углом α и её плечо |
| rameno 0 | плечо 0 |
| **Moment** = jak silně síla otáčí = síla × rameno. **Rameno** [[r_⊥]] je kolmá vzdálenost od osy k přímce síly (⊥ = kolmo) – v trojúhelníku s pákou jako přeponou je to odvěsna naproti α: **[[r_⊥]] = r · sin α**. | **Момент** — насколько сильно сила вращает: сила × плечо. **Плечо** [[r_⊥]] — расстояние по перпендикуляру от оси до линии силы (⊥ — перпендикулярно); в треугольнике с рычагом-гипотенузой это катет напротив α: **[[r_⊥]] = r · sin α**. |
| Nastav posuvníkem „Úhel α“ 90° – rameno je celá délka páky | Поставь ползунком «Угол α» 90° — плечо равно всей длине рычага |
| Nastav α = 30° – rameno je polovina | Поставь α = 30° — плечо вдвое короче |
| Nastav α = 0° – nositelka síly prochází bodem otáčení, rameno je 0 | Поставь α = 0° — линия действия силы проходит через точку вращения, плечо равно 0 |
| Úhel α mezi silou a pákou = $alpha° | Угол α между силой и рычагом = $alpha° |
| Stejný moment vyjde i jinak: otáčí jen složka kolmá k páce, M = (F · sin α) · r. | Тот же момент получается и иначе: вращает только составляющая, перпендикулярная рычагу, M = (F · sin α) · r. |
| Základy statiky: Moment síly | Основы статики: момент силы |

### InclinedBeam

| čeština | русский |
|---|---|
| Šikmý nosník délky 5 m s půdorysem a výškou | Наклонная балка длиной 5 м с проекцией и высотой |
| výška ${num(rise, 2)} m | высота ${num(rise, 2)} м |
| půdorys ${num(plan, 2)} m | проекция ${num(plan, 2)} м |
| Šikmý nosník (krokev, schodiště) je přepona. Jeho **půdorysná délka** (vodorovný průmět) je L · cos α a **převýšení** L · sin α. | Наклонная балка (стропило, лестничный марш) — это гипотенуза. Её **горизонтальная проекция** (půdorys) равна L · cos α, а **высота** — L · sin α. |
| Nastav posuvníkem „Sklon α“ 30° – jak vysoko je horní konec? | Поставь ползунком «Угол наклона α» 30° — на какой высоте верхний конец? |
| Nastav sklon 45° | Поставь угол наклона 45° |
| Sklon α = $alpha° | Угол наклона α = $alpha° |
| půdorysná délka = 5 · cos $alpha° ≈ ${num(plan, 2)} m · převýšení = 5 · sin $alpha° ≈ ${num(rise, 2)} m | проекция = 5 · cos $alpha° ≈ ${num(plan, 2)} м · высота = 5 · sin $alpha° ≈ ${num(rise, 2)} м |
| Krokev dlouhá 5 m má sklon 35° (sin 35° ≈ 0,57; cos 35° ≈ 0,82). Jaká je její půdorysná délka? | Стропило длиной 5 м наклонено под углом 35° (sin 35° ≈ 0,57; cos 35° ≈ 0,82). Какова длина его горизонтальной проекции? |
| 4,10 m | 4,10 м |
| 2,87 m | 2,87 м |
| 2,87 = 5 · sin 35° je výška – odvěsna naproti úhlu. Půdorys leží u úhlu. | 2,87 = 5 · sin 35° — это высота, катет напротив угла. Проекция лежит у угла. |
| 6,10 m | 6,10 м |
| Půdorys nemůže být delší než krokev sama. Násobit, nebo dělit? | Проекция не может быть длиннее самого стропила. Умножать или делить? |
| 5 m | 5 м |
| 5 m by to bylo jen u vodorovného nosníku. | 5 м было бы только у горизонтальной балки. |
| Půdorys leží u úhlu, proto kosinus: 5 · cos 35° ≈ 4,10 m. | Проекция лежит у угла, поэтому косинус: 5 · cos 35° ≈ 4,10 м. |
| Tíha působí svisle dolů, takže její rameno je vodorovná vzdálenost – měř ji v půdorysu, ne po šikmé krokvi. | Вес действует вертикально вниз, поэтому его плечо — горизонтальное расстояние: измеряй его по проекции, а не вдоль наклонного стропила. |

### FinalTask

| čeština | русский |
|---|---|
| Nosník s kloubovou podporou A a šikmou silou 12 kN ve vzdálenosti 2 m | Балка с шарнирной опорой A и наклонной силой 12 кН на расстоянии 2 м |
| Síla [[F]] = 12 kN působí 2 m od bodu A, míří šikmo dolů doleva a s nosníkem svírá 60° (sin 60° ≈ 0,866; cos 60° = 0,5). Urči složky i se znaménky a moment k bodu A (↺ kladný). Znaménko přepneš tlačítkem ± v poli. | Сила [[F]] = 12 кН приложена в 2 м от точки A, направлена наклонно вниз влево и составляет с балкой 60° (sin 60° ≈ 0,866; cos 60° = 0,5). Найди составляющие со знаками и момент относительно точки A (↺ положительный). Знак переключается кнопкой ± в поле. |
| [[F_x]] | [[F_x]] |
| [[F_y]] | [[F_y]] |
| [[M_A]] | [[M_A]] |
| [[F_x]] = −12 · cos 60° = −6 kN, [[F_y]] = −12 · sin 60° ≈ −10,39 kN. K bodu A otáčí jen [[F_y]]: [[M_A]] ≈ −10,39 · 2 ≈ −20,78 kNm (↻); [[F_x]] leží v ose nosníku a rameno nemá. | [[F_x]] = −12 · cos 60° = −6 кН, [[F_y]] = −12 · sin 60° ≈ −10,39 кН. Вокруг A вращает только [[F_y]]: [[M_A]] ≈ −10,39 · 2 ≈ −20,78 кН·м (↻); [[F_x]] лежит на оси балки, плеча у неё нет. |
| Vypadá to na prohozený sin a cos. Která složka leží **u** úhlu 60°? | Похоже, sin и cos перепутаны. Какая составляющая лежит **у** угла 60°? |
| Velikost [[F_x]] sedí. Kam míří šipka vodorovně – doprava (+), nebo doleva (−)? | Величина [[F_x]] верна. Куда стрелка направлена по горизонтали — вправо (+) или влево (−)? |
| Velikost [[F_y]] sedí. Míří šipka nahoru (+), nebo dolů (−)? | Величина [[F_y]] верна. Стрелка направлена вверх (+) или вниз (−)? |
| Složka nemůže být stejně velká jako celá síla 12 kN – je to jen její část. Rozlož sílu pomocí cos a sin. | Составляющая не может равняться всей силе 12 кН — это только её часть. Разложи силу через cos и sin. |
| Celá síla krát vzdálenost platí jen pro kolmou sílu. Otáčí jen [[F_y]] – nebo vezmi rameno 2 · sin 60°. | Вся сила на расстояние — только для перпендикулярной силы. Вращает лишь [[F_y]] — или возьми плечо 2 · sin 60°. |
| Velikost momentu sedí. Kterým směrem síla kolem A otáčí – ↺, nebo ↻? | Величина момента верна. В какую сторону сила вращает вокруг A — ↺ или ↻? |
| Procvičování → | Практика → |
| Trigonometrie ve statice | Тригонометрия в статике |
| Složky, výslednice, její úhel a rameno momentu – všechno z jednoho trojúhelníku. | Составляющие, равнодействующая, её угол и плечо момента — всё из одного треугольника. |

## Slovníček / Словарик

| čeština | русский |
|---|---|
| Pravoúhlý trojúhelník | Прямоугольный треугольник (pravoúhlý trojúhelník) |
| Trojúhelník s jedním pravým úhlem (90°). | Треугольник с одним прямым углом (90°). |
| Přepona | Гипотенуза (přepona) |
| Nejdelší strana, leží naproti pravému úhlu. | Самая длинная сторона, лежит напротив прямого угла. |
| Odvěsna přilehlá | Прилежащий катет (přilehlá odvěsna) |
| Odvěsna, která vychází z vrcholu daného úhlu. | Катет, выходящий из вершины данного угла. |
| Odvěsna protilehlá | Противолежащий катет (protilehlá odvěsna) |
| Odvěsna naproti danému úhlu – nedotýká se ho. | Катет напротив данного угла — он его не касается. |
| Podobné trojúhelníky | Подобные треугольники (podobné) |
| Mají stejné úhly; strany jsou ve stejném poměru. | У них одинаковые углы; стороны находятся в одном и том же отношении. |
| Goniometrické funkce | Тригонометрические функции (goniometrické funkce) |
| sin, cos, tg – podíly stran pravoúhlého trojúhelníku, závisí jen na úhlu. | sin, cos, tg — отношения сторон прямоугольного треугольника, зависят только от угла. |
| Sinus (sin α) | Синус (sin α) |
| Protilehlá odvěsna / přepona. | Противолежащий катет / гипотенуза. |
| Kosinus (cos α) | Косинус (cos α) |
| Přilehlá odvěsna / přepona. | Прилежащий катет / гипотенуза. |
| Tangens (tg α, na kalkulačce tan) | Тангенс (tg α, на калькуляторе tan) |
| Protilehlá / přilehlá odvěsna; také sin α / cos α. Udává sklon. | Противолежащий / прилежащий катет; также sin α / cos α. Задаёт уклон. |
| Stupeň (°) | Градус (°) |
| Jednotka úhlu: pravý úhel = 90°, plný úhel (celá otáčka) = 360°. | Единица угла: прямой угол = 90°, полный оборот = 360°. |
| Radián | Радиан |
| Jednotka obloukové míry: úhel jako délka oblouku na kružnici s poloměrem 1. 180° = π rad ≈ 3,14 rad. | Единица радианной меры: угол как длина дуги на окружности радиуса 1. 180° = π рад ≈ 3,14 рад. |
| DEG / RAD / GRAD | DEG / RAD / GRAD |
| Režimy kalkulačky: stupně, radiány, grady. Pro stupně musí být DEG: sin 30 = 0,5. | Режимы калькулятора: градусы, радианы, грады. Для градусов нужен DEG: sin 30 = 0,5. |
| arcsin, arccos, arctg (sin⁻¹, cos⁻¹, tan⁻¹) | arcsin, arccos, arctg (sin⁻¹, cos⁻¹, tan⁻¹) |
| Inverzní (obrácené) funkce: z podílu stran vrátí úhel. sin⁻¹ neznamená 1/sin. | Обратные функции (inverzní): по отношению сторон возвращают угол. sin⁻¹ не означает 1/sin. |
| Pythagorova věta | Теорема Пифагора |
| a² + b² = c², kde c je přepona. | a² + b² = c², где c — гипотенуза. |
| Odmocnina (√) | Квадратный корень (odmocnina, √) |
| Číslo, které samo se sebou vynásobené dá dané číslo: √25 = 5, √2 ≈ 1,41. | Число, которое при умножении само на себя даёт данное: √25 = 5, √2 ≈ 1,41. |
| Ostrý úhel | Острый угол (ostrý úhel) |
| Úhel menší než 90°. | Угол меньше 90°. |
| Doplňkový úhel | Дополнительный угол (doplňkový) |
| Druhý ostrý úhel, 90° − α. Platí sin α = cos (90° − α). | Второй острый угол, 90° − α. Верно sin α = cos (90° − α). |
| Jednotková kružnice | Единичная окружность (jednotková kružnice) |
| Kružnice s poloměrem 1; bod na ní má souřadnice (cos φ, sin φ). | Окружность радиуса 1; точка на ней имеет координаты (cos φ, sin φ). |
| Kvadrant | Квадрант, четверть (kvadrant) |
| Jedna ze čtyř částí roviny mezi osami; určuje znaménka sin a cos. | Одна из четырёх частей плоскости между осями; определяет знаки sin и cos. |
| Sklon | Уклон (sklon) |
| Výška / vodorovná délka = tg α. Zadává se poměrem výška : délka (1 : 2, 3 : 4) nebo v % (tg α · 100). | Высота / горизонтальная длина = tg α. Задаётся отношением высота : длина (1 : 2, 3 : 4) или в % (tg α · 100). |
| Půdorysná délka (vodorovný průmět) | Горизонтальная проекция (půdorysná délka) |
| Vodorovná délka šikmého nosníku: L · cos α. Převýšení je L · sin α. | Горизонтальная длина наклонной балки: L · cos α. Высота подъёма — L · sin α. |

## Procvičování / Тренировка

### Sady a obrazovka

| čeština | русский |
|---|---|
| K lekci $a | К уроку $a |
| K lekcím $a–$b | К урокам $a–$b |
| Strany a podíly | Стороны и отношения |
| Kalkulačka a známé úhly | Калькулятор и известные углы |
| Pythagoras a vztahy | Пифагор и связи |
| Úhel a sklon | Угол и уклон |
| Znaménka složek | Знаки составляющих |
| Trigonometrie ve statice | Тригонометрия в статике |
| Závěrečný mix | Итоговый микс |

### Typické chyby (Traps)

| čeština | русский |
|---|---|
| Vypadá to na prohozený sin a cos. Která strana leží **u** úhlu? | Похоже, sin и cos перепутаны. Какая сторона лежит **у** угла? |
| Tohle vyjde, když je kalkulačka v režimu RAD. Přepni na DEG a ověř, že sin 30 = 0,5. | Так получается, когда калькулятор в режиме RAD. Переключи на DEG и проверь, что sin 30 = 0,5. |
| Kalkulačka je nejspíš v režimu GRAD (na displeji G). Přepni na DEG. | Калькулятор, скорее всего, в режиме GRAD (на дисплее G). Переключи на DEG. |
| Složka nemůže být stejně velká jako celá síla – je to jen její část. Rozlož sílu pomocí cos a sin. | Составляющая не может равняться всей силе — это только её часть. Разложи силу через cos и sin. |
| Odvěsna nemůže být delší než přepona. Přeponu funkcí násob, neděl. | Катет не может быть длиннее гипотенузы. Гипотенузу на функцию умножай, а не дели. |
| Přepona je nejdelší strana – musí vyjít víc než odvěsna. Dělit, nebo násobit? | Гипотенуза — самая длинная сторона, она должна получиться больше катета. Делить или умножать? |
| Sinus ani kosinus nejsou nikdy větší než 1 – přepona patří ve zlomku dolů. | Ни синус, ни косинус не бывают больше 1 — гипотенуза в дроби стоит снизу. |
| Sčítají se druhé mocniny, ne strany. | Складываются квадраты, а не стороны. |
| To je teprve druhá mocnina výsledku. Ještě odmocni. | Это пока квадрат результата. Осталось извлечь корень. |
| To je druhý ostrý úhel (u druhé odvěsny). Zkontroluj, co je ve zlomku nahoře. | Это второй острый угол (у другого катета). Проверь, что в дроби сверху. |
| To není úhel – tohle vyjde po sin/cos/tan místo sin⁻¹/cos⁻¹/tan⁻¹ (SHIFT, 2nd nebo INV). | Это не угол — так выходит, если нажать sin/cos/tan вместо sin⁻¹/cos⁻¹/tan⁻¹ (SHIFT, 2nd или INV). |
| To je teprve podíl stran. Úhel z něj dostaneš obrácenou funkcí. | Это пока отношение сторон. Угол из него получают обратной функцией. |
| Tohle vyjde s jinou funkcí, než jaká patří k těmto dvěma stranám. Které strany znáš – odvěsny, nebo i přeponu? | Так выходит с другой функцией, не той, что соответствует этим двум сторонам. Какие стороны известны — катеты или ещё и гипотенуза? |
| Procenta nejsou stupně. Sklon v % = tg α · 100. | Проценты — не градусы. Уклон в % = tg α · 100. |
| 100 % není 90°, ale 45°. Procenta se nepřepočítávají přímou úměrou – použij tangens. | 100 % — это не 90°, а 45°. Проценты не переводятся пропорцией — нужен тангенс. |
| Nejdřív převeď procenta na číslo: vyděl stem. | Сначала переведи проценты в число: раздели на сто. |
| Velikost [[F_x]] sedí. Kam míří šipka vodorovně – doprava (+), nebo doleva (−)? | Величина [[F_x]] верна. Куда стрелка направлена по горизонтали — вправо (+) или влево (−)? |
| Velikost [[F_y]] sedí. Míří šipka nahoru (+), nebo dolů (−)? | Величина [[F_y]] верна. Стрелка направлена вверх (+) или вниз (−)? |
| Úhel je tady měřený od **svislice**. Která složka leží u úhlu teď? | Здесь угол отсчитан от **вертикали**. Какая составляющая теперь лежит у угла? |
| Do arctg dej velikosti bez znamének – hledáme ostrý (kladný) úhel. Směr síly je vidět ze znamének složek. | В arctg подставляй величины без знаков — ищем острый (положительный) угол. Направление силы видно по знакам составляющих. |
| Tady se dělí druhou odvěsnou – to je tangens. Pro složku je potřeba přepona: √(m² + n²). | Здесь делят на другой катет — это тангенс. Для составляющей нужна гипотенуза: √(m² + n²). |
| F · r platí jen pro sílu kolmou k páce. Rameno je [[r_⊥]] = r · sin α. | F · r верно только для силы, перпендикулярной рычагу. Плечо — [[r_⊥]] = r · sin α. |
| Rameno leží **naproti** úhlu α (mezi silou a pákou) – proto sinus. | Плечо лежит **напротив** угла α (между силой и рычагом) — поэтому синус. |
| Do součtu patří jen složka šikmé síly, ne celá síla. | В сумму входит только составляющая наклонной силы, а не вся сила. |

### Společné texty úloh

| čeština | русский |
|---|---|
| Výsledek: ${ans.cs} | Ответ: ${ans.ru} |
| Ještě to nesedí. Zkus nápovědu. | Пока не сходится. Попробуй подсказку. |
| Správně! | Верно! |
| Skoro! Počítej s nezaokrouhlenými čísly a výsledek zapiš na 2 desetinná místa (úhel na 1). | Почти! Считай с неокруглёнными числами, а ответ запиши с 2 знаками после запятой (угол — с 1). |
| Doplň všechna pole. | Заполни все поля. |
| Zapiš jen číslo, např. 0,6 (bez zlomku a jednotky). | Введи только число, например 0,6 (без дроби и единиц). |

### Obrazovka procvičování

| čeština | русский |
|---|---|
| Lehká | Лёгкая |
| Střední | Средняя |
| Těžká | Трудная |
| Procvičování | Практика |
| Lehká: kulatá čísla, kalkulačka jen někde · Střední: s kalkulačkou · Těžká: úhly od svislice, znaménka | Лёгкая: круглые числа, калькулятор нужен редко · Средняя: с калькулятором · Трудная: углы от вертикали, знаки |
| z | из |
| ${set.size} úloh | ${set.size} задач |
| Rozpracováno: úloha ${r.index + 1}/${r.tasks.size} | В процессе: задача ${r.index + 1}/${r.tasks.size} |
| Doporučujeme nejdřív lekci ${set.afterLesson % 100} | Советуем сначала урок ${set.afterLesson % 100} |
| z | из |
| Série ${session.seed} | Серия ${session.seed} |
| úloha | задача |
| Tahle série se připravuje. | Эта серия готовится. |
| Začít novou sérii? | Начать новую серию? |
| Začít novou sérii na úrovni ${LEVELS[lv - 1].cs}? Rozdělaná série se zahodí. | Начать новую серию на уровне «${LEVELS[lv - 1].ru}»? Текущая серия пропадёт. |
| Začít novou | Начать новую |
| Zůstat | Остаться |
| Série $current · zadat jiné číslo série | Серия $current · ввести другой номер серии |
| Číslo série | Номер серии |
| Spustit | Запустить |
| Stejné číslo = stejné úlohy (třeba od učitele). | Один номер — одни и те же задачи (например, от учителя). |
| Bod je jen za první pokus. | Балл — только с первой попытки. |
| Zaokrouhli na 2 desetinná místa (úhly na 1). Čárka i tečka fungují. | Округляй до 2 знаков после запятой (углы — до 1). Можно и запятую, и точку. |
|  Znaménko přepneš tlačítkem ± v poli. |  Знак переключается кнопкой ± в поле. |
| Zkontrolovat | Проверить |
| Ukázat výsledek (bez bodu) | Показать ответ (без балла) |
| Další | Дальше |
| Vyřešeno $score z $size · napoprvé ${session.firstTries} · nápovědy: $hints | Решено $score из $size · с первой попытки ${session.firstTries} · подсказок: $hints |
| Zvládnuto: ${LEVELS[session.level - 1].cs} ★ | Освоено: ${LEVELS[session.level - 1].ru} ★ |
| Na ★ je potřeba aspoň ${(size * 3 + 3) / 4} úloh za bod. | Для ★ нужно не меньше ${(size * 3 + 3) / 4} задач с баллом. |
| Bod je za úlohu bez výsledku z nápovědy – u volby jen napoprvé, u čísel nejvýš po $MAX_MISSES chybách. | Балл — за задачу без готового ответа из подсказки: в выборе — только с первой попытки, в числах — не больше $MAX_MISSES ошибок. |
| Na co se ještě podívat | Что стоит повторить |
| Těžší úroveň | Уровень сложнее |
| Další série | Ещё серия |
| Další série | Ещё серия |
| Zkusit znovu (${levelName.cs}) | Ещё раз (${levelName.ru}) |
| Seznam sérií | Список серий |

### Úloha Common

| čeština | русский |
|---|---|
| To je teprve podíl stran (${fn.label} α) – nebo je kalkulačka v RAD. Úhel dostaneš přes $key⁻¹ v režimu DEG. | Это пока отношение сторон (${fn.label} α) — или калькулятор в RAD. Угол получишь через $key⁻¹ в режиме DEG. |
|  Na telefonu: otoč Kalkulačku na šířku (iPhone) nebo rozbal panel funkcí (Android) – objeví se sin, cos, tan a přepínač Deg/Rad; tan⁻¹ je pod 2nd (iPhone) nebo INV (Android). |  На телефоне: поверни Калькулятор горизонтально (iPhone) или разверни панель функций (Android) — появятся sin, cos, tan и переключатель Deg/Rad; tan⁻¹ — под 2nd (iPhone) или INV (Android). |

### Úloha E01

| čeština | русский |
|---|---|
| Názvy stran | Названия сторон |
| přepona | гипотенуза |
| přilehlá odvěsna | прилежащий катет |
| protilehlá odvěsna | противолежащий катет |
| Přepona leží naproti pravému úhlu. Zvýrazněná strana ale k pravému úhlu přiléhá – je to odvěsna. | Гипотенуза лежит напротив прямого угла. А выделенная сторона прилегает к прямому углу — это катет. |
| Zvýrazněná strana leží naproti pravému úhlu (značka ⦜) a je nejdelší. Jak se jí říká? | Выделенная сторона лежит напротив прямого угла (значок ⦜) и она самая длинная. Как она называется? |
| Zvýrazněná strana vychází přímo z vrcholu úhlu α – dotýká se ho. Je u úhlu, ne naproti. | Выделенная сторона выходит прямо из вершины угла α — она его касается. Она у угла, а не напротив. |
| Zvýrazněná strana se vrcholu úhlu α nedotýká – leží naproti němu. | Выделенная сторона не касается вершины угла α — она лежит напротив него. |
| Jak se jmenuje **zvýrazněná** strana vzhledem k vyznačenému úhlu α? | Как называется **выделенная** сторона по отношению к отмеченному углу α? |
| Najdi pravý úhel – naproti němu je přepona. Zbylé dvě strany jsou odvěsny. | Найди прямой угол — напротив него гипотенуза. Две остальные стороны — катеты. |
| Je zvýrazněná strana odvěsna (svírá pravý úhel)? Pak: vychází-li z vrcholu úhlu α, je přilehlá; jinak protilehlá. Strana naproti pravému úhlu je přepona. | Выделенная сторона — катет (образует прямой угол)? Тогда: если она выходит из вершины угла α — прилежащий, иначе противолежащий. Сторона напротив прямого угла — гипотенуза. |
| Správně – je to ${names.getValue(target).cs}. | Верно — это ${names.getValue(target).ru}. |

### Úloha E02

| čeština | русский |
|---|---|
| Podíl stran | Отношение сторон |
| protilehlá | противолежащий |
| přepona | гипотенуза |
| přilehlá | прилежащий |
| přepona | гипотенуза |
| protilehlá | противолежащий |
| přilehlá | прилежащий |
| ${fn.label} α | ${fn.label} α |
| Obrácený zlomek: tangens je **protilehlá / přilehlá** – nahoře odvěsna naproti úhlu. | Перевёрнутая дробь: тангенс — **противолежащий / прилежащий**, сверху катет напротив угла. |
| Tady se dělí odvěsna odvěsnou – to je tangens. Sinus i kosinus dělí přeponou. | Здесь катет делится на катет — это тангенс. И синус, и косинус делят на гипотенузу. |
| Dělíš přeponou – to je sinus. V tangentu přepona vůbec není. | Ты делишь на гипотенузу — это синус. В тангенсе гипотенузы нет. |
| Správně: ${fn.label} α = ${n(a)} / ${n(b)} ${approx(ans, 3)}. | Верно: ${fn.label} α = ${n(a)} / ${n(b)} ${approx(ans, 3)}. |
|  |  |
| Chybějící přepona: $t. Pak  | Недостающая гипотенуза: $t. Тогда  |
| Chybějící odvěsna: $t. Pak  | Недостающий катет: $t. Тогда  |
| Chybějící odvěsna: $t. Pak  | Недостающий катет: $t. Тогда  |

### Úloha E03

| čeština | русский |
|---|---|
| Odvěsna z přepony | Катет по гипотенузе |
| Hledaná strana leží **naproti** úhlu → sin. Odvěsna = přepona · sin α. | Искомая сторона лежит **напротив** угла → sin. Катет = гипотенуза · sin α. |
| Hledaná strana leží **u** úhlu → cos. Odvěsna = přepona · cos α. | Искомая сторона лежит **у** угла → cos. Катет = гипотенуза · cos α. |
| $cs · $fn $al° = $cs · ${fmt(if (opposite) s else co, 3)} | $cs · $fn $al° = $cs · ${fmt(if (opposite) s else co, 3)} |
| To je délka přepony. Odvěsna je vždy kratší – vynásob ji sin nebo cos. | Это длина гипотенузы. Катет всегда короче — умножь её на sin или cos. |
| Správně: $cs · $fn $al° ${approx(ans)} m. | Верно: $cs · $fn $al° ${approx(ans)} м. |

### Úloha E04

| čeština | русский |
|---|---|
| Přepona z odvěsny | Гипотенуза по катету |
| Odvěsna $sideCs měří ${n(a)} m, úhel α = ${deg(al)}. Jak dlouhá je přepona? | Катет $sideRu равен ${n(a)} м, угол α = ${deg(al)}. Какова длина гипотенузы? |
| přepona | гипотенуза |
| ${fn.label} α = odvěsna / přepona, takže **přepona = odvěsna / ${fn.label} α**. | ${fn.label} α = катет / гипотенуза, значит **гипотенуза = катет / ${fn.label} α**. |
| ${n(a)} / ${fn.label} ${deg(al)} = ${n(a)} / ${n(fv, 3)} | ${n(a)} / ${fn.label} ${deg(al)} = ${n(a)} / ${n(fv, 3)} |
| Správně: ${n(a)} / ${fn.label} ${deg(al)} ${approx(ans)} m. | Верно: ${n(a)} / ${fn.label} ${deg(al)} ${approx(ans)} м. |

### Úloha E05

| čeština | русский |
|---|---|
| Odvěsna z odvěsny (tangens) | Катет по катету (тангенс) |
| Sloup vrhá na rovné zemi stín dlouhý ${n(b)} m. Slunce svítí pod úhlem ${deg(al)} nad obzorem. Jak vysoký je sloup? | Столб отбрасывает на ровную землю тень длиной ${n(b)} м. Солнце светит под углом ${deg(al)} над горизонтом. Какой высоты столб? |
| Rampa je vodorovně dlouhá ${n(b)} m a stoupá pod úhlem ${deg(al)}. Do jaké výšky vede? | Пандус имеет горизонтальную длину ${n(b)} м и поднимается под углом ${deg(al)}. На какую высоту он ведёт? |
| výška | высота |
| Přepona tu nehraje roli – obě strany jsou odvěsny, tedy **tangens**: tg α = protilehlá / přilehlá. | Гипотенуза здесь не участвует — обе стороны катеты, значит **тангенс**: tg α = противолежащий / прилежащий. |
| ${n(b)} · tg ${deg(al)} = ${n(b)} · ${n(tv, 3)} | ${n(b)} · tg ${deg(al)} = ${n(b)} · ${n(tv, 3)} |
| Tangens = protilehlá / přilehlá. Hledáš tu nahoře – násob, neděl. | Тангенс = противолежащий / прилежащий. Ищешь тот, что сверху, — умножай, а не дели. |
| Sinus i kosinus potřebují přeponu – ta tu ale není. Obě známé strany jsou odvěsny, tedy tangens. | Синусу и косинусу нужна гипотенуза, а её здесь нет. Обе известные стороны — катеты, значит тангенс. |
| Správně: ${n(b)} · tg ${deg(al)} ${approx(ans)} m. | Верно: ${n(b)} · tg ${deg(al)} ${approx(ans)} м. |

### Úloha E06

| čeština | русский |
|---|---|
| Známé úhly | Известные углы |
| To je hodnota pro ${90 - a}°. Tabulky sin a cos jsou navzájem pozpátku: sin $a° = cos ${90 - a}°. | Это значение для ${90 - a}°. Таблицы sin и cos зеркальны: sin $a° = cos ${90 - a}°. |
| To je tg ${90 - a}°. U tangentu se hodnoty s rostoucím úhlem zvětšují: tg 0° = 0, tg 45° = 1. | Это tg ${90 - a}°. Тангенс с ростом угла растёт: tg 0° = 0, tg 45° = 1. |
| Tahle hodnota patří k jinému úhlu. Tangens roste: tg 0° = 0, tg 30° ≈ 0,577, tg 45° = 1, tg 60° ≈ 1,732, tg 90° není definován. | Это значение другого угла. Тангенс растёт: tg 0° = 0, tg 30° ≈ 0,577, tg 45° = 1, tg 60° ≈ 1,732, tg 90° не определён. |
| Tahle hodnota patří k jinému úhlu. Najdi úhel v řadě 0 – 0,5 – 0,707 – 0,866 – 1 (sin pro 0°, 30°, 45°, 60°, 90°; cos pozpátku). | Это значение другого угла. Найди угол в ряду 0 – 0,5 – 0,707 – 0,866 – 1 (sin для 0°, 30°, 45°, 60°, 90°; cos — в обратном порядке). |
| Bez kalkulačky: kolik je ${fn.label} $a°? | Без калькулятора: чему равен ${fn.label} $a°? |
| tg α = sin α / cos α. V trojúhelníku 45°–45°–90° jsou odvěsny stejné, v 30°–60°–90° mají strany 1, √3 a 2. | tg α = sin α / cos α. В треугольнике 45°–45°–90° катеты равны, в 30°–60°–90° стороны 1, √3 и 2. |
| Řada (√0)/2, (√1)/2, (√2)/2, (√3)/2, (√4)/2 patří k 0°, 30°, 45°, 60°, 90° – pro sin zleva, pro cos zprava. √0 = 0, √1 = 1, √4 = 2. | Ряд (√0)/2, (√1)/2, (√2)/2, (√3)/2, (√4)/2 соответствует 0°, 30°, 45°, 60°, 90° — для sin слева, для cos справа. √0 = 0, √1 = 1, √4 = 2. |
| tg 30° = kratší odvěsna / delší = 1/√3 ≈ 0,577 (píše se i √3/3); tg 45° = 1; tg 60° = delší / kratší = √3 ≈ 1,732. | tg 30° = короткий катет / длинный = 1/√3 ≈ 0,577 (пишут и √3/3); tg 45° = 1; tg 60° = длинный / короткий = √3 ≈ 1,732. |
| V trojúhelníku 30°–60°–90° jsou strany 1, √3, 2; v 45°–45°–90° jsou 1, 1, √2. | В треугольнике 30°–60°–90° стороны 1, √3, 2; в 45°–45°–90° — 1, 1, √2. |
| Správně: ${fn.label} $a° = ${text(correct).cs}. | Верно: ${fn.label} $a° = ${text(correct).ru}. |
| není definován | не определён |
| 1/2 = 0,5 | 1/2 = 0,5 |
| √2/2 ≈ 0,707 | √2/2 ≈ 0,707 |
| √3/2 ≈ 0,866 | √3/2 ≈ 0,866 |
| √3/3 ≈ 0,577 | √3/3 ≈ 0,577 |
| √3 ≈ 1,732 | √3 ≈ 1,732 |

### Úloha E07

| čeština | русский |
|---|---|
| Funkce na kalkulačce | Функция на калькуляторе |
| Spočítej na kalkulačce ${fn.label} $a° (na 3 desetinná místa). | Посчитай на калькуляторе ${fn.label} $a° (до 3 знаков после запятой). |
| ${fn.label} $a° | ${fn.label} $a° |
| To je ${g.label} $a° – jiná funkce. Zkontroluj, která klávesa byla stisknutá. | Это ${g.label} $a° — другая функция. Проверь, какая клавиша нажата. |
| Správně: ${fn.label} $a° ≈ ${n(ans, 3)}. | Верно: ${fn.label} $a° ≈ ${n(ans, 3)}. |

### Úloha E08

| čeština | русский |
|---|---|
| Režim kalkulačky | Режим калькулятора |
| Kalkulačka ukázala $expr = $shown. V jakém je režimu? | Калькулятор показал $expr = $shown. В каком он режиме? |
| Kolik má $expr vyjít ve stupních? Pamatuješ si to z tabulky známých úhlů. | Сколько должно получиться для $expr в градусах? Это есть в таблице известных углов. |
| V RAD bere kalkulačka ${n(a, 0)} jako ${n(a, 0)} radiánů (několik celých otáček); v GRAD má pravý úhel 100 gradů (gonů). | В RAD калькулятор считает ${n(a, 0)} радианами (несколько полных оборотов); в GRAD прямой угол равен 100 град (гон). |

### Úloha E09

| čeština | русский |
|---|---|
| Pythagorova věta | Теорема Пифагора |
| Odvěsny pravoúhlého trojúhelníku měří ${n(a)} m a ${n(b)} m. Jak dlouhá je přepona? | Катеты прямоугольного треугольника равны ${n(a)} м и ${n(b)} м. Какова длина гипотенузы? |
| přepona c | гипотенуза c |
| a² + b² = c². Hledáš přeponu: sečti druhé mocniny odvěsen a odmocni. | a² + b² = c². Ищешь гипотенузу: сложи квадраты катетов и извлеки корень. |
| Správně: c = √${n(a * a + b * b)} ${approx(c)} m. | Верно: c = √${n(a * a + b * b)} ${approx(c)} м. |
| Přepona měří ${n(c)} m a jedna odvěsna ${n(a)} m. Jak dlouhá je druhá odvěsna? | Гипотенуза равна ${n(c)} м, один катет — ${n(a)} м. Какова длина второго катета? |
| odvěsna b | катет b |
| a² + b² = c², takže b² = c² − a². Odvěsna je kratší než přepona – tady se **odečítá**. | a² + b² = c², значит b² = c² − a². Катет короче гипотенузы — здесь **вычитают**. |
| Odvěsna je kratší než přepona – druhé mocniny se tady odečítají, ne sčítají. | Катет короче гипотенузы — квадраты здесь вычитают, а не складывают. |
| Strany se neodečítají přímo – odečítají se jejich druhé mocniny: b² = c² − a². | Стороны не вычитают напрямую — вычитают их квадраты: b² = c² − a². |
| Správně: b = √${n(c * c - a * a)} ${approx(b)} m. | Верно: b = √${n(c * c - a * a)} ${approx(b)} м. |

### Úloha E10

| čeština | русский |
|---|---|
| sin² + cos² = 1 | sin² + cos² = 1 |
| α je ostrý úhel a $given α = ${n(s, 3)}. Kolik je $wanted α? | α — острый угол, $given α = ${n(s, 3)}. Чему равен $wanted α? |
| $wanted α | $wanted α |
| sin² α + cos² α = 1, takže $wanted² α = 1 − $given² α. | sin² α + cos² α = 1, значит $wanted² α = 1 − $given² α. |
| $wanted α = √(1 − ${n(s, 3)}²) = √${n(1 - s * s, 4)} | $wanted α = √(1 − ${n(s, 3)}²) = √${n(1 - s * s, 4)} |
| 1 − $given by platilo, kdyby se sčítaly sin a cos. Sčítají se ale jejich **druhé mocniny**. | 1 − $given было бы верно, если бы складывались sin и cos. Но складываются их **квадраты**. |
| To je teprve $wanted² α. Ještě odmocni. | Это пока $wanted² α. Осталось извлечь корень. |
| Správně: $wanted α = √${n(1 - s * s, 4)} ${approx(ans, 3)}. | Верно: $wanted α = √${n(1 - s * s, 4)} ${approx(ans, 3)}. |

### Úloha E11

| čeština | русский |
|---|---|
| Doplňkový úhel | Дополнительный угол |
| Doplň úhel: $f1 $a° = $f2 ?° | Найди угол: $f1 $a° = $f2 ?° |
| úhel | угол |
| Oba ostré úhly pravoúhlého trojúhelníku dávají dohromady 90°. Co je pro jeden úhel protilehlá odvěsna, je pro druhý přilehlá. | Два острых угла прямоугольного треугольника вместе дают 90°. Катет, противолежащий одному углу, прилежит к другому. |
| 90° − $a° | 90° − $a° |
| $f1 a $f2 téhož úhlu se rovnají jen při 45°. Hledáš druhý ostrý úhel. | $f1 и $f2 одного угла равны только при 45°. Ищем второй острый угол. |
| Doplněk je do 90°, ne do 180°. | Дополнение — до 90°, а не до 180°. |
| Správně: $f1 $a° = $f2 ${90 - a}°. | Верно: $f1 $a° = $f2 ${90 - a}°. |

### Úloha E12

| čeština | русский |
|---|---|
| Úhel ze stran | Угол по сторонам |
| Žebřík (přepona) a zeď tvoří pravoúhlý trojúhelník se stranami podle obrázku. Jaký úhel α svírá žebřík se zemí? (na 1 desetinné místo) | Лестница (гипотенуза) и стена образуют прямоугольный треугольник со сторонами по рисунку. Какой угол α лестница составляет с землёй? (до 1 знака после запятой) |
| Lano je napnuté mezi body podle obrázku. Jaký úhel α svírá s vodorovnou? (na 1 desetinné místo) | Трос натянут между точками по рисунку. Какой угол α он составляет с горизонталью? (до 1 знака после запятой) |
| Střecha má rozměry podle obrázku. Jaký úhel α svírá s vodorovnou? (na 1 desetinné místo) | Размеры крыши — по рисунку. Какой угол α она составляет с горизонталью? (до 1 знака после запятой) |
| Známe dvě strany podle obrázku. Jak velký je úhel α? (na 1 desetinné místo) | Известны две стороны (по рисунку). Чему равен угол α? (до 1 знака после запятой) |
| protilehlá a přepona | противолежащий и гипотенуза |
| přilehlá a přepona | прилежащий и гипотенуза |
| obě odvěsny | оба катета |
| α | α |
| α = ${fn.arc} (${n(x)} / ${n(y)}) = ${fn.arc} ${n(q, 3)} | α = ${fn.arc} (${n(x)} / ${n(y)}) = ${fn.arc} ${n(q, 3)} |
| Správně: α = ${fn.arc} ${n(q, 3)} ≈ ${n(ans, 1)}°. | Верно: α = ${fn.arc} ${n(q, 3)} ≈ ${n(ans, 1)}°. |

### Úloha E13

| čeština | русский |
|---|---|
| Sklon poměrem | Уклон отношением |
| Sklon je $r (výška : vodorovná délka). Jaký úhel svírá s vodorovnou? (na 1 desetinné místo) | Уклон $r (высота : горизонтальная длина). Какой угол он составляет с горизонталью? (до 1 знака после запятой) |
| α | α |
| α = arctg (${n(m)} / ${n(nn)}) = arctg ${n(q, 3)} | α = arctg (${n(m)} / ${n(nn)}) = arctg ${n(q, 3)} |
| To je úhel od svislice – vyjde, když se dělí délka výškou. Nahoře ve zlomku je výška. | Это угол от вертикали — так выходит, если делить длину на высоту. Сверху в дроби стоит высота. |
| ${n(100 * q, 1)} je sklon v procentech, ne úhel. Úhel = arctg ${n(q, 3)}. | ${n(100 * q, 1)} — это уклон в процентах, а не угол. Угол = arctg ${n(q, 3)}. |
| Poměr výška : délka je tangens, ne sinus – použij tan⁻¹. | Отношение высота : длина — это тангенс, а не синус: нужен tan⁻¹. |
| Správně: α = arctg ${n(q, 3)} ≈ ${n(ans, 1)}°. | Верно: α = arctg ${n(q, 3)} ≈ ${n(ans, 1)}°. |

### Úloha E14

| čeština | русский |
|---|---|
| Sklon v procentech | Уклон в процентах |
| $cs má sklon ${n(p)} %. Jaký úhel svírá s vodorovnou? (na 1 desetinné místo) | $ru ${n(p)} %. Какому углу к горизонтали это соответствует? (до 1 знака после запятой) |
| α | α |
| α = arctg ${n(q, 3)} | α = arctg ${n(q, 3)} |
| Správně: tg α = ${n(q, 3)}, α ≈ ${n(ans, 1)}°. | Верно: tg α = ${n(q, 3)}, α ≈ ${n(ans, 1)}°. |
| Rampa stoupá pod úhlem ${deg(a)}. Jaký je její sklon v procentech? (na 1 desetinné místo) | Пандус поднимается под углом ${deg(a)}. Каков его уклон в процентах? (до 1 знака после запятой) |
| Střecha svírá s vodorovnou úhel ${deg(a)}. Jaký je její sklon v procentech? (na 1 desetinné místo) | Скат крыши составляет с горизонталью угол ${deg(a)}. Каков его уклон в процентах? (до 1 знака после запятой) |
| sklon | уклон |
| Sklon v % = tg α · 100 (výška na 100 m vodorovné délky). | Уклон в % = tg α · 100 (высота на 100 м горизонтальной длины). |
| tg ${deg(a)} · 100 = ${n(Fn.TG.deg(a), 3)} · 100 | tg ${deg(a)} · 100 = ${n(Fn.TG.deg(a), 3)} · 100 |
| Sklon se měří k **vodorovné** délce – to je tangens, ne sinus. | Уклон измеряют относительно **горизонтальной** длины — это тангенс, а не синус. |
| Správně: tg ${deg(a)} · 100 ≈ ${n(ans, 1)} %. | Верно: tg ${deg(a)} · 100 ≈ ${n(ans, 1)} %. |

### Úloha E15

| čeština | русский |
|---|---|
| Převýšení ze sklonu | Превышение по уклону |
| převýšení | превышение |
| šikmá délka | длина по склону |

### Úloha E16

| čeština | русский |
|---|---|
| Znaménka složek | Знаки составляющих |
| Směr síly určuje úhel φ = ${deg(phi)}, měřený od kladné poloosy x proti směru hodinových ručiček. Jaká znaménka mají její složky? | Направление силы задано углом φ = ${deg(phi)}, отсчитанным от положительной полуоси x против часовой стрелки. Какие знаки у её составляющих? |
| Jaká znaménka mají složky síly na obrázku? | Какие знаки у составляющих силы на рисунке? |
| Obě znaménka jsou obráceně. Doprava a nahoru je +, doleva a dolů −. | Оба знака наоборот. Вправо и вверх +, влево и вниз −. |
| Doprava +, doleva −, nahoru +, dolů −. Na velikosti úhlu nezáleží. | Вправо +, влево −, вверх +, вниз −. Величина угла не важна. |
| Správně: ${lab(sx, sy).cs}. | Верно: ${lab(sx, sy).ru}. |

### Úloha E17

| čeština | русский |
|---|---|
| Složky síly | Составляющие силы |
| šikmo nahoru doprava | наклонно вверх вправо |
| šikmo nahoru doleva | наклонно вверх влево |
| šikmo dolů doleva | наклонно вниз влево |
| šikmo dolů doprava | наклонно вниз вправо |
| Síla [[F]] = $fs kN míří ${dir.cs} a se **svislicí** svírá ${fmt(shown, 0)}°. Urči složky i se znaménky. | Сила [[F]] = $fs кН направлена ${dir.ru} и составляет с **вертикалью** ${fmt(shown, 0)}°. Найди составляющие со знаками. |
| Síla [[F]] = $fs kN. Její směr určuje úhel φ = ${fmt(shown, 0)}°, měřený od kladné poloosy x proti směru hodinových ručiček. Urči složky i se znaménky. | Сила [[F]] = $fs кН. Её направление задано углом φ = ${fmt(shown, 0)}°, отсчитанным от положительной полуоси x против часовой стрелки. Найди составляющие со знаками. |
| [[F_x]] | [[F_x]] |
| [[F_y]] | [[F_y]] |
| Úhel β = ${fmt(shown, 0)}° leží u **svislice**: u úhlu je svislá složka → [[F_y]] = F · cos β, naproti vodorovná → [[F_x]] = F · sin β. Znaménka podle šipky. | Угол β = ${fmt(shown, 0)}° лежит у **вертикали**: у угла — вертикальная составляющая → [[F_y]] = F · cos β, напротив — горизонтальная → [[F_x]] = F · sin β. Знаки — по стрелке. |
| Nejdřív najdi ostrý úhel k vodorovné: rozdíl φ od nejbližší vodorovné poloosy (180° nebo 360°, u II. kvadrantu 180° − φ). Pak se podívej, kam šipka míří. | Сначала найди острый угол к горизонтали: разность φ и ближайшей горизонтальной полуоси (180° или 360°, во II четверти 180° − φ). Потом посмотри, куда направлена стрелка. |
| [[F_x]] = ${sgn(sx)}$fs · sin ${fmt(shown, 0)}°, [[F_y]] = ${sgn(sy)}$fs · cos ${fmt(shown, 0)}° | [[F_x]] = ${sgn(sx)}$fs · sin ${fmt(shown, 0)}°, [[F_y]] = ${sgn(sy)}$fs · cos ${fmt(shown, 0)}° |
| [[F_x]] = ${sgn(sx)}$fs · cos ${fmt(a, 0)}°, [[F_y]] = ${sgn(sy)}$fs · sin ${fmt(a, 0)}° | [[F_x]] = ${sgn(sx)}$fs · cos ${fmt(a, 0)}°, [[F_y]] = ${sgn(sy)}$fs · sin ${fmt(a, 0)}° |
| Velikosti sedí, ale obě znaménka jsou obráceně. Doprava a nahoru +, doleva a dolů −. | Величины верны, но оба знака наоборот. Вправо и вверх +, влево и вниз −. |
| Složka u úhlu = F · cos α, složka naproti = F · sin α; znaménko podle toho, kam míří šipka. | Составляющая у угла = F · cos α, напротив — F · sin α; знак — по направлению стрелки. |
| Správně: [[F_x]] ${approx(if (signed) fx else abs(fx))} kN, [[F_y]] ${approx(if (signed) fy else abs(fy))} kN. | Верно: [[F_x]] ${approx(if (signed) fx else abs(fx))} кН, [[F_y]] ${approx(if (signed) fy else abs(fy))} кН. |

### Úloha E18

| čeština | русский |
|---|---|
| Úhly přes 90° | Углы больше 90° |
| Kolik je ${fn.label} ${deg(phi)}? (i se znaménkem) | Чему равен ${fn.label} ${deg(phi)}? (со знаком) |
| ${fn.label} ${deg(phi)} | ${fn.label} ${deg(phi)} |
| Najdi ostrý úhel k ose x (tady ${deg(ref)}) a znaménko podle kvadrantu: cos je vodorovná souřadnice, sin svislá. | Найди острый угол к оси x (здесь ${deg(ref)}) и знак по четверти: cos — горизонтальная координата, sin — вертикальная. |
| ${fn.label} ${deg(phi)} = $sign${fn.label} ${deg(ref)} | ${fn.label} ${deg(phi)} = $sign${fn.label} ${deg(ref)} |
| Velikost sedí. Na které straně osy leží bod na kružnici? Vpravo/nahoře +, vlevo/dole −. | Величина верна. С какой стороны от оси лежит точка на окружности? Справа/сверху +, слева/снизу −. |
| Správně: ${fn.label} ${deg(phi)} ${approx(ans, 3)}. | Верно: ${fn.label} ${deg(phi)} ${approx(ans, 3)}. |

### Úloha E19

| čeština | русский |
|---|---|
| Výslednice ze složek | Равнодействующая по составляющим |
| doprava nahoru | вправо вверх |
| doleva nahoru | влево вверх |
| doleva dolů | влево вниз |
| doprava dolů | вправо вниз |
| Složky síly jsou [[F_x]] = ${n(x)} kN a [[F_y]] = ${n(y)} kN. Jak velká je celá síla R a jaký ostrý úhel α svírá s vodorovnou? (úhel na 1 desetinné místo) | Составляющие силы: [[F_x]] = ${n(x)} кН и [[F_y]] = ${n(y)} кН. Какова величина всей силы R и какой острый угол α она составляет с горизонталью? (угол — до 1 знака после запятой) |
| R | R |
| α | α |
| Velikost: Pythagorova věta R = √([[F_x]]² + [[F_y]]²). Úhel: tg α = \|[[F_y]]\| / \|[[F_x]]\| – bez znamének. | Величина: теорема Пифагора R = √([[F_x]]² + [[F_y]]²). Угол: tg α = \|[[F_y]]\| / \|[[F_x]]\| — без знаков. |
| R = √(${n(ax)}² + ${n(ay)}²); α = arctg (${n(ay)} / ${n(ax)}) | R = √(${n(ax)}² + ${n(ay)}²); α = arctg (${n(ay)} / ${n(ax)}) |
| Složky se nesčítají přímo – jsou na sebe kolmé. Použij Pythagorovu větu. | Составляющие нельзя просто сложить — они перпендикулярны. Нужна теорема Пифагора. |
| To je úhel od svislice – vyjde, když se dělí \|[[F_x]]\| / \|[[F_y]]\|. Naproti úhlu u vodorovné leží [[F_y]]. | Это угол от вертикали — так выходит, если делить \|[[F_x]]\| / \|[[F_y]]\|. Напротив угла у горизонтали лежит [[F_y]]. |
| Správně: R ${approx(r)} kN, α ≈ ${n(a, 1)}°; síla míří ${dir.cs}. | Верно: R ${approx(r)} кН, α ≈ ${n(a, 1)}°; сила направлена ${dir.ru}. |
| Do arctg dej velikosti bez znamének – hledáme ostrý (kladný) úhel k vodorovné, ne úhel od osy +x. Směr síly je vidět ze znamének složek. | В arctg подставляй величины без знаков — ищем острый (положительный) угол к горизонтали, а не угол от оси +x. Направление силы видно по знакам составляющих. |

### Úloha E20

| čeština | русский |
|---|---|
| Složky ze sklonu | Составляющие по уклону |
| [[F_x]] | [[F_x]] |
| [[F_y]] | [[F_y]] |
| Úhel nepotřebuješ. Sklon $m : $nn je trojúhelník s odvěsnami $m a $nn – dopočítej jeho přeponu a z ní sin α a cos α. | Угол не нужен. Уклон $m : $nn — это треугольник с катетами $m и $nn: найди его гипотенузу, а по ней sin α и cos α. |
| přepona = √($m² + $nn²) = $c; cos α = $nn/$c, sin α = $m/$c → [[F_x]] = ${n(f)} · $nn/$c; [[F_y]] = ${n(f)} · $m/$c | гипотенуза = √($m² + $nn²) = $c; cos α = $nn/$c, sin α = $m/$c → [[F_x]] = ${n(f)} · $nn/$c; [[F_y]] = ${n(f)} · $m/$c |
| Složky jsou prohozené: vodorovná složka patří k vodorovné délce ($nn), svislá k výšce ($m). | Составляющие перепутаны: горизонтальная относится к горизонтальной длине ($nn), вертикальная — к высоте ($m). |
| Správně: [[F_x]] ${approx(fx)} kN, [[F_y]] ${approx(fy)} kN. | Верно: [[F_x]] ${approx(fx)} кН, [[F_y]] ${approx(fy)} кН. |

### Úloha E21

| čeština | русский |
|---|---|
| Rameno a moment | Плечо и момент |
| [[r_⊥]] | [[r_⊥]] |
| M | M |
| [[r_⊥]] = ${n(r)} · sin ${deg(a)} = ${n(arm, 3)} m; M = ${n(f)} · ${n(arm, 3)} | [[r_⊥]] = ${n(r)} · sin ${deg(a)} = ${n(arm, 3)} м; M = ${n(f)} · ${n(arm, 3)} |
| Rameno nemůže být delší než páka – r násob sinem, neděl. | Плечо не может быть длиннее рычага — r умножай на синус, а не дели. |
| Velikost sedí. Kterým směrem síla kolem O otáčí – ↺ (+), nebo ↻ (−)? | Величина верна. В какую сторону сила вращает вокруг O — ↺ (+) или ↻ (−)? |
| Správně: [[r_⊥]] ${approx(arm, 3)} m, M ${approx(m)} kNm. | Верно: [[r_⊥]] ${approx(arm, 3)} м, M ${approx(m)} кН·м. |

### Úloha E22

| čeština | русский |
|---|---|
| Součet složek | Сумма составляющих |
| vodorovně doprava | горизонтально вправо |
| svisle nahoru | вертикально вверх |
| vodorovně doleva | горизонтально влево |
| svisle dolů | вертикально вниз |
| šikmo doprava nahoru | наклонно вправо вверх |
| šikmo doleva nahoru | наклонно влево вверх |
| šikmo doleva dolů | наклонно влево вниз |
| šikmo doprava dolů | наклонно вправо вниз |
|  |  |
|  pod úhlem ${deg(a)} k vodorovné |  под углом ${deg(a)} к горизонтали |
| Na bod působí [[F_1]] = ${n(f1)} kN ${dirText(phi1).cs}${angleText(a1).cs} a [[F_2]] = ${n(f2)} kN ${dirText(phi2).cs}${angleText(a2).cs}. Urči Σ[[F_x]] a Σ[[F_y]] se znaménky. | На точку действуют [[F_1]] = ${n(f1)} кН ${dirText(phi1).ru}${angleText(a1).ru} и [[F_2]] = ${n(f2)} кН ${dirText(phi2).ru}${angleText(a2).ru}. Найди Σ[[F_x]] и Σ[[F_y]] со знаками. |
| Σ[[F_x]] | Σ[[F_x]] |
| Σ[[F_y]] | Σ[[F_y]] |
| Rozlož každou šikmou sílu na složky (u úhlu cos, naproti sin) a sečti zvlášť vodorovné a zvlášť svislé – doprava a nahoru +. | Разложи каждую наклонную силу на составляющие (у угла cos, напротив sin) и сложи отдельно горизонтальные и вертикальные — вправо и вверх +. |
| Σ[[F_x]] = $hx; Σ[[F_y]] = $hy | Σ[[F_x]] = $hx; Σ[[F_y]] = $hy |
| Zkontroluj znaménko vodorovné složky [[F_2]]: doprava +, doleva −. | Проверь знак горизонтальной составляющей [[F_2]]: вправо +, влево −. |
| Zkontroluj znaménko svislé složky [[F_2]]: nahoru +, dolů −. | Проверь знак вертикальной составляющей [[F_2]]: вверх +, вниз −. |
| Zkontroluj znaménko vodorovné složky [[F_1]]: doprava +, doleva −. | Проверь знак горизонтальной составляющей [[F_1]]: вправо +, влево −. |
| Zkontroluj znaménko svislé složky [[F_1]]: nahoru +, dolů −. | Проверь знак вертикальной составляющей [[F_1]]: вверх +, вниз −. |
| Správně: Σ[[F_x]] ${approx(sx)} kN, Σ[[F_y]] ${approx(sy)} kN. | Верно: Σ[[F_x]] ${approx(sx)} кН, Σ[[F_y]] ${approx(sy)} кН. |

### Úloha E23

| čeština | русский |
|---|---|
| Šikmý nosník | Наклонная балка |
| půdorys | проекция |
| výška | высота |
| Krokev je přepona: půdorys leží u úhlu (cos), výška naproti (sin). | Стропило — гипотенуза: проекция лежит у угла (cos), высота напротив (sin). |
| ${n(len)} · cos ${deg(a)}; ${n(len)} · sin ${deg(a)} | ${n(len)} · cos ${deg(a)}; ${n(len)} · sin ${deg(a)} |
| Správně: půdorys ${approx(px)} m, výška ${approx(h)} m. | Верно: проекция ${approx(px)} м, высота ${approx(h)} м. |

