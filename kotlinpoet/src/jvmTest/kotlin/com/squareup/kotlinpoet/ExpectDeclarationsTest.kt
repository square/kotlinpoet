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

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ExpectDeclarationsTest {
  @Test fun expectFunDeclaration() {
    val methodSpec = FunSpec.builder("function")
      .addModifiers(KModifier.EXPECT)
      .build()

    assertThat(methodSpec.toString()).isEqualTo(
      """
      |public expect fun function()
      |
      """.trimMargin(),
    )
  }

  @Test fun implicitExpectFunDeclaration() {
    val builder = TypeSpec.classBuilder("Test")
      .addModifiers(KModifier.EXPECT)
    val methodSpec = FunSpec.builder("function")
      .build()
    builder.addFunction(methodSpec)

    assertThat(builder.build().toString()).isEqualTo(
      """
        |public expect class Test {
        |  public fun function()
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun expectPropertyDeclaration() {
    val propertySpec = PropertySpec.builder("prop", String::class)
      .addModifiers(KModifier.EXPECT)
      .build()

    assertThat(propertySpec.toString()).isEqualTo(
      """
      |expect val prop: kotlin.String
      |
      """.trimMargin(),
    )
  }

  @Test fun implicitExpectPropertyDeclaration() {
    val builder = TypeSpec.classBuilder("Test")
      .addModifiers(KModifier.EXPECT)
    val propertySpec = PropertySpec.builder("prop", String::class)
      .build()
    builder.addProperty(propertySpec)

    assertThat(builder.build().toString()).isEqualTo(
      """
        |public expect class Test {
        |  public val prop: kotlin.String
        |}
        |
      """.trimMargin(),
    )
  }
}
