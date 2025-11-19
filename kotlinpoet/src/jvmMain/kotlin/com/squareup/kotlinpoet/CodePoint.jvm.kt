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

import kotlin.text.codePointAt as codePointAtKt

internal actual fun String.codePointAt(index: Int): CodePoint = CodePoint(codePointAtKt(index))

internal actual fun CodePoint.isLowerCase(): Boolean = Character.isLowerCase(code)

internal actual fun CodePoint.isUpperCase(): Boolean = Character.isUpperCase(code)

internal actual fun CodePoint.isJavaIdentifierStart(): Boolean =
  Character.isJavaIdentifierStart(code)

internal actual fun CodePoint.isJavaIdentifierPart(): Boolean = Character.isJavaIdentifierPart(code)

internal actual fun CodePoint.charCount(): Int {
  return Character.charCount(code)
}

internal actual fun StringBuilder.appendCodePoint(codePoint: CodePoint): StringBuilder {
  return appendCodePoint(codePoint.code)
}
