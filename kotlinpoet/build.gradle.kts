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
  id("com.vanniktech.maven.publish") version versions.mavenPublish
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

tasks.named<Jar>("jar") {
  manifest {
    attributes("Automatic-Module-Name" to "com.squareup.kotlinpoet")
  }
}

afterEvaluate {
  tasks.named<DokkaTask>("dokka") {
    skipDeprecated = true
    outputDirectory = "$rootDir/docs/1.x"
    outputFormat = "gfm"
  }
}

dependencies {
  api(rootProject.project("kotlinpoet-km"))
  api(deps.kotlin.stdlib)
  implementation(deps.kotlin.reflect)
  implementation(deps.autoCommon)
  testImplementation(deps.kotlin.junit)
  testImplementation(deps.test.truth)
  testImplementation(deps.test.compileTesting)
  testImplementation(deps.test.jimfs)
  testImplementation(deps.test.ecj)
}

repositories {
  mavenCentral()
  jcenter()
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
  freeCompilerArgs = listOf("-XXLanguage:+InlineClasses", "-Xjvm-default=enable")
  jvmTarget = "1.8"
}
