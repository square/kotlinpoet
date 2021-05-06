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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
  freeCompilerArgs = listOf("-Xjvm-default=all")
}

dependencies {
  testImplementation(project(":kotlinpoet"))
  testImplementation(project(":interop:kotlinx-metadata:core"))
  testImplementation(project(":interop:kotlinx-metadata:specs"))
  testImplementation(project(":interop:kotlinx-metadata:classinspectors:elements"))
  testImplementation(project(":interop:kotlinx-metadata:classinspectors:reflect"))
  testImplementation(deps.kotlin.junit)
  testImplementation(deps.test.truth)
  testImplementation(deps.test.compileTesting)
  testImplementation(deps.test.kotlinCompileTesting)
}
