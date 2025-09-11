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
  kotlin("jvm")
}

tasks.compileTestKotlin {
  compilerOptions {
    optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
  }
}

tasks.test {
  // KSP2 needs more memory to run
  minHeapSize = "1g"
  maxHeapSize = "4g"
}

dependencies {
  implementation(projects.kotlinpoet)
  implementation(projects.interop.ksp)
  implementation(libs.autoService)
  compileOnly(libs.ksp.api)
  ksp(libs.autoService.ksp)
  // Always force the latest version of the KSP/kotlin impl in tests to match what we're building against
  testImplementation(libs.ksp.api)
  testImplementation(libs.kotlin.compilerEmbeddable)
  testImplementation(libs.kotlin.annotationProcessingEmbeddable)
  testImplementation(libs.ksp)
  testImplementation(libs.kotlinCompileTesting)
  testImplementation(libs.kotlinCompileTesting.ksp)
  testImplementation(libs.ksp.aaEmbeddable)
  testImplementation(libs.kotlin.junit)
  testImplementation(libs.assertk)
}
