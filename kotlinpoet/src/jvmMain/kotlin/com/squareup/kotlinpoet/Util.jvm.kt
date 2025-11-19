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

import com.squareup.kotlinpoet.CodeBlock.Companion.isPlaceholder
import java.util.Collections

internal actual fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> =
  Collections.unmodifiableMap(LinkedHashMap(this))

internal actual fun <T> Collection<T>.toImmutableList(): List<T> =
  Collections.unmodifiableList(ArrayList(this))

internal actual fun <T> Collection<T>.toImmutableSet(): Set<T> =
  Collections.unmodifiableSet(LinkedHashSet(this))

// TODO Waiting for `CodeBlock` migration.
internal fun CodeBlock.ensureEndsWithNewLine() = trimTrailingNewLine('\n')

// TODO Waiting for `CodeBlock` migration.
internal fun CodeBlock.trimTrailingNewLine(replaceWith: Char? = null) =
  if (isEmpty()) {
    this
  } else {
    with(toBuilder()) {
      val lastFormatPart = trim().formatParts.last()
      if (lastFormatPart.isPlaceholder && args.isNotEmpty()) {
        val lastArg = args.last()
        if (lastArg is String) {
          val trimmedArg = lastArg.trimEnd('\n')
          args[args.size - 1] =
            if (replaceWith != null) {
              trimmedArg + replaceWith
            } else {
              trimmedArg
            }
        }
      } else {
        formatParts[formatParts.lastIndexOf(lastFormatPart)] = lastFormatPart.trimEnd('\n')
        if (replaceWith != null) {
          formatParts += "$replaceWith"
        }
      }
      return@with build()
    }
  }

private val IDENTIFIER_REGEX = IDENTIFIER_REGEX_VALUE.toRegex()

internal actual val String.isIdentifier: Boolean
  get() = IDENTIFIER_REGEX.matches(this)

internal actual fun Char.isJavaIdentifierStart(): Boolean = Character.isJavaIdentifierStart(this)

internal actual fun Char.isJavaIdentifierPart(): Boolean = Character.isJavaIdentifierPart(this)
