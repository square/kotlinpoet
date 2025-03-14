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
  val code = jsCodePointAt(this, index)
  return CodePoint(code)
}

internal actual fun CodePoint.isLowerCase(): Boolean {
  // TODO Will there be a problem? Is there a better way?
  val str = jsFromCodePoint(this.code)

  if (str.length != 1) {
    return false
  }

  return str.first().isLowerCase()
}

internal actual fun CodePoint.isUpperCase(): Boolean {
  // TODO Will there be a problem? Is there a better way?
  val str = jsFromCodePoint(this.code)

  if (str.length != 1) {
    return false
  }

  return str.first().isUpperCase()
}

@Suppress("unused")
private fun jsCodePointAt(str: String, index: Int): Int =
  js("str.codePointAt(index)").unsafeCast<Int>()

@Suppress("unused")
private fun jsFromCodePoint(code: Int): String =
  js("String.fromCodePoint(code)").toString()
