// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: MIT
import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.spotless) apply false
}

allprojects {
  val libs = rootProject.libs

  pluginManager.apply(libs.plugins.spotless.get().pluginId)
  configure<SpotlessExtension> {
    val ktfmtVersion = libs.versions.ktfmt.get()
    kotlin {
      target("**/*.kt")
      targetExclude("**/build/**", ".throwaway/**")
      ktfmt(ktfmtVersion)
      licenseHeaderFile(rootProject.file("spotless/copyright.txt"))
    }
    kotlinGradle {
      target("*.kts")
      ktfmt(ktfmtVersion)
    }
  }

  tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
      allWarningsAsErrors = true
      jvmTarget.set(JvmTarget.JVM_11)
      freeCompilerArgs.addAll(
          "-Xjvm-default=all",
      )
    }
  }

  version = project.property("VERSION_NAME") ?: "0.0.0"
}

tasks.register("printVersion") { doLast { println(project.property("VERSION_NAME")) } }
