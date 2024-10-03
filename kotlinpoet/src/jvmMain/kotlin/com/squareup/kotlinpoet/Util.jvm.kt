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

import java.util.Collections
import java.util.EnumSet
import java.util.TreeSet
import kotlin.collections.toSortedSet as toSortedSetKt
import kotlin.reflect.KClass
import kotlin.sequences.toSortedSet as toSortedSetKt

private val IDENTIFIER_REGEX = IDENTIFIER_REGEX_VALUE.toRegex()

internal actual val String.isIdentifier: Boolean get() = IDENTIFIER_REGEX.matches(this)

internal actual fun formatIsoControlCode(code: Int): String =
  String.format("\\u%04x", code)

internal actual fun Int.toHexStr(): String =
  Integer.toHexString(this)

internal actual fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> =
  Collections.unmodifiableMap(LinkedHashMap(this))

internal actual fun <T> Collection<T>.toImmutableList(): List<T> =
  Collections.unmodifiableList(ArrayList(this))

internal actual fun <T> Collection<T>.toImmutableSet(): Set<T> =
  Collections.unmodifiableSet(LinkedHashSet(this))

internal actual fun <T : Comparable<T>> Sequence<T>.toSortedSet(): Set<T> =
  toSortedSetKt()

internal actual fun <T : Comparable<T>> List<T>.toSortedSet(): Set<T> =
  toSortedSetKt()

internal actual fun <T : Comparable<T>> sortedSetOf(): MutableSet<T> =
  TreeSet()

internal actual inline fun <reified E : Enum<E>> enumSetOf(vararg values: E): MutableSet<E> {
  return when {
    values.isEmpty() -> EnumSet.noneOf(E::class.java)
    values.size == 1 -> EnumSet.of(values[0])
    values.size == 2 -> EnumSet.of(values[0], values[1])
    values.size == 3 -> EnumSet.of(values[0], values[1], values[2])
    values.size == 4 -> EnumSet.of(values[0], values[1], values[2], values[3])
    values.size == 5 -> EnumSet.of(values[0], values[1], values[2], values[3], values[4])
    else -> EnumSet.copyOf(values.toSet())
  }
}

internal actual fun KClass<*>.enclosingClass(): KClass<*>? =
  java.enclosingClass?.kotlin

internal actual fun Char.isJavaIdentifierStart(): Boolean =
  Character.isJavaIdentifierStart(this)

internal actual fun Char.isJavaIdentifierPart(): Boolean =
  Character.isJavaIdentifierPart(this)
