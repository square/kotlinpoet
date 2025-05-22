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
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
  kotlin("multiplatform")
}

spotless {
  kotlin {
    targetExclude(
      // Non-Square licensed files
      "src/*Main/kotlin/com/squareup/kotlinpoet/ClassName.kt",
      "src/*Main/kotlin/com/squareup/kotlinpoet/ClassName.*.kt",
      "src/*Test/kotlin/com/squareup/kotlinpoet/AbstractTypesTest.kt",
      "src/*Test/kotlin/com/squareup/kotlinpoet/ClassNameTest.kt",
      "src/*Test/kotlin/com/squareup/kotlinpoet/TypesEclipseTest.kt",
      "src/*Test/kotlin/com/squareup/kotlinpoet/TypesTest.kt",
    )
  }
}

kotlin {
  jvm()

  js {
    nodejs {
      testTask {
        useMocha()
      }
    }
    binaries.library()
  }

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    nodejs {
      testTask {
        useMocha()
      }
    }
    binaries.library()
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  compilerOptions {
    allWarningsAsErrors = true
    optIn.add("com.squareup.kotlinpoet.DelicateKotlinPoetApi")
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlin.reflect)
      }
    }

    commonTest {
      dependencies {
        implementation(kotlin("test"))
      }
    }

    jvmTest {
      dependencies {
        implementation(libs.kotlin.junit)
        implementation(libs.truth)
        implementation(libs.compileTesting)
        implementation(libs.jimfs)
        implementation(libs.ecj)
        implementation(libs.kotlinCompileTesting)
        implementation(libs.kotlin.annotationProcessingEmbeddable)
        implementation(libs.kotlin.compilerEmbeddable)
      }
    }

    val nonJvmMain by creating {
      dependsOn(commonMain.get())
    }

    jsMain {
      dependsOn(nonJvmMain)
    }
    wasmJsMain {
      dependsOn(nonJvmMain)
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
