/*
 * Copyright (C) 2026 Square, Inc.
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
@file:OptIn(ExperimentalKotlinPoetApi::class)

package com.squareup.kotlinpoet

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.kotlinpoet.KModifier.EXPECT
import com.squareup.kotlinpoet.KModifier.OPERATOR
import com.squareup.kotlinpoet.KModifier.SUSPEND
import kotlin.test.Test

class CompanionBlockSpecTest {
  @Test
  fun companionBlockWithFunctionsAndProperties() {
    val vector =
      vectorTypeSpec()
        .addCompanionBlock(
          CompanionBlockSpec.Builder()
            .addProperty(
              PropertySpec.builder("Zero", VECTOR)
                .getter(FunSpec.getterBuilder().addStatement("return %T(0.0, 0.0)", VECTOR).build())
                .build()
            )
            .addFunction(
              FunSpec.builder("invoke")
                .addModifiers(SUSPEND, OPERATOR)
                .addParameter("file", ClassName(TACOS_PACKAGE, "StructureFormat"))
                .returns(VECTOR)
                .addStatement("val x = file.readDouble()")
                .addStatement("val y = file.readDouble()")
                .addStatement("return %T(x, y)", VECTOR)
                .build()
            )
            .build()
        )
        .build()
    assertThat(toString(vector))
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import kotlin.Double
        |
        |public data class Vector(
        |  public val x: Double,
        |  public val y: Double,
        |) {
        |  companion {
        |    public val Zero: Vector
        |      get() = Vector(0.0, 0.0)
        |
        |    public suspend operator fun invoke(`file`: StructureFormat): Vector {
        |      val x = file.readDouble()
        |      val y = file.readDouble()
        |      return Vector(x, y)
        |    }
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun multipleCompanionBlocks() {
    val vector =
      vectorTypeSpec()
        .addCompanionBlock(
          CompanionBlockSpec.Builder()
            .addProperty(
              PropertySpec.builder("Zero", VECTOR)
                .getter(FunSpec.getterBuilder().addStatement("return %T(0.0, 0.0)", VECTOR).build())
                .build()
            )
            .build()
        )
        .addCompanionBlock(
          CompanionBlockSpec.Builder()
            .addFunction(
              FunSpec.builder("invoke")
                .addModifiers(SUSPEND, OPERATOR)
                .addParameter("file", ClassName(TACOS_PACKAGE, "StructureFormat"))
                .returns(VECTOR)
                .addStatement("val x = file.readDouble()")
                .addStatement("val y = file.readDouble()")
                .addStatement("return %T(x, y)", VECTOR)
                .build()
            )
            .build()
        )
        .build()
    assertThat(toString(vector))
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import kotlin.Double
        |
        |public data class Vector(
        |  public val x: Double,
        |  public val y: Double,
        |) {
        |  companion {
        |    public val Zero: Vector
        |      get() = Vector(0.0, 0.0)
        |  }
        |
        |  companion {
        |    public suspend operator fun invoke(`file`: StructureFormat): Vector {
        |      val x = file.readDouble()
        |      val y = file.readDouble()
        |      return Vector(x, y)
        |    }
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun companionBlocksAndCompanionObject() {
    val vector =
      vectorTypeSpec()
        .addCompanionBlock(
          CompanionBlockSpec.Builder()
            .addProperty(
              PropertySpec.builder("Zero", VECTOR)
                .getter(FunSpec.getterBuilder().addStatement("return %T(0.0, 0.0)", VECTOR).build())
                .build()
            )
            .build()
        )
        .addType(
          TypeSpec.companionObjectBuilder()
            .addFunction(
              FunSpec.builder("invoke")
                .addModifiers(SUSPEND, OPERATOR)
                .addParameter("file", ClassName(TACOS_PACKAGE, "StructureFormat"))
                .returns(VECTOR)
                .addStatement("val x = file.readDouble()")
                .addStatement("val y = file.readDouble()")
                .addStatement("return %T(x, y)", VECTOR)
                .build()
            )
            .build()
        )
        .build()
    assertThat(toString(vector))
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import kotlin.Double
        |
        |public data class Vector(
        |  public val x: Double,
        |  public val y: Double,
        |) {
        |  public companion object {
        |    public suspend operator fun invoke(`file`: StructureFormat): Vector {
        |      val x = file.readDouble()
        |      val y = file.readDouble()
        |      return Vector(x, y)
        |    }
        |  }
        |
        |  companion {
        |    public val Zero: Vector
        |      get() = Vector(0.0, 0.0)
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun companionBlockWithExpectMembers() {
    val vector =
      vectorTypeSpec()
        .addCompanionBlock(
          CompanionBlockSpec.Builder()
            .addProperty(PropertySpec.builder("Zero", VECTOR, EXPECT).build())
            .addFunction(FunSpec.builder("invoke").addModifiers(EXPECT).build())
            .build()
        )
        .build()
    assertThat(toString(vector))
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import kotlin.Double
        |
        |public data class Vector(
        |  public val x: Double,
        |  public val y: Double,
        |) {
        |  companion {
        |    public expect val Zero: Vector
        |
        |    public expect fun invoke()
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun emptyCompanionBlock() {
    val vector = vectorTypeSpec().addCompanionBlock(CompanionBlockSpec.Builder().build()).build()
    assertThat(toString(vector))
      .isEqualTo(
        """
        |package com.squareup.tacos
        |
        |import kotlin.Double
        |
        |public data class Vector(
        |  public val x: Double,
        |  public val y: Double,
        |) {
        |  companion {
        |  }
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun builderEqualityTest() {
    val companionBlockSpec =
      CompanionBlockSpec.Builder()
        .addProperty("tacos", Int::class)
        .addFunction(FunSpec.builder("test").addModifiers(KModifier.PUBLIC).build())
        .build()
    assertThat(companionBlockSpec.toBuilder().build()).isEqualTo(companionBlockSpec)
  }

  companion object {
    private const val TACOS_PACKAGE = "com.squareup.tacos"
    private val VECTOR = ClassName(TACOS_PACKAGE, "Vector")

    private fun vectorTypeSpec(): TypeSpec.Builder {
      return TypeSpec.classBuilder("Vector")
        .addModifiers(KModifier.DATA)
        .primaryConstructor(
          FunSpec.constructorBuilder().addParameter("x", DOUBLE).addParameter("y", DOUBLE).build()
        )
        .addProperty(PropertySpec.builder("x", DOUBLE).initializer("x").build())
        .addProperty(PropertySpec.builder("y", DOUBLE).initializer("y").build())
    }

    private fun toString(typeSpec: TypeSpec): String {
      return FileSpec.get(TACOS_PACKAGE, typeSpec).toString()
    }
  }
}
