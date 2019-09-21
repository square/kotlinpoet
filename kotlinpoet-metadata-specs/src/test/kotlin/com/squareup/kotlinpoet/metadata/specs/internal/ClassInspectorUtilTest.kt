package com.squareup.kotlinpoet.metadata.specs.internal

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlin.test.Test

@KotlinPoetMetadataPreview
class ClassInspectorUtilTest {

  @Test fun bestGuess_simple() {
    assertThat(ClassInspectorUtil.bestGuessClassName("some/path/Foo"))
        .isEqualTo(ClassName("some.path", "Foo"))
  }

  @Test fun bestGuess_nested() {
    assertThat(ClassInspectorUtil.bestGuessClassName("some/path/Foo.Nested"))
        .isEqualTo(ClassName("some.path", "Foo", "Nested"))
  }

  @Test fun bestGuess_simple_dollarNameStart() {
    assertThat(ClassInspectorUtil.bestGuessClassName("some/path/Foo$"))
        .isEqualTo(ClassName("some.path", "Foo$"))
  }

  @Test fun bestGuess_simple_dollarNameMiddle() {
    assertThat(ClassInspectorUtil.bestGuessClassName("some/path/Fo${'$'}o"))
        .isEqualTo(ClassName("some.path", "Fo${'$'}o"))
  }

  @Test fun bestGuess_simple_dollarNameEnd() {
    assertThat(ClassInspectorUtil.bestGuessClassName("some/path/Nested.${'$'}Foo"))
        .isEqualTo(ClassName("some.path", "Nested", "${'$'}Foo"))
  }

  @Test fun bestGuess_nested_dollarNameStart() {
    assertThat(ClassInspectorUtil.bestGuessClassName("some/path/Nested.Foo$"))
        .isEqualTo(ClassName("some.path", "Nested", "Foo$"))
  }

  @Test fun bestGuess_nested_dollarNameMiddle() {
    assertThat(ClassInspectorUtil.bestGuessClassName("some/path/Nested.Fo${'$'}o"))
        .isEqualTo(ClassName("some.path", "Nested", "Fo${'$'}o"))
  }

  @Test fun bestGuess_nested_dollarNameEnd() {
    assertThat(ClassInspectorUtil.bestGuessClassName("some/path/Nested.${'$'}Foo"))
        .isEqualTo(ClassName("some.path", "Nested", "${'$'}Foo"))
  }

  @Test fun bestGuess_noPackageName() {
    assertThat(ClassInspectorUtil.bestGuessClassName("ClassWithNoPackage"))
        .isEqualTo(ClassName("", "ClassWithNoPackage"))
  }

  @Test fun throwsSpec_normal() {
    assertThat(ClassInspectorUtil.createThrowsSpec(listOf(Exception::class.asClassName())))
        .isEqualTo(AnnotationSpec.builder(Throws::class.asClassName())
            .addMember("exceptionClasses = [%T::class]", Exception::class.asClassName())
            .build())
  }
}
