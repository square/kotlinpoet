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
package com.squareup.kotlinpoet

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ExternalDeclarationsTest {
  @Test
  fun externalFunDeclarationWithoutBody() {
    val methodSpec = FunSpec.builder("function").addModifiers(KModifier.EXTERNAL).build()

    assertThat(methodSpec.toString())
      .isEqualTo(
        """
        |public external fun function()
        |"""
          .trimMargin()
      )
  }

  @Test
  fun externalFunDeclarationWithDefinedExternally() {
    val methodSpec =
      FunSpec.builder("function")
        .addModifiers(KModifier.EXTERNAL)
        .returns(STRING)
        .addCode("return kotlin.js.definedExternally")
        .build()

    assertThat(methodSpec.toString())
      .isEqualTo(
        """
        |public external fun function(): kotlin.String = kotlin.js.definedExternally
        |"""
          .trimMargin()
      )
  }

  @Test
  fun externalFunDeclarationWithDefinedExternally2() {
    val methodSpec =
      FunSpec.builder("function")
        .addModifiers(KModifier.EXTERNAL)
        .addCode("kotlin.js.definedExternally")
        .build()

    assertThat(methodSpec.toString())
      .isEqualTo(
        """
        |public external fun function() {
        |  kotlin.js.definedExternally
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun implicitExternalFunDeclarationWithoutBody() {
    val builder = TypeSpec.classBuilder("Test").addModifiers(KModifier.EXTERNAL)
    val methodSpec = FunSpec.builder("function").build()
    builder.addFunction(methodSpec)

    assertThat(builder.build().toString())
      .isEqualTo(
        """
        |public external class Test {
        |  public fun function()
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun implicitExternalFunDeclarationWithDefinedExternally() {
    val builder = TypeSpec.classBuilder("Test").addModifiers(KModifier.EXTERNAL)
    val methodSpec =
      FunSpec.builder("function")
        .returns(STRING)
        .addCode("return kotlin.js.definedExternally")
        .build()
    builder.addFunction(methodSpec)

    assertThat(builder.build().toString())
      .isEqualTo(
        """
        |public external class Test {
        |  public fun function(): kotlin.String = kotlin.js.definedExternally
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun implicitExternalFunDeclarationWithDefinedExternally2() {
    val builder = TypeSpec.classBuilder("Test").addModifiers(KModifier.EXTERNAL)
    val methodSpec =
      FunSpec.builder("function")
        .addModifiers(KModifier.EXTERNAL)
        .addCode("kotlin.js.definedExternally")
        .build()
    builder.addFunction(methodSpec)

    assertThat(builder.build().toString())
      .isEqualTo(
        """
        |public external class Test {
        |  public fun function() {
        |    kotlin.js.definedExternally
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun externalPropertyDeclarationWithoutInitializer() {
    val propertySpec =
      PropertySpec.builder("prop", String::class).addModifiers(KModifier.EXTERNAL).build()

    assertThat(propertySpec.toString())
      .isEqualTo(
        """
        |external val prop: kotlin.String
        |"""
          .trimMargin()
      )
  }

  @Test
  fun externalPropertyDeclarationWithDefinedExternally() {
    val propertySpec =
      PropertySpec.builder("prop", String::class)
        .addModifiers(KModifier.EXTERNAL)
        .initializer("kotlin.js.definedExternally")
        .build()

    assertThat(propertySpec.toString())
      .isEqualTo(
        """
        |external val prop: kotlin.String = kotlin.js.definedExternally
        |"""
          .trimMargin()
      )
  }

  @Test
  fun implicitExternalPropertyDeclarationWithoutInitializer() {
    val builder = TypeSpec.classBuilder("Test").addModifiers(KModifier.EXTERNAL)
    val propertySpec = PropertySpec.builder("prop", String::class).build()
    builder.addProperty(propertySpec)

    assertThat(builder.build().toString())
      .isEqualTo(
        """
        |public external class Test {
        |  public val prop: kotlin.String
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun implicitExternalPropertyDeclarationWithDefinedExternally() {
    val builder = TypeSpec.classBuilder("Test").addModifiers(KModifier.EXTERNAL)
    val propertySpec =
      PropertySpec.builder("prop", String::class).initializer("kotlin.js.definedExternally").build()
    builder.addProperty(propertySpec)

    assertThat(builder.build().toString())
      .isEqualTo(
        """
        |public external class Test {
        |  public val prop: kotlin.String = kotlin.js.definedExternally
        |}
        |"""
          .trimMargin()
      )
  }
}
