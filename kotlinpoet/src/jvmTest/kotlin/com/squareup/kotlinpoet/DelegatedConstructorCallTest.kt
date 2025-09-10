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
package com.squareup.kotlinpoet

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test

class DelegatedConstructorCallTest {
  @Test
  fun defaultPresentInClass() {
    val builder = TypeSpec.classBuilder("Test")
      .superclass(ClassName("testpackage", "TestSuper"))
    assertThat(builder.build().toString()).isEqualTo(
      """
        |public class Test : testpackage.TestSuper()
        |
      """.trimMargin(),
    )
  }

  @Test
  fun defaultPresentInObject() {
    val builder = TypeSpec.objectBuilder("Test")
      .superclass(ClassName("testpackage", "TestSuper"))
    assertThat(builder.build().toString()).isEqualTo(
      """
        |public object Test : testpackage.TestSuper()
        |
      """.trimMargin(),
    )
  }

  @Test
  fun defaultNotPresentInExternalClass() {
    val builder = TypeSpec.classBuilder("Test")
      .addModifiers(KModifier.EXTERNAL)
      .superclass(ClassName("testpackage", "TestSuper"))
    assertThat(builder.build().toString()).isEqualTo(
      """
        |public external class Test : testpackage.TestSuper
        |
      """.trimMargin(),
    )
  }

  @Test
  fun defaultNotPresentInExpectClass() {
    val builder = TypeSpec.classBuilder("Test")
      .addModifiers(KModifier.EXPECT)
      .superclass(ClassName("testpackage", "TestSuper"))
    assertThat(builder.build().toString()).isEqualTo(
      """
        |public expect class Test : testpackage.TestSuper
        |
      """.trimMargin(),
    )
  }

  @Test
  fun defaultNotPresentInExpectObject() {
    val builder = TypeSpec.objectBuilder("Test")
      .addModifiers(KModifier.EXPECT)
      .superclass(ClassName("testpackage", "TestSuper"))
    assertThat(builder.build().toString()).isEqualTo(
      """
        |public expect object Test : testpackage.TestSuper
        |
      """.trimMargin(),
    )
  }

  @Test
  fun defaultNotPresentInExternalObject() {
    val builder = TypeSpec.objectBuilder("Test")
      .addModifiers(KModifier.EXTERNAL)
      .superclass(ClassName("testpackage", "TestSuper"))
    assertThat(builder.build().toString()).isEqualTo(
      """
        |public external object Test : testpackage.TestSuper
        |
      """.trimMargin(),
    )
  }

  @Test
  fun allowedInClass() {
    val builder = TypeSpec.classBuilder("Test")
      .superclass(ClassName("testpackage", "TestSuper"))
      .addSuperclassConstructorParameter("anything")
    assertThat(builder.build().toString()).isEqualTo(
      """
        |public class Test : testpackage.TestSuper(anything)
        |
      """.trimMargin(),
    )
  }

  @Test
  fun allowedInObject() {
    val builder = TypeSpec.objectBuilder("Test")
      .superclass(ClassName("testpackage", "TestSuper"))
      .addSuperclassConstructorParameter("anything")
    assertThat(builder.build().toString()).isEqualTo(
      """
        |public object Test : testpackage.TestSuper(anything)
        |
      """.trimMargin(),
    )
  }

  @Test
  fun allowedInClassSecondary() {
    val builder = TypeSpec.classBuilder("Test")
    val primaryConstructorBuilder = FunSpec.constructorBuilder()
    val primaryConstructor = primaryConstructorBuilder.build()
    builder.primaryConstructor(primaryConstructor)
    val secondaryConstructorBuilder = FunSpec.constructorBuilder()
      .addParameter(ParameterSpec("foo", ClassName("kotlin", "String")))
      .callThisConstructor()
    builder.addFunction(secondaryConstructorBuilder.build())
    assertThat(builder.build().toString()).isEqualTo(
      """
        |public class Test() {
        |  public constructor(foo: kotlin.String) : this()
        |}
        |
      """.trimMargin(),
    )
  }

  @Test
  fun notAllowedInExternalClass() {
    val builder = TypeSpec.classBuilder("Test")
      .addModifiers(KModifier.EXTERNAL)
      .superclass(ClassName("testpackage", "TestSuper"))
      .addSuperclassConstructorParameter("anything")
    assertFailure { builder.build() }
      .isInstanceOf<IllegalStateException>()
  }

  @Test
  fun notAllowedInExternalObject() {
    val builder = TypeSpec.objectBuilder("Test")
      .addModifiers(KModifier.EXTERNAL)
      .superclass(ClassName("testpackage", "TestSuper"))
      .addSuperclassConstructorParameter("anything")
    assertFailure { builder.build() }
      .isInstanceOf<IllegalStateException>()
  }

  @Test
  fun notAllowedInExternalClassSecondary() {
    val builder = TypeSpec.classBuilder("Test")
      .addModifiers(KModifier.EXTERNAL)
    val primaryConstructorBuilder = FunSpec.constructorBuilder()
    builder.primaryConstructor(primaryConstructorBuilder.build())
    val secondaryConstructorBuilder = FunSpec.constructorBuilder()
      .addParameter(ParameterSpec("foo", ClassName("kotlin", "String")))
      .callThisConstructor()
    builder.addFunction(secondaryConstructorBuilder.build())
    assertFailure { builder.build() }
      .isInstanceOf<IllegalStateException>()
  }
}
