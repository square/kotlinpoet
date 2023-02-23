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
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

tasks.jar {
  manifest {
    attributes("Automatic-Module-Name" to "com.squareup.kotlinpoet")
  }
}

tasks.compileTestKotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=com.squareup.kotlinpoet.DelicateKotlinPoetApi")
  }
}

spotless {
  kotlin {
    targetExclude(
      // Non-Square licensed files
      "src/main/java/com/squareup/kotlinpoet/ClassName.kt",
      "src/test/java/com/squareup/kotlinpoet/AbstractTypesTest.kt",
      "src/test/java/com/squareup/kotlinpoet/ClassNameTest.kt",
      "src/test/java/com/squareup/kotlinpoet/TypesEclipseTest.kt",
      "src/test/java/com/squareup/kotlinpoet/TypesTest.kt",
    )
  }
}

dependencies {
  implementation(libs.kotlin.reflect)
  testImplementation(libs.kotlin.junit)
  testImplementation(libs.truth)
  testImplementation(libs.compileTesting)
  testImplementation(libs.jimfs)
  testImplementation(libs.ecj)
  testImplementation(libs.kotlinCompileTesting)
  testImplementation(libs.kotlin.annotationProcessingEmbeddable)
  testImplementation(libs.kotlin.compilerEmbeddable)
}
