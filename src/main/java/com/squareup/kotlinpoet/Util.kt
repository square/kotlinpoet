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

import java.lang.Character.isISOControl
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.LinkedHashMap
import java.util.LinkedHashSet

internal val NULL_APPENDABLE = object : Appendable {
  override fun append(charSequence: CharSequence) = this
  override fun append(charSequence: CharSequence, start: Int, end: Int) = this
  override fun append(c: Char) = this
}

internal fun <K, V> Map<K, List<V>>.toImmutableMultimap(): Map<K, List<V>> {
  val result = LinkedHashMap<K, List<V>>()
  for ((key, value) in this) {
    if (value.isEmpty()) continue
    result.put(key, value.toImmutableList())
  }
  return Collections.unmodifiableMap(result)
}

internal fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V>
    = Collections.unmodifiableMap(LinkedHashMap(this))

internal fun <T> Collection<T>.toImmutableList(): List<T>
    = Collections.unmodifiableList(ArrayList(this))

internal fun <T> Collection<T>.toImmutableSet(): Set<T>
    = Collections.unmodifiableSet(LinkedHashSet(this))

internal fun requireExactlyOneOf(modifiers: Set<KModifier>, vararg mutuallyExclusive: KModifier) {
  val count = mutuallyExclusive.count(modifiers::contains)
  require(count == 1) {
    "modifiers $modifiers must contain one of ${Arrays.toString(mutuallyExclusive)}"
  }
}

internal fun requireNoneOrOneOf(modifiers: Set<KModifier>, vararg mutuallyExclusive: KModifier) {
  val count = mutuallyExclusive.count(modifiers::contains)
  require(count <= 1) {
    "modifiers $modifiers must contain none or only one of ${Arrays.toString(mutuallyExclusive)}"
  }
}

internal fun requireNoneOf(modifiers: Set<KModifier>, vararg forbidden: KModifier) {
  require(forbidden.none(modifiers::contains)) {
    "modifiers $modifiers must contain none of ${Arrays.toString(forbidden)}"
  }
}

// see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
internal fun characterLiteralWithoutSingleQuotes(c: Char) = when {
  c == '\b' -> "\\b"   // \u0008: backspace (BS)
  c == '\t' -> "\\t"   // \u0009: horizontal tab (HT)
  c == '\n' -> "\\n"   // \u000a: linefeed (LF)
  c == '\r' -> "\\r"   // \u000d: carriage return (CR)
  c == '\"' -> "\""    // \u0022: double quote (")
  c == '\'' -> "\\'"   // \u0027: single quote (')
  c == '\\' -> "\\\\"  // \u005c: backslash (\)
  isISOControl(c) -> String.format("\\u%04x", c.toInt())
  else -> Character.toString(c)
}

/** Returns the string literal representing `value`, including wrapping double quotes.  */
internal fun stringLiteralWithQuotes(value: String): String {
  if (value.contains("\n")) {
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
    result.append('"')
    for (i in 0 until value.length) {
      val c = value[i]
      // Trivial case: single quote must not be escaped.
      if (c == '\'') {
        result.append("'")
        continue
      }
      // Trivial case: double quotes must be escaped.
      if (c == '\"') {
        result.append("\\\"")
        continue
      }
      // Default case: just let character literal do its work.
      result.append(characterLiteralWithoutSingleQuotes(c))
      // Need to append indent after linefeed?
    }
    result.append('"')
    return result.toString()
  }
}

internal fun isIdentifier(name: String): Boolean {
  return IDENTIFIER_REGEX.matches(name)
}

internal fun isKeyword(name: String): Boolean {
  return KEYWORDS.contains(name)
}

internal fun isName(name: String): Boolean {
  return name.split("\\.").none { isKeyword(name) }
}

private val IDENTIFIER_REGEX
    = ("((\\p{gc=Lu}+|\\p{gc=Ll}+|\\p{gc=Lt}+|\\p{gc=Lm}+|\\p{gc=Lo}+|\\p{gc=Nl}+)+" +
    "\\d*" +
    "\\p{gc=Lu}*\\p{gc=Ll}*\\p{gc=Lt}*\\p{gc=Lm}*\\p{gc=Lo}*\\p{gc=Nl}*)" +
    "|" +
    "(`[^\n\r`]+`)")
    .toRegex()

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
