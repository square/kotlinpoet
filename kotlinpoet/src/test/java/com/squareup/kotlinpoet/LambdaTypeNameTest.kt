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
import com.squareup.kotlinpoet.KModifier.VARARG
import javax.annotation.Nullable
import kotlin.test.Test

class LambdaTypeNameTest {

  @Retention(AnnotationRetention.RUNTIME)
  annotation class HasSomeAnnotation

  @HasSomeAnnotation
  inner class IsAnnotated

  @Test fun receiverWithoutAnnotationHasNoParens() {
    val typeName = LambdaTypeName.get(
            Int::class.asClassName(),
            listOf(),
            Unit::class.asTypeName())
    assertThat(typeName.toString()).isEqualTo("kotlin.Int.() -> kotlin.Unit")
  }

  @Test fun receiverWithAnnotationHasParens() {
    val annotation = IsAnnotated::class.java.getAnnotation(HasSomeAnnotation::class.java)
    val typeName = LambdaTypeName.get(
            Int::class.asClassName().copy(
                annotations = listOf(AnnotationSpec.get(annotation, includeDefaultValues = true))
            ),
            listOf(),
            Unit::class.asTypeName())
    assertThat(typeName.toString()).isEqualTo(
            "(@com.squareup.kotlinpoet.LambdaTypeNameTest.HasSomeAnnotation kotlin.Int).() -> kotlin.Unit")
  }

  @Test fun paramsWithAnnotationsForbidden() {
    assertThrows<IllegalArgumentException> {
      LambdaTypeName.get(
          parameters = *arrayOf(ParameterSpec.builder("foo", Int::class)
              .addAnnotation(Nullable::class)
              .build()),
          returnType = Unit::class.asTypeName())
    }.hasMessageThat().isEqualTo("Parameters with annotations are not allowed")
  }

  @Test fun paramsWithModifiersForbidden() {
    assertThrows<IllegalArgumentException> {
      LambdaTypeName.get(
          parameters = *arrayOf(ParameterSpec.builder("foo", Int::class)
              .addModifiers(VARARG)
              .build()),
          returnType = Unit::class.asTypeName())
    }.hasMessageThat().isEqualTo("Parameters with modifiers are not allowed")
  }

  @Test fun paramsWithDefaultValueForbidden() {
    assertThrows<IllegalArgumentException> {
      LambdaTypeName.get(
          parameters = *arrayOf(ParameterSpec.builder("foo", Int::class)
              .defaultValue("42")
              .build()),
          returnType = Unit::class.asTypeName())
    }.hasMessageThat().isEqualTo("Parameters with default values are not allowed")
  }

  @Test fun lambdaReturnType() {
    val returnTypeName = LambdaTypeName.get(
        parameters = *arrayOf(Int::class.asTypeName()),
        returnType = Unit::class.asTypeName())
    val typeName = LambdaTypeName.get(
        parameters = *arrayOf(Int::class.asTypeName()),
        returnType = returnTypeName)
    assertThat(typeName.toString())
        .isEqualTo("(kotlin.Int) -> ((kotlin.Int) -> kotlin.Unit)")
  }

  @Test fun lambdaParameterType() {
    val parameterTypeName = LambdaTypeName.get(
        parameters = *arrayOf(Int::class.asTypeName()),
        returnType = Int::class.asTypeName())
    val typeName = LambdaTypeName.get(
        parameters = *arrayOf(parameterTypeName),
        returnType = Unit::class.asTypeName())
    assertThat(typeName.toString())
        .isEqualTo("((kotlin.Int) -> kotlin.Int) -> kotlin.Unit")
  }
}
