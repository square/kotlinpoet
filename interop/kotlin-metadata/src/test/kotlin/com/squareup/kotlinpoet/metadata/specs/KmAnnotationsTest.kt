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
package com.squareup.kotlinpoet.metadata.specs

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.metadata.KmAnnotation
import kotlin.metadata.KmAnnotationArgument.AnnotationValue
import kotlin.metadata.KmAnnotationArgument.ArrayValue
import kotlin.metadata.KmAnnotationArgument.BooleanValue
import kotlin.metadata.KmAnnotationArgument.ByteValue
import kotlin.metadata.KmAnnotationArgument.CharValue
import kotlin.metadata.KmAnnotationArgument.DoubleValue
import kotlin.metadata.KmAnnotationArgument.EnumValue
import kotlin.metadata.KmAnnotationArgument.FloatValue
import kotlin.metadata.KmAnnotationArgument.IntValue
import kotlin.metadata.KmAnnotationArgument.KClassValue
import kotlin.metadata.KmAnnotationArgument.LongValue
import kotlin.metadata.KmAnnotationArgument.ShortValue
import kotlin.metadata.KmAnnotationArgument.StringValue
import kotlin.metadata.KmAnnotationArgument.UByteValue
import kotlin.metadata.KmAnnotationArgument.UIntValue
import kotlin.metadata.KmAnnotationArgument.ULongValue
import kotlin.metadata.KmAnnotationArgument.UShortValue
import kotlin.test.Test

class KmAnnotationsTest {

  @Test fun noMembers() {
    val annotation = KmAnnotation("test/NoMembersAnnotation", emptyMap())
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.NoMembersAnnotation
      """.trimIndent(),
    )
  }

  @Test fun byteValue() {
    val annotation = KmAnnotation(
      "test/ByteValueAnnotation",
      mapOf("value" to ByteValue(2)),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.ByteValueAnnotation(value = 2)
      """.trimIndent(),
    )
  }

  @Test fun charValue() {
    val annotation = KmAnnotation(
      "test/CharValueAnnotation",
      mapOf("value" to CharValue('2')),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.CharValueAnnotation(value = '2')
      """.trimIndent(),
    )
  }

  @Test fun shortValue() {
    val annotation = KmAnnotation(
      "test/ShortValueAnnotation",
      mapOf("value" to ShortValue(2)),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.ShortValueAnnotation(value = 2)
      """.trimIndent(),
    )
  }

  @Test fun intValue() {
    val annotation = KmAnnotation(
      "test/IntValueAnnotation",
      mapOf("value" to IntValue(2)),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.IntValueAnnotation(value = 2)
      """.trimIndent(),
    )
  }

  @Test fun longValue() {
    val annotation = KmAnnotation(
      "test/LongValueAnnotation",
      mapOf("value" to LongValue(2L)),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.LongValueAnnotation(value = 2L)
      """.trimIndent(),
    )
  }

  @Test fun floatValue() {
    val annotation = KmAnnotation(
      "test/FloatValueAnnotation",
      mapOf("value" to FloatValue(2.0F)),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.FloatValueAnnotation(value = 2.0F)
      """.trimIndent(),
    )
  }

  @Test fun doubleValue() {
    val annotation = KmAnnotation(
      "test/DoubleValueAnnotation",
      mapOf("value" to DoubleValue(2.0)),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.DoubleValueAnnotation(value = 2.0)
      """.trimIndent(),
    )
  }

  @Test fun booleanValue() {
    val annotation = KmAnnotation(
      "test/BooleanValueAnnotation",
      mapOf("value" to BooleanValue(true)),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.BooleanValueAnnotation(value = true)
      """.trimIndent(),
    )
  }

  @Test fun uByteValue() {
    val annotation = KmAnnotation(
      "test/UByteValueAnnotation",
      mapOf("value" to UByteValue(2u)),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.UByteValueAnnotation(value = 2u)
      """.trimIndent(),
    )
  }

  @Test fun uShortValue() {
    val annotation = KmAnnotation(
      "test/UShortValueAnnotation",
      mapOf("value" to UShortValue(2u)),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.UShortValueAnnotation(value = 2u)
      """.trimIndent(),
    )
  }

  @Test fun uIntValue() {
    val annotation = KmAnnotation(
      "test/UIntValueAnnotation",
      mapOf("value" to UIntValue(2u)),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.UIntValueAnnotation(value = 2u)
      """.trimIndent(),
    )
  }

  @Test fun uLongValue() {
    val annotation = KmAnnotation(
      "test/ULongValueAnnotation",
      mapOf("value" to ULongValue(2u)),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.ULongValueAnnotation(value = 2u)
      """.trimIndent(),
    )
  }

  @Test fun stringValue() {
    val annotation = KmAnnotation(
      "test/StringValueAnnotation",
      mapOf("value" to StringValue("taco")),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.StringValueAnnotation(value = "taco")
      """.trimIndent(),
    )
  }

  @Test fun kClassValue() {
    val annotation = KmAnnotation(
      "test/KClassValueAnnotation",
      mapOf("value" to KClassValue("test/OtherClass")),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.KClassValueAnnotation(value = test.OtherClass::class)
      """.trimIndent(),
    )
  }

  @Test fun enumValue() {
    val annotation = KmAnnotation(
      "test/EnumValueAnnotation",
      mapOf("value" to EnumValue("test/OtherClass", "VALUE")),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.EnumValueAnnotation(value = test.OtherClass.VALUE)
      """.trimIndent(),
    )
  }

  @Test fun annotationValue() {
    val annotation = KmAnnotation(
      "test/AnnotationValueAnnotation",
      mapOf(
        "value" to AnnotationValue(
          KmAnnotation("test/OtherAnnotation", mapOf("value" to StringValue("Hello!"))),
        ),
      ),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.AnnotationValueAnnotation(value = test.OtherAnnotation(value = "Hello!"))
      """.trimIndent(),
    )
  }

  @Test fun arrayValue() {
    val annotation = KmAnnotation(
      "test/ArrayValueAnnotation",
      mapOf("value" to ArrayValue(listOf(IntValue(1), IntValue(2), IntValue(3)))),
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo(
      """
      @test.ArrayValueAnnotation(value = [1, 2, 3])
      """.trimIndent(),
    )
  }
}
