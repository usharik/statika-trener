# Statika · nosníky — mobilní aplikace (Android a iOS)

Kotlin Multiplatform + Compose Multiplatform. Stejná architektura jako Ear4Music / CzechDeclensionQuiz:

| Modul | Obsah |
|---|---|
| `:shared` (KMP: android, iosArm64 + iosSimulatorArm64, jvm pro testy) | model úloh (`model/` – generátor, mechanika, rozbor rovnic; 1:1 port z `src/model`), stav kroků (`trainer/`), texty cs/ru (`i18n/`), celé UI v Compose (`ui/` – výkres v Canvasu, kroky, paleta) a rozhraní platformních služeb (`platform/`) |
| `:app` (Android) | `MainActivity`, SharedPreferences, Firebase Analytics + Crashlytics, AdMob + UMP (souhlas GDPR), Google Play Billing („remove_ads“) |
| `iosApp/` (iOS, iPhone + iPad) | XcodeGen projekt (`project.yml`), SwiftUI obal Compose UI, Swift mosty: Firebase Analytics + Crashlytics (`FirebaseTelemetry.swift`), AdMob + UMP (`AdMobBridge.swift`), StoreKit 2 (`StoreKitBridge.swift`); Kotlin strana v `shared/src/iosMain` (`IosAppGraph`, `MainViewController`) |

**Co je společné a co platformní.** Pravidla služeb jsou v `shared/src/commonMain/.../platform/` a platí na obou
platformách stejně; platformy dodávají jen tenké adaptéry s voláním SDK:

| Společné (commonMain) | Android (`:app`) | iOS (`iosMain` + Swift) |
|---|---|---|
| `SharedAds` – souhlas UMP → reklama i analytika, PG, „bez reklamy“, mezistránková reklama nejvýš jednou, šířka banneru | `AdMobSdk` | `IosAdsSdk` → `AdMobBridge.swift` |
| `SharedBilling` – uložený stav „bez reklamy“ (start offline bez bliknutí), obchod jako zdroj pravdy, analytika nákupu | `PlayBillingSdk` | `StoreKitBridge.swift` (přímo `BillingSdk`) |
| `NormalizedAnalytics` / `AnalyticsParams` – názvy a parametry podle pravidel Firebase | `firebaseAnalytics` | `FirebaseTelemetry.swift` |
| `seedFromLink` (statika://N, …#N, ?seed=N), `systemLanguageCode`, `versionLabel`, `Products` | `MainActivity`, manifest | `MainViewController`, `OpenLinks` |

## Sestavení a testy

```bash
cd mobile
./gradlew :shared:jvmTest          # generátor (18 000 úloh), rozbor rovnic, kroky, shoda s webem
./gradlew :app:assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:bundleRelease       # AAB pro Google Play
```

Úlohu lze otevřít zvenku: `adb shell am start -n cz.usharik.statika.debug/cz.usharik.statika.MainActivity --ei seed 12345`.

## Shoda s webem

Generátor je přenesený bit po bitu (mulberry32 včetně `Math.imul`, `Math.round` …), takže úloha č. 12345 je v aplikaci
i na webu stejná. Hlídá to `GoldenTest` proti `shared/src/jvmTest/resources/golden.txt` (7 200 úloh vč. reakcí).
Po změně webového generátoru soubor obnovíš z kořene repozitáře:

```bash
GOLDEN=1 npx vitest run src/model/golden.test.ts
```

## Konfigurace pro vydání (není v gitu)

- `app/google-services.json` – Firebase projekt s aplikací `cz.usharik.statika` (bez něj se Firebase nezapne, sestavení projde).
- `local.properties` nebo `~/.gradle/gradle.properties`:
  ```properties
  statika.admob.appId=ca-app-pub-…~…
  statika.admob.bannerId=ca-app-pub-…/…
  statika.admob.interstitialId=ca-app-pub-…/…
  statika.signing.storeFile=/cesta/k/upload.jks
  statika.signing.storePassword=…
  statika.signing.keyAlias=…
  statika.signing.keyPassword=…
  ```
  Bez nich release sestavení skončí chybou; zkušební release (CI, emulátor) s testovacími reklamami:
  `./gradlew :app:assembleRelease -Pstatika.allowTestAds=true`.
- Play Console: jednorázový produkt `remove_ads`; zásady ochrany údajů: `https://usharik.github.io/statika-trener/privacy.html`.
- Verze: soubor `VERSION` (`MAJOR.MINOR` → versionCode = MAJOR·100 + MINOR).

## iOS

Potřeba macOS s Xcode (16+), XcodeGen (`brew install xcodegen`) a JDK 21. Uspořádání je stejné jako v Ear4Music2.

```bash
cd mobile/iosApp && xcodegen generate && open Statika.xcodeproj   # spustit schéma Statika na simulátoru
./iosApp/ci.sh        # (z mobile/) sdílené testy na iOS simulátoru + build + UI testy na iPhonu a iPadu
./gradlew :shared:iosSimulatorArm64Test                              # jen sdílené testy na simulátoru
```

Pokud je vybrané jen Command Line Tools: `export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`.
Krok „Build Kotlin framework“ v Xcode volá `./gradlew :shared:embedAndSignAppleFrameworkForXcode`.

- **Univerzální aplikace**: iPhone na výšku i naležato, iPad ve všech orientacích a ve Split View; na širokém
  displeji se UI samo přepne na dva sloupce (výkres vlevo, kroky vpravo).
- **UI testy** (`StatikaUITests`, spustí `-uiTesting`: čistý stav, čeština, úloha č. 1): celá úloha od uvolnění po
  výsledek včetně palety a systémové klávesnice, výklad, oblíbené, nastavení a sdílení (na iPadu popover);
  `AdsTests` – formulář souhlasu a testovací banner (potřebuje síť, v CI se přeskakuje). Snímky z testů:
  `build/ios/<iphone|ipad>/screenshots/`.
- **Odkazy**: `statika://12345` otevře úlohu podle čísla.
- **Ikona**: `iosApp/tools/render_icon.swift` ji kreslí ze stejné předlohy jako ikonu Androidu.

Co chybí pro App Store (nic z toho není v gitu):

1. **App Store Connect**: aplikace s bundle id `cz.usharik.statika`, nákup „bez reklamy“ jako Non-Consumable
   s id **`remove_ads_statika`** (id v App Store musí být jedinečná v rámci účtu; v Google Play je `remove_ads`),
   App Privacy podle `Statika/PrivacyInfo.xcprivacy`, věková kategorie 4+ (reklama omezená na PG).
2. **AdMob**: iOS aplikace (Apps → Add app → iOS) a dvě jednotky (banner, interstitial) → doplnit do
   `project.yml` (konfigurace Release: `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`). Dokud tam je
   `REPLACE`, Release build běží bez reklamy (nespadne). Debug používá testovací jednotky Googlu.
3. **Firebase**: iOS aplikace ve stejném projektu Firebase → `GoogleService-Info.plist` do `iosApp/Statika/`
   (je v `.gitignore`); bez něj je Firebase vypnutý.
4. **Podpis**: certifikát Apple Distribution (tým `B7PSU5B367`) a provisioning profil **„Statika App Store“**
   (Release je nastavený na ruční podpis s tímto jménem).
5. **Snímky pro obchod**: iPhone 6,9" a iPad 13" (UI testy je dělají na simulátorech těchto velikostí).

## Vydání do obchodů

Postup prvního vydání (App Store i Google Play), workflow TestFlight / App Store Connect / Google Play,
texty stránek a snímky: [iosApp/store/README.md](iosApp/store/README.md) a [docs/google-play/README.md](docs/google-play/README.md).
Tajné klíče do GitHubu nastaví `./setup-github-secrets.sh` (bez `--apply` jen vypíše, co chybí).
Kontrola textů a konfigurace: `python3 scripts/check_store.py`.

## Reklama

Banner jen na obrazovce „Hotovo“, mezistránková reklama nejvýš po každé 3. vyřešené úloze (při „Další úloha“),
obsah reklam omezený na PG. Po nákupu „bez reklamy“ se nic nenačítá. Na iOS stejně (UMP souhlas, PG), bez
App Tracking Transparency – reklama se nepersonalizuje podle IDFA (aplikace je pro studenty).
