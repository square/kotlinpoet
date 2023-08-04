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

project.tasks.withType(org.gradle.jvm.tasks.Jar::class.java) {
  manifest {
    attributes("Automatic-Module-Name" to "com.squareup.kotlinpoet")
  }
}

project.tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlinJvm") {
  compilerOptions {
    freeCompilerArgs.addAll(
      "-Xjvm-default=all",
      "-opt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview",
      "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
    )
  }
}

kotlin {
  sourceSets {
    val jvmMain by getting {
      dependencies {
        implementation(libs.autoCommon)
        implementation(libs.guava)
        api(libs.kotlin.metadata)
        api(projects.kotlinpoet)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(libs.kotlin.junit)
        implementation(libs.truth)
        implementation(libs.compileTesting)
        implementation(libs.kotlinCompileTesting)
        implementation(libs.kotlin.annotationProcessingEmbeddable)
        implementation(libs.kotlin.compilerEmbeddable)
      }
    }
  }
}
