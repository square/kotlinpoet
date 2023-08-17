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
pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version("0.7.0")
}

include(
  ":kotlinpoet",
  ":interop:javapoet",
  ":interop:kotlinx-metadata",
  ":interop:ksp",
  ":interop:ksp:test-processor",
)

rootProject.name = "kotlinpoet-root"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
