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

/**
 * Implements soft line wrapping on an appendable. To use, append characters using
 * [LineWrapper.append], which will handle formatting characters as necessary. Use
 * [LineWrapper.appendNonWrapping] to append a string that never wraps.
 */
internal class LineWrapper(
  private val out: Appendable,
  private val indent: String,
  private val columnLimit: Int,
) : AutoCloseable {

  private var closed = false

  /**
   * Segments of the current line to be joined by spaces or wraps. Never empty, but contains a lone
   * empty string if no data has been emitted since the last newline.
   */
  private val segments = mutableListOf("")

  /** Number of indents in wraps. -1 if the current line has no wraps. */
  private var indentLevel = -1

  /** Optional prefix that will be prepended to wrapped lines. */
  private var linePrefix = ""

  /** @return whether or not there are pending segments for the current line. */
  val hasPendingSegments
    get() = segments.size != 1 || segments[0].isNotEmpty()

  /** Emit `s` handling formatting characters as necessary. */
  fun append(s: String, indentLevel: Int = -1, linePrefix: String = "") {
    check(!closed) { "closed" }

    var pos = 0
    while (pos < s.length) {
      when (s[pos]) {
        '♢' -> {
          // Each wrapping space starts a new empty segment.
          this.indentLevel = indentLevel
          this.linePrefix = linePrefix
          segments += ""
          pos++
        }

        '\n' -> {
          // Each newline emits the current segments.
          newline()
          pos++
        }

        ' ',
        '·' -> {
          // Render · as a non-breaking space.
          segments[segments.size - 1] += " "
          pos++
        }

        else -> {
          var next = s.indexOfAny(SPECIAL_CHARACTERS, pos)
          if (next == -1) next = s.length
          segments[segments.size - 1] += s.substring(pos, next)
          pos = next
        }
      }
    }
  }

  /** Emit `s` leaving formatting characters as-is. */
  fun appendNonWrapping(s: String) {
    check(!closed) { "closed" }
    require(!s.contains("\n"))

    segments[segments.size - 1] += s
  }

  fun newline() {
    check(!closed) { "closed" }

    emitCurrentLine()
    out.append("\n")
    indentLevel = -1
  }

  /** Flush any outstanding text and forbid future writes to this line wrapper. */
  override fun close() {
    emitCurrentLine()
    closed = true
  }

  private fun emitCurrentLine() {
    var start = 0
    var columnCount = segments[0].length

    for (i in 1..<segments.size) {
      val segment = segments[i]
      val newColumnCount = columnCount + 1 + segment.length

      // If this segment doesn't fit in the current run, print the current run and start a new one.
      if (newColumnCount > columnLimit) {
        emitSegmentRange(start, i)
        start = i
        columnCount = segment.length + indent.length * indentLevel
        continue
      }

      columnCount = newColumnCount
    }

    // Print the last run.
    emitSegmentRange(start, segments.size)

    segments.clear()
    segments += ""
  }

  private fun emitSegmentRange(startIndex: Int, endIndex: Int) {
    // If this is a wrapped line we need a newline and an indent.
    if (startIndex > 0) {
      out.append("\n")
      for (i in 0..<indentLevel) {
        out.append(indent)
      }
      out.append(linePrefix)
    }

    // Emit each segment separated by spaces.
    out.append(segments[startIndex])
    for (i in startIndex + 1..<endIndex) {
      out.append(" ")
      out.append(segments[i])
    }
  }

  companion object {
    private val SPECIAL_CHARACTERS = " \n·♢".toCharArray()
  }
}
