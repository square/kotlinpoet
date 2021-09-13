/*
  * Copyright (C) 2021 Square, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *    https://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package com.squareup.kotlinpoet.ksp.test.processor

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TestProcessorTest {

  @Rule
  @JvmField
  val temporaryFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun smokeTest() {
    val compilation = prepareCompilation(
      kotlin(
        "Example.kt",
        """
           package test
           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotation

           @ExampleAnnotation
           class SmokeTestClass<T, R : Any, E : Enum<E>> {
             private val propA: String = ""
             internal val propB: String = ""
             val propC: Int = 0
             val propD: Int? = null
             lateinit var propE: String
             var propF: T? = null

             fun functionA(): String {
               TODO()
             }

             fun functionB(): R {
               TODO()
             }

             fun <F> functionC(param1: String, param2: T, param3: F, param4: F?): R {
               TODO()
             }
           }
           """
      )
    )
    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestSmokeTestClass.kt")
      .readText()
    assertThat(generatedFileText).isEqualTo(
      """
      TODO
      """.trimIndent()
    )
  }

  // TODO
  //  - writeTo
  //  - originating files
  //  - typealias
  //  - generic typealias
  //  - class
  //  - generic class
  //  - function
  //  - generic function
  //  - generic property
  //  - nullable type
  //  - complicated self referencing generics
  //  - generic annotation
  //  - wildcardtypename
  //  - unnamed parameter
  //  - function types
  //  - suspend types

  private fun prepareCompilation(vararg sourceFiles: SourceFile): KotlinCompilation {
    return KotlinCompilation()
      .apply {
        workingDir = temporaryFolder.root
        inheritClassPath = true
        symbolProcessorProviders = listOf(TestProcessorProvider())
        sources = sourceFiles.asList()
        verbose = false
        kspIncremental = true // The default now
      }
  }
}
