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

internal actual val String.isIdentifier: Boolean
  get() {
    val regExp = RegExp(IDENTIFIER_REGEX_VALUE, "gu")
    regExp.reset()

    val match = regExp.exec(this) ?: return false
    return match.index == 0 && regExp.lastIndex == length
  }

internal external interface RegExpMatch {
  val index: Int
  val length: Int
}

internal external class RegExp(pattern: String, flags: String? = definedExternally) : JsAny {
  fun exec(str: String): RegExpMatch?

  override fun toString(): String

  var lastIndex: Int
}

internal fun RegExp.reset() {
  lastIndex = 0
}
