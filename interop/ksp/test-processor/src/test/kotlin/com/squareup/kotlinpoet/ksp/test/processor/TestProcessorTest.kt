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
import com.tschuchort.compiletesting.kspArgs
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

           typealias TypeAliasName = String
           typealias GenericTypeAlias = List<String>

           @ExampleAnnotation
           class SmokeTestClass<T, R : Any, E : Enum<E>> {
             private val propA: String = ""
             internal val propB: String = ""
             val propC: Int = 0
             val propD: Int? = null
             lateinit var propE: String
             var propF: T? = null

             fun functionA(): String {
               error()
             }

             fun functionB(): R {
               error()
             }

             fun <F> functionC(param1: String, param2: T, param3: F, param4: F?): R {
               error()
             }

             suspend fun functionD(
               param1: () -> String,
               param2: (String) -> String,
               param3: String.() -> String
             ) {
             }

             // A whole bunch of wild types from Moshi's codegen smoke tests
             fun wildTypes(
               age: Int,
               nationalities: List<String>,
               weight: Float,
               tattoos: Boolean = false,
               race: String?,
               hasChildren: Boolean = false,
               favoriteFood: String? = null,
               favoriteDrink: String? = "Water",
               wildcardOut: MutableList<out String>,
               nullableWildcardOut: MutableList<out String?>,
               wildcardIn: Array<in String>,
               any: List<*>,
               anyTwo: List<Any>,
               anyOut: MutableList<out Any>,
               nullableAnyOut: MutableList<out Any?>,
               favoriteThreeNumbers: IntArray,
               favoriteArrayValues: Array<String>,
               favoriteNullableArrayValues: Array<String?>,
               nullableSetListMapArrayNullableIntWithDefault: Set<List<Map<String, Array<IntArray?>>>>?,
               // These are actually currently rendered incorrectly and always unwrapped
               aliasedName: TypeAliasName,
               genericAlias: GenericTypeAlias,
               nestedArray: Array<Map<String, Any>>?
             ) {

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
      package test

      import kotlin.Any
      import kotlin.Array
      import kotlin.Boolean
      import kotlin.Enum
      import kotlin.Float
      import kotlin.Function0
      import kotlin.Function1
      import kotlin.Int
      import kotlin.IntArray
      import kotlin.String
      import kotlin.Unit
      import kotlin.collections.List
      import kotlin.collections.Map
      import kotlin.collections.MutableList
      import kotlin.collections.Set

      public class SmokeTestClass<T, R : Any, E : Enum<E>> {
        private val propA: String

        internal val propB: String

        public val propC: Int

        public val propD: Int?

        public lateinit var propE: String

        public var propF: T?

        public fun functionA(): String {
        }

        public fun functionB(): R {
        }

        public fun <F> functionC(
          param1: String,
          param2: T,
          param3: F,
          param4: F?
        ): R {
        }

        public suspend fun functionD(
          param1: Function0<String>,
          param2: Function1<String, String>,
          param3: Function1<String, String>
        ): Unit {
        }

        public fun wildTypes(
          age: Int,
          nationalities: List<String>,
          weight: Float,
          tattoos: Boolean,
          race: String?,
          hasChildren: Boolean,
          favoriteFood: String?,
          favoriteDrink: String?,
          wildcardOut: MutableList<out String>,
          nullableWildcardOut: MutableList<out String?>,
          wildcardIn: Array<in String>,
          any: List<*>,
          anyTwo: List<Any>,
          anyOut: MutableList<out Any>,
          nullableAnyOut: MutableList<*>,
          favoriteThreeNumbers: IntArray,
          favoriteArrayValues: Array<String>,
          favoriteNullableArrayValues: Array<String?>,
          nullableSetListMapArrayNullableIntWithDefault: Set<List<Map<String, Array<IntArray?>>>>?,
          aliasedName: TypeAliasName,
          genericAlias: GenericTypeAlias<String>,
          nestedArray: Array<Map<String, Any>>?
        ): Unit {
        }
      }

      """.trimIndent()
    )
  }

  @Test
  fun unwrapTypeAliases() {
    val compilation = prepareCompilation(
      kotlin(
        "Example.kt",
        """
           package test

           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotation

           typealias TypeAliasName = String
           typealias GenericTypeAlias = List<String>

           @ExampleAnnotation
           class Example {
             fun aliases(
               aliasedName: TypeAliasName,
               genericAlias: GenericTypeAlias
             ) {
             }
           }
           """
      )
    )
    compilation.kspArgs["unwrapTypeAliases"] = "true"
    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestExample.kt")
      .readText()
    assertThat(generatedFileText).isEqualTo(
      """
      package test

      import kotlin.String
      import kotlin.Unit
      import kotlin.collections.List

      public class Example {
        public fun aliases(aliasedName: String, genericAlias: List<String>): Unit {
        }
      }

      """.trimIndent()
    )
  }

  // TODO
  //  - complicated self referencing generics
  //  - unnamed parameter

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
