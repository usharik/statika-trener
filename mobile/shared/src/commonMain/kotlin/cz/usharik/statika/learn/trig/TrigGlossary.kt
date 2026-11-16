package cz.usharik.statika.learn.trig

import cz.usharik.statika.learn.L

/** Slovníček kurzu trigonometrie (pojem → vysvětlení), zobrazí se jako druhý oddíl slovníčku. */
val TRIG_GLOSSARY: List<Pair<L, L>> = listOf(
    L("Pravoúhlý trojúhelník", "Прямоугольный треугольник (pravoúhlý trojúhelník)") to L("Trojúhelník s jedním pravým úhlem (90°).", "Треугольник с одним прямым углом (90°)."),
    L("Přepona", "Гипотенуза (přepona)") to L("Nejdelší strana, leží naproti pravému úhlu.", "Самая длинная сторона, лежит напротив прямого угла."),
    L("Odvěsna přilehlá", "Прилежащий катет (přilehlá odvěsna)") to L("Odvěsna, která vychází z vrcholu daného úhlu.", "Катет, выходящий из вершины данного угла."),
    L("Odvěsna protilehlá", "Противолежащий катет (protilehlá odvěsna)") to L("Odvěsna naproti danému úhlu – nedotýká se ho.", "Катет напротив данного угла — он его не касается."),
    L("Podobné trojúhelníky", "Подобные треугольники (podobné)") to L("Mají stejné úhly; strany jsou ve stejném poměru.", "У них одинаковые углы; стороны находятся в одном и том же отношении."),
    L("Goniometrické funkce", "Тригонометрические функции (goniometrické funkce)") to L("sin, cos, tg – podíly stran pravoúhlého trojúhelníku, závisí jen na úhlu.", "sin, cos, tg — отношения сторон прямоугольного треугольника, зависят только от угла."),
    L("Sinus (sin α)", "Синус (sin α)") to L("Protilehlá odvěsna / přepona.", "Противолежащий катет / гипотенуза."),
    L("Kosinus (cos α)", "Косинус (cos α)") to L("Přilehlá odvěsna / přepona.", "Прилежащий катет / гипотенуза."),
    L("Tangens (tg α, na kalkulačce tan)", "Тангенс (tg α, на калькуляторе tan)") to L("Protilehlá / přilehlá odvěsna; také sin α / cos α. Udává sklon.", "Противолежащий / прилежащий катет; также sin α / cos α. Задаёт уклон."),
    L("Stupeň (°)", "Градус (°)") to L("Jednotka úhlu: pravý úhel = 90°, plný úhel (celá otáčka) = 360°.", "Единица угла: прямой угол = 90°, полный оборот = 360°."),
    L("Radián", "Радиан") to L("Jednotka obloukové míry: úhel jako délka oblouku na kružnici s poloměrem 1. 180° = π rad ≈ 3,14 rad.", "Единица радианной меры: угол как длина дуги на окружности радиуса 1. 180° = π рад ≈ 3,14 рад."),
    L("DEG / RAD / GRAD", "DEG / RAD / GRAD") to L("Režimy kalkulačky: stupně, radiány, grady. Pro stupně musí být DEG: sin 30 = 0,5.", "Режимы калькулятора: градусы, радианы, грады. Для градусов нужен DEG: sin 30 = 0,5."),
    L("arcsin, arccos, arctg (sin⁻¹, cos⁻¹, tan⁻¹)", "arcsin, arccos, arctg (sin⁻¹, cos⁻¹, tan⁻¹)") to L("Inverzní (obrácené) funkce: z podílu stran vrátí úhel. sin⁻¹ neznamená 1/sin.", "Обратные функции (inverzní): по отношению сторон возвращают угол. sin⁻¹ не означает 1/sin."),
    L("Pythagorova věta", "Теорема Пифагора") to L("a² + b² = c², kde c je přepona.", "a² + b² = c², где c — гипотенуза."),
    L("Odmocnina (√)", "Квадратный корень (odmocnina, √)") to L("Číslo, které samo se sebou vynásobené dá dané číslo: √25 = 5, √2 ≈ 1,41.", "Число, которое при умножении само на себя даёт данное: √25 = 5, √2 ≈ 1,41."),
    L("Ostrý úhel", "Острый угол (ostrý úhel)") to L("Úhel menší než 90°.", "Угол меньше 90°."),
    L("Doplňkový úhel", "Дополнительный угол (doplňkový)") to L("Druhý ostrý úhel, 90° − α. Platí sin α = cos (90° − α).", "Второй острый угол, 90° − α. Верно sin α = cos (90° − α)."),
    L("Jednotková kružnice", "Единичная окружность (jednotková kružnice)") to L("Kružnice s poloměrem 1; bod na ní má souřadnice (cos φ, sin φ).", "Окружность радиуса 1; точка на ней имеет координаты (cos φ, sin φ)."),
    L("Kvadrant", "Квадрант, четверть (kvadrant)") to L("Jedna ze čtyř částí roviny mezi osami; určuje znaménka sin a cos.", "Одна из четырёх частей плоскости между осями; определяет знаки sin и cos."),
    L("Sklon", "Уклон (sklon)") to L("Výška / vodorovná délka = tg α. Zadává se poměrem výška : délka (1 : 2, 3 : 4) nebo v % (tg α · 100).", "Высота / горизонтальная длина = tg α. Задаётся отношением высота : длина (1 : 2, 3 : 4) или в % (tg α · 100)."),
    L("Půdorysná délka (vodorovný průmět)", "Горизонтальная проекция (půdorysná délka)") to L("Vodorovná délka šikmého nosníku: L · cos α. Převýšení je L · sin α.", "Горизонтальная длина наклонной балки: L · cos α. Высота подъёма — L · sin α."),
)
