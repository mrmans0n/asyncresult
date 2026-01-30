// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
plugins {
    id("com.gradle.develocity") version "4.3.1"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "asyncresult"
include(
    ":asyncresult",
    ":asyncresult-either",
    ":asyncresult-test",
)
