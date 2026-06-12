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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.squareup.kotlinpoet.KModifier.INLINE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.jvm.jvmInline
import org.junit.Test

class ValueTypeSpecTest {

  private fun classBuilder() = TypeSpec.classBuilder("Guacamole").addModifiers(KModifier.VALUE)

  @Test
  fun validValueClass() {
    val guacamole =
      classBuilder()
        .primaryConstructor(
          FunSpec.constructorBuilder().addParameter("avacado", String::class).build()
        )
        .addProperty(PropertySpec.builder("avacado", String::class).initializer("avacado").build())
        .build()

    assertThat(guacamole.toString())
      .isEqualTo(
        """
        |public value class Guacamole(
        |  public val avacado: kotlin.String,
        |)
        |"""
          .trimMargin()
      )
  }

  @Test
  fun valueClassWithInitBlock() {
    val guacamole =
      classBuilder()
        .primaryConstructor(
          FunSpec.constructorBuilder().addParameter("avacado", String::class).build()
        )
        .addProperty(PropertySpec.builder("avacado", String::class).initializer("avacado").build())
        .addInitializerBlock(CodeBlock.EMPTY)
        .build()

    assertThat(guacamole.toString())
      .isEqualTo(
        """
        |public value class Guacamole(
        |  public val avacado: kotlin.String,
        |) {
        |  init {
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  class ValueSuperClass

  @Test
  fun valueClassWithSuperClass() {
    assertFailure {
        classBuilder()
          .primaryConstructor(
            FunSpec.constructorBuilder().addParameter("avocado", String::class).build()
          )
          .addProperty(
            PropertySpec.builder("avocado", String::class).initializer("avocado").build()
          )
          .superclass(ValueSuperClass::class)
          .build()
      }
      .isInstanceOf<IllegalStateException>()
      .hasMessage("value classes cannot have super classes")
  }

  interface ValueSuperInterface

  @Test
  fun valueClassInheritsFromInterface() {
    val guacamole =
      classBuilder()
        .primaryConstructor(
          FunSpec.constructorBuilder().addParameter("avocado", String::class).build()
        )
        .addProperty(PropertySpec.builder("avocado", String::class).initializer("avocado").build())
        .addSuperinterface(ValueSuperInterface::class)
        .build()

    assertThat(guacamole.toString())
      .isEqualTo(
        """
        |public value class Guacamole(
        |  public val avocado: kotlin.String,
        |) : com.squareup.kotlinpoet.ValueTypeSpecTest.ValueSuperInterface
        |"""
          .trimMargin()
      )
  }

  @Test
  fun valueClassWithoutBackingProperty() {
    assertFailure {
        classBuilder()
          .primaryConstructor(
            FunSpec.constructorBuilder().addParameter("avocado", String::class).build()
          )
          .addProperty("garlic", String::class)
          .build()
      }
      .isInstanceOf<IllegalStateException>()
      .hasMessage("value classes must only have final read-only (val) property parameters")
  }

  @Test
  fun valueClassWithoutProperties() {
    assertFailure {
        classBuilder()
          .primaryConstructor(
            FunSpec.constructorBuilder().addParameter("avocado", String::class).build()
          )
          .build()
      }
      .isInstanceOf<IllegalStateException>()
      .hasMessage("value classes must have at least 1 property")
  }

  @Test
  fun valueClassWithMutableProperties() {
    assertFailure {
        classBuilder()
          .primaryConstructor(
            FunSpec.constructorBuilder().addParameter("avocado", String::class).build()
          )
          .addProperty(
            PropertySpec.builder("avocado", String::class).initializer("avocado").mutable().build()
          )
          .build()
      }
      .isInstanceOf<IllegalStateException>()
      .hasMessage("value classes must only have final read-only (val) property parameters")
  }

  @Test
  fun valueClassWithPrivateConstructor() {
    val guacamole =
      classBuilder()
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameter("avocado", String::class)
            .addModifiers(PRIVATE)
            .build()
        )
        .addProperty(PropertySpec.builder("avocado", String::class).initializer("avocado").build())
        .build()

    assertThat(guacamole.toString())
      .isEqualTo(
        """
        |public value class Guacamole private constructor(
        |  public val avocado: kotlin.String,
        |)
        |"""
          .trimMargin()
      )
  }

  @Test
  fun valueEnumClass() {
    val guacamole =
      TypeSpec.enumBuilder("Foo")
        .addModifiers(KModifier.VALUE)
        .primaryConstructor(FunSpec.constructorBuilder().addParameter("x", Int::class).build())
        .addEnumConstant(
          "A",
          TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%L", 1).build(),
        )
        .addEnumConstant(
          "B",
          TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%L", 2).build(),
        )
        .addProperty(PropertySpec.builder("x", Int::class).initializer("x").build())
        .build()
    assertThat(guacamole.toString())
      .isEqualTo(
        """
        |public enum value class Foo(
        |  public val x: kotlin.Int,
        |) {
        |  A(1),
        |  B(2),
        |  ;
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun inlineForbiddenOnClass() {
    assertFailure {
        TypeSpec.classBuilder("Guacamole")
          .addModifiers(INLINE)
          .primaryConstructor(
            FunSpec.constructorBuilder().addParameter("avocado", String::class).build()
          )
          .addProperty(
            PropertySpec.builder("avocado", String::class).initializer("avocado").build()
          )
          .build()
      }
      .isInstanceOf<IllegalArgumentException>()
      .hasMessage(
        "KotlinPoet doesn't allow setting the inline modifier on " +
          "classes. You should use the value modifier instead."
      )
  }

  @Test
  fun inlineForbiddenOnEnum() {
    assertFailure {
        TypeSpec.enumBuilder("Foo")
          .addModifiers(INLINE)
          .primaryConstructor(FunSpec.constructorBuilder().addParameter("x", Int::class).build())
          .addProperty(PropertySpec.builder("x", Int::class).initializer("x").build())
          .build()
      }
      .isInstanceOf<IllegalArgumentException>()
      .hasMessage(
        "KotlinPoet doesn't allow setting the inline modifier on " +
          "classes. You should use the value modifier instead."
      )
  }

  @Test
  fun multiFieldValueClass() {
    val color =
      TypeSpec.classBuilder("Color")
        .jvmInline()
        .addModifiers(KModifier.VALUE)
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameter("alpha", U_BYTE)
            .addParameter("red", U_BYTE)
            .addParameter("green", U_BYTE)
            .addParameter("blue", U_BYTE)
            .build()
        )
        .addProperty(PropertySpec.builder("alpha", U_BYTE).initializer("alpha").build())
        .addProperty(PropertySpec.builder("red", U_BYTE).initializer("red").build())
        .addProperty(PropertySpec.builder("green", U_BYTE).initializer("green").build())
        .addProperty(PropertySpec.builder("blue", U_BYTE).initializer("blue").build())
        .build()
    assertThat(color.toString())
      .isEqualTo(
        """
        @kotlin.jvm.JvmInline
        public value class Color(
          public val alpha: kotlin.UByte,
          public val red: kotlin.UByte,
          public val green: kotlin.UByte,
          public val blue: kotlin.UByte,
        )

        """
          .trimIndent()
      )
  }
}
