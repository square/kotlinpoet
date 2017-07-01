/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LineWrapperTest {
  @Test fun wrap() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("fghij")
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("abcde\n    fghij")
  }

  @Test fun noWrap() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("fghi")
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("abcde fghi")
  }

  @Test fun multipleWrite() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("ab")
    lineWrapper.wrappingSpace(1)
    lineWrapper.append("cd")
    lineWrapper.wrappingSpace(1)
    lineWrapper.append("ef")
    lineWrapper.wrappingSpace(1)
    lineWrapper.append("gh")
    lineWrapper.wrappingSpace(1)
    lineWrapper.append("ij")
    lineWrapper.wrappingSpace(1)
    lineWrapper.append("kl")
    lineWrapper.wrappingSpace(1)
    lineWrapper.append("mn")
    lineWrapper.wrappingSpace(1)
    lineWrapper.append("op")
    lineWrapper.wrappingSpace(1)
    lineWrapper.append("qr")
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("ab cd ef\n  gh ij kl\n  mn op qr")
  }

  @Test fun fencepost() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.append("fghij")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("k")
    lineWrapper.append("lmnop")
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("abcdefghij\n    klmnop")
  }

  @Test fun overlyLongLinesWithoutLeadingSpace() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcdefghijkl")
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("abcdefghijkl")
  }

  @Test fun overlyLongLinesWithLeadingSpace() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("abcdefghijkl")
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("\n    abcdefghijkl")
  }

  @Test fun noWrapEmbeddedNewlines() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("fghi\njklmn")
    lineWrapper.append("opqrstuvwxy")
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("abcde fghi\njklmnopqrstuvwxy")
  }

  @Test fun wrapEmbeddedNewlines() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("fghij\nklmn")
    lineWrapper.append("opqrstuvwxy")
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("abcde\n    fghij\nklmnopqrstuvwxy")
  }

  @Test fun noWrapMultipleNewlines() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("fghi\nklmnopq\nr")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("stuvwxyz")
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("abcde fghi\nklmnopq\nr stuvwxyz")
  }

  @Test fun wrapMultipleNewlines() {
    val out = StringBuffer()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("fghi\nklmnopq\nrs")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("tuvwxyz1")
    lineWrapper.close()
    assertThat(out.toString()).isEqualTo("abcde fghi\nklmnopq\nrs\n    tuvwxyz1")
  }
}
