/*
 * Copyright (C) 2015 Square, Inc.
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

import com.squareup.kotlinpoet.CodeBlock.Companion.isPlaceholder
import java.util.Collections

internal object NullAppendable : Appendable {
  override fun append(charSequence: CharSequence) = this
  override fun append(charSequence: CharSequence, start: Int, end: Int) = this
  override fun append(c: Char) = this
}

internal fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> =
    Collections.unmodifiableMap(LinkedHashMap(this))

internal fun <T> Collection<T>.toImmutableList(): List<T> =
    Collections.unmodifiableList(ArrayList(this))

internal fun <T> Collection<T>.toImmutableSet(): Set<T> =
    Collections.unmodifiableSet(LinkedHashSet(this))

internal inline fun <reified T : Enum<T>> Collection<T>.toEnumSet(): Set<T> =
    enumValues<T>().filterTo(mutableSetOf(), this::contains)

internal fun requireNoneOrOneOf(modifiers: Set<KModifier>, vararg mutuallyExclusive: KModifier) {
  val count = mutuallyExclusive.count(modifiers::contains)
  require(count <= 1) {
    "modifiers $modifiers must contain none or only one of ${mutuallyExclusive.contentToString()}"
  }
}

internal fun requireNoneOf(modifiers: Set<KModifier>, vararg forbidden: KModifier) {
  require(forbidden.none(modifiers::contains)) {
    "modifiers $modifiers must contain none of ${forbidden.contentToString()}"
  }
}

internal fun <T> T.isOneOf(t1: T, t2: T, t3: T? = null, t4: T? = null, t5: T? = null, t6: T? = null) =
    this == t1 || this == t2 || this == t3 || this == t4 || this == t5 || this == t6

internal fun <T> Collection<T>.containsAnyOf(vararg t: T) = t.any(this::contains)

// see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
internal fun characterLiteralWithoutSingleQuotes(c: Char) = when {
  c == '\b' -> "\\b" // \u0008: backspace (BS)
  c == '\t' -> "\\t" // \u0009: horizontal tab (HT)
  c == '\n' -> "\\n" // \u000a: linefeed (LF)
  c == '\r' -> "\\r" // \u000d: carriage return (CR)
  c == '\"' -> "\"" // \u0022: double quote (")
  c == '\'' -> "\\'" // \u0027: single quote (')
  c == '\\' -> "\\\\" // \u005c: backslash (\)
  c.isIsoControl -> String.format("\\u%04x", c.toInt())
  else -> Character.toString(c)
}

internal fun escapeCharacterLiterals(s: String) = buildString {
  for (c in s) append(characterLiteralWithoutSingleQuotes(c))
}

private val Char.isIsoControl: Boolean
  get() {
    return this in '\u0000'..'\u001F' || this in '\u007F'..'\u009F'
  }

/** Returns the string literal representing `value`, including wrapping double quotes.  */
internal fun stringLiteralWithQuotes(
  value: String,
  escapeDollarSign: Boolean = true,
  isConstantContext: Boolean = false
): String {
  if (!isConstantContext && '\n' in value) {
    val result = StringBuilder(value.length + 32)
    result.append("\"\"\"\n|")
    var i = 0
    while (i < value.length) {
      val c = value[i]
      if (value.regionMatches(i, "\"\"\"", 0, 3)) {
        // Don't inadvertently end the raw string too early
        result.append("\"\"\${'\"'}")
        i += 2
      } else if (c == '\n') {
        // Add a '|' after newlines. This pipe will be removed by trimMargin().
        result.append("\n|")
      } else if (c == '$' && escapeDollarSign) {
        // Escape '$' symbols with ${'$'}.
        result.append("\${\'\$\'}")
      } else {
        result.append(c)
      }
      i++
    }
    // If the last-emitted character wasn't a margin '|', add a blank line. This will get removed
    // by trimMargin().
    if (!value.endsWith("\n")) result.append("\n")
    result.append("\"\"\".trimMargin()")
    return result.toString()
  } else {
    val result = StringBuilder(value.length + 32)
    // using pre-formatted strings allows us to get away with not escaping symbols that would
    // normally require escaping, e.g. "foo ${"bar"} baz"
    if (escapeDollarSign) result.append('"') else result.append("\"\"\"")
    for (i in 0 until value.length) {
      val c = value[i]
      // Trivial case: single quote must not be escaped.
      if (c == '\'') {
        result.append("'")
        continue
      }
      // Trivial case: double quotes must be escaped.
      if (c == '\"' && escapeDollarSign) {
        result.append("\\\"")
        continue
      }
      // Trivial case: $ signs must be escaped.
      if (c == '$' && escapeDollarSign) {
        result.append("\${\'\$\'}")
        continue
      }
      // Default case: just let character literal do its work.
      result.append(characterLiteralWithoutSingleQuotes(c))
      // Need to append indent after linefeed?
    }
    if (escapeDollarSign) result.append('"') else result.append("\"\"\"")
    return result.toString()
  }
}

internal fun CodeBlock.ensureEndsWithNewLine() = if (isEmpty()) this else with(toBuilder()) {
  val lastFormatPart = trim().formatParts.last()
  if (lastFormatPart.isPlaceholder && args.isNotEmpty()) {
    val lastArg = args.last()
    if (lastArg is String) {
      args[args.size - 1] = lastArg.trimEnd('\n') + '\n'
    }
  } else {
    formatParts[formatParts.lastIndexOf(lastFormatPart)] = lastFormatPart.trimEnd('\n')
    formatParts += "\n"
  }
  return@with build()
}

private val IDENTIFIER_REGEX =
    ("((\\p{gc=Lu}+|\\p{gc=Ll}+|\\p{gc=Lt}+|\\p{gc=Lm}+|\\p{gc=Lo}+|\\p{gc=Nl}+)+" +
    "\\d*" +
    "\\p{gc=Lu}*\\p{gc=Ll}*\\p{gc=Lt}*\\p{gc=Lm}*\\p{gc=Lo}*\\p{gc=Nl}*)" +
    "|" +
    "(`[^\n\r`]+`)")
    .toRegex()

internal val String.isIdentifier get() = IDENTIFIER_REGEX.matches(this)

// https://github.com/JetBrains/kotlin/blob/master/core/descriptors/src/org/jetbrains/kotlin/renderer/KeywordStringsGenerated.java
private val KEYWORDS = setOf(
    "package",
    "as",
    "typealias",
    "class",
    "this",
    "super",
    "val",
    "var",
    "fun",
    "for",
    "null",
    "true",
    "false",
    "is",
    "in",
    "throw",
    "return",
    "break",
    "continue",
    "object",
    "if",
    "try",
    "else",
    "while",
    "do",
    "when",
    "interface",
    "typeof"
)

private const val ALLOWED_CHARACTER = '$'

internal val String.isKeyword get() = this in KEYWORDS

internal val String.hasAllowedCharacters get() = this.any { it == ALLOWED_CHARACTER }

// https://github.com/JetBrains/kotlin/blob/master/compiler/frontend.java/src/org/jetbrains/kotlin/resolve/jvm/checkers/JvmSimpleNameBacktickChecker.kt
private val ILLEGAL_CHARACTERS_TO_ESCAPE = setOf('.', ';', '[', ']', '/', '<', '>', ':', '\\')

private fun String.failIfEscapeInvalid() {
  require(!any { it in ILLEGAL_CHARACTERS_TO_ESCAPE }) {
    "Can't escape identifier $this because it contains illegal characters: " +
        ILLEGAL_CHARACTERS_TO_ESCAPE.intersect(this.toSet()).joinToString("") }
}

internal fun String.escapeIfNecessary(validate: Boolean = true): String {
  val escapedString = escapeIfNotJavaIdentifier().escapeIfKeyword().escapeIfHasAllowedCharacters()
  if (validate) {
    escapedString.failIfEscapeInvalid()
  }
  return escapedString
}

private fun String.alreadyEscaped() = startsWith("`") && endsWith("`")

private fun String.escapeIfKeyword() = if (isKeyword && !alreadyEscaped()) "`$this`" else this

private fun String.escapeIfHasAllowedCharacters() = if (hasAllowedCharacters && !alreadyEscaped()) "`$this`" else this

private fun String.escapeIfNotJavaIdentifier(): String {
  return if (!Character.isJavaIdentifierStart(first()) ||
      drop(1).any { !Character.isJavaIdentifierPart(it) } && !alreadyEscaped()) {
    "`$this`"
  } else {
    this
  }
}

internal fun String.escapeSegmentsIfNecessary(delimiter: Char = '.') = split(delimiter)
    .filter { it.isNotEmpty() }
    .joinToString(delimiter.toString()) { it.escapeIfNecessary() }
