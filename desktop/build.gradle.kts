plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            // Project uses src/main/{kotlin,resources} layout (instead of
            // the multiplatform default src/jvmMain/...) for the desktop module.
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")

            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.gson)
                // WebView-based tajweed renderer; uncomment when needed:
                // implementation(libs.webview.multiplatform)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                implementation(libs.junit4)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.nur.quran.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.AppImage)
            packageName = "quran-nur"
            packageVersion = "1.0.0"
            vendor = "Quran Nur"
            description = "Quran Nur — offline-first Quran reader, memorization and reading planner for desktop."
            copyright = "Copyright © 2026 Quran Nur"
            linux {
                iconFile.set(project.file("src/main/resources/drawable/ic_logo.png"))
                menuGroup = "Education"
            }
        }
    }
}
