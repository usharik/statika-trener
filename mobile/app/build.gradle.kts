import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Firebase se zapne, jen když je k dispozici google-services.json (není v gitu): app/ pro všechny buildy,
// nebo app/src/release/ jen pro release (projekt Firebase pak nepotřebuje aplikaci cz.usharik.statika.debug).
// Bez něj se aplikace sestaví také – analytika a hlášení pádů pak nic nedělají.
val hasFirebaseConfig = file("google-services.json").exists() || file("src/release/google-services.json").exists()
if (hasFirebaseConfig) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.plugin.get().pluginId)
    // debug bez vlastní konfigurace jen varuje (aplikace pak Firebase nespustí – viz AndroidServices)
    extensions.configure<com.google.gms.googleservices.GoogleServicesPlugin.GoogleServicesPluginConfig>("googleServices") {
        missingGoogleServicesStrategy = com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy.WARN
    }
}

// Verze z kořenového souboru VERSION ve tvaru MAJOR.MINOR → versionCode = MAJOR * 100 + MINOR.
val versionStr = rootProject.file("VERSION").readText().trim()
val (vMajor, vMinor) = versionStr.split(".").map { it.toInt() }

// Skutečná ID AdMob a podpisový klíč se čtou z local.properties nebo ~/.gradle/gradle.properties
// (klíče statika.*). Bez nich se použijí testovací ID Googlu.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(name: String): String? = localProps.getProperty(name) ?: providers.gradleProperty(name).orNull

val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testBanner = "ca-app-pub-3940256099942544/6300978111"
val testInterstitial = "ca-app-pub-3940256099942544/1033173712"

val gitHash: String = runCatching {
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrDefault("").ifEmpty { "unknown" }

// Release pro Google Play nesmí tiše vzniknout s testovacími reklamami nebo bez podpisu.
// Zkušební release (CI, emulátor): -Pstatika.allowTestAds=true
gradle.taskGraph.whenReady {
    val release = allTasks.any { it.project == project && (it.name == "bundleRelease" || it.name == "assembleRelease") }
    val allowed = providers.gradleProperty("statika.allowTestAds").orNull == "true"
    if (release && !allowed) {
        val missing = listOf("statika.admob.appId", "statika.admob.bannerId", "statika.admob.interstitialId", "statika.signing.storeFile")
            .filter { secret(it) == null }
        if (missing.isNotEmpty()) throw GradleException("Release bez $missing – doplň je do local.properties, nebo zkušebně -Pstatika.allowTestAds=true")
    }
}

android {
    namespace = "cz.usharik.statika"
    compileSdk = 37

    defaultConfig {
        applicationId = "cz.usharik.statika"
        minSdk = 24
        targetSdk = 36
        versionCode = vMajor * 100 + vMinor
        versionName = versionStr
        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
        buildConfigField("boolean", "HAS_FIREBASE", hasFirebaseConfig.toString())
        // ID testovacích zařízení AdMob (z logcatu „setTestDeviceIds…“), oddělená čárkou – na nich i release
        // s pravými ID dostává jen testovací reklamy (vlastní telefon, emulátor pro snímky obrazovky)
        buildConfigField("String", "ADMOB_TEST_DEVICES", "\"${secret("statika.admob.testDevices").orEmpty()}\"")
    }

    signingConfigs {
        val storeFile = secret("statika.signing.storeFile")
        if (storeFile != null) {
            create("release") {
                this.storeFile = file(storeFile)
                storePassword = secret("statika.signing.storePassword")
                keyAlias = secret("statika.signing.keyAlias")
                keyPassword = secret("statika.signing.keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["ADMOB_APP_ID"] = testAppId
            buildConfigField("String", "BANNER_ID", "\"$testBanner\"")
            buildConfigField("String", "INTERSTITIAL_ID", "\"$testInterstitial\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
            manifestPlaceholders["ADMOB_APP_ID"] = secret("statika.admob.appId") ?: testAppId
            buildConfigField("String", "BANNER_ID", "\"${secret("statika.admob.bannerId") ?: testBanner}\"")
            buildConfigField("String", "INTERSTITIAL_ID", "\"${secret("statika.admob.interstitialId") ?: testInterstitial}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    bundle {
        // jazyk se přepíná v aplikaci, nezávisle na systému
        language { enableSplit = false }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.cmp.runtime)
    implementation(libs.cmp.ui)
    implementation(libs.cmp.foundation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.play.services.ads)
    implementation(libs.work.runtime)
    implementation(libs.ump)
    implementation(libs.play.billing.ktx)

    testImplementation(libs.junit)
}
