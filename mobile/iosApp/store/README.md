# App Store – stránka aplikace a TestFlight

Aplikace **Statika: reakce nosníků**, bundle id `cz.usharik.statika`, tým B7PSU5B367, primární jazyk **cs**,
jazyky stránky cs a ru (jazyky rozhraní). iPhone i iPad. Celá infrastruktura je převzatá z Ear4Music2
(`~/dev/Ear4Music2/iosApp/store`, `.github/workflows/{testflight,asc-setup,ios-screenshots}.yml`, `fastlane/`)
a upravená na tuto aplikaci; při změně tam stojí za to porovnat obě.

## Klíče a podpis (Infisical, jako CzechQuiz, MySongbook a PhaserPrint)

Certifikát Apple Distribution týmu a klíč App Store Connect API jsou v Infisicalu (projekt `2e201525-…` CzechQuizu,
env `prod`): `/ios-signing` (`DIST_CERT_ID`, `DIST_P12_BASE64`, `DIST_P12_PASSWORD`) a `/app-store` (`ASC_KEY_ID`,
`ASC_ISSUER_ID`, `ASC_KEY_P8`). Nic z toho není v repozitáři ani na disku; skripty je berou přes `infisical run`.

- `iosApp/upload-testflight.sh <build> [archive|upload|all]` – podepsaný archiv (dočasná klíčenka) a nahrání do
  App Store Connect. Profil „Statika App Store“ stáhne přes API (při prvním běhu ho vytvořil). Verze = `mobile/VERSION`,
  build musí s každým nahráním růst. Hlídá Firebase plist, id AdMob a privacy manifesty jako `testflight.yml`.
- `iosApp/store/asc.sh <příkaz>` – spustí příkaz s klíčem ASC (`ASC_KEY_PATH` = dočasný .p8), např.
  `iosApp/store/asc.sh python3 iosApp/store/tools/asc_setup.py --bundle-id cz.usharik.statika status`
  (Python potřebuje `pyjwt cryptography`) nebo `iosApp/store/asc.sh bundle exec fastlane ios listing`.
- `GoogleService-Info.plist` (Firebase projekt `statika-learn`) je jen lokálně v `iosApp/Statika/` (v .gitignore).

GitHub workflow `testflight.yml`, `asc-setup.yml` a `ios-screenshots.yml` dělají totéž na GitHubu, pokud jsou
v repozitáři tajemství (`./setup-github-secrets.sh`); hlavní cesta je lokální.

## Postup vydání

1. Hotovo: aplikace v App Store Connect (id 6837713584), bundle id `cz.usharik.statika`, profil „Statika App Store“,
   iOS aplikace ve Firebase (`statika-learn`), AdMob aplikace `ca-app-pub-6420074467468909~2665521232` s jednotkami
   banner a interpage (`project.yml`, konfigurace Release).
2. `asc_setup.py` (přes `asc.sh`): `status` → `price-free` → `age-rating` (celý dotazník 2025: reklama ano, jinak nic)
   → `iap --spec iosApp/store/iap/remove_ads_statika.json` a `iap --spec iosApp/store/iap/pdf_solution_statika.json`
   → `review-notes --notes iosApp/store/review_notes.txt`.
3. `iosApp/upload-testflight.sh <build>` → TestFlight; vyzkoušet reklamu, nákupy v sandboxu, obnovení, PDF, souhlas UMP.
4. Snímky: `iosApp/store/capture_screenshots.sh` (simulátory iPhone Pro Max a iPad Pro 13", cs + ru) →
   `screenshots/<locale>/`, `iap/settings-remove-ads.png` a `iap/pdf-solution.png`; znovu `iap` (nahraje snímky ke kontrole).
5. Stránka: `asc.sh /tmp/…/venv/bin/python iosApp/store/tools/asc_listing.py metadata|screenshots|review|availability`,
   kontrola `verify` (Python s `pyjwt`, `cryptography`; fastlane tu není). Dostupnost: všude kromě Číny (ICP, formuláře
   App Compliance pro AI a Security Assessment, AdMob tam nefunguje). Štítek App Privacy ručně podle
   `fastlane/app_privacy_details.json` a **Publish** – uložené, ale nepublikované odpovědi `submit` odmítne.
6. `attach-build --build <číslo>` → `submit --products remove_ads_statika,pdf_solution_statika`
   (první nákupy v aplikaci jdou ke kontrole spolu s verzí). Vydání ručně; po vydání AdMob → „Add app store listing“.

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
úloha (Gerberův nosník), rovnice z palety, výsledek, výklad, lekce o momentu, kurz trigonometrie a série procvičování; navíc okno PDF a nastavení s nákupem pro kontrolu nákupů.

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
