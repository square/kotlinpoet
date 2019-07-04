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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.github.johnrengelman.shadow") version "5.0.0" apply false
  id("com.vanniktech.maven.publish") version "0.8.0"
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

tasks.named<Jar>("jar") {
  manifest {
    attributes("Automatic-Module-Name" to "com.squareup.kotlinpoet.km")
  }
}

afterEvaluate {
  tasks.named<DokkaTask>("dokka") {
    skipDeprecated = true
    outputFormat = "html"
  }
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
  }
}

dependencies {
  api("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
  api("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.1.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
  testImplementation("com.google.truth:truth:0.42")
}

repositories {
  mavenCentral()
}
