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
    assertThat(ClassInspectorUtil.createClassName("some/path/Foo"))
        .isEqualTo(ClassName("some.path", "Foo"))
  }

  @Test fun bestGuess_nested() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Foo.Nested"))
        .isEqualTo(ClassName("some.path", "Foo", "Nested"))
  }

  @Test fun bestGuess_simple_dollarNameStart() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Foo$"))
        .isEqualTo(ClassName("some.path", "Foo$"))
  }

  @Test fun bestGuess_simple_dollarNameMiddle() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Fo${'$'}o"))
        .isEqualTo(ClassName("some.path", "Fo${'$'}o"))
  }

  @Test fun bestGuess_simple_dollarNameEnd() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Nested.${'$'}Foo"))
        .isEqualTo(ClassName("some.path", "Nested", "${'$'}Foo"))
  }

  @Test fun bestGuess_nested_dollarNameStart() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Nested.Foo$"))
        .isEqualTo(ClassName("some.path", "Nested", "Foo$"))
  }

  @Test fun bestGuess_nested_dollarNameMiddle() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Nested.Fo${'$'}o"))
        .isEqualTo(ClassName("some.path", "Nested", "Fo${'$'}o"))
  }

  @Test fun bestGuess_nested_dollarNameEnd() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Nested.${'$'}Foo"))
        .isEqualTo(ClassName("some.path", "Nested", "${'$'}Foo"))
  }

  @Test fun bestGuess_noPackageName() {
    assertThat(ClassInspectorUtil.createClassName("ClassWithNoPackage"))
        .isEqualTo(ClassName("", "ClassWithNoPackage"))
  }

  // Regression test for avoiding https://github.com/square/kotlinpoet/issues/795
  @Test fun bestGuess_noEmptyNames() {
    val noPackage = ClassInspectorUtil.createClassName("ClassWithNoPackage")
    assertThat(noPackage.simpleNames.any { it.isEmpty() }).isFalse()

    val withPackage = ClassInspectorUtil.createClassName("path/to/ClassWithNoPackage")
    assertThat(withPackage.simpleNames.any { it.isEmpty() }).isFalse()
  }

  @Test fun throwsSpec_normal() {
    assertThat(ClassInspectorUtil.createThrowsSpec(listOf(Exception::class.asClassName())))
        .isEqualTo(AnnotationSpec.builder(Throws::class.asClassName())
            .addMember("exceptionClasses = [%T::class]", Exception::class.asClassName())
            .build())
  }
}
