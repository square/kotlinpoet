package com.squareup.kotlinpoet.metadata.specs.internal

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlin.test.Test
import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmAnnotationArgument.AnnotationValue
import kotlinx.metadata.KmAnnotationArgument.ArrayValue
import kotlinx.metadata.KmAnnotationArgument.BooleanValue
import kotlinx.metadata.KmAnnotationArgument.ByteValue
import kotlinx.metadata.KmAnnotationArgument.CharValue
import kotlinx.metadata.KmAnnotationArgument.DoubleValue
import kotlinx.metadata.KmAnnotationArgument.EnumValue
import kotlinx.metadata.KmAnnotationArgument.FloatValue
import kotlinx.metadata.KmAnnotationArgument.IntValue
import kotlinx.metadata.KmAnnotationArgument.KClassValue
import kotlinx.metadata.KmAnnotationArgument.LongValue
import kotlinx.metadata.KmAnnotationArgument.ShortValue
import kotlinx.metadata.KmAnnotationArgument.StringValue
import kotlinx.metadata.KmAnnotationArgument.UByteValue
import kotlinx.metadata.KmAnnotationArgument.UIntValue
import kotlinx.metadata.KmAnnotationArgument.ULongValue
import kotlinx.metadata.KmAnnotationArgument.UShortValue

@KotlinPoetMetadataPreview
class KmAnnotationsTest {

  @Test fun noMembers() {
    val annotation = KmAnnotation("test/NoMembersAnnotation", emptyMap())
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.NoMembersAnnotation
    """.trimIndent())
  }

  @Test fun byteValue() {
    val annotation = KmAnnotation(
        "test/ByteValueAnnotation",
        mapOf("value" to ByteValue(2))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.ByteValueAnnotation(value = 2)
    """.trimIndent())
  }

  @Test fun charValue() {
    val annotation = KmAnnotation(
        "test/CharValueAnnotation",
        mapOf("value" to CharValue('2'))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.CharValueAnnotation(value = '2')
    """.trimIndent())
  }

  @Test fun shortValue() {
    val annotation = KmAnnotation(
        "test/ShortValueAnnotation",
        mapOf("value" to ShortValue(2))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.ShortValueAnnotation(value = 2)
    """.trimIndent())
  }

  @Test fun intValue() {
    val annotation = KmAnnotation(
        "test/IntValueAnnotation",
        mapOf("value" to IntValue(2))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.IntValueAnnotation(value = 2)
    """.trimIndent())
  }

  @Test fun longValue() {
    val annotation = KmAnnotation(
        "test/LongValueAnnotation",
        mapOf("value" to LongValue(2L))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.LongValueAnnotation(value = 2L)
    """.trimIndent())
  }

  @Test fun floatValue() {
    val annotation = KmAnnotation(
        "test/FloatValueAnnotation",
        mapOf("value" to FloatValue(2.0F))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.FloatValueAnnotation(value = 2.0F)
    """.trimIndent())
  }

  @Test fun doubleValue() {
    val annotation = KmAnnotation(
        "test/DoubleValueAnnotation",
        mapOf("value" to DoubleValue(2.0))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.DoubleValueAnnotation(value = 2.0)
    """.trimIndent())
  }

  @Test fun booleanValue() {
    val annotation = KmAnnotation(
        "test/BooleanValueAnnotation",
        mapOf("value" to BooleanValue(true))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.BooleanValueAnnotation(value = true)
    """.trimIndent())
  }

  @Test fun uByteValue() {
    val annotation = KmAnnotation(
        "test/UByteValueAnnotation",
        mapOf("value" to UByteValue(2))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.UByteValueAnnotation(value = 2u)
    """.trimIndent())
  }

  @Test fun uShortValue() {
    val annotation = KmAnnotation(
        "test/UShortValueAnnotation",
        mapOf("value" to UShortValue(2))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.UShortValueAnnotation(value = 2u)
    """.trimIndent())
  }

  @Test fun uIntValue() {
    val annotation = KmAnnotation(
        "test/UIntValueAnnotation",
        mapOf("value" to UIntValue(2))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.UIntValueAnnotation(value = 2u)
    """.trimIndent())
  }

  @Test fun uLongValue() {
    val annotation = KmAnnotation(
        "test/ULongValueAnnotation",
        mapOf("value" to ULongValue(2))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.ULongValueAnnotation(value = 2u)
    """.trimIndent())
  }

  @Test fun stringValue() {
    val annotation = KmAnnotation(
        "test/StringValueAnnotation",
        mapOf("value" to StringValue("taco"))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.StringValueAnnotation(value = "taco")
    """.trimIndent())
  }

  @Test fun kClassValue() {
    val annotation = KmAnnotation(
        "test/KClassValueAnnotation",
        mapOf("value" to KClassValue("test/OtherClass"))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.KClassValueAnnotation(value = test.OtherClass::class)
    """.trimIndent())
  }

  @Test fun enumValue() {
    val annotation = KmAnnotation(
        "test/EnumValueAnnotation",
        mapOf("value" to EnumValue("test/OtherClass", "VALUE"))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.EnumValueAnnotation(value = test.OtherClass.VALUE)
    """.trimIndent())
  }

  @Test fun annotationValue() {
    val annotation = KmAnnotation(
        "test/AnnotationValueAnnotation",
        mapOf("value" to AnnotationValue(
            KmAnnotation("test/OtherAnnotation", mapOf("value" to StringValue("Hello!")))
        ))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.AnnotationValueAnnotation(value = test.OtherAnnotation(value = "Hello!"))
    """.trimIndent())
  }

  @Test fun arrayValue() {
    val annotation = KmAnnotation(
        "test/ArrayValueAnnotation",
        mapOf("value" to ArrayValue(listOf(IntValue(1), IntValue(2), IntValue(3))))
    )
    assertThat(annotation.toAnnotationSpec().toString()).isEqualTo("""
      @test.ArrayValueAnnotation(value = [1, 2, 3])
    """.trimIndent())
  }
}
