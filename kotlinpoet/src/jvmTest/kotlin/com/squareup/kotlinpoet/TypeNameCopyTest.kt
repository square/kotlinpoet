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
package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TypeNameCopyTest {
  private val originalAnnotations =
    listOf(AnnotationSpec.builder(ClassName("test", "Original")).build())
  private val annotations = listOf(AnnotationSpec.builder(ClassName("test", "Copied")).build())
  private val tags: Map<KClass<*>, Any> = mapOf(String::class to "tag")

  @Test
  fun className() {
    val type =
      ClassName("test", "Outer", "Inner").copy(annotations = originalAnnotations, tags = tags)
    val copies: List<ClassName> =
      listOf(
        type.copy(),
        type.copy(nullable = true),
        type.copy(annotations = annotations),
        type.copy(true, annotations),
      )
    assertCopies(type, copies)
  }

  @Test
  fun parameterizedTypeName() {
    val type =
      ClassName("test", "Outer")
        .parameterizedBy(STRING)
        .nestedClass("Inner", listOf(INT))
        .copy(annotations = originalAnnotations, tags = tags)
    val copies: List<ParameterizedTypeName> =
      listOf(
        type.copy(),
        type.copy(nullable = true),
        type.copy(annotations = annotations),
        type.copy(true, annotations),
      )
    assertCopies(type, copies)
  }

  @Test
  @OptIn(ExperimentalKotlinPoetApi::class)
  fun lambdaTypeName() {
    val type =
      LambdaTypeName.get(
          receiver = STRING,
          parameters = listOf(ParameterSpec.builder("value", INT).build()),
          returnType = BOOLEAN,
          contextParameters = listOf(LONG),
        )
        .copy(suspending = true, annotations = originalAnnotations, tags = tags)
    val copies: List<LambdaTypeName> =
      listOf(
        type.copy(),
        type.copy(nullable = true),
        type.copy(annotations = annotations),
        type.copy(true, annotations),
      )
    assertCopies(type, copies)
  }

  @Test
  fun typeVariableName() {
    val type =
      TypeVariableName("T", NUMBER, variance = KModifier.OUT)
        .copy(reified = true, annotations = originalAnnotations, tags = tags)
    val copies: List<TypeVariableName> =
      listOf(
        type.copy(),
        type.copy(nullable = true),
        type.copy(annotations = annotations),
        type.copy(true, annotations),
      )
    assertCopies(type, copies)
  }

  @Test
  fun wildcardTypeName() {
    for (wildcard in
      listOf(WildcardTypeName.producerOf(NUMBER), WildcardTypeName.consumerOf(STRING))) {
      val type = wildcard.copy(annotations = originalAnnotations, tags = tags)
      val copies: List<WildcardTypeName> =
        listOf(
          type.copy(),
          type.copy(nullable = true),
          type.copy(annotations = annotations),
          type.copy(true, annotations),
        )
      assertCopies(type, copies)
    }
  }

  @Test
  fun dynamicCannotBeCopied() {
    assertFailsWith<UnsupportedOperationException> { Dynamic.copy() }
    assertFailsWith<UnsupportedOperationException> { Dynamic.copy(nullable = true) }
  }

  private fun assertCopies(type: TypeName, copies: List<TypeName>) {
    val expected =
      listOf(
        type,
        type.copy(true, originalAnnotations, tags),
        type.copy(false, annotations, tags),
        type.copy(true, annotations, tags),
      )
    assertEquals(expected, copies)
    for (copy in copies) {
      assertEquals(tags, copy.tags)
      assertEquals(type::class, copy::class)
      val asTypeName: TypeName = copy
      assertEquals(type, asTypeName.copy(nullable = false, annotations = originalAnnotations))
    }
    assertEquals(false, type.isNullable)
    assertEquals(originalAnnotations, type.annotations)
    assertEquals(tags, type.tags)
  }
}
