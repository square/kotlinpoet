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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.km.KotlinPoetKm
import org.junit.Test

@KotlinPoetKm
@Suppress("unused", "UNUSED_PARAMETER")
class KmSpecsTest {

  @Test
  fun constructorData() {
    val typeSpec = ConstructorClass::class.toTypeSpec()

    assertThat(typeSpec.primaryConstructor).isNotNull()
    assertThat(typeSpec.primaryConstructor?.parameters).hasSize(2)
    val fooParam = typeSpec.primaryConstructor!!.parameters[0]
    assertThat(fooParam.name).isEqualTo("foo")
    assertThat(fooParam.type).isEqualTo(String::class.asClassName())
    assertThat(fooParam.modifiers).doesNotContain(KModifier.VARARG)
    val barParam = typeSpec.primaryConstructor!!.parameters[1]
    assertThat(barParam.name).isEqualTo("bar")
    assertThat(barParam.modifiers).contains(KModifier.VARARG)
    assertThat(barParam.type).isEqualTo(INT)
  }

  class ConstructorClass(val foo: String, vararg bar: Int) {
    // Secondary constructors are ignored, so we expect this constructor to not be the one picked
    // up in the test.
    constructor(bar: Int) : this("defaultFoo")
  }

  @Test
  fun supertype() {
    val typeSpec = Supertype::class.toTypeSpec()

    assertThat(typeSpec.superclass).isEqualTo(BaseType::class.asClassName())
    assertThat(typeSpec.superinterfaces).containsKey(BaseInterface::class.asClassName())
  }

  abstract class BaseType
  interface BaseInterface
  class Supertype : BaseType(), BaseInterface

  @Test
  fun properties() {
    val typeSpec = Properties::class.toTypeSpec()

    assertThat(typeSpec.propertySpecs).hasSize(4)

    val fooProp = typeSpec.propertySpecs.find { it.name == "foo" } ?: throw AssertionError(
        "Missing foo property")
    assertThat(fooProp.type).isEqualTo(String::class.asClassName())
    assertThat(fooProp.mutable).isFalse()
    val barProp = typeSpec.propertySpecs.find { it.name == "bar" } ?: throw AssertionError(
        "Missing bar property")
    assertThat(barProp.type).isEqualTo(String::class.asClassName().copy(nullable = true))
    assertThat(barProp.mutable).isFalse()
    val bazProp = typeSpec.propertySpecs.find { it.name == "baz" } ?: throw AssertionError(
        "Missing baz property")
    assertThat(bazProp.type).isEqualTo(Int::class.asClassName())
    assertThat(bazProp.mutable).isTrue()
    val listProp = typeSpec.propertySpecs.find { it.name == "aList" } ?: throw AssertionError(
        "Missing baz property")
    assertThat(listProp.type).isEqualTo(List::class.parameterizedBy(Int::class))
    assertThat(listProp.mutable).isTrue()
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
    assertThat(typeSpec.typeSpecs).hasSize(1)
    val companionObject = typeSpec.typeSpecs.find { it.isCompanion }
    checkNotNull(companionObject)
    assertThat(companionObject.name).isEqualTo("Companion")
  }

  class CompanionObject {
    companion object
  }

  @Test
  fun namedCompanionObject() {
    val typeSpec = NamedCompanionObject::class.toTypeSpec()
    assertThat(typeSpec.typeSpecs).hasSize(1)
    val companionObject = typeSpec.typeSpecs.find { it.isCompanion }
    checkNotNull(companionObject)
    assertThat(companionObject.name).isEqualTo("Named")
  }

  class NamedCompanionObject {
    companion object Named
  }

  @Test
  fun generics() {
    val typeSpec = Generics::class.toTypeSpec()

    assertThat(typeSpec.typeVariables).hasSize(3)
    val tType = typeSpec.typeVariables[0]
    assertThat(tType.name).isEqualTo("T")
    assertThat(tType.isReified).isFalse()
    assertThat(tType.variance).isNull() // we don't redeclare out variance
    val rType = typeSpec.typeVariables[1]
    assertThat(rType.name).isEqualTo("R")
    assertThat(rType.isReified).isFalse()
    assertThat(rType.variance).isEqualTo(KModifier.IN)
    val vType = typeSpec.typeVariables[2]
    assertThat(vType.name).isEqualTo("V")
    assertThat(vType.isReified).isFalse()
    assertThat(vType.variance).isNull() // invariance is routed to null

    assertThat(typeSpec.propertySpecs).hasSize(1)
    assertThat(typeSpec.primaryConstructor?.parameters).hasSize(1)

    val param = typeSpec.primaryConstructor!!.parameters[0]
    val property = typeSpec.propertySpecs[0]

    assertThat(param.type).isEqualTo(tType)
    assertThat(property.type).isEqualTo(tType)
  }

  class Generics<out T, in R, V>(val genericInput: T)

  @Test
  fun typeAliases() {
    val typeSpec = TypeAliases::class.toTypeSpec()

    assertThat(typeSpec.primaryConstructor?.parameters).hasSize(2)

    val (param1, param2) = typeSpec.primaryConstructor!!.parameters
    // We always resolve the underlying type of typealiases
    assertThat(param1.type).isEqualTo(String::class.asClassName())
    assertThat(param2.type).isEqualTo(List::class.parameterizedBy(String::class))
  }

  class TypeAliases(val foo: TypeAliasName, val bar: GenericTypeAlias)

  @Test
  fun propertyMutability() {
    val typeSpec = PropertyMutability::class.toTypeSpec()

    assertThat(typeSpec.primaryConstructor?.parameters).hasSize(2)

    val fooProp = typeSpec.propertySpecs.find { it.name == "foo" } ?: throw AssertionError(
        "foo property not found!")
    val mutableFooProp = typeSpec.propertySpecs.find { it.name == "mutableFoo" }
        ?: throw AssertionError("mutableFoo property not found!")
    assertThat(fooProp.mutable).isFalse()
    assertThat(mutableFooProp.mutable).isTrue()
  }

  class PropertyMutability(val foo: String, var mutableFoo: String)

  @Test
  fun collectionMutability() {
    val typeSpec = CollectionMutability::class.toTypeSpec()

    assertThat(typeSpec.primaryConstructor?.parameters).hasSize(2)

    val (immutableProp, mutableListProp) = typeSpec.primaryConstructor!!.parameters
    assertThat(immutableProp.type).isEqualTo(List::class.parameterizedBy(String::class))
    assertThat(mutableListProp.type).isEqualTo(
        ClassName.bestGuess("kotlin.collections.MutableList").parameterizedBy(
            String::class.asTypeName()))
  }

  class CollectionMutability(val immutableList: List<String>, val mutableList: MutableList<String>)

  @Test
  fun suspendTypes() {
    val typeSpec = SuspendTypes::class.toTypeSpec()
    //language=kotlin
    assertThat(typeSpec.toString().trim()).isEqualTo("""
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
    assertThat(typeSpec.toString().trim()).isEqualTo("""
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
    assertThat(typeSpec.toString().trim()).isEqualTo("""
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
    assertThat(typeSpec.toString().trim()).isEqualTo("""
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
    assertThat(typeSpec.toString().trim()).isEqualTo("""
      inline class InlineClass(
        val value: kotlin.String
      )
    """.trimIndent())
  }

  // TODO Functions referencing class type parameter
  // TODO Overridden properties and functions
  // TODO Delegation (class, properties, local vars)
  // TODO Enums (simple and complex)
  // TODO Complex companion objects (implementing interfaces)
  // TODO Tagged km types
  // TODO Backward referencing type arguments (T, B<T>)
  // TODO Excluding delegated, only including declared
}

inline class InlineClass(val value: String)

typealias TypeAliasName = String
typealias GenericTypeAlias = List<String>
typealias NestedTypeAlias = List<GenericTypeAlias>
