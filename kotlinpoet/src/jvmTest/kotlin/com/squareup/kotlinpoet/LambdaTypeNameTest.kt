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
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import com.squareup.kotlinpoet.KModifier.VARARG
import kotlin.test.Test

@OptIn(ExperimentalKotlinPoetApi::class)
class LambdaTypeNameTest {

  @Retention(AnnotationRetention.RUNTIME) annotation class HasSomeAnnotation

  @HasSomeAnnotation inner class IsAnnotated

  @Test
  fun receiverWithoutAnnotationHasNoParens() {
    val typeName =
      LambdaTypeName.get(
        receiver = Int::class.asClassName(),
        parameters = listOf(),
        returnType = Unit::class.asTypeName(),
      )
    assertThat(typeName.toString()).isEqualTo("kotlin.Int.() -> kotlin.Unit")
  }

  @Test
  fun receiverWithAnnotationHasParens() {
    val annotation = IsAnnotated::class.java.getAnnotation(HasSomeAnnotation::class.java)
    val typeName =
      LambdaTypeName.get(
        receiver =
          Int::class.asClassName()
            .copy(
              annotations = listOf(AnnotationSpec.get(annotation, includeDefaultValues = true))
            ),
        parameters = listOf(),
        returnType = Unit::class.asTypeName(),
      )
    assertThat(typeName.toString())
      .isEqualTo(
        "(@com.squareup.kotlinpoet.LambdaTypeNameTest.HasSomeAnnotation kotlin.Int).() -> kotlin.Unit"
      )
  }

  @Test
  fun contextReceiver() {
    val typeName =
      LambdaTypeName.get(
        receiver = Int::class.asTypeName(),
        parameters = listOf(),
        returnType = Unit::class.asTypeName(),
        contextReceivers = listOf(STRING),
      )
    assertThat(typeName.toString()).isEqualTo("context(kotlin.String) kotlin.Int.() -> kotlin.Unit")
  }

  @Test
  fun nullableFunctionWithContextReceiver() {
    val typeName =
      LambdaTypeName.get(
          receiver = Int::class.asTypeName(),
          parameters = listOf(),
          returnType = Unit::class.asTypeName(),
          contextReceivers = listOf(STRING),
        )
        .copy(nullable = true)
    assertThat(typeName.toString())
      .isEqualTo("(context(kotlin.String) kotlin.Int.() -> kotlin.Unit)?")
  }

  @Test
  fun suspendingFunctionWithContextReceiver() {
    val typeName =
      LambdaTypeName.get(
          receiver = Int::class.asTypeName(),
          parameters = listOf(),
          returnType = Unit::class.asTypeName(),
          contextReceivers = listOf(STRING),
        )
        .copy(suspending = true)
    assertThat(typeName.toString())
      .isEqualTo("suspend context(kotlin.String) kotlin.Int.() -> kotlin.Unit")
  }

  @Test
  fun functionWithMultipleContextReceivers() {
    val typeName =
      LambdaTypeName.get(
        Int::class.asTypeName(),
        listOf(),
        Unit::class.asTypeName(),
        listOf(STRING, BOOLEAN),
      )
    assertThat(typeName.toString())
      .isEqualTo("context(kotlin.String, kotlin.Boolean) kotlin.Int.() -> kotlin.Unit")
  }

  @Test
  fun functionWithGenericContextReceiver() {
    val genericType = TypeVariableName("T")
    val typeName =
      LambdaTypeName.get(
        Int::class.asTypeName(),
        listOf(),
        Unit::class.asTypeName(),
        listOf(genericType),
      )

    assertThat(typeName.toString()).isEqualTo("context(T) kotlin.Int.() -> kotlin.Unit")
  }

  @Test
  fun functionWithAnnotatedContextReceiver() {
    val annotatedType =
      STRING.copy(annotations = listOf(AnnotationSpec.get(FunSpecTest.TestAnnotation())))
    val typeName =
      LambdaTypeName.get(
        Int::class.asTypeName(),
        listOf(),
        Unit::class.asTypeName(),
        listOf(annotatedType),
      )

    assertThat(typeName.toString())
      .isEqualTo(
        "context(@com.squareup.kotlinpoet.FunSpecTest.TestAnnotation kotlin.String) kotlin.Int.() -> kotlin.Unit"
      )
  }

  @Test
  fun contextParameter() {
    val typeName =
      LambdaTypeName.get(
        receiver = Int::class.asTypeName(),
        parameters = listOf(),
        returnType = Unit::class.asTypeName(),
        contextParameters = listOf(STRING),
      )
    assertThat(typeName.toString()).isEqualTo("context(kotlin.String) kotlin.Int.() -> kotlin.Unit")
  }

  @Test
  fun nullableFunctionWithContextParameter() {
    val typeName =
      LambdaTypeName.get(
          receiver = Int::class.asTypeName(),
          parameters = listOf(),
          returnType = Unit::class.asTypeName(),
          contextParameters = listOf(STRING),
        )
        .copy(nullable = true)
    assertThat(typeName.toString())
      .isEqualTo("(context(kotlin.String) kotlin.Int.() -> kotlin.Unit)?")
  }

  @Test
  fun suspendingFunctionWithContextParameter() {
    val typeName =
      LambdaTypeName.get(
          receiver = Int::class.asTypeName(),
          parameters = listOf(),
          returnType = Unit::class.asTypeName(),
          contextParameters = listOf(STRING),
        )
        .copy(suspending = true)
    assertThat(typeName.toString())
      .isEqualTo("suspend context(kotlin.String) kotlin.Int.() -> kotlin.Unit")
  }

  @Test
  fun functionWithMultipleContextParameters() {
    val typeName =
      LambdaTypeName.get(
        Int::class.asTypeName(),
        listOf(),
        Unit::class.asTypeName(),
        contextParameters = listOf(STRING, INT),
      )
    assertThat(typeName.toString())
      .isEqualTo("context(kotlin.String, kotlin.Int) kotlin.Int.() -> kotlin.Unit")
  }

  @Test
  fun functionWithAnnotatedContextParameter() {
    val annotatedType =
      STRING.copy(annotations = listOf(AnnotationSpec.get(FunSpecTest.TestAnnotation())))
    val typeName =
      LambdaTypeName.get(
        Int::class.asTypeName(),
        listOf(),
        Unit::class.asTypeName(),
        contextParameters = listOf(annotatedType),
      )

    assertThat(typeName.toString())
      .isEqualTo(
        "context(@com.squareup.kotlinpoet.FunSpecTest.TestAnnotation kotlin.String) kotlin.Int.() -> kotlin.Unit"
      )
  }

  @Test
  fun functionWithContextReceiverAndContextParameter() {
    assertFailure {
        LambdaTypeName.get(
          Int::class.asTypeName(),
          listOf(),
          Unit::class.asTypeName(),
          contextReceivers = listOf(STRING),
          contextParameters = listOf(INT),
        )
      }
      .isInstanceOf<IllegalArgumentException>()
      .hasMessage("Using both context receivers and context parameters is not allowed")
  }

  @Test
  fun paramsWithAnnotationsForbidden() {
    assertFailure {
        LambdaTypeName.get(
          parameters =
            arrayOf(
              ParameterSpec.builder("foo", Int::class).addAnnotation(Deprecated::class).build()
            ),
          returnType = Unit::class.asTypeName(),
        )
      }
      .isInstanceOf<IllegalArgumentException>()
      .hasMessage("Parameters with annotations are not allowed")
  }

  @Test
  fun paramsWithModifiersForbidden() {
    assertFailure {
        LambdaTypeName.get(
          parameters =
            arrayOf(ParameterSpec.builder("foo", Int::class).addModifiers(VARARG).build()),
          returnType = Unit::class.asTypeName(),
        )
      }
      .isInstanceOf<IllegalArgumentException>()
      .hasMessage("Parameters with modifiers are not allowed")
  }

  @Test
  fun paramsWithDefaultValueForbidden() {
    assertFailure {
        LambdaTypeName.get(
          parameters = arrayOf(ParameterSpec.builder("foo", Int::class).defaultValue("42").build()),
          returnType = Unit::class.asTypeName(),
        )
      }
      .isInstanceOf<IllegalArgumentException>()
      .hasMessage("Parameters with default values are not allowed")
  }

  @Test
  fun lambdaReturnType() {
    val returnTypeName =
      LambdaTypeName.get(
        parameters = arrayOf(Int::class.asTypeName()),
        returnType = Unit::class.asTypeName(),
      )
    val typeName =
      LambdaTypeName.get(parameters = arrayOf(Int::class.asTypeName()), returnType = returnTypeName)
    assertThat(typeName.toString()).isEqualTo("(kotlin.Int) -> ((kotlin.Int) -> kotlin.Unit)")
  }

  @Test
  fun lambdaParameterType() {
    val parameterTypeName =
      LambdaTypeName.get(
        parameters = arrayOf(Int::class.asTypeName()),
        returnType = Int::class.asTypeName(),
      )
    val typeName =
      LambdaTypeName.get(
        parameters = arrayOf(parameterTypeName),
        returnType = Unit::class.asTypeName(),
      )
    assertThat(typeName.toString()).isEqualTo("((kotlin.Int) -> kotlin.Int) -> kotlin.Unit")
  }

  @Test
  fun equalsAndHashCode() {
    val lambdaTypeName1 = LambdaTypeName.get(parameters = arrayOf(INT), returnType = INT)
    val lambdaTypeName2 = LambdaTypeName.get(parameters = arrayOf(INT), returnType = INT)
    assertThat(lambdaTypeName1).isEqualTo(lambdaTypeName2)
    assertThat(lambdaTypeName1.hashCode()).isEqualTo(lambdaTypeName2.hashCode())
    assertThat(lambdaTypeName1.toString()).isEqualTo(lambdaTypeName2.toString())

    val differentReceiver =
      LambdaTypeName.get(parameters = arrayOf(INT), returnType = INT, receiver = ANY)
    assertThat(differentReceiver).isNotEqualTo(lambdaTypeName1)

    assertThat(lambdaTypeName1.copy(nullable = true)).isNotEqualTo(lambdaTypeName1)

    assertThat(
        lambdaTypeName1.copy(
          annotations = listOf(AnnotationSpec.builder(Suppress::class.asClassName()).build())
        )
      )
      .isNotEqualTo(lambdaTypeName1)

    assertThat(lambdaTypeName1.copy(suspending = true)).isNotEqualTo(lambdaTypeName1)
  }

  @Test
  fun equalsAndHashCodeIgnoreTags() {
    val lambdaTypeName = LambdaTypeName.get(parameters = arrayOf(INT), returnType = INT)

    val tagged = lambdaTypeName.copy(tags = mapOf(String::class to "test"))

    assertThat(tagged).isEqualTo(lambdaTypeName)
    assertThat(tagged.hashCode()).isEqualTo(lambdaTypeName.hashCode())
  }
}
