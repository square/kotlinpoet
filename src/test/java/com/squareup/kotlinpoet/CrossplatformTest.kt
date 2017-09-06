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
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class CrossplatformTest {

  @Test fun crossplatform() {
    val headerTypeParam = TypeVariableName("V")
    val headerType = "AtomicRef"
    val headerSpec = TypeSpec.headerClassBuilder(headerType)
        .addTypeVariable(headerTypeParam)
        .addModifiers(KModifier.INTERNAL)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("value", headerTypeParam)
            .build())
        .addProperty(PropertySpec.builder("value", headerTypeParam).build())
        .addFunction(FunSpec.builder("get")
            .returns(headerTypeParam)
            .build())
        .addFunction(FunSpec.builder("set")
            .addParameter("value", headerTypeParam)
            .build())
        .addFunction(FunSpec.builder("getAndSet")
            .addParameter("value", headerTypeParam)
            .returns(headerTypeParam)
            .build())
        .addFunction(FunSpec.builder("compareAndSet")
            .addParameter("expect", headerTypeParam)
            .addParameter("update", headerTypeParam)
            .returns(Boolean::class)
            .build())
        .build()
    val implName = ParameterizedTypeName.get(AtomicReference::class.asTypeName(), headerTypeParam)
    val implSpec = TypeAliasSpec.builder(headerType, implName)
        .addTypeVariable(headerTypeParam)
        .addModifiers(KModifier.IMPL)
        .build()
    val fileSpec = FileSpec.builder("", "Test")
        .addType(headerSpec)
        .addTypeAlias(implSpec)
        .build()

    assertThat(fileSpec.toString()).isEqualTo("""
      |import java.util.concurrent.atomic.AtomicReference
      |import kotlin.Boolean
      |
      |header internal class AtomicRef<V>(value: V) {
      |  val value: V
      |
      |  fun get(): V
      |
      |  fun set(value: V)
      |
      |  fun getAndSet(value: V): V
      |
      |  fun compareAndSet(expect: V, update: V): Boolean
      |}
      |
      |impl typealias AtomicRef<V> = AtomicReference<V>
      |""".trimMargin())
  }

  @Test fun headerWithSecondaryConstructors() {
    val headerSpec = TypeSpec.headerClassBuilder("IoException")
        .addModifiers(KModifier.OPEN)
        .superclass(Exception::class)
        .addFunction(FunSpec.constructorBuilder().build())
        .addFunction(FunSpec.constructorBuilder()
            .addParameter("message", String::class)
            .build())
        .build()
    val fileSpec = FileSpec.builder("", "Test")
        .addType(headerSpec)
        .build()

    assertThat(fileSpec.toString()).isEqualTo("""
      |import java.lang.Exception
      |import kotlin.String
      |
      |header open class IoException : Exception {
      |  constructor()
      |
      |  constructor(message: String)
      |}
      |""".trimMargin())
  }

  @Test fun initBlockInHeaderForbidden() {
    assertThrows<IllegalStateException> {
      TypeSpec.headerClassBuilder("AtomicRef")
          .addInitializerBlock(CodeBlock.of("println()"))
    }.hasMessage("HEADER_CLASS can't have initializer blocks")
  }

  @Test fun headerFunctionBodyForbidden() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.headerClassBuilder("AtomicRef")
          .addFunction(FunSpec.builder("print")
              .addStatement("println()")
              .build())
    }.hasMessage("functions in header classes can't have bodies")
  }

  @Test fun headerPropertyInitializerForbidden() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.headerClassBuilder("AtomicRef")
          .addProperty(PropertySpec.builder("a", Boolean::class)
              .initializer("true")
              .build())
    }.hasMessage("properties in header classes can't have initializers")
  }

  @Test fun headerPropertyGetterForbidden() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.headerClassBuilder("AtomicRef")
          .addProperty(PropertySpec.builder("a", Boolean::class)
              .getter(FunSpec.getterBuilder()
                  .addStatement("return true")
                  .build())
              .build())
    }.hasMessage("properties in header classes can't have getters and setters")
  }

  @Test fun headerPropertySetterForbidden() {
    assertThrows<IllegalArgumentException> {
      TypeSpec.headerClassBuilder("AtomicRef")
          .addProperty(PropertySpec.builder("a", Boolean::class)
              .setter(FunSpec.setterBuilder()
                  .addParameter("value", Boolean::class)
                  .addStatement("field = true")
                  .build())
              .build())
    }.hasMessage("properties in header classes can't have getters and setters")
  }
}
