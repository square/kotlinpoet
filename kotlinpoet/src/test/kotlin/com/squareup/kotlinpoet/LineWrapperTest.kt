/*
 * Copyright (C) 2017 Square, Inc.
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

import kotlin.test.Test
import kotlin.test.assertEquals

class LineWrapperTest {
  @Test fun wrap() {
    val out = StringBuilder()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("fghij")
    lineWrapper.close()
    assertEquals("abcde\n    fghij", out.toString())
  }

  @Test fun noWrap() {
    val out = StringBuilder()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("fghi")
    lineWrapper.close()
    assertEquals("abcde fghi", out.toString())
  }

  @Test fun multipleWrite() {
    val out = StringBuilder()
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
    assertEquals("ab cd ef\n  gh ij kl\n  mn op qr", out.toString())
  }

  @Test fun fencepost() {
    val out = StringBuilder()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.append("fghij")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("k")
    lineWrapper.append("lmnop")
    lineWrapper.close()
    assertEquals("abcdefghij\n    klmnop", out.toString())
  }

  @Test fun overlyLongLinesWithoutLeadingSpace() {
    val out = StringBuilder()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcdefghijkl")
    lineWrapper.close()
    assertEquals("abcdefghijkl", out.toString())
  }

  @Test fun overlyLongLinesWithLeadingSpace() {
    val out = StringBuilder()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("abcdefghijkl")
    lineWrapper.close()
    assertEquals("\n    abcdefghijkl", out.toString())
  }

  @Test fun noWrapEmbeddedNewlines() {
    val out = StringBuilder()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("fghi\njklmn")
    lineWrapper.append("opqrstuvwxy")
    lineWrapper.close()
    assertEquals("abcde fghi\njklmnopqrstuvwxy", out.toString())
  }

  @Test fun wrapEmbeddedNewlines() {
    val out = StringBuilder()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("fghij\nklmn")
    lineWrapper.append("opqrstuvwxy")
    lineWrapper.close()
    assertEquals("abcde\n    fghij\nklmnopqrstuvwxy", out.toString())
  }

  @Test fun noWrapMultipleNewlines() {
    val out = StringBuilder()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("fghi\nklmnopq\nr")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("stuvwxyz")
    lineWrapper.close()
    assertEquals("abcde fghi\nklmnopq\nr stuvwxyz", out.toString())
  }

  @Test fun wrapMultipleNewlines() {
    val out = StringBuilder()
    val lineWrapper = LineWrapper(out, "  ", 10)
    lineWrapper.append("abcde")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("fghi\nklmnopq\nrs")
    lineWrapper.wrappingSpace(2)
    lineWrapper.append("tuvwxyz1")
    lineWrapper.close()
    assertEquals("abcde fghi\nklmnopq\nrs\n    tuvwxyz1", out.toString())
  }
}
