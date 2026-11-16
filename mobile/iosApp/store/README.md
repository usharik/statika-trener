# App Store – stránka aplikace a TestFlight

Aplikace **Statika: reakce nosníků**, bundle id `cz.usharik.statika`, tým B7PSU5B367, primární jazyk **cs**,
jazyky stránky cs a ru (jazyky rozhraní). iPhone i iPad. Celá infrastruktura je převzatá z Ear4Music2
(`~/dev/Ear4Music2/iosApp/store`, `.github/workflows/{testflight,asc-setup,ios-screenshots}.yml`, `fastlane/`)
a upravená na tuto aplikaci; při změně tam stojí za to porovnat obě.

## Postup prvního vydání (vlastník účtu)

1. **App Store Connect → Aplikace → +**: nová aplikace iOS, název „Statika: reakce nosníků“, primární jazyk čeština,
   bundle id `cz.usharik.statika` (nejdřív ho zaregistrovat v Certificates, Identifiers & Profiles → Identifiers),
   SKU např. `statika`. Aplikace se přes API vytvořit nedá.
2. **Profil** „Statika App Store“ (Profiles → + → App Store Connect, bundle id `cz.usharik.statika`, certifikát
   Apple Distribution týmu – stejný jako u Ear4Music / CzechDeclensionQuiz). Uložit do `iosApp/signing/`
   jako `Statika_App_Store.mobileprovision`, vedle `dist.p12` a `AuthKey_<KEYID>.p8` (adresář je v .gitignore).
3. **Firebase**: v projektu Statika (stejném jako Android) přidat iOS aplikaci `cz.usharik.statika`, stáhnout
   `GoogleService-Info.plist` do `iosApp/Statika/` (v .gitignore).
4. **AdMob**: Apps → Add app → iOS (bez odkazu na obchod), dvě jednotky: banner („výsledek“) a interstitial.
   ID vepsat do `iosApp/project.yml` (konfigurace Release: `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`).
   Dokud je tam `REPLACE`, testflight.yml build odmítne. Po vydání: AdMob → aplikace → „Add app store listing“.
5. **Tajemství GitHubu**: `./setup-github-secrets.sh` (z `mobile/`, nejdřív bez parametrů – vypíše, co chybí),
   pak `./setup-github-secrets.sh --apply ios`.
6. **App Store Connect setup** (Actions → *App Store Connect setup*), postupně příkazy:
   `status` → `price-free` → `age-rating` → `iap` (nákup `remove_ads_statika` z `iap/remove_ads_statika.json`
   s lokalizacemi, cenou, dostupností a snímkem `iap/settings-remove-ads.png`) → `review-notes`.
7. **TestFlight** (Actions → *TestFlight*, nebo tag `ios-v<cokoli>`): build číslo = číslo běhu, verze = `mobile/VERSION`.
8. **Snímky** (Actions → *iOS store screenshots*, nebo tag `ios-screenshots<cokoli>`): artefakt rozbalit do
   `screenshots/<locale>/` (`iphone-*.png` 1320×2868, `ipad-*.png` 2064×2752) a `iap/settings-remove-ads.png`.
9. **Stránka**: `fastlane ios listing` (texty + snímky), štítek **App Privacy**: `fastlane ios privacy`
   (Apple ID správce, 2FA) nebo ručně podle `fastlane/app_privacy_details.json`.
10. **Odeslat ke kontrole**: `attach-build` (číslo buildu) → `submit` s products `remove_ads_statika`
    (první nákup v aplikaci musí jít ke kontrole spolu s verzí).

## Stránka

| Soubor | Pole | Limit |
|---|---|---|
| `metadata/<locale>/name.txt`, `subtitle.txt` | Název, podtitul | 30 |
| `metadata/<locale>/promotional_text.txt` | Propagační text (mění se bez kontroly) | 170 |
| `metadata/<locale>/description.txt` | Popis | 4000 |
| `metadata/<locale>/keywords.txt` | Klíčová slova oddělená čárkou, bez slov z názvu a podtitulu | 100 |
| `metadata/<locale>/{support,marketing,privacy}_url.txt` | https://usharik.github.io/statika-trener/ (+ `privacy.html`) | |
| `metadata/{copyright,primary_category}.txt` | Copyright, kategorie Education | |
| `screenshots/<locale>/{iphone,ipad}-NN-*.png` | Snímky iPhone 6,9" a iPad 13" | 10 na zařízení |

Limity, URL, nákup a soulad štítku App Privacy s `PrivacyInfo.xcprivacy` hlídá `python3 scripts/check_store.py`
(z `mobile/`).

Snímky jsou skutečné snímky ze simulátoru: `capture_screenshots.sh` spustí `StatikaUITests/StoreScreenshotTests` na
nejnovějším iPhonu Pro Max a iPadu Pro 13" v obou jazycích (argument `-statika.lang`, stavový řádek 9:41):
úloha (Gerberův nosník), rovnice z palety, výsledek, výklad a lekce o momentu.

## Reklama, nákup a soukromí

Zdarma s reklamou jako na Androidu: banner AdMob na obrazovce „Hotovo“ a občas interstitial mezi úlohami
(`AdMobBridge.swift`, `IosAds` v iosMain), obsah reklam max. PG. Jednorázový nákup **remove_ads_statika**
(„Bez reklamy“, non-consumable, cena 1.99 USD a ekvivalenty) přes StoreKit 2 (`StoreKitBridge.swift`); v Nastavení
je vedle něj „Obnovit nákupy“ (pravidlo 3.1.1).

**Bez App Tracking Transparency**: aplikace o sledování nežádá a IDFA nečte, proto štítek App Privacy
i `PrivacyInfo.xcprivacy` uvádějí u všech typů údajů „nepoužívá se ke sledování“ a Info.plist nemá
`NSUserTrackingUsageDescription` (testflight.yml to kontroluje). Ear4Music odmítla App Review (5.1.2(i)) právě
za rozpor „Device ID used to track“ bez výzvy ATT – tenhle rozpor tu být nesmí. Kdyby aplikace někdy ATT přidala,
změnit současně štítek, manifest, zásady soukromí (`public/privacy.html`) i review_notes.txt.

Typy údajů štítku (AdMob vždy, Firebase Analytics + Crashlytics až po souhlasu UMP): Identifiers → Device ID
(linked), Location → Coarse Location (linked), Usage Data → Advertising Data a Product Interaction (linked), Purchases → Purchase History (linked, Analytics –
Firebase zaznamenává nákup „bez reklamy“),
Diagnostics → Crash Data, Performance Data, Other Diagnostic Data (not linked). Nic se nepoužívá ke sledování.

Věkové hodnocení: obsah 4+, **reklama: ano** (`age-rating`), nákupy: ano.
