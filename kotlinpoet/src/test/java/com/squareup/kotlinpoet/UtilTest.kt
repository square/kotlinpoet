/*
 * Copyright (C) 2016 Square, Inc.
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

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals

class UtilTest {
  @Test fun characterLiteral() {
    assertEquals("a", characterLiteralWithoutSingleQuotes('a'))
    assertEquals("b", characterLiteralWithoutSingleQuotes('b'))
    assertEquals("c", characterLiteralWithoutSingleQuotes('c'))
    assertEquals("%", characterLiteralWithoutSingleQuotes('%'))
    // common escapes
    assertEquals("\\b", characterLiteralWithoutSingleQuotes('\b'))
    assertEquals("\\t", characterLiteralWithoutSingleQuotes('\t'))
    assertEquals("\\n", characterLiteralWithoutSingleQuotes('\n'))
    assertEquals("\\u000c", characterLiteralWithoutSingleQuotes('\u000c'))
    assertEquals("\\r", characterLiteralWithoutSingleQuotes('\r'))
    assertEquals("\"", characterLiteralWithoutSingleQuotes('"'))
    assertEquals("\\'", characterLiteralWithoutSingleQuotes('\''))
    assertEquals("\\\\", characterLiteralWithoutSingleQuotes('\\'))
    // octal escapes
    assertEquals("\\u0000", characterLiteralWithoutSingleQuotes('\u0000'))
    assertEquals("\\u0007", characterLiteralWithoutSingleQuotes('\u0007'))
    assertEquals("?", characterLiteralWithoutSingleQuotes('\u003f'))
    assertEquals("\\u007f", characterLiteralWithoutSingleQuotes('\u007f'))
    assertEquals("¿", characterLiteralWithoutSingleQuotes('\u00bf'))
    assertEquals("ÿ", characterLiteralWithoutSingleQuotes('\u00ff'))
    // unicode escapes
    assertEquals("\\u0000", characterLiteralWithoutSingleQuotes('\u0000'))
    assertEquals("\\u0001", characterLiteralWithoutSingleQuotes('\u0001'))
    assertEquals("\\u0002", characterLiteralWithoutSingleQuotes('\u0002'))
    assertEquals("€", characterLiteralWithoutSingleQuotes('\u20AC'))
    assertEquals("☃", characterLiteralWithoutSingleQuotes('\u2603'))
    assertEquals("♠", characterLiteralWithoutSingleQuotes('\u2660'))
    assertEquals("♣", characterLiteralWithoutSingleQuotes('\u2663'))
    assertEquals("♥", characterLiteralWithoutSingleQuotes('\u2665'))
    assertEquals("♦", characterLiteralWithoutSingleQuotes('\u2666'))
    assertEquals("✵", characterLiteralWithoutSingleQuotes('\u2735'))
    assertEquals("✺", characterLiteralWithoutSingleQuotes('\u273A'))
    assertEquals("／", characterLiteralWithoutSingleQuotes('\uFF0F'))
  }

  @Test fun stringLiteral() {
    stringLiteral("abc")
    stringLiteral("♦♥♠♣")
    stringLiteral("€\\t@\\t\${\'\$\'}", "€\t@\t$")
    assertThat(stringLiteralWithQuotes("abc();\ndef();"))
      .isEqualTo("\"\"\"\n|abc();\n|def();\n\"\"\".trimMargin()")
    stringLiteral("This is \\\"quoted\\\"!", "This is \"quoted\"!")
    stringLiteral("e^{i\\\\pi}+1=0", "e^{i\\pi}+1=0")
    assertThat(stringLiteralWithQuotes("abc();\ndef();", isConstantContext = true))
      .isEqualTo("\"abc();\\ndef();\"")
  }

  @Test fun legalIdentifiers() {
    assertThat("foo".isIdentifier).isTrue()
    assertThat("bAr1".isIdentifier).isTrue()
    assertThat("1".isIdentifier).isFalse()
    assertThat("♦♥♠♣".isIdentifier).isFalse()
    assertThat("`♦♥♠♣`".isIdentifier).isTrue()
    assertThat("`  ♣ !`".isIdentifier).isTrue()
    assertThat("€".isIdentifier).isFalse()
    assertThat("`€`".isIdentifier).isTrue()
    assertThat("`1`".isIdentifier).isTrue()
    assertThat("```".isIdentifier).isFalse()
    assertThat("``".isIdentifier).isFalse()
    assertThat("\n".isIdentifier).isFalse()
    assertThat("`\n`".isIdentifier).isFalse()
    assertThat("\r".isIdentifier).isFalse()
    assertThat("`\r`".isIdentifier).isFalse()
    assertThat("when".isIdentifier).isTrue()
    assertThat("fun".isIdentifier).isTrue()
    assertThat("".isIdentifier).isFalse()
  }

  @Test fun escapeNonJavaIdentifiers() {
    assertThat("8startWithNumber".escapeIfNecessary()).isEqualTo("`8startWithNumber`")
    assertThat("with-hyphen".escapeIfNecessary()).isEqualTo("`with-hyphen`")
    assertThat("with space".escapeIfNecessary()).isEqualTo("`with·space`")
    assertThat("with_unicode_punctuation\u2026".escapeIfNecessary()).isEqualTo("`with_unicode_punctuation\u2026`")
  }

  @Test fun escapeSpaceInName() {
    val generated = FileSpec.builder("a", "b")
      .addFunction(
        FunSpec.builder("foo").apply {
          addParameter("aaa bbb", typeNameOf<(Int) -> String>())
          val arg = mutableListOf<String>()
          addStatement(
            StringBuilder().apply {
              repeat(10) {
                append("%N($it) + ")
                arg += "aaa bbb"
              }
              append("%N(100)")
              arg += "aaa bbb"
            }.toString(),
            *arg.toTypedArray(),
          )
        }.build(),
      )
      .build()
      .toString()

    val expectedOutput = """
      package a

      import kotlin.Function1
      import kotlin.Int
      import kotlin.String
      import kotlin.Unit

      public fun foo(`aaa bbb`: Function1<Int, String>): Unit {
        `aaa bbb`(0) + `aaa bbb`(1) + `aaa bbb`(2) + `aaa bbb`(3) + `aaa bbb`(4) + `aaa bbb`(5) +
            `aaa bbb`(6) + `aaa bbb`(7) + `aaa bbb`(8) + `aaa bbb`(9) + `aaa bbb`(100)
      }

    """.trimIndent()

    assertThat(generated).isEqualTo(expectedOutput)
  }

  @Test fun escapeMultipleTimes() {
    assertThat("A-\$B".escapeIfNecessary()).isEqualTo("`A-\$B`")
  }

  @Test fun escapeEscaped() {
    assertThat("`A`".escapeIfNecessary()).isEqualTo("`A`")
  }

  private fun stringLiteral(string: String) = stringLiteral(string, string)

  private fun stringLiteral(expected: String, value: String) =
    assertEquals("\"$expected\"", stringLiteralWithQuotes(value))
}
