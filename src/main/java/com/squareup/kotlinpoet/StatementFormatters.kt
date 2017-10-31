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

/**
 * Performs formatting of statements (code inside `%[` and `%]` placeholders), picking correct
 * formatting strategy based on the type of the statement.
 *
 * Examples:
 *
 * - **Multiline strings**. Each line is indented.
 *
 *   ```
 *   val a = """
 *     |one
 *     |two
 *     |three
 *     |""".trimMargin()
 *   ```
 * - **Expressions**. Each line, except for lines starting with `}`, is indented.
 *
 *   ```
 *   val a = if (2 == 2) {
 *     "one"
 *   } else {
 *     "two"
 *   }
 *   ```
 *
 * - **Chained method calls**. Each line is double-indented.
 *
 *   ```
 *   val a = listOf(1, 2, 3)
 *       .map { it * 2 }
 *       .map { it.toLong() }
 *   ```
 */
internal class StatementFormatter {

  private var strategy: FormattingStrategy? = null

  /**
   * @param firstLineElement A token from the first line of a multiline statement
   */
  fun init(firstLineElement: String) {
    // we need the last token from the first line, so it's fine to override strategy while we're
    // on line 0
    if (strategy == null || strategy!!.statementLine == 0) {
      strategy = createFormattingStrategy(firstLineElement.trim())
    }
  }

  fun nextLine(line: String, codeWriter: CodeWriter) {
    check(strategy != null) { "init() has to be called first!" }
    strategy!!.nextLine(line, codeWriter)
  }

  fun closeStatement(codeWriter: CodeWriter) = strategy?.closeStatement(codeWriter)
}

internal abstract class FormattingStrategy {

  var statementLine = 0

  abstract fun nextLine(line: String, codeWriter: CodeWriter)
  abstract fun closeStatement(codeWriter: CodeWriter)

  companion object {
    internal val NONE = object : FormattingStrategy() {
      override fun nextLine(line: String, codeWriter: CodeWriter) {
      }

      override fun closeStatement(codeWriter: CodeWriter) {
      }
    }
  }
}

internal class LineIndentFormattingStrategy(val indent: Int) : FormattingStrategy() {

  override fun nextLine(line: String, codeWriter: CodeWriter) {
    if (statementLine == 0) {
      codeWriter.indent(indent) // Begin multiple-line statement. Increase the indentation level.
    }
    statementLine++
  }

  override fun closeStatement(codeWriter: CodeWriter) {
    if (statementLine > 0) {
      codeWriter.unindent(indent) // End a multi-line statement. Decrease the indentation level.
    }
  }
}

internal fun createFormattingStrategy(firstLine: String): FormattingStrategy = when {
  firstLine.endsWith("\"\"\"") -> LineIndentFormattingStrategy(1)
  firstLine.endsWith("{") || firstLine.endsWith("->") -> FormattingStrategy.NONE
  else -> LineIndentFormattingStrategy(2)
}
