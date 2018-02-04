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

/**
 * Implements soft line wrapping on an appendable. To use, append characters using
 * [LineWrapper.append] or soft-wrapping spaces using [LineWrapper.wrappingSpace].
 */
internal class LineWrapper(
  private val out: Appendable,
  private val indent: String,
  private val columnLimit: Int
) {
  private var closed = false

  /** The number of characters since the most recent newline. Includes both out and the buffer.  */
  private var column = 0

  private var helper: BufferedLineWrapperHelper = DefaultLineWrapperHelper()

  /** Emit `s`. This may be buffered to permit line wraps to be inserted.  */
  fun append(s: String) {
    check(!closed) { "closed" }

    if (helper.isBuffering) {
      val nextNewline = s.indexOf('\n')

      // If s doesn't cause the current line to cross the limit, buffer it and return. We'll decide
      // whether or not we have to wrap it later.
      if (nextNewline == -1 && column + s.length <= columnLimit) {
        helper.buffer(s)
        column += s.length
        return
      }

      // Wrap if appending s would overflow the current line.
      val wrap = nextNewline == -1 || column + nextNewline > columnLimit
      helper.flush(wrap)
    }

    helper.append(s)
    val lastNewline = s.lastIndexOf('\n')
    column = if (lastNewline != -1)
      s.length - lastNewline - 1 else
      column + s.length
  }

  fun openWrappingGroup() {
    check(!closed) { "closed" }

    helper = GroupLineWrapperHelper()
  }

  /** Emit either a space or a newline character.  */
  fun wrappingSpace(indentLevel: Int) {
    check(!closed) { "closed" }

    helper.wrappingSpace(indentLevel)
    this.column++
  }

  fun closeWrappingGroup(): Boolean {
    check(!closed) { "closed" }

    val wrapped = helper.close()
    helper = DefaultLineWrapperHelper()
    return wrapped
  }

  /** Flush any outstanding text and forbid future writes to this line wrapper.  */
  fun close() {
    helper.close()
    closed = true
  }

  /** Write the space followed by any buffered text that follows it.  */
  private fun flush(buffered: String, wrap: Boolean) {
    if (wrap) {
      out.append('\n')
      for (i in 0 until helper.indentLevel) {
        out.append(indent)
      }
      column = helper.indentLevel * indent.length
      column += buffered.length
    } else {
      out.append(' ')
    }
    out.append(buffered)
  }

  /**
   * Contract for helpers that handle buffering, post-processing and flushing of the input.
   */
  internal interface BufferedLineWrapperHelper {

    val indentLevel: Int

    val isBuffering get() = indentLevel != -1

    /** Append to out, bypassing the buffer */
    fun append(s: String): Appendable

    /** Append to buffer */
    fun buffer(s: String): Appendable

    /**
     * Indicates that a new wrapping space occurred in input.
     *
     * @param indentLevel Indentation level for the new line
     */
    fun wrappingSpace(indentLevel: Int)

    /**
     * Flush any buffered text.
     *
     * @param wrap `true` if buffer contents should be flushed a on new line
     * */
    fun flush(wrap: Boolean)

    /**
     * Flush and clear the buffer.
     *
     * @return `true` if input wrapped to new line
     */
    fun close(): Boolean
  }

  /** Flushes the buffer each time the wrapping space is encountered */
  internal inner class DefaultLineWrapperHelper : BufferedLineWrapperHelper {

    private val buffer = StringBuilder()

    private var _indentLevel = -1

    override val indentLevel get() = _indentLevel

    override fun append(s: String): Appendable = out.append(s)

    override fun buffer(s: String): Appendable = buffer.append(s)

    override fun wrappingSpace(indentLevel: Int) {
      if (isBuffering) flush(false)
      _indentLevel = indentLevel
    }

    override fun flush(wrap: Boolean) {
      flush(buffer.toString(), wrap)
      buffer.delete(0, buffer.length)
      _indentLevel = -1
    }

    override fun close(): Boolean {
      if (isBuffering) flush(false)
      return false
    }
  }

  /**
   * Holds multiple buffers and only flushes when the group is closed. If wrapping happened within
   * a group - each buffer will be flushed on a new line.
   */
  internal inner class GroupLineWrapperHelper : BufferedLineWrapperHelper {

    private val buffer = mutableListOf(StringBuilder())
    private var wrapped = false

    private var _indentLevel = -1

    override val indentLevel get() = _indentLevel

    override fun append(s: String): Appendable = buffer.last().append(s)

    override fun buffer(s: String): Appendable = buffer.last().append(s)

    override fun wrappingSpace(indentLevel: Int) {
      _indentLevel = indentLevel
      buffer += StringBuilder()
    }

    override fun flush(wrap: Boolean) {
      wrapped = wrap
    }

    override fun close(): Boolean {
      if (wrapped) buffer.last().append('\n')
      buffer.forEachIndexed { index, segment ->
        if (index == 0 && !wrapped) {
          out.append(segment)
        } else {
          flush(segment.toString(), wrapped)
        }
      }
      _indentLevel = -1
      return wrapped
    }
  }
}
