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

plugins {
  kotlin("jvm") version "1.3.40" apply false
  id("org.jetbrains.dokka") version "0.9.18" apply false
  id("com.diffplug.gradle.spotless") version "3.22.0"
}

spotless {
  kotlin {
    target("**/*.kt")
    ktlint("0.31.0").userData(mapOf("indent_size" to "2"))
    trimTrailingWhitespace()
    endWithNewline()
  }
}

subprojects {
  repositories {
    mavenCentral()
    jcenter()
  }
}
