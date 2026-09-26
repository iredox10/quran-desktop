plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Pure-JVM shared logic (Tajweed, Mushaf, verse-text helpers).
// Sources live under src/commonMain/kotlin (kept so the module can move
// back to kotlin-multiplatform later without file moves).
kotlin {
    jvmToolchain(17)
}

sourceSets {
    main {
        kotlin.srcDir("src/commonMain/kotlin")
        dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
