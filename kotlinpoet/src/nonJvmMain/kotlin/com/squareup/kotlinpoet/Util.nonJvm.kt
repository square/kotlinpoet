/*
 * Copyright (C) 2024 Square, Inc.
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

import kotlin.reflect.KClass
import kotlin.sequences.toCollection

internal actual fun formatIsoControlCode(code: Int): String {
  return buildString(6) {
    append("\\u")
    appendFormat04x(code)
  }
}

@OptIn(ExperimentalStdlibApi::class)
internal fun Appendable.appendFormat04x(code: Int) {
  val hex = code.toHexString(HexFormatWithoutLeadingZeros)
  if (hex.length < 4) {
    repeat(4 - hex.length) { append('0') }
  }
  append(hex)
}

@OptIn(ExperimentalStdlibApi::class)
internal actual fun Int.toHexStr(): String =
  toHexString(HexFormatWithoutLeadingZeros)

internal actual fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> =
  toMap()

internal actual fun <T> Collection<T>.toImmutableList(): List<T> =
  toList()

internal actual fun <T> Collection<T>.toImmutableSet(): Set<T> =
  toSet()

internal actual inline fun <reified E : Enum<E>> enumSetOf(vararg values: E): MutableSet<E> {
  return values.toMutableSet()
}

internal actual fun <T : Comparable<T>> Sequence<T>.toSortedSet(): Set<T> =
  toCollection(linkedSetOf())

internal actual fun <T : Comparable<T>> List<T>.toSortedSet(): Set<T> =
  toCollection(linkedSetOf())

internal actual fun <T : Comparable<T>> sortedSetOf(): MutableSet<T> = linkedSetOf()

internal actual fun KClass<*>.enclosingClass(): KClass<*>? =
  null

internal actual fun Char.isJavaIdentifierStart(): Boolean {
  return isLetter() ||
    this in CharCategory.LETTER_NUMBER ||
    this == '$' ||
    this == '_'
}

internal actual fun Char.isJavaIdentifierPart(): Boolean {
  // TODO How check Java identifier part?
  // A character may be part of a Java identifier if any of the following conditions are true:
  // - [x] it is a letter
  // - [x] it is a currency symbol (such as '$')
  // - [x] it is a connecting punctuation character (such as  '_')
  // - [x] it is a digit
  // - [x] it is a numeric letter (such as a Roman numeral character)
  // - [ ] it is a combining mark
  // - [x] it is a non-spacing mark
  // - [x] isIdentifierIgnorable returns true for the character
  return isLetter() ||
    isDigit() ||
    this in CharCategory.LETTER_NUMBER ||
    this in CharCategory.NON_SPACING_MARK ||
    this == '_' ||
    this == '$' ||
    isIdentifierIgnorable()
}

internal fun Char.isIdentifierIgnorable(): Boolean {
  // The following Unicode characters are ignorable in a Java identifier or a Unicode identifier:
  // - ISO control characters that are not whitespace
  //   - '\u0000' through '\u0008'
  //   - '\u000E' through '\u001B'
  //   - '\u007F' through '\u009F'
  // - all characters that have the FORMAT general category value
  return (
    isISOControl() && (
      this in '\u0000'..'\u0008' ||
        this in '\u000E'..'\u001B' ||
        this in '\u007F'..'\u009F'
      )
    ) || this in CharCategory.FORMAT
}
