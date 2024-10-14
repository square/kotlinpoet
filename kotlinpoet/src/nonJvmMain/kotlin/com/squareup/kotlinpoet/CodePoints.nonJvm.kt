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

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
internal actual fun StringBuilder.appendCodePoint(codePoint: CodePoint): StringBuilder {
  // TODO Is this correct?
  val code = codePoint.code
  if (code <= Char.MAX_VALUE.code) {
    append(code.toChar())
  } else {
    append(Char.MIN_HIGH_SURROGATE + ((code - 0x10000) shr 10))
    append(Char.MIN_LOW_SURROGATE + (code and 0x3ff))
  }
  return this
}

internal expect fun CodePoint.stringValue(): String

// TODO How check Java identifier start use code point?
internal actual fun CodePoint.isJavaIdentifierStart(): Boolean {
  if (charCount() != 1) return false

  return stringValue()[0].isJavaIdentifierStart()
}

// TODO How check Java identifier part use code point?
internal actual fun CodePoint.isJavaIdentifierPart(): Boolean {
  if (charCount() != 1) return false

  return stringValue()[0].isJavaIdentifierPart()
}

// TODO CodePoint.isLowerCase
internal actual fun CodePoint.isLowerCase(): Boolean {
  if (charCount() != 1) return false

  return stringValue()[0].isLowerCase()
}

// TODO CodePoint.isUpperCase
internal actual fun CodePoint.isUpperCase(): Boolean {
  if (charCount() != 1) return false

  return stringValue()[0].isUpperCase()
}

internal actual fun CodePoint.charCount(): Int {
  return if (code > Char.MAX_VALUE.code) 2 else 1
}
