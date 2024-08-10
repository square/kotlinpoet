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

internal actual fun String.codePointAt(index: Int): CodePoint {
  val str = this
  val code = jsCodePointAt(str, index)
  return CodePoint(code)
}

// TODO CodePoint.isLowerCase
internal actual fun CodePoint.isLowerCase(): Boolean {
  val code = this.code
  val str = jsFromCodePoint(code)

  if (str.length != 1) {
    return false
  }

  return str.first().isLowerCase()
}

// TODO CodePoint.isUpperCase
internal actual fun CodePoint.isUpperCase(): Boolean {
  val code = this.code
  val str = jsFromCodePoint(code)

  if (str.length != 1) {
    return false
  }

  return str.first().isUpperCase()
}

@Suppress("UNUSED_PARAMETER")
private fun jsCodePointAt(str: String, index: Int): Int =
  js("str.codePointAt(index)")

@Suppress("UNUSED_PARAMETER")
private fun jsFromCodePoint(code: Int): String =
  js("String.fromCodePoint(code)")

internal actual fun CodePoint.charCount(): Int {
  return if (code >= 0x010000) 2 else 1
  // return jsFromCodePoint(code).length
}