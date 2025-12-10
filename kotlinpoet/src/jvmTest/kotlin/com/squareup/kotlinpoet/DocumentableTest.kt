/*
 * Copyright (C) 2015 Square, Inc.
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
import java.util.Random
import kotlin.test.Test

class DocumentableTest {
  // https://github.com/square/kotlinpoet/issues/887
  @Test
  fun kdocWithParameters() {
    val someClass =
      TypeSpec.classBuilder("SomeClass")
        .addKdoc("Start of a nested comment: /*\n")
        .addKdoc("[random][%T] reference\n", Random::class)
        .addKdoc("End of a nested comment: */\n")
        .addKdoc(CodeBlock.of("Some comment in args: [%L].\n", "*/"))
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameter(
              ParameterSpec.builder("parameter", Double::class)
                .addKdoc(
                  CodeBlock.of(
                    "%L",
                    """
                    |Parameter Kdoc with comments
                    |/*
                    |"""
                      .trimMargin(),
                  )
                )
                .build()
            )
            .build()
        )
        .addProperty(
          PropertySpec.builder("property", Boolean::class)
            .addKdoc("Property Kdoc with comments: /* */.\n")
            .initializer("false")
            .build()
        )
        .build()
    assertThat(toString(someClass))
      .isEqualTo(
        """
        |package com.squareup.test
        |
        |import kotlin.Boolean
        |import kotlin.Double
        |
        |/**
        | * Start of a nested comment: /&#42;
        | * [random][java.util.Random] reference
        | * End of a nested comment: &#42;/
        | * Some comment in args: [&#42;/].
        | *
        | * @param parameter Parameter Kdoc with comments
        | * /&#42;
        | */
        |public class SomeClass(
        |  parameter: Double,
        |) {
        |  /**
        |   * Property Kdoc with comments: /&#42; &#42;/.
        |   */
        |  public val `property`: Boolean = false
        |}
        |"""
          .trimMargin()
      )
  }

  private fun toString(typeSpec: TypeSpec): String {
    return FileSpec.get("com.squareup.test", typeSpec).toString()
  }
}
