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
import com.vanniktech.maven.publish.MavenPublishPluginExtension
import com.vanniktech.maven.publish.SonatypeHost.S01
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.mavenPublish) apply false
}

allprojects {
  group = property("GROUP") as String
  version = property("VERSION_NAME") as String

  repositories {
    mavenCentral()
  }
}

subprojects {
  tasks.withType<KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }
  }
  // Ensure "org.gradle.jvm.version" is set to "8" in Gradle metadata.
  tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
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
    configure<MavenPublishPluginExtension> {
      sonatypeHost = S01
    }
  }

  apply(plugin = "com.diffplug.spotless")
  configure<SpotlessExtension> {
    kotlin {
      target("**/*.kt")
      ktlint(libs.versions.ktlint.get()).userData(mapOf("indent_size" to "2"))
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

  // Copied from https://github.com/square/retrofit/blob/master/retrofit/build.gradle#L28.
  // Create a test task for each supported JDK.
  for (majorVersion in 8..18) {
    // Adoptium JDK 9 cannot extract on Linux or Mac OS.
    if (majorVersion == 9) continue

    val jdkTest = tasks.register<Test>("testJdk$majorVersion") {
      val javaToolchains = project.extensions.getByType(JavaToolchainService::class)
      javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(majorVersion))
      })

      description = "Runs the test suite on JDK $majorVersion"
      group = LifecycleBasePlugin.VERIFICATION_GROUP

      // Copy inputs from normal Test task.
      val testTask = tasks.getByName<Test>("test")
      classpath = testTask.classpath
      testClassesDirs = testTask.testClassesDirs
    }
    tasks.named("check").configure {
      dependsOn(jdkTest)
    }
  }
}
