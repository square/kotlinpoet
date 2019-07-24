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
package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.km.KotlinPoetKm
import org.junit.Test

@KotlinPoetKm
@Suppress("unused", "UNUSED_PARAMETER")
class KmSpecsTest {

  @Test
  fun constructorData() {
    val typeSpec = ConstructorClass::class.toTypeSpec()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class ConstructorClass(
        val foo: kotlin.String,
        vararg bar: kotlin.Int
      ) {
        constructor(bar: kotlin.Int)
      }
    """.trimIndent())
  }

  class ConstructorClass(val foo: String, vararg bar: Int) {
    // Secondary constructors are ignored, so we expect this constructor to not be the one picked
    // up in the test.
    constructor(bar: Int) : this("defaultFoo")
  }

  @Test
  fun supertype() {
    val typeSpec = Supertype::class.toTypeSpec()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Supertype : com.squareup.kotlinpoet.KmSpecsTest.BaseType(), com.squareup.kotlinpoet.KmSpecsTest.BaseInterface
    """.trimIndent())
  }

  abstract class BaseType
  interface BaseInterface
  class Supertype : BaseType(), BaseInterface

  @Test
  fun properties() {
    val typeSpec = Properties::class.toTypeSpec()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Properties {
        var aList: kotlin.collections.List<kotlin.Int> = TODO("Stub!")
      
        val bar: kotlin.String? = null
      
        var baz: kotlin.Int = TODO("Stub!")
      
        val foo: kotlin.String = TODO("Stub!")
      }
    """.trimIndent())
  }

  class Properties {
    val foo: String = ""
    val bar: String? = null
    var baz: Int = 0
    var aList: List<Int> = emptyList()
  }

  @Test
  fun companionObject() {
    val typeSpec = CompanionObject::class.toTypeSpec()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class CompanionObject {
        companion object
      }
    """.trimIndent())
  }

  class CompanionObject {
    companion object
  }

  @Test
  fun namedCompanionObject() {
    val typeSpec = NamedCompanionObject::class.toTypeSpec()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class NamedCompanionObject {
        companion object Named
      }
    """.trimIndent())
  }

  class NamedCompanionObject {
    companion object Named
  }

  @Test
  fun generics() {
    val typeSpec = Generics::class.toTypeSpec()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Generics<T, in R, V>(
        val genericInput: T
      )
    """.trimIndent())
  }

  class Generics<out T, in R, V>(val genericInput: T)

  @Test
  fun typeAliases() {
    val typeSpec = TypeAliases::class.toTypeSpec()

    // We always resolve the underlying type of typealiases
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class TypeAliases(
        val foo: kotlin.String,
        val bar: kotlin.collections.List<kotlin.String>
      )
    """.trimIndent())
  }

  class TypeAliases(val foo: TypeAliasName, val bar: GenericTypeAlias)

  @Test
  fun propertyMutability() {
    val typeSpec = PropertyMutability::class.toTypeSpec()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class PropertyMutability(
        val foo: kotlin.String,
        var mutableFoo: kotlin.String
      )
    """.trimIndent())
  }

  class PropertyMutability(val foo: String, var mutableFoo: String)

  @Test
  fun collectionMutability() {
    val typeSpec = CollectionMutability::class.toTypeSpec()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class CollectionMutability(
        val immutableList: kotlin.collections.List<kotlin.String>,
        val mutableList: kotlin.collections.MutableList<kotlin.String>
      )
    """.trimIndent())
  }

  class CollectionMutability(val immutableList: List<String>, val mutableList: MutableList<String>)

  @Test
  fun suspendTypes() {
    val typeSpec = SuspendTypes::class.toTypeSpec()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class SuspendTypes {
        val testProp: suspend (kotlin.Int, kotlin.Long) -> kotlin.String = TODO("Stub!")

        suspend fun testComplexSuspendFun(body: suspend (kotlin.Int, suspend (kotlin.Long) -> kotlin.String) -> kotlin.String) {
        }

        fun testFun(body: suspend (kotlin.Int, kotlin.Long) -> kotlin.String) {
        }

        suspend fun testSuspendFun(param1: kotlin.String) {
        }
      }
    """.trimIndent())
  }

  class SuspendTypes {
    val testProp: suspend (Int, Long) -> String = { _, _ -> "" }

    fun testFun(body: suspend (Int, Long) -> String) {
    }

    suspend fun testSuspendFun(param1: String) {
    }

    suspend fun testComplexSuspendFun(body: suspend (Int, suspend (Long) -> String) -> String) {
    }
  }

  @Test
  fun parameters() {
    val typeSpec = Parameters::class.toTypeSpec()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class Parameters {
        inline fun hasDefault(param1: kotlin.String = TODO("Stub!")) {
        }

        inline fun inline(crossinline param1: () -> kotlin.String) {
        }

        inline fun noinline(noinline param1: () -> kotlin.String): kotlin.String {
          TODO("Stub!")
        }
      }
    """.trimIndent())
  }

  class Parameters {
    inline fun inline(crossinline param1: () -> String) {
    }

    inline fun noinline(noinline param1: () -> String): String {
      return ""
    }

    inline fun hasDefault(param1: String = "Nope") {
    }
  }

  @Test
  fun lambdaReceiver() {
    val typeSpec = LambdaReceiver::class.toTypeSpec()
    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class LambdaReceiver {
        fun lambdaReceiver(block: kotlin.String.() -> kotlin.Unit) {
        }

        fun lambdaReceiver2(block: kotlin.String.(kotlin.Int) -> kotlin.Unit) {
        }

        fun lambdaReceiver3(block: kotlin.String.(kotlin.Int, kotlin.String) -> kotlin.Unit) {
        }
      }
    """.trimIndent())
  }

  class LambdaReceiver {
    fun lambdaReceiver(block: String.() -> Unit) {
    }
    fun lambdaReceiver2(block: String.(Int) -> Unit) {
    }
    fun lambdaReceiver3(block: String.(Int, String) -> Unit) {
    }
  }

  @Test
  fun nestedTypeAlias() {
    val typeSpec = NestedTypeAliasTest::class.toTypeSpec()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class NestedTypeAliasTest {
        val prop: kotlin.collections.List<kotlin.collections.List<kotlin.String>> = TODO("Stub!")
      }
    """.trimIndent())
  }

  class NestedTypeAliasTest {
    val prop: NestedTypeAlias = listOf(listOf(""))
  }

  @Test
  fun inlineClass() {
    val typeSpec = InlineClass::class.toTypeSpec()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      inline class InlineClass(
        val value: kotlin.String
      )
    """.trimIndent())
  }

  @Test
  fun functionReferencingTypeParam() {
    val typeSpec = FunctionsReferencingTypeParameters::class.toTypeSpec()

    //language=kotlin
    assertThat(typeSpec.trimmedToString()).isEqualTo("""
      class FunctionsReferencingTypeParameters<T> {
        fun test(param: T) {
        }
      }
    """.trimIndent())
  }

  class FunctionsReferencingTypeParameters<T> {
    fun test(param: T) {

    }
  }

  // TODO Overridden properties and functions
  // TODO Delegation (class, properties, local vars)
  // TODO Enums (simple and complex)
  // TODO Complex companion objects (implementing interfaces)
  // TODO Tagged km types
  // TODO Backward referencing type arguments (T, B<T>)
  // TODO Excluding delegated, only including declared
}

private fun TypeSpec.trimmedToString(): String {
  return toString().trim()
}

inline class InlineClass(val value: String)

typealias TypeAliasName = String
typealias GenericTypeAlias = List<String>
typealias NestedTypeAlias = List<GenericTypeAlias>
