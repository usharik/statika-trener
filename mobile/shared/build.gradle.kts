// Společný kód Android (a později iOS) aplikace: model úloh, mechanika, rozbor rovnic,
// stav jednotlivých kroků, texty a celé UI v Compose Multiplatform.
// Platformní služby (reklama, nákupy, analytika, úložiště) jsou tu jen rozhraní – implementuje je :app.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// Verze a commit pro řádek „Verze“ v iOS aplikaci (Android je bere z BuildConfig).
val iosBuildInfo = tasks.register("generateIosBuildInfo") {
    val commit = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim().ifEmpty { "unknown" } }
    val version = providers.fileContents(rootProject.layout.projectDirectory.file("VERSION")).asText.map { it.trim() }
    val output = layout.buildDirectory.dir("generated/iosBuildInfo/kotlin")
    inputs.property("commit", commit)
    inputs.property("version", version)
    outputs.dir(output)
    doLast {
        val file = output.get().file("cz/usharik/statika/platform/IosBuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package cz.usharik.statika.platform\n\n" +
                "internal const val IOS_GIT_HASH = \"${commit.get()}\"\n" +
                "internal const val IOS_VERSION_NAME = \"${version.get()}\"\n",
        )
    }
}

kotlin {
    android {
        namespace = "cz.usharik.statika.shared"
        compileSdk = 37
        minSdk = 24
    }

    // Testy modelu a stavů kroků běží na JVM (rychle, bez emulátoru).
    jvm()

    // iOS: statický framework „Shared“, který vkládá Xcode projekt (iosApp/project.yml).
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        iosMain { kotlin.srcDir(iosBuildInfo) }
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.foundation)
            implementation(libs.cmp.ui)
            implementation(libs.cmp.ui.backhandler)
            implementation(libs.cmp.material3)
            implementation(libs.cmp.material.icons.core)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        // UI testy karet výkladu (Compose Desktop na JVM)
        jvmTest.dependencies {
            @Suppress("DEPRECATION")
            implementation(compose.desktop.currentOs)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            @Suppress("DEPRECATION")
            implementation(compose.uiTest)
        }
    }
}

// KMP modul nemá úlohu `test`; tahle spustí JVM testy i přes `./gradlew test`.
tasks.register("test") { dependsOn("jvmTest") }

// `-PiosSimulator=<udid nebo název>` vybere simulátor pro iosSimulatorArm64Test (názvy se mění s verzí Xcode).
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest>().configureEach {
    providers.gradleProperty("iosSimulator").orNull?.let { device.set(it) }
}
