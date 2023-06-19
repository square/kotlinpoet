/*
 * Copyright (C) 2019 Square, Inc.
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

tasks.jar {
  manifest {
    attributes("Automatic-Module-Name" to "com.squareup.kotlinpoet.metadata")
  }
}

tasks.compileTestKotlin {
  compilerOptions {
    freeCompilerArgs.addAll(
      "-Xjvm-default=all",
      "-opt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview",
      "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
    )
  }
}

dependencies {
  implementation(libs.autoCommon)
  implementation(libs.guava)
  api(libs.kotlin.metadata)
  api(projects.kotlinpoet)

  testImplementation(libs.kotlin.junit)
  testImplementation(libs.truth)
  testImplementation(libs.compileTesting)
  testImplementation(libs.kotlinCompileTesting)
  testImplementation(libs.kotlin.annotationProcessingEmbeddable)
  testImplementation(libs.kotlin.compilerEmbeddable)
}
