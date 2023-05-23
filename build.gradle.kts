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
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.kotlinBinaryCompatibilityValidator)
}

allprojects {
  // Note that the group name for publishing is "com.squareup" and is declared in gradle.properties. It's set to a
  // different value here to disambiguate the Maven coordinates of the :interop:javapoet submodule and the JavaPoet
  // dependency.
  group = "com.squareup.kotlinpoet"
  version = property("VERSION_NAME") as String

  repositories {
    mavenCentral()
  }
}

subprojects {
  tasks.withType<KotlinCompile> {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_1_8)
      freeCompilerArgs.add("-Xjvm-default=all")
    }
  }
  // Ensure "org.gradle.jvm.version" is set to "8" in Gradle metadata.
  tasks.withType<JavaCompile> {
    options.release.set(8)
  }

  apply(plugin = "org.jetbrains.kotlin.jvm")
  if ("test" !in name && buildFile.exists()) {
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.vanniktech.maven.publish")
    configure<KotlinProjectExtension> {
      explicitApi()
    }
    afterEvaluate {
      tasks.named<DokkaTask>("dokkaHtml") {
        val projectFolder = project.path.trim(':').replace(':', '-')
        outputDirectory.set(rootProject.rootDir.resolve("docs/1.x/$projectFolder"))
        dokkaSourceSets.configureEach {
          skipDeprecated.set(true)
        }
      }
    }
  }

  apply(plugin = "com.diffplug.spotless")
  configure<SpotlessExtension> {
    kotlin {
      target("**/*.kt")
      ktlint(libs.versions.ktlint.get()).editorConfigOverride(
        mapOf("ktlint_standard_filename" to "disabled"),
      )
      trimTrailingWhitespace()
      endWithNewline()

      licenseHeader(
        """
        |/*
        | * Copyright (C) ${'$'}YEAR Square, Inc.
        | *
        | * Licensed under the Apache License, Version 2.0 (the "License");
        | * you may not use this file except in compliance with the License.
        | * You may obtain a copy of the License at
        | *
        | * https://www.apache.org/licenses/LICENSE-2.0
        | *
        | * Unless required by applicable law or agreed to in writing, software
        | * distributed under the License is distributed on an "AS IS" BASIS,
        | * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        | * See the License for the specific language governing permissions and
        | * limitations under the License.
        | */
        """.trimMargin()
      )
    }
  }

  // Only enable the extra toolchain tests on CI. Otherwise local development is broken on Apple Silicon macs
  // because there are no matching toolchains for several older JDK versions.
  if ("CI" in System.getenv()) {
    // Copied from https://github.com/square/retrofit/blob/master/retrofit/build.gradle#L28.
    // Create a test task for each supported JDK. We check every "LTS" + current version.
    val versionsToTest = listOf(8, 11, 17, 19)
    for (majorVersion in versionsToTest) {
      val jdkTest = tasks.register<Test>("testJdk$majorVersion") {
        val javaToolchains = project.extensions.getByType(JavaToolchainService::class)
        javaLauncher.set(javaToolchains.launcherFor {
          languageVersion.set(JavaLanguageVersion.of(majorVersion))
          vendor.set(JvmVendorSpec.AZUL)
        })

        description = "Runs the test suite on JDK $majorVersion"
        group = LifecycleBasePlugin.VERIFICATION_GROUP

        // Copy inputs from normal Test task.
        val testTask = tasks.getByName<Test>("test")
        classpath = testTask.classpath
        testClassesDirs = testTask.testClassesDirs

        testLogging {
          exceptionFormat = TestExceptionFormat.FULL
        }
      }
      tasks.named("check").configure {
        dependsOn(jdkTest)
      }
    }
  }
}

apiValidation {
  nonPublicMarkers += "com.squareup.kotlinpoet.ExperimentalKotlinPoetApi"
  ignoredProjects += listOf(
    "interop", // Empty middle package
    "test-processor" // Test only
  )
}
