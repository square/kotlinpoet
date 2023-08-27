/*
 * Copyright (C) 2021 Square, Inc.
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
package com.squareup.kotlinpoet.ksp.test.processor

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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

           import com.squareup.kotlinpoet.ksp.test.processor.AnnotationEnumValue
           import com.squareup.kotlinpoet.ksp.test.processor.AnotherAnnotation
           import com.squareup.kotlinpoet.ksp.test.processor.ComprehensiveAnnotation
           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotation
           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotationWithDefaults

           typealias TypeAliasName = String
           typealias GenericTypeAlias = List<String>
           typealias ParameterizedTypeAlias<T> = List<T>

           @ComprehensiveAnnotation<String>(
             true, // Omit the name intentionally here to test names are still picked up
             booleanArray = [true],
             byte = 0.toByte(),
             byteArray = [0.toByte()],
             char = 'a',
             charArray = ['a', 'b', 'c'],
             short = 0.toShort(),
             shortArray = [0.toShort()],
             int = 0,
             intArray = [0],
             long = 0L,
             longArray = [0L],
             float = 0f,
             floatArray = [0f],
             double = 0.0,
             doubleArray = [0.0],
             string = "Hello",
             stringArray = ["Hello"],
             someClass = String::class,
             someClasses = [String::class, Int::class],
             enumValue = AnnotationEnumValue.ONE,
             enumValueArray = [AnnotationEnumValue.ONE, AnnotationEnumValue.TWO],
             anotherAnnotation = AnotherAnnotation("Hello"),
             anotherAnnotationArray = [AnotherAnnotation("Hello")]
           )
           @ExampleAnnotationWithDefaults(
             true, // Omit the name intentionally here to test names are still picked up
             booleanArray = [false],
             byte = 0.toByte(),
             byteArray = [1.toByte()],
             char = 'C',
             charArray = ['C'],
             short = 0.toShort(),
             shortArray = [1.toShort()],
             int = 0,
             intArray = [1],
             long = 0L,
             longArray = [1L],
             float = 0f,
             floatArray = [1f],
             double = 1.0,
             doubleArray = [0.0],
             string = "Hello",
             stringArray = [""],
             someClass = String::class,
             someClasses = [Int::class],
             enumValue = AnnotationEnumValue.ONE,
             enumValueArray = [AnnotationEnumValue.ONE, AnnotationEnumValue.TWO],
             anotherAnnotation = AnotherAnnotation(""),
             anotherAnnotationArray = [AnotherAnnotation("Hello")]
           )
           @ExampleAnnotation
           class SmokeTestClass<T, R : Any, E : Enum<E>> {
             @field:AnotherAnnotation("siteTargeting")
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
               parameterizedTypeAlias: ParameterizedTypeAlias<String>,
               nestedArray: Array<Map<String, Any>>?
             ) {

             }
           }
           """,
      ),
    )
    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestSmokeTestClass.kt")
      .readText()
    assertThat(generatedFileText).isEqualTo(
      """
      package test

      import com.squareup.kotlinpoet.ksp.test.processor.AnnotationEnumValue
      import com.squareup.kotlinpoet.ksp.test.processor.AnotherAnnotation
      import com.squareup.kotlinpoet.ksp.test.processor.ComprehensiveAnnotation
      import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotationWithDefaults
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
      import kotlin.collections.List
      import kotlin.collections.Map
      import kotlin.collections.MutableList
      import kotlin.collections.Set

      @ComprehensiveAnnotation<String>(
        boolean = true,
        booleanArray = booleanArrayOf(true),
        byte = 0.toByte(),
        byteArray = byteArrayOf(0.toByte()),
        char = 'a',
        charArray = charArrayOf('a', 'b', 'c'),
        short = 0.toShort(),
        shortArray = shortArrayOf(0.toShort()),
        int = 0,
        intArray = intArrayOf(0),
        long = 0,
        longArray = longArrayOf(0),
        float = 0.0f,
        floatArray = floatArrayOf(0.0f),
        double = 0.0,
        doubleArray = doubleArrayOf(0.0),
        string = "Hello",
        stringArray = arrayOf("Hello"),
        someClass = String::class,
        someClasses = arrayOf(String::class, Int::class),
        enumValue = AnnotationEnumValue.ONE,
        enumValueArray = arrayOf(AnnotationEnumValue.ONE, AnnotationEnumValue.TWO),
        anotherAnnotation = AnotherAnnotation(input = "Hello"),
        anotherAnnotationArray = arrayOf(AnotherAnnotation(input = "Hello")),
        defaultingString = "defaultValue",
      )
      @ExampleAnnotationWithDefaults(
        booleanArray = booleanArrayOf(false),
        byte = 0.toByte(),
        short = 0.toShort(),
        int = 0,
        long = 0,
        float = 0.0f,
        doubleArray = doubleArrayOf(0.0),
        string = "Hello",
        someClasses = arrayOf(Int::class),
        enumValueArray = arrayOf(AnnotationEnumValue.ONE, AnnotationEnumValue.TWO),
      )
      public class SmokeTestClass<T, R : Any, E : Enum<E>> {
        @field:AnotherAnnotation(input = "siteTargeting")
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
          param4: F?,
        ): R {
        }

        public suspend fun functionD(
          param1: Function0<String>,
          param2: Function1<String, String>,
          param3: Function1<String, String>,
        ) {
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
          genericAlias: GenericTypeAlias,
          parameterizedTypeAlias: ParameterizedTypeAlias<String>,
          nestedArray: Array<Map<String, Any>>?,
        ) {
        }
      }

      """.trimIndent(),
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
           typealias GenericMapTypeAlias<V, K> = Map<K, V>
           typealias T1Unused<T1, T2> = Map<T2, String>
           typealias A1<T1, T2> = A2<T2, T1>
           typealias A2<T2, T3> = Map<T3, T2>

           @ExampleAnnotation
           class Example {
             fun aliases(
               aliasedName: TypeAliasName,
               genericAlias: GenericTypeAlias,
               genericMapAlias: GenericMapTypeAlias<String, Int>,
               t1Unused: T1Unused<String, Int>,
               a1: A1<String, Int>,
             ) {
             }
           }
           """,
      ),
    )
    compilation.kspArgs["unwrapTypeAliases"] = "true"
    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestExample.kt")
      .readText()
    assertThat(generatedFileText).isEqualTo(
      """
      package test

      import kotlin.Int
      import kotlin.String
      import kotlin.collections.List
      import kotlin.collections.Map

      public class Example {
        public fun aliases(
          aliasedName: String,
          genericAlias: List<String>,
          genericMapAlias: Map<Int, String>,
          t1Unused: Map<Int, String>,
          a1: Map<String, Int>,
        ) {
        }
      }

      """.trimIndent(),
    )
  }

  @Test
  fun removeDefaultValues() {
    val compilation = prepareCompilation(
      kotlin(
        "Example.kt",
        """
           package test

           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotationWithDefaults
           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotation
           import com.squareup.kotlinpoet.ksp.test.processor.AnotherAnnotation
           import com.squareup.kotlinpoet.ksp.test.processor.AnnotationEnumValue

           @ExampleAnnotation
           @ExampleAnnotationWithDefaults(
             true, // Omit the name intentionally here to test names are still picked up
             booleanArray = [true],
             byte = 1.toByte(),
             byteArray = [1.toByte()],
             char = 'C',
             charArray = ['C'],
             short = 1.toShort(),
             shortArray = [1.toShort()],
             int = 1,
             intArray = [1],
             long = 1L,
             longArray = [1L],
             float = 1f,
             floatArray = [1f],
             double = 1.0,
             doubleArray = [1.0],
             string = "",
             stringArray = [""],
             someClass = String::class,
             someClasses = [String::class],
             enumValue = AnnotationEnumValue.ONE,
             enumValueArray = [AnnotationEnumValue.ONE],
             anotherAnnotation = AnotherAnnotation(""),
             anotherAnnotationArray = [AnotherAnnotation("")]
           )
           open class Node<T : Node<T, R>, R : Node<R, T>> {
             var t: T? = null
             var r: R? = null
           }
           """,
      ),
    )

    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestNode.kt")
      .readText()
    assertThat(generatedFileText).isEqualTo(
      """
      package test

      import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotationWithDefaults

      @ExampleAnnotationWithDefaults
      public open class Node<T : Node<T, R>, R : Node<R, T>> {
        public var t: T?

        public var r: R?
      }

      """.trimIndent(),
    )
  }

  @Test
  fun complexSelfReferencingTypeArgs() {
    val compilation = prepareCompilation(
      kotlin(
        "Example.kt",
        """
           package test

           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotation

           @ExampleAnnotation
           open class Node<T : Node<T, R>, R : Node<R, T>> {
             var t: T? = null
             var r: R? = null
           }
           """,
      ),
    )

    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestNode.kt")
      .readText()
    assertThat(generatedFileText).isEqualTo(
      """
      package test

      public open class Node<T : Node<T, R>, R : Node<R, T>> {
        public var t: T?

        public var r: R?
      }

      """.trimIndent(),
    )
  }

  @Test
  fun wildcardParameterForRecursiveTypeBound() {
    // Enum is an example of a recursive type bound - Enum<E: Enum<E>>
    val compilation = prepareCompilation(
      kotlin(
        "Example.kt",
        """
           package test

           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotation

           @ExampleAnnotation
           class EnumWrapper {
            val enumValue: Enum<*>
           }
           """,
      ),
    )

    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestEnumWrapper.kt")
      .readText()
    assertThat(generatedFileText).isEqualTo(
      """
      package test

      import kotlin.Enum

      public class EnumWrapper {
        public val enumValue: Enum<*>
      }

      """.trimIndent(),
    )
  }

  @Test
  fun transitiveAliases() {
    val compilation = prepareCompilation(
      kotlin(
        "Example.kt",
        """
           package test

           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotation

           typealias Alias23 = (Any) -> Any
           typealias Alias77<Q> = List<Q>
           typealias Alias73<Q> = Map<String, Q>
           typealias Alias55<Q> = Alias73<Q>
           typealias Alias99<Q> = Alias55<Q>
           typealias Alias43<Q> = Alias77<Q>
           typealias Alias47<Q> = Alias43<Q>
           typealias Alias41<Z, Q> = (Alias43<Z>) -> Alias47<Q>

           @ExampleAnnotation
           interface TransitiveAliases {
              fun <T : Alias41<Alias23, out Alias77<Alias73<Int>>>> bar(vararg arg1: T)
           }
           """,
      ),
    )

    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestTransitiveAliases.kt")
      .readText()
    assertThat(generatedFileText).isEqualTo(
      """
    package test

    import kotlin.Int

    public class TransitiveAliases {
      public fun <T : Alias41<Alias23, out Alias77<Alias73<Int>>>> bar(arg1: T) {
      }
    }

      """.trimIndent(),
    )
  }

  @Test
  fun aliasAsTypeArgument() {
    val compilation = prepareCompilation(
      kotlin(
        "Example.kt",
        """
           package test

           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotation

           typealias Alias997 = Map<String, Int>

           @ExampleAnnotation
           interface AliasAsTypeArgument {
              fun bar(arg1: List<Alias997>)
           }
           """,
      ),
    )

    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestAliasAsTypeArgument.kt")
      .readText()

    assertThat(generatedFileText).isEqualTo(
      """
    package test

    import kotlin.collections.List

    public class AliasAsTypeArgument {
      public fun bar(arg1: List<Alias997>) {
      }
    }

      """.trimIndent(),
    )
  }

  @Test
  fun regression_1513() {
    val compilation = prepareCompilation(
      kotlin(
        "Example.kt",
        """
           package test

           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotation

           interface Repository<T>
           @ExampleAnnotation
           class RealRepository @Inject constructor() : Repository<String>
           """,
      ),
    )

    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestRealRepository.kt")
      .readText()

    assertThat(generatedFileText).isEqualTo(
      """
        package test

        import kotlin.String

        public class RealRepository : Repository<String>

      """.trimIndent(),
    )
  }

  @Test
  fun regression_1513_annotation() {
    val compilation = prepareCompilation(
      kotlin(
        "Example.kt",
        """
           package test

           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotation

           annotation class GenericAnnotation<T>

           @ExampleAnnotation
           @GenericAnnotation<String>
           class RealRepository
           """,
      ),
    )

    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestRealRepository.kt")
      .readText()

    assertThat(generatedFileText).isEqualTo(
      """
        package test

        import kotlin.String

        @GenericAnnotation<String>
        public class RealRepository

      """.trimIndent(),
    )
  }

  @Test
  fun regression_1304() {
    val compilation = prepareCompilation(
      kotlin(
        "Example.kt",
        """
           package test

           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotation

           interface Flow<T>
           typealias LeAlias = Map<Int, String>

           @ExampleAnnotation
           class RealRepository {
             lateinit var prop: LeAlias
             lateinit var complicated: Flow<LeAlias>
           }
           """,
      ),
    )

    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestRealRepository.kt")
      .readText()

    assertThat(generatedFileText).isEqualTo(
      """
        package test

        public class RealRepository {
          public lateinit var prop: LeAlias

          public lateinit var complicated: Flow<LeAlias>
        }

      """.trimIndent(),
    )
  }

  @Test
  fun regression_1304_with_type_parameters() {
    val compilation = prepareCompilation(
      kotlin(
        "Example.kt",
        """
           package test

           import com.squareup.kotlinpoet.ksp.test.processor.ExampleAnnotation

           interface Flow<T>
           typealias LeAlias<T> = Flow<T>

           @ExampleAnnotation
           class RealRepository {
             lateinit var prop: LeAlias<String>
           }
           """,
      ),
    )

    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    val generatedFileText = File(compilation.kspSourcesDir, "kotlin/test/TestRealRepository.kt")
      .readText()

    assertThat(generatedFileText).isEqualTo(
      """
        package test

        import kotlin.String

        public class RealRepository {
          public lateinit var prop: LeAlias<String>
        }

      """.trimIndent(),
    )
  }

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
