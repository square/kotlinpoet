/*
 * Copyright (C) 2017 Square, Inc.
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
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeVariableName.Companion.NULLABLE_ANY_LIST
import java.io.Serializable
import kotlin.test.Test

class TypeVariableNameTest {
  @Test
  fun nullableAnyIsImplicitBound() {
    val typeVariableName = TypeVariableName("T")
    assertThat(typeVariableName.bounds).containsExactly(NULLABLE_ANY)
  }

  @Test
  fun oneTypeVariableNoBounds() {
    val funSpec =
      FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T"))
        .returns(TypeVariableName("T").copy(nullable = true))
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString())
      .isEqualTo(
        """
        |public fun <T> foo(): T? = null
        |"""
          .trimMargin()
      )
  }

  @Test
  fun twoTypeVariablesNoBounds() {
    val funSpec =
      FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T"))
        .addTypeVariable(TypeVariableName("U"))
        .returns(TypeVariableName("T").copy(nullable = true))
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString())
      .isEqualTo(
        """
        |public fun <T, U> foo(): T? = null
        |"""
          .trimMargin()
      )
  }

  @Test
  fun oneTypeVariableOneBound() {
    val funSpec =
      FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T", Serializable::class))
        .returns(TypeVariableName("T").copy(nullable = true))
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString())
      .isEqualTo(
        """
        |public fun <T : java.io.Serializable> foo(): T? = null
        |"""
          .trimMargin()
      )
  }

  @Test
  fun twoTypeVariablesOneBoundEach() {
    val funSpec =
      FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T", Serializable::class))
        .addTypeVariable(TypeVariableName("U", Runnable::class))
        .returns(TypeVariableName("T").copy(nullable = true))
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString())
      .isEqualTo(
        """
        |public fun <T : java.io.Serializable, U : java.lang.Runnable> foo(): T? = null
        |"""
          .trimMargin()
      )
  }

  @Test
  fun oneTypeVariableTwoBounds() {
    val funSpec =
      FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T", Serializable::class, Runnable::class))
        .returns(TypeVariableName("T").copy(nullable = true))
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString())
      .isEqualTo(
        """
        |public fun <T> foo(): T? where T : java.io.Serializable, T : java.lang.Runnable = null
        |"""
          .trimMargin()
      )
  }

  @Test
  fun twoTypeVariablesTwoBoundsEach() {
    val funSpec =
      FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T", Serializable::class, Runnable::class))
        .addTypeVariable(TypeVariableName("U", Comparator::class, Cloneable::class))
        .returns(TypeVariableName("T").copy(nullable = true))
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString())
      .isEqualTo(
        "public fun <T, U> foo(): " +
          "T? where T : java.io.Serializable, T : java.lang.Runnable, " +
          "U : java.util.Comparator, U : kotlin.Cloneable = null\n"
      )
  }

  @Test
  fun threeTypeVariables() {
    val funSpec =
      FunSpec.builder("foo")
        .addTypeVariable(TypeVariableName("T", Serializable::class, Runnable::class))
        .addTypeVariable(TypeVariableName("U", Cloneable::class))
        .addTypeVariable(TypeVariableName("V"))
        .returns(TypeVariableName("T").copy(nullable = true))
        .addStatement("return null")
        .build()
    assertThat(funSpec.toString())
      .isEqualTo(
        "public fun <T, U : kotlin.Cloneable, V> foo(): " +
          "T? where T : java.io.Serializable, T : java.lang.Runnable = null\n"
      )
  }

  @Test
  fun addingBoundsRemovesImplicitBound() {
    val typeSpec =
      TypeSpec.classBuilder("Taco")
        .addTypeVariable(TypeVariableName("T").copy(bounds = listOf(Number::class.asTypeName())))
        .build()
    assertThat(typeSpec.toString())
      .isEqualTo(
        """
        |public class Taco<T : kotlin.Number>
        |"""
          .trimMargin()
      )
  }

  @Test
  fun inVariance() {
    val typeSpec =
      TypeSpec.classBuilder("Taco")
        .addTypeVariable(TypeVariableName("E", Number::class, variance = KModifier.IN))
        .build()
    assertThat(typeSpec.toString())
      .isEqualTo(
        """
        |public class Taco<in E : kotlin.Number>
        |"""
          .trimMargin()
      )
  }

  @Test
  fun outVariance() {
    val typeSpec =
      TypeSpec.classBuilder("Taco")
        .addTypeVariable(TypeVariableName("E", Number::class, variance = KModifier.OUT))
        .build()
    assertThat(typeSpec.toString())
      .isEqualTo(
        """
        |public class Taco<out E : kotlin.Number>
        |"""
          .trimMargin()
      )
  }

  @Test
  fun invalidVariance() {
    assertFailure { TypeVariableName("E", KModifier.FINAL) }
      .isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun reified() {
    val funSpec =
      FunSpec.builder("printMembers")
        .addModifiers(KModifier.INLINE)
        .addTypeVariable(TypeVariableName("T").copy(reified = true))
        .addStatement("println(T::class.members)")
        .build()
    assertThat(funSpec.toString())
      .isEqualTo(
        """
        |public inline fun <reified T> printMembers() {
        |  println(T::class.members)
        |}
        |"""
          .trimMargin()
      )
  }

  @Test
  fun anyBoundsIsLegal() {
    val typeSpec = TypeSpec.classBuilder("Taco").addTypeVariable(TypeVariableName("E", ANY)).build()
    assertThat(typeSpec.toString())
      .isEqualTo(
        """
        |public class Taco<E : kotlin.Any>
        |"""
          .trimMargin()
      )
  }

  @Test
  fun filterOutNullableAnyBounds() {
    val typeSpec =
      TypeSpec.classBuilder("Taco").addTypeVariable(TypeVariableName("E", NULLABLE_ANY)).build()
    assertThat(typeSpec.toString())
      .isEqualTo(
        """
        |public class Taco<E>
        |"""
          .trimMargin()
      )
  }

  @Test
  fun emptyBoundsShouldDefaultToAnyNullable() {
    val typeVariable = TypeVariableName("E", bounds = emptyArray<TypeName>())
    val typeSpec = TypeSpec.classBuilder("Taco").addTypeVariable(typeVariable).build()
    assertThat(typeVariable.bounds).isEqualTo(NULLABLE_ANY_LIST)
    assertThat(typeSpec.toString())
      .isEqualTo(
        """
        |public class Taco<E>
        |"""
          .trimMargin()
      )
  }

  @Test
  fun noBoundsShouldDefaultToAnyNullable() {
    val typeVariable = TypeVariableName("E")
    val typeSpec = TypeSpec.classBuilder("Taco").addTypeVariable(typeVariable).build()
    assertThat(typeVariable.bounds).isEqualTo(NULLABLE_ANY_LIST)
    assertThat(typeSpec.toString())
      .isEqualTo(
        """
        |public class Taco<E>
        |"""
          .trimMargin()
      )
  }

  @Test
  fun genericClassNoBoundsShouldDefaultToAnyNullable() {
    val typeVariable = TypeVariableName.get(GenericClass::class.java.typeParameters[0])
    val typeSpec = TypeSpec.classBuilder("Taco").addTypeVariable(typeVariable).build()
    assertThat(typeVariable.bounds).isEqualTo(NULLABLE_ANY_LIST)
    assertThat(typeSpec.toString())
      .isEqualTo(
        """
        |public class Taco<T>
        |"""
          .trimMargin()
      )
  }

  class GenericClass<T>

  @Test
  fun equalsAndHashCode() {
    val typeVariableName1 = TypeVariableName("E", listOf(Number::class.asTypeName()), KModifier.IN)

    val typeVariableName2 = TypeVariableName("E", listOf(Number::class.asTypeName()), KModifier.IN)
    assertThat(typeVariableName1).isEqualTo(typeVariableName2)
    assertThat(typeVariableName1.hashCode()).isEqualTo(typeVariableName2.hashCode())
    assertThat(typeVariableName1.toString()).isEqualTo(typeVariableName2.toString())

    assertThat(typeVariableName1.copy(nullable = true)).isNotEqualTo(typeVariableName1)

    assertThat(
        typeVariableName1.copy(
          annotations = listOf(AnnotationSpec.builder(Suppress::class.asTypeName()).build())
        )
      )
      .isNotEqualTo(typeVariableName1)

    assertThat(typeVariableName1.copy(bounds = listOf(Runnable::class.asTypeName())))
      .isNotEqualTo(typeVariableName1)

    assertThat(typeVariableName1.copy(reified = true)).isNotEqualTo(typeVariableName1)
  }

  @Test
  fun equalsAndHashCodeIgnoreTags() {
    val typeVariableName = TypeVariableName("E", listOf(Number::class.asTypeName()), KModifier.IN)
    val tagged = typeVariableName.copy(tags = mapOf(String::class to "test"))

    assertThat(typeVariableName).isEqualTo(tagged)
    assertThat(typeVariableName.hashCode()).isEqualTo(tagged.hashCode())
  }

  @Test
  fun recursivelyBoundedTypeVariableEqualsAndHashCode() {
    // `Enum<E : Enum<E>>` produces a type variable whose bounds refer back to itself.
    val typeParameter = Enum::class.java.typeParameters[0]
    val first = TypeVariableName.get(typeParameter)
    val second = TypeVariableName.get(typeParameter)

    assertThat(first.hashCode()).isEqualTo(second.hashCode())
    assertThat(first).isEqualTo(second)
    assertThat(second).isEqualTo(first)
    assertThat(first).isNotEqualTo(TypeVariableName("E"))
  }

  @Test
  fun equalsComparesNestedBounds() {
    fun boundedBy(argument: TypeName) =
      TypeVariableName("T")
        .copy(bounds = listOf(LIST.parameterizedBy(WildcardTypeName.producerOf(argument))))

    assertThat(boundedBy(Number::class.asTypeName()))
      .isEqualTo(boundedBy(Number::class.asTypeName()))
    assertThat(boundedBy(Number::class.asTypeName()))
      .isNotEqualTo(boundedBy(String::class.asTypeName()))
  }

  @Test
  fun equalsComparesEnclosingTypeOfBound() {
    fun boundedByInnerOf(outerArgument: TypeName) =
      TypeVariableName("T")
        .copy(
          bounds =
            listOf(
              ClassName("com.example", "Outer")
                .parameterizedBy(outerArgument)
                .nestedClass("Inner", listOf(STRING))
            )
        )

    assertThat(boundedByInnerOf(INT)).isEqualTo(boundedByInnerOf(INT))
    assertThat(boundedByInnerOf(INT)).isNotEqualTo(boundedByInnerOf(STRING))
  }
}
