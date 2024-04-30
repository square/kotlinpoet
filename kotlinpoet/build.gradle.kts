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
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  kotlin("multiplatform")
}

spotless {
  kotlin {
    targetExclude(
      // Non-Square licensed files
      "src/commonMain/kotlin/com/squareup/kotlinpoet/ClassName.kt",
      "src/commonTest/kotlin/com/squareup/kotlinpoet/AbstractTypesTest.kt",
      "src/commonTest/kotlin/com/squareup/kotlinpoet/ClassNameTest.kt",
      "src/commonTest/kotlin/com/squareup/kotlinpoet/TypesEclipseTest.kt",
      "src/commonTest/kotlin/com/squareup/kotlinpoet/TypesTest.kt",
    )
  }
}

kotlin {
  jvm {
    withJava()
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  compilerOptions {
    allWarningsAsErrors.set(true)
    optIn.add("com.squareup.kotlinpoet.DelicateKotlinPoetApi")
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotlin.reflect)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.junit)
        implementation(libs.truth)
        implementation(libs.guava)
        implementation(libs.compileTesting)
        implementation(libs.jimfs)
        implementation(libs.ecj)
        implementation(libs.kotlinCompileTesting)
        implementation(libs.kotlin.annotationProcessingEmbeddable)
        implementation(libs.kotlin.compilerEmbeddable)
      }
    }
  }
}

tasks.withType(org.gradle.jvm.tasks.Jar::class.java) {
  manifest {
    attributes("Automatic-Module-Name" to "com.squareup.kotlinpoet")
  }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlinJvm") {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=com.squareup.kotlinpoet.DelicateKotlinPoetApi")
  }
}

