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

internal fun <K, V> Map<K, List<V>>.toImmutableMultimap(): Map<K, List<V>> {
  val result = LinkedHashMap<K, List<V>>()
  for ((key, value) in this) {
    if (value.isEmpty()) continue
    result.put(key, value.toImmutableList())
  }
  return Collections.unmodifiableMap(result)
}

internal fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> {
  return Collections.unmodifiableMap(LinkedHashMap(this))
}

internal fun <T> Collection<T>.toImmutableList(): List<T> {
  return Collections.unmodifiableList(ArrayList(this))
}

internal fun <T> Collection<T>.toImmutableSet(): Set<T> {
  return Collections.unmodifiableSet(LinkedHashSet(this))
}

internal fun requireExactlyOneOf(modifiers: Set<KModifier>, vararg mutuallyExclusive: KModifier) {
  val count = mutuallyExclusive.count { modifiers.contains(it) }
  if (count != 1) {
    throw IllegalArgumentException(
        String.format("modifiers %s must contain one of %s", modifiers,
            Arrays.toString(mutuallyExclusive)))
  }
}

internal fun characterLiteralWithoutSingleQuotes(c: Char): String {
  // see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
  when (c) {
    '\b' -> return "\\b" /* \u0008: backspace (BS) */
    '\t' -> return "\\t" /* \u0009: horizontal tab (HT) */
    '\n' -> return "\\n" /* \u000a: linefeed (LF) */
    // TODO update this function for Kotlin '\f' -> return "\\f" /* \u000c: form feed (FF) */
    '\r' -> return "\\r" /* \u000d: carriage return (CR) */
    '\"' -> return "\""  /* \u0022: double quote (") */
    '\'' -> return "\\'" /* \u0027: single quote (') */
    '\\' -> return "\\\\"  /* \u005c: backslash (\) */
    else -> return if (isISOControl(c)) String.format("\\u%04x",
        c.toInt()) else Character.toString(c)
  }
}

/** Returns the string literal representing `value`, including wrapping double quotes.  */
internal fun stringLiteralWithDoubleQuotes(value: String, indent: String): String {
  val result = StringBuilder(value.length + 2)
  result.append('"')
  for (i in 0..value.length - 1) {
    val c = value[i]
    // trivial case: single quote must not be escaped
    if (c == '\'') {
      result.append("'")
      continue
    }
    // trivial case: double quotes must be escaped
    if (c == '\"') {
      result.append("\\\"")
      continue
    }
    // default case: just let character literal do its work
    result.append(characterLiteralWithoutSingleQuotes(c))
    // need to append indent after linefeed?
    if (c == '\n' && i + 1 < value.length) {
      result.append("\"\n").append(indent).append(indent).append("+ \"")
    }
  }
  result.append('"')
  return result.toString()
}
