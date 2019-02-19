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

import java.io.Closeable
import java.util.ArrayDeque

/**
 * Implements soft line wrapping on an appendable. To use, append characters using
 * [LineWrapper.append], which will replace spaces with newlines where necessary. Use
 * [LineWrapper.appendNonWrapping] to append a string that never wraps.
 */
internal class LineWrapper(
  private val out: Appendable,
  private val indent: String,
  private val columnLimit: Int
): Closeable {
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

  /** Emit `s` replacing its spaces with line wraps as necessary. */
  fun append(s: String, indentLevel: Int = -1, linePrefix: String = "") {
    check(!closed) { "closed" }

    var pos = 0
    while (pos < s.length) {
      val c = s[pos]
      when (c) {
        ' ' -> {
          // Each space starts a new empty segment.
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

        '·' -> {
          // Render · as a non-breaking space.
          segments[segments.size - 1] += " "
          pos++
        }

        // TODO: Opening bracket -> add it to a new segment
        '(', ')' -> {
          if (segments[segments.size - 1].isEmpty()) {
            segments[segments.size - 1] += c.toString()
          } else {
            segments += c.toString()
          }
          segments += ""
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

  /** Emit `s` leaving spaces as-is. */
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

  /** Flush any outstanding text and forbid future writes to this line wrapper.  */
  override fun close() {
    emitCurrentLine()
    closed = true
  }

  private fun emitCurrentLine() {
    foldUnsafeBreaks()

    var start = 0
    var columnCount = segments[0].length
    val openingBracketIndices = ArrayDeque<Int>()

    for (i in 1 until segments.size) {
      val segment = segments[i]
      // TODO: If segment is an opening bracket -> push its index into the stack
      val newColumnCount = if (segment == "(") {
        openingBracketIndices.push(i)
        columnCount + segment.length // opening bracket is not followed by a space
      } else if (segment == ")") {
        columnCount - 1 + segment.length // closing bracket reclaims space claimed be previous seg
      } else {
        columnCount + 1 + segment.length // other segments are followed by space
      }

      // If this segment doesn't fit in the current run, print the current run and start a new one.
      // TODO: If we've got an unclosed bracket we shouldn't wrap immediately, instead we'll wait
      // until we see the closing bracket and emit each separate segment on single line
      if (newColumnCount > columnLimit) {
        if (openingBracketIndices.isEmpty()) {
          emitSegmentRange(start, i)
          start = i
          columnCount = segment.length + indent.length * indentLevel
          continue
        } else if (segment == ")") {
          val openingBracketIndex = openingBracketIndices.pop()
          if (openingBracketIndices.isNotEmpty()) continue
          emitSegmentRange(start, openingBracketIndex)
          emitStuffBetweenBrackets(openingBracketIndex, i, multiline = true)
          start = i + 1
          columnCount = segment.length + indent.length * indentLevel
          continue
        }
      } else if (segment == ")") {
        val openingBracketIndex = openingBracketIndices.pop()
        if (openingBracketIndices.isNotEmpty()) continue
        emitSegmentRange(start, openingBracketIndex)
        emitStuffBetweenBrackets(openingBracketIndex, i, multiline = false)
        start = i + 1
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
    if (startIndex > 0 && segments[startIndex].isNotEmpty()) {
      out.append("\n")
      for (i in 0 until indentLevel) {
        out.append(indent)
      }
      out.append(linePrefix)
    }

    // Emit each segment separated by spaces.
    out.append(segments[startIndex])
    for (i in startIndex + 1 until endIndex) {
      out.append(" ")
      out.append(segments[i])
    }
  }

  private fun emitStuffBetweenBrackets(
    openingBracketIndex: Int,
    closingBracketIndex: Int,
    multiline: Boolean
  ) {
    out.append(segments[openingBracketIndex])
    for (i in openingBracketIndex + 1 until closingBracketIndex) {
      if (multiline) {
        out.append("\n")
        for (j in 0 until indentLevel + 2) {
          out.append(indent)
        }
        out.append(linePrefix)
      }
      out.append(segments[i])
      if (!multiline && i < closingBracketIndex - 1) {
        out.append(' ')
      }
    }
    if (multiline) {
      out.append('\n')
    }
    out.append(segments[closingBracketIndex])
  }

  /**
   * Any segment that starts with '+' or '-' can't have a break preceding it. Combine it with the
   * preceding segment. Note that this doesn't apply to the first segment.
   */
  private fun foldUnsafeBreaks() {
    var i = 1
    while (i < segments.size) {
      val segment = segments[i]
      if (UNSAFE_LINE_START.matches(segment)) {
        segments[i - 1] = segments[i - 1] + " " + segments[i]
        segments.removeAt(i)
        if (i > 1) i--
      } else {
        i++
      }
    }
  }

  companion object {
    private val UNSAFE_LINE_START = Regex("\\s*[-+][^>=].*")
    private val SPECIAL_CHARACTERS = "() \n·".toCharArray()
  }
}
