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

internal fun CodeBlock.hasExplicitNewLine(): Boolean {
  var argIndex = 0
  for (formatPart in formatParts) {
    if (!formatPart.isPlaceholder) {
      if ('\n' in formatPart) return true
      continue
    }
    if (!formatPart.consumesArgument) continue

    val arg = args[argIndex++]
    if (formatPart != "%L") continue
    if (arg is CodeBlock && arg.hasExplicitNewLine()) return true
    if (arg is String && '\n' in arg) return true
  }
  return false
}

// TODO Waiting for `CodeBlock` migration.
internal fun CodeBlock.trimTrailingNewLine(
  replaceWith: Char? = null,
  trimNonLiteralStringArguments: Boolean = true,
): CodeBlock =
  if (isEmpty()) {
    this
  } else {
    with(toBuilder()) {
      val lastFormatPart = trim().formatParts.last()
      val lastFormatPartIndex = formatParts.lastIndexOf(lastFormatPart)
      if (lastFormatPart.consumesArgument && args.isNotEmpty()) {
        val lastArgIndex =
          formatParts.take(lastFormatPartIndex + 1).count { it.consumesArgument } - 1
        when (val lastArg = args[lastArgIndex]) {
          is String -> {
            if (lastFormatPart == "%L" || trimNonLiteralStringArguments) {
              val trimmedArg = lastArg.trimEnd('\n')
              args[lastArgIndex] =
                if (replaceWith != null) {
                  trimmedArg + replaceWith
                } else {
                  trimmedArg
                }
            }
          }

          is CodeBlock -> {
            if (lastFormatPart == "%L") {
              args[lastArgIndex] =
                lastArg.trimTrailingNewLine(replaceWith, trimNonLiteralStringArguments)
            }
          }
        }
      } else {
        formatParts[lastFormatPartIndex] = lastFormatPart.trimEnd('\n')
        if (replaceWith != null) {
          formatParts += "$replaceWith"
        }
      }
      return@with build()
    }
  }

private val String.consumesArgument
  get() = isPlaceholder && length == 2 && this != "%%"

private val IDENTIFIER_REGEX = IDENTIFIER_REGEX_VALUE.toRegex()

internal actual val String.isIdentifier: Boolean
  get() = IDENTIFIER_REGEX.matches(this)

internal actual fun Char.isJavaIdentifierStart(): Boolean = Character.isJavaIdentifierStart(this)

internal actual fun Char.isJavaIdentifierPart(): Boolean = Character.isJavaIdentifierPart(this)
