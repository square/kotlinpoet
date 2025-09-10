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
package com.squareup.kotlinpoet.metadata.specs.classinspectors

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil
import kotlin.test.Test

class ClassInspectorUtilTest {

  @Test fun createClassName_simple() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Foo"))
      .isEqualTo(ClassName("some.path", "Foo"))
  }

  @Test fun createClassName_nested() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Foo.Nested"))
      .isEqualTo(ClassName("some.path", "Foo", "Nested"))
  }

  @Test fun createClassName_simple_dollarNameStart() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Foo$"))
      .isEqualTo(ClassName("some.path", "Foo$"))
  }

  @Test fun createClassName_simple_dollarNameMiddle() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Fo${'$'}o"))
      .isEqualTo(ClassName("some.path", "Fo${'$'}o"))
  }

  @Test fun createClassName_simple_dollarNameEnd() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Nested.${'$'}Foo"))
      .isEqualTo(ClassName("some.path", "Nested", "${'$'}Foo"))
  }

  @Test fun createClassName_nested_dollarNameStart() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Nested.Foo$"))
      .isEqualTo(ClassName("some.path", "Nested", "Foo$"))
  }

  @Test fun createClassName_nested_dollarNameMiddle() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Nested.Fo${'$'}o"))
      .isEqualTo(ClassName("some.path", "Nested", "Fo${'$'}o"))
  }

  @Test fun createClassName_nested_dollarNameEnd() {
    assertThat(ClassInspectorUtil.createClassName("some/path/Nested.${'$'}Foo"))
      .isEqualTo(ClassName("some.path", "Nested", "${'$'}Foo"))
  }

  @Test fun createClassName_noPackageName() {
    assertThat(ClassInspectorUtil.createClassName("ClassWithNoPackage"))
      .isEqualTo(ClassName("", "ClassWithNoPackage"))
  }

  // Regression test for avoiding https://github.com/square/kotlinpoet/issues/795
  @Test fun createClassName_noEmptyNames() {
    val noPackage = ClassInspectorUtil.createClassName("ClassWithNoPackage")
    assertThat(noPackage.simpleNames.any { it.isEmpty() }).isFalse()

    val withPackage = ClassInspectorUtil.createClassName("path/to/ClassWithNoPackage")
    assertThat(withPackage.simpleNames.any { it.isEmpty() }).isFalse()
  }

  @Test fun createClassName_packageWithCaps() {
    assertThat(ClassInspectorUtil.createClassName("some/Path/Foo.Nested"))
      .isEqualTo(ClassName("some.Path", "Foo", "Nested"))
  }

  @Test fun throwsSpec_normal() {
    assertThat(ClassInspectorUtil.createThrowsSpec(listOf(Exception::class.asClassName())))
      .isEqualTo(
        AnnotationSpec.builder(Throws::class.asClassName())
          .addMember("exceptionClasses = [%T::class]", Exception::class.asClassName())
          .build(),
      )
  }
}
