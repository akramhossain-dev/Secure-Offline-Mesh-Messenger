// ─────────────────────────────────────────────────────────────────────────────
// App Module Build Configuration
// Offline Emergency Mesh Communication System
// ─────────────────────────────────────────────────────────────────────────────

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.mesh.emergency"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mesh.emergency"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── BuildConfig fields ────────────────────────────────────────────
        buildConfigField("String",  "APP_NAME",       "\"OfflineEmergencyMesh\"")
        buildConfigField("String",  "ENVIRONMENT",    "\"debug\"")
        buildConfigField("Long",    "BLE_TIMEOUT_MS", "30_000L")
        buildConfigField("Long",    "LORA_TIMEOUT_MS","60_000L")
        buildConfigField("Int",     "MAX_HOPS",       "10")

        // ── Resource configurations ───────────────────────────────────────
        resourceConfigurations += listOf("en", "bn")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = project.findProperty("RELEASE_KEYSTORE_FILE") as? String
            val keystorePassword = project.findProperty("RELEASE_KEYSTORE_PASSWORD") as? String
            val keyAliasStr = project.findProperty("RELEASE_KEY_ALIAS") as? String
            val keyPasswordStr = project.findProperty("RELEASE_KEY_PASSWORD") as? String

            if (!keystorePath.isNullOrEmpty()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasStr
                keyPassword = keyPasswordStr
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false

            buildConfigField("Boolean", "ENABLE_LOGGING",       "true")
            buildConfigField("Boolean", "FEATURE_BLE",          "false")
            buildConfigField("Boolean", "FEATURE_LORA",         "false")
            buildConfigField("Boolean", "FEATURE_ENCRYPTION",   "false")
            buildConfigField("Boolean", "FEATURE_MAPS",         "false")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules-debug.pro"
            )
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true

            buildConfigField("Boolean", "ENABLE_LOGGING",       "false")
            buildConfigField("Boolean", "FEATURE_BLE",          "true")
            buildConfigField("Boolean", "FEATURE_LORA",         "true")
            buildConfigField("Boolean", "FEATURE_ENCRYPTION",   "true")
            buildConfigField("Boolean", "FEATURE_MAPS",         "true")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val keystorePath = project.findProperty("RELEASE_KEYSTORE_FILE") as? String
            if (!keystorePath.isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    // ── Kotlin / Java Compatibility ───────────────────────────────────────────
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    // ── Compose ───────────────────────────────────────────────────────────────
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // ── KSP — Room Schema Export ─────────────────────────────────────────────
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }

    // ── Packaging ─────────────────────────────────────────────────────────────
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }

    // ── Test Options ──────────────────────────────────────────────────────────
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dependencies
// ─────────────────────────────────────────────────────────────────────────────
dependencies {

    // ── Android Core ──────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.splashscreen)

    // ── Jetpack Compose BOM ───────────────────────────────────────────────────
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.bundles.compose)
    implementation(libs.compose.material3.window)
    implementation(libs.compose.runtime)
    debugImplementation(libs.compose.ui.tooling)

    // ── Lifecycle / ViewModel ─────────────────────────────────────────────────
    implementation(libs.bundles.lifecycle)

    // ── Navigation ────────────────────────────────────────────────────────────
    implementation(libs.navigation.compose)

    // ── Hilt — Dependency Injection ───────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation(libs.bundles.coroutines)

    // ── DataStore ─────────────────────────────────────────────────────────────
    implementation(libs.datastore.preferences)

    // ── WorkManager ───────────────────────────────────────────────────────────
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // ── Room (Database) ───────────────────────────────────────────────────────
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // ── Logging ───────────────────────────────────────────────────────────────
    implementation(libs.timber)

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation(libs.bundles.testing.unit)
    androidTestImplementation(libs.bundles.testing.android)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
