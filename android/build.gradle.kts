// ─────────────────────────────────────────────────────────────────────────────
// Root Build Configuration
// Offline Emergency Mesh Communication System
// ─────────────────────────────────────────────────────────────────────────────

plugins {
    alias(libs.plugins.android.application)     apply false
    alias(libs.plugins.android.library)         apply false
    alias(libs.plugins.kotlin.android)          apply false
    alias(libs.plugins.kotlin.compose)          apply false
    alias(libs.plugins.kotlin.serialization)    apply false
    alias(libs.plugins.ksp)                     apply false
    alias(libs.plugins.hilt)                    apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.spotless)
}

// ─────────────────────────────────────────────────────────────────────────────
// Detekt — Static Analysis
// ─────────────────────────────────────────────────────────────────────────────
detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    source.setFrom(
        files(
            "$rootDir/app/src/main/java",
            "$rootDir/app/src/test/java",
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Spotless — Code Formatting
// ─────────────────────────────────────────────────────────────────────────────
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint("1.0.1")
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeader(
            """
            /*
             * Offline Emergency Mesh Communication System
             * Copyright (c) ${"$"}YEAR. All rights reserved.
             */
            """.trimIndent()
        )
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**/*.gradle.kts")
        ktlint("1.0.1")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// KtLint — Root-level config
// ─────────────────────────────────────────────────────────────────────────────
ktlint {
    version.set("1.0.1")
    debug.set(false)
    verbose.set(true)
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        include("**/kotlin/**")
    }
}
