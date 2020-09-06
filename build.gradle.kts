/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version versions.kotlin apply false
  id("org.jetbrains.dokka") version versions.dokka apply false
  id("com.diffplug.gradle.spotless") version versions.spotless
  id("com.vanniktech.maven.publish") version versions.mavenPublish apply false
}

spotless {
  kotlin {
    target("**/*.kt")
    ktlint(versions.ktlint).userData(mapOf("indent_size" to "2"))
    trimTrailingWhitespace()
    endWithNewline()
  }
}

subprojects {
  repositories {
    mavenCentral()
    jcenter()
  }
  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "1.8"
      languageVersion = "1.3"
      freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
  }
  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    configure<KotlinProjectExtension> {
      explicitApi()
    }
  }

  apply(plugin = "org.jetbrains.kotlin.jvm")
  if (name != "kotlinpoet-metadata-specs-tests") {
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.vanniktech.maven.publish")
    afterEvaluate {
      tasks.named<DokkaTask>("dokkaHtml") {
        outputDirectory.set(rootDir.resolve("docs/1.x"))
        dokkaSourceSets.configureEach {
          skipDeprecated.set(true)
        }
      }
    }
  }
}
