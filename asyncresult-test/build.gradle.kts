// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.library)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.dokka)
}

kotlin {
  jvm()
  androidTarget { publishLibraryVariants("release") }
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  js(IR) {
    browser()
    nodejs()
  }
  @OptIn(ExperimentalWasmDsl::class) wasmJs { browser() }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.asyncresult)
        api(libs.assertk)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.kotlinx.coroutines.test)
      }
    }
  }
}

android {
  namespace = "io.nlopez.asyncresult.test"
  compileSdk = 34

  defaultConfig { minSdk = 21 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}
