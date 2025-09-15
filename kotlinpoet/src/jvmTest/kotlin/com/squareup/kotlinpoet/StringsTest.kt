/*
 * Copyright (C) 2018 Square, Inc.
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
import org.junit.Test

class StringsTest {
  @Test fun singleLineStringWithDollarSymbols() {
    val stringWithTemplate = "$" + "annoyingUser" + " is annoying."
    val funSpec = FunSpec.builder("getString")
      .returns(STRING)
      .addStatement("return %S", stringWithTemplate)
      .build()
    assertThat(funSpec.toString())
      .isEqualTo("public fun getString(): kotlin.String = \"\${\'\$\'}annoyingUser is annoying.\"\n")
  }

  @Test fun multilineStringWithDollarSymbols() {
    val stringWithTemplate = "Some string\n" + "$" + "annoyingUser" + " is annoying."
    val funSpec = FunSpec.builder("getString")
      .returns(STRING)
      .addStatement("return %S", stringWithTemplate)
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      "public fun getString(): kotlin.String = \"\"\"\n" +
        "|Some string\n" +
        "|\${\'\$\'}annoyingUser is annoying.\n" +
        "\"\"\".trimMargin()\n",
    )
  }

  @Test fun singleLineStringTemplate() {
    val stringWithTemplate = "$" + "annoyingUser" + " is annoying."
    val funSpec = FunSpec.builder("getString")
      .returns(STRING)
      .addStatement("return %P", stringWithTemplate)
      .build()
    assertThat(funSpec.toString())
      .isEqualTo("public fun getString(): kotlin.String = \"\"\"\$annoyingUser is annoying.\"\"\"\n")
  }

  @Test fun multilineStringTemplate() {
    val stringWithTemplate = "Some string\n" + "$" + "annoyingUser" + " is annoying."
    val funSpec = FunSpec.builder("getString")
      .returns(STRING)
      .addStatement("return %P", stringWithTemplate)
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      "public fun getString(): kotlin.String = \"\"\"\n" +
        "|Some string\n" +
        "|\$annoyingUser is annoying.\n" +
        "\"\"\".trimMargin()\n",
    )
  }

  // https://github.com/square/kotlinpoet/issues/572
  @Test fun templateStringWithStringLiteralReference() {
    val string = "SELECT * FROM socialFeedItem WHERE message IS NOT NULL AND userId \${ if (userId == null) \"IS\" else \"=\" } ?1 ORDER BY datetime(creation_time) DESC"
    val funSpec = FunSpec.builder("getString")
      .returns(STRING)
      .addStatement("return %P", string)
      .build()
    assertThat(funSpec.toString())
      .isEqualTo("public fun getString(): kotlin.String = \"\"\"SELECT * FROM socialFeedItem WHERE message IS NOT NULL AND userId \${ if (userId == null) \"IS\" else \"=\" } ?1 ORDER BY datetime(creation_time) DESC\"\"\"\n")
  }
}
