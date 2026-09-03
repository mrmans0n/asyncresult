// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp.library)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.dokka)
}

kotlin {
  jvm()
  androidLibrary {
    namespace = "io.nlopez.asyncresult.test"
    compileSdk = 36
    minSdk = 21
    withHostTest {}
  }
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
