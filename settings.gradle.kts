// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
pluginManagement {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
}

plugins { id("com.gradle.develocity") version "4.3.2" }

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

rootProject.name = "asyncresult-root"

include(
    ":asyncresult",
    ":asyncresult-either",
    ":asyncresult-test",
)
