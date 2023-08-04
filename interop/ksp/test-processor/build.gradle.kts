/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
  id("com.google.devtools.ksp")
}

project.tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlinJvm") {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
  }
}

kotlin {
  sourceSets {
    val jvmMain by getting {
      dependencies {
        implementation(projects.kotlinpoet)
        implementation(projects.interop.ksp)
        implementation(libs.autoService)
        compileOnly(libs.ksp.api)
      }
    }
    val jvmTest by getting {
      dependencies {

        // Always force the latest version of the KSP/kotlin impl in tests to match what we're building against
        implementation(libs.ksp.api)
        implementation(libs.kotlin.compilerEmbeddable)
        implementation(libs.kotlin.annotationProcessingEmbeddable)
        implementation(libs.ksp)
        implementation(libs.kotlinCompileTesting)
        implementation(libs.kotlinCompileTesting.ksp)
        implementation(libs.kotlin.junit)
        implementation(libs.truth)
      }
    }
  }
}

dependencies {
  add("kspJvm", libs.autoService.ksp)
}
