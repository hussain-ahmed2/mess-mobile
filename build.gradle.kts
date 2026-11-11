// Top-level build file. Plugins are declared here (but not applied) so that the
// versions are resolved once and shared by every module.
buildscript {
    dependencies {
        // AGP 9 ships with built-in Kotlin support and pulls in Kotlin 2.2.10. Pinning the
        // Kotlin Gradle plugin here upgrades it and keeps it in sync with the Compose compiler
        // plugin below. Keep the version in sync with `gradle/libs.versions.toml`.
        // See https://developer.android.com/build/releases/agp-9-0-0-release-notes
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
