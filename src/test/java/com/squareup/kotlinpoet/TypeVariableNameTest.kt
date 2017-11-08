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
import java.io.Serializable

class TypeVariableNameTest {
  @Test fun oneTypeVariableNoBounds() {
    val funSpec = FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T"))
        .returns(TypeVariableName("T").asNullable())
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |fun <T> foo(): T? = null
      |""".trimMargin())
  }

  @Test fun twoTypeVariablesNoBounds() {
    val funSpec = FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T"))
        .addTypeVariable(TypeVariableName("U"))
        .returns(TypeVariableName("T").asNullable())
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |fun <T, U> foo(): T? = null
      |""".trimMargin())
  }

  @Test fun oneTypeVariableOneBound() {
    val funSpec = FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T", Serializable::class))
        .returns(TypeVariableName("T").asNullable())
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |fun <T : java.io.Serializable> foo(): T? = null
      |""".trimMargin())
  }

  @Test fun twoTypeVariablesOneBoundEach() {
    val funSpec = FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T", Serializable::class))
        .addTypeVariable(TypeVariableName("U", Runnable::class))
        .returns(TypeVariableName("T").asNullable())
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |fun <T : java.io.Serializable, U : java.lang.Runnable> foo(): T? = null
      |""".trimMargin())
  }

  @Test fun oneTypeVariableTwoBounds() {
    val funSpec = FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T", Serializable::class, Runnable::class))
        .returns(TypeVariableName("T").asNullable())
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |fun <T> foo(): T? where T : java.io.Serializable, T : java.lang.Runnable = null
      |""".trimMargin())
  }

  @Test fun twoTypeVariablesTwoBoundsEach() {
    val funSpec = FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T", Serializable::class, Runnable::class))
        .addTypeVariable(TypeVariableName("U", Comparator::class, Cloneable::class))
        .returns(TypeVariableName("T").asNullable())
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |fun <T, U> foo(): T? where T : java.io.Serializable, T : java.lang.Runnable, U : java.util.Comparator, U : kotlin.Cloneable = null
      |""".trimMargin())
  }

  @Test fun threeTypeVariables() {
    val funSpec = FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T", Serializable::class, Runnable::class))
        .addTypeVariable(TypeVariableName("U", Cloneable::class))
        .addTypeVariable(TypeVariableName("V"))
        .returns(TypeVariableName("T").asNullable())
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |fun <T, U : kotlin.Cloneable, V> foo(): T? where T : java.io.Serializable, T : java.lang.Runnable = null
      |""".trimMargin())
  }

  @Test fun inVariance() {
    val typeSpec = TypeSpec.classBuilder("Taco")
        .addTypeVariable(TypeVariableName("E", Number::class, variance = KModifier.IN))
        .build()
    assertThat(typeSpec.toString()).isEqualTo("""
      |class Taco<in E : kotlin.Number>
      |""".trimMargin())
  }

  @Test fun outVariance() {
    val typeSpec = TypeSpec.classBuilder("Taco")
        .addTypeVariable(TypeVariableName("E", Number::class, variance = KModifier.OUT))
        .build()
    assertThat(typeSpec.toString()).isEqualTo("""
      |class Taco<out E : kotlin.Number>
      |""".trimMargin())
  }

  @Test fun invalidVariance() {
    assertThrows<IllegalArgumentException> {
      TypeVariableName("E", KModifier.FINAL)
    }
  }

  @Test fun reified() {
    val funSpec = FunSpec.builder("printMembers")
        .addModifiers(KModifier.INLINE)
        .addTypeVariable(TypeVariableName("T").reified())
        .addStatement("println(T::class.members)")
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |inline fun <reified T> printMembers() {
      |  println(T::class.members)
      |}
      |""".trimMargin())
  }
}
