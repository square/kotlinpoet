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

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class LineWrapperTest {
  @Test
  fun wrap() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde♢fghij", indentLevel = 2)
    lineWrapper.close()
    assertThat(out.toString())
      .isEqualTo(
        """
        |abcde
        |    fghij
        """
          .trimMargin()
      )
  }

  @Test
  fun noWrap() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde♢fghi", indentLevel = 2)
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("abcde fghi")
  }

  @Test
  fun multipleWrite() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("ab♢cd♢ef♢gh♢ij♢kl♢mn♢op♢qr", indentLevel = 1)
    lineWrapper.close()
    assertThat(out.toString())
      .isEqualTo(
        """
        |ab cd ef
        |  gh ij kl
        |  mn op qr
        """
          .trimMargin()
      )
  }

  @Test
  fun fencepost() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde", indentLevel = 2)
    lineWrapper.append("fghij♢k", indentLevel = 2)
    lineWrapper.append("lmnop", indentLevel = 2)
    lineWrapper.close()
    assertThat(out.toString())
      .isEqualTo(
        """
        |abcdefghij
        |    klmnop
        """
          .trimMargin()
      )
  }

  @Test
  fun overlyLongLinesWithoutLeadingSpace() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcdefghijkl", indentLevel = 2)
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("abcdefghijkl")
  }

  @Test
  fun overlyLongLinesWithLeadingSpace() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("♢abcdefghijkl", indentLevel = 2)
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("\n    abcdefghijkl")
  }

  @Test
  fun noWrapEmbeddedNewlines() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde♢fghi\njklmn", indentLevel = 2)
    lineWrapper.append("opqrstuvwxy", indentLevel = 2)
    lineWrapper.close()
    assertThat(out.toString())
      .isEqualTo(
        """
        |abcde fghi
        |jklmnopqrstuvwxy
        """
          .trimMargin()
      )
  }

  @Test
  fun wrapEmbeddedNewlines() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde♢fghij\nklmn", indentLevel = 2)
    lineWrapper.append("opqrstuvwxy", indentLevel = 2)
    lineWrapper.close()
    assertThat(out.toString())
      .isEqualTo(
        """
        |abcde
        |    fghij
        |klmnopqrstuvwxy
        """
          .trimMargin()
      )
  }

  @Test
  fun noWrapMultipleNewlines() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde♢fghi\nklmnopq\nr♢stuvwxyz", indentLevel = 2)
    lineWrapper.close()
    assertThat(out.toString())
      .isEqualTo(
        """
        |abcde fghi
        |klmnopq
        |r stuvwxyz
        """
          .trimMargin()
      )
  }

  @Test
  fun wrapMultipleNewlines() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde♢fghi\nklmnopq\nrs♢tuvwxyz1", indentLevel = 2)
    lineWrapper.close()
    assertThat(out.toString())
      .isEqualTo(
        """
        |abcde fghi
        |klmnopq
        |rs
        |    tuvwxyz1
        """
          .trimMargin()
      )
  }

  @Test
  fun appendNonWrapping() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("ab♢cd♢ef", indentLevel = 2)
    lineWrapper.appendNonWrapping("gh ij kl mn")
    lineWrapper.close()
    assertThat(out.toString())
      .isEqualTo(
        """
        |ab cd
        |    efgh ij kl mn
        """
          .trimMargin()
      )
  }

  @Test
  fun appendNonWrappingSpace() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("ab♢cd♢ef", indentLevel = 2)
    lineWrapper.append("gh ij kl mn", indentLevel = 2)
    lineWrapper.close()
    assertThat(out.toString())
      .isEqualTo(
        """
        |ab cd
        |    efgh ij kl mn
        """
          .trimMargin()
      )
  }

  @Test
  fun loneUnsafeUnaryOperator() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("♢-1", indentLevel = 2)
    lineWrapper.close()
    assertThat(out.toString())
      .isEqualTo(
        """
        | -1
        """
          .trimMargin()
      )
  }

  @Test
  fun linePrefix() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("/**\n")
    lineWrapper.append("♢*♢")
    lineWrapper.append("a♢b♢c♢d♢e♢f♢g♢h♢i♢j♢k♢l♢m♢n\n", linePrefix = " * ")
    lineWrapper.append("♢*/")
    lineWrapper.close()
    assertThat(out.toString())
      .isEqualTo(
        """
        |/**
        | * a b c d
        | * e f g h i j
        | * k l m n
        | */
        """
          .trimMargin()
      )
  }
}
