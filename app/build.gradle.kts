import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * `local.properties` holds machine specific values (SDK path, OAuth client id)
 * and is never committed.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.messmate.bd"

    // The current AndroidX libraries (the Compose BOM pulls in lifecycle 2.11,
    // hilt-navigation-compose needs 37) require the app to compile against API 37.
    // `targetSdk` stays at 36, that is what the Play Store asks for today.
    compileSdk = 37
    compileSdkMinor = 0

    defaultConfig {
        applicationId = "com.messmate.bd"
        minSdk = 24
        targetSdk = 36

        // Overridable from CI: `-PversionCode=42 -PversionName=1.2.3`
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // OAuth client id of the Google Cloud project (client_type = 3).
        // Read from local.properties, then from -P / environment variables.
        val serverClientId: String = (
            localProperties.getProperty("MESSMATE_SERVER_CLIENT_ID")
                ?: providers.gradleProperty("MESSMATE_SERVER_CLIENT_ID").orNull
                ?: System.getenv("MESSMATE_SERVER_CLIENT_ID")
                ?: ""
            ).trim()

        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$serverClientId\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        // `java.time` is used for the dates, the cut-off times and the monthly
        // periods, it is desugared for API levels below 26.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    bundle {
        language {
            // The runtime language switch (English / বাংলা) only works when
            // both languages are part of the base APK.
            enableSplit = false
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                // The Google auth libraries ship the same index file twice.
                "/META-INF/INDEX.LIST",
                "/META-INF/*.kotlin_module",
            )
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Room schema export – lets CI diff/verify migrations.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.browser)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- Persistence & background work ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    // --- Dependency injection ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // --- Google sign in ---
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.identity)
    implementation(libs.play.services.auth)

    // --- Google Drive + Sheets REST clients ---
    implementation(libs.google.api.client)
    implementation(libs.google.http.client.gson)
    implementation(libs.google.api.services.sheets)
    implementation(libs.google.api.services.drive)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
}
