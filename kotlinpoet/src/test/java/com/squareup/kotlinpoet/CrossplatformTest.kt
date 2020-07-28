/*
 * Copyright (C) 2017 Square, Inc.
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
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test

class CrossplatformTest {

  @Test fun crossplatform() {
    val expectTypeParam = TypeVariableName("V")
    val expectType = "AtomicRef"
    val expectSpec = TypeSpec.expectClassBuilder(expectType)
        .addTypeVariable(expectTypeParam)
        .addModifiers(KModifier.INTERNAL)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("value", expectTypeParam)
            .build())
        .addProperty(PropertySpec.builder("value", expectTypeParam).build())
        .addFunction(FunSpec.builder("get")
            .returns(expectTypeParam)
            .build())
        .addFunction(FunSpec.builder("set")
            .addParameter("value", expectTypeParam)
            .build())
        .addFunction(FunSpec.builder("getAndSet")
            .addParameter("value", expectTypeParam)
            .returns(expectTypeParam)
            .build())
        .addFunction(FunSpec.builder("compareAndSet")
            .addParameter("expect", expectTypeParam)
            .addParameter("update", expectTypeParam)
            .returns(Boolean::class)
            .build())
        .build()
    val actualName = AtomicReference::class.asTypeName().parameterizedBy(expectTypeParam)
    val actualSpec = TypeAliasSpec.builder(expectType, actualName)
        .addTypeVariable(expectTypeParam)
        .addModifiers(KModifier.ACTUAL)
        .build()
    val fileSpec = FileSpec.builder("", "Test")
        .addType(expectSpec)
        .addTypeAlias(actualSpec)
        .build()

    assertThat(fileSpec.toString()).isEqualTo("""
      |import java.util.concurrent.atomic.AtomicReference
      |import kotlin.Boolean
      |import kotlin.Unit
      |
      |internal expect class AtomicRef<V>(
      |  value: V
      |) {
      |  public val value: V
      |
      |  public fun get(): V
      |
      |  public fun set(value: V): Unit
      |
      |  public fun getAndSet(value: V): V
      |
      |  public fun compareAndSet(expect: V, update: V): Boolean
      |}
      |
      |public actual typealias AtomicRef<V> = AtomicReference<V>
      |""".trimMargin())
  }

  @Test fun expectWithSecondaryConstructors() {
    val expectSpec = TypeSpec.expectClassBuilder("IoException")
        .addModifiers(KModifier.OPEN)
        .superclass(Exception::class)
        .addFunction(FunSpec.constructorBuilder().build())
        .addFunction(FunSpec.constructorBuilder()
            .addParameter("message", String::class)
            .build())
        .build()
    val fileSpec = FileSpec.builder("", "Test")
        .addType(expectSpec)
        .build()

    assertThat(fileSpec.toString()).isEqualTo("""
      |import java.lang.Exception
      |import kotlin.String
      |
      |public expect open class IoException : Exception {
      |  public constructor()
      |
      |  public constructor(message: String)
      |}
      |""".trimMargin())
  }

  @Test fun topLevelProperties() {
    val fileSpec = FileSpec.builder("", "Test")
        .addProperty(PropertySpec.builder("bar", String::class, KModifier.EXPECT).build())
        .addProperty(PropertySpec.builder("bar", String::class, KModifier.ACTUAL)
            .initializer(CodeBlock.of("%S", "Hello"))
            .build())
        .build()

    assertThat(fileSpec.toString()).isEqualTo("""
      |import kotlin.String
      |
      |public expect val bar: String
      |
      |public actual val bar: String = "Hello"
      |""".trimMargin())
  }

  @Test fun topLevelFunctions() {
    val fileSpec = FileSpec.builder("", "Test")
        .addFunction(FunSpec.builder("f1")
            .addModifiers(KModifier.EXPECT)
            .returns(Int::class)
            .build())
        .addFunction(FunSpec.builder("f1")
            .addModifiers(KModifier.ACTUAL)
            .addStatement("return 1")
            .build())
        .build()

    assertThat(fileSpec.toString()).isEqualTo("""
      |import kotlin.Int
      |
      |public expect fun f1(): Int
      |
      |public actual fun f1() = 1
      |""".trimMargin())
  }

  @Test fun initBlockInExpectForbidden() {
    assertThrows<IllegalStateException> {
      TypeSpec.expectClassBuilder("AtomicRef")
          .addInitializerBlock(CodeBlock.of("println()"))
    }.hasMessageThat().isEqualTo("expect CLASS can't have initializer blocks")
  }

  @Test fun expectFunctionBodyForbidden() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.expectClassBuilder("AtomicRef")
          .addFunction(FunSpec.builder("print")
              .addStatement("println()")
              .build())
          .build()
    }.hasMessageThat().isEqualTo("functions in expect classes can't have bodies")
  }

  @Test fun expectPropertyInitializerForbidden() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.expectClassBuilder("AtomicRef")
          .addProperty(PropertySpec.builder("a", Boolean::class)
              .initializer("true")
              .build())
    }.hasMessageThat().isEqualTo("properties in expect classes can't have initializers")
  }

  @Test fun expectPropertyGetterForbidden() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.expectClassBuilder("AtomicRef")
          .addProperty(PropertySpec.builder("a", Boolean::class)
              .getter(FunSpec.getterBuilder()
                  .addStatement("return true")
                  .build())
              .build())
    }.hasMessageThat().isEqualTo("properties in expect classes can't have getters and setters")
  }

  @Test fun expectPropertySetterForbidden() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.expectClassBuilder("AtomicRef")
          .addProperty(PropertySpec.builder("a", Boolean::class)
              .mutable()
              .setter(FunSpec.setterBuilder()
                  .addParameter("value", Boolean::class)
                  .addStatement("field = true")
                  .build())
              .build())
    }.hasMessageThat().isEqualTo("properties in expect classes can't have getters and setters")
  }
}
