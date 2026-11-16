# Google Play – stránka aplikace a vydání

Aplikace **Statika: reakce nosníků**, balíček `cz.usharik.statika`, jazyky stránky cs-CZ (výchozí) a ru-RU.
Struktura `metadata/android/<locale>/` je formát fastlane supply (stejně jako v Ear4Music2).

| Soubor | Pole | Limit |
|---|---|---|
| `metadata/android/<locale>/title.txt` | Název | 30 |
| `metadata/android/<locale>/short_description.txt` | Krátký popis | 80 |
| `metadata/android/<locale>/full_description.txt` | Úplný popis | 4000 |
| `../../distribution/whatsnew/whatsnew-<locale>` | Co je nového (deploy.yml je přepíše z formuláře) | 500 |

Build do interního testování nahraje `bundle exec fastlane android internal`, texty a grafiku `bundle exec fastlane android metadata` (z `mobile/`, klíč servisního účtu `pc-api-key.json` jako u ostatních aplikací – viz `fastlane/Appfile`).
Limity hlídá `python3 scripts/check_store.py`.

## Vydání

`.github/workflows/deploy.yml` (Actions → *Deploy to Google Play*, nebo tag `android-v<cokoli>` → stopa internal):
testy, podepsaný AAB, nahrání do zvolené stopy s „Co je nového“ v obou jazycích, mapping R8 pro čitelné pády.
Verze je `mobile/VERSION` (versionCode = MAJOR·100 + MINOR) – před každým vydáním ji zvýšit.

**První AAB nahrát ručně** v Play Console (Testování → Interní testování → Vytvořit verzi): Google teprve tím
přiřadí balíček k aplikaci a zapne podepisování aplikací Google Play; teprve pak smí nahrávat servisní účet.

## Data safety (Zabezpečení údajů) – odpovědi

Odpovídají tomu, co aplikace opravdu dělá (AndroidAds.kt, AndroidServices.kt, PlayBilling.kt) a zásadám ochrany
soukromí (`public/privacy.html`):

- **Shromažďuje nebo sdílí aplikace údaje?** Ano.
- **Šifrování při přenosu:** Ano (Firebase i AdMob používají HTTPS).
- **Možnost požádat o smazání:** Aplikace nemá účty; údaje v zařízení smaže odinstalace. Pseudonymní data Firebase
  se po 14 měsících mažou (nastavit uchovávání v Google Analytics na 14 měsíců).
- **Typy údajů:**
  - *Identifikátory zařízení nebo jiné ID* – shromažďováno a sdíleno (reklamní ID pro AdMob, ID instalace Firebase);
    účel: reklama nebo marketing, analytika, funkčnost aplikace; sběr je **povinný** (reklama běží vždy, dokud si
    uživatel nekoupí „Bez reklamy“; personalizace reklamy závisí na souhlasu UMP).
  - *Přibližná poloha* – AdMob ji odvozuje z IP adresy; účel: reklama, analytika; sdíleno s Googlem.
  - *Aktivita v aplikaci → interakce s aplikací* – události Firebase (vyřešená úloha, nápověda, výklad) a interakce
    s reklamou; účel: analytika, reklama; jen po souhlasu (analytika).
  - *Informace o aplikaci a výkonu → protokoly selhání, diagnostika* – Crashlytics, výkon reklamního SDK; účel:
    analytika, funkčnost aplikace.
  - *Finanční údaje → Historie nákupů* – Firebase Analytics automaticky zaznamenává nákup v aplikaci
    (in_app_purchase, „Bez reklamy“); účel: analytika. Platební údaje zpracovává Google Play, aplikace je nevidí.
- **Reklamní ID:** Ano, aplikace používá reklamní ID (oprávnění `AD_ID` v manifestu) – v Play Console → Obsah aplikace
  → Reklamní ID zaškrtnout „Reklama nebo marketing“ a „Analytika“.

## Hodnocení obsahu (IARC) a cílové publikum

- Kategorie: **Vzdělávání** (Education). Dotazník IARC: žádné násilí, strach, sex, hazard ani vulgarity; žádná
  komunikace mezi uživateli, žádné sdílení polohy; **digitální nákupy: ano** (jednorázové „Bez reklamy“).
- **Reklamy: ano** (Obsah aplikace → Reklamy). Obsah reklam je v AdMob omezen na **PG** (`MAX_AD_CONTENT_RATING_PG`).
- **Cílové publikum: 13–15, 16–17 a 18+** – studenti průmyslovek a vysokých škol. Nevybírat věk pod 13 let:
  aplikace pak nespadá do programu Families (ten by vyžadoval certifikované reklamní SDK a jiná pravidla reklamy).
  Na otázku „Může aplikace neúmyslně oslovit děti?“ odpovědět ne – obsah je středoškolská technická mechanika.
- Správa aplikace: aplikace není zpravodajská, nemá účty (odpověď „přihlášení není potřeba“), nesbírá zdravotní ani
  finanční údaje; zásady ochrany soukromí: https://usharik.github.io/statika-trener/privacy.html
