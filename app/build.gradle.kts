plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

/**
 * Release signing values are never stored in the repository.
 *
 * They are read from environment variables first (this is what CI uses) and fall back to
 * Gradle properties so a developer can sign locally from `~/.gradle/gradle.properties`:
 *
 *     MESS_UPLOAD_STORE_FILE=/abs/path/release.keystore
 *     MESS_UPLOAD_STORE_PASSWORD=...
 *     MESS_UPLOAD_KEY_ALIAS=...
 *     MESS_UPLOAD_KEY_PASSWORD=...
 */
fun signingValue(envName: String, propertyName: String): String? =
    providers.environmentVariable(envName).orNull
        ?: providers.gradleProperty(propertyName).orNull

val keystorePath = signingValue("ANDROID_KEYSTORE_PATH", "MESS_UPLOAD_STORE_FILE")
val keystorePassword = signingValue("ANDROID_KEYSTORE_PASSWORD", "MESS_UPLOAD_STORE_PASSWORD")
val keyAlias = signingValue("ANDROID_KEY_ALIAS", "MESS_UPLOAD_KEY_ALIAS")
val keyPassword = signingValue("ANDROID_KEY_PASSWORD", "MESS_UPLOAD_KEY_PASSWORD")

val hasReleaseSigning =
    listOf(keystorePath, keystorePassword, keyAlias, keyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "com.hussain.messmobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hussain.messmobile"
        minSdk = 24
        targetSdk = 36

        // Overridable from CI: `-PversionCode=42 -PversionName=1.2.3`
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                keyAlias = keyAlias
                keyPassword = keyPassword
            }
        }
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
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        // `java.time` is used for the dates and the monthly periods, it is desugared for
        // API levels below 26.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/DEPENDENCIES")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.browser)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.serialization.json)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
